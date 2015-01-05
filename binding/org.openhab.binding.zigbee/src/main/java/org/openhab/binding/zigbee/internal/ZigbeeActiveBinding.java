/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.zigbee.ZigbeeBindingConfig;
import org.openhab.binding.zigbee.ZigbeeBindingProvider;
import org.openhab.binding.zigbee.internal.config.ZigbeeConfiguration;
import org.openhab.binding.zigbee.internal.converter.ZigbeeConverterHandler;
import org.openhab.binding.zigbee.internal.protocol.SerialInterfaceException;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeController;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeEventListener;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeCommandClassValueEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeInitializationCompletedEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeTransactionCompletedEvent;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.binding.BindingProvider;
import org.openhab.core.types.Command;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZigbeeActiveBinding Class. Polls Zigbee nodes frequently,
 * responds to item commands, and also handles events coming 
 * from the Zigbee controller.
 * @author Victor Belov
 * @author Brian Crosby
 * @author Jan-Willem Spuij
 * @author Chris Jackson
 * @since 1.3.0
 */
public class ZigbeeActiveBinding extends AbstractActiveBinding<ZigbeeBindingProvider> implements ManagedService, ZigbeeEventListener {
	/**
	 * The refresh interval which is used to poll values from the Zigbee binding. 
	 */
	private long refreshInterval = 5000;
	
	private int pollingQueue = 1;

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeActiveBinding.class);
	private String port;
	private String ipAddress;
	private boolean isSUC = false;
	private Integer healtime = null;
	private Integer timeout = null;
	private volatile ZigbeeController zController;
	private volatile ZigbeeConverterHandler converterHandler;

	private boolean isZigbeeNetworkReady = false;
	private boolean isZigbeeDevicesAllReady = false;
	
	private Iterator<ZigbeePollItem> pollingIterator = null;
	private List<ZigbeePollItem> pollingList = new ArrayList<ZigbeePollItem>();
	
	private Integer refresh_index=0;
	private Integer REFRESH_MAX=86400; //one day=60s*60*24
	
	// Configuration Service
	ZigbeeConfiguration zConfigurationService;
	
	// Network monitoring class
	ZigbeeNetworkMonitor networkMonitor;

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getName() {
		return "Zigbee Refresh Service";
	}

	/**
	 * Working method that executes refreshing of the bound items. The method is executed
	 * at every refresh interval. The nodes are polled only every 6 refreshes.
	 */
	@Override
	protected void execute() {
		//System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE: execute()");
	/*	if(!isZigbeeNetworkReady){
			logger.debug("Zigbee Network isn't ready yet!");
			if (this.zController != null)
				this.zController.checkForDeadOrSleepingNodes(providers);
			return;
		}*/
		
		// Call the network monitor
		////networkMonitor.execute();
		logger.info("pollingList size: "+pollingList.size());
		// If we're not currently in a poll cycle, restart the polling table
		if(pollingIterator == null) {
			pollingIterator = pollingList.iterator();
		}
		
		// Loop through the polling list. We only allow a certain number of messages
		// into the send queue at a time to avoid congestion within the system.
		// Basically, we don't want the polling to slow down 'important' stuff.
		// The queue ensures all nodes get a chance - if we always started at the top
		// then the last items might never get polled.
		
		while(pollingIterator.hasNext()) {
		/*	if(zController.getSendQueueLength() >= pollingQueue) {
				logger.trace("Polling queue full!");
				break;
			}*/
			ZigbeePollItem poll = pollingIterator.next();
			logger.info("polling item: "+poll.item);
			logger.info("poll.provider: "+ poll.provider);
			if (converterHandler==null)
				logger.info("converterHandler==null");
			
			//check polling item to poll
			ZigbeeBindingConfig bindingConfiguration = poll.provider.getZigbeeBindingConfig(poll.item);
			
			Integer refresh_interval = bindingConfiguration.getRefreshInterval();
			
			if(refresh_index%refresh_interval!=0) {
				continue;
			}
			logger.info("ZigbeeConverterHandler.executeRefresh()");
			converterHandler.executeRefresh(poll.provider, poll.item, false);
		}
		if(pollingIterator.hasNext() == false) {
			pollingIterator = null;
		}
		
		if(refresh_index==REFRESH_MAX-1) {
			refresh_index=0;
		} else {
			refresh_index+=1;
		}
	}
	
	/**
	 * Called, if a single binding has changed. The given item could have been
	 * added or removed. We refresh the binding in case it's in the done stage.
	 * 
	 * @param provider the binding provider where the binding has changed
	 * @param itemName the item name for which the binding has changed
	 */
	@Override
	public void bindingChanged(BindingProvider provider, String itemName) {
		logger.trace("bindingChanged {}", itemName);		
		
		ZigbeeBindingProvider zProvider = (ZigbeeBindingProvider)provider;
		
		if (zProvider != null) {
			ZigbeeBindingConfig bindingConfig = zProvider.getZigbeeBindingConfig(itemName);
			
			if (bindingConfig != null && converterHandler != null) {
				converterHandler.executeRefresh(zProvider, itemName, true);
			}
		}

		// Bindings have changed - rebuild the polling table
		rebuildPollingTable();
		
		super.bindingChanged(provider, itemName);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void allBindingsChanged(BindingProvider provider) {
		logger.trace("allBindingsChanged");		
		super.allBindingsChanged(provider);

		// Bindings have changed - rebuild the polling table
		rebuildPollingTable();
	}

	
	/**
	 * This method rebuilds the polling table. The polling table is a list of items that have
	 * polling enabled (ie a refresh interval is set). This list is then checked periodically
	 * and any item that has passed its polling interval will be polled.
	 */
	private void rebuildPollingTable() {
		// Rebuild the polling table
		pollingList.clear();

		// Loop all binding providers for the Z-wave binding.
		for (ZigbeeBindingProvider eachProvider : providers) {
			// loop all bound items for this provider
			for (String name : eachProvider.getItemNames()) {
				
//				ZigbeePollItem item = new ZigbeePollItem();
//				item.item = name;
//				item.provider = eachProvider;
//				pollingList.add(item);
				
				logger.trace("Polling list: Checking {} == {}", name, converterHandler.getRefreshInterval(eachProvider, name));
				
				// This binding is configured to poll - add it to the list
				if (converterHandler.getRefreshInterval(eachProvider, name) > 0) {
					ZigbeePollItem item = new ZigbeePollItem();
					item.item = name;
					item.provider = eachProvider;
					pollingList.add(item);
					logger.info("Polling list added {}", name);
				}
			}
		}
		pollingIterator = null;
	}
	
	/**
	 * Handles a command update by sending the appropriate Zigbee instructions
	 * to the controller.
	 * {@inheritDoc}
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		boolean handled = false;
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+itemName.toString());
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+this.isProperlyConfigured());
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+isZigbeeNetworkReady);
		// if we are not yet initialized, don't waste time and return
		if((this.isProperlyConfigured() == false) | (isZigbeeNetworkReady == false)) {
			logger.debug("internalReceiveCommand Called, But Not Properly Configure yet or Zigbee Network Isn't Ready, returning.");
			return;
		}

		logger.trace("internalReceiveCommand(itemname = {}, Command = {})", itemName, command.toString());
		for (ZigbeeBindingProvider provider : providers) {

			if (!provider.providesBindingFor(itemName))
				continue;
			
			converterHandler.receiveCommand(provider, itemName, command);
			handled = true;
		}

		if (!handled)
			logger.warn("No converter found for item = {}, command = {}, ignoring.", itemName, command.toString());
	}
	
	/**
	 * Activates the binding. Actually does nothing, because on activation
	 * OpenHAB always calls updated to indicate that the config is updated.
	 * Activation is done there.
	 */
	@Override
	public void activate() {
		
	}
	
	/**
	 * Deactivates the binding. The Controller is stopped and the serial interface
	 * is closed as well.
	 */
	@Override
	public void deactivate() {
		isZigbeeNetworkReady = false;
		if (this.converterHandler != null) {
			this.converterHandler = null;
		}

		if (this.zConfigurationService != null) {
			this.zController.removeEventListener(this.zConfigurationService);
			this.zConfigurationService = null;
		}

		ZigbeeController controller = this.zController;
		if (controller != null) {
			this.zController = null;
			controller.close();
			controller.removeEventListener(this);
		}
	}
	
	/**
	 * Initialises the binding. This is called after the 'updated' method
	 * has been called and all configuration has been passed.
	 * @throws ConfigurationException 
	 */
	private void initialise() throws ConfigurationException {
		try {
			this.setProperlyConfigured(true);
			this.deactivate();
			//isZigbeeNetworkReady = true;
			this.zController = new ZigbeeController(isSUC, port, timeout);
			this.converterHandler = new ZigbeeConverterHandler(this.zController, this.eventPublisher);
			zController.initialize();
			zController.addEventListener(this);
			System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+port);
			// The network monitor service needs to know the controller...
			this.networkMonitor = new ZigbeeNetworkMonitor(this.zController);
			if(healtime != null)
				this.networkMonitor.setHealTime(healtime);

			// The config service needs to know the controller and the network monitor...
			this.zConfigurationService = new ZigbeeConfiguration(this.zController, this.networkMonitor);
			zController.addEventListener(this.zConfigurationService);
			
			if(zController.isZigbeeNetowrkStarted())
				isZigbeeNetworkReady = true;
			
			rebuildPollingTable();
			return;
		} catch (SerialInterfaceException ex) {
			this.setProperlyConfigured(false);
			throw new ConfigurationException("ipAddress", ex.getLocalizedMessage(), ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updated(Dictionary<String, ?> config) throws ConfigurationException {
		if (config == null)
			return;
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE:"+config.toString());
		// Check the serial port configuration value.
		// This value is mandatory.
		if (StringUtils.isNotBlank((String) config.get("port"))) {
			port = (String) config.get("port");
			logger.info("Update config, port = {}", port);
		}
		if (StringUtils.isNotBlank((String) config.get("ip"))) {
			ipAddress = (String) config.get("ip");
			logger.info("Update config, ipAddress = {}", ipAddress);
		}
		if (StringUtils.isNotBlank((String) config.get("healtime"))) {
			try {
				healtime = Integer.parseInt((String) config.get("healtime"));
				logger.info("Update config, healtime = {}", healtime);
			} catch (NumberFormatException e) {
				healtime = null;
				logger.error("Error parsing 'healtime'. This must be a single number to set the hour to perform the heal.");
			}
		}
		if (StringUtils.isNotBlank((String) config.get("refreshInterval"))) {
			try {
				refreshInterval = Integer.parseInt((String) config.get("refreshInterval"));
				logger.info("Update config, refreshInterval = {}", refreshInterval);
			} catch (NumberFormatException e) {
				refreshInterval = 10000;
				logger.error("Error parsing 'refreshInterval'. This must be a single number time in milliseconds.");
			}
		}
		if (StringUtils.isNotBlank((String) config.get("pollingQueue"))) {
			try {
				pollingQueue = Integer.parseInt((String) config.get("pollingQueue"));
				logger.info("Update config, pollingQueue = {}", pollingQueue);
			} catch (NumberFormatException e) {
				pollingQueue = 2;
				logger.error("Error parsing 'pollingQueue'. This must be a single number time in milliseconds.");
			}
		}
		if (StringUtils.isNotBlank((String) config.get("timeout"))) {
			try {
				timeout = Integer.parseInt((String) config.get("timeout"));
				logger.info("Update config, timeout = {}", timeout);
			} catch (NumberFormatException e) {
				timeout = null;
				logger.error("Error parsing 'timeout'. This must be an Integer.");
			}
		}
		if (StringUtils.isNotBlank((String) config.get("setSUC"))) {
			try {
				isSUC = Boolean.parseBoolean((String) config.get("setSUC"));
				logger.info("Update config, setSUC = {}", isSUC);
			} catch (NumberFormatException e) {
				isSUC = false;
				logger.error("Error parsing 'setSUC'. This must be boolean.");
			}
		}

		// Now that we've read ALL the configuration, initialise the binding.
		initialise();
	}

	/**
	 * Returns the port value.
	 * @return
	 */
	public String getPort() {
		return port;
	}
	
	/**
	 * Returns the ipAddress value.
	 * @return
	 */
	public String getIP() {
		return ipAddress;
	}

	/**
	 * Event handler method for incoming Zigbee events.
	 * @param event the incoming Zigbee event.
	 */
	@Override
	public void ZigbeeIncomingEvent(ZigbeeEvent event) {
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE: ZigbeeIncomingEvent"+event.getClass());
		// if we are not yet initialized, don't waste time and return
		if (!this.isProperlyConfigured())
			return;
		System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE: ZigbeeIncomingEvent1");
		if (!isZigbeeNetworkReady) {
			if (event instanceof ZigbeeInitializationCompletedEvent) {
				logger.debug("ZigbeeIncomingEvent Called, Network Event, Init Done. Setting Zigbee Network Ready.");
				isZigbeeNetworkReady = true;
				System.out.println("ZIGBEEEEEEEEEEEEEEEEEEEE: ZigbeeIncomingEvent2");
				// Initialise the polling table
				rebuildPollingTable();

				return;
			}		
		}
		
		logger.debug("ZigbeeIncomingEvent");
		
		// ignore transaction completed events.
		if (event instanceof ZigbeeTransactionCompletedEvent)
			return;
		
		// handle command class value events.
		if (event instanceof ZigbeeCommandClassValueEvent) {
			handleZigbeeCommandClassValueEvent((ZigbeeCommandClassValueEvent)event);
			return;
		}
	}

	/**
	 * Handle an incoming Command class value event
	 * @param event the incoming Zigbee event.
	 */
	private void handleZigbeeCommandClassValueEvent(
		ZigbeeCommandClassValueEvent event) {
		boolean handled = false;

		logger.debug("Got a value event from Zigbee network for nodeId = {}, endpoint = {}, command class = {}, value = {}", 
				new Object[] { event.getNodeId(), event.getEndpoint(), event.getCommandClass().getLabel(), event.getValue() } );

		for (ZigbeeBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				ZigbeeBindingConfig bindingConfig = provider.getZigbeeBindingConfig(itemName);
				
				if (bindingConfig.getNodeId() != event.getNodeId() || bindingConfig.getEndpoint() != event.getEndpoint())
					continue;
				
				converterHandler.handleEvent(provider, itemName, event);
				handled = true;
			}
		}
		
		if (!handled)
			logger.warn("No item bound for event from nodeId = {}, endpoint = {}, command class = {}, value = {}, ignoring.", 
					new Object[] { event.getNodeId(), event.getEndpoint(), event.getCommandClass().getLabel(), event.getValue() } );
	}
	
	class ZigbeePollItem {
		ZigbeeBindingProvider provider;
		String item;
	}
}
