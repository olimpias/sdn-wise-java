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
package com.github.sdnwiselab.sdnwise.mote.standalone;

import com.github.sdnwiselab.sdnwise.mote.battery.Dischargeable;
import com.github.sdnwiselab.sdnwise.mote.battery.SinkBattery;
import static com.github.sdnwiselab.sdnwise.mote.core.AbstractCore.MAX_RSSI;
import com.github.sdnwiselab.sdnwise.mote.core.SinkCore;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Models a SDN-WISE Sink Node as a standalone application.
 *
 * @author Sebastiano Milardo
 */
public class Sink extends AbstractMote {

    private final InetSocketAddress ctrl;
    private Socket tcpSocket;
    private DataOutputStream inviaOBJ;
    private DataInputStream riceviOBJ;

    /**
     * Creates and starts a new Sink application.
     *
     * @param net the Network Id of the node
     * @param myAddress the address of the node
     * @param port the listening port of the node
     * @param controller address of the controller
     * @param neighboursPath the path to the file containing neighbours info
     * @param logLevel log level of the logger of the node
     * @param dpid dpid of the sink
     * @param mac mac address of the sink
     * @param sPort physical port of the sink
     */
    public Sink(final byte net, final NodeAddress myAddress, final int port,
            final InetSocketAddress controller,
            final String neighboursPath, final String logLevel,
            final String dpid, final String mac, final long sPort) {

        super(port, neighboursPath, logLevel);
        this.ctrl = controller;
        Dischargeable battery = new SinkBattery();
        core = new SinkCore(net, myAddress, battery, dpid, mac, sPort, ctrl);
        core.start();
    }

    private class TcpListener implements Runnable {

        @Override
        public void run() {
            try {
                riceviOBJ = new DataInputStream(tcpSocket.getInputStream());
                while (true) {
                    NetworkPacket np = new NetworkPacket(riceviOBJ);
                    core.rxRadioPacket(np, MAX_RSSI);
                }
            } catch (IOException ex) {
                Logger.getGlobal().log(Level.SEVERE, null, ex);
            }
        }
    }

    private final class TcpSender implements Runnable {

        @Override
        public void run() {
            try {
                inviaOBJ = new DataOutputStream(tcpSocket.getOutputStream());
                while (true) {
                    NetworkPacket np = ((SinkCore) core)
                            .getControllerPacketTobeSend();
                    inviaOBJ.write(np.toByteArray());
                }
            } catch (IOException | InterruptedException ex) {
                Logger.getGlobal().log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    protected final void startThreads() {
        super.startThreads();
        try {
            tcpSocket = new Socket(ctrl.getAddress(), ctrl.getPort());
            new Thread(new TcpListener()).start();
            new Thread(new TcpSender()).start();
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }

    }
}
