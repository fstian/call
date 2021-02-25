package com.example.nettytest.userinterface;

import com.example.nettytest.pub.phonecall.CommonCall;

public class UserCallMessage extends UserMessage {

    public final int NORMAL_CALL_TYPE = 1;
    public final int EMERGENCY_CALL_TYPE = 2;
    public final int BROADCAST_CALL_TYPE = 3;

    public int callType;
    public String callId;

    public int callerType;
    public String callerId;
    public String calleeId;
    public String operaterId;

    public String deviceName;
    public String patientName;
    public int patientAge;
    public String bedName;
    public String roomId;

    public UserCallMessage(){
        super();
        type = CALL_MESSAGE_UNKONWQ;
        reason = FailReason.FAIL_REASON_NO;
        devId = "";

        callType = NORMAL_CALL_TYPE;
        callId = "";

        callerId = "";
        calleeId = "";
        operaterId = "";

        deviceName = "";
        patientName = "";
        patientAge = 18;
        bedName = "";
        roomId = "";
    }

}
