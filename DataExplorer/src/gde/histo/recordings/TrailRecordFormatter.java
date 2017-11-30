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

import java.text.DecimalFormat;
import java.util.Vector;

import gde.GDE;
import gde.device.IDevice;
import gde.device.TrailTypes;
import gde.histo.datasources.HistoSet;
import gde.ui.DataExplorer;
import gde.utils.StringHelper;

/**
 * Output formatting based on trailRecord data.
 * @author Thomas Eickert (USER)
 */
public final class TrailRecordFormatter {

	/**
	 * Performant replacement for String.format("%.8s", ss)
	 */
	private static String trunc(String ss, int maxLength) {
		return ss.substring(0, Math.min(maxLength, ss.length()));
	}

	private final IDevice			device	= DataExplorer.getInstance().getActiveDevice();

	private final TrailRecord	trailRecord;

	public TrailRecordFormatter(TrailRecord trailRecord) {
		this.trailRecord = trailRecord;
	}

	/**
	 * Define category based on maxValueAbs AND minValueAbs.
	 * @param newNumberFormat
	 */
	public DecimalFormat getDecimalFormat(int newNumberFormat) {
		DecimalFormat df = new DecimalFormat();

		double maxScaleValue = this.trailRecord.getMaxScaleValue();
		double minScaleValue = this.trailRecord.getMinScaleValue();

		switch (newNumberFormat) {
		case -1:
			if (Math.abs(maxScaleValue) < 10 && Math.abs(minScaleValue) < 10) {
				if (maxScaleValue - minScaleValue < 0.01)
					df.applyPattern("0.0000"); //$NON-NLS-1$
				else if (maxScaleValue - minScaleValue < 0.1)
					df.applyPattern("0.000"); //$NON-NLS-1$
				else
					df.applyPattern("0.00"); //$NON-NLS-1$
			} else if (Math.abs(maxScaleValue) < 100 && Math.abs(minScaleValue) < 100) {
				if (maxScaleValue - minScaleValue < 0.1)
					df.applyPattern("0.000"); //$NON-NLS-1$
				else if (maxScaleValue - minScaleValue < 1.)
					df.applyPattern("0.00"); //$NON-NLS-1$
				else
					df.applyPattern("0.0"); //$NON-NLS-1$
			} else if (Math.abs(maxScaleValue) < 1000 && Math.abs(minScaleValue) < 1000) {
				if (maxScaleValue - minScaleValue < 1.)
					df.applyPattern("0.00"); //$NON-NLS-1$
				else if (maxScaleValue - minScaleValue < 10.)
					df.applyPattern("0.0"); //$NON-NLS-1$
				else
					df.applyPattern("0"); //$NON-NLS-1$
			} else if (Math.abs(maxScaleValue) < 10000 && Math.abs(minScaleValue) < 10000) {
				if (maxScaleValue - minScaleValue < 10.)
					df.applyPattern("0.0"); //$NON-NLS-1$
				else
					df.applyPattern("0"); //$NON-NLS-1$
			} else {
				df.applyPattern("0"); //$NON-NLS-1$
			}
			break;
		case 0:
			df.applyPattern("0"); //$NON-NLS-1$
			break;
		case 1:
			df.applyPattern("0.0"); //$NON-NLS-1$
			break;
		case 2:
		default:
			df.applyPattern("0.00"); //$NON-NLS-1$
			break;
		case 3:
			df.applyPattern("0.000"); //$NON-NLS-1$
			break;
		}

		return df;
	}

	/**
	 * @param value is the value to be displayed (without applying a factor or GPS coordinates fraction correction)
	 * @return the translated and decimal formatted value at the given index
	 */
	public String getScaleValue(double finalValue) {
		if (this.device.isGPSCoordinates(this.trailRecord)) {
			if (this.trailRecord.getUnit().endsWith("'")) //$NON-NLS-1$
				return StringHelper.getFormatedWithMinutes("%2d %04.1f", finalValue); //$NON-NLS-1$
			else
				return this.trailRecord.getDecimalFormat().format(finalValue);
		} else
			return this.trailRecord.getDecimalFormat().format(finalValue);
	}

	/**
	 * @param index
	 * @param trailType
	 * @return the formatted value also for GPS coordinates
	 */
	public String getTableSuiteValue(int index, TrailTypes trailType) {
		String cellValue;
		StringBuilder sb = new StringBuilder();
		sb.append(getTruncatedTableValue(trailType.getSuiteLowerIndex(), index, 8));
		String d = sb.length() > 3 ? Character.toString((char) 183) : GDE.STRING_BLANK_COLON_BLANK; // 183: middle dot
		sb.append(d).append(getTruncatedTableValue(trailType.getSuiteMasterIndex(), index, 8));
		sb.append(d).append(getTruncatedTableValue(trailType.getSuiteUpperIndex(), index, 8));
		cellValue = sb.toString();
		return cellValue;
	}

	/**
	 * @param suiteOrdinal
	 * @param index
	 * @return the formatted and truncated value also for GPS coordinates
	 */
	private String getTruncatedTableValue(int suiteOrdinal, int index, int length) {
		return trunc(getTableValue(this.trailRecord.getSuiteRecords().get(suiteOrdinal).get(index) / 1000.), length);
	}

	/**
	 * Supports suites and null values.
	 * @param index
	 * @return the translated and decimal formatted value at the given index or a standard string in case of a null value
	 */
	public String getMeasureValue(int index) {
		final Vector<Integer> points = this.trailRecord.getPoints();

		if (points.elementAt(index) != null) {
			return getTableValue(points.elementAt(index) / 1000.);
		} else
			return GDE.STRING_STAR;
	}

	/**
	 * @param value is the untranslated value (<em>intValue / 1000.</em>)
	 * @return the formatted value also for GPS coordinates
	 */
	public String getTableValue(double value) {
		final String formattedValue;
		if (this.device.isGPSCoordinates(this.trailRecord)) {
			// if (this.getDataType() == DataType.GPS_LATITUDE etc ???
			if (this.trailRecord.getUnit().endsWith("'")) { //$NON-NLS-1$
				formattedValue = StringHelper.getFormatedWithMinutes("%2d %07.4f", HistoSet.decodeVaultValue(this.trailRecord, value)).trim(); //$NON-NLS-1$
			} else {
				formattedValue = String.format("%8.6f", HistoSet.decodeVaultValue(this.trailRecord, value)); //$NON-NLS-1$
			}
		} else {
			formattedValue = this.trailRecord.getDecimalFormat().format(HistoSet.decodeVaultValue(this.trailRecord, value));
		}
		return formattedValue;
	}

	/**
	 * @param index
	 * @return the formatted value also for GPS coordinates
	 */
	public String getTableValue(int index) {
		return getTableValue(this.trailRecord.elementAt(index) / 1000.);
	}

}
