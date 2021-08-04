package com.example.nettytest.pub.transaction;

import com.alibaba.fastjson.*;
import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.SystemSnap;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TransManager {

    protected final HashMap<String, Transaction> transLists;

    public void TransTimerProcess(){
        synchronized (TransManager.class) {
            for(Iterator<Map.Entry<String, Transaction>> it = transLists.entrySet().iterator(); it.hasNext();){
                Map.Entry<String, Transaction>item = it.next();
                Transaction trans = item.getValue();
                trans.liveTime++;
                switch(trans.state){
                    case Transaction.TRANSCATION_STATE_REQUIRING:
                        if(trans.liveTime>= Transaction.TRANSCATION_REQUIRING_TIME) {
                            trans.state = Transaction.TRANSCATION_STATE_FINISHED;
                            if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Dev %s Send Req %s Timeover",trans.devID,ProtocolPacket.GetTypeName(trans.reqPacket.type));
                            else
                                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Server Send Req %s to DEV %s Timeover",ProtocolPacket.GetTypeName(trans.reqPacket.type),trans.devID);
                            TransactionTimeOver(trans.reqPacket);
                        }else if(trans.liveTime% Transaction.TRANSCATION_RESEND_INTERVAL==0){
                            if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_INFO,"Dev %s Resend Req %s to Server",trans.devID,ProtocolPacket.GetTypeName(trans.reqPacket.type));
                            else if(trans.direction == Transaction.TRANSCATION_DIRECTION_S2C)
                                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_INFO,"Server Resend Req %s to Dev %s ",ProtocolPacket.GetTypeName(trans.reqPacket.type),trans.devID);
                                ByteBuf buf  = Unpooled.copiedBuffer(ProtocolFactory.PacketData(trans.reqPacket).getBytes(),"\r\n".getBytes());
                                SendTransactionBuf(trans.devID,buf);
//                                String sendData = ProtocolFactory.PacketData(trans.reqPacket)+"\r\n";
//                                SendTransactionBuf(trans.devID,sendData);
                        }
                        break;
                    case Transaction.TRANSCATION_STATE_WAITRELEASE:
                        if(trans.liveTime>=Transaction.TRANSCATION_REQUIRING_TIME)
                            trans.state = Transaction.TRANSCATION_STATE_FINISHED;
                        break;
                    case Transaction.TRANSCATION_STATE_RESPONDING:
                        if(trans.liveTime> Transaction.TRANSCATION_RESPONDING_TIME)
                            trans.state = Transaction.TRANSCATION_STATE_FINISHED;
                        break;
                    case Transaction.TRANSCATION_STATE_FINISHED:
                        if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                            LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_VERBOSE,"Dev %s Release Tran Req %s",trans.devID,ProtocolPacket.GetTypeName(trans.reqPacket.type));
                        else if(trans.direction == Transaction.TRANSCATION_DIRECTION_S2C)
                            LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_VERBOSE,"Server Release Tran Req %s ",ProtocolPacket.GetTypeName(trans.reqPacket.type));
                        if(trans.reqPacket!=null)
                            trans.reqPacket.Release();
                        trans.reqPacket = null;
                        if(trans.resPacket!=null)
                            trans.resPacket.Release();
                        trans.resPacket = null;
                        it.remove();
                        break;
                }
            }

        }
    }


    public TransManager() {
        transLists = new HashMap<>();
    }

    public boolean RemoveTransaction(String id){
        boolean result = false;
        synchronized (TransManager.class) {
            Transaction trans = transLists.remove(id);
            if(trans!=null){
//                if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
//                    LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_TEMP_DBG,"Dev %s Directly Remove Tran Req %s ",trans.devID,ProtocolPacket.GetTypeName(trans.reqPacket.type));
//                else if(trans.direction == Transaction.TRANSCATION_DIRECTION_S2C)
//                    LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_TEMP_DBG,"Server Directly Remove Tran Req %s ",ProtocolPacket.GetTypeName(trans.reqPacket.type));
                if(trans.reqPacket!=null)
                    trans.reqPacket.Release();
                trans.reqPacket = null;
                if(trans.resPacket!=null)
                    trans.resPacket.Release();
                trans.resPacket = null;
                result = true;
            }
        }

        return result;
   }

    public int GetTransCount(){
        int count;
        synchronized (TransManager.class){
            count = transLists.size();
        }
        return count;
    }


    public boolean AddTransaction(String msgID, Transaction tran){
        synchronized (TransManager.class) {
            boolean result;
            result =  AddTransaction_in(msgID,tran);
            return result;
        }
    }

    private boolean AddTransaction_in(String msgID, Transaction tran){
        transLists.put(msgID,tran);
        if(tran.state == Transaction.TRANSCATION_STATE_REQUIRING) {
            if(tran.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Add %s to Server",tran.devID,ProtocolPacket.GetTypeName(tran.reqPacket.type));
            else if(tran.direction==Transaction.TRANSCATION_DIRECTION_S2C)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Add %s to DEV %s",ProtocolPacket.GetTypeName(tran.reqPacket.type),tran.devID);
                ByteBuf buf = Unpooled.wrappedBuffer(ProtocolFactory.PacketData(tran.reqPacket).getBytes(),"\r\n".getBytes());
                SendTransactionBuf(tran.devID,buf);
//                String sendData = ProtocolFactory.PacketData(tran.reqPacket)+"\r\n";
//                SendTransactionBuf(tran.devID,sendData);

        }else if(tran.state == Transaction.TRANSCATION_STATE_RESPONDING){
            if(tran.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv %s and Add %s to Server",tran.devID,ProtocolPacket.GetTypeName(tran.reqPacket.type),ProtocolPacket.GetTypeName(tran.resPacket.type));
            else if(tran.direction == Transaction.TRANSCATION_DIRECTION_S2C)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Recv %s and Add %s to DEV %s",ProtocolPacket.GetTypeName(tran.reqPacket.type),ProtocolPacket.GetTypeName(tran.resPacket.type),tran.devID);
                ByteBuf buf = Unpooled.wrappedBuffer(ProtocolFactory.PacketData(tran.resPacket).getBytes(),"\r\n".getBytes());
                SendTransactionBuf(tran.devID,buf);
//                String sendData = ProtocolFactory.PacketData(tran.resPacket)+"\r\n";
//                SendTransactionBuf(tran.devID,sendData);
        }
        return true;
    }

    public void ProcessPacket(ProtocolPacket packet){
        if(packet==null){
            return ;
        }
        if(packet.msgID==null){
            return ;
        }
        synchronized (TransManager.class){
            Transaction trans = transLists.get(packet.msgID);
            if(trans!=null){
                if(trans.state== Transaction.TRANSCATION_STATE_RESPONDING) {
                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_S2C)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_INFO,"Server Rercev %s and Resend %s to Dev %s ",ProtocolPacket.GetTypeName(packet.type),ProtocolPacket.GetTypeName(trans.resPacket.type),trans.devID);
                    else if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_INFO,"Dev %s Rerecv %s and Resend %s to Server ",trans.devID,ProtocolPacket.GetTypeName(packet.type),ProtocolPacket.GetTypeName(trans.resPacket.type));
                        ByteBuf buf = Unpooled.wrappedBuffer(ProtocolFactory.PacketData(trans.resPacket).getBytes(),"\r\n".getBytes());
                        SendTransactionBuf(trans.devID,buf);
//                        String sendData = ProtocolFactory.PacketData(trans.resPacket)+"\r\n";
//                        SendTransactionBuf(trans.devID,sendData);
                }else if(trans.state== Transaction.TRANSCATION_STATE_REQUIRING) {
                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_S2C)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Recv %s from DEV %s",ProtocolPacket.GetTypeName(packet.type),trans.devID);
                    else if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv %s from Server",trans.devID,ProtocolPacket.GetTypeName(packet.type));
                    TransactionResRecv(packet);
                    trans.state = Transaction.TRANSCATION_STATE_WAITRELEASE;
                }else {
                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_S2C){
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_INFO,"DEV %s Rerecv %s when Transaction state is %s ",trans.devID,ProtocolPacket.GetTypeName(packet.type),Transaction.GetStateName(trans.state));
                    }else{
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_INFO,"Server Rerecv %s when Transaction state is %s Fro Dev %s",ProtocolPacket.GetTypeName(packet.type),Transaction.GetStateName(trans.state),trans.devID);
                    }
                }
            }else{
                if(packet.type < ProtocolPacket.MAX_REQ_TYPE){
                    TransactionReqRecv(packet);
                }else{
                    LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_ERROR,"Recv Unexcept %d(%s) From %s to %s, but could not find Transaction",packet.type,ProtocolPacket.GetTypeName(packet.type),packet.sender,packet.receiver);
                }
            }
        }
    }


    public void TransactionTimeOver(ProtocolPacket packet) {
    }

    public void TransactionReqRecv(ProtocolPacket packet) {

    }

    public void TransactionResRecv(ProtocolPacket packet) {

    }

    public void SendTransactionBuf(String ID,ByteBuf buf){

    }

    public void SendTransactionBuf(String ID,String data){

    }

    private ArrayList<TransactionInfo> GetTransactionsInfo(){
        ArrayList<TransactionInfo> list = new ArrayList<>();
        synchronized (TransManager.class){
            for(Transaction trans:transLists.values()){
                TransactionInfo info = new TransactionInfo();
                info.msgId = trans.reqPacket.msgID;
                info.type = trans.reqPacket.type;
                info.sender = trans.reqPacket.sender;
                info.receiver = trans.reqPacket.receiver;
                info.devId = trans.devID;
                list.add(info);
            }
        }
        return list;
    }

    public ArrayList<byte[]>





    GetTransactionDetail(int cmdType){
        ArrayList<byte[]> resList = new ArrayList<>();
        int iCount = 0;
        byte[] res;
        JSONObject resourceInfo = null;
        JSONArray transArray = null;
        ArrayList<TransactionInfo> transInfo = GetTransactionsInfo();
        try {
            for(TransactionInfo info:transInfo){
                if((iCount%10)==0){
                    if(resourceInfo!=null){
                        res = resourceInfo.toString().getBytes();
                        resList.add(res);
                    }
                    resourceInfo = new JSONObject();
                    resourceInfo.put(SystemSnap.SNAP_CMD_TYPE_NAME, cmdType);
                    transArray = new JSONArray();
                    resourceInfo.put(SystemSnap.SNAP_TRANS_NAME,transArray);
                }
                JSONObject tranJson = new JSONObject();
                tranJson.put(SystemSnap.SNAP_DEVID_NAME,info.devId);
                tranJson.put(SystemSnap.SNAP_MSGID_NAME,info.msgId);
                tranJson.put(SystemSnap.SNAP_SENDER_NAME,info.sender);
                tranJson.put(SystemSnap.SNAP_RECEIVER_NAME,info.receiver);
                tranJson.put(SystemSnap.SNAP_TRANSTYPE_NAME,info.type);
                transArray.add(tranJson);
                iCount++;
            }

            if(transArray != null){
                if(transArray.size()>0){
                    res = resourceInfo.toString().getBytes();
                    resList.add(res);
                }
            }

            if(resourceInfo!=null){
                resourceInfo.clear();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resList;
    }
}
