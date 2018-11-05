package com.github.sdnwiselab.sdnwise.stats;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by cemturker on 05.11.2018.
 */

//TODO will be updated with GRPC interface...
public class StatCollector {
    private Map<String, LinkedList<BatteryStatus>> batteryStatusMap;

    public StatCollector(){
        this.batteryStatusMap = new ConcurrentHashMap<>();
    }

    public synchronized void addBatteryStat(String nodeID, BatteryStatus batteryStatus) {
        LinkedList<BatteryStatus> batteryStatuses = this.batteryStatusMap.get(nodeID);
        if (batteryStatuses == null) {
            batteryStatuses = new LinkedList<>();
            this.batteryStatusMap.put(nodeID,batteryStatuses);
        }
        batteryStatuses.add(batteryStatus);
    }

    public void exportNodeStats(String nodeID) {
        try {
            PrintWriter printWriter = new PrintWriter(nodeID+".data", "UTF-8");
            StringBuffer stringBuffer = new StringBuffer();
            LinkedList<BatteryStatus> batteryStatuses = this.batteryStatusMap.get(nodeID);
            batteryStatuses.forEach(batteryStatus -> {
                stringBuffer.append(batteryStatus.getTime()+ " "+batteryStatus.getLevel()+"\n");
            });
            printWriter.write(stringBuffer.toString());
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}
