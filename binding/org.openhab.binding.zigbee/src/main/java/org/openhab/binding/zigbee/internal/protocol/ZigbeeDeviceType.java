package org.openhab.binding.zigbee.internal.protocol;

/**
 * enum defining the different Zigbee node types
 * 
 * @author Chris Jackson
 * @since 1.5
 */
public enum ZigbeeDeviceType {
	UNKNOWN("Unknown"), 
	SLAVE("Slave"), 
	PRIMARY("Primary Controller"), 
	SECONDARY("Secondary Controller"),
	SUC("Static Update Controller");

	private ZigbeeDeviceType(final String text) {
		this.text = text;
	}

	private final String text;

	public String getLabel() {
		return text;
	}

	public static ZigbeeDeviceType fromString(String text) {
		if (text != null) {
			for (ZigbeeDeviceType c : ZigbeeDeviceType.values()) {
				if (text.equalsIgnoreCase(c.name())) {
					return c;
				}
			}
		}
		return null;
	}
}
