package com.example.nettytest.backend.backendphone;

import com.example.nettytest.pub.CallParams;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.result.FailReason;

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
        BackEndPhone phone = phoneList.get(id);
        return phone;
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
}


