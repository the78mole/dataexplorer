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
import org.eclipse.swt.layout.GridLayout;
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
	
	private int														xDown = 0;
	private int														xUp = 0;
	private int														xLast = 0;
	private int														yDown = 0;
	private int														yUp = 0;
	private int														yLast = 0;
	private Image 												curveArea;
	private int														offSetX, offSetY;
	
	private int 													xPosMeasure = 0, yPosMeasure;
	private int														xPosDelta = 0, yPosDelta;
	private boolean												isMouseMeasure = false;
	private boolean												isMouseDeltaMeasure = false;
	


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
			GridLayout sashForm1Layout2 = new GridLayout();
			sashForm1Layout2.makeColumnsEqualWidth = true;
			graphicSashForm.setLayout(sashForm1Layout2);
			{ // curveSelector
				curveSelector = new Composite(graphicSashForm, SWT.BORDER);
				FormLayout curveSelectorLayout = new FormLayout();
				curveSelector.setLayout(curveSelectorLayout);
				GridData curveSelectorLData = new GridData();
				curveSelector.setLayoutData(curveSelectorLData);
				{
					curveSelectorHeader = new CLabel(curveSelector, SWT.NONE);
					curveSelectorHeader.setText("Kurvenselektor");
					//curveSelectorHeader.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					FormData curveSelectorHeaderLData = new FormData();
					curveSelectorHeaderLData.width = 145;
					curveSelectorHeaderLData.height = 26;
					curveSelectorHeaderLData.left = new FormAttachment(0, 1000, 0);
					curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 3);
					curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
					curveSelectorHeader.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					curveSelectorTable = new Table(curveSelector, SWT.SINGLE | SWT.CHECK);
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
							Point size = curveSelectorTable.getSize();
							tableSelectorColumn.setWidth(size.x);
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
						tableSelectorColumn.setWidth(100);
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
						if (recordSet != null && curveArea != null && (evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
							try {
								Canvas canvas = (Canvas) evt.widget;
								Rectangle drawAreaBounds = curveArea.getBounds();
								// check mouse within drawArea
								int minX = offSetX;
								int maxX = offSetX + drawAreaBounds.width - 1;
								int minY = offSetY;
								int maxY = offSetY + drawAreaBounds.height - 1;
								if (evt.x < minX || evt.x > maxX) {
									evt.x = evt.x < minX ? minX : maxX;
								}
								if (evt.y < minY || evt.y > maxY) {
									evt.y = evt.y < minY ? minY : maxY;
								}
								// get the graphics context and set the gc properties
								GC gc = SWTResourceManager.getGC(canvas, "curveArea_" + type);

								if (recordSet.isZoomMode()) {
									if (log.isLoggable(Level.FINER)) log.finer(String.format("xDown = %d, evt.x = %d, xLast = %d  -  yDown = %d, evt.y = %d, yLast = %d", xDown, evt.x, xLast, yDown, evt.y, yLast));

									gc.setLineWidth(1);
									gc.setLineStyle(SWT.LINE_DASH);
									if (xDown < evt.x && yDown < evt.y) { //top left -> bottom right
										if (xLast > evt.x && yLast >= yDown || yLast > evt.y && xLast >= xDown) {
											gc.drawImage(curveArea, xDown - offSetX, yDown - offSetY, xLast - xDown + 1, yLast - yDown + 1, xDown, yDown, xLast - xDown + 1, yLast - yDown + 1);
										}
										else
											gc.drawImage(curveArea, xDown - offSetX, yDown - offSetY, evt.x - xDown + 1, evt.y - yDown + 1, xDown, yDown, evt.x - xDown + 1, evt.y - yDown + 1);

										gc.drawRectangle(xDown, yDown, evt.x - xDown, evt.y - yDown);
									}
									if (xDown < evt.x && yDown > evt.y) { // bottom left -> top right
										if (xLast > evt.x && yDown >= yLast || yLast != 0 && yLast < evt.y && xLast >= xDown) {
											gc.drawImage(curveArea, xDown - offSetX, yLast - offSetY, xLast - xDown + 1, yDown - yLast + 1, xDown, yLast, xLast - xDown + 1, yDown - yLast + 1);
										}
										else
											gc.drawImage(curveArea, xDown - offSetX, evt.y - offSetY, evt.x - xDown + 1, yDown - evt.y + 1, xDown, evt.y, evt.x - xDown + 1, yDown - evt.y + 1);

										gc.drawRectangle(xDown, evt.y, evt.x - xDown, yDown - evt.y);
									}
									if (xDown > evt.x && yDown < evt.y) { //top right -> left bottom
										if (xLast != 0 && xLast < evt.x && yDown <= yLast || yLast > evt.y && xDown >= xLast) {
											gc.drawImage(curveArea, xLast - offSetX, yDown - offSetY, xDown - xLast + 1, yLast - yDown + 1, xLast, yDown, xDown - xLast + 1, yLast - yDown + 1);
										}
										else
											gc.drawImage(curveArea, evt.x - offSetX, yDown - offSetY, xDown - evt.x + 1, evt.y - yDown + 1, evt.x, yDown, xDown - evt.x + 1, evt.y - yDown + 1);

										gc.drawRectangle(evt.x, yDown, xDown - evt.x, evt.y - yDown);
									}
									if (xDown > evt.x && yDown > evt.y) { // bottom left -> top right
										if (xLast != 0 && xLast < evt.x && yDown >= yLast || yLast != 0 && yLast < evt.y) {
											gc.drawImage(curveArea, xLast - offSetX, yLast - offSetY, xDown - xLast + 1, yDown - yLast + 1, xLast, yLast, xDown - xLast + 1, yDown - yLast + 1);
										}
										else
											gc.drawImage(curveArea, evt.x - offSetX, evt.y - offSetY, xDown - evt.x + 1, yDown - evt.y + 1, evt.x, evt.y, xDown - evt.x + 1, yDown - evt.y + 1);

										gc.drawRectangle(evt.x, evt.y, xDown - evt.x, yDown - evt.y);
									}
									if (xDown < evt.x + 5 && xDown > evt.x - 5 && yLast != 0) { // overlap x-direction
										int top = yLast - yDown > 0 ? yDown - 5 : yLast - 5;
										top = top < minY ? minY : top;
										int delta = yLast - yDown > 0 ? yLast - yDown + 10 : yDown - yLast + 10;
										delta = (top + delta) > maxY ? (maxY - top) : delta;
										int left = (xDown - 5 - offSetX) < minX ? 0 : (xDown - 5 - offSetX);
										int width = (xDown + 11) > maxX ? (maxX - left - offSetX + 1) : 11;
										gc.drawImage(curveArea, left, top - offSetY, width, delta, left + offSetX, top, width, delta);
									}
									if (yDown < evt.y + 5 && yDown > evt.y - 5 && xLast != 0) { // overlap y-direction
										int left = xLast - xDown > 0 ? xDown - 5 : xLast - 5;
										left = left < minX ? minX : left;
										int delta = xLast - xDown > 0 ? xLast - xDown + 10 : xDown - xLast + 10;
										delta = (left + delta) > maxX ? (maxX - left) : delta;
										int top = (yDown - 5 - offSetY) < 0 ? 0 : (yDown - 5 - offSetY);
										int height = (top + offSetY + 11) > maxY ? (maxY - top - offSetY + 1) : 11;
										gc.drawImage(curveArea, left - offSetX, top, delta, height, left, top + offSetY, delta, height);
									}
									xLast = evt.x;
									yLast = evt.y;
								}
								else if (isMouseMeasure) {
									// clear old lines
									int left = (xPosMeasure - 2) < minX ? minX : xPosMeasure - 2;
									int width = (left + 5) > maxX ? maxX - xPosMeasure + 2 + 1 : 5;
									int top = yPosMeasure - 2 < minY ? offSetY + yPosMeasure - minY: yPosMeasure - 2;
									int height = yPosMeasure - 2 + 5 + offSetY > maxY ? maxY - yPosMeasure - 2 - offSetY + 5 - 1 : 5;
									gc.drawImage(curveArea, left - offSetX, 0, width, drawAreaBounds.height, left, minY, width, drawAreaBounds.height);
									gc.drawImage(curveArea, 0, top, drawAreaBounds.width, height, offSetX, top + offSetY, drawAreaBounds.width, height);

									Record record = recordSet.getRecord(recordSet.getRecordKeyMeasurement());

									if (recordSet.isDeltaMeasurementMode()) {
										// clear old delta measure lines
										left = (xPosDelta - 2) < minX ? minX : xPosDelta - 2;
										width = (left + 5) > maxX ? maxX - xPosDelta + 2 + 1 : 5;
										top = yPosDelta - 2 < minY ? offSetY + yPosDelta - minY: yPosDelta - 2;
										height = yPosDelta - 2 + 5 + offSetY > maxY ? maxY - yPosDelta - 2 - offSetY + 5 - 1 : 5;
										gc.drawImage(curveArea, left - offSetX, 0, width, drawAreaBounds.height, left, offSetY, width, drawAreaBounds.height);
										gc.drawImage(curveArea, 0, top, drawAreaBounds.width, height, offSetX, top + offSetY, drawAreaBounds.width, height);

										gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
										gc.drawLine(xPosDelta + offSetX, offSetY, xPosDelta + offSetX, drawAreaBounds.height + offSetY - 1);

										yPosDelta = record.getDisplayDataPoint(xPosDelta, drawAreaBounds, offSetY);
										gc.drawLine(offSetX, yPosDelta + offSetY, offSetX + drawAreaBounds.width - 1, yPosDelta + offSetY);

										gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
									}

									xPosMeasure = evt.x;
									gc.drawLine(xPosMeasure, offSetY, xPosMeasure, drawAreaBounds.height + offSetY);

									yPosMeasure = record.getDisplayDataPoint(xPosMeasure - offSetX, drawAreaBounds, offSetY);
									gc.drawLine(offSetX, yPosMeasure + offSetY, offSetX + drawAreaBounds.width - 1, yPosMeasure + offSetY);

									if (recordSet.isDeltaMeasurementMode()) {
										application.setStatusMessage("  " + record.getName() + " (delta) = " + record.getDisplayDeltaValue(yPosMeasure - yPosDelta, drawAreaBounds) + " " + record.getUnit());
									}
									else
										application.setStatusMessage("  " + record.getName() + " = " + record.getDisplayPointValue(yPosMeasure, drawAreaBounds) + " " + record.getUnit());
								}
								else if (isMouseDeltaMeasure) {
									// clear old delta measure lines
									int left = (xPosDelta - 2) < minX ? minX : xPosDelta - 2;
									int width = (left + 5) > maxX ? maxX - xPosDelta + 2 + 1 : 5;
									int top = yPosDelta - 2 < minY ? offSetY + yPosDelta - minY: yPosDelta - 2;
									int height = yPosDelta - 2 + 5 + offSetY > maxY ? maxY - yPosDelta - 2 - offSetY + 5 - 1 : 5;
									log.info("maxY - yPosMeasure - 2 - offSetY = " + (maxY - yPosMeasure - 2 - offSetY + 5) + " height = " + height + " maxY = " + maxY);
									gc.drawImage(curveArea, left - offSetX, 0, width, drawAreaBounds.height, left, offSetY, width, drawAreaBounds.height);
									gc.drawImage(curveArea, 0, top, drawAreaBounds.width, height, offSetX, top + offSetY, drawAreaBounds.width, height);
									
									// clear old measure lines
									left = (xPosMeasure - 2) < minX ? minX : xPosMeasure - 2;
									width = (left + 5) > maxX ? maxX - xPosMeasure + 2 + 1 : 5;
									top = yPosMeasure - 2 < minY ? offSetY + yPosMeasure - minY: yPosMeasure - 2;
									height = yPosMeasure - 2 + 5 + offSetY > maxY ? maxY - yPosMeasure - 2 - offSetY + 5 - 1 : 5;
									gc.drawImage(curveArea, left - offSetX, 0, width, drawAreaBounds.height, left, offSetY, width, drawAreaBounds.height);
									gc.drawImage(curveArea, 0, top, drawAreaBounds.width, height, offSetX, top + offSetY, drawAreaBounds.width, height);
									gc.drawLine(xPosMeasure + offSetX, offSetY, xPosMeasure + offSetX, drawAreaBounds.height + offSetY);

									Record record = recordSet.getRecord(recordSet.getRecordKeyMeasurement());
									yPosMeasure = record.getDisplayDataPoint(xPosMeasure, drawAreaBounds, offSetY);
									gc.drawLine(offSetX, yPosMeasure + offSetY, offSetX + drawAreaBounds.width - 1, yPosMeasure + offSetY);

									// update the new delta position
									xPosDelta = evt.x;
									gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
									gc.drawLine(xPosDelta, offSetY, xPosDelta, drawAreaBounds.height + offSetY - 1);

									yPosDelta = record.getDisplayDataPoint(xPosDelta - offSetX, drawAreaBounds, offSetY);
									gc.drawLine(offSetX, yPosDelta + offSetY, offSetX + drawAreaBounds.width - 1, yPosDelta + offSetY);

									application.setStatusMessage("  " + record.getName() + " (delta) = " + record.getDisplayDeltaValue(yPosMeasure - yPosDelta, drawAreaBounds) + " " + record.getUnit());
									gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
								}
							}
							catch (RuntimeException e) {
								log.log(Level.WARNING, "mouse pointer out of range", e);
							}
						}
					}
				});
				graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
					public void mouseExit(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseExit, event="+evt);
						application.setDefaultCursor();
					}
					public void mouseEnter(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseEnter, event="+evt);
						RecordSet recordSet = (type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
						if (recordSet != null && curveArea != null) {
							if (recordSet.isZoomMode())
								application.setCursor(SWT.CURSOR_CROSS);
						}
					}
				});
				graphicCanvas.addMouseListener(new MouseAdapter() {
					public void mouseDown(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseDown, event="+evt);
						if (curveArea != null) {
							xDown = evt.x;
							yDown = evt.y;
							// get the draw area size
							Rectangle drawAreaBounds = curveArea.getBounds();
							int minX = offSetX;
							int maxX = offSetX + drawAreaBounds.width - 1;
							int minY = offSetY;
							int maxY = offSetY + drawAreaBounds.height - 1;
							if (xDown < minX || xDown > maxX) {
								xDown = xDown < minX ? minX : maxX;
							}
							if (yDown < minY || yDown > maxY) {
								yDown = yDown < minY ? minY : maxY;
							}
							if(log.isLoggable(Level.FINE)) log.fine("xDown = " + xDown + " yDown = " + yDown);
							
							if (xPosMeasure + offSetX + 1 >= xDown && xPosMeasure + offSetX - 1 <= xDown) {
								isMouseMeasure = true;
								log.fine("isMouseMeasure = true");
							}
							else if (xPosDelta + offSetX + 1 >= xDown && xPosDelta + offSetX - 1 <= xDown) {
								isMouseDeltaMeasure = true;
								log.fine("isMouseDeltaMeasure = true");
							}

						}
					}
					public void mouseUp(MouseEvent evt) {
						if(log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseUp, event="+evt);
						RecordSet recordSet = (type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
						if (curveArea != null && recordSet != null) {
							xUp = evt.x;
							yUp = evt.y;
							// get the draw area size and check or correct 
							Rectangle drawAreaBounds = curveArea.getBounds();
							int minX = offSetX;
							int maxX = offSetX + drawAreaBounds.width - 1;
							int minY = offSetY;
							int maxY = offSetY + drawAreaBounds.height - 1;
							if (xUp < minX || xUp > maxX) {
								xUp = xUp < minX ? minX : maxX;
							}
							if (yUp < minY || yUp > maxY) {
								yUp = yUp < minY ? minY : maxY;
							}
							if(log.isLoggable(Level.FINER)) log.finer("xUp = " + xUp + " yUp = " + yUp);

							if (recordSet.isZoomMode()) {
								// sort the zoom values
								int xStart = ((xDown < xUp ? xDown : xUp) - offSetX) * 100 / drawAreaBounds.width;
								int xEnd = ((xDown > xUp ? xDown : xUp) - offSetX) * 100 / drawAreaBounds.width;
								int yMin = (drawAreaBounds.height - ((yDown > yUp ? yDown : yUp) - offSetY)) * 100 / drawAreaBounds.height;
								int yMax = (drawAreaBounds.height - ((yDown < yUp ? yDown : yUp) - offSetY)) * 100 / drawAreaBounds.height;
								if (log.isLoggable(Level.FINE)) log.fine(String.format("xStart = %d, xEnd = %d, yMin = %d, yMax = %d", xStart, xEnd, yMin, yMax));
								//channels.getActiveChannel().getActiveRecordSet().setZoomValues(xStart, xEnd, yMin, yMax);
								//graphicCanvas.redraw();
							}
							else if (isMouseMeasure) {
								isMouseMeasure = false;
								xPosMeasure = xPosMeasure - offSetX;
								log.fine("isMouseMeasure = false");
							}
							else if (isMouseDeltaMeasure) {
								isMouseDeltaMeasure = false;
								xPosDelta = xPosDelta - offSetX;
								log.fine("isMouseDeltaMeasure = false");
							}
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
			graphicSashForm.setWeights(new int[] { 9, 100 }); // 9:100  -> 9 == width required for curveSelectorTable
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
		int maxX = canvas.getSize().x - 5; // enable a small gap if no axis is shown 
		int maxY = canvas.getSize().y;
		log.fine("canvas size = " + maxX + " x " + maxY);

		//set the font to be used
		// do not use extra font since the OS window manager has control
		//evt.gc.setFont(this.font);

		switch (type) {
		case TYPE_COMPARE:
			if (application.getCompareSet() != null && application.getCompareSet().size() > 0) {
				drawCurves(application.getCompareSet(), evt.gc, canvas, maxX, maxY);
			}
			break;

		default: // TYPE_NORMAL
			if (channels.getActiveChannel() != null && channels.getActiveChannel().getActiveRecordSet() != null) {
				drawCurves(channels.getActiveChannel().getActiveRecordSet(), evt.gc, canvas, maxX, maxY);
			}
			break;
		}

	}

	/**
	 * method to draw the curves with it scales
	 * @param recordSet
	 * @param gc
	 * @param canvas
	 * @param maxX
	 * @param maxY
	 */
	private void drawCurves(RecordSet recordSet, GC gc, Canvas canvas, int maxX, int maxY) {
		int[] timeScale = timeLine.getScaleMaxTimeNumber(recordSet);
		int maxTimeNumber = timeScale[0];
		int scaleFactor = timeScale[1];

		//prepare measurement scales
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		String[] recordNames = recordSet.getRecordNames();
		for (String string : recordNames) {
			if (recordSet.getRecord(string).isVisible()) {
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

		// Calculate the horizontal area to used for plotting graphs
		int maxTime = maxTimeNumber; // alle 10 min/sec eine Markierung
		Point pt = gc.textExtent("000,00");
		int dataScaleWidth = pt.x + pt.y * 2 + 5; // space used for text and scales with description or legend
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;
		int x0 = (int) maxX - (maxX - spaceLeft) + 5; // enable a small gap if no axis is shown
		int xMax = (int) maxX - spaceRight;
		int fitTimeWidth = (xMax - x0) - ((xMax - x0) % 10); // make time line modulo 10 to enable every 10 min/sec a tick mark
		int width = fitTimeWidth; // make the time width  the width for the curves
		xMax = x0 + width;

		int verticalSpace = 3 * pt.y;// space used for text and scales with description or legend
		int spaceTop = 20;
		int spaceBot = verticalSpace;
		int y0 = (int) maxY - spaceBot;
		int yMax = (int) maxY - (maxY - spaceTop);
		int height = (y0 - yMax) - (y0 - yMax) % 10; // make modulo 20
		yMax = y0 - height;
		if (log.isLoggable(Level.FINE)) log.fine("draw area x0=" + x0 + ", y0=" + y0 + ",xMax=" + xMax + ", yMax=" + yMax + "width=" + width + ", height=" + height + ", timeWidth=" + fitTimeWidth);

		// draw x coordinate	- time scale
		timeLine.drawTimeLine(gc, x0, y0, fitTimeWidth, 0, maxTime, scaleFactor, OpenSerialDataExplorer.COLOR_BLACK);

		// draw curves for each active record
		recordSet.setCurveBounds(new Rectangle(x0, y0-height, width, height));
		log.fine("curve bounds = " + x0 + " " + (y0-height) + " " + width + " " + height);
		
		// get the image and prepare GC
		curveArea = SWTResourceManager.getImage(width, height);
		GC imgGC = SWTResourceManager.getGC(curveArea);
		imgGC.setBackground(gc.getBackground());
		// clear the image
		imgGC.fillRectangle(curveArea.getBounds());
		// draw clipping bounding 
		imgGC.setForeground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
		imgGC.drawLine(0, 0, width, 0);
		imgGC.drawLine(0, 0, 0, height);
		imgGC.drawLine(width-1, 0, width-1, height);

		for (String record : recordSet.getRecordNames()) {
			Record actualRecord = recordSet.getRecord(record);
			log.fine("drawing record = " + actualRecord.getName());
			if (actualRecord.isVisible() && actualRecord.isDisplayable()){
				CurveUtils.drawScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth);
				CurveUtils.drawCurve(actualRecord, imgGC, 0, height, width, height, recordSet.isCompareSet());
			}
		}
		gc.drawImage(curveArea, offSetX = x0, offSetY = y0-height);
	}

	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGrahics() {
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
					
					String[] recordKeys = device.getMeasurementNames();
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
	 * @return the drawArea
	 */
	public Image getDrawArea() {
		return curveArea;
	}

	/**
	 * @param evt
	 * @param recordSet
	 * @param record
	 */
	public void drawMeasurePointer() {
		RecordSet recordSet = (this.type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : application.getCompareSet();
		Record record = recordSet.get(recordSet.getRecordKeyMeasurement());
		
		Rectangle drawAreaBounds = curveArea.getBounds();
		// get the graphics context and set the gc properties
		GC gc = SWTResourceManager.getGC(graphicCanvas, "curveArea_" + this.type);
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_DASH);
		
		// clean old measurement pointer
		if (xPosMeasure != 0) {
			gc.drawImage(curveArea, xPosMeasure, 0, 1, drawAreaBounds.height, xPosMeasure + offSetX, offSetY, 1, drawAreaBounds.height);
			gc.drawImage(curveArea, 0, yPosMeasure, drawAreaBounds.width, 1, offSetX, yPosMeasure + offSetY, drawAreaBounds.width, 1);
		}
		if (xPosDelta != 0) {
			gc.drawImage(curveArea, xPosDelta, 0, 1, drawAreaBounds.height, xPosDelta + offSetX, offSetY, 1, drawAreaBounds.height);
			gc.drawImage(curveArea, 0, yPosDelta, drawAreaBounds.width, 1, offSetX, yPosDelta + offSetY, drawAreaBounds.width, 1);
		}
		
		if (recordSet.isMeasurementMode()) {
			// check mouse within drawArea
			xPosMeasure = drawAreaBounds.width / 4;
			log.fine("xPosMeasure = " + xPosMeasure);

			gc.drawLine(xPosMeasure + offSetX, 0 + offSetY, xPosMeasure + offSetX, drawAreaBounds.height + offSetY);

			yPosMeasure = record.getDisplayDataPoint(xPosMeasure, drawAreaBounds, offSetY);
			gc.drawLine(offSetX, yPosMeasure + offSetY, offSetX + drawAreaBounds.width - 1, yPosMeasure + offSetY);

			application.setStatusMessage("  " + record.getName() + " = " + record.getDisplayPointValue(yPosMeasure, drawAreaBounds) + " " + record.getUnit());
		}
		else if (recordSet.isDeltaMeasurementMode()) {
			// check mouse within drawArea
			xPosMeasure = drawAreaBounds.width / 4;
			xPosDelta = drawAreaBounds.width / 3 * 2;

			// measure position
			gc.drawLine(xPosMeasure + offSetX, 0 + offSetY, xPosMeasure + offSetX, drawAreaBounds.height + offSetY);

			yPosMeasure = record.getDisplayDataPoint(xPosMeasure, drawAreaBounds, offSetY);
			gc.drawLine(offSetX, yPosMeasure + offSetY, offSetX + drawAreaBounds.width - 1, yPosMeasure + offSetY);
			
			// delta position
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
			gc.drawLine(xPosDelta + offSetX, 0 + offSetY, xPosDelta + offSetX, drawAreaBounds.height + offSetY);
			
			yPosDelta = record.getDisplayDataPoint(xPosDelta, drawAreaBounds, offSetY);
			gc.drawLine(offSetX, yPosDelta + offSetY, offSetX + drawAreaBounds.width - 1, yPosDelta + offSetY);
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			application.setStatusMessage("  " + record.getName() + " (delta) = " + record.getDisplayDeltaValue(yPosMeasure - yPosDelta, drawAreaBounds) + " " + record.getUnit());
		}
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}
}
