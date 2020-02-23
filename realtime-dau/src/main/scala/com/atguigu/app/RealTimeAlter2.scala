package com.atguigu.app

import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.alibaba.fastjson.JSON
import com.atguigu.GmallConstants
import com.atguigu.bean.{CouponAlertInfo, EventInfo}
import com.atguigu.utils.MyKafkaUtil
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.util.control.Breaks._

object RealTimeAlter2 {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeAlter2").setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
    //读取kafka数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.KAFKA_TOPIC_EVENT))

    //将数据转换成样例类
    val eventInfoDStream: DStream[EventInfo] = kafkaDStream.map {
      case (_, value) => {
        val eventInfo: EventInfo = JSON.parseObject(value, classOf[EventInfo])
        //补全logDate和logHour
        val date: String = sdf.format(new Date(eventInfo.ts))
        val dataArr: Array[String] = date.split(" ")
        eventInfo.logDate = dataArr(0)
        eventInfo.logHour = dataArr(1)
        eventInfo
      }
    }
    //按照mid进行分组
    val midToIterDStream: DStream[(String, Iterable[EventInfo])] = eventInfoDStream.map(data => (data.mid, data))
      .groupByKey()
    //进行结构转换(boolean,couponAlterInfo)
    val booleanToCouponAlterInfoDStream: DStream[(Boolean, CouponAlertInfo)] = midToIterDStream.map {
      case (mid, iter) => {
        //存放优惠券登录过得uid
        val uids: util.HashSet[String] = new java.util.HashSet[String]()
        //存放优惠券涉及的商品id
        val itemIds: util.HashSet[String] = new java.util.HashSet[String]()
        //存放发生过的行为
        val events: util.ArrayList[String] = new util.ArrayList[String]()
        var isClick: Boolean = false
        breakable {
          iter.foreach(data => {
            events.add(data.evid)
            if ("clickItem".equals(data.evid)) {
              isClick = true
              break()
            } else if ("coupon".equals(data.evid)) {
              uids.add(data.uid)
              itemIds.add(data.itemid)
            }
          })
        }
        (!isClick && uids.size() >= 3, CouponAlertInfo(mid, uids, itemIds, events, System.currentTimeMillis()))
      }
    }
    val couponAlertInfoDStream: DStream[CouponAlertInfo] = booleanToCouponAlterInfoDStream.filter(_._1)
      .map(_._2)

    couponAlertInfoDStream.print()

    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
