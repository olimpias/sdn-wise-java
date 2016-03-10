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

import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;

/**
 * Representation of a TCP Adapter. Configuration data are passed using a
 * {@code Map<String,String>} which contains all the options needed in the
 * constructor of the class.
 *
 * @author Sebastiano Milardo
 */
public class AdapterTcp extends AbstractAdapter {

    private final int PORT;
    private final String IP;
    private final boolean IS_SERVER;
    private TcpElement tcpElement;
    private Thread th;

    /**
     * Creates an AdapterTCP object. The conf map is used to pass the
     * configuration settings for the serial port as strings. Specifically
     * needed parameters are:
     * <ol>
     * <li>IN_PORT</li>
     * </ol>
     *
     * @param conf contains the serial port configuration data.
     */
    public AdapterTcp(Map<String, String> conf) {
        this.IS_SERVER = Boolean.parseBoolean(conf.get("IS_SERVER"));
        this.IP = conf.get("IP");
        this.PORT = Integer.parseInt(conf.get("PORT"));
    }

    @Override
    public final boolean open() {
        if (IS_SERVER) {
            tcpElement = new TcpServer(PORT);
        } else {
            tcpElement = new TcpClient(IP, PORT);
        }

        tcpElement.addObserver(this);
        th = new Thread(tcpElement);
        th.start();
        return true;
    }

    @Override
    public final boolean close() {
        tcpElement.isStopped = true;
        return true;
    }

    @Override
    public final void send(byte[] data) {
        tcpElement.send(data);
    }

    private abstract class TcpElement extends Observable implements Runnable, Observer {

        boolean isStopped;
        final int port;

        TcpElement(int port) {
            this.port = port;
        }

        public abstract void send(byte[] data);

        synchronized boolean isStopped() {
            return this.isStopped;
        }

        @Override
        public final void update(Observable o, Object arg) {
            setChanged();
            notifyObservers(arg);
        }
    }

    private class TcpServer extends TcpElement {

        private ServerSocket serverSocket = null;
        private final LinkedList<Socket> clientSockets = new LinkedList<>();
        private final LinkedList<Socket> removableSockets = new LinkedList<>();

        TcpServer(int port) {
            super(port);
        }

        @Override
        public void run() {
            openServerSocket();
            Socket clientSocket;
            while (!isStopped()) {
                try {
                    clientSocket = this.serverSocket.accept();
                } catch (IOException e) {
                    if (isStopped()) {
                        return;
                    }
                    throw new RuntimeException(
                            "Error accepting client connection", e);
                }
                clientSockets.add(clientSocket);
                WorkerRunnable wr = new WorkerRunnable(clientSocket);
                wr.addObserver(this);
                new Thread(wr).start();
            }
        }

        public synchronized void stop() {
            this.isStopped = true;
            try {
                this.serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing server", e);
            }
        }

        private void openServerSocket() {
            try {
                this.serverSocket = new ServerSocket(this.port);
            } catch (IOException e) {
                throw new RuntimeException("Cannot open port", e);
            }
        }

        @Override
        public void send(byte[] data) {
            clientSockets.stream().forEach((sck) -> {
                try {
                    OutputStream out = sck.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(out);
                    dos.write(data);
                } catch (IOException ex) {
                    log(Level.SEVERE, ex.toString());
                    removableSockets.add(sck);
                }
            });

            if (!removableSockets.isEmpty()) {
                clientSockets.removeAll(removableSockets);
                removableSockets.clear();
            }
        }

        private class WorkerRunnable extends Observable implements Runnable {

            private final Socket clientSocket;

            WorkerRunnable(Socket clientSocket) {
                this.clientSocket = clientSocket;
            }

            @Override
            public void run() {
                try {
                    InputStream in = clientSocket.getInputStream();
                    DataInputStream dis = new DataInputStream(in);
                    while (true) {
                        int net = Byte.toUnsignedInt(dis.readByte());
                        int len = Byte.toUnsignedInt(dis.readByte());
                        byte[] data = new byte[len];
                        data[NET_INDEX] = (byte) net;
                        data[LEN_INDEX] = (byte) len;
                        if (len > 0) {
                            dis.readFully(data, LEN_INDEX + 1, len - 2);
                        }
                        setChanged();
                        notifyObservers(data);
                    }
                } catch (IOException ex) {
                    log(Level.SEVERE, ex.toString());
                }
            }
        }
    }

    private class TcpClient extends TcpElement {

        Socket socket;

        TcpClient(String ip, int port) {
            super(port);
            try {
                socket = new Socket(ip, port);
            } catch (IOException ex) {
                log(Level.SEVERE, ex.toString());
            }

        }

        @Override
        public void send(byte[] data) {
            try {
                OutputStream out = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(out);
                dos.write(data);
            } catch (IOException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                DataInputStream dis = new DataInputStream(in);
                while (true) {
                    int net = Byte.toUnsignedInt(dis.readByte());
                    int len = Byte.toUnsignedInt(dis.readByte());
                    if (len > 0) {
                        byte[] data = new byte[len];
                        data[NET_INDEX] = (byte) net;
                        data[LEN_INDEX] = (byte) len;
                        dis.readFully(data, LEN_INDEX + 1, len - 2);
                        setChanged();
                        notifyObservers(data);
                    }
                }
            } catch (IOException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }
    }
}
