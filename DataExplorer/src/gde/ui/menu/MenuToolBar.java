package osde.ui.menu;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import osde.config.DeviceConfiguration;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.DeviceSelectionDialog;

public class MenuToolBar {
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private final OpenSerialDataExplorer	application;
	private CoolBar												menuCoolBar;
	private CoolItem											menuCoolItem;
	private Composite											composite1;
	private ToolItem											toolBoxToolItem;
	private ToolItem											nextDeviceToolItem;
	private ToolItem											prevDeviceToolItem;
	private ToolItem											deviceToolItem;
	private ToolItem											separatorToolItem;
	private ToolItem											saveAsToolItem;
	private ToolItem											saveToolItem;
	private ToolItem											openToolItem;
	private ToolItem											newToolItem;
	private ToolBar												menuToolBar;

	public MenuToolBar(OpenSerialDataExplorer parent, CoolBar menuCoolBar) {
		this.application = parent;
		this.menuCoolBar = menuCoolBar;
	}

	public void init() {
		menuCoolBar = new CoolBar(this.application, SWT.NONE);
		menuCoolBar.setSize(530, 50);
		create();
	}

	public void create() {
		{
			menuCoolItem = new CoolItem(menuCoolBar, SWT.NONE);
			menuCoolItem.setPreferredSize(new org.eclipse.swt.graphics.Point(48, 218));
			menuCoolItem.setMinimumSize(new org.eclipse.swt.graphics.Point(48, 218));
			//menuCoolItem.setSize(48, 218);
			{
				composite1 = new Composite(menuCoolBar, SWT.NONE);
				menuCoolItem.setControl(composite1);
				RowLayout composite1Layout2 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				composite1.setLayout(composite1Layout2);
				{
					RowData menuToolBarLData = new RowData();
					menuToolBarLData.width = 324;
					menuToolBarLData.height = 32;
					menuToolBar = new ToolBar(composite1, SWT.FLAT);
					menuToolBar.setLayoutData(menuToolBarLData);
					{
						newToolItem = new ToolItem(menuToolBar, SWT.PUSH);
						newToolItem.setToolTipText("Löscht die aktuellen Aufzeichnungen und legt einen leeren Datensatz an");
						newToolItem.setImage(SWTResourceManager.getImage("osde/resource/New.gif"));
						newToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("newToolItem.widgetSelected, event=" + evt);
								application.getDeviceSelectionDialog().setupDataChannels(application.getActiveConfig());
								application.updateDataTable();
								application.updateDigitalWindow();
							}
						});
					}
					{
						openToolItem = new ToolItem(menuToolBar, SWT.NONE);
						openToolItem.setToolTipText("Verwirft den aktuellen Datensatz und  ersetz ihn durch einen dem Dateiinhalt entsprechenden");
						openToolItem.setImage(SWTResourceManager.getImage("osde/resource/Open.gif"));
						openToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("openToolItem.widgetSelected, event=" + evt);
								//TODO add your code for openToolItem.widgetSelected
							}
						});
					}
					{
						saveToolItem = new ToolItem(menuToolBar, SWT.NONE);
						saveToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen in eine Datei");
						saveToolItem.setImage(SWTResourceManager.getImage("osde/resource/Save.gif"));
						saveToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("saveToolItem.widgetSelected, event=" + evt);
								//TODO add your code for saveToolItem.widgetSelected
							}
						});
					}
					{
						saveAsToolItem = new ToolItem(menuToolBar, SWT.NONE);
						saveAsToolItem.setToolTipText("Sichert die aktuellen Aufzeichnungen unter einem anzugebenden Namen");
						saveAsToolItem.setImage(SWTResourceManager.getImage("osde/resource/SaveAs.gif"));
						saveAsToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("saveAsToolItem.widgetSelected, event=" + evt);
								//TODO add your code for saveAsToolItem.widgetSelected
							}
						});
					}
					{
						separatorToolItem = new ToolItem(menuToolBar, SWT.SEPARATOR);
						separatorToolItem.setEnabled(false);
						separatorToolItem.setWidth(5);
						separatorToolItem.setImage(SWTResourceManager.getImage("osde/resource/SeparatorHorizontal.gif"));
					}
					{
						deviceToolItem = new ToolItem(menuToolBar, SWT.NONE);
						deviceToolItem.setToolTipText("Geräteauswahl mit Einstellungen");
						deviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/DeviceSelection.gif"));
						deviceToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("deviceToolItem.widgetSelected, event=" + evt);
								application.getDeviceSelectionDialog().open();
							}
						});
					}
					{
						prevDeviceToolItem = new ToolItem(menuToolBar, SWT.NONE);
						prevDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLeft.gif"));
						prevDeviceToolItem.setToolTipText("Schalte zum verhergehenden Gerät");
						prevDeviceToolItem.setWidth(20);
						prevDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldLefHot.gif"));
						prevDeviceToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("prevDeviceToolItem.widgetSelected, event=" + evt);
								if (application.getDeviceSerialPort() == null || !application.getDeviceSerialPort().isConnected()) { // allow device switch only if port noct connected
									DeviceConfiguration deviceConfig;
									DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size();
									if (selection > 0 && selection <= size) {
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection - 1));
									}
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(size - 1));

									if (application.getDeviceDialog() != null) application.getDeviceDialog().close();
									deviceSelect.setActiveConfig(deviceConfig);
									if (!deviceSelect.checkPortSelection()) application.setActiveConfig(application.getDeviceSelectionDialog().open());
									deviceSelect.setupDevice(deviceConfig);
									
									application.updateDataTable();
									application.updateDigitalWindow();
								}
								else {
									application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
								}
							}
						});
					}
					{
						nextDeviceToolItem = new ToolItem(menuToolBar, SWT.NONE);
						nextDeviceToolItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
						nextDeviceToolItem.setToolTipText("Schalte zum nachfolgenden Gerät");
						nextDeviceToolItem.setText("");
						nextDeviceToolItem.setWidth(20);
						nextDeviceToolItem.setHotImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRightHot.gif"));
						nextDeviceToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("nextDeviceToolItem.widgetSelected, event=" + evt);
								if (application.getDeviceSerialPort() == null || !application.getDeviceSerialPort().isConnected()) { // allow device switch only if port noct connected
									DeviceConfiguration deviceConfig;
									DeviceSelectionDialog deviceSelect = application.getDeviceSelectionDialog();
									int selection = deviceSelect.getActiveDevices().indexOf(deviceSelect.getActiveConfig().getName());
									int size = deviceSelect.getActiveDevices().size() - 1;
									if (selection >= 0 && selection < size)
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(selection + 1));
									else
										deviceConfig = deviceSelect.getDevices().get(deviceSelect.getActiveDevices().get(0));

									if (application.getDeviceDialog() != null) application.getDeviceDialog().close();
									deviceSelect.setActiveConfig(deviceConfig);
									if (!deviceSelect.checkPortSelection()) application.setActiveConfig(application.getDeviceSelectionDialog().open());
									deviceSelect.setupDevice(deviceConfig);
									
									application.updateDataTable();
									application.updateDigitalWindow();
								}
								else {
									application.openMessageDialog("Das Gerät kann nicht gewechselt werden, solange der serielle Port geöffnet ist!");
								}
							}
						});
					}
					{
						toolBoxToolItem = new ToolItem(menuToolBar, SWT.NONE);
						toolBoxToolItem.setImage(SWTResourceManager.getImage("osde/resource/Tools.gif"));
						toolBoxToolItem.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.finest("toolBoxToolItem.widgetSelected, event=" + evt);
								final OpenSerialDataExplorer inst = OpenSerialDataExplorer.getInstance();
								//if (inst.getDeviceSerialPort().isConnected()) {
								inst.getDeviceDialog().open();
								//}
								//else
								//	inst.openMessageDialog("Erst den seriellen Port öffnen");
							}
						});
					}
					{
						separatorToolItem = new ToolItem(menuToolBar, SWT.SEPARATOR);
						separatorToolItem.setWidth(5);
					}
					{
						new ToolItem(menuToolBar, SWT.NONE);
					}
					menuToolBar.pack();
				}
			}
			Point size = composite1.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			Point itemSize = menuCoolItem.computeSize(size.x, size.y);
			menuCoolItem.setSize(itemSize);
			menuCoolItem.setMinimumSize(itemSize);
			menuCoolItem.setPreferredSize(itemSize);

		}
	}

}
