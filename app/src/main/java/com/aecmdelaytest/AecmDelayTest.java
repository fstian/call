package com.aecmdelaytest;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class AecmDelayTest {

    static Handler audioWriteHandler;
    static int sample;
    static int outMode;
    static int inMode;
    static int packSize;
    static AudioReadThread audioReadThread;
    static AudioWriteThread audioWriteThread;
    static int status = 0;
    static int putOffset = 0;

    static class AudioReadThread extends Thread {
        @Override
        public void run() {
            short musicData[];
            int offset = 0;
            short[] audioReadData = new short[packSize];

            int audioInBufSize = AudioRecord.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioRecord recorder = new AudioRecord(inMode, sample,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
//                    audioInBufSize);
                    packSize);

            recorder.startRecording();

            while(!interrupted()){
                musicData = MusicData.GetMusicData(offset,packSize);
                if(musicData==null){
                    break;
                }
                offset+=packSize;

                Message msg = new Message();
                msg.arg1 = 1;
                msg.obj = musicData;
                audioWriteHandler.sendMessage(msg);

                int readNum = recorder.read(audioReadData, 0, packSize);
                MusicData.PutRecordData(audioReadData,putOffset,packSize);
                putOffset+=packSize;

            }
            audioWriteHandler.getLooper().quit();
            recorder.release();
            MusicData.SaveRecordData();
            status = 0;
        }
    }

    static class AudioWriteThread extends Thread{
        public AudioWriteThread(){
            super();
        }
        @Override
        public void run() {

            int audioOutBufSize = AudioTrack.getMinBufferSize(sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            AudioTrack player = new AudioTrack(outMode, sample,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
//                    audioOutBufSize,
                    packSize,
                    AudioTrack.MODE_STREAM);

            player.play();

            Looper.prepare();
            audioWriteHandler = new Handler(message->{
                if(message.arg1==1){
                    short pcmData[] = (short [])message.obj;
                    player.write(pcmData, 0, pcmData.length);
                }

                return false;
            });

            audioReadThread = new AudioReadThread();
            audioReadThread.start();

            Looper.loop();
            player.release();
        }
    }
    public static int StartTest(int s,int i,int o,int p){
        if(status==0) {
            status = 1;
            sample = s;
            inMode = i;
            outMode = o;
            packSize = p;
            putOffset = 0;
            MusicData.InitMusicData();

            audioWriteThread = new AudioWriteThread();
            audioWriteThread.start();
        }

        return 0;
    }
}
