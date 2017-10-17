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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.device.TrailTypes;
import gde.histo.recordings.TrailRecord;
import gde.histo.utils.SecureHash;
import gde.log.Level;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Add functions to the pure serializable histo vault object.
 * @author Thomas Eickert (USER)
 */
public final class ExtendedVault extends HistoVault {
	final private static String	$CLASS_NAME	= ExtendedVault.class.getName();
	final private static Logger	log					= Logger.getLogger($CLASS_NAME);

	private static Path					activeDevicePath;														// criterion for the active device version key cache
	private static String				activeDeviceKey;														// caches the version key for the active device which is calculated only if the device is changed by the user
	private static long					activeDeviceLastModified_ms;								// caches the version key for the active device which is calculated only if the device is changed by the user

	public static Path getCacheDirectory() {
		return Paths.get(Settings.getInstance().getApplHomePath(), Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
	}

	/**
	 * @param newVaultsDirectoryName directory or zip file name
	 * @return true if the vault directory name conforms to current versions of the Data Explorer / device XML, to the current channel and to user settings (e.g. sampling timespan) and to various additional attributes
	 */
	public static boolean isValidDirectory(String newVaultsDirectoryName) {
		return newVaultsDirectoryName.equals(getVaultsDirectoryName());
	}

	/**
	 * The vaults directory name is determined without any log file contents.
	 * This supports vault directory scanning functions in the future.
	 * @return directory or zip file name as a unique identifier encoding the data explorer version, the device xml file contents(sha1) plus channel number and some settings values
	 */
	public static String getVaultsDirectoryName() {
		String tmpSubDirectoryLongKey = String.format("%s,%s,%d,%d,%f,%f", GDE.VERSION, getActiveDeviceKey(), DataExplorer.getInstance().getActiveChannelNumber(), //$NON-NLS-1$
				Settings.getInstance().getSamplingTimespan_ms(), Settings.getInstance().getMinmaxQuantileDistance(), Settings.getInstance().getAbsoluteTransitionLevel());
		return SecureHash.sha1(tmpSubDirectoryLongKey);

	}

	/**
	 * @return the path to the directory or zip file
	 */
	public static Path getVaultsFolder() {
		return getCacheDirectory().resolve(getVaultsDirectoryName());
	}

	/**
	 * The vaults file name is determined without any log file contents and without file location properties (path).
	 * This supports creating a truss or accessing a vault without having read the original log file.
	 * @param newLogFileName file name + lastModified + newFileLength are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newFileLastModified_ms
	 * @param newFileLength in bytes
	 * @param newLogRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @return the file name as a unique identifier (sha1)
	 */
	public static String getVaultName(Path newLogFileName, long newFileLastModified_ms, long newFileLength, int newLogRecordSetOrdinal) {
		return SecureHash.sha1(getVaultsDirectoryName() + String.format("%s,%d,%d,%d", newLogFileName.getFileName(), newFileLastModified_ms, newFileLength, newLogRecordSetOrdinal)); //$NON-NLS-1$
	}

	/**
	 * @param newLogFileName file name + lastModified + newFileLength are a simple solution for getting a SHA-1 hash from the file contents
	 * @param newFileLastModified_ms
	 * @param newFileLength in bytes
	 * @param newLogRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @return the path with filename as a unique identifier (sha1)
	 */
	public static Path getVaultRelativePath(Path newLogFileName, long newFileLastModified_ms, long newFileLength, int newLogRecordSetOrdinal) {
		// do not include as these attributes are determined after reading the histoset: logChannelNumber, logObjectKey, logStartTimestampMs
		Path subDirectory = Paths.get(getVaultsDirectoryName());
		return subDirectory.resolve(getVaultName(newLogFileName.getFileName(), newFileLastModified_ms, newFileLength, newLogRecordSetOrdinal));
	}

	/**
	 * Supports key caching which is essential for the system performance.
	 * @return sha1 key as a unique identifier for the device xml file contents
	 */
	public static String getActiveDeviceKey() {
		File file = new File(DataExplorer.getInstance().getActiveDevice().getPropertiesFileName());
		if (activeDeviceKey == null || activeDevicePath == null || !activeDevicePath.equals(file.toPath()) || activeDeviceLastModified_ms != file.lastModified()) {
			try {
				activeDeviceKey = SecureHash.sha1(file);
				activeDevicePath = file.toPath();
				activeDeviceLastModified_ms = file.lastModified();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return activeDeviceKey;
	}

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final Settings			settings		= Settings.getInstance();

	/**
	 * @param objectDirectory validated object key
	 * @param filePath file name + lastModified + file length are a simple solution for getting a SHA-1 hash from the file contents
	 * @param fileLastModified_ms
	 * @param fileLength in bytes
	 * @param fileVersion is the version of the log origin file
	 * @param logRecordSetSize is the number of recordsets in the log origin file
	 * @param logRecordSetOrdinal identifies multiple recordsets in one single file (0-based)
	 * @param logRecordSetBaseName the base name without recordset number
	 * @param logStartTimestamp_ms of the log or recordset
	 * @param logDeviceName
	 * @param logChannelNumber may differ from UI settings in case of channel mix
	 * @param logObjectKey may differ from UI settings (empty in OSD files, validated parent path for bin files)
	 */
	public ExtendedVault(String objectDirectory, Path filePath, long fileLastModified_ms, long fileLength, int fileVersion, int logRecordSetSize, int logRecordSetOrdinal, String logRecordSetBaseName,
			String logDeviceName, long logStartTimestamp_ms, int logChannelNumber, String logObjectKey) {
		this.vaultDataExplorerVersion = GDE.VERSION;
		this.vaultDeviceKey = getActiveDeviceKey();
		this.vaultDeviceName = this.application.getActiveDevice().getName();
		this.vaultChannelNumber = this.application.getActiveChannelNumber();
		this.vaultObjectKey = this.application.getObjectKey();
		this.vaultSamplingTimespanMs = this.settings.getSamplingTimespan_ms();

		this.logLinkPath = GDE.STRING_EMPTY;
		this.logFilePath = filePath.toString(); // toString in order to avoid 'Object' during marshalling
		this.logFileLastModified = fileLastModified_ms;
		this.logFileLength = fileLength;
		this.logFileVersion = fileVersion;

		this.logObjectDirectory = objectDirectory;
		this.logRecordSetSize = logRecordSetSize;
		this.logRecordSetOrdinal = logRecordSetOrdinal;
		this.logRecordsetBaseName = logRecordSetBaseName;
		this.logDeviceName = logDeviceName;
		this.logChannelNumber = logChannelNumber;
		this.logObjectKey = logObjectKey;
		this.logStartTimestampMs = logStartTimestamp_ms;

		this.vaultDirectory = getVaultsDirectoryName();
		this.vaultName = getVaultName(filePath, fileLastModified_ms, fileLength, logRecordSetOrdinal);
		this.vaultCreatedMs = System.currentTimeMillis();

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER,
				String.format("HistoVault.ctor  objectDirectory=%s  path=%s  lastModified=%s  logRecordSetOrdinal=%d  logRecordSetBaseName=%s  startTimestamp_ms=%d   channelConfigNumber=%d   objectKey=%s", //$NON-NLS-1$
						objectDirectory, filePath.getFileName().toString(), logRecordSetBaseName, StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.logFileLastModified), //$NON-NLS-1$
						StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss.SSS", this.logStartTimestampMs), logChannelNumber, logObjectKey)); //$NON-NLS-1$
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("vaultDirectory=%s  vaultName=%s", this.vaultDirectory, this.vaultName)); //$NON-NLS-1$
	}

	/**
	 * Convert into extended vault via shallow copy.
	 * @param histoVault
	 */
	public ExtendedVault(HistoVault histoVault) {
		this.vaultName = histoVault.vaultName;
		this.vaultDirectory = histoVault.vaultDirectory;
		this.vaultCreatedMs = histoVault.vaultCreatedMs;

		this.vaultDataExplorerVersion = histoVault.vaultDataExplorerVersion;
		this.vaultDeviceKey = histoVault.vaultDeviceKey;
		this.vaultDeviceName = histoVault.vaultDeviceName;
		this.vaultChannelNumber = histoVault.vaultChannelNumber;
		this.vaultObjectKey = histoVault.vaultObjectKey;
		this.vaultSamplingTimespanMs = histoVault.vaultSamplingTimespanMs;

		this.logLinkPath = histoVault.logLinkPath;
		this.logFilePath = histoVault.logFilePath;
		this.logFileLastModified = histoVault.logFileLastModified;
		this.logFileLength = histoVault.logFileLength;
		this.logFileVersion = histoVault.logFileVersion;

		this.logObjectDirectory = histoVault.logObjectDirectory;
		this.logRecordSetSize = histoVault.logRecordSetSize;
		this.logRecordSetOrdinal = histoVault.logRecordSetOrdinal;
		this.logRecordsetBaseName = histoVault.logRecordsetBaseName;
		this.logDeviceName = histoVault.logDeviceName;
		this.logChannelNumber = histoVault.logChannelNumber;
		this.logObjectKey = histoVault.logObjectKey;
		this.logStartTimestampMs = histoVault.logStartTimestampMs;

		this.measurements = histoVault.measurements;
		this.settlements = histoVault.settlements;
		this.scores = histoVault.scores;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.vaultName).append(GDE.STRING_COMMA_BLANK);
		sb.append("isTruss=").append(isTruss()).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("logRecordSetOrdinal=").append(this.logRecordSetOrdinal).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("logRecordsetBaseName=").append(this.logRecordsetBaseName).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("logChannelNumber=").append(this.logChannelNumber).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("logObjectKey=").append(this.logObjectKey).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append("logStartTimestampMs=").append(this.logStartTimestampMs).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
		sb.append(this.logLinkPath).append(GDE.STRING_COMMA_BLANK);
		sb.append(this.logFilePath).append(GDE.STRING_COMMA_BLANK);
		sb.append("vaultDirectory=").append(this.vaultDirectory); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * @param measurementOrdinal may specify an ordinal which is not present in the vault (earlier osd file - measurements added in the meantime)
	 * @return empty in case of unavailable measurement
	 */
	public HashMap<Integer, PointType> getMeasurementPoints(int measurementOrdinal) {
		return this.getMeasurements().containsKey(measurementOrdinal) ? new HashMap<Integer, PointType>() : this.getMeasurements().get(measurementOrdinal).getTrails();
	}

	/**
	 * @param measurementOrdinal may specify an ordinal which is not present in the vault (earlier osd file - measurements added in the meantime)
	 * @param trailOrdinal
	 * @return null in case of unavailable measurement or trail
	 */
	public Integer getMeasurementPoint(int measurementOrdinal, int trailOrdinal) {
		if (this.getMeasurements().containsKey(measurementOrdinal)) {
			return this.getMeasurements().get(measurementOrdinal).getTrails().containsKey(trailOrdinal) ? this.getMeasurements().get(measurementOrdinal).getTrails().get(trailOrdinal).value : null;
		}
		else {
			return null;
		}
	}

	/**
	 * @param settlementId may specify an ordinal which is not present in the vault (earlier osd file - measurements added in the meantime)
	 * @return empty in case of unavailable settlementId
	 */
	public HashMap<Integer, PointType> getSettlementPoints(int settlementId) {
		return this.getSettlements().containsKey(settlementId) ? new HashMap<Integer, PointType>() : this.getSettlements().get(settlementId).getTrails();
	}

	/**
	 * @param settlementId may specify an ordinal which is not present in the vault (earlier osd file - measurements added in the meantime)
	 * @param trailOrdinal
	 * @return null in case of unavailable settlement or trail
	 */
	public Integer getSettlementPoint(int settlementId, int trailOrdinal) {
		if (this.getSettlements().containsKey(settlementId)) {
			return this.getSettlements().get(settlementId).getTrails().containsKey(trailOrdinal) ? this.getSettlements().get(settlementId).getTrails().get(trailOrdinal).value : null;
		}
		else {
			return null;
		}
	}

	public HashMap<Integer, PointType> getScorePoints() {
		return this.getScores();
	}

	/**
	 * @param scoreLabelOrdinal
	 * @return null in case of unavailable score
	 */
	public Integer getScorePoint(int scoreLabelOrdinal) {
		return this.getScores().get(scoreLabelOrdinal).getValue();
	}

	/**
	 * @param trailRecord
	 * @param trailType
	 * @return the vault value for the selected trail type
	 */
	public Integer getPoint(TrailRecord trailRecord, TrailTypes trailType) {
		Integer point;
		if (trailRecord.isMeasurement())
			point = getMeasurementPoint(trailRecord.getOrdinal(), trailType.ordinal());
		else if (trailRecord.isSettlement())
			point = getSettlementPoint(trailRecord.getSettlement().getSettlementId(), trailType.ordinal());
		else if (trailRecord.isScoreGroup()) {
			point = getScorePoint(trailType.ordinal());
		}
		else
			throw new UnsupportedOperationException();

		if (log.isLoggable(Level.FINEST))
			log.log(Level.FINEST, String.format(" %s trail %3d  %s %s", trailRecord.getName(), trailRecord.getTrailSelector().getTrailOrdinal(), getVaultFileName(), getLogFilePath())); //$NON-NLS-1$
		return point;
	}

	/**
	 * @return yyyy-MM-dd HH:mm:ss
	 */
	public String getStartTimeStampFormatted() {
		return StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", this.logStartTimestampMs); //$NON-NLS-1$
	}

	public String getLogFileExtension() {
		String extension = "";
		int i = this.logFilePath.lastIndexOf('.');
		if (i > 0) {
			extension = this.logFilePath.substring(i + 1);
		}
		return extension;
	}

	public Path getLogFileAsPath() {
		return Paths.get(this.logFilePath);
	}

	/**
	 * @return relative path (directory name)
	 */
	public Path getVaultDirectoryPath() {
		return Paths.get(this.vaultDirectory);
	}

	public Path getVaultFileName() {
		return Paths.get(this.vaultName);
	}

	/**
	 * @return the non-validated object key or alternatively (if empty) the non-validated object directory
	 */
	public String getRectifiedObjectKey() {
		return this.logObjectKey.isEmpty() ? this.logObjectDirectory : this.logObjectKey;
	}

	/**
	 * @return true if this is a vault skeleton only
	 */
	public boolean isTruss() {
		return this.getMeasurements().isEmpty();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.logChannelNumber;
		result = prime * result + ((this.logDeviceName == null) ? 0 : this.logDeviceName.hashCode());
		result = prime * result + (int) (this.logStartTimestampMs ^ (this.logStartTimestampMs >>> 32));
		return result;
	}

	/**
	 * Identify duplicates originating from copied log files or from file conversions (e.g. bin to osd).
	 * @param obj
	 * @return true if a truss or a fully populated vault have the same origin log file
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		HistoVault other = (HistoVault) obj;
		if (this.logChannelNumber != other.logChannelNumber) return false;
		if (this.logDeviceName == null) {
			if (other.logDeviceName != null) return false;
		} else if (!this.logDeviceName.equals(other.logDeviceName)) return false;
		if (this.logStartTimestampMs != other.logStartTimestampMs) return false;
		return true;
	}

}
