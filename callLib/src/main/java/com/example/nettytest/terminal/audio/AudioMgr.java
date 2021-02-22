package com.example.nettytest.terminal.audio;

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
}
