package com.example.nettytest.terminal.test;

import com.example.nettytest.userinterface.UserCallMessage;

public class LocalCallInfo {

    public final static int LOCAL_CALL_STATUS_OUTGOING = 1;
    public final static int LOCAL_CALL_STATUS_RINGING = 2;
    public final static int LOCAL_CALL_STATUS_INCOMING = 3;
    public final static int LOCAL_CALL_STATUS_CONNECTED = 4;
    public final static int LOCAL_CALL_STATUS_DISCONNECT = 5;
    public String caller;
    public String callee;
    public String answer;
    public int callType;

    public int status;
    public String callID;

    public LocalCallInfo(){
        callType = UserCallMessage.NORMAL_CALL_TYPE;
        caller = "";
        callee = "";
        answer = "";
        callID = "";
        status = LOCAL_CALL_STATUS_DISCONNECT;
    }

}
