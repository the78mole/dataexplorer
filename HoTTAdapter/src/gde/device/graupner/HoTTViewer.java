package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.data.Channel;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.graupner.hott.MessageIds;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.Messages;

import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

public class HoTTViewer extends HoTTAdapter implements IDevice {
	final static Logger									logg														= Logger.getLogger(HoTTViewer.class.getName());

	/**
	 * constructor using properties file
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public HoTTViewer(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_OPEN_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * constructor using existing device configuration
	 * @param deviceConfig device configuration
	 */
	public HoTTViewer(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		if (this.application.getMenuToolBar() != null) {
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_OPEN_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404), Messages.getString(MessageIds.GDE_MSGT2404));
			updateFileExportMenu(this.application.getMenuBar().getExportMenu());
		}
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	@Override
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(MessageIds.GDE_MSGT2405));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter2.logger.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2406));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter2.logger.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(MessageIds.GDE_MSGT2407));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					HoTTAdapter2.logger.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
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
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
		String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) this.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (HoTTAdapter2.logger.isLoggable(Level.FINER))
				HoTTAdapter2.logger.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$
			
			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8) + ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addNoneCalculationRecordsPoints(points, 
						(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8)	+ ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) this.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) this.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
		if (recordSet.getRecordSetDescription().contains(GDE.STRING_LEFT_PARENTHESIS))
			recordSet.setRecordSetDescription(this.getName() + recordSet.getRecordSetDescription().substring(recordSet.getRecordSetDescription().lastIndexOf(GDE.STRING_LEFT_PARENTHESIS)));
		else if (recordSet.getRecordSetDescription().contains(GDE.STRING_MESSAGE_CONCAT))
			recordSet.setRecordSetDescription(this.getName() + recordSet.getRecordSetDescription().substring(recordSet.getRecordSetDescription().indexOf(GDE.STRING_MESSAGE_CONCAT)));
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
				//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
				//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
				//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
				//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
				final int latOrdinal = 22, lonOrdinal = 23;
				if (ordinal == latOrdinal || ordinal == lonOrdinal) { //13=Latitude, 14=Longitude 
					int grad = record.realGet(rowIndex) / 1000000;
					double minuten = record.realGet(rowIndex) % 1000000 / 10000.0;
					dataTableRow[index + 1] = String.format("%02d %07.4f", grad, minuten); //$NON-NLS-1$
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
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization 
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
				//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
				//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
				//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
				containsGPSdata = activeRecordSet.get(22).hasReasonableData() && activeRecordSet.get(23).hasReasonableData();
			}
		}
		return containsGPSdata;
	}
	
	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		final int latOrdinal = 22, lonOrdinal = 23;
		return record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		return 7;
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
				//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
				//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
				//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
				final int latOrdinal = 22, lonOrdinal = 23, altOrdinal = 6, climbOrdinal = 3, speedOrdinal = 7;
				exportFileName = new FileHandler().exportFileKMZ(lonOrdinal, latOrdinal, altOrdinal, speedOrdinal, climbOrdinal, -1, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	@Override
	public void export2KMZ3D(int type) {
		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		final int latOrdinal = 22, lonOrdinal = 23, altOrdinal = 6, climbOrdinal = 3, speedOrdinal = 7;
		new FileHandler().exportFileKMZ(Messages.getString(MessageIds.GDE_MSGT2403), lonOrdinal, latOrdinal, altOrdinal, speedOrdinal, climbOrdinal, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE | DeviceConfiguration.HEIGHT_CLAMPTOGROUND
	 */
	@Override
	public void export2GPX(final boolean isGarminExtension) {
		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		if (isGarminExtension)
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 	22, 23, 6, 7, -1, -1, -1, -1, new int[] {-1,-1,-1});
		else
			new FileHandler().exportFileGPX(Messages.getString(gde.messages.MessageIds.GDE_MSGT0730), 	22, 23, 6, 7, -1, -1, -1, -1, new int[0]);
	}

	/**
	 * function to translate measured values from a device to values represented
	 * this function should be over written by device and measurement specific algorithm
	 * @return double of device dependent value
	 */
	@Override
	public double translateValue(Record record, double value) {
		double newValue = 0;

		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		final int latOrdinal = 22, lonOrdinal = 23;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { //13=Latitude, 14=Longitude
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			double factor = record.getFactor(); // != 1 if a unit translation is required
			double offset = record.getOffset(); // != 0 if a unit translation is required
			double reduction = record.getReduction(); // != 0 if a unit translation is required
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

		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		final int latOrdinal = 22, lonOrdinal = 23;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { // 13=Latitude, 14=Longitude
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = (value - offset) / factor + reduction;
		}

		HoTTAdapter2.logger.log(java.util.logging.Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	/**
	 * function to calculate values for inactive records, data not readable from device
	 * if calculation is done during data gathering this can be a loop switching all records to displayable
	 * for calculation which requires more effort or is time consuming it can call a background thread, 
	 * target is to make sure all data point not coming from device directly are available and can be displayed 
	 */
	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {

		//0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
		//9=Voltage, 10=Current, 11=Capacity, 12=Power 13=Fuel, 14=Balance, 15=CellAverage
		//16=Temperature 1, 17=Temperature 2, 18=Voltage 1, 19=Voltage 2, 
		//20=DistanceStart, 21=DirectionStart, 22=Latitude, 23=Longitude, 24=VoltageTx
		recordSet.syncScaleOfSyncableRecords();
		this.application.updateStatisticsData(true);
		this.updateVisibilityStatus(recordSet, true);
		this.application.getActiveChannel().setFileDescription(recordSet.getFormatedTime_sec(0, true).trim().split(GDE.STRING_BLANK)[0]);
		
//    // start laps calculation
//    String[] measurements = recordSet.getActiveRecordNames(); //0=RXSQ, 1=VoltageRx, 2=TemperatureRx, 3=Climb 1, 4=Climb 3, 5=Climb 10, 6=Height, 7=Speed, 8=Revolution
//    int regressionInterval = 6; //5 seconds interval used for first linear regression
//    this.calculationThread = new LinearRegression(recordSet, measurements[0], measurements[1], regressionInterval); //RXSQ is source record VPacks is temporary target
//    try {
//        this.calculationThread.start();
//        this.calculationThread.join();
//    }
//    catch (Exception e) {
//        log.log(Level.WARNING, e.getMessage(), e);
//    }
//   
//    regressionInterval = 3; //5 seconds interval used for first linear regression       
//    this.calculationThread = new LinearRegression(recordSet, measurements[1], measurements[2], regressionInterval); //tmp VPack is source, Strenght is temporary target
//    try {
//        this.calculationThread.start();
//        this.calculationThread.join();
//    }
//    catch (Exception e) {
//        log.log(Level.WARNING, e.getMessage(), e);
//    }
//
//    long deadTime_ms = 5000; //12 seconds time minimum time space between laps
//    long maxTimeForLapCounting_ms = 6 * 60 * 1000; // maximum time frame in which laps should be calculated
//    double lapStartTime_ms = 0;
//    int lapTime = 0;
//    Record laps = recordSet.get(3); //RXSQ this is target for the laps
//    Record smoothedAndDiffRxdbm = recordSet.get(2); //this record contains the last stored smoothed data
//   
//    int i = 0;
//    for (; i < smoothedAndDiffRxdbm.realSize(); i++) { //skip time to start lap calculation
//        laps.set(i, lapTime);
//        if (smoothedAndDiffRxdbm.getTime_ms(i) > 1.5*deadTime_ms)
//            break;
//    }
//    int lastValue = 0;
//    for (; i < smoothedAndDiffRxdbm.realSize() && smoothedAndDiffRxdbm.getTime_ms(i) <= maxTimeForLapCounting_ms; i++) {
//        if (lastValue > 0 && smoothedAndDiffRxdbm.get(i) <= 0) { //lap event detected
//            if (lapStartTime_ms != 0) {
//                log.log(Level.OFF, String.format("Lap time in sec %03.1f", (recordSet.getTime_ms(i) - lapStartTime_ms)/1000.0));
//                lapTime = (int) (recordSet.getTime_ms(i) - lapStartTime_ms);
//            }
//            lapStartTime_ms = recordSet.getTime_ms(i);
//            while (smoothedAndDiffRxdbm.getTime_ms(i)-lapStartTime_ms < deadTime_ms && i < smoothedAndDiffRxdbm.realSize()-1) // skip dead time before start search next event
//                laps.set(i++, lapTime);
//        }
//        laps.set(i, lapTime);
//        lastValue = smoothedAndDiffRxdbm.get(i);
//    }
//    for (; i < smoothedAndDiffRxdbm.realSize(); i++) {
//        laps.set(i, 0);
//    }
//
//		GPSHelper.calculateLabs(this, recordSet, 22, 23, 20, -1, 7);
		recordSet.setSaved(true);

	}

}
