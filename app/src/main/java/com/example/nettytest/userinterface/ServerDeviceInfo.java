package com.example.nettytest.userinterface;

public class ServerDeviceInfo {
    public String deviceName;
    public String bedName;
    public String roomId;
    public String areaId;

    public ServerDeviceInfo(){
        deviceName = "";
        bedName = "";
        roomId = "";
        areaId = "";
    }

    public ServerDeviceInfo(ServerDeviceInfo old){
        deviceName = old.deviceName;
        bedName = old.bedName;
        roomId = old.roomId;
        areaId = old.areaId;
    }
}
