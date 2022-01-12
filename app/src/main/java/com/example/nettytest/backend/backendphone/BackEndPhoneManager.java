package com.example.nettytest.backend.backendphone;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.nettytest.backend.backendcall.BackEndCallConvergenceManager;
import com.example.nettytest.pub.BackEndStatistics;
import com.example.nettytest.pub.CallParams;
import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.DeviceStatistics;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.JsonPort;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.MsgReceiver;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.phonecall.CommonCall;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.pub.protocol.ConfigReqPack;
import com.example.nettytest.pub.protocol.ConfigResPack;
import com.example.nettytest.pub.protocol.DevQueryReqPack;
import com.example.nettytest.pub.protocol.DevQueryResPack;
import com.example.nettytest.pub.protocol.ListenCallReqPack;
import com.example.nettytest.pub.protocol.ListenCallResPack;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.protocol.RegReqPack;
import com.example.nettytest.pub.protocol.RegResPack;
import com.example.nettytest.pub.protocol.SystemConfigReqPack;
import com.example.nettytest.pub.protocol.SystemConfigResPack;
import com.example.nettytest.pub.protocol.TransferReqPack;
import com.example.nettytest.pub.protocol.TransferResPack;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserInterface;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BackEndPhoneManager {

    public static final int MSG_NEW_PACKET = 1;
    public static final int MSG_SECOND_TICK = 2;
    public static final int MSG_REQ_TIMEOVER = 3;

    private final HashMap<String, BackEndZone> serverAreaLists;

    private ArrayList<ConfigItem> systemConfigList;
    static Thread snapThread = null;
    long runSecond;

    private final BackEndCallConvergenceManager backEndCallConvergencyMgr;

    BackEndPhoneMsgReceiver msgReceiver;
    
    MsgReceiver userMsgReceiver;

    boolean isSendingDevInfo = false;
    

    private class BackEndPhoneMsgReceiver extends MsgReceiver{
        public BackEndPhoneMsgReceiver(String name){
            super(name);
        }

        @Override
        public void CallPubMessageRecv(ArrayList<CallPubMessage> list) {
            CallPubMessage msg;
            ProtocolPacket packet;
            int type;

            synchronized (BackEndPhoneManager.class) {
                while(list.size()>0) {
                    msg = list.remove(0);
                    type = msg.arg1;
                    switch(type) {
                        case MSG_NEW_PACKET:
                            packet = (ProtocolPacket) msg.obj;
                            PacketRecvProcess(packet);
                            break;
                        case MSG_SECOND_TICK:
                            CallConvergencySecondTick();
                            UpdatePhonesRegTick();
                            break;
                        case MSG_REQ_TIMEOVER:
                            packet = (ProtocolPacket) msg.obj;
                            PacketTimeOverProcess(packet);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " +type);
                    }
                }
            }
        }
    }
    

    public BackEndPhoneManager(){
        serverAreaLists = new HashMap<>();
        systemConfigList = new ArrayList<>();
        
        userMsgReceiver = null;

        msgReceiver = new BackEndPhoneMsgReceiver("BackEndMsgReceiver");

        HandlerMgr.ReadSystemType();

        backEndCallConvergencyMgr = new BackEndCallConvergenceManager();

        new Timer("BackEndTimeTick").schedule(new TimerTask() {
            @Override
            public void run() {
                HandlerMgr.PostBackEndPhoneMsg(BackEndPhoneManager.MSG_SECOND_TICK,"");
                HandlerMgr.BackEndTransactionTick();
                runSecond++;
            }
        },0,1000);

    }

    public long GetRunSecond(){
        return runSecond;
    }

// only used in callconverce
    public BackEndPhone GetDevice(String id){
        BackEndPhone matchedPhone;
        matchedPhone = GetLocalDevice(id);
        return matchedPhone;

    }
    
    public int PostBackEndPhoneMessage(int type,Object obj) {
    	msgReceiver.AddMessage(type,obj);
    	return 0;
    }

    public boolean GetPhoneRegStatus(String id){
        boolean status = false;
        boolean isfound = false;
        synchronized(BackEndPhoneManager.class) {
            for(BackEndZone zone:serverAreaLists.values()) {
                for(BackEndPhone phone:zone.phoneList.values()) {
                    if(phone.id.compareToIgnoreCase(id)==0) {
                        status = phone.isReg;
                        isfound = true;
                        break;
                    }
                }
                if(isfound)
                    break;
            }
        }

        return status;
    }
    
    public DeviceStatistics GetRegStatistics() {
        DeviceStatistics statist = new DeviceStatistics();
        synchronized(BackEndPhoneManager.class) {
            for(BackEndZone zone:serverAreaLists.values()) {
                for(BackEndPhone phone:zone.phoneList.values()) {
                    if(phone.isReg)
                        statist.regSuccNum++;
                    else
                        statist.regFailNum++;
                }
            }
        }
        return statist;
    }
    
    public DeviceStatistics GetRegStatistics(String areaId) {
        DeviceStatistics statist = new DeviceStatistics();
        synchronized(BackEndPhoneManager.class) {
            for(BackEndZone zone:serverAreaLists.values()) {
                if(zone.areaId.compareToIgnoreCase(areaId)==0) {
                    for(BackEndPhone phone:zone.phoneList.values()) {
                        if (phone.isReg)
                            statist.regSuccNum++;
                        else
                            statist.regFailNum++;
                    }
                    break;
                }
            }
        }
        return statist;
    }

    public int GetCallCount(){
        return backEndCallConvergencyMgr.GetCallCount();
    }


    private String GetAreaId(String id){
        String areaId;
        if(id==null)
            areaId = BackEndZone.DEFAULT_AREA_ID;
        else if(id.isEmpty())
            areaId = BackEndZone.DEFAULT_AREA_ID;
        else
            areaId = id;
        return areaId;
    }


// only used in callconverce
    public ArrayList<BackEndPhone> GetListenDevices(String areaId,int callType){

        ArrayList<BackEndPhone> devices = new ArrayList<>();

        synchronized(BackEndPhoneManager.class) {
            BackEndZone area = serverAreaLists.get(GetAreaId(areaId));
            if(area!=null){
                area.GetListenDevices(devices,callType);
            }
        }

        if(callType== CommonCall.CALL_TYPE_BROADCAST){
            //remove inviting, incoming, talking device
            for(int iTmp=devices.size()-1;iTmp>=0;iTmp--){
                BackEndPhone phone = devices.get(iTmp);
                if(!backEndCallConvergencyMgr.CheckBroadCastEnabled(phone)){
                    devices.remove(iTmp);
                }
            }
        }else{
            //remove inviting device
            for(int iTmp=devices.size()-1;iTmp>=0;iTmp--){
                BackEndPhone phone = devices.get(iTmp);
                if(!backEndCallConvergencyMgr.CheckListenEnabled(phone)){
                    devices.remove(iTmp);
                }
            }
        }
        
        return devices;
    }

    private String GetTransferAreaIdLocal(String phoneId){
        String areaId = "";
        BackEndPhone matchedPhone;
        for(BackEndZone area:serverAreaLists.values()){
            matchedPhone = area.GetDevice(phoneId);
            if(matchedPhone!=null){
                areaId = area.GetTransferAreaId();
                break;
            }
        }
        return areaId;
    }

    private String GetWorkAreaIdLocal(String phoneId){
        String areaId = "";
        BackEndPhone matchedPhone;
        for(BackEndZone area:serverAreaLists.values()){
            matchedPhone = area.GetDevice(phoneId);
            if(matchedPhone!=null){
                areaId = area.GetWorkAreaId();
                break;
            }
        }
        return areaId;
    }

    public String GetForwardAreaId(String phoneId){
        String areaId;

        areaId = GetTransferAreaIdLocal(phoneId);
        if(areaId.isEmpty()){
            areaId = GetWorkAreaIdLocal(phoneId);
        }
        return areaId;
    }

 
    public int AddArea(String areaId,String areaName){
        int result = FailReason.FAIL_REASON_NO;
        BackEndZone area;
        synchronized (BackEndPhoneManager.class) {
            area = serverAreaLists.get(areaId);
            if(area!=null){
                result = FailReason.FAIL_REASON_HASEXIST;
            }else{
                area = new BackEndZone(areaId,areaName);
                serverAreaLists.put(areaId,area);
            }
        }
        return result;
    }

    public int UpdateAreaParams(String areaId,CallParams param){
        BackEndZone area;
        synchronized (BackEndPhoneManager.class) {
            area = serverAreaLists.get(areaId);
            if(area==null)
                return -1;
            area.params = param;
        }
        return 0;
    }
    
    public int UpdateAreaDevices(String areaId,ArrayList<UserDevice> devList,ArrayList<ServerDeviceInfo> infoList) {
        BackEndZone area;
        ArrayList<PhoneDevice> phoneList;
        int iTmp;
        ArrayList<UserDevice> newPhoneList = new ArrayList<>();
        ArrayList<ServerDeviceInfo> newInfoList = new ArrayList<>();
        BackEndPhone backEndPhone;
        UserDevice userDev;
        ServerDeviceInfo info;

        boolean isMatched;
        synchronized (BackEndPhoneManager.class) {
            area = serverAreaLists.get(areaId);
            if(area==null)
                return -1;
            phoneList = area.GetDeviceList();

            for(PhoneDevice dev:phoneList) {
                isMatched = false;
                for(iTmp=0;iTmp<devList.size();iTmp++) {
                    userDev = devList.get(iTmp);
                    if(dev.id.compareToIgnoreCase(userDev.devid)==0) {
                        backEndPhone = area.GetDevice(dev.id);
                        info  = infoList.get(iTmp);
                        if(backEndPhone.devInfo.CompareInfo(info)!=0) {
                            UserInterface.ConfigDeviceInfoOnServer(userDev.devid, info);
                            System.out.println(String.format("Update Dev %s DevInfo",dev.id));
                        }
                        isMatched = true;
                        break;
                    }
                }
                if(!isMatched) {
                    HandlerMgr.RemoveBackEndPhone(dev.id);
                }
            }

            for(iTmp=0;iTmp<devList.size();iTmp++) {
                userDev = devList.get(iTmp);
                if(area.GetDevice(userDev.devid)==null){
                    newPhoneList.add(userDev);
                    newInfoList.add(infoList.get(iTmp));
                }
            }

        }

        for(iTmp=0;iTmp<newPhoneList.size();iTmp++){
            userDev = newPhoneList.get(iTmp);
            info = newInfoList.get(iTmp);
            UserInterface.AddDeviceOnServer(userDev.devid, userDev.type, userDev.netMode,areaId);
            UserInterface.ConfigDeviceInfoOnServer(userDev.devid, info);
        }
        return 0;
    }

    
    public int UpdateAreas(ArrayList<BackEndZone> list) {
    	BackEndZone oldArea;
    	boolean matchedArea;
    	
        synchronized (BackEndPhoneManager.class) {
	        for(Iterator<Map.Entry<String, BackEndZone>> it = serverAreaLists.entrySet().iterator(); it.hasNext();){
	            Map.Entry<String, BackEndZone>item = it.next();
	            BackEndZone curArea = item.getValue();
	    		matchedArea = false;
	    		for(BackEndZone newArea:list) {
	    			if(curArea.areaId.compareToIgnoreCase(newArea.areaId)==0) {
	    				matchedArea = true;
	    				break;
	    			}
	    		}
	    		if(!matchedArea) {
	    			curArea.RemoveAllDevices();
	    			it.remove();
	    		}
	    	}
	    	
	    	for(BackEndZone newArea:list) {
	    		oldArea = serverAreaLists.get(newArea.areaId);
	    		if(oldArea==null) {
	    			AddArea(newArea.areaId,newArea.areaName);
	    		}else {
	    			oldArea.areaName = newArea.areaName;
	    		}
	    	}
	    	
        }
    	return 0;
    }
    

    public int AddPhone(String id, int t,String areaId){
        BackEndPhone matchedPhone;
        int result = FailReason.FAIL_REASON_NO;
        synchronized (BackEndPhoneManager.class) {
            
            BackEndZone area = serverAreaLists.get(GetAreaId(areaId));
            if(area==null){
                result = FailReason.FAIL_REASON_NOTFOUND;
            }else {

//            matchedPhone = serverPhoneLists.get(id);
                matchedPhone = area.GetDevice(id);
                if (matchedPhone == null) {
                    matchedPhone = new BackEndPhone(id, t);
                    matchedPhone.devInfo.areaId = areaId;
                    matchedPhone.devInfo.areaName = area.areaName;

//                serverPhoneLists.put(id,matchedPhone);
                    area.AddDevice(id, matchedPhone);
                    LogWork.Print(LogWork.BACKEND_PHONE_MODULE, LogWork.LOG_INFO, "Add Phone Device %s in area %s On Server", id,areaId);
                } else {
                    result = FailReason.FAIL_REASON_HASEXIST;
//                    LogWork.Print(LogWork.BACKEND_PHONE_MODULE, LogWork.LOG_INFO, "Add Phone Device %s in area %s On Server, but it had created", id,areaId);
                }
            }
        }

        if(snapThread==null){
            snapThread = new Thread("BackEndSnap") {
                @Override
                public void run() {
                    byte[] recvBuf = new byte[1024];
                    DatagramPacket recvPack;
                    DatagramSocket testSocket;
                    testSocket = SystemSnap.OpenSnapSocket(PhoneParam.snapStartPort,PhoneParam.SNAP_BACKEND_GROUP);
                    if(testSocket!=null){
                        DatagramPacket resPack;
                        while (!testSocket.isClosed()) {
                            java.util.Arrays.fill(recvBuf,(byte)0);
                            recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                            try {
                                testSocket.receive(recvPack);
                                if (recvPack.getLength() > 0) {
                                    if(PhoneParam.serverActive) {
                                        String recv = new String(recvBuf, "UTF-8");
                                        JSONObject json = JSONObject.parseObject(recv);
                                        if(json!=null){
                                            int type = json.getIntValue(SystemSnap.SNAP_CMD_TYPE_NAME);
                                            synchronized (BackEndPhoneManager.class) {
                                                if (type == SystemSnap.SNAP_BACKEND_CALL_REQ) {
                                                    String devid = JsonPort.GetJsonString(json,SystemSnap.SNAP_DEVID_NAME);
                                                    byte[] resultInfo;
                                                    resultInfo = backEndCallConvergencyMgr.MakeCallConvergenceSnap(devid);
                                                    if(resultInfo!=null) {
   //                                                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Get BackEnd Call Snap for dev %s, total %d bytes, send to %s:%d",devid,result.length,recvPack.getAddress().getHostAddress(),recvPack.getPort());
                                                        resPack = new DatagramPacket(resultInfo, resultInfo.length, recvPack.getAddress(), recvPack.getPort());
                                                        testSocket.send(resPack);
                                                    }
                                                } else if (type == SystemSnap.SNAP_BACKEND_TRANS_REQ) {
    //                                                resList = HandlerMgr.GetBackEndTransInfo();
    //                                                for (byte[] data : resList) {
    //                                                    resPack = new DatagramPacket(data, data.length, recvPack.getAddress(), recvPack.getPort());
    //                                                    testSocket.send(resPack);
    //                                                }
                                                }else if(type == SystemSnap.SNAP_SYSTEM_INFO_REQ){
                                                    byte[] systemInfo;
                                                    String curAreaId;

                                                    curAreaId = JsonPort.GetJsonString(json,SystemSnap.SNAP_AREAID_NAME);
                                                    systemInfo = MakeSystemInfo(curAreaId);
                                                    if(systemInfo!=null){
                                                        resPack = new DatagramPacket(systemInfo, systemInfo.length, recvPack.getAddress(), recvPack.getPort());
                                                        testSocket.send(resPack);
                                                    }
                                                } else if (type == SystemSnap.SNAP_DEL_LOG_REQ&&!PhoneParam.clientActive) {
                                                    String logFileName;
                                                    int logIndex = 1;
                                                    File logFile;
                                                    while (logIndex<=100) {
                                                        logFileName = LogWork.GetLogFileName(logIndex);
                                                        logFile = new File(logFileName);
                                                        if (logFile.exists() && logFile.isFile()) {
                                                            logFile.delete();
                                                        }
                                                        logIndex++;
                                                    }
                                                } else if (type == SystemSnap.LOG_CONFIG_REQ_CMD) {
                                                    int value;
                                                    LogWork.backEndNetModuleLogEnable = json.getIntValue(SystemSnap.LOG_BACKEND_NET_NAME) == 1;

                                                    LogWork.backEndDeviceModuleLogEnable = json.getIntValue(SystemSnap.LOG_BACKEND_DEVICE_NAME) == 1;

                                                    LogWork.backEndCallModuleLogEnable = json.getIntValue(SystemSnap.LOG_BACKEND_CALL_NAME) == 1;

                                                    LogWork.backEndPhoneModuleLogEnable = json.getIntValue(SystemSnap.LOG_BACKEND_PHONE_NAME) == 1;


                                                    LogWork.transactionModuleLogEnable = json.getIntValue(SystemSnap.LOG_TRANSACTION_NAME) == 1;

                                                    LogWork.debugModuleLogEnable = json.getIntValue(SystemSnap.LOG_DEBUG_NAME) == 1;

                                                    LogWork.bLogToFiles = json.getIntValue(SystemSnap.LOG_WIRTE_FILES_NAME) == 1;

                                                    value = json.getIntValue(SystemSnap.LOG_FILE_INTERVAL_NAME);
                                                    if (value <= 0)
                                                        value = 1;
                                                    LogWork.logInterval = value;

                                                    LogWork.dbgLevel = json.getIntValue(SystemSnap.LOG_DBG_LEVEL_NAME);

                                                }else if(type == SystemSnap.SNAP_DEL_LOG_REQ){
                                                    LogWork.ResetLogIndex();
                                                }else if(type== SystemSnap.SNAP_DEV_REQ){
                                                    CreateDevInfoSendTask(testSocket,recvPack.getAddress(),recvPack.getPort());
                                                }
                                            }
                                            json.clear();
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }catch(Exception ee){
                                LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_ERROR,"Socket of BackEnd Snap err with %s",ee.getMessage());
                            }
                        }
                    }
                }
            };
            snapThread.start();
        }

        return result;
    }

    void CreateDevInfoSendTask(DatagramSocket socket, InetAddress serverAddress,int serverPort){
        if(isSendingDevInfo==false){
            isSendingDevInfo = true;
            Thread sendingthread = new Thread("devInfo"){
                @Override
                public void run() {
                    DatagramPacket resPack;
                    int sentCount = 0;
                    String ipAddress;
                    String localAddress = PhoneParam.GetLocalAddress();
                    for(BackEndZone zone:serverAreaLists.values()){
                        for(BackEndPhone phone:zone.phoneList.values()){
                            JSONObject resJson = new JSONObject();
                            resJson.put(SystemSnap.SNAP_CMD_TYPE_NAME, SystemSnap.SNAP_DEV_RES);
                            resJson.put(SystemSnap.SNAP_AREAID_NAME,zone.areaId);
                            resJson.put(SystemSnap.SNAP_DEVID_NAME, phone.id);
                            resJson.put(SystemSnap.SNAP_DEVTYPE_NAME,phone.type);
                            if(phone.isReg)
                                resJson.put(SystemSnap.SNAP_REG_NAME,1);
                            else
                                resJson.put(SystemSnap.SNAP_REG_NAME,0);
                            ipAddress = HandlerMgr.GetBackEndPhoneAddress(phone.id);
                            if(ipAddress.compareToIgnoreCase("127.0.0.1")==0){
                                ipAddress = localAddress;
                            }
                            resJson.put(SystemSnap.SNAP_IP_ADDRESS,ipAddress);
                            byte[] resBuf = resJson.toString().getBytes();
                            resPack = new DatagramPacket(resBuf, resBuf.length, serverAddress, serverPort);
                            try {
                                socket.send(resPack);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            sentCount++;
                            if((sentCount%20)==19){
                                try {
                                    sleep(20);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    isSendingDevInfo = false;
                }
            };

            sendingthread.start();
        }
    }

// used in local
    private BackEndPhone GetLocalDevice(String id){
        BackEndPhone matchedPhone=null;
        for(BackEndZone area:serverAreaLists.values()){
            matchedPhone = area.GetDevice(id);
            if(matchedPhone!=null)
                break;
        }
        return matchedPhone;
    }

    private int CheckTransferCount(String areaId){
        int count = 0;

        for(BackEndZone area:serverAreaLists.values()){
            if(areaId.compareToIgnoreCase(area.transferAreaId)==0){
                count++;
            }
        }
        
        return count;
    }

    private int ClearAreaTransfer(String devid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        for(BackEndZone area:serverAreaLists.values()){
            if(area.GetDevice(devid)!=null){
                area.transferAreaId = "";
                result = ProtocolPacket.STATUS_OK;
                break;
            }
        }

        return result;
    }

    private int SetAreaTransfer(String devid,String areaid){
        int result = ProtocolPacket.STATUS_NOTFOUND;
        String configAreaId = "";
        BackEndZone oldArea;

        // clear transfer in dest area
        oldArea = serverAreaLists.get(areaid);
        if(oldArea!=null){
            if(!oldArea.transferAreaId.isEmpty()) {
                oldArea.transferAreaId = "";
                oldArea.NotifyTransferChangExcept("");
            }
        }

        // set transfer
        for(BackEndZone area:serverAreaLists.values()){
            if(area.GetDevice(devid)!=null){
                area.transferAreaId = areaid;
                configAreaId = area.areaId;
                result = ProtocolPacket.STATUS_OK;
                area.NotifyTransferChangExcept(devid);
                break;
            }
        }

        // clear transfer to config area
        if(!configAreaId.isEmpty()){
            for(BackEndZone area:serverAreaLists.values()){
                if(area.transferAreaId.compareToIgnoreCase(configAreaId)==0){
                    area.transferAreaId = "";
                    area.NotifyTransferChangExcept("");
                }
            }
        }

        return result;
    }

    public void CancelListenCall(String id){
        backEndCallConvergencyMgr.CancelListenCall(id);
    }
    
    public void RemovePhone(String id){
         LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_INFO,"Remove Phone %s On Server",id);
        synchronized (BackEndPhoneManager.class){
            
//            matchedPhone = serverPhoneLists.get(id);
            for(BackEndZone area:serverAreaLists.values()){
                if(area.RemoveDevice(id)==FailReason.FAIL_REASON_NO)
                    break;
            }
        }
    }

    public void RemoveAllPhone(){
        synchronized (BackEndPhoneManager.class){
            for(Iterator<Map.Entry<String, BackEndZone>> it = serverAreaLists.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, BackEndZone>item = it.next();
                BackEndZone area = item.getValue();
                area.RemoveAllDevices();
                it.remove();
                LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_INFO,"Remove All Phone Devices in area %s On Server",area.areaId);
            }
        }
    }

    public boolean SetSystemConfig(ArrayList<ConfigItem> list){
        systemConfigList = list;
        return true;
    }

    public boolean SetDeviceConfig(String id, ArrayList<ConfigItem> list){
        BackEndPhone matchedPhone;
        boolean result = false;
        synchronized (BackEndPhoneManager.class) {
            matchedPhone = GetLocalDevice(id);
            if (matchedPhone != null) {
                matchedPhone.SetDeviceConfig(list);
                result = true;
            }
        }
        return result;
    }

    public boolean SetDeviceInfo(String id, ServerDeviceInfo info){
        BackEndPhone matchedPhone;
        boolean result = false;
        synchronized (BackEndPhoneManager.class) {
            matchedPhone = GetLocalDevice(id);
            if (matchedPhone != null) {
                matchedPhone.SetDeviceInfo(info);
                result = true;
            }
        }
        return result;
    }

    private ArrayList<ConfigItem> GetDeviceConfig(String id){
        BackEndPhone matchedPhone;
        ArrayList<ConfigItem> paramList = new ArrayList<>();
        matchedPhone = GetLocalDevice(id);
        if (matchedPhone != null)
            matchedPhone.GetDeviceConfig(paramList);
        return paramList;
    }
    
    private ArrayList<ConfigItem> GetSystemConfig(){
        return systemConfigList;
    }

    private void ClearListenInZoneExcept(String zoneId,String id){
        for(BackEndZone area:serverAreaLists.values()){
            if(zoneId.compareToIgnoreCase(area.areaId)==0) {
                area.ClearAllListenExcept(id);
            }
        }
    }

    private int  SetListenDevice(String id,boolean status){
        BackEndPhone phone;
        String zoneId;
        int resStatus = ProtocolPacket.STATUS_OK;
        
        phone = GetLocalDevice(id);
        zoneId = GetWorkAreaIdLocal(id);
        if(phone==null){
            resStatus = ProtocolPacket.STATUS_NOTFOUND;
        }else{
            if(phone.type!=PhoneDevice.BED_CALL_DEVICE){
                resStatus = ProtocolPacket.STATUS_NOTSUPPORT;
            }else{
                if(status){
                    ClearListenInZoneExcept(zoneId,id);
                }            
                phone.enableListen = status;
                resStatus = ProtocolPacket.STATUS_OK;
                LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_DEBUG,"BackEnd Set Dev %s Listen State %b",phone.id,status);
            }
        }
        return resStatus;
    }

    private void PacketRecvProcess(ProtocolPacket packet){
        String devID;
        Transaction trans;
        BackEndPhone phone;

        int resStatus;

        LogWork.Print(LogWork.BACKEND_PHONE_MODULE,LogWork.LOG_DEBUG,"Server recv %s %s Packet",packet.sender,ProtocolPacket.GetTypeName(packet.type));

        devID = packet.sender;
        switch (packet.type) {
            case ProtocolPacket.REG_REQ:
                RegReqPack regPacket = (RegReqPack)packet;
                RegResPack regResP;
                phone = GetLocalDevice(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_FORBID;
                    regResP = new RegResPack(resStatus,regPacket);
                }else{
                    resStatus = ProtocolPacket.STATUS_OK;
                    phone.UpdateRegStatus(regPacket.expireTime);
                    regResP = new RegResPack(resStatus,regPacket);
                    regResP.areaId = phone.devInfo.areaId;
                    regResP.areaName = phone.devInfo.areaName;
                    regResP.transferAreaId = GetTransferAreaIdLocal(devID);
                    regResP.listenCallEnable = phone.enableListen;
                    regResP.snapPort = PhoneParam.snapStartPort;
                }

                trans = new Transaction(devID,packet,regResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID, trans);
                break;
            case ProtocolPacket.CALL_TRANSFER_REQ:
                TransferReqPack transferPacket = (TransferReqPack)packet;
                String areaId = transferPacket.transferAreaId;
                boolean state = transferPacket.transferEnabled;
                int hasTransferedCount;
                phone = GetLocalDevice(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_NOTFOUND;
                }else{
                    if(phone.type!=PhoneDevice.NURSE_CALL_DEVICE){
                        resStatus = ProtocolPacket.STATUS_NOTSUPPORT;
                    }else{
                        if(state){
                            hasTransferedCount = CheckTransferCount(areaId);
                            if(hasTransferedCount>=PhoneParam.transferMaxNum){
                                resStatus = ProtocolPacket.STATUS_BUSY;
                            }else{
                                resStatus = SetAreaTransfer(devID,areaId);
                            }
                        }else{
                            resStatus = ClearAreaTransfer(devID);
                        }
                    }
                }
                TransferResPack transferResP = new TransferResPack(resStatus,transferPacket);

                trans = new Transaction(devID,packet,transferResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID, trans);
            break;
            case ProtocolPacket.CALL_LISTEN_REQ:
                ListenCallReqPack listenPacket=(ListenCallReqPack)packet;
                resStatus = SetListenDevice(devID,listenPacket.listenEnable);
                ListenCallResPack listgenResP = new ListenCallResPack(resStatus,listenPacket);

                trans = new Transaction(devID,packet,listgenResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID, trans);
                break;
            case ProtocolPacket.DEV_QUERY_REQ:
                DevQueryReqPack devReqP = (DevQueryReqPack)packet;
                DevQueryResPack devResP;
                phone = GetLocalDevice(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_NOTFOUND;
                }else{
                    resStatus = ProtocolPacket.STATUS_OK;
                }
                devResP = new DevQueryResPack(resStatus,devReqP);
                if(resStatus==ProtocolPacket.STATUS_OK){
                    devResP.phoneList = GetDeviceList(phone.devInfo.areaId);
                }

                trans = new Transaction(devID,packet,devResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID,trans);
                break;
            case ProtocolPacket.DEV_CONFIG_REQ:
                ConfigReqPack configReqP = (ConfigReqPack)packet;
                ConfigResPack configResP;
                phone = GetLocalDevice(devID);
                if(phone==null){
                    resStatus = ProtocolPacket.STATUS_NOTFOUND;
                }else{
                    resStatus = ProtocolPacket.STATUS_OK;
                }
                configResP = new ConfigResPack(resStatus,configReqP);
                if(resStatus==ProtocolPacket.STATUS_OK){
                    configResP.params = GetDeviceConfig(devID);
                }
                trans = new Transaction(devID,packet,configResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID,trans);
                break;
            case ProtocolPacket.SYSTEM_CONFIG_REQ:
                SystemConfigReqPack systemConfigReqP = (SystemConfigReqPack)packet;
                SystemConfigResPack systemConfigResP;
                resStatus = ProtocolPacket.STATUS_OK;
                systemConfigResP = new SystemConfigResPack(resStatus,systemConfigReqP);
                systemConfigResP.params = GetSystemConfig();
                trans = new Transaction(devID,packet,systemConfigResP,Transaction.TRANSCATION_DIRECTION_S2C);
                HandlerMgr.AddBackEndTrans(packet.msgID,trans);
                
            case ProtocolPacket.CALL_REQ:
            case ProtocolPacket.CALL_RES:
            case ProtocolPacket.END_REQ:
            case ProtocolPacket.END_RES:
            case ProtocolPacket.ANSWER_REQ:
            case ProtocolPacket.ANSWER_RES:
            case ProtocolPacket.CALL_UPDATE_REQ:
            case ProtocolPacket.CALL_VIDEO_INVITE_REQ:
            case ProtocolPacket.CALL_VIDEO_INVITE_RES:
            case ProtocolPacket.CALL_VIDEO_ANSWER_REQ:
            case ProtocolPacket.CALL_VIDEO_ANSWER_RES:
            case ProtocolPacket.CALL_VIDEO_END_REQ:
            case ProtocolPacket.CALL_VIDEO_END_RES:
            case ProtocolPacket.CALL_CANCEL_REQ:
            case ProtocolPacket.CALL_CANCEL_RES:
                CallConvergencyProcessPacket(packet);
                break;
        }
    }

    private void PacketTimeOverProcess(ProtocolPacket packet){
        CallConvergencyProcessTimeOver(packet);
    }

    private void UpdatePhonesRegTick(){
        for(BackEndZone area:serverAreaLists.values()){
            area.IncreaseRegTick();
        }
    }

    private void CallConvergencyProcessPacket(ProtocolPacket packet){
        backEndCallConvergencyMgr.ProcessPacket(packet);
    }

    private void CallConvergencySecondTick(){
        backEndCallConvergencyMgr.ProcessSecondTick();
    }

    
    private void CallConvergencyProcessTimeOver(ProtocolPacket packet){
        switch(packet.type){
            case ProtocolPacket.CALL_REQ:
            case ProtocolPacket.ANSWER_REQ:
            case ProtocolPacket.END_REQ:
                backEndCallConvergencyMgr.ProcessTimeOver(packet);
                break;
        }
    }

    public ArrayList<PhoneDevice> GetDeviceList(String areaId){
        ArrayList<PhoneDevice> phoneList;
        BackEndZone area = serverAreaLists.get(GetAreaId(areaId));
        if(area==null)
            phoneList = new ArrayList<>();
        else
            phoneList = area.GetDeviceList();

        return phoneList;
    }

    private byte[] MakeSystemInfo(String areaId){
        BackEndStatistics backEndStatist = UserInterface.GetBackEndStatistics();
        BackEndStatistics curAreaStatist = UserInterface.GetBackEndStatistics(areaId);
        JSONObject json = new JSONObject();
        byte[] result;

        json.put(SystemSnap.SNAP_CMD_TYPE_NAME,SystemSnap.SNAP_SYSTEM_INFO_RES);
        json.put(SystemSnap.SNAP_INFO_CALLCONVERGENCE_NUM_NAME,backEndStatist.callConvergenceNum);
        json.put(SystemSnap.SNAP_INFO_BACKEND_TRANS_NUM_NAME,backEndStatist.transNum);
        json.put(SystemSnap.SNAP_INFO_BACKEND_REGSUCC_NUM_NAME,backEndStatist.regSuccDevNum);
        json.put(SystemSnap.SNAP_INFO_BACKEND_REGFAIL_NUM_NAME,backEndStatist.regFailDevNum);
        
        json.put(SystemSnap.SNAP_INFO_BACKEND_CURAREA_REGSUCC_NUM_NAME,curAreaStatist.regSuccDevNum);
        json.put(SystemSnap.SNAP_INFO_BACKEND_CURAREA_REGFAIL_NUM_NAME,curAreaStatist.regFailDevNum);

        result = json.toString().getBytes();
        json.clear();
        return result;
    }

    public void SetMsgReceiver(MsgReceiver receiver) {
        userMsgReceiver = receiver;
    }

    public void PostBackEndUserMsg(int type, Object obj) {
        if(userMsgReceiver!=null) {
            CallPubMessage msg = new CallPubMessage(type,obj);
            userMsgReceiver.AddMessage(msg);
        }
    }

}
