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
import static com.github.sdnwiselab.sdnwise.util.Utils.*;
import java.util.Arrays;

/**
 * Window is part of the structure of the Entry of a FlowTable. This Class
 * implements FlowTableInterface.
 *
 * @author Sebastiano Milardo
 */
public class Window implements FlowTableInterface {

    /**
     * | Operator | Operator | Operator | Left Location | Left Location | Right
     * Location | Right Location | Size - | | | | | Left High byte | | | - | | |
     * | | Left Low Byte | | | - | | | | | Right High Byte | | | - | | | | |
     * Right Low Byte | | | -
     */
    public final static byte SIZE = 5;

    // size
    public final static byte SDN_WISE_SIZE_1 = 0;
    public final static byte SDN_WISE_SIZE_2 = 1;

    // operators
    public final static byte SDN_WISE_EQUAL = 0;
    public final static byte SDN_WISE_NOT_EQUAL = 1;
    public final static byte SDN_WISE_BIGGER = 2;
    public final static byte SDN_WISE_LESS = 3;
    public final static byte SDN_WISE_EQUAL_OR_BIGGER = 4;
    public final static byte SDN_WISE_EQUAL_OR_LESS = 5;

    private final static byte operatorBit = 5;
    private final static byte operatorLen = 3;
    private final static byte leftBit = 3;
    private final static byte leftLen = 2;
    private final static byte rightBit = 1;
    private final static byte rightLen = leftLen;
    private final static byte sizeBit = 0;
    private final static byte sizeLen = 1;

    private final static byte operationIndex = 0;
    private final static byte leftHighIndex = 1;
    private final static byte leftLowIndex = 2;
    private final static byte rightHighIndex = 3;
    private final static byte rightLowIndex = 4;

    public static Window fromString(String val) {
        Window w = new Window();
        String[] operands = val.split(" ");
        if (operands.length == 3) {
            // TODO setSize missing for lhs and rhs default 1
            String lhs = operands[0];
            int[] tmpLhs = w.getOperandFromString(lhs);
            w.setLhsLocation(tmpLhs[0]);
            w.setLhs(tmpLhs[1]);

            w.setOperator(w.getOperatorFromString(operands[1]));

            String rhs = operands[2];
            int[] tmpRhs = w.getOperandFromString(rhs);
            w.setRhsLocation(tmpRhs[0]);
            w.setRhs(tmpRhs[1]);

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
    public Window(byte[] value) {
        if (value.length == SIZE) {
            System.arraycopy(value, 0, window, 0, value.length);
        } else {
            Arrays.fill(window, (byte) 0);
        }
    }

    /**
     * Getter method to obtain Size.
     *
     * @return an int value of size.
     */
    public int getSize() {
        return getBitRange(window[operationIndex], sizeBit, sizeLen);
    }

    /**
     * Getter method to obtain Operator.
     *
     * @return an int value of operator.
     */
    public int getOperator() {
        return getBitRange(window[operationIndex], operatorBit, operatorLen);
    }

    /**
     * Getter method to obtain lhs Location.
     *
     * @return an int value of location.
     */
    public int getLhsLocation() {
        return getBitRange(window[operationIndex], leftBit, leftLen);
    }

    /**
     * Getter method to obtain rhs Location.
     *
     * @return an int value of location.
     */
    public int getRhsLocation() {
        return getBitRange(window[operationIndex], rightBit, rightLen);
    }

    /**
     * Setter method to set operationIndex of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setSize(int value) {
        window[operationIndex] = (byte) setBitRange(window[operationIndex], sizeBit, sizeLen, value);
        return this;
    }

    /**
     * Setter method to set operationIndex of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setOperator(int value) {
        window[operationIndex] = (byte) setBitRange(window[operationIndex], operatorBit, operatorLen, value);
        return this;
    }

    /**
     * Setter method to set operationIndex of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setRhsLocation(int value) {
        window[operationIndex] = (byte) setBitRange(window[operationIndex], rightBit, rightLen, value);
        return this;
    }

    /**
     * Setter method to set operationIndex of window[].
     *
     * @param value value to set
     * @return this Window
     */
    public Window setLhsLocation(int value) {
        window[operationIndex] = (byte) setBitRange(window[operationIndex], leftBit, leftLen, value);
        return this;
    }

    /**
     * Getter method to obtain Pos.
     *
     * @return an int value of pos.
     */
    public int getLhs() {
        return Utils.mergeBytes(window[leftHighIndex], window[leftLowIndex]);
    }

    /**
     * Setter method to set offsetIndex of window[].
     *
     * @param val value to set
     * @return this Window
     */
    public Window setLhs(int val) {
        this.window[leftHighIndex] = (byte) ((val >> 8) & 0xFF);
        this.window[leftLowIndex] = (byte) (val & 0xFF);
        return this;
    }

    /**
     * Getter method to obtain High Value.
     *
     * @return an int value of high value.
     */
    public int getRhs() {
        return Utils.mergeBytes(window[rightHighIndex], window[rightLowIndex]);
    }

    /**
     * Setter method to set highValueIndex of window[].
     *
     * @param val value to set
     * @return this Window
     */
    public Window setRhs(int val) {
        this.window[rightHighIndex] = (byte) ((val >> 8) & 0xFF);
        this.window[rightLowIndex] = (byte) (val & 0xFF);
        return this;
    }

    @Override
    public String toString() {
        return this.getLhsToString()
                + this.getOperatorToString()
                + this.getRhsToString();
    }

    @Override
    public byte[] toByteArray() {
        return Arrays.copyOf(window, SIZE);
    }

    /**
     * Getter method to obtain Operator in String.
     *
     * @return a string of operator.
     */
    public String getOperatorToString() {
        switch (getOperator()) {
            case (SDN_WISE_EQUAL):
                return " == ";
            case (SDN_WISE_NOT_EQUAL):
                return " != ";
            case (SDN_WISE_BIGGER):
                return " > ";
            case (SDN_WISE_LESS):
                return " < ";
            case (SDN_WISE_EQUAL_OR_BIGGER):
                return " >= ";
            case (SDN_WISE_EQUAL_OR_LESS):
                return " <= ";
            default:
                return "";
        }
    }

    public int getOperatorFromString(String val) {
        switch (val) {
            case ("=="):
                return SDN_WISE_EQUAL;
            case ("!="):
                return SDN_WISE_NOT_EQUAL;
            case (">"):
                return SDN_WISE_BIGGER;
            case ("<"):
                return SDN_WISE_LESS;
            case (">="):
                return SDN_WISE_EQUAL_OR_BIGGER;
            case ("<="):
                return SDN_WISE_EQUAL_OR_LESS;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Getter method to obtain Size in string.
     *
     * @return a string in size.
     */
    public String getSizeToString() {
        return String.valueOf(getSize() + 1);
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

    public int[] getOperandFromString(String val) {
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

    public int getValueFromString(String val) {
        return Integer.parseInt(val.split("\\.")[1]);
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

    @Override
    public boolean equals(Object obj) {
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Arrays.hashCode(this.window);
        return hash;
    }

}
