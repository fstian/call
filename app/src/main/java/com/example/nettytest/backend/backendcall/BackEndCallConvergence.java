package com.example.nettytest.backend.backendcall;

import android.os.Message;

import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.backend.backendphone.BackEndPhoneManager;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.Transaction;

import java.util.ArrayList;

public class BackEndCallConvergence {
    BackEndCall inviteCall;

    ArrayList<BackEndCall> listenCallList;

    public BackEndCallConvergence(BackEndPhone caller, InviteReqPack pack) {
        BackEndCall listenCall;
        InviteReqPack invitePacket;
        ArrayList<BackEndPhone> listenDevices = HandlerMgr.GetBackEndListenDevices();

        InviteResPack inviteResP = new InviteResPack(ProtocolPacket.STATUS_OK,pack);

        Transaction transaction = new Transaction(caller.id,pack, inviteResP,Transaction.TRANSCATION_DIRECTION_S2C);
        HandlerMgr.AddBackEndTrans(pack.msgID,transaction);

        inviteCall = new BackEndCall(caller.id,pack);

        listenCallList = new ArrayList<>();
        for(BackEndPhone phone:listenDevices){
            if(CheckForwardEnable(phone)) {
                invitePacket = new InviteReqPack();
                invitePacket.ExchangeCopyData(pack);
                invitePacket.receiver = phone.id;
                invitePacket.msgID = UniqueIDManager.GetUniqueID(phone.id, UniqueIDManager.MSG_UNIQUE_ID);

                listenCall = new BackEndCall(phone.id, invitePacket);
                transaction = new Transaction(phone.id, invitePacket,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(invitePacket.msgID, transaction);
                listenCallList.add(listenCall);
            }
        }
        inviteCall.state = CommonCall.CALL_STATE_DIALING;
    }

    public BackEndCallConvergence(BackEndPhone caller,BackEndPhone callee, InviteReqPack pack){
        InviteReqPack invitePacket;

        InviteResPack inviteResP = new InviteResPack(ProtocolPacket.STATUS_OK,pack);

        Transaction transaction = new Transaction(caller.id,pack, inviteResP,Transaction.TRANSCATION_DIRECTION_S2C);
        HandlerMgr.AddBackEndTrans(pack.msgID,transaction);

        inviteCall = new BackEndCall(caller.id,pack);

        listenCallList = new ArrayList<>();
        if(callee!=null) {
            invitePacket = new InviteReqPack();
            invitePacket.ExchangeCopyData(pack);
            invitePacket.receiver = callee.id;
            invitePacket.msgID = UniqueIDManager.GetUniqueID(callee.id, UniqueIDManager.MSG_UNIQUE_ID);
            transaction = new Transaction(callee.id, invitePacket, Transaction.TRANSCATION_DIRECTION_S2C);
            HandlerMgr.AddBackEndTrans(invitePacket.msgID, transaction);
        }
        inviteCall.state = CommonCall.CALL_STATE_INCOMING;
    }

    public boolean EndCall(EndReqPack endReqP){
        EndReqPack endReqForwardP;
        EndResPack endResP;
        Transaction trans;

        endResP = new EndResPack(ProtocolPacket.STATUS_OK,endReqP);

        if(inviteCall.caller.compareToIgnoreCase(endReqP.sender)==0) {
            trans = new Transaction(inviteCall.caller, endReqP, endResP, Transaction.TRANSCATION_DIRECTION_S2C);
            HandlerMgr.AddBackEndTrans(endResP.msgID, trans);
        }else {
            endReqForwardP = new EndReqPack(endReqP,inviteCall.caller);
            trans = new Transaction(inviteCall.caller,endReqForwardP,Transaction.TRANSCATION_DIRECTION_S2C);
            HandlerMgr.AddBackEndTrans(endReqForwardP.msgID, trans);
        }

        if(inviteCall.state==CommonCall.CALL_STATE_CONNECTED){
            if(inviteCall.answer.compareToIgnoreCase(endReqP.sender)==0){
                trans = new Transaction(inviteCall.answer, endReqP, endResP, Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(endResP.msgID, trans);
            }else {
                endReqForwardP = new EndReqPack(endReqP, inviteCall.answer);
                trans = new Transaction(inviteCall.answer, endReqForwardP, Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(endReqForwardP.msgID, trans);
                inviteCall.answer = "";
            }
        }else {
            if (inviteCall.callee.compareToIgnoreCase(PhoneParam.CALL_SERVER_ID) != 0) {
                if (inviteCall.callee.compareToIgnoreCase(endReqP.sender) == 0) {
                    trans = new Transaction(inviteCall.callee, endReqP, endResP, Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(endResP.msgID, trans);
                } else {
                    endReqForwardP = new EndReqPack(endReqP, inviteCall.callee);
                    trans = new Transaction(inviteCall.callee, endReqForwardP, Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(endReqForwardP.msgID, trans);
                }
            }
        }

        for(CommonCall listenCall:listenCallList){
            if(listenCall.devID.compareToIgnoreCase(endReqP.sender)==0){
                trans = new Transaction(listenCall.devID,endReqP, endResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(endResP.msgID, trans);
            }else{
                endReqForwardP = new EndReqPack(endReqP,listenCall.devID);
                trans = new Transaction(listenCall.devID,endReqForwardP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(endReqForwardP.msgID, trans);
            }
        }

        listenCallList.clear();
        inviteCall.state = CommonCall.CALL_STATE_DISCONNECTED;
        return true;
    }

    public void UpdateCall(UpdateReqPack updateReqP){
        int status = ProtocolPacket.STATUS_NOTFOUND;
        String updateDevId = updateReqP.devId;
        Transaction trans;

        if(updateDevId.compareToIgnoreCase(inviteCall.caller)==0) {
            status = ProtocolPacket.STATUS_OK;
            inviteCall.callerWaitUpdateCount = 0;
        }
        if(updateDevId.compareToIgnoreCase(inviteCall.callee)==0) {
            status = ProtocolPacket.STATUS_OK;
            inviteCall.calleeWaitUpdateCount = 0;
        }
        if(updateDevId.compareToIgnoreCase(inviteCall.answer)==0) {
            status = ProtocolPacket.STATUS_OK;
            inviteCall.answerWaitUpdateCount = 0;
        }

        for(BackEndCall call:listenCallList){
            if(updateDevId.compareToIgnoreCase(call.devID)==0){
                status = ProtocolPacket.STATUS_OK;
                break;
            }
        }

        UpdateResPack updateResP = new UpdateResPack(status,updateReqP);
        trans = new Transaction(updateReqP.devId,updateReqP,updateResP,Transaction.TRANSCATION_DIRECTION_S2C);
        HandlerMgr.AddBackEndTrans(updateReqP.msgID, trans);
    }

    public void AnswerCall(AnswerReqPack packet){
        AnswerResPack answerResP;
        AnswerReqPack answerForwareP;
        EndReqPack endP;
        Transaction trans;

        answerResP = new AnswerResPack(ProtocolPacket.STATUS_OK,packet);
        trans = new Transaction(packet.answerer, packet, answerResP, Transaction.TRANSCATION_DIRECTION_S2C);
        HandlerMgr.AddBackEndTrans(answerResP.msgID, trans);

        answerForwareP = new AnswerReqPack(packet,inviteCall.caller);
        trans = new Transaction(inviteCall.caller, answerForwareP,Transaction.TRANSCATION_DIRECTION_S2C);
        HandlerMgr.AddBackEndTrans(answerForwareP.msgID, trans);

        inviteCall.answer = packet.answerer;

        if(inviteCall.callee.compareToIgnoreCase(PhoneParam.CALL_SERVER_ID)!=0) {
            if (inviteCall.callee.compareToIgnoreCase(packet.answerer) != 0) {
                endP = new EndReqPack();

                endP.sender = PhoneParam.CALL_SERVER_ID;
                endP.receiver = inviteCall.callee;
                endP.msgID = UniqueIDManager.GetUniqueID(endP.sender, UniqueIDManager.MSG_UNIQUE_ID);

                endP.endDevID = packet.answerer;
                endP.callID = inviteCall.callID;

                trans = new Transaction(inviteCall.callee, endP, Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(endP.msgID, trans);

            }
        }

        // end call for listeners;
        for(CommonCall listenCall:listenCallList){
            if(listenCall.devID.compareToIgnoreCase(packet.answerer)!=0) {
                endP = new EndReqPack();

                endP.sender = PhoneParam.CALL_SERVER_ID;
                endP.receiver = listenCall.devID;
                endP.msgID = UniqueIDManager.GetUniqueID(listenCall.devID, UniqueIDManager.MSG_UNIQUE_ID);

                endP.endDevID = packet.answerer;
                endP.callID = listenCall.callID;

                trans = new Transaction(listenCall.devID, endP, Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(endP.msgID, trans);
            }
        }

        listenCallList.clear();
        inviteCall.state = CommonCall.CALL_STATE_CONNECTED;
    }

    public void ProcessSecondTick(){
        inviteCall.callerWaitUpdateCount++;
        if(inviteCall.answer.isEmpty()){
            if(inviteCall.callee.compareToIgnoreCase(PhoneParam.CALL_SERVER_ID)!=0){
                inviteCall.calleeWaitUpdateCount++;
            }
        }else{
            inviteCall.answerWaitUpdateCount++;
        }

        if(inviteCall.callerWaitUpdateCount>CommonCall.UPDATE_INTERVAL*2+5
                ||inviteCall.calleeWaitUpdateCount>CommonCall.UPDATE_INTERVAL*2+5
                ||inviteCall.answerWaitUpdateCount>CommonCall.UPDATE_INTERVAL*2+5){
            if(inviteCall.callerWaitUpdateCount>CommonCall.UPDATE_INTERVAL*2+5)
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"BackEnd End Call %s for Miss Update of Caller DEV %s ",inviteCall.callID,inviteCall.caller);
            if(inviteCall.calleeWaitUpdateCount>CommonCall.UPDATE_INTERVAL*2+5)
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"BackEnd End Call %s for Miss Update of Callee DEV %s ",inviteCall.callID,inviteCall.callee);
            if(inviteCall.answerWaitUpdateCount>CommonCall.UPDATE_INTERVAL*2+5)
                LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"BackEnd End Call %s for Miss Update of Answer DEV %s ",inviteCall.callID,inviteCall.answer);
            Message phonemsg = new Message();
            phonemsg.arg1 = BackEndPhoneManager.MSG_NEW_PACKET;
            phonemsg.obj = new EndReqPack(inviteCall.callID);
            HandlerMgr.PostBackEndPhoneMsg(phonemsg);

        }
    }

    public void UpdateStatus(ProtocolPacket packet){
        if(packet.type == ProtocolPacket.CALL_RES ){
            InviteResPack inviteResPack = (InviteResPack)packet;
            if(inviteResPack.status == ProtocolPacket.STATUS_OK)
                inviteCall.state = CommonCall.CALL_STATE_RINGING;
            else{
                if(inviteResPack.sender.compareToIgnoreCase(inviteCall.callee)==0){
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE,LogWork.LOG_ERROR,"BackEnd End Call %s when Recv Call Res with %s",inviteCall.callID,ProtocolPacket.GetResString(inviteResPack.status));
                    Message phonemsg = new Message();
                    phonemsg.arg1 = BackEndPhoneManager.MSG_NEW_PACKET;
                    phonemsg.obj = new EndReqPack(inviteCall.callID);
                    HandlerMgr.PostBackEndPhoneMsg(phonemsg);
                }
            }
        }
    }

    public boolean CheckAnswerEnable(BackEndPhone phone,String callid){
        boolean result = true;

        if(phone==null)
            return false;
        if(!phone.isReg)
            return false;

        if(callid.compareToIgnoreCase(inviteCall.callID)!=0){
            if(inviteCall.state==CommonCall.CALL_STATE_CONNECTED){
                if(inviteCall.caller.compareToIgnoreCase(phone.id)==0)
                    result=false;
                else if(inviteCall.answer.compareToIgnoreCase(phone.id)==0)
                    result = false;
            }
        }

        return result;
    }

    public boolean CheckInviteEnable(BackEndPhone phone){
        boolean result= true;
        if(!phone.isReg)
            result = false;
        else {
            switch (phone.type) {
                case BackEndPhone.BED_CALL_DEVICE:
                case BackEndPhone.DOOR_CALL_DEVICE:
                case BackEndPhone.NURSE_CALL_DEVICE:
                    if (inviteCall.caller.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    else if (inviteCall.callee.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    else {
                        for (CommonCall listenCall : listenCallList) {
                            if (listenCall.callee.compareToIgnoreCase(phone.id) == 0) {
                                result = false;
                                break;
                            }
                        }
                    }
                    break;
                case BackEndPhone.EMER_CALL_DEVICE:
                    if (inviteCall.caller.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    break;
                case BackEndPhone.CORRIDOR_CALL_DEVICE:
                case BackEndPhone.TV_CALL_DEVICE:
                    result = false;
                    break;
            }
        }
        return result;
    }

    private boolean CheckForwardEnable(BackEndPhone phone){
        boolean result = true;

        if(!phone.isReg)
            result = false;
        else {
            switch (phone.type) {
                case BackEndPhone.BED_CALL_DEVICE:
                    if (inviteCall.caller.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    else if (inviteCall.callee.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    else {
                        for (CommonCall listenCall : listenCallList) {
                            if (listenCall.callee.compareToIgnoreCase(phone.id) == 0) {
                                result = false;
                                break;
                            }
                        }
                    }
                    break;
                case BackEndPhone.CORRIDOR_CALL_DEVICE:
                case BackEndPhone.DOOR_CALL_DEVICE:
                case BackEndPhone.NURSE_CALL_DEVICE:
                case BackEndPhone.TV_CALL_DEVICE:
                    break;
                case BackEndPhone.EMER_CALL_DEVICE:
                    result = false;
                    break;
            }
        }

        return result;
    }

    public boolean CheckInvitedEnable(BackEndPhone phone) {
        boolean result = true;

        if(!phone.isReg)
            result = false;
        else{
            switch (phone.type) {
                case BackEndPhone.BED_CALL_DEVICE:
                    if (inviteCall.caller.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    else if (inviteCall.callee.compareToIgnoreCase(phone.id) == 0)
                        result = false;
                    else {
                        for (CommonCall listenCall : listenCallList) {
                            if (listenCall.callee.compareToIgnoreCase(phone.id) == 0) {
                                result = false;
                                break;
                            }
                        }
                    }
                    break;
                case BackEndPhone.NURSE_CALL_DEVICE:
                    break;
                case BackEndPhone.CORRIDOR_CALL_DEVICE:
                case BackEndPhone.DOOR_CALL_DEVICE:
                case BackEndPhone.TV_CALL_DEVICE:
                case BackEndPhone.EMER_CALL_DEVICE:
                    result = false;
                    break;
            }
        }

        return result;
    }
}
