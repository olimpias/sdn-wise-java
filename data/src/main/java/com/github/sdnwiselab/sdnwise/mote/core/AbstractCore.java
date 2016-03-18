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

import com.github.sdnwiselab.sdnwise.mote.battery.BatteryInterface;
import com.github.sdnwiselab.sdnwise.mote.battery.Battery;
import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.*;
import static com.github.sdnwiselab.sdnwise.flowtable.SetAction.*;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import static com.github.sdnwiselab.sdnwise.flowtable.Window.*;
import com.github.sdnwiselab.sdnwise.function.FunctionInterface;
import static com.github.sdnwiselab.sdnwise.mote.core.Constants.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty;
import static com.github.sdnwiselab.sdnwise.packet.NetworkPacket.*;
import com.github.sdnwiselab.sdnwise.util.*;
import static com.github.sdnwiselab.sdnwise.util.Utils.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;
import static com.github.sdnwiselab.sdnwise.flowtable.Stats.ENTRY_TTL_PERMANENT;

/**
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractCore {

    // Routing
    private int sinkDistance;
    private int sinkRssi;

    // Timers
    private int cntBeacon;
    private int cntReport;
    private int cntUpdTable;

    // Battery
    private final BatteryInterface battery;

    // A Mote becomes active after it receives a beacon. A Sink is always active
    protected boolean isActive;

    // Contains the NetworkPacket and the RSSI coming from the radio/controller
    final ArrayBlockingQueue<Pair<NetworkPacket, Integer>> rxQueue = new ArrayBlockingQueue<>(100);

    // Contains the NetworkPacket that will be processed by the WISE Flow Table
    final ArrayBlockingQueue<NetworkPacket> ftQueue = new ArrayBlockingQueue<>(100);

    // Contains the NetworkPacket that will be sent over the radio
    final ArrayBlockingQueue<NetworkPacket> txQueue = new ArrayBlockingQueue<>(100);

    // Contains the Log messages
    final ArrayBlockingQueue<Pair<Level, String>> logQueue = new ArrayBlockingQueue<>(100);

    // Configuration parameters
    protected int myNet,
            cnt_beacon_max,
            cnt_report_max,
            cnt_updtable_max,
            rule_ttl,
            rssi_min;

    private byte requestId;

    // The address of the node
    protected NodeAddress myAddress;
    protected final Set<Neighbor> neighborTable;

    // WISE Flow Table
    protected List<FlowTableEntry> flowTable = new LinkedList<>();

    // Accepted IDs
    protected List<NodeAddress> acceptedId = new LinkedList<>();

    // Status Register
    protected ArrayList<Integer> statusRegister = new ArrayList<>();

    // Sensors
    protected HashMap<String, Object> sensors = new HashMap<>();

    // Functions
    HashMap<Integer, LinkedList<byte[]>> functionBuffer = new HashMap<>();
    HashMap<Integer, FunctionInterface> functions = new HashMap<>();

    AbstractCore(byte net, NodeAddress address, Battery battery) {
        this.neighborTable = Collections.synchronizedSet(new HashSet<>(100));
        this.myAddress = address;
        this.myNet = net;
        this.battery = battery;
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

    public NetworkPacket getNetworkPacketToBeSend() throws InterruptedException {
        return txQueue.take();
    }

    public Pair<Level, String> getLogToBePrinted() throws InterruptedException {
        return logQueue.take();
    }

    public final int getFlowTableSize() {
        return flowTable.size();
    }

    public final NodeAddress getMyAddress() {
        return myAddress;
    }

    public int getNet() {
        return myNet;
    }

    public void timer() {
        if (isActive) {
            cntBeacon++;
            cntReport++;
            cntUpdTable++;

            if ((cntBeacon) >= cnt_beacon_max) {
                cntBeacon = 0;
                radioTX(prepareBeacon());
            }

            if ((cntReport) >= cnt_report_max) {
                cntReport = 0;
                controllerTX(prepareReport());
            }

            if ((cntUpdTable) >= cnt_updtable_max) {
                cntUpdTable = 0;
                updateTable();
            }
        }
    }

    public void start() {
        initFlowTable();
        initStatusRegister();
        initSdnWise();
        new Thread(new rxPacketManager()).start();
        new Thread(new ftPacketManager()).start();
    }

    private void initFlowTable() {
        FlowTableEntry toSink = new FlowTableEntry();
        toSink.addWindow(new Window()
                .setOperator(EQUAL)
                .setSize(W_SIZE_2)
                .setLhsLocation(SDN_WISE_PACKET)
                .setLhs(DST_INDEX)
                .setRhsLocation(SDN_WISE_CONST)
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

    private FunctionInterface createServiceInterface(byte[] classFile) {
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
                        .setLhsLocation(SDN_WISE_PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(SDN_WISE_CONST)
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
                        .setLhsLocation(SDN_WISE_PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(SDN_WISE_CONST)
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

    private int searchRule(FlowTableEntry rule) {
        int i = 0;
        for (FlowTableEntry fte : flowTable) {
            if (fte.equalWindows(rule)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    protected boolean isAcceptedIdAddress(NodeAddress addrP) {
        return (addrP.equals(myAddress)
                || addrP.isBroadcast()
                || acceptedId.contains(addrP));
    }

    protected boolean isAcceptedIdPacket(NetworkPacket packet) {
        return isAcceptedIdAddress(packet.getDst());
    }

    private int getOperand(final NetworkPacket packet, final int size,
            final int location, int value) {
        switch (location) {
            case SDN_WISE_NULL:
                return 0;
            case SDN_WISE_CONST:
                return value;
            case SDN_WISE_PACKET:
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
            case SDN_WISE_STATUS:
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

    // Check if a windows is true or not
    private int matchWindow(Window window, NetworkPacket packet) {
        int operator = window.getOperator();
        int size = window.getSize();
        int lhs = getOperand(
                packet, size, window.getLhsLocation(), window.getLhs());
        int rhs = getOperand(
                packet, size, window.getRhsLocation(), window.getRhs());
        return compare(operator, lhs, rhs);
    }

    // check if there is a match for the packet
    private int matchRule(FlowTableEntry rule, NetworkPacket packet) {
        if (rule.getWindows().isEmpty()) {
            return 0;
        }

        int target = rule.getWindows().size();
        int actual = 0;

        for (Window w : rule.getWindows()) {
            actual = actual + matchWindow(w, packet);
        }
        return (actual == target ? 1 : 0);
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
                    if (ftam.getResLocation() == SDN_WISE_PACKET) {
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

    private int doOperation(int operatore, int item1, int item2) {
        switch (operatore) {
            case SDN_WISE_ADD:
                return item1 + item2;
            case SDN_WISE_SUB:
                return item1 - item2;
            case SDN_WISE_DIV:
                return item1 / item2;
            case SDN_WISE_MUL:
                return item1 * item2;
            case SDN_WISE_MOD:
                return item1 % item2;
            case SDN_WISE_AND:
                return item1 & item2;
            case SDN_WISE_OR:
                return item1 | item2;
            case SDN_WISE_XOR:
                return item1 ^ item2;
            default:
                return 0;
        }
    }

    private int compare(int operatore, int item1, int item2) {
        if (item1 == -1 || item2 == -1) {
            return 0;
        }
        switch (operatore) {
            case EQUAL:
                return item1 == item2 ? 1 : 0;
            case NOT_EQUAL:
                return item1 != item2 ? 1 : 0;
            case GREATER:
                return item1 > item2 ? 1 : 0;
            case LESS:
                return item1 < item2 ? 1 : 0;
            case GREATER_OR_EQUAL:
                return item1 >= item2 ? 1 : 0;
            case LESS_OR_EQUAL:
                return item1 <= item2 ? 1 : 0;
            default:
                return 0;
        }
    }

    protected void initSdnWise() {
        cnt_beacon_max = SDN_WISE_DFLT_CNT_BEACON_MAX;
        cnt_report_max = SDN_WISE_DFLT_CNT_REPORT_MAX;
        cnt_updtable_max = SDN_WISE_DFLT_CNT_UPDTABLE_MAX;
        rssi_min = SDN_WISE_DFLT_RSSI_MIN;
        rule_ttl = DFLT_TTL_MAX;
        initSdnWiseSpecific();
    }

    protected final void setSinkDistance(int sinkDistance) {
        this.sinkDistance = sinkDistance;
    }

    protected final void setSinkRssi(int rssi) {
        this.sinkRssi = rssi;
    }

    protected final void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    protected final int getSinkDistance() {
        return sinkDistance;
    }

    protected final int getSinkRssi() {
        return sinkRssi;
    }

    protected final NodeAddress getNextHopVsSink() {
        return ((AbstractForwardAction) (flowTable.get(0).getActions().get(0))).getNextHop();
    }

    protected final void rxData(DataPacket packet) {
        if (isAcceptedIdPacket(packet)) {
            dataCallback(packet);
        } else if (isAcceptedIdAddress(packet.getNxh())) {
            runFlowMatch(packet);
        }
    }

    protected void rxBeacon(BeaconPacket bp, int rssi) {
        Neighbor nb = new Neighbor(bp.getSrc(), rssi, bp.getBattery());
        this.neighborTable.add(nb);
    }

    protected final void runFlowMatch(NetworkPacket packet) {
        int i = 0;
        boolean matched = false;
        for (FlowTableEntry fte : flowTable) {
            i++;
            if (matchRule(fte, packet) == 1) {
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
            RequestPacket[] rps = RequestPacket.createPackets((byte) myNet, myAddress, getActualSinkAddress(), requestId++, packet.toByteArray());

            for (RequestPacket rp : rps) {
                controllerTX(rp);
            }
        }
    }

    protected abstract void rxConfig(ConfigPacket packet);

    protected NodeAddress getActualSinkAddress() {
        return new NodeAddress(flowTable.get(0).getWindows().get(0).getRhs());
    }

    protected abstract void dataCallback(DataPacket packet);

    protected abstract void controllerTX(NetworkPacket pck);

    protected int marshalPacket(ConfigPacket packet) {
        int toBeSent = 0;
        try {
            ConfigProperty id = packet.getConfigId();
            byte[] value = packet.getParams();

            if (packet.isWrite()) {
                int idValue = Byte.toUnsignedInt(value[0]);
                switch (id) {
                    case MY_ADDRESS:
                        myAddress = new NodeAddress(value);
                        break;
                    case MY_NET:
                        myNet = idValue;
                        break;
                    case BEACON_PERIOD:
                        cnt_beacon_max = mergeBytes(value[0], value[1]);
                        break;
                    case REPORT_PERIOD:
                        cnt_report_max = mergeBytes(value[0], value[1]);
                        break;
                    case RULE_TTL:
                        cnt_updtable_max = idValue;
                        break;
                    case PACKET_TTL:
                        rule_ttl = idValue;
                        break;
                    case RSSI_MIN:
                        rssi_min = idValue;
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
                        // TODO we need to decide what to do with response packets
                        break;
                    case RESET:
                        reset();
                        break;
                    case ADD_FUNCTION:
                        if (functionBuffer.get(idValue) == null) {
                            functionBuffer.put(idValue, new LinkedList<>());
                        }

                        byte[] functionPayload = Arrays.copyOfRange(value, 3, value.length);
                        int totalParts = Byte.toUnsignedInt(value[2]);
                        functionBuffer.get(idValue).add(functionPayload);
                        if (functionBuffer.get(idValue).size() == totalParts) {
                            int total = 0;
                            total = functionBuffer.get(idValue).stream().map((n)
                                    -> (n.length)).reduce(total, Integer::sum);
                            int pointer = 0;
                            byte[] func = new byte[total];
                            for (byte[] n : functionBuffer.get(idValue)) {
                                for (int j = 0; j < n.length; j++) {
                                    func[pointer] = n[j];
                                    pointer++;
                                }
                            }
                            functions.put(idValue, createServiceInterface(func));
                            log(Level.INFO, "New Function Added at position: " + idValue);
                            functionBuffer.remove(idValue);
                        }
                        break;
                    case REM_FUNCTION:
                        functions.remove(idValue);
                        break;
                    default:
                        break;
                }
            } else {
                toBeSent = 1;
                int size = id.getSize();
                switch (id) {
                    case MY_ADDRESS:
                        packet.setParams(myAddress.getArray(), size);
                        break;
                    case MY_NET:
                        packet.setParams(new byte[]{(byte) myNet}, size);
                        break;
                    case BEACON_PERIOD:
                        packet.setParams(splitInteger(cnt_beacon_max), size);
                        break;
                    case REPORT_PERIOD:
                        packet.setParams(splitInteger(cnt_report_max), size);
                        break;
                    case RULE_TTL:
                        packet.setParams(new byte[]{(byte) cnt_updtable_max}, size);
                        break;
                    case PACKET_TTL:
                        packet.setParams(new byte[]{(byte) rule_ttl}, size);
                        break;
                    case RSSI_MIN:
                        packet.setParams(new byte[]{(byte) rssi_min}, size);
                        break;
                    case GET_ALIAS:
                        int aIndex = Byte.toUnsignedInt(value[0]);
                        if (aIndex < acceptedId.size()) {
                            byte[] tmp = acceptedId.get(aIndex).getArray();
                            packet.setParams(ByteBuffer.allocate(tmp.length + 1)
                                    .put((byte) aIndex).put(tmp).array(), -1);
                        } else {
                            toBeSent = 0;
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
                            toBeSent = 0;
                        }
                        break;
                    case GET_FUNCTION:
                        // TODO
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            log(Level.SEVERE, ex.toString());
        }
        return toBeSent;
    }

    void log(Level level, String logMessage) {
        try {
            logQueue.put(new Pair<>(level, logMessage));
        } catch (InterruptedException ex) {
            Logger.getGlobal().log(Level.SEVERE, null, ex);
        }
    }

    abstract void initSdnWiseSpecific();

    protected final void radioTX(final NetworkPacket np) {
        np.decrementTtl();
        txQueue.add(np);
    }

    abstract void reset();

    BeaconPacket prepareBeacon() {
        BeaconPacket bp = new BeaconPacket(
                myNet,
                myAddress,
                getActualSinkAddress(),
                sinkDistance,
                battery.getByteLevel());
        return bp;
    }

    ReportPacket prepareReport() {

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

    final void updateTable() {
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

    private class CustomClassLoader extends ClassLoader {

        public Class defClass(byte[] data, int len) {
            return defineClass(null, data, 0, len);
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
}
