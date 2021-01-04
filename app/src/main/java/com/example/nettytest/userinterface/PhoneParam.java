package com.example.nettytest.userinterface;

public class PhoneParam {
    public static final int CALL_SERVER_PORT = 10002;
//    public static final String CALL_SERVER_ADDRESS = "172.16.1.102";
//    public static final String CALL_SERVER_ADDRESS = "192.168.1.35";
    public static final String CALL_SERVER_ADDRESS = "127.0.0.1";
    public static final String CALL_SERVER_ID = "FFFFFFFF";
//    public static final String LOCAL_ADDRESS = "172.16.1.30";
    public static final String LOCAL_ADDRESS = "127.0.0.1";
    public static final String BROAD_ADDRESS = "255.255.255.255";
    public static final int CLIENT_REG_EXPIRE = 3600;

    public static final int RTP_CODEC_711MU = 0;
    public static final int RTP_CODEC_711A = 8;
    public static final int RTP_CODEC_729A = 18;

    public static final int CALL_RTP_PORT = 9090;
    public static final int CALL_RTP_CODEC = RTP_CODEC_711MU;
    public static final int CALL_RTP_PTIME = 20;

}
