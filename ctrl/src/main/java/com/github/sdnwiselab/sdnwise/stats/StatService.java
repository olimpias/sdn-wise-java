package com.github.sdnwiselab.sdnwise.stats;

/**
 * Created by cemturker on 15.11.2018.
 */
public interface StatService {
    void initialize();
    void add(BatteryInfoNode batteryInfoNode);
    BatteryInfoNode forecastBattery(String id);
    void stop();
}
