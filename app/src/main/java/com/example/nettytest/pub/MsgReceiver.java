package com.example.nettytest.pub;

import java.util.ArrayList;

public class MsgReceiver {

    final ArrayList<CallPubMessage> comMsgList;

    public MsgReceiver(String name){
        comMsgList = new ArrayList<>();
        new Thread(name){
            @Override
            public void run() {
                ArrayList<CallPubMessage> newMsgList = new ArrayList<>();
                CallPubMessage msg;
                while(!isInterrupted()){
                    synchronized (comMsgList){
                        try {
                            newMsgList.clear();
                            comMsgList.wait();
                            while (comMsgList.size()>0){
                                msg = comMsgList.remove(0);
                                newMsgList.add(msg);
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }
                    }
                    CallPubMessageRecv(newMsgList);
                    newMsgList.clear();
                }

            }
        }.start();
    }

    public void CallPubMessageRecv(ArrayList<CallPubMessage> list){

    }

    public boolean AddMessage(int type, Object obj){
        CallPubMessage msg = new CallPubMessage(type,obj);
        synchronized (comMsgList){
            comMsgList.add(msg);
            comMsgList.notify();
        }
        return true;
    }
    public boolean AddMessage(CallPubMessage msg){
        synchronized (comMsgList){
            comMsgList.add(msg);
            comMsgList.notify();
        }
        return true;
    }
}

