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
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty;
import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import static com.github.sdnwiselab.sdnwise.util.Utils.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import net.jodah.expiringmap.ExpiringMap;

/**
 * Representation of the sensor network and resolver all the routing requests
 * coming from the network itself. This abstract class has two main methods.
 * manageRoutingRequest and graphUpdate. The first is called when a request is
 * coming from the network while the latter is called when something in the
 * topology of the network changes.
 * <p>
 * There are two main implementation of this class: ControllerDijkstra and
 * AbstractController Static.
 * <p>
 * This class also offers methods to send messages and configure the nodes in
 * the network.
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractController extends ControlPlaneLayer implements ControllerInterface {

    /**
     * Timeout for requests in cache.
     */
    protected static final int CACHE_EXP_TIME = 5;
    /**
     * To avoid garbage collection of the logger.
     */
    protected static final Logger LOGGER = Logger.getLogger("CTRL");
    /**
     * Timeout for a node request. Increase when using COOJA.
     */
    protected static final int RESPONSE_TIMEOUT = 300;

    private final ArrayBlockingQueue<NetworkPacket> bQ = new ArrayBlockingQueue<>(1000);
    private final Map<String, ConfigPacket> configCache = ExpiringMap
            .builder().expiration(CACHE_EXP_TIME, TimeUnit.SECONDS).build();
    private final InetSocketAddress id;
    private final Map<String, RequestPacket> requestCache = ExpiringMap
            .builder().expiration(CACHE_EXP_TIME, TimeUnit.SECONDS).build();
    private NodeAddress sinkAddress;

    protected final NetworkGraph networkGraph;
    protected final HashMap<NodeAddress, LinkedList<NodeAddress>> results = new HashMap<>();

    public static List<ConfigPacket> createPackets(
            final byte net,
            final NodeAddress src,
            final NodeAddress dst,
            final byte id,
            final byte[] buf) {
        LinkedList<ConfigPacket> ll = new LinkedList<>();

        int FUNCTION_HEADER_LEN = 4;
        int FUNCTION_PAYLOAD_LEN = MAX_PACKET_LENGTH
                - (DFLT_HDR_LEN + FUNCTION_HEADER_LEN);

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
                    ConfigPacket np = new ConfigPacket(net, src, dst, ADD_FUNCTION, payload);
                    ll.add(np);
                }
            }

            if (remaining > 0) {
                byte[] payload = ByteBuffer.allocate(remaining + 3)
                        .put(id)
                        .put((byte) (i + 1))
                        .put((byte) totalPackets)
                        .put(Arrays.copyOfRange(buf, pointer, pointer + remaining)).array();
                ConfigPacket np = new ConfigPacket(net, src, dst, ADD_FUNCTION, payload);
                ll.add(np);
            }
        }
        return ll;
    }

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
    public final void addNodeAlias(byte net, NodeAddress dst,
            NodeAddress newAddr) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, ADD_ALIAS, newAddr.getArray());
        sendNetworkPacket(cp);
    }

    @Override
    public void addNodeFunction(byte net, NodeAddress dst, byte id, String className) {
        try {
            InputStream is = FunctionInterface.class.getResourceAsStream(className);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();

            List<ConfigPacket> ll = createPackets(
                    net, sinkAddress, dst, id, buffer.toByteArray());
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
    public final void addNodeRule(byte net, NodeAddress destination, FlowTableEntry rule) {
        ResponsePacket rp = new ResponsePacket(net, sinkAddress, destination, rule);
        sendNetworkPacket(rp);
    }

    @Override
    public final InetSocketAddress getId() {
        return id;
    }

    @Override
    public NetworkGraph getNetworkGraph() {
        return networkGraph;
    }

    @Override
    public final NodeAddress getNodeAddress(byte net, NodeAddress dst) {
        return new NodeAddress(getNodeValue(net, dst, MY_ADDRESS));
    }

    @Override
    public NodeAddress getNodeAlias(byte net, NodeAddress dst, byte index) {
        try {
            ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, GET_ALIAS);
            cp.setParams(new byte[]{(byte) index}, GET_RULE.getSize());
            ConfigPacket response = sendQuery(cp);
            byte[] rule = Arrays.copyOfRange(response.getParams(), 1, response.getPayloadSize() - 1);
            return new NodeAddress(rule);
        } catch (TimeoutException ex) {
            return null;
        }

    }

    @Override
    public final List<NodeAddress> getNodeAliases(final byte net,
            final NodeAddress dst) {
        List<NodeAddress> list = new LinkedList<>();
        NodeAddress na;
        int i = 0;
        while ((na = getNodeAlias(net, dst, (byte) i)) != null) {
            list.add(i, na);
            i++;
        }
        return list;
    }

    @Override
    public final int getNodeBeaconPeriod(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, BEACON_PERIOD);
    }

    @Override
    public final int getNodeEntryTtl(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, RULE_TTL);
    }

    @Override
    public final int getNodeNet(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, MY_NET);
    }

    @Override
    public final int getNodePacketTtl(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, PACKET_TTL);
    }

    @Override
    public final int getNodeReportPeriod(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, REPORT_PERIOD);
    }

    @Override
    public final int getNodeRssiMin(byte net, NodeAddress dst) {
        return getNodeValue(net, dst, RSSI_MIN);
    }

    @Override
    public final FlowTableEntry getNodeRule(byte net, NodeAddress dst, int index) {
        try {
            ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, GET_RULE);
            cp.setParams(new byte[]{(byte) index}, GET_RULE.getSize());
            ConfigPacket response = sendQuery(cp);
            byte[] rule = Arrays.copyOfRange(response.getParams(), 1, response.getPayloadSize() - 1);
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
    public final List<FlowTableEntry> getNodeRules(final byte net,
            final NodeAddress dst) {
        List<FlowTableEntry> list = new ArrayList<>();
        FlowTableEntry fte;
        int i = 0;
        while ((fte = getNodeRule(net, dst, i)) != null) {
            list.add(i, fte);
            i++;
        }
        return list;
    }

    @Override
    public NodeAddress getSinkAddress() {
        return sinkAddress;
    }

    public void managePacket(final NetworkPacket data) {

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
                            + cp.getConfigId() + " " + cp.getParams()[0];
                } else {
                    key = cp.getNet() + " " + cp.getSrc() + " "
                            + cp.getConfigId();
                }
                configCache.put(key, cp);
                break;
            case REG_PROXY:
                sinkAddress = data.getSrc();
                break;

            default:
                break;
        }
    }

    @Override
    public final void removeNodeAlias(byte net, NodeAddress dst, byte index) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, REM_ALIAS, new byte[]{index});
        sendNetworkPacket(cp);
    }

    @Override
    public void removeNodeFunction(byte net, NodeAddress dst, byte index) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, REM_FUNCTION, new byte[]{index});
        sendNetworkPacket(cp);
    }

    @Override
    public final void removeNodeRule(byte net, NodeAddress dst, byte index) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, REM_RULE, new byte[]{index});
        sendNetworkPacket(cp);
    }

    @Override
    public final void resetNode(byte net, NodeAddress dst) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, RESET);
        sendNetworkPacket(cp);
    }

    @Override
    public final void sendPath(byte net, NodeAddress dst, List<NodeAddress> path) {
        OpenPathPacket op = new OpenPathPacket(net, sinkAddress, dst, path);
        sendNetworkPacket(op);
    }

    @Override
    public final void setNodeAddress(byte net, NodeAddress dst, NodeAddress newAddress) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, MY_ADDRESS, newAddress.getArray());
        sendNetworkPacket(cp);
    }

    @Override
    public final void setNodeBeaconPeriod(byte net, NodeAddress dst, short period) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, BEACON_PERIOD, splitInteger(period));
        sendNetworkPacket(cp);
    }

    @Override
    public final void setNodeEntryTtl(byte net, NodeAddress dst, short period) {
        //TODO TTL should be in seconds
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, RULE_TTL, splitInteger(period));
        sendNetworkPacket(cp);
    }

    @Override
    public final void setNodeNet(byte net, NodeAddress dst, byte newNet) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, MY_NET, new byte[]{newNet});
        sendNetworkPacket(cp);
    }

    @Override
    public final void setNodePacketTtl(byte net, NodeAddress dst, byte newTtl) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, PACKET_TTL, new byte[]{newTtl});
        sendNetworkPacket(cp);
    }

    @Override
    public final void setNodeReportPeriod(byte net, NodeAddress dst, short period) {
        ConfigPacket cp = new ConfigPacket(
                net, sinkAddress, dst, REPORT_PERIOD, splitInteger(period));
        sendNetworkPacket(cp);
    }

    @Override
    public final void setNodeRssiMin(byte net, NodeAddress dst, byte newRssi) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, RSSI_MIN, new byte[]{newRssi});
        sendNetworkPacket(cp);
    }

    @Override
    public void setupLayer() {
        new Thread(new Worker(bQ)).start();
        networkGraph.addObserver(this);
        register();
        setupNetwork();
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

    private int getNodeValue(final byte net, final NodeAddress dst,
            final ConfigProperty cfp) {
        ConfigPacket cp = new ConfigPacket(net, sinkAddress, dst, cfp);
        try {
            byte[] res = sendQuery(cp).getParams();
            if (cfp.getSize() == 1) {
                return Byte.toUnsignedInt(res[0]);
            } else {
                return mergeBytes(res[0], res[1]);
            }
        } catch (TimeoutException ex) {
            log(Level.SEVERE, ex.toString());
            return -1;
        }
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

    private void register() {
        //TODO we need to implement same sort of security check/auth.
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
                    + cp.getParams()[0];
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
        private boolean isStopped;

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
