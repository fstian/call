package com.example.nettytest.backend.backendphone;

public class BackEndConfig {
    public static boolean normalCallToBed=false;
    public static boolean normalCallToRoom=true;
    public static boolean normalCallToTv=true;
    public static boolean normalCallToCorridor=true;

    public static boolean emerCallToBed=false;
    public static boolean emerCallToRoom=true;
    public static boolean emerCallToTv=true;
    public static boolean emerCallToCorridor=true;

    public static boolean broadCallToBed=true;
    public static boolean broadCallToRoom=true;
    public static boolean broadCallToTv=false;
    public static boolean broadCallToCorridor=true;

    public void Copy(BackEndConfig config){
        normalCallToBed = config.normalCallToBed;
        normalCallToRoom = config.normalCallToRoom;
        normalCallToTv = config.normalCallToTv;
        normalCallToCorridor = config.normalCallToCorridor;

        emerCallToBed = config.emerCallToBed;
        emerCallToRoom = config.emerCallToRoom;
        emerCallToTv = config.emerCallToTv;
        emerCallToCorridor = config.emerCallToCorridor;
    }
}
