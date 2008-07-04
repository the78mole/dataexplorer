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
package osde.messages;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
	private static final String					BUNDLE_NAME			= "osde.messages.messages";								//$NON-NLS-1$

	private static final ResourceBundle	RESOURCE_BUNDLE	= ResourceBundle.getBundle(BUNDLE_NAME); //, Locale.GERMANY);

	private Messages() {
	}

//	/**
//	 * testing message class
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		System.out.println(Messages.getString(MessageIds.OSDE_MSGI0001));
//		System.out.println(Messages.getString(MessageIds.OSDE_MSGI0002, new String[] {"UniLog"}));
//		System.out.println(Messages.getString(MessageIds.OSDE_MSGI0003, new Object[] {"UniLog", 5}));
//		System.out.println(Messages.getString(MessageIds.OSDE_MSGI0003, new Object[] {"UniLog"}));
//	}
	
	/**
	 * example usage: application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSG001, new Object{"hallo", "world"));
	 * @param key
	 * @param params as object array
	 * @return the message as string with unlined parameters
	 */
	public static String getString(String key, Object[] params) {
		try {
			String result = RESOURCE_BUNDLE.getString(key);
			String[] array = result.split("[{}]");
			StringBuilder sb = new StringBuilder();
			if (array.length > 1) {
				for (int i = 0, j = 0; i < array.length; i++) {
					if (i != 0 && i % 2 != 0)
						sb.append(params.length >= (j + 1) ? params[j++] : "?");
					else
						sb.append(array[i]);
				}
				result = sb.toString();
			}
			
			return result;
		}
		catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	/**
	 * example usage: application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSG001));
	 * @param key
	 * @return the string matching the given key
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		}
		catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

}
