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
import java.util.logging.Logger;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.TrailRecordSet.DataTag;
import gde.histo.recordings.TrailRecordSet.DisplayTag;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * Trail record data mapping for the histo table tab.
 * @author Thomas Eickert (USER)
 */
public final class HistoTableMapper {
	private final static String		$CLASS_NAME	= HistoTableMapper.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

	/**
	 * @return the column headers starting with the first data column
	 */
	public static String[] getTableHeaderRow(TrailRecordSet trailRecordSet) {
		int dataSize = trailRecordSet.getTimeStepSize();
		String[] headerRow = new String[dataSize];
		if (HistoTableMapper.settings.isXAxisReversed()) {
			for (int i = 0; i < dataSize; i++) {
				StringBuilder sb = new StringBuilder();
				sb.append(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, trailRecordSet.getTime_ms(i)));
				headerRow[i] = sb.toString();
			}
		}
		else {
			for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--) {
				StringBuilder sb = new StringBuilder();
				sb.append(LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, trailRecordSet.getTime_ms(j)));
				headerRow[j] = sb.toString();
			}
		}
		return headerRow;
	}

	public static String getTableRowText(TrailRecord trailRecord) {
		return trailRecord.getUnit().length() > 0 ? (trailRecord.getNameReplacement() + GDE.STRING_BLANK_LEFT_BRACKET + trailRecord.getUnit() + GDE.STRING_RIGHT_BRACKET).intern()
				: trailRecord.getNameReplacement().intern();
	}

	/**
	 * Get all calculated and formated data table points.
	 * @return record name and trail text followed by formatted values as string array
	 */
	public static String[] getTableRow(TrailRecord trailRecord) {
		TrailRecordFormatter formatter = new TrailRecordFormatter(trailRecord);
		TrailSelector selector = trailRecord.getTrailSelector();

		final String[] dataTableRow = new String[trailRecord.realSize() + 2];
		dataTableRow[0] = getTableRowText(trailRecord).intern();
		dataTableRow[1] = selector.getTrailText().intern();
		if (!selector.isTrailSuite()) {
			if (HistoTableMapper.settings.isXAxisReversed()) {
				for (int i = 0; i < trailRecord.realSize(); i++)
					if (trailRecord.elementAt(i) != null) dataTableRow[i + 2] = formatter.getTableValue(i);
			}
			else {
				for (int i = 0, j = trailRecord.realSize() - 1; i < trailRecord.realSize(); i++, j--)
					if (trailRecord.elementAt(j) != null) dataTableRow[i + 2] = formatter.getTableValue(j);
			}
		}
		else {
			if (HistoTableMapper.settings.isXAxisReversed()) {
				for (int i = 0; i < trailRecord.realSize(); i++) {
					if (trailRecord.getSuiteRecords().getSuiteValue(selector.getTrailType().getSuiteMasterIndex(), i) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%.8s", formatter.getTableValue(selector.getTrailType().getSuiteLowerIndex(), i))); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(String.format("%.8s", formatter.getTableValue(selector.getTrailType().getSuiteMasterIndex(), i))); //$NON-NLS-1$
						sb.append(delimiter).append(String.format("%.8s", formatter.getTableValue(selector.getTrailType().getSuiteUpperIndex(), i))); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
				}
			}
			else {
				for (int i = 0, j = trailRecord.realSize() - 1; i < trailRecord.realSize(); i++, j--)
					if (trailRecord.getSuiteRecords().getSuiteValue(selector.getTrailType().getSuiteMasterIndex(), j) != null) {
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%.8s", formatter.getTableValue(selector.getTrailType().getSuiteLowerIndex(), i))); //$NON-NLS-1$
						String delimiter = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
						sb.append(delimiter).append(String.format("%.8s", formatter.getTableValue(selector.getTrailType().getSuiteMasterIndex(), i))); //$NON-NLS-1$
						sb.append(delimiter).append(String.format("%.8s", formatter.getTableValue(selector.getTrailType().getSuiteUpperIndex(), i))); //$NON-NLS-1$
						dataTableRow[i + 2] = sb.toString().intern();
					}
			}
		}

		return dataTableRow;
	}

	/**
	 * Get all tags for all recordsets / vaults.
	 * Please note that the GPS location tags are filled in asynchronously.
	 * @param displayTag
	 * @return empty record name and display tag description as a trail text replacement followed by the tag values
	 */
	public static String[] getTableTagRow(TrailRecordSet trailRecordSet, DisplayTag displayTag) {
		TrailDataTags dataTags = trailRecordSet.getDataTags();
		int dataSize = trailRecordSet.getTimeStepSize();
		String[] dataTableRow = new String[dataSize + 2];

		if (dataSize > 0) {
			if (displayTag == DisplayTag.FILE_NAME)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0838);
			else if (displayTag == DisplayTag.DIRECTORY_NAME)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0839);
			else if (displayTag == DisplayTag.BASE_PATH)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0840);
			else if (displayTag == DisplayTag.CHANNEL_NUMBER)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0841);
			else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0842);
			else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0844);
			else if (displayTag == DisplayTag.GPS_LOCATION) {
				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0845);
			}
			else
				throw new UnsupportedOperationException();

			if (HistoTableMapper.settings.isXAxisReversed()) {
				if (displayTag == DisplayTag.FILE_NAME)
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = Paths.get(dataTags.get(DataTag.FILE_PATH).get(i)).getFileName().toString();
				else if (displayTag == DisplayTag.DIRECTORY_NAME)
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = Paths.get(dataTags.get(DataTag.FILE_PATH).get(i)).getParent().getFileName().toString();
				else if (displayTag == DisplayTag.BASE_PATH)
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = Paths.get(dataTags.get(DataTag.FILE_PATH).get(i)).getParent().toString();
				else if (displayTag == DisplayTag.CHANNEL_NUMBER)
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = dataTags.get(DataTag.CHANNEL_NUMBER).get(i);
				else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = dataTags.get(DataTag.RECTIFIED_OBJECTKEY).get(i);
				else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = dataTags.get(DataTag.RECORDSET_BASE_NAME).get(i);
				else if (displayTag == DisplayTag.GPS_LOCATION) {
					for (int i = 0; i < dataSize; i++)
						dataTableRow[i + 2] = dataTags.get(DataTag.GPS_LOCATION).get(i);
				}
				else
					dataTableRow = null; // for test only
			}
			else {
				if (displayTag == DisplayTag.FILE_NAME)
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = Paths.get(dataTags.get(DataTag.FILE_PATH).get(j)).getFileName().toString();
				else if (displayTag == DisplayTag.DIRECTORY_NAME)
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = Paths.get(dataTags.get(DataTag.FILE_PATH).get(j)).getParent().getFileName().toString();
				else if (displayTag == DisplayTag.BASE_PATH)
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = Paths.get(dataTags.get(DataTag.FILE_PATH).get(j)).getParent().toString();
				else if (displayTag == DisplayTag.CHANNEL_NUMBER)
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = dataTags.get(DataTag.CHANNEL_NUMBER).get(j);
				else if (displayTag == DisplayTag.RECTIFIED_OBJECTKEY)
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = dataTags.get(DataTag.RECTIFIED_OBJECTKEY).get(j);
				else if (displayTag == DisplayTag.RECORDSET_BASE_NAME)
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = dataTags.get(DataTag.RECORDSET_BASE_NAME).get(j);
				else if (displayTag == DisplayTag.GPS_LOCATION) {
					for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
						dataTableRow[i + 2] = dataTags.get(DataTag.GPS_LOCATION).get(j);
				}
				else
					dataTableRow = null; // for test only
			}
		}
		return dataTableRow;
	}

}
