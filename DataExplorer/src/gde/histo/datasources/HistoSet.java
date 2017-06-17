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
package gde.histo.datasources;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.HistoVault;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.exclusions.ExclusionData;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Supports the selection of histo vaults and provides a trail recordset based on the vaults.
 * Sorted by recordSet startTimeStamp in reverse order; each timestamp may hold multiple vaults.
 * @author Thomas Eickert
 */
public final class HistoSet extends TreeMap<Long, List<HistoVault>> {
	private final static String	$CLASS_NAME				= HistoSet.class.getName();
	private static final long		serialVersionUID	= 1111377035274863787L;
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application				= DataExplorer.getInstance();
	private final Settings			settings					= Settings.getInstance();

	private static HistoSet			histoSet					= null;

	private DirectoryScanner		directoryScanner;
	private HistoSetCollector		histoSetCollector;

	/**
	 * Defines the first step during rebuilding the histoset data.
	 * A minimum of steps may be selected for performance reasons.
	 */
	public enum RebuildStep {
		/**
		* true starts from scratch on
		*/
		A_HISTOSET(5),
		/**
		* true starts building the histo vaults
		*/
		B_HISTOVAULTS(4),
		/**
		* true starts building the trail recordset from the histo vaults
		*/
		C_TRAILRECORDSET(3),
		/**
		* true starts refreshing the trail data from the histo vaults
		*/
		D_TRAIL_DATA(2),
		/**
		* starts updating the graphics and table
		*/
		E_USER_INTERFACE(1),
		/**
		* starts with a file check only which decides which update activity is required
		*/
		F_FILE_CHECK(0);

		/**
		 * zero is the lowest scopeOfWork.
		 */
		public final int					scopeOfWork;
		/**
		 * use this to avoid repeatedly cloning actions instead of values()
		 */
		public static RebuildStep	values[]	= values();

		private RebuildStep(int scopeOfWork) {
			this.scopeOfWork = scopeOfWork;
		}

		/**
		 * @param scopeOfWork zero is the lowest scopeOfWork
		 * @return the rebuild step corresponding to the scope of work
		 */
		public static RebuildStep area(int scopeOfWork) {
			for (RebuildStep rebuildStep : values()) {
				if (rebuildStep.scopeOfWork == scopeOfWork) {
					return rebuildStep;
				}
			}
			throw new IllegalArgumentException(String.valueOf(scopeOfWork));
		}

	};

	public static HistoSet getInstance() {
		if (HistoSet.histoSet == null) HistoSet.histoSet = new HistoSet();
		return HistoSet.histoSet;
	}

	private HistoSet() {
		super(Collections.reverseOrder());
		this.directoryScanner = new DirectoryScanner();
		this.histoSetCollector = new HistoSetCollector();
	}

	/**
	 * Re- initializes the singleton.
	 */
	public synchronized void initialize() {
		this.clear();

		this.directoryScanner.initialize();
		this.histoSetCollector.initialize();

		if (log.isLoggable(Level.INFO))
			log.log(Level.INFO, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
					this.application.getActiveChannelNumber(), this.application.getObjectKey()));
	}

	/**
	 * Clears trails for refill but keeps the trail recordset.
	 */
	@Override
	public void clear() {
		// deep clear in order to reduce memory consumption prior to garbage collection
		for (List<HistoVault> timestampHistoVaults : this.values()) {
			timestampHistoVaults.clear();
		}
		super.clear();

		// this.histoFilePaths.clear(); is accomplished by files validation
		if (log.isLoggable(Level.OFF))
			log.log(Level.OFF, String.format("device=%s  channel=%d  objectKey=%s", this.application.getActiveDevice() == null ? null : this.application.getActiveDevice().getName(), //$NON-NLS-1$
					this.application.getActiveChannelNumber(), this.application.getObjectKey()));
	}

	/**
	 * Add vaults to the histoSet and discard trusses.
	 * @param vaults in state 'truss' or full vaults
	 * @return the file length sum for all vaults added to the histoSet
	 */
	public long putFullVaults(List<HistoVault> vaults) {
		long localSizeSum_B = 0;
		for (HistoVault histoVault : vaults) {
			if (!histoVault.isTruss()) {
				putVault(histoVault);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("added   startTimeStamp=%s  %s  logRecordSetOrdinal=%d  logChannelNumber=%d  %s", //$NON-NLS-1$
						histoVault.getStartTimeStampFormatted(), histoVault.getVaultFileName(), histoVault.getLogRecordSetOrdinal(), histoVault.getLogChannelNumber(), histoVault.getLogFilePath()));
				localSizeSum_B += Paths.get(histoVault.getLogFilePath()).toFile().length();
			}
		}
		return localSizeSum_B;
	}

	private void putVault(HistoVault histoVault) {
		List<HistoVault> timeStampHistoVaults = this.get(histoVault.getLogStartTimestamp_ms());
		if (timeStampHistoVaults == null) {
			this.put(histoVault.getLogStartTimestamp_ms(), timeStampHistoVaults = new ArrayList<HistoVault>());
		}
		timeStampHistoVaults.add(histoVault);
	}

	public void setHistoFilePaths4Test(Path filePath, int subDirLevelMax, TreeMap<String, DeviceConfiguration> devices) throws IOException, NotSupportedFileFormatException {
		this.directoryScanner.setHistoFilePaths4Test(filePath, subDirLevelMax, devices);
	}

	public void rebuild4Test(TreeMap<String, DeviceConfiguration> deviceConfigurations) throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		this.histoSetCollector.rebuild4Test(deviceConfigurations);
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.histoSetCollector.getTrailRecordSet();
	}

	/**
	 * Toggles the exclusion directory definition and cleans exclusion directories.
	 * @param isDataSettingsAtHomePath true if the history data settings are stored in the user's home path
	 */
	public synchronized void setDataSettingsAtHomePath(boolean isDataSettingsAtHomePath) {
		if (this.settings.isDataSettingsAtHomePath() != isDataSettingsAtHomePath) {
			ArrayList<Path> dataPaths = new ArrayList<Path>();
			dataPaths.add(Paths.get(this.settings.getDataFilePath()));
			ExclusionData.deleteExclusionsDirectory(dataPaths);

			this.settings.setDataSettingsAtHomePath(isDataSettingsAtHomePath);
		}
	}

	/**
	 * @return the validatedDirectories which hold the history recordsets
	 */
	public Map<DirectoryType, Path> getValidatedDirectories() {
		return this.directoryScanner.getValidatedDirectories();
	}

	public Map<String, VaultCollector> getSuppressedTrusses() {
		return this.directoryScanner.getSuppressedTrusses();
	}

	public Map<String, VaultCollector> getUnsuppressedTrusses() {
		return this.directoryScanner.getUnsuppressedTrusses();
	}

	public DirectoryScanner getDirectoryScanner() {
		return this.directoryScanner;
	}

	public HistoSetCollector getHistoSetCollector() {
		return this.histoSetCollector;
	}

	public String getDirectoryScanStatistics() {
		return Messages.getString(MessageIds.GDE_MSGI0064,
				new Object[] { String.format("%,d", this.directoryScanner.getDirectoryFilesCount()), String.format("%,d", this.directoryScanner.getSelectedFilesCount()), //
						String.format("%,d", this.directoryScanner.getSuppressedTrusses().size() +  this.directoryScanner.getUnsuppressedTrusses().size()), String.format("%,d", this.directoryScanner.getUnsuppressedTrusses().size()), //
						String.format("%,d",  this.histoSetCollector.getMatchingTrussesCount()), String.format("%,d", this.histoSetCollector.getAvailableTrussesCount()), //
						String.format("%.2f", this.histoSetCollector.getElapsedTime_ms() / 1000.) });

	}
}
