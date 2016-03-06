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

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.REQUEST;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import static com.github.sdnwiselab.sdnwise.util.Utils.concatByteArray;

/**
 * This class models a Request packet.
 *
 * @author Sebastiano Milardo
 */
public class RequestPacket extends NetworkPacket {

    private static final byte REQUEST_HEADER_SIZE = 3;
    private static final byte REQUEST_PAYLOAD_SIZE
            = NetworkPacket.MAX_PACKET_LENGTH
            - (SDN_WISE_DFLT_HDR_LEN + REQUEST_HEADER_SIZE);

    private final static byte ID_INDEX = 0,
            PART_INDEX = 1,
            TOTAL_INDEX = 2;

    /**
     * This constructor initialize a data packet starting from a byte array.
     *
     * @param data the byte array representing the data packet.
     */
    public RequestPacket(byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a Data packet starting from a NetworkPacket.
     *
     * @param data the NetworkPacket representing the data packet.
     */
    public RequestPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    private RequestPacket(int net,
            NodeAddress src,
            NodeAddress dst,
            int id,
            int part,
            int total,
            byte[] data) {
        super(net, src, dst);
        this.setTyp(REQUEST);
        this.setId(id);
        this.setTotal(total);
        this.setPart(part);
        this.setData(data);
    }

    /**
     * This constructor initialize a data packet starting from a int array.
     *
     * @param data the int array representing the data packet, all int are
     * casted to byte.
     */
    public RequestPacket(int[] data) {
        super(data);
    }

    public static RequestPacket[] createPackets(
            int net,
            NodeAddress src,
            NodeAddress dest,
            byte id,
            byte[] buf) {

        int i = (buf.length > REQUEST_PAYLOAD_SIZE) ? 2 : 1;

        int remaining = buf.length % REQUEST_PAYLOAD_SIZE;
        RequestPacket[] ll = new RequestPacket[i];

        byte[] payload = new byte[i == 1 ? remaining : REQUEST_PAYLOAD_SIZE];
        System.arraycopy(buf, 0, payload, 0, payload.length);
        RequestPacket np = new RequestPacket(net, src, dest, id, 0, i, payload);
        ll[0] = np;

        if (i > 1) {
            payload = new byte[remaining];
            System.arraycopy(buf, REQUEST_PAYLOAD_SIZE, payload, 0, remaining);
            np = new RequestPacket(net, src, dest, id, 1, i, payload);
            ll[1] = np;
        }

        return ll;
    }

    public static NetworkPacket mergePackets(RequestPacket rp0, RequestPacket rp1) {
        if (rp0.getPart() == 0) {
            return new NetworkPacket(concatByteArray(rp0.getData(), rp1.getData()));
        } else {
            return new NetworkPacket(concatByteArray(rp1.getData(), rp0.getData()));
        }
    }

    private void setId(int id) {
        this.setPayloadAt((byte) id, ID_INDEX);
    }

    public int getId() {
        return this.getPayloadAt(ID_INDEX);
    }

    private void setPart(int part) {
        this.setPayloadAt((byte) part, PART_INDEX);
    }

    public int getPart() {
        return this.getPayloadAt(PART_INDEX);
    }

    private void setTotal(int total) {
        this.setPayloadAt((byte) total, TOTAL_INDEX);
    }

    public int getTotal() {
        return this.getPayloadAt(TOTAL_INDEX);
    }

    private void setData(byte[] data) {
        this.setPayload(data, 0, TOTAL_INDEX + 1, data.length);
    }

    public byte[] getData() {
        return this.getPayloadFromTo(TOTAL_INDEX + 1, getPayloadSize());
    }

    public int getDataSize() {
        return this.getPayloadSize() - (TOTAL_INDEX + 1);
    }
}
