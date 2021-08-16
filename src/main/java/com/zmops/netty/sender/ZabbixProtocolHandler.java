package com.zmops.netty.sender;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author nantian created at 2021/8/16 1:49
 */
@Data
@Slf4j
public class ZabbixProtocolHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static ChannelHandlerContext context;


    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            System.out.println(decodeToPayload(ctx, msg));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ctx.disconnect();
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        context = ctx;
    }

    public static void sendMessage(String message) throws IOException {
        int payloadLength = message.length();

        byte[] header = new byte[]{
                'Z', 'B', 'X', 'D', '\1',
                (byte) (payloadLength & 0xFF),
                (byte) ((payloadLength >> 8) & 0xFF),
                (byte) ((payloadLength >> 16) & 0xFF),
                (byte) ((payloadLength >> 24) & 0xFF),
                '\0', '\0', '\0', '\0'};

        ByteBuf buffer = context.alloc().buffer(header.length + payloadLength);
        buffer.writeBytes(header);
        buffer.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        buffer.retain();

        context.writeAndFlush(buffer);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("An exception was thrown, cause:" + cause.getMessage());
        ctx.close();
    }


    private static final int    HEADER_LEN = 9;
    private static final byte[] PROTOCOL   = new byte[]{'Z', 'B', 'X', 'D'};

    public String decodeToPayload(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws InterruptedException, ZabbixErrorProtocolException {
        int readable  = byteBuf.readableBytes();
        int baseIndex = byteBuf.readerIndex();
        if (readable < HEADER_LEN) {
            byteBuf.readerIndex(baseIndex);
            return null;
        }

        // Read header
        ByteBuf headerBuf = byteBuf.readSlice(HEADER_LEN);
        if (headerBuf.getByte(0) != PROTOCOL[0] || headerBuf.getByte(1) != PROTOCOL[1]
                || headerBuf.getByte(2) != PROTOCOL[2] || headerBuf.getByte(3) != PROTOCOL[3]) {
            throw new ZabbixErrorProtocolException("header is not right");
        }

        // Only support communications protocol
        if (headerBuf.getByte(4) != 1) {
            throw new ZabbixErrorProtocolException("header flags only support communications protocol");
        }

        // Check payload
        int dataLength = headerBuf.getByte(5) & 0xFF
                | (headerBuf.getByte(6) & 0xFF) << 8
                | (headerBuf.getByte(7) & 0xFF) << 16
                | (headerBuf.getByte(8) & 0xFF) << 24;
        int totalLength = HEADER_LEN + dataLength + 4;
        // If not receive all data, reset buffer and re-decode after content receive finish
        if (readable < totalLength) {
            byteBuf.readerIndex(baseIndex);
            return null;
        }

        if (dataLength <= 0) {
            throw new ZabbixErrorProtocolException("content could not be empty");
        }

        // Skip protocol extensions
        byteBuf.skipBytes(4);

        // Reading content
        ByteBuf payload = byteBuf.readSlice(dataLength);

        return payload.toString(StandardCharsets.UTF_8);
    }
}
