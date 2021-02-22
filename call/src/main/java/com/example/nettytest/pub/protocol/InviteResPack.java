package com.example.nettytest.pub.protocol;

public class InviteResPack extends ProtocolPacket {
    public String callID;
    public int status;
    public String result;

    public InviteResPack(){
        super();
        type = ProtocolPacket.CALL_RES;
        status = UNKONWSTATUATYPE;
        callID = "";
        result = "";
    }

    public InviteResPack(int status,InviteReqPack invitePack){
        ExchangeCopyData(invitePack);
        type = ProtocolPacket.CALL_RES;
        this.status = status;
        result = ProtocolPacket.GetResString(status);
        callID = invitePack.callID;
    }
}
