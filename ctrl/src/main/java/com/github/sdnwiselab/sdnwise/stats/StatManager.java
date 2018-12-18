package com.github.sdnwiselab.sdnwise.stats;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by cemturker on 05.11.2018.
 */

public class StatManager implements StatService, ForecastServiceCallback {

    private static final Logger logger = Logger.getLogger(MonitorServiceClient.class.getName());

    private MonitorServiceClient monitorServiceClient;

    private AtomicBoolean hasStarted;
    private String grpcAddress;
    private int grpcPort;

    private Map<String, ForecastNode> forecastNodeMap = null;

    public StatManager(String grpcAddress, int grpcPort){
        this.grpcAddress = grpcAddress;
        this.grpcPort = grpcPort;
        this.hasStarted = new AtomicBoolean(false);
        logger.log(Level.INFO,"Grpc address: "+grpcAddress + ", port: "+grpcPort);
        forecastNodeMap = new ConcurrentHashMap<>();
    }

    @Override
    public void initialize() {
        if (hasStarted.get()) {
            logger.log(Level.WARNING,"Already started");
        }
        hasStarted.set(true);
        if(monitorServiceClient != null) {
            monitorServiceClient.showdown();
        }
        monitorServiceClient = new MonitorServiceClient(this.grpcAddress,this.grpcPort);
        monitorServiceClient.startForecastingObserver(this);
    }

    @Override
    public void add(BatteryInfoNode batteryInfoNode) {
        if (!hasStarted.get()) {
            logger.log(Level.WARNING,"Monitor client is not started");
            return;
        }
        logger.log(Level.INFO,"Observered node info: " +batteryInfoNode);
        this.monitorServiceClient.sendNode(batteryInfoNode);
    }

    @Override
    public ForecastNode forecastBattery(String id) {
        dumpMap();
        ForecastNode node = this.forecastNodeMap.get(id);
        if (node == null) {
            throw new ForecastNotFoundException();
        }
        return node;
    }

    private void dumpMap(){
        logger.log(Level.INFO, this.forecastNodeMap.toString());
    }

    @Override
    public void stop() {
        if (!hasStarted.get()) {
            logger.log(Level.WARNING,"Already stopped");
        }
        hasStarted.set(false);
        this.monitorServiceClient.showdown();
    }

    @Override
    public void predictedBatteries(List<ForecastNode> forecastNodes) {
        synchronized (this) {
            this.forecastNodeMap.clear();
            for (ForecastNode node : forecastNodes) {
                this.forecastNodeMap.put(node.getId(),node);
            }
        }
    }
}
