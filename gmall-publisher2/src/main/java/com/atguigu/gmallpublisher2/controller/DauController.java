package com.atguigu.gmallpublisher2.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmallpublisher2.service.DauService;
import com.atguigu.gmallpublisher2.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
public class DauController {
    @Autowired
    DauService dauService;
    @Autowired
    OrderService orderService;
    //处理新增日活和新增设备请求
    @RequestMapping("realtime-total")
    public String realTimeTotal(@RequestParam("date") String date){
        //获得当日的新增用户数
        Integer total = dauService.getTotal(date);
        Integer midCount = dauService.getMidCount(date);
        Double totalAmount = orderService.selectOrderAmountTotal(date);
        //存放最终结果的集合
        ArrayList<Map<String, Object>> resultLists = new ArrayList<>();
        HashMap<String, Object> maps1 = new HashMap<>();
        HashMap<String, Object> maps2 = new HashMap<>();
        HashMap<String, Object> maps3 = new HashMap<>();

        maps1.put("id","dau");
        maps1.put("name","新增日活");
        maps1.put("value",total);

        maps2.put("id","new_mid");
        maps2.put("name","新增设备");
        maps2.put("value",midCount);

        maps3.put("id","order_amount");
        maps3.put("name","新增交易额");
        maps3.put("value",totalAmount);

        resultLists.add(maps1);
        resultLists.add(maps2);
        resultLists.add(maps3);
        return JSON.toJSONString(resultLists);
    }

    @RequestMapping("realtime-hours")
    public String realTimeHours(@RequestParam("id") String id,@RequestParam("date") String date) throws ParseException {
        String yesterday = getYesterday(date);

        HashMap<String, Map> resultMaps = new HashMap<>();
        Map map1 = null;
        Map map2 = null;
        if("order_amount".equals(id)){
            map1 = orderService.selectOrderAmountHourMap(yesterday);
            map2 = orderService.selectOrderAmountHourMap(date);
        }else{
            map1 = dauService.selectDauTotalHourMap(yesterday);
            map2 = dauService.selectDauTotalHourMap(date);
        }
        resultMaps.put("yesterday",map1);
        resultMaps.put("today",map2);

        return JSON.toJSONString(resultMaps);
    }

    private String getYesterday(@RequestParam("date") String date) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar instance = Calendar.getInstance();
        instance.setTime(sdf.parse(date));
        instance.add(Calendar.DAY_OF_MONTH,-1);
        return sdf.format(new Date(instance.getTimeInMillis()));
    }
}
