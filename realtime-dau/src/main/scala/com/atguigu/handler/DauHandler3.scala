package com.atguigu.handler

import java.text.SimpleDateFormat
import java.util
import java.util.{Calendar, Date}

import com.atguigu.bean.StartUpLog
import com.atguigu.utils.RedisUtil
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream
import redis.clients.jedis.Jedis

object DauHandler3 {
  def saveMidToRedis(filterByBatch: DStream[StartUpLog]) = {
    filterByBatch.foreachRDD(rdd=>{
      rdd.foreachPartition(iter=>{
        val client: Jedis = RedisUtil.getJedisClient
        iter.foreach(data=>{
          //redisKey
          val redisKey = s"dau:${data.logDate}"
          client.sadd(redisKey,data.mid)
        })
        client.close()
      })
    })
  }

  def filterDataByBatch(filterByRedisDStream: DStream[StartUpLog]): DStream[StartUpLog] = {
    val dateMidToStartUpLogDStream: DStream[((String, String), StartUpLog)] = filterByRedisDStream.map(data => ((data.logDate, data.mid), data))
    val dateMidToIterDStream: DStream[((String, String), Iterable[StartUpLog])] = dateMidToStartUpLogDStream.groupByKey()
    val filterByBatch: DStream[StartUpLog] = dateMidToIterDStream.flatMap {
      case ((_, _), iter) => {
        iter.toList.sortWith(_.ts < _.ts).take(1)
      }
    }
    filterByBatch
  }

  private val sdf = new SimpleDateFormat("yyyy-MM-dd")

  def filterByRedis(startUpLogDStream: DStream[StartUpLog], ssc: StreamingContext): DStream[StartUpLog] = {
    startUpLogDStream.transform(rdd => {
      //获取前一天
      val date = new Date(System.currentTimeMillis())
      val today: String = sdf.format(date)
      val calendar: Calendar = Calendar.getInstance()
      calendar.setTime(date)
      calendar.add(Calendar.DAY_OF_WEEK, -1)
      val yesterday: String = sdf.format(calendar.getTime)
      //创建redis连接
      val client: Jedis = RedisUtil.getJedisClient
      val yesterdaySet: util.Set[String] = client.smembers(s"dau:${yesterday}")
      val todaySet: util.Set[String] = client.smembers(s"dau:${today}")
      val yesterdayAndTodayMap: Map[String, util.Set[String]] = Map(yesterday -> yesterdaySet, today -> todaySet)
      //放入广播变量
      val yesterdayAndTodayMapBC: Broadcast[Map[String, util.Set[String]]] = ssc.sparkContext.broadcast(yesterdayAndTodayMap)
      //关闭redis连接
      client.close()
      //进行过滤
      rdd.filter(data => {
        val duplicateSet: util.Set[String] = yesterdayAndTodayMapBC.value.getOrElse(data.logDate, null)
        if (duplicateSet != null) {
          !duplicateSet.contains(data.mid)
        } else {
          true
        }
      })
    })
  }

}
