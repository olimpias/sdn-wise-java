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

import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.*;

/**
 *
 * @author Sebastiano Milardo
 */
public class ConfigFunctionPacket extends ConfigPacket {

    private static final byte SDN_WISE_CNF_FUNCTION_HEADER_LEN = 5;
    private static final byte SDN_WISE_CNF_FUNCTION_PAYLOAD_LEN
            = NetworkPacket.MAX_PACKET_LENGTH
            - (SDN_WISE_DFLT_HDR_LEN + SDN_WISE_CNF_FUNCTION_HEADER_LEN);

    public ConfigFunctionPacket(NetworkPacket data) {
        super(data);
    }

    public ConfigFunctionPacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
    }

    public final ConfigPacket setAddFunctionAtPositionValue(int index, int part,
            int total, byte[] payload) {
        this.setCurrentPart(part)
                .setTotalParts(total)
                .setFunctionPayload(payload)
                .setWrite()
                .setConfigId(ADD_FUNCTION)
                .setValue(index);
        return this;
    }

    public final ConfigPacket setRemoveFunctionAtPositionValue(int index) {
        return setWrite().setConfigId(REMOVE_FUNCTION).setValue(index);
    }

    public final ConfigFunctionPacket setCurrentPart(int part) {
        this.setPayloadAt((byte) part, 3);
        return this;
    }

    public final int getCurrentPart() {
        return this.getPayloadAt(3);
    }

    public final ConfigFunctionPacket setTotalParts(int total) {
        this.setPayloadAt((byte) total, 4);
        return this;
    }

    public final int getTotalParts() {
        return this.getPayloadAt(4);
    }

    public final ConfigFunctionPacket setFunctionPayload(byte[] payload) {
        super.setPayload(payload, 0, 5, payload.length);
        return this;
    }

    public final byte[] getFunctionPayload() {
        return super.getPayloadFromTo(5, super.getPayloadSize());
    }

    public static List<ConfigFunctionPacket> createPackets(
            byte netId,
            NodeAddress src,
            NodeAddress dest,
            NodeAddress nextHop,
            byte id,
            byte[] buf) {
        LinkedList<ConfigFunctionPacket> ll = new LinkedList<>();

        int packetNumber = buf.length / SDN_WISE_CNF_FUNCTION_PAYLOAD_LEN;
        int remaining = buf.length % SDN_WISE_CNF_FUNCTION_PAYLOAD_LEN;
        int totalPackets = packetNumber + (remaining > 0 ? 1 : 0);
        int pointer = 0;
        int i = 0;

        if (packetNumber < 256) {
            if (packetNumber > 0) {
                for (i = 0; i < packetNumber; i++) {
                    byte[] payload = new byte[SDN_WISE_CNF_FUNCTION_PAYLOAD_LEN];
                    System.arraycopy(buf, pointer, payload, 0, SDN_WISE_CNF_FUNCTION_PAYLOAD_LEN);
                    pointer += SDN_WISE_CNF_FUNCTION_PAYLOAD_LEN;
                    ConfigFunctionPacket np = new ConfigFunctionPacket(netId, src, dest);
                    np.setNxh(nextHop);
                    np.setAddFunctionAtPositionValue(id, i + 1, totalPackets, payload);
                    ll.add(np);
                }
            }

            if (remaining > 0) {
                byte[] payload = new byte[remaining];
                System.arraycopy(buf, pointer, payload, 0, remaining);
                ConfigFunctionPacket np = new ConfigFunctionPacket(netId, src, dest);
                np.setNxh(nextHop);
                np.setAddFunctionAtPositionValue(id, i + 1, totalPackets, payload);
                ll.add(np);
            }
        }
        return ll;
    }
}
