package com.atguigu.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.GmallConstants
import com.atguigu.bean.StartUpLog
import com.atguigu.handler.DauHandler2
import com.atguigu.utils.MyKafkaUtil
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object RealTimeDAU2 {
  def main(args: Array[String]): Unit = {
    //日期格式化
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
    val conf: SparkConf = new SparkConf().setAppName("RealTimeDAU2").setMaster("local[*]")
    val ssc = new StreamingContext(conf,Seconds(5))

    //读取kafka数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc,Set(GmallConstants.KAFKA_TOPIC_STARTUP))
    //将kafka数据转换为样例类
    val startUpLogDStream: DStream[StartUpLog] = kafkaDStream.map {
      case (_, value) => {
        //将字符串转换成对象
        val startUpLog: StartUpLog = JSON.parseObject(value, classOf[StartUpLog])
        val date: String = sdf.format(new Date(startUpLog.ts))
        val splits: Array[String] = date.split(" ")
        startUpLog.logDate = splits(0)
        startUpLog.logHour = splits(1)
        startUpLog
      }
    }

    //批次间过滤
    val filterByRedisDStream:DStream[StartUpLog] = DauHandler2.filterByRedis(startUpLogDStream,ssc)

    //批次内过滤
    val filterByBatch: DStream[StartUpLog] = DauHandler2.filterDataByBatch(filterByRedisDStream)

    //做缓存
    filterByBatch.cache()

    //将数据的mid写进redis重复集合中
    DauHandler2.saveMidToRedis(filterByBatch)

    //测试
    startUpLogDStream.count().print()
    filterByRedisDStream.count().print()
    filterByBatch.count().print()

    //将数据写入到phoenix中
    import org.apache.phoenix.spark._
    filterByBatch.foreachRDD(rdd=>{
      rdd.saveToPhoenix("GMALL_DAU", Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"), new Configuration, Some("hadoop111,hadoop112,hadoop113:2181"))
    })

    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
