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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
									2017 Thomas Eickert
****************************************************************************************/
package gde.junit;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import gde.histo.utils.Quantile;
import gde.histo.utils.Quantile.Fixings;
import gde.histo.utils.Spot;
import gde.histo.utils.UniversalQuantile;

public class QuantileTest extends TestSuperClass { 
	//maybe better to choose another directory structure: http://stackoverflow.com/a/2388285 
	//-> we have our own JunitTest project referenced hint is related if test code is part of each project only
	private final static String	$CLASS_NAME										= QuantileTest.class.getName();
	private final static Logger	log														= Logger.getLogger($CLASS_NAME);

	/**
	 * value 100; reduce to 0 to shorten the elapsed Junit test time
	 */
	private final static int		performanceTestLoops					= 100;

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
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
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
		// special double example :  -zero
		final Double d3 = -0d; // try this code with d3 = 0d; for comparison
		if (d3 < 0d)
			System.out.println("is never printed");
		else
			System.out.println("Double -zero is equal to +zero");
		if (Double.compare(d3, 0d) < 0)
			System.out.println("Double -zero is less compared to +zero");
		else
			System.out.println("is never printed");

		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		for (int i = 0; i < 15; i++) {
			record.addAll(record);
		}
		{
			this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
			ArrayList<Double> arrayList = new ArrayList<Double>();
			for (Integer value : record) {
				arrayList.add(value != null ? value.doubleValue() : null);
			}
			Quantile quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
			log.log(Level.INFO, ">>> Class Quantile <<<");
			log.log(Level.INFO, "Avg   " + this.quantile.getAvgFigure() + " bisher " + this.quantile.getAvgOBS());
			log.log(Level.INFO, "Sigma " + this.quantile.getSigmaFigure() + " bisher " + this.quantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + quantileArray.getAvgFigure() + " bisher " + quantileArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + quantileArray.getSigmaFigure() + " bisher " + quantileArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					nanoTimeSigmaInt -= System.nanoTime();
					this.quantile.getSigmaRunningOBS();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), true);
					quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), true);
					nanoTimeSigmaInt -= System.nanoTime();
					this.quantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms Stream -> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
					nanoTimeSigmaInt -= System.nanoTime();
					this.quantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
		{
			ArrayList<Point> recordPoints = new ArrayList<>();
			ArrayList<Point2D.Double> arrayList = new ArrayList<>();
			int counter = 0;
			for (Integer value : record) {
				if (value != null) {
					recordPoints.add(new Point(counter, value));
					arrayList.add(new Point2D.Double(counter, value));
				}
				counter++;
			}
			//			this.quantile = new Quantile(recordPoints,  6, 9);
			Quantile quantileArray = new Quantile(arrayList, 6, 9);
			log.log(Level.INFO, ">>> Class Quantile with Point2D <<<");
			//			log.log(Level.INFO, "Avg   " + this.quantile.getAvgFigure() + " bisher " + this.quantile.getAvgOBS());
			//			log.log(Level.INFO, "Sigma " + this.quantile.getSigmaFigure() + " bisher " + this.quantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + quantileArray.getAvgFigure() + " bisher " + quantileArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + quantileArray.getSigmaFigure() + " bisher " + quantileArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					//					genericQuantile = new GenericQuantile(recordList, false, 6, 9, exclusions);
					quantileArray = new Quantile(arrayList, 6, 9);
					//					nanoTimeSigmaInt -= System.nanoTime();
					//					genericQuantile.getSigmaRunningOBS();
					//					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(2 * (System.nanoTime() - nanoTime)) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					//					genericQuantile = new GenericQuantile(recordList, false, 6, 9, exclusions);
					quantileArray = new Quantile(arrayList, 6, 9);
					//					nanoTimeSigmaInt -= System.nanoTime();
					//					genericQuantile.getSigmaFigure();
					//					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					quantileArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(2 * (System.nanoTime() - nanoTime)) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
		{
			List<Integer> recordList = new ArrayList<>(record);
			ArrayList<Double> arrayList = new ArrayList<>();
			for (Integer value : record) {
				arrayList.add(value != null ? value.doubleValue() : null);
			}
			List<Integer> iExclusions = new ArrayList<>();
			iExclusions.add(null);
			List<Double> dExclusions = new ArrayList<>();
			dExclusions.add(null);
			UniversalQuantile<Integer> genericQuantile = new UniversalQuantile<>(recordList, false, 6., 9., iExclusions);
			UniversalQuantile<Double> genericArray = new UniversalQuantile<>(arrayList, false, 6., 9., dExclusions);
			log.log(Level.INFO, ">>> Class GenericQuantile with Number <<<");
			log.log(Level.INFO, "Avg   " + genericQuantile.getAvgFigure() + " bisher " + genericQuantile.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericQuantile.getSigmaFigure() + " bisher " + genericQuantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + genericArray.getAvgFigure() + " bisher " + genericArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericArray.getSigmaFigure() + " bisher " + genericArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericQuantile = new UniversalQuantile<>(recordList, false, 6., 9., iExclusions);
					genericArray = new UniversalQuantile<>(arrayList, false, 6., 9., dExclusions);
					nanoTimeSigmaInt -= System.nanoTime();
					genericQuantile.getSigmaRunningOBS();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericQuantile = new UniversalQuantile<>(recordList, false, 6., 9., iExclusions);
					genericArray = new UniversalQuantile<>(arrayList, false, 6., 9., dExclusions);
					nanoTimeSigmaInt -= System.nanoTime();
					genericQuantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
		{
			List<Spot<Integer>> recordPoints = new ArrayList<>();
			List<Spot<Integer>> iExclusions = new ArrayList<>();
			iExclusions.add(null);
			List<Spot<Double>> arrayPoints = new ArrayList<>();
			List<Spot<Double>> dExclusions = new ArrayList<>();
			dExclusions.add(null);
			int counter = 0;
			for (Integer value : record) {
				if (value != null) {
					recordPoints.add(new Spot<Integer>(counter, value));
					arrayPoints.add(new Spot<Double>((double) counter, (double) value));
				}
				counter++;
			}
			UniversalQuantile<Integer> genericQuantile = new UniversalQuantile<>(recordPoints, 6., 9.);
			UniversalQuantile<Double> genericArray = new UniversalQuantile<>(arrayPoints, 6., 9.);
			log.log(Level.INFO, ">>> Class GenericQuantile with Spot<Number> <<<");
			log.log(Level.INFO, "Avg   " + genericQuantile.getAvgFigure() + " bisher " + genericQuantile.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericQuantile.getSigmaFigure() + " bisher " + genericQuantile.getSigmaRunningOBS());
			log.log(Level.INFO, "Avg   " + genericArray.getAvgFigure() + " bisher " + genericArray.getAvgOBS());
			log.log(Level.INFO, "Sigma " + genericArray.getSigmaFigure() + " bisher " + genericArray.getSigmaRunningOBS());

			for (int j = 0; j < 4; j++) {
				long nanoTime = System.nanoTime(), nanoTimeSigmaInt = 0, nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericQuantile = new UniversalQuantile<>(recordPoints, 6., 9.);
					genericArray = new UniversalQuantile<>(arrayPoints, 6., 9.);
					nanoTimeSigmaInt -= System.nanoTime();
					genericQuantile.getSigmaRunningOBS();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaRunningOBS();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms noStream> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
				nanoTime = System.nanoTime();
				nanoTimeSigmaInt = 0;
				nanoTimeSigmaDouble = 0;
				for (int i = 0; i < performanceTestLoops / 2; i++) {
					genericQuantile = new UniversalQuantile<>(recordPoints, 6., 9.);
					genericArray = new UniversalQuantile<>(arrayPoints, 6., 9.);
					nanoTimeSigmaInt -= System.nanoTime();
					genericQuantile.getSigmaFigure();
					nanoTimeSigmaInt += System.nanoTime();
					nanoTimeSigmaDouble -= System.nanoTime();
					genericArray.getSigmaFigure();
					nanoTimeSigmaDouble += System.nanoTime();
				}
				log.log(Level.INFO, "ms opt  ---> " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + "    sigmaInt " + TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaInt) + "    sigmaDouble "
						+ TimeUnit.NANOSECONDS.toMillis(nanoTimeSigmaDouble));
			}
		}
	}

	/**
	 * ET: Vector    Integer performance for sigma based on parallel streams vs. conventional: 17 sec vs. 23 sec
	 * ET: Vector    Double  performance for sigma based on parallel streams vs. conventional: 39 sec vs. 71 sec
	 * ET: ArrayList Double  performance for sigma based on parallel streams vs. conventional: 38 sec vs. 69 sec
	 * ET figures: 10.000 calculations for a Vector / ArrayList with 589.824 elements
	 */
	private void estSigmaPerformance() { // ET 10.06.2017 remove sigmaFigure caching before reactivating
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		for (int i = 0; i < 15; i++) {
			record.addAll(record);
		}
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
		ArrayList<Double> arrayList = new ArrayList<Double>();
		for (Integer value : record) {
			arrayList.add(value != null ? value.doubleValue() : null);
		}
		Quantile quantileArray = new Quantile(arrayList, EnumSet.of(Fixings.REMOVE_NULLS), 6, 9);
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
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS, Fixings.IS_SAMPLE), 6, 9);
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
			this.quantile = new Quantile(record, EnumSet.noneOf(Fixings.class), 6, 9);
			fail("Should throw an exception");
		}
		catch (Exception e) {
			//
		}
	}

	public void testPopulationZero2Null() {
		Vector<Integer> record = new Vector<>(Arrays.asList(recordArray));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
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
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		assertEquals("qLowerWhiskerZeros2Null=" + qLowerWhiskerZeros2Null, qLowerWhiskerZeros2Null, this.quantile.getQuantileLowerWhisker(), DELTA);
		assertEquals("qUpperWhiskerZeros2Null=" + qUpperWhiskerZeros2Null, qUpperWhiskerZeros2Null, this.quantile.getQuantileUpperWhisker(), DELTA);
		log.log(Level.INFO, " ---> " + this.quantile.getQuantileLowerWhisker());
		log.log(Level.INFO, " ---> " + this.quantile.getQuantileUpperWhisker());
	}

	public void testQuantilesAtSize1() {
		Vector<Integer> record = new Vector<>(Arrays.asList(size1Array));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		assertEquals("q0Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile0());
		assertEquals("q1Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile1());
		assertEquals("q2Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile2());
		assertEquals("q3Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile3());
		assertEquals("q4Zeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuartile4());
		assertEquals("q33PerCentZeros2Null=" + qxSize1Array, qxSize1Array, this.quantile.getQuantile(.33));
	}

	public void testQuantilesAtSize1ArrayNull() {
		Vector<Integer> record = new Vector<>(Arrays.asList(size1ArrayNull));
		this.quantile = new Quantile(record, EnumSet.of(Fixings.REMOVE_NULLS, Fixings.REMOVE_ZEROS), 6, 9);
		try {
			this.quantile.getQuartile2();
			fail("Should throw an exception");
		}
		catch (Exception e) {
			//
		}
	}
}
