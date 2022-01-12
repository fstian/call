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
import java.util.Iterator;

public class SqlAreasConf {

    private final String AREAS_NAME = "areas";
    private final String FLOOR_NAME = "floor";
    private final String LOCATION_NAME = "location";
    private final String AREANAME_NAME = "name";
    private final String AREACODE_NAME = "code";
    private final String NETWORK_NAME = "network";
    private final String BEDNUM_NAME = "bedNum";
    private final String DOORNUM_NAME = "doorNum";
    private final String OLDAREACODE_NAME = "oldCode";
    private final String CLEAROLD_NAME = "clearOld";
    private final String SKIPFOUR_NAME = "Skip4";

    private final int AREA_SQL_TYPE = 1;
    private final String SQL_AREAS_FILE = "infusion_areainfo.sql";
    private final String SQL_AREAS_UPDATE_FILE = "infusion_update_areainfo.sql";
    private final int ROOM_SQL_TYPE = 2;
    private final String SQL_ROOMS_FILE = "infusion_roominfo.sql";
    private final String SQL_ROOMS_UPDATE_FILE = "infusion_update_roominfo.sql";
    private final int BED_SQL_TYPE = 3;
    private final String SQL_BED_FILE = "infusion_bedinfo.sql";
    private final String SQL_BED_UPDATE_FILE = "infusion_update_bedinfo.sql";
    private final int DEV_SQL_TYPE = 4;
    private final String SQL_DEV_FILE = "infusion_devinfo.sql";
    private final String SQL_DEV_UPDATE_FILE = "infusion_update_devinfo.sql";
    private final int PATIENT_SQL_TYPE = 5;
    private final String SQL_PATIENT_FILE = "infusion_patientinfo.sql";
    private final String SQL_PATIENT_UPDATE_FILE = "infusion_update_patientinfo.sql";
    private final int CALL_PARAM_SQL_TYPE = 6;
    private final String SQL_CALL_PARAM_FILE = "infusion_call_params.sql";
    private final int INFUSION_PARAM_SQL_TYPE = 7;
    private final String SQL_INFUSION_PARAM_FILE = "infusion_ifs_params.sql";

    private final int SIP_NUMBER_FILES_TYPE = 8;
    private final String SIP_NUMBER_SAMPLE_FILE = "sipSample.xml";

    private final String PARAMS_NAME = "params";
    private final String PARAM_DESC_NAME = "desc";
    private final String PARAM_NAME_NAME = "name";
    private final String PARAM_VALUE_NAME = "value";

    ArrayList<SqlAreaInfo> areaLists;

    ArrayList<SqlParamItem> callParamList;

    public SqlAreasConf(){
        areaLists = new ArrayList<>();
        callParamList = new ArrayList<>();
    }

    private int ApplyAreaConfig(String config){
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
            sqlArea.oldCode = areaJson.getString(OLDAREACODE_NAME);
            sqlArea.netWork = areaJson.getInteger(NETWORK_NAME);
            sqlArea.bedNum = areaJson.getInteger(BEDNUM_NAME);
            sqlArea.doorNum = areaJson.getInteger(DOORNUM_NAME);
            sqlArea.clearOld = areaJson.getBooleanValue(CLEAROLD_NAME);
            sqlArea.skipFour = areaJson.getBooleanValue(SKIPFOUR_NAME);
            areaLists.add(sqlArea);
        }

        return 0;
    }

    private ArrayList<SqlParamItem> ApplayParams(String buf){
        ArrayList<SqlParamItem> lists = new ArrayList<>();

        JSONObject json;
        JSONArray paramsJson;
        JSONObject itemJson;
        int iTmp;

        json = JSONObject.parseObject(buf);
        if(json == null)
            return lists;
        paramsJson = json.getJSONArray(PARAMS_NAME);
        if(paramsJson==null)
            return lists;

        for(iTmp=0;iTmp<paramsJson.size();iTmp++) {
            itemJson = paramsJson.getJSONObject(iTmp);
            SqlParamItem paramItem = new SqlParamItem();
            paramItem.desc = itemJson.getString(PARAM_DESC_NAME);
            paramItem.name = itemJson.getString(PARAM_NAME_NAME);
            paramItem.value = itemJson.getIntValue(PARAM_VALUE_NAME);

            lists.add(paramItem);
        }
        return lists;
    }

    private ArrayList<String> CreateAreasSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;

        cmd = "DELETE FROM `infusion_areainfo`;\n";
        cmds.add(cmd);

        for(SqlAreaInfo area:areaLists){
            cmd = String.format("INSERT INTO `infusion_areainfo` (`area_code`, `area_name`) VALUES " +
                            "(\'%s\', \'%s\');\n",
                    area.code,area.name);

            cmds.add(cmd);
        }
        return cmds;
    }

    private ArrayList<String> CreateAreasUpdateSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;

        for(SqlAreaInfo area:areaLists){

            if(area.oldCode!=null) {
                if(!area.oldCode.isEmpty()) {
                    cmd = String.format("UPDATE infusion_areainfo SET area_code=\'%s\',area_name=\'%s\' where area_code=\'%s\';\n", area.code, area.name, area.oldCode);
                    cmds.add(cmd);

                    cmd = String.format("UPDATE infusion_devinfo SET f_area_code=\'%s\' where f_area_code=\'%s\';\n", area.code, area.oldCode);
                    cmds.add(cmd);

                    cmd = String.format("UPDATE infusion_roominfo SET f_area_code=\'%s\' where f_area_code=\'%s\';\n", area.code, area.oldCode);
                    cmds.add(cmd);

                    cmd = String.format("UPDATE infusion_bedinfo SET f_area_code=\'%s\' where f_area_code=\'%s\';\n", area.code, area.oldCode);
                    cmds.add(cmd);

                    cmd = String.format("DELETE FROM infusion_roominfo where f_area_code=%s and room_code=4;\n",area.code);
                    cmds.add(cmd);

                    cmd = String.format("DELETE FROM infusion_roominfo where f_area_code=%s and room_code=14;\n",area.code);
                    cmds.add(cmd);

                    cmd = String.format("INSERT INTO infusion_roominfo (`f_area_code`,`room_code`,`room_name`) values (\'%s\',\'%d\',\'%d\');\n",area.code,area.doorNum+1,area.doorNum+1);
                    cmds.add(cmd);

                    cmd = String.format("INSERT INTO infusion_roominfo (`f_area_code`,`room_code`,`room_name`) values (\'%s\',\'%d\',\'%d\');\n",area.code,area.doorNum+2,area.doorNum+2);
                    cmds.add(cmd);
                }
            }
        }
        return cmds;
    }

    private ArrayList<String> CreateRoomSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        int iTmp;

        cmd = "DELETE FROM `infusion_roominfo`;\n";
        cmds.add(cmd);
        for(SqlAreaInfo area:areaLists) {
            for(iTmp=0;iTmp<area.doorNum;iTmp++){
                cmd = String.format("INSERT INTO `infusion_roominfo`(`f_area_code`, `room_code`, `room_name`) VALUES " +
                                "(\'%s\', \'%s\', \'%s\');\n",
                        area.code,""+iTmp+1,"R_"+iTmp+1);

                cmds.add(cmd);
            }
        }
        return cmds;
    }

    private ArrayList<String> CreateBedSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        int iTmp;

        cmd = "DELETE FROM `infusion_bedinfo`;\n";
        cmds.add(cmd);
        for(SqlAreaInfo area:areaLists) {
            for(iTmp=0;iTmp<area.bedNum;iTmp++){

                cmd = String.format("INSERT INTO `infusion_bedinfo`(`f_area_code`, `f_patient_id`, `bed_name`, `used`, `doctor`, `nurse`, `enable`, `f_room_code`) VALUES" +
                                " (\'%s\', \'%s\', \'%s\', \'1\', \'\', \'\', \'1\',\'%s\');\n",
                        area.code,area.CreateBedCode(iTmp),""+iTmp+1,area.CreateRoomOfBed(area.skipFour,iTmp));

                cmds.add(cmd);
            }
        }
        return cmds;
    }

    private ArrayList<String> CreateBedUpdateSqlFile(SqlAreaInfo area,ArrayList<String> bedList) {
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        int iTmp;

        cmd = String.format("DELETE FROM `infusion_bedinfo` where f_area_code=\'%s\';\n",area.code);
        cmds.add(cmd);

        iTmp = 0;

        for(String bedName:bedList){
            if(iTmp<area.bedNum) {
                cmd = String.format("INSERT INTO `infusion_bedinfo`(`f_area_code`, `f_patient_id`, `bed_name`, `used`, `doctor`, `nurse`, `enable`, `f_room_code`) VALUES" +
                                " (\'%s\', \'%s\', \'%s\', \'1\', \'\', \'\', \'1\',\'\');\n",
                        area.code, area.CreatePatientCode(iTmp), bedName);
                cmds.add(cmd);
            }else {
                cmd = String.format("INSERT INTO `infusion_bedinfo`(`f_area_code`, `f_patient_id`, `bed_name`, `used`, `doctor`, `nurse`, `enable`, `f_room_code`) VALUES" +
                                " (\'%s\', \'%s\', \'%s\', \'1\', \'\', \'\', \'1\',\'\');\n",
                        area.code, "", bedName);
                cmds.add(cmd);
            }
            iTmp++;
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
//                cmd = String.format("INSERT INTO `infusion_patientinfo`(`id`, `patient_name`, `in_id`, `diagnosis_id`, `in_date`, `out_date`, `doctor_name`, `nurse_name`, `gender`, `age`, `in_diagnosis`, `out_diagnosis`, `phone`, `allergy`, `mitype`, `carelv`, `p_idx`, `meal`, `isolation`, `fall_tumble`, `drop_off`, `press_sore`, `text_care`, `text_meal`, `critical_ill`, `serious_ill`, `abo_group`, `ah_group`, `touch_isolation`, `prepare_in`, `know_sensitive`, `monitor_pain`, `monitor_drug`, `monitor_drop`, `monitor_fall`, `monitor_press`) VALUES " +
//                                "(%d, \'%s\', \'%s\', \'%s\', \'2020-12-09 08:29:26\', \'2020-12-16 10:48:54\', \'\', \'\', \'女\', \'58岁\', \'\', \'\', \'\', \'\', \'省内异地居民医保\', \'\', \'%s\', \'\', \'\', 0, 0, 0, \'\', \'\', 0, 0, \'\', \'\', 0, \'\', 0, \'N\', \'N\', \'N\', \'N\', \'N\');\n",
//                        recordNum,area.CreatePatientName(iTmp),area.CreatePatientCode(iTmp),area.CreatePatientCode(iTmp),area.CreatePatientCode(iTmp));

                cmd = String.format("INSERT INTO `infusion_patientinfo`(`patient_name`, `in_id`, `diagnosis_id`, `in_date`, `out_date`, `doctor_name`, `nurse_name`, `gender`, `age`, `in_diagnosis`, `out_diagnosis`, `phone`, `allergy`, `mitype`, `carelv`, `p_idx`, `meal`, `isolation`, `fall_tumble`, `drop_off`, `press_sore`, `text_care`, `text_meal`, `critical_ill`, `serious_ill`, `abo_group`, `ah_group`, `touch_isolation`, `prepare_in`, `know_sensitive`, `monitor_pain`, `monitor_drug`, `monitor_drop`, `monitor_fall`, `monitor_press`) VALUES " +
                                "(\'%s\', \'%s\', \'%s\', \'2020-12-09 08:29:26\', \'2020-12-16 10:48:54\', \'\', \'\', \'女\', \'58岁\', \'\', \'\', \'\', \'\', \'省内异地居民医保\', \'\', \'%s\', \'\', \'\', 0, 0, 0, \'\', \'\', 0, 0, \'\', \'\', 0, \'\', 0, \'N\', \'N\', \'N\', \'N\', \'N\');\n",
                        area.CreatePatientName(iTmp),area.CreatePatientCode(iTmp),area.CreatePatientCode(iTmp),area.CreatePatientCode(iTmp));

                recordNum++;
                cmds.add(cmd);
            }
        }
        return cmds;
    }

    private ArrayList<String> CreateDevSqlFile(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        int iTmp;
        int recordNum = 1;

        for(SqlAreaInfo area:areaLists) {
            cmd = String.format("DELETE FROM infusion_devinfo where f_area_code=\'%s\' and dev_type=2;\n",area.code);
            cmds.add(cmd);

            for(iTmp=0;iTmp<area.bedNum;iTmp++){
                // bed_id is bed_name
                cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                "(2, \'%s\', \'%s\', \'%s\', \'\', \'0-%s\', \'%s\', \'%s\', \'%s\', 0, 1, 1, 1, 0, 1, 0, 0, \'\');\n",
                        area.CreateBedDevCode(iTmp),area.CreateBedDevName(iTmp),area.code,area.GetDefaultRoute(),area.GetDefaultRoute(),""+(iTmp+1),area.GetDefaultRoute());

                recordNum++;
                cmds.add(cmd);


                //emergency
                if(area.location==2) {
                    if(area.floor>=5) {
                        if (iTmp % 3 == 0) {
                            cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                            "(4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                                    area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(area.skipFour,iTmp));
                            recordNum++;
                            cmds.add(cmd);
                        }
                    }
                }else if(area.location==1) {
                    if(area.floor>=6) {
                        if (iTmp % 3 == 0 && iTmp / 3 < area.doorNum) {
                            cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                            "(4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                                    area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(area.skipFour,iTmp));
                            recordNum++;
                            cmds.add(cmd);
                        }
                    }
//                    }else {
//                        cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                                        "(4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
//                                area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(iTmp));
//                        recordNum++;
//                        cmds.add(cmd);
//                    }
                }else if(area.location==3) {
                    cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
                                    "(4, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%s\');\n",
                            area.CreateEmergencyDevCode(iTmp), area.CreateEmergencyDevName(iTmp), area.code, area.CreateRoomOfBed(area.skipFour,iTmp));
                    recordNum++;
                    cmds.add(cmd);
                }
            }

            //door
//            for(iTmp=0;iTmp<area.doorNum;iTmp++){
//                cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                                "(6, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, \'%d\');\n",
//                        area.CreateDoorDevCode(iTmp),area.CreateDoorDevName(iTmp),area.code,iTmp+1);
//                recordNum++;
//                cmds.add(cmd);
//            }
            // master
//            cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                            "(0, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
//                    area.GetDefaultRoute(),area.GetDefaultRoute(),area.code);
//            recordNum++;
//            cmds.add(cmd);

            //tv
//            cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                            "(8, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
//                    area.GetTVCode(),area.GetTVCode(),area.code);
//            recordNum++;
//            cmds.add(cmd);

            //corridor
//            cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                            "(7, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
//                    area.GetCorridorCode(),area.GetCorridorCode(),area.code);
//            recordNum++;
//            cmds.add(cmd);

            //Nurser Phone
//            cmd = String.format("INSERT INTO `infusion_devinfo`(`dev_type`, `dev_id`, `dev_name`, `f_area_code`, `note`, `route`, `parent_id`, `f_bed_id`, `mngr_id`, `shut_screen`, `online_status`, `comm_status`, `separate`, `detected`, `hold_back`, `low_power`, `motor_life_used`, `f_room_code`) VALUES " +
//                            "(9, \'%s\', \'%s\', \'%s\', \'\', \'\', \'\', \'\', \'\', 0, 0, 0, 0, 0, 0, 0, 0, NULL);\n",
//                    area.GetPhoneCode(),area.GetPhoneCode(),area.code);
//            recordNum++;
//            cmds.add(cmd);
        }
        return cmds;

    }

    private ArrayList<String> CreateDevUpdateSqlFile(SqlAreaInfo area){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;
        int iTmp;

        if(area.clearOld) {
            cmd = String.format("UPDATE infusion_devinfo set f_bed_id=\'\',f_room_code=\'\' where f_area_code=\'%s\';\n", area.code);
            cmds.add(cmd);
        }

        for (iTmp = 0; iTmp < area.bedNum; iTmp++) {
            cmd = String.format("update infusion_devinfo set dev_name=\'%s\' where dev_id=\'%s\';\n",area.CreateBedDevName(iTmp),area.CreateBedDevCode(iTmp));
            cmds.add(cmd);

            cmd = String.format("update infusion_devinfo set dev_name=\'%s\' where dev_id=\'%s\';\n",area.CreateEmergencyDevName(iTmp),area.CreateEmergencyDevCode(iTmp));
            cmds.add(cmd);

            if(area.location==1||area.location==2) {
                if ((iTmp % 3) != 0 || (iTmp ) / 3 >= area.doorNum) {
                    cmd = String.format("delete from infusion_devinfo where dev_id=\'%s\';\n", area.CreateEmergencyDevCode(iTmp));
                    cmds.add(cmd);
                }
            }
        }

        for(iTmp=0;iTmp<area.doorNum;iTmp++) {
            cmd = String.format("update infusion_devinfo set dev_name=\'%s\' where dev_id=\'%s\';\n", area.CreateDoorDevName(iTmp), area.CreateDoorDevCode(iTmp));
            cmds.add(cmd);
        }

        return cmds;
    }

    private ArrayList<String> CreateCallParamCmds(){
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;

        cmd = String.format("delete from infusion_call_config;\n");
        cmds.add(cmd);

        for(SqlAreaInfo area:areaLists){
            for(SqlParamItem item:callParamList){
                cmd = String.format("insert into `infusion_call_config`(`area_code`,`param_name`,`param_id`,`default_val`) values (\'%s\',\'%s\',\'%s\', %d);\n",area.code,item.desc,item.name,item.value);
                cmds.add(cmd);
            }
        }

        return cmds;
    }

    private ArrayList<String> CreateInfusionParamCmd() {
        ArrayList<String> cmds= new ArrayList<>();
        String cmd;

        cmd = String.format("delete from infusion_config;\n");
        cmds.add(cmd);

        for(SqlAreaInfo area:areaLists){
            for(SqlParamItem item:callParamList){
                cmd = String.format("insert into `infusion_config`(`area_code`,`param_name`,`param_id`,`param_val`) values (\'%s\',\'%s\',\'%s\', %d);\n",area.code,item.desc,item.name,item.value);
                cmds.add(cmd);
            }
        }

        return cmds;

    }

    private int CreateUpdateSqlFile(String path,int type,SqlBedInfo beds) {
        String fileName = null;
        BufferedWriter bw=null;
        ArrayList<String> cmds=null;
        File sqlFile;

        switch(type){
            case AREA_SQL_TYPE:
                fileName = SQL_AREAS_UPDATE_FILE;
                break;
            case BED_SQL_TYPE:
                fileName = SQL_BED_UPDATE_FILE;
                break;
            case DEV_SQL_TYPE:
                fileName = SQL_DEV_UPDATE_FILE;
                break;
        }

        if(type==AREA_SQL_TYPE) {
            sqlFile = new File(path, fileName);
            try {
                bw = new BufferedWriter(new FileWriter(sqlFile, false));
                cmds = CreateAreasUpdateSqlFile();

                if (cmds != null) {
                    for (String cmd : cmds) {
                        bw.write(cmd);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bw != null) {
                    try {
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }else if(type==BED_SQL_TYPE||type==DEV_SQL_TYPE){
            for(SqlAreaInfo area:areaLists){
                ArrayList<String> bedList = beds.GetBedList(area.code);
                if(bedList!=null) {
                    sqlFile = new File(path, area.oldCode+"_"+area.code+"_"+fileName);
                    if(type==BED_SQL_TYPE)
                        cmds = CreateBedUpdateSqlFile(area,bedList);
                    else
                        cmds = CreateDevUpdateSqlFile(area);
                    try {
                        bw = new BufferedWriter(new FileWriter(sqlFile, false));

                        if (cmds != null) {
                            for (String cmd : cmds) {
                                bw.write(cmd);
                            }
                        }
                    }catch (IOException e){
                        e.printStackTrace();
                    }finally {
                        if(bw!=null){
                            try{
                                bw.close();
                            }catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return 0;

    }

    public int CreateSipNumberFile(String path){
        int iTmp;

        for(SqlAreaInfo area:areaLists){
            for(iTmp=0;iTmp<area.bedNum;iTmp++){
                
            }
        }
        return 0;
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
            case CALL_PARAM_SQL_TYPE:
                fileName = SQL_CALL_PARAM_FILE;
                break;
            case INFUSION_PARAM_SQL_TYPE:
                fileName = SQL_INFUSION_PARAM_FILE;
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
                case CALL_PARAM_SQL_TYPE:
                    cmds = CreateCallParamCmds();
                    break;
                case INFUSION_PARAM_SQL_TYPE:
                    cmds = CreateInfusionParamCmd();
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

    public int CreateCallParamSqlFile(String path){
        CreateSqlFile(path,CALL_PARAM_SQL_TYPE);
        return 0;
    }

    public int CreateInfusionParamSqlFile(String path){
        CreateSqlFile(path,INFUSION_PARAM_SQL_TYPE);
        return 0;
    }

    public int CreateSqlFiles(String path) {
        CreateSqlFile(path,AREA_SQL_TYPE);
        CreateSqlFile(path,ROOM_SQL_TYPE);
        CreateSqlFile(path,BED_SQL_TYPE);
        CreateSqlFile(path,DEV_SQL_TYPE);
        CreateSqlFile(path,PATIENT_SQL_TYPE);

        return 0;
    }

    public int CreateUpdateSqlFile(String path,SqlBedInfo beds){

        CreateUpdateSqlFile(path,AREA_SQL_TYPE,beds);
        CreateUpdateSqlFile(path,BED_SQL_TYPE,beds);
        CreateUpdateSqlFile(path,DEV_SQL_TYPE,beds);
//        CreateUpdateSqlFile(path,PATIENT_SQL_TYPE,beds);

        return 0;
    }

    public int InitCallParams(String path,String fileName){
        String fileContext = SqlParamItem.ReadConfigFile(path,fileName);
        callParamList = ApplayParams(fileContext);
        return 0;
    }

    public int InitAreas(String path,String fileName){

        String fileContext = SqlParamItem.ReadConfigFile(path,fileName);
        ApplyAreaConfig(fileContext);

        return 0;
    }

}
