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
****************************************************************************************/
package gde.device.bantam;

import java.io.FileNotFoundException;
import java.util.Vector;

import javax.xml.bind.JAXBException;

import gde.DE;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.exception.DataInconsitsentException;
import gde.log.Level;

/**
 * eStation BC6 device class
 * @author Winfried Brügmann
 */
public class eStationBC6 extends eStation {

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public eStationBC6(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public eStationBC6(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		this.dialog = new EStationDialog(this.application.getShell(), this);
	}

	/**
	 * @return the dialog
	 */
	public EStationDialog getDialog() {
		return this.dialog;
	}
	
	/**
	 * convert the device bytes into raw values, no calculation will take place here, see translateValue reverseTranslateValue
	 * inactive or to be calculated data point are filled with 0 and needs to be handles after words
	 * @param points pointer to integer array to be filled with converted data
	 * @param dataBuffer byte arrax with the data to be converted
	 */
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {		
		
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg. 
		points[0] = Integer.valueOf((((dataBuffer[35] & 0xFF)-0x80)*100 + ((dataBuffer[36] & 0xFF)-0x80))*10);  //35,36   feed-back voltage
		points[1] = Integer.valueOf((((dataBuffer[33] & 0xFF)-0x80)*100 + ((dataBuffer[34] & 0xFF)-0x80))*10);  //33,34   feed-back current : 0=0.0A,900=9.00A
		points[2] = Integer.valueOf((((dataBuffer[43] & 0xFF)-0x80)*100 + ((dataBuffer[44] & 0xFF)-0x80))*1000);  //43,44  cq_capa_dis;  : charged capacity
		points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = Integer.valueOf((((dataBuffer[41] & 0xFF)-0x80)*100 + ((dataBuffer[42] & 0xFF)-0x80))*10);  //41,42  fd_in_12v;    : input voltage(00.00V 30.00V)
		// 6=SpannungZelle1 7=SpannungZelle2 8=SpannungZelle3 9=SpannungZelle4 10=SpannungZelle5 11=SpannungZelle6
		for (int i=0, j=0; i<points.length - 6; ++i, j+=2) {
			//log_base.info("cell " + (i+1) + " points[" + (i+6) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
			points[i+6]  = Integer.valueOf((((dataBuffer[j+45] & 0xFF)-0x80)*100 + ((dataBuffer[j+46] & 0xFF)-0x80))*10);  //45,46 CELL_420v[1];
		}

		return points;
	}
	
	/**
	 * add record data size points from file stream to each measurement
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest
	 * do not forget to call makeInActiveDisplayable afterwords to calualte th emissing data
	 * since this is a long term operation the progress bar should be updated to signal busyness to user 
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		byte[] convertBuffer = new byte[dataBufferSize];
		int[] points = new int[recordSet.getRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId());
		int progressCycle = 0;
		Vector<Integer> timeStamps = new Vector<Integer>(1,1);
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
		
		int timeStampBufferSize = 0;
		if(!recordSet.isTimeStepConstant()) {
			timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
			byte[] timeStampBuffer = new byte[timeStampBufferSize];
			System.arraycopy(dataBuffer, 0, timeStampBuffer, 0, timeStampBufferSize);

			for (int i = 0; i < recordDataSize; i++) {
				timeStamps.add(((timeStampBuffer[0 + (i * 4)] & 0xff) << 24) + ((timeStampBuffer[1 + (i * 4)] & 0xff) << 16) + ((timeStampBuffer[2 + (i * 4)] & 0xff) << 8) + ((timeStampBuffer[3 + (i * 4)] & 0xff) << 0));
			}
		}
		log.log(Level.FINE, timeStamps.size() + " timeStamps = " + timeStamps.toString());
		
		for (int i = 0; i < recordDataSize; i++) {
			log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + i*dataBufferSize+timeStampBufferSize);
			System.arraycopy(dataBuffer, i*dataBufferSize+timeStampBufferSize, convertBuffer, 0, dataBufferSize);
			
			// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=VersorgungsSpg. 
			points[0] = (((convertBuffer[0]&0xff) << 24) + ((convertBuffer[1]&0xff) << 16) + ((convertBuffer[2]&0xff) << 8) + ((convertBuffer[3]&0xff) << 0));
			points[1] = (((convertBuffer[4]&0xff) << 24) + ((convertBuffer[5]&0xff) << 16) + ((convertBuffer[6]&0xff) << 8) + ((convertBuffer[7]&0xff) << 0));
			points[2] = (((convertBuffer[8]&0xff) << 24) + ((convertBuffer[9]&0xff) << 16) + ((convertBuffer[10]&0xff) << 8) + ((convertBuffer[11]&0xff) << 0));
			points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
			points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
			points[5] = (((convertBuffer[12]&0xff) << 24) + ((convertBuffer[13]&0xff) << 16) + ((convertBuffer[14]&0xff) << 8) + ((convertBuffer[15]&0xff) << 0));
			// 6=SpannungZelle1 7=SpannungZelle2 8=SpannungZelle3 9=SpannungZelle4 10=SpannungZelle5 11=SpannungZelle6
			for (int j=0, k=0; j<points.length - 6; ++j, k+=GDE.SIZE_BYTES_INTEGER) {
				//log_base.info("cell " + (i+1) + " points[" + (i+8) + "]  = new Integer((((dataBuffer[" + (j+45) + "] & 0xFF)-0x80)*100 + ((dataBuffer[" + (j+46)+ "] & 0xFF)-0x80))*10);");  //45,46 CELL_420v[1];
				points[j+6] = (((convertBuffer[k+16]&0xff) << 24) + ((convertBuffer[k+17]&0xff) << 16) + ((convertBuffer[k+18]&0xff) << 8) + ((convertBuffer[k+19]&0xff) << 0));
			}
			
			if(recordSet.isTimeStepConstant()) 
				recordSet.addPoints(points);
			else
				recordSet.addPoints(points, timeStamps.get(i)/10.0);
			
			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle*5000)/recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
	}
}
