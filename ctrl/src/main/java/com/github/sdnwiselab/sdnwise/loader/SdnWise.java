/*
 * Copyright (C) 2015 SDN-WISE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.sdnwiselab.sdnwise.loader;

import com.github.sdnwiselab.sdnwise.adaptation.Adaptation;
import com.github.sdnwiselab.sdnwise.adaptation.AdaptationFactory;
import com.github.sdnwiselab.sdnwise.configuration.Configurator;
import com.github.sdnwiselab.sdnwise.controller.AbstractController;
import com.github.sdnwiselab.sdnwise.controller.ControllerFactory;
import com.github.sdnwiselab.sdnwise.controller.ControllerGui;
import com.github.sdnwiselab.sdnwise.flowvisor.FlowVisor;
import com.github.sdnwiselab.sdnwise.flowvisor.FlowVisorFactory;
import com.github.sdnwiselab.sdnwise.mote.standalone.Mote;
import com.github.sdnwiselab.sdnwise.mote.standalone.Sink;
import com.github.sdnwiselab.sdnwise.packet.DataPacket;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.stats.LifeTimeMonitorController;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starter class of the SDN-WISE project. This class loads the configuration
 * file and starts the Adaptation, the FlowVisor, and the Controller.
 *
 * @author Sebastiano Milardo
 */
public final class SdnWise {

    /**
     * Dafault config file location.
     */
    private static final String CONFIG_FILE = "/config.json";
    /**
     * Set to true to start an emulated network.
     */
    private static final boolean EMULATED = false;
    /**
     * Emulation constants.
     */
    private static final int NO_OF_MOTES = 12,
            ADA_PORT = 9990, BASE_NODE_PORT = 7770, SETUP_TIME = 60000,
            TIMEOUT = 100;

    private static final String RANDOM_TEXT = "asdadaasdasdasdaqwewqeqasdasdasdasdasdasdasdasdasdasdasdasdasdasda" +
            "asdasasdasdasdasdasdasdasd";
    private static ScheduledExecutorService executorService;

    /**
     * Starts the components of the SDN-WISE AbstractController. An SdnWise
     * object is made of three main components: A Controller, an Adaptation, and
     * a FlowVisor. The Controller manages the requests coming from the network,
     * and creates a representation of the topology. The Adaptation adapts the
     * format of the packets coming from the nodes in order to be accepted by
     * the other components of the architecture and vice versa. The FlowVisor is
     * responsible for authenticating nodes and controllers, allowing the
     * slicing of the network.
     *
     * @param args the first argument is the path to the configuration file. if
     * not specificated, the default one is loaded
     */
    public static void main(final String[] args) {
        InputStream is = null;

        if (args.length > 0) {
            try {
                is = new FileInputStream(args[0]);
            } catch (FileNotFoundException ex) {
                Logger.getGlobal().log(Level.SEVERE, ex.toString());
            }
        } else {
            is = SdnWise.class.getResourceAsStream(CONFIG_FILE);
        }
        startExemplaryControlPlane(Configurator.load(is));
    }

    /**
     * Starts the Adaptation layer of the SDN-WISE network. The configurator
     * contains the parameters of the Adaptation layer. In particular: a "lower"
     * Adapter, in order to communicate with the Nodes and an "upper" Adapter to
     * communicate with the FlowVisor
     *
     * @param conf contains the configuration parameters for the Adaptation
     * layer
     * @return the AbstractController layer of the current SDN-WISE network
     */
    public static Adaptation startAdaptation(final Configurator conf) {
        Adaptation adaptation = AdaptationFactory.getAdaptation(conf);
        new Thread(adaptation).start();
        return adaptation;
    }

    /**
     * Starts the AbstractController layer of the SDN-WISE network. The
     * configurator class contains the configuration parameters of the
     * Controller layer. In particular: a "lower" Adapter, that specifies hoe to
     * communicate with the FlowVisor, an "algorithm" to calculate the shortest
     * path in the network. The only supported at the moment is "DIJKSTRA". A
     * "map" which contains a "TIMEOUT" value in seconds, after which a non
     * responding node is removed from the topology, a "RSSI_RESOLUTION" value
     * that triggers an event when a link rssi value changes more than this
     * threshold. "GRAPH" option that set the kind of gui used for the
     * representation of the network, the only possible values at the moment is
     * "GFX" for a GraphStream graph.
     *
     * @param conf contains the configuration parameters of the layer
     * @return the AbstractController layer of the current SDN-WISE network
     */
    public static AbstractController startController(final Configurator conf) {
        AbstractController controller = new ControllerFactory()
                .getController(conf);
        new Thread(controller).start();
        return controller;
    }

    /**
     * Creates a SDN-WISE network. This method creates a Controller, a FlowVisor
     * and an Adaptation plus a simulated network
     *
     * @param conf contains the configuration parameters for the Control plane
     */
    public static void startExemplaryControlPlane(final Configurator conf) {

        // Start the elements of the Control Plane
        // TODO depending on the order of execution the different control plane
        // may be started in different order thus the connection refused
        AbstractController controller = startController(conf);
        try {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }

        //startFlowVisor(conf);
        /*
        InetSocketAddress onos = new InetSocketAddress("192.168.1.108", 9999);

        // Add the nodes IDs that will be managed
        HashSet<NodeAddress> nodeSetAll = new HashSet<>();
        for (int i = 0; i <= 128; i++) {
            nodeSetAll.add(new NodeAddress(i));
        }
        
        // Register the Controllers
        flowVisor.addController(controller.getId(), nodeSetAll);
        flowVisor.addController(onos, nodeSetAll);
         */
        try {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }
        startAdaptation(conf);

        if (EMULATED) {
            startVirtualNetwork();

            // Wait for the nodes to be discovered
            try {
                Thread.sleep(SETUP_TIME);
            } catch (InterruptedException ex) {
                Logger.getGlobal().log(Level.SEVERE, null, ex);
            }
        }
        try {
            int nOfNodes = Integer.parseInt(conf.getController().getMap().get("N_OF_NODES"));
            int nOfCommandedNodes = Integer.parseInt(conf.getController().getMap().get("N_OF_COMMANDED_NODES"));
            int topologyLabel = Integer.parseInt(conf.getController().getMap().get("LABEL"));
            enterFlowRulesForMonitorNodes(controller, nOfNodes, nOfCommandedNodes);
            startSendingMessages(controller, nOfNodes, nOfCommandedNodes);
            LifeTimeMonitorController.Instance().setNumberOfNodes(nOfNodes);
            LifeTimeMonitorController.Instance().setLabel(topologyLabel);
        }catch (Exception e) {
            e.printStackTrace();
        }
        // You can verify the behaviour of the node  using the GUI
        java.awt.EventQueue.invokeLater(() -> {
            new ControllerGui(controller).setVisible(true);
        });

    }

    private static void startSendingMessages(AbstractController controller, int nOfNodes, int nOfCommandedNodes) {
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new RequestSender(controller, nOfNodes, nOfCommandedNodes),
                1,1, TimeUnit.SECONDS);
    }

    private static void enterFlowRulesForMonitorNodes(AbstractController controller,  int nOfNodes, int nOfCommandedNodes){
        for (int i = nOfNodes - nOfCommandedNodes + 1;i<=nOfNodes;i++) {
            controller.addNodeFunction(
                    (byte) 1,
                    new NodeAddress(i),
                    (byte) 1,
                    "HelloWorld.class");
        }
    }

    private static class RequestSender implements Runnable {
        private static int netId = 1;
        private final AbstractController controller;
        private final int nOfNodes;
        private final int nOfCommandedNodes;

        public RequestSender(AbstractController controller, int nOfNodes, int nOfCommandedNodes) {
            this.controller = controller;
            this.nOfNodes = nOfNodes;
            this.nOfCommandedNodes = nOfCommandedNodes;
        }

        @Override
        public void run() {
            for (int i =nOfNodes - nOfCommandedNodes + 1;i<=nOfNodes;i++) {
                NodeAddress src = controller.getSinkAddress();
                NodeAddress dst = new NodeAddress(i);
                NetworkPacket networkPacket = new NetworkPacket(netId,src,dst);
                DataPacket p = new DataPacket(networkPacket);
                p.setNxh(src);
                p.setPayload((RANDOM_TEXT).getBytes(Charset.forName("UTF-8")));
                controller.sendNetworkPacket(p);
            }
        }
    }

    /**
     * Starts the FlowVisor layer of the SDN-WISE network. The configurator
     * class contains the configuration parameters of the FlowVisor layer. In
     * particular: a "lower" Adapter, in order to communicate with the
     * Adaptation and an "upper" Adapter to communicate with the Controller.
     *
     * @param conf contains the configuration parameters of the layer
     * @return the AbstractController layer of the current SDN-WISE network
     */
    public static FlowVisor startFlowVisor(final Configurator conf) {
        FlowVisor flowVisor = FlowVisorFactory.getFlowvisor(conf);
        new Thread(flowVisor).start();
        return flowVisor;
    }

    /**
     * Creates a virtual Network of SDN-WISE nodes. The links between the nodes
     * are specified in each Node#.txt file in the resources directory. These
     * files contain the id of the neighbor, its ip address and port on which it
     * is listening and the rssi between the nodes. For example if in Node0.txt
     * we find 0.1,localhost,7771,215 it means that node 0.0 has 0.1 as
     * neighbor, which is listening on localhost:7771 and the rssi between 0.0
     * and 0.1 is 215.
     */
    public static void startVirtualNetwork() {
        Thread th = new Thread(new Sink(
                // its own id
                (byte) 1,
                // its own address
                new NodeAddress("0.1"),
                // listener port
                BASE_NODE_PORT + 1,
                // controller address
                new InetSocketAddress("localhost", ADA_PORT),
                // neigh file
                "Node1.txt",
                "FINEST",
                "00000001",
                "00:01:02:03:04:05",
                1)
        );
        th.start();

        for (int i = 2; i <= NO_OF_MOTES; i++) {
            new Thread(new Mote(
                    // its own id
                    (byte) 1,
                    // its own address
                    new NodeAddress(i),
                    // listener port
                    BASE_NODE_PORT + i,
                    // neigh file
                    "Node" + i + ".txt",
                    "FINEST")).start();
        }
    }

    /**
     * Private constructor.
     */
    private SdnWise() {
        // Nothing to do here
    }
}
