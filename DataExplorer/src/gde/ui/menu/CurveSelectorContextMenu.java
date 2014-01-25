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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014 Winfried Bruegmann
****************************************************************************************/
package gde.ui.menu;

import gde.log.Level;

import java.util.Map.Entry;
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

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.dialog.AxisEndValuesDialog;
import gde.ui.tab.GraphicsComposite;
import gde.ui.tab.GraphicsWindow;
import gde.utils.TimeLine;

/**
 * Context menu class of the curve selection window acts as popup menu
 * @author Winfried Brügmann
 */
public class CurveSelectorContextMenu {
	final static Logger									 log	= Logger.getLogger(CurveSelectorContextMenu.class.getName());

	Menu													menu;
	Menu													lineWidthMenu, lineTypeMenu, axisEndValuesMenu, axisNumberFormatMenu, axisPositionMenu, timeGridMenu, horizontalGridMenu, measurementMenu;
	MenuItem											recordName, lineVisible, lineColor, copyCurveCompare, cleanCurveCompare;
	MenuItem											lineWidth, lineWidthMenuItem1, lineWidthMenuItem2, lineWidthMenuItem3;
	MenuItem											lineType, lineTypeMenuItem1, lineTypeMenuItem2, lineTypeMenuItem3, smoothAtCurrentDropItem, smoothVoltageCurveItem;
	MenuItem											axisEndValues, axisEndAuto, axisEndRound, axisStarts0, axisEndManual;
	MenuItem											axisNumberFormat, axisNumberFormatAuto, axisNumberFormat0, axisNumberFormat1, axisNumberFormat2, axisNumberFormat3;
	MenuItem											axisPosition, axisPositionLeft, axisPositionRight;
	MenuItem 											measurement, measurementRecordName, simpleMeasure, deltaMeasure;
	MenuItem											timeGridColor, timeGrid, timeGridOff, timeGridMain, timeGridMod60;
	MenuItem											horizontalGridRecordName, horizontalGridColor, horizontalGrid, horizontalGridOff, horizontalGridEveryTick, horizontalGridEverySecond;

	RecordSet											recordSet;
	final DataExplorer						application;
	final Settings								settings = Settings.getInstance();
	AxisEndValuesDialog						axisEndValuesDialog;
	
	// states of initiated sub dialogs
	boolean												isActiveColorDialog = false;
	boolean												isActiveEnValueDialog = false;

	
	TableItem 										selectedItem;
	Record												actualRecord = null;
	boolean 											isRecordVisible = false;
	boolean 											isSmoothAtCurrentDrop = false;
	boolean 											isSmoothVoltageCurve = false;
	String 												recordNameKey = null;
	String 												recordNameMeasurement = GDE.STRING_BLANK;
	boolean 											isWindowTypeCompare = false;
	boolean 											isWindowTypeUtility = false;
	
	public CurveSelectorContextMenu() {
		super();
		this.application = DataExplorer.getInstance();
		this.axisEndValuesDialog = new AxisEndValuesDialog(this.application.getShell(), SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL);
	}

	public void createMenu(final Menu popupmenu) {
		this.menu = popupmenu;
		try {
			popupmenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "popupmenu MenuListener.menuShown " + evt); //$NON-NLS-1$
					CurveSelectorContextMenu.this.selectedItem = (TableItem) popupmenu.getData(DataExplorer.CURVE_SELECTION_ITEM);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						log.log(Level.FINER, CurveSelectorContextMenu.this.selectedItem.toString());
						if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
							CurveSelectorContextMenu.this.recordNameKey = CurveSelectorContextMenu.this.selectedItem.getText();
							log.log(Level.FINE, "===>>" + CurveSelectorContextMenu.this.recordNameKey);
							CurveSelectorContextMenu.this.isWindowTypeCompare = CurveSelectorContextMenu.this.application.isRecordSetVisible(GraphicsWindow.TYPE_COMPARE);
							CurveSelectorContextMenu.this.isWindowTypeUtility = CurveSelectorContextMenu.this.application.isRecordSetVisible(GraphicsWindow.TYPE_UTIL);
							CurveSelectorContextMenu.this.recordSet = CurveSelectorContextMenu.this.application.getRecordSetOfVisibleTab();

							if (CurveSelectorContextMenu.this.recordSet != null) {
								setAllEnabled(true);

								if (CurveSelectorContextMenu.this.recordNameKey != null && CurveSelectorContextMenu.this.recordNameKey.length() > 1) {
									CurveSelectorContextMenu.this.actualRecord = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey);
									if (CurveSelectorContextMenu.this.actualRecord != null) {
										CurveSelectorContextMenu.this.recordName.setText(">>>>  " + CurveSelectorContextMenu.this.recordNameKey + "  <<<<"); //$NON-NLS-1$ //$NON-NLS-2$
										CurveSelectorContextMenu.this.lineVisible.setText(Messages.getString(MessageIds.GDE_MSGT0085));
										CurveSelectorContextMenu.this.isRecordVisible = CurveSelectorContextMenu.this.actualRecord.isVisible();
										CurveSelectorContextMenu.this.lineVisible.setSelection(CurveSelectorContextMenu.this.isRecordVisible);
										CurveSelectorContextMenu.this.isSmoothAtCurrentDrop = CurveSelectorContextMenu.this.actualRecord.getParent().isSmoothAtCurrentDrop();
										CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setSelection(CurveSelectorContextMenu.this.isSmoothAtCurrentDrop);
										if (CurveSelectorContextMenu.this.recordSet.getDevice().getName().startsWith("Ultra")) {
											CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(true);
											CurveSelectorContextMenu.this.isSmoothVoltageCurve = CurveSelectorContextMenu.this.actualRecord.getParent().isSmoothVoltageCurve();
											CurveSelectorContextMenu.this.smoothVoltageCurveItem.setSelection(CurveSelectorContextMenu.this.isSmoothVoltageCurve);
										}
										else {
											CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(false);
											CurveSelectorContextMenu.this.smoothVoltageCurveItem.setSelection(false);
										}
										
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
								// check zoom mode
								if (CurveSelectorContextMenu.this.recordSet.isZoomMode()) {
									CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
									CurveSelectorContextMenu.this.axisEndValues.setText(Messages.getString(MessageIds.GDE_MSGT0083));
								}
								else {
									CurveSelectorContextMenu.this.axisEndValues.setText(Messages.getString(MessageIds.GDE_MSGT0084));
								}

								// check if record switched and measurement mode needs to be reset
								if (!CurveSelectorContextMenu.this.recordSet.isMeasurementMode(CurveSelectorContextMenu.this.recordNameMeasurement)
										&& !CurveSelectorContextMenu.this.recordSet.isDeltaMeasurementMode(CurveSelectorContextMenu.this.recordNameMeasurement)) {
									CurveSelectorContextMenu.this.recordNameMeasurement = GDE.STRING_BLANK;
									CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
									CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);

								}

								// compare window has fixed defined scale end values
								if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
									CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setEnabled(false);
									if (CurveSelectorContextMenu.this.smoothVoltageCurveItem != null) 
										CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(false);
									CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
									CurveSelectorContextMenu.this.axisPosition.setEnabled(false);
									CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
								}

								// utility window
								if (CurveSelectorContextMenu.this.isWindowTypeUtility) {
									CurveSelectorContextMenu.this.smoothAtCurrentDropItem.setEnabled(false);
									if (CurveSelectorContextMenu.this.smoothVoltageCurveItem != null)
										CurveSelectorContextMenu.this.smoothVoltageCurveItem.setEnabled(false);
									CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
									CurveSelectorContextMenu.this.measurement.setEnabled(false);
								}

								// disable clear, if nothing to clear
								if (CurveSelectorContextMenu.this.application.getCompareSet().size() == 0) {
									CurveSelectorContextMenu.this.cleanCurveCompare.setEnabled(false);
								}
							}
							else
								setAllEnabled(false);
						}
					}
					else {
						CurveSelectorContextMenu.this.recordName.setText(">>>>  " + Messages.getString(MessageIds.GDE_MSGT0408) + "  <<<<"); //$NON-NLS-1$ //$NON-NLS-2$
						setAllEnabled(false);
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "popupmenu MenuListener.menuHidden " + evt); //$NON-NLS-1$
				}
			});
			popupmenu.addListener(SWT.FocusOut, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "widgetDisposed Action performed! " + e); //$NON-NLS-1$
					CurveSelectorContextMenu.this.menu.setData(DataExplorer.RECORD_NAME, null);
					CurveSelectorContextMenu.this.menu.setData(DataExplorer.CURVE_SELECTION_ITEM, null);
				}
			});
			this.recordName = new MenuItem(popupmenu, SWT.None);

			new MenuItem(popupmenu, SWT.SEPARATOR);
			
			this.lineVisible = new MenuItem(popupmenu, SWT.CHECK);
			this.lineVisible.setText(Messages.getString(MessageIds.GDE_MSGT0085));
			this.lineVisible.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineVisible selected evt=" + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null) {
						boolean checked = CurveSelectorContextMenu.this.lineVisible.getSelection();
						CurveSelectorContextMenu.this.actualRecord.setVisible(checked);
						//CurveSelectorContextMenu.this.selectedItem.setChecked(checked);
						CurveSelectorContextMenu.this.recordSet.syncScaleOfSyncableRecords();
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}		
				}
			});

			this.lineColor = new MenuItem(popupmenu, SWT.PUSH);
			this.lineColor.setText(Messages.getString(MessageIds.GDE_MSGT0086));
			this.lineColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event evt) {
					log.log(Level.FINER, "lineColor performed! " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.isActiveColorDialog = true;
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							Color color = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
							CurveSelectorContextMenu.this.selectedItem.setForeground(color);
							CurveSelectorContextMenu.this.actualRecord.setColor(color);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						}
						CurveSelectorContextMenu.this.isActiveColorDialog = false;
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.lineWidth = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineWidth.setText(Messages.getString(MessageIds.GDE_MSGT0087));
			this.lineWidthMenu = new Menu(this.lineWidth);
			this.lineWidth.setMenu(this.lineWidthMenu);
			this.lineWidthMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "lineWidthMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
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
			this.lineWidthMenuItem1.setImage(SWTResourceManager.getImage("gde/resource/LineWidth1.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem1.setText(Messages.getString(MessageIds.GDE_MSGT0088));
			this.lineWidthMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, Messages.getString(MessageIds.GDE_MSGT0089) + e); 
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
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
			this.lineWidthMenuItem2.setImage(SWTResourceManager.getImage("gde/resource/LineWidth2.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem2.setText(Messages.getString(MessageIds.GDE_MSGT0090));
			this.lineWidthMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, Messages.getString(MessageIds.GDE_MSGT0091) + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
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
			this.lineWidthMenuItem3.setImage(SWTResourceManager.getImage("gde/resource/LineWidth3.gif")); //$NON-NLS-1$
			this.lineWidthMenuItem3.setText(Messages.getString(MessageIds.GDE_MSGT0092));
			this.lineWidthMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, Messages.getString(MessageIds.GDE_MSGT0093) + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
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
			this.lineType.setText(Messages.getString(MessageIds.GDE_MSGT0094));
			this.lineTypeMenu = new Menu(this.lineType);
			this.lineType.setMenu(this.lineTypeMenu);
			this.lineTypeMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "lineTypeMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
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
			this.lineTypeMenuItem1.setImage(SWTResourceManager.getImage("gde/resource/LineType1.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem1.setText(Messages.getString(MessageIds.GDE_MSGT0095));
			this.lineTypeMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineTypeMenuItem1 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
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
			this.lineTypeMenuItem2.setImage(SWTResourceManager.getImage("gde/resource/LineType2.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem2.setText(Messages.getString(MessageIds.GDE_MSGT0096));
			this.lineTypeMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineTypeMenuItem2 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
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
			this.lineTypeMenuItem3.setImage(SWTResourceManager.getImage("gde/resource/LineType3.gif")); //$NON-NLS-1$
			this.lineTypeMenuItem3.setText(Messages.getString(MessageIds.GDE_MSGT0097));
			this.lineTypeMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "lineTypeMenuItem3 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setLineStyle(SWT.LINE_DOT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.smoothAtCurrentDropItem = new MenuItem(popupmenu, SWT.CHECK);
			this.smoothAtCurrentDropItem.setText(Messages.getString(MessageIds.GDE_MSGT0335));
			this.smoothAtCurrentDropItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "smoothAtCurrentDropItem selected evt=" + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null) {
						boolean checked = CurveSelectorContextMenu.this.smoothAtCurrentDropItem.getSelection();
						CurveSelectorContextMenu.this.recordSet.setSmoothAtCurrentDrop(checked);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.smoothVoltageCurveItem = new MenuItem(popupmenu, SWT.CHECK);
			this.smoothVoltageCurveItem.setText(Messages.getString(MessageIds.GDE_MSGT0685));
			this.smoothVoltageCurveItem.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "smoothVoltageCurveItem selected evt=" + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null) {
						boolean checked = CurveSelectorContextMenu.this.smoothVoltageCurveItem.getSelection();
						CurveSelectorContextMenu.this.recordSet.setSmoothVoltageCurve(checked);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.axisEndValues = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisEndValues.setText(Messages.getString(MessageIds.GDE_MSGT0098));
			this.axisEndValuesMenu = new Menu(this.axisEndValues);
			this.axisEndValues.setMenu(this.axisEndValuesMenu);
			this.axisEndValuesMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "axisEndValuesMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isRounded = CurveSelectorContextMenu.this.actualRecord.isRoundOut();
						boolean isStart0 = CurveSelectorContextMenu.this.actualRecord.isStartpointZero();
						boolean isManual = CurveSelectorContextMenu.this.actualRecord.isStartEndDefined();
						boolean isAuto = !isRounded && !isManual;
						CurveSelectorContextMenu.this.axisEndAuto.setSelection(isAuto);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(isRounded);
						CurveSelectorContextMenu.this.axisStarts0.setSelection(isStart0);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(isManual);
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "axisEndValuesMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.axisEndAuto = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndAuto.setText(Messages.getString(MessageIds.GDE_MSGT0099));
			this.axisEndAuto.setSelection(true);
			this.axisEndAuto.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisEndAuto.SelectionListener = " + e);
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setRoundOut(false);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndRound = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndRound.setText(Messages.getString(MessageIds.GDE_MSGT0101));
			this.axisEndRound.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisEndRound.SelectionListener = " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
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
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisStarts0 = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisStarts0.setText(Messages.getString(MessageIds.GDE_MSGT0103));
			this.axisStarts0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisStarts0 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						if (CurveSelectorContextMenu.this.axisStarts0.getSelection()) { // true
							CurveSelectorContextMenu.this.actualRecord.setStartpointZero(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.actualRecord.setStartpointZero(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.actualRecord.setStartEndDefined(false, 0, 0);
						}
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndManual = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndManual.setText(Messages.getString(MessageIds.GDE_MSGT0104));
			this.axisEndManual.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisEndManual Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.isActiveEnValueDialog = true;
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
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_END_VALUES);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.isActiveEnValueDialog = false;
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisNumberFormat = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisNumberFormat.setText(Messages.getString(MessageIds.GDE_MSGT0105));
			this.axisNumberFormatMenu = new Menu(this.axisNumberFormat);
			this.axisNumberFormat.setMenu(this.axisNumberFormatMenu);
			this.axisNumberFormatMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "axisNumberFormatMenu MenuListener.menuShown " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int format = CurveSelectorContextMenu.this.actualRecord.getNumberFormat();
						switch (format) {
						case -1:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 0:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 1:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						default:
						case 2:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat0.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat1.setSelection(false);
							CurveSelectorContextMenu.this.axisNumberFormat2.setSelection(true);
							CurveSelectorContextMenu.this.axisNumberFormat3.setSelection(false);
							break;
						case 3:
							CurveSelectorContextMenu.this.axisNumberFormatAuto.setSelection(false);
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

			this.axisNumberFormatAuto = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormatAuto.setText(Messages.getString(MessageIds.GDE_MSGT0099));
			this.axisNumberFormatAuto.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormatAuto " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(-1);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat0 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat0.setText(Messages.getString(MessageIds.GDE_MSGT0106));
			this.axisNumberFormat0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat0 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(0);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat1 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat1.setText(Messages.getString(MessageIds.GDE_MSGT0107));
			this.axisNumberFormat1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat1 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(1);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat2 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat2.setText(Messages.getString(MessageIds.GDE_MSGT0108));
			this.axisNumberFormat2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat2 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(2);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat3 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat3.setText(Messages.getString(MessageIds.GDE_MSGT0109));
			this.axisNumberFormat3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisNumberFormat3 " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setNumberFormat(3);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_NUMBER_FORMAT);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisPosition = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisPosition.setText(Messages.getString(MessageIds.GDE_MSGT0110));
			this.axisPositionMenu = new Menu(this.axisPosition);
			this.axisPosition.setMenu(this.axisPositionMenu);
			this.axisPositionMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "axisPositionMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
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
			this.axisPositionLeft.setText(Messages.getString(MessageIds.GDE_MSGT0111));
			this.axisPositionLeft.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisPositionLeft Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setPositionLeft(true);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_SCALE_POSITION);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisPositionRight = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionRight.setText(Messages.getString(MessageIds.GDE_MSGT0112));
			this.axisPositionRight.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "axisPositionRight Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.actualRecord.setPositionLeft(false);
						actualRecord.getParent().syncMasterSlaveRecords(actualRecord, Record.TYPE_AXIS_SCALE_POSITION);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.timeGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.timeGrid.setText(Messages.getString(MessageIds.GDE_MSGT0113));
			this.timeGridMenu = new Menu(this.timeGrid);
			this.timeGrid.setMenu(this.timeGridMenu);
			this.timeGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "timeGridMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
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
			this.timeGridOff.setText(Messages.getString(MessageIds.GDE_MSGT0114));
			this.timeGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridOff Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_NONE);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_NONE);
						}
					}
				}
			});
			this.timeGridMain = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMain.setText(Messages.getString(MessageIds.GDE_MSGT0115));
			this.timeGridMain.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridMain Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_MAIN);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MAIN);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.timeGridMod60 = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMod60.setText(Messages.getString(MessageIds.GDE_MSGT0116));
			this.timeGridMod60.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridMod60 Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_MOD60);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MOD60);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.timeGridColor = new MenuItem(this.timeGridMenu, SWT.PUSH);
			this.timeGridColor.setText(Messages.getString(MessageIds.GDE_MSGT0117));
			this.timeGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "timeGridColor Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setTimeGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();

							if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
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
			this.horizontalGrid.setText(Messages.getString(MessageIds.GDE_MSGT0100));
			this.horizontalGridMenu = new Menu(this.horizontalGrid);
			this.horizontalGrid.setMenu(this.horizontalGridMenu);
			this.horizontalGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "horizontalGridMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.horizontalGridRecordName.setText(Messages.getString(MessageIds.GDE_MSGT0118) 
								+ CurveSelectorContextMenu.this.recordSet.get(CurveSelectorContextMenu.this.recordSet.getHorizontalGridRecordOrdinal()).getName());
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
			this.horizontalGridOff.setText(Messages.getString(MessageIds.GDE_MSGT0119));
			this.horizontalGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridOff Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_NONE);
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_NONE);
						}
					}
				}
			});
			this.horizontalGridEveryTick = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridEveryTick.setText(Messages.getString(MessageIds.GDE_MSGT0120));
			this.horizontalGridEveryTick.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridMain Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_EVERY);
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridRecordOrdinal(CurveSelectorContextMenu.this.recordSet.isCompareSet() ? 0 : CurveSelectorContextMenu.this.actualRecord.getOrdinal());
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_EVERY);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.horizontalGridEverySecond = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridEverySecond.setText(Messages.getString(MessageIds.GDE_MSGT0121));
			this.horizontalGridEverySecond.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridMod60 Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_SECOND);
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridRecordOrdinal(CurveSelectorContextMenu.this.actualRecord.getOrdinal());
						if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
						CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();

						if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_SECOND);
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
				}
			});
			this.horizontalGridColor = new MenuItem(this.horizontalGridMenu, SWT.PUSH);
			this.horizontalGridColor.setText(Messages.getString(MessageIds.GDE_MSGT0122));
			this.horizontalGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "horizontalGridColor Action performed! " + e); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.recordSet != null && CurveSelectorContextMenu.this.actualRecord != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setHorizontalGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							if (!CurveSelectorContextMenu.this.isRecordVisible) CurveSelectorContextMenu.this.actualRecord.setVisible(true);
							CurveSelectorContextMenu.this.recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
							
							if (CurveSelectorContextMenu.this.isWindowTypeCompare) {
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
			this.measurement.setText(Messages.getString(MessageIds.GDE_MSGT0123));
			this.measurementMenu = new Menu(this.horizontalGrid);
			this.measurement.setMenu(this.measurementMenu);
			this.measurementMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.log(Level.FINEST, "measurementMenu MenuListener " + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.measurementRecordName.setText(Messages.getString(MessageIds.GDE_MSGT0124) + CurveSelectorContextMenu.this.recordNameMeasurement);
					}
				}
				public void menuHidden(MenuEvent evt) {
					log.log(Level.FINEST, "measurementMenu MenuListener " + evt); //$NON-NLS-1$
				}
			});

			this.measurementRecordName = new MenuItem(this.measurementMenu, SWT.NONE);
			
			new MenuItem(this.measurementMenu, SWT.SEPARATOR);
			
			this.simpleMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.simpleMeasure.setText(Messages.getString(MessageIds.GDE_MSGT0125));
			this.simpleMeasure.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "measure.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && !CurveSelectorContextMenu.this.isRecordVisible) {
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
						CurveSelectorContextMenu.this.application.setStatusMessage(GDE.STRING_EMPTY);
					}
				}
			});
			this.deltaMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.deltaMeasure.setText(Messages.getString(MessageIds.GDE_MSGT0126));
			this.deltaMeasure.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "deltaMeasure.widgetSelected, event=" + evt); //$NON-NLS-1$
					if (CurveSelectorContextMenu.this.actualRecord != null && !CurveSelectorContextMenu.this.isRecordVisible) {
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
						CurveSelectorContextMenu.this.application.setStatusMessage(GDE.STRING_EMPTY);
					}
				}
			});

			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.copyCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.copyCurveCompare.setText(Messages.getString(MessageIds.GDE_MSGT0127));
			this.copyCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.log(Level.FINEST, "copyCurveCompare Action performed! " + e); //$NON-NLS-1$
					CurveSelectorContextMenu.this.application.createCompareWindowTabItem(); // if ot already exist
					String copyFromRecordKey = (String) popupmenu.getData(DataExplorer.RECORD_NAME);
					RecordSet copyFromRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
					if (copyFromRecordSet != null && copyFromRecordKey != null) {
						Record copyFromRecord = copyFromRecordSet.get(copyFromRecordKey);
						if (copyFromRecord != null && copyFromRecord.isVisible()) {
							RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
							if (!compareSet.isEmpty() && !compareSet.get(compareSet.getFirstRecordName()).getUnit().equalsIgnoreCase(copyFromRecord.getUnit())) {
								CurveSelectorContextMenu.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0004, new Object[] { copyFromRecordKey + GDE.STRING_MESSAGE_CONCAT
										+ compareSet.getFirstRecordName() }));
								return;
							}
							if (compareSet.size() > 0) {
								// while adding a new curve to compare set - reset the zoom mode
								CurveSelectorContextMenu.this.application.setCompareWindowGraphicsMode(GraphicsComposite.MODE_RESET, false);
							}
							
							String newRecordkey = copyFromRecord.getChannelConfigKey() +  GDE.STRING_UNDER_BAR + copyFromRecordKey + GDE.STRING_UNDER_BAR + compareSet.size();
							Record newRecord = compareSet.put(newRecordkey, copyFromRecord.clone()); // will delete channelConfigKey
							newRecord.setOrdinal(copyFromRecord.getOrdinal());
							newRecord.setDescription(copyFromRecordSet.getRecordSetDescription());
							newRecord.setVisible(true); // if a non visible record added

							if (compareSet.size() == 1) { //set grid line mode and color from settings (previous compare behavior)
								compareSet.setHorizontalGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalType());
								compareSet.setHorizontalGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalColor());
								compareSet.setTimeGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalType());
								compareSet.setTimeGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalColor());
								compareSet.setHorizontalGridRecordOrdinal(0); 
							}
							// check if the new added record exceeds the existing one in time or set draw limit and pad with dummy points
							double maxRecordTime_ms = compareSet.getCompareSetMaxScaleTime_ms();
							double newRecordMaxTime_ms = newRecord.getMaxTime_ms();
							newRecord.setCompareSetDrawLimit_ms(newRecordMaxTime_ms);

							if (newRecordMaxTime_ms > maxRecordTime_ms) {
								compareSet.setCompareSetMaxScaleTime_ms(newRecordMaxTime_ms);
								maxRecordTime_ms = newRecordMaxTime_ms;
								log.log(Level.FINE, "adapt compareSet maxRecordTime_sec = " + TimeLine.getFomatedTimeWithUnit(maxRecordTime_ms)); //$NON-NLS-1$

								// new added record exceed size of existing, existing needs draw limit to be updated and to be padded
								for (Entry<String, Record> tmpRecordEntry : compareSet.entrySet()) {
									if (!newRecordkey.equals(tmpRecordEntry.getKey())) {
										Record tmpRecord = tmpRecordEntry.getValue();
										if (tmpRecord.getCompareSetDrawLimit_ms() == Integer.MAX_VALUE // draw linit untouched
												|| tmpRecord.getCompareSetDrawLimit_ms() < newRecordMaxTime_ms) {
											double avgTimeStep_ms = tmpRecord.getAverageTimeStep_ms();
											int steps = (int) ((maxRecordTime_ms - tmpRecord.getCompareSetDrawLimit_ms()) / avgTimeStep_ms);
											int value = tmpRecord.lastElement();
											double timeStep = newRecord.getLastTime_ms();
											for (int i = 0; i < steps; i++) {
												tmpRecord.add(value, timeStep += avgTimeStep_ms);
											}
											tmpRecord.setDrawTimeWidth(newRecordMaxTime_ms);
										}
									}
								}
							}
							else { // new record is shorter and needs to be padded and the draw limit to set
								double avgTimeStep_ms = newRecord.getAverageTimeStep_ms();
								int steps = (int) ((maxRecordTime_ms - newRecordMaxTime_ms) / avgTimeStep_ms);
								int value = newRecord.lastElement();
								double timeStep = newRecord.getLastTime_ms();
								for (int i = 0; i < steps; i++) {
									newRecord.add(value, timeStep += avgTimeStep_ms);
								}
								newRecord.setDrawTimeWidth(maxRecordTime_ms);
							}

							double oldMinValue = compareSet.getMinValue();
							double oldMaxValue = compareSet.getMaxValue();
							log.log(Level.FINE, String.format("scale values from compare set min=%.3f max=%.3f", oldMinValue, oldMaxValue)); //$NON-NLS-1$
							for (Record record : compareSet.values()) {
								double newMinValue = record.getMinScaleValue();
								double newMaxValue = record.getMaxScaleValue();
								log.log(Level.FINE, String.format("scale values from record (" + record.getName() + ") to be checked min=%.3f max=%.3f", newMinValue, newMaxValue)); //$NON-NLS-1$ //$NON-NLS-2$

								if (newMinValue < oldMinValue) {
									compareSet.setMinValue(newMinValue); // store new min value into record set
								}
								oldMinValue = compareSet.getMinValue();
								if (newMaxValue > oldMaxValue) {
									compareSet.setMaxValue(newMaxValue); // store new max value into record set
								}
							}
							for (Entry<String, Record> entry : compareSet.entrySet()) {
								entry.getValue().setStartEndDefined(true, compareSet.getMinValue(), compareSet.getMaxValue());
							}

							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
						else 
							CurveSelectorContextMenu.this.application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGW0005));
						
						//TODO check, why this is required before zoom operation ?
						CurveSelectorContextMenu.this.application.setCompareWindowGraphicsMode(GraphicsComposite.MODE_RESET, false);
					}
				}
			});
			this.cleanCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.cleanCurveCompare.setText(Messages.getString(MessageIds.GDE_MSGT0128));
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
		this.smoothAtCurrentDropItem.setEnabled(enabled); 
		if (this.smoothVoltageCurveItem != null) this.smoothVoltageCurveItem.setEnabled(enabled);
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
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			isChanged = true;
		}
		this.recordNameMeasurement = tmpRecordNameMeasurement;
		return isChanged;
	}
	
	/**
	 * query the state of context menu and sub dialogs
	 * @return inactive state true/false
	 */
	public boolean isActive() {
		boolean isActive = (this.menu != null && this.menu.isVisible()) || this.isActiveEnValueDialog || this.isActiveColorDialog;
		log.log(Level.FINE, "isActive = " + isActive);
		return isActive;
	}

}
