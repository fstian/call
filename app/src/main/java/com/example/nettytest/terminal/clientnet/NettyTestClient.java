package com.example.nettytest.terminal.clientnet;

import com.example.nettytest.pub.LogWork;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;

public class NettyTestClient {
    private final int port;
    private final String host;
    private final String devID;

    public NettyTestClient(String devID,String h,int p){
        host = h;
        port = p;
        this.devID = devID;
    }

    public void run() {
        EventLoopGroup clientWorker = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(clientWorker).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) {
                    socketChannel.pipeline().addLast(new LineBasedFrameDecoder(0x10000));
                    socketChannel.pipeline().addLast(new NettyTestClientInHandler(devID));
//                        socketChannel.pipeline().addLast(new NettyTestClientExceptionHandler());
//                    socketChannel.pipeline().addLast(new NettyTestClientOutHandler());
                }
            });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            if (future.isSuccess()) {
                LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_DEBUG, "Phone %s Connect to Server Success", devID);
            }

            future.channel().closeFuture().sync();
            LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_DEBUG, "Phone %s Close Connect", devID);
        }catch (Exception e){
            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Phone %s Connect to Server Fail",devID);

        }finally {
            clientWorker.shutdownGracefully();
            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Phone %s Close Connect",devID);
        }
    }
}
