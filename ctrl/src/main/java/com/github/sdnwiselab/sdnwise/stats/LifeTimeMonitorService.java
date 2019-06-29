package com.github.sdnwiselab.sdnwise.stats;

import java.util.Map;

/**
 * Created by cemturker on 29.11.2018.
 */
public interface LifeTimeMonitorService {
    void setLabel(int topologyLabel);
    void setNumberOfNodes(int numberOfNodes);
    void setMonitorType(MonitorType type);
    void setBatteryWeight(float bWeight);
    void logPassedTime();
    void start();
    void end();
    void increaseHopCount();
    void updateBatteryValue(String nodeId, int battery);
}
