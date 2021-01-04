package com.example.nettytest.terminal.terminalcall;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;

import java.util.HashMap;

public class TerminalCallManager {

    HashMap<String, TerminalCall> callLists;
    int devType;

    public TerminalCallManager(int devType){
        this.devType = devType;
        callLists = new HashMap<>();
    }

    public int EndCall(String callid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalCall call = callLists.get(callid);
        if(call!=null){
            result = call.EndCall();
            callLists.remove(callid);
        }

        return result;
    }

    public int AnswerCall(String callid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalCall call = callLists.get(callid);
        if(call!=null){
            result = call.Answer();
        }

        return result;
    }

    public String BuildCall(String devID,String dstID,int callType){
        String callid;
        TerminalCall call;
        if (!callLists.isEmpty()) {
            return null;
        }
        call = new TerminalCall(devID,dstID,callType);
        if(devType== TerminalPhone.NURSE_CALL_DEVICE)
            call.direct = CommonCall.CALL_DIRECT_M2S;
        else
            call.direct = CommonCall.CALL_DIRECT_S2M;

        callLists.put(call.callID, call);
        callid = call.callID;
        return callid;
    }

    public int RecvIncomingCall(InviteReqPack packet){
        int result = ProtocolPacket.STATUS_OK;
        TerminalCall call;

        if(devType==TerminalPhone.BED_CALL_DEVICE){
            if(callLists.size()>0)
                result = ProtocolPacket.STATUS_BUSY;
        }else if(devType==TerminalPhone.EMER_CALL_DEVICE){
            result = ProtocolPacket.STATUS_NOTFOUND;
        }

        if(result==ProtocolPacket.STATUS_OK){
            call = new TerminalCall(packet);
            if(devType==TerminalPhone.NURSE_CALL_DEVICE){
                call.direct = CommonCall.CALL_DIRECT_M2S;
            }else{
                call.direct = CommonCall.CALL_DIRECT_S2M;
            }
            callLists.put(call.callID,call);
        }

        if(result!=ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_ERROR,"DEV %s Reject Call From %s for reason %s",packet.callee,packet.caller,ProtocolPacket.GetResString(result));
            InviteResPack inviteResPack = new InviteResPack(result,packet);
            Transaction trans = new Transaction(packet.callee,packet,inviteResPack,Transaction.TRANSCATION_DIRECTION_S2C);
            HandlerMgr.AddPhoneTrans(packet.msgID, trans);
        }

        return result;
    }

    public boolean UpdateSecondTick(){
        for(TerminalCall call:callLists.values()){
            call.UpdateSecondTick();
        }
        return true;
    }

    public void UpdateStatus(String devid, ProtocolPacket packet){

        String callid;
        TerminalCall call;
        switch(packet.type){
            case ProtocolPacket.CALL_RES:
                InviteResPack inviteResPack = (InviteResPack)packet;
                callid = inviteResPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.UpdateCallStatus(inviteResPack);
                    if(call.state!=CommonCall.CALL_STATE_RINGING)
                        callLists.remove(callid);
                }else{
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_ERROR,"Could not Find Call %s, cur has Call",inviteResPack.callID);
                    for(TerminalCall testCall:callLists.values()){
                        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_ERROR,"Call From %s to %s, callid is %s",testCall.caller,testCall.callee,testCall.callID);
                    }
                }
                break;
            case ProtocolPacket.CALL_UPDATE_RES:
                UpdateResPack updateResP = (UpdateResPack)packet;
                callid = updateResP.callid;
                call = callLists.get(callid);
                if(call!=null){
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Call Update Res for callid %s,Status is %s",call.devID,callid,ProtocolPacket.GetResString(updateResP.status));
                    call.UpdateCallStatus(updateResP);
                    if(call.state==CommonCall.CALL_STATE_DISCONNECTED){
                        callLists.remove(callid);
                    }
                }
                break;
            case ProtocolPacket.END_REQ:
                EndReqPack endReqPack = (EndReqPack)packet;
                callid = endReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.Finish(endReqPack);
                    callLists.remove(callid);
                }else{
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Phone Recv %s End For Call %s , but Could not Find it",devid,callid);
                    EndResPack endResPack = new EndResPack(ProtocolPacket.STATUS_NOTFOUND,endReqPack);
                    Transaction trans = new Transaction(devid,endReqPack,endResPack,Transaction.TRANSCATION_DIRECTION_C2S);
                    HandlerMgr.AddPhoneTrans(endReqPack.msgID,trans);
                }
                break;
            case ProtocolPacket.ANSWER_REQ:
                AnswerReqPack answerReqPack = (AnswerReqPack)packet;
                callid = answerReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.UpdateCallStatus(answerReqPack);
                }else{
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Phone Recv %s Answer For Call %s , but Could not Find it",devid,callid);
                    AnswerResPack answerResP = new AnswerResPack(ProtocolPacket.STATUS_NOTFOUND,answerReqPack);
                    Transaction trans = new Transaction(devid,answerReqPack,answerResP,Transaction.TRANSCATION_DIRECTION_C2S);
                    HandlerMgr.AddPhoneTrans(answerResP.msgID,trans);

                }
                break;
        }
    }

    public void UpdateTimeOver(ProtocolPacket packet) {
        String callid;
        TerminalCall call;

        switch(packet.type){
            case ProtocolPacket.CALL_REQ:
                InviteReqPack inviteReqPack = (InviteReqPack)packet;
                callid = inviteReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.Fail(ProtocolPacket.CALL_REQ,ProtocolPacket.STATUS_TIMEOVER);
                    callLists.remove(callid);
                }
                break;
            case ProtocolPacket.ANSWER_REQ:
                AnswerReqPack answerReqPack = (AnswerReqPack)packet;
                callid = answerReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.Fail(ProtocolPacket.ANSWER_REQ,ProtocolPacket.STATUS_TIMEOVER);
                }
                break;
            case ProtocolPacket.CALL_UPDATE_REQ:
                UpdateReqPack updateReqP = (UpdateReqPack)packet;
                callid = updateReqP.callId;
                call = callLists.get(callid);
                if(call!=null){
                    call.Fail(ProtocolPacket.CALL_UPDATE_REQ,ProtocolPacket.STATUS_TIMEOVER);
                    if(call.caller.compareToIgnoreCase(updateReqP.devId)==0||call.callee.compareToIgnoreCase(updateReqP.devId)==0) {
                        call.EndCall();
                        callLists.remove(callid);
                    }
                }
                break;
            case ProtocolPacket.END_REQ:
                EndReqPack endReqPack = (EndReqPack)packet;
                callid = endReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.Fail(ProtocolPacket.END_REQ,ProtocolPacket.STATUS_TIMEOVER);
                    callLists.remove(callid);
                }
                break;
        }

    }

}
