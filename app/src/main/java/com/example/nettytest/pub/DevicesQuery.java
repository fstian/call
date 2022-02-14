package com.example.nettytest.pub;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.UserArea;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DevicesQuery {
	
    final static String JSON_STATUS_NAME = "status";
    final static String JSON_RESULT_NAME = "result";
    final static String JSON_LIST_NAME = "list";
    final static String JSON_ZONE_NAME_NAME = "zoneName";
    final static String JSON_ZONE_ID_NAME = "zoneId";
    
    final static String JSON_DEVICE_TYPE_NAME = "deviceType";
    final static String JSON_DEVICE_ID_NAME = "deviceID";
    final static String JSON_BED_NAME_NAME = "bedName";

    final static String JSON_DEVICE_NAME_NAME = "deviceName";
    final static String JSON_ROOM_ID_NAME = "roomID";
    final static String JSON_ROOM_NAME_NAME = "roomName";

    final static String JSON_PARAM_NORMALCALLTOBED = "normalCallToBed";
    final static String JSON_PARAM_NORMALCALLTOROOM = "normalCallToRoom";
    final static String JSON_PARAM_NORMALCALLTOTV = "normalCallToTV";
    final static String JSON_PARAM_NORMALCALLTOCORRIDOR = "normalCallToCorridor";

    final static String JSON_PARAM_EMERCALLTOBED = "emerCallToBed";
    final static String JSON_PARAM_EMERCALLTOROOM = "emerCallToRoom";
    final static String JSON_PARAM_EMERCALLTOTV = "emerCallToTV";
    final static String JSON_PARAM_EMERCALLTOCORRIDOR = "emerCallToCorridor";

    final static String JSON_PARAM_BROADCALLTOBED = "broadCallToBed";
    final static String JSON_PARAM_BROADCALLTOROOM = "broadCallToRoom";
    final static String JSON_PARAM_BROADCALLTOTV = "broadCallToTV";
    final static String JSON_PARAM_BROADCALLTOCORRIDOR = "broadCallToCorridor";

    final static String JSON_PARAM_PARAM_VALUE = "param_val";
    final static String JSON_PARAM_PARAM_ID = "param_id";

    final static int JSON_STATUS_OK = 200;
    
    static final int QUERY_TIMER_TICK = 1;
    static final int QUERY_AREAS_RES = 2;
    static final int QUERY_DEVICES_RES = 3;
    static final int QUERY_PARAMS_RES = 4;
    static final int QUERY_FAIL_REPORT = 100;
    static final int QUERY_UNKNOW_MSG = 200;
    
    static final int QUERY_STATE_IDLE = 0;
    static final int QUERY_STATE_AREAS = 1;
    static final int QUERY_STATE_DEVICES = 2;
    static final int QUERY_STATE_PARAMS = 3;
    
    static final int QUERY_RETRY_TIME = 10;

    static final int QUERY_RETRY_MAX = 5;
    
    int state = QUERY_STATE_IDLE;
    int areaPos = 0;
    int tickCount = QUERY_RETRY_TIME+5;
    int retryCount = 0;

    int totalDeviceNum = 0;
    
    ArrayList<UserArea> areas;
    
    OkHttpClient client;

    int msgQueurId = 100000;

    final ArrayList<QueryMessage> msgList ;

    String serviceAddress;
    int servicePort;
    
    class QueryMessage{
        int type;
        String res;
        String areaId;
        String areaName;
        int msgId;
        
        public QueryMessage() {
            type = QUERY_UNKNOW_MSG;
            res = "";
            areaId = "";
            areaName = "";
            synchronized(QueryMessage.class){            
                msgQueurId++;
                msgId = msgQueurId;
            }
        }
        
    }

    public DevicesQuery(String address,int port) {
        msgList = new ArrayList<>();
        areas = new ArrayList<>();
        
        client = null;

        serviceAddress = address;
        servicePort = port;
    }
        
    private void ResetQuery() {
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Reset Query Process!!!!!!!!!!!!!!!!!!!!!");
        state = QUERY_STATE_IDLE;
        areaPos = 0;
        tickCount = 0;  
        retryCount = 0;
        totalDeviceNum = 0;
    }
    
    private void BeginQuery() {
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Begin Query Process!!!!!!!!!!!!!!!!!!!!!");
        state = QUERY_STATE_AREAS;
        areaPos = 0;
        tickCount = 0;
        retryCount = 0;
        totalDeviceNum = 0;
        QueryAreas();
    }


    public void StartQuery() {
        
        new Timer("QueryTimer").schedule(new TimerTask() {

            @Override
            public void run() {
                synchronized(msgList) {
                    
                    if(msgList.size()<5000) {
                        QueryMessage msg = new QueryMessage();
                        msg.type = QUERY_TIMER_TICK;
                        msgList.add(msg);
                        msgList.notify();
                    }
                }
            }
            
        }, 0,1000);
        
        new Thread("DevQuery") {
            @Override
            public void run() {
                ArrayList<QueryMessage> list = new ArrayList<>();
                QueryMessage msg;
                while(!isInterrupted()) {
                    synchronized(msgList) {
                        try {
                            msgList.wait();
                            while(msgList.size()>0) {
                                msg = msgList.remove(0);
                                list.add(msg);
                            }                                                                                          
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            break;
                        }                        
                    }

                    while(list.size()>0) {
                        msg = list.remove(0);
                        ProcessQueryMessage(msg);
                    }
                }
                
            }
        }.start();
        BeginQuery();
    }
    
    private void ProcessQueryMessage(QueryMessage msg) {
        UserArea area;
        
        switch(state) {
        case QUERY_STATE_IDLE:
            if(msg.type==QUERY_TIMER_TICK) {
                tickCount++;
                if(tickCount>PhoneParam.serviceUpdateTime) {
                    tickCount = 0;
                    BeginQuery();
                }
            }
            break;
        case QUERY_STATE_AREAS:
            if(msg.type == QUERY_TIMER_TICK ) {
                tickCount++;
                if(tickCount>QUERY_RETRY_TIME) {
                    tickCount = 0;
                    retryCount++;
                    if(retryCount<QUERY_RETRY_MAX){
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Aears TimerOver %d time, Retry Query",retryCount);
                        QueryAreas();
                    }else{
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Aears TimerOver %d time, Reseet Query !!!!!",retryCount);
                        ResetQuery();
                    }
                }               
            }else if(msg.type == QUERY_AREAS_RES) {
                tickCount = 0;
                ArrayList<UserArea> list = UpdateAreas(msg.res);
                if(list!=null) {
                    areas.clear();
                    areas = list;
                    areaPos = 0;
                    retryCount = 0;
                    state = QUERY_STATE_DEVICES;
                    UserInterface.UpdateAreas(areas);
                    if(areaPos<areas.size()) {
                        area = areas.get(areaPos);
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Get  %d Aears ",areas.size());
//                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Begin Query Device in %d Aear %s",areaPos+1,area.areaId);
                        QueryDevices(area.areaId,area.areaName);
                    }else{
                        ResetQuery();
                    }
                }
            }
            
            break;
        case QUERY_STATE_DEVICES:
            if(msg.type == QUERY_TIMER_TICK) {
                tickCount++;
                if(tickCount>QUERY_RETRY_TIME) {
                    tickCount = 0;
                    retryCount++;
                    area = areas.get(areaPos);
                    if(retryCount<QUERY_RETRY_MAX){
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Device in Area %s TimerOver %d time, Retry Query",area.areaId,retryCount);
                        QueryDevices(area.areaId,area.areaName);
                    }else{
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Device in Area %s TimerOver %d time, Reset Query !!!!!!!!!!",area.areaId,retryCount);
                        ResetQuery();
                    }
                }
            }else if(msg.type == QUERY_DEVICES_RES) {
                tickCount = 0;

//                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Recv Devices Res For %d Area %s",areaPos+1,msg.areaId);
                int deviceNum = UpdateDevices(msg.areaId,msg.areaName,msg.res);
                retryCount = 0;
                if(deviceNum>=0) {
                    totalDeviceNum += deviceNum;                  
                }
                areaPos++;
                if(areaPos>=areas.size()) {
//                    area = areas.get(areas.size()-1);
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query %d Aears and Get %d Devices!!!!!!!!!!!!!!",areaPos,totalDeviceNum);
                    areaPos = 0;
                    area = areas.get(areaPos);
//                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Begin Query Params in %d Aear %s",areaPos+1,area.areaId);
                    QueryParams(area.areaId);
                    state = QUERY_STATE_PARAMS;
                }else {
                    area = areas.get(areaPos);
//                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Begin Query Device in %d Aear %s",areaPos+1,area.areaId);
                    QueryDevices(area.areaId,area.areaName);
                }
            }
            break;
        case QUERY_STATE_PARAMS:
            if(msg.type == QUERY_TIMER_TICK) {
                tickCount++;
                if(tickCount>QUERY_RETRY_TIME) {
                    tickCount = 0;
                    retryCount++;
                    area = areas.get(areaPos);
                    if(retryCount<QUERY_RETRY_MAX){
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Param in Area %s TimerOver %d time, Retry Query",area.areaId,retryCount);
                        QueryParams(area.areaId);
                    }else{
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Device in Param %s TimerOver %d time, Reset Query !!!!!!!!!!",area.areaId,retryCount);
                        ResetQuery();
                    }
                }
            }else if(msg.type == QUERY_PARAMS_RES){               
                tickCount = 0;
//                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Recv Params Res For Area %s",msg.areaId);
                
                UpdateParams(msg.areaId,msg.res);

                areaPos++;

                if(areaPos>=areas.size()){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Finish All Devices and Params Query of %d areas!!!!!!!!!!!",areas.size());
                    ResetQuery();
                }else{
                    area = areas.get(areaPos);
//                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Http Query Begin Query Param in %d Aear %s",areaPos+1,area.areaId);
                    QueryParams(area.areaId);
                }
            }
            break;
        }
    }
    
    private void httpGetProcess(String url,Callback cb) {
        if(client==null) {
            client =  new OkHttpClient.Builder()
            .addInterceptor(new Interceptor() {
                public Response intercept(Chain chain) throws IOException {  
                    Request request = chain.request();  
                    Response response = chain.proceed(request);  
                    return response;  
                }  
            })  
            .connectTimeout(4000, TimeUnit.MILLISECONDS)  
            .readTimeout(4000,TimeUnit.MILLISECONDS)  
            .writeTimeout(4000, TimeUnit.MILLISECONDS)
            .build();  
        }

        final Request req = new Request.Builder().url(url).get().build();

        Call call = client.newCall(req);

        call.enqueue(cb);
   	
    }
    
    private void QueryAreas() {
        final String url = String.format("http://%s:%d/call/router/areas", serviceAddress, servicePort);
        areaPos = 0;
        
        httpGetProcess(url,new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_ERROR, String.format("Send %s Fail!!!!",url));
            }

            @Override
            public void onResponse(Call c, Response res) {
              
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG, String.format("Send %s And Recv Res !!!!",url));

                String resValue;
                
                if(res.code()==200) {
                    try {
//                        resValue =res.body().string();
                        resValue =new String(res.body().bytes(),"UTF-8");
                        QueryMessage msg = new QueryMessage();
                        msg.type = QUERY_AREAS_RES;
                        msg.res = resValue;
                        
                        synchronized(msgList) {
                            msgList.add(msg);
                            msgList.notify();   
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 
                }
                res.close();
            }        	
        });
    }

    private void QueryDevices(final String areaId,final String areaName) {
        final String url = String.format("http://%s:%d/call/router/area/%s/device",serviceAddress,servicePort,areaId);

        httpGetProcess(url,new Callback() {

            @Override
            public void onFailure(Call c, IOException e) {
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_ERROR, String.format("Recv Fail %s of Req %s in Thread %d!!!!",e.getMessage(),url,Thread.currentThread().getId()));

            }

            @Override
            public void onResponse(Call c, Response res){
                String resString;
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG, String.format("Recv Res for Req %s in Thread %d !!!!",url,Thread.currentThread().getId()));
                if(res.code()==200) {
                    try {
                        resString = res.body().string();
//                        resString =new String(res.body().bytes(),"UTF-8");
                        QueryMessage msg = new QueryMessage();
                        msg.type = QUERY_DEVICES_RES;
                        msg.res = resString;
                        msg.areaId = areaId;
                        msg.areaName = areaName;
                        
                        synchronized(msgList) {
                            msgList.add(msg);
                            msgList.notify();   
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                res.close();
                

            }

        });
    }

    private void QueryParams(final String areaId) {
        final String url = String.format("http://%s:%d/call/router/area/%s/params",serviceAddress,servicePort,areaId);

        httpGetProcess(url,new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_ERROR, String.format("Recv Fail %s of Req %s in Thread %d!!!!",e.getMessage(),url,Thread.currentThread().getId()));
            }            
            @Override
            public void onResponse(Call c, Response res) {
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG, String.format("Recv Res for Req %s in Thread %d !!!!",url,Thread.currentThread().getId()));
            	
                if(res.code()==200) {
                    try {
                        String resValue =res.body().string();
//                        String resValue =new String(res.body().bytes(),"UTF-8");
                        QueryMessage msg = new QueryMessage();
                        msg.type = QUERY_PARAMS_RES;
                        msg.res = resValue;
                        msg.areaId = areaId;
                        
                        synchronized(msgList) {
                            msgList.add(msg);
                            msgList.notify();   
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } 
                }
                res.close();                
//                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,String.format("Get %d params for area %s From Res !!!!",result,areaId));
            }
        });
        
    }


    private boolean UpdateCallParam(CallParams param,JSONObject jsonObj){

        String paramId;
        int paramVal;
        
        paramId = JsonPort.GetJsonString(jsonObj, JSON_PARAM_PARAM_ID);
        paramVal = jsonObj.getIntValue(JSON_PARAM_PARAM_VALUE);

        if(paramId.compareToIgnoreCase(JSON_PARAM_NORMALCALLTOBED)==0) {
            param.normalCallToBed = paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_NORMALCALLTOROOM)==0) {
            param.normalCallToRoom = paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_NORMALCALLTOTV)==0) {
            param.normalCallToTV = paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_NORMALCALLTOCORRIDOR)==0) {
            param.normalCallToCorridor= paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_EMERCALLTOBED)==0) {
            param.emerCallToBed = paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_EMERCALLTOROOM)==0) {
            param.emerCallToRoom= paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_EMERCALLTOTV)==0) {
            param.emerCallToTV= paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_EMERCALLTOCORRIDOR)==0) {
            param.emerCallToCorridor= paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_BROADCALLTOBED)==0) {
            param.boardCallToBed = paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_BROADCALLTOROOM)==0) {
            param.boardCallToRoom= paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_BROADCALLTOTV)==0) {
            param.boardCallToTV= paramVal != 0;
        }else if(paramId.compareToIgnoreCase(JSON_PARAM_BROADCALLTOCORRIDOR)==0) {
            param.boardCallToCorridor= paramVal != 0;
        }

        return true;
    }
    
    private int UpdateParams(String areaId,String data) {
        int result = -1;
        int status;
        int iTmp;
        JSONObject json;
        JSONObject resultJson;
        JSONArray paramList;
        JSONObject param;

        try{
            json=JSONObject.parseObject(data);
            if(json==null)
                return -1;
        
            status = json.getIntValue(JSON_STATUS_NAME);
            if(JSON_STATUS_OK == status) {
                resultJson = json.getJSONObject(JSON_RESULT_NAME);
                if(resultJson==null)
                    return -2;
                paramList = resultJson.getJSONArray(JSON_LIST_NAME);
                if(paramList==null)
                    return -3;
                result = 0;
                CallParams callParam = new CallParams();
                for(iTmp=0;iTmp<paramList.size();iTmp++){
                    param = paramList.getJSONObject(iTmp);
                    if(UpdateCallParam(callParam,param)){
                        result++;
                    }
                }
                UserInterface.UpdateAreaParam(areaId, callParam);
            }
            json.clear();
        }catch(JSONException e){
            e.printStackTrace();
        }

        return result;
    }

    
    private int UpdateDevices(String areaId,String areaName,String data) {
        int status;
        int iTmp;
        JSONObject json;
        JSONObject result;
        JSONArray deviceList;
        int hasNameCount = 0;
        int noNameCount = 0;
        int bedNum = 0;

        int hasRoomCount = 0;
        int noRoomCount = 0;
        int emerNum = 0;

        ArrayList<UserDevice> userDeviceList = new ArrayList<>();
        ArrayList<ServerDeviceInfo> deviceInfoList = new ArrayList<>();

        try{
            json = JSONObject.parseObject(data);
            if(json==null)
                return -1;
            status = json.getIntValue(JSON_STATUS_NAME);
            if(JSON_STATUS_OK == status) {
                result = json.getJSONObject(JSON_RESULT_NAME);
                if(result==null)
                    return -2;
                deviceList = result.getJSONArray(JSON_LIST_NAME);
                if(deviceList==null)
                    return -3;
                for(iTmp = 0; iTmp<deviceList.size();iTmp++) {
                    UserDevice device = new UserDevice();
                    ServerDeviceInfo deviceInfo = new ServerDeviceInfo();
                    JSONObject jsonDevice = deviceList.getJSONObject(iTmp);
                    int netMode = UserInterface.NET_MODE_TCP;

                    device.type = jsonDevice.getIntValue(JSON_DEVICE_TYPE_NAME);
                    device.devid = JsonPort.GetJsonString(jsonDevice,JSON_DEVICE_ID_NAME);
                    device.bedName = JsonPort.GetJsonString(jsonDevice,JSON_BED_NAME_NAME);
                    device.roomId =JsonPort.GetJsonString(jsonDevice,JSON_ROOM_ID_NAME);
                    device.roomName = JsonPort.GetJsonString(jsonDevice,JSON_ROOM_NAME_NAME);

                    if(device.type == UserInterface.CALL_BED_DEVICE){
                        bedNum++;
                        if(device.bedName.isEmpty()) {
                            noNameCount++;
                            if(!PhoneParam.bedSupportNoName){
                                continue;
                            }
                        }else {
                            hasNameCount++;
                        }
                    }

                    if(device.type == UserInterface.CALL_EMERGENCY_DEVICE){
                        emerNum++;
                        if(device.roomId.isEmpty()){
                            noRoomCount++;
                            if(!PhoneParam.bedSupportNoName){
                                continue;
                            }
                        }else{
                            hasRoomCount++;
                        }
                    }

                    if(PhoneParam.emerUseUdp){
                        if(device.type==UserInterface.CALL_EMERGENCY_DEVICE){
                            netMode = UserInterface.NET_MODE_UDP;
                        }
                    }
                    device.netMode = netMode;
                    userDeviceList.add(device);
                    deviceInfo.areaId = areaId;
                    deviceInfo.areaName = areaName; 
                    deviceInfo.bedName = JsonPort.GetJsonString(jsonDevice,JSON_BED_NAME_NAME);
                    deviceInfo.deviceName = JsonPort.GetJsonString(jsonDevice,JSON_DEVICE_NAME_NAME);
                    deviceInfo.roomId = JsonPort.GetJsonString(jsonDevice,JSON_ROOM_ID_NAME);
                    deviceInfo.roomName = JsonPort.GetJsonString(jsonDevice,JSON_ROOM_NAME_NAME);
                    deviceInfoList.add(deviceInfo);

                }
                UserInterface.UpdateAreaDevices(areaId,userDeviceList,deviceInfoList);
                json.clear();
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG, String.format("Area %s Has %d bed, %d has Name, %d no Name!!!!",areaId,bedNum,hasNameCount,noNameCount));
//                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG, String.format("Area %s Has %d Emer, %d has Room, %d no Room!!!!",areaId,emerNum,hasRoomCount,noRoomCount));
                return deviceInfoList.size();
            }else {
                json.clear();
                return -100;
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
        return -100;
    }
    
    private ArrayList<UserArea> UpdateAreas(String data){
        JSONObject json;
        JSONObject result;
        JSONArray zoneList;
        JSONObject zone;
        UserArea areaInfo;
        int status;
        int iTmp;

        ArrayList<UserArea> areaList =null;

        try{
            json = JSONObject.parseObject(data);
            if(json!=null){
                status = json.getIntValue(JSON_STATUS_NAME);
                if(JSON_STATUS_OK == status) {
                    result = json.getJSONObject(JSON_RESULT_NAME);
                    if(result!=null){
                        zoneList = result.getJSONArray(JSON_LIST_NAME);
                        if(zoneList!=null){
                            areaList = new ArrayList<>();
                            for(iTmp=0;iTmp<zoneList.size();iTmp++) {
                                String zoneName;
                                String zoneId;
                                zone = zoneList.getJSONObject(iTmp);
                                zoneName = JsonPort.GetJsonString(zone,JSON_ZONE_NAME_NAME);
                                zoneId = JsonPort.GetJsonString(zone,JSON_ZONE_ID_NAME);
                                if(zoneId!=null&&!zoneId.isEmpty()) {
                                    areaInfo = new UserArea(zoneId,zoneName);
                                    areaList.add(areaInfo);
                                }
                            }
                        }
                    }
                }
                json.clear();
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
        return areaList;

    }
    
}
