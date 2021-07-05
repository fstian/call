package com.androidport.port.audio;

import com.example.nettytest.pub.AudioMode;
import com.greenu.ptttalk.g729main;

public class Rtp {

    static final int RTP_HEADER_LEN = 12;
    static long rtpSessionId = 0x80000;
    static int rtpSeq = 0;
    static g729main g729;

    static {
        System.loadLibrary("G729Test");
        g729 = new g729main();
        g729.G729Init(0);
    }

    static void ResetRtp(){
        rtpSeq = 0;
        rtpSessionId++;
        if(rtpSessionId>=0x80000000)
            rtpSessionId = 0x80000;
    }

    static private void MakeRtpHeader(byte[] rtpHeader,int codec,int srcLen){
        long timeStamp;
        rtpHeader[0] = (byte)0x80;
        rtpHeader[1] = (byte)codec;
        rtpHeader[2] = (byte)((rtpSeq&0xff00)>>8);
        rtpHeader[3] = (byte)(rtpSeq&0xff);

        timeStamp = rtpSeq*srcLen;
        rtpHeader[4] = (byte)((timeStamp&0xff000000)>>24);
        rtpHeader[5] = (byte)((timeStamp&0xff0000)>>16);
        rtpHeader[6] = (byte)((timeStamp&0xff00)>>8);
        rtpHeader[7] = (byte)(timeStamp&0xff);

        rtpHeader[8] = (byte)((rtpSessionId&0xff000000)>>24);
        rtpHeader[9] = (byte)((rtpSessionId&0xff0000)>>16);
        rtpHeader[10] = (byte)((rtpSessionId&0xff00)>>8);
        rtpHeader[11] = (byte)(rtpSessionId&0xff);
    }

    public static byte[] EncloureRtp(short[] src,int codec){
        byte[] rtpData=null;
        int iTmp;
        int srcLen;

        srcLen = src.length;

        if(codec== AudioMode.RTP_CODEC_729) {
            byte[] g729Data = new byte[srcLen];
            int encodeSize = g729.LinearToG729(src,src.length,g729Data);
            rtpData = new byte[encodeSize+RTP_HEADER_LEN];
            MakeRtpHeader(rtpData,codec,srcLen);
            for(iTmp=0;iTmp<encodeSize;iTmp++) {
                rtpData[iTmp + RTP_HEADER_LEN] = g729Data[iTmp];
            }
        }else if(codec== AudioMode.RTP_CODEC_711A){
            rtpData = new byte[srcLen+RTP_HEADER_LEN];
            MakeRtpHeader(rtpData,codec,srcLen);
            for(iTmp=0;iTmp<srcLen;iTmp++){
                rtpData[iTmp+RTP_HEADER_LEN] = G711.linear2alaw(src[iTmp]);
            }
        }else if(codec==AudioMode.RTP_CODEC_711MU){
            rtpData = new byte[srcLen+RTP_HEADER_LEN];
            MakeRtpHeader(rtpData,codec,srcLen);
            for(iTmp=0;iTmp<srcLen;iTmp++){
                rtpData[iTmp+RTP_HEADER_LEN] = G711.linear2ulaw(src[iTmp]);
            }
        }
        rtpSeq++;

        return rtpData;
    }

    public static short[] UnenclosureRtp(byte[] data, int srcLen,int codec){
        short[] pcm=null;
        int iTmp;
        int offset=0;

        if(codec==AudioMode.RTP_CODEC_729){
            byte[] g729Data = new byte[srcLen];
            for(iTmp=0;iTmp<srcLen;iTmp++){
                g729Data[iTmp] = data[iTmp];
            }
            pcm = new short[srcLen*8];
            g729.G729ToLinear(g729Data,srcLen,pcm);
        }else if(codec==AudioMode.RTP_CODEC_711MU) {
            pcm = new short[srcLen];
            for (iTmp = 0; iTmp < srcLen; iTmp++) {
                pcm[iTmp] = G711.ulaw2linear(data[iTmp+offset]);
            }
        }else if(codec==AudioMode.RTP_CODEC_711A){
            pcm = new short[srcLen];
            for (iTmp = 0; iTmp < srcLen; iTmp++) {
                pcm[iTmp] = G711.alaw2linear(data[iTmp+offset]);
            }
        }

        return pcm;
    }
}
