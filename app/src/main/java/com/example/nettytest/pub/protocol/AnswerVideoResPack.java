package com.example.nettytest.pub.protocol;

public class AnswerVideoResPack extends ProtocolPacket {
    public String callId;
    public String answerVideoDevId;
    public int status;
    public String result;

    public AnswerVideoResPack(){
        super();
        callId = "";
        answerVideoDevId = "";
        status = UNKONWSTATUATYPE;
    }
    public AnswerVideoResPack(int status,AnswerVideoReqPack reqPack){
        ExchangeCopyData(reqPack);
        type = ProtocolPacket.CALL_VIDEO_ANSWER_RES;
        callId = reqPack.callId;
        answerVideoDevId = reqPack.answerDevId;

        this.status = status;
        result = ProtocolPacket.GetResString(status);
    }

}
