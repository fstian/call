package com.example.nettytest.pub;

import com.example.nettytest.userinterface.PhoneParam;

import java.net.DatagramSocket;
import java.net.SocketException;

public class SystemSnap {

    public static final int SNAP_TEST_REQ = 1;

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

    public static final int AUDIO_CONFIG_WRITE_REQ_CMD = 8;
    public static final int AUDIO_CONFIG_WRITE_RES_CMD = 108;

    public static final int SNAP_DEV_REQ = 9;
    public static final int SNAP_DEV_RES = 109;

    public static final int SNAP_SYSTEM_INFO_REQ = 11;
    public static final int SNAP_SYSTEM_INFO_RES = 111;

    public static final int SNAP_CLEAN_CALL_REQ = 12;
    public static final int SNAP_CLEAN_CALL_RES = 112;


    public static final int AUDIO_CONFIG_READ_REQ_CMD = 13;
    public static final int AUDIO_CONFIG_READ_RES_CMD = 113;

    public static final int SNAP_DEL_LOG_REQ = 10;

    public static final String SNAP_AUTOTEST_NAME = "autoTest";
    public static final String SNAP_REALTIME_NAME = "realTime";
    public static final String SNAP_TIMEUNIT_NAME = "timeUnit";
    public static final String SNAP_TEST_MODE_NAME = "testMode";

    public static final String SNAP_CMD_TYPE_NAME = "type";
    public static final String SNAP_AREAID_NAME = "areaId";
    public static final String SNAP_DEVID_NAME = "devId";
    public static final String SNAP_DEVTYPE_NAME = "devType";
    public static final String SNAP_TRANS_NAME = "transactions";
    public static final String SNAP_TRANSTYPE_NAME = "transType";
    public static final String SNAP_SENDER_NAME = "sender";
    public static final String SNAP_RECEIVER_NAME = "receiver";
    public static final String SNAP_MSGID_NAME = "msgId";
    public static final String SNAP_REG_NAME = "reg";
    public static final String SNAP_VER_NAME = "ver";
    public static final String SNAP_RUN_TIME_NAME = "runTime";
    public static final String SNAP_IP_ADDRESS = "address";

    public static final String SNAP_LISTEN_STATUS_NAME = "listenStatus";
    public static final String SNAP_TRANSFER_AREA_NAME = "transferAreaId";

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
    public static final String LOG_WIRTE_FILES_NAME = "logToFiles";
    public static final String LOG_FILE_INTERVAL_NAME = "logFileInterval";

    public static final String AUDIO_RTP_CODEC_NAME = "codec";
    public static final String AUDIO_RTP_DATARATE_NAME = "dataRate";
    public static final String AUDIO_RTP_PTIME_NAME = "PTime";
    public static final String AUDIO_AEC_DELAY_NAME = "aecDelay";
    public static final String AUDIO_AEC_MODE_NAME = "aecMode";
    public static final String AUDIO_NS_MODE_NAME = "nsMode";
    public static final String AUDIO_NS_THRESHOLD_NAME = "noiseThreshold";
    public static final String AUDIO_NS_RANGE_NAME = "noiseRange";
    public static final String AUDIO_NS_TIME_NAME = "noiseTime";
    public static final String AUDIO_AGC_MODE_NAME = "agcMode";
    public static final String AUDIO_AEC_DELAY_ESTIMATOR_NAME = "aecDelayEstimator";
    public static final String AUDIO_INPUT_MODE_NAME = "inputMode";
    public static final String AUDIO_INPUT_GAIN_NAME = "inputGain";
    public static final String AUDIO_OUTPUT_MODE_NAME = "outputMode";
    public static final String AUDIO_OUTPUT_GAIN_NAME = "outputGain";

    public static final String AUDIO_MODE_NAME = "audioMode";
    public static final String AUDIO_SPEAKER_NAME = "audioSpeaker";

    public static final String SNAP_INFO_CALLCONVERGENCE_NUM_NAME = "callConvergenceNum";
    public static final String SNAP_INFO_CALL_NUM_NAME = "callNum";
    public static final String SNAP_INFO_CLIENT_TRANS_NUM_NAME = "clientTransNum";
    public static final String SNAP_INFO_CLIENT_REGSUCC_NUM_NAME = "clientRegSuccNum";
    public static final String SNAP_INFO_CLIENT_REGFAIL_NUM_NAME = "clientRegFailNum";
    public static final String SNAP_INFO_CLIENT_CURAREA_REGSUCC_NUM_NAME = "clientCurAreaRegSuccNum";
    public static final String SNAP_INFO_CLIENT_CURAREA_REGFAIL_NUM_NAME = "clientCurAreaRegFailNum";
    
    public static final String SNAP_INFO_BACKEND_TRANS_NUM_NAME = "backEndTransNum";
    public static final String SNAP_INFO_BACKEND_REGSUCC_NUM_NAME = "backEndRegSuccNum";
    public static final String SNAP_INFO_BACKEND_REGFAIL_NUM_NAME = "backEndRegFailNum";
    public static final String SNAP_INFO_BACKEND_CURAREA_REGSUCC_NUM_NAME = "backEndCurAreaRegSuccNum";
    public static final String SNAP_INFO_BACKEND_CURAREA_REGFAIL_NUM_NAME = "backEndCurAreaRegFailNum";

    public static DatagramSocket OpenSnapSocket(int startPort,int group){
        DatagramSocket socket=null;
        int iTmp;
        int port ;
        if(startPort==0)
            startPort = 11005;
        port=startPort;
        switch(group){
            case PhoneParam.SNAP_MMI_GROUP:
                port = startPort;
                break;
            case PhoneParam.SNAP_TERMINAL_GROUP:
                port = startPort+PhoneParam.SNAP_PORT_INTERVAL;
                break;
            case PhoneParam.SNAP_BACKEND_GROUP:
                port = startPort+2*PhoneParam.SNAP_PORT_INTERVAL;
                break;
        }
        for(iTmp = 0;iTmp<=PhoneParam.SNAP_PORT_INTERVAL;iTmp++){
            socket = null;
            try {
                socket = new DatagramSocket(port+iTmp);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            if(socket!=null)
                break;
        }
        return socket;
    }

}
