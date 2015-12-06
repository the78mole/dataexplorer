package gde.comm;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gnu.io.SerialPort;

import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import javax.usb.UsbClaimException;
import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;

public class DeviceUsbPortImpl implements IDeviceCommPort {
	private final static Logger								log												= Logger.getLogger(DeviceUsbPortImpl.class.getName());

	final protected DeviceConfiguration		deviceConfig;
	final protected DataExplorer					application;
	final Settings												settings;

	boolean																isConnected								= false;

	
	/**
	 * normal constructor to be used within DataExplorer
	 * @param currentDeviceConfig
	 * @param currentApplication
	 */
	public DeviceUsbPortImpl(DeviceConfiguration currentDeviceConfig, DataExplorer currentApplication) {
		this.deviceConfig = currentDeviceConfig;
		this.application = currentApplication;
		this.settings = Settings.getInstance();
	}
	
	/**
	 * constructor for test purpose only, do not use within DataExplorer
	 */
	public DeviceUsbPortImpl() {
		this.deviceConfig = null;
		this.application = null;
		this.settings = null;
	}

	public SerialPort open() throws ApplicationConfigurationException, SerialPortException {
		// Auto-generated method stub
		return null;
	}

	public void close() {
		// Auto-generated method stub

	}

	public byte[] read(byte[] readBuffer, int timeout_msec) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, boolean checkFailedQuery) throws IOException, FailedQueryException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, int stableIndex, int minCountBytes) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public byte[] read(byte[] readBuffer, int timeout_msec, Vector<Long> waitTimes) throws IOException, TimeOutException {
		// Auto-generated method stub
		return null;
	}

	public void write(byte[] writeBuffer) throws IOException {
		// Auto-generated method stub

	}

	public void write(byte[] writeBuffer, long gap_ms) throws IOException {
		// Auto-generated method stub

	}

	public int cleanInputStream() throws IOException {
		// Auto-generated method stub
		return 0;
	}

	public long wait4Bytes(int timeout_msec) throws InterruptedException, TimeOutException, IOException {
		// Auto-generated method stub
		return 0;
	}

	public int wait4Bytes(int numBytes, int timeout_msec) throws IOException {
		// Auto-generated method stub
		return 0;
	}

	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex) throws InterruptedException, TimeOutException, IOException {
		// Auto-generated method stub
		return 0;
	}

	public int waitForStableReceiveBuffer(int expectedBytes, int timeout_msec, int stableIndex, int minCount) throws InterruptedException, TimeOutException, IOException {
		// Auto-generated method stub
		return 0;
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	public int getXferErrors() {
		// Auto-generated method stub
		return 0;
	}

	public void addXferError() {
		// Auto-generated method stub

	}

	public int getTimeoutErrors() {
		// Auto-generated method stub
		return 0;
	}

	public void addTimeoutError() {
		// Auto-generated method stub

	}

	public boolean isMatchAvailablePorts(String newSerialPortStr) {
		// Auto-generated method stub
		return false;
	}

	public int getAvailableBytes() throws IOException {
		// Auto-generated method stub
		return 0;
	}
	
  /**
   * find USB device to be identified by vendor ID and product ID
   * @param vendorId
   * @param productId
   * @return
   * @throws UsbException
   */
	public UsbDevice findUsbDevice(final short vendorId, final short productId) throws UsbException {
		UsbServices services = UsbHostManager.getUsbServices();
		UsbDevice myUsbDevice = findDevice(services.getRootUsbHub(), vendorId, productId);
		return myUsbDevice;
	}

	/**
	 * find USB device starting from hub (root hub)
	 * @param hub
	 * @param vendorId
	 * @param productId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public UsbDevice findDevice(UsbHub hub, short vendorId, short productId) {
		for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
			UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
			if (desc.idVendor() == vendorId && desc.idProduct() == productId)
				return device;
			if (device.isUsbHub()) {
				device = findDevice((UsbHub) device, vendorId, productId);
				if (device != null) {
					log.log(Level.FINE, device.getUsbDeviceDescriptor().toString());
					return device;
				}
			}
		}
		return null;
	}

	/**
	 * dump required information for a USB device with known product ID and
	 * vendor ID
	 * @param vendorId
	 * @param productId
	 * @throws UsbException
	 */
	@SuppressWarnings("unchecked")
	public void dumpUsbDevice(final short vendorId, final short productId) throws UsbException {
		UsbDevice myUsbDevice = findUsbDevice(vendorId, productId);
		if (myUsbDevice != null) {
			for (UsbConfiguration configuration : (List<UsbConfiguration>) myUsbDevice.getUsbConfigurations()) {
				System.out.println(configuration.getUsbConfigurationDescriptor());
				for (int i = 0; i < configuration.getUsbConfigurationDescriptor().bNumInterfaces(); i++) {
					for (UsbEndpoint endpoint : (List<UsbEndpoint>) configuration.getUsbInterface(configuration.getUsbConfigurationDescriptor().iConfiguration()).getUsbEndpoints()) {
						System.out.println(endpoint.getUsbEndpointDescriptor());
					}
				}
			}
		}
	}
	
	/**
	 * claim USB interface with given number which correlates to open a USB port
	 * @param IDevice the actual device in use
	 * @return
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public UsbInterface openUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException {
		byte ifaceId = activeDevice.getUsbInterface();
		UsbDevice usbDevice = findUsbDevice(activeDevice.getUsbVendorId(), activeDevice.getUsbProductId());
		if (usbDevice == null) throw new UsbException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
		UsbInterface usbInterface = ((UsbConfiguration) usbDevice.getUsbConfigurations().get(0)).getUsbInterface(ifaceId);
		log.log(Level.FINE, usbInterface.toString());
		usbInterface.claim();
		log.log(Level.OFF, "interface claimed");
		this.isConnected = true;
		if (this.application != null) this.application.setPortConnected(true);
		return usbInterface;
	}

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public void closeUsbPort(UsbInterface usbInterface) throws UsbClaimException, UsbException {
		this.isConnected = false;
		if (usbInterface == null) {
			UsbDevice usbDevice = findUsbDevice(this.application.getActiveDevice().getUsbVendorId(), this.application.getActiveDevice().getUsbProductId());
			usbInterface = ((UsbConfiguration) usbDevice.getUsbConfigurations().get(0)).getUsbInterface(this.application.getActiveDevice().getUsbInterface());
		}
		if (usbInterface.isClaimed()) {
			usbInterface.release();		
			log.log(Level.OFF, "interface released");
		}
		if (this.application != null) this.application.setPortConnected(false);
	}

	
	/**
	 * write a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data
	 * @return number of bytes sent
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public int write(final UsbInterface iface, final byte endpointAddress, final byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		if (this.application != null) this.application.setSerialTxOn();
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpointAddress);
		int sent = 0;
		UsbPipe pipe = endpoint.getUsbPipe();
		pipe.open();
		try {
			sent = pipe.syncSubmit(data);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, sent + " bytes sent");
		}
		finally {
			pipe.close();
		}
		if (this.application != null) this.application.setSerialTxOff();
		return sent;
	}

	/**
	 * read a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data receive buffer
	 * @return number of bytes received
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public int read(final UsbInterface iface, final byte endpointAddress, byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		if (this.application != null) this.application.setSerialRxOn();
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpointAddress);
		int received = 0;
		UsbPipe pipe = endpoint.getUsbPipe();
		pipe.open();
		try
		{
		    received = pipe.syncSubmit(data);
		    if (log.isLoggable(Level.FINE)) log.log(Level.FINE, received + " bytes received");
		}
		finally
		{
		    pipe.close();
		}
		if (this.application != null) this.application.setSerialRxOff();
		return received;
	}

}
