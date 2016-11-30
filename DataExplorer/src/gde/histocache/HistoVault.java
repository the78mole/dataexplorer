//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.22 at 03:14:10 PM MEZ 
//
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
    
    Copyright (c) 2016 Thomas Eickert
****************************************************************************************/

package gde.histocache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.validation.SchemaFactory;

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * aggregated history recordset data related to
 * 				measurements, settlements and scores
 * 			
 * 
 * <p>Java class for histoVault complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="histoVault">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="cacheKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="dataExplorerVersion" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="deviceKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="uiDeviceName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="uiChannelNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="uiObjectKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="samplingTimespan_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="maxLogDuration_mm" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="filePath" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="fileLastModified_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="logChannelNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logObjectKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logRecordSetNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logRecordsetName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="logStartTimestamp_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="measurements" type="{}entries"/>
 *         &lt;element name="settlements" type="{}entries"/>
 *         &lt;element name="scores" type="{}entryPoints"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
/**
 * suitable for history persistence and xml serialization.
 * find the constructors and non-xsd code a good way down for simplified merging from JAXB generated class.  
 * @author Thomas Eickert
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "histoVault", propOrder = { "cacheKey", "dataExplorerVersion", "deviceKey", "uiDeviceName", "uiChannelNumber", "uiObjectKey", "samplingTimespanMs", "maxLogDurationMm", "filePath",
		"fileLastModifiedMs", "logChannelNumber", "logObjectKey", "logRecordSetNumber", "logRecordsetName", "logStartTimestampMs", "measurements", "settlements", "scores" })
public class HistoVault {
	final private static String	$CLASS_NAME								= HistoVault.class.getName();
	final private static Logger	log												= Logger.getLogger($CLASS_NAME);

	private static int					activeDataExplorerVersion	= Integer.parseInt(GDE.VERSION.substring(8).replace(GDE.STRING_DOT, GDE.STRING_EMPTY));

	private static Path					activeDevicePath;																																																	// criterion for the active device version key cache
	private static String				activeDeviceKey;																																																	// caches the version key for the active device which is calculated only if the device is changed by the user
	private static JAXBContext	jaxbContext;
	private static Unmarshaller	jaxbUnmarshaller;
	private static Marshaller		jaxbMarshaller;

	@XmlTransient
	private final DataExplorer	application								= DataExplorer.getInstance();
	@XmlTransient
	private final Settings			settings									= Settings.getInstance();
	@XmlTransient
	private final IDevice				device										= application.getActiveDevice();

	@XmlElement(required = true)
	protected String						cacheKey;
	protected int								dataExplorerVersion;
	@XmlElement(required = true)
	protected String						deviceKey;
	@XmlElement(required = true)
	protected String						uiDeviceName;
	protected int								uiChannelNumber;
	@XmlElement(required = true)
	protected String						uiObjectKey;
	@XmlElement(name = "samplingTimespan_ms")
	protected long							samplingTimespanMs;
	@XmlElement(name = "maxLogDuration_mm")
	protected int								maxLogDurationMm;
	@XmlElement(required = true)
	protected String						filePath;
	@XmlElement(name = "fileLastModified_ms")
	protected long							fileLastModifiedMs;
	protected int								logChannelNumber;
	@XmlElement(required = true)
	protected String						logObjectKey;
	protected int								logRecordSetNumber;
	@XmlElement(required = true)
	protected String						logRecordsetName;
	@XmlElement(name = "logStartTimestamp_ms")
	protected long							logStartTimestampMs;
	@XmlElement(required = true)
	protected Entries						measurements;
	@XmlElement(required = true)
	protected Entries						settlements;
	@XmlElement(required = true)
	protected EntryPoints				scores;

	/**
	 * Gets the value of the cacheKey property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getCacheKey() {
		return cacheKey;
	}

	/**
	 * Gets the value of the dataExplorerVersion property.
	 * 
	 */
	public int getDataExplorerVersion() {
		return dataExplorerVersion;
	}

	/**
	 * Gets the value of the deviceKey property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getDeviceKey() {
		return deviceKey;
	}

	/**
	 * Gets the value of the uiDeviceName property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getUiDeviceName() {
		return uiDeviceName;
	}

	/**
	   * Gets the value of the uiChannelNumber property.
	 * 
	 */
	public int getUiChannelNumber() {
		return uiChannelNumber;
	}

	/**
	   * Gets the value of the uiObjectKey property.
	 * 
	 * @return
	 *     possible object is
	   *     {@link String }
	 *     
	 */
	public String getUiObjectKey() {
		return uiObjectKey;
	}

	/**
	 * Gets the value of the samplingTimespanMs property.
	 * 
	 */
	public long getSamplingTimespan_ms() {
		return samplingTimespanMs;
	}

	/**
	 * Gets the value of the maxLogDurationMm property.
	 * 
	 */
	public int getMaxLogDuration_mm() {
		return maxLogDurationMm;
	}

	/**
	   * Gets the value of the filePath property.
	   * 
	   * @return
	   *     possible object is
	   *     {@link String }
	   * 
	   */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Gets the value of the fileLastModifiedMs property.
	 * 
	 */
	public long getFileLastModified_ms() {
		return fileLastModifiedMs;
	}

	/**
	 * Gets the value of the logChannelNumber property.
	 * 
	 */
	public int getLogChannelNumber() {
		return logChannelNumber;
	}

	/**
	 * Gets the value of the logObjectKey property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogObjectKey() {
		return logObjectKey;
	}

	/**
	 * Gets the value of the logRecordsetNumber property.
	*     
	*/
	public int getLogRecordSetNumber() {
		return logRecordSetNumber;
	}

	/**
	 * Gets the value of the logRecordsetName property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getLogRecordsetName() {
		return logRecordsetName;
	}

	/**
	 * Gets the value of the logStartTimestampMs property.
	 * 
	 */
	public long getLogStartTimestamp_ms() {
		return logStartTimestampMs;
	}

	/**
	* Gets the value of the measurements property.
	* 
	* @return
	*     possible object is
	*     {@link Entries }
	*     
	*/
	public Entries getMeasurements() {
		return measurements;
	}

	/**
	 * Sets the value of the measurements property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Entries }
	 *     
	 */
	public void setMeasurements(Entries value) {
		this.measurements = value;
	}

	/**
	 * Gets the value of the settlements property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Entries }
	 *     
	 */
	public Entries getSettlements() {
		return settlements;
	}

	/**
	 * Sets the value of the settlements property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Entries }
	 *     
	 */
	public void setSettlements(Entries value) {
		this.settlements = value;
	}

	/**
	   * Gets the value of the scores property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link EntryPoints }
	 *     
	 */
	public EntryPoints getScores() {
		return scores;
	}

	/**
	   * Sets the value of the scores property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link EntryPoints }
	 *     
	 */
	public void setScores(EntryPoints value) {
		this.scores = value;
	}

	@Deprecated // for marshalling purposes only
	public HistoVault() {
	}

	/**
	 * @param filePath file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logRecordSetNumber identifies multiple recordsets within on single file
	 * @param logRecordSetName 
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, parent path for bin files)
	 */
	private HistoVault(Path filePath, long fileLastModified_ms, int logRecordSetNumber, String logRecordSetName, long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
		this.dataExplorerVersion = HistoVault.activeDataExplorerVersion;
		this.deviceKey = HistoVault.getActiveDeviceKey();
		this.uiDeviceName = application.getActiveDevice().getName();
		this.uiChannelNumber = application.getActiveChannelNumber();
		this.uiObjectKey = application.getObjectKey();
		this.logChannelNumber = logChannelNumber;
		this.logObjectKey = logObjectKey;
		this.samplingTimespanMs = settings.getSamplingTimespan_ms();
		this.maxLogDurationMm = settings.getMaxLogDuration_mm();
		this.filePath = filePath.toString(); // toString due to avoid 'Object' during marshalling
		this.fileLastModifiedMs = fileLastModified_ms;
		this.logRecordSetNumber = logRecordSetNumber;
		this.logRecordsetName = logRecordSetName;
		this.logStartTimestampMs = logStartTimestamp_ms;

		this.cacheKey = HistoVault.getCacheKey(filePath, fileLastModified_ms, logRecordSetNumber).toString();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER,
				String.format("HistoVault.ctor  path=%s  lastModified=%s  recordsetName=%s  startTimestamp_ms=%s   channelConfigNumber=%d   objectKey=%s", //$NON-NLS-1$
						filePath.getFileName().toString(), logRecordSetName, StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", fileLastModified_ms),
						StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", logStartTimestamp_ms), logChannelNumber, logObjectKey));
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("this.cacheKey=%s", this.cacheKey));
	}

	/**
	 * @param filePath file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logRecordSetNumber identifies multiple recordsets within on single file
	 * @param logRecordsetName 
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, parent path for bin files)
	 * @param measurementEntries
	 * @param settlementEntries
	 * @param scorePoints
	 * @return new instance with a full set of data
	 */
	public static HistoVault createHistoVault(Path filePath, long fileLastModified_ms, int logRecordSetNumber, String logRecordsetName, long logStartTimestamp_ms, int logChannelNumber,
			String logObjectKey, Entries measurementEntries, Entries settlementEntries, EntryPoints scorePoints) {
		HistoVault newHistoVault = new HistoVault(filePath, fileLastModified_ms, logRecordSetNumber, logRecordsetName, logStartTimestamp_ms, logChannelNumber, logObjectKey);
		newHistoVault.setMeasurements(measurementEntries);
		newHistoVault.setSettlements(settlementEntries);
		newHistoVault.setScores(scorePoints);
		return newHistoVault;
	}

	/**
	 * @param fullQualifiedFileName path
	 * @return new instance 
	 */
	public static HistoVault load(Path fullQualifiedFileName) {
		HistoVault newHistoVault = null;
		try {
			newHistoVault = (HistoVault) HistoVault.getUnmarshaller().unmarshal(fullQualifiedFileName.toFile());
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
		return newHistoVault;
	}

	/**
	 * @param inputStream
	 * @return new instance 
	 */
	public static HistoVault load(InputStream inputStream) {
		HistoVault newHistoVault = null;
		try {
			newHistoVault = (HistoVault) HistoVault.getUnmarshaller().unmarshal(inputStream);
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
		return newHistoVault;
	}

	/**
	 * writes device properties XML to given full qualified file name
	 * @param fullQualifiedFileName
	 */
	public void store(Path fullQualifiedFileName) {
		try {
			HistoVault.getMarshaller().marshal(this, fullQualifiedFileName.toFile());
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * writes device properties XML to given full qualified file name
	 * @param fullQualifiedFileName
	 */
	public void store(OutputStream outputStream) {
		try {
			HistoVault.getMarshaller().marshal(this, outputStream);
		}
		catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return context singleton (creating the context is slow)
	 */
	private static JAXBContext getJaxbContext() {
		if (HistoVault.jaxbContext == null) {
			try {
				HistoVault.jaxbContext = JAXBContext.newInstance(HistoVault.class);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbContext;
	}

	/**
	 * @return cached instance (unmarshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	private static Unmarshaller getUnmarshaller() {
		if (HistoVault.jaxbUnmarshaller == null) {
			try {
				HistoVault.jaxbUnmarshaller = getJaxbContext().createUnmarshaller();
				HistoVault.jaxbUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
						.newSchema(Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME).toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbUnmarshaller;
	}

	/**
	 * @return cached instance (marshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	private static Marshaller getMarshaller() {
		if (HistoVault.jaxbMarshaller == null) {
			try {
				HistoVault.jaxbMarshaller = getJaxbContext().createMarshaller();
				HistoVault.jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				HistoVault.jaxbMarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
						.newSchema(Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME).toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbMarshaller;
	}

	/**
	 * @return true if the vault object conforms to current versions of the Data Explorer / device XML, to current user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public boolean isValid() {
		return this.cacheKey.equals(HistoVault.getCacheKey(Paths.get(this.filePath), this.fileLastModifiedMs, this.getLogRecordSetNumber()));
	}

	/**
	 * @param logFileName file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logFileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logRecordSetNumber identifies multiple recordsets within on single file
	 * @return true if the vault object conforms to current versions of the Data Explorer / device XML, to current user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public boolean isValid(String logFileName, long logFileLastModified_ms, int logRecordSetNumber) {
		return this.cacheKey.equals(HistoVault.getCacheKey(Paths.get(logFileName), fileLastModifiedMs, logRecordSetNumber));
	}

	/**
	 * @return sub-directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus channel number and some settings values
	 */
	public static Path getVaultSubDirectory() {
		String tmpSubDirectoryLongKey = String.format("%d%s%d%d%d", HistoVault.activeDataExplorerVersion, getActiveDeviceKey(), DataExplorer.getInstance().getActiveChannelNumber(),
				Settings.getInstance().getSamplingTimespan_ms(), Settings.getInstance().getMaxLogDuration_mm());
		return Paths.get(HistoVault.sha1(tmpSubDirectoryLongKey));

	}

	/**
	 * @param fileName
	 * @param fileLastModified_ms
	 * @param logRecordSetNumber identifies multiple recordsets in one single file
	 * @return full path with filename as a unique identifier (sha1)
	 */
	public static Path getCacheKey(Path fileName, long fileLastModified_ms, int logRecordSetNumber) {
		// do not include as these attributes are determined after reading the histoset: logChannelNumber, logObjectKey, logStartTimestampMs
		return Paths.get(HistoVault.sha1(HistoVault.getVaultSubDirectory() + String.format("%s%d%d", fileName.getFileName(), fileLastModified_ms, logRecordSetNumber)));
	}

	/**
	 * @return sha1 key as a unique identifier for the device xml file contents
	 */
	private static String getActiveDeviceKey() {
		Path tmpActiveDevicePath = Paths.get(DataExplorer.getInstance().getActiveDevice().getPropertiesFileName());
		if (HistoVault.activeDeviceKey == null || HistoVault.activeDevicePath == null || !HistoVault.activeDevicePath.equals(tmpActiveDevicePath)) {
			HistoVault.activeDevicePath = tmpActiveDevicePath;
			try {
				HistoVault.activeDeviceKey = HistoVault.sha1(tmpActiveDevicePath.toFile());
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return HistoVault.activeDeviceKey;
	}

	/**
	 * source: http://www.sha1-online.com/sha1-java/
	 * @param input
	 * @return SHA-1 hash value rendered as a hexadecimal number, 40 digits long
	 */
	private static String sha1(String input) {
		byte[] hashBytes = null;
		try {
			hashBytes = MessageDigest.getInstance("SHA1").digest(input.getBytes());
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hashBytes.length; i++) {
			sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	/**
	 * source: http://www.sha1-online.com/sha1-java/
	 * @param file 
	 * @return the file's full data SHA1 checksum
	 * @throws IOException
	 */
	private static String sha1(File file) throws IOException {
		MessageDigest sha1 = null;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] hashBytes = null;
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] data = new byte[8192]; // most file systems are configured to use block sizes of 4096 or 8192
			int read = 0;
			while ((read = fis.read(data)) != -1) {
				sha1.update(data, 0, read);
			}
			hashBytes = sha1.digest();
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hashBytes.length; i++) {
			sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	public List<Point> getMeasurements(int measurementOrdinal) {
			return this.measurements.getEntryPoints().get(measurementOrdinal).getPoints();
	}

	public Integer getMeasurement(int measurementOrdinal, int trailTextSelectedIndex) {
		if (this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().size() == 0) {
			return null;
		}
		else {
			if (trailTextSelectedIndex > this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().size())
				throw new UnsupportedOperationException();
			return this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().get(trailTextSelectedIndex).getValue();
		}
	}

	public List<Point> getSettlements(int settlementId) {
			return this.settlements.getEntryPoints().get(settlementId).getPoints();
	}

	public Integer getSettlement(int settlementId, int trailTextSelectedIndex) {
		if (this.settlements.getEntryPoints().get(settlementId).getPoints().size() == 0) {
			return null;
		}
		else {
			if (trailTextSelectedIndex >= this.measurements.getEntryPoints().get(settlementId).getPoints().size())
				throw new UnsupportedOperationException();
			return this.settlements.getEntryPoints().get(settlementId).getPoints().get(trailTextSelectedIndex).getValue();
		}
	}

	public List<Point> getScorePoints() {
		return scores.getPoints();
	}

	public Integer getScorePoint(int scoreLabelOrdinal) {
		if (this.scores.getPoints() == null) {
			return null;
		}
		else {
			if (scoreLabelOrdinal >= scores.getPoints().size())
				throw new UnsupportedOperationException();
			return this.scores.getPoints().get(scoreLabelOrdinal).getValue();
		}
	}

}
