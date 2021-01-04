package com.example.nettytest.backend.backendphone;

import com.example.nettytest.pub.commondevice.PhoneDevice;

public class BackEndPhone extends PhoneDevice {

    public final int DEFAULT_REG_EXPIRE = 600;

    public int regExpire;
    public int regCount;

    public BackEndPhone(String id,int type){
        this.type = type;
        this.id = id;
        isReg = false;
        regCount = 0;
        regExpire = DEFAULT_REG_EXPIRE;
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

}
