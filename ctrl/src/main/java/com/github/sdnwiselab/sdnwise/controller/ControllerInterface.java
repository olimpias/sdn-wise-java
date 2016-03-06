/*
 * Copyright (C) 2016 Seby
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

import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author Sebastiano Milardo
 */
public interface ControllerInterface {

    /**
     * This method adds a new address in the list of addresses accepted by the
     * node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newAddr the address.
     */
    void addNodeAlias(byte net, NodeAddress dst, NodeAddress newAddr);

    void addNodeFunction(byte net, NodeAddress dst, byte id, String className);

    /**
     * This method installs a rule in the node
     *
     * @param netId network id of the destination node.
     * @param destination network address of the destination node.
     * @param rule the rule to be installed.
     */
    void addNodeRule(byte netId, NodeAddress destination, FlowTableEntry rule);

    InetSocketAddress getId();

    /**
     * This method gets the NetworkGraph of the controller.
     *
     * @return returns a NetworkGraph object.
     */
    NetworkGraph getNetworkGraph();

    /**
     * This method reads the address of a node.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @return returns the NodeAddress of a node, null if it does exists.
     */
    NodeAddress getNodeAddress(byte net, NodeAddress dst);

    /**
     * This method returns the list of addresses accepted by the node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index
     * @return returns the list of accepted Addresses.
     */
    NodeAddress getNodeAlias(byte net, NodeAddress dst, byte index);

    /**
     * This method returns the list of addresses accepted by the node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the list of accepted Addresses.
     */
    List<NodeAddress> getNodeAliases(byte net, NodeAddress dst);

    /**
     * This method reads the beacon period of a node.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @return returns the beacon period, -1 if not found
     */
    int getNodeBeaconPeriod(byte net, NodeAddress dst);

    /**
     * This method reads the Update table period of a node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the updateTablePeriod, -1 if not found.
     */
    int getNodeEntryTtl(byte net, NodeAddress dst);

    /**
     * This method reads the Network ID of a node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the nedId, -1 if not found.
     */
    int getNodeNet(byte net, NodeAddress dst);

    /**
     * This method reads the maximum time to live for each message sent by a
     * node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the maximum time to live, -1 if not found.
     */
    int getNodePacketTtl(byte net, NodeAddress dst);

    /**
     * This method reads the report period of a node.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @return returns the report period, -1 if not found
     */
    int getNodeReportPeriod(byte net, NodeAddress dst);

    /**
     * This method reads the minimum RSSI in order to consider a node as a
     * neighbor.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the minimum RSSI, -1 if not found.
     */
    int getNodeRssiMin(byte net, NodeAddress dst);

    /**
     * This method gets the WISE flow table entry of a node at position n.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index position of the entry in the table.
     * @return returns the list of the entries in the WISE Flow Table.
     * @throws java.util.concurrent.TimeoutException
     */
    FlowTableEntry getNodeRule(byte net, NodeAddress dst, int index) throws TimeoutException;

    /**
     * This method gets the WISE flow table of a node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @return returns the list of the entries in the WISE Flow Table.
     */
    List<FlowTableEntry> getNodeRules(byte net, NodeAddress dst);

    NodeAddress getSinkAddress();

    /**
     * This method removes an address in the list of addresses accepted by the
     * node at position index.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index the address.
     */
    void removeNodeAlias(byte net, NodeAddress dst, byte index);

    void removeNodeFunction(byte net, NodeAddress dst, byte index);

    /**
     * This method removes a rule in the node.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param index index of the erased row.
     */
    void removeNodeRule(byte net, NodeAddress dst, byte index);

    /**
     * This method sets the Network ID of a node. The new value is passed using
     * a byte.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     */
    void resetNode(byte net, NodeAddress dst);

    /**
     * This method sends a SDN_WISE_OPEN_PATH messages to a generic node. This
     * kind of message holds a list of nodes that will create a path inside the
     * network.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param path the list of all the NodeAddresses in the path.
     */
    void sendPath(byte net, NodeAddress dst, List<NodeAddress> path);

    /**
     * This method sets the address of a node. The new address value is passed
     * using two bytes.
     *
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newAddress the new address.
     */
    void setNodeAddress(byte net, NodeAddress dst, NodeAddress newAddress);

    /**
     * This method sets the beacon period of a node. The new value is passed
     * using a short.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param period beacon period in seconds
     */
    void setNodeBeaconPeriod(byte net, NodeAddress dst, short period);

    /**
     * This method sets the update table period of a node. The new value is
     * passed using a short.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param period update table period in seconds
     */
    void setNodeEntryTtl(byte net, NodeAddress dst, short period);

    /**
     * This method sets the Network ID of a node. The new value is passed using
     * a byte.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param newNetId value of the new net ID
     */
    void setNodeNet(byte net, NodeAddress dst, byte newNetId);

    /**
     * This method sets the maximum time to live for each message sent by a
     * node. The new value is passed using a byte.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newTtl time to live in number of hops.
     */
    void setNodePacketTtl(byte net, NodeAddress dst, byte newTtl);

    /**
     * This method sets the report period of a node. The new value is passed
     * using a short.
     *
     * @param net network id of the destination node
     * @param dst network address of the destination node
     * @param period report period in seconds
     */
    void setNodeReportPeriod(byte net, NodeAddress dst, short period);

    /**
     * This method sets the minimum RSSI in order to consider a node as a
     * neighbor.
     *
     * @param net network id of the destination node.
     * @param dst network address of the destination node.
     * @param newRssi new threshold rssi value.
     */
    void setNodeRssiMin(byte net, NodeAddress dst, byte newRssi);

    /**
     * Method called when the network starts. It could be used to configuration
     * rules or network at the beginning of the application.
     */
    void setupNetwork();

    /**
     * Method called to update the graph of Network.
     *
     */
    void graphUpdate();

    /**
     * Method to manage Request about Routing for a NetworkPacket.
     *
     * @param req the last RequestPacket containing the request
     * @param data NetworkPacket will be managed.
     */
    void manageRoutingRequest(RequestPacket req, NetworkPacket data);
}
