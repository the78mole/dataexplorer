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
package osde.ui.tab;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.CurveSelectorContextMenu;
import osde.utils.CurveUtils;
import osde.utils.TimeLine;

/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class GraphicsWindow {
	private Logger												log											= Logger.getLogger(this.getClass().getName());

	public static final int								TYPE_NORMAL							= 0;
	public static final int								TYPE_COMPARE						= 1;
	public static final String						WINDOW_TYPE							= "window_type";
	
	public final static int								MODE_RESET							= 0;
	public final static int								MODE_ZOOM								= 1;
	public final static int								MODE_MEASURE						= 2;
	public final static int								MODE_MEASURE_DELTA			= 3;
	public final static int								MODE_PAN								= 4;
	

	private final TabFolder								displayTab;
	private SashForm											graphicSashForm;
	// Curve Selector Table with popup menu
	private Composite											curveSelector;
	private CLabel												curveSelectorHeader;
	private Table													curveSelectorTable;
	private TableColumn										tableSelectorColumn;
	private TabItem												graphic;
	private Menu													popupmenu;
	private CurveSelectorContextMenu			contextMenu;
	
	// drawing canvas
	private Canvas												graphicCanvas;

	private final OpenSerialDataExplorer	application;
	private final Channels								channels;
	private final String									name;
	private final TimeLine								timeLine								= new TimeLine();
	private final int											type;
	private boolean												isCurveSelectorEnabled	= true;
	private int selectorHeaderWidth;
	
	private int														xDown = 0;
	private int														xUp = 0;
	private int														xLast = 0;
	private int														yDown = 0;
	private int														yUp = 0;
	private int														yLast = 0;
	private int														offSetX, offSetY;
	private Image 												curveArea;
	private GC 														curveAreaGC;
	private Rectangle											curveAreaBounds;
	private GC 														canvasGC;
	
	private boolean												isLeftMouseMeasure = false;
	private boolean												isRightMouseMeasure = false;
	private int 													xPosMeasure = 0, yPosMeasure = 0;
	private int														xPosDelta = 0, yPosDelta = 0;

	private boolean												isZoomMouse = false;
	
	private boolean												isPanMouse = false;
	private int														xDeltaPan = 0;
	private int														yDeltaPan = 0;

	public GraphicsWindow(TabFolder displayTab, int type, String name) {
		this.displayTab = displayTab;
		this.type = type;
		this.name = name;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	public void create() {
		graphic = new TabItem(displayTab, SWT.NONE);
		graphic.setText(name);
		{ // graphicSashForm
			graphicSashForm = new SashForm(displayTab, SWT.HORIZONTAL);
			graphic.setControl(graphicSashForm);
//			GridLayout sashForm1Layout2 = new GridLayout();
//			sashForm1Layout2.makeColumnsEqualWidth = true;
//			graphicSashForm.setLayout(sashForm1Layout2);
			{ // curveSelector
				curveSelector = new Composite(graphicSashForm, SWT.BORDER);
				FormLayout curveSelectorLayout = new FormLayout();
				curveSelector.setLayout(curveSelectorLayout);
				GridData curveSelectorLData = new GridData();
				curveSelector.setLayoutData(curveSelectorLData);
				{
					curveSelectorHeader = new CLabel(curveSelector, SWT.NONE);
					curveSelectorHeader.setText("Kurvenselektor");
					curveSelectorHeader.pack();
					selectorHeaderWidth = curveSelectorHeader.getSize().x;
					//curveSelectorHeader.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					FormData curveSelectorHeaderLData = new FormData();
					curveSelectorHeaderLData.width = selectorHeaderWidth;
					curveSelectorHeaderLData.height = 24;
					curveSelectorHeaderLData.left = new FormAttachment(0, 1000, 0);
					curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 3);
					curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
					curveSelectorHeader.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					curveSelectorTable = new Table(curveSelector, SWT.SINGLE | SWT.CHECK | SWT.EMBEDDED);
					curveSelectorTable.setLinesVisible(true);
					FormData curveTableLData = new FormData();
					curveTableLData.width = 82;
					curveTableLData.height = 457;
					curveTableLData.left = new FormAttachment(0, 1000, 4);
					curveTableLData.top = new FormAttachment(0, 1000, 37);
					curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
					curveTableLData.right = new FormAttachment(1000, 1000, 0);
					curveSelectorTable.setLayoutData(curveTableLData);

					popupmenu = new Menu(application.getShell(), SWT.POP_UP);
					curveSelectorTable.setMenu(popupmenu);
					curveSelectorTable.layout();
					contextMenu = new CurveSelectorContextMenu();
					contextMenu.createMenu(popupmenu);
					curveSelectorTable.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							log.finest("curveTable.paintControl, event=" + evt);
							int selectorItemWidth = graphicSashForm.getSize().x / 10;
							curveSelectorHeader.setSize(selectorItemWidth, curveSelectorHeader.getSize().y);
							tableSelectorColumn.setWidth(selectorItemWidth);
						}
					});
					curveSelectorTable.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.finest("curveTable.widgetSelected, event=" + evt);
							TableItem item = (TableItem) evt.item;
							String recordName = ((TableItem) evt.item).getText();
							log.fine("selected = " + recordName);
							popupmenu.setData(OpenSerialDataExplorer.RECORD_NAME, recordName);
							popupmenu.setData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM, evt.item);
							if (item.getChecked() != (Boolean) item.getData(OpenSerialDataExplorer.OLD_STATE)) {
								Record activeRecord;
								switch (type) {
								case TYPE_COMPARE:
									activeRecord = application.getCompareSet().getRecord(recordName);
									break;

								default:
									activeRecord = channels.getActiveChannel().getActiveRecordSet().getRecord(recordName);
									break;
								}
								if (item.getChecked()) {
									activeRecord.setVisible(true);
									popupmenu.getItem(0).setSelection(true);
									item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) true);
									item.setData(WINDOW_TYPE, type);
									graphicCanvas.redraw();
								}
								else {
									activeRecord.setVisible(false);
									popupmenu.getItem(0).setSelection(false);
									item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) false);
									item.setData(WINDOW_TYPE, type);
									graphicCanvas.redraw();
								}
							}
						}
					});
					{
						tableSelectorColumn = new TableColumn(curveSelectorTable, SWT.LEFT);
						tableSelectorColumn.setWidth(selectorHeaderWidth);
					}
				}
			} // curveSelector
			{ // graphicCanvas
				graphicCanvas = new Canvas(graphicSashForm, SWT.NONE);
				FillLayout graphicCanvasLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				graphicCanvas.setLayout(graphicCanvasLayout);
				graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
					public void mouseMove(MouseEvent evt) {
						if (log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseMove = " + evt);
						RecordSet recordSet = (type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
						if (recordSet != null && curveArea != null) {
							String configKey = recordSet.getChannelName();
							Point point = checkCurveBounds(evt.x, evt.y);
							evt.x = point.x;
							evt.y = point.y;
							
							String measureRecordKey = recordSet.getRecordKeyMeasurement();
							canvasGC.setLineWidth(1);
							canvasGC.setLineStyle(SWT.LINE_DASH);

							if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
								try {
									if (isZoomMouse && recordSet.isZoomMode()) {
										if (log.isLoggable(Level.FINER)) log.finer(String.format("xDown = %d, evt.x = %d, xLast = %d  -  yDown = %d, evt.y = %d, yLast = %d", xDown, evt.x, xLast, yDown, evt.y, yLast));

										//clean obsolete rectangle
										int left = xLast - xDown > 0 ? xDown : xLast;
										int top = yLast - yDown > 0 ? yDown : yLast;
										int width = xLast - xDown > 0 ? xLast - xDown : xDown - xLast;
										int height = yLast - yDown > 0 ? yLast - yDown : yDown - yLast;
										if (log.isLoggable(Level.FINER)) log.finer("clean left = " + left + " top = " + top + " width = " + width + " height = " + height);
										eraseHorizontalLine(top, left, width + 1, 1);
										eraseVerticalLine(left, top, height + 1, 1);
										eraseHorizontalLine(top + height, left + 1, width, 1);
										eraseVerticalLine(left + width, top + 1, height, 1);

										left = evt.x - xDown > 0 ? xDown + offSetX : evt.x + offSetX;
										top = evt.y - yDown > 0 ? yDown + offSetY : evt.y + offSetY;
										width = evt.x - xDown > 0 ? evt.x - xDown : xDown - evt.x;
										height = evt.y - yDown > 0 ? evt.y - yDown : yDown - evt.y;
										if (log.isLoggable(Level.FINER)) log.finer("draw  left = " + (left - offSetX) + " top = " + (top - offSetY) + " width = " + width + " height = " + height);
										canvasGC.drawRectangle(left, top, width, height);

										/* detect directions to enable same behavior as LogView
										if (xDown < evt.x && yDown < evt.y) { //top left -> bottom right
										}
										else if (xDown < evt.x && yDown > evt.y) { // bottom left -> top right
										}
										if (xDown > evt.x && yDown < evt.y) { //top right -> left bottom
										}
										if (xDown > evt.x && yDown > evt.y) { // bottom left -> top right
										}
										*/
										xLast = evt.x;
										yLast = evt.y;
									}
									else if (isLeftMouseMeasure) {
										Record record = recordSet.getRecord(measureRecordKey);
										// clear old measure lines
										eraseVerticalLine(xPosMeasure, 0, curveAreaBounds.height, 1);
										//no change don't needs to be calculated, but the calculation limits to bounds
										yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
										eraseHorizontalLine(yPosMeasure, 0, curveAreaBounds.width, 1);

										if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
											// clear old delta measure lines
											eraseVerticalLine(xPosDelta, 0, curveAreaBounds.height, 1);
											//no change don't needs to be calculated, but the calculation limits to bounds
											yPosDelta = record.getDisplayPointDataValue(xPosDelta, curveAreaBounds);
											eraseHorizontalLine(yPosDelta, 0, curveAreaBounds.width, 1);

											canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
											drawVerticalLine(xPosDelta, 0, curveAreaBounds.height);
											drawHorizontalLine(yPosDelta, 0, curveAreaBounds.width);
											canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
										}
										// all obsolete lines are cleaned up now draw new position marker
										xPosMeasure = evt.x; // evt.x is already relative to curve area
										drawVerticalLine(xPosMeasure, 0, curveAreaBounds.height);
										yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
										drawHorizontalLine(yPosMeasure, 0, curveAreaBounds.width);
										if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
											StringBuilder sb = new StringBuilder();
											sb.append(" ").append(record.getName()).append(" (delta) = ").append(record.getDisplayDeltaValue(yPosMeasure - yPosDelta, curveAreaBounds)).append(" ").append(
													record.getUnit());
											sb.append(" ===> ").append(record.getSlopeValue(new Point(xPosDelta - xPosMeasure, yPosMeasure - yPosDelta), curveAreaBounds)).append(" ").append(
													record.getUnit()).append("/sec");
											application.setStatusMessage(sb.toString());
										}
										else {
											application.setStatusMessage("  " + record.getName() + " = " + record.getDisplayPointValueString(yPosMeasure, curveAreaBounds) + " "
													+ record.getUnit() + " - (" + recordSet.getDisplayPointTime(xPosMeasure) + ") ");
										}
									}
									else if (isRightMouseMeasure) {
										Record record = recordSet.getRecord(measureRecordKey);
										// clear old delta measure lines
										eraseVerticalLine(xPosDelta, 0, curveAreaBounds.height, 1);
										//no change don't needs to be calculated, but the calculation limits to bounds
										yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
										eraseHorizontalLine(yPosDelta, 0, curveAreaBounds.width, 1);

										// clear old measure lines
										eraseVerticalLine(xPosMeasure, 0, curveAreaBounds.height, 1);
										//no change don't needs to be calculated, but the calculation limits to bounds
										yPosDelta = record.getDisplayPointDataValue(xPosDelta, curveAreaBounds);
										eraseHorizontalLine(yPosMeasure, 0, curveAreaBounds.width, 1);

										// always needs to draw measurement pointer
										drawVerticalLine(xPosMeasure, 0, curveAreaBounds.height);
										//no change don't needs to be calculated yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
										drawHorizontalLine(yPosMeasure, 0, curveAreaBounds.width);

										// update the new delta position
										xPosDelta = evt.x; // evt.x is already relative to curve area
										canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
										drawVerticalLine(xPosDelta, 0, curveAreaBounds.height);
										yPosDelta = record.getDisplayPointDataValue(xPosDelta, curveAreaBounds);
										drawHorizontalLine(yPosDelta, 0, curveAreaBounds.width);
										canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

										StringBuilder sb = new StringBuilder();
										sb.append(" ").append(record.getName()).append(" (delta) = ").append(record.getDisplayDeltaValue(yPosMeasure - yPosDelta, curveAreaBounds)).append(" ").append(
												record.getDevice().getMeasurementUnit(configKey, measureRecordKey));
										sb.append(" ===> ").append(record.getSlopeValue(new Point(xPosDelta - xPosMeasure, yPosMeasure - yPosDelta), curveAreaBounds)).append(" ").append(
												record.getDevice().getMeasurementUnit(configKey, measureRecordKey)).append("/sec");
										application.setStatusMessage(sb.toString());
									}
									else if(isPanMouse) {
										xDeltaPan = (xLast != 0 && xLast != evt.x) ? (xDeltaPan + (xLast < evt.x ? -1 : 1)) : 0;
										yDeltaPan = (yLast != 0 && yLast != evt.y) ? (yDeltaPan + (yLast < evt.y ? 1 : -1)) : 0;
										if (log.isLoggable(Level.FINER)) log.finer(" xDeltaPan = " + xDeltaPan + " yDeltaPan = " + yDeltaPan);
										if ((xDeltaPan != 0 && xDeltaPan %5 == 0) || (yDeltaPan != 0 && yDeltaPan %5 == 0)) {
											recordSet.shift(xDeltaPan, yDeltaPan); // 10% each direction
											graphicCanvas.redraw();
											xDeltaPan = yDeltaPan = 0;
										}
										xLast = evt.x;
										yLast = evt.y;
									}
								}
								catch (RuntimeException e) {
									log.log(Level.WARNING, "mouse pointer out of range", e);
								}
							}
							else if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey))) {
								if (xPosMeasure + 1 >= evt.x && xPosMeasure - 1 <= evt.x 
										|| xPosDelta + 1 >= evt.x && xPosDelta - 1 <= evt.x) { // snap mouse pointer
									graphicCanvas.setCursor(SWTResourceManager.getCursor("osde/resource/MoveH.gif"));
								}
								else {
									graphicCanvas.setCursor(application.getCursor());
								}
							}
							else if (isZoomMouse && !isPanMouse) {
								graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
							}
							else if (isPanMouse) {
								graphicCanvas.setCursor(SWTResourceManager.getCursor("osde/resource/Hand.gif"));
							}
							else {
								graphicCanvas.setCursor(application.getCursor());
							}
						}
					}
				});
				graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseExit(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseExit, event="+evt);
						graphicCanvas.setCursor(application.getCursor());
					}
				});
				graphicCanvas.addMouseListener(new MouseAdapter() {
					public void mouseDown(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseDown, event="+evt);
						RecordSet recordSet = (type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
						if (curveArea != null && recordSet != null) {
							String measureRecordKey = recordSet.getRecordKeyMeasurement();
							Point point = checkCurveBounds(evt.x, evt.y);
							xDown = point.x;
							yDown = point.y;
							
							if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey)) && xPosMeasure + 1 >= xDown && xPosMeasure - 1 <= xDown) { // snap mouse pointer
								isLeftMouseMeasure = true;
								isRightMouseMeasure = false;
							}
							else if(measureRecordKey != null && recordSet.isDeltaMeasurementMode(measureRecordKey) && xPosDelta + 1 >= xDown && xPosDelta - 1 <= xDown) { // snap mouse pointer
								isRightMouseMeasure = true;
								isLeftMouseMeasure = false;
							}
							else {
								isLeftMouseMeasure = false;
								isRightMouseMeasure = false;
							}
							if(log.isLoggable(Level.FINER)) log.finer("isMouseMeasure = " + isLeftMouseMeasure + " isMouseDeltaMeasure = " + isRightMouseMeasure);
						}
					}
					public void mouseUp(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseUp, event="+evt);
						RecordSet recordSet = (type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
						if (curveArea != null && recordSet != null) {
							Point point = checkCurveBounds(evt.x, evt.y);
							xUp = point.x;
							yUp = point.y;
							
							if (isZoomMouse) {
								// sort the zoom values
								int xStart = xDown < xUp ? xDown : xUp;
								int xEnd = xDown > xUp ? xDown + 1 : xUp + 1;
								int yMin = curveAreaBounds.height - 1 - (yDown > yUp ? yDown : yUp);
								int yMax = curveAreaBounds.height - (yDown < yUp ? yDown : yUp);
								if(log.isLoggable(Level.FINER)) log.finer("zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax);
								if (xEnd-xStart > 5 && yMax-yMin > 5) {
									recordSet.setZoomOffsetAndWidth(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
									graphicCanvas.redraw();
								}
							}
							else if (isLeftMouseMeasure) {
								isLeftMouseMeasure = false;
								//application.setStatusMessage("");
							}
							else if (isRightMouseMeasure) {
								isRightMouseMeasure = false;
								//application.setStatusMessage("");
							}
							if(log.isLoggable(Level.FINER)) log.finer("isMouseMeasure = " + isLeftMouseMeasure + " isMouseDeltaMeasure = " + isRightMouseMeasure);
						}
					}
				});
				graphicCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
				graphicCanvas.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						drawAreaPaintControl(evt);
					}
				});
			} // graphicCanvas
			graphicSashForm.setWeights(new int[] { 10, 100 }); // 10:100  -> 9 == width required for curveSelectorTable
		} // graphicSashForm
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records 
	 * @param evt
	 */
	private void drawAreaPaintControl(PaintEvent evt) {
		log.finest("drawAreaPaintControl.paintControl, event=" + evt);
		// Get the canvas and its dimensions
		Canvas canvas = (Canvas) evt.widget;
		canvasGC = SWTResourceManager.getGC(canvas, "curveArea_" + type);

		Point canvasSize = canvas.getSize();
		int maxX = canvasSize.x - 5; // enable a small gap if no axis is shown 
		int maxY = canvasSize.y;
		log.fine("canvas size = " + maxX + " x " + maxY);

		RecordSet recordSet = null;
		switch (type) {
		case TYPE_COMPARE:
			if (application.getCompareSet() != null && application.getCompareSet().size() > 0) {
				recordSet = application.getCompareSet();
			}
			break;

		default: // TYPE_NORMAL
			if (channels.getActiveChannel() != null && channels.getActiveChannel().getActiveRecordSet() != null) {
				recordSet = channels.getActiveChannel().getActiveRecordSet();
			}
			break;
		}
		if (recordSet != null) {
			// draw curves
			drawCurves(recordSet, maxX, maxY);
			if (recordSet.isMeasurementMode(recordSet.getRecordKeyMeasurement()) || recordSet.isDeltaMeasurementMode(recordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(MODE_MEASURE, true);
			}
		}	
	}

	/**
	 * method to draw the curves with it scales and defines the curve area
	 * @param recordSet
	 * @param maxX
	 * @param maxY
	 */
	private void drawCurves(RecordSet recordSet, int maxX, int maxY) {
		int[] timeScale = timeLine.getScaleMaxTimeNumber(recordSet);
		int maxTimeNumber = timeScale[0];
		int scaleFactor = timeScale[1];
		int timeFormat = timeScale[2];

		//prepare measurement scales
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		String[] recordNames = recordSet.getRecordNames();
		for (String string : recordNames) {
			if (recordSet.getRecord(string).isVisible() && recordSet.getRecord(string).isDisplayable()) {
				if (recordSet.getRecord(string).isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = numberCurvesRight > 0 ? 1 : 0;
		}
		log.fine("nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight);

		int dataScaleWidth; // space used for text and scales with description or legend
		int x0; // enable a small gap if no axis is shown
		int width; // make the time width  the width for the curves
		int y0;
		int height; // make modulo 20
		// draw x coordinate	- time scale
		int startTime, endTime;
		// Calculate the horizontal area to used for plotting graphs
		int maxTime = maxTimeNumber; // alle 10 min/sec eine Markierung
		Point pt = canvasGC.textExtent("000,00");
		dataScaleWidth = pt.x + pt.y * 2 + 5;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;
		x0 = (int) maxX - (maxX - spaceLeft) + 5;
		int xMax = (int) maxX - spaceRight;
		int fitTimeWidth = (xMax - x0) - ((xMax - x0) % 10); // make time line modulo 10 to enable every 10 min/sec a tick mark
		width = fitTimeWidth;
		xMax = x0 + width;
		int verticalSpace = 3 * pt.y;// space used for text and scales with description or legend
		int spaceTop = 20;
		int spaceBot = verticalSpace;
		y0 = (int) maxY - spaceBot;
		int yMax = (int) maxY - (maxY - spaceTop);
		height = (y0 - yMax) - (y0 - yMax) % 10;
		yMax = y0 - height;
		if (log.isLoggable(Level.FINE)) log.fine("draw area x0=" + x0 + ", y0=" + y0 + ",xMax=" + xMax + ", yMax=" + yMax + "width=" + width + ", height=" + height + ", timeWidth=" + fitTimeWidth);
		// draw curves for each active record
		recordSet.setDrawAreaBounds(new Rectangle(x0, y0 - height, width, height));
		if (log.isLoggable(Level.FINE)) log.fine("curve bounds = " + x0 + " " + (y0 - height) + " " + width + " " + height);
		startTime = TimeLine.convertTimeInFormatNumber(recordSet.getStartTime(), timeFormat);
		endTime = startTime + maxTime;
		timeLine.drawTimeLine(canvasGC, x0, y0, fitTimeWidth, startTime, endTime, scaleFactor, timeFormat, OpenSerialDataExplorer.COLOR_BLACK);

		// get the image and prepare GC
		curveArea = SWTResourceManager.getImage(width, height);
		curveAreaGC = SWTResourceManager.getGC(curveArea);
		curveAreaBounds = curveArea.getBounds();
		
		// clear the image
		curveAreaGC.setBackground(canvasGC.getBackground());
		curveAreaGC.fillRectangle(curveArea.getBounds());
		
		// draw draw area bounding 
		curveAreaGC.setForeground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
		curveAreaGC.drawLine(0, 0, width, 0);
		curveAreaGC.drawLine(0, 0, 0, height-1);
		curveAreaGC.drawLine(width-1, 0, width-1, height-1);

		// draw each record
		for (String record : recordSet.getRecordNames()) {
			Record actualRecord = recordSet.getRecord(record);
			log.fine("drawing record = " + actualRecord.getName() + "-" + actualRecord.isVisible() +  "-" + actualRecord.isDisplayable());
			if (actualRecord.isVisible() && actualRecord.isDisplayable()){
				CurveUtils.drawScale(actualRecord, canvasGC, x0, y0, width, height, dataScaleWidth);
				CurveUtils.drawCurve(actualRecord, curveAreaGC, 0, height, width, height, recordSet.isCompareSet());
			}
		}
		offSetX = x0;
		offSetY = y0-height;
		// check for activated time grid
		if (recordSet.getGridType() > 0) {
			curveAreaGC.setForeground(recordSet.getColorTimeGrid());
			curveAreaGC.setLineStyle(recordSet.getLineStyleTimeGrid());
			for (Integer x : recordSet.getTimeGrid()) {
				curveAreaGC.drawLine(x-offSetX, 0, x-offSetX, height-1);
			}
		}
		canvasGC.drawImage(curveArea, offSetX, offSetY);
		

		if (startTime != 0) { // scaled window 
			String strStartTime = "Ausschnittsbeginn bei " + TimeLine.getFomatedTime(recordSet.getStartTime());
			Point point = canvasGC.textExtent(strStartTime);
			int yPosition = (int)(y0 + pt.y * 2.5);
			canvasGC.drawText(strStartTime, 10, yPosition - point.y/2);
		}
	}

	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGraphics() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				updateCurveSelectorTable();
				graphicCanvas.redraw();
			}
		});
	}

	/**
	 * method to update the curves displayed in the curve selector panel 
	 */
	public void updateCurveSelectorTable() {
		final IDevice device = application.getActiveDevice();
		final RecordSet recordSet = type == TYPE_NORMAL ? channels.getActiveChannel().getActiveRecordSet() : application.getCompareSet();

		if (isCurveSelectorEnabled && recordSet != null) {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					curveSelectorTable.removeAll();
					
					String[] recordKeys = device.getMeasurementNames(recordSet.getChannelName());
					for (int i = 0; i < recordSet.size(); i++) {
						Record record;
						switch (type) {
						case TYPE_COMPARE:
							String recordKey = recordSet.getRecordNames()[0].split("_")[0];
							record = recordSet.getRecord(recordKey + "_" + i);
							break;

						default: // TYPE_NORMAL
							record = recordSet.getRecord(recordKeys[i]);
							break;
						}
						if (log.isLoggable(Level.FINER)) log.finer(record.getName());

						TableItem item = new TableItem(curveSelectorTable, SWT.NULL);
						item.setForeground(record.getColor());
						item.setText(type == TYPE_NORMAL ? record.getName() : record.getName() + "_" + i);
						//item.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.jpg"));
						if (record.isDisplayable()) {
							if (record.isVisible()) {
								item.setChecked(true);
								item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) true);
								item.setData(WINDOW_TYPE, type);
							}
							else {
								item.setChecked(false);
								item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) false);
								item.setData(WINDOW_TYPE, type);
							}
						}
						else {
							item.setChecked(false);
							item.setData(OpenSerialDataExplorer.OLD_STATE, (boolean) false);
							item.setData(WINDOW_TYPE, type);
							item.dispose();
						}
					}
				}
			});
		}
		else curveSelectorTable.removeAll();
	}

	public Canvas getGraphicCanvas() {
		return graphicCanvas;
	}

	public boolean isCurveSelectorEnabled() {
		return isCurveSelectorEnabled;
	}

	public void setCurveSelectorEnabled(boolean isCurveSelectorEnabled) {
		this.isCurveSelectorEnabled = isCurveSelectorEnabled;
	}

	public SashForm getGraphicSashForm() {
		return graphicSashForm;
	}

	/**
	 * draw the start pointer for measurement modes
	 * @param mode
	 * @param isRefresh
	 */
	public void drawMeasurePointer(int mode, boolean isRefresh) {
		this.setModeState(mode); // cleans old pointer if required
		
		// get the record set to work with
		boolean isGraphicsWindow = this.type == GraphicsWindow.TYPE_NORMAL;
		RecordSet recordSet = isGraphicsWindow ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
		String measureRecordKey = recordSet.getRecordKeyMeasurement();
		Record record = recordSet.get(measureRecordKey);
		
		// set the gc properties
		canvasGC.setLineWidth(1);
		canvasGC.setLineStyle(SWT.LINE_DASH);
		canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		
		if (recordSet.isMeasurementMode(measureRecordKey)) {
			// initial measure position
			xPosMeasure = isRefresh ? xPosMeasure : curveAreaBounds.width / 4;
			yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
			log.fine("initial xPosMeasure = " + xPosMeasure + " yPosMeasure = " + yPosMeasure);

			drawVerticalLine(xPosMeasure, 0, curveAreaBounds.height);
			drawHorizontalLine(yPosMeasure, 0, curveAreaBounds.width);

			application.setStatusMessage("  " + record.getName() + " = " + record.getDisplayPointValueString(yPosMeasure, curveAreaBounds) + " " + record.getUnit() + " - (" + recordSet.getDisplayPointTime(xPosMeasure) + ") ");
		}
		else if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
			xPosMeasure = isRefresh ? xPosMeasure : curveAreaBounds.width / 4;
			yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);

			// measure position
			drawVerticalLine(xPosMeasure, 0, curveAreaBounds.height);
			drawHorizontalLine(yPosMeasure, 0, curveAreaBounds.width);
			
			// delta position
			xPosDelta = isRefresh ? xPosDelta : curveAreaBounds.width / 3 * 2;
			yPosDelta = record.getDisplayPointDataValue(xPosDelta, curveAreaBounds);
			
			canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
			drawVerticalLine(xPosDelta, 0, curveAreaBounds.height);
			drawHorizontalLine(yPosDelta, 0, curveAreaBounds.width);
			canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			StringBuilder sb = new StringBuilder();
			sb.append(" ").append(record.getName()). append(" (delta) = ").append(record.getDisplayDeltaValue(yPosMeasure - yPosDelta, curveAreaBounds)).append(" ").append(record.getUnit());
			sb.append(" ===> ").append(record.getSlopeValue(new Point(xPosDelta - xPosMeasure, yPosMeasure - yPosDelta), curveAreaBounds)).append(" ").append(record.getUnit()).append("/sec");
			application.setStatusMessage(sb.toString());
		}
	}
	
	/**
	 * draws horizontal line as defined relative to curve draw area, where there is an offset from left and an offset from top  
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 */
	private void drawVerticalLine(int posFromLeft, int posFromTop, int length) {
		canvasGC.drawLine(posFromLeft+offSetX, posFromTop+offSetY, posFromLeft+offSetX, posFromTop+offSetY+length-1);
	}
	
	/**
	 * draws vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top 
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 */
	private void drawHorizontalLine(int posFromTop, int posFromLeft, int length) {
		canvasGC.drawLine(posFromLeft+offSetX, posFromTop+offSetY, posFromLeft+offSetX+length-1, posFromTop+offSetY);
	}

	/**
	 * erase a vertical line by re-drawing the curve area image 
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 * @param lineWidth
	 */
	private void eraseVerticalLine(int posFromLeft, int posFromTop, int length, int lineWidth) {
		canvasGC.drawImage(curveArea, posFromLeft, posFromTop, lineWidth, length, posFromLeft+offSetX, posFromTop+offSetY, lineWidth, length);
	}

	/**
	 * erase a horizontal line by re-drawing the curve area image 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 * @param lineWidth
	 */
	private void eraseHorizontalLine(int posFromTop, int posFromLeft, int length, int lineWidth) {
		canvasGC.drawImage(curveArea, posFromLeft, posFromTop, length, lineWidth, posFromLeft+offSetX, posFromTop+offSetY, length, lineWidth);
	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 * @param drawAreaBounds
	 * @param gc
	 */
	public void cleanMeasurementPointer() {
		if ((xPosMeasure != 0 && (xPosMeasure < offSetX || xPosMeasure > offSetX + curveAreaBounds.width))
				|| (yPosMeasure != 0 && (yPosMeasure < offSetY || yPosMeasure > offSetY + curveAreaBounds.height))
				|| (xPosDelta != 0 && (xPosDelta < offSetX || xPosDelta > offSetX + curveAreaBounds.width))
				|| (yPosDelta != 0 && (yPosDelta < offSetY || yPosDelta > offSetY + curveAreaBounds.height))	) {
			this.redrawGraphics();
			xPosMeasure = xPosDelta = 0;
		}
		else {
			if (xPosMeasure > 0) {
				eraseVerticalLine(xPosMeasure, 0, curveAreaBounds.height, 1);
				eraseHorizontalLine(yPosMeasure, 0, curveAreaBounds.width, 1);
			}
			if (xPosDelta > 0) {
				eraseVerticalLine(xPosDelta, 0, curveAreaBounds.height, 1);
				eraseHorizontalLine(yPosDelta, 0, curveAreaBounds.width, 1);
			}
		}
	}

	/**
	 * query the graphics window type
	 * @return the type TYPE_NORMALE | TYPE_COMPARE
	 */
	public int getType() {
		return type;
	}

	/**
	 * switch graphics window mouse mode
	 * @param mode MODE_RESET, MODE_ZOOM, MODE_MEASURE, MODE_DELTA_MEASURE
	 */
	public void setModeState(int mode) {
		this.cleanMeasurementPointer();
		switch (mode) {
		case MODE_ZOOM:
			this.isZoomMouse = true;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			break;
		case MODE_MEASURE:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			break;
		case MODE_MEASURE_DELTA:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = true;
			this.isPanMouse = false;
			break;
		case MODE_PAN:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = true;
			break;
		case MODE_RESET:
		default:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			application.setStatusMessage("");
			break;
		}
	}

	/**
	 * check input x,y value against curve are bounds and correct to bound if required
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		if(log.isLoggable(Level.FINER)) log.finer("in  xPos = " + xPos + " yPos = " + yPos);
		xPos = xPos - offSetX;
		yPos = yPos - offSetY;
		int minX = 0;
		int maxX = curveAreaBounds.width - 1;
		int minY = 0;
		int maxY = curveAreaBounds.height - 1;
		if (xPos < minX || xPos > maxX) {
			xPos = xPos < minX ? minX : maxX;
		}
		if (yPos < minY || yPos > maxY) {
			yPos = yPos < minY ? minY : maxY;
		}
		if(log.isLoggable(Level.FINER)) log.finer("out xPos = " + xPos + " yPos = " + yPos);
		return new Point(xPos, yPos);
	}
}
