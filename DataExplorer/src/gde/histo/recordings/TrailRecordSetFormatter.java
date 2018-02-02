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

import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.Collectors;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.StringHelper;

/**
 * Formatting methods.
 * @author Thomas Eickert (USER)
 */
public final class TrailRecordSetFormatter {

	private static final Settings	settings	= Settings.getInstance();
	private static final Channels	channels	= Channels.getInstance();

	public static String getSelectedMeasurementsAsTable(long timestamp_ms) {
		Properties displayProps = settings.getMeasurementDisplayProperties();
		TrailRecordSet trailRecordSet = getTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Vector<TrailRecord> records = trailRecordSet.getVisibleAndDisplayableRecords();

			StringBuilder sb = new StringBuilder().append(String.format("%-11.11s", Messages.getString(MessageIds.GDE_MSGT0799))); //$NON-NLS-1$
			sb.append(GDE.STRING_OR).append(String.format("%-16s", Messages.getString(MessageIds.GDE_MSGT0652))); //$NON-NLS-1$
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = records.get(i);
				if (displayProps.getProperty(record.getName()) != null)
					sb.append(GDE.STRING_OR).append(String.format("%-10s", displayProps.getProperty(record.getName()))); //$NON-NLS-1$
				else {
					final String unit = GDE.STRING_LEFT_BRACKET + record.getUnit() + GDE.STRING_RIGHT_BRACKET;
					final String name = record.getName().substring(0, record.getName().length() >= 10 - unit.length() ? 10 - unit.length()
							: record.getName().length());
					final String format = "%-" + (10 - unit.length()) + "s%" + unit.length() + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					sb.append(GDE.STRING_OR).append(String.format(format, name, unit));
				}
			}
			sb.append(GDE.STRING_OR).append(GDE.LINE_SEPARATOR);

			final int index = trailRecordSet.getIndex(timestamp_ms);
			sb.append(String.format("%-11.11s", trailRecordSet.getDataTags().getByIndex(index).get(DataTag.RECORDSET_BASE_NAME))); //$NON-NLS-1$
			sb.append(GDE.STRING_OR).append(String.format("%-16s", LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmm, timestamp_ms)).substring(0, 16)); //$NON-NLS-1$
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = records.get(i);
				sb.append(GDE.STRING_OR).append(String.format("%.10s", StringHelper.center(new TrailRecordFormatter(record).getMeasureValue(index), 10))); //$NON-NLS-1$
			}
			return sb.append(GDE.STRING_OR).toString();
		} else {
			return GDE.STRING_EMPTY;
		}
	}

	public static String getFileNameLines(List<Integer> indices) {
		return indices.stream().sorted() //
				.map(i -> Paths.get(getTrailRecordSet().getDataTags(i).get(DataTag.FILE_PATH))) //
				.map(p -> p.getFileName().toString()) //
				.collect(Collectors.joining(GDE.STRING_NEW_LINE));
	}

	private static TrailRecordSet getTrailRecordSet() {
		TrailRecordSet trailRecordSet = null;
		if (channels.getActiveChannel() != null) {
			trailRecordSet = DataExplorer.getInstance().getPresentHistoExplorer().getTrailRecordSet();
		}
		return trailRecordSet;
	}

}
