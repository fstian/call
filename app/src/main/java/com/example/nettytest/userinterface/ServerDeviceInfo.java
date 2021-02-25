package com.example.nettytest.userinterface;

public class ServerDeviceInfo {
    public String deviceName;
    public String bedName;
    public String roomId;

    public ServerDeviceInfo(){
        deviceName = "";
        bedName = "";
        roomId = "";
    }

    public ServerDeviceInfo(ServerDeviceInfo old){
        deviceName = old.deviceName;
        bedName = old.bedName;
        roomId = old.roomId;
    }
}
