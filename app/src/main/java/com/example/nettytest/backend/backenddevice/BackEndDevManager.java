package com.example.nettytest.backend.backenddevice;

import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.commondevice.NetDevice;
import com.example.nettytest.pub.commondevice.NetDeviceManager;
import com.example.nettytest.userinterface.PhoneParam;

import java.util.Iterator;
import java.util.Map;

public class BackEndDevManager extends NetDeviceManager {

    public void AddDevice(String id,int netMode){
        NetDevice matchedDev;

        synchronized (NetDeviceManager.class) {
            matchedDev = (NetDevice)devLists.get(id);
            if (matchedDev == null) {
                if(netMode == PhoneParam.TCP_PROTOCOL)
                    matchedDev = new BackEndTcpDevice(id);
                else if(netMode == PhoneParam.UDP_PROTOCOL)
                    matchedDev = new BackEndUdpDevice(id);
                devLists.put(id,matchedDev);
                LogWork.Print(LogWork.BACKEND_DEVICE_MODULE,LogWork.LOG_INFO,"Add Net Device %s On Server",id);
            }else{
//                LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_INFO,"Add Net Device %s On Server, but it had created",id);
            }
        }
    }

    public String GetDeviceAddress(String id){
        String address = "";
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class) {
            matchedDev = devLists.get(id);
            if(matchedDev!=null){
                address = matchedDev.GetNetAddress();
            }
        }
        return address;
    }

    public void RemoveDevice(String id){
        NetDevice matchedDev;
        synchronized (NetDeviceManager.class){
            matchedDev = (NetDevice)devLists.get(id);
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
