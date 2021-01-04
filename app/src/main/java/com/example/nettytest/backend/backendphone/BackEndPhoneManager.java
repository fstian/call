package com.example.nettytest.backend.backendphone;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.nettytest.backend.backendcall.BackEndCallConvergenceManager;
import com.example.nettytest.backend.backenddevice.BackEndDevManager;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.DevQueryReqPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegReqPack;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.transaction.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


public class BackEndPhoneManager {

    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;

    private final HashMap<String, BackEndPhone> serverPhoneLists;
    Handler msgHandler=null;

    private BackEndCallConvergenceManager backEndCallConvergencyMgr;

    public BackEndPhoneManager(){
        serverPhoneLists = new HashMap<>();
         backEndCallConvergencyMgr = new BackEndCallConvergenceManager();
        BackEndPhoneThread msgProcessThread = new BackEndPhoneThread();
        msgProcessThread.start();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Message phonemsg = new Message();
                phonemsg.arg1 = BackEndPhoneManager.MSG_SECOND_TICK;
                phonemsg.obj = "";
                HandlerMgr.PostBackEndPhoneMsg(phonemsg);

            }
        },0,1000);
    }

    public BackEndPhone GetDevice(String id){
        BackEndPhone matchedPhone;

        matchedPhone = serverPhoneLists.get(id);

        return matchedPhone;

    }

    public ArrayList<BackEndPhone> GetListenDevices(){
        ArrayList<BackEndPhone> devices = new ArrayList<>();
            for(String devid:serverPhoneLists.keySet()){
                BackEndPhone phone = serverPhoneLists.get(devid);
                if(phone==null)
                    break;
                if(phone.type==BackEndPhone.DOOR_CALL_DEVICE
                ||phone.type==BackEndPhone.CORRIDOR_CALL_DEVICE
                ||phone.type==BackEndPhone.NURSE_CALL_DEVICE
                ||phone.type==BackEndPhone.TV_CALL_DEVICE){
                    devices.add(phone);
                }
            }
        return devices;
    }

    public void AddPhone(String id, int t){
        BackEndPhone matchedPhone;

        synchronized (serverPhoneLists) {
            matchedPhone = serverPhoneLists.get(id);
            if (matchedPhone == null) {
                matchedPhone = new BackEndPhone(id,t);
                serverPhoneLists.put(id,matchedPhone);
            }
        }

    }

    public void PostBackEndPhoneMessage(Message msg){
        if(msgHandler!=null){
            msgHandler.sendMessage(msg);
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

    private class BackEndPhoneThread extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            msgHandler = new Handler(msg -> {
                int type = msg.arg1;
                synchronized (serverPhoneLists) {
                    switch (type) {
                        case MSG_NEW_PACKET:
                            ProtocolPacket packet = (ProtocolPacket) msg.obj;
                            PacketRecvProcess(packet);
                            break;
                        case MSG_SECOND_TICK:
                            CallConvergencySecondTick();
                            UpdatePhonesRegTick();
                            break;
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


    private void GetDeviceList(ArrayList<PhoneDevice> list){
        PhoneDevice dev;
        for(BackEndPhone phone:serverPhoneLists.values()){
            dev = new PhoneDevice();
            dev.id = phone.id;
            dev.type = phone.type;
            dev.isReg = phone.isReg;
            list.add(dev);
        }
    }
}
