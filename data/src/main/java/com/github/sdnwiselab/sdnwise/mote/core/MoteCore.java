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

import com.github.sdnwiselab.sdnwise.mote.battery.Battery;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import static com.github.sdnwiselab.sdnwise.flowtable.FlowTableInterface.*;
import static com.github.sdnwiselab.sdnwise.flowtable.Window.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.nio.charset.Charset;
import java.util.logging.Level;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.DST_INDEX;

/**
 *
 * @author Sebastiano Milardo
 */
public class MoteCore extends AbstractCore {

    public MoteCore(byte netId, NodeAddress address, Battery battery) {
        super(netId, address, battery);
    }

    @Override
    final void initSdnWiseSpecific() {
        reset();
    }

    @Override
    public void SDN_WISE_Callback(DataPacket packet) {
        if (this.functions.get(1) == null) {
            log(Level.INFO, new String(packet.getPayload(), Charset.forName("UTF-8")));
            packet.setSrc(myAddress)
                    .setDst(getActualSinkAddress())
                    .setTtl((byte) rule_ttl);
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
        if (rssi > rssi_min) {
            if (bp.getDistance() < this.getSinkDistance()
                    && (rssi > getSinkRssi())) {
                setActive(true);
                FlowTableEntry toSink = new FlowTableEntry();
                toSink.addWindow(new Window()
                        .setOperator(SDN_WISE_EQUAL)
                        .setSize(SDN_WISE_SIZE_2)
                        .setLhsLocation(SDN_WISE_PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(SDN_WISE_CONST)
                        .setRhs(bp.getSinkAddress().intValue()));
                toSink.addWindow(Window.fromString("P.TYP == 3"));
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
    public final void controllerTX(NetworkPacket pck) {
        pck.setNxh(getNextHopVsSink());
        radioTX(pck);
    }

    @Override
    public void rxConfig(ConfigPacket packet) {
        NodeAddress dest = packet.getDst();
        if (!dest.equals(myAddress)) {
            runFlowMatch(packet);
        } else if (this.marshalPacket(packet) != 0) {
            packet.setSrc(myAddress);
            packet.setDst(getActualSinkAddress());
            packet.setTtl((byte) rule_ttl);
            runFlowMatch(packet);
        }
    }

    @Override
    final void reset() {
        setSinkDistance(rule_ttl + 1);
        setSinkRssi(0);
        setActive(false);
    }
}
