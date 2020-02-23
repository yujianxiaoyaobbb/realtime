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

public class CanalClient3 {
    public static void main(String[] args) throws InvalidProtocolBufferException {
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(new InetSocketAddress("hadoop111", 11111), "example", "", "");
        while(true){
            //获得连接，注册
            canalConnector.connect();
            canalConnector.subscribe("gmall.*");
            //拉取数据
            Message message = canalConnector.get(100);
            //判断是否有sql查询
            if(message.getEntries().size() <= 0){
                System.out.println("没有数据");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                for (CanalEntry.Entry entry : message.getEntries()) {
                    //判断是否是rowdata
                    if(CanalEntry.EntryType.ROWDATA.equals(entry.getEntryType())){
                        //获取表名
                        String tableName = entry.getHeader().getTableName();
                        //获取行数据
                        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                        //获取事件类型
                        CanalEntry.EventType eventType = rowChange.getEventType();
                        //获取行数据集合
                        List<CanalEntry.RowData> rowDatasList = rowChange.getRowDatasList();
                        handller(tableName,eventType,rowDatasList);
                    }
                }
            }
        }
    }

    private static void handller(String tableName, CanalEntry.EventType eventType, List<CanalEntry.RowData> rowDatasList) {
        if("order_info".equals(tableName) && CanalEntry.EventType.INSERT.equals(eventType)){
            for (CanalEntry.RowData rowData : rowDatasList) {
                //转换成JSON
                JSONObject jsonObject = new JSONObject();
                for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                    jsonObject.put(column.getName(),column.getValue());
                }
                //打印
                System.out.println(jsonObject);
                //写到kafka里
                KafkaSender.send(GmallConstants.GMALL_ORDER_INFO_TOPIC,jsonObject.toJSONString());
            }
        }
    }
}
