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

import static com.github.sdnwiselab.sdnwise.packet.ConfigPacket.ConfigProperty.*;
import com.github.sdnwiselab.sdnwise.util.NodeAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.*;

/**
 *
 * @author Sebastiano Milardo
 */
public class ConfigSecurityPacket extends ConfigPacket {

    public ConfigSecurityPacket(NetworkPacket data) {
        super(data);
    }

    public ConfigSecurityPacket(int netId, NodeAddress src, NodeAddress dst) {
        super(netId, src, dst);
    }

    /**
     * Request to the belong Controller to change Sink Node and Controller.
     *
     * @param ip
     * @return
     */
    public ConfigSecurityPacket requestPermissionToChange(InetAddress ip) {
        this.setRead();
        this.setConfigId(SEC_CHANGE);
        this.setIpController(ip);
        return this;
    }

    /**
     * Request to the new Sink/Controller the Controller's IP Address.
     *
     * @param newSink
     */
    public void requestIPController(NodeAddress newSink) {
        this.setRead();
        this.setConfigId(SEC_IP);
        this.setDst(newSink);
    }

    /**
     * Receive from the new Sink/Controller the Controller's IP Address.
     *
     * @return
     */
    public InetAddress getIpController() {
        try {
            return InetAddress.getByAddress(this.getPayloadFromTo(1, 5));
        } catch (UnknownHostException ex) {
            Logger.getLogger(ConfigSecurityPacket.class.getName()).log(Level.SEVERE, null, ex);
        }
        return InetAddress.getLoopbackAddress();
    }

    /**
     * Request to the new Sink/Controller the Controller's IP Address.
     *
     * @param addr
     * @return
     */
    public ConfigSecurityPacket setIpController(InetAddress addr) {
        byte[] ip = addr.getAddress();
        this.setPayloadAt(ip[0], 1);
        this.setPayloadAt(ip[1], 2);
        this.setPayloadAt(ip[2], 3);
        this.setPayloadAt(ip[3], 4);
        return this;
    }

    /**
     * Receive Response about change Sink/Controller.
     *
     * @param packet
     */
    public void setResponse(int packet[]) {

        String response = Arrays.toString(packet);
        if (response.equals("OK")) { // Token

        } else { //something went wrong

        }
    }

    /**
     * Receive the token from Controller and complete secuirty process.
     *
     * @param token
     * @return
     */
    public ConfigSecurityPacket setToken(byte token[]) {
        this.setPayload(token, (byte) 0, (byte) 1, token.length);
        return this;
    }

    /**
     * Method to ensure that the security process is completed. Send a Token to
     * the New Controller in the network.
     *
     * @return
     */
    public byte[] getToken() {
        return this.getPayloadFromTo(1, this.getPayloadSize());
    }
}
