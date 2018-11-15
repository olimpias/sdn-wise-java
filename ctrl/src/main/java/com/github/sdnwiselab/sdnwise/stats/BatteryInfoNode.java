package com.github.sdnwiselab.sdnwise.stats;

/**
 * Created by cemturker on 05.11.2018.
 */
public class BatteryInfoNode {
    protected String id;
    protected int level;

    public BatteryInfoNode(String id, int level) {
        this.id = id;
        this.level = level;
    }

    public String getId() {
        return id;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BatteryInfoNode{");
        sb.append("id='").append(id).append('\'');
        sb.append(", level=").append(level);
        sb.append('}');
        return sb.toString();
    }
}
