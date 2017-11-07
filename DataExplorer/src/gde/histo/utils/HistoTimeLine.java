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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
    					2016,2017 Thomas Eickert
****************************************************************************************/
package gde.histo.utils;

import static gde.histo.utils.HistoTimeLine.Density.EXTREME;
import static gde.histo.utils.HistoTimeLine.Density.HIGH;
import static gde.histo.utils.HistoTimeLine.Density.LOW;
import static gde.histo.utils.HistoTimeLine.Density.MEDIUM;
import static java.util.logging.Level.FINER;

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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import gde.GDE;
import gde.config.Settings;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.utils.GraphicsUtils;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;

/**
 * Draw time line with tick marks and numbers.
 * Support logarithmic distances and elaborate x-axis point placement based on box width and number of points.
 * @author Thomas Eickert
 */
public final class HistoTimeLine {
	private final static String						$CLASS_NAME			= HistoTimeLine.class.getName();
	private final static Logger						log							= Logger.getLogger($CLASS_NAME);

	private final Settings								settings				= Settings.getInstance();

	private final TreeMap<Long, Integer>	scalePositions	= new TreeMap<>(Collections.reverseOrder());

	enum Density {
		EXTREME(4), HIGH(8), MEDIUM(10), LOW(16);

		private final Settings			settings					= Settings.getInstance();

		/** box may grow or shrink by this value based on log duration */
		public final int						boxWidthAmplitude	= 2;

		private final int						boxWidth;																		// in pixel

		/** use this to avoid repeatedly cloning actions instead of values() */
		public static final Density	VALUES[]					= values();

		private Density(int boxWidth) {
			this.boxWidth = boxWidth;
		}

		public static Density fromOrdinal(int ordinal) {
			return Density.VALUES[ordinal];
		}

		public static String toString(Density density) {
			return density.name();
		}

		public int getScaledBoxWidth() {
			return (int) (this.boxWidth * (1 + (this.settings.getBoxplotScaleOrdinal() - 1) / 3.));
		}
	}

	private TrailRecordSet								trailRecordSet;
	/**
	 * degree of population on the x -axis
	 */
	private Density												density;
	/**
	 * number of pixels for the timescale length (includes left/right margins and the chart region)
	 */
	private int														width;
	/**
	 * define the corners of the chart region
	 */
	private long													leftmostTimeStamp, rightmostTimeStamp;
	/**
	 * maps histoset timestamps to x-axis with range 0 to 1
	 */
	private TreeMap<Long, Double>					relativeTimeScale;
	/**
	 * access timestamps by x axis position (naturalOrder)
	 */
	private TreeMap<Integer, List<Long>>	scaleTimeStamps_ms	= new TreeMap<Integer, List<Long>>();

	/**
	 * Take the timeline width and calculates the x-axis pixel positions for the timestamp values.
	 * @param newTrailRecordSet any recordset object (no trail recordset required)
	 * @param newWidth number of pixels for the timescale length including left/right margins for boxplots
	 */
	public synchronized void initialize(TrailRecordSet newTrailRecordSet, int newWidth) {
		this.trailRecordSet = newTrailRecordSet;
		this.width = newWidth;
		setRelativeScale();
		defineDensity();
		setScalePositions();
	}

	@Override
	public String toString() {
		int scaleTimeStampsSum_ms = this.scaleTimeStamps_ms.entrySet().parallelStream().mapToInt(c -> c.getValue().size()).sum();
		return String.format("width=%d  leftmostTimeStamp = %,d  rightmostTimeStamp = %,d  density=%s scalePositionsSize=%d scaleTimeStamps_ms=%d", this.width, this.leftmostTimeStamp, this.rightmostTimeStamp, this.density.toString(), this.scaleTimeStamps_ms.size(), scaleTimeStampsSum_ms);
	}

	/**
	 * Draw the time line for the standard x-axis and the logarithmic distance axis.
	 * Respect left/right margin, i.e. uses the chart region only.
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 */
	public void drawTimeLine(GC gc, int x0, int y0) {
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(DataExplorer.COLOR_BLACK);
		gc.drawLine(x0 - 1, y0, x0 + this.width + 1, y0);
		log.fine(() -> String.format("time line - x0=%d y0=%d - width=%d", x0, y0, this.width)); //$NON-NLS-1$

		// calculate the maximum time to be displayed and define the corresponding label format
		final DateTimePattern timeFormat = getScaleFormat(this.trailRecordSet.getLeftmostTimeStamp_ms() - this.trailRecordSet.getRightmostTimeStamp_ms());

		String timeLineDescription;
		Point pt; // to calculate the space required to draw the time values
		if (timeFormat == DateTimePattern.MMdd) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0745);
		} else if (timeFormat == DateTimePattern.MMdd_HH) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0744);
		} else if (timeFormat == DateTimePattern.yyyyMM) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0743);
		} else if (timeFormat == DateTimePattern.yyyyMMdd) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0742);
		} else if (timeFormat == DateTimePattern.yyyyMMdd_HHmm) {
			timeLineDescription = Messages.getString(MessageIds.GDE_MSGT0741);
		} else {
			throw new UnsupportedOperationException();
		}
		long sampleTimeStamp_ms = trailRecordSet.getTimeStepSize() == 0 ? 11 : trailRecordSet.getLeftmostTimeStamp_ms();
		pt = gc.textExtent(LocalizedDateTime.getFormatedTime(timeFormat, sampleTimeStamp_ms)); // $NON-NLS-1$
		GraphicsUtils.drawTimeLineText(timeLineDescription, (x0 + this.width / 2), y0 + pt.y * 5 / 2 + 2, gc, SWT.HORIZONTAL);
		drawTickMarks(gc, x0, y0, pt, timeFormat);
	}

	/**
	 * Calculate the maximum time to be displayed and defines the corresponding label format.
	 * @param totalDisplayTime_ms timespan
	 * @return format (e.g. TimeLine.TIME_LINE_DATE_TIME)
	 */
	private DateTimePattern getScaleFormat(long totalDisplayTime_ms) {
		final DateTimePattern timeFormat;
		log.finer(() -> "totalDisplayTime_ms = " + totalDisplayTime_ms); //$NON-NLS-1$

		long totalTime_month = TimeUnit.DAYS.convert(totalDisplayTime_ms, TimeUnit.MILLISECONDS) / 30;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(this.leftmostTimeStamp); // start year
		if (totalTime_month < 12 && Calendar.getInstance().get(Calendar.YEAR) == cal.get(Calendar.YEAR)) {
			// starts in the current year
			if (this.density == EXTREME)
				timeFormat = DateTimePattern.MMdd;
			else if (this.density == HIGH)
				timeFormat = DateTimePattern.MMdd_HH;
			else if (this.density == MEDIUM)
				timeFormat = DateTimePattern.yyyyMMdd;
			else
				timeFormat = DateTimePattern.yyyyMMdd_HHmm;
		} else {
			if (this.density == EXTREME)
				timeFormat = DateTimePattern.yyyyMM;
			else if (this.density == HIGH)
				timeFormat = DateTimePattern.yyyyMM;
			else if (this.density == MEDIUM)
				timeFormat = DateTimePattern.yyyyMMdd;
			else
				timeFormat = DateTimePattern.yyyyMMdd_HHmm;
		}
		log.finer(() -> "timeLineText = " + Messages.getString(MessageIds.GDE_MSGT0267)); //$NON-NLS-1$
		return timeFormat;
	}

	/**
	 * @return list holding the timestamps and pixel positions (leftmost: 0, rightmost: width)
	 */
	public TreeMap<Long, Integer> getScalePositions() {
		return this.scalePositions;
	}

	/**
	 * Take the timeline width and calculates the pixel positions of the timestamp values.
	 * Left and right margins are left free for boxplots.
	 * In case of few timestamps the left and right margins will be increased.
	 */
	public void setScalePositions() {
		this.scalePositions.clear();
		this.scaleTimeStamps_ms.clear();
		int leftMargin;
		int rightMargin;
		if (this.settings.isXAxisLogarithmicDistance() || this.density == Density.LOW || this.relativeTimeScale.size() <= 2) {
			// bigger margins present the data in a more harmonic manner
			int remainingWidthPerItem = (this.width - this.density.boxWidth) / this.relativeTimeScale.size();
			if (remainingWidthPerItem > 2 * Density.LOW.boxWidth)
				// in case of few items: placing the items acc to the average distance is more harmonic than using the double box size
				leftMargin = rightMargin = this.width / (this.relativeTimeScale.size() + 1);
			else
				leftMargin = rightMargin = (this.density.getScaledBoxWidth()) / 2; // minimum distance equal to boxWidth assumed
		} else {
			leftMargin = rightMargin = (this.density.getScaledBoxWidth()) / 2; // minimum distance equal to boxWidth assumed
		}
		int netWidth = this.width - leftMargin - rightMargin;
		log.finer(() -> String.format("width = %4d|leftMargin = %4d|rightMargin = %4d|netWidth = %4d", //
				this.width, leftMargin, rightMargin, netWidth));

		for (Entry<Long, Double> entry : this.relativeTimeScale.entrySet()) {
			this.scalePositions.put(entry.getKey(), leftMargin + (int) (entry.getValue() * netWidth));
			log.finest(() -> "timeStamp = " + entry.getKey() + " position = " + this.scalePositions.get( //$NON-NLS-1$ //$NON-NLS-2$
					entry.getKey()));
		}
	}

	/**
	 * Calculate the relative position on the timescale based on isReversed and isTimeframeAdjustment.
	 * (leftmost: 0, rightmost: 1)
	 */
	@SuppressWarnings({ "unused", "serial" })
	private void setRelativeScale() {
		long maxVerifiedTimeStamp = -1; // first timeStamp maximumTimeStamp which is the first one due to descending order

		LinkedHashMap<Long, Long> applicableDistances = new LinkedHashMap<>();
		long applicableDistancesSum = 0;
		{// pass 1: build distances list and distances sum for all timestamps in the timestamp sliding window
			log.log(FINER, "", this); //$NON-NLS-1$
			long lastTimeStamp = 0;
			for (int i = 0; i < this.trailRecordSet.getTimeStepSize(); i++) {
				long currentTimeStamp = ((long) this.trailRecordSet.getTime_ms(i));
				if (log.isLoggable(FINER)) {
					ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimeStamp), ZoneId.systemDefault());
					log.log(FINER, "timestamp = " + currentTimeStamp + "  " + zdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (lastTimeStamp != 0) {
					long currentDistance = lastTimeStamp - currentTimeStamp;
					applicableDistances.put(currentTimeStamp, currentDistance);
					applicableDistancesSum += currentDistance;
				} else {
					maxVerifiedTimeStamp = currentTimeStamp;
				}
				lastTimeStamp = currentTimeStamp;
			}
			if (log.isLoggable(FINER))
				log.log(FINER, "applicableDistancesSum = " + applicableDistancesSum + " number of distances = " + applicableDistances.size()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		{// pass 2: build the relative timeScale list from relative distances
			if (this.settings.isXAxisReversed()) {
				this.relativeTimeScale = new TreeMap<Long, Double>(Collections.reverseOrder());
				this.relativeTimeScale.put(maxVerifiedTimeStamp, 0.); // highest timestamp is at the leftmost position
			} else {
				this.relativeTimeScale = new TreeMap<Long, Double>() {
					/**
					 * value is based on descending order and must be transformed
					 * (e.g. a lefthand value becomes a righthand value)
					 */
					@Override
					public Double put(Long key, Double value) {
						return super.put(key, 1. - value);
					}
				};
				this.relativeTimeScale.put(maxVerifiedTimeStamp, 0.); // highest timestamp is at the rightmost position
			}
		}
		{
			@SuppressWarnings("unused")
			final boolean isNaturalTimescale = false;
			if (isNaturalTimescale == true) {
				// true is not used anymore:
				// use 'non-logarithmic' with a spread ordinal of zero which results in values very close to the natural timescale
				// build final relative TimeScale List --- this a simple version illustrating the calculation baseline
				double relativeTimeScaleSum = 0.;
				for (Entry<Long, Long> entry : applicableDistances.entrySet()) {
					double relativeDistance = entry.getValue() / applicableDistancesSum;
					// relativeDistances.add(relativeDistance);
					relativeTimeScaleSum += relativeDistance;
					this.relativeTimeScale.put(entry.getKey(), relativeTimeScaleSum);
					log.finest(() -> "relativeTimeScale = " + entry.getValue()); //$NON-NLS-1$
				}
				log.log(FINER, "relativeTimeScaleSum = ", relativeTimeScaleSum); //$NON-NLS-1$
			} else {
				// pass 2.1: build normalized distances List
				LinkedHashMap<Long, Double> normalizedDistances = new LinkedHashMap<>();
				double normalizedDistancesMin = 0.;
				double normalizedDistancesSum = 0.;
				for (Entry<Long, Long> entry : applicableDistances.entrySet()) {
					double normalizedDistance;
					if (this.settings.isXAxisLogarithmicDistance()) {
						normalizedDistance = entry.getValue() != 0 ? Math.log(entry.getValue() / applicableDistancesSum) : 0;
					} else {
						normalizedDistance = entry.getValue() / applicableDistancesSum;
					}
					normalizedDistances.put(entry.getKey(), normalizedDistance);
					normalizedDistancesMin = Math.min(normalizedDistancesMin, normalizedDistance);
					normalizedDistancesSum += normalizedDistance;
					log.finest(() -> "normalizedDistance=" + normalizedDistance); //$NON-NLS-1$
				}
				log.log(FINER, "normalizedDistancesSum=", normalizedDistancesSum); //$NON-NLS-1$

				// pass 2.2: take logDistanceMin as a reference and scale to the final position range which is 0 to 1
				// high spread values result in a rather equidistant placement of timesteps (distant timestamps move close together which
				// results in more space for timesteps with small distances)
				// so we get reasonable adjustments for spreadValues from 0 to 5
				double logBaseDivisor = Math.log(1 + Math.pow(10, this.settings.getXAxisSpreadOrdinal() - 3));
				double scaleConstant = normalizedDistancesSum / logBaseDivisor + (1 - normalizedDistancesMin / logBaseDivisor) * normalizedDistances.size();
				double relativeTimeScaleSum = 0.;
				for (Entry<Long, Double> entry2 : normalizedDistances.entrySet()) {
					double relativeDistance = (1. + (entry2.getValue() - normalizedDistancesMin) / logBaseDivisor) / scaleConstant;
					relativeTimeScaleSum += relativeDistance;
					this.relativeTimeScale.put(entry2.getKey(), relativeTimeScaleSum);
					log.finest(() -> "relativeTimeScale=" + this.relativeTimeScale.get(entry2.getKey())); //$NON-NLS-1$
				}
				log.log(FINER, "relativeTimeScaleSum=", relativeTimeScaleSum); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Calculate a density indicator based on the current width and the boxplot element scaled sizes
	 * without width amplitude settings.
	 * Does not take elements which have a distance less than one pixel and will be thus positioned at the same place.
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
				else
					log.finer(() -> String.format("subpixel distance discarded at %d    absoluteDistance = %f", //$NON-NLS-1$
							entry.getKey(), absoluteDistance));

			}
			lastEntry = entry;
		}
		if (relativeDistances.size() < 3)
			this.density = LOW;
		else {
			Collections.sort(relativeDistances);
			// a high distanceQuantile value indicates that there is plenty of space between the x-axis points
			// use relative distance and calculate lower or higher quantile according to user's priorities
			double distanceQuantile = relativeDistances.get(relativeDistances.size() * (2 + 2 * (this.settings.getBoxplotScaleOrdinal() + 1)) / 10);
			int absoluteDistance = (int) (this.width * distanceQuantile + .5);
			// use the box size as a standard of comparison
			if (absoluteDistance > LOW.boxWidth)
				this.density = LOW;
			else if (absoluteDistance > MEDIUM.boxWidth)
				this.density = MEDIUM;
			else if (absoluteDistance > HIGH.boxWidth)
				this.density = HIGH;
			else
				this.density = EXTREME;
			log.fine(() -> String.format("absoluteDistance=%d  boxType=%s   relativeDistancesMin=%f relativeDistancesMax=%f", //$NON-NLS-1$
					absoluteDistance, this.density.toString(), relativeDistances.get(0), relativeDistances.get(relativeDistances.size() - 1)));
		}
		log.log(FINER, "", this); //$NON-NLS-1$
	}

	private void drawTickMarks(GC gc, int x0, int y0, Point pt, DateTimePattern timeFormat) {
		int tickLength = pt.y / 2 + 1;
		int gap = pt.y / 3;
		int labelGap = 4 + pt.x;
		int lastXPosLabel = -Integer.MAX_VALUE;
		String lastText = GDE.STRING_BLANK;
		for (Entry<Long, Integer> entry : this.getScalePositions().entrySet()) {
			int xPos = x0 + entry.getValue();
			String text = LocalizedDateTime.getFormatedTime(timeFormat, entry.getKey());
			if (!lastText.equals(text) && (Math.abs(lastXPosLabel - xPos) > labelGap)) { // abs supports asc / desc scale
				gc.drawLine(xPos, y0 + 1, xPos, y0 + tickLength);
				GraphicsUtils.drawTextCentered(text, xPos, y0 + tickLength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				lastText = text;
				lastXPosLabel = xPos;
			} else {
				gc.drawLine(xPos, y0 + 1, xPos, y0 + tickLength / 2);
			}
		}
	}

	/**
	 * @param timestamp_ms
	 * @return the timestamp position in the drawing area (relative to x0 which is the left position of the drawing canvas)
	 */
	public int getXPosTimestamp(long timestamp_ms) {
		log.finest(() -> "scalePositionsSize=" + this.scalePositions.size() + "  timestamp_ms=  " + timestamp_ms); //$NON-NLS-1$
		return this.scalePositions.get(timestamp_ms);
	}

	/**
	 * @param xPos current position in the drawing area (relative to x0 which is the left position of the drawing canvas)
	 * @return the time stamp of the trail recordset which is the closest one to xPos --- or null if xPos is too far away
	 */
	public Long getSnappedTimestamp(int xPos) {
		final int xPosTolerance = 20;
		Long timeStamp_ms = null;

		if (this.scaleTimeStamps_ms.isEmpty()) setScaleTimeStamps_ms();

		final Entry<Integer, List<Long>> lowerEntry = this.scaleTimeStamps_ms.lowerEntry(xPos);
		if (lowerEntry != null && xPos == lowerEntry.getKey())
			timeStamp_ms = lowerEntry.getValue().get(0); // take the first timeStamp for simplicity reasons
		else {
			Entry<Integer, List<Long>> ceilingEntry = this.scaleTimeStamps_ms.ceilingEntry(xPos);
			if (ceilingEntry == null && lowerEntry == null)
				throw new UnsupportedOperationException("an empty scale is not allowed");
			else if (ceilingEntry == null)
				timeStamp_ms = lowerEntry.getValue().get(lowerEntry.getValue().size() - 1);
			else if (lowerEntry == null)
				timeStamp_ms = ceilingEntry.getValue().get(0);
			else if (xPos > lowerEntry.getKey() + xPosTolerance && xPos < ceilingEntry.getKey() - xPosTolerance)
				timeStamp_ms = null; // midst in the distance but too far away
			else
				timeStamp_ms = lowerEntry != null && xPos <= (ceilingEntry.getKey() + lowerEntry.getKey()) / 2 //
						? lowerEntry.getValue().get(lowerEntry.getValue().size() - 1) //
						: ceilingEntry.getValue().get(0);
		}
		return timeStamp_ms;
	}

	/**
	 * @param xPos current position in the drawing area (relative to x0 which is the left position of the drawing canvas)
	 * @return the time stamp of the trail recordset which is the closest one to xPos
	 */
	public long getAdjacentTimestamp(int xPos) {
		long timeStamp_ms;

		if (this.scaleTimeStamps_ms.isEmpty()) setScaleTimeStamps_ms();

		final Entry<Integer, List<Long>> lowerEntry = this.scaleTimeStamps_ms.lowerEntry(xPos);
		if (lowerEntry != null && xPos == lowerEntry.getKey())
			timeStamp_ms = lowerEntry.getValue().get(0); // take the first timeStamp for simplicity reasons
		else {
			final Entry<Integer, List<Long>> ceilingEntry = this.scaleTimeStamps_ms.ceilingEntry(xPos);
			if (ceilingEntry == null && lowerEntry == null)
				throw new UnsupportedOperationException(" an empty scale is not allowed");
			else if (ceilingEntry == null)
				timeStamp_ms = lowerEntry.getValue().get(lowerEntry.getValue().size() - 1);
			else
				timeStamp_ms = lowerEntry != null && xPos <= (ceilingEntry.getKey() + lowerEntry.getKey()) / 2//
						? lowerEntry.getValue().get(lowerEntry.getValue().size() - 1)//
						: ceilingEntry.getValue().get(0);
		}
		return timeStamp_ms;
	}

	/**
	 * Create the map for optimized access to timestamps by x axis position.
	 * Optimized for naturally ordered x positions and timestamps ordered based on the user's x axis order setting.
	 */
	private void setScaleTimeStamps_ms() {
		this.scaleTimeStamps_ms.clear();
		if (this.settings.isXAxisReversed()) {
			Entry<Integer, List<Long>> previous = null;
			for (Entry<Long, Integer> entry : this.getScalePositions().entrySet()) { // TreeMap is reversed
				log.finest(() -> String.format("xPos=%d  entryValue=%d", entry.getValue(), entry.getKey())); //$NON-NLS-1$
				if (previous == null || entry.getValue() != previous.getKey()) {
					this.scaleTimeStamps_ms.put(entry.getValue(), new ArrayList<Long>());
					previous = this.scaleTimeStamps_ms.lastEntry();
				}
				previous.getValue().add(entry.getKey()); // results in a reverse timeStamp order (decreasing timestamps)
			}
		} else {
			Entry<Integer, List<Long>> previous = null;
			for (Entry<Long, Integer> entry : this.getScalePositions().entrySet()) { // TreeMap is reversed
				log.finest(() -> String.format("xPos=%d  entryValue=%d", entry.getValue(), entry.getKey())); //$NON-NLS-1$
				if (previous == null || entry.getValue() != previous.getKey()) {
					this.scaleTimeStamps_ms.put(entry.getValue(), new ArrayList<Long>());
					previous = this.scaleTimeStamps_ms.firstEntry(); // due to decreasing x axis positions in the scalePositions map
				}
				previous.getValue().add(0, entry.getKey()); // results in a natural timeStamp order
			}
		}
	}

	public Density getDensity() {
		return this.density;
	}

}
