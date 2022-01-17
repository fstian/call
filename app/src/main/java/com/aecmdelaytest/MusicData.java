package com.aecmdelaytest;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public class MusicData {

    static int InitData1(){
        int iTmp;
        int tmpValue;
        if(musicSrc==null) {
            try {
                File rawFile = new File(Environment.getExternalStorageDirectory().getPath()+"/Out.raw");
                FileInputStream finput = new FileInputStream(rawFile);
                int len = finput.available() ;
                byte[] data = new byte[len];
                finput.read(data);
                musicDataLen = len/2-1;
                musicSrc = new short[musicDataLen];
                recordData = new short[musicDataLen];
                for(iTmp=0;iTmp<musicDataLen;iTmp++){

                    if(data[2*iTmp+1]<0)
                        tmpValue = 0x100+data[2*iTmp+1];
                    else
                        tmpValue = data[2*iTmp+1];
                    tmpValue = tmpValue<<8;

                    if(data[2*iTmp]<0)
                        tmpValue = tmpValue+0x100+data[2*iTmp];
                    else
                        tmpValue = tmpValue+data[2*iTmp];

                    if(tmpValue==256)
                        tmpValue = 0;

                    if(tmpValue>0x8000)
                        tmpValue = tmpValue-0x10000;
                    musicSrc[iTmp] = (short)tmpValue;
                }
                finput.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return musicDataLen;
    }


    public static int InitMusicData() {
        maxPutPos = 0;
        return InitData1();
    }

    public static int PutRecordData(short[] data,int offset,int size){
        if(offset+size<musicDataLen){
            System.arraycopy(data,0,recordData,offset,size);
            if(offset+size>maxPutPos)
                maxPutPos = offset+size;
        }
        return 0;
    }

    public static int SaveRecordData(){
        int iTmp;
        int value;
        int byteValue;
        if(recordData!=null){
            try {
                File rawFile = new File(Environment.getExternalStorageDirectory().getPath()+"/Record.raw");
                FileOutputStream foutput = new FileOutputStream(rawFile);
                byte[] rawData = new byte[musicDataLen*2];
                for(iTmp=0;iTmp<musicDataLen;iTmp++){
                    if(recordData[iTmp]<0)
                        value = 0x10000+recordData[iTmp];
                    else
                        value = recordData[iTmp];

                    byteValue = value&0xff;
                    if(byteValue>=0x80)
                        byteValue = byteValue-0x100;
                    rawData[2*iTmp] = (byte)byteValue;

                    byteValue = (value&0xff00)>>8;
                    if(byteValue>=0x80)
                        byteValue = byteValue-0x100;
                    rawData[2*iTmp+1] = (byte)byteValue;

                }
                foutput.write(rawData);
                foutput.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return 0;
    }

    public static short[] GetMusicData(int offset, int size) {

        if(musicSrc==null)
            return null;

        if (offset + size < musicSrc.length) {
            short[] data = new short[size];
            System.arraycopy(musicSrc, offset, data, 0, size);
            return data;
        } else {
            return null;
        }
    }

    static short musicSrc[] = null;
    static short recordData[] = null;
    static int musicDataLen = 0;
    static int maxPutPos = 0;

}
