package com.example.nettytest.userinterface;

import com.example.nettytest.userinterface.UserInterface;

public class CallLogMessage extends UserMessage{
    public String callId;
    public String caller;
    public String callee;
    public String answer;
    public String ender;

    public int callType;

    public long startTime;
    public long answerTime;
    public long endTime;

    public CallLogMessage(){
        super();
        callId = "";
        caller = "";
        callee = "";
        answer = "";
        ender = "";
        callType = UserInterface.CALL_NORMAL_TYPE;
        startTime = 0;
        answerTime = 0;
        endTime = 0;
    }
}
