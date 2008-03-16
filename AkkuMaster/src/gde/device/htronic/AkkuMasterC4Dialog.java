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
package osde.device.htronic;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.Settings;
import osde.data.Channels;
import osde.device.DeviceDialog;
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
/**
 * Dialog class for device AkkuMaster C4
 * @author Winfried Brügmann
 */
public class AkkuMasterC4Dialog extends DeviceDialog {
	private Logger									log										= Logger.getLogger(this.getClass().getName());

	private CLabel									totalDischargeCurrentLabel;
	private CLabel									totalChargeCurrentLabel;
	private Text										totalDischargeCurrentText;
	private Text										totalChareCurrentText;
	private Text										totalChargeCurrentUnit;
	private Text										totalDischargeCurrentUnit;
	private Composite								statusComposite;
	private Text										versionFrontplateType;
	private Text										versionCurrentType;
	private Text										versionDate;
	private Text										versionNumber;
	private Composite								versionComposite;
	private CTabItem								versionTabItem;
	private CTabFolder							tabFolder;
	private Button 									closeButton;

	private final Settings					settings;
	private final AkkuMasterC4			device;
	private final AkkuMasterC4SerialPort	serialPort;
	private final OpenSerialDataExplorer	application;
	private AkkuMasterChannelTab		channel1Tab, channel2Tab, channel3Tab, channel4Tab;

	private int											totalDischargeCurrent	= 0000;																				// mA
	private int											totalChargeCurrent		= 0000;																				// mA
	private HashMap<String, Object>	version;
	private Thread									versionThread;
	private final int								numberChannels;
	private final int								maxCurrent						= 2000; // [mA]
	private Channels								channels;

	/**
	 * constructor initialize all variables required
	 * @param parent Shell
	 * @param device AkkuMasterC4 class implementation == IDevice
	 */
	public AkkuMasterC4Dialog(Shell parent, AkkuMasterC4 device) {
		super(parent);
		this.device = device;
		this.serialPort = device.getSerialPort();
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
		this.numberChannels = device.getChannelCount();
		this.settings = Settings.getInstance();
	}

	public void open() {
		log.fine("dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed()));
		if (dialogShell == null || dialogShell.isDisposed()) {
			if (this.settings.isDeviceDialogsModal())
				dialogShell = new Shell(this.application.getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			else
				dialogShell = new Shell(application.getDisplay(), SWT.DIALOG_TRIM);
			
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(null);
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(440, 590);
			dialogShell.setText("Akkumaster C4 ToolBox");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/ToolBoxHot.gif"));
			{
				tabFolder = new CTabFolder(dialogShell, SWT.NONE);
				tabFolder.setBounds(0, 0, 430, 425);
				
				String[] aCapacity = new String[] { "100", "250", "500", "600", "800", "1000", "1250", "1500", "1750", "2000", "2500", "3000", "4000", "5000" };
				String[] aCellCount = new String[] { "1 Zelle", "2 Zellen", "3 Zellen", "4 Zellen", "5 Zellen", "6 Zellen", "7 Zellen", "8 Zellen", "9 Zellen", "10 Zellen", "11 Zellen", "12 Zellen", "13 Zellen", "14 Zellen"};
				String[] aAkkuType = new String[] { "0 NiCa", "1 NiMh", "2 Pb" };
				String[] aProgramm = new String[] { "1 nur laden", "2 nur entladen", "3 entladen-laden", "4 laden-entladen-laden", "5 formieren", "6 überwintern", "7 auffrischen", "8 ermittle Kapazität",
						"9 auffrischen" };
				String[] aChargeCurrent_mA = new String[] { "50", "100", "150", "200", "250", "300", "400", "500", "750", "900", "1000", "1500", "2000" };
				String[] aDischargeCurrent_mA = aChargeCurrent_mA;
				String[] aCycleCount = new String[] { "0", "1", "2", "3", "4", "5" };
				String[] aWaitTime_Min = new String[] { "5", "10", "15", "20", "30", "60", "120" };
				
				///////////////////////////////////////////////////				
				if (channel1Tab == null && numberChannels > 0)
					channel1Tab = new AkkuMasterChannelTab(this, (" " + device.getChannelName(1)), AkkuMasterC4SerialPort.channel_1, serialPort, channels.get(1), aCapacity, aCellCount, aAkkuType, aProgramm, aChargeCurrent_mA,
							aDischargeCurrent_mA, aCycleCount, aWaitTime_Min);
				channel1Tab.addChannelTab(tabFolder);
				
				if (channel2Tab == null && numberChannels > 1)
					channel2Tab = new AkkuMasterChannelTab(this, (" " + device.getChannelName(2)), AkkuMasterC4SerialPort.channel_2, serialPort, channels.get(2), aCapacity, aCellCount, aAkkuType, aProgramm, aChargeCurrent_mA,
							aDischargeCurrent_mA, aCycleCount, aWaitTime_Min);
				channel2Tab.addChannelTab(tabFolder);
				
				if (channel3Tab == null && numberChannels > 2)
					channel3Tab = new AkkuMasterChannelTab(this, (" " + device.getChannelName(3)), AkkuMasterC4SerialPort.channel_3, serialPort, channels.get(3), aCapacity, aCellCount, aAkkuType, aProgramm, aChargeCurrent_mA,
							aDischargeCurrent_mA, aCycleCount, aWaitTime_Min);
				channel3Tab.addChannelTab(tabFolder);
				
				if (channel4Tab == null && numberChannels > 3)
					channel4Tab = new AkkuMasterChannelTab(this, (" " + device.getChannelName(4)), AkkuMasterC4SerialPort.channel_4, serialPort, channels.get(4), aCapacity, aCellCount, aAkkuType, aProgramm, aChargeCurrent_mA,
							aDischargeCurrent_mA, aCycleCount, aWaitTime_Min);
				channel4Tab.addChannelTab(tabFolder);
				///////////////////////////////////////////////////		
				
				{
					versionTabItem = new CTabItem(tabFolder, SWT.NONE);
					versionTabItem.setText("Version");
					{
						versionComposite = new Composite(tabFolder, SWT.NONE);
						versionComposite.setLayout(null);
						versionTabItem.setControl(versionComposite);
						versionComposite.addPaintListener(new PaintListener() {
							public void paintControl(PaintEvent evt) {
								log.finest("versionComposite.paintControl, event=" + evt);
								if (version != null) {
									versionNumber.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_NUMBER, "\t\t", (String) version.get(AkkuMasterC4SerialPort.VERSION_NUMBER)));
									versionDate.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_DATE, "\t\t", (String) version.get(AkkuMasterC4SerialPort.VERSION_DATE)));
									versionCurrentType.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, "\t\t", (String) version.get(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT)));
									versionFrontplateType.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, "\t\t", (String) version.get(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT)));
								}
								else {
									versionNumber.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_NUMBER, "\t\t", "?"));
									versionDate.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_DATE, "\t\t", "?"));
									versionCurrentType.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT, "\t\t", "?"));
									versionFrontplateType.setText(String.format("%-20s %s:   %s", AkkuMasterC4SerialPort.VERSION_TYPE_FRONT, "\t\t", "?"));
									updateVersion();
								}
							}
						});
						{
							versionNumber = new Text(versionComposite, SWT.NONE);
							versionNumber.setBounds(24, 62, 288, 30);
							versionNumber.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionNumber.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							versionNumber.setEditable(false);
						}
						{
							versionDate = new Text(versionComposite, SWT.NONE);
							versionDate.setBounds(24, 111, 288, 30);
							versionDate.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionDate.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							versionDate.setEditable(false);
						}
						{
							versionCurrentType = new Text(versionComposite, SWT.NONE);
							versionCurrentType.setBounds(24, 159, 288, 30);
							versionCurrentType.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionCurrentType.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							versionCurrentType.setEditable(false);
						}
						{
							versionFrontplateType = new Text(versionComposite, SWT.NONE);
							versionFrontplateType.setBounds(24, 212, 288, 30);
							versionFrontplateType.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionFrontplateType.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
							versionFrontplateType.setEditable(false);
						}
					}
				}
				tabFolder.setSelection(Channels.getInstance().getActiveChannelNumber() - 1);
			}
			{
				statusComposite = new Composite(dialogShell, SWT.NONE);
				statusComposite.setLayout(null);
				statusComposite.setBounds(0, 430, 430, 65);
				statusComposite.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						totalDischargeCurrentLabel.setText(new Integer(totalDischargeCurrent).toString());
						totalChargeCurrentLabel.setText(new Integer(totalChargeCurrent).toString());
					}
				});
				{
					totalDischargeCurrentLabel = new CLabel(statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					totalDischargeCurrentLabel.setBounds(235, 34, 50, 16);
					totalDischargeCurrentLabel.setText(new Double(totalDischargeCurrent).toString());
					totalDischargeCurrentLabel.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					totalChargeCurrentLabel = new CLabel(statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					totalChargeCurrentLabel.setBounds(235, 8, 50, 16);
					totalChargeCurrentLabel.setText(new Double(totalChargeCurrent).toString());
					totalChargeCurrentLabel.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					totalChareCurrentText = new Text(statusComposite, SWT.NONE);
					totalChareCurrentText.setText("Gesammtladestrom       :");
					totalChareCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalChareCurrentText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					totalChareCurrentText.setBounds(20, 10, 190, 20);
				}
				{
					totalDischargeCurrentText = new Text(statusComposite, SWT.NONE);
					totalDischargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalDischargeCurrentText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					totalDischargeCurrentText.setText("Gesammtentladestrom  :");
					totalDischargeCurrentText.setBounds(20, 35, 190, 20);
				}
				{
					totalDischargeCurrentUnit = new Text(statusComposite, SWT.NONE);
					totalDischargeCurrentUnit.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalDischargeCurrentUnit.setText("[mA]");
					totalDischargeCurrentUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					totalDischargeCurrentUnit.setBounds(300, 10, 119, 20);
				}
				{
					totalChargeCurrentUnit = new Text(statusComposite, SWT.NONE);
					totalChargeCurrentUnit.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalChargeCurrentUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					totalChargeCurrentUnit.setText("[mA]");
					totalChargeCurrentUnit.setBounds(300, 35, 119, 20);
				}
			}
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.fine("dialogShell.widgetDisposed, event=" + evt);
					if (serialPort != null && serialPort.isConnected()) serialPort.close();
					if (versionThread.isAlive()) versionThread = null;
				}
			});
			dialogShell.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					application.openHelpDialog("AkkuMaster", "HelpInfo.html");
				}
			});
			{
				closeButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				closeButton.setText("Schliessen");
				closeButton.setBounds(82, 509, 260, 30);
				closeButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						if(channel1Tab.isDataColletionActive() || channel2Tab.isDataColletionActive() || channel3Tab.isDataColletionActive() && channel4Tab.isDataColletionActive())
							isClosePossible = false;
						else 
							isClosePossible = true;
						
						dispose();
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			updateVersion();
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

	public void close() {
		if (dialogShell != null && !dialogShell.isDisposed()) {
			channel1Tab.stopTimer();
			channel2Tab.stopTimer();
			channel3Tab.stopTimer();
			channel4Tab.stopTimer();
			dialogShell.dispose();
		}
	}

	public boolean isDataColletionActive() {
		return channel1Tab.isCollectData() && channel1Tab.isCollectDataStopped()
			&& channel2Tab.isCollectData() && channel2Tab.isCollectDataStopped()
			&& channel3Tab.isCollectData() && channel3Tab.isCollectDataStopped()
			&& channel4Tab.isCollectData() && channel4Tab.isCollectDataStopped();
	}

	/**
	 * add current to discharge current sum
	 * @param newCurrent
	 */
	public void addTotalDischargeCurrent(int newCurrent) {
		totalDischargeCurrent = totalDischargeCurrent + newCurrent;
	}

	/**
	 * subtract current to discharge current sum
	 * @param newCurrent
	 */
	public void subtractTotalDischargeCurrent(int newCurrent) {
		totalDischargeCurrent = totalDischargeCurrent - newCurrent;
	}

	/**
	 * add current to charge current sum
	 * @param newCurrent
	 */
	public void addTotalChargeCurrent(int newCurrent) {
		totalChargeCurrent = totalChargeCurrent + newCurrent;
	}

	/**
	 * subtract current to charge current sum
	 * @param newCurrent
	 */
	public void subtractTotalChargeCurrent(int newCurrent) {
		totalChargeCurrent = totalChargeCurrent - newCurrent;
	}

	/**
	 * method to query sum of charge and discharge current in mA to enable overload check
	 */
	public int getActiveCurrent() {
		return totalChargeCurrent + totalDischargeCurrent;
	}

	public int getMaxCurrent() {
		return maxCurrent;
	}

	/**
	 * update version string 
	 */
	private void updateVersion() {
		try {
			if (serialPort != null) {
				if (!serialPort.isConnected()) {
					serialPort.open();
				}
				if (versionThread == null || !versionThread.isAlive()) {
					versionThread = new Thread() {
						public void run() {
							try {
								version = serialPort.getVersion();
								dialogShell.getDisplay().asyncExec(new Runnable() {
									public void run() {
										versionComposite.redraw();
									}
								});
							}
							catch (Exception e) {
								application.openMessageDialog("Bei der seriellen Kommunikation gibt es Probleme, bitte die Portkonfiguration überprüfen.\n" + e.getMessage());
							}
						}
					};
					versionThread.start();
				}
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
			application.openMessageDialog("Der Versuch den seriellen Port zu öffnen ist gescheitert. Bitte gegebenenfalls die Portkonfiguration überprüfen.\n" + e.getMessage());
		}
	}
}
