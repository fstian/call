package com.example.nettytest.pub.protocol;

public class StopVideoResPack extends ProtocolPacket {
    public String callid;
    public String stopVideoDevId;
    public int status;
    public String result;

    public StopVideoResPack(){
        super();
        type = ProtocolPacket.CALL_VIDEO_END_RES;

        callid = "";
        status = ProtocolPacket.STATUS_OK;
        result = ProtocolPacket.GetResString(status);
    }

    public StopVideoResPack(int error,StopVideoReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_VIDEO_END_RES;

        callid = reqP.callID;
        stopVideoDevId = reqP.stopVideoDevId;
        
        status = error;
        result = ProtocolPacket.GetResString(status);
    }
}


