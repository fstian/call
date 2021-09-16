package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.userinterface.PhoneParam;

public class EndReqPack extends ProtocolPacket{

    final static public int END_BY_CALLER = 2;
    final static public int END_BY_CALLEE = 3;
    final static public int END_BY_LISTENER = 4;
    final static public int END_BY_ANSWER = 5;
    final static public int END_BY_USER_CANCEL = 6;
    
    final static public int END_FOR_CALLEE_UPDATE_FAIL = 10;
    final static public int END_FOR_CALLER_UPDATE_FAIL = 11;
    final static public int END_FOR_ANSWER_UPDATE_FAIL = 12;
    final static public int END_FOR_NO_LISTEN = 13;
    final static public int END_FOR_OTHER_ANSWER = 14;
    final static public int END_FOR_CALLEE_REJECT = 15;
    final static public int END_FOR_INVITE_TIMEOVER = 16;

    final static public int END_FOR_SERVER_CANCEL = 17;

    final static public int END_FOR_UNKNOW = 100;
    
    public String callID;
    public String endDevID;
    public int endReason;

    public EndReqPack(){
        super();
        type = ProtocolPacket.END_REQ;
        callID = "";
        endDevID = "";
        endReason = END_FOR_UNKNOW;
    }

    public  EndReqPack(EndReqPack endReq,String devid){
        CopyData(endReq);
        receiver = devid;
        msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);
        callID = endReq.callID;
        endDevID = endReq.endDevID;
        endReason = endReq.endReason;
    }

    public EndReqPack(String callId){
        super();
        type = ProtocolPacket.END_REQ;
        sender = PhoneParam.CALL_SERVER_ID;
        receiver = PhoneParam.CALL_SERVER_ID;
        msgID = UniqueIDManager.GetUniqueID(PhoneParam.CALL_SERVER_ID,UniqueIDManager.MSG_UNIQUE_ID);

        this.callID = callId;
        endDevID = PhoneParam.CALL_SERVER_ID;
        endReason = END_FOR_UNKNOW;
    }

    public static String GetEndReasonName(int reason){
        String reasonName = "Unknow End Reason";

        switch(reason){
            case END_BY_CALLER:
                reasonName = "End_By_Caller";
                break;
            case END_BY_CALLEE:
                reasonName = "End_By_Callee";
                break;
            case END_BY_LISTENER:
                reasonName = "End_By_Listen";
                break;
            case END_BY_ANSWER:
                reasonName = "End_By_Answer";
                break;
            case END_FOR_CALLEE_UPDATE_FAIL:
                reasonName = "End_For_Callee_Update_Fail";
                break;
            case END_FOR_CALLER_UPDATE_FAIL:
                reasonName = "End_For_Caller_Update_Fail";
                break;
            case END_FOR_ANSWER_UPDATE_FAIL:
                reasonName = "End_For_Answer_Update_Fail";
                break;
            case END_FOR_NO_LISTEN:
                reasonName = "End_For_No_Listen";
                break;
            case END_FOR_OTHER_ANSWER:
                reasonName = "End_For_Other_answer";
                break;
            case END_FOR_CALLEE_REJECT:
                reasonName = "End_For_Callee_Reject";
                break;
            case END_FOR_INVITE_TIMEOVER:
                reasonName = "End_For_Invite_Time_Over";
                break;
            case END_FOR_SERVER_CANCEL:
                reasonName = "End_For_Server_Cancel";
                break;
            case END_BY_USER_CANCEL:
                reasonName = "End_BY_User_Cancel";
                break;
        }

        return reasonName;
    }
}
