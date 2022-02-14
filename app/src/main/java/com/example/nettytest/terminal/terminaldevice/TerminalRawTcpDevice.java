package com.example.nettytest.terminal.terminaldevice;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.commondevice.RawTcpNetDevice;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.userinterface.PhoneParam;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class TerminalRawTcpDevice extends RawTcpNetDevice {

    Selector selector;
    String remainData = "";

    public TerminalRawTcpDevice(String id){
        super(id);
        {
            try {
                selector = Selector.open();
            } catch (IOException e) {
                e.printStackTrace();
            }
            new Thread("client_"+id){
                @Override
                public void run() {
                    boolean isReset = true;
                    boolean isPrintError = true;
                    while(!isInterrupted()){
                        try {
                            if(isReset) {
                                sc = SocketChannel.open();
                                sc.configureBlocking(false);
                                sc.register(selector, SelectionKey.OP_CONNECT);
                                LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Raw-TCP Dev %s Begine Use Local Port %d Connecting to %s:%d",id,sc.socket().getLocalPort(),PhoneParam.callServerAddress,PhoneParam.callClientPort);
                                sc.connect(new InetSocketAddress(PhoneParam.callServerAddress,PhoneParam.callClientPort));
                                isReset = false;
                            }
                            int events = selector.select();
                            if(events>0){
                                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                                while(keys.hasNext()){
                                    SelectionKey key = keys.next();
                                    keys.remove();
                                    if(key.isConnectable()){
                                        SocketChannel chnnl = (SocketChannel)key.channel();
                                        if(chnnl.isConnectionPending()) {
                                            chnnl.finishConnect();
                                        }

                                        chnnl.configureBlocking(false);
                                        chnnl.register(selector,SelectionKey.OP_READ);
                                        if(chnnl.isConnected()) {
//                                            sc.socket().setReuseAddress(true);
//                                            sc.socket().setSoTimeout(2000);
                                            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Raw-TCP Client %s connecte to server success with local port %d", id,chnnl.socket().getLocalPort());
                                            isPrintError = true;
                                        }else {
                                            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Raw-TCP Client %s connecte to server Fail", id);
                                        }
                                    }else if(key.isReadable()){
                                        SocketChannel channel = (SocketChannel)key.channel();
                                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                                        int readRtn = channel.read(buffer);
                                        if(readRtn<=0) {
                                            isReset = true;
                                            LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Raw-TCP Client %s Read Return %d, Reset Socket",id,readRtn);
                                        }else {                                     
                                        ((Buffer)buffer).flip();
                                        byte[] bytes = new byte[buffer.limit()-buffer.position()];
                                        buffer.get(bytes);
                                        String data = new String(bytes,"UTF-8");
                                        if(!remainData.isEmpty()){
                                            data = remainData+data;
                                            remainData = "";
                                        }
                                        String[] jsonData = data.split("\r\n");
                                        for(int iTmp = 0;iTmp<jsonData.length;iTmp++) {
                                            String jsonString = jsonData[iTmp];
                                            if(jsonString.length()<1)
                                                continue;
                                            if(iTmp==jsonData.length-1){
                                                String lastString = jsonString.substring(jsonString.length()-1);
                                                byte[] lastbytes = lastString.getBytes();
                                                if(lastbytes[0]!=0x7d&&lastbytes[0]!=0x0d&&lastbytes[0]!=0x0a){
                                                    remainData += jsonString;
                                                    LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_ERROR,"Raw-TCP Client %s save Temp Data %s",id,jsonString);
                                                    break;
                                                }
                                            }
                                            ProtocolPacket packet = ProtocolFactory.ParseData(jsonString);
                                            if (packet != null) {
                                                LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_DEBUG, "Raw-TCP Client %s Read %s Packet From Raw-TCP", id, ProtocolPacket.GetTypeName(packet.type));
                                                HandlerMgr.PhoneProcessPacket(packet);
                                            } else {
                                                LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_ERROR, "Raw-TCP Client %s Recv %s", id, new String(bytes, "UTF-8"));
                                            }
                                        }
                                    }
                                }
                            }
                            }else{
                                LogWork.Print(LogWork.TERMINAL_NET_MODULE,LogWork.LOG_DEBUG,"Raw-TCP Dev %s Select Fail, Return is %d",id,events);
                            }
                        } catch (IOException e) {
//                            e.printStackTrace();
                            if(isPrintError) {
//                                LogWork.Print(LogWork.TERMINAL_NET_MODULE, LogWork.LOG_ERROR, "Raw-TCP Client %s Tcp Error in Recv with Msg, Reset the TCP connection", id, e.getMessage());
                                isPrintError = false;
                            }
                            isReset = true;
                        }
                        if(isReset){
                            try {
                                sc.socket().close();
                                sc.close();
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }.start();
        }

    }

    @Override
    public void Close() {
        super.Close();
    }


}

