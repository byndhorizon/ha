/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.converter;

import java.util.Map;

import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeCommandClassValueEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

/**
 * ZigbeeCommandClassConverter class. Base class for all converters
 * that convert between Zigbee command classes and openHAB
 * items.
 * @author Jan-Willem Spuij
 * @since 1.4.0
 * @param <COMMAND_CLASS_TYPE> the {@link ZigbeeCommandClass} that this converter converts for.
 */
public abstract class ZigbeeCommandClassConverter<COMMAND_CLASS_TYPE extends ZigbeeCommandClass>
	extends ZigbeeConverterBase {
	
	/**
	 * Constructor. Creates a new instance of the {@link ZigbeeCommandClassConverter}
	 * class.
	 * @param controller the {@link ZigbeeController} to use to send messages.
	 * @param eventPublisher the {@link EventPublisher} that can be used to send updates.
	 */
	public ZigbeeCommandClassConverter(ZigbeeController controller, EventPublisher eventPublisher) {
		super(controller, eventPublisher);
	}

	/**
	 * Execute refresh method. This method is called every time a binding item is
	 * refreshed and the corresponding node should be sent a message.
	 * @param node the {@link ZigbeeNode} that is bound to the item.
	 * @param commandClass the {@link ZigbeeCommandClass} that will be used to send a polling message.
	 * @param endpointId the endpoint id to send the message.
	 */
	abstract void executeRefresh(ZigbeeNode node, COMMAND_CLASS_TYPE commandClass, int endpointId, Map<String,String> arguments);

	/**
	 * Handles an incoming {@link ZigbeeCommandClassValueEvent}. Implement
	 * this message in derived classes to convert the value and post an
	 * update on the openHAB bus.
	 * @param event the received {@link ZigbeeCommandClassValueEvent}.
	 * @param item the {@link Item} that should receive the event.
	 */
	abstract void handleEvent(ZigbeeCommandClassValueEvent event, Item item, Map<String,String> arguments);
	
	/**
	 * Receives a command from openHAB and translates it to an operation
	 * on the Zigbee network.
	 * @param command the received command
	 * @param node the {@link ZigbeeNode} to send the command to
	 * @param commandClass the {@link ZigbeeCommandClass} to send the command to.
	 * @param endpointId the endpoint ID to send the command to.
	 */
	abstract void receiveCommand(Item item, Command command, ZigbeeNode node, COMMAND_CLASS_TYPE commandClass, int endpointId, Map<String,String> arguments);
	
}
