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
import java.util.function.BiFunction;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.TrailRecordSet.DataTag;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * Trail record data mapping for the histo table tab.
 * @author Thomas Eickert (USER)
 */
public final class HistoTableMapper {

	public enum DisplayTag {
		FILE_NAME {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> Paths.get(t.get(DataTag.FILE_PATH).get(i)) //
						.getFileName().toString();
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0838);
				return dataTableRow;
			}
		},
		DIRECTORY_NAME {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> Paths.get(t.get(DataTag.FILE_PATH).get(i)) //
						.getParent().getFileName().toString();
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0839);
				return dataTableRow;
			}

		},
		BASE_PATH {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> Paths.get(t.get(DataTag.FILE_PATH).get(i)) //
						.getParent().toString();
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0840);
				return dataTableRow;
			}

		},
		CHANNEL_NUMBER {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> t.get(DataTag.CHANNEL_NUMBER).get(i);
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0841);
				return dataTableRow;
			}

		},
		RECTIFIED_OBJECTKEY {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> t.get(DataTag.RECTIFIED_OBJECTKEY).get(i);
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0842);
				return dataTableRow;
			}

		},
		RECORDSET_BASE_NAME {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> t.get(DataTag.RECORDSET_BASE_NAME).get(i);
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0844);
				return dataTableRow;
			}

		},
		GPS_LOCATION {
			@Override
			String[] getTableTagRow(TrailRecordSet trailRecordSet) {
				BiFunction<TrailDataTags, Integer, String> scoreFunction = (t, i) -> t.get(DataTag.GPS_LOCATION).get(i);
				String[] dataTableRow = getTagRowWithValues(trailRecordSet, scoreFunction);

				dataTableRow[1] = Messages.getString(MessageIds.GDE_MSGT0845);
				return dataTableRow;
			}

		};

		/** use this instead of values() to avoid repeatedly cloning actions. */
		public static final DisplayTag VALUES[] = values();

		static DisplayTag fromOrdinal(int ordinal) {
			return DisplayTag.VALUES[ordinal];
		}

		/**
		 * @param trailRecordSet provides the values
		 * @return a new data table row populated with the header column and the value columns
		 */
		abstract String[] getTableTagRow(TrailRecordSet trailRecordSet);

		/**
		 * @param function provides the value based on two parameters:</br>
		 *          {@code TrailDataTags} references all the values</br>
		 *          {@code Integer} references the index used for accessing the value
		 * @return a new tag table row with an empty cell followed by the tag text and the value cells
		 */
		private static String[] getTagRowWithValues(TrailRecordSet trailRecordSet, BiFunction<TrailDataTags, Integer, String> scoreFunction) {
			int dataSize = trailRecordSet.getTimeStepSize();
			String[] dataTableRow = new String[dataSize + 2];
			if (Settings.getInstance().isXAxisReversed()) {
				for (int i = 0; i < dataSize; i++)
					dataTableRow[i + 2] = scoreFunction.apply(trailRecordSet.getDataTags(), i);
			} else {
				for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
					dataTableRow[i + 2] = scoreFunction.apply(trailRecordSet.getDataTags(), j);
			}
			return dataTableRow;
		}

	}

	/**
	 * @return the column headers starting with the first data column </br>
	 *         (and is thus 2 cells shorter than a value row)
	 */
	public static String[] getTableHeaderRow(TrailRecordSet trailRecordSet) {
		int dataSize = trailRecordSet.getTimeStepSize();
		String[] dataTableRow = new String[dataSize];
		if (Settings.getInstance().isXAxisReversed()) {
			for (int i = 0; i < dataSize; i++)
				dataTableRow[i] = LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, trailRecordSet.getTime_ms(i));
		} else {
			for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
				dataTableRow[i] = LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, trailRecordSet.getTime_ms(j));
		}

		return dataTableRow;
	}

	/**
	 * Get all calculated and formated data table points.
	 * @return record name and trail text followed by formatted values as string array
	 */
	public static String[] getTableRow(TrailRecord trailRecord) {
		int dataSize = trailRecord.getParentTrail().getTimeStepSize();
		String[] dataTableRow = new String[dataSize + 2];

		BiFunction<TrailRecord, Integer, String> valueFunction = trailRecord.getTrailSelector().isTrailSuite() //
				? (r, i) -> getSuiteCellValue(r, i) //
				: (r, i) -> getSingleCellValue(r, i);

		if (Settings.getInstance().isXAxisReversed()) {
			for (int i = 0; i < dataSize; i++)
				dataTableRow[i + 2] = valueFunction.apply(trailRecord, i);
		} else {
			for (int i = 0, j = dataSize - 1; i < dataSize; i++, j--)
				dataTableRow[i + 2] = valueFunction.apply(trailRecord, j);
		}
		dataTableRow[0] = trailRecord.getTableRowHeader().intern();
		dataTableRow[1] = trailRecord.getTrailSelector().getTrailText().intern();

		return dataTableRow;
	}

	private static String getSingleCellValue(TrailRecord trailRecord, int index) {
		String cellValue = "";
		if (trailRecord.elementAt(index) != null) {
			TrailRecordFormatter formatter = new TrailRecordFormatter(trailRecord);
			cellValue = formatter.getTableValue(index);
		}
		return cellValue;
	}

	private static String getSuiteCellValue(TrailRecord trailRecord, int index) {
		String cellValue = "";
		TrailSelector selector = trailRecord.getTrailSelector();
		if (trailRecord.getSuiteRecords().getSuiteValue(selector.getTrailType().getSuiteMasterIndex(), index) != null) {
			StringBuilder sb = new StringBuilder();
			TrailRecordFormatter formatter = new TrailRecordFormatter(trailRecord);
			sb.append(trunc(formatter.getTableValue(selector.getTrailType().getSuiteLowerIndex(), index), 8));
			String d = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK;
			sb.append(d).append(trunc(formatter.getTableValue(selector.getTrailType().getSuiteMasterIndex(), index), 8));
			sb.append(d).append(trunc(formatter.getTableValue(selector.getTrailType().getSuiteUpperIndex(), index), 8));
			cellValue = sb.toString().intern();
		}
		return cellValue;
	}

	/**
	 * Performant replacement for String.format("%.8s", ss)
	 */
	private static String trunc(String ss, int maxLength) {
		return ss.substring(0, Math.min(maxLength, ss.length()));
	}

	/**
	 * Get the tags row.
	 * Please note that the GPS location tags are filled in asynchronously.
	 * @param displayTag
	 * @return empty record name and display tag description as a trail text replacement followed by the tag values
	 */
	public static String[] getTableTagRow(TrailRecordSet trailRecordSet, DisplayTag displayTag) {
		return displayTag.getTableTagRow(trailRecordSet);
	}

}
