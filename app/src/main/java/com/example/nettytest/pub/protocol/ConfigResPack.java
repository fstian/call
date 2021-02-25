package com.example.nettytest.pub.protocol;

import java.util.ArrayList;

public class ConfigResPack extends ProtocolPacket {
    public String result;
    public int status;
    public String devId;

    public ArrayList<ConfigItem> params;

    public ConfigResPack(){
        devId = "";
        status = ProtocolPacket.STATUS_OK;
        result = ProtocolPacket.GetResString(status);
        params = new ArrayList<>();
    }

    public ConfigResPack(int status,ConfigReqPack pack){
        ExchangeCopyData(pack);
        devId = pack.devId;
        type = ProtocolPacket.DEV_CONFIG_RES;
        params = new ArrayList<>();
        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }

}
