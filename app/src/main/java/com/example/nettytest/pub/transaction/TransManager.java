package com.example.nettytest.pub.transaction;

import com.example.nettytest.pub.LogWork;
import com.example.nettytest.pub.protocol.ProtocolFactory;
import com.example.nettytest.pub.protocol.ProtocolPacket;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class TransManager {

    protected final HashMap<String, Transaction> transLists;

    public TransManager() {
        transLists = new HashMap<>();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (transLists) {
                    for(Iterator<Map.Entry<String, Transaction>> it = transLists.entrySet().iterator(); it.hasNext();){
                        Map.Entry<String, Transaction>item = it.next();
                        Transaction trans = item.getValue();
                        trans.liveTime++;
                        switch(trans.state){
                            case Transaction.TRANSCATION_STATE_REQUIRING:
                                if(trans.liveTime>= Transaction.TRANSCATION_REQUIRING_TIME) {
                                    trans.state = Transaction.TRANSCATION_STATE_FINISHED;
                                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Dev %s send Req %s Timeover",trans.devID,ProtocolPacket.GetTypeName(trans.reqPacket.type));
                                    else
                                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Server send Req %s to DEV %s Timeover",ProtocolPacket.GetTypeName(trans.reqPacket.type),trans.devID);
                                    TransactionTimeOver(trans.reqPacket);
                                }else if(trans.liveTime% Transaction.TRANSCATION_RESEND_INTERVAL==0){
                                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Dev %s Resend Req %s to Server",trans.devID,ProtocolPacket.GetTypeName(trans.reqPacket.type));
                                    else if(trans.direction == Transaction.TRANSCATION_DIRECTION_S2C)
                                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Server Resend Req %s to Dev %s ",ProtocolPacket.GetTypeName(trans.reqPacket.type),trans.devID);
                                    ByteBuf buf  = Unpooled.copiedBuffer(ProtocolFactory.PacketData(trans.reqPacket).getBytes(),"\r\n".getBytes());
                                    SendTransactionBuf(trans.devID,buf);
                                }
                                break;
                            case Transaction.TRANSCATION_STATE_RESPONDING:
                                trans.liveTime++;
                                if(trans.liveTime> Transaction.TRANSCATION_RESPONDING_TIME)
                                    trans.state = Transaction.TRANSCATION_STATE_FINISHED;
                                break;
                            case Transaction.TRANSCATION_STATE_FINISHED:
                                it.remove();
                                break;
                        }
                    }

                }
            }
        },0,1000);
    }


    public boolean AddTransaction(String msgID, Transaction tran){
        synchronized (transLists) {
            boolean result;
            result =  AddTransaction_in(msgID,tran);
            return result;
        }
    }

    private boolean AddTransaction_in(String msgID, Transaction tran){
        transLists.put(msgID,tran);
        if(tran.state == Transaction.TRANSCATION_STATE_REQUIRING) {
            if(tran.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Send %s to Server",tran.devID,ProtocolPacket.GetTypeName(tran.reqPacket.type));
            else if(tran.direction==Transaction.TRANSCATION_DIRECTION_S2C)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Send %s to DEV %s",ProtocolPacket.GetTypeName(tran.reqPacket.type),tran.devID);
            ByteBuf buf = Unpooled.wrappedBuffer(ProtocolFactory.PacketData(tran.reqPacket).getBytes(),"\r\n".getBytes());
            SendTransactionBuf(tran.devID,buf);

        }else if(tran.state == Transaction.TRANSCATION_STATE_RESPONDING){
            if(tran.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Send %s to Server",tran.devID,ProtocolPacket.GetTypeName(tran.resPacket.type));
            else if(tran.direction == Transaction.TRANSCATION_DIRECTION_S2C)
                LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Send %s to DEV %s",ProtocolPacket.GetTypeName(tran.resPacket.type),tran.devID);
            ByteBuf buf = Unpooled.wrappedBuffer(ProtocolFactory.PacketData(tran.resPacket).getBytes(),"\r\n".getBytes());
            SendTransactionBuf(tran.devID,buf);
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
        synchronized (transLists){
            Transaction trans = transLists.get(packet.msgID);
            if(trans!=null){
                if(trans.state== Transaction.TRANSCATION_STATE_RESPONDING) {
                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_S2C)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Server Rercev %s and Resend %s to Dev %s ",ProtocolPacket.GetTypeName(packet.type),ProtocolPacket.GetTypeName(trans.resPacket.type),trans.devID);
                    else if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_WARN,"Dev %s Rerecv %s and Resend %s to Server ",trans.devID,ProtocolPacket.GetTypeName(packet.type),ProtocolPacket.GetTypeName(trans.resPacket.type));
                    ByteBuf buf = Unpooled.wrappedBuffer(ProtocolFactory.PacketData(trans.resPacket).getBytes(),"\r\n".getBytes());
                    SendTransactionBuf(trans.devID,buf);
                }else if(trans.state== Transaction.TRANSCATION_STATE_REQUIRING) {
                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_S2C)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Recv %s from DEV %s",ProtocolPacket.GetTypeName(packet.type),trans.devID);
                    else if(trans.direction==Transaction.TRANSCATION_DIRECTION_C2S)
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Recv %s from Server",trans.devID,ProtocolPacket.GetTypeName(packet.type));
                    TransactionResRecv(packet);
                    trans.state = Transaction.TRANSCATION_STATE_WAITRELEASE;
                }else {
                    if(trans.direction==Transaction.TRANSCATION_DIRECTION_S2C){
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"DEV %s Rerecv %s when Transaction state is %s ",trans.devID,ProtocolPacket.GetTypeName(packet.type),Transaction.GetStateName(trans.state));
                    }else{
                        LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_DEBUG,"Server Rerecv %s when Transaction state is %s for Dev %d",ProtocolPacket.GetTypeName(packet.type),Transaction.GetStateName(trans.state),trans.devID);
                    }
                }
            }else{
                if(packet.type < ProtocolPacket.MAX_REQ_TYPE){
                    TransactionReqRecv(packet);
                }else{
                    LogWork.Print(LogWork.TRANSACTION_MODULE,LogWork.LOG_ERROR,"Recv %s From %s to %s, but could not find Transaction",ProtocolPacket.GetTypeName(packet.type),packet.sender,packet.receiver);
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
}
