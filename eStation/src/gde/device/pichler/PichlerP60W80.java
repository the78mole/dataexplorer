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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.device.pichler;

import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.InputTypes;
import gde.device.bantam.eStationBC6;
import gde.exception.DataInconsitsentException;
import gde.io.LogViewReader;
import gde.log.Level;
import gde.utils.StringHelper;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * Pichler P60 device class which is 100% eStation BC6
 * @author Winfried Brügmann
 */
public class PichlerP60W80 extends eStationBC6 {
	final static Logger						log	= Logger.getLogger(PichlerP60W80.class.getName());

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public PichlerP60W80(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public PichlerP60W80(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * get LogView data bytes size, as far as known modulo 16 and depends on the bytes received from device 
	 */
	@Override
	public int getLovDataByteSize() {
		return 120;  
	}
	/**
	 * add record data size points from LogView data stream to each measurement, if measurement is calculation 0 will be added
	 * adaption from LogView stream data format into the device data buffer format is required
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data
	 * this method is more usable for real logger, where data can be stored and converted in one block
	 * @param recordSet
	 * @param dataBuffer
	 * @param recordDataSize
	 * @param doUpdateProgressBar
	 * @throws DataInconsitsentException 
	 */
	@Override
	public synchronized void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int deviceDataBufferSize = Math.abs(this.getDataBlockSize(InputTypes.SERIAL_IO));
		int[] points = new int[this.getNumberOfMeasurements(1)];
		int offset = 0;
		int progressCycle = 0;
		int lovDataSize = this.getLovDataByteSize();
		long lastDateTime = 0, sumTimeDelta = 0, deltaTime = 0; 
		
		if (dataBuffer[0] == 0x7B) {
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset + i * lovDataSize, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
			
			recordSet.setTimeStep_ms(this.getAverageTimeStep_ms() != null ? this.getAverageTimeStep_ms() : 1280); // no average time available, use a hard coded one
		}
		else { // none constant time steps
			byte[] sizeBuffer = new byte[4];
			byte[] convertBuffer = new byte[deviceDataBufferSize];
			
			if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);
			for (int i = 0; i < recordDataSize; i++) {
				System.arraycopy(dataBuffer, offset, sizeBuffer, 0, 4);
				lovDataSize = 4 + LogViewReader.parse2Int(sizeBuffer);
				System.arraycopy(dataBuffer, offset + 4, convertBuffer, 0, deviceDataBufferSize);
				recordSet.addPoints(convertDataBytes(points, convertBuffer));
				offset += lovDataSize;
				
				StringBuilder sb = new StringBuilder();
				byte[] timeBuffer = new byte[lovDataSize - deviceDataBufferSize - 4];
				//sb.append(timeBuffer.length).append(" - ");
				System.arraycopy(dataBuffer, offset - timeBuffer.length, timeBuffer, 0, timeBuffer.length);
				String timeStamp = new String(timeBuffer).substring(0, timeBuffer.length-8)+"0000000000";
				long dateTime = new Long(timeStamp.substring(6,17));
				log.log(Level.FINEST, timeStamp + " " + timeStamp.substring(6,17) + " " + dateTime);
				sb.append(dateTime);
				//System.arraycopy(dataBuffer, offset - 4, sizeBuffer, 0, 4);
				//sb.append(" ? ").append(LogViewReader.parse2Int(sizeBuffer));
				deltaTime = lastDateTime == 0 ? 0 : (dateTime - lastDateTime)/1000 - 217; // value 217 is a compromis manual selected
				sb.append(" - ").append(deltaTime);
				sb.append(" - ").append(sumTimeDelta += deltaTime);
				log.log(Level.FINER, sb.toString());
				lastDateTime = dateTime;
				
				recordSet.addTimeStep_ms(sumTimeDelta);

				if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
			}
//			recordSet.setTimeStep_ms((double)sumTimeDelta/recordDataSize);
//			log.log(Level.FINE, sumTimeDelta/recordDataSize + " " + sumTimeDelta);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		updateVisibilityStatus(recordSet, true);
		recordSet.syncScaleOfSyncableRecords();
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
				
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, StringHelper.byte2FourDigitsIntegerString(dataBuffer, (byte)0x80, 1, dataBuffer.length-2));
		
		// 0=Spannung 1=Strom 2=Ladung 3=Leistung 4=Energie 5=Temp.extern 6=VersorgungsSpg. 7=Balance
		points[0] = Integer.valueOf((((dataBuffer[15] & 0xFF)-0x80)*100 + ((dataBuffer[16] & 0xFF)-0x80))*10);  //15,16   feed-back voltage
		points[1] = Integer.valueOf((((dataBuffer[11] & 0xFF)-0x80)*100 + ((dataBuffer[12] & 0xFF)-0x80))*10);  //11,12   feed-back current : 0=0.0A,900=9.00A
		points[2] = Integer.valueOf((((dataBuffer[6] & 0xFF)-0x80)*10 + ((dataBuffer[7] & 0xFF)-0x80)*100 + ((dataBuffer[8] & 0xFF)-0x80))*1000);//6,7,8  : charged capacity
		points[3] = Double.valueOf((points[0] / 1000.0) * (points[1] / 1000.0) * 1000).intValue(); 							// power U*I [W]
		points[4] = Double.valueOf((points[0] / 1000.0) * (points[2] / 1000.0)).intValue();											// energy U*C [mWh]
		points[5] = Integer.valueOf((((dataBuffer[37] & 0xFF)-0x80)*100 + ((dataBuffer[38] & 0xFF)-0x80))*10);  //37,38  fd_ex_th;     : external temperature
		points[6] = Integer.valueOf((((dataBuffer[4] & 0xFF)-0x80)*100 + ((dataBuffer[5] & 0xFF)-0x80))*10);  //4,5     : input voltage(00.00V 30.00V)

		// 8=SpannungZelle1 9=SpannungZelle2 10=SpannungZelle3 11=SpannungZelle4 12=SpannungZelle5 13=SpannungZelle6
		for (int i=0, j=0; i<points.length - 8; ++i, j+=2) {
			points[i + 8]  = Integer.valueOf((((dataBuffer[j+19] & 0xFF)-0x80)*100 + ((dataBuffer[j+20] & 0xFF)-0x80))*10);  //19,20 CELL_420v[1];
			if (points[i + 8] > 0) {
				maxVotage = points[i + 8] > maxVotage ? points[i + 8] : maxVotage;
				minVotage = points[i + 8] < minVotage ? points[i + 8] : minVotage;
			}
		}
		//calculate balance on the fly
		points[7] = maxVotage != Integer.MIN_VALUE && minVotage != Integer.MAX_VALUE ? maxVotage - minVotage : 0;

		return points;
	}

}
