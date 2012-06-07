package gde.device.graupner;

import gde.GDE;
import gde.config.Settings;
import gde.exception.ApplicationConfigurationException;
import gde.exception.SerialPortException;
import gde.exception.TimeOutException;
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
	final static String		$CLASS_NAME									= FileTransferTabItem.class.getName();
	final static Logger		log													= Logger.getLogger(FileTransferTabItem.$CLASS_NAME);

	private ProgressBar transferProgressBar;
	private CLabel transferProgressLabel;
	private Button deleteFileButton;
	private Composite tranferProgressComposite;
	private Button downLoadButton;
	private Button upLoadButton;
	private TreeItem sdRootDirectoryTreeItem;
	private Tree sdFolderTree;
	private ProgressBar sdCardSpaceProgressBar;
	private CLabel sdCardSpaceinfoLabel;
	private Composite sdCardSizeComposite;
	private Button disconnectButton;
	private Button connectButton;
	private Group sdCardActionGroup;
	private Table targetDirectoryTable, sdCardFoldersTable;
	private TableColumn indexColumn, fileNameColum, fileDateColum, fileTimeColum, fileSizeColum;
	private Composite filler;
	private Button pcBaseFolderButton;
	private CLabel pcBaseFolderSelectionLabel;
	private TreeItem pcRootTreeItem;
	private Tree pcFolderTree;
	private Composite sdCardActionComposite;
	private Group pcFolderGroup;
	private Composite innerComposite, outherComposite;
	private ScrolledComposite scrolledComposite;

	final CTabFolder							tabFolder;
	final HoTTAdapter							device;
	final HoTTAdapterSerialPort		serialPort;
	final DataExplorer						application							= DataExplorer.getInstance();
	final Settings								settings								= Settings.getInstance();
	
	HashMap<String, String[]> sdFoldersAndFiles = new HashMap<String, String[]>();
	StringBuilder selectedSdFolder;
	StringBuilder selectedPcBaseFolder = new StringBuilder().append(settings.getDataFilePath());
	StringBuilder selectedPcFolder = new StringBuilder();
	TreeItem lastSelectedSdTreeItem, lastSelectedPcTreeItem;
	
	public FileTransferTabItem(CTabFolder parent, int style, int position, HoTTAdapter useDevice, HoTTAdapterSerialPort useSerialPort) {
		super(parent, style, position);
		this.tabFolder = parent;
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.device = useDevice;
		this.serialPort = useSerialPort;
		this.setText("File Transfer");
		SWTResourceManager.registerResourceUser(this);
		create();
	}
	
	private void create() {
		try {
			scrolledComposite = new ScrolledComposite(this.tabFolder, SWT.H_SCROLL | SWT.V_SCROLL);
			this.setControl(scrolledComposite);
			{
				outherComposite = new Composite(scrolledComposite, SWT.NONE);
				outherComposite.setBounds(0,0,1100, 550);
				outherComposite.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
				
				innerComposite = new Composite(outherComposite, SWT.NONE);
				innerComposite.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
				innerComposite.setSize(1100, 550);
				innerComposite.setLocation(0, 0);
				RowLayout innerCompositeLayout = new RowLayout(org.eclipse.swt.SWT.VERTICAL);
				innerComposite.setLayout(innerCompositeLayout);
				{
					pcFolderGroup = new Group(innerComposite, SWT.NONE);
					pcFolderGroup.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
					RowLayout pcGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					pcFolderGroup.setLayout(pcGroupLayout);
					RowData pcGroupLData = new RowData();
					pcGroupLData.width = 1090;
					pcGroupLData.height = 250;
					pcFolderGroup.setLayoutData(pcGroupLData);
					pcFolderGroup.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
					pcFolderGroup.setText("Computer Target Directories");
					{
						filler = new Composite(pcFolderGroup, SWT.NONE);
						filler.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						GridLayout composite1Layout = new GridLayout();
						composite1Layout.makeColumnsEqualWidth = true;
						RowData composite1LData = new RowData();
						composite1LData.width = 5;
						composite1LData.height = 20;
						filler.setLayoutData(composite1LData);
						filler.setLayout(composite1Layout);
					}
					{
						pcBaseFolderButton = new Button(pcFolderGroup, SWT.PUSH | SWT.CENTER);
						RowData pcBaseFolderButtonLData = new RowData();
						pcBaseFolderButtonLData.width = 153;
						pcBaseFolderButtonLData.height = 30;
						pcBaseFolderButton.setLayoutData(pcBaseFolderButtonLData);
						pcBaseFolderButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						pcBaseFolderButton.setText("Select base folder");
						pcBaseFolderButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "deleteFileButton.widgetSelected, event="+evt);
								String baseFolderName = application.openDirFileDialog("Select pc base folder", selectedPcBaseFolder.toString());
								if (baseFolderName != null && baseFolderName.length() > 0) {
									pcBaseFolderSelectionLabel.setText(baseFolderName);
									updatePcBaseFolder();
								}
							}
						});
					}
					{
						pcBaseFolderSelectionLabel = new CLabel(pcFolderGroup, SWT.NONE);
						pcBaseFolderSelectionLabel.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						pcBaseFolderSelectionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						pcBaseFolderSelectionLabel.setText(selectedPcBaseFolder.toString());
						RowData pcBaseFolderSelectionLabelLData = new RowData();
						pcBaseFolderSelectionLabelLData.width = 580+337;
						pcBaseFolderSelectionLabelLData.height = 26;
						pcBaseFolderSelectionLabel.setLayoutData(pcBaseFolderSelectionLabelLData);
					}
					{
						filler = new Composite(pcFolderGroup, SWT.NONE);
						filler.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						GridLayout compositeLayout = new GridLayout();
						compositeLayout.makeColumnsEqualWidth = true;
						RowData compositeLData = new RowData();
						compositeLData.width = 5;
						compositeLData.height = 180;
						filler.setLayoutData(compositeLData);
						filler.setLayout(compositeLayout);
					}
					{
						RowData pcFolderTreeLData = new RowData();
						pcFolderTreeLData.width = 425;
						pcFolderTreeLData.height = 185;
						pcFolderTree = new Tree(pcFolderGroup, SWT.BORDER);
						pcFolderTree.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						pcFolderTree.setLayoutData(pcFolderTreeLData);
						pcFolderTree.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "pcFolderTree.widgetSelected, event="+evt);
								TreeItem evtItem = (TreeItem) evt.item;
								log.log(Level.FINEST, "pcFolderTree.widgetSelected, tree item = "+ evtItem.getText());
								updateSelectedPcFolder(evtItem);
							}
						});
						{
							pcRootTreeItem = new TreeItem(pcFolderTree, SWT.NONE);
							pcRootTreeItem.setText(selectedPcBaseFolder.substring(selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1));
							updatePcBaseFolder();
						}
					}
					{
						filler = new Composite(pcFolderGroup, SWT.NONE);
						filler.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						GridLayout composite1Layout = new GridLayout();
						composite1Layout.makeColumnsEqualWidth = true;
						RowData composite1LData = new RowData();
						composite1LData.width = 15;
						composite1LData.height = 180;
						filler.setLayoutData(composite1LData);
						filler.setLayout(composite1Layout);
					}
					{
						targetDirectoryTable = new Table(pcFolderGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
						RowData targetDirectoryTableLData = new RowData();
						targetDirectoryTableLData.width = 580;
						targetDirectoryTableLData.height = 185;
						targetDirectoryTable.setLayoutData(targetDirectoryTableLData);
						targetDirectoryTable.setLinesVisible(true);
						targetDirectoryTable.setHeaderVisible(true);
						targetDirectoryTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						setTableHeader(targetDirectoryTable);
						targetDirectoryTable.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
										TableItem item = (TableItem) event.item;
										log.log(Level.FINE, "Selection={" + item.getText(1) + "}");			
										TableItem[] selection = targetDirectoryTable.getSelection();
										String string = "";
										for (int i=0; i<selection.length; i++) string += selection [i] + " ";
										log.log(Level.FINE, "Selection={" + string + "}");
							}
						});
					}
				}
				{
					sdCardActionGroup = new Group(innerComposite, SWT.NONE);
					sdCardActionGroup.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
					RowLayout TransmitterSourceGroupLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					sdCardActionGroup.setLayout(TransmitterSourceGroupLayout);
					RowData transmitterSourceGroupLData = new RowData();
					transmitterSourceGroupLData.width = 1090;
					transmitterSourceGroupLData.height = 250;
					sdCardActionGroup.setLayoutData(transmitterSourceGroupLData);
					sdCardActionGroup.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
					sdCardActionGroup.setText("Transmitter Source Control");
					{
						sdCardSizeComposite = new Composite(sdCardActionGroup, SWT.NONE);
						sdCardSizeComposite.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						RowLayout sdCardSizeCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData sdCardSizeCompositeLData = new RowData();
						sdCardSizeCompositeLData.width = 923;
						sdCardSizeCompositeLData.height = 36;
						sdCardSizeComposite.setLayoutData(sdCardSizeCompositeLData);
						sdCardSizeComposite.setLayout(sdCardSizeCompositeLayout);
						{
							sdCardSpaceinfoLabel = new CLabel(sdCardSizeComposite, SWT.NONE);
							sdCardSpaceinfoLabel.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
							RowData sdCardSpaceinfoLabelLData = new RowData();
							sdCardSpaceinfoLabelLData.width = 869;
							sdCardSpaceinfoLabelLData.height = 18;
							sdCardSpaceinfoLabel.setLayoutData(sdCardSpaceinfoLabelLData);
							sdCardSpaceinfoLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							sdCardSpaceinfoLabel.setText("Disk Space - Total : Mbytes - Free : MBytes");
						}
						{
							sdCardSpaceProgressBar = new ProgressBar(sdCardSizeComposite, SWT.NONE);
							RowData sdCardSpaceProgressBarLData = new RowData();
							sdCardSpaceProgressBarLData.width = 869;
							sdCardSpaceProgressBarLData.height = 10;
							sdCardSpaceProgressBar.setLayoutData(sdCardSpaceProgressBarLData);
							sdCardSpaceProgressBar.setMinimum(0);
							sdCardSpaceProgressBar.setMaximum(100);
						}
					}
					{
						sdCardActionComposite = new Composite(sdCardActionGroup, SWT.NONE);
						sdCardActionComposite.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						GridLayout composite2Layout = new GridLayout();
						composite2Layout.makeColumnsEqualWidth = true;
						sdCardActionComposite.setLayout(composite2Layout);
						RowData composite2LData = new RowData();
						composite2LData.width = 203;
						composite2LData.height = 150;
						sdCardActionComposite.setLayoutData(composite2LData);
						{
							connectButton = new Button(sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData connectButtonLData = new GridData();
							connectButtonLData.widthHint = 153;
							connectButtonLData.heightHint = 31;
							connectButton.setLayoutData(connectButtonLData);
							connectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							connectButton.setText("Connect");
							connectButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "connectButton.widgetSelected, event="+evt);									
									try {
										if (!serialPort.isConnected()) {
											serialPort.open();
										}
										serialPort.prepareSdCard();
										
										updateSdCardSizes(serialPort.querySdCardSizes());
										listSdCardBaseDirs();
									}
									catch (SerialPortException e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}
									catch (ApplicationConfigurationException e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}
									catch (IOException e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}
									catch (TimeOutException e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}
									catch (Exception e) {
										log.log(Level.SEVERE, e.getMessage(), e);
									}
								}
							});
						}
						{
							upLoadButton = new Button(sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData upLoadButtonLData = new GridData();
							upLoadButtonLData.widthHint = 153;
							upLoadButtonLData.heightHint = 31;
							upLoadButton.setLayoutData(upLoadButtonLData);
							upLoadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							upLoadButton.setText("SD -> PC");
							upLoadButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "upLoadButton.widgetSelected, event="+evt);
									TableItem[] selection = sdCardFoldersTable.getSelection();
									
									if (selection == null || selection.length < 1 || selectedSdFolder == null || selectedSdFolder.length() < 5 || selectedSdFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) < 0) {
										application.openMessageDialog("There is no source file selected, select file and re-start transfer.");
										return;
									}
									else if (selectedPcFolder == null || selectedPcFolder.length() == 0) {
										application.openMessageDialog("There is no PC target folder selected, select target folder and re-start transfer.");
										return;
									}
									
									final StringBuilder filesInfo = new StringBuilder();
									long tmpTotalSize = 0;
									for (int i=0; i<selection.length; i++) {
										String[] date = selection[i].getText(2).split(GDE.STRING_DASH);
										String[] time = selection[i].getText(3).split(GDE.STRING_COLON);
										GregorianCalendar calendar = new GregorianCalendar(Integer.parseInt(date[0]),Integer.parseInt(date[1])-1,Integer.parseInt(date[2]), Integer.parseInt(time[0]),Integer.parseInt(time[1]));
										long timeStamp = calendar.getTimeInMillis();
										filesInfo.append(selection[i].getText(0)).append(GDE.STRING_COMMA).append(selection[i].getText(1)).append(GDE.STRING_COMMA).append(timeStamp).append(GDE.STRING_SEMICOLON);
										tmpTotalSize += Long.parseLong(selection[i].getText(4));
									}
									log.log(Level.FINE, "Selection={" + filesInfo + "}");
									
									final long totalSize = tmpTotalSize;
									new Thread("Upload") {
										@Override
										public void run() {
											try {
												serialPort.upLoadFiles(selectedSdFolder.toString()+GDE.FILE_SEPARATOR_UNIX, selectedPcFolder.toString(), filesInfo.toString().split(GDE.STRING_SEMICOLON), totalSize, 
														FileTransferTabItem.this);
											}
											catch (Exception e) {
												log.log(Level.SEVERE, e.getMessage(), e);
											}
										}
									}.start();
								}
							});
						}
						{
							downLoadButton = new Button(sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData downLoadButtonLData = new GridData();
							downLoadButtonLData.widthHint = 153;
							downLoadButtonLData.heightHint = 31;
							downLoadButton.setLayoutData(downLoadButtonLData);
							downLoadButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							downLoadButton.setText("PC -> SD");
							downLoadButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "downLoadButton.widgetSelected, event="+evt);
									TableItem[] selection = targetDirectoryTable.getSelection();
									
									if (selection == null || selection.length < 1 || selectedSdFolder == null || selectedSdFolder.length() < 5 || selectedSdFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) < 0) {
										application.openMessageDialog("There is no source file selected, select file and re-start transfer.");
										return;
									}
									else if (selectedSdFolder == null || selectedPcFolder.length() == 0) {
										application.openMessageDialog("There is no PC target folder selected, select target folder and re-start transfer.");
										return;
									}
									
									final StringBuilder filesInfo = new StringBuilder();
									long tmpTotalSize = 0;
									for (int i=0; i<selection.length; i++) {
										filesInfo.append(selection[i].getText(0)).append(GDE.STRING_COMMA).append(selection[i].getText(1)).append(GDE.STRING_SEMICOLON);
										tmpTotalSize += Long.parseLong(selection[i].getText(4));
									}
									log.log(Level.FINE, "Selection={" + filesInfo + "}");
									
									final long totalSize = tmpTotalSize;
									new Thread("Download") {
										@Override
										public void run() {
											try {
												serialPort.downLoadFiles(selectedSdFolder.toString()+GDE.FILE_SEPARATOR_UNIX, selectedPcFolder.toString(), filesInfo.toString().split(GDE.STRING_SEMICOLON), totalSize, 
														FileTransferTabItem.this);
											}
											catch (Exception e) {
												log.log(Level.SEVERE, e.getMessage(), e);
											}
										}
									}.start();
								}
							});
						}
						{
							disconnectButton = new Button(sdCardActionComposite, SWT.PUSH | SWT.CENTER);
							GridData disconnectButtonLData = new GridData();
							disconnectButtonLData.widthHint = 153;
							disconnectButtonLData.heightHint = 31;
							disconnectButton.setLayoutData(disconnectButtonLData);
							disconnectButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							disconnectButton.setText("Disconnect");
							disconnectButton.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent evt) {
									log.log(Level.FINEST, "disconnectButton.widgetSelected, event="+evt);
									serialPort.close();
									setTableHeader(sdCardFoldersTable);
									for (TreeItem item : sdRootDirectoryTreeItem.getItems()) {
										for (TreeItem subItem : item.getItems()) { 
											log.log(Level.FINER,  "dispose " + subItem.getText());
											subItem.dispose();
										}
										log.log(Level.FINER,  "dispose " + item.getText());
										item.dispose();
									}
								}
							});
						}
					}
					{
						RowData sourceFolderTreeLData = new RowData();
						sourceFolderTreeLData.width = 250;
						sourceFolderTreeLData.height = 130;
						sdFolderTree = new Tree(sdCardActionGroup, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
						sdFolderTree.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						sdFolderTree.setLayoutData(sourceFolderTreeLData);
						sdFolderTree.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "sourceFolderTree.widgetSelected, event="+evt);
								TreeItem evtTreeitem = (TreeItem) evt.item;
								log.log(Level.FINEST, "sourceFolderTree.widgetSelected, tree item = "+ evtTreeitem.getText());
								if (serialPort.isConnected() && !evtTreeitem.getText().equals(GDE.FILE_SEPARATOR_UNIX)) {
									updateSelectedSdFolder(evtTreeitem);
								}
							}
						});
						{
							sdRootDirectoryTreeItem = new TreeItem(sdFolderTree, SWT.NONE);
							sdRootDirectoryTreeItem.setText("/");
							sdRootDirectoryTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
						}
					}
					{
						sdCardFoldersTable = new Table(sdCardActionGroup, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
						RowData sourceDirectoryTableLData = new RowData();
						sourceDirectoryTableLData.width = 580;
						sourceDirectoryTableLData.height = 130;
						sdCardFoldersTable.setLayoutData(sourceDirectoryTableLData);
						sdCardFoldersTable.setLinesVisible(true);
						sdCardFoldersTable.setHeaderVisible(true);
						sdCardFoldersTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						setTableHeader(sdCardFoldersTable);
						sdCardFoldersTable.addListener(SWT.Selection, new Listener() {
							public void handleEvent(Event event) {
										TableItem item = (TableItem) event.item;
										log.log(Level.FINE, "Selection={" + item.getText(1) + "}");			
										TableItem[] selection = sdCardFoldersTable.getSelection();
										String string = "";
										for (int i=0; i<selection.length; i++) string += selection [i] + " ";
										log.log(Level.FINE, "Selection={" + string + "}");
							}
						});
					}
					{
						tranferProgressComposite = new Composite(sdCardActionGroup, SWT.NONE);
						tranferProgressComposite.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
						RowLayout spacerCompositeLayout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
						RowData spacerCompositeLData = new RowData();
						spacerCompositeLData.width = 925;
						spacerCompositeLData.height = 35;
						tranferProgressComposite.setLayoutData(spacerCompositeLData);
						tranferProgressComposite.setLayout(spacerCompositeLayout);
						{
							transferProgressLabel = new CLabel(tranferProgressComposite, SWT.NONE);
							transferProgressLabel.setBackground(SWTResourceManager.getColor(settings.getUtilitySurroundingBackground()));
							RowData transferProgressLabelLData = new RowData();
							transferProgressLabelLData.width = 869;
							transferProgressLabelLData.height = 18;
							transferProgressLabel.setLayoutData(transferProgressLabelLData);
							transferProgressLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
							transferProgressLabel.setText("file transfer : " + 0 + " of " + 0 + " bytes");
						}
						{
							RowData transferProgressBarLData = new RowData();
							transferProgressBarLData.width = 869;
							transferProgressBarLData.height = 10;
							transferProgressBar = new ProgressBar(tranferProgressComposite, SWT.NONE);
							transferProgressBar.setLayoutData(transferProgressBarLData);
							transferProgressBar.setMinimum(0);
							transferProgressBar.setMaximum(100);
						}
					}
					{
						deleteFileButton = new Button(sdCardActionGroup, SWT.PUSH | SWT.CENTER);
						RowData deleteFileButtonLData = new RowData();
						deleteFileButtonLData.width = 150;
						deleteFileButtonLData.height = 31;
						deleteFileButton.setLayoutData(deleteFileButtonLData);
						deleteFileButton.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
						deleteFileButton.setText("delete selected file");
						deleteFileButton.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "deleteFileButton.widgetSelected, event="+evt);
								TableItem[] selection = sdCardFoldersTable.getSelection();
								
								if (selection == null || selection.length < 1 || selectedSdFolder == null || selectedSdFolder.length() < 5 || selectedSdFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX) < 0) {
									application.openMessageDialog("There is no source file selected, select file and re-start transfer.");
									return;
								}
								
								final StringBuilder filesInfo = new StringBuilder();
								for (int i=0; i<selection.length; i++) {
									filesInfo.append(selection[i].getText(1)).append(GDE.STRING_SEMICOLON);
								}
								log.log(Level.FINE, "Selection={" + filesInfo + "}");

								try {
									serialPort.deleteFiles(selectedSdFolder.toString()+GDE.FILE_SEPARATOR_UNIX, filesInfo.toString().split(GDE.STRING_SEMICOLON));
									updateSelectedSdFolder(lastSelectedSdTreeItem);
								}
								catch (Exception e) {
									log.log(Level.SEVERE, e.getMessage(), e);
								}
							}
						});
					}
				}
				innerComposite.layout();
				System.out.println(pcFolderGroup.getClientArea().width);
			}
			scrolledComposite.setContent(outherComposite);
			scrolledComposite.addControlListener(new ControlAdapter() {
				@Override
				public void controlResized(ControlEvent evt) {
					log.log(Level.FINEST, "scrolledComposite.controlResized, event="+evt);
					Rectangle bounds = scrolledComposite.getClientArea();
					if (bounds.width > 1100 && bounds.height <= 550) {
						outherComposite.setSize(bounds.width, 550);
						innerComposite.setLocation((bounds.width-1100)/2, 0);
					}
					else if (bounds.width <= 1100 && bounds.height > 550) {
						outherComposite.setSize(1100, bounds.height);
						innerComposite.setLocation(0, (bounds.height-550)/2);
					}
					else if (bounds.width > 1100 && bounds.height > 550) {
						outherComposite.setSize(bounds.width, bounds.height);
						innerComposite.setLocation((bounds.width-1100)/2, (bounds.height-550)/2);
					}
					else {
						outherComposite.setSize(1100, 550);
						innerComposite.setLocation(0, 0);
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
		for (TreeItem item : pcRootTreeItem.getItems()) {
			item.dispose();
		}
		selectedPcBaseFolder = new StringBuilder().append(pcBaseFolderSelectionLabel.getText().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX));
		pcRootTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
		String baseFolderName = selectedPcBaseFolder.length() > selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1 ? selectedPcBaseFolder.substring(selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1) 
				: GDE.IS_WINDOWS ? selectedPcBaseFolder.substring(0, selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)) : selectedPcBaseFolder.substring(selectedPcBaseFolder.lastIndexOf(GDE.FILE_SEPARATOR_UNIX));
		pcRootTreeItem.setText(baseFolderName);
		try {
			//getDirListing gets only direct child folders, no sub child folders
			List<File> folderList = FileUtils.getDirListing(new File(selectedPcBaseFolder.toString()));
			for (File folder : folderList) {
					TreeItem tmpTreeItem = new TreeItem(pcRootTreeItem, SWT.NONE);
					tmpTreeItem.setText(folder.getName());
					tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
			}
			
			//display opened folder icon and expand the tree if there are child nodes
			if (pcRootTreeItem.getItemCount() > 1) {
				pcRootTreeItem.setExpanded(true);
				pcRootTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif"));
			}
		}
		catch (FileNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
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
		indexColumn = new TableColumn(table, SWT.CENTER);
		indexColumn.setWidth(44);
		indexColumn.setText("No"); //0001
		fileNameColum = new TableColumn(table, SWT.LEFT);
		fileNameColum.setWidth(218);
		fileNameColum.setText("                      file name"); //0005_2012-4-25.bin
		fileDateColum = new TableColumn(table, SWT.CENTER);
		fileDateColum.setWidth(118);
		fileDateColum.setText("date"); //2012-05-28
		fileTimeColum = new TableColumn(table, SWT.CENTER);
		fileTimeColum.setWidth(64);
		fileTimeColum.setText("time"); //2012-05-28
		fileSizeColum = new TableColumn(table, SWT.RIGHT);
		fileSizeColum.setWidth(123);
		fileSizeColum.setText("size [Byte]         "); //2012-05-28
	}

	private synchronized void updateSdDataTable() {
		if (sdFoldersAndFiles.get("FILES") != null) {
			for (String fileItem : sdFoldersAndFiles.get("FILES")) {
				new TableItem(sdCardFoldersTable, SWT.NONE).setText(fileItem.split(GDE.STRING_COMMA));
			}
		}
	}

	/**
	 * @param sdSizes
	 */
	private void updateSdCardSizes(long[] sdSizes) {
		if (sdSizes != null && sdSizes.length == 2 && sdSizes[0]  > 1000 && sdSizes[1] > 1000) {
			sdCardSpaceinfoLabel.setText("Disk Space - Total : " + sdSizes[0] + " KBytes - Free : " + sdSizes[1] + " KBytes");
			sdCardSpaceProgressBar.setSelection((int) ((sdSizes[0]-sdSizes[1])*100/sdSizes[0]));
		}
	}

	/**
	 * @throws Exception
	 */
	private void listSdCardBaseDirs() throws Exception {
		setTableHeader(sdCardFoldersTable);
		for (TreeItem item : sdRootDirectoryTreeItem.getItems()) {
			for (TreeItem subItem : item.getItems()) { 
				log.log(Level.FINER,  "dispose " + subItem.getText());
				subItem.dispose();
			}
			log.log(Level.FINER,  "dispose " + item.getText());
			item.dispose();
		}
		for (String folder : serialPort.querySdDirs()) {
			TreeItem tmpTreeItem = new TreeItem(sdRootDirectoryTreeItem, SWT.NONE);
			tmpTreeItem.setText(folder);
			tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
		}
		sdRootDirectoryTreeItem.setExpanded(true);
		sdRootDirectoryTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif"));
	}

	/**
	 * @param evtItem
	 */
	private void updateSelectedPcFolder(TreeItem evtItem) {
		try {
			TreeItem parentItem, tmpItem;
			setTableHeader(targetDirectoryTable);

			for (TreeItem item : evtItem.getItems()) {
				item.dispose();
			}
			//apply close d folder icon to previous selected tree item
			if (lastSelectedPcTreeItem != null && !lastSelectedPcTreeItem.isDisposed()) {
				lastSelectedPcTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
				while (!pcRootTreeItem.getText().equals((parentItem = lastSelectedPcTreeItem.getParentItem()).getText())) {
					parentItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
					lastSelectedPcTreeItem = parentItem;
				}
			}

			//build path traversing tree items, apply open folder icon
			selectedPcFolder = new StringBuilder().append(GDE.FILE_SEPARATOR_UNIX).append(evtItem.getText());
			tmpItem = evtItem;
			while (!pcRootTreeItem.getText().equals((parentItem = tmpItem.getParentItem()).getText())) {
				selectedPcFolder.insert(0, parentItem.getText());
				selectedPcFolder.insert(0, GDE.FILE_SEPARATOR_UNIX );
				parentItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif"));
				tmpItem = parentItem;
			}
			selectedPcFolder.insert(0, selectedPcBaseFolder);
			
			//update with new folder and file information
			List<File> files = FileUtils.getFileListing(new File(selectedPcFolder.toString()), 1);
			int index = 0;
			for (File file : files) {
				new TableItem(targetDirectoryTable, SWT.NONE).setText(new String[] { GDE.STRING_EMPTY + index++, file.getName(), StringHelper.getFormatedTime("yyyy-MM-dd", file.lastModified()),
						StringHelper.getFormatedTime("HH:mm", file.lastModified()), GDE.STRING_EMPTY + file.length() });
			}
			List<File> folders = FileUtils.getDirListing(new File(selectedPcFolder.toString()));
			for (File folder : folders) {
					TreeItem tmpTreeItem = new TreeItem(evtItem, SWT.NONE);
					tmpTreeItem.setText(folder.getName());
					tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
			}							
			evtItem.setExpanded(true);
			evtItem.setImage(SWTResourceManager.getImage("/gde/resource/FolderOpen.gif"));
			lastSelectedPcTreeItem = evtItem;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @param evtTreeitem
	 */
	private void updateSelectedSdFolder(TreeItem evtTreeitem) {
		try {
			setTableHeader(sdCardFoldersTable);

			for (TreeItem item : evtTreeitem.getItems()) {
				item.dispose();
			}
			selectedSdFolder = new StringBuilder().append(GDE.FILE_SEPARATOR_UNIX ).append(evtTreeitem.getText());
			TreeItem parentItem;
			while (!(parentItem = evtTreeitem.getParentItem()).getText().equals(GDE.FILE_SEPARATOR_UNIX)) {
				selectedSdFolder.insert(0, parentItem.getText());
				selectedSdFolder.insert(0, GDE.FILE_SEPARATOR_UNIX );
				evtTreeitem = parentItem;
			}
			sdFoldersAndFiles = serialPort.queryListDir(selectedSdFolder.toString());
			for (String folder : sdFoldersAndFiles.get("FOLDER")) {
				if (folder.length() > 3) {
					TreeItem tmpTreeItem = new TreeItem(evtTreeitem, SWT.NONE);
					tmpTreeItem.setText(folder);
					tmpTreeItem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
				}
			}
			evtTreeitem.setExpanded(true);
			evtTreeitem.setImage(SWTResourceManager.getImage("/gde/resource/Folder.gif"));
			updateSdDataTable();
			lastSelectedSdTreeItem = evtTreeitem;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void updateFileTransferProgress(final long totalSize, final long remainingSize) {
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				transferProgressBar.setSelection((int) ((totalSize - remainingSize) * 100 / totalSize));
				transferProgressLabel.setText("file transfer : " + (totalSize - remainingSize) + " of " + totalSize + " bytes");
			}
		});
	}
	
	public void updatePcFolder() {
		final TreeItem treeItem = lastSelectedPcTreeItem;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				updateSelectedPcFolder(treeItem);
			}
		});
	}
	
	public void updateSdFolder(final long[] sdSizes) {
		final TreeItem treeItem = lastSelectedSdTreeItem;
		GDE.display.asyncExec(new Runnable() {
			public void run() {
				updateSdCardSizes(sdSizes);
				updateSelectedSdFolder(treeItem);
			}
		});
	}
}
