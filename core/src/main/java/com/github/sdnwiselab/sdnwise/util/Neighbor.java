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
package com.github.sdnwiselab.sdnwise.util;

/**
 * This Class represents the Neighbor of a NodeAddress, specifying its rssi and
 * level battery value.
 *
 * @author Sebastiano Milardo
 */
public final class Neighbor {

    private final NodeAddress addr;
    private final int rssi;
    private final int batt;

    /**
     * Constructor method for this class by following attributes.
     *
     * @param a NodeAddress Object.
     * @param r r of the NodeAddress.
     * @param b battery value of the NodeAddress.
     */
    public Neighbor(final NodeAddress a, final int r, final int b) {
        this.addr = a;
        this.rssi = r;
        this.batt = b;
    }

    /**
     * Simple Constructor method for this class.
     *
     */
    public Neighbor() {
        this.addr = new NodeAddress(255, 255);
        this.rssi = 255;
        this.batt = 255;
    }

    /**
     * Getter method to obtain NodeAddress object.
     *
     * @return a NodeAddress.
     */
    public NodeAddress getAddr() {
        return addr;
    }

    /**
     * Getter method to obtain rssi of a NodeAddress object.
     *
     * @return int value rssi of the NodeAddress.
     */
    public int getRssi() {
        return rssi;
    }

    /**
     * Getter method to obtain battery value of a NodeAddress object.
     *
     * @return int value battery of the NodeAddress.
     */
    public int getBatt() {
        return batt;
    }
}
