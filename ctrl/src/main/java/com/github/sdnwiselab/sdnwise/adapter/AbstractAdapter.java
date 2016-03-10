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
package com.github.sdnwiselab.sdnwise.adapter;

import com.github.sdnwiselab.sdnwise.controlplane.ControlPlaneLogger;
import java.util.*;
import java.util.logging.*;

/**
 * Representation of an abstract adapter. It is an observable class for the
 * adaptation, but it is also an observer for changes coming from the specific
 * adapter type.
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractAdapter extends Observable implements Observer {

    protected static final Logger LOGGER = Logger.getLogger("ADP");

    AbstractAdapter() {
        ControlPlaneLogger.setupLogger("ADP");
    }

    /**
     * Opens this adapter.
     *
     * @return a boolean indicating the correct ending of the operation
     */
    public abstract boolean open();

    /**
     * Closes this adapter.
     *
     * @return a boolean indicating the correct ending of the operation
     */
    public abstract boolean close();

    /**
     * Sends a byte array using this adapter.
     *
     * @param data the array to be sent
     */
    public abstract void send(byte[] data);

    @Override
    public final void update(Observable o, Object arg) {
        setChanged();
        notifyObservers(arg);
    }

    /**
     * Logs messages depending on the verbosity level.
     *
     * @param level a standard logging level
     * @param msg the string message to be logged
     */
    protected final void log(Level level, String msg) {
        LOGGER.log(level, msg);
    }
}
