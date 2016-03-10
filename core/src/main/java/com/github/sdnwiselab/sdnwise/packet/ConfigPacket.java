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
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

/**
 * This class models a Configuration packet. This packet is sent to a node to
 * read/write a parameter or to get/set/remove a rule, a funtion, or a node
 * address alias.
 *
 * @author Sebastiano Milardo
 */
public class ConfigPacket extends NetworkPacket {

    private static final byte CNF_WRITE = 1;

    /**
     * This constructor initialize a config packet starting from a byte array.
     *
     * @param data the byte array representing the config packet
     */
    public ConfigPacket(final byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize a config packet starting from a int array.
     *
     * @param data the int array representing the config packet, all int are
     * casted to byte
     */
    public ConfigPacket(final int[] data) {
        super(data);
    }

    /**
     * This constructor initialize a config packet starting from a
     * NetworkPacket.
     *
     * @param data the NetworkPacket representing the beacon packet
     */
    public ConfigPacket(final NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * This constructor initialize a config packet. The type of the packet is
     * set to {@code CONFIG} and the read/write bit is set to {@code READ}.
     *
     * @param net Network ID of the packet
     * @param src source address of the packet
     * @param dst destination address of the packet
     * @param read the name of the property to read
     */
    public ConfigPacket(final int net, final NodeAddress src,
            final NodeAddress dst,
            final ConfigProperty read) {
        super(net, src, dst);
        setConfigId(read).setTyp(CONFIG);
    }

    /**
     * This constructor initialize a config packet. The type of the packet is
     * set to {@code CONFIG} and the read/write bit is set to {@code WRITE}.
     *
     * @param net the Network ID of the node
     * @param src source address
     * @param dst destination address
     * @param write the name of the property to write
     * @param value the value to be written
     */
    public ConfigPacket(final int net, final NodeAddress src,
            final NodeAddress dst,
            final ConfigProperty write,
            final byte[] value) {
        super(net, src, dst);
        this.setConfigId(write).setWrite().setParams(value, write.size)
                .setTyp(CONFIG);
    }

    /**
     * Returns true if the Config packet is a write packet.
     *
     * @return a boolean indicating if the packet is a write packet
     */
    public final boolean isWrite() {
        int value = (getPayloadAt((byte) 0) & Byte.MAX_VALUE) >> 7;
        return (value == CNF_WRITE);
    }

    /**
     * Returns the Configuration ID of the property to read/write.
     *
     * @return the ConfigProperty set in the packet
     */
    public final ConfigProperty getConfigId() {
        return ConfigProperty.fromByte((byte) (getPayloadAt((byte) 0) & 0x7F));
    }

    /**
     * Sets the value of the property to write. If the ConfigProperty of the
     * packet is a Get or Remove it contains the index of the item. If it is an
     * Add, the item itself.
     *
     * @param bytes the value of the property as a byte[]
     * @param size the size of the property
     * @return the packet itself
     */
    public final ConfigPacket setParams(final byte[] bytes, final int size) {
        if (size != -1) {
            for (int i = 0; i < size; i++) {
                setPayloadAt(bytes[i], i + 1);
            }
        } else {
            for (int i = 0; i < bytes.length; i++) {
                setPayloadAt(bytes[i], i + 1);
            }
        }
        return this;
    }

    /**
     * Gets the value of the read property.
     *
     * @return the configuration property as a byte[]
     */
    public final byte[] getParams() {
        return getPayloadFromTo(1, getPayloadSize());
    }

    private ConfigPacket setWrite() {
        setPayloadAt((byte) ((getPayloadAt(0)) | (CNF_WRITE << 7)), 0);
        return this;
    }

    private ConfigPacket setConfigId(final ConfigProperty id) {
        setPayloadAt(id.value, 0);
        return this;
    }

    // Configuration Properties
    public enum ConfigProperty {
        RESET(0, 0),
        MY_NET(1, 1),
        MY_ADDRESS(2, 2),
        PACKET_TTL(3, 1),
        RSSI_MIN(4, 1),
        BEACON_PERIOD(5, 2),
        REPORT_PERIOD(6, 2),
        RULE_TTL(7, 1),
        ADD_ALIAS(8, 2),
        REM_ALIAS(9, 1),
        GET_ALIAS(10, 1),
        ADD_RULE(11, -1),
        REM_RULE(12, 1),
        GET_RULE(13, 1),
        ADD_FUNCTION(14, -1),
        REM_FUNCTION(15, 1),
        GET_FUNCTION(16, 1);

        private final byte value;
        public final int size;

        private static final ConfigProperty[] VALUES = ConfigProperty.values();

        public static ConfigProperty fromByte(final byte value) {
            return VALUES[value];
        }

        private ConfigProperty(final int v, final int s) {
            this.value = (byte) v;
            this.size = s;
        }
    }
}
