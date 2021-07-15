package com.example.nettytest.terminal.terminalphone;

import com.alibaba.fastjson.*;
import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.DeviceStatistics;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.JsonPort;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.TerminalStatistics;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.AnswerVideoReqPack;
import com.example.nettytest.pub.protocol.AnswerVideoResPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ListenCallResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.StartVideoReqPack;
import com.example.nettytest.pub.protocol.StartVideoResPack;
import com.example.nettytest.pub.protocol.StopVideoReqPack;
import com.example.nettytest.pub.protocol.StopVideoResPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.protocol.TransferResPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;


import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalPhoneManager {
    HashMap<String, TerminalPhone> clientPhoneLists;
    
    ArrayList<CallPubMessage> userMsgQ = null;
    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;
    public static final int MSG_REQ_TIMEOVER = 3;
    static Thread snapThread = null;
    long runSecond = 0;

    private ArrayList<CallPubMessage> pubMsgList;

    public TerminalPhoneManager(){
        clientPhoneLists = new HashMap<>();
        pubMsgList = new ArrayList<>();

        HandlerMgr.ReadSystemType();

        TerminalPhoneThread terminalPhoneThread = new TerminalPhoneThread();
        terminalPhoneThread.start();
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
//                                                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Get Terminal Call Snap for dev %s, total %d bytes, send to %s:%d",devId,snapResult.length,recvPack.getAddress().getHostName(),recvPack.getPort());
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
                                            int value;

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
                                        } else if (type == SystemSnap.AUDIO_CONFIG_REQ_CMD) {
                                            PhoneParam.callRtpCodec = json.getIntValue(SystemSnap.AUDIO_RTP_CODEC_NAME);
                                            PhoneParam.callRtpDataRate = json.getIntValue(SystemSnap.AUDIO_RTP_DATARATE_NAME);
                                            PhoneParam.callRtpPTime = json.getIntValue(SystemSnap.AUDIO_RTP_PTIME_NAME);
                                            PhoneParam.aecDelay = json.getIntValue(SystemSnap.AUDIO_RTP_AEC_DELAY_NAME);
                                        } else if (type == SystemSnap.SNAP_DEV_REQ) {
                                            int sendCount = 0;
                                            for (TerminalPhone dev : clientPhoneLists.values()) {
                                                JSONObject resJson = new JSONObject();
                                                resJson.put(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_DEV_RES);
                                                resJson.put(SystemSnap.SNAP_AREAID_NAME,dev.areaId);
                                                resJson.put(SystemSnap.SNAP_DEVID_NAME, dev.id);
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
                                            String logFileName;
                                            int logIndex = 1;
                                            File logFile;
                                            while (true) {
                                                logFileName = String.format("/storage/self/primary/CallModuleLog%04d.txt", logIndex);
                                                logFile = new File(logFileName);
                                                if (logFile.exists() && logFile.isFile()) {
                                                    logFile.delete();
                                                } else {
                                                    break;
                                                }
                                                logIndex++;
                                                if (logIndex > 1000)
                                                    break;
                                            }
                                        }else if(type==SystemSnap.SNAP_SYSTEM_INFO_REQ){
                                            byte[] systemInfo;
                                            systemInfo = MakeSystemInfo();
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

    public void SetMessageHandler(ArrayList<CallPubMessage> msgQ){
        if(userMsgQ==null)
            userMsgQ = msgQ;
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
        if (userMsgQ != null) {
            synchronized(userMsgQ) {
                CallPubMessage msg = new CallPubMessage();
                msg.arg1 = type;
                msg.obj = data;
                userMsgQ.add(msg);
                userMsgQ.notify();
            }
        }
    }

    public TerminalPhone GetDevice(String id){
        TerminalPhone matchedDev;

        matchedDev = clientPhoneLists.get(id);

        return matchedDev;

    }

    public void PostTerminalPhoneMessage(CallPubMessage msg){
        if (pubMsgList != null) {            
            synchronized(pubMsgList) {
                pubMsgList.add(msg);
                pubMsgList.notify();                
            }
        }
    }

    public String BuildCall(String caller,String callee,int callType){
        String callid = null;
        TerminalPhone matchedDev;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(caller);
            if(matchedDev!=null){
                callid = matchedDev.MakeOutGoingCall(callee,callType);
            }else{
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

    private class TerminalPhoneThread extends Thread{
        public TerminalPhoneThread(){
            super("TerminalPhoneThread");
        }
        
        @Override
        public void run() {
            ArrayList<CallPubMessage> recvList = new ArrayList<>();
            CallPubMessage msg = new CallPubMessage();
            
            while(!isInterrupted()) {                
                synchronized(pubMsgList) {
                    try {
                        pubMsgList.wait();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    while(pubMsgList.size()>0) {
                        msg = pubMsgList.remove(0);
                        recvList.add(msg);
                    }
                }
                
                while(recvList.size()>0) {
                    msg = recvList.remove(0);
                    int type = msg.arg1;
                    ProtocolPacket packet;
                    synchronized (TerminalPhoneManager.class) {
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

    private byte[] MakeSystemInfo(){
        TerminalStatistics terminalStatist = UserInterface.GetTerminalStatistics();
        JSONObject json = new JSONObject();
        byte[] result = null;

        json.put(SystemSnap.SNAP_CMD_TYPE_NAME,SystemSnap.SNAP_SYSTEM_INFO_RES);
        json.put(SystemSnap.SNAP_INFO_CALL_NUM_NAME,terminalStatist.callNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_TRANS_NUM_NAME,terminalStatist.transNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_REGSUCC_NUM_NAME,terminalStatist.regSuccDevNum);
        json.put(SystemSnap.SNAP_INFO_CLIENT_REGFAIL_NUM_NAME,terminalStatist.regFailDevNum);

        result = json.toString().getBytes();
        json.clear();
        return result;
    }

}
