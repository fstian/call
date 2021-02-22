package com.example.nettytest.userinterface;

public class UserCallMessage extends UserMessage {
    public String callId;
    public String callerId;
    public String calleeId;
    public String operaterId;

    public UserCallMessage(){
        type = CALL_MESSAGE_UNKONWQ;
        reason = FailReason.FAIL_REASON_NO;
        devId = "";
        callId = "";
        callerId = "";
        calleeId = "";
        operaterId = "";
    }

}
