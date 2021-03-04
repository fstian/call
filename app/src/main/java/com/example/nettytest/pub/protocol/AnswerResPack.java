package com.example.nettytest.pub.protocol;

public class AnswerResPack extends ProtocolPacket{
    public String callID;
    public int status;
    public String result;

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
