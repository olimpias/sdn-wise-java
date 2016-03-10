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
package com.github.sdnwiselab.sdnwise.util;

import java.nio.ByteBuffer;

/**
 * @author Sebastiano Milardo
 */
public class Utils {

    private static final String DIGITS = "0123456789abcdef";
    
    /**
     * Return the passed in byte array as a hex string.
     *
     * @param data the bytes to be converted.
     * @return a hex representation of data.
     */
    public static final String toHex(final byte[] data) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            int v = Byte.toUnsignedInt(data[i]);
            buf.append(DIGITS.charAt(v >> 4));
            buf.append(DIGITS.charAt(v & 0xf));
        }

        return buf.toString();
    }

    public static final int mergeBytes(final int high, final int low) {
        return (((byte) high & 0xFF) << 8) | ((byte) low & 0xFF);
    }

    public static final byte[] splitInteger(final int value) {
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort((short) value);
        return b.array();
    }

    public static final int getBitRange(final int b, final int s, final int n) {
        return (((b & 0xFF) >> (s & 0xFF)) & ((1 << (n & 0xFF)) - 1)) & 0xFF;
    }

    public static final int setBitRange(final int val,
            final int start, final int len, final int newVal) {
        int mask = ((1 << len) - 1) << start;
        return (val & ~mask) | ((newVal << start) & mask);
    }

    public static final byte[] concatByteArray(final byte[] a, final byte[] b) {
        return ByteBuffer.allocate(a.length + b.length).put(a).put(b).array();
    }
}
