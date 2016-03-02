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
import static com.github.sdnwiselab.sdnwise.flowtable.Stats.SDN_WISE_RL_TTL_PERMANENT;
import com.github.sdnwiselab.sdnwise.flowtable.*;
import static com.github.sdnwiselab.sdnwise.flowtable.Window.*;
import com.github.sdnwiselab.sdnwise.function.FunctionInterface;
import static com.github.sdnwiselab.sdnwise.mote.core.Constants.*;
import static com.github.sdnwiselab.sdnwise.packet.ConfigAcceptedIdPacket.*;
import com.github.sdnwiselab.sdnwise.packet.*;
import com.github.sdnwiselab.sdnwise.util.*;
import static com.github.sdnwiselab.sdnwise.util.NodeAddress.BROADCAST_ADDR;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.*;

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
    int flowTableSize,
            neighbors_number,
            myNetId,
            cnt_beacon_max,
            cnt_report_max,
            cnt_updtable_max,
            cnt_sleep_max,
            ttl_max,
            rssi_min;

    byte requestId;

    // The address of the node
    protected NodeAddress myAddress;

    // Neighbors
    protected ArrayList<Neighbor> neighborTable = new ArrayList<>(100);

    // WISE Flow Table
    protected ArrayList<FlowTableEntry> flowTable = new ArrayList<>(100);

    // Accepted IDs
    protected ArrayList<NodeAddress> acceptedId = new ArrayList<>(100);

    // Status Register
    protected ArrayList<Integer> statusRegister = new ArrayList<>();

    // Sensors
    protected HashMap<String, Object> sensors = new HashMap<>();

    // Functions
    HashMap<Integer, LinkedList<byte[]>> functionBuffer = new HashMap<>();
    HashMap<Integer, FunctionInterface> functions = new HashMap<>();

    AbstractCore(byte netId, NodeAddress address, Battery battery) {
        this.myAddress = address;
        this.myNetId = netId;
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
                log(Level.SEVERE, ex.getLocalizedMessage());
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
        return flowTableSize;
    }

    public final NodeAddress getMyAddress() {
        return myAddress;
    }

    public int getNetId() {
        return myNetId;
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
        initNeighborTable();
        initAcceptedId();
        initStatusRegister();
        initSdnWise();
        new Thread(new rxPacketManager()).start();
        new Thread(new ftPacketManager()).start();
    }

    private void initFlowTable() {
        FlowTableEntry toSink = new FlowTableEntry();
        toSink.addWindow(new Window()
                .setOperator(SDN_WISE_EQUAL)
                .setSize(SDN_WISE_SIZE_2)
                .setLhsLocation(SDN_WISE_PACKET)
                .setLhs(DST_INDEX)
                .setRhsLocation(SDN_WISE_CONST)
                .setRhs(this.myAddress.intValue()));
        toSink.addWindow(Window.fromString("P.TYP == 3"));
        toSink.addAction(new ForwardUnicastAction(myAddress));
        toSink.getStats().setPermanent();
        flowTable.add(0, toSink);

        for (int i = 1; i < SDN_WISE_RLS_MAX; i++) {
            flowTable.add(i, new FlowTableEntry());
        }
    }

    private void initNeighborTable() {
        for (int i = 0; i < SDN_WISE_NEIGHBORS_MAX; i++) {
            neighborTable.add(i, new Neighbor());
        }
        neighbors_number = 0;
    }

    private void initAcceptedId() {
        for (int i = 0; i < SDN_WISE_ACCEPTED_ID_MAX; i++) {
            acceptedId.add(i, BROADCAST_ADDR);
        }
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
            log(Level.SEVERE, ex.getLocalizedMessage());
        }
        return srvI;
    }

    protected void rxHandler(NetworkPacket packet, int rssi) {

        if (!packet.isSdnWise()) {
            runFlowMatch(packet);
        } else if (packet.getLen() > SDN_WISE_DFLT_HDR_LEN
                && packet.getNet() == myNetId
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
            insertRule(rp.getRule(), searchRule(rp.getRule()));
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
                        .setOperator(SDN_WISE_EQUAL)
                        .setSize(SDN_WISE_SIZE_2)
                        .setLhsLocation(SDN_WISE_PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(SDN_WISE_CONST)
                        .setRhs(path.get(0).intValue()));

                rule.getWindows().addAll(opp.getOptionalWindows());
                rule.addAction(new ForwardUnicastAction(path.get(i - 1)));

                int p = searchRule(rule);
                insertRule(rule, p);
            }

            if (i < (path.size() - 1)) {
                FlowTableEntry rule = new FlowTableEntry();
                rule.addWindow(new Window()
                        .setOperator(SDN_WISE_EQUAL)
                        .setSize(SDN_WISE_SIZE_2)
                        .setLhsLocation(SDN_WISE_PACKET)
                        .setLhs(DST_INDEX)
                        .setRhsLocation(SDN_WISE_CONST)
                        .setRhs(path.get(path.size() - 1).intValue()));

                rule.getWindows().addAll(opp.getOptionalWindows());
                rule.addAction(new ForwardUnicastAction(path.get(i + 1)));

                int p = searchRule(rule);
                insertRule(rule, p);

                opp.setDst(path.get(i + 1));
                opp.setNxh(path.get(i + 1));
                radioTX(opp);
            }

        } else {
            runFlowMatch(opp);
        }
    }

    protected void insertRule(FlowTableEntry rule, int pos) {
        if (pos >= SDN_WISE_RLS_MAX) {
            pos = flowTableSize;
            flowTableSize++;
            if (flowTableSize >= SDN_WISE_RLS_MAX) {
                flowTableSize = 1;
            }
        }
        log(Level.INFO, "Inserting rule " + rule + " at position " + pos);
        flowTable.set(pos, rule);
    }

    protected int searchRule(FlowTableEntry rule) {
        int i, j, sum, target;
        for (i = 0; i < SDN_WISE_RLS_MAX; i++) {
            sum = 0;
            target = rule.getWindows().size();
            if (flowTable.get(i).getWindows().size() == target) {
                for (j = 0; j < rule.getWindows().size(); j++) {
                    if (flowTable.get(i).getWindows().get(j).equals(rule.getWindows().get(j))) {
                        sum++;
                    }
                }
            }
            if (sum == target) {
                return i;
            }
        }
        return SDN_WISE_RLS_MAX + 1;
    }

    protected boolean isAcceptedIdAddress(NodeAddress addrP) {
        return (addrP.equals(myAddress)
                || addrP.isBroadcast()
                || (searchAcceptedId(addrP)
                != SDN_WISE_ACCEPTED_ID_MAX + 1));
    }

    protected boolean isAcceptedIdPacket(NetworkPacket packet) {
        return isAcceptedIdAddress(packet.getDst());
    }

    private int getOperand(NetworkPacket packet, int size, int location, int value) {
        switch (location) {
            case SDN_WISE_NULL:
                return 0;
            case SDN_WISE_CONST:
                return value;
            case SDN_WISE_PACKET:
                int[] intPacket = packet.toIntArray();
                if (size == SDN_WISE_SIZE_1) {
                    if (value >= intPacket.length) {
                        return -1;
                    }
                    return intPacket[value];
                }
                if (size == SDN_WISE_SIZE_2) {
                    if (value + 1 >= intPacket.length) {
                        return -1;
                    }
                    return Utils.mergeBytes(intPacket[value], intPacket[value + 1]);
                }
            case SDN_WISE_STATUS:
                if (size == SDN_WISE_SIZE_1) {
                    if (value >= statusRegister.size()) {
                        return -1;
                    }
                    return statusRegister.get(value);
                }
                if (size == SDN_WISE_SIZE_2) {
                    if (value + 1 >= statusRegister.size()) {
                        return -1;
                    }
                    return Utils.mergeBytes(
                            statusRegister.get(value),
                            statusRegister.get(value + 1));
                }
        }
        return -1;
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
                    int lhs = getOperand(
                            np, SDN_WISE_SIZE_1, ftam.getLhsLocation(), ftam.getLhs());
                    int rhs = getOperand(
                            np, SDN_WISE_SIZE_1, ftam.getRhsLocation(), ftam.getRhs());
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
                    FunctionInterface srvI = functions.get(ftac.getCallbackId());
                    if (srvI != null) {
                        log(Level.INFO, "Function called: " + myAddress);
                        srvI.function(sensors,
                                flowTable,
                                neighborTable,
                                statusRegister,
                                acceptedId,
                                ftQueue,
                                txQueue,
                                ftac.getArg0(),
                                ftac.getArg1(),
                                ftac.getArg2(),
                                np
                        );
                    }
                    break;
                case ASK:
                    RequestPacket[] rps = RequestPacket.createPackets((byte) myNetId, myAddress, getActualSinkAddress(), requestId++, np.toByteArray());

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
            log(Level.SEVERE, ex.getLocalizedMessage());
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
            case SDN_WISE_EQUAL:
                return item1 == item2 ? 1 : 0;
            case SDN_WISE_NOT_EQUAL:
                return item1 != item2 ? 1 : 0;
            case SDN_WISE_BIGGER:
                return item1 > item2 ? 1 : 0;
            case SDN_WISE_LESS:
                return item1 < item2 ? 1 : 0;
            case SDN_WISE_EQUAL_OR_BIGGER:
                return item1 >= item2 ? 1 : 0;
            case SDN_WISE_EQUAL_OR_LESS:
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
        ttl_max = SDN_WISE_DFLT_TTL_MAX;
        flowTableSize = 1;
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
            SDN_WISE_Callback(packet);
        } else if (isAcceptedIdAddress(packet.getNxh())) {
            runFlowMatch(packet);
        }
    }

    protected void rxBeacon(BeaconPacket bp, int rssi) {
        int index = getNeighborIndex(bp.getSrc());

        if (index != (SDN_WISE_NEIGHBORS_MAX + 1)) {
            if (index != -1) {
                neighborTable.get(index).setRssi(rssi);
                neighborTable.get(index).setBatt(bp.getBatt());
            } else {
                neighborTable.get(neighbors_number).setAddr(bp.getSrc());
                neighborTable.get(neighbors_number).setRssi(rssi);
                neighborTable.get(neighbors_number).setBatt(bp.getBatt());
                neighbors_number++;
            }
        }
    }

    protected final void runFlowMatch(NetworkPacket packet) {
        int j, i;
        boolean matched = false;
        for (j = 0; j < SDN_WISE_RLS_MAX; j++) {
            i = getActualFlowIndex(j);
            if (matchRule(flowTable.get(i), packet) == 1) {
                log(Level.FINE, "Matched Rule #" + j + " " + flowTable.get(i).toString());
                matched = true;
                for (AbstractAction a : flowTable.get(i).getActions()) {
                    runAction(a, packet);
                }
                flowTable.get(i).getStats()
                        .setCounter(flowTable.get(i).getStats().getCounter() + 1);
                break;
            }
        }
        if (!matched) {
            // send a rule request
            RequestPacket[] rps = RequestPacket.createPackets((byte) myNetId, myAddress, getActualSinkAddress(), requestId++, packet.toByteArray());

            for (RequestPacket rp : rps) {
                controllerTX(rp);
            }
        }
    }

    protected abstract void rxConfig(ConfigPacket packet);

    protected NodeAddress getActualSinkAddress() {
        return new NodeAddress(flowTable.get(0).getWindows().get(0).getRhs());
    }

    protected abstract void SDN_WISE_Callback(DataPacket packet);

    protected abstract void controllerTX(NetworkPacket pck);

    protected int marshalPacket(ConfigPacket packet) {
        int toBeSent = 0;
        int pos;
        boolean isWrite = packet.isWrite();
        ConfigProperty id = packet.getConfigId();
        int value = packet.getValue();
        if (isWrite) {
            switch (id) {
                case ADDRESS:
                    myAddress = new NodeAddress(value);
                    break;
                case NET_ID:
                    myNetId = value;
                    break;
                case BEACON_MAX:
                    cnt_beacon_max = value;
                    break;
                case REPORT_MAX:
                    cnt_report_max = value;
                    break;
                case UPDTABLE_MAX:
                    cnt_updtable_max = value;
                    break;
                case SLEEP_MAX:
                    cnt_sleep_max = value;
                    break;
                case TTL_MAX:
                    ttl_max = value;
                    break;
                case RSSI_MIN:
                    rssi_min = value;
                    break;
                case ADD_ACCEPTED:
                    pos = searchAcceptedId(new NodeAddress(value));
                    if (pos == (SDN_WISE_ACCEPTED_ID_MAX + 1)) {
                        pos = searchAcceptedId(new NodeAddress(65535));
                        acceptedId.set(pos, new NodeAddress(value));
                    }
                    break;
                case REMOVE_ACCEPTED:
                    pos = searchAcceptedId(new NodeAddress(value));
                    if (pos != (SDN_WISE_ACCEPTED_ID_MAX + 1)) {
                        acceptedId.set(pos, new NodeAddress(65535));
                    }
                    break;
                case REMOVE_RULE_INDEX:
                    if (value != 0) {
                        flowTable.set(getActualFlowIndex(value), new FlowTableEntry());
                    }
                    break;
                case REMOVE_RULE:
                case ADD_RULE:
                case RESET:
                    //TODO
                    break;
                case ADD_FUNCTION:
                    ConfigFunctionPacket cfp = new ConfigFunctionPacket(packet);

                    if (functionBuffer.get(value) == null) {
                        functionBuffer.put(value, new LinkedList<>());
                    }
                    functionBuffer.get(value).add(cfp.getFunctionPayload());
                    if (functionBuffer.get(value).size() == cfp.getTotalParts()) {
                        int total = 0;
                        total = functionBuffer.get(value).stream().map((n)
                                -> (n.length)).reduce(total, Integer::sum);
                        int pointer = 0;
                        byte[] func = new byte[total];
                        for (byte[] n : functionBuffer.get(value)) {
                            for (int j = 0; j < n.length; j++) {
                                func[pointer] = n[j];
                                pointer++;
                            }
                        }
                        functions.put(value, createServiceInterface(func));
                        log(Level.INFO, "New Function Added at position: " + value);
                        functionBuffer.remove(value);
                    }
                    break;
                case REMOVE_FUNCTION:
                    functions.remove(value);
                    break;
                default:
                    break;
            }
        } else {
            toBeSent = 1;
            switch (id) {
                case ADDRESS:
                    packet.setValue(myAddress.intValue());
                    break;
                case NET_ID:
                    packet.setValue(myNetId);
                    break;
                case BEACON_MAX:
                    packet.setValue(cnt_beacon_max);
                    break;
                case REPORT_MAX:
                    packet.setValue(cnt_report_max);
                    break;
                case UPDTABLE_MAX:
                    packet.setValue(cnt_updtable_max);
                    break;
                case SLEEP_MAX:
                    packet.setValue(cnt_sleep_max);
                    break;
                case TTL_MAX:
                    packet.setValue(ttl_max);
                    break;
                case RSSI_MIN:
                    packet.setValue(rssi_min);
                    break;
                case LIST_ACCEPTED:
                    toBeSent = 0;
                    ConfigAcceptedIdPacket packetList
                            = new ConfigAcceptedIdPacket(
                                    myNetId,
                                    packet.getDst(),
                                    packet.getSrc());
                    packetList.setReadAcceptedAddressesValue();

                    for (int jj = 0; jj < SDN_WISE_ACCEPTED_ID_MAX; jj++) {
                        if (!acceptedId.get(jj).isBroadcast()) {
                            packetList.addAcceptedAddressAtIndex(myAddress, jj);
                        }
                    }
                    controllerTX(packetList);
                    break;
                case GET_RULE_INDEX:
                    toBeSent = 0;
                    ConfigRulePacket packetRule = new ConfigRulePacket(
                            myNetId,
                            packet.getDst(),
                            packet.getSrc()
                    );
                    int jj = getActualFlowIndex(value);
                    packetRule.setRule(flowTable.get(jj))
                            .setGetRuleAtIndexValue(value);
                    controllerTX(packetRule);
                    break;
                default:
                    break;
            }
        }
        return toBeSent;
    }

    void log(Level level, String logMessage) {
        try {
            logQueue.put(new Pair<>(level, logMessage));
        } catch (InterruptedException ex) {
            Logger.getLogger(AbstractCore.class.getName()).log(Level.SEVERE, null, ex);
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
                myNetId,
                myAddress,
                getActualSinkAddress(),
                sinkDistance,
                battery.getByteLevel());
        return bp;
    }

    ReportPacket prepareReport() {

        ReportPacket rp = new ReportPacket(
                myNetId,
                myAddress,
                getActualSinkAddress(),
                sinkDistance,
                battery.getByteLevel());

        rp.setNeigh(neighbors_number)
                .setNxh(getNextHopVsSink());

        for (int j = 0; j < neighbors_number; j++) {
            rp.setNeighbourAddressAt(neighborTable.get(j).getAddr(), j)
                    .setNeighbourWeightAt((byte) neighborTable.get(j).getRssi(), j);
        }
        initNeighborTable();
        return rp;
    }

    final void updateTable() {
        for (int i = 0; i < SDN_WISE_RLS_MAX; i++) {
            FlowTableEntry tmp = flowTable.get(i);
            if (tmp.getWindows().size() > 1) {
                int ttl = tmp.getStats().getTtl();
                if (ttl != SDN_WISE_RL_TTL_PERMANENT) {
                    if (ttl >= SDN_WISE_RL_TTL_DECR) {
                        tmp.getStats().decrementTtl(SDN_WISE_RL_TTL_DECR);
                    } else {
                        flowTable.set(i, new FlowTableEntry());
                        log(Level.INFO, "Removing rule at position " + i);
                        if (i == 0) {
                            reset();
                        }
                    }
                }
            }
        }
    }

    final int getNeighborIndex(NodeAddress addr) {
        int i;
        for (i = 0; i < SDN_WISE_NEIGHBORS_MAX; i++) {
            if (neighborTable.get(i).getAddr().equals(addr)) {
                return i;
            }
            if (neighborTable.get(i).getAddr().isBroadcast()) {
                return -1;
            }
        }
        return SDN_WISE_NEIGHBORS_MAX + 1;
    }

    final int searchAcceptedId(NodeAddress addr) {
        int i;
        for (i = 0; i < SDN_WISE_ACCEPTED_ID_MAX; i++) {
            if (acceptedId.get(i).equals(addr)) {
                return i;
            }
        }
        return SDN_WISE_ACCEPTED_ID_MAX + 1;
    }

    final int getActualFlowIndex(int j) {
        //j = j % SDN_WISE_RLS_MAX;
        int i;
        if (j == 0) {
            i = 0;
        } else {
            i = flowTableSize - j;
            if (i == 0) {
                i = SDN_WISE_RLS_MAX - 1;
            } else if (i < 0) {
                i = SDN_WISE_RLS_MAX - 1 + i;
            }
        }
        return i;
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
                log(Level.SEVERE, ex.getLocalizedMessage());
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
                log(Level.SEVERE, ex.getLocalizedMessage());
            }
        }
    }
}
