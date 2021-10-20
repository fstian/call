package com.mysqltest;

import com.alibaba.fastjson.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SqlAreasConf {

    private final String AREAS_NAME = "areas";
    private final String FLOOR_NAME = "floor";
    private final String LOCATION_NAME = "location";
    private final String AREANAME_NAME = "name";
    private final String AREACODE_NAME = "code";
    private final String NETWORK_NAME = "network";
    private final String BEDNUM_NAME = "bedNum";
    private final String DOORNUM_NAME = "doorNum";

    private final int AREA_SQL_TYPE = 1;
    private final String SQL_AREAS_FILE = "infusion_areainfo.sql";
    private final int ROOM_SQL_TYPE = 2;
    private final String SQL_ROOMS_FILE = "infusion_roominfo.sql";
    private final int BED_SQL_TYPE = 3;
    private final String SQL_BED_FILE = "infusion_bedinfo.sql";
    private final int DEV_SQL_TYPE = 4;
    private final String SQL_DEV_FILE = "infusion_devinfo.sql";
    private final int PATIENT_SQL_TYPE = 5;
    private final String SQL_PATIENT_FILE = "infusion_patientinfo.sql";

    ArrayList<SqlAreaInfo> areaLists;

    public SqlAreasConf(){
        areaLists = new ArrayList<>();
    }

    private int ApplyConfig(String config){
        JSONObject json;
        JSONArray areasJson;
        JSONObject areaJson;
        SqlAreaInfo sqlArea;

        int iTmp;

        json = JSONObject.parseObject(config);
        if(json == null)
            return -1;
        areasJson = json.getJSONArray(AREAS_NAME);
        if(areasJson==null)
            return -1;

        for(iTmp=0;iTmp<areasJson.size();iTmp++){
            areaJson = areasJson.getJSONObject(iTmp);
            sqlArea = new SqlAreaInfo();
            sqlArea.floor = areaJson.getIntValue(FLOOR_NAME);
            sqlArea.location = areaJson.getInteger(LOCATION_NAME);
            sqlArea.name = areaJson.getString(AREANAME_NAME);
            sqlArea.code = areaJson.getString(AREACODE_NAME);
            sqlArea.netWork = areaJson.getInteger(NETWORK_NAME);
            sqlArea.bedNum = areaJson.getInteger(BEDNUM_NAME);
            sqlArea.doorNum = areaJson.getInteger(DOORNUM_NAME);
            areaLists.add(sqlArea);
        }

        return 0;
    }

    private ArrayList<String> CreateAreasSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
//        String areaCode;
        int iTmp=1;

        cmd = "DELETE FROM `infusion_areainfo`;\n";
        cmds.add(cmd);

        for(SqlAreaInfo area:areaLists){
//            areaCode =  area.CreateAreaCode();
            cmd = String.format("INSERT INTO `infusion_areainfo`(`id`, `area_code`, `area_name`) VALUES " +
                            "(%d, \'%s\', \'%s\');\n",
                    iTmp,area.code,area.name);
            cmds.add(cmd);
            iTmp++;
        }
        return cmds;
    }

    private ArrayList<String> CreateRoomSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
//        String areaCode;
        int iTmp;
        int recordNum = 1;

        cmd = "DELETE FROM `infusion_roominfo`;\n";
        cmds.add(cmd);
        for(SqlAreaInfo area:areaLists) {
//            areaCode = area.CreateAreaCode();
            for(iTmp=0;iTmp<area.doorNum;iTmp++){
                cmd = String.format("INSERT INTO `infusion_roominfo`(`id`, `f_area_code`, `room_code`, `room_name`) VALUES " +
                                "(%d, \'%s\', \'%d\', \'%d\');\n",
                        recordNum,area.code,iTmp+1,iTmp+1);
                recordNum++;
                cmds.add(cmd);
            }
        }
        return cmds;
    }

    private ArrayList<String> CreateBedSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
//        String areaCode;
        int iTmp;
        int recordNum = 1;

        cmd = "DELETE FROM `infusion_bedinfo`;\n";
        cmds.add(cmd);
        for(SqlAreaInfo area:areaLists) {
//            areaCode = area.CreateAreaCode();
            for(iTmp=0;iTmp<area.bedNum;iTmp++){
                cmd = String.format("INSERT INTO `infusion_bedinfo`(`id`, `f_area_code`, `f_patient_id`, `bed_name`, `used`, `doctor`, `nurse`, `enable`, `f_room_code`) VALUES" +
                                " (%d, \'%s\', \'%s\', \'%d\', \'1\', \'\', \'\', \'1\',\'%s\');\n",
                        recordNum,area.code,area.CreateBedCode(iTmp),iTmp+1,area.CreateRoomOfBed(iTmp));
                recordNum++;
                cmds.add(cmd);
            }
        }
        return cmds;
    }

    private ArrayList<String> CreatePatientSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        int iTmp;
        int recordNum = 1;

        cmd = "DELETE FROM `infusion_patientinfo`;\n";
        cmds.add(cmd);
        for(SqlAreaInfo area:areaLists) {
            for(iTmp=0;iTmp<area.bedNum;iTmp++){
                cmd = String.format("INSERT INTO `infusion_patientinfo`(`id`, `patient_name`, `in_id`, `diagnosis_id`, `in_date`, `out_date`, `doctor_name`, `nurse_name`, `gender`, `age`, `in_diagnosis`, `out_diagnosis`, `phone`, `allergy`, `mitype`, `carelv`, `p_idx`, `meal`, `isolation`, `fall_tumble`, `drop_off`, `press_sore`, `text_care`, `text_meal`, `critical_ill`, `serious_ill`, `abo_group`, `ah_group`, `touch_isolation`, `prepare_in`, `know_sensitive`, `monitor_pain`, `monitor_drug`, `monitor_drop`, `monitor_fall`, `monitor_press`) VALUES " +
                                "(%d, \'%s\', \'%s\', \'%s\', \'2020-12-09 08:29:26\', \'2020-12-16 10:48:54\', \'\', \'\', \'女\', \'58岁\', \'\', \'\', \'\', \'\', \'省内异地居民医保\', \'\', \'%s\', \'\', \'\', 0, 0, 0, \'\', \'\', 0, 0, \'\', \'\', 0, \'\', 0, \'N\', \'N\', \'N\', \'N\', \'N\');\n",
                        recordNum,area.CreatePatientName(iTmp),area.CreatePatientCode(iTmp),area.CreatePatientCode(iTmp),area.CreatePatientCode(iTmp));
                recordNum++;
                cmds.add(cmd);
            }
        }
        return cmds;
    }

    private ArrayList<String> CreateDevSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
//        String areaCode;
        int iTmp;
        int recordNum = 1;

        cmd = "DELETE FROM `infusion_devinfo`;\n";
        cmds.add(cmd);
        for(SqlAreaInfo area:areaLists) {
//            areaCode = area.CreateAreaCode();
            for(iTmp=0;iTmp<area.bedNum;iTmp++){
                // bed_id is bed_name
                cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                                "(%d, 2, \'%s\', \'%s\', \'%s\', \'\', \'0-%s\', \'%s\', \'%s\', \'%s\', 0, 1, 1, 1, 0, 1, 0, 0, \'\');\n",
//                        recordNum,area.CreateBedDevCode(iTmp),area.CreateBedDevName(iTmp),area.code,area.GetDefaultRoute(),area.GetDefaultRoute(),""+(iTmp+1),area.GetDefaultRoute());
                        //remove bed info
                                "(%d, 2, \'%s\', \'%s\', \'%s\', \'\', \'0-%s\', \'%s\', \'\', \'%s\', 0, 1, 1, 1, 0, 1, 0, 0, \'\');\n",
                        recordNum,area.CreateBedDevCode(iTmp),area.CreateBedDevName(iTmp),area.code,area.GetDefaultRoute(),area.GetDefaultRoute(),area.GetDefaultRoute());
                recordNum++;
                cmds.add(cmd);


                //emergency
                if(area.location==2) {
                    if(area.floor>=5) {
                        if (iTmp % 3 == 0) {
                            cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                            "(%d, 4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                                    recordNum, area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(iTmp));
                            recordNum++;
                            cmds.add(cmd);
                        }
                    }
                }else if(area.location==1) {
                    if(area.floor>=6){
                        if(iTmp%3==0&&iTmp/3<area.doorNum){
                            cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                            "(%d, 4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                                    recordNum, area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(iTmp));
                            recordNum++;
                            cmds.add(cmd);
                        }
                    }else {
                        cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                        "(%d, 4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                                recordNum, area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(iTmp));
                        recordNum++;
                        cmds.add(cmd);
                    }
                }else if(area.location==3) {
                    cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                    "(%d, 4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                            recordNum, area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(iTmp));
                    recordNum++;
                    cmds.add(cmd);
                }
            }

            //door
            for(iTmp=0;iTmp<area.doorNum;iTmp++){
                cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                "(%d, 6, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%d\');\n",
                        recordNum,area.CreateDoorDevCode(iTmp),area.CreateDoorDevName(iTmp),area.code,iTmp+1);
                recordNum++;
                cmds.add(cmd);
            }
            // master
            cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                            "(%d, 0, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
                    recordNum,area.GetDefaultRoute(),area.GetDefaultRoute(),area.code);
            recordNum++;
            cmds.add(cmd);

            //tv
            cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                            "(%d, 8, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
                    recordNum,area.GetTVCode(),area.GetTVCode(),area.code);
            recordNum++;
            cmds.add(cmd);

            //corridor
            cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                            "(%d, 7, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
                    recordNum,area.GetCorridorCode(),area.GetCorridorCode(),area.code);
            recordNum++;
            cmds.add(cmd);

            //Nurser Phone
            cmd = String.format("INSERT INTO `infusion_devinfo`(`id`, `dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                            "(%d, 9, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
                    recordNum,area.GetPhoneCode(),area.GetPhoneCode(),area.code);
            recordNum++;
            cmds.add(cmd);
        }
        return cmds;

    }


    private int CreateSqlFile(String path,int type){
        String fileName = null;
        BufferedWriter bw=null;
        ArrayList<String> cmds=null;

        switch(type){
            case AREA_SQL_TYPE:
                fileName = SQL_AREAS_FILE;
                break;
            case ROOM_SQL_TYPE:
                fileName = SQL_ROOMS_FILE;
                break;
            case BED_SQL_TYPE:
                fileName = SQL_BED_FILE;
                break;
            case DEV_SQL_TYPE:
                fileName = SQL_DEV_FILE;
                break;
            case PATIENT_SQL_TYPE:
                fileName = SQL_PATIENT_FILE;
                break;
        }
        File sqlFile = new File(path,fileName);
        try {
            bw = new BufferedWriter(new FileWriter(sqlFile,false));
            switch(type) {
                case AREA_SQL_TYPE:
                    cmds = CreateAreasSqlFile();
                    break;
                case ROOM_SQL_TYPE:
                    cmds = CreateRoomSqlFile();
                    break;
                case BED_SQL_TYPE:
                    cmds = CreateBedSqlFile();
                    break;
                case DEV_SQL_TYPE:
                    cmds = CreateDevSqlFile();
                    break;
                case PATIENT_SQL_TYPE:
                    cmds = CreatePatientSqlFile();
                    break;
            }
            if(cmds!=null) {
                for (String cmd : cmds) {
                    bw.write(cmd);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(bw!=null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return 0;
    }

    public int InitAreas(String path,String fileName){

        File configFile = new File(path, fileName);
        try {
            if (configFile.exists()) {
                FileInputStream finput = new FileInputStream(configFile);
                int len = finput.available();
                byte[] data = new byte[len];
                int readlen = finput.read(data);
                finput.close();
                if(readlen>0) {
                    String config = new String(data, "UTF-8");
                    ApplyConfig(config);
                    CreateSqlFile(path,AREA_SQL_TYPE);
                    CreateSqlFile(path,ROOM_SQL_TYPE);
                    CreateSqlFile(path,BED_SQL_TYPE);
                    CreateSqlFile(path,DEV_SQL_TYPE);
                    CreateSqlFile(path,PATIENT_SQL_TYPE);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return 0;
    }

}
