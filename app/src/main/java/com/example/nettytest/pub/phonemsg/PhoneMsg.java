package com.example.nettytest.pub.phonemsg;

public class PhoneMsg {
    public int msgType;
    public String sender;
    public String receiver;

    public PhoneMsg(int type,String sender,String receiver){
        msgType = type;
        this.sender = sender;
        this.receiver = receiver;
    }
}
