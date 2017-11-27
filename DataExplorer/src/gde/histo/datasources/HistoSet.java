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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gde.config.Settings;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.exception.NotSupportedFileFormatException;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.exclusions.ExclusionData;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;

/**
 * Facade of the history module.
 * Supports the selection of histo vaults and provides a trail recordset based on the vaults.
 * @author Thomas Eickert
 */
public final class HistoSet {
	private final static String			$CLASS_NAME														= HistoSet.class.getName();
	@SuppressWarnings("unused")
	private final static Logger			log																		= Logger.getLogger($CLASS_NAME);

	private final Settings					settings															= Settings.getInstance();

	private final HistoSetCollector	histoSetCollector;

	/**
	 * We allow 1 lower and 1 upper outlier for a log with 740 measurements
	 */
	public static final double			OUTLIER_SIGMA_DEFAULT									= 3.;
	/**
	 * Specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Tolerance interval: <em>TI = &plusmn; z * &sigma; with z >= 0</em><br>
	 * Outliers are identified only if they lie beyond this limit.
	 */
	public static final double			OUTLIER_RANGE_FACTOR_DEFAULT					= 2.;
	/**
	 * Outlier detection for the summary graphics.
	 * We allow 1 outlier for 6 vaults.
	 */
	public static final double			SUMMARY_OUTLIER_SIGMA_DEFAULT					= 1.36;
	/**
	 * Specifies the outlier distance limit ODL from the tolerance interval (<em>ODL = &rho; * TI with &rho; > 0</em>).<br>
	 * Tolerance interval: <em>TI = &plusmn; z * &sigma; with z >= 0</em><br>
	 * Outliers are identified only if they lie beyond this limit.
	 */
	public static final double			SUMMARY_OUTLIER_RANGE_FACTOR_DEFAULT	= 2.;

	/**
	 * Defines the first step during rebuilding the histoset data.
	 * A minimum of steps may be selected for performance reasons.
	 */
	public enum RebuildStep {
		/**
		 * starts from scratch on
		 */
		A_HISTOSET(5),
		/**
		 * starts building the histo vaults
		 */
		B_HISTOVAULTS(4),
		/**
		 * starts building the trail recordset from the histo vaults
		 */
		C_TRAILRECORDSET(3),
		/**
		 * starts refreshing the trail data from the histo vaults
		 */
		D_TRAIL_DATA(2),
		/**
		 * starts updating the graphics and table
		 */
		E_USER_INTERFACE(1),
		/**
		 * starts with a file check only which decides which update activity is required --- not implemented
		 */
		F_FILE_CHECK(0);

		/** zero is the lowest scopeOfWork. */
		public final int					scopeOfWork;

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static RebuildStep	VALUES[]	= values();

		private RebuildStep(int scopeOfWork) {
			this.scopeOfWork = scopeOfWork;
		}
	};

	public HistoSet() {
		this.histoSetCollector = new HistoSetCollector();
		this.histoSetCollector.initialize();
	}

	/**
	 * Determine histo files, build a recordset based job list and read from the log file or the cache for each job.
	 * Populate the trail recordset.
	 * Disregard rebuild steps if histo file paths have changed which may occur if new files have been added by the user
	 * or the device, channel or object was modified.
	 * @param rebuildStep
	 * @param isWithUi true allows actions on the user interface (progress bar, message boxes)
	 * @return true if the HistoSet was rebuilt
	 */
	public synchronized boolean rebuild4Screening(RebuildStep rebuildStep, boolean isWithUi) //
			throws FileNotFoundException, IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		return this.histoSetCollector.rebuild4Screening(rebuildStep, isWithUi);
	}

	public void rebuild4Test(Path filePath, TreeMap<String, DeviceConfiguration> devices) //
			throws IOException, NotSupportedFileFormatException, DataInconsitsentException, DataTypeException {
		this.histoSetCollector.rebuild4Test(filePath, devices);
	}

	public TrailRecordSet getTrailRecordSet() {
		return this.histoSetCollector.getTrailRecordSet();
	}

	/**
	 * Is thread safe with respect to concurrent rebuilds.
	 */
	public synchronized void cleanExclusionData() {
		ArrayList<Path> dataPaths = new ArrayList<Path>();
		dataPaths.add(Paths.get(this.settings.getDataFilePath()));
		ExclusionData.deleteExclusionsDirectory(dataPaths);
	}

	/**
	 * @return the validatedDirectories which hold the history recordsets
	 */
	public Map<DirectoryType, Path> getValidatedDirectories() {
		return this.histoSetCollector.getValidatedDirectories();
	}

	/**
	 * @return the paths which have been ignored on a file basis or suppressed on a recordset basis
	 */
	public List<Path> getExcludedPaths() {
		List<Path> result = this.histoSetCollector.getExcludedFiles();
		for (VaultCollector truss : this.histoSetCollector.getSuppressedTrusses()) {
			result.add(truss.getVault().getLogFileAsPath());
		}
		return result;
	}

	public String getDirectoryScanStatistics() {
		return Messages.getString(MessageIds.GDE_MSGI0064, //
				new Object[] { String.format("%,d", this.histoSetCollector.getDirectoryFilesCount()), //
						String.format("%,d", this.histoSetCollector.getReadFilesCount()), //
						String.format("%.2f", this.histoSetCollector.getRecordSetBytesSum() / 1024 / 1024.), //
						String.format("%.2f", this.histoSetCollector.getElapsedTime_ms() / 1000.), //
						String.format("%,d", this.histoSetCollector.getTrussesCount()), //
						String.format("%,d", this.histoSetCollector.getTimeStepSize()) });

	}
}
