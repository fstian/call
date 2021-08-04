package com.example.nettytest.userinterface;

public class TestInfo {
    public boolean isAutoTest;
    public boolean isRealTimeFlash;
    public int timeUnit;
    public int testMode;

    public TestInfo(){
        isAutoTest = false;
        isRealTimeFlash = false;
        timeUnit = 10;
        testMode = 0;
    }
}
