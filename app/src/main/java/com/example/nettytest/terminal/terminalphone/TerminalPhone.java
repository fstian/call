package com.example.nettytest.terminal.terminalphone;

import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.pub.protocol.ConfigReqPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.SystemConfigReqPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.protocol.UpdateReqPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserConfig;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.FailReason;
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

    public TerminalPhone(final String devid, final int t){
        this.id = devid;
        type = t;
        isReg = false;
        regWaitCount = 0;
        info = new TerminalDeviceInfo();

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
                RegResPack regResP= new RegResPack(ProtocolPacket.STATUS_NOTSUPPORT,(RegReqPack)packet);
                resPack = regResP;
                break;
            case ProtocolPacket.DEV_QUERY_REQ:
                DevQueryResPack devResP= new DevQueryResPack(ProtocolPacket.STATUS_NOTSUPPORT,(DevQueryReqPack)packet);
                resPack = devResP;
                break;
            case ProtocolPacket.CALL_UPDATE_REQ:
                UpdateResPack updateResP = new UpdateResPack(ProtocolPacket.STATUS_NOTSUPPORT,(UpdateReqPack) packet);
                resPack = updateResP;
                break;
            case ProtocolPacket.DEV_CONFIG_REQ:
                ConfigResPack configResP = new ConfigResPack(ProtocolPacket.STATUS_NOTSUPPORT,(ConfigReqPack) packet);
                resPack = configResP;
                break;
            case ProtocolPacket.SYSTEM_CONFIG_REQ:
                SystemConfigResPack sysConfigResP = new SystemConfigResPack(ProtocolPacket.STATUS_NOTSUPPORT,(SystemConfigReqPack)packet);
                resPack = sysConfigResP;
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

    public void UpdateRegStatus(int status){
        UserRegMessage regMsg = new UserRegMessage();

        regMsg.devId = id;

        if(status ==ProtocolPacket.STATUS_OK){
            LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"Phone %s Update Reg Status Success ",id);
            isReg = true;
            regWaitCount = PhoneParam.CLIENT_REG_EXPIRE;
            regMsg.type = UserCallMessage.REGISTER_MESSAGE_SUCC;

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
        callManager.RecvIncomingCall(packet);
    }

    public void RecvAnswerCall(AnswerReqPack packet){
        callManager.RecvAnswerCall(id,packet);
    }

    public void RecvEndCall(EndReqPack packet){
        callManager.RecvEndCall(id,packet);
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

    public int AnswerCall(String callid){
        return callManager.AnswerCall(id,callid);
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

