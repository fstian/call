package com.example.nettytest.pub;

import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.HandlerMgr;

import java.util.ArrayList;

public class MsgReceiver {
    static protected ArrayList<CallPubMessage> msgList=null;

    static protected ArrayList<CallPubMessage> backEndMsgList=null;

    public static int CreateTerminalMsgList(){
        if(msgList==null){
            msgList=new ArrayList<>();
            HandlerMgr.SetTerminalMessageHandler(msgList);
        }
        return 0;
    }

    public static ArrayList<CallPubMessage> GetTerminalMsgs(){
        ArrayList<CallPubMessage> msgs = new ArrayList<>();
        CallPubMessage msg;
        synchronized (msgList){
            try {
                msgList.wait();
                while (msgList.size() > 0) {
                    msg = msgList.remove(0);
                    msgs.add(msg);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return msgs;
    }

    public static int CreateBackEndMsgList(){
        if(backEndMsgList==null){
            backEndMsgList=new ArrayList<>();
            HandlerMgr.SetBackEndMessageHandler(backEndMsgList);
        }
        return 0;
    }

    public static ArrayList<CallPubMessage> GetBackEndMsgs(){
        ArrayList<CallPubMessage> msgs = new ArrayList<>();
        CallPubMessage msg;
        synchronized (backEndMsgList){
            try {
                backEndMsgList.wait();
                while (backEndMsgList.size() > 0) {
                    msg = backEndMsgList.remove(0);
                    msgs.add(msg);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return msgs;
    }
}

