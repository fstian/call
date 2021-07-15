package com.example.nettytest.pub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.example.nettytest.userinterface.CallLogMessage;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Interceptor.Chain;
import okhttp3.MediaType;

public class CallLogSaver {   
    
    private final String JSON_CALL_LOG_AREA_CODE = "areaCode";
    private final String JSON_CALL_LO_CALL_ID = "callID";
    private final String JSON_CALL_LOG_INVITE_TYPE = "inviteType";
    private final String JSON_CALL_LOG_INVITE_DIRECT = "inviteDirect";
    private final String JSON_CALL_LOG_INVITE_DEVID = "inviteDeviceID";
    private final String JSON_CALL_LOG_INVITE_DEVNAME = "inviteDeviceName";
    private final String JSON_CALL_LOG_INVITE_DEVTYPE = "inviteDeviceType";
    private final String JSON_CALL_LOG_INVITE_TIME = "inviteTime";
    private final String JSON_CALL_LOG_ANSWER_DEVID = "answerDeviceID";
    private final String JSON_CALL_LOG_ANSWER_DEVNAME = "answerDeviceName";
    private final String JSON_CALL_LOG_ANSWER_DEVTYPE = "answerDeviceType";
    private final String JSON_CALL_LOG_ANSWER_MODE = "answerMode";
    private final String JSON_CALL_LOG_ANSWER_TIME = "answerTime";
    private final String JSON_CALL_LOG_END_TIME = "endTime";
    private final String JSON_CALL_LOG_CALL_DURATION = "callDuration";
    
    class CallLogSaveMessage {
        final static int CALL_LOG_SEC_TICK = 1;
        final static int CALL_LOG_SAVE_FINISH = 2;

        int type;
        
        public CallLogSaveMessage(int type) {
            this.type = type;
        }       

    }
    OkHttpClient client = null;
    
    ArrayList<CallLogMessage> callLogList=null;
    CallLogMessage curCallLog = null;
    ArrayList<CallLogSaveMessage> msgList=null;
    
    final int CALL_LOG_SAVER_IDLE = 1;
    final int CALL_LOG_SAVER_WRITTING = 2;
    
    private final int WRITE_WAIT_TIME = 10;
    private final int RETRY_MAX_COUNT = 5;
    
    int state =CALL_LOG_SAVER_IDLE;
    int retryCount = 0;
    int writeWaitCount = 0;
    
    String serviceAddress;
    int servicePort;
    
    public CallLogSaver() {
        callLogList = new ArrayList<>();
    }

    private void httpPostProcess(String url,String body,Callback cb) {
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
        
        MediaType mediaType = MediaType.parse("application/json;charset=UTF-8");

        final RequestBody requestBody = RequestBody.create(mediaType, body);

        final Request req = new Request.Builder().post(requestBody).url(url).build();

        Call call = client.newCall(req);

        call.enqueue(cb);
    
    }
    
    public int StartCallLogServer(String address,int port) {
        serviceAddress = address;
        servicePort = port;
        
        if(msgList==null) {
            msgList = new ArrayList<>();
            new Thread("CallLogSaver") {
                @Override
                public void run() {
                    ArrayList<CallLogSaveMessage> msgs = new ArrayList<>();
                    CallLogSaveMessage msg;
                    while(!isInterrupted()) {
                        synchronized(msgList) {
                            try {
                                msgList.wait();
                                while(msgList.size()>0) {
                                    msg = msgList.remove(0);
                                    msgs.add(msg);
                                }
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                break;
                            }
                        }
                        
                        while(msgs.size()>0) {
                            msg = msgs.remove(0);
                            ProcessCallLogMessage(msg);
                        }
                    }
                    
                }
            }.start();
            
            new Timer("CallLogTimer").schedule(new TimerTask() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    CallLogSaveMessage msg = new CallLogSaveMessage(CallLogSaveMessage.CALL_LOG_SEC_TICK);
                    PostCallLogMessage(msg);
                }
                
            }, 0,1000);
        }
        
        return 0;
    }
    
    public int AddCallLog(CallLogMessage callLog) {
        if(callLogList!=null) {
            synchronized(callLogList) {
                callLogList.add(callLog);
            }
        }
        return 0;
    }
    
    private void PostCallLogMessage(CallLogSaveMessage msg) {
        if(msgList!=null) {
            synchronized(msgList) {
                msgList.add(msg);
                msgList.notify();
                
            }
        }
    }
    
    private void ProcessCallLogMessage(CallLogSaveMessage msg) {
        switch(msg.type){
        case CallLogSaveMessage.CALL_LOG_SEC_TICK:
            if(state==CALL_LOG_SAVER_IDLE) {
                curCallLog = null;
                synchronized(callLogList) {
                    if(callLogList!=null) {
                        if(callLogList.size()>0) {
                            curCallLog = callLogList.remove(0);
                        }
                    }
                }                
                if(curCallLog!=null) {
                    state = CALL_LOG_SAVER_WRITTING;
                    WriteCallLog(curCallLog);
                }
                
            }else {
                writeWaitCount++;
                if(writeWaitCount>WRITE_WAIT_TIME) {
                    retryCount++;
                    writeWaitCount= 0;
                    if(retryCount>RETRY_MAX_COUNT) {
                        retryCount = 0;
                        state = CALL_LOG_SAVER_IDLE;
                        LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Save Call Log for %s Fail",curCallLog.callId);
                        // discard Fail CallLog
                        curCallLog = null;
                    }
                }
            }
            break;
        case CallLogSaveMessage.CALL_LOG_SAVE_FINISH:
            retryCount = 0;
            state = CALL_LOG_SAVER_IDLE;
            break;            
        }
    }
    
    private void WriteCallLog(CallLogMessage callLog) {
        String cmd = String.format("http://%s:%d/logstore/set/call/log",serviceAddress,servicePort);
        String body = "";
        JSONObject json = new JSONObject();
        
        json.put(JSON_CALL_LOG_AREA_CODE, callLog.areaId);
        json.put(JSON_CALL_LO_CALL_ID, callLog.callId);
        json.put(JSON_CALL_LOG_INVITE_TYPE, callLog.callType);
        json.put(JSON_CALL_LOG_INVITE_DIRECT, callLog.callDirection);
        
        json.put(JSON_CALL_LOG_INVITE_DEVID, callLog.callerNum);
        json.put(JSON_CALL_LOG_INVITE_DEVNAME, callLog.callerName);
        json.put(JSON_CALL_LOG_INVITE_DEVTYPE, callLog.callerType);
        json.put(JSON_CALL_LOG_INVITE_TIME, callLog.startTime/1000);
        
        if(callLog.answerNum.isEmpty()) {
            json.put(JSON_CALL_LOG_ANSWER_DEVID, callLog.enderNum);
            json.put(JSON_CALL_LOG_ANSWER_DEVNAME, callLog.enderName);
            json.put(JSON_CALL_LOG_ANSWER_DEVTYPE, callLog.enderType);
            json.put(JSON_CALL_LOG_ANSWER_TIME, callLog.endTime/1000);
            json.put(JSON_CALL_LOG_END_TIME, callLog.endTime/1000);
            json.put(JSON_CALL_LOG_CALL_DURATION, 0);
        }else {
            json.put(JSON_CALL_LOG_ANSWER_DEVID, callLog.answerNum);
            json.put(JSON_CALL_LOG_ANSWER_DEVNAME, callLog.answerName);
            json.put(JSON_CALL_LOG_ANSWER_DEVTYPE, callLog.answerType);            
            json.put(JSON_CALL_LOG_ANSWER_TIME, callLog.answerTime/1000);
            json.put(JSON_CALL_LOG_END_TIME, callLog.endTime/1000);
            json.put(JSON_CALL_LOG_CALL_DURATION, (callLog.endTime-callLog.answerTime)/1000);
        }
        
        json.put(JSON_CALL_LOG_ANSWER_MODE, callLog.answerMode);
        
        body = json.toString();
        json.clear();
        
        httpPostProcess(cmd,body,new Callback() {

            @Override
            public void onFailure(Call c, IOException e) {
                // TODO Auto-generated method stub
                LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_ERROR, "Save Call Log %s Fail with msg %s",curCallLog.callId,e.getMessage());                
            }

            @Override
            public void onResponse(Call c, Response res) throws IOException {
                // TODO Auto-generated method stub
                if(res.code()==200) {
                    LogWork.Print(LogWork.BACKEND_CALL_MODULE, LogWork.LOG_DEBUG, "Save Call Log %s succ with Res %s",curCallLog.callId,res.body().string());
                    CallLogSaveMessage msg = new CallLogSaveMessage(CallLogSaveMessage.CALL_LOG_SAVE_FINISH);
                    synchronized(msgList) {
                        msgList.add(msg);
                        msgList.notify();
                    }
                }
            }
            
        });
        
    }

}
