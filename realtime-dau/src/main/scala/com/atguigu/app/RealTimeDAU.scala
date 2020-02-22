package com.atguigu.app

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.alibaba.fastjson.JSON
import com.atguigu.GmallConstants
import com.atguigu.bean.StartUpLog
import com.atguigu.handler.DauHandler
import com.atguigu.utils.{MyKafkaUtil, RedisUtil}
import org.apache.hadoop.conf.Configuration
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}


object RealTimeDAU {
  def main(args: Array[String]): Unit = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
    val conf: SparkConf = new SparkConf().setAppName("RealTimeDAU").setMaster("local[*]")

    //创建ssc
    val ssc = new StreamingContext(conf,Seconds(5))
    //读取kafka数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc,Set(GmallConstants.KAFKA_TOPIC_STARTUP))

    //将数据转换成样例类
    val startUpLogDStream: DStream[StartUpLog] = kafkaDStream.map {
      case (_, value) => {
        //将json字符串转换为json
        val jsonLog: StartUpLog = JSON.parseObject(value, classOf[StartUpLog])
        //获取ts
        val ts: Long = jsonLog.ts
        //用日期格式化
        val sdfDate: String = sdf.format(new Date(ts))
        val splits: Array[String] = sdfDate.split(" ")
        jsonLog.logDate = splits(0)
        jsonLog.logHour = splits(1)
        jsonLog
      }
    }
//    startUpLogDStream.print()
    //过滤之前数据中已经登陆过的用户数据
    val filterByRedisDStream:DStream[StartUpLog] = DauHandler.filterByRedis(startUpLogDStream,ssc)

//    filterByRedisDStream.print()
    //批次内进行过滤
    val filterByBatch: DStream[StartUpLog] = DauHandler.filterDataByBatch(filterByRedisDStream)

    //做缓存，复用
    filterByBatch.cache()

    //将过滤后的数据的mid放入重复mid表中
    DauHandler.saveMidToRedis(filterByBatch)

    //测试
   /* startUpLogDStream.count().print()
    filterByRedisDStream.count().print()
    filterByBatch.count().print()*/

    //将过滤后的数据写入phoenix
    import org.apache.phoenix.spark._
    filterByBatch.foreachRDD(rdd =>
      rdd.saveToPhoenix("GMALL_DAU", Seq("MID", "UID", "APPID", "AREA", "OS", "CH", "TYPE", "VS", "LOGDATE", "LOGHOUR", "TS"), new Configuration, Some("hadoop111,hadoop112,hadoop113:2181"))
    )
    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
