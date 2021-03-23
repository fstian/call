package com.example.nettytest.terminal.terminaldevice;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.commondevice.UdpNetDevice;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.userinterface.PhoneParam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TerminalUdpDevice extends UdpNetDevice {

    TerminalUdpReadThread readThread=null;

    private class TerminalUdpReadThread extends Thread{
        @Override
        public void run() {
            byte[] recvBuf = new byte[4096];
            while(!isInterrupted()){
                DatagramPacket pack = new DatagramPacket(recvBuf,recvBuf.length);
                if(!localSocket.isClosed()){
                    try {
                        localSocket.receive(pack);
                        ProtocolPacket packet = ProtocolFactory.ParseData(pack.getData());
                        HandlerMgr.PhoneProcessPacket(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    public TerminalUdpDevice(String id){
        super(id);
        try {
            DatagramSocket socket = new DatagramSocket();
            UpdatePeerAddress(socket, InetAddress.getByName(PhoneParam.callServerAddress),PhoneParam.callServerPort);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void UpdatePeerAddress(DatagramSocket socket, InetAddress address, int port) {
        super.UpdatePeerAddress(socket, address, port);
        if(readThread!=null){
            readThread.interrupt();
        }
        readThread = new TerminalUdpReadThread();
        readThread.start();
    }
}
