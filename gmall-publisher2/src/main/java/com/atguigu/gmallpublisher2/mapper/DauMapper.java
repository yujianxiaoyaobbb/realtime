package com.atguigu.gmallpublisher2.mapper;

import java.util.List;
import java.util.Map;

public interface DauMapper {
    public Integer getTotal(String date);

    public Integer getMidCount(String date);

    public List<Map<String,Integer>> selectDauTotalHourMap(String date);
}
