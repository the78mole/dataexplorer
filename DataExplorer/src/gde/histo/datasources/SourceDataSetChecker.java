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

package gde.histo.datasources;

import java.nio.file.Paths;
import java.util.EnumSet;

import gde.device.IDevice;
import gde.histo.cache.HistoVault;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.datasources.DirectoryScanner.SourceFolders;
import gde.log.Logger;

/**
 * Check log files without respecting the active device, channel, object etc.
 * @author Thomas Eickert (USER)
 */
public final class SourceDataSetChecker extends AbstractSourceDataSets {
	private static final String						$CLASS_NAME	= SourceDataSetExplorer.class.getName();
	private static final Logger						log					= Logger.getLogger($CLASS_NAME);

	private final IDevice									device;
	private final EnumSet<DirectoryType>	validDirectoryTypes;
	private final TrussCriteria						trussCriteria;

	/**
	 * @param directoryTypes may hold the import directory type as well
	 */
	public SourceDataSetChecker(IDevice device, EnumSet<DirectoryType> directoryTypes, int channelNumber, String objectKey) {
		super(new SourceFolders());
		this.device = device;
		this.validDirectoryTypes = directoryTypes;
		this.trussCriteria = TrussCriteria.createTrussCriteria(device, channelNumber, objectKey);
		initSourceFolders(device);
	}

	/**
	 * @return true if the current device supports both directory type and file extension
	 */
	public boolean isValidVault(HistoVault vault) {
		SourceDataSet originFile = SourceDataSet.createSourceDataSet(Paths.get(vault.getLogFilePath()), device);
		if (originFile != null && originFile.isWorkableFile(validDirectoryTypes, sourceFolders)) {
			return originFile.isValidDeviceChannelObjectAndStart(trussCriteria, vault);
		} else {
			return false;
		}

	}

}
