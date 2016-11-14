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

import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * history data points for scores and reference to the history record vaults for measurements and settlements. 
 * history data persistence / serialization / caching prototype.
 * @author Thomas Eickert
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class HistoVault { // todo in case of persistence: must hold offset/factor/reduction or should be based on values instead of points

	private String													creationDeviceName;						// device used for recording or log import
	private String													creationDeviceVersion;				// device used for recording or log import
	private int															creationDataExplorerVersion;	// device used for recording or log import
	private int															creationChannelNumber;				// may differ from UI settings in case of channel mix
	private String													creationObjectKey;						// may differ from UI settings (empty in OSD files, parent path for bin files)
	private long														creationTimestamp_ms;					// timestamp + file + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	private String													creationFile;									// timestamp + file + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	private long														creationFileLastModified_ms;	// timestamp + file + lastModified are a simple solution for getting a SHA-1 hash from the file contents
	private String													creationFileContentsHash;			// option: file contents SHA-1 hash value rendered as a hexadecimal number, 40 digits long --- same algorithm as in git

	private Map<Integer, HistoVaultRecord>	measurementVaultRecords;			// index is measurement ordinal number
	private Map<Integer, HistoVaultRecord>	settlementVaultRecords;				// index is settlementId
	private HistoVaultRecord								scoreVaultRecordS;

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

	public String getCreationFile() {
		return creationFile;
	}

	public void setCreationFile(String creationFile) {
		this.creationFile = creationFile;
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

	public void setMeasurementVaultRecords(Map<Integer, HistoVaultRecord> measurementVaultRecords) {
		this.measurementVaultRecords = measurementVaultRecords;
	}

	public Map<Integer, HistoVaultRecord> getSettlementVaultRecords() {
		return settlementVaultRecords;
	}

	public void setSettlementVaultRecords(Map<Integer, HistoVaultRecord> settlementVaultRecords) {
		this.settlementVaultRecords = settlementVaultRecords;
	}

	public HistoVaultRecord getScoreVaultRecordS() {
		return scoreVaultRecordS;
	}

	public void setScoreVaultRecordS(HistoVaultRecord scoreVaultRecordS) {
		this.scoreVaultRecordS = scoreVaultRecordS;
	}

}
