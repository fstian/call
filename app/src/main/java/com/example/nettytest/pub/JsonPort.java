package com.example.nettytest.pub;

import com.alibaba.fastjson.JSONObject;

public class JsonPort {
	
	public static String GetJsonString(JSONObject jsonObj,String name) {
		String value;
		value = jsonObj.getString(name);
		if(value==null)
			value = "";
		return value;
	}

}
