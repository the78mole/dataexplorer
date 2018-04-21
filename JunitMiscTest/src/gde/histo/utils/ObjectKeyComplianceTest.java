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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/
package gde.histo.utils;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import gde.histo.base.HistoTestCase;
import gde.utils.ObjectKeyCompliance;

class ObjectKeyComplianceTest extends HistoTestCase {
	private final static String	$CLASS_NAME	= ObjectKeyComplianceTest.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		log.setLevel(Level.INFO);
		log.setUseParentHandlers(true);
	}

	@Tag("performance")
	@Test
	void testReadSourcePathsObjectKeysPerformance() {
		for (int i = 0; i < 7; i++) {
			long nanoTime = System.nanoTime();
			Set<String> objectKeyCandidates = ObjectKeyCompliance.defineObjectKeyCandidates(application.getDeviceConfigurations().getAllConfigurations());

			System.out.println(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " ms");
			System.out.println(objectKeyCandidates);
		}
	}

}
