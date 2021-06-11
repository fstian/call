package com.example.nettytest.userinterface;

import com.example.nettytest.backend.backendphone.BackEndZone;
import com.example.nettytest.backend.backendphone.BackEndConfig;
import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.backend.callserver.DemoServer;
import com.example.nettytest.pub.CallParams;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.pub.result.OperationResult;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;

import java.util.ArrayList;

public class UserInterface {
    public final static int CALL_BED_DEVICE = 2;
    public final static int CALL_DOOR_DEVICE = 6;
    public final static int CALL_NURSER_DEVICE = 9;
    public final static int CALL_TV_DEVICE = 8;
    public final static int CALL_CORRIDOR_DEVICE = 7;
    public final static int CALL_EMERGENCY_DEVICE = 4;
    public final static int CALL_DOOR_LIGHT_DEVICE = 5;
    public final static int CALL_WHITE_BOARD_DEVICE = 10;
    public final static int CALL_SERVER_DEVICE = 90;
    public final static int CALL_UNKNOW_DEVICE = 100;

    public final static int CALL_NORMAL_TYPE = 1;
    public final static int CALL_EMERGENCY_TYPE = 2;
    public final static int CALL_BROADCAST_TYPE = 3;
    public final static int CALL_ASSIST_TYPE = 4;

    public final static int CALL_DIRECTION_S2C = 1;
    public final static int CALL_DIRECTION_C2S = 2;

    public final static int CALL_ANSWER_MODE_STOP = 1;
    public final static int CALL_ANSWER_MODE_HANDLE = 2;
    public final static int CALL_ANSWER_MODE_ANSWER = 3;

    public final static int NET_MODE_TCP = 1;
    public final static int NET_MODE_UDP = 2;

    public static DemoServer callServer=null;


    // backend

    public static void InitParam(String path,String fileName){
        PhoneParam.InitPhoneParam(path, fileName);
    }
    
    public static void StartServer(){
        if(PhoneParam.serverActive)
        callServer = new DemoServer(PhoneParam.callServerPort);
    }

    public static void CreateServerDevices(){
        if(PhoneParam.serverActive) {
            if(!PhoneParam.serviceActive){
                for(int iTmp=0;iTmp<PhoneParam.devicesOnServer.size();iTmp++){
                    UserDevice dev = PhoneParam.devicesOnServer.get(iTmp);
                    UserInterface.AddAreaInfoOnServer(dev.areaId,"");
                    UserInterface.AddDeviceOnServer(dev.devid,dev.type,dev.netMode,dev.areaId);
                }
            }else{
                HandlerMgr.StartCheckDevices(PhoneParam.serviceAddress,PhoneParam.servicePort);
            }
        }
        
        if(PhoneParam.clientActive) {
            
        }
    }

    public static OperationResult ConfigDeviceParamOnServer(String id, ArrayList<UserConfig>list){
        OperationResult result = new OperationResult();

        if(!HandlerMgr.SetBackEndPhoneConfig(id,list)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
        }
        return result;
    }

    public static OperationResult ConfigSystemParamOnServer(ArrayList<UserConfig>list){
        OperationResult result = new OperationResult();

        if(!HandlerMgr.SetBackEndSystemConfig(list)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
        }
        return result;
        
    }

    public static OperationResult ConfigServerParam(String areaId,BackEndConfig config){
        OperationResult result = new OperationResult();
        HandlerMgr.SetBackEndConfig(areaId,config);
        return result;
    }

    public static OperationResult RemoveDeviceOnServer(String id){
        OperationResult result = new OperationResult();
        if(!HandlerMgr.RemoveBackEndPhone(id)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
            PrintLog("Remove Device %s From Server Fail",id);
        }else{
            PrintLog("Remove Device %s From Server Success",id);
        }
        return result;

    }

    
    public static OperationResult RemoveAllDeviceOnServer(){
        OperationResult result = new OperationResult();

        if(!HandlerMgr.RemoveAllBackEndPhone()){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
            PrintLog("Remove All Devices From Server Fail");
        }else{
            PrintLog("Remove All Devices From Server Success");
        }
        return result;

    }

    public static OperationResult ConfigDeviceInfoOnServer(String id, ServerDeviceInfo info){
        OperationResult result = new OperationResult();

        if(!HandlerMgr.SetBackEndPhoneInfo(id,info)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
        }
        return result;

    }

    public static OperationResult AddAreaInfoOnServer(String areaId,String areaName){
        OperationResult result = new OperationResult();
        int failReason;

        failReason = HandlerMgr.AddBackEndArea(areaId,areaName);
        
        if(failReason!= FailReason.FAIL_REASON_NO){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = failReason;
        }
        return result;
    }


    public static OperationResult AddDeviceOnServer(String id,int type){
        return AddDeviceOnServer(id,type,UserInterface.NET_MODE_TCP);
    }

    public static OperationResult AddDeviceOnServer(String id,int type,int netMode){
        return AddDeviceOnServer(id,type,netMode, BackEndZone.DEFAULT_AREA_ID);
    }

    public static OperationResult AddDeviceOnServer(String id,int type,int netMode,String area){
        OperationResult result = new OperationResult();
        int failReason;
        int typeInServer = CALL_BED_DEVICE;
        int netType;

        if(netMode == UserInterface.NET_MODE_UDP)
            netType = PhoneParam.UDP_PROTOCOL;
        else
            netType = PhoneParam.TCP_PROTOCOL;

        switch(type){
            case CALL_BED_DEVICE:
                typeInServer = BackEndPhone.BED_CALL_DEVICE;
                break;
            case CALL_DOOR_DEVICE:
                typeInServer = BackEndPhone.DOOR_CALL_DEVICE;
                break;
            case CALL_NURSER_DEVICE:
                typeInServer = BackEndPhone.NURSE_CALL_DEVICE;
                break;
            case CALL_TV_DEVICE:
                typeInServer = BackEndPhone.TV_CALL_DEVICE;
                break;
            case CALL_CORRIDOR_DEVICE:
                typeInServer = BackEndPhone.CORRIDOR_CALL_DEVICE;
                break;
            case CALL_DOOR_LIGHT_DEVICE:
                typeInServer = BackEndPhone.DOOR_LIGHT_CALL_DEVICE;
                break;
            case CALL_WHITE_BOARD_DEVICE:
                typeInServer = BackEndPhone.WHITE_BOARD_DEVICE;
                break;
            case CALL_EMERGENCY_DEVICE:
                typeInServer = BackEndPhone.EMER_CALL_DEVICE;
                break;
        }

        failReason = callServer.AddBackEndPhone(id, typeInServer,netType,area);
        if(failReason!= FailReason.FAIL_REASON_NO){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = failReason;
        }
        return result;
    }

    public static ArrayList<UserDevice> GetDeviceInfoOnServer(String areaId){
        ArrayList<UserDevice> devLists = HandlerMgr.GetBackEndPhoneInfo(areaId);
        return devLists;
    }

    public static int UpdateAreas(ArrayList<UserArea> areaList) {
    	ArrayList<BackEndZone> list = new ArrayList<>();
    	for(UserArea area:areaList) {
    		BackEndZone zone = new BackEndZone(area.areaId,area.areaName);
    		list.add(zone);
    	}
    	HandlerMgr.UpdateAreas(list);
    	return 0;
    }

    public static int UpdateAreaDevices(String areaId,ArrayList<UserDevice> devList,ArrayList<ServerDeviceInfo> infoList) {
    	HandlerMgr.UpdateAreaDevices(areaId,devList,infoList);
    	return 0;
    }
    
    public static int UpdateAreaParam(String areaId,CallParams params){
        HandlerMgr.UpdateAreaParam(areaId,params);
        return 0;
    }

    public static OperationResult BuildDevice(int type, String ID) {
        return BuildDevice(type,ID,UserInterface.NET_MODE_TCP);
    }

    public static OperationResult BuildDevice(int type, String ID,int netMode){
        OperationResult result = new OperationResult();
        int netType;
        if(netMode == UserInterface.NET_MODE_UDP)
            netType = PhoneParam.UDP_PROTOCOL;
        else
            netType = PhoneParam.TCP_PROTOCOL;
        switch(type) {
            case CALL_BED_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.BED_CALL_DEVICE,netType);
                break;
            case CALL_NURSER_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.NURSE_CALL_DEVICE,netType);
                break;
            case CALL_DOOR_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.DOOR_CALL_DEVICE,netType);
                break;
            case CALL_TV_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.TV_CALL_DEVICE,netType);
                break;
            case CALL_CORRIDOR_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.CORRIDOR_CALL_DEVICE,netType);
                break;
            case CALL_WHITE_BOARD_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.WHITE_BOARD_DEVICE,netType);
                break;
            case CALL_EMERGENCY_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.EMER_CALL_DEVICE,netType);
                break;
            case CALL_DOOR_LIGHT_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.DOOR_LIGHT_CALL_DEVICE,netType);
                break;
            default:
                result.result = OperationResult.OP_RESULT_FAIL;
                result.reason = FailReason.FAIL_REASON_NOTSUPPORT;
                break;

        }
        return result;
    }

    public static OperationResult RemoveDevice(String id){
        OperationResult result = new OperationResult();
        HandlerMgr.RemoveTerminalPhone(id);
        return result;
    }

    public static OperationResult BuildCall(String id, String peerId, int type){
        OperationResult result = new OperationResult();
        String callid;

        int terminamCallType;
        switch(type){
            case CALL_EMERGENCY_TYPE:
                terminamCallType = CommonCall.CALL_TYPE_EMERGENCY;
                break;
            case CALL_BROADCAST_TYPE:
                terminamCallType = CommonCall.CALL_TYPE_BROADCAST;
                break;
            case CALL_ASSIST_TYPE:
                terminamCallType = CommonCall.CALL_TYPE_ASSIST;
                break;
            default:
                terminamCallType = CommonCall.CALL_TYPE_NORMAL;
                break;
        }
        callid = HandlerMgr.BuildTerminalCall(id, peerId,terminamCallType);
        if(callid!=null){
            result.callID = callid;
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Create Call From %s to %s Success, CallID = %s",id,peerId,callid);
        }else{
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTSUPPORT;
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Create Call From %s to %s Fail, Reason is %s",id,peerId,FailReason.GetFailName(result.reason));
        }

        return result;
    }

    public static OperationResult EndCall(String devid, String callid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.EndTerminalCall(devid,callid);
        result = new OperationResult(operationCode);

        if(operationCode== ProtocolPacket.STATUS_OK)
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"End Call %s by %s success",callid,devid);
        else
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"End Call %s by %s Fail, Reason is %s",callid,devid,FailReason.GetFailName(result.reason));

        result.callID = callid;
        return result;
    }

    public static OperationResult AnswerCall(String devid, String callid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.AnswerTerminalCall(devid,callid);
        result = new OperationResult(operationCode);
        result.callID = callid;
        if(operationCode== ProtocolPacket.STATUS_OK)
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Answer Call %s by %s success",callid,devid);
        else
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Anwser Call %s by %s Fail, Reason is %s",callid,devid,FailReason.GetFailName(result.reason));
        return result;
    }

    public static OperationResult StopVideo(String devid,String callid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.StopTerminalVideo(devid,callid);
        result = new OperationResult(operationCode);
        result.callID = callid;
        if(operationCode== ProtocolPacket.STATUS_OK)
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Stop Video in Call %s by %s success",callid,devid);
        else
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Stop Video in Call %s by %s Fail, Reason is %s",callid,devid,FailReason.GetFailName(result.reason));
        return result;
    }

    public static OperationResult StartVideo(String devid,String callid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.StartTerminalVideo(devid,callid);
        result = new OperationResult(operationCode);
        result.callID = callid;
        if(operationCode== ProtocolPacket.STATUS_OK)
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Start Video in Call %s by %s success",callid,devid);
        else
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Start Video in Call %s by %s Fail, Reason is %s",callid,devid,FailReason.GetFailName(result.reason));
        return result;
    }

    public static OperationResult AnswerVideo(String devId,String callId){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.AnswerTerminalVideo(devId,callId);
        result = new OperationResult(operationCode);
        result.callID = callId;
        if(operationCode== ProtocolPacket.STATUS_OK)
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Answer Video in Call %s by %s success",callId,devId);
        else
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Answer Video in Call %s by %s Fail, Reason is %s",callId,devId,FailReason.GetFailName(result.reason));
        return result;
    }

    public static OperationResult QueryDevs(String devid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.QueryTerminalLists(devid);
        result = new OperationResult(operationCode);

        return result;
    }

    public static OperationResult SetTransferCall(String devid,String areaId,boolean state){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.RequireCallTransfer(devid,areaId,state);
        result = new OperationResult(operationCode);

        return result;
    }

    public static OperationResult SetListenCall(String devid,boolean state){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.RequireBedListen(devid,state);
        result = new OperationResult(operationCode);

        return result;
    }

    public static OperationResult QueryDevConfig(String devid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.QueryTerminalConfig(devid);
        result = new OperationResult(operationCode);

        return result;

    }

    public static OperationResult QuerySystemConfig(String devid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.QuerySystemConfig(devid);
        result = new OperationResult(operationCode);

        return result;

    }


    public static OperationResult SetDevInfo(String devid,TerminalDeviceInfo info){
        OperationResult result = new OperationResult();
        if(!HandlerMgr.SetTerminalPhoneConfig(devid,info)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
        }
        return result;
    }

    public static boolean PrintLog(String f,Object...param){
        LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_DEBUG,f,param);
        return true;
    }

    public static boolean PrintLog(String f){
        LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_DEBUG,f);
        return true;
    }

    public static String GetDeviceTypeName(int type){
        String name = "";

        switch (type){
            case CALL_BED_DEVICE:
                name = "bed";
                break;
            case CALL_DOOR_DEVICE:
                name = "door";
                break;
            case CALL_NURSER_DEVICE:
                name = "nurser";
                break;
            case CALL_TV_DEVICE:
                name = "TV";
                break;
            case CALL_EMERGENCY_DEVICE:
                name = "emergency";
                break;
            case CALL_DOOR_LIGHT_DEVICE:
                name = "door light";
                break;
            case CALL_WHITE_BOARD_DEVICE:
                name = "white board";
                break;
            case CALL_CORRIDOR_DEVICE:
                name = "corridor";
                break;
        }

        return name;
    }

    public static int GetDeviceType(String name){
        int type = CALL_BED_DEVICE;

        if(name.compareToIgnoreCase("bed")==0)
            type = CALL_BED_DEVICE;
        else if(name.compareToIgnoreCase("door")==0)
            type = CALL_DOOR_DEVICE;
        else if(name.compareToIgnoreCase("nurser")==0)
            type = CALL_NURSER_DEVICE;
        else if(name.compareToIgnoreCase("TV")==0)
            type = CALL_TV_DEVICE;
        else if(name.compareToIgnoreCase("emergency")==0)
            type = CALL_EMERGENCY_DEVICE;
        else if(name.compareToIgnoreCase("doorLight")==0)
            type = CALL_DOOR_LIGHT_DEVICE;
        else if(name.compareToIgnoreCase("corridor")==0)
            type = CALL_CORRIDOR_DEVICE;
        else if(name.compareToIgnoreCase("whiteBoard")==0)
            type = CALL_WHITE_BOARD_DEVICE;
        return type;
    }

    public static String GetModuleVer(){
        return PhoneParam.VER_STR;
    }
    
   
}
