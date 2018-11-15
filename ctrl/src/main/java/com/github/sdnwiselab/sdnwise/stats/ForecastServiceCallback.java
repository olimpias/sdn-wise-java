package com.github.sdnwiselab.sdnwise.stats;

/**
 * Created by cemturker on 15.11.2018.
 */
public interface ForecastServiceCallback {
    void predictedBattery(ForecastNode forecastNode);
}
