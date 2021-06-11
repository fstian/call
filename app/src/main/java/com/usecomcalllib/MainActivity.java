package com.usecomcalllib;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nettytest.R;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.userinterface.CallLogMessage;
import com.example.nettytest.userinterface.ListenCallMessage;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TestInfo;
import com.example.nettytest.userinterface.TransferMessage;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserConfig;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserVideoMessage;
import com.usecomcalllib.androidPort.CallMsgReceiver;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;
import com.usecomcalllib.androidTest.TestDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {



    private class AudioTest{

        TestDevice[] testDevices;
        TestDevice curDevice;

        DatagramSocket testSocket;

        long testStartTime;
        boolean isTestFlag = false;

        boolean isUIActive = false;
    }

    int iTestCount = 0;
    String audioTestId="";

    static AudioTest audioTest = null;
    static boolean isAudioTestCreate = false;

    Handler terminalCallMessageHandler = null;
    Timer uiUpdateTimer = null;
    NetworkStateChangedReceiver wifiReceiver = null;
    int testCount = 0;

    private void CreateAudioTest(){
        if(!isAudioTestCreate){
            System.out.println(String.format("screen CreateAudioTest create server and clients"));
            audioTest = new AudioTest();
            isAudioTestCreate = true;

            InitAudioDevice();
            InitServer();

            new CallMessageProcess().start();
        }
    }

    private void ReplaceDevice(String id,TestDevice dev){
        for(int iTmp=0;iTmp<audioTest.testDevices.length;iTmp++){
            if(audioTest.testDevices[iTmp].devid.compareToIgnoreCase(id)==0){
                if(audioTest.curDevice == audioTest.testDevices[iTmp]) {
                    audioTest.curDevice = dev;
                }
                audioTest.testDevices[iTmp] = dev;
            }
        }
    }


    public void FunctionTest(){
        TestDevice dev;
        String curAreaId;
        String transferAreaId = "";
        boolean listenCall = false;
        if(audioTest.curDevice!=null){
            dev = audioTest.curDevice;
            if(dev.type==UserInterface.CALL_NURSER_DEVICE){
                if(dev.IsTalking()){
                    if(dev.isVideo){
                        UserInterface.PrintLog("Dev %s Stop Video", dev.devid);
                        dev.StopVideo();
                    }else{
                        UserInterface.PrintLog("Dev %s Start Video", dev.devid);
                        dev.StartVideo();
                    }
                }else{
                    curAreaId = dev.areaId;
                    if(dev.transferAreaId.isEmpty()){
                        for(TestDevice checkDev:audioTest.testDevices){
                            if(curAreaId.compareToIgnoreCase(checkDev.areaId)!=0){
                                if(!checkDev.areaId.isEmpty()){
                                    transferAreaId = checkDev.areaId;
                                    break;
                                }
                            }
                        }
                        if(!transferAreaId.isEmpty()) {
                            UserInterface.CallTransfer(dev.devid, transferAreaId, true);
                            UserInterface.PrintLog("Dev %s Set Transfer to %", dev.devid, transferAreaId);
                        }
                    }else{
                        UserInterface.CallTransfer(dev.devid, "", false);
                        UserInterface.PrintLog("Dev %s Clear Transfer Status",dev.devid);
                    }
                }
            }else if(dev.type==UserInterface.CALL_BED_DEVICE){
                listenCall = dev.bedlistenCalls;
                if(listenCall){
                    UserInterface.SetBedListenCall(dev.devid,false);
                }else{
                    UserInterface.SetBedListenCall(dev.devid,true);
                }
            }

        }
    }

    private void InitAudioDevice(){
        PhoneParam.InitPhoneParam("/sdcard/","devConfig.conf");
        int deviceNum = PhoneParam.deviceList.size();
        int iTmp;
        if(deviceNum>=1) {
            audioTest.testDevices = new TestDevice[deviceNum];

            for (iTmp = 0; iTmp < deviceNum; iTmp++) {
                System.out.println("deviceList has "+PhoneParam.deviceList.size()+" items, try to get "+iTmp+ " item");
                UserDevice dev = PhoneParam.deviceList.get(iTmp);
                audioTest.testDevices[iTmp] = new TestDevice(dev.type, dev.devid,dev.netMode);
            }
        }else{
            audioTest.testDevices = new TestDevice[1];
            audioTest.testDevices[0] = new TestDevice(UserInterface.CALL_BED_DEVICE,"20105101");
        }
        audioTest.curDevice = audioTest.testDevices[0];
        try {
            audioTest.testSocket = new DatagramSocket(PhoneParam.snapStartPort);
        }catch (SocketException e){
            e.printStackTrace();
        }

        new Thread("UserSnapThread"){
            @Override
            public void run() {
                byte[] recvBuf = new byte[1024];
                DatagramPacket recvPack;
                while (!audioTest.testSocket.isClosed()) {
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        audioTest.testSocket.receive(recvPack);
                        if (recvPack.getLength() > 0) {
                            String recv = new String(recvBuf, "UTF-8");
                            JSONObject json = new JSONObject(recv);
                            TestInfo info = new TestInfo();
                            int type = json.optInt(SystemSnap.SNAP_CMD_TYPE_NAME);
                            if (type == SystemSnap.SNAP_TEST_REQ) {
                                int isAuto = json.optInt(SystemSnap.SNAP_AUTOTEST_NAME);
                                int isRealTime = json.optInt(SystemSnap.SNAP_REALTIME_NAME);
                                int timeUnit = json.optInt(SystemSnap.SNAP_TIMEUNIT_NAME);
                                info.isAutoTest = isAuto == 1;

                                info.isRealTimeFlash = isRealTime == 1;

                                info.timeUnit = timeUnit;

                                for (TestDevice dev : audioTest.testDevices) {
                                    if (dev == null)
                                        break;
                                    dev.SetTestInfo(info);

                                }

                                if (info.isAutoTest) {
                                    StartTestTimer();
                                } else {
                                    StopTestTimer();
                                }
                            } else if (type == SystemSnap.SNAP_MMI_CALL_REQ) {
                                String devId = json.optString(SystemSnap.SNAP_DEVID_NAME);
                                for (TestDevice dev : audioTest.testDevices) {
                                    if (dev == null)
                                        break;
                                    if (dev.devid.compareToIgnoreCase(devId) == 0) {
                                        byte[] resBuf = dev.MakeSnap();
//                                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Get User Call Snap for dev %s, total %d bytes, send to %s:%d",devId,resBuf.length,recvPack.getAddress().getHostName(),recvPack.getPort());
                                        DatagramPacket resPack = new DatagramPacket(resBuf, resBuf.length, recvPack.getAddress(), recvPack.getPort());
                                        audioTest.testSocket.send(resPack);
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        System.out.println(String.format("screen onCreate with %d",getResources().getConfiguration().orientation));
        if(getResources().getConfiguration().orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else {

            CreateAudioTest();

            InitGui();

            uiUpdateTimer = new Timer("UiUpdateTimer");
            uiUpdateTimer.schedule(new TimerTask() {
                @SuppressLint("DefaultLocale")
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        TextView tv;
                        if (audioTest.isUIActive) {
                            tv = findViewById(R.id.runTimeId);
                            if (audioTest.isTestFlag) {
                                long testTime = System.currentTimeMillis() - audioTest.testStartTime;
                                testTime = testTime / 1000;
                                tv.setText(String.format("R: %d-%02d:%02d:%02d", testTime / 86400, (testTime % 86400) / 3600, (testTime % 3600) / 60, testTime % 60));
                            } else {
                                tv.setText("");
                            }
                        }
                        tv = findViewById(R.id.audioOwnerId);
                        String audioOwner = CallMsgReceiver.GetAudioOwner();
                        if (audioOwner.isEmpty()) {
                            tv.setText("Audio is Free");
                        } else {
                            tv.setText(String.format("Audio Owner is %s", audioOwner));
                        }
                        tv = findViewById(R.id.statisticsId);
                        tv.setText(String.format("B(C=%d,T=%d),T(C=%d,T=%d)", HandlerMgr.GetBackCallCount(), HandlerMgr.GetBackTransCount(), HandlerMgr.GetTermCallCount(), HandlerMgr.GetTermTransCount()));
                    });
                }
            }, 0, 1000);

            audioTest.isUIActive = true;
        }

        TextView tv = findViewById(R.id.deviceStatusId);

        tv.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                boolean result;
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if(audioTest!=null) {
                    if (audioTest.curDevice != null) {
                        synchronized (MainActivity.class) {
                            result = audioTest.curDevice.Operation(0, (int) x, (int) y);
                        }
                        if (result)
                            UpdateHMI(audioTest.curDevice);
                    }
                }
            }
            return true;
        });


        tv = findViewById(R.id.callListId);

        tv.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if(action==MotionEvent.ACTION_DOWN){
                boolean result;
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if(audioTest!=null) {
                    if (audioTest.curDevice != null) {
                        synchronized (MainActivity.class) {
                            result = audioTest.curDevice.Operation(1, (int) x, (int) y);
                        }
                        if (result)
                            UpdateHMI(audioTest.curDevice);
                    }

// test for all device answer one call
//                    for(TestDevice dev:audioTest.testDevices){
//                        synchronized (MainActivity.class){
//                            result = dev.Operation(1,(int)x,(int)y);
//                        }
//                        if(result){
//                            if(audioTest.curDevice==dev){
//                                UpdateHMI(dev);
//                            }
//                        }
//                    }
                }
            }
            return true;
        });

        Button bt = (Button)findViewById(R.id.increaseId);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if(testCount==0) {
//                    testCount = 1;
//                    audioTestId = AudioMgr.OpenAudio("201051A1", 9090, 9092, "172.16.2.79", 8000, 20, Rtp.RTP_CODEC_711A, AudioDevice.SEND_RECV_MODE);
//                }else {
//                    new Thread() {
//                        @Override
//                        public void run() {
//                            AudioMgr.CloseAudio(audioTestId);
//                            audioTestId = AudioMgr.OpenAudio("201051A1", 9090, 9092, "172.16.2.79", 8000, 20, Rtp.RTP_CODEC_711A, AudioDevice.SEND_RECV_MODE);
//                        }
//                    }.start();
//                }
//                bt.setText("Audio test");

//                if(testCount==0) {
//                    audioTestId = AudioMgr.OpenAudio("201051A1", 9090, 9092, "172.16.2.79", 8000, 20, Rtp.RTP_CODEC_711A, AudioDevice.SEND_RECV_MODE);
//                    testCount = 1;
//                    bt.setText("Audio Start");
//                }else{
//                    AudioMgr.CloseAudio(audioTestId);
//                    testCount = 0;
//                    bt.setText("Audio Stop");
//                }
//                AudioMgr.CloseAudio(audioId);
//                if(testCount==0){
//                    testCount = 1;
////                    SerialPort.SetGpioStatus(45,1);
////                    bt.setText("MIC Hand");
//                }else if(testCount==1){
//                    testCount = 2;
////                    SerialPort.SetGpioStatus(45,0);
////                    bt.setText("MIC Main");
//                }else if(testCount==2){
//                    testCount = 3;
////                    bt.setText("MIC Off");
////                    AudioManager audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
////                    audioManager.setMicrophoneMute(false);
//                }else if(testCount==3){
//                    testCount = 0;
////                    bt.setText("MIC On");
////                    AudioManager audioManager=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
////                    audioManager.setMicrophoneMute(true);
//                }


//                UserInterface.RemoveAllDeviceOnServer();
                if (audioTest != null) {
                    FunctionTest();
                    if (audioTest.curDevice != null) {
                        if(audioTest.curDevice.type == UserInterface.CALL_BED_DEVICE ){

//                            TestDevice newDevice;
//                            String oldDevId = audioTest.curDevice.id;
//                            long idValue = Long.parseLong(audioTest.curDevice.id,16);
//                            idValue++;
//                            UserInterface.RemoveDevice(audioTest.curDevice.id);
//                            newDevice = new TestDevice(UserInterface.CALL_BED_DEVICE,String.format("%X",idValue));
//                            ReplaceDevice(oldDevId,newDevice);
                        }else if(audioTest.curDevice.type == UserInterface.CALL_DOOR_DEVICE||
                                audioTest.curDevice.type == UserInterface.CALL_NURSER_DEVICE){
//                            audioTest.curDevice.SaveCallRecord();
                        }

                    }
                }
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiReceiver = new NetworkStateChangedReceiver();
        registerReceiver(wifiReceiver,filter);
    }

    private void StartTestTimer(){
        if(!audioTest.isTestFlag){
            audioTest.testStartTime = System.currentTimeMillis();
            audioTest.isTestFlag = true;
        }
    }

    private void StopTestTimer(){
        audioTest.isTestFlag = false;
    }

    public static void StopTest(String reason){
        if(audioTest.testSocket!=null){
            if(!audioTest.testSocket.isClosed()){
                String stopCmd = "{\"type\":1,\"autoTest\":0,\"realTime\":1,\"timeUnit\":10,\"reason\":\""+reason+"\"}";
                byte[] sendBuf = stopCmd.getBytes();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DatagramPacket packet;
                        try {
                            packet = new DatagramPacket(sendBuf,sendBuf.length,InetAddress.getByName("255.255.255.255"),PhoneParam.snapStartPort);
                            audioTest.testSocket.send(packet);
                            packet = new DatagramPacket(sendBuf,sendBuf.length,InetAddress.getByName("255.255.255.255"),PhoneParam.DEFAULT_SNAP_PORT);
                            audioTest.testSocket.send(packet);
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

    private void InitServer(){
        ServerDeviceInfo devInfo = new ServerDeviceInfo();
        ArrayList<UserConfig> paramList = new ArrayList<>();

        UserConfig param = new UserConfig();
        param.param_id="iDJSYSM";
        param.param_name="ddd";
        param.param_value = "2000";
        param.param_unit = "time";
        paramList.add(param);

        param = new UserConfig();
        param.param_id="CallAlert";
        param.param_name="ffff";
        param.param_value = "1";
        param.param_unit = "0/1";
        paramList.add(param);

        if(PhoneParam.serverActive) {
            UserInterface.StartServer();
            for(int iTmp=0;iTmp<PhoneParam.devicesOnServer.size();iTmp++){
                UserDevice dev = PhoneParam.devicesOnServer.get(iTmp);
                UserInterface.AddAreaInfoOnServer(dev.areaId,"");
                UserInterface.AddDeviceOnServer(dev.devid,dev.type,dev.netMode,dev.areaId);
                UserInterface.ConfigDeviceParamOnServer(dev.devid,paramList);
                devInfo.roomId = "2001";
                devInfo.bedName = "bed"+(iTmp+1);
                devInfo.deviceName = "people"+(iTmp+1);
                devInfo.areaId = dev.areaId;
                UserInterface.ConfigDeviceInfoOnServer(dev.devid,devInfo);
            }
        }

        paramList.clear();

        paramList = new ArrayList<>();
        param = new UserConfig();
        param.param_id="iaeraName";
        param.param_name="area";
        param.param_value = "daas";
        param.param_unit = "";
        paramList.add(param);

        param = new UserConfig();
        param.param_id="ihospital";
        param.param_name="hospital";
        param.param_value = "afeea";
        param.param_unit = "";
        paramList.add(param);

        UserInterface.ConfigSystemParamOnServer(paramList);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(wifiReceiver!=null)
            unregisterReceiver(wifiReceiver);

        if(terminalCallMessageHandler!=null) {
            terminalCallMessageHandler.getLooper().quit();
            terminalCallMessageHandler = null;
        }

        if(uiUpdateTimer!=null) {
            uiUpdateTimer.cancel();
            uiUpdateTimer = null;
        }
        if(audioTest!=null)
            audioTest.isUIActive = false;
        System.out.println("screen OnDestory");
    }

    private void InitGui(){
        Spinner deviceSpinner;
        int iTmp;
        String[] arr;

        deviceSpinner = findViewById(R.id.deviceSelectId);

        int deviceNum = audioTest.testDevices.length;

        arr = new String[deviceNum];

        for (iTmp = 0; iTmp < deviceNum; iTmp++) {
            System.out.println("deviceList has "+PhoneParam.deviceList.size()+" items, try to get "+iTmp+ " item");
            arr[iTmp] = UserInterface.GetDeviceTypeName(audioTest.testDevices[iTmp].type) + "    " + audioTest.testDevices[iTmp].devid;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arr);

        deviceSpinner.setAdapter(adapter);

        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i<audioTest.testDevices.length) {
                    audioTest.curDevice = audioTest.testDevices[i];
                    UpdateHMI(audioTest.curDevice);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


    }

    private class CallMessageProcess extends Thread{

        public CallMessageProcess(){
            super("UserMessageProcess");
        }

        @Override
        public void run() {
            Looper.prepare();
            terminalCallMessageHandler = new Handler(message -> {
                int msgType = message.arg1;
                UserMessage terminalMsg = (UserMessage)message.obj;
                TestDevice device=null;
                if (msgType == UserMessage.MESSAGE_CALL_INFO
                        || msgType == UserMessage.MESSAGE_REG_INFO
                        || msgType == UserMessage.MESSAGE_DEVICES_INFO
                        || msgType==UserMessage.MESSAGE_CONFIG_INFO
                        || msgType == UserMessage.MESSAGE_SYSTEM_CONFIG_INFO
                        || msgType == UserMessage.MESSAGE_TRANSFER_INFO
                        || msgType == UserMessage.MESSAGE_LISTEN_CALL_INFO
                        || msgType == UserMessage.MESSAGE_VIDEO_INFO) {

                    UserInterface.PrintLog("DEV %s Recv Msg %d(%s) ", terminalMsg.devId, terminalMsg.type, UserMessage.GetMsgName(terminalMsg.type));
                    for (TestDevice testDevice : audioTest.testDevices) {
                        if(testDevice==null)
                            break;
                        if (testDevice.devid.compareToIgnoreCase(terminalMsg.devId) == 0) {
                            device = testDevice;
                            break;
                        }
                    }
                    if (device != null) {
                        switch (msgType) {
                            case UserMessage.MESSAGE_CALL_INFO:
                                String result;
                                synchronized (MainActivity.class) {
                                    result = device.UpdateCallInfo((UserCallMessage) terminalMsg);
                                }
                                if(device==audioTest.curDevice) {
                                    if((!audioTest.isTestFlag)||device.testInfo.isRealTimeFlash)
                                        UpdateHMI(device);
                                }
                                if(!result.isEmpty()){
                                    StopTest(result);
                                }
                                break;
                            case UserMessage.MESSAGE_REG_INFO:
                                device.UpdateRegisterInfo((UserRegMessage) terminalMsg);
                                if (device == audioTest.curDevice){
                                    if((!audioTest.isTestFlag)||device.testInfo.isRealTimeFlash)
                                        UpdateHMI(device);
                                }
                                break;
                            case UserMessage.MESSAGE_TRANSFER_INFO:
                                device.UpdateTransferInfo((TransferMessage)terminalMsg);
                                if (device == audioTest.curDevice){
                                    if((!audioTest.isTestFlag)||device.testInfo.isRealTimeFlash)
                                        UpdateHMI(device);
                                }
                                break;
                            case UserMessage.MESSAGE_LISTEN_CALL_INFO:
                                device.UpdateListenInfo((ListenCallMessage) terminalMsg);
                                if (device == audioTest.curDevice){
                                    if((!audioTest.isTestFlag)||device.testInfo.isRealTimeFlash)
                                        UpdateHMI(device);
                                }
                                break;
                            case UserMessage.MESSAGE_DEVICES_INFO:
                                synchronized (MainActivity.class) {
                                    device.UpdateDeviceList((UserDevsMessage) terminalMsg);
                                }
                                if (device == audioTest.curDevice) {
                                    if(device.type==UserInterface.CALL_NURSER_DEVICE )
                                        UpdateNurserHMI(device);
                                }
                                break;
                            case UserMessage.MESSAGE_CONFIG_INFO:
                                synchronized (MainActivity.class) {
                                    device.UpdateConfig((UserConfigMessage )terminalMsg);
                                }
                                break;
                            case UserMessage.MESSAGE_SYSTEM_CONFIG_INFO:
                                UserInterface.PrintLog("Recv System Config Info");
                                break;
                            case UserMessage.MESSAGE_VIDEO_INFO:
                                synchronized (MainActivity.class) {
                                    device.UpdateVideoState((UserVideoMessage)terminalMsg);
                                }
                                break;
                        }
                    }
                } else if (msgType == UserMessage.MESSAGE_TEST_TICK) {
                    for (TestDevice testDevice : audioTest.testDevices) {
                        boolean testFlag;
                        synchronized (MainActivity.class) {
                            if(testDevice==null)
                                break;
                            testFlag = testDevice.TestProcess();
                        }
                        if(audioTest.curDevice==testDevice){
                            if (testFlag) {
                                if((!audioTest.isTestFlag)||testDevice.testInfo.isRealTimeFlash)
                                    UpdateHMI(testDevice);
                            }

                            iTestCount++;
                            if(iTestCount>20){
                                iTestCount = 0;
                                if(audioTest.isTestFlag&&(!testDevice.testInfo.isRealTimeFlash))
                                    UpdateHMI(testDevice);
                            }
                        }
                    }
                }else if(msgType == UserMessage.MESSAGE_BACKEND_CALL_LOG){
                    CallLogMessage callLog = (CallLogMessage)terminalMsg;
                    UserInterface.PrintLog("Recv Call Log , CallID = %s Type=%d direction=%d answerMode=%d ",callLog.callId,callLog.callType,callLog.callDirection,callLog.answerMode);
                }

                return false;
            });
            CallMsgReceiver.SetMessageHandler(terminalCallMessageHandler);
            CallMsgReceiver.SetBackEndMessageHandler(terminalCallMessageHandler);
            Looper.loop();
            System.out.println("CallMessageProcess Exit!!!!!");
        }
    }

    private void UpdateNurserHMI(TestDevice dev){
        if(audioTest.isUIActive) {
            runOnUiThread(() -> {
                synchronized (MainActivity.class) {
                    TextView tv = findViewById(R.id.deviceStatusId);
                    if (dev.type == UserInterface.CALL_NURSER_DEVICE) {
                        StringBuilder status;
                        status = dev.GetNurserDeviceInfo();
                        tv.setText(status.toString());
                    }
                }
            });
        }
    }

    private void UpdateHMI(TestDevice dev){
        if(audioTest.isUIActive) {
            runOnUiThread(() -> {
                StringBuilder status;
                synchronized (MainActivity.class) {
                    TextView tv = findViewById(R.id.deviceStatusId);
                    status = dev.GetDeviceInfo();

                    tv.setText(status.toString());

                    if (dev.type == UserInterface.CALL_NURSER_DEVICE) {
                        dev.QueryDevs();
                    }

                    tv = findViewById(R.id.callListId);
                    status = dev.GetCallInfo();
                    tv.setText(status.toString());
                    Button bt = findViewById(R.id.increaseId);
                    if(dev.type == UserInterface.CALL_NURSER_DEVICE){
                        if(dev.IsTalking()){
                            if(dev.isVideo)
                                bt.setText("Close Video");
                            else
                                bt.setText("Open Video");
                        }else{
                            if(dev.transferAreaId.isEmpty()){
                                bt.setText("Set Tranfer");
                            }else{
                                bt.setText("Clear Tranfer");
                            }
                        }
                    }else if(dev.type == UserInterface.CALL_BED_DEVICE){
                        if(dev.bedlistenCalls){
                            bt.setText("Clear Listen");
                        }else{
                            bt.setText("Set Listen");
                        }
                    }else{
                        bt.setText("");
                    }
                }
            });
        }
    }

    class NetworkStateChangedReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())){
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,-1);
                switch(state){
                    case WifiManager.WIFI_STATE_DISABLED:
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Disabled!!");
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Disabling!!");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Enabled!!");
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Enabling!!");
                        break;
                }
            }
            if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = networkInfo.getState();
                if(state ==NetworkInfo.State.CONNECTING){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Connecting!!");
                }else if(state == NetworkInfo.State.CONNECTED){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Connected!!");
                }else if(state == NetworkInfo.State.DISCONNECTING){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Disconnecting!!");
                }else if(state == NetworkInfo.State.DISCONNECTED){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Disconnected!!");
                }else if(state == NetworkInfo.State.SUSPENDED){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Suspended!!");
                }else if(state == NetworkInfo.State.UNKNOWN){
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_INFO,"Wifi Network is Unknow!!");
                }
            }
        }
    }
}
