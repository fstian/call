package com.example.nettytest.backend.callserver;

import com.example.nettytest.backend.servernet.NettyTestServer;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.userinterface.PhoneParam;


public class DemoServer {

    Thread serverThread = null;
    UdpServer udpServer = null;

    public DemoServer(int port){
        
        serverThread = new Thread(){
            @Override
            public void run() {
                NettyTestServer testServer = new NettyTestServer(port);
                while(!isInterrupted()) {
                    try {
                        testServer.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        serverThread.start();

        udpServer = new UdpServer(port);
        udpServer.start();

    }

    public boolean AddBackEndPhone(String id,int type,int netMode){
        return HandlerMgr.AddBackEndPhone(id,type,netMode);
    }

    public void StopServer(){
        if(serverThread!=null)
            serverThread.interrupt();
        if(udpServer!=null)
            udpServer.interrupt();
    }

}
