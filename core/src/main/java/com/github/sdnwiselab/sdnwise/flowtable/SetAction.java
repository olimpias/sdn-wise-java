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

import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.ActionType.SET;
import com.github.sdnwiselab.sdnwise.packet.NetworkPacket;
import static com.github.sdnwiselab.sdnwise.util.Utils.getBitRange;
import static com.github.sdnwiselab.sdnwise.util.Utils.mergeBytes;
import static com.github.sdnwiselab.sdnwise.util.Utils.setBitRange;

/**
 * Window is part of the structure of the Entry of a FlowTable. This Class
 * implements FlowTableInterface.
 *
 * @author Sebastiano Milardo
 */
public final class SetAction extends AbstractAction {

    /**
     * SetAction operators.
     */
    public static final byte SDN_WISE_ADD = 0,
            SDN_WISE_SUB = 1,
            SDN_WISE_MUL = 2,
            SDN_WISE_DIV = 3,
            SDN_WISE_MOD = 4,
            SDN_WISE_AND = 5,
            SDN_WISE_OR = 6,
            SDN_WISE_XOR = 7;

    private static final byte operatorBit = 3;
    private static final byte operatorLen = 3;
    private static final byte leftBit = 1;
    private static final byte leftLen = 2;
    private static final byte rightBit = 6;
    private static final byte rightLen = leftLen;
    private static final byte resBit = 0;
    private static final byte resLen = 1;

    private static final byte operationIndex = 0;
    private static final byte resIndexH = 1;
    private static final byte resIndexL = 2;

    private static final byte leftIndexH = 3;
    private static final byte leftIndexL = 4;

    private static final byte rightIndexH = 5;
    private static final byte rightIndexL = 6;

    private static final byte SIZE = 7;

    /**
     * Simple constructor for the Window object.
     *
     * Set action[] values at zero.
     */
    public SetAction() {
        super(SET, SIZE);
    }

    public SetAction(final byte[] value) {
        super(value);
    }

    public SetAction(final String val) {
        super(SET, SIZE);
        String[] operands = val.split(" ");
        if (operands.length == 6) {
            String res = operands[1];
            String lhs = operands[3];
            String rhs = operands[5];

            int[] tmpRes = getResFromString(res);
            int[] tmpLhs = getOperandFromString(lhs);
            int[] tmpRhs = getOperandFromString(rhs);

            setResLocation(tmpRes[0]);
            setRes(tmpRes[1]);

            setLhsLocation(tmpLhs[0]);
            setLhs(tmpLhs[1]);

            setOperator(getOperatorFromString(operands[4]));

            setRhsLocation(tmpRhs[0]);
            setRhs(tmpRhs[1]);

        } else if (operands.length == 4) {

            String res = operands[1];
            String lhs = operands[3];

            int[] tmpRes = getResFromString(res);
            int[] tmpLhs = getOperandFromString(lhs);

            setResLocation(tmpRes[0]);
            setRes(tmpRes[1]);

            setLhsLocation(tmpLhs[0]);
            setLhs(tmpLhs[1]);

            setRhsLocation(SDN_WISE_NULL);
            setRhs(0);
        }

    }

    /**
     * Getter method to obtain Size.
     *
     * @return an int value of SIZE.
     */
    public int getResLocation() {
        return getBitRange(getValue(operationIndex), resBit, resLen) + 2;
    }

    /**
     * Getter method to obtain Operator.
     *
     * @return an int value of operator.
     */
    public int getOperator() {
        return getBitRange(getValue(operationIndex), operatorBit, operatorLen);
    }

    /**
     * Getter method to obtain lhs Location.
     *
     * @return an int value of location.
     */
    public int getLhsLocation() {
        return getBitRange(getValue(operationIndex), leftBit, leftLen);
    }

    /**
     * Getter method to obtain rhs Location.
     *
     * @return an int value of location.
     */
    public int getRhsLocation() {
        return getBitRange(getValue(operationIndex), rightBit, rightLen);
    }

    /**
     * Setter method to set operationIndex of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setResLocation(final int value) {
        setValue(operationIndex, (byte) setBitRange(getValue(operationIndex),
                resBit, resLen, value));
        return this;
    }

    /**
     * Setter method to set operationIndex of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setOperator(final int value) {
        setValue(operationIndex, (byte) setBitRange(getValue(operationIndex),
                operatorBit, operatorLen, value));
        return this;
    }

    /**
     * Setter method to set operationIndex of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setRhsLocation(final int value) {
        setValue(operationIndex, (byte) setBitRange(getValue(operationIndex),
                rightBit, rightLen, value));
        return this;
    }

    /**
     * Setter method to set operationIndex of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setLhsLocation(final int value) {
        setValue(operationIndex, (byte) setBitRange(getValue(operationIndex),
                leftBit, leftLen, value));
        return this;
    }

    /**
     * Getter method to obtain Pos.
     *
     * @return an int value of pos.
     */
    public int getLhs() {
        return mergeBytes(getValue(leftIndexH), getValue(leftIndexL));
    }

    /**
     * Setter method to set offsetIndex of action[].
     *
     * @param val value to set
     * @return this Window
     */
    public SetAction setLhs(final int val) {
        setValue(leftIndexL, (byte) (val & 0xFF));
        setValue(leftIndexH, (byte) (val >> 8));
        return this;
    }

    /**
     * Getter method to obtain High Value.
     *
     * @return an int value of high value.
     */
    public int getRes() {
        return mergeBytes(getValue(resIndexH), getValue(resIndexL));
    }

    /**
     * Getter method to obtain High Value.
     *
     * @return an int value of high value.
     */
    public int getRhs() {
        return mergeBytes(getValue(rightIndexH), getValue(rightIndexL));
    }

    /**
     * Setter method to set highValueIndex of action[].
     *
     * @param val value to set
     * @return this Window
     */
    public SetAction setRes(final int val) {
        setValue(resIndexL, (byte) (val & 0xFF));
        setValue(resIndexH, (byte) (val >> 8));
        return this;
    }

    /**
     * Setter method to set highValueIndex of action[].
     *
     * @param val value to set
     * @return this Window
     */
    public SetAction setRhs(final int val) {
        setValue(rightIndexL, (byte) (val & 0xFF));
        setValue(rightIndexH, (byte) (val >> 8));
        return this;
    }

    @Override
    public String toString() {
        String f = this.getResToString();
        String l = this.getLhsToString();
        String r = this.getRhsToString();
        String o = this.getOperatorToString();

        if (!l.isEmpty() && !r.isEmpty()) {
            return f + l + o + r;
        } else if (r.isEmpty()) {
            return f + l;
        } else {
            return f + r;
        }
    }

    /**
     * Getter method to obtain Operator in String.
     *
     * @return a string of operator.
     */
    public String getOperatorToString() {
        switch (getOperator()) {
            case (SDN_WISE_ADD):
                return " + ";
            case (SDN_WISE_SUB):
                return " - ";
            case (SDN_WISE_MUL):
                return " * ";
            case (SDN_WISE_DIV):
                return " / ";
            case (SDN_WISE_MOD):
                return " % ";
            case (SDN_WISE_AND):
                return " & ";
            case (SDN_WISE_OR):
                return " | ";
            case (SDN_WISE_XOR):
                return " ^ ";
            default:
                return "";
        }
    }

    public int getOperatorFromString(final String val) {
        switch (val.trim()) {
            case ("+"):
                return SDN_WISE_ADD;
            case ("-"):
                return SDN_WISE_SUB;
            case ("*"):
                return SDN_WISE_MUL;
            case ("/"):
                return SDN_WISE_DIV;
            case ("%"):
                return SDN_WISE_MOD;
            case ("&"):
                return SDN_WISE_AND;
            case ("|"):
                return SDN_WISE_OR;
            case ("^"):
                return SDN_WISE_XOR;
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Getter method to obtain Size in string.
     *
     * @return a string in SIZE.
     */
    public String getResToString() {
        switch (getResLocation()) {
            case SDN_WISE_PACKET:
                return SET.name() + " P."
                        + NetworkPacket.getNetworkPacketByteName(getRes())
                        + " = ";
            case SDN_WISE_STATUS:
                return SET.name() + " R." + getRes() + " = ";
            default:
                return "";
        }
    }

    /**
     * Getter method to obtain memory in string.
     *
     * @return a string value of memory.
     */
    public String getRhsToString() {
        switch (getRhsLocation()) {
            case SDN_WISE_NULL:
                return "";
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
     * Getter method to obtain memory in string.
     *
     * @return a string value of memory.
     */
    public String getLhsToString() {
        switch (getLhsLocation()) {
            case SDN_WISE_NULL:
                return "";
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
        }

        if (tmp[0] == SDN_WISE_PACKET) {
            tmp[1] = NetworkPacket.getNetworkPacketByteFromName(strVal[1]);
        } else if (tmp[0] == SDN_WISE_CONST) {
            tmp[1] = Integer.parseInt(strVal[0]);
        } else {
            tmp[1] = Integer.parseInt(strVal[1]);
        }
        return tmp;
    }

    public int[] getResFromString(final String val) {
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
                throw new IllegalArgumentException();
        }

        if (tmp[0] == SDN_WISE_PACKET) {
            tmp[1] = NetworkPacket.getNetworkPacketByteFromName(strVal[1]);
        } else {
            tmp[1] = Integer.parseInt(strVal[1]);
        }
        return tmp;
    }

}
