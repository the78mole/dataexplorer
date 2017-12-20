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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.ui.menu;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
import gde.histo.datasources.DirectoryScanner.SourceDataSet;
import gde.histo.exclusions.ExclusionActivity;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.tab.GraphicsWindow;
import gde.ui.tab.GraphicsWindow.GraphicsType;
import gde.utils.FileUtils;

/**
 * Provides a context menu to the graphics chart area and enable selection of background color, ...
 * @author Thomas Eickert (USER)
 */
public class HistoTabAreaContextMenu {
	private final static String	$CLASS_NAME	= HistoTabAreaContextMenu.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final Settings			settings		= Settings.getInstance();

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
	protected boolean						isCreated		= false;

	private MenuItem						fileName, openRecordSetItem, deleteFileItem, openFolderItem, hideItem;
	private Menu								hideMenu;
	private MenuItem						hideMenuRecordSetItem, hideMenuFileItem, hideMenuRevokeItem;
	private Menu								trackMenu, warningMenu;
	private MenuItem						trackItem, boxplotItem, spotsItem;
	private MenuItem						warningItem, warningCountItem0, warningCountItem1, warningCountItem2, warningCountItem3;
	private MenuItem						suppressModeItem;

	public enum TabMenuType {
		HISTOGRAPHICS, HISTOSUMMARY, HISTOTABLE;

		boolean isHistoChart() {
			return this == HISTOGRAPHICS || this == HISTOSUMMARY;
		}
	};

	public enum TabMenuOnDemand {
		IS_CURSOR_IN_CANVAS, DATA_LINK_PATH, DATA_FILE_PATH, RECORDSET_BASE_NAME, EXCLUDED_LIST
	};

	public void createMenu(Menu popupMenu, TabMenuType type) {
		popupMenu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				HistoTabAreaContextMenu.log.log(Level.FINEST, "menuShown action " + e); //$NON-NLS-1$
				if (type.isHistoChart()) {
					suppressModeItem.setSelection(application.getMenuBar().getSuppressModeItem().getSelection());
					curveSelectionItem.setSelection(application.getMenuBar().getCurveSelectionMenuItem().getSelection());
					displayGraphicsHeaderItem.setSelection(application.getMenuBar().getGraphicsHeaderMenuItem().getSelection());
					displayGraphicsCommentItem.setSelection(application.getMenuBar().getRecordCommentMenuItem().getSelection());
					displayGraphicsCurveSurvey.setSelection(application.getMenuBar().getGraphicsCurveSurveyMenuItem().getSelection());

					fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
					openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));

					if (popupMenu.getData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString()) == null) { // not within the curve area canvas bounds
						setAllEnabled(false);
						suppressModeItem.setEnabled(true);
						curveSelectionItem.setEnabled(true);
						hideItem.setEnabled(true);
						hideMenuRevokeItem.setEnabled(true);
						trackItem.setEnabled(true);
						boxplotItem.setEnabled(true);
						spotsItem.setEnabled(true);
						warningItem.setEnabled(true);
						warningCountItem0.setEnabled(true);
						warningCountItem1.setEnabled(true);
						warningCountItem2.setEnabled(true);
						warningCountItem3.setEnabled(true);
						displayGraphicsHeaderItem.setEnabled(true);
						displayGraphicsCommentItem.setEnabled(true);
						displayGraphicsCurveSurvey.setEnabled(true);
						copyTabItem.setEnabled(true);
						copyPrintImageItem.setEnabled(true);
						borderColorItem.setEnabled(true);
					} else {
						setAllEnabled(true);
						String dataFilePath = popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString();
						if (dataFilePath.isEmpty()) {
							fileName.setEnabled(false);
							openRecordSetItem.setEnabled(false);
							deleteFileItem.setEnabled(false);
							openFolderItem.setEnabled(false);

							hideMenuRecordSetItem.setEnabled(false);
							hideMenuFileItem.setEnabled(false);
						} else {
							openFolderItem.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

							String tmpFileName = Paths.get(dataFilePath).getFileName().toString();
							String displayName = tmpFileName.length() > 22 ? "..." + tmpFileName.substring(tmpFileName.length() - 22) : tmpFileName;
							fileName.setText(">> " + displayName.toString() + GDE.STRING_BLANK_COLON_BLANK + String.format("%1.22s", popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString()) + " <<"); //$NON-NLS-1$ //$NON-NLS-2$
							openRecordSetItem.setText(Messages.getString(dataFilePath.toString().endsWith(GDE.FILE_ENDING_DOT_BIN) ? MessageIds.GDE_MSGT0850
									: MessageIds.GDE_MSGT0849));
						}
						String excludedList = popupMenu.getData(TabMenuOnDemand.EXCLUDED_LIST.toString()).toString();
						if (!GDE.IS_OS_ARCH_ARM) hideItem.setToolTipText(excludedList.isEmpty() ? Messages.getString(MessageIds.GDE_MSGT0798) : excludedList);
						setWarningCountIndex(settings.getWarningCountIndex());
					}
				}

				if (type == TabMenuType.HISTOTABLE) {
					suppressModeItem.setSelection(application.getMenuBar().getSuppressModeItem().getSelection());
					curveSelectionItem.setSelection(application.getMenuBar().getCurveSelectionMenuItem().getSelection());
					displayGraphicsHeaderItem.setSelection(application.getMenuBar().getGraphicsHeaderMenuItem().getSelection());
					displayGraphicsCommentItem.setSelection(application.getMenuBar().getRecordCommentMenuItem().getSelection());
					displayGraphicsCurveSurvey.setSelection(application.getMenuBar().getGraphicsCurveSurveyMenuItem().getSelection());

					fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
					openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));

					if (popupMenu.getData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString()) == null) { // not within the curve area canvas bounds
						setAllEnabled(false);
						suppressModeItem.setEnabled(true);
						hideItem.setEnabled(true);
						hideMenuRevokeItem.setEnabled(true);
						copyTabItem.setEnabled(true);
					} else {
						setAllEnabled(true);
						curveSelectionItem.setEnabled(false);
						trackItem.setEnabled(false);
						displayGraphicsHeaderItem.setEnabled(false);
						displayGraphicsCommentItem.setEnabled(false);
						displayGraphicsCurveSurvey.setEnabled(false);
						copyPrintImageItem.setEnabled(false);
						dateTimeItem.setEnabled(false);
						editTableItem.setEnabled(false);
						outherAreaColorItem.setEnabled(false);
						innerAreaColorItem.setEnabled(false);

						final String dataFilePath = popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString();
						if (dataFilePath.isEmpty()) {
							fileName.setEnabled(false);
							openRecordSetItem.setEnabled(false);
							deleteFileItem.setEnabled(false);
							openFolderItem.setEnabled(false);

							hideMenuRecordSetItem.setEnabled(false);
							hideMenuFileItem.setEnabled(false);
						} else {
							openFolderItem.setEnabled(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN));

							String tmpFileName = Paths.get(dataFilePath).getFileName().toString();
							String displayName = tmpFileName.length() > 22 ? "..." + tmpFileName.substring(tmpFileName.length() - 22) : tmpFileName;
							fileName.setText(">> " + displayName.toString() + GDE.STRING_BLANK_COLON_BLANK + String.format("%1.22s", popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString()) + " <<"); //$NON-NLS-2$ //$NON-NLS-3$
							openRecordSetItem.setText(Messages.getString(dataFilePath.toString().endsWith(GDE.FILE_ENDING_DOT_BIN) ? MessageIds.GDE_MSGT0850
									: MessageIds.GDE_MSGT0849));
						}
						String excludedList = popupMenu.getData(TabMenuOnDemand.EXCLUDED_LIST.toString()).toString();
						if (!GDE.IS_OS_ARCH_ARM) hideItem.setToolTipText(excludedList.isEmpty() ? Messages.getString(MessageIds.GDE_MSGT0798) : excludedList);
						setWarningCountIndex(settings.getWarningCountIndex());
					}
				}

				// clear consumed menu type selector
				popupMenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.toString(), null);
			}

			@Override
			public void menuHidden(MenuEvent e) {
				// ignore
			}
		});
		if (!isCreated) {
			{
				fileName = new MenuItem(popupMenu, SWT.None);
				fileName.setText(">> [" + Messages.getString(MessageIds.GDE_MSGT0864) + "] <<"); //$NON-NLS-1$ //$NON-NLS-2$
				if (!GDE.IS_OS_ARCH_ARM) fileName.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0863));
			}
			new MenuItem(popupMenu, SWT.SEPARATOR);
			{
				openRecordSetItem = new MenuItem(popupMenu, SWT.PUSH);
				openRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0849));
				if (!GDE.IS_OS_ARCH_ARM) openRecordSetItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0851));
				openRecordSetItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						SourceDataSet sourceDataSet = new SourceDataSet(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()));
						String recordSetName = popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString().split(Pattern.quote(TrailRecordSet.BASE_NAME_SEPARATOR))[0];
						if (sourceDataSet.load(recordSetName)) {
							application.selectTab(c -> c instanceof GraphicsWindow && ((GraphicsWindow) c).getGraphicsType().equals(GraphicsType.NORMAL));
							application.updateGraphicsWindow();
						}
					}
				});
			}
			{
				deleteFileItem = new MenuItem(popupMenu, SWT.PUSH);
				deleteFileItem.setText(Messages.getString(MessageIds.GDE_MSGT0861));
				if (!GDE.IS_OS_ARCH_ARM) deleteFileItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0862));
				deleteFileItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "deleteFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						// check if the file exists and if the user really wants to delete it
						File file = new File(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString());
						if (FileUtils.checkFileExist(file.getPath())) {
							String linkFilePath = popupMenu.getData(TabMenuOnDemand.DATA_LINK_PATH.toString()).toString();
							if (file.getPath().endsWith(GDE.FILE_ENDING_DOT_OSD) && !linkFilePath.isEmpty()) {
								FileUtils.deleteFile(linkFilePath);
								if (application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0071, new Object[] {
										file.getAbsolutePath() })) == SWT.YES) {
									FileUtils.deleteFile(file.getPath());
									application.getPresentHistoExplorer().resetHisto();
								}
							} else {
								if (application.openYesNoMessageDialog(Messages.getString(MessageIds.GDE_MSGI0050, new Object[] {
										file.getAbsolutePath() })) == SWT.YES) {
									FileUtils.deleteFile(file.getPath());
									application.getPresentHistoExplorer().resetHisto();
								}
							}
						}
					}
				});
			}
			{
				openFolderItem = new MenuItem(popupMenu, SWT.PUSH);
				openFolderItem.setText(Messages.getString(MessageIds.GDE_MSGT0873));
				if (!GDE.IS_OS_ARCH_ARM) openFolderItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0874));
				openFolderItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "openFolderItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						if (Desktop.isDesktopSupported()) {
							try {
								Desktop.getDesktop().open(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()).getParent().toFile());
							} catch (IOException e) {
								DataExplorer.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0065));
							}
						}
					}
				});
			}
			new MenuItem(popupMenu, SWT.SEPARATOR);
			{
				hideItem = new MenuItem(popupMenu, SWT.CASCADE);
				hideItem.setText(Messages.getString(MessageIds.GDE_MSGT0852));
				hideMenu = new Menu(hideItem);
				hideItem.setMenu(hideMenu);
			}
			{
				hideMenuRecordSetItem = new MenuItem(hideMenu, SWT.PUSH);
				hideMenuRecordSetItem.setText(Messages.getString(MessageIds.GDE_MSGT0853));
				if (!GDE.IS_OS_ARCH_ARM) hideMenuRecordSetItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0854));
				hideMenuRecordSetItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuRecordSetItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						ExclusionActivity.setExcludeRecordSet(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()), popupMenu.getData(TabMenuOnDemand.RECORDSET_BASE_NAME.toString()).toString());

						settings.setSuppressMode(true);
						application.getMenuBar().getSuppressModeItem().setSelection(true);

						application.getPresentHistoExplorer().updateHistoTabs(false, true);
					}
				});
			}
			{
				hideMenuFileItem = new MenuItem(hideMenu, SWT.PUSH);
				hideMenuFileItem.setText(Messages.getString(MessageIds.GDE_MSGT0855));
				if (!GDE.IS_OS_ARCH_ARM) hideMenuFileItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0856));
				hideMenuFileItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						ExclusionActivity.setExcludeRecordSet(Paths.get(popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString()).toString()), GDE.STRING_EMPTY);

						settings.setSuppressMode(true);
						application.getMenuBar().getSuppressModeItem().setSelection(true);

						application.getPresentHistoExplorer().updateHistoTabs(false, true);
					}
				});
			}
			new MenuItem(hideMenu, SWT.SEPARATOR);
			{
				hideMenuRevokeItem = new MenuItem(hideMenu, SWT.PUSH);
				hideMenuRevokeItem.setText(Messages.getString(MessageIds.GDE_MSGT0857));
				if (!GDE.IS_OS_ARCH_ARM) hideMenuRevokeItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0858));
				hideMenuRevokeItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "hideMenuFileItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						String dataFilePath = (String) popupMenu.getData(TabMenuOnDemand.DATA_FILE_PATH.toString());
						Path path = dataFilePath != null ? Paths.get(dataFilePath.toString()).getParent(): null;
						ExclusionActivity.clearExcludeLists(path);

						application.getPresentHistoExplorer().resetHisto();
					}
				});
			}

			new MenuItem(popupMenu, SWT.SEPARATOR);
			{
				trackItem = new MenuItem(popupMenu, SWT.CASCADE);
				trackItem.setText(Messages.getString(MessageIds.GDE_MSGT0890));
				if (!GDE.IS_OS_ARCH_ARM) trackItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0891));
				trackMenu = new Menu(trackItem);
				trackItem.setMenu(trackMenu);
			}
			{
				boxplotItem = new MenuItem(trackMenu, SWT.CHECK);
				boxplotItem.setText(Messages.getString(MessageIds.GDE_MSGT0892));
				if (!GDE.IS_OS_ARCH_ARM) boxplotItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0893));
				boxplotItem.setSelection(settings.isSummaryBoxVisible());
				boxplotItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "boxplotItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						boolean selection = boxplotItem.getSelection();
						settings.setSummaryBoxVisible(selection);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}
			{
				spotsItem = new MenuItem(trackMenu, SWT.CHECK);
				spotsItem.setText(Messages.getString(MessageIds.GDE_MSGT0894));
				if (!GDE.IS_OS_ARCH_ARM) spotsItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0895));
				spotsItem.setSelection(settings.isSummarySpotsVisible());
				spotsItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "spotsItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						boolean selection = spotsItem.getSelection();
						settings.setSummarySpotsVisible(selection);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}

			{
				warningItem = new MenuItem(trackMenu, SWT.CASCADE);
				warningItem.setText(Messages.getString(MessageIds.GDE_MSGT0888));
				if (!GDE.IS_OS_ARCH_ARM) warningItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0889));
				warningMenu = new Menu(hideItem);
				warningItem.setMenu(warningMenu);
			}
			{
				warningCountItem0 = new MenuItem(warningMenu, SWT.CHECK);
				warningCountItem0.setText(String.valueOf(settings.getWarningCount(0)) + " logs");
				warningCountItem0.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem0.widgetSelected, event=" + evt); //$NON-NLS-1$
						setWarningCountIndex(0);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}
			{
				warningCountItem1 = new MenuItem(warningMenu, SWT.CHECK);
				warningCountItem1.setText(String.valueOf(settings.getWarningCount(1)) + " logs");
				warningCountItem1.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem1.widgetSelected, event=" + evt); //$NON-NLS-1$
						setWarningCountIndex(1);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}
			{
				warningCountItem2 = new MenuItem(warningMenu, SWT.CHECK);
				warningCountItem2.setText(String.valueOf(settings.getWarningCount(2)) + " logs");
				warningCountItem2.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem2.widgetSelected, event=" + evt); //$NON-NLS-1$
						setWarningCountIndex(2);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}
			{
				warningCountItem3 = new MenuItem(warningMenu, SWT.CHECK);
				warningCountItem3.setText(String.valueOf(settings.getWarningCount(3)) + " logs");
				warningCountItem3.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "warningCountItem3.widgetSelected, event=" + evt); //$NON-NLS-1$
						setWarningCountIndex(3);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}

			new MenuItem(popupMenu, SWT.SEPARATOR);
			{
				suppressModeItem = new MenuItem(popupMenu, SWT.CHECK);
				suppressModeItem.setText(Messages.getString(MessageIds.GDE_MSGT0859));
				if (!GDE.IS_OS_ARCH_ARM) suppressModeItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0860));
				suppressModeItem.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "suppressModeItem.widgetSelected, event=" + evt); //$NON-NLS-1$
						settings.setSuppressMode(suppressModeItem.getSelection());
						application.getMenuBar().getSuppressModeItem().setSelection(suppressModeItem.getSelection());

						application.getPresentHistoExplorer().resetHisto();
					}
				});
			}

			curveSelectionItem = new MenuItem(popupMenu, SWT.CHECK);
			curveSelectionItem.setText(Messages.getString(MessageIds.GDE_MSGT0040));
			curveSelectionItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HistoTabAreaContextMenu.log.log(Level.FINEST, "curveSelectionItem action performed! " + e); //$NON-NLS-1$
					boolean selection = curveSelectionItem.getSelection();
					application.enableCurveSelector(selection);
					application.getMenuBar().getCurveSelectionMenuItem().setSelection(selection);
					application.getPresentHistoExplorer().updateHistoTabs(false, false);
				}
			});
			displayGraphicsHeaderItem = new MenuItem(popupMenu, SWT.CHECK);
			displayGraphicsHeaderItem.setText(Messages.getString(MessageIds.GDE_MSGT0041));
			displayGraphicsHeaderItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HistoTabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsHeaderItem action performed! " + e); //$NON-NLS-1$
					boolean selection = displayGraphicsHeaderItem.getSelection();
					application.getMenuBar().getGraphicsHeaderMenuItem().setSelection(selection);
					application.enableGraphicsHeader(selection);
					application.getPresentHistoExplorer().updateHistoTabs(false, false);
				}
			});
			displayGraphicsCommentItem = new MenuItem(popupMenu, SWT.CHECK);
			displayGraphicsCommentItem.setText(Messages.getString(MessageIds.GDE_MSGT0042));
			if (!GDE.IS_OS_ARCH_ARM) displayGraphicsCommentItem.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0878));
			displayGraphicsCommentItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HistoTabAreaContextMenu.log.log(Level.FINEST, "toggleViewGraphicsCommentItem action performed! " + e); //$NON-NLS-1$
					boolean selection = displayGraphicsCommentItem.getSelection();
					application.getMenuBar().getRecordCommentMenuItem().setSelection(selection);
					application.enableRecordSetComment(selection);
					application.getPresentHistoExplorer().updateHistoTabs(false, false);
				}
			});

			displayGraphicsCurveSurvey = new MenuItem(popupMenu, SWT.CHECK);
			displayGraphicsCurveSurvey.setText(Messages.getString(MessageIds.GDE_MSGT0876));
			if (!GDE.IS_OS_ARCH_ARM) displayGraphicsCurveSurvey.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0877));
			displayGraphicsCurveSurvey.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HistoTabAreaContextMenu.log.log(Level.FINEST, "displayGraphicsCurveSurvey action performed! " + e); //$NON-NLS-1$
					boolean selection = displayGraphicsCurveSurvey.getSelection();
					application.getMenuBar().getGraphicsCurveSurveyMenuItem().setSelection(selection);
					application.getPresentHistoExplorer().enableCurveSurvey(selection);
					application.getPresentHistoExplorer().updateHistoTabs(false, false);
				}
			});
			separatorView = new MenuItem(popupMenu, SWT.SEPARATOR);

			copyTabItem = new MenuItem(popupMenu, SWT.PUSH);
			copyTabItem.setText(Messages.getString(MessageIds.GDE_MSGT0026).substring(0, Messages.getString(MessageIds.GDE_MSGT0026).lastIndexOf('\t')));
			copyTabItem.addListener(SWT.Selection, new Listener() {

				@Override
				public void handleEvent(Event e) {
					HistoTabAreaContextMenu.log.log(Level.FINEST, "copyTabItem action performed! " + e); //$NON-NLS-1$
					application.copyTabContentAsImage();
				}
			});

			copyPrintImageItem = new MenuItem(popupMenu, SWT.PUSH);
			copyPrintImageItem.setText(Messages.getString(MessageIds.GDE_MSGT0027).substring(0, Messages.getString(MessageIds.GDE_MSGT0027).lastIndexOf('\t')));
			copyPrintImageItem.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event e) {
					HistoTabAreaContextMenu.log.log(Level.FINEST, "copyPrintImageItem action performed! " + e); //$NON-NLS-1$
					application.copyGraphicsPrintImage();
				}
			});

			if (type == TabMenuType.HISTOTABLE) {
				//
				// {
				separatorCopy = new MenuItem(popupMenu, SWT.SEPARATOR);
				outherAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				outherAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0462));
				outherAreaColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						HistoTabAreaContextMenu.log.log(Level.FINEST, "outherAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = application.openColorDialog();
						if (rgb != null) {
							application.setSurroundingBackground(application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
				innerAreaColorItem = new MenuItem(popupMenu, SWT.PUSH);
				innerAreaColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0463));
				innerAreaColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						HistoTabAreaContextMenu.log.log(Level.FINEST, "innerAreaColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = application.openColorDialog();
						if (rgb != null) {
							application.setInnerAreaBackground(application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type.isHistoChart()) {
				borderColorItem = new MenuItem(popupMenu, SWT.PUSH);
				borderColorItem.setText(Messages.getString(MessageIds.GDE_MSGT0464));
				borderColorItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						HistoTabAreaContextMenu.log.log(Level.FINEST, "borderColorItem action performed! " + e); //$NON-NLS-1$
						RGB rgb = application.openColorDialog();
						if (rgb != null) {
							application.setBorderColor(application.getTabSelectionIndex(), SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
						}
					}
				});
			}

			if (type == TabMenuType.HISTOTABLE) {
				dateTimeItem = new MenuItem(popupMenu, SWT.CHECK);
				dateTimeItem.setText(Messages.getString(MessageIds.GDE_MSGT0436));
				dateTimeItem.setSelection(Settings.getInstance().isTimeFormatAbsolute());
				dateTimeItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						HistoTabAreaContextMenu.log.log(Level.FINEST, "dateTimeItem action performed! " + e); //$NON-NLS-1$
						application.setAbsoluteDateTime(dateTimeItem.getSelection());
					}
				});
				editTableItem = new MenuItem(popupMenu, SWT.CHECK);
				editTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0731));
				editTableItem.setSelection(Settings.getInstance().isDataTableEditable());
				editTableItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						HistoTabAreaContextMenu.log.log(Level.FINEST, "editTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setDataTableEditable(editTableItem.getSelection());
					}
				});
				partialTableItem = new MenuItem(popupMenu, SWT.CHECK);
				partialTableItem.setText(Messages.getString(MessageIds.GDE_MSGT0704));
				partialTableItem.setSelection(Settings.getInstance().isPartialDataTable());
				partialTableItem.addListener(SWT.Selection, new Listener() {
					@Override
					public void handleEvent(Event e) {
						HistoTabAreaContextMenu.log.log(Level.FINEST, "partialTableItem action performed! " + e); //$NON-NLS-1$
						Settings.getInstance().setPartialDataTable(partialTableItem.getSelection());
						application.updateAllTabs(true, false);
						application.getPresentHistoExplorer().updateHistoTabs(false, false);
					}
				});
			}

			isCreated = true;
		}

	}

	private void setAllEnabled(boolean enabled) {
		fileName.setEnabled(enabled);
		openRecordSetItem.setEnabled(enabled);
		deleteFileItem.setEnabled(enabled);
		openFolderItem.setEnabled(enabled);
		hideItem.setEnabled(enabled);
		hideMenuRecordSetItem.setEnabled(enabled);
		hideMenuFileItem.setEnabled(enabled);
		hideMenuRevokeItem.setEnabled(enabled);
		suppressModeItem.setEnabled(enabled);
		trackItem.setEnabled(enabled);
		boxplotItem.setEnabled(enabled);
		spotsItem.setEnabled(enabled);
		warningItem.setEnabled(enabled);
		warningCountItem0.setEnabled(enabled);
		warningCountItem1.setEnabled(enabled);
		warningCountItem2.setEnabled(enabled);
		warningCountItem3.setEnabled(enabled);

		curveSelectionItem.setEnabled(enabled);
		displayGraphicsHeaderItem.setEnabled(enabled);
		displayGraphicsCommentItem.setEnabled(enabled);
		displayGraphicsCurveSurvey.setEnabled(enabled);
		separatorView.setEnabled(enabled);
		copyTabItem.setEnabled(enabled);
		copyPrintImageItem.setEnabled(enabled);

		if (separatorCopy != null) separatorCopy.setEnabled(enabled);
		if (outherAreaColorItem != null) outherAreaColorItem.setEnabled(enabled);
		if (innerAreaColorItem != null) innerAreaColorItem.setEnabled(enabled);
		if (borderColorItem != null) borderColorItem.setEnabled(enabled);
		if (dateTimeItem != null) dateTimeItem.setEnabled(enabled);
		if (editTableItem != null) editTableItem.setEnabled(enabled);
		if (partialTableItem != null) partialTableItem.setEnabled(enabled);
		if (setDigitalFontItem != null) setDigitalFontItem.setEnabled(enabled);
	}

	/**
	 * @param newIndex
	 */
	private void setWarningCountIndex(int newIndex) {
		settings.setWarningCountIndex(String.valueOf(newIndex));
		warningCountItem0.setSelection(newIndex == 0);
		warningCountItem1.setSelection(newIndex == 1);
		warningCountItem2.setSelection(newIndex == 2);
		warningCountItem3.setSelection(newIndex == 3);
	}

}
