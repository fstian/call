package com.usecomcalllib.androidTest;

import com.usecomcalllib.androidTest.TestDevice;

import java.util.ArrayList;

public class TestArea {
    public ArrayList<TestDevice> devList;
    public String areaId;

    public TestArea(String id){
        areaId = id;
        devList = new ArrayList<>();
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
