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
    
    Copyright (c) 2008 - 2010 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.logging.Logger;


/**
 * Class to maintain the voltage levels of Lithium battries used for cell voltage bar graph, ...
 * @author Winfried Brügmann
 */
public class CellVoltageValues {
	final static String					$CLASS_NAME						= CellVoltageValues.class.getName();
	final static Logger					log										= Logger.getLogger(CellVoltageValues.$CLASS_NAME);
	
	public final static String[] upperLimitVoltage		= new String[] { "4.600", "4.500", "4.400", "4.300", "4.200", "4.100", "4.000", "3.900", "3.800", "3.700", "3.600", "1.600", "1.500", "1.400", "1.300"};
	public final static String[] upperLimitColorRed 	= new String[] { "4.615", "4.515", "4.415", "4.315", "4.215", "4.115", "4.015", "3.915", "3.815", "3.715", "3.615", "1.600", "1.550", "1.500", "1.450", "1.400"};
	public final static String[] lowerLimitColorGreen = new String[] { "4.550", "4.450", "4.350", "4.250", "4.150", "4.050", "3.950", "3.850", "3.750", "3.650", "3.550", "1.350", "1.300", "1.250", "1.200"};
	public final static String[] beginSpreadVoltage 	= new String[] { "4.400", "4.300", "4.200", "4.100", "4.000", "3.900", "3.800", "3.700", "3.600", "3.500", "3.400", "3.300", "3.200", "3.300", "1.100", "1.000", "0.900"};
	public final static String[] lowerLimitColorRed		= new String[] { "3.000", "2.900", "2.800", "2.700", "2.600", "2.500", "2.400", "2.300", "2.200", "2.100", "2.000", "0.900", "0.800", "0.700", "0.600"};
	public final static String[] lowerLimitVoltage		= new String[] { "2.700", "2.600", "2.500", "2.400", "2.300", "2.200", "2.000", "1.900", "1.800", "1.700", "1.600", "1.500", "1.000", "0.500", "0.000" };
	
	public final static int[]			liPoLimits					= new int[] {4200, 4215, 4150, 4000, 2600, 2300};
	public final static int[]			liIoLimits					= new int[] {4100, 4115, 4050, 3900, 2600, 2300};
	public final static int[]			liFeLimits					= new int[] {3800, 3815, 3750, 3300, 2100, 1900};
	public final static int[]			niMhLimits					= new int[] {1500, 1500, 1250, 1000, 800, 500};

	// all initial values fit to LiPo akku type LiPo 3.7 V, LiIo 3.6 V LiFe 3.3 V
	static int[]	voltageLimits				= new int[] {4200, 4215, 4150, 4000, 2600, 2300};

	public static int[] getLiPoVoltageLimits() {
		//{upperLimitVoltage=0,  upperLimitColorRed=1, lowerLimitColorGreen=2, beginSpreadVoltage=3, lowerLimitColorRed=4, lowerLimitVoltage=5}; 
		CellVoltageValues.setVoltageLimits(liPoLimits);
		return voltageLimits;
	}

	public static int[] getLiIoVoltageLimits() {
		//{upperLimitVoltage=0,  upperLimitColorRed=1, lowerLimitColorGreen=2, beginSpreadVoltage=3, lowerLimitColorRed=4, lowerLimitVoltage=5}; 
		CellVoltageValues.setVoltageLimits(liIoLimits);
		return voltageLimits;
	}

	public static int[] getLiFeVoltageLimits() {
		//{upperLimitVoltage=0,  upperLimitColorRed=1, lowerLimitColorGreen=2, beginSpreadVoltage=3, lowerLimitColorRed=4, lowerLimitVoltage=5}; 
		CellVoltageValues.setVoltageLimits(liFeLimits);
		return voltageLimits;
	}

	public static int[] getNiMhVoltageLimits() {
		//{upperLimitVoltage=0,  upperLimitColorRed=1, lowerLimitColorGreen=2, beginSpreadVoltage=3, lowerLimitColorRed=4, lowerLimitVoltage=5}; 
		CellVoltageValues.setVoltageLimits(niMhLimits);
		return voltageLimits;
	}

	/**
	 * @return the voltage limits as int array
	 */
	public static int[] getVoltageLimits() {
		return voltageLimits;
	}

	/**
	 * set the voltage limits for the bar graph
	 * @param newUpperLimitVoltage the upperLimitVoltage to set
	 * @param newUpperLimitColorRed the upperLimitColorRed to set
	 * @param newLowerLimitColorRed the lowerLimitColorRed to set
	 * @param newBeginSpreadVoltage the beginSpreadVoltage to set
	 * @param newLowerLimitColorGreen the lowerLimitColorGreen to set
	 * @param newLowerLimitVoltage the lowerLimitVoltage to set
	 */
	public static void setVoltageLimits(int newUpperLimitVoltage, int newUpperLimitColorRed, int newLowerLimitColorGreen, int newBeginSpreadVoltage, int newLowerLimitColorRed, int newLowerLimitVoltage) {
		voltageLimits[0] = newUpperLimitVoltage;
		voltageLimits[1] = newUpperLimitColorRed;
		voltageLimits[2] = newLowerLimitColorGreen;
		voltageLimits[3] = newBeginSpreadVoltage;
		voltageLimits[4] = newLowerLimitColorRed;
		voltageLimits[5] = newLowerLimitVoltage;
	}

	/**
	 * set the voltage limits for the bar graph
	 * @param newVoltageLimits the voltage limits to set
	 */
	public static void setVoltageLimits(int[] newVoltageLimits) {
		if (voltageLimits.length <= newVoltageLimits.length)
			System.arraycopy(newVoltageLimits, 0, voltageLimits, 0, voltageLimits.length);
		else if (voltageLimits.length > newVoltageLimits.length)
			System.arraycopy(newVoltageLimits, 0, voltageLimits, 0, newVoltageLimits.length);			
	}

	/**
	 * method to compare voltage limit settings
	 * @return true if all limits match limits2compare values
	 */
	public static boolean compareVoltageLimits(int[] limits2compared) {
		return voltageLimits[0] == limits2compared[0] 
		    && voltageLimits[1] == limits2compared[1] 
		    && voltageLimits[2] == limits2compared[2] 
		    && voltageLimits[3] == limits2compared[3] 
		    && voltageLimits[4] == limits2compared[4] 
		    && voltageLimits[5] == limits2compared[5];
	}

}
