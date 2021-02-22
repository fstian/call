package com.example.nettytest.backend.callserver;

import com.example.nettytest.backend.servernet.NettyTestServer;
import com.example.nettytest.pub.HandlerMgr;

public class DemoServer {

    Thread serverThread;

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



    }

    public boolean AddBackEndPhone(String id,int type){
        return HandlerMgr.AddBackEndPhone(id,type);
    }

    public void StopServer(){
        serverThread.interrupt();
    }

}
