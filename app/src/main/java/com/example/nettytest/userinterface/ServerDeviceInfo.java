package com.example.nettytest.userinterface;

public class ServerDeviceInfo {
    public String deviceName;
    public String bedName;
    public String roomId;
    public String roomName;
    public String areaId;
    public String areaName;

    public ServerDeviceInfo(){
        deviceName = "";
        bedName = "";
        roomId = "";
        roomName = "";
        areaId = "";
        areaName = "";
    }

    public ServerDeviceInfo(ServerDeviceInfo old){
        deviceName = old.deviceName;
        bedName = old.bedName;
        roomId = old.roomId;
        roomName = old.roomName;
        areaId = old.areaId;
        areaName = old.areaName;
    }

    public int CompareInfo(ServerDeviceInfo info){
        int rtn = 0;

        rtn = deviceName.compareToIgnoreCase(info.deviceName);
        if(rtn!=0)
            return rtn;

        rtn = bedName.compareToIgnoreCase(info.bedName);
        if(rtn!=0)
            return rtn;

        rtn = roomId.compareToIgnoreCase(info.roomId);
        if(rtn!=0)
            return rtn;

        rtn = roomName.compareToIgnoreCase(info.roomName);
        if(rtn!=0)
            return rtn;

        rtn = areaId.compareToIgnoreCase(info.areaId);
        if(rtn!=0)
            return rtn;

        rtn = areaName.compareToIgnoreCase(info.areaName);
        if(rtn!=0)
            return rtn;

        return rtn;
    }
}
