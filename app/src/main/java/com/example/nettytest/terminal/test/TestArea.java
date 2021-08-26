package com.example.nettytest.terminal.test;

import java.util.ArrayList;

public class TestArea {
    public ArrayList<TestDevice> devList;
    public String areaId;
    public String waitTransferAreaId;

    public TestArea(String id){
        areaId = id;
        devList = new ArrayList<>();
        waitTransferAreaId = "";
    }

    public int AddTestDevice(TestDevice dev){
        devList.add(dev);
        return 0;
    }

    public TestDevice GetDevice(int index){
        TestDevice dev = null;

        if(index<devList.size()){
            dev = devList.get(index);
        }
        return dev;
    }
}
