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
        return Arrays.copyOfRange(action, ARGS_INDEX, action.length);
    }

    public FunctionAction setArgs(final byte[] args) {
        System.arraycopy(args, 0, action, ARGS_INDEX, args.length);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(FUNCTION.name());
        sb.append(" ").append(getId()).append(" ");
        for (int i = 2; i < action.length; i++) {
            sb.append(action[i]).append(" ");
        }
        return sb.toString();
    }

    public FunctionAction(final String str) {
        super(FUNCTION, 0);
        String[] tmp = str.split(" ");

        if (tmp[0].equals(FUNCTION.name())) {
            action = new byte[tmp.length];
            setType(FUNCTION);
            setId(Integer.parseInt(tmp[1]));

            for (int i = 2; i < action.length; i++) {
                action[i] = (byte) (Integer.parseInt(tmp[i]));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
}
