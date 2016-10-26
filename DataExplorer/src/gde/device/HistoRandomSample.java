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
import java.util.Random;
import java.util.logging.Logger;

import gde.log.Level;

/**
 * randomized measurements samples based on a sampling timespan which also supports a 100% sample.
 * remarks: a) the sampling algorithm guarantees all min/max points to be elements of the samples. 
 * this may result in a small number of additional samples in areas with new min/max values.
 * b) the min/max points are identified on the basis of full sets of points.
 * so it is not sure that all individual min/max points to appear in the samples.
 * c) the algorithm may be tought with a selection of sample points to avoid areas with new min/max values which results in oversampling. 
 * a sufficient number of representative points from the same population will build an internal min/max estimation.
 * d) the samples are selected randomly. 
 * so multiple runs will neither select the same samples nor the same number of samples.
 * pls note that the resulting accuracy is better than the display accuracy in the history table and history graphics.
 * @author Thomas Eickert
 */
public class HistoRandomSample {
	private final static String	$CLASS_NAME						= HistoRandomSample.class.getName();
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	private final int					recordTimespan_ms				= 10;											// TODO smallest time interval to the next sample: one record every 10 ms
	private final int					histoSamplingTimespan_ms	= 1000;										// TODO from settings
	private final int					timespan_ms;																	// actual used timespan; one sample in this time span + potential oversampling samples
	private final Random				rand								= new Random();

	private static int[]				minPoints;
	private static int[]				maxPoints;

	private TimeStepValues			timeStep;
	private TimeStepValues			lastTimeStep;
	private int							readingCount					= 0;
	private int							samplingCount					= 0;
	private int							oversamplingCount				= 0;

	private class TimeStepValues {
		int		nextSamplingTimeStamp_ms;
		int		nextStartTimeStamp_ms;
		boolean	isSamplingDone;
		boolean	isSampleTimePassed;
		int		timeSpanSamplingCount;
		int[]		points;
		long		timeStep_ms	= -1;
		boolean	isMinMax;
		boolean	isTime4Sample;
	}

	public HistoRandomSample(int pointsLength, boolean keepMinMax) { // reading a binFile with 500K records into a recordset takes 45 to 15 s; reading the file with 5k samples takes 0,60 to 0,35 s which is 1% to 2,5%
		if (!keepMinMax) {
			minPoints = new int[pointsLength];
			maxPoints = new int[pointsLength];
			Arrays.fill(minPoints, Integer.MAX_VALUE);
			Arrays.fill(maxPoints, Integer.MIN_VALUE);
		}
		this.lastTimeStep = new TimeStepValues();
		this.timespan_ms = this.histoSamplingTimespan_ms < this.recordTimespan_ms ? this.recordTimespan_ms : this.histoSamplingTimespan_ms; // biggest time interval to the next sample
		if (log.isLoggable(Level.FINER))
			log.log(Level.FINER, String.format("HistoRandomSample  keepMinMax=%b", keepMinMax)); //$NON-NLS-1$
	}

	private enum Action {
		DISCARD, // predecessor points are not used as a sample
		FORCE_RELEASE, // predecessor points are a mandatory sample
		DEFER_RELEASE, // points are a candidate for release but may be discarded if better points come up; current points must be minmax points
		COMPENSATE_RELEASE, // points are a candidate for release but may be discarded to compensate an oversampling action; predecessor points must NOT be minmax points
	}

	/**
	 * takes a sample at the sampling time or in the case of a new set of minmax measurement points.
	 * tries to take not more than one sample per sampling timespan.
	 * thus a premature sample is only taken if a minmax state has been detected which did not persist in the next measurement points (oversampling case).
	 * in all cases the sample is made ready for use one cycle later which results in loosing the last sample of the population.
	 * @param points
	 * @param timeStep_ms
	 * @return indicates a valid sample which must be fetched by calling the properties getSamplePoints and getSampleTimeStep_ms
	 */
	public boolean isValidSample(int[] points, long timeStep_ms) { //
		boolean isValidSample = false;
		if (this.timespan_ms <= this.recordTimespan_ms) { // each timeStep must be a "sample"
			this.lastTimeStep.points = points;
			this.lastTimeStep.timeStep_ms = timeStep_ms;
			this.samplingCount = ++this.readingCount;
			isValidSample = true;
		} else { // take real samples
			this.timeStep = new TimeStepValues();
			Action predecessorAction;
			if (timeStep_ms >= this.lastTimeStep.nextStartTimeStamp_ms) { // lastTimeStep was the last in the current sampling timespan
				this.timeStep.nextSamplingTimeStamp_ms = this.lastTimeStep.nextStartTimeStamp_ms + this.rand.nextInt(this.timespan_ms);
				this.timeStep.nextStartTimeStamp_ms = this.lastTimeStep.nextStartTimeStamp_ms + this.timespan_ms;
				this.timeStep.isSamplingDone = false;
				this.timeStep.isSampleTimePassed = false;
				this.timeStep.timeSpanSamplingCount = 0;
				if (log.isLoggable(Level.FINEST))
					log.log(Level.SEVERE, "******************** " + String.format("timeStep_ms=%d nextSamplingTimeStamp_ms=%d", timeStep_ms, this.timeStep.nextSamplingTimeStamp_ms));
			} else {
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
				log.log(Level.SEVERE, String.format("timeStep_ms=%,d isSampleTimePassed=%b isSamplingDone=%b isTime4Sample=%b", timeStep_ms, this.timeStep.isSampleTimePassed, this.timeStep.isSamplingDone, this.timeStep.isTime4Sample));
			{
				if (this.lastTimeStep.isMinMax && this.lastTimeStep.isTime4Sample) {
					if (this.timeStep.isMinMax)
						if (this.timeStep.isTime4Sample)
							predecessorAction = Action.FORCE_RELEASE; // {}
						else
							predecessorAction = Action.DEFER_RELEASE; // <> take the next points as they are new minmax values
					else
						predecessorAction = Action.FORCE_RELEASE; // {}
				} else if (this.lastTimeStep.isMinMax && !this.lastTimeStep.isTime4Sample) {
					if (this.timeStep.isMinMax)
						if (this.timeStep.isTime4Sample)
							predecessorAction = Action.DISCARD;
						else
							predecessorAction = Action.DEFER_RELEASE; // <> take the next points as they are new minmax values
					else
						predecessorAction = Action.FORCE_RELEASE; // OVERSAMPLING;
				} else if (!this.lastTimeStep.isMinMax && this.lastTimeStep.isTime4Sample) {
					if (this.timeStep.isMinMax)
						if (this.timeStep.isTime4Sample)
							predecessorAction = Action.COMPENSATE_RELEASE; // ()
						else
							predecessorAction = Action.DEFER_RELEASE; // <>
					else
						predecessorAction = Action.COMPENSATE_RELEASE; // ()
				} else if (!this.lastTimeStep.isMinMax && !this.lastTimeStep.isTime4Sample) {
					predecessorAction = Action.DISCARD;
				} else {
					predecessorAction = null; // is never reached
				}
				if (log.isLoggable(Level.FINEST))
					// if (timeStep_ms > 1107500 && timeStep_ms < 1115000)
					log.log(Level.SEVERE, String.format("action=%s isLastMinMax=%b isLastTime4Sample=%b isMinMax=%b isTime4Sample=%b", predecessorAction.toString(), this.lastTimeStep.isMinMax, this.lastTimeStep.isTime4Sample, this.timeStep.isMinMax, this.timeStep.isTime4Sample));
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
				} else {
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
			if (log.isLoggable(Level.FINEST))
				log.log(Level.SEVERE, String.format("%5s  %s isValidSample=%b isSamplingDone=%b readingCount=%d samplingCount=%d", isValidSample, predecessorAction.toString(), isValidSample, this.timeStep.isSamplingDone, this.readingCount, this.samplingCount));
			if (this.lastTimeStep.nextStartTimeStamp_ms < this.timeStep.nextStartTimeStamp_ms) {
				this.oversamplingCount += this.lastTimeStep.timeSpanSamplingCount > 1 ? this.lastTimeStep.timeSpanSamplingCount - 1 : 0; // one sample per timespan is the standard case
				if (log.isLoggable(Level.FINEST) && this.oversamplingCount > 0)
					log.log(Level.SEVERE, "******************** "
							+ String.format("timeSpanSamplingCount =%,9d samplingCount=%,9d oversamplingCount=%,9d oversamplingCountMax=%,9d   ", this.timeStep.timeSpanSamplingCount, this.samplingCount, this.oversamplingCount, this.oversamplingCount));
			}
			if (log.isLoggable(Level.FINEST) && this.oversamplingCount > 0)
				// if (timeStep_ms > 1107500 && timeStep_ms < 1115000)
				log.log(Level.SEVERE, String.format("%,9d  ", timeStep_ms) + String.format(String.format("%0" + this.oversamplingCount + "d", 0)));
			// prepare the sample for fetching by the caller
			this.lastTimeStep = this.timeStep;
		}
		return isValidSample;
	}

	private boolean calculateMinMax(int[] points) {
		boolean isChanged = false;
		for (int i = 0; i < points.length; i++) {
			int point = points[i];
			if (point < minPoints[i]) {
				minPoints[i] = point;
				isChanged = true;
			}
			if (point > maxPoints[i]) {
				maxPoints[i] = point;
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

}
