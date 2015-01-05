/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.binding.zigbee.internal.converter.command.ZigbeeCommandConverter;
import org.openhab.binding.zigbee.internal.converter.state.StateComparator;
import org.openhab.binding.zigbee.internal.converter.state.ZigbeeStateConverter;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * ZigbeeConverterBase class. Base class for all converters
 * that convert between the Zigbee API and openHAB
 * items.
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public abstract class ZigbeeConverterBase {

	private final ZigbeeController controller;
	private final EventPublisher eventPublisher;
	private final Map<Class<? extends State>,ZigbeeStateConverter<?,?>> stateConverters = new HashMap<Class<? extends State>, ZigbeeStateConverter<?,?>>();
	private final Map<Class<? extends Command>, ZigbeeCommandConverter<?,?>> commandConverters = new HashMap<Class<? extends Command>, ZigbeeCommandConverter<?,?>>();
	
	/**
	 * Constructor. Creates a new instance of the {@link ZigbeeConverterBase}
	 * class.
	 * @param controller the {@link ZigbeeController} to use to send messages.
	 * @param eventPublisher the {@link EventPublisher} that can be used to send updates.
	 */
	public ZigbeeConverterBase(ZigbeeController controller, EventPublisher eventPublisher) {
		this.controller = controller;
		this.eventPublisher = eventPublisher;
	}

	/**
	 * Returns the {@link EventPublisher} that can be used
	 * inside the converter to publish event updates.
	 * @return the eventPublisher
	 */
	protected EventPublisher getEventPublisher() {
		return this.eventPublisher;
	}

	/**
	 * Returns the {@link ZigbeeController} that is used to send messages.
	 * @return the controller to use to send messages.
	 */
	protected ZigbeeController getController() {
		return this.controller;
	}

	/**
	 * Add a {@link ZigbeeStateConverter} to the map of converters for fast lookup.
	 * @param converter the {@link ZigbeeStateConverter} to add.
	 */
	protected void addStateConverter(ZigbeeStateConverter<?,?> converter) {
		this.stateConverters.put(converter.getState(), converter);
	}
	
	/**
	 * Add a {@link ZigbeeStateConverter} to the map of converters for fast lookup.
	 * @param converter the {@link ZigbeeStateConverter} to add.
	 */
	protected void addCommandConverter(ZigbeeCommandConverter<?,?> converter) {
		this.commandConverters.put(converter.getCommand(), converter);
	}
	
	/**
	 * Returns the default Refresh interval for the converter to use.
	 * 0 (zero) indicates that the converter does not refresh by default or does not support it.
	 * @return the refresh interval for this converter.
	 */
	abstract int getRefreshInterval();
	
	/**
	 * Gets a {@link ZigbeeStateConverter} that is suitable for this {@link CommandClass} and the data types supported by
	 * the {@link Item}
	 * @param commandClass the {@link CommandClass} that sent the value.
	 * @param item the {@link Item} that has to receive the State;
	 * @return a converter object that converts between the value and the state;
	 */
	protected ZigbeeStateConverter<?,?> getStateConverter(Item item, Object value) {
		if(item == null)
			return null;

		List<Class<? extends State>> list = new ArrayList<Class<? extends State>>(item.getAcceptedDataTypes());
		Collections.sort(list, new StateComparator());

		for (Class<? extends State> stateClass : list) {
			ZigbeeStateConverter<?,?> result = stateConverters.get(stateClass);

			if (result == null || !result.getType().isInstance(value))
				continue;
			
			return result;
		}
		
		return null;
	}

	/**
	 * Gets a {@link ZigbeeCommandConverter} that is suitable for this {@link CommandClass}
	 * @param commandClass the {@link CommandClass} that sent the value.
	 * @return a converter object that converts between the value and the state;
	 */
	protected ZigbeeCommandConverter<?,?> getCommandConverter(Class<? extends Command> commandClass) {
		return this.commandConverters.get(commandClass);
	}	
}
