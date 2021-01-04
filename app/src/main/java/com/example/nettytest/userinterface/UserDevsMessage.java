package com.example.nettytest.userinterface;

import java.util.ArrayList;

public class UserDevsMessage extends UserMessage {
    public ArrayList<UserDevice> deviceList;

    public UserDevsMessage(){
        super();
        deviceList = new ArrayList<>();
    }
}
