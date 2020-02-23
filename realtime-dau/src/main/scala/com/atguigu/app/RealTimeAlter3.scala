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

object RealTimeAlter3 {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeAlter3").setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")

    //读取kafka数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.KAFKA_TOPIC_EVENT))

    //转化成样例类
    val eventInfoDStream: DStream[EventInfo] = kafkaDStream.map {
      case (_, value) => {
        val eventInfo: EventInfo = JSON.parseObject(value, classOf[EventInfo])
        val date: String = sdf.format(new Date(eventInfo.ts))
        val dateArr: Array[String] = date.split(" ")
        eventInfo.logDate = dateArr(0)
        eventInfo.logHour = dateArr(1)
        eventInfo
      }
    }
    //按照mid进行分组
    val midToEventInfoDStream: DStream[(String, Iterable[EventInfo])] = eventInfoDStream.map(data => (data.mid, data))
      .groupByKey()
    //转变结构
    val booleanToCouponAlertInfoDStream: DStream[(Boolean, CouponAlertInfo)] = midToEventInfoDStream.map {
      case (mid, iter) => {
        val uids = new util.HashSet[String]()
        val itemIds = new util.HashSet[String]()
        val events = new util.ArrayList[String]()
        var isClick: Boolean = false
        breakable {
          for (elem <- iter) {
            events.add(elem.evid)
            if ("clickItem".equals(elem.evid)) {
              isClick = true
              break()
            } else if ("coupon".equals(elem.evid)) {
              uids.add(elem.uid)
              itemIds.add(elem.itemid)
            }
          }
        }
        (!isClick && uids.size() >= 3, CouponAlertInfo(mid, uids, itemIds, events, System.currentTimeMillis()))
      }
    }
    //过滤出预警
    val couponAlertInfoDStream: DStream[CouponAlertInfo] = booleanToCouponAlertInfoDStream.filter(_._1)
      .map(_._2)

    couponAlertInfoDStream.print()

    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
