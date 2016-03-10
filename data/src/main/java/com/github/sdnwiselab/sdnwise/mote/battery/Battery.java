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
public class Battery implements BatteryInterface {

    private static final double MAX_LEVEL = 5000;    // 9000000 mC = 2 AAA batteries = 15 Days  
    // 5000 mC = 12 min 
    private static final double KEEP_ALIVE = 6.8;        // mC spent every 1 s
    private static final double RADIO_TX = 0.0027; // mC to send 1byte
    private static final double RADIO_RX = 0.00094; // mC to receive 1byte
    private double level;

    /**
     * Initialize a new Battery object. The battery level is set to MAX_LEVEL.
     */
    public Battery() {
        this.level = Battery.MAX_LEVEL;
    }

    /**
     * Getter for the battery level of the Battery.
     *
     * @return the battery level of the node as a double. Can't be negative.
     */
    public double getLevel() {
        return this.level;
    }

    /**
     * Setter for the battery level of the Battery.
     *
     * @param batteryLevel the battery level. If negative, the battery level is
     * set to 0.
     */
    public void setLevel(double batteryLevel) {
        if (batteryLevel >= 0) {
            this.level = batteryLevel;
        } else {
            this.level = 0;
        }
    }

    /**
     * Simulates the battery consumption for sending nByte bytes.
     *
     * @param nBytes the number of bytes sent over the radio
     * @return the Battery object
     */
    public Battery transmitRadio(int nBytes) {
        double new_val = this.level - Battery.RADIO_TX * nBytes;
        this.setLevel(new_val);
        return this;
    }

    /**
     * Simulates the battery consumption for receiving nByte bytes.
     *
     * @param nBytes the number of bytes received over the radio
     * @return the Battery object
     */
    public Battery receiveRadio(int nBytes) {
        double new_val = this.level - Battery.RADIO_RX * nBytes;
        this.setLevel(new_val);
        return this;
    }

    /**
     * Simulates the battery consumption for staying alive for n seconds.
     *
     * @param n the number of seconds the node is turned on.
     * @return the Battery object
     */
    public Battery keepAlive(int n) {
        double new_val = this.level - Battery.KEEP_ALIVE * n;
        this.setLevel(new_val);
        return this;
    }

    /**
     * Getter for the battery level as a percent of the MAX_LEVEL.
     *
     * @return the Battery level in the range [0-255].
     */
    @Override
    public int getByteLevel() {
        if (Battery.MAX_LEVEL != 0) {
            return (int) ((this.level / Battery.MAX_LEVEL) * 255);
        } else {
            return 0;
        }
    }
}
