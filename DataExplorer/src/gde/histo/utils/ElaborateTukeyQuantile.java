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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable quantile calculation of a probability distribution after removing outliers.
 * Supports numbers only.<p>
 * Types of removed outliers:
 * <Li>Castaways lie beyond 9 * IQR
 * <Li>Constant outliers lie beyond 3 * IQR, are the outmost value which occurs with the highest frequency in this range
 * @see <a href="https://www.google.de/search?q=Tukey+boxplot">Tukey Boxplot</a>
 * @author Thomas Eickert (USER)
 */
public final class ElaborateTukeyQuantile<T extends Number & Comparable<T>> extends UniversalQuantile<T> { // todo check merge with UniversalQuantile

	/**
	 * This sigma value for the inner 50% of the population.<br>
	 * Interquartile range <em>IQR = 0.25 < p < 0.75</em>
	 */
	public static final double	INTER_QUARTILE_SIGMA		= 0.674489694;
	/**
	 * Values beyond this limit are outliers.<br>
	 * Maximum whisker length <em>MWL = c * IQR</em>.
	 */
	public static final double	CLOSE_OUTLIER_LIMIT			= 1.5;
	/**
	 * Outliers below this limit are close outliers, those beyond this limit are far outliers.<br>
	 * Outlier distance limit <em>ODL = c * IQR</em>.
	 */
	public static final int			FAR_OUTLIER_LIMIT				= 3;
	/**
	 * Outliers beyond this limit are discarded.<br>
	 * Castaways outlier distance limit (<em>CODL = c * IQR</em>.
	 */
	public static final int			CASTAWAY_OUTLIER_LIMIT	= 9;

	/**
	 * Used for channel item values from records during vault creation.
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 */
	public ElaborateTukeyQuantile(List<T> population, boolean isSample) {
		super(population, isSample, INTER_QUARTILE_SIGMA, 99., 99., new ArrayList<>());
	}

	/**
	 * Used for channel item values from records during vault creation.
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param removeCastaways true removes castaways which lie beyond 9 * IQR
	 */
	public ElaborateTukeyQuantile(List<T> population, boolean isSample, boolean removeCastaways) {
		super(population, isSample, INTER_QUARTILE_SIGMA, removeCastaways ? CASTAWAY_OUTLIER_LIMIT : 99., //
				removeCastaways ? CASTAWAY_OUTLIER_LIMIT : 99., new ArrayList<>());
	}

	/**
	 * Used for channel item values from records during vault creation.
	 * @param population
	 * @param isSample true calculates the sample standard deviation
	 * @param removeCastaways true removes castaways which lie beyond 9 * IQR
	 * @param removeConstantOutliers true removes constant outliers lie beyond 3 * IQR, are the outmost value and occur with the highest
	 *          frequency in this range
	 */
	public ElaborateTukeyQuantile(List<T> population, boolean isSample, boolean removeCastaways, boolean removeConstantOutliers) {
		super(population, isSample, INTER_QUARTILE_SIGMA, removeCastaways ? CASTAWAY_OUTLIER_LIMIT : 99., //
				removeConstantOutliers ? FAR_OUTLIER_LIMIT : removeCastaways ? CASTAWAY_OUTLIER_LIMIT : 99., new ArrayList<>());
	}

}
