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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.utils;

import gde.GDE;
import gde.config.Settings;
import gde.data.HistoRecordSet;
import gde.data.HistoSet;
import gde.data.RecordSet;
import gde.histocache.HistoVault;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

/**
 * Utility class to draw time line with tick marks and numbers.
 * Supports logarithmic distances and elaborate x-axis point placement based on box width and number of points.
 * @author Thomas Eickert
 */
public class HistoTimeLine {
	private final static String	$CLASS_NAME	= HistoTimeLine.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final Settings			settings		= Settings.getInstance();
	private final HistoSet			histoSet		= HistoSet.getInstance();

	private enum TimeLine {
		MONTH_TO_DAY, MONTH_TO_HOUR, YEAR_TO_MONTH, DATE, DATE_TIME
	}

	private enum TimeFormat {
		MonthDay(new SimpleDateFormat("MM-dd")), MonthHour(new SimpleDateFormat("MM-dd HH")), YearMonth(new SimpleDateFormat("yyyy-MM")), Date(new SimpleDateFormat("yyyy-MM-dd")), DateTime(
				new SimpleDateFormat("yyyy-MM-dd HH:mm"));

		private final SimpleDateFormat	simpleDateFormat;
		/**
		 * use this to avoid repeatedly cloning actions instead of values()
		 */
		public final static TimeFormat	values[]	= values();	

		private TimeFormat(SimpleDateFormat simpleDateFormat) {
			this.simpleDateFormat = simpleDateFormat;
		}

		public String toPattern() {
			return this.simpleDateFormat.toPattern();
		}
	}

	enum Density {
		EXTREME(4), HIGH(6), MEDIUM(10), LOW(16);

		private final Settings			settings					= Settings.getInstance();
		public final int						boxWidthAmplitude	= 2;											// box may grow or shrink by this value based on log duration

		private final int						boxWidth;																		// in pixel
		/**
		 * use this to avoid repeatedly cloning actions instead of  values()
		 */
		public final static Density	values[]					= values();					

		private Density(int boxWidth) {
			this.boxWidth = boxWidth;
		}

		public static Density fromOrdinal(int ordinal) {
			return Density.values[ordinal];
		}

		public static String toString(Density density) {
			return density.name();
		}

		public int getScaledBoxWidth() {
			return (int) (this.boxWidth * (1 + (this.settings.getBoxplotScaleOrdinal() - 1) / 3.));
		}
	}

	private RecordSet							trailRecordSet;													// this class does not utilize any specific trail recordset methods
	private int										width;																	// number of pixels for the timescale length (includes left/right margins and the histo chart region)
	private long									leftmostTimeStamp, rightmostTimeStamp;	// define the corners of the histo chart region
	private TreeMap<Long, Double>	relativeTimeScale;											// maps histoset timestamps to x-axis with range 0 to 1
	private Density								density;																// degree of population on the x -axis

	/**
	 * takes the timeline width and calculates the x-axis pixel positions for the histo timestamp values. 
	 * @param trailRecordSet any recordset object (no trail recordset required)
	 * @param width  number of pixels for the timescale length including left/right margins for boxplots 
	 * @param leftmostTimeStamp  in modes zoom, pan, scope
	 * @param rightmostTimeStamp  in modes zoom, pan, scope
	 */
	public void initialize(RecordSet trailRecordSet, int width, long leftmostTimeStamp, long rightmostTimeStamp) {
		this.trailRecordSet = trailRecordSet;
		this.width = width;
		this.leftmostTimeStamp = leftmostTimeStamp;
		this.rightmostTimeStamp = rightmostTimeStamp;
		this.relativeTimeScale = getRelativeScale();
		defineDensity();
	}

	/**
	 * draws the histo time line for the standard x-axis and the logarithmic distance axis.
	 * respects left/right margin, i.e. uses the histo chart region only.
	 * based on histo recordSets (not on trailRecordSet!).
	 * @param recordSet any recordset object (no trail recordset required)
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width the end point in points
	 */
	public void drawTimeLine(GC gc, int x0, int y0) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(DataExplorer.COLOR_BLACK);
		gc.drawLine(x0 - 1, y0, x0 + this.width + 1, y0);
		if (HistoTimeLine.log.isLoggable(Level.FINE)) HistoTimeLine.log.log(Level.FINE, String.format("time line - x0=%d y0=%d - width=%d", x0, y0, this.width)); //$NON-NLS-1$

		long totalDisplayTime_ms = (long) (this.trailRecordSet.getStartTimeStamp() - (long) this.trailRecordSet.getMaxTime_ms());
		TimeLine timeFormat = this.getScaleFormat(totalDisplayTime_ms);

		String timeLineDescription;
		Point pt; // to calculate the space required to draw the time values
		if (timeFormat == TimeLine.MONTH_TO_DAY) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0745);
			pt = gc.textExtent(StringHelper.getFormatedTime(TimeFormat.MonthDay.toPattern(), this.histoSet.firstKey())); // $NON-NLS-1$
		}
		else if (timeFormat == TimeLine.MONTH_TO_HOUR) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0744);
			pt = gc.textExtent(StringHelper.getFormatedTime(TimeFormat.MonthHour.toPattern(), this.histoSet.firstKey())); // $NON-NLS-1$
		}
		else if (timeFormat == TimeLine.YEAR_TO_MONTH) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0743);
			pt = gc.textExtent(StringHelper.getFormatedTime(TimeFormat.YearMonth.toPattern(), this.histoSet.firstKey())); // $NON-NLS-1$
		}
		else if (timeFormat == TimeLine.DATE) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0742);
			pt = gc.textExtent(StringHelper.getFormatedTime(TimeFormat.Date.toPattern(), this.histoSet.firstKey())); // $NON-NLS-1$
		}
		else {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0741);
			pt = gc.textExtent(StringHelper.getFormatedTime(TimeFormat.DateTime.toPattern(), this.histoSet.firstKey())); // $NON-NLS-1$
		}
		GraphicsUtils.drawTimeLineText(timeLineDescription, (x0 + this.width / 2), y0 + pt.y * 5 / 2 + 2, gc, SWT.HORIZONTAL);
		drawTickMarks(gc, x0, y0, pt, timeFormat);
	}

	/**
	 * calculates the maximum time to be displayed and defines the corresponding label format
	 * @param totalDisplayTime_ms timespan
	 * @return format (e.g. TimeLine.TIME_LINE_DATE_TIME)
	 */
	public TimeLine getScaleFormat(long totalDisplayTime_ms) {
		TimeLine format = TimeLine.DATE; // the time format type
		if (HistoTimeLine.log.isLoggable(Level.FINER)) HistoTimeLine.log.log(Level.FINER, "totalDisplayTime_ms = " + totalDisplayTime_ms); //$NON-NLS-1$

		long totalTime_sec = totalDisplayTime_ms / 1000;
		long totalTime_days = TimeUnit.DAYS.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_month = TimeUnit.DAYS.convert(totalTime_sec, TimeUnit.SECONDS) / 30;
		long totalTime_year = TimeUnit.DAYS.convert(totalTime_sec, TimeUnit.SECONDS) / 365;
		if (HistoTimeLine.log.isLoggable(Level.FINER))
			HistoTimeLine.log.log(Level.FINER, "totalTime_year = " + totalTime_year + "; totalTime_month = " + totalTime_month + "; totalTime_days = " + totalTime_days); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(this.leftmostTimeStamp); // start year
		if (totalTime_month < 12 && Calendar.getInstance().get(Calendar.YEAR) == cal.get(Calendar.YEAR)) { // starts in the current year
			if (this.density == Density.EXTREME)
				format = TimeLine.MONTH_TO_DAY;
			else if (this.density == Density.HIGH)
				format = TimeLine.MONTH_TO_HOUR;
			else if (this.density == Density.MEDIUM)
				format = TimeLine.DATE;
			else
				format = TimeLine.DATE_TIME;
		}
		else {
			if (this.density == Density.EXTREME)
				format = TimeLine.YEAR_TO_MONTH;
			else if (this.density == Density.HIGH)
				format = TimeLine.YEAR_TO_MONTH;
			else if (this.density == Density.MEDIUM)
				format = TimeLine.DATE;
			else
				format = TimeLine.DATE_TIME;
		}
		if (HistoTimeLine.log.isLoggable(Level.FINER)) HistoTimeLine.log.log(Level.FINER, "timeLineText = " + Messages.getString(MessageIds.GDE_MSGT0267)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return format;
	}

	/**
	 * takes the timeline width and calculates the pixel positions of the timestamp values. 
	 * left and right margins are left free for boxplots.
	 * in case of few timestamps the left and right margins will be increased. 
	 * @return list holding the timestamps and pixel positions (leftmost: 0, rightmost: width)
	 */
	public TreeMap<Long, Integer> getScalePositions() {
		TreeMap<Long, Integer> position = new TreeMap<>(Collections.reverseOrder());
		int leftMargin;
		int rightMargin;
		if (settings.isXAxisLogarithmicDistance() || this.density == Density.LOW || this.relativeTimeScale.size() <= 2) {
			// bigger margins present the data in a more harmonic manner
			int remainingWidthPerItem = (this.width - this.density.boxWidth) / this.relativeTimeScale.size();
			if (remainingWidthPerItem > 2 * Density.LOW.boxWidth) {
				// in case of few items: placing the items according to the average distance is more harmonic than using the double box size
				leftMargin = rightMargin = this.width / (this.relativeTimeScale.size() + 1);
			}
			else
				leftMargin = rightMargin = (this.density.getScaledBoxWidth()) / 2; // minimum distance equal to boxWidth assumed
			// else {
			// // the first and last timescale items are placed at the margins without trying to set them proportionally according their timestamps
			// leftMargin = rightMargin = Math.max((this.boxWidths[this.density] + this.boxWidthAmplitude), this.width
			// - 2 * this.boxWidths[TimeLine.DENSITY_LOW] * (this.relativeTimeScale.size() + 1)) / 2; // minimum distance equal to 2*boxWidth assumed
			// }
		}
		else {
			leftMargin = rightMargin = (this.density.getScaledBoxWidth()) / 2; // minimum distance equal to boxWidth assumed
		}
		int netWidth = this.width - leftMargin - rightMargin;
		HistoTimeLine.log.log(Level.FINER, String.format("width = %4d|leftMargin = %4d|rightMargin = %4d|netWidth = %4d", width, leftMargin, rightMargin, netWidth)); //$NON-NLS-1$

		for (Entry<Long, Double> entry : this.relativeTimeScale.entrySet()) {
			position.put(entry.getKey(), leftMargin + (int) (entry.getValue() * netWidth));
			if (HistoTimeLine.log.isLoggable(Level.FINEST)) HistoTimeLine.log.log(Level.FINEST, "timeStamp = " + entry.getKey() + " position = " + position.get(entry.getKey())); //$NON-NLS-1$
		}
		return position;
	}

	/**
	 * calculates the relative position on the timescale based on isReversed and isTimeframeAdjustment.
	 * @return relative positions (leftmost: 0, rightmost: 1)
	 */
	private TreeMap<Long, Double> getRelativeScale() {
		long maxVerifiedTimeStamp = -1; // first timeStamp in the HistoSet after maximumTimeStamp which is the first one due to descending order

		// pass 1: build distances list and distances sum for all timestamps in the timestamp sliding window
		HistoTimeLine.log.log(Level.FINER, String.format("leftmostTimeStamp = %,d  rightmostTimeStamp = %,d", this.leftmostTimeStamp, this.rightmostTimeStamp)); //$NON-NLS-1$
		LinkedHashMap<Long, Long> applicableDistances = new LinkedHashMap<>();
		long lastTimeStamp = 0;
		long applicableDistancesSum = 0;
		for (Entry<Long, List<HistoVault>> entry : this.histoSet.subMap(this.leftmostTimeStamp, true, this.rightmostTimeStamp, true).entrySet()) {
			long currentTimeStamp = entry.getKey();
			if (HistoTimeLine.log.isLoggable(Level.FINER)) {
				ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimeStamp), ZoneId.systemDefault());
				HistoTimeLine.log.log(Level.FINER, "timestamp = " + currentTimeStamp + "  " + zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); //$NON-NLS-1$
			}
			if (lastTimeStamp != 0) {
				long currentDistance = lastTimeStamp - currentTimeStamp;
				applicableDistances.put(currentTimeStamp, currentDistance);
				applicableDistancesSum += currentDistance;
			}
			else {
				maxVerifiedTimeStamp = currentTimeStamp;
			}
			lastTimeStamp = currentTimeStamp;
		}
		HistoTimeLine.log.log(Level.FINER, "applicableDistancesSum = " + applicableDistancesSum + " number of distances = " + applicableDistances.size()); //$NON-NLS-1$

		//pass 2: build the relative timeScale list from relative distances
		TreeMap<Long, Double> relativeTimeScale = new TreeMap<>();
		if (settings.isXAxisReversed()) {
			relativeTimeScale = new TreeMap<>(Collections.reverseOrder());
			relativeTimeScale.put(maxVerifiedTimeStamp, 0.); // highest timestamp is at the leftmost position
		}
		else {
			relativeTimeScale = new TreeMap<>();
			relativeTimeScale.put(maxVerifiedTimeStamp, 1.); // highest timestamp is at the rightmost position
		}
		double applicableDistancesTotal = applicableDistancesSum;
		final boolean isNaturalTimescale = false; // true is not used anymore: use 'non-logarithmic' with a spread ordinal of zero which results in values very close to the natural timescale
		if (isNaturalTimescale == true) { // build final relative TimeScale List --- this a simple version illustrating the calculation baseline
			double relativeTimeScaleSum = 0.;
			for (Entry<Long, Long> entry : applicableDistances.entrySet()) {
				double relativeDistance = entry.getValue() / applicableDistancesTotal;
				// relativeDistances.add(relativeDistance);
				relativeTimeScaleSum += relativeDistance;
				if (settings.isXAxisReversed())
					relativeTimeScale.put(entry.getKey(), relativeTimeScaleSum);
				else
					relativeTimeScale.put(entry.getKey(), 1. - relativeTimeScaleSum);
				if (HistoTimeLine.log.isLoggable(Level.FINEST)) {
					HistoTimeLine.log.log(Level.FINEST, "relativeTimeScale = " + entry.getValue()); //$NON-NLS-1$
				}
			}
			HistoTimeLine.log.log(Level.FINER, "relativeTimeScaleSum = " + relativeTimeScaleSum); //$NON-NLS-1$
		}
		else {
			// pass 2.1: build normalized distances List
			LinkedHashMap<Long, Double> normalizedDistances = new LinkedHashMap<>();
			double normalizedDistancesMin = 0.;
			double normalizedDistancesSum = 0.;
			for (Entry<Long, Long> entry : applicableDistances.entrySet()) {
				double normalizedDistance;
				if (settings.isXAxisLogarithmicDistance())
					normalizedDistance = Math.log(entry.getValue() / applicableDistancesTotal);
				else
					normalizedDistance = entry.getValue() / applicableDistancesTotal;
				normalizedDistances.put(entry.getKey(), normalizedDistance);
				normalizedDistancesMin = Math.min(normalizedDistancesMin, normalizedDistance);
				normalizedDistancesSum += normalizedDistance;
				if (HistoTimeLine.log.isLoggable(Level.FINEST)) {
					HistoTimeLine.log.log(Level.FINEST, "normalizedDistance=" + normalizedDistance); //$NON-NLS-1$
				}
			}
			HistoTimeLine.log.log(Level.FINER, "normalizedDistancesSum=" + normalizedDistancesSum); //$NON-NLS-1$

			// pass 2.2: take logDistanceMin as a reference and scale to the final position range which is 0 to 1
			// high spread values result in a rather equidistant placement of timesteps (distant timestamps move close together which results in more space for timesteps with small distances)
			double logBaseDivisor = Math.log(1 + Math.pow(10, settings.getXAxisSpreadOrdinal() - 3)); // so we get reasonable adjustments for spreadValues from 0 to 5
			double scaleConstant = normalizedDistancesSum / logBaseDivisor + (1 - normalizedDistancesMin / logBaseDivisor) * normalizedDistances.size();
			double relativeTimeScaleSum = 0.;
			for (Entry<Long, Double> entry2 : normalizedDistances.entrySet()) {
				// =(1+(logDistance-logMin)/logBase)/(logSum/logBase+(1-logMin/logBase)*listSize())
				double relativeDistance = (1. + (entry2.getValue() - normalizedDistancesMin) / logBaseDivisor) / scaleConstant;
				// relativeDistances.add(relativeDistance);
				relativeTimeScaleSum += relativeDistance;
				if (settings.isXAxisReversed())
					relativeTimeScale.put(entry2.getKey(), relativeTimeScaleSum);
				else
					relativeTimeScale.put(entry2.getKey(), 1. - relativeTimeScaleSum);
				if (HistoTimeLine.log.isLoggable(Level.FINEST)) {
					HistoTimeLine.log.log(Level.FINEST, "relativeTimeScale=" + relativeTimeScale.get(entry2.getKey())); //$NON-NLS-1$
				}
			}
			HistoTimeLine.log.log(Level.FINER, "relativeTimeScaleSum=" + relativeTimeScaleSum); //$NON-NLS-1$
		}
		// // pass 3: determine how full the chart is
		// Collections.sort(relativeDistances);
		// // a high distanceQuantile value indicates that there is plenty of space between the x-axis points
		// double distanceQuantile = relativeDistances.get(relativeDistances.size() * (2 + 2 * preferredBoxSize) / 10); // use relative distance and calculate lower or higher quantile according to user's priorities
		// int absoluteDistance = (int) (this.width * distanceQuantile);
		// // use the box size as a standard of comparison
		// if (absoluteDistance >= this.boxWidths[TimeLine.DENSITY_LOW] + this.boxWidthAmplitude)
		// this.density = TimeLine.DENSITY_LOW;
		// else if (absoluteDistance >= this.boxWidths[TimeLine.DENSITY_MEDIUM] + this.boxWidthAmplitude)
		// this.density = TimeLine.DENSITY_MEDIUM;
		// else if (absoluteDistance >= this.boxWidths[TimeLine.DENSITY_HIGH] + this.boxWidthAmplitude)
		// this.density = TimeLine.DENSITY_HIGH;
		// else
		// this.density = TimeLine.DENSITY_EXTREME;
		// if (TimeLine.log.isLoggable(Level.SEVERE))
		// TimeLine.log.log(Level.SEVERE, String.format("pixel distance requested = %d boxType selected = %d Distance min = %f max = %f", absoluteDistance, this.density, relativeDistances.get(0), relativeDistances.get(relativeDistances.size()
		// - 1)));
		return relativeTimeScale;
	}

	/**
	 * calculates a density indicator based on the current width and the boxplot element scaled sizes without width amplitude settings. 
	 * Does not take elements which have a distance less than one pixel and will be thus positioned at the same place. 
	 * @return density constant (e.g. DENSITY_EXTREME)
	 */
	private void defineDensity() {
		List<Double> relativeDistances = new ArrayList<>();
		Entry<Long, Double> lastEntry = null;
		for (Entry<Long, Double> entry : this.relativeTimeScale.entrySet()) {
			if (lastEntry != null) {
				double relativeDistance = Math.abs(lastEntry.getValue() - entry.getValue());
				double absoluteDistance = relativeDistance * this.width;
				if (absoluteDistance > 0.5)
					relativeDistances.add(Math.abs(lastEntry.getValue() - entry.getValue()));
				else if (HistoTimeLine.log.isLoggable(Level.FINER))
					HistoTimeLine.log.log(Level.FINER, String.format("subpixel distance discarded at %d    absoluteDistance = %f", entry.getKey(), absoluteDistance));

			}
			lastEntry = entry;
		}
		if (relativeDistances.size() < 3)
			this.density = Density.LOW;
		else {
			Collections.sort(relativeDistances);
			// a high distanceQuantile value indicates that there is plenty of space between the x-axis points
			double distanceQuantile = relativeDistances.get(relativeDistances.size() * (2 + 2 * (settings.getBoxplotScaleOrdinal() + 1)) / 10); // use relative distance and calculate lower or higher quantile according to user's priorities
			int absoluteDistance = (int) (this.width * distanceQuantile);
			// use the box size as a standard of comparison
			if (absoluteDistance >= Density.LOW.boxWidth)
				this.density = Density.LOW;
			else if (absoluteDistance >= Density.MEDIUM.boxWidth)
				this.density = Density.MEDIUM;
			else if (absoluteDistance >= Density.HIGH.boxWidth)
				this.density = Density.HIGH;
			else
				this.density = Density.EXTREME;
			if (HistoTimeLine.log.isLoggable(Level.FINE)) HistoTimeLine.log.log(Level.FINE, String.format("absoluteDistance=%d  boxType=%s   relativeDistancesMin=%f relativeDistancesMax=%f",
					absoluteDistance, density.toString(), relativeDistances.get(0), relativeDistances.get(relativeDistances.size() - 1)));
		}
		if (HistoTimeLine.log.isLoggable(Level.FINER)) HistoTimeLine.log.log(Level.FINER, String.format("density = %s  ", this.density.toString()));
	}

	private void drawTickMarks(GC gc, int x0, int y0, Point pt, TimeLine timeFormat) {
		int tickLength = pt.y / 2 + 1;
		int gap = pt.y / 3;
		int labelGap = 4;
		int lastXPosLabel = -Integer.MAX_VALUE;
		String timePattern;
		if (timeFormat == TimeLine.MONTH_TO_DAY)
			timePattern = TimeFormat.MonthDay.toPattern();
		else if (timeFormat == TimeLine.MONTH_TO_HOUR)
			timePattern = TimeFormat.MonthHour.toPattern();
		else if (timeFormat == TimeLine.YEAR_TO_MONTH)
			timePattern = TimeFormat.YearMonth.toPattern();
		else if (timeFormat == TimeLine.DATE)
			timePattern = TimeFormat.Date.toPattern();
		else
			timePattern = TimeFormat.DateTime.toPattern();
		String lastText = GDE.STRING_BLANK;
		for (Entry<Long, Integer> entry : this.getScalePositions().entrySet()) { // TreeMap is reversed -> important for next if
			int xPos = x0 + entry.getValue();
			String text = StringHelper.getFormatedTime(timePattern, entry.getKey());
			if ((settings.isXAxisReversed() && lastXPosLabel + pt.x + labelGap < xPos && !lastText.equals(text))
					|| (!settings.isXAxisReversed() && lastXPosLabel - pt.x - labelGap > xPos && !lastText.equals(text))) {
				gc.drawLine(xPos, y0, xPos, y0 + tickLength);
				GraphicsUtils.drawTextCentered(text, xPos, y0 + tickLength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				lastText = text;
				lastXPosLabel = xPos;
			}
			else {
				gc.drawLine(xPos, y0, xPos, y0 + tickLength / 2);
			}
		}
	}

	public Density getDensity() {
		return this.density;
	}

}
