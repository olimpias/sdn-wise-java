/*
 * Copyright (C) 2016 Seby
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
package com.github.sdnwiselab.sdnwise.controlplane;

import com.github.sdnwiselab.sdnwise.adapter.AbstractAdapter;
import java.nio.charset.Charset;
import java.util.Observer;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Models a layer of the Control Plane. Each layer has a lower and upper adapter
 * and a scanner to intercept commands coming from the standard input
 *
 * @author Sebastiano Milardo
 */
public abstract class ControlPlaneLayer implements Observer, Runnable {

    /**
     * Identify the layer. This string is reported in each log message.
     */
    protected final String layerShortName;
    /**
     * Adapters.
     */
    protected final AbstractAdapter lower, upper;
    /**
     * Scanner. Reads incoming commands.
     */
    private final Scanner scanner;
    /**
     * Manages the status of the layer.
     */
    protected boolean isStopped;
    /**
     * Charset in use.
     */
    protected static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    /**
     * Creates a ControlPlane layer give a name and two adapters.
     *
     * @param name the name of the layer
     * @param low lower adapter
     * @param up upper adapter
     */
    public ControlPlaneLayer(final String name,
            final AbstractAdapter low,
            final AbstractAdapter up) {
        this.layerShortName = name;
        this.lower = low;
        this.upper = up;
        this.scanner = new Scanner(System.in, "UTF-8");
    }

    /**
     * Starts the layer thread and checks if both adapters have been opened
     * correctly. This method is listening for incoming closing messages from
     * the standard input.
     */
    @Override
    public final void run() {
        if (setupAdapter(lower) && setupAdapter(upper)) {
            setupLayer();
            while (!isStopped) {
                if (scanner.nextLine().equals("q")) {
                    isStopped = true;
                }
            }
            closeAdapter(lower);
            closeAdapter(upper);
        }
    }

    private boolean setupAdapter(final AbstractAdapter a) {
        if (a == null) {
            return true;
        }
        if (a.open()) {
            a.addObserver(this);
            return true;
        }
        return false;
    }

    private void closeAdapter(final AbstractAdapter a) {
        if (a != null) {
            a.close();
        }
    }

    protected abstract void setupLayer();

    /**
     * Logs messages depending on the verbosity level.
     *
     * @param level a standard logging level
     * @param msg the string message to be logged
     */
    protected final void log(final Level level, final String msg) {
        Logger.getLogger(layerShortName).log(level, msg);
    }
}
