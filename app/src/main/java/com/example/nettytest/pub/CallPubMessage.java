package com.example.nettytest.pub;

public class CallPubMessage {
    public int arg1;
    public Object obj;

    public CallPubMessage(){
        arg1 = 0;
        obj = null;
    }

    public CallPubMessage(int type,Object obj){
        arg1 = type;
        this.obj = obj;
    }
}
