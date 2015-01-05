/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
 package org.openhab.binding.zigbee.internal.converter.command;

import java.lang.reflect.ParameterizedType;

import org.openhab.core.items.Item;
import org.openhab.core.types.Command;

/**
 * Abstract base class for converting item commands to
 * Zigbee command class commands. 
 * @author Jan-Willem Spuij
 * @since 1.4.0
 * @param <OPENHAB_TYPE> the command to convert
 * @param <ZIGBEE_TYPE> the Zigbee type to convert to.
 */
public abstract class ZigbeeCommandConverter<OPENHAB_TYPE extends Command, ZIGBEE_TYPE> {

	/**
	 * Returns the type of the openHAB {@link Command} that this {@link ZigbeeCommandConverter} converts to.
	 * @return the supported {@link Command}
	 */
	@SuppressWarnings("unchecked")
	public Class<? extends Command> getCommand() {
		return (Class<? extends Command>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	/**
	 * Converts a an OpenHab command to a Zigbee value.
	 * @param command the {@link Command} to convert.
	 * @param item the item to convert the command for.
	 * @return the Zigbee value to convert to.
	 */
	protected abstract ZIGBEE_TYPE convert(Item item, OPENHAB_TYPE command);
	
	/**
	 * Converts a an OpenHab command to a Zigbee value.
	 * @param command the {@link Command} to convert.
	 * @param item the item to convert the command for.
	 * @return the Zigbee value to convert to.
	 */
	@SuppressWarnings("unchecked")
	public Object convertFromCommandToValue(Item item, Command command) {
		
		if (command == null)
			return null;
		
		return convert(item, (OPENHAB_TYPE) command);
	}
}
