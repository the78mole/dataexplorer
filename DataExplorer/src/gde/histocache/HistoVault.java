//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
	// Generated on: 2016.11.20 at 11:02:16 AM MEZ 
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
 *         &lt;element name="logStartTimestamp_ms" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="logChannelNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="logObjectKey" type="{http://www.w3.org/2001/XMLSchema}string"/>
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
 * holds aggregated history recordset data for measurements, settlements and scores.
 * suitable for history persistence and xml serialization.
 * the vault object cache key is based on versions (dataExplorer and device xml), on current UI settings and on logging data (channel, object, filename and file modified date).
 * @author Thomas Eickert
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "histoVault", propOrder = {
    "cacheKey",
    "dataExplorerVersion",
    "deviceKey",
    "uiDeviceName",
    "uiChannelNumber",
    "uiObjectKey",
    "samplingTimespanMs",
    "maxLogDurationMm",
    "filePath",
    "fileLastModifiedMs",
    "logStartTimestampMs",
    "logChannelNumber",
    "logObjectKey",
    "measurements",
    "settlements",
    "scores"
})
public class HistoVault {
	final private static String	$CLASS_NAME								= HistoVault.class.getName();
	final private static Logger	log												= Logger.getLogger($CLASS_NAME);

	private static int					activeDataExplorerVersion	= Integer.parseInt(GDE.VERSION.substring(8).replace(GDE.STRING_DOT, GDE.STRING_EMPTY));

	private static Path					activeDevicePath;																																																	// criterion for the active device version key cache
	private static String				activeDeviceKey;																																																	// caches the version key for the active device which is calculated only if the device is changed by the user
	private static String				subDirectoryLongKey;																																															// criterion for the sub directory key cache
	private static String				subDirectoryKey;																																																	// caches sub directory sha1 key which is calculated only if the device or the channel is changed by the user
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
    @XmlElement(name = "logStartTimestamp_ms")
	protected long							logStartTimestampMs;
    protected int logChannelNumber;
	@XmlElement(required = true)
    protected String logObjectKey;
    @XmlElement(required = true)
	protected Entries						measurements;
	@XmlElement(required = true)
	protected Entries						settlements;
	@XmlElement(required = true)
	protected EntryPoints				scores;

	@Deprecated // for marshalling purposes only
	public HistoVault() {
	}

	/**
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, parent path for bin files)
	 * @param filePath file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logStartTimestamp_ms of the log or recordset
	 */
	private HistoVault(Path filePath, long fileLastModified_ms, long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
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
		this.logStartTimestampMs = logStartTimestamp_ms;

		this.cacheKey = HistoVault.getCacheKey(filePath, fileLastModified_ms).toString();
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
				String.format("HistoVault(Path, long, long, int, String)  path=%s  lastModified=%s  startTimestamp_ms=%,d   channelConfigNumber=%d   objectKey=%s", //$NON-NLS-1$
						filePath.getFileName().toString(), StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", fileLastModified_ms),
						StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", logStartTimestamp_ms), logChannelNumber, logObjectKey));
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("this.cacheKey=%s", this.cacheKey));
	}

	/**
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, parent path for bin files)
	 * @param filePath file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param measurementEntries
	 * @param settlementEntries
	 * @param scorePoints
	 * @return new instance with a full set of data
	 */
	public static HistoVault createHistoVault(Path filePath, long fileLastModified_ms, long logStartTimestamp_ms, int logChannelNumber, String logObjectKey, Entries measurementEntries,
			Entries settlementEntries, EntryPoints scorePoints) {
		HistoVault newHistoVault = new HistoVault(filePath, fileLastModified_ms, logStartTimestamp_ms, logChannelNumber, logObjectKey);
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

	private static Unmarshaller getUnmarshaller() throws JAXBException {
		if (HistoVault.jaxbUnmarshaller == null) {
			JAXBContext jaxbContext = JAXBContext.newInstance(HistoVault.class);
			HistoVault.jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			try {
				HistoVault.jaxbUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
						.newSchema(Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME).toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return HistoVault.jaxbUnmarshaller;
	}

	private static Marshaller getMarshaller() throws JAXBException {
		if (HistoVault.jaxbMarshaller == null) {
			JAXBContext jaxbContext = JAXBContext.newInstance(HistoVault.class);
			HistoVault.jaxbMarshaller = jaxbContext.createMarshaller();
			//			Path schemaPath = Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME, Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			//			Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaPath.toFile()); //$NON-NLS-1$
			//			HistoVault.jaxbMarshaller.setSchema(schema);
			//			HistoVault.jaxbMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			HistoVault.jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			try {
				HistoVault.jaxbUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
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
		return this.cacheKey.equals(HistoVault.getCacheKey(Paths.get(this.filePath), this.fileLastModifiedMs));
	}

	/**
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, parent path for bin files)
	 * @param filePath file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @param logFileLastModified_ms file name + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	 * @return true if the vault object conforms to current versions of the Data Explorer / device XML, to current user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public boolean isValid(String logFileName, long logFileLastModified_ms) {
		return this.cacheKey.equals(HistoVault.getCacheKey(Paths.get(logFileName), fileLastModifiedMs));
	}

	/**
	 * @return sub-directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus channel number and some settings values
	 */
	public static Path getVaultSubDirectory() {
		String tmpSubDirectoryLongKey = String.format("%d%s%d%d%d", HistoVault.activeDataExplorerVersion, getActiveDeviceKey(), DataExplorer.getInstance().getActiveChannelNumber(),
				Settings.getInstance().getSamplingTimespan_ms(), Settings.getInstance().getMaxLogDuration_mm());
		if (!tmpSubDirectoryLongKey.equals(HistoVault.subDirectoryLongKey)) {
			HistoVault.subDirectoryLongKey = tmpSubDirectoryLongKey;
			HistoVault.subDirectoryKey = HistoVault.sha1(tmpSubDirectoryLongKey);
		}
		return Paths.get(HistoVault.subDirectoryKey);

	}

	/**
	 * @param fileName
	 * @param fileLastModified_ms
	 * @return filename as a unique identifier (sha1)
	 */
	public static Path getCacheKey(Path fileName, long fileLastModified_ms) {
		// do not include as these attributes are determined after reading the histoset: logChannelNumber, logObjectKey, logStartTimestampMs
		return Paths.get(HistoVault.sha1(HistoVault.getVaultSubDirectory() + String.format("%s%d", fileName.getFileName(), fileLastModified_ms)));
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

	public Integer getMeasurement(int measurementOrdinal, int trailOrdinal) {
		if (this.measurements.getEntryPoints().get(measurementOrdinal).getPoints() == null) {
			return null;
		}
		else {
			return this.measurements.getEntryPoints().get(measurementOrdinal).getPoints().get(trailOrdinal).getValue();
		}
	}

	public List<Point> getSettlements(int settlementId) {
		return this.settlements.getEntryPoints().get(settlementId).getPoints();
	}

	public Integer getSettlement(int settlementId, int trailOrdinal) {
		if (this.settlements.getEntryPoints().get(settlementId).getPoints() == null) {
			return null;
		}
		else {
			return this.settlements.getEntryPoints().get(settlementId).getPoints().get(trailOrdinal).getValue();
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
			return this.scores.getPoints().get(scoreLabelOrdinal).getValue();
		}
	}

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
     * Gets the value of the logStartTimestampMs property.
	 *     
	 */
    public long getLogStartTimestamp_ms() {
        return logStartTimestampMs;
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

}
