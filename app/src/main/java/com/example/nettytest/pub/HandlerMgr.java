package com.example.nettytest.pub;

import android.os.Handler;
import android.os.Message;

import com.example.nettytest.backend.backenddevice.BackEndDevManager;
import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.backend.backendphone.BackEndPhoneManager;
import com.example.nettytest.backend.backendtranscation.BackEndTransactionMgr;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.terminal.terminaldevice.TerminalDevManager;
import com.example.nettytest.terminal.terminaldevice.TerminalDevice;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;
import com.example.nettytest.terminal.terminalphone.TerminalPhoneManager;
import com.example.nettytest.terminal.terminaltransaction.TerminalTransactionMgr;

import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class HandlerMgr {
    static private BackEndTransactionMgr backEndTransMgr = new BackEndTransactionMgr();
    static private BackEndDevManager backEndDevMgr = new BackEndDevManager();
    static private BackEndPhoneManager backEndPhoneMgr = new BackEndPhoneManager();

    static private TerminalTransactionMgr terminalTransMgr = new TerminalTransactionMgr();
    static private TerminalDevManager terminalDevManager = new TerminalDevManager();
    static private TerminalPhoneManager terminalPhoneMgr = new TerminalPhoneManager();

    static private Handler terminalMsgHandler = null;

//for terminal NetDevice

    static public void UpdatePhoneDevChannel(String ID, Channel channel){
        terminalDevManager.UpdateDevChannel(ID,channel);
    }

    static public void PhoneDevSendBuf(String ID,ByteBuf buf){
        terminalDevManager.DevSendBuf(ID,buf);
    }

// for TerminalTranscation
    static public boolean AddPhoneTrans(String ID, Transaction trans){
        return terminalTransMgr.AddTransaction(ID,trans);
    }

    static public void PhoneProcessPacket(ProtocolPacket packet){
        terminalTransMgr.ProcessPacket(packet);
    }

// for terminal Phone
    static public TerminalPhone GetPhoneDev(String ID){

        return terminalPhoneMgr.GetDevice(ID);
    }

    static public void PostTerminalPhoneMsg(Message msg){
        terminalPhoneMgr.PostTerminalPhoneMessage(msg);
    }

    static public void SetTerminalMessageHandler(Handler h){
        terminalMsgHandler = h;
        terminalPhoneMgr.SetMessageHandler(h);
    }

    static public void SendMessageToUser(Message msg){
        terminalPhoneMgr.SendUserMessage(msg);
    }

    static public void CreateTerminalPhone(String id,int type){
        TerminalDevice dev = new TerminalDevice(id);
        TerminalPhone phone = new TerminalPhone(id,type,terminalMsgHandler);
        terminalDevManager.AddDevice(id,dev);
        dev.Start();

        terminalPhoneMgr.AddDevice(phone);
    }

    static public String BuildTerminalCall(String caller,String callee,int callType){
        return terminalPhoneMgr.BuildCall(caller,callee,callType);
    }

    static public int EndTerminalCall(String devid,String callID){
        return terminalPhoneMgr.EndCall(devid,callID);
    }

    static public int AnswerTerminalCall(String devid,String callID){
        return terminalPhoneMgr.AnswerCall(devid,callID);
    }

    static public int QueryTerminalLists(String devid){
        return terminalPhoneMgr.QueryDevs(devid);
    }

// for backend NetDevice
    static public void UpdateBackEndDevChannel(String ID,Channel ch){
        backEndDevMgr.UpdateDevChannel(ID,ch);
    }

    static public void BackEndDevSendBuf(String ID,ByteBuf buf){
        backEndDevMgr.DevSendBuf(ID, buf);
    }


//for backend transcation
    static public void AddBackEndTrans(String ID,Transaction trans){
        backEndTransMgr.AddTransaction(ID,trans);
    }

    static public void BackEndProcessPacket(ProtocolPacket packet){
        backEndTransMgr.ProcessPacket(packet);
    }



// for backend phone
    static public BackEndPhone GetBackEndPhone(String ID) {
        return backEndPhoneMgr.GetDevice(ID);
    }


    static public void PostBackEndPhoneMsg(Message msg){
        backEndPhoneMgr.PostBackEndPhoneMessage(msg);
    }

// create backend device
    static public boolean AddBackEndPhone(String ID,int type){
        backEndDevMgr.AddDevice(ID);
        backEndPhoneMgr.AddPhone(ID,type);
        return true;
    }

// get listen phone
    static public ArrayList<BackEndPhone> GetBackEndListenDevices(){
        return backEndPhoneMgr.GetListenDevices();
    }

}
