package com.example.nettytest.pub.commondevice;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class NetDevice {
    protected String id;
    protected Channel channel;

    public NetDevice(String id){
        this.id = id;
        channel = null;
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

    public void Close(){
        channel.close();
    }
}
