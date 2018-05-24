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
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

/**
 * Representation of a Dijkstra routing algorithm based SDN-WISE controller.
 * When a request from the network is sent, this class sends a OpenPath message
 * with the shortest p. No action is taken if the topology of the network
 * changes.
 *
 * @author Sebastiano Milardo
 */
public final class ControllerStatic extends AbstractController {



    /**
     * Used to calculate a path according to Dijkstra's algorithm.
     */
    private final Dijkstra dijkstra;
    /**
     * Last modification time of the networkGraph object.
     */
    private long lastModification = -1;
    /**
     * Last Node address for which there are calculated paths.
     */
    private String lastSource = "";
    /**
     * Contains the paths to reach the mobile node
     */
    private HashMap<String, LinkedList<NodeAddress>> buffer = new HashMap<>();
    /**
     * Current best edge between the mobile node and the fixed nodes
     */
    private String currentBestEdge;

    /**
     * Creates a ControllerDijkstra object.
     *
     * @param id ControllerId object.
     * @param lower Lower Adpater object.
     * @param networkGraph NetworkGraph object.
     */
    public ControllerStatic(final InetSocketAddress id,
                              final List<AbstractAdapter> lower,
                              final NetworkGraph networkGraph) {
        super(id, lower, networkGraph);
        dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");

        NodeAddress[] n = new NodeAddress[5];

        for (int i = 0 ; i<n.length;i++){
            n[i] = new NodeAddress(i+1);
        }

        LinkedList<NodeAddress> p1 = new LinkedList<>();
        LinkedList<NodeAddress> p2 = new LinkedList<>();
        LinkedList<NodeAddress> p3 = new LinkedList<>();

        p1.add(n[4]);
        p1.add(n[1]);
        p1.add(n[0]);
        p1.add(n[3]);

        p2.add(n[4]);
        p2.add(n[1]);
        p2.add(n[3]);

        p3.add(n[4]);
        p3.add(n[1]);
        p3.add(n[2]);
        p3.add(n[3]);

        buffer.put("1.0.4-1.0.1",p1);
        buffer.put("1.0.4-1.0.2",p2);
        buffer.put("1.0.4-1.0.3",p3);

    }

    @Override
    public void graphUpdate() {
        NetworkGraph network = getNetworkGraph();
        Node srcNode = network.getNode("1.0.4");
        String bestEdge = null;
        if (srcNode != null) {
            int rssi = 255;
            for (Edge e : srcNode.getLeavingEdgeSet()) {
                // Find the edge with the best rssi
                int newRssi = (int) e.getAttribute("length");
                System.out.println(e.getId() + " " + newRssi);
                if (newRssi <= rssi) {
                    rssi = newRssi;
                    bestEdge = e.getId();
                }
            }
            if (bestEdge != null) {
                System.out.println("Best: " + bestEdge);
                if (!bestEdge.equals(currentBestEdge)){
                    currentBestEdge = bestEdge;
                    sendPath((byte) 1, getSinkAddress(), buffer.get(currentBestEdge));
                    log(Level.INFO, "Best Edge: " + bestEdge + ", Sending new path");
                }
            }
        }
    }

    @Override
    public void manageRoutingRequest(final RequestPacket req,
                                     final NetworkPacket data) {

        log(Level.INFO, data.toString());
        NetworkGraph network = getNetworkGraph();
        HashMap<NodeAddress, LinkedList<NodeAddress>> results = getResults();

        String dst = data.getNet() + "." + data.getDst();
        String src = data.getNet() + "." + req.getSrc();

        if (!src.equals(dst)) {

            Node srcNode = network.getNode(src);
            Node dstNode = network.getNode(dst);
            LinkedList<NodeAddress> p = null;

            if (srcNode != null && dstNode != null) {
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
                        p.add(node.getAttribute("nodeAddress"));
                    }
                    log(Level.INFO, "Path: " + p);
                    results.put(data.getDst(), p);
                }
                if (p.size() > 1) {
                    sendPath((byte) data.getNet(), p.getFirst(), p);
                    data.setSrc(req.getSrc());
                    data.setNxh(getSinkAddress());
                    sendNetworkPacket(data);
                }
            }
        }
    }

    @Override
    public void setupNetwork() {
        // Nothing to do here
    }
}
