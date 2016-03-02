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
package com.github.sdnwiselab.sdnwise.mote.core;

import com.github.sdnwiselab.sdnwise.packet.RegProxyPacket;
import com.github.sdnwiselab.sdnwise.mote.battery.Battery;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;

/**
 *
 * @author Sebastiano Milardo
 */
public class SinkCore extends AbstractCore {

    String switchDPid;
    String switchMac;
    long switchPort;

    InetAddress addrController;
    int port;

    // Contains the NetworkPacket that will be sent over the serial port to the controller
    final ArrayBlockingQueue<NetworkPacket> txControllerQueue = new ArrayBlockingQueue<>(100);

    public SinkCore(
            byte netId,
            NodeAddress address,
            Battery battery,
            String switchDPid,
            String switchMac,
            long switchPort,
            InetAddress addrController,
            int port) {
        super(netId, address, battery);
        this.switchDPid = switchDPid;
        this.switchMac = switchMac;
        this.switchPort = switchPort;
        this.addrController = addrController;
        this.port = port;
    }

    @Override
    public final void controllerTX(NetworkPacket pck) {
        try {
            txControllerQueue.put(pck);
            log(Level.FINE, "C-TX " + pck);
        } catch (InterruptedException ex) {
            log(Level.SEVERE, ex.getLocalizedMessage());
        }
    }

    public final NetworkPacket getControllerPacketTobeSend() throws InterruptedException {
        return txControllerQueue.take();
    }

    @Override
    public void SDN_WISE_Callback(DataPacket packet) {
        controllerTX(packet);
    }

    @Override
    public void rxConfig(ConfigPacket packet) {
        NodeAddress dest = packet.getDst();
        NodeAddress src = packet.getSrc();

        if (!dest.equals(myAddress)) {
            runFlowMatch(packet);
        } else if (!src.equals(myAddress)) {
            controllerTX(packet);
        } else if (marshalPacket(packet) != 0) {
            controllerTX(packet);
        }
    }

    @Override
    public NodeAddress getActualSinkAddress() {
        return myAddress;
    }

    @Override
    final void initSdnWiseSpecific() {
        setSinkDistance(0);
        setSinkRssi(255);
        setActive(true);

        InetSocketAddress iAddr;
        iAddr = new InetSocketAddress(addrController, port);
        RegProxyPacket rpp = new RegProxyPacket(1, myAddress, switchDPid,
                switchMac, switchPort, iAddr);
        controllerTX(rpp);
    }

    @Override
    final void reset() {
    }
}
