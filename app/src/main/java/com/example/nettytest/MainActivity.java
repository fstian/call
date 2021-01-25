package com.example.nettytest;

import android.annotation.SuppressLint;
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

import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.TestInfo;
import com.example.nettytest.userinterface.UserCallMessage;
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

public class MainActivity extends AppCompatActivity {


    CallMessageProcess callMessageThread;
    Handler terminalCallMessageHandler = null;

    TestDevice[] testDevices;
    TestDevice curDevice;

    DatagramSocket testSocket;
    int iTestCount = 0;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        PhoneParam.InitPhoneParam();

        InitServer();

        InitGui();
/*
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                clientTestCount++;
                if(clientTestCount==10){
                    callResult = testDevices[0].BuildCall("201051A1",UserInterface.CALL_NORMAL_TYPE);
                    if(curDevice==testDevices[0]){
                        UpdateHMI(curDevice);
                    }
                }else if(clientTestCount==20){
                    if(callResult!=null){
                        testDevices[0].EndCall(callResult.callID);
                        if(curDevice==testDevices[0]){
                            UpdateHMI(curDevice);
                        }
                    }
                }

                if(clientTestCount>=20){
                    clientTestCount = 0;
                }
            }
        },0,1000);
*/
        TextView tv = findViewById(R.id.deviceStatusId);

        tv.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if(action==MotionEvent.ACTION_DOWN){
                boolean result;
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if(curDevice!=null){
                    synchronized (MainActivity.class) {
                        result = curDevice.Operation(0, (int) x, (int) y);
                    }
                    if(result)
                        UpdateHMI(curDevice);
                }
            }
            return false;
        });

        tv = findViewById(R.id.callListId);

        tv.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if(action==MotionEvent.ACTION_DOWN){
                boolean result;
                float x = motionEvent.getX();
                float y = motionEvent.getY();
                if(curDevice!=null){
                    synchronized (MainActivity.class) {
                        result = curDevice.Operation(1, (int) x, (int) y);
                    }
                    if(result)
                        UpdateHMI(curDevice);
                }
            }
            return false;
        });

        new Thread(() -> {
            try {
                byte[] recvBuf=new byte[1024];
                DatagramPacket recvPack;
                testSocket = new DatagramSocket(SystemSnap.SNAP_MMI_PORT);
                while (!testSocket.isClosed()){
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        testSocket.receive(recvPack);
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

                                for (TestDevice dev : testDevices) {
                                    if(dev==null)
                                        break;
                                    dev.SetTestInfo(info);

                                    JSONObject resJson = new JSONObject();
                                    resJson.putOpt(SystemSnap.SNAP_CMD_TYPE_NAME,SystemSnap.SNAP_DEV_RES);
                                    resJson.putOpt(SystemSnap.SNAP_DEVID_NAME,dev.id);
                                    resJson.putOpt(SystemSnap.SNAP_DEVTYPE_NAME,dev.type);
                                    byte[] resBuf = resJson.toString().getBytes();
                                    DatagramPacket resPack= new DatagramPacket(resBuf,resBuf.length,recvPack.getAddress(),recvPack.getPort());
                                    testSocket.send(resPack);
                                }
                            }else if(type==SystemSnap.SNAP_MMI_CALL_REQ){
                                for (TestDevice dev : testDevices) {
                                    if(dev==null)
                                        break;
                                    byte[] resBuf = dev.MakeSnap();
                                    DatagramPacket resPack= new DatagramPacket(resBuf,resBuf.length,recvPack.getAddress(),recvPack.getPort());
                                    testSocket.send(resPack);
                                }
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

    private void StopTest(){
        if(testSocket!=null){
            if(!testSocket.isClosed()){
                String stopCmd = "{\"autoTest\":0,\"realTime\":1,\"timeUnit\":10}";
                byte[] sendBuf = stopCmd.getBytes();
                DatagramPacket packet;
                try {
                    packet = new DatagramPacket(sendBuf,sendBuf.length, InetAddress.getByName("255.255.255.255"),10005);
                    testSocket.send(packet);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void InitServer(){
        if(PhoneParam.serverActive) {
            UserInterface.StartServer();
            for(int iTmp=0;iTmp<PhoneParam.devicesOnServer.size();iTmp++){
                UserDevice dev = PhoneParam.devicesOnServer.get(iTmp);
                UserInterface.AddDeviceOnServer(dev.devid,dev.type);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void InitGui(){
        Spinner deviceSpinner;
        String devId;
        int iTmp;
        String[] arr;

        deviceSpinner = findViewById(R.id.deviceSelectId);

        int deviceNum = PhoneParam.deviceList.size();

        if(deviceNum>=1) {
            testDevices = new TestDevice[deviceNum];
            arr = new String[deviceNum];

            callMessageThread = new CallMessageProcess();
            callMessageThread.start();

            for (iTmp = 0; iTmp < deviceNum; iTmp++) {
                System.out.println("deviceList has "+PhoneParam.deviceList.size()+" items, try to get "+iTmp+ " item");
                UserDevice dev = PhoneParam.deviceList.get(iTmp);
                testDevices[iTmp] = new TestDevice(dev.type, dev.devid);
                arr[iTmp] = UserInterface.GetDeviceTypeName(dev.type) + "    " + dev.devid;
            }
        }else{
            testDevices = new TestDevice[1];
            arr = new String[1];
            devId = "20105101";
            testDevices[0] = new TestDevice(UserInterface.CALL_BED_DEVICE,devId);
            arr[0] = UserInterface.GetDeviceTypeName(UserInterface.CALL_BED_DEVICE) + "    " + devId;
        }
        curDevice = testDevices[0];

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arr);

        deviceSpinner.setAdapter(adapter);

        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if(i<testDevices.length) {
                    curDevice = testDevices[i];
                    UpdateHMI(curDevice);
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
                if (msgType == UserMessage.MESSAGE_CALL_INFO || msgType == UserMessage.MESSAGE_REG_INFO || msgType == UserMessage.MESSAGE_DEVICES_INFO) {

                    UserInterface.PrintLog("DEV %s Recv Msg %d(%s) ", terminalMsg.devId, terminalMsg.type, UserMessage.GetMsgName(terminalMsg.type));
                    for (TestDevice testDevice : testDevices) {
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
                                if(device==curDevice)
                                    UpdateHMI(device);
                                if(result<0){
                                    StopTest();
                                }
                                break;
                            case UserMessage.MESSAGE_REG_INFO:
                                device.UpdateRegisterInfo((UserRegMessage) terminalMsg);
                                if (device == curDevice)
                                    UpdateHMI(device);
                                break;
                            case UserMessage.MESSAGE_DEVICES_INFO:
                                synchronized (MainActivity.class) {
                                    device.UpdateDeviceList((UserDevsMessage) terminalMsg);
                                }
                                if (device == curDevice) {
                                    if(device.type==UserInterface.CALL_NURSER_DEVICE )
                                        UpdateNurserHMI(device);
                                }
                                break;
                        }
                    }
                } else if (msgType == UserMessage.MESSAGE_TEST_TICK) {
                    for (TestDevice testDevice : testDevices) {
                        boolean testFlag;
                        synchronized (MainActivity.class) {
                            if(testDevice==null)
                                break;
                            testFlag = testDevice.TestProcess();
                        }
                        if(curDevice==testDevice){
                            if (testFlag) {
                                UpdateHMI(curDevice);
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
        }
    }

    private void UpdateNurserHMI(TestDevice dev){
        runOnUiThread(() -> {
            synchronized (MainActivity.class) {
                if (dev.type == UserInterface.CALL_NURSER_DEVICE) {
                    StringBuilder status;
                    TextView tv = findViewById(R.id.deviceStatusId);
                    if (dev.devLists == null)
                        UserInterface.PrintLog("Recv DEV_REQ for %s, Has NO bed Device", dev.id);
                    else
                        UserInterface.PrintLog("Recv DEV_REQ for %s, Has %d bed Device", dev.id, dev.devLists.size());
                    if (dev.isCallOut) {
                        if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                            status = new StringBuilder(String.format("%s Call to %s\n", curDevice.GetDeviceName(), dev.outGoingCall.callee));
                        else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                            status = new StringBuilder(String.format("%s Call to %s, Ringing....\n", dev.GetDeviceName(), dev.outGoingCall.callee));
                        else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                            status = new StringBuilder(String.format("%s Call to %s, Talking....\n", dev.GetDeviceName(), dev.outGoingCall.callee));
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

    private void UpdateHMI(TestDevice dev){
        runOnUiThread(() -> {
            StringBuilder status;
            int talkingNum = 0;
            synchronized (MainActivity.class) {
                TextView tv = findViewById(R.id.deviceStatusId);
                if (dev.isCallOut) {
                    if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                        status = new StringBuilder(String.format("%s Call to %s", curDevice.GetDeviceName(), dev.outGoingCall.callee));
                    else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                        status = new StringBuilder(String.format("%s Call to %s, Ringing....", dev.GetDeviceName(), dev.outGoingCall.callee));
                    else if (dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                        status = new StringBuilder(String.format("%s Call to %s, Talking....", dev.GetDeviceName(), dev.outGoingCall.callee));
                    else
                        status = new StringBuilder(String.format("%s Call to %s, Unknow....", dev.GetDeviceName(), dev.outGoingCall.callee));
                } else {
                    if (dev.isRegOk)
                        status = new StringBuilder(String.format("%s Register Suss", dev.GetDeviceName()));
                    else
                        status = new StringBuilder(String.format("%s Register Fail", dev.GetDeviceName()));
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
                            status.append(String.format("From %s, Talking\n", callInfo.caller));
                            talkingNum++;
                            break;
                        default:
                            status.append(String.format("From %s, Unexcept\n", callInfo.caller));
                            break;

                    }
                }
                tv.setText(status.toString());
                if(talkingNum>1){
                    StopTest();
                }
            }

        });

    }



}