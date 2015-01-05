/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zigbee.internal.protocol.commandclass;

/**
 * Interface to implement for all command classes that implement the basic
 * commands like GET/SET value.
 * @author Jan-Willem Spuij
 * @since 1.3.0
 */
public interface ZigbeeBasicCommands extends ZigbeeGetCommands, ZigbeeSetCommands {
}
