/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.converter.command;

import java.math.BigDecimal;

import org.openhab.core.items.Item;
import org.openhab.core.library.types.DecimalType;

/**
 * Converts from {@link DecimalType} command to a Zigbee value.
 * @author Matthew Bowman
 * @since 1.4.0
 */
public class DecimalCommandConverter extends
		ZigbeeCommandConverter<DecimalType, BigDecimal> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BigDecimal convert(Item item, DecimalType command) {
		return command.toBigDecimal();
	}

}
