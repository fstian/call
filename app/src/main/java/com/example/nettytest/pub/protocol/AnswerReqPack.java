package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;

public class AnswerReqPack extends ProtocolPacket{
    public String callID;

    public String answerer;
    public String bedID;

    public int codec;
    public int pTime;

    public String answererRtpIP;
    public int answererRtpPort;

    private void CopyAnswerData(AnswerReqPack ans){
        CopyData(ans);
        callID = ans.callID;

        answerer = ans.answerer;
        bedID = ans.bedID;

        codec = ans.codec;
        pTime = ans.pTime;

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
    }

    public AnswerReqPack(AnswerReqPack pack,String id){
        CopyAnswerData(pack);
        answerer = id;
        msgID = UniqueIDManager.GetUniqueID(id,UniqueIDManager.MSG_UNIQUE_ID);
        receiver = id;
    }

}
