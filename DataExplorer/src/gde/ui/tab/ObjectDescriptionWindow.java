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

	final static Logger						log									= Logger.getLogger(ObjectDescriptionWindow.class.getName());

	CTabItem							objectTabItem;
	StyledText						styledText;
	Canvas								imageCanvas;
	CLabel								objectName;
	CLabel								objectNameLabel;

	CoolBar								editCoolBar;
	ToolBar								fontSelectToolBar;
	ToolBar								editToolBar;
	//	static final int							leadFill	= 4+(OSDE.IS_WINDOWS == true ? 0 : 3);
	//	static final int							trailFill	= 4+(OSDE.IS_WINDOWS == true ? 0 : 3);
	int														toolButtonHeight		= 23;
	CoolItem							editCoolItem;

	ToolItem							fontSelect;
	Composite											fontSizeSelectComposite;
	CCombo												fontSizeSelectCombo;
	Point													fontSizeSelectSize	= new Point(40, 21);
	ToolItem							strikeoutButton;
	ToolItem							underlineButton;
	ToolItem							italicButton;
	ToolItem							boldButton;
	Vector<StyleRange>						cachedStyles				= new Vector<StyleRange>();

	Composite							tabComposite;

	final OpenSerialDataExplorer	application;
	String												objectFilePath;
	String												activeObjectKey;
	boolean												isObjectDataSaved		= true;
	Composite							styledTextComposite;

	Composite							statusComposite;
	Text									objectTypeText;
	CLabel								objectTypeLable;
	Composite							typeComposite;
	Group									mainObjectCharacterisitcsGroup;
	CLabel								dateLabel;
	Composite							dateComposite;
	Group									editGroup;
	ToolItem							fColorButton, bColorButton;
	ToolItem							cutButton, copyButton, pasteButton;
	Text									dateText;
	CCombo								statusText;
	CLabel								statusLabel;

	Menu													popupmenu;
	ImageContextMenu							contextMenu;
	ObjectData										object;
	Image													image;

	final Settings								settings;
	final CTabFolder							tabFolder;
	//static CTabFolder cTabFolder1;
	private Composite							headerComposite;

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
			if (this.objectTabItem.isDisposed()) {
				create();
			}
		}
		else {
			this.objectTabItem.getControl().dispose();
			this.objectTabItem.dispose();
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
				if (fontSize.equals(OSDE.STRING_EMPTY + fd.getHeight())) {
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
		this.objectTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		this.objectTabItem.setText("Object Characteristics");
		{
			this.tabComposite = new Composite(this.tabFolder, SWT.NONE);
			this.objectTabItem.setControl(this.tabComposite);
			FormLayout composite1Layout = new FormLayout();
			this.tabComposite.setLayout(composite1Layout);
			this.tabComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			{
				this.headerComposite = new Composite(this.tabComposite, SWT.NONE);
				RowLayout composite2Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
				this.headerComposite.setLayout(composite2Layout);
				FormData composite2LData = new FormData();
				composite2LData.width = 419;
				composite2LData.height = 36;
				composite2LData.left = new FormAttachment(0, 1000, 15);
				composite2LData.top = new FormAttachment(0, 1000, 17);
				this.headerComposite.setLayoutData(composite2LData);
				this.headerComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				{
					this.objectNameLabel = new CLabel(this.headerComposite, SWT.NONE);
					this.objectNameLabel.setText("Object Name :");
					this.objectNameLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 0, false, false));
					this.objectNameLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				}
				{
					this.objectName = new CLabel(this.headerComposite, SWT.NONE);
					this.objectName.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
					RowData cLabel1LData = new RowData();
					cLabel1LData.width = 299;
					cLabel1LData.height = 26;
					this.objectName.setLayoutData(cLabel1LData);
					this.objectName.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				}
			}
			{
				this.mainObjectCharacterisitcsGroup = new Group(this.tabComposite, SWT.NONE);
				GridLayout group2Layout = new GridLayout();
				group2Layout.makeColumnsEqualWidth = true;
				this.mainObjectCharacterisitcsGroup.setLayout(group2Layout);
				FormData group2LData = new FormData();
				group2LData.width = 414;
				group2LData.height = 411;
				group2LData.left = new FormAttachment(0, 1000, 15);
				group2LData.top = new FormAttachment(0, 1000, 60);
				this.mainObjectCharacterisitcsGroup.setLayoutData(group2LData);
				this.mainObjectCharacterisitcsGroup.setText("Main Characteristics ");
				this.mainObjectCharacterisitcsGroup.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.mainObjectCharacterisitcsGroup.setToolTipText("Describes the main characteristics of the object");
				{
					this.typeComposite = new Composite(this.mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout3 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.typeComposite.setLayout(composite1Layout3);
					GridData typeCompositeLData = new GridData();
					typeCompositeLData.verticalAlignment = GridData.BEGINNING;
					typeCompositeLData.grabExcessHorizontalSpace = true;
					typeCompositeLData.horizontalAlignment = GridData.BEGINNING;
					typeCompositeLData.heightHint = 28;
					this.typeComposite.setLayoutData(typeCompositeLData);
					this.typeComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.objectTypeLable = new CLabel(this.typeComposite, SWT.NONE);
						this.objectTypeLable.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						this.objectTypeLable.setText("Object Type :");
						RowData cLabel1LData1 = new RowData();
						cLabel1LData1.width = 120;
						cLabel1LData1.height = 22;
						this.objectTypeLable.setLayoutData(cLabel1LData1);
						this.objectTypeLable.setToolTipText("give a type name, sample:battery, Soaring model, motor flyer, trainer, solar collector, ....");
					}
					{
						this.objectTypeText = new Text(this.typeComposite, SWT.NONE);
						this.objectTypeText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						this.objectTypeText.setEditable(true);
						RowData cLabel2LData = new RowData();
						cLabel2LData.width = 267;
						cLabel2LData.height = 23;
						this.objectTypeText.setLayoutData(cLabel2LData);
						this.objectTypeText.setToolTipText("give a type name, sample: NiMh battery, soaring model, motor flyer, trainer, solar collector, ....");
						this.objectTypeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "objectTypeText.keyReleased, event=" + evt);
								ObjectDescriptionWindow.this.isObjectDataSaved = false;
							}
						});
					}
				}
				{
					this.dateComposite = new Composite(this.mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout1 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.dateComposite.setLayout(composite1Layout1);
					GridData dateCompositeLData = new GridData();
					dateCompositeLData.grabExcessHorizontalSpace = true;
					dateCompositeLData.verticalAlignment = GridData.BEGINNING;
					dateCompositeLData.widthHint = 246;
					dateCompositeLData.horizontalAlignment = GridData.BEGINNING;
					dateCompositeLData.heightHint = 28;
					this.dateComposite.setLayoutData(dateCompositeLData);
					this.dateComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.dateLabel = new CLabel(this.dateComposite, SWT.NONE);
						this.dateLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						RowData dateLabelLData = new RowData();
						dateLabelLData.width = 120;
						dateLabelLData.height = 22;
						this.dateLabel.setLayoutData(dateLabelLData);
						this.dateLabel.setText("Date of first usage :");
						this.dateLabel.setToolTipText("Kaufdatum, Datum der Fertigstellung ");
					}
					{
						this.dateText = new Text(this.dateComposite, SWT.NONE);
						RowData dateTextLData = new RowData();
						dateTextLData.width = 114;
						dateTextLData.height = 21;
						this.dateText.setLayoutData(dateTextLData);
						this.dateText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						this.dateText.setToolTipText("Kaufdatum, Datum der Fertigstellung ");
						this.dateText.setEditable(true);
						this.dateText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "dateText.keyReleased, event=" + evt);
								ObjectDescriptionWindow.this.isObjectDataSaved = false;
							}
						});
					}
				}
				{
					this.statusComposite = new Composite(this.mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout2 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.statusComposite.setLayout(composite1Layout2);
					GridData statusCompositeLData = new GridData();
					statusCompositeLData.grabExcessHorizontalSpace = true;
					statusCompositeLData.verticalAlignment = GridData.BEGINNING;
					statusCompositeLData.horizontalAlignment = GridData.BEGINNING;
					statusCompositeLData.widthHint = 246;
					statusCompositeLData.heightHint = 28;
					this.statusComposite.setLayoutData(statusCompositeLData);
					this.statusComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.statusLabel = new CLabel(this.statusComposite, SWT.NONE);
						this.statusLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						RowData statusLabelLData = new RowData();
						statusLabelLData.width = 120;
						statusLabelLData.height = 22;
						this.statusLabel.setLayoutData(statusLabelLData);
						this.statusLabel.setText("Status information :");
						this.statusLabel.setToolTipText("selct most fitting usage state of the object, active, in use, damaged, outdated, lost, sold ");
					}
					{
						this.statusText = new CCombo(this.statusComposite, SWT.NONE);
						this.statusText.setItems(new String[] { "unknown", "active", "in use", "damaged", "outdated", "lost", "sold", "under repair" });
						this.statusText.select(0);
						RowData group1LData = new RowData();
						group1LData.width = 117;
						group1LData.height = 22;
						this.statusText.setLayoutData(group1LData);
						this.statusText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						this.statusText.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "statusText.widgetSelected, event=" + evt);
								ObjectDescriptionWindow.this.isObjectDataSaved = false;
							}
						});
					}
				}
				{
					this.imageCanvas = new Canvas(this.mainObjectCharacterisitcsGroup, SWT.BORDER);
					GridData imageCanvasLData = new GridData();
					imageCanvasLData.minimumWidth = 400;
					imageCanvasLData.minimumHeight = 300;
					imageCanvasLData.verticalAlignment = GridData.FILL;
					imageCanvasLData.grabExcessHorizontalSpace = true;
					imageCanvasLData.widthHint = 400;
					this.imageCanvas.setLayoutData(imageCanvasLData);
					this.imageCanvas.setToolTipText("drag image here, 400x300 is the recommended  size");
					this.imageCanvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/" + this.settings.getLocale() + "/ObjectImage.gif"));
					this.imageCanvas.setSize(400, 300);
					this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
					this.contextMenu = new ImageContextMenu();
					this.contextMenu.createMenu(this.popupmenu);
					this.imageCanvas.setMenu(this.popupmenu);
					this.imageCanvas.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "imageCanvas.paintControl, event=" + evt);
							if (ObjectDescriptionWindow.this.popupmenu.getData(ImageContextMenu.OBJECT_IMAGE_CHANGED) != null && (Boolean) ObjectDescriptionWindow.this.popupmenu.getData("OBJECT_IMAGE_CHANGED")) {
								ObjectDescriptionWindow.this.image = SWTResourceManager.getImage(new Image(ObjectDescriptionWindow.this.imageCanvas.getDisplay(), (String) ObjectDescriptionWindow.this.popupmenu
										.getData(ImageContextMenu.OBJECT_IMAGE_PATH)).getImageData(), ObjectDescriptionWindow.this.object.getKey(), ObjectDescriptionWindow.this.object.getImageWidth(),
										ObjectDescriptionWindow.this.object.getImageHeight(), true);
								ObjectDescriptionWindow.this.object.setImage(ObjectDescriptionWindow.this.image);
								ObjectDescriptionWindow.this.popupmenu.setData("OBJECT_IMAGE_CHANGED", false);
								ObjectDescriptionWindow.this.isObjectDataSaved = false;
								//imageCanvas.redraw(0,0,400,300,true);
							}
							ObjectDescriptionWindow.this.imageCanvas.setSize(400, 300);
							if (ObjectDescriptionWindow.this.image != null) {
								Rectangle imgBounds = ObjectDescriptionWindow.this.image.getBounds();
								evt.gc.drawImage(ObjectDescriptionWindow.this.image, 0, 0, imgBounds.width, imgBounds.height, 0, 0, 400, 300);
							}
						}
					});
				}
			}
			{
				FormData composite1LData = new FormData();
				composite1LData.right = new FormAttachment(1000, 1000, -15);
				composite1LData.top = new FormAttachment(0, 1000, 60);
				composite1LData.bottom = new FormAttachment(1000, 1000, -15);
				composite1LData.left = new FormAttachment(440, 1000, 0);
				this.editGroup = new Group(this.tabComposite, SWT.NONE);
				FormLayout editGroupLayout = new FormLayout();
				this.editGroup.setLayout(editGroupLayout);
				this.editGroup.setLayoutData(composite1LData);
				this.editGroup.setText("Additional Characteristics");
				this.editGroup.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.editGroup.setToolTipText("describe additionla characteristics here ");
				{
					FormData composite1LData1 = new FormData();
					composite1LData1.height = 40;
					composite1LData1.left = new FormAttachment(0, 1000, 5);
					composite1LData1.right = new FormAttachment(1000, 1000, -5);
					composite1LData1.top = new FormAttachment(0, 1000, 5);
					composite1LData1.width = 500;
					this.editCoolBar = new CoolBar(this.editGroup, SWT.FLAT);
					this.editCoolBar.setLayoutData(composite1LData1);
					this.editCoolBar.setLayout(new RowLayout(SWT.HORIZONTAL));
					this.editCoolBar.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.editCoolItem = new CoolItem(this.editCoolBar, SWT.FLAT);
						{
							this.fontSelectToolBar = new ToolBar(this.editCoolBar, SWT.FLAT);
							this.editCoolItem.setControl(this.fontSelectToolBar);

							new ToolItem(this.fontSelectToolBar, SWT.SEPARATOR);
							{
								this.fontSelect = new ToolItem(this.fontSelectToolBar, SWT.BORDER);
								this.fontSelect.setImage(SWTResourceManager.getImage("osde/resource/Font.gif"));
								this.fontSelect.setToolTipText("Select the font to used or set");
								this.fontSelect.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "fontSelect.widgetSelected, event=" + evt); //$NON-NLS-1$
										setFont();
									}
								});
							}
							Point size = this.fontSelectToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							this.fontSelectToolBar.setSize(size);

							this.editToolBar = new ToolBar(this.editCoolBar, SWT.FLAT);
							this.editCoolItem.setControl(this.editToolBar);

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
										@Override
										public void widgetSelected(SelectionEvent evt) {
											log.log(Level.FINEST, "fontSizeSelectCombo.widgetSelected, event=" + evt); //$NON-NLS-1$
											setFontSize(Float.parseFloat(ObjectDescriptionWindow.this.fontSizeSelectCombo.getText()));
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
								this.boldButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.boldButton.setImage(SWTResourceManager.getImage("osde/resource/Bold.gif"));
								this.boldButton.setToolTipText("toggle bold text");
								this.boldButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "boldButton.widgetSelected, event=" + evt);
										setStyle(ObjectDescriptionWindow.this.boldButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.italicButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.italicButton.setImage(SWTResourceManager.getImage("osde/resource/Italic.gif"));
								this.italicButton.setToolTipText("toggle italic font style");
								this.italicButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "italicButton.widgetSelected, event=" + evt);
										setStyle(ObjectDescriptionWindow.this.italicButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.underlineButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.underlineButton.setImage(SWTResourceManager.getImage("osde/resource/Underline.gif"));
								this.underlineButton.setToolTipText("underline selected text");
								this.underlineButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "underlineButton.widgetSelected, event=" + evt);
										setStyle(ObjectDescriptionWindow.this.underlineButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.strikeoutButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.strikeoutButton.setImage(SWTResourceManager.getImage("osde/resource/Strikeout.gif"));
								this.strikeoutButton.setToolTipText("strike trough selected text");
								this.strikeoutButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "strikeoutButton.widgetSelected, event=" + evt);
										setStyle(ObjectDescriptionWindow.this.strikeoutButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.fColorButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.fColorButton.setImage(SWTResourceManager.getImage("osde/resource/fColor.gif"));
								this.fColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "colorItem.widgetSelected, event=" + evt);
										RGB rgb = new ColorDialog(ObjectDescriptionWindow.this.editToolBar.getShell()).open();
										ObjectDescriptionWindow.this.fColorButton.setData(rgb);
										setStyle(ObjectDescriptionWindow.this.fColorButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.bColorButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.bColorButton.setImage(SWTResourceManager.getImage("osde/resource/bColor.gif"));
								this.bColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "colorItem.widgetSelected, event=" + evt);
										RGB rgb = new ColorDialog(ObjectDescriptionWindow.this.editToolBar.getShell()).open();
										ObjectDescriptionWindow.this.bColorButton.setData(rgb);
										setStyle(ObjectDescriptionWindow.this.bColorButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.copyButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.copyButton.setImage(SWTResourceManager.getImage("osde/resource/Copy.gif"));
								this.copyButton.setToolTipText("copy selected text");
								this.copyButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "copyButton.widgetSelected, event=" + evt);
										handleCutCopy();
										ObjectDescriptionWindow.this.styledText.copy();
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.cutButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.cutButton.setImage(SWTResourceManager.getImage("osde/resource/Cut.gif"));
								this.cutButton.setToolTipText("cut selected text");
								this.cutButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "cutButton.widgetSelected, event=" + evt);
										handleCutCopy();
										ObjectDescriptionWindow.this.styledText.cut();
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.pasteButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.pasteButton.setImage(SWTResourceManager.getImage("osde/resource/Paste.gif"));
								this.pasteButton.setToolTipText("paste selected text");
								this.pasteButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "pasteButton.widgetSelected, event=" + evt);
										ObjectDescriptionWindow.this.styledText.paste();
										//								    Clipboard clipboard = new Clipboard(tabComposite.getDisplay());
										//						        String data = (String) clipboard.getContents(RTFTransfer.getInstance());
										//						        System.out.println(data);
										//						        FileInputStream stream = new FileInputStream("sample.rtf");
										//						        RTFEditorKit kit = new RTFEditorKit();
										//						        Document doc = kit.createDefaultDocument();
										//						        kit.read(stream, doc, 0);
										//					        	String plainText = doc.getText(0, doc.getLength());
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);

							size = this.editToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							this.editToolBar.setSize(size);
						}
						Point size = this.editCoolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
						this.editToolBar.setSize(size);
					}
				}
				{
					this.styledTextComposite = new Composite(this.editGroup, SWT.BORDER);
					FormLayout styledTextCompositeLayout = new FormLayout();
					this.styledTextComposite.setLayout(styledTextCompositeLayout);
					this.styledTextComposite.setBackground(SWTResourceManager.getColor(255, 255, 255));
					FormData styledTextCompositeLData = new FormData();
					styledTextCompositeLData.width = 540;
					styledTextCompositeLData.height = 383;
					styledTextCompositeLData.top = new FormAttachment(0, 1000, 49);
					styledTextCompositeLData.left = new FormAttachment(0, 1000, 5);
					styledTextCompositeLData.bottom = new FormAttachment(1000, 1000, -3);
					styledTextCompositeLData.right = new FormAttachment(1000, 1000, -4);
					this.styledTextComposite.setLayoutData(styledTextCompositeLData);
					{
						this.styledText = new StyledText(this.styledTextComposite, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
						FormLayout styledTextLayout = new FormLayout();
						this.styledText.setLayout(styledTextLayout);
						this.styledText.setEditable(true);
						this.styledText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
						this.styledText.setHorizontalIndex(2);
						this.styledText.setTopIndex(1);
						FormData styledTextLData = new FormData();
						styledTextLData.width = 400;
						styledTextLData.height = 300;
						styledTextLData.top = new FormAttachment(0, 1000, 5);
						styledTextLData.left = new FormAttachment(0, 1000, 8);
						styledTextLData.bottom = new FormAttachment(1000, 1000, 0);
						styledTextLData.right = new FormAttachment(1000, 1000, 0);
						this.styledText.setLayoutData(styledTextLData);
						this.styledText.addExtendedModifyListener(new ExtendedModifyListener() {
							public void modifyText(ExtendedModifyEvent evt) {
								log.log(Level.INFO, "styledText.modifyText, event=" + evt);
								if (evt.length == 0) return;
								StyleRange style;
								if (evt.length == 1 || ObjectDescriptionWindow.this.styledText.getTextRange(evt.start, evt.length).equals(ObjectDescriptionWindow.this.styledText.getLineDelimiter())) {
									// Have the new text take on the style of the text to its right
									// (during
									// typing) if no style information is active.
									int caretOffset = ObjectDescriptionWindow.this.styledText.getCaretOffset();
									style = null;
									if (caretOffset < ObjectDescriptionWindow.this.styledText.getCharCount()) style = ObjectDescriptionWindow.this.styledText.getStyleRangeAtOffset(caretOffset);
									if (style != null) {
										style = (StyleRange) style.clone();
										style.start = evt.start;
										style.length = evt.length;
									}
									else {
										style = new StyleRange(evt.start, evt.length, null, null, SWT.NORMAL);
									}
									if (ObjectDescriptionWindow.this.boldButton.getSelection()) style.fontStyle |= SWT.BOLD;
									if (ObjectDescriptionWindow.this.italicButton.getSelection()) style.fontStyle |= SWT.ITALIC;
									style.underline = ObjectDescriptionWindow.this.underlineButton.getSelection();
									style.strikeout = ObjectDescriptionWindow.this.strikeoutButton.getSelection();
									if (!style.isUnstyled()) ObjectDescriptionWindow.this.styledText.setStyleRange(style);
								}
								else {
									// paste occurring, have text take on the styles it had when it was
									// cut/copied
									for (int i = 0; i < ObjectDescriptionWindow.this.cachedStyles.size(); i++) {
										style = ObjectDescriptionWindow.this.cachedStyles.elementAt(i);
										StyleRange newStyle = (StyleRange) style.clone();
										newStyle.start = style.start + evt.start;
										ObjectDescriptionWindow.this.styledText.setStyleRange(newStyle);
									}
								}
							}
						});
						this.styledText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(Level.INFO, "styledText.keyReleased, event=" + evt);
								ObjectDescriptionWindow.this.isObjectDataSaved = false;
							}

							@Override
							public void keyPressed(KeyEvent evt) {
								log.log(Level.FINEST, "styledText.keyPressed, event=" + evt);
								if ((evt.stateMask & SWT.CTRL) != 0) {
									if (evt.keyCode == 'x') { //cut
										System.out.println("SWT.CTRL + 'x'");
										handleCutCopy();
										ObjectDescriptionWindow.this.styledText.cut();
									}
									else if (evt.keyCode == 'c') { //copy
										System.out.println("SWT.CTRL + 'c'");
										handleCutCopy();
										ObjectDescriptionWindow.this.styledText.copy();
									}
									else if (evt.keyCode == 'v') { //paste
										System.out.println("SWT.CTRL + 'v'");
										ObjectDescriptionWindow.this.styledText.paste();
									}
								}
							}
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
				RGB rgb = (RGB) this.fColorButton.getData();
				style.foreground = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
			}
			else if (widget == this.bColorButton) {
				RGB rgb = (RGB) this.bColorButton.getData();
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
		FontDialog fontDialog = new FontDialog(this.editToolBar.getShell());
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
		this.styledText.setFont(SWTResourceManager.getFont(fontData));
		this.isObjectDataSaved = false;
	}

	/**
	 * Cache the style information for styled text that has been cut or copied
	 * Save the cut/copied style info so that during paste we will maintain the style information
	 */
	void handleCutCopy() {
		this.cachedStyles = new Vector<StyleRange>();
		Point sel = this.styledText.getSelectionRange();
		int startX = sel.x;
		for (int i = sel.x; i <= sel.x + sel.y - 1; i++) {
			StyleRange style = this.styledText.getStyleRangeAtOffset(i);
			if (style != null) {
				style.start = style.start - startX;
				if (!this.cachedStyles.isEmpty()) {
					StyleRange lastStyle = this.cachedStyles.lastElement();
					if (lastStyle.similarTo(style) && lastStyle.start + lastStyle.length == style.start) {
						lastStyle.length++;
					}
					else {
						this.cachedStyles.addElement(style);
					}
				}
				else {
					this.cachedStyles.addElement(style);
				}
			}
		}
	}
}
