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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
****************************************************************************************/
package gde.ui.menu;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.histo.device.IHistoDevice;
import gde.histo.exclusions.ExclusionActivity;
import gde.io.FileHandler;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.FileUtils;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class TabAreaContextMenu {
	private final static String	$CLASS_NAME	= TabAreaContextMenu.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final Settings			settings		= Settings.getInstance();
	private final HistoSet			histoSet		= HistoSet.getInstance();

	MenuItem										curveSelectionItem;
	MenuItem										displayGraphicsHeaderItem;
	MenuItem										displayGraphicsCommentItem, displayGraphicsCurveSurvey;
	MenuItem										separatorView;
	MenuItem										copyTabItem;
	MenuItem										copyPrintImageItem;
	MenuItem										separatorCopy;
	MenuItem										outherAreaColorItem;
	MenuItem										innerAreaColorItem;
	MenuItem										borderColorItem;
	MenuItem										dateTimeItem;
	MenuItem										partialTableItem;
	MenuItem										editTableItem;
	MenuItem										setDigitalFontItem;
	boolean											isCreated		= false;

	private MenuItem						fileName, openRecordSetItem, deleteFileItem, openFolderItem, hideItem;
	private Menu								hideMenu;
	private MenuItem						hideMenuRecordSetItem, hideMenuFileItem, hideMenuRevokeItem;
	private MenuItem						suppressModeItem;

	public enum TabMenuType {
		GRAPHICS, COMPARE, UTILITY, HISTOGRAPHICS, HISTOTABLE, SIMPLE, TABLE, DIGITAL
	};

	public enum TabMenuOnDemand {
		IS_CURSOR_IN_CANVAS, DATA_FILE_PATH, RECORDSET_BASE_NAME, EXCLUDED_LIST
	};

	public void createMenu(Menu popupMenu, TabMenuType type) {
		popupMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				TabAreaContextMenu.log.log(Level.FINEST, "menuShown action " + e); //$NON-NLS-1$
				if (type == TabMenuType.GRAPHICS) {
					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCurveSurvey.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsCurveSurveyMenuItem.getSelection());
				}
				if (type == TabMenuType.HISTOGRAPHICS) {
					TabAreaContextMenu.this.suppressModeItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().suppressModeItem.getSelection());
					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCurveSurvey.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsCurveSurveyMenuItem.getSelection());

					TabAreaContextMenu.this.fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
					TabAreaContextMenu.this.openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));

					if (popupMenu.getData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString()) == null) { // not within the curve area canvas bounds
						setAllEnabled(false);
						TabAreaContextMenu.this.suppressModeItem.setEnabled(true);
						TabAreaContextMenu.this.curveSelectionItem.setEnabled(true);
						TabAreaContextMenu.this.hideItem.setEnabled(true);
						TabAreaContextMenu.this.hideMenuRevokeItem.setEnabled(true);
						TabAreaContextMenu.this.displayGraphicsHeaderItem.setEnabled(true);
						TabAreaContextMenu.this.displayGraphicsCommentItem.setEnabled(true);
						TabAreaContextMenu.this.displayGraphicsCurveSurvey.setEnabled(true);
						TabAreaContextMenu.this.copyTabItem.setEnabled(true);
						TabAreaContextMenu.this.copyPrintImageItem.setEnabled(true);
					}
					else {
						setAllEnabled(true);
						final String dataFilePath = popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString();
						if (dataFilePath.isEmpty()) {
							TabAreaContextMenu.this.fileName.setEnabled(false);
							TabAreaContextMenu.this.openRecordSetItem.setEnabled(false);
							TabAreaContextMenu.this.deleteFileItem.setEnabled(false);
							TabAreaContextMenu.this.openFolderItem.setEnabled(false);

							TabAreaContextMenu.this.hideMenuRecordSetItem.setEnabled(false);
							TabAreaContextMenu.this.hideMenuFileItem.setEnabled(false);
						}
						else {
							TabAreaContextMenu.this.openFolderItem.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

							final String tmpFileName = Paths.get(dataFilePath).getFileName().toString();
							TabAreaContextMenu.this.fileName.setText(">> " + (tmpFileName.length() > 22 ? "..." + tmpFileName.substring(tmpFileName.length() - 22) : tmpFileName).toString() //$NON-NLS-1$//$NON-NLS-2$
									+ GDE.STRING_BLANK_COLON_BLANK + String.format("%1.22s", popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString()) + " <<"); //$NON-NLS-1$ //$NON-NLS-2$
							TabAreaContextMenu.this.openRecordSetItem.setText(Messages.getString(dataFilePath.toString().endsWith(GDE.FILE_ENDING_DOT_BIN) ? MessageIds.GDE_MSGT0850 : MessageIds.GDE_MSGT0849));
						}
						String excludedList = popupMenu.getData(TabMenuOnDemand.EXCLUDED_LIST.toString()).toString();
						if (!GDE.IS_OS_ARCH_ARM) TabAreaContextMenu.this.hideItem.setToolTipText(excludedList.isEmpty() ? Messages.getString(MessageIds.GDE_MSGT0798) : excludedList);
					}
				}
				if (type == TabMenuType.HISTOTABLE) {
					TabAreaContextMenu.this.suppressModeItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().suppressModeItem.getSelection());
					TabAreaContextMenu.this.curveSelectionItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsHeaderItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCommentItem.setSelection(TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.getSelection());
					TabAreaContextMenu.this.displayGraphicsCurveSurvey.setSelection(TabAreaContextMenu.this.application.getMenuBar().graphicsCurveSurveyMenuItem.getSelection());

					TabAreaContextMenu.this.fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
					TabAreaContextMenu.this.openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));

					if (popupMenu.getData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString()) == null) { // not within the curve area canvas bounds
						setAllEnabled(false);
						TabAreaContextMenu.this.suppressModeItem.setEnabled(true);
						TabAreaContextMenu.this.hideItem.setEnabled(true);
						TabAreaContextMenu.this.hideMenuRevokeItem.setEnabled(true);
						TabAreaContextMenu.this.copyTabItem.setEnabled(true);
					}
					else {
						setAllEnabled(true);
						TabAreaContextMenu.this.curveSelectionItem.setEnabled(false);
						TabAreaContextMenu.this.displayGraphicsHeaderItem.setEnabled(false);
						TabAreaContextMenu.this.displayGraphicsCommentItem.setEnabled(false);
						TabAreaContextMenu.this.displayGraphicsCurveSurvey.setEnabled(false);
						TabAreaContextMenu.this.copyPrintImageItem.setEnabled(false);
						TabAreaContextMenu.this.dateTimeItem.setEnabled(false);
						TabAreaContextMenu.this.editTableItem.setEnabled(false);
						TabAreaContextMenu.this.outherAreaColorItem.setEnabled(false);
						TabAreaContextMenu.this.innerAreaColorItem.setEnabled(false);

						final String dataFilePath = popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString();
						if (dataFilePath.isEmpty()) {
							TabAreaContextMenu.this.fileName.setEnabled(false);
							TabAreaContextMenu.this.openRecordSetItem.setEnabled(false);
							TabAreaContextMenu.this.deleteFileItem.setEnabled(false);
							TabAreaContextMenu.this.openFolderItem.setEnabled(false);

							TabAreaContextMenu.this.hideMenuRecordSetItem.setEnabled(false);
							TabAreaContextMenu.this.hideMenuFileItem.setEnabled(false);
						}
						else {
							TabAreaContextMenu.this.openFolderItem.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

							final String tmpFileName = Paths.get(dataFilePath).getFileName().toString();
							TabAreaContextMenu.this.fileName.setText(">> " + (tmpFileName.length() > 22 ? "..." + tmpFileName.substring(tmpFileName.length() - 22) : tmpFileName).toString() //$NON-NLS-1$//$NON-NLS-2$
									+ GDE.STRING_BLANK_COLON_BLANK + String.format("%1.22s", popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString()) + " <<"); //$NON-NLS-1$ //$NON-NLS-2$
							TabAreaContextMenu.this.openRecordSetItem.setText(Messages.getString(dataFilePath.toString().endsWith(GDE.FILE_ENDING_DOT_BIN) ? MessageIds.GDE_MSGT0850 : MessageIds.GDE_MSGT0849));
						}
						String excludedList = popupMenu.getData(TabMenuOnDemand.EXCLUDED_LIST.toString()).toString();
						if (!GDE.IS_OS_ARCH_ARM) TabAreaContextMenu.this.hideItem.setToolTipText(excludedList.isEmpty() ? Messages.getString(MessageIds.GDE_MSGT0798) : excludedList);
					}
				}
				if (type == TabMenuType.TABLE && TabAreaContextMenu.this.editTableItem != null) {
					TabAreaContextMenu.this.editTableItem.setSelection(Settings.getInstance().isDataTableEditable());
				}
				// clear consumed menu type selector
				popupMenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString(), null);
			}

			@Override
			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		if (!this.isCreated) {
			if (type == TabMenuType.HISTOGRAPHICS || type == TabMenuType.HISTOTABLE) {
				{
					this.fileName = new MenuItem(popupMenu, SWT.None);
					this.fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
					if (!GDE.IS_OS_ARCH_ARM) this.fileName.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0863));
				}
				new MenuItem(popupMenu, SWT.SEPARATOR);
				{
					this.openRecordSetItem = new MenuItem(popupMenu, SWT.PUSH);
					this.openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));
					if (!GDE.IS_OS_ARCH_ARM) this.openRecordSetItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0851));
					this.openRecordSetItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							File file = new File(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString());
							if (FileUtils.checkFileExist(file.getPath())) {
								String validatedImportExtention = TabAreaContextMenu.this.application.getActiveDevice() instanceof IHistoDevice
										? ((IHistoDevice) TabAreaContextMenu.this.application.getActiveDevice()).getSupportedImportExtention() : GDE.STRING_EMPTY;

								if (file.getAbsolutePath().endsWith(GDE.FILE_ENDING_DOT_OSD)) {
									new FileHandler().openOsdFile(file.getAbsolutePath());
								}
								else if (!validatedImportExtention.isEmpty() && file.getAbsolutePath().endsWith(validatedImportExtention)) {
									((IHistoDevice) TabAreaContextMenu.this.application.getActiveDevice()).importDeviceData(file.toPath());
								}
								TabAreaContextMenu.this.application.selectTab(c -> c instanceof GraphicsWindow && ((GraphicsWindow) c).getGraphicsType().equals(GraphicsType.NORMAL));
								TabAreaContextMenu.this.application.updateGraphicsWindow();							}
						}
					});
				}
				{
					this.deleteFileItem = new MenuItem(popupMenu, SWT.PUSH);
					this.deleteFileItem.setText(Messages.getString(MessageIds.GDE_MSGT0861));
					if (!GDE.IS_OS_ARCH_ARM) this.deleteFileItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0862));
					this.deleteFileItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deleteFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							// check if the file exists and if the user really wants to delete it
							File file = new File(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString());
							if (FileUtils.checkFileExist(file.getPath())
									&& TabAreaContextMenu.this.application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0050, new Object[] { file.getAbsolutePath() })) == SWT.YES) {
								FileUtils.deleteFile(file.getPath());

								TabAreaContextMenu.this.application.setupHistoWindows();
							}
						}
					});
				}
				{
					this.openFolderItem = new MenuItem(popupMenu, SWT.PUSH);
					this.openFolderItem.setText(Messages.getString(MessageIds.GDE_MSGT0873));
					if (!GDE.IS_OS_ARCH_ARM) this.openFolderItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0874));
					this.openFolderItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openFolderItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (Desktop.isDesktopSupported()) {
								try {
									Desktop.getDesktop().open(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()).getParent().toFile());
								}
								catch (IOException e) {
									DataExplorer.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0065));
								}
							}
						}
					});
				}
				new MenuItem(popupMenu, SWT.SEPARATOR);
				{
					this.hideItem = new MenuItem(popupMenu, SWT.CASCADE);
					this.hideItem.setText(Messages.getString(MessageIds.GDE_MSGT0852));
					this.hideMenu = new Menu(this.hideItem);
					this.hideItem.setMenu(this.hideMenu);
				}
				{
					this.hideMenuRecordSetItem = new MenuItem(this.hideMenu, SWT.PUSH);
					this.hideMenuRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0853));
					if (!GDE.IS_OS_ARCH_ARM) this.hideMenuRecordSetItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0854));
					this.hideMenuRecordSetItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							ExclusionActivity.setExcludeRecordSet(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()),
									popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString());

							TabAreaContextMenu.this.settings.setSuppressMode(true);
							TabAreaContextMenu.this.application.getMenuBar().suppressModeItem.setSelection(true);

							TabAreaContextMenu.this.application.setupHistoWindows();
						}
					});
				}
				{
					this.hideMenuFileItem = new MenuItem(this.hideMenu, SWT.PUSH);
					this.hideMenuFileItem.setText(Messages.getString(MessageIds.GDE_MSGT0855));
					if (!GDE.IS_OS_ARCH_ARM) this.hideMenuFileItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0856));
					this.hideMenuFileItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							ExclusionActivity.setExcludeRecordSet(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()), GDE.STRING_EMPTY);

							TabAreaContextMenu.this.settings.setSuppressMode(true);
							TabAreaContextMenu.this.application.getMenuBar().suppressModeItem.setSelection(true);

							TabAreaContextMenu.this.application.setupHistoWindows();
						}
					});
				}
				new MenuItem(this.hideMenu, SWT.SEPARATOR);
				{
					this.hideMenuRevokeItem = new MenuItem(this.hideMenu, SWT.PUSH);
					this.hideMenuRevokeItem.setText(Messages.getString(MessageIds.GDE_MSGT0857));
					if (!GDE.IS_OS_ARCH_ARM) this.hideMenuRevokeItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0858));
					this.hideMenuRevokeItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							if (popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()) != null)
								ExclusionActivity.clearExcludeLists(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()).getParent());
							else
								ExclusionActivity.clearExcludeLists(null);

							TabAreaContextMenu.this.application.setupHistoWindows();
						}
					});
				}
				new MenuItem(popupMenu, SWT.SEPARATOR);
				{
					this.suppressModeItem = new MenuItem(popupMenu, SWT.CHECK);
					this.suppressModeItem.setText(Messages.getString(MessageIds.GDE_MSGT0859));
					if (!GDE.IS_OS_ARCH_ARM) this.suppressModeItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0860));
					this.suppressModeItem.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent evt) {
							if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "suppressModeItem.widgetSelected, event=" + evt); //$NON-NLS-1$
							TabAreaContextMenu.this.settings.setSuppressMode(TabAreaContextMenu.this.suppressModeItem.getSelection());
							TabAreaContextMenu.this.application.getMenuBar().suppressModeItem.setSelection(TabAreaContextMenu.this.suppressModeItem.getSelection());

							TabAreaContextMenu.this.application.setupHistoWindows();
						}
					});
				}
			}

			if (type == TabMenuType.GRAPHICS || type == TabMenuType.HISTOGRAPHICS || type == TabMenuType.HISTOTABLE) { // -1 as index mean initialization phase
				this.curveSelectionItem = new MenuItem(popupMenu, SWT.CHECK);
				this.curveSelectionItem.setText(Messages.getString(MessageIds.GDE_MSGT0040));
				this.curveSelectionItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.curveSelectionItem.getSelection();
						TabAreaContextMenu.this.application.setCurveSelectorEnabled(selection);
						TabAreaContextMenu.this.application.getMenuBar().curveSelectionMenuItem.setSelection(selection);
					}
				});
				this.displayGraphicsHeaderItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.GDE_MSGT0041));
				this.displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsHeaderItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().graphicsHeaderMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableGraphicsHeader(selection);
						TabAreaContextMenu.this.application.updateHistoTabs(false, false);
						;
					}
				});
				this.displayGraphicsCommentItem = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsCommentItem.setText(Messages.getString(MessageIds.GDE_MSGT0042));
				if (!GDE.IS_OS_ARCH_ARM) this.displayGraphicsCommentItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0878));
				this.displayGraphicsCommentItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsCommentItem action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsCommentItem.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().recordCommentMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableRecordSetComment(selection);
						TabAreaContextMenu.this.application.updateHistoTabs(false, false);
						;
					}
				});
				this.displayGraphicsCurveSurvey = new MenuItem(popupMenu, SWT.CHECK);
				this.displayGraphicsCurveSurvey.setText(Messages.getString(MessageIds.GDE_MSGT0876));
				if (!GDE.IS_OS_ARCH_ARM) this.displayGraphicsCurveSurvey.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0877));
				this.displayGraphicsCurveSurvey.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "displayGraphicsCurveSurvey action performed! " + e); //$NON-NLS-1$
						boolean selection = TabAreaContextMenu.this.displayGraphicsCurveSurvey.getSelection();
						TabAreaContextMenu.this.application.getMenuBar().graphicsCurveSurveyMenuItem.setSelection(selection);
						TabAreaContextMenu.this.application.enableCurveSurvey(selection);
						TabAreaContextMenu.this.application.updateHistoTabs(false, false);
						;
					}
				});
				this.separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);
			}

			this.copyTabItem = new MenuItem(popupMenu, SWT.PUSH);
			this.copyTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0026).substring(0, Messages.getString(MessageIds.GDE_MSGT0026).lastIndexOf('\t')));
			this.copyTabItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event e) {
					TabAreaContextMenu.log.log(Level.FINEST, "copyTabItem action performed! " + e); //$NON-NLS-1$
					TabAreaContextMenu.this.application.copyTabContentAsImage();
				}
			});

			if (type == TabMenuType.GRAPHICS || type == TabMenuType.COMPARE || type == TabMenuType.UTILITY || type == TabMenuType.HISTOGRAPHICS || type == TabMenuType.HISTOTABLE) {
				this.copyPrintImageItem = new MenuItem(popupMenu, SWT.PUSH);
				this.copyPrintImageItem.setText(Messages.getString(MessageIds.GDE_MSGT0027).substring(0, Messages.getString(MessageIds.GDE_MSGT0027).lastIndexOf('\t')));
				this.copyPrintImageItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.copyGraphicsPrintImage();
					}
				});
			}

			if (type != TabMenuType.TABLE || type == TabMenuType.HISTOTABLE) {
				//
				//			{
				this.separatorCopy = new MenuItem(popupMenu, SWT.SEPARATOR);
				this.outherAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.outherAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0462));
				this.outherAreaColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setSurroundingBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
				this.innerAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.innerAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0463));
				this.innerAreaColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setInnerAreaBackground(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type == TabMenuType.GRAPHICS || type == TabMenuType.COMPARE || type == TabMenuType.UTILITY || type == TabMenuType.HISTOGRAPHICS) {
				this.borderColorItem = new MenuItem(popupMenu, SWT.PUSH);
				this.borderColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0464));
				this.borderColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = TabAreaContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							TabAreaContextMenu.this.application.setBorderColor(TabAreaContextMenu.this.application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type == TabMenuType.TABLE || type == TabMenuType.HISTOTABLE) {
				this.dateTimeItem = new MenuItem(popupMenu, SWT.CHECK);
				this.dateTimeItem.setText(Messages.getString(MessageIds.GDE_MSGT0436));
				this.dateTimeItem.setSelection(Settings.getInstance().isTimeFormatAbsolute());
				this.dateTimeItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "dateTimeItem action performed! " + e); //$NON-NLS-1$
						TabAreaContextMenu.this.application.setAbsoluteDateTime(TabAreaContextMenu.this.dateTimeItem.getSelection());
					}
				});
				this.editTableItem = new MenuItem(popupMenu, SWT.CHECK);
				this.editTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0731));
				this.editTableItem.setSelection(Settings.getInstance().isDataTableEditable());
				this.editTableItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.OFF, "editTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setDataTableEditable(TabAreaContextMenu.this.editTableItem.getSelection());
					}
				});
				this.partialTableItem = new MenuItem(popupMenu, SWT.CHECK);
				this.partialTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0704));
				this.partialTableItem.setSelection(Settings.getInstance().isPartialDataTable());
				this.partialTableItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "partialTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setPartialDataTable(TabAreaContextMenu.this.partialTableItem.getSelection());
						TabAreaContextMenu.this.application.updateAllTabs(true, false);
						TabAreaContextMenu.this.application.updateHistoTabs(false, false);
					}
				});
			}

			if (type == TabMenuType.DIGITAL) {
				this.setDigitalFontItem = new MenuItem(popupMenu, SWT.PUSH);
				this.setDigitalFontItem.setText(Messages.getString(MessageIds.GDE_MSGT0726));
				this.setDigitalFontItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						TabAreaContextMenu.log.log(Level.FINEST, "setDigitalFontItem action performed! " + e); //$NON-NLS-1$
						int selectedFontSize = TabAreaContextMenu.this.application.openFontSizeDialog();
						if (selectedFontSize != 0) TabAreaContextMenu.this.application.setTabFontSize(TabAreaContextMenu.this.application.getTabSelectionIndex(), selectedFontSize);
					}
				});
			}
			this.isCreated = true;
		}

	}

	private void setAllEnabled(boolean enabled) {
		if (this.fileName != null) this.fileName.setEnabled(enabled);
		if (this.openRecordSetItem != null) this.openRecordSetItem.setEnabled(enabled);
		if (this.deleteFileItem != null) this.deleteFileItem.setEnabled(enabled);
		if (this.openFolderItem != null) this.openFolderItem.setEnabled(enabled);
		if (this.hideItem != null) this.hideItem.setEnabled(enabled);
		if (this.hideMenuRecordSetItem != null) this.hideMenuRecordSetItem.setEnabled(enabled);
		if (this.hideMenuFileItem != null) this.hideMenuFileItem.setEnabled(enabled);
		if (this.hideMenuRevokeItem != null) this.hideMenuRevokeItem.setEnabled(enabled);
		if (this.suppressModeItem != null) this.suppressModeItem.setEnabled(enabled);

		if (this.curveSelectionItem != null) this.curveSelectionItem.setEnabled(enabled);
		if (this.displayGraphicsHeaderItem != null) this.displayGraphicsHeaderItem.setEnabled(enabled);
		if (this.displayGraphicsCommentItem != null) this.displayGraphicsCommentItem.setEnabled(enabled);
		if (this.displayGraphicsCurveSurvey != null) this.displayGraphicsCurveSurvey.setEnabled(enabled);
		if (this.separatorView != null) this.separatorView.setEnabled(enabled);
		if (this.copyTabItem != null) this.copyTabItem.setEnabled(enabled);
		if (this.copyPrintImageItem != null) this.copyPrintImageItem.setEnabled(enabled);
		if (this.separatorCopy != null) this.separatorCopy.setEnabled(enabled);
		if (this.outherAreaColorItem != null) this.outherAreaColorItem.setEnabled(enabled);
		if (this.innerAreaColorItem != null) this.innerAreaColorItem.setEnabled(enabled);
		if (this.borderColorItem != null) this.borderColorItem.setEnabled(enabled);
		if (this.dateTimeItem != null) this.dateTimeItem.setEnabled(enabled);
		if (this.editTableItem != null) this.editTableItem.setEnabled(enabled);
		if (this.partialTableItem != null) this.partialTableItem.setEnabled(enabled);
		if (this.setDigitalFontItem != null) this.setDigitalFontItem.setEnabled(enabled);
	}

}
