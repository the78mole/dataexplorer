/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.elprog;

import java.util.logging.Logger;

import gde.device.CheckSumTypes;
import gde.device.FormatTypes;
import gde.exception.DevicePropertiesInconsistenceException;
import gde.io.DataParser;

/**
 * @author brueg
 *
 */
public class PulsarDataParser extends DataParser {
	static Logger			log										= Logger.getLogger(DataParser.class.getName());

	protected final int offset;
	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useDataSize
	 */
	public PulsarDataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, int useDataSize, int offset) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useDataSize);
		this.offset = offset;
	}

	/**
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType
	 * @param useCheckSumFormatType
	 * @param useDataSize
	 * @param useDataFormatType
	 * @param doMultiply1000
	 */
	public PulsarDataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType,
			boolean doMultiply1000, int offset) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useCheckSumFormatType, useDataSize, useDataFormatType, doMultiply1000);
		this.offset = offset;
	}
	/**
	 * constructor to initialize required configuration parameter
	 * @param useTimeFactor
	 * @param useLeaderChar
	 * @param useSeparator
	 * @param useCheckSumType 
	 * @param useCheckSumFormatType if null, no checksum calculation will occur
	 * @param useDataSize
	 * @param useDataFormatType FormatTypes.TEXT where dataBlockSize specifies the number of contained values, FormatTypes.BINARY where dataBlockSize specifies the number of bytes received via seral connection
	 * @param doMultiply1000 some transferred values are already in a format required to enable 3 digits 
	 */
	public PulsarDataParser(int useTimeFactor, String useLeaderChar, String useSeparator, CheckSumTypes useCheckSumType, FormatTypes useCheckSumFormatType, int useDataSize, FormatTypes useDataFormatType, boolean doMultiply1000) {
		super(useTimeFactor, useLeaderChar, useSeparator, useCheckSumType, useCheckSumFormatType, useDataSize, useDataFormatType, doMultiply1000);
		this.offset = 2;
	}


	/**
	 * default parse method for #03D11____B2,00000,11302,00000,000,00000,00000,3772,0,3768,0,3770,0,0000,0,0000,0,0000,0,0000,0,0000,0,0000,0 like lines
	 * @param inputLine
	 * @param strValues
	 * @throws DevicePropertiesInconsistenceException
	 */
	@Override
	public void parse(String inputLine, int lineNum) throws DevicePropertiesInconsistenceException {
		String[] strValues = inputLine.split(this.separator);
		String strValue = strValues[0].trim().substring(1);
		this.channelConfigNumber = 1; 
		this.state = Integer.parseInt(strValue.substring(strValue.length()-1));
		this.valueSize = this.dataBlockSize;
		this.values = new int[this.valueSize];
		int[] tmpValues = new int[this.valueSize];


//		//run time
//		strValue = strValues[1].trim();
//		strValue = strValue.length() > 0 ? strValue : "0";
		

		for (int i = 0, j = 0; i < this.valueSize-3; i++,j++) {
			strValue = strValues[j+this.offset].trim();
			try {
				tmpValues[i] = strValue.length() > 0 ? Integer.parseInt(strValue) : 0;
				if (i >= 5 ) j++;
			}
			catch (NumberFormatException e) {
				tmpValues[i] = 0;
			}
		}
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;
		
		//0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie
		values[0] = tmpValues[0];
		values[1] = tmpValues[1];
		values[2] = tmpValues[3] * 1000;			
		values[3] = Double.valueOf((values[0] / 1000.0) * (values[1] / 1000.0) * 10000).intValue();							// power U*I [W]
		values[4] = Double.valueOf((values[0] / 1000.0) * (values[2] / 1000.0)).intValue();											// energy U*C [mWh]
		//5=Temperature 6=Ri
		values[5] = tmpValues[2];
		values[6] = tmpValues[4];
		//7=Balance
		//values[7] = 0;
		//8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6 ... 23=SpannungZelle16
		for (int i = 0; i < 16; i++) {
			values[i+8] = tmpValues[i+5];
			if (values[i + 8] > 0) {
				maxVotage = values[i + 8] > maxVotage ? values[i + 8] : maxVotage;
				minVotage = values[i + 8] < minVotage ? values[i + 8] : minVotage;
			}
		}
		//7=Balance
		values[7] = (maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0) * 1000;

	}

}