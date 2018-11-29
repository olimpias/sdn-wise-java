package com.github.sdnwiselab.sdnwise.stats;

/**
 * Created by cemturker on 29.11.2018.
 */
public interface LifeTimeMonitorService {
    void setMonitorType(MonitorType type);
    void start();
    void end();
}
