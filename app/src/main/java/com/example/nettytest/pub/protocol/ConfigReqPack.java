package com.example.nettytest.pub.protocol;

public class ConfigReqPack extends ProtocolPacket {
    public String devId;

    public ConfigReqPack(){
        super();
        type = DEV_CONFIG_REQ;
        devId = "";
    }

}
