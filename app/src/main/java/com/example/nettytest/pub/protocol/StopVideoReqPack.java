package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.UniqueIDManager;

public class StopVideoReqPack extends ProtocolPacket{
    public String callID;

    public String stopVideoDevId;

    private void CopyStopVideoData(StopVideoReqPack videoReq){
        CopyData(videoReq);
        callID = videoReq.callID;

        stopVideoDevId = videoReq.stopVideoDevId;
    }

    public void ExchangeCopyData(StopVideoReqPack stopReq){
        super.ExchangeCopyData(stopReq);
        CopyStopVideoData(stopReq);
    }

    public StopVideoReqPack(){
        super();
        type = ProtocolPacket.CALL_VIDEO_END_REQ;
        callID= "";
    }

    public StopVideoReqPack(StopVideoReqPack pack,String id){
        CopyStopVideoData(pack);
        msgID = UniqueIDManager.GetUniqueID(id,UniqueIDManager.MSG_UNIQUE_ID);
        receiver = id;
    }

}


