package com.example.nettytest.terminal.test;

import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.TestInfo;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.OperationResult;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserRegMessage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TestDevice {

    public int type;
    public String id;

    public boolean isRegOk;
    public boolean isCallOut;
    public boolean isTalking;
    public String talkPeer;
    public LocalCallInfo outGoingCall;
    public TestInfo testInfo;
    private int testTickCount;
    private int testWaitTick;
    final int INCOMING_LINE_HEIGHT = 35;

    private ArrayList<UserDevice> devLists;
    private ArrayList<LocalCallInfo> inComingCallInfos;

    public TestDevice(int type,String id){
        TerminalDeviceInfo info = new TerminalDeviceInfo();
        this.type = type;
        this.id = id;
        UserInterface.BuildDevice(type,id);
        info.patientName = "patient"+id;
        info.patientAge = 18+type;
        UserInterface.SetDevInfo(id,info);
        inComingCallInfos = new ArrayList<>();
        outGoingCall = new LocalCallInfo();
        isCallOut = false;
        isRegOk = false;
        isTalking = false;
        talkPeer = "";
        testInfo = new TestInfo();
        devLists = null;
        testWaitTick = (int)(Math.random()*testInfo.timeUnit)+1;
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

    public void SetTestInfo(TestInfo info){
        testInfo.isAutoTest = info.isAutoTest;
        testInfo.timeUnit = info.timeUnit;
        testInfo.isRealTimeFlash = info.isRealTimeFlash;
    }

    public byte[] MakeSnap(){
        JSONObject snap = new JSONObject();

        try {
            snap.putOpt(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_MMI_CALL_RES);
            snap.putOpt(SystemSnap.SNAP_DEVID_NAME, id);
            if(isRegOk) {
                snap.putOpt(SystemSnap.SNAP_REG_NAME, 1);
            }else{
                snap.putOpt(SystemSnap.SNAP_REG_NAME,0);
            }
            snap.putOpt(SystemSnap.SNAP_CALLSTATUS_NAME,outGoingCall.status);
            if(outGoingCall.status!=LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT) {
                if(outGoingCall.status==LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                    snap.putOpt(SystemSnap.SNAP_PEER_NAME,outGoingCall.answer);
                else
                    snap.putOpt(SystemSnap.SNAP_PEER_NAME, outGoingCall.callee);
                snap.putOpt(SystemSnap.SNAP_CALLID_NAME, outGoingCall.callID);
            }

            JSONArray callList = new JSONArray();
            synchronized (TestDevice.class) {
                for (int iTmp = 0; iTmp < inComingCallInfos.size(); iTmp++) {
                    JSONObject call = new JSONObject();
                    call.putOpt(SystemSnap.SNAP_CALLSTATUS_NAME, inComingCallInfos.get(iTmp).status);
                    if (inComingCallInfos.get(iTmp).status != LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT) {
                        call.putOpt(SystemSnap.SNAP_PEER_NAME, inComingCallInfos.get(iTmp).caller);
                        call.putOpt(SystemSnap.SNAP_CALLID_NAME, inComingCallInfos.get(iTmp).callID);
                    }
                    callList.put(call);
                }
            }
            snap.putOpt(SystemSnap.SNAP_INCOMINGS_NAME,callList);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return snap.toString().getBytes();
    }

    public OperationResult AnswerCall(String callid){
        OperationResult result;
        result = UserInterface.AnswerCall(id,callid);
        if(result.result == OperationResult.OP_RESULT_OK) {
            isCallOut = false;
            isTalking = true;
            talkPeer = GetIncomingCaller(callid);
        }
        return result;
    }

    private String GetIncomingCaller(String id){
        String peer = "";
        synchronized(TestDevice.class){
            for(LocalCallInfo info:inComingCallInfos){
                if(info.callID.compareToIgnoreCase(id)==0){
                    peer = info.caller;
                    break;
                }
            }
        }
        return peer;
    }

    public void QueryDevs(){
        UserInterface.QueryDevs(id);
    }

    public void QueryConfig() {UserInterface.QueryDevConfig(id);}

    private OperationResult EndCall(String callid){
        OperationResult result;
        result = UserInterface.EndCall(id,callid);
        if(isTalking) {
            if(isCallOut){
                if(outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                    if (outGoingCall.callID.compareToIgnoreCase(callid) == 0) {
                        isTalking = false;
                        talkPeer = "";
                    }
                }
            }else{
                for (LocalCallInfo callInfo : inComingCallInfos) {
                    if(callInfo.status==LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                        if(callInfo.callID.compareToIgnoreCase(callid)==0) {
                            isTalking = false;
                            talkPeer = "";
                            break;
                        }
                    }
                }
            }
        }

        if(isCallOut) {
            if(outGoingCall.callID.compareToIgnoreCase(callid)==0)
                isCallOut = false;
        }

        return result;
    }

    public boolean Operation(int tvIndex,int x,int y){
        boolean result = false;
        OperationResult opResult;
        if(!isRegOk)
            return false;
//        UserInterface.PrintLog("Device List TextView Touch at (%d,%d)", x, y);
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
                    }else{
                        opResult = BuildCall(PhoneParam.CALL_SERVER_ID, UserInterface.CALL_BROADCAST_TYPE);
                        if(opResult.result!=OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s Make BroadCast Call Fail",id);
                        }else{
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

    public boolean TestProcess(){
        boolean result = false;
        if(testInfo.isAutoTest&& isRegOk) {
            testTickCount++;
            if (testTickCount >= testWaitTick) {
                testTickCount = 0;
                testWaitTick = (int) (Math.random() * testInfo.timeUnit) + 1;
                result = true;
                synchronized (TestDevice.class) {
                    if (isCallOut) {
//                    EndCall(outGoingCall.callID);
                    } else {
                        if (inComingCallInfos.size() == 0) {
                            if (type == UserInterface.CALL_NURSER_DEVICE) {
                                String callDevId = GetRandomBedDeviceId();
                                if (callDevId != null)
                                    BuildCall(callDevId, UserInterface.CALL_NORMAL_TYPE);
                            } else if (type == UserInterface.CALL_BED_DEVICE) {
                                BuildCall(PhoneParam.CALL_SERVER_ID, UserInterface.CALL_NORMAL_TYPE);
                            }
                        } else {
                            int selectCall = (int) (Math.random() * inComingCallInfos.size());
                            if (selectCall >= inComingCallInfos.size())
                                selectCall = inComingCallInfos.size() - 1;
                            if (isTalking) {
                                EndCall(inComingCallInfos.get(selectCall).callID);
                            } else {
                                if (Math.random() > 0.5)
                                    AnswerCall(inComingCallInfos.get(selectCall).callID);
//                            else
//                                EndCall(inComingCallInfos.get(selectCall).callID);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    public void UpdateDeviceList(UserDevsMessage msg){
        if(devLists!=null)
            devLists.clear();
        devLists = msg.deviceList;
    }

    public void UpdateConfig(UserConfigMessage msg){
        msg.paramList.clear();
    }

    public void UpdateRegisterInfo(UserRegMessage msg){
        switch (msg.type){
            case UserCallMessage.REGISTER_MESSAGE_SUCC:
                isRegOk = true;
                QueryConfig();
                break;
            case UserCallMessage.REGISTER_MESSAGE_FAIL:
                isRegOk = false;
                break;
        }
    }

    public int UpdateCallInfo(UserCallMessage msg){
        int result = 0;
        synchronized (TestDevice.class) {
            switch (msg.type) {
                case UserCallMessage.CALL_MESSAGE_RINGING:
                    outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_RINGING;
                    UserInterface.PrintLog("Dev %s Set Out Goning Call %s to Ringing", id, outGoingCall.callID);
                    break;
                case UserCallMessage.CALL_MESSAGE_DISCONNECT:
                case UserCallMessage.CALL_MESSAGE_UPDATE_FAIL:
                case UserCallMessage.CALL_MESSAGE_INVITE_FAIL:
                case UserCallMessage.CALL_MESSAGE_ANSWER_FAIL:
                case UserCallMessage.CALL_MESSAGE_UNKNOWFAIL:
                    if (msg.callId.compareToIgnoreCase(outGoingCall.callID) == 0) {
                        if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                            isTalking = false;
                            talkPeer = "";
                        }
                        outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT;
                        UserInterface.PrintLog("Dev %s Set Out Going Call %s Disconnected", id, outGoingCall.callID);
                        isCallOut = false;
                    } else {
                        for (LocalCallInfo info : inComingCallInfos) {
                            if (info.callID.compareToIgnoreCase(msg.callId) == 0) {
                                if (info.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                                    isTalking = false;
                                    talkPeer = "";
                                }
                                inComingCallInfos.remove(info);
                                UserInterface.PrintLog("Dev %s Remove Incoming Call %s", id, info.callID);
                                break;
                            }
                        }
                    }
                    break;
                case UserCallMessage.CALL_MESSAGE_ANSWERED:
                    outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED;
                    outGoingCall.answer = msg.operaterId;
                    UserInterface.PrintLog("Dev %s Set Out Going Call %s Connected", id, outGoingCall.callID);
                    isTalking = true;
                    talkPeer = outGoingCall.answer;
                    break;
                case UserCallMessage.CALL_MESSAGE_INCOMING:
                    LocalCallInfo info = new LocalCallInfo();
                    info.status = LocalCallInfo.LOCAL_CALL_STATUS_INCOMING;
                    info.callID = msg.callId;
                    info.callee = msg.calleeId;
                    info.caller = msg.callerId;
                    inComingCallInfos.add(info);
                    UserInterface.PrintLog("Dev %s Recv Incoming Call %s from room-%s , bed-%s , patient-%s, age-%d", id, info.callID,msg.roomId,msg.bedName,msg.patientName,msg.patientAge);
                    break;
                case UserCallMessage.CALL_MESSAGE_CONNECT:
                    for (LocalCallInfo info1 : inComingCallInfos) {
                        if (info1.callID.compareToIgnoreCase(msg.callId) == 0) {
                            info1.status = LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED;
                            UserInterface.PrintLog("Dev %s Set Incoming Call %s Connected", id, info1.callID);
                            talkPeer = info1.caller;
                            break;
                        }
                    }
                    break;
            }

            int talkingCount = 0;
            for (LocalCallInfo info : inComingCallInfos) {
                if (info.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                    talkingCount++;
            }
            if (talkingCount > 1)
                result = -1;
        }
        return result;
    }

    private String GetDeviceName(){

        return String.format("%s",id);
    }

    private String GetRandomBedDeviceId(){
        int bedNum ;
        int iTmp;
        int curBedNum;
        if(devLists==null){
            return null;
        }

        bedNum =0;
        for(iTmp=0;iTmp<devLists.size();iTmp++){
            if(devLists.get(iTmp).type == UserInterface.CALL_BED_DEVICE&&devLists.get(iTmp).isReg)
                bedNum++;
        }
        if(bedNum==0)
            return null;

        int selectDev = (int)(Math.random()*bedNum);
        if(selectDev>bedNum)
            selectDev = bedNum-1;

        curBedNum = 0;
        for(iTmp=0;iTmp<devLists.size();iTmp++){
            if(devLists.get(iTmp).type == UserInterface.CALL_BED_DEVICE&&devLists.get(iTmp).isReg) {
                if(curBedNum==selectDev)
                    break;
                else
                    curBedNum++;
            }
        }

        if(iTmp<=devLists.size())
            return devLists.get(iTmp).devid;
        else
            return null;
    }
    
    public StringBuilder GetCallInfo(){
        StringBuilder status = new StringBuilder();
        for (LocalCallInfo callInfo : inComingCallInfos) {
            switch (callInfo.status) {
                case LocalCallInfo.LOCAL_CALL_STATUS_INCOMING:
                    status.append(String.format("From %s, Incoming\n", callInfo.caller));
                    break;
                case LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED:
                    status.append(String.format("%s Talking with %s\n", id,talkPeer));
                    break;
                default:
                    status.append(String.format("From %s, Unexcept\n", callInfo.caller));
                    break;

            }
        }
        return status;
    }

    public boolean CheckTestStatus(){
        int talkNum = 0;
        boolean isRight = true;
        synchronized (TestDevice.class) {
            if(outGoingCall.status==LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                talkNum++;

            for (LocalCallInfo callInfo : inComingCallInfos) {
                switch (callInfo.status) {
                    case LocalCallInfo.LOCAL_CALL_STATUS_INCOMING:
                        break;
                    case LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED:
                        talkNum++;
                        if (talkNum > 1) {
                            isRight = false;
                            break;
                        }
                        break;
                }
            }

            if(isRight) {
                for (int iTmp = 0; iTmp < inComingCallInfos.size(); iTmp++) {
                    LocalCallInfo cp1 = inComingCallInfos.get(iTmp);
                    for (int jTmp = iTmp+1; jTmp < inComingCallInfos.size(); jTmp++) {
                        LocalCallInfo cp2 = inComingCallInfos.get(jTmp);
                        if (cp1.callID.compareToIgnoreCase(cp2.callID) == 0) {
                            isRight = false;
                            break;
                        }
                        if (!isRight)
                            break;
                    }
                }
            }
        }
        return isRight;
    }

    public StringBuilder GetDeviceInfo(){
        StringBuilder status;
        if (isCallOut) {
            if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                status = new StringBuilder(String.format("%s Call to %s\n", GetDeviceName(), outGoingCall.callee));
            else if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                status = new StringBuilder(String.format("%s Call to %s, Ringing....\n", GetDeviceName(), outGoingCall.callee));
            else if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                status = new StringBuilder(String.format("%s Talking with %s\n", GetDeviceName(), talkPeer));
            else
                status = new StringBuilder(String.format("%s Call to %s, Unknow....\n", GetDeviceName(), outGoingCall.callee));
        } else {
            if (isRegOk)
                status = new StringBuilder(String.format("%s Register Suss\n", GetDeviceName()));
            else
                status = new StringBuilder(String.format("%s Register Fail\n", GetDeviceName()));
        }
        return status;
    }

    public StringBuilder GetNurserDeviceInfo(){
        StringBuilder status;

        status = GetDeviceInfo();

        if (devLists != null) {
            for (int iTmp = 0; iTmp < devLists.size(); iTmp++) {
                UserDevice bedPhone = devLists.get(iTmp);
                if (bedPhone.isReg) {
                    status.append(String.format("%s Register succ\n", bedPhone.devid));
                    UserInterface.PrintLog("%s-%s Register Succ",bedPhone.devid,bedPhone.bedName);
                } else {
                    status.append(String.format("%s Register Fail\n", bedPhone.devid));
                    UserInterface.PrintLog("%s-%s Register Fail",bedPhone.devid,bedPhone.bedName);
                }
            }
        }

        return status;
    }
}
