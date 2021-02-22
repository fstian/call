package com.example.nettytest.pub.protocol;

public class AnswerResPack extends ProtocolPacket{
    String callID;
    int status;
    String result;

    public AnswerResPack(){
        super();
        type = ProtocolPacket.ANSWER_RES;
        status = UNKONWSTATUATYPE;
        callID = "";
        result = "";
    }

    public AnswerResPack(int status,AnswerReqPack reqPack){
        ExchangeCopyData(reqPack);
        type = ProtocolPacket.ANSWER_RES;
        callID = reqPack.callID;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }

}
