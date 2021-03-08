package com.example.nettytest.pub.protocol;

import com.example.nettytest.pub.commondevice.PhoneDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

public class ProtocolFactory {

    public static ProtocolPacket ParseData(ByteBuf data) {
        ProtocolPacket p = null;
        try {
            JSONObject json = new JSONObject(data.toString(CharsetUtil.UTF_8));
            JSONObject context;
            int type = json.getInt(ProtocolPacket.PACKET_TYPE_NAME);
            switch(type){
                case ProtocolPacket.REG_REQ:
                    RegReqPack regReqPack = new RegReqPack();
                    PutDefaultData(regReqPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    regReqPack.address = context.optString(ProtocolPacket.PACKET_ADDRESS_NAME);
                    regReqPack.devID = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    regReqPack.expireTime = context.optInt(ProtocolPacket.PACKET_EXPIRE_NAME);
                    regReqPack.devType = context.optInt(ProtocolPacket.PACKET_DEVTYPE_NAME);
                    p = regReqPack;
                    break;
                case ProtocolPacket.REG_RES:
                    RegResPack regResPack = new RegResPack();
                    PutDefaultData(regResPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    regResPack.status = context.optInt(ProtocolPacket.PACKET_STATUS_NAME);
                    regResPack.result = context.optString(ProtocolPacket.PACKET_RESULT_NAME);
                    p = regResPack;
                    break;
                case ProtocolPacket.CALL_REQ:
                    InviteReqPack inviteReqPack = new InviteReqPack();
                    PutDefaultData(inviteReqPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    inviteReqPack.callType = context.optInt(ProtocolPacket.PACKET_CALLTYPE_NAME);
                    inviteReqPack.callDirect = context.optInt(ProtocolPacket.PACKET_CALLDIRECT_NAME);
                    inviteReqPack.callID = context.optString(ProtocolPacket.PACKET_CALLID_NAME);

                    inviteReqPack.caller = context.optString(ProtocolPacket.PACKET_CALLER_NAME);
                    inviteReqPack.callee = context.optString(ProtocolPacket.PACKET_CALLEE_NAME);
                    inviteReqPack.callerType = context.optInt(ProtocolPacket.PACKET_CALLERTYPE_NAME);

                    inviteReqPack.codec = context.optInt(ProtocolPacket.PACKET_CODEC_NAME);
                    inviteReqPack.callerRtpIP = context.optString(ProtocolPacket.PACKET_CALLERIP_MAME);
                    inviteReqPack.callerRtpPort = context.optInt(ProtocolPacket.PACKET_CALLERPORT_NAME);

                    inviteReqPack.patientName = context.optString(ProtocolPacket.PACKET_PATIENT_NAME_NAME);
                    inviteReqPack.patientAge = context.optString(ProtocolPacket.PACKET_PATIENT_AGE_NAME);
                    inviteReqPack.bedName = context.optString(ProtocolPacket.PACKET_BEDID_NAME);

                    inviteReqPack.deviceName = context.optString(ProtocolPacket.PACKET_DEVICE_NAME_NAME);
                    inviteReqPack.roomId = context.optString(ProtocolPacket.PACKET_ROOMID_NAME);

                    inviteReqPack.pTime = context.optInt(ProtocolPacket.PACKET_PTIME_NAME);
                    inviteReqPack.codec = context.optInt(ProtocolPacket.PACKET_CODEC_NAME);
                    inviteReqPack.sample = context.optInt(ProtocolPacket.PACKET_SAMPLE_NAME);
                    inviteReqPack.autoAnswerTime = context.optInt(ProtocolPacket.PACKET_AUTOANSWER_TIME_NAME);

                    p = inviteReqPack;
                    break;
                case  ProtocolPacket.CALL_RES:
                    InviteResPack inviteResPack = new InviteResPack();
                    PutDefaultData(inviteResPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);

                    inviteResPack.status = context.optInt(ProtocolPacket.PACKET_STATUS_NAME);
                    inviteResPack.result = context.optString(ProtocolPacket.PACKET_RESULT_NAME);

                    inviteResPack.callID = context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    p = inviteResPack;
                    break;
                case ProtocolPacket.END_REQ:
                    EndReqPack endReqPack = new EndReqPack();
                    PutDefaultData(endReqPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    endReqPack.endDevID = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    endReqPack.callID = context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    p = endReqPack;
                    break;
                case ProtocolPacket.END_RES:
                    EndResPack endResPack = new EndResPack();
                    PutDefaultData(endResPack,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    endResPack.status = context.optInt(ProtocolPacket.PACKET_STATUS_NAME);
                    endResPack.result = context.optString(ProtocolPacket.PACKET_RESULT_NAME);
                    endResPack.callId = context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    p = endResPack;
                    break;
                case ProtocolPacket.ANSWER_REQ:
                    AnswerReqPack answerReqP = new AnswerReqPack();
                    PutDefaultData(answerReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    answerReqP.callID = context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    answerReqP.answerer = context.optString(ProtocolPacket.PACKET_ANSWERER_NAME);
                    answerReqP.answererRtpIP = context.optString(ProtocolPacket.PACKET_CALLEEIP_MAME);
                    answerReqP.answererRtpPort = context.optInt(ProtocolPacket.PACKET_CALLEEPORT_NAME);
                    answerReqP.answerBedName = context.optString(ProtocolPacket.PACKET_BEDID_NAME);
                    answerReqP.codec = context.optInt(ProtocolPacket.PACKET_CODEC_NAME);
                    answerReqP.pTime = context.optInt(ProtocolPacket.PACKET_PTIME_NAME);
                    answerReqP.sample= context.optInt(ProtocolPacket.PACKET_SAMPLE_NAME);
                    answerReqP.callType = context.optInt(ProtocolPacket.PACKET_CALLTYPE_NAME);
                    answerReqP.answerDeviceName = context.optString(ProtocolPacket.PACKET_DEVICE_NAME_NAME);
                    answerReqP.answerRoomId = context.optString(ProtocolPacket.PACKET_ROOMID_NAME);
                    p = answerReqP;
                    break;
                case ProtocolPacket.ANSWER_RES:
                    AnswerResPack answerResP = new AnswerResPack();
                    PutDefaultData(answerResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    answerResP.callID =  context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    answerResP.status = context.optInt(ProtocolPacket.PACKET_STATUS_NAME);
                    answerResP.result = context.optString(ProtocolPacket.PACKET_RESULT_NAME);
                    p = answerResP;
                    break;
                case ProtocolPacket.DEV_QUERY_REQ:
                    DevQueryReqPack devReqP = new DevQueryReqPack();
                    PutDefaultData(devReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    devReqP.devid = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    p = devReqP;
                    break;
                case ProtocolPacket.DEV_QUERY_RES:
                    DevQueryResPack devResP = new DevQueryResPack();
                    PutDefaultData(devResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    JSONArray phoneLists = context.getJSONArray(ProtocolPacket.PACKET_DETAIL_NAME);
                    if(phoneLists!=null) {
                        for (int iTmp = 0; iTmp < phoneLists.length(); iTmp++) {
                            JSONObject jsonObj = phoneLists.getJSONObject(iTmp);
                            PhoneDevice phone = new PhoneDevice();
                            phone.id = jsonObj.optString(ProtocolPacket.PACKET_DEVID_NAME);
                            phone.type = jsonObj.optInt(ProtocolPacket.PACKET_DEVTYPE_NAME);
                            phone.isReg = jsonObj.optBoolean(ProtocolPacket.PACKET_STATUS_NAME);
                            phone.bedName = jsonObj.optString(ProtocolPacket.PACKET_BEDID_NAME);
                            devResP.phoneList.add(phone);
                        }
                    }
                    p = devResP;
                    break;
                case ProtocolPacket.CALL_UPDATE_REQ:
                    UpdateReqPack updateReqP = new UpdateReqPack();
                    PutDefaultData(updateReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    updateReqP.callId = context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    updateReqP.devId = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    p = updateReqP;
                    break;
                case ProtocolPacket.CALL_UPDATE_RES:
                    UpdateResPack updateResP = new UpdateResPack();
                    PutDefaultData(updateResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    updateResP.status = context.optInt(ProtocolPacket.PACKET_STATUS_NAME);
                    updateResP.result = context.optString(ProtocolPacket.PACKET_RESULT_NAME);
                    updateResP.callid = context.optString(ProtocolPacket.PACKET_CALLID_NAME);
                    p = updateResP;
                    break;
                case ProtocolPacket.DEV_CONFIG_REQ:
                    ConfigReqPack configReqP = new ConfigReqPack();
                    PutDefaultData(configReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    configReqP.devId = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    p = configReqP;
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_REQ:
                    SystemConfigReqPack systemConfigReqP = new SystemConfigReqPack();
                    PutDefaultData(systemConfigReqP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    systemConfigReqP.devId = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    p = systemConfigReqP;
                    break;
                case ProtocolPacket.DEV_CONFIG_RES:
                    ConfigResPack configResP = new ConfigResPack();
                    PutDefaultData(configResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    configResP.devId = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    JSONArray paramList = context.getJSONArray(ProtocolPacket.PACKET_PARAMS_NAME);
                    if(paramList!=null){
                        for(int iTmp=0;iTmp<paramList.length();iTmp++){
                            JSONObject item = paramList.getJSONObject(iTmp);
                            ConfigItem param = new ConfigItem();
                            param.param_id = item.optString(ProtocolPacket.PACKET_PARAM_ID_NAME);
                            param.param_name = item.optString(ProtocolPacket.PACKET_PARAM_NAME_NAME);
                            param.param_value = item.optString(ProtocolPacket.PACKET_PARAM_VALUE_NAME);
                            param.param_unit = item.optString(ProtocolPacket.PACKET_PARAM_UNIT_NAME);
                            configResP.params.add(param);
                        }
                    }
                    p = configResP;
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_RES:
                    SystemConfigResPack systemConfigResP = new SystemConfigResPack();
                    PutDefaultData(systemConfigResP,json);
                    context = json.getJSONObject(ProtocolPacket.PACKET_CONTEXT_NAME);
                    systemConfigResP.devId = context.optString(ProtocolPacket.PACKET_DEVID_NAME);
                    JSONArray systemParamList = context.getJSONArray(ProtocolPacket.PACKET_PARAMS_NAME);
                    if(systemParamList!=null){
                        for(int iTmp=0;iTmp<systemParamList.length();iTmp++){
                            JSONObject item = systemParamList.getJSONObject(iTmp);
                            ConfigItem param = new ConfigItem();
                            param.param_id = item.optString(ProtocolPacket.PACKET_PARAM_ID_NAME);
                            param.param_name = item.optString(ProtocolPacket.PACKET_PARAM_NAME_NAME);
                            param.param_value = item.optString(ProtocolPacket.PACKET_PARAM_VALUE_NAME);
                            param.param_unit = item.optString(ProtocolPacket.PACKET_PARAM_UNIT_NAME);
                            systemConfigResP.params.add(param);
                        }
                    }
                    p = systemConfigResP;
                    break;
            }
        }catch (JSONException e){
            e.printStackTrace();
        }


        return p;
    }


    public static String PacketData(ProtocolPacket p){
        String data;
        JSONObject json = new JSONObject();
        JSONObject context = new JSONObject();
        try {
            json.putOpt(ProtocolPacket.PACKET_TYPE_NAME,p.type);
            json.putOpt(ProtocolPacket.PACKET_MSGID_NAME,p.msgID);
            json.putOpt(ProtocolPacket.PACKET_SENDERID_NAME,p.sender);
            json.putOpt(ProtocolPacket.PACKET_RECEIVERID_NAME,p.receiver);
            switch(p.type){
                case ProtocolPacket.REG_REQ:
                    RegReqPack regReqP = (RegReqPack) p;
                    context.putOpt(ProtocolPacket.PACKET_ADDRESS_NAME,regReqP.address);
                    context.putOpt(ProtocolPacket.PACKET_DEVTYPE_NAME,regReqP.devType);
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,regReqP.devID);
                    context.putOpt(ProtocolPacket.PACKET_EXPIRE_NAME,regReqP.expireTime);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.REG_RES:
                    RegResPack regResP = (RegResPack) p;
                    context.putOpt(ProtocolPacket.PACKET_STATUS_NAME,regResP.status);
                    context.putOpt(ProtocolPacket.PACKET_RESULT_NAME,regResP.result);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_REQ:
                    InviteReqPack inviteReqP = (InviteReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_CALLTYPE_NAME,inviteReqP.callType);
                    context.putOpt(ProtocolPacket.PACKET_CALLDIRECT_NAME,inviteReqP.callDirect);
                    context.putOpt(ProtocolPacket.PACKET_CODEC_NAME,inviteReqP.codec);
                    context.putOpt(ProtocolPacket.PACKET_PTIME_NAME,inviteReqP.pTime);
                    context.putOpt(ProtocolPacket.PACKET_SAMPLE_NAME,inviteReqP.sample);
                    context.putOpt(ProtocolPacket.PACKET_CALLER_NAME,inviteReqP.caller);
                    context.putOpt(ProtocolPacket.PACKET_CALLEE_NAME,inviteReqP.callee);
                    context.putOpt(ProtocolPacket.PACKET_CALLERIP_MAME,inviteReqP.callerRtpIP);
                    context.putOpt(ProtocolPacket.PACKET_CALLERPORT_NAME,inviteReqP.callerRtpPort);
                    context.putOpt(ProtocolPacket.PACKET_CALLERTYPE_NAME,inviteReqP.callerType);
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,inviteReqP.callID);
                    context.putOpt(ProtocolPacket.PACKET_PATIENT_NAME_NAME,inviteReqP.patientName);
                    context.putOpt(ProtocolPacket.PACKET_PATIENT_AGE_NAME,inviteReqP.patientAge);
                    context.putOpt(ProtocolPacket.PACKET_ROOMID_NAME,inviteReqP.roomId);
                    context.putOpt(ProtocolPacket.PACKET_BEDID_NAME,inviteReqP.bedName);
                    context.putOpt(ProtocolPacket.PACKET_DEVICE_NAME_NAME,inviteReqP.deviceName);
                    context.putOpt(ProtocolPacket.PACKET_AUTOANSWER_TIME_NAME,inviteReqP.autoAnswerTime);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_RES:
                    InviteResPack inviteResP = (InviteResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_STATUS_NAME,inviteResP.status);
                    context.putOpt(ProtocolPacket.PACKET_RESULT_NAME,inviteResP.result);
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,inviteResP.callID);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.ANSWER_REQ:
                    AnswerReqPack answerReqP = (AnswerReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,answerReqP.callID);
                    context.putOpt(ProtocolPacket.PACKET_ANSWERER_NAME,answerReqP.answerer);
                    context.putOpt(ProtocolPacket.PACKET_CALLEEIP_MAME,answerReqP.answererRtpIP);
                    context.putOpt(ProtocolPacket.PACKET_CALLEEPORT_NAME,answerReqP.answererRtpPort);
                    context.putOpt(ProtocolPacket.PACKET_BEDID_NAME,answerReqP.answerBedName);
                    context.putOpt(ProtocolPacket.PACKET_CODEC_NAME,answerReqP.codec);
                    context.putOpt(ProtocolPacket.PACKET_PTIME_NAME,answerReqP.pTime);
                    context.putOpt(ProtocolPacket.PACKET_SAMPLE_NAME,answerReqP.sample);
                    context.putOpt(ProtocolPacket.PACKET_CALLTYPE_NAME,answerReqP.callType);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.ANSWER_RES:
                    AnswerResPack answerResP = (AnswerResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_STATUS_NAME,answerResP.status);
                    context.putOpt(ProtocolPacket.PACKET_RESULT_NAME,answerResP.result);
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,answerResP.callID);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.END_REQ:
                    EndReqPack endReqP = (EndReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,endReqP.callID);
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,endReqP.endDevID);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.END_RES:
                    EndResPack endResP = (EndResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_STATUS_NAME,endResP.status);
                    context.putOpt(ProtocolPacket.PACKET_RESULT_NAME,endResP.result);
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,endResP.callId);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_QUERY_REQ:
                    DevQueryReqPack devReqP = (DevQueryReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,devReqP.devid);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_QUERY_RES:
                    DevQueryResPack devResP = (DevQueryResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_STATUS_NAME,devResP.status);
                    context.putOpt(ProtocolPacket.PACKET_RESULT_NAME,devResP.result);
                    JSONArray listArray = new JSONArray();
                    for(PhoneDevice phone:devResP.phoneList){
                        JSONObject phoneJson = new JSONObject();
                        phoneJson.putOpt(ProtocolPacket.PACKET_DEVID_NAME,phone.id);
                        phoneJson.putOpt(ProtocolPacket.PACKET_DEVTYPE_NAME,phone.type);
                        phoneJson.putOpt(ProtocolPacket.PACKET_STATUS_NAME,phone.isReg);
                        phoneJson.putOpt(ProtocolPacket.PACKET_BEDID_NAME,phone.bedName);
                        listArray.put(phoneJson);
                    }
                    context.putOpt(ProtocolPacket.PACKET_DETAIL_NAME,listArray);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_UPDATE_REQ:
                    UpdateReqPack updateReqP =(UpdateReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,updateReqP.callId);
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,updateReqP.devId);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.CALL_UPDATE_RES:
                    UpdateResPack updateResP = (UpdateResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_STATUS_NAME,updateResP.status);
                    context.putOpt(ProtocolPacket.PACKET_RESULT_NAME,updateResP.result);
                    context.putOpt(ProtocolPacket.PACKET_CALLID_NAME,updateResP.callid);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_REQ:
                    SystemConfigReqPack systemConfigReqP = (SystemConfigReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,systemConfigReqP.devId);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_CONFIG_REQ:
                    ConfigReqPack configReqP = (ConfigReqPack)p;
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,configReqP.devId);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.DEV_CONFIG_RES:
                    ConfigResPack configResP = (ConfigResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,configResP.devId);
                    JSONArray paramArray = new JSONArray();
                    for(ConfigItem item:configResP.params){
                        JSONObject itemJson = new JSONObject();
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_ID_NAME,item.param_id);
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_NAME_NAME,item.param_name);
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_VALUE_NAME,item.param_value);
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_UNIT_NAME,item.param_unit);
                        paramArray.put(itemJson);
                    }
                    context.putOpt(ProtocolPacket.PACKET_PARAMS_NAME,paramArray);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
                case ProtocolPacket.SYSTEM_CONFIG_RES:
                    SystemConfigResPack systemConfigResP = (SystemConfigResPack)p;
                    context.putOpt(ProtocolPacket.PACKET_DEVID_NAME,systemConfigResP.devId);
                    JSONArray systemParamArray = new JSONArray();
                    for(ConfigItem item:systemConfigResP.params){
                        JSONObject itemJson = new JSONObject();
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_ID_NAME,item.param_id);
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_NAME_NAME,item.param_name);
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_VALUE_NAME,item.param_value);
                        itemJson.putOpt(ProtocolPacket.PACKET_PARAM_UNIT_NAME,item.param_unit);
                        systemParamArray.put(itemJson);
                    }
                    context.putOpt(ProtocolPacket.PACKET_PARAMS_NAME,systemParamArray);
                    json.putOpt(ProtocolPacket.PACKET_CONTEXT_NAME,context);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        data = json.toString();
        return data;
    }

    private static void PutDefaultData(ProtocolPacket packet,JSONObject json) throws JSONException {
        packet.type = json.getInt(ProtocolPacket.PACKET_TYPE_NAME);
        packet.msgID = json.getString(ProtocolPacket.PACKET_MSGID_NAME);
        packet.sender = json.getString(ProtocolPacket.PACKET_SENDERID_NAME);
        packet.receiver = json.getString(ProtocolPacket.PACKET_RECEIVERID_NAME);
    }
}
