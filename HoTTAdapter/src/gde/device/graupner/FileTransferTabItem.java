package gde.device.graupner;

import gde.GDE;
import gde.comm.DeviceCommPort;
import gde.config.Settings;
import gde.device.graupner.hott.MessageIds;
import gde.exception.ApplicationConfigurationException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;
import gde.utils.StringHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class FileTransferTabItem extends CTabItem {
	private static final int		INNER_COMPOSITE_HEIGHT	= 580;
	final static String					$CLASS_NAME							= FileTransferTabItem.class.getName();
	final static Logger					log											= Logger.getLogger(FileTransferTabItem.$CLASS_NAME);

	private ScrolledComposite		scrolledComposite;
	private Composite						innerComposite, outherComposite, filler;
	private Group								pcFolderGroup;
	private Button							pcBaseFolderButton;
	private CLabel							pcBaseFolderSelectionLabel;
	private Tree								pcFolderTree;
	private TreeItem						pcRootTreeItem;
	private Table								pcFoldersTable;

	private Group								sdCardActionGroup;
	private Composite						sdCardSizeComposite;
	private CLabel							sdCardSpaceinfoLabel;
	private ProgressBar					sdCardSpaceProgressBar;
	private Composite						sdCardActionComposite;
	private Button							connectButton, upLoadButton, stopButton, downLoadButton, disconnectButton, deleteFileButton;
	private Tree								sdFolderTree;
	private TreeItem						sdRootDirectoryTreeItem;
	private Table								sdCardFoldersTable;
	private Composite						tranferProgressComposite;
	private CLabel							transferProgressLabel;
	private ProgressBar					transferProgressBar;
	private TableColumn					indexColumn, fileNameColum, fileDateColum, fileTimeColum, fileSizeColum;

	final CTabFolder						tabFolder;
	final HoTTAdapter						device;
	final HoTTAdapterSerialPort	serialPort;
	final DataExplorer					application							= DataExplorer.getInstance();
	final Settings							settings								= Settings.getInstance();

	HashMap<String, String[]>		sdFoldersAndFiles				= new HashMap<String, String[]>();
	StringBuilder								selectedSdFolder;
	StringBuilder								selectedPcBaseFolder		= new StringBuilder().append(this.settings.getDataFilePath());
	StringBuilder								selectedPcFolder				= new StringBuilder();
	TreeItem										lastSelectedSdTreeItem, lastSelectedPcTreeItem;

	public FileTransferTabItem(CTabFolder parent, int style, int position, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort) {
		super(parent, style, position);
		this.tabFolder = parent;
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.setText(Messages.getString(MessageIds.GDE_MSGT2426));
		SWTResourceManager.registerResourceUser(this);
		create();
	}

	private void create() {
		try {
			this.scrolledComposite = new ScrolledComposite(this.tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
			this.setControl(this.scrolledComposite);
			{
				this.outherComposite = new Composite(this.scrolledComposite, SWT.NONE);
				this.outherComposite.setBounds(0, 0, 1100, FileTransferTabItem.INNER_COMPOSITE_HEIGHT);
				this.outherComposite.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));

				this.innerComposite = new Composite(this.outherComposite, SWT.NONE);
				this.innerComposite.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
				this.innerComposite.setSize(1100, FileTransferTabItem.INNER_COMPOSITE_HEIGHT);
				this.innerComposite.setLocation(0, 0);
				RowLayout innerCompositeLayout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
				this.innerComposite.setLayout(innerCompositeLayout);
				{
					this.pcFolderGroup = new Group(this.innerComposite, SWT.NONE);
					this.pcFolderGroup.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
					RowLayout pcGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.pcFolderGroup.setLayout(pcGroupLayout);
					RowData pcGroupLData = new RowData();
					pcGroupLData.width = 1090;
					pcGroupLData.height = 250;
					this.pcFolderGroup.setLayoutData(pcGroupLData);
					this.pcFolderGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.NORMAL));
					this.pcFolderGroup.setText(Messages.getString(MessageIds.GDE_MSGT2427));
					{
						this.filler = new Composite(this.pcFolderGroup, SWT.NONE);
						this.filler.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						GridLayout composite1Layout = new GridLayout();
						composite1Layout.makeColumnsEqualWidth = true;
						RowData composite1LData = new RowData();
						composite1LData.width = 5;
						composite1LData.height = 20;
						this.filler.setLayoutData(composite1LData);
						this.filler.setLayout(composite1Layout);
					}
					{
						this.pcBaseFolderButton = new Button(this.pcFolderGroup, SWT.PUSH | SWT.CENTER);
						RowData pcBaseFolderButtonLData = new RowData();
						pcBaseFolderButtonLData.width = 155;
						pcBaseFolderButtonLData.height = 30;
						this.pcBaseFolderButton.setLayoutData(pcBaseFolderButtonLData);
						this.pcBaseFolderButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.pcBaseFolderButton.setText(Messages.getString(MessageIds.GDE_MSGT2432));
						this.pcBaseFolderButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								FileTransferTabItem.log.log(Level.FINEST, "pcBaseFolderButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								String baseFolderName = FileTransferTabItem.this.application.openDirFileDialog(Messages.getString(MessageIds.GDE_MSGT2432), FileTransferTabItem.this.selectedPcBaseFolder.toString());
								if (baseFolderName != null && baseFolderName.length() > 0) {
									FileTransferTabItem.this.pcBaseFolderSelectionLabel.setText(baseFolderName);
									updatePcBaseFolder();
								}
							}
						});
					}
					{
						this.pcBaseFolderSelectionLabel = new CLabel(this.pcFolderGroup, SWT.NONE);
						this.pcBaseFolderSelectionLabel.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						this.pcBaseFolderSelectionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.pcBaseFolderSelectionLabel.setText(this.selectedPcBaseFolder.toString());
						RowData pcBaseFolderSelectionLabelLData = new RowData();
						pcBaseFolderSelectionLabelLData.width = 580 + 337;
						pcBaseFolderSelectionLabelLData.height = 26;
						this.pcBaseFolderSelectionLabel.setLayoutData(pcBaseFolderSelectionLabelLData);
					}
					{
						this.filler = new Composite(this.pcFolderGroup, SWT.NONE);
						this.filler.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						GridLayout compositeLayout = new GridLayout();
						compositeLayout.makeColumnsEqualWidth = true;
						RowData compositeLData = new RowData();
						compositeLData.width = 5;
						compositeLData.height = 180;
						this.filler.setLayoutData(compositeLData);
						this.filler.setLayout(compositeLayout);
					}
					{
						RowData pcFolderTreeLData = new RowData();
						pcFolderTreeLData.width = 425;
						pcFolderTreeLData.height = 175;
						this.pcFolderTree = new Tree(this.pcFolderGroup, SWT.BORDER);
						this.pcFolderTree.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.pcFolderTree.setLayoutData(pcFolderTreeLData);
						this.pcFolderTree.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								FileTransferTabItem.log.log(Level.FINEST, "pcFolderTree.widgetSelected, event=" + evt); //$NON-NLS-1$
								TreeItem evtItem = (TreeItem) evt.item;
								FileTransferTabItem.log.log(Level.FINEST, "pcFolderTree.widgetSelected, tree item = " + evtItem.getText()); //$NON-NLS-1$
								updateSelectedPcFolder(evtItem);
							}
						});
						{
							this.pcRootTreeItem = new TreeItem(this.pcFolderTree, SWT.NONE);
							this.pcRootTreeItem.setText(this.selectedPcBaseFolder.substring(this.selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1));
							updatePcBaseFolder();
						}
					}
					{
						this.filler = new Composite(this.pcFolderGroup, SWT.NONE);
						this.filler.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						GridLayout composite1Layout = new GridLayout();
						composite1Layout.makeColumnsEqualWidth = true;
						RowData composite1LData = new RowData();
						composite1LData.width = 15;
						composite1LData.height = 180;
						this.filler.setLayoutData(composite1LData);
						this.filler.setLayout(composite1Layout);
					}
					{
						this.pcFoldersTable = new Table(this.pcFolderGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
						RowData targetDirectoryTableLData = new RowData();
						targetDirectoryTableLData.width = 580;
						targetDirectoryTableLData.height = 175;
						this.pcFoldersTable.setLayoutData(targetDirectoryTableLData);
						this.pcFoldersTable.setLinesVisible(true);
						this.pcFoldersTable.setHeaderVisible(true);
						this.pcFoldersTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						setTableHeader(this.pcFoldersTable);
						this.pcFoldersTable.addListener(SWT.Selection, new Listener() {
							@Override
							public void handleEvent(Event event) {
								TableItem item = (TableItem) event.item;
								FileTransferTabItem.log.log(Level.FINE, "Selection={" + item.getText(1) + "}"); //$NON-NLS-1$ //$NON-NLS-2$
								TableItem[] selection = FileTransferTabItem.this.pcFoldersTable.getSelection();
								StringBuilder sb = new StringBuilder();
								for (TableItem element : selection)
									sb.append(element).append(GDE.STRING_BLANK);
								FileTransferTabItem.log.log(Level.FINE, "Selection={" + sb.toString() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
							}
						});
					}
				}
				{
					this.sdCardActionGroup = new Group(this.innerComposite, SWT.NONE);
					this.sdCardActionGroup.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
					RowLayout TransmitterSourceGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.sdCardActionGroup.setLayout(TransmitterSourceGroupLayout);
					RowData transmitterSourceGroupLData = new RowData();
					transmitterSourceGroupLData.width = 1090;
					transmitterSourceGroupLData.height = 280;
					this.sdCardActionGroup.setLayoutData(transmitterSourceGroupLData);
					this.sdCardActionGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 2, SWT.NORMAL));
					this.sdCardActionGroup.setText(Messages.getString(MessageIds.GDE_MSGT2428));
					{
						this.sdCardSizeComposite = new Composite(this.sdCardActionGroup, SWT.NONE);
						this.sdCardSizeComposite.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						RowLayout sdCardSizeCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData sdCardSizeCompositeLData = new RowData();
						sdCardSizeCompositeLData.width = 923;
						sdCardSizeCompositeLData.height = 36;
						this.sdCardSizeComposite.setLayoutData(sdCardSizeCompositeLData);
						this.sdCardSizeComposite.setLayout(sdCardSizeCompositeLayout);
						{
							this.sdCardSpaceinfoLabel = new CLabel(this.sdCardSizeComposite, SWT.NONE);
							this.sdCardSpaceinfoLabel.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
							RowData sdCardSpaceinfoLabelLData = new RowData();
							sdCardSpaceinfoLabelLData.width = 869;
							sdCardSpaceinfoLabelLData.height = 18;
							this.sdCardSpaceinfoLabel.setLayoutData(sdCardSpaceinfoLabelLData);
							this.sdCardSpaceinfoLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.sdCardSpaceinfoLabel.setText(Messages.getString(MessageIds.GDE_MSGT2450, new Object[] { 0, 0 }));
						}
						{
							this.sdCardSpaceProgressBar = new ProgressBar(this.sdCardSizeComposite, SWT.NONE);
							RowData sdCardSpaceProgressBarLData = new RowData();
							sdCardSpaceProgressBarLData.width = 869;
							sdCardSpaceProgressBarLData.height = 10;
							this.sdCardSpaceProgressBar.setLayoutData(sdCardSpaceProgressBarLData);
							this.sdCardSpaceProgressBar.setMinimum(0);
							this.sdCardSpaceProgressBar.setMaximum(100);
						}
					}
					{
						this.sdCardActionComposite = new Composite(this.sdCardActionGroup, SWT.NONE);
						this.sdCardActionComposite.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						GridLayout composite2Layout = new GridLayout();
						composite2Layout.makeColumnsEqualWidth = true;
						this.sdCardActionComposite.setLayout(composite2Layout);
						RowData composite2LData = new RowData();
						composite2LData.width = 178;
						composite2LData.height = 180;
						this.sdCardActionComposite.setLayoutData(composite2LData);
						{
							this.connectButton = new Button(this.sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData connectButtonLData = new GridData();
							connectButtonLData.widthHint = 155;
							connectButtonLData.heightHint = 30;
							this.connectButton.setLayoutData(connectButtonLData);
							this.connectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.connectButton.setText(Messages.getString(MessageIds.GDE_MSGT2429));
							this.connectButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									FileTransferTabItem.log.log(Level.FINEST, "connectButton.widgetSelected, event=" + evt); //$NON-NLS-1$
									try {
										if (!FileTransferTabItem.this.serialPort.isConnected()) {
											FileTransferTabItem.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_START_STOP, Messages.getString(MessageIds.GDE_MSGT2404),
													Messages.getString(MessageIds.GDE_MSGT2404));
											FileTransferTabItem.this.serialPort.open();
											FileTransferTabItem.this.disconnectButton.setEnabled(true);
										}
										FileTransferTabItem.this.serialPort.prepareSdCard();

										updateSdCardSizes(FileTransferTabItem.this.serialPort.querySdCardSizes());
										listSdCardBaseDirs();
									}
									catch (Exception e) {
										FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
										enableStopButton(false);
										FileTransferTabItem.this.application.openMessageDialog(e.getMessage());
									}
								}
							});
						}
						{
							this.upLoadButton = new Button(this.sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData upLoadButtonLData = new GridData();
							upLoadButtonLData.widthHint = 155;
							upLoadButtonLData.heightHint = 30;
							this.upLoadButton.setLayoutData(upLoadButtonLData);
							this.upLoadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.upLoadButton.setText(Messages.getString(MessageIds.GDE_MSGT2430));
							this.upLoadButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									FileTransferTabItem.log.log(Level.FINEST, "upLoadButton.widgetSelected, event=" + evt); //$NON-NLS-1$
									TableItem[] selection = FileTransferTabItem.this.sdCardFoldersTable.getSelection();

									if (selection == null || selection.length < 1 || FileTransferTabItem.this.selectedSdFolder == null || FileTransferTabItem.this.selectedSdFolder.length() < 5
											|| FileTransferTabItem.this.selectedSdFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) < 0) {
										FileTransferTabItem.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT2431));
										return;
									}
									else if (FileTransferTabItem.this.selectedPcFolder == null || FileTransferTabItem.this.selectedPcFolder.length() == 0) {
										FileTransferTabItem.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT2433));
										return;
									}

									final StringBuilder filesInfo = new StringBuilder();
									long tmpTotalSize = 0;
									for (TableItem element : selection) {
										String[] date = element.getText(2).split(GDE.STRING_DASH);
										String[] time = element.getText(3).split(GDE.STRING_COLON);
										GregorianCalendar calendar = new GregorianCalendar(Integer.parseInt(date[0]), Integer.parseInt(date[1]) - 1, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer
												.parseInt(time[1]));
										long timeStamp = calendar.getTimeInMillis();
										filesInfo.append(element.getText(0)).append(GDE.STRING_COMMA).append(element.getText(1)).append(GDE.STRING_COMMA).append(timeStamp).append(GDE.STRING_SEMICOLON);
										tmpTotalSize += Long.parseLong(element.getText(4));
									}
									FileTransferTabItem.log.log(Level.FINE, "Selection={" + filesInfo + "}"); //$NON-NLS-1$ //$NON-NLS-2$

									final long totalSize = tmpTotalSize;
									new Thread("Upload") { //$NON-NLS-1$
										@Override
										public void run() {
											try {
												FileTransferTabItem.this.serialPort.setInterruptedByUser(false);
												FileTransferTabItem.this.serialPort.upLoadFiles(FileTransferTabItem.this.selectedSdFolder.toString() + GDE.FILE_SEPARATOR_UNIX,
														FileTransferTabItem.this.selectedPcFolder.toString(), filesInfo.toString().split(GDE.STRING_SEMICOLON), totalSize, FileTransferTabItem.this);
												enableStopButton(false);
											}
											catch (Exception e) {
												FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
												enableStopButton(false);
												FileTransferTabItem.this.application.openMessageDialog(e.getMessage());
											}
										}
									}.start();
									FileTransferTabItem.this.stopButton.setEnabled(true);
								}
							});
						}
						{
							this.stopButton = new Button(this.sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData stopButtonnLData = new GridData();
							stopButtonnLData.widthHint = 155;
							stopButtonnLData.heightHint = 30;
							this.stopButton.setLayoutData(stopButtonnLData);
							this.stopButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.stopButton.setText(Messages.getString(MessageIds.GDE_MSGT2435));
							this.stopButton.setEnabled(false);
							this.stopButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									FileTransferTabItem.log.log(Level.FINEST, "stopButton.widgetSelected, event=" + evt); //$NON-NLS-1$
									FileTransferTabItem.this.serialPort.setInterruptedByUser(true);
								}
							});
						}
						{
							this.downLoadButton = new Button(this.sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData downLoadButtonLData = new GridData();
							downLoadButtonLData.widthHint = 155;
							downLoadButtonLData.heightHint = 30;
							this.downLoadButton.setLayoutData(downLoadButtonLData);
							this.downLoadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.downLoadButton.setText(Messages.getString(MessageIds.GDE_MSGT2436));
							this.downLoadButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									FileTransferTabItem.log.log(Level.FINEST, "downLoadButton.widgetSelected, event=" + evt); //$NON-NLS-1$
									TableItem[] selection = FileTransferTabItem.this.pcFoldersTable.getSelection();

									if (selection == null || selection.length < 1 || FileTransferTabItem.this.selectedSdFolder == null || FileTransferTabItem.this.selectedSdFolder.length() < 5
											|| FileTransferTabItem.this.selectedSdFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) < 0) {
										FileTransferTabItem.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT2431));
										return;
									}
									else if (FileTransferTabItem.this.selectedSdFolder == null || FileTransferTabItem.this.selectedPcFolder.length() == 0) {
										FileTransferTabItem.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT2433));
										return;
									}

									final StringBuilder filesInfo = new StringBuilder();
									long tmpTotalSize = 0;
									for (TableItem element : selection) {
										filesInfo.append(element.getText(0)).append(GDE.STRING_COMMA).append(element.getText(1)).append(GDE.STRING_SEMICOLON);
										tmpTotalSize += Long.parseLong(element.getText(4));
									}
									FileTransferTabItem.log.log(Level.FINE, "Selection={" + filesInfo + "}"); //$NON-NLS-1$ //$NON-NLS-2$

									final long totalSize = tmpTotalSize;
									new Thread("Download") { //$NON-NLS-1$
										@Override
										public void run() {
											try {
												FileTransferTabItem.this.serialPort.setInterruptedByUser(false);
												FileTransferTabItem.this.serialPort.downLoadFiles(FileTransferTabItem.this.selectedSdFolder.toString() + GDE.FILE_SEPARATOR_UNIX,
														FileTransferTabItem.this.selectedPcFolder.toString(), filesInfo.toString().split(GDE.STRING_SEMICOLON), totalSize, FileTransferTabItem.this);
												enableStopButton(false);
											}
											catch (Exception e) {
												FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
												FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
												enableStopButton(false);
												FileTransferTabItem.this.application.openMessageDialog(e.getMessage());
											}
										}
									}.start();
									FileTransferTabItem.this.stopButton.setEnabled(true);
								}
							});
						}
						{
							this.disconnectButton = new Button(this.sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData disconnectButtonLData = new GridData();
							disconnectButtonLData.widthHint = 155;
							disconnectButtonLData.heightHint = 30;
							this.disconnectButton.setLayoutData(disconnectButtonLData);
							this.disconnectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.disconnectButton.setText(Messages.getString(MessageIds.GDE_MSGT2441));
							this.disconnectButton.setEnabled(false);
							this.disconnectButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									FileTransferTabItem.log.log(Level.FINEST, "disconnectButton.widgetSelected, event=" + evt); //$NON-NLS-1$
									FileTransferTabItem.this.serialPort.setInterruptedByUser(true);
									FileTransferTabItem.this.serialPort.close();
									setTableHeader(FileTransferTabItem.this.sdCardFoldersTable);
									for (TreeItem item : FileTransferTabItem.this.sdRootDirectoryTreeItem.getItems()) {
										for (TreeItem subItem : item.getItems()) {
											FileTransferTabItem.log.log(Level.FINER, "dispose " + subItem.getText()); //$NON-NLS-1$
											subItem.dispose();
										}
										FileTransferTabItem.log.log(Level.FINER, "dispose " + item.getText()); //$NON-NLS-1$
										item.dispose();
									}
									FileTransferTabItem.this.stopButton.setEnabled(false);
									FileTransferTabItem.this.disconnectButton.setEnabled(false);
									FileTransferTabItem.this.device.configureSerialPortMenu(DeviceCommPort.ICON_SET_IMPORT_CLOSE, Messages.getString(MessageIds.GDE_MSGT2404),
											Messages.getString(MessageIds.GDE_MSGT2404));
								}
							});
						}
					}
					{
						RowData sourceFolderTreeLData = new RowData();
						sourceFolderTreeLData.width = 250;
						sourceFolderTreeLData.height = 160;
						this.sdFolderTree = new Tree(this.sdCardActionGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
						this.sdFolderTree.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.sdFolderTree.setLayoutData(sourceFolderTreeLData);
						this.sdFolderTree.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								FileTransferTabItem.log.log(Level.FINEST, "sourceFolderTree.widgetSelected, event=" + evt); //$NON-NLS-1$
								TreeItem evtTreeitem = (TreeItem) evt.item;
								FileTransferTabItem.log.log(Level.FINEST, "sourceFolderTree.widgetSelected, tree item = " + evtTreeitem.getText()); //$NON-NLS-1$
								if (FileTransferTabItem.this.serialPort.isConnected() && !evtTreeitem.getText().equals(GDE.FILE_SEPARATOR_UNIX)) {
									updateSelectedSdFolder(evtTreeitem);
								}
							}
						});
						{
							this.sdRootDirectoryTreeItem = new TreeItem(this.sdFolderTree, SWT.NONE);
							this.sdRootDirectoryTreeItem.setText(GDE.FILE_SEPARATOR_UNIX);
							this.sdRootDirectoryTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
						}
					}
					{
						this.filler = new Composite(this.sdCardActionGroup, SWT.NONE);
						this.filler.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						GridLayout composite1Layout = new GridLayout();
						composite1Layout.makeColumnsEqualWidth = true;
						RowData composite1LData = new RowData();
						composite1LData.width = 15;
						composite1LData.height = 150;
						this.filler.setLayoutData(composite1LData);
						this.filler.setLayout(composite1Layout);
					}
					{
						this.sdCardFoldersTable = new Table(this.sdCardActionGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
						RowData sourceDirectoryTableLData = new RowData();
						sourceDirectoryTableLData.width = 580;
						sourceDirectoryTableLData.height = 160;
						this.sdCardFoldersTable.setLayoutData(sourceDirectoryTableLData);
						this.sdCardFoldersTable.setLinesVisible(true);
						this.sdCardFoldersTable.setHeaderVisible(true);
						this.sdCardFoldersTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						setTableHeader(this.sdCardFoldersTable);
						this.sdCardFoldersTable.addListener(SWT.Selection, new Listener() {
							@Override
							public void handleEvent(Event event) {
								TableItem item = (TableItem) event.item;
								FileTransferTabItem.log.log(Level.FINE, "Selection={" + item.getText(1) + "}"); //$NON-NLS-1$ //$NON-NLS-2$
								TableItem[] selection = FileTransferTabItem.this.sdCardFoldersTable.getSelection();
								StringBuilder sb = new StringBuilder();
								for (TableItem element : selection)
									sb.append(element).append(GDE.STRING_BLANK);
								FileTransferTabItem.log.log(Level.FINE, "Selection={" + sb.toString() + "}"); //$NON-NLS-1$ //$NON-NLS-2$
							}
						});
					}
					{
						this.tranferProgressComposite = new Composite(this.sdCardActionGroup, SWT.NONE);
						this.tranferProgressComposite.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
						RowLayout spacerCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData spacerCompositeLData = new RowData();
						spacerCompositeLData.width = 925;
						spacerCompositeLData.height = 35;
						this.tranferProgressComposite.setLayoutData(spacerCompositeLData);
						this.tranferProgressComposite.setLayout(spacerCompositeLayout);
						{
							this.transferProgressLabel = new CLabel(this.tranferProgressComposite, SWT.NONE);
							this.transferProgressLabel.setBackground(SWTResourceManager.getColor(this.settings.getUtilitySurroundingBackground()));
							RowData transferProgressLabelLData = new RowData();
							transferProgressLabelLData.width = 869;
							transferProgressLabelLData.height = 18;
							this.transferProgressLabel.setLayoutData(transferProgressLabelLData);
							this.transferProgressLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							this.transferProgressLabel.setText(Messages.getString(MessageIds.GDE_MSGT2443, new Object[] { 0, 0 }));
						}
						{
							RowData transferProgressBarLData = new RowData();
							transferProgressBarLData.width = 869;
							transferProgressBarLData.height = 10;
							this.transferProgressBar = new ProgressBar(this.tranferProgressComposite, SWT.NONE);
							this.transferProgressBar.setLayoutData(transferProgressBarLData);
							this.transferProgressBar.setMinimum(0);
							this.transferProgressBar.setMaximum(100);
						}
					}
					{
						this.deleteFileButton = new Button(this.sdCardActionGroup, SWT.PUSH | SWT.CENTER);
						RowData deleteFileButtonLData = new RowData();
						deleteFileButtonLData.width = 155;
						deleteFileButtonLData.height = 30;
						this.deleteFileButton.setLayoutData(deleteFileButtonLData);
						this.deleteFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						this.deleteFileButton.setText(Messages.getString(MessageIds.GDE_MSGT2444));
						this.deleteFileButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								FileTransferTabItem.log.log(Level.FINEST, "deleteFileButton.widgetSelected, event=" + evt); //$NON-NLS-1$
								TableItem[] selection = FileTransferTabItem.this.sdCardFoldersTable.getSelection();

								if (selection == null || selection.length < 1 || FileTransferTabItem.this.selectedSdFolder == null || FileTransferTabItem.this.selectedSdFolder.length() < 5
										|| FileTransferTabItem.this.selectedSdFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) < 0) {
									FileTransferTabItem.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGT2431));
									return;
								}

								final StringBuilder filesInfo = new StringBuilder();
								for (TableItem element : selection) {
									filesInfo.append(element.getText(1)).append(GDE.STRING_SEMICOLON);
								}
								FileTransferTabItem.log.log(Level.FINE, "Selection={" + filesInfo + "}"); //$NON-NLS-1$ //$NON-NLS-2$

								try {
									FileTransferTabItem.this.serialPort.deleteFiles(FileTransferTabItem.this.selectedSdFolder.toString() + GDE.FILE_SEPARATOR_UNIX, filesInfo.toString().split(GDE.STRING_SEMICOLON));
									updateSelectedSdFolder(FileTransferTabItem.this.lastSelectedSdTreeItem);
								}
								catch (Exception e) {
									FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
								}
							}
						});
					}
				}
				this.innerComposite.layout();
			}
			this.scrolledComposite.setContent(this.outherComposite);
			this.scrolledComposite.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent evt) {
					FileTransferTabItem.log.log(Level.FINEST, "scrolledComposite.controlResized, event=" + evt); //$NON-NLS-1$
					Rectangle bounds = FileTransferTabItem.this.scrolledComposite.getClientArea();
					if (bounds.width > 1100 && bounds.height <= FileTransferTabItem.INNER_COMPOSITE_HEIGHT) {
						FileTransferTabItem.this.outherComposite.setSize(bounds.width, FileTransferTabItem.INNER_COMPOSITE_HEIGHT);
						FileTransferTabItem.this.innerComposite.setLocation((bounds.width - 1100) / 2, 0);
					}
					else if (bounds.width <= 1100 && bounds.height > FileTransferTabItem.INNER_COMPOSITE_HEIGHT) {
						FileTransferTabItem.this.outherComposite.setSize(1100, bounds.height);
						FileTransferTabItem.this.innerComposite.setLocation(0, (bounds.height - FileTransferTabItem.INNER_COMPOSITE_HEIGHT) / 2);
					}
					else if (bounds.width > 1100 && bounds.height > FileTransferTabItem.INNER_COMPOSITE_HEIGHT) {
						FileTransferTabItem.this.outherComposite.setSize(bounds.width, bounds.height);
						FileTransferTabItem.this.innerComposite.setLocation((bounds.width - 1100) / 2, (bounds.height - FileTransferTabItem.INNER_COMPOSITE_HEIGHT) / 2);
					}
					else {
						FileTransferTabItem.this.outherComposite.setSize(1100, FileTransferTabItem.INNER_COMPOSITE_HEIGHT);
						FileTransferTabItem.this.innerComposite.setLocation(0, 0);
					}
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	private void updatePcBaseFolder() {
		for (TreeItem item : this.pcRootTreeItem.getItems()) {
			item.dispose();
		}
		this.selectedPcBaseFolder = new StringBuilder().append(this.pcBaseFolderSelectionLabel.getText().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX));
		this.pcRootTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
		String baseFolderName = this.selectedPcBaseFolder.length() > this.selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1 ? this.selectedPcBaseFolder.substring(this.selectedPcBaseFolder
				.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) + 1) : GDE.IS_WINDOWS ? this.selectedPcBaseFolder.substring(0, this.selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX))
				: this.selectedPcBaseFolder.substring(this.selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX));
		this.pcRootTreeItem.setText(baseFolderName);
		try {
			//getDirListing gets only direct child folders, no sub child folders
			List<File> folderList = FileUtils.getDirListing(new File(this.selectedPcBaseFolder.toString()));
			for (File folder : folderList) {
				TreeItem tmpTreeItem = new TreeItem(this.pcRootTreeItem, SWT.NONE);
				tmpTreeItem.setText(folder.getName());
				tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
			}

			//display opened folder icon and expand the tree if there are child nodes
			if (this.pcRootTreeItem.getItemCount() > 1) {
				this.pcRootTreeItem.setExpanded(true);
				this.pcRootTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif")); //$NON-NLS-1$
			}
		}
		catch (FileNotFoundException e) {
			FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * 
	 */
	private void setTableHeader(Table table) {
		table.removeAll();
		TableColumn[] columns = table.getColumns();
		for (TableColumn tableColumn : columns) {
			tableColumn.dispose();
		}
		this.indexColumn = new TableColumn(table, SWT.CENTER);
		this.indexColumn.setWidth(44);
		this.indexColumn.setText(Messages.getString(MessageIds.GDE_MSGT2445)); //0001 
		this.fileNameColum = new TableColumn(table, SWT.LEFT);
		this.fileNameColum.setWidth(218);
		this.fileNameColum.setText(Messages.getString(MessageIds.GDE_MSGT2446)); //0005_2012-4-25.bin 
		this.fileDateColum = new TableColumn(table, SWT.CENTER);
		this.fileDateColum.setWidth(118);
		this.fileDateColum.setText(Messages.getString(MessageIds.GDE_MSGT2447)); //2012-05-28 
		this.fileTimeColum = new TableColumn(table, SWT.CENTER);
		this.fileTimeColum.setWidth(64);
		this.fileTimeColum.setText(Messages.getString(MessageIds.GDE_MSGT2448)); //2012-05-28
		this.fileSizeColum = new TableColumn(table, SWT.RIGHT);
		this.fileSizeColum.setWidth(123);
		this.fileSizeColum.setText(Messages.getString(MessageIds.GDE_MSGT2449)); //2012-05-28
	}

	private synchronized void updateSdDataTable() {
		if (this.sdFoldersAndFiles.get("FILES") != null) { //$NON-NLS-1$
			for (String fileItem : this.sdFoldersAndFiles.get("FILES")) { //$NON-NLS-1$
				new TableItem(this.sdCardFoldersTable, SWT.NONE).setText(fileItem.split(GDE.STRING_COMMA));
			}
		}
	}

	/**
	 * @param sdSizes
	 */
	private void updateSdCardSizes(long[] sdSizes) {
		if (sdSizes != null && sdSizes.length == 2 && sdSizes[0] > 1000 && sdSizes[1] > 1000) {
			this.sdCardSpaceinfoLabel.setText(Messages.getString(MessageIds.GDE_MSGT2450, new Object[] { sdSizes[0], sdSizes[1] }));
			this.sdCardSpaceProgressBar.setSelection((int) ((sdSizes[0] - sdSizes[1]) * 100 / sdSizes[0]));
		}
	}

	/**
	 * @throws Exception
	 */
	private void listSdCardBaseDirs() throws Exception {
		setTableHeader(this.sdCardFoldersTable);
		for (TreeItem item : this.sdRootDirectoryTreeItem.getItems()) {
			for (TreeItem subItem : item.getItems()) {
				FileTransferTabItem.log.log(Level.FINER, "dispose " + subItem.getText()); //$NON-NLS-1$
				subItem.dispose();
			}
			FileTransferTabItem.log.log(Level.FINER, "dispose " + item.getText()); //$NON-NLS-1$
			item.dispose();
		}
		for (String folder : this.serialPort.querySdDirs()) {
			TreeItem tmpTreeItem = new TreeItem(this.sdRootDirectoryTreeItem, SWT.NONE);
			tmpTreeItem.setText(folder);
			tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
		}
		this.sdRootDirectoryTreeItem.setExpanded(true);
		this.sdRootDirectoryTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif")); //$NON-NLS-1$
	}

	/**
	 * @param evtItem
	 */
	private void updateSelectedPcFolder(TreeItem evtItem) {
		try {
			TreeItem parentItem, tmpItem;
			setTableHeader(this.pcFoldersTable);

			for (TreeItem item : evtItem.getItems()) {
				item.dispose();
			}
			//apply close d folder icon to previous selected tree item
			if (this.lastSelectedPcTreeItem != null && !this.lastSelectedPcTreeItem.isDisposed()) {
				this.lastSelectedPcTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
				while (!this.pcRootTreeItem.getText().equals((parentItem = this.lastSelectedPcTreeItem.getParentItem()).getText())) {
					parentItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
					this.lastSelectedPcTreeItem = parentItem;
				}
			}

			//build path traversing tree items, apply open folder icon
			this.selectedPcFolder = new StringBuilder().append(GDE.FILE_SEPARATOR_UNIX).append(evtItem.getText());
			tmpItem = evtItem;
			while (!this.pcRootTreeItem.getText().equals((parentItem = tmpItem.getParentItem()).getText())) {
				this.selectedPcFolder.insert(0, parentItem.getText());
				this.selectedPcFolder.insert(0, GDE.FILE_SEPARATOR_UNIX);
				parentItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif")); //$NON-NLS-1$
				tmpItem = parentItem;
			}
			this.selectedPcFolder.insert(0, this.selectedPcBaseFolder);

			//update with new folder and file information
			List<File> files = FileUtils.getFileListing(new File(this.selectedPcFolder.toString()), 1);
			int index = 0;
			for (File file : files) {
				new TableItem(this.pcFoldersTable, SWT.NONE).setText(new String[] { GDE.STRING_EMPTY + index++, file.getName(), StringHelper.getFormatedTime("yyyy-MM-dd", file.lastModified()), //$NON-NLS-1$
						StringHelper.getFormatedTime("HH:mm", file.lastModified()), GDE.STRING_EMPTY + file.length() }); //$NON-NLS-1$
			}
			List<File> folders = FileUtils.getDirListing(new File(this.selectedPcFolder.toString()));
			for (File folder : folders) {
				TreeItem tmpTreeItem = new TreeItem(evtItem, SWT.NONE);
				tmpTreeItem.setText(folder.getName());
				tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
			}
			evtItem.setExpanded(true);
			evtItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif")); //$NON-NLS-1$
			this.lastSelectedPcTreeItem = evtItem;
		}
		catch (Exception e) {
			FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @param evtTreeitem
	 */
	private void updateSelectedSdFolder(TreeItem evtTreeitem) {
		try {
			setTableHeader(this.sdCardFoldersTable);

			for (TreeItem item : evtTreeitem.getItems()) {
				item.dispose();
			}
			this.selectedSdFolder = new StringBuilder().append(GDE.FILE_SEPARATOR_UNIX).append(evtTreeitem.getText());
			TreeItem parentItem;
			while (!(parentItem = evtTreeitem.getParentItem()).getText().equals(GDE.FILE_SEPARATOR_UNIX)) {
				this.selectedSdFolder.insert(0, parentItem.getText());
				this.selectedSdFolder.insert(0, GDE.FILE_SEPARATOR_UNIX);
				evtTreeitem = parentItem;
			}
			this.sdFoldersAndFiles = this.serialPort.queryListDir(this.selectedSdFolder.toString());
			for (String folder : this.sdFoldersAndFiles.get("FOLDER")) { //$NON-NLS-1$
				if (folder.length() > 3) {
					TreeItem tmpTreeItem = new TreeItem(evtTreeitem, SWT.NONE);
					tmpTreeItem.setText(folder);
					tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
				}
			}
			evtTreeitem.setExpanded(true);
			evtTreeitem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif")); //$NON-NLS-1$
			updateSdDataTable();
			this.lastSelectedSdTreeItem = evtTreeitem;
		}
		catch (Exception e) {
			FileTransferTabItem.log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	public void updateFileTransferProgress(final long totalSize, final long remainingSize) {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				FileTransferTabItem.this.transferProgressBar.setSelection((int) ((totalSize - remainingSize) * 100 / totalSize));
				FileTransferTabItem.this.transferProgressLabel.setText(Messages.getString(MessageIds.GDE_MSGT2443, new Object[] { (totalSize - remainingSize), totalSize }));
			}
		});
	}

	public void updatePcFolder() {
		final TreeItem treeItem = this.lastSelectedPcTreeItem;
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				updateSelectedPcFolder(treeItem);
			}
		});
	}

	public void updateSdFolder(final long[] sdSizes) {
		final TreeItem treeItem = this.lastSelectedSdTreeItem;
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				updateSdCardSizes(sdSizes);
				updateSelectedSdFolder(treeItem);
			}
		});
	}

	public void enableStopButton(final boolean enable) {
		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				FileTransferTabItem.this.stopButton.setEnabled(enable);
			}
		});
	}
}
