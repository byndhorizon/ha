package org.openhab.binding.zigbee.internal.protocol;

import java.util.HashMap;

import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.cluster.Cluster;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;

public class ZigbeeDeviceTypes {

	private static HashMap<String, Class<? extends Cluster>> clusterTypesMap = null;

	/*
	public ZigbeeDeviceTypes() {
		clusterTypesMap.put("SWITCH", OnOff.class);
	}
	*/
	
	public static Class<? extends Cluster> getTypeDispatcher(String type) {
		if (clusterTypesMap==null) {
			clusterTypesMap.put("SWITCH", OnOff.class);
		}
		
		return clusterTypesMap.get(type);
	}
	
	public Class<? extends Cluster> getCluster(String type) {
		return clusterTypesMap.get(type);
	}
}

