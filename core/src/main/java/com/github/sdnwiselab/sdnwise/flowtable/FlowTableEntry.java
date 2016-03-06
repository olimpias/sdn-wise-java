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

import java.nio.ByteBuffer;
import java.util.*;

/**
 * FlowTableEntry represents the structure of the Entry of a FlowTable. It is
 * made of Window[], AbstractAction and Statistics. This Class implements
 * FlowTableInterface.
 *
 * @author Sebastiano Milardo
 */
public class FlowTableEntry implements FlowTableInterface {

    public static FlowTableEntry fromString(String val) {
        val = val.toUpperCase();
        FlowTableEntry res = new FlowTableEntry();

        String[] strWindows
                = (val.substring(val.indexOf("(") + 1, val.indexOf(")"))).split("&&");

        for (String w : strWindows) {
            res.addWindow(Window.fromString(w.trim()));
        }

        String[] strActions
                = (val.substring(val.indexOf("{") + 1, val.indexOf("}"))).trim().split(";");

        for (String a : strActions) {
            res.addAction(ActionBuilder.build(a.trim()));
        }
        return res;
    }

    private final List<Window> windows = new LinkedList<>();
    private final List<AbstractAction> actions = new LinkedList<>();
    private Stats stats = new Stats();

    /**
     * Simple constructor for the FlowTableEntry object.
     *
     * It creates new Window instances setting all the values to 0.
     */
    public FlowTableEntry() {
    }

    /**
     * Constructor for the FlowTableEntry object. It initializes new Window[],
     * AbstractAction and Stats instances.
     *
     * @param entry From byte array to FlowTableEntry
     */
    public FlowTableEntry(byte[] entry) {
        int i = 0;

        int nWindows = entry[i];

        for (i = 1; i <= nWindows; i += Window.SIZE) {
            windows.add(new Window(Arrays.copyOfRange(entry, i, i + Window.SIZE)));
        }

        while (i < entry.length - (Stats.SIZE)) {
            int len = entry[i++];
            actions.add(ActionBuilder.build(Arrays.copyOfRange(entry, i, i + len)));
            i += len;
        }

        stats = new Stats(
                Arrays.copyOfRange(entry, entry.length - (Stats.SIZE + 1), entry.length)
        );

    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("if (");

        windows.stream().map((Window w) -> {
            StringBuilder part = new StringBuilder();
            part.append(w.toString());
            return part;
        }).filter((part) -> (!part.toString().isEmpty())).forEach((part) -> {
            if (out.toString().equals("if (")) {
                out.append(part);
            } else {
                out.append(" && ").append(part);
            }
        });
        if (!out.toString().isEmpty()) {
            out.append(") { ");
            actions.stream().forEach((a) -> {
                out.append(a.toString()).append("; ");
            });
            out.append("} (")
                    .append(getStats().toString())
                    .append(")");
        }
        return out.toString().toUpperCase();
    }

    /**
     * Getter method to obtain the window array of the FlowTable entry.
     *
     * @return the window[] of the FlowTable
     */
    public List<Window> getWindows() {
        return windows;
    }

    /**
     * Setter method to set window array of the FlowTable entry.
     *
     * @param windows the window[] to set
     */
    public void setWindows(List<Window> windows) {
        this.windows.clear();
        this.windows.addAll(windows);
    }

    public boolean addWindow(Window window) {
        return windows.add(window);
    }

    /**
     * Getter method to obtain the AbstractAction part of the FlowTable entry.
     *
     * @return the action of the FlowTable
     */
    public List<AbstractAction> getActions() {
        return actions;
    }

    /**
     * Setter method to set the AbstractAction part of the FlowTable entry.
     *
     * @param actions the action to set
     */
    public void setAction(List<AbstractAction> actions) {
        this.actions.clear();
        this.actions.addAll(actions);
    }

    public boolean addAction(AbstractAction action) {
        return actions.add(action);
    }

    /**
     * Getter method to obtain the Statistics of the FlowTable entry.
     *
     * @return the statistics of the FlowTable entry.
     */
    public Stats getStats() {
        return stats;
    }

    /**
     * Setter method to set statistics of the FlowTable entry.
     *
     * @param stats the statistics will be set.
     */
    public void setStats(Stats stats) {
        this.stats = stats;
    }

    @Override
    public byte[] toByteArray() {
        int size = (1 + windows.size() * Window.SIZE) + Stats.SIZE;
        for (AbstractAction a : actions) {
            size = size + a.action.length + 1;
        }

        ByteBuffer target = ByteBuffer.allocate(size);
        target.put((byte) (windows.size() * Window.SIZE));

        windows.stream().forEach((fw) -> {
            target.put(fw.toByteArray());
        });

        actions.stream().map((a) -> {
            target.put((byte) a.action.length);
            return a;
        }).forEach((a) -> {
            target.put(a.toByteArray());
        });

        target.put(stats.toByteArray());

        return target.array();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.windows);
        hash = 59 * hash + Objects.hashCode(this.actions);
        hash = 59 * hash + Objects.hashCode(this.stats);
        return hash;
    }

    public boolean equalWindows(FlowTableEntry other){
        return Objects.deepEquals(this.windows, other.windows);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FlowTableEntry other = (FlowTableEntry) obj;
        if (!Objects.deepEquals(this.windows, other.windows)) {
            return false;
        }
        return Objects.deepEquals(this.actions, other.actions);
    }

}
