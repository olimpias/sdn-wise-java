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

import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.ActionType.FORWARD_B;
import static com.github.sdnwiselab.sdnwise.util.NodeAddress.BROADCAST_ADDR;

/**
 * @author Sebastiano Milardo
 */
public final class ForwardBroadcastAction extends AbstractForwardAction {

    public ForwardBroadcastAction() {
        super(FORWARD_B);
        setNextHop(BROADCAST_ADDR);
    }

    public ForwardBroadcastAction(byte[] value) {
        super(value);
    }

}
