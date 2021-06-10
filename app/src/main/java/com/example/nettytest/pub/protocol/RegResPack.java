package com.example.nettytest.pub.protocol;

public class RegResPack extends ProtocolPacket{
    public int status;
    public String result;
    public String areaId;
    public String transferAreaId;
    public boolean listenCallEnable;

    public RegResPack(){
        super();
        type = ProtocolPacket.REG_RES;
        result = "";
        areaId = "";
        transferAreaId = "";
        listenCallEnable = false;
    }

    public RegResPack(int status,RegReqPack regPack){
        ExchangeCopyData(regPack);
        type = ProtocolPacket.REG_RES;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
        areaId = "";
        transferAreaId = "";
        listenCallEnable = false;
    }
}
