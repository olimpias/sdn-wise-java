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

import com.github.sdnwiselab.sdnwise.mote.battery.Dischargeable;
import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.*;
import static com.github.sdnwiselab.sdnwise.flowtable.SetAction.*;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import static com.github.sdnwiselab.sdnwise.flowtable.Window.*;
import com.github.sdnwiselab.sdnwise.function.FunctionInterface;
import static com.github.sdnwiselab.sdnwise.mote.core.Constants.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import static com.github.sdnwiselab.sdnwise.flowtable.Stats.ENTRY_TTL_PERMANENT;
import com.github.sdnwiselab.sdnwise.util.Neighbor;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import static com.github.sdnwiselab.sdnwise.util.Utils.mergeBytes;
import static com.github.sdnwiselab.sdnwise.util.Utils.splitInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractCore {

    /**
     * Battery.
     */
    private final Dischargeable battery;
    /**
     * Timers.
     */
    private int cntBeacon, cntReport, cntUpdTable;
    /**
     * Requests count.
     */
    private byte requestId;
    /**
     * Routing.
     */
    private int sinkDistance, sinkRssi;
    /**
     * Accepted IDs.
     */
    protected List<NodeAddress> acceptedId = new LinkedList<>();
    /**
     * Timers max values.
     */
    private int cntBeaconMax, cntReportMax, cntUpdtableMax;
    /**
     * WISE Flow Table.
     */
    protected final List<FlowTableEntry> flowTable = new LinkedList<>();
    /**
     * Contains the NetworkPacket that will be processed by the WISE Flow Table.
     */
    protected final ArrayBlockingQueue<NetworkPacket> ftQueue = new ArrayBlockingQueue<>(100);
    /**
     * Function Buffer.
     */
    private final HashMap<Integer, LinkedList<byte[]>> functionBuffer = new HashMap<>();
    /**
     * Function Array.
     */
    protected final HashMap<Integer, FunctionInterface> functions = new HashMap<>();
    /**
     * A Mote becomes active after it receives a beacon. A Sink is always
     * active.
     */
    private boolean isActive;
    /**
     * Contains the Log messages.
     */
    private final ArrayBlockingQueue<Pair<Level, String>> logQueue = new ArrayBlockingQueue<>(100);
    /**
     * The address of the node.
     */
    private NodeAddress myAddress;
    /**
     * Configuration parameters.
     */
    private int myNet;
    protected final Set<Neighbor> neighborTable;
    protected int rssiMin;
    protected int ruleTtl;
    /**
     * Contains the NetworkPacket and the RSSI coming from the radio/controller.
     */
    private final ArrayBlockingQueue<Pair<NetworkPacket, Integer>> rxQueue
            = new ArrayBlockingQueue<>(100);
    /**
     * Sensors.
     */
    protected HashMap<String, Object> sensors = new HashMap<>();
    /**
     * Status Register.
     */
    protected ArrayList<Integer> statusRegister = new ArrayList<>();
    /**
     * Contains the NetworkPacket that will be sent over the radio.
     */
    protected final ArrayBlockingQueue<NetworkPacket> txQueue
            = new ArrayBlockingQueue<>(100);

    AbstractCore(byte net, NodeAddress address, Dischargeable battery) {
        this.neighborTable = Collections.synchronizedSet(new HashSet<>(100));
        this.myAddress = address;
        this.myNet = net;
        this.battery = battery;
    }

    /**
     * Gets the battery.
     *
     * @return the battery of the node
     */
    public final Dischargeable getBattery() {
        return battery;
    }

    public final int getFlowTableSize() {
        return flowTable.size();
    }

    public Pair<Level, String> getLogToBePrinted() throws InterruptedException {
        return logQueue.take();
    }

    public final NodeAddress getMyAddress() {
        return myAddress;
    }

    public int getNet() {
        return myNet;
    }

    public NetworkPacket getNetworkPacketToBeSend() throws InterruptedException {
        return txQueue.take();
    }

    public void rxRadioPacket(NetworkPacket np, int rssi) {
        if (np.getDst().isBroadcast()
                || np.getNxh().equals(myAddress)
                || acceptedId.contains(np.getNxh())
                || !np.isSdnWise()) {
            try {
                rxQueue.put(new Pair<>(np, rssi));
            } catch (InterruptedException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }
    }

    public final void start() {
        initFlowTable();
        initStatusRegister();
        initSdnWise();
        new Thread(new rxPacketManager()).start();
        new Thread(new ftPacketManager()).start();
    }

    public final void timer() {
        if (isActive) {
            cntBeacon++;
            cntReport++;
            cntUpdTable++;

            if ((cntBeacon) >= cntBeaconMax) {
                cntBeacon = 0;
                radioTX(prepareBeacon());
            }

            if ((cntReport) >= cntReportMax) {
                cntReport = 0;
                controllerTX(prepareReport());
            }

            if ((cntUpdTable) >= cntUpdtableMax) {
                cntUpdTable = 0;
                updateTable();
            }
        }
    }

    private boolean compare(final int operatore, final int item1,
            final int item2) {
        if (item1 == -1 || item2 == -1) {
            return false;
        }
        switch (operatore) {
            case EQUAL:
                return item1 == item2;
            case NOT_EQUAL:
                return item1 != item2;
            case GREATER:
                return item1 > item2;
            case LESS:
                return item1 < item2;
            case GREATER_OR_EQUAL:
                return item1 >= item2;
            case LESS_OR_EQUAL:
                return item1 <= item2;
            default:
                return false;
        }
    }

    private FunctionInterface createServiceInterface(final byte[] classFile) {
        CustomClassLoader cl = new CustomClassLoader();
        FunctionInterface srvI = null;
        Class service = cl.defClass(classFile, classFile.length);
        try {
            srvI = (FunctionInterface) service.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            log(Level.SEVERE, ex.toString());
        }
        return srvI;
    }

    private int doOperation(final int op, final int item1, final int item2) {
        switch (op) {
            case ADD:
                return item1 + item2;
            case SUB:
                return item1 - item2;
            case DIV:
                return item1 / item2;
            case MUL:
                return item1 * item2;
            case MOD:
                return item1 % item2;
            case AND:
                return item1 & item2;
            case OR:
                return item1 | item2;
            case XOR:
                return item1 ^ item2;
            default:
                return 0;
        }
    }

    private int getOperand(final NetworkPacket packet, final int size,
            final int location, final int value) {
        switch (location) {
            case NULL:
                return 0;
            case CONST:
                return value;
            case PACKET:
                int[] intPacket = packet.toIntArray();
                if (size == W_SIZE_1) {
                    if (value >= intPacket.length) {
                        return -1;
                    }
                    return intPacket[value];
                }
                if (size == W_SIZE_2) {
                    if (value + 1 >= intPacket.length) {
                        return -1;
                    }
                    return mergeBytes(intPacket[value], intPacket[value + 1]);
                }
                return -1;
            case STATUS:
                if (size == W_SIZE_1) {
                    if (value >= statusRegister.size()) {
                        return -1;
                    }
                    return statusRegister.get(value);
                }
                if (size == W_SIZE_2) {
                    if (value + 1 >= statusRegister.size()) {
                        return -1;
                    }
                    return mergeBytes(statusRegister.get(value),
                            statusRegister.get(value + 1));
                }
                return -1;
            default:
                return -1;
        }
    }

    private void initFlowTable() {
        FlowTableEntry toSink = new FlowTableEntry();
        toSink.addWindow(new Window()
                .setOperator(EQUAL)
                .setSize(W_SIZE_2)
                .setLhsLocation(PACKET)
                .setLhs(DST_INDEX)
                .setRhsLocation(CONST)
                .setRhs(this.myAddress.intValue()));
        toSink.addWindow(fromString("P.TYP == 3"));
        toSink.addAction(new ForwardUnicastAction(myAddress));
        toSink.getStats().setPermanent();
        flowTable.add(toSink);
    }

    private void initStatusRegister() {
        for (int i = 0; i < SDN_WISE_STATUS_LEN; i++) {
            statusRegister.add(0);
        }
    }

    // check if there is a match for the packet
    private boolean matchRule(final FlowTableEntry rule,
            final NetworkPacket packet) {
        if (rule.getWindows().isEmpty()) {
            return false;
        }

        int target = rule.getWindows().size();
        int actual = 0;

        for (Window w : rule.getWindows()) {
            if (matchWindow(w, packet)) {
                actual++;
            }
        }
        return (actual == target);
    }

    // Check if a windows is true or not
    private boolean matchWindow(final Window w, final NetworkPacket packet) {
        int operator = w.getOperator();
        int size = w.getSize();
        int lhs = getOperand(packet, size, w.getLhsLocation(), w.getLhs());
        int rhs = getOperand(packet, size, w.getRhsLocation(), w.getRhs());
        return compare(operator, lhs, rhs);
    }

    private BeaconPacket prepareBeacon() {
        BeaconPacket bp = new BeaconPacket(
                myNet,
                myAddress,
                getActualSinkAddress(),
                sinkDistance,
                battery.getByteLevel());
        return bp;
    }

    private ReportPacket prepareReport() {

        ReportPacket rp = new ReportPacket(
                myNet,
                myAddress,
                getActualSinkAddress(),
                sinkDistance,
                battery.getByteLevel());

        rp.setNeighbors(this.neighborTable.size())
                .setNxh(getNextHopVsSink());

        int j = 0;
        synchronized (neighborTable) {
            for (Neighbor n : neighborTable) {
                rp.setNeighborAddressAt(n.getAddr(), j)
                        .setLinkQualityAt((byte) n.getRssi(), j);
                j++;
            }
            neighborTable.clear();
        }
        return rp;
    }

    // Run the corresponding action
    private void runAction(AbstractAction action, NetworkPacket np) {
        try {
            switch (action.getType()) {

                case FORWARD_U:
                case FORWARD_B:
                    np.setNxh(((AbstractForwardAction) action).getNextHop());
                    radioTX(np);
                    break;
                case SET:
                    SetAction ftam = (SetAction) action;
                    int operator = ftam.getOperator();
                    int lhs = getOperand(np, W_SIZE_1, ftam.getLhsLocation(), ftam.getLhs());
                    int rhs = getOperand(np, W_SIZE_1, ftam.getRhsLocation(), ftam.getRhs());
                    if (lhs == -1 || rhs == -1) {
                        throw new IllegalArgumentException("Operators out of bound");
                    }
                    int res = doOperation(operator, lhs, rhs);
                    if (ftam.getResLocation() == PACKET) {
                        int[] packet = np.toIntArray();
                        if (ftam.getRes() >= packet.length) {
                            throw new IllegalArgumentException("Result out of bound");
                        }
                        packet[ftam.getRes()] = res;
                        np.setArray(packet);
                    } else {
                        statusRegister.set(ftam.getRes(), res);
                        log(Level.INFO, "SET R." + ftam.getRes() + " = " + res + ". Done.");
                    }
                    break;
                case FUNCTION:
                    FunctionAction ftac = (FunctionAction) action;
                    FunctionInterface srvI = functions.get(ftac.getId());
                    if (srvI != null) {
                        log(Level.INFO, "Function called: " + myAddress);
                        srvI.function(sensors,
                                flowTable,
                                neighborTable,
                                statusRegister,
                                acceptedId,
                                ftQueue,
                                txQueue,
                                ftac.getArgs(),
                                np
                        );
                    }
                    break;
                case ASK:
                    RequestPacket[] rps = RequestPacket.createPackets((byte) myNet, myAddress,
                            getActualSinkAddress(),
                            requestId++, np.toByteArray());

                    for (RequestPacket rp : rps) {
                        controllerTX(rp);
                    }
                    break;
                case MATCH:
                    ftQueue.put(np);
                    break;
                default:
                    break;
            } //switch
        } catch (InterruptedException ex) {
            log(Level.SEVERE, ex.toString());
        }
    }

    private int searchRule(final FlowTableEntry rule) {
        int i = 0;
        for (FlowTableEntry fte : flowTable) {
            if (fte.equalWindows(rule)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void updateTable() {
        int i = 0;
        for (Iterator<FlowTableEntry> it = flowTable.iterator(); it.hasNext();) {
            i++;
            FlowTableEntry fte = it.next();
            int ttl = fte.getStats().getTtl();
            if (ttl != ENTRY_TTL_PERMANENT) {
                if (ttl >= ENTRY_TTL_DECR) {
                    fte.getStats().decrementTtl(ENTRY_TTL_DECR);
                } else {
                    it.remove();
                    log(Level.INFO, "Removing rule at position " + i);
                    if (i == 0) {
                        reset();
                    }
                }
            }
        }
    }

    protected abstract void controllerTX(NetworkPacket pck);

    protected abstract void dataCallback(DataPacket packet);

    protected final void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    protected NodeAddress getActualSinkAddress() {
        return new NodeAddress(flowTable.get(0).getWindows().get(0).getRhs());
    }

    protected final NodeAddress getNextHopVsSink() {
        return ((AbstractForwardAction) (flowTable.get(0).getActions().get(0)))
                .getNextHop();
    }

    protected final int getSinkDistance() {
        return sinkDistance;
    }

    protected final void setSinkDistance(int sinkDistance) {
        this.sinkDistance = sinkDistance;
    }

    protected final int getSinkRssi() {
        return sinkRssi;
    }

    protected final void setSinkRssi(int rssi) {
        this.sinkRssi = rssi;
    }

    protected void initSdnWise() {
        cntBeaconMax = SDN_WISE_DFLT_CNT_BEACON_MAX;
        cntReportMax = SDN_WISE_DFLT_CNT_REPORT_MAX;
        cntUpdtableMax = SDN_WISE_DFLT_CNT_UPDTABLE_MAX;
        rssiMin = SDN_WISE_DFLT_RSSI_MIN;
        ruleTtl = DFLT_TTL_MAX;
        initSdnWiseSpecific();
    }

    protected abstract void initSdnWiseSpecific();

    protected void insertRule(FlowTableEntry rule) {
        int i = searchRule(rule);
        if (i != -1) {
            flowTable.set(i, rule);
            log(Level.INFO, "Replacing rule " + rule
                    + " at position " + i);
        } else {
            flowTable.add(rule);
            log(Level.INFO, "Inserting rule " + rule
                    + " at position " + (flowTable.size() - 1));
        }

    }

    protected boolean isAcceptedIdAddress(NodeAddress addrP) {
        return (addrP.equals(myAddress)
                || addrP.isBroadcast()
                || acceptedId.contains(addrP));
    }

    protected boolean isAcceptedIdPacket(NetworkPacket packet) {
        return isAcceptedIdAddress(packet.getDst());
    }

    protected final void log(final Level level, final String logMessage) {
        try {
            logQueue.put(new Pair<>(level, logMessage));
        } catch (InterruptedException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Executes the commands contained in a write ConfigPacket.
     *
     * @param packet a Config packet
     */
    private void execWriteConfigPacket(final ConfigPacket packet) {
        byte[] value = packet.getParams();
        int idValue = Byte.toUnsignedInt(value[0]);
        switch (packet.getConfigId()) {
            case MY_ADDRESS:
                myAddress = new NodeAddress(value);
                break;
            case MY_NET:
                myNet = idValue;
                break;
            case BEACON_PERIOD:
                cntBeaconMax = mergeBytes(value[0], value[1]);
                break;
            case REPORT_PERIOD:
                cntReportMax = mergeBytes(value[0], value[1]);
                break;
            case RULE_TTL:
                cntUpdtableMax = idValue;
                break;
            case PACKET_TTL:
                ruleTtl = idValue;
                break;
            case RSSI_MIN:
                rssiMin = idValue;
                break;
            case ADD_ALIAS:
                acceptedId.add(new NodeAddress(value));
                break;
            case REM_ALIAS:
                acceptedId.remove(idValue);
                break;
            case REM_RULE:
                if (idValue != 0) {
                    flowTable.remove(idValue);
                }
                break;
            case ADD_RULE:
                // TODO we need to decide what to do with response  packets
                break;
            case RESET:
                reset();
                break;
            case ADD_FUNCTION:
                if (functionBuffer.get(idValue) == null) {
                    functionBuffer.put(idValue, new LinkedList<>());
                }
                byte[] function = Arrays.copyOfRange(value, 3, value.length);
                int totalParts = Byte.toUnsignedInt(value[2]);
                functionBuffer.get(idValue).add(function);
                if (functionBuffer.get(idValue).size() == totalParts) {
                    int total = 0;
                    total = functionBuffer.get(idValue).stream().map((n)
                            -> (n.length)).reduce(total, Integer::sum);
                    int pointer = 0;
                    byte[] func = new byte[total];
                    for (byte[] n : functionBuffer.get(idValue)) {
                        System.arraycopy(n, 0, func, pointer, n.length);
                        pointer += n.length;
                    }
                    functions.put(idValue, createServiceInterface(func));
                    log(Level.INFO, "New Function Added at pos.: " + idValue);
                    functionBuffer.remove(idValue);
                }
                break;
            case REM_FUNCTION:
                functions.remove(idValue);
                break;
            default:
                break;
        }
    }

    /**
     * Executes the commands contained in a read ConfigPacket.
     *
     * @param packet a Config packet
     * @return a boolean indicating if the packet has to be send back
     */
    private boolean execReadConfigPacket(final ConfigPacket packet) {
        ConfigProperty id = packet.getConfigId();
        byte[] value = packet.getParams();
        int size = id.getSize();
        switch (id) {
            case MY_ADDRESS:
                packet.setParams(myAddress.getArray(), size);
                break;
            case MY_NET:
                packet.setParams(new byte[]{(byte) myNet}, size);
                break;
            case BEACON_PERIOD:
                packet.setParams(splitInteger(cntBeaconMax), size);
                break;
            case REPORT_PERIOD:
                packet.setParams(splitInteger(cntReportMax), size);
                break;
            case RULE_TTL:
                packet.setParams(new byte[]{(byte) cntUpdtableMax}, size);
                break;
            case PACKET_TTL:
                packet.setParams(new byte[]{(byte) ruleTtl}, size);
                break;
            case RSSI_MIN:
                packet.setParams(new byte[]{(byte) rssiMin}, size);
                break;
            case GET_ALIAS:
                int aIndex = Byte.toUnsignedInt(value[0]);
                if (aIndex < acceptedId.size()) {
                    byte[] tmp = acceptedId.get(aIndex).getArray();
                    packet.setParams(ByteBuffer.allocate(tmp.length + 1)
                            .put((byte) aIndex).put(tmp).array(), -1);
                } else {
                    return false;
                }
                break;
            case GET_RULE:
                int i = Byte.toUnsignedInt(value[0]);
                if (i < flowTable.size()) {
                    FlowTableEntry fte = flowTable.get(i);
                    byte[] tmp = fte.toByteArray();
                    packet.setParams(ByteBuffer.allocate(tmp.length + 1)
                            .put((byte) i).put(tmp).array(), -1);
                } else {
                    return false;
                }
                break;
            case GET_FUNCTION:
                // TODO
                break;
            default:
                break;
        }
        return true;
    }

    protected final boolean execConfigPacket(final ConfigPacket packet) {
        boolean toBeSent = false;
        try {
            if (packet.isWrite()) {
                execWriteConfigPacket(packet);
            } else {
                toBeSent = execReadConfigPacket(packet);
            }
        } catch (Exception ex) {
            log(Level.SEVERE, ex.toString());
        }
        return toBeSent;
    }

    protected final void radioTX(final NetworkPacket np) {
        np.decrementTtl();
        txQueue.add(np);
    }

    protected abstract void reset();

    protected final void runFlowMatch(NetworkPacket packet) {
        int i = 0;
        boolean matched = false;
        for (FlowTableEntry fte : flowTable) {
            i++;
            if (matchRule(fte, packet)) {
                log(Level.FINE, "Matched Rule #" + i + " " + fte.toString());
                matched = true;
                fte.getActions().stream().forEach((a) -> {
                    runAction(a, packet);
                });
                fte.getStats().increaseCounter();
                break;
            }
        }

        if (!matched) {
            // send a rule request
            RequestPacket[] rps = RequestPacket.createPackets((byte) myNet,
                    myAddress, getActualSinkAddress(), requestId++,
                    packet.toByteArray());

            for (RequestPacket rp : rps) {
                controllerTX(rp);
            }
        }
    }

    protected void rxBeacon(BeaconPacket bp, int rssi) {
        Neighbor nb = new Neighbor(bp.getSrc(), rssi, bp.getBattery());
        this.neighborTable.add(nb);
    }

    protected abstract void rxConfig(ConfigPacket packet);

    protected final void rxData(DataPacket packet) {
        if (isAcceptedIdPacket(packet)) {
            dataCallback(packet);
        } else if (isAcceptedIdAddress(packet.getNxh())) {
            runFlowMatch(packet);
        }
    }

    protected void rxHandler(NetworkPacket packet, int rssi) {

        if (!packet.isSdnWise()) {
            runFlowMatch(packet);
        } else if (packet.getLen() > DFLT_HDR_LEN
                && packet.getNet() == myNet
                && packet.getTtl() != 0) {

            switch (packet.getTyp()) {
                case DATA:
                    rxData(new DataPacket(packet));
                    break;

                case BEACON:
                    rxBeacon(new BeaconPacket(packet), rssi);
                    break;

                case REPORT:
                    rxReport(new ReportPacket(packet));
                    break;

                case REQUEST:
                    rxRequest(new RequestPacket(packet));
                    break;

                case RESPONSE:
                    rxResponse(new ResponsePacket(packet));
                    break;

                case OPEN_PATH:
                    rxOpenPath(new OpenPathPacket(packet));
                    break;

                case CONFIG:
                    rxConfig(new ConfigPacket(packet));
                    break;

                default:
                    runFlowMatch(packet);
                    break;
            }

        }
    }

    protected void rxOpenPath(OpenPathPacket opp) {
        if (isAcceptedIdPacket(opp)) {
            List<NodeAddress> path = opp.getPath();
            int i;
            for (i = 0; i < path.size(); i++) {
                NodeAddress actual = path.get(i);
                if (isAcceptedIdAddress(actual)) {
                    break;
                }
            }

            if (i > 0) {
                FlowTableEntry rule = new FlowTableEntry();
                rule.addWindow(new Window()
                        .setOperator(EQUAL)
                        .setSize(W_SIZE_2)
                        .setLhsLocation(PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(CONST)
                        .setRhs(path.get(0).intValue()));

                rule.getWindows().addAll(opp.getWindows());
                rule.addAction(new ForwardUnicastAction(path.get(i - 1)));
                insertRule(rule);
            }

            if (i < (path.size() - 1)) {
                FlowTableEntry rule = new FlowTableEntry();
                rule.addWindow(new Window()
                        .setOperator(EQUAL)
                        .setSize(W_SIZE_2)
                        .setLhsLocation(PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(CONST)
                        .setRhs(path.get(path.size() - 1).intValue()));

                rule.getWindows().addAll(opp.getWindows());
                rule.addAction(new ForwardUnicastAction(path.get(i + 1)));
                insertRule(rule);
                opp.setDst(path.get(i + 1));
                opp.setNxh(path.get(i + 1));
                radioTX(opp);
            }

        } else {
            runFlowMatch(opp);
        }
    }

    protected void rxReport(ReportPacket packet) {
        controllerTX(packet);
    }

    protected void rxRequest(RequestPacket packet) {
        controllerTX(packet);
    }

    protected void rxResponse(ResponsePacket rp) {
        if (isAcceptedIdPacket(rp)) {
            rp.getRule().setStats(new Stats());
            insertRule(rp.getRule());
        } else {
            runFlowMatch(rp);
        }
    }

    private class CustomClassLoader extends ClassLoader {

        public Class defClass(byte[] data, int len) {
            return defineClass(null, data, 0, len);
        }
    }

    private class ftPacketManager implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    NetworkPacket np = ftQueue.take();
                    rxQueue.put(new Pair<>(np, 255));
                }
            } catch (InterruptedException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }
    }

    private class rxPacketManager implements Runnable {

        @Override
        public void run() {
            try {
                while (true) {
                    Pair<NetworkPacket, Integer> p = rxQueue.take();
                    rxHandler(p.getKey(), p.getValue());
                }
            } catch (InterruptedException ex) {
                log(Level.SEVERE, ex.toString());
            }
        }
    }
}
