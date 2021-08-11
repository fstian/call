package com.example.nettytest.pub.protocol;

public class ListenClearResPack extends ProtocolPacket{
    public int status;   // operation result, succ or fail
    public String result;

    public String devId;
    public boolean state;    // listen state, enable or disable

    public ListenClearResPack(){
        type = ProtocolPacket.CALL_LISTEN_CLEAR_RES;
    }

    public ListenClearResPack(int error,ListenClearReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_LISTEN_CLEAR_RES;

        devId = reqP.devID;
        state = reqP.status;
        status = error;
        result = ProtocolPacket.GetResString(status);
    }
}
