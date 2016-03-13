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

import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.Action.SET;
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

    private static final byte OP_BIT = 3,
            OP_LEN = 3,
            LEFT_BIT = 1,
            LEFT_LEN = 2,
            RIGHT_BIT = 6,
            RIGHT_LEN = LEFT_LEN,
            RES_BIT = 0,
            RES_LEN = 1,
            OP_INDEX = 0,
            RES_INDEX_H = 1,
            RES_INDEX_L = 2,
            LEFT_INDEX_H = 3,
            LEFT_INDEX_L = 4,
            RIGHT_INDEX_H = 5,
            RIGHT_INDEX_L = 6;

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
        return getBitRange(getValue(OP_INDEX), RES_BIT, RES_LEN) + 2;
    }

    /**
     * Getter method to obtain Operator.
     *
     * @return an int value of operator.
     */
    public int getOperator() {
        return getBitRange(getValue(OP_INDEX), OP_BIT, OP_LEN);
    }

    /**
     * Getter method to obtain lhs Location.
     *
     * @return an int value of location.
     */
    public int getLhsLocation() {
        return getBitRange(getValue(OP_INDEX), LEFT_BIT, LEFT_LEN);
    }

    /**
     * Getter method to obtain rhs Location.
     *
     * @return an int value of location.
     */
    public int getRhsLocation() {
        return getBitRange(getValue(OP_INDEX), RIGHT_BIT, RIGHT_LEN);
    }

    /**
     * Setter method to set OP_INDEX of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setResLocation(final int value) {
        setValue(OP_INDEX, (byte) setBitRange(getValue(OP_INDEX),
                RES_BIT, RES_LEN, value));
        return this;
    }

    /**
     * Setter method to set OP_INDEX of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setOperator(final int value) {
        setValue(OP_INDEX, (byte) setBitRange(getValue(OP_INDEX),
                OP_BIT, OP_LEN, value));
        return this;
    }

    /**
     * Setter method to set OP_INDEX of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setRhsLocation(final int value) {
        setValue(OP_INDEX, (byte) setBitRange(getValue(OP_INDEX),
                RIGHT_BIT, RIGHT_LEN, value));
        return this;
    }

    /**
     * Setter method to set OP_INDEX of action[].
     *
     * @param value value to set
     * @return this Window
     */
    public SetAction setLhsLocation(final int value) {
        setValue(OP_INDEX, (byte) setBitRange(getValue(OP_INDEX),
                LEFT_BIT, LEFT_LEN, value));
        return this;
    }

    /**
     * Getter method to obtain Pos.
     *
     * @return an int value of pos.
     */
    public int getLhs() {
        return mergeBytes(getValue(LEFT_INDEX_H), getValue(LEFT_INDEX_L));
    }

    /**
     * Setter method to set offsetIndex of action[].
     *
     * @param val value to set
     * @return this Window
     */
    public SetAction setLhs(final int val) {
        setValue(LEFT_INDEX_L, (byte) val);
        setValue(LEFT_INDEX_H, (byte) val >>> Byte.SIZE);
        return this;
    }

    /**
     * Getter method to obtain High Value.
     *
     * @return an int value of high value.
     */
    public int getRes() {
        return mergeBytes(getValue(RES_INDEX_H), getValue(RES_INDEX_L));
    }

    /**
     * Getter method to obtain High Value.
     *
     * @return an int value of high value.
     */
    public int getRhs() {
        return mergeBytes(getValue(RIGHT_INDEX_H), getValue(RIGHT_INDEX_L));
    }

    /**
     * Setter method to set highValueIndex of action[].
     *
     * @param val value to set
     * @return this Window
     */
    public SetAction setRes(final int val) {
        setValue(RES_INDEX_L, (byte) val);
        setValue(RES_INDEX_H, (byte) val >>> Byte.SIZE);
        return this;
    }

    /**
     * Setter method to set highValueIndex of action[].
     *
     * @param val value to set
     * @return this Window
     */
    public SetAction setRhs(final int val) {
        setValue(RIGHT_INDEX_L, (byte) val);
        setValue(RIGHT_INDEX_H, (byte) val >>> Byte.SIZE);
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
     * Gets the operator as a String.
     *
     * @return a string representation of the operator.
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

    /**
     * Gets the operator id from a String.
     *
     * @param val the char representing the operator
     * @return the operator id starting from a string.
     */
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
                break;
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
