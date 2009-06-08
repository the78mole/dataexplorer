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
package osde.ui.tab;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import osde.OSDE;
import osde.config.Settings;
import osde.data.ObjectData;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.ImageContextMenu;

/**
 * @author Winfried Brügmann
 * This class defines the layout for the object description, this window(tab) will only shown, if not device oriented
 */
public class ObjectDescriptionWindow {

	final static Logger									 log	= Logger.getLogger(ObjectDescriptionWindow.class.getName());

	private CTabItem							objectTabItem;
	private StyledText						styledText;
	private Canvas								imageCanvas;
	private CLabel							objectName;
	private CLabel								objectNameLabel;
	private ToolItem							fontSelectItem;
	private ToolBar								editToolBar;
	private Composite							tabComposite;

	final OpenSerialDataExplorer	application;
	String objectFilePath;
	String activeObjectKey;
	boolean	isObjectDataSaved = true;
	private Composite composite1;

	private Composite statusComposite;
	private Text objectTypeText;
	private CLabel objectTypeLable;
	private Composite typeComposite;
	private Group mainObjectCharacterisitcsGroup;
	private CLabel dateLabel;
	private Composite dateComposite;
	private Group editComposite;
	private ToolItem colorItem;
	private Text dateText;
	private ToolItem leftJustifyItem;
	private ToolItem strikeThroughItem;
	private ToolItem underlineItem;
	private ToolItem italicItem;
	private ToolItem boldItem;
	private ToolItem fontSizeItem;
	private CCombo statusText;
	private CLabel statusLabel; 
		
	Menu													popupmenu;
	ImageContextMenu contextMenu;
	ObjectData	object;
	Image image;

	final Settings								settings;
	final CTabFolder							tabFolder;
	static private CTabFolder cTabFolder1;
	private Composite headerComposite;

	public ObjectDescriptionWindow(OpenSerialDataExplorer currenApplication, CTabFolder objectDescriptionTab) {
		this.application = currenApplication;
		this.tabFolder = objectDescriptionTab;
		this.settings = Settings.getInstance();
		this.activeObjectKey = this.settings.getActiveObject();
	}

	public ObjectDescriptionWindow(CTabFolder objectDescriptionTab) {
		this.application = null;
		this.tabFolder = objectDescriptionTab;
		this.settings = null;
		this.tabFolder.setSize(1020, 554);
	}

	public void setVisible(boolean isVisible) {
		if (isVisible) {
			if (objectTabItem.isDisposed()) {
				create();
			}
			this.isObjectDataSaved = false;
		}
		else {
			objectTabItem.getControl().dispose();
			objectTabItem.dispose();
		}
	}
	
	/**
	 * loads the object relevant data from file if exist, or defaults
	 */
	public void update() {
		if (!this.objectTabItem.isDisposed()) {
			if (this.object != null && !this.isObjectDataSaved) {
				this.object.setType(this.objectTypeText.getText());
				this.object.setActivationDate(this.dateText.getText());
				this.object.setStatus(this.statusText.getText());
				this.object.setImage(SWTResourceManager.getImage(this.image.getImageData(), this.activeObjectKey));
				this.object.setStyledText(this.styledText.getText());
				this.object.setStyleRanges(this.styledText.getStyleRanges());
				this.object.save(this.objectFilePath + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_ENDING_DOT_ZIP);
				this.isObjectDataSaved = true;
			}
			this.activeObjectKey = this.application.getMenuToolBar().getActiveObjectKey();
			this.objectFilePath = this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey;
			if (new File(this.objectFilePath + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_ENDING_DOT_ZIP).exists()) {
				this.object = ObjectData.load(this.objectFilePath + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_ENDING_DOT_ZIP);
				this.objectName.setText(this.object.getKey());
				this.objectTypeText.setText(this.object.getType());
				this.dateText.setText(this.object.getActivationDate());
				this.statusText.setText(this.object.getStatus());
				this.image = SWTResourceManager.getImage(this.object.getImage().getImageData(), this.object.getKey());
				this.styledText.setText(this.object.getStyledText());
				this.styledText.setStyleRanges(this.object.getStyleRanges());
				this.isObjectDataSaved = true;
			}
			else { //use defaults
				this.object = new ObjectData(this.objectFilePath);
				this.objectName.setText(this.object.getKey());
				this.objectTypeText.setText(this.object.getType());
				this.dateText.setText(this.object.getActivationDate());
				this.statusText.setText(this.object.getStatus());
				this.image = SWTResourceManager.getImage("osde/resource/" + this.settings.getLocale() + "/ObjectImage.gif");
				styledText.setText(this.object.getStyledText());
				this.styledText.setStyleRanges(this.object.getStyleRanges());
				this.isObjectDataSaved = false;
			}
			this.imageCanvas.redraw();
		}
	}
	
	/**
	 * creates the window content
	 */
	public void create() {
		objectTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		objectTabItem.setText("Object Characteristics");
		{
			tabComposite = new Composite(this.tabFolder, SWT.NONE);
			objectTabItem.setControl(tabComposite);
			FormLayout composite1Layout = new FormLayout();
			tabComposite.setLayout(composite1Layout);
			tabComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			{
				headerComposite = new Composite(tabComposite, SWT.NONE);
				RowLayout composite2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				headerComposite.setLayout(composite2Layout);
				FormData composite2LData = new FormData();
				composite2LData.width = 419;
				composite2LData.height = 36;
				composite2LData.left =  new FormAttachment(0, 1000, 15);
				composite2LData.top =  new FormAttachment(0, 1000, 17);
				headerComposite.setLayoutData(composite2LData);
				headerComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				{
					objectNameLabel = new CLabel(headerComposite, SWT.NONE);
					objectNameLabel.setText("Object Name :");
					objectNameLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif",12,0,false,false));
					objectNameLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				}
				{
					objectName = new CLabel(headerComposite, SWT.NONE);
					objectName.setFont(SWTResourceManager.getFont("Microsoft Sans Serif",12,1,false,false));			
					RowData cLabel1LData = new RowData();
					cLabel1LData.width = 299;
					cLabel1LData.height = 26;
					objectName.setLayoutData(cLabel1LData);
					objectName.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				}
			}
			{
				mainObjectCharacterisitcsGroup = new Group(tabComposite, SWT.NONE);
				GridLayout group2Layout = new GridLayout();
				group2Layout.makeColumnsEqualWidth = true;
				mainObjectCharacterisitcsGroup.setLayout(group2Layout);
				FormData group2LData = new FormData();
				group2LData.width = 414;
				group2LData.height = 435;
				group2LData.left =  new FormAttachment(0, 1000, 15);
				group2LData.top =  new FormAttachment(0, 1000, 60);
				group2LData.bottom =  new FormAttachment(1000, 1000, -15);
				group2LData.right =  new FormAttachment(426, 1000, 0);
				mainObjectCharacterisitcsGroup.setLayoutData(group2LData);
				mainObjectCharacterisitcsGroup.setText("Main Characteristics ");
				mainObjectCharacterisitcsGroup.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				mainObjectCharacterisitcsGroup.setToolTipText("Describes the main characteristics of the object");
				{
					typeComposite = new Composite(mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout3 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					typeComposite.setLayout(composite1Layout3);
					GridData typeCompositeLData = new GridData();
					typeCompositeLData.verticalAlignment = GridData.BEGINNING;
					typeCompositeLData.grabExcessHorizontalSpace = true;
					typeCompositeLData.horizontalAlignment = GridData.BEGINNING;
					typeCompositeLData.heightHint = 28;
					typeComposite.setLayoutData(typeCompositeLData);
					typeComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						objectTypeLable = new CLabel(typeComposite, SWT.NONE);
						objectTypeLable.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						objectTypeLable.setText("Object Type :");
						RowData cLabel1LData1 = new RowData();
						cLabel1LData1.width = 120;
						cLabel1LData1.height = 22;
						objectTypeLable.setLayoutData(cLabel1LData1);
						objectTypeLable.setToolTipText("give a type name, sample:battery, Soaring model, motor flyer, trainer, solar collector, ....");
					}
					{
						objectTypeText = new Text(typeComposite, SWT.NONE);
						objectTypeText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						objectTypeText.setEditable(true);
						RowData cLabel2LData = new RowData();
						cLabel2LData.width = 267;
						cLabel2LData.height = 23;
						objectTypeText.setLayoutData(cLabel2LData);
						objectTypeText.setToolTipText("give a type name, sample: NiMh battery, soaring model, motor flyer, trainer, solar collector, ....");
						objectTypeText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "objectTypeText.keyReleased, event="+evt);
								isObjectDataSaved = false;
							}
						});
					}
				}
				{
					dateComposite = new Composite(mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout1 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					dateComposite.setLayout(composite1Layout1);
					GridData dateCompositeLData = new GridData();
					dateCompositeLData.grabExcessHorizontalSpace = true;
					dateCompositeLData.verticalAlignment = GridData.BEGINNING;
					dateCompositeLData.widthHint = 246;
					dateCompositeLData.horizontalAlignment = GridData.BEGINNING;
					dateCompositeLData.heightHint = 28;
					dateComposite.setLayoutData(dateCompositeLData);
					dateComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						dateLabel = new CLabel(dateComposite, SWT.NONE);
						dateLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						RowData dateLabelLData = new RowData();
						dateLabelLData.width = 120;
						dateLabelLData.height = 22;
						dateLabel.setLayoutData(dateLabelLData);
						dateLabel.setText("Date of first usage :");
						dateLabel.setToolTipText("Kaufdatum, Datum der Fertigstellung ");
					}
					{
						dateText = new Text(dateComposite, SWT.NONE);
						RowData dateTextLData = new RowData();
						dateTextLData.width = 114;
						dateTextLData.height = 21;
						dateText.setLayoutData(dateTextLData);
						dateText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						dateText.setToolTipText("Kaufdatum, Datum der Fertigstellung ");
						dateText.setEditable(true);
						dateText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "dateText.keyReleased, event="+evt);
								isObjectDataSaved = false;
							}
						});
					}
				}
				{
					statusComposite = new Composite(mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout2 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					statusComposite.setLayout(composite1Layout2);
					GridData statusCompositeLData = new GridData();
					statusCompositeLData.grabExcessHorizontalSpace = true;
					statusCompositeLData.verticalAlignment = GridData.BEGINNING;
					statusCompositeLData.horizontalAlignment = GridData.BEGINNING;
					statusCompositeLData.widthHint = 246;
					statusCompositeLData.heightHint = 28;
					statusComposite.setLayoutData(statusCompositeLData);
					statusComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						statusLabel = new CLabel(statusComposite, SWT.NONE);
						statusLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						RowData statusLabelLData = new RowData();
						statusLabelLData.width = 120;
						statusLabelLData.height = 22;
						statusLabel.setLayoutData(statusLabelLData);
						statusLabel.setText("Status information :");
						statusLabel.setToolTipText("selct most fitting usage state of the object, active, in use, damaged, outdated, lost, sold ");
					}
					{
						statusText = new CCombo(statusComposite, SWT.NONE);
						statusText.setItems(new String[] {"unknown", "active", "in use", "damaged", "outdated", "lost", "sold", "under repair" });
						statusText.select(0);
						RowData group1LData = new RowData();
						group1LData.width = 117;
						group1LData.height = 22;
						statusText.setLayoutData(group1LData);
						statusText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						statusText.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "statusText.widgetSelected, event="+evt);
								isObjectDataSaved = false;
							}
						});
					}
				}
				{
					imageCanvas = new Canvas(mainObjectCharacterisitcsGroup, SWT.BORDER);
					GridData imageCanvasLData = new GridData();
					imageCanvasLData.minimumWidth = 400;
					imageCanvasLData.minimumHeight = 300;
					imageCanvasLData.verticalAlignment = GridData.FILL;
					imageCanvasLData.grabExcessHorizontalSpace = true;
					imageCanvasLData.widthHint = 400;
					imageCanvas.setLayoutData(imageCanvasLData);
					imageCanvas.setToolTipText("drag image here, 400x300 is the recommended  size");
					imageCanvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/"+ settings.getLocale() +"/ObjectImage.gif"));
					imageCanvas.setSize(400, 300);
					popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
					contextMenu = new ImageContextMenu();
					contextMenu.createMenu(this.popupmenu);
					imageCanvas.setMenu(popupmenu);
					imageCanvas.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "imageCanvas.paintControl, event="+evt);
							if (popupmenu.getData(ImageContextMenu.OBJECT_IMAGE_CHANGED) != null && (Boolean)popupmenu.getData("OBJECT_IMAGE_CHANGED")) {
								image = SWTResourceManager.getImage(new Image(imageCanvas.getDisplay(), (String)popupmenu.getData(ImageContextMenu.OBJECT_IMAGE_PATH)).getImageData(), object.getKey());
								object.setImage(SWTResourceManager.getImage(image.getImageData(), object.getKey()));
								popupmenu.setData("OBJECT_IMAGE_CHANGED", false);
								isObjectDataSaved = false;
								imageCanvas.redraw(0,0,400,300,true);
							}
							imageCanvas.setSize(400, 300);
							Rectangle imgBounds = image.getBounds();
							evt.gc.drawImage(image, 0, 0, imgBounds.width, imgBounds.height, 0, 0, 400, 300);
						}
					});
				}
			}
			{
				FormData composite1LData = new FormData();
				composite1LData.right =  new FormAttachment(1000, 1000, -15);
				composite1LData.top =  new FormAttachment(0, 1000, 60);
				composite1LData.bottom =  new FormAttachment(1000, 1000, -15);
				composite1LData.width = 549;
				composite1LData.height = 435;
				composite1LData.left =  new FormAttachment(439, 1000, 0);
				editComposite = new Group(tabComposite, SWT.NONE);
				GridLayout editCompositeLayout = new GridLayout();
				editCompositeLayout.makeColumnsEqualWidth = true;
				editComposite.setLayout(editCompositeLayout);
				editComposite.setLayoutData(composite1LData);
				editComposite.setText("Free Editable Characteristics");
				editComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				editComposite.setToolTipText("describe additionla characteristics here ");
				{
					editToolBar = new ToolBar(editComposite, SWT.NONE);
					editToolBar.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						fontSelectItem = new ToolItem(editToolBar, SWT.NONE);
						fontSelectItem.setImage(SWTResourceManager.getImage("osde/resource/Edit.gif"));
						fontSelectItem.setToolTipText("select the font");
					}
					{
						fontSizeItem = new ToolItem(editToolBar, SWT.NONE);
						fontSizeItem.setImage(SWTResourceManager.getImage("osde/resource/Edit.gif"));
						fontSizeItem.setToolTipText("select font size");
					}
					{
						boldItem = new ToolItem(editToolBar, SWT.NONE);
						boldItem.setImage(SWTResourceManager.getImage("osde/resource/ArrowWhiteGreenFieldRight.gif"));
						boldItem.setToolTipText("toggle bold text");
					}
					{
						italicItem = new ToolItem(editToolBar, SWT.NONE);
						italicItem.setImage(SWTResourceManager.getImage("osde/resource/EditWhite.gif"));
						italicItem.setToolTipText("toggle italic font style");
					}
					{
						underlineItem = new ToolItem(editToolBar, SWT.NONE);
						underlineItem.setImage(SWTResourceManager.getImage("osde/resource/LineType1.gif"));
						underlineItem.setToolTipText("underline selected text");
					}
					{
						strikeThroughItem = new ToolItem(editToolBar, SWT.NONE);
						strikeThroughItem.setImage(SWTResourceManager.getImage("osde/resource/LineWidth2.gif"));
						strikeThroughItem.setToolTipText("strike trough selected text");
					}
					{
						leftJustifyItem = new ToolItem(editToolBar, SWT.NONE);
						leftJustifyItem.setImage(SWTResourceManager.getImage("osde/resource/Measure.gif"));
					}
					{
						colorItem = new ToolItem(editToolBar, SWT.NONE);
						colorItem.setImage(SWTResourceManager.getImage("osde/resource/OpenAdd.gif"));
					}
					GridData editToolBarLData = new GridData();
					editToolBarLData.horizontalAlignment = GridData.FILL;
					editToolBarLData.verticalAlignment = GridData.FILL;
					editToolBarLData.grabExcessHorizontalSpace = true;
					editToolBar.setLayoutData(editToolBarLData);
					editToolBar.setBounds(0, 0, 490, 30);
				}
				{
					GridData composite1LData1 = new GridData();
					composite1LData1.verticalAlignment = GridData.BEGINNING;
					composite1LData1.horizontalAlignment = GridData.BEGINNING;
					composite1LData1.widthHint = 539;
					composite1LData1.heightHint = 390;
					composite1LData1.grabExcessHorizontalSpace = true;
					composite1LData1.grabExcessVerticalSpace = true;
					composite1 = new Composite(editComposite, SWT.NONE);
					composite1.setLayout(null);
					composite1.setLayoutData(composite1LData1);
					composite1.setBackground(SWTResourceManager.getColor(255, 255, 255));
					{
						styledText = new StyledText(composite1, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
						styledText.setLocation(0, 30);
						styledText.setText(" Hersteller\t\t:\n Händler\t\t\t:\n Preis\t\t\t\t:\n Bauzeit\t\t\t:\n .....\t\t\t\t\t:\n");
						styledText.setEditable(true);
						styledText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif",10,1,false,false));
						styledText.setHorizontalIndex(2);
						styledText.setTopIndex(1);
						styledText.setBounds(7, 7, 532, 380);
						styledText.addExtendedModifyListener(new ExtendedModifyListener() {
							public void modifyText(ExtendedModifyEvent evt) {
								System.out.println("styledText.modifyText, event="+evt);
								//TODO add your code for styledText.modifyText
							}
						});
						styledText.addMenuDetectListener(new MenuDetectListener() {
							public void menuDetected(MenuDetectEvent evt) {
								System.out.println("styledText.menuDetected, event="+evt);
								//TODO add your code for styledText.menuDetected
							}
						});
						styledText.addSelectionListener(new SelectionAdapter() {
							public void widgetSelected(SelectionEvent evt) {
								System.out.println("styledText.widgetSelected, event="+evt);
								//TODO add your code for styledText.widgetSelected
							}
						});
						styledText.addVerifyKeyListener(new VerifyKeyListener() {
							public void verifyKey(VerifyEvent evt) {
								System.out.println("styledText.verifyKey, event="+evt);
								//TODO add your code for styledText.verifyKey
							}
						});
						styledText.addModifyListener(new ModifyListener() {
							public void modifyText(ModifyEvent evt) {
								System.out.println("styledText.modifyText, event="+evt);
								//TODO add your code for styledText.modifyText
							}
						});
						styledText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "styledText.keyReleased, event="+evt);
								isObjectDataSaved = false;
							}
							public void keyPressed(KeyEvent evt) {
								System.out.println("styledText.keyPressed, event="+evt);
								//TODO add your code for styledText.keyPressed
							}
						});
					}
				}
			}
		}
		this.update();
	}

	/**
	 * dummy main to enable form edit
	 * @param args
	 */
	public static void main(String[] args) {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		Composite inst = new Composite(shell, SWT.NULL);

		FillLayout instLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
		inst.setLayout(instLayout);
			{
				cTabFolder1 = new CTabFolder(inst, SWT.NONE);
				
				ObjectDescriptionWindow objectDescriptionWindow = new ObjectDescriptionWindow(cTabFolder1);
				objectDescriptionWindow.create();
				
				cTabFolder1.setSelection(0);
			}
			inst.layout();

		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0) {
			inst.pack();
			shell.pack();
		}
		else {
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) display.sleep();
		}
	}
	
	/**
	 * @return the imageCanvas to enable redraw
	 */
	public void redrawImageCanvas() {
		this.imageCanvas.redraw();
	}
}