package com.example.nettytest.pub.protocol;

public class RegResPack extends ProtocolPacket{
    public int status;
    public String result;

    public RegResPack(){
        super();
        type = ProtocolPacket.REG_RES;
        result = "";
    }

    public RegResPack(int status,RegReqPack regPack){
        ExchangeCopyData(regPack);
        type = ProtocolPacket.REG_RES;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }
}
