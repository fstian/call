package com.example.nettytest.pub.protocol;

public class UpdateReqPack extends ProtocolPacket{
    public String callId;
    public String devId;

    public UpdateReqPack(){
        super();
        type = ProtocolPacket.CALL_UPDATE_REQ;
        callId = "";
        devId = "";
    }
}
