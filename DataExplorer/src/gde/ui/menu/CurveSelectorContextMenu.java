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
	private Logger												log	= Logger.getLogger(this.getClass().getName());

	private Menu													lineWidthMenu, lineTypeMenu, axisEndValuesMenu, axisNumberFormatMenu, axisPositionMenu, timeGridMenu, horizontalGridMenu;
	private MenuItem											lineVisible, lineColor, copyCurveCompare, cleanCurveCompare;
	private MenuItem											lineWidth, lineWidthMenuItem1, lineWidthMenuItem2, lineWidthMenuItem3;
	private MenuItem											lineType, lineTypeMenuItem1, lineTypeMenuItem2, lineTypeMenuItem3;
	private MenuItem											axisEndValues, axisEndAuto, axisEndRound, axisStarts0, axisEndManual;
	private MenuItem											axisNumberFormat, axisNumberFormat0, axisNumberFormat1, axisNumberFormat2, axisNumberFormat3;
	private MenuItem											axisPosition, axisPositionLeft, axisPositionRight;
	private MenuItem 											measure, deltaMeasure;
	private MenuItem											timeGridColor, timeGrid, timeGridOff, timeGridMain, timeGridMod60;
	private MenuItem											horizontalGridRecordName, horizontalGridColor, horizontalGrid, horizontalGridOff, horizontalGridEveryTick, horizontalGridEverySecond;

	private RecordSet											recordSet;
	private final OpenSerialDataExplorer	application;
	private AxisEndValuesDialog						axisEndValuesDialog;

	public CurveSelectorContextMenu() {
		super();
		this.application = OpenSerialDataExplorer.getInstance();
		this.axisEndValuesDialog = new AxisEndValuesDialog(OpenSerialDataExplorer.getInstance().getShell(), SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	public void createMenu(final Menu popupmenu) {
		try {
			popupmenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("popupmenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						int type = (Integer) selectedItem.getData(GraphicsWindow.WINDOW_TYPE);
						recordSet = (type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();

						if (recordSet != null) {
							setAllEnabled(true);

							String recordNameKey = selectedItem.getText();
							lineVisible.setSelection(recordSet.getRecord(recordNameKey).isVisible());
							if (type == GraphicsWindow.TYPE_COMPARE) copyCurveCompare.setEnabled(false);

							// check zoom mode
							if (recordSet.isZoomMode()) {
								axisEndValues.setEnabled(false);
								axisEndValues.setText("Zoommode aktiv");
							}
							else {
								axisEndValues.setEnabled(true);
								axisEndValues.setText("Achsen-Endwerte");
							}
							
							// check measurement selections
							measure.setSelection(recordSet.isMeasurementMode(recordNameKey));
							deltaMeasure.setSelection(recordSet.isDeltaMeasurementMode(recordNameKey));
						}
						else
							setAllEnabled(false);
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});
			
			lineVisible = new MenuItem(popupmenu, SWT.CHECK);
			lineVisible.setText("Kurve sichtbar");
			lineVisible.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineVisibler Action performed!");
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						if (lineVisible.getSelection()) { // true
							recordSet.getRecord(recordNameKey).setVisible(true);
							selectedItem.setChecked(true);
						}
						else { // false
							recordSet.getRecord(recordNameKey).setVisible(false);
							selectedItem.setChecked(false);
						}
						application.updateGraphicsWindow();
					}
				}
			});

			lineColor = new MenuItem(popupmenu, SWT.PUSH);
			lineColor.setText("KurvenLinienFarbe");
			lineColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event evt) {
					log.finest("lineColor performed! " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						RGB rgb = application.openColorDialog();
						if (rgb != null) {
							Color color = SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue);
							selectedItem.setForeground(color);
							recordSet.getRecord(recordNameKey).setColor(color);
							application.updateGraphicsWindow();
						}
					}
				}
			});
			lineWidth = new MenuItem(popupmenu, SWT.CASCADE);
			lineWidth.setText("KurvenLinienDicke");
			lineWidthMenu = new Menu(lineWidth);
			lineWidth.setMenu(lineWidthMenu);
			lineWidthMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("lineWidthMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						int width = recordSet.getRecord(recordNameKey).getLineWidth();
						switch (width) {
						case 1:
							lineWidthMenuItem1.setSelection(true);
							lineWidthMenuItem2.setSelection(false);
							lineWidthMenuItem3.setSelection(false);
							break;
						case 2:
							lineWidthMenuItem1.setSelection(false);
							lineWidthMenuItem2.setSelection(true);
							lineWidthMenuItem3.setSelection(false);
							break;
						case 3:
							lineWidthMenuItem1.setSelection(false);
							lineWidthMenuItem2.setSelection(false);
							lineWidthMenuItem3.setSelection(true);
							break;
						default:
							lineWidthMenuItem1.setSelection(false);
							lineWidthMenuItem2.setSelection(false);
							lineWidthMenuItem3.setSelection(false);
							break;
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			lineWidthMenuItem1 = new MenuItem(lineWidthMenu, SWT.CHECK);
			lineWidthMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.gif"));
			lineWidthMenuItem1.setText("  1");
			lineWidthMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 1");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setLineWidth(1);
						application.updateGraphicsWindow();
						lineWidthMenuItem2.setSelection(false);
						lineWidthMenuItem3.setSelection(false);
					}
				}
			});
			lineWidthMenuItem2 = new MenuItem(lineWidthMenu, SWT.CHECK);
			lineWidthMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineWidth2.gif"));
			lineWidthMenuItem2.setText("  2");
			lineWidthMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 2");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setLineWidth(2);
						application.updateGraphicsWindow();
						lineWidthMenuItem1.setSelection(false);
						lineWidthMenuItem3.setSelection(false);
					}
				}
			});
			lineWidthMenuItem3 = new MenuItem(lineWidthMenu, SWT.CHECK);
			lineWidthMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineWidth3.gif"));
			lineWidthMenuItem3.setText("  3");
			lineWidthMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("Linienedicke 3");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setLineWidth(3);
						application.updateGraphicsWindow();
						lineWidthMenuItem1.setSelection(false);
						lineWidthMenuItem2.setSelection(false);
					}
				}
			});

			lineType = new MenuItem(popupmenu, SWT.CASCADE);
			lineType.setText("KurvenLinienTyp");
			lineTypeMenu = new Menu(lineType);
			lineType.setMenu(lineTypeMenu);
			lineTypeMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("lineTypeMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						int type = recordSet.getRecord(recordNameKey).getLineStyle();
						switch (type) {
						case SWT.LINE_SOLID:
							lineTypeMenuItem1.setSelection(true);
							lineTypeMenuItem2.setSelection(false);
							lineTypeMenuItem3.setSelection(false);
							break;
						case SWT.LINE_DASH:
							lineTypeMenuItem1.setSelection(false);
							lineTypeMenuItem2.setSelection(true);
							lineTypeMenuItem3.setSelection(false);
							break;
						case SWT.LINE_DOT:
							lineTypeMenuItem1.setSelection(false);
							lineTypeMenuItem2.setSelection(false);
							lineTypeMenuItem3.setSelection(true);
							break;
						default:
							lineTypeMenuItem1.setSelection(false);
							lineTypeMenuItem2.setSelection(false);
							lineTypeMenuItem3.setSelection(false);
							break;
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			lineTypeMenuItem1 = new MenuItem(lineTypeMenu, SWT.CHECK);
			lineTypeMenuItem1.setImage(SWTResourceManager.getImage("osde/resource/LineType1.gif"));
			lineTypeMenuItem1.setText("durchgezogen");
			lineTypeMenuItem1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem1");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setLineStyle(SWT.LINE_SOLID);
						application.updateGraphicsWindow();
						lineTypeMenuItem2.setSelection(false);
						lineTypeMenuItem3.setSelection(false);
					}
				}
			});
			lineTypeMenuItem2 = new MenuItem(lineTypeMenu, SWT.CHECK);
			lineTypeMenuItem2.setImage(SWTResourceManager.getImage("osde/resource/LineType2.gif"));
			lineTypeMenuItem2.setText("gestrichelt");
			lineTypeMenuItem2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem2");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setLineStyle(SWT.LINE_DASH);
						application.updateGraphicsWindow();
						lineTypeMenuItem1.setSelection(false);
						lineTypeMenuItem3.setSelection(false);
					}
				}
			});
			lineTypeMenuItem3 = new MenuItem(lineTypeMenu, SWT.CHECK);
			lineTypeMenuItem3.setImage(SWTResourceManager.getImage("osde/resource/LineType3.gif"));
			lineTypeMenuItem3.setText("gepunktet");
			lineTypeMenuItem3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("lineTypeMenuItem3");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setLineStyle(SWT.LINE_DOT);
						application.updateGraphicsWindow();
						lineTypeMenuItem1.setSelection(false);
						lineTypeMenuItem2.setSelection(false);
					}
				}
			});
			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			axisEndValues = new MenuItem(popupmenu, SWT.CASCADE);
			axisEndValues.setText("Achsen-Endwerte");
			axisEndValuesMenu = new Menu(axisEndValues);
			axisEndValues.setMenu(axisEndValuesMenu);
			axisEndValuesMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisEndValuesMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						boolean isRounded = recordSet.getRecord(recordNameKey).isRoundOut();
						boolean isStart0 = recordSet.getRecord(recordNameKey).isStartpointZero();
						boolean isManual = recordSet.getRecord(recordNameKey).isStartEndDefined();
						axisEndAuto.setSelection(true);
						axisEndRound.setSelection(false);
						axisStarts0.setSelection(false);
						axisEndManual.setSelection(false);
						if (isManual) {
							axisEndAuto.setSelection(false);
							axisEndRound.setSelection(false);
							axisStarts0.setSelection(false);
							axisEndManual.setSelection(true);
						}
						if (isStart0) {
							axisEndAuto.setSelection(false);
							//axisEndRound.setSelection(false);
							axisStarts0.setSelection(true);
							axisEndManual.setSelection(false);
						}
						if (isRounded) {
							axisEndAuto.setSelection(false);
							axisEndRound.setSelection(true);
							//axisStarts0.setSelection(false);
							axisEndManual.setSelection(false);
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			axisEndAuto = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisEndAuto.setText("automatic");
			axisEndAuto.setSelection(true);
			axisEndAuto.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndAuto");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						axisStarts0.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartpointZero(false);
						axisEndRound.setSelection(false);
						recordSet.getRecord(recordNameKey).setRoundOut(false);
						axisEndManual.setSelection(false);
						recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
						application.updateGraphicsWindow();
					}
				}
			});
			axisEndRound = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisEndRound.setText("gerundet");
			axisEndRound.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndRound");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						if (axisEndRound.getSelection()) { //true
							axisEndAuto.setSelection(false);
							recordSet.getRecord(recordNameKey).setRoundOut(true);
							axisEndManual.setSelection(false);
							recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
						}
						else { // false
							axisEndAuto.setSelection(true);
							recordSet.getRecord(recordNameKey).setRoundOut(false);
							axisEndManual.setSelection(false);
							recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
						}
						application.updateGraphicsWindow();
					}
				}
			});
			axisStarts0 = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisStarts0.setText("beginnt bei 0");
			axisStarts0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisStarts0");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						if (axisStarts0.getSelection()) { // true
							axisEndAuto.setSelection(false);
							recordSet.getRecord(recordNameKey).setStartpointZero(true);
							axisEndManual.setSelection(false);
							recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
						}
						else { // false
							axisEndAuto.setSelection(false);
							recordSet.getRecord(recordNameKey).setStartpointZero(false);
							axisEndManual.setSelection(false);
							recordSet.getRecord(recordNameKey).setStartEndDefined(false, 0, 0);
						}
						application.updateGraphicsWindow();
					}
				}
			});
			axisEndManual = new MenuItem(axisEndValuesMenu, SWT.CHECK);
			axisEndManual.setText("manuell");
			axisEndManual.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisEndManual Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						axisEndManual.setSelection(true);
						axisEndAuto.setSelection(false);
						axisStarts0.setSelection(false);
						Record record = recordSet.getRecord(recordNameKey);
						record.setStartpointZero(false);
						axisEndRound.setSelection(false);
						record.setRoundOut(false);
						double[] oldMinMax = {record.getMinScaleValue(), record.getMaxScaleValue()};
						double[] newMinMax = axisEndValuesDialog.open(oldMinMax);
						record.setStartEndDefined(true, newMinMax[0], newMinMax[1]);
						application.updateGraphicsWindow();
					}
				}
			});

			axisNumberFormat = new MenuItem(popupmenu, SWT.CASCADE);
			axisNumberFormat.setText("Achsen-Zahlenformat");
			axisNumberFormatMenu = new Menu(axisNumberFormat);
			axisNumberFormat.setMenu(axisNumberFormatMenu);
			axisNumberFormatMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisNumberFormatMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						int format = recordSet.getRecord(recordNameKey).getNumberFormat();
						switch (format) {
						case 0:
							axisNumberFormat0.setSelection(true);
							axisNumberFormat1.setSelection(false);
							axisNumberFormat2.setSelection(false);
							axisNumberFormat3.setSelection(false);
							break;
						case 1:
							axisNumberFormat0.setSelection(false);
							axisNumberFormat1.setSelection(true);
							axisNumberFormat2.setSelection(false);
							axisNumberFormat3.setSelection(false);
							break;
						default:
						case 2:
							axisNumberFormat0.setSelection(false);
							axisNumberFormat1.setSelection(false);
							axisNumberFormat2.setSelection(true);
							axisNumberFormat3.setSelection(false);
							break;
						case 3:
							axisNumberFormat0.setSelection(false);
							axisNumberFormat1.setSelection(false);
							axisNumberFormat2.setSelection(false);
							axisNumberFormat3.setSelection(true);
							break;
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			axisNumberFormat0 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat0.setText("0000");
			axisNumberFormat0.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat0");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setNumberFormat(0);
						application.updateGraphicsWindow();
					}
				}
			});
			axisNumberFormat1 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat1.setText("000.0");
			axisNumberFormat1.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat1");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setNumberFormat(1);
						application.updateGraphicsWindow();
					}
				}
			});
			axisNumberFormat2 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat2.setText("00.00");
			axisNumberFormat2.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat2");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setNumberFormat(2);
						application.updateGraphicsWindow();
					}
				}
			});
			axisNumberFormat3 = new MenuItem(axisNumberFormatMenu, SWT.CHECK);
			axisNumberFormat3.setText("00.000");
			axisNumberFormat3.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisNumberFormat3");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setNumberFormat(3);
						application.updateGraphicsWindow();
					}
				}
			});

			axisPosition = new MenuItem(popupmenu, SWT.CASCADE);
			axisPosition.setText("Achsen-Position");
			axisPositionMenu = new Menu(axisPosition);
			axisPosition.setMenu(axisPositionMenu);
			axisPositionMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("axisPositionMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						String recordNameKey = selectedItem.getText();
						boolean isLeft = recordSet.getRecord(recordNameKey).isPositionLeft();
						if (isLeft) {
							axisPositionLeft.setSelection(true);
							axisPositionRight.setSelection(false);
						}
						else {
							axisPositionLeft.setSelection(false);
							axisPositionRight.setSelection(true);
						}
					}
				}

				public void menuHidden(MenuEvent evt) {
				}
			});

			axisPositionLeft = new MenuItem(axisPositionMenu, SWT.CHECK);
			axisPositionLeft.setText("linke Seite");
			axisPositionLeft.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisPositionLeft Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setPositionLeft(true);
						application.updateGraphicsWindow();
					}
				}
			});
			axisPositionRight = new MenuItem(axisPositionMenu, SWT.CHECK);
			axisPositionRight.setText("rechte Seite");
			axisPositionRight.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("axisPositionRight Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.getRecord(recordNameKey).setPositionLeft(false);
						application.updateGraphicsWindow();
					}
				}
			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			timeGrid = new MenuItem(popupmenu, SWT.CASCADE);
			timeGrid.setText("Zeit-Grid(vertical)");
			timeGridMenu = new Menu(timeGrid);
			timeGrid.setMenu(timeGridMenu);
			timeGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("timeGridMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						int gridType = recordSet.getTimeGridType();
						switch (gridType) {
						case RecordSet.TIME_GRID_MAIN:
							timeGridOff.setSelection(false);
							timeGridMain.setSelection(true);
							timeGridMod60.setSelection(false);
							break;
						case RecordSet.TIME_GRID_MOD60:
							timeGridOff.setSelection(false);
							timeGridMain.setSelection(false);
							timeGridMod60.setSelection(true);
							break;
						case RecordSet.TIME_GRID_NONE:
						default:
							timeGridOff.setSelection(true);
							timeGridMain.setSelection(false);
							timeGridMod60.setSelection(false);
							break;
						}
					}
				}
				public void menuHidden(MenuEvent evt) {
				}
			});

			timeGridOff = new MenuItem(timeGridMenu, SWT.CHECK);
			timeGridOff.setText("aus");
			timeGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridOff Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.setTimeGridType(RecordSet.TIME_GRID_NONE);
						application.updateGraphicsWindow();
					}
				}
			});
			timeGridMain = new MenuItem(timeGridMenu, SWT.CHECK);
			timeGridMain.setText("jede Zeitmarke");
			timeGridMain.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridMain Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.setTimeGridType(RecordSet.TIME_GRID_MAIN);
						application.updateGraphicsWindow();
					}
				}
			});
			timeGridMod60 = new MenuItem(timeGridMenu, SWT.CHECK);
			timeGridMod60.setText("jede Zeitmarke mod 60");
			timeGridMod60.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridMod60 Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.setTimeGridType(RecordSet.TIME_GRID_MOD60);
						application.updateGraphicsWindow();
					}
				}
			});
			timeGridColor = new MenuItem(timeGridMenu, SWT.PUSH);
			timeGridColor.setText("Linienfarbe");
			timeGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("timeGridColor Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						RGB rgb = application.openColorDialog();
						if (rgb != null) {
							recordSet.setTimeGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							application.updateGraphicsWindow();
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
//					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
//					if (recordNameKey != null) {
//						recordSet.setTimeGridLineStyle(SWT.LINE_DOT); //TODO
//						application.updateGraphicsWindow();
//					}
//				}
//			});
			
			horizontalGrid = new MenuItem(popupmenu, SWT.CASCADE);
			horizontalGrid.setText("Kurven-Grid(horizontal)");
			horizontalGridMenu = new Menu(horizontalGrid);
			horizontalGrid.setMenu(horizontalGridMenu);
			horizontalGridMenu.addMenuListener(new MenuListener() {
				public void menuShown(MenuEvent evt) {
					log.finest("horizontalGridMenu MenuListener " + evt);
					TableItem selectedItem = (TableItem) popupmenu.getData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM);
					if (selectedItem != null && !selectedItem.isDisposed()) {
						horizontalGridRecordName.setText("gesetzt -> " + recordSet.getHorizontalGridRecordName());
						int gridType = recordSet.getHorizontalGridType();
						switch (gridType) {
						case RecordSet.HORIZONTAL_GRID_EVERY:
							horizontalGridOff.setSelection(false);
							horizontalGridEveryTick.setSelection(true);
							horizontalGridEverySecond.setSelection(false);
							break;
						case RecordSet.HORIZONTAL_GRID_SECOND:
							horizontalGridOff.setSelection(false);
							horizontalGridEveryTick.setSelection(false);
							horizontalGridEverySecond.setSelection(true);
							break;
						case RecordSet.HORIZONTAL_GRID_NONE:
						default:
							horizontalGridOff.setSelection(true);
							horizontalGridEveryTick.setSelection(false);
							horizontalGridEverySecond.setSelection(false);
							break;
						}
					}
				}
				public void menuHidden(MenuEvent evt) {
				}
			});

			horizontalGridRecordName = new MenuItem(horizontalGridMenu, SWT.NONE);

			horizontalGridOff = new MenuItem(horizontalGridMenu, SWT.CHECK);
			horizontalGridOff.setText("aus");
			horizontalGridOff.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridOff Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_NONE);
						application.updateGraphicsWindow();
					}
				}
			});
			horizontalGridEveryTick = new MenuItem(horizontalGridMenu, SWT.CHECK);
			horizontalGridEveryTick.setText("jede Zeitmarke");
			horizontalGridEveryTick.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridMain Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_EVERY);
						recordSet.setHorizontalGridRecordKey(recordNameKey);
						application.updateGraphicsWindow();
					}
				}
			});
			horizontalGridEverySecond = new MenuItem(horizontalGridMenu, SWT.CHECK);
			horizontalGridEverySecond.setText("jede zweite Zeitmarke");
			horizontalGridEverySecond.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridMod60 Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						recordSet.setHorizontalGridType(RecordSet.HORIZONTAL_GRID_SECOND);
						recordSet.setHorizontalGridRecordKey(recordNameKey);
						application.updateGraphicsWindow();
					}
				}
			});
			horizontalGridColor = new MenuItem(horizontalGridMenu, SWT.PUSH);
			horizontalGridColor.setText("Linienfarbe");
			horizontalGridColor.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("horizontalGridColor Action performed!");
					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (recordNameKey != null) {
						RGB rgb = application.openColorDialog();
						if (rgb != null) {
							recordSet.setHorizontalGridColor(SWTResourceManager.getColor(rgb.red, rgb.green, rgb.blue));
							application.updateGraphicsWindow();
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
//					String recordNameKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
//					if (recordNameKey != null) {
//						recordSet.setHorizontalGridLineStyle(SWT.LINE_DASH); //TODO
//						application.updateGraphicsWindow();
//					}
//				}
//			});

			new MenuItem(popupmenu, SWT.SEPARATOR);

			measure = new MenuItem(popupmenu, SWT.CHECK);
			measure.setText("Kurvenpunkt messen");
			measure.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.finest("measure.widgetSelected, event=" + evt);
					if (measure.getSelection() == true) {
						application.setMeasurementActive((String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME), true);
					}
					else {
						application.setMeasurementActive((String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME), false);
						application.setStatusMessage("");
					}
				}
			});
			deltaMeasure = new MenuItem(popupmenu, SWT.CHECK);
			deltaMeasure.setText("Punktdifferenz messen");
			deltaMeasure.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent evt) {
					log.finest("deltaMeasure.widgetSelected, event=" + evt);
					if (deltaMeasure.getSelection() == true) {
						application.setDeltaMeasurementActive((String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME), true);
					}
					else {
						application.setDeltaMeasurementActive((String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME), false);
						application.setStatusMessage("");
					}
				}
			});

			
			new MenuItem(popupmenu, SWT.SEPARATOR);

			copyCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			copyCurveCompare.setText("Kopiere Kurvenvergleich");
			copyCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("copyCurveCompare Action performed!");
					String newRecordKey = (String) popupmenu.getData(OpenSerialDataExplorer.RECORD_NAME);
					if (newRecordKey != null) {
						RecordSet compareSet = application.getCompareSet();
						boolean isComparable = true;
						if (!compareSet.isEmpty() && compareSet.getTimeStep_ms() != recordSet.getTimeStep_ms()) {
							application
									.openMessageDialog("Zeitbasis der Kurven, die verglichen werden sollen passt nicht zusammen! Vermutlich wurden sie mit unterschiedlichen Geräten aufgenommen. Das ist zu Zeit noch nicht unterstützt, da hier eine Umrechnung auf eine einheitliche Zeitbasis stattfinden muss.");
							isComparable = false;
							return;
						}
						if (!compareSet.isEmpty() && !compareSet.getActiveAndVisibleRecordNames()[0].startsWith(newRecordKey)) {
							application.openMessageDialog("Type der Kurven (" + newRecordKey + "-" + compareSet.getRecordNames()[0].split("_")[0] + "), die verglichen werden sollen passen nicht zusammen!");
							isComparable = false;
							return;
						}
						if (compareSet.isEmpty() || isComparable) {
							compareSet.setTimeStep_ms(recordSet.getTimeStep_ms());
							String recordkey = newRecordKey + "_" + compareSet.size();
							compareSet.addRecordName(recordkey);
							compareSet.put(recordkey, recordSet.get(newRecordKey).clone()); // will delete channelConfigKey
							compareSet.get(recordkey).setChannelConfigKey(recordSet.get(newRecordKey).getChannelConfigKey());
							int maxRecordSize = compareSet.getSize();
							double oldMinValue = compareSet.getMinValue();
							double oldMaxValue = compareSet.getMaxValue();
							log.fine(String.format("scale values from compare set min=%.3f max=%.3f", oldMinValue, oldMaxValue));
							for (String recordKey : compareSet.keySet()) {
								if (compareSet.get(recordKey).size() > maxRecordSize) {
									compareSet.setMaxSize(compareSet.get(recordKey).size());
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

							application.updateCompareWindow();
						}
					}
				}
			});
			cleanCurveCompare = new MenuItem(popupmenu, SWT.PUSH);
			cleanCurveCompare.setText("Lösche Kurvenvergleich");
			cleanCurveCompare.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					log.finest("cleanCurveCompare Action performed!");
					RecordSet compareSet = application.getCompareSet();
					compareSet.clear();
					application.updateCompareWindow();
				}
			});
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setAllEnabled(boolean enabled) {
		lineVisible.setEnabled(enabled);
		lineColor.setEnabled(enabled);
		lineWidth.setEnabled(enabled);
		lineType.setEnabled(enabled);
		axisEndValues.setEnabled(enabled);
		axisNumberFormat.setEnabled(enabled);
		axisPosition.setEnabled(enabled);
		timeGrid.setEnabled(enabled);
		horizontalGrid.setEnabled(enabled);
		measure.setEnabled(enabled);
		deltaMeasure.setEnabled(enabled);
		copyCurveCompare.setEnabled(enabled);
		cleanCurveCompare.setEnabled(enabled);
	}
}
