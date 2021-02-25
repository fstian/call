package com.example.nettytest.backend.backendcall;

import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.InviteReqPack;

public class BackEndCall extends CommonCall {

    public int inviterRtpPort;
    public String inviterRtpAddress;
    public int answerRtpPort;
    public String answerRtpAddress;

    public int callerWaitUpdateCount;
    public int calleeWaitUpdateCount;
    public int answerWaitUpdateCount;
    public int callType;

    public BackEndCall( String id,InviteReqPack pack){
        super(id,pack);

        inviterRtpAddress = pack.callerRtpIP;
        inviterRtpPort = pack.callerRtpPort;
        callType = pack.callType;
        answerRtpAddress = "";
        answerRtpPort = 0;
        callerWaitUpdateCount = 0;
        calleeWaitUpdateCount = 0;
        answerWaitUpdateCount = 0;
    }

}
