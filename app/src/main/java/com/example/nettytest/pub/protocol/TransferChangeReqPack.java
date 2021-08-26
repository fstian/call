package com.example.nettytest.pub.protocol;

public class TransferChangeReqPack extends ProtocolPacket{
    public String devID;
    public String transferAreaId;
    public boolean state;

    public TransferChangeReqPack(){
        super();
        type = ProtocolPacket.CALL_TRANSFER_CHANGE_REQ;
        devID = "";
        transferAreaId = "";
        state = false;
    }

    public TransferChangeReqPack(String id){
        super();
        type = ProtocolPacket.CALL_TRANSFER_CHANGE_REQ;
        devID = id;
        transferAreaId = "";
        state = false;
    }

}
