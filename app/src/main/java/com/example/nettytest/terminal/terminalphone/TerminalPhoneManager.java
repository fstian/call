package com.example.nettytest.terminal.terminalphone;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.UpdateResPack;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalPhoneManager {
    HashMap<String, TerminalPhone> clientPhoneLists;
    Handler phoneManagerMsgHandler =null;
    Handler userMsgHandler = null;
    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;
    public static final int MSG_REQ_TIMEOVER = 3;


    public TerminalPhoneManager(){
        clientPhoneLists = new HashMap<>();
        TerminalPhoneThread terminalPhoneThread = new TerminalPhoneThread();
        terminalPhoneThread.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message phonemsg = new Message();
                phonemsg.arg1 = TerminalPhoneManager.MSG_SECOND_TICK;
                phonemsg.obj = "";
                HandlerMgr.PostTerminalPhoneMsg(phonemsg);
            }
        },0,1000);
    }

    public void AddDevice(TerminalPhone phone){
        TerminalPhone matchedDev;

        synchronized (this) {
            matchedDev = clientPhoneLists.get(phone.id);
            if(matchedDev==null)
                clientPhoneLists.put(phone.id,phone);
        }

    }
    public void SetMessageHandler(Handler h){
            userMsgHandler = h;
            for (TerminalPhone phone : clientPhoneLists.values()) {
                phone.SetMessageHandler(h);
            }
    }

    public void SendUserMessage(Message msg){
        if (userMsgHandler != null) {
            userMsgHandler.sendMessage(msg);
        }
    }

    public TerminalPhone GetDevice(String id){
        TerminalPhone matchedDev;

        matchedDev = clientPhoneLists.get(id);

        return matchedDev;

    }

    public void PostTerminalPhoneMessage(Message msg){
        if (phoneManagerMsgHandler != null) {
            phoneManagerMsgHandler.sendMessage(msg);
        }
    }

    public String BuildCall(String caller,String callee,int callType){
        String callid = null;
        TerminalPhone matchedDev;

        synchronized (this) {
            matchedDev = clientPhoneLists.get(caller);
            if(matchedDev!=null){
                callid = matchedDev.MakeOutGoingCall(callee,callType);
            }
        }

        return callid;
    }

    public int EndCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;

        synchronized (this) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.EndCall(callID);
            }
        }
        return result;
    }

    public int AnswerCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;
        synchronized (this) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.AnswerCall(callID);
            }
        }
        return result;
    }

    public int QueryDevs(String devid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone phone;
        synchronized (this){
            phone = clientPhoneLists.get(devid);
            if(phone!=null){
                result = phone.QueryDevs();
            }
        }
        return result;
    }

    private void PacketRecvProcess(ProtocolPacket packet) {

        TerminalPhone phone = GetDevice(packet.receiver);
        if(phone!=null){
            switch(packet.type){
                case ProtocolPacket.REG_RES:
                    RegResPack resP = (RegResPack)packet;
                    phone.UpdateRegStatus(resP.status);
                    break;
                case ProtocolPacket.CALL_RES:
                    InviteResPack inviteResP = (InviteResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Invite Res, call id is %s",inviteResP.receiver,inviteResP.callID);
                    phone.UpdateCallStatus(inviteResP);
                    break;
                case ProtocolPacket.CALL_REQ:
                    InviteReqPack inviteReqPack = (InviteReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Invite Req, call id is %s",inviteReqPack.receiver,inviteReqPack.callID);
                    phone.RecvIncomingCall(inviteReqPack);
                    break;
                case ProtocolPacket.END_REQ:
                    EndReqPack endReqPack = (EndReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv End Req, call id is %s",endReqPack.receiver,endReqPack.callID);
                    phone.UpdateCallStatus(endReqPack);
                    break;
                case ProtocolPacket.END_RES:
                    EndResPack endResPack = (EndResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv End Res, call id is %s",endResPack.receiver,endResPack.callID);
                    break;
                case ProtocolPacket.ANSWER_REQ:
                    AnswerReqPack answerReqPack = (AnswerReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Answer Req, call id is %s",answerReqPack.receiver,answerReqPack.callID);
                    phone.UpdateCallStatus(answerReqPack);
                    break;
                case ProtocolPacket.CALL_UPDATE_RES:
                    UpdateResPack updateResP = (UpdateResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Update Res,Status is %s, call id is %s",updateResP.receiver,ProtocolPacket.GetResString(updateResP.status),updateResP.callid);
                    phone.UpdateCallStatus(updateResP);
                    break;
                case ProtocolPacket.DEV_QUERY_RES:
                    DevQueryResPack devResP = (DevQueryResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv DevQuery Res",devResP.receiver);
                    phone.UpdateDevLists(devResP);
                    break;
            }
        }

    }

    private void PacketTimeOverProcess(ProtocolPacket packet){
        TerminalPhone phone = GetDevice(packet.sender);
        if(phone!=null){
            switch(packet.type){
                case ProtocolPacket.REG_REQ:
                    phone.UpdateRegStatus(ProtocolPacket.STATUS_TIMEOVER);
                    break;
                case ProtocolPacket.CALL_REQ:
                case ProtocolPacket.ANSWER_REQ:
                case ProtocolPacket.END_REQ:
                case ProtocolPacket.CALL_UPDATE_REQ:
                    phone.UpdateCalTimeOver(packet);
                    break;
            }
        }

    }

    private class TerminalPhoneThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            phoneManagerMsgHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(@NotNull Message msg) {
                    ProtocolPacket packet;
                    int type = msg.arg1;
                    synchronized (this) {
                        switch (type) {
                            case MSG_NEW_PACKET:
                                packet = (ProtocolPacket) msg.obj;
                                PacketRecvProcess(packet);
                                break;
                            case MSG_SECOND_TICK:
                                for (TerminalPhone phone : clientPhoneLists.values()) {
                                    phone.UpdateSecondTick();
                                }
                                break;
                            case MSG_REQ_TIMEOVER:
                                packet = (ProtocolPacket) msg.obj;
                                PacketTimeOverProcess(packet);
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + type);
                        }
                    }
                    return false;
                }
            });
            Looper.loop();
        }
    }

}
