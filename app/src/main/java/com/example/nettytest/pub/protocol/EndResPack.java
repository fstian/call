package com.example.nettytest.pub.protocol;

public class EndResPack extends ProtocolPacket{
    public String callID;
    public int status;
    public String result;

    public EndResPack(){
        super();
        type = ProtocolPacket.END_RES;
        status = UNKONWSTATUATYPE;
        callID = "";
        result = "";
    }

    public EndResPack(int status,EndReqPack reqPack){
        ExchangeCopyData(reqPack);
        type = ProtocolPacket.END_RES;
        callID = reqPack.callID;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }

}
