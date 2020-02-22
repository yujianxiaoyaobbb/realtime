package com.atguigu.gmallpublisher.service.impl;

import com.atguigu.gmallpublisher.mapper.OrderMapper;
import com.atguigu.gmallpublisher.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    OrderMapper orderMapper;
    @Override
    public Double selectOrderAmountTotal(String date) {
        return orderMapper.selectOrderAmountTotal(date);
    }

    public Map<String,Double> selectDauTotalHourMap(String date){
        List<Map<String, Object>> maps = orderMapper.selectOrderAmountHourMap(date);
        HashMap<String, Double> hourToCount = new HashMap<>();
        for (Map<String, Object> map : maps) {
            hourToCount.put((String)map.get("CREATE_HOUR"),(Double)map.get("SUM_AMOUNT"));
        }
        return hourToCount;
    }
}
