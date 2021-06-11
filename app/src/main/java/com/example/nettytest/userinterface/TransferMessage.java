package com.example.nettytest.userinterface;

public class TransferMessage extends UserMessage{
    
    public String transferAreaId;
    public boolean state;

    public TransferMessage(){
        super();
        transferAreaId = "";
        state  = false;
    }

}
