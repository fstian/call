package com.example.nettytest.backend.servernet;

import com.example.nettytest.pub.LogWork;

import java.net.SocketAddress;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class NettyTestServerExceptionHandler extends ChannelDuplexHandler {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Uncaught exceptions from inbound handlers will propagate up to this handler
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"Netty Sever Caught err %s",cause.getMessage());
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        ctx.connect(remoteAddress, localAddress, promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    // Handle connect exception here...
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"Netty Sever connect Fail From %s to %s ",remoteAddress.toString(),localAddress.toString());
                }else{
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"Netty Sever connect Succ From %s to %s ",remoteAddress.toString(),localAddress.toString());
                }
            }
        }));
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        ctx.write(msg, promise.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    // Handle write exception here...
//                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"Netty Sever Write Fail ");
                }else{
//                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"Netty Sever Write Suucc");
                }
            }
        }));
    }

    // ... override more outbound methods to handle their exceptions as well
}