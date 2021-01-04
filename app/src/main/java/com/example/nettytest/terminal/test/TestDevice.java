package com.example.nettytest.terminal.test;

import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.OperationResult;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserRegMessage;

import java.util.ArrayList;

public class TestDevice {

    public int type;
    public String id;

    public boolean isRegOk;
    public boolean isCallOut;
    public boolean isTalking;
    public LocalCallInfo outGoingCall;
    final int INCOMING_LINE_HEIGHT = 60;
    public ArrayList<UserDevice> devLists;

    public ArrayList<LocalCallInfo> inComingCallInfos;

    public TestDevice(int type,String id){
        this.type = type;
        this.id = id;
        UserInterface.BuildDevice(type,id);
        inComingCallInfos = new ArrayList<>();
        outGoingCall = new LocalCallInfo();
        isCallOut = false;
        isRegOk = false;
        isTalking = false;
        devLists = null;
    }

    public OperationResult BuildCall(String peerId, int type){
        OperationResult result;
        result = UserInterface.BuildCall(id,peerId,type);
        if(result.result == OperationResult.OP_RESULT_OK){
            isCallOut = true;
            outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING;
            outGoingCall.caller = id;
            outGoingCall.callee = peerId;
            outGoingCall.callID = result.callID;
        }
        return result;
    }

    public OperationResult AnswerCall(String callid){
        OperationResult result;
        result = UserInterface.AnswerCall(id,callid);
        if(result.result == OperationResult.OP_RESULT_OK) {
            isCallOut = false;
            isTalking = true;
        }
        return result;
    }

    public void QueryDevs(){
        UserInterface.QueryDevs(id);
    }

    public OperationResult EndCall(String callid){
        OperationResult result;
        result = UserInterface.EndCall(id,callid);
        isCallOut = false;
        isTalking = false;
        return result;
    }

    public boolean Operation(int tvIndex,int x,int y){
        boolean result = false;
        OperationResult opResult;
        if(!isRegOk)
            return false;
        if(tvIndex==0) {
            if (type == UserInterface.CALL_BED_DEVICE) {
                if (inComingCallInfos.size() == 0) {
                    if (!isCallOut) {
                        opResult = BuildCall(PhoneParam.CALL_SERVER_ID,UserInterface.CALL_NORMAL_TYPE);
                        if(opResult.result != OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s Make Call Fail",id);
                        }else{
                            result = true;
                        }
                    } else {
                        opResult = EndCall(outGoingCall.callID);
                        if(opResult.result != OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s End Call %s Fail",id,outGoingCall.callID);
                        }else{
                            result = true;
                        }
                    }
                }
            }else if(type == UserInterface.CALL_NURSER_DEVICE){
                int selected = y / INCOMING_LINE_HEIGHT;
                if(selected==0){
                    if(isCallOut){
                        opResult = EndCall(outGoingCall.callID);
                        if(opResult.result!=OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s End Call %s Fail",id,outGoingCall.callID);
                        }else {
                            result = true;
                        }
                    }
                }else {
                    if (devLists == null) {
                        UserInterface.PrintLog("Device List TextView Touch at (%d,%d), But Device Is NULL", x, y);
                    } else {
                        if (selected < 1 || selected - 1 >= devLists.size()) {
                            UserInterface.PrintLog("Device List TextView Touch at (%d,%d), Select is Out of Range", x, y);
                        } else {
                            UserInterface.PrintLog("Device List TextView Touch at (%d,%d),Select %d device", x, y, selected - 1);
                            if (isCallOut) {
                                UserInterface.PrintLog("Device %s is Calling Out!!!!!!!!!!!!", id);
                            } else {
                                UserDevice device = devLists.get(selected - 1);
                                UserInterface.PrintLog("Device %s Calling %s", id, device.devid);
                                opResult = BuildCall(device.devid, UserInterface.CALL_NORMAL_TYPE);
                                if(opResult.result!=OperationResult.OP_RESULT_OK){
                                    UserInterface.PrintLog("DEV %s Makd Call to DEV %s Fail",id,device.devid);
                                }else{
                                    result = true;
                                }
                            }
                        }
                    }
                }
            }
        }else {
            if(type==UserInterface.CALL_NURSER_DEVICE||
                    type==UserInterface.CALL_DOOR_DEVICE||
                    type == UserInterface.CALL_BED_DEVICE) {
                int selected = y / INCOMING_LINE_HEIGHT;
                if (selected < inComingCallInfos.size()) {
                    if(!isCallOut) {
                        UserInterface.PrintLog("Call List TextView Touch at (%d,%d), Select %d Call", x, y, selected);
                        LocalCallInfo callInfo;
                        callInfo = inComingCallInfos.get(selected);
                        if (callInfo.status == LocalCallInfo.LOCAL_CALL_STATUS_INCOMING) {
                            if(!isTalking) {
                                opResult = AnswerCall(callInfo.callID);
                                if(opResult.result!=OperationResult.OP_RESULT_OK){
                                    UserInterface.PrintLog("DEV %s Answer Call  %s Fail",id,callInfo.callID);
                                }else{
                                    result = true;
                                }
                            }else
                                UserInterface.PrintLog("Dev %s is Taling , Could not Answer");
                        }else {
                            opResult = EndCall(callInfo.callID);
                            if(opResult.result!=OperationResult.OP_RESULT_OK){
                                UserInterface.PrintLog("DEV %s End Call  %s Fail",id,callInfo.callID);
                            }else{
                                result = true;
                            }
                        }
                    }else{
                        UserInterface.PrintLog("Dev %s is Outgoing Call, Could not Answer");
                    }
                }else{
                    UserInterface.PrintLog("Call List TextView Touch at (%d,%d), Select Out of Range",x,y);
                }
            }

        }
        return result;
    }

    public void UpdateDeviceList(UserDevsMessage msg){
        devLists = msg.deviceList;
    }

    public void UpdateRegisterInfo(UserRegMessage msg){
        switch (msg.type){
            case UserCallMessage.REGISTER_MESSAGE_SUCC:
                isRegOk = true;
                break;
            case UserCallMessage.REGISTER_MESSAGE_FAIL:
                isRegOk = false;
                break;
        }
    }

    public void UpdateCallInfo(UserCallMessage msg){
        switch(msg.type){
            case UserCallMessage.CALL_MESSAGE_RINGING:
                outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_RINGING;
                UserInterface.PrintLog("Dev %s Set Out Goning Call %s to Ringing",id,outGoingCall.callID);
                break;
            case UserCallMessage.CALL_MESSAGE_DISCONNECT:
                if(msg.callId.compareToIgnoreCase(outGoingCall.callID)==0) {
                    outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT;
                    UserInterface.PrintLog("Dev %s Set Out Going Call %s Disconnected",id,outGoingCall.callID);
                    isCallOut = false;
                }else{
                    for(LocalCallInfo info:inComingCallInfos){
                        if(info.callID.compareToIgnoreCase(msg.callId)==0){
                            inComingCallInfos.remove(info);
                            UserInterface.PrintLog("Dev %s Remove Incoming Call %s",id,info.callID);
                            break;
                        }
                    }
                }
                isTalking = false;
                break;
            case UserCallMessage.CALL_MESSAGE_ANSWERED:
                outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED;
                outGoingCall.answer = msg.operaterId;
                UserInterface.PrintLog("Dev %s Set Out Going Call %s Connected",id,outGoingCall.callID);
                isTalking = true;
                break;
            case UserCallMessage.CALL_MESSAGE_INCOMING:
                LocalCallInfo info = new LocalCallInfo();
                info.status = LocalCallInfo.LOCAL_CALL_STATUS_INCOMING;
                info.callID = msg.callId;
                info.callee = msg.calleeId;
                info.caller = msg.callerId;
                inComingCallInfos.add(info);
                UserInterface.PrintLog("Dev %s Recv Incoming Call %s",id,info.callID);
                break;
            case UserCallMessage.CALL_MESSAGE_CONNECT:
                for(LocalCallInfo info1:inComingCallInfos){
                    if(info1.callID.compareToIgnoreCase(msg.callId)==0){
                        info1.status = LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED;
                        UserInterface.PrintLog("Dev %s Set Incoming Call %s Connected",id,info1.callID);
                        break;
                    }
                }
                break;
        }
    }

    public String GetDeviceName(){

        return String.format("%s %s ",UserInterface.GetDeviceTypeName(type),id);
    }

}
