package com.example.nettytest.pub.transaction;

import com.example.nettytest.pub.protocol.ProtocolPacket;

public class Transaction {
    public final static int TRANSCATION_STATE_REQUIRING = 1;
    public final static int TRANSCATION_STATE_RESPONDING = 2;
    public final static int TRANSCATION_STATE_WAITRELEASE = 3;
    public final static int TRANSCATION_STATE_FINISHED = 4;
    public final static int TRANSCATION_STATE_INITIALZE = 0;

    public final static int TRANSCATION_REQUIRING_TIME = 20;
    public final static int TRANSCATION_RESPONDING_TIME =30;
    public final static int TRANSCATION_RESEND_INTERVAL = 5;

    public final static int TRANSCATION_DIRECTION_C2S = 1;
    public final static int TRANSCATION_DIRECTION_S2C = 2;

    public int liveTime;

    public int state;

    protected String devID;
    protected int direction;

    protected ProtocolPacket reqPacket;
    protected ProtocolPacket resPacket;

    public  Transaction(String devID,ProtocolPacket req,int direction){
        liveTime = 0;
        state = TRANSCATION_STATE_REQUIRING;
        reqPacket = req;
        resPacket = null;
        this.devID = devID;
        this.direction = direction;
    }

    public Transaction(String devID,ProtocolPacket req,ProtocolPacket res,int direction){
        liveTime = 0;
        state = TRANSCATION_STATE_RESPONDING;
        reqPacket = req;
        resPacket = res;
        this.devID = devID;
        this.direction = direction;
    }

    public static String GetStateName(int state){
        String stateName = "";

        switch(state){
            case TRANSCATION_STATE_REQUIRING:
                stateName = "REQUIRING";
                break;
            case TRANSCATION_STATE_RESPONDING:
                stateName = "RESPONDING";
                break;
            case TRANSCATION_STATE_WAITRELEASE:
                stateName = "WAITRELEASE";
                break;
            case TRANSCATION_STATE_FINISHED:
                stateName = "FINISHED";
                break;
            case TRANSCATION_STATE_INITIALZE:
                stateName = "INITIALIZE";
                break;
        }
        return stateName;
    }
}
