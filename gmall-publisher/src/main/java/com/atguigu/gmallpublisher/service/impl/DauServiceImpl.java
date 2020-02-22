package com.atguigu.gmallpublisher.service.impl;

import com.atguigu.gmallpublisher.mapper.DauMapper;
import com.atguigu.gmallpublisher.service.DauService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DauServiceImpl implements DauService {
    @Autowired
    DauMapper dauMapper;
    @Override
    public Integer getTotal(String date) {
        return dauMapper.getTotal(date);
    }

    public Map<String,Integer> selectDauTotalHourMap(String date){
        List<Map<String, Integer>> totalHourList = dauMapper.selectDauTotalHourMap(date);
        HashMap<String, Integer> maps = new HashMap<>();
        for (Map<String, Integer> map : totalHourList) {
            maps.put(String.valueOf(map.get("LH")),map.get("CT"));
        }
        return maps;
    }

}
