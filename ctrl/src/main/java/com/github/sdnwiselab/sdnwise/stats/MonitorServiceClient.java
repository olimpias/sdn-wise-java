package com.github.sdnwiselab.sdnwise.stats;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by cemturker on 16.11.2018.
 */
public class MonitorServiceClient {
    private static final Logger logger = Logger.getLogger(MonitorServiceClient.class.getName());
    private static final int QUEUE_SIZE = 20;
    private final ManagedChannel channel;
    private final MonitorGrpc.MonitorBlockingStub blockingStub;
    private ExecutorService forecastExecutor;
    private ExecutorService producerExecutor;
    private final ArrayBlockingQueue<BatteryInfoNode> bQ;

    public MonitorServiceClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build());
    }

    private MonitorServiceClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = MonitorGrpc.newBlockingStub(channel);
        bQ = new ArrayBlockingQueue<>(QUEUE_SIZE);
        this.forecastExecutor = Executors.newSingleThreadExecutor();
        this.producerExecutor = Executors.newSingleThreadExecutor();
    }

    public void showdown() {
        forecastExecutor.shutdownNow();
        producerExecutor.shutdownNow();
        channel.shutdownNow();
    }

    public void startForecastingObserver(ForecastServiceCallback callback){
        logger.info("Forecasting service has been started");
        forecastExecutor.execute(new ForecastStreamer(callback));
        logger.info("Battery producer has been added");
        producerExecutor.execute(new BatteryProducer());
    }

    public void sendNode(BatteryInfoNode node) {
        try {
            this.bQ.put(node);
            logger.info("Node "+node+" is queued");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class ForecastStreamer implements Runnable {
        private ForecastServiceCallback callback;

        public ForecastStreamer(ForecastServiceCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            streamObserver();
        }

        private void streamObserver () {
            Empty empty = Empty.newBuilder().build();
            Iterator<SdnWise.ForecastResponse> responseIterator;
            try {
                responseIterator = blockingStub.forecastBatteries(empty);
                for (;responseIterator.hasNext();) {
                    SdnWise.ForecastResponse response = responseIterator.next();
                    List<ForecastNode> nodes = StatsUtils.forecastNodesGrpcToLocal(response);
                    this.callback.predictedBatteries(nodes);
                    logger.info(response+" is received");
                }
            }catch (StatusRuntimeException e) {
                logger.info("RPC failed: "+e.getStatus());
                e.printStackTrace();
            }
        }
    }

    public class BatteryProducer implements Runnable {

        @Override
        public void run() {
            BatteryInfoNode infoNode;
            logger.info("Deque has been started");
            while (true) {
                try {
                    infoNode = bQ.take();
                    logger.info("Deque node: "+infoNode);
                    sendNode(infoNode);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    logger.warning(e.getMessage());
                }

            }

        }

        private void sendNode(BatteryInfoNode node) {
            SdnWise.StatSaveRequest statSaveRequest = SdnWise.StatSaveRequest.newBuilder()
                    .setCurrentBattery(node.getLevel())
                    .setNodeID(node.getId())
                    .build();
            SdnWise.StatSaveResponse response = null;
            try {
                logger.info("Node "+node+" will be sent");
                response = blockingStub.saveBattery(statSaveRequest);
                if(!response.getNodeID().equals(node.getId())) {
                    throw new UnmatchedException();
                }
            }catch (StatusRuntimeException ex) {
                logger.info("RPC failed: "+ex.getStatus());
                ex.printStackTrace();
                return;
            }catch (UnmatchedException ex) {
                logger.warning(response == null ? "Response is null":
                        "Miss match on nodes. Response node: "+response.getNodeID()+" node: "+node.getId());
                return;
            }catch (Exception ex) {
                logger.warning("ERROR :"+ex.getMessage());
                return;
            }
            logger.info(node.getId()+" has sent to Monitor service");
        }
    }


}
