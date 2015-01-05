package com.cc.test;

import java.awt.List;

import org.bubblecloud.zigbee.ZigBeeApi;
import org.bubblecloud.zigbee.api.Device;
import org.bubblecloud.zigbee.api.ZigBeeDeviceException;
import org.bubblecloud.zigbee.api.cluster.general.OnOff;
import org.bubblecloud.zigbee.api.cluster.impl.api.core.Reporter;
import org.bubblecloud.zigbee.network.model.NetworkMode;
import org.bubblecloud.zigbee.network.packet.ZToolAddress16;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_IEEE_ADDR_REQ;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_IEEE_ADDR_RSP;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_MGMT_PERMIT_JOIN_REQ;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_NODE_DESC_REQ;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_NODE_DESC_RSP;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_SIMPLE_DESC_REQ;
import org.bubblecloud.zigbee.network.packet.zdo.ZDO_SIMPLE_DESC_RSP;
import org.bubblecloud.zigbee.network.serial.ZigBeeNetworkManagerSerialImpl;
import org.slf4j.LoggerFactory;

public class Zigbee4javaTest {
	public static void main(final String[] args) {
		
//		System.setProperty(ZigBeeNetworkManagerSerialImpl.RESEND_TIMEOUT_KEY, "10000");
		
		ZigBeeApi zigbeeApi = new ZigBeeApi("COM8", 0x8F20, 11, false);
//		zigbeeApi.getZigBeeNetworkManager().setZigBeeNodeMode(NetworkMode.Coordinator);
		zigbeeApi.startup();
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(zigbeeApi.getZigBeeNetworkManager()
				.getCurrentChannel()
				+ ", "
				+ zigbeeApi.getZigBeeNetworkManager().getCurrentPanId()
				+ ", "
				+ zigbeeApi.getZigBeeNetworkManager().getCurrentState()
				+ ", "
				+ zigbeeApi.getZigBeeNetworkManager().getIEEEAddress());


//		00:12:4B:00:01:45:00:B8, ZToolAddress16.BROADCAST
		ZToolAddress16 corNwkAddress = new ZToolAddress16(0xFF, 0xFF);
		ZDO_MGMT_PERMIT_JOIN_REQ request = new ZDO_MGMT_PERMIT_JOIN_REQ(
				corNwkAddress, 60, 0);
		zigbeeApi.getZigBeeNetworkManager().sendPermitJoinRequest(request);

		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ZDO_NODE_DESC_RSP result = zigbeeApi.getZigBeeNetworkManager()
				.sendZDONodeDescriptionRequest(
						new ZDO_NODE_DESC_REQ((short) 0x4F3E));
		System.out.println(result);
		
		ZDO_SIMPLE_DESC_RSP result1 = zigbeeApi.getZigBeeNetworkManager()
				.sendZDOSimpleDescriptionRequest(
						new ZDO_SIMPLE_DESC_REQ((short) 0x4F3E, (byte) 8));
		System.out.println(result1);

		// 0x796F, 31087
		ZDO_IEEE_ADDR_RSP result2 = zigbeeApi
				.getZigBeeNetworkManager()
				.sendZDOIEEEAddressRequest(
						new ZDO_IEEE_ADDR_REQ(
								(short) 0x4F3E,
								ZDO_IEEE_ADDR_REQ.REQ_TYPE.SINGLE_DEVICE_RESPONSE,
								(byte) 0));
		System.out.println(result2);
		java.util.List<Device> l = zigbeeApi.getDevices();
		System.out.println(l.size());
//		zigbeeApi.getZigBeeNetwork().addNode()
//		zigbeeApi.getZigBeeApiContext().addDevice(device);
		Device lamp = zigbeeApi.getZigBeeApiContext().getDevice(
				"00:12:4B:00:03:A5:CB:1F/8");

		
//		try {
//			OnOff onOff = lamp.getCluster(OnOff.class);
//			onOff.on();
//		} catch (ZigBeeDeviceException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// int onOffAttributeIndex = 0;
		// Reporter reporter =
		// onOff.getAttribute(onOffAttributeIndex).getReporter();
		// reporter.addReportListener(reportListener);
		
		zigbeeApi.shutdown();
	}

}
