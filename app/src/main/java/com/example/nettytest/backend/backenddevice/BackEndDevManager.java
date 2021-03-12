package com.example.nettytest.backend.backenddevice;

import com.example.nettytest.pub.LogWork;
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
                LogWork.Print(LogWork.BACKEND_DEVICE_MODULE,LogWork.LOG_INFO,"Add Net Device %s On Server",id);
            }else{
                LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_INFO,"Add Net Device %s On Server, but it had created",id);
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
                LogWork.Print(LogWork.BACKEND_DEVICE_MODULE,LogWork.LOG_INFO,"Remove Net Device %s On Server",id);
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
