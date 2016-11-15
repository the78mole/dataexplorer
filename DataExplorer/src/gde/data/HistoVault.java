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

package gde.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import gde.GDE;

/**
 * history data points for scores and reference to the history record vaults for measurements and settlements. 
 * history data persistence / serialization / caching prototype.
 * @author Thomas Eickert
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HistoVault { // todo in case of persistence: must hold offset/factor/reduction or should be based on values instead of points

	private int															creationDataExplorerVersion	= 0;								// device used for recording or log import
	private String													creationDeviceVersion				= GDE.STRING_BLANK;	// device used for recording or log import
	private String													creationDeviceName					= GDE.STRING_BLANK;	// device used for recording or log import
	private int															creationChannelNumber				= 0;								// may differ from UI settings in case of channel mix
	private String													creationObjectKey						= GDE.STRING_BLANK;	// may differ from UI settings (empty in OSD files, parent path for bin files)
	private long														creationTimestamp_ms				= 0;								// timestamp + file + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	private String													creationFile								= GDE.STRING_BLANK;	// timestamp + file + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	private long														creationFileLastModified_ms	= 0;								// timestamp + file + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	private String													creationFileContentsHash;												// option: file contents SHA-1 hash value rendered as a hexadecimal number, 40 digits long --- same algorithm as in git

	private Map<Integer, HistoVaultRecord>	measurementVaultRecords;												// index is measurement ordinal number
	private Map<Integer, HistoVaultRecord>	settlementVaultRecords;													// index is settlementId
	private HistoVaultRecord								scoreVaultRecord;

	public HistoVault() {

	}

	public HistoVault(String creationDeviceName, int creationChannelNumber, String creationObjectKey, long creationTimestamp_ms, Path creationFile, long creationFileLastModified_ms) {
//		if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
//				String.format("TrailVault(IDevice, int, String, long, Path)  %s  channelConfigNumber=%d   objectKey=%s  timestamp_ms=,d  path=%s  lastModified=%s", creationDeviceName, //$NON-NLS-1$
//						creationChannelNumber, creationObjectKey, creationTimestamp_ms, StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", creationTimestamp_ms), creationFile,
//						StringHelper.getFormatedTime("yyyy-MM-dd HH:mm:ss", creationFileLastModified_ms)));
		this.creationDeviceName = creationDeviceName;
		this.creationChannelNumber = creationChannelNumber;
		this.creationObjectKey = creationObjectKey;
		this.creationTimestamp_ms = creationTimestamp_ms;
		this.creationFile = creationFile.toAbsolutePath().toString();
		this.creationFileLastModified_ms = creationFileLastModified_ms;
	}

	/**
		 * clears the data points in all vault records.
		 * removes all vault records.
		 */
	public void cleanup() {
		for (Entry<Integer, HistoVaultRecord> entry : this.measurementVaultRecords.entrySet()) {
			entry.getValue().setPoints(new Integer[0]);
		}
		this.measurementVaultRecords.clear();
		for (Entry<Integer, HistoVaultRecord> entry : this.settlementVaultRecords.entrySet()) {
			entry.getValue().setPoints(new Integer[0]);
		}
		this.settlementVaultRecords.clear();
		this.scoreVaultRecord = new HistoVaultRecord();
	}

	/**
	 * @return simple mapping of a sha-1 key to a path with directory, subdirectory and file named with the sha-1key 
	 */
	public Path getPathFromSha1(String sha1Key) {
		String chars = Integer.toHexString(-Integer.parseUnsignedInt(sha1Key.substring(0, 8), 16));
		return Paths.get(chars.substring(2, 4), chars.substring(6, 8), sha1Key);
	}

	public String getKey() {
		return String.format("%s%s%d%d%s%d%d%s", creationDeviceName, creationDeviceVersion, creationDataExplorerVersion, creationChannelNumber, creationObjectKey, creationTimestamp_ms,
				creationFileLastModified_ms, creationFile);
	}

	/**
	 * unique key representation of this object based on both creation parameters and filename.
	 * @return null or SHA-1 hash value rendered as a hexadecimal number, 40 digits long
	 */
	public String getHashedKey() {
		try {
			return sha1(getKey());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * unique key representation of this object based on both creation parameters and file contents.
	 * @return null or SHA-1 hash value rendered as a hexadecimal number, 40 digits long
	 */
	public String getHashedContentsBasedKey() {
		try {
			return sha1(String.format("%s%s%d%d%s%s", creationDeviceName, creationDeviceVersion, creationDataExplorerVersion, creationChannelNumber, creationObjectKey, sha1(creationFile)));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * source: http://www.sha1-online.com/sha1-java/
	 * @param input
	 * @return SHA-1 hash value rendered as a hexadecimal number, 40 digits long
	 * @throws NoSuchAlgorithmException
	 */
	private String sha1(String input) throws NoSuchAlgorithmException {
		byte[] hashBytes = MessageDigest.getInstance("SHA1").digest(input.getBytes());
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < hashBytes.length; i++) {
			sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	/**
	 * source: http://www.sha1-online.com/sha1-java/
	 * @param filepath including name of a file
	 * @return the file's SHA1 checksum
	 * @throws NoSuchAlgorithmException
	 * @throws FileNotFoundException 
	 * @throws IOException
	 */
	private String sha1(Path filePath) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
		byte[] hashBytes = null;
		MessageDigest sha1 = MessageDigest.getInstance("SHA1");
		try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
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

	public String getCreationDeviceName() {
		return creationDeviceName;
	}

	public void setCreationDeviceName(String creationDeviceName) {
		this.creationDeviceName = creationDeviceName;
	}

	public String getCreationDeviceVersion() {
		return creationDeviceVersion;
	}

	public void setCreationDeviceVersion(String creationDeviceVersion) {
		this.creationDeviceVersion = creationDeviceVersion;
	}

	public int getCreationDataExplorerVersion() {
		return creationDataExplorerVersion;
	}

	public void setCreationDataExplorerVersion(int creationDataExplorerVersion) {
		this.creationDataExplorerVersion = creationDataExplorerVersion;
	}

	public int getCreationChannelNumber() {
		return creationChannelNumber;
	}

	public void setCreationChannelNumber(int creationChannelNumber) {
		this.creationChannelNumber = creationChannelNumber;
	}

	public String getCreationObjectKey() {
		return creationObjectKey;
	}

	public void setCreationObjectKey(String creationObjectKey) {
		this.creationObjectKey = creationObjectKey;
	}

	public long getCreationTimestamp_ms() {
		return creationTimestamp_ms;
	}

	public void setCreationTimestamp_ms(long creationTimestamp_ms) {
		this.creationTimestamp_ms = creationTimestamp_ms;
	}

	public Path getCreationFile() {
		return Paths.get(creationFile);
	}

	public void setCreationFile(Path creationFile) {
		this.creationFile = creationFile.toAbsolutePath().toString();
	}

	public long getCreationFileLastModified_ms() {
		return creationFileLastModified_ms;
	}

	public void setCreationFileLastModified_ms(long creationFileLastModified_ms) {
		this.creationFileLastModified_ms = creationFileLastModified_ms;
	}

	public Map<Integer, HistoVaultRecord> getMeasurementVaultRecords() {
		return measurementVaultRecords;
	}

	/**
	 * @param measurementOrdinal
	 * @return points associated to the trail types with the ordinal as index.
	 */
	public Integer[] getMeasurements(int measurementOrdinal) {
		return this.measurementVaultRecords.get(measurementOrdinal).getPoints();
	}

	public void setMeasurementVaultRecords(Map<Integer, HistoVaultRecord> measurementVaultRecords) {
		this.measurementVaultRecords = measurementVaultRecords;
	}

	/**
	 * @param measurementsPoints list of rows with index measurement type number. each row holds the points associated to the trail types with the ordinal as index.
	 */
	public void setMeasurements(HashMap<Integer, Integer[]> measurementsPoints) {
		for (Entry<Integer, Integer[]> entry : measurementsPoints.entrySet()) {
			setMeasurements(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * @param points associated to the trail types with the ordinal as index.
	 * @param measurementOrdinal
	 */
	public void setMeasurements(Integer[] points, int measurementOrdinal) {
		this.measurementVaultRecords.put(measurementOrdinal, new HistoVaultRecord());
		this.measurementVaultRecords.get(measurementOrdinal).setPoints(points);
	}

	/**
	 * @param settlementsPoints list of rows with index settlementId. each row holds the points associated to the trail types with the ordinal as index.
	 */
	public void setSettlements(HashMap<Integer, Integer[]> settlementsPoints) {
		for (Entry<Integer, Integer[]> entry : settlementsPoints.entrySet()) {
			setSettlements(entry.getValue(), entry.getKey());
		}
	}

	/**
	 * @param points associated to the trail types with the ordinal as index.
	 * @param settlementId
	 */
	public void setSettlements(Integer[] points, int settlementId) {
		this.settlementVaultRecords.put(settlementId, new HistoVaultRecord());
		this.settlementVaultRecords.get(settlementId).setPoints(points);
	}

	/**
	 * @param settlementId
	 * @return points associated to the trail types with the ordinal as index.
	 */
	public Integer[] getSettlements(int settlementId) {
		return this.settlementVaultRecords.get(settlementId).getPoints();
	}

	public Map<Integer, HistoVaultRecord> getSettlementVaultRecords() {
		return settlementVaultRecords;
	}

	public void setSettlementVaultRecords(Map<Integer, HistoVaultRecord> settlementVaultRecords) {
		this.settlementVaultRecords = settlementVaultRecords;
	}

	/**
	 * @return points associated to the scores with the label ordinal as index.
	 */
	public Integer[] getScores() {
		return this.scoreVaultRecord.getPoints();
	}

	public HistoVaultRecord getScoreVaultRecord() {
		return scoreVaultRecord;
	}

	/**
	 * @param points associated to the scores with the label ordinal as index.
	 */
	public void setScores(Integer[] points) {
		this.scoreVaultRecord = new HistoVaultRecord();
		this.scoreVaultRecord.setPoints(points);
	}

	public void setScoreVaultRecord(HistoVaultRecord scoreVaultRecord) {
		this.scoreVaultRecord = scoreVaultRecord;
	}

}
