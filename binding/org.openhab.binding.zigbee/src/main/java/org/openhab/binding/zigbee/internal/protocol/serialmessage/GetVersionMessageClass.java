/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.serialmessage;

import org.apache.commons.lang.ArrayUtils;
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
public class GetVersionMessageClass extends ZigbeeCommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(GetVersionMessageClass.class);
	
	private String ZigbeeVersion = "Unknown";
	private int ZigbeeLibraryType = 0;
	
	public SerialMessage doRequest() {
		return new SerialMessage(SerialMessageClass.GetVersion, SerialMessageType.Request, SerialMessageClass.GetVersion, SerialMessagePriority.High);
	}
	
	@Override
	public boolean handleResponse(ZigbeeController zController, SerialMessage lastSentMessage, SerialMessage incomingMessage) {
		ZigbeeLibraryType = incomingMessage.getMessagePayloadByte(12);
		ZigbeeVersion = new String(ArrayUtils.subarray(incomingMessage.getMessagePayload(), 0, 11));
		logger.debug(String.format("Got MessageGetVersion response. Version = %s, Library Type = 0x%02X", ZigbeeVersion, ZigbeeLibraryType));

		checkTransactionComplete(lastSentMessage, incomingMessage);
		
		return true;
	}

	public String getVersion() {
		return ZigbeeVersion;
	}

	public int getLibraryType() {
		return ZigbeeLibraryType;
	}
}
