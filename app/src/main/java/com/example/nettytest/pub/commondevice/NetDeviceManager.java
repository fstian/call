package com.example.nettytest.pub.commondevice;

import com.example.nettytest.pub.LogWork;

import java.util.HashMap;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class NetDeviceManager {

        protected final HashMap<String, NetDevice> devLists;

        public NetDeviceManager(){
                devLists = new HashMap<>();
        }


        public void DevSendBuf(String id, ByteBuf buf){
                NetDevice matchedDev;
                synchronized (devLists) {
                        matchedDev = devLists.get(id);
                        if(matchedDev!=null){
                                matchedDev.SendBuffer(buf);
                        }
                }
        }

        public void UpdateDevChannel(String id, Channel ch){
                NetDevice matchedDev;
                synchronized (devLists) {
                        matchedDev = devLists.get(id);
                        if (matchedDev != null) {
                                matchedDev.UpdateChannel(ch);
                        }
                }
        }
}
