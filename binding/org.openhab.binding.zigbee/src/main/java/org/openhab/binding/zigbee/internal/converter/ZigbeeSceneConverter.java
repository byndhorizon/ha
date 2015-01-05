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

import org.openhab.binding.zigbee.internal.converter.state.BinaryDecimalTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.IntegerOnOffTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.IntegerOpenClosedTypeConverter;
import org.openhab.binding.zigbee.internal.converter.state.ZigbeeStateConverter;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeBasicCommandClass;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeCommandClassValueEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZigbeeSceneConverter class. Converters between binding items
 * and the Zigbee API for scene controllers.
 * @author Chris Jackson
 * @since 1.4.0
 */
public class ZigbeeSceneConverter extends ZigbeeCommandClassConverter<ZigbeeBasicCommandClass> {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeSceneConverter.class);

	/**
	 * Constructor. Creates a new instance of the {@link ZigbeeConverterBase}
	 * class.
	 * @param controller the {@link ZigbeeController} to use to send messages.
	 * @param eventPublisher the {@link EventPublisher} that can be used to send updates.
	 */
	public ZigbeeSceneConverter(ZigbeeController controller, EventPublisher eventPublisher) {
		super(controller, eventPublisher);
        
		// State converters used by this converter. 
		this.addStateConverter(new BinaryDecimalTypeConverter());
		this.addStateConverter(new IntegerOnOffTypeConverter());
		this.addStateConverter(new IntegerOpenClosedTypeConverter());
    }

	/**
	 * {@inheritDoc}
	 */
	@Override
	int getRefreshInterval() {
		return 0;
	}

	@Override
	void executeRefresh(ZigbeeNode node, ZigbeeBasicCommandClass commandClass, int endpointId,
			Map<String, String> arguments) {
	}

	@Override
	void handleEvent(ZigbeeCommandClassValueEvent event, Item item, Map<String, String> arguments) {
		if(arguments.get("scene")==null)
			return;
		
		int scene = Integer.parseInt(arguments.get("scene"));
		if(scene != (Integer)event.getValue())
			return;
		Integer state = Integer.parseInt(arguments.get("state"));
		ZigbeeStateConverter<?,?> converter = this.getStateConverter(item, state);
 
		if (converter == null) {
			logger.warn("No converter found for item = {}, node = {} endpoint = {}, ignoring event.", item.getName(), event.getNodeId(), event.getEndpoint());
			return;
		}
		
		State itemState = converter.convertFromValueToState(event.getValue());
		this.getEventPublisher().postUpdate(item.getName(), itemState);
	}

	@Override
	void receiveCommand(Item item, Command command, ZigbeeNode node, ZigbeeBasicCommandClass commandClass,
			int endpointId, Map<String, String> arguments) {
	}
}
