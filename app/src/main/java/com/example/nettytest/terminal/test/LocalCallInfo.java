package com.example.nettytest.terminal.test;

public class LocalCallInfo {

    public final static int LOCAL_CALL_STATUS_OUTGOING = 1;
    public final static int LOCAL_CALL_STATUS_RINGING = 2;
    public final static int LOCAL_CALL_STATUS_CONNECTED = 3;
    public final static int LOCAL_CALL_STATUS_INCOMING = 4;
    public final static int LOCAL_CALL_STATUS_DISCONNECT = 5;
    public String caller;
    public String callee;
    public String answer;

    public int status;
    public String callID;

    public LocalCallInfo(){
        caller = "";
        callee = "";
        answer = "";
        callID = "";
        status = LOCAL_CALL_STATUS_DISCONNECT;
    }
}
