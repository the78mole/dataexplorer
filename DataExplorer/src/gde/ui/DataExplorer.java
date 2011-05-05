/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.ui;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.ObjectData;
import gde.data.RecordSet;
import gde.device.DeviceDialog;
import gde.device.IDevice;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.dialog.AboutDialog;
import gde.ui.dialog.DeviceSelectionDialog;
import gde.ui.dialog.HelpInfoDialog;
import gde.ui.dialog.SettingsDialog;
import gde.ui.menu.MenuBar;
import gde.ui.menu.MenuToolBar;
import gde.ui.tab.AnalogWindow;
import gde.ui.tab.CellVoltageWindow;
import gde.ui.tab.DataTableWindow;
import gde.ui.tab.DigitalWindow;
import gde.ui.tab.FileCommentWindow;
import gde.ui.tab.GraphicsComposite;
import gde.ui.tab.GraphicsWindow;
import gde.ui.tab.ObjectDescriptionWindow;
import gde.ui.tab.StatisticsWindow;
import gde.ui.tab.UtilGraphicsWindow;
import gde.utils.FileUtils;
import gde.utils.OperatingSystemHelper;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;
import gde.utils.WebBrowser;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TaskBar;
import org.eclipse.swt.widgets.TaskItem;

/**
 * Main application class of DataExplorer
 * @author Winfried Brügmann
 */
public class DataExplorer extends Composite {
	final static String 					$CLASS_NAME 											= DataExplorer.class.getName();
	final static Logger						log																= Logger.getLogger(DataExplorer.class.getName());
	{
		SWTResourceManager.registerResourceUser(this);
	}
	
	final HashMap<String, String>	extensionFilterMap								= new HashMap<String, String>();

	public final static String		RECORD_NAME												= "recordName"; //$NON-NLS-1$
	public final static String		CURVE_SELECTION_ITEM							= "curveSelectedItem"; //$NON-NLS-1$
	public final static String		OLD_STATE													= "oldState"; //$NON-NLS-1$

	public final static Color			COLOR_WHITE												= SWTResourceManager.getColor(SWT.COLOR_WHITE);
	public final static Color			COLOR_LIGHT_GREY									= SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND);
	public final static Color			COLOR_GREY												= SWTResourceManager.getColor(SWT.COLOR_GRAY);
	public final static Color			COLOR_CANVAS_YELLOW								= SWTResourceManager.getColor(250, 249, 211);
	public final static Color			COLOR_BLUE												= SWTResourceManager.getColor(SWT.COLOR_BLUE);
	public final static Color			COLOR_DARK_GREEN									= SWTResourceManager.getColor(SWT.COLOR_DARK_GREEN);
	public final static Color			COLOR_BLACK												= SWTResourceManager.getColor(SWT.COLOR_BLACK);

	public final static int				TAB_INDEX_GRAPHIC									= 0;
	public final static int				TAB_INDEX_DATA_TABLE							= 1;
	public final static int				TAB_INDEX_DIGITAL									= 2;
	public final static int				TAB_INDEX_ANALOG									= 3;
	public final static int				TAB_INDEX_CELL_VOLTAGE						= 4;
	public final static int				TAB_INDEX_COMPARE									= 5;
	public final static int				TAB_INDEX_COMMENT									= 6;

	public final static String		COMPARE_RECORD_SET								= "compare_set"; //$NON-NLS-1$
	public final static String		UTILITY_RECORD_SET								= "utility_set"; //$NON-NLS-1$

	public static DataExplorer		application												= null;	

	gde.io.FileHandler						fileHandler;
	CTabFolder										displayTab;
	Settings											settings;
	IDevice												activeDevice											= null;
	Menu													menu;
	Label													filler;
	MenuBar												menuBar;
	CoolBar												menuCoolBar;
	int[]													order;
	int[]													wrapIndices;
	Point[]												sizes;

	MenuToolBar										menuToolBar;
	GraphicsWindow								graphicsTabItem;

	GraphicsWindow								compareTabItem;
	DataTableWindow								dataTableTabItem;
	StatisticsWindow							statisticsTabItem;
	DigitalWindow									digitalTabItem;
	AnalogWindow									analogTabItem;
	CellVoltageWindow							cellVoltageTabItem;
	FileCommentWindow							fileCommentTabItem;
	ObjectDescriptionWindow				objectDescriptionTabItem; 
	CTabItem											customTabItem;
	UtilGraphicsWindow						utilGraphicsTabItem;
	Composite											tabComposite;
	Composite											statusComposite;
	StatusBar											statusBar;
	int														progessPercentage									= 0;
	boolean												isDeviceDialogModal;

	SettingsDialog								settingsDialog;
	HelpInfoDialog								helpDialog;
	DeviceSelectionDialog					deviceSelectionDialog;

	Channels											channels;
	RecordSet											compareSet;
	RecordSet											utilitySet;
	final long										threadId;
	String												progressBarUser = null;
	TaskItem											taskBarItem;

	boolean												isRecordCommentVisible						= false;
	boolean												isGraphicsHeaderVisible						= false;
	boolean												isObjectWindowVisible							= false;

	int														openYesNoMessageDialogAsyncValue	= -1;

	DropTarget										target;																																												// = new DropTarget(dropTable, operations);

	final FileTransfer						fileTransfer											= FileTransfer.getInstance();
	Transfer[]										types															= new Transfer[] { this.fileTransfer};
	
	/**
	 * main application class constructor
	 * @param parent
	 * @param style
	 * @throws MalformedURLException
	 */
	private DataExplorer() {
		super(GDE.shell, SWT.NONE);
		this.threadId = Thread.currentThread().getId();
		
		this.extensionFilterMap.put(GDE.FILE_ENDING_OSD, Messages.getString(MessageIds.GDE_MSGT0139));
		this.extensionFilterMap.put(GDE.FILE_ENDING_LOV, Messages.getString(MessageIds.GDE_MSGT0140));
		this.extensionFilterMap.put(GDE.FILE_ENDING_CSV, Messages.getString(MessageIds.GDE_MSGT0141));
		this.extensionFilterMap.put(GDE.FILE_ENDING_XML, Messages.getString(MessageIds.GDE_MSGT0142));
		this.extensionFilterMap.put(GDE.FILE_ENDING_PNG, Messages.getString(MessageIds.GDE_MSGT0213));
		this.extensionFilterMap.put(GDE.FILE_ENDING_GIF, Messages.getString(MessageIds.GDE_MSGT0214));
		this.extensionFilterMap.put(GDE.FILE_ENDING_JPG, Messages.getString(MessageIds.GDE_MSGT0215));
		this.extensionFilterMap.put(GDE.FILE_ENDING_KMZ, Messages.getString(MessageIds.GDE_MSGT0222));
//		this.extensionFilterMap.put(GDE.FILE_ENDING_GPX, Messages.getString(MessageIds.GDE_MSGT0223));
		this.extensionFilterMap.put(GDE.FILE_ENDING_STAR, Messages.getString(MessageIds.GDE_MSGT0216));
		this.extensionFilterMap.put(GDE.FILE_ENDING_INI, Messages.getString(MessageIds.GDE_MSGT0368));
	}

	/**
	 * get the instance of singleton DataExplorer
	 */
	public static DataExplorer getInstance() {
		if (DataExplorer.application == null) {
			application = new DataExplorer();
		}
		return DataExplorer.application;
	}

	/**
	 * Initializes the GUI.
	 */
	private void initGUI() {
		//final String $METHOD_NAME = "initGUI"; //$NON-NLS-1$
		try {
			this.setBackground(DataExplorer.COLOR_GREY);
			this.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL, false, false));

			GridLayout thisLayout = new GridLayout(1, true);
			thisLayout.marginWidth = 0;
			thisLayout.marginHeight = 0;
			thisLayout.numColumns = 1;
			thisLayout.makeColumnsEqualWidth = true;
			thisLayout.horizontalSpacing = 0;
			thisLayout.verticalSpacing = 0;
			this.setLayout(thisLayout);
			{
				this.menu = new Menu(GDE.shell, SWT.BAR);
				this.menuBar = new MenuBar(this, this.menu);
				this.menuBar.create();
				GDE.shell.setMenuBar(this.menu);
			}
			{
				this.filler = new Label(this, SWT.SEPARATOR | SWT.HORIZONTAL);
				GridData label1LData = new GridData();
				label1LData.horizontalAlignment = GridData.FILL;
				label1LData.grabExcessHorizontalSpace = true;
				this.filler.setLayoutData(label1LData);
			}
			{
				this.menuCoolBar = new CoolBar(this, SWT.FLAT);
				GridData menuCoolBarLData = new GridData();
				menuCoolBarLData.horizontalAlignment = GridData.FILL;
				menuCoolBarLData.verticalAlignment = GridData.BEGINNING;
				menuCoolBarLData.grabExcessHorizontalSpace = true;
				this.menuCoolBar.setLayoutData(menuCoolBarLData);
				//this.menuCoolBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				this.menuToolBar = new MenuToolBar(this, this.menuCoolBar);
				this.menuToolBar.create();
				// restore cool bar items position and wrap
				this.menuCoolBar.setItemLayout(this.settings.getCoolBarOrder(), this.settings.getCoolBarWraps(), this.settings.getCoolBarSizes());
			}
			{ // begin main tab display
				this.displayTab = new CTabFolder(this, SWT.BORDER);
				GridData tabCompositeLData = new GridData();
				tabCompositeLData.verticalAlignment = GridData.FILL;
				tabCompositeLData.horizontalAlignment = GridData.FILL;
				tabCompositeLData.grabExcessHorizontalSpace = true;
				tabCompositeLData.grabExcessVerticalSpace = true;
				this.displayTab.setLayoutData(tabCompositeLData);
				this.displayTab.setSimple(false);
				{
					this.graphicsTabItem = new GraphicsWindow(this.displayTab, SWT.NONE, GraphicsWindow.TYPE_NORMAL, Messages.getString(MessageIds.GDE_MSGT0143), 0);
					this.graphicsTabItem.create();
				}
				this.displayTab.setSelection(0);
			} // end main tab display
			{
				this.statusComposite = new Composite(this, SWT.NONE);
				GridData statusCompositeLData = new GridData();
				statusCompositeLData.grabExcessHorizontalSpace = true;
				statusCompositeLData.horizontalAlignment = GridData.FILL;
				statusCompositeLData.verticalAlignment = GridData.END;
				this.statusComposite.setLayoutData(statusCompositeLData);
				RowLayout statusCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.statusComposite.setLayout(statusCompositeLayout);
				this.statusComposite.layout();
				{
					this.statusBar = new StatusBar(this.statusComposite);
					this.statusBar.create();
				}
			}
			this.layout();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * execute OpenSerielDataExplorer
	 */
	public void execute(final String inputFilePath) {
		final String $METHOD_NAME = "execute"; //$NON-NLS-1$
		try {
			//cleanup possible old files and native libraries
			FileUtils.cleanupPre();

			//init settings
			this.settings = Settings.getInstance();
			log.logp(Level.INFO, $CLASS_NAME, $METHOD_NAME, this.settings.toString());

			this.isDeviceDialogModal = this.settings.isDeviceDialogsModal();

			if (this.settings.isWindowMaximized()) {
				GDE.shell.setLocation(this.settings.getWindow().x, this.settings.getWindow().y);
				GDE.shell.setSize(this.settings.getWindow().width, this.settings.getWindow().height);
				GDE.shell.setMaximized(true);
			}
			else {
				Rectangle displayBounds = GDE.display.getBounds();
				if (this.settings.getWindow().x < displayBounds.x || this.settings.getWindow().x > (displayBounds.width + displayBounds.x) 				 // check location x,y inside display bounds
						|| this.settings.getWindow().y < displayBounds.y || this.settings.getWindow().y > (displayBounds.height + displayBounds.y)) {
					GDE.shell.setLocation(50, 50);
					GDE.shell.setSize(this.settings.getWindow().width, this.settings.getWindow().height);
				}
				else {
					GDE.shell.setBounds(this.settings.getWindow());
				}
			}
			
			this.fileHandler = new gde.io.FileHandler();
			this.initGUI();

			this.channels = Channels.getInstance(this);
			//this.compareSet = new RecordSet(null, GDE.STRING_EMPTY, DataExplorer.COMPARE_RECORD_SET, 1);
			//this.utilitySet = new RecordSet(null, GDE.STRING_EMPTY, DataExplorer.UTILITY_RECORD_SET, 1);

			GDE.shell.setLayout(new FillLayout());
			GDE.shell.setImage(SWTResourceManager.getImage(GDE.IS_MAC ? "gde/resource/DataExplorer_MAC.png" : "gde/resource/DataExplorer.png")); //$NON-NLS-1$ //$NON-NLS-2$
			GDE.shell.setText(GDE.NAME_LONG);

			GDE.seStartupProgress(100);
			GDE.shell.layout();
			GDE.shell.open();
			if (GDE.splash != null) GDE.splash.dispose();
			TaskBar taskBar = GDE.display.getSystemTaskBar();
			if (taskBar == null)
				this.taskBarItem = null;
			else {
				this.taskBarItem = taskBar.getItem(GDE.shell) != null ? taskBar.getItem(GDE.shell) : taskBar.getItem(null);
			}


			if (this.settings.isDevicePropertiesUpdated() || this.settings.isGraphicsTemplateUpdated() || this.settings.isDevicePropertiesReplaced()) {
				StringBuilder sb = new StringBuilder();
				if (this.settings.isDevicePropertiesUpdated()) sb.append(Messages.getString(MessageIds.GDE_MSGI0016)).append(GDE.STRING_NEW_LINE);
				if (this.settings.isGraphicsTemplateUpdated()) sb.append(Messages.getString(MessageIds.GDE_MSGI0017)).append(GDE.STRING_NEW_LINE);
				if (this.settings.isDevicePropertiesReplaced()) sb.append(Messages.getString(MessageIds.GDE_MSGI0028)).append(GDE.STRING_NEW_LINE);
				application.openMessageDialog(GDE.shell, sb.toString());
			}

			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.postInitGUI(inputFilePath);

					for (String errorMessage : GDE.getInitErrors()) {
						MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
						if (errorMessage.contains(GDE.STRING_SEMICOLON)) {
							String[] messages = errorMessage.split(GDE.STRING_SEMICOLON);
							messageDialog.setText(messages[0]);
							messageDialog.setMessage(messages[1]);
						}
						else {
							messageDialog.setText(GDE.NAME_LONG);
							messageDialog.setMessage(errorMessage);
						}
						messageDialog.open();
					}
				}
			});
			
			if (log.isLoggable(Level.TIME)) log.logp(Level.TIME, DataExplorer.$CLASS_NAME, $METHOD_NAME, "total init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			while (!GDE.shell.isDisposed()) {
				if (!GDE.display.readAndDispatch()) GDE.display.sleep();
			}
		}
		catch (Throwable t) {
			log.log(Level.SEVERE, t.getMessage(), t);
			t.printStackTrace(System.err);
		}
		
		//cleanup out dated resources
		FileUtils.cleanupPost();
	}

	/**
	 * init/update logger
	 */
	private void updateLogger() {
		Handler logHandler;
		LogFormatter lf = new LogFormatter();
		Logger rootLogger = Logger.getLogger(GDE.STRING_EMPTY);

		// cleanup previous log handler
		rootLogger.removeHandler(GDE.logHandler);

		if (System.getProperty(GDE.ECLIPSE_STRING) == null) { // running outside eclipse
			try {
				logHandler = new FileHandler(this.settings.getLogFilePath(), 5000000, 3);
				rootLogger.addHandler(logHandler);
				logHandler.setFormatter(lf);
				logHandler.setLevel(Level.ALL);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			logHandler = new ConsoleHandler();
			rootLogger.addHandler(logHandler);
			logHandler.setFormatter(lf);
			logHandler.setLevel(Level.ALL);
		}
		// set logging levels
		this.settings.updateLogLevel();
	}

	/**
	 * Add your post-init code in here
	 */
	private void postInitGUI(final String inputFilePath) {
		final String $METHOD_NAME = "postInitGUI"; //$NON-NLS-1$
		try {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "init tabs"); //$NON-NLS-1$
			this.statisticsTabItem = new StatisticsWindow(this.displayTab, SWT.NONE);
			this.statisticsTabItem.create();

			// initialization of table, digital, analog and cell voltage are done while initializing the device
						
			this.fileCommentTabItem = new FileCommentWindow(this.displayTab, SWT.NONE);
			this.fileCommentTabItem.create();

			//createCompareWindowTabItem();

			this.setObjectDescriptionTabVisible(this.menuToolBar.isObjectoriented());
			
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "init listener"); //$NON-NLS-1$
			GDE.shell.addListener(SWT.Close, new Listener() {
				public void handleEvent(Event evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, GDE.shell.getLocation().toString() + "event = " + evt); //$NON-NLS-1$

					// checkk all data saved - prevent closing application
					evt.doit = getDeviceSelectionDialog().checkDataSaved();
				}
			});
			GDE.shell.addControlListener(new ControlListener() {			
				public void controlResized(ControlEvent controlevent) {
					if (log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, "controlResized", GDE.shell.getLocation().toString() + "event = " + controlevent); //$NON-NLS-1$ //$NON-NLS-2$
					DataExplorer.application.settings.setWindowMaximized(GDE.shell.getMaximized());
					if (!DataExplorer.application.settings.isWindowMaximized()) {
						DataExplorer.application.settings.setWindow(GDE.shell.getLocation(), GDE.shell.getSize());
					}
				}		
				public void controlMoved(ControlEvent controlevent) {
					if (log.isLoggable(Level.FINEST)) log.logp(Level.FINEST, $CLASS_NAME, "controlResized", GDE.shell.getLocation().toString() + "event = " + controlevent); //$NON-NLS-1$ //$NON-NLS-2$
					if (!GDE.shell.getMaximized()) 
						DataExplorer.application.settings.setWindow(GDE.shell.getLocation(), GDE.shell.getSize());
				}
			});
			this.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "widgetDisposed", GDE.shell.getLocation().toString() + "event = " + evt); //$NON-NLS-1$ //$NON-NLS-2$
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, "widgetDisposed", GDE.shell.getSize().toString()); //$NON-NLS-1$
					//cleanup
					// if help browser is open, dispose it
					if (DataExplorer.this.helpDialog != null && !DataExplorer.this.helpDialog.isDisposed()) {
						DataExplorer.this.helpDialog.dispose();
					}
					if (DataExplorer.application.getActiveDevice() != null) {
						DataExplorer.application.getActiveDevice().storeDeviceProperties();
						
						if (DataExplorer.application.getDeviceDialog() != null && !DataExplorer.application.getDeviceDialog().isDisposed()) {// if a device tool box is open, dispose it
							DataExplorer.application.getDeviceDialog().forceDispose();
						}					
						//close open communication ports
						if (DataExplorer.application.getActiveDevice().getCommunicationPort() != null) {// if serial port still open, close it
							DataExplorer.application.getActiveDevice().getCommunicationPort().close();
							DataExplorer.application.getActiveDevice().storeDeviceProperties();
						}
					}
					// query the item definition to save it for restore option 
					DataExplorer.this.order = DataExplorer.this.menuCoolBar.getItemOrder();
					DataExplorer.this.wrapIndices = DataExplorer.this.menuCoolBar.getWrapIndices();
					if (DataExplorer.this.wrapIndices.length > 0) {
						if (DataExplorer.this.wrapIndices[0] != 0) {
							int[] newWraps = new int[DataExplorer.this.wrapIndices.length + 1];
							for (int i = 0; i < DataExplorer.this.wrapIndices.length; i++) {
								newWraps[i + 1] = DataExplorer.this.wrapIndices[i];
							}
							DataExplorer.this.wrapIndices = newWraps;
						}
					}
					DataExplorer.this.sizes = DataExplorer.this.menuCoolBar.getItemSizes();
					DataExplorer.application.settings.setCoolBarStates(DataExplorer.this.order, DataExplorer.this.wrapIndices, DataExplorer.this.sizes);

					if (DataExplorer.this.objectDescriptionTabItem != null && !DataExplorer.this.objectDescriptionTabItem.isDisposed()) {
						DataExplorer.this.objectDescriptionTabItem.checkSaveObjectData(); // check if data needs to be saved
					}
					
					// finally save application settings
					DataExplorer.application.settings.store();
				}
			});
			this.menuCoolBar.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent evt) {
					log.logp(Level.FINEST, $CLASS_NAME, $METHOD_NAME, "menuCoolBar.controlResized, event=" + evt); //$NON-NLS-1$
					// menuCoolBar.controlResized signals collBar item moved
					if (DataExplorer.this.displayTab != null && getSize().y != 0) {
						Point fillerSize = DataExplorer.this.filler.getSize();
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "filler.size = " + fillerSize); //$NON-NLS-1$
						Point menuCoolBarSize = DataExplorer.this.menuCoolBar.getSize();
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "menuCoolBar.size = " + menuCoolBarSize); //$NON-NLS-1$
						Point shellSize = new Point(getClientArea().width, getClientArea().height);
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "shellClient.size = " + shellSize); //$NON-NLS-1$
						Point statusBarSize = DataExplorer.this.statusComposite.getSize();
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "statusBar.size = " + statusBarSize); //$NON-NLS-1$
						DataExplorer.this.displayTab.setBounds(0, menuCoolBarSize.y + fillerSize.y, shellSize.x, shellSize.y - menuCoolBarSize.y - statusBarSize.y - fillerSize.y);
						if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "displayTab.bounds = " + DataExplorer.this.displayTab.getBounds()); //$NON-NLS-1$
					}
				}
			});
			this.displayTab.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME,
							"displayTab.paintControl " + DataExplorer.this.displayTab.getItems()[DataExplorer.this.displayTab.getSelectionIndex()].getText() //$NON-NLS-1$
									+ GDE.STRING_MESSAGE_CONCAT + DataExplorer.this.displayTab.getSelectionIndex() + GDE.STRING_MESSAGE_CONCAT + evt);
					if (isRecordSetVisible(GraphicsWindow.TYPE_NORMAL)) {
						if (DataExplorer.this.graphicsTabItem.isCurveSelectorEnabled()) {
							DataExplorer.this.graphicsTabItem.setSashFormWeights(DataExplorer.this.graphicsTabItem.getCurveSelectorComposite().getSelectorColumnWidth());
						}
						else {
							DataExplorer.this.graphicsTabItem.setSashFormWeights(0);
						}
					}
					else if (isRecordSetVisible(GraphicsWindow.TYPE_COMPARE)) {
						DataExplorer.this.compareTabItem.setSashFormWeights(DataExplorer.this.compareTabItem.getCurveSelectorComposite().getSelectorColumnWidth());
					}
					if (DataExplorer.this.objectDescriptionTabItem != null) {
						if (DataExplorer.this.objectDescriptionTabItem.isVisible()) {
							log.log(Level.FINEST, "displayTab.focusGained " + evt); //$NON-NLS-1$
							DataExplorer.this.isObjectWindowVisible = true;
						}
						else if (DataExplorer.this.isObjectWindowVisible) {
							log.log(Level.FINEST, "displayTab.focusLost " + evt); //$NON-NLS-1$
							DataExplorer.this.checkSaveObjectData();
							DataExplorer.this.isObjectWindowVisible = false;
						}
					}
				}
			});
			this.displayTab.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "addSelectionListener, event=" + evt); //$NON-NLS-1$
					CTabFolder tabFolder = (CTabFolder) evt.widget;
					int tabSelectionIndex = tabFolder.getSelectionIndex();
					if (tabSelectionIndex == 0) {
						DataExplorer.this.menuToolBar.enableScopePointsCombo(true);
					}
					else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) 
							&& DataExplorer.this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE)) {
						DataExplorer.this.menuToolBar.enableScopePointsCombo(false);
					}
				}
			});
			// drag filePath support
			this.target = new DropTarget(this, DND.DROP_COPY | DND.DROP_DEFAULT);
			this.target.setTransfer(this.types);
			this.target.addDropListener(new DropTargetAdapter() {
				@Override
				public void dragEnter(DropTargetEvent event) {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_COPY) != 0) {
							event.detail = DND.DROP_COPY;
						}
						else {
							event.detail = DND.DROP_NONE;
						}
					}
					for (TransferData element : event.dataTypes) {
						if (DataExplorer.this.fileTransfer.isSupportedType(element)) {
							event.currentDataType = element;
							if (event.detail != DND.DROP_COPY) {
								event.detail = DND.DROP_NONE;
							}
							break;
						}
					}
				}

				@Override
				public void dragOperationChanged(DropTargetEvent event) {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_COPY) != 0) {
							event.detail = DND.DROP_COPY;
						}
						else {
							event.detail = DND.DROP_NONE;
						}
					}
					if (DataExplorer.this.fileTransfer.isSupportedType(event.currentDataType)) {
						if (event.detail != DND.DROP_COPY) {
							event.detail = DND.DROP_NONE;
						}
					}
				}

				@Override
				public void drop(DropTargetEvent event) {
					if (DataExplorer.this.fileTransfer.isSupportedType(event.currentDataType)) {
						String[] files = (String[]) event.data;
						for (String filePath : files) {
							if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "dropped file = " + filePath); //$NON-NLS-1$
							if (filePath.toLowerCase().endsWith(GDE.FILE_ENDING_OSD)) {
								DataExplorer.this.fileHandler.openOsdFile(filePath);
							}
							else if (filePath.toLowerCase().endsWith(GDE.FILE_ENDING_LOV)) {
								DataExplorer.this.fileHandler.openLovFile(filePath);
							}
							else {
								application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0022));
							}
						}
					}
				}
			});

			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "init help listener"); //$NON-NLS-1$
			this.menuCoolBar.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "this.helpRequested, event=" + evt); //$NON-NLS-1$
					DataExplorer.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_3.html"); //$NON-NLS-1$
				}
			});
			this.menu.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "this.helpRequested, event=" + evt); //$NON-NLS-1$
					DataExplorer.application.openHelpDialog(GDE.STRING_EMPTY, "HelpInfo_3.html"); //$NON-NLS-1$
				}
			});

			//restore window settings
			this.isRecordCommentVisible = this.settings.isRecordCommentVisible();
			if (this.isRecordCommentVisible) {
				this.menuBar.setRecordCommentMenuItemSelection(this.isRecordCommentVisible);
				this.enableRecordSetComment(this.isRecordCommentVisible);
			}
			this.isGraphicsHeaderVisible = this.settings.isGraphicsHeaderVisible();
			if (this.isGraphicsHeaderVisible) {
				this.menuBar.setGraphicsHeaderMenuItemSelection(this.isGraphicsHeaderVisible);
				this.enableGraphicsHeader(this.isGraphicsHeaderVisible);
			}

			this.deviceSelectionDialog = new DeviceSelectionDialog(GDE.shell, SWT.PRIMARY_MODAL, this);

			if (!this.settings.isDesktopShortcutCreated()) {
				this.settings.setProperty(Settings.IS_DESKTOP_SHORTCUT_CREATED, GDE.STRING_EMPTY + OperatingSystemHelper.createDesktopLink());
			}

			if (!this.settings.isApplicationRegistered()) {
				this.settings.setProperty(Settings.IS_APPL_REGISTERED, GDE.STRING_EMPTY + OperatingSystemHelper.registerApplication());
			}

			if ((GDE.IS_MAC || GDE.IS_LINUX) && !this.settings.isLockUucpHinted()) {
				if (GDE.IS_MAC && !OperatingSystemHelper.isUucpMember()) 
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0046));
				else if (GDE.IS_LINUX && !OperatingSystemHelper.isUucpMember()) 
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGI0045));
				this.settings.setProperty(Settings.IS_LOCK_UUCP_HINTED, "true"); //$NON-NLS-1$
			}

			// check initial application settings
			if (!this.settings.isOK()) {
				this.openSettingsDialog();
			}
			// check configured device
			if (this.settings.getActiveDevice().equals(Settings.EMPTY)) { //$NON-NLS-1$
				this.deviceSelectionDialog = new DeviceSelectionDialog(GDE.shell, SWT.PRIMARY_MODAL, this);
				this.deviceSelectionDialog.open();
			}
			else {
				// channels HashMap will filled with empty records matching the active device, the dummy content is replaced
				this.deviceSelectionDialog.setupDevice();
			}

			this.updateLogger();

			if (inputFilePath.length() > 5) {
				if (inputFilePath.endsWith(GDE.FILE_ENDING_OSD))
					this.fileHandler.openOsdFile(inputFilePath);
				else if (inputFilePath.endsWith(GDE.FILE_ENDING_LOV)) this.fileHandler.openLovFile(inputFilePath);
			}
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + e.getMessage());
		}
	}

	/**
	 * updates the statistics window using current record set data
	 */
	public void updateStatisticsData() {
		if (this.statisticsTabItem != null && !this.statisticsTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.statisticsTabItem.updateStatisticsData(true);
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.statisticsTabItem.updateStatisticsData(true);
					}
				});
			}
		}
	}

	/**
	 * updates the statistics window using current record set data
	 * @param forceUpdate
	 */
	public void updateStatisticsData(final boolean forceUpdate) {
		if (this.statisticsTabItem != null && !this.statisticsTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.statisticsTabItem.updateStatisticsData(forceUpdate);
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.statisticsTabItem.updateStatisticsData(forceUpdate);
					}
				});
			}
		}
	}

	/**
	 * setup the data table header with current record set data
	 */
	public void setupDataTableHeader() {
		if (this.dataTableTabItem != null && !this.dataTableTabItem.isDisposed()) this.dataTableTabItem.setHeader();
	}

	/**
	 * updates the data table with current record set data
	 * @param requestingRecordSetName
	 */
	public synchronized void updateDataTable(String requestingRecordSetName, boolean forceClean) {
		final Channel activeChannel = this.channels != null ? this.channels.getActiveChannel() : null;
		final RecordSet activeRecordSet = activeChannel != null ? activeChannel.getActiveRecordSet() : null;

		if (activeRecordSet != null && this.dataTableTabItem != null && !this.dataTableTabItem.isDisposed()
				&& activeRecordSet.getName().equals(requestingRecordSetName)
				&& activeRecordSet.getDevice().isTableTabRequested()
				&& activeRecordSet.checkAllRecordsDisplayable()) {
			if (forceClean) {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.dataTableTabItem.setHeader();
						DataExplorer.this.dataTableTabItem.cleanTable();
					}
				});
			}
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.dataTableTabItem.setRowCount(activeRecordSet.getRecordDataSize(true));	
						DataExplorer.this.dataTableTabItem.updateTopIndex();
					}
				});
		}
		else {
			if (activeRecordSet == null || requestingRecordSetName.equals(GDE.STRING_EMPTY)) {
				if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
					if (this.dataTableTabItem != null) {
						//this.dataTableTabItem.setHeader();
						this.dataTableTabItem.cleanTable();
					}
				}
				else {
					GDE.display.asyncExec(new Runnable() {
						public void run() {
							if (DataExplorer.this.dataTableTabItem != null) {
							//DataExplorer.this.dataTableTabItem.setHeader();
							DataExplorer.this.dataTableTabItem.cleanTable();
							}
						}
					});
				}
			}
		}	
	}

	/**
	 * updates the digital window children displays with current record set data
	 */
	public void updateDigitalWindow() {
		if (this.digitalTabItem != null && !this.digitalTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.digitalTabItem.update(true);
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.digitalTabItem.update(true);
					}
				});
			}
		}
	}

	/**
	 * updates the digital window children displays with current record set data
	 */
	public void updateDigitalWindowChilds() {
		if (this.digitalTabItem != null && !this.digitalTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.digitalTabItem.updateChilds();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.digitalTabItem.updateChilds();
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void updateAnalogWindow() {
		if (this.analogTabItem != null && !this.analogTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.analogTabItem.update(true);
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.analogTabItem.update(true);
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void updateAnalogWindowChilds() {
		if (this.analogTabItem != null && !this.analogTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.analogTabItem.updateChilds();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.analogTabItem.updateChilds();
					}
				});
			}
		}
	}

	/**
	 * updates the cell voltage window display measurement ordinal
	 * @param measurementOrdinals {firstMeasurementOrdinal, secondMeasurementOrdinal}
	 */
	public void setCellVoltageWindowOrdinal(int[] measurementOrdinals) {
		if (this.cellVoltageTabItem != null) this.cellVoltageTabItem.setMeasurements(measurementOrdinals[0], measurementOrdinals[1]);
	}

	/**
	 * updates the cell voltage window
	 */
	public void updateCellVoltageWindow() {
		if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.cellVoltageTabItem.getCellVoltageMainComposite().redraw();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.cellVoltageTabItem.getCellVoltageMainComposite().redraw();
					}
				});
			}
		}
	}

	/**
	 * updates the cell voltage window children displays according to current record set data
	 */
	public void updateCellVoltageChilds() {
		if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.cellVoltageTabItem.updateChilds();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.cellVoltageTabItem.updateChilds();
					}
				});
			}
		}
	}

	/**
	 * updates the cell voltage limits selector group
	 */
	public void updateCellVoltageLimitsSelector() {
		if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.cellVoltageTabItem.updateVoltageLimitsSelection();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.cellVoltageTabItem.updateVoltageLimitsSelection();
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void updateFileCommentWindow() {
		if (this.fileCommentTabItem != null) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.fileCommentTabItem.update();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.fileCommentTabItem.update();
					}
				});
			}
		}
	}
	
	/**
	 * updates the object describtion window with current object data
	 */
	public void updateObjectDescriptionWindow() {
		if (this.objectDescriptionTabItem != null && !this.objectDescriptionTabItem.isDisposed()) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.objectDescriptionTabItem.update();
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.objectDescriptionTabItem.update();
					}
				});
			}
		}
	}

	/**
	 * updates the analog window children displays with current record set data
	 */
	public void cleanHeaderAndCommentInGraphicsWindow() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.graphicsTabItem.clearHeaderAndComment();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.graphicsTabItem.clearHeaderAndComment();
				}
			});
		}
	}

	public DeviceSelectionDialog getDeviceSelectionDialog() {
		return this.deviceSelectionDialog;
	}

	public void setStatusMessage(final String message, final int swtColor) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.statusBar.setMessage(message, swtColor);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.statusBar.setMessage(message, swtColor);
				}
			});
		}
	}

	public void setStatusMessage(final String message) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.statusBar.setMessage(message);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.statusBar.setMessage(message);
				}
			});
		}
	}

	/**
	 * set the progress bar percentage, only one process can use the progress bar at one cycle to show its progress - therefore
	 * if a percentage value of 0 or >99 is specified the user will be reset to enable usage of progress bar for another process user
	 * else the progress bar will check user for user equality, not equal will skip processing
	 * @param percentage
	 * @param user
	 */
	public void setProgress(final int percentage, final String user) {
		if (this.progressBarUser == null || user == null || this.progressBarUser.equals(user)) {
			if (percentage > 99 | percentage == 0) 	this.progressBarUser = null;
			else																		this.progressBarUser = user;

			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				this.statusBar.setProgress(percentage);
				if (this.taskBarItem != null) {
					if (user == null) this.taskBarItem.setProgressState(SWT.DEFAULT);
					else this.taskBarItem.setProgressState(GDE.IS_MAC?SWT.PAUSED:SWT.NORMAL);
					
					this.taskBarItem.setProgress(percentage);						
				}
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						DataExplorer.this.statusBar.setProgress(percentage);
						if (DataExplorer.this.taskBarItem != null) {
							if (user == null) DataExplorer.this.taskBarItem.setProgressState(SWT.DEFAULT);
							else DataExplorer.this.taskBarItem.setProgressState(GDE.IS_MAC?SWT.PAUSED:SWT.NORMAL);
							
							DataExplorer.this.taskBarItem.setProgress(percentage);						
						}
					}
				});
			}
			this.progessPercentage = percentage;
		}
	}

	public int getProgressPercentage() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.progessPercentage = this.statusBar.getProgressPercentage();
		}
		else { // if the percentage is not up to date it will updated later
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.progessPercentage = DataExplorer.this.statusBar.getProgressPercentage();
				}
			});
		}
		return this.progessPercentage == 100 ? 0 : this.progessPercentage;
	}

	public void setSerialTxOn() {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				DataExplorer.this.statusBar.setSerialTxOn();
			}
		});
	}

	public void setSerialTxOff() {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				DataExplorer.this.statusBar.setSerialTxOff();
			}
		});
	}

	public void setSerialRxOn() {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				DataExplorer.this.statusBar.setSerialRxOff();
				DataExplorer.this.statusBar.setSerialRxOn();
			}
		});
	}

	public void setSerialRxOff() {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				DataExplorer.this.statusBar.setSerialRxOn();
				DataExplorer.this.statusBar.setSerialRxOff();
			}
		});
	}

	public IDevice getActiveDevice() {
		return this.activeDevice;
	}

	public void openDeviceDialog() {
		if (DataExplorer.this.getDeviceDialog() != null) {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.getDeviceDialog().open();
				}
			});
		}
	}

	public void setGloabalSerialPort(String newPort) {
		if (this.activeDevice != null) {
			this.activeDevice.setPort(newPort);
			this.settings.setActiveDevice(this.activeDevice.getName() + GDE.STRING_SEMICOLON + this.activeDevice.getManufacturer() + GDE.STRING_SEMICOLON + this.activeDevice.getPort());
			this.updateTitleBar(this.getObjectKey(), this.activeDevice.getName(), this.activeDevice.getPort());
		}
		else {
			this.deviceSelectionDialog.getActiveConfig().setPort(newPort);
			this.updateTitleBar(this.getObjectKey(), this.settings.getActiveDevice(), this.settings.getSerialPort());
		}
	}
	
	/**
	 * set the active device in main settings
	 * @param device
	 */
	public void setActiveDeviceWoutUI(IDevice device) {
		if (device != null) {
			if (this.activeDevice == null || !this.activeDevice.getName().equals(device.getName())) this.activeDevice = device;
		}
	}

	/**
	 * set the active device in main settings
	 * @param device
	 */
	public void setActiveDevice(IDevice device) {
		if (device != null) {
			if (this.activeDevice == null || !this.activeDevice.getName().equals(device.getName())) this.activeDevice = device;
			// do always update, the port might be changed
			this.settings.setActiveDevice(device.getName() + GDE.STRING_SEMICOLON + device.getManufacturer() + GDE.STRING_SEMICOLON + device.getPort());
			this.updateTitleBar(this.getObjectKey(), device.getName(), device.getPort());
			if (this.deviceSelectionDialog.getNumberOfActiveDevices() > 1)
				this.enableDeviceSwitchButtons(true);
			else
				this.enableDeviceSwitchButtons(false);
		}
		else { // no device
			this.settings.setActiveDevice(Settings.EMPTY_SIGNATURE);
			this.updateTitleBar(this.getObjectKey(), Messages.getString(MessageIds.GDE_MSGI0023), GDE.STRING_EMPTY);
			this.activeDevice = null;
			this.channels.cleanup();
			this.enableDeviceSwitchButtons(false);
		}
		
		//cleanup device specific utility graphics tab item
		if (this.utilGraphicsTabItem != null) {
			this.utilGraphicsTabItem.dispose();
			this.utilGraphicsTabItem = null;
		}
		//cleanup device specific custom tab item
		if (this.customTabItem != null && !this.customTabItem.isDisposed()) {
			this.customTabItem.dispose();
			this.customTabItem = null;		
		}
	}

	public void updateTitleBar(final String objectName, final String deviceName, final String devicePort) {
		StringBuilder sb = new StringBuilder().append(GDE.NAME_LONG);
		String separator = "  -  "; //$NON-NLS-1$
		String actualFileName = (this.channels != null && this.channels.getActiveChannel() != null) ? this.channels.getActiveChannel().getFileName() : null;
		if (actualFileName != null && actualFileName.length() > 4) sb.append(separator).append(actualFileName);
		if (objectName != null && objectName.length() > 0 
				&& !(actualFileName != null && actualFileName.length() > 4 && actualFileName.contains(objectName))
				&& !objectName.startsWith(Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0])) 
			sb.append(separator).append(objectName);
		if (deviceName != null && deviceName.length() > 0) sb.append(separator).append(deviceName);
		if (devicePort != null && devicePort.length() > 0) sb.append(separator).append(devicePort);
		final String headerText = sb.toString();

		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			GDE.shell.setText(headerText);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					GDE.shell.setText(headerText);
				}
			});
		}
	}
	
	public void updateTitleBar() {
		final IDevice actualDevice = this.getActiveDevice();
		if (actualDevice != null) {
			if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
				updateTitleBar(this.getObjectKey(), actualDevice.getName(), actualDevice.getPort());
			}
			else {
				GDE.display.asyncExec(new Runnable() {
					public void run() {
						updateTitleBar(DataExplorer.this.getObjectKey(), actualDevice.getName(), actualDevice.getPort());
					}
				});
			}
		}
	}

	public void openMessageDialog(final String message) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
			messageDialog.setText(GDE.NAME_LONG);
			messageDialog.setMessage(message);
			messageDialog.open();
		}
		else {
			GDE.display.syncExec(new Runnable() {
				public void run() {
					MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
					messageDialog.setText(GDE.NAME_LONG);
					messageDialog.setMessage(message);
					messageDialog.open();
				}
			});
		}
	}

	public void openMessageDialog(final Shell parent, final String message) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			Shell useParent = (parent != null  && !parent.isDisposed()) ? parent : GDE.shell;
			MessageBox messageDialog = new MessageBox(useParent, SWT.OK | SWT.ICON_WARNING);
			messageDialog.setText(GDE.NAME_LONG);
			messageDialog.setMessage(message);
			messageDialog.open();
		}
		else {
			GDE.display.syncExec(new Runnable() {
				public void run() {
					// parent might be disposed ??
					Shell useParent = (parent != null  && !parent.isDisposed()) ? parent : GDE.shell;
					MessageBox messageDialog = new MessageBox(useParent, SWT.OK | SWT.ICON_WARNING);
					messageDialog.setText(GDE.NAME_LONG);
					messageDialog.setMessage(message);
					messageDialog.open();
				}
			});
		}
	}

	public void openMessageDialogAsync(final String message) {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				MessageBox messageDialog = new MessageBox(GDE.shell, SWT.OK | SWT.ICON_WARNING);
				messageDialog.setText(GDE.NAME_LONG);
				messageDialog.setMessage(message);
				messageDialog.open();
			}
		});
	}

	public void openMessageDialogAsync(Shell parent, final String message) {
		final Shell useParent = (parent != null  && !parent.isDisposed()) ? parent : GDE.shell;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				MessageBox messageDialog = new MessageBox(useParent, SWT.OK | SWT.ICON_WARNING | SWT.MODELESS);
				messageDialog.setText(GDE.NAME_LONG);
				messageDialog.setMessage(message);
				messageDialog.open();
			}
		});
	}

	public int openOkCancelMessageDialog(Shell parent, final String message) {
		final Shell useParent = (parent != null  && !parent.isDisposed()) ? parent : GDE.shell;
		MessageBox okCancelMessageDialog = new MessageBox(useParent, SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
		okCancelMessageDialog.setText(GDE.NAME_LONG);
		okCancelMessageDialog.setMessage(message);
		return okCancelMessageDialog.open();
	}

	public int openOkCancelMessageDialog(final String message) {
		MessageBox okCancelMessageDialog = new MessageBox(this.getShell(), SWT.PRIMARY_MODAL | SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
		okCancelMessageDialog.setText(GDE.NAME_LONG);
		okCancelMessageDialog.setMessage(message);
		return okCancelMessageDialog.open();
	}

	public int openYesNoMessageDialog(Shell parent, final String message) {
		final Shell useParent = (parent != null  && !parent.isDisposed()) ? parent : GDE.shell;
		MessageBox yesNoMessageDialog = new MessageBox(useParent, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		yesNoMessageDialog.setText(GDE.NAME_LONG);
		yesNoMessageDialog.setMessage(message);
		return yesNoMessageDialog.open();
	}

	public int openYesNoMessageDialog(final String message) {
		MessageBox yesNoMessageDialog = new MessageBox(GDE.shell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
		yesNoMessageDialog.setText(GDE.NAME_LONG);
		yesNoMessageDialog.setMessage(message);
		return yesNoMessageDialog.open();
	}
	
	public int openYesNoMessageDialogSync(final String message) {
		this.openYesNoMessageDialogAsyncValue = -1;
		GDE.display.syncExec(new Runnable() {
			public void run() {
				MessageBox yesNoMessageDialog = new MessageBox(GDE.shell, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.ICON_QUESTION);
				yesNoMessageDialog.setText(GDE.NAME_LONG);
				yesNoMessageDialog.setMessage(message);
				DataExplorer.this.openYesNoMessageDialogAsyncValue = yesNoMessageDialog.open();
			}
		});
		int counter = 5000;
		while (this.openYesNoMessageDialogAsyncValue == -1 && counter-- > 0) {
			WaitTimer.delay(100);
		}
		return this.openYesNoMessageDialogAsyncValue;
	}

	public int openYesNoCancelMessageDialog(Shell parent, final String message) {
		final Shell useParent = (parent != null  && !parent.isDisposed()) ? parent : GDE.shell;
		MessageBox yesNoCancelMessageDialog = new MessageBox(useParent, SWT.PRIMARY_MODAL | SWT.YES | SWT.NO | SWT.CANCEL	| SWT.ICON_QUESTION);
		yesNoCancelMessageDialog.setText(GDE.NAME_LONG);
		yesNoCancelMessageDialog.setMessage(message);
		return yesNoCancelMessageDialog.open();
	}

	public void openAboutDialog() {
		new AboutDialog(GDE.shell, SWT.PRIMARY_MODAL).open();
	}

	public DeviceDialog getDeviceDialog() {
		return this.activeDevice != null ? this.activeDevice.getDialog() : null;
	}

	public MenuBar getMenuBar() {
		return this.menuBar;
	}

	public void updateSubHistoryMenuItem(String addFilePath) {
		this.menuBar.updateSubHistoryMenuItem(addFilePath);
	}
	
	public MenuToolBar getMenuToolBar() {
		return this.menuToolBar;
	}
	
	/**
	 * @return the isObjectoriented
	 */
	public boolean isObjectoriented() {
		return this.menuToolBar != null ? this.menuToolBar.isObjectoriented() : false;
	}
	
	/**
	 * @return the object key, if device oriented an empty string will be returned
	 */
	public String getObjectKey() {
		return this.menuToolBar != null ? this.menuToolBar.isObjectoriented() ? this.menuToolBar.getActiveObjectKey() : GDE.STRING_EMPTY : GDE.STRING_EMPTY;
	}
	
	/**
	 * @return the object keys, if there are no object key defined return an empty string array
	 */
	public String[] getObjectKeys() {
		return this.settings.getObjectList();
	}
	
	/**
	 * @return the object keys, if there are no object key defined return an empty string array
	 */
	public void selectObjectKey(final String newObjectKey) {
		if (!this.getObjectKey().equals(newObjectKey)) {
			String[] objectKeys = this.settings.getObjectList();
			for (int searchSelectionIndex = 0; searchSelectionIndex < objectKeys.length; ++searchSelectionIndex) {
				if (newObjectKey.equals(objectKeys[searchSelectionIndex])) {
					if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
						this.menuToolBar.selectObjectKey(searchSelectionIndex);
					}
					else {
						final int selectionIndex = searchSelectionIndex;
						GDE.display.asyncExec(new Runnable() {
							public void run() {
								DataExplorer.this.menuToolBar.selectObjectKey(selectionIndex);
							}
						});
					}
					break;
				}
			}
		}
	}
	
	/**
	 * check if some the object data needs to be saved 
	 */
	public void checkSaveObjectData() {
		if (this.objectDescriptionTabItem != null) this.objectDescriptionTabItem.checkSaveObjectData();
	}

	/**
	 * @return the full qualified object file path
	 */
	public String getObjectFilePath() {
		String objectkey = this.menuToolBar.getActiveObjectKey();
		FileUtils.checkDirectoryAndCreate(Settings.getInstance().getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + objectkey);
		return this.settings.getDataFilePath() + GDE.FILE_SEPARATOR_UNIX + objectkey + GDE.FILE_SEPARATOR_UNIX;
	}
	
	/**
	 * set a new object key list from outside (object key scanner)
	 */
	public void setObjectList(final String[] newObjectKeyList, final String newObjectKey) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuToolBar.setObjectList(newObjectKeyList, newObjectKey);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.menuToolBar.setObjectList(newObjectKeyList, newObjectKey);
				}
			});
		}
	}

	/**
	 * enable / disable the device switch buttons
	 * @param enabled
	 */
	public void enableDeviceSwitchButtons(boolean enabled) {
		this.menuToolBar.enableDeviceSwitchButtons(enabled);
		this.menuBar.enableDeviceSwitchButtons(enabled);
	}

	/**
	 * enable/disable some menu action (buttons) to avoid exceptions 
	 * sample: while loading file content, disable device switch or record set deletion
	 */
	public void enableMenuActions(final boolean enabled) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuToolBar.enableDeviceSwitchButtons(enabled);
			this.menuBar.enableDeviceSwitchButtons(enabled);
			
			this.menuToolBar.enableChannelActions(enabled);
			this.menuToolBar.enableRecordSetActions(enabled);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.menuToolBar.enableDeviceSwitchButtons(enabled);
					DataExplorer.this.menuBar.enableDeviceSwitchButtons(enabled);
					
					DataExplorer.this.menuToolBar.enableChannelActions(enabled);
					DataExplorer.this.menuToolBar.enableRecordSetActions(enabled);
				}
			});
		}
}
	
	/**
	 * method to update the channel selector of the data tool bar
	 * @param activeChannel to be set
	 */
	public void updateChannelSelector(int activeChannel) {
		String[] channelNames = new String[this.channels.size()];
		for (int i = 0; i < channelNames.length; i++) {
			channelNames[i] = this.channels.get(i + 1).getName();
		}
		CCombo channelSelect = this.menuToolBar.getChannelSelectCombo();
		channelSelect.setItems(channelNames); // "K1: Kanal 1", "K2: Kanal 2", "K3: Kanal 3", "K4: Kanal 4"
		channelSelect.select(activeChannel); // kanalCombo.setText("K1: Kanal 1");
	}

	/**
	 * method to update the channel selector of the data tool bar
	 * @param activeRecord to be set
	 */
	public void updateRecordSelector(int activeRecord) {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			String[] recordNames = activeChannel.getRecordSetNames();
			if (recordNames != null && recordNames.length > 0 && recordNames[0] != null) {
				CCombo recordSelect = this.menuToolBar.getRecordSelectCombo();
				recordSelect.setItems(recordNames); // 1) Datensatz
				recordSelect.select(activeRecord); // kanalCombo.setText("K1: Kanal 1");
			}
		}
	}

	public FileDialog openFileOpenDialog(String name, String[] extensions, String path, String fileName, int addStyle) {
		final String $METHOD_NAME = "openFileOpenDialog"; //$NON-NLS-1$
		FileDialog fileOpenDialog = new FileDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.OPEN | addStyle);
		path = path.replace(GDE.FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
		path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileOpenDialog.setText(name);
		fileOpenDialog.setFileName(fileName == null ? GDE.STRING_EMPTY : fileName);
		if (extensions != null) {
			adaptFilter(fileOpenDialog, extensions);
		}
		if (path != null) fileOpenDialog.setFilterPath(path);
		fileOpenDialog.open();
		return fileOpenDialog;
	}

	/**
	 * adapt extensions list to upper and lower case if OS distinguish
	 * @param fileOpenDialog
	 * @param extensions
	 */
	private void adaptFilter(FileDialog fileOpenDialog, String[] extensions) {
		if (GDE.IS_LINUX) { // Apples MAC OS seams to reply with case insensitive file names
			Vector<String> tmpExt = new Vector<String>();
			for (String extension : extensions) {
				if (!extension.equals(GDE.FILE_ENDING_STAR_STAR)) {
					tmpExt.add(extension); // lower case is default
					tmpExt.add(extension.toUpperCase());
				}
				else tmpExt.add(extension);
			}
			extensions = tmpExt.toArray(new String[1]);
		}
		fileOpenDialog.setFilterExtensions(extensions);
		fileOpenDialog.setFilterNames(getExtensionDescription(extensions));
	}

	public FileDialog openFileOpenDialog(Shell parent, String name, String[] extensions, String path, String fileName, int addStyle) {
		final String $METHOD_NAME = "openFileOpenDialog"; //$NON-NLS-1$
		FileDialog fileOpenDialog = new FileDialog(parent, SWT.PRIMARY_MODAL | SWT.OPEN | addStyle);
		path = path.replace(GDE.FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
		path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileOpenDialog.setText(name);
		fileOpenDialog.setFileName(fileName == null ? GDE.STRING_EMPTY : fileName);
		if (extensions != null) {
			adaptFilter(fileOpenDialog, extensions);
		}
		if (path != null) fileOpenDialog.setFilterPath(path);
		fileOpenDialog.open();
		return fileOpenDialog;
	}

	public FileDialog prepareFileSaveDialog(String name, String[] extensions, String path, String fileName) {
		final String $METHOD_NAME = "openFileSaveDialog"; //$NON-NLS-1$
		FileDialog fileSaveDialog = new FileDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.SAVE);
		path = path.replace(GDE.FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
		path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileSaveDialog.setText(name);
		if (extensions != null) {
			adaptFilter(fileSaveDialog, extensions);
		}
		if (path != null) fileSaveDialog.setFilterPath(path);
		fileSaveDialog.setFileName(fileName != null && fileName.length() > 5 ? fileName : GDE.STRING_EMPTY);
		return fileSaveDialog;
	}

	public FileDialog prepareFileSaveDialog(Shell parent, String name, String[] extensions, String path, String fileName) {
		final String $METHOD_NAME = "openFileSaveDialog"; //$NON-NLS-1$
		FileDialog fileSaveDialog = new FileDialog(parent, SWT.PRIMARY_MODAL | SWT.SAVE | SWT.ON_TOP);
		path = path.replace(GDE.FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
		path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileSaveDialog.setText(name);
		if (extensions != null) {
			adaptFilter(fileSaveDialog, extensions);
		}
		if (path != null) fileSaveDialog.setFilterPath(path);
		fileSaveDialog.setFileName(fileName != null && fileName.length() > 5 ? fileName : GDE.STRING_EMPTY);
		return fileSaveDialog;
	}

	/**
	 * @param extensions
	 * @return the mapped extension description *.osd -> DataExplorer files
	 */
	public String[] getExtensionDescription(String[] extensions) {
		String[] filterNames = new String[extensions.length];
		for (int i = 0; i < filterNames.length; i++) {
			int beginIndex = extensions[i].indexOf(GDE.STRING_DOT);
			String tmpExt = (beginIndex != -1 ? extensions[i].substring(beginIndex+1) : extensions[i]);			
			filterNames[i] = this.extensionFilterMap.get(tmpExt.toLowerCase());
			
			if (filterNames[i] == null)
				filterNames[i] = extensions[i];
			else {
				String tmpFilterExt = filterNames[i].substring(filterNames[i].indexOf(GDE.STRING_DOT)+1, filterNames[i].length()-1);
				filterNames[i] = tmpExt.equals(tmpFilterExt) ? filterNames[i] : filterNames[i].replace(tmpFilterExt, tmpExt);
			}
		}
		return filterNames;
	}

	public String openDirFileDialog(String name, String path) {
		final String $METHOD_NAME = "openDirFileDialog"; //$NON-NLS-1$
		DirectoryDialog fileDirDialog = new DirectoryDialog(GDE.shell, SWT.PRIMARY_MODAL | SWT.NONE);
		path = path.replace(GDE.FILE_SEPARATOR_UNIX, GDE.FILE_SEPARATOR);
		path = !path.endsWith(GDE.FILE_SEPARATOR) ? path + GDE.FILE_SEPARATOR : path;
		if (log.isLoggable(Level.FINER)) log.logp(Level.FINER, $CLASS_NAME, $METHOD_NAME, "dialogName = " + name + " path = " + path); //$NON-NLS-1$ //$NON-NLS-2$
		fileDirDialog.setText(name);
		if (path != null) fileDirDialog.setFilterPath(path);
		return fileDirDialog.open();
	}

	public RGB openColorDialog() {
		ColorDialog colorDialog = new ColorDialog(GDE.shell);
		colorDialog.setText(this.getClass().getSimpleName() + Messages.getString(MessageIds.GDE_MSGT0145));
		return colorDialog.open();
	}
	
	/**
	 * update all visualization windows
	 */
	public void updateAllTabs(final boolean force) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.updateGraphicsWindow();
			this.updateStatisticsData(true);
			if (force) {
				this.updateDigitalWindow();
				this.updateAnalogWindow();
			}
			else {
				this.updateDigitalWindowChilds();
				this.updateAnalogWindowChilds();
			}
			this.updateCellVoltageWindow();
			this.updateFileCommentWindow();
			if (this.getActiveRecordSet() != null) {
				this.updateDataTable(this.getActiveRecordSet().getName(), force);
			}
			else {
				DataExplorer.this.updateDataTable(GDE.STRING_EMPTY, force);
			}
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.updateGraphicsWindow();
					DataExplorer.this.updateStatisticsData(true);
					if (force) {
						DataExplorer.this.updateDigitalWindow();
						DataExplorer.this.updateAnalogWindow();
					}
					else {
						DataExplorer.this.updateDigitalWindowChilds();
						DataExplorer.this.updateAnalogWindowChilds();
					}
					DataExplorer.this.updateCellVoltageWindow();
					DataExplorer.this.updateFileCommentWindow();
					if (DataExplorer.this.getActiveRecordSet() != null) {
						DataExplorer.this.updateDataTable(DataExplorer.this.getActiveRecordSet().getName(), force);
					}
					else {
						DataExplorer.this.updateDataTable(GDE.STRING_EMPTY, force);
					}
				}
			});
		}
	}
	
	/**
	 * update the graphicsWindow
	 */
	public void updateGraphicsWindow() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (!this.graphicsTabItem.isActiveCurveSelectorContextMenu()) {
				int tabSelectionIndex = this.displayTab.getSelectionIndex();
				if (tabSelectionIndex == 0) {
					this.graphicsTabItem.redrawGraphics();
				}
				else if (this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) {
					this.compareTabItem.redrawGraphics();
				}
			}
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					if (!DataExplorer.this.graphicsTabItem.isActiveCurveSelectorContextMenu()) {
						int tabSelectionIndex = DataExplorer.this.displayTab.getSelectionIndex();
						if (tabSelectionIndex == 0) {
							DataExplorer.this.graphicsTabItem.redrawGraphics();
						}
						else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) {
							DataExplorer.this.compareTabItem.redrawGraphics();
						}
					}
				}
			});
		}
	}
	
	/**
	 * update graphics captions (header and description)
	 */
	public void updateGraphicsCaptions() {
		if (this.graphicsTabItem != null) this.graphicsTabItem.updateCaptions();
	}

	/**
	 * update the graphics window curve selector table only (after calculating values to make records displayable)
	 */
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.graphicsTabItem.updateCurveSelectorTable();
			if (this.compareTabItem != null && !this.compareTabItem.isDisposed())
				this.compareTabItem.updateCurveSelectorTable();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.graphicsTabItem.updateCurveSelectorTable();
					if (DataExplorer.this.compareTabItem != null && !DataExplorer.this.compareTabItem.isDisposed())
						DataExplorer.this.compareTabItem.updateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * method to switch to another display tab
	 */
	public void switchDisplayTab(int tabIndex) {
		this.displayTab.setSelection(tabIndex);
	}

	/**
	 * method to make record comment window visible
	 */
	public void setRecordCommentEnabled(boolean enabled) {
		this.settings.setRecordCommentVisible(enabled);
		this.isRecordCommentVisible = enabled;
	}

	/**
	 * set the curve selector visible
	 */
	public void setCurveSelectorEnabled(boolean value) {
		this.graphicsTabItem.setCurveSelectorEnabled(value);
	}

	/**
	 * set the graphics window sashForm weights
	 */
	public void setGraphicsSashFormWeights(int newSelectorCopositeWidth, int windowType) {
		switch (windowType) {
		case GraphicsWindow.TYPE_COMPARE:
			this.compareTabItem.setSashFormWeights(newSelectorCopositeWidth);
			break;

		default:
			this.graphicsTabItem.setSashFormWeights(newSelectorCopositeWidth);
			break;
		}
	}

	/**
	 * opens the settingsDialog
	 */
	public void openSettingsDialog() {
		this.settingsDialog = new SettingsDialog(GDE.shell, SWT.PRIMARY_MODAL);
		this.settingsDialog.open();
	}

	public void updateDisplayTab() {
		this.displayTab.redraw();
	}

	public void updateCompareWindow() {
		this.compareTabItem.redrawGraphics();
	}

	public RecordSet getCompareSet() {
		return this.compareSet == null ? this.compareSet = new RecordSet(null, GDE.STRING_EMPTY, DataExplorer.COMPARE_RECORD_SET, 1) : this.compareSet;
	}
	
	public RecordSet getUtilitySet() {
		return this.utilitySet == null ? this.utilitySet = new RecordSet(null, GDE.STRING_EMPTY, DataExplorer.UTILITY_RECORD_SET, 1) : this.utilitySet;
	}

	/**
	 * check the given graphics window type is visible to decide which record set to be used
	 * @param type (GraphicsWindow.TYPE_NORMAL/GraphicsWindow.TYPE_COMPARE)
	 * @return true if the the record set in it window is visible
	 */
	public boolean isRecordSetVisible(int type) {
		boolean result = false;
		switch (type) {
		case GraphicsWindow.TYPE_COMPARE:
			result = this.compareTabItem!= null && !this.compareTabItem.isDisposed() && this.compareTabItem.isVisible();
			break;

		case GraphicsWindow.TYPE_NORMAL:
		default:
			result = this.graphicsTabItem.isVisible();
			break;
		}
		return result;
	}

	/**
	 * reset the graphicsWindow zoom mode and measurement pointer
	 */
	public void resetGraphicsWindowZoomAndMeasurement() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.graphicsTabItem.setModeState(GraphicsComposite.MODE_RESET);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.graphicsTabItem.setModeState(GraphicsComposite.MODE_RESET);
				}
			});
		}
	}

	/**
	 * set the graphics window mode to the just visible window
	 * @param graphicsMode (GraphicsWindow.MODE_ZOOM, GraphicsWindow.MODE_PAN)
	 * @param enabled
	 */
	public void setGraphicsMode(int graphicsMode, boolean enabled) {
		final String $METHOD_NAME = "setGraphicsMode"; //$NON-NLS-1$
		if (isRecordSetVisible(GraphicsWindow.TYPE_NORMAL)) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "graphicsWindow.getGraphicCanvas().isVisible() == true"); //$NON-NLS-1$
			setGraphicsWindowGraphicsMode(graphicsMode, enabled);
		}
		else if (isRecordSetVisible(GraphicsWindow.TYPE_COMPARE) && graphicsMode != GraphicsComposite.MODE_SCOPE) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "compareWindow.getGraphicCanvas().isVisible() == true"); //$NON-NLS-1$
			setCompareWindowGraphicsMode(graphicsMode, enabled);
		}
	}

	/**
	 * set the graphics window mode to the graphics window
	 * @param graphicsMode
	 * @param enabled
	 */
	public void setGraphicsWindowGraphicsMode(int graphicsMode, boolean enabled) {
		RecordSet recordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
		if (recordSet != null) {
			switch (graphicsMode) {
			case GraphicsComposite.MODE_ZOOM:
				recordSet.resetMeasurement();
				recordSet.setZoomMode(enabled);
				this.graphicsTabItem.setModeState(graphicsMode);
				break;
			case GraphicsComposite.MODE_SCOPE:
				recordSet.resetZoomAndMeasurement();
				recordSet.setScopeSizeRecordPoints(this.getMenuToolBar().getScopeModeLevelValue());
				this.graphicsTabItem.setModeState(graphicsMode);
				this.updateGraphicsWindow();
				break;
			case GraphicsComposite.MODE_PAN:
				if (!recordSet.isPanMode())
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007));
				else {
					recordSet.resetMeasurement();
					this.graphicsTabItem.setModeState(graphicsMode);
				}
				break;
			default:
			case GraphicsComposite.MODE_RESET:
				recordSet.resetZoomAndMeasurement();
				this.graphicsTabItem.setModeState(graphicsMode);
				this.updateGraphicsWindow();
			}
		}
	}

	/**
	 * set the graphics window mode to the compare window
	 * @param graphicsMode
	 * @param enabled
	 */
	public void setCompareWindowGraphicsMode(int graphicsMode, boolean enabled) {
		RecordSet recordSet = DataExplorer.application.getCompareSet();
		if (recordSet != null) {
			switch (graphicsMode) {
			case GraphicsComposite.MODE_ZOOM:
				recordSet.resetMeasurement();
				recordSet.setZoomMode(enabled);
				if (this.compareTabItem!= null && !this.compareTabItem.isDisposed())
					this.compareTabItem.setModeState(graphicsMode);
				break;
			case GraphicsComposite.MODE_PAN:
				if (!recordSet.isPanMode())
					this.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007));
				else {
					recordSet.resetMeasurement();
					if (this.compareTabItem!= null && !this.compareTabItem.isDisposed())
						this.compareTabItem.setModeState(graphicsMode);
				}
				break;
			default:
			case GraphicsComposite.MODE_RESET:
				recordSet.resetZoomAndMeasurement();
				if (this.compareTabItem!= null && !this.compareTabItem.isDisposed())
					this.compareTabItem.setModeState(graphicsMode);
				this.updateGraphicsWindow();
			}
		}
	}

	/**
	 * switch application into measurement mode for the visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setMeasurementActive(String recordKey, boolean enabled) {
		boolean isWindowTypeNormal = isRecordSetVisible(GraphicsWindow.TYPE_NORMAL);
		RecordSet recordSet = isWindowTypeNormal ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.compareSet;
		if (recordSet != null && recordSet.containsKey(recordKey)) {
			if (isWindowTypeNormal) {
				recordSet.setMeasurementMode(recordKey, enabled);
				if (enabled)
					this.graphicsTabItem.getGraphicsComposite().drawMeasurePointer(recordSet, GraphicsComposite.MODE_MEASURE, false);
				else
					this.graphicsTabItem.getGraphicsComposite().cleanMeasurementPointer();
			}
			else if (this.compareTabItem!= null && !this.compareTabItem.isDisposed()) {
				recordSet = DataExplorer.application.getCompareSet();
				if (recordSet != null && recordSet.containsKey(recordKey)) {
					recordSet.setMeasurementMode(recordKey, enabled);
					if (enabled)
						this.compareTabItem.getGraphicsComposite().drawMeasurePointer(recordSet, GraphicsComposite.MODE_MEASURE, false);
					else
						this.compareTabItem.getGraphicsComposite().cleanMeasurementPointer();
				}
			}
		}
	}

	/**
	 * switch application into delta measurement mode for visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setDeltaMeasurementActive(String recordKey, boolean enabled) {
		boolean isWindowTypeNormal = isRecordSetVisible(GraphicsWindow.TYPE_NORMAL);
		RecordSet recordSet = isWindowTypeNormal ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.compareSet;
		if (recordSet != null && recordSet.containsKey(recordKey)) {
			if (isWindowTypeNormal) {
				recordSet.setDeltaMeasurementMode(recordKey, enabled);
				if (enabled)
					this.graphicsTabItem.getGraphicsComposite().drawMeasurePointer(recordSet, GraphicsComposite.MODE_MEASURE_DELTA, false);
				else
					this.graphicsTabItem.getGraphicsComposite().cleanMeasurementPointer();
			}
			else if (this.compareTabItem!= null && !this.compareTabItem.isDisposed()) {
				recordSet = DataExplorer.application.getCompareSet();
				if (recordSet != null && recordSet.containsKey(recordKey)) {
					recordSet.setDeltaMeasurementMode(recordKey, enabled);
					if (enabled)
						this.compareTabItem.getGraphicsComposite().drawMeasurePointer(recordSet, GraphicsComposite.MODE_MEASURE_DELTA, false);
					else
						this.compareTabItem.getGraphicsComposite().cleanMeasurementPointer();
				}
			}
		}
	}

	/**
	 * switch application graphics window into cut mode
	 * @param leftEnabled
	 * @param rightEnabled
	 */
	public void setCutModeActive(boolean leftEnabled, boolean rightEnabled) {
		if (leftEnabled || rightEnabled)
			if (leftEnabled)
				this.graphicsTabItem.getGraphicsComposite().drawCutPointer(GraphicsComposite.MODE_CUT_LEFT, leftEnabled, rightEnabled);
			else if (rightEnabled)
				this.graphicsTabItem.getGraphicsComposite().drawCutPointer(GraphicsComposite.MODE_CUT_RIGHT, leftEnabled, rightEnabled);
			else
				this.graphicsTabItem.getGraphicsComposite().drawCutPointer(GraphicsComposite.MODE_RESET, false, false);
		else
			this.graphicsTabItem.getGraphicsComposite().drawCutPointer(GraphicsComposite.MODE_RESET, false, false);
	}

	@Override
	public void setCursor(final Cursor newCursor) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			DataExplorer.application.getParent().setCursor(newCursor);
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.application.getParent().setCursor(newCursor);
				}
			});
		}
	}

	public long getThreadId() {
		return this.threadId;
	}

	public void setPortConnected(final boolean isOpenStatus) {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			this.menuBar.setPortConnected(isOpenStatus);
			this.menuToolBar.setPortConnected(isOpenStatus);
			if (isOpenStatus) {
				this.statusBar.setSerialPortConnected();
			}
			else {
				this.statusBar.setSerialPortDisconnected();
				this.statusBar.setSerialRxOff();
				this.statusBar.setSerialTxOff();
			}
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					DataExplorer.this.menuBar.setPortConnected(isOpenStatus);
					DataExplorer.this.menuToolBar.setPortConnected(isOpenStatus);
					if (isOpenStatus) {
						DataExplorer.this.statusBar.setSerialPortConnected();
					}
					else {
						DataExplorer.this.statusBar.setSerialPortDisconnected();
						DataExplorer.this.statusBar.setSerialRxOff();
						DataExplorer.this.statusBar.setSerialTxOff();
					}
				}
			});
		}
	}

	/**
	 * open the dialog and displays content of given HTML file 
	 * @param deviceName 
	 * @param fileName the help HTML file
	 */
	public void openHelpDialog(String deviceName, String fileName) {
		final String $METHOD_NAME = "openHelpDialog"; //$NON-NLS-1$
		try {
			if (this.helpDialog == null || this.helpDialog.isDisposed()) {
				this.helpDialog = new HelpInfoDialog(GDE.shell, SWT.NONE);
			}
			if (GDE.IS_WINDOWS || GDE.IS_MAC) { //$NON-NLS-1$
				if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "using syle SWT.NONE (windows IE)"); //$NON-NLS-1$
				//this.helpDialog.dispose();
				this.helpDialog.open(deviceName, fileName, SWT.NONE);
			}
			else {
				if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "using syle SWT.MOZILLA (xulrunner)"); //$NON-NLS-1$
				this.helpDialog.open(deviceName, fileName, SWT.MOZILLA);
			}
		}
		catch (Error e) {
			if (log.isLoggable(Level.FINE)) log.logp(Level.FINE, $CLASS_NAME, $METHOD_NAME, "using OS registered web browser"); //$NON-NLS-1$
			WebBrowser.openURL(deviceName, fileName);
			application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0025));
		}
		catch (Throwable t) {
			application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + t.getClass().getSimpleName() + GDE.STRING_MESSAGE_CONCAT + t.getMessage());
		}
	}

	/**
	 * open the dialog and displays content of given HTML file 
	 * @param stringURL of the help HTML file
	 */
	public void openWebBrowser(String stringURL) {
		WebBrowser.openBrowser(stringURL);
	}

	/**
	 * @return true for application modal device dialogs
	 */
	public boolean isDeviceDialogModal() {
		//return this.settings.isDeviceDialogsModal();
		return this.isDeviceDialogModal;
	}

	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsTabItem.enableGraphicsHeader(enabled);
		this.settings.setGraphicsHeaderVisible(enabled);
		this.isGraphicsHeaderVisible = enabled;
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		this.graphicsTabItem.enableRecordSetComment(enabled);
		this.settings.setRecordCommentVisible(enabled);
		this.isRecordCommentVisible = enabled;
	}

	/**
	 * @return the statusBar
	 */
	public StatusBar getStatusBar() {
		return this.statusBar;
	}

	/**
	 * update the object image
	 */
	public void updateObjectImage() {
		if (this.objectDescriptionTabItem != null) this.objectDescriptionTabItem.redrawImageCanvas();
	}

	/**
	 * check if file comment has pending change and update if required
	 */
	public void checkUpdateFileComment() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (this.fileCommentTabItem != null && this.fileCommentTabItem.isFileCommentChanged()) 
				this.fileCommentTabItem.setFileComment();
		}
		else { // if the percentage is not up to date it will updated later
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					if (DataExplorer.this.fileCommentTabItem != null && DataExplorer.this.fileCommentTabItem.isFileCommentChanged()) 
						DataExplorer.this.fileCommentTabItem.setFileComment();
				}
			});
		}
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	public boolean isFileCommentChanged() {
		return this.fileCommentTabItem.isFileCommentChanged();
	}

	/**
	 * check if file comment has pending change and update if required
	 */
	public void checkUpdateRecordSetComment() {
		if (Thread.currentThread().getId() == DataExplorer.application.getThreadId()) {
			if (this.graphicsTabItem != null && this.graphicsTabItem.getGraphicsComposite().isRecordCommentChanged()) 
				this.graphicsTabItem.getGraphicsComposite().updateRecordSetComment();
		}
		else { // if the percentage is not up to date it will updated later
			GDE.display.asyncExec(new Runnable() {
				public void run() {
					if (DataExplorer.this.graphicsTabItem.getGraphicsComposite().isRecordCommentChanged()) 
						DataExplorer.this.graphicsTabItem.getGraphicsComposite().updateRecordSetComment();
				}
			});
		}
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	public boolean isRecordCommentChanged() {
		return this.graphicsTabItem.getGraphicsComposite().isRecordCommentChanged();
	}

	/**
	 * @return the canvasImage alias graphics w1ndow
	 */
	public Image getGraphicsPrintImage() {
		return this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE) 
			? this.compareTabItem.getGraphicsComposite().getGraphicsPrintImage() 
			: this.graphicsTabItem.getGraphicsComposite().getGraphicsPrintImage();
	}
	
	/**
	 * return statistics window content as image
   */
	public Image getGraphicsTabContentAsImage() {
		return this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE) 
		? this.compareTabItem.getContentAsImage() 
		: this.graphicsTabItem.getContentAsImage();
	}
	
	/**
	 * return statistics window content as image
   */
	public Image getStatisticsTabContentAsImage() {
		return this.statisticsTabItem.getContentAsImage();
	}
	
	/**
	 * return statistics window content as formated string
   */
	public String getStatisticsAsText() {
		return this.statisticsTabItem.getContentAsText();
	}
	
	/**
	 * return table tab window content as image
   */
	public Image getTableTabContentAsImage() {
		return this.dataTableTabItem.getContentAsImage();
	}
	
	/**
	 * return digital tab window content as image
   */
	public Image getDigitalTabContentAsImage() {
		return this.digitalTabItem.getContentAsImage();
	}
	
	/**
	 * return analog tab window content as image
   */
	public Image getAnalogTabContentAsImage() {
		return this.analogTabItem.getContentAsImage();
	}
	
	/**
	 * return cell voltage tab window content as image
   */
	public Image getCellVoltageTabContentAsImage() {
		return this.cellVoltageTabItem.getContentAsImage();
	}
	
	/**
	 * return file description tab window content as image
   */
	public Image getFileDescriptionTabContentAsImage() {
		return this.fileCommentTabItem.getContentAsImage();
	}

	/**
	 * get object description window as image for printing purpose
	 * @return object description window as image
	 */
	public Image getObjectTabContentAsImage() {
		return this.objectDescriptionTabItem.getContentAsImage();
	}


	/**
	 * copy tabulator content as image into clipboard
	 */
	public void copyTabContentAsImage() {
		Image graphicsImage = null;
		int tabSelectionIndex = this.displayTab.getSelectionIndex();
		if (tabSelectionIndex == 0) {
			graphicsImage = this.graphicsTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof StatisticsWindow) {
			graphicsImage = this.statisticsTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof DataTableWindow) {
			graphicsImage = this.dataTableTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
			graphicsImage = this.digitalTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof AnalogWindow) {
			graphicsImage = this.analogTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof CellVoltageWindow) {
			graphicsImage = this.cellVoltageTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof FileCommentWindow) {
			graphicsImage = this.fileCommentTabItem.getContentAsImage();
		}
		else if (DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof ObjectDescriptionWindow) {
			graphicsImage = this.objectDescriptionTabItem.getContentAsImage();
		}
		else if ((DataExplorer.this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) 
				&& DataExplorer.this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE)) {
			graphicsImage = this.compareTabItem.getContentAsImage();
		}
		else {
			graphicsImage = this.graphicsTabItem.getContentAsImage();
		}
		Clipboard clipboard = new Clipboard(GDE.display);
		clipboard.setContents(new Object[]{graphicsImage.getImageData()}, new Transfer[]{ImageTransfer.getInstance()});	
		clipboard.dispose();
		graphicsImage.dispose();
	}

	/**
	 * 
	 */
	public void copyGraphicsPrintImage() {
		Image graphicsImage = this.getGraphicsPrintImage();
		Clipboard clipboard = new Clipboard(GDE.display);
		clipboard.setContents(new Object[]{graphicsImage.getImageData()}, new Transfer[]{ImageTransfer.getInstance()});	
		clipboard.dispose();
		graphicsImage.dispose();
	}
	/**
	 * switch tab by selection index
	 * @param index
	 */
	public void selectTab(int index) {
		this.displayTab.setSelection(index);
		this.displayTab.showSelection();
	}

	/**
	 * @return the tab selection index
	 */
	public int getTabSelectionIndex() {
		return this.displayTab.getSelectionIndex();
	}
	
	/**
	 * set the inner area background color (for curve graphics the curve area, for statistics ...)
	 * @param tabSelectionIndex the current tabulator item index
	 * @param innerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(int tabSelectionIndex, Color innerAreaBackground) {
		if (tabSelectionIndex == 0) {
			this.settings.setGraphicsCurveAreaBackground(innerAreaBackground);
			this.graphicsTabItem.setCurveAreaBackground(innerAreaBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof StatisticsWindow) {
			this.settings.setSatisticsInnerAreaBackground(innerAreaBackground);
			this.statisticsTabItem.setInnerAreaBackground(innerAreaBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
			this.settings.setDigitalInnerAreaBackground(innerAreaBackground);
			this.digitalTabItem.setInnerAreaBackground(innerAreaBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof AnalogWindow) {
			this.settings.setAnalogInnerAreaBackground(innerAreaBackground);
			this.analogTabItem.setInnerAreaBackground(innerAreaBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof CellVoltageWindow) {
			this.settings.setCellVoltageInnerAreaBackground(innerAreaBackground);
			this.cellVoltageTabItem.setInnerAreaBackground(innerAreaBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof FileCommentWindow) {
			this.settings.setFileCommentInnerAreaBackground(innerAreaBackground);
			this.fileCommentTabItem.setInnerAreaBackground(innerAreaBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof ObjectDescriptionWindow) {
			this.settings.setObjectDescriptionInnerAreaBackground(innerAreaBackground);
			this.objectDescriptionTabItem.setInnerAreaBackground(innerAreaBackground);
		}
		else if ((this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) 
				&& this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE)) {
			this.settings.setCompareCurveAreaBackground(innerAreaBackground);
			this.compareTabItem.setCurveAreaBackground(innerAreaBackground);
		}
	}
	
	/**
	 * set the border color (for curve graphics the curve area border color, ...)
	 * @param tabItemIndex the current tabulator item index
	 * @param borderColor the borderColor to set
	 */
	public void setBorderColor(int tabItemIndex, Color borderColor) {
		if (tabItemIndex == 0) {
			this.settings.setCurveGraphicsBorderColor(borderColor);
			this.graphicsTabItem.setCurveAreaBorderColor(borderColor);
		}
		else if ((this.displayTab.getItem(tabItemIndex) instanceof GraphicsWindow) 
				&& this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE)) {
			this.settings.setCurveCompareBorderColor(borderColor);
			this.compareTabItem.setCurveAreaBorderColor(borderColor);
		}
	}

	/**
	 * set the surrounding area background color (for curve graphics, the area surrounding the curve area, ...)
	 * @param tabSelectionIndex the current tabulator item index
	 * @param surroundingBackground the surroundingBackground to set
	 */
	public void setSurroundingBackground(int tabSelectionIndex, Color surroundingBackground) {
		if (tabSelectionIndex == 0) {
			this.settings.setGraphicsSurroundingBackground(surroundingBackground);
			this.graphicsTabItem.setSurroundingBackground(surroundingBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof StatisticsWindow) {
			this.settings.setSatisticsSurroundingAreaBackground(surroundingBackground);
			this.statisticsTabItem.setSurroundingAreaBackground(surroundingBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof DigitalWindow) {
			this.settings.setDigitalSurroundingAreaBackground(surroundingBackground);
			this.digitalTabItem.setSurroundingAreaBackground(surroundingBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof AnalogWindow) {
			this.settings.setAnalogSurroundingAreaBackground(surroundingBackground);
			this.analogTabItem.setSurroundingAreaBackground(surroundingBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof CellVoltageWindow) {
			this.settings.setCellVoltageSurroundingAreaBackground(surroundingBackground);
			this.cellVoltageTabItem.setSurroundingAreaBackground(surroundingBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof FileCommentWindow) {
			this.settings.setFileCommentSurroundingAreaBackground(surroundingBackground);
			this.fileCommentTabItem.setSurroundingAreaBackground(surroundingBackground);
		}
		else if (this.displayTab.getItem(tabSelectionIndex) instanceof ObjectDescriptionWindow) {
			this.settings.setObjectDescriptionSurroundingAreaBackground(surroundingBackground);
			this.objectDescriptionTabItem.setSurroundingAreaBackground(surroundingBackground);
		}
		else if ((this.displayTab.getItem(tabSelectionIndex) instanceof GraphicsWindow) 
				&& this.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE)) {
			this.settings.setCompareSurroundingBackground(surroundingBackground);
			this.compareTabItem.setSurroundingBackground(surroundingBackground);
		}
	}
	
	/**
	 * set data table tab item visible or invisible
	 * @param visible
	 */
	public void setDataTableTabItemVisible(boolean visible) {
		if (visible) {
			if (this.dataTableTabItem == null || this.dataTableTabItem.isDisposed()) {
				this.dataTableTabItem = new DataTableWindow(this.displayTab, SWT.NONE, 2);
				this.dataTableTabItem.create();
			}
		}
		else {
			if (this.dataTableTabItem != null && !this.dataTableTabItem.isDisposed()) {
				this.dataTableTabItem.dispose();
				this.dataTableTabItem = null;
			}
		}
	}
	
	/**
	 * set digital tab item visible or invisible
	 * @param visible
	 */
	public void setDigitalTabItemVisible(boolean visible) {
		if (visible) {
			if (this.digitalTabItem == null || this.digitalTabItem.isDisposed()) {
				int position = (this.displayTab.getItem(2) instanceof DataTableWindow) ? 3 : 2;
				this.digitalTabItem = new DigitalWindow(this.displayTab, SWT.NONE, position);
				this.digitalTabItem.create();
			}
		}
		else {
			if (this.digitalTabItem != null && !this.digitalTabItem.isDisposed()) {
				this.digitalTabItem.dispose();
				this.digitalTabItem = null;
			}
		}
	}
	
	/**
	 * set analog tab item visible or invisible
	 * @param visible
	 */
	public void setAnalogTabItemVisible(boolean visible) {
		if (visible) {
			if (this.analogTabItem == null || this.analogTabItem.isDisposed()) {
				int position = (this.displayTab.getItem(2) instanceof DataTableWindow) && (this.displayTab.getItem(3) instanceof DigitalWindow) ? 4 
						: (this.displayTab.getItem(2) instanceof DataTableWindow) || (this.displayTab.getItem(2) instanceof DigitalWindow) ? 3 : 2;
				this.analogTabItem = new AnalogWindow(this.displayTab, SWT.NONE, position);
				this.analogTabItem.create();
			}
		}
		else {
			if (this.analogTabItem != null && !this.analogTabItem.isDisposed()) {
				this.analogTabItem.dispose();
				this.analogTabItem = null;
			}
		}
	}
	
	/**
	 * set data table tab item visible or invisible
	 * @param visible
	 */
	public void setCellVoltageTabItemVisible(boolean visible) {
		if (visible) {
			if (this.cellVoltageTabItem == null || this.cellVoltageTabItem.isDisposed()) {
				int position = 2;
				CTabItem[] tabItems = this.displayTab.getItems();
				for (int i = 1; i < tabItems.length; i++) {
					if (tabItems[i] instanceof GraphicsWindow || tabItems[i] instanceof FileCommentWindow) position = i;
				}
				this.cellVoltageTabItem = new CellVoltageWindow(this.displayTab, SWT.NONE, position);
				this.cellVoltageTabItem.create();
			}
		}
		else {
			if (this.cellVoltageTabItem != null && !this.cellVoltageTabItem.isDisposed()) {
				this.cellVoltageTabItem.dispose();
				this.cellVoltageTabItem = null;
			}
		}
	}

	/**
	 * create a compare window tab item
	 */
	public void createCompareWindowTabItem() {
		if (this.compareTabItem == null || this.compareTabItem.isDisposed()) {
			for (int i = 0; i < this.displayTab.getItemCount(); ++i) {
				CTabItem tabItem = this.displayTab.getItems()[i];
				if (tabItem instanceof FileCommentWindow) {
					this.compareTabItem = new GraphicsWindow(this.displayTab, SWT.NONE, GraphicsWindow.TYPE_COMPARE, Messages.getString(MessageIds.GDE_MSGT0144), i);
					this.compareTabItem.create();
					break;
				}
			}
		}
	}
	/**
	 * @param visible boolean value to set the object description tabulator visible
	 */
	public void setObjectDescriptionTabVisible(boolean visible) {
		if (visible) {
			if (this.objectDescriptionTabItem == null || this.objectDescriptionTabItem.isDisposed()) {
				for (int i = 0; i < this.displayTab.getItemCount(); ++i) {
					CTabItem tabItem = this.displayTab.getItems()[i];
					if (tabItem instanceof FileCommentWindow) {
						this.objectDescriptionTabItem = new ObjectDescriptionWindow(this.displayTab, SWT.NONE, i+1);
						this.objectDescriptionTabItem.create();
						break;
					}
				}
			}
		}
		else {
			if (this.objectDescriptionTabItem != null) {
				this.objectDescriptionTabItem.dispose();
				this.objectDescriptionTabItem = null;
			}
		}
	}
	
	/**
	 * @return the current object data object if object oriented, else null wil be returned
	 */
	public void updateCurrentObjectData(String newObjectKey) {
		if (this.objectDescriptionTabItem != null) {
			this.objectDescriptionTabItem.setObject(this.objectDescriptionTabItem.getObject(), newObjectKey);
			this.updateObjectDescriptionWindow();
		}
	}
	
	/**
	 * register a utility graphics tab item for device specific purpose
	 */
	public UtilGraphicsWindow setUtilGraphicsWindowVisible(boolean visible) {
		if (visible) {
			if (this.utilGraphicsTabItem == null || this.utilGraphicsTabItem.isDisposed()) {
				this.utilGraphicsTabItem = new UtilGraphicsWindow(this.displayTab, SWT.NONE, GraphicsWindow.TYPE_UTIL, Messages.getString(MessageIds.GDE_MSGT0282), this.displayTab.getItemCount()); //$NON-NLS-1$
				this.utilGraphicsTabItem.create();
			}
		}
		else {
			if (this.utilGraphicsTabItem != null) {
				this.utilGraphicsTabItem.dispose();
				this.utilGraphicsTabItem = null;
			}
		}
		return utilGraphicsTabItem;
	}
	
	/**
	 * register a custom tab item for device specific purpose
	 */
	public CTabItem registerCustomTabItem(CTabItem customDeviceTabItem) {
		if (customDeviceTabItem == null) {
			if (this.customTabItem != null && !this.customTabItem.isDisposed()) {
				this.customTabItem.dispose();
				this.customTabItem = null;		
			}
		}
		else {
			this.customTabItem = customDeviceTabItem;
		}
		return customTabItem;
	}
	
	/**
	 * @return the display tab folder which is required to create and register a new custom tabItem	 * @return
	 */
	public CTabFolder getTabFolder() {
		return this.displayTab;
	}
	
	/**
	 * set the main shell icon to its default
	 */
	public void resetShellIcon() {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				GDE.shell.setImage(SWTResourceManager.getImage(GDE.IS_MAC ? "gde/resource/DataExplorer_MAC.png" : "gde/resource/DataExplorer.png")); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
	}

	/**
	 * @return the object data from the objectDescriptionTabItem
	 */
	public ObjectData getObject() {
		return this.objectDescriptionTabItem.getObject();
	}
	
	/**
	 * @return the active record set or null
	 */
	public RecordSet getActiveRecordSet() {
		RecordSet activeRecordSet = null;
		Channel activeChannnel = this.channels.getActiveChannel();
		if(activeChannnel != null)
			activeRecordSet = activeChannnel.getActiveRecordSet();
		
		return activeRecordSet;
	}
	
	/**
	 * @return the number of the active channel or null
	 */
	public Integer getActiveChannelNumber() {
		Integer activeChannelNumber = null;
		Channel activeChannnel = this.channels.getActiveChannel();
		if(activeChannnel != null)
			activeChannelNumber = activeChannnel.getNumber();
		
		return activeChannelNumber;
	}
	
	/**
	 * @return the active channel or null
	 */
	public Channel getActiveChannel() {
		return this.channels.getActiveChannel();
	}
}
