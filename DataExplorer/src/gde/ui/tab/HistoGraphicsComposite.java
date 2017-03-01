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
    
    Copyright (c) 2016,2017 Thomas Eickert
****************************************************************************************/
package gde.ui.tab;

import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.HistoSet;
import gde.data.Record;
import gde.data.RecordSet;
import gde.data.TrailRecord;
import gde.data.TrailRecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.utils.GraphicsUtils;
import gde.utils.HistoCurveUtils;
import gde.utils.HistoTimeLine;
import gde.utils.LocalizedDateTime;
import gde.utils.LocalizedDateTime.DateTimePattern;
import gde.utils.StringHelper;

/**
 * curve definition table for the histo graphics window.
 * @author Thomas Eickert
 */
public class HistoGraphicsComposite extends Composite {
	private final static String	$CLASS_NAME					= HistoGraphicsComposite.class.getName();
	private final static Logger	log									= Logger.getLogger($CLASS_NAME);

	private final HistoSet			histoSet						= HistoSet.getInstance();
	private final DataExplorer	application					= DataExplorer.getInstance();
	private final Settings			settings						= Settings.getInstance();
	private final Channels			channels						= Channels.getInstance();
	private final HistoTimeLine	timeLine						= new HistoTimeLine();
	private final SashForm			graphicSashForm;
	private final int						windowType;

	Menu												popupmenu;
	TabAreaContextMenu					contextMenu;
	Color												curveAreaBackground;
	Color												surroundingBackground;
	Color												curveAreaBorderColor;

	// drawing canvas
	Text												graphicsHeader;
	Text												recordSetComment;

	Canvas											graphicCanvas;
	int													headerHeight				= 0;
	int													headerGap						= 0;
	int													commentHeight				= 0;
	int													commentGap					= 0;
	String											graphicsHeaderText, recordSetCommentText;
	Point												oldSize							= new Point(0, 0);												// composite size - control resized

	HashMap<String, Integer>		leftSideScales			= new HashMap<String, Integer>();
	HashMap<String, Integer>		rightSideScales			= new HashMap<String, Integer>();
	int													oldScopeLevel				= 0;
	boolean											oldZoomLevel				= false;

	// mouse actions
	int													xDown								= 0;
	int													xUp									= 0;
	int													xLast								= 0;
	int													yDown								= 0;
	int													yUp									= 0;
	int													yLast								= 0;
	int													leftLast						= 0;
	int													topLast							= 0;
	int													rightLast						= 0;
	int													bottomLast					= 0;
	int													offSetX, offSetY;
	Rectangle										canvasBounds;
	Image												canvasImage;
	GC													canvasImageGC;
	GC													canvasGC;
	Rectangle										curveAreaBounds			= new Rectangle(0, 0, 1, 1);

	boolean											isLeftMouseMeasure	= false;
	boolean											isRightMouseMeasure	= false;
	int													xPosMeasure					= 0, yPosMeasure = 0;
	int													xPosDelta						= 0, yPosDelta = 0;
	private long								timestampMeasure_ms, timestampDelta_ms;
	
	HistoGraphicsComposite(final SashForm useParent) {
		super(useParent, GraphicsWindow.TYPE_HISTO);
		SWTResourceManager.registerResourceUser(this);
		this.graphicSashForm = useParent;
		this.windowType = GraphicsWindow.TYPE_HISTO;

		//get the background colors
		this.curveAreaBackground = this.settings.getGraphicsCurveAreaBackground();
		this.surroundingBackground = this.settings.getGraphicsSurroundingBackground();
		this.curveAreaBorderColor = this.settings.getGraphicsCurvesBorderColor();

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();

		init();
	}

	void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(this.surroundingBackground);

		this.contextMenu.createMenu(this.popupmenu, this.windowType);

		// help lister does not get active on Composite as well as on Canvas
		this.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "GraphicsComposite.controlResized() = " + evt); //$NON-NLS-1$
				Rectangle clientRect = HistoGraphicsComposite.this.getClientArea();
				Point size = new Point(clientRect.width, clientRect.height);
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, HistoGraphicsComposite.this.oldSize + " - " + size); //$NON-NLS-1$
				if (!HistoGraphicsComposite.this.oldSize.equals(size)) {
					if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "size changed, update " + HistoGraphicsComposite.this.oldSize + " - " + size); //$NON-NLS-1$ //$NON-NLS-2$
					HistoGraphicsComposite.this.oldSize = size;
					setComponentBounds();
					doRedrawGraphics();
				}
			}
		});
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "GraphicsComposite.helpRequested " + evt); //$NON-NLS-1$
				switch (HistoGraphicsComposite.this.windowType) {
				default:
				case GraphicsWindow.TYPE_NORMAL:
					HistoGraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				}
			}
		});
		{
			this.graphicsHeader = new Text(this, SWT.SINGLE | SWT.CENTER);
			this.graphicsHeader.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 3, SWT.BOLD));
			this.graphicsHeader.setBackground(this.surroundingBackground);
			this.graphicsHeader.setMenu(this.popupmenu);
			this.graphicsHeader.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					final String dataDirName = HistoGraphicsComposite.this.histoSet.getValidatedDataDir() != null ? HistoGraphicsComposite.this.histoSet.getValidatedDataDir().getFileName().toString()
							: GDE.STRING_EMPTY;
					final String importDirName = HistoGraphicsComposite.this.histoSet.getValidatedImportDir() != null ? HistoGraphicsComposite.this.histoSet.getValidatedImportDir().getFileName().toString()
							: GDE.STRING_EMPTY;
					final String tmpHeader = dataDirName.isEmpty() || importDirName.isEmpty() ? dataDirName + importDirName : dataDirName + GDE.STRING_EMPTY + GDE.STRING_PLUS + GDE.STRING_EMPTY + importDirName;
					if (HistoGraphicsComposite.this.graphicsHeaderText == null || !tmpHeader.equals(HistoGraphicsComposite.this.graphicsHeaderText)) {
						HistoGraphicsComposite.this.graphicsHeader.setText(HistoGraphicsComposite.this.graphicsHeaderText = tmpHeader);
					}
				}
			});
		}
		{
			this.graphicCanvas = new Canvas(this, SWT.NONE);
			this.graphicCanvas.setBackground(this.surroundingBackground);
			this.graphicCanvas.setMenu(this.popupmenu);
			this.graphicCanvas.addMouseMoveListener(new MouseMoveListener() {
				@Override
				public void mouseMove(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
					mouseMoveAction(evt);
				}
			});
			this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseExit(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
					HistoGraphicsComposite.this.graphicCanvas.setCursor(HistoGraphicsComposite.this.application.getCursor());
				}
			});
			this.graphicCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1) {
						mouseDownAction(evt);
					}
				}

				@Override
				public void mouseUp(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1) {
						mouseUpAction(evt);
					}
				}
			});
			this.graphicCanvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
					// System.out.println("width = " + GraphicsComposite.this.getSize().x);
					try {
						drawAreaPaintControl(evt);
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			});
		}
		{
			this.recordSetComment = new Text(this, SWT.MULTI | SWT.LEFT);
			this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
			this.recordSetComment.setBackground(this.surroundingBackground);
			this.recordSetComment.setMenu(this.popupmenu);
			this.recordSetComment.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetComment.paintControl, event=" + evt); //$NON-NLS-1$
				}
			});

			this.recordSetComment.addHelpListener(new HelpListener() {
				@Override
				public void helpRequested(HelpEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetCommentText.helpRequested " + evt); //$NON-NLS-1$
					DataExplorer.getInstance().openHelpDialog("", "HelpInfo_11.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
		}
	}

	/**
	 * this method is called in case of an paint event (redraw) and draw the containing records 
	 * @param evt
	 */
	void drawAreaPaintControl(PaintEvent evt) {
		if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$
		// Get the canvas and its dimensions
		this.canvasBounds = this.graphicCanvas.getClientArea();
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "canvas size = " + this.canvasBounds); //$NON-NLS-1$

		if (this.canvasImage != null) this.canvasImage.dispose();
		this.canvasImage = new Image(GDE.display, this.canvasBounds);
		this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
		this.canvasImageGC.setBackground(this.surroundingBackground);
		this.canvasImageGC.fillRectangle(this.canvasBounds);
		this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		// get gc for other drawing operations
		this.canvasGC = new GC(this.graphicCanvas); // SWTResourceManager.getGC(this.graphicCanvas, "curveArea_" + this.windowType);

		TrailRecordSet trailRecordSet = null;
		if (this.channels.getActiveChannel() != null) {
			trailRecordSet = this.histoSet.getTrailRecordSet();
		}

		if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
			drawCurves(trailRecordSet, this.canvasBounds, this.canvasImageGC);
			this.canvasGC.drawImage(this.canvasImage, 0, 0);
			// changed curve selection may change the scale end values
			trailRecordSet.syncScaleOfSyncableRecords();

			if (trailRecordSet.isMeasurementMode(trailRecordSet.getRecordKeyMeasurement()) || trailRecordSet.isDeltaMeasurementMode(trailRecordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(trailRecordSet, GraphicsComposite.MODE_MEASURE, true);
			}
		}
		else
			this.canvasGC.drawImage(this.canvasImage, 0, 0);

		this.canvasGC.dispose();
		this.canvasImageGC.dispose();
		// this.canvasImage.dispose(); //zooming, marking, ... needs a reference to canvasImage
	}

	/**
	 * method to draw the curves with it scales and defines the curve area
	 * @param trailRecordSet the record set to be drawn
	 * @param bounds the bounds where the curves and scales are drawn
	 * @param gc the graphics context to be used for the graphics operations
	 */
	private void drawCurves(TrailRecordSet trailRecordSet, Rectangle bounds, GC gc) {
		long startInitTime = new Date().getTime();

		// calculate number of curve scales, left and right side
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (int i = 0; i < trailRecordSet.getRecordsSortedForDisplay().length; i++) {
			TrailRecord tmpRecord = (TrailRecord) trailRecordSet.getRecordsSortedForDisplay()[i];
			if (tmpRecord != null && tmpRecord.isScaleVisible()) {
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "==>> " + tmpRecord.getName() + " isScaleVisible = " + tmpRecord.isScaleVisible()); //$NON-NLS-1$ //$NON-NLS-2$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		// calculate the bounds left for the curves
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width - time scale
		int height; // y coordinate - make modulo 10 ??

		// calculate the horizontal space width to be used for the scales
		Point pt = gc.textExtent("-000,00"); //$NON-NLS-1$
		int horizontalGap = pt.x / 5;
		int horizontalNumberExtend = pt.x;
		int horizontalCaptionExtend = pt.y;
		dataScaleWidth = horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;

		// calculate the horizontal area available for plotting graphs
		int gapSide = 10; // free gap left or right side of the curves
		x0 = spaceLeft + (numberCurvesLeft > 0 ? gapSide / 2 : gapSide);// enable a small gap if no axis is shown
		xMax = bounds.width - spaceRight - (numberCurvesRight > 0 ? gapSide / 2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = 10; // free gap on top of the curves
		int gapBot = 3 * pt.y + 4; // space used for time scale text and scales with description or legend;
		y0 = bounds.height - yMax - gapBot;
		height = y0 - yMax; // recalculate due to modulo 10 ??
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		// set offset values used for mouse measurement pointers
		this.offSetX = x0;
		this.offSetY = y0 - height;

		if (trailRecordSet.getRecordDataSize(true) > 0) {
			// draw curves for each active record
			this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
			trailRecordSet.setDrawAreaBounds(this.curveAreaBounds);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curve bounds = " + this.curveAreaBounds); //$NON-NLS-1$

			gc.setBackground(this.curveAreaBackground);
			gc.fillRectangle(this.curveAreaBounds);
			gc.setBackground(this.surroundingBackground);

			this.timeLine.initialize(trailRecordSet, width, trailRecordSet.getFirstTimeStamp_ms(), trailRecordSet.getLastTimeStamp_ms());
			this.timeLine.drawTimeLine(gc, x0, y0);

			// draw draw area bounding
			gc.setForeground(this.curveAreaBorderColor);

			gc.drawLine(x0 - 1, yMax - 1, xMax + 1, yMax - 1);
			gc.drawLine(x0 - 1, yMax - 1, x0 - 1, y0);
			gc.drawLine(xMax + 1, yMax - 1, xMax + 1, y0);

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "draw init time   =  " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() - startInitTime))); //$NON-NLS-1$ //$NON-NLS-2$

			long startTime = new Date().getTime();
			drawTrailRecordSet(trailRecordSet, gc, dataScaleWidth, x0, y0, width, height);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "draw records time = " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() - startTime))); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * draws the visible curves for all measurements.
	 * supports multiple curves for one single measurement.
	 * was drawHistoRecordData
	 */
	private void drawTrailRecordSet(TrailRecordSet trailRecordSet, GC gc, int dataScaleWidth, int x0, int y0, int width, int height) {
		// check for activated horizontal grid
		boolean isCurveGridEnabled = trailRecordSet.getHorizontalGridType() > 0;

		// draw each record using sorted record set names
		boolean isDrawScaleInRecordColor = this.settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = this.settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = this.settings.isDrawNumbersInRecordColor();
		trailRecordSet.updateSyncRecordScale();
		for (int i = 0; i < trailRecordSet.getRecordsSortedForDisplay().length; i++) {
			TrailRecord actualRecord = (TrailRecord) trailRecordSet.getRecordsSortedForDisplay()[i];
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (log.isLoggable(Level.FINE) && isActualRecordEnabled)
				log.log(Level.FINE, "record=" + actualRecord.getName() + "  isVisible=" + actualRecord.isVisible() + " isDisplayable=" + actualRecord.isDisplayable() //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						+ " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$
			if (actualRecord.isScaleVisible())
				HistoCurveUtils.drawScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth, isDrawScaleInRecordColor, isDrawNameInRecordColor, isDrawNumbersInRecordColor);

			if (isCurveGridEnabled && actualRecord.getOrdinal() == trailRecordSet.getHorizontalGridRecordOrdinal()) // check for activated horizontal grid
				drawCurveGrid(trailRecordSet, gc, this.curveAreaBounds, this.settings.getGridDashStyle());

			if (isActualRecordEnabled) {
				// gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				// gc.drawRectangle(x0, y0-height, width, height);
				gc.setClipping(x0 - 1, y0 - height - 1, width + 2, height + 2);
				if (actualRecord.isTrailSuite()) {
					HistoCurveUtils.drawHistoSuite(actualRecord, gc, x0, y0, width, height, this.timeLine);
				}
				else {
					// CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
					HistoCurveUtils.drawHistoCurve(actualRecord, gc, x0, y0, width, height, this.timeLine);
				}
				gc.setClipping(this.canvasBounds);
			}
		}
	}

	/**
	 * draw horizontal (curve) grid lines according the vector prepared during daring specified curve scale 
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	private void drawCurveGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getHorizontalGridColor());

		Vector<Integer> horizontalGridVector = recordSet.getHorizontalGrid();
		for (int i = 0; i < horizontalGridVector.size(); i += recordSet.getHorizontalGridType()) {
			int y = horizontalGridVector.get(i);
			if (y > bounds.y && y < (bounds.y + bounds.height)) gc.drawLine(bounds.x, y, bounds.x + bounds.width, y);
		}
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during drawing of time scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param bounds
	 * @param dashLineStyle to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dashLineStyle) {
		gc.setLineWidth(1);
		gc.setLineDash(dashLineStyle);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x, bounds.y, x, bounds.y + bounds.height);
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
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					doRedrawGraphics();
				}
			});
		}
	}

	/**
	 * updates the graphics canvas, while repeatable redraw calls it optimized to the required area
	 */
	synchronized void doRedrawGraphics() {
		if (!GDE.IS_LINUX) { // old code changed due to Mountain Lion refresh problems
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "this.graphicCanvas.redraw(5,5,5,5,true); // image based - let OS handle the update"); //$NON-NLS-1$
			Point size = this.graphicCanvas.getSize();
			this.graphicCanvas.redraw(5, 5, 5, 5, true); // image based - let OS handle the update
			this.graphicCanvas.redraw(size.x - 5, 5, 5, 5, true);
			this.graphicCanvas.redraw(5, size.y - 5, 5, 5, true);
			this.graphicCanvas.redraw(size.x - 5, size.y - 5, 5, 5, true);
		}
		else {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "this.graphicCanvas.redraw(); // do full update where required"); //$NON-NLS-1$
			this.graphicCanvas.redraw(); // do full update where required
		}
		this.recordSetComment.redraw();
	}

	public void notifySelected() {
		this.recordSetComment.notifyListeners(SWT.FocusOut, new Event());
	}

	/**
	 * draw the start pointer for measurement modes.
	 * select only valid timestamps on the x axis.
	 * @param trailRecordSet
	 * @param mode
	 * @param isRefresh
	 */
	public void drawMeasurePointer(TrailRecordSet trailRecordSet, int mode, boolean isRefresh) {
		this.setModeState(mode); // cleans old pointer if required 

		String measureRecordKey = trailRecordSet.getRecordKeyMeasurement();
		TrailRecord trailRecord = (TrailRecord) trailRecordSet.get(measureRecordKey);

		// set the gc properties
		this.canvasGC = new GC(this.graphicCanvas);
		this.canvasGC.setLineWidth(1);
		this.canvasGC.setLineStyle(SWT.LINE_DASH);
		this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

		clearOldMeasureLines(trailRecordSet, trailRecord);

		if (trailRecordSet.isMeasurementMode(measureRecordKey)) {
			// initial measure position
			this.timestampMeasure_ms = isRefresh ? this.timestampMeasure_ms : this.timeLine.getAdjacentTimestamp(this.curveAreaBounds.width / 4);
			if (log.isLoggable(Level.OFF)) log.log(Level.OFF, "timestampMeasure_ms=" + this.timestampMeasure_ms + " isRefresh=" + isRefresh); //$NON-NLS-1$ //$NON-NLS-2$
			this.xPosMeasure = this.timeLine.getXPosTimestamp(this.timestampMeasure_ms);
			this.yPosMeasure = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timestampMeasure_ms));
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "initial xPosMeasure = " + this.xPosMeasure + " yPosMeasure = " + this.yPosMeasure); //$NON-NLS-1$ //$NON-NLS-2$

			drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());

			String formattedTimeWithUnit = LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timeLine.getAdjacentTimestamp(this.xPosMeasure));
			this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0256, new Object[] { trailRecord.getName(),
					trailRecord.getFormattedScaleValue(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure))), trailRecord.getUnit(), formattedTimeWithUnit }));
		}
		else if (trailRecordSet.isDeltaMeasurementMode(measureRecordKey)) {
			this.timestampMeasure_ms = isRefresh ? this.timestampMeasure_ms : this.timeLine.getAdjacentTimestamp(this.curveAreaBounds.width / 4);
			this.xPosMeasure = this.timeLine.getXPosTimestamp(this.timestampMeasure_ms);
			this.yPosMeasure = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)));

			// measure position
			drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

			// delta position
			this.timestampDelta_ms = isRefresh ? this.timestampDelta_ms : this.timeLine.getAdjacentTimestamp(this.curveAreaBounds.width / 3 * 2);
			this.xPosDelta = this.timeLine.getXPosTimestamp(this.timestampDelta_ms);
			this.yPosDelta = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timestampDelta_ms));

			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
			drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
			drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);

			drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);

			this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());

			this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0848,
					new Object[] { trailRecord.getName(),
							trailRecord.getDeltaAsFormattedScaleValue(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)),
									trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosDelta))),
							trailRecord.getUnit(), LocalizedDateTime.getFormatedDistance(this.timeLine.getAdjacentTimestamp(this.xPosMeasure), this.timeLine.getAdjacentTimestamp(this.xPosDelta)) }));
		}
		this.canvasGC.dispose();
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
	 * draws line as defined relative to curve draw area, where there is an offset from left and an offset from top 
	 * for performance reason specify line width, line style and line color outside 
	 * @param posFromTop1
	 * @param posFromLeft1
	 * @param posFromTop2
	 * @param posFromLeft2
	 */
	private void drawConnectingLine(int posFromLeft1, int posFromTop1, int posFromLeft2, int posFromTop2, int swtColor) {
		this.canvasGC.setForeground(SWTResourceManager.getColor(swtColor));
		this.canvasGC.setLineStyle(SWT.LINE_SOLID);
		this.canvasGC.drawLine(posFromLeft1 + this.offSetX, posFromTop1 + this.offSetY, posFromLeft2 + this.offSetX, posFromTop2 + this.offSetY);
	}

	/**
	 * erase a vertical line by re-drawing the curve area image 
	 * @param posFromLeft
	 * @param posFromTop
	 * @param length
	 * @param lineWidth
	 */
	void eraseVerticalLine(int posFromLeft, int posFromTop, int length, int lineWidth) {
		this.canvasGC.drawImage(this.canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length, posFromLeft + this.offSetX, posFromTop + this.offSetY, lineWidth, length);
	}

	/**
	 * erase a horizontal line by re-drawing the curve area image 
	 * @param posFromTop
	 * @param posFromLeft
	 * @param length
	 * @param lineWidth
	 */
	void eraseHorizontalLine(int posFromTop, int posFromLeft, int length, int lineWidth) {
		this.canvasGC.drawImage(this.canvasImage, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth, posFromLeft + this.offSetX, posFromTop + this.offSetY, length, lineWidth);
	}

	/**
	 * clean connecting line by re-drawing the untouched curve area image of this area
	 */
	void cleanConnectingLineObsoleteRectangle() {
		this.leftLast = this.leftLast == 0 ? this.xPosMeasure : this.leftLast;
		int left = this.xPosMeasure <= this.xPosDelta ? this.leftLast < this.xPosMeasure ? this.leftLast : this.xPosMeasure : this.leftLast < this.xPosDelta ? this.leftLast : this.xPosDelta;

		this.topLast = this.topLast == 0 ? this.yPosDelta : this.topLast;
		int top = this.yPosDelta <= this.yPosMeasure ? this.topLast < this.yPosDelta ? this.topLast : this.yPosDelta : this.topLast < this.yPosMeasure ? this.topLast : this.yPosMeasure;

		this.rightLast = this.rightLast == 0 ? this.xPosDelta - left : this.rightLast;
		int width = this.xPosDelta >= this.xPosMeasure ? this.rightLast > this.xPosDelta ? this.rightLast - left : this.xPosDelta - left
				: this.rightLast > this.xPosMeasure ? this.rightLast - left : this.xPosMeasure - left;

		this.bottomLast = this.bottomLast == 0 ? this.yPosMeasure - top : this.bottomLast;
		int height = this.yPosMeasure >= this.yPosDelta ? this.bottomLast > this.yPosMeasure ? this.bottomLast - top : this.yPosMeasure - top
				: this.bottomLast > this.yPosDelta ? this.bottomLast - top : this.yPosDelta - top;

		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "leftLast = " + this.leftLast + " topLast = " + this.topLast + " rightLast = " + this.rightLast + " bottomLast = " + this.bottomLast); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		if (width > 0 && height > 0 && width < this.curveAreaBounds.width && height < this.curveAreaBounds.height) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "left = " + left + " top = " + top + " width = " + width + " height = " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			this.canvasGC.drawImage(this.canvasImage, left + this.offSetX, top + this.offSetY, width, height, left + this.offSetX, top + this.offSetY, width, height);
		}

		this.leftLast = this.xPosMeasure <= this.xPosDelta ? this.xPosMeasure : this.xPosDelta;
		this.topLast = this.yPosDelta <= this.yPosMeasure ? this.yPosDelta : this.yPosMeasure;
		this.rightLast = this.xPosDelta >= this.xPosMeasure ? this.xPosDelta : this.xPosMeasure;
		this.bottomLast = this.yPosDelta >= this.yPosMeasure ? this.yPosDelta : this.yPosMeasure;
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "leftLast = " + this.leftLast + " topLast = " + this.topLast + " rightLast = " + this.rightLast + " bottomLast = " + this.bottomLast); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * erase connecting line by re-drawing the curve area image 
	 * @param posFromLeft1
	 * @param posFromTop1
	 * @param posFromLeft2
	 * @param posFromTop2
	 */
	void eraseConnectingLine(int left, int top, int width, int height) {
		if (width > 0 && height > 0 && width < this.curveAreaBounds.width && height < this.curveAreaBounds.height) {
			this.canvasGC.drawImage(this.canvasImage, left, top, width, height, left + this.offSetX, top + this.offSetY, width, height);
		}
	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 */
	public void cleanMeasurementPointer() {
		try {
			boolean isGCset = false;
			if (this.canvasGC != null && this.canvasGC.isDisposed()) {
				this.canvasGC = new GC(this.graphicCanvas);
			}
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
					cleanConnectingLineObsoleteRectangle();
				}
			}
			if (isGCset) this.canvasGC.dispose();
			if (this.recordSetCommentText != null) {
				this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
				this.recordSetComment.setText(this.recordSetCommentText);
			}
			else {
				this.recordSetComment.setText(GDE.STRING_EMPTY);
			}
			this.application.setStatusMessage(GDE.STRING_EMPTY);
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
	@Deprecated
	public void drawCutPointer(int mode, boolean leftEnabled, boolean rightEnabled) {
	}

	/**
	 * clean cutting edge pointer
	 */
	@Deprecated
	public void cleanCutPointer() {
	}

	/**
	 * switch graphics window mouse mode
	 * @param mode MODE_RESET, MODE_ZOOM, MODE_MEASURE, MODE_DELTA_MEASURE
	 */
	public void setModeState(int mode) {
		switch (mode) {
		case GraphicsComposite.MODE_MEASURE:
			if (!this.isLeftMouseMeasure) this.cleanMeasurementPointer();
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;
			break;
		case GraphicsComposite.MODE_MEASURE_DELTA:
			if (!this.isRightMouseMeasure) this.cleanMeasurementPointer();
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = true;
			break;
		case GraphicsComposite.MODE_RESET:
		default:
			this.cleanMeasurementPointer();
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			this.xLast = 0;
			this.yLast = 0;
			this.leftLast = 0;
			this.topLast = 0;
			this.rightLast = 0;
			this.bottomLast = 0;
			// updateCutModeButtons();
			this.application.getMenuToolBar().resetZoomToolBar();
			break;
		}
	}

	/**
	 * check input x,y value against curve are bounds and correct to bound if required
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
		int tmpxPos = xPos - this.offSetX;
		int tmpyPos = yPos - this.offSetY;
		int minX = 0;
		int maxX = this.curveAreaBounds.width;
		int minY = 0;
		int maxY = this.curveAreaBounds.height;
		if (tmpxPos < minX || tmpxPos > maxX) {
			tmpxPos = tmpxPos < minX ? minX : maxX;
		}
		if (tmpyPos < minY || tmpyPos > maxY) {
			tmpyPos = tmpyPos < minY ? minY : maxY;
		}
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			TrailRecordSet trailRecordSet = HistoSet.getInstance().getTrailRecordSet();
			if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0 && this.canvasImage != null) {
				this.canvasGC = new GC(this.graphicCanvas);
				Point point = checkCurveBounds(evt.x, evt.y);
				evt.x = point.x;
				evt.y = point.y;

				String measureRecordKey = trailRecordSet.getRecordKeyMeasurement();
				this.canvasGC.setLineWidth(1);
				this.canvasGC.setLineStyle(SWT.LINE_DASH);

				if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
					try {
						if (this.isLeftMouseMeasure) {
							TrailRecord trailRecord = (TrailRecord) trailRecordSet.getRecord(measureRecordKey);
							clearOldMeasureLines(trailRecordSet, trailRecord);

							if (trailRecordSet.isDeltaMeasurementMode(measureRecordKey)) {
								this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
								this.canvasGC.setLineStyle(SWT.LINE_DASH);
								drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
								drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);
								this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
							}
							// all obsolete lines are cleaned up now draw new position marker
							this.timestampMeasure_ms = this.timeLine.getAdjacentTimestamp(evt.x); // evt.x is already relative to curve area
							this.xPosMeasure = this.timeLine.getXPosTimestamp(this.timestampMeasure_ms);
							drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
							this.yPosMeasure = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)));
							drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);
							if (log.isLoggable(Level.OFF)) log.log(Level.OFF, "timestampMeasure_ms=" + this.timestampMeasure_ms); //$NON-NLS-1$ 

							if (trailRecordSet.isDeltaMeasurementMode(measureRecordKey)) {
								if (this.xPosMeasure != this.xPosDelta && this.yPosMeasure != this.yPosDelta) {
									drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);
								}
								this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
								this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0848,
										new Object[] { trailRecord.getName(),
												trailRecord.getDeltaAsFormattedScaleValue(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)),
														trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosDelta))),
												trailRecord.getUnit(), LocalizedDateTime.getFormatedDistance(this.timeLine.getAdjacentTimestamp(this.xPosMeasure), this.timeLine.getAdjacentTimestamp(this.xPosDelta)) }));
							}
							else {
								this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
								this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0256,
										new Object[] { trailRecord.getName(), trailRecord.getFormattedScaleValue(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure))), trailRecord.getUnit(),
												LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmmss, this.timeLine.getAdjacentTimestamp(this.xPosMeasure)) }));
							}
						}
						else if (this.isRightMouseMeasure) {
							TrailRecord trailRecord = (TrailRecord) trailRecordSet.getRecord(measureRecordKey);
							clearOldMeasureLines(trailRecordSet, trailRecord);

							// always needs to draw measurement pointer
							drawVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height);
							// no change don't needs to be calculated yPosMeasure = record.getDisplayPointDataValue(xPosMeasure, curveAreaBounds);
							drawHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width);

							// update the new delta position
							this.timestampDelta_ms = this.timeLine.getAdjacentTimestamp(evt.x); // evt.x is already relative to curve area
							this.xPosDelta = this.timeLine.getXPosTimestamp(this.timestampDelta_ms); // evt.x is already relative to curve area
							this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLUE));
							this.canvasGC.setLineStyle(SWT.LINE_DASH);
							drawVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height);
							this.yPosDelta = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosDelta)));
							drawHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width);

							if (this.xPosMeasure != this.xPosDelta && this.yPosMeasure != this.yPosDelta) {
								drawConnectingLine(this.xPosMeasure, this.yPosMeasure, this.xPosDelta, this.yPosDelta, SWT.COLOR_BLACK);
							}

							this.canvasGC.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));

							this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0848,
									new Object[] { trailRecord.getName(),
											trailRecord.getDeltaAsFormattedScaleValue(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)),
													trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosDelta))),
											trailRecord.getUnit(), LocalizedDateTime.getFormatedDistance(this.timeLine.getAdjacentTimestamp(this.xPosMeasure), this.timeLine.getAdjacentTimestamp(this.xPosDelta)) }));
						}
					}
					catch (RuntimeException e) {
						log.log(Level.WARNING, "mouse pointer out of range", e); //$NON-NLS-1$
					}
				}
				else if (measureRecordKey != null && (trailRecordSet.isMeasurementMode(measureRecordKey) || trailRecordSet.isDeltaMeasurementMode(measureRecordKey))) {
					if (this.xPosMeasure + 1 >= evt.x && this.xPosMeasure - 1 <= evt.x || this.xPosDelta + 1 >= evt.x && this.xPosDelta - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/MoveH.gif")); //$NON-NLS-1$
					}
					else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
				}
				else {
					this.graphicCanvas.setCursor(this.application.getCursor());
					String text = String.valueOf(evt.x) + "|" + String.valueOf(evt.y) + "|" + this.timeLine.getAdjacentTimestamp(evt.x) + "|" + this.timeLine.getSnappedTimestampText(evt.x); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (evt.x > 0 && evt.y > this.curveAreaBounds.height - this.offSetY) {
						if (!(text.equals(this.graphicCanvas.getToolTipText()))) {
							this.graphicCanvas.setToolTipText(text);
						}
					}
					else
						this.graphicCanvas.setToolTipText(null);
				}
				this.canvasGC.dispose();
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseDownAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			TrailRecordSet trailRecordSet = HistoSet.getInstance().getTrailRecordSet();
			if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
				String measureRecordKey = trailRecordSet.getRecordKeyMeasurement();
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xDown = point.x;
				this.yDown = point.y;

				if (measureRecordKey != null && (trailRecordSet.isMeasurementMode(measureRecordKey) || trailRecordSet.isDeltaMeasurementMode(measureRecordKey)) && this.xPosMeasure + 1 >= this.xDown
						&& this.xPosMeasure - 1 <= this.xDown) { // snap mouse pointer
					this.isLeftMouseMeasure = true;
					this.isRightMouseMeasure = false;
				}
				else if (measureRecordKey != null && trailRecordSet.isDeltaMeasurementMode(measureRecordKey) && this.xPosDelta + 1 >= this.xDown && this.xPosDelta - 1 <= this.xDown) { // snap mouse pointer
					this.isRightMouseMeasure = true;
					this.isLeftMouseMeasure = false;
				}
				else {
					this.isLeftMouseMeasure = false;
					this.isRightMouseMeasure = false;
				}
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			TrailRecordSet trailRecordSet = this.histoSet.getTrailRecordSet();
			if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xUp = point.x;
				this.yUp = point.y;

				if (this.isLeftMouseMeasure) {
					this.isLeftMouseMeasure = false;
					// application.setStatusMessage(GDE.STRING_EMPTY);
				}
				else if (this.isRightMouseMeasure) {
					this.isRightMouseMeasure = false;
					// application.setStatusMessage(GDE.STRING_EMPTY);
				}
				// updatePanMenueButton();
				// updateCutModeButtons();
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

	/**
	 * check if cut mode can be activated
	 * @param recordSet
	 */
	@Deprecated
	void updateCutModeButtons() {
	}

	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		if (enabled) {
			this.headerGap = 5;
			GC gc = new GC(this.graphicsHeader);
			int stringHeight = gc.stringExtent(this.graphicsHeader.getText()).y;
			this.headerGap = 5;
			this.headerHeight = stringHeight;
			gc.dispose();
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
			this.commentGap = 0;
			GC gc = new GC(this.recordSetComment);
			int stringHeight = gc.stringExtent(this.recordSetComment.getText()).y;
			this.commentHeight = stringHeight * 2 + 8;
			gc.dispose();
		}
		else {
			this.commentGap = 0;
			this.commentHeight = 0;
		}
		setComponentBounds();
	}

	public void clearHeaderAndComment() {
		if (HistoGraphicsComposite.this.channels.getActiveChannel() != null) {
			HistoGraphicsComposite.this.recordSetComment.setText(GDE.STRING_EMPTY);
			HistoGraphicsComposite.this.graphicsHeader.setText(GDE.STRING_EMPTY);
			HistoGraphicsComposite.this.graphicsHeaderText = null;
			HistoGraphicsComposite.this.recordSetCommentText = null;
			this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
			updateCaptions();
		}
	}

	public synchronized void updateCaptions() {
		HistoGraphicsComposite.this.recordSetComment.redraw();
		HistoGraphicsComposite.this.graphicsHeader.redraw();
	}

	/**
	 * resize the three areas: header, curve, comment
	 */
	void setComponentBounds() {
		Rectangle graphicsBounds = this.getClientArea();
		// this.application.setGraphicsSashFormWeights(this.graphicSashForm.getSize().x - graphicsBounds.width);
		int x = 0;
		int y = this.headerGap;
		int width = graphicsBounds.width;
		int height = this.headerHeight;
		this.graphicsHeader.setBounds(x, y, width, height);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetHeader.setBounds " + this.graphicsHeader.getBounds()); //$NON-NLS-1$

		y = this.headerGap + this.headerHeight;
		height = graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight);
		this.graphicCanvas.setBounds(x, y, width, height);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "graphicCanvas.setBounds " + this.graphicCanvas.getBounds()); //$NON-NLS-1$

		y = this.headerGap + this.headerHeight + height + this.commentGap;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width - 40, height - 5);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetComment.setBounds " + this.recordSetComment.getBounds()); //$NON-NLS-1$
	}

	/**
	 * @return the isRecordCommentChanged
	 */
	@Deprecated
	public boolean isRecordCommentChanged() {
		return false;
	}

	@Deprecated
	public void updateRecordSetComment() {
		String graphicsHeaderExtend = this.graphicsHeaderText == null ? "ET22" : this.graphicsHeaderText; //$NON-NLS-1$
		this.graphicsHeader.setText(this.graphicsHeaderText = graphicsHeaderExtend);
		this.graphicsHeader.redraw();
	}

	/**
	 * @return the graphic window content as image 
	 */
	public Image getGraphicsPrintImage() {
		Image graphicsImage = null;
		int graphicsHeight = 30 + this.canvasBounds.height + 40;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			TrailRecordSet trailRecordSet = this.histoSet.getTrailRecordSet();
			if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
				if (this.canvasImage != null) this.canvasImage.dispose();
				this.canvasImage = new Image(GDE.display, this.canvasBounds);
				this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
				this.canvasImageGC.setBackground(this.surroundingBackground);
				this.canvasImageGC.fillRectangle(this.canvasBounds);
				this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.canvasGC = new GC(this.graphicCanvas); // SWTResourceManager.getGC(this.graphicCanvas, "curveArea_" + this.windowType);
				drawCurves(trailRecordSet, this.canvasBounds, this.canvasImageGC);
				graphicsImage = new Image(GDE.display, this.canvasBounds.width, graphicsHeight);
				GC graphicsGC = new GC(graphicsImage);
				graphicsGC.setForeground(this.graphicsHeader.getForeground());
				graphicsGC.setBackground(this.surroundingBackground);
				graphicsGC.setFont(this.graphicsHeader.getFont());
				graphicsGC.fillRectangle(0, 0, this.canvasBounds.width, graphicsHeight);
				if (this.graphicsHeader.getText().length() > 1) {
					GraphicsUtils.drawTextCentered(this.graphicsHeader.getText(), this.canvasBounds.width / 2, 20, graphicsGC, SWT.HORIZONTAL);
				}
				graphicsGC.setFont(this.recordSetComment.getFont());
				if (this.recordSetComment.getText().length() > 1) {
					GraphicsUtils.drawText(this.recordSetComment.getText(), 20, graphicsHeight - 40, graphicsGC, SWT.HORIZONTAL);
				}
				graphicsGC.drawImage(this.canvasImage, 0, 30);
				graphicsGC.dispose();
				this.canvasGC.dispose();
				this.canvasImageGC.dispose();
			}
		}
		return graphicsImage;
	}

	@Deprecated
	public void setFileComment() {
	}

	private String getSelectedMeasurementsAsTable() {
		Properties displayProps = this.settings.getMeasurementDisplayProperties();
		TrailRecordSet trailRecordSet = HistoSet.getInstance().getTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
			this.recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD)); //$NON-NLS-1$
			Vector<Record> records = trailRecordSet.getVisibleAndDisplayableRecords();

			final long timestamp_ms = this.timeLine.getAdjacentTimestamp(this.xPosMeasure);
			StringBuilder sb = new StringBuilder().append(String.format("%-17s", Messages.getString(MessageIds.GDE_MSGT0652))); //$NON-NLS-1$
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = (TrailRecord) records.get(i);
				if (displayProps.getProperty(record.getName()) != null)
					sb.append(String.format("|%-10s", displayProps.getProperty(record.getName()))); //$NON-NLS-1$
				else {
					final String unit = GDE.STRING_LEFT_BRACKET + record.getUnit() + GDE.STRING_RIGHT_BRACKET;
					final String name = record.getName().substring(0, record.getName().length() >= 10 - unit.length() ? 10 - unit.length() : record.getName().length());
					final String format = "|%-" + (10 - unit.length()) + "s%" + unit.length() + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					sb.append(String.format(format, name, unit));
				}
			}
			sb.append("| ").append(GDE.LINE_SEPARATOR).append(String.format("%-17s", LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmm, timestamp_ms))); //$NON-NLS-1$ //$NON-NLS-2$
			int index = trailRecordSet.getIndex(timestamp_ms);
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = (TrailRecord) records.get(i);
				sb.append(String.format("|%7s   ", record.getFormattedScaleValue(index))); //$NON-NLS-1$
			}
			return sb.append("|").toString(); //$NON-NLS-1$
		}
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		return this.recordSetCommentText != null ? this.recordSetCommentText : GDE.STRING_EMPTY;
	}

	/**
	 * @param trailRecordSet
	 * @param trailRecord
	 */
	private void clearOldMeasureLines(TrailRecordSet trailRecordSet, TrailRecord trailRecord) {
		eraseVerticalLine(this.xPosMeasure, 0, this.curveAreaBounds.height, 1);
		// no change don't needs to be calculated, but the calculation limits to bounds
		this.yPosMeasure = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosMeasure)));
		eraseHorizontalLine(this.yPosMeasure, 0, this.curveAreaBounds.width, 1);

		eraseVerticalLine(this.xPosDelta, 0, this.curveAreaBounds.height, 1);
		// no change don't needs to be calculated, but the calculation limits to bounds
		this.yPosDelta = trailRecord.getVerticalDisplayPointPos(trailRecordSet.getIndex(this.timeLine.getAdjacentTimestamp(this.xPosDelta)));
		eraseHorizontalLine(this.yPosDelta, 0, this.curveAreaBounds.width, 1);

		cleanConnectingLineObsoleteRectangle();
	}

}
