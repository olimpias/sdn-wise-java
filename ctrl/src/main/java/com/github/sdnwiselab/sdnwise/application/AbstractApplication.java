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
package com.github.sdnwiselab.sdnwise.application;

import com.github.sdnwiselab.sdnwise.adapter.AbstractAdapter;
import com.github.sdnwiselab.sdnwise.controller.AbstractController;
import com.github.sdnwiselab.sdnwise.controlplane.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.DATA;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.Observable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;

/**
 * Representation of the sensor network and resolver of all the data packets
 * coming from the network itself. This abstract class has two main private
 * methods. managePacket and graphUpdate(abstract). The first is called when a
 * request is coming from the network while the latter is called when something
 * in the topology of the network changes.
 * <p>
 * There are a send and a receive methods to/from the Adaptation Layer
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractApplication extends ControlPlaneLayer {

    // to avoid garbage collection of the logger
    protected static final Logger LOGGER = Logger.getLogger("APP");

    private final ArrayBlockingQueue<NetworkPacket> bQ;
    final AbstractController controller;

    /**
     * Creates an Application Abstract Class.
     *
     * @param ctrl the controller to be used
     * @param lower the adapter to be used
     */
    public AbstractApplication(AbstractController ctrl, AbstractAdapter lower) {
        super("APP", lower, null);
        ControlPlaneLogger.setupLogger(layerShortName);
        this.controller = ctrl;
        bQ = new ArrayBlockingQueue<>(1000);
    }

    public abstract void receivePacket(DataPacket data);

    public abstract void graphUpdate();

    @Override
    public final void update(Observable o, Object arg) {
        if (o.equals(lower)) {
            try {
                bQ.put(new NetworkPacket((byte[]) arg));
            } catch (InterruptedException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }
    }

    /**
     * Stops the working thread that manages incoming requests.
     */
    public final void stop() {
        isStopped = true;
    }

    /**
     * Sends a generic message to a node. The message is represented by an array
     * of bytes.
     *
     * @param net network id of the dst node
     * @param dst network address of the dst node
     * @param message the content of the message to be sent
     */
    public final void sendMessage(byte net, NodeAddress dst, byte[] message) {
        if (message.length != 0) {
            DataPacket dp = new DataPacket(net, controller.getSinkAddress(), dst, message);
            dp.setNxh(controller.getSinkAddress());
            lower.send(dp.toByteArray());
        }
    }

    /**
     * Sends a generic message to a node. The message is represented by string.
     *
     * @param net network id of the destination node
     * @param destination network address of the destination node
     * @param message the content of the message to be sent
     */
    public final void sendMessage(byte net, NodeAddress destination, String message) {
        if (message != null && !message.isEmpty()) {
            this.sendMessage(net, destination, message.getBytes(UTF8_CHARSET));
        }
    }

    /**
     * Gets the Network Graph of this AbstractController.
     *
     * @return the controller network graph.
     */
    public NetworkGraph getNetworkGraph() {
        return controller.getNetworkGraph();
    }

    private void managePacket(NetworkPacket data) {
        if (data.getTyp() == DATA) {
            receivePacket(new DataPacket(data));
        }
    }

    @Override
    protected final void setupLayer() {
        new Thread(new Worker(bQ)).start();
    }

    private class Worker implements Runnable {

        private final ArrayBlockingQueue<NetworkPacket> bQ;
        boolean isStopped;

        Worker(ArrayBlockingQueue<NetworkPacket> bQ) {
            this.bQ = bQ;
        }

        @Override
        public void run() {
            while (!isStopped) {
                try {
                    managePacket(bQ.take());
                } catch (InterruptedException ex) {
                    log(Level.SEVERE, ex.toString());
                    isStopped = true;
                }
            }
        }
    }
}
