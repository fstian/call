package com.example.nettytest.userinterface;

import java.util.ArrayList;

public class UserConfigMessage extends UserMessage {

    public ArrayList<UserConfig> paramList;

    public UserConfigMessage(){
        super();
        type = UserMessage.CONFIG_MESSAGE_LIST;
        paramList = new ArrayList<>();
    }
}
