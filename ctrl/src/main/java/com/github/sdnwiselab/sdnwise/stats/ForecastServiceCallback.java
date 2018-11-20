package com.github.sdnwiselab.sdnwise.stats;

import java.util.List;

/**
 * Created by cemturker on 15.11.2018.
 */
public interface ForecastServiceCallback {
    void predictedBatteries(List<ForecastNode> forecastNodes);
}
