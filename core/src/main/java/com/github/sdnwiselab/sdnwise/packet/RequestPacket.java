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

    /**
     * Indexes of the fields.
     */
    private static final byte ID_INDEX = 0, PART_INDEX = 1, TOTAL_INDEX = 2;

    private static final byte REQUEST_HEADER_SIZE = 3,
            REQUEST_PAYLOAD_SIZE = NetworkPacket.MAX_PACKET_LENGTH
            - (DFLT_HDR_LEN + REQUEST_HEADER_SIZE);

    public static RequestPacket[] createPackets(
            final int net,
            final NodeAddress src,
            final NodeAddress dest,
            final byte id,
            final byte[] buf) {

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

    public static NetworkPacket mergePackets(final RequestPacket rp0,
            final RequestPacket rp1) {
        if (rp0.getPart() == 0) {
            return new NetworkPacket(
                    concatByteArray(rp0.getData(), rp1.getData()));
        } else {
            return new NetworkPacket(
                    concatByteArray(rp1.getData(), rp0.getData()));
        }
    }

    /**
     * This constructor initialize a Request packet starting from a byte array.
     *
     * @param data the byte array representing the data packet.
     */
    public RequestPacket(final byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a Request packet starting from a
     * NetworkPacket.
     *
     * @param data the NetworkPacket representing the data packet.
     */
    public RequestPacket(final NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * This constructor initialize a data packet starting from a int array.
     *
     * @param data the int array representing the data packet, all ints are
     * casted to byte.
     */
    public RequestPacket(final int[] data) {
        super(data);
    }

    /**
     * Construct a Request packet from its fields. It is used only inside this
     * class.
     *
     * @param net Network ID of the packet
     * @param src source address of the packet
     * @param dst destination address of the packet
     * @param id identificator of the request
     * @param part identificator of the part of the request
     * @param total the total number of parts
     * @param data the data payload of the request
     */
    private RequestPacket(final int net,
            final NodeAddress src,
            final NodeAddress dst,
            final int id,
            final int part,
            final int total,
            final byte[] data) {
        super(net, src, dst);
        setTyp(REQUEST);
        setId(id).setTotal(total).setPart(part).setData(data);
    }
    /**
     * Gets the data payload of the request.
     *
     * @return a byte array representing the data payload of a single packet
     */
    public final byte[] getData() {
        return this.getPayloadFromTo(TOTAL_INDEX + 1, getPayloadSize());
    }

    /**
     * Gets the size of the data payload of the request.
     *
     * @return data payload size in bytes
     */
    public final int getDataSize() {
        return this.getPayloadSize() - (TOTAL_INDEX + 1);
    }

    /**
     * Gets the ID of the request. The request ID is used to identify requests
     * coming from the same node.
     *
     * @return the id of the request
     */
    public final int getId() {
        return this.getPayloadAt(ID_INDEX);
    }
    /**
     * Gets the part number of the Request packet. A NetworkPacket cannot be
     * longer than NetworkPacket.MAX_PACKET_LENGTH, so it can be incapsulated in
     * maximum two packets. Therefore this value is 0 or 1.
     *
     * @return the part number
     */
    public final int getPart() {
        return this.getPayloadAt(PART_INDEX);
    }
    /**
     * Gets the Total expected number of parts. A NetworkPacket cannot be longer
     * than NetworkPacket.MAX_PACKET_LENGTH, so it can be incapsulated in
     * maximum two packets. Therefore this value is 1 or 2.
     *
     * @return the total number of parts
     */
    public final int getTotal() {
        return this.getPayloadAt(TOTAL_INDEX);
    }

    /**
     * Sets the data payload of the request.
     *
     * @param data the data payload
     * @return the packet itself
     */
    private RequestPacket setData(final byte[] data) {
        this.setPayload(data, 0, TOTAL_INDEX + 1, data.length);
        return this;
    }

    /**
     * Sets the ID of the request. The request ID is used to identify requests
     * coming from the same node.
     *
     * @param id the id to be set
     * @return the packet itself
     */
    private RequestPacket setId(final int id) {
        this.setPayloadAt((byte) id, ID_INDEX);
        return this;
    }


    /**
     * Sets the part number of the Request packet. A NetworkPacket cannot be
     * longer than NetworkPacket.MAX_PACKET_LENGTH, so it can be incapsulated in
     * maximum two packets. Therefore this value is 0 or 1.
     *
     * @param part the value of part
     * @return the packet itself
     */
    private RequestPacket setPart(final int part) {
        this.setPayloadAt((byte) part, PART_INDEX);
        return this;
    }


    /**
     * Sets the Total expected number of parts. A NetworkPacket cannot be longer
     * than NetworkPacket.MAX_PACKET_LENGTH, so it can be incapsulated in
     * maximum two packets. Therefore this value is 1 or 2.
     *
     * @return the total number of parts
     * @param total the expected number of parts
     */
    private RequestPacket setTotal(final int total) {
        this.setPayloadAt((byte) total, TOTAL_INDEX);
        return this;
    }

}
