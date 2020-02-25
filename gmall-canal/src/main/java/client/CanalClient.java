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
import java.util.Random;


public class CanalClient {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        //获取canal连接器
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop111", 11111), "example", "", "");

        //抓取数据并解析
        while(true){
            //连接canal
            canalConnector.connect();
            //指定订阅的数据库
            canalConnector.subscribe("gmall.*");
            //抓取数据
            Message message = canalConnector.get(100);
            //判断当前抓取的是否有数据
            if(message.getEntries().size() <= 0){
                System.out.println("当前没有数据");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                List<CanalEntry.Entry> entries = message.getEntries();
                //只要对数据操作的内容
                for (CanalEntry.Entry entry : entries) {
                    if(CanalEntry.EntryType.ROWDATA.equals(entry.getEntryType())){
                        //反序列数据
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        //获取表名
                        String tableName = entry.getHeader().getTableName();
                        //取出事件类型
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        //处理
                        handler(tableName,eventType,rowChange);
                    }
                }
            }
        }
    }

    private static void handler(String tableName, CanalEntry.EventType eventType, CanalEntry.RowChange rowChange) {
        //判断是否是订单表并且是新增的
        if("order_info".equals(tableName) && CanalEntry.EventType.INSERT.equals(eventType)){
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                sendToKafka(rowData,GmallConstants.GMALL_ORDER_INFO_TOPIC);
            }
            //判断是订单明细表并且是新增
        }else if("order_detail".equals(tableName) && CanalEntry.EventType.INSERT.equals(eventType)){
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                sendToKafka(rowData,GmallConstants.GMALL_ORDER_DETAIL_TOPIC);
            }
            //判断是用户表并且是新增或修改
        }else if("user_info".equals(tableName) && (CanalEntry.EventType.INSERT.equals(eventType) || CanalEntry.EventType.UPDATE.equals(eventType))){
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                sendToKafka(rowData,GmallConstants.GMALL_USER_INFO_TOPIC);
            }
        }
    }

    private static void sendToKafka(CanalEntry.RowData rowData,String topic) {
        //创建Json对象
        JSONObject jsonObject = new JSONObject();
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            jsonObject.put(column.getName(),column.getValue());
        }
        System.out.println(jsonObject.toJSONString());
        try {
            Thread.sleep(new Random().nextInt(5) * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //将json写入kafka
        KafkaSender.send(topic,jsonObject.toJSONString());
    }
}
