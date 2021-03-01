package com.example.nettytest.backend.backendcall;

import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.InviteReqPack;

public class BackEndCall extends CommonCall {

  
    public int callerWaitUpdateCount;
    public int calleeWaitUpdateCount;
    public int answerWaitUpdateCount;
    public int callType;

    public BackEndCall( String id,InviteReqPack pack){
        super(id,pack);

        callType = pack.callType;
        callerWaitUpdateCount = 0;
        calleeWaitUpdateCount = 0;
        answerWaitUpdateCount = 0;
    }

}
