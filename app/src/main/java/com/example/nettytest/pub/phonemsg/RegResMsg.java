package com.example.nettytest.pub.phonemsg;

public class RegResMsg extends PhoneMsg{
    public int status;
    public String result;

    public RegResMsg(int type, String sender, String receiver) {
        super(type, sender, receiver);
    }
}
