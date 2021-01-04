package com.example.nettytest;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nettytest.backend.callserver.DemoServer;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserDevice;
import com.example.nettytest.userinterface.UserDevsMessage;
import com.example.nettytest.userinterface.UserInterface;
import com.example.nettytest.terminal.test.LocalCallInfo;
import com.example.nettytest.terminal.test.TestDevice;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserRegMessage;

public class MainActivity extends AppCompatActivity {

    final int BED_DEVICE_NUM = 4;
    final int DOOR_DEVICE_NUM = 2;
    final int NURSE_DEVICE_NUM = 2;
    final int TV_DEVICE_NUM = 1;

    CallMessageProcess callMessageThread;
    Handler terminalCallMessageHandler = null;

    DemoServer callServer;

    TestDevice[] testDevices;
    TestDevice curDevice;

    

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int iTmp;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        TextView tv = (TextView)findViewById(R.id.deviceStatusId);

        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action==MotionEvent.ACTION_DOWN){
                    boolean result = false;
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    if(curDevice!=null){
                        result = curDevice.Operation(0,(int)x,(int)y);
                        if(result)
                            UpdateHMI(curDevice);
                    }
                }
                return false;
            }
        });

        tv = (TextView)findViewById(R.id.callListId);

        tv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getAction();
                if(action==MotionEvent.ACTION_DOWN){
                    boolean result = false;
                    float x = motionEvent.getX();
                    float y = motionEvent.getY();
                    if(curDevice!=null){
                        result = curDevice.Operation(1,(int)x,(int)y);
                        if(result)
                            UpdateHMI(curDevice);
                    }
                }
                return false;
            }
        });
    }

    private int InitServer(){
        UserInterface.StartServer();
        UserInterface.AddDeviceOnServer("20105101",UserInterface.CALL_BED_DEVICE);
        UserInterface.AddDeviceOnServer("20105102",UserInterface.CALL_BED_DEVICE);
        UserInterface.AddDeviceOnServer("20105103",UserInterface.CALL_BED_DEVICE);
        UserInterface.AddDeviceOnServer("20105104",UserInterface.CALL_BED_DEVICE);
        UserInterface.AddDeviceOnServer("20105105",UserInterface.CALL_BED_DEVICE);

        UserInterface.AddDeviceOnServer("20105191",UserInterface.CALL_DOOR_DEVICE);
        UserInterface.AddDeviceOnServer("20105192",UserInterface.CALL_DOOR_DEVICE);

        UserInterface.AddDeviceOnServer("201051A1",UserInterface.CALL_NURSER_DEVICE);
        UserInterface.AddDeviceOnServer("201051A2",UserInterface.CALL_NURSER_DEVICE);

        UserInterface.AddDeviceOnServer("201051B1",UserInterface.CALL_TV_DEVICE);
        return 0;
    }

    private int InitGui(){
        Spinner deviceSpinner;

        deviceSpinner = (Spinner) findViewById(R.id.deviceSelectId);
        testDevices = new TestDevice[BED_DEVICE_NUM+DOOR_DEVICE_NUM+NURSE_DEVICE_NUM+TV_DEVICE_NUM];
        String[] arr = new String[BED_DEVICE_NUM+DOOR_DEVICE_NUM+NURSE_DEVICE_NUM+TV_DEVICE_NUM];
        String devId;
        int iTmp;

        callMessageThread = new CallMessageProcess();
        callMessageThread.start();

        for(iTmp=0;iTmp<BED_DEVICE_NUM;iTmp++){
            devId = String.format("%X",0x20105101+iTmp);
            testDevices[iTmp] = new TestDevice(UserInterface.CALL_BED_DEVICE,devId);
            arr[iTmp] = UserInterface.GetDeviceTypeName(UserInterface.CALL_BED_DEVICE)+"    "+devId;
        }
        for(iTmp=0;iTmp<DOOR_DEVICE_NUM;iTmp++){
            devId = String.format("%X",0x20105191+iTmp);
            testDevices[iTmp+BED_DEVICE_NUM] = new TestDevice(UserInterface.CALL_DOOR_DEVICE,devId);
            arr[iTmp+BED_DEVICE_NUM] = UserInterface.GetDeviceTypeName(UserInterface.CALL_DOOR_DEVICE)+"    "+devId;
        }
        for(iTmp=0;iTmp<NURSE_DEVICE_NUM;iTmp++){
            devId = String.format("%X",0x201051A1+iTmp);
            testDevices[iTmp+BED_DEVICE_NUM+DOOR_DEVICE_NUM] = new TestDevice(UserInterface.CALL_NURSER_DEVICE,devId);
            arr[iTmp+BED_DEVICE_NUM+DOOR_DEVICE_NUM] = UserInterface.GetDeviceTypeName(UserInterface.CALL_NURSER_DEVICE)+"    "+devId;
        }
        for(iTmp=0;iTmp<TV_DEVICE_NUM;iTmp++){
            devId = String.format("%X",0x201051B1+iTmp);
            testDevices[iTmp+BED_DEVICE_NUM+DOOR_DEVICE_NUM+NURSE_DEVICE_NUM] = new TestDevice(UserInterface.CALL_TV_DEVICE,devId);
            arr[iTmp+BED_DEVICE_NUM+DOOR_DEVICE_NUM+NURSE_DEVICE_NUM] = UserInterface.GetDeviceTypeName(UserInterface.CALL_TV_DEVICE)+"    "+devId;
        }
        curDevice = testDevices[0];

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,arr);

        deviceSpinner.setAdapter(adapter);

        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                System.out.println(String.format("Item Select I=%d,L=%d",i,l));
                if(i<testDevices.length) {
                    curDevice = testDevices[i];
                    UpdateHMI(curDevice);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                System.out.println("Item noThing selected");

            }
        });


        return 0;
    }

    private class CallMessageProcess extends Thread{
        @Override
        public void run() {
            Looper.prepare();
            terminalCallMessageHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message message) {
                    int msgType = message.arg1;
                    UserMessage terminalMsg = (UserMessage)message.obj;
                    TestDevice device=null;
                    UserInterface.PrintLog("DEV %s Recv Msg %d(%s) ", terminalMsg.devId,terminalMsg.type, UserMessage.GetMsgName(terminalMsg.type));
                    for(int iTmp=0;iTmp<testDevices.length;iTmp++){
                        if(testDevices[iTmp].id.compareToIgnoreCase(terminalMsg.devId)==0){
                            device =testDevices[iTmp];
                            break;
                        }
                    }
                    if(device!=null){
                        switch(msgType){
                            case UserMessage.MESSAGE_CALL_INFO:
                                device.UpdateCallInfo((UserCallMessage)terminalMsg);
                                if(device==curDevice)
                                    UpdateHMI(device);
                                break;
                            case UserMessage.MESSAGE_REG_INFO:
                                device.UpdateRegisterInfo((UserRegMessage)terminalMsg);
                                if(device==curDevice)
                                    UpdateHMI(device);
                                break;
                            case UserMessage.MESSAGE_DEVICES_INFO:
                                device.UpdateDeviceList((UserDevsMessage)terminalMsg);
                                if(device==curDevice)
                                    UpdateNurserHMI(device);
                                break;
                        }
                    }

                    return false;
                }
            });
            UserInterface.SetMessageHandler(terminalCallMessageHandler);
            Looper.loop();
        }
    }

    private boolean UpdateNurserHMI(TestDevice dev){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(dev.type == UserInterface.CALL_NURSER_DEVICE){
                    String status = "";
                    TextView tv = (TextView)findViewById(R.id.deviceStatusId);
                    if(dev.devLists==null)
                        UserInterface.PrintLog("Recv DEV_REQ for %s, Has NO bed Device",dev.id);
                    else
                        UserInterface.PrintLog("Recv DEV_REQ for %s, Has %d bed Device",dev.id,dev.devLists.size());
                    if(dev.isCallOut){
                        if(dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                            status = String.format("%s Call to %s\n\n",curDevice.GetDeviceName(),dev.outGoingCall.callee);
                        else if(dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                            status = String.format("%s Call to %s, Ringing....\n\n",dev.GetDeviceName(),dev.outGoingCall.callee);
                        else if(dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                            status = String.format("%s Call to %s, Talking....\n\n",dev.GetDeviceName(),dev.outGoingCall.callee);
                        else
                            status = String.format("%s Call to %s, Unknow....\n\n",dev.GetDeviceName(),dev.outGoingCall.callee);
                    }else{
                        if(dev.isRegOk)
                            status = String.format("%s Register Suss\n\n",dev.GetDeviceName());
                        else
                            status = String.format("%s Register Fail\n\n",dev.GetDeviceName());
                    }
                    if(dev.devLists!=null) {
                        for (int iTmp = 0; iTmp < dev.devLists.size();iTmp++){
                            UserDevice bedPhone = dev.devLists.get(iTmp);
                            if(bedPhone.isReg)
                                status += String.format("%s Register succ\n\n",bedPhone.devid);
                            else
                                status += String.format("%s Register Fail\n\n",bedPhone.devid);
                        }
                    }
                    tv.setText(status);
                }
            }
        });
        return false;
    }

    private boolean UpdateHMI(TestDevice dev){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String status = "";
                TextView tv = (TextView)findViewById(R.id.deviceStatusId);
                if(dev.isCallOut){
                    if(dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_OUTGOING)
                        status = String.format("%s Call to %s",curDevice.GetDeviceName(),dev.outGoingCall.callee);
                    else if(dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_RINGING)
                        status = String.format("%s Call to %s, Ringing....",dev.GetDeviceName(),dev.outGoingCall.callee);
                    else if(dev.outGoingCall.status == LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED)
                        status = String.format("%s Call to %s, Talking....",dev.GetDeviceName(),dev.outGoingCall.callee);
                    else
                        status = String.format("%s Call to %s, Unknow....",dev.GetDeviceName(),dev.outGoingCall.callee);
                }else{
                    if(dev.isRegOk)
                        status = String.format("%s Register Suss",dev.GetDeviceName());
                    else
                        status = String.format("%s Register Fail",dev.GetDeviceName());
                }

                if(dev.type== UserInterface.CALL_NURSER_DEVICE){
                    dev.QueryDevs();
                }
                tv.setText(status);

                tv = (TextView)findViewById(R.id.callListId);
                status = "";
                for(LocalCallInfo callInfo:dev.inComingCallInfos){
                    switch(callInfo.status){
                        case LocalCallInfo.LOCAL_CALL_STATUS_INCOMING:
                            status += String.format("Call From %s, Incoming...\n\n",callInfo.caller);
                            break;
                        case LocalCallInfo.LOCAL_CALL_STATUS_CONNECTED:
                            status += String.format("Call From %s, Talking...\n\n",callInfo.caller);
                            break;
                        default:
                            status += String.format("Call From %s, Unexcept...\n\n",callInfo.caller);
                            break;

                    }
                }
                tv.setText(status);

            }
        });

        return false;
    }

}