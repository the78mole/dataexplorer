package gde.device.spektrum;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.monstarmike.tlmreader.Flight;
import com.monstarmike.tlmreader.IFlight;
import com.monstarmike.tlmreader.TLMReader;
import com.monstarmike.tlmreader.datablock.AirspeedBlock;
import com.monstarmike.tlmreader.datablock.AltitudeBlock;
import com.monstarmike.tlmreader.datablock.AltitudeZeroBlock;
import com.monstarmike.tlmreader.datablock.CurrentBlock;
import com.monstarmike.tlmreader.datablock.DataBlock;
import com.monstarmike.tlmreader.datablock.EscBlock;
import com.monstarmike.tlmreader.datablock.FlightPackBlock;
import com.monstarmike.tlmreader.datablock.GForceBlock;
import com.monstarmike.tlmreader.datablock.GPSCollectorBlock;
import com.monstarmike.tlmreader.datablock.GPSLocationBlock;
import com.monstarmike.tlmreader.datablock.GPSStatusBlock;
import com.monstarmike.tlmreader.datablock.HeaderBlock;
import com.monstarmike.tlmreader.datablock.HeaderNameBlock;
import com.monstarmike.tlmreader.datablock.JetCatBlock;
import com.monstarmike.tlmreader.datablock.PowerBoxBlock;
import com.monstarmike.tlmreader.datablock.RxBlock;
import com.monstarmike.tlmreader.datablock.ServoDataBlock;
import com.monstarmike.tlmreader.datablock.StandardBlock;
import com.monstarmike.tlmreader.datablock.TemperatureBlock;
import com.monstarmike.tlmreader.datablock.VarioBlock;
import com.monstarmike.tlmreader.datablock.VoltageBlock;

import gde.Analyzer;
import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.resource.DeviceXmlResource;
import gde.exception.DataInconsitsentException;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuToolBar;
import gde.utils.FileUtils;
import gde.utils.ObjectKeyCompliance;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

public class SpektrumAdapter extends DeviceConfiguration implements IDevice {
	
	final static Logger		log																= Logger.getLogger(SpektrumAdapter.class.getName());

	
	final static Analyzer 						analyser 							= Analyzer.getInstance();	
	final static DataExplorer					application						=	DataExplorer.getInstance();
	final Settings										settings;
	final static Channels							channels							= Channels.getInstance();


	public SpektrumAdapter(String deviceProperties) throws FileNotFoundException, JAXBException {
		super(deviceProperties);
		// initializing the resource bundle for this device
		//Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.settings = Settings.getInstance();
		if (SpektrumAdapter.application.getMenuToolBar() != null) {
			String toolTipText = SpektrumAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(SpektrumAdapter.application.getMenuBar().getExportMenu());
			updateFileImportMenu(SpektrumAdapter.application.getMenuBar().getImportMenu());
		}
	}

	public SpektrumAdapter(DeviceConfiguration deviceConfig) {
		super(deviceConfig);
		// initializing the resource bundle for this device
		//Messages.setDeviceResourceBundle("gde.device.graupner.hott.messages", Settings.getInstance().getLocale(), this.getClass().getClassLoader()); //$NON-NLS-1$

		this.settings = Settings.getInstance();
		if (SpektrumAdapter.application.getMenuToolBar() != null) {
			String toolTipText = SpektrumAdapter.getImportToolTip();
			this.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, toolTipText, toolTipText);
			updateFileExportMenu(SpektrumAdapter.application.getMenuBar().getExportMenu());
			updateFileImportMenu(SpektrumAdapter.application.getMenuBar().getImportMenu());
		}
	}

	/**
	 * @return the tooltip text for the import menu bar button
	 */
	public static String getImportToolTip() {
		DeviceConfiguration hoTTConfiguration = Analyzer.getInstance().getDeviceConfigurations().get("HoTTAdapter");
		String fileExtentions = hoTTConfiguration != null ? hoTTConfiguration.getDataBlockPreferredFileExtention() : GDE.STRING_QUESTION_MARK;
		return Messages.getString(gde.messages.MessageIds.GDE_MSGT0964, new Object[] { fileExtentions });
	}

	/**
	 * export a file of the actual channel/record set
	 * @return full qualified file path depending of the file ending type
	 */
	@Override
	public String exportFile(String fileEndingType, boolean isExport2TmpDir) {
		String exportFileName = GDE.STRING_EMPTY;
		Channel activeChannel = SpektrumAdapter.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null && fileEndingType.contains(GDE.FILE_ENDING_KMZ)) {
				//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
				//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
				//Vario 12=Altitude V, 13=Climb V
				//Altitude	14=Altitude A
				//AltitudeZero 15=Altitude Offset
				//Voltage 16=Voltage V
				//Current 17=Current C
				//Temperature 18=Temperature T
				//AirSpeed 19=AirSpeed
				//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
				//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
				//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
				//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
				//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
				//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
				//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
				final int latOrdinal = 21, lonOrdinal = 22, altOrdinal = 20, climbOrdinal = 13, tripOrdinal = 28;
				final int additionalMeasurementOrdinal = this.getGPS2KMZMeasurementOrdinal();
				exportFileName = new FileHandler().exportFileKMZ(lonOrdinal, latOrdinal, altOrdinal, additionalMeasurementOrdinal, climbOrdinal, tripOrdinal, -1, true, isExport2TmpDir);
			}
		}
		return exportFileName;
	}

	/**
	 * exports the actual displayed data set to KML file format
	 * @param type DeviceConfiguration.HEIGHT_RELATIVE | DeviceConfiguration.HEIGHT_ABSOLUTE
	 */
	public void export2KMZ3D(int type) {
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		//21=Latitude, 22=Longitude, 20=Height, 13=Altitude, 23=Speed, 13=Climb, -1=TripLength, -1=Azimuth
	new FileHandler().exportFileKMZ(Messages.getString(gde.messages.MessageIds.GDE_MSGT0963), 21, 22, 20, 23, 13, -1, -1, type == DeviceConfiguration.HEIGHT_RELATIVE, type == DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
	}

	/**
	 * update the file export menu by adding two new entries to export KML/GPX files
	 * @param exportMenue
	 */
	public void updateFileExportMenu(Menu exportMenue) {
		MenuItem convertKMZ3DRelativeItem;
		MenuItem convertKMZDAbsoluteItem;
		// MenuItem convertGPXItem;
		// MenuItem convertGPXGarminItem;

		if (exportMenue.getItem(exportMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0732))) {
			new MenuItem(exportMenue, SWT.SEPARATOR);

			convertKMZ3DRelativeItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZ3DRelativeItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0965));
			convertKMZ3DRelativeItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZ3DRelativeItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_RELATIVE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0966));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_ABSOLUTE);
				}
			});

			convertKMZDAbsoluteItem = new MenuItem(exportMenue, SWT.PUSH);
			convertKMZDAbsoluteItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0967));
			convertKMZDAbsoluteItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "convertKMZDAbsoluteItem action performed! " + e); //$NON-NLS-1$
					export2KMZ3D(DeviceConfiguration.HEIGHT_CLAMPTOGROUND);
				}
			});
		}
	}

	/**
	 * update the file import menu by adding new entry to import device specific files
	 * @param importMenue
	 */
	public void updateFileImportMenu(Menu importMenue) {
		MenuItem importDeviceLogItem;

		if (importMenue.getItem(importMenue.getItemCount() - 1).getText().equals(Messages.getString(gde.messages.MessageIds.GDE_MSGT0018))) {
			new MenuItem(importMenue, SWT.SEPARATOR);

			importDeviceLogItem = new MenuItem(importMenue, SWT.PUSH);
			String[] messageParams = new String[GDE.MOD1.length + 1];
			System.arraycopy(GDE.MOD1, 0, messageParams, 1, GDE.MOD1.length);
			messageParams[0] = this.getDeviceConfiguration().getDataBlockPreferredFileExtention();
			importDeviceLogItem.setText(Messages.getString(gde.messages.MessageIds.GDE_MSGT0962, messageParams));
			importDeviceLogItem.setAccelerator(SWT.MOD1 + Messages.getAcceleratorChar(gde.messages.MessageIds.GDE_MSGT0962));
			importDeviceLogItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					SpektrumAdapter.log.log(java.util.logging.Level.FINEST, "importDeviceLogItem action performed! " + e); //$NON-NLS-1$
					importDeviceData();
				}
			});
		}
	}

	/**
	 * compose the record set extend to give capability to identify source of
	 * this record set
	 *
	 * @param fileName
	 * @return
	 */
	protected static String getRecordSetExtend(String fileName) {
		String recordSetNameExtend = GDE.STRING_EMPTY;
		if (fileName.contains(GDE.STRING_UNDER_BAR)) {
			try {
				Integer.parseInt(fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)).length() <= 8)
					recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.lastIndexOf(GDE.CHAR_UNDER_BAR)) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		else {
			try {
				Integer.parseInt(fileName.substring(0, 4));
				recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, 4) + GDE.STRING_RIGHT_BRACKET;
			}
			catch (Exception e) {
				if (fileName.substring(0, fileName.length()).length() <= 8 + 4) recordSetNameExtend = GDE.STRING_BLANK_LEFT_BRACKET + fileName.substring(0, fileName.length() - 4) + GDE.STRING_RIGHT_BRACKET;
			}
		}
		return recordSetNameExtend;
	}

	/**
	 * import device specific *.bin data files
	 */
	protected void importDeviceData() {
		final FileDialog fd = FileUtils.getImportDirectoryFileDialog(this, Messages.getString(gde.messages.MessageIds.GDE_MSGT0961), "LogData"); //$NON-NLS-1$

		Thread reader = new Thread("reader") { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					SpektrumAdapter.application.setPortConnected(true);

					for (String tmpFileName : fd.getFileNames()) {
						String selectedImportFile = fd.getFilterPath() + GDE.STRING_FILE_SEPARATOR_UNIX + tmpFileName;
						if (!selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TLM) && !selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_LOG)) {
							log.log(Level.WARNING, String.format("skip selectedImportFile %s since it has not a supported file ending", selectedImportFile));
						}
						SpektrumAdapter.log.log(java.util.logging.Level.FINE, "selectedImportFile = " + selectedImportFile); //$NON-NLS-1$

						if (fd.getFileName().length() > MIN_FILENAME_LENGTH) {
							// String recordNameExtend = selectedImportFile.substring(selectedImportFile.lastIndexOf(GDE.CHAR_DOT) - 4, selectedImportFile.lastIndexOf(GDE.CHAR_DOT));

							String directoryName = ObjectKeyCompliance.getUpcomingObjectKey(Paths.get(selectedImportFile));
							if (!directoryName.isEmpty()) ObjectKeyCompliance.createObjectKey(directoryName);

							try {
								long startTime = System.nanoTime() / 1000000;
								if (selectedImportFile.toLowerCase().endsWith(GDE.FILE_ENDING_DOT_TLM)) {
									TLMReader reader = new TLMReader();
									RecordSet tmpRecordSet = null;
									MenuToolBar menuToolBar = SpektrumAdapter.application.getMenuToolBar();
									SpektrumAdapter device = (SpektrumAdapter) SpektrumAdapter.application.getActiveDevice();
									String recordSetName = GDE.STRING_EMPTY;
									String recordSetNameExtend = getRecordSetExtend(new File(selectedImportFile).getName());
									Channel channel = null;
									int channelNumber = SpektrumAdapter.analyser.getActiveChannel().getNumber();
									
									if (new File(selectedImportFile).exists()) {
										String modelName = "???";
										PeriodFormatter formatter = new PeriodFormatterBuilder().appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().appendSuffix(".").appendMillis()
												.toFormatter();
										int index = 0;
										List<IFlight> flights = reader.parseFlightDefinitions(selectedImportFile);
										if (log.isLoggable(Level.FINE)) 
											log.log(Level.FINE, "found " + flights.size() + " flight sessions");
										String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date().getTime()); //$NON-NLS-1$
										String dateTime = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss").format(new Date().getTime()); //$NON-NLS-1$
										channel = SpektrumAdapter.channels.get(channelNumber);
										channel.setFileDescription(SpektrumAdapter.application.isObjectoriented() ? date + GDE.STRING_BLANK + SpektrumAdapter.application.getObjectKey() : date);
										
//										//print all measurement names										
//										System.out.println(new StandardBlock(new byte[25], new HeaderRpmBlock(new byte[25])).getMeasurementNames().toString());
//										System.out.println(new RxBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new VarioBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new AltitudeBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new AltitudeZeroBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new VoltageBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new CurrentBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new TemperatureBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new AirspeedBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(GPSCollectorBlock.getInstance().getMeasurementNames().toString());
//										System.out.println(new FlightPackBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new EscBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new PowerBoxBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new JetCatBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new GForceBlock(new byte[25]).getMeasurementNames().toString());
//										System.out.println(new ServoDataBlock(new byte[25]).getMeasurementNames().toString());
										if (log.isLoggable(Level.TIME)) 
											log.log(Level.TIME, "read flight definitions time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$

										for (IFlight flight : flights) {
											if (flight.getDuration().getMillis() > 5000 || flight.getNumberOfDataBlocks() > 10 || flight.getHeaderBlocks().size() > 0) {
												if (log.isLoggable(Level.INFO)) 
													log.log(Level.INFO, String.format("flight.getDuration() = %d ms", flight.getDuration().getMillis()));

												Flight currentFlight = reader.parseFlight(selectedImportFile, index);

												for (HeaderBlock header : currentFlight.getHeaderBlocks()) {
													if (header instanceof HeaderNameBlock) {
														if (((HeaderNameBlock) header).getModelName().length() > 3) modelName = ((HeaderNameBlock) header).getModelName();
													}
//													else if (header instanceof HeaderRxBlock)
//														System.out.println("isSpectrumTelemetrySystem=" + ((HeaderRxBlock) header).isSpectrumTelemetrySystem());
//													else if (header instanceof HeaderDataBlock) {
//														System.out.println("SensorTypeEnabled=" + ((HeaderDataBlock) header).getSensorTypeEnabled());
//														System.out.println("isTerminatingBlock=" + ((HeaderDataBlock) header).isTerminatingBlock());
//													}
//													else
//														System.out.println(header.getClass().getSimpleName() + " - " + header.toString());
												}

												if (currentFlight.getDuration().getStandardSeconds() > 5) {
													if (log.isLoggable(Level.INFO)) 
														log.log(Level.INFO, String.format("model %s flight %d duration() = %s", modelName, index, formatter.print(currentFlight.getDuration().toPeriod())));
													List<DataBlock> dataBlocks = currentFlight.getDataBlocks();
													if (log.isLoggable(Level.INFO)) 
														log.log(Level.INFO, "current flight contains " + dataBlocks.size() + " DataBlocks, and " + currentFlight.getHeaderBlocks().size() + " headerBlocks");
													
													currentFlight.removeRedundantDataBlocks();
													
													int recordSetNumber = SpektrumAdapter.channels.get(1).maxSize() + 1;
													long numberDatablocks = dataBlocks.size() + 1;
													int progressIndicator = 10;
													GDE.getUiNotification().setProgress(0);
													int indexDataBlock = 0;
													recordSetName = recordSetNumber + device.getRecordSetStemNameReplacement() + recordSetNameExtend;
													tmpRecordSet = RecordSet.createRecordSet(recordSetName, device, channelNumber, true, true, true);
													channel.put(recordSetName, tmpRecordSet);
													tmpRecordSet = channel.get(recordSetName);
													tmpRecordSet.setRecordSetDescription(device.getName() + GDE.STRING_MESSAGE_CONCAT + Messages.getString(MessageIds.GDE_MSGT0129) + dateTime);
													//tmpRecordSet.setStartTimeStamp(startTimeStamp_ms);
													int[] points = new int[device.getNumberOfMeasurements(channelNumber)];
													long timeOffset = -1;
													//recordSet initialized and ready to add data
													
													//1=Standard 2=Rx 3=Vario 4=Altitude 5=AltitudeZero 6=Voltage 7=Current 8=Temperature 9=AirSpeed
													//10=GPS	11=FlightPack 12=ESC 13=PowerBox 14=JetCat 15=GForce 16=Servo
													int sizeSupportedDataBlockTypes = 16;
													boolean[] isResetMinMax = new boolean[sizeSupportedDataBlockTypes];

													for (DataBlock data : dataBlocks) {
														++indexDataBlock;
														if (data instanceof StandardBlock) {
															//System.out.println(((StandardBlock) data).toString());
															//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
															isResetMinMax[0] = mergeRawData(((StandardBlock) data).getMeasurementValues(), points, 0, 5, tmpRecordSet, isResetMinMax[0], 3);
														}
														else if (data instanceof RxBlock) {
															//System.out.println(((RxBlock) data).toString());
															//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
															isResetMinMax[1] = mergeRawData(((RxBlock) data).getMeasurementValues(), points, 5, 7, tmpRecordSet, isResetMinMax[1], 6);
														}
														else if (data instanceof VarioBlock) {
															//System.out.println(((VarioBlock) data).toString());
															//Vario 12=Altitude V, 13=Climb V
															mergeRawData(((VarioBlock) data).getMeasurementValues(), points, 12, 2, tmpRecordSet, true, -1);
														}
														//primitive data blocks
														else if (data instanceof AltitudeBlock) {
															//System.out.println(((AltitudeBlock) data).toString());
															//Altitude	14=Altitude A
															mergeRawData(((AltitudeBlock) data).getMeasurementValues(), points, 14, 1, tmpRecordSet, true, -1);
														}
														else if (data instanceof AltitudeZeroBlock) {
															//System.out.println(((AltitudeZeroBlock) data).toString());
															//AltitudeZero 15=Altitude Offset
															mergeRawData(((AltitudeZeroBlock) data).getMeasurementValues(), points, 15, 1, tmpRecordSet, true, -1);
														}
														else if (data instanceof VoltageBlock) {
															//System.out.println(((VoltageBlock) data).toString());
															//Voltage 16=Voltage V
															isResetMinMax[5] = mergeRawData(((VoltageBlock) data).getMeasurementValues(), points, 16, 1, tmpRecordSet, isResetMinMax[5], 0);
														}
														else if (data instanceof CurrentBlock) {
															//System.out.println(((CurrentBlock) data).toString());
															//Current 17=Current C
															mergeRawData(((CurrentBlock) data).getMeasurementValues(), points, 17, 1, tmpRecordSet, true, -1);
														}
														else if (data instanceof TemperatureBlock) {
															//System.out.println(((TemperatureBlock) data).toString());
															//Temperature 18=Temperature T
															isResetMinMax[7] = mergeRawData(((TemperatureBlock) data).getMeasurementValues(), points, 18, 1, tmpRecordSet, isResetMinMax[7], 0);
														}
														else if (data instanceof AirspeedBlock) {
															//System.out.println(((AirspeedBlock) data).toString());
															//AirSpeed 19=AirSpeed
															mergeRawData(((AirspeedBlock) data).getMeasurementValues(), points, 19, 1, tmpRecordSet, true, -1);
														}
														//other important data blocks
														else if (data instanceof GPSLocationBlock) {
															//System.out.println(((GPSLocationBlock) data).toString());
															GPSCollectorBlock.getInstance().updateLocation((GPSLocationBlock) data);
															if (GPSCollectorBlock.getInstance().isUpdated()) {
																//System.out.println(GPSCollectorBlock.getInstance().toString());
																//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
																isResetMinMax[9] = mergeGPSRawData(GPSCollectorBlock.getInstance().getMeasurementValues(), points, 20, 8, tmpRecordSet, isResetMinMax[9], 1);
															}
														}
														else if (data instanceof GPSStatusBlock) {
															//System.out.println(((GPSStatusBlock) data).toString());
															GPSCollectorBlock.getInstance().updateStatus((GPSStatusBlock) data);
															if (GPSCollectorBlock.getInstance().isUpdated()) {
																//System.out.println(GPSCollectorBlock.getInstance().toString());
																isResetMinMax[9] = mergeGPSRawData(GPSCollectorBlock.getInstance().getMeasurementValues(), points, 20, 8, tmpRecordSet, isResetMinMax[9], 1);
															}
														}
														else if (data instanceof FlightPackBlock) {
															//System.out.println(((FlightPackBlock) data).toString());
															//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
															mergeRawData(((FlightPackBlock) data).getMeasurementValues(), points, 29, 6, tmpRecordSet, true, -1);
														}
														else if (data instanceof EscBlock) {
															//System.out.println(((EscBlock) data).toString());
															//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
															isResetMinMax[11] = mergeRawData(((EscBlock) data).getMeasurementValues(), points, 35, 9, tmpRecordSet, isResetMinMax[11], 1);
														}
														else if (data instanceof PowerBoxBlock) {
															//System.out.println(((PowerBoxBlock) data).toString());
															//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
															isResetMinMax[12] = mergeRawData(((PowerBoxBlock) data).getMeasurementValues(), points, 44, 5, tmpRecordSet, isResetMinMax[12], 0);
														}
														else if (data instanceof JetCatBlock) {
															//System.out.println(((JetCatBlock) data).toString());
															//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
															isResetMinMax[13] = mergeRawData(((JetCatBlock) data).getMeasurementValues(), points, 49, 7, tmpRecordSet, isResetMinMax[13], 2);
														}
														else if (data instanceof GForceBlock) {
															//System.out.println(((GForceBlock) data).toString());
															//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
															mergeRawData(((GForceBlock) data).getMeasurementValues(), points, 56, 7, tmpRecordSet, true, -1);
														}
														else if (data instanceof ServoDataBlock) {
															//System.out.println(((ServoDataBlock) data).toString());
															//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
															mergeRawData(((ServoDataBlock) data).getMeasurementValues(), points, 63, 20, tmpRecordSet, true, -1);
														}
														else
															log.log(Level.WARNING, data.toString());
														

														if (timeOffset == -1) timeOffset = data.getTimestamp();

														tmpRecordSet.addPoints(points, (data.getTimestamp() - timeOffset) * 10.0);
														if (indexDataBlock % progressIndicator == 0) 
															GDE.getUiNotification().setProgress((int) (indexDataBlock * 100 / numberDatablocks));
													}
													
													if (GDE.isWithUi() && tmpRecordSet != null) {
														device.makeInActiveDisplayable(tmpRecordSet);

														// write filename after import to record description
														tmpRecordSet.descriptionAppendFilename(new File(selectedImportFile).getName());
														channel.applyTemplate(recordSetName, false);
														menuToolBar.updateChannelSelector();
														menuToolBar.updateRecordSetSelectCombo();
														SpektrumAdapter.channels.switchChannel(channelNumber, recordSetName);
														GDE.getUiNotification().setProgress(100);
													}
													if (log.isLoggable(Level.TIME)) 
														log.log(Level.TIME, String.format("read flight %d time = %s", index, StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime)))); //$NON-NLS-1$ //$NON-NLS-2$
													WaitTimer.delay(100); //enable refresh
												}
												++index;
											}
										}
									}
								}
								if (log.isLoggable(Level.TIME)) 
									log.log(Level.TIME, "overall read time = " + StringHelper.getFormatedTime("mm:ss:SSS", (System.nanoTime() / 1000000 - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
							} catch (Exception e) {
								SpektrumAdapter.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
							}
						}
					}
				} finally {
					SpektrumAdapter.application.setPortConnected(false);
				}
			}

			/**
			 * merge method to integrate raw data values into normalized output data points
			 * @param measurementRawValues input values from DataBlock
			 * @param points output data points
			 * @param destPos 
			 * @param length
			 * @param recordSet to be reset min/max with start values
			 * @param isResetMinMax supported data block index
			 * @param zeroValueIndex value to be use to detect valid data
			 */
			private boolean mergeRawData(List<Integer> measurementRawValues, int[] points, int destPos, int length, RecordSet recordSet, boolean isResetMinMax, int zeroValueIndex) {
				boolean isReset = isResetMinMax;
				int[] srcValues = new int[measurementRawValues.size()];
				for (int i=0; i < measurementRawValues.size(); ++i)
					srcValues[i] = measurementRawValues.get(i) * 1000;
				
				if (!isResetMinMax && measurementRawValues.get(zeroValueIndex) != 0) {
					for (int i = destPos, j = 0; i < destPos+length; ++i, ++j) 
						recordSet.get(i).setMinMax(srcValues[j], srcValues[j]);
					isReset = true;
				}

				System.arraycopy(srcValues, 0, points, destPos, length);
				return isReset;
			}

			/**
			 * merge method to integrate raw data values into normalized output data points
			 * @param measurementRawValues input values from DataBlock
			 * @param points output data points
			 * @param destPos 
			 * @param length
			 * @param recordSet to be reset min/max with start values
			 * @param isResetMinMax supported data block index
			 * @param zeroValueIndex value to be use to detect valid data
			 */
			private boolean mergeGPSRawData(List<Integer> measurementRawValues, int[] points, int destPos, int length, RecordSet recordSet, boolean isResetMinMax, int zeroValueIndex) {
				boolean isReset = isResetMinMax;
				int[] srcValues = new int[measurementRawValues.size()-1];
				for (int i=0; i < measurementRawValues.size()-1; ++i)
					if (i == 1 || i == 2)
						srcValues[i] = measurementRawValues.get(i);
					else
						srcValues[i] = measurementRawValues.get(i) * 1000;

				if (!isResetMinMax && measurementRawValues.get(zeroValueIndex) != 0) {
					for (int i = destPos, j = 0; i < destPos+length; ++i, ++j) 
						recordSet.get(i).setMinMax(srcValues[j], srcValues[j]);
					isReset = true;
				}

				System.arraycopy(srcValues, 0, points, destPos, length);
				return isReset;
			}
		};
		reader.start();
	}

	@Override
	public HashMap<String, String> getLovKeyMappings(HashMap<String, String> lov2osdMap) {
		return null;
	}

	@Override
	public String getConvertedRecordConfigurations(HashMap<String, String> header, HashMap<String, String> lov2osdMap, int channelNumber) {
		return null;
	}

	@Override
	public int getLovDataByteSize() {
		return 0;
	}

	@Override
	public int[] convertDataBytes(int[] points, byte[] dataBuffer) {
		return points;
	}

	@Override
	public void addDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
		int dataBufferSize = GDE.SIZE_BYTES_INTEGER * recordSet.getNoneCalculationRecordNames().length;
		int[] points = new int[recordSet.getNoneCalculationRecordNames().length];
					String sThreadId = String.format("%06d", Thread.currentThread().getId()); //$NON-NLS-1$
		int progressCycle = 1;
		if (doUpdateProgressBar) SpektrumAdapter.application.setProgress(progressCycle, sThreadId);

		int timeStampBufferSize = GDE.SIZE_BYTES_INTEGER * recordDataSize;
		int index = 0;
		for (int i = 0; i < recordDataSize; i++) {
			index = i * dataBufferSize + timeStampBufferSize;
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, i + " i*dataBufferSize+timeStampBufferSize = " + index); //$NON-NLS-1$

			for (int j = 0; j < points.length; j++) {
				points[j] = (((dataBuffer[0 + (j * 4) + index] & 0xff) << 24) + ((dataBuffer[1 + (j * 4) + index] & 0xff) << 16) + ((dataBuffer[2 + (j * 4) + index] & 0xff) << 8)
						+ ((dataBuffer[3 + (j * 4) + index] & 0xff) << 0));
			}

			recordSet.addNoneCalculationRecordsPoints(points,
					(((dataBuffer[0 + (i * 4)] & 0xff) << 24) + ((dataBuffer[1 + (i * 4)] & 0xff) << 16) + ((dataBuffer[2 + (i * 4)] & 0xff) << 8) + ((dataBuffer[3 + (i * 4)] & 0xff) << 0)) / 10.0);

			if (doUpdateProgressBar && i % 50 == 0) SpektrumAdapter.application.setProgress(((++progressCycle * 5000) / recordDataSize), sThreadId);
		}
		if (doUpdateProgressBar) SpektrumAdapter.application.setProgress(100, sThreadId);
		recordSet.syncScaleOfSyncableRecords();
		}

	@Override
	public void addConvertedLovDataBufferAsRawDataPoints(RecordSet recordSet, byte[] dataBuffer, int recordDataSize, boolean doUpdateProgressBar) throws DataInconsitsentException {
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
				int ordinal = record.getOrdinal();
				//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
				//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
				//Vario 12=Altitude V, 13=Climb V
				//Altitude	14=Altitude A
				//AltitudeZero 15=Altitude Offset
				//Voltage 16=Voltage V
				//Current 17=Current C
				//Temperature 18=Temperature T
				//AirSpeed 19=AirSpeed
				//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
				//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
				//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
				//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
				//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
				//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
				//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
				if (ordinal >= 3 && ordinal <= 10) {
					dataTableRow[index + 1] = String.format("%.0f", (record.realGet(rowIndex) / 1000.0)); //$NON-NLS-1$
				}
				else {
					dataTableRow[index + 1] = record.getFormattedTableValue(rowIndex);
				}
				++index;
			}
		}
		catch (RuntimeException e) {
			log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
		}
		return dataTableRow;
	}

	/**
	 * query if the given record is longitude or latitude of GPS data, such data needs translation for display as graph
	 * @param record
	 * @return
	 */
	@Override
	public boolean isGPSCoordinates(Record record) {
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		final int latOrdinal = 21, lonOrdinal = 22;
		return record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal;
	}

	/**
	 * query if the actual record set of this device contains GPS data to enable KML export to enable google earth visualization
	 * set value of -1 to suppress this measurement
	 */
	@Override
	public boolean isActualRecordSetWithGpsData() {
		boolean containsGPSdata = false;
		Channel activeChannel = SpektrumAdapter.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
				final int latOrdinal = 21, lonOrdinal = 22;
				containsGPSdata = activeRecordSet.get(latOrdinal).hasReasonableData() && activeRecordSet.get(lonOrdinal).hasReasonableData();
			}
		}
		return containsGPSdata;
	}

	/**
	 * @return the measurement ordinal where velocity limits as well as the colors are specified (GPS-velocity)
	 */
	@Override
	public Integer getGPS2KMZMeasurementOrdinal() {
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		if (kmzMeasurementOrdinal == null) // keep usage as initial supposed and use speed measurement ordinal
			return 23;

		return kmzMeasurementOrdinal;
	}

	@Override
	public double translateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double newValue = 0;
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		final int latOrdinal = 21, lonOrdinal = 22;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { //15=Latitude, 16=Longitude
			int grad = ((int) (value / 1000));
			double minuten = (value - (grad * 1000.0)) / 10.0;
			newValue = grad + minuten / 60.0;
		}
		else {
			newValue = value * factor;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	@Override
	public double reverseTranslateValue(Record record, double value) {
		double factor = record.getFactor(); // != 1 if a unit translation is required
		double newValue = 0;
		//Standard 0=RPM St, 1=Volt St, 2=Temperature St, 3=dbm_A, 4=dbm_B
		//Rx	5=LostPacketsReceiver A, 6=LostPacketsReceiver B, 7=LostPacketsReceiver L, 8=LostPacketsReceiver R, 9=FrameLoss, 10=Holds, 11=VoltageRx
		//Vario 12=Altitude V, 13=Climb V
		//Altitude	14=Altitude A
		//AltitudeZero 15=Altitude Offset
		//Voltage 16=Voltage V
		//Current 17=Current C
		//Temperature 18=Temperature T
		//AirSpeed 19=AirSpeed
		//GPS	20=Altitude GPS, 21=Latitude, 22=Longitude, 23=Speed GPS, 24=Satellites GPS, 25=Course, 26=HDOP, 27=GPSFix, 28=Trip/UTC
		//FlightPack 29=Current FPA, 30=Capacity FPA, 31=Temperature FPA, 32=Current FPB, 33=Capacity FPB, 34=Temperature FPB
		//ESC 35=RPM ESC, 36=Voltage ESC, 37=TempFET ESC, 38=Current ESC, 39=CurrentBEC ESC, 40=VoltsBEC ESC, 41=Throttle ESC, 42=PowerOut ESC, 43=PowerIn ESC
		//PowerBox 44=Voltage PB1, 45=Capacity PB1, 46=Voltage PB2, 47=Capacity PB2, 48=Alarms PB
		//JetCat 49=RawECUStatus JC, 50=Throttle JC, 51=PackVoltage JC, 52=PumpVoltage JC, 53=RPM JC, 54=EGT JC, 55=RawOffCondition JC
		//GForce 56=X GF, 57=Y GF, 58=Z GF, 59=Xmax GF, 60=Ymax GF, 61=Zmax GF, 62=Zmin GF
		//Channel 63=Ch 1, ..., 70=Ch 8, ..., 82=Ch 20]
		final int latOrdinal = 21, lonOrdinal = 22;
		if (record.getOrdinal() == latOrdinal || record.getOrdinal() == lonOrdinal) { // 15=Latitude, 16=Longitude
			int grad = (int) value;
			double minuten = (value - grad * 1.0) * 60.0;
			newValue = (grad + minuten / 100.0) * 1000.0;
		}
		else {
			newValue = value / factor;
		}

		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "for " + record.getName() + " in value = " + value + " out value = " + newValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return newValue;
	}

	@Override
	public void makeInActiveDisplayable(RecordSet recordSet) {
		if (recordSet != null) {
			//calculateInactiveRecords(recordSet);
			recordSet.syncScaleOfSyncableRecords();
			this.updateVisibilityStatus(recordSet, true);
			SpektrumAdapter.application.updateStatisticsData();
		}
	}

	@Override
	public void updateVisibilityStatus(RecordSet recordSet, boolean includeReasonableDataCheck) {
		int channelConfigNumber = recordSet.getChannelConfigNumber();
		int displayableCounter = 0;
		boolean configChanged = this.isChangePropery();
		Record record;

		// check if measurements isActive == false and set to isDisplayable == false
		for (int i = 0; i < recordSet.size(); ++i) {
			// since actual record names can differ from device configuration measurement names, match by ordinal
			record = recordSet.get(i);
			if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE))
				SpektrumAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " = " + DeviceXmlResource.getInstance().getReplacement(this.getMeasurementNames(channelConfigNumber)[i])); //$NON-NLS-1$

			MeasurementType measurement = this.getMeasurement(channelConfigNumber, i);
			if (record.isActive() && record.isActive() != measurement.isActive()) { //corrected values from older OSD might be overwritten p.e. VoltageRx_min
				record.setActive(measurement.isActive());
				record.setVisible(measurement.isActive());
				record.setDisplayable(measurement.isActive());
				if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, "switch " + record.getName() + " to " + measurement.isActive()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (includeReasonableDataCheck) {
				record.setDisplayable(measurement.isActive() && record.hasReasonableData());
				if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, record.getName() + " hasReasonableData " + record.hasReasonableData()); //$NON-NLS-1$
			}

			if (record.isActive() && record.isDisplayable()) {
				++displayableCounter;
				if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, "add to displayable counter: " + record.getName()); //$NON-NLS-1$
			}
		}
		if (SpektrumAdapter.log.isLoggable(java.util.logging.Level.FINE)) SpektrumAdapter.log.log(java.util.logging.Level.FINE, "displayableCounter = " + displayableCounter); //$NON-NLS-1$
		recordSet.setConfiguredDisplayable(displayableCounter);
		this.setChangePropery(configChanged); // reset configuration change indicator to previous value, do not vote automatic configuration change at all
	}

	@Override
	public String[] getUsedPropertyKeys() {
		return new String[] {FACTOR};
	}

	@Override
	public void open_closeCommPort() {
		importDeviceData();
	}
}
