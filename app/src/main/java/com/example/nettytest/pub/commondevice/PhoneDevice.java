package com.example.nettytest.pub.commondevice;

public class PhoneDevice {
    public static final int BED_CALL_DEVICE = 2;
    public static final int DOOR_CALL_DEVICE = 6;
    public static final int NURSE_CALL_DEVICE = 9;
    public static final int TV_CALL_DEVICE = 8;
    public static final int CORRIDOR_CALL_DEVICE = 7;
    public static final int EMER_CALL_DEVICE = 4;
    public static final int DOOR_LIGHT_CALL_DEVICE = 5;
    public static final int WHITE_BOARD_DEVICE = 10;
    public static final int UNKNOW_CALL_DEVICE = 0xff;

    public String id;
    public String bedName;
    public int type;
    public boolean isReg;

    public PhoneDevice(){
        id = "";
        bedName = "";
        isReg = false;
        type = PhoneDevice.UNKNOW_CALL_DEVICE;
    }
}
