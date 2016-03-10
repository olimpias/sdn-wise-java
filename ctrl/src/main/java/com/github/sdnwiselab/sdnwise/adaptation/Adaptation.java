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
package com.github.sdnwiselab.sdnwise.adaptation;

import com.github.sdnwiselab.sdnwise.adapter.AbstractAdapter;
import com.github.sdnwiselab.sdnwise.controlplane.*;
import java.util.*;
import java.util.logging.*;

/**
 * Incorporates the communication adapters for connecting the controller to the
 * sensor networks and vice versa.
 * <p>
 * This class is implemented as an Observer, so it has an update method that is
 * called every time a new message is received by one of the two adapters. This
 * class also implements runnable and it works on a separate thread.
 * <p>
 * The behavior of this class is equal to a transparent proxy that send messages
 * coming from the lower adapter to the upper adapter and from the upper adapter
 * to the lower.
 *
 * @author Sebastiano Milardo
 */
public class Adaptation extends ControlPlaneLayer {

    // to avoid garbage collector
    protected static final Logger LOGGER = Logger.getLogger("ADA");

    /**
     * Creates an adaptation object given two adapters.
     *
     * @param lower the adapter that receives messages from the sensor network
     * @param upper the adapter that receives messages from the controller
     */
    Adaptation(AbstractAdapter lower, AbstractAdapter upper) {
        super("ADA", lower, upper);
        ControlPlaneLogger.setupLogger(layerShortName);
    }

    /**
     * Called by each message coming from the adapters. Messages coming from
     * the lower adapter are sent to the upper one and vice versa.
     *
     * @param o the adapter that has received the message
     * @param arg the message received as a byte array
     */
    @Override
    public final void update(Observable o, Object arg) {
        if (o.equals(lower)) {
            log(Level.INFO, "\u2191" + Arrays.toString((byte[]) arg));
            upper.send((byte[]) arg);
        } else if (o.equals(upper)) {
            log(Level.INFO, "\u2193" + Arrays.toString((byte[]) arg));
            lower.send((byte[]) arg);
        }
    }

    @Override
    public void setupLayer() {
    }
}
