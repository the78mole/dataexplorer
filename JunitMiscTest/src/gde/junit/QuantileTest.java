/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
									2016 Thomas Eickert
****************************************************************************************/
package gde.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import gde.utils.Quantile;
import gde.utils.Quantile.Fixings;

public class QuantileTest extends TestSuperClass { // TODO maybe better to choose another directory structure: http://stackoverflow.com/a/2388285
	private final static String	$CLASS_NAME										= QuantileTest.class.getName();
	private final static Logger	log														= Logger.getLogger($CLASS_NAME);

	private Quantile						quantile;
	private final Integer[]			recordArray										= { 99999, -99999, null, 0, null, 8, 10, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, -5 };	// Size = 18
	private final Integer[]			sortedArray										= { -99999, -5, 0, 8, 10, 25, 49, 50, 51, 75, 99, 100, 101, 134, 175, 99999, null, null };	// realSize = 16 with zero
	private final double				q0WithZeros										= -99999;																																										// minimum
	private final double				q1WithZeros										= (8 + 10) / 2.0;																																						// 25%
	private final double				q2WithZeros										= (50 + 51) / 2.0;																																					// median
	private final double				q3WithZeros										= (100 + 101) / 2.0;																																				// 75%
	private final double				q4WithZeros										= 99999.;																																										// maximum
	private final double				q33PerCentWithZeros						= 25.;
	private final double				q0Zeros2Null									= -99999.;																																									// minimum
	private final double				q1Zeros2Null									= 10.;																																											// 25%
	private final double				q2Zeros2Null									= 51.;																																											// median
	private final double				q3Zeros2Null									= 101.;																																											// 75%
	private final double				q4Zeros2Null									= 99999.;																																										// maximum
	private final double				q33PerCentZeros2Null					= 25.;
	private final double				lowerWhiskerZeros2NullLimit		= 10. - (101 - 10) * 1.5;																																		// -126.5																												
	private final double				upperWhiskerZeros2NullLimit		= 101. + (101 - 10) * 1.5;																																	// 237.5																												
	private final double				qLowerWhiskerZeros2Null				= -5;
	private final double				qUpperWhiskerZeros2Null				= 175.;
	private final double				lowerWhiskerSampleWithZeros		= 10. - (101 - 10) * 1.5;																																		// -126.5
	private final double				upperWhiskerSampleWithZeros		= 101. + (101 - 10) * 1.5;																																	// 237.5
	private final double				qLowerWhiskerSampleWithZeros	= -129.875;																																									// equals the lower fence (1.5*IQR below Q1) for the sample 
	private final double				qUpperWhiskerSampleWithZeros	= 239.125;																																									// equals the upper fence (1.5*IQR above Q3) for the sample
	private final double				q0SampleWithZeros							= -99999;																																										// minimum
	private final double				q1SampleWithZeros							= 8 + .25 * (10 - 8);																																				// 25%
	private final double				q2SampleWithZeros							= 50 + .5 * (51 - 50);																																			// median
	private final double				q3SampleWithZeros							= 100 + .75 * (101 - 100);																																	// 75%
	private final double				q4SampleWithZeros							= 99999.;																																										// maximum
	private final double				q33PerCentSampleWithZeros			= 10 + .61 * (25 - 10);
	private final double				q0SampleZeros2Null						= -99999.;																																									// minimum
	private final double				q1SampleZeros2Null						= 10.;																																											// 25%
	private final double				q2SampleZeros2Null						= 51.;																																											// median
	private final double				q3SampleZeros2Null						= 101.;																																											// 75%
	private final double				q4SampleZeros2Null						= 99999.;																																										// maximum
	private final double				q33PerCentSampleZeros2Null		= 25 + .28 * (49 - 25);
	private final Integer[]			size1Array										= { -99999 };
	private final double				qxSize1Array									= -99999.;
	private final Integer[]			size1ArrayNull								= { null };

	private static final double	DELTA													= 1e-9;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	public void testSigma() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS),6);
		final double avg = this.quantile.getAvgFigure();
		final double avgSlow = this.quantile.getAvgSlow();
		final double sigmaOBS = this.quantile.getSigmaOBS();
		final double sigmaRunningOBS = this.quantile.getSigmaRunningOBS();
		final double sigma = this.quantile.getSigmaFigure();
		assertEquals("getAvgFigure=" + avg, avg, avgSlow, DELTA);
		assertEquals("getSigmaOBS=" + sigmaOBS, sigmaOBS, sigmaRunningOBS, DELTA);
		assertEquals("getSigmaFigure=" + sigma, sigma, sigmaRunningOBS, DELTA);
	}

	public void testSortPerformance() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		for (int i = 0; i < 15; i++) {
			record.addAll(record);
		}

		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6);
		ArrayList<Double> arrayList = new ArrayList<Double>();
		for (Integer value : record) {
			arrayList.add(value != null ? value.doubleValue() : null);
		}
		Quantile quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS),6);
		log.log(Level.INFO, "Avg   " + this.quantile.getAvgFigure() + " bisher " + this.quantile.getAvgOBS());
		log.log(Level.INFO, "Sigma " + this.quantile.getSigmaFigure() + " bisher " + this.quantile.getSigmaOBS());
		log.log(Level.INFO, "Avg   " + quantileArray.getAvgFigure() + " bisher " + quantileArray.getAvgOBS());
		log.log(Level.INFO, "Sigma " + quantileArray.getSigmaFigure() + " bisher " + quantileArray.getSigmaOBS());

		for (int j = 0; j < 4; j++) {
			long nanoTime = System.nanoTime(), nanoTimeSigma =0;
			int qCount = 100;
			for (int i = 0; i < qCount / 2; i++) {
				this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS),6);
				quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS),6);
				nanoTimeSigma -= System.nanoTime();
				this.quantile.getSigmaOBS();
				quantileArray.getSigmaOBS();
				nanoTimeSigma += System.nanoTime();
			}
			log.log(Level.INFO, "ms bisher ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigma " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigma));
			nanoTime = System.nanoTime();
			nanoTimeSigma = 0;
			for (int i = 0; i < qCount / 2; i++) {
				this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), true);
				quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), true);
				nanoTimeSigma -= System.nanoTime();
				this.quantile.getSigmaFigure();
				quantileArray.getSigmaFigure();
				nanoTimeSigma += System.nanoTime();
			}
			log.log(Level.INFO, "ms neu    ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigma " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigma));
			nanoTime = System.nanoTime();
			nanoTimeSigma = 0;
			for (int i = 0; i < qCount / 2; i++) {
				this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS),6);
				quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS),6);
				nanoTimeSigma -= System.nanoTime();
				this.quantile.getSigmaFigure();
				quantileArray.getSigmaFigure();
				nanoTimeSigma += System.nanoTime();
			}
			log.log(Level.INFO, "ms opt    ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigma " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigma));
		}
	}

	/**
	 * ET: Vector    Integer performance for sigma based on parallel streams vs. conventional: 17 sec vs. 23 sec
	 * ET: Vector    Double  performance for sigma based on parallel streams vs. conventional: 39 sec vs. 71 sec
	 * ET: ArrayList Double  performance for sigma based on parallel streams vs. conventional: 38 sec vs. 69 sec
	 * ET figures: 10.000 calculations for a Vector / ArrayList with 589.824 elements 
	 */
	private void estSigmaPerformance() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		for (int i = 0; i < 15; i++) {
			record.addAll(record);
		}
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6);
		ArrayList<Double> arrayList = new ArrayList<Double>();
		for (Integer value : record) {
			arrayList.add(value != null ? value.doubleValue() : null);
		}
		Quantile quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6);
		log.log(Level.INFO, " ---> " + this.quantile.getAvgFigure());
		log.log(Level.INFO, " ---> " + this.quantile.getAvgSlow());
		log.log(Level.INFO, " ---> " + this.quantile.getSigmaOBS());
		log.log(Level.INFO, " ---> " + this.quantile.getSigmaRunningOBS());
		log.log(Level.INFO, " ---> " + this.quantile.getSigmaFigure());
		long nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			this.quantile.getSigmaOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			this.quantile.getSigmaRunningOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			this.quantile.getSigmaFigure();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			this.quantile.getSigmaOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			this.quantile.getSigmaRunningOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			this.quantile.getSigmaFigure();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			quantileArray.getSigmaOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			quantileArray.getSigmaRunningOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			quantileArray.getSigmaFigure();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			quantileArray.getSigmaOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			quantileArray.getSigmaRunningOBS();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
		nanoTime = System.nanoTime();
		for (int i = 0; i < 9999; i++) {
			quantileArray.getSigmaFigure();
		}
		log.log(Level.INFO, " ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime));
	}

	public void testSampleZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS, Fixings.IS_SAMPLE),6);
		assertEquals("q0SampleZeros2Null=" + q0SampleZeros2Null, q0SampleZeros2Null, this.quantile.getQuartile0(), DELTA);
		assertEquals("q1SampleZeros2Null=" + q1SampleZeros2Null, q1SampleZeros2Null, this.quantile.getQuartile1(), DELTA);
		assertEquals("q2SampleZeros2Null=" + q2SampleZeros2Null, q2SampleZeros2Null, this.quantile.getQuartile2(), DELTA);
		assertEquals("q3SampleZeros2Null=" + q3SampleZeros2Null, q3SampleZeros2Null, this.quantile.getQuartile3(), DELTA);
		assertEquals("q4SampleZeros2Null=" + q4SampleZeros2Null, q4SampleZeros2Null, this.quantile.getQuartile4(), DELTA);
		assertEquals("q33PerCentSampleZeros2Null=" + q33PerCentSampleZeros2Null, q33PerCentSampleZeros2Null, this.quantile.getQuantile(.33), DELTA);
		log.log(Level.INFO, " ---> " + this.quantile.getQuartile3());
	}

	public void testPopulationWithZerosForbiddenNulls() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		try {
			this.quantile = new Quantile(record, EnumSet.noneOf(Fixings.class),6);
			fail("Should throw an exception");
		}
		catch (Exception e) {
			//
		}
	}

	public void testPopulationZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS),6);
		assertEquals("q0Zeros2Null=" + q0Zeros2Null, q0Zeros2Null, this.quantile.getQuartile0());
		assertEquals("q1Zeros2Null=" + q1Zeros2Null, q1Zeros2Null, this.quantile.getQuartile1());
		assertEquals("q2Zeros2Null=" + q2Zeros2Null, q2Zeros2Null, this.quantile.getQuartile2());
		assertEquals("q3Zeros2Null=" + q3Zeros2Null, q3Zeros2Null, this.quantile.getQuartile3());
		assertEquals("q4Zeros2Null=" + q4Zeros2Null, q4Zeros2Null, this.quantile.getQuartile4());
		assertEquals("q33PerCentZeros2Null=" + q33PerCentZeros2Null, q33PerCentZeros2Null, this.quantile.getQuantile(.33));
		log.log(Level.INFO, " ---> " + this.quantile.getQuartile3());
	}

	public void testQuantilesWhiskerZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS),6);
		assertEquals("qLowerWhiskerZeros2Null=" + qLowerWhiskerZeros2Null, qLowerWhiskerZeros2Null, this.quantile.getQuantileLowerWhisker(), DELTA);
		assertEquals("qUpperWhiskerZeros2Null=" + qUpperWhiskerZeros2Null, qUpperWhiskerZeros2Null, this.quantile.getQuantileUpperWhisker(), DELTA);
		log.log(Level.INFO, " ---> " + this.quantile.getQuantileLowerWhisker());
		log.log(Level.INFO, " ---> " + this.quantile.getQuantileUpperWhisker());
	}

	public void testQuantilesAtSize1() {
		Vector<Integer> record = new Vector<>(Arrays.asList(size1Array));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS),6);
		assertEquals("q0Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile0());
		assertEquals("q1Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile1());
		assertEquals("q2Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile2());
		assertEquals("q3Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile3());
		assertEquals("q4Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile4());
		assertEquals("q33PerCentZeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuantile(.33));
	}

	public void testQuantilesAtSize1ArrayNull() {
		Vector<Integer> record = new Vector<>(Arrays.asList(size1ArrayNull));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS),6);
		try {
			this.quantile.getQuartile2();
			fail("Should throw an exception");
		}
		catch (Exception e) {
			//
		}
	}
}
