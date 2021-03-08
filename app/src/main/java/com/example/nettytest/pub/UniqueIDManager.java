package com.example.nettytest.pub;

public class UniqueIDManager {
    static int seq = 0;
    public static final int MSG_UNIQUE_ID = 1;
    public static final int CALL_UNIQUE_ID = 2;
    public static final int AUDIO_UNIQUE_ID = 3;

    public static String GetUniqueID(String devID, int type){
        String msgID="";
        synchronized (UniqueIDManager.class){
            if(type == MSG_UNIQUE_ID)
                msgID = String.format("M-%s-%X-%08X",devID,System.currentTimeMillis(),seq++);
            else if(type==CALL_UNIQUE_ID)
                msgID = String.format("C-%s-%X-%08X",devID,System.currentTimeMillis(),seq++);
            else if(type==AUDIO_UNIQUE_ID)
                msgID = String.format("A-%s-%X-%08X",devID,System.currentTimeMillis(),seq++);
        }
        return msgID;
    }
}
