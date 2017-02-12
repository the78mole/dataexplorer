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
package gde.device.graupner;

import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

/**
 * Graupner HoTT device class with Mikrokopter adaptation, GPS coordinates in decimal degrees instead of degree decimal mintes
 * @author Winfried Brügmann
 */
public class HoTTAdapter2M extends HoTTAdapter2 {

	/**
	 * @param deviceProperties
	 * @throws FileNotFoundException
	 * @throws JAXBException
	 */
	public HoTTAdapter2M(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
	}

	/**
	 * @param deviceConfig
	 */
	public HoTTAdapter2M(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
	}

	/**
	 * function to prepare a data table row of record set while translating available measurement values
	 * @return pointer to filled data table row with formated values
	 */
	@Override
	public String[] prepareDataTableRow(RecordSet recordSet, String[] dataTableRow, int rowIndex) {
		try {
			int index = 0;
			for (final Record record : recordSet.getVisibleAndDisplayableRecordsForTable()) {
				double offset = record.getOffset(); // != 0 if curve has an defined offset
				double reduction = record.getReduction();
				double factor = record.getFactor(); // != 1 if a unit translation is required
				int ordinal = record.getOrdinal();
				//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
				//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
				//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
				//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
				//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
				//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
				//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM
				//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM

				if (ordinal == 13 || ordinal == 14) { //12=Latitude, 13=Longitude 
					dataTableRow[index + 1] = String.format("%02.7f", record.realGet(rowIndex) / 1000000.0); //$NON-NLS-1$
				}
				else if (ordinal >= 0 && ordinal <= 5){
					dataTableRow[index + 1] = String.format("%.0f",(record.realGet(rowIndex) / 1000.0));
				}
				else {
					dataTableRow[index + 1] = record.getDecimalFormat().format((offset + ((record.realGet(rowIndex) / 1000.0) - reduction) * factor));
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			HoTTAdapter2.logger.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM
		//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM

		if (record.getOrdinal() == 13 || record.getOrdinal() == 14) { //13=Latitude, 14=Longitude
			newValue = value / 1000.0;
		}
		else {
			newValue = (value - reduction) * factor + offset;
		}

		HoTTAdapter2.logger.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to reverse translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double offset = record.getOffset(); // != 0 if a unit translation is required
		double reduction = record.getReduction(); // != 0 if a unit translation is required
		double newValue = 0;

		//0=RX-TX-VPacks, 1=RXSQ, 2=Strength, 3=VPacks, 4=Tx, 5=Rx, 6=VoltageRx, 7=TemperatureRx 8=VoltageRxMin
		//9=Height, 10=Climb 1, 11=Climb 3, 12=Climb 10
		//13=Latitude, 14=Longitude, 15=Velocity, 16=DistanceStart, 17=DirectionStart, 18=TripDistance
		//19=Voltage G, 20=Current G, 21=Capacity G, 22=Power G, 23=Balance G, 24=CellVoltage G1, 25=CellVoltage G2 .... 29=CellVoltage G6, 30=Revolution G, 31=FuelLevel, 32=Voltage G1, 33=Voltage G2, 34=Temperature G1, 35=Temperature G2
		//36=Voltage E, 37=Current E, 38=Capacity E, 39=Power E, 40=Balance E, 41=CellVoltage E1, 42=CellVoltage E2 .... 54=CellVoltage E14, 55=Voltage E1, 56=Voltage E2, 57=Temperature E1, 58=Temperature E2 59=Revolution E
		//60=Ch 1, 61=Ch 2, 62=Ch 3 .. 75=Ch 16, 76=PowerOff, 77=BatterieLow, 78=Reset, 79=reserve
		//80=VoltageM, 81=CurrentM, 82=CapacityM, 83=PowerM, 84=RevolutionM, 85=TemperatureM
		//60=VoltageM, 61=CurrentM, 62=CapacityM, 63=PowerM, 64=RevolutionM, 65=TemperatureM

		if (record.getOrdinal() == 13 || record.getOrdinal() == 14) { // 13=Latitude, 14=Longitude
			newValue = value * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		HoTTAdapter2.logger.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

}
