package com.mysqltest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SqlBedInfo {

    private final String AREAS_NAME = "areas";
    private final String AREA_NAME = "area";
    private final String BEDS_NAME = "beds";
    private final String NAME_NAME = "name";

    private HashMap<String, ArrayList<String>> beds;

    public SqlBedInfo(){
        beds=new HashMap<>();
    }

    public ArrayList<String> GetBedList(String areaCode){
        return beds.get(areaCode);
    }

    private int ApplyBedConfig(String conf){
        JSONObject json;
        JSONArray areasJson;
        JSONObject areaJson;
        String areaCode;
        JSONArray bedsJson;
        JSONObject bedJson;
        ArrayList<String> bedList;

        int iTmp,jTmp;

        json = JSONObject.parseObject(conf);
        if(json == null)
            return -1;
        areasJson = json.getJSONArray(AREAS_NAME);
        if(areasJson==null)
            return -1;

        for(iTmp=0;iTmp<areasJson.size();iTmp++){
            areaJson = areasJson.getJSONObject(iTmp);
            areaCode = areaJson.getString(AREA_NAME);
            bedsJson = areaJson.getJSONArray(BEDS_NAME);
            bedList = new ArrayList<String>();
            for(jTmp=0;jTmp<bedsJson.size();jTmp++) {
                bedJson = bedsJson.getJSONObject(jTmp);
                bedList.add(bedJson.getString(NAME_NAME));
            }
            beds.put(areaCode,bedList);
        }

        return 0;
    }

    public int InitBeds(String path,String fileName) {
        String fileContext = SqlParamItem.ReadConfigFile(path,fileName);
        ApplyBedConfig(fileContext);
        return 0;
    }



}
