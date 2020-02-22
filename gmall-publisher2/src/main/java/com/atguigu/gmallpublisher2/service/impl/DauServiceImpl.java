package com.atguigu.gmallpublisher2.service.impl;

import com.atguigu.gmallpublisher2.mapper.DauMapper;
import com.atguigu.gmallpublisher2.service.DauService;
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

    @Override
    public Integer getMidCount(String date) {
        return dauMapper.getMidCount(date);
    }

    @Override
    public Map<String, Integer> selectDauTotalHourMap(String date) {
        List<Map<String, Integer>> totalHourMap = dauMapper.selectDauTotalHourMap(date);
        HashMap<String, Integer> maps = new HashMap<>();
        for (Map<String, Integer> map : totalHourMap) {
            maps.put(String.valueOf(map.get("LH")),map.get("CT"));
        }
        return maps;
    }
}
