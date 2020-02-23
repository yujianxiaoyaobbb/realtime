package com.atguigu.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.atguigu.GmallConstants
import com.atguigu.bean.OrderInfo
import com.atguigu.utils.MyKafkaUtil
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object RealTimeGMV3 {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeGMV3")
      .setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))
    //读取kafka的数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_ORDER_INFO_TOPIC))

    //将kafka数据封装成包装类
    val orderInfoDStream: DStream[OrderInfo] = kafkaDStream.map {
      case (_, value) => {
        val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
        //补全创建日期和创建小时
        val dateArr: Array[String] = orderInfo.create_time.split(" ")
        orderInfo.create_date = dateArr(0)
        orderInfo.create_hour = dateArr(1).split(":")(0)
        //对手机号进行脱敏
        orderInfo.consignee_tel = orderInfo.consignee_tel.charAt(3) + "*****" + orderInfo.consignee_tel.substring(7)
        orderInfo
      }
    }
    //缓存
    orderInfoDStream.cache()
    //打印测试
    orderInfoDStream.print()
    //写入到phoenix
    import org.apache.phoenix.spark._
    orderInfoDStream.foreachRDD(rdd=>{
      rdd.saveToPhoenix("GMALL_ORDER_INFO",Seq("ID", "PROVINCE_ID", "CONSIGNEE", "ORDER_COMMENT", "CONSIGNEE_TEL", "ORDER_STATUS", "PAYMENT_WAY", "USER_ID", "IMG_URL", "TOTAL_AMOUNT", "EXPIRE_TIME", "DELIVERY_ADDRESS", "CREATE_TIME", "OPERATE_TIME", "TRACKING_NO", "PARENT_ORDER_ID", "OUT_TRADE_NO", "TRADE_BODY", "CREATE_DATE", "CREATE_HOUR"),new Configuration(),Some("hadoop111,hadoop112,hadoop113:2181"))
    })

    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
