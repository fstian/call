package com.example.nettytest.terminal.terminalphone;

import android.os.Handler;
import android.os.Message;

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

    TerminalCallManager callManager;
//    Handler msgHandler;

    public TerminalPhone(final String devid, final int t,Handler handler){
        this.id = devid;
        type = t;
        isReg = false;
        regWaitCount = 0;
//        msgHandler = handler;

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
                tDevice.isReg = pDevice.isReg;
                tDevice.type = pDevice.type;
                devsMsg.deviceList.add(tDevice);
            }
        }
        HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_DEVICES_INFO,devsMsg);
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

    public String MakeOutGoingCall(String dst,int callType){
        String callid;

        callid = callManager.BuildCall(id,dst,callType);
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

    public void SetMessageHandler(Handler h){
        //msgHandler = h;
    }

    public int EndCall(String callid){
        return callManager.EndCall(callid);
    }

    public int AnswerCall(String callid){
        return callManager.AnswerCall(callid);
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
}
