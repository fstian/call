package com.example.nettytest.pub;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogWork {

    public final static int TERMINAL_PHONE_MODULE = 1;
    public final static int TERMINAL_DEVICE_MODULE = 2;
    public final static int TERMINAL_CALL_MODULE = 3;
    public final static int TERMINAL_NET_MODULE = 4;
    public final static int TERMINAL_USER_MODULE = 5;
    public final static int TERMINAL_AUDIO_MODULE = 6;

    public final static int BACKEND_PHONE_MODULE = 101;
    public final static int BACKEND_DEVICE_MODULE = 102;
    public final static int BACKEND_CALL_MODULE = 103;
    public final static int BACKEND_NET_MODULE = 104;

    public final static int TRANSACTION_MODULE = 201;

    public final static int DEBUG_MODULE = 301;

    public static boolean terminalPhoneModuleLogEnable = false;
    public static boolean terminalDeviceModuleLogEnable = false;
    public static boolean terminalNetModuleLogEnable = false;
    public static boolean terminalCallModuleLogEnable = false;
    public static boolean terminalUserModuleLogEnable = false;
    public static boolean terminalAudioModuleLogEnable = false;

    public static boolean backEndPhoneModuleLogEnable = false;
    public static boolean backEndDeviceModuleLogEnable = false;
    public static boolean backEndCallModuleLogEnable = false;
    public static boolean backEndNetModuleLogEnable = false;

    public static boolean transactionModuleLogEnable = false;
    public static boolean debugModuleLogEnable = false;

    public final static int LOG_VERBOSE = 1;    // for verbose
    public final static int LOG_DEBUG = 2;      // for debug
    public final static int LOG_INFO = 3;       // for important
    public final static int LOG_WARN = 4;       // for reasonable error
    public final static int LOG_ERROR = 5;      // for unreasonable error
    public final static int LOG_FATAL = 6;      // for fatal
    public final static int LOG_TEMP_DBG = 7;      // for fatal

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public final static String LOG_DEVICE = "50110001";

    public static int dbgLevel = LOG_ERROR;

    public static boolean bLogToFiles = false;
    private static long begineLogTime = System.currentTimeMillis();
    public static long logInterval = 1; //hour
    private static int logIndex = 1;
    private static File logWriteFile=null;

    public static int Print(int module,int degLevel,String buf){
        return Print(module,degLevel,buf,"");
    }

    private static class Log {
    	public static void v(String tag,String dbg) {
    		System.out.println(tag+" V/ "+dbg);
    	}
    	public static void d(String tag,String dbg) {
    		System.out.println(tag+" D/ "+dbg);
    	}
    	public static void i(String tag,String dbg) {
    		System.out.println(tag+" I/ "+dbg);
    	}
    	public static void w(String tag,String dbg) {
    		System.out.println(tag+" W/ "+dbg);
    	}
    	public static void e(String tag,String dbg) {
    		System.out.println(tag+" E/ "+dbg);
    	}
    }


    public static int Print(int module,int degLevel,String format,Object...param){
        boolean isPrint = false;
        String tag = "  ";
        long curTime;
        Date date;
        
        if (degLevel >= dbgLevel||degLevel==LOG_TEMP_DBG) {
            switch (module) {
                case TERMINAL_PHONE_MODULE:
                    isPrint = terminalPhoneModuleLogEnable;
                    tag = "HT500_TERMINAL_PHONE ";
                    break;
                case TERMINAL_DEVICE_MODULE:
                    isPrint = terminalDeviceModuleLogEnable;
                    tag = "HT500_TERMINAL_DEVICE";
                    break;
                case TERMINAL_CALL_MODULE:
                    isPrint = terminalCallModuleLogEnable;
                    tag = "HT500_TERMINAL_CALL  ";
                    break;
                case TERMINAL_NET_MODULE:
                    isPrint = terminalNetModuleLogEnable;
                    tag = "HT500_TERMINAL_NET   ";
                    break;
                case TERMINAL_USER_MODULE:
                    isPrint = terminalUserModuleLogEnable;
                    tag = "HT500_TERMINAL_USER  ";
                    break;
                case TERMINAL_AUDIO_MODULE:
                    isPrint = terminalAudioModuleLogEnable;
                    tag = "HT500_TERMINAL_AUDIO ";
                    break;
                case BACKEND_PHONE_MODULE:
                    isPrint = backEndPhoneModuleLogEnable;
                    tag = "HT500_BACKEND_PHONE  ";
                    break;
                case BACKEND_DEVICE_MODULE:
                    isPrint = backEndDeviceModuleLogEnable;
                    tag = "HT500_BACKEND_DEVICE ";
                    break;
                case BACKEND_CALL_MODULE:
                    isPrint = backEndCallModuleLogEnable;
                    tag = "HT500_BACKEND_CALL   ";
                    break;
                case BACKEND_NET_MODULE:
                    isPrint = backEndNetModuleLogEnable;
                    tag = "HT500_BACKEND_NET    ";
                    break;
                case TRANSACTION_MODULE:
                    isPrint = transactionModuleLogEnable;
                    tag = "HT500_TRANSACTIO     ";
                    break;
                case DEBUG_MODULE:
                    isPrint = debugModuleLogEnable;
                    tag = "HT500_DEBUG          ";
                    break;
            }

            if(degLevel==LOG_TEMP_DBG){
                isPrint = true;
            }
            if (isPrint) {
                curTime = System.currentTimeMillis();
                date = new Date(curTime);
                String dbgString = String.format(format, param);
//                if(dbgString.indexOf(LOG_DEVICE)<=0)
//                    return 0;
                String levelString = " D/ ";
                switch (degLevel) {
                    case LOG_VERBOSE:
                        Log.v(tag, dbgString);
                        levelString = " V/ ";
                        break;
                    case LOG_DEBUG:
                        Log.d(tag, dbgString);
                        levelString = " D/ ";
                        break;
                    case LOG_INFO:
                        Log.i(tag,dbgString);
                        levelString = " I/ ";
                        break;
                    case LOG_WARN:
                        Log.w(tag,dbgString);
                        levelString = " W/ ";
                        break;
                    case LOG_ERROR:
                        Log.e(tag,dbgString);
                        levelString = " E/ ";
                        break;
                    case LOG_FATAL:
                        Log.e(tag,dbgString);
                        levelString = " F/ ";
                        break;
                    case LOG_TEMP_DBG:
                        Log.e(tag,dbgString);
                        levelString = " T/ ";
                        break;
                }

                if(bLogToFiles){
                    synchronized (LogWork.class) {
                        if (logWriteFile == null) {
                            logWriteFile = new File(GetLogFileName(logIndex));
                        } else if (curTime > begineLogTime + logInterval * 3600 * 1000) {
                            begineLogTime = curTime;
                            if (logIndex > 100)
                                return 0;
                            logIndex++;
                            logWriteFile = new File(GetLogFileName(logIndex));
                        }
                        String writeString = dateFormat.format(date) + levelString + tag + ":  " + dbgString + "\r\n";
                        BufferedWriter bw = null;
                        try {
                            bw = new BufferedWriter(new FileWriter(logWriteFile, true));
                            bw.write(writeString);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (bw != null) {
                                    bw.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return 0;
    }

    public static String GetLogFileName(int index){
        int osType;
        String fileName;
        
        osType = HandlerMgr.GetOSType();
        if(osType==HandlerMgr.WINDOWS_OS||osType==HandlerMgr.LINUX_OS)
            fileName = String.format("./CallModuleLog%04d.txt",index);
        else
            fileName = String.format("/sdcard/CallModuleLog%04d.txt",index);
        return fileName;
    }

    public static void ResetLogIndex(){
        synchronized (LogWork.class) {
            String logFileName;
            int iTmp = 1;
            File logFile;
            while (iTmp<=100) {
                logFileName = LogWork.GetLogFileName(iTmp);
                logFile = new File(logFileName);
                if (logFile.exists() && logFile.isFile()) {
                    logFile.delete();
                }
                iTmp++;
            }

            logIndex = 1;
            begineLogTime = System.currentTimeMillis();
            logWriteFile = null;
        }
    }

}
