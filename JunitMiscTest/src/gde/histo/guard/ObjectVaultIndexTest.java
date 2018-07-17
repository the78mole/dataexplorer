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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gde.histo.base.HistoTestCase;
import gde.histo.datasources.HistoSetTest;
import gde.histo.guard.ObjectVaultIndex.DetailSelector;

/**
 *
 * @author Thomas Eickert (USER)
 */
class ObjectVaultIndexTest extends HistoTestCase {
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
	 * Test method for {@link gde.histo.guard.ObjectVaultIndex#rebuild()}.
	 */
	@Test
	void testRebuild() {
		long nanoTime = System.nanoTime();
		new ObjectVaultIndex().rebuild();
		System.out.println("elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " [ms]");
	}

	/**
	 * Test method for {@link gde.histo.guard.ObjectVaultIndex#selectVaultKeys(Function, Function)}.
	 */
	@Test
	void testSelectVaults() throws Exception {
		Stream.of(1, 2, 3, 4, 5).forEach(i -> {
			long nanoTime = System.nanoTime();
			DetailSelector detailSelector = DetailSelector.createEmptyFilter();
			new ObjectVaultIndex().selectVaultKeys(detailSelector);
			System.out.println("elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " [ms]");

			nanoTime = System.nanoTime();
			new ObjectVaultIndex(new String[] { "Kult" }).selectVaultKeys(new String[] { "Kult" }, detailSelector);
			System.out.println("selected strict elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " [ms]");

			nanoTime = System.nanoTime();
			new ObjectVaultIndex().selectVaultKeys(new String[] { "Kult" }, detailSelector);
			System.out.println("selected       elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " [ms]");

			nanoTime = System.nanoTime();
			detailSelector = DetailSelector.createFunctionFilter(o -> o.logStartTimestampMs <= 1347111194000L);
			new ObjectVaultIndex().selectVaultKeys(new String[] { "Kult" }, detailSelector);
			System.out.println("selected                    elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime) + " [ms]");
		});
	}
}