package com.example.nettytest.userinterface;

public class UserArea {
    public String areaId;
    public String areaName;

    public UserArea(String id,String name) {
        if(id==null)
            areaId = "";
        else
            areaId = id;
        if(name==null)
            areaName="";
        else
            areaName = name;
    }
}
