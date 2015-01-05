/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.converter.state;

import java.math.BigDecimal;

import org.openhab.core.library.types.OnOffType;

/**
 * Converts from big decimal Zigbee value to a {@link OnOffType}
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class BigDecimalOnOffTypeConverter extends
		ZigbeeStateConverter<BigDecimal, OnOffType> {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected OnOffType convert(BigDecimal value) {
		return BigDecimal.ZERO.compareTo(value) != 0 ? OnOffType.ON : OnOffType.OFF;
	}

}
