package com.example.nettytest.pub.protocol;

public class SystemConfigReqPack extends ProtocolPacket {
    public String devId;

    public SystemConfigReqPack() {
        super();
        type = SYSTEM_CONFIG_REQ;
        devId = "";
    }
}
