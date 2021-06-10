package com.example.nettytest.pub.commondevice;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class NetDeviceManager {
        protected final HashMap<String, NetDevice> devLists;
        UdpDeviceSendDataThread udpSendThread;
        
        ArrayList<NetManagerMsg> udpNetMassageList;

        private class UdpSendMessage{
                UdpNetDevice dev;
                byte[] data;
        }

        private class NetManagerMsg{
        	int type;
        	UdpSendMessage msg;
        	
        	public NetManagerMsg(int type,UdpSendMessage msg) {
        		this.type = type;
        		this.msg = msg;
        	}
        }
        
        
        private int AddUdpNetMessage(int type,UdpSendMessage msg) {
        	synchronized(udpNetMassageList) {
        		NetManagerMsg netMsg = new NetManagerMsg(type,msg);
        		udpNetMassageList.add(netMsg);
        		udpNetMassageList.notify();
        	}
        	return 0;
        }

        private class UdpDeviceSendDataThread extends Thread{
        	ArrayList<NetManagerMsg> localMsgList;
                public UdpDeviceSendDataThread(){
                    super("UdpDeviceSendThread");
                localMsgList = new ArrayList<NetManagerMsg>();
                }
                
                @Override
                public void run() {
            	NetManagerMsg  msg;
            	while(!isInterrupted()) {
	            	synchronized(udpNetMassageList) {
	            		try {
							udpNetMassageList.wait();
		            		while(udpNetMassageList.size()>0) {
		            			msg = udpNetMassageList.remove(0);
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
	                        UdpSendMessage sendMsg = (UdpSendMessage)msg.msg;
                                        sendMsg.dev.SendBuffer(sendMsg.data);
                                }
	            	}
            	}

                }
        }

        public NetDeviceManager(){
                devLists = new HashMap<>();
                udpSendThread = new UdpDeviceSendDataThread();
                udpSendThread.start();
                udpNetMassageList = new ArrayList<>();
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
                                                UdpSendMessage sendMsg = new UdpSendMessage();
                                                sendMsg.data = data;
                                                sendMsg.dev = dev;
		                AddUdpNetMessage(1,sendMsg);
				                
					}else if(matchedDev.netType ==NetDevice.TCP_NET_DEVICE) {
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
