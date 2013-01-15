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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.RecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;

import java.util.HashMap;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * Class to enable a file comment
 * @author Winfried Brügmann
 */
public class FileCommentWindow extends CTabItem {

	final static Logger						log										= Logger.getLogger(FileCommentWindow.class.getName());

	Composite											commentMainComposite;
	CLabel												infoLabel;
	Text													fileCommentText;
	boolean												isFileCommentChanged	= false;
	Table													recordCommentTable;
	TableColumn										recordCommentTableHeader;
	TableColumn										recordCommentTableHeader2;
	Color													innerAreaBackground;
	Color													surroundingBackground;
	
	int fileCommentCaretPosition = 0;
	boolean isFocusGained = false;

	final DataExplorer	application;
	final Channels								channels;
	final CTabFolder							displayTab;
	final Menu										popupmenu;
	final TabAreaContextMenu			contextMenu;

	/**
	 * constructor with CTabFolder parent
	 * @param currentDisplayTab
	 */
	public FileCommentWindow(CTabFolder currentDisplayTab, int style) {
		super(currentDisplayTab, style);
		this.displayTab = currentDisplayTab;
		this.application = DataExplorer.getInstance();
		this.channels = Channels.getInstance();
		SWTResourceManager.registerResourceUser(this);
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE+1, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0239));

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.innerAreaBackground = Settings.getInstance().getFileCommentInnerAreaBackground();
		this.surroundingBackground = Settings.getInstance().getFileCommentSurroundingAreaBackground();
	}

	/**
	 * method to create the window and register required event listener
	 */
	public void create() {
		this.commentMainComposite = new Composite(this.displayTab, SWT.NONE);
		this.setControl(this.commentMainComposite);
		this.commentMainComposite.setLayout(null);
		this.commentMainComposite.setBackground(this.surroundingBackground);
		this.commentMainComposite.setMenu(this.popupmenu);
		this.commentMainComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "commentMainComposite.paintControl, event=" + evt); //$NON-NLS-1$
				setFileComment();
				updateRecordSetTable();
			}
		});
		this.commentMainComposite.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "commentMainComposite.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_92.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.commentMainComposite.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "commentMainComposite.focusLost() , event=" + evt); //$NON-NLS-1$
				setFileComment();
			}
			public void focusGained(FocusEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "commentMainComposite.focusGained() , event=" + evt); //$NON-NLS-1$
			}
		});
		{
			this.infoLabel = new CLabel(this.commentMainComposite, SWT.LEFT);
			this.infoLabel.setText(Messages.getString(MessageIds.GDE_MSGT0240));
			this.infoLabel.setFont(SWTResourceManager.getFont(this.application, 12, SWT.BOLD));
			this.infoLabel.setBackground(this.surroundingBackground);
			this.infoLabel.setMenu(this.popupmenu);
			this.infoLabel.setBounds(50, 10, 500, 26);
			this.infoLabel.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "infoLabel.paintControl " + evt); //$NON-NLS-1$
					FileCommentWindow.this.contextMenu.createMenu(FileCommentWindow.this.popupmenu, TabAreaContextMenu.TYPE_SIMPLE);
				}
			});
		}
		{
			this.fileCommentText = new Text(this.commentMainComposite, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
			this.fileCommentText.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.fileCommentText.setBackground(this.innerAreaBackground);
			this.fileCommentText.setText(Messages.getString(MessageIds.GDE_MSGT0241));
			this.fileCommentText.setBounds(50, 40, 500, 100);
			this.fileCommentText.setText(this.channels.getActiveChannel() != null ? this.channels.getActiveChannel().getFileDescription() : GDE.STRING_EMPTY);
			this.fileCommentCaretPosition = this.fileCommentText.getText().length();
			this.fileCommentText.setMenu(this.popupmenu);
			this.fileCommentText.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_92.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.fileCommentText.addVerifyListener(new VerifyListener() {		
				public void verifyText(VerifyEvent e) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.verifyText , event=" + e); //$NON-NLS-1$
					if (isFocusGained) {
						e.doit = false;
						isFocusGained = false;
					}
					else
						e.doit = true;
					//System.out.println("verify " + FileCommentWindow.this.fileCommentText.getCaretPosition());					
				}
			});
			this.fileCommentText.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.keyPressed , event=" + e); //$NON-NLS-1$

					if (e.character == '\0' || e.keyCode == SWT.ESC) {
						//ignore when only Shift, Ctrl, etc or ESC was pressed
					} 
					else if((e.stateMask & SWT.MOD1) != 0 && ((char) e.keyCode) == 'a' ) {
						// select all text on Ctrl+a
						fileCommentText.selectAll();
					}
					else {
						FileCommentWindow.this.isFileCommentChanged = true;
					}
				}
			});
			this.fileCommentText.addFocusListener(new FocusListener() {
				public void focusLost(FocusEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.focusLost() , event=" + evt); //$NON-NLS-1$
					setFileComment();
					FileCommentWindow.this.fileCommentCaretPosition = FileCommentWindow.this.fileCommentCaretPosition == 0 && FileCommentWindow.this.fileCommentText.getCaretPosition() == 0
							? FileCommentWindow.this.fileCommentText.getText().length() : FileCommentWindow.this.fileCommentText.getCaretPosition();
					//System.out.println("lost " + FileCommentWindow.this.fileCommentText.getCaretPosition() + " " + FileCommentWindow.this.fileCommentCaretPosition);
				}
				public void focusGained(FocusEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "fileCommentText.focusGained() , event=" + evt); //$NON-NLS-1$
					FileCommentWindow.this.fileCommentText.setSelection(FileCommentWindow.this.fileCommentCaretPosition);
					//System.out.println("gained " + FileCommentWindow.this.fileCommentText.getCaretPosition());
					isFocusGained = true;
				}
			});
		}
		{
			this.recordCommentTable = new Table(this.commentMainComposite, SWT.BORDER | SWT.V_SCROLL);
			this.recordCommentTable.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.recordCommentTable.setBounds(50, 200, 500, 100);
			//this.table.setControl(this.dataTable);
			this.recordCommentTable.setLinesVisible(true);
			this.recordCommentTable.setHeaderVisible(true);
			this.recordCommentTable.setBackground(this.innerAreaBackground);
			this.recordCommentTable.setMenu(this.popupmenu);
			this.recordCommentTable.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordCommentTable.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_92.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});

			this.recordCommentTableHeader = new TableColumn(this.recordCommentTable, SWT.LEFT);
			this.recordCommentTableHeader.setWidth(250);
			this.recordCommentTableHeader.setText(Messages.getString(MessageIds.GDE_MSGT0242));

			this.recordCommentTableHeader2 = new TableColumn(this.recordCommentTable, SWT.LEFT);
			this.recordCommentTableHeader2.setWidth(500);
			this.recordCommentTableHeader2.setText(Messages.getString(MessageIds.GDE_MSGT0243));
		}
	}

	public synchronized void update() {
		if (this.channels.getActiveChannel() != null) {
			this.fileCommentText.setText(this.channels.getActiveChannel().getFileDescription());
		}
		updateRecordSetTable();
	}

	/**
	 * update the record set entry table
	 */
	synchronized void updateRecordSetTable() {
		Point mainSize = FileCommentWindow.this.commentMainComposite.getSize();
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "mainSize = " + mainSize.toString());
		Rectangle bounds = new Rectangle(mainSize.x * 5 / 100, mainSize.y * 10 / 100, mainSize.x * 90 / 100, mainSize.y * 40 / 100);
		//if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "cover bounds = " + bounds.toString());
		FileCommentWindow.this.infoLabel.setBounds(50, 10, bounds.width, bounds.y - 10);
		FileCommentWindow.this.fileCommentText.setBounds(bounds);
		if (this.channels.getActiveChannel() != null) {
			this.fileCommentText.setText(this.channels.getActiveChannel().getFileDescription());
		}

		bounds = new Rectangle(mainSize.x * 5 / 100, mainSize.y * 50 / 100, mainSize.x * 90 / 100, mainSize.y * 40 / 100);
		FileCommentWindow.this.recordCommentTable.setBounds(bounds);
		FileCommentWindow.this.recordCommentTable.removeAll();
		FileCommentWindow.this.recordCommentTableHeader2.setWidth(bounds.width - 205);
		Channel channel = Channels.getInstance().getActiveChannel();
		TableItem item;
		if (channel != null) {
			synchronized (channel) {
				HashMap<String, RecordSet> recordSets = channel.getRecordSets();
				for (String recordSetKey : channel.getRecordSetNames()) {
					if (recordSetKey != null) {
						item = new TableItem(FileCommentWindow.this.recordCommentTable, SWT.LEFT);
						RecordSet tmpRecordSet = recordSets.get(recordSetKey);
						if (tmpRecordSet != null) item.setText(new String[] { recordSetKey, tmpRecordSet.getRecordSetDescription() });
					}
				}
			}
		}
	}

	/**
	 * @return the isFileCommentChanged
	 */
	public boolean isFileCommentChanged() {
		return this.isFileCommentChanged;
	}

	public void setFileComment() {
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null && this.isFileCommentChanged) {
			activeChannel.setFileDescription(FileCommentWindow.this.fileCommentText.getText());
			activeChannel.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
			this.isFileCommentChanged = false;
		}
	}

	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.commentMainComposite.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.commentMainComposite.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}

	/**
	 * @param newInnerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color newInnerAreaBackground) {
		this.innerAreaBackground = newInnerAreaBackground;
		this.fileCommentText.setBackground(newInnerAreaBackground);
		this.recordCommentTable.setBackground(newInnerAreaBackground);
		this.fileCommentText.redraw();
		this.recordCommentTable.redraw();
	}

	/**
	 * @param newSurroundingBackground the surroundingAreaBackground to set
	 */
	public void setSurroundingAreaBackground(Color newSurroundingBackground) {
		this.commentMainComposite.setBackground(newSurroundingBackground);
		this.infoLabel.setBackground(newSurroundingBackground);
		this.surroundingBackground = newSurroundingBackground;
		this.commentMainComposite.redraw();
	}
}