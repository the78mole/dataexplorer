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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.device.IDevice;
import gde.histo.cache.ExtendedVault;
import gde.histo.cache.HistoVault;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.device.IHistoDevice;
import gde.histo.utils.PathUtils;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Check vaults without Ui, i.e. active device, channel, object.
 * @author Thomas Eickert (USER)
 */
public final class VaultChecker {
	private static final String	$CLASS_NAME	= VaultChecker.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Holds the selection criteria for trusses (vault skeletons) or vaults.
	 */
	public static final class TrussCriteria {
		final List<Integer>	channelMixConfigNumbers;
		final long					minStartTimeStamp_ms;
		final String				activeObjectKey;
		final boolean				ignoreLogObjectKey;
		final Set<String>		realObjectKeys;

		public static TrussCriteria createUiBasedTrussCriteria() {
			return new TrussCriteria(DataExplorer.getInstance().getActiveDevice(), DataExplorer.getInstance().getActiveChannelNumber(),
					Settings.getInstance().getActiveObjectKey());
		}

		public static TrussCriteria createTrussCriteria(IDevice device, int channelNumber, String objectKey) {
			return new TrussCriteria(device, channelNumber, objectKey);
		}

		private TrussCriteria(IDevice device, int channelNumber, String objectKey) {
			channelMixConfigNumbers = device.getDeviceConfiguration().getChannelMixConfigNumbers(channelNumber);
			minStartTimeStamp_ms = LocalDate.now().minusMonths(Settings.getInstance().getRetrospectMonths()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
			activeObjectKey = objectKey;
			ignoreLogObjectKey = Settings.getInstance().getIgnoreLogObjectKey();
			realObjectKeys = Settings.getInstance().getRealObjectKeys().collect(Collectors.toSet());
		}

		@Override
		public String toString() {
			return "TrussCriteria [channelMixConfigNumbers=" + this.channelMixConfigNumbers + ", minStartTimeStamp_ms=" + this.minStartTimeStamp_ms + ", filesWithOtherObject=" + this.ignoreLogObjectKey + ", realObjectKeys=" + this.realObjectKeys + "]";
		}
	}

	/**
	 * @return true if the current device supports both directory type and file extension
	 */
	public static boolean isWorkableDataSet(IDevice device, Path logFilePath, Set<DirectoryType> directoryTypes, SourceFolders sourceFolders) {
		if (!sourceFolders.isMatchingPath(logFilePath)) {
			log.log(Level.OFF, "not a matching file path      ", logFilePath);
			return false;
		}
		for (DirectoryType directoryType : directoryTypes) {
			if (directoryType.getDataSetExtensions(device).contains(PathUtils.getFileExtension(logFilePath.getFileName().toString()))) {
				return true;
			}
		}
		log.log(Level.INFO, "not a valid extension         ", logFilePath);
		return false;
	}

	/**
	 * @return true if the vault complies with the current device, object and start timestamp
	 */
	public static boolean isValidDeviceChannelObjectAndStart(IDevice device, TrussCriteria trussCriteria, HistoVault vault) {
		String extension = PathUtils.getFileExtension(Paths.get(vault.getLogFilePath()));
		if (extension.equals(GDE.FILE_ENDING_OSD))
			return VaultChecker.matchDeviceChannelObjectAndStart(device, trussCriteria, vault);
		else if (device instanceof IHistoDevice) {
			return VaultChecker.matchObjectAndStart(trussCriteria, vault);
		} else {
			return false;
		}
	}

	public static boolean matchObjectAndStart(TrussCriteria trussCriteria, HistoVault vault) {
		if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match startTime%,7d kiB %s", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match startTime  %s", vault.getLogFilePath()));
			return false;
		}
		if (!trussCriteria.activeObjectKey.isEmpty()) {
			boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObjectKey);
			if (!consistentObjectKey) {
				if (trussCriteria.ignoreLogObjectKey) return true;
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", ((ExtendedVault) vault).getRectifiedObjectKey(), ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
				return false;
			}
		}
		return true;
	}

	public static boolean matchDeviceChannelObjectAndStart(IDevice device, TrussCriteria trussCriteria, HistoVault vault) {
		if (device != null && !vault.getLogDeviceName().equals(device.getName()) //
		// HoTTViewer V3 -> HoTTViewerAdapter
				&& !(vault.getLogDeviceName().startsWith("HoTTViewer") && device.getName().equals("HoTTViewer"))) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match device   %,7d kiB %s", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match device  %s", vault.getLogFilePath()));
			return false;
		}
		if (!trussCriteria.channelMixConfigNumbers.contains(vault.getLogChannelNumber())) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match channel%2d%,7d kiB %s", vault.getLogChannelNumber(), ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match channel  %s", vault.getLogFilePath()));
			return false;
		}
		if (vault.getLogStartTimestamp_ms() < trussCriteria.minStartTimeStamp_ms) {
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match startTime%,7d kiB %s", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match startTime  %s", vault.getLogFilePath()));
			return false;
		}

		if (trussCriteria.activeObjectKey.isEmpty()) {
			if (!vault.getLogObjectKey().isEmpty()) return true;
			// no object in the osd file is a undesirable case but the log is accepted in order to show all logs
			log.log(Level.INFO, vault instanceof ExtendedVault //
					? String.format("no match objectKey=%8s%,7d kiB %s", "empty", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
					: String.format("no match objectKey  %s", vault.getLogFilePath()));
		} else {
			if (vault.getLogObjectKey().isEmpty()) {
				if (trussCriteria.ignoreLogObjectKey) return true;
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", "empty", ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
				return false;
			}
			boolean matchingObjectKey = trussCriteria.realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(vault.getLogObjectKey().trim()));
			if (!matchingObjectKey) {
				if (trussCriteria.ignoreLogObjectKey) return true;
				String objectDirectory = vault instanceof ExtendedVault ? ((ExtendedVault) vault).getLoadObjectDirectory() : vault.getLogObjectDirectory();
				boolean matchingObjectDirectory = trussCriteria.realObjectKeys.stream().anyMatch(s -> s.equalsIgnoreCase(objectDirectory));
				if (matchingObjectDirectory) return true;
				log.log(Level.INFO, vault instanceof ExtendedVault //
						? String.format("no match objectKey=%8s%,7d kiB %s", ((ExtendedVault) vault).getRectifiedObjectKey(), ((ExtendedVault) vault).getLoadFileAsPath().toFile().length() / 1024, ((ExtendedVault) vault).getLoadFileAsPath().toString()) //
						: String.format("no match objectKey  %s", vault.getLogFilePath()));
				return false;
			}
			boolean consistentObjectKey = vault.getLogObjectKey().equalsIgnoreCase(trussCriteria.activeObjectKey);
			if (!consistentObjectKey) {
				// finally we found a log with a validated object key which does not correspond to the desired one
				return false;
			}
		}
		return true;
	}

	private final IDevice									device;
	private final EnumSet<DirectoryType>	validDirectoryTypes;
	private final String									objectKey;
	private final SourceFolders						sourceFolders;

	/**
	 * @param directoryTypes may hold the import directory type as well
	 */
	public VaultChecker(IDevice device, EnumSet<DirectoryType> directoryTypes, String objectKey) {
		this.device = device;
		this.validDirectoryTypes = directoryTypes;
		this.objectKey = objectKey;
		this.sourceFolders = new SourceFolders(objectKey);
		this.sourceFolders.defineDirectories(device, false);
	}

	/**
	 * @return true if the current device supports both directory type and file extension
	 */
	public boolean isValidVault(HistoVault vault) {
		Path logFilePath = Paths.get(vault.getLogFilePath());
		if (VaultChecker.isWorkableDataSet(device, logFilePath, validDirectoryTypes, sourceFolders)) {
			TrussCriteria trussCriteria = TrussCriteria.createTrussCriteria(device, vault.getLogChannelNumber(), objectKey);
			return VaultChecker.isValidDeviceChannelObjectAndStart(device, trussCriteria, vault);
		} else {
			return false;
		}

	}

}
