package com.example.nettytest.userinterface;

import android.os.Handler;

import com.example.nettytest.backend.backendphone.BackEndConfig;
import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.backend.callserver.DemoServer;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;

import java.util.ArrayList;

public class UserInterface {
    public final static int CALL_BED_DEVICE = 2;
    public final static int CALL_DOOR_DEVICE = 6;
    public final static int CALL_NURSER_DEVICE = 9;
    public final static int CALL_TV_DEVICE = 8;
    public final static int CALL_CORRIDOR_DEVICE = 7;

    public final static int CALL_NORMAL_TYPE = 1;
    public final static int CALL_EMERGENCY_TYPE = 2;
    public final static int CALL_BROADCAST_TYPE = 3;

    public static DemoServer callServer=null;

    // backend
    public static void StartServer(){
        callServer = new DemoServer(PhoneParam.callServerPort);
    }

    public static OperationResult ConfigDeviceParamOnServer(String id, ArrayList<UserConfig>list){
        OperationResult result = new OperationResult();

        if(!HandlerMgr.SetBackEndPhoneConfig(id,list)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTFOUND;
        }
        return result;
    }

    public static OperationResult ConfigServerParam(BackEndConfig config){
        OperationResult result = new OperationResult();
        HandlerMgr.SetBackEndConfig(config);
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

    public static OperationResult AddDeviceOnServer(String id,int type){
        OperationResult result = new OperationResult();
        int typeInServer = CALL_BED_DEVICE;

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
        }

        if(!callServer.AddBackEndPhone(id, typeInServer)){
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_UNKNOW;
        }
        return result;
    }

    public static boolean SetMessageHandler(Handler handler){
        HandlerMgr.SetTerminalMessageHandler(handler);
        return true;
    }

    //Terminal

    public static OperationResult BuildDevice(int type, String ID){
        OperationResult result = new OperationResult();
        switch(type) {
            case CALL_BED_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.BED_CALL_DEVICE);
                break;
            case CALL_NURSER_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.NURSE_CALL_DEVICE);
                break;
            case CALL_DOOR_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.DOOR_CALL_DEVICE);
                break;
            case CALL_TV_DEVICE:
                HandlerMgr.CreateTerminalPhone(ID, TerminalPhone.TV_CALL_DEVICE);
                break;
            default:
                result.result = OperationResult.OP_RESULT_FAIL;
                result.reason = FailReason.FAIL_REASON_NOTSUPPORT;
                break;

        }
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
            default:
                terminamCallType = CommonCall.CALL_TYPE_NORMAL;
                break;
        }
        callid = HandlerMgr.BuildTerminalCall(id, peerId,terminamCallType);
        if(callid!=null){
            result.callID = callid;
        }else{
            result.result = OperationResult.OP_RESULT_FAIL;
            result.reason = FailReason.FAIL_REASON_NOTSUPPORT;
        }
        return result;
    }

    public static OperationResult EndCall(String devid, String callid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.EndTerminalCall(devid,callid);
        result = new OperationResult(operationCode);

        result.callID = callid;
        return result;
    }

    public static OperationResult AnswerCall(String devid, String callid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.AnswerTerminalCall(devid,callid);
        result = new OperationResult(operationCode);
        result.callID = callid;
        return result;
    }

    public static OperationResult QueryDevs(String devid){
        int operationCode;
        OperationResult result;

        operationCode = HandlerMgr.QueryTerminalLists(devid);
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
        }

        return name;
    }

    public static int GetDeviceType(String name){
        int type = CALL_BED_DEVICE;

        if(name.compareToIgnoreCase("door")==0)
            type = CALL_DOOR_DEVICE;
        else if(name.compareToIgnoreCase("nurser")==0)
            type = CALL_NURSER_DEVICE;
        else if(name.compareToIgnoreCase("TV")==0)
            type = CALL_TV_DEVICE;
        return type;
    }
}
