package osde.device.wb;

import gnu.io.NoSuchPortException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import osde.config.DeviceConfiguration;
import osde.data.RecordSet;
import osde.device.DeviceSerialPort;
import osde.device.IDevice;
import osde.ui.StatusBar;

/**
 * @author Winfried Bruegmann
 *
 */
public class SimulatorSerialPort extends DeviceSerialPort {

	private int			lastRecord	= -1;
	private int	lastVoltage	= 0;
	private int	lastCurrent	= 0;
	private int xBound = 0;

	/**
	 * constructor of default implementation
	 * @param deviceConfig - required by super class to initialize the serial communication port
	 * @param statusBar - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 * @throws NoSuchPortException
	 */
	public SimulatorSerialPort(DeviceConfiguration deviceConfig, StatusBar statusBar) throws NoSuchPortException {
		super(deviceConfig, statusBar);
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @param channel signature if device has more than one or required by device
	 * @return map containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public HashMap<String, Object> getData(byte[] channel, int recordNumber, IDevice dialog) throws IOException {
		if (recordNumber != lastRecord) {
			lastRecord = recordNumber;
			lastVoltage = 0;
			lastCurrent = 0;
			xBound = 0;
		}

		HashMap<String, Object> data = new HashMap<String, Object>();
		Vector<Integer> voltage = new Vector<Integer>();
		Vector<Integer> current = new Vector<Integer>();

		//int xBound = new Double(Math.random() * 1000000).intValue();
		xBound = xBound + 100;
		lastVoltage = xBound / 2;
		int yBound = deviceConfig.getClusterSize();
		for (int i = 0; i < deviceConfig.getClusterSize(); i++) {
			current.add(i * 3000 + lastCurrent);
			voltage.add(getNormalizedSine(i, xBound / 2, yBound) - lastVoltage);
		}
		lastCurrent = yBound * 3000;

		data.put(RecordSet.VOLTAGE, voltage);
		data.put(RecordSet.CURRENT, current);
		return data;
	}

	/**
	 * calculates the sine value
	 * @param x the value along the x-axis
	 * @param halfY the value of the y-axis
	 * @param maxX the width of the x-axis
	 * @return int
	 */
	int getNormalizedSine(int x, int halfY, int maxX) {
		double piDouble = 2 * Math.PI;
		double factor = piDouble / maxX;
		return (int) (Math.sin(x * factor) * halfY + halfY);
	}
}
