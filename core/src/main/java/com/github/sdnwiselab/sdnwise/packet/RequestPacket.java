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

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.PacketType.REQUEST;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import static com.github.sdnwiselab.sdnwise.util.Utils.concatByteArray;

/**
 * This class models a Request packet.
 *
 * @author Sebastiano Milardo
 */
public class RequestPacket extends NetworkPacket {

    private static final byte SDN_WISE_REQUEST_HEADER_LEN = 3;
    private static final byte SDN_WISE_REQUEST_PAYLOAD_LEN
            = NetworkPacket.SDN_WISE_MAX_LEN
            - (SDN_WISE_DFLT_HDR_LEN + SDN_WISE_REQUEST_HEADER_LEN);

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

    /**
     * This constructor initialize a data packet. The type of the packet is set
     * to SDN_WISE_DATA.
     *
     * @param netId
     * @param src
     * @param dst
     * @param id
     * @param part
     * @param total
     * @param data
     */
    public RequestPacket(int netId,
            NodeAddress src,
            NodeAddress dst,
            int id,
            int part,
            int total,
            byte[] data) {
        super(netId, src, dst);
        this.setType(REQUEST);
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
            int netId,
            NodeAddress src,
            NodeAddress dest,
            byte id,
            byte[] buf) {

        int i = (buf.length > SDN_WISE_REQUEST_PAYLOAD_LEN) ? 2 : 1;

        int remaining = buf.length % SDN_WISE_REQUEST_PAYLOAD_LEN;
        RequestPacket[] ll = new RequestPacket[i];

        byte[] payload = new byte[i == 1 ? remaining : SDN_WISE_REQUEST_PAYLOAD_LEN];
        System.arraycopy(buf, 0, payload, 0, payload.length);
        RequestPacket np = new RequestPacket(netId, src, dest, id, 0, i, payload);
        ll[0] = np;

        if (i > 1) {
            payload = new byte[remaining];
            System.arraycopy(buf, SDN_WISE_REQUEST_PAYLOAD_LEN, payload, 0, remaining);
            np = new RequestPacket(netId, src, dest, id, 1, i, payload);
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
        this.setPayloadAt((byte) id, 0);
    }

    public int getId() {
        return this.getPayloadAt(0);
    }

    private void setPart(int part) {
        this.setPayloadAt((byte) part, 1);
    }

    public int getPart() {
        return this.getPayloadAt(1);
    }

    private void setTotal(int total) {
        this.setPayloadAt((byte) total, 2);
    }

    public int getTotal() {
        return this.getPayloadAt(2);
    }

    private void setData(byte[] data) {
        this.setPayload(data, 0, 3, data.length);
    }

    public byte[] getData() {
        return this.getPayloadFromTo(3, getPayloadSize());
    }

    public int getDataSize() {
        return this.getPayloadSize() - 3;
    }
}
