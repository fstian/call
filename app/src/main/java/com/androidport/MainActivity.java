package com.androidport;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.androidport.port.CallMsgReceiver;
import com.androidport.port.TerminalUserMsgReceiver;
import com.example.nettytest.R;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.terminal.test.ClientTest;
import com.example.nettytest.terminal.test.ServerTest;
import com.example.nettytest.terminal.test.TestDevice;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.userinterface.UserMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;


public class    MainActivity extends AppCompatActivity {


    final static int SELECT_ITEM_HEIGHT = 35;

    boolean isUIActive = false;


    boolean isGuiInit = false;

    static ClientTest clientTest  = null;
    static ServerTest serverTest = null;
    static boolean isAudioTestCreate = false;

    Handler terminalCallMessageHandler = null;
    Timer uiUpdateTimer = null;

    NetworkStateChangedReceiver wifiReceiver = null;

    private void CreateAudioTest(){

        if(!isAudioTestCreate){
            new Thread("InitDevices"){
                @Override
                public void run() {
                    isAudioTestCreate = true;

                    UserInterface.PrintLog("Begin Init Audio Test");

                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Begin Read Params");
//                    PhoneParam.InitPhoneParam("/sdcard/","devConfig.conf");
                    PhoneParam.InitPhoneParam(Environment.getExternalStorageDirectory().getPath(),"devConfig.conf");
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Finished Read Params");

                    clientTest = new ClientTest();
                    serverTest = new ServerTest();

                    new CallMessageProcess().start();

                    InitClientDevice();

                    InitServerDevice();

                    isUIActive = true;

                    if(terminalCallMessageHandler!=null){
                        Message initMsg = terminalCallMessageHandler.obtainMessage();
                        initMsg.arg1 = UserMessage.MESSAGE_INIT_FINISHED;
                        initMsg.obj = null;
                        terminalCallMessageHandler.sendMessage(initMsg);
                    }
                    
                    LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Finished Init Audio Test");
                }
            }.start();
        }
    }


    public void FunctionTest(){
        TestDevice dev;
        dev = clientTest.GetCurTestDevice();
        if(dev!=null){
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
                    if(dev.transferAreaId.isEmpty()){
                        String transferAreaId = clientTest.GetOtherAreaId(dev.areaId);
                        if(!transferAreaId.isEmpty()) {
                            UserInterface.SetTransferCall(dev.devid, transferAreaId, true);
                            UserInterface.PrintLog("Dev %s Set Transfer to %s", dev.devid, transferAreaId);
                        }
                    }else{
                        UserInterface.SetTransferCall(dev.devid, "", false);
                        UserInterface.PrintLog("Dev %s Clear Transfer Status",dev.devid);
                    }
                }
            }else if(dev.type==UserInterface.CALL_BED_DEVICE){
                boolean listenCall = dev.bedlistenCalls;
                UserInterface.SetListenCall(dev.devid, !listenCall);
            }

        }
    }

    private void InitServerDevice(){
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Begin Start Call Server");
        serverTest.CreateServerDevice();
    }

    private void InitClientDevice(){

        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Begin Add Client Device");
        clientTest.CreateClientDevice();
        clientTest.ChangeSelectArea(0);
        clientTest.ChangeSelectDevice(0);
        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_DEBUG,"Finished Add Client Device");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        UserInterface.PrintLog("screen onCreate with %d",getResources().getConfiguration().orientation);
        if(getResources().getConfiguration().orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }else {

            CreateAudioTest();

            uiUpdateTimer = new Timer("UiUpdateTimer");
            uiUpdateTimer.schedule(new TimerTask() {
                @SuppressLint("DefaultLocale")
                @Override
                public void run() {
                    runOnUiThread(() -> {
                        TextView tv;
                        if(clientTest!=null) {
                            if (isUIActive) {
                                tv = findViewById(R.id.runTimeId);
                                if (clientTest.isTestFlag) {
                                    long testTime = System.currentTimeMillis() - clientTest.testStartTime;
                                    testTime = testTime / 1000;
                                    tv.setText(String.format("R: %d-%02d:%02d:%02d", testTime / 86400, (testTime % 86400) / 3600, (testTime % 3600) / 60, testTime % 60));
                                } else {
                                    tv.setText("");
                                }
                            }
                        }
                        tv = findViewById(R.id.audioOwnerId);
                        String audioOwner = TerminalUserMsgReceiver.GetAudioOwner();
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

        }

        TextView tv = findViewById(R.id.deviceStatusId);

        tv.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                boolean result;
                float y = motionEvent.getY();
                if(clientTest!=null) {
                    TestDevice dev = clientTest.GetCurTestDevice();
//                    TestDevice otherDev = clientTest.GetOtherDevice();

                    if (dev != null) {

                        synchronized (MainActivity.class) {
                            result = dev.Operation(0,  (int) y/SELECT_ITEM_HEIGHT);
                        }
                        if (result)
                            UpdateHMI();

//                        if(dev.type==UserInterface.CALL_NURSER_DEVICE) {
//                            if(otherDev!=null){
//                                synchronized (MainActivity.class) {
//                                    result = otherDev.Operation(0,  (int) y/SELECT_ITEM_HEIGHT);
//                                }
//                            }
//                        }
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
                float y = motionEvent.getY();
                if(clientTest!=null) {
                    TestDevice dev = clientTest.GetCurTestDevice();
                    if (dev != null) {
                        synchronized (MainActivity.class) {
                            result = dev.Operation(1,  (int) y/SELECT_ITEM_HEIGHT);
                        }
                        if (result)
                            UpdateHMI();
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
        bt.setOnClickListener(view -> {
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
            if (clientTest != null) {
                FunctionTest();
                TestDevice dev = clientTest.GetCurTestDevice();
                if(dev.type == UserInterface.CALL_BED_DEVICE ){

//                            TestDevice newDevice;
//                            String oldDevId = audioTest.curDevice.id;
//                            long idValue = Long.parseLong(audioTest.curDevice.id,16);
//                            idValue++;
//                            UserInterface.RemoveDevice(audioTest.curDevice.id);
//                            newDevice = new TestDevice(UserInterface.CALL_BED_DEVICE,String.format("%X",idValue));
//                            ReplaceDevice(oldDevId,newDevice);
                }else if(dev.type == UserInterface.CALL_DOOR_DEVICE||
                        dev.type == UserInterface.CALL_NURSER_DEVICE){
//                            audioTest.curDevice.SaveCallRecord();
                }

            }
//            ComponentName compName= new ComponentName(this,Admin);
//            DevicePolicyManager devicePolicy = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                devicePolicy.lockNow(0);
//            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiReceiver = new NetworkStateChangedReceiver();
        registerReceiver(wifiReceiver,filter);

        //StartUdpRecvTest();
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

       isUIActive = false;
       UserInterface.PrintLog("screen OnDestory");
    }

    private void InitGui(){

        runOnUiThread(()->{
            Spinner spinner;
            String[] arr;
            ArrayAdapter<String> adapter;
            spinner = findViewById(R.id.areaSelectId);

            arr = clientTest.GetAreaList();

            if(arr!=null) {

                adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arr);

                spinner.setAdapter(adapter);

                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        clientTest.ChangeSelectArea(i);
                        UpdateArea();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                UpdateArea();
            }

            spinner = findViewById(R.id.deviceSelectId);

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    clientTest.ChangeSelectDevice(i);
                    TestDevice dev = clientTest.GetCurTestDevice();
                    if(dev!=null){
                        UpdateHMI();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }

            });
        });

    }

    private void UpdateArea(){
        Spinner spinner;
        String[] arr;
        ArrayAdapter<String> adapter;

        spinner = findViewById(R.id.deviceSelectId);

        arr = clientTest.GetCurAreaDevList();

        if(arr==null){
            spinner.removeAllViews();
        }else{

            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arr);

            spinner.setAdapter(adapter);

            UpdateHMI();

        }

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
                boolean result;

                if(msgType==UserMessage.MESSAGE_INIT_FINISHED&& !isGuiInit) {
                    InitGui();
                    isGuiInit = true;
                }
                synchronized(MainActivity.class) {
                    if (clientTest == null)
                        return false;

                    result = clientTest.ProcessMessage(msgType,terminalMsg);

                }

                if(result) {
                    if(msgType==UserMessage.MESSAGE_DEVICES_INFO)
                        UpdateNurserHMI();
                    else
                        UpdateHMI();
                }
                return false;
            });

            CallMsgReceiver.SetMessageHandler(terminalCallMessageHandler);
            Looper.loop();
            UserInterface.PrintLog("CallMessageProcess Exit!!!!!");
        }
    }

    private void UpdateNurserHMI(){
        if(isUIActive) {
            runOnUiThread(() -> {
                synchronized (MainActivity.class) {
                    TestDevice dev = clientTest.GetCurTestDevice();
                    if(dev!=null) {
                        TextView tv = findViewById(R.id.deviceStatusId);
                        if (dev.type == UserInterface.CALL_NURSER_DEVICE) {
                            String status;
                            status = dev.GetNurserDeviceInfo();
                            tv.setText(status);
                        }
                    }
                }
            });
        }
    }

    private void UpdateHMI(){
        if(isUIActive) {
            runOnUiThread(() -> {
                String status;
                synchronized (MainActivity.class) {
                    TestDevice dev = clientTest.GetCurTestDevice();
                    TextView tv = findViewById(R.id.deviceStatusId);
                    status = dev.GetDeviceInfo();

                    tv.setText(status);

                    if (dev.type == UserInterface.CALL_NURSER_DEVICE) {
                        dev.QueryDevs();
                    }

                    tv = findViewById(R.id.callListId);
                    status = dev.GetCallInfo();
                    tv.setText(status);
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
                        LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Disabled!!");
                        break;
                    case WifiManager.WIFI_STATE_DISABLING:
                        LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Disabling!!");
                        break;
                    case WifiManager.WIFI_STATE_ENABLED:
                        LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Enabled!!");
                        break;
                    case WifiManager.WIFI_STATE_ENABLING:
                        LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Enabling!!");
                        break;
                }
            }
            if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                NetworkInfo.State state = networkInfo.getState();
                if(state ==NetworkInfo.State.CONNECTING){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Connecting!!");
                }else if(state == NetworkInfo.State.CONNECTED){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Connected!!");
                }else if(state == NetworkInfo.State.DISCONNECTING){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Disconnecting!!");
                }else if(state == NetworkInfo.State.DISCONNECTED){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Disconnected!!");
                }else if(state == NetworkInfo.State.SUSPENDED){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Suspended!!");
                }else if(state == NetworkInfo.State.UNKNOWN){
                    LogWork.Print(LogWork.TERMINAL_USER_MODULE,LogWork.LOG_INFO,"Wifi Network is Unknow!!");
                }
            }
        }
    }
    DatagramSocket testsocket;
    void StartUdpRecvTest(){
        new Thread(){
            @Override
            public void run() {

            while(true){
                System.out.println("qkq test Do UDP socket Test ");
                try {
                    testsocket = new  DatagramSocket(19900);
                    testsocket.setSoTimeout(1000);
                    new Thread(){
                        @Override
                        public void run() {
                            testsocket.close();
                        }
                    }.start();
                    new Thread(){
                        @Override
                        public void run() {
                            byte[] recvBuf=new byte[1024];
                            DatagramPacket pack = new DatagramPacket(recvBuf,recvBuf.length);
                            try {
                                testsocket.receive(pack);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                } catch (SocketException e) {
                    e.printStackTrace();
                }catch(Exception ee){
                    UserInterface.PrintLog("Socket of MMI Snap err with %s",ee.getMessage());
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            }
        }.start();
    }
}
