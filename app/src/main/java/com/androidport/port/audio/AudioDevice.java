package com.androidport.port.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.android.webrtc.audio.MobileAEC;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.pub.AudioMode;
import com.example.nettytest.userinterface.PhoneParam;
import com.example.nettytest.userinterface.UserInterface;
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

    AudioRecord recorder = null;
    AudioTrack player = null;

    JitterBuffer jb;
    int jbIndex;

    int socketOpenCount = 0;
    int audioOpenCount = 0;

    int packSize;

    final int AUDIO_PLAY_MSG = 1;

    public String id;
    public String devId;

    MobileAEC aec;

    boolean isSockReadRuning = false;
    boolean isAudioReadRuning = false;
    boolean isAudioWriteRuning = false;

    int audioMode;

    static{
        System.loadLibrary("JitterBuffer");
    }

    public AudioDevice(String devId,int src,int dst,String address,int sample,int ptime,int codec,int mode){

        dstPort = dst;
        srcPort = src;
        dstAddress = address;
        this.ptime = ptime;
        this.sample = sample;
        this.codec = codec;
        this.devId = devId;
        audioMode = mode;

        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Create Audio, Mode = %d !!!!!!!",audioMode);

        OpenSocket();
        OpenAudio();

        id = UniqueIDManager.GetUniqueID(this.devId,UniqueIDManager.AUDIO_UNIQUE_ID);
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Open Audio %s on %d peer %s:%d, Codec=%d, Sample=%d, PTime=%d, mode=%d",id,src,address,dst,codec,sample,ptime,mode);

    }

    public void AudioSwitch(String devId,int src,int dst,String address,int sample,int ptime,int codec,int mode){

        boolean isSocketSwitch = false;
        boolean isAudioSwitch = false;
        boolean isDeviceSwitch = false;
        boolean isDestSwitch = false;

        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Switch From Audio %s mode %d!!!!!!!",id,audioMode);

       if(srcPort!=src||audioMode!=mode){
            // do something to reopen socket;
            CloseSocket();
            isSocketSwitch = true;
        }

        if(sample!=this.sample||ptime!=this.ptime||codec!=this.codec||audioMode!=mode){
            // do something to reset audio thread
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
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Switch Audio %s to %s on %d peer %s:%d, Codec=%d, Sample=%d, PTime=%d, mode=%d",oldId,id,src,address,dst,codec,sample,ptime,audioMode);
        }else{
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Switch Audio %s on %d peer %s:%d, Codec=%d, Sample=%d, PTime=%d, mode=%d, but not Change",id,src,address,dst,codec,sample,ptime,audioMode);
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
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_ERROR, String.format("Audio %s had Closed , Could't suspend"));
            }else{
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
                jb.resetJb(jbIndex);
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

        jb = new JitterBuffer();
        jb.initJb();
        jbIndex = jb.openJb(codec,ptime,sample);

        try {
            if(audioMode== AudioMode.RECV_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE){
                audioSocket = new DatagramSocket(srcPort);
                audioSocket.setSoTimeout(300);
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
            socketOpenCount++;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After OpenSocket Count =%d",socketOpenCount);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void CloseSocket(){
        int count = 0;
        if(audioSocket!=null) {
            if(socketReadThread!=null)
                socketReadThread.interrupt();
            while(isSockReadRuning){
                try {
                    Thread.sleep(100);
                    count++;
                    if(count>200){
                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Audio Socket Thread is Still Runing");
                        count = 0;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if(!audioSocket.isClosed()){
                audioSocket.close();
            }
            socketReadThread = null;
            socketOpenCount--;

            jb.closeJb(jbIndex);
            jb.deInitJb();

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After CloseSocket Count =%d",socketOpenCount);
        }
    }

    private void OpenAudio(){
        int count = 0;
        int state;
        if(audioMode==AudioMode.SEND_RECV_MODE){
//            aec =new MobileAEC(new SamplingFrequency(sample));
            aec =new MobileAEC(null);
            aec.setAecmMode(MobileAEC.AggressiveMode.MOST_AGGRESSIVE).prepare();
        }


        packSize = sample*ptime/1000;

        // create audio read device and write device int synchronized

        if(audioMode==AudioMode.SEND_RECV_MODE||audioMode==AudioMode.RECV_ONLY_MODE){

            int audioOutBufSize = AudioTrack.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);


            player = new AudioTrack(AudioManager.STREAM_MUSIC, sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    packSize,
//                    audioOutBufSize,
                    AudioTrack.MODE_STREAM);

            state =  player.getState();
            if(state!=AudioTrack.STATE_INITIALIZED){
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"AudioTrack Init Fail State=%d, sample=%d,PTime=%d,packSize=%d,audioOutBufSize=%d",state,sample,ptime,packSize,audioOutBufSize);
            }else{
                LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"AudioTrack Init Success, sample=%d,PTime=%d,packSize=%d,audioOutBufSize=%d",sample,ptime,packSize,audioOutBufSize);
            }

            player.play();

            audioWriteThread = new AudioWriteThread();
            audioWriteThread.start();

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


        int audioInBufSize = AudioRecord.getMinBufferSize(sample,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sample,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
//                audioInBufSize);
                packSize);

        state = recorder.getState();
        if(state!=AudioRecord.STATE_INITIALIZED){
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"AudioRecord Init Fail state=%d, sample=%d,PTime=%d,packSize=%d,audioInBufSize=%d",state,sample,ptime,packSize,audioInBufSize);
        }else{
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"AudioRecord Init Success, sample=%d,PTime=%d,packSize=%d,audioInBufSize=%d",sample,ptime,packSize,audioInBufSize);
        }

        recorder.startRecording();

        audioReadThread = new AudioReadThread();
        audioReadThread.start();
        while(!isAudioReadRuning){
            try {
                Thread.sleep(100);
                count++;
                if(count>20){
                    LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Audio Read Thread is Still not Runing");
                    count = 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        audioOpenCount++;
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After OpenAudio Count =%d",audioOpenCount);

    }

    private void CloseAudio(){

        int waitCount = 0;
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close Audio !!!!!!!");

        if(audioReadThread!=null) {
            audioReadThread.interrupt();
            audioReadThread = null;
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
            audioWriteHandler.getLooper().quit();
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
                if(waitCount>200){
                    LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"recorder and player is not NULL after Close");
                    waitCount = 0;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        if(audioMode==AudioMode.SEND_RECV_MODE){
            aec.close();
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
                    try {
                        recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                        audioSocket.receive(recvPack);
                        if(recvPack.getLength()>0){
//                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Recv %d byte from %s:%d",recvPack.getLength(),recvPack.getAddress().getHostName(),recvPack.getPort());
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
                        e.printStackTrace();
                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s err with %s",id,devId,e.getMessage());
                    } catch (NullPointerException e){
                        e.printStackTrace();
                        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s err with %s",id,devId,e.getMessage());
                        break;
                    }
                }else{
                    break;
                }

            }
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio SocketReadThread");
            isSockReadRuning = false;
        }
    }

    // get data from JB and notify play thread; read date from audio , AEC and send .
    class AudioReadThread extends Thread{
        public AudioReadThread(){
            super("AudioDeviceRead");
        }
    
        @Override
        public void run() {
            byte[] jbData = new byte[packSize];
            int jbDataLen;
            short[] pcmData;
            byte[] rtpData;
            short[] audioReadData = new short[packSize];
            short[] aecData ;
            int readNum;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio AudioReadThread");


            isAudioReadRuning = true;
            
            if(audioMode==AudioMode.SEND_RECV_MODE){
                aecData = new short[packSize];
            }else{
                aecData = audioReadData;
            }

            while (!isInterrupted()) {
                jbDataLen = jb.getPackage(jbIndex, jbData, packSize);
                if (jbDataLen > 0) {
                    pcmData = Rtp.UnenclosureRtp(jbData, jbDataLen, codec);
                    // notify play thread;
                    if(audioMode==AudioMode.RECV_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE){
                        if (audioWriteHandlerEnabled && audioWriteHandler != null) {
                            Message playMsg = audioWriteHandler.obtainMessage();
                            playMsg.arg1 = AUDIO_PLAY_MSG;
                            playMsg.obj = pcmData;
                            audioWriteHandler.sendMessage(playMsg);
                        }
                    }

                    //read ;
                    readNum = recorder.read(audioReadData, 0, packSize);
//                    LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Read %d sample from Audio device, Return =%d",packSize,readNum);

                    //aec process
                    if(audioMode==AudioMode.SEND_RECV_MODE){
                        try {
                            aec.farendBuffer(pcmData,pcmData.length);
                            aec.echoCancellation(audioReadData,null,aecData,(short)packSize,(short) PhoneParam.aecDelay);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        aecData = audioReadData;
                    }
                    // rtp packet and send
                    if(audioMode==AudioMode.SEND_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE&&audioSocket!=null){
                        rtpData = Rtp.EncloureRtp(aecData, codec);
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
                            e.printStackTrace();
                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_ERROR,"Socket of Audio %s of Dev %s is Closed when Send",id,devId);
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
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio AudioReadThread");

            recorder.stop();
            recorder.release();
            recorder = null;
            isAudioReadRuning = false;
        }
    }

    // audio play
    class AudioWriteThread extends Thread{
        public AudioWriteThread(){
            super("AudioDeviceWrite");
        }
        
        @Override
        public void run() {
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio AudioWriteThread");

            isAudioWriteRuning = true;

           Looper.prepare();
            audioWriteHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(@NonNull Message message) {
                    if (message.arg1 == AUDIO_PLAY_MSG) {
                        short[] rtpData = (short[]) message.obj;
                        //LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Recv Play Msg with %d Byte Data",rtpData.length);
                        if (player != null)
                            player.write(rtpData, 0, rtpData.length);
                    }
                    return false;
                }
            });
            audioWriteHandlerEnabled = true;

            Looper.loop();

            player.stop();
            player.release();
            player = null;

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio AudioWriteThread");
            isAudioWriteRuning = false;
        }
    }
}
