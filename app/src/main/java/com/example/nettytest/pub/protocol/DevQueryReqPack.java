package com.example.nettytest.pub.protocol;


public class DevQueryReqPack extends ProtocolPacket {
    public String devid;

    public DevQueryReqPack(){

        super();
        devid = "";
        type = ProtocolPacket.DEV_QUERY_REQ;
    }

}
