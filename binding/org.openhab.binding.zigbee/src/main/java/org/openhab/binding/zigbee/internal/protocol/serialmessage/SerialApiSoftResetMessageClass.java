/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.serialmessage;

import org.openhab.binding.zigbee.internal.protocol.SerialMessage;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zigbee controller
 * @author Chris Jackson
 * @since 1.5.0
 */
public class SerialApiSoftResetMessageClass extends ZigbeeCommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SerialApiSoftResetMessageClass.class);
	
	public SerialMessage doRequest() {
		return new SerialMessage(SerialMessageClass.SerialApiSoftReset, SerialMessageType.Request, SerialMessageClass.SerialApiSoftReset, SerialMessagePriority.High);
	}
	
	@Override
	public boolean handleResponse(ZigbeeController zController, SerialMessage lastSentMessage, SerialMessage incomingMessage) {
		logger.debug(String.format("Received soft reset response"));

		checkTransactionComplete(lastSentMessage, incomingMessage);
		
		return true;
	}
}
