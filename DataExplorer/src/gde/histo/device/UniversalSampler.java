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

    Copyright (c) 2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
    					2016,2017 Thomas Eickert
****************************************************************************************/
package gde.histo.device;

import static java.util.logging.Level.FINER;

import java.util.Arrays;
import java.util.Random;

import gde.config.Settings;
import gde.device.TransitionClassTypes;
import gde.device.TransitionType;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Randomized measurements sampling based on a constant sampling timespan which also supports a 100% sampling.<br>
 * Supports oversampling (i.e. sampling timespans with multiple sample sets) in order not to loose extremum values.<br>
 * Remarks: <br>
 * a) The sampling algorithm guarantees all min/max points to be elements of the samples.<br>
 * This may result in a small number of additional samples ('oversampling') in sampling timespans with multiple new min/max sets.<br>
 * b) The algorithm may be taught with a selection of sample points to reduce oversampling.<br>
 * Max/min estimations derived form a sufficient number of point sets can be fed into the Sampler object.<br>
 * c) The samples are selected randomly.<br>
 * So multiple runs will neither select the same samples nor the same number of samples.
 * @author Thomas Eickert
 */
public final class UniversalSampler {
	private final static String		$CLASS_NAME				= UniversalSampler.class.getName();
	private final static Logger		log								= Logger.getLogger($CLASS_NAME);

	private final int							samplingTimespan_ms;																	// actual used timespan; one sample in this time span +
																																											// potential oversampling samples

	private int										readingCount			= 0;
	private int										samplingCount			= 0;

	private final int							pointsLength;																					// supports internal points array with more elements than the
																																											// external points

	private Candidate							lastCandidate;

	// for random sampling only
	private Candidate							thisCandidate;																				// 2nd candidate pool element
	private int										oversamplingCount	= 0;
	private final MaxMinObserver	maxMinObserver;
	private final Random					rand							= new Random();

	/**
	 * Decide if sampling is required.
	 * Set the sampling timespan based on transition requirements.
	 * @param channelNumber is the log channel number which may differ in case of channel mix
	 * @param maxPoints log measurement points with a length equal to the number of log measurement points
	 * @param minPoints measurement points with a length equal to the number of log measurement points
	 * @param newRecordTimespan_ms log measurement rate
	 */
	private UniversalSampler(int channelNumber, int[] maxPoints, int[] minPoints, int newRecordTimespan_ms) {
		if (maxPoints.length != minPoints.length) throw new IllegalArgumentException();

		this.pointsLength = maxPoints.length;

		// find the maximum sampling timespan
		int proposedTimespan_ms = Settings.getInstance().getSamplingTimespan_ms();
		for (TransitionType transitionType : DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getChannelType(channelNumber).getTransitions().values()) {
			if (transitionType.getClassType() == TransitionClassTypes.PEAK) {
				// ensure that the peak consists of at least 2 measurements
				if (transitionType.getPeakMinimumTimeMsec() == null)
					proposedTimespan_ms = Math.min(proposedTimespan_ms, newRecordTimespan_ms * 2);
				else
					proposedTimespan_ms = Math.min(proposedTimespan_ms, transitionType.getPeakMinimumTimeMsec().orElse(0) / 2);
			}
			// ensure that both the reference time and the threshold time consists of at least 2 measurements
			// do not care about the recovery time because it might be 0 in case of slopes
			int detectableTimespan_ms = Math.min(transitionType.getReferenceTimeMsec(), transitionType.getThresholdTimeMsec()) / 2;
			proposedTimespan_ms = Math.min(proposedTimespan_ms, detectableTimespan_ms);
		}

		if (proposedTimespan_ms >= newRecordTimespan_ms) {
			// sampling timespan must not be smaller than the recording timespan
			this.samplingTimespan_ms = proposedTimespan_ms;
			this.maxMinObserver = new MaxMinObserver(maxPoints, minPoints);
			this.lastCandidate = new RandomCandidate(this.samplingTimespan_ms, this.maxMinObserver);
			this.thisCandidate = new RandomCandidate(this.samplingTimespan_ms, this.maxMinObserver);
		} else {
			this.samplingTimespan_ms = -1;
			this.maxMinObserver = null;
			this.lastCandidate = new Candidate();
			this.thisCandidate = null;
		}
		log.log(FINER, "", this); //$NON-NLS-1$
	}

	/**
	 * @param channelNumber is the log channel number which may differ in case of channel mix
	 * @param newPointsLength number of log measurement points
	 * @param newRecordTimespan_ms log measurement rate
	 * @return a new instance
	 */
	public static UniversalSampler createSampler(int channelNumber, int newPointsLength, int newRecordTimespan_ms) { // reading a binFile with 500K
																																																										// records into a recordset takes
																																																										// 45 to 15 s; reading the file
																																																										// with 5k samples takes 0,60 to
																																																										// 0,35 s which is 1% to 2,5%
		int[] tmpMaxPoints = new int[newPointsLength];
		int[] tmpMinPoints = new int[newPointsLength];
		Arrays.fill(tmpMaxPoints, Integer.MIN_VALUE);
		Arrays.fill(tmpMinPoints, Integer.MAX_VALUE);
		return new UniversalSampler(channelNumber, tmpMaxPoints, tmpMinPoints, newRecordTimespan_ms);
	}

	/**
	 * @param channelNumber is the log channel number which may differ in case of channel mix
	 * @param maxPoints log measurement points with a length equal to the number of log measurement points
	 * @param minPoints measurement points with a length equal to the number of log measurement points
	 * @param newRecordTimespan_ms log measurement rate
	 * @return a new instance
	 */
	public static UniversalSampler createSampler(int channelNumber, int[] maxPoints, int[] minPoints, int newRecordTimespan_ms) {
		return new UniversalSampler(channelNumber, maxPoints, minPoints, newRecordTimespan_ms);
	}

	@Override
	public String toString() {
		return String.format("pointsLength=%d  samplingTimespan_ms=%d userSamplingTimespan_ms=%d  readingCount=%d samplingCount=%d oversamplingCount=%d", this.pointsLength, this.samplingTimespan_ms, Settings.getInstance().getSamplingTimespan_ms(), this.readingCount, this.samplingCount, this.oversamplingCount);
	}

	/**
	 * Accept or reject a new candidate.
	 * <p>
	 * In case of random sampling:<br>
	 * Try to take not more than one sample per sampling timespan.
	 * Thus a premature sample is only taken if a minmax state has been detected which did not persist in the next measurement points
	 * (oversampling case).
	 * In all cases the sample is made ready for use one cycle later which results in loosing the last sample of the population.
	 * @param newPoints
	 * @param newTimeStep_ms
	 * @return true if a valid sample is available which must be fetched by calling the properties getSamplePoints and getSampleTimeStep_ms
	 */
	public boolean isValidSample(int[] newPoints, long newTimeStep_ms) {
		boolean isValidSample;
		if (!(this.lastCandidate instanceof RandomCandidate)) {
			this.lastCandidate.processSample(newPoints, newTimeStep_ms);

			isValidSample = true;
			setCounters(1, 0, isValidSample);
		} else {
			// take the next candidate from the pool
			RandomCandidate candidate = (RandomCandidate) this.lastCandidate;
			// shift the current candidate
			this.lastCandidate = this.thisCandidate;
			isValidSample = candidate.processSamplingCandidate(newPoints, newTimeStep_ms, (RandomCandidate) this.lastCandidate, this.rand);

			int overSamplingIncrement = candidate.isNewTimeSpan() ? candidate.getOverSamplingCount() : 0;
			setCounters(1, overSamplingIncrement, isValidSample);

			this.thisCandidate = candidate;
		}
		return isValidSample;
	}

	/**
	 * @param increment
	 * @param overSamplingIncrement
	 * @param isValidSample
	 */
	private void setCounters(int increment, int overSamplingIncrement, boolean isValidSample) {
		this.readingCount += increment;
		if (isValidSample) {
			this.samplingCount += increment;
		}
		this.oversamplingCount += overSamplingIncrement;
		if (overSamplingIncrement > 0)
			log.finer(() -> String.format("%,12d  ", this.lastCandidate.timeStep_ms) + String.format(String.format("%0" + this.oversamplingCount + "d", 0)));
	}

	/**
	 * Supports using the minMax values from a previous run.
	 * Reduces oversampling.
	 * @param newMaxPoints holds the values or is an array with length 0 which results in resetting the values
	 * @param newMinPoints holds the values or is an array with length 0 which results in resetting the values
	 */
	public void setMaxMinPoints(int[] newMaxPoints, int[] newMinPoints) {
		if (this.maxMinObserver != null) {
			if (newMaxPoints.length == 0 || newMinPoints.length == 0) {
				int[] maxPoints = new int[this.pointsLength];
				Arrays.fill(maxPoints, Integer.MIN_VALUE);
				int[] minPoints = new int[this.pointsLength];
				Arrays.fill(minPoints, Integer.MAX_VALUE);
				this.maxMinObserver.setMaxMinPoints(maxPoints, minPoints);
			} else if (this.pointsLength != newMaxPoints.length || this.pointsLength != newMinPoints.length) {
				throw new UnsupportedOperationException();
			} else {
				this.maxMinObserver.setMaxMinPoints(newMaxPoints, newMinPoints);
			}
		}
	}

	/**
	 * @return the points or null if there is no valid sample
	 */
	public int[] getSamplePoints() {
		return this.lastCandidate.points;
	}

	/**
	 * @return the timestep
	 */
	public long getSampleTimeStep_ms() {
		return this.lastCandidate.timeStep_ms;
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

	public int[] getMaxPoints() {
		return this.maxMinObserver != null ? this.maxMinObserver.maxPoints : new int[0];
	}

	public int[] getMinPoints() {
		return this.maxMinObserver != null ? this.maxMinObserver.minPoints : new int[0];
	}

}
