package com.example.nettytest.terminal.test;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.example.nettytest.pub.BackEndStatistics;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.TerminalStatistics;
import com.example.nettytest.userinterface.ListenCallMessage;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TestInfo;
import com.example.nettytest.userinterface.TransferMessage;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;
import com.example.nettytest.userinterface.UserVideoMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientTest {
    ArrayList<TestArea> areaList;

    int selectArea = 0;
    int selectDevice = 0;
    static DatagramSocket testSocket=null;

    public boolean isTestFlag = false;
    public long testStartTime = 0;
    private int printSkip = 10;

    static ClientSnapThread snapThread = null; 

    class ClientSnapThread extends Thread{
        public ClientSnapThread(){
            super("UserSnapThread");
        }

        @Override
        public void run() {
            byte[] recvBuf = new byte[1024];
            DatagramPacket recvPack;
            while (!testSocket.isClosed()) {
                java.util.Arrays.fill(recvBuf,(byte)0);
                recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                try {
                    testSocket.receive(recvPack);
                    if (recvPack.getLength() > 0) {
                        String recv = new String(recvBuf, "UTF-8");
                        JSONObject json = JSONObject.parseObject(recv);
                        if(json!=null){
                            TestInfo info = new TestInfo();
                            int type = json.getIntValue(SystemSnap.SNAP_CMD_TYPE_NAME);
                            if (type == SystemSnap.SNAP_TEST_REQ) {
                                int isAuto = json.getIntValue(SystemSnap.SNAP_AUTOTEST_NAME);
                                int isRealTime = json.getIntValue(SystemSnap.SNAP_REALTIME_NAME);
                                int timeUnit = json.getIntValue(SystemSnap.SNAP_TIMEUNIT_NAME);
                                int testMode = json.getIntValue(SystemSnap.SNAP_TEST_MODE_NAME);
                                info.isAutoTest = isAuto == 1;

                                info.isRealTimeFlash = isRealTime == 1;

                                info.timeUnit = timeUnit;

                                info.testMode = testMode;

                                synchronized(areaList){
                                    for(TestArea area:areaList){
                                        for (TestDevice dev : area.devList) {
                                            if (dev == null)
                                                break;
                                            dev.SetTestInfo(info);

                                        }
                                    }
                                }

                                if (info.isAutoTest) {
                                    StartTestTimer();
                                } else {
                                    StopTestTimer();
                                }
                            } else if (type == SystemSnap.SNAP_MMI_CALL_REQ) {
                                String devId = json.getString(SystemSnap.SNAP_DEVID_NAME);
                                for(TestArea area:areaList){
                                    for (TestDevice dev : area.devList) {
                                        if (dev.devid.compareToIgnoreCase(devId) == 0) {
                                            byte[] resBuf = dev.MakeSnap();
                                            DatagramPacket resPack = new DatagramPacket(resBuf, resBuf.length, recvPack.getAddress(), recvPack.getPort());
                                            testSocket.send(resPack);
                                        }
                                    }
                                }
                            }else if(type == SystemSnap.SNAP_CLEAN_CALL_REQ){
                                for(TestArea area:areaList){
                                    for (TestDevice dev : area.devList) {
                                        dev.CleanCall();
                                    }
                                }
                            }
                            json.clear();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }catch(Exception ee){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_ERROR,"Socket of ClientTest Snap err with %s",ee.getMessage());
                }
            }
        }
    }

    private void StartClientSnap(int port){
        if(snapThread==null){
//                testSocket = new DatagramSocket(PhoneParam.snapStartPort);
            testSocket = SystemSnap.OpenSnapSocket(port,PhoneParam.SNAP_MMI_GROUP);
            if(testSocket!=null){
                snapThread = new ClientSnapThread();
                snapThread.start();
            }


        }
    }

    public ClientTest(){

        areaList = new ArrayList<>();
    }

    private void StartTestTimer(){
        if(!isTestFlag){
            isTestFlag = true;
            testStartTime = System.currentTimeMillis();
        }
    }

    private void StopTestTimer(){
        if(isTestFlag)
            isTestFlag = false;
    }

    public String GetOtherAreaId(String areaId){
        String otherAreaId = "";

        synchronized(areaList){
            for(TestArea area:areaList){
                if(area.areaId.compareToIgnoreCase(areaId)!=0){
                    otherAreaId = area.areaId;
                    break;
                }
            }
        }

        return otherAreaId;
    }

    public String[] GetAreaList(){
        String[] nameList=null;
        int iTmp;
        synchronized(areaList){
            if(areaList.size()>0){
                nameList = new String[areaList.size()];
                for(iTmp=0;iTmp<areaList.size();iTmp++){
                    nameList[iTmp] = areaList.get(iTmp).areaId;
                }
            }
        }

        return nameList;
    }

    public String[] GetCurAreaDevList(){
        String[] nameList=null;
        TestArea area;
        TestDevice dev;
        int iTmp;

        synchronized(areaList){
            if(selectArea<areaList.size()){
                area = areaList.get(selectArea);
                if(area.devList.size()>0){
                    nameList = new String[area.devList.size()];
                    for(iTmp=0;iTmp<area.devList.size();iTmp++){
                        dev  = area.devList.get(iTmp);
                        nameList[iTmp] = UserInterface.GetDeviceTypeName(dev.type) + "  "+dev.devid;
                    }
                }
            }
        }

        return nameList;
    }

    public static void StopTest(String reason){
        if(testSocket!=null){
            if(!testSocket.isClosed()){
                String stopCmd = "{\"type\":1,\"autoTest\":0,\"realTime\":1,\"timeUnit\":10,\"reason\":\""+reason+"\"}";
                byte[] sendBuf = stopCmd.getBytes();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DatagramPacket packet;
                        try {
                            packet = new DatagramPacket(sendBuf,sendBuf.length, InetAddress.getByName("255.255.255.255"),PhoneParam.snapStartPort);
                            testSocket.send(packet);
                            packet = new DatagramPacket(sendBuf,sendBuf.length,InetAddress.getByName("255.255.255.255"),PhoneParam.DEFAULT_SNAP_PORT);
                            testSocket.send(packet);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }

    public int CreateClientDevice(){
        int iTmp;
        TestArea matchedArea;
        int deviceNum;
        UserDevice dev;
        TestDevice device;
        int num = 0;

        deviceNum = PhoneParam.deviceList.size();
        if(PhoneParam.clientActive){
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Begin Add Client Device");
            synchronized(areaList){
                for(iTmp=0;iTmp<deviceNum;iTmp++){
                    dev = PhoneParam.deviceList.get(iTmp);
                    matchedArea = null;
                    if(!dev.areaId.isEmpty()){
                        device = new TestDevice(dev.type, dev.devid,dev.netMode);
                       for (TestArea area : areaList) {
                            if (area.areaId.compareToIgnoreCase(dev.areaId) == 0) {
                                matchedArea = area;
                                break;
                            }
                        }
                        if (matchedArea == null) {
                            matchedArea = new TestArea(dev.areaId);
                            areaList.add(matchedArea);
                        }
                        matchedArea.AddTestDevice(device);
                        device.StartDevice();
                        num++;
                    }
                }
            }
            LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Finished Add %d Client Device",deviceNum);

        }
        return num;
    }

    public void ChangeSelectArea(int area){
        selectArea = area;
        selectDevice = 0;
    }

    public void ChangeSelectDevice(int device){
        selectDevice = device;
    }

    public TestDevice GetCurTestDevice(){
        TestDevice dev = null;
        TestArea area = null;
        synchronized(areaList){
            if(selectArea<areaList.size()) {
                area = areaList.get(selectArea);
                if (area != null){
                    if(selectDevice<area.devList.size()){
                        dev = area.devList.get(selectDevice);
                    }
                }
            }
        }
        return  dev;
    }


    public boolean ProcessMessage(int type , UserMessage msg){
        boolean matchedCurDev = false;
        boolean findMatched = false;
        boolean testResult = false;
        int devPos;
        int areaPos;
        TerminalStatistics terminalstatics;
        BackEndStatistics backEndStatics;

        if(type==UserMessage.MESSAGE_CALL_INFO
            || type == UserMessage.MESSAGE_REG_INFO
            || type == UserMessage.MESSAGE_DEVICES_INFO
            || type == UserMessage.MESSAGE_CONFIG_INFO
            || type == UserMessage.MESSAGE_SYSTEM_CONFIG_INFO
            || type == UserMessage.MESSAGE_TRANSFER_INFO
            || type == UserMessage.MESSAGE_LISTEN_CALL_INFO
            || type == UserMessage.MESSAGE_VIDEO_INFO) {
            areaPos = 0;
            String devId = msg.devId;

            synchronized(areaList){
                for (TestArea area : areaList) {
                    devPos = 0;
                    for (TestDevice dev : area.devList) {
                        if (dev.devid.compareToIgnoreCase(devId) == 0) {
                            switch (type) {
                                case UserMessage.MESSAGE_CALL_INFO:
                                    String callResult = dev.UpdateCallInfo((UserCallMessage)msg);
                                    if(!callResult.isEmpty())
                                        StopTest(callResult);
                                    break;
                                case UserMessage.MESSAGE_REG_INFO:
                                    UserRegMessage regMsg =(UserRegMessage)msg ;
                                    dev.UpdateRegisterInfo(regMsg);
                                    if(regMsg.isReg){
                                        StartClientSnap(regMsg.snapPort);
                                    }
                                    
                                    break;
                                case UserMessage.MESSAGE_DEVICES_INFO:
                                    dev.UpdateDeviceList((UserDevsMessage)msg);
                                    break;
                                case UserMessage.MESSAGE_CONFIG_INFO:
                                    dev.UpdateConfig((UserConfigMessage)msg);
                                    break;
                                case UserMessage.MESSAGE_SYSTEM_CONFIG_INFO:
                                    // do nothing
                                    break;
                                case UserMessage.MESSAGE_TRANSFER_INFO:
                                    dev.UpdateTransferInfo((TransferMessage)msg);
                                    break;
                                case UserMessage.MESSAGE_LISTEN_CALL_INFO:
                                    dev.UpdateListenInfo((ListenCallMessage)msg);
                                    break;
                                case UserMessage.MESSAGE_VIDEO_INFO:
                                    dev.UpdateVideoState((UserVideoMessage)msg);
                                    break;
                            }
                            findMatched = true;
                            break;
                        }
                        devPos++;
                    }
                    if (findMatched) {
                        if (devPos==selectDevice&&areaPos == selectArea){
                            matchedCurDev = true;
                        }
                        break;
                    }
                    areaPos++;
                }
            }
        }else if(type == UserMessage.MESSAGE_TEST_TICK){
            areaPos = 0;
            synchronized(areaList){
                for(TestArea area:areaList){
                    devPos = 0;
                    for(TestDevice dev:area.devList){
                        testResult = dev.TestProcess();
                        if(areaPos==selectArea&&devPos == selectDevice){
                            matchedCurDev = testResult;
                        }
                        devPos++;
                    }
                    areaPos++;
                }
            }
//            printSkip--;
            if(printSkip==0) {
                printSkip = 10;
                terminalstatics = UserInterface.GetTerminalStatistics();
                LogWork.Print(LogWork.TERMINAL_USER_MODULE, LogWork.LOG_DEBUG, "Terminal Has %d Call, %d Trans, %d Dev RegSucc, %d Dev RegFail",terminalstatics.callNum,terminalstatics.transNum,terminalstatics.regSuccDevNum,terminalstatics.regFailDevNum);
                backEndStatics = UserInterface.GetBackEndStatistics();
                LogWork.Print(LogWork.TERMINAL_USER_MODULE, LogWork.LOG_DEBUG, "BackEnd Has %d CallConvergence, %d Trans, %d Dev RegSucc, %d Dev RegFail",backEndStatics.callConvergenceNum,backEndStatics.transNum,backEndStatics.regSuccDevNum,backEndStatics.regFailDevNum);
            }
        }

        return matchedCurDev;
    }

}
