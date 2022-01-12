package com.example.nettytest.backend.backendcall;

import com.example.nettytest.pub.CallLogSaver;
import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.MsgReceiver;
import com.example.nettytest.userinterface.CallLogMessage;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserMessage;

import java.util.ArrayList;

public class BackEndMsgReceiver extends MsgReceiver{
    CallLogSaver logSaver = null;
    
    public BackEndMsgReceiver() {
        super("JavaMsgReceiver");
        if(PhoneParam.serviceActive) {
            logSaver = new CallLogSaver();            
            logSaver.StartCallLogServer(PhoneParam.serviceAddress, PhoneParam.logPort);
        }
    }
    
    @Override
    public void CallPubMessageRecv(ArrayList<CallPubMessage> list){
        CallPubMessage msg;
        int type;
        UserMessage userMsg;
        
        while(list.size()>0) {
            msg = list.remove(0);
            
            type = msg.arg1;
            userMsg =(UserMessage) msg.obj;
            if(type == UserMessage.MESSAGE_BACKEND_CALL_LOG) {
                if(logSaver!=null) {
                    CallLogMessage log = (CallLogMessage)userMsg;
                    logSaver.AddCallLog(log);
                }
            }
        }
        
    }
       
}
