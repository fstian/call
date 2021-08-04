package com.example.nettytest.terminal.terminaltransaction;


import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.TransManager;
import com.example.nettytest.terminal.terminalphone.TerminalPhoneManager;

import io.netty.buffer.ByteBuf;

public class TerminalTransactionMgr extends TransManager {
    @Override
    public void TransactionTimeOver(ProtocolPacket packet) {

        CallPubMessage phonemsg = new CallPubMessage();
        phonemsg.arg1 = TerminalPhoneManager.MSG_REQ_TIMEOVER;
        phonemsg.obj = packet;
        HandlerMgr.PostTerminalPhoneMsg(phonemsg);
    }

    @Override
    public void TransactionReqRecv(ProtocolPacket packet) {
        CallPubMessage phonemsg = new CallPubMessage();
        phonemsg.arg1 = TerminalPhoneManager.MSG_NEW_PACKET;
        phonemsg.obj = packet;
        HandlerMgr.PostTerminalPhoneMsg(phonemsg);

    }

    @Override
    public void TransactionResRecv(ProtocolPacket packet) {
        CallPubMessage phonemsg = new CallPubMessage();
        phonemsg.arg1 = TerminalPhoneManager.MSG_NEW_PACKET;
        phonemsg.obj = packet;
        HandlerMgr.PostTerminalPhoneMsg(phonemsg);
    }

    @Override
    public void SendTransactionBuf(String ID,ByteBuf buf) {
        HandlerMgr.PhoneDevSendBuf(ID,buf);
    }

    @Override
    public void SendTransactionBuf(String ID,String data){
        HandlerMgr.PhoneDevSendBuf(ID,data);
    }
}
