package com.example.nettytest.terminal.terminalphone;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.androidport.port.audio.AudioMgr;
import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.DeviceStatistics;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.JsonPort;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.MsgReceiver;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.TerminalStatistics;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.AnswerVideoReqPack;
import com.example.nettytest.pub.protocol.AnswerVideoResPack;
import com.example.nettytest.pub.protocol.CancelReqPack;
import com.example.nettytest.pub.protocol.CancelResPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ListenCallResPack;
import com.example.nettytest.pub.protocol.ListenClearReqPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.StartVideoReqPack;
import com.example.nettytest.pub.protocol.StartVideoResPack;
import com.example.nettytest.pub.protocol.StopVideoReqPack;
import com.example.nettytest.pub.protocol.StopVideoResPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.protocol.TransferChangeReqPack;
import com.example.nettytest.pub.protocol.TransferResPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalPhoneManager {
    HashMap<String, TerminalPhone> clientPhoneLists;
    
    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;
    public static final int MSG_REQ_TIMEOVER = 3;
    Thread snapThread = null;
    long runSecond = 0;
     
    MsgReceiver userMsgReceiver = null;
    TerminalPhoneMsgRecevier phoneMsgRecevier;

    class TerminalPhoneMsgRecevier extends MsgReceiver{
        public TerminalPhoneMsgRecevier(String name){
            super(name);
        }

        @Override
        public void CallPubMessageRecv(ArrayList<CallPubMessage> list) {
            CallPubMessage msg;
            ProtocolPacket packet;
            int type;
            synchronized (TerminalPhoneManager.class) {
                while(list.size()>0) {
                    msg = list.remove(0);
                    type = msg.arg1;
                    switch (type) {
                        case MSG_NEW_PACKET:
                            packet = (ProtocolPacket) msg.obj;
                            PacketRecvProcess(packet);
                            break;
                        case MSG_SECOND_TICK:
                            for (TerminalPhone phone : clientPhoneLists.values()) {
                                phone.UpdateSecondTick();
                            }
                            break;
                        case MSG_REQ_TIMEOVER:
                            packet = (ProtocolPacket) msg.obj;
                            PacketTimeOverProcess(packet);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + type);
                    }
                }
            }
        }
    }

    public void SetMsgReceiver(MsgReceiver receiver){
        userMsgReceiver = receiver;
    }

    public TerminalPhoneManager(){
        clientPhoneLists = new HashMap<>();
        phoneMsgRecevier = new TerminalPhoneMsgRecevier("TermMsgReceiver");

        HandlerMgr.ReadSystemType();

        new Timer("TerminalTimeTick").schedule(new TimerTask() {
            @Override
            public void run() {
                CallPubMessage phonemsg = new CallPubMessage();
                phonemsg.arg1 = TerminalPhoneManager.MSG_SECOND_TICK;
                phonemsg.obj = new UserMessage();
                HandlerMgr.PostTerminalPhoneMsg(phonemsg);

                HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_TEST_TICK,new UserMessage());

                HandlerMgr.TerminalPhoneTransactionTick();
                runSecond++;
            }
        },0,1000);

    }


    public int StartSnap(int port){
        if(snapThread==null){
            snapThread = new Thread("TerminalSnapThread"){
                @Override
                public void run() {
                    byte[] recvBuf = new byte[1024];
                    DatagramPacket recvPack;
                    DatagramSocket testSocket;
                    byte[] snapResult;
                    testSocket = SystemSnap.OpenSnapSocket(port,PhoneParam.SNAP_TERMINAL_GROUP);
                    DatagramPacket resPack;
                    int value;
                    if (testSocket != null) {
                        while (!testSocket.isClosed()) {
                            java.util.Arrays.fill(recvBuf,(byte)0);
                            recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                            try {
                                testSocket.receive(recvPack);
                                if (recvPack.getLength() > 0) {
                                    String recv = new String(recvBuf, "UTF-8");
                                    JSONObject json = JSONObject.parseObject(recv);
                                    if(json==null)
                                        continue;
                                    int type = json.getIntValue(SystemSnap.SNAP_CMD_TYPE_NAME);
                                    synchronized (TerminalPhoneManager.class) {
                                        if (type == SystemSnap.SNAP_TERMINAL_CALL_REQ) {
                                            String devId = JsonPort.GetJsonString(json,SystemSnap.SNAP_DEVID_NAME);
                                            snapResult = MakeCallsSnap(devId);
                                            if (snapResult != null) {
//                                                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Get Terminal Call Snap for dev %s, total %d bytes, send to %s:%d",devId,snapResult.length,recvPack.getAddress().getHostAddress(),recvPack.getPort());
                                                resPack = new DatagramPacket(snapResult, snapResult.length, recvPack.getAddress(), recvPack.getPort());
                                                testSocket.send(resPack);
                                            }
                                        } else if (type == SystemSnap.SNAP_TERMINAL_TRANS_REQ) {
//                                                ArrayList<byte[]> resList;
//                                                resList = HandlerMgr.GetTerminalTransInfo();
//                                                for (byte[] data : resList) {
//                                                    resPack = new DatagramPacket(data, data.length, recvPack.getAddress(), recvPack.getPort());
//                                                    testSocket.send(resPack);
//                                                }
                                        } else if (type == SystemSnap.LOG_CONFIG_REQ_CMD) {

                                            LogWork.terminalNetModuleLogEnable = json.getIntValue(SystemSnap.LOG_TERMINAL_NET_NAME) == 1;

                                            LogWork.terminalDeviceModuleLogEnable = json.getIntValue(SystemSnap.LOG_TERMINAL_DEVICE_NAME) == 1;

                                            LogWork.terminalCallModuleLogEnable = json.getIntValue(SystemSnap.LOG_TERMINAL_CALL_NAME) == 1;

                                            LogWork.terminalPhoneModuleLogEnable = json.getIntValue(SystemSnap.LOG_TERMINAL_PHONE_NAME) == 1;

                                            LogWork.terminalUserModuleLogEnable = json.getIntValue(SystemSnap.LOG_TERMINAL_USER_NAME) == 1;

                                            LogWork.terminalAudioModuleLogEnable = json.getIntValue(SystemSnap.LOG_TERMINAL_AUDIO_NAME) == 1;

                                            LogWork.transactionModuleLogEnable = json.getIntValue(SystemSnap.LOG_TRANSACTION_NAME) == 1;

                                            LogWork.debugModuleLogEnable = json.getIntValue(SystemSnap.LOG_DEBUG_NAME) == 1;

                                            LogWork.bLogToFiles = json.getIntValue(SystemSnap.LOG_WIRTE_FILES_NAME) == 1;

                                            value = json.getIntValue(SystemSnap.LOG_FILE_INTERVAL_NAME);
                                            if (value <= 0)
                                                value = 1;
                                            LogWork.logInterval = value;

                                            LogWork.dbgLevel = json.getIntValue(SystemSnap.LOG_DBG_LEVEL_NAME);
                                        } else if (type == SystemSnap.AUDIO_CONFIG_WRITE_REQ_CMD) {
                                            PhoneParam.callRtpCodec = json.getIntValue(SystemSnap.AUDIO_RTP_CODEC_NAME);
                                            PhoneParam.callRtpDataRate = json.getIntValue(SystemSnap.AUDIO_RTP_DATARATE_NAME);
                                            PhoneParam.callRtpPTime = json.getIntValue(SystemSnap.AUDIO_RTP_PTIME_NAME);
                                            PhoneParam.aecDelay = json.getIntValue(SystemSnap.AUDIO_AEC_DELAY_NAME);
                                            PhoneParam.aecMode = json.getIntValue(SystemSnap.AUDIO_AEC_MODE_NAME);
                                            PhoneParam.nsMode = json.getIntValue(SystemSnap.AUDIO_NS_MODE_NAME);
                                            
                                            value = json.getIntValue(SystemSnap.AUDIO_NS_RANGE_NAME);
                                            if(value/100*100<value){
                                                value=(value+100)/100*100;
                                            }
                                            PhoneParam.nsRange= value;
                                            
                                            value = json.getIntValue(SystemSnap.AUDIO_NS_TIME_NAME);
                                            if(value/200*20<value){
                                                value=(value+20)/20*20;
                                            }
                                            if(value==1)
                                                value = 1;
                                            PhoneParam.nsTime = value;

                                            PhoneParam.nsThreshold = json.getIntValue(SystemSnap.AUDIO_NS_THRESHOLD_NAME);
                                            PhoneParam.agcMode = json.getIntValue(SystemSnap.AUDIO_AGC_MODE_NAME);
                                            PhoneParam.inputMode = json.getIntValue(SystemSnap.AUDIO_INPUT_MODE_NAME);
                                            PhoneParam.inputGain = json.getIntValue(SystemSnap.AUDIO_INPUT_GAIN_NAME);
                                            PhoneParam.outputMode = json.getIntValue(SystemSnap.AUDIO_OUTPUT_MODE_NAME);
                                            PhoneParam.outputGain = json.getIntValue(SystemSnap.AUDIO_OUTPUT_GAIN_NAME);
                                            PhoneParam.audioMode= json.getIntValue(SystemSnap.AUDIO_MODE_NAME);
                                            PhoneParam.audioSpeaker = json.getIntValue(SystemSnap.AUDIO_SPEAKER_NAME);
                                            UserMessage msg = new UserMessage();
                                            msg.type = UserMessage.SNAP_CONFIG;
                                            HandlerMgr.SendMessageToUser(UserCallMessage.MESSAGE_SNAP,msg);
                                        }else if(type == SystemSnap.AUDIO_CONFIG_READ_REQ_CMD){
                                            snapResult = MakeAudioConfig();
                                            if (snapResult != null) {
//                                                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Get Terminal Call Snap for dev %s, total %d bytes, send to %s:%d",devId,snapResult.length,recvPack.getAddress().getHostAddress(),recvPack.getPort());
                                                resPack = new DatagramPacket(snapResult, snapResult.length, recvPack.getAddress(), recvPack.getPort());
                                                testSocket.send(resPack);
                                            }
                                        } else if (type == SystemSnap.SNAP_DEV_REQ) {
                                            int sendCount = 0;
                                            for (TerminalPhone dev : clientPhoneLists.values()) {
                                                JSONObject resJson = new JSONObject();
                                                resJson.put(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_DEV_RES);
                                                resJson.put(SystemSnap.SNAP_AREAID_NAME,dev.areaId);
                                                resJson.put(SystemSnap.SNAP_DEVID_NAME, dev.id);
                                                if(dev.isReg)
                                                    resJson.put(SystemSnap.SNAP_REG_NAME, 1);
                                                else
                                                    resJson.put(SystemSnap.SNAP_REG_NAME, 0);
                                                resJson.put(SystemSnap.SNAP_DEVTYPE_NAME, dev.type);
                                                byte[] resBuf = resJson.toString().getBytes();
                                                resPack = new DatagramPacket(resBuf, resBuf.length, recvPack.getAddress(), recvPack.getPort());
                                                testSocket.send(resPack);
                                                sendCount++;
                                                if((sendCount%20)==19){
                                                    try {
                                                        Thread.sleep(20);
                                                    } catch (InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                                resJson.clear();
                                            }
                                        } else if (type == SystemSnap.SNAP_DEL_LOG_REQ) {
                                           LogWork.ResetLogIndex();
                                        }else if(type==SystemSnap.SNAP_SYSTEM_INFO_REQ){
                                            byte[] systemInfo;
                                            String curArea = JsonPort.GetJsonString(json,SystemSnap.SNAP_AREAID_NAME);
                                            systemInfo = MakeSystemInfo(curArea);
                                            try {
                                                Thread.sleep((int)(Math.random()*1000.0));
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                            if(systemInfo!=null){
                                                resPack = new DatagramPacket(systemInfo, systemInfo.length, recvPack.getAddress(), recvPack.getPort());
                                                testSocket.send(resPack);
                                            }
                                        }
                                    }
                                    json.clear();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }catch(Exception ee){
                                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Socket of Terminal Snap err with %s",ee.getMessage());
                            }
                        }
                    }
                }
            };
            snapThread.start();
        }
        return 0;
    }

    public void AddDevice(TerminalPhone phone){
        TerminalPhone matchedDev;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(phone.id);
            if(matchedDev==null)
                clientPhoneLists.put(phone.id,phone);

        }

    }

    public void RemovePhone(String id){
        TerminalPhone matchedDev;
        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(id);
            if(matchedDev!=null){
                matchedDev.UpdateRegStatus(ProtocolPacket.STATUS_NOTFOUND,null);
                clientPhoneLists.remove(id);
            }
        }
    }

    public int GetCallCount(){
        int count = 0;
        synchronized (TerminalPhoneManager.class){
            for(TerminalPhone phone:clientPhoneLists.values()){
                count += phone.GetCallCount();
            }
        }
        return count;
    }

    public void SendUserMessage(int type,Object data){
        if(userMsgReceiver!=null)
            userMsgReceiver.AddMessage(type,data);
    }

    public TerminalPhone GetDevice(String id){
        TerminalPhone matchedDev;

        matchedDev = clientPhoneLists.get(id);

        return matchedDev;

    }

    public void PostTerminalPhoneMessage(CallPubMessage msg){
        phoneMsgRecevier.AddMessage(msg);
    }

    public String BuildCall(String caller,String callee,int callType){
        String callid = null;
        TerminalPhone matchedDev;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(caller);
            if(matchedDev!=null){
                if(matchedDev.isReg)
                    callid = matchedDev.MakeOutGoingCall(callee,callType);
            }

            if(callid==null){
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Build Call From %s to %s Fail, Could not Find DEV %s",caller,callee,caller);
            }
        }

        return callid;
    }

    public boolean SetConfig(String id, TerminalDeviceInfo info){
        TerminalPhone matchedDev;
        boolean result = false;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(id);
            if(matchedDev!=null){
                TerminalDeviceInfo devInfo = new TerminalDeviceInfo();
                devInfo.Copy(info);
                matchedDev.SetConfig(devInfo);
                result = true;
            }
        }
        return result;
    }

    public int EndCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.EndCall(callID);
            }else{
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"End Call DEV %s Call %s Fail, Could not Find DEV %s",devid,callID,devid);
            }
        }
        return result;
    }

    public int EndCall(String devid,int type){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.EndCall(type);
            }else{
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"End Call DEV %s Call type %d Fail, Could not Find DEV %s",devid,type,devid);
            }
        }
        return result;
    }
    

    public int AnswerCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;
        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.AnswerCall(callID);
            }else{
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Answer Call DEV %s Call %s Fail, Could not Find DEV %s",devid,callID,devid);
            }
        }
        return result;
    }

    public int StartVideoCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;
        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.StartVideo(callID);
            }else{
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Start Video in Call DEV %s Call %s Fail, Could not Find DEV %s",devid,callID,devid);
            }
        }
        return result;
    }

    public int AnswerVideoCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;
        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.AnswerVideo(callID);
            }else{
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Answer Video in Call DEV %s Call %s Fail, Could not Find DEV %s",devid,callID,devid);
            }
        }
        return result;
    }

    public int StopVideoCall(String devid,String callID){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone matchedDev;
        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(devid);
            if(matchedDev!=null){
                result = matchedDev.StopVideo(callID);
            }else{
                LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_ERROR,"Stop Video in Call DEV %s Call %s Fail, Could not Find DEV %s",devid,callID,devid);
            }
        }
        return result;
    }

    public int QueryDevs(String devid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone phone;
        synchronized (TerminalPhoneManager.class){
            phone = clientPhoneLists.get(devid);
            if(phone!=null){
                result = phone.QueryDevs();
            }
        }
        return result;
    }

    public int RequireCallTransfer(String devid,String areaId,boolean state){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone phone;
        synchronized (TerminalPhoneManager.class){
            phone = clientPhoneLists.get(devid);
            if(phone!=null){
                result = phone.CallTransfer(areaId,state);
            }
        }
        return result;
    }

    public long GetRunSecond(){
        return runSecond;
    }

    public int RequireBedListen(String devid,boolean state){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone phone;
        synchronized (TerminalPhoneManager.class){
            phone = clientPhoneLists.get(devid);
            if(phone!=null){
                if(phone.type== PhoneDevice.BED_CALL_DEVICE)
                    result = phone.ListenCall(state);
                else
                    result = ProtocolPacket.STATUS_NOTSUPPORT;
            }
        }
        return result;
    }

    public int QueryConfig(String devid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone phone;
        synchronized (TerminalPhoneManager.class){
            phone = clientPhoneLists.get(devid);
            if(phone!=null){
                result = phone.QueryConfig();
            }
        }
        return result;
    }

    public int QuerySystemConfig(String devid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        TerminalPhone phone;
        synchronized (TerminalPhoneManager.class){
            phone = clientPhoneLists.get(devid);
            if(phone!=null){
                result = phone.QuerySystemConfig();
            }
        }
        return result;
    }

    private void PacketRecvProcess(ProtocolPacket packet) {

        TerminalPhone phone = GetDevice(packet.receiver);
        if(phone!=null){
            switch(packet.type){
                case ProtocolPacket.CALL_REQ:
                    InviteReqPack inviteReqPack = (InviteReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Invite Req, call id is %s",inviteReqPack.receiver,inviteReqPack.callID);
                    phone.RecvIncomingCall(inviteReqPack);
                    break;
                case ProtocolPacket.ANSWER_REQ:
                    AnswerReqPack answerReqPack = (AnswerReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Answer Req, call id is %s, answerer is %s",answerReqPack.receiver,answerReqPack.callID,answerReqPack.answerer);
                    phone.RecvAnswerCall(answerReqPack);
                    break;
                case ProtocolPacket.END_REQ:
                    EndReqPack endReqPack = (EndReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv End Req, call id is %s",endReqPack.receiver,endReqPack.callID);
                    phone.RecvEndCall(endReqPack);
                    break;
                case ProtocolPacket.CALL_CANCEL_REQ:
                    CancelReqPack cancelReqPack = (CancelReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Cancel Req, call id is %s",cancelReqPack.receiver,cancelReqPack.callID);
                    phone.RecvCancelCall(cancelReqPack);
                    break;
                case ProtocolPacket.CALL_RES:
                    InviteResPack inviteResP = (InviteResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Invite Res, call id is %s",inviteResP.receiver,inviteResP.callID);
                    phone.UpdateCallStatus(inviteResP);
                    break;
                case ProtocolPacket.REG_RES:
                    RegResPack resP = (RegResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Reg Res",resP.receiver);
                    phone.UpdateRegStatus(resP.status,resP);
                    break;
                case ProtocolPacket.END_RES:
                    EndResPack endResPack = (EndResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv End Res, call id is %s",endResPack.receiver,endResPack.callId);
                    phone.UpdateCallStatus(endResPack);
                    break;
                case ProtocolPacket.CALL_CANCEL_RES:
                    CancelResPack cancelResPack = (CancelResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Cancel Res, call id is %s",cancelResPack.receiver,cancelResPack.callId);
                    phone.UpdateCallStatus(cancelResPack);
                    break;
                case ProtocolPacket.ANSWER_RES:
                    AnswerResPack answerResPack = (AnswerResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Answer Res, call id is %s",answerResPack.receiver,answerResPack.callID);
                    phone.UpdateCallStatus(answerResPack);
                    break;
                case ProtocolPacket.CALL_UPDATE_RES:
                    UpdateResPack updateResP = (UpdateResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Update Res,Status is %s, call id is %s",updateResP.receiver,ProtocolPacket.GetResString(updateResP.status),updateResP.callid);
                    phone.UpdateCallStatus(updateResP);
                    break;
                case ProtocolPacket.DEV_QUERY_RES:
                    DevQueryResPack devResP = (DevQueryResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv DevQuery Res",devResP.receiver);
                    phone.UpdateDevLists(devResP);
                    break;
                case ProtocolPacket.DEV_CONFIG_RES:
                    ConfigResPack configResP = (ConfigResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv ConfigQuery Res",configResP.receiver);
                    phone.UpdateConfig(configResP);
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_RES:
                    SystemConfigResPack systemConfigResP = (SystemConfigResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv System ConfigQuery Res",systemConfigResP.receiver);
                    phone.UpdateSystemConfig(systemConfigResP);
                    break;
                case ProtocolPacket.CALL_TRANSFER_RES:
                    TransferResPack transferResP = (TransferResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Call Transfer Res",transferResP.receiver);
                    phone.UpdateCallTransfer(transferResP);
                    break;
                case ProtocolPacket.CALL_LISTEN_RES:
                    ListenCallResPack listenResP = (ListenCallResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Call Listen Res",listenResP.receiver);
                    phone.UpdateListenCall(listenResP);
                    break;
                case ProtocolPacket.CALL_LISTEN_CLEAR_REQ:
                    ListenClearReqPack listenClearP = (ListenClearReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Call Listen Clear Req",listenClearP.receiver);
                    phone.UpdateListenCall(listenClearP);
                    break;
                case ProtocolPacket.CALL_TRANSFER_CHANGE_REQ:
                    TransferChangeReqPack transferChangeP = (TransferChangeReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Call Transfer Clear Req",transferChangeP.receiver);
                    phone.UpdateCallTransfer(transferChangeP);
                    break;
                case ProtocolPacket.CALL_VIDEO_INVITE_REQ:
                    StartVideoReqPack startVideoReqP = (StartVideoReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Video Start Req",startVideoReqP.receiver);
                    phone.RecvStartVideoReq(startVideoReqP);
                    break;
                case ProtocolPacket.CALL_VIDEO_INVITE_RES:
                    StartVideoResPack startVideoResP = (StartVideoResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Video Start Res",startVideoResP.receiver);
                    phone.RecvStartVideoRes(startVideoResP);
                    break;
                case ProtocolPacket.CALL_VIDEO_ANSWER_REQ:
                    AnswerVideoReqPack answerVideoReqP = (AnswerVideoReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Video Answer Req",answerVideoReqP.receiver);
                    phone.RecvAnswerVideoReq(answerVideoReqP);
                    break;
                case ProtocolPacket.CALL_VIDEO_ANSWER_RES:
                    AnswerVideoResPack answerVideoResP = (AnswerVideoResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Video Answer Res",answerVideoResP.receiver);
                    phone.RecvAnswerVideoRes(answerVideoResP);
                    break;
                case ProtocolPacket.CALL_VIDEO_END_REQ:
                    StopVideoReqPack stopVideoReqP = (StopVideoReqPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Video Stop Req",stopVideoReqP.receiver);
                    phone.RecvStopVideoReq(stopVideoReqP);
                    break;
                case ProtocolPacket.CALL_VIDEO_END_RES:
                    StopVideoResPack stopVideoResP = (StopVideoResPack)packet;
                    LogWork.Print(LogWork.TERMINAL_PHONE_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv Video Stop Res",stopVideoResP.receiver);
                    phone.RecvStopVideoRes(stopVideoResP);
                    break;
                default:
//                    phone.RecvUnsupport(packet);
                    break;
            }
        }

    }

    private void PacketTimeOverProcess(ProtocolPacket packet){
        TerminalPhone phone = GetDevice(packet.sender);
        if(phone!=null){
            switch(packet.type){
                case ProtocolPacket.REG_REQ:
                    phone.UpdateRegStatus(ProtocolPacket.STATUS_TIMEOVER,null);
                    break;
                case ProtocolPacket.CALL_REQ:
                case ProtocolPacket.ANSWER_REQ:
                case ProtocolPacket.END_REQ:
                case ProtocolPacket.CALL_UPDATE_REQ:
                    phone.UpdateCalTimeOver(packet);
                    break;
            }
        }

    }


    private byte[] MakeCallsSnap(String id){
        byte[] result = null;
        synchronized (TerminalPhoneManager.class) {
            for (TerminalPhone phone : clientPhoneLists.values()) {
                if(phone.id.compareToIgnoreCase(id)==0) {
                    result = phone.MakeCallSnap();
                    break;
                }
            }
        }
        return result;
    }

    private byte[] MakeAudioConfig(){
        JSONObject json = new JSONObject();
        String snap;
        try {
            json.put(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.AUDIO_CONFIG_READ_RES_CMD);
            json.put(SystemSnap.AUDIO_RTP_CODEC_NAME, PhoneParam.callRtpCodec);
            json.put(SystemSnap.AUDIO_RTP_DATARATE_NAME, PhoneParam.callRtpDataRate);
            json.put(SystemSnap.AUDIO_RTP_PTIME_NAME, PhoneParam.callRtpPTime);
            json.put(SystemSnap.AUDIO_AEC_DELAY_NAME, PhoneParam.aecDelay);
            json.put(SystemSnap.AUDIO_AEC_MODE_NAME, PhoneParam.aecMode);
            json.put(SystemSnap.AUDIO_NS_MODE_NAME, PhoneParam.nsMode);
            json.put(SystemSnap.AUDIO_NS_THRESHOLD_NAME, PhoneParam.nsThreshold);
            json.put(SystemSnap.AUDIO_NS_RANGE_NAME, PhoneParam.nsRange);
            json.put(SystemSnap.AUDIO_NS_TIME_NAME, PhoneParam.nsTime);
            json.put(SystemSnap.AUDIO_AGC_MODE_NAME, PhoneParam.agcMode);
            json.put(SystemSnap.AUDIO_INPUT_MODE_NAME, PhoneParam.inputMode);
            json.put(SystemSnap.AUDIO_INPUT_GAIN_NAME, PhoneParam.inputGain);
            json.put(SystemSnap.AUDIO_OUTPUT_MODE_NAME, PhoneParam.outputMode);
            json.put(SystemSnap.AUDIO_OUTPUT_GAIN_NAME, PhoneParam.outputGain);
            json.put(SystemSnap.AUDIO_MODE_NAME, PhoneParam.audioMode);
            json.put(SystemSnap.AUDIO_SPEAKER_NAME, PhoneParam.audioSpeaker);
            json.put(SystemSnap.AUDIO_AEC_DELAY_ESTIMATOR_NAME, AudioMgr.GetAudioDelay());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        snap = json.toString();
        json.clear();
        return snap.getBytes();
    }

    public DeviceStatistics GetRegStatist() {
        DeviceStatistics statics = new DeviceStatistics();
        synchronized (TerminalPhoneManager.class) {
            for(TerminalPhone phone:clientPhoneLists.values()) {
                if(phone.isReg)
                    statics.regSuccNum++;
                else
                    statics.regFailNum++;
            }
        }
        return statics;
    }

    public DeviceStatistics GetRegStatist(String areaId) {
        DeviceStatistics statics = new DeviceStatistics();
        synchronized (TerminalPhoneManager.class) {
            for(TerminalPhone phone:clientPhoneLists.values()) {
                if(phone.areaId.compareToIgnoreCase(areaId)==0) {
                    if (phone.isReg)
                        statics.regSuccNum++;
                    else
                        statics.regFailNum++;
                }
            }
        }
        return statics;
    }

    private byte[] MakeSystemInfo(String curAreaId){
        TerminalStatistics terminalStatist = UserInterface.GetTerminalStatistics();
        TerminalStatistics curAreaStatist = UserInterface.GetTerminalStatistics(curAreaId);
        JSONObject json = new JSONObject();
        byte[] result = null;

        json.put(SystemSnap.SNAP_CMD_TYPE_NAME,SystemSnap.SNAP_SYSTEM_INFO_RES);
        json.put(SystemSnap.SNAP_INFO_CALL_NUM_NAME,terminalStatist.callNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_TRANS_NUM_NAME,terminalStatist.transNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_REGSUCC_NUM_NAME,terminalStatist.regSuccDevNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_REGFAIL_NUM_NAME,terminalStatist.regFailDevNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_CURAREA_REGSUCC_NUM_NAME,curAreaStatist.regSuccDevNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_CURAREA_REGFAIL_NUM_NAME,curAreaStatist.regFailDevNum);

        result = json.toString().getBytes();
        json.clear();
        return result;
    }

}
