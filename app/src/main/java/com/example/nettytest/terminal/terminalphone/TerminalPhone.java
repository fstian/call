package com.example.nettytest.terminal.terminalphone;

import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.AnswerVideoReqPack;
import com.example.nettytest.pub.protocol.AnswerVideoResPack;
import com.example.nettytest.pub.protocol.CancelReqPack;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.pub.protocol.ConfigReqPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.ListenCallReqPack;
import com.example.nettytest.pub.protocol.ListenCallResPack;
import com.example.nettytest.pub.protocol.ListenClearReqPack;
import com.example.nettytest.pub.protocol.ListenClearResPack;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.StartVideoReqPack;
import com.example.nettytest.pub.protocol.StartVideoResPack;
import com.example.nettytest.pub.protocol.StopVideoReqPack;
import com.example.nettytest.pub.protocol.StopVideoResPack;
import com.example.nettytest.pub.protocol.SystemConfigReqPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.protocol.TransferChangeReqPack;
import com.example.nettytest.pub.protocol.TransferChangeResPack;
import com.example.nettytest.pub.protocol.TransferReqPack;
import com.example.nettytest.pub.protocol.TransferResPack;
import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.pub.result.OperationResult;
import com.example.nettytest.userinterface.ListenCallMessage;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.TransferMessage;
import com.example.nettytest.userinterface.UserConfig;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.DevQueryReqPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.RegReqPack;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.terminal.terminalcall.TerminalCallManager;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;

public class TerminalPhone extends PhoneDevice {

    int regWaitCount;
    TerminalDeviceInfo info;

    TerminalCallManager callManager;

    public String areaId;

    public boolean isListenCall;

    
    public TerminalPhone(final String devid, final int t){
        this.id = devid;
        type = t;
        isReg = false;
        regWaitCount = 0;
        info = new TerminalDeviceInfo();
        areaId = "";
        isListenCall = false;

        callManager = new TerminalCallManager(type);
    }

    public int QueryDevs(){
        int result = ProtocolPacket.STATUS_OK;

        DevQueryReqPack devReqP = BuildDevReqPacket(id);
        Transaction devReqTrans = new Transaction(id,devReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Send Dev Query Req!",id);
        HandlerMgr.AddPhoneTrans(devReqP.msgID,devReqTrans);

        return result;
    }

    public int CallTransfer(String areaId,boolean state){
        int result = ProtocolPacket.STATUS_OK;

        TransferReqPack transferReqP = BuildCallTransferPacket(id,areaId,state);
        Transaction transferReqTrans = new Transaction(id,transferReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Send Dev Query Req!",id);
        HandlerMgr.AddPhoneTrans(transferReqP.msgID,transferReqTrans);

        return result;
    }

    public int ListenCall(boolean state){
        int result = ProtocolPacket.STATUS_OK;

        ListenCallReqPack listenReqP = BuildListenCallPacket(id,state);
        Transaction listenReqTrans = new Transaction(id,listenReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        if(state)
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Enable Call Listen!",id);
        else
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Disable Call Listen!",id);
        HandlerMgr.AddPhoneTrans(listenReqP.msgID,listenReqTrans);

        return result;
    }

    public void SetConfig(TerminalDeviceInfo info){
        this.info = info;
    }

    public int QueryConfig(){
        int result = ProtocolPacket.STATUS_OK;

        ConfigReqPack configReqP = BuildConfigReqPacket(id);
        Transaction devReqTrans = new Transaction(id,configReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Send Config Query Req!",id);
        HandlerMgr.AddPhoneTrans(configReqP.msgID,devReqTrans);

        return result;

    }

    public int QuerySystemConfig(){
        int result = ProtocolPacket.STATUS_OK;

        SystemConfigReqPack configReqP = BuildSystemConfigReqPacket(id);
        Transaction devReqTrans = new Transaction(id,configReqP,Transaction.TRANSCATION_DIRECTION_C2S);
        LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Send System Config Query Req!",id);
        HandlerMgr.AddPhoneTrans(configReqP.msgID,devReqTrans);

        return result;
    }

    public int RecvUnsupport(ProtocolPacket packet){      
        LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Phone %s Recv Unsupport %s(%d) Req!",id,ProtocolPacket.GetTypeName(packet.type),packet.type);
        ProtocolPacket resPack = null;
        switch(packet.type){
            case ProtocolPacket.REG_REQ:
                resPack = new RegResPack(ProtocolPacket.STATUS_NOTSUPPORT,(RegReqPack)packet);
                break;
            case ProtocolPacket.DEV_QUERY_REQ:
                resPack = new DevQueryResPack(ProtocolPacket.STATUS_NOTSUPPORT,(DevQueryReqPack)packet);
                break;
            case ProtocolPacket.CALL_UPDATE_REQ:
                resPack = new UpdateResPack(ProtocolPacket.STATUS_NOTSUPPORT,(UpdateReqPack) packet);
                break;
            case ProtocolPacket.DEV_CONFIG_REQ:
                resPack = new ConfigResPack(ProtocolPacket.STATUS_NOTSUPPORT,(ConfigReqPack) packet);
                break;
            case ProtocolPacket.SYSTEM_CONFIG_REQ:
                resPack = new SystemConfigResPack(ProtocolPacket.STATUS_NOTSUPPORT,(SystemConfigReqPack)packet);
                break;
        }
        if(resPack!=null){
            Transaction devResTrans = new Transaction(id,packet,resPack,Transaction.TRANSCATION_DIRECTION_C2S);
            HandlerMgr.AddPhoneTrans(resPack.msgID,devResTrans);
        }
        return 0;
    }

    public int GetCallCount(){
        return callManager.GetCallCount();
    }

    public void UpdateRegStatus(int status,RegResPack resP){
        UserRegMessage regMsg = new UserRegMessage();

        regMsg.devId = id;

        if(status ==ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Update Reg Status Success ",id);
            isReg = true;
            regWaitCount = PhoneParam.CLIENT_REG_EXPIRE;
            regMsg.type = UserCallMessage.REGISTER_MESSAGE_SUCC;
            regMsg.areaId = resP.areaId;
            regMsg.areaName = resP.areaName;
            regMsg.transferAreaId = resP.transferAreaId;
            regMsg.enableListenCall = resP.listenCallEnable;
            regMsg.snapPort = resP.snapPort;
            regMsg.isReg = true;
            this.areaId = resP.areaId;
            isListenCall = resP.listenCallEnable;
            HandlerMgr.TerminalStartSnap(resP.snapPort);
        }else if(status==ProtocolPacket.STATUS_TIMEOVER){
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_WARN,"Phone %s Reg TimerOver ",id);
            isReg = false;
            regWaitCount = 0;

            regMsg.type = UserCallMessage.REGISTER_MESSAGE_FAIL;
            regMsg.reason = FailReason.FAIL_REASON_TIMEOVER;
        }else{
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_WARN,"Phone %s Reg Fail for unkonw reason ",id);
            isReg = false;
            regWaitCount = PhoneParam.CLIENT_REG_EXPIRE/4+30;

            regMsg.type = UserCallMessage.REGISTER_MESSAGE_FAIL;
            regMsg.reason = FailReason.FAIL_REASON_UNKNOW;
        }

        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_REG_INFO,regMsg);
    }

    public void UpdateDevLists(DevQueryResPack p){
        UserDevsMessage devsMsg = new UserDevsMessage();

        devsMsg.type = UserMessage.DEV_MESSAGE_LIST;
        devsMsg.devId = id;
        for(int iTmp=0;iTmp<p.phoneList.size();iTmp++){
            PhoneDevice pDevice = p.phoneList.get(iTmp);
            if(pDevice.type == PhoneDevice.BED_CALL_DEVICE) {
                UserDevice tDevice = new UserDevice();
                tDevice.devid = pDevice.id;
                tDevice.isRegOk = pDevice.isReg;
                tDevice.type = pDevice.type;
                tDevice.bedName = pDevice.bedName;
                devsMsg.deviceList.add(tDevice);
            }
        }
        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_DEVICES_INFO,devsMsg);
    }

    public void UpdateCallTransfer(TransferResPack p){
        TransferMessage transferMsg = new TransferMessage();

        if(p.status == ProtocolPacket.STATUS_OK){

            transferMsg.type = UserMessage.CALL_TRANSFER_SUCC;
            transferMsg.devId = id;
            transferMsg.state = p.state;
            transferMsg.transferAreaId = p.transferAreaId;
            transferMsg.reason = OperationResult.GetUserFailReason(p.status);

            if(transferMsg.state)
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Set Call Transfer to %s Success ",id,transferMsg.transferAreaId);
            else
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Clear Call Transfer Success ",id);

        }else{
            transferMsg.type = UserMessage.CALL_TRANSFER_FAIL;
            transferMsg.devId = id;
            transferMsg.reason = OperationResult.GetUserFailReason(p.status);
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Set Call Transfer Fail, result is %s ",id,p.result);
        }
        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_TRANSFER_INFO,transferMsg);
    }

    public void UpdateCallTransfer(TransferChangeReqPack p){
        TransferMessage transferMsg = new TransferMessage();
        TransferChangeResPack resP = new TransferChangeResPack(ProtocolPacket.STATUS_OK,p);


        transferMsg.type = UserMessage.CALL_TRANSFER_CHANGE;
        transferMsg.devId = id;
        transferMsg.state = p.state;
        transferMsg.transferAreaId = p.transferAreaId;

        Transaction trans = new Transaction(id,p,resP,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(resP.msgID,trans);

        if(transferMsg.state)
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Call Transfer to %s ",id,transferMsg.transferAreaId);
        else
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Recv Clear Call Transfer ",id);

        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_TRANSFER_INFO,transferMsg);
    }

    public void UpdateListenCall(ListenCallResPack p){
        ListenCallMessage listenMsg = new ListenCallMessage();

        listenMsg.devId = id;
        listenMsg.reason = OperationResult.GetUserFailReason(p.status);

        if(p.status==ProtocolPacket.STATUS_OK){
            listenMsg.type = UserMessage.CALL_LISTEN_SUCC;
            listenMsg.state = p.state;
            if(p.state)
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Set Call Listen Succ",id);
            else
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Clear Call Listen Succ",id);
            isListenCall = p.state;
        }else{
            listenMsg.type = UserMessage.CALL_LISTEN_FAIL;
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Set Call Listen Fail, result is %s ",id,p.result);
        }

        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_LISTEN_CALL_INFO,listenMsg);
        
    }

    public void UpdateListenCall(ListenClearReqPack p){
        ListenCallMessage listenMsg = new ListenCallMessage();
        ListenClearResPack resP = new ListenClearResPack(ProtocolPacket.STATUS_OK,p);

        listenMsg.type = UserMessage.CALL_LISTEN_CHANGE;
        listenMsg.devId = id;
        listenMsg.reason = OperationResult.GetUserFailReason(ProtocolPacket.STATUS_OK);
        listenMsg.state = p.status;

        isListenCall = p.status;

        Transaction trans = new Transaction(id,p,resP,Transaction.TRANSCATION_DIRECTION_C2S);
        HandlerMgr.AddPhoneTrans(resP.msgID,trans);

        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_LISTEN_CALL_INFO,listenMsg);
        LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_DEBUG,"Phone %s Type %s Recv Listen Change to %b",id,PhoneDevice.GetTypeName(type),p.status);

        if(!p.status){
            callManager.CancelListenCall(id);
        }
    }

    public void UpdateConfig(ConfigResPack res){
        UserConfigMessage configMsg = new UserConfigMessage();
        configMsg.type = UserMessage.CONFIG_MESSAGE_LIST;
        configMsg.devId = res.devId;
        for(int iTmp=0;iTmp<res.params.size();iTmp++){
            ConfigItem item = res.params.get(iTmp);
            UserConfig config = new UserConfig();
            config.param_id = item.param_id;
            config.param_name = item.param_name;
            config.param_value = item.param_value;
            config.param_unit = item.param_unit;
            configMsg.paramList.add(config);
        }
        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_CONFIG_INFO,configMsg);
    }

    public void UpdateSystemConfig(SystemConfigResPack res){
        UserConfigMessage configMsg = new UserConfigMessage();
        configMsg.type = UserMessage.CONFIG_MESSAGE_LIST;
        configMsg.devId = res.devId;
        for(int iTmp=0;iTmp<res.params.size();iTmp++){
            ConfigItem item = res.params.get(iTmp);
            UserConfig config = new UserConfig();
            config.param_id = item.param_id;
            config.param_name = item.param_name;
            config.param_value = item.param_value;
            config.param_unit = item.param_unit;
            configMsg.paramList.add(config);
        }
        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_SYSTEM_CONFIG_INFO,configMsg);
    }

    public void UpdateCallStatus(ProtocolPacket packet){
        callManager.UpdateStatus(id, packet);
    }

    public void UpdateCalTimeOver(ProtocolPacket packet){
        callManager.UpdateTimeOver(id,packet);
    }

    public void RecvIncomingCall(InviteReqPack packet){
        callManager.RecvIncomingCall(id,packet);
    }

    public void RecvAnswerCall(AnswerReqPack packet){
        if(CheckAnsweredEnable()){
            callManager.RecvAnswerCall(id,packet);
        }else{
            LogWork.Print(LogWork.TERMINAL_CALL_MODULE,LogWork.LOG_WARN,"Phone %s Type %s Recv %s Answer For Call %s , but %s",id,PhoneDevice.GetTypeName(type),packet.answerer,packet.callID,ProtocolPacket.GetResString(ProtocolPacket.STATUS_NOTSUPPORT));
            AnswerResPack answerResP = new AnswerResPack(ProtocolPacket.STATUS_NOTSUPPORT,packet);
            Transaction trans = new Transaction(id,packet,answerResP,Transaction.TRANSCATION_DIRECTION_C2S);
            HandlerMgr.AddPhoneTrans(answerResP.msgID,trans);
        }
    }

    public void RecvEndCall(EndReqPack packet){
        callManager.RecvEndCall(id,packet);
    }

    public void RecvCancelCall(CancelReqPack packet){
        callManager.RecvCancelCall(id,packet);
    }

    public void RecvStartVideoReq(StartVideoReqPack packet){
        callManager.RecvStartVideoReq(id,packet);
    }

    public void RecvStartVideoRes(StartVideoResPack pack){
        // do nothing;
    }

    public void RecvAnswerVideoReq(AnswerVideoReqPack packet){
        callManager.RecvAnswerVideoReq(id,packet);
    }

    public void RecvAnswerVideoRes(AnswerVideoResPack pack){
        // do nothing;
    }

    public void RecvStopVideoReq(StopVideoReqPack packet){
        callManager.RecvStopVideoReq(id,packet);
    }

    public void RecvStopVideoRes(StopVideoResPack pack){
        // do nothing;
    }
    public boolean CheckAnsweredEnable(){
        boolean result = false;
        if(type==BED_CALL_DEVICE||type==NURSE_CALL_DEVICE||type==CORRIDOR_CALL_DEVICE){
            result = true;
        }
        return result;
    }

    public String MakeOutGoingCall(String dst,int callType){
        String callid;

        callid = callManager.BuildCall(id,info,dst,callType);
        return callid;
    }

    public void UpdateSecondTick(){
        callManager.UpdateSecondTick();
        regWaitCount--;
        if (regWaitCount < PhoneParam.CLIENT_REG_EXPIRE/4) {
            RegReqPack regPack = BuildRegPacket(id,type);
            Transaction regTransaction = new Transaction(id,regPack,Transaction.TRANSCATION_DIRECTION_C2S);
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s, begine Register!",id);
            if(HandlerMgr.AddPhoneTrans(regPack.msgID,regTransaction)){
                regWaitCount = PhoneParam.CLIENT_REG_EXPIRE;
            }else
                regWaitCount = 0;
        }
    }

    public byte[] MakeCallSnap(){
        byte[] res;
        res = callManager.MakeCallSnap(id,isReg);
        return res;
    }

    public int EndCall(String callid){
        return callManager.EndCall(id,callid);
    }

    public int EndCall(int type){
        return callManager.EndCall(id,type);
    }

    public int AnswerCall(String callid){
        return callManager.AnswerCall(id,callid);
    }

    public int StartVideo(String callid){
        return callManager.StartVideo(id,callid);
    }

    public int AnswerVideo(String callid){
        return callManager.AnswerVideo(id,callid);
    }

    public int StopVideo(String callid){
        return callManager.StopVideo(id,callid);
    }

    private RegReqPack BuildRegPacket(String devid, int type){
        RegReqPack regPack = new RegReqPack();

        regPack.sender = devid;
        regPack.receiver = PhoneParam.CALL_SERVER_ID;
        regPack.type = ProtocolPacket.REG_REQ;
        regPack.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);

        regPack.address = PhoneParam.GetLocalAddress();
        regPack.devID = devid;
        regPack.devType = type;
        regPack.expireTime = PhoneParam.CLIENT_REG_EXPIRE;

        return regPack;
    }

    private DevQueryReqPack BuildDevReqPacket(String devid){
        DevQueryReqPack devReqP = new DevQueryReqPack();

        devReqP.sender = devid;
        devReqP.receiver = PhoneParam.CALL_SERVER_ID;
        devReqP.type = ProtocolPacket.DEV_QUERY_REQ;
        devReqP.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);

        devReqP.devid = devid;
        return devReqP;
    }

    private TransferReqPack BuildCallTransferPacket(String devid,String areaid,boolean state){
        TransferReqPack transferReqP = new TransferReqPack(devid,areaid);
        
        transferReqP.sender = devid;
        transferReqP.receiver = PhoneParam.CALL_SERVER_ID;
        transferReqP.transferEnabled = state;
        transferReqP.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);
        return transferReqP;
    }

    private ListenCallReqPack BuildListenCallPacket(String devid,boolean state){
        ListenCallReqPack listenReqP = new ListenCallReqPack(devid, state);
        
        listenReqP.sender = devid;
        listenReqP.receiver = PhoneParam.CALL_SERVER_ID;
        listenReqP.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);
        return listenReqP;
   }

    private ConfigReqPack BuildConfigReqPacket(String devid){
        ConfigReqPack configReqP = new ConfigReqPack();

        configReqP.sender = devid;
        configReqP.receiver = PhoneParam.CALL_SERVER_ID;
        configReqP.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);

        configReqP.devId = devid;

        return configReqP;
    }

    private SystemConfigReqPack BuildSystemConfigReqPacket(String devid){
        SystemConfigReqPack configReqP = new SystemConfigReqPack();

        configReqP.sender = devid;
        configReqP.receiver = PhoneParam.CALL_SERVER_ID;
        configReqP.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);

        configReqP.devId = devid;

        return configReqP;
    }
}

