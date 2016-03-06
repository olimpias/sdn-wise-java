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

import com.github.sdnwiselab.sdnwise.util.SimplerFormatter;
import com.github.sdnwiselab.sdnwise.mote.battery.Battery;
import com.github.sdnwiselab.sdnwise.mote.core.*;
import com.github.sdnwiselab.sdnwise.mote.logger.*;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.DATA;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;

/**
 * @author Sebastiano Milardo
 */
public abstract class AbstractMote implements Runnable {

    // Statistics
    private int sentBytes;
    private int receivedBytes;
    private int sentDataBytes;
    private int receivedDataBytes;

    private final byte[] buf;
    private Logger logger;
    private Logger measureLogger;

    int port;

    DatagramSocket socket;
    String neighborFilePath;
    Battery battery;

    Map<NodeAddress, FakeInfo> neighbourList;

    AbstractCore core;

    public AbstractMote(
            int port,
            String neighborFilePath,
            String level) {
        buf = new byte[NetworkPacket.MAX_PACKET_LENGTH];

        this.neighborFilePath = neighborFilePath;
        this.neighbourList = new HashMap<>();
        this.port = port;

    }

    public void logger() {
        measureLogger.log(Level.FINEST, // NODE;BATTERY LVL(mC);BATTERY LVL(%);NO. RULES INSTALLED; B SENT; B RECEIVED;
                "{0};{1};{2};{3};{4};{5};{6};{7};",
                new Object[]{core.getMyAddress(),
                    String.valueOf(battery.getLevel()),
                    String.valueOf(battery.getByteLevel() / 2.55),
                    core.getFlowTableSize(),
                    sentBytes, receivedBytes,
                    sentDataBytes, receivedDataBytes});
    }

    @Override
    public final void run() {
        try {

            measureLogger = initLogger(Level.FINEST, "M_" + core.getMyAddress()
                    + ".log", new MoteFormatter());

            logger = initLogger(Level.FINE, core.getMyAddress()
                    + ".log", new SimplerFormatter(core.getMyAddress().toString()));

            Path path = Paths.get(neighborFilePath);

            if (!Files.exists(path)) {
                path = Paths.get(this.getClass().getResource("/" + neighborFilePath).toURI());
                logger.log(Level.INFO, "External Config file not found, using embedded one {0}", neighborFilePath);
            }

            try (Stream<String> lines = Files.lines(path)) {
                neighbourList = lines.parallel()
                        .map(line -> line.split(","))
                        .map(e -> new Object() {
                            NodeAddress addr = new NodeAddress(e[0]);
                            FakeInfo fk = new FakeInfo(
                                    new InetSocketAddress(e[1], Integer.parseInt(e[2])
                                    ), Integer.parseInt(e[3])
                            );
                        }
                        ).collect(Collectors.toConcurrentMap(e -> e.addr, e -> e.fk));
            }

            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            socket = new DatagramSocket(port);

            new Timer().schedule(new TaskTimer(), 1000, 1000);
            startThreads();

            while (battery.getByteLevel() > 0) {
                socket.receive(packet);
                NetworkPacket np = new NetworkPacket(packet.getData());
                int rssi = 255;
                if (np.isSdnWise()) {
                    logger.log(Level.FINE, "RRX {0}", np);
                    FakeInfo fk = neighbourList.get(np.getSrc());
                    if (fk != null) {
                        rssi = fk.rssi;
                    } else {
                        rssi = 255;
                    }

                    if (DATA == np.getTyp()) {
                        receivedDataBytes += np.getPayloadSize();
                    }
                }
                core.rxRadioPacket(np, rssi);
            }

        } catch (IOException | RuntimeException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        } catch (URISyntaxException ex) {
            Logger.getLogger(AbstractMote.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public final void radioTX(NetworkPacket np) {

        if (np.isSdnWise()) {
            sentBytes += np.getLen();
            if (DATA == np.getTyp()) {
                sentDataBytes += np.getPayloadSize();
            }
        }

        battery.transmitRadio(np.getLen());

        logger.log(Level.FINE, "RTX {0}", np);

        NodeAddress tmpNxHop = np.getNxh();
        NodeAddress tmpDst = np.getDst();

        if (tmpDst.isBroadcast() || tmpNxHop.isBroadcast()) {

            neighbourList.entrySet().stream().map((isa) -> new DatagramPacket(np.toByteArray(),
                    np.getLen(), isa.getValue().inetAddress)).forEach((pck) -> {
                try {
                    socket.send(pck);
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            });
        } else {
            FakeInfo isa = neighbourList.get(tmpNxHop);
            if (isa != null) {
                try {
                    DatagramPacket pck = new DatagramPacket(np.toByteArray(),
                            np.getLen(), isa.inetAddress);
                    socket.send(pck);

                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private Logger initLogger(Level level, String file, java.util.logging.Formatter formatter) {
        Logger LOGGER = java.util.logging.Logger.getLogger(file);
        LOGGER.setLevel(level);
        try {
            FileHandler fh;
            File dir = new File("logs");
            dir.mkdir();
            fh = new FileHandler("logs/" + file);
            fh.setFormatter(formatter);
            LOGGER.addHandler(fh);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return LOGGER;
    }

    void startThreads() {
        new Thread(new SenderRunnable()).start();
        new Thread(new LoggerRunnable()).start();
    }

    private class FakeInfo {

        InetSocketAddress inetAddress;
        int rssi;

        FakeInfo(InetSocketAddress inetAddress, int rssi) {
            this.inetAddress = inetAddress;
            this.rssi = rssi;
        }
    }

    private class TaskTimer extends TimerTask {

        @Override
        public void run() {
            if (battery.getByteLevel() > 0) {
                core.timer();
                battery.keepAlive(1);
            }
            logger();
        }
    }

    private class SenderRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    radioTX(core.getNetworkPacketToBeSend());
                }
            } catch (InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage());
            }
        }
    }

    private class LoggerRunnable implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    Pair<Level, String> tmp = core.getLogToBePrinted();
                    logger.log(tmp.getKey(), tmp.getValue());
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, ex.getLocalizedMessage());
            }
        }
    }
}
