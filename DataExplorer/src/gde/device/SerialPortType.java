/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import osde.utils.XMLUtils;

/**
 * SerialPortType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 * @author Winfried Brügmann
 */
public class SerialPortType {
	private String						port;
	private final int					baudeRate;
	private final int					dataBits;
	private final int					parity;
	private final int					stopBits;
	private final int					flowControlMode;
	private final boolean			isRTS;
	private final boolean			isDTS;
	private final TimeOutType	timeOut;
	private DataBlockType			dataBlock;

	private Element						domElement;

	public final static int		PARITY_NONE							= 0;
	public final static int		PARITY_ODD							= 1;
	public final static int		PARITY_EVEN							= 2;
	public final static int		PARITY_MARK							= 3;
	public final static int		PARITY_SPACE						= 4;

	public final static int		FLOWCONTROL_NONE				= 0;
	public final static int		FLOWCONTROL_RTSCTS_IN		= 1;
	public final static int		FLOWCONTROL_RTSCTS_OUT	= 2;
	public final static int		FLOWCONTROL_XONXOFF_IN	= 4;
	public final static int		FLOWCONTROL_XONXOFF_OUT	= 8;

	public final static int		STOPBITS_1							= 1;
	public final static int		STOPBITS_2							= 2;
	public final static int		STOPBITS_1_5						= 3;
	
	/**
	 * constructs a SerialPort class using a XML DOM element
	 * @param element (DOM)
	 */
	public SerialPortType(Element element) {
		this.domElement = element;
		this.port = XMLUtils.getTextValue(element, "port");
		this.baudeRate = XMLUtils.getIntValue(element, "baudeRate");
		this.dataBits = XMLUtils.getIntValue(element, "dataBits");
		this.parity = getParity(XMLUtils.getTextValue(element, "parity"));
		this.stopBits = getStopBits(XMLUtils.getTextValue(element, "stopBits"));
		this.flowControlMode = getFlowControlMode(XMLUtils.getTextValue(element, "flowControlMode"));
		this.isRTS = XMLUtils.getBooleanValue(element, "isRTS");
		this.isDTS = XMLUtils.getBooleanValue(element, "isDTS");

		TimeOutType to = new TimeOutType(); // optional element
		NodeList timeOutNodeList = element.getElementsByTagName("TimeOut");
		if (timeOutNodeList != null && timeOutNodeList.getLength() > 0) {
			for (int i = 0; i < timeOutNodeList.getLength(); i++) {
				Element el = (Element) timeOutNodeList.item(i);
				to = new TimeOutType(el);
			}
		}
		this.timeOut = to;

		DataBlockType db = new DataBlockType(); // optional element
		NodeList dataBlockNodeList = element.getElementsByTagName("DataBlock");
		if (dataBlockNodeList != null && dataBlockNodeList.getLength() > 0) {
			for (int i = 0; i < dataBlockNodeList.getLength(); i++) {
				//get the elements Device, SerialPort, TimeBase, Channel
				Element el = (Element) dataBlockNodeList.item(i);
				// get the Device object
				db = new DataBlockType(el);
			}
		}
		this.dataBlock = db;

	}

	public SerialPortType(String port, int baudeRate, int dataBits, int parity, int stopBits, int flowControlMode, boolean isRTS, boolean isDTS) {
		this.port = port;
		this.baudeRate = baudeRate;
		this.dataBits = dataBits;
		this.parity = parity;
		this.stopBits = stopBits;
		this.flowControlMode = flowControlMode;
		this.isRTS = isRTS;
		this.isDTS = isDTS;
		this.timeOut = new TimeOutType();
		this.dataBlock = new DataBlockType();
	}

	public SerialPortType(String port, int baudeRate, int dataBits, int parity, int stopBits, int flowControlMode, boolean isRTS, boolean isDTS, DataBlockType dataBlock) {
		this.port = port;
		this.baudeRate = baudeRate;
		this.dataBits = dataBits;
		this.parity = parity;
		this.stopBits = stopBits;
		this.flowControlMode = flowControlMode;
		this.isRTS = isRTS;
		this.isDTS = isDTS;
		this.timeOut = new TimeOutType();
		this.dataBlock = dataBlock;
	}

	public SerialPortType(String port, int baudeRate, int dataBits, int parity, int stopBits, int flowControlMode, boolean isRTS, boolean isDTS, DataBlockType dataBlock, TimeOutType timeOut) {
		this.port = port;
		this.baudeRate = baudeRate;
		this.dataBits = dataBits;
		this.parity = parity;
		this.stopBits = stopBits;
		this.flowControlMode = flowControlMode;
		this.isRTS = isRTS;
		this.isDTS = isDTS;
		this.timeOut = timeOut;
		this.dataBlock = dataBlock;
	}

	public String toString() {
		return String.format("<SerialPort> port = %s, baudeRate = %d, dataBits = %d, parity = %d, stopBits = %d, flowControlMode = %d, isRTS = %s, isDTS = %s, %s, %s", port, baudeRate, dataBits, parity,
				stopBits, flowControlMode, isRTS, isDTS, dataBlock.toString(), timeOut.toString());
	}

	/**
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(String port) {
		XMLUtils.setStringValue(domElement, "port", port);
		this.port = port;
	}

	/**
	 * @return the dataBlock
	 */
	public DataBlockType getDataBlock() {
		return dataBlock;
	}

	/**
	 * @param dataBlock the dataBlock to set
	 */
	public void setDataBlock(DataBlockType dataBlock) {
		this.dataBlock = dataBlock;
	}

	/**
	 * @return the baudeRate
	 */
	public int getBaudeRate() {
		return baudeRate;
	}

	/**
	 * @return the dataBits
	 */
	public int getDataBits() {
		return dataBits;
	}

	/**
	 * @return the parity
	 */
	public int getParity() {
		return parity;
	}

	/**
	 * @return the stopBits
	 */
	public int getStopBits() {
		return stopBits;
	}

	/**
	 * @return the flowControlMode
	 */
	public int getFlowControlMode() {
		return flowControlMode;
	}

	/**
	 * @return the isRTS
	 */
	public boolean isRTS() {
		return isRTS;
	}

	/**
	 * @return the isDTS
	 */
	public boolean isDTS() {
		return isDTS;
	}

	/**
	 * @return the timeOut
	 */
	public TimeOutType getTimeOut() {
		return timeOut;
	}
	
	private int getParity(String key) {
		int result = PARITY_NONE;
		if (key.equals("PARITY_NONE")) result = PARITY_NONE;
		else if (key.equals("PARITY_ODD")) result = PARITY_ODD;
		else if (key.equals("PARITY_EVEN")) result = PARITY_EVEN;
		else if (key.equals("PARITY_MARK")) result = PARITY_MARK;
		else if (key.equals("PARITY_SPACE")) result = PARITY_SPACE;
		return result;
	}
	
	private int getStopBits( String key) {
		int result = STOPBITS_1;
		if (key.equals("STOPBITS_1")) result = STOPBITS_1;
		else if (key.equals("STOPBITS_2")) result = STOPBITS_2;
		else if (key.equals("STOPBITS_1_5")) result = STOPBITS_1_5;
		return result;
	}
	
	private int getFlowControlMode(String key) {
		int result = FLOWCONTROL_NONE;
		if (key.equals("FLOWCONTROL_NONE")) result = FLOWCONTROL_NONE;
		else if (key.equals("FLOWCONTROL_RTSCTS_IN")) result = FLOWCONTROL_RTSCTS_IN;
		else if (key.equals("FLOWCONTROL_RTSCTS_OUT")) result = FLOWCONTROL_RTSCTS_OUT;
		else if (key.equals("FLOWCONTROL_XONXOFF_IN")) result = FLOWCONTROL_XONXOFF_IN;
		else if (key.equals("FLOWCONTROL_XONXOFF_OUT")) result = FLOWCONTROL_XONXOFF_OUT;
		return result;
	}
}
