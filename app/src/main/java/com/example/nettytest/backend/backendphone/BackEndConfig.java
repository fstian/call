package com.example.nettytest.backend.backendphone;

public class BackEndConfig {
    public boolean normalCallToBed=false;
    public boolean normalCallToRoom=true;
    public boolean normalCallToTv=true;
    public boolean normalCallToCorridor=true;

    public boolean emerCallToBed=false;
    public boolean emerCallToRoom=true;
    public boolean emerCallToTv=true;
    public boolean emerCallToCorridor=true;

    public boolean broadCallToBed=true;
    public boolean broadCallToRoom=true;
    public boolean broadCallToTv=false;
    public boolean broadCallToCorridor=true;

    public void Copy(BackEndConfig config){
        normalCallToBed = config.normalCallToBed;
        normalCallToRoom = config.normalCallToRoom;
        normalCallToTv = config.normalCallToTv;
        normalCallToCorridor = config.normalCallToCorridor;

        emerCallToBed = config.emerCallToBed;
        emerCallToRoom = config.emerCallToRoom;
        emerCallToTv = config.emerCallToTv;
        emerCallToCorridor = config.emerCallToCorridor;

        broadCallToBed = config.broadCallToBed;
        broadCallToRoom = config.broadCallToRoom;
        broadCallToCorridor = config.broadCallToCorridor;
        broadCallToTv = config.broadCallToTv;
    }
}
