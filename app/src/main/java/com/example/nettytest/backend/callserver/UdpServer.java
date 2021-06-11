package com.example.nettytest.backend.callserver;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UdpServer extends Thread{

    int port;
    public UdpServer(int port){
        super("UdpServer");
        this.port = port;
    }

    @Override
    public void run(){
        byte[] recvBuf=new byte[4096];
        DatagramPacket recvPack;

        try {
            DatagramSocket udpServerSocket = new DatagramSocket(port);
            while(!isInterrupted()){
                java.util.Arrays.fill(recvBuf,(byte)0);
                recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                udpServerSocket.receive(recvPack);
                if(recvPack.getLength()>0){
                    LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_DEBUG,"Server Recv Data from %s:%d",recvPack.getAddress().getHostAddress(),recvPack.getPort());
                    ProtocolPacket packet = ProtocolFactory.ParseData(recvPack.getData());
                    if(packet!=null) {
                        HandlerMgr.UpdateBackEndDevSocket(packet.sender,udpServerSocket,recvPack.getAddress(),recvPack.getPort());
                        HandlerMgr.BackEndProcessPacket(packet);
                    }
                }
            }
            udpServerSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
