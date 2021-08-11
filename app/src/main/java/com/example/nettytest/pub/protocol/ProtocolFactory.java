package com.example.nettytest.pub.protocol;

import com.alibaba.fastjson.*;
import com.example.nettytest.pub.JsonPort;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.commondevice.PhoneDevice;
import java.io.UnsupportedEncodingException;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;


public class ProtocolFactory {

    public static ProtocolPacket ParseData(ByteBuf data) {
        return ParseData(data.toString(CharsetUtil.UTF_8));
    }

    public static ProtocolPacket ParseData(byte[] data) {
        ProtocolPacket packet = null;
        try {
            String dataString = new String(data,"UTF-8");
            packet = ParseData(dataString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return packet;
    }

    public static ProtocolPacket ParseData(String data) {
        ProtocolPacket p = null;
        if(data==null){
            LogWork.Print(LogWork.DEBUG_MODULE, LogWork.LOG_ERROR, "Call JSON Para with NULL String");
            return null;
        }
        try {
            JSONObject json = JSONObject.parseObject(data);
            if(json==null){
                LogWork.Print(LogWork.DEBUG_MODULE, LogWork.LOG_ERROR, "JSON Para String %s Fail",data);
                return null;
            }
                
            JSONObject context;
            int type = json.getIntValue(ProtocolPacket.PACKET_TYPE_NAME);
            switch(type){
                case ProtocolPacket.REG_REQ:
                    RegReqPack regReqPack = new RegReqPack();
                    PutDefaultData(regReqPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        regReqPack.address = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_ADDRESS_NAME);
                        regReqPack.devID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        regReqPack.expireTime = context.getIntValue(ProtocolPacket.PACKET_EXPIRE_NAME);
                        regReqPack.devType = context.getIntValue(ProtocolPacket.PACKET_DEVTYPE_NAME);
                        p = regReqPack;
                    }
                    break;
                case ProtocolPacket.REG_RES:
                    RegResPack regResPack = new RegResPack();
                    PutDefaultData(regResPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        regResPack.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        regResPack.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        regResPack.areaId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_AREAID_NAME);
                        regResPack.areaName = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_AREANAME_NAME);
                        regResPack.transferAreaId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_TRANSFER_AREAID_NAME);
                        regResPack.listenCallEnable = context.getBooleanValue(ProtocolPacket.PACKET_LISTEN_STATE_NAME);
                        regResPack.snapPort = context.getIntValue(ProtocolPacket.PACKET_SNAP_PORT_NAME);
                        p = regResPack;
                    }
                    break;
                case ProtocolPacket.CALL_REQ:
                    InviteReqPack inviteReqPack = new InviteReqPack();
                    PutDefaultData(inviteReqPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        inviteReqPack.callType = context.getIntValue(ProtocolPacket.PACKET_CALLTYPE_NAME);
                        inviteReqPack.callDirect = context.getIntValue(ProtocolPacket.PACKET_CALLDIRECT_NAME);
                        inviteReqPack.callID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);

                        inviteReqPack.caller = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLER_NAME);
                        inviteReqPack.callee = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLEE_NAME);
                        inviteReqPack.callerType = context.getIntValue(ProtocolPacket.PACKET_CALLERTYPE_NAME);

                        inviteReqPack.codec = context.getIntValue(ProtocolPacket.PACKET_CODEC_NAME);
                        inviteReqPack.callerRtpIP = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLERIP_MAME);
                        inviteReqPack.callerRtpPort = context.getIntValue(ProtocolPacket.PACKET_CALLERPORT_NAME);

                        inviteReqPack.patientName = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_PATIENT_NAME_NAME);
                        inviteReqPack.patientAge = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_PATIENT_AGE_NAME);
                        inviteReqPack.bedName = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_BEDID_NAME);
                        inviteReqPack.areaId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_AREAID_NAME);
                        inviteReqPack.areaName= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_AREANAME_NAME);
                        inviteReqPack.isTransfer=context.getBooleanValue(ProtocolPacket.PACKET_ISTRANSFER_NAME);

                        inviteReqPack.deviceName = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVICE_NAME_NAME);
                        inviteReqPack.roomId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_ROOMID_NAME);

                        inviteReqPack.pTime = context.getIntValue(ProtocolPacket.PACKET_PTIME_NAME);
                        inviteReqPack.codec = context.getIntValue(ProtocolPacket.PACKET_CODEC_NAME);
                        inviteReqPack.sample = context.getIntValue(ProtocolPacket.PACKET_SAMPLE_NAME);
                        inviteReqPack.autoAnswerTime = context.getIntValue(ProtocolPacket.PACKET_AUTOANSWER_TIME_NAME);

                        p = inviteReqPack;
                    }
                    break;
                case  ProtocolPacket.CALL_RES:
                    InviteResPack inviteResPack = new InviteResPack();
                    PutDefaultData(inviteResPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    if(context!=null){
                        inviteResPack.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        inviteResPack.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);

                        inviteResPack.callID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        p = inviteResPack;
                    }
                    break;
                case ProtocolPacket.END_REQ:
                    EndReqPack endReqPack = new EndReqPack();
                    PutDefaultData(endReqPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        endReqPack.endDevID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        endReqPack.callID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        p = endReqPack;
                    }
                    break;
                case ProtocolPacket.END_RES:
                    EndResPack endResPack = new EndResPack();
                    PutDefaultData(endResPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        endResPack.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        endResPack.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        endResPack.callId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        p = endResPack;
                    }
                    break;
                case ProtocolPacket.ANSWER_REQ:
                    AnswerReqPack answerReqP = new AnswerReqPack();
                    PutDefaultData(answerReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        answerReqP.callID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        answerReqP.answerer = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_ANSWERER_NAME);
                        answerReqP.answererRtpIP = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLEEIP_MAME);
                        answerReqP.answererRtpPort = context.getIntValue(ProtocolPacket.PACKET_CALLEEPORT_NAME);
                        answerReqP.answerBedName = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_BEDID_NAME);
                        answerReqP.codec = context.getIntValue(ProtocolPacket.PACKET_CODEC_NAME);
                        answerReqP.pTime = context.getIntValue(ProtocolPacket.PACKET_PTIME_NAME);
                        answerReqP.sample= context.getIntValue(ProtocolPacket.PACKET_SAMPLE_NAME);
                        answerReqP.callType = context.getIntValue(ProtocolPacket.PACKET_CALLTYPE_NAME);
                        answerReqP.answerDeviceName = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVICE_NAME_NAME);
                        answerReqP.answerRoomId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_ROOMID_NAME);
                        p = answerReqP;
                    }
                    break;
                case ProtocolPacket.ANSWER_RES:
                    AnswerResPack answerResP = new AnswerResPack();
                    PutDefaultData(answerResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        answerResP.callID =  JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        answerResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        answerResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        p = answerResP;
                    }
                    break;
                case ProtocolPacket.DEV_QUERY_REQ:
                    DevQueryReqPack devReqP = new DevQueryReqPack();
                    PutDefaultData(devReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        devReqP.devid = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        p = devReqP;
                    }
                    break;
                case ProtocolPacket.DEV_QUERY_RES:
                    DevQueryResPack devResP = new DevQueryResPack();
                    PutDefaultData(devResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        devResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        devResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        JSONArray phoneLists = context.getJSONArray(ProtocolPacket.PACKET_DETAIL_NAME);
                        if(phoneLists!=null) {
                            for (int iTmp = 0; iTmp < phoneLists.size(); iTmp++) {
                                JSONObject jsonObj = phoneLists.getJSONObject(iTmp);
                                PhoneDevice phone = new PhoneDevice();
                                phone.id = JsonPort.GetJsonString(jsonObj,ProtocolPacket.PACKET_DEVID_NAME);
                                phone.type = jsonObj.getIntValue(ProtocolPacket.PACKET_DEVTYPE_NAME);
                                phone.isReg = jsonObj.getBooleanValue(ProtocolPacket.PACKET_STATUS_NAME);
                                phone.bedName = JsonPort.GetJsonString(jsonObj,ProtocolPacket.PACKET_BEDID_NAME);
                                devResP.phoneList.add(phone);
                            }
                        }
                        p = devResP;
                    }
                    break;
                case ProtocolPacket.CALL_UPDATE_REQ:
                    UpdateReqPack updateReqP = new UpdateReqPack();
                    PutDefaultData(updateReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        updateReqP.callId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        updateReqP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        p = updateReqP;
                    }
                    break;
                case ProtocolPacket.CALL_UPDATE_RES:
                    UpdateResPack updateResP = new UpdateResPack();
                    PutDefaultData(updateResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        updateResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        updateResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        updateResP.callid = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                        p = updateResP;
                    }
                    break;
                case ProtocolPacket.DEV_CONFIG_REQ:
                    ConfigReqPack configReqP = new ConfigReqPack();
                    PutDefaultData(configReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        configReqP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        p = configReqP;
                    }
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_REQ:
                    SystemConfigReqPack systemConfigReqP = new SystemConfigReqPack();
                    PutDefaultData(systemConfigReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        systemConfigReqP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        p = systemConfigReqP;
                    }
                    break;
                case ProtocolPacket.DEV_CONFIG_RES:
                    ConfigResPack configResP = new ConfigResPack();
                    PutDefaultData(configResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        configResP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        JSONArray paramList = context.getJSONArray(ProtocolPacket.PACKET_PARAMS_NAME);
                        if(paramList!=null){
                            for(int iTmp=0;iTmp<paramList.size();iTmp++){
                                JSONObject item = paramList.getJSONObject(iTmp);
                                ConfigItem param = new ConfigItem();
                                param.param_id = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_ID_NAME);
                                param.param_name = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_NAME_NAME);
                                param.param_value = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_VALUE_NAME);
                                param.param_unit = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_UNIT_NAME);
                                configResP.params.add(param);
                            }
                        }
                        p = configResP;
                    }
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_RES:
                    SystemConfigResPack systemConfigResP = new SystemConfigResPack();
                    PutDefaultData(systemConfigResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        systemConfigResP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        JSONArray systemParamList = context.getJSONArray(ProtocolPacket.PACKET_PARAMS_NAME);
                        if(systemParamList!=null){
                            for(int iTmp=0;iTmp<systemParamList.size();iTmp++){
                                JSONObject item = systemParamList.getJSONObject(iTmp);
                                ConfigItem param = new ConfigItem();
                                param.param_id = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_ID_NAME);
                                param.param_name = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_NAME_NAME);
                                param.param_value = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_VALUE_NAME);
                                param.param_unit = JsonPort.GetJsonString(item,ProtocolPacket.PACKET_PARAM_UNIT_NAME);
                                systemConfigResP.params.add(param);
                            }
                        }
                        p = systemConfigResP;
                    }
                    break;
                case ProtocolPacket.CALL_TRANSFER_REQ:
                    TransferReqPack transferReqP = new TransferReqPack();
                    PutDefaultData(transferReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        transferReqP.transferAreaId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_TRANSFER_AREAID_NAME);
                        transferReqP.devID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        transferReqP.transferEnabled = context.getBooleanValue(ProtocolPacket.PACKET_TRANSFER_STATE_NAME);
                        p = transferReqP;
                    }
                    break;
                case ProtocolPacket.CALL_TRANSFER_RES:
                    TransferResPack transferResP = new TransferResPack();
                    PutDefaultData(transferResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        transferResP.transferAreaId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_TRANSFER_AREAID_NAME);
                        transferResP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        transferResP.state = context.getBooleanValue(ProtocolPacket.PACKET_TRANSFER_STATE_NAME);
                        transferResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        transferResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        p = transferResP;
                    }
                    break;
                case ProtocolPacket.CALL_LISTEN_REQ:
                    ListenCallReqPack listeReqP = new ListenCallReqPack();
                    PutDefaultData(listeReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        listeReqP.devID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        listeReqP.listenEnable = context.getBooleanValue(ProtocolPacket.PACKET_LISTEN_STATE_NAME);
                        p = listeReqP;
                    }
                    break;
                case ProtocolPacket.CALL_LISTEN_CLEAR_REQ:
                    ListenClearReqPack listenClearReqP = new ListenClearReqPack();
                    PutDefaultData(listenClearReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        listenClearReqP.devID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        listenClearReqP.status = context.getBooleanValue(ProtocolPacket.PACKET_LISTEN_STATE_NAME);
                        p = listenClearReqP;
                    }
                    break;
                case ProtocolPacket.CALL_LISTEN_RES:
                    ListenCallResPack listenResP = new ListenCallResPack();
                    PutDefaultData(listenResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        listenResP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        listenResP.state = context.getBooleanValue(ProtocolPacket.PACKET_LISTEN_STATE_NAME);
                        listenResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        listenResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        p = listenResP;
                    }
                    break;
                case ProtocolPacket.CALL_LISTEN_CLEAR_RES:
                    ListenClearResPack listenClearResP = new ListenClearResPack();
                    PutDefaultData(listenClearResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    if(context!=null){
                        listenClearResP.devId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                        listenClearResP.state = context.getBooleanValue(ProtocolPacket.PACKET_LISTEN_STATE_NAME);
                        listenClearResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                        listenClearResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                        p = listenClearResP;
                    }
                    break;
                case ProtocolPacket.CALL_VIDEO_INVITE_REQ:
                    StartVideoReqPack startReqP = new StartVideoReqPack();
                    PutDefaultData(startReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    
                    startReqP.startVideoDevId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                    startReqP.callID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                    p = startReqP;
                    break;
                case ProtocolPacket.CALL_VIDEO_INVITE_RES:
                    StartVideoResPack startResP = new StartVideoResPack();
                    PutDefaultData(startResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    startResP.startVideoDevId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                    startResP.callid = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                    startResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                    startResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                    p = startResP;
                    break;
                case ProtocolPacket.CALL_VIDEO_END_REQ:
                    StopVideoReqPack stopReqP = new StopVideoReqPack();
                    PutDefaultData(stopReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    stopReqP.stopVideoDevId = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                    stopReqP.callID = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                    p = stopReqP;
                    break;
                case ProtocolPacket.CALL_VIDEO_END_RES:
                    StopVideoResPack stopResP = new StopVideoResPack();
                    PutDefaultData(stopResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    stopResP.stopVideoDevId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                    stopResP.callid= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                    stopResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                    stopResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                    p = stopResP;
                    break;
                case ProtocolPacket.CALL_VIDEO_ANSWER_REQ:
                    AnswerVideoReqPack answerVideoReqP = new AnswerVideoReqPack();
                    PutDefaultData(answerVideoReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    answerVideoReqP.answerDevId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                    answerVideoReqP.callId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                    p = answerVideoReqP;
                    break;
                case ProtocolPacket.CALL_VIDEO_ANSWER_RES:
                    AnswerVideoResPack answerVideoResP = new AnswerVideoResPack();
                    PutDefaultData(answerVideoResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    answerVideoResP.answerVideoDevId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_DEVID_NAME);
                    answerVideoResP.callId= JsonPort.GetJsonString(context,ProtocolPacket.PACKET_CALLID_NAME);
                    answerVideoResP.result = JsonPort.GetJsonString(context,ProtocolPacket.PACKET_RESULT_NAME);
                    answerVideoResP.status = context.getIntValue(ProtocolPacket.PACKET_STATUS_NAME);
                    p = answerVideoResP;
                    break;
            }
            json.clear();
        }catch (JSONException e){
            e.printStackTrace();
            LogWork.Print(LogWork.TERMINAL_USER_MODULE, LogWork.LOG_TEMP_DBG, "Recv Unparse data %s", data);
            if(HasNoPrintableChar(data)){
                LogWork.Print(LogWork.TERMINAL_USER_MODULE, LogWork.LOG_TEMP_DBG, "Recv Unparse data %s", GetRawData(data));
            }
        }

        return p;
    }

    public static boolean HasNoPrintableChar(String data){
        boolean flag = false;
        byte[] bdata = data.getBytes();
        int len = bdata.length;
        for(int iTmp=0;iTmp<len;iTmp++){
            if(bdata[iTmp]<0x20) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static String GetRawData(String data){
        byte[] bdata = data.getBytes();
        int len = bdata.length;
        StringBuilder raw = new StringBuilder();
        for(int iTmp=0;iTmp<len;iTmp++) {
            raw.append(String.format("%02x",bdata[iTmp]));
        }
        return raw.toString();
    }



    public static String PacketData(ProtocolPacket p){
        String data;
        JSONObject json = new JSONObject();
        JSONObject context = new JSONObject();
        try {
            json.put(ProtocolPacket.PACKET_TYPE_NAME,p.type);
            json.put(ProtocolPacket.PACKET_MSGID_NAME,p.msgID);
            json.put(ProtocolPacket.PACKET_SENDERID_NAME,p.sender);
            json.put(ProtocolPacket.PACKET_RECEIVERID_NAME,p.receiver);
            switch(p.type){
                case ProtocolPacket.REG_REQ:
                    RegReqPack regReqP = (RegReqPack) p;
                    context.put(ProtocolPacket.PACKET_ADDRESS_NAME,regReqP.address);
                    context.put(ProtocolPacket.PACKET_DEVTYPE_NAME,regReqP.devType);
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,regReqP.devID);
                    context.put(ProtocolPacket.PACKET_EXPIRE_NAME,regReqP.expireTime);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.REG_RES:
                    RegResPack regResP = (RegResPack) p;
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,regResP.status);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,regResP.result);
                    context.put(ProtocolPacket.PACKET_AREAID_NAME,regResP.areaId);
                    context.put(ProtocolPacket.PACKET_AREANAME_NAME,regResP.areaName);
                    context.put(ProtocolPacket.PACKET_TRANSFER_AREAID_NAME,regResP.transferAreaId);
                    context.put(ProtocolPacket.PACKET_SNAP_PORT_NAME,regResP.snapPort);
                    context.put(ProtocolPacket.PACKET_LISTEN_STATE_NAME,regResP.listenCallEnable);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_REQ:
                    InviteReqPack inviteReqP = (InviteReqPack)p;
                    context.put(ProtocolPacket.PACKET_CALLTYPE_NAME,inviteReqP.callType);
                    context.put(ProtocolPacket.PACKET_CALLDIRECT_NAME,inviteReqP.callDirect);
                    context.put(ProtocolPacket.PACKET_CODEC_NAME,inviteReqP.codec);
                    context.put(ProtocolPacket.PACKET_PTIME_NAME,inviteReqP.pTime);
                    context.put(ProtocolPacket.PACKET_SAMPLE_NAME,inviteReqP.sample);
                    context.put(ProtocolPacket.PACKET_CALLER_NAME,inviteReqP.caller);
                    context.put(ProtocolPacket.PACKET_CALLEE_NAME,inviteReqP.callee);
                    context.put(ProtocolPacket.PACKET_CALLERIP_MAME,inviteReqP.callerRtpIP);
                    context.put(ProtocolPacket.PACKET_CALLERPORT_NAME,inviteReqP.callerRtpPort);
                    context.put(ProtocolPacket.PACKET_CALLERTYPE_NAME,inviteReqP.callerType);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,inviteReqP.callID);
                    context.put(ProtocolPacket.PACKET_PATIENT_NAME_NAME,inviteReqP.patientName);
                    context.put(ProtocolPacket.PACKET_PATIENT_AGE_NAME,inviteReqP.patientAge);
                    context.put(ProtocolPacket.PACKET_ROOMID_NAME,inviteReqP.roomId);
                    context.put(ProtocolPacket.PACKET_BEDID_NAME,inviteReqP.bedName);
                    context.put(ProtocolPacket.PACKET_DEVICE_NAME_NAME,inviteReqP.deviceName);
                    context.put(ProtocolPacket.PACKET_AREAID_NAME,inviteReqP.areaId);
                    context.put(ProtocolPacket.PACKET_AREANAME_NAME,inviteReqP.areaName);
                    context.put(ProtocolPacket.PACKET_ISTRANSFER_NAME,inviteReqP.isTransfer);
                    context.put(ProtocolPacket.PACKET_AUTOANSWER_TIME_NAME,inviteReqP.autoAnswerTime);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_RES:
                    InviteResPack inviteResP = (InviteResPack)p;
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,inviteResP.status);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,inviteResP.result);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,inviteResP.callID);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.ANSWER_REQ:
                    AnswerReqPack answerReqP = (AnswerReqPack)p;
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,answerReqP.callID);
                    context.put(ProtocolPacket.PACKET_ANSWERER_NAME,answerReqP.answerer);
                    context.put(ProtocolPacket.PACKET_CALLEEIP_MAME,answerReqP.answererRtpIP);
                    context.put(ProtocolPacket.PACKET_CALLEEPORT_NAME,answerReqP.answererRtpPort);
                    context.put(ProtocolPacket.PACKET_BEDID_NAME,answerReqP.answerBedName);
                    context.put(ProtocolPacket.PACKET_CODEC_NAME,answerReqP.codec);
                    context.put(ProtocolPacket.PACKET_PTIME_NAME,answerReqP.pTime);
                    context.put(ProtocolPacket.PACKET_SAMPLE_NAME,answerReqP.sample);
                    context.put(ProtocolPacket.PACKET_CALLTYPE_NAME,answerReqP.callType);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.ANSWER_RES:
                    AnswerResPack answerResP = (AnswerResPack)p;
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,answerResP.status);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,answerResP.result);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,answerResP.callID);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.END_REQ:
                    EndReqPack endReqP = (EndReqPack)p;
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,endReqP.callID);
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,endReqP.endDevID);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.END_RES:
                    EndResPack endResP = (EndResPack)p;
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,endResP.status);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,endResP.result);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,endResP.callId);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_QUERY_REQ:
                    DevQueryReqPack devReqP = (DevQueryReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,devReqP.devid);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_QUERY_RES:
                    DevQueryResPack devResP = (DevQueryResPack)p;
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,devResP.status);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,devResP.result);
                    context.put(ProtocolPacket.PACKET_AREAID_NAME,devResP.areaId);
                    JSONArray listArray = new JSONArray();
                    for(PhoneDevice phone:devResP.phoneList){
                        JSONObject phoneJson = new JSONObject();
                        phoneJson.put(ProtocolPacket.PACKET_DEVID_NAME,phone.id);
                        phoneJson.put(ProtocolPacket.PACKET_DEVTYPE_NAME,phone.type);
                        phoneJson.put(ProtocolPacket.PACKET_STATUS_NAME,phone.isReg);
                        phoneJson.put(ProtocolPacket.PACKET_BEDID_NAME,phone.bedName);
                        listArray.add(phoneJson);
                    }
                    context.put(ProtocolPacket.PACKET_DETAIL_NAME,listArray);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_UPDATE_REQ:
                    UpdateReqPack updateReqP =(UpdateReqPack)p;
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,updateReqP.callId);
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,updateReqP.devId);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_UPDATE_RES:
                    UpdateResPack updateResP = (UpdateResPack)p;
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,updateResP.status);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,updateResP.result);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,updateResP.callid);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_REQ:
                    SystemConfigReqPack systemConfigReqP = (SystemConfigReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,systemConfigReqP.devId);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_CONFIG_REQ:
                    ConfigReqPack configReqP = (ConfigReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,configReqP.devId);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_CONFIG_RES:
                    ConfigResPack configResP = (ConfigResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,configResP.devId);
                    JSONArray paramArray = new JSONArray();
                    for(ConfigItem item:configResP.params){
                        JSONObject itemJson = new JSONObject();
                        itemJson.put(ProtocolPacket.PACKET_PARAM_ID_NAME,item.param_id);
                        itemJson.put(ProtocolPacket.PACKET_PARAM_NAME_NAME,item.param_name);
                        itemJson.put(ProtocolPacket.PACKET_PARAM_VALUE_NAME,item.param_value);
                        itemJson.put(ProtocolPacket.PACKET_PARAM_UNIT_NAME,item.param_unit);
                        paramArray.add(itemJson);
                    }
                    context.put(ProtocolPacket.PACKET_PARAMS_NAME,paramArray);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_RES:
                    SystemConfigResPack systemConfigResP = (SystemConfigResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,systemConfigResP.devId);
                    JSONArray systemParamArray = new JSONArray();
                    for(ConfigItem item:systemConfigResP.params){
                        JSONObject itemJson = new JSONObject();
                        itemJson.put(ProtocolPacket.PACKET_PARAM_ID_NAME,item.param_id);
                        itemJson.put(ProtocolPacket.PACKET_PARAM_NAME_NAME,item.param_name);
                        itemJson.put(ProtocolPacket.PACKET_PARAM_VALUE_NAME,item.param_value);
                        itemJson.put(ProtocolPacket.PACKET_PARAM_UNIT_NAME,item.param_unit);
                        systemParamArray.add(itemJson);
                    }
                    context.put(ProtocolPacket.PACKET_PARAMS_NAME,systemParamArray);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_TRANSFER_REQ:
                    TransferReqPack transferReqP = (TransferReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,transferReqP.devID);
                    context.put(ProtocolPacket.PACKET_TRANSFER_STATE_NAME,transferReqP.transferEnabled);
                    context.put(ProtocolPacket.PACKET_TRANSFER_AREAID_NAME,transferReqP.transferAreaId);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_TRANSFER_RES:
                    TransferResPack transferResP = (TransferResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,transferResP.devId);
                    context.put(ProtocolPacket.PACKET_TRANSFER_STATE_NAME,transferResP.state);
                    context.put(ProtocolPacket.PACKET_TRANSFER_AREAID_NAME,transferResP.transferAreaId);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,transferResP.result);
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,transferResP.status);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_LISTEN_REQ:
                    ListenCallReqPack listenReqP = (ListenCallReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,listenReqP.devID);
                    context.put(ProtocolPacket.PACKET_LISTEN_STATE_NAME,listenReqP.listenEnable);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_LISTEN_RES:
                    ListenCallResPack listenResP = (ListenCallResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,listenResP.devId);
                    context.put(ProtocolPacket.PACKET_LISTEN_STATE_NAME,listenResP.state);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,listenResP.result);
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,listenResP.status);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_LISTEN_CLEAR_REQ:
                    ListenClearReqPack listenClearReqP = (ListenClearReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,listenClearReqP.devID);
                    context.put(ProtocolPacket.PACKET_LISTEN_STATE_NAME,listenClearReqP.status);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_LISTEN_CLEAR_RES:
                    ListenClearResPack listenClearResP = (ListenClearResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,listenClearResP.devId);
                    context.put(ProtocolPacket.PACKET_LISTEN_STATE_NAME,listenClearResP.state);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,listenClearResP.result);
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,listenClearResP.status);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_VIDEO_INVITE_REQ:
                    StartVideoReqPack startReqP = (StartVideoReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,startReqP.startVideoDevId);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,startReqP.callID);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_VIDEO_INVITE_RES:
                    StartVideoResPack startResP = (StartVideoResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,startResP.startVideoDevId);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,startResP.callid);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,startResP.result);
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,startResP.status);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_VIDEO_ANSWER_REQ:
                    AnswerVideoReqPack answerVideoReqP = (AnswerVideoReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,answerVideoReqP.answerDevId);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,answerVideoReqP.callId);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_VIDEO_ANSWER_RES:
                    AnswerVideoResPack answerVideoResP = (AnswerVideoResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,answerVideoResP.answerVideoDevId);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,answerVideoResP.callId);
                    context.put(ProtocolPacket.PACKET_RESULT_NAME,answerVideoResP.result);
                    context.put(ProtocolPacket.PACKET_STATUS_NAME,answerVideoResP.status);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_VIDEO_END_REQ:
                    StopVideoReqPack stopVideoReqP = (StopVideoReqPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,stopVideoReqP.stopVideoDevId);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,stopVideoReqP.callID);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_VIDEO_END_RES:
                    StopVideoResPack stopReqP = (StopVideoResPack)p;
                    context.put(ProtocolPacket.PACKET_DEVID_NAME,stopReqP.stopVideoDevId);
                    context.put(ProtocolPacket.PACKET_CALLID_NAME,stopReqP.callid);
                    json.put(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        data = json.toString();       
        json.clear();
        return data;
    }

    private static void PutDefaultData(ProtocolPacket packet,JSONObject json) throws JSONException {
        packet.type = json.getIntValue(ProtocolPacket.PACKET_TYPE_NAME);
        packet.msgID = JsonPort.GetJsonString(json,ProtocolPacket.PACKET_MSGID_NAME);
        packet.sender = JsonPort.GetJsonString(json,ProtocolPacket.PACKET_SENDERID_NAME);
        packet.receiver = JsonPort.GetJsonString(json,ProtocolPacket.PACKET_RECEIVERID_NAME);
    }
}
