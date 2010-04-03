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
****************************************************************************************/
package osde.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;

import osde.DE;
import osde.device.DataTypes;
import osde.serial.DeviceSerialPort;

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
						if (value.startsWith(DE.STRING_EQUAL)) value = value.substring(1).trim();
						resultMap.put(key, value);
						break;
					}
				}
				tmpStr = tmpStr.substring(endindex + delimiter.length(), tmpStr.length());
			}
			for (String key : hashKeys) {
				if (tmpStr.startsWith(key)) {
					String value = tmpStr.substring(key.length()).trim();
					if (value.startsWith(DE.STRING_EQUAL)) value = value.substring(1).trim();
					resultMap.put(key, value);
					break;
				}
			}
			if (log.isLoggable(Level.FINER)) {
				for (String key : hashKeys) {
					log.log(Level.FINER, key + " = " + resultMap.get(key)); //$NON-NLS-1$
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
					log.log(Level.FINER, stripString + " = " + string); //$NON-NLS-1$
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
		String[] tmpDev = inputString.split(DE.STRING_BLANK);
		StringBuilder sb = new StringBuilder();
		for (String tmp : tmpDev) {
			sb.append(tmp);
		}
		return sb.toString();
	}
	
	public static String intArrayToString(int[] values) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; values != null && i < values.length; i++) {
			sb.append(values[i]);
			if (i != values.length - 1) sb.append(';');
		}
		return sb.toString();
	}

	public static int[] stringToIntArray(String values) {
		String[] stringValues = values.split(";"); //$NON-NLS-1$
		int[] array = new int[stringValues.length];
		for (int i = 0; i < stringValues.length && stringValues.length > 1; i++) {
			array[i] = new Integer(stringValues[i].trim()).intValue();
		}
		return array;
	}

	public static String pointArrayToString(Point[] points) {
		StringBuffer sb = new StringBuffer();

		for (int i = 0; points != null && i < points.length; i++) {
			sb.append(points[i].x).append(':').append(points[i].y);
			if (i != points.length - 1) sb.append(';');
		}
		return sb.toString();
	}

	public static Point[] stringToPointArray(String values) {
		String[] stringValues = values.split(";"); //$NON-NLS-1$
		Point[] points = new Point[stringValues.length];
		for (int i = 0; i < points.length && points.length > 1; i++) {
			String[] xy = stringValues[i].split(":"); //$NON-NLS-1$
			points[i] = new Point(new Integer(xy[0].trim()).intValue(), new Integer(xy[1].trim()).intValue());
		}
		return points;
	}
	
	/**
	 * print the SWT key code by log info
	 * @param evt
	 */
	public static void printSWTKeyCode(KeyEvent evt) {
		String keyCode = ""+evt.character;
		switch (evt.keyCode) {
		/* Keyboard and Mouse Masks */
		case SWT.ALT: 		keyCode = "ALT"; break;
		case SWT.SHIFT: 	keyCode = "SHIFT"; break;
		case SWT.CONTROL:	keyCode = "CONTROL"; break;
		case SWT.COMMAND:	keyCode = "COMMAND"; break;
			
		/* Non-Numeric Keypad Keys */
		case SWT.ARROW_UP:		keyCode = "ARROW_UP"; break;
		case SWT.ARROW_DOWN:	keyCode = "ARROW_DOWN"; break;
		case SWT.ARROW_LEFT:	keyCode = "ARROW_LEFT"; break;
		case SWT.ARROW_RIGHT:	keyCode = "ARROW_RIGHT"; break;
		case SWT.PAGE_UP:			keyCode = "PAGE_UP"; break;
		case SWT.PAGE_DOWN:		keyCode = "PAGE_DOWN"; break;
		case SWT.HOME:				keyCode = "HOME"; break;
		case SWT.END:					keyCode = "END"; break;
		case SWT.INSERT:			keyCode = "INSERT"; break;

		/* Virtual and Ascii Keys */
		case SWT.BS:	keyCode = "BS"; break;
		case SWT.CR:	keyCode = "CR"; break;		
		case SWT.DEL:	keyCode = "DEL"; break;
		case SWT.ESC:	keyCode = "ESC"; break;
		case SWT.LF:	keyCode = "LF"; break;
		case SWT.TAB:	keyCode = "TAB"; break;

		/* Functions Keys */
		case SWT.F1:	keyCode = "F1"; break;
		case SWT.F2:	keyCode = "F2"; break;
		case SWT.F3:	keyCode = "F3"; break;
		case SWT.F4:	keyCode = "F4"; break;
		case SWT.F5:	keyCode = "F5"; break;
		case SWT.F6:	keyCode = "F6"; break;
		case SWT.F7:	keyCode = "F7"; break;
		case SWT.F8:	keyCode = "F8"; break;
		case SWT.F9:	keyCode = "F9"; break;
		case SWT.F10:	keyCode = "F10"; break;
		case SWT.F11:	keyCode = "F11"; break;
		case SWT.F12:	keyCode = "F12"; break;
		case SWT.F13:	keyCode = "F13"; break;
		case SWT.F14:	keyCode = "F14"; break;
		case SWT.F15:	keyCode = "F15"; break;
		
		/* Numeric Keypad Keys */
		case SWT.KEYPAD_ADD:			keyCode = "KEYPAD_ADD"; break;
		case SWT.KEYPAD_SUBTRACT:	keyCode = "KEYPAD_SUBTRACT"; break;
		case SWT.KEYPAD_MULTIPLY:	keyCode = "KEYPAD_MULTIPLY"; break;
		case SWT.KEYPAD_DIVIDE:		keyCode = "KEYPAD_DIVIDE"; break;
		case SWT.KEYPAD_DECIMAL:	keyCode = "KEYPAD_DECIMAL"; break;
		case SWT.KEYPAD_CR:				keyCode = "KEYPAD_CR"; break;
		case SWT.KEYPAD_0:				keyCode = "KEYPAD_0"; break;
		case SWT.KEYPAD_1:				keyCode = "KEYPAD_1"; break;
		case SWT.KEYPAD_2:				keyCode = "KEYPAD_2"; break;
		case SWT.KEYPAD_3:				keyCode = "KEYPAD_3"; break;
		case SWT.KEYPAD_4:				keyCode = "KEYPAD_4"; break;
		case SWT.KEYPAD_5:				keyCode = "KEYPAD_5"; break;
		case SWT.KEYPAD_6:				keyCode = "KEYPAD_6"; break;
		case SWT.KEYPAD_7:				keyCode = "KEYPAD_7"; break;
		case SWT.KEYPAD_8:				keyCode = "KEYPAD_8"; break;
		case SWT.KEYPAD_9:				keyCode = "KEYPAD_9"; break;
		case SWT.KEYPAD_EQUAL:		keyCode = "KEYPAD_EQUAL"; break;

		/* Other keys */
		case SWT.CAPS_LOCK:			keyCode = "CAPS_LOCK"; break;
		case SWT.NUM_LOCK:			keyCode = "NUM_LOCK"; break;
		case SWT.SCROLL_LOCK:		keyCode = "SCROLL_LOCK"; break;
		case SWT.PAUSE:					keyCode = "PAUSE"; break;
		case SWT.BREAK:					keyCode = "BREAK"; break;
		case SWT.PRINT_SCREEN:	keyCode = "PRINT_SCREEN"; break;
		case SWT.HELP:					keyCode = "HELP"; break;
		default :								
		}
		log.log(Level.INFO, "keyCode = SWT." + keyCode);
	}

	/**
	 * @return available serial port list
	 */
	public static String[] prepareSerialPortList(Vector<String> availablePorts) {
		String[] serialPortList = new String[availablePorts.size()];
		String[] tmpSerialPortList = availablePorts.toArray(new String[availablePorts.size()]);
		for (int i = 0; i < tmpSerialPortList.length; i++) {
			if (DE.IS_WINDOWS) {
				try {
					int portNumber = Integer.parseInt(tmpSerialPortList[i].substring(3));
					String portDescription = DeviceSerialPort.getWindowsPorts().get(portNumber)==null ? "" : DeviceSerialPort.getWindowsPorts().get(portNumber);
					serialPortList[i] = DE.STRING_BLANK + tmpSerialPortList[i] + " - " + portDescription;
				}
				catch (Exception e) {
					serialPortList[i] = DE.STRING_BLANK + tmpSerialPortList[i];
				}
			}
			else
			serialPortList[i] = DE.STRING_BLANK + tmpSerialPortList[i];
		}
		return serialPortList;
	}

	/**
	 * verify the user input while typing port names
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static boolean verifyPortInput(String eventText) {
		char[] chars = new char[eventText.length()];
		eventText.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			log.log(Level.FINER, "\"" + chars[i] + "\"");
			if (DE.IS_WINDOWS) {
				if (!('0' <= chars[i] && chars[i] <= '9' || 'c' == chars[i] || 'C' == chars[i] || 'o' == chars[i] || 'O' == chars[i] || 'm' == chars[i] || 'M' == chars[i] || ' ' == chars[i])) {
					return false;
				}
			}
			else if (DE.IS_LINUX) {
				if (!('0' <= chars[i] && chars[i] <= '9' || '/' == chars[i] || 'd' == chars[i] || 'e' == chars[i] || 'v' == chars[i] || 't' == chars[i] || 'y' == chars[i] || ' ' == chars[i]
						|| 'U' == chars[i] || 'S' == chars[i] || 'B' == chars[i])) {
					return false;
				}
			}
			else if (DE.IS_MAC) { 
//				if (!(('0' <= chars[i] && chars[i] <= '9') || '/' == chars[i] || '.' == chars[i] || ('a' <= chars[i] && 'z' <= chars[i]) || ('A' <= chars[i] && 'Z' <= chars[i]))) {
//					return false;
//				}
			}
		}
		return true;
	}

	/**
	 * verify the user input while typing DataTypes typed input text
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static boolean verifyTypedInput(DataTypes useType, String eventText) {
		boolean doIt = true;
		switch (useType) {
		case INTEGER:
			try {
				if (eventText.equals(DE.STRING_EMPTY)) eventText = "0";
				Integer.parseInt(eventText.trim());
			}
			catch (Exception e) {
				doIt = false;
			}
			break;
		case DOUBLE:
			try {
				if (eventText.equals("-") || eventText.equals(",") || eventText.equals(".") || eventText.equals("")) 
					doIt = true;
				else
					Double.parseDouble(eventText.replace(",", ".").trim());
			}
			catch (Exception e) {
				doIt = false;
			}
			break;
		case BOOLEAN:
			try {
				Boolean.parseBoolean(eventText.trim());
			}
			catch (Exception e) {
				doIt = false;
			}
			break;
		case STRING:
		default:
			doIt = true;
			break;
		}
		return doIt;
	}

	/**
	 * verify the user input while typing DataTypes typed input text
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static String verifyTypedString(DataTypes useType, String eventText) {
		String result = eventText;
		while (result.startsWith("0")) {
			result = result.substring(1);
		}
		switch (useType) {
		case INTEGER:
			try {
				if (eventText.replace(',', '.').contains(".")) {
					result = eventText.substring(0, eventText.indexOf('.'));
				}
				Integer.parseInt(result);
			}
			catch (Exception e) {
				result = "0";
			}
			break;
		case DOUBLE:
			try {
				if (!eventText.replace(',', '.').contains(".")) {
					result = eventText + ".0";
				}
				if (result.startsWith(".")) {
					result = "0" + result;
				}
				Double.parseDouble(result);
			}
			catch (Exception e) {
				result = "1.0";
			}
			break;
		case BOOLEAN:
			if (!(result.equals("true") || result.equals("false"))) {
				result = "true";
			}
			break;
		case STRING:
		default:
			break;
		}
		return result;
	}

	/**
	 * prints a byte in 8 digits binary display
	 * @param inByte
	 * @param newLine true appends a new line at the end, false will append a blank
	 */
	public static void printBinary(byte inByte, boolean newLine) {
		for (int i = 0; i < 8; i++) {
			log.log(Level.INFO, ""+((0x80 & inByte) >> 7));
			inByte = (byte)(inByte << 1); 
		}
		if (newLine) {
			log.log(Level.INFO, DE.LINE_SEPARATOR);
		}
		else {
			log.log(Level.INFO, DE.STRING_BLANK);
		}
	}

	/**
	 * verify the user input while typing hex input '0a0d'
	 * @param eventText of the VerifyEvent test containing the char(s) to be verified
	 */
	public static boolean verifyHexAsString(String eventText) {
		char[] chars = new char[eventText.length()];
		eventText.getChars(0, chars.length, chars, 0);
		for (int i = 0; i < chars.length; i++) {
			log.log(Level.FINER, "\"" + chars[i] + "\"");
			if (!('0' <= chars[i] && chars[i] <= '9' || 'a' == chars[i] || 'A' == chars[i] || 'b' == chars[i] || 'B' == chars[i] || 'c' == chars[i] || 'C' == chars[i] 
			     || 'd' == chars[i] || 'D' == chars[i] || 'e' == chars[i] || 'E' == chars[i] || 'f' == chars[i] || 'F' == chars[i])) {
					return false;
				}
		}
		return true;
	}

	/**
	 * convert a hexadecimal input byte array into string
	 * @param byteBuffer
	 * @return string with converted characters
	 */
	public static String convertHexInput(byte[] byteBuffer) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteBuffer.length; i++) {
			sb.append(String.format("%02X", byteBuffer[i])); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	/**
	 * convert a string two char per byte input into byte array 
	 * @param twoCharsPerByte
	 * @return byte array with converted characters
	 */
	public static byte[] convert2ByteArray(String twoCharsPerByte) {
		int length = twoCharsPerByte.length()/2;
		byte[] buffer = new byte[length];
		twoCharsPerByte = twoCharsPerByte + "0";
		for (int i = 0,j = 0; i < length; i++,j+=2) {
			buffer[i] = (byte)Integer.parseInt(twoCharsPerByte.substring(j, j+2), 16);
		}
		return buffer;
	}
	
	/**
	 * build a sting array from enumeration (combo.setItems(String[]))
	 * @param enumValues
	 * @return string array of the enumeration
	 */
	public static String[] enumValues2StringArray(Object[] enumValues) {
		Vector<String> tmpVec = new Vector<String>();
		for (Object element : enumValues) {
			tmpVec.add(element.toString());
		}
		return tmpVec.toArray(new String[0]);
	}

}
