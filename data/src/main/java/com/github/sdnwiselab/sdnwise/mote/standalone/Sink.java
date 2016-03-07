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

import com.github.sdnwiselab.sdnwise.mote.battery.SinkBattery;
import com.github.sdnwiselab.sdnwise.mote.core.SinkCore;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.*;
import java.net.*;
import java.util.logging.*;

/**
 * @author Sebastiano Milardo
 */
public class Sink extends AbstractMote {

    private final String addrController;
    private final int portController;
    private Socket tcpSocket;
    private DataOutputStream inviaOBJ;
    private DataInputStream riceviOBJ;

    public Sink(byte net, NodeAddress myAddress,
            String ipAddress,
            int port,
            String addrController,
            int portController,
            String neighboursPath,
            String logLevel,
            String dpid,
            String mac,
            long sPort) {

        super(port, neighboursPath, logLevel);
        this.addrController = addrController;
        this.portController = portController;
        battery = new SinkBattery();

        try {
            core = new SinkCore(net,
                    myAddress,
                    battery,
                    dpid,
                    mac,
                    sPort,
                    InetAddress.getByName(addrController),
                    portController);
            core.start();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Sink.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(Sink.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class TcpSender implements Runnable {

        @Override
        public void run() {
            try {
                inviaOBJ = new DataOutputStream(tcpSocket.getOutputStream());
                while (true) {
                    NetworkPacket np = ((SinkCore) core).getControllerPacketTobeSend();
                    inviaOBJ.write(np.toByteArray());
                }
            } catch (IOException | InterruptedException ex) {
                Logger.getLogger(Sink.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    void startThreads() {
        super.startThreads();
        try {
            tcpSocket = new Socket(addrController, portController);
            new Thread(new TcpListener()).start();
            new Thread(new TcpSender()).start();
        } catch (IOException ex) {
            Logger.getLogger(Sink.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
}
