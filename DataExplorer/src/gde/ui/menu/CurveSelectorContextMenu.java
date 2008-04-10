/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.menu;

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

import osde.config.Settings;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
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
	boolean 											isRecordVisible = false;
	String 												recordNameKey = " ";
	String 												recordNameMeasurement = " ";
	int 													windowType = GraphicsWindow.TYPE_NORMAL;

	public CurveSelectorContextMenu() {
		super();
		this.application = OpenSerialDataExplorer.getInstance();
		this.axisEndValuesDialog = new AxisEndValuesDialog(OpenSerialDataExplorer.getInstance().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	public void createMenu(final Menu popupmenu) {
		try {
			popupmenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("popupmenu MenuListener.menuShown " + evt);
					CurveSelectorContextMenu.this.selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.recordNameKey = CurveSelectorContextMenu.this.selectedItem.getText();
						CurveSelectorContextMenu.this.windowType = (Integer) CurveSelectorContextMenu.this.selectedItem.getData(GraphicsWindow.WINDOW_TYPE);
						CurveSelectorContextMenu.this.recordSet = (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : CurveSelectorContextMenu.this.application.getCompareSet();

						if (CurveSelectorContextMenu.this.recordSet != null) {
							setAllEnabled(true);						
							
							if (CurveSelectorContextMenu.this.recordNameKey != null && CurveSelectorContextMenu.this.recordNameKey.length() > 1) {
								CurveSelectorContextMenu.this.recordName.setText(">>>>  " + CurveSelectorContextMenu.this.recordNameKey + "  <<<<");
								CurveSelectorContextMenu.this.setRecordVisible(CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).isVisible());
								CurveSelectorContextMenu.this.lineVisible.setSelection(isRecordVisible());
								// check measurement selections
								//deltaMeasure.setSelection(recordSet.isDeltaMeasurementMode(recordNameKey));
								//disable all menu items which makes only sense if record is visible
								if(!isRecordVisible()) {
									CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
								}
							}
							
							// check zoom mode
							if (CurveSelectorContextMenu.this.recordSet.isZoomMode()) {
								CurveSelectorContextMenu.this.axisEndValues.setEnabled(false);
								CurveSelectorContextMenu.this.axisEndValues.setText("Zoommode aktiv");
							}
							else {
								CurveSelectorContextMenu.this.axisEndValues.setEnabled(true);
								CurveSelectorContextMenu.this.axisEndValues.setText("Achsen-Endwerte");
							}
							
							// check if record switched and measurement mode needs to be reset
							if (!CurveSelectorContextMenu.this.recordSet.isMeasurementMode(CurveSelectorContextMenu.this.recordNameMeasurement) && !CurveSelectorContextMenu.this.recordSet.isDeltaMeasurementMode(CurveSelectorContextMenu.this.recordNameMeasurement)) {
								CurveSelectorContextMenu.this.recordNameMeasurement = " ";
								CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
								CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);

							}
														
							if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
								CurveSelectorContextMenu.this.copyCurveCompare.setEnabled(false);
							}
							
							// disable clear, if nothing to clear
							if (CurveSelectorContextMenu.this.application.getCompareSet().size() == 0)
								CurveSelectorContextMenu.this.cleanCurveCompare.setEnabled(false);
						}
						else
							setAllEnabled(false);
					}
				}

				public void menuHidden(MenuEvent evt) {
					log.finest("popupmenu MenuListener.menuHidden " + evt);
				}
			});
			this.recordName = new MenuItem(popupmenu, SWT.None);

			new MenuItem(popupmenu, SWT.SEPARATOR);
			
			this.lineVisible = new MenuItem(popupmenu, SWT.CHECK);
			this.lineVisible.setText("Kurve sichtbar");
			this.lineVisible.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineVisibler Action performed! " + e);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						if (CurveSelectorContextMenu.this.lineVisible.getSelection()) { // true
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
							CurveSelectorContextMenu.this.selectedItem.setChecked(true);
						}
						else { // false
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(false);
							CurveSelectorContextMenu.this.selectedItem.setChecked(false);
						}
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.lineColor = new MenuItem(popupmenu, SWT.PUSH);
			this.lineColor.setText("KurvenLinienFarbe");
			this.lineColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event evt) {
					log.finest("lineColor performed! " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							Color color = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
							CurveSelectorContextMenu.this.selectedItem.setForeground(color);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setColor(color);
							if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						}
					}
				}
			});
			this.lineWidth = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineWidth.setText("KurvenLinienDicke");
			this.lineWidthMenu = new Menu(this.lineWidth);
			this.lineWidth.setMenu(this.lineWidthMenu);
			this.lineWidthMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("lineWidthMenu MenuListener " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int width = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).getLineWidth();
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
					log.finest("lineWidthMenu MenuListener " + evt);
				}
			});

			this.lineWidthMenuItem1 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.gif"));
			this.lineWidthMenuItem1.setText("  1");
			this.lineWidthMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 1 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setLineWidth(1);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
					}
				}
			});
			this.lineWidthMenuItem2 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineWidth2.gif"));
			this.lineWidthMenuItem2.setText("  2");
			this.lineWidthMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 2 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setLineWidth(2);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem3.setSelection(false);
					}
				}
			});
			this.lineWidthMenuItem3 = new MenuItem(this.lineWidthMenu, SWT.CHECK);
			this.lineWidthMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineWidth3.gif"));
			this.lineWidthMenuItem3.setText("  3");
			this.lineWidthMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 3 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setLineWidth(3);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.lineWidthMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineWidthMenuItem2.setSelection(false);
					}
				}
			});

			this.lineType = new MenuItem(popupmenu, SWT.CASCADE);
			this.lineType.setText("KurvenLinienTyp");
			this.lineTypeMenu = new Menu(this.lineType);
			this.lineType.setMenu(this.lineTypeMenu);
			this.lineTypeMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("lineTypeMenu MenuListener " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int type = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).getLineStyle();
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
					log.finest("lineTypeMenu MenuListener " + evt);
				}
			});

			this.lineTypeMenuItem1 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineType1.gif"));
			this.lineTypeMenuItem1.setText("durchgezogen");
			this.lineTypeMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem1 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setLineStyle(SWT.LINE_SOLID);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
					}
				}
			});
			this.lineTypeMenuItem2 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineType2.gif"));
			this.lineTypeMenuItem2.setText("gestrichelt");
			this.lineTypeMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem2 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setLineStyle(SWT.LINE_DASH);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem3.setSelection(false);
					}
				}
			});
			this.lineTypeMenuItem3 = new MenuItem(this.lineTypeMenu, SWT.CHECK);
			this.lineTypeMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineType3.gif"));
			this.lineTypeMenuItem3.setText("gepunktet");
			this.lineTypeMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem3 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setLineStyle(SWT.LINE_DOT);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						CurveSelectorContextMenu.this.lineTypeMenuItem1.setSelection(false);
						CurveSelectorContextMenu.this.lineTypeMenuItem2.setSelection(false);
					}
				}
			});
			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.axisEndValues = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisEndValues.setText("Achsen-Endwerte");
			this.axisEndValuesMenu = new Menu(this.axisEndValues);
			this.axisEndValues.setMenu(this.axisEndValuesMenu);
			this.axisEndValuesMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisEndValuesMenu MenuListener " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isRounded = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).isRoundOut();
						boolean isStart0 = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).isStartpointZero();
						boolean isManual = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).isStartEndDefined();
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
					log.finest("axisEndValuesMenu MenuListener " + evt);
				}
			});

			this.axisEndAuto = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndAuto.setText("automatik");
			this.axisEndAuto.setSelection(true);
			this.axisEndAuto.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndAuto " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setRoundOut(false);
						CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartEndDefined(false, 0, 0);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndRound = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndRound.setText("gerundet");
			this.axisEndRound.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndRound " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						if (CurveSelectorContextMenu.this.axisEndRound.getSelection()) { //true
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setRoundOut(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(true);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setRoundOut(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartEndDefined(false, 0, 0);
						}
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisStarts0 = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisStarts0.setText("beginnt bei 0");
			this.axisStarts0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisStarts0 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						if (CurveSelectorContextMenu.this.axisStarts0.getSelection()) { // true
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartpointZero(true);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartEndDefined(false, 0, 0);
						}
						else { // false
							CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartpointZero(false);
							CurveSelectorContextMenu.this.axisEndManual.setSelection(false);
							CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setStartEndDefined(false, 0, 0);
						}
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisEndManual = new MenuItem(this.axisEndValuesMenu, SWT.CHECK);
			this.axisEndManual.setText("manuell");
			this.axisEndManual.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndManual Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.axisEndManual.setSelection(true);
						CurveSelectorContextMenu.this.axisEndAuto.setSelection(false);
						CurveSelectorContextMenu.this.axisStarts0.setSelection(false);
						Record record = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey);
						record.setStartpointZero(false);
						CurveSelectorContextMenu.this.axisEndRound.setSelection(false);
						record.setRoundOut(false);
						double[] oldMinMax = {record.getMinScaleValue(), record.getMaxScaleValue()};
						double[] newMinMax = CurveSelectorContextMenu.this.axisEndValuesDialog.open(oldMinMax);
						record.setStartEndDefined(true, newMinMax[0], newMinMax[1]);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisNumberFormat = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisNumberFormat.setText("Achsen-Zahlenformat");
			this.axisNumberFormatMenu = new Menu(this.axisNumberFormat);
			this.axisNumberFormat.setMenu(this.axisNumberFormatMenu);
			this.axisNumberFormatMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisNumberFormatMenu MenuListener.menuShown " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						int format = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).getNumberFormat();
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
					log.finest("axisNumberFormatMenu MenuListener.menuHidden " + evt);
				}
			});

			this.axisNumberFormat0 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat0.setText("0000");
			this.axisNumberFormat0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat0 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setNumberFormat(0);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat1 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat1.setText("000.0");
			this.axisNumberFormat1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat1 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setNumberFormat(1);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat2 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat2.setText("00.00");
			this.axisNumberFormat2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat2 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setNumberFormat(2);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisNumberFormat3 = new MenuItem(this.axisNumberFormatMenu, SWT.CHECK);
			this.axisNumberFormat3.setText("0.000");
			this.axisNumberFormat3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat3 " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setNumberFormat(3);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			this.axisPosition = new MenuItem(popupmenu, SWT.CASCADE);
			this.axisPosition.setText("Achsen-Position");
			this.axisPositionMenu = new Menu(this.axisPosition);
			this.axisPosition.setMenu(this.axisPositionMenu);
			this.axisPositionMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisPositionMenu MenuListener " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						boolean isLeft = CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).isPositionLeft();
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
					log.finest("axisPositionMenu MenuListener " + evt);
				}
			});

			this.axisPositionLeft = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionLeft.setText("linke Seite");
			this.axisPositionLeft.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisPositionLeft Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setPositionLeft(true);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});
			this.axisPositionRight = new MenuItem(this.axisPositionMenu, SWT.CHECK);
			this.axisPositionRight.setText("rechte Seite");
			this.axisPositionRight.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisPositionRight Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setPositionLeft(false);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
				}
			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.timeGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.timeGrid.setText("Zeit-Grid(vertical)");
			this.timeGridMenu = new Menu(this.timeGrid);
			this.timeGrid.setMenu(this.timeGridMenu);
			this.timeGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("timeGridMenu MenuListener " + evt);
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
					log.finest("timeGridMenu MenuListener " + evt);
				}
			});

			this.timeGridOff = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridOff.setText("aus");
			this.timeGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridOff Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_NONE);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_NONE);
						}
					}
				}
			});
			this.timeGridMain = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMain.setText("jede Zeitmarke");
			this.timeGridMain.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridMain Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_MAIN);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MAIN);
						}
					}
				}
			});
			this.timeGridMod60 = new MenuItem(this.timeGridMenu, SWT.CHECK);
			this.timeGridMod60.setText("jede Zeitmarke mod 60");
			this.timeGridMod60.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridMod60 Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setTimeGridType(RecordSet.TIME_GRID_MOD60);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalType(RecordSet.TIME_GRID_MOD60);
						}
					}
				}
			});
			this.timeGridColor = new MenuItem(this.timeGridMenu, SWT.PUSH);
			this.timeGridColor.setText("Linienfarbe");
			this.timeGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridColor Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setTimeGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
							if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
								CurveSelectorContextMenu.this.settings.setGridCompareWindowVerticalColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
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
//					log.finest("timeGridLineStyle Action performed!");
//					if (recordNameKey != null) {
//						recordSet.setTimeGridLineStyle(SWT.LINE_DOT); 
//						application.updateGraphicsWindow();
//					}
//				}
//			});
			
			this.horizontalGrid = new MenuItem(popupmenu, SWT.CASCADE);
			this.horizontalGrid.setText("Kurven-Grid(horizontal)");
			this.horizontalGridMenu = new Menu(this.horizontalGrid);
			this.horizontalGrid.setMenu(this.horizontalGridMenu);
			this.horizontalGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("horizontalGridMenu MenuListener " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.horizontalGridRecordName.setText("gesetzt -> " + CurveSelectorContextMenu.this.recordSet.getHorizontalGridRecordName());
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
					log.finest("horizontalGridMenu MenuListener " + evt);
				}
			});

			this.horizontalGridRecordName = new MenuItem(this.horizontalGridMenu, SWT.NONE);

			new MenuItem(this.horizontalGridMenu, SWT.SEPARATOR);
			
			this.horizontalGridOff = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridOff.setText("aus");
			this.horizontalGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridOff Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_NONE);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_NONE);
						}
					}
				}
			});
			this.horizontalGridEveryTick = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridEveryTick.setText("jede Zeitmarke");
			this.horizontalGridEveryTick.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridMain Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_EVERY);
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridRecordKey(CurveSelectorContextMenu.this.recordNameKey);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_EVERY);
						}
					}
				}
			});
			this.horizontalGridEverySecond = new MenuItem(this.horizontalGridMenu, SWT.CHECK);
			this.horizontalGridEverySecond.setText("jede zweite Zeitmarke");
			this.horizontalGridEverySecond.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridMod60 Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_SECOND);
						CurveSelectorContextMenu.this.recordSet.setHorizontalGridRecordKey(CurveSelectorContextMenu.this.recordNameKey);
						if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
						if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
							CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalType(RecordSet.HORIZONTAL_GRID_SECOND);
						}
					}
				}
			});
			this.horizontalGridColor = new MenuItem(this.horizontalGridMenu, SWT.PUSH);
			this.horizontalGridColor.setText("Linienfarbe");
			this.horizontalGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridColor Action performed! " + e);
					if (CurveSelectorContextMenu.this.recordNameKey != null) {
						RGB rgb = CurveSelectorContextMenu.this.application.openColorDialog();
						if (rgb != null) {
							CurveSelectorContextMenu.this.recordSet.setHorizontalGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							if (!isRecordVisible()) CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
							CurveSelectorContextMenu.this.application.updateGraphicsWindow();
							
							if (CurveSelectorContextMenu.this.windowType == GraphicsWindow.TYPE_COMPARE){
								CurveSelectorContextMenu.this.settings.setGridCompareWindowHorizontalColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
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
//					log.finest("horizontalGridLineStyle Action performed!");
//					if (recordNameKey != null) {
//						recordSet.setHorizontalGridLineStyle(SWT.LINE_DASH); 
//						application.updateGraphicsWindow();
//					}
//				}
//			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.measurement = new MenuItem(popupmenu, SWT.CASCADE);
			this.measurement.setText("Kurven vermessen");
			this.measurementMenu = new Menu(this.horizontalGrid);
			this.measurement.setMenu(this.measurementMenu);
			this.measurementMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("measurementMenu MenuListener " + evt);
					if (CurveSelectorContextMenu.this.selectedItem != null && !CurveSelectorContextMenu.this.selectedItem.isDisposed()) {
						CurveSelectorContextMenu.this.measurementRecordName.setText("gesetzt -> " + CurveSelectorContextMenu.this.recordNameMeasurement);
					}
				}
				public void menuHidden(MenuEvent evt) {
					log.finest("measurementMenu MenuListener " + evt);
				}
			});

			this.measurementRecordName = new MenuItem(this.measurementMenu, SWT.NONE);
			
			new MenuItem(this.measurementMenu, SWT.SEPARATOR);
			
			this.simpleMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.simpleMeasure.setText("Kurvenpunkt messen");
			this.simpleMeasure.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.finest("measure.widgetSelected, event=" + evt);
					if (!isRecordVisible()) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
					if (isMeasurementWhileNameChanged(CurveSelectorContextMenu.this.recordNameKey) || CurveSelectorContextMenu.this.simpleMeasure.getSelection() == true) {
						CurveSelectorContextMenu.this.application.setMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, true);
						CurveSelectorContextMenu.this.simpleMeasure.setSelection(true);
						CurveSelectorContextMenu.this.deltaMeasure.setSelection(false);
					}
					else {
						CurveSelectorContextMenu.this.application.setMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, false);
						CurveSelectorContextMenu.this.application.setStatusMessage("");
					}
				}
			});
			this.deltaMeasure = new MenuItem(this.measurementMenu, SWT.CHECK);
			this.deltaMeasure.setText("Punktdifferenz messen");
			this.deltaMeasure.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.finest("deltaMeasure.widgetSelected, event=" + evt);
					if (!isRecordVisible()) {
						CurveSelectorContextMenu.this.recordSet.getRecord(CurveSelectorContextMenu.this.recordNameKey).setVisible(true);
						CurveSelectorContextMenu.this.application.updateGraphicsWindow();
					}
					if (isMeasurementWhileNameChanged(CurveSelectorContextMenu.this.recordNameKey) || CurveSelectorContextMenu.this.deltaMeasure.getSelection() == true) {
						CurveSelectorContextMenu.this.application.setDeltaMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, true);
						CurveSelectorContextMenu.this.deltaMeasure.setSelection(true);
						CurveSelectorContextMenu.this.simpleMeasure.setSelection(false);
					}
					else {
						CurveSelectorContextMenu.this.application.setDeltaMeasurementActive(CurveSelectorContextMenu.this.recordNameKey, false);
						CurveSelectorContextMenu.this.application.setStatusMessage("");
					}
				}
			});

			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			this.copyCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.copyCurveCompare.setText("Kopiere Kurvenvergleich");
			this.copyCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("copyCurveCompare Action performed! " + e);
					String oldRecordKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (oldRecordKey != null && CurveSelectorContextMenu.this.recordSet.get(oldRecordKey).isVisible()) {
						RecordSet compareSet = CurveSelectorContextMenu.this.application.getCompareSet();
						boolean isComparable = true;
						if (!compareSet.isEmpty() && compareSet.getTimeStep_ms() != CurveSelectorContextMenu.this.recordSet.getTimeStep_ms()) {
							CurveSelectorContextMenu.this.application.openMessageDialog("Zeitbasis der Kurven, die verglichen werden sollen passt nicht zusammen! Vermutlich wurden sie mit unterschiedlichen Geräten aufgenommen. Das ist zu Zeit noch nicht unterstützt, da hier eine Umrechnung auf eine einheitliche Zeitbasis stattfinden muss.");
							isComparable = false;
							return;
						}
						if (!compareSet.isEmpty() && !compareSet.getFirstRecordName().startsWith(oldRecordKey)) {
							CurveSelectorContextMenu.this.application.openMessageDialog("Type der Kurven (" + oldRecordKey + "-" + compareSet.getFirstRecordName().split("_")[0] + "), die verglichen werden sollen passen nicht zusammen!");
							isComparable = false;
							return;
						}
						if (compareSet.isEmpty() || isComparable) {
							// while adding a new curve to compare set - reset the zoom mode
							CurveSelectorContextMenu.this.application.setCompareWindowGraphicsMode(GraphicsWindow.MODE_RESET ,false);
							
							compareSet.setTimeStep_ms(CurveSelectorContextMenu.this.recordSet.getTimeStep_ms());
							String newRecordkey = oldRecordKey + "_" + compareSet.size();
							Record oldRecord = CurveSelectorContextMenu.this.recordSet.get(oldRecordKey);
							compareSet.put(newRecordkey, oldRecord.clone()); // will delete channelConfigKey
							Record newRecord = compareSet.get(newRecordkey);
							newRecord.setChannelConfigKey(oldRecord.getChannelConfigKey());
							newRecord.setName(newRecordkey);
							newRecord.setVisible(true); // if a non visible record added
							
							if (compareSet.size() == 1) {	//set grid line mode and color from settings (previous compare behavior)
								compareSet.setHorizontalGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalType());
								compareSet.setHorizontalGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowHorizontalColor());
								compareSet.setTimeGridType(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalType());
								compareSet.setTimeGridColor(CurveSelectorContextMenu.this.settings.getGridCompareWindowVerticalColor());
								compareSet.setHorizontalGridRecordKey(newRecordkey);
							}
							int maxRecordSize = compareSet.getMaxSize();
							double oldMinValue = compareSet.getMinValue();
							double oldMaxValue = compareSet.getMaxValue();
							log.fine(String.format("scale values from compare set min=%.3f max=%.3f", oldMinValue, oldMaxValue));
							for (String recordKey : compareSet.keySet()) {
								if (compareSet.get(recordKey).realSize() > maxRecordSize) {
									compareSet.setMaxSize(compareSet.get(recordKey).realSize());
								}
								double newMinValue = compareSet.get(recordKey).getMinScaleValue();
								double newMaxValue = compareSet.get(recordKey).getMaxScaleValue();
								log.fine(String.format("scale values from record to be added min=%.3f max=%.3f", newMinValue, newMaxValue));

								if (newMinValue < oldMinValue) {
									compareSet.setMinValue(newMinValue); // store new min value into record set
								}
								oldMinValue = compareSet.getMinValue();
								if (newMaxValue > oldMaxValue) {
									compareSet.setMaxValue(newMaxValue); // store new max value into record set
								}
								for (String minRecordKey : compareSet.keySet()) { // loop through all and make equal
									compareSet.get(minRecordKey).setStartEndDefined(true, compareSet.getMinValue(), compareSet.getMaxValue());
								}
							}

							CurveSelectorContextMenu.this.application.updateCompareWindow();
						}
					}
					else if( oldRecordKey != null) CurveSelectorContextMenu.this.application.openMessageDialog("Die Kurve sollte sichtbar sein, bevor man sie in den Kurvenvergleich kopiert!");
				}
			});
			this.cleanCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			this.cleanCurveCompare.setText("Lösche Kurvenvergleich");
			this.cleanCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("cleanCurveCompare Action performed! " + e);
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
			this.application.setStatusMessage("");
			isChanged = true;
		}
		this.recordNameMeasurement = tmpRecordNameMeasurement;
		return isChanged;
	}

	/**
	 * @param enabled the isRecordVisible to set
	 */
	public void setRecordVisible(boolean enabled) {
		this.isRecordVisible = enabled;
	}

	/**
	 * @return the isRecordVisible
	 */
	public boolean isRecordVisible() {
		return this.isRecordVisible;
	}
}
