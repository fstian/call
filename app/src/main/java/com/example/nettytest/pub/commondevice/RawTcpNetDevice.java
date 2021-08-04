package com.example.nettytest.pub.commondevice;


import com.example.nettytest.pub.LogWork;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class RawTcpNetDevice extends NetDevice {
    protected SocketChannel sc;

    public RawTcpNetDevice(String id){
        super(id);
        netType = RAW_TCP_NET_DEVICE;
    }

    public int SendBuffer(byte[] data){
        try {
            if (sc.isConnected()) {
                try {
                    sc.write(ByteBuffer.wrap(data));
                    LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_DEBUG, "Raw-TCP dev %s Send data succ", id);
                } catch (IOException e) {
                    LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_ERROR, "Raw-TCP dev %s Send data fail with msg %s", id, e.getMessage());
                    e.printStackTrace();
                }
            } else {
                LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_DEBUG, "Raw-TCP dev %s could't send Data for disconnected", id);
            }
        }catch (Exception e){
            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_TEMP_DBG,"Raw-TCP dev %s Caught Exception %s when Send",e.getMessage());
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

