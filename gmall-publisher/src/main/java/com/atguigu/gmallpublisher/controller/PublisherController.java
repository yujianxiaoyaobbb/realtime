package com.atguigu.gmallpublisher.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmallpublisher.service.DauService;
import com.atguigu.gmallpublisher.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class PublisherController {
    @Autowired
    DauService dauService;
    @Autowired
    OrderService orderService;

    @RequestMapping("realtime-total")
    public String getRealTimeTotal(@RequestParam("date") String date) {
        //获得总数
        Integer total = dauService.getTotal(date);
        //获得Gmv总交易额
        Double totalAmount = orderService.selectOrderAmountTotal(date);
        ArrayList<HashMap<String, Object>> realTimeTotalList = new ArrayList<>();
        HashMap<String, Object> totalMap = new HashMap<>();
        HashMap<String, Object> newMidMap = new HashMap<>();
        HashMap<String, Object> GmvMap = new HashMap<>();
        //填充新增日活的map
        totalMap.put("id","dau");
        totalMap.put("name","新增日活");
        totalMap.put("value",total);
        //填充新增设备的map
        newMidMap.put("id","new_mid");
        newMidMap.put("name","新增设备");
        newMidMap.put("value","233");
        //填充GMV的map
        GmvMap.put("id","order_amount");
        GmvMap.put("name","新增交易额");
        GmvMap.put("value",totalAmount);
        //将map添加到list中
        realTimeTotalList.add(totalMap);
        realTimeTotalList.add(newMidMap);
        realTimeTotalList.add(GmvMap);

        return JSON.toJSONString(realTimeTotalList);
    }

    @RequestMapping("realtime-hours")
    public String realTimeHours(@RequestParam("id") String id,@RequestParam("date") String date) throws ParseException {
        HashMap<String, Map> resultMap = new HashMap<>();
        //获得 昨天的日期
        String yesterday = getYesterdady(date);
        Map yesterdayMap = null;
        Map todayMap = null;
        if("order_amount".equals(id)){
            //获得下层的返回结果
            yesterdayMap = orderService.selectDauTotalHourMap(yesterday);
            todayMap = orderService.selectDauTotalHourMap(date);
        }else{
            //获得下层的返回结果
            yesterdayMap = dauService.selectDauTotalHourMap(date);
            todayMap = dauService.selectDauTotalHourMap(yesterday);
        }

        resultMap.put("today",todayMap);
        resultMap.put("yesterday",yesterdayMap);
        return JSON.toJSONString(resultMap);
    }

    private String getYesterdady(@RequestParam("date") String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar instance = Calendar.getInstance();
        instance.setTime(sdf.parse(date));
        instance.add(Calendar.DAY_OF_MONTH,-1);
        return sdf.format(new Date(instance.getTimeInMillis()));
    }
}
