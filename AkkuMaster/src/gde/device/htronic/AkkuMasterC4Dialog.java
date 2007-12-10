package osde.device.htronic;

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import osde.config.DeviceConfiguration;
import osde.data.Channels;
import osde.data.RecordSet;
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
public class AkkuMasterC4Dialog extends Dialog implements osde.device.DeviceDialog {
	private Logger									log										= Logger.getLogger(this.getClass().getName());

	private Shell										dialogShell;
	private CLabel									gesammtentladestromAnzeigeText;
	private CLabel									gesammtladestromAnzeigeText;
	private Text										gesammtEntladeStromText;
	private Text										gesammtLadeStromText;
	private Text										totalChargeCurrentUnit;
	private Text										totalDischargeCurrentUnit;
	private Composite								statusComposite;
	private Text										frontplattenvariante;
	private Text										stromvariante;
	private Text										datum;
	private Text										versionNumber;
	private Composite								versionComposite;
	private CTabItem								versionTabItem;
	private CTabFolder							tabFolder;

	private AkkuMasterChannelTab		channel1Tab, channel2Tab, channel3Tab, channel4Tab;
	private AkkuMasterC4SerialPort	serialPort;
	private OpenSerialDataExplorer	application;

	private int											totalDischargeCurrent	= 0000;																				// mA
	private int											totalChargeCurrent		= 0000;																				// mA
	private HashMap<String, Object>	version;
	private final int								numberChannels				= 4;
	private final int								maxCurrent						= 2000;
	private DeviceConfiguration			deviceConfig;
	private AkkuMasterCalculationThread	threadPower, threadEnergy;
	private Channels										channels;
	private int													lastTabFolderNummer	= 0;

public AkkuMasterC4Dialog(Shell parent, int style, DeviceConfiguration deviceConfig) {
		super(parent, style);
		this.deviceConfig = deviceConfig;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	public void open() {
		serialPort = (AkkuMasterC4SerialPort) application.getDeviceSerialPort();

		FormData gesammtEntladeStromTextLData = new FormData();
		gesammtEntladeStromTextLData.width = 170;
		gesammtEntladeStromTextLData.height = 20;
		gesammtEntladeStromTextLData.left = new FormAttachment(0, 1000, 12);
		gesammtEntladeStromTextLData.top = new FormAttachment(0, 1000, 34);

		log.fine("dialogShell.isDisposed() " + ((dialogShell == null) ? "null" : dialogShell.isDisposed()));
		if (dialogShell == null || dialogShell.isDisposed()) {

			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM);

			{
				//Register as a resource user - SWTResourceManager will
				//handle the obtaining and disposing of resources
				SWTResourceManager.registerResourceUser(dialogShell);
			}

			dialogShell.setLayout(new FormLayout());
			dialogShell.layout();
			dialogShell.pack();
			dialogShell.setSize(439, 593);
			dialogShell.setText("Akkumaster C4 ToolBox");
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
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
				String[] aZellenZahl = new String[] { "1 Zelle", "2 Zellen", "3 Zellen", "4 Zellen", "5 Zellen", "6 Zellen", "7 Zellen", "8 Zellen", "9 Zellen", "10 Zellen", "11 Zellen", "12 Zellen" };
				String[] aAkkuTyp = new String[] { "0 NiCa", "1 NiMh", "2 Pb" };
				String[] aProgramm = new String[] { "1 nur laden", "2 nur entladen", "3 entladen-laden", "4 laden-entladen-laden", "5 formieren", "6 überwintern", "7 auffrischen", "8 ermittle Kapazität",
						"9 auffrischen" };
				String[] aLadestromMilliA = new String[] { "50", "100", "150", "200", "250", "300", "400", "500", "750", "900", "1000", "1500", "2000" };
				String[] aEntladeStromMilliA = aLadestromMilliA;
				String[] aAnzahlWiederholungen = new String[] { "0", "1", "2", "3", "4", "5" };
				String[] aWarteZeitMin = new String[] { "5", "10", "15", "20", "30", "60", "120" };

				///////////////////////////////////////////////////				
				if (channel1Tab == null && numberChannels > 0)
					channel1Tab = new AkkuMasterChannelTab(this, " Kanal 1 ", AkkuMasterC4SerialPort.channel_1, serialPort, channels.get(1), aCapacity, aZellenZahl, aAkkuTyp, aProgramm,
							aLadestromMilliA, aEntladeStromMilliA, aAnzahlWiederholungen, aWarteZeitMin);
				channel1Tab.addChannelTab(tabFolder);

				if (channel2Tab == null && numberChannels > 1)
					channel2Tab = new AkkuMasterChannelTab(this, " Kanal 2 ", AkkuMasterC4SerialPort.channel_2, serialPort, channels.get(2), aCapacity, aZellenZahl, aAkkuTyp, aProgramm,
							aLadestromMilliA, aEntladeStromMilliA, aAnzahlWiederholungen, aWarteZeitMin);
				channel2Tab.addChannelTab(tabFolder);

				if (channel3Tab == null && numberChannels > 2)
					channel3Tab = new AkkuMasterChannelTab(this, " Kanal 3 ", AkkuMasterC4SerialPort.channel_3, serialPort, channels.get(3), aCapacity, aZellenZahl, aAkkuTyp, aProgramm,
							aLadestromMilliA, aEntladeStromMilliA, aAnzahlWiederholungen, aWarteZeitMin);
				channel3Tab.addChannelTab(tabFolder);

				if (channel4Tab == null && numberChannels > 3)
					channel4Tab = new AkkuMasterChannelTab(this, " Kanal 4 ", AkkuMasterC4SerialPort.channel_4, serialPort, channels.get(4), aCapacity, aZellenZahl, aAkkuTyp, aProgramm,
							aLadestromMilliA, aEntladeStromMilliA, aAnzahlWiederholungen, aWarteZeitMin);
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
									version = serialPort.getVersion();
									versionNumber.setText(AkkuMasterC4SerialPort.VERSION_NUMBER + " :  " + (String)version.get(AkkuMasterC4SerialPort.VERSION_NUMBER));
									datum.setText(AkkuMasterC4SerialPort.VERSION_DATE + " :  " + (String)version.get(AkkuMasterC4SerialPort.VERSION_DATE));
									stromvariante.setText(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT + " :  " + (String)version.get(AkkuMasterC4SerialPort.VERSION_TYPE_CURRENT));
									frontplattenvariante.setText(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT + " :  " + (String)version.get(AkkuMasterC4SerialPort.VERSION_TYPE_FRONT));
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
							datum = new Text(versionComposite, SWT.NONE);
							datum.setBounds(24, 111, 288, 30);
							datum.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							datum.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
						}
						{
							stromvariante = new Text(versionComposite, SWT.NONE);
							stromvariante.setBounds(24, 159, 288, 30);
							stromvariante.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							stromvariante.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
						}
						{
							frontplattenvariante = new Text(versionComposite, SWT.NONE);
							frontplattenvariante.setBounds(24, 212, 288, 30);
							frontplattenvariante.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
							frontplattenvariante.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
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
					FormData GesammtentladestromAnzeigeTextLData = new FormData();
					GesammtentladestromAnzeigeTextLData.width = 50;
					GesammtentladestromAnzeigeTextLData.height = 16;
					GesammtentladestromAnzeigeTextLData.left = new FormAttachment(0, 1000, 235);
					GesammtentladestromAnzeigeTextLData.top = new FormAttachment(0, 1000, 34);
					gesammtentladestromAnzeigeText = new CLabel(statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					gesammtentladestromAnzeigeText.setLayoutData(GesammtentladestromAnzeigeTextLData);
					gesammtentladestromAnzeigeText.setText(new Double(totalDischargeCurrent).toString());
					gesammtentladestromAnzeigeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					gesammtentladestromAnzeigeText.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							gesammtentladestromAnzeigeText.setText(new Integer(totalDischargeCurrent).toString());
						}
					});
				}
				{
					FormData GesammtladestromAnzeigeTextLData = new FormData();
					GesammtladestromAnzeigeTextLData.width = 50;
					GesammtladestromAnzeigeTextLData.height = 16;
					GesammtladestromAnzeigeTextLData.left = new FormAttachment(0, 1000, 235);
					GesammtladestromAnzeigeTextLData.top = new FormAttachment(0, 1000, 8);
					gesammtladestromAnzeigeText = new CLabel(statusComposite, SWT.RIGHT | SWT.EMBEDDED);
					//gesammtladestromAnzeigeText = new Text(statusComposite, SWT.NONE);
					gesammtladestromAnzeigeText.setLayoutData(GesammtladestromAnzeigeTextLData);
					gesammtladestromAnzeigeText.setText(new Double(totalChargeCurrent).toString());
					gesammtladestromAnzeigeText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					gesammtladestromAnzeigeText.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							gesammtladestromAnzeigeText.setText(new Integer(totalChargeCurrent).toString());
						}
					});
				}
				{
					FormData GesammtstromTextLData = new FormData();
					GesammtstromTextLData.width = 190;
					GesammtstromTextLData.height = 20;
					GesammtstromTextLData.left = new FormAttachment(0, 1000, 20);
					GesammtstromTextLData.top = new FormAttachment(0, 1000, 10);
					gesammtLadeStromText = new Text(statusComposite, SWT.NONE);
					gesammtLadeStromText.setLayoutData(GesammtstromTextLData);
					gesammtLadeStromText.setText("Gesammtladestrom       :");
					gesammtLadeStromText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					gesammtLadeStromText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
				}
				{
					FormData gesammtEntladeStromTextLData1 = new FormData();
					gesammtEntladeStromTextLData1.width = 190;
					gesammtEntladeStromTextLData1.height = 20;
					gesammtEntladeStromTextLData1.left = new FormAttachment(0, 1000, 20);
					//gesammtEntladeStromTextLData1.right = new FormAttachment(423, 1000, 0);
					gesammtEntladeStromTextLData1.top = new FormAttachment(0, 1000, 35);
					//gesammtEntladeStromTextLData1.bottom = new FormAttachment(455, 1000, 0);
					gesammtEntladeStromText = new Text(statusComposite, SWT.NONE);
					gesammtEntladeStromText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					gesammtEntladeStromText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					gesammtEntladeStromText.setLayoutData(gesammtEntladeStromTextLData1);
					gesammtEntladeStromText.setText("Gesammtentladestrom  :");
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
			dialogShell.setActive();
			//dialogShell.forceFocus();
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

	public CTabFolder getTabFolderKanal1(Composite parent) {
		return tabFolder;
	}

	/**
	 * add current to discharge current sum
	 * @param current
	 */
	public void addTotalDischargeCurrent(int newCurrent) {
		totalDischargeCurrent = totalDischargeCurrent + newCurrent;
		//		OSDE.display.asyncExec(new Runnable() {
		//			public void run() {
		//				gesammtentladestromAnzeigeText.redraw();
		//			}
		//		});
	}

	/**
	 * subtract current to discharge current sum
	 * @param current
	 */
	public void subtractTotalDischargeCurrent(int newCurrent) {
		totalDischargeCurrent = totalDischargeCurrent - newCurrent;
		//		OSDE.display.asyncExec(new Runnable() {
		//			public void run() {
		//				gesammtentladestromAnzeigeText.redraw();
		//			}
		//		});
	}

	/**
	 * add current to charge current sum
	 * @param current
	 */
	public void addTotalChargeCurrent(int newCurrent) {
		totalChargeCurrent = totalChargeCurrent + newCurrent;
		//		OSDE.display.asyncExec(new Runnable() {
		//			public void run() {
		//				gesammtladestromAnzeigeText.redraw();
		//			}
		//		});
	}

	/**
	 * subtract current to charge current sum
	 * @param current
	 */
	public void subtractTotalChargeCurrent(int newCurrent) {
		totalChargeCurrent = totalChargeCurrent - newCurrent;
		//		OSDE.display.asyncExec(new Runnable() {
		//			public void run() {
		//				gesammtladestromAnzeigeText.redraw();
		//			}
		//		});
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
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double translateValue(String recordKey, double value) {
		double newValue = 0;
		log.fine(String.format("input value for %s - %f", recordKey, value));
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CURRENT)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CHARGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.POWER)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.ENERGY)) {
			newValue = value;
		}
		log.fine(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * function to translate measured value from a device to values represented
	 * @return double with the adapted value
	 */
	public double reverseTranslateValue(String recordKey, double value) {
		double newValue = 0;
		log.fine(String.format("input value for %s - %f", recordKey, value));
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CURRENT)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.CHARGE)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.POWER)) {
			newValue = value;
		}
		else if (recordKey.startsWith(RecordSet.ENERGY)) {
			newValue = value;
		}
		log.fine(String.format("value calculated for %s - %f", recordKey, newValue));
		return newValue;
	}

	/**
	 * @return the dataUnit
	 */
	public String getDataUnit(String recordKey) {
		String unit = "";
		recordKey = recordKey.split("_")[0];
		HashMap<String, Object> record = deviceConfig.getRecordConfig(recordKey);
		if (recordKey.startsWith(RecordSet.VOLTAGE)) {
			unit = (String) record.get("Einheit1");
		}
		else if (recordKey.startsWith(RecordSet.CURRENT)) {
			unit = (String) record.get("Einheit2");
		}
		else if (recordKey.startsWith(RecordSet.CHARGE)) {
			unit = (String) record.get("Einheit3");
		}
		else if (recordKey.startsWith(RecordSet.POWER)) {
			unit = (String) record.get("Einheit4");
		}
		else if (recordKey.startsWith(RecordSet.ENERGY)) {
			unit = (String) record.get("Einheit5");
		}
		return unit;
	}

	/**
	 * function to calculate values for inactive records
	 * @return double with the adapted value
	 */
	public void makeInActiveDisplayable(RecordSet recordSet) {
		// since there are measurement point every 10 seconds during cpturing only and the calculation will take place directly switch all to displayable
		if (recordSet.isFromFile() && recordSet.isRaw()) {
			// calculate the values required
			try {
				threadPower = new AkkuMasterCalculationThread(RecordSet.POWER, channels.getActiveChannel().getActiveRecordSet());
				threadPower.start();
				threadEnergy = new AkkuMasterCalculationThread(RecordSet.ENERGY, channels.getActiveChannel().getActiveRecordSet());
				threadEnergy.start();
			}
			catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * the number drives which tab folder is displayed next if dialog opened
	 * @param lastTabFolderNummer the lastTabFolderNummer to set
	 */
	public void setLastTabFolderNummer(int lastTabFolderNummer) {
		this.lastTabFolderNummer = lastTabFolderNummer;
	}

}
