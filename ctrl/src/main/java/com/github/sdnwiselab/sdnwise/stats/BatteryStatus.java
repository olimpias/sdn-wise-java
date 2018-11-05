package com.github.sdnwiselab.sdnwise.stats;

/**
 * Created by cemturker on 05.11.2018.
 */
public class BatteryStatus {
    private int level;
    private long time;

    public BatteryStatus(int level, long time) {
        this.level = level;
        this.time = time;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
