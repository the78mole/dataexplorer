/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import gde.comm.DeviceCommPort;
import gde.comm.IDeviceCommPort;
import gde.device.IDevice;
import gde.device.InputTypes;
import gde.io.DataParser;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

import java.io.IOException;
import java.util.logging.Logger;

import javax.usb.UsbInterface;

/**
 * @author Winfried Brügmann
 */
public class iChargerUsbPort extends DeviceCommPort implements IDeviceCommPort {
	final static String $CLASS_NAME = iChargerUsbPort.class.getName();
	final static Logger	log	= Logger.getLogger($CLASS_NAME);
		
  // The communication timeout in milliseconds. */
  protected static final int TIMEOUT = 1000;
  
  protected final byte interfaceId;
  protected final byte endpointIn;
  protected final byte endpointOut;

	final int  dataSize;
	
	/**
	 * constructor of default implementation
	 * @param currentDeviceConfig - required by super class to initialize the serial communication port
	 * @param currentApplication - may be used to reflect serial receive,transmit on/off status or overall status by progress bar 
	 */
	public iChargerUsbPort(IDevice currentDevice, DataExplorer currentApplication) {
		super(currentDevice, currentApplication);
		this.dataSize = this.deviceConfig.getDataBlockSize(InputTypes.SERIAL_IO);
		this.interfaceId = this.device.getUsbInterface();
		this.endpointIn = this.device.getUsbEndpointIn();
		this.endpointOut = this.device.getUsbEndpointOut();
	}

	/**
	 * method to gather data from device, implementation is individual for device
	 * @return byte array containing gathered data - this can individual specified per device
	 * @throws IOException
	 */
	public synchronized byte[] getData(final UsbInterface iface) throws Exception {
		final String $METHOD_NAME = "getData"; //$NON-NLS-1$
		byte[] data = new byte[Math.abs(this.dataSize)];

		try {
			this.read(iface, this.endpointOut, data, TIMEOUT);
			
			if (log.isLoggable(Level.FINE)) {
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, StringHelper.byte2Hex2CharString(data, data.length));
				log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, DataParser.parse2Int(data, 3)/1000/60 + " min " + (DataParser.parse2Int(data, 3)/1000)%60 + " sec run time");
			}
		}
		catch (Exception e) {
				log.logp(Level.WARNING, $CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
		return data;
	}
}