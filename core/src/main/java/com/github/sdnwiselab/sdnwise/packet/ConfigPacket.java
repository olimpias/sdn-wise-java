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

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.CONFIG;
import com.github.sdnwiselab.sdnwise.util.*;

/**
 * This class models a Configuration packet.
 *
 * @author Sebastiano Milardo
 */
public class ConfigPacket extends NetworkPacket {

    // Configuration Properties
    public enum ConfigProperty {
        ADDRESS(0),
        NET_ID(1),
        BEACON_MAX(2),
        REPORT_MAX(3),
        UPDTABLE_MAX(4),
        SLEEP_MAX(5),
        TTL_MAX(6),
        RSSI_MIN(7),
        ADD_ACCEPTED(8),
        REMOVE_ACCEPTED(9),
        LIST_ACCEPTED(10),
        ADD_RULE(11),
        REMOVE_RULE(12),
        REMOVE_RULE_INDEX(13),
        GET_RULE_INDEX(14),
        RESET(15),
        ADD_FUNCTION(16),
        REMOVE_FUNCTION(17),
        SEC_CHANGE(18),
        SEC_IP(19),
        SEC_TOKEN(20);

        private final byte value;
        private final static ConfigProperty[] configPropertyValues = ConfigProperty.values();

        public static ConfigProperty fromByte(byte value) {
            return configPropertyValues[value];
        }

        private ConfigProperty(int value) {
            this.value = (byte) value;
        }
    }

    public final static byte SDN_WISE_CNF_READ = 0,
            SDN_WISE_CNF_WRITE = 1;

    private boolean isWrite;

    public ConfigPacket(byte[] data) {
        super(data);
    }

    public ConfigPacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
        this.setTyp(CONFIG);
    }

    public ConfigPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    public ConfigPacket(int[] data) {
        super(data);
    }

    public boolean isWrite() {
        return this.getPayloadAt(0) >> 7 != SDN_WISE_CNF_READ;
    }

    public final ConfigProperty getConfigId() {
        return ConfigProperty.fromByte((byte) (super.getPayloadAt((byte) 0) & 0x7F));
    }

    protected ConfigPacket setRead() {
        if (this.getPayloadSize() < 1) {
            this.setPayloadSize((byte) 1);
        }
        isWrite = false;
        setPayloadAt((byte) (this.getPayloadAt(0)), 0);
        setPayloadAt((byte) 0, 1);
        setPayloadAt((byte) 0, 2);
        return this;
    }

    protected ConfigPacket setWrite() {
        if (this.getPayloadSize() < 1) {
            this.setPayloadSize((byte) 1);
        }
        isWrite = true;
        setPayloadAt((byte) ((this.getPayloadAt(0)) | (SDN_WISE_CNF_WRITE << 7)), 0);
        return this;
    }

    protected ConfigPacket setConfigId(ConfigProperty property) {
        byte id = property.value;
        if (isWrite) {
            setPayloadAt((byte) (id | (1 << 7)), 0);
        } else {
            setPayloadAt((byte) (id), 0);
        }
        return this;
    }

    public ConfigPacket setValue(byte high, byte low) {
        super.setPayloadAt(high, 1);
        super.setPayloadAt(low, 2);
        return this;
    }

    public int getValue() {
        return Utils.mergeBytes(getPayloadAt(1), getPayloadAt(2));
    }

    public ConfigPacket setValue(int value) {
        super.setPayloadAt((byte) (value >> 8), (byte) 1);
        super.setPayloadAt((byte) (value & 0xFF), (byte) 2);
        return this;
    }

}
