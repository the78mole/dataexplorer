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
	final static Logger						log			= Logger.getLogger(ChannelTypeTabItem.class.getName());
	
	Composite serialPortComposite, timeOutComposite;
	Label serialPortDescriptionLabel, timeOutDescriptionLabel;
	Label portNameLabel, baudeRateLabel, dataBitsLabel, stopBitsLabel, parityLabel, flowControlLabel, rtsLabel, dtrLabel, timeOutLabel;
	Text	portNameText;
	CCombo	baudeRateCombo, dataBitsCombo, stopBitsCombo, parityCombo, flowControlCombo;
	Button	isRTSButton, isDTRButton, timeOutButton;
	Label	_RTOCharDelayTimeLabel, _RTOExtraDelayTimeLabel, _WTOCharDelayTimeLabel, _WTOExtraDelayTimeLabel;
	Text	_RTOCharDelayTimeText, _RTOExtraDelayTimeText, _WTOCharDelayTimeText, _WTOExtraDelayTimeText;

	String portName = OSDE.STRING_EMPTY;
	int baudeRateIndex = 0;
	int dataBitsIndex = 0;
	int stopBitsIndex = 0;
	int parityIndex = 0;
	int flowControlIndex = 0;
	boolean isRTS = false;
	boolean isDTR = false;
	boolean useTimeOut = false;
	int RTOCharDelayTime = 0;
	int RTOExtraDelayTime = 0;
	int WTOCharDelayTime = 0;
	int WTOExtraDelayTime = 0;
	DeviceConfiguration deviceConfig;
	final CTabFolder tabFolder;


	public SeriaPortTypeTabItem(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		tabFolder = parent;
		System.out.println("SeriaPortTypeTabItem ");
		initGUI();
	}

	private void initGUI() {
		try {
			SWTResourceManager.registerResourceUser(this);
			this.setText("Serial Port");
			this.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
			this.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "this.widgetDisposed, event="+evt);
					if (deviceConfig != null) {
						deviceConfig.removeSerialPortType();
					}
				}
			});
			{
				serialPortComposite = new Composite(tabFolder, SWT.NONE);
				serialPortComposite.setLayout(null);
				this.setControl(serialPortComposite);
				serialPortComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "serialPortComposite.paintControl, event=" + evt);
						portNameText.setText(portName);
						baudeRateCombo.select(baudeRateIndex);
						dataBitsCombo.select(dataBitsIndex);
						stopBitsCombo.select(stopBitsIndex);
						parityCombo.select(parityIndex);
						flowControlCombo.select(flowControlIndex);
						isRTSButton.setSelection(isRTS);
						isDTRButton.setSelection(isDTR);
					}
				});
				{
					serialPortDescriptionLabel = new Label(serialPortComposite, SWT.CENTER | SWT.WRAP);
					serialPortDescriptionLabel.setText("This optional section descibes the serial port configuration.\nFor devices where the data comes from file instead through serial communication, it can be removed.");
					serialPortDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					serialPortDescriptionLabel.setBounds(12, 6, 602, 56);
				}
				{
					portNameLabel = new Label(serialPortComposite, SWT.RIGHT);
					portNameLabel.setText("port name");
					portNameLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					portNameLabel.setBounds(5, 74, 100, 20);
				}
				{
					portNameText = new Text(serialPortComposite, SWT.BORDER);
					portNameText.setBounds(141, 76, 180, 20);
					portNameText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					portNameText.setEditable(false);
				}
				{
					baudeRateLabel = new Label(serialPortComposite, SWT.RIGHT);
					baudeRateLabel.setText("baude rate");
					baudeRateLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					baudeRateLabel.setBounds(5, 99, 100, 20);
				}
				{
					baudeRateCombo = new CCombo(serialPortComposite, SWT.BORDER);
					baudeRateCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					baudeRateCombo.setItems(new String[] { "2400", "4800", "7200", "9600", "14400", "28800", "38400", "57600", "115200" });
					baudeRateCombo.setBounds(142, 101, 180, 20);
					baudeRateCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "baudeRateCombo.widgetSelected, event=" + evt);
							if (deviceConfig != null) {
								deviceConfig.setBaudeRate(new BigInteger(baudeRateCombo.getText()));
							}
							baudeRateIndex = baudeRateCombo.getSelectionIndex();
						}
					});
				}
				{
					dataBitsLabel = new Label(serialPortComposite, SWT.RIGHT);
					dataBitsLabel.setText("data bits");
					dataBitsLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					dataBitsLabel.setBounds(5, 124, 100, 20);
				}
				{
					dataBitsCombo = new CCombo(serialPortComposite, SWT.BORDER);
					dataBitsCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					dataBitsCombo.setItems(new String[] {"5", "6", "7", "8"});
					dataBitsCombo.setBounds(142, 126, 180, 20);
					dataBitsCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "dataBitsCombo.widgetSelected, event="+evt);
							if (deviceConfig != null) {
								deviceConfig.setDataBits(new BigInteger(dataBitsCombo.getText()));
							}
							dataBitsIndex = dataBitsCombo.getSelectionIndex();
						}
					});
				}
				{
					stopBitsLabel = new Label(serialPortComposite, SWT.RIGHT);
					stopBitsLabel.setText("stop bits");
					stopBitsLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					stopBitsLabel.setBounds(5, 149, 100, 20);
				}
				{
					stopBitsCombo = new CCombo(serialPortComposite, SWT.BORDER);
					stopBitsCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					stopBitsCombo.setItems(new String[] { "STOPBITS_1", "STOPBITS_2", "STOPBITS_1_5" });
					stopBitsCombo.setBounds(142, 151, 180, 20);
					stopBitsCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "stopBitsCombo.widgetSelected, event="+evt);
							if (deviceConfig != null) {
								deviceConfig.setStopBits(StopBitsTypes.values()[stopBitsCombo.getSelectionIndex()]);
							}
							stopBitsIndex = stopBitsCombo.getSelectionIndex();
						}
					});
				}
				{
					parityLabel = new Label(serialPortComposite, SWT.RIGHT);
					parityLabel.setText("parity");
					parityLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					parityLabel.setBounds(5, 174, 100, 20);
				}
				{
					parityCombo = new CCombo(serialPortComposite, SWT.BORDER);
					parityCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					parityCombo.setItems(new String[] { "PARITY_NONE", "PARITY_ODD", "PARITY_EVEN", "PARITY_MARK", "PARITY_SPACE" });
					parityCombo.setBounds(142, 176, 180, 20);
					parityCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "parityCombo.widgetSelected, event="+evt);
							if (deviceConfig != null) {
								deviceConfig.setParity(ParityTypes.values()[parityCombo.getSelectionIndex()]);
							}
							parityIndex = parityCombo.getSelectionIndex();
						}
					});
				}
				{
					flowControlLabel = new Label(serialPortComposite, SWT.RIGHT);
					flowControlLabel.setText("flow control");
					flowControlLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					flowControlLabel.setBounds(5, 199, 100, 20);
				}
				{
					flowControlCombo = new CCombo(serialPortComposite, SWT.BORDER);
					flowControlCombo.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					flowControlCombo.setItems(new String[] { "FLOWCONTROL_NONE", "FLOWCONTROL_RTSCTS_IN", "FLOWCONTROL_RTSCTS_OUT", "FLOWCONTROL_XONXOFF_IN", "FLOWCONTROL_XONXOFF_OUT" });
					flowControlCombo.setBounds(142, 201, 180, 20);
					flowControlCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "flowControlCombo.widgetSelected, event="+evt);
							if (deviceConfig != null) {
								deviceConfig.setFlowCtrlMode(FlowControlTypes.values()[flowControlCombo.getSelectionIndex()]);
							}
							flowControlIndex = flowControlCombo.getSelectionIndex();
						}
					});
				}
				{
					rtsLabel = new Label(serialPortComposite, SWT.RIGHT);
					rtsLabel.setText(" RTS");
					rtsLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					rtsLabel.setBounds(5, 224, 100, 20);
				}
				{
					isRTSButton = new Button(serialPortComposite, SWT.CHECK);
					isRTSButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					isRTSButton.setBounds(142, 224, 180, 20);
					isRTSButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "isRTSButton.widgetSelected, event="+evt);
							if (deviceConfig != null) {
								deviceConfig.setIsRTS(isRTSButton.getSelection());
							}
							isRTS = isRTSButton.getSelection();
						}
					});
				}
				{
					dtrLabel = new Label(serialPortComposite, SWT.RIGHT);
					dtrLabel.setText(" DTR");
					dtrLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					dtrLabel.setBounds(5, 249, 100, 20);
				}
				{
					isDTRButton = new Button(serialPortComposite, SWT.CHECK);
					isDTRButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
					isDTRButton.setBounds(142, 249, 180, 20);
					isDTRButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "isDTRButton.widgetSelected, event="+evt);
							if (deviceConfig != null) {
								deviceConfig.setIsDTR(isDTRButton.getSelection());
							}
							isDTR = isDTRButton.getSelection();
						}
					});
				}
				{
					timeOutComposite = new Composite(serialPortComposite, SWT.BORDER);
					timeOutComposite.setLayout(null);
					timeOutComposite.setBounds(356, 78, 250, 207);
					timeOutComposite.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "dialogShell.paintControl, event="+evt);
							_RTOCharDelayTimeText.setText(""+RTOCharDelayTime);
							_RTOExtraDelayTimeText.setText(""+RTOExtraDelayTime);
							_WTOCharDelayTimeText.setText(""+WTOCharDelayTime);
							_WTOExtraDelayTimeText.setText(""+WTOExtraDelayTime);
							
							timeOutButton.setSelection(useTimeOut);
							if (timeOutButton.getSelection()) {
								_RTOCharDelayTimeLabel.setEnabled(true);
								_RTOCharDelayTimeText.setEnabled(true);
								_RTOExtraDelayTimeLabel.setEnabled(true);
								_RTOExtraDelayTimeText.setEnabled(true);
								_WTOCharDelayTimeLabel.setEnabled(true);
								_WTOCharDelayTimeText.setEnabled(true);
								_WTOExtraDelayTimeLabel.setEnabled(true);
								_WTOExtraDelayTimeText.setEnabled(true);
							}
							else {
								_RTOCharDelayTimeLabel.setEnabled(false);
								_RTOCharDelayTimeText.setEnabled(false);
								_RTOExtraDelayTimeLabel.setEnabled(false);
								_RTOExtraDelayTimeText.setEnabled(false);
								_WTOCharDelayTimeLabel.setEnabled(false);
								_WTOCharDelayTimeText.setEnabled(false);
								_WTOExtraDelayTimeLabel.setEnabled(false);
								_WTOExtraDelayTimeText.setEnabled(false);
							}
						}
					});
					{
						timeOutLabel = new Label(timeOutComposite, SWT.RIGHT);
						timeOutLabel.setText("specify time out");
						timeOutLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						timeOutLabel.setBounds(6, 58, 140, 20);
					}
					{
						timeOutButton = new Button(timeOutComposite, SWT.CHECK);
						timeOutButton.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						timeOutButton.setBounds(161, 56, 70, 20);
						timeOutButton.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "timeOutButton.widgetSelected, event="+evt);
								useTimeOut = timeOutButton.getSelection();
								if (useTimeOut) {
									if (deviceConfig != null) {
										RTOCharDelayTime = deviceConfig.getRTOCharDelayTime();
										RTOExtraDelayTime = deviceConfig.getRTOExtraDelayTime();
										WTOCharDelayTime = deviceConfig.getWTOCharDelayTime();
										WTOExtraDelayTime = deviceConfig.getWTOExtraDelayTime();
									}
									else {
										RTOCharDelayTime = 0;
										RTOExtraDelayTime = 0;
										WTOCharDelayTime = 0;
										WTOExtraDelayTime = 0;
									}
								}
								else {
									if (deviceConfig != null) {
										deviceConfig.removeSerialPortTimeOut();
									}
										RTOCharDelayTime = 0;
										RTOExtraDelayTime = 0;
										WTOCharDelayTime = 0;
										WTOExtraDelayTime = 0;
								}
								timeOutComposite.redraw();
							}
						});
					}
					{
						_RTOCharDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
						_RTOCharDelayTimeLabel.setText("RTOCharDelayTime");
						_RTOCharDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_RTOCharDelayTimeLabel.setBounds(6, 88, 140, 20);
					}
					{
						_RTOCharDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
						_RTOCharDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_RTOCharDelayTimeText.setBounds(162, 86, 70, 20);
						_RTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(Level.FINEST, "_RTOCharDelayTimeText.verifyText, event="+evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						_RTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "_RTOCharDelayTimeText.keyReleased, event="+evt);
								RTOCharDelayTime = Integer.parseInt(_RTOCharDelayTimeText.getText());
								if(deviceConfig != null) {
									deviceConfig.setRTOCharDelayTime(RTOCharDelayTime);
								}
							}
						});
					}
					{
						_RTOExtraDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
						_RTOExtraDelayTimeLabel.setText("RTOExtraDelayTime");
						_RTOExtraDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_RTOExtraDelayTimeLabel.setBounds(6, 118, 140, 20);
					}
					{
						_RTOExtraDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
						_RTOExtraDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_RTOExtraDelayTimeText.setBounds(162, 116, 70, 20);
						_RTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(Level.FINEST, "_RTOExtraDelayTimeText.verifyText, event="+evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						_RTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "_RTOExtraDelayTimeText.keyReleased, event="+evt);
								RTOExtraDelayTime = Integer.parseInt(_RTOExtraDelayTimeText.getText());
								if(deviceConfig != null) {
									deviceConfig.setRTOExtraDelayTime(RTOExtraDelayTime);
								}
							}
						});
					}
					{
						_WTOCharDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
						_WTOCharDelayTimeLabel.setText("WTOCharDelayTime");
						_WTOCharDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_WTOCharDelayTimeLabel.setBounds(6, 148, 140, 20);
					}
					{
						_WTOCharDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
						_WTOCharDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_WTOCharDelayTimeText.setBounds(162, 146, 70, 20);
						_WTOCharDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(Level.FINEST, "_WRTOCharDelayTimeText.verifyText, event="+evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						_WTOCharDelayTimeText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "_WRTOCharDelayTimeText.keyReleased, event="+evt);
								WTOCharDelayTime = Integer.parseInt(_WTOCharDelayTimeText.getText());
								if(deviceConfig != null) {
									deviceConfig.setWTOCharDelayTime(WTOCharDelayTime);
								}
							}
						});
					}
					{
						_WTOExtraDelayTimeLabel = new Label(timeOutComposite, SWT.RIGHT);
						_WTOExtraDelayTimeLabel.setText("WTOExtraDelayTime");
						_WTOExtraDelayTimeLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_WTOExtraDelayTimeLabel.setBounds(6, 178, 140, 20);
					}
					{
						_WTOExtraDelayTimeText = new Text(timeOutComposite, SWT.BORDER);
						_WTOExtraDelayTimeText.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						_WTOExtraDelayTimeText.setBounds(162, 176, 70, 20);
						_WTOExtraDelayTimeText.addVerifyListener(new VerifyListener() {
							public void verifyText(VerifyEvent evt) {
								log.log(Level.FINEST, "_WTOExtraDelayTimeText.verifyText, event="+evt);
								evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
							}
						});
						_WTOExtraDelayTimeText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "_WTOExtraDelayTimeText.keyReleased, event="+evt);
								WTOExtraDelayTime = Integer.parseInt(_WTOExtraDelayTimeText.getText());
								if(deviceConfig != null) {
									deviceConfig.setWTOExtraDelayTime(WTOExtraDelayTime);
								}
							}
						});
					}
					{
						timeOutDescriptionLabel = new Label(timeOutComposite, SWT.WRAP);
						timeOutDescriptionLabel.setText("Time out section describes Read and Write delay time. This delay and extra delay are only required in special purpose. ");
						timeOutDescriptionLabel.setFont(SWTResourceManager.getFont(DevicePropertiesEditor.widgetFontName, DevicePropertiesEditor.widgetFontSize, SWT.NORMAL));
						timeOutDescriptionLabel.setBounds(6, 3, 232, 52);
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
		portName = deviceConfig.getPort();
		baudeRateIndex = getSelectionIndex(baudeRateCombo, "" + deviceConfig.getBaudeRate());
		dataBitsIndex = getSelectionIndex(dataBitsCombo, "" + deviceConfig.getDataBits());
		stopBitsIndex = deviceConfig.getStopBits() - 1;
		parityIndex = deviceConfig.getParity();
		flowControlIndex = deviceConfig.getFlowCtrlMode();
		isRTS = deviceConfig.isRTS();
		isDTR = deviceConfig.isDTR();
		
		if(deviceConfig.getSerialPortType().getTimeOut() != null) {
			timeOutButton.setSelection(useTimeOut = true);
		}
		else {
			timeOutButton.setSelection(useTimeOut = false);
		}
		RTOCharDelayTime = deviceConfig.getRTOCharDelayTime();
		RTOExtraDelayTime = deviceConfig.getRTOExtraDelayTime();
		WTOCharDelayTime = deviceConfig.getWTOCharDelayTime();
		WTOExtraDelayTime = deviceConfig.getWTOExtraDelayTime();
		timeOutComposite.redraw();

		serialPortComposite.redraw();
	
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
