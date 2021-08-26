package com.example.nettytest.backend.backendphone;

import com.example.nettytest.pub.CallParams;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.ListenCallReqPack;
import com.example.nettytest.pub.protocol.ListenClearReqPack;
import com.example.nettytest.pub.protocol.TransferChangeReqPack;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.userinterface.PhoneParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BackEndZone {
    public String areaId;
    String areaName;
    CallParams params;
    public String transferAreaId;
    public static String DEFAULT_AREA_ID = "Default Area";

    HashMap<String,BackEndPhone> phoneList;

    public BackEndZone(String id,String name){
        areaId = id;
        areaName = name;
        transferAreaId = "";
        phoneList = new HashMap<>();
        params = new CallParams();
    }

    public BackEndPhone GetDevice(String id){
        return phoneList.get(id);
    }

    public ArrayList<PhoneDevice> GetDeviceList(){
        PhoneDevice dev;
        ArrayList<PhoneDevice> lists = new ArrayList<>();
        for(BackEndPhone phone:phoneList.values()){
            dev= new PhoneDevice();
            dev.type = phone.type;
            dev.id = phone.id;
            dev.isReg = phone.isReg;
            dev.bedName = phone.devInfo.bedName;
            lists.add(dev);
        }
        return lists;
    }

    public int RemoveAllDevices(){
        for(Iterator<Map.Entry<String, BackEndPhone>> it = phoneList.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, BackEndPhone>item = it.next();
            BackEndPhone phone = item.getValue();
            phone.paramList.clear();
            it.remove();
        }
        return FailReason.FAIL_REASON_NO;
    }

    public int RemoveDevice(String id){
        int result = FailReason.FAIL_REASON_NO;
        BackEndPhone phone = phoneList.remove(id);
        if(phone==null){
            result = FailReason.FAIL_REASON_NOTFOUND;
        }else{
            phone.paramList.clear();
        }
        return result;
    }

    public int NotifyTransferChangExcept(String devId){
        for(BackEndPhone phone:phoneList.values()){
            if(phone.type==PhoneDevice.NURSE_CALL_DEVICE){
                if(devId.compareToIgnoreCase(phone.id)!=0){
                    TransferChangeReqPack req = new TransferChangeReqPack(phone.id);
                    req.sender = PhoneParam.CALL_SERVER_ID;
                    req.receiver = phone.id;
                    if(!transferAreaId.isEmpty()){
                        req.transferAreaId = transferAreaId;
                        req.state = true;
                    }
                    req.msgID = UniqueIDManager.GetUniqueID(phone.id,UniqueIDManager.MSG_UNIQUE_ID);
                    Transaction trans = new Transaction(phone.id,req,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(req.msgID, trans);
                }
            }
        }
        return 0;
    }

    public String GetTransferAreaId(){
        return transferAreaId;
    }

    public String GetWorkAreaId(){
        return areaId;
    }

    public int IncreaseRegTick(){
        for(BackEndPhone phone:phoneList.values()) {
            phone.IncreaseRegTick();
        }
        return 0;
    }

    public int ClearAllListenExcept(String id){
        for(BackEndPhone phone:phoneList.values()) {
            if(phone.type == PhoneDevice.BED_CALL_DEVICE) {
                if (phone.enableListen&&id.compareToIgnoreCase(phone.id)!=0) {
                    ListenClearReqPack clearReqP = BuildListenClearPacket(phone.id);
                    Transaction clearTrans = new Transaction(phone.id,clearReqP,Transaction.TRANSCATION_DIRECTION_S2C);
                    HandlerMgr.AddBackEndTrans(clearReqP.msgID,clearTrans);
                    phone.enableListen = false;
                    LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_DEBUG,"BackEnd Send Cmd to Clear Listen Flag on Dev %s",phone.id);
                }
            }
        }
        return 0;
    }

    public void AddDevice(String id,BackEndPhone phone){
        phoneList.put(id, phone);
    }

    public void GetListenDevices(ArrayList<BackEndPhone> devices, int callType){
            for(String devid:phoneList.keySet()){
                boolean isAdd = false;
                BackEndPhone phone = phoneList.get(devid);
                if(phone==null)
                    break;
                switch(callType){
                    case CommonCall.CALL_TYPE_BROADCAST:
                        switch(phone.type){
                            case BackEndPhone.BED_CALL_DEVICE:
                                if(params.boardCallToBed)
                                    isAdd = true;
                                break;
                            case BackEndPhone.DOOR_CALL_DEVICE:
                                if(params.boardCallToRoom)
                                    isAdd = true;
                                break;
                            case BackEndPhone.CORRIDOR_CALL_DEVICE:
                                if(params.boardCallToCorridor)
                                    isAdd = true;
                                break;
                            case BackEndPhone.NURSE_CALL_DEVICE:
                                isAdd = false;
                                break;
                            case BackEndPhone.TV_CALL_DEVICE:
                                if(params.boardCallToTV)
                                    isAdd = true;
                                break;
                        }
                        break;
                    case CommonCall.CALL_TYPE_EMERGENCY:
                        switch(phone.type){
                            case BackEndPhone.BED_CALL_DEVICE:
                                if(params.emerCallToBed)
                                    isAdd = true;
                                else if(phone.enableListen)
                                    isAdd = true;
                                break;
                            case BackEndPhone.DOOR_CALL_DEVICE:
                                if(params.emerCallToRoom)
                                    isAdd = true;
                                break;
                            case BackEndPhone.CORRIDOR_CALL_DEVICE:
                                if(params.emerCallToCorridor)
                                    isAdd = true;
                                break;
                            case BackEndPhone.NURSE_CALL_DEVICE:
                                isAdd = true;
                                break;
                            case BackEndPhone.TV_CALL_DEVICE:
                                if(params.emerCallToTV)
                                    isAdd = true;
                                break;
                        }
                        break;
                    case CommonCall.CALL_TYPE_NORMAL:
                    case CommonCall.CALL_TYPE_ASSIST:
                        switch(phone.type){
                            case BackEndPhone.BED_CALL_DEVICE:
                                if(params.normalCallToBed)
                                    isAdd = true;
                                else if(phone.enableListen)
                                    isAdd = true;
                                break;
                            case BackEndPhone.DOOR_CALL_DEVICE:
                                if(params.normalCallToRoom)
                                    isAdd = true;
                                break;
                            case BackEndPhone.CORRIDOR_CALL_DEVICE:
                                if(params.normalCallToCorridor)
                                    isAdd = true;
                                break;
                            case BackEndPhone.NURSE_CALL_DEVICE:
                                isAdd = true;
                                break;
                            case BackEndPhone.TV_CALL_DEVICE:
                                if(params.normalCallToTV)
                                    isAdd = true;
                                break;
                        }
                        break;
                }
                if(isAdd){
                    devices.add(phone);
                }
            }
    }

    private ListenClearReqPack BuildListenClearPacket(String devid){
        ListenClearReqPack listenClearReqP = new ListenClearReqPack(devid);

        listenClearReqP.sender = PhoneParam.CALL_SERVER_ID;
        listenClearReqP.receiver = devid;
        listenClearReqP.msgID = UniqueIDManager.GetUniqueID(devid,UniqueIDManager.MSG_UNIQUE_ID);
        return listenClearReqP;
    }
}


