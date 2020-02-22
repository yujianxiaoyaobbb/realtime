package com.atguigu.gmallpublisher2.mapper;

import java.util.List;
import java.util.Map;

public interface OrderMapper {
    public Double selectOrderAmountTotal(String date);

    public List<Map<String,Object>> selectOrderAmountHourMap(String date);
}
