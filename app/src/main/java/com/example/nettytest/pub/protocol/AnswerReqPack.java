package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;

public class AnswerReqPack extends ProtocolPacket{
    public String callID;

    public String answerer;
    public String answerBedName;
    public String answerRoomId;
    public String answerDeviceName;
    public int callType;

    public int codec;
    public int pTime;
    public int sample;

    public String answererRtpIP;
    public int answererRtpPort;

    private void CopyAnswerData(AnswerReqPack ans){
        CopyData(ans);
        callID = ans.callID;

        answerer = ans.answerer;
        answerBedName = ans.answerBedName;
        answerDeviceName = ans.answerDeviceName;
        answerRoomId = ans.answerRoomId;
        callType = ans.callType;

        codec = ans.codec;
        pTime = ans.pTime;
        sample = ans.sample;

        answererRtpIP = ans.answererRtpIP;
        answererRtpPort = ans.answererRtpPort;
    }

    public void ExchangeCopyData(AnswerReqPack ans){
        super.ExchangeCopyData(ans);
        CopyAnswerData(ans);
    }

    public AnswerReqPack(){
        super();
        type = ProtocolPacket.ANSWER_REQ;
        callID= "";
        answerer = "";
        answerRoomId = "";
        answerBedName="";
        answerDeviceName = "";
    }

    public AnswerReqPack(AnswerReqPack pack,String id){
        CopyAnswerData(pack);
        msgID = UniqueIDManager.GetUniqueID(id,UniqueIDManager.MSG_UNIQUE_ID);
        receiver = id;
    }

}
