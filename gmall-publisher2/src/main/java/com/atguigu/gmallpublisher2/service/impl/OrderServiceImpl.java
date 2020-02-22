package com.atguigu.gmallpublisher2.service.impl;

import com.atguigu.gmallpublisher2.mapper.OrderMapper;
import com.atguigu.gmallpublisher2.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    OrderMapper orderMapper;
    @Autowired
    OrderService orderService;
    @Override
    public Double selectOrderAmountTotal(String date) {
        return orderMapper.selectOrderAmountTotal(date);
    }

    @Override
    public Map<String, Double> selectOrderAmountHourMap(String date) {
        List<Map<String, Object>> maps = orderMapper.selectOrderAmountHourMap(date);
        HashMap<String, Double> resultMap = new HashMap<>();
        for (Map<String, Object> map : maps) {
            resultMap.put(String.valueOf(map.get("CREATE_HOUR")),(Double)map.get("SUM_AMOUNT"));
        }
        return resultMap;
    }
}
