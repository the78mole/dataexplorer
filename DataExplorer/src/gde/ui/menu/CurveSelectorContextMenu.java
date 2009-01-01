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
package osde.ui.menu;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableItem;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.dialog.AxisEndValuesDialog;
import osde.ui.tab.GraphicsWindow;

/**
 * Context menu class of the curve selection window acts as popup menu
 * @author Winfried Brügmann
 */
public class CurveSelectorContextMenu {
	final static Logger									 log	= Logger.getLogger(CurveSelectorContextMenu.class.getName());

	Menu													lineWidthMenu, lineTypeMenu, axisEndValuesMenu, axisNumberFormatMenu, axisPositionMenu, timeGridMenu, horizontalGridMenu, measurementMenu;
	MenuItem											recordName, lineVisible, lineColor, copyCurveCompare, cleanCurveCompare;
	MenuItem											lineWidth, lineWidthMenuItem1, lineWidthMenuItem2, lineWidthMenuItem3;
	MenuItem											lineType, lineTypeMenuItem1, lineTypeMenuItem2, lineTypeMenuItem3;
	MenuItem											axisEndValues, axisEndAuto, axisEndRound, axisStarts0, axisEndManual;
	MenuItem											axisNumberFormat, axisNumberFormat0, axisNumberFormat1, axisNumberFormat2, axisNumberFormat3;
	MenuItem											axisPosition, axisPositionLeft, axisPositionRight;
	MenuItem 											measurement, measurementRecordName, simpleMeasure, deltaMeasure;
	MenuItem											timeGridColor, timeGrid, timeGridOff, timeGridMain, timeGridMod60;
	MenuItem											horizontalGridRecordName, horizontalGridColor, horizontalGrid, horizontalGridOff, horizontalGridEveryTick, horizontalGridEverySecond;

	RecordSet											recordSet;
	final OpenSerialDataExplorer	application;
	final Settings								settings = Settings.getInstance();
	AxisEndValuesDialog						axisEndValuesDialog;
	
	TableItem 										selectedItem;
	Record												actualRecord = null;
	boolean 											isRecordVisible = false;
	String 												recordNameKey = null;
	String 												recordNameMeasurement = OSDE.STRING_BLANK;
	int 													windowType = GraphicsWindow.TYPE_NORMAL;
	boolean												isSyncPlaceholder = false; // the sync placeholder record
	boolean												isScaleSynced = false; // scale sync for syncable records is requested
	
	public CurveSelectorContextMenu() {
		super();
		this.application = OpenSerialDataExplorer.getInstance();
		this.axisEndValuesDialog = new AxisEndValuesDialog(OpenSerialDataExplorer.getInstance().getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
	}

	public void createMenu(final Menu popupmenu) {
		try {
			popupmenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "popupmenu MenuListener.menuShown " + evt); //$NON-NLS-1$
					CurveSelectorContextMenu.this.selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					log.log(Level.FINER, CurveSelectorContextMenu.this.selectedItem.toString());
					CurveSelectorContextMenu.this.isSyncPlaceholder = new Boolean(""+CurveSelectorContextMenu.this.selectedItem.getData(OpenSerialDataExplorer.RECORD_SYNC_PLACEHOLDER)).booleanValue();
					CurveSelectorContextMenu.this.isScaleSynced = new Boolean(""+CurveSelectorContextMenu.this.selectedItem.getData(OpenSerialDataExplorer.RECORD_SYNC_PLACEHOLDER)).booleanValue();
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.recordNameKey = CurveSelectorContextMenu.this.selectedItem.getText();
						if (CurveSelectorContextMenu.this.isSyncPlaceholder) {
							CurveSelectorContextMenu.this.recordNameKey = CurveSelectorContextMenu.this.recordNameKey.substring(CurveSelectorContextMenu.this.recordNameKey.indexOf(' ')).trim();
						}
						log.info("===>>" + CurveSelectorContextMenu.this.recordNameKey);
						CurveSelectorContextMenu.this.windowType = (Integer) CurveSelectorContextMenu.this.selectedItem.getData(GraphicsWindow.WINDOW_TYPE);
						CurveSelectorContextMenu.this.recordSet = (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : CurveSelectorContextMenu.this.application.getCompareSet();

						if (CurveSelectorContextMenu.this.recordSet != null) {
							setAllEnabled(true);						
							
							if (CurveSelectorContextMenu.this.recordNameKey != null && CurveSelectorContextMenu.this.recordNameKey.length() > 1) {
								CurveSelectorContextMenu.this.actualRecord = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey);
								if (CurveSelectorContextMenu.this.actualRecord != null) {
									CurveSelectorContextMenu.this.recordName.setText(">>>>  " + CurveSelectorContextMenu.this.recordNameKey + "  <<<<"); //$NON-NLS-1$ //$NON-NLS-2$
									if (CurveSelectorContextMenu.this.isSyncPlaceholder) {
										CurveSelectorContextMenu.this.lineVisible.setText("Sync " + CurveSelectorContextMenu.this.recordSet.getSyncableName());
										CurveSelectorContextMenu.this.isRecordVisible = CurveSelectorContextMenu.this.recordSet.isSyncableSynced();
									}
									else {
										CurveSelectorContextMenu.this.lineVisible.setText(Messages.getString(MessageIds.OSDE_MSGT0085));
										CurveSelectorContextMenu.this.isRecordVisible = CurveSelectorContextMenu.this.actualRecord.isVisible();
									}
									CurveSelectorContextMenu.this.lineVisible.setSelection(CurveSelectorContextMenu.this.isRecordVisible);
									// check measurement selections
									//deltaMeasure.setSelection(recordSet.isDeltaMeasurementMode(recordNameKey));
									//disable all menu items which makes only sense if record is visible
									if (!CurveSelectorContextMenu.this.isRecordVisible) {
										CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
									}
								}
								else {
									return; // actual record is null, record related operations not possible
								}
							}
							// check sync placeholder record
							if (CurveSelectorContextMenu.this.isSyncPlaceholder) {
								CurveSelectorContextMenu.this.lineColor.setEnabled(false);
								CurveSelectorContextMenu.this.lineType.setEnabled(false);
								CurveSelectorContextMenu.this.lineWidth.setEnabled(false);
								CurveSelectorContextMenu.this.horizontalGrid.setEnabled(false);
								CurveSelectorContextMenu.this.measurement.setEnabled(false);
								CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
								if (!CurveSelectorContextMenu.this.isRecordVisible) {
									CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
									CurveSelectorContextMenu.this.axisNumberFormat.setEnabled(false);
								}
							}
							// check for scale synced
							if (CurveSelectorContextMenu.this.actualRecord.isScaleSynced()) {
								CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
								CurveSelectorContextMenu.this.axisNumberFormat.setEnabled(false);
								CurveSelectorContextMenu.this.axisPosition.setEnabled(false);
							}
							// check zoom mode
							if (CurveSelectorContextMenu.this.recordSet.isZoomMode()) {
								CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
								CurveSelectorContextMenu.this.axisEndValues.setText(Messages.getString(MessageIds.OSDE_MSGT0083));
							}
							else {
								CurveSelectorContextMenu.this.axisEndValues.setText(Messages.getString(MessageIds.OSDE_MSGT0084));
							}
							
							// check if record switched and measurement mode needs to be reset
							if (!CurveSelectorContextMenu.this.recordSet.isMeasurementMode(CurveSelectorContextMenu.this.recordNameMeasurement) && !CurveSelectorContextMenu.this.recordSet.isDeltaMeasurementMode(CurveSelectorContextMenu.this.recordNameMeasurement)) {
								CurveSelectorContextMenu.this.recordNameMeasurement = OSDE.STRING_BLANK;
								CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
								CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);

							}
														
							if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
								CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
							}
							
							// disable clear, if nothing to clear
							if (CurveSelectorContextMenu.this.application.getCompareSet().size() == 0) {
								CurveSelectorContextMenu.this.cleanCurveCompare.setEnabled(false);
							}
							// compare window has fixed defined scale end values
							if (CurveSelectorContextMenu.this.recordSet.isCompareSet()) {
								CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
							}
						}
						else
							setAllEnabled(false);
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "popupmenu MenuListener.menuHidden " + evt); //$NON-NLS-1$
				}
			});
			this.recordName = new MenuItem(popupmenu, SWT.None);

			new MenuItem(popupmenu, SWT.SEPARATOR);
			
			this.lineVisible = new MenuItem(popupmenu, SWT.CHECK);
			this.lineVisible.setText(Messages.getString(MessageIds.OSDE_MSGT0085));
			this.lineVisible.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineVisibler Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean checked = CurveSelectorContextMenu.this.lineVisible.getSelection();
						if (CurveSelectorContextMenu.this.isSyncPlaceholder) {
							CurveSelectorContextMenu.this.recordSet.setSyncRequested(checked);
						}
						else {
							CurveSelectorContextMenu.this.actualRecord.setVisible(checked);
						}
						CurveSelectorContextMenu.this.selectedItem.setChecked(checked);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.lineColor = new MenuItem(popupmenu, SWT.PUSH);
			this.lineColor.setText(Messages.getString(MessageIds.OSDE_MSGT0086));
			this.lineColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event evt) {
					log.log(Level.FINER, "lineColor performed! " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							Color color = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
							CurveSelectorContextMenu.this.selectedItem.setForeground(color);
							CurveSelectorContextMenu.this.actualRecord.setColor(color);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						}
					}
				}
			});
			this.lineWidth = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineWidth.setText(Messages.getString(MessageIds.OSDE_MSGT0087));
			this.lineWidthMenu = new Menu(this.lineWidth);
			this.lineWidth.setMenu(this.lineWidthMenu);
			this.lineWidthMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "lineWidthMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int width = CurveSelectorContextMenu.this.actualRecord.getLineWidth();
						switch (width) {
						case 1:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(true);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
							break;
						case 2:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(true);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
							break;
						case 3:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(true);
							break;
						default:
							CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
							break;
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "lineWidthMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.lineWidthMenuItem1 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem1.setText(Messages.getString(MessageIds.OSDE_MSGT0088));
			this.lineWidthMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, Messages.getString(MessageIds.OSDE_MSGT0089) + e); 
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineWidth(1);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineWidthMenuItem2 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineWidth2.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem2.setText(Messages.getString(MessageIds.OSDE_MSGT0090));
			this.lineWidthMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, Messages.getString(MessageIds.OSDE_MSGT0091) + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineWidth(2);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineWidthMenuItem3 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineWidth3.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem3.setText(Messages.getString(MessageIds.OSDE_MSGT0092));
			this.lineWidthMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, Messages.getString(MessageIds.OSDE_MSGT0093) + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineWidth(3);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.lineType = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineType.setText(Messages.getString(MessageIds.OSDE_MSGT0094));
			this.lineTypeMenu = new Menu(this.lineType);
			this.lineType.setMenu(this.lineTypeMenu);
			this.lineTypeMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "lineTypeMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int type = CurveSelectorContextMenu.this.actualRecord.getLineStyle();
						switch (type) {
						case SWT.LINE_SOLID:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(true);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
							break;
						case SWT.LINE_DASH:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(true);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
							break;
						case SWT.LINE_DOT:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(true);
							break;
						default:
							CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
							CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
							break;
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "lineTypeMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.lineTypeMenuItem1 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineType1.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem1.setText(Messages.getString(MessageIds.OSDE_MSGT0095));
			this.lineTypeMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineTypeMenuItem1 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_SOLID);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineTypeMenuItem2 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineType2.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem2.setText(Messages.getString(MessageIds.OSDE_MSGT0096));
			this.lineTypeMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineTypeMenuItem2 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_DASH);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineTypeMenuItem3 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineType3.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem3.setText(Messages.getString(MessageIds.OSDE_MSGT0097));
			this.lineTypeMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineTypeMenuItem3 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_DOT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.axisEndValues = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisEndValues.setText(Messages.getString(MessageIds.OSDE_MSGT0098));
			this.axisEndValuesMenu = new Menu(this.axisEndValues);
			this.axisEndValues.setMenu(this.axisEndValuesMenu);
			this.axisEndValuesMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "axisEndValuesMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isRounded = CurveSelectorContextMenu.this.actualRecord.isRoundOut();
						boolean isStart0 = CurveSelectorContextMenu.this.actualRecord.isStartpointZero();
						boolean isManual = CurveSelectorContextMenu.this.actualRecord.isStartEndDefined();
						CurveSelectorContextMenu.this.axisEndAuto.setSelection(true);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						if (isManual) {
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
							CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(true);
						}
						if (isStart0) {
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							//axisEndRound.setSelection(false);
							CurveSelectorContextMenu.this.axisStarts0.setSelection(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						}
						if (isRounded) {
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisEndRound.setSelection(true);
							//axisStarts0.setSelection(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "axisEndValuesMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.axisEndAuto = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndAuto.setText(Messages.getString(MessageIds.OSDE_MSGT0099));
			this.axisEndAuto.setSelection(true);
			this.axisEndAuto.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisEndAuto.SelectionListener = " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setRoundOut(false);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndRound = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndRound.setText(Messages.getString(MessageIds.OSDE_MSGT0101));
			this.axisEndRound.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisEndRound.SelectionListener = " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						if (CurveSelectorContextMenu.this.axisEndRound.getSelection()) { //true
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setRoundOut(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(true);
							CurveSelectorContextMenu.this.actualRecord.setRoundOut(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisStarts0 = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisStarts0.setText(Messages.getString(MessageIds.OSDE_MSGT0103));
			this.axisStarts0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisStarts0 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						if (CurveSelectorContextMenu.this.axisStarts0.getSelection()) { // true
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartpointZero(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartpointZero(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndManual = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndManual.setText(Messages.getString(MessageIds.OSDE_MSGT0104));
			this.axisEndManual.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisEndManual Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.axisEndManual.setSelection(true);
						CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
						CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
						Record record = CurveSelectorContextMenu.this.actualRecord;
						record.setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						record.setRoundOut(false);
						double[] oldMinMax = {record.getMinScaleValue(), record.getMaxScaleValue()};
						double[] newMinMax = CurveSelectorContextMenu.this.axisEndValuesDialog.open(oldMinMax);
						record.setStartEndDefined(true, newMinMax[0], newMinMax[1]);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisNumberFormat = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisNumberFormat.setText(Messages.getString(MessageIds.OSDE_MSGT0105));
			this.axisNumberFormatMenu = new Menu(this.axisNumberFormat);
			this.axisNumberFormat.setMenu(this.axisNumberFormatMenu);
			this.axisNumberFormatMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "axisNumberFormatMenu MenuListener.menuShown " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int format = CurveSelectorContextMenu.this.actualRecord.getNumberFormat();
						switch (format) {
						case 0:
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 1:
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						default:
						case 2:
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 3:
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(true);
							break;
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "axisNumberFormatMenu MenuListener.menuHidden " + evt); //$NON-NLS-1$
				}
			});

			this.axisNumberFormat0 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat0.setText(Messages.getString(MessageIds.OSDE_MSGT0106));
			this.axisNumberFormat0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat0 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(0);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat1 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat1.setText(Messages.getString(MessageIds.OSDE_MSGT0107));
			this.axisNumberFormat1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat1 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(1);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat2 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat2.setText(Messages.getString(MessageIds.OSDE_MSGT0108));
			this.axisNumberFormat2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat2 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(2);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat3 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat3.setText(Messages.getString(MessageIds.OSDE_MSGT0109));
			this.axisNumberFormat3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat3 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(3);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisPosition = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisPosition.setText(Messages.getString(MessageIds.OSDE_MSGT0110));
			this.axisPositionMenu = new Menu(this.axisPosition);
			this.axisPosition.setMenu(this.axisPositionMenu);
			this.axisPositionMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "axisPositionMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isLeft = CurveSelectorContextMenu.this.actualRecord.isPositionLeft();
						if (isLeft) {
							CurveSelectorContextMenu.this.axisPositionLeft.setSelection(true);
							CurveSelectorContextMenu.this.axisPositionRight.setSelection(false);
						}
						else {
							CurveSelectorContextMenu.this.axisPositionLeft.setSelection(false);
							CurveSelectorContextMenu.this.axisPositionRight.setSelection(true);
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "axisPositionMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.axisPositionLeft = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionLeft.setText(Messages.getString(MessageIds.OSDE_MSGT0111));
			this.axisPositionLeft.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisPositionLeft Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setPositionLeft(true);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisPositionRight = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionRight.setText(Messages.getString(MessageIds.OSDE_MSGT0112));
			this.axisPositionRight.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisPositionRight Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.actualRecord.setPositionLeft(false);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.timeGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.timeGrid.setText(Messages.getString(MessageIds.OSDE_MSGT0113));
			this.timeGridMenu = new Menu(this.timeGrid);
			this.timeGrid.setMenu(this.timeGridMenu);
			this.timeGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "timeGridMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int gridType = CurveSelectorContextMenu.this.recordSet.getTimeGridType();
						switch (gridType) {
						case RecordSet.TIME_GRID_MAIN:
							CurveSelectorContextMenu.this.timeGridOff.setSelection(false);
							CurveSelectorContextMenu.this.timeGridMain.setSelection(true);
							CurveSelectorContextMenu.this.timeGridMod60.setSelection(false);
							break;
						case RecordSet.TIME_GRID_MOD60:
							CurveSelectorContextMenu.this.timeGridOff.setSelection(false);
							CurveSelectorContextMenu.this.timeGridMain.setSelection(false);
							break;
						case RecordSet.TIME_GRID_NONE:
						default:
							CurveSelectorContextMenu.this.timeGridOff.setSelection(true);
							CurveSelectorContextMenu.this.timeGridMain.setSelection(false);
							CurveSelectorContextMenu.this.timeGridMod60.setSelection(false);
							break;
						}
					}
				}
				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "timeGridMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.timeGridOff = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridOff.setText(Messages.getString(MessageIds.OSDE_MSGT0114));
			this.timeGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridOff Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_NONE);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_NONE);
						}
					}
				}
			});
			this.timeGridMain = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMain.setText(Messages.getString(MessageIds.OSDE_MSGT0115));
			this.timeGridMain.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridMain Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_MAIN);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MAIN);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.timeGridMod60 = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMod60.setText(Messages.getString(MessageIds.OSDE_MSGT0116));
			this.timeGridMod60.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridMod60 Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_MOD60);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MOD60);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.timeGridColor = new MenuItem(this.timeGridMenu, SWT.PUSH);
			this.timeGridColor.setText(Messages.getString(MessageIds.OSDE_MSGT0117));
			this.timeGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridColor Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setTimeGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();

							if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
								CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
								if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
								CurveSelectorContextMenu.this.application.updateCompareWindow();
							}
						}
					}
				}
			});
//			timeGridLineStyle = new MenuItem(timeGridMenu, SWT.PUSH);
//			timeGridLineStyle.setText("Linientype");
//			timeGridLineStyle.setEnabled(false);
//			timeGridLineStyle.addListener(SWT.Selection, new Listener() {
//				public void handleEvent(Event e) {
//					log.log(Level.FINEST, "timeGridLineStyle Action performed!");
//					if (recordNameKey != null) {
//						recordSet.setTimeGridLineStyle(SWT.LINE_DOT); 
//						application.updateGraphicsWindow();
//					}
//				}
//			});
			
			this.horizontalGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.horizontalGrid.setText(Messages.getString(MessageIds.OSDE_MSGT0100));
			this.horizontalGridMenu = new Menu(this.horizontalGrid);
			this.horizontalGrid.setMenu(this.horizontalGridMenu);
			this.horizontalGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "horizontalGridMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.horizontalGridRecordName.setText(Messages.getString(MessageIds.OSDE_MSGT0118) + CurveSelectorContextMenu.this.recordSet.getHorizontalGridRecordName());
						int gridType = CurveSelectorContextMenu.this.recordSet.getHorizontalGridType();
						switch (gridType) {
						case RecordSet.HORIZONTAL_GRID_EVERY:
							CurveSelectorContextMenu.this.horizontalGridOff.setSelection(false);
							CurveSelectorContextMenu.this.horizontalGridEveryTick.setSelection(true);
							CurveSelectorContextMenu.this.horizontalGridEverySecond.setSelection(false);
							break;
						case RecordSet.HORIZONTAL_GRID_SECOND:
							CurveSelectorContextMenu.this.horizontalGridOff.setSelection(false);
							CurveSelectorContextMenu.this.horizontalGridEveryTick.setSelection(false);
							CurveSelectorContextMenu.this.horizontalGridEverySecond.setSelection(true);
							break;
						case RecordSet.HORIZONTAL_GRID_NONE:
						default:
							CurveSelectorContextMenu.this.horizontalGridOff.setSelection(true);
							CurveSelectorContextMenu.this.horizontalGridEveryTick.setSelection(false);
							CurveSelectorContextMenu.this.horizontalGridEverySecond.setSelection(false);
							break;
						}
					}
				}
				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "horizontalGridMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.horizontalGridRecordName = new MenuItem(this.horizontalGridMenu, SWT.NONE);

			new MenuItem(this.horizontalGridMenu, SWT.SEPARATOR);
			
			this.horizontalGridOff = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridOff.setText(Messages.getString(MessageIds.OSDE_MSGT0119));
			this.horizontalGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridOff Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_NONE);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_NONE);
						}
					}
				}
			});
			this.horizontalGridEveryTick = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridEveryTick.setText(Messages.getString(MessageIds.OSDE_MSGT0120));
			this.horizontalGridEveryTick.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridMain Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_EVERY);
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridRecordKey(CurveSelectorContextMenu.this.recordNameKey);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_EVERY);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.horizontalGridEverySecond = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridEverySecond.setText(Messages.getString(MessageIds.OSDE_MSGT0121));
			this.horizontalGridEverySecond.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridMod60 Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_SECOND);
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridRecordKey(CurveSelectorContextMenu.this.recordNameKey);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_SECOND);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.horizontalGridColor = new MenuItem(this.horizontalGridMenu, SWT.PUSH);
			this.horizontalGridColor.setText(Messages.getString(MessageIds.OSDE_MSGT0122));
			this.horizontalGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridColor Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setHorizontalGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
							
							if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE) {
								CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
								if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
								CurveSelectorContextMenu.this.application.updateCompareWindow();
							}
						}
					}
				}
			});
//			horizontalGridLineStyle = new MenuItem(horizontalGridMenu, SWT.PUSH);
//			horizontalGridLineStyle.setText("Linientype");
//			horizontalGridLineStyle.setEnabled(false);
//			horizontalGridLineStyle.addListener(SWT.Selection, new Listener() {
//				public void handleEvent(Event e) {
//					log.log(Level.FINEST, "horizontalGridLineStyle Action performed!");
//					if (recordNameKey != null) {
//						recordSet.setHorizontalGridLineStyle(SWT.LINE_DASH); 
//						application.updateGraphicsWindow();
//					}
//				}
//			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.measurement = new MenuItem(popupmenu, SWT.CASCADE);
			this.measurement.setText(Messages.getString(MessageIds.OSDE_MSGT0123));
			this.measurementMenu = new Menu(this.horizontalGrid);
			this.measurement.setMenu(this.measurementMenu);
			this.measurementMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "measurementMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.measurementRecordName.setText(Messages.getString(MessageIds.OSDE_MSGT0124) + CurveSelectorContextMenu.this.recordNameMeasurement);
					}
				}
				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "measurementMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.measurementRecordName = new MenuItem(this.measurementMenu, SWT.NONE);
			
			new MenuItem(this.measurementMenu, SWT.SEPARATOR);
			
			this.simpleMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.simpleMeasure.setText(Messages.getString(MessageIds.OSDE_MSGT0125));
			this.simpleMeasure.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "measure.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isRecordVisible) {
						CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
					if (isMeasurementWhileNameChanged(CurveSelectorContextMenu.this.recordNameKey) || CurveSelectorContextMenu.this.simpleMeasure.getSelection() == true) {
						CurveSelectorContextMenu.this.application.setMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, true);
						CurveSelectorContextMenu.this.simpleMeasure.setSelection(true);
						CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);
					}
					else {
						CurveSelectorContextMenu.this.application.setMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, false);
						CurveSelectorContextMenu.this.application.setStatusMessage(OSDE.STRING_EMPTY);
					}
				}
			});
			this.deltaMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.deltaMeasure.setText(Messages.getString(MessageIds.OSDE_MSGT0126));
			this.deltaMeasure.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "deltaMeasure.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (!CurveSelectorContextMenu.this.isRecordVisible) {
						CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
					if (isMeasurementWhileNameChanged(CurveSelectorContextMenu.this.recordNameKey) || CurveSelectorContextMenu.this.deltaMeasure.getSelection() == true) {
						CurveSelectorContextMenu.this.application.setDeltaMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, true);
						CurveSelectorContextMenu.this.deltaMeasure.setSelection(true);
						CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
					}
					else {
						CurveSelectorContextMenu.this.application.setDeltaMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, false);
						CurveSelectorContextMenu.this.application.setStatusMessage(OSDE.STRING_EMPTY);
					}
				}
			});

			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.copyCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.copyCurveCompare.setText(Messages.getString(MessageIds.OSDE_MSGT0127));
			this.copyCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "copyCurveCompare Action performed! " + e); //$NON-NLS-1$
					String oldRecordKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (oldRecordKey != null && CurveSelectorContextMenu.this.recordSet.get(oldRecordKey).isVisible()) {
						RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
						boolean isComparable = true;
						if (!compareSet.isEmpty() && compareSet.getTimeStep_ms() != CurveSelectorContextMenu.this.recordSet.getTimeStep_ms()) {
							CurveSelectorContextMenu.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0003));
							isComparable = false;
							return;
						}
						if (!compareSet.isEmpty() && !compareSet.getFirstRecordName().startsWith(oldRecordKey)) {
							CurveSelectorContextMenu.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0004, new Object[] {oldRecordKey, OSDE.STRING_MESSAGE_CONCAT, compareSet.getFirstRecordName().split(OSDE.STRING_UNDER_BAR)[0]}));
							isComparable = false;
							return;
						}
						if (compareSet.isEmpty() || isComparable) {
							// while adding a new curve to compare set - reset the zoom mode
							CurveSelectorContextMenu.this.application.setCompareWindowGraphicsMode(GraphicsWindow.MODE_RESET ,false);
							
							compareSet.setTimeStep_ms(CurveSelectorContextMenu.this.recordSet.getTimeStep_ms());
							String newRecordkey = oldRecordKey + OSDE.STRING_UNDER_BAR + compareSet.size();
							Record oldRecord = CurveSelectorContextMenu.this.recordSet.get(oldRecordKey);
							compareSet.put(newRecordkey, oldRecord.clone()); // will delete channelConfigKey
							Record newRecord = compareSet.get(newRecordkey);
							newRecord.setSourceRecordSetNames(CurveSelectorContextMenu.this.recordSet.getRecordNames());
							newRecord.setChannelConfigKey(oldRecord.getChannelConfigKey());
							newRecord.setVisible(true); // if a non visible record added
							newRecord.setName(newRecordkey);
							
							if (compareSet.size() == 1) {	//set grid line mode and color from settings (previous compare behavior)
								compareSet.setHorizontalGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalType());
								compareSet.setHorizontalGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalColor());
								compareSet.setTimeGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalType());
								compareSet.setTimeGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalColor());
								compareSet.setHorizontalGridRecordKey(newRecordkey);
							}
							int maxRecordSize = compareSet.getMaxSize();
							for (String recordKey : compareSet.keySet()) {
								if (compareSet.get(recordKey).realSize() > maxRecordSize) {
									compareSet.setMaxSize(compareSet.get(recordKey).realSize());
								}
							}
							log.log(Level.FINE, " adapt compare set maxRecordSize = " + maxRecordSize); //$NON-NLS-1$
							
							double oldMinValue = compareSet.getMinValue();
							double oldMaxValue = compareSet.getMaxValue();
							log.log(Level.FINE, String.format("scale values from compare set min=%.3f max=%.3f", oldMinValue, oldMaxValue)); //$NON-NLS-1$
							for (String recordKey : compareSet.keySet()) {
								double newMinValue = compareSet.get(recordKey).getMinScaleValue();
								double newMaxValue = compareSet.get(recordKey).getMaxScaleValue();
								log.log(Level.FINE, String.format("scale values from record (" + recordKey + ") to be checked min=%.3f max=%.3f", newMinValue, newMaxValue)); //$NON-NLS-1$ //$NON-NLS-2$

								if (newMinValue < oldMinValue) {
									compareSet.setMinValue(newMinValue); // store new min value into record set
								}
								oldMinValue = compareSet.getMinValue();
								if (newMaxValue > oldMaxValue) {
									compareSet.setMaxValue(newMaxValue); // store new max value into record set
								}
							}
							for (String minRecordKey : compareSet.keySet()) { // loop through all and make equal
								compareSet.get(minRecordKey).setStartEndDefined(true, compareSet.getMinValue(), compareSet.getMaxValue());
							}

							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
					else if( oldRecordKey != null) CurveSelectorContextMenu.this.application.openMessageDialog(Messages.getString(MessageIds.OSDE_MSGW0005));
				}
			});
			this.cleanCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.cleanCurveCompare.setText(Messages.getString(MessageIds.OSDE_MSGT0128));
			this.cleanCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "cleanCurveCompare Action performed! " + e); //$NON-NLS-1$
					RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
					compareSet.clear();
					CurveSelectorContextMenu.this.application.updateCompareWindow();
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	void setAllEnabled(boolean enabled) {
		this.lineVisible.setEnabled(enabled);
		this.lineColor.setEnabled(enabled);
		this.lineWidth.setEnabled(enabled);
		this.lineType.setEnabled(enabled);
		this.axisEndValues.setEnabled(enabled);
		this.axisNumberFormat.setEnabled(enabled);
		this.axisPosition.setEnabled(enabled);
		this.timeGrid.setEnabled(enabled);
		this.horizontalGrid.setEnabled(enabled);
		this.measurement.setEnabled(enabled);
		this.copyCurveCompare.setEnabled(enabled);
		this.cleanCurveCompare.setEnabled(enabled);
	}

	/**
	 * check measurement record name and reset if changed
	 */
	boolean isMeasurementWhileNameChanged(String tmpRecordNameMeasurement) {
		boolean isChanged = false;
		if (!this.recordNameMeasurement.equals(tmpRecordNameMeasurement) && this.recordNameMeasurement.length() > 1) {
			this.application.setMeasurementActive(this.recordNameMeasurement, false);
			this.application.setDeltaMeasurementActive(this.recordNameMeasurement, false);
			this.application.setStatusMessage(OSDE.STRING_EMPTY);
			isChanged = true;
		}
		this.recordNameMeasurement = tmpRecordNameMeasurement;
		return isChanged;
	}
}
