package com.example.nettytest.pub.phonemsg;

public class RegReqMsg extends PhoneMsg {
    public int status;
    public int regExpire;
    public RegReqMsg(int type,String sender,String receiver){
        super(type,sender,receiver);
    }
}
