package com.example.nettytest.pub.commondevice;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class NetDeviceManager {
    protected final HashMap<String, NetDevice> devLists;
    DeviceSendDataThread sendThread;

    ArrayList<NetManagerMsg> netMassageList;

    private class NetSendMessage {
        NetDevice dev;
        byte[] data;

    }

    private class NetManagerMsg{
        int type;
        NetSendMessage msg;

        public NetManagerMsg(int type, NetSendMessage msg) {
            this.type = type;
            this.msg = msg;
        }
    }


    private int AddNetSendMessage(int type, NetSendMessage msg) {
        synchronized(netMassageList) {
            NetManagerMsg netMsg = new NetManagerMsg(type,msg);
            netMassageList.add(netMsg);
            netMassageList.notify();
        }
        return 0;
    }

    private class DeviceSendDataThread extends Thread{
        ArrayList<NetManagerMsg> localMsgList;

        @Override
        public void run() {
            NetManagerMsg  msg;
            String test = "aaaa\r\n";
            String[] splitData = test.split("\r\n");

            while(!isInterrupted()) {
                synchronized(netMassageList) {
                    try {
                        netMassageList.wait();
                        while(netMassageList.size()>0) {
                            msg = netMassageList.remove(0);
                            localMsgList.add(msg);
                        }
                    } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                while(localMsgList.size()>0) {
                    msg = localMsgList.remove(0);
                    int type = msg.type;
                    if(type == 1){
                        NetSendMessage sendMsg = (NetSendMessage)msg.msg;
                        sendMsg.dev.SendBuffer(sendMsg.data);
                    }
                }
            }

        }

        public DeviceSendDataThread(){
            super("UdpDeviceSendThread");
            localMsgList = new ArrayList<NetManagerMsg>();
        }
    }

    public NetDeviceManager(){
        devLists = new HashMap<>();
        sendThread = new DeviceSendDataThread();
        sendThread.start();
        netMassageList = new ArrayList<>();
    }


    public void DevSendBuf(String id, ByteBuf buf){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if(matchedDev!=null){
                if(matchedDev.netType ==NetDevice.UDP_NET_DEVICE||
                   matchedDev.netType == NetDevice.RAW_TCP_NET_DEVICE) {
                    NetDevice dev = matchedDev;
                    byte[] data = new byte[buf.readableBytes()];
                    int readerIndex = buf.readerIndex();
                    buf.getBytes(readerIndex,data);
                    NetSendMessage sendMsg = new NetSendMessage();
                    sendMsg.data = data;
                    sendMsg.dev = dev;
                    AddNetSendMessage(1,sendMsg);

                }else if(matchedDev.netType ==NetDevice.TCP_NET_DEVICE) {
                    TcpNetDevice dev = (TcpNetDevice)matchedDev;
                    dev.SendBuffer(buf);
//                }else if(matchedDev.netType == NetDevice.RAW_TCP_NET_DEVICE){
//                    RawTcpNetDevice dev = (RawTcpNetDevice)matchedDev;
//                    byte[] bytebuf = new byte[buf.readableBytes()];
//                    buf.readBytes(bytebuf);
//                    dev.SendBuffer(bytebuf);
                }
            }
        }
    }

    public void UpdateDevChannel(String id, Channel ch){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if (matchedDev != null) {
                if(matchedDev.netType == NetDevice.TCP_NET_DEVICE) {
                    TcpNetDevice dev = (TcpNetDevice)matchedDev;

                    dev.UpdateChannel(ch);
                }
            }
        }
    }

    public void UpdateDevSocket(String id, DatagramSocket ssocket, InetAddress hostAddress, int port){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if (matchedDev != null) {
                if(matchedDev.netType == NetDevice.UDP_NET_DEVICE) {
                    UdpNetDevice dev = (UdpNetDevice)matchedDev;
                    dev.UpdatePeerAddress(ssocket,hostAddress, port);
                }
            }
        }

    }
}
