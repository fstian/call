package com.example.nettytest.backend.servernet;

import com.example.nettytest.pub.LogWork;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;

public class NettyTestServer {

    private final int port;


    public NettyTestServer(int p){
        System.out.println("Create NettyTest Server");
        port = p;
    }

    public void run() throws  Exception{
        EventLoopGroup leader = new NioEventLoopGroup();
        EventLoopGroup coder = new NioEventLoopGroup();

        try {
            ServerBootstrap server = new ServerBootstrap();
            server.group(leader, coder).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline().addLast(new LineBasedFrameDecoder(0x10000));
                    socketChannel.pipeline().addLast(new NettyTestServerHandler());
//                    socketChannel.pipeline().addLast(new NettyTestServerExceptionHandler());
                }
            }).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture channelFuture = server.bind(port).sync();

            if (channelFuture.isSuccess()) {
                LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_DEBUG,"Start Server Success");
            }

            channelFuture.channel().closeFuture().sync();
            LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_DEBUG,"Server Closed");
        }finally {
            leader.shutdownGracefully();
            coder.shutdownGracefully();
            LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_DEBUG,"Finally All shutdown");
        }
    }

}
