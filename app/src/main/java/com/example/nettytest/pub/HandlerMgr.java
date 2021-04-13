package com.example.nettytest.pub;

import android.os.Handler;
import android.os.Message;

import com.example.nettytest.backend.backenddevice.BackEndDevManager;
import com.example.nettytest.backend.backendphone.BackEndConfig;
import com.example.nettytest.backend.backendphone.BackEndPhone;
import com.example.nettytest.backend.backendphone.BackEndPhoneManager;
import com.example.nettytest.backend.backendtranscation.BackEndTransactionMgr;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import com.example.nettytest.pub.protocol.ConfigItem;
import com.example.nettytest.pub.protocol.ProtocolPacket;
import com.example.nettytest.pub.transaction.Transaction;
import com.example.nettytest.terminal.audio.AudioMgr;
import com.example.nettytest.terminal.terminaldevice.TerminalDevManager;
import com.example.nettytest.terminal.terminaldevice.TerminalTcpDevice;
import com.example.nettytest.terminal.terminalphone.TerminalPhone;
import com.example.nettytest.terminal.terminalphone.TerminalPhoneManager;
import com.example.nettytest.terminal.terminaltransaction.TerminalTransactionMgr;
import com.example.nettytest.userinterface.FailReason;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.TerminalDeviceInfo;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserConfig;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class HandlerMgr {
    static private BackEndTransactionMgr backEndTransMgr = new BackEndTransactionMgr();
    static private BackEndDevManager backEndDevMgr = new BackEndDevManager();
    static private BackEndPhoneManager backEndPhoneMgr = new BackEndPhoneManager();

    static private TerminalTransactionMgr terminalTransMgr = new TerminalTransactionMgr();
    static private TerminalDevManager terminalDevManager = new TerminalDevManager();
    static private TerminalPhoneManager terminalPhoneMgr = new TerminalPhoneManager();
    static private BackEndConfig backEndConfig = new BackEndConfig();


//for terminal TcpNetDevice

    static public void UpdatePhoneDevChannel(String ID, Channel channel){
        terminalDevManager.UpdateDevChannel(ID,channel);
    }

    static public void PhoneDevSendBuf(String ID,ByteBuf buf){
        terminalDevManager.DevSendBuf(ID,buf);
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

    static public void PostTerminalPhoneMsg(Message msg){
        terminalPhoneMgr.PostTerminalPhoneMessage(msg);
    }

    static public void SetTerminalMessageHandler(Handler h){
        terminalPhoneMgr.SetMessageHandler(h);
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

    static public int AnswerTerminalCall(String devid,String callID){
        return terminalPhoneMgr.AnswerCall(devid,callID);
    }

    static public int GetTermCallCount(){
        return terminalPhoneMgr.GetCallCount();
    }

    static public int QueryTerminalLists(String devid){
        return terminalPhoneMgr.QueryDevs(devid);
    }

    static public int QueryTerminalConfig(String devid){
        return terminalPhoneMgr.QueryConfig(devid);
    }

    static public int QuerySystemConfig(String devid){
        return terminalPhoneMgr.QuerySystemConfig(devid);
    }


// for backend TcpNetDevice
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


// for backend phone
    static public BackEndPhone GetBackEndPhone(String ID) {
        return backEndPhoneMgr.GetDevice(ID);
    }
       
    static public void SetBackEndConfig(BackEndConfig config){
        backEndConfig.Copy(config);
    }

    static public BackEndConfig GetBackEndConfig(){
        return backEndConfig;
    }

    static public void PostBackEndPhoneMsg(int type,Object obj){
        backEndPhoneMgr.PostBackEndPhoneMessage(type,obj);
    }

    static public void SetBackEndlMessageHandler(Handler h){
        backEndPhoneMgr.SetMessageHandler(h);
    }
    
// create backend device
    static public boolean AddBackEndPhone(String ID,int type,int netMode){
        backEndDevMgr.AddDevice(ID,netMode);
        backEndPhoneMgr.AddPhone(ID,type);
        return true;
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

    static public boolean GetBackEndPhoneInfo(ArrayList<UserDevice> lists){
        ArrayList<PhoneDevice> phoneLists = new ArrayList<>();
        backEndPhoneMgr.GetDeviceList(phoneLists);
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
            lists.add(userDev);
        }
        return true;
    }

// get listen phone
    static public ArrayList<BackEndPhone> GetBackEndListenDevices(int callType){
        return backEndPhoneMgr.GetListenDevices(callType);
    }

    static public boolean CheckForwardEnable(BackEndPhone phone, int callType){
        return backEndPhoneMgr.CheckForwardEnable(phone,callType);
    }

    public static ArrayList<byte[]> GetBackEndTransInfo(){
        return backEndTransMgr.GetTransactionDetail(SystemSnap.SNAP_BACKEND_TRANS_RES);
    }

// get audio Owner

    public static String GetAudioOwner(){
        return AudioMgr.GetAudioOwnwer();
    }
}
