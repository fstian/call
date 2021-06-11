package com.usecomcalllib.androidPort.audio;

import com.example.nettytest.pub.LogWork;

public class AudioMgr {
    static AudioDevice audio = null;

    public static String OpenAudio(String devId,int src,int dst,String address,int sample,int ptime,int codec,int mode){
        synchronized (AudioMgr.class) {
            if (audio == null) {
                audio = new AudioDevice(devId,src, dst, address, sample, ptime, codec, mode);
            } else {
                audio.AudioSwitch(devId,src, dst, address, sample, ptime, codec, mode);
            }
            return audio.id;
        }
    }

    public static String GetAudioOwnwer(){
        String owner = "";
        synchronized (AudioMgr.class) {
            if (audio != null) {
                owner = audio.devId;
            }
        }

        return owner;
    }

    public static void CloseAudio(String id){
        synchronized (AudioMgr.class) {
            if (audio != null) {
                if(audio.AudioStop(id)) {
                    audio = null;
                }
            }
        }
    }

    public static void SuspendAudio(String devId,String id){
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Dev %s Try to suspend Audio %s ",devId,id);
        synchronized (AudioMgr.class) {
            if (audio != null) {
                if(audio.AudioSuspend(id)) {
                }
            }
        }
    }

    public static void ResumeAudio(String devId,String id){
        LogWork.Print(LogWork.TERMINAL_AUDIO_MODULE,LogWork.LOG_DEBUG,"Dev %s Try to resume Audio %s ",devId,id);
        synchronized (AudioMgr.class) {
            if (audio != null) {
                if(audio.AudioResume(id)) {
                }
            }
        }
    }
}
