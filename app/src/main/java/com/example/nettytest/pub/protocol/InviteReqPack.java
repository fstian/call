package com.example.nettytest.pub.protocol;

public class InviteReqPack extends ProtocolPacket{
    public int callType;
    public int callDirect;
    public String callID;

    // add by caller
    public String caller;
    public int callerType;   
    public String patientName;
    public String patientAge;

    // add by server    
    public String bedName;
    public String roomId;
    public String roomName;
    public String deviceName;
    public String areaId;
    public String areaName;
    public boolean isTransfer;
   
    public String callee;

    public int codec;
    public int pTime;
    public int sample;

    public String callerRtpIP;
    public int callerRtpPort;

    public int autoAnswerTime;

    private void CopyInviteData(InviteReqPack invitePack){
        callType = invitePack.callType;
        callDirect = invitePack.callDirect;
        callID = invitePack.callID;

        caller = invitePack.caller;
        callerType = invitePack.callerType;
        callee = invitePack.callee;

        bedName = invitePack.bedName;
        deviceName = invitePack.deviceName;
        patientAge = invitePack.patientAge;
        patientName = invitePack.patientName;
        roomId = invitePack.roomId;
        roomName = invitePack.roomName;

        areaId = invitePack.areaId;
        areaName = invitePack.areaName;
        isTransfer = invitePack.isTransfer;

        codec = invitePack.codec;
        pTime = invitePack.pTime;
        sample = invitePack.sample;

        callerRtpPort = invitePack.callerRtpPort;
        callerRtpIP = invitePack.callerRtpIP;

        autoAnswerTime = invitePack.autoAnswerTime;
    }

    public int ExchangeCopyData(InviteReqPack pack){
        super.ExchangeCopyData(pack);

        CopyInviteData(pack);

        return 1;
    }
}
