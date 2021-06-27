package com.example.nettytest.pub.commondevice;



import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.userinterface.PhoneParam;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class RawTcpNetDevice extends NetDevice {
    protected SocketChannel sc;

    public RawTcpNetDevice(String id){
        super(id);
        netType = RAW_TCP_NET_DEVICE;
    }

    public int SendBuffer(byte[] data){
        if(sc.isConnected()){
            try {
                sc.write(ByteBuffer.wrap(data));
                LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Raw-TCP dev %s Send data succ",id);
            } catch (IOException e) {
                LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Raw-TCP dev %s Send data fail for %s",id,e.getMessage());
                e.printStackTrace();
            }
        }else{
            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Raw-TCP dev %s could't send Data for disconnected",id);
        }
        return 0;
    }

    @Override
    public void Close() {
        super.Close();
        try {
            if(sc.isOpen()) {
                sc.close();
                LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Raw-TCP Close Dev %s",id);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

