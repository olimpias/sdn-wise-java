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
import com.github.sdnwiselab.sdnwise.packet.DataPacket;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.DATA;
import com.github.sdnwiselab.sdnwise.topology.NetworkGraph;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.Observable;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;

/**
 * This class holds a representation of the sensor network and resolves all the
 * requests coming from the network itself. This abstract class has two main
 * private methods. managePacket and graphUpdate(abstract). The first is called
 * when a request is coming from the network while the latter is called when
 * something in the topology of the network changes.
 * <p>
 * There are send and receive(abstract) methods to and from the Adaptation Layer
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractApplication extends ControlPlaneLayer {

    // to avoid garbage collection of the logger
    protected static final Logger LOGGER = Logger.getLogger("APP");

    final AbstractController controller;
    private final ArrayBlockingQueue<NetworkPacket> bQ;

    /**
     * Constructor method for Application Abstract Class.
     *
     * @param ctrl the ctrl to be set
     * @param lower the adapter to be set
     */
    public AbstractApplication(AbstractController ctrl, AbstractAdapter lower) {
        super("APP", lower, null);
        ControlPlaneLogger.setupLogger(layerShortName);
        this.controller = ctrl;
        bQ = new ArrayBlockingQueue<>(1000);
    }

    public abstract void receivePacket(DataPacket data);

    public abstract void graphUpdate();

    private void managePacket(NetworkPacket data) {
        if (data.getTyp() == DATA) {
            receivePacket(new DataPacket(data));
        }
    }

    /**
     * This methods manages updates coming from the lower adapter or the network
     * representation. When a message is received from the lower adapter it is
     * inserted in a ArrayBlockingQueue and then the method managePacket it is
     * called on it. While for updates coming from the network representation
     * the method graphUpdate is invoked.
     *
     * @param o
     * @param arg
     */
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

    @Override
    protected final void setupLayer() {
        new Thread(new Worker(bQ)).start();
    }

    /**
     * Stops the working thread that manages incoming requests.
     */
    public final void stop() {
        isStopped = true;
    }

    /**
     * This method sends a generic message to a node. The message is represented
     * by an array of bytes.
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
     * This method sends a generic message to a node. The message is represented
     * by string.
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
     * Getter method to obtain the Network Graph of this AbstractController.
     *
     * @return the controller network graph.
     */
    public NetworkGraph getNetworkGraph() {
        return controller.getNetworkGraph();
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
