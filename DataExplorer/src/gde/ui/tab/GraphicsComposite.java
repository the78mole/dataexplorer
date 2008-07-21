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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CurveUtils;
import osde.utils.TimeLine;

/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class GraphicsComposite extends Composite {
	final static Logger						log											= Logger.getLogger(GraphicsComposite.class.getName());
	
	public final static int				MODE_RESET							= 0;
	public final static int				MODE_ZOOM								= 1;
	public final static int				MODE_MEASURE						= 2;
	public final static int				MODE_MEASURE_DELTA			= 3;
	public final static int				MODE_PAN								= 4;
	public final static int				MODE_CUT_LEFT						= 6;
	public final static int				MODE_CUT_RIGHT					= 7;

	final	OpenSerialDataExplorer	application 						= OpenSerialDataExplorer.getInstance();
	final Channels								channels								= Channels.getInstance();
	final TimeLine								timeLine								= new TimeLine();
	final SashForm								graphicSashForm;
	final int											windowType;

	// drawing canvas
	Text													recordSetHeader;
	Text													recordSetComment;
	Canvas												graphicCanvas;
	int														headerHeight						= 0;
	int														headerGap								= 0;
	int														commentHeight						= 0;
	int														commentGap							= 0;
	String												oldRecordSetHeader, oldRecordSetComment;
	Point													oldSize = new Point(0,0); // composite size - control resized
	
	// update graphics only area required
	int 													oldNumberVisibleDisplayable	= 0;
	RecordSet											oldActiveRecordSet	= null;
	int 													numScaleLeft = 0;
	HashMap<String, Integer>			scaleTicks = new HashMap<String, Integer>();
	boolean												oldZoomLevel = false;

	// mouse actions
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

	GraphicsComposite(final SashForm useParent, int useWindowType) {
		super(useParent, SWT.NONE);
		this.graphicSashForm = useParent;
		this.windowType = useWindowType;

		SWTResourceManager.registerResourceUser(this);
		init();
	}

	void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
//		this.addPaintListener(new PaintListener() {
//			public void paintControl(PaintEvent evt) {
//				log.fine("GraphicsComposite.paintControl() = " + evt);
//				doRedrawGraphics();
//			}
//		});
		this.addControlListener(new ControlListener() {
			public void controlResized(ControlEvent evt) {
				log.finer("GraphicsComposite.controlResized() = " + evt);
				Point size = GraphicsComposite.this.getSize();
				log.finer(GraphicsComposite.this.oldSize + " - " + size);
				if (!GraphicsComposite.this.oldSize.equals(size)) {
					log.finer(GraphicsComposite.this.oldSize + " - " + size);
					GraphicsComposite.this.oldSize = size;
					setComponentBounds();
					doRedrawGraphics();
				}
			}
			public void controlMoved(ControlEvent evt) {
				log.finest("GraphicsComposite.controlMoved() = " + evt);
			}
		});
		this.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				log.finer("graphicsComposite.helpRequested " + evt); 			//$NON-NLS-1$
				if (GraphicsComposite.this.windowType == GraphicsWindow.TYPE_NORMAL)
					GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); 	//$NON-NLS-1$ //$NON-NLS-2$
				else
					GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_9.html"); 	//$NON-NLS-1$ //$NON-NLS-2$
			}
		});

		{
			this.recordSetHeader = new Text(this, SWT.SINGLE | SWT.CENTER);
			this.recordSetHeader.setFont(SWTResourceManager.getFont("Sans Serif", 12, 1, false, false)); //$NON-NLS-1$
			this.recordSetHeader.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
			this.recordSetHeader.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.finer("recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					//System.out.println("width = " + GraphicsComposite.this.getSize().x);
					if (GraphicsComposite.this.channels.getActiveChannel() != null) {
						RecordSet recordSet = GraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
						if (recordSet != null && (GraphicsComposite.this.oldRecordSetHeader == null || !recordSet.getHeader().equals(GraphicsComposite.this.oldRecordSetHeader))) {
							GraphicsComposite.this.recordSetHeader.setText(recordSet.getHeader());
							GraphicsComposite.this.oldRecordSetHeader = recordSet.getHeader();
						}
					}
				}
			});
		}
		{
			this.graphicCanvas = new Canvas(this, SWT.NONE);
			this.graphicCanvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
			this.graphicCanvas.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.finer("graphicCanvas.helpRequested " + evt); //$NON-NLS-1$
					if (GraphicsComposite.this.windowType == GraphicsWindow.TYPE_NORMAL)
						GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					else
						GraphicsComposite.this.application.openHelpDialog("", "HelpInfo_9.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
				public void mouseMove(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
					mouseMoveAction(evt);
				}
			});
			this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
				public void mouseExit(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
					GraphicsComposite.this.graphicCanvas.setCursor(GraphicsComposite.this.application.getCursor());
				}
			});
			this.graphicCanvas.addMouseListener(new MouseAdapter() {
				public void mouseDown(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
					mouseDownAction(evt);
				}

				public void mouseUp(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.finest("graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					mouseUpAction(evt);
				}
			});
			this.graphicCanvas.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.finer("graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
					//System.out.println("width = " + GraphicsComposite.this.getSize().x);
					drawAreaPaintControl(evt);
				}
			});
		}
		{
			this.recordSetComment = new Text(this, SWT.MULTI | SWT.LEFT);
			this.recordSetComment.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW); // light yellow
			this.recordSetComment.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.finest("recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					//System.out.println("width = " + GraphicsComposite.this.getSize().x);
					GraphicsComposite.this.recordSetComment.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

					if (GraphicsComposite.this.channels.getActiveChannel() != null) {
						RecordSet recordSet = GraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
						if (recordSet != null && (GraphicsComposite.this.oldRecordSetComment == null || !recordSet.getRecordSetDescription().equals(GraphicsComposite.this.oldRecordSetComment))) {
							GraphicsComposite.this.recordSetComment.setText(recordSet.getRecordSetDescription());
							GraphicsComposite.this.oldRecordSetComment = recordSet.getRecordSetDescription();
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
					GraphicsComposite.this.recordSetComment.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
					if (GraphicsComposite.this.channels.getActiveChannel() != null) {
						//if (evt.character == SWT.CR || evt.character == '\0' || evt.character == '') {
							RecordSet recordSet = GraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
							if (recordSet != null) {
								recordSet.setRecordSetDescription(GraphicsComposite.this.recordSetComment.getText());
								recordSet.setUnsaved(RecordSet.UNSAVED_REASON_DATA);
							}
						//}
					}
				}
			});
		}
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records 
	 * @param evt
	 */
	synchronized void drawAreaPaintControl(PaintEvent evt) {
		GraphicsWindow.log.finer("drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$
		// Get the canvas and its dimensions
		Canvas canvas = (Canvas) evt.widget;
		this.canvasGC = SWTResourceManager.getGC(canvas, "curveArea_" + this.windowType); //$NON-NLS-1$

		Point canvasSize = canvas.getSize();
		int maxX = canvasSize.x - 5; // enable a small gap if no axis is shown 
		int maxY = canvasSize.y;
		GraphicsWindow.log.finer("canvas size = " + maxX + " x " + maxY); //$NON-NLS-1$ //$NON-NLS-2$

		RecordSet recordSet = null;
		switch (this.windowType) {
		case GraphicsWindow.TYPE_COMPARE:
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
	 * updates the graphics canvas, while repeatabel redraw calls it optimized to the required area
	 */
	void doRedrawGraphics() {
		if (Channels.getInstance().getActiveChannel() != null) {
			RecordSet activeRecordSet = Channels.getInstance().getActiveChannel().getActiveRecordSet();
			if (activeRecordSet != null) {
				boolean isFullUpdateRequired = false;
				int numberVisibleDisplayable = activeRecordSet.getNumberOfVisibleAndDisplayableRecords();
				if (this.oldActiveRecordSet != null && !this.oldActiveRecordSet.getName().equals(activeRecordSet.getName())) {
					this.scaleTicks = new HashMap<String, Integer>();
					isFullUpdateRequired = true;
					log.finer(this.oldActiveRecordSet.getName() + " != " + activeRecordSet.getName());
				}
				else if (this.oldZoomLevel != activeRecordSet.isZoomMode()) {
					log.finer("zoom mode changed");
					isFullUpdateRequired = true;
				}
				else if (this.numScaleLeft != activeRecordSet.getNumberVisibleWithAxisPosLeft()) {
					log.finer("numScaleLeft " + this.numScaleLeft + " activeRecordSet.getNumberVisibleWithAxisPosLeft " + activeRecordSet.getNumberVisibleWithAxisPosLeft());
					isFullUpdateRequired = true;
				}
				else if (this.oldNumberVisibleDisplayable != numberVisibleDisplayable) {
					isFullUpdateRequired = true;
					log.finer("numberVisibleDisplayable " + numberVisibleDisplayable + " oldNumberActiveVisible " + this.oldNumberVisibleDisplayable);
				}
				else {
					for (String recordKey : activeRecordSet.getVisibleRecordNames()) {
						Record record = activeRecordSet.get(recordKey);
						int numberScaleTicks = record.getNumberScaleTicks();
						int oldNumberScaleTicks = this.scaleTicks.get(recordKey) == null ? 0 : this.scaleTicks.get(recordKey);
						if (oldNumberScaleTicks == 0 || oldNumberScaleTicks != numberScaleTicks) {
							this.scaleTicks.remove(recordKey);
							this.scaleTicks.put(recordKey, numberScaleTicks);
							isFullUpdateRequired = true;
							log.finer("scale ticks  changed " + oldNumberScaleTicks + " != " + numberScaleTicks);
						}
					}
				}
				if (isFullUpdateRequired) {
					log.fine("redrawing full " + this.graphicCanvas.getClientArea());
					this.graphicCanvas.redraw();
				}
				else {
					Rectangle curveBounds = activeRecordSet.getDrawAreaBounds();
					int timeScaleHeight = 30;
					if (curveBounds != null) {
						//int height = this.graphicCanvas.getClientArea().height;
						this.graphicCanvas.redraw(curveBounds.x, curveBounds.y, curveBounds.width+10, curveBounds.height+timeScaleHeight, true);
						log.finer("refresh rect = " + new Rectangle(curveBounds.x, curveBounds.y, curveBounds.width, curveBounds.height+timeScaleHeight).toString());
					}
					else {
						log.finer("redrawing full curveBounds == null");
						this.graphicCanvas.redraw();
					}
				}
				this.oldNumberVisibleDisplayable = numberVisibleDisplayable;
				this.oldActiveRecordSet = activeRecordSet;
				this.numScaleLeft = activeRecordSet.getNumberVisibleWithAxisPosLeft();
				this.oldZoomLevel = activeRecordSet.isZoomMode();
			}
			else { // enable clear
				log.finer("recordSet == null");
				this.graphicCanvas.redraw();
			}
		}
		else { // enable clear
			log.finer("channel == null");
			this.graphicCanvas.redraw();
		}
	}

	/**
	 * draw the start pointer for measurement modes
	 * @param mode
	 * @param isRefresh
	 */
	public void drawMeasurePointer(int mode, boolean isRefresh) {
		this.setModeState(mode); // cleans old pointer if required

		// get the record set to work with
		boolean isGraphicsWindow = this.windowType == GraphicsWindow.TYPE_NORMAL;
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
			log.fine("initial xPosMeasure = " + this.xPosMeasure + " yPosMeasure = " + this.yPosMeasure); //$NON-NLS-1$ //$NON-NLS-2$

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
		boolean isGraphicsWindow = this.windowType == GraphicsWindow.TYPE_NORMAL;
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
	 * clean cutting edge pointer
	 */
	public void cleanCutPointer() {
		this.application.setStatusMessage(" "); //$NON-NLS-1$
		eraseVerticalLine(this.xPosCut, 0, this.curveAreaBounds.height, 2);
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
		if (log.isLoggable(Level.FINER)) log.finer("in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (log.isLoggable(Level.FINER)) log.finer("out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
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
							if (log.isLoggable(Level.FINER))
								log.finer(String.format("xDown = %d, evt.x = %d, xLast = %d  -  yDown = %d, evt.y = %d, yLast = %d", this.xDown, evt.x, this.xLast, this.yDown, evt.y, this.yLast)); //$NON-NLS-1$

							//clean obsolete rectangle
							int left = this.xLast - this.xDown > 0 ? this.xDown : this.xLast;
							int top = this.yLast - this.yDown > 0 ? this.yDown : this.yLast;
							int width = this.xLast - this.xDown > 0 ? this.xLast - this.xDown : this.xDown - this.xLast;
							int height = this.yLast - this.yDown > 0 ? this.yLast - this.yDown : this.yDown - this.yLast;
							if (log.isLoggable(Level.FINER)) log.finer("clean left = " + left + " top = " + top + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							eraseHorizontalLine(top, left, width + 1, 1);
							eraseVerticalLine(left, top, height + 1, 1);
							eraseHorizontalLine(top + height, left + 1, width, 1);
							eraseVerticalLine(left + width, top + 1, height, 1);

							left = evt.x - this.xDown > 0 ? this.xDown + this.offSetX : evt.x + this.offSetX;
							top = evt.y - this.yDown > 0 ? this.yDown + this.offSetY : evt.y + this.offSetY;
							width = evt.x - this.xDown > 0 ? evt.x - this.xDown : this.xDown - evt.x;
							height = evt.y - this.yDown > 0 ? evt.y - this.yDown : this.yDown - evt.y;
							if (log.isLoggable(Level.FINER))
								log.finer("draw  left = " + (left - this.offSetX) + " top = " + (top - this.offSetY) + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
							if (log.isLoggable(Level.FINER)) log.finer(" xDeltaPan = " + this.xDeltaPan + " yDeltaPan = " + this.yDeltaPan); //$NON-NLS-1$ //$NON-NLS-2$
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
						log.log(Level.WARNING, "mouse pointer out of range", e); //$NON-NLS-1$
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
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? activeChannel.getActiveRecordSet() : this.application.getCompareSet();
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
				if (log.isLoggable(Level.FINER)) log.finer("isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
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
					if (log.isLoggable(Level.FINER)) log.finer("zoom xStart = " + xStart + " xEnd = " + xEnd + " yMin = " + yMin + " yMax = " + yMax); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
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
						setModeState(GraphicsComposite.MODE_RESET);
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
						setModeState(GraphicsComposite.MODE_RESET);
					}
				}
				updatePanMenueButton();
				updateCutModeButtons();
				if (log.isLoggable(Level.FINER)) log.finer("isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
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
			RecordSet recordSet = (this.windowType == GraphicsWindow.TYPE_NORMAL) ? Channels.getInstance().getActiveChannel().getActiveRecordSet() : this.application.getCompareSet();
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
		setComponentBounds();
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
		setComponentBounds();
	}
	
	public void updateHeaderText(String newHeaderText) {
		this.recordSetHeader.setText(newHeaderText);
	}

	public void clearHeaderAndComment() {
		if (GraphicsComposite.this.channels.getActiveChannel() != null) {
			RecordSet recordSet = GraphicsComposite.this.channels.getActiveChannel().getActiveRecordSet();
			if (recordSet == null) {
				GraphicsComposite.this.recordSetComment.setText(OSDE.STRING_EMPTY);
				GraphicsComposite.this.recordSetHeader.setText(OSDE.STRING_EMPTY);
				GraphicsComposite.this.oldRecordSetHeader = null;
				GraphicsComposite.this.oldRecordSetComment = null;
			}
			GraphicsComposite.this.recordSetComment.redraw();
			GraphicsComposite.this.recordSetHeader.redraw();
		}
	}

	/**
	 * 
	 */
	void setComponentBounds() {
		Rectangle graphicsBounds = this.getClientArea();
		//this.application.setGraphicsSashFormWeights(this.graphicSashForm.getSize().x - graphicsBounds.width);
		int x = 0;
		int y = this.headerGap;
		int width = graphicsBounds.width-1;
		int height = this.headerHeight;
		this.recordSetHeader.setBounds(x, y, width, height);
		
		y = this.headerGap + this.headerHeight;
		height = graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight);
		this.graphicCanvas.setBounds(x, y, width, height);
		
		y =  this.headerGap + this.headerHeight + height;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width-20, height);
	}

}
