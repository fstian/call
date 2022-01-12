package com.nettime;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class TimeSyn {
    static private boolean isStart = false;

    static private long synTime = 0;
    static private boolean synHasSucc = false;

    static private NtpClient synThread = null;

    static private DatagramSocket udpServerSocket = null;

    static private int interval=30;
    static private String server="";

    static private int synTick = 0;

    static private boolean enableLog = true;

    final static int NTP_PORT = 123;
    final static int RTP_PACK_LEN = 48;

    final static long LI = (0<<30);
    final static long VN = (3<<27);
    final static long MODE = (3<<24);
    final static long STRATUM = (0<<16);
    final static long POLL = (4<<8);
    final static long PREC = 0xfa;  // -6

    final static int NTP_PACK_LEN = 48;

    final static long ROOT_DELAY = 0x10000;
    final static long ROOT_DISP = 0x10000;

    final static long JAN_1970 = (0x83aa7e80L);


    static private class NtpClient extends Thread {
        public NtpClient(){
            super("TimeSyn");
        }

        @Override
        public void run() {
            byte[] recvBuf=new byte[100];
            DatagramPacket recvPack;

            try {
                udpServerSocket = new DatagramSocket();
                InetAddress addr = InetAddress.getByName(server);
                byte[] data = MakeSynReq();
                DatagramPacket packet = new DatagramPacket(data, data.length, addr, NTP_PORT);
                udpServerSocket.send(packet);
                if(enableLog)
                    System.out.println("qkq add TimeSyn Send syn Req");

                while(!isInterrupted()){
                    java.util.Arrays.fill(recvBuf,(byte)0);
                    recvPack = new DatagramPacket(recvBuf, recvBuf.length);
                    udpServerSocket.receive(recvPack);
                    if(recvPack.getLength()==NTP_PACK_LEN) {
                        UpdateSynTime(recvBuf);
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static private void UpdateSynTime(byte[] data){
        synHasSucc = true;
        synTime = (GetValue(data,32,4)-JAN_1970)*1000;

        if(enableLog){
            System.out.println(String.format("qkq add TimeSyn System is %d",System.currentTimeMillis()));
            System.out.println(String.format("qkq add TimeSyn Get Syn Value %d",synTime));
            Date date = new Date(synTime);
            System.out.println("qkq add TimeSyn SynTime Date is " + date.toString());
        }

    }

    static private byte[] MakeSynReq(){
        byte[] reqData = new byte[RTP_PACK_LEN];
        long value;
        java.util.Arrays.fill(reqData,(byte)0);

        value = LI|VN|MODE|STRATUM|POLL|PREC;

        PutValue(value,reqData,0,4);

        PutValue(ROOT_DELAY,reqData,4,4);

        PutValue(ROOT_DISP,reqData,8,4);

        if(synHasSucc)
            value = synTime/1000L+JAN_1970;
        else {
            value = System.currentTimeMillis() / 1000+JAN_1970;
        }

        PutValue(value,reqData,40,4);

        return reqData;
    }

    static private void PutValue(long value,byte[] data, int offset,int len){
        long byteValue;
        long mask = 0xff;
        int iTmp;

        for(iTmp=0;iTmp<len;iTmp++){
            byteValue = value&(mask<<(iTmp*8));
            byteValue = byteValue>>(iTmp*8);
            data[offset+len-iTmp-1] = (byte)byteValue;
        }
    }

    static private long GetValue(byte[] data, int offset, int len){
        long value = 0;
        int iTmp;
        long byteValue;

        for(iTmp=0;iTmp<len;iTmp++){
            value = value<<8;
            if(data[offset+iTmp]>=0)
                byteValue = (long)data[offset+iTmp];
            else
                byteValue = (long)data[offset+iTmp]+0x100;
            value = value|byteValue;

        }

        return value;
    }

    public static long GetSynTime(){
        long value;

        if(synHasSucc){
            value = synTime;
        }else{
            value = System.currentTimeMillis();
        }
        return value;
    }

    public static void SetServerAddress(String serverAddress){
        server = serverAddress;
    }

    public static void SetSynInterval(int val){
        interval = val;
    }

    public static void SetLogStatus(boolean status){
        enableLog = status;
    }

    public static void BuildTimeSyn(String serverAddress,int intervalTime){

        server = serverAddress;
        interval = intervalTime;

        if(!isStart){
            isStart = true;
            synThread = new NtpClient();
            synThread.start();

            new Timer("NtpTimer").schedule(new TimerTask() {
                @Override
                public void run() {
                    synTick++;
                    if(synHasSucc){
                        synTime+=1000;
                    }

                    Date date = new Date(GetSynTime());

                    if(synTick<interval){
                        return;
                    }
                    synTick = 0;
                    if(udpServerSocket!=null) {
                        try {
                            InetAddress addr = InetAddress.getByName(server);
                            byte[] data = MakeSynReq();
                            DatagramPacket packet = new DatagramPacket(data, data.length, addr, NTP_PORT);
                            udpServerSocket.send(packet);
                            if(enableLog)
                                System.out.println("qkq add TimeSyn Send syn Req");
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            },0,1000);

        }
    }
}
