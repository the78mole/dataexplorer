/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.dialog.edit;

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.device.DataTypes;
import osde.device.DeviceConfiguration;
import osde.device.FlowControlTypes;
import osde.device.ParityTypes;
import osde.device.StopBitsTypes;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * class defining a CTabItem with SerialPortType configuration data
 * @author Winfried Brügmann
 */
public class SeriaPortTypeTabItem extends CTabItem {
	final static Logger	log								= Logger.getLogger(ChannelTypeTabItem.class.getName());

	Composite						serialPortComposite, timeOutComposite;
	Label								serialPortDescriptionLabel, timeOutDescriptionLabel;
	Label								portNameLabel, baudeRateLabel, dataBitsLabel, stopBitsLabel, parityLabel, flowControlLabel, rtsLabel, dtrLabel, timeOutLabel;
	Text								portNameText;
	CCombo							baudeRateCombo, dataBitsCombo, stopBitsCombo, parityCombo, flowControlCombo;
	Button							isRTSButton, isDTRButton, timeOutButton;
	Label								_RTOCharDelayTimeLabel, _RTOExtraDelayTimeLabel, _WTOCharDelayTimeLabel, _WTOExtraDelayTimeLabel;
	Text								_RTOCharDelayTimeText, _RTOExtraDelayTimeText, _WTOCharDelayTimeText, _WTOExtraDelayTimeText;

	String							portName					= OSDE.STRING_EMPTY;
	int									baudeRateIndex		= 0;
	int									dataBitsIndex			= 0;
	int									stopBitsIndex			= 0;
	int									parityIndex				= 0;
	int									flowControlIndex	= 0;
	boolean							isRTS							= false;
	boolean							isDTR							= false;
	boolean							useTimeOut				= false;
	int									RTOCharDelayTime	= 0;
	int									RTOExtraDelayTime	= 0;
	int									WTOCharDelayTime	= 0;
	int									WTOExtraDelayTime	= 0;
	DeviceConfiguration	deviceConfig;
	final CTabFolder		tabFolder;

	public SeriaPortTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		this.tabFolder = parent;
		SeriaPortTypeTabItem.log.log(Level.FINE, "SeriaPortTypeTabItem ");
		initGUI();
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText("Serial Port");
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			this.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					SeriaPortTypeTabItem.log.log(Level.FINEST, "this.widgetDisposed, event=" + evt);
					if (SeriaPortTypeTabItem.this.deviceConfig != null) {
						SeriaPortTypeTabItem.this.deviceConfig.removeSerialPortType();
					}
				}
			});
			{
				this.serialPortComposite = new Composite(this.tabFolder, SWT.NONE);
				this.serialPortComposite.setLayout(null);
				this.setControl(this.serialPortComposite);
				this.serialPortComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						SeriaPortTypeTabItem.log.log(Level.FINEST, "serialPortComposite.paintControl, event=" + evt);
						SeriaPortTypeTabItem.this.portNameText.setText(SeriaPortTypeTabItem.this.portName);
						SeriaPortTypeTabItem.this.baudeRateCombo.select(SeriaPortTypeTabItem.this.baudeRateIndex);
						SeriaPortTypeTabItem.this.dataBitsCombo.select(SeriaPortTypeTabItem.this.dataBitsIndex);
						SeriaPortTypeTabItem.this.stopBitsCombo.select(SeriaPortTypeTabItem.this.stopBitsIndex);
						SeriaPortTypeTabItem.this.parityCombo.select(SeriaPortTypeTabItem.this.parityIndex);
						SeriaPortTypeTabItem.this.flowControlCombo.select(SeriaPortTypeTabItem.this.flowControlIndex);
						SeriaPortTypeTabItem.this.isRTSButton.setSelection(SeriaPortTypeTabItem.this.isRTS);
						SeriaPortTypeTabItem.this.isDTRButton.setSelection(SeriaPortTypeTabItem.this.isDTR);
					}
				});
				{
					this.serialPortDescriptionLabel = new Label(this.serialPortComposite, SWT.CENTER | SWT.WRAP);
					this.serialPortDescriptionLabel
							.setText("This optional section descibes the serial port configuration.\nFor devices where the data comes from file instead through serial communication, it can be removed.");
					this.serialPortDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.serialPortDescriptionLabel.setBounds(12, 6, 602, 56);
				}
				{
					this.portNameLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.portNameLabel.setText("port name");
					this.portNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.portNameLabel.setBounds(5, 74, 100, 20);
				}
				{
					this.portNameText = new Text(this.serialPortComposite, SWT.BORDER);
					this.portNameText.setBounds(141, 76, 180, 20);
					this.portNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.portNameText.setEditable(false);
				}
				{
					this.baudeRateLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.baudeRateLabel.setText("baude rate");
					this.baudeRateLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.baudeRateLabel.setBounds(5, 99, 100, 20);
				}
				{
					this.baudeRateCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.baudeRateCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.baudeRateCombo.setItems(new String[] { "2400", "4800", "7200", "9600", "14400", "28800", "38400", "57600", "115200" });
					this.baudeRateCombo.setBounds(142, 101, 180, 20);
					this.baudeRateCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "baudeRateCombo.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setBaudeRate(new BigInteger(SeriaPortTypeTabItem.this.baudeRateCombo.getText()));
							}
							SeriaPortTypeTabItem.this.baudeRateIndex = SeriaPortTypeTabItem.this.baudeRateCombo.getSelectionIndex();
						}
					});
				}
				{
					this.dataBitsLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.dataBitsLabel.setText("data bits");
					this.dataBitsLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.dataBitsLabel.setBounds(5, 124, 100, 20);
				}
				{
					this.dataBitsCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.dataBitsCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.dataBitsCombo.setItems(new String[] { "5", "6", "7", "8" });
					this.dataBitsCombo.setBounds(142, 126, 180, 20);
					this.dataBitsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "dataBitsCombo.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setDataBits(new BigInteger(SeriaPortTypeTabItem.this.dataBitsCombo.getText()));
							}
							SeriaPortTypeTabItem.this.dataBitsIndex = SeriaPortTypeTabItem.this.dataBitsCombo.getSelectionIndex();
						}
					});
				}
				{
					this.stopBitsLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.stopBitsLabel.setText("stop bits");
					this.stopBitsLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.stopBitsLabel.setBounds(5, 149, 100, 20);
				}
				{
					this.stopBitsCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.stopBitsCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.stopBitsCombo.setItems(new String[] { "STOPBITS_1", "STOPBITS_2", "STOPBITS_1_5" });
					this.stopBitsCombo.setBounds(142, 151, 180, 20);
					this.stopBitsCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "stopBitsCombo.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setStopBits(StopBitsTypes.values()[SeriaPortTypeTabItem.this.stopBitsCombo.getSelectionIndex()]);
							}
							SeriaPortTypeTabItem.this.stopBitsIndex = SeriaPortTypeTabItem.this.stopBitsCombo.getSelectionIndex();
						}
					});
				}
				{
					this.parityLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.parityLabel.setText("parity");
					this.parityLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.parityLabel.setBounds(5, 174, 100, 20);
				}
				{
					this.parityCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.parityCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.parityCombo.setItems(new String[] { "PARITY_NONE", "PARITY_ODD", "PARITY_EVEN", "PARITY_MARK", "PARITY_SPACE" });
					this.parityCombo.setBounds(142, 176, 180, 20);
					this.parityCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "parityCombo.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setParity(ParityTypes.values()[SeriaPortTypeTabItem.this.parityCombo.getSelectionIndex()]);
							}
							SeriaPortTypeTabItem.this.parityIndex = SeriaPortTypeTabItem.this.parityCombo.getSelectionIndex();
						}
					});
				}
				{
					this.flowControlLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.flowControlLabel.setText("flow control");
					this.flowControlLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.flowControlLabel.setBounds(5, 199, 100, 20);
				}
				{
					this.flowControlCombo = new CCombo(this.serialPortComposite, SWT.BORDER);
					this.flowControlCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.flowControlCombo.setItems(new String[] { "FLOWCONTROL_NONE", "FLOWCONTROL_RTSCTS_IN", "FLOWCONTROL_RTSCTS_OUT", "FLOWCONTROL_XONXOFF_IN", "FLOWCONTROL_XONXOFF_OUT" });
					this.flowControlCombo.setBounds(142, 201, 180, 20);
					this.flowControlCombo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "flowControlCombo.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setFlowCtrlMode(FlowControlTypes.values()[SeriaPortTypeTabItem.this.flowControlCombo.getSelectionIndex()]);
							}
							SeriaPortTypeTabItem.this.flowControlIndex = SeriaPortTypeTabItem.this.flowControlCombo.getSelectionIndex();
						}
					});
				}
				{
					this.rtsLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.rtsLabel.setText(" RTS");
					this.rtsLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.rtsLabel.setBounds(5, 224, 100, 20);
				}
				{
					this.isRTSButton = new Button(this.serialPortComposite, SWT.CHECK);
					this.isRTSButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.isRTSButton.setBounds(142, 224, 180, 20);
					this.isRTSButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "isRTSButton.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setIsRTS(SeriaPortTypeTabItem.this.isRTSButton.getSelection());
							}
							SeriaPortTypeTabItem.this.isRTS = SeriaPortTypeTabItem.this.isRTSButton.getSelection();
						}
					});
				}
				{
					this.dtrLabel = new Label(this.serialPortComposite, SWT.RIGHT);
					this.dtrLabel.setText(" DTR");
					this.dtrLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.dtrLabel.setBounds(5, 249, 100, 20);
				}
				{
					this.isDTRButton = new Button(this.serialPortComposite, SWT.CHECK);
					this.isDTRButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					this.isDTRButton.setBounds(142, 249, 180, 20);
					this.isDTRButton.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "isDTRButton.widgetSelected, event=" + evt);
							if (SeriaPortTypeTabItem.this.deviceConfig != null) {
								SeriaPortTypeTabItem.this.deviceConfig.setIsDTR(SeriaPortTypeTabItem.this.isDTRButton.getSelection());
							}
							SeriaPortTypeTabItem.this.isDTR = SeriaPortTypeTabItem.this.isDTRButton.getSelection();
						}
					});
				}
				{
					this.timeOutComposite = new Composite(this.serialPortComposite, SWT.BORDER);
					this.timeOutComposite.setLayout(null);
					this.timeOutComposite.setBounds(356, 78, 250, 207);
					this.timeOutComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							SeriaPortTypeTabItem.log.log(Level.FINEST, "dialogShell.paintControl, event=" + evt);
							SeriaPortTypeTabItem.this._RTOCharDelayTimeText.setText("" + SeriaPortTypeTabItem.this.RTOCharDelayTime);
							SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.setText("" + SeriaPortTypeTabItem.this.RTOExtraDelayTime);
							SeriaPortTypeTabItem.this._WTOCharDelayTimeText.setText("" + SeriaPortTypeTabItem.this.WTOCharDelayTime);
							SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.setText("" + SeriaPortTypeTabItem.this.WTOExtraDelayTime);

							SeriaPortTypeTabItem.this.timeOutButton.setSelection(SeriaPortTypeTabItem.this.useTimeOut);
							if (SeriaPortTypeTabItem.this.timeOutButton.getSelection()) {
								SeriaPortTypeTabItem.this._RTOCharDelayTimeLabel.setEnabled(true);
								SeriaPortTypeTabItem.this._RTOCharDelayTimeText.setEnabled(true);
								SeriaPortTypeTabItem.this._RTOExtraDelayTimeLabel.setEnabled(true);
								SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.setEnabled(true);
								SeriaPortTypeTabItem.this._WTOCharDelayTimeLabel.setEnabled(true);
								SeriaPortTypeTabItem.this._WTOCharDelayTimeText.setEnabled(true);
								SeriaPortTypeTabItem.this._WTOExtraDelayTimeLabel.setEnabled(true);
								SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.setEnabled(true);
							}
							else {
								SeriaPortTypeTabItem.this._RTOCharDelayTimeLabel.setEnabled(false);
								SeriaPortTypeTabItem.this._RTOCharDelayTimeText.setEnabled(false);
								SeriaPortTypeTabItem.this._RTOExtraDelayTimeLabel.setEnabled(false);
								SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.setEnabled(false);
								SeriaPortTypeTabItem.this._WTOCharDelayTimeLabel.setEnabled(false);
								SeriaPortTypeTabItem.this._WTOCharDelayTimeText.setEnabled(false);
								SeriaPortTypeTabItem.this._WTOExtraDelayTimeLabel.setEnabled(false);
								SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.setEnabled(false);
							}
						}
					});
					{
						this.timeOutLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this.timeOutLabel.setText("specify time out");
						this.timeOutLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this.timeOutLabel.setBounds(6, 58, 140, 20);
					}
					{
						this.timeOutButton = new Button(this.timeOutComposite, SWT.CHECK);
						this.timeOutButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this.timeOutButton.setBounds(161, 56, 70, 20);
						this.timeOutButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "timeOutButton.widgetSelected, event=" + evt);
								SeriaPortTypeTabItem.this.useTimeOut = SeriaPortTypeTabItem.this.timeOutButton.getSelection();
								if (SeriaPortTypeTabItem.this.useTimeOut) {
									if (SeriaPortTypeTabItem.this.deviceConfig != null) {
										SeriaPortTypeTabItem.this.RTOCharDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getRTOCharDelayTime();
										SeriaPortTypeTabItem.this.RTOExtraDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getRTOExtraDelayTime();
										SeriaPortTypeTabItem.this.WTOCharDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getWTOCharDelayTime();
										SeriaPortTypeTabItem.this.WTOExtraDelayTime = SeriaPortTypeTabItem.this.deviceConfig.getWTOExtraDelayTime();
									}
									else {
										SeriaPortTypeTabItem.this.RTOCharDelayTime = 0;
										SeriaPortTypeTabItem.this.RTOExtraDelayTime = 0;
										SeriaPortTypeTabItem.this.WTOCharDelayTime = 0;
										SeriaPortTypeTabItem.this.WTOExtraDelayTime = 0;
									}
								}
								else {
									if (SeriaPortTypeTabItem.this.deviceConfig != null) {
										SeriaPortTypeTabItem.this.deviceConfig.removeSerialPortTimeOut();
									}
									SeriaPortTypeTabItem.this.RTOCharDelayTime = 0;
									SeriaPortTypeTabItem.this.RTOExtraDelayTime = 0;
									SeriaPortTypeTabItem.this.WTOCharDelayTime = 0;
									SeriaPortTypeTabItem.this.WTOExtraDelayTime = 0;
								}
								SeriaPortTypeTabItem.this.timeOutComposite.redraw();
							}
						});
					}
					{
						this._RTOCharDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._RTOCharDelayTimeLabel.setText("RTOCharDelayTime");
						this._RTOCharDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._RTOCharDelayTimeLabel.setBounds(6, 88, 140, 20);
					}
					{
						this._RTOCharDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._RTOCharDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._RTOCharDelayTimeText.setBounds(162, 86, 70, 20);
						this._RTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_RTOCharDelayTimeText.verifyText, event=" + evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._RTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_RTOCharDelayTimeText.keyReleased, event=" + evt);
								SeriaPortTypeTabItem.this.RTOCharDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._RTOCharDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setRTOCharDelayTime(SeriaPortTypeTabItem.this.RTOCharDelayTime);
								}
							}
						});
					}
					{
						this._RTOExtraDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._RTOExtraDelayTimeLabel.setText("RTOExtraDelayTime");
						this._RTOExtraDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._RTOExtraDelayTimeLabel.setBounds(6, 118, 140, 20);
					}
					{
						this._RTOExtraDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._RTOExtraDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._RTOExtraDelayTimeText.setBounds(162, 116, 70, 20);
						this._RTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_RTOExtraDelayTimeText.verifyText, event=" + evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._RTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_RTOExtraDelayTimeText.keyReleased, event=" + evt);
								SeriaPortTypeTabItem.this.RTOExtraDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._RTOExtraDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setRTOExtraDelayTime(SeriaPortTypeTabItem.this.RTOExtraDelayTime);
								}
							}
						});
					}
					{
						this._WTOCharDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._WTOCharDelayTimeLabel.setText("WTOCharDelayTime");
						this._WTOCharDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._WTOCharDelayTimeLabel.setBounds(6, 148, 140, 20);
					}
					{
						this._WTOCharDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._WTOCharDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._WTOCharDelayTimeText.setBounds(162, 146, 70, 20);
						this._WTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_WRTOCharDelayTimeText.verifyText, event=" + evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._WTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_WRTOCharDelayTimeText.keyReleased, event=" + evt);
								SeriaPortTypeTabItem.this.WTOCharDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._WTOCharDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setWTOCharDelayTime(SeriaPortTypeTabItem.this.WTOCharDelayTime);
								}
							}
						});
					}
					{
						this._WTOExtraDelayTimeLabel = new Label(this.timeOutComposite, SWT.RIGHT);
						this._WTOExtraDelayTimeLabel.setText("WTOExtraDelayTime");
						this._WTOExtraDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._WTOExtraDelayTimeLabel.setBounds(6, 178, 140, 20);
					}
					{
						this._WTOExtraDelayTimeText = new Text(this.timeOutComposite, SWT.BORDER);
						this._WTOExtraDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this._WTOExtraDelayTimeText.setBounds(162, 176, 70, 20);
						this._WTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_WTOExtraDelayTimeText.verifyText, event=" + evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						this._WTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								SeriaPortTypeTabItem.log.log(Level.FINEST, "_WTOExtraDelayTimeText.keyReleased, event=" + evt);
								SeriaPortTypeTabItem.this.WTOExtraDelayTime = Integer.parseInt(SeriaPortTypeTabItem.this._WTOExtraDelayTimeText.getText());
								if (SeriaPortTypeTabItem.this.deviceConfig != null) {
									SeriaPortTypeTabItem.this.deviceConfig.setWTOExtraDelayTime(SeriaPortTypeTabItem.this.WTOExtraDelayTime);
								}
							}
						});
					}
					{
						this.timeOutDescriptionLabel = new Label(this.timeOutComposite, SWT.WRAP);
						this.timeOutDescriptionLabel.setText("Time out section describes Read and Write delay time. This delay and extra delay are only required in special purpose. ");
						this.timeOutDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						this.timeOutDescriptionLabel.setBounds(6, 3, 232, 52);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param deviceConfig the deviceConfig to set
	 */
	public void setDeviceConfig(DeviceConfiguration deviceConfig) {
		this.deviceConfig = deviceConfig;

		//String tmpPortString = OSDE.IS_WINDOWS ? "COM1" : OSDE.IS_LINUX ? "/dev/ttyS0" : OSDE.IS_MAC ? "/dev/tty.usbserial" : "COMx";
		//deviceConfig.setPort(tmpPortString);
		this.portName = deviceConfig.getPort();
		this.baudeRateIndex = getSelectionIndex(this.baudeRateCombo, "" + deviceConfig.getBaudeRate());
		this.dataBitsIndex = getSelectionIndex(this.dataBitsCombo, "" + deviceConfig.getDataBits());
		this.stopBitsIndex = deviceConfig.getStopBits() - 1;
		this.parityIndex = deviceConfig.getParity();
		this.flowControlIndex = deviceConfig.getFlowCtrlMode();
		this.isRTS = deviceConfig.isRTS();
		this.isDTR = deviceConfig.isDTR();

		if (deviceConfig.getSerialPortType().getTimeOut() != null) {
			this.timeOutButton.setSelection(this.useTimeOut = true);
		}
		else {
			this.timeOutButton.setSelection(this.useTimeOut = false);
		}
		this.RTOCharDelayTime = deviceConfig.getRTOCharDelayTime();
		this.RTOExtraDelayTime = deviceConfig.getRTOExtraDelayTime();
		this.WTOCharDelayTime = deviceConfig.getWTOCharDelayTime();
		this.WTOExtraDelayTime = deviceConfig.getWTOExtraDelayTime();
		this.timeOutComposite.redraw();

		this.serialPortComposite.redraw();

	}

	/**
	 * search the index of a given string within the items of a combo box items
	 * @param useCombo
	 * @param searchString
	 * @return
	 */
	private int getSelectionIndex(CCombo useCombo, String searchString) {
		int searchIndex = 0;
		for (String item : useCombo.getItems()) {
			if (item.equals(searchString)) break;
			++searchIndex;
		}
		return searchIndex;
	}
}
