package com.example.nettytest.terminal.test;

import com.alibaba.fastjson.*;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.userinterface.ListenCallMessage;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.TestInfo;
import com.example.nettytest.userinterface.TransferMessage;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.pub.result.OperationResult;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;
import com.example.nettytest.userinterface.UserVideoMessage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class TestDevice extends UserDevice{
    public boolean isCallOut;
    public int callOutNum;
    public String talkPeer;
    public LocalCallInfo outGoingCall;
    public TestInfo testInfo;
    private int testTickCount;
    private int testWaitTick;

    public String transferAreaId;
    public boolean bedlistenCalls;

    public boolean isVideo;
    public String videoCallId;

    private ArrayList<UserDevice> devLists;
    private ArrayList<LocalCallInfo> inComingCallInfos;

    private HashMap<String, Integer> inComingCallRecord;

    public TestDevice(int type,String id){

        TerminalDeviceInfo info = new TerminalDeviceInfo();
        this.type = type;
        this.devid = id;
        UserInterface.BuildDevice(type,id,netMode);
        info.patientName = "patient"+id;
        info.patientAge = String.format("%d",18+type);
        UserInterface.SetDevInfo(id,info);
        inComingCallInfos = new ArrayList<>();
        outGoingCall = new LocalCallInfo();
        isCallOut = false;
        callOutNum = 0;
        isRegOk = false;
        talkPeer = "";
        testInfo = new TestInfo();
        devLists = null;
        transferAreaId = "";
        bedlistenCalls = false;
        testWaitTick = (int)(Math.random()*testInfo.timeUnit)+10;
        inComingCallRecord = new HashMap<>();
        isVideo = false;
        videoCallId = "";
    }

    public TestDevice(int type,String id,int netMode){
        this.type = type;
        this.devid = id;
        this.netMode = netMode;
        inComingCallInfos = new ArrayList<>();
        outGoingCall = new LocalCallInfo();
        isCallOut = false;
        isRegOk = false;
        talkPeer = "";
        testInfo = new TestInfo();
        devLists = null;
        transferAreaId = "";
        bedlistenCalls = false;
        testWaitTick = (int)(Math.random()*testInfo.timeUnit)+5;
        inComingCallRecord = new HashMap<>();
        isVideo = false;
        videoCallId = "";
    }

    public void StartDevice(){
        TerminalDeviceInfo info = new TerminalDeviceInfo();

        UserInterface.BuildDevice(type,devid,netMode);
        info.patientName = "patient"+devid;
        info.patientAge = String.format("%d",18+type);
        UserInterface.SetDevInfo(devid,info);
    }

    public OperationResult BuildCall(String peerId, int type){
        OperationResult result;
        result = UserInterface.BuildCall(devid,peerId,type);
        if(result.result == OperationResult.OP_RESULT_OK){
            callOutNum++;
            outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING;
            outGoingCall.caller = devid;
            outGoingCall.callee = peerId;
            outGoingCall.callID = result.callID;
            outGoingCall.callType = type;
            isCallOut = true;
            UserInterface.PrintLog("Build Outging Call %s by Dev %s",outGoingCall.callID,devid);
        }
        return result;
    }

    public void SetTestInfo(TestInfo info){
        testInfo.isAutoTest = info.isAutoTest;
        testInfo.timeUnit = info.timeUnit;
        testInfo.isRealTimeFlash = info.isRealTimeFlash;
        testInfo.testMode = info.testMode;
    }

    public byte[] MakeSnap(){
        JSONObject snap = new JSONObject();
        String snapRes;
        try {
            snap.put(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_MMI_CALL_RES);
            snap.put(SystemSnap.SNAP_DEVID_NAME, devid);
            if(isRegOk) {
                snap.put(SystemSnap.SNAP_REG_NAME, 1);
            }else{
                snap.put(SystemSnap.SNAP_REG_NAME,0);
            }
            snap.put(SystemSnap.SNAP_VER_NAME,PhoneParam.VER_STR);
            synchronized (TestDevice.class) {
                snap.put(SystemSnap.SNAP_CALLSTATUS_NAME,outGoingCall.status);
                if(outGoingCall.status!= LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT) {
                    if(outGoingCall.status== LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                        snap.put(SystemSnap.SNAP_PEER_NAME,outGoingCall.answer);
                    else
                        snap.put(SystemSnap.SNAP_PEER_NAME, outGoingCall.callee);
                    snap.put(SystemSnap.SNAP_CALLID_NAME, outGoingCall.callID);
                }

                JSONArray callList = new JSONArray();
                for (int iTmp = 0; iTmp < inComingCallInfos.size(); iTmp++) {
                    JSONObject call = new JSONObject();
                    call.put(SystemSnap.SNAP_CALLSTATUS_NAME, inComingCallInfos.get(iTmp).status);
                    if (inComingCallInfos.get(iTmp).status != LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT) {
                        call.put(SystemSnap.SNAP_PEER_NAME, inComingCallInfos.get(iTmp).caller);
                        call.put(SystemSnap.SNAP_CALLID_NAME, inComingCallInfos.get(iTmp).callID);
                    }
                    callList.add(call);
                }
                snap.put(SystemSnap.SNAP_INCOMINGS_NAME,callList);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        snapRes = snap.toString();
        snap.clear();
        
        return snapRes.getBytes();
    }

    void CleanCall(){
        synchronized(TestDevice.class){
            if(isCallOut){
                EndCall(outGoingCall.callID);
            }
            while(inComingCallInfos.size()>0){
                LocalCallInfo call = inComingCallInfos.get(0);
                EndCall(call.callID);
            }
        }
    }

    public OperationResult AnswerCall(String callid){
        OperationResult result;
        result = UserInterface.AnswerCall(devid,callid);
        if(result.result == OperationResult.OP_RESULT_OK) {
// in listen mode, phone will answer when call out
//            isCallOut = false;

// call status is not connected. answer maybe fail           
// but status is not connected, phone will maybe answer other call.
//            talkPeer = GetIncomingCaller(callid);
            UserInterface.PrintLog("Answer Incoming Call %s by Dev %s and Set talkPeer=%s",callid,devid,talkPeer);
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
        UserInterface.QueryDevs(devid);
    }

    public void QueryConfig() {UserInterface.QueryDevConfig(devid);}
    
    public void QuerySystemConfig() {
        UserInterface.QuerySystemConfig(devid);
    }

    private OperationResult EndCall(String callid){
        OperationResult result;
        result = UserInterface.EndCall(devid,callid);
//        UserInterface.EndCall("20105105",callid);
        
        if (outGoingCall.callID.compareToIgnoreCase(callid) == 0) {
            if(outGoingCall.status== LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED){
                talkPeer = "";
//                UserInterface.PrintLog("%s Clear talkPeer when End Outgoing Call %s ",devid,callid);
            }
            outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT;
            isCallOut = false;
            UserInterface.PrintLog("Stop Outgoing Call %s by Dev %s",outGoingCall.callID,devid);
        }else{
            for (LocalCallInfo callInfo : inComingCallInfos) {
                if(callInfo.callID.compareToIgnoreCase(callid)==0) {
                    inComingCallInfos.remove(callInfo);
                    if(callInfo.status== LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED){
                        talkPeer = "";
//                        UserInterface.PrintLog("%s Clear talkPeer when End Call %s in CallList",devid,callid);
                    }
                    UserInterface.PrintLog("Stop Incoming Call %s by Dev %s",callInfo.callID,devid);
                    break;
                }
            }
        }

        return result;
    }

    public boolean Operation(int tvIndex,int selected){
        boolean result = false;
        OperationResult opResult;
//        if(!isRegOk)
//            return false;
//        UserInterface.PrintLog("Device List TextView Touch at (%d,%d)", x, y);
        if(tvIndex==0) {
            if (type == UserInterface.CALL_BED_DEVICE||type==UserInterface.CALL_EMERGENCY_DEVICE) {
                if (inComingCallInfos.size() == 0) {
                    if (!isCallOut) {
                        if(type==UserInterface.CALL_EMERGENCY_DEVICE)
                            opResult = BuildCall(PhoneParam.CALL_SERVER_ID,UserInterface.CALL_EMERGENCY_TYPE);
                        else
                            opResult = BuildCall(PhoneParam.CALL_SERVER_ID,UserInterface.CALL_NORMAL_TYPE);
                        if(opResult.result != OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s Make Call Fail",devid);
                        }else{
                            result = true;
                        }
                    } else {
                        opResult = EndCall(outGoingCall.callID);
                        if(opResult.result != OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s End Call %s Fail",devid,outGoingCall.callID);
                        }else{
                            result = true;
                        }
                    }
                }
            }else if(type == UserInterface.CALL_NURSER_DEVICE){
                if(selected==0){
                    if(isCallOut){
                        opResult = EndCall(outGoingCall.callID);
                        if(opResult.result!=OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s End Call %s Fail",devid,outGoingCall.callID);
                        }else {
                            result = true;
                        }
                    }else{
                        opResult = BuildCall(PhoneParam.CALL_SERVER_ID, UserInterface.CALL_BROADCAST_TYPE);
                        if(opResult.result!=OperationResult.OP_RESULT_OK){
                            UserInterface.PrintLog("DEV %s Make BroadCast Call Fail",devid);
                        }else{
                            result = true;
                        }
                    }
                }else {
                    if (devLists == null) {
                        UserInterface.PrintLog("Select the %d Device , But Device Is NULL", selected);
                    } else {
                        if (selected < 1 || selected - 1 >= devLists.size()) {
                            UserInterface.PrintLog("Select the %d Device, Select is Out of Range", selected);
                        } else {
                            UserInterface.PrintLog("Select the %d Device, realy Select %d device", selected, selected - 1);
                            if (isCallOut) {
                                UserInterface.PrintLog("Device %s is Calling Out!!!!!!!!!!!!", devid);
                            } else {
                                UserDevice device = devLists.get(selected - 1);
                                UserInterface.PrintLog("Device %s Calling %s", devid, device.devid);
                                opResult = BuildCall(device.devid, UserInterface.CALL_NORMAL_TYPE);
                                if(opResult.result!=OperationResult.OP_RESULT_OK){
                                    UserInterface.PrintLog("DEV %s Make Call to DEV %s Fail, Reason=%s",devid,device.devid, FailReason.GetFailName(opResult.reason));
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
                    type == UserInterface.CALL_BED_DEVICE||
                    type == UserInterface.CALL_CORRIDOR_DEVICE) {
//                MainActivity.StopTest("Stop Test.......");
                if(inComingCallInfos.size()==1)
                    selected = 0;
                if (selected < inComingCallInfos.size()) {
                    if(true){//!isCallOut) {
                        UserInterface.PrintLog("Select %d Call",  selected);
                        LocalCallInfo callInfo;
                        callInfo = inComingCallInfos.get(selected);
                        if (callInfo.status == LocalCallInfo.LOCAL_CALL_STATUS_INCOMING) {
                            if(callInfo.callType==UserCallMessage.EMERGENCY_CALL_TYPE){
                                opResult = EndCall(callInfo.callID);
                                if(opResult.result!=OperationResult.OP_RESULT_OK){
                                    UserInterface.PrintLog("DEV %s End Call  %s Fail",devid,callInfo.callID);
                                }else{
                                    result = true;
                                }
                            }else if(callInfo.callType==UserCallMessage.NORMAL_CALL_TYPE){
                                if(talkPeer.isEmpty()) {
                                    opResult = AnswerCall(callInfo.callID);
                                    if(opResult.result!=OperationResult.OP_RESULT_OK){
                                        UserInterface.PrintLog("DEV %s Answer Call  %s Fail",devid,callInfo.callID);
                                    }else{
                                        result = true;
                                    }
                                }else{
                                    UserInterface.PrintLog("Dev %s is Talking , Could not Answer");
                                }
                            }
                        }else {
                            opResult = EndCall(callInfo.callID);
                            if(opResult.result!=OperationResult.OP_RESULT_OK){
                                UserInterface.PrintLog("DEV %s End Call  %s Fail",devid,callInfo.callID);
                            }else{
                                result = true;
                            }
                        }

// test for device answer all calls
//                        for(LocalCallInfo callInfo:inComingCallInfos){
//                            if (callInfo.status == LocalCallInfo.LOCAL_CALL_STATUS_INCOMING) {
//                                AnswerCall(callInfo.callID);
//                            }
//                        }

                        // test for answer 2 incoming Call
                        
                    }else{
                        UserInterface.PrintLog("Dev %s is Outgoing Call, Could not Answer");
                    }
                }else{
                    UserInterface.PrintLog("Select the %d Call, Select Out of Range",selected);
                }
            }

        }
        return result;
    }

    public boolean TestProcess(){
        boolean result = false;
        double randValue;
        LocalCallInfo callInfo;
        if(testInfo.isAutoTest&& isRegOk) {
            testTickCount++;
            if (testTickCount >= testWaitTick) {
                testTickCount = 0;
                randValue = Math.random();
                testWaitTick = (int) (randValue * (double)testInfo.timeUnit) + 10;
                result = true;
                synchronized (TestDevice.class) {
                    if(testInfo.testMode==0){ // call by bed
                        if(type==UserInterface.CALL_BED_DEVICE){
                            if(isCallOut){
                                if(outGoingCall.status== LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                                    EndCall(outGoingCall.callID);
                            }else{
                                BuildCall(PhoneParam.CALL_SERVER_ID, UserInterface.CALL_NORMAL_TYPE);
                            }
                        }else if(type==UserInterface.CALL_NURSER_DEVICE||
                                type==UserInterface.CALL_DOOR_DEVICE){
                            boolean hasConnectedCall = false;
                            for(LocalCallInfo info:inComingCallInfos){
                                if(info.status== LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED){
                                    hasConnectedCall = true;
                                    EndCall(info.callID);
                                    break;
                                }
                            }
                            if(!hasConnectedCall&&inComingCallInfos.size()>0) {
                                int selectCall = (int) (Math.random() * inComingCallInfos.size());
                                if (selectCall >= inComingCallInfos.size())
                                    selectCall = inComingCallInfos.size() - 1;
                                callInfo= inComingCallInfos.get(selectCall);
                                if (Math.random() > -10&&callInfo.callType==UserCallMessage.NORMAL_CALL_TYPE&&callInfo.status== LocalCallInfo.LOCAL_CALL_STATUS_INCOMING) {
                                    AnswerCall(callInfo.callID);
                                }else {
                                    EndCall(callInfo.callID);
                                }
                            }
                        }

                    }else if(testInfo.testMode==1) {  // nurser call
                        if(type==UserInterface.CALL_BED_DEVICE){
                            if(isCallOut){
                            }else{
                                if(inComingCallInfos.size()>0){
                                    callInfo = inComingCallInfos.get(0);
                                    if(callInfo.status==LocalCallInfo.LOCAL_CALL_STATUS_INCOMING)
                                        AnswerCall(callInfo.callID);
                                }
                            }
                        }else if(type==UserInterface.CALL_NURSER_DEVICE){
                            if(inComingCallInfos.size()<=0) {
                                if (!isCallOut) {
                                    if (devLists != null) {
                                        if (devLists.size() > 0) {
                                            int selectDev = (int) (Math.random() * devLists.size());
                                            if(selectDev>=devLists.size())
                                                selectDev = devLists.size()-1;
                                            UserDevice dev = devLists.get(selectDev);
                                            BuildCall(dev.devid, UserInterface.CALL_NORMAL_TYPE);
                                        }
                                    }
                                } else {
                                    if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                                        EndCall(outGoingCall.callID);
                                    }
                                }
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
                if(!isRegOk)
                    UserInterface.PrintLog("Receive Reg Succ of Dev %s , areaId=%s, areaName=%s",devid,msg.areaId,msg.areaName);
                isRegOk = true;
                transferAreaId = msg.transferAreaId;
                areaId = msg.areaId;
                bedlistenCalls = msg.enableListenCall;
                QueryConfig();
                QuerySystemConfig();
                break;
            case UserCallMessage.REGISTER_MESSAGE_FAIL:
                if(isRegOk)
                    UserInterface.PrintLog("Receive Reg Fail of Dev %s ",devid);
                isRegOk = false;
                break;
        }
    }

    public void UpdateVideoState(UserVideoMessage msg){
        if(msg.type==UserMessage.CALL_VIDEO_INVITE){
            UserInterface.PrintLog("Dev %s Receive Start Video for Call %s",devid,msg.callId);
            UserInterface.AnswerVideo(devid, msg.callId);
            videoCallId = msg.callId;
            isVideo = true;
        }else if(msg.type==UserMessage.CALL_VIDEO_ANSWERED){
            UserInterface.PrintLog("Dev %s Receive Answer Video for Call %s",devid,msg.callId);
            videoCallId = msg.callId;
            isVideo = true;
        }else if(msg.type==UserMessage.CALL_VIDEO_END){
            UserInterface.PrintLog("Dev %s Receive End Video for Call %s",devid,msg.callId);
            videoCallId = "";
            isVideo = false;
        }
    }

    public void UpdateTransferInfo(TransferMessage msg){
        if(msg.type == UserMessage.CALL_TRANSFER_SUCC||msg.type==UserMessage.CALL_TRANSFER_CHANGE){
            if(msg.state==true){
                transferAreaId = msg.transferAreaId;
            }else{
                transferAreaId = "";
            }
            UserInterface.PrintLog("TestDevice %s Update Transfer area to %s",devid,transferAreaId);
        }
    }

    public void UpdateListenInfo(ListenCallMessage msg){
        if(msg.type==UserMessage.CALL_LISTEN_SUCC||msg.type==UserMessage.CALL_LISTEN_CHANGE){
            bedlistenCalls = msg.state;
            UserInterface.PrintLog("TestDevice %s Update Listen State to %b",devid,bedlistenCalls);
        }
    }

    public String UpdateCallInfo(UserCallMessage msg){
        int result = 0;
        String failReason = "";
        boolean isFindMatched = false;
        synchronized (TestDevice.class) {
            switch (msg.type) {
                case UserCallMessage.CALL_MESSAGE_RINGING:
                    if(outGoingCall.status== LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING) {
                        outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_RINGING;
                        UserInterface.PrintLog("Set Out Goning Call %s to Ringing in Dev %s",  outGoingCall.callID,devid);
                        isFindMatched = true;
                    }

                    if(!isFindMatched){
                        UserInterface.PrintLog("ERROR %s Recv %s for Call %s, but couldn't find matched Call",devid,UserMessage.GetMsgName(msg.type),msg.callId);
                    }
                    break;
                case UserCallMessage.CALL_MESSAGE_DISCONNECT:
                case UserCallMessage.CALL_MESSAGE_UPDATE_FAIL:
                case UserCallMessage.CALL_MESSAGE_END_FAIL:    
                case UserCallMessage.CALL_MESSAGE_INVITE_FAIL:
                case UserCallMessage.CALL_MESSAGE_UNKNOWFAIL:
                    if(msg.type==UserCallMessage.CALL_MESSAGE_DISCONNECT){
                        UserInterface.PrintLog("DEV %s Recv End Msg for Call %s, reason is %s",devid,msg.callId,UserCallMessage.GetEndReasonName(msg.endReason));
                    }
                    if (outGoingCall.status!= LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT&&msg.callId.compareToIgnoreCase(outGoingCall.callID) == 0) {
                        if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                            talkPeer = "";
                        }
                        outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_DISCONNECT;
                        UserInterface.PrintLog("Disconnect Outgoing Call %s in Dev %s When Recv %s for %s", outGoingCall.callID,devid,UserMessage.GetMsgName(msg.type),FailReason.GetFailName(msg.reason));
                        isCallOut = false;
                        isFindMatched = true;
                    } else {
                        for (LocalCallInfo info : inComingCallInfos) {
                            if (info.callID.compareToIgnoreCase(msg.callId) == 0) {
                                if (info.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                                    talkPeer = "";
                                }
                                UserInterface.PrintLog("Disconnect Incoming Call %s in Dev %s When Recv %s for %s",info.callID,devid,UserMessage.GetMsgName(msg.type),FailReason.GetFailName(msg.reason));
                                inComingCallInfos.remove(info);
                                isFindMatched = true;
                                break;
                            }
                        }
                    }

                    if(!isFindMatched){
                        UserInterface.PrintLog("ERROR! Recv Disconnect Call %s in Dev %s, but couldn't find matched Call",msg.callId,devid,UserMessage.GetMsgName(msg.type));
                    }
                    break;
                case UserCallMessage.CALL_MESSAGE_ANSWERED:
                    if(outGoingCall.status== LocalCallInfo.LOCAL_CALL_STATUS_RINGING&&outGoingCall.callID.compareToIgnoreCase(msg.callId)==0) {
                        outGoingCall.status = LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED;
                        outGoingCall.answer = msg.operaterId;
                        UserInterface.PrintLog("Recv Answered Outgoing Call %s in Dev %s", outGoingCall.callID,devid);
                        talkPeer = outGoingCall.answer;
                    }else{
                        UserInterface.PrintLog("ERROR! %s Recv Answered of Call %s , but outgoingcall status = %d", devid, msg.callId,outGoingCall.status);
                    }
                    break;
                case UserCallMessage.CALL_MESSAGE_UPDATE_SUCC:
                        UserInterface.PrintLog("Recv Update Succ for Call %s in Dev %s", msg.callId,devid);
                    break;
                case UserCallMessage.CALL_MESSAGE_INCOMING:
                    LocalCallInfo info = new LocalCallInfo();
                    info.status = LocalCallInfo.LOCAL_CALL_STATUS_INCOMING;
                    info.callID = msg.callId;
                    info.callee = msg.calleeId;
                    info.caller = msg.callerId;
                    info.callType = msg.callType;
                    inComingCallInfos.add(info);
                    UserInterface.PrintLog("Recv Incoming Call %s in Dev %s ",info.callID,devid);
                    Integer count = inComingCallRecord.get(info.caller);
                    if(count==null){
                        inComingCallRecord.put(info.caller,1);
                    }else{
                        inComingCallRecord.put(info.caller,count+1);
                    }
                    break;
                case UserCallMessage.CALL_MESSAGE_CONNECT:
                    for (LocalCallInfo info1 : inComingCallInfos) {
                        if (info1.callID.compareToIgnoreCase(msg.callId) == 0) {
                            info1.status = LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED;
                            UserInterface.PrintLog("Set Incoming Call %s Connected in Dev %s",  info1.callID,devid);
                            talkPeer = info1.caller;
                            isFindMatched = true;
                            break;
                        }
                    }
                    if(!isFindMatched){
                        UserInterface.PrintLog("ERROR! %s Recv %s for Call %s, but couldn't find matched Call",devid,UserMessage.GetMsgName(msg.type),msg.callId);
                    }
                    break;
                case UserCallMessage.CALL_MESSAGE_ANSWER_FAIL:
                    UserInterface.PrintLog("ERROR! Dev %s Recv Answer Fail For Call %s , reason is %s",devid,msg.callId,UserMessage.GetMsgName(msg.reason));
                    // do nothing
                    break;
            }


            failReason = CheckTestStatus();
            if(!failReason.isEmpty())
                result = -1;
        }
        return failReason;
    }

    private String GetDeviceName(){

        return String.format("%s",devid);
    }


    public boolean IsTalking(){
        if(talkPeer.isEmpty())
            return false;
        else
            return true;
    }

    public void StartVideo(){
        String callId=null;
        
        if(outGoingCall.status==LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
            callId = outGoingCall.callID;
        else{
            for(LocalCallInfo info:inComingCallInfos){
                if(info.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED) {
                    callId = info.callID;
                    break;
                }
            }
        }

        if(callId!=null) {
            UserInterface.StartVideo(devid, callId);
            videoCallId = callId;
            isVideo = true;
        }
    }

    public void StopVideo(){
        if(!videoCallId.isEmpty()) {
            UserInterface.StopVideo(devid, videoCallId);
            videoCallId = "";
            isVideo = false;
        }
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
            if(devLists.get(iTmp).type == UserInterface.CALL_BED_DEVICE&&devLists.get(iTmp).isRegOk)
                bedNum++;
        }
        if(bedNum==0)
            return null;

        int selectDev = (int)(Math.random()*bedNum);
        if(selectDev>bedNum)
            selectDev = bedNum-1;

        curBedNum = 0;
        for(iTmp=0;iTmp<devLists.size();iTmp++){
            if(devLists.get(iTmp).type == UserInterface.CALL_BED_DEVICE&&devLists.get(iTmp).isRegOk) {
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
    
    public String GetCallInfo(){
        String status="" ;
        for (LocalCallInfo callInfo : inComingCallInfos) {
            switch (callInfo.status) {
                case LocalCallInfo.LOCAL_CALL_STATUS_INCOMING:
                    status += String.format("From %s, Incoming\n", callInfo.caller);
                    break;
                case LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED:
                    status += String.format("%s Talking with %s\n", devid,talkPeer);
                    if(talkPeer.isEmpty()){
                        ClientTest.StopTest(    String.format("TalkPeer of DEV %s Incoming Call %s is Empty",devid,callInfo.callID));
                    }
                    break;
                default:
                    status += String.format("From %s, Unexcept\n", callInfo.caller);
                    break;

            }
        }
        return status;
    }

    private String CheckTestStatus(){
        int talkNum = 0;
        boolean isRight = true;
        String failReason = "";
        String talkCallId="";
        if(outGoingCall.status== LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
            talkNum++;

        for (LocalCallInfo callInfo : inComingCallInfos) {
            switch (callInfo.status) {
                case LocalCallInfo.LOCAL_CALL_STATUS_INCOMING:
                    break;
                case LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED:
                    talkNum++;
                    talkCallId += callInfo.callID+"//";
                    if (talkNum > 1) {
                        UserInterface.PrintLog("Find Dev %s have %d Connected Call",devid,talkNum);
                        isRight = false;

                    }
                    break;
            }
        }
        if(!isRight){
            failReason = String.format("DEV %s has %d Talking Calls %s",devid,talkNum,talkCallId);
        }
        
        if(talkNum==0&&!talkPeer.isEmpty()){
            isRight = false;
            UserInterface.PrintLog("Find Dev %s have 0 Connected Call, but talkPeer is %s Not empty",devid,talkPeer);
            failReason = String.format("DEV %s Has No Talking Call, but talkpeer is %s, not empty",devid,talkPeer);
        }

        if(isRight) {
            for (int iTmp = 0; iTmp < inComingCallInfos.size(); iTmp++) {
                LocalCallInfo cp1 = inComingCallInfos.get(iTmp);
                for (int jTmp = iTmp+1; jTmp < inComingCallInfos.size(); jTmp++) {
                    LocalCallInfo cp2 = inComingCallInfos.get(jTmp);
                    if (cp1.callID.compareToIgnoreCase(cp2.callID) == 0) {
                        isRight = false;
                        UserInterface.PrintLog("Find Dev %s have Duplicate Call ID %s",devid,cp1.callID);
                        failReason = String.format("DEV %s Has Same Call %s",devid,cp1.callID);
                        break;
                    }
                    if (!isRight)
                        break;
                }
            }
        }
        return failReason;
    }

    public String GetDeviceInfo(){
        String status = "";
        if (isCallOut) {
            if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                status += String.format("%s Call to %s\n", GetDeviceName(), outGoingCall.callee);
            else if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                status += String.format("%s Call to %s, Ringing....\n", GetDeviceName(), outGoingCall.callee);
            else if (outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED){
                status += String.format("%s Talking with %s\n", GetDeviceName(), talkPeer);
                if(talkPeer.isEmpty()){
                    ClientTest.StopTest(String.format("TalkPeer of DEV %s Outgoing Call %s is Empty",devid,outGoingCall.callID));
                }
            }
            else
                status = String.format("%s Call to %s, Unknow....\n", GetDeviceName(), outGoingCall.callee);
        } else {
            if (isRegOk)
                status = String.format("%s Register Suss\n", GetDeviceName());
            else
                status = String.format("%s Register Fail\n", GetDeviceName());
        }
        return status;
    }

    public String GetNurserDeviceInfo(){
        String status;

        status = GetDeviceInfo();
        Integer incomingRecordNum;

        if (devLists != null) {
            for (int iTmp = 0; iTmp < devLists.size(); iTmp++) {
                UserDevice bedPhone = devLists.get(iTmp);
                incomingRecordNum = inComingCallRecord.get(bedPhone.devid);
                if(incomingRecordNum==null)
                    incomingRecordNum = 0;
                if (bedPhone.isRegOk) {
                    status += String.format("%s Register succ (%d)\n", bedPhone.devid,incomingRecordNum);
//                    UserInterface.PrintLog("%s-%s Register Succ",bedPhone.devid,bedPhone.bedName);
                } else {
                    status += String.format("%s Register Fail (%d)\n", bedPhone.devid,incomingRecordNum);
//                    UserInterface.PrintLog("%s-%s Register Fail",bedPhone.devid,bedPhone.bedName);
                }
            }
        }

        return status;
    }



    public  void SaveCallRecord(){
        File logWriteFile = new File(String.format("/storage/self/primary/CallRecord-%s.txt",devid));
        BufferedWriter bw = null;
        String writeString = "";
        try {
            bw = new BufferedWriter(new FileWriter(logWriteFile, true));
            for(String key:inComingCallRecord.keySet()) {
                Integer count = inComingCallRecord.get(key);
                bw.write(String.format("Recv %s inComing Call %d\r\n",key,count));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
