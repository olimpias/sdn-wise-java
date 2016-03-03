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
package com.github.sdnwiselab.sdnwise.packet;

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.REPORT;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * This class models a Report packet.
 *
 * @author Sebastiano Milardo
 */
public class ReportPacket extends BeaconPacket {

    private final static byte MAX_NEIG = 35;
    private final static int NEIGH_INDEX = 2;

    /**
     * This constructor initialize a report packet starting from a byte array.
     *
     * @param data the byte array representing the report packet.
     */
    public ReportPacket(byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a report packet. The type of the packet is
     * set to SDN_WISE_REPORT.
     *
     * @param net the Network ID of the node
     * @param src the NodeAddress address of the source node
     * @param dst the NodeAddress address of the destination node
     * @param distance the distance in hops between the source node and the sink
     * @param battery the battery level of the source node
     */
    public ReportPacket(int net, NodeAddress src, NodeAddress dst, int distance, int battery) {
        super(net, src, dst, distance, battery);
        setDst(dst);
        this.setTyp(REPORT);
    }

    /**
     * This constructor initialize a report packet starting from a int array.
     *
     * @param data the int array representing the report packet, all int are
     * casted to byte.
     */
    public ReportPacket(int[] data) {
        super(data);
    }

    /**
     * This constructor initialize a report packet starting from a
     * NetworkPacket.
     *
     * @param data the NetworkPacket representing the report packet.
     */
    public ReportPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * Getter for the number of neighbors of the source node.
     *
     * @return the number of neighbors.
     */
    public final int getNeigborsSize() {
        return Byte.toUnsignedInt(getPayloadAt(NEIGH_INDEX));
    }

    /**
     * Setter for the number of neighbors of the source node.
     *
     * @param value the number of neighbors.
     * @return the packet itself.
     */
    public final ReportPacket setNeighbors(int value) {
        if (value <= MAX_NEIG) {
            this.setPayloadAt((byte) value, NEIGH_INDEX);
            this.setPayloadSize((byte) (3 + value * 3));
        } else {
            throw new IllegalArgumentException("Too many neighbors");
        }
        return this;
    }

    /**
     * Getter for the NodeAddress of the i-th node in the neighbor list.
     *
     * @param i the i-th node in the neighbors list
     * @return the NodeAddress of the i-th node in the neighbors list
     */
    public final NodeAddress getNeighborAddress(int i) {
        if (i <= MAX_NEIG) {
            return new NodeAddress(
                    this.getPayloadAt(NEIGH_INDEX + 1 + i * 3),
                    this.getPayloadAt(NEIGH_INDEX + 2 + (i * 3)));
        } else {
            throw new IllegalArgumentException("Index exceeds max number of neighbors");
        }
    }

    /**
     * Setter for the NodeAddress of the i-th node in the neighbor list.
     *
     * @param addr the address of the i-th NodeAddress.
     * @param i the position where the NodeAddress will be inserted.
     * @return
     */
    public final ReportPacket setNeighborAddressAt(NodeAddress addr, int i) {
        if (i <= MAX_NEIG) {
            this.setPayloadAt(addr.getHigh(), (byte) (NEIGH_INDEX + 1 + i * 3));
            this.setPayloadAt(addr.getLow(), (byte) (NEIGH_INDEX + 2 + (i * 3)));
            if (this.getNeigborsSize() < i) {
                this.setNeighbors(i);
            }
            return this;
        } else {
            throw new IllegalArgumentException("Index exceeds max number of neighbors");
        }
    }

    /**
     * Getter for the rssi value between the i-th node in the neighbor list and
     * the source node.
     *
     * @param i the i-th node in the neighbors list
     * @return the rssi value
     */
    public final int getLinkQuality(int i) {
        if (i <= MAX_NEIG) {
            return this.getPayloadAt(5 + (i * 3));
        } else {
            throw new IllegalArgumentException("Index exceeds max number of neighbors");
        }
    }

    /**
     * Setter for the rssi value between the i-th node in the neighbor list and
     * the source node.
     *
     * @param i the i-th node in the neighbors list.
     * @param value the weight of the link.
     * @return the packet itself.
     */
    public final ReportPacket setLinkQualityAt(byte value, int i) {
        if (i <= MAX_NEIG) {
            this.setPayloadAt(value, (byte) (5 + i * 3));
            if (this.getNeigborsSize() < i) {
                this.setNeighbors(i);
            }
            return this;
        } else {
            throw new IllegalArgumentException("Index exceeds max number of neighbors");
        }
    }

    /**
     * Gets the list of Neighbors.
     *
     * @return an HashMap filled with the neighbors and their weights.
     */
    public final HashMap<NodeAddress, Byte> getNeighbors() {
        HashMap<NodeAddress, Byte> map = new HashMap<>();
        int nNeig = this.getNeigborsSize();
        for (int i = 0; i < nNeig; i++) {
            map.put(this.getNeighborAddress(i),
                    (byte) this.getLinkQuality(i));
        }
        return map;
    }

    /**
     * Sets the list of Neighbors.
     *
     * @param map the map of neighbors to be set
     * @return
     */
    public ReportPacket setNeighbors(HashMap<NodeAddress, Byte> map) {
        int i = 0;
        for (Map.Entry<NodeAddress, Byte> entry : map.entrySet()) {
            this.setNeighborAddressAt(entry.getKey(), i);
            this.setLinkQualityAt(entry.getValue(), i);
            i++;
        }
        this.setNeighbors((byte) map.size());
        return this;
    }

}
