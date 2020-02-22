package com.atguigu.app

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.GmallConstants
import com.atguigu.bean.OrderInfo
import com.atguigu.utils.MyKafkaUtil
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}



object RealTimeGMV2 {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeGMV2").setMaster("local[*]")
    val ssc = new StreamingContext(conf,Seconds(5))

    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc,Set(GmallConstants.GMALL_ORDER_INFO_TOPIC))

    val orderInfoDStream: DStream[OrderInfo] = kafkaDStream.map {
      case (_, value) => {
        val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
        val timeArr: Array[String] = orderInfo.create_time.split(" ")
        orderInfo.create_date = timeArr(0)
        orderInfo.create_hour = timeArr(1).split(":")(0)
        orderInfo.consignee_tel = orderInfo.consignee_tel.splitAt(3) + "********"
        orderInfo
      }
    }

    orderInfoDStream.foreachRDD(rdd=>{
      import org.apache.phoenix.spark._
      rdd.saveToPhoenix("GMALL_ORDER_INFO",Seq("ID", "PROVINCE_ID", "CONSIGNEE", "ORDER_COMMENT", "CONSIGNEE_TEL", "ORDER_STATUS", "PAYMENT_WAY", "USER_ID", "IMG_URL", "TOTAL_AMOUNT", "EXPIRE_TIME", "DELIVERY_ADDRESS", "CREATE_TIME", "OPERATE_TIME", "TRACKING_NO", "PARENT_ORDER_ID", "OUT_TRADE_NO", "TRADE_BODY", "CREATE_DATE", "CREATE_HOUR"),new Configuration(),Some("hadoop111,hadoop112,hadoop113:2181"))
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
