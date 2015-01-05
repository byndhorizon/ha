/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol;

//import gnu.io.CommPort;
//import gnu.io.CommPortIdentifier;
//import gnu.io.NoSuchPortException;
//import gnu.io.PortInUseException;
//import gnu.io.SerialPort;
//import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.ZigBeeApiContext;
import org.bubblecloud.zigbee.api.Device;
import org.openhab.binding.zigbee.ZigbeeBindingConfig;
import org.openhab.binding.zigbee.ZigbeeBindingProvider;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessageClass;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessagePriority;
import org.openhab.binding.zigbee.internal.protocol.SerialMessage.SerialMessageType;
import org.openhab.binding.zigbee.internal.protocol.NodeStage;
import org.openhab.binding.zigbee.internal.protocol.SocketMessage.SocketMessageClass;
import org.openhab.binding.zigbee.internal.protocol.SocketMessage.SocketMessageType;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeInclusionEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeInitializationCompletedEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeNodeStatusEvent;
import org.openhab.binding.zigbee.internal.protocol.event.ZigbeeTransactionCompletedEvent;
import org.openhab.binding.zigbee.internal.protocol.initialization.ZigbeeNodeSerializer;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.AddNodeMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.AssignReturnRouteMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.AssignSucReturnRouteMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.DeleteReturnRouteMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.EnableSucMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.GetControllerCapabilitiesMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.GetSucNodeIdMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.IdentifyNodeMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.RemoveNodeMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.RequestNodeNeighborUpdateMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.RemoveFailedNodeMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.RequestNodeInfoMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.GetRoutingInfoMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.SendDataMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.SerialApiSoftResetMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.serialmessage.SetSucNodeMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.ZigbeeCommandProcessor;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.GetVersionMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.MemoryGetIdMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.SerialApiGetCapabilitiesMessageClass;
import org.openhab.binding.zigbee.internal.protocol.serialmessage.SerialApiGetInitDataMessageClass;
//import org.openhab.binding.zigbee.internal.protocol.socketmessage.SocketGetDeviceListClass;
//import org.openhab.binding.zigbee.internal.protocol.socketmessage.SocketPermitJoinClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Zigbee controller class. Implements communication with the Zigbee
 * controller stick using serial messages.
 * @author Victor Belov
 * @author Brian Crosby
 * @author Chris Jackson
 * @since 1.3.0
 */
public class ZigbeeController {
	
	private static final Logger logger = LoggerFactory.getLogger(ZigbeeController.class);
	
	private static final int QUERY_STAGE_TIMEOUT = 120000;
	private static final int ZIGBEE_RESPONSE_TIMEOUT = 5000;		// 5000 ms ZIGBEE_RESPONSE TIMEOUT
	private static final int ZIGBEE_RECEIVE_TIMEOUT = 1000;		// 1000 ms ZIGBEE_RECEIVE_TIMEOUT
	private static final int INITIAL_QUEUE_SIZE = 128; 
	private static final long WATCHDOG_TIMER_PERIOD = 10000;	// 10 seconds watchdog timer

	private static final int TRANSMIT_OPTION_ACK = 0x01;
	private static final int TRANSMIT_OPTION_AUTO_ROUTE = 0x04;
	private static final int TRANSMIT_OPTION_EXPLORE = 0x20;
	
	private Map<Long, ZigbeeNode> zigbeeNodes = new HashMap<Long, ZigbeeNode>();
	//the map to store devices in zigbee network
	private Map<String, Device> zigbeeDevices = new HashMap<String, Device>();
	private final ArrayList<ZigbeeEventListener> zigbeeEventListeners = new ArrayList<ZigbeeEventListener>();
	private final PriorityBlockingQueue<SocketMessage> sendQueue = new PriorityBlockingQueue<SocketMessage>(INITIAL_QUEUE_SIZE, new SocketMessage.SocketMessageComparator(this));
	private ZigbeeSendThread sendThread;
	private ZigbeeReceiveThread receiveThread;
	
	private final Semaphore transactionCompleted = new Semaphore(1);
	private volatile SocketMessage lastSentMessage = null;
	private long lastMessageStartTime = 0;
	private long longestResponseTime = 0;
//	private SerialPort serialPort;
	private ZigBeeApi zigbeeApi;
	private ZigBeeApiContext zigApiContext;
	private Boolean isZigbeeNetworkStarted = false;
	private Boolean enableShutdown = false;
	private Socket socketClient;
	private OutputStream socketOut;
	private InputStream socketIn;
	private int port = 2540;
	private int ZigbeeResponseTimeout = ZIGBEE_RESPONSE_TIMEOUT;
	private Timer watchdog;
	
	private String ZigbeeVersion = "Unknown";
	private String serialAPIVersion = "Unknown";
	private int homeId = 0;
	private int ownNodeId = 0;
	private int manufactureId = 0;
	private int deviceType = 0; 
	private int deviceId = 0;
	private int ZigbeeLibraryType = 0;
	private int sentDataPointer = 1;
	private boolean setSUC = false;
	private ZigbeeDeviceType controllerType = ZigbeeDeviceType.UNKNOWN;
	private int sucID = 0;
	
	private int SOFCount = 0;
	private int CANCount = 0;
	private int NAKCount = 0;
	private int ACKCount = 0;
	private int OOFCount = 0;
	private AtomicInteger timeOutCount = new AtomicInteger(0);
	
	private boolean initializationComplete = false;
	
	private boolean isConnected;

	// Constructors
	
	/**
	 * Constructor. Creates a new instance of the Zigbee controller class.
	 * @param serialPortName the serial port name to use for 
	 * communication with the Zigbee controller stick.
	 * @throws SerialInterfaceException when a connection error occurs.
	 */
	public ZigbeeController(final boolean isSUC, final String port, final Integer timeout) throws SerialInterfaceException {
			logger.info("Starting Zigbee controller");
			this.setSUC = isSUC;
			if(timeout != null && timeout >= 1500 && timeout <= 10000) {
				ZigbeeResponseTimeout = timeout;
			}
			logger.info("Zigbee timeout is set to {}ms.", ZigbeeResponseTimeout);
			connect(port);
			this.watchdog = new Timer(true);
		//	this.watchdog.schedule(
		//			new WatchDogTimerTask(ipAddress), 
		//			WATCHDOG_TIMER_PERIOD, WATCHDOG_TIMER_PERIOD);
	}

	// Incoming message handlers
	
	/**
	 * Handles incoming Serial Messages. Serial messages can either be messages
	 * that are a response to our own requests, or the stick asking us information.
	 * @param incomingMessage the incoming message to process.
	 */
	private void handleIncomingMessage(SocketMessage incomingMessage, SocketMessage lastSentMessage) {
		logger.trace("Incoming message to process");
		//logger.debug(incomingMessage.toString());
		
		
		switch (lastSentMessage.getMessageType()) {
			case Request:
				handleIncomingRequestMessage(incomingMessage, lastSentMessage);
				break;
			case Response:
				handleIncomingResponseMessage(incomingMessage, lastSentMessage);
				break;
			default:
				logger.warn("Unsupported incomingMessageType: 0x%02X", incomingMessage.getMessageType());
		}
		
	}

	/**
	 * Handles an incoming request message.
	 * An incoming request message is a message initiated by a node or the controller.
	 * @param incomingMessage the incoming message to process.
	 */
	private void handleIncomingRequestMessage(SocketMessage incomingMessage, SocketMessage lastSentMessage) {
		logger.trace("Message type = REQUEST");

		ZigbeeCommandProcessor processor = ZigbeeCommandProcessor.getMessageDispatcher(incomingMessage.getMessageClass());
		if(processor != null) {
			processor.handleRequest(this, lastSentMessage, incomingMessage);

			if(processor.isTransactionComplete()) {
				notifyEventListeners(new ZigbeeTransactionCompletedEvent(this.lastSentMessage));
				transactionCompleted.release();
				logger.trace("Released. Transaction completed permit count -> {}", transactionCompleted.availablePermits());
			}
		}
		else {
			logger.warn(String.format("TODO: Implement processing of Request Message = %s (0x%02X)",
					incomingMessage.getMessageClass().getLabel(),
					incomingMessage.getMessageClass().getKey()));
		}
	}

	/**
	 * Handles a failed SendData request. This can either be because of the stick actively reporting it
	 * or because of a time-out of the transaction in the send thread.
	 * @param originalMessage the original message that was sent
	 */
	private void handleFailedSendDataRequest(SerialMessage originalMessage) {
		//new SendDataMessageClass().handleFailedSendDataRequest(this, originalMessage);
	}

	/**
	 * Handles an incoming response message.
	 * An incoming response message is a response, based one of our own requests.
	 * @param incomingMessage the response message to process.
	 */
	private void handleIncomingResponseMessage(SocketMessage incomingMessage, SocketMessage lastSentMessage) {
		logger.trace("Message type = RESPONSE");

		ZigbeeCommandProcessor processor = ZigbeeCommandProcessor.getMessageDispatcher(lastSentMessage.getMessageClass());
		if(processor != null) {
			processor.handleResponse(this, lastSentMessage, incomingMessage);

		/*	if(processor.isTransactionComplete()) {
				notifyEventListeners(new ZigbeeTransactionCompletedEvent(this.lastSentMessage));
				transactionCompleted.release();
				logger.trace("Released. Transaction completed permit count -> {}", transactionCompleted.availablePermits());
			}*/
		}
		else {
			logger.warn(String.format("TODO: Implement processing of Response Message = %s (0x%02X)",
					incomingMessage.getMessageClass().getLabel(),
					incomingMessage.getMessageClass().getKey()));
		}

		switch (lastSentMessage.getMessageClass()) {
			case GetDeviceList:
				this.isConnected = true;
//				this.zigbeeNodes = ((SocketGetDeviceListClass)processor).getNodes();
				break;
			case SetSwitch:
				break;
			case SendData:
				break;
				
			case GetVersion:
//				this.ZigbeeVersion = ((GetVersionMessageClass)processor).getVersion();
				this.ZigbeeLibraryType = ((GetVersionMessageClass)processor).getLibraryType();
				break;
			case MemoryGetId:
				this.ownNodeId = ((MemoryGetIdMessageClass)processor).getNodeId();
				this.homeId = ((MemoryGetIdMessageClass)processor).getHomeId();
				break;
			case SerialApiGetInitData:
			/*	this.isConnected = true;
				for(Integer nodeId : ((SerialApiGetInitDataMessageClass)processor).getNodes()) {
					// Place nodes in the local Zigbee Controller
					ZigbeeNode node = new ZigbeeNode(this.homeId, nodeId, this);
					if(nodeId == this.ownNodeId) {
						// This is the controller node.
						// We already know the device type, id, manufacturer so set it here
						// It won't be set later as we probably won't request the manufacturer specific data
						node.setDeviceId(this.getDeviceId());
						node.setDeviceType(this.getDeviceType());
						node.setManufacturer(this.getManufactureId());
					}
					this.zigbeeNodes.put(nodeId, node);
					node.advanceNodeStage(NodeStage.PROTOINFO);
				}*/
				break;
			case GetSucNodeId:
			/*	// Remember the SUC ID
				this.sucID = ((GetSucNodeIdMessageClass)processor).getSucNodeId();
				
				// If we want to be the SUC, enable it here
				if(this.setSUC == true && this.sucID == 0) {
					// We want to be SUC
					this.enqueue(new EnableSucMessageClass().doRequest(EnableSucMessageClass.SUCType.SERVER));
					this.enqueue(new SetSucNodeMessageClass().doRequest(this.ownNodeId, SetSucNodeMessageClass.SUCType.SERVER));
				}
				else if(this.setSUC == false && this.sucID == this.ownNodeId) {
					// We don't want to be SUC, but we currently are!
					// Disable SERVER functionality, and set the node to 0
					this.enqueue(new EnableSucMessageClass().doRequest(EnableSucMessageClass.SUCType.NONE));
					this.enqueue(new SetSucNodeMessageClass().doRequest(this.ownNodeId, SetSucNodeMessageClass.SUCType.NONE));
				}
				this.enqueue(new GetControllerCapabilitiesMessageClass().doRequest());*/
				break;
			case SerialApiGetCapabilities:
			/*	this.serialAPIVersion = ((SerialApiGetCapabilitiesMessageClass)processor).getSerialAPIVersion();
				this.manufactureId = ((SerialApiGetCapabilitiesMessageClass)processor).getManufactureId();
				this.deviceId = ((SerialApiGetCapabilitiesMessageClass)processor).getDeviceId();
				this.deviceType = ((SerialApiGetCapabilitiesMessageClass)processor).getDeviceType();
				
				this.enqueue(new SerialApiGetInitDataMessageClass().doRequest());*/
				break;
			case GetControllerCapabilities:
				this.controllerType = ((GetControllerCapabilitiesMessageClass)processor).getDeviceType();
				break;
			default:
				break;				
		}
	}
	
	// Controller methods

	/**
	 * Connects to the comm port and starts send and receive threads.
	 * @param serialPortName the port name to open
	 * @throws SerialInterfaceException when a connection error occurs.
	 */
	public void connect(final String port)
			throws SerialInterfaceException {
		logger.info("Connecting to serial port {}", port);
		 
		logger.info("ZigBee API starting up...");
		try {
			zigbeeApi = new ZigBeeApi(port, 32011, 11, false);
			logger.info("zigbeeapi init done");
			isZigbeeNetworkStarted = zigbeeApi.startup();
		} catch (Exception e) {
			e.printStackTrace();
			logger.info(e.getMessage());
		}

        logger.info("##########################zigbee startup staus "+ isZigbeeNetworkStarted);
        /*try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
        System.out.print("Browsing network ...");
        while (!enableShutdown && !zigbeeApi.isInitialBrowsingComplete()) {
            System.out.print('.');
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                break;
            }
        }
        /*
		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(ipAddress);
			CommPort commPort = portIdentifier.open("org.openhab.binding.zigbee",2000);
			this.serialPort = (SerialPort) commPort;
			this.serialPort.setSerialPortParams(115200,SerialPort.DATABITS_8,SerialPort.STOPBITS_1,SerialPort.PARITY_NONE);
			this.serialPort.enableReceiveThreshold(1);
			this.serialPort.enableReceiveTimeout(ZIGBEE_RECEIVE_TIMEOUT);
			this.receiveThread = new ZigbeeReceiveThread();
			this.receiveThread.start();
			this.sendThread = new ZigbeeSendThread();
			this.sendThread.start();
			socketClient = new Socket(ipAddress, port);
			
			//this.receiveThread = new ZigbeeReceiveThread();
			//this.receiveThread.start();
			this.sendThread = new ZigbeeSendThread();
		//	this.sendThread.start();
			logger.info("Zigbee Socket is initialized");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public boolean isZigbeeNetowrkStarted() {
		return isZigbeeNetworkStarted;
	}
	
	/**
	 * Closes the connection to the Zigbee controller.
	 */
	public void close()	{
		if (watchdog != null) {
			watchdog.cancel();
			watchdog = null;
		}
		
		disconnect();
		
		// clear nodes collection and send queue
		for (Object listener : this.zigbeeEventListeners.toArray()) {
			if (!(listener instanceof ZigbeeNode))
				continue;
			
			this.zigbeeEventListeners.remove(listener);
		}
		
		this.zigbeeNodes.clear();
		this.sendQueue.clear();
		
		logger.info("Stopped Zigbee controller");
	}

	/**
	 * Disconnects from the serial interface and stops
	 * send and receive threads.
	 */
	public void disconnect() {
		if (sendThread != null) {
			sendThread.interrupt();
			try {
				sendThread.join();
			} catch (InterruptedException e) {
			}
			sendThread = null;
		}
		if (receiveThread != null) {
			receiveThread.interrupt();
			try {
				receiveThread.join();
			} catch (InterruptedException e) {
			}
			receiveThread = null;
		}
		if(transactionCompleted.availablePermits() < 0)
			transactionCompleted.release(transactionCompleted.availablePermits());
		
		transactionCompleted.drainPermits();
		logger.trace("Transaction completed permit count -> {}", transactionCompleted.availablePermits());
//		if (this.serialPort != null) {
//			this.serialPort.close();
//			this.serialPort = null;
//		}
		logger.info("Disconnected from serial port");
	}
	
	/**
	 * Enqueues a message for sending on the send queue.
	 * @param socketMessage the serial message to enqueue.
	 */
	public void enqueue(SocketMessage socketMessage) {
		this.sendQueue.add(socketMessage);
		logger.debug("Enqueueing message. Queue length = {}", this.sendQueue.size());
	}

	/**
	 * Returns the size of the send queue.
	 */
	public int getSendQueueLength() {
		return this.sendQueue.size();
	}

	/**
	 * Notify our own event listeners of a Zigbee event.
	 * @param event the event to send.
	 */
	public void notifyEventListeners(ZigbeeEvent event) {
		logger.debug("Notifying event listeners");
		for (ZigbeeEventListener listener : this.zigbeeEventListeners) {
			logger.trace("Notifying {}", listener.toString());
			listener.ZigbeeIncomingEvent(event);
		}
		
		// We also need to handle the inclusion internally within the controller
		/*if(event instanceof ZigbeeInclusionEvent) {
			ZigbeeInclusionEvent incEvent = (ZigbeeInclusionEvent)event;
			switch(incEvent.getEvent()) {
			case IncludeDone:
				logger.debug("NODE {}: Including node.", incEvent.getNodeId());
				// First make sure this isn't an existing node
				if(getNode(incEvent.getNodeId()) != null) {
					logger.debug("NODE {}: Newly included node already exists - not initialising.", incEvent.getNodeId());
					break;
				}
				
				// Initialise the new node
				ZigbeeNode node = new ZigbeeNode(this.homeId, incEvent.getNodeId(), this);

				this.zigbeeNodes.put(incEvent.getNodeId(), node);
				node.advanceNodeStage(NodeStage.PROTOINFO);
				break;
			case ExcludeDone:
				logger.debug("NODE {}: Excluding node.", incEvent.getNodeId());
				// Remove the node from the controller
				if(getNode(incEvent.getNodeId()) == null) {
					logger.debug("NODE {}: Excluding node that doesn't exist.", incEvent.getNodeId());
					break;
				}
				this.zigbeeNodes.remove(incEvent.getNodeId());
				
				// Remove the XML file
				ZigbeeNodeSerializer nodeSerializer = new ZigbeeNodeSerializer();
				nodeSerializer.DeleteNode(event.getNodeId());
				break;
			default:
				break;
			}
		}*/
	}
	
	/**
	 * Initializes communication with the Zigbee controller stick.
	 */
	public void initialize() {
		//this.enqueue(new GetVersionMessageClass().doRequest());
		//this.enqueue(new MemoryGetIdMessageClass().doRequest());
		//this.enqueue(new SerialApiGetCapabilitiesMessageClass().doRequest());
		//this.enqueue(new GetSucNodeIdMessageClass().doRequest());
		//System.out.println("start to send get device list ...");
		//this.enqueue(new SocketGetDeviceListClass().doRequest());
		
        System.out.println("ZigbeeController: get device list");
        System.out.println("Found " + zigbeeApi.getDevices().size() + " nodes.");
        zigApiContext = zigbeeApi.getZigBeeApiContext();
        zigbeeDevices.clear();
        List<Device> devices = zigbeeApi.getDevices();
        for (int i=0; i<devices.size(); i++) {
        	Device device = devices.get(i);
        	zigbeeDevices.put(device.getIEEEAddress()+"/"+device.getEndpointId(), device);
        }
		
	}
	
	/**
	 * Send Identify Node message to the controller.
	 * @param nodeId the nodeId of the node to identify
	 * @throws SerialInterfaceException when timing out or getting an invalid response.
	 */
	public void identifyNode(long nodeId) throws SerialInterfaceException {
		/*this.enqueue(new IdentifyNodeMessageClass().doRequest(nodeId));*/
	}
	
	/**
	 * Send Request Node info message to the controller.
	 * @param nodeId the nodeId of the node to identify
	 * @throws SerialInterfaceException when timing out or getting an invalid response.
	 */
	public void requestNodeInfo(long nodeId) {
		/*this.enqueue(new RequestNodeInfoMessageClass().doRequest(nodeId));*/
	}
	
	/**
	 * Checks for dead or sleeping nodes during Node initialization.
	 * JwS: merged checkInitComplete and checkForDeadOrSleepingNodes to prevent possibly looping nodes multiple times.
	 * @param providers 
	 */
	public void checkForDeadOrSleepingNodes(Collection<ZigbeeBindingProvider> providers){
		int completeCount = 0;
		boolean dead_flag = false;
		
		if (zigbeeNodes.isEmpty())
			return;
		
		ZigbeeEvent zEvent = new ZigbeeInitializationCompletedEvent(this.ownNodeId);
		this.notifyEventListeners(zEvent);
		
		for (ZigbeeBindingProvider provider : providers) {
			for (String itemName : provider.getItemNames()) {
				ZigbeeBindingConfig bindingConfig = provider.getZigbeeBindingConfig(itemName);
				
				if(zigbeeNodes.get(bindingConfig.getNodeId())!=null) {
					continue;
				}
				else {
					dead_flag = true;
					
//					this.enqueue(new SocketPermitJoinClass().doRequest( new ZigbeeNode(1, bindingConfig.getNodeId(), this)));					
				}
			}
		}
		
		if (dead_flag) {
			//this.enqueue(new SocketPermitJoinClass().doRequest());
		}
		/*
		// There are still nodes waiting to get a ping.
		// So skip the dead node checking.
		for (SerialMessage serialMessage : sendQueue) {
			if (serialMessage.getPriority() == SerialMessagePriority.Low)
				return;
		}
		
		logger.trace("Checking for Dead or Sleeping Nodes.");
		for (Map.Entry<Long, ZigbeeNode> entry : zigbeeNodes.entrySet()){
			if (entry.getValue().getNodeStage() == NodeStage.EMPTYNODE)
				continue;
			
			logger.debug(String.format("NODE %d: Has been in Stage %s since %s", entry.getKey(), entry.getValue().getNodeStage().getLabel(), entry.getValue().getQueryStageTimeStamp().toString()));
			
			if(entry.getValue().getNodeStage() == NodeStage.DONE || entry.getValue().getNodeStage() == NodeStage.DEAD
					|| (!entry.getValue().isListening() && !entry.getValue().isFrequentlyListening())) {
				completeCount++;
				continue;
			}
			
			logger.trace("NODE {}: Checking if {} miliseconds have passed in current stage.", entry.getKey(), QUERY_STAGE_TIMEOUT);
			
			if(Calendar.getInstance().getTimeInMillis() < (entry.getValue().getQueryStageTimeStamp().getTime() + QUERY_STAGE_TIMEOUT))
				continue;
			
			logger.warn(String.format("NODE %d: May be dead, setting stage to DEAD.", entry.getKey()));
			entry.getValue().setNodeStage(NodeStage.DEAD);

			completeCount++;
		}
		
		// If all nodes are completed, then we say the binding is ready for business
		if(this.zigbeeNodes.size() == completeCount && initializationComplete == false) {
			// We only want this event once!
			initializationComplete = true;
			
			ZigbeeEvent zEvent = new ZigbeeInitializationCompletedEvent(this.ownNodeId);
			this.notifyEventListeners(zEvent);
			
			// If there are DEAD nodes, send a Node Status event
			// We do that here to avoid messing with the binding initialisation
			for(ZigbeeNode node : this.getNodes()) {
				if (node.isDead()) {
					logger.debug("NODE {}: DEAD node.", node.getNodeId());

					zEvent = new ZigbeeNodeStatusEvent(node.getNodeId(), ZigbeeNodeStatusEvent.State.Dead);
					this.notifyEventListeners(zEvent);
				}
			}
		}*/
	}

	/**
	 * Request the node routing information.
	 *
	 * @param nodeId The address of the node to update
	 */
	public void requestNodeRoutingInfo(int nodeId)
	{
		/*this.enqueue(new GetRoutingInfoMessageClass().doRequest(nodeId));*/
	}

	/**
	 * Request the node neighbor list to be updated for the specified node.
	 * Once this is complete, the requestNodeRoutingInfo will be called
	 * automatically to update the data in the binding.
	 *
	 * @param nodeId The address of the node to update
	 */
	public void requestNodeNeighborUpdate(int nodeId)
	{
		/*this.enqueue(new RequestNodeNeighborUpdateMessageClass().doRequest(nodeId));*/
	}

	/**
	 * Puts the controller into inclusion mode to add new nodes
	 */
	public void requestAddNodesStart()
	{
		/*this.enqueue(new AddNodeMessageClass().doRequestStart(true));*/
	}

	/**
	 * Terminates the inclusion mode
	 */
	public void requestAddNodesStop()
	{
		/*this.enqueue(new AddNodeMessageClass().doRequestStop());*/
	}

	/**
	 * Puts the controller into exclusion mode to remove new nodes
	 */
	public void requestRemoveNodesStart()
	{
		/*this.enqueue(new RemoveNodeMessageClass().doRequestStart(true));*/
	}

	/**
	 * Terminates the exclusion mode
	 */
	public void requestRemoveNodesStop()
	{
		/*this.enqueue(new RemoveNodeMessageClass().doRequestStop());*/
	}

	/**
	 * Removes a failed node from the network.
	 * Note that this won't remove nodes that have not failed.
	 * @param nodeId The address of the node to remove
	 */
	public void requestRemoveFailedNode(int nodeId)
	{
		/*this.enqueue(new RemoveFailedNodeMessageClass().doRequest(nodeId));*/
	}

	/**
	 * Delete all return nodes from the specified node. This should be performed
	 * before updating the routes
	 * 
	 * @param nodeId
	 */
	public void requestDeleteAllReturnRoutes(int nodeId)
	{
		/*this.enqueue(new DeleteReturnRouteMessageClass().doRequest(nodeId));*/
	}

	/**
	 * Request the controller to set the return route between two nodes
	 * 
	 * @param nodeId
	 *            Source node
	 * @param destinationId
	 *            Destination node
	 */
	public void requestAssignReturnRoute(int nodeId, int destinationId)
	{
		/*this.enqueue(new AssignReturnRouteMessageClass().doRequest(nodeId, destinationId, getCallbackId()));*/
	}

	/**
	 * Request the controller to set the return route from a node to the controller
	 * 
	 * @param nodeId
	 *            Source node
	 */
	public void requestAssignSucReturnRoute(int nodeId)
	{
		/*this.enqueue(new AssignSucReturnRouteMessageClass().doRequest(nodeId, getCallbackId()));*/
	}

	/**
	 * Returns the next callback ID
	 * @return callback ID
	 */
	public int getCallbackId() {
		if (++sentDataPointer > 0xFF)
			sentDataPointer = 1;
		logger.debug("Callback ID = {}", sentDataPointer);
		
		return sentDataPointer;
	}
	
	/**
	 * Transmits the SerialMessage to a single Zigbee Node.
	 * Sets the transmission options as well.
	 * @param serialMessage the Serial message to send.
	 */
	public void sendData(SocketMessage serialMessage)
	{
    /*	if (serialMessage.getMessageClass() != SocketMessageClass.SendData) {
    		logger.error(String.format("Invalid message class %s (0x%02X) for sendData", serialMessage.getMessageClass().getLabel(), serialMessage.getMessageClass().getKey()));
    		return;
    	}
    	if (serialMessage.getMessageType() != SocketMessageType.Request) {
    		logger.error("Only request messages can be sent");
    		return;
    	}
    	
    	ZigbeeNode node = this.getNode(serialMessage.getMessageNode());
    	
    	// Keep track of the number of packets sent to this device
    	node.incrementSendCount();*/
    	/*
    	if (!node.isListening() && !node.isFrequentlyListening() && serialMessage.getPriority() != SerialMessagePriority.Low) {
			ZigbeeWakeUpCommandClass wakeUpCommandClass = (ZigbeeWakeUpCommandClass)node.getCommandClass(CommandClass.WAKE_UP);

			// If it's a battery operated device, check if it's awake or place in wake-up queue.
			if (wakeUpCommandClass != null && !wakeUpCommandClass.processOutgoingWakeupMessage(serialMessage)) {
				return;
			}
		}
    	*/
    /*	serialMessage.setTransmitOptions(TRANSMIT_OPTION_ACK | TRANSMIT_OPTION_AUTO_ROUTE | TRANSMIT_OPTION_EXPLORE);
    	serialMessage.setCallbackId(getCallbackId());
    	this.enqueue(serialMessage);*/
	}
	
	/**
	 * Add a listener for Zigbee events to this controller.
	 * @param eventListener the event listener to add.
	 */
	public void addEventListener(ZigbeeEventListener eventListener) {
		this.zigbeeEventListeners.add(eventListener);
	}

	/**
	 * Remove a listener for Zigbee events to this controller.
	 * @param eventListener the event listener to remove.
	 */
	public void removeEventListener(ZigbeeEventListener eventListener) {
		this.zigbeeEventListeners.remove(eventListener);
	}
	
    /**
     * Gets the API Version of the controller.
	 * @return the serialAPIVersion
	 */
	public String getSerialAPIVersion() {
		return serialAPIVersion;
	}
	
    /**
     * Gets the Zigbee Version of the controller.
	 * @return the ZigbeeVersion
	 */
	public String getZigbeeVersion() {
		return ZigbeeVersion;
	}

	/**
	 * Gets the Manufacturer ID of the controller. 
	 * @return the manufactureId
	 */
	public int getManufactureId() {
		return manufactureId;
	}

	/**
	 * Gets the device type of the controller;
	 * @return the deviceType
	 */
	public int getDeviceType() {
		return deviceType;
	}

	/**
	 * Gets the device ID of the controller.
	 * @return the deviceId
	 */
	public int getDeviceId() {
		return deviceId;
	}
	
	/**
	 * Gets the node ID of the controller.
	 * @return the deviceId
	 */
	public int getOwnNodeId() {
		return ownNodeId;
	}

	/**
	 * Gets the device type of the controller.
	 * @return the device type
	 */
	public ZigbeeDeviceType getControllerType() {
		return controllerType;
	}

	/**
	 * Gets the networks SUC controller ID.
	 * @return the device id of the SUC, or 0 if none exists
	 */
	public int getSucId() {
		return sucID;
	}
	
	/**
	 * Gets the node object using it's node ID as key.
	 * Returns null if the node is not found
	 * @param nodeId the Node ID of the node to get.
	 * @return node object
	 */
	public ZigBeeApiContext getZigbeeApiContext() {
		return this.zigApiContext;
	}
	
	/**
	 * Gets the node object using it's node ID as key.
	 * Returns null if the node is not found
	 * @param nodeId the Node ID of the node to get.
	 * @return node object
	 */
	public Device getDevice(String EndpoindId) {
		return this.zigbeeDevices.get(EndpoindId);
	}

	/**
	 * Gets the node object using it's node ID as key.
	 * Returns null if the node is not found
	 * @param nodeId the Node ID of the node to get.
	 * @return node object
	 */
	public ZigbeeNode getNode(long nodeId) {
		return this.zigbeeNodes.get(nodeId);
	}
	
	/**
	 * Gets the node list
	 * @return
	 */
	public Collection<ZigbeeNode> getNodes() {
		return this.zigbeeNodes.values();
	}

	/**
	 * Indicates a working connection to the
	 * Zigbee controller stick and initialization complete
	 * @return isConnected;
	 */
	public boolean isConnected() {
		return isConnected && initializationComplete;
	}
	
	/**
	 * Gets the number of Start Of Frames received.
	 * @return the sOFCount
	 */
	public int getSOFCount() {
		return SOFCount;
	}

	/**
	 * Gets the number of Canceled Frames received.
	 * @return the cANCount
	 */
	public int getCANCount() {
		return CANCount;
	}

	/**
	 * Gets the number of Not Acknowledged Frames received.
	 * @return the nAKCount
	 */
	public int getNAKCount() {
		return NAKCount;
	}

	/**
	 * Gets the number of Acknowledged Frames received.
	 * @return the aCKCount
	 */
	public int getACKCount() {
		return ACKCount;
	}

	/**
	 * Returns the number of Out of Order frames received.
	 * @return the oOFCount
	 */
	public int getOOFCount() {
		return OOFCount;
	}
	
	/**
	 * Returns the number of Time-Outs while sending.
	 * @return the oOFCount
	 */
	public int getTimeOutCount() {
		return timeOutCount.get();
	}
	
	// Nested classes and enumerations
	
	/**
	 * Zigbee controller Send Thread. Takes care of sending all messages.
	 * It uses a semaphore to synchronize communication with the receiving thread.
	 * @author Jan-Willem Spuij
	 * @since 1.3.0
	 */
	private class ZigbeeSendThread extends Thread {
	
		private final Logger logger = LoggerFactory.getLogger(ZigbeeSendThread.class);
		
		/**
    	 * Processes incoming message and notifies event handlers.
    	 * @param buffer the buffer to process.
    	 */
    	private void processIncomingMessage(byte[] buffer, SocketMessage lastSentMessage) {
    		SocketMessage incomingMessage = new SocketMessage(buffer);
    		
    		
    		handleIncomingMessage(incomingMessage, lastSentMessage);
        }

		/**
		 * Run method. Runs the actual sending process.
		 */
		@Override
		public void run() {
			logger.debug("Starting Zigbee send thread");
			while (!interrupted()) {
				
				try {
					lastSentMessage = sendQueue.take();
					logger.debug("Took message from queue for sending. Queue length = {}", sendQueue.size());
				} catch (InterruptedException e1) {
					break;
				}
				
				if (lastSentMessage == null)
					continue;
				
				// Send the message to the controller
				//byte[] buffer = lastSentMessage.getMessageBuffer();
				//logger.debug("Sending Message = " + SocketMessage.bb2hex(buffer));
				lastMessageStartTime = System.currentTimeMillis();
				/*NwkGetDeviceListReq req = Nwkmgr.NwkGetDeviceListReq.newBuilder()
						.setCmdId(nwkMgrCmdId_t.NWK_GET_DEVICE_LIST_REQ).build();
				System.out.println(req.getCmdId());
				System.out.println(req.getSerializedSize());
				byte[] bA = req.toByteArray();
				System.out.println(bA.length);
				for (int i = 0; i < bA.length; i++) {
					System.out.print(bA[i] + ":");
				}
				byte[] reqByteArray = req.toByteArray();
				byte header_len_0 = (byte) (reqByteArray.length);
				byte header_len_1 = (byte) (reqByteArray.length >> 8);
				byte subsystem = 18;
//				byte cmd_id = (byte) nwkMgrCmdId_t.NWK_GET_LOCAL_DEVICE_INFO_REQ
//						.getNumber();
				byte cmd_id = (byte) nwkMgrCmdId_t.NWK_GET_DEVICE_LIST_REQ
						.getNumber();
				ByteBuffer bb = ByteBuffer.allocate(4 + reqByteArray.length);
				bb.put(header_len_0);
				bb.put(header_len_1);
				bb.put(subsystem);
				bb.put(cmd_id);
				bb.put(reqByteArray);
				System.out.println("start socket message send...........................");
				Gateway.gwAddressStruct_t dstAddress = Gateway.gwAddressStruct_t
						.newBuilder().setAddressType(gwAddressType_t.UNICAST)
						.setIeeeAddr(5149013021094902L).setEndpointId(8).build();
				// cmdId, dstAddress, state
				Gateway.DevSetOnOffStateReq req = Gateway.DevSetOnOffStateReq
						.newBuilder().setCmdId(gwCmdId_t.DEV_SET_ONOFF_STATE_REQ)
						.setDstAddress(dstAddress).setState(gwOnOffState_t.ON_STATE).build();
				ZigbeeTcpPacket gwPkt = new ZigbeeTcpPacket(
						(byte) zStackGwSysId_t.RPC_SYS_PB_GW.getNumber(), (byte) req
								.getCmdId().getNumber(), req);*/
				
				try {
					//socketOut.write(bb);
					//socketOut.write(gwPkt.toByteArray());
					socketOut = socketClient.getOutputStream();
					socketOut.write(lastSentMessage.getMessagePayload());
					socketOut.flush();					
					socketClient.shutdownOutput();
					
					// Receive message from Server
					socketIn = socketClient.getInputStream();
					byte[] buffer = new byte[2048];
					int len = socketIn.read(buffer);
					processIncomingMessage(buffer, lastSentMessage);

				} catch (IOException e) {
					logger.error("Got I/O exception {} during sending. exiting thread.", e.getLocalizedMessage());
					break;
				}
				
				/*
				// If this message is a data packet to a node
				// then make sure the node is not a battery device.
				// If it's a battery device, it needs to be awake, or we queue the frame until it is.
				if (lastSentMessage.getMessageClass() == SerialMessageClass.SendData) {
					ZigbeeNode node = getNode(lastSentMessage.getMessageNode());
					
					if (node != null && !node.isListening() && !node.isFrequentlyListening() && lastSentMessage.getPriority() != SerialMessagePriority.Low) {
						ZigbeeWakeUpCommandClass wakeUpCommandClass = (ZigbeeWakeUpCommandClass)node.getCommandClass(CommandClass.WAKE_UP);

						// If it's a battery operated device, check if it's awake or place in wake-up queue.
						if (wakeUpCommandClass != null && !wakeUpCommandClass.processOutgoingWakeupMessage(lastSentMessage)) {
							continue;
						}
					}
				}
				
				// Clear the semaphore used to acknowledge the response.
				transactionCompleted.drainPermits();
				
				// Send the message to the controller
				byte[] buffer = lastSentMessage.getMessageBuffer();
				logger.debug("Sending Message = " + SerialMessage.bb2hex(buffer));
				lastMessageStartTime = System.currentTimeMillis();
				try {
					synchronized (serialPort.getOutputStream()) {
						serialPort.getOutputStream().write(buffer);
						serialPort.getOutputStream().flush();
					}
				} catch (IOException e) {
					logger.error("Got I/O exception {} during sending. exiting thread.", e.getLocalizedMessage());
					break;
				}
				
				// Now wait for the response...
				try {
					if (!transactionCompleted.tryAcquire(1, ZigbeeResponseTimeout, TimeUnit.MILLISECONDS)) {
						timeOutCount.incrementAndGet();
						if (lastSentMessage.getMessageClass() == SerialMessageClass.SendData) {
							
							buffer = new SerialMessage(SerialMessageClass.SendDataAbort, SerialMessageType.Request, SerialMessageClass.SendData, SerialMessagePriority.High).getMessageBuffer();
							logger.debug("Sending Message = " + SerialMessage.bb2hex(buffer));
							try {
								synchronized (serialPort.getOutputStream()) {
									serialPort.getOutputStream().write(buffer);
									serialPort.getOutputStream().flush();
								}
							} catch (IOException e) {
								logger.error("Got I/O exception {} during sending. exiting thread.", e.getLocalizedMessage());
								break;
							}
						}

						if (--lastSentMessage.attempts >= 0) {
							logger.error("NODE {}: Timeout while sending message. Requeueing", lastSentMessage.getMessageNode());
							if (lastSentMessage.getMessageClass() == SerialMessageClass.SendData)
								handleFailedSendDataRequest(lastSentMessage);
							else
								enqueue(lastSentMessage);
						} else
						{
							logger.warn("NODE {}: Discarding message: {}", lastSentMessage.getMessageNode(), lastSentMessage.toString());
						}
						continue;
					}
					long responseTime = System.currentTimeMillis() - lastMessageStartTime;
					if(responseTime > longestResponseTime)
						longestResponseTime = responseTime;
					logger.debug("Response processed after {}ms/{}ms.", responseTime, longestResponseTime);
					logger.trace("Acquired. Transaction completed permit count -> {}", transactionCompleted.availablePermits());
				} catch (InterruptedException e) {
					break;
				}
				*/
			}
			logger.debug("Stopped Zigbee send thread");
		}
	}

	/**
	 * Zigbee controller Receive Thread. Takes care of receiving all messages.
	 * It uses a semaphore to synchronize communication with the sending thread.
	 * @author Jan-Willem Spuij
	 * @since 1.3.0
	 */	
	private class ZigbeeReceiveThread extends Thread {
		
		private static final int SOF = 0x01;
		private static final int ACK = 0x06;
		private static final int NAK = 0x15;
		private static final int CAN = 0x18;
		
		private final Logger logger = LoggerFactory.getLogger(ZigbeeReceiveThread.class);

		/**
    	 * Sends 1 byte frame response.
    	 * @param response the response code to send.
    	 */
		private void sendResponse(int response) {
//			try {
//				synchronized (serialPort.getOutputStream()) {
//					serialPort.getOutputStream().write(response);
//					serialPort.getOutputStream().flush();
//				}
//			} catch (IOException e) {
//				logger.error(e.getMessage());
//			}
		}
		
		/**
    	 * Processes incoming message and notifies event handlers.
    	 * @param buffer the buffer to process.
    	 */
    	private void processIncomingMessage(byte[] buffer) {
    		SerialMessage serialMessage = new SerialMessage(buffer);
    		if (serialMessage.isValid) {
    			logger.trace("Message is valid, sending ACK");
    			sendResponse(ACK);
    		} else {
    			logger.error("Message is not valid, discarding");
    			return;
    		}
    		
    		//handleIncomingMessage(serialMessage);
        }
		
		/**
		 * Run method. Runs the actual receiving process.
		 */
		@Override
		public void run() {
			logger.debug("Starting Zigbee receive thread");

			// Send a NAK to resynchronise communications
			sendResponse(NAK);
			
			// If we want to do a soft reset on the serial interface, do it here.
			// It seems there's no response to this message, so sending it through
			// 'normal' channels will cause a timeout.
//			try {
//				synchronized (serialPort.getOutputStream()) {
//					SerialMessage resetMsg = new SerialApiSoftResetMessageClass().doRequest();
//					byte[] buffer = resetMsg.getMessageBuffer();
//
//					serialPort.getOutputStream().write(buffer);
//					serialPort.getOutputStream().flush();
//				}
//			} catch (IOException e) {
//				logger.error(e.getMessage());
//			}
//
//			while (!interrupted()) {
//				int nextByte;
//				
//				try {
//					nextByte = serialPort.getInputStream().read();
//					
//					if (nextByte == -1)
//						continue;
//					
//				} catch (IOException e) {
//					logger.error("Got I/O exception {} during receiving. exiting thread.", e.getLocalizedMessage());
//					break;
//				}
//				
//				switch (nextByte) {
//					case SOF:
//						int messageLength;
//						
//						try {
//							messageLength = serialPort.getInputStream().read();
//							
//						} catch (IOException e) {
//							logger.error("Got I/O exception {} during receiving. exiting thread.", e.getLocalizedMessage());
//							break;
//						}
//						
//						byte[] buffer = new byte[messageLength + 2];
//						buffer[0] = SOF;
//						buffer[1] = (byte)messageLength;
//						int total = 0;
//						
//						while (total < messageLength) {
//							try {
//								int read = serialPort.getInputStream().read(buffer, total + 2, messageLength - total); 
//								total += (read > 0 ? read : 0);
//							} catch (IOException e) {
//								logger.error("Got I/O exception {} during receiving. exiting thread.", e.getLocalizedMessage());
//								return;
//							}
//						}
//						
//						logger.trace("Reading message finished" );
//						logger.debug("Receive Message = {}", SerialMessage.bb2hex(buffer));
//						processIncomingMessage(buffer);
//						SOFCount++;
//						break;
//					case ACK:
//    					logger.trace("Received ACK");
//						ACKCount++;
//						break;
//					case NAK:
//    					logger.error("Message not acklowledged by controller (NAK), discarding");
//    					transactionCompleted.release();
//    					logger.trace("Released. Transaction completed permit count -> {}", transactionCompleted.availablePermits());
//						NAKCount++;
//						break;
//					case CAN:
//    					logger.error("Message cancelled by controller (CAN), resending");
//						try {
//							Thread.sleep(100);
//						} catch (InterruptedException e) {
//							break;
//						}
//    					enqueue(lastSentMessage);
//    					transactionCompleted.release();
//    					logger.trace("Released. Transaction completed permit count -> {}", transactionCompleted.availablePermits());
//						CANCount++;
//						break;
//					default:
//						logger.warn(String.format("Out of Frame flow. Got 0x%02X. Sending NAK.", nextByte));
//    					sendResponse(NAK);
//    					OOFCount++;
//    					break;
//				}
//			}
//			logger.debug("Stopped Zigbee receive thread");
		}
	}

	/**
	 * WatchDogTimerTask class. Acts as a watch dog and
	 * checks the serial threads to see whether they are
	 * still running.  
	 * @author Jan-Willem Spuij
	 * @since 1.3.0
	 */
	private class WatchDogTimerTask extends TimerTask {
		
		private final Logger logger = LoggerFactory.getLogger(WatchDogTimerTask.class);
		private final String serialPortName;
		
		/**
		 * Creates a new instance of the WatchDogTimerTask class.
		 * @param serialPortName the serial port name to reconnect to
		 * in case the serial threads have died.
		 */
		public WatchDogTimerTask(String serialPortName) {
			this.serialPortName = serialPortName;
		}
		
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void run() {
			logger.trace("Watchdog: Checking Serial threads");
			if ((receiveThread != null && !receiveThread.isAlive()) ||
					(sendThread != null && !sendThread.isAlive()))
			{
				logger.warn("Threads not alive, respawning");
				disconnect();
				try {
					connect(serialPortName);
				} catch (SerialInterfaceException e) {
					logger.error("unable to restart Serial threads: {}", e.getLocalizedMessage());
				}
			}
		}
	}
	

}
