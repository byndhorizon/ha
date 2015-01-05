/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol;

import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeEvent;

/**
 * Zigbee Event Listener interface. Classes that implement this interface
 * need to be able to handle incoming ZigbeeEvent events.
 * @author Brian Crosby
 * @since 1.3.0
 */
public interface ZigbeeEventListener {

	/**
	 * Event handler method for incoming Zigbee events.
	 * @param event the incoming Zigbee event.
	 */
	void ZigbeeIncomingEvent(ZigbeeEvent event);
}
