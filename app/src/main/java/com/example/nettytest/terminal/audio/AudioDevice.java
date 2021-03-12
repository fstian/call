package com.example.nettytest.terminal.audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.android.webrtc.audio.MobileAEC;
import com.android.webrtc.audio.MobileAEC.SamplingFrequency;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.UniqueIDManager;
import com.example.nettytest.userinterface.PhoneParam;
import com.witted.ptt.JitterBuffer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class AudioDevice {

    public final static int NO_SEND_RECV_MODE = 0;
    public final static int RECV_ONLY_MODE = 1;
    public final static int SEND_ONLY_MODE = 2;
    public final static int SEND_RECV_MODE = 3;
    
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

    int audioMode;

    static{
        System.loadLibrary("JitterBuffer");
        System.loadLibrary("webrtc_aecm");
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
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE, LogWork.LOG_DEBUG, String.format("Close Audio %s but is invalid", id));
            return false;
        }
    }

    private void OpenSocket(){
        try {
            audioSocket = new DatagramSocket(srcPort);
            if(audioMode==RECV_ONLY_MODE||audioMode==SEND_RECV_MODE){
                socketReadThread = new SocketReadThread();
                socketReadThread.start();
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
        if(audioSocket!=null) {
            if(socketReadThread!=null)
                socketReadThread.interrupt();
            audioSocket.close();
            socketReadThread = null;
            audioSocket = null;
            socketOpenCount--;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After CloseSocket Count =%d",socketOpenCount);
        }
    }

    private void OpenAudio(){

        if(audioMode==SEND_RECV_MODE){
//            aec =new MobileAEC(new SamplingFrequency(sample));
            aec =new MobileAEC(null);
            aec.setAecmMode(MobileAEC.AggressiveMode.MOST_AGGRESSIVE).prepare();
        }

        jb = new JitterBuffer();
        jb.initJb();
        jbIndex = jb.openJb(codec,ptime,sample);

        packSize = sample*ptime/1000;

        if(audioMode==SEND_RECV_MODE||audioMode==RECV_ONLY_MODE){
            audioWriteThread = new AudioWriteThread();
            audioWriteThread.start();
        }

        audioReadThread = new AudioReadThread();
        audioReadThread.start();
        
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

        if(socketReadThread!=null) {
            socketReadThread.interrupt();
            socketReadThread = null;
        }

        audioWriteHandlerEnabled = false;
        if(audioWriteHandler !=null) {
            audioWriteHandler.getLooper().quit();
        }

        try {
            while(recorder!=null||player!=null) {
                if(waitCount<50) {
                    Thread.sleep(20);
                }else{
                    break;
                }
                waitCount++;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        jb.closeJb(jbIndex);
        jb.deInitJb();

        if(audioMode==SEND_RECV_MODE){
            aec.close();
        }

        audioOpenCount--;
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"After CloseAudio Count =%d",audioOpenCount);
    }


    // read data from socket and save to JB
    class SocketReadThread extends Thread{
        @Override
        public void run() {
            byte[] recvBuf=new byte[1024];
            DatagramPacket recvPack;
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio SocketReadThread");
            while(!isInterrupted()){
                DatagramSocket curSocket = audioSocket;
                if(curSocket!=null){
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    try {
                        curSocket.receive(recvPack);
                        if(recvPack.getLength()>0){
//                            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Recv %d byte from %s:%d",recvPack.getLength(),recvPack.getAddress().getHostName(),recvPack.getPort());
                            if(recvPack.getPort()==dstPort&&recvPack.getAddress().getHostAddress().compareToIgnoreCase(dstAddress)==0){
                                jb.addPackage(jbIndex,recvPack.getData(),recvPack.getLength());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio SocketReadThread");
        }
    }

    // get data from JB and notify play thread; read date from audio , AEC and send .
    class AudioReadThread extends Thread{
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

            int audioInBufSize = AudioRecord.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    packSize);

            recorder.startRecording();

            if(audioMode==SEND_RECV_MODE){
                aecData = new short[packSize];
            }else{
                aecData = audioReadData;
            }

            while (!isInterrupted()) {
                jbDataLen = jb.getPackage(jbIndex, jbData, packSize);
                if (jbDataLen > 0) {
                    pcmData = Rtp.UnenclosureRtp(jbData, jbDataLen, codec);
                    // notify play thread;
                    if(audioMode==RECV_ONLY_MODE||audioMode==SEND_RECV_MODE){
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
                    if(audioMode==SEND_RECV_MODE){
                        try {
                            aec.farendBuffer(pcmData,pcmData.length);
                            aec.echoCancellation(audioReadData,null,aecData,(short)packSize,(short) PhoneParam.aecDelay);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
//                        aecData = audioReadData;
                    }
                    // rtp packet and send
                    if(audioMode==SEND_ONLY_MODE||audioMode==SEND_RECV_MODE){
                        rtpData = Rtp.EncloureRtp(aecData, codec);
                        DatagramPacket dp = new DatagramPacket(rtpData, rtpData.length);
                        try {
                            dp.setAddress(InetAddress.getByName(dstAddress));
                            dp.setPort(dstPort);
                            DatagramSocket curSocket = audioSocket;
                            if (curSocket != null) {
                                curSocket.send(dp);
                                //LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Send %d byte to %s:%d",rtpData.length,dstAddress,dstPort);
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio AudioReadThread");

            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

    // audio play
    class AudioWriteThread extends Thread{
        @Override
        public void run() {
            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Begin Audio AudioWriteThread");

            int audioOutBufSize = AudioTrack.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);


            player = new AudioTrack(AudioManager.STREAM_MUSIC, sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    packSize,
                    AudioTrack.MODE_STREAM);


            player.play();

            Looper.prepare();
            audioWriteHandler = new Handler(message -> {
                if(message.arg1 == AUDIO_PLAY_MSG){
                    short[] rtpData = (short [])message.obj;
                    //LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Recv Play Msg with %d Byte Data",rtpData.length);
                    if(player!=null)
                        player.write(rtpData,0,rtpData.length);
                }
                return false;
            });
            audioWriteHandlerEnabled = true;

            Looper.loop();

            player.stop();
            player.release();
            player = null;

            LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Exit Audio AudioWriteThread");

        }
    }
}
