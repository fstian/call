package com.example.nettytest.pub.commondevice;


import java.net.SocketAddress;

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
            if(channel.isActive()&&channel.isWritable()) {
                channel.writeAndFlush(buf);
            }
        }
    }


    @Override
    public String GetNetAddress() {
        String address="";
        if(channel!=null){
            address = channel.remoteAddress().toString();
            if(address.contains("/")){
                address = address.substring(address.indexOf('/')+1);
            }
            if(address.contains(":")){
                address = address.substring(0,address.indexOf(':'));
            }
        }
        return address;
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
