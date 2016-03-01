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
package com.github.sdnwiselab.sdnwise.packet;

import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import com.github.sdnwiselab.sdnwise.util.*;

/**
 *
 * @author Sebastiano Milardo
 */
public class ConfigTimerPacket extends ConfigPacket {

    public final ConfigPacket setReadBeaconPeriodValue() {
        return setRead().setConfigId(BEACON_MAX);
    }

    public final ConfigPacket setReadReportPeriodValue() {
        return setRead().setConfigId(REPORT_MAX);
    }

    public final ConfigPacket setReadUpdateTablePeriodValue() {
        return setRead().setConfigId(UPDTABLE_MAX);
    }

    public final ConfigPacket setReadSleepIntervalValue() {
        return setRead().setConfigId(SLEEP_MAX);
    }

    public ConfigTimerPacket(NetworkPacket data) {
        super(data);
    }

    public ConfigTimerPacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
    }

    public final ConfigPacket setBeaconPeriodValue(int period) {
        return setWrite().setConfigId(BEACON_MAX).setValue(period);
    }

    public final ConfigPacket setReportPeriodValue(int period) {
        return setWrite().setConfigId(REPORT_MAX).setValue(period);
    }

    public final ConfigPacket setUpdateTablePeriodValue(int period) {
        return setWrite().setConfigId(UPDTABLE_MAX).setValue(period);
    }

    public final ConfigPacket setSleepIntervalValue(int period) {
        return setWrite().setConfigId(SLEEP_MAX).setValue(period);
    }

    public final int getBeaconPeriodValue() {
        if (getConfigId() == BEACON_MAX) {
            return getValue();
        } else {
            return -1;
        }
    }

    public final int getReportPeriodValue() {
        if (getConfigId() == REPORT_MAX) {
            return getValue();
        } else {
            return -1;
        }
    }

    public final int getUpdateTablePeriodValue() {
        if (getConfigId() == UPDTABLE_MAX) {
            return getValue();
        } else {
            return -1;
        }
    }

    public final int getSleepIntervalValue() {
        if (getConfigId() == SLEEP_MAX) {
            return getValue();
        } else {
            return -1;
        }
    }

}
