/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import gde.DataAccess;
import gde.DataAccess.LocalAccess;
import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.device.DevicePropertiesType;
import gde.log.Level;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 *
 * @author Thomas Eickert (USER)
 */
public class DeviceSerialization {
	private static final String	$CLASS_NAME	= DeviceSerialization.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	// JAXB XML environment
	private JAXBContext					jc;
	private Unmarshaller				unmarshaller;
	private Marshaller					marshaller;

	public Thread createXsdThread() {
		Thread xsdThread = new Thread("xsdValidation") {
			@Override
			public void run() {
				Settings.log.log(java.util.logging.Level.INFO, Settings.$CLASS_NAME, "xsdThread.run()");
				// device properties context
				try (InputStream inputStream = DataAccess.getInstance().getDeviceXsdInputStream()) {
					Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(inputStream));
					DeviceSerialization.this.jc = JAXBContext.newInstance("gde.device"); //$NON-NLS-1$
					DeviceSerialization.this.unmarshaller = DeviceSerialization.this.jc.createUnmarshaller();
					DeviceSerialization.this.unmarshaller.setSchema(schema);
					DeviceSerialization.this.marshaller = DeviceSerialization.this.jc.createMarshaller();
					DeviceSerialization.this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
					DeviceSerialization.this.marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, Settings.DEVICE_PROPERTIES_XSD_NAME);
					DeviceSerialization.log.log(Level.TIME, "schema factory setup time = ", StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (Exception e) {
					Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, "xsdThread.run()", e.getMessage(), e);
				}
			}
		};
		return xsdThread;
	}

	public Thread createMigrationThread() {
		Thread migrationThread = new Thread("migration") {
			@Override
			public void run() {
				int lastVersion = Integer.valueOf(GDE.DEVICE_PROPERTIES_XSD_VERSION.substring(GDE.DEVICE_PROPERTIES_XSD_VERSION.lastIndexOf("_V") + 2)) - 1;
				for (int i = lastVersion; i >= 10; i--) {
					if (((LocalAccess) DataAccess.getInstance()).existsDeviceMigrationFolder(i)) {
						log.log(Level.INFO, "previous devices exist, migrate from version " + i);
						try (InputStream inputStream = ((LocalAccess) DataAccess.getInstance()).getDeviceXsdMigrationStream(i)) {
							while (Settings.getInstance().isXsdThreadPending()) {
								WaitTimer.delay(7);
							}

							Unmarshaller tmpUnmarshaller = JAXBContext.newInstance("gde.device").createUnmarshaller();//$NON-NLS-1$
							tmpUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new StreamSource(inputStream)));
							TreeMap<String, DeviceConfiguration> actualConfigurations = DataExplorer.getInstance().getDeviceSelectionDialog().getDevices();
							List<File> deviceProperties = ((LocalAccess) DataAccess.getInstance()).getDeviceXmls(i);
							for (File file : deviceProperties) {
								if (file.getAbsolutePath().endsWith(GDE.FILE_ENDING_DOT_XML)) {
									DeviceConfiguration oldConfig = new DeviceConfiguration(file.getAbsolutePath(), tmpUnmarshaller);
									DeviceConfiguration newConfig = actualConfigurations.get(oldConfig.getName());
									if (oldConfig.isUsed() && newConfig != null) {
										newConfig.setUsed(true);
										if (oldConfig.getPort().length() > 1 && !oldConfig.getPort().startsWith("USB")) newConfig.setPort(oldConfig.getPort());
										if (oldConfig.getDataBlockPreferredDataLocation().length() > 1)
											newConfig.setDataBlockPreferredDataLocation(oldConfig.getDataBlockPreferredDataLocation());

										newConfig.storeDeviceProperties();
										log.log(Level.INFO, "migrated device configuration " + newConfig.getName());
									}
								}
							}
						} catch (Exception e) {
							Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, "xsdThread.run()", e.getMessage(), e);
						}
						break;
					}
				}
			}
		};
		return migrationThread;
	}

	@SuppressWarnings("unchecked") // cast to (JAXBElement<DevicePropertiesType>)
	public JAXBElement<DevicePropertiesType> getTopElement(String xmlFilePath) throws JAXBException {
		return (JAXBElement<DevicePropertiesType>) this.unmarshaller.unmarshal(new File(xmlFilePath));
	}

	/**
	 * Use this for roaming data sources support via the DataAccess class.
	 * @param fileSubPath is a relative path based on the roaming folder
	 * @return the device xml header reference
	 */
	@SuppressWarnings("unchecked") // cast to (JAXBElement<DevicePropertiesType>)
	public JAXBElement<DevicePropertiesType> getTopElement(Path fileSubPath) throws JAXBException, FileNotFoundException {
		return (JAXBElement<DevicePropertiesType>) this.unmarshaller.unmarshal(DataAccess.getInstance().getDeviceXmlInputStream(fileSubPath) );
	}

	public void marshall(JAXBElement<DevicePropertiesType> element, Path xmlFile) throws JAXBException {
		marshaller.marshal(element, xmlFile.toFile());
	}
}
