package com.example.nettytest.pub.protocol;

public class ListenCallResPack extends ProtocolPacket{
    public int status;   // operation result, succ or fail
    public String result;  

    public String devId;
    public boolean state;    // listen state, enable or disable

    public ListenCallResPack(){
        type = ProtocolPacket.CALL_LISTEN_RES;
    }

    public ListenCallResPack(int error,ListenCallReqPack reqP){
        ExchangeCopyData(reqP);
        type = ProtocolPacket.CALL_LISTEN_RES;

        devId = reqP.devID;
        state = reqP.listenEnable;
        status = error;
        result = ProtocolPacket.GetResString(status);

    }
}


