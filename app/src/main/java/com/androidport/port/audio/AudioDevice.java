package com.androidport.port.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;


import com.android.webrtc.audio.MobileAEC;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.pub.AudioMode;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserInterface;
import com.qd.gtcom.wangzhengcheng.ns.AgcUtils;
import com.qd.gtcom.wangzhengcheng.ns.NsUtils;
import com.witted.ptt.JitterBuffer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AudioDevice {

    int dstPort ;
    int srcPort ;
    int ptime ;
    int sample ;
    int codec;

    String dstAddress;

    Handler audioWriteHandler;
    boolean audioWriteHandlerEnabled = false;

    DatagramSocket audioSocket = null;
    SocketReadThread socketReadThread = null;

    AudioReadThread audioReadThread = null;
    AudioWriteThread audioWriteThread = null;

    AudioReadSimpleThread audioReadSimpleThread = null;
    AudioWriteSimpleThread audioWriteSimpleThread = null;


    AudioRecord recorder = null;
    AudioTrack player = null;

    JitterBuffer jb=null;
    int jbIndex;

    int socketOpenCount = 0;
    int audioOpenCount = 0;

    int packSize;

    final int AUDIO_PLAY_MSG = 1;

    public String id;
    public String devId;

    MobileAEC aec = null;
    NsUtils nsUtils = null;
    AgcUtils inputAgc = null;
    AgcUtils outputAgc = null;
    int nsHandle = -1;

    boolean isSockReadRuning = false;
    boolean isAudioReadRuning = false;
    boolean isAudioWriteRuning = false;

    int audioMode;

    final int PLAY_KARRAY_SIZE_MAX = 100;

    int[] karray ;
    int kpos = 0;

    static{
        System.loadLibrary("JitterBuffer");
    }

    public AudioDevice(String devId,int src,int dst,String address,int sample,int ptime,int codec,int mode){

        karray=new int[PLAY_KARRAY_SIZE_MAX];
        for(int iTmp=0;iTmp<PLAY_KARRAY_SIZE_MAX;iTmp++)
            karray[iTmp] = 100;
        dstPort = dst;
        srcPort = src;
        dstAddress = address;
        this.ptime = ptime;
        this.sample = sample;
        this.codec = codec;
        this.devId = devId;
        audioMode = mode;

        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Create Audio, Mode = %s !!!!!!!",GetAudioModeName(audioMode));

        OpenSocket();
        OpenAudio();

        id = UniqueIDManager.GetUniqueID(this.devId,UniqueIDManager.AUDIO_UNIQUE_ID);
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Open Audio %s on %d peer %s:%d, Codec=%d, Sample=%d, PTime=%d, mode=%s",id,src,address,dst,codec,sample,ptime,GetAudioModeName(mode));

    }

    public void AudioSwitch(String devId,int src,int dst,String address,int sample,int ptime,int codec,int mode){

        boolean isSocketSwitch = false;
        boolean isAudioSwitch = false;
        boolean isDeviceSwitch = false;
        boolean isDestSwitch = false;

        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Switch From Audio %s mode %s!!!!!!!",id,GetAudioModeName(audioMode));


        if(srcPort!=src||sample!=this.sample||ptime!=this.ptime||codec!=this.codec||audioMode!=mode){
            CloseSocket();
            isSocketSwitch = true;

            CloseAudio();
            isAudioSwitch = true;
        }

        srcPort = src;
        this.ptime = ptime;
        this.sample = sample;
        this.codec = codec;
        this.audioMode = mode;

        if(isSocketSwitch) {
            OpenSocket();
        }
        if(isAudioSwitch) {
            OpenAudio();
        }

        if(devId.compareToIgnoreCase(this.devId)!=0){
            this.devId = devId;
            isDeviceSwitch = true;
        }

        if(dst!=dstPort||dstAddress.compareToIgnoreCase(address)!=0){
            dstPort = dst;
            dstAddress = address;
            isDestSwitch = true;
        }

        if(isDestSwitch||isSocketSwitch||isAudioSwitch||isDeviceSwitch){
            String oldId = id;
            id = UniqueIDManager.GetUniqueID(this.devId,UniqueIDManager.AUDIO_UNIQUE_ID);
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Switch Audio %s to %s on %d peer %s:%d, Codec=%d, Sample=%d, PTime=%d, mode=%s",oldId,id,src,address,dst,codec,sample,ptime,GetAudioModeName(audioMode));
        }else{
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Switch Audio %s on %d peer %s:%d, Codec=%d, Sample=%d, PTime=%d, mode=%s, but not Change",id,src,address,dst,codec,sample,ptime,GetAudioModeName(audioMode));
        }
    }

    public boolean AudioStop(String id){
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,String.format("Begin Stop Audio %s!!!!!!!",id));
        if(this.id.compareToIgnoreCase(id)==0) {
            CloseSocket();
            CloseAudio();
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Audio %s is Closed", id));
            return true;
        }else{
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Close Audio %s but is invalid, cur is %s", id,this.id));
            return false;
        }
    }

    public boolean AudioSuspend(String id){
        if(this.id.compareToIgnoreCase(id)==0){
            if(audioOpenCount<=0){
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, String.format("Audio %s had Closed , Couldn't suspend",id));
            }else{
                CloseSocket();
                CloseAudio();
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Audio %s is Suspend", id));
            }
            return true;
        }else{
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Suspend Audio %s but is invalid, cur is  %s", id,this.id));
            return false;
        }
    }

    public boolean AudioResume(String id){
        if(this.id.compareToIgnoreCase(id)==0){
            if(audioOpenCount>=1){
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, String.format("Audio %s had Open , Could't Open Again", id));
            }else{
                OpenSocket();
                OpenAudio();
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Audio %s is Resume", id));
            }
            return true;
        }else{
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Resume Audio %s but is invalid, cur is %s", id,this.id));
            return false;
        }
    }


    private void OpenSocket(){
        int count = 0;


        try {
            if(audioMode== AudioMode.RECV_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE||audioMode==AudioMode.SEND_ONLY_MODE){
                audioSocket = new DatagramSocket(srcPort);
                audioSocket.setSoTimeout(300);
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Open AudioSocket On Port %d when AudioMode = %s",srcPort,GetAudioModeName(audioMode));

                socketOpenCount++;
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After OpenSocket Count =%d",socketOpenCount);
            }

            if(audioMode== AudioMode.RECV_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE){
                if(jb==null) {
                    jb = new JitterBuffer();
                    jb.initJb();
                    jbIndex = jb.openJb(codec,ptime,sample);
                }else{
                    jb.resetJb(jbIndex);
                }
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Open JB when AudioMode = %s",GetAudioModeName(audioMode));

                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Start AudioSocketRead Thread when AudioMode = %s",GetAudioModeName(audioMode));

                socketReadThread = new SocketReadThread();
                socketReadThread.start();
                while(!isSockReadRuning){
                    try {
                        Thread.sleep(100);
                        count++;
                        if(count>200){
                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Audio Socket Thread is Still not Runing");
                            count = 0;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                
            }else{
                socketReadThread = null;
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public int GetAudioDelay(){
        if(aec==null)
            return -1;
        else
            return aec.getdelay();
    }

    private void CloseSocket(){
        int count = 0;
        if(audioSocket!=null) {
            if(socketReadThread!=null) {

                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Stop AudioSocketRead Thread ");
                socketReadThread.interrupt();
                while (isSockReadRuning) {
                    try {
                        Thread.sleep(100);
                        count++;
                        if (count > 20) {
                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, "Audio Socket Thread is Still Runing");
                            count = 0;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                socketReadThread = null;
            }

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close AudioSocket");
            if(!audioSocket.isClosed()){
                audioSocket.close();
            }

            socketOpenCount--;

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After CloseSocket Count =%d",socketOpenCount);
        }
    }

    private void OpenAudio(){
        int count = 0;
        int state;
        MobileAEC.SamplingFrequency aecSample;
        packSize = sample*ptime/1000;
        int nsMode;

        if(sample==8000)
            aecSample = MobileAEC.SamplingFrequency.FS_8000Hz;
        else if(sample==16000)
            aecSample = MobileAEC.SamplingFrequency.FS_16000Hz;
        else
            aecSample = MobileAEC.SamplingFrequency.FS_8000Hz;

        if(audioMode==AudioMode.SEND_RECV_MODE) {
            if(PhoneParam.aecMode!=PhoneParam.AUDIO_PROCESS_DISABLE){
                aec = new MobileAEC(aecSample);

                switch(PhoneParam.aecMode){
                    case PhoneParam.AUDIO_PROCESS_MILD:
                        aec.setAecmMode(MobileAEC.AggressiveMode.MILD).prepare();
                    break;
                    case PhoneParam.AUDIO_PROCESS_MEDIUM:
                        aec.setAecmMode(MobileAEC.AggressiveMode.MEDIUM).prepare();
                    break;
                    case PhoneParam.AUDIO_PROCESS_HIGH:
                        aec.setAecmMode(MobileAEC.AggressiveMode.HIGH).prepare();
                    break;
                    case PhoneParam.AUDIO_PROCESS_AGGRESSIVE:
                        aec.setAecmMode(MobileAEC.AggressiveMode.AGGRESSIVE).prepare();
                    break;
                    default :
                        aec.setAecmMode(MobileAEC.AggressiveMode.MOST_AGGRESSIVE).prepare();
                    break;
                        
                }
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Create AEC %s when AudioMode =%s ", GetAudioProcessModeName(PhoneParam.aecMode),GetAudioModeName(audioMode));
            }


            if(PhoneParam.nsMode!=PhoneParam.AUDIO_PROCESS_DISABLE){
                nsUtils = new NsUtils();
                nsHandle = nsUtils.nsxCreate();
                nsUtils.nsxInit(nsHandle,sample);
                nsMode = PhoneParam.nsMode;
                
                switch(PhoneParam.nsMode){
                    case PhoneParam.AUDIO_PROCESS_MILD:
                        nsMode = 0;
                    break;
                    case PhoneParam.AUDIO_PROCESS_MEDIUM:
                        nsMode = 1;
                    break;
                    case PhoneParam.AUDIO_PROCESS_HIGH:
                        nsMode = 2;
                    break;
                    default:
                        nsMode = 3;
                    break;
                }
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Create NS %s when AudioMode =%s ", GetAudioProcessModeName(PhoneParam.aecMode),GetAudioModeName(audioMode));
                nsUtils.nsxSetPolicy(nsHandle,nsMode);
            }

            if(PhoneParam.agcMode ==PhoneParam.AUDIO_PROCESS_ENABLE){
                inputAgc = new AgcUtils();
                inputAgc.setAgcConfig(3,20,1).prepare();
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "Create InputAGC when AudioMode =%s ", GetAudioModeName(audioMode));
            }

//            outputAgc = new AgcUtils();
//            outputAgc.setAgcConfig(20,20,1).prepare();
        }

        // create audio read device and write device int synchronized

        if(audioMode==AudioMode.SEND_RECV_MODE||audioMode==AudioMode.RECV_ONLY_MODE){

            int audioOutBufSize = AudioTrack.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            int outMode;
            switch(PhoneParam.outputMode){
                case PhoneParam.AUDIO_OUTPUT_CALL:
                    outMode = AudioManager.STREAM_VOICE_CALL;
                    break;
                case PhoneParam.AUDIO_OUTPUT_SYSTEM:
                    outMode = AudioManager.STREAM_SYSTEM;
                    break;
                default:
                    outMode =  AudioManager.STREAM_MUSIC;
                    break;
            }
            player = new AudioTrack(outMode, sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
//                    packSize,
                    audioOutBufSize,
                    AudioTrack.MODE_STREAM);

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create Player when AudioMode =%s ",GetAudioModeName(audioMode));

            state =  player.getState();
            if(state!=AudioTrack.STATE_INITIALIZED){
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"AudioTrack Init Fail State=%d, sample=%d,PTime=%d,packSize=%d,audioOutBufSize=%d",state,sample,ptime,packSize,audioOutBufSize);
            }else{
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"AudioTrack Init Success, sample=%d,PTime=%d,packSize=%d,audioOutBufSize=%d",sample,ptime,packSize,audioOutBufSize);
            }

            player.play();

            if(audioMode==AudioMode.SEND_RECV_MODE) {
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create AudioWrite Thread when AudioMode =%s ",GetAudioModeName(audioMode));
                audioWriteThread = new AudioWriteThread();
                audioWriteThread.start();
            }else if(audioMode==AudioMode.RECV_ONLY_MODE){
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create AudioWriteSimple Thread when AudioMode =%s ",GetAudioModeName(audioMode));
                audioWriteSimpleThread = new AudioWriteSimpleThread();
                audioWriteSimpleThread.start();
            }

            while(!isAudioWriteRuning){
                try {
                    Thread.sleep(100);
                    count++;
                    if(count>20){
                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Audio Write Thread is Still not Runing");
                        count = 0;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if(audioMode==AudioMode.SEND_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE) {
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create Recorder when AudioMode =%s ",GetAudioModeName(audioMode));

            int audioInBufSize = AudioRecord.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            int inMode;
            switch(PhoneParam.inputMode){
                case PhoneParam.AUDIO_INPUT_CALL:
                    inMode = MediaRecorder.AudioSource.VOICE_CALL;
                    break;
                case PhoneParam.AUDIO_INPUT_CAMCORDER:
                    inMode = MediaRecorder.AudioSource.CAMCORDER;
                    break;
                case PhoneParam.AUDIO_INPUT_COMMUNICATION:
                    inMode = MediaRecorder.AudioSource.VOICE_COMMUNICATION;
                    break;
                case PhoneParam.AUDIO_INPUT_DEFAULT:
                    inMode = MediaRecorder.AudioSource.DEFAULT;
                    break;
                default:
                    inMode = MediaRecorder.AudioSource.MIC;
                    break;

            }
            recorder = new AudioRecord(inMode, sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                audioInBufSize);
//                    packSize);

            state = recorder.getState();
            if (state != AudioRecord.STATE_INITIALIZED) {
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, "AudioRecord Init Fail state=%d, sample=%d,PTime=%d,packSize=%d,audioInBufSize=%d", state, sample, ptime, packSize, audioInBufSize);
            } else {
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, "AudioRecord Init Success, sample=%d,PTime=%d,packSize=%d,audioInBufSize=%d", sample, ptime, packSize, audioInBufSize);
            }

            recorder.startRecording();

            if(audioMode==AudioMode.SEND_RECV_MODE) {
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create AudioRead Thread when AudioMode =%s ",GetAudioModeName(audioMode));
                audioReadThread = new AudioReadThread();
                audioReadThread.start();
            }else if(audioMode==AudioMode.SEND_ONLY_MODE){
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create AudioReadSimple Thread when AudioMode =%s ",GetAudioModeName(audioMode));
                audioReadSimpleThread = new AudioReadSimpleThread();
                audioReadSimpleThread.start();
            }

            while (!isAudioReadRuning) {
                try {
                    Thread.sleep(100);
                    count++;
                    if (count > 20) {
                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, "Audio Read Thread is Still not Runing");
                        count = 0;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        
        audioOpenCount++;
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After OpenAudio Count =%d",audioOpenCount);

    }

    private void CloseAudio(){

        int waitCount = 0;
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close Audio !!!!!!!");

        if(audioReadThread!=null) {
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close AudioRead Thread");
            audioReadThread.interrupt();
            audioReadThread = null;
        }

        if(audioReadSimpleThread!=null){
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close AudioReadSimple Thread");
            audioReadSimpleThread.interrupt();
            audioReadSimpleThread = null;
        }

        while(isAudioReadRuning){
            try {
                Thread.sleep(100);
                waitCount++;
                if(waitCount>200){
                    LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Audio Read Thread Still Runing after interrupt");
                    waitCount = 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        audioWriteHandlerEnabled = false;
        if(audioWriteHandler !=null) {
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close AudioWrite Thread");
            audioWriteHandler.getLooper().quit();
        }
        if(audioWriteSimpleThread!=null){
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close AudioWriteSimple Thread");
            audioWriteSimpleThread.interrupt();
            audioWriteSimpleThread = null;
        }

        while(isAudioWriteRuning){
            try {
                Thread.sleep(100);
                waitCount++;
                if(waitCount>200){
                    LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Audio Write Thread Still Runing after interrupt");
                    waitCount = 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        while(recorder!=null||player!=null) {
            try {
                Thread.sleep(100);
                waitCount++;
                if(waitCount>20){
                    LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"recorder and player is not NULL after Close");
                    waitCount = 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if(aec!=null){
            aec.close();
            aec = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close AEC");
        }

        if(nsUtils!=null){
            nsUtils.nsxFree(nsHandle);
            nsUtils = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close NS");
        }

        if(jb!=null) {
            jb.closeJb(jbIndex);
            jb.deInitJb();
            jb = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close JB");
        }

        if(inputAgc!=null){
            inputAgc.close();
            inputAgc = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close input AGC");
        }

        if(outputAgc!=null){
            outputAgc.close();
            outputAgc = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close output AGC");
        }

        audioOpenCount--;
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After CloseAudio Count =%d",audioOpenCount);
    }


    // read data from socket and save to JB
    class SocketReadThread extends Thread{
        public SocketReadThread(){
            super("AudioSocketRead");
        }
    
        @Override
        public void run() {
            byte[] recvBuf=new byte[1024];
            DatagramPacket recvPack;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio SocketReadThread");
            isSockReadRuning = true;
            while(!isInterrupted()){
                if(!audioSocket.isClosed()){
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        audioSocket.receive(recvPack);
                        if(recvPack.getLength()>0){
//                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Recv %d byte from %s:%d",recvPack.getLength(),recvPack.getAddress().getHostAddress(),recvPack.getPort());
                            if(recvPack.getPort()==dstPort&&recvPack.getAddress().getHostAddress().compareToIgnoreCase(dstAddress)==0){
                                if(jb==null){
                                    UserInterface.PrintLog("jb is NULL!!!!!!!!!!!!!!!!!!");
                                }else
                                    jb.addPackage(jbIndex,recvPack.getData(),recvPack.getLength());
                            }
                        }else{
                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Audio Socket Recv 0 bytes");
                        }
                    } catch (IOException e) {
//                        e.printStackTrace();
//                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s err with %s",id,devId,e.getMessage());
//                    } catch (NullPointerException e){
//                        e.printStackTrace();
//                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s err with %s",id,devId,e.getMessage());
//                        break;
                    }catch(Exception ee){
                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s err with %s",id,devId,ee.getMessage());
                    }
                }else{
                    break;
                }

            }
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio SocketReadThread");
            isSockReadRuning = false;
        }
    }

    class AudioReadSimpleThread extends Thread{
        public AudioReadSimpleThread(){
            super("AudioSimpleRead");
        }

        @Override
        public void run() {
            short[] audioReadData = new short[packSize];
            byte[] rtpData;
            isAudioReadRuning = true;
            while (!isInterrupted()) {
                int readNum = recorder.read(audioReadData, 0, packSize);
                rtpData = Rtp.EncloureRtp(audioReadData, codec);
                DatagramPacket dp = new DatagramPacket(rtpData, rtpData.length);
                try {
                    dp.setAddress(InetAddress.getByName(dstAddress));
                    dp.setPort(dstPort);
                    if (!audioSocket.isClosed()) {
                        audioSocket.send(dp);
                        //LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Send %d byte to %s:%d",rtpData.length,dstAddress,dstPort);
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
//                            e.printStackTrace();
//                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s is Closed when Send",id,devId);
                }

            }

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Release Audio Recorder");
            recorder.stop();
            recorder.release();
            recorder = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit AudioReadSimple Thread");
            isAudioReadRuning = false;
        }
    }


    // get data from JB and notify play thread; read date from audio , AEC and send .

    private short[] AecProcess(short[] farData,short[] nearData){
        int size = nearData.length;
        short[] aecData;
        if(aec==null){
            aecData = nearData;
        }else {
            aecData =new short[size];
            try {
                aec.farendBuffer(farData, farData.length);
                aec.echoCancellation(nearData, null, aecData, (short) packSize, (short) PhoneParam.aecDelay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return aecData;
    }

    private short[] InputAgcProcess(short[] data){
        int size = data.length;
        short[] agcData;

        if(inputAgc==null){
            agcData = data;
        }else{
            agcData = new short[size];
            inputAgc.agcProcess(data, 0, size, agcData, 0, 0, 0, 0);
        }
        return agcData;
    }

    private short[] OutputAgcProcess(short[] data){
        int size = data.length;
        short[] agcData;

        if(outputAgc==null){
            agcData = data;
        }else{
            agcData = new short[size];
            outputAgc.agcProcess(data, 0, size, agcData, 0, 0, 0, 0);
        }
        return agcData;
    }

    private int VolumeControl(short[] data,int gain){
        int size =data.length;
        int iTmp;

        if(gain<=0||gain>7)
            return -1;
        for(iTmp=0;iTmp<size;iTmp++) {
            switch (gain) {
                case 1:
                    data[iTmp] = (short) (data[iTmp] / 3 * 2);
                    break;
                case 2:
                    data[iTmp] = (short) (data[iTmp] / 2);
                    break;
                case 3:
                    data[iTmp] = (short) (data[iTmp] / 3);
                    break;
                case 4:
                    data[iTmp] = (short) (data[iTmp] / 4);
                    break;
                case 5:
                    data[iTmp] = (short) (data[iTmp] / 6);
                    break;
                case 6:
                    data[iTmp] = (short) (data[iTmp] / 8);
                    break;
                case 7:
                    data[iTmp] = (short) (data[iTmp] / 10);
                    break;
            }
        }
        return 0;
    }

    short[] NSProcess(short[] data){
        int size = data.length;
        int iTmp;
        short[] nsData;
        if(nsUtils!=null) {
            short[] blockSrc = new short[80];
            short[] blockDst = new short[80];
            nsData = new short[size];
            for(iTmp=0;iTmp<size;iTmp+=80){
                System.arraycopy(data,iTmp,blockSrc,0,80);
                nsUtils.nsxProcess(nsHandle,blockSrc, null, blockDst, null);
                System.arraycopy(blockDst,0,nsData,iTmp,80);
            }
        }else{
            nsData = data;
        }
        return nsData;
    }

    int CutNoiseProcess(short[] data){
        int totalValue;
        int iTmp;
        int k;
        int curK;
        int valueTmp;
        int avgR;
        int size;

        size = data.length;
        if (PhoneParam.nsThreshold > 0) {
            int kCount = PhoneParam.nsTime / 20;
            if (kCount < 1)
                kCount = 1;
            totalValue = 0;
            for (iTmp = 0; iTmp < size; iTmp++) {
                if (data[iTmp] >= 0)
                    totalValue += data[iTmp];
                else
                    totalValue += -data[iTmp];
            }

            avgR = totalValue / size;

            if (avgR < PhoneParam.nsThreshold) {
                k = 0;
            } else if (avgR < PhoneParam.nsThreshold + PhoneParam.nsRange) {
                int kTmp = PhoneParam.nsRange / 100;
                if (kTmp < 1)
                    kTmp = 1;
                k = (avgR - PhoneParam.nsThreshold) / kTmp + 1;
            } else {
                k = 100;
            }
            karray[kpos] = k;
            kpos++;
            if (kpos >= kCount) {
                kpos = 0;
            }
            curK = 0;
            for (iTmp = 0; iTmp < kCount; iTmp++) {
                curK += karray[iTmp];
            }
            curK = curK / kCount;

            for (iTmp = 0; iTmp < size; iTmp++) {
                valueTmp = data[iTmp] * curK;
                valueTmp = valueTmp / 100;
                data[iTmp] = (short) valueTmp;
            }
        }
        return 0;
    }

    class AudioReadThread extends Thread{
        public AudioReadThread(){
            super("AudioRead");
        }
    
        @Override
        public void run() {
            byte[] jbData = new byte[packSize];
            int jbDataLen;
            short[] pcmData;
            byte[] rtpData;
            short[] audioReadData = new short[packSize];

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio AudioReadThread");

            isAudioReadRuning = true;

            while (!isInterrupted()) {
                jbDataLen = jb.getPackage(jbIndex, jbData, packSize);
                if (jbDataLen > 0) {
                    pcmData = Rtp.UnenclosureRtp(jbData, jbDataLen, codec);

                    pcmData = OutputAgcProcess(pcmData);

                    CutNoiseProcess(pcmData);

                    VolumeControl(pcmData,PhoneParam.outputGain);

                    // notify play thread;
                    if (audioMode == AudioMode.RECV_ONLY_MODE || audioMode == AudioMode.SEND_RECV_MODE) {
                        if (audioWriteHandlerEnabled && audioWriteHandler != null) {
                            Message playMsg = audioWriteHandler.obtainMessage();
                            playMsg.arg1 = AUDIO_PLAY_MSG;
                            playMsg.obj = pcmData;
                            audioWriteHandler.sendMessage(playMsg);
                        }
                    }

                    //read ;
                    int readNum = recorder.read(audioReadData, 0, packSize);

                    audioReadData = InputAgcProcess(audioReadData);

                    audioReadData = AecProcess(pcmData,audioReadData);

                    audioReadData = NSProcess(audioReadData);

                    VolumeControl(audioReadData,PhoneParam.inputGain);

                    // rtp packet and send
                    if((audioMode==AudioMode.SEND_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE)&&audioSocket!=null){
                        rtpData = Rtp.EncloureRtp(audioReadData, codec);
                        DatagramPacket dp = new DatagramPacket(rtpData, rtpData.length);
                        try {
                            dp.setAddress(InetAddress.getByName(dstAddress));
                            dp.setPort(dstPort);
                            if (!audioSocket.isClosed()) {
                                audioSocket.send(dp);
                                //LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Send %d byte to %s:%d",rtpData.length,dstAddress,dstPort);
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
//                            e.printStackTrace();
//                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s is Closed when Send",id,devId);
                        }
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // interrupted exception maybe catch here , and should break while
                        break;
                    }
                }
            }
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Release Audio Recorder");
            recorder.stop();
            recorder.release();
            recorder = null;
            isAudioReadRuning = false;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio AudioReadThread");
        }
    }

    // audio play
    class AudioWriteThread extends Thread{
        public AudioWriteThread(){
            super("AudioWrite");
        }
        
        @Override
        public void run() {
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio AudioWriteThread");

            isAudioWriteRuning = true;

           Looper.prepare();
            audioWriteHandler = new Handler(message -> {
                if (message.arg1 == AUDIO_PLAY_MSG) {
                    short[] rtpData = (short[]) message.obj;

                    if (player != null)
                        player.write(rtpData, 0, rtpData.length);
                }
                return false;
            });
            audioWriteHandlerEnabled = true;

            Looper.loop();

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Release Audio Player");
            player.stop();
            player.release();
            player = null;

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio AudioWriteThread");
            audioWriteHandler = null;
            isAudioWriteRuning = false;
        }
    }

    class AudioWriteSimpleThread extends Thread{
        public AudioWriteSimpleThread(){
            super("AudioSimpleWrite");
        }

        @Override
        public void run() {
            byte[] jbData = new byte[packSize];
            int jbDataLen;
            short[] pcmData;
            isAudioWriteRuning = true;
            while (!isInterrupted()) {
                jbDataLen = jb.getPackage(jbIndex, jbData, packSize);
                if (jbDataLen > 0) {
                    pcmData = Rtp.UnenclosureRtp(jbData, jbDataLen, codec);
                    if (player != null)
                        player.write(pcmData, 0, pcmData.length);
                }else{
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        // interrupted exception maybe catch here , and should break while
                        break;
                    }
                }
            }
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Release Audio Player");
            player.stop();
            player.release();
            player = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit AudioWriteSimple Thread");
            isAudioWriteRuning = false;
        }
    }

    private String GetAudioModeName(int mode){
        String audioModeName = "Unknow Mode";

        switch(mode){
            case AudioMode.RECV_ONLY_MODE:
                audioModeName = "RECV_ONLY";
            break;
            case AudioMode.SEND_ONLY_MODE:
                audioModeName = "SEND_ONLY";
            break;
            case AudioMode.SEND_RECV_MODE:
                audioModeName = "SEND_RECV";
                break;
            case AudioMode.NO_SEND_RECV_MODE:
                audioModeName = "NO_SEND_RECV";
                break;
        }

        return audioModeName;
    }

    private String GetAudioProcessModeName(int mode){
        String audioprocessModeName = "Unknow Mode";

        switch(mode){
            case PhoneParam.AUDIO_PROCESS_AGGRESSIVE:
                audioprocessModeName = "AGGRESSIV MODE";
                break;
            case PhoneParam.AUDIO_PROCESS_MOST_AGGRESSIVE:
                audioprocessModeName = "MOST AGGRESSIV MODE";
                break;
            case PhoneParam.AUDIO_PROCESS_HIGH:
                audioprocessModeName = "HIGH MODE";
                break;
            case PhoneParam.AUDIO_PROCESS_MEDIUM:
                audioprocessModeName = "MEDIUM MODE";
                break;
            case PhoneParam.AUDIO_PROCESS_MILD:
                audioprocessModeName = "MILD MODE";
                break;
        }

        return audioprocessModeName;
    }

    public int RestartAudio(){
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Restart Audio  !!!!!!!");
        CloseSocket();
        CloseAudio();
        OpenSocket();
        OpenAudio();
        return 0;
    }
}
