package com.example.nettytest.terminal.terminalcall;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
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
import com.example.nettytest.userinterface.TerminalDeviceInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class TerminalCallManager {

    HashMap<String, TerminalCall> callLists;
    int devType;

    public TerminalCallManager(int devType){
        this.devType = devType;
        callLists = new HashMap<>();
    }

    public int GetCallCount(){
        return callLists.size();
    }

    public byte[] MakeCallSnap(String devId,boolean isReg){
        JSONObject json = new JSONObject();
        try {
            json.putOpt(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_TERMINAL_CALL_RES);
            JSONArray callArray = new JSONArray();
            json.putOpt(SystemSnap.SNAP_DEVID_NAME,devId);
            if(isReg)
                json.putOpt(SystemSnap.SNAP_REG_NAME,1);
            else
                json.putOpt(SystemSnap.SNAP_REG_NAME,0);
            for(TerminalCall call:callLists.values()){
                JSONObject callJson = new JSONObject();
                callJson.putOpt(SystemSnap.SNAP_CALLID_NAME,call.callID);
                callJson.putOpt(SystemSnap.SNAP_CALLER_NAME,call.caller);
                callJson.putOpt(SystemSnap.SNAP_CALLEE_NAME,call.callee);
                callJson.putOpt(SystemSnap.SNAP_ANSWERER_NAME,call.answer);
                callJson.putOpt(SystemSnap.SNAP_CALLSTATUS_NAME,call.state);
                callArray.put(callJson);
            }
            json.putOpt(SystemSnap.SNAP_CALLS_NAME,callArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json.toString().getBytes();
    }

    public int EndCall(String callid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalCall call = callLists.get(callid);
        if(call!=null){
            result = call.EndCall();
            callLists.remove(callid); // must del there, otherwise call will auto update
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

    public String BuildCall(String devID, TerminalDeviceInfo info,String dstID, int callType){
        String callid;
        TerminalCall call;
        if (!callLists.isEmpty()) {
            return null;
        }
        call = new TerminalCall(devID,info,dstID,callType);
        if(devType== TerminalPhone.NURSE_CALL_DEVICE)
            call.direct = CommonCall.CALL_DIRECT_M2S;
        else
            call.direct = CommonCall.CALL_DIRECT_S2M;

        callLists.put(call.callID, call);
        callid = call.callID;
        return callid;
    }

    public void RecvIncomingCall(InviteReqPack packet){
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
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"DEV %s Reject Call From %s for reason %s",packet.callee,packet.caller,ProtocolPacket.GetResString(result));
            InviteResPack inviteResPack = new InviteResPack(result,packet);
            Transaction trans = new Transaction(packet.callee,packet,inviteResPack,Transaction.TRANSCATION_DIRECTION_C2S);
            HandlerMgr.AddPhoneTrans(packet.msgID, trans);
        }

    }

    public void RecvAnswerCall(String devid,AnswerReqPack answerReqPack){
        String callid = answerReqPack.callID;
        TerminalCall call = callLists.get(callid);
        
        if(call!=null){
            call.RecvAnswer(answerReqPack);
        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone Recv %s Answer For Call %s , but Could not Find it",devid,callid);
            AnswerResPack answerResP = new AnswerResPack(ProtocolPacket.STATUS_NOTFOUND,answerReqPack);
            Transaction trans = new Transaction(devid,answerReqPack,answerResP,Transaction.TRANSCATION_DIRECTION_C2S);
            HandlerMgr.AddPhoneTrans(answerResP.msgID,trans);

        }
    }

    public void RecvEndCall(String devid,EndReqPack endReqP){
        String callid = endReqP.callID;
        TerminalCall call = callLists.get(callid);

        if(call!=null){
            call.RecvEnd(endReqP);
            callLists.remove(callid);
        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone Recv %s End For Call %s , but Could not Find it",devid,callid);
            EndResPack endResP = new EndResPack(ProtocolPacket.STATUS_NOTFOUND,endReqP);
            Transaction trans = new Transaction(devid,endReqP,endResP,Transaction.TRANSCATION_DIRECTION_C2S);
            HandlerMgr.AddPhoneTrans(endResP.msgID,trans);
        }
    }

    public void UpdateSecondTick(){
        for(TerminalCall call:callLists.values()){
            call.UpdateSecondTick();
        }
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
                    call.UpdateByInviteRes(inviteResPack);
                    if(call.state!=CommonCall.CALL_STATE_RINGING)
                        callLists.remove(callid);
                }else{
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Could not Find Call %s for DEV %s",inviteResPack.callID,devid);
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"DEV %s have %d Calls",devid,callLists.size());
                    for(TerminalCall testCall:callLists.values()){
                        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"DEV %s has Call From %s to %s, callid is %s",devid,testCall.caller,testCall.callee,testCall.callID);
                    }
                }
                break;
            case ProtocolPacket.ANSWER_RES:
                AnswerResPack answerResPack = (AnswerResPack)packet;
                callid = answerResPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.UpdateByAnswerRes(answerResPack);
                    if(call.state!=CommonCall.CALL_STATE_CONNECTED)
                        callLists.remove(callid);
                }else{
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Could not Find Call %s for DEV %s when Recv Answer Res",answerResPack.callID,devid);
                }
                break;
            case ProtocolPacket.END_RES:
                EndResPack endResP = (EndResPack)packet;
                callid = endResP.callId;
                call = callLists.get(callid);
                if(call!=null){
                    call.UpdateByEndRes(endResP);
                    callLists.remove(callid);
                }else{
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Could not Find Call %s for DEV %s when Recv End Res",endResP.callId,devid);
                }
                break;
            case ProtocolPacket.CALL_UPDATE_RES:
                UpdateResPack updateResP = (UpdateResPack)packet;
                callid = updateResP.callid;
                call = callLists.get(callid);
                if(call!=null){
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Call Update Res for callid %s,Status is %s",call.devID,callid,ProtocolPacket.GetResString(updateResP.status));
                    call.UpdateByUpdateRes(updateResP);
                    if(call.state==CommonCall.CALL_STATE_DISCONNECTED){
                        callLists.remove(callid);
                    }
                }else{
                    LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Could not Find Call %s for DEV %s when Recv Update Res",updateResP.callid,devid);
                }
                break;
        }
    }

    public void UpdateTimeOver(String devid,ProtocolPacket packet) {
        String callid;
        TerminalCall call;

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"DEV %s TimerOver for %s Req",devid,ProtocolPacket.GetTypeName(packet.type));
        switch(packet.type){
            case ProtocolPacket.CALL_REQ:
                InviteReqPack inviteReqPack = (InviteReqPack)packet;
                callid = inviteReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.InviteTimeOver();
                    callLists.remove(callid);
                }
                break;
            case ProtocolPacket.ANSWER_REQ:
                AnswerReqPack answerReqPack = (AnswerReqPack)packet;
                callid = answerReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.AnswerTimeOver();
                }
                break;
            case ProtocolPacket.CALL_UPDATE_REQ:
                UpdateReqPack updateReqP = (UpdateReqPack)packet;
                callid = updateReqP.callId;
                call = callLists.get(callid);
                if(call!=null){
                    call.UpdateTimeOver();
                    callLists.remove(callid);
                }
                break;
            case ProtocolPacket.END_REQ:
                EndReqPack endReqPack = (EndReqPack)packet;
                callid = endReqPack.callID;
                call = callLists.get(callid);
                if(call!=null){
                    call.EndTimeOver();
                    callLists.remove(callid);
                }
                break;
        }

    }

}
