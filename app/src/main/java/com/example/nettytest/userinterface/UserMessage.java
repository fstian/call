package com.example.nettytest.userinterface;

public class UserMessage {
    public final static int MESSAGE_CALL_INFO = 1;
    public final static int MESSAGE_REG_INFO = 2;
    public final static int MESSAGE_DEVICES_INFO = 3;
    public final static int MESSAGE_CONFIG_INFO = 4;
    public final static int MESSAGE_TEST_TICK = 5;
    public final static int MESSAGE_UNKNOW = 100;

    public final static int REGISTER_MESSAGE_SUCC = 1;
    public final static int REGISTER_MESSAGE_FAIL = 2;
    public final static int CALL_MESSAGE_INCOMING = 3;
    public final static int CALL_MESSAGE_CONNECT = 4;
    public final static int CALL_MESSAGE_DISCONNECT = 5;
    public final static int CALL_MESSAGE_RINGING = 6;
    public final static int CALL_MESSAGE_ANSWERED = 7;
    public final static int CALL_MESSAGE_INVITE_FAIL = 101;
    public final static int CALL_MESSAGE_ANSWER_FAIL = 102;
    public final static int CALL_MESSAGE_END_FAIL = 103;
    public final static int CALL_MESSAGE_UPDATE_FAIL = 104;
    public final static int CALL_MESSAGE_UNKNOWFAIL = 199;

    public final static int DEV_MESSAGE_LIST =200;
    public final static int CONFIG_MESSAGE_LIST = 201;

    public final static int CALL_MESSAGE_UNKONWQ = 0xffff;

    public int type;
    public int reason;
    public String devId;

    public UserMessage(){
        type = MESSAGE_UNKNOW;
        reason = FailReason.FAIL_REASON_NO;
        devId = "";
        devId = "";
    }

    public static String GetMsgName(int type){
        String msgName ;

        switch(type){
            case REGISTER_MESSAGE_SUCC:
                msgName = "Register Succ";
                break;
            case REGISTER_MESSAGE_FAIL:
                msgName = "Register Fail";
                break;
            case CALL_MESSAGE_INCOMING:
                msgName = "Incoming Call";
                break;
            case CALL_MESSAGE_CONNECT:
                msgName = "Call Connected";
                break;
            case CALL_MESSAGE_DISCONNECT:
                msgName = "Call Disconnected";
                break;
            case CALL_MESSAGE_RINGING:
                msgName = "Call Ringing";
                break;
            case CALL_MESSAGE_ANSWERED:
                msgName = "Call Answered";
                break;
            case CALL_MESSAGE_UNKNOWFAIL:
                msgName = "Call Fail";
                break;
            case CALL_MESSAGE_INVITE_FAIL:
                msgName = "Invite Fail";
                break;
            case CALL_MESSAGE_ANSWER_FAIL:
                msgName = "Answer Fail";
                break;
            case CALL_MESSAGE_END_FAIL:
                msgName = "End Fail";
                break;
            case CALL_MESSAGE_UPDATE_FAIL:
                msgName = "Update Fail";
                break;

            case CALL_MESSAGE_UNKONWQ:
                msgName = "Message Unknow";
                break;
            case DEV_MESSAGE_LIST:
                msgName = "Dev List";
                break;
            default:
                msgName = "Message Unknow DD";
                break;
        }

        return msgName;
    }
}
