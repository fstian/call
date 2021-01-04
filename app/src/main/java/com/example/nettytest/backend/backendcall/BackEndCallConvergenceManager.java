package com.example.nettytest.backend.backendcall;

import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
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
import com.example.nettytest.userinterface.PhoneParam;

import java.util.HashMap;

public class BackEndCallConvergenceManager {

    HashMap<String, BackEndCallConvergence> callConvergenceList;

    public BackEndCallConvergenceManager(){
        callConvergenceList = new HashMap<>();
    }

    private boolean CheckInviteEnable(BackEndPhone phone){
        boolean result = true;
        if(phone==null)
            return  false;
        if(!phone.isReg)
            return false;
        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            if(!callConvergence.CheckInviteEnable(phone)) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean CheckAnswerEnable(BackEndPhone phone,String callid){
        boolean result = true;
        if(phone == null)
            return false;
        if(!phone.isReg)
            return false;
        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            if(!callConvergence.CheckAnswerEnable(phone,callid)) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean CheckInvitedEnable(BackEndPhone phone){
        boolean result = true;
        if(phone==null)
            return  false;
        if(!phone.isReg)
            return false;
        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            if(!callConvergence.CheckInvitedEnable(phone)) {
                result = false;
                break;
            }
        }
        return result;
    }

    public void ProcessSecondTick(){
        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            callConvergence.ProcessSecondTick();
        }
    }


    public void ProcessPacket(ProtocolPacket packet){
        BackEndCallConvergence callConvergence;
        Transaction trans;
        int error;
        switch (packet.type) {
            case ProtocolPacket.CALL_REQ:
                InviteReqPack inviteReqPack = (InviteReqPack) packet;
                int  resultCode = ProtocolPacket.STATUS_OK;
                BackEndPhone caller = HandlerMgr.GetBackEndPhone(inviteReqPack.caller);
                BackEndPhone callee = HandlerMgr.GetBackEndPhone(inviteReqPack.callee);
                if(CheckInviteEnable(caller)) {
                    if(inviteReqPack.callee.compareToIgnoreCase(PhoneParam.CALL_SERVER_ID)==0){
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call Req from %s to %s",caller.id,PhoneParam.CALL_SERVER_ID);
                        callConvergence = new BackEndCallConvergence(caller,inviteReqPack);
                        callConvergenceList.put(inviteReqPack.callID,callConvergence);
                    }else if(CheckInvitedEnable(callee)){
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call Req from %s to %s",caller.id,callee.id);
                        callConvergence = new BackEndCallConvergence(caller,callee,inviteReqPack);
                        callConvergenceList.put(inviteReqPack.callID,callConvergence);
                    }else{
                        if(callee==null){
                            LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_INFO,"Server Could not Find DEV %s",inviteReqPack.callee);
                            resultCode = ProtocolPacket.STATUS_NOTFOUND;
                        }else {
                            LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_INFO, "Server Find DEV %s is busy", inviteReqPack.callee);
                            resultCode = ProtocolPacket.STATUS_DECLINE;
                        }
                    }
                }else{
                    if(caller==null){
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_INFO,"Server Could not Find DEV %s",inviteReqPack.caller);
                        resultCode = ProtocolPacket.STATUS_NOTFOUND;
                    }else {
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_INFO, "Server Find DEV %s is busy", inviteReqPack.caller);
                        resultCode = ProtocolPacket.STATUS_DECLINE;
                    }
                }

                if(resultCode!=ProtocolPacket.STATUS_OK){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Reject Call From %s to %s for %s",inviteReqPack.caller,inviteReqPack.callee,ProtocolPacket.GetResString(resultCode));
                    InviteResPack inviteResPack = new InviteResPack(resultCode,inviteReqPack);
                    trans = new Transaction(inviteReqPack.caller,packet,inviteResPack,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(packet.msgID, trans);
                }
                break;
            case ProtocolPacket.END_REQ:
                EndReqPack endReqPack = (EndReqPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call End From %s",endReqPack.endDevID);
                callConvergence = callConvergenceList.get(endReqPack.callID);
                if(callConvergence!=null) {
                    callConvergence.EndCall(endReqPack);
                    callConvergenceList.remove(endReqPack.callID);
                }else{
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Server Recv Call End From %s for CallID %s, But Could not Find this Call",endReqPack.endDevID,endReqPack.callID);
                    EndResPack endResP = new EndResPack(ProtocolPacket.STATUS_NOTFOUND,endReqPack);
                    trans = new Transaction(endReqPack.sender,endReqPack,endResP,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(endReqPack.msgID,trans);
                }
                break;
            case ProtocolPacket.ANSWER_REQ:
                AnswerReqPack answerReqPack  = (AnswerReqPack)packet;
                BackEndPhone answerPhone = HandlerMgr.GetBackEndPhone(answerReqPack.answerer);
                if(CheckAnswerEnable(answerPhone,answerReqPack.callID)) {
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call Answer %s",answerReqPack.answerer);
                    callConvergence = callConvergenceList.get(answerReqPack.callID);
                    if (callConvergence != null) {
                        callConvergence.AnswerCall(answerReqPack);
                    } else {
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Server Recv Call Answer From %s for CallID %s, But Could not Find this Call", answerReqPack.answerer, answerReqPack.callID);
                    }
                }else{
                    error = ProtocolPacket.STATUS_NOTFOUND;
                    if(answerPhone==null) {
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Server Could not Find DEV %s", answerReqPack.answerer);
                    }else{
                        error = ProtocolPacket.STATUS_FORBID;
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Server Reject Answer From %s for call %s", answerReqPack.answerer, answerReqPack.callID);
                    }
                    AnswerResPack answerResPack = new AnswerResPack(error,answerReqPack);
                    trans = new Transaction(answerReqPack.answerer,answerReqPack,answerResPack,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(answerReqPack.msgID, trans);
                }
                break;
            case ProtocolPacket.CALL_UPDATE_REQ:
                UpdateReqPack updateReqP = (UpdateReqPack)packet;
                String callid = updateReqP.callId;
                callConvergence = callConvergenceList.get(callid);
                if(callConvergence==null){
                    error = ProtocolPacket.STATUS_NOTFOUND;
                    UpdateResPack updateResP = new UpdateResPack(error,updateReqP);
                    trans = new Transaction(updateReqP.devId,updateReqP,updateResP,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(updateReqP.msgID, trans);
                }else{
                    callConvergence.UpdateCall(updateReqP);
                }
                break;
            case ProtocolPacket.CALL_RES:
                InviteResPack inviteResPack = (InviteResPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call Res From %s for call %s",inviteResPack.sender, inviteResPack.callID);
                callConvergence = callConvergenceList.get(inviteResPack.callID);
                if(callConvergence!=null)
                    callConvergence.UpdateStatus(inviteResPack);
                else
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Server Recv Call Res From %s for call %s, but could not Find this Call ",inviteResPack.sender, inviteResPack.callID);
                break;
            case ProtocolPacket.END_RES:
                EndResPack endResPack = (EndResPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv End Res From %s for call %s",endResPack.sender, endResPack.callID);
                break;
        }
    }
}
