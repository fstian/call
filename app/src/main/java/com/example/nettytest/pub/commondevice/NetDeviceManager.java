package com.example.nettytest.pub.commondevice;

import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.MsgReceiver;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;

public class NetDeviceManager {
    protected final HashMap<String, NetDevice> devLists;
    NetDeviceMgrMsgReciever msgReceiver;

    final private int SEND_DATA_MSG =1;

    private class NetDeviceMgrMsgReciever extends MsgReceiver{
        public NetDeviceMgrMsgReciever(String name){
            super(name);
        }

        @Override
        public void CallPubMessageRecv(ArrayList<CallPubMessage> list) {
            int type;
            CallPubMessage msg;
            synchronized (NetDeviceManager.class) {
                while(list.size()>0) {
                    msg = list.remove(0);
                    type = msg.arg1;
                    if(type == SEND_DATA_MSG){
                        NetSendMessage sendMsg = (NetSendMessage)msg.obj;
                        sendMsg.dev.SendBuffer(sendMsg.data);
                    }
                }
            }
        }
    }

    private static class NetSendMessage {
        NetDevice dev;
        byte[] data;

    }

    private void AddNetSendMessage(NetSendMessage msg) {
        msgReceiver.AddMessage(SEND_DATA_MSG,msg);
    }


    public NetDeviceManager(){
        devLists = new HashMap<>();
        msgReceiver = new NetDeviceMgrMsgReciever("NetDevMgrMsgReceiver");
    }

    public void DevSendBuf(String id, String data){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if(matchedDev!=null){
                if(matchedDev.netType ==NetDevice.UDP_NET_DEVICE||
                   matchedDev.netType == NetDevice.RAW_TCP_NET_DEVICE) {
                    byte[] bdata = data.getBytes(CharsetUtil.UTF_8);
                    NetSendMessage sendMsg = new NetSendMessage();
                    sendMsg.data = bdata;
                    sendMsg.dev = matchedDev;
                    // couldn't send data in Main Thread
                    AddNetSendMessage(sendMsg);
                }else if(matchedDev.netType ==NetDevice.TCP_NET_DEVICE){
                    TcpNetDevice dev = (TcpNetDevice)matchedDev;
                    byte[] bytes = data.getBytes(CharsetUtil.UTF_8);
                    ByteBuf buf = Unpooled.wrappedBuffer(bytes);
                    dev.SendBuffer(buf);
                }
            }
        }
    }

    public void DevSendBuf(String id, ByteBuf buf){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if(matchedDev!=null){
                if(matchedDev.netType ==NetDevice.UDP_NET_DEVICE||
                   matchedDev.netType == NetDevice.RAW_TCP_NET_DEVICE) {
                    byte[] data = new byte[buf.readableBytes()];
                    int readerIndex = buf.readerIndex();
                    buf.getBytes(readerIndex,data);
                    NetSendMessage sendMsg = new NetSendMessage();
                    sendMsg.data = data;
                    sendMsg.dev = matchedDev;
                    // couldn't send data in Main Thread
                    AddNetSendMessage(sendMsg);

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
