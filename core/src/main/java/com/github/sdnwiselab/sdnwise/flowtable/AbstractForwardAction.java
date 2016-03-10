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
 * @author Sebastiano Milardo
 */
public abstract class AbstractForwardAction extends AbstractAction {

    private static final byte NXH_INDEX = 0;

    public AbstractForwardAction(ActionType actionType) {
        super(actionType, 2);
    }

    public AbstractForwardAction(byte[] value) {
        super(value);
    }

    public final AbstractForwardAction setNextHop(NodeAddress addr) {
        setValue(NXH_INDEX, addr.getHigh());
        setValue(NXH_INDEX + 1, addr.getLow());
        return this;
    }

    public final NodeAddress getNextHop() {
        return new NodeAddress(getValue(NXH_INDEX), getValue(NXH_INDEX + 1));
    }
}
