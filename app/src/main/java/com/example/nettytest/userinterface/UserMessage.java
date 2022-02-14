package com.example.nettytest.userinterface;

import com.example.nettytest.pub.result.FailReason;

public class UserMessage {
    public final static int MESSAGE_CALL_INFO = 1;
    public final static int MESSAGE_REG_INFO = 2;
    public final static int MESSAGE_DEVICES_INFO = 3;
    public final static int MESSAGE_CONFIG_INFO = 4;
    public final static int MESSAGE_TEST_TICK = 5;
    public final static int MESSAGE_SYSTEM_CONFIG_INFO = 6;
    public final static int MESSAGE_BACKEND_CALL_LOG = 7;
    public final static int MESSAGE_TRANSFER_INFO=8;
    public final static int MESSAGE_LISTEN_CALL_INFO = 9;
    public final static int MESSAGE_VIDEO_INFO = 10;
    public final static int MESSAGE_SNAP = 11;
    public final static int MESSAGE_INIT_FINISHED = 60;
    public final static int MESSAGE_UNKNOW = 100;

    public final static int REGISTER_MESSAGE_SUCC = 1;
    public final static int CALL_MESSAGE_INCOMING = 3;
    public final static int CALL_MESSAGE_CONNECT = 4;
    public final static int CALL_MESSAGE_DISCONNECT = 5;
    public final static int CALL_MESSAGE_RINGING = 6;
    public final static int CALL_MESSAGE_ANSWERED = 7;
    public final static int CALL_TRANSFER_SUCC = 8;
    public final static int CALL_LISTEN_SUCC = 9;

    public final static int CALL_VIDEO_INVITE = 10;
    public final static int CALL_VIDEO_ANSWERED = 11;
    public final static int CALL_VIDEO_END = 12;
    public final static int CALL_VIDEO_REQ_ANSWER = 13;
    public final static int CALL_MESSAGE_UPDATE_SUCC = 14;
    public final static int CALL_LISTEN_CHANGE = 15;
    public final static int CALL_TRANSFER_CHANGE = 16;

    public final static int CALL_MESSAGE_SUCC_MAX = 100;
    public final static int CALL_MESSAGE_INVITE_FAIL = 101;
    public final static int CALL_MESSAGE_ANSWER_FAIL = 102;
    public final static int CALL_MESSAGE_END_FAIL = 103;
    public final static int CALL_MESSAGE_UPDATE_FAIL = 104;
    public final static int REGISTER_MESSAGE_FAIL = 105;
    public final static int CALL_TRANSFER_FAIL = 106;
    public final static int CALL_LISTEN_FAIL = 107;


    public final static int CALL_MESSAGE_UNKNOWFAIL = 199;

    public final static int DEV_MESSAGE_LIST =200;
    public final static int CONFIG_MESSAGE_LIST = 201;

    public final static int SNAP_CONFIG = 221;

    public final static int CALL_MESSAGE_UNKONWQ = 0xffff;

    public int type;
    public int reason;
    public String devId;

    public UserMessage(){
        type = MESSAGE_UNKNOW;
        reason = FailReason.FAIL_REASON_NO;
        devId = "";
    }

    public static String GetMsgName(int type){
        String msgName ;

        switch(type){
            case REGISTER_MESSAGE_SUCC:
                msgName = "Register_Succ";
                break;
            case REGISTER_MESSAGE_FAIL:
                msgName = "Register_Fail";
                break;
            case CALL_MESSAGE_INCOMING:
                msgName = "Incoming_Call";
                break;
            case CALL_MESSAGE_CONNECT:
                msgName = "Call_Connected";
                break;
            case CALL_MESSAGE_DISCONNECT:
                msgName = "Call_Disconnected";
                break;
            case CALL_MESSAGE_RINGING:
                msgName = "Call_Ringing";
                break;
            case CALL_MESSAGE_ANSWERED:
                msgName = "Call_Answered";
                break;
            case CALL_MESSAGE_UNKNOWFAIL:
                msgName = "Call_Fail";
                break;
            case CALL_MESSAGE_INVITE_FAIL:
                msgName = "Invite_Fail";
                break;
            case CALL_MESSAGE_ANSWER_FAIL:
                msgName = "Answer_Fail";
                break;
            case CALL_MESSAGE_END_FAIL:
                msgName = "End_Fail";
                break;
            case CALL_MESSAGE_UPDATE_FAIL:
                msgName = "Update_Fail";
                break;
            case CALL_MESSAGE_UPDATE_SUCC:
                msgName = "Update_Succ";
                break;

            case CALL_MESSAGE_UNKONWQ:
                msgName = "Message_Unknow";
                break;
            case DEV_MESSAGE_LIST:
                msgName = "Dev_List";
                break;
            case CONFIG_MESSAGE_LIST:
                msgName = "Config_List";
                break;

            case CALL_VIDEO_INVITE:
                msgName = "Video_Invite";
                break;
            case CALL_VIDEO_ANSWERED:
                msgName = "Video_answered";
                break;
            case CALL_VIDEO_REQ_ANSWER:
                msgName = "Video_answer";
                break;
            case CALL_VIDEO_END:
                msgName = "Video_End";
                break;

            case CALL_LISTEN_SUCC:
                msgName = "Listen_End";
                break;
            case CALL_TRANSFER_SUCC:
                msgName = "Transfer_Succ";
                break;

            case CALL_TRANSFER_FAIL:
                msgName = "Transfer_Fail";
                break;
            case CALL_LISTEN_FAIL:
                msgName = "Listen_Fail";
                break;
            case CALL_LISTEN_CHANGE:
                msgName = "Listen_Change";
                break;
            case CALL_TRANSFER_CHANGE:
                msgName = "Transfer_Change";
                break;
            case SNAP_CONFIG:
                msgName = "Snap_Config";
                break;

            default:
                msgName = "Message_Unknow_DD";
                break;
        }

        return msgName;
    }
}
