/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.initialization;

import java.util.Calendar;
import java.util.Collection;

import org.openhab.binding.zigbee.internal.protocol.NodeStage;
import org.openhab.binding.zigbee.internal.protocol.SerialInterfaceException;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeEndpoint;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClassDynamicState;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClassInitialization;
//import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeManufacturerSpecificCommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeMultiInstanceCommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeNoOperationCommandClass;
//import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeVersionCommandClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZigbeeNodeStageAdvancer class. Advances the node stage, thereby controlling
 * the initialization of a node.
 * 
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZigbeeNodeStageAdvancer {

	private static final ZigbeeNodeSerializer nodeSerializer = new ZigbeeNodeSerializer();
	private static final Logger logger = LoggerFactory.getLogger(ZigbeeNodeStageAdvancer.class);

	private ZigbeeNode node;
	private ZigbeeController controller;
	private int queriesPending = -1;
	private boolean initializationComplete = false;
	private boolean restoredFromConfigfile = false;

	/**
	 * Constructor. Creates a new instance of the ZigbeeNodeStageAdvancer class.
	 * 
	 * @param node
	 *            the node this advancer belongs to.
	 * @param controller
	 *            the controller to use
	 */
	public ZigbeeNodeStageAdvancer(ZigbeeNode node, ZigbeeController controller) {
		this.node = node;
		this.controller = controller;
	}

	/**
	 * Advances the initialization stage for this node. Every node follows a
	 * certain path through it's initialization phase. These stages are visited
	 * one by one to finally end up with a completely built node structure
	 * through querying the controller / node. TODO: Handle the rest of the node
	 * stages
	 */
	public void advanceNodeStage(NodeStage targetStage) {
		if (targetStage.getStage() <= this.node.getNodeStage().getStage() && targetStage != NodeStage.DONE) {
			logger.warn(String.format("NODE %d: Already in or beyond node stage, ignoring. current = %s, requested = %s", this.node.getNodeId(),
					this.node.getNodeStage().getLabel(), targetStage.getLabel()));
			return;
		}
		logger.debug(String.format("NODE %d: Setting stage. current = %s, requested = %s", this.node.getNodeId(),
				this.node.getNodeStage().getLabel(), targetStage.getLabel()));

		this.node.setQueryStageTimeStamp(Calendar.getInstance().getTime());
		switch (this.node.getNodeStage()) {
		case EMPTYNODE:
			try {
				this.node.setNodeStage(NodeStage.PROTOINFO);
				this.controller.identifyNode(this.node.getNodeId());
			} catch (SerialInterfaceException e) {
				logger.error("NODE {}: Got error {}, while identifying node", this.node.getNodeId(), e.getLocalizedMessage());
			}
			break;
		case PROTOINFO:
			if (this.node.getNodeId() != this.controller.getOwnNodeId()) {
				ZigbeeNoOperationCommandClass zigbeeCommandClass = (ZigbeeNoOperationCommandClass) this.node
						.getCommandClass(CommandClass.NO_OPERATION);
				if (zigbeeCommandClass == null)
					break;

				this.node.setNodeStage(NodeStage.PING);
				//this.controller.sendData(zigbeeCommandClass.getNoOperationMessage());
			} else {
				logger.debug("NODE {}: Initialisation complete.", this.node.getNodeId());
				initializationComplete = true;
				this.node.setNodeStage(NodeStage.DONE); // nothing
														// more
														// to
														// do
														// for
														// this
														// node.
			}
			break;
		case PING:
		case WAKEUP:
			// if restored from a config file, redo from the dynamic
			// node stage.
			if (this.isRestoredFromConfigfile()) {
				this.node.setNodeStage(NodeStage.DYNAMIC);				
				advanceNodeStage(NodeStage.DONE);
				break;
			}
			this.node.setNodeStage(NodeStage.DETAILS);
			this.controller.requestNodeInfo(this.node.getNodeId());
			break;
		case DETAILS:
			// try and get the manufacturerSpecific command class.
		/*	ZigbeeManufacturerSpecificCommandClass manufacturerSpecific = (ZigbeeManufacturerSpecificCommandClass) this.node
					.getCommandClass(CommandClass.MANUFACTURER_SPECIFIC);

			if (manufacturerSpecific != null) {
				// if this node implements the Manufacturer Specific command
				// class, we use it to get manufacturer info.
				this.node.setNodeStage(NodeStage.MANSPEC01);
				//this.controller.sendData(manufacturerSpecific.getManufacturerSpecificMessage());
				break;
			}
		*/
			logger.warn("NODE {}: does not support MANUFACTURER_SPECIFIC, proceeding to version node stage.",
					this.node.getNodeId());
		case MANSPEC01:
			this.node.setNodeStage(NodeStage.VERSION);
			// try and get the version command class.
		/*	ZigbeeVersionCommandClass version = (ZigbeeVersionCommandClass) this.node
					.getCommandClass(CommandClass.VERSION);

			boolean checkVersionCalled = false;
			for (ZigbeeCommandClass zigbeeCommandClass : this.node.getCommandClasses()) {
				if (version != null && zigbeeCommandClass.getMaxVersion() > 1) {
					version.checkVersion(zigbeeCommandClass); // check version
																// for this
																// command
																// class.
					checkVersionCalled = true;
				} else
					zigbeeCommandClass.setVersion(1);
			}
		
			if (checkVersionCalled) // wait for another call of advanceNodeStage
		*/							// before continuing.
				break;
		case VERSION:
			this.node.setNodeStage(NodeStage.INSTANCES_ENDPOINTS);
			// try and get the multi instance / channel command class.
			ZigbeeMultiInstanceCommandClass multiInstance = (ZigbeeMultiInstanceCommandClass) this.node
					.getCommandClass(CommandClass.MULTI_INSTANCE);

			if (multiInstance != null) {
				multiInstance.initEndpoints();
				break;
			}

			logger.trace("NODE {}: does not support MULTI_INSTANCE, proceeding to static node stage.",
					this.node.getNodeId());
		case INSTANCES_ENDPOINTS:
			this.node.setNodeStage(NodeStage.STATIC_VALUES);
		case STATIC_VALUES:
			if (queriesPending == -1) {
				queriesPending = 0;
				for (ZigbeeCommandClass zigbeeCommandClass : this.node.getCommandClasses()) {
					logger.trace("NODE {}: Inspecting command class {}", this.node.getNodeId(), zigbeeCommandClass.getCommandClass().getLabel());
					if (zigbeeCommandClass instanceof ZigbeeCommandClassInitialization) {
						logger.debug("NODE {}: Found initializable command class {}", this.node.getNodeId(), zigbeeCommandClass.getCommandClass()
								.getLabel());
						ZigbeeCommandClassInitialization zcci = (ZigbeeCommandClassInitialization) zigbeeCommandClass;
						int instances = zigbeeCommandClass.getInstances();
						if (instances == 0) {
							Collection<SerialMessage> initqueries = zcci.initialize();
							for (SerialMessage serialMessage : initqueries) {
								//this.controller.sendData(serialMessage);
								queriesPending++;
							}
						} else {
							for (int i = 1; i <= instances; i++) {
								Collection<SerialMessage> initqueries = zcci.initialize();
								for (SerialMessage serialMessage : initqueries) {
									//this.controller
									//		.sendData(this.node.encapsulate(serialMessage, zigbeeCommandClass, i));
									queriesPending++;
								}
							}
						}
					} else if (zigbeeCommandClass instanceof ZigbeeMultiInstanceCommandClass) {
						ZigbeeMultiInstanceCommandClass multiInstanceCommandClass = (ZigbeeMultiInstanceCommandClass) zigbeeCommandClass;
						for (ZigbeeEndpoint endpoint : multiInstanceCommandClass.getEndpoints()) {
							for (ZigbeeCommandClass endpointCommandClass : endpoint.getCommandClasses()) {
								logger.trace(String.format("NODE %d: Inspecting command class %s for endpoint %d", this.node.getNodeId(), endpointCommandClass
										.getCommandClass().getLabel(), endpoint.getEndpointId()));
								if (endpointCommandClass instanceof ZigbeeCommandClassInitialization) {
									logger.debug("NODE {}: Found initializable command class {}", this.node.getNodeId(), endpointCommandClass
											.getCommandClass().getLabel());
									ZigbeeCommandClassInitialization zcci2 = (ZigbeeCommandClassInitialization) endpointCommandClass;
									Collection<SerialMessage> initqueries = zcci2.initialize();
									//for (SerialMessage serialMessage : initqueries) {
									//	this.controller.sendData(this.node.encapsulate(serialMessage,
									//			endpointCommandClass, endpoint.getEndpointId()));
									//	queriesPending++;
									//}
								}
							}
						}
					}
				}
			}
			if (queriesPending-- > 0) // there is still something to be
										// initialized.
				break;

			logger.trace("NODE {}: Done getting static values, proceeding to dynamic node stage.", this.node.getNodeId());
			queriesPending = -1;
			this.node.setNodeStage(NodeStage.DYNAMIC);
		case DYNAMIC:
			if (queriesPending == -1) {
				queriesPending = 0;
				for (ZigbeeCommandClass zigbeeCommandClass : this.node.getCommandClasses()) {
					logger.trace("NODE {}: Inspecting command class {}", this.node.getNodeId(), zigbeeCommandClass.getCommandClass().getLabel());
					if (zigbeeCommandClass instanceof ZigbeeCommandClassDynamicState) {
						logger.debug("NODE {}: Found dynamic state command class {}", this.node.getNodeId(), zigbeeCommandClass.getCommandClass()
								.getLabel());
						ZigbeeCommandClassDynamicState zdds = (ZigbeeCommandClassDynamicState) zigbeeCommandClass;
						int instances = zigbeeCommandClass.getInstances();
						/*if (instances == 0) {
							Collection<SerialMessage> dynamicQueries = zdds.getDynamicValues();
							for (SerialMessage serialMessage : dynamicQueries) {
								this.controller.sendData(serialMessage);
								queriesPending++;
							}
						} else {
							for (int i = 1; i <= instances; i++) {
								Collection<SerialMessage> dynamicQueries = zdds.getDynamicValues();
								for (SerialMessage serialMessage : dynamicQueries) {
									this.controller
											.sendData(this.node.encapsulate(serialMessage, zigbeeCommandClass, i));
									queriesPending++;
								}
							}
						}*/
					} else if (zigbeeCommandClass instanceof ZigbeeMultiInstanceCommandClass) {
						ZigbeeMultiInstanceCommandClass multiInstanceCommandClass = (ZigbeeMultiInstanceCommandClass) zigbeeCommandClass;
						for (ZigbeeEndpoint endpoint : multiInstanceCommandClass.getEndpoints()) {
							for (ZigbeeCommandClass endpointCommandClass : endpoint.getCommandClasses()) {
								logger.trace(String.format("NODE %d: Inspecting command class %s for endpoint %d", this.node.getNodeId(), endpointCommandClass
										.getCommandClass().getLabel(), endpoint.getEndpointId()));
								/*if (endpointCommandClass instanceof ZigbeeCommandClassDynamicState) {
									logger.debug("NODE {}: Found dynamic state command class {}", this.node.getNodeId(), endpointCommandClass
											.getCommandClass().getLabel());
									ZigbeeCommandClassDynamicState zdds2 = (ZigbeeCommandClassDynamicState) endpointCommandClass;
									Collection<SerialMessage> dynamicQueries = zdds2.getDynamicValues();
									for (SerialMessage serialMessage : dynamicQueries) {
										this.controller.sendData(this.node.encapsulate(serialMessage,
												endpointCommandClass, endpoint.getEndpointId()));
										queriesPending++;
									}
								}*/
							}
						}
					}
				}
			}
			if (queriesPending-- > 0) // there is still something to be
										// initialized.
				break;
			logger.trace("NODE {}: Done getting dynamic values, proceeding to done node stage.", this.node.getNodeId());
			queriesPending = -1;

			this.node.setNodeStage(NodeStage.DONE); // nothing
													// more
													// to
													// do
													// for
													// this
													// node.

			nodeSerializer.SerializeNode(this.node);

			logger.debug("NODE {}: Initialisation complete.", this.node.getNodeId());
			initializationComplete = true;
			break;
		case DONE:
		case DEAD:
			break;
		default:
			logger.error("NODE {}: Unknown node state {} encountered.", this.node.getNodeId(), this.node.getNodeStage().getLabel());
		}
	}

	/**
	 * Returns whether the initialization process has completed.
	 * 
	 * @return true if initialization has completed. False otherwise.
	 */
	public boolean isInitializationComplete() {
		return initializationComplete;
	}

	/**
	 * Returns whether the node was restored from a config file.
	 * 
	 * @return the restoredFromConfigfile
	 */
	public boolean isRestoredFromConfigfile() {
		return restoredFromConfigfile;
	}

	/**
	 * Restores a node from an XML file using the @ ZigbeeNodeSerializer} class.
	 * 
	 * @return true if succeeded, false otherwise.
	 */
	public boolean restoreFromConfig() {
		ZigbeeNode restoredNode = nodeSerializer.DeserializeNode(this.node.getNodeId());
		
		if (restoredNode == null)
			return false;

		// Sanity check the data from the file
		if (restoredNode.getVersion() != this.node.getVersion()
				|| restoredNode.getManufacturer() == 0
				|| restoredNode.isListening() != this.node.isListening()
				|| restoredNode.isFrequentlyListening() != this.node.isFrequentlyListening()
				|| restoredNode.isRouting() != this.node.isRouting()
				|| !restoredNode.getDeviceClass().equals(this.node.getDeviceClass())) {
			logger.warn("NODE {}: Config file differs from controler information, ignoring config.",
					this.node.getNodeId());
			return false;
		}
		
		this.node.setDeviceId(restoredNode.getDeviceId());
		this.node.setDeviceType(restoredNode.getDeviceType());
		this.node.setManufacturer(restoredNode.getManufacturer());

		for (ZigbeeCommandClass commandClass : restoredNode.getCommandClasses()) {
			commandClass.setController(this.controller);
			commandClass.setNode(this.node);
			
			if (commandClass instanceof ZigbeeMultiInstanceCommandClass) {
				for (ZigbeeEndpoint endPoint : ((ZigbeeMultiInstanceCommandClass)commandClass).getEndpoints()) {
					for (ZigbeeCommandClass endpointCommandClass : endPoint.getCommandClasses()) {
						endpointCommandClass.setController(this.controller);
						endpointCommandClass.setNode(this.node);
						endpointCommandClass.setEndpoint(endPoint);
					}
				}
			}
			
			this.node.addCommandClass(commandClass);
		}

		logger.debug("NODE {}: Restored from config.", this.node.getNodeId());
		restoredFromConfigfile = true;
		return true;
	}
}
