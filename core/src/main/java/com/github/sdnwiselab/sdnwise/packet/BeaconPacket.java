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

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.BEACON;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import static com.github.sdnwiselab.sdnwise.util.NodeAddress.BROADCAST_ADDR;

/**
 * This class models a Beacon packet.
 *
 * @author Sebastiano Milardo
 */
public class BeaconPacket extends NetworkPacket {

    private final static byte DIST_INDEX = 0,
            BATT_INDEX = 1;

    /**
     * This constructor initialize a beacon packet starting from a byte array.
     *
     * @param data the byte array representing the beacon packet.
     */
    public BeaconPacket(byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a beacon packet starting from a
     * NetworkPacket.
     *
     * @param data the NetworkPacket representing the beacon packet.
     */
    public BeaconPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * This constructor initialize a beacon packet. The type of the packet is
     * set to SDN_WISE_BEACON and the destination address is BROADCAST_ADDR.
     *
     * @param net
     * @param src
     * @param sink
     * @param distance
     * @param battery
     */
    public BeaconPacket(int net, NodeAddress src, NodeAddress sink,
            int distance, int battery) {
        super(net, src, BROADCAST_ADDR);
        setTyp(BEACON);
        setSinkAddress(sink);
        setDistance((byte) distance);
        setBattery((byte) battery);
    }

    /**
     * This constructor initialize a beacon packet starting from a int array.
     *
     * @param data the int array representing the beacon packet, all int are
     * casted to byte.
     */
    public BeaconPacket(int[] data) {
        super(data);
    }

    /**
     * Getter for the number of hops between the source node and the sink.
     *
     * @return the number of hops between the source node and the sink.
     */
    public final int getDistance() {
        return Byte.toUnsignedInt(getPayloadAt(DIST_INDEX));
    }

    /**
     * Setter for the number of hops between the source node and the sink.
     *
     * @param value the number of hops.
     * @return the packet itself
     */
    public final BeaconPacket setDistance(byte value) {
        this.setPayloadAt(value, DIST_INDEX);
        return this;
    }

    /**
     * Returns an estimation of the residual charge of the batteries of the
     * node. The possible values are: [0-255] 0 = no charge, 255 = full charge.
     *
     * @return an estimation of the residual charge of the batteries of the node
     */
    public final int getBattery() {
        return Byte.toUnsignedInt(getPayloadAt(BATT_INDEX));
    }

    /**
     * Set the battery level in the packet. The possible values are: [0-FF] 0 =
     * no charge,FF = full charge.
     *
     * @param value the value of the battery level.
     * @return the packet itself
     */
    public final BeaconPacket setBattery(byte value) {
        this.setPayloadAt(value, BATT_INDEX);
        return this;
    }

    /**
     * Set the address of the Sink to which this node is connected.
     *
     * @param addr the address of the Sink.
     * @return the packet itself
     */
    public final BeaconPacket setSinkAddress(NodeAddress addr) {
        this.setNxh(addr);
        return this;
    }

    /**
     * Get the address of the Sink to which this node is connected.
     *
     * @return the address of the Sink.
     */
    public final NodeAddress getSinkAddress() {
        return this.getNxh();
    }
}
