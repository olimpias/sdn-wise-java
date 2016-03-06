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
    public final static String toHex(byte[] data) {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            int v = Byte.toUnsignedInt(data[i]);

            buf.append(DIGITS.charAt(v >> 4));
            buf.append(DIGITS.charAt(v & 0xf));
        }

        return buf.toString();
    }

    public final void arraycopy(int[] src, int srcPos, byte[] dst, int dstPos, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstPos + i] = (byte) src[srcPos + i];
        }
    }

    public final void arraycopy(byte[] src, int srcPos, int[] dst, int dstPos, int len) {
        for (int i = 0; i < len; i++) {
            dst[dstPos + i] = src[srcPos + i];
        }
    }

    public final static int mergeBytes(int high, int low) {
        high = (byte) high;
        low = (byte) low;
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    public final static byte[] splitInteger(int value) {
        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort((short) value);
        return b.array();
    }

    public final static int getBitRange(int b, int s, int n) {
        return (((b & 0xFF) >> (s & 0xFF)) & ((1 << (n & 0xFF)) - 1)) & 0xFF;
    }

    public final static int setBitRange(int val, int start, int len, int newVal) {
        int mask = ((1 << len) - 1) << start;
        return (val & ~mask) | ((newVal << start) & mask);
    }

    public final static byte[] concatByteArray(byte[] a, byte[] b) {
        return ByteBuffer.allocate(a.length + b.length).put(a).put(b).array();
    }
}
