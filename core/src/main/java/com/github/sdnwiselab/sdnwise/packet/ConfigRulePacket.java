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

import com.github.sdnwiselab.sdnwise.flowtable.FlowTableEntry;
import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;

/**
 *
 * @author Sebastiano Milardo
 */
public class ConfigRulePacket extends ConfigPacket {

    public ConfigRulePacket(NetworkPacket data) {
        super(data);
    }

    public ConfigRulePacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
    }

    public final ConfigRulePacket setAddRuleValue(FlowTableEntry flow) {
        byte[] flowArray = flow.toByteArray();
        this.setWrite()
                .setConfigId(ADD_RULE)
                .setPayload(flowArray, (byte) 0, (byte) 1, (byte) flowArray.length);
        return this;
    }

    public final ConfigRulePacket setAddRuleAtPositionValue(FlowTableEntry flow,
            int index) {
        byte[] flowArray = flow.toByteArray();
        this.setWrite()
                .setConfigId(ADD_RULE)
                .setValue(index)
                .setPayload(flowArray, (byte) 0, (byte) 3, (byte) flowArray.length);
        return this;
    }

    public final ConfigRulePacket setRemoveRuleValue(FlowTableEntry flow) {
        byte[] flowArray = flow.toByteArray();
        this.setWrite()
                .setConfigId(REMOVE_RULE)
                .setPayload(flowArray, (byte) 0, (byte) 1, (byte) flowArray.length);
        return this;
    }

    public final ConfigRulePacket setRemoveRuleAtPositionValue(int index) {
        setWrite().setConfigId(REMOVE_RULE_INDEX).setValue(index);
        return this;
    }

    public final ConfigRulePacket setGetRuleAtIndexValue(int index) {
        setRead().setConfigId(GET_RULE_INDEX).setValue(index);
        return this;
    }

    public FlowTableEntry getRule() {
        FlowTableEntry rule = null;
        if (getConfigId() == GET_RULE_INDEX) {
            rule = new FlowTableEntry(this.copyPayloadOfRange(3, this.getPayloadSize()));
        }
        return rule;
    }

    public ConfigRulePacket setRule(FlowTableEntry fte) {
        byte[] tmp = fte.toByteArray();
        this.setPayload(tmp, (byte) 0, (byte) 3, tmp.length);
        return this;
    }
}
