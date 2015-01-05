/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.event;

import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;

/**
 * Zigbee Command Class event. This event is fired when a command class
 * receives a value from the node. The event can be subclasses to add
 * additional information to the event. 
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZigbeeCommandClassValueEvent extends ZigbeeEvent {

	private final CommandClass commandClass;
	private final Object value;
	
	/**
	 * Constructor. Creates a new instance of the ZigbeeCommandClassValueEvent class.
	 * @param nodeId the nodeId of the event
	 * @param endpoint the endpoint of the event.
	 * @param commandClass the command class that fired the ZigbeeCommandClassValueEvent;
	 * @param value the value for the event.
	 */
	public ZigbeeCommandClassValueEvent(long nodeId, int endpoint, CommandClass commandClass, Object value) {
		super(nodeId, endpoint);
		
		this.commandClass = commandClass;
		this.value = value;
	}

	/**
	 * Gets the command class that fired the ZigbeeCommandClassValueEvent;
	 * @return the command class.
	 */
	public CommandClass getCommandClass() {
		return commandClass;
	}

	/**
	 * Gets the value for the event.
	 * @return the value.
	 */
	public Object getValue() {
		return value;
	}
}
