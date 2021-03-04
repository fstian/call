package com.example.nettytest.backend.backendphone;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.nettytest.backend.backendcall.BackEndCallConvergenceManager;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.pub.protocol.ConfigReqPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.DevQueryReqPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegReqPack;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.SystemConfigReqPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.PhoneParam;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BackEndPhoneManager {

    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;
    public static final int MSG_REQ_TIMEOVER = 3;

    private final HashMap<String, BackEndPhone> serverPhoneLists;
    Handler localMsgHandler=null;
    private ArrayList<ConfigItem> systemConfigList;

    private BackEndCallConvergenceManager backEndCallConvergencyMgr;

    public BackEndPhoneManager(){
        serverPhoneLists = new HashMap<>();
        systemConfigList = new ArrayList<>();
        backEndCallConvergencyMgr = new BackEndCallConvergenceManager();
        BackEndPhoneThread msgProcessThread = new BackEndPhoneThread();
        msgProcessThread.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message phonemsg = new Message();
                HandlerMgr.PostBackEndPhoneMsg(BackEndPhoneManager.MSG_SECOND_TICK,"");

                HandlerMgr.BackEndTransactionTick();

            }
        },0,1000);

        new Thread(() -> {
            try {
                byte[] recvBuf = new byte[1024];
                DatagramPacket recvPack;
                DatagramSocket testSocket;
                ArrayList<byte[]> resList;
                testSocket = new DatagramSocket(PhoneParam.snapStartPort+2);
                DatagramPacket resPack;
                while (!testSocket.isClosed()) {
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        testSocket.receive(recvPack);
                        if (recvPack.getLength() > 0) {
                            if(PhoneParam.serverActive) {
                                String recv = new String(recvBuf, "UTF-8");
                                JSONObject json = new JSONObject(recv);
                                int type = json.optInt(SystemSnap.SNAP_CMD_TYPE_NAME);
                                synchronized (BackEndPhoneManager.class) {
                                    if (type == SystemSnap.SNAP_BACKEND_CALL_REQ) {
                                        String devid = json.optString(SystemSnap.SNAP_DEVID_NAME);
                                        byte[] result;
                                        result = backEndCallConvergencyMgr.MakeCallConvergenceSnap(devid);
                                        if(result!=null) {
                                            resPack = new DatagramPacket(result, result.length, recvPack.getAddress(), recvPack.getPort());
                                            testSocket.send(resPack);
                                        }
                                    } else if (type == SystemSnap.SNAP_BACKEND_TRANS_REQ) {
                                        resList = HandlerMgr.GetBackEndTransInfo();
                                        for (byte[] data : resList) {
                                            resPack = new DatagramPacket(data, data.length, recvPack.getAddress(), recvPack.getPort());
                                            testSocket.send(resPack);
                                        }
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
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public BackEndPhone GetDevice(String id){
        BackEndPhone matchedPhone;

        matchedPhone = serverPhoneLists.get(id);

        return matchedPhone;

    }

    public void SetMessageHandler(Handler h){
        backEndCallConvergencyMgr.SetUserMessageHandler(h);
    }

    public int GetCallCount(){
        return backEndCallConvergencyMgr.GetCallCount();
    }

    public boolean CheckForwardEnable(BackEndPhone phone,int callType){
        return backEndCallConvergencyMgr.CheckForwardEnable(phone, callType);
    }

    public ArrayList<BackEndPhone> GetListenDevices(int callType){
        ArrayList<BackEndPhone> devices = new ArrayList<>();
            for(String devid:serverPhoneLists.keySet()){
                boolean isAdd = false;
                BackEndPhone phone = serverPhoneLists.get(devid);
                if(phone==null)
                    break;
                switch(callType){
                    case CommonCall.CALL_TYPE_BROADCAST:
                        switch(phone.type){
                            case BackEndPhone.BED_CALL_DEVICE:
                                if(BackEndConfig.broadCallToBed)
                                    isAdd = true;
                                break;
                            case BackEndPhone.DOOR_CALL_DEVICE:
                                if(BackEndConfig.broadCallToRoom)
                                    isAdd = true;
                                break;
                            case BackEndPhone.CORRIDOR_CALL_DEVICE:
                                if(BackEndConfig.broadCallToCorridor)
                                    isAdd = true;
                                break;
                            case BackEndPhone.NURSE_CALL_DEVICE:
                                isAdd = false;
                                break;
                            case BackEndPhone.TV_CALL_DEVICE:
                                if(BackEndConfig.broadCallToTv)
                                    isAdd = true;
                                break;
                        }
                        break;
                    case CommonCall.CALL_TYPE_EMERGENCY:
                        switch(phone.type){
                            case BackEndPhone.BED_CALL_DEVICE:
                                if(BackEndConfig.emerCallToBed)
                                    isAdd = true;
                                break;
                            case BackEndPhone.DOOR_CALL_DEVICE:
                                if(BackEndConfig.emerCallToRoom)
                                    isAdd = true;
                                break;
                            case BackEndPhone.CORRIDOR_CALL_DEVICE:
                                if(BackEndConfig.emerCallToCorridor)
                                    isAdd = true;
                                break;
                            case BackEndPhone.NURSE_CALL_DEVICE:
                                isAdd = true;
                                break;
                            case BackEndPhone.TV_CALL_DEVICE:
                                if(BackEndConfig.emerCallToTv)
                                    isAdd = true;
                                break;
                        }
                        break;
                    case CommonCall.CALL_TYPE_NORMAL:
                        switch(phone.type){
                            case BackEndPhone.BED_CALL_DEVICE:
                                if(BackEndConfig.normalCallToBed)
                                    isAdd = true;
                                break;
                            case BackEndPhone.DOOR_CALL_DEVICE:
                                if(BackEndConfig.normalCallToRoom)
                                    isAdd = true;
                                break;
                            case BackEndPhone.CORRIDOR_CALL_DEVICE:
                                if(BackEndConfig.normalCallToCorridor)
                                    isAdd = true;
                                break;
                            case BackEndPhone.NURSE_CALL_DEVICE:
                                isAdd = true;
                                break;
                            case BackEndPhone.TV_CALL_DEVICE:
                                if(BackEndConfig.normalCallToTv)
                                    isAdd = true;
                                break;
                        }
                        break;
                }
                if(isAdd){
                    devices.add(phone);
                }
            }
        return devices;
    }

    public void AddPhone(String id, int t){
        BackEndPhone matchedPhone;

        synchronized (BackEndPhoneManager.class) {
            matchedPhone = serverPhoneLists.get(id);
            if (matchedPhone == null) {
                matchedPhone = new BackEndPhone(id,t);
                serverPhoneLists.put(id,matchedPhone);
            }
        }

    }

    public void RemovePhone(String id){
        BackEndPhone matchedPhone;
        synchronized (BackEndPhoneManager.class){
            matchedPhone = serverPhoneLists.get(id);
            if(matchedPhone!=null)
                matchedPhone.paramList.clear();
                serverPhoneLists.remove(id);
        }
    }

    public void RemoveAllPhone(){
        synchronized (BackEndPhoneManager.class){
            for(Iterator<Map.Entry<String, BackEndPhone>> it = serverPhoneLists.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, BackEndPhone>item = it.next();
                BackEndPhone phone = item.getValue();
                phone.paramList.clear();
                it.remove();
            }

        }
    }

    public boolean SetSystemConfig(ArrayList<ConfigItem> list){
        systemConfigList = list;
        return true;
    }

    public boolean SetDeviceConfig(String id, ArrayList<ConfigItem> list){
        BackEndPhone matchedPhone;
        boolean result = false;
        synchronized (BackEndPhoneManager.class) {
            matchedPhone = serverPhoneLists.get(id);
            if (matchedPhone != null) {
                matchedPhone.SetDeviceConfig(list);
                result = true;
            }
        }
        return result;
    }

    public boolean SetDeviceInfo(String id, ServerDeviceInfo info){
        BackEndPhone matchedPhone;
        boolean result = false;
        synchronized (BackEndPhoneManager.class) {
            matchedPhone = serverPhoneLists.get(id);
            if (matchedPhone != null) {
                matchedPhone.SetDeviceInfo(info);
                result = true;
            }
        }
        return result;
    }

    private ArrayList<ConfigItem> GetDeviceConfig(String id){
        BackEndPhone matchedPhone;
        ArrayList<ConfigItem> paramList = new ArrayList<>();
        synchronized (BackEndPhoneManager.class) {
            matchedPhone = serverPhoneLists.get(id);
            if (matchedPhone != null)
                matchedPhone.GetDeviceConfig(paramList);
        }
        return paramList;
    }
    
    private ArrayList<ConfigItem> GetSystemConfig(){
        return systemConfigList;
    }

    public void PostBackEndPhoneMessage(int type,Object obj){
        
        if(localMsgHandler!=null){
            Message msg = localMsgHandler.obtainMessage();
            msg.arg1 = type;
            msg.obj = obj;
            localMsgHandler.sendMessage(msg);
        }
    }

    private void PacketRecvProcess(ProtocolPacket packet){
        String devID;
        Transaction trans;

        int resStatus;

        LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_DEBUG,"Server recv %s %s Packet",packet.sender,ProtocolPacket.GetTypeName(packet.type));

        devID = packet.sender;
        switch (packet.type) {
            case ProtocolPacket.REG_REQ:
                RegReqPack regPacket = (RegReqPack)packet;
                BackEndPhone phone;
                phone = HandlerMgr.GetBackEndPhone(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_FORBID;
                }else{
                    resStatus = ProtocolPacket.STATUS_OK;
                    phone.UpdateRegStatus(regPacket.expireTime);
                }
                RegResPack regResP = new RegResPack(resStatus,regPacket);

                trans = new Transaction(devID,packet,regResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID, trans);
                break;
            case ProtocolPacket.DEV_QUERY_REQ:
                DevQueryReqPack devReqP = (DevQueryReqPack)packet;
                DevQueryResPack devResP;
                phone = HandlerMgr.GetBackEndPhone(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_NOTFOUND;
                }else{
                    resStatus = ProtocolPacket.STATUS_OK;
                }
                devResP = new DevQueryResPack(resStatus,devReqP);
                if(resStatus==ProtocolPacket.STATUS_OK){
                    GetDeviceList(devResP.phoneList);
                }

                trans = new Transaction(devID,packet,devResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID,trans);
                break;
            case ProtocolPacket.DEV_CONFIG_REQ:
                ConfigReqPack configReqP = (ConfigReqPack)packet;
                ConfigResPack configResP;
                phone = HandlerMgr.GetBackEndPhone(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_NOTFOUND;
                }else{
                    resStatus = ProtocolPacket.STATUS_OK;
                }
                configResP = new ConfigResPack(resStatus,configReqP);
                if(resStatus==ProtocolPacket.STATUS_OK){
                    configResP.params = GetDeviceConfig(devID);
                }
                trans = new Transaction(devID,packet,configResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID,trans);
                break;
            case ProtocolPacket.SYSTEM_CONFIG_REQ:
                SystemConfigReqPack systemConfigReqP = (SystemConfigReqPack)packet;
                SystemConfigResPack systemConfigResP;
                resStatus = ProtocolPacket.STATUS_OK;
                systemConfigResP = new SystemConfigResPack(resStatus,systemConfigReqP);
                systemConfigResP.params = GetSystemConfig();
                trans = new Transaction(devID,packet,systemConfigResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID,trans);
                
            case ProtocolPacket.CALL_REQ:
            case ProtocolPacket.CALL_RES:
            case ProtocolPacket.END_REQ:
            case ProtocolPacket.END_RES:
            case ProtocolPacket.ANSWER_REQ:
            case ProtocolPacket.ANSWER_RES:
            case ProtocolPacket.CALL_UPDATE_REQ:
                CallConvergencyProcessPacket(packet);
                break;
        }
    }

    private void PacketTimeOverProcess(ProtocolPacket packet){
        CallConvergencyProcessTimeOver(packet);
    }

    private class BackEndPhoneThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            localMsgHandler = new Handler(msg -> {
                int type = msg.arg1;
                ProtocolPacket packet;
                synchronized (BackEndPhoneManager.class) {
                    switch (type) {
                        case MSG_NEW_PACKET:
                            packet = (ProtocolPacket) msg.obj;
                            PacketRecvProcess(packet);
                            break;
                        case MSG_SECOND_TICK:
                            CallConvergencySecondTick();
                            UpdatePhonesRegTick();
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

    private void UpdatePhonesRegTick(){
        for(BackEndPhone phone:serverPhoneLists.values()){
            phone.IncreaseRegTick();
        }
    }

    private void CallConvergencyProcessPacket(ProtocolPacket packet){
        backEndCallConvergencyMgr.ProcessPacket(packet);
    }

    private void CallConvergencySecondTick(){
        backEndCallConvergencyMgr.ProcessSecondTick();
    }

    
    private void CallConvergencyProcessTimeOver(ProtocolPacket packet){
        switch(packet.type){
            case ProtocolPacket.CALL_REQ:
            case ProtocolPacket.ANSWER_REQ:
            case ProtocolPacket.END_REQ:
                backEndCallConvergencyMgr.ProcessTimeOver(packet);
                break;
        }
    }

    public void GetDeviceList(ArrayList<PhoneDevice> list){
        PhoneDevice dev;
        for(BackEndPhone phone:serverPhoneLists.values()){
            dev = new PhoneDevice();
            dev.id = phone.id;
            dev.bedName = phone.devInfo.bedName;
            dev.type = phone.type;
            dev.isReg = phone.isReg;
            list.add(dev);
        }
    }
}
