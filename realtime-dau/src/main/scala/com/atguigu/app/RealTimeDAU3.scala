package com.atguigu.app

import java.text.SimpleDateFormat
import java.util.Date

import com.alibaba.fastjson.JSON
import com.atguigu.GmallConstants
import com.atguigu.bean.StartUpLog
import com.atguigu.handler.{DauHandler, DauHandler3}
import com.atguigu.utils.MyKafkaUtil
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object RealTimeDAU3 {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeDAU3").setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
    //读取kafka数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.KAFKA_TOPIC_STARTUP))

    //将数据转换成样例类
    val startUpLogDStream: DStream[StartUpLog] = kafkaDStream.map {
      case (_, value) => {
        val startUpLog: StartUpLog = JSON.parseObject(value, classOf[StartUpLog])
        //补全logDate和logHour
        val date: String = sdf.format(new Date(startUpLog.ts))
        val dateArr: Array[String] = date.split(" ")
        startUpLog.logDate = dateArr(0)
        startUpLog.logHour = dateArr(1)
        startUpLog
      }
    }
    //    startUpLogDStream.print()
    //批次间去重
    val filterByRedisDStream: DStream[StartUpLog] = DauHandler3.filterByRedis(startUpLogDStream, ssc)

    //批次内去重
    val filterByBatch: DStream[StartUpLog] = DauHandler3.filterDataByBatch(filterByRedisDStream)

    //做缓存
    filterByBatch.cache()

    //将这一批次的数据的mid写入到redis的重复set中
    DauHandler3.saveMidToRedis(filterByBatch)
    //将数据写入到phoenix中
    import org.apache.phoenix.spark._
    filterByBatch.foreachRDD(rdd=>{
      rdd.saveToPhoenix("GMALL_DAU", Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"), new Configuration, Some("hadoop111,hadoop112,hadoop113:2181"))
    })

    //测试
    startUpLogDStream.count().print()
    filterByRedisDStream.count().print()
    filterByBatch.count().print()
    filterByBatch.print(100)
    //开始任务
    ssc.start()
    ssc.awaitTermination()
  }
}
