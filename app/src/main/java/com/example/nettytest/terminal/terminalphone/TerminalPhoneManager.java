package com.example.nettytest.terminal.terminalphone;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.protocol.AnswerReqPack;
import com.example.nettytest.pub.protocol.AnswerResPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.EndReqPack;
import com.example.nettytest.pub.protocol.EndResPack;
import com.example.nettytest.pub.protocol.InviteReqPack;
import com.example.nettytest.pub.protocol.InviteResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.protocol.UpdateResPack;
import com.example.nettytest.terminal.test.TestDevice;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class TerminalPhoneManager {
    HashMap<String, TerminalPhone> clientPhoneLists;
    Handler phoneManagerMsgHandler =null;
    Handler userMsgHandler = null;
    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;
    public static final int MSG_REQ_TIMEOVER = 3;
    static Thread snapThread = null;


    public TerminalPhoneManager(){
        clientPhoneLists = new HashMap<>();
        TerminalPhoneThread terminalPhoneThread = new TerminalPhoneThread();
        terminalPhoneThread.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message phonemsg = new Message();
                phonemsg.arg1 = TerminalPhoneManager.MSG_SECOND_TICK;
                phonemsg.obj = new UserMessage();
                HandlerMgr.PostTerminalPhoneMsg(phonemsg);

                HandlerMgr.SendMessageToUser(UserMessage.MESSAGE_TEST_TICK,new UserMessage());

                HandlerMgr.TerminalPhoneTransactionTick();
            }
        },0,1000);

    }

    private DatagramSocket OpenSnapSocket(int group){
        DatagramSocket socket=null;
        int iTmp;
        int port =PhoneParam.snapStartPort;
        switch(group){
            case PhoneParam.SNAP_MMI_GROUP:
                port = PhoneParam.snapStartPort;
                break;
            case PhoneParam.SNAP_TERMINAL_GROUP:
                port = PhoneParam.snapStartPort+PhoneParam.SNAP_PORT_INTERVAL;
                break;
            case PhoneParam.SNAP_BACKEND_GROUP:
                port = PhoneParam.snapStartPort+2*PhoneParam.SNAP_PORT_INTERVAL;
                break;
        }
        for(iTmp = 0;iTmp<=PhoneParam.SNAP_PORT_INTERVAL;iTmp++){
            socket = null;
            try {
                socket = new DatagramSocket(port+iTmp);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            if(socket!=null)
                break;
        }
        return socket;
    }

    public void AddDevice(TerminalPhone phone){
        TerminalPhone matchedDev;

        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(phone.id);
            if(matchedDev==null)
                clientPhoneLists.put(phone.id,phone);

            if(snapThread==null){
                snapThread = new Thread(() -> {
                    byte[] recvBuf = new byte[1024];
                    DatagramPacket recvPack;
                    DatagramSocket testSocket;
                    byte[] snapResult;
                    testSocket = OpenSnapSocket(PhoneParam.SNAP_TERMINAL_GROUP);
                    DatagramPacket resPack;
                    if(testSocket!=null){
                        while (!testSocket.isClosed()) {
                            recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                            try {
                                testSocket.receive(recvPack);
                                if (recvPack.getLength() > 0) {
                                    String recv = new String(recvBuf, "UTF-8");
                                    JSONObject json = new JSONObject(recv);
                                    int type = json.optInt(SystemSnap.SNAP_CMD_TYPE_NAME);
                                    synchronized (TerminalPhoneManager.class) {
                                        if (type == SystemSnap.SNAP_TERMINAL_CALL_REQ) {
                                            String devId = json.optString(SystemSnap.SNAP_DEVID_NAME);
                                            snapResult = MakeCallsSnap(devId);
                                            if(snapResult!=null) {
//                                                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Get Terminal Call Snap for dev %s, total %d bytes, send to %s:%d",devId,snapResult.length,recvPack.getAddress().getHostName(),recvPack.getPort());
                                                resPack = new DatagramPacket(snapResult, snapResult.length, recvPack.getAddress(), recvPack.getPort());
                                                testSocket.send(resPack);
                                            }
                                        } else if (type == SystemSnap.SNAP_TERMINAL_TRANS_REQ) {
                                            ArrayList<byte[]>resList;
                                            resList = HandlerMgr.GetTerminalTransInfo();
                                            for (byte[] data : resList) {
                                                resPack = new DatagramPacket(data, data.length, recvPack.getAddress(), recvPack.getPort());
                                                testSocket.send(resPack);
                                            }
                                        }else if(type==SystemSnap.LOG_CONFIG_REQ_CMD){
                                            int value;
                                            LogWork.backEndNetModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_NET_NAME) == 1;

                                            LogWork.backEndDeviceModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_DEVICE_NAME) == 1;

                                            LogWork.backEndCallModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_CALL_NAME) == 1;

                                            LogWork.backEndPhoneModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_PHONE_NAME) == 1;

                                            LogWork.terminalNetModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_NET_NAME) == 1;

                                            LogWork.terminalDeviceModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_DEVICE_NAME) == 1;

                                            LogWork.terminalCallModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_CALL_NAME) == 1;

                                            LogWork.terminalPhoneModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_PHONE_NAME) == 1;

                                            LogWork.terminalUserModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_USER_NAME) == 1;

                                            LogWork.terminalAudioModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_AUDIO_NAME) ==1;

                                            LogWork.transactionModuleLogEnable = json.optInt(SystemSnap.LOG_TRANSACTION_NAME) == 1;

                                            LogWork.debugModuleLogEnable = json.optInt(SystemSnap.LOG_DEBUG_NAME) == 1;

                                            LogWork.bLogToFiles = json.optInt(SystemSnap.LOG_WIRTE_FILES_NAME)==1;

                                            value = json.optInt(SystemSnap.LOG_FILE_INTERVAL_NAME);
                                            if(value<=0)
                                                value = 1;
                                            LogWork.logInterval = value;

                                            LogWork.dbgLevel = json.optInt(SystemSnap.LOG_DBG_LEVEL_NAME);
                                        }else if(type==SystemSnap.AUDIO_CONFIG_REQ_CMD){
                                            PhoneParam.callRtpCodec = json.optInt(SystemSnap.AUDIO_RTP_CODEC_NAME);
                                            PhoneParam.callRtpDataRate = json.optInt(SystemSnap.AUDIO_RTP_DATARATE_NAME);
                                            PhoneParam.callRtpPTime = json.optInt(SystemSnap.AUDIO_RTP_PTIME_NAME);
                                            PhoneParam.aecDelay = json.optInt(SystemSnap.AUDIO_RTP_AEC_DELAY_NAME);
                                        }else if(type==SystemSnap.SNAP_DEV_REQ){
                                            for (TerminalPhone dev : clientPhoneLists.values()) {
                                                JSONObject resJson = new JSONObject();
                                                resJson.putOpt(SystemSnap.SNAP_CMD_TYPE_NAME,SystemSnap.SNAP_DEV_RES);
                                                resJson.putOpt(SystemSnap.SNAP_DEVID_NAME,dev.id);
                                                resJson.putOpt(SystemSnap.SNAP_DEVTYPE_NAME,dev.type);
                                                byte[] resBuf = resJson.toString().getBytes();
                                                resPack= new DatagramPacket(resBuf,resBuf.length,recvPack.getAddress(),recvPack.getPort());
                                                testSocket.send(resPack);
                                            }
                                        }else if(type==SystemSnap.SNAP_DEL_LOG_REQ){
                                            String logFileName;
                                            int logIndex = 1;
                                            File logFile;
                                            while(true){
                                                logFileName = String.format("/storage/self/primary/CallModuleLog%04d.txt",logIndex);
                                                logFile = new File(logFileName);
                                                if(logFile.exists()&&logFile.isFile()){
                                                    logFile.delete();
                                                }else{
                                                    break;
                                                }
                                                logIndex++;
                                                if(logIndex>1000)
                                                    break;
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                snapThread.start();
            }
        }

    }

    public void RemovePhone(String id){
        TerminalPhone matchedDev;
        synchronized (TerminalPhoneManager.class) {
            matchedDev = clientPhoneLists.get(id);
            if(matchedDev!=null){
                matchedDev.UpdateRegStatus(ProtocolPacket.STATUS_NOTFOUND);
                clientPhoneLists.remove(id);
            }
        }
    }

    public void SetMessageHandler(Handler h){
        userMsgHandler = h;
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
        if (userMsgHandler != null) {
            Message msg = userMsgHandler.obtainMessage();
            msg.arg1 = type;
            msg.obj = data;
            userMsgHandler.sendMessage(msg);
        }
    }

    public TerminalPhone GetDevice(String id){
        TerminalPhone matchedDev;

        matchedDev = clientPhoneLists.get(id);

        return matchedDev;

    }

    public void PostTerminalPhoneMessage(Message msg){
        if (phoneManagerMsgHandler != null) {
            phoneManagerMsgHandler.sendMessage(msg);
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
                    phone.UpdateRegStatus(resP.status);
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
                    phone.UpdateRegStatus(ProtocolPacket.STATUS_TIMEOVER);
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
        @Override
        public void run() {
            Looper.prepare();
            phoneManagerMsgHandler = new Handler(msg -> {
                ProtocolPacket packet;
                int type = msg.arg1;
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
                return false;
            });
            Looper.loop();
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


}
