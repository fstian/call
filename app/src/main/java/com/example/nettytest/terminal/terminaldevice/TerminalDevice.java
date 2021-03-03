package com.example.nettytest.terminal.terminaldevice;

import com.example.nettytest.pub.commondevice.NetDevice;
import com.example.nettytest.terminal.clientnet.NettyTestClient;
import com.example.nettytest.userinterface.PhoneParam;

public class TerminalDevice extends NetDevice {
    NettyTestClient client;
    boolean isActive;

    public TerminalDevice(String id){
        super(id);
        isActive = true;
    }

    public void Start(){
//        client = new NettyTestClient(id,PhoneParam.CALL_SERVER_PORT);
        client = new NettyTestClient(id,PhoneParam.callServerAddress,PhoneParam.callServerPort);
        new Thread(){
            @Override
            public void run() {
                while(isActive) {
                    client.run();
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }.start();
    }

    public void Stop(){
        isActive = false;
    }
}
