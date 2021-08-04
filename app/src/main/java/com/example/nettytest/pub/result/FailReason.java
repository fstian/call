package com.example.nettytest.pub.result;

public class FailReason {
    public final static int FAIL_REASON_NO = 0;
    public final static int FAIL_REASON_BUSY = 1;
    public final static int FAIL_REASON_NOTSUPPORT = 2;
    public final static int FAIL_REASON_NOTFOUND = 3;
    public final static int FAIL_REASON_TIMEOVER = 4;
    public final static int FAIL_REASON_CONFLICT = 5;
    public final static int FAIL_REASON_FORBID = 6;
    public final static int FAIL_REASON_HASEXIST = 7;
    public final static int FAIL_REASON_UNKNOW = 100;

   static public String GetFailName(int reason){
        String failName;

        switch(reason){
            case FAIL_REASON_NO:
                failName = "No_Error";
                break;
            case FAIL_REASON_BUSY:
                failName = "Busy";
                break;
            case FAIL_REASON_NOTSUPPORT:
                failName = "Not_Support";
                break;
            case FAIL_REASON_NOTFOUND:
                failName = "Not_Found";
                break;
            case FAIL_REASON_TIMEOVER:
                failName = "Time_Over";
                break;
            case FAIL_REASON_CONFLICT:
                failName = "Conflict";
                break;
            case FAIL_REASON_FORBID:
                failName = "Forbid";
                break;
            default:
                failName = "Unknow_Error";
                break;
        }

        return failName;
   }

}
