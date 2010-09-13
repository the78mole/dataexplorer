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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.io;

import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.logging.Logger;

import gde.device.CheckSumTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.utils.Checksum;

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
	int									time_ms;
	int[]								values;
	int									checkSum;

	final int						timeFactor;
	final String				separator;
	final String				leader;
	final CheckSumTypes	checkSumType;
	final int						size;

	public DataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize) {
		this.timeFactor = useTimeFactor;
		this.separator = useSeparator;
		this.leader = useLeaderChar;
		this.checkSumType = useCheckSumType;
		this.size = useDataSize;
	}

	public void parse(String inputLine) throws DevicePropertiesInconsistenceException, NumberFormatException {
		try {
			if(!inputLine.startsWith(this.leader)) 
				throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0046, new String[] {this.leader}));
			if(!inputLine.contains(separator)) 
				throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0047, new String[] {inputLine, separator})); 
			
			this.values = new int[this.size];
			String[] strValues = inputLine.split(this.separator); // {$1, 1, 0, 14780, 0,598, 1,000, 8,838, 22}
			log.log(Level.FINER, "parser inputLine = " + inputLine); //$NON-NLS-1$
			if (strValues.length-4 != this.size)  throw new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0048, new String[] {inputLine}));
			
			String strValue = strValues[0].trim().substring(1);
			this.recordNumber = Integer.parseInt(strValue);
			
			strValue = strValues[1].trim();
			this.state = Integer.parseInt(strValue);

			strValue = strValues[2].trim();
			time_ms = Integer.parseInt(strValue) * this.timeFactor; // Seconds * 1000 = msec
			
			for (int i = 0; i < this.size; i++) { 
				strValue = strValues[i+3].trim();
				try {
					long tmpValue = strValue.length() > 0 ? Long.parseLong(strValue) : 0;
					if (tmpValue < Integer.MAX_VALUE/1000 && tmpValue > Integer.MIN_VALUE/1000)
						this.values[i] = (int) (tmpValue*1000); // enable 3 positions after decimal place
					else // needs special processing with factor, ...
						if (tmpValue < Integer.MAX_VALUE || tmpValue > Integer.MIN_VALUE) {
							this.values[i] = (int) tmpValue;
						}
						else {
							this.values[i] = (int) (tmpValue/1000);
						}
				}
				catch (NumberFormatException e) {
					this.values[i] = 0;
				}
			}

			strValue = strValues[this.values.length].trim();
			int checkSum = Integer.parseInt(strValue);
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
				DevicePropertiesInconsistenceException e = new DevicePropertiesInconsistenceException(Messages.getString(MessageIds.GDE_MSGE0049, new String[] {strValue})); 
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
	public long getTime_ms() {
		return time_ms;
	}

	/**
	 * @return the values
	 */
	public int[] getValues() {
		return values;
	}
}
