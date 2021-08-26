package com.example.nettytest.userinterface;

import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.pub.AudioMode;

public class UserCallMessage extends UserMessage {

    public static final int NORMAL_CALL_TYPE = 1;
    public static final int EMERGENCY_CALL_TYPE = 2;
    public static final int BROADCAST_CALL_TYPE = 3;

    public static final int CALL_END_BY_SELF = 1;
    // same with EndReqPack
    public static final int CALL_END_BY_CALLER = 2;
    public static final int CALL_END_BY_CALLEE = 3;
    public static final int CALL_END_BY_LISTEN = 4;
    public static final int CALL_END_BY_ANSWER = 5;

    public static final int CALL_END_FOR_CALLER_UPDATE_FAIL = 10;
    public static final int CALL_END_FOR_CALLEE_UPDATE_FAIL = 11;
    public static final int CALL_END_FOR_ANSWER_UPDATE_FAIL = 12;    
    public static final int CALL_END_FOR_NO_LISTEN = 13;
    public static final int CALL_END_FOR_OTHER_ANSWER = 14;
    public static final int CALL_END_FOR_CALLEE_REJECT = 15;
    public static final int CALL_END_FOR_INVITE_TIMEOVER = 16;
    

    public int callType;
    public String callId;

    public int callerType;
    public String callerId;
    public String calleeId;
    public String operaterId;

    public String deviceName;
    public String patientName;
    public String patientAge;
    public String bedName;
    public String roomId;

    public String areaId;
    public String areaName;
    public boolean isTransfer;

    public String remoteRtpAddress;
    public int remoteRtpPort;
    public int localRtpPort;

    public int rtpCodec;
    public int rtpPTime;
    public int rtpSample;

    public int audioMode;

    public int endReason;

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
        patientAge = "";
        bedName = "";
        roomId = "";

        areaId = "";
        areaName = "";
        isTransfer = false;

        remoteRtpAddress = "";
        remoteRtpPort = PhoneParam.INVITE_CALL_RTP_PORT;
        localRtpPort = PhoneParam.INVITE_CALL_RTP_PORT;

        rtpCodec= PhoneParam.callRtpCodec;
        rtpPTime = PhoneParam.callRtpPTime;
        rtpSample = PhoneParam.callRtpDataRate;

        audioMode = AudioMode.NO_SEND_RECV_MODE;
    }

    public static String GetEndReasonName(int reason){
        String reasonName = "Unknow End Reason";

        switch(reason){
            case CALL_END_BY_SELF:
                reasonName = "End_By_Self";
                break;
            case CALL_END_BY_CALLER:
                reasonName = "End_By_Caller";
                break;
            case CALL_END_BY_CALLEE:
                reasonName = "End_By_Callee";
                break;
            case CALL_END_BY_LISTEN:
                reasonName = "End_By_Listen";
                break;
            case CALL_END_BY_ANSWER:
                reasonName = "End_By_Answer";
                break;
            case CALL_END_FOR_CALLEE_UPDATE_FAIL:
                reasonName = "End_For_Callee_Update_Fail";
                break;
            case CALL_END_FOR_CALLER_UPDATE_FAIL:
                reasonName = "End_For_Caller_Update_Fail";
                break;
            case CALL_END_FOR_ANSWER_UPDATE_FAIL:
                reasonName = "End_For_Answer_Update_Fail";
                break;
            case CALL_END_FOR_NO_LISTEN:
                reasonName = "End_For_No_Listen";
                break;
            case CALL_END_FOR_OTHER_ANSWER:
                reasonName = "End_For_Other_answer";
                break;
            case CALL_END_FOR_CALLEE_REJECT:
                reasonName = "End_For_Callee_Reject";
                break;
            case CALL_END_FOR_INVITE_TIMEOVER:
                reasonName = "End_For_Invite_Time_Over";
                break;
        }

        return reasonName;
    }
}
