package com.example.nettytest.pub.result;

import com.example.nettytest.pub.protocol.ProtocolPacket;

public class    OperationResult {
    public final static int OP_RESULT_OK = 1;
    public final static int OP_RESULT_FAIL = -1;


    public int result;
    public int reason;
    public String callID;

    public OperationResult(){
        result = OP_RESULT_OK;
        reason = FailReason.FAIL_REASON_NO;
        callID = "";
    }

    static public int GetUserFailReason(int status){
        int localReason;
        switch(status){
            case ProtocolPacket.STATUS_BUSY:
                localReason = FailReason.FAIL_REASON_BUSY;
                break;
            case ProtocolPacket.STATUS_CONFILICT:
                localReason = FailReason.FAIL_REASON_CONFLICT;
                break;
            case ProtocolPacket.STATUS_DECLINE:
                localReason = FailReason.FAIL_REASON_NOTSUPPORT;
                break;
            case ProtocolPacket.STATUS_FORBID:
                localReason = FailReason.FAIL_REASON_FORBID;
                break;
            case ProtocolPacket.STATUS_NOTFOUND:
                localReason = FailReason.FAIL_REASON_NOTFOUND;
                break;
            case ProtocolPacket.STATUS_TIMEOVER:
                localReason = FailReason.FAIL_REASON_TIMEOVER;
                break;
            case ProtocolPacket.STATUS_OK:
                localReason = FailReason.FAIL_REASON_NO;
                break;
            default:
                localReason = FailReason.FAIL_REASON_UNKNOW;
                break;
        }
        return localReason;
    }

    public OperationResult(int status){
        if(status == ProtocolPacket.STATUS_OK){
            result = OP_RESULT_OK;
            reason = FailReason.FAIL_REASON_NO;
        }else{
            result = OP_RESULT_FAIL;
            reason = GetUserFailReason(status);
        }
        callID = "";
    }
}
