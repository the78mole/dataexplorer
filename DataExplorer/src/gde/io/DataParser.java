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
package osde.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import osde.device.CheckSumTypes;
import osde.exception.DataInconsitsentException;
import osde.utils.Checksum;

/**
 * Class to parse comma separated input line from a comma separated textual line which simulates serial data 
 * one data line consist of $1;1;0; 14780;  598;  1000;  8838;.....;0002;
 * where $recordSetNumber; stateNumber; timeStepSeconds; firstIntValue; secondIntValue; .....;checkSumIntValue;
 * All properties around the textual data in this line has to be specified in DataBlockType (type=TEXT, size number of values, separator=;, ...), refer to DeviceProperties_XY.XSD
 * @author Winfried Brügmann
 */
public class DataParser {
	static Logger					log			= Logger.getLogger(DataParser.class.getName());

	int									recordNumber;
	int									state;
	long								time;
	int[]								values;
	int									checkSum;

	final String				separator;
	final CheckSumTypes	checkSumType;
	final int						size;

	public DataParser(String useSeparator, CheckSumTypes useCheckSumType, int useDataSize) {
		this.separator = useSeparator;
		this.checkSumType = useCheckSumType;
		this.size = useDataSize;
	}

	public void parse(String inputLine) throws DataInconsitsentException, NumberFormatException {
		try {
			this.values = new int[this.size];
			String[] strValues = inputLine.split(this.separator); // {$1, 1, 0, 14780, 0,598, 1,000, 8,838, 22}
			log.log(Level.FINER, "parser inputLine = " + inputLine);
			String strValue = strValues[0].trim().substring(1);
			this.recordNumber = Integer.valueOf(strValue);
			
			strValue = strValues[1].trim();
			this.state = Integer.valueOf(strValue);

			strValue = strValues[2].trim();
			time = Long.valueOf(strValue) * 1000; // Seconds * 1000 = msec
			
			for (int i = 0; i < this.size; i++) { 
				strValue = strValues[i+3].trim();
				try {
					long tmpValue = Integer.valueOf(strValue);
					if (tmpValue > Integer.MAX_VALUE/1000 || tmpValue < Integer.MIN_VALUE/1000)
						this.values[i] = Integer.valueOf(strValue);
					else
						this.values[i] = Integer.valueOf(strValue)*1000;
				}
				catch (NumberFormatException e) {
					this.values[i] = 0;
				}
			}

			strValue = strValues[this.values.length].trim();
			int checkSum = Integer.valueOf(strValue);
			boolean isValid = true;
			if (checkSumType != null) {
				switch (checkSumType) {
				case ADD:
					isValid = checkSum == Checksum.ADD(this.values, 0, this.size);
					break;
				case XOR:
					isValid = checkSum == Checksum.XOR(this.values, 0, this.size);
					break;
				case OR:
					isValid = checkSum == Checksum.OR(this.values, 0, this.size);
					break;
				case AND:
					isValid = checkSum == Checksum.AND(this.values, 0, this.size);
					break;
				}
			}
			if (!isValid) {
				DataInconsitsentException e = new DataInconsitsentException("Checksum error: actual value = " + strValue);
				log.log(Level.WARNING, e.getMessage(), e);
				throw e;
			}
		}
		catch (NumberFormatException e) {
			log.log(Level.WARNING, e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * @return the recordNumber
	 */
	public int getRecordNumber() {
		return recordNumber;
	}

	/**
	 * @return the state
	 */
	public int getState() {
		return state;
	}

	/**
	 * @return the time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * @return the values
	 */
	public int[] getValues() {
		return values;
	}
}
