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

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.DATA;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

/**
 * This class models a Data packet.
 *
 * @author Sebastiano Milardo
 */
public class DataPacket extends NetworkPacket {

    /**
     * This constructor initialize a data packet starting from a byte array.
     *
     * @param data the byte array representing the data packet.
     */
    public DataPacket(byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a Data packet starting from a NetworkPacket.
     *
     * @param data the NetworkPacket representing the data packet.
     */
    public DataPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * This constructor initialize a data packet. The type of the packet is set
     * to SDN_WISE_DATA.
     *
     * @param netId
     * @param src
     * @param dst
     */
    public DataPacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
        this.setTyp(DATA);
    }

    /**
     * This constructor initialize a data packet starting from a int array.
     *
     * @param data the int array representing the data packet, all int are
     * casted to byte.
     */
    public DataPacket(int[] data) {
        super(data);
    }

    @Override
    public final byte[] getPayload() {
        return super.getPayload();
    }

    @Override
    public DataPacket setPayload(byte[] payload) {
        super.setPayload(payload);
        return this;
    }
}
