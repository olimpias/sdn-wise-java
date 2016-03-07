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

import com.github.sdnwiselab.sdnwise.flowtable.Window;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.OPEN_PATH;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.*;

/**
 * This class models an Open Path packet.
 *
 * @author Sebastiano Milardo
 */
public class OpenPathPacket extends NetworkPacket {

    private static final int WINDOWS_SIZE_INDEX = 0;

    /**
     * This constructor initialize an open path packet starting from a byte
     * array.
     *
     * @param data the byte array representing the open path packet.
     */
    public OpenPathPacket(byte[] data) {
        super(data);
    }

    /**
     * This constructor initialize an open path packet. The type of the packet
     * is set to SDN_WISE_OPEN_PATH.
     *
     * @param net
     * @param src
     * @param dst
     * @param path
     */
    public OpenPathPacket(int net, NodeAddress src, NodeAddress dst, List<NodeAddress> path) {
        super(net, src, dst);
        this.setTyp(OPEN_PATH);
        this.setPayloadAt((byte) 0, WINDOWS_SIZE_INDEX);
        this.setPath(path);
    }

    /**
     * This constructor initialize an open path packet starting from a int
     * array.
     *
     * @param data the int array representing the open path packet, all int are
     * casted to byte.
     */
    public OpenPathPacket(int[] data) {
        super(data);
    }

    /**
     * This constructor initialize an open path packet starting from a
     * NetworkPacket.
     *
     * @param data the NetworkPacket representing the open path packet.
     */
    public OpenPathPacket(NetworkPacket data) {
        super(data.toByteArray());
    }

    /**
     * Setter for the path in the Open Path packet. A path is a list of
     * NodeAddress objects. Each node receiving this method will learn two
     * rules. One to reach the first node in the path and one to reach the last
     * one in the path.
     *
     * @param path a list containing all the node in a path.
     * @return the packet itself
     */
    public final OpenPathPacket setPath(List<NodeAddress> path) {
        int i = (getPayloadAt(WINDOWS_SIZE_INDEX) * Window.SIZE) + 1;
        for (NodeAddress addr : path) {
            setPayloadAt(addr.getHigh(), i);
            i++;
            setPayloadAt(addr.getLow(), i);
            i++;
        }
        return this;
    }

    /**
     * Getter for the path in the Open Path packet. A path is a list of
     * NodeAddress objects. Each node receiving this method will learn two
     * rules. One to reach the first node in the path and one to reach the last
     * one in the path.
     *
     * @return the list of NodeAddress in the path.
     */
    public final List<NodeAddress> getPath() {
        LinkedList<NodeAddress> list = new LinkedList<>();
        byte[] payload = this.getPayload();
        int p = (getPayloadAt(WINDOWS_SIZE_INDEX) * Window.SIZE) + 1;
        for (int i = p; i < payload.length - 1; i += 2) {
            list.add(new NodeAddress(payload[i], payload[i + 1]));
        }
        return list;
    }

    public final OpenPathPacket setWindows(List<Window> conditions) {
        List<NodeAddress> tmp = getPath();

        this.setPayloadAt((byte) conditions.size(), WINDOWS_SIZE_INDEX);
        int i = WINDOWS_SIZE_INDEX + 1;

        for (Window w : conditions) {
            byte[] win = w.toByteArray();
            this.setPayload(win, 0, i, win.length);
            i = i + win.length;
        }
        this.setPath(tmp);
        return this;
    }

    public final List<Window> getWindows() {
        LinkedList<Window> w = new LinkedList<>();

        int nWindows = this.getPayloadAt(WINDOWS_SIZE_INDEX);
        int j = 0;
        for (int i = 0; i < nWindows; i++) {
            Window win = new Window(this.getPayloadFromTo(WINDOWS_SIZE_INDEX + 1 + j,
                    WINDOWS_SIZE_INDEX + 1 + Window.SIZE + j));
            w.add(win);
            j = j + Window.SIZE;
        }
        return w;
    }
}
