package com.example.nettytest.pub.commondevice;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpNetDevice extends NetDevice {

    protected InetAddress peerAddress;
    protected int peerPort = 0;
    protected DatagramSocket localSocket=null;

    public UdpNetDevice(String id){
        super(id);
        netType = UDP_NET_DEVICE;
        peerAddress = null;
    }

    @Override
    public String GetNetAddress() {
        String address="";
        if(peerAddress!=null)
            address = peerAddress.getHostAddress();
        return address;
    }

    public int SendBuffer(byte[] data){
        if(localSocket!=null) {
            if (!localSocket.isClosed()) {
                if(peerAddress!=null){
                    DatagramPacket pack = new DatagramPacket(data, data.length, peerAddress, peerPort);
                    try {
                        localSocket.send(pack);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return 0;
    }

    public void UpdatePeerAddress(DatagramSocket socket,InetAddress address,int port){
        localSocket = socket;
        peerAddress = address;
        peerPort = port;

    }

}
