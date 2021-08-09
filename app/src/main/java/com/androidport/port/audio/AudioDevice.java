package com.androidport.port.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;


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
        packSize = sample*ptime/1000;

        if(audioMode==AudioMode.SEND_RECV_MODE){
//            aec =new MobileAEC(new SamplingFrequency(sample));
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Create AEC when AudioMode =%s ",GetAudioModeName(audioMode));

            aec =new MobileAEC(null);
            aec.setAecmMode(MobileAEC.AggressiveMode.MOST_AGGRESSIVE).prepare();
        }

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

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
//                audioInBufSize);
                    packSize);

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

        if(jb!=null) {
            jb.closeJb(jbIndex);
            jb.deInitJb();
            jb = null;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Close JB");
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
            short[] aecData ;

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
                    int readNum = recorder.read(audioReadData, 0, packSize);
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
                    if((audioMode==AudioMode.SEND_ONLY_MODE||audioMode==AudioMode.SEND_RECV_MODE)&&audioSocket!=null){
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
                    //LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Recv Play Msg with %d Byte Data",rtpData.length);
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
}
