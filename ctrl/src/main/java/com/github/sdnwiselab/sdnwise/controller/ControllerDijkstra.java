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
import java.util.LinkedList;
import java.util.logging.Level;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Node;

/**
 * Representation of a Dijkstra routing algorithm based SDN-WISE controller.
 * When a request from the network is sent, this class sends a OpenPath message
 with the shortest p. No action is taken if the topology of the network
 changes.
 *
 * @author Sebastiano Milardo
 */
public final class ControllerDijkstra extends AbstractController {
    /**
     * Used to calculate a path according to Dijkstra's algorithm.
     */
    private final Dijkstra dijkstra;
    /**
     * Last Node address for which there are calculated paths.
     */
    private String lastSource = "";
    /**
     * Last modification time of the networkGraph object.
     */
    private long lastModification = -1;

    /**
     * Creates a ControllerDijkstra object.
     *
     * @param id ControllerId object.
     * @param lower Lower Adpater object.
     * @param networkGraph NetworkGraph object.
     */
    public ControllerDijkstra(final InetSocketAddress id,
            final AbstractAdapter lower,
            final NetworkGraph networkGraph) {
        super(id, lower, networkGraph);
        this.dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
    }

    @Override
    public void graphUpdate() {

    }

    @Override
    public void manageRoutingRequest(final RequestPacket req,
            final NetworkPacket data) {

        log(Level.INFO, data.toString());

        String dst = data.getNet() + "." + data.getDst();
        String src = data.getNet() + "." + req.getSrc();

        if (!src.equals(dst)) {

            Node srcNode = networkGraph.getNode(src);
            Node dstNode = networkGraph.getNode(dst);
            LinkedList<NodeAddress> p = null;

            if (srcNode != null && dstNode != null) {

                if (!lastSource.equals(src) || lastModification
                        != networkGraph.getLastModification()) {
                    results.clear();
                    dijkstra.init(networkGraph.getGraph());
                    dijkstra.setSource(networkGraph.getNode(src));
                    dijkstra.compute();
                    lastSource = src;
                    lastModification = networkGraph.getLastModification();
                } else {
                    p = results.get(data.getDst());
                }
                if (p == null) {
                    p = new LinkedList<>();
                    for (Node node : dijkstra.getPathNodes(networkGraph
                            .getNode(dst))) {
                        p.push((NodeAddress) node.getAttribute("nodeAddress"));
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

    }
}
