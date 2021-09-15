package com.example.nettytest.pub.protocol;

public class CancelResPack extends ProtocolPacket{
    public String callId;
    public int status;
    public String result;

    public CancelResPack(){
        super();
        type = ProtocolPacket.CALL_CANCEL_RES;
        status = UNKONWSTATUATYPE;
        callId = "";
        result = "";
    }

    public CancelResPack(int status,CancelReqPack reqPack){
        ExchangeCopyData(reqPack);
        type = ProtocolPacket.CALL_CANCEL_RES;
        callId = reqPack.callID;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }

}
