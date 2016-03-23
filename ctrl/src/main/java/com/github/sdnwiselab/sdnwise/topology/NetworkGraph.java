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
package com.github.sdnwiselab.sdnwise.topology;

import com.github.sdnwiselab.sdnwise.packet.ReportPacket;
import com.github.sdnwiselab.sdnwise.packet.RequestPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.List;
import java.util.HashSet;
import java.util.Observable;
import java.util.Set;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

/**
 * Holder of the {@code org.graphstream.graph.Graph} object which represent the
 * topology of the wireless sensor network. The method updateMap is invoked when
 * a message with topology updates is sent to the controller.
 *
 * @author Sebastiano Milardo
 */
public class NetworkGraph extends Observable {

    /**
     * Timers.
     */
    private long lastCheck, lastModification;
    /**
     * TTL of a node. If a node is not sending a message for timeout seconds it
     * is removed from the network.
     */
    private final int timeout;
    /**
     * Represents the topology of the network
     */
    protected final Graph graph;
    /**
     * If the absolute value of the difference between two successive
     * measurements of the rssi of a link is greater than this value, an event
     * is sent to the controller.
     */
    protected final int rssiResolution;

    /**
     * Creates the NetworkGraph object. It requires a time to live for each node
     * in the network and a value representing the RSSI resolution in order to
     * consider a change of the RSSI value a change in the network.
     *
     * @param ttl the time to live for a node in seconds
     * @param rssiRes the RSSI resolution
     */
    public NetworkGraph(final int ttl, final int rssiRes) {
        graph = new MultiGraph("SDN-WISE Network");
        lastModification = Long.MIN_VALUE;
        rssiResolution = rssiRes;
        timeout = ttl;
        lastCheck = System.currentTimeMillis();
        graph.setAutoCreate(true);
        graph.setStrict(false);
    }

    public <T extends Edge> T addEdge(String id, String from, String to,
            boolean directed) {
        return graph.addEdge(id, from, to, directed);
    }

    public <T extends Node> T addNode(String id) {
        return graph.addNode(id);
    }

    public final boolean checkConsistency(long now) {
        boolean modified = false;
        if (now - lastCheck > (timeout * 1000L)) {
            lastCheck = now;
            for (Node n : graph) {
                if (n.getAttribute("net", Integer.class) < 63
                        && n.getAttribute("lastSeen", Long.class) != null
                        && !isAlive(timeout, (long) n.getNumber("lastSeen"),
                                now)) {
                    removeNode(n);
                    modified = true;
                }

            }
        }
        return modified;
    }

    /**
     * Gets an Edge of the Graph.
     *
     * @param <T> the type of edge in the graph.
     * @param id string id value to get an Edge.
     * @return the edge of the graph
     */
    public final <T extends Edge> T getEdge(final String id) {
        return graph.getEdge(id);
    }

    /**
     * Gets the Graph contained in the NetworkGraph.
     *
     * @return returns a Graph object
     */
    public final Graph getGraph() {
        return graph;
    }

    /**
     * Returns the last time instant when the NetworkGraph was updated.
     *
     * @return a long representing the last time instant when the NetworkGraph
     * was updated
     */
    public final synchronized long getLastModification() {
        return lastModification;
    }

    /**
     * Gets a Node of the Graph.
     *
     * @param <T> the type of node in the graph.
     * @param id string id value to get a Node.
     * @return the node of the graph
     */
    public final <T extends Node> T getNode(final String id) {
        return graph.getNode(id);
    }

    public final boolean isAlive(final long thrs, final long last, final long now) {
        return ((now - last) < thrs * 1000);
    }

    public <T extends Edge> T removeEdge(Edge edge) {
        return graph.removeEdge(edge);
    }

    public <T extends Node> T removeNode(Node node) {
        return graph.removeNode(node);
    }

    public void setupEdge(Edge edge, int newLen) {
        edge.addAttribute("length", newLen);
    }

    public void setupNode(Node node, int batt, long now, int net, NodeAddress addr) {
        node.addAttribute("battery", batt);
        node.addAttribute("lastSeen", now);
        node.addAttribute("net", net);
        node.addAttribute("nodeAddress", addr);
    }

    public void updateEdge(Edge edge, int newLen) {
        edge.addAttribute("length", newLen);
    }

    public final synchronized void updateMap(RequestPacket req, int net, String node1, List<String> neig) {
        String origin = req.getNet() + "." + req.getSrc();

        long now = System.currentTimeMillis();
        boolean modified = checkConsistency(now);

        Node ori = getNode(origin);
        if (ori == null) {
            ori = addNode(origin);
            setupNode(ori, 0, now, req.getNet(), req.getSrc());
        }

        String fullNodeId = node1;
        Node node = getNode(fullNodeId);

        if (node == null) {
            node = addNode(fullNodeId);
            setupNode(node, 255, now, net, null);

            for (String node2 : neig) {
                String other = node2;
                if (getNode(other) == null) {
                    Node tmp = addNode(other);
                    setupNode(tmp, 255, now, net, null);
                }

                Edge edge = addEdge(other + "-" + fullNodeId, other, fullNodeId, true);
                setupEdge(edge, 100);
            }
            modified = true;

        } else {
            updateNode(node, 255, now);

            for (String node2 : neig) {
                String other = node2;
                if (getNode(other) == null) {
                    Node tmp = addNode(other);
                    setupNode(tmp, 255, now, net, null);
                }

                Edge edge = getEdge(other + "-" + fullNodeId);
                if (edge == null) {
                    Edge tmp = addEdge(other + "-" + fullNodeId, other, node.getId(), true);
                    setupEdge(tmp, 100);
                    modified = true;
                }
            }
        }

        Edge firstEdge = addEdge(origin + "-" + node1, origin, node1, true);
        setupEdge(firstEdge, 100);

        if (modified) {
            lastModification++;
            setChanged();
            notifyObservers();
        }
    }

    /**
     * Invoked when a message with topology updates is received by the
     * controller. It updates the network topology according to the message and
     * checks if all the nodes in the network are still alive.
     *
     * @param packet the NetworkPacket received
     */
    public final synchronized void updateMap(final ReportPacket packet) {

        long now = System.currentTimeMillis();
        boolean modified = checkConsistency(now);

        int net = packet.getNet();
        int batt = packet.getBattery();
        String nodeId = packet.getSrc().toString();
        String fullNodeId = net + "." + nodeId;
        NodeAddress addr = packet.getSrc();

        Node node = getNode(fullNodeId);

        if (node == null) {
            node = addNode(fullNodeId);
            setupNode(node, batt, now, net, addr);

            for (int i = 0; i < packet.getNeigborsSize(); i++) {
                NodeAddress otheraddr = packet.getNeighborAddress(i);
                String other = net + "." + otheraddr.toString();
                if (getNode(other) == null) {
                    Node tmp = addNode(other);
                    setupNode(tmp, 0, now, net, otheraddr);
                }

                int newLen = 255 - packet.getLinkQuality(i);
                String edgeId = other + "-" + fullNodeId;
                Edge edge = addEdge(edgeId, other, node.getId(), true);
                setupEdge(edge, newLen);
            }
            modified = true;

        } else {
            updateNode(node, batt, now);
            Set<Edge> oldEdges = new HashSet<>();
            for (Edge e : node.getEnteringEdgeSet()) {
                oldEdges.add(e);
            }

            for (int i = 0; i < packet.getNeigborsSize(); i++) {
                NodeAddress otheraddr = packet.getNeighborAddress(i);
                String other = net + "." + otheraddr.toString();
                if (getNode(other) == null) {
                    Node tmp = addNode(other);
                    setupNode(tmp, 0, now, net, otheraddr);
                }

                int newLen = 255 - packet.getLinkQuality(i);

                String edgeId = other + "-" + fullNodeId;
                Edge edge = getEdge(edgeId);
                if (edge != null) {
                    oldEdges.remove(edge);
                    int oldLen = edge.getAttribute("length");
                    if (Math.abs(oldLen - newLen) > rssiResolution) {
                        updateEdge(edge, newLen);
                        modified = true;
                    }
                } else {
                    Edge tmp = addEdge(edgeId, other, node.getId(), true);
                    setupEdge(tmp, newLen);
                    modified = true;
                }
            }

            if (!oldEdges.isEmpty()) {
                oldEdges.stream().forEach((e) -> {
                    removeEdge(e);
                });
                modified = true;
            }
        }

        if (modified) {
            lastModification++;
            setChanged();
            notifyObservers();
        }
    }

    public void updateNode(Node node, int batt, long now) {
        node.addAttribute("battery", batt);
        node.addAttribute("lastSeen", now);
    }

}
