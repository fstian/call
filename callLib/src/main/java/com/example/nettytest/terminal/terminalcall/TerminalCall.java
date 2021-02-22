package com.example.nettytest.terminal.terminalcall;

import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.terminal.audio.AudioMgr;
import com.example.nettytest.userinterface.FailReason;
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

    public String remoteRtpAddress;
    public int remoteRtpPort;

    public int updateTick;

    String audioId = "";

    // call out
    public TerminalCall(String caller, String callee, int type) {
        super(caller, callee, type);
        remoteRtpAddress = "";
        remoteRtpPort = 0;
        updateTick =CommonCall.UPDATE_INTERVAL;

        InviteReqPack invitePack = BuildInvitePacket();
        Transaction inviteTransaction = new Transaction(devID,invitePack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s invite Phone %s, CallID = %s! ",caller,callee,callID);
        HandlerMgr.AddPhoneTrans(invitePack.msgID,inviteTransaction);
    }

    // incoming call
    public TerminalCall(InviteReqPack pack){
        super(pack.receiver,pack);
        remoteRtpAddress = pack.callerRtpIP;
        remoteRtpPort = pack.callerRtpPort;
        updateTick =CommonCall.UPDATE_INTERVAL;

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

        Transaction inviteResTransaction = new Transaction(devID,pack,resPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Invite From %s to %s, CallID = %s",devID,caller,callee,callID);
        HandlerMgr.AddPhoneTrans(pack.msgID,inviteResTransaction);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

    public int Answer(){
        AnswerReqPack answerPack = BuildAnswerPacket();

        UserCallMessage callMsg = new UserCallMessage();
        callMsg.type = UserCallMessage.CALL_MESSAGE_CONNECT;
        callMsg.devId = devID;
        callMsg.callId = callID;

        state = CommonCall.CALL_STATE_CONNECTED;
        audioId = AudioMgr.OpenAudio(devID,localRtpPort,remoteRtpPort,remoteRtpAddress,audioSample,rtpTime,audioCodec,0);

        Transaction answerTrans = new Transaction(devID,answerPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Answer Call %s! ",devID,callID);
        HandlerMgr.AddPhoneTrans(answerPack.msgID,answerTrans);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
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

    public void UpdateCallStatus(InviteResPack packet){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = packet.callID;

        if(packet.status == ProtocolPacket.STATUS_OK) {
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Invite in Call %s! ",devID,callID);
            state = CommonCall.CALL_STATE_RINGING;
            callMsg.type = UserCallMessage.CALL_MESSAGE_RINGING;
        }else {
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_INFO,"Phone %s Recv %d(%s) for Invite in Call %s! ",devID,packet.status,ProtocolPacket.GetResString(packet.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;
            callMsg.reason = FailReason.FAIL_REASON_UNKNOW;
        }

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

    }

    public void UpdateCallStatus(UpdateResPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callid;

        if(pack.status!=ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone %s Recv %d(%s) for Update in Call %s!",devID,pack.status,ProtocolPacket.GetResString(pack.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;
            callMsg.reason = FailReason.FAIL_REASON_TIMEOVER;

            HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Update in Call %s! ",devID,callID);
        }

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
    }

    public void UpdateCallStatus(AnswerReqPack pack){
        AnswerResPack answerResPack = new AnswerResPack(ProtocolPacket.STATUS_OK,pack);
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWERED;
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.operaterId = pack.answerer;

        answer = pack.answerer;
        state = CommonCall.CALL_STATE_CONNECTED;
        remoteRtpPort = pack.answererRtpPort;
        remoteRtpAddress = pack.answererRtpIP;
        audioId = AudioMgr.OpenAudio(devID,localRtpPort,remoteRtpPort,remoteRtpAddress,pack.sample,pack.pTime,pack.codec,0);

        Transaction answerResTrans = new Transaction(devID,pack,answerResPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call Answer Req for CallID = %s, and Send Answer Res to Server! ",devID,callID);
        HandlerMgr.AddPhoneTrans(answerResPack.msgID,answerResTrans);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

    }

    public void Finish(EndReqPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;

        if(!audioId.isEmpty())
            AudioMgr.CloseAudio(audioId);

        EndResPack endResPack = new EndResPack(ProtocolPacket.STATUS_OK,pack);
        Transaction endResTrans = new Transaction(devID,pack,endResPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call End Req for CallID = %s, and Send End Res to Server! ",devID,callID);
        HandlerMgr.AddPhoneTrans(endResPack.msgID,endResTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

    public void Fail(int req, int status){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        if(req == ProtocolPacket.CALL_REQ)
            callMsg.type = UserCallMessage.CALL_MESSAGE_INVITE_FAIL;
        else if(req == ProtocolPacket.ANSWER_REQ)
            callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWER_FAIL;
        else if(req == ProtocolPacket.END_REQ)
            callMsg.type = UserCallMessage.CALL_MESSAGE_END_FAIL;
        else if(req == ProtocolPacket.CALL_UPDATE_REQ)
            callMsg.type = UserCallMessage.CALL_MESSAGE_UPDATE_FAIL;
        else
            callMsg.type = UserCallMessage.CALL_MESSAGE_UNKNOWFAIL;

        if (status == ProtocolPacket.STATUS_TIMEOVER) {
            callMsg.reason = FailReason.FAIL_REASON_TIMEOVER;
        } else {
            callMsg.reason = FailReason.FAIL_REASON_UNKNOW;
        }

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

    }

    private InviteReqPack BuildInvitePacket(){
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
        invitePack.bedID = "";

        invitePack.codec = audioCodec;
        invitePack.pTime = rtpTime;
        invitePack.sample = audioSample;

        invitePack.callerRtpIP = PhoneParam.GetLocalAddress();
        invitePack.callerRtpPort = localRtpPort;
        invitePack.broadcastIP = PhoneParam.BROAD_ADDRESS;
        invitePack.broadcastPort = PhoneParam.INVITE_CALL_RTP_PORT;
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
