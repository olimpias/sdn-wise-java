package com.github.sdnwiselab.sdnwise.stats;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by cemturker on 29.11.2018.
 */
public class LifeTimeMonitorController implements LifeTimeMonitorService {
    private static final Logger logger = Logger.getLogger(LifeTimeMonitorController.class.getName());

    private static LifeTimeMonitorService service;

    private MonitorType type = MonitorType.DEFAULT;
    private Date startTime;
    private Date endTime;
    private boolean isEnded;
    private boolean hasStarted;
    private float rssiWeight;
    private float batteryWeight;
    private int topologyLabel;
    private AtomicInteger hopCount;
    private int numberOfNodes;
    private final Map<String, Integer> lastBatteryStats =  new ConcurrentHashMap<>();;

    public synchronized static LifeTimeMonitorService Instance(){
        if(service == null) {
            service = new LifeTimeMonitorController();
        }
        return service;
    }

    private LifeTimeMonitorController() {
        hopCount = new AtomicInteger();
    }

    @Override
    public void setNumberOfNodes(int numberOfNodes) {
        this.numberOfNodes = numberOfNodes;
    }

    @Override
    public void setMonitorType(MonitorType type) {
        this.type = type;
    }

    public int getLabel() {
        return topologyLabel;
    }

    public void setLabel(int topologyLabel) {
        this.topologyLabel = topologyLabel;
    }

    @Override
    public void setBatteryWeight(float bWeight) {
        this.batteryWeight = bWeight;
        this.rssiWeight = 1 - bWeight;
    }

    @Override
    public void logPassedTime() {
        if (isEnded){
            return;
        }
        Date currentTime = new Date();
        if(startTime != null)
            logger.log(Level.INFO,"Passed time: "+(currentTime.getTime() - startTime.getTime())/1000);
    }

    @Override
    public void start() {
        if(hasStarted){
            return;
        }
        this.startTime = new Date();
        hasStarted = true;
        isEnded = false;
        logger.info("Monitor timer has started");
    }

    @Override
    public void end() {
        if(isEnded) {
            logger.info("Monitor timer has already ended");
            return;
        }
        logger.info("Monitor timer has ended");
        isEnded = true;
        this.endTime = new Date();
        dataExporter();
        hasStarted = false;
        logger.log(Level.INFO,"End time: "+(endTime.getTime() - startTime.getTime())/1000);
        System.exit(0);
    }

    @Override
    public void increaseHopCount() {
        hopCount.incrementAndGet();
    }

    @Override
    public void updateBatteryValue(String nodeId, int battery) {
        this.lastBatteryStats.put(nodeId, battery);
    }

    private void dataExporter(){
        SimpleDateFormat dt = new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss");
        int totalBatteryLeft = 0;
        synchronized (this.lastBatteryStats) {
            for(Map.Entry<String, Integer> entry : this.lastBatteryStats.entrySet()) {
                totalBatteryLeft += entry.getValue();
            }
        }
        UUID uuid = UUID.randomUUID();
        PrintWriter printWriter = null;
        try {
            File file = new File(String.format("%d/%.3f/%s-t%d.data",this.numberOfNodes,this.rssiWeight,uuid.toString(),this.topologyLabel));
            file.getParentFile().mkdirs();
            printWriter = new PrintWriter(file);
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("Type: "+type+"\n");
            stringBuffer.append("Battery Weight: " + batteryWeight + "\n");
            stringBuffer.append("Rssi Weight: " + rssiWeight + "\n");
            stringBuffer.append("Test start time: "+ dt.format(startTime)+"\n");
            stringBuffer.append("Test end time: "+ dt.format(endTime)+"\n");
            stringBuffer.append("Spend time: "+(endTime.getTime() - startTime.getTime())/1000+"s\n");
            stringBuffer.append("Hop Count: "+hopCount.get()+"\n");
            stringBuffer.append("Total remaining battery: "+totalBatteryLeft+"\n");
            stringBuffer.append("Total battery consumption: "+ ((this.lastBatteryStats.size() * 255) - totalBatteryLeft)+"\n");
            printWriter.write(stringBuffer.toString());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (printWriter != null)
                printWriter.close();
        }
    }


}
