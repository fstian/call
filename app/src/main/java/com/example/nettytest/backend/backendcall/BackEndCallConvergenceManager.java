package com.example.nettytest.backend.backendcall;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.AnswerVideoReqPack;
import com.example.nettytest.pub.protocol.AnswerVideoResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.StartVideoReqPack;
import com.example.nettytest.pub.protocol.StartVideoResPack;
import com.example.nettytest.pub.protocol.StopVideoReqPack;
import com.example.nettytest.pub.protocol.StopVideoResPack;
import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.userinterface.CallLogMessage;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserMessage;

import java.util.HashMap;

public class BackEndCallConvergenceManager {

    HashMap<String, BackEndCallConvergence> callConvergenceList;


    public BackEndCallConvergenceManager(){

        callConvergenceList = new HashMap<>();
    }



    public int GetCallCount(){
        return callConvergenceList.size();
    }

    public byte[] MakeCallConvergenceSnap(String devid){
        byte[] result = null;
        JSONObject json = new JSONObject();
        BackEndPhone phone;

        phone = HandlerMgr.GetBackEndPhone(devid);

        try {
            json.put(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_BACKEND_CALL_RES);
            json.put(SystemSnap.SNAP_DEVID_NAME, devid);
            json.put(SystemSnap.SNAP_VER_NAME,PhoneParam.VER_STR);
            json.put(SystemSnap.SNAP_RUN_TIME_NAME,HandlerMgr.GetBackEndRunSecond());
            json.put(SystemSnap.SNAP_LISTEN_STATUS_NAME,phone.enableListen);
            
            JSONArray outCalls = new JSONArray();
            JSONArray incomingCalls = new JSONArray();
            JSONObject calljson;
            for (BackEndCallConvergence callConvergence:callConvergenceList.values()) {
                if(callConvergence.inviteCall.caller.compareToIgnoreCase(devid)==0){
                    calljson = new JSONObject();
                    calljson.put(SystemSnap.SNAP_CALLER_NAME,callConvergence.inviteCall.caller);
                    calljson.put(SystemSnap.SNAP_CALLEE_NAME,callConvergence.inviteCall.callee);
                    calljson.put(SystemSnap.SNAP_ANSWERER_NAME,callConvergence.inviteCall.answer);
                    calljson.put(SystemSnap.SNAP_CALLID_NAME,callConvergence.inviteCall.callID);
                    calljson.put(SystemSnap.SNAP_CALLSTATUS_NAME,callConvergence.inviteCall.state);
                    outCalls.add(calljson);
                }

                if(callConvergence.inviteCall.callee.compareToIgnoreCase(devid)==0||callConvergence.inviteCall.answer.compareToIgnoreCase(devid)==0){
                    calljson = new JSONObject();
                    calljson.put(SystemSnap.SNAP_CALLER_NAME,callConvergence.inviteCall.caller);
                    calljson.put(SystemSnap.SNAP_CALLID_NAME,callConvergence.inviteCall.callID);
                    calljson.put(SystemSnap.SNAP_CALLSTATUS_NAME,callConvergence.inviteCall.state);
                    incomingCalls.add(calljson);
                }

                for(BackEndCall call:callConvergence.listenCallList){
                    if(call.devID.compareToIgnoreCase(devid)==0){
                        calljson = new JSONObject();
                        calljson.put(SystemSnap.SNAP_CALLER_NAME,call.caller);
                        calljson.put(SystemSnap.SNAP_CALLID_NAME,call.callID);
                        calljson.put(SystemSnap.SNAP_CALLSTATUS_NAME,call.state);
                        incomingCalls.add(calljson);
                    }
                }
            }
            json.put(SystemSnap.SNAP_OUTGOINGS_NAME,outCalls);
            json.put(SystemSnap.SNAP_INCOMINGS_NAME,incomingCalls);
            result = json.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        json.clear();

        return result;
    }

    private boolean CheckDevTypeInviteEnable(int type){
        boolean result = true;
        if(type== PhoneDevice.CORRIDOR_CALL_DEVICE||
            type==PhoneDevice.TV_CALL_DEVICE||
            type==PhoneDevice.DOOR_CALL_DEVICE||
            type==PhoneDevice.WHITE_BOARD_DEVICE||
            type==PhoneDevice.DOOR_LIGHT_CALL_DEVICE){
            result = false;
        }
        return result;
    }

    private boolean CheckInviteEnable(BackEndPhone phone){
        boolean result = true;
        if(phone==null)
            return  false;
        if(!phone.isReg)
            return false;
        if(!CheckDevTypeInviteEnable(phone.type)){
            LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Dev %s Type is %d, Not Support Build Call",phone.id,phone.type);
            result = false;
        }else{
            for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
                if(!callConvergence.CheckInviteEnable(phone)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

        private boolean CheckDevTypeAnswerEnable(int type){
        boolean result = true;
        if(type==PhoneDevice.TV_CALL_DEVICE||
            type==PhoneDevice.WHITE_BOARD_DEVICE||
            type==PhoneDevice.DOOR_LIGHT_CALL_DEVICE||
            type==PhoneDevice.EMER_CALL_DEVICE){

            result = false;
        }
        return result;
    }


    private boolean CheckAnswerEnable(BackEndPhone phone,String callid){
        boolean result = true;
        if(phone == null)
            return false;
        if(!phone.isReg)
            return false;
        if(!CheckDevTypeAnswerEnable(phone.type)){
            LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Dev %s Type is %d, Not Support Answer Call",phone.id,phone.type);
            return false;
        }
        BackEndCallConvergence curCallConvergence = callConvergenceList.get(callid);
        if(curCallConvergence==null)
            return false;
        BackEndPhone callerPhone = HandlerMgr.GetBackEndPhone(curCallConvergence.GetCallerId());
        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            if(!callConvergence.CheckAnswerEnable(phone,callerPhone,callid)) {
                result = false;
                break;
            }
        }
        return result;
    }

//    public boolean CheckForwardEnable(BackEndPhone phone,int callType){
//        boolean result = true;
//        if(phone == null)
//            return false;
//        if(!phone.isReg)
//            return false;
//        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
//            if(!callConvergence.CheckForwardEnable(phone,callType)) {
//                result = false;
//                break;
//            }
//        }
//        return result;
//    }

    public boolean CheckListenEnabled(BackEndPhone phone){
        boolean result = true;
        if(phone==null)
            return  false;
        if(!phone.isReg)
            return false;

        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            if(!callConvergence.CheckListenEnabled(phone)) {
                result = false;
                break;
            }
        }

        return result;
    }
    
    public boolean CheckBroadCastEnabled(BackEndPhone phone){
        boolean result = true;
        if(phone==null)
            return  false;
        if(!phone.isReg)
            return false;

        for(BackEndCallConvergence callConvergence:callConvergenceList.values()){
            if(!callConvergence.CheckBroadCastEnabled(phone)) {
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
        String callid;
        switch (packet.type) {
            case ProtocolPacket.CALL_REQ:
                InviteReqPack inviteReqPack = (InviteReqPack) packet;
                int  resultCode = ProtocolPacket.STATUS_OK;
                BackEndPhone caller = HandlerMgr.GetBackEndPhone(inviteReqPack.caller);
                BackEndPhone callee = HandlerMgr.GetBackEndPhone(inviteReqPack.callee);
                if(CheckInviteEnable(caller)) {

                    inviteReqPack.roomId = caller.devInfo.roomId;
                    inviteReqPack.bedName = caller.devInfo.bedName;
                    inviteReqPack.deviceName = caller.devInfo.deviceName;
                    inviteReqPack.areaId = caller.devInfo.areaId;
                    inviteReqPack.areaName = caller.devInfo.areaName;

                    if(inviteReqPack.callee.compareToIgnoreCase(PhoneParam.CALL_SERVER_ID)==0){
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv %s Req from %s to %s",CommonCall.GetCallTypeName(inviteReqPack.callType),caller.id,PhoneParam.CALL_SERVER_ID);
                        callConvergence = new BackEndCallConvergence(caller,inviteReqPack);
                        callConvergenceList.put(inviteReqPack.callID,callConvergence);
                    }else if(CheckInvitedEnable(callee)){
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call Req from %s to %s",caller.id,callee.id);
                        callConvergence = new BackEndCallConvergence(caller,callee,inviteReqPack);
                        callConvergenceList.put(inviteReqPack.callID,callConvergence);
                    }else{
                        if(callee==null){
                            LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Server Could not Find callee DEV %s",inviteReqPack.callee);
                            resultCode = ProtocolPacket.STATUS_NOTFOUND;
                        }else {
                            LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_INFO, "Server Find DEV %s is not Reg or is Busy", inviteReqPack.callee);
                            resultCode = ProtocolPacket.STATUS_DECLINE;
                        }
                    }
                }else{
                    if(caller==null){
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"Server Could not Find Caller DEV %s",inviteReqPack.caller);
                        resultCode = ProtocolPacket.STATUS_NOTFOUND;
                    }else {
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_INFO, "Server Find DEV %s is not Reg or Busy", inviteReqPack.caller);
                        resultCode = ProtocolPacket.STATUS_DECLINE;
                    }
                }

                if(resultCode!=ProtocolPacket.STATUS_OK){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_WARN,"Server Reject Call From %s to %s for %s",inviteReqPack.caller,inviteReqPack.callee,ProtocolPacket.GetResString(resultCode));
                    InviteResPack inviteResPack = new InviteResPack(resultCode,inviteReqPack);
                    trans = new Transaction(inviteReqPack.caller,packet,inviteResPack,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(packet.msgID, trans);
                }
                break;
            case ProtocolPacket.END_REQ:
                EndReqPack endReqPack = (EndReqPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call End From %s for Call %s",endReqPack.endDevID,endReqPack.callID);
                callConvergence = callConvergenceList.get(endReqPack.callID);
                
                if(callConvergence!=null) {
                    if(callConvergence.inviteCall.type==CommonCall.CALL_TYPE_BROADCAST) {
                        if(endReqPack.endDevID.compareToIgnoreCase(callConvergence.inviteCall.caller)==0||endReqPack.endDevID.compareToIgnoreCase(PhoneParam.CALL_SERVER_ID)==0){
                            LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server End Call %s",endReqPack.callID);
                            callConvergence.EndCall(endReqPack);
                            CallLogMessage  log = callConvergence.CreateCallLog();
                            PostUserMessage(UserMessage.MESSAGE_BACKEND_CALL_LOG,log);
                            callConvergenceList.remove(endReqPack.callID);
                            callConvergence.Release();
                        }else{
                            LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Remove %s From Call %s",endReqPack.endDevID,endReqPack.callID);
                            callConvergence.RecvSingleEnd(endReqPack);
                        }
                    }else if(callConvergence.inviteCall.type==CommonCall.CALL_TYPE_NORMAL||callConvergence.inviteCall.type==CommonCall.CALL_TYPE_EMERGENCY||callConvergence.inviteCall.type==CommonCall.CALL_TYPE_ASSIST) {
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server End Call %s",endReqPack.callID);
                        callConvergence.EndCall(endReqPack);
                        CallLogMessage  log = callConvergence.CreateCallLog();
                        PostUserMessage(UserMessage.MESSAGE_BACKEND_CALL_LOG,log);
                        callConvergenceList.remove(endReqPack.callID);
                        callConvergence.Release();
                    }
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
                if(answerPhone!=null){
                    answerReqPack.answerBedName = answerPhone.devInfo.bedName;
                    answerReqPack.answerDeviceName = answerPhone.devInfo.deviceName;
                    answerReqPack.answerRoomId = answerPhone.devInfo.roomId;
                }
                callConvergence = callConvergenceList.get(answerReqPack.callID);
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Call Answer From Dev %s for Call %s",answerReqPack.answerer,answerReqPack.callID);
                error = ProtocolPacket.STATUS_OK;
                if(callConvergence==null){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Server Recv Call Answer From %s for CallID %s, But Could not Find this Call", answerReqPack.answerer, answerReqPack.callID);
                    error = ProtocolPacket.STATUS_NOTFOUND;
                }else if(callConvergence.inviteCall.callType==CommonCall.CALL_TYPE_EMERGENCY){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Server Recv Call Answer From %s for CallID %s, But Type is %d , Couldn't be Answered", answerReqPack.answerer, answerReqPack.callID,callConvergence.inviteCall.callType);
                    error = ProtocolPacket.STATUS_NOTSUPPORT;
                }else{
                    if(callConvergence.inviteCall.callType==CommonCall.CALL_TYPE_BROADCAST){
                        callConvergence.AnswerBroadCall(answerReqPack);
                    }else{
                        if(CheckAnswerEnable(answerPhone,answerReqPack.callID)) {
                            callConvergence.AnswerCall(answerReqPack);
                        }else{
                            if(answerPhone==null) {
                                error = ProtocolPacket.STATUS_NOTFOUND;
                                LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Server Could not Find DEV %s", answerReqPack.answerer);
                            }else{
                                error = ProtocolPacket.STATUS_FORBID;
                                LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_WARN, "Server Reject Answer From %s for call %s", answerReqPack.answerer, answerReqPack.callID);
                            }
                        }
                    }
                }
                if(error!=ProtocolPacket.STATUS_OK){
                    AnswerResPack answerResPack = new AnswerResPack(error,answerReqPack);
                    trans = new Transaction(answerReqPack.answerer,answerReqPack,answerResPack,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(answerReqPack.msgID, trans);
                }
                break;
            case ProtocolPacket.CALL_UPDATE_REQ:
                UpdateReqPack updateReqP = (UpdateReqPack)packet;
                callid = updateReqP.callId;
                callConvergence = callConvergenceList.get(callid);
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv Update From Dev %s for Call %s",updateReqP.devId,updateReqP.callId);
                if(callConvergence==null){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_WARN,"Server Could not Find Call %s",callid);
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
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_WARN,"Server Recv Call Res From %s for call %s, but could not Find this Call ",inviteResPack.sender, inviteResPack.callID);
                break;
            case ProtocolPacket.END_RES:
                EndResPack endResPack = (EndResPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv End Res From %s for call %s",endResPack.sender, endResPack.callId);
                break;
            case ProtocolPacket.CALL_VIDEO_INVITE_REQ:
                StartVideoReqPack startReqPack = (StartVideoReqPack)packet;
                callid = startReqPack.callID;
                callConvergence = callConvergenceList.get(callid);
                if(callConvergence==null){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_WARN,"Server Could not Find Call %s",callid);
                    error = ProtocolPacket.STATUS_NOTFOUND;
                    StartVideoResPack startVideoResP = new StartVideoResPack(error,startReqPack);
                    trans = new Transaction(startReqPack.startVideoDevId,startReqPack,startVideoResP,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(startReqPack.msgID, trans);
                }else{
                    callConvergence.StartVideo(startReqPack);
                }
                break;
            case ProtocolPacket.CALL_VIDEO_INVITE_RES:
                StartVideoResPack startResP= (StartVideoResPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv StartVideo Res From %s for call %s, the video requier is %s",startResP.sender, startResP.callid,startResP.startVideoDevId);
                break;
            case ProtocolPacket.CALL_VIDEO_ANSWER_REQ:
                AnswerVideoReqPack answerVideoReq = (AnswerVideoReqPack)packet;
                callid = answerVideoReq.callId;
                callConvergence = callConvergenceList.get(callid);
                if(callConvergence==null){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_WARN,"Server Could not Find Call %s",callid);
                    error = ProtocolPacket.STATUS_NOTFOUND;
                    AnswerVideoResPack answerVideoResP = new AnswerVideoResPack(error,answerVideoReq);
                    trans = new Transaction(answerVideoReq.answerDevId,answerVideoReq,answerVideoResP,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(answerVideoReq.msgID, trans);
                }else{
                    callConvergence.AnswerVideo(answerVideoReq);
                }
                break;
            case ProtocolPacket.CALL_VIDEO_ANSWER_RES:
                AnswerVideoResPack answerResP= (AnswerVideoResPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv answerVideo Res From %s for call %s, the video requier is %s",answerResP.sender, answerResP.callId,answerResP.answerVideoDevId);
                break;
            case ProtocolPacket.CALL_VIDEO_END_REQ:
                StopVideoReqPack stopVideoReqP = (StopVideoReqPack)packet;
                callid = stopVideoReqP.callID;
                callConvergence = callConvergenceList.get(callid);
                if(callConvergence==null){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_WARN,"Server Could not Find Call %s",callid);
                    error = ProtocolPacket.STATUS_NOTFOUND;
                    StopVideoResPack stopVideoResP = new StopVideoResPack(error,stopVideoReqP);
                    trans = new Transaction(stopVideoReqP.stopVideoDevId,stopVideoReqP,stopVideoResP,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(stopVideoResP.msgID, trans);
                }else{
                    callConvergence.StopVideo(stopVideoReqP);
                }
                break;
            case ProtocolPacket.CALL_VIDEO_END_RES:
                StopVideoResPack stopVideoResP= (StopVideoResPack)packet;
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_DEBUG,"Server Recv StopVideo Res From %s for call %s, the video requier is %s",stopVideoResP.sender, stopVideoResP.callid,stopVideoResP.stopVideoDevId);
                break;
        }
    }

    public void ProcessTimeOver(ProtocolPacket packet){
        BackEndCallConvergence callConvergence;
        if (packet.type == ProtocolPacket.CALL_REQ) {
            InviteReqPack inviteReqPack = (InviteReqPack) packet;
            callConvergence = callConvergenceList.get(inviteReqPack.callID);
            if (callConvergence != null) {
                LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_DEBUG, "Server Send Call Req to Dev %s TimeOver for Call %s", packet.receiver, inviteReqPack.callID);
                callConvergence.InviteTimeOver(inviteReqPack.receiver);
            }
        }
        
    }
    

    private void PostUserMessage(int type,Object obj){
        HandlerMgr.PostBackEndUserMsg(type,obj);
    }

}

