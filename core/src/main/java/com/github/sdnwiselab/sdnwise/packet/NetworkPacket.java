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
package com.github.sdnwiselab.sdnwise.packet;

import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.io.*;
import java.util.Arrays;
import java.util.logging.*;

/**
 * This class represents a generic SDN-WISE packet.
 *
 * @author Sebastiano Milardo
 * @version 0.1
 */
public class NetworkPacket implements Cloneable {


    /**
     * The maximum length of a NetworkPAcket.
     */
    public final static byte MAX_PACKET_LENGTH = 116;

    /**
     * The indexes of the different fields in the packet.
     */
    public final static byte NET_INDEX = 0,
            LEN_INDEX = 1,
            DST_INDEX = 2,
            SRC_INDEX = 4,
            TYP_INDEX = 6,
            TTL_INDEX = 7,
            NXH_INDEX = 8,
            PLD_INDEX = 10;

    /**
     * The possible values of the type of a packet.
     */
    public final static byte DATA = 0,
            BEACON = 1,
            REPORT = 2,
            REQUEST = 3,
            RESPONSE = 4,
            OPEN_PATH = 5,
            CONFIG = 6,
            REG_PROXY = 7;

    /**
     * An SDN-WISE header is always 10 bytes long.
     */
    public final static byte SDN_WISE_DFLT_HDR_LEN = 10;
    
    /**
     * The maximum number of hops allowed in the network.
     */
    public final static byte SDN_WISE_DFLT_TTL_MAX = 100;
    
     /**
     * Returns the index of a byte in the header given a string.
     */
    public static int getNetworkPacketByteFromName(String b) {
        switch (b) {
            case "LEN":
                return 1;
            case "NET":
                return 0;
            case "SRC":
                return 4;
            case "DST":
                return 2;
            case "TYP":
                return 6;
            case "TTL":
                return 7;
            case "NXH":
                return 8;
            default:
                return Integer.parseInt(b);
        }
    }
    
    /**
     * Check if a byte array is an SDN-WISE packet.
     * @param data a byte array
     * @return a boolean depending if is an SDN-WISE packet or not
     */
    public static boolean isSdnWise(byte[] data) {
        return (Byte.toUnsignedInt(data[NET_INDEX]) < 63);
    }
    
    /**
     * Returns a string representation of a byte of the header.
     * @param b an integer representing the index of a byte in the header
     * @return a string representation of a byte of the header
     */  
    public static String getNetworkPacketByteName(int b) {
        switch (b) {
            case (0):
                return "NET";
            case (1):
                return "LEN";
            case (2):
                return "DST";
            case (4):
                return "SRC";
            case (6):
                return "TYPE";
            case (7):
                return "TTL";
            case (8):
                return "NXH";
            default:
                return String.valueOf(b);
        }
    }
    


    private final byte[] data;

    /**
     * Returns a NetworkPacket given a byte array.
     *
     * @param data the data contained in the NetworkPacket
     */
    public NetworkPacket(byte[] data) {
        this.data = new byte[MAX_PACKET_LENGTH];
        setArray(data);
    }

    /**
     * Creates an empty NetworkPacket. The TTL and LEN values are set to
     * default.
     *
     * @param net
     * @param src
     * @param dst
     */
    public NetworkPacket(int net, NodeAddress src, NodeAddress dst) {
        this.data = new byte[MAX_PACKET_LENGTH];
        setNet((byte) net);
        setSrc(src);
        setDst(dst);
        setTtl(SDN_WISE_DFLT_TTL_MAX);
        setLen(SDN_WISE_DFLT_HDR_LEN);
    }

    /**
     * Returns a NetworkPacket given a int array. Integer values will be
     * truncated to byte.
     *
     * @param data the data contained in the NetworkPacket
     */
    public NetworkPacket(int[] data) {
        this.data = new byte[MAX_PACKET_LENGTH];
        setArray(fromIntArrayToByteArray(data));
    }

    public NetworkPacket(DataInputStream dis) {
        this.data = new byte[MAX_PACKET_LENGTH];
        byte[] tmpData = new byte[MAX_PACKET_LENGTH];
        try {
            int net = Byte.toUnsignedInt(dis.readByte());
            int len = Byte.toUnsignedInt(dis.readByte());
            if (len > 0) {
                tmpData[NET_INDEX] = (byte) net;
                tmpData[LEN_INDEX] = (byte) len;
                dis.readFully(tmpData, LEN_INDEX + 1, len - 2);

            }
        } catch (IOException ex) {
            Logger.getLogger(NetworkPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        setArray(tmpData);
    }

    public final void setArray(int[] array) {
        setArray(fromIntArrayToByteArray(array));
    }

    public final void setArray(byte[] array) {
        if (isSdnWise(array)) {
            if (array.length <= MAX_PACKET_LENGTH && array.length
                    >= SDN_WISE_DFLT_HDR_LEN) {

                this.setLen(array[LEN_INDEX]);
                this.setNet(array[NET_INDEX]);
                this.setSrc(array[SRC_INDEX], array[SRC_INDEX + 1]);
                this.setDst(array[DST_INDEX], array[DST_INDEX + 1]);
                this.setTyp(array[TYP_INDEX]);
                this.setTtl(array[TTL_INDEX]);
                this.setNxh(array[NXH_INDEX], array[NXH_INDEX + 1]);
                this.setPayload(Arrays.copyOfRange(array, SDN_WISE_DFLT_HDR_LEN,
                        this.getLen()));
            } else {
                throw new IllegalArgumentException("Invalid array size: " + array.length);
            }
        } else {
            System.arraycopy(array, 0, data, 0, array.length);
        }
    }

    /**
     * Returns the length of the message.
     *
     * @return an integer representing the length of the message
     */
    public final int getLen() {
        if (this.isSdnWise()) {
            return Byte.toUnsignedInt(data[LEN_INDEX]);
        } else {
            return data.length;
        }
    }

    /**
     * Sets the length of the message.
     *
     * @param value an integer representing the length of the message.
     * @return the packet itself.
     */
    public final NetworkPacket setLen(byte value) {
        int v = Byte.toUnsignedInt(value);
        if (v <= MAX_PACKET_LENGTH && v > 0) {
            data[LEN_INDEX] = value;
        } else {
            throw new IllegalArgumentException("Invalid length: " + v);
        }
        return this;
    }

    /**
     * Returns the NetworkId of the message.
     *
     * @return an integer representing the NetworkId of the message
     */
    public final int getNet() {
        return Byte.toUnsignedInt(data[NET_INDEX]);
    }

    /**
     * Sets the NetworkId of the message.
     *
     * @param value the networkId of the packet.
     * @return the packet itself.
     */
    public final NetworkPacket setNet(byte value) {
        data[NET_INDEX] = value;
        return this;
    }

    /**
     * Returns the address of the source node.
     *
     * @return the NodeAddress of the source node
     */
    public final NodeAddress getSrc() {
        return new NodeAddress(data[SRC_INDEX], data[SRC_INDEX + 1]);
    }

    /**
     * Sets the address of the source node.
     *
     * @param valueH the high byte of the address.
     * @param valueL the low byte of the address.
     * @return the packet itself.
     */
    public final NetworkPacket setSrc(byte valueH, byte valueL) {
        data[SRC_INDEX] = valueH;
        data[SRC_INDEX + 1] = valueL;
        return this;
    }

    /**
     * Sets the address of the source node.
     *
     * @param address the NodeAddress of the source node.
     * @return the packet itself.
     */
    public final NetworkPacket setSrc(NodeAddress address) {
        setSrc(address.getHigh(), address.getLow());
        return this;
    }

    /**
     * Returns the address of the destination node.
     *
     * @return the NodeAddress of the destination node
     */
    public final NodeAddress getDst() {
        return new NodeAddress(data[DST_INDEX], data[DST_INDEX + 1]);
    }

    /**
     * Set the address of the destination node.
     *
     * @param valueH high value of the address of the destination.
     * @param valueL low value of the address of the destination.
     * @return the packet itself.
     */
    public final NetworkPacket setDst(byte valueH, byte valueL) {
        data[DST_INDEX] = valueH;
        data[DST_INDEX + 1] = valueL;
        return this;
    }

    /**
     * Set the address of the destination node.
     *
     * @param address the NodeAddress value of the destination.
     * @return the packet itself.
     */
    public final NetworkPacket setDst(NodeAddress address) {
        setDst(address.getHigh(), address.getLow());
        return this;
    }

    /**
     * Returns the type of the message.
     *
     * @return an integer representing the type of the message
     */
    public final int getTyp() {
        return data[TYP_INDEX];
    }

    /**
     * Sets the type of the message.
     *
     * @param value an integer representing the type of the message
     * @return
     */
    public final NetworkPacket setTyp(byte value) {
        data[TYP_INDEX] = value;
        return this;
    }

    /**
     * Returns the Time To Live of the message. When the TTL of a packet reaches
     * 0 the receiving node will drop the packet.
     *
     * @return an integer representing the Time To Live of the message
     */
    public final int getTtl() {
        return Byte.toUnsignedInt(data[TTL_INDEX]);
    }

    /**
     * Sets the Time To Live of the message. When the TTL of a packet reaches 0
     * the receiving node will drop the packet.
     *
     * @param value an integer representing the Time To Live of the message.
     * @return the packet itself.
     */
    public final NetworkPacket setTtl(byte value) {
        data[TTL_INDEX] = value;
        return this;
    }

    /**
     * Decrements the Time To Live of the message by 1. When the TTL of a packet
     * reaches 0 the receiving node will drop the packet.
     *
     * @return the packet itself.
     */
    public final NetworkPacket decrementTtl() {
        if (data[TTL_INDEX] > 0) {
            data[TTL_INDEX]--;
        }
        return this;
    }

    /**
     * Returns the NodeAddress of the next hop towards the destination.
     *
     * @return the NodeAddress of the the next hop towards the destination node
     */
    public final NodeAddress getNxh() {
        return new NodeAddress(data[NXH_INDEX], data[NXH_INDEX + 1]);
    }

    /**
     * Sets the NodeAddress of the next hop towards the destination.
     *
     * @param valueH high value of the address of the next hop.
     * @param valueL low value of the address of the next hop.
     * @return packet itself.
     */
    public final NetworkPacket setNxh(byte valueH, byte valueL) {
        data[NXH_INDEX] = valueH;
        data[NXH_INDEX + 1] = valueL;
        return this;
    }

    /**
     * Sets the NodeAddress of the next hop towards the destination.
     *
     * @param address the NodeAddress address of the next hop.
     * @return packet itself.
     */
    public final NetworkPacket setNxh(NodeAddress address) {
        NetworkPacket.this.setNxh(address.getHigh(), address.getLow());
        return this;
    }

    /**
     * Sets the NodeAddress of the next hop towards the destination.
     *
     * @param address a string representing the address of the next hop.
     * @return packet itself.
     */
    public final NetworkPacket setNxh(String address) {
        NetworkPacket.this.setNxh(new NodeAddress(address));
        return this;
    }
    /**
     * Gets the payload size of the packet.
     *
     * @return the packet payload size.
     */
    public final int getPayloadSize() {
        return (this.getLen() - SDN_WISE_DFLT_HDR_LEN);
    }
    /**
     * Returns a String representation of the NetworkPacket.
     *
     * @return a String representation of the NetworkPacket
     */
    @Override
    public String toString() {
        return Arrays.toString(this.toIntArray());
    }
    /**
     * Returns a byte array representation of the NetworkPacket.
     *
     * @return a byte array representation of the NetworkPacket
     */
    public final byte[] toByteArray() {
        return Arrays.copyOf(data, this.getLen());
    }
    /**
     * Returns an int array representation of the NetworkPacket.
     *
     * @return a int array representation of the NetworkPacket
     */
    public final int[] toIntArray() {
        int[] tmp = new int[this.getLen()];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = Byte.toUnsignedInt(data[i]);
        }
        return tmp;
    }
    @Override
    public NetworkPacket clone() throws CloneNotSupportedException {
        super.clone();
        return new NetworkPacket(data.clone());
    }
    public String getTypeToString() {
        switch (getTyp()) {
            case DATA:
                return "DATA";
            case BEACON:
                return "BEACON";
            case REPORT:
                return "REPORT";
            case REQUEST:
                return "REQUEST";
            case RESPONSE:
                return "RESPONSE";
            case OPEN_PATH:
                return "OPEN_PATH";
            case CONFIG:
                return "CONFIG";
            case REG_PROXY:
                return "REG_PROXY";
            default:
                return String.valueOf(getTyp());
        }
    }
    public boolean isSdnWise() {
        return (Byte.toUnsignedInt(data[NET_INDEX]) < 63);
    }
    private byte[] fromIntArrayToByteArray(int[] array) {
        byte[] dataToByte = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            dataToByte[i] = (byte) array[i];
        }
        return dataToByte;
    }

    /**
     * Returns the payload of the packet as a byte array.
     *
     * @return the payload of the packet
     */
    protected byte[] getPayload() {
        return Arrays.copyOfRange(data, SDN_WISE_DFLT_HDR_LEN,
                this.getLen());
    }

    /**
     * Sets the payload of the packet from a byte array.
     *
     * @param payload the payload of the packet.
     * @return the payload of the packet.
     */
    protected NetworkPacket setPayload(byte[] payload) {
        if (payload.length + SDN_WISE_DFLT_HDR_LEN <= MAX_PACKET_LENGTH) {
            System.arraycopy(payload, 0, data, SDN_WISE_DFLT_HDR_LEN, payload.length);
            this.setLen((byte) (payload.length + SDN_WISE_DFLT_HDR_LEN));
        } else {
            throw new IllegalArgumentException("Payload exceeds packet size");
        }
        return this;
    }

    /**
     * Sets the payload size of the packet.
     *
     * @param size the payload size.
     * @return the packet itself.
     */
    protected final NetworkPacket setPayloadSize(int size) {
        if (SDN_WISE_DFLT_HDR_LEN + size <= MAX_PACKET_LENGTH) {
            this.setLen((byte) (SDN_WISE_DFLT_HDR_LEN + size));
        } else {
            throw new IllegalArgumentException("Index cannot be greater than "
                    + "the maximum payload size: " + size);
        }
        return this;
    }


    /**
     * Sets a single payload byte.
     *
     * @param index the index of the payload. The first byte of the payload is
     * 0.
     * @param newData the new data to be set.
     * @return the packet itself.
     */
    protected final NetworkPacket setPayloadAt(byte newData, int index) {
        if (SDN_WISE_DFLT_HDR_LEN + index < MAX_PACKET_LENGTH) {
            data[SDN_WISE_DFLT_HDR_LEN + index] = newData;
            if ((index + SDN_WISE_DFLT_HDR_LEN) >= this.getLen()) {
                this.setLen((byte) (SDN_WISE_DFLT_HDR_LEN + index + 1));
            }
        } else {
            throw new IllegalArgumentException("Index cannot be greater than "
                    + "the maximum payload size");
        }
        return this;
    }

    /**
     * Sets a part of the payload of the NetworkPacket. Differently from
     * copyPayload this method updates also the length of the packet
     *
     * @param src the new data to be set.
     * @param srcPos starting from this byte of src.
     * @param payloadPos copying to this byte of payload.
     * @param length this many bytes.
     * @return the packet itself.
     */
    protected final NetworkPacket setPayload(byte[] src, int srcPos,
            int payloadPos, int length) {

        if (srcPos < 0 || payloadPos < 0 || length < 0) {
            throw new IllegalArgumentException("Negative index");
        } else {
            this.copyPayload(src, srcPos, payloadPos, length);
            this.setPayloadSize(length + payloadPos);
        }
        return this;
    }

    /**
     * Copy a part of the payload of the NetworkPacket. Differently from
     * copyPayload this method does not update the length of the packet
     *
     * @param src the new data to be set.
     * @param srcPos starting from this byte of src.
     * @param payloadPos copying to this byte of payload.
     * @param length this many bytes.
     * @return the packet itself.
     */
    protected final NetworkPacket copyPayload(byte[] src, int srcPos,
            int payloadPos, int length) {
        for (int i = 0; i < length; i++) {
            setPayloadAt(src[i + srcPos], i + payloadPos);
        }
        return this;
    }

    /**
     * Gets a byte from the payload of the packet at position index.
     *
     * @param index the offset of the byte.
     * @return the byte of the payload.
     */
    protected final byte getPayloadAt(int index) {
        if (index + SDN_WISE_DFLT_HDR_LEN < this.getLen()) {
            return data[SDN_WISE_DFLT_HDR_LEN + index];
        } else {
            throw new IllegalArgumentException("Index cannot be greater than "
                    + "the maximum payload size");
        }
    }

    /**
     * Gets a byte array from the payload of the packet from position start to
     * position end.
     *
     * @param start starting index inclusive.
     * @param stop stop index exclusive.
     * @return the byte of the payload.
     */
    protected final byte[] getPayloadFromTo(int start, int stop) {
        if (start >= stop) {
            throw new IllegalArgumentException("Start must be less than stop");
        }
        if (stop <= 0) {
            throw new IllegalArgumentException("Stop must be greater than 0");
        }
        if (start + SDN_WISE_DFLT_HDR_LEN > this.getLen()) {
            throw new IllegalArgumentException("Start is greater than packet size");
        }
        stop = Math.min(stop + SDN_WISE_DFLT_HDR_LEN, this.getLen());
        return Arrays.copyOfRange(data, start + SDN_WISE_DFLT_HDR_LEN, stop);
    }

    /**
     * Gets a part of the payload of the packet from position start, to position
     * end.
     *
     * @param start start the copy from this byte.
     * @param end to this byte.
     * @return a byte[] part of the payload.
     */
    protected final byte[] copyPayloadOfRange(int start, int end) {
        return Arrays.copyOfRange(data, SDN_WISE_DFLT_HDR_LEN + start,
                SDN_WISE_DFLT_HDR_LEN + end);
    }

}
