/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.event;

import org.openhab.binding.zigbee.internal.protocol.SerialMessage;
import org.openhab.binding.zigbee.internal.protocol.SocketMessage;

/**
 * Zigbee Transaction Completed Event. Indicated that a transaction (a 
 * sequence of messages with an expected reply) has completed successfully.
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZigbeeTransactionCompletedEvent extends ZigbeeEvent {

	private final SocketMessage completedMessage;
	
	/**
	 * Constructor. Creates a new instance of the ZigbeeTransactionCompletedEvent
	 * class.
	 * @param lastSentMessage the original {@link SerialMessage} that has been completed.
	 */
	public ZigbeeTransactionCompletedEvent(SocketMessage lastSentMessage) {
		super(lastSentMessage.getMessageNode(), 1);

		this.completedMessage = lastSentMessage;
	}

	/**
	 * Gets the original {@link SerialMessage} that has been completed.
	 * @return the original message.
	 */
	public SocketMessage getCompletedMessage() {
		return completedMessage;
	}
}
