package com.example.nettytest.userinterface;

import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.pub.AudioMode;

public class UserCallMessage extends UserMessage {

    public static final int NORMAL_CALL_TYPE = 1;
    public static final int EMERGENCY_CALL_TYPE = 2;
    public static final int BROADCAST_CALL_TYPE = 3;

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

    public String remoteRtpAddress;
    public int remoteRtpPort;
    public int localRtpPort;

    public int rtpCodec;
    public int rtpPTime;
    public int rtpSample;

    public int audioMode;

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
        patientAge = "18";
        bedName = "";
        roomId = "";

        remoteRtpAddress = "";
        remoteRtpPort = PhoneParam.INVITE_CALL_RTP_PORT;
        localRtpPort = PhoneParam.INVITE_CALL_RTP_PORT;

        rtpCodec= PhoneParam.callRtpCodec;
        rtpPTime = PhoneParam.callRtpPTime;
        rtpSample = PhoneParam.callRtpDataRate;

        audioMode = AudioMode.NO_SEND_RECV_MODE;
    }

}
