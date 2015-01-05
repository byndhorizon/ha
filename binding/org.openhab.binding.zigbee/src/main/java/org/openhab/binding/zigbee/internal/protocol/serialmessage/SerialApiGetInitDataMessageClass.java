/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.serialmessage;

import java.util.ArrayList;

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
public class SerialApiGetInitDataMessageClass extends ZigbeeCommandProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SerialApiGetInitDataMessageClass.class);

	private ArrayList<Integer>zigbeeNodes = new ArrayList<Integer>();

	private static final int NODE_BYTES = 29; // 29 bytes = 232 bits, one for each supported node by Zigbee;
	
	public SerialMessage doRequest() {
		return new SerialMessage(SerialMessageClass.SerialApiGetInitData, SerialMessageType.Response, SerialMessageClass.SerialApiGetInitData, SerialMessagePriority.High);
	}
	
	@Override
	public boolean handleResponse(ZigbeeController zController, SerialMessage lastSentMessage, SerialMessage incomingMessage) {
		logger.debug(String.format("Got MessageSerialApiGetInitData response."));
		int nodeBytes = incomingMessage.getMessagePayloadByte(2);
		
		if (nodeBytes != NODE_BYTES) {
			logger.error("Invalid number of node bytes = {}", nodeBytes);
			return false;
		}

		int nodeId = 1;
		
		// loop bytes
		for (int i = 3;i < 3 + nodeBytes;i++) {
			int incomingByte = incomingMessage.getMessagePayloadByte(i);
			// loop bits in byte
			for (int j=0;j<8;j++) {
				int b1 = incomingByte & (int)Math.pow(2.0D, j);
				int b2 = (int)Math.pow(2.0D, j);
				if (b1 == b2) {
					logger.info("NODE {}: Node found", nodeId);

					zigbeeNodes.add(nodeId);
				}
				nodeId++;
			}
		}
		
		logger.info("Zigbee Controller using {} API", ((incomingMessage.getMessagePayloadByte(1) & 0x01) == 1) ? "Slave" : "Controller");
		logger.info("Zigbee Controller is {} Controller", ((incomingMessage.getMessagePayloadByte(1) & 0x04) == 1) ? "Secondary" : "Primary");
		logger.info("------------Number of Nodes Found Registered to Zigbee Controller------------");
		logger.info(String.format("# Nodes = %d", zigbeeNodes.size()));
		logger.info("----------------------------------------------------------------------------");

		checkTransactionComplete(lastSentMessage, incomingMessage);

		return true;
	}

	public ArrayList<Integer> getNodes() {
		return zigbeeNodes;
	}
}
