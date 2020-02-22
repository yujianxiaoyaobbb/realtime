package com.atguigu.gmallpublisher2.service;

import java.util.Map;

public interface OrderService {

    public Double selectOrderAmountTotal(String date);

    public Map<String,Double> selectOrderAmountHourMap(String date);
 }
