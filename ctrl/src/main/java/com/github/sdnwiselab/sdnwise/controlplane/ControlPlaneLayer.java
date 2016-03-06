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
import java.util.*;
import java.util.logging.*;

/**
 * @author Sebastiano Milardo
 */
public abstract class ControlPlaneLayer implements Observer, Runnable {

    protected final String layerShortName;
    protected final AbstractAdapter lower;
    protected final AbstractAdapter upper;
    protected final Scanner scanner;
    protected boolean isStopped;
    protected final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    public ControlPlaneLayer(String layerShortName, AbstractAdapter lower, AbstractAdapter upper) {
        this.layerShortName = layerShortName;
        this.lower = lower;
        this.upper = upper;
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

    protected abstract void setupLayer();

    private boolean setupAdapter(AbstractAdapter a) {
        if (a == null) {
            return true;
        }
        if (a.open()) {
            a.addObserver(this);
            return true;
        }
        return false;
    }

    private void closeAdapter(AbstractAdapter a) {
        if (a != null) {
            a.close();
        }
    }

    /**
     * Logs messages depending on the verbosity level.
     *
     * @param level a standard logging level
     * @param msg the string message to be logged
     */
    protected void log(Level level, String msg) {
        Logger.getLogger(layerShortName).log(level, msg);
    }
}
