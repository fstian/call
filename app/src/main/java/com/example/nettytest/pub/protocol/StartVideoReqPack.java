package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;

public class StartVideoReqPack extends ProtocolPacket{
    public String callID;

    public String startVideoDevId;

    private void CopyStartVideoData(StartVideoReqPack videoReq){
        CopyData(videoReq);
        callID = videoReq.callID;

        startVideoDevId = videoReq.startVideoDevId;
    }

    public void ExchangeCopyData(StartVideoReqPack ans){
        super.ExchangeCopyData(ans);
        CopyStartVideoData(ans);
    }

    public StartVideoReqPack(){
        super();
        type = ProtocolPacket.CALL_VIDEO_INVITE_REQ;
        callID= "";
    }

    public StartVideoReqPack(StartVideoReqPack pack,String id){
        CopyStartVideoData(pack);
        msgID = UniqueIDManager.GetUniqueID(id,UniqueIDManager.MSG_UNIQUE_ID);
        receiver = id;
    }

}

