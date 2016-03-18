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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Level;

/**
 * Representation an UDP Adapter. Configuration data are passed using a
 * {@code Map<String,String>} which contains all the options needed in the
 * constructor of the class.
 *
 * @author Sebastiano Milardo
 */
public class AdapterUdp extends AbstractAdapter {

    private final int IN_PORT;
    private final String OUT_IP;
    private final int OUT_PORT;
    private DatagramSocket sck;
    private UDPServer udpServer;
    final int MAX_PAYLOAD;

    /**
     * Creates an AdapterUDP object. The conf map is used to pass the
     * configuration settings for the serial port as strings. Specifically
     * needed parameters are:
     * <ol>
     * <li>OUT_IP</li>
     * <li>OUT_PORT</li>
     * <li>IN_PORT</li>
     * <li>MAX_PAYLOAD</li>
     * </ol>
     *
     * @param conf contains the serial port configuration data.
     */
    public AdapterUdp(final Map<String, String> conf) {
        this.OUT_IP = conf.get("OUT_IP");
        this.OUT_PORT = Integer.parseInt(conf.get("OUT_PORT"));
        this.IN_PORT = Integer.parseInt(conf.get("IN_PORT"));
        this.MAX_PAYLOAD = Integer.parseInt(conf.get("MAX_PAYLOAD"));
    }

    @Override
    public final boolean close() {
        udpServer.isStopped = true;
        sck.close();
        return true;
    }

    @Override
    public final boolean open() {
        try {
            sck = new DatagramSocket(IN_PORT);
            udpServer = new UDPServer(sck);
            udpServer.addObserver(this);
            new Thread(udpServer).start();
            return true;
        } catch (SocketException ex) {
            log(Level.SEVERE, ex.toString());
            return false;
        }
    }

    @Override
    public final void send(final byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getByName(OUT_IP), OUT_PORT);
            sck.send(packet);
        } catch (IOException ex) {
            log(Level.SEVERE, ex.toString());
        }
    }

    /**
     * Sends a byte array using this adapter. This method also specifies the
     * destination IP address and UDP port.
     *
     * @param data the array to be sent
     * @param ip a string containing the IP address of the destination
     * @param port an integer containing the UDP port of the destination
     */
    public final void send(final byte[] data, final String ip, final int port) {
        try {
            DatagramPacket packet = new DatagramPacket(
                    data, data.length, InetAddress.getByName(ip), port);
            sck.send(packet);
        } catch (IOException ex) {
            log(Level.SEVERE, ex.toString());
        }
    }

    private final class UDPServer extends Observable implements Runnable {

        private boolean isStopped;
        private final DatagramSocket sck;

        UDPServer(final DatagramSocket socket) {
            this.sck = socket;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[MAX_PAYLOAD];
                DatagramPacket p = new DatagramPacket(buffer, buffer.length);
                while (!isStopped) {
                    sck.receive(p);
                    setChanged();
                    notifyObservers(Arrays.copyOf(p.getData(), p.getLength()));
                }
            } catch (IOException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }
    }
}
