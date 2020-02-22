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

object DauHandler {
  def saveMidToRedis(filterByBatch: DStream[StartUpLog]) = {
    filterByBatch.foreachRDD(rdd=>{
      rdd.foreachPartition(itr=>{
        //创建redis连接
        val client: Jedis = RedisUtil.getJedisClient
        //将mid写入到redis
        itr.foreach(data=>{
          val redisKey = s"dau:${data.logDate}"
          client.sadd(redisKey,data.mid)
        })
        //关闭redis连接
        client.close()
      })
    })
  }

  //日期格式化
  private val sdf = new SimpleDateFormat("yyyy-MM-dd")
  //批次间过滤
  def filterByRedis(startUpLogDStream: DStream[StartUpLog], ssc: StreamingContext): DStream[StartUpLog] = {
    startUpLogDStream.transform(rdd=>{
      //创建redis连接
      val client: Jedis = RedisUtil.getJedisClient
      //获得当前时间
      val date = new Date(System.currentTimeMillis())
      val today: String = sdf.format(date)
      //日历
      val calendar: Calendar = Calendar.getInstance()
      calendar.setTime(sdf.parse(today))
      calendar.add(Calendar.DAY_OF_WEEK,-1)
      //获得前一天
      val yesterday: String = sdf.format(calendar.getTime)
      val yesterdayKey = s"dau:${yesterday}"
      val todayKey = s"dau:${today}"
      //前一天的重复mid集合
      val yesterdaySet: util.Set[String] = client.smembers(yesterdayKey)
      //今天的重复mid集合
      val todaySet: util.Set[String] = client.smembers(todayKey)
      val duplicateMaps: Map[String, util.Set[String]] = Map(yesterdayKey->yesterdaySet,todayKey->todaySet)
      //放入广播变量
      val duplicateMapsBC: Broadcast[Map[String, util.Set[String]]] = ssc.sparkContext.broadcast(duplicateMaps)
      //关闭redis连接
      client.close()
      //进行批次间的重复mid过滤
      rdd.filter(data=>{
        val maps: Map[String, util.Set[String]] = duplicateMapsBC.value
        val sets: util.Set[String] = maps.getOrElse(s"dau:${data.logDate}",null)
        if(sets != null){
          !sets.contains(data.mid)
        }else{
          true
        }
      })
    })
  }
  //同批次过滤
  def filterDataByBatch(filterByRedisDStream: DStream[StartUpLog]): DStream[StartUpLog] = {
    //((date,mid),log)
    val dateMidToLogDStream: DStream[((String, String), StartUpLog)] = filterByRedisDStream.map(data=>((data.logDate,data.mid),data))

    //分组
    val dateMidToIterDStream: DStream[((String, String), Iterable[StartUpLog])] = dateMidToLogDStream.groupByKey()

    //相同(date,mid)只取第一个
    val value: DStream[StartUpLog] = dateMidToIterDStream.flatMap {
      case ((_, _), logItr) => {
        logItr.toList.sortWith(_.ts < _.ts).take(1)
      }
    }
    value
  }
}
