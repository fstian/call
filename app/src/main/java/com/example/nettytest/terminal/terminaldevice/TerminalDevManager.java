package com.example.nettytest.terminal.terminaldevice;

import com.example.nettytest.pub.commondevice.NetDevice;
import com.example.nettytest.pub.commondevice.NetDeviceManager;
import com.example.nettytest.userinterface.PhoneParam;

public class TerminalDevManager extends NetDeviceManager {

    public TerminalDevManager(){
        super();
    }

    public void AddDevice(String id,int netMode){
        NetDevice matchedDev;

        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if(matchedDev==null) {
                if(netMode==PhoneParam.TCP_PROTOCOL) {
                    TerminalTcpDevice dev = new TerminalTcpDevice(id);
                    devLists.put(id, dev);
                    dev.Start();
                }else if(netMode == PhoneParam.UDP_PROTOCOL){
                    TerminalUdpDevice dev = new TerminalUdpDevice(id);
                    devLists.put(id,dev);
                }
            }
        }

    }


    public void RemovePhone(String id){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if(matchedDev!=null){
                matchedDev.Stop();
                matchedDev.Close();
                devLists.remove(id);
            }
        }
    }

}
