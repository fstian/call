package com.example.nettytest.pub.protocol;

public class CancelReqPack extends ProtocolPacket{
    public String callID;
    public String cancelDevID;

    public CancelReqPack(){
        super();
        type = CALL_CANCEL_REQ;
        callID = "";
        cancelDevID = "";
    }

    public CancelReqPack(String callId,String canceler,String cancelpeer){
        super();
        type = CALL_CANCEL_REQ;
        callID = callId;
        cancelDevID = canceler;
        sender = canceler;
        receiver = cancelpeer;
    }
}
