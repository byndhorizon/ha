/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.commandclass;

import org.openhab.binding.zigbee.internal.protocol.SerialMessage;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeEndpoint;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.NodeStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Handles the Hail command class. Some devices handle state changes by
 * 'hailing' the controller and asking it to request the device values
 * @author Ben Jones
 * @since 1.4.0
 */
@XStreamAlias("hailCommandClass")
public class ZigbeeHailCommandClass extends ZigbeeCommandClass {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeHailCommandClass.class);
	
	private static final int HAIL = 1;
	
	/**
	 * Creates a new instance of the ZigbeeHailCommandClass class.
	 * @param node the node this command class belongs to
	 * @param controller the controller to use
	 * @param endpoint the endpoint this Command class belongs to
	 */
	public ZigbeeHailCommandClass(ZigbeeNode node, 
			ZigbeeController controller, ZigbeeEndpoint endpoint) {
		super(node, controller, endpoint);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandClass getCommandClass() {
		return CommandClass.HAIL;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleApplicationCommandRequest(SerialMessage serialMessage, 
			int offset, int endpoint) {
		logger.trace("Handle Message Manufacture Specific Request");
		logger.debug(String.format("Received Hail command for Node ID = %d", this.getNode().getNodeId()));
		int command = serialMessage.getMessagePayloadByte(offset);
		switch (command) {
			case HAIL:
				logger.trace("Process Hail command");				
				logger.debug(String.format("Request an update of the dynamic values for node id %d", this.getNode().getNodeId()));
				
				// We only rerequest dynamic values for nodes that are completely initialized.
				if (this.getNode().getNodeStage() != NodeStage.DONE)
					return;
				
				this.getNode().setNodeStage(NodeStage.DYNAMIC);
				this.getNode().advanceNodeStage(NodeStage.DONE);
				break;
			default:
			logger.warn(String.format("Unsupported Command 0x%02X for command class %s (0x%02X).", 
					command, 
					this.getCommandClass().getLabel(),
					this.getCommandClass().getKey()));
		}
	}
}

