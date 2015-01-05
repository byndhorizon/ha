/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.config;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;

/**
 * Implements a label class for the XML product database
 * @author Chris Jackson
 * @since 1.4.0
 *
 */
@XStreamConverter(value=ToAttributedValueConverter.class, strings={"Label"})
public class ZigbeeDbLabel {
	@XStreamAlias("lang")
	String Language;
	
	String Label;
}
