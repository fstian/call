package com.example.nettytest.backend.backendtranscation;

import com.example.nettytest.backend.backendphone.BackEndPhoneManager;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.TransManager;

import io.netty.buffer.ByteBuf;

public class BackEndTransactionMgr extends TransManager{

    @Override
    public void TransactionTimeOver(ProtocolPacket packet) {
        HandlerMgr.PostBackEndPhoneMsg(BackEndPhoneManager.MSG_REQ_TIMEOVER,packet);
    }

    @Override
    public  void TransactionReqRecv(ProtocolPacket packet) {
        HandlerMgr.PostBackEndPhoneMsg(BackEndPhoneManager.MSG_NEW_PACKET,packet);
    }

    @Override
    public  void TransactionResRecv(ProtocolPacket packet) {
        HandlerMgr.PostBackEndPhoneMsg(BackEndPhoneManager.MSG_NEW_PACKET,packet);
    }

    @Override
    public void SendTransactionBuf(String ID,ByteBuf buf) {
        HandlerMgr.BackEndDevSendBuf(ID,buf);
    }

    @Override
    public void SendTransactionBuf(String ID,String data){
        HandlerMgr.BackEndDevSendBuf(ID,data);
    }

}
