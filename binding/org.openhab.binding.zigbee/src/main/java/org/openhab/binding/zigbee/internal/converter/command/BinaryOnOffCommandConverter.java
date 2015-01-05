/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.converter.command;

import org.openhab.core.items.Item;
import org.openhab.core.library.types.OnOffType;

/**
 * Converts from {@link OnOffType} command to a Zigbee value.
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class BinaryOnOffCommandConverter extends
		ZigbeeCommandConverter<OnOffType, Integer> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Integer convert(Item item, OnOffType command) {
		return command == OnOffType.ON ? 0xFF : 0x00;
	}

}