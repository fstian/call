package com.example.nettytest.backend.callserver;

import com.example.nettytest.backend.servernet.NettyTestServer;
import com.example.nettytest.pub.HandlerMgr;


public class DemoServer {

    Thread serverThread;
    UdpServer udpServer;

    public DemoServer(final int port){
        
        serverThread = new Thread("DemoServer"){
            
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

    public int AddBackEndPhone(String id,int type,int netMode,String area){
        return HandlerMgr.AddBackEndPhone(id,type,netMode,area);
    }

    public void StopServer(){
        if(serverThread!=null)
            serverThread.interrupt();
        if(udpServer!=null)
            udpServer.interrupt();
    }

}
