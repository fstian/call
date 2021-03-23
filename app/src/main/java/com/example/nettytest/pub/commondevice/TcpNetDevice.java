package com.example.nettytest.pub.commondevice;

import com.example.nettytest.pub.LogWork;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class TcpNetDevice extends NetDevice{
    // for netty device
    protected Channel channel;


    public TcpNetDevice(String id){
        super(id);
        netType = TCP_NET_DEVICE;
    }

    public void SendBuffer(ByteBuf buf){
        if(channel!=null) {
            if(channel.isActive()) {
                channel.writeAndFlush(buf);
            }
        }
    }

    public void UpdateChannel(Channel ch){
        channel= ch;
    }


    @Override
    public void Close() {
        super.Close();
        if(channel!=null) {
            channel.close();
            channel = null;
        }
    }
}
