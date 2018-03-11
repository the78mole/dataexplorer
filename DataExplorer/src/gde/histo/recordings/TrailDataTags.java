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

package gde.histo.recordings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import gde.GDE;
import gde.config.Settings;
import gde.histo.cache.ExtendedVault;
import gde.histo.gpslocations.GeoCodes;
import gde.histo.gpslocations.GpsCluster;
import gde.histo.recordings.HistoTableMapper.DisplayTag;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.utils.GpsCoordinate;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Tags corresponding to timestep entries.
 * @author Thomas Eickert (USER)
 */
public final class TrailDataTags extends EnumMap<DataTag, List<String>> {
	private static final String	$CLASS_NAME				= TrailDataTags.class.getName();
	private static final long		serialVersionUID	= -1091858232851684060L;
	private static final Logger	log								= Logger.getLogger($CLASS_NAME);

	public enum DataTag {
		LINK_PATH, FILE_PATH, CHANNEL_NUMBER, RECTIFIED_OBJECTKEY, RECORDSET_BASE_NAME, RECORDSET_ORDINAL, GPS_LOCATION
	};

	private final List<String>	dataLinkPaths						= new ArrayList<String>();
	private final List<String>	dataFilePaths						= new ArrayList<String>();
	private final List<String>	dataChannelNumbers			= new ArrayList<String>();
	private final List<String>	dataRectifiedObjectKeys	= new ArrayList<String>();
	private final List<String>	dataRecordsetBaseNames	= new ArrayList<String>();
	private final List<String>	dataRecordSetOrdinals		= new ArrayList<String>();
	private final List<String>	dataGpsLocations				= new ArrayList<String>();

	/**
	 * Caching for active tags.
	 */
	private EnumSet<DisplayTag>	activeDisplayTags				= null;

	public TrailDataTags() {
		super(DataTag.class);
		this.put(DataTag.LINK_PATH, this.dataLinkPaths);
		this.put(DataTag.FILE_PATH, this.dataFilePaths);
		this.put(DataTag.CHANNEL_NUMBER, this.dataChannelNumbers);
		this.put(DataTag.RECTIFIED_OBJECTKEY, this.dataRectifiedObjectKeys);
		this.put(DataTag.RECORDSET_BASE_NAME, this.dataRecordsetBaseNames);
		this.put(DataTag.RECORDSET_ORDINAL, this.dataRecordsetBaseNames);
		this.put(DataTag.GPS_LOCATION, this.dataGpsLocations);
	}

	@Override
	public void clear() {
		// super.clear(); do not clear the list of valid tags
		for (List<String> list : this.values()) {
			list.clear();
		}

		this.activeDisplayTags = null;
	}

	public void add(ExtendedVault histoVault) {
		this.dataLinkPaths.add(histoVault.getLogLinkPath().intern());
		this.dataFilePaths.add(histoVault.getLogFilePath().intern());
		this.dataChannelNumbers.add(String.valueOf(histoVault.getLogChannelNumber()).intern());
		this.dataRectifiedObjectKeys.add(histoVault.getRectifiedObjectKey().intern());
		this.dataRecordsetBaseNames.add(histoVault.getLogRecordsetBaseName().intern());
		this.dataRecordSetOrdinals.add(String.valueOf(histoVault.getLogRecordSetOrdinal()).intern());
	}

	/**
	 * @param gpsCluster
	 */
	public void add(GpsCluster gpsCluster) {
		List<String> tmpGpsLocations = new ArrayList<String>();
		for (GpsCoordinate gpsCoordinate : gpsCluster) {
			// preserve the correct vaults sequence
			if (gpsCoordinate != null)
				tmpGpsLocations.add(GeoCodes.getLocation(gpsCluster.getAssignedClusters().get(gpsCoordinate).getCenter()));
			else
				tmpGpsLocations.add(GDE.STRING_EMPTY);
		}
		// fill the data tags only if there is at least one GPS coordinate
		if (tmpGpsLocations.parallelStream().filter(s -> !s.isEmpty()).count() > 0) this.dataGpsLocations.addAll(tmpGpsLocations);
	}

	/**
	 * @return the dataTags
	 */
	public Map<DataTag, String> getByIndex(int index) {
		if (index >= 0) {
			Map<DataTag, String> dataTags4Index = new EnumMap<>(DataTag.class);
			for (Entry<DataTag, List<String>> logTagEntry : this.entrySet()) {
				if (logTagEntry.getValue().size() > 0) dataTags4Index.put(logTagEntry.getKey(), logTagEntry.getValue().get(index));
			}
			return dataTags4Index;
		} else
			return new EnumMap<DataTag, String>(DataTag.class);
	}

	/**
	 * @return the dataTag value
	 */
	public String getText(int index, DataTag dataTag) {
		if (index >= 0) {
			return this.get(dataTag).get(index);
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * @return the tags which have been filled and carry non-redundant data
	 */
	public EnumSet<DisplayTag> getActiveDisplayTags() {
		if (this.activeDisplayTags == null) {
			defineActiveDisplayTags();
		}
		return this.activeDisplayTags;
	}

	/**
	 *
	 */
	public void defineActiveDisplayTags() {
		EnumSet<DisplayTag> resultTags = EnumSet.allOf(DisplayTag.class);
		if (this.get(DataTag.GPS_LOCATION).isEmpty()) resultTags.remove(DisplayTag.GPS_LOCATION);

		if (!this.get(DataTag.FILE_PATH).isEmpty() && Settings.getInstance().isPartialDataTable()) {
			{
				Path directoryPath = Paths.get(this.get(getSourcePathTag(0)).get(0)).getParent();
				boolean sameDirectory = true;
				boolean sameBase = true;
				for (int i = 0; i < this.get(DataTag.FILE_PATH).size(); i++) {
					Path path = Paths.get(this.get(getSourcePathTag(i)).get(i));
					if (!path.getParent().getFileName().equals(directoryPath.getFileName())) sameDirectory = false;
					if (path.getNameCount() > 2 && !path.getParent().getParent().equals(directoryPath.getParent())) sameBase = false;
					if (!sameDirectory && !sameBase) break;
				}
				if (sameDirectory) resultTags.remove(DisplayTag.DIRECTORY_NAME);
				if (sameBase) resultTags.remove(DisplayTag.BASE_PATH);
			}
			{
				String channelNumber = this.get(DataTag.CHANNEL_NUMBER).get(0);
				boolean sameChannel = true;
				for (String tmp : this.get(DataTag.CHANNEL_NUMBER)) {
					if (!tmp.equals(channelNumber) || DataExplorer.application.getActiveChannelNumber() != Integer.parseInt(tmp)) {
						sameChannel = false;
						break;
					}
				}
				if (sameChannel) resultTags.remove(DisplayTag.CHANNEL_NUMBER);
			}
			{
				String objectKey = this.get(DataTag.RECTIFIED_OBJECTKEY).get(0);
				boolean sameObject = true;
				for (String tmp : this.get(DataTag.RECTIFIED_OBJECTKEY)) {
					if (!tmp.equals(objectKey)) {
						sameObject = false;
						break;
					}
				}
				if (sameObject) resultTags.remove(DisplayTag.RECTIFIED_OBJECTKEY);
			}
		}
		this.activeDisplayTags = resultTags;
		log.finer(() -> "activeDisplayTags.size()=" + resultTags.size()); //$NON-NLS-1$
	}

	/**
	 * @param index points to the record and / or time scale element
	 * @return the data tag which defines the access to the link path or file path
	 */
	public DataTag getSourcePathTag(int index) {
		return this.get(DataTag.LINK_PATH).get(index).isEmpty() || !DataExplorer.getInstance().isObjectoriented() ? DataTag.FILE_PATH : DataTag.LINK_PATH;
	}

	public List<String> getDataGpsLocations() {
		return this.dataGpsLocations;
	}

	public List<String> getDataLinkPaths() {
		return this.dataLinkPaths;
	}

	public List<String> getDataFilePaths() {
		return this.dataFilePaths;
	}

	public List<String> getDataChannelNumbers() {
		return this.dataChannelNumbers;
	}

	public List<String> getDataRectifiedObjectKeys() {
		return this.dataRectifiedObjectKeys;
	}

	public List<String> getDataRecordsetBaseNames() {
		return this.dataRecordsetBaseNames;
	}

	public List<String> getDataRecordSetOrdinals() {
		return this.dataRecordSetOrdinals;
	}
}
