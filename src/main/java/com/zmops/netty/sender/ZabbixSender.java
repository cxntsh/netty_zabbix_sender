package com.zmops.netty.sender;

import com.google.gson.Gson;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
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
                .remoteAddress(new InetSocketAddress("127.0.0.1", 10051)) // zabbix server 部署地址
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast(new ZabbixProtocolHandler());
                    }
                });


        Gson gson = new Gson();


        // 模拟测试数据，5s 发送一次
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
