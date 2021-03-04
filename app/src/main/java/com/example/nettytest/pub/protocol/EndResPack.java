package com.example.nettytest.pub.protocol;

public class EndResPack extends ProtocolPacket{
    public String callId;
    public int status;
    public String result;

    public EndResPack(){
        super();
        type = ProtocolPacket.END_RES;
        status = UNKONWSTATUATYPE;
        callId = "";
        result = "";
    }

    public EndResPack(int status,EndReqPack reqPack){
        ExchangeCopyData(reqPack);
        type = ProtocolPacket.END_RES;
        callId = reqPack.callID;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }

}
