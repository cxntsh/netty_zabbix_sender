package com.zmops.netty.sender;

import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Random;

/**
 * @author nantian created at 2021/8/16 1:49
 */
public class ZabbixSender {

    private static ChannelFuture channelFuture;


    public static void main(String[] args) {
        EventLoopGroup group  = new NioEventLoopGroup();
        Bootstrap      client = new Bootstrap();

        client.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress("172.16.3.72", 10051))
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast(new ZabbixProtocolHandler());
                    }
                });


        Gson gson = new Gson();


        for (; ; ) {

            ZabbixTrapperRequest.SenderData senderData = new ZabbixTrapperRequest.SenderData();
            senderData.setHost("10084");
            senderData.setKey("cpu.temp");
            senderData.setValue(new Random().nextInt(100) + "");


            ZabbixTrapperRequest request = new ZabbixTrapperRequest();
            request.setData(Collections.singletonList(senderData));


            try {
                channelFuture = client.connect().sync();
                ZabbixProtocolHandler.sendMessage(gson.toJson(request));
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}
