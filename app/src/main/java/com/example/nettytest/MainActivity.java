package com.example.nettytest;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.nettytest.backend.backendphone.BackEndConfig;
import com.example.nettytest.pub.HandlerMgr;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.userinterface.ServerDeviceInfo;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TestInfo;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserConfig;
import com.example.nettytest.userinterface.UserConfigMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.terminal.test.LocalCallInfo;
import com.example.nettytest.terminal.test.TestDevice;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;

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

    static AudioTest audioTest = null;
    static boolean isAudioTestCreate = false;
    
    Handler terminalCallMessageHandler = null;
    Timer uiUpdateTimer = null;
    

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

    private void InitAudioDevice(){
        PhoneParam.InitPhoneParam();
        int deviceNum = PhoneParam.deviceList.size();
        int iTmp;
        if(deviceNum>=1) {
            audioTest.testDevices = new TestDevice[deviceNum];

            for (iTmp = 0; iTmp < deviceNum; iTmp++) {
                System.out.println("deviceList has "+PhoneParam.deviceList.size()+" items, try to get "+iTmp+ " item");
                UserDevice dev = PhoneParam.deviceList.get(iTmp);
                audioTest.testDevices[iTmp] = new TestDevice(dev.type, dev.devid);
            }
        }else{
            audioTest.testDevices = new TestDevice[1];
            audioTest.testDevices[0] = new TestDevice(UserInterface.CALL_BED_DEVICE,"20105101");
        }
        audioTest.curDevice = audioTest.testDevices[0];

        new Thread(() -> {
            try {
                byte[] recvBuf=new byte[1024];
                DatagramPacket recvPack;
                audioTest.testSocket = new DatagramSocket(SystemSnap.SNAP_MMI_PORT);
                while (!audioTest.testSocket.isClosed()){
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        audioTest.testSocket.receive(recvPack);
                        if(recvPack.getLength()>0){
                            String recv = new String(recvBuf,"UTF-8");
                            JSONObject json = new JSONObject(recv);
                            TestInfo info = new TestInfo();
                            int type = json.optInt(SystemSnap.SNAP_CMD_TYPE_NAME);
                            if(type == SystemSnap.SNAP_DEV_REQ) {
                                int isAuto = json.optInt(SystemSnap.SNAP_AUTOTEST_NAME);
                                int isRealTime = json.optInt(SystemSnap.SNAP_REALTIME_NAME);
                                int timeUnit = json.optInt(SystemSnap.SNAP_TIMEUNIT_NAME);
                                info.isAutoTest = isAuto == 1;

                                info.isRealTimeFlash = isRealTime == 1;

                                info.timeUnit = timeUnit;

                                for (TestDevice dev : audioTest.testDevices) {
                                    if(dev==null)
                                        break;
                                    dev.SetTestInfo(info);

                                    JSONObject resJson = new JSONObject();
                                    resJson.putOpt(SystemSnap.SNAP_CMD_TYPE_NAME,SystemSnap.SNAP_DEV_RES);
                                    resJson.putOpt(SystemSnap.SNAP_DEVID_NAME,dev.id);
                                    resJson.putOpt(SystemSnap.SNAP_DEVTYPE_NAME,dev.type);
                                    byte[] resBuf = resJson.toString().getBytes();
                                    DatagramPacket resPack= new DatagramPacket(resBuf,resBuf.length,recvPack.getAddress(),recvPack.getPort());
                                    audioTest.testSocket.send(resPack);
                                }

                                if(info.isAutoTest){
                                    StartTestTimer();
                                }else{
                                    StopTestTimer();
                                }
                            }else if(type==SystemSnap.SNAP_MMI_CALL_REQ){
                                for (TestDevice dev : audioTest.testDevices) {
                                    if(dev==null)
                                        break;
                                    byte[] resBuf = dev.MakeSnap();
                                    DatagramPacket resPack= new DatagramPacket(resBuf,resBuf.length,recvPack.getAddress(),recvPack.getPort());
                                    audioTest.testSocket.send(resPack);
                                }
                            }else if(type==SystemSnap.LOG_CONFIG_REQ_CMD){
                                LogWork.backEndNetModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_NET_NAME) == 1;

                                LogWork.backEndDeviceModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_DEVICE_NAME) == 1;

                                LogWork.backEndCallModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_CALL_NAME) == 1;

                                LogWork.backEndPhoneModuleLogEnable = json.optInt(SystemSnap.LOG_BACKEND_PHONE_NAME) == 1;

                                LogWork.terminalNetModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_NET_NAME) == 1;

                                LogWork.terminalDeviceModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_DEVICE_NAME) == 1;

                                LogWork.terminalCallModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_CALL_NAME) == 1;

                                LogWork.terminalPhoneModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_PHONE_NAME) == 1;

                                LogWork.terminalUserModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_USER_NAME) == 1;

                                LogWork.terminalAudioModuleLogEnable = json.optInt(SystemSnap.LOG_TERMINAL_AUDIO_NAME) ==1;

                                LogWork.transactionModuleLogEnable = json.optInt(SystemSnap.LOG_TRANSACTION_NAME) == 1;

                                LogWork.debugModuleLogEnable = json.optInt(SystemSnap.LOG_DEBUG_NAME) == 1;

                                LogWork.dbgLevel = json.optInt(SystemSnap.LOG_DBG_LEVEL_NAME);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }).start();

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

            uiUpdateTimer = new Timer();
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
                        String audioOwner = HandlerMgr.GetAudioOwner();
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
                }
            }
            return true;
        });


    }

    private void StartTestTimer(){
        audioTest.testStartTime = System.currentTimeMillis();
        audioTest.isTestFlag = true;
    }

    private void StopTestTimer(){
        audioTest.isTestFlag = false;
    }

    private void StopTest(){
        if(audioTest.testSocket!=null){
            if(!audioTest.testSocket.isClosed()){
                String stopCmd = "{\"autoTest\":0,\"realTime\":1,\"timeUnit\":10}";
                byte[] sendBuf = stopCmd.getBytes();
                DatagramPacket packet;
                try {
                    packet = new DatagramPacket(sendBuf,sendBuf.length, InetAddress.getByName("255.255.255.255"),10005);
                    audioTest.testSocket.send(packet);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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
                UserInterface.AddDeviceOnServer(dev.devid,dev.type);
                UserInterface.ConfigDeviceParamOnServer(dev.devid,paramList);
                devInfo.roomId = "2001";
                devInfo.bedName = "bed"+(iTmp+1);
                devInfo.deviceName = "people"+(iTmp+1);
                UserInterface.ConfigDeviceInfoOnServer(dev.devid,devInfo);
                UserInterface.ConfigServerParam(new BackEndConfig());
            }
        }

        paramList.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
/*
        if(terminalCallMessageHandler!=null) {
            terminalCallMessageHandler.getLooper().quit();
            terminalCallMessageHandler = null;
        }
 */
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
            arr[iTmp] = UserInterface.GetDeviceTypeName(audioTest.testDevices[iTmp].type) + "    " + audioTest.testDevices[iTmp].id;
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

        @Override
        public void run() {
            Looper.prepare();
            terminalCallMessageHandler = new Handler(message -> {
                int msgType = message.arg1;
                UserMessage terminalMsg = (UserMessage)message.obj;
                TestDevice device=null;
                if (msgType == UserMessage.MESSAGE_CALL_INFO || msgType == UserMessage.MESSAGE_REG_INFO || msgType == UserMessage.MESSAGE_DEVICES_INFO||msgType==UserMessage.MESSAGE_CONFIG_INFO) {

                    UserInterface.PrintLog("DEV %s Recv Msg %d(%s) ", terminalMsg.devId, terminalMsg.type, UserMessage.GetMsgName(terminalMsg.type));
                    for (TestDevice testDevice : audioTest.testDevices) {
                        if(testDevice==null)
                            break;
                        if (testDevice.id.compareToIgnoreCase(terminalMsg.devId) == 0) {
                            device = testDevice;
                            break;
                        }
                    }
                    if (device != null) {
                        switch (msgType) {
                            case UserMessage.MESSAGE_CALL_INFO:
                                int result;
                                synchronized (MainActivity.class) {
                                    result = device.UpdateCallInfo((UserCallMessage) terminalMsg);
                                }
                                if(device==audioTest.curDevice)
                                    UpdateHMI(device);
                                if(result<0){
                                    StopTest();
                                }
                                break;
                            case UserMessage.MESSAGE_REG_INFO:
                                device.UpdateRegisterInfo((UserRegMessage) terminalMsg);
                                if (device == audioTest.curDevice)
                                    UpdateHMI(device);
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
                                UpdateHMI(audioTest.curDevice);
                            }

                            //                            iTestCount++;
                            if(iTestCount>20){
                                iTestCount = 0;
//                                UpdateHMI(curDevice);
                            }
                        }
                    }
                }

                return false;
            });
            UserInterface.SetMessageHandler(terminalCallMessageHandler);
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
                        if (dev.devLists == null)
                            UserInterface.PrintLog("Recv DEV_REQ for %s, Has NO bed Device", dev.id);
                        else
                            UserInterface.PrintLog("Recv DEV_REQ for %s, Has %d bed Device", dev.id, dev.devLists.size());
                        if (dev.isCallOut) {
                            if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                                status = new StringBuilder(String.format("%s Call to %s\r", audioTest.curDevice.GetDeviceName(), dev.outGoingCall.callee));
                            else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                                status = new StringBuilder(String.format("%s Call to %s, Ringing....\n", dev.GetDeviceName(), dev.outGoingCall.callee));
                            else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                                status = new StringBuilder(String.format("%s Talking with %s\n", dev.GetDeviceName(), dev.talkPeer));
                            else
                                status = new StringBuilder(String.format("%s Call to %s, Unknow....\n", dev.GetDeviceName(), dev.outGoingCall.callee));
                        } else {
                            if (dev.isRegOk)
                                status = new StringBuilder(String.format("%s Register Suss\n", dev.GetDeviceName()));
                            else
                                status = new StringBuilder(String.format("%s Register Fail\n", dev.GetDeviceName()));
                        }

                        if (dev.devLists != null) {
                            for (int iTmp = 0; iTmp < dev.devLists.size(); iTmp++) {
                                UserDevice bedPhone = dev.devLists.get(iTmp);
                                if (bedPhone.isReg) {
                                    status.append(String.format("%s Register succ\n", bedPhone.devid));
                                } else
                                    status.append(String.format("%s Register Fail\n", bedPhone.devid));
                            }
                        }
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
                int talkingNum = 0;
                synchronized (MainActivity.class) {
                    TextView tv = findViewById(R.id.deviceStatusId);
                    if (dev.isCallOut) {
                        if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                            status = new StringBuilder(String.format("%s Call to %s\n", audioTest.curDevice.GetDeviceName(), dev.outGoingCall.callee));
                        else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                            status = new StringBuilder(String.format("%s Call to %s, Ringing....\n", dev.GetDeviceName(), dev.outGoingCall.callee));
                        else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                            status = new StringBuilder(String.format("%s Talking with %s\n", dev.GetDeviceName(), dev.talkPeer));
                        else
                            status = new StringBuilder(String.format("%s Call to %s, Unknow....\n", dev.GetDeviceName(), dev.outGoingCall.callee));
                    } else {
                        if (dev.isRegOk)
                            status = new StringBuilder(String.format("%s Register Suss\n", dev.GetDeviceName()));
                        else
                            status = new StringBuilder(String.format("%s Register Fail\n", dev.GetDeviceName()));
                    }
                    tv.setText(status.toString());
 
                    if (dev.type == UserInterface.CALL_NURSER_DEVICE) {
                        dev.QueryDevs();
                    }

                    tv = findViewById(R.id.callListId);
                    status = new StringBuilder();
                    for (LocalCallInfo callInfo : dev.inComingCallInfos) {
                        switch (callInfo.status) {
                            case LocalCallInfo.LOCAL_CALL_STATUS_INCOMING:
                                status.append(String.format("From %s, Incoming\n", callInfo.caller));
                                break;
                            case LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED:
                                status.append(String.format("%s Talking with %s\n", dev.id,dev.talkPeer));
                                talkingNum++;
                                break;
                            default:
                                status.append(String.format("From %s, Unexcept\n", callInfo.caller));
                                break;

                        }
                    }
                    tv.setText(status.toString());
                    if (talkingNum > 1) {
                        StopTest();
                    }
                }
            });
        }
    }
}
