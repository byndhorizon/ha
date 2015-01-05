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

import org.openhab.binding.zigbee.internal.converter.command.BinaryOnOffCommandConverter;
import org.openhab.binding.zigbee.internal.converter.command.ZigbeeCommandConverter;
import org.openhab.binding.zigbee.internal.converter.state.BinaryDecimalTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.BinaryPercentTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.IntegerOnOffTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.IntegerOpenClosedTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.IntegerUpDownTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.ZigbeeStateConverter;
import org.openhab.binding.zigbee.internal.protocol.SocketMessage;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeBinarySwitchCommandClass;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeCommandClassValueEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * ZigbeeBinarySwitchConverter class. Converter for communication with the 
 * {@link ZigbeeBatteryCommandClass}. Implements polling of the battery
 * status and receiving of battery events.
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZigbeeBinarySwitchConverter extends ZigbeeCommandClassConverter<ZigbeeBinarySwitchCommandClass> {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeBinarySwitchConverter.class);
	private static final int REFRESH_INTERVAL = 0; // refresh interval in seconds for the binary switch;

	/**
	 * Constructor. Creates a new instance of the {@link ZigbeeBinarySwitchConverter} class.
	 * @param controller the {@link ZigbeeController} to use for sending messages.
	 * @param eventPublisher the {@link EventPublisher} to use to publish events.
	 */
	public ZigbeeBinarySwitchConverter(ZigbeeController controller, EventPublisher eventPublisher) {
		super(controller, eventPublisher);
		
		// State and commmand converters used by this converter. 
		this.addStateConverter(new BinaryDecimalTypeConverter());
		this.addStateConverter(new BinaryPercentTypeConverter());
		this.addStateConverter(new IntegerOnOffTypeConverter());
		this.addStateConverter(new IntegerOpenClosedTypeConverter());
		this.addStateConverter(new IntegerUpDownTypeConverter());
		
		this.addCommandConverter(new BinaryOnOffCommandConverter());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeRefresh(ZigbeeNode node, 
			ZigbeeBinarySwitchCommandClass commandClass, int endpointId, Map<String,String> arguments) {
		logger.debug("Generating poll message for {} for node {} endpoint {}", commandClass.getCommandClass().getLabel(), node.getNodeId(), endpointId);
		SocketMessage serialMessage = node.encapsulate(commandClass.getValueMessage(), commandClass, endpointId);
		
		if (serialMessage == null) {
			logger.warn("Generating message failed for command class = {}, node = {}, endpoint = {}", commandClass.getCommandClass().getLabel(), node.getNodeId(), endpointId);
			return;
		}
		
		this.getController().sendData(serialMessage);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleEvent(ZigbeeCommandClassValueEvent event, Item item, Map<String,String> arguments) {
		ZigbeeStateConverter<?,?> converter = this.getStateConverter(item, event.getValue());
		
		if (converter == null) {
			logger.warn("No converter found for item = {}, node = {} endpoint = {}, ignoring event.", item.getName(), event.getNodeId(), event.getEndpoint());
			return;
		}
		
		State state = converter.convertFromValueToState(event.getValue());
		this.getEventPublisher().postUpdate(item.getName(), state);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void receiveCommand(Item item, Command command, ZigbeeNode node,
			ZigbeeBinarySwitchCommandClass commandClass, int endpointId, Map<String,String> arguments) {
		ZigbeeCommandConverter<?,?> converter = this.getCommandConverter(command.getClass());
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE: ZigbeeBinarySwitchConverter.receiveCommand"+converter.getClass());
		if (converter == null) {
			logger.warn("No converter found for item = {}, node = {} endpoint = {}, ignoring command.", item.getName(), node.getNodeId(), endpointId);
			return;
		}
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE: ZigbeeBinarySwitchConverter.receiveCommand"+converter.getClass());

		SocketMessage serialMessage = node.encapsulate(commandClass.setValueMessage((Integer)converter.convertFromCommandToValue(item, command)), commandClass, endpointId);
		
		if (serialMessage == null) {
			logger.warn("Generating message failed for command class = {}, node = {}, endpoint = {}", commandClass.getCommandClass().getLabel(), node.getNodeId(), endpointId);
			return;
		}
		
		this.getController().sendData(serialMessage);
		
		if (command instanceof State)
			this.getEventPublisher().postUpdate(item.getName(), (State)command);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	int getRefreshInterval() {
		return REFRESH_INTERVAL;
	}
}
