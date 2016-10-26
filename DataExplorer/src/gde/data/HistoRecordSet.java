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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/

package gde.data;

import gde.GDE;
import gde.config.Settings;
import gde.device.ChannelType;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.device.MeasurementType;
import gde.device.SettlementType;
import gde.device.TransitionType;
import gde.exception.DataInconsitsentException;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * holds histo records for the configured measurements and for histo settlements.
 * the settlements support evaluations with calculations and transitions and simple properties for score values.
 * @author Thomas Eickert
 */
/**
 * @author Thomas Eickert
 */
public class HistoRecordSet extends RecordSet {
	final static String															$CLASS_NAME								= HistoRecordSet.class.getName();
	final static long																serialVersionUID					= -1580283867987273535L;
	final static Logger															log												= Logger.getLogger($CLASS_NAME);

	final static int																initialRecordCapacity			= 5555;														// vector capacity values are crucial for performance
	final static int																initialSettlementCapacity	= 22;															// vector capacity values are crucial for performance

	private LinkedHashMap<String, HistoSettlement>	evaluationSettlements;
	private int																			readingsCounter;
	private int																			sampledCounter;
	private int																			packagesCounter;
	private int																			packagesLostCounter;
	private Integer																	packagesLostRatioPerMille;
	private Integer																	packagesLostMax_ms;
	private Integer																	packagesLostMin_ms;
	private Integer																	packagesLostAvg_ms;
	private Integer																	packagesLostSigma_ms;
	private String[]																sensors										= new String[0];

	/**
	 * record set data buffers according the size of given names array, where the name is the key to access the data buffer
	 * @param channelNumber the channel number to be used
	 * @param newName for the records like "1) Laden"
	 * @param recordNames array of the device supported measurement and settlement names
	 * @param newTimeStep_ms time in msec of device measures points
	 * @param isRawValue specified if dependent values has been calculated
	 * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	 */
	public HistoRecordSet(IDevice useDevice, int channelNumber, String newName, String[] recordNames, double newTimeStep_ms, boolean isRawValue, boolean isFromFileValue) {
		super(useDevice, channelNumber, newName, recordNames, newTimeStep_ms, isRawValue, isFromFileValue);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, newName + " HistoRecordSet(IDevice, int, String, String[], double, boolean, boolean"); //$NON-NLS-1$
	}

	// /**
	// * record set data buffers according the size of given names array, where the name is the key to access the data buffer
	// * @param channelNumber the channel number to be used
	// * @param newName for the records like "1) Laden"
	// * @param measurementNames array of the device supported measurement names
	// * @param newTimeStep_ms time in msec of device measures points
	// * @param isRawValue specified if dependent values has been calculated
	// * @param isFromFileValue specifies if the data are red from file and if not modified don't need to be saved
	// */
	// private HistoRecordSet(IDevice useDevice, int channelNumber, String newName, String[] recordNames, double newTimeStep_ms, boolean isRawValue,
	// boolean isFromFileValue) {
	// super(useDevice, channelNumber, newName, recordNames, newTimeStep_ms, isRawValue, isFromFileValue);
	// if (log.isLoggable(Level.FINE))
	// log.log(Level.FINE, newName + " HistoRecordSet(IDevice, int, String, String[], double, boolean, boolean"); //$NON-NLS-1$
	//
	// }

	/**
	 * copy constructor - used to copy a record set during, where the configuration coming from the device properties file
	 * @param recordSet
	 * @param newChannelConfiguration
	 */
	protected HistoRecordSet(RecordSet recordSet, int dataIndex, boolean isFromBegin) {
		super(recordSet, dataIndex, isFromBegin);

	}

	/**
	 * clone method re-writes data points of all records of this record set - if isFromBegin == true, the given index is the index where the record starts after this operation - if isFromBegin == false, the given index represents the last data point index of the records.
	 * @param dataIndex
	 * @param isFromBegin
	 * @return new created record set
	 */
	public HistoRecordSet clone(int dataIndex, boolean isFromBegin) {
		return new HistoRecordSet(this, dataIndex, isFromBegin);
	}

	/**
	 * method to create a histo record set containing records according the device channel configuration
	 * which are loaded from device properties file.
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device 
	 * @param channelConfigNumber (number of the outlet or configuration)
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	public static HistoRecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, boolean isRaw, boolean isFromFile) {
		String[] recordNames = device.getMeasurementNames(channelConfigNumber);
		if (recordNames.length == 0) { // simple check for valid device and record names, as fall back use the config from the first channel/configuration
			recordNames = device.getMeasurementNames(channelConfigNumber = 1);
		}
		HistoRecordSet newRecordSet = new HistoRecordSet(device, channelConfigNumber, recordSetName, recordNames, device.getTimeStep_ms(), isRaw, isFromFile);
		if (log.isLoggable(Level.FINE)) printRecordNames("createHistoRecordSet() " + newRecordSet.name + " - ", newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		newRecordSet.timeStep_ms = new TimeSteps(device.getTimeStep_ms());

		String[] recordSymbols = new String[recordNames.length];
		String[] recordUnits = new String[recordNames.length];
		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			recordSymbols[i] = measurement.getSymbol();
			recordUnits[i] = measurement.getUnit();
		}
		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			Record tmpRecord = new Record(device, i, recordNames[i], recordSymbols[i], recordUnits[i], measurement.isActive(), measurement.getStatistics(), measurement.getProperty(), initialRecordCapacity);
			tmpRecord.setColorDefaultsAndPosition(i);
			newRecordSet.put(recordNames[i], tmpRecord);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("added record for %s - %d", recordNames[i], newRecordSet.size())); //$NON-NLS-1$
		}

		// check and update object key
		DataExplorer application = DataExplorer.getInstance();
		if (application != null && application.isObjectoriented()) {
			Channel activeChannel = Channels.getInstance().getActiveChannel();
			if (activeChannel != null && !activeChannel.getObjectKey().equals(Settings.getInstance().getActiveObject())) {
				activeChannel.setObjectKey(Settings.getInstance().getActiveObject());
			}
		}
		newRecordSet.syncScaleOfSyncableRecords();
		return newRecordSet;
	}

	/**
	 * method to create a histo record set containing records according the given record names, symbols and units.
	 * active status as well as statistics and properties are used from device properties.
	 * @param recordSetName the name of the record set
	 * @param device the instance of the device 
	 * @param channelConfigNumber (name of the outlet or configuration)
	 * @param recordNames array of names to be used for created records
	 * @param recordSymbols array of symbols to be used for created records
	 * @param recordUnits array of units to be used for created records
	 * @param timeStep_ms 
	 * @param isRaw defines if the data needs translation using device specific properties
	 * @param isFromFile defines if a configuration change must be recorded to signal changes
	 * @return a record set containing all records (empty) as specified
	 */
	public static HistoRecordSet createRecordSet(String recordSetName, IDevice device, int channelConfigNumber, String[] recordNames, String[] recordSymbols, String[] recordUnits, double timeStep_ms,
			boolean isRaw, boolean isFromFile) {
		HistoRecordSet newRecordSet = new HistoRecordSet(device, channelConfigNumber, recordSetName, recordNames, timeStep_ms, isRaw, isFromFile);
		if (log.isLoggable(Level.FINE)) printRecordNames("createHistoRecordSet() " + newRecordSet.name + " - ", newRecordSet.getRecordNames()); //$NON-NLS-1$ //$NON-NLS-2$

		newRecordSet.timeStep_ms = new TimeSteps(device.getTimeStep_ms());

		for (int i = 0; i < recordNames.length; i++) {
			MeasurementType measurement = device.getMeasurement(channelConfigNumber, i);
			Record tmpRecord = new Record(device, i, recordNames[i], recordSymbols[i], recordUnits[i], measurement.isActive(), measurement.getStatistics(), measurement.getProperty(), initialRecordCapacity);
			tmpRecord.setColorDefaultsAndPosition(i);
			newRecordSet.put(recordNames[i], tmpRecord);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("added record for %s - %d", recordNames[i], newRecordSet.size())); //$NON-NLS-1$
		}

		// check and update object key
		DataExplorer application = DataExplorer.getInstance();
		if (application != null && application.isObjectoriented()) {
			Channel activeChannel = Channels.getInstance().getActiveChannel();
			if (activeChannel != null && !activeChannel.getObjectKey().equals(Settings.getInstance().getActiveObject())) {
				activeChannel.setObjectKey(Settings.getInstance().getActiveObject());
			}
		}
		newRecordSet.syncScaleOfSyncableRecords();
		return newRecordSet;
	}

	/**
	 * method to add a series of points to the associated records
	 * @param points, where the length must fit records.size()
	 * @throws DataInconsitsentException 
	 */
	public synchronized void addNullablePoints(Integer[] points) throws DataInconsitsentException {
		final String $METHOD_NAME = "addNullablePoints"; //$NON-NLS-1$
		if (points.length == this.size()) {
			for (int i = 0; i < points.length; i++) {
				this.getRecord(this.recordNames[i]).add(points[i]);
			}
			if (log.isLoggable(Level.FINEST)) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < points.length; i++) {
					sb.append(points[i]).append(GDE.STRING_BLANK);
				}
				log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, sb.toString());
			}
		}
		else
			throw new DataInconsitsentException(Messages.getString(MessageIds.GDE_MSGE0035, new Object[] { this.getClass().getSimpleName(), $METHOD_NAME, points.length, this.size() })); // $NON-NLS-1$

		this.hasDisplayableData = true;
	}

	/**
	 * fetch liveRecords objects from the liveRecordSet and drop them into the corresponding record.
	 * do nothing if the device does not support live statistics or live statistics have not been collected.
	 * @param liveRecordSet
	 */
	public void dispatchLiveStatistics() {
		//		this.liveRecordSet = ((DeviceConfiguration) this.getDevice()).getLiveRecordSet();
		//		if (this.liveRecordSet != null) {
		//			for (Entry<String, Record> entry : this.entrySet()) {
		//				entry.getValue().setLiveRecord(this.liveRecordSet.getLiveRecords().get(entry.getKey()));
		//				if (log.isLoggable(Level.FINER)) {
		//					log.log(Level.FINER, this.liveRecordSet.getLiveRecords().get(entry.getKey()).toString());
		//					log.log(Level.FINER, String.format("%s compare to avgRecord %d  sigmaRecord %d  sizeRecord %d", super.getName() , entry.getValue().getAvgValue() , entry.getValue().getSigmaValue(), entry.getValue().realSize())); //$NON-NLS-1$ //$NON-NLS-2$
		//				}
		//			}
		//		}
	}

	/**
	 * returns a specific data vector selected by given key data name
	 * @param recordNameKey
	 * @return Vector<Integer>
	 */
	public HistoSettlement getEvaluationSettlement(String recordNameKey) {
		return this.evaluationSettlements.get(recordNameKey);
	}

	public Integer getReadingsCounter() {
		return this.readingsCounter;
	}

	public void setReadingsCounter(Integer value) {
		this.readingsCounter = value;
	}

	public Integer getSampledCounter() {
		return this.sampledCounter;
	}

	public void setSampledCounter(Integer value) {
		this.sampledCounter = value;
	}

	public Integer getPackagesCounter() {
		return this.packagesCounter;
	}

	public void setPackagesCounter(Integer value) {
		this.packagesCounter = value;
	}

	public Integer getPackagesLostCounter() {
		return this.packagesLostCounter;
	}

	public void setPackagesLostCounter(Integer value) {
		this.packagesLostCounter = value;
	}

	public Integer getPackagesLostPerMille() {
		return this.packagesLostRatioPerMille;
	}

	public void setPackagesLostPerMille(Integer value) {
		this.packagesLostRatioPerMille = value;
	}

	public Integer getPackagesLostMin_ms() {
		return this.packagesLostMin_ms;
	}

	public void setPackagesLostMin_ms(Integer value) {
		this.packagesLostMin_ms = value;
	}

	public Integer getPackagesLostMax_ms() {
		return this.packagesLostMax_ms;
	}

	public void setPackagesLostMax_ms(Integer value) {
		this.packagesLostMax_ms = value;
	}

	public Integer getPackagesLostAvg_ms() {
		return this.packagesLostAvg_ms;
	}

	public void setPackagesLostAvg_ms(Integer value) {
		this.packagesLostAvg_ms = value;
	}

	public Integer getPackagesLostSigma_ms() {
		return this.packagesLostSigma_ms;
	}

	public void setPackagesLostSigma_ms(Integer value) {
		this.packagesLostSigma_ms = value;
	}

	public String[] getSensors() {
		return this.sensors;
	}

	public void setSensors(String[] sensors) {
		this.sensors = sensors;
	}

	/**
	 * @return the minimum time step in msec
	 */
	public double getMinimumTimeStep_ms() {
		return this.timeStep_ms.getMinimumTimeStep_ms();
	}

	/**
	 * @return the maximum time step in msec
	 */
	public double getMaximumTimeStep_ms() {
		return this.timeStep_ms.getMaximumTimeStep_ms();
	}

	/**
	 * @return the standard deviation of the time step population in msec
	 */
	public double getSigmaTimeStep_ms() {
		return this.timeStep_ms.getSigmaTimeStep_ms();
	}

	public int getSyncMasterRecordOrdinal(HistoSettlement histoSettlement) {
		// TODO find better solution for syncing records and histo settlements
		throw new IllegalStateException();
	}

	public boolean isOneOfSyncableRecord(HistoSettlement histoSettlement) {
		// TODO find better solution for syncing records and histo settlements
		return false;
	}

	/**
	 * replaces existing settlements by new ones based on channel settlements and transitions.
	 * populates settlements according to evaluation rules.
	 */
	public void addEvaluationSettlements() {
		ChannelType channelType = ((DeviceConfiguration) this.device).getChannel(super.getChannelConfigNumber());
		HistoTransitions transitions = new HistoTransitions(this.device, this);
		for (TransitionType transitionType : channelType.getTransition()) {
			transitions.addFromRecord(this.get(this.recordNames[transitionType.getRefOrdinal()]), transitionType);
		}
		this.evaluationSettlements = new LinkedHashMap<String, HistoSettlement>();
		if (transitions.size() == 0) { // TODO implement evaluations w/o transitions
			for (SettlementType settlementType : channelType.getSettlement()) {
				if (settlementType.getEvaluation() != null) {
					HistoSettlement histoSettlement = new HistoSettlement(this.device, settlementType, this, initialSettlementCapacity);
					// logic misssing
					this.evaluationSettlements.put(settlementType.getName(), histoSettlement);
				}
			}
		}
		else {
			for (SettlementType settlementType : channelType.getSettlement()) {
				if (settlementType.getEvaluation() != null) {
					HistoSettlement histoSettlement = new HistoSettlement(this.device, settlementType, this, initialSettlementCapacity);
					Integer transitionId = settlementType.getEvaluation().getCalculation().getTransitionId(); // TODO decide if evaluations without calculation are useful
					histoSettlement.addFromTransitions(transitions, channelType.getTransitionById(transitionId));
					this.evaluationSettlements.put(settlementType.getName(), histoSettlement);
				}
			}
		}
	}
}