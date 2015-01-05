/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.initialization;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.openhab.binding.zigbee.internal.ZigbeeActivator;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeDeviceClass;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeEndpoint;
import org.openhab.binding.zigbee.internal.protocol.ZigbeeNode;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass;
import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeCommandClass.CommandClass;
//import org.openhab.binding.zigbee.internal.protocol.commandclass.ZigbeeMeterCommandClass.MeterScale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * ZigbeeNodeSerializer class. Serializes nodes to XML and back again.
 * 
 * @author Jan-Willem Spuij
 * @since 1.4.0
 */
public class ZigbeeNodeSerializer {

	private static final Logger logger = LoggerFactory.getLogger(ZigbeeNodeSerializer.class);
	private static final String FOLDER_NAME = "etc/zigbee";
	private final XStream stream = new XStream(new StaxDriver());
	private final String versionedFolderName;

	/**
	 * Constructor. Creates a new instance of the {@link ZigbeeNodeSerializer}
	 * class.
	 */
	public ZigbeeNodeSerializer() {
		logger.trace("Initializing ZigbeeNodeSerializer.");
		this.versionedFolderName = String.format("%s/%d.%d/", FOLDER_NAME, 
				ZigbeeActivator.getVersion().getMajor(), ZigbeeActivator.getVersion().getMinor());

		File folder = new File(versionedFolderName);
		// create path for serialization.
		if (!folder.exists()) {
			logger.debug("Creating directory {}", versionedFolderName);
			folder.mkdirs();
		}
		stream.processAnnotations(ZigbeeNode.class);
		stream.processAnnotations(ZigbeeEndpoint.class);
		stream.processAnnotations(ZigbeeDeviceClass.class);
		stream.processAnnotations(ZigbeeCommandClass.class);
		stream.processAnnotations(CommandClass.class);
		for (CommandClass commandClass : CommandClass.values()) {
			Class<? extends ZigbeeCommandClass> cc = commandClass.getCommandClassClass();

			if (cc == null) {
				continue;
			}

			stream.processAnnotations(cc);
			for (Class<?> inner : cc.getDeclaredClasses()) {
				stream.processAnnotations(inner);
			}
		}
		//stream.processAnnotations(MeterScale.class);
		logger.trace("Initialized ZigbeeNodeSerializer.");
	}

	/**
	 * Serializes an XML tree of a {@link ZigbeeNode}
	 * 
	 * @param node
	 *            the node to serialize
	 */
	public void SerializeNode(ZigbeeNode node) {
		synchronized (stream) {
			File file = new File(this.versionedFolderName, String.format("node%d.xml", node.getNodeId()));
			BufferedWriter writer = null;

			logger.debug("NODE {}: Serializing to file {}", node.getNodeId(), file.getPath());

			try {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
				stream.marshal(node, new PrettyPrintWriter(writer));
				writer.flush();
			} catch (IOException e) {
				logger.error("NODE {}: There was an error writing the node config to a file: {}", node.getNodeId(), e.getMessage());
			} finally {
				if (writer != null)
					try {
						writer.close();
					} catch (IOException e) {
					}
			}
		}
	}

	/**
	 * Deserializes an XML tree of a {@link ZigbeeNode}
	 * 
	 * @param nodeId
	 *            the number of the node to deserialize
	 * @return returns the Node or null in case Serialization failed.
	 */
	public ZigbeeNode DeserializeNode(long nodeId) {
		synchronized (stream) {
			File file = new File(this.versionedFolderName, String.format("node%d.xml", nodeId));
			BufferedReader reader = null;

			logger.debug("NODE {}: Deserializing from file {}", nodeId, file.getPath());

			if (!file.exists()) {
				logger.debug("NODE {}: Deserializing from file {} failed, file does not exist.", nodeId, file.getPath());
				return null;
			}

			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				return (ZigbeeNode)stream.fromXML(reader);
			} catch (IOException e) {
				logger.error("NODE {}: There was an error reading the node config from a file: {}", nodeId, e.getMessage());
			} finally {
				if (reader != null)
					try {
						reader.close();
					} catch (IOException e) {
					}
			}
			return null;
		}
	}
	
	/**
	 * Deletes the persistence store for the specified node.
	 * 
	 * @param nodeId The node ID to remove
	 * @return true if the file was deleted
	 */
	public boolean DeleteNode(int nodeId) {
		synchronized (stream) {
			File file = new File(this.versionedFolderName, String.format("node%d.xml", nodeId));

			return file.delete();
		}
	}
}
