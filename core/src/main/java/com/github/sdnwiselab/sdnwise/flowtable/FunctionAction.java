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

import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.ActionType.FUNCTION;
import com.github.sdnwiselab.sdnwise.util.Utils;

/**
 *
 * @author Sebastiano Milardo
 */
public class FunctionAction extends AbstractAction {

    private final static byte SIZE = 7;

    // convert to array
    private final static byte idIndex = 0;
    private final static byte arg0Index = 1;
    private final static byte arg1Index = 2;
    private final static byte arg2Index = 3;
    private final static byte arg3Index = 4;
    private final static byte arg4Index = 5;
    private final static byte arg5Index = 6;

    public FunctionAction(byte[] value) {
        super(value);
    }

    public final FunctionAction setCallbackId(int id) {
        setValue(idIndex, id);
        return this;
    }

    public int getCallbackId() {
        return getValue(idIndex);
    }

    public int getArg0() {
        return Utils.mergeBytes(getValue(arg0Index), getValue(arg1Index));
    }

    public int getArg1() {
        return Utils.mergeBytes(getValue(arg2Index), getValue(arg3Index));
    }

    public int getArg2() {
        return Utils.mergeBytes(getValue(arg4Index), getValue(arg5Index));
    }

    public FunctionAction setArg0(int argument) {
        byte[] tmp = Utils.splitInteger(argument);
        setValue(arg0Index, tmp[0]);
        setValue(arg1Index, tmp[1]);
        return this;
    }

    public FunctionAction setArg1(int argument) {
        byte[] tmp = Utils.splitInteger(argument);
        setValue(arg2Index, tmp[0]);
        setValue(arg3Index, tmp[1]);
        return this;
    }

    public FunctionAction setArg2(int argument) {
        byte[] tmp = Utils.splitInteger(argument);
        setValue(arg4Index, tmp[0]);
        setValue(arg5Index, tmp[1]);
        return this;
    }

    @Override
    public String toString() {
        return FUNCTION.name() + " " + getCallbackId() + " "
                + getValue(arg0Index) + " "
                + getValue(arg1Index) + " "
                + getValue(arg2Index) + " "
                + getValue(arg3Index) + " "
                + getValue(arg4Index) + " "
                + getValue(arg5Index);
    }

    public FunctionAction(String str) {
        super(FUNCTION, SIZE);
        String[] tmp = str.split(" ");
        if (tmp[0].equals(FUNCTION.name())) {
            setCallbackId(Integer.parseInt(tmp[1]));
            setValue(arg0Index, Integer.parseInt(tmp[2]));
            setValue(arg1Index, Integer.parseInt(tmp[3]));
            setValue(arg2Index, Integer.parseInt(tmp[4]));
            setValue(arg3Index, Integer.parseInt(tmp[5]));
            setValue(arg4Index, Integer.parseInt(tmp[6]));
            setValue(arg5Index, Integer.parseInt(tmp[7]));
        } else {
            throw new IllegalArgumentException();
        }
    }
}
