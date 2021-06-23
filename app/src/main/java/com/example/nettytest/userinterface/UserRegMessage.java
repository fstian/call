package com.example.nettytest.userinterface;

public class UserRegMessage extends UserMessage {
    public boolean isReg;
    public int regExpire;
    public String areaId;
    public String areaName;
    public String transferAreaId;
    public boolean enableListenCall;

    public UserRegMessage(){
        super();
        areaId = "";
        transferAreaId = "";
        areaName = "";
        enableListenCall = false;
    }
}
