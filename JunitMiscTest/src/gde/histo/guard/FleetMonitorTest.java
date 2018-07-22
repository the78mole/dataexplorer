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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde.histo.guard;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gde.histo.base.NonUiTestCase;
import gde.histo.datasources.HistoSetTest;
import gde.histo.guard.FleetMonitor.ObjectSummary;

/**
 *
 * @author Thomas Eickert (USER)
 */
class FleetMonitorTest extends NonUiTestCase {
	private final static String	$CLASS_NAME	= HistoSetTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * @throws java.lang.Exception
	 */
	@Override
	@BeforeEach
	protected void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	/**
	 * Test method for {@link gde.histo.guard.FleetMonitor#defineOverview(java.util.function.Function)}.
	 */
	@Test
	void testDefineOverview() {
		this.analyzer.getDeviceConfigurations(); // get now because lazy loading will disturb response times
		FleetMonitor fleetMonitor = new FleetMonitor();
		log.log(Level.FINER, "FleetMonitor initialized");
		List<ObjectSummary> overview = fleetMonitor.defineOverview("KwikFly");
		log.log(Level.OFF, "KwikFly  HoTTAdapter  number of overview records", overview.size());
		assertFalse("no records found", overview.isEmpty());
	}
}
