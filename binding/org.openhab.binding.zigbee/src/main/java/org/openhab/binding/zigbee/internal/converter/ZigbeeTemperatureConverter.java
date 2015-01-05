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

import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.ZigBeeDeviceException;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.ZigBeeClusterException;
import org.bubblecloud.zigbee.api.cluster.measureament_sensing.TemperatureMeasurement;
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
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
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
public class ZigbeeTemperatureConverter extends Zigbee4JavaCommandConverter  {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeTemperatureConverter.class);
	private static final int REFRESH_INTERVAL = 0; // refresh interval in seconds for the binary switch;

	/**
	 * Constructor. Creates a new instance of the {@link ZigbeeTemperatureConverter} class.
	 * @param controller the {@link ZigbeeController} to use for sending messages.
	 * @param eventPublisher the {@link EventPublisher} to use to publish events.
	 */
	public ZigbeeTemperatureConverter(ZigbeeController controller, EventPublisher eventPublisher) {
		super(controller, eventPublisher);
		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void executeRefresh(Item item, Device device,
			 Map<String,String> arguments) {
		final TemperatureMeasurement temp = device.getCluster(TemperatureMeasurement.class);
		try {
			System.out.println("temp.getMeasuredValue(), id"
					+ temp.getMeasuredValue().getId() + "name"
					+ temp.getMeasuredValue().getName() + "value"
					+ temp.getMeasuredValue().getValue());
			
			this.getEventPublisher().postUpdate(item.getName(), 
					(State)new DecimalType((int)temp.getMeasuredValue().getValue()));
		} catch (ZigBeeClusterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void receiveCommand(Item item, Command command, Device device,
			 Map<String,String> arguments) {
		

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	int getRefreshInterval() {
		return REFRESH_INTERVAL;
	}
}
