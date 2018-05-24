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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018 Winfried Bruegmann
									2017,2018 Thomas Eickert
****************************************************************************************/
package gde.histo.gpslocations;

import static org.junit.Assert.assertNotEquals;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gde.histo.base.BasicTestCase;
import gde.histo.utils.GpsCoordinate;

class GpsGeoCodesTest extends BasicTestCase {
	private final static String	$CLASS_NAME	= GpsGeoCodesTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	@Test
	void testGetRandomLocation() {
		Random random = new Random();
		double lat = random.nextFloat() * 20. + 40.; // Napoli to Stockholm
		double lon = random.nextFloat() * 20.; // Greenwich to Krakow
		String location = GeoCodes.getLocation(new GpsCoordinate(lat, lon));
		System.out.println(location);
		assertNotEquals("  location", "", location);
	}

	@Test
	void testLocationFiles() {
		String location;
		location = GeoCodes.getLocation(new GpsCoordinate(0., 179.)); // pure ocean
		System.out.println(location);
		assertEquals("  location", "", location);
		location = GeoCodes.getLocation(new GpsCoordinate(48.8696877, 10.3573054)); // 73441 Bopfingen, Germany (via Google)
		System.out.println(location);
		assertTrue("  location", location.contains("Heidmühle, Bopfingen, Verwaltungsgemeinschaft Bopfingen, Ostalbkreis, Regierungsbezirk Stuttgart, Baden-Württemberg, 73441, Deutschland"));
		location = GeoCodes.getLocation(new GpsCoordinate(45.0064253, 14.3965299));
		System.out.println(location);
		assertEquals("  location", "D100, Vodice, Primorsko-goranska županija, 51557, Hrvatska", location);
	}
}
