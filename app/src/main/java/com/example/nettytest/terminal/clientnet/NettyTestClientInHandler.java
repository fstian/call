package com.example.nettytest.terminal.clientnet;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class NettyTestClientInHandler extends ChannelInboundHandlerAdapter {
    String devID;
    public NettyTestClientInHandler(String devID) {
        super();
        this.devID = devID;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf m = (ByteBuf)msg;
        try {
            ProtocolPacket packet = ProtocolFactory.ParseData(m);
            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Client %s Read %s Packet From Netty",devID, ProtocolPacket.GetTypeName(packet.type));
            HandlerMgr.PhoneProcessPacket(packet);
        }finally {
            m.release();
        }
    }

    @Override

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        HandlerMgr.UpdatePhoneDevChannel(devID,ctx.channel());
        LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Client %s Connect Success in Handler",devID);
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Device %s Connect to Server Success",devID);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Client %s Recv Inactive in Handler",devID);
        HandlerMgr.UpdatePhoneDevChannel(devID,null);
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_WARN,"Device %s Disconnect From Server",devID);
    }
}
