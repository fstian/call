package com.example.nettytest.terminal.terminalcall;

import com.example.nettytest.pub.AudioMode;
import com.example.nettytest.pub.protocol.AnswerVideoReqPack;
import com.example.nettytest.pub.protocol.AnswerVideoResPack;
import com.example.nettytest.pub.protocol.CancelReqPack;
import com.example.nettytest.pub.protocol.CancelResPack;
import com.example.nettytest.pub.protocol.StartVideoReqPack;
import com.example.nettytest.pub.protocol.StartVideoResPack;
import com.example.nettytest.pub.protocol.StopVideoReqPack;
import com.example.nettytest.pub.protocol.StopVideoResPack;
import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.pub.result.OperationResult;
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
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserVideoMessage;

public class TerminalCall extends CommonCall {


    private int updateTick;

    private int updateTimeOverCount ;

    int autoAnswerTime;
    int autoAnswerTick;

    // call out
    public TerminalCall(String caller, TerminalDeviceInfo info,String callee, int type,int direction) {
        super(caller, callee, type);
        updateTick =CommonCall.UPDATE_INTERVAL;
        autoAnswerTime = -1;
        autoAnswerTick = 0;
        direct = direction;
        updateTimeOverCount = 0;

        InviteReqPack invitePack = BuildInvitePacket(info);
        Transaction inviteTransaction = new Transaction(devID,invitePack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s invite Phone %s, CallID = %s! ",caller,callee,callID);
        HandlerMgr.AddPhoneTrans(invitePack.msgID,inviteTransaction);
        inviteReqMsgId = invitePack.msgID;
    }

    // incoming call
    public TerminalCall(InviteReqPack pack){
        super(pack.receiver,pack);
        inviteReqMsgId = "";
        updateTick =CommonCall.UPDATE_INTERVAL;
        autoAnswerTime = pack.autoAnswerTime;
        autoAnswerTick = 0;
        if(pack.callType == CommonCall.CALL_TYPE_BROADCAST){
            localRtpPort = pack.callerRtpPort;
            remoteRtpPort = pack.callerRtpPort;
        }

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
        callMsg.roomName = pack.roomName;
        callMsg.bedName = pack.bedName;
        callMsg.deviceName = pack.deviceName;

        callMsg.areaId = pack.areaId;
        callMsg.areaName = pack.areaName;
        callMsg.isTransfer = pack.isTransfer;

        Transaction inviteResTransaction = new Transaction(devID,pack,resPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Invite From %s to %s, CallID = %s",devID,caller,callee,callID);
        HandlerMgr.AddPhoneTrans(pack.msgID,inviteResTransaction);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

    public int Answer(){
        AnswerReqPack answerPack = BuildAnswerPacket();
        answer = devID;

        Transaction answerTrans = new Transaction(devID,answerPack,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(answerPack.msgID,answerTrans);

//     answer maybe rejected
//        state = CommonCall.CALL_STATE_CONNECTED;

//        Success(ProtocolPacket.ANSWER_REQ);

        return ProtocolPacket.STATUS_OK;
    }

    public int StartVideo(){
        
        StartVideoReqPack startVideoReqP = BuildStartVideoPacket();

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Start Video in Call %s! ",devID,callID);
//        AudioMgr.SuspendAudio(audioId);
        Transaction startVideoTrans = new Transaction(devID,startVideoReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(startVideoReqP.msgID,startVideoTrans);

        return ProtocolPacket.STATUS_OK;
    }

    public int AnswerVideo(){
        AnswerVideoReqPack answerVideoReqP = BuildAnswerVideoPacket();

        UserVideoMessage videoMsg = new UserVideoMessage();
        videoMsg.type = UserMessage.CALL_VIDEO_REQ_ANSWER;
        videoMsg.callId = callID;
        videoMsg.devId = devID;

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Answer Video in Call %s! ",devID,callID);
        Transaction answerVideoTrans = new Transaction(devID,answerVideoReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(answerVideoReqP.msgID,answerVideoTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_VIDEO_INFO,videoMsg);

        return ProtocolPacket.STATUS_OK;
    }

    public int StopVideo(){
        
        StopVideoReqPack stopPack = BuildStopVideoPacket();
        UserVideoMessage videoMsg = new UserVideoMessage();
        videoMsg.type = UserMessage.CALL_VIDEO_END;
        videoMsg.callId = callID;
        videoMsg.devId = devID;

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Stop Video in Call %s! ",devID,callID);
        Transaction stopVideoTrans = new Transaction(devID,stopPack,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(stopPack.msgID,stopVideoTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_VIDEO_INFO,videoMsg);

        return ProtocolPacket.STATUS_OK;
    }


    public int EndCall(){
        int reason;
        if(devID.compareToIgnoreCase(caller)==0)
            reason = EndReqPack.END_BY_CALLER;
        else if(devID.compareToIgnoreCase(answer)==0)
            reason = EndReqPack.END_BY_ANSWER;
        else if(devID.compareToIgnoreCase(callee)==0)
            reason = EndReqPack.END_BY_CALLEE;
        else
            reason = EndReqPack.END_BY_LISTENER;

        EndReqPack endPack = BuildEndPacket(reason);

        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s End Call %s! ",devID,callID);

        UserCallMessage callMsg = new UserCallMessage();
        callMsg.type = UserCallMessage.CALL_MESSAGE_DISCONNECT;
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.callType = type;
        callMsg.endReason = UserCallMessage.CALL_END_BY_SELF;
//        if(!audioId.isEmpty())
//            AudioMgr.CloseAudio(audioId);

        if(!inviteReqMsgId.isEmpty())
            HandlerMgr.RemovePhoneTrans(inviteReqMsgId);

        Transaction endTransaction = new Transaction(devID,endPack,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(endPack.msgID,endTransaction);

        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
        return ProtocolPacket.STATUS_OK;
    }

    public void UpdateByInviteRes(InviteResPack packet){
        UserCallMessage callMsg = new UserCallMessage();
        
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.callType = type;

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
        callMsg.callerType = type;

        if(pack.status==ProtocolPacket.STATUS_OK){
            int audioMode;

            if(type==CALL_TYPE_BROADCAST){
                audioMode = AudioMode.RECV_ONLY_MODE;
            }else if(type==CALL_TYPE_EMERGENCY){
                audioMode = AudioMode.NO_SEND_RECV_MODE;
            }else{
                audioMode = AudioMode.SEND_RECV_MODE;
            }

            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Answer Call %s! sample=%d,ptime=%d,codec=%d",devID,callID,audioSample,rtpTime,audioCodec);
//        audioId = AudioMgr.OpenAudio(devID,localRtpPort,remoteRtpPort,remoteRtpAddress,audioSample,rtpTime,audioCodec,audioMode);

            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Answer in Call %s! ",devID,callID);
            state = CommonCall.CALL_STATE_CONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_CONNECT;
            callMsg.reason = FailReason.FAIL_REASON_NO;
            

            callMsg.localRtpPort = localRtpPort;
            callMsg.remoteRtpPort = remoteRtpPort;
            callMsg.remoteRtpAddress = remoteRtpAddress;
            callMsg.rtpSample = audioSample;
            callMsg.rtpPTime = rtpTime;
            callMsg.rtpCodec = audioCodec;
            callMsg.audioMode = audioMode;


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
        callMsg.callType = type;

        if(pack.status!=ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone %s Recv %d(%s) for Update in Call %s!",devID,pack.status,ProtocolPacket.GetResString(pack.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;
            callMsg.type = UserCallMessage.CALL_MESSAGE_UPDATE_FAIL;
            callMsg.reason = OperationResult.GetUserFailReason(pack.status);

        }else{
            updateTimeOverCount = 0;
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Update in Call %s! ",devID,callID);
            callMsg.type = UserCallMessage.CALL_MESSAGE_UPDATE_SUCC;

        }
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);

    }

    public void UpdateByEndRes(EndResPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callId;
        callMsg.callType = type;

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

    public void UpdateByCancelRes(CancelResPack pack){

        if(pack.status!=ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone %s Recv %d(%s) for Cancel Call %s!",devID,pack.status,ProtocolPacket.GetResString(pack.status),callID);
            state = CommonCall.CALL_STATE_DISCONNECTED;

        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv OK for Cancel in Call %s! ",devID,callID);
        }
    }

    public boolean RecvCancel(String devId,CancelReqPack cancelReqP){
        boolean result = true;
        CancelResPack cancelResP;

        if(caller.compareToIgnoreCase(devId)==0){
            result = false;
        }

        if(callee.compareToIgnoreCase(devId)==0){
            result = false;
        }

        if(result){
            cancelResP = new CancelResPack(ProtocolPacket.STATUS_OK,cancelReqP);
            UserCallMessage callMsg = new UserCallMessage();
            callMsg.devId = devID;
            callMsg.callId = cancelReqP.callID;
            callMsg.callType = type;
            callMsg.type = UserMessage.CALL_MESSAGE_DISCONNECT;
            callMsg.endReason = UserCallMessage.CALL_CANCEL_FOR_SERVER;
            HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call Cancel Req for CallID  %s from Server,  and Send Call Cancel Res to Server! ",devID,callID);
        }else{
            cancelResP = new CancelResPack(ProtocolPacket.STATUS_NOTSUPPORT,cancelReqP);
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call Cancel Req for CallID  %s from Server,  But not support, Caller is %s, Callee is %s",devID,callID,caller,callee);
        }
        Transaction endResTrans = new Transaction(devID,cancelReqP,cancelResP,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(cancelResP.msgID,endResTrans);

        return result;
    }
    
    public void RecvEnd(EndReqPack pack){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.callType = type;
        callMsg.type = UserMessage.CALL_MESSAGE_DISCONNECT;
        callMsg.endReason = pack.endReason;

//        if(!audioId.isEmpty())
//            AudioMgr.CloseAudio(audioId);

        EndResPack endResPack = new EndResPack(ProtocolPacket.STATUS_OK,pack);
        Transaction endResTrans = new Transaction(devID,pack,endResPack,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call End Req for CallID = %s from Dev %s, Reason is %s, and Send End Res to Server! ",devID,callID,pack.endDevID,EndReqPack.GetEndReasonName(pack.endReason));
        HandlerMgr.AddPhoneTrans(endResPack.msgID,endResTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
        
    }


    public void RecvStartVideoReq(StartVideoReqPack pack){
        UserVideoMessage videoMsg = new UserVideoMessage();
        videoMsg.type = UserMessage.CALL_VIDEO_INVITE;
        videoMsg.callId = pack.callID;
        videoMsg.devId = devID;

        StartVideoResPack startVideoResP = new StartVideoResPack(ProtocolPacket.STATUS_OK,pack);
        Transaction startVideoResTrans = new Transaction(devID,pack,startVideoResP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Start Video Req for CallID = %s from Dev %s, and Send Start Video Res to Server! ",devID,callID,pack.startVideoDevId);
        HandlerMgr.AddPhoneTrans(startVideoResP.msgID,startVideoResTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_VIDEO_INFO,videoMsg);
    }

    public void RecvAnswerVideoReq(AnswerVideoReqPack pack){
        UserVideoMessage videoMsg = new UserVideoMessage();
        videoMsg.type = UserMessage.CALL_VIDEO_ANSWERED;
        videoMsg.callId = pack.callId;
        videoMsg.devId = devID;

        AnswerVideoResPack answerVideoResP = new AnswerVideoResPack(ProtocolPacket.STATUS_OK,pack);
        Transaction answerVideoResTrans = new Transaction(devID,pack,answerVideoResP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Answer Video Req for CallID = %s from Dev %s, and Send Answer Video Res to Server! ",devID,callID,pack.answerDevId);
        HandlerMgr.AddPhoneTrans(answerVideoResP.msgID,answerVideoResTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_VIDEO_INFO,videoMsg);
    }

    public void RecvStopVideoReq(StopVideoReqPack pack){
        UserVideoMessage videoMsg = new UserVideoMessage();
        videoMsg.type = UserMessage.CALL_VIDEO_END;
        videoMsg.callId = pack.callID;
        videoMsg.devId = devID;

        StopVideoResPack stopVideoResP = new StopVideoResPack(ProtocolPacket.STATUS_OK,pack);
        Transaction stopVideoResTrans = new Transaction(devID,pack,stopVideoResP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv End Video Req for CallID = %s from Dev %s, and Send End Res to Server! ",devID,callID,pack.stopVideoDevId);
        HandlerMgr.AddPhoneTrans(stopVideoResP.msgID,stopVideoResTrans);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_VIDEO_INFO,videoMsg);
    }

    public void ReleaseCall(){
//        if(!audioId.isEmpty())
//            AudioMgr.CloseAudio(audioId);
    }


   public int RecvAnswer(AnswerReqPack pack){
        int status = ProtocolPacket.STATUS_OK;
        AnswerResPack answerResPack = new AnswerResPack(ProtocolPacket.STATUS_OK,pack);
        UserCallMessage callMsg = new UserCallMessage();
        int audioMode;
        callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWERED;
        callMsg.devId = devID;
        callMsg.callId = pack.callID;
        callMsg.callType = type;
        callMsg.operaterId = pack.answerer;

        answer = pack.answerer;
        
        state = CommonCall.CALL_STATE_CONNECTED;
        remoteRtpPort = pack.answererRtpPort;
        remoteRtpAddress = pack.answererRtpIP;
        if(type==CALL_TYPE_BROADCAST){
            audioMode = AudioMode.SEND_ONLY_MODE;
            callMsg.localRtpPort = pack.answererRtpPort;
        }else if(type == CALL_TYPE_EMERGENCY){
            audioMode = AudioMode.NO_SEND_RECV_MODE;
            callMsg.localRtpPort = localRtpPort;
        }else{
            audioMode = AudioMode.SEND_RECV_MODE;
            callMsg.localRtpPort = localRtpPort;
        }

        callMsg.remoteRtpPort = remoteRtpPort;
        callMsg.remoteRtpAddress = remoteRtpAddress;
        callMsg.rtpSample = pack.sample;
        callMsg.rtpPTime = pack.pTime;
        callMsg.rtpCodec = pack.codec;
        callMsg.audioMode = audioMode;
        
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call Answer Req for CallID = %s, and Send Answer Res to Server! ",devID,callID);
//        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Open Audio sample=%d,ptime=%d,codec=%d",devID,pack.sample,pack.pTime,pack.codec);
//        audioId = AudioMgr.OpenAudio(devID,localRtpPort,remoteRtpPort,remoteRtpAddress,pack.sample,pack.pTime,pack.codec,audioMode);

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
        callMsg.callType = type;
        callMsg.type = UserCallMessage.CALL_MESSAGE_INVITE_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }
    
    public void AnswerTimeOver(){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.callType = type;
        callMsg.type = UserCallMessage.CALL_MESSAGE_ANSWER_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }

    public void EndTimeOver(){
        UserCallMessage callMsg = new UserCallMessage();
        callMsg.devId = devID;
        callMsg.callId = callID;
        callMsg.callType = type;
        callMsg.type = UserCallMessage.CALL_MESSAGE_END_FAIL;
        callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
        HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
    }


    public boolean UpdateTimeOver(){
        boolean removeCall = false;
        updateTimeOverCount++;
        if(updateTimeOverCount>=2){
            UserCallMessage callMsg = new UserCallMessage();
            callMsg.devId = devID;
            callMsg.callId = callID;
            callMsg.callType = type;
            callMsg.type = UserCallMessage.CALL_MESSAGE_UPDATE_FAIL;
            callMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_TIMEOVER);
            HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_CALL_INFO,callMsg);
            removeCall = true;
    //        if(!audioId.isEmpty())
    //            AudioMgr.CloseAudio(audioId);
        }
        return removeCall;
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
        UserInterface.PrintLog("Build Invite on Local  %s:%d",invitePack.callerRtpIP,localRtpPort);
        invitePack.callerRtpPort = localRtpPort;
        if(type==CALL_TYPE_BROADCAST){
            invitePack.autoAnswerTime = PhoneParam.BROADCALL_ANSWER_WAIT;
        }else{
            invitePack.autoAnswerTime  = -1;
        }
        return invitePack;
    }

    private EndReqPack BuildEndPacket(int reason){
        EndReqPack endPacket = new EndReqPack();

        endPacket.sender = devID;
        endPacket.receiver = PhoneParam.CALL_SERVER_ID;
        endPacket.type = ProtocolPacket.END_REQ;
        endPacket.msgID = UniqueIDManager.GetUniqueID(caller,UniqueIDManager.MSG_UNIQUE_ID);

        endPacket.callID = callID;
        endPacket.endDevID = devID;
        endPacket.endReason = reason;

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
        UserInterface.PrintLog("Build Answer on Local  %s:%d",answerReqPack.answererRtpIP,localRtpPort);

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

    private StartVideoReqPack BuildStartVideoPacket(){
        StartVideoReqPack startPack = new StartVideoReqPack();

        startPack.type = ProtocolPacket.CALL_VIDEO_INVITE_REQ;
        startPack.sender = devID;
        startPack.receiver = PhoneParam.CALL_SERVER_ID;
        startPack.msgID = UniqueIDManager.GetUniqueID(devID,UniqueIDManager.MSG_UNIQUE_ID);

        startPack.callID = callID;
        startPack.startVideoDevId = devID;

        return startPack;
    }

    private AnswerVideoReqPack BuildAnswerVideoPacket(){
        AnswerVideoReqPack answerPack = new AnswerVideoReqPack();
        
        answerPack.type = ProtocolPacket.CALL_VIDEO_ANSWER_REQ;
        answerPack.sender = devID;
        answerPack.receiver = PhoneParam.CALL_SERVER_ID;
        answerPack.msgID = UniqueIDManager.GetUniqueID(devID,UniqueIDManager.MSG_UNIQUE_ID);

        answerPack.callId = callID;
        answerPack.answerDevId= devID;

        return answerPack;
    }

    private StopVideoReqPack BuildStopVideoPacket(){
        StopVideoReqPack stopPack = new StopVideoReqPack();

        stopPack.type = ProtocolPacket.CALL_VIDEO_END_REQ;
        stopPack.sender = devID;
        stopPack.receiver = PhoneParam.CALL_SERVER_ID;
        stopPack.msgID = UniqueIDManager.GetUniqueID(devID,UniqueIDManager.MSG_UNIQUE_ID);

        stopPack.callID = callID;
        stopPack.stopVideoDevId = devID;
        return stopPack;
    }

}
