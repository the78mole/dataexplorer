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

    Copyright (c) 2012,2013,2014,2015,2016,2017 Winfried Bruegmann
****************************************************************************************/
package gde.device.junsi;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import gde.GDE;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.io.DataParser;
import gde.log.Level;

/**
 * Junsi iCharger 34010DUO device class
 * @author Winfried Brügmann
 */
public class iCharger4010DUO extends iChargerUsb {

	/**
	 * constructor using properties file
	 * @throws JAXBException
	 * @throws FileNotFoundException
	 */
	public iCharger4010DUO(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public iCharger4010DUO(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		int maxVotage = Integer.MIN_VALUE;
		int minVotage = Integer.MAX_VALUE;

		//2C 10 01 F8 7D 07 00 02 01 00 E5 FF 04 87 2B 3C CF FF FF FF 64 01 00 00 02 0F 0B 0F 0E 0F 00 0F 00 00 00 00 00 00 00 00 00 00 00
	  //                                    34564 15403             35,6        3842  3851  3854  3840
		//0=Strom 1=VersorgungsSpg. 2=Spannung
		points[0] = DataParser.parse2Short(dataBuffer, 10);
		points[1] = DataParser.parse2UnsignedShort(dataBuffer, 12);
		points[2] = DataParser.parse2Short(dataBuffer, 14);
		//3=Ladung 4=Leistung 5=Energie
		points[3] = DataParser.parse2Int(dataBuffer, 16);
		points[4] = points[0] * points[2] / 100; 																	// power U*I [W]
		points[5] = Double.valueOf(points[2]/1.0 * points[3]/1000.0).intValue();	// energy U*C [mWh]
		//6=Temp.intern 7=Temp.extern
		points[6] = DataParser.parse2Short(dataBuffer, 20);
		points[7] = DataParser.parse2Short(dataBuffer, 22);
		//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8
		for (int i = 0,j=0; i < 10; i++,j+=2) {
			points[i+9] = DataParser.parse2Short(dataBuffer, 24+j);
			if (points[i + 9] > 0) {
				maxVotage = points[i + 9] > maxVotage ? points[i + 9] : maxVotage;
				minVotage = points[i + 9] < minVotage ? points[i + 9] : minVotage;
			}
		}
		//8=Balance
		points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

		return points;
	}

	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * since this is a long term operation the progress bar should be updated to signal business to user
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException
	 */
	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.size()];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 0;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize); //$NON-NLS-1$
			System.arraycopy(dataBuffer, i*dataBufferSize, convertBuffer, 0, dataBufferSize);

			//0=VersorgungsSpg. 1=Spannung 2=Strom
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			//3=Ladung 4=Leistung 5=Energie
			points[3] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			points[4] = Double.valueOf((points[1] / 1000.0) * (points[2] / 1000.0) * 10000).intValue(); 						// power U*I [W]
			points[5] = Double.valueOf((points[1] / 1000.0) * (points[3] / 1000.0)).intValue();											// energy U*C [mWh]
			//6=Temp.intern 7=Temp.extern
			points[6] = (((convertBuffer[16]&0xff) << 24) + ((convertBuffer[17]&0xff) << 16) + ((convertBuffer[18]&0xff) << 8) + ((convertBuffer[19]&0xff) << 0));
			points[7] = (((convertBuffer[20]&0xff) << 24) + ((convertBuffer[21]&0xff) << 16) + ((convertBuffer[22]&0xff) << 8) + ((convertBuffer[23]&0xff) << 0));

			int maxVotage = Integer.MIN_VALUE;
			int minVotage = Integer.MAX_VALUE;
			//7=SpannungZelle1 8=SpannungZelle2 9=SpannungZelle3 10=SpannungZelle4 11=SpannungZelle5 12=SpannungZelle6 13=SpannungZelle7 14=SpannungZelle8
			for (int j=0, k=0; j<10; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				//log.log(Level.OFF, j + " k+19 = " + (k+19));
				points[j + 9] = (((convertBuffer[k+24]&0xff) << 24) + ((convertBuffer[k+25]&0xff) << 16) + ((convertBuffer[k+26]&0xff) << 8) + ((convertBuffer[k+27]&0xff) << 0));
				if (points[j + 9] > 0) {
					maxVotage = points[j + 9] > maxVotage ? points[j + 9] : maxVotage;
					minVotage = points[j + 9] < minVotage ? points[j + 9] : minVotage;
				}
			}
			//calculate balance on the fly
			points[8] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

			recordSet.addPoints(points);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*2500)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
	}

	/**
	 * query number of Lithium cells of this charger device
	 * @return
	 */
	@Override
	public int getNumberOfLithiumCells() {
		return 10;
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread,
	 * target is to make sure all data point not coming from device directly are available and can be displayed
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are live measurement points only the calculation will take place directly after switch all to displayable
		if (recordSet.isRaw()) {
			// calculate the values required
			try {
				//0=Strom 1=VersorgungsSpg. 2=Spannung 3=Ladung 4=Leistung 5=Energie 6=Temp.intern 7=Temp.extern 8=Balance
				//9=SpannungZelle1 10=SpannungZelle2 11=SpannungZelle3 12=SpannungZelle4 13=SpannungZelle5 14=SpannungZelle6 15=SpannungZelle7 16=SpannungZelle8 17=Widerstand
				int displayableCounter = 0;

				Record recordCurrent = recordSet.get(0);
				Record recordVoltage = recordSet.get(2);
				Record recordCapacity = recordSet.get(3);
				Record recordPower = recordSet.get(4);
				Record recordEnergy = recordSet.get(5);
				Record recordBalance = recordSet.get(8);

				recordPower.clear();
				recordEnergy.clear();
				recordBalance.clear();

				for (int i = 0; i < recordCurrent.size(); i++) {
					//4=Leistung 5=Energie
					recordPower.add(recordVoltage.get(i) * recordCurrent.get(i) / 100); // power U*I [W]
					recordEnergy.add(recordVoltage.get(i) * recordCapacity.get(i) / 1000); // energy U*C [mWh]

					int maxVotage = Integer.MIN_VALUE;
					int minVotage = Integer.MAX_VALUE;
					for (int j = 0; j < this.getNumberOfLithiumCells(); j++) {

						int value = recordSet.get(j + 9).get(i);
						if (value > 0) {
							maxVotage = value > maxVotage ? value : maxVotage;
							minVotage = value < minVotage ? value : minVotage;
						}
					}
					//8=Balance
					recordBalance.add(maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0);

				}

				// check if measurements isActive == false and set to isDisplayable == false
				for (int i = 0; i < recordSet.size(); i++) {
					Record record = recordSet.get(i);
					if (record.isActive() && record.hasReasonableData()) {
						++displayableCounter;
					}
				}

				log.log(Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
				recordSet.setConfiguredDisplayable(displayableCounter);

				if (this.channels.getActiveChannel().getActiveRecordSet() == null || recordSet.getName().equals(this.channels.getActiveChannel().getActiveRecordSet().getName())) {
					this.application.updateGraphicsWindow();
				}
			}
			catch (RuntimeException e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
}
