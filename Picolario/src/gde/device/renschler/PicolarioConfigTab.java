/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device.renschler;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DataTypes;
import osde.device.IDevice;
import osde.device.MeasurementType;
import osde.device.PropertyType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CalculationThread;

/**
 * Configuration tab for Picolario configration entries
 * @author Winfried Bruegmann
 */
public class PicolarioConfigTab extends Composite {
	{
		SWTResourceManager.registerResourceUser(this);
	}

	final static Logger						log										= Logger.getLogger(PicolarioConfigTab.class.getName());

	Group													heightAdaptionGroup;
	Label													heightLabel;
	Button												noAdaptionButton;
	Button												reduceByDefinedValueButton;
	Button												reduceByFirstValueButton;
	Button												reduceByLastValueButton;
	CCombo												heightOffset;
	CLabel												slopeUnit;
	CLabel												slopeName;
	CLabel												heightUnit;
	CLabel												calculationTypeLabel;
	CCombo												slopeCalculationTypeCombo;
	CCombo												regressionTime;

	String												heightDataUnit				= "m";																									// Meter is default
	boolean												doNoAdation						= false;
	boolean												doHeightAdaption			= false;																								// indicates to adapt height values
	boolean												doSubtractFirst				= true;																								// indicates to subtract first values from all other
	boolean												doSubtractLast				= false;																								// indicates to subtract last values from all other
	boolean												doOffsetHeight				= false;																								// indicates that the height has to be corrected by an offset
	int														heightOffsetSelection	= 7;																										// represents the offset the measurment should be modified
	double												heightOffsetValue			= 100;																									// represents the offset value
	String												slopeDataUnit					= "m/s";																								// Meter is default
	String												slopeTypeSelection		= CalculationThread.REGRESSION_TYPE_CURVE;
	int														slopeTimeSelection;

	final Picolario								device;																																			// get device specific things, get serial port, ...
	final OpenSerialDataExplorer	application;
	String												configName;																																	// tabName

	/**
	 * panel tab describing a configuration
	 * @param parent
	 * @param useDevice
	 * @param tabName
	 */
	public PicolarioConfigTab(Composite parent, Picolario useDevice, String tabName) {
		super(parent, SWT.NONE);
		this.device = useDevice;
		this.configName = tabName;
		this.application = OpenSerialDataExplorer.getInstance();
		initEditable();
		initGUI();
	}

	private void initGUI() {
		try {
			FillLayout thisLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
			this.setLayout(thisLayout);
			this.setSize(310, 180);
			{
				this.setLayout(null);
				FillLayout composite1Layout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.setLayout(composite1Layout);
				{ // group 2
					this.heightAdaptionGroup = new Group(this, SWT.NONE);
					this.heightAdaptionGroup.setLayout(null);
					this.heightAdaptionGroup.setText("Berechnungseinstellung für Höhe und Steigung");
					this.heightAdaptionGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							PicolarioConfigTab.log.finest("heightAdaptionGroup2.paintControl, event=" + evt);
							PicolarioConfigTab.this.heightUnit.setText("[" + PicolarioConfigTab.this.heightDataUnit + "]");
							PicolarioConfigTab.this.noAdaptionButton.setSelection(PicolarioConfigTab.this.doNoAdation);
							PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(PicolarioConfigTab.this.doSubtractFirst);
							PicolarioConfigTab.this.reduceByLastValueButton.setSelection(PicolarioConfigTab.this.doSubtractLast);
							PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(PicolarioConfigTab.this.doOffsetHeight);
							PicolarioConfigTab.this.heightOffset.setText(new Double(PicolarioConfigTab.this.heightOffsetValue).toString());

							PicolarioConfigTab.this.slopeUnit.setText("[" + PicolarioConfigTab.this.slopeDataUnit + "]");
							PicolarioConfigTab.this.regressionTime.select(PicolarioConfigTab.this.slopeTimeSelection - 1);
							PicolarioConfigTab.this.slopeCalculationTypeCombo.select(PicolarioConfigTab.this.slopeTypeSelection.equals(CalculationThread.REGRESSION_TYPE_CURVE) ? 1 : 0);
						}
					});
					{
						this.noAdaptionButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.noAdaptionButton.setText("Höhenwerte nicht anpassen");
						this.noAdaptionButton.setBounds(12, 40, 186, 16);
						this.noAdaptionButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								PicolarioConfigTab.log.finest("noAdaptioButton.widgetSelected, event=" + evt);
								PicolarioConfigTab.this.noAdaptionButton.setSelection(true);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(false);
								PicolarioConfigTab.this.doNoAdation = true;
								PicolarioConfigTab.this.doSubtractFirst = false;
								PicolarioConfigTab.this.doSubtractLast = false;
								PicolarioConfigTab.this.doOffsetHeight = false;

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[1]; // height
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doNoAdation);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doSubtractFirst);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doSubtractLast);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doOffsetHeight);
								PicolarioConfigTab.this.heightOffsetValue = 0.0;
								PicolarioConfigTab.this.heightOffset.setText(new Double(PicolarioConfigTab.this.heightOffsetValue).toString());
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, IDevice.OFFSET, DataTypes.DOUBLE,
										PicolarioConfigTab.this.heightOffsetValue);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								PicolarioConfigTab.this.application.updateGraphicsWindow();
							}
						});
					}
					{
						this.reduceByFirstValueButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.reduceByFirstValueButton.setText("ersten Höhenwert von den folgenden abziehen");
						this.reduceByFirstValueButton.setSelection(this.doSubtractFirst);
						this.reduceByFirstValueButton.setBounds(12, 61, 297, 16);
						this.reduceByFirstValueButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								PicolarioConfigTab.log.finest("reduceByFirstValueButton.widgetSelected, event=" + evt);
								PicolarioConfigTab.this.noAdaptionButton.setSelection(false);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(true);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(false);
								PicolarioConfigTab.this.doNoAdation = false;
								PicolarioConfigTab.this.doSubtractFirst = true;
								PicolarioConfigTab.this.doSubtractLast = false;
								PicolarioConfigTab.this.doOffsetHeight = false;

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[1]; // height
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN, false);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, true);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN, false);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN, false);
								PicolarioConfigTab.this.heightOffsetValue = 0.0;
								PicolarioConfigTab.this.heightOffset.setText(new Double(PicolarioConfigTab.this.heightOffsetValue).toString());
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, IDevice.OFFSET, DataTypes.DOUBLE,
										PicolarioConfigTab.this.heightOffsetValue);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								PicolarioConfigTab.this.application.updateGraphicsWindow();
							}
						});
					}
					{
						this.reduceByLastValueButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.reduceByLastValueButton.setBounds(12, 80, 293, 18);
						this.reduceByLastValueButton.setText("letzten Höhenwert von allen anderen abziehen");
						this.reduceByLastValueButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								PicolarioConfigTab.log.finest("reduceByLastValueButton.widgetSelected, event=" + evt);
								PicolarioConfigTab.this.noAdaptionButton.setSelection(false);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(true);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(false);
								PicolarioConfigTab.this.doNoAdation = false;
								PicolarioConfigTab.this.doSubtractFirst = false;
								PicolarioConfigTab.this.doSubtractLast = true;
								PicolarioConfigTab.this.doOffsetHeight = false;

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[1]; // height
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doNoAdation);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doSubtractFirst);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doSubtractLast);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doOffsetHeight);
								PicolarioConfigTab.this.heightOffsetValue = 0.0;
								PicolarioConfigTab.this.heightOffset.setText(new Double(PicolarioConfigTab.this.heightOffsetValue).toString());
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, IDevice.OFFSET, DataTypes.DOUBLE,
										PicolarioConfigTab.this.heightOffsetValue);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								PicolarioConfigTab.this.application.updateGraphicsWindow();
							}
						});
					}
					{
						this.reduceByDefinedValueButton = new Button(this.heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						this.reduceByDefinedValueButton.setText("Offset Höhe");
						this.reduceByDefinedValueButton.setSelection(this.doOffsetHeight);
						this.reduceByDefinedValueButton.setBounds(12, 103, 143, 16);
						this.reduceByDefinedValueButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								PicolarioConfigTab.log.finest("reduceByDefinedValueButton.widgetSelected, event=" + evt);
								PicolarioConfigTab.this.noAdaptionButton.setSelection(false);
								PicolarioConfigTab.this.reduceByFirstValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByLastValueButton.setSelection(false);
								PicolarioConfigTab.this.reduceByDefinedValueButton.setSelection(true);
								PicolarioConfigTab.this.doNoAdation = false;
								PicolarioConfigTab.this.doOffsetHeight = true;
								PicolarioConfigTab.this.doSubtractFirst = false;
								PicolarioConfigTab.this.doSubtractLast = false;
								PicolarioConfigTab.this.heightOffsetValue = new Double(PicolarioConfigTab.this.heightOffset.getText()).doubleValue();

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[1]; // height
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doNoAdation);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doSubtractFirst);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doSubtractLast);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN,
										PicolarioConfigTab.this.doOffsetHeight);
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, IDevice.OFFSET, DataTypes.DOUBLE,
										PicolarioConfigTab.this.heightOffsetValue);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								PicolarioConfigTab.this.application.updateGraphicsWindow();
							}
						});
					}
					{
						this.heightOffset = new CCombo(this.heightAdaptionGroup, SWT.BORDER);
						final String[] heightOffsetValues = new String[] { "200", "100", "50", "0", "-50", "-100", "-150", "-200", "-250", "-300", "-400", "-500", "-750", "-1000", "-1500" };
						this.heightOffset.setItems(heightOffsetValues);
						this.heightOffset.setText(new Double(this.heightOffsetValue).toString());
						for (@SuppressWarnings("unused")String element : heightOffsetValues) { // loop only
							if (heightOffsetValues.equals(this.heightOffsetValue)) this.heightOffset.select(this.heightOffsetSelection);
						}
						this.heightOffset.setBounds(184, 101, 116, 21);
						this.heightOffset.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								PicolarioConfigTab.log.finest("heightOffset.widgetSelected, event=" + evt);
								PicolarioConfigTab.this.heightOffsetValue = new Double(heightOffsetValues[PicolarioConfigTab.this.heightOffset.getSelectionIndex()]).doubleValue();

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[1]; // height
								MeasurementType measurement = PicolarioConfigTab.this.device.getMeasurement(PicolarioConfigTab.this.configName, measurementKey);
								measurement.setOffset(PicolarioConfigTab.this.heightOffsetValue);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								PicolarioConfigTab.this.application.updateGraphicsWindow();
							}
						});
						this.heightOffset.addKeyListener(new KeyAdapter() {
							@Override
							public void keyPressed(KeyEvent evt) {
								PicolarioConfigTab.log.finest("heightOffset.keyPressed, event=" + evt);
								if (evt.character == SWT.CR) {
									//heightOffsetSelection 
									try {
										PicolarioConfigTab.this.heightOffsetValue = new Double(PicolarioConfigTab.this.heightOffset.getText().replace(',', '.')).doubleValue();

										String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[1]; // height
										MeasurementType measurement = PicolarioConfigTab.this.device.getMeasurement(PicolarioConfigTab.this.configName, measurementKey);
										measurement.setOffset(PicolarioConfigTab.this.heightOffsetValue);
										PicolarioConfigTab.this.device.setChangePropery(true);
										PicolarioConfigTab.this.device.storeDeviceProperties();
										PicolarioConfigTab.this.application.updateGraphicsWindow();
									}
									catch (NumberFormatException e) {
										PicolarioConfigTab.log.log(Level.WARNING, e.getMessage(), e);
										PicolarioConfigTab.this.application.openMessageDialog("Eingabefehler : " + e.getMessage());
									}
								}
							}
						});
					}
					{
						this.heightLabel = new Label(this.heightAdaptionGroup, SWT.NONE);
						this.heightLabel.setBounds(12, 21, 76, 20);
						this.heightLabel.setText("Höhe");
					}
					{
						this.heightUnit = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.heightUnit.setBounds(90, 16, 60, 20);
						this.heightUnit.setText("[m]");
					}
					{
						this.calculationTypeLabel = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.calculationTypeLabel.setBounds(33, 151, 87, 20);
						this.calculationTypeLabel.setText("Berechnung");
					}
					{
						this.slopeName = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.slopeName.setBounds(12, 127, 76, 20);
						this.slopeName.setText("Steigung");
					}
					{
						this.slopeCalculationTypeCombo = new CCombo(this.heightAdaptionGroup, SWT.BORDER);
						this.slopeCalculationTypeCombo.setBounds(140, 151, 97, 20);
						this.slopeCalculationTypeCombo.setItems(new String[] { " " + CalculationThread.REGRESSION_TYPE_LINEAR, " " + CalculationThread.REGRESSION_TYPE_CURVE });
						this.slopeCalculationTypeCombo.setToolTipText("Hier den Berechnungstyp einstellen");
						this.slopeCalculationTypeCombo.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								if (PicolarioConfigTab.log.isLoggable(Level.FINEST)) PicolarioConfigTab.log.finest("slopeCalculationTypeCombo.widgetSelected, event=" + evt);
								if (PicolarioConfigTab.this.slopeCalculationTypeCombo.getSelectionIndex() == 1)
									PicolarioConfigTab.this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
								else
									PicolarioConfigTab.this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_LINEAR;

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[2]; // slope
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, CalculationThread.REGRESSION_TYPE, DataTypes.STRING,
										PicolarioConfigTab.this.slopeTypeSelection);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
								activeRecordSet.get(measurementKey).setDisplayable(false);
								PicolarioConfigTab.this.device.makeInActiveDisplayable(activeRecordSet);
							}
						});
					}
					{
						this.regressionTime = new CCombo(this.heightAdaptionGroup, SWT.BORDER);
						this.regressionTime.setBounds(239, 151, 61, 20);
						this.regressionTime.setItems(new String[] { " 1 s", " 2 s", " 3 s", " 4 s", " 5 s", " 6 s", " 7 s", " 8 s", " 9 s", "10 s", "11 s", "12 s", "13 s", "14 s", "15 s", "16 s", "17 s", "18 s",
								"19 s", "20 s" });
						this.regressionTime.setToolTipText("Hier die Regressionszeit in Sekunden einstellen");
						this.regressionTime.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								if (PicolarioConfigTab.log.isLoggable(Level.FINEST)) PicolarioConfigTab.log.finest("regressionTime.widgetSelected, event=" + evt);
								PicolarioConfigTab.this.slopeTimeSelection = PicolarioConfigTab.this.regressionTime.getSelectionIndex() + 1;

								String measurementKey = PicolarioConfigTab.this.device.getMeasurementNames(PicolarioConfigTab.this.configName)[2]; // slope
								PicolarioConfigTab.this.device.setMeasurementPropertyValue(PicolarioConfigTab.this.configName, measurementKey, CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER,
										PicolarioConfigTab.this.slopeTimeSelection);
								PicolarioConfigTab.this.device.setChangePropery(true);
								PicolarioConfigTab.this.device.storeDeviceProperties();
								Channel activeChannel = Channels.getInstance().getActiveChannel();
								if (activeChannel != null) {
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null) {
										activeRecordSet.get(measurementKey).setDisplayable(false);
										PicolarioConfigTab.this.device.makeInActiveDisplayable(activeRecordSet);
									}
								}
							}
						});
					}
					{
						this.slopeUnit = new CLabel(this.heightAdaptionGroup, SWT.NONE);
						this.slopeUnit.setBounds(90, 130, 60, 20);
						this.slopeUnit.setText("[m/s]");
					}
				}

			}
			this.layout();
		}
		catch (Exception e) {
			PicolarioConfigTab.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * retrieve initial values from device properties file for editable fields
	 */
	private void initEditable() {
		String recordKey = this.device.getMeasurementNames(this.configName)[1]; // height
		MeasurementType measurement = this.device.getMeasurement(this.configName, recordKey);

		this.heightDataUnit = measurement.getUnit();
		PicolarioConfigTab.log.finer("heightDataUnit = " + this.heightDataUnit);

		PropertyType property = measurement.getProperty(Picolario.DO_NO_ADAPTION);
		this.doNoAdation = new Boolean(property != null ? property.getValue() : "false").booleanValue();
		PicolarioConfigTab.log.finer("doHeightAdaption = " + this.doHeightAdaption);

		property = measurement.getProperty(Picolario.DO_SUBTRACT_FIRST);
		this.doSubtractFirst = new Boolean(property != null ? property.getValue() : "false").booleanValue();
		PicolarioConfigTab.log.finer("doSubtractFirst = " + this.doSubtractFirst);

		property = measurement.getProperty(Picolario.DO_SUBTRACT_LAST);
		this.doSubtractLast = new Boolean(property != null ? property.getValue() : "false").booleanValue();
		PicolarioConfigTab.log.finer("doSubtractLast = " + this.doSubtractLast);

		property = measurement.getProperty(Picolario.DO_OFFSET_HEIGHT);
		this.doOffsetHeight = this.heightOffsetValue != 0 && this.doHeightAdaption;
		PicolarioConfigTab.log.finer("doOffsetHeight = " + this.doOffsetHeight);

		property = measurement.getProperty(IDevice.OFFSET);
		this.heightOffsetValue = new Double(property != null ? property.getValue() : "0.0").doubleValue();
		PicolarioConfigTab.log.finer("heightOffsetValue = " + this.heightOffsetValue);

		recordKey = this.device.getMeasurementNames(this.configName)[2]; // slope
		measurement = this.device.getMeasurement(this.configName, recordKey);
		this.slopeDataUnit = measurement.getUnit();
		PicolarioConfigTab.log.finer("slopeDataUnit = " + this.slopeDataUnit);

		PropertyType typeSelection = this.device.getMeasruementProperty(this.configName, this.device.getMeasurementNames(this.configName)[2], CalculationThread.REGRESSION_TYPE);
		if (typeSelection == null)
			this.slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
		else
			this.slopeTypeSelection = typeSelection.getValue(); // CalculationThread.REGRESSION_TYPE_*
		PicolarioConfigTab.log.finer("slopeTypeSelection = " + this.slopeTypeSelection);

		PropertyType timeSelection = this.device.getMeasruementProperty(this.configName, this.device.getMeasurementNames(this.configName)[2], CalculationThread.REGRESSION_INTERVAL_SEC);
		if (timeSelection == null)
			this.slopeTimeSelection = 4;
		else
			this.slopeTimeSelection = new Integer(timeSelection.getValue());
		PicolarioConfigTab.log.finer("slopeTimeSelection = " + this.slopeTimeSelection);
	}

	/**
	 * returns the check info as follow
	 *   1 = nichtAnpassenButton.setSelection(true);
	 *   2 = ertsenWertAbziehenButton.setSelection(true);
	 *   4 = höheVerringernButton.setSelection(true);
	 *   only 1 or 2 or 4 can returned
	 * @return integer of unit type
	 */
	public int getReduceHeightSelectionType() {
		int selection = this.noAdaptionButton.getSelection() ? 1 : 0;
		selection = selection + (this.reduceByFirstValueButton.getSelection() ? 1 : 0);
		selection = selection + (this.reduceByDefinedValueButton.getSelection() ? 1 : 0);
		return selection;
	}

	/**
	 * call this method, if reduceHeightSelectiontYpe is 4
	 * @return integer of dialog selected value
	 */
	public int getReduceHeightSelection() {
		return new Integer(this.heightOffset.getText()).intValue();
	}

	/**
	 * @return the heightOffsetValue
	 */
	public double getHeightOffsetValue() {
		return this.heightOffsetValue;
	}

	/**
	 * @return the doReduceHeight
	 */
	public boolean isDoReduceHeight() {
		return this.doOffsetHeight;
	}

	/**
	 * @return the doSubtractFirst
	 */
	public boolean isDoSubtractFirst() {
		return this.doSubtractFirst;
	}

	/**
	 * @return the doSubtractLast
	 */
	public boolean isDoSubtractLast() {
		return this.doSubtractLast;
	}

	/**
	 * @return the heightDataUnit
	 */
	public String getHeightDataUnit() {
		return this.heightDataUnit;
	}

}
