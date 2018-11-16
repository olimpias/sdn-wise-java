package com.github.sdnwiselab.sdnwise.stats;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cemturker on 16.11.2018.
 */
public final class StatsUtils {

    private StatsUtils(){}

    public static List<ForecastNode> forecastNodesGrpcToLocal(SdnWise.ForecastResponse response) {
        List<ForecastNode> nodes = new ArrayList<>();
        for (SdnWise.ForecastNode node :response.getNodesList()) {
            nodes.add(forecastNodeGrpcToLocal(node));
        }
        return nodes;
    }

    public static ForecastNode  forecastNodeGrpcToLocal(@Nonnull SdnWise.ForecastNode gForecastNode) {
        return new ForecastNode(gForecastNode.getNodeID(),gForecastNode.getEstimatedBattery(),gForecastNode.getTime());
    }

}
