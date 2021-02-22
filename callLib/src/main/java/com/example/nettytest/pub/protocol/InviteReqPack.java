package com.example.nettytest.pub.protocol;

public class InviteReqPack extends ProtocolPacket{
    public int callType;
    public int callDirect;
    public String callID;

    public String caller;
    public int callerType;
    public String bedID;

    public String callee;

    public int codec;
    public int pTime;
    public int sample;

    public String callerRtpIP;
    public int callerRtpPort;

    public String broadcastIP;
    public int broadcastPort;

    private void CopyInviteData(InviteReqPack invitePack){
        callType = invitePack.callType;
        callDirect = invitePack.callDirect;
        callID = invitePack.callID;

        caller = invitePack.caller;
        callerType = invitePack.callerType;
        callee = invitePack.callee;
        bedID = invitePack.bedID;

        codec = invitePack.codec;
        pTime = invitePack.pTime;
        sample = invitePack.sample;

        callerRtpPort = invitePack.callerRtpPort;
        callerRtpIP = invitePack.callerRtpIP;

        broadcastPort = invitePack.broadcastPort;
        broadcastIP = invitePack.broadcastIP;
    }

    public int ExchangeCopyData(InviteReqPack pack){
        super.ExchangeCopyData(pack);

        CopyInviteData(pack);

        return 1;
    }
}
