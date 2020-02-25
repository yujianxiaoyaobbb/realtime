package com.atguigu.app

import java.util

import com.alibaba.fastjson.{JSON, JSONObject}
import com.atguigu.GmallConstants
import com.atguigu.bean.{OrderDetail, OrderInfo, SaleDetail, UserInfo}
import com.atguigu.utils.{MyKafkaUtil, RedisUtil}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization
import redis.clients.jedis.Jedis

import scala.collection.mutable.ListBuffer

object RealTimeSale {
  def main(args: Array[String]): Unit = {
    val conf: SparkConf = new SparkConf().setAppName("RealTimeSale").setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(5))

    //读取kafka数据
    val kafkaOrderInfoDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_ORDER_INFO_TOPIC))
    val kafkaOrderDetailDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_ORDER_DETAIL_TOPIC))
    val kafkaUserInfoDStream: InputDStream[(String, String)] = MyKafkaUtil.getKafkaStream(ssc, Set(GmallConstants.GMALL_USER_INFO_TOPIC))

    //将kafka数据转化为样例类
    //订单表
    val orderInfoDStream: DStream[OrderInfo] = kafkaOrderInfoDStream.map {
      case (_, value) => {
        val orderInfo: OrderInfo = JSON.parseObject(value, classOf[OrderInfo])
        //创建create_date和create_hour
        val timeArr: Array[String] = orderInfo.create_time.split(" ")
        orderInfo.create_date = timeArr(0)
        orderInfo.create_hour = timeArr(1).split(":")(0)
        //对手机号进行脱敏
        orderInfo.consignee_tel = orderInfo.consignee_tel.splitAt(3) + "********"
        orderInfo
      }
    }
    //订单详情表
    val orderDetailDStream: DStream[OrderDetail] = kafkaOrderDetailDStream.map {
      case (_, value) => {
        val orderDetail: OrderDetail = JSON.parseObject(value, classOf[OrderDetail])
        orderDetail
      }
    }
    //用户表
    val userInfoDStream: DStream[UserInfo] = kafkaUserInfoDStream.map {
      case (_, value) => {
        val userInfo: UserInfo = JSON.parseObject(value, classOf[UserInfo])
        userInfo
      }
    }
    //转换结构
    val IdToOrderInfoDStream: DStream[(String, OrderInfo)] = orderInfoDStream.map(data => (data.id, data))
    val orderIdToOrderDatailDStream: DStream[(String, OrderDetail)] = orderDetailDStream.map(data => (data.order_id, data))

    //进行join
    val joinDStream: DStream[(String, (Option[OrderInfo], Option[OrderDetail]))] = IdToOrderInfoDStream.fullOuterJoin(orderIdToOrderDatailDStream)

    val saleDetailDStream: DStream[SaleDetail] = joinDStream.mapPartitions {
      iter => {
        val details = new ListBuffer[SaleDetail]
        //创建redis
        val client: Jedis = RedisUtil.getJedisClient
        implicit val format: DefaultFormats.type = org.json4s.DefaultFormats
        iter.foreach {
          case (orderId, (orderInfo, orderDetail)) => {
            //如果orderInfo不为空
            if (orderInfo.isDefined) {
              //获取orderInfo
              val orderInfoVa: OrderInfo = orderInfo.get
              //如果orderDetail不为空
              if (orderDetail.isDefined) {
                //获取orderDetail
                val orderDetailVa: OrderDetail = orderDetail.get
                //存入集合中
                details += new SaleDetail(orderInfoVa, orderDetailVa)
              }
              //只要orderDetail不为空，就一定会将orderInfo存入redis
              //redisKey
              val orderKey = s"order:$orderId"
              val orderInfoVaJSON: String = Serialization.write(orderInfoVa)
              client.set(orderKey, orderInfoVaJSON)
              client.expire(orderKey, 300)
              val detailKey = s"detail:$orderId"
              //再去redis中查orderDetail
              val orderDetailSet: util.Set[String] = client.smembers(detailKey)
              val itr: util.Iterator[String] = orderDetailSet.iterator()
              while (itr.hasNext) {
                val str: String = itr.next()
                val detail: OrderDetail = JSON.parseObject(str, classOf[OrderDetail])
                details += new SaleDetail(orderInfoVa, detail)
              }
            } else {
              //去redis查找
              val orderKey = s"order:$orderId"
              if (client.exists(orderKey)) {
                val order: String = client.get(orderKey)
                val orderInfoVa: OrderInfo = JSON.parseObject(order, classOf[OrderInfo])
                val detail: OrderDetail = orderDetail.get
                details += new SaleDetail(orderInfoVa, detail)
              } else {
                //没有找到则存入redis
                val detailKey = s"detail:$orderId"
                client.sadd(detailKey)
              }
            }
          }
        }
        details.toIterator
      }
    }
    //测试
    saleDetailDStream.print(100)

    //开启任务
    ssc.start()
    ssc.awaitTermination()
  }
}
