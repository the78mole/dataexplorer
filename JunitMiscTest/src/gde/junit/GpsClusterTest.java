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

import java.util.logging.Level;
import java.util.logging.Logger;

import gde.histo.gpslocations.GeoCodes;
import gde.histo.gpslocations.GpsCluster;
import gde.histo.utils.GpsCoordinate;

public class GpsClusterTest extends TestSuperClass {
	//maybe better to choose another directory structure: http://stackoverflow.com/a/2388285
	//-> we have our own JunitTest project referenced hint is related if test code is part of each project only
	private final static String	$CLASS_NAME	= GpsClusterTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private GpsCluster					gpsCluster;
	private final GpsCoordinate	s21					= new GpsCoordinate(48.7829167, 9.1822427);
	private final GpsCoordinate	s21_E				= new GpsCoordinate(48.7829160, 9.1822420);			// approx 0.1 m distance
	private final GpsCoordinate	sts					= new GpsCoordinate(48.7810382, 9.1840221);
	private final GpsCoordinate	ssa					= new GpsCoordinate(41.8843714, 12.4801847);
	private final GpsCoordinate	chi					= new GpsCoordinate(-43.8098021, -176.7061281);
	private final GpsCoordinate	nkv					= new GpsCoordinate(-38.977128, 177.858698);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	public void testSingleMemberCluster() {
		this.s21.toAngularCoordinate();
		new GpsCoordinate(48.781970, 9.183130).toAngularCoordinate();
		this.sts.toAngularCoordinate();
		this.ssa.toAngularCoordinate();
		this.chi.toAngularCoordinate();
		this.nkv.toAngularCoordinate();

		assertEquals("stsDistance", .246, this.s21.getDistance(this.sts), .001);
		assertEquals("ssaDistance", 809.034, this.s21.getDistance(this.ssa), .6);
		assertEquals("chiDistance", 19259.79, this.s21.getDistance(this.chi), 42.);
		assertEquals("nkvDistance", 702.745, this.nkv.getDistance(this.chi), .5);

		assertEquals("equal objects", this.s21, this.s21);
		assertEquals("not equal but within GPS accuracy", this.s21, this.s21_E);
		assertNotSame("close but beyond GPS accuracy", this.s21, this.sts);

		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(s21_E);
		assertEquals("s21-s21_E center", this.s21_E, this.gpsCluster.getCenter());

		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(sts);
		assertEquals("s21-sts center", new GpsCoordinate(48.781970, 9.183130), this.gpsCluster.getCenter());

		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(sts);
		this.gpsCluster.add(ssa);
		assertEquals("s21-sts-ssa center", new GpsCoordinate(46.494939, 10.373230), this.gpsCluster.getCenter());

		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(ssa);
		this.gpsCluster.add(chi);
		this.gpsCluster.add(nkv);
		GpsCoordinate center1 = this.gpsCluster.getCenter();
		assertEquals("center BW with New Zealand", new GpsCoordinate(19.359332, 115.308856), center1);
		this.gpsCluster.setClusters(); // 1,9450991|95,4767329
		assertEquals("SingleMemberClusters", 4, this.gpsCluster.getClusters().size());

		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(sts);
		this.gpsCluster.add(ssa);
		this.gpsCluster.setClusters();
		assertEquals("S TwoMemberCluster", 3, this.gpsCluster.getClusters().size());
	}

	public void testLocationFiles() {
		String location;
		location = GeoCodes.getLocation(this.s21);
		System.out.println(location);
		location = GeoCodes.getLocation(this.s21_E);
		System.out.println(location);
		location = GeoCodes.getLocation(this.sts);
		System.out.println(location);
		location = GeoCodes.getLocation(this.ssa);
		System.out.println(location);
		location = GeoCodes.getLocation(this.chi);
		System.out.println(location);
		location = GeoCodes.getLocation(this.nkv);
		System.out.println(location);
		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(sts);
		this.gpsCluster.add(ssa);
		location = GeoCodes.getLocation(this.gpsCluster.getCenter());
		System.out.println("center of S21, sts, ssa : " + location);
		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(ssa);
		location = GeoCodes.getLocation(this.gpsCluster.getCenter());
		System.out.println("center of S21, ssa : " + location);
		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(ssa);
		this.gpsCluster.add(chi);
		location = GeoCodes.getLocation(this.gpsCluster.getCenter());
		System.out.println("center of S21, ssa, chi : " + location);
		this.gpsCluster = new GpsCluster();
		this.gpsCluster.add(s21);
		this.gpsCluster.add(chi);
		location = GeoCodes.getLocation(this.gpsCluster.getCenter());
		System.out.println("center of S21, chi : " + location + "    " + this.gpsCluster.getCenter().toCsvString());
	}
}
