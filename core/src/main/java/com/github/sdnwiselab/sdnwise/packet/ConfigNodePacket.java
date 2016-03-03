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

/**
 *
 * @author Sebastiano Milardo
 */
public class ConfigNodePacket extends ConfigPacket {

    public ConfigNodePacket(NetworkPacket data) {
        super(data);
    }

    public ConfigNodePacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
    }

    public final ConfigPacket setReadNodeAddressValue() {
        return setRead().setConfigId(MY_ADDRESS);
    }

    public final ConfigPacket setReadNetworkIdValue() {
        return setRead().setConfigId(MY_NET);
    }

    public final ConfigPacket setReadDefaultTtlMaxValue() {
        return setRead().setConfigId(TTL_MAX);
    }

    public final ConfigPacket setReadDefaultRssiMinValue() {
        return setRead().setConfigId(RSSI_MIN);
    }

    public final ConfigPacket setNodeAddressValue(NodeAddress newAddr) {
        return setWrite().setConfigId(MY_ADDRESS).setValue(newAddr.getHigh(), newAddr.getLow());
    }

    public final ConfigPacket setNetworkIdValue(byte id) {
        return setWrite().setConfigId(MY_NET).setValue((byte) 0, id);
    }

    public final ConfigPacket setResetValue() {
        return setWrite().setConfigId(RESET);
    }

    public final NodeAddress getNodeAddress() {
        if (getConfigId() == MY_ADDRESS) {
            return new NodeAddress(getValue());
        } else {
            return null;
        }
    }

    public final int getNetworkIdValue() {
        if (getConfigId() == MY_NET) {
            return getPayloadAt(2);
        } else {
            return -1;
        }
    }

    public final ConfigPacket setDefaultTtlMaxValue(byte ttl) {
        this.setWrite()
                .setConfigId(TTL_MAX)
                .setValue((byte) 0, ttl);
        return this;
    }

    public final ConfigPacket setDefaultRssiMinValue(byte rssi) {
        this.setWrite()
                .setConfigId(RSSI_MIN)
                .setValue((byte) 0, rssi);
        return this;
    }

    public final int getDefaultTtlMaxValue() {
        if (getConfigId() == TTL_MAX) {
            return Byte.toUnsignedInt(getPayloadAt(2));
        } else {
            return -1;
        }
    }

    public final int getDefaultRssiMinValue() {
        if (getConfigId() == RSSI_MIN) {
            return Byte.toUnsignedInt(getPayloadAt(2));
        } else {
            return -1;
        }
    }
}
