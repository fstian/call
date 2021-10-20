package com.mysqltest;

public class SqlAreaInfo {
    public static final String NETWORK_GROUP = "10.120.";

    public int floor;
    public int location;
    public String name;
    public String code;
    public int netWork;
    public int bedNum;
    public int doorNum;

    public SqlAreaInfo(){
        floor = 1;
        location = 1;
        name = "";
        code = "";
        netWork = 1;
        bedNum = 1;
        doorNum = 1;
    }

    public String CreateAreaCode(){
        return String.format("%02d%d",floor,location);
    }

    public String CreateBedCode(int bed){
        String bedCode;
        if(bed<50){
            bedCode = String.format("50%03d%03d",netWork,bed+201);
        }else{
            bedCode = String.format("50%03d%03d",netWork,bed+101);
        }
        return bedCode;
    }

    public String CreateBedDevCode(int bed){
        return CreateBedCode(bed);
    }

    public String CreateEmergencyCode(int bed){
        String bedCode;
        if(bed<50){
            bedCode = String.format("56%03d%03d",netWork,bed+201);
        }else{
            bedCode = String.format("56%03d%03d",netWork,bed+101);
        }
        return bedCode;
    }

    public String CreateEmergencyDevCode(int bed){
        return CreateEmergencyCode(bed);
    }

    public String CreateDoorCode(int bed){
        String doorCode;
        doorCode = String.format("51%03d%03d",netWork,bed+161);
        return doorCode;
    }

    public String CreateDoorDevCode(int bed){
        return CreateDoorCode(bed);
    }


    public String CreateBedDevName(int bed){
        String bedDevName = String.format("B_%s_%03d",CreateAreaCode(),(bed+1));

        return bedDevName;
    }

    public String CreateEmergencyDevName(int bed){
        String emergency = String.format("E_%s_%03d",CreateAreaCode(),(bed+1));
        return emergency;
    }

    public String CreateDoorDevName(int door){
        String bedDevName = String.format("D_%s_%03d",CreateAreaCode(),(door+1));

        return bedDevName;
    }

    public String CreatePatientName(int bed){
        String patientName = String.format("测试%02d",bed+1);
        return patientName;
    }

    public String CreatePatientCode(int bed){
        return CreateBedDevCode(bed);
    }

    public String GetTVCode(){
        return String.format("5%02d%1d3001",floor,location);
    }

    public String GetCorridorCode(){
        return String.format("5%02d%1d4001",floor,location);
    }

    public String GetPhoneCode(){
        return String.format("5%02d%1d5001",floor,location);
    }

    public String GetDefaultRoute(){
        return CreateAreaCode()+"FFFFFF";
    }

//room of bed is unknow
    public String CreateRoomOfBed(int bed){
        String roomCode;
        int everyBedInRoom = bedNum/doorNum;
        if(everyBedInRoom<1)
            everyBedInRoom = 1;
        if(bed<everyBedInRoom*doorNum)
            roomCode = String.format("%d",bed/everyBedInRoom+1);
        else
            roomCode = String.format("%d",doorNum);

        return roomCode;
    }

}
