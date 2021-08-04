package com.androidport.port;

import android.os.Handler;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.MsgReceiver;

public class CallMsgReceiver {
    static Handler userMessageHandler = null;
    static Handler backendMessageHandler = null;

    static TerminalUserMsgReceiver msgReceiver = null;

    static public void SetMessageHandler(Handler h) {
        if (userMessageHandler == null) {
            userMessageHandler = h;
        }

        if(msgReceiver==null){
            msgReceiver = new TerminalUserMsgReceiver("TermUserMsgReceiver");
            msgReceiver.SetMessageHandler(h);
            HandlerMgr.SetTerminalUserMsgReceiver((MsgReceiver)msgReceiver );
        }
    }

    static public void SetBackEndMessageHandler(Handler h) {
        if (backendMessageHandler == null) {

            backendMessageHandler = h;
        }
    }
}
