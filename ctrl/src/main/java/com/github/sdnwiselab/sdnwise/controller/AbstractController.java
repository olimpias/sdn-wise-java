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
import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import static com.github.sdnwiselab.sdnwise.util.Utils.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
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

    final static int FLOW_TABLE_SIZE = 16;
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

    @Override
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

                String key;
                if (cp.getConfigId() == (GET_RULE)) {
                    key = cp.getNet() + " " + cp.getSrc() + " "
                            + cp.getConfigId() + " " + cp.getValue()[0];
                } else {
                    key = cp.getNet() + " " + cp.getSrc() + " "
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
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param path the list of all the NodeAddresses in the path.
     */
    @Override
    public final void sendPath(byte net, NodeAddress dst, List<NodeAddress> path) {
        OpenPathPacket op = new OpenPathPacket(net, sinkAddress, dst, path);
        sendNetworkPacket(op);
    }

    /**
     * This method sets the address of a node. The new address value is passed
     * using two bytes.
     *
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newAddress the new address.
     */
    @Override
    public final void setNodeAddress(byte net, NodeAddress dst, NodeAddress newAddress) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, MY_ADDRESS, newAddress.getArray());
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the address of a node.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @return returns the NodeAddress of a node, null if it does exists.
     */
    @Override
    public final NodeAddress getNodeAddress(byte net, NodeAddress dst) {
        return new NodeAddress(getNodeValue(net, dst, MY_ADDRESS));
    }

    /**
     * This method sets the Network ID of a node. The new value is passed using
     * a byte.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     */
    @Override
    public final void resetNode(byte net, NodeAddress dst) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, RESET);
        sendNetworkPacket(cp);
    }

    private int getNodeValue(byte net, NodeAddress dst, ConfigProperty cfp) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, cfp);
        try {
            byte res[] = sendQuery(cp).getValue();
            if (cfp.size == 1) {
                return res[0] & 0xFF;
            } else {
                return mergeBytes(res[0], res[1]);
            }
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
    }

    /**
     * This method sets the Network ID of a node. The new value is passed using
     * a byte.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param newNetId value of the new net ID
     */
    @Override
    public final void setNodeNet(byte net, NodeAddress dst, byte newNetId) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, MY_NET, new byte[]{newNetId});
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the Network ID of a node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the nedId, -1 if not found.
     */
    @Override
    public final int getNodeNet(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, MY_NET);
    }

    /**
     * This method sets the beacon period of a node. The new value is passed
     * using a short.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param period beacon period in seconds
     */
    @Override
    public final void setNodeBeaconPeriod(byte net, NodeAddress dst, short period) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, BEACON_PERIOD, splitInteger(period));
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the beacon period of a node.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @return returns the beacon period, -1 if not found
     */
    @Override
    public final int getNodeBeaconPeriod(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, BEACON_PERIOD);
    }

    /**
     * This method sets the report period of a node. The new value is passed
     * using a short.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param period report period in seconds
     */
    @Override
    public final void setNodeReportPeriod(byte net, NodeAddress dst, short period) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, REPORT_PERIOD, splitInteger(period));
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the report period of a node.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @return returns the report period, -1 if not found
     */
    @Override
    public final int getNodeReportPeriod(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, BEACON_PERIOD);
    }

    /**
     * This method sets the update table period of a node. The new value is
     * passed using a short.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param period update table period in seconds (TODO check)
     */
    @Override
    public final void setNodeEntryTtl(byte net, NodeAddress dst, short period) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, RULE_TTL, splitInteger(period));
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the Update table period of a node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the updateTablePeriod, -1 if not found.
     */
    @Override
    public final int getNodeEntryTtl(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, RULE_TTL);
    }

    /**
     * This method sets the maximum time to live for each message sent by a
     * node. The new value is passed using a byte.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newTtl time to live in number of hops.
     */
    @Override
    public final void setNodePacketTtl(byte net, NodeAddress dst, byte newTtl) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, PACKET_TTL, new byte[]{newTtl});
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the maximum time to live for each message sent by a
     * node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the maximum time to live, -1 if not found.
     */
    @Override
    public final int getNodePacketTtl(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, PACKET_TTL);
    }

    /**
     * This method sets the minimum RSSI in order to consider a node as a
     * neighbor.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newRssi new threshold rssi value.
     */
    @Override
    public final void setNodeRssiMin(byte net, NodeAddress dst, byte newRssi) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, PACKET_TTL, new byte[]{newRssi});
        sendNetworkPacket(cp);
    }

    /**
     * This method reads the minimum RSSI in order to consider a node as a
     * neighbor.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the minimum RSSI, -1 if not found.
     */
    @Override
    public final int getNodeRssiMin(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, RSSI_MIN);
    }

    /**
     * This method adds a new address in the list of addresses accepted by the
     * node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newAddr the address.
     */
    @Override
    public final void addNodeAlias(byte net, NodeAddress dst,
            NodeAddress newAddr) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, ADD_ALIAS, newAddr.getArray());
        sendNetworkPacket(cp);
    }

    /**
     * This method removes an address in the list of addresses accepted by the
     * node at position index.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index the address.
     */
    @Override
    public final void removeNodeAlias(byte net, NodeAddress dst, byte index) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, REM_ALIAS, new byte[]{index});
        sendNetworkPacket(cp);
    }

    /**
     * This method returns the list of addresses accepted by the node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the list of accepted Addresses.
     */
    @Override
    public final List<NodeAddress> getNodeAliases(byte net, NodeAddress dst) {
        List<NodeAddress> list = new LinkedList<>();
        for (int i = 0; i < FLOW_TABLE_SIZE; i++) {
            NodeAddress na = getNodeAlias(net, dst, (byte) i);
            if (na != null) {
                list.add(i, na);
            } else {
                break;
            }
        }
        return list;
    }

    /**
     * This method returns the list of addresses accepted by the node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index
     * @return returns the list of accepted Addresses.
     */
    @Override
    public NodeAddress getNodeAlias(byte net, NodeAddress dst, byte index) {
        try {
            ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, GET_ALIAS);
            cp.setValue(new byte[]{(byte) index}, GET_RULE.size);
            ConfigPacket response = sendQuery(cp);
            byte[] rule = Arrays.copyOfRange(response.getValue(), 1, response.getPayloadSize() - 1);
            return new NodeAddress(rule);
        } catch (TimeoutException ex) {
            return null;
        }

    }

    /**
     * This method installs a rule in the node
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param rule the rule to be installed.
     */
    @Override
    public final void addNodeRule(byte netId, NodeAddress destination, FlowTableEntry rule) {
        ResponsePacket rp = new ResponsePacket(netId, sinkAddress, destination, rule);
        sendNetworkPacket(rp);
    }

    /**
     * This method removes a rule in the node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index index of the erased row.
     */
    @Override
    public final void removeNodeRule(byte net, NodeAddress dst, byte index) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, REM_RULE, new byte[]{index});
        sendNetworkPacket(cp);
    }

    /**
     * This method gets the WISE flow table of a node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the list of the entries in the WISE Flow Table.
     */
    @Override
    public final List<FlowTableEntry> getNodeRules(byte net, NodeAddress dst) {
        List<FlowTableEntry> list = new ArrayList<>(FLOW_TABLE_SIZE);
        for (int i = 0; i < FLOW_TABLE_SIZE; i++) {
            FlowTableEntry fte = getNodeRule(net, dst, i);
            if (fte != null) {
                list.add(i, fte);
            } else {
                break;
            }
        }
        return list;
    }

    /**
     * This method gets the WISE flow table entry of a node at position n.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index position of the entry in the table.
     * @return returns the list of the entries in the WISE Flow Table.
     */
    @Override
    public final FlowTableEntry getNodeRule(byte net, NodeAddress dst, int index) {
        try {
            ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, GET_RULE);
            cp.setValue(new byte[]{(byte) index}, GET_RULE.size);
            ConfigPacket response = sendQuery(cp);
            byte[] rule = Arrays.copyOfRange(response.getValue(), 1, response.getPayloadSize() - 1);
            if (rule.length > 0) {
                return new FlowTableEntry(rule);
            } else {
                return null;
            }
        } catch (TimeoutException ex) {
            return null;
        }
    }

    @Override
    public void addNodeFunction(byte net, NodeAddress dst, byte id, String className) {
        try {
            URL main = FunctionInterface.class.getResource(className);
            File path = new File(main.getPath());
            byte[] buf = Files.readAllBytes(path.toPath());
            List<ConfigPacket> ll = createPackets(
                    net, sinkAddress, dst, id, buf);
            Iterator<ConfigPacket> llIterator = ll.iterator();
            if (llIterator.hasNext()) {
                sendNetworkPacket(llIterator.next());
                Thread.sleep(200);
                while (llIterator.hasNext()) {
                    sendNetworkPacket(llIterator.next());
                }
            }
        } catch (IOException | InterruptedException ex) {
            log(Level.SEVERE, ex.toString());
        }

    }

    @Override
    public void removeNodeFunction(byte net, NodeAddress dst, byte index) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, REM_FUNCTION, new byte[]{index});
        sendNetworkPacket(cp);
    }

    public static List<ConfigPacket> createPackets(
            byte netId,
            NodeAddress src,
            NodeAddress dst,
            byte id,
            byte[] buf) {
        LinkedList<ConfigPacket> ll = new LinkedList<>();

        int FUNCTION_HEADER_LEN = 4;
        int FUNCTION_PAYLOAD_LEN
                = NetworkPacket.MAX_PACKET_LENGTH
                - (SDN_WISE_DFLT_HDR_LEN + FUNCTION_HEADER_LEN);

        int packetNumber = buf.length / FUNCTION_PAYLOAD_LEN;
        int remaining = buf.length % FUNCTION_PAYLOAD_LEN;
        int totalPackets = packetNumber + (remaining > 0 ? 1 : 0);
        int pointer = 0;
        int i = 0;

        if (packetNumber < 256) {
            if (packetNumber > 0) {
                for (i = 0; i < packetNumber; i++) {
                    byte[] payload = ByteBuffer.allocate(FUNCTION_PAYLOAD_LEN + 3)
                            .put(id)
                            .put((byte) (i + 1))
                            .put((byte) totalPackets)
                            .put(Arrays.copyOfRange(buf, pointer, pointer + FUNCTION_PAYLOAD_LEN)).array();
                    pointer += FUNCTION_PAYLOAD_LEN;
                    ConfigPacket np = new ConfigPacket(netId, src, dst, ADD_FUNCTION, payload);
                    ll.add(np);
                }
            }

            if (remaining > 0) {
                byte[] payload = ByteBuffer.allocate(remaining + 3)
                        .put(id)
                        .put((byte) (i + 1))
                        .put((byte) totalPackets)
                        .put(Arrays.copyOfRange(buf, pointer, pointer + remaining)).array();
                ConfigPacket np = new ConfigPacket(netId, src, dst, ADD_FUNCTION, payload);
                ll.add(np);
            }
        }
        return ll;
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

        if (cp.getConfigId() == (GET_RULE)) {
            key = cp.getNet() + " "
                    + cp.getDst() + " "
                    + cp.getConfigId() + " "
                    + cp.getValue()[0];
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
        packet.setNxh(sinkAddress);
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
