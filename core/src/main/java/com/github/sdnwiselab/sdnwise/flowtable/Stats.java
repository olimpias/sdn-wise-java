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
package com.github.sdnwiselab.sdnwise.flowtable;

import java.util.Arrays;

/**
 * Stats is part of the structure of the Entry of a FlowTable. This Class
 * implements FlowTableInterface.
 *
 * @author Sebastiano Milardo
 */
public class Stats implements FlowTableInterface {

    public final static byte SIZE = 2;
    public static final int ENTRY_TTL_PERMANENT = 255;
    public static final int SDN_WISE_RL_TTL_MAX = 254;

    private final static byte TTL_INDEX = 0;
    private final static byte COUNT_INDEX = 1;
    private final byte[] stats = new byte[SIZE];

    /**
     * Simple constructor for the FlowTableStats object.
     *
     * It sets the statistical fields to the default values.
     */
    public Stats() {
        stats[TTL_INDEX] = (byte) SDN_WISE_RL_TTL_MAX;
        stats[COUNT_INDEX] = 0;
    }

    /**
     * Constructor for the FlowTableStats object.
     *
     * @param value byte array to copy into the statistical part.
     */
    public Stats(byte[] value) {
        switch (value.length) {
            case 2:
                stats[TTL_INDEX] = value[TTL_INDEX];
                stats[COUNT_INDEX] = value[COUNT_INDEX];
                break;
            case 1:
                stats[TTL_INDEX] = value[TTL_INDEX];
                stats[COUNT_INDEX] = 0;
                break;
            default:
                stats[TTL_INDEX] = (byte) SDN_WISE_RL_TTL_MAX;
                stats[COUNT_INDEX] = 0;
                break;
        }
    }

    /**
     * Getter Method to obtain the ttl value. When the TTL of an entry is equal
     * to 0 the entry is remove from the FlowTable.
     *
     * @return value of ttl of stats[].
     */
    public int getTtl() {
        return Byte.toUnsignedInt(stats[TTL_INDEX]);
    }

    /**
     * Getter Method to obtain count value. The count value represent the number
     * of times an entry has been executed in the FlowTable. This value is not
     * sent to a node.
     *
     * @return value of count of stats[].
     */
    public int getCounter() {
        return Byte.toUnsignedInt(stats[COUNT_INDEX]);
    }

    /**
     * Setter Method to set count value. The count value represent the number of
     * times an entry has been executed in the FlowTable. This value is not sent
     * to a node.
     *
     * @param count to be set
     * @return this Stats
     */
    public Stats setCounter(int count) {
        stats[COUNT_INDEX] = (byte) count;
        return this;
    }

    public Stats increaseCounter() {
        stats[COUNT_INDEX]++;
        return this;
    }

    @Override
    public final String toString() {
        return "TTL: " + (getTtl() == ENTRY_TTL_PERMANENT
                ? "PERM" : getTtl()) + ", U: " + getCounter();
    }

    @Override
    public byte[] toByteArray() {
        return Arrays.copyOf(stats, SIZE);
    }

    public Stats setPermanent() {
        this.setTtl(ENTRY_TTL_PERMANENT);
        return this;
    }

    public Stats restoreTtl() {
        this.setTtl(SDN_WISE_RL_TTL_MAX);
        return this;
    }

    public Stats decrementTtl(int value) {

        this.setTtl(getTtl() - value);
        return this;
    }

    private Stats setTtl(int ttl) {
        this.stats[TTL_INDEX] = (byte) ttl;
        return this;
    }
}
