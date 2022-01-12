package com.mysqltest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SqlParamItem {
    public String desc;
    public String name;
    public int value;

    public SqlParamItem(){
        desc="";
        name="";
        value=0;
    }

    static String ReadConfigFile(String path,String fileName){
        String fileContext = "";
        File configFile = new File(path, fileName);
        try {
            if (configFile.exists()) {
                FileInputStream finput = new FileInputStream(configFile);
                int len = finput.available();
                byte[] data = new byte[len];
                int readlen = finput.read(data);
                finput.close();
                if(readlen>0) {
                    fileContext = new String(data, "UTF-8");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileContext;

    }

}
