package com.example.nettytest.pub.protocol;

public class TransferChangeResPack extends ProtocolPacket{
    public int status;   // operation result, succ or fail
    public String result;

    public String devId;
    public String transferAreaId;    // listen state, enable or disable

    public TransferChangeResPack(){
        type = ProtocolPacket.CALL_TRANSFER_CHANGE_RES;
    }

    public TransferChangeResPack(int error, TransferChangeReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_TRANSFER_CHANGE_RES;

        devId = reqP.devID;
        transferAreaId = "";
        status = error;
        result = ProtocolPacket.GetResString(status);
    }
}
