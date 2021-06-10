package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;

public class AnswerVideoReqPack extends ProtocolPacket{
    public String callId;
    public String answerDevId;

    private void CopyAnswerVideoData(AnswerVideoReqPack req){
        CopyData(req);
        callId = req.callId;
        answerDevId = req.answerDevId;
    }

    public AnswerVideoReqPack(){
        super();
        type = CALL_VIDEO_ANSWER_REQ;
        answerDevId = "";
    }

    public AnswerVideoReqPack(AnswerVideoReqPack req,String devId){
        CopyAnswerVideoData(req);
        msgID = UniqueIDManager.GetUniqueID(devId,UniqueIDManager.MSG_UNIQUE_ID);
        receiver = devId;

        callId = req.callId;
        answerDevId = req.answerDevId;
    }
}
