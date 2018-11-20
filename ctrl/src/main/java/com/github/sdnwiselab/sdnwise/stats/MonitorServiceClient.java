package com.github.sdnwiselab.sdnwise.stats;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Created by cemturker on 16.11.2018.
 */
public class MonitorServiceClient {
    private static final Logger logger = Logger.getLogger(MonitorServiceClient.class.getName());

    private final ManagedChannel channel;
    private final MonitorGrpc.MonitorBlockingStub blockingStub;
    private ExecutorService executor;

    public MonitorServiceClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(false)
                .build());
    }

    private MonitorServiceClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = MonitorGrpc.newBlockingStub(channel);
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void showdown() {
        executor.shutdownNow();
        channel.shutdownNow();
    }

    public void startForecastingObserver(ForecastServiceCallback callback){
        logger.info("Forecasting service has been started");
        executor.execute(new ForecastStreamer(callback));
    }

    public void sendNode(BatteryInfoNode node) {
        logger.info("Node "+node+" will be sent");
        SdnWise.StatSaveRequest statSaveRequest = SdnWise.StatSaveRequest.newBuilder()
                .setCurrentBattery(node.getLevel())
                .setNodeID(node.getId())
                .build();
        SdnWise.StatSaveResponse response;
        try {
            response = blockingStub.saveBattery(statSaveRequest);
            if(!response.getNodeID().equals(node.getId())) {
                throw new UnmatchedException();
            }
        }catch (StatusRuntimeException ex) {
            logger.info("RPC failed: "+ex.getStatus());
            return;
        }
        logger.info(node.getId()+" has sent to Monitor service");
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
            }
        }
    }


}
