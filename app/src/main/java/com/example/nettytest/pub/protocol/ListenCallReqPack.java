package com.example.nettytest.pub.protocol;

public class ListenCallReqPack extends ProtocolPacket {
    public String devID;
    public boolean listenEnable;

    public ListenCallReqPack(){
        super();
        type = ProtocolPacket.CALL_LISTEN_REQ;
        devID = "";
        listenEnable = false;
    }

    public ListenCallReqPack(String id,boolean state){
        super();
        type = ProtocolPacket.CALL_LISTEN_REQ;
        devID = id;
        listenEnable = state;
    }
}

