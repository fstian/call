package com.example.nettytest.backend.backendphone;

public class BackEndConfig {
    public boolean normalCallToBed=false;
    public boolean normalCallToRoom=true;
    public boolean normalCallToTv=false;
    public boolean normalCallToCorridor=true;

    public boolean emerCallToBed=false;
    public boolean emerCallToRoom=true;
    public boolean emerCallToTv=false;
    public boolean emerCallToCorridor=true;

    public void Copy(BackEndConfig config){
        normalCallToBed = config.normalCallToBed;
        normalCallToRoom = config.normalCallToRoom;
        normalCallToTv = config.normalCallToTv;
        normalCallToCorridor = config.normalCallToCorridor;

        emerCallToBed = config.emerCallToBed;
        emerCallToRoom = config.emerCallToRoom;
        emerCallToTv = config.emerCallToTv;
        emerCallToCorridor = config.emerCallToCorridor;
    }
}
