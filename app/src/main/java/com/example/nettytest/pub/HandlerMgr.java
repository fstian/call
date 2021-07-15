package com.example.nettytest.pub;


import com.example.nettytest.backend.backenddevice.BackEndDevManager;
import com.example.nettytest.backend.backendphone.BackEndZone;
import com.example.nettytest.backend.backendphone.BackEndConfig;
import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.backend.backendphone.BackEndPhoneManager;
import com.example.nettytest.backend.backendtranscation.BackEndTransactionMgr;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.terminal.terminaldevice.TerminalDevManager;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;
import com.example.nettytest.terminal.terminalphone.TerminalPhoneManager;
import com.example.nettytest.terminal.terminaltransaction.TerminalTransactionMgr;
import com.example.nettytest.pub.result.FailReason;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserConfig;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;
import com.example.nettytest.userinterface.UserVideoMessage;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class HandlerMgr {
    static private BackEndTransactionMgr backEndTransMgr = new BackEndTransactionMgr();
    static private BackEndDevManager backEndDevMgr = new BackEndDevManager();
    static private BackEndPhoneManager backEndPhoneMgr = new BackEndPhoneManager();

    static private TerminalTransactionMgr terminalTransMgr = new TerminalTransactionMgr();
    static private TerminalDevManager terminalDevManager = new TerminalDevManager();
    static private TerminalPhoneManager terminalPhoneMgr = new TerminalPhoneManager();

    static private DevicesQuery deviceQuery= null;

    final static int ANDORID_OS = 1;
    final static int LINUX_OS = 2;
    final static int WINDOWS_OS = 3;

    static int OS_TYPE = ANDORID_OS;


//for terminal TcpNetDevice

    static public void ReadSystemType(){
        String osName;

        Properties prop = System.getProperties();
        osName = prop.getProperty("os.name");
        osName = osName.toLowerCase();
        if(osName.indexOf("windows")>=0){
            OS_TYPE = WINDOWS_OS;
        }else {
            OS_TYPE = LINUX_OS;
            Map<String, String> osMap = System.getenv();
            for (Map.Entry<String, String> entry : osMap.entrySet()) {
                String key = entry.getKey();
                if(key.indexOf("ANDROID")>=0){
                    OS_TYPE = ANDORID_OS;
                    break;
                }
            }
        }
    }

    static int GetOSType(){
        return OS_TYPE;
    }

    static public long GetTerminalRunSecond(){
        return terminalPhoneMgr.GetRunSecond();
    }

    static public void CloseAllTerminalDevice(){
        terminalDevManager.CloseAllDevice();
    }

    static public void UpdatePhoneDevChannel(String ID, Channel channel){
        terminalDevManager.UpdateDevChannel(ID,channel);
    }

    static public void PhoneDevSendBuf(String ID,ByteBuf buf){
        terminalDevManager.DevSendBuf(ID,buf);
    }
    
    static public DeviceStatistics GetTerminalRegDevNum() {
        DeviceStatistics devStatist = terminalPhoneMgr.GetRegStatist();
        return devStatist;
    }

// for TerminalTranscation
    static public boolean AddPhoneTrans(String ID, Transaction trans){
        return terminalTransMgr.AddTransaction(ID,trans);
    }

    static public int GetTermTransCount(){
        return terminalTransMgr.GetTransCount();
    }

    static public void PhoneProcessPacket(ProtocolPacket packet){
        terminalTransMgr.ProcessPacket(packet);
    }

    static public void TerminalPhoneTransactionTick(){
        terminalTransMgr.TransTimerProcess();
    }

    static public ArrayList<byte[]> GetTerminalTransInfo(){
        return terminalTransMgr.GetTransactionDetail(SystemSnap.SNAP_TERMINAL_TRANS_RES);
    }

// for terminal Phone
    static public TerminalPhone GetPhoneDev(String ID){

        return terminalPhoneMgr.GetDevice(ID);
    }

    static public void PostTerminalPhoneMsg(CallPubMessage msg){
        terminalPhoneMgr.PostTerminalPhoneMessage(msg);
    }

    static public void SetTerminalMessageHandler(ArrayList<CallPubMessage> h){
        terminalPhoneMgr.SetMessageHandler(h);
    }

    static public void TerminalStartSnap(int port){
        terminalPhoneMgr.StartSnap(port);
    }

    static public void SetBackEndMessageHandler(ArrayList<CallPubMessage> h){
        backEndPhoneMgr.SetMessageHandler(h);
    }

    static public void SendMessageToUser(int type,Object obj){
        if(type== UserMessage.MESSAGE_CALL_INFO) {
            UserCallMessage msg = (UserCallMessage)obj;
            if(msg.type<UserMessage.CALL_MESSAGE_SUCC_MAX)
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Send Msg %s of Call %s to dev %s", UserMessage.GetMsgName(msg.type),msg.callId,msg.devId);
            else
                LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Send Msg %s of Call %s to dev %s, reason is %s", UserMessage.GetMsgName(msg.type),msg.callId,msg.devId, FailReason.GetFailName(msg.reason));
        }else if(type==UserMessage.MESSAGE_REG_INFO){
            UserRegMessage regMsg = (UserRegMessage)obj;
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Send Msg %s to dev %s", UserMessage.GetMsgName(regMsg.type),regMsg.devId);
        }else if(type == UserMessage.MESSAGE_VIDEO_INFO){
            UserVideoMessage videoMsg = (UserVideoMessage)obj;
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Send Msg %s to dev %s", UserMessage.GetMsgName(videoMsg.type),videoMsg.devId);
        }
        terminalPhoneMgr.SendUserMessage(type,obj);
    }

    static public void CreateTerminalPhone(String id,int type,int netMode){
        terminalDevManager.AddDevice(id,netMode);

        TerminalPhone phone = new TerminalPhone(id,type);
        terminalPhoneMgr.AddDevice(phone);
    }

    static public void RemoveTerminalPhone(String id){
        terminalDevManager.RemovePhone(id);
        terminalPhoneMgr.RemovePhone(id);
    }


    static public boolean SetTerminalPhoneConfig(String id,TerminalDeviceInfo info){
        return terminalPhoneMgr.SetConfig(id,info);
    }

    static public String BuildTerminalCall(String caller,String callee,int callType){
        return terminalPhoneMgr.BuildCall(caller,callee,callType);
    }

    static public int EndTerminalCall(String devid,String callID){
        return terminalPhoneMgr.EndCall(devid,callID);
    }

    static public int EndTerminalCall(String devid,int type){
        return terminalPhoneMgr.EndCall(devid,type);
    }

    static public int AnswerTerminalCall(String devid,String callID){
        return terminalPhoneMgr.AnswerCall(devid,callID);
    }

    static public int StartTerminalVideo(String devid,String callID){
        return terminalPhoneMgr.StartVideoCall(devid,callID);
    }

    static public int AnswerTerminalVideo(String devid,String callID){
        return terminalPhoneMgr.AnswerVideoCall(devid,callID);
    }

    static public int StopTerminalVideo(String devid,String callID){
        return terminalPhoneMgr.StopVideoCall(devid,callID);
    }

    static public int GetTermCallCount(){
        return terminalPhoneMgr.GetCallCount();
    }

    static public int QueryTerminalLists(String devid){
        return terminalPhoneMgr.QueryDevs(devid);
    }

    static public int RequireCallTransfer(String devId,String areaId,boolean state){
        return terminalPhoneMgr.RequireCallTransfer(devId,areaId,state);
    }

    static public int RequireBedListen(String devId,boolean state){
        return terminalPhoneMgr.RequireBedListen(devId,state);
    }

    static public int QueryTerminalConfig(String devid){
        return terminalPhoneMgr.QueryConfig(devid);
    }

    static public int QuerySystemConfig(String devid){
        return terminalPhoneMgr.QuerySystemConfig(devid);
    }

    static public int GetTerminalCalNum() {
        return terminalPhoneMgr.GetCallCount();
    }


// for backend TcpNetDevice
    // for devices check
    static public void StartCheckDevices(String server,int port){
        if(deviceQuery==null){
            deviceQuery = new DevicesQuery(server,port);
            deviceQuery.StartQuery();
        }
    }

    static public void UpdateBackEndDevChannel(String ID,Channel ch){
        backEndDevMgr.UpdateDevChannel(ID,ch);
    }

    static public void UpdateBackEndDevSocket(String ID, DatagramSocket socket,InetAddress hostAddr, int port){
        backEndDevMgr.UpdateDevSocket(ID,socket,hostAddr,port);
    }

    static public void BackEndDevSendBuf(String ID,ByteBuf buf){
        backEndDevMgr.DevSendBuf(ID, buf);
    }


//for backend transcation
    static public void AddBackEndTrans(String ID,Transaction trans){
        backEndTransMgr.AddTransaction(ID,trans);
    }

    static public void BackEndProcessPacket(ProtocolPacket packet){
        backEndTransMgr.ProcessPacket(packet);
    }

    static public int GetBackTransCount(){
        return backEndTransMgr.GetTransCount();
    }

    static public int GetBackCallCount(){
        return backEndPhoneMgr.GetCallCount();
    }

    static public void BackEndTransactionTick(){
        backEndTransMgr.TransTimerProcess();
    }

    static public long GetBackEndRunSecond(){
        return backEndPhoneMgr.GetRunSecond();
    }
    
    static public DeviceStatistics GetBackEndRegDevNum() {
        return backEndPhoneMgr.GetRegStatistics();
    }


// for backend phone
    static public BackEndPhone GetBackEndPhone(String ID) {
        return backEndPhoneMgr.GetDevice(ID);
    }
       
    static public void SetBackEndConfig(String areaId,BackEndConfig config){
        CallParams param = new CallParams();
        
        param.normalCallToBed = config.normalCallToBed;
        param.normalCallToCorridor = config.normalCallToCorridor;
        param.normalCallToRoom = config.normalCallToRoom;
        param.normalCallToTV = config.normalCallToTv;
        param.emerCallToBed = config.emerCallToBed;
        param.emerCallToCorridor = config.emerCallToCorridor;
        param.emerCallToRoom = config.emerCallToRoom;
        param.emerCallToTV = config.emerCallToTv;

        backEndPhoneMgr.UpdateAreaParams(areaId, param);
    }

    static public void PostBackEndPhoneMsg(int type,Object obj){
        backEndPhoneMgr.PostBackEndPhoneMessage(type,obj);
    }

    static public void UpdateAreas(ArrayList<BackEndZone> list) {
    	backEndPhoneMgr.UpdateAreas(list);
    }
    
    static public void UpdateAreaDevices(String areaId,ArrayList<UserDevice> devList,ArrayList<ServerDeviceInfo> infoList) {
    	backEndPhoneMgr.UpdateAreaDevices(areaId,devList,infoList);
    }

    static public void UpdateAreaParam(String areaId,CallParams params){
        backEndPhoneMgr.UpdateAreaParams(areaId,params);
    }
// create backend area

    static public int AddBackEndArea(String areaId,String areaName){
        return backEndPhoneMgr.AddArea(areaId,areaName);
    }
    
// create backend device
    static public int  AddBackEndPhone(String ID,int type,int netMode,String area){
        int result;

        result = backEndPhoneMgr.AddPhone(ID,type,area);
        if(result== FailReason.FAIL_REASON_NO)
            backEndDevMgr.AddDevice(ID,netMode);
        return result;
    }

    static public boolean SetBackEndPhoneConfig(String id, ArrayList<UserConfig>list){
        ArrayList<ConfigItem> configList = new ArrayList<>();
        boolean result;
        for(int iTmp=0;iTmp<list.size();iTmp++){
            ConfigItem item = new ConfigItem();
            UserConfig config = list.get(iTmp);
            item.param_id = config.param_id;
            item.param_name = config.param_name;
            item.param_value = config.param_value;
            item.param_unit = config.param_unit;
            configList.add(item);
        }
        result = backEndPhoneMgr.SetDeviceConfig(id,configList);
        if(!result)
            configList.clear();
        return result;
    }

    static public boolean SetBackEndSystemConfig( ArrayList<UserConfig>list){
        ArrayList<ConfigItem> configList = new ArrayList<>();
        boolean result;
        for(int iTmp=0;iTmp<list.size();iTmp++){
            ConfigItem item = new ConfigItem();
            UserConfig config = list.get(iTmp);
            item.param_id = config.param_id;
            item.param_name = config.param_name;
            item.param_value = config.param_value;
            item.param_unit = config.param_unit;
            configList.add(item);
        }
        result = backEndPhoneMgr.SetSystemConfig(configList);
        if(!result)
            configList.clear();
        return result;
    }

    static public boolean SetBackEndPhoneInfo(String id, ServerDeviceInfo info){
        boolean result;
        ServerDeviceInfo devInfo = new ServerDeviceInfo(info);
        result = backEndPhoneMgr.SetDeviceInfo(id,devInfo);
        return result;
    }

    static public boolean RemoveBackEndPhone(String ID){
        backEndPhoneMgr.RemovePhone(ID);
        backEndDevMgr.RemoveDevice(ID);
        return true;
    }

    static public boolean RemoveAllBackEndPhone(){
        backEndPhoneMgr.RemoveAllPhone();
        backEndDevMgr.RemoveAllDevice();
        return true;
    }

    static public ArrayList<UserDevice> GetBackEndPhoneInfo(String areaId){
        
        ArrayList<PhoneDevice> phoneLists = backEndPhoneMgr.GetDeviceList(areaId);
        ArrayList<UserDevice> userListList = new ArrayList<>();
        for(PhoneDevice dev:phoneLists){
            UserDevice userDev = new UserDevice();
            userDev.isRegOk = dev.isReg;
            userDev.devid = dev.id;
            switch(dev.type) {
                case PhoneDevice.BED_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_BED_DEVICE;
                    break;
                case PhoneDevice.DOOR_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_DOOR_DEVICE;
                    break;
                case PhoneDevice.CORRIDOR_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_CORRIDOR_DEVICE;
                    break;
                case PhoneDevice.NURSE_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_NURSER_DEVICE;
                    break;
                case PhoneDevice.EMER_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_EMERGENCY_DEVICE;
                    break;
                case PhoneDevice.TV_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_TV_DEVICE;
                    break;
                case PhoneDevice.WHITE_BOARD_DEVICE:
                    userDev.type = UserInterface.CALL_WHITE_BOARD_DEVICE;
                    break;
                case PhoneDevice.DOOR_LIGHT_CALL_DEVICE:
                    userDev.type = UserInterface.CALL_DOOR_LIGHT_DEVICE;
                    break;
                default:
                    userDev.type = dev.type;
                    break;
            }
            userDev.bedName = dev.bedName;
            userListList.add(userDev);
        }
        return userListList;
    }

// get listen phone

    static public String GetListenAreaId(String phoneId){
        return backEndPhoneMgr.GetListenAreaId(phoneId);
    }

    static public ArrayList<BackEndPhone> GetBackEndListenDevices(String areaId,int callType){
        return backEndPhoneMgr.GetListenDevices(areaId,callType);
    }


    public static ArrayList<byte[]> GetBackEndTransInfo(){
        return backEndTransMgr.GetTransactionDetail(SystemSnap.SNAP_BACKEND_TRANS_RES);
    }

// get audio Owner

}
