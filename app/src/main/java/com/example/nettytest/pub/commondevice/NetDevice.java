package com.example.nettytest.pub.commondevice;

public class NetDevice {
    final static int UDP_NET_DEVICE  = 1;
    final static int TCP_NET_DEVICE = 2;
    final static int RAW_TCP_NET_DEVICE = 3;
    final static int UNKNOW_NET_DEVICE = 0xff;
    protected String id;
    int netType;

    public NetDevice(String id){
        this.id = id;
        netType = UNKNOW_NET_DEVICE;
    }

    public String GetNetAddress(){
        return "";
    }

    public int SendBuffer(byte[] data){
        return  0;
    }

    public void Close(){

    }

    public void Stop(){

    }
}
