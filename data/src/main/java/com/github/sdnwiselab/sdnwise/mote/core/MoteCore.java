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

import com.github.sdnwiselab.sdnwise.flowtable.*;
import static com.github.sdnwiselab.sdnwise.flowtable.FlowTableInterface.*;
import static com.github.sdnwiselab.sdnwise.flowtable.Window.*;
import com.github.sdnwiselab.sdnwise.mote.battery.Dischargeable;
import com.github.sdnwiselab.sdnwise.packet.*;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.DST_INDEX;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.nio.charset.Charset;
import java.util.logging.Level;

/**
 * @author Sebastiano Milardo
 */
public class MoteCore extends AbstractCore {

    public MoteCore(byte net, NodeAddress address, Dischargeable battery) {
        super(net, address, battery);
    }

    @Override
    public final void controllerTX(NetworkPacket pck) {
        pck.setNxh(getNextHopVsSink());
        radioTX(pck);
    }

    @Override
    public void dataCallback(final DataPacket packet) {
        if (this.functions.get(1) == null) {
            log(Level.INFO, new String(packet.getData(),
                    Charset.forName("UTF-8")));
            packet.setSrc(getMyAddress())
                    .setDst(getActualSinkAddress())
                    .setTtl((byte) ruleTtl);
            runFlowMatch(packet);
        } else {
            this.functions.get(1).function(sensors,
                    flowTable,
                    neighborTable,
                    statusRegister,
                    acceptedId,
                    ftQueue,
                    txQueue,
                    new byte[0],
                    packet);
        }
    }

    @Override
    public void rxBeacon(BeaconPacket bp, int rssi) {
        if (rssi > rssiMin) {
            if (bp.getDistance() < this.getSinkDistance()
                    && (rssi > getSinkRssi())) {
                setActive(true);
                FlowTableEntry toSink = new FlowTableEntry();
                toSink.addWindow(new Window()
                        .setOperator(EQUAL)
                        .setSize(W_SIZE_2)
                        .setLhsLocation(PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(CONST)
                        .setRhs(bp.getSinkAddress().intValue()));
                toSink.addWindow(fromString("P.TYP == 3"));
                toSink.addAction(new ForwardUnicastAction(bp.getSrc()));
                flowTable.set(0, toSink);

                setSinkDistance(bp.getDistance() + 1);
                setSinkRssi(rssi);
            } else if ((bp.getDistance() + 1) == this.getSinkDistance()
                    && getNextHopVsSink().equals(bp.getSrc())) {
                flowTable.get(0).getStats().restoreTtl();
                flowTable.get(0).getWindows().get(0)
                        .setRhs(bp.getSinkAddress().intValue());
            }
            super.rxBeacon(bp, rssi);
        }
    }

    @Override
    public void rxConfig(ConfigPacket packet) {
        NodeAddress dest = packet.getDst();
        if (!dest.equals(getMyAddress())) {
            runFlowMatch(packet);
        } else if (this.marshalPacket(packet) != 0) {
            packet.setSrc(getMyAddress());
            packet.setDst(getActualSinkAddress());
            packet.setTtl((byte) ruleTtl);
            runFlowMatch(packet);
        }
    }

    @Override
    protected final void initSdnWiseSpecific() {
        reset();
    }

    @Override
    protected final void reset() {
        setSinkDistance(ruleTtl + 1);
        setSinkRssi(0);
        setActive(false);
    }
}
