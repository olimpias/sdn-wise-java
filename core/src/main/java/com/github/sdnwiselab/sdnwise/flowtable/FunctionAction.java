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

import static com.github.sdnwiselab.sdnwise.flowtable.AbstractAction.Action.FUNCTION;
import java.util.Arrays;

/**
 * @author Sebastiano Milardo
 */
public final class FunctionAction extends AbstractAction {

    /**
     * Function id is at index 0, the arguments starts at 1.
     */
    private static final byte ID_INDEX = 0,
            ARGS_INDEX = 1;

    public FunctionAction(final byte[] value) {
        super(value);
    }

    public FunctionAction setId(final int id) {
        setValue(ID_INDEX, id);
        return this;
    }

    public int getId() {
        return getValue(ID_INDEX);
    }

    public byte[] getArgs() {
        byte[] value = getValue();
        return Arrays.copyOfRange(value, ARGS_INDEX, value.length);
    }

    public FunctionAction setArgs(final byte[] args) {
        int i = 0;
        for (byte b : args) {
            this.setValue(ARGS_INDEX + i, b);
            i++;
        }
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(FUNCTION.name());
        sb.append(" ").append(getId()).append(" ");
        for (byte b : getArgs()) {
            sb.append(Byte.toUnsignedInt(b)).append(" ");
        }
        return sb.toString();
    }

    public FunctionAction(final String str) {
        super(FUNCTION, 0);
        String[] tmp = str.split(" ");
        if (tmp[0].equals(FUNCTION.name())) {
            byte[] args = new byte[tmp.length - 1];
            for (int i = 0; i < args.length; i++) {
                args[i] = (byte) (Integer.parseInt(tmp[i + 1]));
            }
            this.setValue(args);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
