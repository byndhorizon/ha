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
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessageType;
import org.openhab.binding.zigbee.internal.protocol.NodeStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Handles the no operation command class. The No Operation command class is used 
 * to check if a node is reachable by sending a serial message without a command 
 * to the specified node. This can for instance be used to check that a node is 
 * non-responding.
 * @author Jan-Willem Spuij
 * @since 1.3.0
 */
@XStreamAlias("noOperationCommandClass")
public class ZigbeeNoOperationCommandClass extends ZigbeeCommandClass {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeNoOperationCommandClass.class);
	
	/**
	 * Creates a new instance of the ZigbeeNoOperationCommandClass class.
	 * @param node the node this command class belongs to
	 * @param controller the controller to use
	 * @param endpoint the endpoint this Command class belongs to
	 */
	public ZigbeeNoOperationCommandClass(ZigbeeNode node,
			ZigbeeController controller, ZigbeeEndpoint endpoint) {
		super(node, controller, endpoint);
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public CommandClass getCommandClass() {
		return CommandClass.NO_OPERATION;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleApplicationCommandRequest(SerialMessage serialMessage,
			int offset, int endpoint) {
		logger.trace("Handle No Operation Request");
		logger.debug(String.format("NODE {}: Received No Operation", this.getNode().getNodeId()));
		
		// advance node stage.
		this.getNode().advanceNodeStage(NodeStage.DETAILS);
	}

	/**
	 * Gets a SerialMessage with the No Operation command 
	 * @return the serial message
	 */
	public SerialMessage getNoOperationMessage() {
		logger.debug("NODE {}: Creating new message for application command No Operation", this.getNode().getNodeId());
		/*SerialMessage result = new SerialMessage(this.getNode().getNodeId(), SerialMessageClass.SendData, SerialMessageType.Request, SerialMessageClass.SendData, SerialMessagePriority.Low);
    	byte[] newPayload = { 	(byte) this.getNode().getNodeId(), 
    							1, 
								(byte) getCommandClass().getKey() }; 
    	result.setMessagePayload(newPayload);
    	return result;		*/
		return null;
	}

}
