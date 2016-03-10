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
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.logging.Level;
import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Node;

/**
 * Representation of a Dijkstra routing algorithm based SDN-WISE controller.
 * When a request from the network is sent, this class sends a
 * OpenPath message with the shortest path. No action is taken if the
 * topology of the network changes.
 *
 * @author Sebastiano Milardo
 */
public class ControllerDijkstra extends AbstractController {

    private final Dijkstra dijkstra;
    private String lastSource = "";
    private long lastModification = -1;

    /**
     * Creates a ControllerDijkstra object.
     * 
     * @param id ControllerId object.
     * @param lower Lower Adpater object.
     * @param networkGraph NetworkGraph object.
     */
    public ControllerDijkstra(InetSocketAddress id, AbstractAdapter lower, NetworkGraph networkGraph) {
        super(id, lower, networkGraph);
        this.dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
    }

    @Override
    public final void graphUpdate() {

    }

    @Override
    public void manageRoutingRequest(RequestPacket req, NetworkPacket data) {

        log(Level.INFO, data.toString());

        String destination = data.getNet() + "." + data.getDst();
        String source = data.getNet() + "." + req.getSrc();

        if (!source.equals(destination)) {

            Node sourceNode = networkGraph.getNode(source);
            Node destinationNode = networkGraph.getNode(destination);
            LinkedList<NodeAddress> path = null;

            if (sourceNode != null && destinationNode != null) {

                if (!lastSource.equals(source) || lastModification != networkGraph.getLastModification()) {
                    results.clear();
                    dijkstra.init(networkGraph.getGraph());
                    dijkstra.setSource(networkGraph.getNode(source));
                    dijkstra.compute();
                    lastSource = source;
                    lastModification = networkGraph.getLastModification();
                } else {
                    path = results.get(data.getDst());
                }
                if (path == null) {
                    path = new LinkedList<>();
                    for (Node node : dijkstra.getPathNodes(networkGraph.getNode(destination))) {
                        path.push((NodeAddress) node.getAttribute("nodeAddress"));
                    }
                    log(Level.INFO, "Path: " + path);
                    results.put(data.getDst(), path);
                }
                if (path.size() > 1) {
                    sendPath((byte) data.getNet(), path.getFirst(), path);
                    data.setSrc(req.getSrc());
                    data.setNxh(getSinkAddress());
                    sendNetworkPacket(data);

                } else {
                    // TODO send a rule in order to say "wait I dont have a path"
                    //sendMessage(data.getNet(), data.getDst(),(byte) 4, new byte[10]);
                }
            }
        }
    }

    @Override
    public void setupNetwork() {

    }
}
