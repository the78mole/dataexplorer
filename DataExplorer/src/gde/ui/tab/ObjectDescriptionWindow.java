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
import osde.data.Channel;
import osde.data.Channels;
import osde.data.ObjectData;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.ObjectImageContextMenu;

/**
 * @author Winfried Brügmann
 * This class defines the layout for the object description, this window(tab) will only shown, if not device oriented
 */
public class ObjectDescriptionWindow {
	final static Logger						log									= Logger.getLogger(ObjectDescriptionWindow.class.getName());

	CTabItem											objectTabItem;
	Composite											tabComposite;

	Group													editGroup;
	CoolBar												editCoolBar;
	ToolBar												fontSelectToolBar;
	ToolBar												editToolBar;
	int														toolButtonHeight		= 23;
	CoolItem											editCoolItem;
	ToolItem											fontSelect;
	Composite											fontSizeSelectComposite;
	CCombo												fontSizeSelectCombo;
	Point													fontSizeSelectSize	= new Point((OSDE.IS_WINDOWS ? 40 : 60), (OSDE.IS_WINDOWS ? 21 : 25));
	ToolItem											strikeoutButton;
	ToolItem											underlineButton;
	ToolItem											italicButton;
	ToolItem											boldButton;
	ToolItem											fColorButton, bColorButton;
	ToolItem											cutButton, copyButton, pasteButton, printButton;
	Composite											styledTextComposite;
	StyledText										styledText;

	Group													mainObjectCharacterisitcsGroup;
	Composite											headerComposite;	
	CLabel												objectNameLabel;
	CLabel												objectName;
	Composite											typeComposite;
	CLabel												objectTypeLable;
	Text													objectTypeText;
	Composite											dateComposite;
	CLabel												dateLabel;
	Text													dateText;
	Composite											statusComposite;
	CCombo												statusText;
	CLabel												statusLabel;

	Canvas												imageCanvas;
	Menu													popupmenu;
	ObjectImageContextMenu				contextMenu;

	final CTabFolder							tabFolder;
	//static CTabFolder cTabFolder1;
	final OpenSerialDataExplorer	application;
	final Settings								settings;
	final Channels								channels;
	String												objectFilePath;
	String												activeObjectKey;
	boolean												isObjectDataSaved		= true;
	Vector<StyleRange>						cachedStyles				= new Vector<StyleRange>();
	ObjectData										object;
	Image													image;

	public ObjectDescriptionWindow(OpenSerialDataExplorer currenApplication, CTabFolder objectDescriptionTab) {
		this.application = currenApplication;
		this.tabFolder = objectDescriptionTab;
		this.settings = Settings.getInstance();
		this.channels = Channels.getInstance();
		this.activeObjectKey = this.settings.getActiveObject();
	}

	public ObjectDescriptionWindow(CTabFolder objectDescriptionTab) {
		this.application = null;
		this.tabFolder = objectDescriptionTab;
		this.settings = null;
		this.channels = null;
		this.tabFolder.setSize(1020, 554);
	}
	
	public boolean isVisible() {
		return this.objectTabItem != null && !this.objectTabItem.isDisposed() && this.objectTabItem.getControl().isVisible();
	}

	public void setVisible(boolean isVisible) {
		if (isVisible) {
			if (this.objectTabItem == null || this.objectTabItem.isDisposed()) {
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

			checkSaveObjectData();

			this.activeObjectKey = this.application.getMenuToolBar().getActiveObjectKey();
			this.objectFilePath = this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_SEPARATOR_UNIX + this.activeObjectKey + OSDE.FILE_ENDING_DOT_ZIP;
			this.object = new ObjectData(this.objectFilePath);

			// check if object data can be load from file
			if (new File(this.objectFilePath).exists()) {
				this.object.load();
				if (this.object.getImage() != null) 
					this.image = SWTResourceManager.getImage(this.object.getImage().getImageData(), this.object.getKey(), this.object.getImageWidth(), this.object.getImageHeight(), true);
			}
			else {
				this.image = null;
			}
			this.imageCanvas.redraw();

			this.objectName.setText(this.object.getKey().equals(this.settings.getActiveObject()) ? this.object.getKey() : this.settings.getActiveObject());
			this.objectTypeText.setText(this.object.getType());
			this.dateText.setText(this.object.getActivationDate());
			this.statusText.setText(this.object.getStatus());

			FontData fd = this.object.getFontData();
			this.styledText.setFont(SWTResourceManager.getFont(this.application, fd.getHeight(), fd.getStyle()));
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
	 * method to check isf the object data are changed and needs to be saved
	 */
	public void checkSaveObjectData() {
		if (this.object != null && !this.isObjectDataSaved) {
			this.object.setType(this.objectTypeText.getText());
			this.object.setActivationDate(this.dateText.getText());
			this.object.setStatus(this.statusText.getText());
			if (this.image != null)
				this.object.setImage(SWTResourceManager.getImage(this.image.getImageData(), this.activeObjectKey, this.object.getImageWidth(), this.object.getImageHeight(), false));
			else
				this.object.setImage(null);
			this.object.setStyledText(this.styledText.getText());
			this.object.setFont(this.styledText.getFont());
			this.object.setStyleRanges(this.styledText.getStyleRanges().clone());
			this.object.save();
			this.isObjectDataSaved = true;
			Channel activeChannel = this.channels.getActiveChannel();
			if (activeChannel != null) {
				activeChannel.setUnsaved(Channel.UNSAVED_REASON_CHANGED_OBJECT_DATA);
			}
				
		}
	}

	/**
	 * creates the window content
	 */
	public void create() {
		this.objectTabItem = new CTabItem(this.tabFolder, SWT.NONE);
		this.objectTabItem.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.objectTabItem.setText(Messages.getString(MessageIds.OSDE_MSGT0403));
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
				composite2LData.width = 600;
				composite2LData.height = 36;
				composite2LData.left = new FormAttachment(0, 1000, 15);
				composite2LData.top = new FormAttachment(0, 1000, 17);
				this.headerComposite.setLayoutData(composite2LData);
				this.headerComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				{
					this.objectNameLabel = new CLabel(this.headerComposite, SWT.NONE);
					this.objectNameLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0404));
					this.objectNameLabel.setFont(SWTResourceManager.getFont(this.application, 12, SWT.NORMAL));
					RowData cLabel1LData = new RowData();
					cLabel1LData.width = 130;
					cLabel1LData.height = 26;
					this.objectNameLabel.setLayoutData(cLabel1LData);
					this.objectNameLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				}
				{
					this.objectName = new CLabel(this.headerComposite, SWT.NONE);
					this.objectName.setFont(SWTResourceManager.getFont(this.application, 12, SWT.BOLD));
					RowData cLabel1LData = new RowData();
					cLabel1LData.width = 300;
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
				group2LData.width = 410;
				group2LData.height = 425;
				group2LData.left = new FormAttachment(0, 1000, 15);
				group2LData.top = new FormAttachment(0, 1000, 60);
				this.mainObjectCharacterisitcsGroup.setLayoutData(group2LData);
				this.mainObjectCharacterisitcsGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0416));
				this.mainObjectCharacterisitcsGroup.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				{
					this.typeComposite = new Composite(this.mainObjectCharacterisitcsGroup, SWT.NONE);
					RowLayout composite1Layout3 = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
					this.typeComposite.setLayout(composite1Layout3);
					GridData typeCompositeLData = new GridData();
					typeCompositeLData.verticalAlignment = GridData.BEGINNING;
					typeCompositeLData.grabExcessHorizontalSpace = true;
					typeCompositeLData.horizontalAlignment = GridData.BEGINNING;
					typeCompositeLData.heightHint = OSDE.IS_WINDOWS ? 28 : 32;
					this.typeComposite.setLayoutData(typeCompositeLData);
					this.typeComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.objectTypeLable = new CLabel(this.typeComposite, SWT.NONE);
						this.objectTypeLable.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						this.objectTypeLable.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
						this.objectTypeLable.setText(Messages.getString(MessageIds.OSDE_MSGT0425));
						RowData cLabel1LData1 = new RowData();
						cLabel1LData1.width = 140;
						cLabel1LData1.height = 22;
						this.objectTypeLable.setLayoutData(cLabel1LData1);
						this.objectTypeLable.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0405));
					}
					{
						this.objectTypeText = new Text(this.typeComposite, SWT.BORDER);
						this.objectTypeText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						this.objectTypeText.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
						this.objectTypeText.setEditable(true);
						RowData cLabel2LData = new RowData();
						cLabel2LData.width = 240;
						cLabel2LData.height = 18;
						this.objectTypeText.setLayoutData(cLabel2LData);
						this.objectTypeText.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0405));
						this.objectTypeText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "objectTypeText.keyReleased, event=" + evt); //$NON-NLS-1$
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
					dateCompositeLData.horizontalAlignment = GridData.BEGINNING;
					dateCompositeLData.heightHint = OSDE.IS_WINDOWS ? 28 : 32;
					this.dateComposite.setLayoutData(dateCompositeLData);
					this.dateComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.dateLabel = new CLabel(this.dateComposite, SWT.NONE);
						this.dateLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						this.dateLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
						RowData dateLabelLData = new RowData();
						dateLabelLData.width = 140;
						dateLabelLData.height = 22;
						this.dateLabel.setLayoutData(dateLabelLData);
						this.dateLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0406));
						this.dateLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0407));
					}
					{
						this.dateText = new Text(this.dateComposite, SWT.BORDER);
						this.dateText.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
						RowData dateTextLData = new RowData();
						dateTextLData.width = OSDE.IS_WINDOWS ? 118: 116;
						dateTextLData.height = 18;
						this.dateText.setLayoutData(dateTextLData);
						this.dateText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						this.dateText.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0407));
						this.dateText.setEditable(true);
						this.dateText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "dateText.keyReleased, event=" + evt); //$NON-NLS-1$
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
					statusCompositeLData.heightHint = OSDE.IS_WINDOWS ? 28 : 32;
					this.statusComposite.setLayoutData(statusCompositeLData);
					this.statusComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.statusLabel = new CLabel(this.statusComposite, SWT.NONE);
						this.statusLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
						this.statusLabel.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
						RowData statusLabelLData = new RowData();
						statusLabelLData.width = 140;
						statusLabelLData.height = 22;
						this.statusLabel.setLayoutData(statusLabelLData);
						this.statusLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0410));
						this.statusLabel.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0411));
					}
					{
						this.statusText = new CCombo(this.statusComposite, SWT.BORDER);
						this.statusText.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL)); 
						this.statusText.setItems(Messages.getString(MessageIds.OSDE_MSGT0412).split(OSDE.STRING_SEMICOLON));
						this.statusText.select(0);
						RowData group1LData = new RowData();
						group1LData.width = 120;
						group1LData.height = OSDE.IS_WINDOWS ? 21 : 24;
						this.statusText.setLayoutData(group1LData);
						this.statusText.setBackground(SWTResourceManager.getColor(255, 255, 255));
						this.statusText.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0411));
						this.statusText.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent evt) {
								log.log(Level.FINEST, "statusText.widgetSelected, event=" + evt); //$NON-NLS-1$
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
					imageCanvasLData.verticalAlignment = GridData.END;
					imageCanvasLData.grabExcessHorizontalSpace = true;
					imageCanvasLData.widthHint = 400;
					this.imageCanvas.setLayoutData(imageCanvasLData);
					this.imageCanvas.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0413));
					this.imageCanvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/" + this.settings.getLocale() + "/ObjectImage.gif")); //$NON-NLS-1$ //$NON-NLS-2$
					this.imageCanvas.setSize(400, 300);
					this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
					this.contextMenu = new ObjectImageContextMenu();
					this.contextMenu.createMenu(this.popupmenu);
					this.imageCanvas.setMenu(this.popupmenu);
					this.imageCanvas.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.log(Level.FINEST, "imageCanvas.paintControl, event=" + evt); //$NON-NLS-1$
							if (ObjectDescriptionWindow.this.popupmenu.getData(ObjectImageContextMenu.OBJECT_IMAGE_CHANGED) != null && (Boolean) ObjectDescriptionWindow.this.popupmenu.getData("OBJECT_IMAGE_CHANGED")) {
								String imagePath = (String) ObjectDescriptionWindow.this.popupmenu.getData(ObjectImageContextMenu.OBJECT_IMAGE_PATH);
								if (imagePath != null) {
								ObjectDescriptionWindow.this.image = SWTResourceManager.getImage(new Image(ObjectDescriptionWindow.this.imageCanvas.getDisplay(), 
										imagePath).getImageData(), 
										ObjectDescriptionWindow.this.object.getKey(), 
										ObjectDescriptionWindow.this.object.getImageWidth(),
										ObjectDescriptionWindow.this.object.getImageHeight(), true);
								}
								else {
									ObjectDescriptionWindow.this.image = null;
								}
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
				this.editGroup = new Group(this.tabComposite, SWT.NONE);
				FormData composite1LData = new FormData();
				composite1LData.width = 540;
				composite1LData.right = new FormAttachment(1000, 1000, -15);
				composite1LData.top = new FormAttachment(0, 1000, 60);
				composite1LData.bottom = new FormAttachment(1000, 1000, -15);
				composite1LData.left = new FormAttachment(0, 1000, 440);
				this.editGroup.setLayoutData(composite1LData);				
				this.editGroup.setLayout(new GridLayout());
				this.editGroup.setText(Messages.getString(MessageIds.OSDE_MSGT0414));
				this.editGroup.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				{
					this.editCoolBar = new CoolBar(this.editGroup, SWT.FLAT);
					GridData editCoolBarLData = new GridData();
					editCoolBarLData.grabExcessHorizontalSpace = true;
					editCoolBarLData.horizontalAlignment = GridData.FILL;
					editCoolBarLData.verticalAlignment = GridData.BEGINNING;
					editCoolBarLData.minimumWidth = 505;
					editCoolBarLData.heightHint = 40;		
					editCoolBarLData.minimumHeight = 40;				
					this.editCoolBar.setLayoutData(editCoolBarLData);		
					this.editCoolBar.setLayout(new RowLayout(SWT.HORIZONTAL));
					this.editCoolBar.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					{
						this.editCoolItem = new CoolItem(this.editCoolBar, SWT.FLAT);
						{
							this.fontSelectToolBar = new ToolBar(this.editCoolBar, SWT.FLAT);
							this.editCoolItem.setControl(this.fontSelectToolBar);
							this.fontSelectToolBar.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

							new ToolItem(this.fontSelectToolBar, SWT.SEPARATOR);
							{
								this.fontSelect = new ToolItem(this.fontSelectToolBar, SWT.BORDER);
								this.fontSelect.setImage(SWTResourceManager.getImage("osde/resource/Font.gif")); //$NON-NLS-1$
								this.fontSelect.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0417));
								this.fontSelect.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "fontSelect.widgetSelected, event=" + evt); //$NON-NLS-1$
										setFont();
									}
								});
							}
							new ToolItem(this.fontSelectToolBar, SWT.SEPARATOR);
							{
								ToolItem fontSizeSelectComboSep = new ToolItem(this.fontSelectToolBar, SWT.SEPARATOR);
								{
									this.fontSizeSelectComposite = new Composite(this.fontSelectToolBar, SWT.FLAT);
									this.fontSizeSelectComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
									this.fontSizeSelectCombo = new CCombo(this.fontSizeSelectComposite, SWT.BORDER | SWT.LEFT | SWT.READ_ONLY);
									this.fontSizeSelectCombo.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.NORMAL));
									this.fontSizeSelectCombo.setItems(new String[] { "6", "7", "8", "9", "10", "12", "14", "16", "18" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$
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
							Point size = this.fontSelectToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							this.fontSelectToolBar.setSize(size);

							this.editToolBar = new ToolBar(this.editCoolBar, SWT.FLAT);
							this.editCoolItem.setControl(this.editToolBar);
							this.editToolBar.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.boldButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.boldButton.setImage(SWTResourceManager.getImage("osde/resource/Bold.gif")); //$NON-NLS-1$
								this.boldButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0419));
								this.boldButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "boldButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										setStyle(ObjectDescriptionWindow.this.boldButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.italicButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.italicButton.setImage(SWTResourceManager.getImage("osde/resource/Italic.gif")); //$NON-NLS-1$
								this.italicButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0420));
								this.italicButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "italicButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										setStyle(ObjectDescriptionWindow.this.italicButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.underlineButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.underlineButton.setImage(SWTResourceManager.getImage("osde/resource/Underline.gif")); //$NON-NLS-1$
								this.underlineButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0421));
								this.underlineButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "underlineButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										setStyle(ObjectDescriptionWindow.this.underlineButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.strikeoutButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.strikeoutButton.setImage(SWTResourceManager.getImage("osde/resource/Strikeout.gif")); //$NON-NLS-1$
								this.strikeoutButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0422));
								this.strikeoutButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "strikeoutButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										setStyle(ObjectDescriptionWindow.this.strikeoutButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.fColorButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.fColorButton.setImage(SWTResourceManager.getImage("osde/resource/fColor.gif")); //$NON-NLS-1$
								this.fColorButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0423));
								this.fColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "colorItem.widgetSelected, event=" + evt); //$NON-NLS-1$
										RGB rgb = new ColorDialog(ObjectDescriptionWindow.this.editToolBar.getShell()).open();
										ObjectDescriptionWindow.this.fColorButton.setData(rgb);
										setStyle(ObjectDescriptionWindow.this.fColorButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.bColorButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.bColorButton.setImage(SWTResourceManager.getImage("osde/resource/bColor.gif")); //$NON-NLS-1$
								this.bColorButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0424));
								this.bColorButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "colorItem.widgetSelected, event=" + evt); //$NON-NLS-1$
										RGB rgb = new ColorDialog(ObjectDescriptionWindow.this.editToolBar.getShell()).open();
										ObjectDescriptionWindow.this.bColorButton.setData(rgb);
										setStyle(ObjectDescriptionWindow.this.bColorButton);
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.copyButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.copyButton.setImage(SWTResourceManager.getImage("osde/resource/Copy.gif")); //$NON-NLS-1$
								this.copyButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0426));
								this.copyButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "copyButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										handleCutCopy();
										ObjectDescriptionWindow.this.styledText.copy();
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.cutButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.cutButton.setImage(SWTResourceManager.getImage("osde/resource/Cut.gif")); //$NON-NLS-1$
								this.cutButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0427));
								this.cutButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "cutButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										handleCutCopy();
										ObjectDescriptionWindow.this.styledText.cut();
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.pasteButton = new ToolItem(this.editToolBar, SWT.PUSH);
								this.pasteButton.setImage(SWTResourceManager.getImage("osde/resource/Paste.gif")); //$NON-NLS-1$
								this.pasteButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0428));
								this.pasteButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "pasteButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										ObjectDescriptionWindow.this.styledText.paste();
										//Clipboard clipboard = new Clipboard(tabComposite.getDisplay());
										//String data = (String) clipboard.getContents(RTFTransfer.getInstance());
										//log.log(Level.FINEST, data);
										//FileInputStream stream = new FileInputStream("sample.rtf");
										//RTFEditorKit kit = new RTFEditorKit();
										//Document doc = kit.createDefaultDocument();
										//kit.read(stream, doc, 0);
										//String plainText = doc.getText(0, doc.getLength());
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);
							{
								this.printButton = new ToolItem(this.editToolBar, SWT.PUSH | SWT.BORDER);
								this.printButton.setImage(SWTResourceManager.getImage("osde/resource/Print.gif")); //$NON-NLS-1$
								this.printButton.setToolTipText(Messages.getString(MessageIds.OSDE_MSGT0429));
								this.printButton.addSelectionListener(new SelectionAdapter() {
									@Override
									public void widgetSelected(SelectionEvent evt) {
										log.log(Level.FINEST, "printButton.widgetSelected, event=" + evt); //$NON-NLS-1$
										ObjectDescriptionWindow.this.object.print();
									}
								});
							}
							new ToolItem(this.editToolBar, SWT.SEPARATOR);

							size = this.editToolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT);
							this.editToolBar.setSize(size);
						}
//						System.out.println(this.editToolBar.getSize() + " " + this.fontSelectToolBar.getSize());
//						System.out.println(this.editCoolItem.getSize());
//						this.editCoolItem.setMinimumSize(500, this.fontSelectToolBar.getSize().y);
					}
				}
				{
					this.styledTextComposite = new Composite(this.editGroup, SWT.BORDER);
					FormLayout styledTextCompositeLayout = new FormLayout();
					this.styledTextComposite.setLayout(styledTextCompositeLayout);
					this.styledTextComposite.setBackground(SWTResourceManager.getColor(255, 255, 255));
					GridData styledTextGData = new GridData();
					styledTextGData.grabExcessHorizontalSpace = true;
					styledTextGData.horizontalAlignment = GridData.FILL;
					styledTextGData.grabExcessVerticalSpace = true;
					styledTextGData.verticalAlignment = GridData.FILL;
					//styledTextGData.widthHint = 200;
					//styledTextGData.heightHint = 200;					
					this.styledTextComposite.setLayoutData(styledTextGData);
					{
						this.styledText = new StyledText(this.styledTextComposite, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
						FormLayout styledTextLayout = new FormLayout();
						this.styledText.setLayout(styledTextLayout);
						this.styledText.setEditable(true);
						this.styledText.setFont(SWTResourceManager.getFont(this.application, this.application.getWidgetFontSize(), SWT.BOLD));
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
								log.log(Level.FINEST, "styledText.modifyText, event=" + evt); //$NON-NLS-1$
								if (evt.length == 0) return;
								StyleRange style;
								if (evt.length == 1 || ObjectDescriptionWindow.this.styledText.getTextRange(evt.start, evt.length).equals(ObjectDescriptionWindow.this.styledText.getLineDelimiter())) {
									// Have the new text take on the style of the text to its right
									// (during typing) if no style information is active.
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
									try {
										// paste occurring, have text take on the styles it had when it was cut/copied
										for (int i = 0; i < ObjectDescriptionWindow.this.cachedStyles.size(); i++) {
											style = ObjectDescriptionWindow.this.cachedStyles.elementAt(i);
											StyleRange newStyle = (StyleRange) style.clone();
											newStyle.start = style.start + evt.start;
											ObjectDescriptionWindow.this.styledText.setStyleRange(newStyle);
										}
									}
									catch (Exception e) {
										// switch object occurs while there is cached style
									}
								}
							}
						});
						this.styledText.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent evt) {
								log.log(Level.FINEST, "styledText.keyReleased, event=" + evt); //$NON-NLS-1$
								ObjectDescriptionWindow.this.isObjectDataSaved = false;
							}

							@Override
							public void keyPressed(KeyEvent evt) {
								log.log(Level.FINEST, "styledText.keyPressed, event=" + evt); //$NON-NLS-1$
								if ((evt.stateMask & SWT.CTRL) != 0) {
									if (evt.keyCode == 'x') { //cut
										log.log(Level.FINE, "SWT.CTRL + 'x'"); //$NON-NLS-1$
										handleCutCopy();
									}
									else if (evt.keyCode == 'c') { //copy
										log.log(Level.FINE, "SWT.CTRL + 'c'"); //$NON-NLS-1$
										handleCutCopy();
									}
									else if (evt.keyCode == 'v') { //paste
										log.log(Level.FINE, "SWT.CTRL + 'v'"); //$NON-NLS-1$
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
