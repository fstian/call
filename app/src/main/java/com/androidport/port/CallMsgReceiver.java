package com.androidport.port;


import android.os.Handler;
import android.os.Message;

import com.example.nettytest.pub.CallPubMessage;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.MsgReceiver;
import com.example.nettytest.userinterface.UserCallMessage;
import com.example.nettytest.userinterface.UserMessage;
import com.example.nettytest.userinterface.UserVideoMessage;
import com.androidport.port.audio.AudioMgr;

import java.util.ArrayList;

public class CallMsgReceiver extends MsgReceiver {

    static protected Handler userMessageHandler = null;
    static protected Handler backendMessageHandler = null;

    static String audioId = "";
    static String callId = "";
    static String audioDevId = "";

    static public void SetMessageHandler(Handler h){
        if(userMessageHandler==null){

            userMessageHandler = h;
            CreateTerminalMsgList();

            new Thread("TermMsgReceiver"){
                @Override
                public void run() {
                    ArrayList<CallPubMessage> newMsgList = new ArrayList<>();
                    CallPubMessage msg;
                    while (!isInterrupted()) {
                        newMsgList = GetTerminalMsgs();
//                        LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"CallMsgRecevier Recv %d Terminal Msg",newMsgList.size());
                        int type;
                        while (newMsgList.size() > 0) {
                            msg = newMsgList.remove(0);
                            Message userMsg = userMessageHandler.obtainMessage();
                            userMsg.arg1 = msg.arg1;
                            userMsg.obj = msg.obj;
                            type = userMsg.arg1;
                            if (type == UserMessage.MESSAGE_CALL_INFO) {
                                UserCallMessage callMsg = (UserCallMessage) userMsg.obj;
                                if (callMsg.type == UserMessage.CALL_MESSAGE_CONNECT || callMsg.type == UserMessage.CALL_MESSAGE_ANSWERED) {
                                    callId = callMsg.callId;
                                    String id = AudioMgr.OpenAudio(callMsg.devId, callMsg.localRtpPort, callMsg.remoteRtpPort, callMsg.remoteRtpAddress, callMsg.rtpSample, callMsg.rtpPTime, callMsg.rtpCodec, callMsg.audioMode);
                                    if(!id.isEmpty()){
                                        audioDevId = callMsg.devId;
                                        audioId = id;
                                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Open Audio %s for Dev %s Call %s", audioId, audioDevId, callId);
                                    }else{
                                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, "Open Audio Fail, Audio is Occupy by Dev %s Call %s Audio", audioDevId, callId,audioId);
                                    }
                                } else if (callMsg.type == UserMessage.CALL_MESSAGE_DISCONNECT) {
                                    if (callMsg.callId.compareToIgnoreCase(callId) == 0 && callMsg.devId.compareToIgnoreCase(audioDevId) == 0) {
                                        AudioMgr.CloseAudio(audioId);
                                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Close Audio %s for Dev %s Call %s", audioId, audioDevId, callId);
                                        audioId = "";
                                        callId = "";
                                        audioDevId = "";
                                    } else {
                                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Cur Dev %s, Call %s ,Audio %s Not match the Audio Stop requier Dev %s, Call %s", audioDevId, callId, audioId, callMsg.devId, callMsg.callId);
                                    }
                                }
                            } else if (type == UserMessage.MESSAGE_VIDEO_INFO) {
                                UserVideoMessage videoMsg = (UserVideoMessage) userMsg.obj;
                                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Dev %s recv Message %s", videoMsg.devId, UserMessage.GetMsgName(videoMsg.type));
                                if (videoMsg.type == UserMessage.CALL_VIDEO_ANSWERED || videoMsg.type == UserMessage.CALL_VIDEO_REQ_ANSWER) {
                                    if (videoMsg.devId.compareToIgnoreCase(audioDevId) != 0 || videoMsg.callId.compareToIgnoreCase(callId) != 0) {
                                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Cur Dev %s, Call %s ,Audio %s Not match the Video Start requier Dev %s, Call %s", audioDevId, callId, audioId, videoMsg.devId, videoMsg.callId);
                                    } else
                                        AudioMgr.SuspendAudio(videoMsg.devId, audioId);
                                } else if (videoMsg.type == UserMessage.CALL_VIDEO_END) {
                                    if (videoMsg.devId.compareToIgnoreCase(audioDevId) != 0 || videoMsg.callId.compareToIgnoreCase(callId) != 0) {
                                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Cur Dev %s, Call %s ,Audio %s Not match the Video Stop requier Dev %s, Call %s", audioDevId, callId, audioId, videoMsg.devId, videoMsg.callId);
                                    } else
                                        AudioMgr.ResumeAudio(videoMsg.devId, audioId);
                                }
                            }

                            userMessageHandler.sendMessage(userMsg);

                        }
                    }
                }
            }.start();
        }else{
            userMessageHandler = h;
        }
    }

    static public void SetBackEndMessageHandler(Handler h){
        if(backendMessageHandler==null){

            backendMessageHandler = h;
            CreateBackEndMsgList();

            new Thread("BackEndMsgReceiver"){
                @Override
                public void run() {
                    ArrayList<CallPubMessage> newMsgList = new ArrayList<>();
                    CallPubMessage msg;
                    while(!isInterrupted()){
                        newMsgList = GetBackEndMsgs();
//                           LogWork.Print(LogWork.DEBUG_MODULE,LogWork.LOG_TEMP_DBG,"CallMsgRecevier Recv %d BackEnd Msg",newMsgList.size());

                        while(newMsgList.size()>0){
                            msg = newMsgList.remove(0);
                            Message userMsg = backendMessageHandler.obtainMessage();
                            userMsg.arg1 = msg.arg1;
                            userMsg.obj = msg.obj;
                            backendMessageHandler.sendMessage(userMsg);
                        }
                    }

                }
            }.start();
        }
    }

    public static String GetAudioOwner(){
        return AudioMgr.GetAudioOwnwer();
    }
}
