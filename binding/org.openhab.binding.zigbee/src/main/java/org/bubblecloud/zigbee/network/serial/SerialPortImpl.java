package org.bubblecloud.zigbee.network.serial;

import j.extensions.comm.SerialComm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The serial-comm implementation of SerialPort.
 *
 * @author <a href="mailto:tommi.s.e.laukkanen@gmail.com">Tommi S.E. Laukkanen</a>
 */
public class SerialPortImpl implements SerialPort {
    /**
     * The logger.
     */
    private final static Logger logger = LoggerFactory.getLogger(SerialPortImpl.class);
    /**
     * The serial port.
     */
    private SerialComm serialPort;
    /**
     * The serial port input stream.
     */
    private InputStream inputStream;
    /**
     * The serial port output stream.
     */
    private OutputStream outputStream;

    @Override
    public boolean open(final String port, final int baudRate) {
    	System.out.println("serialPortIpml 0000000000000000");
        try {
            openSerialPort(port, 0, baudRate, 8, SerialComm.ONE_STOP_BIT,
                    SerialComm.NO_PARITY, SerialComm.FLOW_CONTROL_DISABLED);
            System.out.println("serialPortIpml 111111111111111");
            return true;
        } catch (Exception e) {
            logger.error("Error...", e);
            System.out.println("serialPortIpml 222222222222222222");
            return false;
        }
    }

    /**
     * Opens serial port.
     *
     * @param port          the port
     * @param timeoutMillis the timeout in milliseconds
     * @param baudRate      the baud rate
     * @param dataBits      the data bits count
     * @param stopBits      the stop bits count
     * @param parity        the parity
     * @param flowControl   the flow control mode
     */
    private void openSerialPort(String port, int timeoutMillis, int baudRate, int dataBits, int stopBits, int parity, int flowControl) {
        if (serialPort != null) {
            throw new RuntimeException("Serial port '" + serialPort.getSystemPortName()
                    + "' is already startup for this serial comm instance.");
        }
        System.out.println("openSerialPort 111111111111111111111");
        final SerialComm[] ports = SerialComm.getCommPorts();
        logger.trace("Serial ports:");
        System.out.println("openSerialPort 22222222222222222222");
        final Map<String, SerialComm> portMap = new HashMap<String, SerialComm>();
        for (int i = 0; i < ports.length; ++i) {
            logger.trace(i + ") '" + ports[i].getSystemPortName() + "': " + ports[i].getDescriptivePortName());
            System.out.println("openSerialPort 33333333333333333333333333");

            portMap.put(ports[i].getSystemPortName(), ports[i]);
        }
        System.out.println("openSerialPort 444444444444444");
        if (!portMap.containsKey(port)) {
            throw new RuntimeException("Serial port '" + port + "' not found.");
        }
        System.out.println("openSerialPort 55555555555555555");
        serialPort = portMap.get(port);
        logger.info("Opening serial port '" + serialPort.getSystemPortName() + "'.");
        if (!serialPort.openPort()) {
            throw new RuntimeException("Serial port '" + port + "' startup failed.");
        }
        System.out.println("openSerialPort 66666666666666666666");
        serialPort.setComPortTimeouts(SerialComm.TIMEOUT_READ_BLOCKING, timeoutMillis, 0);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(dataBits);
        serialPort.setNumStopBits(stopBits);
        serialPort.setParity(parity);
        serialPort.setFlowControl(flowControl);
        System.out.println("openSerialPort 77777777777777777777");
        inputStream = serialPort.getInputStream();
        outputStream = serialPort.getOutputStream();
    }

    @Override
    public void close() {
        try {
            if (serialPort != null) {
                while (inputStream.available() > 0) {
                    try {
                        Thread.sleep(100);
                    } catch (final InterruptedException e) {
                        logger.warn("Interrupted while waiting input stream to flush.");
                    }
                }
                inputStream.close();
                outputStream.flush();
                outputStream.close();
                serialPort.closePort();
                logger.info("Serial port '" + serialPort.getSystemPortName() + "' closed.");
                serialPort = null;
                inputStream = null;
                outputStream = null;
            }
        } catch (Exception e) {
            logger.warn("Error closing serial port: '" + serialPort.getSystemPortName() + "'", e);
        }
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }
}
