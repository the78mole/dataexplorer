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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import osde.OSDE;

/**
 * @author Winfried Brügmann
 * class with collection of string helper finctions
 */
public class StringHelper {
	final static Logger	log	= Logger.getLogger(StringHelper.class.getName());

	/**
	 * split the given string at delimiter to hash map where the keys are used for hashes
	 * @param line
	 * @param delimiter
	 * @param hashKeys
	 */
	public static HashMap<String,String> splitString(String line, String delimiter, String[] hashKeys) {
		HashMap<String,String> resultMap = new HashMap<String, String>();
		if (line != null && line.length() > 5) {
			String tmpStr = line;
			int endindex = 0;
			while ((endindex = tmpStr.indexOf(delimiter)) != -1) {
				for (String key : hashKeys) {
					if (tmpStr.startsWith(key)) {
						String value = tmpStr.substring(key.length(), endindex).trim();
						if (value.startsWith(OSDE.STRING_EQUAL)) value = value.substring(1).trim();
						resultMap.put(key, value);
						break;
					}
				}
				tmpStr = tmpStr.substring(endindex + delimiter.length(), tmpStr.length());
			}
			for (String key : hashKeys) {
				if (tmpStr.startsWith(key)) {
					String value = tmpStr.substring(key.length()).trim();
					if (value.startsWith(OSDE.STRING_EQUAL)) value = value.substring(1).trim();
					resultMap.put(key, value);
					break;
				}
			}
			if (log.isLoggable(Level.FINER)) {
				for (String key : hashKeys) {
					log.finer(key + " = " + resultMap.get(key)); //$NON-NLS-1$
				}
			}
		}
		return resultMap;
	}

	/**
	 * split a string at delimiter into string array, if the array element contains stripString at begin/end, it will be removed
	 * @param line
	 * @param delimiter
	 * @return string array with split string 
	 */
	public static String[] splitString(String line, String delimiter, String stripString) {
		Vector<String> result = new Vector<String>();
		if (line != null && line.length() > 5) {
			String tmpStr = line.endsWith(delimiter) ? line.substring(0, line.lastIndexOf(delimiter)) : line;
			int endindex = 0;
			while ((endindex = tmpStr.indexOf(delimiter)) != -1) {
				String tmp = tmpStr.substring(0, endindex);
				if (tmp.startsWith(stripString)) {
					tmp = tmp.substring(stripString.length());
				}
				else if (tmp.endsWith(stripString)) {
					tmp = tmp.substring(0, tmp.indexOf(stripString));
				}
				result.add(tmp);
				tmpStr = tmpStr.substring(endindex + delimiter.length(), tmpStr.length());
			}
			if (tmpStr.length() > 0) {
				if (tmpStr.startsWith(stripString))
					tmpStr = tmpStr.substring(stripString.length());
				else if (tmpStr.endsWith(stripString)) tmpStr = tmpStr.substring(0, tmpStr.indexOf(stripString));
				result.add(tmpStr);
			}
			if (log.isLoggable(Level.FINER)) {
				for (String string : result) {
					log.finer(stripString + " = " + string); //$NON-NLS-1$
				}
			}
		}
		return result.toArray(new String[0]);
	}
	
	/**
	 * method to receive formated data and time
	 */
	public static String getDateAndTime() {
		return  new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date().getTime()); //$NON-NLS-1$
	}
	
	/**
	 * method to get current date
	 */
	public static String getDate() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date()); //$NON-NLS-1$
	}

	/**
	 * method to get formated time by given format string and time in millis seconds
	 */
	public static String getFormatedTime(String format, long millisec) {
		return new SimpleDateFormat(format).format(millisec);
	}

	/**
	 * remove blanks within a string
	 * @param inputString
	 * @return cleaned string
	 */
	public static String removeBlanks(String inputString) {
		String[] tmpDev = inputString.split(OSDE.STRING_BLANK);
		StringBuilder sb = new StringBuilder();
		for (String tmp : tmpDev) {
			sb.append(tmp);
		}
		return sb.toString();
	}
}
