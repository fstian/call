package com.example.nettytest.pub.protocol;

public class StartVideoResPack extends ProtocolPacket {
    public String callid;
    public String startVideoDevId;
    public int status;
    public String result;

    public StartVideoResPack(){
        super();
        type = ProtocolPacket.CALL_VIDEO_INVITE_RES;

        callid = "";
        status = ProtocolPacket.STATUS_OK;
        result = ProtocolPacket.GetResString(status);
    }

    public StartVideoResPack(int error,StartVideoReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_VIDEO_INVITE_RES;

        callid = reqP.callID;
        startVideoDevId = reqP.startVideoDevId;
        
        status = error;
        result = ProtocolPacket.GetResString(status);
    }
}

