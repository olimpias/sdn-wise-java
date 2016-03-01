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
package com.github.sdnwiselab.sdnwise.flowtable;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;

/**
 *
 * @author Sebastiano Milardo
 */
public abstract class AbstractForwardAction extends AbstractAction {

    private final static byte NX_HOP_HIGH_INDEX = 0;
    private final static byte NX_HOP_LOW_INDEX = 1;
    private final static byte SIZE = 2;

    public AbstractForwardAction(ActionType actionType) {
        super(actionType, SIZE);
    }

    public AbstractForwardAction(byte[] value) {
        super(value);
    }

    public final AbstractForwardAction setNextHop(NodeAddress addr) {
        setValue(NX_HOP_HIGH_INDEX, addr.getHigh());
        setValue(NX_HOP_LOW_INDEX, addr.getLow());
        return this;
    }

    public final NodeAddress getNextHop() {
        return new NodeAddress(
                this.getValue(NX_HOP_HIGH_INDEX),
                this.getValue(NX_HOP_LOW_INDEX));
    }
}
