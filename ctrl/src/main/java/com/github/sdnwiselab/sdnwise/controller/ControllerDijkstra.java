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
import com.github.sdnwiselab.sdnwise.flow.FlowPathManager;
import com.github.sdnwiselab.sdnwise.flow.FlowPathService;
import com.github.sdnwiselab.sdnwise.flow.SrcDstPair;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.packet.RequestPacket;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Node;

/**
 * Representation of a Dijkstra routing algorithm based SDN-WISE controller.
 * When a request from the network is sent, this class sends a OpenPath message
 * with the shortest p. No action is taken if the topology of the network
 * changes.
 *
 * @author Sebastiano Milardo
 */
public final class ControllerDijkstra extends AbstractController {

    /**
     * Used to calculate a path according to Dijkstra's algorithm.
     */
    private final Dijkstra dijkstra;

    private final Dijkstra dijkstraCal;

    /**
     * Last modification time of the networkGraph object.
     */
    private long lastModification = -1;
    /**
     * Last Node address for which there are calculated paths.
     */
    private String lastSource = "";

    /**
     * Creates a ControllerDijkstra object.
     *
     * @param id ControllerId object.
     * @param lower Lower Adpater object.
     * @param networkGraph NetworkGraph object.
     */
    public ControllerDijkstra(final InetSocketAddress id,
            final List<AbstractAdapter> lower,
            final NetworkGraph networkGraph,
            final NodeAddress sinkAddress) {
        super(id, lower, networkGraph, sinkAddress);
        dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
        dijkstraCal = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
    }

    @Override
    public void graphUpdate() {
        log(Level.INFO, "Graph update is received");
        NetworkGraph network = getNetworkGraph();
        FlowPathService service = FlowPathManager.SingletonInstance();
        LinkedList<NodeAddress> nodeAddresses = new LinkedList<>();
        Node source;
        for (SrcDstPair pair : service.getPairs()) {
            nodeAddresses.clear();
            source = network.getNode(pair.getSrc());
            if (source == null) {
                log(Level.WARNING,"Source is not found +"+pair.getSrc());
                continue;
            }
            dijkstraCal.init(network.getGraph());
            dijkstraCal.setSource(source);
            dijkstraCal.compute();
            dijkstraCal.getPathNodes(network
                    .getNode(pair.getDst()));
            for (Node node : dijkstraCal.getPathNodes(network
                    .getNode(pair.getDst()))) {
                nodeAddresses.push(node.getAttribute("nodeAddress"));
            }
            if (nodeAddresses.isEmpty()) {
                continue;
            }
            if(!service.getPath(pair).equals(nodeAddresses)) {
                updatePath(pair, nodeAddresses);
                service.addPath(pair, nodeAddresses);
                getResults().put(nodeAddresses.getLast(),nodeAddresses);
            }
        }
    }

    @Override
    public void manageRoutingRequest(final RequestPacket req,
            final NetworkPacket data) {
        log(Level.INFO, "Manage Routing Req");

        log(Level.INFO, data.toString());
        NetworkGraph network = getNetworkGraph();
        HashMap<NodeAddress, LinkedList<NodeAddress>> results = getResults();

        String dst = data.getNet() + "." + data.getDst();
        String src = data.getNet() + "." + req.getSrc();
        SrcDstPair pair = new SrcDstPair(src,dst, data.getNet());
        if (src.equals(dst)) {
            return;
        }
        Node srcNode = network.getNode(src);
        Node dstNode = network.getNode(dst);

        LinkedList<NodeAddress> p = null;

        if (srcNode == null || dstNode == null) {
            return;
        }
        if (!lastSource.equals(src) || lastModification
                != network.getLastModification()) {
            results.clear();
            dijkstra.init(network.getGraph());
            dijkstra.setSource(network.getNode(src));
            dijkstra.compute();
            lastSource = src;
            lastModification = network.getLastModification();
        } else {
            p = results.get(data.getDst());
        }
        if (p == null) {
            p = new LinkedList<>();
            for (Node node : dijkstra.getPathNodes(network
                    .getNode(dst))) {
                p.push(node.getAttribute("nodeAddress"));
            }
            log(Level.INFO, "Path: " + p);
            results.put(data.getDst(), p);
        }
        if (p.size() > 1) {
            updatePath(pair, p);
            data.setSrc(req.getSrc());
            data.setNxh(getSinkAddress());
            sendNetworkPacket(data);
        }
    }

    private void updatePath(SrcDstPair pair,LinkedList<NodeAddress> path) {
        FlowPathService service = FlowPathManager.SingletonInstance();
        service.addPath(pair, path);
        sendPath((byte) pair.getNetworkId(), path.getFirst(), path);
        Collections.reverse(path);
        sendPath((byte) pair.getNetworkId(), path.getFirst(), path);
    }

    @Override
    public void setupNetwork() {
        // Nothing to do here
    }
}
