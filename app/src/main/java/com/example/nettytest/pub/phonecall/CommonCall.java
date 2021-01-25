package com.example.nettytest.pub.phonecall;

import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.userinterface.PhoneParam;

public class CommonCall {
    public final static int CALL_DIRECT_M2S = 1;
    public final static int CALL_DIRECT_S2M = 2;

    public final static int CALL_TYPE_NORMAL = 1;
    public final static int CALL_TYPE_EMERGENCY = 2;
    public final static int CALL_TYPE_BROADCAST = 3;

    public final static int CALL_STATE_DIALING = 1;
    public final static int CALL_STATE_RINGING = 2;
    public final static int CALL_STATE_INCOMING = 3;
    public final static int CALL_STATE_CONNECTED = 4;
    public final static int CALL_STATE_DISCONNECTED =5;

    public final static int UPDATE_INTERVAL = Transaction.TRANSCATION_RESEND_INTERVAL*2;

    public int type;
    public int direct;
    public int state;

    public String caller;
    public String callee;
    public String answer;

    public String devID;  //dev receive the call info

    public String callID;

    public int audioCodec;
    public int rtpTime;

    public CommonCall(String caller,String callee,int type){
        this.callee = callee;
        this.caller = caller;
        answer = "";
        this.type = type;
        this.state = CALL_STATE_DIALING;

        devID = caller;
        callID = UniqueIDManager.GetUniqueID(caller, UniqueIDManager.CALL_UNIQUE_ID);

        rtpTime = PhoneParam.CALL_RTP_PTIME;
        audioCodec = PhoneParam.CALL_RTP_CODEC;


    }

    public CommonCall(String id,InviteReqPack pack){
        callee = pack.callee;
        caller = pack.caller;
        type = pack.type;
        direct = pack.callDirect;

        answer = "";

        devID = id;
        state = CALL_STATE_INCOMING;
        callID = pack.callID;

        rtpTime = pack.pTime;
        audioCodec = pack.codec;

    }

}
