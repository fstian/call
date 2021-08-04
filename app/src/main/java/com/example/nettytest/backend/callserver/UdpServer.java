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
                try{
                    udpServerSocket.receive(recvPack);
                    if(recvPack.getLength()>0){
                        ProtocolPacket packet = ProtocolFactory.ParseData(recvPack.getData());
                        if(packet!=null) {
                            LogWork.Print(LogWork.BACKEND_NET_MODULE,LogWork.LOG_DEBUG,"Server Recv Dev %s Data from %s:%d",packet.sender,recvPack.getAddress().getHostAddress(),recvPack.getPort());
                            HandlerMgr.UpdateBackEndDevSocket(packet.sender,udpServerSocket,recvPack.getAddress(),recvPack.getPort());
                            HandlerMgr.BackEndProcessPacket(packet);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }catch(Exception ee){
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Socket of Terminal Snap err with %s",ee.getMessage());
                }
            }
            udpServerSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
}
