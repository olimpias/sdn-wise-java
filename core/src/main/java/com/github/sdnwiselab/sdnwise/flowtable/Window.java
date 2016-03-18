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

import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import com.github.sdnwiselab.sdnwise.util.Utils;
import static com.github.sdnwiselab.sdnwise.util.Utils.getBitRange;
import static com.github.sdnwiselab.sdnwise.util.Utils.setBitRange;
import java.util.Arrays;

/**
 * Window is part of the structure of the Entry of a FlowTable. This Class
 * implements FlowTableInterface.
 *
 * @author Sebastiano Milardo
 */
public final class Window implements FlowTableInterface {

    /**
     * Window Operators.
     */
    public static final byte EQUAL = 0,
            GREATER = 2,
            GREATER_OR_EQUAL = 4,
            LESS = 3,
            LESS_OR_EQUAL = 5,
            NOT_EQUAL = 1;

    public static final byte SIZE = 5;

    /**
     * Window Sizes.
     */
    public static final byte W_SIZE_1 = 0,
            W_SIZE_2 = 1;

    private static final byte LEFT_BIT = 3, LEFT_INDEX_H = 1, LEFT_INDEX_L = 2,
            LEFT_LEN = 2, OP_BIT = 5, OP_INDEX = 0, OP_LEN = 3,
            RIGHT_BIT = 1, RIGHT_INDEX_H = 3, RIGHT_INDEX_L = 4,
            RIGHT_LEN = LEFT_LEN,
            SIZE_BIT = 0,
            SIZE_LEN = 1;

    public static Window fromString(final String val) {
        Window w = new Window();
        String[] operands = val.split(" ");
        if (operands.length == 3) {
            String lhs = operands[0];
            int[] tmpLhs = w.getOperandFromString(lhs);
            w.setLhsLocation(tmpLhs[0]);
            w.setLhs(tmpLhs[1]);
            w.setOperator(w.getOperatorFromString(operands[1]));

            String rhs = operands[2];
            int[] tmpRhs = w.getOperandFromString(rhs);
            w.setRhsLocation(tmpRhs[0]);
            w.setRhs(tmpRhs[1]);

            if ("P.SRC".equals(lhs)
                    || "P.DST".equals(lhs)
                    || "P.NXH".equals(lhs)
                    || "P.SRC".equals(rhs)
                    || "P.DST".equals(rhs)
                    || "P.NXH".equals(rhs)) {
                w.setSize(W_SIZE_2);
            }
        }
        return w;
    }

    private final byte[] window = new byte[SIZE];

    /**
     * Simple constructor for the FlowTableWindow object.
     *
     * Set window[] values at zero.
     */
    public Window() {
        Arrays.fill(window, (byte) 0);
    }

    /**
     * Constructor for the FlowTableWindow object.
     *
     * @param value byte array contains value to copy in actions[]
     */
    public Window(final byte[] value) {
        if (value.length == SIZE) {
            System.arraycopy(value, 0, window, 0, value.length);
        } else {
            Arrays.fill(window, (byte) 0);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Window other = (Window) obj;
        return Arrays.equals(other.window, window);
    }

    /**
     * Getter method to obtain Pos.
     *
     * @return an int value of pos.
     */
    public int getLhs() {
        return Utils.mergeBytes(window[LEFT_INDEX_H], window[LEFT_INDEX_L]);
    }

    /**
     * Getter method to obtain lhs Location.
     *
     * @return an int value of location.
     */
    public int getLhsLocation() {
        return getBitRange(window[OP_INDEX], LEFT_BIT, LEFT_LEN);
    }

    /**
     * Getter method to obtain memory in string.
     *
     * @return a string value of memory.
     */
    public String getLhsToString() {
        switch (getLhsLocation()) {
            case SDN_WISE_CONST:
                return String.valueOf(this.getLhs());
            case SDN_WISE_PACKET:
                return "P." + NetworkPacket.getNetworkPacketByteName(getLhs());
            case SDN_WISE_STATUS:
                return "R." + getLhs();
            default:
                return "";
        }
    }

    public int[] getOperandFromString(final String val) {
        int[] tmp = new int[2];
        String[] strVal = val.split("\\.");
        switch (strVal[0]) {
            case "P":
                tmp[0] = SDN_WISE_PACKET;
                break;
            case "R":
                tmp[0] = SDN_WISE_STATUS;
                break;
            default:
                tmp[0] = SDN_WISE_CONST;
                break;
        }

        switch (tmp[0]) {
            case SDN_WISE_PACKET:
                tmp[1] = NetworkPacket.getNetworkPacketByteFromName(strVal[1]);
                break;
            case SDN_WISE_CONST:
                tmp[1] = Integer.parseInt(strVal[0]);
                break;
            default:
                tmp[1] = Integer.parseInt(strVal[1]);
                break;
        }
        return tmp;
    }

    /**
     * Getter method to obtain Operator.
     *
     * @return an int value of operator.
     */
    public int getOperator() {
        return getBitRange(window[OP_INDEX], OP_BIT, OP_LEN);
    }

    public int getOperatorFromString(final String val) {
        switch (val) {
            case ("=="):
                return EQUAL;
            case ("!="):
                return NOT_EQUAL;
            case (">"):
                return GREATER;
            case ("<"):
                return LESS;
            case (">="):
                return GREATER_OR_EQUAL;
            case ("<="):
                return LESS_OR_EQUAL;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Getter method to obtain Operator in String.
     *
     * @return a string of operator.
     */
    public String getOperatorToString() {
        switch (getOperator()) {
            case (EQUAL):
                return " == ";
            case (NOT_EQUAL):
                return " != ";
            case (GREATER):
                return " > ";
            case (LESS):
                return " < ";
            case (GREATER_OR_EQUAL):
                return " >= ";
            case (LESS_OR_EQUAL):
                return " <= ";
            default:
                return "";
        }
    }

    /**
     * Getter method to obtain High Value.
     *
     * @return an int value of high value.
     */
    public int getRhs() {
        return Utils.mergeBytes(window[RIGHT_INDEX_H], window[RIGHT_INDEX_L]);
    }

    /**
     * Getter method to obtain rhs Location.
     *
     * @return an int value of location.
     */
    public int getRhsLocation() {
        return getBitRange(window[OP_INDEX], RIGHT_BIT, RIGHT_LEN);
    }

    /**
     * Getter method to obtain memory in string.
     *
     * @return a string value of memory.
     */
    public String getRhsToString() {
        switch (getRhsLocation()) {
            case SDN_WISE_CONST:
                return String.valueOf(this.getRhs());
            case SDN_WISE_PACKET:
                return "P." + NetworkPacket.getNetworkPacketByteName(getRhs());
            case SDN_WISE_STATUS:
                return "R." + getRhs();
            default:
                return "";
        }
    }

    /**
     * Getter method to obtain Size.
     *
     * @return an int value of size.
     */
    public int getSize() {
        return getBitRange(window[OP_INDEX], SIZE_BIT, SIZE_LEN);
    }

    /**
     * Getter method to obtain Size in string.
     *
     * @return a string in size.
     */
    public String getSizeToString() {
        return String.valueOf(getSize() + 1);
    }

    public int getValueFromString(final String val) {
        return Integer.parseInt(val.split("\\.")[1]);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Arrays.hashCode(this.window);
        return hash;
    }

    /**
     * Setter method to set offsetIndex of window[].
     *
     * @param val value to set
     * @return this Window
     */
    public Window setLhs(final int val) {
        this.window[LEFT_INDEX_H] = (byte) (val >>> Byte.SIZE);
        this.window[LEFT_INDEX_L] = (byte) val;
        return this;
    }

    /**
     * Setter method to set OP_INDEX of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setLhsLocation(final int value) {
        window[OP_INDEX] = (byte) setBitRange(
                window[OP_INDEX], LEFT_BIT, LEFT_LEN, value);
        return this;
    }

    /**
     * Setter method to set OP_INDEX of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setOperator(final int value) {
        window[OP_INDEX] = (byte) setBitRange(
                window[OP_INDEX], OP_BIT, OP_LEN, value);
        return this;
    }

    /**
     * Setter method to set highValueIndex of window[].
     *
     * @param val value to set
     * @return this Window
     */
    public Window setRhs(final int val) {
        this.window[RIGHT_INDEX_H] = (byte) (val >>> Byte.SIZE);
        this.window[RIGHT_INDEX_L] = (byte) val;
        return this;
    }

    /**
     * Setter method to set OP_INDEX of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setRhsLocation(final int value) {
        window[OP_INDEX] = (byte) setBitRange(
                window[OP_INDEX], RIGHT_BIT, RIGHT_LEN, value);
        return this;
    }

    /**
     * Setter method to set OP_INDEX of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setSize(final int value) {
        window[OP_INDEX] = (byte) setBitRange(
                window[OP_INDEX], SIZE_BIT, SIZE_LEN, value);
        return this;
    }

    @Override
    public byte[] toByteArray() {
        return Arrays.copyOf(window, SIZE);
    }

    @Override
    public String toString() {
        return this.getLhsToString()
                + this.getOperatorToString()
                + this.getRhsToString();
    }

}
