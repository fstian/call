package com.example.nettytest.pub.protocol;

public class ListenClearReqPack extends ProtocolPacket{
    public String devID;
    public boolean status;

    public ListenClearReqPack(){
        super();
        type = ProtocolPacket.CALL_LISTEN_CLEAR_REQ;
        devID = "";
        status = false;
    }

    public ListenClearReqPack(String id){
        super();
        type = ProtocolPacket.CALL_LISTEN_CLEAR_REQ;
        devID = id;
        status = false;
    }

}
