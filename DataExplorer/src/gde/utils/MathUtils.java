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

import gde.log.Level;

import java.util.Locale;
import java.util.logging.Logger;

/**
 * Class contains mathematics utility methods
 * @author Winfried Brügmann
 */
public class MathUtils {
	private static Logger log = Logger.getLogger(MathUtils.class.getName());
	
	/**
	 * round up results in higher value 
	 * round up for a positive value results in a higher value
	 * round up for a negative value results in a higher value
	 * @param value
	 * @return rounded double value
	 */
	public static double roundUp(double value) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (value < 0.25)
					roundValue = value + (0.05 - value % 0.05);
				else if (value < 0.5)
					roundValue = value + (0.01 - value % 0.01);
				else if (value < 1.0)
					roundValue = value + (0.05 - value % 0.05);
				else if (value < 2.5)
					roundValue = value + (0.1 - value % 0.1);
				else if (value < 5)
					roundValue = value + (0.25 - value % 0.25);
				else if (value < 10)
					roundValue = value + (0.5 - value % 0.5);
				else if (value < 25)
					roundValue = value + (1 - value % 1);
				else if (value < 50)
					roundValue = value + (2.5 - value % 2.5);
				else if (value < 100)
					roundValue = value + (5 - value % 5);
				else if (value < 500)
					roundValue = value + (10 - value % 10);
				else if (value < 1000)
					roundValue = value + (50 - value % 50);
				else  
					roundValue = value + (100 - value % 100);
			}
			else {// value < 0 
				if (value > -0.25)
					roundValue = value - (value % 0.05);
				else if (value > -0.5)
					roundValue = value - (value % 0.01);
				else if (value > -1)
					roundValue = value - (value % 0.05);
				else if (value > -2.5)
					roundValue = value - (value % 0.1);
				else if (value > -5)
					roundValue = value - (value % 0.25);
				else if (value > -10)
					roundValue = value - (value % 0.5);
				else if (value > -25)
					roundValue = value - (value % 1);
				else if (value > -50)
					roundValue = value - (value % 2.5);
				else if (value > -100)
					roundValue = value - (value % 5);
				else if (value > -500)
					roundValue = value - (value % 10);
				else if (value > -1000)
					roundValue = value - (value % 50);
				else
					roundValue = value - (value % 100);
			}
		}
		return roundValue;
	}

	/**
	 * @param value
	 * @return
	 */
	private static double checkRoundReq(double value) {
		double roundValue = 0.0;
		if (value > 0)
			roundValue = value <= 0.1 ? value - (value*100%5)/100 : value <= 0.5 ? value - (value*100%1)/100 : value <= 1 ? value - (value*10%1)/10 : value <= 10 ? value - value %1 : value <= 50 ? value - value %10 : value <= 500 ? value - value %50 : value - value % 100 ;
		else
			roundValue = value >= -0.1 ? value - (value*100%5)/100 : value >= -0.5 ? value - (value*100%1)/100 : value >= -1 ? value - (value*10%1)/10 : value >= -10 ? value - value %1 : value >= -50 ? value - value %10 : value >= -500 ? value - value %50 : value - value % 100 ;
		return roundValue;
	}
	
	/**
	 * round down results in a lower value 
	 * round down for a positive value results in a lower value
	 * round down for a negative value results in a lower value
	 * @param value
	 * @return rounded double value
	 */
	public static double roundDown(double value) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (value < 0.25)
					roundValue = value - (value % 0.05);
				else if (value < 0.5)
					roundValue = value - (value % 0.025);
				else if (value < 1)
					roundValue = value - (value % 0.05);
				else if (value < 2.5)
					roundValue = value - (value % 0.1);
				else if (value < 5)
					roundValue = value - (value % 0.25);
				else if (value < 10)
					roundValue = value - (value % 0.5);
				else if (value < 25)
					roundValue = value - (value % 1);
				else if (value < 25)
					roundValue = value - (value % 2.5);
				else if (value < 100)
					roundValue = value - (value % 5);
				else if (value < 500)
					roundValue = value - (value % 10);
				else if (value < 1000)
					roundValue = value - (value % 50);
				else  
					roundValue = value - (value % 100);
			}
			else {// value < 0 
				if (value > -0.25)
					roundValue = value - (0.05 + value % 0.05);
				else if (value > -0.5)
					roundValue = value - (0.025 + value % 0.025);
				else if (value > -1)
					roundValue = value - (0.05 + value % 0.05);
				else if (value > -2.5)
					roundValue = value - (0.1 + value % 0.1);
				else if (value > -5)
					roundValue = value - (0.25 + value % 0.25);
				else if (value > -10)
					roundValue = value - (0.5 + value % 0.5);
				else if (value > -25)
					roundValue = value - (1 + value % 1);
				else if (value > -50)
					roundValue = value - (2.5 + value % 2.5);
				else if (value > -100)
					roundValue = value - (5 + value % 5);
				else if (value > -500)
					roundValue = value - (10 + value % 10);
				else if (value > -1000)
					roundValue = value - (50 + value % 50);
				else
					roundValue = value - (100 + value % 100);
			}
		}
		return roundValue;
	}
	
	/**
	 * round up given value according value delta value level 
	 * round up for a positive value results in a higher value
	 * round up for a negative value results in a higher value
	 * @param value
	 * @param delta
	 * @return rounded double value
	 */
	public static double roundUp(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.05)
					roundValue = value + (0.01 - value % 0.01);
				else if (delta < 0.25)
					roundValue = value + (0.05 - value % 0.05);
				else if (delta < 0.5)
					roundValue = value + (0.025 - value % 0.025);
				else if (delta < 1.0)
					roundValue = value + (0.05 - value % 0.05);
				else if (delta < 2.5)
					roundValue = value + (0.1 - value % 0.1);
				else if (delta < 5)
					roundValue = value + (0.25 - value % 0.25);
				else if (delta < 10)
					roundValue = value + (0.5 - value % 0.5);
				else if (delta < 25)
					roundValue = value + (1 - value % 1);
				else if (delta < 50)
					roundValue = value + (2.5 - (value % 2.5));
				else if (delta < 100)
					roundValue = value + (5 - value % 5);
				else if (delta < 500)
					roundValue = value + (10 - value % 10);
				else if (delta < 1000)
					roundValue = value + (50 - value % 50);
				else  
					roundValue = value + (100 - value % 100);
			}
			else {// value < 0 
				if (delta < 0.05)
					roundValue = value - (value % 0.01);
				else if (delta < 0.25)
					roundValue = value - (value % 0.05);
				else if (delta < 0.5)
					roundValue = value - (value % 0.025);
				else if (delta < 1)
					roundValue = value - (value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (value % 0.1);
				else if (delta < 5)
					roundValue = value - (value % 0.25);
				else if (delta < 10)
					roundValue = value - (value % 0.5);
				else if (delta < 25)
					roundValue = value - (value % 1);
				else if (delta < 50)
					roundValue = value - (value % 2.5);
				else if (delta < 100)
					roundValue = value - (value % 5);
				else if (delta < 500)
					roundValue = value - (value % 10);
				else if (delta < 1000)
					roundValue = value - (value % 50);
				else
					roundValue = value - (value % 100);
			}
		}
		else if (delta == 0) {
			if (value > 0) {
				roundValue = value + 0.05;
			}
			else {
				roundValue = value - 0.05;
			}
		}
		return roundValue;
	}
	
	/**
	 * round down given value according value delta value level 
	 * round down for a positive value results in a lower value
	 * round down for a negative value results in a lower value
	 * @param value
	 * @param delta
	 * @return rounded double value
	 */
	public static double roundDown(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.05)
					roundValue = value - (value % 0.01);
				else if (delta < 0.25)
					roundValue = value - (value % 0.05);
				else if (delta < 0.5)
					roundValue = value - (value % 0.025);
				else if (delta < 1.0)
					roundValue = value - (value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (value % 0.1);
				else if (delta < 5)
					roundValue = value - (value % 0.25);
				else if (delta < 10)
					roundValue = value - (value % 0.5);
				else if (delta < 25)
					roundValue = value - (value % 1);
				else if (delta < 50)
					roundValue = value - (value % 2.5);
				else if (delta < 100)
					roundValue = value - (value % 5);
				else if (delta < 500)
					roundValue = value - (value % 10);
				else if (delta < 1000)
					roundValue = value - (value % 50);
				else  
					roundValue = value - (value % 100);
			}
			else {// value < 0 
				if (delta < 0.05)
					roundValue = value - (0.01 + value % 0.01);
				else if (delta < 0.25)
					roundValue = value - (0.05 + value % 0.05);
				else if (delta < 0.5)
					roundValue = value - (0.025 + value % 0.025);
				else if (delta < 1.0)
					roundValue = value - (0.05 + value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (0.1 + value % 0.1);
				else if (delta < 5)
					roundValue = value - (0.25 + value % 0.25);
				else if (delta < 10)
					roundValue = value - (0.5 + value % 0.5);
				else if (delta < 25)
					roundValue = value - (1 + value % 1);
				else if (delta < 50)
					roundValue = value - (2.5 + value % 2.5);
				else if (delta < 100)
					roundValue = value - (5 + value % 5);
				else if (delta < 500)
					roundValue = value - (10 + value % 10);
				else if (delta < 1000)
					roundValue = value - (50 + value % 50);
				else
					roundValue = value - (100 + value % 100);
			}
		}
		else if (delta == 0) {
			if (value > 0) {
				roundValue = value - 0.05;
			}
			else {
				roundValue = value + 0.05;
			}
		}
		return roundValue;
	}
	
	/**
	 * round up given value according value delta value level for auto scale
	 * round up for a positive value results in a higher value
	 * round up for a negative value results in a higher value
	 * @param value
	 * @param delta
	 * @return rounded double value
	 */
	public static double roundUpAuto(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.05)
					roundValue = value + (0.0001 - value % 0.0001);
				else if (delta < 0.25)
					roundValue = value + (0.001 - value % 0.001);
				else if (delta < 0.5)
					roundValue = value + (0.01 - value % 0.01);
				else if (delta < 1)
					roundValue = value + (0.025 - value % 0.025);
				else if (delta < 2.5)
					roundValue = value + (0.05 - value % 0.05);
				else if (delta < 5)
					roundValue = value + (0.1 - value % 0.1);
				else if (delta < 10)
					roundValue = value + (0.25 - value % 0.25);
				else if (delta < 25)
					roundValue = value + (0.5 - value % 0.5);
				else if (delta < 50)
					roundValue = value + (1 - value % 1);
				else if (delta < 100)
					roundValue = value + (2.5 - (value % 2.5));
				else if (delta < 500)
					roundValue = value + (5 - value % 5);
				else if (delta < 1000)
					roundValue = value + (10 - value % 10);
				else if (delta < 2000)
					roundValue = value + (50 - value % 50);
				else  
					roundValue = value + (100 - value % 100);
			}
			else {// value < 0 
				if (delta < 0.05)
					roundValue = value - (value % 0.0001);
				else if (delta < 0.25)
					roundValue = value - (value % 0.001);
				else if (delta < 0.5)
					roundValue = value - (value % 0.01);
				else if (delta < 1)
					roundValue = value - (value % 0.025);
				else if (delta < 2.5)
					roundValue = value - (value % 0.05);
				else if (delta < 5)
					roundValue = value - (value % 0.1);
				else if (delta < 10)
					roundValue = value - (value % 0.25);
				else if (delta < 25)
					roundValue = value - (value % 0.5);
				else if (delta < 50)
					roundValue = value - (value % 1);
				else if (delta < 100)
					roundValue = value - (value % 2.5);
				else if (delta < 500)
					roundValue = value - (value % 5);
				else if (delta < 1000)
					roundValue = value - (value % 10);
				else if (delta < 2000)
					roundValue = value - (value % 50);
				else
					roundValue = value - (value % 100);
			}
		}
		return roundValue;
	}
	
	/**
	 * round down given value according value delta value level for auto scale
	 * round down for a positive value results in a lower value
	 * round down for a negative value results in a lower value
	 * @param value
	 * @param delta
	 * @return rounded double value
	 */
	public static double roundDownAuto(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.05)
					roundValue = value - (value % 0.0001);
				else if (delta < 0.25)
					roundValue = value - (value % 0.001);
				else if (delta < 0.5)
					roundValue = value - (value % 0.01);
				else if (delta < 1)
					roundValue = value - (value % 0.025);
				else if (delta < 2.5)
					roundValue = value - (value % 0.05);
				else if (delta < 5)
					roundValue = value - (value % 0.1);
				else if (delta < 10)
					roundValue = value - (value % 0.25);
				else if (delta < 25)
					roundValue = value - (value % 0.5);
				else if (delta < 50)
					roundValue = value - (value % 1);
				else if (delta < 100)
					roundValue = value - (value % 2.5);
				else if (delta < 500)
					roundValue = value - (value % 5);
				else if (delta < 1000)
					roundValue = value - (value % 10);
				else if (delta < 2000)
					roundValue = value - (value % 50);
				else  
					roundValue = value - (value % 100);
			}
			else {// value < 0 
				if (delta < 0.05)
					roundValue = value - (0.0001 + value % 0.0001);
				else if (delta < 0.25)
					roundValue = value - (0.001 + value % 0.001);
				else if (delta < 0.5)
					roundValue = value - (0.01 + value % 0.01);
				else if (delta < 1)
					roundValue = value - (0.025 + value % 0.025);
				else if (delta < 2.5)
					roundValue = value - (0.025 + value % 0.05);
				else if (delta < 5)
					roundValue = value - (0.05 + value % 0.1);
				else if (delta < 10)
					roundValue = value - (0.25 + value % 0.25);
				else if (delta < 25)
					roundValue = value - (0.5 + value % 0.5);
				else if (delta < 50)
					roundValue = value - (1 + value % 1);
				else if (delta < 100)
					roundValue = value - (2.5 + value % 2.5);
				else if (delta < 500)
					roundValue = value - (5 + value % 5);
				else if (delta < 1000)
					roundValue = value - (10 + value % 10);
				else if (delta < 2000)
					roundValue = value - (50 + value % 50);
				else
					roundValue = value - (100 + value % 100);
			}
		}
		return roundValue;
	}


	/**
	 * adapted rounding  
	 * - a small number needs different rounding compared to a big number 0.05 -> 0.1, 529 -> 550
	 * - a small value delta needs different rounding compared to a big delta 10 -> +-1, 200 +-10 
	 * - think about delta scale at this time to enable %2 or %5 or %10, depending on delta value
	 * @param minValue
	 * @param maxValue 
	 * @param isAuto - if true no real rounding will applied, scale end values are calculated during scale value calculation
	 * 							 - if false end values are rounded and based on this values the scale will be calculated
	 * @param maxNumberTicks - required as limit for scale value calculation
	 * @return double array roundMinValue, roundMaxValue 
	 */
	public static Object[] adaptRounding(double minValue, double maxValue, boolean isAuto, int maxNumberTicks) {
		Object[] results = null;
		double tmpMinValue = 0.0, tmpMaxValue = 0.0, deltaValue = maxValue - minValue;

		// process normal rounding mechanism first
		if (isAuto) {
			tmpMinValue = minValue;
			tmpMaxValue = maxValue;
		}
		else { // normal rounding
			if (minValue != 0) {
				tmpMinValue = MathUtils.roundDown(minValue, deltaValue);
			}
			if (maxValue != 0) {
				tmpMaxValue = MathUtils.roundUp(maxValue, deltaValue);
			}
		}

		// check delta scale enable easy readable tick marks
		double tmpDeltaScale = tmpMaxValue - tmpMinValue;
		if (isAuto) {
			if (tmpDeltaScale <= 0) {
				results = evaluateNumTicksAuto(tmpMinValue - 0.05, tmpMaxValue + 0.05, maxNumberTicks, 1000);
			}
			if (tmpDeltaScale <= 0.005) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 20000);
			}
			else if (tmpDeltaScale <= 0.010) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 10000);
			}
			else if (tmpDeltaScale <= 0.050) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 2000);
			}
			else if (tmpDeltaScale <= 0.10) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 1000);
			}
			else if (tmpDeltaScale <= 0.50) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 200);
			}
			else if (tmpDeltaScale <= 1) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 100);
			}
			else if (tmpDeltaScale <= 5) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 20);
			}
			else if (tmpDeltaScale <= 10) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 10);
			}
			else if (tmpDeltaScale <= 50) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 2);
			}
			else if (tmpDeltaScale <= 100) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 1);
			}
			else if (tmpDeltaScale <= 500) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.2);
			}
			else if (tmpDeltaScale <= 1000) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.1);
			}
			else if (tmpDeltaScale <= 5000) {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.02);
			}
			else {
				results = evaluateNumTicksAuto(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.01);
			}
		}
		else {
			if (tmpDeltaScale <= 0) {
				results = evaluateNumTicks(tmpMinValue - 0.05, tmpMaxValue + 0.05, maxNumberTicks, 1000);
			}
			if (tmpDeltaScale <= 0.005) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 20000);
			}
			else if (tmpDeltaScale <= 0.010) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 10000);
			}
			else if (tmpDeltaScale <= 0.050) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 2000);
			}
			else if (tmpDeltaScale <= 0.10) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 1000);
			}
			else if (tmpDeltaScale <= 0.50) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 200);
			}
			else if (tmpDeltaScale <= 1) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 100);
			}
			else if (tmpDeltaScale <= 5) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 50);
			}
			else if (tmpDeltaScale <= 10) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 10);
			}
			else if (tmpDeltaScale <= 50) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 2);
			}
			else if (tmpDeltaScale <= 100) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 1);
			}
			else if (tmpDeltaScale <= 500) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.2);
			}
			else if (tmpDeltaScale <= 1000) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.1);
			}
			else if (tmpDeltaScale <= 5000) {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.02);
			}
			else {
				results = evaluateNumTicks(tmpMinValue, tmpMaxValue, maxNumberTicks, 0.01);
			}
		}
		log.log(Level.FINER, minValue + " --> " + tmpMinValue + " " + maxValue + " --> " + tmpMaxValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$	
		return results;
	}

	private static Object[] evaluateNumTicksAuto(double tmpMinValue, double tmpMaxValue, int maxNumberTicks, double raise) {
		int newNumberTicks = 2, newNumberMiniTicks = 5;
		double newMinValue = 0.0, newMaxValue = 0.0;
		boolean isMinNegative = tmpMinValue < 0, isMaxNegative = tmpMaxValue < 0;
		
		if (!isMinNegative && !isMaxNegative) {
			newMinValue = tmpMinValue * raise % 2 != 0 ? tmpMinValue + (2 - tmpMinValue * raise % 2) / raise : tmpMinValue;
			newMaxValue = tmpMaxValue * raise % 2 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 2) / raise : tmpMaxValue;
			newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 2 + 0.5);
			newNumberMiniTicks = 4;
			if (newNumberTicks > maxNumberTicks) {
				newMinValue = tmpMinValue * raise % 4 != 0 ? tmpMinValue + (4 - tmpMinValue * raise % 4) / raise : tmpMinValue;
				newMaxValue = tmpMaxValue * raise % 4 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 4) / raise : tmpMaxValue;
				newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 4 + 0.5);
				newNumberMiniTicks = 4;
				if (newNumberTicks > maxNumberTicks) {
					newMinValue = tmpMinValue * raise % 5 != 0 ? tmpMinValue + (5 - tmpMinValue * raise % 5) / raise : tmpMinValue;
					newMaxValue = tmpMaxValue * raise % 5 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 5) / raise : tmpMaxValue;
					newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 5 + 0.5);
					newNumberMiniTicks = 5;
					if (newNumberTicks > maxNumberTicks) {
						newMinValue = tmpMinValue * raise % 8 != 0 ? tmpMinValue + (8 - tmpMinValue * raise % 8) / raise : tmpMinValue;
						newMaxValue = tmpMaxValue * raise % 8 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 8) / raise : tmpMaxValue;
						newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 8 + 0.5);
						newNumberMiniTicks = 4;
						if (newNumberTicks > maxNumberTicks) {
							newMinValue = tmpMinValue * raise % 10 != 0 ? tmpMinValue + (10 - tmpMinValue * raise % 10) / raise : tmpMinValue;
							newMaxValue = tmpMaxValue * raise % 10 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 10) / raise : tmpMaxValue;
							newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 10 + 0.5);
							newNumberMiniTicks = 5;
							if (newNumberTicks > maxNumberTicks) {
								newMinValue = tmpMinValue * raise % 16 != 0 ? tmpMinValue + (16 - tmpMinValue * raise % 16) / raise : tmpMinValue;
								newMaxValue = tmpMaxValue * raise % 16 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 16) / raise : tmpMaxValue;
								newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 16 + 0.5);
								newNumberMiniTicks = 4;
								if (newNumberTicks > maxNumberTicks) {
									newMinValue = tmpMinValue * raise % 20 != 0 ? tmpMinValue + (20 - tmpMinValue * raise % 20) / raise : tmpMinValue;
									newMaxValue = tmpMaxValue * raise % 20 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 20) / raise : tmpMaxValue;
									newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 20 + 0.5);
									newNumberMiniTicks = 5;
									if (newNumberTicks > maxNumberTicks) {
										newMinValue = tmpMinValue * raise % 32 != 0 ? tmpMinValue + (32 - tmpMinValue * raise % 32) / raise : tmpMinValue;
										newMaxValue = tmpMaxValue * raise % 32 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 32) / raise : tmpMaxValue;
										newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 32 + 0.5);
										newNumberMiniTicks = 4;
										if (newNumberTicks > maxNumberTicks) {
											newMinValue = tmpMinValue * raise % 40 != 0 ? tmpMinValue + (40 - tmpMinValue * raise % 40) / raise : tmpMinValue;
											newMaxValue = tmpMaxValue * raise % 40 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 40) / raise : tmpMaxValue;
											newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 40 + 0.5);
											newNumberMiniTicks = 5;
											if (newNumberTicks > maxNumberTicks) {
												newMinValue = tmpMinValue * raise % 80 != 0 ? tmpMinValue + (80 - tmpMinValue * raise % 80) / raise : tmpMinValue;
												newMaxValue = tmpMaxValue * raise % 80 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 80) / raise : tmpMaxValue;
												newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 80 + 0.5);
												newNumberMiniTicks = 5;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		else if (isMinNegative && !isMaxNegative) {
			newMinValue = tmpMinValue * raise % 2 != 0 ? tmpMinValue - (tmpMinValue * raise % 2) / raise : tmpMinValue;
			newMaxValue = tmpMaxValue * raise % 2 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 2) / raise : tmpMaxValue;
			newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 2 + 0.5);
			newNumberMiniTicks = 4;
			if (newNumberTicks > maxNumberTicks) {
				newMinValue = tmpMinValue * raise % 4 != 0 ? tmpMinValue - (tmpMinValue * raise % 4) / raise : tmpMinValue;
				newMaxValue = tmpMaxValue * raise % 4 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 4) / raise : tmpMaxValue;
				newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 4 + 0.5);
				newNumberMiniTicks = 4;
				if (newNumberTicks > maxNumberTicks) {
					newMinValue = tmpMinValue * raise % 5 != 0 ? tmpMinValue - (tmpMinValue * raise % 5) / raise : tmpMinValue;
					newMaxValue = tmpMaxValue * raise % 5 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 5) / raise : tmpMaxValue;
					newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 5 + 0.5);
					newNumberMiniTicks = 5;
					if (newNumberTicks > maxNumberTicks) {
						newMinValue = tmpMinValue * raise % 8 != 0 ? tmpMinValue - (tmpMinValue * raise % 8) / raise : tmpMinValue;
						newMaxValue = tmpMaxValue * raise % 8 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 8) / raise : tmpMaxValue;
						newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 8 + 0.5);
						newNumberMiniTicks = 4;
						if (newNumberTicks > maxNumberTicks) {
							newMinValue = tmpMinValue * raise % 10 != 0 ? tmpMinValue - (tmpMinValue * raise % 10) / raise : tmpMinValue;
							newMaxValue = tmpMaxValue * raise % 10 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 10) / raise : tmpMaxValue;
							newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 10 + 0.5);
							newNumberMiniTicks = 5;
							if (newNumberTicks > maxNumberTicks) {
								newMinValue = tmpMinValue * raise % 16 != 0 ? tmpMinValue - (tmpMinValue * raise % 16) / raise : tmpMinValue;
								newMaxValue = tmpMaxValue * raise % 16 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 16) / raise : tmpMaxValue;
								newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 16 + 0.5);
								newNumberMiniTicks = 4;
								if (newNumberTicks > maxNumberTicks) {
									newMinValue = tmpMinValue * raise % 20 != 0 ? tmpMinValue - (tmpMinValue * raise % 20) / raise : tmpMinValue;
									newMaxValue = tmpMaxValue * raise % 20 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 20) / raise : tmpMaxValue;
									newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 20 + 0.5);
									newNumberMiniTicks = 5;
									if (newNumberTicks > maxNumberTicks) {
										newMinValue = tmpMinValue * raise % 32 != 0 ? tmpMinValue - (tmpMinValue * raise % 32) / raise : tmpMinValue;
										newMaxValue = tmpMaxValue * raise % 32 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 32) / raise : tmpMaxValue;
										newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 32 + 0.5);
										newNumberMiniTicks = 4;
										if (newNumberTicks > maxNumberTicks) {
											newMinValue = tmpMinValue * raise % 40 != 0 ? tmpMinValue - (tmpMinValue * raise % 40) / raise : tmpMinValue;
											newMaxValue = tmpMaxValue * raise % 40 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 40) / raise : tmpMaxValue;
											newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 40 + 0.5);
											newNumberMiniTicks = 5;
											if (newNumberTicks > maxNumberTicks) {
												newMinValue = tmpMinValue * raise % 80 != 0 ? tmpMinValue - (tmpMinValue * raise % 80) / raise : tmpMinValue;
												newMaxValue = tmpMaxValue * raise % 80 != 0 ? tmpMaxValue - (tmpMaxValue * raise % 80) / raise : tmpMaxValue;
												newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 80 + 0.5);
												newNumberMiniTicks = 5;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		else if (isMinNegative && isMaxNegative) {
			newMinValue = tmpMinValue * raise % 2 != 0 ? tmpMinValue - (tmpMinValue * raise % 2) / raise : tmpMinValue;
			newMaxValue = tmpMaxValue * raise % 2 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 2) / raise : tmpMaxValue;
			newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 2 + 0.5);
			newNumberMiniTicks = 4;
			if (newNumberTicks > maxNumberTicks) {
				newMinValue = tmpMinValue * raise % 4 != 0 ? tmpMinValue - (tmpMinValue * raise % 4) / raise : tmpMinValue;
				newMaxValue = tmpMaxValue * raise % 4 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 4) / raise : tmpMaxValue;
				newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 4 + 0.5);
				newNumberMiniTicks = 4;
				if (newNumberTicks > maxNumberTicks) {
					newMinValue = tmpMinValue * raise % 5 != 0 ? tmpMinValue - (tmpMinValue * raise % 5) / raise : tmpMinValue;
					newMaxValue = tmpMaxValue * raise % 5 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 5) / raise : tmpMaxValue;
					newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 5 + 0.5);
					newNumberMiniTicks = 5;
					if (newNumberTicks > maxNumberTicks) {
						newMinValue = tmpMinValue * raise % 8 != 0 ? tmpMinValue - (tmpMinValue * raise % 8) / raise : tmpMinValue;
						newMaxValue = tmpMaxValue * raise % 8 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 8) / raise : tmpMaxValue;
						newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 8 + 0.5);
						newNumberMiniTicks = 4;
						if (newNumberTicks > maxNumberTicks) {
							newMinValue = tmpMinValue * raise % 10 != 0 ? tmpMinValue - (tmpMinValue * raise % 10) / raise : tmpMinValue;
							newMaxValue = tmpMaxValue * raise % 10 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 10) / raise : tmpMaxValue;
							newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 10 + 0.5);
							newNumberMiniTicks = 5;
							if (newNumberTicks > maxNumberTicks) {
								newMinValue = tmpMinValue * raise % 16 != 0 ? tmpMinValue - (tmpMinValue * raise % 16) / raise : tmpMinValue;
								newMaxValue = tmpMaxValue * raise % 16 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 16) / raise : tmpMaxValue;
								newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 16 + 0.5);
								newNumberMiniTicks = 4;
								if (newNumberTicks > maxNumberTicks) {
									newMinValue = tmpMinValue * raise % 20 != 0 ? tmpMinValue - (tmpMinValue * raise % 20) / raise : tmpMinValue;
									newMaxValue = tmpMaxValue * raise % 20 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 20) / raise : tmpMaxValue;
									newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 20 + 0.5);
									newNumberMiniTicks = 5;
									if (newNumberTicks > maxNumberTicks) {
										newMinValue = tmpMinValue * raise % 32 != 0 ? tmpMinValue - (tmpMinValue * raise % 32) / raise : tmpMinValue;
										newMaxValue = tmpMaxValue * raise % 32 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 32) / raise : tmpMaxValue;
										newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 32 + 0.5);
										newNumberMiniTicks = 4;
										if (newNumberTicks > maxNumberTicks) {
											newMinValue = tmpMinValue * raise % 40 != 0 ? tmpMinValue - (tmpMinValue * raise % 40) / raise : tmpMinValue;
											newMaxValue = tmpMaxValue * raise % 40 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 40) / raise : tmpMaxValue;
											newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 40 + 0.5);
											newNumberMiniTicks = 5;
											if (newNumberTicks > maxNumberTicks) {
												newMinValue = tmpMinValue * raise % 80 != 0 ? tmpMinValue - (tmpMinValue * raise % 80) / raise : tmpMinValue;
												newMaxValue = tmpMaxValue * raise % 80 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 80) / raise : tmpMaxValue;
												newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 80 + 0.5);
												newNumberMiniTicks = 5;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return new Object[] {Double.parseDouble(String.format(Locale.ENGLISH, "%.3f", newMinValue)), Double.parseDouble(String.format(Locale.ENGLISH, "%.3f", newMaxValue)), newNumberTicks, newNumberMiniTicks, new Object()};
	}

	private static Object[] evaluateNumTicks(double tmpMinValue, double tmpMaxValue, int maxNumberTicks, double raise) {
		int newNumberTicks = 2, newNumberMiniTicks = 5;
		double newMinValue = 0.0, newMaxValue = 0.0;
		boolean isMinNegative = tmpMinValue < 0, isMaxNegative = tmpMaxValue < 0;
		
		if (!isMinNegative && !isMaxNegative) {
			newMinValue = tmpMinValue * raise % 2 != 0 ? tmpMinValue - (tmpMinValue * raise % 2) / raise : tmpMinValue;
			newMaxValue = tmpMaxValue * raise % 2 != 0 ? tmpMaxValue + (2 - tmpMaxValue * raise % 2) / raise : tmpMaxValue;
			newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 2 + 0.5);
			newNumberMiniTicks = 4;
			if (newNumberTicks > maxNumberTicks) {
				newMinValue = tmpMinValue * raise % 4 != 0 ? tmpMinValue - (tmpMinValue * raise % 4) / raise : tmpMinValue;
				newMaxValue = tmpMaxValue * raise % 4 != 0 ? tmpMaxValue + (4 - tmpMaxValue * raise % 4) / raise : tmpMaxValue;
				newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 4 + 0.5);
				newNumberMiniTicks = 4;
				if (newNumberTicks > maxNumberTicks) {
					newMinValue = tmpMinValue * raise % 5 != 0 ? tmpMinValue - (tmpMinValue * raise % 5) / raise : tmpMinValue;
					newMaxValue = tmpMaxValue * raise % 5 != 0 ? tmpMaxValue + (5 - tmpMaxValue * raise % 5) / raise : tmpMaxValue;
					newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 5 + 0.5);
					newNumberMiniTicks = 5;
					if (newNumberTicks > maxNumberTicks) {
						newMinValue = tmpMinValue * raise % 8 != 0 ? tmpMinValue - (tmpMinValue * raise % 8) / raise : tmpMinValue;
						newMaxValue = tmpMaxValue * raise % 8 != 0 ? tmpMaxValue + (8 - tmpMaxValue * raise % 8) / raise : tmpMaxValue;
						newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 8 + 0.5);
						newNumberMiniTicks = 4;
						if (newNumberTicks > maxNumberTicks) {
							newMinValue = tmpMinValue * raise % 10 != 0 ? tmpMinValue - (tmpMinValue * raise % 10) / raise : tmpMinValue;
							newMaxValue = tmpMaxValue * raise % 10 != 0 ? tmpMaxValue + (10 - tmpMaxValue * raise % 10) / raise : tmpMaxValue;
							newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 10 + 0.5);
							newNumberMiniTicks = 5;
							if (newNumberTicks > maxNumberTicks) {
								newMinValue = tmpMinValue * raise % 16 != 0 ? tmpMinValue - (tmpMinValue * raise % 16) / raise : tmpMinValue;
								newMaxValue = tmpMaxValue * raise % 16 != 0 ? tmpMaxValue + (16 - tmpMaxValue * raise % 16) / raise : tmpMaxValue;
								newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 16 + 0.5);
								newNumberMiniTicks = 4;
								if (newNumberTicks > maxNumberTicks) {
									newMinValue = tmpMinValue * raise % 20 != 0 ? tmpMinValue - (tmpMinValue * raise % 20) / raise : tmpMinValue;
									newMaxValue = tmpMaxValue * raise % 20 != 0 ? tmpMaxValue + (20 - tmpMaxValue * raise % 20) / raise : tmpMaxValue;
									newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 20 + 0.5);
									newNumberMiniTicks = 5;
									if (newNumberTicks > maxNumberTicks) {
										newMinValue = tmpMinValue * raise % 32 != 0 ? tmpMinValue - (tmpMinValue * raise % 32) / raise : tmpMinValue;
										newMaxValue = tmpMaxValue * raise % 32 != 0 ? tmpMaxValue + (32 - tmpMaxValue * raise % 32) / raise : tmpMaxValue;
										newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 32 + 0.5);
										newNumberMiniTicks = 4;
										if (newNumberTicks > maxNumberTicks) {
											newMinValue = tmpMinValue * raise % 40 != 0 ? tmpMinValue - (tmpMinValue * raise % 40) / raise : tmpMinValue;
											newMaxValue = tmpMaxValue * raise % 40 != 0 ? tmpMaxValue + (40 - tmpMaxValue * raise % 40) / raise : tmpMaxValue;
											newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 40 + 0.5);
											newNumberMiniTicks = 5;
											if (newNumberTicks > maxNumberTicks) {
												newMinValue = tmpMinValue * raise % 80 != 0 ? tmpMinValue - (tmpMinValue * raise % 80) / raise : tmpMinValue;
												newMaxValue = tmpMaxValue * raise % 80 != 0 ? tmpMaxValue + (80 - tmpMaxValue * raise % 80) / raise : tmpMaxValue;
												newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 80 + 0.5);
												newNumberMiniTicks = 5;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		else if (isMinNegative && !isMaxNegative) {
			newMinValue = tmpMinValue * raise % 2 != 0 ? tmpMinValue - (2 + tmpMinValue * raise % 2) / raise : tmpMinValue;
			newMaxValue = tmpMaxValue * raise % 2 != 0 ? tmpMaxValue + (2 - tmpMaxValue * raise % 2) / raise : tmpMaxValue;
			newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 2 + 0.5);
			newNumberMiniTicks = 4;
			if (newNumberTicks > maxNumberTicks) {
				newMinValue = tmpMinValue * raise % 4 != 0 ? tmpMinValue - (4 + tmpMinValue * raise % 4) / raise : tmpMinValue;
				newMaxValue = tmpMaxValue * raise % 4 != 0 ? tmpMaxValue + (4 - tmpMaxValue * raise % 4) / raise : tmpMaxValue;
				newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 4 + 0.5);
				newNumberMiniTicks = 4;
				if (newNumberTicks > maxNumberTicks) {
					newMinValue = tmpMinValue * raise % 5 != 0 ? tmpMinValue - (5 + tmpMinValue * raise % 5) / raise : tmpMinValue;
					newMaxValue = tmpMaxValue * raise % 5 != 0 ? tmpMaxValue + (5 - tmpMaxValue * raise % 5) / raise : tmpMaxValue;
					newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 5 + 0.5);
					newNumberMiniTicks = 5;
					if (newNumberTicks > maxNumberTicks) {
						newMinValue = tmpMinValue * raise % 8 != 0 ? tmpMinValue - (8 + tmpMinValue * raise % 8) / raise : tmpMinValue;
						newMaxValue = tmpMaxValue * raise % 8 != 0 ? tmpMaxValue + (8 - tmpMaxValue * raise % 8) / raise : tmpMaxValue;
						newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 8 + 0.5);
						newNumberMiniTicks = 4;
						if (newNumberTicks > maxNumberTicks) {
							newMinValue = tmpMinValue * raise % 10 != 0 ? tmpMinValue - (10 + tmpMinValue * raise % 10) / raise : tmpMinValue;
							newMaxValue = tmpMaxValue * raise % 10 != 0 ? tmpMaxValue + (10 - tmpMaxValue * raise % 10) / raise : tmpMaxValue;
							newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 10 + 0.5);
							newNumberMiniTicks = 5;
							if (newNumberTicks > maxNumberTicks) {
								newMinValue = tmpMinValue * raise % 16 != 0 ? tmpMinValue - (16 + tmpMinValue * raise % 16) / raise : tmpMinValue;
								newMaxValue = tmpMaxValue * raise % 16 != 0 ? tmpMaxValue + (16 - tmpMaxValue * raise % 16) / raise : tmpMaxValue;
								newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 16 + 0.5);
								newNumberMiniTicks = 4;
								if (newNumberTicks > maxNumberTicks) {
									newMinValue = tmpMinValue * raise % 20 != 0 ? tmpMinValue - (20 + tmpMinValue * raise % 20) / raise : tmpMinValue;
									newMaxValue = tmpMaxValue * raise % 20 != 0 ? tmpMaxValue + (20 - tmpMaxValue * raise % 20) / raise : tmpMaxValue;
									newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 20 + 0.5);
									newNumberMiniTicks = 5;
									if (newNumberTicks > maxNumberTicks) {
										newMinValue = tmpMinValue * raise % 32 != 0 ? tmpMinValue - (32 + tmpMinValue * raise % 32) / raise : tmpMinValue;
										newMaxValue = tmpMaxValue * raise % 32 != 0 ? tmpMaxValue + (32 - tmpMaxValue * raise % 32) / raise : tmpMaxValue;
										newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 32 + 0.5);
										newNumberMiniTicks = 4;
										if (newNumberTicks > maxNumberTicks) {
											newMinValue = tmpMinValue * raise % 40 != 0 ? tmpMinValue - (40 + tmpMinValue * raise % 40) / raise : tmpMinValue;
											newMaxValue = tmpMaxValue * raise % 40 != 0 ? tmpMaxValue + (40 - tmpMaxValue * raise % 40) / raise : tmpMaxValue;
											newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 40 + 0.5);
											newNumberMiniTicks = 5;
											if (newNumberTicks > maxNumberTicks) {
												newMinValue = tmpMinValue * raise % 80 != 0 ? tmpMinValue - (80 + tmpMinValue * raise % 80) / raise : tmpMinValue;
												newMaxValue = tmpMaxValue * raise % 80 != 0 ? tmpMaxValue + (80 - tmpMaxValue * raise % 80) / raise : tmpMaxValue;
												newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 80 + 0.5);
												newNumberMiniTicks = 5;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		else if (isMinNegative && isMaxNegative) {
			newMinValue = tmpMinValue * raise % 2 != 0 ? tmpMinValue - (tmpMinValue * raise % 2) / raise : tmpMinValue;
			newMaxValue = tmpMaxValue * raise % 2 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 2) / raise : tmpMaxValue;
			newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 2 + 0.5);
			newNumberMiniTicks = 4;
			if (newNumberTicks > maxNumberTicks) {
				newMinValue = tmpMinValue * raise % 4 != 0 ? tmpMinValue - (tmpMinValue * raise % 4) / raise : tmpMinValue;
				newMaxValue = tmpMaxValue * raise % 4 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 4) / raise : tmpMaxValue;
				newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 4 + 0.5);
				newNumberMiniTicks = 4;
				if (newNumberTicks > maxNumberTicks) {
					newMinValue = tmpMinValue * raise % 5 != 0 ? tmpMinValue - (tmpMinValue * raise % 5) / raise : tmpMinValue;
					newMaxValue = tmpMaxValue * raise % 5 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 5) / raise : tmpMaxValue;
					newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 5 + 0.5);
					newNumberMiniTicks = 5;
					if (newNumberTicks > maxNumberTicks) {
						newMinValue = tmpMinValue * raise % 8 != 0 ? tmpMinValue - (tmpMinValue * raise % 8) / raise : tmpMinValue;
						newMaxValue = tmpMaxValue * raise % 8 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 8) / raise : tmpMaxValue;
						newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 8 + 0.5);
						newNumberMiniTicks = 4;
						if (newNumberTicks > maxNumberTicks) {
							newMinValue = tmpMinValue * raise % 10 != 0 ? tmpMinValue - (tmpMinValue * raise % 10) / raise : tmpMinValue;
							newMaxValue = tmpMaxValue * raise % 10 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 10) / raise : tmpMaxValue;
							newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 10 + 0.5);
							newNumberMiniTicks = 5;
							if (newNumberTicks > maxNumberTicks) {
								newMinValue = tmpMinValue * raise % 16 != 0 ? tmpMinValue - (tmpMinValue * raise % 16) / raise : tmpMinValue;
								newMaxValue = tmpMaxValue * raise % 16 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 16) / raise : tmpMaxValue;
								newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 16 + 0.5);
								newNumberMiniTicks = 4;
								if (newNumberTicks > maxNumberTicks) {
									newMinValue = tmpMinValue * raise % 20 != 0 ? tmpMinValue - (tmpMinValue * raise % 20) / raise : tmpMinValue;
									newMaxValue = tmpMaxValue * raise % 20 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 20) / raise : tmpMaxValue;
									newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 20 + 0.5);
									newNumberMiniTicks = 5;
									if (newNumberTicks > maxNumberTicks) {
										newMinValue = tmpMinValue * raise % 32 != 0 ? tmpMinValue - (tmpMinValue * raise % 32) / raise : tmpMinValue;
										newMaxValue = tmpMaxValue * raise % 32 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 32) / raise : tmpMaxValue;
										newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 32 + 0.5);
										newNumberMiniTicks = 4;
										if (newNumberTicks > maxNumberTicks) {
											newMinValue = tmpMinValue * raise % 40 != 0 ? tmpMinValue - (tmpMinValue * raise % 40) / raise : tmpMinValue;
											newMaxValue = tmpMaxValue * raise % 40 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 40) / raise : tmpMaxValue;
											newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 40 + 0.5);
											newNumberMiniTicks = 5;
											if (newNumberTicks > maxNumberTicks) {
												newMinValue = tmpMinValue * raise % 80 != 0 ? tmpMinValue - (tmpMinValue * raise % 80) / raise : tmpMinValue;
												newMaxValue = tmpMaxValue * raise % 80 != 0 ? tmpMaxValue + (tmpMaxValue * raise % 80) / raise : tmpMaxValue;
												newNumberTicks = (int) ((newMaxValue - newMinValue) * raise / 80 + 0.5);
												newNumberMiniTicks = 5;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		return new Object[] {Double.parseDouble(String.format(Locale.ENGLISH, "%.3f", newMinValue)), Double.parseDouble(String.format(Locale.ENGLISH, "%.3f", newMaxValue)), newNumberTicks, newNumberMiniTicks, new Object()};
	}
}
