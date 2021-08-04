package com.example.nettytest.pub;

public class CallParams {
    public boolean normalCallToBed;
    public boolean normalCallToRoom;
    public boolean normalCallToTV;
    public boolean normalCallToCorridor;

    public boolean emerCallToBed;
    public boolean emerCallToRoom;
    public boolean emerCallToTV;
    public boolean emerCallToCorridor;

    public boolean boardCallToBed;
    public boolean boardCallToRoom;
    public boolean boardCallToTV;
    public boolean boardCallToCorridor;

    public CallParams() {
        normalCallToBed = false;
        normalCallToRoom= true;
        normalCallToTV = true;
        normalCallToCorridor = true;

        emerCallToBed = false;
        emerCallToRoom= true;
        emerCallToTV = true;
        emerCallToCorridor = true;

        boardCallToBed = true;
        boardCallToRoom = true;
        boardCallToTV = false;
        boardCallToCorridor = true;
    }
}
