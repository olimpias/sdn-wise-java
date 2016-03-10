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

/**
 * Public Interface for the FlowTable structure.
 *
 * @author Sebastiano Milardo
 */
public interface FlowTableInterface {

    /**
     * Location constants.
     */
    byte SDN_WISE_NULL = 0,
        SDN_WISE_CONST = 1,
        SDN_WISE_PACKET = 2,
        SDN_WISE_STATUS = 3;

    /**
     * Converts a FlowTable entry part in a byte array.
     *
     * @return byte array from an element.
     */
    byte[] toByteArray();
}
