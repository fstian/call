package com.example.nettytest.backend.backendtranscation;

import android.os.Message;

import com.example.nettytest.backend.backendphone.BackEndPhoneManager;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.TransManager;

import io.netty.buffer.ByteBuf;

public class BackEndTransactionMgr extends TransManager {

    @Override
    public void TransactionTimeOver(ProtocolPacket packet) {
        Message phonemsg = new Message();
        phonemsg.arg1 = BackEndPhoneManager.MSG_REQ_TIMEOVER;
        phonemsg.obj = packet;
        HandlerMgr.PostBackEndPhoneMsg(phonemsg);
    }

    @Override
    public  void TransactionReqRecv(ProtocolPacket packet) {
        Message phonemsg = new Message();
        phonemsg.arg1 = BackEndPhoneManager.MSG_NEW_PACKET;
        phonemsg.obj = packet;
        HandlerMgr.PostBackEndPhoneMsg(phonemsg);
    }

    @Override
    public  void TransactionResRecv(ProtocolPacket packet) {
        Message phonemsg = new Message();
        phonemsg.arg1 = BackEndPhoneManager.MSG_NEW_PACKET;
        phonemsg.obj = packet;
        HandlerMgr.PostBackEndPhoneMsg(phonemsg);
    }

    @Override
    public void SendTransactionBuf(String ID,ByteBuf buf) {
        HandlerMgr.BackEndDevSendBuf(ID,buf);
    }

}
