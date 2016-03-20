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
package com.github.sdnwiselab.sdnwise.mote.battery;

/**
 * This class simulates the behavior of a Battery of a simulated Wireless Sensor
 * Node. The values are calculated considering the datasheet of a real sensor
 * node.
 *
 * @author Sebastiano Milardo
 */
public class Battery implements Dischargeable {

    /**
     * Battery statistics.
     */
    private static final double MAX_LEVEL = 5000,
        // 9000000 mC = 2 AAA batteries = 15 Days
        // 5000 mC = 12 min
        KEEP_ALIVE = 6.8, // mC spent every 1 s
        RADIO_TX = 0.0027, // mC to send 1byte
        RADIO_RX = 0.00094; // mC to receive 1byte

    /**
     * Battery level.
     */
    private double level;

    /**
     * Initialize a new Battery object. The battery level is set to MAX_LEVEL.
     */
    public Battery() {
        this.level = Battery.MAX_LEVEL;
    }

    @Override
    public final double getLevel() {
        return this.level;
    }

    @Override
    public final void setLevel(final double batteryLevel) {
        if (batteryLevel >= 0) {
            this.level = batteryLevel;
        } else {
            this.level = 0;
        }
    }

    @Override
    public Battery transmitRadio(final int nBytes) {
        double newVal = this.level - Battery.RADIO_TX * nBytes;
        this.setLevel(newVal);
        return this;
    }

    @Override
    public Battery receiveRadio(final int nBytes) {
        double newVal = this.level - Battery.RADIO_RX * nBytes;
        this.setLevel(newVal);
        return this;
    }

    @Override
    public Battery keepAlive(final int n) {
        double newVal = this.level - Battery.KEEP_ALIVE * n;
        this.setLevel(newVal);
        return this;
    }

    @Override
    public final int getByteLevel() {
        if (Battery.MAX_LEVEL != 0) {
            return (int) ((this.level / Battery.MAX_LEVEL) * 255);
        } else {
            return 0;
        }
    }
}
