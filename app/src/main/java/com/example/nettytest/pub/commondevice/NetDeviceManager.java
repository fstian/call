package com.example.nettytest.pub.commondevice;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class NetDeviceManager {
        protected final HashMap<String, NetDevice> devLists;
        UdpDeviceSendDataThread udpSendThread;
        Handler udpSendHandler= null;

        private class UdpSendMessage{
                UdpNetDevice dev;
                byte[] data;
        }

        private class UdpDeviceSendDataThread extends Thread{
                @Override
                public void run() {
                        Looper.prepare();
                        udpSendHandler = new Handler(msg->{
                                int type = msg.arg1;
                                if(type == 1){
                                        UdpSendMessage sendMsg = (UdpSendMessage)msg.obj;
                                        sendMsg.dev.SendBuffer(sendMsg.data);
                                }
                                return  false;
                        });
                        Looper.loop();

                }
        }

        public NetDeviceManager(){
                devLists = new HashMap<>();
                udpSendThread = new UdpDeviceSendDataThread();
                udpSendThread.start();
        }


        public void DevSendBuf(String id, ByteBuf buf){
                NetDevice matchedDev;
                synchronized (NetDeviceManager.class) {
                        matchedDev = devLists.get(id);
                        if(matchedDev!=null){
                                if(matchedDev.netType ==NetDevice.UDP_NET_DEVICE) {
                                        UdpNetDevice dev = (UdpNetDevice)matchedDev;
                                        byte[] data = new byte[buf.readableBytes()];
                                        int readerIndex = buf.readerIndex();
                                        buf.getBytes(readerIndex,data);
                                        if(udpSendHandler!=null) {
                                                Message msg = udpSendHandler.obtainMessage();
                                                UdpSendMessage sendMsg = new UdpSendMessage();
                                                sendMsg.data = data;
                                                sendMsg.dev = dev;
                                                msg.arg1 = 1;
                                                msg.obj = sendMsg;
                                                udpSendHandler.sendMessage(msg);
                                        }
                                }
                                else if(matchedDev.netType ==NetDevice.TCP_NET_DEVICE) {
                                        TcpNetDevice dev = (TcpNetDevice)matchedDev;
                                        dev.SendBuffer(buf);
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
