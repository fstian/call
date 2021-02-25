package com.example.nettytest.terminal.terminaldevice;

import com.example.nettytest.pub.commondevice.NetDeviceManager;

public class TerminalDevManager extends NetDeviceManager {

    public TerminalDevManager(){
        super();
    }

    public void AddDevice(String id, TerminalDevice device){
        TerminalDevice matchedDev;

        synchronized (NetDeviceManager.class) {
            matchedDev = (TerminalDevice)devLists.get(id);
            if(matchedDev==null)
                devLists.put(id,device);
        }

    }




}
