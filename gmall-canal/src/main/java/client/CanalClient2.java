package client;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.atguigu.GmallConstants;
import com.google.protobuf.InvalidProtocolBufferException;
import utils.KafkaSender;

import java.net.InetSocketAddress;
import java.util.List;

public class CanalClient2 {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop111", 11111), "example", "", "");
        while(true){
            canalConnector.connect();
            canalConnector.subscribe("gmall.*");
            Message message = canalConnector.get(100);
            if(message.getEntries().size() <= 0){
                System.out.println("没有数据");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                for (CanalEntry.Entry entry : message.getEntries()) {
                    if(CanalEntry.EntryType.ROWDATA.equals(entry.getEntryType())){
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        String tableName = entry.getHeader().getTableName();
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                        handler(tableName,eventType,rowDatasList);
                    }
                }
            }
        }
    }

    private static void handler(String tableName, CanalEntry.EventType eventType, List<CanalEntry.RowData> rowDatasList) {
        if("order_info".equals(tableName) && CanalEntry.EventType.INSERT.equals(eventType)){
            for (CanalEntry.RowData rowData : rowDatasList) {
                JSONObject jsonObject = new JSONObject();
                for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                    jsonObject.put(column.getName(),column.getValue());
                }
                System.out.println(jsonObject.toJSONString());
                KafkaSender.send(GmallConstants.GMALL_ORDER_INFO_TOPIC,jsonObject.toJSONString());
            }
        }
    }
}
