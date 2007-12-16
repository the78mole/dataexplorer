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
import org.eclipse.swt.custom.CLabel;
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
public class SimulatorDialog extends DeviceDialog {
	private Logger												log										= Logger.getLogger(this.getClass().getName());

	private Shell													dialogShell;
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
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.NULL); // SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(336, 393);
			dialogShell.setText("Simulator");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
			{
				FormData descriptionLData = new FormData();
				descriptionLData.width = 277;
				descriptionLData.height = 60;
				descriptionLData.left = new FormAttachment(0, 1000, 24);
				descriptionLData.top = new FormAttachment(0, 1000, 87);
				description = new Text(dialogShell, SWT.CENTER | SWT.WRAP);
				description.setLayoutData(descriptionLData);
				description.setText("Mit START wird eine neue Serie an Daten generiert, bis STOP gedrückt wird");
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
				startButtonLData.width = 198;
				startButtonLData.height = 31;
				startButtonLData.left = new FormAttachment(0, 1000, 62);
				startButtonLData.top = new FormAttachment(0, 1000, 203);
				startButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				startButton.setLayoutData(startButtonLData);
				startButton.setText("START");
				startButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						System.out.println("startButton.widgetSelected, event=" + evt);
						channel = channels.getActiveChannel();
						channels.switchChannel(channel.getName());

						// prepare timed data gatherer thread
						int delay = 0;
						int period = device.getTimeStep_ms() * device.getClusterSize();
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
											application.getDataToolBar().addRecordSetName(recordSetKey);
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

									Vector<Integer> voltage = (Vector<Integer>) data.get(RecordSet.VOLTAGE);
									Vector<Integer> current = (Vector<Integer>) data.get(RecordSet.CURRENT);
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
				okButtonLData.width = 203;
				okButtonLData.height = 33;
				okButtonLData.left = new FormAttachment(0, 1000, 62);
				okButtonLData.top = new FormAttachment(0, 1000, 273);
				stopButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				stopButton.setLayoutData(okButtonLData);
				stopButton.setText("STOP");
				stopButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.fine("stopButton.widgetSelected, event=" + evt);
						stopTimer();
						isCollectDataStopped = true;
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
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
