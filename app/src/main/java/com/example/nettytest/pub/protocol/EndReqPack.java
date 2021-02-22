package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.userinterface.PhoneParam;

public class EndReqPack extends ProtocolPacket{
    public String callID;
    public String endDevID;

    public EndReqPack(){
        super();
        type = ProtocolPacket.END_REQ;
        callID = "";
        endDevID = "";
    }

    public  EndReqPack(EndReqPack endReq,String devid){
        CopyData(endReq);
        receiver = devid;
        msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);
        callID = endReq.callID;
        endDevID = endReq.endDevID;
    }

    public EndReqPack(String callId){
        super();
        type = ProtocolPacket.END_REQ;
        sender = PhoneParam.CALL_SERVER_ID;
        receiver = PhoneParam.CALL_SERVER_ID;
        msgID = UniqueIDManager.GetUniqueID(PhoneParam.CALL_SERVER_ID,UniqueIDManager.MSG_UNIQUE_ID);

        this.callID = callId;
        endDevID = PhoneParam.CALL_SERVER_ID;
    }
}
