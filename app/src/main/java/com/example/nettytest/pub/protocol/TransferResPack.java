package com.example.nettytest.pub.protocol;

public class TransferResPack extends ProtocolPacket{
    public int status;   // operation result, succ or fail
    public String result;  

    public String devId;
    public String transferAreaId;
    public boolean state;    // transfer state, enable or disable

    public TransferResPack(){
        type = ProtocolPacket.CALL_TRANSFER_RES;
    }

    public TransferResPack(int error,TransferReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_TRANSFER_RES;

        devId = reqP.devID;
        state = reqP.transferEnabled;
        transferAreaId = reqP.transferAreaId;
        status = error;
        result = ProtocolPacket.GetResString(status);

    }
}

