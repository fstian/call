package com.example.nettytest.terminal.terminaldevice;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
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

    static TerminalUdpReadThread readThread=null;
    static DatagramSocket recvSocket=null;

    private class TerminalUdpReadThread extends Thread{
        public TerminalUdpReadThread(){
            super("TerminalUdpReadThread");
        }
        
        @Override
        public void run() {
            byte[] recvBuf = new byte[4096];
            while(!isInterrupted()){
                java.util.Arrays.fill(recvBuf,(byte)0);
                DatagramPacket pack = new DatagramPacket(recvBuf,recvBuf.length);
                if(!localSocket.isClosed()){
                    try {
                        localSocket.receive(pack);
                        if(pack.getLength()>0) {
                            ProtocolPacket packet = ProtocolFactory.ParseData(pack.getData());
                            if(packet!=null){
                                LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_DEBUG, "Terminal %s Recv Data from %s:%d, type is %s", packet.receiver ,pack.getAddress().getHostAddress(), pack.getPort(),ProtocolPacket.GetTypeName(packet.type));
                                HandlerMgr.PhoneProcessPacket(packet);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }catch(Exception ee){
                        LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Socket of UDP of Dev %s err with %s",id,ee.getMessage());
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
            if(recvSocket==null)
                recvSocket = new DatagramSocket();
            UpdatePeerAddress(recvSocket, InetAddress.getByName(PhoneParam.callServerAddress),PhoneParam.callClientPort);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void UpdatePeerAddress(DatagramSocket socket, InetAddress address, int port) {
        LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Terminal Update Peer %s:%d for Dev %s",address.getHostAddress(),port,id);
        super.UpdatePeerAddress(socket, address, port);
        if(readThread==null){
            readThread = new TerminalUdpReadThread();
            readThread.start();
        }
    }

    @Override
    public void Close() {
        super.Close();
//        if(readThread!=null){
//            readThread.interrupt();
//        }
//        if(localSocket!=null){
//            if(!localSocket.isClosed()){
//                localSocket.close();
//            }
//        }
    }

    @Override
    public int SendBuffer(byte[] data) {
        LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Terminal %s Send Data to %s:%d",id,peerAddress.getHostAddress(),peerPort);
        return super.SendBuffer(data);
    }
}
