package com.example.nettytest.userinterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

import com.alibaba.fastjson.*;
import com.example.nettytest.pub.AudioMode;
import com.example.nettytest.pub.JsonPort;

public class PhoneParam {
    final static String JSON_SERVE_NAME = "server";
    final static String JSON_ADDRESS_NAME = "address";
    final static String JSON_PORT_NAME = "port";
    final static String JSON_LOG_PORT_NAME = "logPort";
    final static String JSON_ACTIVE_NAME = "active";
    final static String JSON_SNAP_PORT_NAME = "snapPort";
    final static String JSON_NET_MODE_NAME = "netMode";
    final static String JSON_UDP_MODE_NAME = "UDP";
    final static String JSON_TCP_MODE_NAME = "TCP";
    final static String JSON_RAWTCP_MODE_NAME = "RAW-TCP";
    final static String JSON_SERVICE_NAME = "service";
    final static String JSON_UPDATE_TIME_NAME = "updateTime";
    
    final static String JSON_AREAS_NAME = "areas";
    final static String JSON_AREA_NAME_NAME = "areaName";
    final static String JSON_START_ID_NAME = "startId";
    final static String JSON_NUM_NAME = "num";

    final static String JSON_TESTAREAS_NAME = "testAreas";
    final static String JSON_AREA_START_NAME = "startArea";
    final static String JSON_AREA_NUM_NAME = "areaNum";
    final static String JSON_BED_NUM_NAME = "bedNum";
    final static String JSON_DOOR_NUM_NAME = "doorNum";
    final static String JSON_NURSER_NUM_NAME = "nurserNum";
    final static String JSON_EMER_USE_UDP_NAME = "emerUseUdp";
    final static String JSON_BED_SUPPORT_NONAME_NAME = "bedSupportNoName";
    

    final static String JSON_CLIENT_NAME = "client";

    final static String JSON_DEVICES_NAME = "devices";
    final static String JSON_DEVICES_ID_NAME = "id";
    final static String JSON_DEVICE_TYPE_NAME = "type";
    final static String JSON_AREA_ID_NAME = "area";
    final static String JSON_LOAD_MODE_NAME = "loadMode";

    final public static String CALL_SERVER_ID = "FFFFFFFF";
    final public static String BROAD_ADDRESS = "255.255.255.255";
    final public static int CLIENT_REG_EXPIRE = 60;

    final public static int INVITE_CALL_RTP_PORT = 9090;
    final public static int ANSWER_CALL_RTP_PORT = 9092;
    final public static int BROADCAST_CALL_RTP_PORT = 9094;

    final public static int DEFAULT_AEC_DELAY = 40;

    final public static int BROADCALL_ANSWER_WAIT = 3;
    
    final public static int BROADCALL_USE_UNICAST = 1;
    final public static int BROADCALL_USE_BROADCAST = 2;

    final static String JSON_SERVER_ADDRESS_NAME = "address";
    final static String JSON_SERVER_PORT_NAME = "port";
    final static String JSON_SERVER_ACTIVE_NAME = "active";

    public final static int UDP_PROTOCOL = 1;
    public final static int TCP_PROTOCOL = 2;
    public final static int RAW_TCP_PROTOCOL = 3;

    public final static int AUDIO_PROCESS_DISABLE = 0;
    public final static int AUDIO_PROCESS_ENABLE = 1;
    public final static int AUDIO_PROCESS_MILD = 1;
    public final static int AUDIO_PROCESS_MEDIUM = 2;
    public final static int AUDIO_PROCESS_HIGH = 3;
    public final static int AUDIO_PROCESS_AGGRESSIVE= 4;
    public final static int AUDIO_PROCESS_MOST_AGGRESSIVE= 5;

    public final static int AUDIO_INPUT_MIC = 0;
    public final static int AUDIO_INPUT_COMMUNICATION = 1;
    public final static int AUDIO_INPUT_CALL = 2;
    public final static int AUDIO_INPUT_CAMCORDER = 3;
    public final static int AUDIO_INPUT_DEFAULT = 4;

    public final static int AUDIO_OUTPUT_MUSIC = 0;
    public final static int AUDIO_OUTPUT_CALL = 1;
    public final static int AUDIO_OUTPUT_SYSTEM = 2;

    public final static int AUDIO_MODE_NORMAL = 0;
    public final static int AUDIO_MODE_COMMUNICATION = 1;
    public final static int AUDIO_MODE_CALL = 2;
    public final static int AUDIO_MODE_CALL_SCREENING = 3;
    

    public static int callRtpCodec = AudioMode.RTP_CODEC_711A;

    public static ArrayList<UserDevice> devicesOnServer = new ArrayList<>();
    public static ArrayList<UserDevice> deviceList = new ArrayList<>();

    public static int callServerPort = 10002;
    public static int callClientPort = 10002;
    public static String callServerAddress = "127.0.0.1";
    public static boolean serverActive = false;
    public static boolean clientActive = false;
    public static boolean emerUseUdp = false;
    public static int broadcallCastMode = BROADCALL_USE_BROADCAST;
    
    public static boolean serviceActive = false;
    public static String serviceAddress = "127.0.0.1";
    public static int servicePort = 80;
    public static int logPort=9996;
    public static int serviceUpdateTime = 120;
    
    public static int aecDelay = DEFAULT_AEC_DELAY;
    public static int aecMode = AUDIO_PROCESS_MEDIUM;
    public static int nsMode = AUDIO_PROCESS_MEDIUM;
    public static int nsThreshold =30;
    public static int nsRange = 200;
    public static int nsTime = 100;
    public static int agcMode = AUDIO_PROCESS_DISABLE;

    public static int inputMode = AUDIO_INPUT_COMMUNICATION;
    public static int inputGain = 0;
    public static int outputMode = AUDIO_OUTPUT_MUSIC;
    public static int outputGain = 0;

    public static int audioMode = AUDIO_MODE_NORMAL;
    public static int audioSpeaker = 1;
    
    public static int callRtpPTime = 20;
    public static int callRtpDataRate = 8000;

    public static int snapStartPort = 11005;
    public static int transferMaxNum = 2;

    public static boolean bedSupportNoName = true;

    public final static int SNAP_MMI_GROUP = 1;
    public final static int SNAP_TERMINAL_GROUP = 2;
    public final static int SNAP_BACKEND_GROUP = 3;
    public final static int SNAP_PORT_INTERVAL = 5;

    public static int DEFAULT_SNAP_PORT = 11004;

    public final static String VER_STR = "1.2.4";

    public static String localAddress = "";

    static private UserDevice CheckUserDevice(JSONObject device){
        UserDevice userdev = new UserDevice();
        String netMode;
        
        userdev.devid = JsonPort.GetJsonString(device,JSON_DEVICES_ID_NAME);
        userdev.areaId = JsonPort.GetJsonString(device,JSON_AREA_ID_NAME);
        netMode = JsonPort.GetJsonString(device,JSON_NET_MODE_NAME);
        if(netMode.compareToIgnoreCase(JSON_UDP_MODE_NAME)==0)
            userdev.netMode = UserInterface.NET_MODE_UDP;
        else if(netMode.compareToIgnoreCase(JSON_RAWTCP_MODE_NAME)==0)
            userdev.netMode = UserInterface.NET_MODE_RAW_TCP;
        else
            userdev.netMode = UserInterface.NET_MODE_TCP;
        userdev.type = UserInterface.GetDeviceType(JsonPort.GetJsonString(device,JSON_DEVICE_TYPE_NAME));

        return userdev;
    }

    static private ArrayList<UserDevice> CheckTestAreas(JSONObject testAreas){
        ArrayList<UserDevice> list = new ArrayList<>();
        int iTmp;
        int jTmp;
        String areaId;
        String devId;
        UserDevice dev;

        String startArea = JsonPort.GetJsonString(testAreas, JSON_AREA_START_NAME);
        int startAreaValue = Integer.parseInt(startArea);
        int areaNum = testAreas.getIntValue(JSON_AREA_NUM_NAME);
        int bedNum = testAreas.getIntValue(JSON_BED_NUM_NAME);
        int doorNum = testAreas.getIntValue(JSON_DOOR_NUM_NAME);
        int nurserNum = testAreas.getIntValue(JSON_NURSER_NUM_NAME);
        int netMode;
        String netModeValue = JsonPort.GetJsonString(testAreas, JSON_NET_MODE_NAME);
        if(netModeValue.compareToIgnoreCase(JSON_UDP_MODE_NAME)==0)
            netMode = UserInterface.NET_MODE_UDP;
        else if(netModeValue.compareToIgnoreCase(JSON_RAWTCP_MODE_NAME)==0)
            netMode = UserInterface.NET_MODE_RAW_TCP;
        else
            netMode = UserInterface.NET_MODE_TCP;
        
        for(iTmp=0;iTmp<areaNum;iTmp++) {
            areaId = ""+(startAreaValue+iTmp);
            for(jTmp=0;jTmp<bedNum;jTmp++) {
                devId = ""+((startAreaValue+iTmp)*1000+jTmp+1);
                dev = new UserDevice();
                dev.devid = devId;
                dev.areaId = areaId;
                dev.type = UserInterface.CALL_BED_DEVICE;
                dev.netMode = netMode;
                list.add(dev);
            }            
            for(jTmp=0;jTmp<doorNum;jTmp++) {
                devId = ""+((startAreaValue+iTmp)*1000+100+jTmp+1);
                dev = new UserDevice();
                dev.devid = devId;
                dev.areaId = areaId;
                dev.type = UserInterface.CALL_DOOR_DEVICE;
                dev.netMode = netMode;
                list.add(dev);
            }            
            for(jTmp=0;jTmp<nurserNum;jTmp++) {
                devId = ""+((startAreaValue+iTmp)*1000+200+jTmp+1);
                dev = new UserDevice();
                dev.devid = devId;
                dev.areaId = areaId;
                dev.type = UserInterface.CALL_NURSER_DEVICE;
                dev.netMode = netMode;
                list.add(dev);
            }            
        }
        
        return list;
    }

    static private ArrayList<UserDevice> CheckUserDeviceGroup(String areaId,JSONObject devGroupJson){
        ArrayList<UserDevice> list = new ArrayList<>();
        String sValue;
        long beginId;
        int num ;
        int type ;
        int netMode ;
        int iTmp;
        sValue = JsonPort.GetJsonString(devGroupJson,JSON_START_ID_NAME);
        beginId = Integer.parseInt(sValue);
        num = devGroupJson.getIntValue(JSON_NUM_NAME);
        sValue = JsonPort.GetJsonString(devGroupJson,JSON_NET_MODE_NAME);
        if(sValue.compareToIgnoreCase(JSON_UDP_MODE_NAME)==0)
            netMode = UserInterface.NET_MODE_UDP;
        else if(sValue.compareToIgnoreCase(JSON_RAWTCP_MODE_NAME)==0)
            netMode = UserInterface.NET_MODE_RAW_TCP;
        else
            netMode = UserInterface.NET_MODE_TCP;
        type = UserInterface.GetDeviceType(JsonPort.GetJsonString(devGroupJson,JSON_DEVICE_TYPE_NAME));

        for(iTmp=0;iTmp<num;iTmp++) {
            UserDevice dev = new UserDevice();
            dev.areaId = areaId;
            dev.devid = String.format("%d", beginId+iTmp);
            dev.type = type;
            dev.netMode = netMode;
            list.add(dev);
        }
        return list;
    }

    static void InitServerAndDevicesConfig(String info){
        JSONObject json;
        JSONObject serviceJson;
        JSONObject serverJson;
        JSONObject clientJson;
        JSONArray devicesJson;
        JSONObject deviceGroupJson;
        JSONObject device;
        JSONArray areasJson;
        JSONObject areaJson;
        JSONObject testAreas;
        UserDevice userdev;
        int iTmp,jTmp;

        try {
            json = JSONObject.parseObject(info);
            if(json==null)
                return ;
            snapStartPort = json.getIntValue(JSON_SNAP_PORT_NAME);
            serverJson = json.getJSONObject(JSON_SERVE_NAME);
            serviceJson = json.getJSONObject(JSON_SERVICE_NAME);
            clientJson = json.getJSONObject(JSON_CLIENT_NAME);

            if(serviceJson!=null){
                serviceAddress = JsonPort.GetJsonString(serviceJson,JSON_ADDRESS_NAME);
                servicePort = serviceJson.getIntValue(JSON_PORT_NAME);
                logPort =serviceJson.getIntValue(JSON_LOG_PORT_NAME);
                serviceActive = serviceJson.getBooleanValue(JSON_ACTIVE_NAME);
                serviceUpdateTime = serviceJson.getIntValue(JSON_UPDATE_TIME_NAME);
                bedSupportNoName = clientJson.getBooleanValue(JSON_BED_SUPPORT_NONAME_NAME);
                if(serviceUpdateTime==0)
                    serviceUpdateTime = 120;
                if(logPort==0)
                    logPort = 9996;
            }

            if(serverJson!=null){
                callServerPort = serverJson.getIntValue(JSON_PORT_NAME);
                serverActive = serverJson.getBooleanValue(JSON_ACTIVE_NAME);
                emerUseUdp = serverJson.getBooleanValue(JSON_EMER_USE_UDP_NAME);
                devicesJson = serverJson.getJSONArray(JSON_DEVICES_NAME);
                areasJson = serverJson.getJSONArray(JSON_AREAS_NAME);
                testAreas = serverJson.getJSONObject(JSON_TESTAREAS_NAME);

                devicesOnServer.clear();
                if(devicesJson!=null&&serverActive&&!serviceActive){
                    for(iTmp=0;iTmp<devicesJson.size();iTmp++){
                        device = devicesJson.getJSONObject(iTmp);
                        userdev = CheckUserDevice(device);
                        devicesOnServer.add(userdev);
                    }
                }
                if(areasJson!=null&&serverActive&&!serviceActive){
                    for(iTmp=0;iTmp<areasJson.size();iTmp++){
                        areaJson = areasJson.getJSONObject(iTmp);
                        String areaId = JsonPort.GetJsonString(areaJson,JSON_AREA_ID_NAME);
                        String areaName = JsonPort.GetJsonString(areaJson,JSON_AREA_NAME_NAME);
                        UserInterface.AddAreaInfoOnServer(areaId,areaName);
                        devicesJson = areaJson.getJSONArray(JSON_DEVICES_NAME);       
                        if(devicesJson!=null){
                            for(jTmp=0;jTmp<devicesJson.size();jTmp++){
                                deviceGroupJson = devicesJson.getJSONObject(jTmp);
                                ArrayList<UserDevice> devGropu = CheckUserDeviceGroup(areaId,deviceGroupJson);
                                devicesOnServer.addAll(devGropu);
                            }
                        }
                    }
                }
                
                if(testAreas!=null&&serverActive&&!serviceActive) {
                    ArrayList<UserDevice> devTest = CheckTestAreas(testAreas);
                    devicesOnServer.addAll(devTest);
                }
            }

            if(clientJson!=null){
                callClientPort = clientJson.getIntValue(JSON_PORT_NAME);
                callServerAddress = JsonPort.GetJsonString(clientJson,JSON_ADDRESS_NAME);
                devicesJson = clientJson.getJSONArray(JSON_DEVICES_NAME);
                areasJson = clientJson.getJSONArray(JSON_AREAS_NAME);
                clientActive = clientJson.getBooleanValue(JSON_ACTIVE_NAME);
                testAreas = clientJson.getJSONObject(JSON_TESTAREAS_NAME);
                
                deviceList.clear();
                if(devicesJson!=null&&clientActive){
                    for (iTmp = 0; iTmp < devicesJson.size(); iTmp++) {
                        device = devicesJson.getJSONObject(iTmp);
                        userdev = CheckUserDevice(device);
                        deviceList.add(userdev);
                    }
                }

                if(testAreas!=null&&clientActive) {
                    ArrayList<UserDevice> devTest = CheckTestAreas(testAreas);
                    deviceList.addAll(devTest);
                }

                if(areasJson!=null&&clientActive){
                    for(iTmp=0;iTmp<areasJson.size();iTmp++){
                        areaJson = areasJson.getJSONObject(iTmp);
                        String areaId = JsonPort.GetJsonString(areaJson,JSON_AREA_ID_NAME);
                        String areaName = JsonPort.GetJsonString(areaJson,JSON_AREA_NAME_NAME);
                        UserInterface.AddAreaInfoOnServer(areaId,areaName);
                        devicesJson = areaJson.getJSONArray(JSON_DEVICES_NAME);       
                        if(devicesJson!=null){
                            for(jTmp=0;jTmp<devicesJson.size();jTmp++){
                                deviceGroupJson = devicesJson.getJSONObject(jTmp);
                                ArrayList<UserDevice> devGropu = CheckUserDeviceGroup(areaId,deviceGroupJson);
                                deviceList.addAll(devGropu);
                            }
                        }
                    }
                }
            }
            json.clear();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void InitPhoneParam(String path,String fileName){

        UserInterface.PrintLog("-------------------------------> Config File is "+path+fileName);

        File configFile = new File(path, fileName);

        try {
            if (configFile.exists()) {
                FileInputStream finput = new FileInputStream(configFile);
                int len = finput.available();
                byte[] data = new byte[len];
                int readlen = finput.read(data);
                finput.close();
                if(readlen>0) {
                    String config = new String(data, "UTF-8");
                    InitServerAndDevicesConfig(config);
                }
            } else {
                FileOutputStream foutput = new FileOutputStream(configFile);
                String config = "hello";
                byte[] data = config.getBytes();
                foutput.write(data);
                foutput.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String GetLocalAddress(){
        String address= "";
        String curAddress;
        int iMatchNum = 0;
        String[] serverIp;
        String prio1Name="wlan";
        String prio2Name="eth0";
        byte[] macAddress;

        serverIp = callServerAddress.split("\\.");
        int len = serverIp.length;

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address))
                    {
                        curAddress =  inetAddress.getHostAddress();
                        String[] localAddress = curAddress.split("\\.");
                        if(serverIp.length==4&&localAddress.length==4) {
                            int curMatched = 0;
                            if(intf.getName().contains(prio1Name)){
                                curMatched = 1;
                                macAddress = intf.getHardwareAddress();
                            }else if(intf.getName().contains(prio2Name)){
                                curMatched = 1;
                                macAddress = intf.getHardwareAddress();
                            }
                            if(serverIp[0].compareToIgnoreCase(localAddress[0])==0){
                                curMatched = 1;
                                if(serverIp[1].compareToIgnoreCase(localAddress[1])==0){
                                    curMatched = 2;
                                    if(serverIp[2].compareToIgnoreCase(localAddress[2])==0){
                                        curMatched = 3;
                                    }
                                }
                            }
                            if(curMatched>iMatchNum){
                                iMatchNum = curMatched;
                                address = curAddress;
                            }
                        }else {
                            if(iMatchNum==0)
                                address=curAddress;
                        }
                    }
                }
            }
        }        catch (SocketException ex){
            ex.printStackTrace();
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        if(!address.isEmpty()&&address.compareToIgnoreCase("0.0.0.0")!=0)
            localAddress = address;
        else if(!localAddress.isEmpty())
            address = localAddress;
        
        return address;
    }

    public static void ResetLocalAddress(){
        localAddress = "";
    }

}


