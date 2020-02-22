package com.atguigu.gmallpublisher.service;

import java.util.Map;

public interface OrderService {
    public Double selectOrderAmountTotal(String date);

    public Map<String,Double> selectDauTotalHourMap(String date);
}
