/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.config;

import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * Implements the top level class for the product file
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
public class ZigbeeDbProductFile {
	public String Model;
	public Integer Endpoints;
	@XStreamImplicit
	public List<ZigbeeDbLabel> Label;

	public ZigbeeDbCommandClassList CommandClasses;

	public ZigbeeDbConfiguration Configuration;
	public ZigbeeDbAssociation Associations;

	List<ZigbeeDbConfigurationParameter> getConfiguration() {
		return Configuration.Parameter;
	}
	
	List<ZigbeeDbAssociationGroup> getAssociations() {
		return Associations.Group;
	}

	class ZigbeeDbCommandClassList {
		@XStreamImplicit
		public List<ZigbeeDbCommandClass> Class;		
	}
	
	class ZigbeeDbConfiguration {
		@XStreamImplicit
		public List<ZigbeeDbConfigurationParameter> Parameter;		
	}

	class ZigbeeDbAssociation {
		@XStreamImplicit
		List<ZigbeeDbAssociationGroup> Group;
	}
}
	
