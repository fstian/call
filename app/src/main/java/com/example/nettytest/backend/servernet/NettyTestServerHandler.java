package com.example.nettytest.backend.servernet;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyTestServerHandler extends ChannelInboundHandlerAdapter {

    String devId = "";

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf)msg;
        try {
            ProtocolPacket packet = ProtocolFactory.ParseData(buf);
            if(packet!=null) {
                LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_DEBUG,String.format("Recv Dev %s TCP Data",devId));
                HandlerMgr.UpdateBackEndDevChannel(packet.sender,ctx.channel());
                devId = packet.sender;
                HandlerMgr.BackEndProcessPacket(packet);
            }
        }finally {
            buf.release();
        }

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        super.exceptionCaught(ctx, cause);
//        cause.printStackTrace();
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"Netty Sever Dev %s Caught err %s",devId,cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(!devId.isEmpty()){
            LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_ERROR,String.format("Dev %s TCP link Inactive",devId));
            HandlerMgr.UpdateBackEndDevChannel(devId,null);
        }
        super.channelInactive(ctx);
    }

}
