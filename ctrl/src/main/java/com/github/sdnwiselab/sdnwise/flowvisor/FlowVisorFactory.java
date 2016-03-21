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
package com.github.sdnwiselab.sdnwise.flowvisor;

import com.github.sdnwiselab.sdnwise.adapter.AbstractAdapter;
import com.github.sdnwiselab.sdnwise.adapter.AdapterUdp;
import com.github.sdnwiselab.sdnwise.configuration.ConfigFlowVisor;
import com.github.sdnwiselab.sdnwise.configuration.Configurator;

/**
 * FlowVisorFactory creates an FlowVisor object given the specifications
 * contained in a ConfigFlowVisor object. This class implements the factory
 * object pattern.
 *
 * @author Sebastiano Milardo
 */
public class FlowVisorFactory {

    private static ConfigFlowVisor conf;

    /**
     *
     * Getter Method to obtain a FlowVisor object, created by this Factory
     * Class.
     *
     * @param config ConfigFlowVisor file for this FlowVisorFactory.
     *
     * @return FlowVisor object with a new AdapterUdp for Lower AbstractAdapter
     * and Upper AbstractAdapter
     */
    public static final FlowVisor getFlowvisor(final Configurator config) {
        conf = config.getFlowvisor();
        return new FlowVisor(getLower(), getUpper());
    }

    private static AbstractAdapter getLower() {
        String type = conf.getLower().get("TYPE");
        switch (type) {
            case "UDP":
                return new AdapterUdp(conf.getLower());
            default:
                throw new UnsupportedOperationException(
                        "Error in Configuration file");
        }
    }

    private static AdapterUdp getUpper() {
        String type = conf.getUpper().get("TYPE");
        switch (type) {
            case "UDP":
                return new AdapterUdp(conf.getUpper());
            default:
                throw new UnsupportedOperationException(
                        "Error in Configuration file");
        }
    }

    private FlowVisorFactory() {
    }
}
