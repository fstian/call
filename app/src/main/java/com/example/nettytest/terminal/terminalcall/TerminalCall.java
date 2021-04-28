package com.example.nettytest.terminal.terminalcall;

import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.terminal.audio.AudioDevice;
import com.example.nettytest.terminal.audio.AudioMgr;
import com.example.nettytest.userinterface.FailReason;
import com.example.nettytest.userinterface.OperationResult;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
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
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;

public class TerminalCall extends CommonCall {


    public int updateTick;

    String audioId = "";

    int autoAnswerTime;
    int autoAnswerTick;

    // call out
    public TerminalCall(String caller, TerminalDeviceInfo info,String callee, int type,int direction) {
        super(caller, callee, type);
        updateTick =CommonCall.UPDATE_INTERVAL;
        autoAnswerTime = -1;
        autoAnswerTick = 0;
        direct = direction;

        InviteReqPack invitePack = BuildInvitePacket(info);
        Transaction inviteTransaction = new Transaction(devID,invitePack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s invite Phone %s, CallID = %s! ",caller,callee,callID);
        HandlerMgr.AddPhoneTrans(invitePack.msgID,inviteTransaction);
    }

    // incoming call
    public TerminalCall(InviteReqPack pack){
        super(pack.receiver,pack);
        updateTick =CommonCall.UPDATE_INTERVAL;
        autoAnswerTime = pack.autoAnswerTime;
        autoAnswerTick = 0;

        InviteResPack resPack = new InviteResPack();
        resPack.ExchangeCopyData(pack);
        resPack.type = ProtocolPacket.CALL_RES;
        resPack.callID = pack.callID;
        resPack.status = ProtocolPacket.STATUS_OK;
        resPack.result = ProtocolPacket.GetResString(resPack.status);

        UserCallMessage callMsg = new UserCallMessage();
        callMsg.type = UserCallMessage.CALL_MESSAGE_INCOMING;
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.callerId = pack.caller;
        callMsg.calleeId = pack.callee;
        switch(pack.callerType){
            case CALL_TYPE_EMERGENCY:
                callMsg.callerType = UserCallMessage.EMERGENCY_CALL_TYPE;
                break;
            case CALL_TYPE_BROADCAST:
                callMsg.callerType = UserCallMessage.BROADCAST_CALL_TYPE;
                break;
            default:
                callMsg.callerType = UserCallMessage.NORMAL_CALL_TYPE;
                break;
        }
        callMsg.callerType = pack.callerType;
        callMsg.callType = pack.callType;

        callMsg.patientName = pack.patientName;
        callMsg.patientAge = pack.patientAge;
        callMsg.roomId = pack.roomId;
        callMsg.bedName = pack.bedName;
        callMsg.deviceName = pack.deviceName;

        Transaction inviteResTransaction = new Transaction(devID,pack,resPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Invite From %s to %s, CallID = %s",devID,caller,callee,callID);
        HandlerMgr.AddPhoneTrans(pack.msgID,inviteResTransaction);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

    public int Answer(){
        int audioMode;
        AnswerReqPack answerPack = BuildAnswerPacket();


        if(type==CALL_TYPE_BROADCAST){
            audioMode = AudioDevice.RECV_ONLY_MODE;
        }else if(type==CALL_TYPE_EMERGENCY){
            audioMode = AudioDevice.NO_SEND_RECV_MODE;
        }else{
            audioMode = AudioDevice.SEND_RECV_MODE;
        }

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Answer Call %s! sample=%d,ptime=%d,codec=%d",devID,callID,audioSample,rtpTime,audioCodec);
        audioId = AudioMgr.OpenAudio(devID,localRtpPort,remoteRtpPort,remoteRtpAddress,audioSample,rtpTime,audioCodec,audioMode);
        Transaction answerTrans = new Transaction(devID,answerPack,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(answerPack.msgID,answerTrans);

//     answer maybe rejected
//        state = CommonCall.CALL_STATE_CONNECTED;

//        Success(ProtocolPacket.ANSWER_REQ);

        return ProtocolPacket.STATUS_OK;
    }

    public int EndCall(){
        EndReqPack endPack = BuildEndPacket();

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s End Call %s! ",devID,callID);

        UserCallMessage callMsg = new UserCallMessage();
        callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;
        callMsg.devId = devID;
        callMsg.callId = callID;
        if(!audioId.isEmpty())
            AudioMgr.CloseAudio(audioId);

        Transaction endTransaction = new Transaction(devID,endPack,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(endPack.msgID,endTransaction);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
        return ProtocolPacket.STATUS_OK;
    }

    public void UpdateByInviteRes(InviteResPack packet){
        UserCallMessage callMsg = new UserCallMessage();
        
        callMsg.devId = devID;
        callMsg.callId = callID;

        if(packet.status == ProtocolPacket.STATUS_OK) {
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Invite in Call %s! ",devID,callID);
            state = CommonCall.CALL_STATE_RINGING;
            callMsg.type = UserCallMessage.CALL_MESSAGE_RINGING;
            callMsg.reason = FailReason.FAIL_REASON_NO;

        }else {
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_INFO,"Phone %s Recv %d(%s) for Invite in Call %s! ",devID,packet.status,ProtocolPacket.GetResString(packet.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_INVITE_FAIL;
            callMsg.reason = OperationResult.GetUserFailReason(packet.status);
        }
        
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

   public void UpdateByAnswerRes(AnswerResPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        
        callMsg.devId = devID;
        callMsg.callId = callID;

        if(pack.status==ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Answer in Call %s! ",devID,callID);
            state = CommonCall.CALL_STATE_CONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_CONNECT;
            callMsg.reason = FailReason.FAIL_REASON_NO;
        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_INFO,"Phone %s Recv %d(%s) for Answer in Call %s! ",devID,pack.status,ProtocolPacket.GetResString(pack.status),callID);
            callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWER_FAIL;
            callMsg.reason = OperationResult.GetUserFailReason(pack.status);
        }
        
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
   }

    public void UpdateByUpdateRes(UpdateResPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callid;

        if(pack.status!=ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone %s Recv %d(%s) for Update in Call %s!",devID,pack.status,ProtocolPacket.GetResString(pack.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;
            callMsg.reason = OperationResult.GetUserFailReason(pack.status);

            HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Update in Call %s! ",devID,callID);
        }

    }

    public void UpdateByEndRes(EndResPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callId;

        if(pack.status!=ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone %s Recv %d(%s) for End Call %s!",devID,pack.status,ProtocolPacket.GetResString(pack.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_END_FAIL;
            callMsg.reason = OperationResult.GetUserFailReason(pack.status);
            
            HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for End in Call %s! ",devID,callID);
        }

    }
    
    public void RecvEnd(EndReqPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;

        if(!audioId.isEmpty())
            AudioMgr.CloseAudio(audioId);

        EndResPack endResPack = new EndResPack(ProtocolPacket.STATUS_OK,pack);
        Transaction endResTrans = new Transaction(devID,pack,endResPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call End Req for CallID = %s from Dev %s, and Send End Res to Server! ",devID,callID,pack.endDevID);
        HandlerMgr.AddPhoneTrans(endResPack.msgID,endResTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
        
    }

    public void ReleaseCall(){
        if(!audioId.isEmpty())
            AudioMgr.CloseAudio(audioId);
    }


   public int RecvAnswer(AnswerReqPack pack){
        int status = ProtocolPacket.STATUS_OK;
        AnswerResPack answerResPack = new AnswerResPack(ProtocolPacket.STATUS_OK,pack);
        UserCallMessage callMsg = new UserCallMessage();
        int audioMode;
        callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWERED;
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.operaterId = pack.answerer;

        answer = pack.answerer;
        
        state = CommonCall.CALL_STATE_CONNECTED;
        remoteRtpPort = pack.answererRtpPort;
        remoteRtpAddress = pack.answererRtpIP;
        if(type==CALL_TYPE_BROADCAST){
            audioMode = AudioDevice.SEND_ONLY_MODE;
        }else if(type == CALL_TYPE_EMERGENCY){
            audioMode = AudioDevice.NO_SEND_RECV_MODE;
        }else{
            audioMode = AudioDevice.SEND_RECV_MODE;
        }
        
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call Answer Req for CallID = %s, and Send Answer Res to Server! ",devID,callID);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Open Audio sample=%d,ptime=%d,codec=%d",devID,pack.sample,pack.pTime,pack.codec);
        audioId = AudioMgr.OpenAudio(devID,localRtpPort,remoteRtpPort,remoteRtpAddress,pack.sample,pack.pTime,pack.codec,audioMode);

        Transaction answerResTrans = new Transaction(devID,pack,answerResPack,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(answerResPack.msgID,answerResTrans);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
        return status;

    }


    public void UpdateSecondTick(){
        updateTick--;
        if(updateTick==0){
            // resend update;
            UpdateReqPack updateReqP = BuildUpdatePacket();
            Transaction updateReqTrans = new Transaction(devID,updateReqP,Transaction.TRANSCATION_DIRECTION_C2S);
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Send Update to Server for call %s! ",devID,callID);
            HandlerMgr.AddPhoneTrans(updateReqP.msgID,updateReqTrans);
            updateTick = CommonCall.UPDATE_INTERVAL;
        }
        
        if(state==CALL_STATE_INCOMING){
            if(autoAnswerTime>=0&&type==CALL_TYPE_BROADCAST){
                if(autoAnswerTick<=autoAnswerTime){
                    autoAnswerTick++;
                    if(autoAnswerTick>autoAnswerTime){
                        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s AutoAnswer call %s! ",devID,callID);
                        Answer();
                    }
                }
            }
        }
    }

    public void InviteTimeOver(){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.type = UserCallMessage.CALL_MESSAGE_INVITE_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }
    
    public void AnswerTimeOver(){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWER_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

    public void EndTimeOver(){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.type = UserCallMessage.CALL_MESSAGE_END_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }


    public void UpdateTimeOver(){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.type = UserCallMessage.CALL_MESSAGE_UPDATE_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
        if(!audioId.isEmpty())
            AudioMgr.CloseAudio(audioId);
    }
   
    private InviteReqPack BuildInvitePacket(TerminalDeviceInfo info){
        InviteReqPack invitePack = new InviteReqPack();
        TerminalPhone phone = HandlerMgr.GetPhoneDev(caller);

        invitePack.sender = caller;
        invitePack.receiver = PhoneParam.CALL_SERVER_ID;
        invitePack.type = ProtocolPacket.CALL_REQ;
        invitePack.msgID = UniqueIDManager.GetUniqueID(caller,UniqueIDManager.MSG_UNIQUE_ID);

        invitePack.callType = type;
        invitePack.callDirect = direct;
        invitePack.callID = callID;

        invitePack.caller = caller;
        invitePack.callee = callee;
        invitePack.callerType = phone.type;
        invitePack.bedName = "";
        invitePack.patientName = info.patientName;
        invitePack.patientAge = info.patientAge;

        invitePack.codec = audioCodec;
        invitePack.pTime = rtpTime;
        invitePack.sample = audioSample;

        invitePack.callerRtpIP = PhoneParam.GetLocalAddress();
        invitePack.callerRtpPort = localRtpPort;
        if(type==CALL_TYPE_BROADCAST){
            invitePack.autoAnswerTime = PhoneParam.BROADCALL_ANSWER_WAIT;
        }else{
            invitePack.autoAnswerTime  = -1;
        }
        return invitePack;
    }

    private EndReqPack BuildEndPacket(){
        EndReqPack endPacket = new EndReqPack();

        endPacket.sender = devID;
        endPacket.receiver = PhoneParam.CALL_SERVER_ID;
        endPacket.type = ProtocolPacket.END_REQ;
        endPacket.msgID = UniqueIDManager.GetUniqueID(caller,UniqueIDManager.MSG_UNIQUE_ID);

        endPacket.callID = callID;
        endPacket.endDevID = devID;

        return endPacket;
    }

    private AnswerReqPack BuildAnswerPacket(){
        AnswerReqPack answerReqPack = new AnswerReqPack();

        answerReqPack.type = ProtocolPacket.ANSWER_REQ;
        answerReqPack.sender = devID;
        answerReqPack.receiver = PhoneParam.CALL_SERVER_ID;
        answerReqPack.msgID = UniqueIDManager.GetUniqueID(devID,UniqueIDManager.MSG_UNIQUE_ID);
        answerReqPack.callType = type;

        answerReqPack.answerer = devID;
        answerReqPack.callID = callID;

        answerReqPack.answererRtpPort = localRtpPort;
        answerReqPack.answererRtpIP = PhoneParam.GetLocalAddress();

        answerReqPack.codec = audioCodec;
        answerReqPack.pTime = rtpTime;
        answerReqPack.sample = audioSample;

        return answerReqPack;
    }

    private UpdateReqPack BuildUpdatePacket(){
        UpdateReqPack updateReqP = new UpdateReqPack();

        updateReqP.type = ProtocolPacket.CALL_UPDATE_REQ;
        updateReqP.sender = devID;
        updateReqP.receiver = PhoneParam.CALL_SERVER_ID;
        updateReqP.msgID = UniqueIDManager.GetUniqueID(devID,UniqueIDManager.MSG_UNIQUE_ID);

        updateReqP.callId = callID;
        updateReqP.devId = devID;

        return updateReqP;
    }
}
