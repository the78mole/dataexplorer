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
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.FontDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

import osde.OSDE;
import osde.config.Settings;
import osde.data.ObjectData;
import osde.messages.MessageIds;
import osde.messages.Messages;
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
	
	private CoolBar								editCoolBar;
	private ToolBar								fontSelectToolBar;
	private ToolBar								editToolBar;
//	static final int							leadFill	= 4+(OSDE.IS_WINDOWS == true ? 0 : 3);
//	static final int							trailFill	= 4+(OSDE.IS_WINDOWS == true ? 0 : 3);
	int														toolButtonHeight = 23;
	private CoolItem editCoolItem;

	private ToolItem fontSelect;
	Composite fontSizeSelectComposite;
	CCombo fontSizeSelectCombo;
	Point fontSizeSelectSize = new Point(40, 21);
	private ToolItem strikeoutButton;
	private ToolItem underlineButton;
	private ToolItem italicButton;
	private ToolItem boldButton;
	Vector<StyleRange>			cachedStyles	= new Vector<StyleRange>();

	
	private Composite							tabComposite;

	final OpenSerialDataExplorer	application;
	String objectFilePath;
	String activeObjectKey;
	boolean	isObjectDataSaved = true;
	private Composite styledTextComposite;

	private Composite statusComposite;
	private Text objectTypeText;
	private CLabel objectTypeLable;
	private Composite typeComposite;
	private Group mainObjectCharacterisitcsGroup;
	private CLabel dateLabel;
	private Composite dateComposite;
	private Group editGroup;
	private ToolItem fColorButton, bColorButton;
	private Text dateText;
	private CCombo statusText;
	private CLabel statusLabel; 
		
	Menu													popupmenu;
	ImageContextMenu contextMenu;
	ObjectData	object;
	Image image;

	final Settings								settings;
	final CTabFolder							tabFolder;
	//static private CTabFolder cTabFolder1;
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
				this.object.setImage(SWTResourceManager.getImage(this.image.getImageData(), this.activeObjectKey, this.object.getImageWidth(), this.object.getImageHeight(), false));
				this.object.setStyledText(this.styledText.getText());
				this.object.setFont(this.styledText.getFont());
				this.object.setStyleRanges(this.styledText.getStyleRanges().clone());
				this.object.save();
				this.isObjectDataSaved = true;
			}

			this.activeObjectKey = this.application.getMenuToolBar().getActiveObjectKey();
			this.objectFilePath = this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_ENDING_DOT_ZIP;
			this.object = new ObjectData(this.objectFilePath);

			// check if object data can be load from file
			if (new File(this.objectFilePath).exists()) {
				this.object.load();
				this.image = SWTResourceManager.getImage(this.object.getImage().getImageData(), this.object.getKey(), this.object.getImageWidth(), this.object.getImageHeight(), true);
				this.imageCanvas.redraw();
			}
			else {
				this.image = null;
			}

			this.objectName.setText(this.object.getKey().equals(this.settings.getActiveObject()) ? this.object.getKey() : this.settings.getActiveObject());
			this.objectTypeText.setText(this.object.getType());
			this.dateText.setText(this.object.getActivationDate());
			this.statusText.setText(this.object.getStatus());

			FontData fd = this.object.getFontData();
			this.styledText.setFont(SWTResourceManager.getFont(fd.getName(), fd.getHeight(), fd.getStyle(), false, false));
			this.styledText.setText(this.object.getStyledText());
			this.styledText.setStyleRanges(this.object.getStyleRanges().clone());
			int index = 0;
			for (String fontSize : this.fontSizeSelectCombo.getItems()) {
				if (fontSize.equals(OSDE.STRING_EMPTY+fd.getHeight())) {
					this.fontSizeSelectCombo.select(index);
					break;
				}
				++index;
			}
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
				group2LData.height = 411;
				group2LData.left =  new FormAttachment(0, 1000, 15);
				group2LData.top =  new FormAttachment(0, 1000, 60);
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
								image = SWTResourceManager.getImage(new Image(imageCanvas.getDisplay(), (String)popupmenu.getData(ImageContextMenu.OBJECT_IMAGE_PATH)).getImageData(), object.getKey(), object.getImageWidth(), object.getImageHeight(), true);
								object.setImage(image);
								popupmenu.setData("OBJECT_IMAGE_CHANGED", false);
								isObjectDataSaved = false;
								//imageCanvas.redraw(0,0,400,300,true);
							}
							imageCanvas.setSize(400, 300);
							if (image != null) {
								Rectangle imgBounds = image.getBounds();
								evt.gc.drawImage(image, 0, 0, imgBounds.width, imgBounds.height, 0, 0, 400, 300);
							}
						}
					});
				}
			}
			{
				FormData composite1LData = new FormData();
				composite1LData.right =  new FormAttachment(1000, 1000, -15);
				composite1LData.top =  new FormAttachment(0, 1000, 60);
				composite1LData.bottom =  new FormAttachment(1000, 1000, -15);
				composite1LData.left =  new FormAttachment(440, 1000, 0);
				editGroup = new Group(tabComposite, SWT.NONE);
				FormLayout editGroupLayout = new FormLayout();
				editGroup.setLayout(editGroupLayout);
				editGroup.setLayoutData(composite1LData);
				editGroup.setText("Additional Characteristics");
				editGroup.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				editGroup.setToolTipText("describe additionla characteristics here ");
				{
					FormData composite1LData1 = new FormData();
					composite1LData1.height = 40;
					composite1LData1.left = new FormAttachment(0, 1000, 5);
					composite1LData1.right = new FormAttachment(1000, 1000, -5);
					composite1LData1.top = new FormAttachment(0, 1000, 5);
					composite1LData1.width = 500;
					editCoolBar = new CoolBar(editGroup, SWT.FLAT);
					editCoolBar.setLayoutData(composite1LData1);
					editCoolBar.setLayout(new RowLayout(SWT.HORIZONTAL));
					editCoolBar.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						editCoolItem = new CoolItem(editCoolBar, SWT.FLAT);
						{
							fontSelectToolBar = new ToolBar(editCoolBar, SWT.FLAT);
							editCoolItem.setControl(fontSelectToolBar);
							
							new ToolItem(this.fontSelectToolBar, SWT.SEPARATOR);
							{
								fontSelect = new ToolItem(this.fontSelectToolBar, SWT.BORDER);
								fontSelect.setImage(SWTResourceManager.getImage("osde/resource/Font.gif"));
								fontSelect.setToolTipText("Select the font to used or set");
								fontSelect.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										setFont();
									}
								});
							}
							Point size = fontSelectToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							fontSelectToolBar.setSize(size);

							editToolBar = new ToolBar(editCoolBar, SWT.FLAT);
							editCoolItem.setControl(editToolBar);
							
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								ToolItem fontSizeSelectComboSep = new ToolItem(this.editToolBar, SWT.SEPARATOR);
								{
									this.fontSizeSelectComposite = new Composite(this.editToolBar, SWT.FLAT);
									this.fontSizeSelectComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
									this.fontSizeSelectCombo = new CCombo(this.fontSizeSelectComposite, SWT.BORDER | SWT.LEFT | SWT.READ_ONLY);
									this.fontSizeSelectCombo.setItems(new String[] { "6", "7", "8", "9", "10", "12", "14", "16", "18" });
									this.fontSizeSelectCombo.select(3);
									this.fontSizeSelectCombo.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0201));
									this.fontSizeSelectCombo.setEditable(false);
									this.fontSizeSelectCombo.setBackground(OpenSerialDataExplorer.COLOR_WHITE);
									this.fontSizeSelectCombo.setVisibleItemCount(5);
									this.fontSizeSelectCombo.addSelectionListener(new SelectionAdapter() {
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "fontSizeSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											setFontSize(Float.parseFloat(fontSizeSelectCombo.getText()));
										}
									});
									this.toolButtonHeight = this.fontSelect.getBounds().height;
									this.fontSizeSelectCombo.setSize(this.fontSizeSelectSize);
									this.fontSizeSelectComposite.setSize(this.fontSizeSelectSize.x, this.toolButtonHeight);
									this.fontSizeSelectCombo.setLocation(0, (this.toolButtonHeight - this.fontSizeSelectSize.y) / 2);
								}
								fontSizeSelectComboSep.setWidth(this.fontSizeSelectComposite.getSize().x);
								fontSizeSelectComboSep.setControl(this.fontSizeSelectComposite);
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								boldButton = new ToolItem(editToolBar, SWT.PUSH);
								boldButton.setImage(SWTResourceManager.getImage("osde/resource/Bold.gif"));
								boldButton.setToolTipText("toggle bold text");
								boldButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "boldButton.widgetSelected, event=" + evt);
										setStyle(boldButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								italicButton = new ToolItem(editToolBar, SWT.PUSH);
								italicButton.setImage(SWTResourceManager.getImage("osde/resource/Italic.gif"));
								italicButton.setToolTipText("toggle italic font style");
								italicButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "italicButton.widgetSelected, event=" + evt);
										setStyle(italicButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								underlineButton = new ToolItem(editToolBar, SWT.PUSH);
								underlineButton.setImage(SWTResourceManager.getImage("osde/resource/Underline.gif"));
								underlineButton.setToolTipText("underline selected text");
								underlineButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "underlineButton.widgetSelected, event=" + evt);
										setStyle(underlineButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								strikeoutButton = new ToolItem(editToolBar, SWT.PUSH);
								strikeoutButton.setImage(SWTResourceManager.getImage("osde/resource/Strikeout.gif"));
								strikeoutButton.setToolTipText("strike trough selected text");
								strikeoutButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "strikeoutButton.widgetSelected, event=" + evt);
										setStyle(strikeoutButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								fColorButton = new ToolItem(editToolBar, SWT.PUSH);
								fColorButton.setImage(SWTResourceManager.getImage("osde/resource/fColor.gif"));
								fColorButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "colorItem.widgetSelected, event=" + evt);
										RGB rgb = new ColorDialog(editToolBar.getShell()).open();
										fColorButton.setData(rgb);
										setStyle(fColorButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								bColorButton = new ToolItem(editToolBar, SWT.PUSH);
								bColorButton.setImage(SWTResourceManager.getImage("osde/resource/bColor.gif"));
								bColorButton.addSelectionListener(new SelectionAdapter() {
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "colorItem.widgetSelected, event=" + evt);
										RGB rgb = new ColorDialog(editToolBar.getShell()).open();
										bColorButton.setData(rgb);
										setStyle(bColorButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);

							size = editToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							editToolBar.setSize(size);
						}
						Point size = editCoolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
						editToolBar.setSize(size);
					}
				}
				{
					styledTextComposite = new Composite(editGroup, SWT.BORDER);
					FormLayout styledTextCompositeLayout = new FormLayout();
					styledTextComposite.setLayout(styledTextCompositeLayout);
					styledTextComposite.setBackground(SWTResourceManager.getColor(255, 255, 255));
					FormData styledTextCompositeLData = new FormData();
					styledTextCompositeLData.width = 540;
					styledTextCompositeLData.height = 383;
					styledTextCompositeLData.top =  new FormAttachment(0, 1000, 49);
					styledTextCompositeLData.left =  new FormAttachment(0, 1000, 5);
					styledTextCompositeLData.bottom =  new FormAttachment(1000, 1000, -3);
					styledTextCompositeLData.right =  new FormAttachment(1000, 1000, -4);
					styledTextComposite.setLayoutData(styledTextCompositeLData);
					{
						styledText = new StyledText(styledTextComposite, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
						FormLayout styledTextLayout = new FormLayout();
						styledText.setLayout(styledTextLayout);
						styledText.setText(" Hersteller\t\t:\n Händler\t\t\t:\n Preis\t\t\t\t:\n Bauzeit\t\t\t:\n .....\t\t\t\t\t:\n");
						styledText.setEditable(true);
						styledText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif",10,1,false,false));
						styledText.setHorizontalIndex(2);
						styledText.setTopIndex(1);
						FormData styledTextLData = new FormData();
						styledTextLData.width = 400;
						styledTextLData.height = 300;
						styledTextLData.top =  new FormAttachment(0, 1000, 5);
						styledTextLData.left =  new FormAttachment(0, 1000, 8);
						styledTextLData.bottom =  new FormAttachment(1000, 1000, 0);
						styledTextLData.right =  new FormAttachment(1000, 1000, 0);
						styledText.setLayoutData(styledTextLData);
						styledText.addExtendedModifyListener(new ExtendedModifyListener() {
							public void modifyText(ExtendedModifyEvent evt) {
								log.log(Level.INFO, "styledText.modifyText, event="+evt);
								if (evt.length == 0) return;
								StyleRange style;
								if (evt.length == 1 || styledText.getTextRange(evt.start, evt.length).equals(styledText.getLineDelimiter())) {
									// Have the new text take on the style of the text to its right
									// (during
									// typing) if no style information is active.
									int caretOffset = styledText.getCaretOffset();
									style = null;
									if (caretOffset < styledText.getCharCount()) style = styledText.getStyleRangeAtOffset(caretOffset);
									if (style != null) {
										style = (StyleRange) style.clone();
										style.start = evt.start;
										style.length = evt.length;
									}
									else {
										style = new StyleRange(evt.start, evt.length, null, null, SWT.NORMAL);
									}
									if (boldButton.getSelection()) style.fontStyle |= SWT.BOLD;
									if (italicButton.getSelection()) style.fontStyle |= SWT.ITALIC;
									style.underline = underlineButton.getSelection();
									style.strikeout = strikeoutButton.getSelection();
									if (!style.isUnstyled()) styledText.setStyleRange(style);
								}
								else {
									// paste occurring, have text take on the styles it had when it was
									// cut/copied
									for (int i = 0; i < cachedStyles.size(); i++) {
										style = (StyleRange) cachedStyles.elementAt(i);
										StyleRange newStyle = (StyleRange) style.clone();
										newStyle.start = style.start + evt.start;
										styledText.setStyleRange(newStyle);
									}
								}
							}
						});
//						styledText.addMenuDetectListener(new MenuDetectListener() {
//							public void menuDetected(MenuDetectEvent evt) {
//								log.log(Level.FINEST, "styledText.menuDetected, event="+evt);
//								//TODO add your code for styledText.menuDetected
//							}
//						});
//						styledText.addSelectionListener(new SelectionAdapter() {
//							public void widgetSelected(SelectionEvent evt) {
//								log.log(Level.FINEST, "styledText.widgetSelected, event="+evt);
//								//TODO add your code for styledText.widgetSelected
//							}
//						});
//						styledText.addVerifyKeyListener(new VerifyKeyListener() {
//							public void verifyKey(VerifyEvent evt) {
//								log.log(Level.FINEST, "styledText.verifyKey, event="+evt);
//								//TODO add your code for styledText.verifyKey
//							}
//						});
//						styledText.addModifyListener(new ModifyListener() {
//							public void modifyText(ModifyEvent evt) {
//								log.log(Level.FINEST, "styledText.modifyText, event="+evt);
//								//TODO add your code for styledText.modifyText
//							}
//						});
						styledText.addKeyListener(new KeyAdapter() {
							public void keyReleased(KeyEvent evt) {
								log.log(Level.INFO, "styledText.keyReleased, event="+evt);
								isObjectDataSaved = false;
							}
//							public void keyPressed(KeyEvent evt) {
//								log.log(Level.FINEST, "styledText.keyPressed, event="+evt);
//								//TODO add your code for styledText.keyPressed
//							}
						});
					}
				}
			}
		}
		this.update();
	}

//	/**
//	 * dummy main to enable form edit
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		Display display = Display.getDefault();
//		Shell shell = new Shell(display);
//		Composite inst = new Composite(shell, SWT.NULL);
//
//		FillLayout instLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
//		inst.setLayout(instLayout);
//			{
//				cTabFolder1 = new CTabFolder(inst, SWT.NONE);
//				
//				ObjectDescriptionWindow objectDescriptionWindow = new ObjectDescriptionWindow(cTabFolder1);
//				objectDescriptionWindow.create();
//				
//				cTabFolder1.setSelection(0);
//			}
//			inst.layout();
//
//		Point size = inst.getSize();
//		shell.setLayout(new FillLayout());
//		shell.layout();
//		if (size.x == 0 && size.y == 0) {
//			inst.pack();
//			shell.pack();
//		}
//		else {
//			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
//			shell.setSize(shellBounds.width, shellBounds.height);
//		}
//		shell.open();
//		while (!shell.isDisposed()) {
//			if (!display.readAndDispatch()) display.sleep();
//		}
//	}
	
	/**
	 * @return the imageCanvas to enable redraw
	 */
	public void redrawImageCanvas() {
		this.imageCanvas.redraw();
	}
	
	/**
	 * Set a style
	 */
	void setStyle(Widget widget) {
		Point sel = this.styledText.getSelectionRange();
		if ((sel == null) || (sel.y == 0)) return;
		StyleRange style;
		for (int i = sel.x; i < sel.x + sel.y; i++) {
			StyleRange range = this.styledText.getStyleRangeAtOffset(i);
			if (range != null) {
				style = (StyleRange) range.clone();
				style.start = i;
				style.length = 1;
			}
			else {
				style = new StyleRange(i, 1, null, null, SWT.NORMAL);
			}
			if (widget == this.boldButton) {
				style.fontStyle ^= SWT.BOLD;
			}
			else if (widget == this.italicButton) {
				style.fontStyle ^= SWT.ITALIC;
			}
			else if (widget == this.underlineButton) {
				style.underline = !style.underline;
			}
			else if (widget == this.strikeoutButton) {
				style.strikeout = !style.strikeout;
			}
			else if (widget == this.fColorButton) {
				RGB rgb = (RGB)this.fColorButton.getData();
				style.foreground = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
			}
			else if (widget == this.bColorButton) {
				RGB rgb = (RGB)this.bColorButton.getData();
				style.background = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
			}
			this.styledText.setStyleRange(style);
		}
		this.styledText.setSelectionRange(sel.x + sel.y, 0);
		this.isObjectDataSaved = false;
	}
	
	/**
	 * Set a font for the wole text
	 */
	void setFont() {
		FontDialog fontDialog = new FontDialog(editToolBar.getShell());
		fontDialog.setFontList((this.styledText.getFont()).getFontData());
		FontData fontData = fontDialog.open();
		if (fontData != null) {
			this.styledText.setFont(SWTResourceManager.getFont(fontData));
		}
		this.isObjectDataSaved = false;
	}
	
	/**
	 * Set a font for the wole text
	 */
	void setFontSize(float height) {
		FontData fontData = this.styledText.getFont().getFontData()[0];
		fontData.height = height;
		if (fontData != null) {
			this.styledText.setFont(SWTResourceManager.getFont(fontData));
		}
		this.isObjectDataSaved = false;
	}
}
