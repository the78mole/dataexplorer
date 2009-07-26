/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class contains mathematics utility methods
 * @author Winfried Brügmann
 */
public class MathUtils {
	private static Logger log = Logger.getLogger(MathUtils.class.getName());
	
	/**
	 * round up given value according value level 
	 * round up for a positive value results in a higher value
	 * round up for a negative value results in a lower value
	 * @param value
	 * @return
	 */
	public static double roundUp(double value) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (value < 0.25)
					roundValue = value + (0.05 - value % 0.05);
//				else if (value < 0.5)
//					roundValue = value + (0.01 - value % 0.01);
//				else if (value < 1.0)
//					roundValue = value + (0.05 - value % 0.05);
				else if (value < 2.5)
					roundValue = value + (0.1 - value % 0.1);
				else if (value < 5)
					roundValue = value + (0.25 - value % 0.25);
				else if (value < 10)
					roundValue = value + (0.5 - value % 0.5);
				else if (value < 25)
					roundValue = (int) (value + 1);
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
					roundValue = value - (0.05 + value % 0.05);
//				else if (value > -0.5)
//					roundValue = value - (0.01 + value % 0.01);
//				else if (value > -1)
//					roundValue = value - (0.05 + value % 0.05);
				else if (value > -2.5)
					roundValue = value - (0.1 + value % 0.1);
				else if (value > -5)
					roundValue = value - (0.25 + value % 0.25);
				else if (value > -10)
					roundValue = value - (0.5 + value % 0.5);
				else if (value > -25)
					roundValue = (int) (value - 1);
				else if (value > -50)
					roundValue = value - (2.5 + (value % 2.5));
				else if (value > -100)
					roundValue = value - (5 + (value % 5));
				else if (value > -500)
					roundValue = value - (10 + (value % 10));
				else if (value > -1000)
					roundValue = value - (50 + (value % 50));
				else
					roundValue = value - (100 + (value % 100));
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
			roundValue = value <= 0.1 ? value - value*100%5 : value <= 1 ? value - value*10%5 : value <= 10 ? value - value %1 : value <= 50 ? value - value %10 : value <= 500 ? value - value %50 : value - value % 100 ;
		else
			roundValue = value >= -0.1 ? value - value*100%5 : value >= -1 ? value - value*10%5 : value >= -10 ? value - value %1 : value >= -50 ? value - value %10 : value >= -500 ? value - value %50 : value - value % 100 ;
		return roundValue;
	}
	
	/**
	 * round down given value according value level 
	 * round down for a positive value results in a lower value
	 * round down for a negative value results in a higher value
	 * @param value
	 * @return
	 */
	public static double roundDown(double value) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (value < 0.25)
					roundValue = value - (value % 0.05);
//				else if (value < 0.5)
//					roundValue = value - (value % 0.025);
//				else if (value < 1)
//					roundValue = value - (value % 0.05);
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
					roundValue = value - (value % 0.05);
//				else if (value > -0.5)
//					roundValue = value - (0.005 + value % 0.025);
//				else if (value > -1)
//					roundValue = value - (0.025 + value % 0.05);
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
	 * round up given value according value delta value level 
	 * round up for a positive value results in a higher value
	 * round up for a negative value results in a lower value
	 * @param value
	 * @param delta
	 * @return
	 */
	public static double roundUp(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.25)
					roundValue = value + (0.05 - value % 0.05);
//				else if (delta < 0.5)
//					roundValue = value + (0.025 - value % 0.025);
//				else if (delta < 1.0)
//					roundValue = value + (0.05 - value % 0.05);
				else if (delta < 2.5)
					roundValue = value + (0.1 - value % 0.1);
				else if (delta < 5)
					roundValue = value + (0.25 - value % 0.25);
				else if (delta < 10)
					roundValue = value + (0.5 - value % 0.5);
				else if (delta < 25)
					roundValue = (int) (value + 1);
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
				if (delta < 0.25)
					roundValue = value - (0.05 + value % 0.05);
//				else if (delta < 0.5)
//					roundValue = value - (0.025 + value % 0.025);
//				else if (delta < 1)
//					roundValue = value - (0.05 + value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (0.1 + value % 0.1);
				else if (delta < 5)
					roundValue = value - (0.25 + value % 0.25);
				else if (delta < 10)
					roundValue = value - (0.5 + value % 0.5);
				else if (delta < 25)
					roundValue = (int) (value - 1);
				else if (delta < 50)
					roundValue = value - (2.5 + (value % 2.5));
				else if (delta < 100)
					roundValue = value - (5 + (value % 5));
				else if (delta < 500)
					roundValue = value - (10 + (value % 10));
				else if (delta < 1000)
					roundValue = value - (50 + (value % 50));
				else
					roundValue = value - (100 + (value % 100));
			}
		}
		return roundValue;
	}
	
	/**
	 * round down given value according value delta value level 
	 * round down for a positive value results in a lower value
	 * round down for a negative value results in a higher value
	 * @param value
	 * @param delta
	 * @return
	 */
	public static double roundDown(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.25)
					roundValue = value - (value % 0.05);
//				else if (delta < 0.5)
//					roundValue = value - (value % 0.025);
//				else if (delta < 1.0)
//					roundValue = value - (value % 0.05);
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
				if (delta < 0.25)
					roundValue = value - (0.025 + value % 0.05);
//				else if (delta < 0.5)
//					roundValue = value - (0.0125 + value % 0.025);
//				else if (delta < 1.0)
//					roundValue = value - (0.025 + value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (0.05 + value % 0.1);
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
		return roundValue;
	}
	
	/**
	 * round up given value according value delta value level for auto scale
	 * round up for a positive value results in a higher value
	 * round up for a negative value results in a lower value
	 * @param value
	 * @param delta
	 * @return
	 */
	public static double roundUpAuto(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.05)
					roundValue = value + (0.001 - value % 0.001);
				else if (delta < 0.25)
					roundValue = value + (0.01 - value % 0.01);
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
					roundValue = (int) (value + 1);
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
					roundValue = value - (0.001 + value % 0.001);
				else if (delta < 0.25)
					roundValue = value - (0.01 + value % 0.01);
				else if (delta < 0.5)
					roundValue = value - (0.025 + value % 0.025);
				else if (delta < 1)
					roundValue = value - (0.05 + value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (0.1 + value % 0.1);
				else if (delta < 5)
					roundValue = value - (0.25 + value % 0.25);
				else if (delta < 10)
					roundValue = value - (0.5 + value % 0.5);
				else if (delta < 25)
					roundValue = (int) (value - 1);
				else if (delta < 50)
					roundValue = value - (2.5 + (value % 2.5));
				else if (delta < 100)
					roundValue = value - (5 + (value % 5));
				else if (delta < 500)
					roundValue = value - (10 + (value % 10));
				else if (delta < 1000)
					roundValue = value - (50 + (value % 50));
				else
					roundValue = value - (100 + (value % 100));
			}
		}
		return roundValue;
	}
	
	/**
	 * round down given value according value delta value level for auto scale
	 * round down for a positive value results in a lower value
	 * round down for a negative value results in a higher value
	 * @param value
	 * @param delta
	 * @return
	 */
	public static double roundDownAuto(double value, double delta) {
		double roundValue = checkRoundReq(value);

		if (value != roundValue) {
			if (value > 0) {
				if (delta < 0.05)
					roundValue = value - (value % 0.001);
				else if (delta < 0.25)
					roundValue = value - (value % 0.01);
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
					roundValue = value - (0.00125 + value % 0.001);
				else if (delta < 0.25)
					roundValue = value - (0.005 + value % 0.01);
				else if (delta < 0.5)
					roundValue = value - (0.0125 + value % 0.025);
				else if (delta < 1.0)
					roundValue = value - (0.025 + value % 0.05);
				else if (delta < 2.5)
					roundValue = value - (0.05 + value % 0.1);
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
		return roundValue;
	}


	/**
	 * adapted rounding  
	 * - a small number needs different rounding compared to a big number 0.05 -> 0.1, 529 -> 550
	 * - a small value delta needs different rounding compared to a big delta 10 -> +-1, 200 +-10 
	 * @param minValue
	 * @param maxValue 
	 * @return double array roundMinValue, roundMaxValue 
	 */
	public static double[] round(double minValue, double maxValue) {
		double[] outValues = { 0.0, 0.0 };

		if (minValue != 0) {
			if (minValue < 0) {
				if (minValue > -1)
					outValues[0] = minValue - (0.1 + (minValue - 0.1) % 0.1);
				else if (minValue > -2.5)
					outValues[0] = minValue - (0.25 + (minValue - 0.25) % 0.25);
				else if (minValue > -5)
					outValues[0] = minValue - (0.5 + (minValue - 0.5) % 0.5);
				else if (minValue > -10)
					outValues[0] = (int) (minValue - 1);
				else if (minValue < -50)
					outValues[0] = minValue - (10 + (minValue % 10));
				else
					outValues[0] = minValue - (5 + (minValue % 5));
			}
			else {// minValue > 0 
				if (minValue <= 1.0)
					outValues[0] = minValue - (0.1 + (minValue - 0.1) % 0.1);
				else if (minValue <= 2.5)
					outValues[0] = minValue - (0.25 + (minValue - 0.25) % 0.25);
				else if (minValue <= 5)
					outValues[0] = minValue - (0.5 + (minValue - 0.5) % 0.5);
				else if (minValue <= 10)
					outValues[0] = (int) (minValue - 1);
				else if (minValue <= 50)
					outValues[0] = minValue - (minValue % 10);
				else
					outValues[0] = minValue - (minValue % 5);
			}
		}

		if (maxValue != 0) {
			if (maxValue < 0) {
				if (maxValue > -1)
					outValues[1] = maxValue + (0.1 - (maxValue - 0.1) % 0.1);
				else if (maxValue > -2.5)
					outValues[1] = maxValue + (0.25 - (maxValue - 0.25) % 0.25);
				else if (maxValue > -5)
					outValues[1] = maxValue + (0.5 - (maxValue - 0.5) % 0.5);
				else if (maxValue > -10)
					outValues[1] = (int) (maxValue + 1);
				else if (maxValue > -50)
					outValues[1] = maxValue + 5 - (maxValue % 5);
				else
					outValues[1] = maxValue + 10 - (maxValue % 10);
			}
			else {
				if (maxValue <= 1)
					outValues[1] = maxValue + (0.1 - (maxValue + 0.1) % 0.1);
				else if (maxValue <= 2.5)
					outValues[1] = maxValue + (0.25 - (maxValue + 0.25) % 0.25);
				else if (maxValue <= 5)
					outValues[1] = maxValue + (0.5 - (maxValue + 0.5) % 0.5);
				else if (maxValue <= 10)
					outValues[1] = (int) (maxValue + 1);
				else if (maxValue > 50)
					outValues[1] = maxValue + 10 - (maxValue % 10);
				else
					outValues[1] = maxValue + 5 - (maxValue % 5);
			}
		}
		// check delta scale enable easy readable tick marks
		double deltaScale = outValues[1] - outValues[0];
		if (deltaScale <= 1) {
			//numberTicks = new Double(deltaScale * 20 / 1).intValue();
			outValues[0] = outValues[0] - (outValues[0] % 0.1 == 0 ? 0 : (0.1 + outValues[0] % 0.1));
			outValues[1] = outValues[1] + (outValues[1] % 0.1 == 0 ? 0 : (0.1 - outValues[1] % 0.1));
		}
		else if (deltaScale <= 2) {
			//numberTicks = (int)(deltaScale) * 10 / 1;
			outValues[0] = outValues[0] - (outValues[0] % 0.25 == 0 ? 0 : (0.25 + outValues[0] % 0.25));
			outValues[1] = outValues[1] + (outValues[1] % 0.25 == 0 ? 0 : (0.25 - outValues[1] % 0.25));
		}
		else if (deltaScale <= 5) {
			//numberTicks = (int)(deltaScale) * 5 / 1;
			outValues[0] = outValues[0] - (outValues[0] % 0.5 == 0 ? 0 : (0.5 + outValues[0] % 0.5));
			outValues[1] = outValues[1] + (outValues[1] % 0.5 == 0 ? 0 : (0.5 - outValues[1] % 0.5));
		}
		else if (deltaScale <= 10) {
			//numberTicks = (int)deltaScale;
			outValues[0] = outValues[0] - (outValues[0] % 1 == 0 ? 0 : (1 + outValues[0] % 1));
			outValues[1] = outValues[1] + (outValues[1] % 1 == 0 ? 0 : (1 - outValues[1] % 1));
		}
		else if (deltaScale <= 25) {
			//numberTicks = (int)deltaScale;
			outValues[0] = outValues[0] - (outValues[0] % 1.5 == 0 ? 0 : (1.5 + outValues[0] % 1.5));
			outValues[1] = outValues[1] + (outValues[1] % 1.5 == 0 ? 0 : (1.5 - outValues[1] % 1.5));
		}
		else if (deltaScale <= 50) {
			//numberTicks = (int)deltaScale;
			outValues[0] = outValues[0] - (outValues[0] % 2.5 == 0 ? 0 : (2.5 + outValues[0] % 2.5));
			outValues[1] = outValues[1] + (outValues[1] % 2.5 == 0 ? 0 : (2.5 - outValues[1] % 2.5));
		}
		else if (deltaScale <= 100) {
			//numberTicks = (int)(deltaScale / 5);
			outValues[0] = outValues[0] - (outValues[0] % 5 == 0 ? 0 : (5 + outValues[0] % 5));
			outValues[1] = outValues[1] + (outValues[1] % 5 == 0 ? 0 : (5 - outValues[1] % 5));
		}
		else if (deltaScale <= 300) {
			//numberTicks = (int)(deltaScale / 20);
			outValues[0] = outValues[0] - (outValues[0] % 10 == 0 ? 0 : (10 + outValues[0] % 10));
			outValues[1] = outValues[1] + (outValues[1] % 10 == 0 ? 0 : (10 - outValues[1] % 10));
		}
		else { // > 300
			outValues[0] = outValues[0] - (outValues[0] % 20 == 0 ? 0 : (20 + outValues[0] % 20));
			outValues[1] = outValues[1] + (outValues[1] % 20 == 0 ? 0 : (20 - outValues[1] % 20));
		}

		// enable scale value 0.0  -- algorithm must fit scale tick mark calculation DrawUtile.drawVerticalTickMarks
		if (minValue < 0 && maxValue > 0) {
			if (deltaScale <= 1) {
				//numberTicks = new Double(deltaScale * 20 / 1).intValue();
				outValues[0] = outValues[0] - (outValues[0] % 0.1 == 0 ? 0 : (0.1 + outValues[0] % 0.1));
				outValues[1] = outValues[1] + (outValues[1] % 0.1 == 0 ? 0 : (0.1 - outValues[1] % 0.1));
			}
			else if (deltaScale <= 2) {
				//numberTicks = (int)(deltaScale) * 10 / 1;
				outValues[0] = outValues[0] - (outValues[0] % 0.25 == 0 ? 0 : (0.25 + outValues[0] % 0.25));
				outValues[1] = outValues[1] + (outValues[1] % 0.25 == 0 ? 0 : (0.25 - outValues[1] % 0.25));
			}
			else if (deltaScale <= 5) {
				//numberTicks = (int)(deltaScale) * 5 / 1;
				outValues[0] = outValues[0] - (outValues[0] % 0.5 == 0 ? 0 : (0.5 + outValues[0] % 0.5));
				outValues[1] = outValues[1] + (outValues[1] % 0.5 == 0 ? 0 : (0.5 - outValues[1] % 0.5));
			}
			else if (deltaScale <= 10) {
				//numberTicks = (int)deltaScale;
				outValues[0] = outValues[0] - (outValues[0] % 1 == 0 ? 0 : (1 + outValues[0] % 1));
				outValues[1] = outValues[1] + (outValues[1] % 1 == 0 ? 0 : (1 - outValues[1] % 1));
			}
			else if (deltaScale <= 25) {
				//numberTicks = (int)deltaScale;
				outValues[0] = outValues[0] - (outValues[0] % 1.5 == 0 ? 0 : (1.5 + outValues[0] % 1.5));
				outValues[1] = outValues[1] + (outValues[1] % 1.5 == 0 ? 0 : (1.5 - outValues[1] % 1.5));
			}
			else if (deltaScale <= 50) {
				//numberTicks = (int)deltaScale;
				outValues[0] = outValues[0] - (outValues[0] % 2.5 == 0 ? 0 : (2.5 + outValues[0] % 2.5));
				outValues[1] = outValues[1] + (outValues[1] % 2.5 == 0 ? 0 : (2.5 - outValues[1] % 2.5));
			}
			else if (deltaScale <= 100) {
				//numberTicks = (int)(deltaScale / 5);
				outValues[0] = outValues[0] - (outValues[0] % 5 == 0 ? 0 : (5 + outValues[0] % 5));
				outValues[1] = outValues[1] + (outValues[1] % 5 == 0 ? 0 : (5 - outValues[1] % 5));
			}
			else if (deltaScale <= 300) {
				//numberTicks = (int)(deltaScale / 20);
				outValues[0] = outValues[0] - (outValues[0] % 10 == 0 ? 0 : (10 + outValues[0] % 10));
				outValues[1] = outValues[1] + (outValues[1] % 10 == 0 ? 0 : (10 - outValues[1] % 10));
			}
			else { // > 300
				outValues[0] = outValues[0] - (outValues[0] % 20 == 0 ? 0 : (20 + outValues[0] % 20));
				outValues[1] = outValues[1] + (outValues[1] % 20 == 0 ? 0 : (20 - outValues[1] % 20));
			}
			log.log(Level.FINER, "reminder = " +  (outValues[1] - outValues[0]) % 20);
		}

		log.log(Level.INFO, minValue + " --> " + outValues[0] + " " + maxValue + " --> " + outValues[1]); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return outValues;
	}

}
