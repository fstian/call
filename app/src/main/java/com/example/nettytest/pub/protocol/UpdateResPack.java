package com.example.nettytest.pub.protocol;

public class UpdateResPack extends ProtocolPacket {
    public String callid;
    public int status;
    public String result;

    public UpdateResPack(){
        super();
        type = ProtocolPacket.CALL_UPDATE_RES;

        callid = "";
        status = ProtocolPacket.STATUS_OK;
        result = ProtocolPacket.GetResString(status);
    }

    public UpdateResPack(int error,UpdateReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_UPDATE_RES;

        callid = reqP.callId;
        status = error;
        result = ProtocolPacket.GetResString(status);
    }
}
