package com.example.nettytest.pub.protocol;

import java.util.ArrayList;

public class SystemConfigResPack extends ProtocolPacket {
        public String result;
        public int status;
        public String devId;

        public ArrayList<ConfigItem> params;

        public SystemConfigResPack(){
            devId = "";
            status = ProtocolPacket.STATUS_OK;
            result = ProtocolPacket.GetResString(status);
            params = new ArrayList<>();
        }

        public SystemConfigResPack(int status,SystemConfigReqPack pack){
            ExchangeCopyData(pack);
            devId = pack.devId;
            type = ProtocolPacket.SYSTEM_CONFIG_RES;
            params = new ArrayList<>();
            this.status = status;
            result = ProtocolPacket.GetResString(status);
        }

}
