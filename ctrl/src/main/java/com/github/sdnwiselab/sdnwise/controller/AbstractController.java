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
package com.github.sdnwiselab.sdnwise.controller;

import com.github.sdnwiselab.sdnwise.adapter.AbstractAdapter;
import com.github.sdnwiselab.sdnwise.controlplane.*;
import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.function.FunctionInterface;
import static com.github.sdnwiselab.sdnwise.packet.ConfigFunctionPacket.createPackets;
import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import net.jodah.expiringmap.ExpiringMap;

/**
 * This class holds a representation of the sensor network and resolves all the
 * requests coming from the network itself. This abstract class has two main
 * methods. manageRoutingRequest and graphUpdate. The first is called when a
 * request is coming from the network while the latter is called when something
 * in the topology of the network changes.
 * <p>
 * There are two main implementation of this class: ControllerDijkstra and
 * AbstractController Static.
 * <p>
 * This class also offers methods to send messages and configure the nodes in
 * the network.
 *
 * @author Sebastiano Milardo
 * @version 0.1
 */
public abstract class AbstractController extends ControlPlaneLayer implements ControllerInterface {

    // to avoid garbage collection of the logger
    protected static final Logger LOGGER = Logger.getLogger("CTRL");

    final static int SDN_WISE_RLS_MAX = 16;
    final static int RESPONSE_TIMEOUT = 300; // Increase when using COOJA
    final static int CACHE_EXP_TIME = 5;

    private final ArrayBlockingQueue<NetworkPacket> bQ = new ArrayBlockingQueue<>(1000);
    private NodeAddress sinkAddress;
    private final InetSocketAddress id;

    final HashMap<NodeAddress, LinkedList<NodeAddress>> results = new HashMap<>();
    final Map<String, ConfigPacket> configCache = ExpiringMap.builder()
            .expiration(CACHE_EXP_TIME, TimeUnit.SECONDS)
            .build();
    final Map<String, RequestPacket> requestCache = ExpiringMap.builder()
            .expiration(CACHE_EXP_TIME, TimeUnit.SECONDS)
            .build();

    final NetworkGraph networkGraph;

    /**
     * Constructor Method for the Controller Class.
     *
     * @param id ControllerId object.
     * @param lower Lower Adpater object.
     * @param networkGraph NetworkGraph object.
     */
    AbstractController(InetSocketAddress id, AbstractAdapter lower, NetworkGraph networkGraph) {
        super("CTRL", lower, null);
        this.sinkAddress = new NodeAddress("0.1");
        ControlPlaneLogger.setupLogger(layerShortName);
        this.id = id;
        this.networkGraph = networkGraph;
    }

    public NodeAddress getSinkAddress() {
        return sinkAddress;
    }

    @Override
    public final InetSocketAddress getId() {
        return id;
    }

    public void managePacket(NetworkPacket data) {

        switch (data.getTyp()) {
            case REPORT:
                networkGraph.updateMap(new ReportPacket(data));
                break;

            case REQUEST:
                RequestPacket req = new RequestPacket(data);
                NetworkPacket p = putInRequestCache(req);
                if (p != null) {
                    manageRoutingRequest(req, p);
                }
                break;

            case CONFIG:
                ConfigPacket cp = new ConfigPacket(data);

                switch (cp.getConfigId()) {
                    case MY_ADDRESS:
                    case MY_NET:
                    case RESET:
                    case TTL_MAX:
                    case RSSI_MIN:
                        cp = new ConfigNodePacket(data);
                        break;
                    case BEACON_MAX:
                    case REPORT_MAX:
                    case UPDTABLE_MAX:
                    case SLEEP_MAX:
                        cp = new ConfigTimerPacket(data);
                        break;
                    case ADD_ACCEPTED:
                    case LIST_ACCEPTED:
                    case REMOVE_ACCEPTED:
                        cp = new ConfigAcceptedIdPacket(data);
                        break;
                    case ADD_RULE:
                    case GET_RULE_AT:
                    case REMOVE_RULE:
                    case REMOVE_RULE_AT:
                        cp = new ConfigRulePacket(data);
                        break;
                    case ADD_FUNCTION:
                    case REMOVE_FUNCTION:
                        cp = new ConfigFunctionPacket(data);
                        break;
                    default:
                        break;
                }

                String key;
                if (cp.getConfigId() == (GET_RULE_AT)) {
                    key = cp.getNet() + " "
                            + cp.getSrc() + " "
                            + cp.getConfigId() + " "
                            + cp.getValue();
                } else {
                    key = cp.getNet() + " "
                            + cp.getSrc() + " "
                            + cp.getConfigId();
                }
                configCache.put(key, cp);
                break;
            case REG_PROXY:
                sinkAddress = data.getSrc();
            default:
                break;
        }
    }

    /**
     * This methods manages updates coming from the lower adapter or the network
     * representation. When a message is received from the lower adapter it is
     * inserted in a ArrayBlockingQueue and then the method managePacket it is
     * called on it. While for updates coming from the network representation
     * the method graphUpdate is invoked.
     *
     * @param o the source of the event.
     * @param arg Object sent by Observable.
     */
    @Override
    public void update(Observable o, Object arg) {
        if (o.equals(lower)) {
            try {
                bQ.put(new NetworkPacket((byte[]) arg));
            } catch (InterruptedException ex) {
                log(Level.SEVERE, ex.toString());
            }
        } else if (o.equals(networkGraph)) {
            graphUpdate();
        }
    }

    @Override
    public void setupLayer() {
        new Thread(new Worker(bQ)).start();
        networkGraph.addObserver(this);
        register();
        setupNetwork();
    }

    /**
     * This method sends a SDN_WISE_OPEN_PATH messages to a generic node. This
     * kind of message holds a list of nodes that will create a path inside the
     * network.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param path the list of all the NodeAddresses in the path.
     */
    @Override
    public final void sendPath(byte netId, NodeAddress destination,
            List<NodeAddress> path) {
        OpenPathPacket op = new OpenPathPacket(netId, sinkAddress, destination, path);
        op.setNxh(sinkAddress);
        sendNetworkPacket(op);
    }


    /**
     * This method sets the address of a node. The new address value is passed
     * using two bytes.
     *
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param newAddress the new address.
     */
    @Override
    public final void setNodeAddress(byte netId, NodeAddress destination,
            NodeAddress newAddress) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setNodeAddressValue(newAddress)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the address of a node.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @return returns the NodeAddress of a node, null if it does exists.
     */
    public final NodeAddress getNodeAddress(byte netId,
            NodeAddress destination) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setReadNodeAddressValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return null;
        }
        return ((ConfigNodePacket) response).getNodeAddress();
    }

    /**
     * This method sets the Network ID of a node. The new value is passed using
     * a byte.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     */
    @Override
    public final void resetNode(byte netId, NodeAddress destination) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setResetValue()
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method sets the Network ID of a node. The new value is passed using
     * a byte.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @param newNetId value of the new net ID
     */
    @Override
    public final void setNodeNetId(byte netId, NodeAddress destination,
            byte newNetId) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setNetworkIdValue(newNetId)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the Network ID of a node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @return returns the nedId, -1 if not found.
     */
    public final int getNodeNetId(byte netId, NodeAddress destination) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setReadNetworkIdValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
        return ((ConfigNodePacket) response).getNetworkIdValue();
    }

    /**
     * This method sets the beacon period of a node. The new value is passed
     * using a short.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @param period beacon period in seconds
     */
    @Override
    public final void setNodeBeaconPeriod(byte netId, NodeAddress destination,
            short period) {
        ConfigTimerPacket cp = new ConfigTimerPacket(netId, sinkAddress, destination);
        cp.setBeaconPeriodValue(period)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the beacon period of a node.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @return returns the beacon period, -1 if not found
     */
    @Override
    public final int getNodeBeaconPeriod(byte netId, NodeAddress destination) {
        ConfigTimerPacket cp = new ConfigTimerPacket(netId, sinkAddress, destination);
        cp.setReadBeaconPeriodValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;

        }
        return ((ConfigTimerPacket) response).getBeaconPeriodValue();
    }

    /**
     * This method sets the report period of a node. The new value is passed
     * using a short.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @param period report period in seconds
     */
    @Override
    public final void setNodeReportPeriod(byte netId, NodeAddress destination,
            short period) {
        ConfigTimerPacket cp = new ConfigTimerPacket(netId, sinkAddress, destination);
        cp.setReportPeriodValue(period)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the report period of a node.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @return returns the report period, -1 if not found
     */
    @Override
    public final int getNodeReportPeriod(byte netId, NodeAddress destination) {
        ConfigTimerPacket cp = new ConfigTimerPacket(netId, sinkAddress, destination);
        cp.setReadReportPeriodValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
        return ((ConfigTimerPacket) response).getReportPeriodValue();
    }

    /**
     * This method sets the update table period of a node. The new value is
     * passed using a short.
     *
     * @param netId network id of the destination node
     * @param destination network address of the destination node
     * @param period update table period in seconds (TODO check)
     */
    @Override
    public final void setNodeUpdateTablePeriod(byte netId,
            NodeAddress destination, short period) {
        ConfigTimerPacket cp = new ConfigTimerPacket(netId, sinkAddress, destination);
        cp.setUpdateTablePeriodValue(period)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the Update table period of a node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @return returns the updateTablePeriod, -1 if not found.
     */
    @Override
    public final int getNodeUpdateTablePeriod(byte netId,
            NodeAddress destination) {
        ConfigTimerPacket cp = new ConfigTimerPacket(netId, sinkAddress, destination);
        cp.setReadUpdateTablePeriodValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
        return ((ConfigTimerPacket) response).getUpdateTablePeriodValue();
    }

    /**
     * This method sets the maximum time to live for each message sent by a
     * node. The new value is passed using a byte.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param newTtl time to live in number of hops.
     */
    @Override
    public final void setNodeTtlMax(byte netId, NodeAddress destination,
            byte newTtl) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setDefaultTtlMaxValue(newTtl)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the maximum time to live for each message sent by a
     * node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @return returns the maximum time to live, -1 if not found.
     */
    @Override
    public final int getNodeTtlMax(byte netId, NodeAddress destination) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setReadDefaultTtlMaxValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
        return ((ConfigNodePacket) response).getDefaultTtlMaxValue();
    }

    /**
     * This method sets the minimum RSSI in order to consider a node as a
     * neighbor.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param newRssi new threshold rssi value.
     */
    @Override
    public final void setNodeRssiMin(byte netId, NodeAddress destination,
            byte newRssi) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setDefaultRssiMinValue(newRssi)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the minimum RSSI in order to consider a node as a
     * neighbor.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @return returns the minimum RSSI, -1 if not found.
     */
    @Override
    public final int getNodeRssiMin(byte netId, NodeAddress destination) {
        ConfigNodePacket cp = new ConfigNodePacket(netId, sinkAddress, destination);
        cp.setReadDefaultRssiMinValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
        return ((ConfigNodePacket) response).getDefaultRssiMinValue();
    }

    /**
     * This method adds a new address in the list of addresses accepted by the
     * node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param newAddr the address.
     */
    @Override
    public final void addAcceptedAddress(byte netId, NodeAddress destination,
            NodeAddress newAddr) {
        ConfigAcceptedIdPacket cp = new ConfigAcceptedIdPacket(netId, sinkAddress, destination);
        cp.setAddAcceptedAddressValue(newAddr)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method removes an address in the list of addresses accepted by the
     * node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param newAddr the address.
     */
    @Override
    public final void removeAcceptedAddress(byte netId, NodeAddress destination,
            NodeAddress newAddr) {
        ConfigAcceptedIdPacket cp = new ConfigAcceptedIdPacket(netId, sinkAddress, destination);
        cp.setRemoveAcceptedAddressValue(newAddr)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method returns the list of addresses accepted by the node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @return returns the list of accepted Addresses.
     */
    @Override
    public final List<NodeAddress> getAcceptedAddressesList(byte netId,
            NodeAddress destination) {
        ConfigAcceptedIdPacket cp = new ConfigAcceptedIdPacket(netId, sinkAddress, destination);
        cp.setReadAcceptedAddressesValue()
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return null;
        }
        return new ConfigAcceptedIdPacket(response).getAcceptedAddressesValues();
    }

    /**
     * This method installs a rule in the node
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param rule the rule to be installed.
     */
    @Override
    public final void addRule(byte netId, NodeAddress destination,
            FlowTableEntry rule) {
        /*
         ConfigPacket cp = new ConfigPacket();
         cp.setAddRuleValue(rule)
         .setNet(netId)
         .setDst(destination)
         .setSrc(sinkAddress)
         .setNxh(sinkAddress);
         sendNetworkPacket(cp);
         */

        ResponsePacket rp = new ResponsePacket(netId, sinkAddress, destination, rule);
        rp.setNxh(sinkAddress);
        sendNetworkPacket(rp);
    }

    /**
     * This method removes a rule in the node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param index index of the erased row.
     */
    @Override
    public final void removeRule(byte netId,
            NodeAddress destination, int index) {
        ConfigRulePacket cp = new ConfigRulePacket(netId, sinkAddress, destination);
        cp.setRemoveRuleAtPositionValue(index)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method removes a rule in the node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param rule the rule to be removed.
     */
    @Override
    public final void removeRule(byte netId, NodeAddress destination,
            FlowTableEntry rule) {
        ConfigRulePacket cp = new ConfigRulePacket(netId, sinkAddress, destination);
        cp.setRemoveRuleValue(rule)
                .setNxh(sinkAddress);
        sendNetworkPacket(cp);
    }

    /**
     * This method gets the WISE flow table of a node.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @return returns the list of the entries in the WISE Flow Table.
     */
    @Override
    public final List<FlowTableEntry> getRules(byte netId,
            NodeAddress destination) {
        List<FlowTableEntry> list = new ArrayList<>(SDN_WISE_RLS_MAX);
        for (int i = 0; i < SDN_WISE_RLS_MAX; i++) {
            list.add(i, getRuleAtPosition(netId, destination, i));
        }
        return list;
    }

    /**
     * This method gets the WISE flow table entry of a node at position n.
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param index position of the entry in the table.
     * @return returns the list of the entries in the WISE Flow Table.
     */
    @Override
    public final FlowTableEntry getRuleAtPosition(byte netId,
            NodeAddress destination, int index) {
        ConfigRulePacket cp = new ConfigRulePacket(netId, sinkAddress, destination);
        cp.setGetRuleAtIndexValue(index)
                .setNxh(sinkAddress);
        ConfigPacket response;
        try {
            response = sendQuery(cp);
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return null;
        }
        return ((ConfigRulePacket) response).getRule();
    }

    @Override
    public void sendFunction(
            byte netId,
            NodeAddress dest,
            byte id,
            String className
    ) {
        try {
            URL main = FunctionInterface.class.getResource(className);
            File path = new File(main.getPath());
            byte[] buf = Files.readAllBytes(path.toPath());
            List<ConfigFunctionPacket> ll = createPackets(
                    netId, sinkAddress, dest, sinkAddress, id, buf);
            Iterator<ConfigFunctionPacket> llIterator = ll.iterator();
            if (llIterator.hasNext()) {
                this.sendNetworkPacket(llIterator.next());
                Thread.sleep(200);
                while (llIterator.hasNext()) {
                    this.sendNetworkPacket(llIterator.next());
                }
            }
        } catch (IOException | InterruptedException ex) {
            log(Level.SEVERE, ex.toString());
        }
    }

    /**
     * This method gets the NetworkGraph of the controller.
     *
     * @return returns a NetworkGraph object.
     */
    @Override
    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }

    private ConfigPacket sendQuery(ConfigPacket cp) throws TimeoutException {

        sendNetworkPacket(cp);

        try {
            Thread.sleep(RESPONSE_TIMEOUT);
        } catch (InterruptedException ex) {
            log(Level.SEVERE, ex.toString());
        }

        String key;

        if (cp.getConfigId() == (GET_RULE_AT)) {
            key = cp.getNet() + " "
                    + cp.getDst() + " "
                    + cp.getConfigId() + " "
                    + cp.getValue();
        } else {
            key = cp.getNet() + " "
                    + cp.getDst() + " "
                    + cp.getConfigId();
        }
        if (configCache.containsKey(key)) {
            return configCache.remove(key);
        } else {
            throw new TimeoutException("No answer from the node");
        }
    }

    /**
     * This method is used to register the AbstractController with the
     * FlowVisor.
     */
    //TODO we need to implement same sort of security check/auth.
    private void register() {
    }

    private NetworkPacket putInRequestCache(RequestPacket rp) {
        if (rp.getTotal() == 1) {
            return new NetworkPacket(rp.getData());
        }

        String key = rp.getSrc() + "." + rp.getId();

        if (requestCache.containsKey(key)) {
            RequestPacket p0 = requestCache.remove(key);
            return RequestPacket.mergePackets(p0, rp);
        } else {
            requestCache.put(key, rp);
        }
        return null;
    }
    /**
     * This method sends a generic message to a node. The message is represented
     * by a NetworkPacket.
     *
     * @param packet the packet to be sent.
     */
    protected void sendNetworkPacket(NetworkPacket packet) {
        lower.send(packet.toByteArray());
    }

    private class Worker implements Runnable {

        private final ArrayBlockingQueue<NetworkPacket> bQ;
        boolean isStopped;

        Worker(ArrayBlockingQueue<NetworkPacket> bQ) {
            this.bQ = bQ;
        }

        @Override
        public void run() {
            while (!isStopped) {
                try {
                    managePacket(bQ.take());
                } catch (InterruptedException ex) {
                    isStopped = true;
                }
            }
        }
    }
}
