package com.example.nettytest.pub;

public class SystemSnap {

    public static final int SNAP_DEV_REQ = 1;
    public static final int SNAP_DEV_RES = 101;

    public static final int SNAP_TERMINAL_TRANS_REQ = 2;
    public static final int SNAP_TERMINAL_TRANS_RES = 102;

    public static final int SNAP_TERMINAL_CALL_REQ = 3;
    public static final int SNAP_TERMINAL_CALL_RES = 103;

    public static final int SNAP_BACKEND_TRANS_REQ = 4;
    public static final int SNAP_BACKEND_TRANS_RES = 104;

    public static final int SNAP_BACKEND_CALL_REQ = 5;
    public static final int SNAP_BACKEND_CALL_RES = 105;

    public static final int SNAP_MMI_CALL_REQ = 6;
    public static final int SNAP_MMI_CALL_RES = 106;

    public static final int LOG_CONFIG_REQ_CMD = 7;
    public static final int LOG_CONFIG_REQ_RES = 107;

    public static final int AUDIO_CONFIG_REQ_CMD = 8;
    public static final int AUDIO_CONFIG_RES_CMD = 108;

    public static final String SNAP_AUTOTEST_NAME = "autoTest";
    public static final String SNAP_REALTIME_NAME = "realTime";
    public static final String SNAP_TIMEUNIT_NAME = "timeUnit";

    public static final String SNAP_CMD_TYPE_NAME = "type";
    public static final String SNAP_DEVID_NAME = "devId";
    public static final String SNAP_DEVTYPE_NAME = "devType";
    public static final String SNAP_TRANS_NAME = "transactions";
    public static final String SNAP_TRANSTYPE_NAME = "transType";
    public static final String SNAP_SENDER_NAME = "sender";
    public static final String SNAP_RECEIVER_NAME = "receiver";
    public static final String SNAP_MSGID_NAME = "msgId";
    public static final String SNAP_REG_NAME = "reg";

    public static final String SNAP_INCOMINGS_NAME = "incomingCalls";
    public static final String SNAP_OUTGOINGS_NAME = "outGoingCalls";
    public static final String SNAP_CALLER_NAME = "caller";
    public static final String SNAP_CALLEE_NAME = "callee";
    public static final String SNAP_PEER_NAME = "peer";
    public static final String SNAP_CALLS_NAME = "calls";
    public static final String SNAP_ANSWERER_NAME = "answerer";
    public static final String SNAP_CALLID_NAME = "callId";
    public static final String SNAP_LISTENS_NAME = "listens";
    public static final String SNAP_LISTENER_NAME = "listener";
    public static final String SNAP_CALLSTATUS_NAME = "status";

    public static final String LOG_BACKEND_NET_NAME  = "backEndNetLog";
    public static final String LOG_BACKEND_DEVICE_NAME = "backEndDeviceLog";
    public static final String LOG_BACKEND_CALL_NAME = "backEndCallLog";
    public static final String LOG_BACKEND_PHONE_NAME  ="backEndPhoneLog";
    public static final String LOG_TERMINAL_NET_NAME = "terminalNetLog";
    public static final String LOG_TERMINAL_DEVICE_NAME = "terminalDeviceLog";
    public static final String LOG_TERMINAL_CALL_NAME = "terminalCallLog";
    public static final String LOG_TERMINAL_PHONE_NAME = "terminalPhoneLog";
    public static final String LOG_TERMINAL_USER_NAME = "terminalUserLog";
    public static final String LOG_TERMINAL_AUDIO_NAME = "terminalAudioLog";
    public static final String LOG_TRANSACTION_NAME = "transactionLog";
    public static final String LOG_DEBUG_NAME = "debugLog";
    public static final String LOG_DBG_LEVEL_NAME = "dbgLevel";

    public static final String AUDIO_RTP_CODEC_NAME = "codec";
    public static final String AUDIO_RTP_DATARATE_NAME = "dataRate";
    public static final String AUDIO_RTP_PTIME_NAME = "PTime";
    public static final String AUDIO_RTP_AEC_DELAY_NAME = "aecDelay";

}
