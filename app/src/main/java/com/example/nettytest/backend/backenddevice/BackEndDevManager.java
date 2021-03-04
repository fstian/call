package com.example.nettytest.backend.backenddevice;

import com.example.nettytest.pub.commondevice.NetDevice;
import com.example.nettytest.pub.commondevice.NetDeviceManager;

import java.util.Iterator;
import java.util.Map;

public class BackEndDevManager extends NetDeviceManager {

    public void AddDevice(String id){
        BackEndDevice matchedDev;

        synchronized (NetDeviceManager.class) {
            matchedDev = (BackEndDevice)devLists.get(id);
            if (matchedDev == null) {
                matchedDev = new BackEndDevice(id);
                devLists.put(id,matchedDev);
            }
        }
    }

    public void RemoveDevice(String id){
        BackEndDevice matchedDev;
        synchronized (NetDeviceManager.class){
            matchedDev = (BackEndDevice)devLists.get(id);
            if(matchedDev!=null) {
                matchedDev.Close();
                devLists.remove(id);
            }
        }
    }

    public void RemoveAllDevice(){
        synchronized (NetDeviceManager.class){
            for(Iterator<Map.Entry<String, NetDevice>> it = devLists.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, NetDevice>item = it.next();
                NetDevice phone = item.getValue();
                phone.Close();
                it.remove();
            }
        }
    }


}
