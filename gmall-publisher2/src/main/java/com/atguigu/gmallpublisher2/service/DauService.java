package com.atguigu.gmallpublisher2.service;

import java.util.List;
import java.util.Map;

public interface DauService {
    public Integer getTotal(String date);

    public Integer getMidCount(String date);

    public Map<String, Integer> selectDauTotalHourMap(String date);
}
