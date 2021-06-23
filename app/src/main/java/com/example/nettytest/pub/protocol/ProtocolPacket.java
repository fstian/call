package com.example.nettytest.pub.protocol;

public class ProtocolPacket {

    public final static String PACKET_TYPE_NAME = "msgType";
    public final static String PACKET_MSGID_NAME = "msgID";
    public final static String PACKET_SENDERID_NAME = "SenderID";
    public final static String PACKET_RECEIVERID_NAME = "receiverID";
    public final static String PACKET_CONTEXT_NAME = "context";
    public final static String PACKET_LISTEN_STATE_NAME = "listenState";
    public final static String PACKET_TRANSFER_STATE_NAME = "transferState";

    public final static String PACKET_DEVTYPE_NAME = "deviceType";
    public final static String PACKET_DEVID_NAME = "deviceID";
    public final static String PACKET_ADDRESS_NAME = "localIP";
    public final static String PACKET_EXPIRE_NAME = "expireTime";

    public final static String PACKET_AREAID_NAME  = "areaId";
    public final static String PACKET_TRANSFER_AREAID_NAME  = "transferAreaId";
    public final static String PACKET_AREANAME_NAME  = "areaName";
    public final static String PACKET_ISTRANSFER_NAME = "isTransfer";

    public final static String PACKET_STATUS_NAME = "status";
    public final static String PACKET_RESULT_NAME = "result";

    public final static String PACKET_CALLTYPE_NAME = "callType";
    public final static String PACKET_CALLDIRECT_NAME = "callDirect";
    public final static String PACKET_CODEC_NAME = "codec";
    public final static String PACKET_PTIME_NAME = "ptime";
    public final static String PACKET_SAMPLE_NAME = "sample";
    public final static String PACKET_CALLER_NAME = "caller";
    public final static String PACKET_CALLEE_NAME = "callee";
    public final static String PACKET_ANSWERER_NAME = "answerer";
    public final static String PACKET_BEDID_NAME = "bedName";
    public final static String PACKET_PATIENT_NAME_NAME = "patientName";
    public final static String PACKET_PATIENT_AGE_NAME = "patientAge";
    public final static String PACKET_ROOMID_NAME = "roomId";
    public final static String PACKET_DEVICE_NAME_NAME = "deviceName";
    public final static String PACKET_CALLERIP_MAME = "callerIP";
    public final static String PACKET_CALLERPORT_NAME = "callerPort";
    public final static String PACKET_CALLERTYPE_NAME = "callerType";
    public final static String PACKET_CALLEEIP_MAME = "calleeIP";
    public final static String PACKET_CALLEEPORT_NAME = "calleePort";
    public final static String PACKET_CALLID_NAME = "callID";
    public final static String PACKET_DETAIL_NAME = "detail";
    public final static String PACKET_AUTOANSWER_TIME_NAME = "autoAnswerTime";

    public final static String PACKET_PARAMS_NAME = "params";
    public final static String PACKET_PARAM_ID_NAME = "paramId";
    public final static String PACKET_PARAM_NAME_NAME = "paramName";
    public final static String PACKET_PARAM_VALUE_NAME = "paramValue";
    public final static String PACKET_PARAM_UNIT_NAME = "paramUnit";

    public final static int REG_REQ = 1;
    public final static int REG_RES = 101;
    public final static int CALL_REQ = 2;
    public final static int CALL_RES = 102;
    public final static int ANSWER_REQ = 3;
    public final static int ANSWER_RES = 103;
    public final static int END_REQ = 4;
    public final static int END_RES = 104;
    public final static int CALL_QUERY_REQ = 5;
    public final static int CALL_QUERY_RES = 105;
    public final static int DEV_QUERY_REQ = 6;
    public final static int DEV_QUERY_RES = 106;
    public final static int CALL_UPDATE_REQ = 7;
    public final static int CALL_UPDATE_RES = 107;
    public final static int DEV_CONFIG_REQ = 8;
    public final static int DEV_CONFIG_RES = 108;
    public final static int SYSTEM_CONFIG_REQ = 9;
    public final static int SYSTEM_CONFIG_RES = 109;
    public final static int CALL_TRANSFER_REQ = 10;
    public final static int CALL_TRANSFER_RES = 110;
    public final static int CALL_LISTEN_REQ = 11;
    public final static int CALL_LISTEN_RES = 111;

    public final static int CALL_VIDEO_INVITE_REQ = 12;
    public final static int CALL_VIDEO_INVITE_RES = 112;

    public final static int CALL_VIDEO_ANSWER_REQ = 13;
    public final static int CALL_VIDEO_ANSWER_RES = 113;
    

    public final static int CALL_VIDEO_END_REQ = 14;
    public final static int CALL_VIDEO_END_RES = 114;

    public final static int MAX_REQ_TYPE = 100;
    public final static int UNKNOW_CMD_RES = 200;

    public final static int UNKNOWPROTOCOLTYPE = 0xefff;

    public final static int STATUS_OK = 200;
    public final static int STATUS_BADREQ = 400;
    public final static int STATUS_FORBID = 403;
    public final static int STATUS_NOTFOUND = 404;
    public final static int STATUS_TIMEOVER = 408;
    public final static int STATUS_CONFILICT = 409;
    public final static int STATUS_BUSY = 486;
    public final static int STATUS_NOTSUPPORT = 490;
    public final static int STATUS_DUPLICATE = 491;

    public final static int STATUS_DECLINE = 603;

    public final static int UNKONWSTATUATYPE = 0xffff;


    public int type;
    public String msgID;
    public String sender;
    public String receiver;

    public ProtocolPacket(){
        type = UNKNOWPROTOCOLTYPE;
        msgID = "";
        sender = "";
        receiver = "";
    }

    public void CopyData(ProtocolPacket p){
        type = p.type;
        msgID = p.msgID;
        sender = p.sender;
        receiver = p.receiver;
    }

    public int ExchangeCopyData(ProtocolPacket p){
        type = p.type;
        msgID = p.msgID;
        sender = p.receiver;
        receiver = p.sender;
        return 0;
    }

    public static String GetResString(int res){
        String resName = "Unknow Reason";
        switch(res){
            case STATUS_OK:
                resName = "200 OK";
                break;
            case STATUS_FORBID:
                resName = "403 Forbidden";
                break;
            case STATUS_NOTFOUND:
                resName = "404 Not Found";
                break;
            case STATUS_TIMEOVER:
                resName = "408 Request timeout";
                break;
            case STATUS_CONFILICT:
                resName = "409 Confilict";
                break;
            case STATUS_BUSY:
                resName = "486 Busy";
                break;
            case STATUS_DECLINE:
                resName = "603 Decline";
                break;
            case STATUS_NOTSUPPORT:
                resName = "490 Not Support";
                break;
            case STATUS_DUPLICATE:
                resName = "491 Duplicate";
                break;
            case STATUS_BADREQ:
                resName = "400 Bad Req";
                break;
        }
        return resName;
    }

    public static String GetTypeName(int type){
        String packetTypeName = "Unknow Req";
        switch(type){
            case REG_REQ:
                packetTypeName = "Reg_Req";
                break;
            case REG_RES:
                packetTypeName = "Reg_Res";
                break;
            case CALL_REQ:
                packetTypeName = "Call_Req";
                break;
            case CALL_RES:
                packetTypeName = "Call_Res";
                break;
            case ANSWER_REQ:
                packetTypeName = "Answer_Req";
                break;
            case ANSWER_RES:
                packetTypeName = "Answer_Res";
                break;
            case END_REQ:
                packetTypeName = "End_Req";
                break;
            case END_RES:
                packetTypeName = "End_Res";
                break;
            case CALL_QUERY_REQ:
                packetTypeName = "Call_Query_Req";
                break;
            case CALL_QUERY_RES:
                packetTypeName = "Call_Query_Res";
                break;
            case DEV_QUERY_REQ:
                packetTypeName = "DEV_Query_Req";
                break;
            case DEV_QUERY_RES:
                packetTypeName = "DEV_Query_Res";
                break;
            case CALL_UPDATE_REQ:
                packetTypeName = "Call_Update_Req";
                break;
            case CALL_UPDATE_RES:
                packetTypeName = "Call_Update_Res";
                break;
            case DEV_CONFIG_REQ:
                packetTypeName = "Dev_Config_req";
                break;
            case DEV_CONFIG_RES:
                packetTypeName = "Dev_Config_res";
                break;
            case SYSTEM_CONFIG_REQ:
                packetTypeName = "System_Config_req";
                break;
            case SYSTEM_CONFIG_RES:
                packetTypeName = "System_Config_res";
                break;
            case CALL_TRANSFER_REQ:
                packetTypeName = "Call_Transfer_req";
                break;
            case CALL_TRANSFER_RES:
                packetTypeName = "Call_Transfer_res";
                break;
            case CALL_LISTEN_REQ:
                packetTypeName = "Call_Listen_req";
                break;
            case CALL_LISTEN_RES:
                packetTypeName = "Call_Listen_res";
                break;
            case CALL_VIDEO_INVITE_REQ:
                packetTypeName = "Video_Invite_req";
                break;
            case CALL_VIDEO_INVITE_RES:
                packetTypeName = "Video_Invite_res";
                break;
            case CALL_VIDEO_END_REQ:
                packetTypeName = "Video_End_req";
                break;
            case CALL_VIDEO_END_RES:
                packetTypeName = "Video_End_res";
                break;
            case CALL_VIDEO_ANSWER_REQ:
                packetTypeName = "Video_Answer_req";
                break;
            case CALL_VIDEO_ANSWER_RES:
                packetTypeName = "Video_Answer_res";
                break;
        }
        return packetTypeName;
    }

    public void Release(){

    }
}
