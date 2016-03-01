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
package com.github.sdnwiselab.sdnwise.flowvisor;

import com.github.sdnwiselab.sdnwise.adapter.AbstractAdapter;
import com.github.sdnwiselab.sdnwise.adapter.AdapterUdp;
import com.github.sdnwiselab.sdnwise.controlplane.*;
import com.github.sdnwiselab.sdnwise.packet.DataPacket;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.packet.ReportPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Observable;
import java.util.Set;
import java.util.logging.*;

/**
 * This class registers Nodes and Controllers of the SDN-WISE Network.
 *
 * This class is created by FlowVisorFactory. It permits Network slicing and
 * implements Runnable and the Observer pattern.
 *
 * @author Sebastiano Milardo
 * @version 0.1
 */
public class FlowVisor extends ControlPlaneLayer {

    // to avoid garbage collection of the logger
    protected static final Logger LOGGER = Logger.getLogger("FLW");

    private final HashMap<InetSocketAddress, Set<NodeAddress>> controllerMapping;
    private final HashMap<InetSocketAddress, InetSocketAddress> applicationMapping;

    /**
     * Constructor for the FlowVisor. It defines Lower and Upper
     * AbstractAdapter.
     */
    FlowVisor(AbstractAdapter lower, AdapterUdp upper) {
        super("FLW", lower, upper);
        ControlPlaneLogger.setupLogger(layerShortName);

        controllerMapping = new HashMap<>();
        applicationMapping = new HashMap<>();
    }

    /**
     * This method permits to register a Controller to this FlowVisor and its
     * Nodes.
     *
     * @param controller Controller Identity to register
     * @param set Set of Nodes to register
     */
    public final void addController(InetSocketAddress controller, Set<NodeAddress> set) {
        controllerMapping.put(controller, set);
    }

    /**
     * This method permits to register an Application to this FlowVisor and its
     * Controller.
     *
     * @param application Application Identity to register
     * @param controller Controller Identity for the Application
     */
    public final void addApplication(InetSocketAddress application, InetSocketAddress controller) {
        applicationMapping.put(application, controller);
    }

    /**
     * Remove a Controller from this FlowVisor
     *
     * @param controller Controller Identity to remove
     */
    public final void removeController(InetSocketAddress controller) {
        controllerMapping.remove(controller);
    }

    /**
     * Remove an Application from this FlowVisor
     *
     * @param application Application Identity to remove
     */
    public final void removeApplication(InetSocketAddress application) {
        applicationMapping.remove(application);
    }

    @Override
    public final void update(Observable o, Object arg) {
        if (o.equals(lower)) {
            // if it is a data packet send to the application, else send it to
            // the controller
            byte[] data = (byte[]) arg;
            NetworkPacket np = new NetworkPacket(data);
            switch (np.getType()) {
                case DATA:
                    manageData(data);
                    break;
                case REPORT:
                    manageReports(data);
                    break;
                default:
                    manageRequests(data);
                    break;
            }
        } else if (o.equals(upper)) {
            manageResponses((byte[]) arg);
        }
    }

    /**
     * This method consists of a way to manage reports.
     *
     * @param data Byte Array contains data message
     */
    private void manageReports(byte[] data) {
        controllerMapping.entrySet().stream().forEach((set) -> {
            ReportPacket pkt = new ReportPacket(
                    Arrays.copyOf(data, data.length));
            HashMap<NodeAddress, Byte> map = pkt.getNeighborsHashMap();
            if (set.getValue().contains(pkt.getSrc())) {
                boolean mod = false;
                final int numNeigh = pkt.getNeigh();
                for (int i = 0; i < numNeigh; i++) {
                    NodeAddress tmp = pkt.getNeighbourAddress(i);
                    if (!set.getValue().contains(tmp)) {
                        map.remove(tmp);
                        mod = true;
                    }
                }

                if (mod) {
                    pkt.setNeighborsHashMap(map);
                }

                ((AdapterUdp) upper).send(pkt.toByteArray(), set.getKey().getAddress().getHostAddress(),
                        set.getKey().getPort());
            }
        });
    }

    /**
     * This method consists of a way to manage requests.
     *
     * @param data Byte Array contains data message
     */
    private void manageRequests(byte[] data) {
        NetworkPacket pkt = new NetworkPacket(data);
        controllerMapping.entrySet().stream().filter((set) -> (set.getValue().contains(pkt.getSrc())
                && set.getValue().contains(pkt.getDst()))).map((set) -> {
            ((AdapterUdp) upper).send(data, set.getKey().getAddress().getHostAddress(),
                    set.getKey().getPort());
            return set;
        }).forEach((set) -> {
            log(Level.INFO, "Sending request to " + set.getKey().getAddress() + ":"
                    + set.getKey().getPort());
        });
    }

    private void manageData(byte[] data) {
        DataPacket pkt = new DataPacket(data);

        applicationMapping.keySet().stream().forEach((app) -> {
            Set<NodeAddress> nodes = controllerMapping.get(applicationMapping.get(app));
            if (nodes.contains(pkt.getSrc())
                    && nodes.contains(pkt.getDst())) {
                ((AdapterUdp) upper).send(data, app.getAddress().getHostAddress(),
                        app.getPort());
                log(Level.INFO, "Sending data to " + app.getAddress() + ":"
                        + app.getPort());
            }
        });
    }

    private void manageResponses(byte[] data) {
        log(Level.INFO, "Receiving " + Arrays.toString(data));
        lower.send(data);
    }

    @Override
    public void setupLayer() {
    }
}
