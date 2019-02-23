package gde.comm;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbPipe;
import javax.usb.UsbServices;
import javax.usb.event.UsbPipeDataEvent;
import javax.usb.event.UsbPipeErrorEvent;
import javax.usb.event.UsbPipeListener;

import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

import gde.GDE;
import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.exception.ApplicationConfigurationException;
import gde.exception.FailedQueryException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.WaitTimer;
import gnu.io.SerialPort;

public class DeviceUsbPortImpl implements IDeviceCommPort {
	private final static Logger								log												= Logger.getLogger(DeviceUsbPortImpl.class.getName());

	final protected DeviceConfiguration		deviceConfig;
	final protected DataExplorer					application;
	final Settings												settings;

	boolean																isConnected								= false;
	int 																	asyncReceived							= 0;
	boolean																isAsyncReceived						= false;
	
	Set<UsbDevice> 												usbDevices 								= new HashSet<UsbDevice>();
	Set<Device> 													libUsbDevices 						= new HashSet<Device>();

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
	@Override
	public Set<UsbDevice> findUsbDevices(final short vendorId, final short productId) throws UsbException {
		usbDevices.clear();
		libUsbDevices.clear();
		UsbServices services = UsbHostManager.getUsbServices();
		return findDevices(services.getRootUsbHub(), vendorId, productId);
	}

	/**
	 * find USB device starting from hub (root hub)
	 * @param hub
	 * @param vendorId
	 * @param productId
	 * @return
	 */
	@Override
	public Set<UsbDevice> findDevices(UsbHub hub, short vendorId, short productId) {
		for (Object tmpDevice : hub.getAttachedUsbDevices()) {
			if (tmpDevice instanceof UsbDevice) {
				UsbDevice device = (UsbDevice) tmpDevice;
				if (log.isLoggable(Level.FINER)) {
					log.log(Level.FINER, device.toString());
					log.log(Level.FINER, device.getUsbDeviceDescriptor().toString());
				}
				UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
				if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
					if (log.isLoggable(Level.FINE)) {
						log.log(Level.FINE, device.toString());
						log.log(Level.FINE, device.getUsbDeviceDescriptor().toString());
					}
					usbDevices.add(device);
				}
				else if (device.isUsbHub()) {
					usbDevices.addAll(findDevices((UsbHub) device, vendorId, productId));
				}
			}
		}
		return usbDevices;
	}
	
	public Set<Device> findDevices(short vendorId, short productId) throws LibUsbException {
	    // Read the USB device list
	    DeviceList list = new DeviceList();
	    Context context = new Context();
	    int result = LibUsb.init(context);
      if (result < 0) {
          throw new LibUsbException("Unable to initialize libusb", result);
      }
	    result = LibUsb.getDeviceList(context, list);
	    if (result < 0) 
	    	throw new LibUsbException("Unable to get device list", result);

	    try {
	        // Iterate over all devices and scan for the right one
	        for (Device device: list) {
	            DeviceDescriptor descriptor = new DeviceDescriptor();
	            result = LibUsb.getDeviceDescriptor(device, descriptor);
	            if (result != LibUsb.SUCCESS) 
	            	throw new LibUsbException("Unable to read device descriptor", result);
	            else if (descriptor.idVendor() == vendorId && descriptor.idProduct() == productId) 
	            	libUsbDevices.add(device);
	        }
	    }
	    finally {
	        // Ensure the allocated device list is freed
	        LibUsb.freeDeviceList(list, true);
	    }
	    return libUsbDevices;
	}

	/**
	 * find all USB devices recursive over USB hubs
	 * @param hub
	 */
	public void findUsbDevices(UsbHub hub) {
		for (Object tmpDevice : hub.getAttachedUsbDevices()) {
			if (tmpDevice instanceof UsbDevice) {
				UsbDevice usbDevice = (UsbDevice) tmpDevice;
				log.log(Level.INFO, usbDevice.toString());
				log.log(Level.INFO, usbDevice.getUsbDeviceDescriptor().toString());
				if (usbDevice.isUsbHub()) {
					findUsbDevices((UsbHub) usbDevice);
				}
			}
		}
	}

	/**
	 * dump information for all USB devices
	 * @throws UsbException
	 */
	public void dumpUsbDevices() throws UsbException {
		log.log(Level.INFO, "Use UsbHostManager.getUsbServices()\n");
		UsbServices services = UsbHostManager.getUsbServices();
		UsbHub hub = services.getRootUsbHub();
		findUsbDevices((UsbHub) hub);
	}

	/**
	 * dump information for all USB devices using LibUsb
	 * @throws UsbException
	 */
	public void dumpLibUsbDevices() throws LibUsbException {
		log.log(Level.INFO, "Use LibUsb.getDeviceList()\n");
		DeviceList list = new DeviceList();
		Context context = new Context();

		int result = LibUsb.init(context);
		if (result < 0) throw new LibUsbException("Unable to initialize libusb", result);

		result = LibUsb.getDeviceList(context, list);
		if (result < 0) throw new LibUsbException("Unable to get device list", result);

		try {
			for (Device device : list) {
				DeviceDescriptor descriptor = new DeviceDescriptor();
				result = LibUsb.getDeviceDescriptor(device, descriptor);
				if (result == LibUsb.SUCCESS) {
					log.log(Level.INFO, device.toString());
					log.log(Level.INFO, descriptor.dump());
				}
			}
		}
		finally {
			LibUsb.freeDeviceList(list, true);
		}
	}

	
	/**
	 * dump required information for a USB device with known product ID and
	 * vendor ID
	 * @param vendorId
	 * @param productId
	 * @throws UsbException
	 */
	public void dumpUsbDevices(final short vendorId, final short productId) throws UsbException {
		Set<UsbDevice> myUsbDevices = findUsbDevices(vendorId, productId);
		for (UsbDevice myUsbDevice : myUsbDevices) {
			log.log(Level.FINE, myUsbDevice.toString());
			log.log(Level.FINE, myUsbDevice.getUsbDeviceDescriptor().toString());
		}
		
		
		Set<Device> myLibUsbDevices = findDevices(vendorId, productId);
		for (Device myLibUsbDevice : myLibUsbDevices) {
			DeviceDescriptor descriptor = new DeviceDescriptor();
      int result = LibUsb.getDeviceDescriptor(myLibUsbDevice, descriptor);
      if (result == LibUsb.SUCCESS) {
      	log.log(Level.FINE, myLibUsbDevice.toString());
      	log.log(Level.FINE, descriptor.dump());
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
	public synchronized UsbInterface openUsbPort(final IDevice activeDevice) throws UsbClaimException, UsbException {
		
		if (log.isLoggable(Level.FINE)) {
			dumpUsbDevices();
		}
		else if (log.isLoggable(Level.INFO)) {
			dumpUsbDevices();
			dumpLibUsbDevices();
		}
		
		byte ifaceId = activeDevice.getUsbInterface();
		UsbInterface usbInterface = null;
		Set<UsbDevice> usbDevices = findUsbDevices(activeDevice.getUsbVendorId(), activeDevice.getUsbProductId());
		if (usbDevices.size() == 0) {
			this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0050));
		} else if (usbDevices.size() == 1) {
			for (UsbDevice usbDevice : usbDevices) {
				if (usbDevice == null) 
					throw new UsbException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
				usbInterface = claimUsbInterface(usbDevice, ifaceId);
			}
		} else {
			for (UsbDevice usbDevice : usbDevices) {
				UsbInterface tmpUsbInterface = ((UsbConfiguration) usbDevice.getUsbConfigurations().get(0)).getUsbInterface(ifaceId);
				if (tmpUsbInterface!= null && !(tmpUsbInterface.isActive() || tmpUsbInterface.isClaimed())) {
					usbInterface = claimUsbInterface(usbDevice, ifaceId);
					break;
				}
			}
		}
		if (usbInterface == null) {
			throw new UsbException(Messages.getString(gde.messages.MessageIds.GDE_MSGE0050));
		}
		log.log(Level.FINE, "interface claimed");
		this.isConnected = true;
		if (this.application != null) this.application.setPortConnected(true);
		return usbInterface;
	}

	/**
	 * @param usbDevice
	 * @param ifaceId
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	private UsbInterface claimUsbInterface(UsbDevice usbDevice, byte ifaceId) throws UsbClaimException, UsbException {
		UsbInterface usbInterface = ((UsbConfiguration) usbDevice.getUsbConfigurations().get(0)).getUsbInterface(ifaceId);
		log.log(Level.FINE, usbInterface.toString());
		if (GDE.IS_LINUX) {
			usbInterface.claim(new UsbInterfacePolicy()
			{            
			    @Override
			    public boolean forceClaim(UsbInterface usbInterface_)
			    {
//			    	DeviceHandle handle = new DeviceHandle();
//			    	int result = LibUsb.open(findDevice(activeDevice.getUsbVendorId(), activeDevice.getUsbProductId()), handle);
//			    	if (result != LibUsb.SUCCESS) throw new LibUsbException("Unable to open USB device", result);
//			    	try
//			    	{
//						   // Check if kernel driver must be detached
//				    	boolean detach = LibUsb.hasCapability(LibUsb.CAP_SUPPORTS_DETACH_KERNEL_DRIVER) 
//				    	    && 1 == LibUsb.kernelDriverActive(handle, activeDevice.getUsbInterface());
//				    	log.log(Level.INFO, "kernel driver must be detached = " + detach);
//
//				    	// Detach the kernel driver
//				    	if (detach)
//				    	{
//				    	    int detachResult = LibUsb.detachKernelDriver(handle,  activeDevice.getUsbInterface());
//				    	    if (detachResult != LibUsb.SUCCESS) throw new LibUsbException("Unable to detach kernel driver", detachResult);
//				    	}
//			    	}
//			    	finally
//			    	{
//			    	    LibUsb.close(handle);
//			    	}

			        return true;
			    }
			});
		}
		else {
			usbInterface.claim();
		}
		return usbInterface;
	}

	/**
	 * release or close the given interface
	 * @param usbInterface
	 * @throws UsbClaimException
	 * @throws UsbException
	 */
	public synchronized void closeUsbPort(UsbInterface usbInterface) throws UsbClaimException, UsbException {
		this.isConnected = false;
		WaitTimer.delay(200); // enable gatherer thread to stop execution
		if (usbInterface != null && usbInterface.isClaimed()) {
			usbInterface.release();		
			log.log(Level.FINE, "interface released");
		}
		if (this.application != null) {
			this.application.setSerialTxOff();
			this.application.setSerialRxOff();
			this.application.setPortConnected(false);
		}
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
	public synchronized int write(final UsbInterface iface, final byte endpointAddress, final byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
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
	public synchronized int read(final UsbInterface iface, final byte endpointAddress, byte[] data) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
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
		return received;
	}

	/**
	 * read a byte array of data using the given interface and its end point address
	 * @param iface
	 * @param endpointAddress
	 * @param data receive buffer
	 * @param timeout_msec
	 * @return number of bytes received
	 * @throws UsbNotActiveException
	 * @throws UsbNotClaimedException
	 * @throws UsbDisconnectedException
	 * @throws UsbException
	 */
	public synchronized int read(final UsbInterface iface, final byte endpointAddress, final byte[] data, final int timeout_msec) throws UsbNotActiveException, UsbNotClaimedException, UsbDisconnectedException, UsbException {
		UsbEndpoint endpoint = iface.getUsbEndpoint(endpointAddress);
		int waitTime_msec = timeout_msec;
		int waitDelay_msec = timeout_msec/100;
		asyncReceived = 0;
		isAsyncReceived = false;
		final UsbPipe pipe = endpoint.getUsbPipe();
		UsbPipeListener usbPipeListener = new UsbPipeListener() {

			public void errorEventOccurred(UsbPipeErrorEvent evt) {
				log.log(Level.WARNING, evt.toString());
			}

			public void dataEventOccurred(UsbPipeDataEvent evt) {
				byte[] tmpData = evt.getData();
				isAsyncReceived = true;
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, (asyncReceived=tmpData.length) + " bytes received");
			}
		};
		try {
			pipe.open();
			pipe.addUsbPipeListener(usbPipeListener);
			pipe.asyncSubmit(data);
			
			while (isConnected && !isAsyncReceived && waitTime_msec > 0) {
				WaitTimer.delay(waitDelay_msec);
				waitTime_msec -= waitDelay_msec;
			}
		}
		finally
		{
			pipe.removeUsbPipeListener(usbPipeListener);
			if (pipe.isOpen()) {
				pipe.abortAllSubmissions();
				pipe.close();
			}
		}
		return asyncReceived;
	}
}
