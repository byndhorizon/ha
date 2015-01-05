/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.converter;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.bubblecloud.zigbee.ZigBeeApiContext;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.ZigBeeDeviceException;
import org.bubblecloud.zigbee.api.cluster.Cluster;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.ZigBeeClusterException;
import org.bubblecloud.zigbee.api.cluster.measureament_sensing.TemperatureMeasurement;
import org.openhab.binding.zigbee.ZigbeeBindingConfig;
import org.openhab.binding.zigbee.ZigbeeBindingProvider;
import org.openhab.binding.zigbee.internal.converter.command.BinaryOnOffCommandConverter;
import org.openhab.binding.zigbee.internal.converter.command.ZigbeeCommandConverter;
import org.openhab.binding.zigbee.internal.protocol.SocketMessage;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeDeviceTypes;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeEndpoint;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.NodeStage;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeMultiInstanceCommandClass;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeCommandClassValueEvent;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.ContactItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZigbeeConverterHandler class. Acts as a factory
 * and manager of all the converters the binding can
 * use to convert between the Zigbee api and the binding.
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZigbeeConverterHandler {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeConverterHandler.class);
	
	private final Map<CommandClass, ZigbeeCommandClassConverter<?>> converters = new HashMap<CommandClass, ZigbeeCommandClassConverter<?>>();
	private final Map<String, Zigbee4JavaCommandConverter> z4j_converters = new HashMap<String, Zigbee4JavaCommandConverter>();
	private final Map<Class<? extends Item>, CommandClass[]> preferredCommandClasses = new HashMap<Class<? extends Item>, CommandClass[]>();
	private final ZigbeeController controller;
	private final ZigbeeInfoConverter infoConverter;
	private final EventPublisher eventPublisher;
	
	private Integer refresh_index=0;
	private Integer REFRESH_MAX=86400; //one day=60s*60*24
	
	/**
	 * Constructor. Creates a new instance of the {@ link ZigbeeConverterHandler} class.
	 * @param controller the {@link ZigbeeController} to use to send messages.
	 * @param eventPublisher the {@link EventPublisher} to use to post updates.
	 */
	public ZigbeeConverterHandler(ZigbeeController controller, EventPublisher eventPublisher) {
		this.controller = controller;
		this.eventPublisher = eventPublisher;
		// add converters here
		z4j_converters.put("onoff", new ZigbeeOnOffConverter(controller, eventPublisher));
		//z4j_converters.put("temperature", new ZigbeeTemperatureConverter(controller, eventPublisher));
		//converters.put(CommandClass.THERMOSTAT_SETPOINT, new ZigbeeThermostatSetpointConverter(controller, eventPublisher));
		//converters.put(CommandClass.BATTERY, new ZigbeeBatteryConverter(controller, eventPublisher));
		converters.put(CommandClass.SWITCH_BINARY, new ZigbeeBinarySwitchConverter(controller, eventPublisher));
		//converters.put(CommandClass.SWITCH_MULTILEVEL, new ZigbeeMultiLevelSwitchConverter(controller, eventPublisher));
		//converters.put(CommandClass.SENSOR_BINARY, new ZigbeeBinarySensorConverter(controller, eventPublisher));
		//converters.put(CommandClass.SENSOR_MULTILEVEL, new ZigbeeMultiLevelSensorConverter(controller, eventPublisher));
		//converters.put(CommandClass.SENSOR_ALARM, new ZigbeeAlarmSensorConverter(controller, eventPublisher));
		//converters.put(CommandClass.METER, new ZigbeeMeterConverter(controller, eventPublisher));
		//converters.put(CommandClass.BASIC, new ZigbeeBasicConverter(controller, eventPublisher));
		converters.put(CommandClass.SCENE_ACTIVATION, new ZigbeeSceneConverter(controller, eventPublisher));
		infoConverter = new ZigbeeInfoConverter(controller, eventPublisher);
		
		// add preferred command classes per Item class here
		preferredCommandClasses.put(SwitchItem.class, new CommandClass[] { CommandClass.SWITCH_BINARY, CommandClass.BASIC });
		/*preferredCommandClasses.put(SwitchItem.class, new CommandClass[] { CommandClass.SWITCH_BINARY, CommandClass.SWITCH_MULTILEVEL, 
			CommandClass.METER, CommandClass.BASIC, CommandClass.SENSOR_BINARY, CommandClass.SENSOR_ALARM });
		preferredCommandClasses.put(DimmerItem.class, new CommandClass[] { CommandClass.SWITCH_MULTILEVEL, CommandClass.SWITCH_BINARY, 
			CommandClass.BASIC, CommandClass.SENSOR_MULTILEVEL, CommandClass.SENSOR_BINARY, CommandClass.SENSOR_ALARM });
		preferredCommandClasses.put(RollershutterItem.class, new CommandClass[] { CommandClass.SWITCH_MULTILEVEL, CommandClass.SWITCH_BINARY, 
			CommandClass.BASIC, CommandClass.SENSOR_MULTILEVEL, CommandClass.SENSOR_BINARY, CommandClass.SENSOR_ALARM });
		preferredCommandClasses.put(NumberItem.class, new CommandClass[] { CommandClass.SENSOR_MULTILEVEL, CommandClass.METER, 
			CommandClass.SWITCH_MULTILEVEL, CommandClass.BATTERY, CommandClass.BASIC, CommandClass.SENSOR_BINARY, 
			CommandClass.SENSOR_ALARM, CommandClass.SWITCH_BINARY, CommandClass.THERMOSTAT_SETPOINT });
		preferredCommandClasses.put(ContactItem.class, new CommandClass[] { CommandClass.SENSOR_BINARY, CommandClass.SENSOR_ALARM, 
			CommandClass.SWITCH_BINARY, CommandClass.BASIC });*/
	}
	
	/**
	 * Returns the {@link EventPublisher} that can be used
	 * inside the converter to publish event updates.
	 * @return the eventPublisher
	 */
	protected EventPublisher getEventPublisher() {
		return this.eventPublisher;
	}

	/**
	 * Returns a converter to convert between the Zigbee API and the binding.
	 * @param commandClass the {@link CommandClass} to create a converter for.
	 * @return a {@link ZigbeeCommandClassConverter} or null if a converter is not found.
	 */
	public ZigbeeCommandClassConverter<?> getConverter(CommandClass commandClass) {
		return converters.get(commandClass);
	}
	
	/**
	 * Returns a converter to convert between the Zigbee API and the binding.
	 * @param commandClass the {@link CommandClass} to create a converter for.
	 * @return a {@link ZigbeeCommandClassConverter} or null if a converter is not found.
	 */
	public Zigbee4JavaCommandConverter getConverter(String command) {
		return z4j_converters.get(command);
	}
	
	/**
	 * Returns the command class that provides the best suitable converter to convert between the Zigbee API and the binding.
	 * @param item the {@link item} to resolve a converter for.
	 * @param node the {@link ZigbeeNode} node to resolve a Command class on.
	 * @param the enpoint ID to use to resolve a converter.
	 * @return the {@link ZigbeeCommandClass} that can be used to get a converter suitable to do the conversion.
	 */
	private ZigbeeCommandClass resolveConverter(Item item, Device node, int endpointId) {
		if(item == null)
			return null;

		ZigbeeMultiInstanceCommandClass multiInstanceCommandClass = null;
		ZigbeeCommandClass result = null;
		
		//if (endpointId != 1)
		//	multiInstanceCommandClass = (ZigbeeMultiInstanceCommandClass)node.getCommandClass(CommandClass.MULTI_INSTANCE);

		if (!preferredCommandClasses.containsKey(item.getClass())) {
			logger.warn("No preferred command classes found for item class = {}", item.getClass().toString());
			return null;
		}
		
		for (CommandClass commandClass : preferredCommandClasses.get(item.getClass())) {
			if (multiInstanceCommandClass != null && multiInstanceCommandClass.getVersion() == 2) {
				ZigbeeEndpoint endpoint = multiInstanceCommandClass.getEndpoint(endpointId);
				
				if (endpoint != null) { 
					result = endpoint.getCommandClass(commandClass);
				} 
			}
			
		/*	if (result == null)
				result = node.getCommandClass(commandClass);*/
			
			if (result == null)
				continue;
			
			if (multiInstanceCommandClass != null && multiInstanceCommandClass.getVersion() == 1 &&
					result.getInstances() < endpointId)
				continue;
			
			if (converters.containsKey(commandClass))
				return result;
		}
		/*
		logger.warn("No matching command classes found for item class = {}, node id = {}, endpoint id = {}", 
				item.getClass().toString(), node.getNodeId(), endpointId);*/
		return null;
	}
	
	/**
	 * Execute refresh method. This method is called every time a binding item is
	 * refreshed and the corresponding node should be sent a message.
	 * @param provider the {@link ZigbeeBindingProvider} that provides the item
	 * @param itemName the name of the item to poll.
	 * @param forceRefresh indicates that a polling refresh should be forced.
	 */
	@SuppressWarnings("unchecked")
	public void executeRefresh(ZigbeeBindingProvider provider, String itemName, boolean forceRefresh) {
		ZigbeeBindingConfig bindingConfiguration = provider.getZigbeeBindingConfig(itemName);
		
//		Integer refresh_interval = bindingConfiguration.getRefreshInterval();
//		logger.info("ZigbeeConverterHandler.executeRefresh()");
//		if (refresh_interval.equals(0)) {
//			return;
//		}
//		if(refresh_index==REFRESH_MAX-1) {
//			refresh_index=0;
//			return;
//		}
//		else if(refresh_index%refresh_interval!=0) {
//			refresh_index+=1;
//			return;
//		}
		
		logger.info("ZigbeeConverterHandler.executeRefresh:  start to refresh");
		//get Device from zigbeeDevices by ieee+endpoingid
		//if can't find the Device, send permitJoin to add this device to controller's zigbeeDevices
		ZigBeeApiContext zigbeeApiContext = this.controller.getZigbeeApiContext();
		Device device = zigbeeApiContext.getDevice(bindingConfiguration.getEndpointId());
		if(device == null) {
			logger.error("Item {} has non existant node {}", itemName, bindingConfiguration.getEndpointId());
			return;
		}
		System.out.println("Yxxxxxxxxxxxxxxxxxxx111");
		//get CommandClass from command string in provider
		//need to have a map between commands in demo.items and CommandClass in zigbee4java
		//also need to create the map between openhab items(Switch, Dimmer, Number, String etc.) and CommandClass's function in zigbee4java
		Map<String, String> arguments = bindingConfiguration.getArguments();
		if (!arguments.containsKey("command")) {
			logger.error("Item {} has no device type set {}", itemName, bindingConfiguration.getEndpointId());
			return;
		}
		System.out.println("Yxxxxxxxxxxxxxxxxxxx222");
		Item item = provider.getItem(itemName);
		
		Zigbee4JavaCommandConverter converter = getConverter(arguments.get("command"));

		converter.executeRefresh(provider.getItem(itemName), device, bindingConfiguration.getArguments());


		/*Class<? extends Cluster> deviceTypeClass = ZigbeeDeviceTypes.getTypeDispatcher(arguments.get("type"));
		//Class<? extends Cluster> ZigbeeDeviceTypes().getCluster("Switch");
		Cluster cluster = device.getCluster(deviceTypeClass);
		*/
		//translate command to CommandClass's function
		//then execute the function
		//then send response to UI/content provider
//		System.out.println("print command: executeRefresh");
//		//System.out.println(command);
//		if (arguments.get("command").equalsIgnoreCase("DECIMAL")) {
//			
//			final TemperatureMeasurement temp = device.getCluster(TemperatureMeasurement.class);
//				try {
//					System.out.println("temp.getMeasuredValue(), id"
//							+ temp.getMeasuredValue().getId() + "name"
//							+ temp.getMeasuredValue().getName() + "value"
//							+ temp.getMeasuredValue().getValue());
//					
//					this.getEventPublisher().postUpdate(item.getName(), 
//							(State)new DecimalType((int)temp.getMeasuredValue().getValue()));
//				} catch (ZigBeeClusterException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//				
//				
//			
//            
//		} 
//		
//		System.out.println("xxxxxxxxxxxxxxxxxxx333");
			
		/*
		ZigbeeBindingConfig bindingConfiguration = provider.getZigbeeBindingConfig(itemName);
		ZigbeeCommandClass commandClass;
		String commandClassName = bindingConfiguration.getArguments().get("command");
		
		// this binding is configured not to poll.
		if (!forceRefresh && bindingConfiguration.getRefreshInterval() != null && 0 == bindingConfiguration.getRefreshInterval())
			return;
		
		ZigbeeNode node = this.controller.getNode(bindingConfiguration.getNodeId());
		
		// ignore nodes that are not initialized.
		if (node == null)
			return;
		
		if (commandClassName != null) {
			
			// this is a report item, handle it with the report info converter.
			if (commandClassName.equalsIgnoreCase("info")) {
				infoConverter.executeRefresh(provider.getItem(itemName), node, bindingConfiguration.getEndpoint(), bindingConfiguration.getArguments());
				return;
			}
			
			// ignore nodes that are not initialized or dead.
			if (node.getNodeStage() != NodeStage.DONE)
				return;
			
			commandClass = node.resolveCommandClass(CommandClass.getCommandClass(commandClassName), bindingConfiguration.getEndpoint());
			
			if (commandClass == null) {
				 logger.warn("No command class found for item = {}, command class name = {}, ignoring execute refresh.", itemName, commandClassName);
				 return;
			}
		} else {
			commandClass = null; //resolveConverter(provider.getItem(itemName), node
				//, bindingConfiguration.getEndpoint());
		}
		
		 if (commandClass == null) {
			 logger.warn("No converter found for item = {}, ignoring execute refresh.", itemName);
			 return;
		 }
		 
		 ZigbeeCommandClassConverter<ZigbeeCommandClass> converter = (ZigbeeCommandClassConverter<ZigbeeCommandClass>) getConverter(commandClass.getCommandClass());
		 
		 if (converter == null) {
			 logger.warn("No converter found for item = {}, ignoring execute refresh.", itemName);
			 return;
		 }
		 
		 if (bindingConfiguration.getRefreshInterval() == null) {
			 bindingConfiguration.setRefreshInterval(converter.getRefreshInterval());
	
			 // this binding is configured not to poll.
			if (!forceRefresh && 0 == bindingConfiguration.getRefreshInterval())
				return;
		 }

		 // not enough time has passed to refresh the item.
		 if (!forceRefresh && bindingConfiguration.getLastRefreshed() != null 
				 && (bindingConfiguration.getLastRefreshed().getTime() + (bindingConfiguration.getRefreshInterval() * 1000) 
						 > Calendar.getInstance().getTimeInMillis()))
			 return;
			 
		 bindingConfiguration.setLastRefreshed(Calendar.getInstance().getTime());
		 converter.executeRefresh(node, commandClass, bindingConfiguration.getEndpoint(), bindingConfiguration.getArguments());
	*/}

	/**
	 * Get the refresh interval for an item binding
	 * 
	 * @param provider
	 *            the {@link ZigbeeBindingProvider} that provides the item
	 * @param itemName
	 *            the name of the item to poll.
	 */
	@SuppressWarnings("unchecked")
	public Integer getRefreshInterval(ZigbeeBindingProvider provider, String itemName) {
		ZigbeeBindingConfig bindingConfiguration = provider.getZigbeeBindingConfig(itemName);
		ZigbeeCommandClass commandClass;
		String commandClassName = bindingConfiguration.getArguments().get("command");

		// this binding is configured not to poll.
		if (bindingConfiguration.getRefreshInterval() != null && 0 == bindingConfiguration.getRefreshInterval())
			return 0;

//		ZigbeeNode node = this.controller.getNode(bindingConfiguration.getNodeId());
//
//		// ignore nodes that are not initialized.
//		if (node == null)
//			return 0;
//
//		if (commandClassName != null) {
//			// this is a report item, handle it with the report info converter.
//			if (commandClassName.equalsIgnoreCase("info")) {
//				return infoConverter.getRefreshInterval();
//			}
//
//			commandClass = node.resolveCommandClass(CommandClass.getCommandClass(commandClassName),
//					bindingConfiguration.getEndpoint());
//
//			if (commandClass == null) {
//				logger.warn("No command class found for item = {}, command class name = {}, using 0 refresh interval.",
//						itemName, commandClassName);
//				return 0;
//			}
//		} else {
//			commandClass = null;//resolveConverter(provider.getItem(itemName), node, bindingConfiguration.getEndpoint());
//		}
//
//		if (commandClass == null) {
//			logger.warn("No converter found for item = {}, using 0 refresh interval.", itemName);
//			return 0;
//		}
//
//		ZigbeeCommandClassConverter<ZigbeeCommandClass> converter = (ZigbeeCommandClassConverter<ZigbeeCommandClass>) getConverter(commandClass
//				.getCommandClass());
//
//		if (converter == null) {
//			logger.warn("No converter found for item = {}, using 0 refresh interval.", itemName);
//			return 0;
//		}
//
//		if (bindingConfiguration.getRefreshInterval() == null) {
//			bindingConfiguration.setRefreshInterval(converter.getRefreshInterval());
//		}

		return bindingConfiguration.getRefreshInterval();
	}

	/**
	 * Handles an incoming {@link ZigbeeCommandClassValueEvent}. Implement
	 * this message in derived classes to convert the value and post an
	 * update on the openHAB bus.
	 * @param provider the {@link ZigbeeBindingProvider} that provides the item
	 * @param itemName the name of the item that will receive the event.
	 * @param event the received {@link ZigbeeCommandClassValueEvent}.
	 */
	 public void handleEvent(ZigbeeBindingProvider provider, String itemName, ZigbeeCommandClassValueEvent event) {
		ZigbeeBindingConfig bindingConfiguration = provider.getZigbeeBindingConfig(itemName);
		Item item = provider.getItem(itemName);
		String commandClassName = bindingConfiguration.getArguments().get("command");
		boolean respondToBasic = "true".equalsIgnoreCase(bindingConfiguration.getArguments().get("respond_to_basic"));

		logger.trace("Getting converter for item = {}, command class = {}, item command class = {}", itemName, event.getCommandClass().getLabel(), commandClassName);
		
		if (item == null)
			return;
		
		// check whether this item is bound to the right command class.
		
		if (commandClassName != null && !commandClassName.equalsIgnoreCase(event.getCommandClass().getLabel().toLowerCase()) &&
				!(respondToBasic && event.getCommandClass() == CommandClass.BASIC)) 
			return;
		
		 ZigbeeCommandClassConverter<?> converter = this.getConverter(event.getCommandClass());
		 
		 
		 if (converter == null) {
			 logger.warn("No converter found for command class = {}, ignoring event.",event.getCommandClass().toString());
			 return;
		 }
		 
		 converter.handleEvent(event, item, bindingConfiguration.getArguments());
	 }
	
	/**
	 * Receives a command from openHAB and translates it to an operation
	 * on the Zigbee network.
	 * @param provider the {@link ZigbeeBindingProvider} that provides the item
	 * @param itemName the name of the item that will receive the event.
	 * @param command the received {@link Command}
	 */
	@SuppressWarnings("unchecked")
	public void receiveCommand(ZigbeeBindingProvider provider, String itemName, Command command) {
		ZigbeeBindingConfig bindingConfiguration = provider.getZigbeeBindingConfig(itemName);
		
		//get Device from zigbeeDevices by ieee+endpoingid
		//if can't find the Device, send permitJoin to add this device to controller's zigbeeDevices
		ZigBeeApiContext zigbeeApiContext = this.controller.getZigbeeApiContext();
		Device device = zigbeeApiContext.getDevice(bindingConfiguration.getEndpointId());
		if(device == null) {
			logger.error("Item {} has non existant node {}", itemName, bindingConfiguration.getEndpointId());
			return;
		}
		
		//get CommandClass from command string in provider
		//need to have a map between commands in demo.items and CommandClass in zigbee4java
		//also need to create the map between openhab items(Switch, Dimmer, Number, String etc.) and CommandClass's function in zigbee4java
		Map<String, String> arguments = bindingConfiguration.getArguments();
		if (!arguments.containsKey("command")) {
			logger.error("Item {} has no device type set {}", itemName, bindingConfiguration.getEndpointId());
			return;
		}
		
		Item item = provider.getItem(itemName);
		
		Zigbee4JavaCommandConverter converter = getConverter(arguments.get("command"));

		converter.receiveCommand(provider.getItem(itemName), command, device, bindingConfiguration.getArguments());

		/*Class<? extends Cluster> deviceTypeClass = ZigbeeDeviceTypes.getTypeDispatcher(arguments.get("type"));
		//Class<? extends Cluster> ZigbeeDeviceTypes().getCluster("Switch");
		Cluster cluster = device.getCluster(deviceTypeClass);
		*/
		//translate command to CommandClass's function
		//then execute the function
		//then send response to UI/content provider
		System.out.println("print command:");
		System.out.println(command);
		if (arguments.get("command").equalsIgnoreCase("SWITCH_BINARY")) {
			
			
		
			OnOff onOff = device.getCluster(OnOff.class);
			
            try {
            	if (command==OnOffType.ON) {
            		System.out.println("xxxxxxxxxxxxxxxxxxxONNN");
            		onOff.on();
            	}
            	else if (command==OnOffType.OFF) {
            		System.out.println("xxxxxxxxxxxxxxxxxxxOFFF");
                	onOff.off();
                	
            	}
            	else
            		System.out.println("wrong commands!!!!!!!!!!!!!11");
            } catch (ZigBeeDeviceException e) {
                e.printStackTrace();
            }
		} 
		
		System.out.println("xxxxxxxxxxxxxxxxxxx333");
		if (command instanceof State)
			this.getEventPublisher().postUpdate(item.getName(), (State)command);
		
		/*
		 OnOff onOff = device.getCluster(OnOff.class);
            try {
                onOff.on();
            } catch (ZigBeeDeviceException e) {
                e.printStackTrace();
            }
		 */
		

		
	/*
		ZigBeeApiContext zigbeeApiContext = this.controller.getZigbeeApiContext();
		Device device = zigbeeApiContext.getDevice(bindingConfiguration.getEndpointId());
		if(device == null) {
			logger.error("Item {} has non existant node {}", itemName, bindingConfiguration.getEndpointId());
			return;
		}
		ZigbeeCommandClass commandClass;
		String commandClassName = bindingConfiguration.getArguments().get("command");
		
		if (commandClassName != null) {
			//commandClass = node.resolveCommandClass(CommandClass.getCommandClass(commandClassName), bindingConfiguration.getEndpoint());
			if (commandClass == null) {
				 logger.warn("No command class found for item = {}, command class name = {}, ignoring command.", itemName, commandClassName);
				 return;
			}
		} else {
			commandClass = resolveConverter(provider.getItem(itemName), device
				, bindingConfiguration.getEndpoint());
		}
		
		 if (commandClass == null) {
			 logger.warn("No converter found for item = {}, ignoring command.", itemName);
			 return;
		 }
		 System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+commandClass.getClass());
		 ZigbeeCommandClassConverter<ZigbeeCommandClass> converter = (ZigbeeCommandClassConverter<ZigbeeCommandClass>) getConverter(commandClass.getCommandClass());
		 
		 //ZigbeeCommandClassConverter<ZigbeeCommandClass> converter = null;
		 if (converter == null) {
			 logger.warn("No converter found for item = {}, ignoring command.", itemName);
			 return; 
		 }
		 System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+converter.getClass());
		 
		 converter.receiveCommand(provider.getItem(itemName), command, device, commandClass, bindingConfiguration.getEndpoint(), bindingConfiguration.getArguments());
		*/
	}
		
}
