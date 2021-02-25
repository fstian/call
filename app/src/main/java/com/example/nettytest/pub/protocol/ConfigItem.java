package com.example.nettytest.pub.protocol;

public class ConfigItem {
    public String param_id;
    public String param_name;
    public String param_value;
    public String param_unit;

    public ConfigItem(){
        param_id = "";
        param_name = "";
        param_value = "";
        param_unit = "";
    }

    public void Copy(ConfigItem src){
        param_id = src.param_id;
        param_name = src.param_name;
        param_value = src.param_value;
        param_unit = src.param_unit;
    }
}
