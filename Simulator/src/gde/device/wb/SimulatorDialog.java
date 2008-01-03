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
package osde.device.wb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.device.DeviceDialog;
import osde.device.DeviceSerialPort;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Dialog sample for the simulator device 
 * @author Winfried Brügmann
 */
public class SimulatorDialog extends DeviceDialog {
	private Logger												log										= Logger.getLogger(this.getClass().getName());
	private CLabel currentLabel;
	private CCombo currentCombo;
	private CLabel voltageLabel;
	private CCombo voltageCombo;
	private CLabel timeLabel;
	private CCombo timeCombo;

	private Button												stopButton;

	private final Simulator								device;																															// get device specific things, get serial port, ...
	private Text													description;
	private CLabel												descriptionLabel;
	private Button												startButton;
	private final DeviceSerialPort				serialPort;																													// open/close port execute getData()....
	private final OpenSerialDataExplorer	application;																													// interaction with application instance
	private final Channels								channels;																														// interaction with channels, source of all records

	private Channel												channel;
	private Timer													timer;
	private CLabel timeResultLabel;
	private CLabel timesLabel;
	private CCombo clusterCombo;
	private CLabel clusterLabel;
	private TimerTask											timerTask;
	private boolean												isCollectDataStopped	= false;
	private int														recordNumber					= 0;

	/**
	* main method to test this dialog inside a shell 
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			Simulator device = new Simulator("c:\\Documents and Settings\\user\\Application Data\\OpenSerialDataExplorer\\Devices\\Htronic Akkumaster C4.ini");
			SimulatorDialog inst = new SimulatorDialog(shell, device);
			inst.open();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * default constructor initialize all variables required
	 * @param parent Shell
	 * @param device device specific class implementation
	 */
	public SimulatorDialog(Shell parent, Simulator device) {
		super(parent);
		this.serialPort = device.getSerialPort();
		this.device = device;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	public void open() {
		try {
			log.fine("dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed()));
			if (dialogShell == null || dialogShell.isDisposed()) {
				dialogShell = new Shell(new Shell(SWT.MODELESS), SWT.DIALOG_TRIM);
				SWTResourceManager.registerResourceUser(dialogShell);
				dialogShell.setLayout(new FormLayout());
				dialogShell.layout();
				dialogShell.pack();
				dialogShell.setSize(336, 393);
				dialogShell.setText("Simulator");
				dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
				{
					FormData timeResultLabelLData = new FormData();
					timeResultLabelLData.width = 94;
					timeResultLabelLData.height = 22;
					timeResultLabelLData.left =  new FormAttachment(0, 1000, 213);
					timeResultLabelLData.top =  new FormAttachment(0, 1000, 111);
					timeResultLabel = new CLabel(dialogShell, SWT.NONE);
					timeResultLabel.setLayoutData(timeResultLabelLData);
					timeResultLabel.setText("=  Zeit pro Takt");
				}
				{
					FormData timesLabelLData = new FormData();
					timesLabelLData.width = 13;
					timesLabelLData.height = 22;
					timesLabelLData.left =  new FormAttachment(0, 1000, 106);
					timesLabelLData.top =  new FormAttachment(0, 1000, 111);
					timesLabel = new CLabel(dialogShell, SWT.NONE);
					timesLabel.setLayoutData(timesLabelLData);
					timesLabel.setText("*");
				}
				{
					FormData clusterComboLData = new FormData();
					clusterComboLData.width = 84;
					clusterComboLData.height = 17;
					clusterComboLData.left =  new FormAttachment(0, 1000, 125);
					clusterComboLData.top =  new FormAttachment(0, 1000, 138);
					clusterCombo = new CCombo(dialogShell, SWT.NONE);
					clusterCombo.setLayoutData(clusterComboLData);
					clusterCombo.setItems(new String[] {"10", "20", "50", "100" });
					clusterCombo.setText(new Integer(device.getDataBlockSize()).toString());
					clusterCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							device.setDataBlockSize(new Integer(clusterCombo.getText()).intValue());
							log.fine(" new clusterSize = " + device.getDataBlockSize());
						}
					});
				}
				{
					FormData clusterLabelLData = new FormData();
					clusterLabelLData.width = 82;
					clusterLabelLData.height = 22;
					clusterLabelLData.left =  new FormAttachment(0, 1000, 125);
					clusterLabelLData.top =  new FormAttachment(0, 1000, 111);
					clusterLabel = new CLabel(dialogShell, SWT.NONE);
					clusterLabel.setLayoutData(clusterLabelLData);
					clusterLabel.setText("Datenpunkte");
				}
				{
					FormData currentLabelLData = new FormData();
					currentLabelLData.width = 70;
					currentLabelLData.height = 22;
					currentLabelLData.left =  new FormAttachment(0, 1000, 29);
					currentLabelLData.top =  new FormAttachment(0, 1000, 221);
					currentLabel = new CLabel(dialogShell, SWT.CENTER | SWT.EMBEDDED);
					currentLabel.setLayoutData(currentLabelLData);
					currentLabel.setText("Strom");
				}
				{
					FormData currentComboLData = new FormData();
					currentComboLData.width = 84;
					currentComboLData.height = 17;
					currentComboLData.left =  new FormAttachment(0, 1000, 29);
					currentComboLData.top =  new FormAttachment(0, 1000, 244);
					currentCombo = new CCombo(dialogShell, SWT.NONE);
					currentCombo.setLayoutData(currentComboLData);
					currentCombo.setText("cCombo1");
					currentCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.fine("currentCombo.widgetSelected, event="+evt);
							//TODO add your code for currentCombo.widgetSelected
						}
					});
				}
				{
					FormData voltageLabelLData = new FormData();
					voltageLabelLData.width = 84;
					voltageLabelLData.height = 22;
					voltageLabelLData.left =  new FormAttachment(0, 1000, 29);
					voltageLabelLData.top =  new FormAttachment(0, 1000, 167);
					voltageLabel = new CLabel(dialogShell, SWT.NONE);
					voltageLabel.setLayoutData(voltageLabelLData);
					voltageLabel.setText("Spannung");
				}
				{
					FormData voltageComboLData = new FormData();
					voltageComboLData.width = 84;
					voltageComboLData.height = 17;
					voltageComboLData.left =  new FormAttachment(0, 1000, 29);
					voltageComboLData.top =  new FormAttachment(0, 1000, 198);
					voltageCombo = new CCombo(dialogShell, SWT.NONE);
					voltageCombo.setLayoutData(voltageComboLData);
					voltageCombo.setText("cCombo1");
					voltageCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.fine("voltageCombo.widgetSelected, event="+evt);
							//TODO add your code for voltageCombo.widgetSelected
						}
					});
				}
				{
					FormData timeLabelLData = new FormData();
					timeLabelLData.width = 74;
					timeLabelLData.height = 22;
					timeLabelLData.left =  new FormAttachment(0, 1000, 29);
					timeLabelLData.top =  new FormAttachment(0, 1000, 111);
					timeLabel = new CLabel(dialogShell, SWT.NONE);
					timeLabel.setLayoutData(timeLabelLData);
					timeLabel.setText("Zeit [msec]");
				}
				{
					FormData timeComboLData = new FormData();
					timeComboLData.width = 84;
					timeComboLData.height = 17;
					timeComboLData.left =  new FormAttachment(0, 1000, 29);
					timeComboLData.top =  new FormAttachment(0, 1000, 138);
					timeCombo = new CCombo(dialogShell, SWT.NONE);
					timeCombo.setLayoutData(timeComboLData);
					timeCombo.setItems(new String[] {"1", "2", "3", "4" ,"5", "10", "20", "50", "100", "1000", "10000" });
					timeCombo.setText(new Integer(device.getTimeStep_ms()).toString());
					timeCombo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							device.setTimeStep_ms(new Integer(timeCombo.getText()).intValue());
							log.fine(" new timeStep_ms = " + device.getTimeStep_ms());
						}
					});
				}
				dialogShell.addDisposeListener(new DisposeListener() {
					public void widgetDisposed(DisposeEvent evt) {
						log.fine("dialogShell.widgetDisposed, event=" + evt);
						//TODO check if some thing to do before exiting
					}
				});
				{
					FormData descriptionLData = new FormData();
					descriptionLData.width = 277;
					descriptionLData.height = 60;
					descriptionLData.left =  new FormAttachment(0, 1000, 26);
					descriptionLData.top =  new FormAttachment(0, 1000, 49);
					description = new Text(dialogShell, SWT.CENTER | SWT.WRAP);
					description.setLayoutData(descriptionLData);
					description.setText("Mit START wird eine neue Serie an Daten generiert, bis STOP gedrückt wird ");
					description.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				}
				{
					FormData descriptionLabelLData = new FormData();
					descriptionLabelLData.width = 304;
					descriptionLabelLData.height = 37;
					descriptionLabelLData.left = new FormAttachment(0, 1000, 12);
					descriptionLabelLData.top = new FormAttachment(0, 1000, 12);
					descriptionLabelLData.right = new FormAttachment(1000, 1000, -12);
					descriptionLabel = new CLabel(dialogShell, SWT.CENTER | SWT.WRAP | SWT.EMBEDDED);
					descriptionLabel.setLayoutData(descriptionLabelLData);
					descriptionLabel.setText("Datengenerator");
					descriptionLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 14, 0, false, false));
				}
				{
					FormData startButtonLData = new FormData();
					startButtonLData.width = 121;
					startButtonLData.height = 31;
					startButtonLData.left =  new FormAttachment(0, 1000, 29);
					startButtonLData.top =  new FormAttachment(0, 1000, 296);
					startButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
					startButton.setLayoutData(startButtonLData);
					startButton.setText("START");
					startButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.fine("startButton.widgetSelected, event=" + evt);
							startButton.setEnabled(false);
							stopButton.setEnabled(true);
							channel = channels.getActiveChannel();
							channels.switchChannel(channel.getName());

							// prepare timed data gatherer thread
							int delay = 0;
							int period = device.getTimeStep_ms() * device.getDataBlockSize();
							log.fine("timer period = " + period + " ms");
							timer = new Timer();
							timerTask = new TimerTask() {
								String									recordSetKeyStem	= ") Datensatz";
								HashMap<String, Object>	data;															// Spannung, Strom
								String									recordSetKey;

								@SuppressWarnings("unchecked")
								//(Vector<Integer>)
								public void run() {
									RecordSet recordSet;
									try {

										data = serialPort.getData(null, recordNumber, null);

										if (channel.size() == 0 || isCollectDataStopped) {
											isCollectDataStopped = false;
											recordNumber++;

											recordSetKey = (channel.size() + 1) + recordSetKeyStem;
											channel.put(recordSetKey, RecordSet.createRecordSet(recordSetKey, application.getActiveDevice(), true, false));
											log.fine(recordSetKey + " created for channel " + channel.getName());
											if (channel.getActiveRecordSet() == null) Channels.getInstance().getActiveChannel().setActiveRecordSet(recordSetKey);
											channel.get(recordSetKey).setAllDisplayable();
											channel.applyTemplate(recordSetKey);

											// switch the active record set if the current record set is child of active channel
											if (channel.getName().equals(channels.getActiveChannel().getName())) {
												application.getMenuToolBar().addRecordSetName(recordSetKey);
												channels.getActiveChannel().getActiveRecordSet().switchRecordSet(recordSetKey);
											}
										}
										else {
											log.fine("re-using " + recordSetKey);
										}

										// prepare the data for adding to record set
										recordSet = channel.get(recordSetKey);
										// build the point array according curves from record set
										int[] points = new int[recordSet.size()];

										String[] measurements = device.getMeasurementNames(); // 0=Spannung, 1=Strom
										Vector<Integer> voltage = (Vector<Integer>) data.get(measurements[0]);
										Vector<Integer> current = (Vector<Integer>) data.get(measurements[1]);
										Iterator<Integer> iterV = voltage.iterator();
										Iterator<Integer> iterA = current.iterator();
										while (iterV.hasNext()) {
											points[0] = iterV.next().intValue();//Spannung 
											points[1] = iterA.next().intValue();//Strom 
											log.fine(String.format("Spannung = %d mV, Strom = %d mA", points[0], points[1]));
											recordSet.addPoints(points, false);
										}

										application.updateGraphicsWindow();
										//application.updateDataTable();
										//application.updateDigitalWindow();
									}
									catch (IOException e) {
										e.printStackTrace();
									}
								}
							};
							timer.scheduleAtFixedRate(timerTask, delay, period);
						}
					});
				}
				{
					FormData okButtonLData = new FormData();
					okButtonLData.width = 121;
					okButtonLData.height = 31;
					okButtonLData.left =  new FormAttachment(0, 1000, 180);
					okButtonLData.top =  new FormAttachment(0, 1000, 296);
					stopButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
					stopButton.setLayoutData(okButtonLData);
					stopButton.setText("STOP");
					stopButton.setEnabled(false);
					stopButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.fine("stopButton.widgetSelected, event=" + evt);
							stopTimer();
							isCollectDataStopped = true;
							startButton.setEnabled(true);
							stopButton.setEnabled(false);
						}
					});
				}
				dialogShell.setLocation(getParent().toDisplay(100, 100));
				dialogShell.open();
			}
			else {
				dialogShell.setVisible(true);
				dialogShell.setActive();
			}
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * stop the timer task thread, this tops data capturing
	 */
	public void stopTimer() {
		if (timerTask != null) timerTask.cancel();
		if (timer != null) timer.cancel();
	}

}
