package com.example.nettytest.backend.backendphone;

import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.userinterface.ServerDeviceInfo;

import java.util.ArrayList;

public class BackEndPhone extends PhoneDevice {

    public final int DEFAULT_REG_EXPIRE = 600;

    public int regExpire;
    public int regCount;
    public boolean enableListen;

    ArrayList<ConfigItem> paramList;

    public ServerDeviceInfo devInfo;

    public BackEndPhone(String id,int type){
        super();
        this.type = type;
        this.id = id;
        isReg = false;
        regCount = 0;
        regExpire = DEFAULT_REG_EXPIRE;
        paramList = new ArrayList<>();
        devInfo = new ServerDeviceInfo();
        enableListen = false;
    }

    public void UpdateRegStatus(int expire){
        regCount = 0;
        regExpire = expire;
        isReg = true;
    }


    public void IncreaseRegTick(){
        if(isReg){
            if(regExpire>0)
                regExpire--;
            else {
                isReg = false;
            }
        }
    }

    public void GetDeviceConfig(ArrayList<ConfigItem> list){
        int iTmp;
        for(iTmp=0;iTmp<paramList.size();iTmp++){
            ConfigItem item = new ConfigItem();
            item.Copy(paramList.get(iTmp));
            list.add(item);
        }
    }

    public void SetDeviceInfo(ServerDeviceInfo info){
        devInfo = info;
    }

    public void SetDeviceConfig(ArrayList<ConfigItem> list){
        paramList=list;
    }
}

