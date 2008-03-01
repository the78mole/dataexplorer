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
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
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
 * DSialog class for device AkkuMaster C4
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

	private final Settings					settings;
	private final AkkuMasterC4			device;
	private final AkkuMasterC4SerialPort	serialPort;
	private final OpenSerialDataExplorer	application;
	private AkkuMasterChannelTab		channel1Tab, channel2Tab, channel3Tab, channel4Tab;

	private int											totalDischargeCurrent	= 0000;																				// mA
	private int											totalChargeCurrent		= 0000;																				// mA
	private HashMap<String, Object>	version;
	private final int								numberChannels;
	private final int								maxCurrent						= 2000; // [mA]
	private Channels								channels;
	private int											lastTabFolderNummer		= 0;

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
			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(439, 593);
			dialogShell.setText("Akkumaster C4 ToolBox");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
			dialogShell.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					log.fine("dialogShell.widgetDisposed, event=" + evt);
					//TODO check if some thing to do before exiting
				}
			});
			{
				FormData tabFolderLData = new FormData();
				tabFolderLData.width = 427;
				tabFolderLData.height = 462;
				tabFolderLData.left = new FormAttachment(0, 1000, 0);
				tabFolderLData.top = new FormAttachment(0, 1000, 0);
				tabFolderLData.right = new FormAttachment(1000, 1000, 0);
				tabFolderLData.bottom = new FormAttachment(1000, 1000, -73);
				tabFolder = new CTabFolder(dialogShell, SWT.NONE);
				tabFolder.setLayoutData(tabFolderLData);

				String[] aCapacity = new String[] { "100", "250", "500", "600", "800", "1000", "1250", "1500", "1750", "2000", "2500", "3000", "4000", "5000" };
				String[] aCellCount = new String[] { "1 Zelle", "2 Zellen", "3 Zellen", "4 Zellen", "5 Zellen", "6 Zellen", "7 Zellen", "8 Zellen", "9 Zellen", "10 Zellen", "11 Zellen", "12 Zellen" };
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
								try {
									if (serialPort != null && serialPort.isConnected()) {
										version = serialPort.getVersion();
										versionNumber.setText(AkkuMasterC4SerialPort.VERSION_NUMBER + " :  " + (String) version.get(AkkuMasterC4SerialPort.VERSION_NUMBER));
										versionDate.setText(AkkuMasterC4SerialPort.VERSION_DATE + " :  " + (String) version.get(AkkuMasterC4SerialPort.VERSION_DATE));
										versionCurrentType.setText(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT + " :  " + (String) version.get(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT));
										versionFrontplateType.setText(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT + " :  " + (String) version.get(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT));
									}
									else 
										application.openMessageDialog("Erst den seriellen Port öffnen");
								}
								catch (Exception e) {
									application.openMessageDialog("Das angeschlossene Gerät antwortet nicht auf dem seriellen Port!");
								}
							}
						});
						{
							versionNumber = new Text(versionComposite, SWT.NONE);
							versionNumber.setBounds(24, 62, 288, 30);
							versionNumber.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionNumber.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
						}
						{
							versionDate = new Text(versionComposite, SWT.NONE);
							versionDate.setBounds(24, 111, 288, 30);
							versionDate.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionDate.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
						}
						{
							versionCurrentType = new Text(versionComposite, SWT.NONE);
							versionCurrentType.setBounds(24, 159, 288, 30);
							versionCurrentType.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionCurrentType.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
						}
						{
							versionFrontplateType = new Text(versionComposite, SWT.NONE);
							versionFrontplateType.setBounds(24, 212, 288, 30);
							versionFrontplateType.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							versionFrontplateType.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
						}
					}
				}
				tabFolder.setSelection(lastTabFolderNummer);
			}
			{
				FormData StatusAnzeigeLData = new FormData();
				StatusAnzeigeLData.width = 431;
				StatusAnzeigeLData.height = 67;
				StatusAnzeigeLData.left = new FormAttachment(0, 1000, 0);
				StatusAnzeigeLData.top = new FormAttachment(0, 1000, 493);
				StatusAnzeigeLData.right = new FormAttachment(1000, 1000, 0);
				StatusAnzeigeLData.bottom = new FormAttachment(1000, 1000, 0);
				statusComposite = new Composite(dialogShell, SWT.NONE);
				FormLayout StatusAnzeigeLayout = new FormLayout();
				statusComposite.setLayout(StatusAnzeigeLayout);
				statusComposite.setLayoutData(StatusAnzeigeLData);
				{
					FormData totalDischargeCurrentTextLData = new FormData();
					totalDischargeCurrentTextLData.width = 50;
					totalDischargeCurrentTextLData.height = 16;
					totalDischargeCurrentTextLData.left = new FormAttachment(0, 1000, 235);
					totalDischargeCurrentTextLData.top = new FormAttachment(0, 1000, 34);
					totalDischargeCurrentLabel = new CLabel(statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					totalDischargeCurrentLabel.setLayoutData(totalDischargeCurrentTextLData);
					totalDischargeCurrentLabel.setText(new Double(totalDischargeCurrent).toString());
					totalDischargeCurrentLabel.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalDischargeCurrentLabel.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							totalDischargeCurrentLabel.setText(new Integer(totalDischargeCurrent).toString());
						}
					});
				}
				{
					FormData totalChargeCurrentTextLData = new FormData();
					totalChargeCurrentTextLData.width = 50;
					totalChargeCurrentTextLData.height = 16;
					totalChargeCurrentTextLData.left = new FormAttachment(0, 1000, 235);
					totalChargeCurrentTextLData.top = new FormAttachment(0, 1000, 8);
					totalChargeCurrentLabel = new CLabel(statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					totalChargeCurrentLabel.setLayoutData(totalChargeCurrentTextLData);
					totalChargeCurrentLabel.setText(new Double(totalChargeCurrent).toString());
					totalChargeCurrentLabel.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalChargeCurrentLabel.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							totalChargeCurrentLabel.setText(new Integer(totalChargeCurrent).toString());
						}
					});
				}
				{
					FormData totalChargeCurrentTextLData1 = new FormData();
					totalChargeCurrentTextLData1.width = 190;
					totalChargeCurrentTextLData1.height = 20;
					totalChargeCurrentTextLData1.left = new FormAttachment(0, 1000, 20);
					totalChargeCurrentTextLData1.top = new FormAttachment(0, 1000, 10);
					totalChareCurrentText = new Text(statusComposite, SWT.NONE);
					totalChareCurrentText.setLayoutData(totalChargeCurrentTextLData1);
					totalChareCurrentText.setText("Gesammtladestrom       :");
					totalChareCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalChareCurrentText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
				}
				{
					FormData totalDischargeCurrentTextLData1 = new FormData();
					totalDischargeCurrentTextLData1.width = 190;
					totalDischargeCurrentTextLData1.height = 20;
					totalDischargeCurrentTextLData1.left = new FormAttachment(0, 1000, 20);
					totalDischargeCurrentTextLData1.top = new FormAttachment(0, 1000, 35);
					totalDischargeCurrentText = new Text(statusComposite, SWT.NONE);
					totalDischargeCurrentText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalDischargeCurrentText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					totalDischargeCurrentText.setLayoutData(totalDischargeCurrentTextLData1);
					totalDischargeCurrentText.setText("Gesammtentladestrom  :");
				}
				{
					totalDischargeCurrentUnit = new Text(statusComposite, SWT.NONE);
					totalDischargeCurrentUnit.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalDischargeCurrentUnit.setText("[mA]");
					totalDischargeCurrentUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					FormData text1LData = new FormData();
					text1LData.width = 150;
					text1LData.height = 20;
					text1LData.left = new FormAttachment(0, 1000, 300);
					text1LData.top = new FormAttachment(0, 1000, 10);
					totalDischargeCurrentUnit.setLayoutData(text1LData);
				}
				{
					totalChargeCurrentUnit = new Text(statusComposite, SWT.NONE);
					totalChargeCurrentUnit.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					totalChargeCurrentUnit.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					totalChargeCurrentUnit.setText("[mA]");
					FormData text2LData = new FormData();
					text2LData.width = 150;
					text2LData.height = 20;
					text2LData.left = new FormAttachment(0, 1000, 300);
					text2LData.top = new FormAttachment(0, 1000, 35);
					totalChargeCurrentUnit.setLayoutData(text2LData);
				}
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

	public void close() {
		if (dialogShell != null && !dialogShell.isDisposed()) {
			channel1Tab.stopTimer();
			channel2Tab.stopTimer();
			channel3Tab.stopTimer();
			channel4Tab.stopTimer();
			dialogShell.dispose();
		}
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
	 * the number drives which tab folder is displayed next if dialog opened
	 * @param lastTabFolderNummer the lastTabFolderNummer to set
	 */
	public void setLastTabFolderNummer(int lastTabFolderNummer) {
		this.lastTabFolderNummer = lastTabFolderNummer;
	}
}
