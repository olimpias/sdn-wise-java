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
import com.github.sdnwiselab.sdnwise.mote.core.SinkCore;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Sebastiano Milardo
 */
public class Sink extends AbstractMote {

    private final String addrController;
    private final int portController;
    private Socket tcpSocket;
    private DataOutputStream inviaOBJ;
    private DataInputStream riceviOBJ;

    public Sink(final byte net, final NodeAddress myAddress, final int port,
            final String addrCtrl, final int portCtrl,
            final String neighboursPath, final String logLevel,
            final String dpid, final String mac, final long sPort) {

        super(port, neighboursPath, logLevel);
        this.addrController = addrCtrl;
        this.portController = portCtrl;
        Dischargeable battery = new SinkBattery();

        try {
            core = new SinkCore(net, myAddress, battery, dpid, mac, sPort,
                    InetAddress.getByName(addrController), portController);
            core.start();
        } catch (UnknownHostException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }
    }

    private class TcpListener implements Runnable {

        @Override
        public void run() {
            try {
                riceviOBJ = new DataInputStream(tcpSocket.getInputStream());
                while (true) {
                    NetworkPacket np = new NetworkPacket(riceviOBJ);
                    core.rxRadioPacket(np, 255);
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
                    NetworkPacket np = ((SinkCore) core).getControllerPacketTobeSend();
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
            tcpSocket = new Socket(addrController, portController);
            new Thread(new TcpListener()).start();
            new Thread(new TcpSender()).start();
        } catch (IOException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }

    }
}
