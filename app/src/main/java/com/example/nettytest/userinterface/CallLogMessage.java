package com.example.nettytest.userinterface;

public class CallLogMessage extends UserMessage{
    public String callId;
    public String areaId;
    
    public String callerNum;
    public String callerName;
    public int callerType;
    
    public String calleeNum;
    public String calleeName;
    public int calleeType;
    
    public String answerNum;
    public String answerName;
    public int answerType;
    
    public String enderNum;
    public String enderName;
    public int enderType;
    
    public int callDirection;
    public int callType;
    public int answerMode;

    public long startTime;
    public long answerTime;
    public long endTime;

    public CallLogMessage(){
        super();
        callId = "";
        areaId = "";

        callerNum = "";
        callerName = "";
        callerType = UserInterface.CALL_UNKNOW_DEVICE;

        calleeNum = "";
        calleeName = "";
        calleeType = UserInterface.CALL_UNKNOW_DEVICE;

        answerNum = "";
        answerName = "";
        answerType = UserInterface.CALL_UNKNOW_DEVICE;

        enderNum = "";
        enderName = "";
        enderType = UserInterface.CALL_UNKNOW_DEVICE;
        
        callType = UserInterface.CALL_NORMAL_TYPE;
        callDirection = UserInterface.CALL_DIRECTION_C2S;
        answerMode = UserInterface.CALL_ANSWER_MODE_HANDLE;
        
        startTime = 0;
        answerTime = 0;
        endTime = 0;
    }
}
