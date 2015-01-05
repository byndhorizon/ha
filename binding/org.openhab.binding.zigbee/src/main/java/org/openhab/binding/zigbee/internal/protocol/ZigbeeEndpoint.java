/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeDeviceClass.Basic;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeDeviceClass.Generic;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeDeviceClass.Specific;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * ZigbeeEndpoint class. Represents an endpoint in case of a Multi-channel node.
 * 
 * @author Jan-Willem Spuij
 * @since 1.3.0
 */
@XStreamAlias("endPoint")
public class ZigbeeEndpoint {

	private final ZigbeeDeviceClass deviceClass;
	private final int endpointId;

	private Map<CommandClass, ZigbeeCommandClass> supportedCommandClasses = new HashMap<CommandClass, ZigbeeCommandClass>();

	/**
	 * Constructor. Creates a new instance of the ZigbeeEndpoint class.
	 * @param node the parent node of this endpoint.
	 * @param endpointId the endpoint ID.
	 */
	public ZigbeeEndpoint(int endpointId) {
		this.endpointId = endpointId;
		this.deviceClass = new ZigbeeDeviceClass(Basic.NOT_KNOWN,
				Generic.NOT_KNOWN, Specific.NOT_USED);
	}

	/**
	 * Gets the endpoint ID
	 * @return endpointId the endpointId
	 */
	public int getEndpointId() {
		return endpointId;
	}

	/**
	 * Gets the Command classes this endpoint implements.
	 * @return the command classes.
	 */
	public Collection<ZigbeeCommandClass> getCommandClasses() {
		return supportedCommandClasses.values();
	}

	/**
	 * Gets a commandClass object this endpoint implements. Returns null if
	 * this endpoint does not support this command class.
	 * 
	 * @param commandClass
	 *            The command class to get.
	 * @return the command class.
	 */
	public ZigbeeCommandClass getCommandClass(CommandClass commandClass) {
		return supportedCommandClasses.get(commandClass);
	}

	/**
	 * Adds a command class to the list of supported command classes by this
	 * endpoint. Does nothing if command class is already added.
	 * @param commandClass the command class instance to add.
	 */
	public void addCommandClass(ZigbeeCommandClass commandClass) {
		CommandClass key = commandClass.getCommandClass();

		if (!supportedCommandClasses.containsKey(key))
			supportedCommandClasses.put(key, commandClass);
	}

	/**
	 * Returns the device class for this endpoint.
	 * @return the deviceClass
	 */
	public ZigbeeDeviceClass getDeviceClass() {
		return deviceClass;
	}
}
