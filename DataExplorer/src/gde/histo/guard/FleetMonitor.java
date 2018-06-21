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

package gde.histo.guard;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import gde.config.Settings;
import gde.device.IChannelItem;
import gde.device.IDevice;
import gde.device.ScoreLabelTypes;
import gde.histo.cache.HistoVault;
import gde.histo.cache.SimpleVaultReader;
import gde.histo.config.HistoGraphicsTemplate;
import gde.histo.datasources.SourceFolders.DirectoryType;
import gde.histo.datasources.VaultChecker;
import gde.histo.device.ChannelItems;
import gde.histo.exclusions.InclusionData;
import gde.histo.guard.ObjectVaultIndex.DetailSelector;
import gde.histo.guard.ObjectVaultIndex.ObjectVaultIndexEntry;
import gde.histo.guard.ObjectVaultIndex.VaultKeyPair;
import gde.histo.guard.Reminder.ReminderType;
import gde.histo.recordings.TrailSelector;
import gde.log.Level;
import gde.log.Logger;

/**
 * Aggregated data for object keys and sub-levels like device, channel and measurement / settlement / score.
 * Based on vaults (no access to log files).
 * @author Thomas Eickert (USER)
 */
public final class FleetMonitor {
	private static final String	$CLASS_NAME	= FleetMonitor.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	static final class ObjectSummary {

		String					objectKey;
		String					vaultDeviceName;

		int							vaultCount					= 0;
		long						minFileLastModified	= Long.MAX_VALUE;
		long						maxFileLastModified	= 0;
		long						minStartTimestampMs	= Long.MAX_VALUE;
		long						maxStartTimestampMs	= 0;
		int							minDuration_MM			= Integer.MAX_VALUE;
		int							maxDuration_MM			= 0;
		ReminderType[]	minReminders				= new ReminderType[] { ReminderType.NONE, ReminderType.NONE };
		ReminderType[]	maxReminders				= new ReminderType[] { ReminderType.NONE, ReminderType.NONE };

		/**
		 * Supports arbitrary selection of vaults, e.g. mixed channels
		 * @param vaults
		 */
		public static final ObjectSummary createObjectSummary(String objectKey, String deviceName, List<HistoVault> vaults) {
			ObjectSummary oS = new ObjectSummary();
			oS.objectKey = objectKey;
			oS.vaultDeviceName = deviceName;

			Map<Integer, TreeSet<HistoVault>> channelVaults = new HashMap<>();
			for (HistoVault v : vaults) {
				if (channelVaults.get(v.getLogChannelNumber()) == null) {
					channelVaults.put(v.getLogChannelNumber(), //
							new TreeSet<HistoVault>((v1, v2) -> Long.compare(v2.getLogStartTimestamp_ms(), v1.getLogStartTimestamp_ms()))); // reversed !
				}
				channelVaults.get(v.getLogChannelNumber()).add(v);
			}
			for (Entry<Integer, TreeSet<HistoVault>> e : channelVaults.entrySet()) {
				HistoVault[] indexedVaults = e.getValue().toArray(new HistoVault[e.getValue().size()]);
				ObjectSummary channelSummary = createChannelSummary(objectKey, deviceName, e.getKey(), indexedVaults);
				oS.vaultCount += channelSummary.vaultCount;
				oS.minFileLastModified = Math.max(oS.minFileLastModified, channelSummary.minFileLastModified);
				oS.maxFileLastModified = Math.max(oS.maxFileLastModified, channelSummary.maxFileLastModified);
				oS.minStartTimestampMs = Math.max(oS.minStartTimestampMs, channelSummary.minStartTimestampMs);
				oS.maxStartTimestampMs = Math.max(oS.maxStartTimestampMs, channelSummary.maxStartTimestampMs);
				oS.minDuration_MM = Math.min(oS.minDuration_MM, channelSummary.minDuration_MM);
				oS.maxDuration_MM = Math.max(oS.maxDuration_MM, channelSummary.maxDuration_MM);
				oS.minReminders[0] = ReminderType.min(oS.minReminders[0], oS.minReminders[0]);
				oS.maxReminders[1] = ReminderType.max(oS.maxReminders[1], oS.maxReminders[1]);
				oS.minReminders[0] = ReminderType.min(oS.minReminders[0], oS.minReminders[0]);
				oS.maxReminders[1] = ReminderType.max(oS.maxReminders[1], oS.maxReminders[1]);
			}
			return oS;
		}

		/**
		 * Supports aggregation for objectKey + device + channel.
		 * @param vaults in start timestamp reverse order
		 */
		private static final ObjectSummary createChannelSummary(String objectKey, String deviceName, int channelNumber, HistoVault[] indexedVaults) {
			ObjectSummary oS = new ObjectSummary();
			oS.objectKey = objectKey;
			oS.vaultDeviceName = deviceName;
			oS.vaultCount = indexedVaults.length;
			for (HistoVault v : indexedVaults) {
				oS.minFileLastModified = Math.min(oS.minFileLastModified, v.getLogFileLastModified());
				oS.maxFileLastModified = Math.max(oS.maxFileLastModified, v.getLogFileLastModified());
				oS.minStartTimestampMs = Math.min(oS.minStartTimestampMs, v.getLogStartTimestamp_ms());
				oS.maxStartTimestampMs = Math.max(oS.maxStartTimestampMs, v.getLogStartTimestamp_ms());
				oS.minDuration_MM = Math.min(oS.minDuration_MM, v.getScores().get(ScoreLabelTypes.DURATION_MM.ordinal()).getValue());
				oS.maxDuration_MM = Math.max(oS.maxDuration_MM, v.getScores().get(ScoreLabelTypes.DURATION_MM.ordinal()).getValue());
			}

			HistoGraphicsTemplate template = HistoGraphicsTemplate.createReadonlyTemplate(deviceName, channelNumber, objectKey);
			template.load();
			boolean smartStatistics = Boolean.parseBoolean(template.getProperty(HistoGraphicsTemplate.SMART_STATISTICS, "true"));

			InclusionData inclusionData = new InclusionData(Paths.get(Settings.getInstance().getDataFilePath(), objectKey));

			ReminderType[] minReminders = new ReminderType[] { ReminderType.NONE, ReminderType.NONE };
			ReminderType[] maxReminders = new ReminderType[] { ReminderType.NONE, ReminderType.NONE };
			int logLimit = Settings.getInstance().getReminderCount();
			BiConsumer<Integer, IChannelItem> channelItemAction = (idx, itm) -> {
				boolean isActive = Boolean.parseBoolean(template.getRecordProperty(itm.getName(), HistoGraphicsTemplate.IS_ACTIVE, "true")); // todo recordName Jeti
				boolean isIncluded = inclusionData.isIncluded(itm.getName(), true); // todo recordName Jeti
				if (!isActive || !isIncluded) return;

				TrailSelector trailSelector = itm.createTrailSelector(deviceName, channelNumber, itm.getName(), smartStatistics); // todo recordName Jeti
				Reminder[] minMaxReminder = Guardian.defineMinMaxReminder(indexedVaults, itm, trailSelector, logLimit);
				if (minMaxReminder[0] != null) {
					minMaxReminder[0] = minMaxReminder[0];
					if (minMaxReminder[0].getReminderType().ordinal() > maxReminders[0].ordinal())
						log.off(() -> "idx=" + idx + " " + itm.getName() + ":  low  warning increased " + minMaxReminder[0].getReminderType());
					minReminders[0] = ReminderType.min(minReminders[0], minMaxReminder[0].getReminderType());
					maxReminders[0] = ReminderType.max(maxReminders[0], minMaxReminder[0].getReminderType());
				}
				if (minMaxReminder[1] != null) {
					minMaxReminder[1] = minMaxReminder[0];
					if (minMaxReminder[1].getReminderType().ordinal() > maxReminders[1].ordinal())
						log.off(() -> "idx=" + idx + " " + itm.getName() + ":  high warning increased " + minMaxReminder[1].getReminderType());
					minReminders[1] = ReminderType.min(minReminders[1], minMaxReminder[1].getReminderType());
					maxReminders[1] = ReminderType.max(maxReminders[1], minMaxReminder[1].getReminderType());
				}
			};

			ChannelItems channelItems = new ChannelItems(deviceName, channelNumber);
			channelItems.processItems(channelItemAction, channelItemAction, channelItemAction);
			oS.minReminders = minReminders;
			oS.maxReminders = maxReminders;
			return oS;
		}

		@Override
		public String toString() {
			return "ObjectSummary [objectKey=" + this.objectKey + ", vaultDeviceName=" + this.vaultDeviceName + ", maxFileLastModified=" + this.maxFileLastModified + ", maxStartTimestampMs=" + this.maxStartTimestampMs + ", maxDuration_MM=" + this.maxDuration_MM + ", maxWarning=" + Arrays.toString(this.maxReminders) + ", vaultCount=" + this.vaultCount + "]";
		}
	}

	static final class ObjectDetail {

		String	objectKey;

		String	vaultDeviceName;
		int			vaultChannelNumber;

		String	logFilePath;
		long		logFileLastModified;
		String	logRecordsetBaseName;
		int			logChannelNumber;
		long		logStartTimestampMs;

	}

	/**
	 * Initialize the objectVault index.
	 */
	public FleetMonitor() {
		new ObjectVaultIndex().rebuild();
	}

	FleetMonitor(boolean rebuildIndex) {
		if (rebuildIndex) new ObjectVaultIndex().rebuild();
	}

	/**
	 * @return the object summaries per device consisting of aggregated vault data and object base data
	 */
	public List<ObjectSummary> defineOverview(String objectKey) {
		List<ObjectSummary> summaries = new ArrayList<>();

		ObjectVaultIndex objectVaultIndex = new ObjectVaultIndex(new String[] { objectKey });
		Map<String, IDevice> usedDevices = objectVaultIndex.selectExistingDevices(new String[] { objectKey });
		log.log(Level.OFF, "devices", usedDevices);

		DetailSelector detailSelector = DetailSelector.createDeviceNameFilter(usedDevices.keySet());
		Function<ObjectVaultIndexEntry, String> classifier = e -> e.vaultDeviceName;
		TreeMap<String, List<VaultKeyPair>> vaultKeys = objectVaultIndex.selectGroupedVaultKeys(objectKey, detailSelector, classifier);
		for (Entry<String, List<VaultKeyPair>> e : vaultKeys.entrySet()) {
			IDevice device = usedDevices.get(e.getKey());
			EnumSet<DirectoryType> directoryTypes = DirectoryType.getValidDirectoryTypes(device);
			VaultChecker checker = new VaultChecker(device, directoryTypes, objectKey);

			List<HistoVault> allVaults = SimpleVaultReader.readVaults(e.getValue());
			List<HistoVault> validVaults = new ArrayList<>();
			for (HistoVault vault : allVaults) {
				if (checker.isValidVault(vault)) validVaults.add(vault);
			}
			log.log(Level.OFF, "valid vaults size=", validVaults.size());

			summaries.add(ObjectSummary.createObjectSummary(objectKey, device.getName(), validVaults));
		}
		log.off(() -> summaries.toString());
		return summaries;
	}

}
