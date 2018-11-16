package com.github.sdnwiselab.sdnwise.stats;

/**
 * Created by cemturker on 15.11.2018.
 */
public class ForecastNode extends BatteryInfoNode {

    private long forecastTime;
    public ForecastNode(String id, long level, long time) {
        super(id, level);
        this.forecastTime = time;
    }

    public long getForecastTime() {
        return forecastTime;
    }

    public void setForecastTime(long forecastTime) {
        this.forecastTime = forecastTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ForecastNode{");
        sb.append("id='").append(id).append('\'');
        sb.append(", forecastTime=").append(forecastTime);
        sb.append(", level=").append(level);
        sb.append('}');
        return sb.toString();
    }
}
