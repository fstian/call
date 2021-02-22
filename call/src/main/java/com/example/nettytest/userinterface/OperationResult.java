package com.example.nettytest.userinterface;

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

    public OperationResult(int status){
        if(status == ProtocolPacket.STATUS_OK){
            result = OP_RESULT_OK;
            reason = FailReason.FAIL_REASON_NO;
        }else{
            result = OP_RESULT_FAIL;
            switch(status){
                case ProtocolPacket.STATUS_BUSY:
                    reason = FailReason.FAIL_REASON_BUSY;
                    break;
                case ProtocolPacket.STATUS_CONFILICT:
                    reason = FailReason.FAIL_REASON_CONFILICT;
                    break;
                case ProtocolPacket.STATUS_DECLINE:
                    reason = FailReason.FAIL_REASON_NOTSUPPORT;
                    break;
                case ProtocolPacket.STATUS_FORBID:
                    reason = FailReason.FAIL_REASON_FORBID;
                    break;
                case ProtocolPacket.STATUS_NOTFOUND:
                    reason = FailReason.FAIL_REASON_NOTFOUND;
                    break;
                case ProtocolPacket.STATUS_TIMEOVER:
                    reason = FailReason.FAIL_REASON_TIMEOVER;
                    break;
                default:
                    reason = FailReason.FAIL_REASON_UNKNOW;
                    break;
            }
        }
        callID = "";
    }
}
