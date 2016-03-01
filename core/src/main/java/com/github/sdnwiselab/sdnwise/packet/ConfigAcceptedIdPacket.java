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
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Sebastiano Milardo
 */
public class ConfigAcceptedIdPacket extends ConfigPacket {

    public final ConfigPacket setReadAcceptedAddressesValue() {
        return setRead().setConfigId(LIST_ACCEPTED);
    }

    public ConfigAcceptedIdPacket(NetworkPacket data) {
        super(data);
    }

    public ConfigAcceptedIdPacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
    }

    public final ConfigPacket setAddAcceptedAddressValue(NodeAddress addr) {
        return setWrite().setConfigId(ADD_ACCEPTED).setValue(addr.getHigh(), addr.getLow());
    }

    public final ConfigPacket setRemoveAcceptedAddressValue(NodeAddress addr) {
        return setWrite().setConfigId(REMOVE_ACCEPTED).setValue(addr.getHigh(), addr.getLow());
    }

    public List<NodeAddress> getAcceptedAddressesValues() {
        LinkedList<NodeAddress> list = new LinkedList<>();
        if (getConfigId() == LIST_ACCEPTED) {
            for (int i = 1; i < getPayloadSize(); i += 2) {
                if (getPayloadAt(i) != -1 && getPayloadAt(i + 1) != -1) {
                    list.add(new NodeAddress(
                            getPayloadAt(i) & 0xFF,
                            getPayloadAt(i + 1) & 0xFF)
                    );
                }
            }
        }
        return list;
    }

    public final ConfigPacket addAcceptedAddressAtIndex(NodeAddress address,
            int index) {
        setPayloadAt((address.getHigh()), index * 2 + 1);
        setPayloadAt((address.getLow()), index * 2 + 2);
        return this;
    }
}
