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
    
    Copyright (c) 2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.device;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import gde.config.Settings;
import gde.log.Level;
import gde.ui.DataExplorer;

/**
 * randomized measurements samples based on a sampling timespan which also supports a 100% sample.
 * remarks: a) the sampling algorithm guarantees all min/max points to be elements of the samples. 
 * this may result in a small number of additional samples in areas with new min/max values.
 * b) the min/max points are identified on the basis of full sets of points.
 * so it is not sure that all individual min/max points to appear in the samples.
 * c) the algorithm may be taught with a selection of sample points to avoid areas with new min/max values which results in oversampling. 
 * a sufficient number of representative points from the same population will build an internal min/max estimation.
 * d) the samples are selected randomly. 
 * so multiple runs will neither select the same samples nor the same number of samples.
 * pls note that the resulting accuracy is better than the display accuracy in the history table and history graphics.
 * @author Thomas Eickert
 */
public class HistoRandomSample {
	private final static String	$CLASS_NAME				= HistoRandomSample.class.getName();
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	private final Settings			settings					= Settings.getInstance();

	private static final int		samplesInBaseTime	= 5;																// the reference and thresholdTimeMsec time should have at last this number of samples 

	private final int						recordTimespan_ms;																		// smallest time interval to the next sample: one record every 10 ms
	private final int						samplingTimespan_ms;																	// actual used timespan; one sample in this time span + potential oversampling samples
	private final Random				rand							= new Random();

	private int[]								minPoints;
	private int[]								maxPoints;

	private TimeStepValues			timeStep;
	private TimeStepValues			lastTimeStep;
	private int									readingCount			= 0;
	private int									samplingCount			= 0;
	private int									oversamplingCount	= 0;
	private int									pointsLength;

	private class TimeStepValues {
		int			nextSamplingTimeStamp_ms;
		int			nextStartTimeStamp_ms;
		boolean	isSamplingDone;
		boolean	isSampleTimePassed;
		int			timeSpanSamplingCount;
		int[]		points;
		long		timeStep_ms	= -1;
		boolean	isMinMax;
		boolean	isTime4Sample;
	}

	private enum Action {
		/**
		 *  predecessor points are not used as a sample
		 */
		DISCARD,
		/**
		 * predecessor points are a mandatory sample
		 */
		FORCE_RELEASE,
		/**
		 * points are a candidate for release but may be discarded if better points come up; current points must be minmax points
		 */
		DEFER_RELEASE,
		/**
		 * points are a candidate for release but may be discarded to compensate an oversampling action; predecessor points must NOT be minmax points
		 */
		COMPENSATE_RELEASE
	}

	/**
	 * sets the sampling timespan based on transition requirements.
	 * @param channelNumber is the log channel number which may differ in case of channel mix
	 * @param maxPoints log measurement points with a length equal to the number of log measurement points
	 * @param minPointslog measurement points with a length equal to the number of log measurement points
	 * @param recordTimespan_ms log measurement rate
	 */
	private HistoRandomSample(int channelNumber, int[] maxPoints, int[] minPoints, int recordTimespan_ms) {
		this.pointsLength = maxPoints.length;
		this.maxPoints = maxPoints;
		this.minPoints = minPoints;
		this.recordTimespan_ms = recordTimespan_ms;

		this.lastTimeStep = new TimeStepValues();

		// find the maximum sampling timespan
		int proposedTimespan_ms = this.settings.getSamplingTimespan_ms();
		final ChannelPropertyType channelProperty = DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getChannelProperty(ChannelPropertyTypes.MINIMUM_TRANSITION_STEPS);
		Map<Integer, TransitionType> transitionTypes = DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getChannelType(channelNumber).getTransitions();
		if (channelProperty != null && !channelProperty.getValue().isEmpty()) {
			proposedTimespan_ms = this.recordTimespan_ms;
		}
		else if (!transitionTypes.isEmpty()) {
			for (TransitionType transitionType : transitionTypes.values()) {
				if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
					proposedTimespan_ms = this.recordTimespan_ms;
					if (log.isLoggable(Level.FINE))
						log.log(Level.FINE, String.format("peak transitions inhibit sampling  samplingTimespan_ms effective=%d user=%d", this.recordTimespan_ms, this.settings.getSamplingTimespan_ms())); //$NON-NLS-1$
					break; // peaks are always based on short term measurements and this requires all measurement points
				}
				else {
					// do not care about the recovery time because it might be 0 in case of slopes
					int detectableTimespan_ms = Math.min(transitionType.referenceTimeMsec, transitionType.thresholdTimeMsec) / HistoRandomSample.samplesInBaseTime;
					proposedTimespan_ms = Math.min(proposedTimespan_ms, detectableTimespan_ms);
				}
			}
		}
		this.samplingTimespan_ms = Math.max(proposedTimespan_ms, this.recordTimespan_ms); // sampling timespan must not be smaller than the recording timespan
		if (proposedTimespan_ms != this.settings.getSamplingTimespan_ms()) {
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
					String.format("HistoRandomSample  pointsLength=%d  samplingTimespan_ms effective=%d user=%d", this.pointsLength, this.samplingTimespan_ms, this.settings.getSamplingTimespan_ms())); //$NON-NLS-1$
		}
	}

	/**
	 * @param channelNumber is the log channel number which may differ in case of channel mix
	 * @param newPointsLength number of log measurement points
	 * @param newRecordTimespan_ms log measurement rate
	 */
	public static HistoRandomSample createHistoRandomSample(int channelNumber, int newPointsLength, int newRecordTimespan_ms) { // reading a binFile with 500K records into a recordset takes 45 to 15 s; reading the file with 5k samples takes 0,60 to 0,35 s which is 1% to 2,5%
		int[] tmpMaxPoints = new int[newPointsLength];
		int[] tmpMinPoints = new int[newPointsLength];
		Arrays.fill(tmpMaxPoints, Integer.MIN_VALUE);
		Arrays.fill(tmpMinPoints, Integer.MAX_VALUE);
		return new HistoRandomSample(channelNumber, tmpMaxPoints, tmpMinPoints, newRecordTimespan_ms);
	}

	/**
	 * @param channelNumber is the log channel number which may differ in case of channel mix
	 * @param maxPoints log measurement points with a length equal to the number of log measurement points
	 * @param minPointslog measurement points with a length equal to the number of log measurement points
	 * @param newRecordTimespan_ms log measurement rate
	 * @return
	 */
	public static HistoRandomSample createHistoRandomSample(int channelNumber, int[] maxPoints, int[] minPoints, int newRecordTimespan_ms) {
		return new HistoRandomSample(channelNumber, maxPoints, minPoints, newRecordTimespan_ms);
	}

	/**
	 * takes a sample at the sampling time or in the case of a new set of minmax measurement points.
	 * tries to take not more than one sample per sampling timespan.
	 * thus a premature sample is only taken if a minmax state has been detected which did not persist in the next measurement points (oversampling case).
	 * in all cases the sample is made ready for use one cycle later which results in loosing the last sample of the population.
	 * @param points
	 * @param timeStep_ms
	 * @return true if a valid sample is available which must be fetched by calling the properties getSamplePoints and getSampleTimeStep_ms
	 */
	public boolean isValidSample(int[] points, long timeStep_ms) { //
		boolean isValidSample = false;
		if (this.samplingTimespan_ms <= this.recordTimespan_ms) { // every timeStep must be a "sample"
			this.lastTimeStep.points = points;
			this.lastTimeStep.timeStep_ms = timeStep_ms;
			this.samplingCount = ++this.readingCount;
			isValidSample = true;
		}
		else { // take real samples
			this.timeStep = new TimeStepValues();
			Action predecessorAction;
			if (timeStep_ms >= this.lastTimeStep.nextStartTimeStamp_ms) { // lastTimeStep was the last in the current sampling timespan
				this.timeStep.nextSamplingTimeStamp_ms = this.lastTimeStep.nextStartTimeStamp_ms + this.rand.nextInt(this.samplingTimespan_ms);
				this.timeStep.nextStartTimeStamp_ms = this.lastTimeStep.nextStartTimeStamp_ms + this.samplingTimespan_ms;
				this.timeStep.isSamplingDone = false;
				this.timeStep.isSampleTimePassed = false;
				this.timeStep.timeSpanSamplingCount = 0;
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, "******************** " + String.format("timeStep_ms=%d nextSamplingTimeStamp_ms=%d", timeStep_ms, this.timeStep.nextSamplingTimeStamp_ms));
			}
			else {
				this.timeStep.nextSamplingTimeStamp_ms = this.lastTimeStep.nextSamplingTimeStamp_ms;
				this.timeStep.nextStartTimeStamp_ms = this.lastTimeStep.nextStartTimeStamp_ms;
				this.timeStep.isSamplingDone = this.lastTimeStep.isSamplingDone;
				this.timeStep.isSampleTimePassed = this.lastTimeStep.isSampleTimePassed || this.lastTimeStep.isTime4Sample;
				this.timeStep.timeSpanSamplingCount = this.lastTimeStep.timeSpanSamplingCount;
			}
			this.timeStep.points = points;
			this.timeStep.timeStep_ms = timeStep_ms;
			this.timeStep.isMinMax = calculateMinMax(points);
			this.timeStep.isTime4Sample = !this.timeStep.isSampleTimePassed && timeStep_ms >= this.timeStep.nextSamplingTimeStamp_ms;
			;
			if (log.isLoggable(Level.FINEST))
				// if (timeStep_ms > 1107500 && timeStep_ms < 1115000)
				log.log(Level.FINEST, String.format("timeStep_ms=%,d isSampleTimePassed=%b isSamplingDone=%b isTime4Sample=%b", timeStep_ms, this.timeStep.isSampleTimePassed, this.timeStep.isSamplingDone,
						this.timeStep.isTime4Sample));
			{
				if (this.lastTimeStep.isMinMax && this.lastTimeStep.isTime4Sample) {
					if (this.timeStep.isMinMax)
						if (this.timeStep.isTime4Sample)
							predecessorAction = Action.FORCE_RELEASE; // {}
						else
							predecessorAction = Action.DEFER_RELEASE; // <> take the next points as they are new minmax values
					else
						predecessorAction = Action.FORCE_RELEASE; // {}
				}
				else if (this.lastTimeStep.isMinMax && !this.lastTimeStep.isTime4Sample) {
					if (this.timeStep.isMinMax)
						if (this.timeStep.isTime4Sample)
							predecessorAction = Action.DISCARD;
						else
							predecessorAction = Action.DEFER_RELEASE; // <> take the next points as they are new minmax values
					else
						predecessorAction = Action.FORCE_RELEASE; // OVERSAMPLING;
				}
				else if (!this.lastTimeStep.isMinMax && this.lastTimeStep.isTime4Sample) {
					if (this.timeStep.isMinMax)
						if (this.timeStep.isTime4Sample)
							predecessorAction = Action.COMPENSATE_RELEASE; // ()
						else
							predecessorAction = Action.DEFER_RELEASE; // <>
					else
						predecessorAction = Action.COMPENSATE_RELEASE; // ()
				}
				else if (!this.lastTimeStep.isMinMax && !this.lastTimeStep.isTime4Sample) {
					predecessorAction = Action.DISCARD;
				}
				else {
					predecessorAction = null; // is never reached
				}
				if (log.isLoggable(Level.FINEST))
					// if (timeStep_ms > 1107500 && timeStep_ms < 1115000)
					log.log(Level.FINEST, String.format("action=%s isLastMinMax=%b isLastTime4Sample=%b isMinMax=%b isTime4Sample=%b", predecessorAction.toString(), this.lastTimeStep.isMinMax,
							this.lastTimeStep.isTime4Sample, this.timeStep.isMinMax, this.timeStep.isTime4Sample));
			}
			switch (predecessorAction) {
			case DISCARD:
				break;
			case FORCE_RELEASE:
				this.timeStep.isSamplingDone = isValidSample = true;
				break;
			case DEFER_RELEASE:
				if (!this.lastTimeStep.isSamplingDone && this.lastTimeStep.nextStartTimeStamp_ms < this.timeStep.nextStartTimeStamp_ms) {
					// at least one sample in the histoSamplingTimespan
					this.timeStep.isSamplingDone = isValidSample = true;
				}
				break;
			case COMPENSATE_RELEASE:
				if (this.lastTimeStep.timeSpanSamplingCount <= 0) {
					this.timeStep.isSamplingDone = isValidSample = true;
				}
				else {
					this.timeStep.isSamplingDone = true; // compensates an oversampling event
				}
				break;
			default:
				// is never reached
				break;
			}
			++this.readingCount;
			if (isValidSample) {
				++this.samplingCount;
				if (this.lastTimeStep.nextStartTimeStamp_ms < this.timeStep.nextStartTimeStamp_ms)
					++this.lastTimeStep.timeSpanSamplingCount; // sample belongs to the last timespan
				else
					++this.timeStep.timeSpanSamplingCount;
			}
			if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, String.format("%5s  %s isValidSample=%b isSamplingDone=%b readingCount=%d samplingCount=%d", isValidSample, predecessorAction.toString(),
					isValidSample, this.timeStep.isSamplingDone, this.readingCount, this.samplingCount));
			if (this.lastTimeStep.nextStartTimeStamp_ms < this.timeStep.nextStartTimeStamp_ms) {
				this.oversamplingCount += this.lastTimeStep.timeSpanSamplingCount > 1 ? this.lastTimeStep.timeSpanSamplingCount - 1 : 0; // one sample per timespan is the standard case
				if (log.isLoggable(Level.FINEST) && this.oversamplingCount > 0)
					log.log(Level.FINEST, "******************** " + String.format("timeSpanSamplingCount =%,9d samplingCount=%,9d oversamplingCount=%,9d oversamplingCountMax=%,9d   ",
							this.timeStep.timeSpanSamplingCount, this.samplingCount, this.oversamplingCount, this.oversamplingCount));
			}
			if (log.isLoggable(Level.FINEST) && this.oversamplingCount > 0)
				// if (timeStep_ms > 1107500 && timeStep_ms < 1115000)
				log.log(Level.FINEST, String.format("%,9d  ", timeStep_ms) + String.format(String.format("%0" + this.oversamplingCount + "d", 0)));
			// prepare the sample for fetching by the caller
			this.lastTimeStep = this.timeStep;
		}
		return isValidSample;
	}

	private boolean calculateMinMax(int[] points) {
		boolean isChanged = false;
		for (int i = 0; i < points.length; i++) {
			int point = points[i];
			if (point < this.minPoints[i]) {
				this.minPoints[i] = point;
				isChanged = true;
			}
			if (point > this.maxPoints[i]) {
				this.maxPoints[i] = point;
				isChanged = true;
			}
		}
		return isChanged;
	}

	public int[] getSamplePoints() {
		return this.lastTimeStep.points;
	}

	public long getSampleTimeStep_ms() {
		return this.lastTimeStep.timeStep_ms;
	}

	public int getReadingCount() {
		return this.readingCount;
	}

	public int getSamplingCount() {
		return this.samplingCount;
	}

	public int getOverSamplingCount() {
		return this.oversamplingCount;
	}

	public int[] getMinPoints() {
		return this.minPoints;
	}

	/**
	 * @param minPoints utilizes the minMax values from a previous run and thus reduces oversampling
	 */
	public void setMinPoints(int[] minPoints) {
		if (minPoints.length == 0) {
			this.minPoints = new int[this.pointsLength];
			Arrays.fill(this.minPoints, Integer.MAX_VALUE);
		}
		else {
			this.minPoints = minPoints;
		}
	}

	public int[] getMaxPoints() {
		return this.maxPoints;
	}

	/**
	 * @param maxPoints utilizes the minMax values from a previous run and thus reduces oversampling
	 */
	public void setMaxPoints(int[] maxPoints) {
		if (maxPoints.length == 0) {
			this.maxPoints = new int[this.pointsLength];
			Arrays.fill(this.maxPoints, Integer.MIN_VALUE);
		}
		else {
			this.maxPoints = maxPoints;
		}
	}

}
