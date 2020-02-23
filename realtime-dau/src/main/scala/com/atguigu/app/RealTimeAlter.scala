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


object RealTimeAlter {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeAlter").setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))
    val sdf = new SimpleDateFormat("yyyy-MM-dd HH")
    //读取kafka数据
    val kafkaDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.KAFKA_TOPIC_EVENT))

    //将kafka数据转变为包装类
    val eventInfoDStream: DStream[EventInfo] = kafkaDStream.map {
      case (_, value) => {
        val eventInfo: EventInfo = JSON.parseObject(value, classOf[EventInfo])
        //补全logDate和logHour
        val date: String = sdf.format(new Date(eventInfo.ts))
        val dateArr: Array[String] = date.split(" ")
        eventInfo.logDate = dateArr(0)
        eventInfo.logHour = dateArr(1)
        eventInfo
      }
    }

    //按照mid分组
    val midToIterDStream: DStream[(String, Iterable[EventInfo])] = eventInfoDStream
      .map(data => (data.mid, data))
      .groupByKey()
    //按照条件进行过滤预警信息
    val booleanToCouponDStream: DStream[(Boolean, CouponAlertInfo)] = midToIterDStream.map {
      case (mid, iter) => {
        //发生过的行为
        val events: util.List[String] = new util.ArrayList[String]()
        //领取优惠券登陆过的uid
        val uids: util.HashSet[String] = new util.HashSet[String]()
        //优惠券涉及的商品id
        val itemIds: util.HashSet[String] = new util.HashSet[String]()

        var isClicked: Boolean = false
        //遍历iter
        breakable {
          for (elem <- iter) {
            events.add(elem.evid)
            if ("clickItem".equals(elem.evid)) {
              isClicked = true
              break()
            } else if ("coupon".equals(elem.evid)) {
              uids.add(elem.uid)
              itemIds.add(elem.itemid)
            }
          }
        }
        (!isClicked && uids.size() >= 3, CouponAlertInfo(mid, uids, itemIds, events, System.currentTimeMillis()))
      }
    }
    val couponAltertInfoDStream: DStream[(Boolean, CouponAlertInfo)] = booleanToCouponDStream.filter(_._1)
    val value: DStream[CouponAlertInfo] = couponAltertInfoDStream.map(_._2)
    value.print()
    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
