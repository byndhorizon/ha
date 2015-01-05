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

import org.openhab.core.library.types.OpenClosedType;

/**
 * Converts from a Zigbee big decimal value to a {@link OpenClosedType}
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class BigDecimalOpenClosedTypeConverter extends
		ZigbeeStateConverter<BigDecimal, OpenClosedType> {
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected OpenClosedType convert(BigDecimal value) {
		return BigDecimal.ZERO.compareTo(value) != 0 ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
	}

}
