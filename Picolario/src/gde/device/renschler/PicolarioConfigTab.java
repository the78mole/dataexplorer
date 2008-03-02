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
import osde.device.MeasurementType;
import osde.device.PropertyType;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CalculationThread;


/**
* This code was edited or generated using CloudGarden's Jigloo
* SWT/Swing GUI Builder, which is free for non-commercial
* use. If Jigloo is being used commercially (ie, by a corporation,
* company or business for any purpose whatever) then you
* should purchase a license for each developer using Jigloo.
* Please visit www.cloudgarden.com for details.
* Use of Jigloo implies acceptance of these licensing terms.
* A COMMERCIAL LICENSE HAS NOT BEEN PURCHASED FOR
* THIS MACHINE, SO JIGLOO OR THIS CODE CANNOT BE USED
* LEGALLY FOR ANY CORPORATE OR COMMERCIAL PURPOSE.
*/
public class PicolarioConfigTab extends Composite {
	{
		SWTResourceManager.registerResourceUser(this);
	}

	private Logger												log										= Logger.getLogger(this.getClass().getName());

	private Group													heightAdaptionGroup;
	private Label													heightLabel;
	private Button												noAdaptionButton;
	private Button												reduceByDefinedValueButton;
	private Button												reduceByFirstValueButton;
	private Button												reduceByLastValueButton;
	private CCombo												heightOffset;
	private CLabel slopeUnit;
	private CLabel slopeName;
	private CLabel heightUnit;
	private CLabel												calculationTypeLabel;
	private CCombo												slopeCalculationTypeCombo;
	private CCombo												regressionTime;
	
	private String												heightDataUnit				= "m";																					// Meter is default
	private boolean												doNoAdation						= false;
	private boolean												doHeightAdaption			= false;																				// indicates to adapt height values
	private boolean												doSubtractFirst				= true;																					// indicates to subtract first values from all other
	private boolean												doSubtractLast				= false;																				// indicates to subtract last values from all other
	private boolean												doOffsetHeight				= false;																				// indicates that the height has to be corrected by an offset
	private int														heightOffsetSelection	= 7;																						// represents the offset the measurment should be modified
	private double												heightOffsetValue			= 100;																					// represents the offset value
	private String												slopeDataUnit					= "m/s";																					// Meter is default
	private String												slopeTypeSelection		= CalculationThread.REGRESSION_TYPE_CURVE;
	private int														slopeTimeSelection;

	private final Picolario								device;																															// get device specific things, get serial port, ...
	private final OpenSerialDataExplorer	application;
	private String												configName;																													// tabName

	/**
	 * panel tab describing a configuration
	 * @param parent
	 * @param device
	 * @param tabName
	 */
	public PicolarioConfigTab(Composite parent, Picolario device, String tabName) {
		super(parent, SWT.NONE);
		this.device = device;
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
					heightAdaptionGroup = new Group(this, SWT.NONE);
					heightAdaptionGroup.setLayout(null);
					heightAdaptionGroup.setText("2. Berechnungseinstellung für Höhe und Steigung");
					heightAdaptionGroup.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.finest("heightAdaptionGroup2.paintControl, event="+evt);
							heightUnit.setText("[" + heightDataUnit + "]");
							noAdaptionButton.setSelection(doNoAdation);
							reduceByFirstValueButton.setSelection(doSubtractFirst);
							reduceByLastValueButton.setSelection(doSubtractLast);
							reduceByDefinedValueButton.setSelection(doOffsetHeight);
							heightOffset.setText(new Double(heightOffsetValue).toString());
							
							slopeUnit.setText("[" + slopeDataUnit + "]");
							regressionTime.select(slopeTimeSelection - 1);
							slopeCalculationTypeCombo.select(slopeTypeSelection.equals(CalculationThread.REGRESSION_TYPE_CURVE) ? 1 : 0);
						}
					});
					{
						noAdaptionButton = new Button(heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						noAdaptionButton.setText("Höhenwerte nicht anpassen");
						noAdaptionButton.setBounds(12, 40, 186, 16);
						noAdaptionButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("noAdaptioButton.widgetSelected, event=" + evt);
								noAdaptionButton.setSelection(true);
								reduceByFirstValueButton.setSelection(false);
								reduceByLastValueButton.setSelection(false);
								reduceByDefinedValueButton.setSelection(false);
								doNoAdation = true;
								doSubtractFirst = false;
								doSubtractLast = false;
								doOffsetHeight = false;
															
								String measurementKey = device.getMeasurementNames(configName)[1]; // height
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN, doNoAdation);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, doSubtractFirst);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN, doSubtractLast);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN, doOffsetHeight);
								heightOffsetValue = 0.0;
								heightOffset.setText(new Double(heightOffsetValue).toString());
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.OFFSET, DataTypes.DOUBLE, heightOffsetValue);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								application.updateGraphicsWindow();
							}
						});
					}
					{
						reduceByFirstValueButton = new Button(heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						reduceByFirstValueButton.setText("ersten Höhenwert von den folgenden abziehen");
						reduceByFirstValueButton.setSelection(doSubtractFirst);
						reduceByFirstValueButton.setBounds(12, 61, 297, 16);
						reduceByFirstValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("reduceByFirstValueButton.widgetSelected, event=" + evt);
								noAdaptionButton.setSelection(false);
								reduceByFirstValueButton.setSelection(true);
								reduceByLastValueButton.setSelection(false);
								reduceByDefinedValueButton.setSelection(false);
								doNoAdation = false;
								doSubtractFirst = true;
								doSubtractLast = false;
								doOffsetHeight = false;
								
								String measurementKey = device.getMeasurementNames(configName)[1]; // height
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN, false);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, true);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN, false);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN, false);
								heightOffsetValue = 0.0;
								heightOffset.setText(new Double(heightOffsetValue).toString());
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.OFFSET, DataTypes.DOUBLE, heightOffsetValue);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								application.updateGraphicsWindow();
							}
						});
					}
					{
						reduceByLastValueButton = new Button(heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						reduceByLastValueButton.setBounds(12, 80, 293, 18);
						reduceByLastValueButton.setText("letzten Höhenwert von allen anderen abziehen");
						reduceByLastValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("reduceByLastValueButton.widgetSelected, event=" + evt);
								noAdaptionButton.setSelection(false);
								reduceByFirstValueButton.setSelection(false);
								reduceByLastValueButton.setSelection(true);
								reduceByDefinedValueButton.setSelection(false);
								doNoAdation = false;
								doSubtractFirst = false;
								doSubtractLast = true;
								doOffsetHeight = false;
															
								String measurementKey = device.getMeasurementNames(configName)[1]; // height
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN, doNoAdation);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, doSubtractFirst);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN, doSubtractLast);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN, doOffsetHeight);
								heightOffsetValue = 0.0;
								heightOffset.setText(new Double(heightOffsetValue).toString());
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.OFFSET, DataTypes.DOUBLE, heightOffsetValue);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								application.updateGraphicsWindow();
							}
						});
					}
					{
						reduceByDefinedValueButton = new Button(heightAdaptionGroup, SWT.RADIO | SWT.LEFT);
						reduceByDefinedValueButton.setText("Offset Höhe");
						reduceByDefinedValueButton.setSelection(doOffsetHeight);
						reduceByDefinedValueButton.setBounds(12, 103, 143, 16);
						reduceByDefinedValueButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("reduceByDefinedValueButton.widgetSelected, event=" + evt);
								noAdaptionButton.setSelection(false);
								reduceByFirstValueButton.setSelection(false);
								reduceByLastValueButton.setSelection(false);
								reduceByDefinedValueButton.setSelection(true);
								doNoAdation = false;
								doOffsetHeight = true;
								doSubtractFirst = false;
								doSubtractLast = false;
								heightOffsetValue = new Double(heightOffset.getText()).doubleValue();
								
								String measurementKey = device.getMeasurementNames(configName)[1]; // height
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_NO_ADAPTION, DataTypes.BOOLEAN, doNoAdation);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_FIRST, DataTypes.BOOLEAN, doSubtractFirst);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_SUBTRACT_LAST, DataTypes.BOOLEAN, doSubtractLast);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.DO_OFFSET_HEIGHT, DataTypes.BOOLEAN, doOffsetHeight);
								device.setMeasurementPropertyValue(configName, measurementKey, Picolario.OFFSET, DataTypes.DOUBLE, heightOffsetValue);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								application.updateGraphicsWindow();
							}
						});
					}
					{
						heightOffset = new CCombo(heightAdaptionGroup, SWT.BORDER);
						final String[] heightOffsetValues = new String[] { "200", "100", "50", "0", "-50", "-100", "-150", "-200", "-250", "-300", "-400", "-500", "-750", "-1000", "-1500" };
						heightOffset.setItems(heightOffsetValues);
						heightOffset.setText(new Double(heightOffsetValue).toString());
						for (int i = 0; i < heightOffsetValues.length; i++) {
							if (heightOffsetValues.equals(heightOffsetValue)) heightOffset.select(heightOffsetSelection);
						}
						heightOffset.setBounds(184, 101, 116, 21);
						heightOffset.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("heightOffset.widgetSelected, event=" + evt);
								heightOffsetValue = new Double(heightOffsetValues[heightOffset.getSelectionIndex()]).doubleValue();
								
								String measurementKey = device.getMeasurementNames(configName)[1]; // height
								MeasurementType measurement = device.getMeasurement(configName, measurementKey);
								measurement.setOffset(heightOffsetValue);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								application.updateGraphicsWindow();
							}
						});
						heightOffset.addKeyListener(new KeyAdapter() {
							public void keyPressed(KeyEvent evt) {
								log.finest("heightOffset.keyPressed, event=" + evt);
								if (evt.character == SWT.CR) {
									//heightOffsetSelection 
									try {
										heightOffsetValue = new Double(heightOffset.getText().replace(',', '.')).doubleValue();
										
										String measurementKey = device.getMeasurementNames(configName)[1]; // height
										MeasurementType measurement = device.getMeasurement(configName, measurementKey);
										measurement.setOffset(heightOffsetValue);
										device.setChangePropery(true);
										device.storeDeviceProperties();
										application.updateGraphicsWindow();
									}
									catch (NumberFormatException e) {
										log.log(Level.WARNING, e.getMessage(), e);
										application.openMessageDialog("Eingabefehler : " + e.getMessage());
									}
								}
							}
						});
					}
					{
						heightLabel = new Label(heightAdaptionGroup, SWT.NONE);
						heightLabel.setBounds(12, 21, 76, 20);
						heightLabel.setText("Höhe");
					}
					{
						heightUnit = new CLabel(heightAdaptionGroup, SWT.NONE);
						heightUnit.setBounds(90, 16, 60, 20);
						heightUnit.setText("[m]");
					}
					{
						calculationTypeLabel = new CLabel(heightAdaptionGroup, SWT.NONE);
						calculationTypeLabel.setBounds(33, 151, 87, 20);
						calculationTypeLabel.setText("Berechnung");
					}
					{
						slopeName = new CLabel(heightAdaptionGroup, SWT.NONE);
						slopeName.setBounds(12, 127, 76, 20);
						slopeName.setText("Steigung");
					}
					{
						slopeCalculationTypeCombo = new CCombo(heightAdaptionGroup, SWT.BORDER);
						slopeCalculationTypeCombo.setBounds(140, 151, 97, 20);
						slopeCalculationTypeCombo.setItems(new String[] { " " + CalculationThread.REGRESSION_TYPE_LINEAR, " " + CalculationThread.REGRESSION_TYPE_CURVE });
						slopeCalculationTypeCombo.setToolTipText("Hier den Berechnungstyp einstellen");
						slopeCalculationTypeCombo.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST)) log.finest("slopeCalculationTypeCombo.widgetSelected, event=" + evt);
								if (slopeCalculationTypeCombo.getSelectionIndex() == 1)
									slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
								else
									slopeTypeSelection = CalculationThread.REGRESSION_TYPE_LINEAR;
								
								String measurementKey = device.getMeasurementNames(configName)[2]; // slope
								device.setMeasurementPropertyValue(configName, measurementKey, CalculationThread.REGRESSION_TYPE, DataTypes.INTEGER, slopeTypeSelection);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
								activeRecordSet.get(measurementKey).setDisplayable(false);
								device.makeInActiveDisplayable(activeRecordSet);
							}
						});
					}
					{
						regressionTime = new CCombo(heightAdaptionGroup, SWT.BORDER);
						regressionTime.setBounds(239, 151, 61, 20);
						regressionTime.setItems(new String[] { " 1 s", " 2 s", " 3 s", " 4 s", " 5 s", " 6 s", " 7 s", " 8 s", " 9 s", "10 s", "11 s", "12 s", "13 s", "14 s", "15 s", "16 s", "17 s", "18 s",
								"19 s", "20 s" });
						regressionTime.setToolTipText("Hier die Regressionszeit in Sekunden einstellen");
						regressionTime.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								if (log.isLoggable(Level.FINEST)) log.finest("regressionTime.widgetSelected, event=" + evt);
								slopeTimeSelection = regressionTime.getSelectionIndex() + 1;
								
								String measurementKey = device.getMeasurementNames(configName)[2]; // slope
								device.setMeasurementPropertyValue(configName, measurementKey, CalculationThread.REGRESSION_INTERVAL_SEC, DataTypes.INTEGER, slopeTimeSelection);
								device.setChangePropery(true);
								device.storeDeviceProperties();
								Channel activeChannel = Channels.getInstance().getActiveChannel();
								if (activeChannel != null) {
									RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
									if (activeRecordSet != null) {
										activeRecordSet.get(measurementKey).setDisplayable(false);
										device.makeInActiveDisplayable(activeRecordSet);
									}
								}
							}
						});
					}
					{
						slopeUnit = new CLabel(heightAdaptionGroup, SWT.NONE);
						slopeUnit.setBounds(90, 130, 60, 20);
						slopeUnit.setText("[m/s]");
					}
				}

			}
			this.layout();
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * retrieve initial values from device properties file for editable fields
	 */
	private void initEditable() {
		String recordKey = device.getMeasurementNames(configName)[1]; // height
		MeasurementType measurement = device.getMeasurement(configName, recordKey);
		
		heightDataUnit = measurement.getUnit();
		log.finer("heightDataUnit = " + heightDataUnit);
		
		PropertyType property = measurement.getProperty(Picolario.DO_NO_ADAPTION);
		doNoAdation = new Boolean(property != null ? property.getValue() : "false").booleanValue();
		log.finer("doHeightAdaption = " + doHeightAdaption);
		
		property = measurement.getProperty(Picolario.DO_SUBTRACT_FIRST);
		doSubtractFirst = new Boolean(property != null ? property.getValue() : "false").booleanValue();
		log.finer("doSubtractFirst = " + doSubtractFirst);
		
		property = measurement.getProperty(Picolario.DO_SUBTRACT_LAST);
		doSubtractLast = new Boolean(property != null ? property.getValue() : "false").booleanValue();
		log.finer("doSubtractLast = " + doSubtractLast);
		
		property = measurement.getProperty(Picolario.DO_OFFSET_HEIGHT);
		doOffsetHeight = heightOffsetValue != 0 && doHeightAdaption;
		log.finer("doOffsetHeight = " + doOffsetHeight);
	
		property = measurement.getProperty(Picolario.OFFSET);
		heightOffsetValue = new Double(property != null ? property.getValue() : "0.0").doubleValue();
		log.finer("heightOffsetValue = " + heightOffsetValue);
		
		recordKey = device.getMeasurementNames(configName)[2]; // slope
		measurement = device.getMeasurement(configName, recordKey);
		slopeDataUnit = measurement.getUnit();
		log.finer("slopeDataUnit = " + slopeDataUnit);
		
		PropertyType typeSelection = device.getMeasruementProperty(configName, device.getMeasurementNames(configName)[2], CalculationThread.REGRESSION_TYPE);
		if (typeSelection == null)
			slopeTypeSelection = CalculationThread.REGRESSION_TYPE_CURVE;
		else
			slopeTypeSelection = typeSelection.getValue(); // CalculationThread.REGRESSION_TYPE_*
		log.finer("slopeTypeSelection = " + slopeTypeSelection);
		
		PropertyType timeSelection = device.getMeasruementProperty(configName, device.getMeasurementNames(configName)[2], CalculationThread.REGRESSION_INTERVAL_SEC);
		if (timeSelection == null)
			slopeTimeSelection = 4;
		else
			slopeTimeSelection = new Integer(timeSelection.getValue());
		log.finer("slopeTimeSelection = " + slopeTimeSelection);
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
		int selection = noAdaptionButton.getSelection() ? 1 : 0;
		selection = selection + (reduceByFirstValueButton.getSelection() ? 1 : 0);
		selection = selection + (reduceByDefinedValueButton.getSelection() ? 1 : 0);
		return selection;
	}

	/**
	 * call this method, if reduceHeightSelectiontYpe is 4
	 * @return integer of dialog selected value
	 */
	public int getReduceHeightSelection() {
		return new Integer(heightOffset.getText()).intValue();
	}

	/**
	 * @return the heightOffsetValue
	 */
	public double getHeightOffsetValue() {
		return heightOffsetValue;
	}

	/**
	 * @return the doReduceHeight
	 */
	public boolean isDoReduceHeight() {
		return doOffsetHeight;
	}

	/**
	 * @return the doSubtractFirst
	 */
	public boolean isDoSubtractFirst() {
		return doSubtractFirst;
	}

	/**
	 * @return the doSubtractLast
	 */
	public boolean isDoSubtractLast() {
		return doSubtractLast;
	}

	/**
	 * @return the heightDataUnit
	 */
	public String getHeightDataUnit() {
		return heightDataUnit;
	}

}
