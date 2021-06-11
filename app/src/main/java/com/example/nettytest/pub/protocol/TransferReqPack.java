package com.example.nettytest.pub.protocol;

public class TransferReqPack extends ProtocolPacket {
    public String devID;
    public String transferAreaId;
    public boolean transferEnabled;

    public TransferReqPack(){
        super();
        type = ProtocolPacket.CALL_TRANSFER_REQ;
        devID = "";
        transferAreaId = "";
        transferEnabled = false;
    }

    public TransferReqPack(String id,String areaId){
        super();
        type = ProtocolPacket.CALL_TRANSFER_REQ;
        devID = id;
        transferAreaId = areaId;
        transferEnabled = false;
    }
}
