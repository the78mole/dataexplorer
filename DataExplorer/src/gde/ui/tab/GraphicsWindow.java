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

import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.messages.MessageIds;
import osde.messages.Messages;
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
	final static Logger						log											= Logger.getLogger(GraphicsWindow.class.getName());

	public static final int				TYPE_NORMAL							= 0;
	public static final int				TYPE_COMPARE						= 1;
	public static final String		WINDOW_TYPE							= "window_type"; //$NON-NLS-1$

	public final static int				MODE_RESET							= 0;
	public final static int				MODE_ZOOM								= 1;
	public final static int				MODE_MEASURE						= 2;
	public final static int				MODE_MEASURE_DELTA			= 3;
	public final static int				MODE_PAN								= 4;
	public final static int				MODE_CUT_LEFT						= 6;
	public final static int				MODE_CUT_RIGHT					= 7;

	final TabFolder								displayTab;
	SashForm											graphicSashForm;
	// Curve Selector Table with popup menu
	Composite											curveSelector;
	CLabel												curveSelectorHeader;
	Table													curveSelectorTable;
	TableColumn										tableSelectorColumn;
	TabItem												graphic;
	Menu													popupmenu;
	CurveSelectorContextMenu			contextMenu;

	// drawing canvas
	Composite											graphicsComposite;
	Text													recordSetHeader;
	Text													recordSetComment;
	Canvas												graphicCanvas;
	int														headerHeight						= 0;
	int														headerGap								= 0;
	int														commentHeight						= 0;
	int														commentGap							= 0;
	String												oldRecordSetHeader, oldRecordSetComment;
	
	// update graphics only area required
	int 													oldNumberActiveVisible	= 0;
	RecordSet											oldActiveRecordSet	= null;
	int 													numScaleLeft = 0;
	class MinMaxValues {
		String min;
		String max;
		MinMaxValues(String newMin, String newMax) {
			this.min = newMin;
			this.max = newMax;
		}
	}
	HashMap<String, MinMaxValues>	minMaxValues = new HashMap<String, MinMaxValues>();

	final OpenSerialDataExplorer	application;
	final Channels								channels;
	final String									name;
	final TimeLine								timeLine								= new TimeLine();
	final int											type;
	boolean												isCurveSelectorEnabled	= true;
	int														selectorHeaderWidth;
	int														selectorColumnWidth;
	int[]													sashformWeights					= new int[] { 100, 1000 };

	int														xDown										= 0;
	int														xUp											= 0;
	int														xLast										= 0;
	int														yDown										= 0;
	int														yUp											= 0;
	int														yLast										= 0;
	int														offSetX, offSetY;
	Image													curveArea;
	GC														curveAreaGC;
	Rectangle											curveAreaBounds;
	GC														canvasGC;

	boolean												isLeftMouseMeasure			= false;
	boolean												isRightMouseMeasure			= false;
	int														xPosMeasure							= 0, yPosMeasure = 0;
	int														xPosDelta								= 0, yPosDelta = 0;

	boolean												isZoomMouse							= false;

	boolean												isPanMouse							= false;
	int														xDeltaPan								= 0;
	int														yDeltaPan								= 0;

	boolean												isLeftCutMode						= false;
	boolean												isRightCutMode					= false;
	int														xPosCut									= 0;
	
	public GraphicsWindow(TabFolder currentDisplayTab, int currentType, String useName) {
		this.displayTab = currentDisplayTab;
		this.type = currentType;
		this.name = useName;
		this.application = OpenSerialDataExplorer.getInstance();
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.graphic = new TabItem(this.displayTab, SWT.NONE);
		this.graphic.setText(this.name);
		SWTResourceManager.registerResourceUser(this.graphic);

		{ // graphicSashForm
			this.graphicSashForm = new SashForm(this.displayTab, SWT.HORIZONTAL);
			this.graphic.setControl(this.graphicSashForm);
			{ // curveSelector
				this.curveSelector = new Composite(this.graphicSashForm, SWT.BORDER);
				FormLayout curveSelectorLayout = new FormLayout();
				this.curveSelector.setLayout(curveSelectorLayout);
				GridData curveSelectorLData = new GridData();
				this.curveSelector.setLayoutData(curveSelectorLData);
				this.curveSelector.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						GraphicsWindow.log.finer("curveSelector.helpRequested " + evt); //$NON-NLS-1$
						GraphicsWindow.this.application.openHelpDialog("", "HelpInfo_41.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.curveSelectorHeader = new CLabel(this.curveSelector, SWT.NONE);
					this.curveSelectorHeader.setText(Messages.getString(MessageIds.OSDE_MSGT0254));
					this.curveSelectorHeader.pack();
					this.selectorHeaderWidth = this.curveSelectorHeader.getSize().x + 10;
					//curveSelectorHeader.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 10, 1, false, false));
					FormData curveSelectorHeaderLData = new FormData();
					curveSelectorHeaderLData.width = this.selectorHeaderWidth;
					curveSelectorHeaderLData.height = 24;
					curveSelectorHeaderLData.left = new FormAttachment(0, 1000, 0);
					curveSelectorHeaderLData.top = new FormAttachment(0, 1000, 3);
					this.curveSelectorHeader.setLayoutData(curveSelectorHeaderLData);
					this.curveSelectorHeader.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					this.curveSelectorTable = new Table(this.curveSelector, SWT.SINGLE | SWT.CHECK | SWT.EMBEDDED);
					this.curveSelectorTable.setLinesVisible(true);
					FormData curveTableLData = new FormData();
					curveTableLData.width = 82;
					curveTableLData.height = 457;
					curveTableLData.left = new FormAttachment(0, 1000, 4);
					curveTableLData.top = new FormAttachment(0, 1000, 37);
					curveTableLData.bottom = new FormAttachment(1000, 1000, 0);
					curveTableLData.right = new FormAttachment(1000, 1000, 0);
					this.curveSelectorTable.setLayoutData(curveTableLData);

					this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
					this.curveSelectorTable.setMenu(this.popupmenu);
					this.curveSelectorTable.layout();
					this.contextMenu = new CurveSelectorContextMenu();
					this.contextMenu.createMenu(this.popupmenu);
					this.curveSelectorTable.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							GraphicsWindow.log.finest("curveSelectorTable.paintControl, event=" + evt); //$NON-NLS-1$
							Point graphicsSize = GraphicsWindow.this.graphicsComposite.getSize();
							int x = 0;
							int y = GraphicsWindow.this.headerGap;
							int width = graphicsSize.x;
							int height = GraphicsWindow.this.headerHeight;
							GraphicsWindow.this.recordSetHeader.setBounds(x, y, width, height);
							
							y = GraphicsWindow.this.headerGap + GraphicsWindow.this.headerHeight;
							height = graphicsSize.y - (GraphicsWindow.this.headerGap + GraphicsWindow.this.commentGap + GraphicsWindow.this.commentHeight + GraphicsWindow.this.headerHeight);
							GraphicsWindow.this.graphicCanvas.setBounds(x, y, width, height);
							
							y =  GraphicsWindow.this.headerGap + GraphicsWindow.this.headerHeight + height;
							height = GraphicsWindow.this.commentHeight;
							GraphicsWindow.this.recordSetComment.setBounds(20, y, width-20, height);
							
							clearHeaderAndComment();
						}
					});
					this.curveSelectorTable.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							GraphicsWindow.log.finest("curveTable.widgetSelected, event=" + evt); //$NON-NLS-1$
							TableItem item = (TableItem) evt.item;
							String recordName = ((TableItem) evt.item).getText();
							GraphicsWindow.log.fine("selected = " + recordName); //$NON-NLS-1$
							GraphicsWindow.this.popupmenu.setData(OpenSerialDataExplorer.RECORD_NAME, recordName);
							GraphicsWindow.this.popupmenu.setData(OpenSerialDataExplorer.CURVE_SELECTION_ITEM, evt.item);
							if (item.getChecked() != (Boolean) item.getData(OpenSerialDataExplorer.OLD_STATE)) {
								Record activeRecord;
								switch (GraphicsWindow.this.type) {
								case TYPE_COMPARE:
									activeRecord = GraphicsWindow.this.application.getCompareSet().getRecord(recordName);
									break;

								default:
									activeRecord = GraphicsWindow.this.channels.getActiveChannel().getActiveRecordSet().getRecord(recordName);
									break;
								}
								if (activeRecord != null) {
									if (item.getChecked()) {
										activeRecord.setVisible(true);
										GraphicsWindow.this.popupmenu.getItem(0).setSelection(true);
										item.setData(OpenSerialDataExplorer.OLD_STATE, true);
										item.setData(GraphicsWindow.WINDOW_TYPE, GraphicsWindow.this.type);
										GraphicsWindow.this.redrawGraphics();
										GraphicsWindow.this.application.updateDigitalWindow();
										GraphicsWindow.this.application.updateAnalogWindow();
										GraphicsWindow.this.application.updateCellVoltageWindow();
										GraphicsWindow.this.application.updateFileCommentWindow();
									}
									else {
										activeRecord.setVisible(false);
										GraphicsWindow.this.popupmenu.getItem(0).setSelection(false);
										item.setData(OpenSerialDataExplorer.OLD_STATE, false);
										item.setData(GraphicsWindow.WINDOW_TYPE, GraphicsWindow.this.type);
										GraphicsWindow.this.redrawGraphics();
										GraphicsWindow.this.application.updateDigitalWindow();
										GraphicsWindow.this.application.updateAnalogWindow();
										GraphicsWindow.this.application.updateCellVoltageWindow();
										GraphicsWindow.this.application.updateFileCommentWindow();
									}
								}
								else {
									log.log(Level.WARNING, "GraphicsWindow.type = " + GraphicsWindow.this.type + " recordName = \"" + recordName + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								}
							}
						}
					});
					{
						this.tableSelectorColumn = new TableColumn(this.curveSelectorTable, SWT.LEFT);
						this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
					}
				}
			} // curveSelector
			{ // graphics composite
				this.graphicsComposite = new Composite(this.graphicSashForm, SWT.NONE);
				this.graphicsComposite.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
				this.graphicsComposite.addHelpListener(new HelpListener() {
					public void helpRequested(HelpEvent evt) {
						GraphicsWindow.log.finer("graphicsComposite.helpRequested " + evt); //$NON-NLS-1$
						if (GraphicsWindow.this.type == GraphicsWindow.TYPE_NORMAL)
							GraphicsWindow.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
						else
							GraphicsWindow.this.application.openHelpDialog("", "HelpInfo_9.html"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				});
				{
					this.recordSetHeader = new Text(this.graphicsComposite, SWT.SINGLE | SWT.CENTER);
					this.recordSetHeader.setFont(SWTResourceManager.getFont("Sans Serif", 12, 1, false, false)); //$NON-NLS-1$
					this.recordSetHeader.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
					this.recordSetHeader.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							GraphicsWindow.log.finest("recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
							if (GraphicsWindow.this.channels.getActiveChannel() != null) {
								RecordSet recordSet = GraphicsWindow.this.channels.getActiveChannel().getActiveRecordSet();
								if (recordSet != null && (GraphicsWindow.this.oldRecordSetHeader == null || !recordSet.getHeader().equals(GraphicsWindow.this.oldRecordSetHeader))) {
									GraphicsWindow.this.recordSetHeader.setText(recordSet.getHeader());
									GraphicsWindow.this.oldRecordSetHeader = recordSet.getHeader();
								}
							}
						}
					});
				}
				{
					this.graphicCanvas = new Canvas(this.graphicsComposite, SWT.NONE);
					this.graphicCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
					this.graphicCanvas.addHelpListener(new HelpListener() {
						public void helpRequested(HelpEvent evt) {
							GraphicsWindow.log.finer("graphicCanvas.helpRequested " + evt); //$NON-NLS-1$
							if (GraphicsWindow.this.type == GraphicsWindow.TYPE_NORMAL)
								GraphicsWindow.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
							else
								GraphicsWindow.this.application.openHelpDialog("", "HelpInfo_9.html"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});
					this.graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
						public void mouseMove(MouseEvent evt) {
							if (GraphicsWindow.log.isLoggable(Level.FINEST)) GraphicsWindow.log.finest("graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
							mouseMoveAction(evt);
						}
					});
					this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
						public void mouseExit(MouseEvent evt) {
							if (GraphicsWindow.log.isLoggable(Level.FINEST)) GraphicsWindow.log.finest("graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
							GraphicsWindow.this.graphicCanvas.setCursor(GraphicsWindow.this.application.getCursor());
						}
					});
					this.graphicCanvas.addMouseListener(new MouseAdapter() {
						public void mouseDown(MouseEvent evt) {
							if (GraphicsWindow.log.isLoggable(Level.FINEST)) GraphicsWindow.log.finest("graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
							mouseDownAction(evt);
						}

						public void mouseUp(MouseEvent evt) {
							if (GraphicsWindow.log.isLoggable(Level.FINEST)) GraphicsWindow.log.finest("graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
							mouseUpAction(evt);
						}
					});
					this.graphicCanvas.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
							drawAreaPaintControl(evt);
						}
					});
				}
				{
					this.recordSetComment = new Text(this.graphicsComposite, SWT.MULTI | SWT.LEFT);
					this.recordSetComment.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
					this.recordSetComment.addPaintListener(new PaintListener() {
						public void paintControl(PaintEvent evt) {
							if (GraphicsWindow.log.isLoggable(Level.FINEST)) GraphicsWindow.log.finest("recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
							GraphicsWindow.this.recordSetComment.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

							if (GraphicsWindow.this.channels.getActiveChannel() != null) {
								RecordSet recordSet = GraphicsWindow.this.channels.getActiveChannel().getActiveRecordSet();
								if (recordSet != null && (GraphicsWindow.this.oldRecordSetComment == null || !recordSet.getRecordSetDescription().equals(GraphicsWindow.this.oldRecordSetComment))) {
									GraphicsWindow.this.recordSetComment.setText(recordSet.getRecordSetDescription());
									GraphicsWindow.this.oldRecordSetComment = recordSet.getRecordSetDescription();
								}
							}
						}
					});
					this.recordSetComment.addHelpListener(new HelpListener() {
						public void helpRequested(HelpEvent evt) {
							log.finer("recordSetCommentText.helpRequested " + evt); //$NON-NLS-1$
							OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_10.html"); //$NON-NLS-1$ //$NON-NLS-2$
						}
					});

					this.recordSetComment.addKeyListener(new KeyAdapter() {
						public void keyPressed(KeyEvent evt) {
							if(log.isLoggable(Level.FINER)) log.finer("recordSetCommentText.keyPressed, event=" + evt); //$NON-NLS-1$
							GraphicsWindow.this.recordSetComment.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
							if (GraphicsWindow.this.channels.getActiveChannel() != null) {
								//if (evt.character == SWT.CR || evt.character == '\0' || evt.character == '') {
									RecordSet recordSet = GraphicsWindow.this.channels.getActiveChannel().getActiveRecordSet();
									if (recordSet != null) {
										recordSet.setRecordSetDescription(GraphicsWindow.this.recordSetComment.getText());
										recordSet.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
									}
								//}
							}
						}
					});
				}
			} // graphics composite
		} // graphicSashForm
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records 
	 * @param evt
	 */
	synchronized void drawAreaPaintControl(PaintEvent evt) {
		GraphicsWindow.log.finer("drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$
		// Get the canvas and its dimensions
		Canvas canvas = (Canvas) evt.widget;
		this.canvasGC = SWTResourceManager.getGC(canvas, "curveArea_" + this.type); //$NON-NLS-1$

		Point canvasSize = canvas.getSize();
		int maxX = canvasSize.x - 5; // enable a small gap if no axis is shown 
		int maxY = canvasSize.y;
		GraphicsWindow.log.finer("canvas size = " + maxX + " x " + maxY); //$NON-NLS-1$ //$NON-NLS-2$

		RecordSet recordSet = null;
		switch (this.type) {
		case TYPE_COMPARE:
			if (this.application.getCompareSet() != null && this.application.getCompareSet().size() > 0) {
				recordSet = this.application.getCompareSet();
			}
			break;

		default: // TYPE_NORMAL
			if (this.channels.getActiveChannel() != null && this.channels.getActiveChannel().getActiveRecordSet() != null) {
				recordSet = this.channels.getActiveChannel().getActiveRecordSet();
			}
			break;
		}
		if (recordSet != null) {
			// draw curves
			drawCurves(recordSet, maxX, maxY);
			if (recordSet.isMeasurementMode(recordSet.getRecordKeyMeasurement()) || recordSet.isDeltaMeasurementMode(recordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(GraphicsWindow.MODE_MEASURE, true);
			}
			else if (this.isLeftCutMode) {
				drawCutPointer(GraphicsWindow.MODE_CUT_LEFT, true, false);
			}
			else if (this.isRightCutMode) {
				drawCutPointer(GraphicsWindow.MODE_CUT_RIGHT, false, true);
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
		int[] timeScale = this.timeLine.getScaleMaxTimeNumber(recordSet);
		int maxTime = timeScale[0];
		int scaleFactor = timeScale[1];
		int timeFormat = timeScale[2];

		//prepare measurement scales
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (String recordKey : recordSet.getRecordNames()) {
			Record tmpRecord = recordSet.getRecord(recordKey);
			if (tmpRecord.isVisible() && tmpRecord.isDisplayable()) {
				GraphicsWindow.log.fine("==>> " + recordKey + " isVisible = " + tmpRecord.isVisible() + " isDisplayable = " + tmpRecord.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = numberCurvesRight > 0 && numberCurvesLeft == 0 ? 1 : 0;
		}
		GraphicsWindow.log.fine("nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		int dataScaleWidth; // space used for text and scales with description or legend
		int x0; // enable a small gap if no axis is shown
		int width; // make the time width  the width for the curves
		int y0;
		int height; // make modulo 20
		// draw x coordinate	- time scale
		int startTime, endTime;
		// Calculate the horizontal area to used for plotting graphs
		Point pt = this.canvasGC.textExtent("000,00"); //$NON-NLS-1$
		dataScaleWidth = pt.x + pt.y * 2 + 5;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;
		x0 = maxX - (maxX - spaceLeft) + 5;
		int xMax = maxX - spaceRight;
		int fitTimeWidth = (xMax - x0) - ((xMax - x0) % 10); // make time line modulo 10 to enable every 10 min/sec a tick mark
		width = fitTimeWidth;
		xMax = x0 + width;
		int verticalSpace = 3 * pt.y;// space used for text and scales with description or legend
		int spaceTop = 20;
		int spaceBot = verticalSpace;
		y0 = maxY - spaceBot;
		int yMax = maxY - (maxY - spaceTop);
		height = (y0 - yMax) - (y0 - yMax) % 10;
		yMax = y0 - height;
		if (GraphicsWindow.log.isLoggable(Level.FINE))
			GraphicsWindow.log.fine("draw area x0=" + x0 + ", y0=" + y0 + ",xMax=" + xMax + ", yMax=" + yMax + "width=" + width + ", height=" + height + ", timeWidth=" + fitTimeWidth); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		// draw curves for each active record
		recordSet.setDrawAreaBounds(new Rectangle(x0, y0 - height, width, height));
		if (GraphicsWindow.log.isLoggable(Level.FINE)) GraphicsWindow.log.fine("curve bounds = " + x0 + " " + (y0 - height) + " " + width + " " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		startTime = TimeLine.convertTimeInFormatNumber(recordSet.getStartTime(), timeFormat);
		endTime = startTime + maxTime;
		this.timeLine.drawTimeLine(recordSet, this.canvasGC, x0, y0, fitTimeWidth, startTime, endTime, scaleFactor, timeFormat, OpenSerialDataExplorer.COLOR_BLACK);

		// get the image and prepare GC
		this.curveArea = SWTResourceManager.getImage(width, height);
		this.curveAreaGC = SWTResourceManager.getGC(this.curveArea);
		this.curveAreaBounds = this.curveArea.getBounds();

		// clear the image
		this.curveAreaGC.setBackground(this.canvasGC.getBackground());
		this.curveAreaGC.fillRectangle(this.curveArea.getBounds());

		// draw draw area bounding 
		if(System.getProperty("os.name").toLowerCase().startsWith("windows"))  //$NON-NLS-1$ //$NON-NLS-2$
			this.curveAreaGC.setForeground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
		else
			this.curveAreaGC.setForeground(OpenSerialDataExplorer.COLOR_GREY);
		this.curveAreaGC.drawLine(0, 0, width, 0);
		this.curveAreaGC.drawLine(0, 0, 0, height - 1);
		this.curveAreaGC.drawLine(width - 1, 0, width - 1, height - 1);

		// prepare grid lines
		this.offSetX = x0;
		this.offSetY = y0 - height;
		int[] dash = Settings.getInstance().getGridDashStyle();

		// check for activated time grid
		if (recordSet.getTimeGridType() > 0) drawTimeGrid(recordSet, this.curveAreaGC, this.offSetX, height, dash);

		// check for activated horizontal grid
		boolean isCurveGridEnabled = recordSet.getHorizontalGridType() > 0;
		String curveGridRecordName = recordSet.getHorizontalGridRecordName();
		String[] recordSetNames = recordSet.getRecordNames().clone();
		// sort the record set names to get the one which makes the grid lines drawn first
		for (int i = 0; i < recordSetNames.length; i++) {
			if (recordSetNames[i].equals(curveGridRecordName)) {
				recordSetNames[i] = recordSetNames[0]; // exchange with record set at index 0
				recordSetNames[0] = curveGridRecordName; // replace with the one which makes the grid lines
				break;
			}
		}

		// draw each record using sorted record set names
		for (String record : recordSetNames) {
			Record actualRecord = recordSet.getRecord(record);
			GraphicsWindow.log.fine("drawing record = " + actualRecord.getName() + " isVisibel=" + actualRecord.isVisible() + " isDisplayable=" + actualRecord.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (isActualRecordEnabled) CurveUtils.drawScale(actualRecord, this.canvasGC, x0, y0, width, height, dataScaleWidth);

			if (isCurveGridEnabled && record.equals(curveGridRecordName)) // check for activated horizontal grid
				drawCurveGrid(recordSet, this.curveAreaGC, this.offSetY, width, dash);

			if (isActualRecordEnabled) CurveUtils.drawCurve(actualRecord, this.curveAreaGC, 0, height, width, height, recordSet.isCompareSet(), recordSet.isZoomMode());
		}

		this.canvasGC.drawImage(this.curveArea, this.offSetX, this.offSetY);

		if (startTime != 0) { // scaled window 
			String strStartTime = Messages.getString(MessageIds.OSDE_MSGT0255) + TimeLine.getFomatedTime(recordSet.getStartTime());
			Point point = this.canvasGC.textExtent(strStartTime);
			int yPosition = (int) (y0 + pt.y * 2.5);
			this.canvasGC.drawText(strStartTime, 10, yPosition - point.y / 2);
		}
	}

	/**
	 * draw horizontal (curve) grid lines according the vector prepared during daring specified curve scale 
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param useOffsetY the offset in vertical direction
	 * @param width
	 * @param dash to be used for the custom line style
	 */
	private void drawCurveGrid(RecordSet recordSet, GC gc, int useOffSetY, int width, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getHorizontalGridColor());
		//curveAreaGC.setLineStyle(recordSet.getHorizontalGridLineStyle());
		Vector<Integer> horizontalGridVector = recordSet.getHorizontalGrid();
		for (int i = 0; i < horizontalGridVector.size() - 1; i += recordSet.getHorizontalGridType()) {
			int y = horizontalGridVector.get(i);
			gc.drawLine(0, y - useOffSetY, width - 1, y - useOffSetY);
		}
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during drawing of time scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param height
	 * @param dash to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, int useOffSetX, int height, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x - useOffSetX, 0, x - useOffSetX, height - 1);
		}
	}

	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doRedrawGraphics();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					doRedrawGraphics();
				}
			});
		}
	}

	/**
	 * updates the gravics canvas, while repeatabel redraw calls it optimized to the required area
	 */
	void doRedrawGraphics() {
		if (Channels.getInstance().getActiveChannel() != null) {
			RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				boolean isFullUpdateRequired = false;
				if (this.oldActiveRecordSet != null && !this.oldActiveRecordSet.equals(activeRecordSet)) {
					this.minMaxValues = new HashMap<String, MinMaxValues>();
					isFullUpdateRequired = true;
				}
				if (this.numScaleLeft != activeRecordSet.getNumberVisibleWithAxisPosLeft()) {
					isFullUpdateRequired = true;
				}
				int numberActiveVisible = activeRecordSet.getVisibleRecordNames().length;
				if (this.oldNumberActiveVisible != numberActiveVisible) {
					isFullUpdateRequired = true;
				}
//				for (String recordKey : activeRecordSet.getVisibleRecordNames()) {
//					Record record = activeRecordSet.get(recordKey);
//					String minFormated = record.getFormatedMinDisplayValue(), maxFormatted = record.getFormatedMaxDisplayValue();
//					MinMaxValues minMax = this.minMaxValues.get(recordKey);
//					if (minMax == null || !minFormated.equals(minMax.min) || !maxFormatted.equals(minMax.max)) {
//						this.minMaxValues.remove(recordKey);
//						this.minMaxValues.put(recordKey, new MinMaxValues(minFormated, maxFormatted));
//						isFullUpdateRequired = true;
//					}
//				}
				if (isFullUpdateRequired) {
					doUpdateCurveSelectorTable();
					this.graphicCanvas.redraw();
				}
				else {
					doUpdateCurveSelectorTable();
					Rectangle curveBounds = activeRecordSet.getDrawAreaBounds();
					int timeScaleHeight = 40;
					if (curveBounds != null) {
						//int height = this.graphicCanvas.getClientArea().height;
						this.graphicCanvas.redraw(curveBounds.x, curveBounds.y, curveBounds.width+10, curveBounds.height+timeScaleHeight+10, true);
						log.finer("refresh rect = " + new Rectangle(curveBounds.x, curveBounds.y, curveBounds.width, curveBounds.height+timeScaleHeight).toString());
					}
					else
						this.graphicCanvas.redraw();
				}
				this.oldNumberActiveVisible = numberActiveVisible;
				this.oldActiveRecordSet = activeRecordSet;
				this.numScaleLeft = activeRecordSet.getNumberVisibleWithAxisPosLeft();
			}
			else { // enable clear
				doUpdateCurveSelectorTable();
				GraphicsWindow.this.graphicCanvas.redraw();
			}
		}
		else { // enable clear
			doUpdateCurveSelectorTable();
			GraphicsWindow.this.graphicCanvas.redraw();
		}
	}

	/**
	 * method to update the curves displayed in the curve selector panel 
	 */
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doUpdateCurveSelectorTable();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * executes the update of the curve selector table
	 */
	void doUpdateCurveSelectorTable() {
		IDevice device = this.application.getActiveDevice();
		int itemWidth = this.selectorHeaderWidth;
		RecordSet recordSet = this.type == GraphicsWindow.TYPE_NORMAL ? this.channels.getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
		if (this.isCurveSelectorEnabled && recordSet != null && device != null) {
			this.curveSelectorTable.removeAll();
			this.curveSelectorHeader.pack(true);
			itemWidth = this.selectorHeaderWidth = this.curveSelectorHeader.getSize().x;

			String[] recordKeys = recordSet.getRecordNames();
			for (int i = 0; i < recordSet.size(); i++) {
				Record record;
				switch (this.type) {
				case TYPE_COMPARE:
					String recordKey = recordSet.getFirstRecordName().split("_")[0]; //$NON-NLS-1$
					record = recordSet.getRecord(recordKey + "_" + i); //$NON-NLS-1$
					break;

				default: // TYPE_NORMAL
					record = recordSet.getRecord(recordKeys[i]);
					break;
				}
				if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer(record.getName());

				TableItem item = new TableItem(this.curveSelectorTable, SWT.NULL);
				item.setForeground(record.getColor());
				item.setText(record.getName());
				//curveSelectorTable.pack();
				//log.info(item.getText() + OSDE.STRING_BLANK + item.getBounds().width);
				if (itemWidth < item.getBounds().width) itemWidth = item.getBounds().width;
				//log.info(item.getText() + OSDE.STRING_BLANK + itemWidth);
				//item.setImage(SWTResourceManager.getImage("osde/resource/LineWidth1.jpg"));
				if (record.isDisplayable()) {
					if (record.isVisible()) {
						item.setChecked(true);
						item.setData(OpenSerialDataExplorer.OLD_STATE, true);
						item.setData(GraphicsWindow.WINDOW_TYPE, this.type);
					}
					else {
						item.setChecked(false);
						item.setData(OpenSerialDataExplorer.OLD_STATE, false);
						item.setData(GraphicsWindow.WINDOW_TYPE, this.type);
					}
				}
				else {
					item.setChecked(false);
					item.setData(OpenSerialDataExplorer.OLD_STATE, false);
					item.setData(GraphicsWindow.WINDOW_TYPE, this.type);
					item.dispose();
				}
			}
		}
		else
			this.curveSelectorTable.removeAll();

		this.selectorColumnWidth = itemWidth + 30;
		if (GraphicsWindow.log.isLoggable(Level.FINER)) {
			GraphicsWindow.log.finer("curveSelectorTable width = " + this.selectorColumnWidth); //$NON-NLS-1$
			GraphicsWindow.log.finer("graphicSashForm width = " + this.graphicSashForm.getSize().x); //$NON-NLS-1$
		}
		if (this.isCurveSelectorEnabled) {
			int sashformWidth = this.graphicSashForm.getSize().x > 100 ? this.graphicSashForm.getSize().x : this.selectorColumnWidth * 10;
			this.curveSelectorHeader.setSize(this.selectorColumnWidth, this.curveSelectorHeader.getSize().y);
			this.tableSelectorColumn.setWidth(this.selectorColumnWidth);
			this.sashformWeights = new int[] { this.selectorColumnWidth, sashformWidth - this.selectorColumnWidth };
			this.graphicSashForm.setWeights(this.sashformWeights);
		}
	}

	public Canvas getGraphicCanvas() {
		return this.graphicCanvas;
	}

	public boolean isCurveSelectorEnabled() {
		return this.isCurveSelectorEnabled;
	}

	public void setCurveSelectorEnabled(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
	}

	public SashForm getGraphicSashForm() {
		return this.graphicSashForm;
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
		RecordSet recordSet = isGraphicsWindow ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
		String measureRecordKey = recordSet.getRecordKeyMeasurement();
		Record record = recordSet.get(measureRecordKey);

		// set the gc properties
		this.canvasGC.setLineWidth(1);
		this.canvasGC.setLineStyle(SWT.LINE_DASH);
		this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

		if (recordSet.isMeasurementMode(measureRecordKey)) {
			// initial measure position
			this.xPosMeasure = isRefresh ? this.xPosMeasure : this.curveAreaBounds.width / 4;
			this.yPosMeasure = record.getDisplayPointDataValue(this.xPosMeasure, this.curveAreaBounds);
			GraphicsWindow.log.fine("initial xPosMeasure = " + this.xPosMeasure + " yPosMeasure = " + this.yPosMeasure); //$NON-NLS-1$ //$NON-NLS-2$

			drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

			this.application.setStatusMessage(Messages.getString(
					MessageIds.OSDE_MSGT0256, 
					new Object[] { record.getName(), record.getDisplayPointValueString(this.yPosMeasure, this.curveAreaBounds), record.getUnit(), recordSet.getDisplayPointTime(this.xPosMeasure) }
			));
		}
		else if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
			this.xPosMeasure = isRefresh ? this.xPosMeasure : this.curveAreaBounds.width / 4;
			this.yPosMeasure = record.getDisplayPointDataValue(this.xPosMeasure, this.curveAreaBounds);

			// measure position
			drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

			// delta position
			this.xPosDelta = isRefresh ? this.xPosDelta : this.curveAreaBounds.width / 3 * 2;
			this.yPosDelta = record.getDisplayPointDataValue(this.xPosDelta, this.curveAreaBounds);

			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
			drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);
			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			this.application.setStatusMessage(
					Messages.getString(MessageIds.OSDE_MSGT0257, new Object[] { record.getName(), record.getDisplayDeltaValue(this.yPosMeasure - this.yPosDelta, this.curveAreaBounds), 
					record.getUnit(), record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta), this.curveAreaBounds), record.getUnit() }
			));
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
		this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX, posFromTop + this.offSetY + length - 1);
	}

	/**
	 * draws vertical line as defined relative to curve draw area, where there is an offset from left and an offset from top 
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 */
	private void drawHorizontalLine(int posFromTop, int posFromLeft, int length) {
		this.canvasGC.drawLine(posFromLeft + this.offSetX, posFromTop + this.offSetY, posFromLeft + this.offSetX + length - 1, posFromTop + this.offSetY);
	}

	/**
	 * erase a vertical line by re-drawing the curve area image 
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 * @param lineWidth
	 */
	private void eraseVerticalLine(int posFromLeft, int posFromTop, int length, int lineWidth) {
		this.canvasGC.drawImage(this.curveArea, posFromLeft, posFromTop, lineWidth, length, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length);
	}

	/**
	 * erase a horizontal line by re-drawing the curve area image 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 * @param lineWidth
	 */
	private void eraseHorizontalLine(int posFromTop, int posFromLeft, int length, int lineWidth) {
		this.canvasGC.drawImage(this.curveArea, posFromLeft, posFromTop, length, lineWidth, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth);
	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 */
	public void cleanMeasurementPointer() {
		try {
			if ((this.xPosMeasure != 0 && (this.xPosMeasure < this.offSetX || this.xPosMeasure > this.offSetX + this.curveAreaBounds.width))
					|| (this.yPosMeasure != 0 && (this.yPosMeasure < this.offSetY || this.yPosMeasure > this.offSetY + this.curveAreaBounds.height))
					|| (this.xPosDelta != 0 && (this.xPosDelta < this.offSetX || this.xPosDelta > this.offSetX + this.curveAreaBounds.width))
					|| (this.yPosDelta != 0 && (this.yPosDelta < this.offSetY || this.yPosDelta > this.offSetY + this.curveAreaBounds.height))) {
				this.redrawGraphics();
				this.xPosMeasure = this.xPosDelta = 0;
			}
			else {
				if (this.xPosMeasure > 0) {
					eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
					eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);
				}
				if (this.xPosDelta > 0) {
					eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
					eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);
				}
			}
		}
		catch (RuntimeException e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * draw the cut pointer for cut modes
	 * @param mode
	 * @param leftEnabled
	 * @param rightEnabled
	 */
	public void drawCutPointer(int mode, boolean leftEnabled, boolean rightEnabled) {
		this.setModeState(mode); // cleans old pointer if required

		// allow only get the record set to work with
		boolean isGraphicsWindow = this.type == GraphicsWindow.TYPE_NORMAL;
		if (isGraphicsWindow) {
			// set the gc properties
			this.canvasGC.setLineWidth(1);
			this.canvasGC.setLineStyle(SWT.LINE_SOLID);
			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));

			if (leftEnabled) {
				this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0258));
				//cleanCutPointer();
				this.xPosCut = this.xPosCut > 0 ? this.xPosCut : this.curveAreaBounds.width * 1 / 4;
				this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
				this.canvasGC.fillRectangle(0 + this.offSetX, 0 + this.offSetY, this.xPosCut, this.curveAreaBounds.height);
				this.canvasGC.setAdvanced(false);
				drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
			}
			else if (rightEnabled) {
				this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0259));
				//cleanCutPointer();
				this.xPosCut = this.xPosCut > 0 ? this.xPosCut : this.curveAreaBounds.width * 3 / 4;
				this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
				this.canvasGC.fillRectangle(this.xPosCut + this.offSetX, 0 + this.offSetY, this.curveAreaBounds.width - this.xPosCut, this.curveAreaBounds.height);
				this.canvasGC.setAdvanced(false);
				drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
			}
			else {
				cleanCutPointer();
			}
		}
	}

	/**
	 * 
	 */
	public void cleanCutPointer() {
		this.application.setStatusMessage(OSDE.STRING_BLANK);
		eraseVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height, 2);
	}

	/**
	 * query the graphics window type
	 * @return the type TYPE_NORMALE | TYPE_COMPARE
	 */
	public int getType() {
		return this.type;
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
		case MODE_CUT_LEFT:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = true;
			this.isRightCutMode = false;
			break;
		case MODE_CUT_RIGHT:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = true;
			break;
		case MODE_RESET:
		default:
			this.isZoomMouse = false;
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.isPanMouse = false;
			this.isLeftCutMode = false;
			this.isRightCutMode = false;
			this.application.setStatusMessage(OSDE.STRING_EMPTY);
			this.xPosCut = -1;
			updatePanMenueButton();
			updateCutModeButtons();
			//this.redrawGraphics();
			break;
		}
	}

	/**
	 * 
	 */
	private void updatePanMenueButton() {
		this.application.getMenuToolBar().enablePanButton(this.isZoomMouse);
	}

	/**
	 * check input x,y value against curve are bounds and correct to bound if required
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
		int tmpxPos = xPos - this.offSetX;
		int tmpyPos = yPos - this.offSetY;
		int minX = 0;
		int maxX = this.curveAreaBounds.width - 1;
		int minY = 0;
		int maxY = this.curveAreaBounds.height - 1;
		if (tmpxPos < minX || tmpxPos > maxX) {
			tmpxPos = tmpxPos < minX ? minX : maxX;
		}
		if (tmpyPos < minY || tmpyPos > maxY) {
			tmpyPos = tmpyPos < minY ? minY : maxY;
		}
		if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @return the selectorColumnWidth
	 */
	public int[] getSashformWeights() {
		return this.sashformWeights;
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.type == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
			if (recordSet != null && this.curveArea != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				evt.x = point.x;
				evt.y = point.y;

				String measureRecordKey = recordSet.getRecordKeyMeasurement();
				this.canvasGC.setLineWidth(1);
				this.canvasGC.setLineStyle(SWT.LINE_DASH);

				if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
					try {
						if (this.isZoomMouse && recordSet.isZoomMode()) {
							if (GraphicsWindow.log.isLoggable(Level.FINER))
								GraphicsWindow.log.finer(String.format("xDown = %d, evt.x = %d, xLast = %d  -  yDown = %d, evt.y = %d, yLast = %d", this.xDown, evt.x, this.xLast, this.yDown, evt.y, this.yLast)); //$NON-NLS-1$

							//clean obsolete rectangle
							int left = this.xLast - this.xDown > 0 ? this.xDown : this.xLast;
							int top = this.yLast - this.yDown > 0 ? this.yDown : this.yLast;
							int width = this.xLast - this.xDown > 0 ? this.xLast - this.xDown : this.xDown - this.xLast;
							int height = this.yLast - this.yDown > 0 ? this.yLast - this.yDown : this.yDown - this.yLast;
							if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("clean left = " + left + " top = " + top + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							eraseHorizontalLine(top, left, width + 1, 1);
							eraseVerticalLine(left, top, height + 1, 1);
							eraseHorizontalLine(top + height, left + 1, width, 1);
							eraseVerticalLine(left + width, top + 1, height, 1);

							left = evt.x - this.xDown > 0 ? this.xDown + this.offSetX : evt.x + this.offSetX;
							top = evt.y - this.yDown > 0 ? this.yDown + this.offSetY : evt.y + this.offSetY;
							width = evt.x - this.xDown > 0 ? evt.x - this.xDown : this.xDown - evt.x;
							height = evt.y - this.yDown > 0 ? evt.y - this.yDown : this.yDown - evt.y;
							if (GraphicsWindow.log.isLoggable(Level.FINER))
								GraphicsWindow.log.finer("draw  left = " + (left - this.offSetX) + " top = " + (top - this.offSetY) + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							this.canvasGC.drawRectangle(left, top, width, height);

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
							this.xLast = evt.x;
							this.yLast = evt.y;
						}
						else if (this.isLeftMouseMeasure) {
							Record record = recordSet.getRecord(measureRecordKey);
							// clear old measure lines
							eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
							//no change don't needs to be calculated, but the calculation limits to bounds
							this.yPosMeasure = record.getDisplayPointDataValue(this.xPosMeasure, this.curveAreaBounds);
							eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);

							if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
								// clear old delta measure lines
								eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
								//no change don't needs to be calculated, but the calculation limits to bounds
								this.yPosDelta = record.getDisplayPointDataValue(this.xPosDelta, this.curveAreaBounds);
								eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);

								this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
								drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
								drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);
								this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
							}
							// all obsolete lines are cleaned up now draw new position marker
							this.xPosMeasure = evt.x; // evt.x is already relative to curve area
							drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
							this.yPosMeasure = record.getDisplayPointDataValue(this.xPosMeasure, this.curveAreaBounds);
							drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);
							if (recordSet.isDeltaMeasurementMode(measureRecordKey)) {
								this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0257, 
										new Object[] { record.getName(), record.getDisplayDeltaValue(this.yPosMeasure - this.yPosDelta, this.curveAreaBounds), record.getUnit(), 
										record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta), this.curveAreaBounds), record.getUnit() }
								)); 
							}
							else {
								this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0256, 
										new Object[] { record.getName(), record.getDisplayPointValueString(this.yPosMeasure, this.curveAreaBounds),
										record.getUnit(), recordSet.getDisplayPointTime(this.xPosMeasure) }
								)); 
							}
						}
						else if (this.isRightMouseMeasure) {
							Record record = recordSet.getRecord(measureRecordKey);
							// clear old delta measure lines
							eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
							//no change don't needs to be calculated, but the calculation limits to bounds
							this.yPosMeasure = record.getDisplayPointDataValue(this.xPosMeasure, this.curveAreaBounds);
							eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);

							// clear old measure lines
							eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
							//no change don't needs to be calculated, but the calculation limits to bounds
							this.yPosDelta = record.getDisplayPointDataValue(this.xPosDelta, this.curveAreaBounds);
							eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);

							// always needs to draw measurement pointer
							drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
							//no change don't needs to be calculated yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
							drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

							// update the new delta position
							this.xPosDelta = evt.x; // evt.x is already relative to curve area
							this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
							drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
							this.yPosDelta = record.getDisplayPointDataValue(this.xPosDelta, this.curveAreaBounds);
							drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);
							this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

							this.application.setStatusMessage(Messages.getString(MessageIds.OSDE_MSGT0257, 
									new Object[] { record.getName(), record.getDisplayDeltaValue(this.yPosMeasure - this.yPosDelta, this.curveAreaBounds), record.getUnit(),
									record.getSlopeValue(new Point(this.xPosDelta - this.xPosMeasure, this.yPosMeasure - this.yPosDelta), this.curveAreaBounds), record.getUnit() }
							));
						}
						else if (this.isPanMouse) {
							this.xDeltaPan = (this.xLast != 0 && this.xLast != evt.x) ? (this.xDeltaPan + (this.xLast < evt.x ? -1 : 1)) : 0;
							this.yDeltaPan = (this.yLast != 0 && this.yLast != evt.y) ? (this.yDeltaPan + (this.yLast < evt.y ? 1 : -1)) : 0;
							if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer(" xDeltaPan = " + this.xDeltaPan + " yDeltaPan = " + this.yDeltaPan); //$NON-NLS-1$ //$NON-NLS-2$
							if ((this.xDeltaPan != 0 && this.xDeltaPan % 5 == 0) || (this.yDeltaPan != 0 && this.yDeltaPan % 5 == 0)) {
								recordSet.shift(this.xDeltaPan, this.yDeltaPan); // 10% each direction
								this.redrawGraphics(); //this.graphicCanvas.redraw();?
								this.xDeltaPan = this.yDeltaPan = 0;
							}
							this.xLast = evt.x;
							this.yLast = evt.y;
						}
						else if (this.isLeftCutMode) { 
							// clear old cut area
							if (evt.x < this.xPosCut) {
								this.canvasGC.drawImage(this.curveArea, evt.x, 0, this.xPosCut - evt.x + 1, this.curveAreaBounds.height, evt.x + this.offSetX, this.offSetY, this.xPosCut - evt.x + 1,
										this.curveAreaBounds.height);
							}
							else { // evt.x > this.xPosCut
								this.canvasGC.drawImage(this.curveArea, this.xPosCut, 0, evt.x - this.xPosCut, this.curveAreaBounds.height, this.xPosCut + this.offSetX, this.offSetY, evt.x - this.xPosCut,
										this.curveAreaBounds.height);
								this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
								this.canvasGC.fillRectangle(this.xPosCut + this.offSetX, 0 + this.offSetY, evt.x - this.xPosCut, this.curveAreaBounds.height);
								this.canvasGC.setAdvanced(false);
							}
							this.xPosCut = evt.x;
							this.canvasGC.setLineStyle(SWT.LINE_SOLID);
							drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
						}
						else if (this.isRightCutMode) {
							// clear old cut lines
							if (evt.x > this.xPosCut) {
								this.canvasGC.drawImage(this.curveArea, this.xPosCut, 0, evt.x - this.xPosCut, this.curveAreaBounds.height, this.offSetX + this.xPosCut, this.offSetY, evt.x - this.xPosCut,
										this.curveAreaBounds.height);
							}
							else { // evt.x < this.xPosCut
								this.canvasGC.drawImage(this.curveArea, evt.x, 0, this.xPosCut - evt.x + 1, this.curveAreaBounds.height, evt.x + this.offSetX, this.offSetY, this.xPosCut - evt.x + 1,
										this.curveAreaBounds.height);
								this.canvasGC.setBackgroundPattern(SWTResourceManager.getPattern(0, 0, 50, 50, SWT.COLOR_CYAN, 128, SWT.COLOR_WIDGET_BACKGROUND, 128));
								this.canvasGC.fillRectangle(evt.x + this.offSetX, 0 + this.offSetY, this.xPosCut - evt.x + 1, this.curveAreaBounds.height);
								this.canvasGC.setAdvanced(false);
							}
							this.xPosCut = evt.x;
							this.canvasGC.setLineStyle(SWT.LINE_SOLID);
							drawVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height);
						}
					}
					catch (RuntimeException e) {
						GraphicsWindow.log.log(Level.WARNING, "mouse pointer out of range", e); //$NON-NLS-1$
					}
				}
				else if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey))) {
					if (this.xPosMeasure + 1 >= evt.x && this.xPosMeasure - 1 <= evt.x || this.xPosDelta + 1 >= evt.x && this.xPosDelta - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor("osde/resource/MoveH.gif")); //$NON-NLS-1$
					}
					else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				}
				else if (this.isZoomMouse && !this.isPanMouse) {
					this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
				}
				else if (this.isPanMouse) {
					this.graphicCanvas.setCursor(SWTResourceManager.getCursor("osde/resource/Hand.gif")); //$NON-NLS-1$
				}
				else if (this.isLeftCutMode || this.isRightCutMode) {
					if (this.xPosCut + 1 >= evt.x && this.xPosCut - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor("osde/resource/MoveH.gif")); //$NON-NLS-1$
					}
					else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				}
				else {
					this.graphicCanvas.setCursor(this.application.getCursor());
				}
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseDownAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.type == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
			if (this.curveArea != null && recordSet != null) {
				String measureRecordKey = recordSet.getRecordKeyMeasurement();
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xDown = point.x;
				this.yDown = point.y;

				if (measureRecordKey != null && (recordSet.isMeasurementMode(measureRecordKey) || recordSet.isDeltaMeasurementMode(measureRecordKey)) && this.xPosMeasure + 1 >= this.xDown
						&& this.xPosMeasure - 1 <= this.xDown) { // snap mouse pointer
					this.isLeftMouseMeasure = true;
					this.isRightMouseMeasure = false;
				}
				else if (measureRecordKey != null && recordSet.isDeltaMeasurementMode(measureRecordKey) && this.xPosDelta + 1 >= this.xDown && this.xPosDelta - 1 <= this.xDown) { // snap mouse pointer
					this.isRightMouseMeasure = true;
					this.isLeftMouseMeasure = false;
				}
				else {
					this.isLeftMouseMeasure = false;
					this.isRightMouseMeasure = false;
				}
				if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
			if (this.curveArea != null && recordSet != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xUp = point.x;
				this.yUp = point.y;

				if (this.isZoomMouse) {
					// sort the zoom values
					int xStart = this.xDown < this.xUp ? this.xDown : this.xUp;
					int xEnd = this.xDown > this.xUp ? this.xDown + 1 : this.xUp + 1;
					int yMin = this.curveAreaBounds.height - 1 - (this.yDown > this.yUp ? this.yDown : this.yUp);
					int yMax = this.curveAreaBounds.height - (this.yDown < this.yUp ? this.yDown : this.yUp);
					if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					if (xEnd - xStart > 5 && yMax - yMin > 5) {
						recordSet.setZoomOffsetAndWidth(new Rectangle(xStart, yMin, xEnd - xStart, yMax - yMin));
						this.redrawGraphics(); //this.graphicCanvas.redraw();
					}
				}
				else if (this.isLeftMouseMeasure) {
					this.isLeftMouseMeasure = false;
					//application.setStatusMessage(OSDE.STRING_EMPTY);
				}
				else if (this.isRightMouseMeasure) {
					this.isRightMouseMeasure = false;
					//application.setStatusMessage(OSDE.STRING_EMPTY);
				}
				else if (this.isLeftCutMode) {
					if (SWT.OK == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.OSDE_MSGT0260))) {
						recordSet = recordSet.clone(recordSet.getPointIndexFromDisplayPoint(this.xUp), true);
						recordSet.setRecalculationRequired();
						this.channels.getActiveChannel().put(recordSet.getName(), recordSet);
						this.application.getMenuToolBar().addRecordSetName(recordSet.getName());
						this.channels.getActiveChannel().switchRecordSet(recordSet.getName());
						this.channels.getActiveChannel().applyTemplate(recordSet.getName());
						setModeState(GraphicsWindow.MODE_RESET);
					}
				}
				else if (this.isRightCutMode) {
					if (SWT.OK == this.application.openOkCancelMessageDialog(Messages.getString(MessageIds.OSDE_MSGT0260))) {
						recordSet = recordSet.clone(recordSet.getRecordZoomOffset() + recordSet.getPointIndexFromDisplayPoint(this.xUp), false);
						recordSet.setRecalculationRequired();
						this.channels.getActiveChannel().put(recordSet.getName(), recordSet);
						this.application.getMenuToolBar().addRecordSetName(recordSet.getName());
						this.channels.getActiveChannel().switchRecordSet(recordSet.getName());
						this.channels.getActiveChannel().applyTemplate(recordSet.getName());
						setModeState(GraphicsWindow.MODE_RESET);
					}
				}
				updatePanMenueButton();
				updateCutModeButtons();
				if (GraphicsWindow.log.isLoggable(Level.FINER)) GraphicsWindow.log.finer("isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * check if cut mode can be activated
	 * @param recordSet
	 */
	void updateCutModeButtons() {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.type == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
			if (this.curveArea != null && recordSet != null) {
				// 
				if (recordSet.isCutLeftEdgeEnabled()) {
					this.application.getMenuToolBar().enableCutButtons(true, false);
				}
				else if (recordSet.isCutRightEdgeEnabled()) {
					this.application.getMenuToolBar().enableCutButtons(false, true);
				}
				else {
					this.application.getMenuToolBar().enableCutButtons(false, false);
				}
			}
		}
	}
	
	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		if (enabled) {
			this.headerGap = 5;
			this.headerHeight = 20;
		}
		else {
			this.headerGap = 0;
			this.headerHeight = 0;
		}
		this.curveSelectorTable.redraw();
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		if (enabled) {
			this.commentGap = 10;
			this.commentHeight = 40;
		}
		else {
			this.commentGap = 0;
			this.commentHeight = 0;
		}		
		this.curveSelectorTable.redraw();
	}
	
	public void updateHeaderText(String newHeaderText) {
		this.recordSetHeader.setText(newHeaderText);
	}

	public void clearHeaderAndComment() {
		if (GraphicsWindow.this.channels.getActiveChannel() != null) {
			RecordSet recordSet = GraphicsWindow.this.channels.getActiveChannel().getActiveRecordSet();
			if (recordSet == null) {
				GraphicsWindow.this.recordSetComment.setText(OSDE.STRING_EMPTY);
				GraphicsWindow.this.recordSetHeader.setText(OSDE.STRING_EMPTY);
			}
			GraphicsWindow.this.recordSetComment.redraw();
			GraphicsWindow.this.recordSetHeader.redraw();
		}
	}
}
