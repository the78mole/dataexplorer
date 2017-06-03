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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
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
import gde.data.HistoSet.DirectoryType;
import gde.data.Record;
import gde.data.RecordSet;
import gde.data.TrailRecord;
import gde.data.TrailRecordSet;
import gde.data.TrailRecordSet.DataTag;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuOnDemand;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;
import gde.ui.tab.GraphicsWindow.GraphicsType;
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
	private final static String	$CLASS_NAME	= HistoGraphicsComposite.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	public enum HistoGraphicsMode {
		MEASURE, MEASURE_DELTA, RESET
	};

	private final HistoSet			histoSet						= HistoSet.getInstance();
	private final DataExplorer	application					= DataExplorer.getInstance();
	private final Settings			settings						= Settings.getInstance();
	private final Channels			channels						= Channels.getInstance();
	private final HistoTimeLine	timeLine						= new HistoTimeLine();
	private final SashForm			graphicSashForm;
	private final GraphicsType	graphicsType;

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
	String											graphicsHeaderText;
	Point												oldSize							= new Point(0, 0);						// composite size - control resized

	// mouse actions
	int													xDown								= 0;
	int													xUp									= 0;
	int													xLast								= 0;
	int													yDown								= 0;
	int													yUp									= 0;

	Rectangle										canvasBounds;
	Image												canvasImage;
	GC													canvasImageGC;
	GC													canvasGC;
	Rectangle										curveAreaBounds			= new Rectangle(0, 0, 1, 1);

	boolean											isLeftMouseMeasure	= false;
	boolean											isRightMouseMeasure	= false;

	private CurveSurvey					curveSurvey					= null;

	HistoGraphicsComposite(final SashForm useParent) {
		super(useParent, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);
		this.graphicSashForm = useParent;
		this.graphicsType = GraphicsType.HISTO;

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

		this.contextMenu.createMenu(this.popupmenu, TabMenuType.HISTOGRAPHICS);

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
				switch (HistoGraphicsComposite.this.graphicsType) {
				default:
				case NORMAL:
					HistoGraphicsComposite.this.application.openHelpDialog("", "HelpInfo_4.html"); //$NON-NLS-1$ //$NON-NLS-2$
					break;
				}
			}
		});
		{
			this.graphicsHeader = new Text(this, SWT.SINGLE | SWT.CENTER | SWT.READ_ONLY);
			this.graphicsHeader.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 3, SWT.BOLD));
			this.graphicsHeader.setBackground(this.surroundingBackground);
			this.graphicsHeader.setMenu(this.popupmenu);
			this.graphicsHeader.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					final String ellipsisText = Messages.getString(MessageIds.GDE_MSGT0864);
					final StringBuilder sb = new StringBuilder();
					String toolTipText = GDE.STRING_EMPTY;
					for (Entry<DirectoryType, Path> directoryEntry : HistoGraphicsComposite.this.histoSet.getValidatedDirectories().entrySet()) {
						final String truncatedPath = directoryEntry.getValue().getFileName().toString().length() > 22 ? directoryEntry.getValue().getFileName().toString().substring(0, 22) + ellipsisText
								: directoryEntry.getValue().getFileName().toString();
						sb.append(GDE.STRING_BLANK + GDE.STRING_OR + GDE.STRING_BLANK).append(truncatedPath);
						toolTipText += GDE.STRING_NEW_LINE + directoryEntry.getKey().toString() + GDE.STRING_BLANK_COLON_BLANK + directoryEntry.getValue().toString();
					}
					final String levelsText = Settings.getInstance().getSubDirectoryLevelMax() > 0
							? GDE.STRING_NEW_LINE + "+ " + Settings.getInstance().getSubDirectoryLevelMax() + GDE.STRING_BLANK + Messages.getString(MessageIds.GDE_MSGT0870) : GDE.STRING_EMPTY; //$NON-NLS-1$
					final String tmpHeaderText = sb.length() >= 3 ? sb.substring(3) : GDE.STRING_EMPTY;
					if (HistoGraphicsComposite.this.graphicsHeaderText == null || !tmpHeaderText.equals(HistoGraphicsComposite.this.graphicsHeaderText)) {
						HistoGraphicsComposite.this.graphicsHeader.setText(HistoGraphicsComposite.this.graphicsHeaderText = tmpHeaderText);
					}
					if (!toolTipText.isEmpty()) HistoGraphicsComposite.this.graphicsHeader.setToolTipText(toolTipText.substring(1) + levelsText);
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
					if (evt.button == 1 || evt.button == 3) {
						mouseDownAction(evt);
					}
				}

				@Override
				public void mouseUp(MouseEvent evt) {
					if (log.isLoggable(Level.FINEST)) log.log(Level.FINEST, "graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1 || evt.button == 3) {
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
			this.recordSetComment = new Text(this, SWT.MULTI | SWT.LEFT | SWT.READ_ONLY);
			this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
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
		if (this.curveSurvey != null) this.curveSurvey.setCanvasGC(this.canvasGC);

		setRecordSetCommentStandard();

		TrailRecordSet trailRecordSet = getTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
			drawCurves(trailRecordSet, this.canvasBounds, this.canvasImageGC);
			this.canvasGC.drawImage(this.canvasImage, 0, 0);
			// changed curve selection may change the scale end values
			trailRecordSet.syncScaleOfSyncableRecords();

			if (trailRecordSet.isMeasurementMode(trailRecordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(trailRecordSet, HistoGraphicsMode.MEASURE, true);
			}
			else if (trailRecordSet.isDeltaMeasurementMode(trailRecordSet.getRecordKeyMeasurement())) {
				drawMeasurePointer(trailRecordSet, HistoGraphicsMode.MEASURE_DELTA, true);
			}
		}
		else
			this.canvasGC.drawImage(this.canvasImage, 0, 0);

		this.canvasGC.dispose();
		this.canvasImageGC.dispose();
		// this.canvasImage.dispose(); //zooming, marking, ... needs a reference to canvasImage
	}

	private TrailRecordSet getTrailRecordSet() {
		TrailRecordSet trailRecordSet = null;
		if (this.channels.getActiveChannel() != null) {
			trailRecordSet = this.histoSet.getTrailRecordSet();
		}
		return trailRecordSet;
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
		this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "curve bounds = " + this.curveAreaBounds); //$NON-NLS-1$

		if (trailRecordSet.getRecordDataSize(true) > 0) {
			// initialize early in order to avoid problems in mouse move events
			this.timeLine.initialize(trailRecordSet, width, trailRecordSet.getFirstTimeStamp_ms(), trailRecordSet.getLastTimeStamp_ms());

			// draw curves for each active record
			trailRecordSet.setDrawAreaBounds(this.curveAreaBounds);

			gc.setBackground(this.curveAreaBackground);
			gc.fillRectangle(this.curveAreaBounds);
			gc.setBackground(this.surroundingBackground);

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
				HistoCurveUtils.drawHistoScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth, isDrawScaleInRecordColor, isDrawNameInRecordColor, isDrawNumbersInRecordColor);

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

	private void setRecordSetCommentStandard() {
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.recordSetComment.setText(Messages.getString(MessageIds.GDE_MSGI0064,
				new Object[] { String.format("%,d", this.histoSet.getDirectoryFilesCount()), String.format("%,d", this.histoSet.getSelectedFilesCount()), //
						String.format("%,d", this.histoSet.getTotalTrussesCount()), String.format("%,d", this.histoSet.getUnsuppressedTrussesCount()), //
						String.format("%,d", this.histoSet.getMatchingTrussesCount()), String.format("%,d", this.histoSet.getAvailableTrussesCount()), //
						String.format("%.2f", this.histoSet.getElapsedTime_ms() / 1000.) }));
	}

	/**
	 * draw the start pointer for measurement modes.
	 * select only valid timestamps on the x axis.
	 * @param trailRecordSet
	 * @param mode
	 * @param isRefresh true keeps the measure timestamps, false tries to find new measure timestamps
	 */
	public void drawMeasurePointer(TrailRecordSet trailRecordSet, HistoGraphicsMode mode, boolean isRefresh) {
		this.setModeState(mode);

		String measureRecordKey = trailRecordSet.getRecordKeyMeasurement();
		TrailRecord trailRecord = (TrailRecord) trailRecordSet.get(measureRecordKey);

		// set the gc properties
		this.canvasGC = new GC(this.graphicCanvas);
		this.canvasGC.setForeground(trailRecord.getColor());

		if (this.curveSurvey == null) {
			this.curveSurvey = new CurveSurvey(this.canvasGC, trailRecord, this.timeLine);
		}
		else {
			this.curveSurvey.setCanvasGC(this.canvasGC);
		}

		if (trailRecordSet.isMeasurementMode(measureRecordKey)) {
			// initial measure position
			long timestampMeasureNew_ms = isRefresh ? this.curveSurvey.getTimestampMeasure_ms() : this.timeLine.getAdjacentTimestamp(this.curveAreaBounds.width / 4);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "timestampMeasure_ms=" + timestampMeasureNew_ms + " isRefresh=" + isRefresh); //$NON-NLS-1$ //$NON-NLS-2$

			this.curveSurvey.setPosMeasure(this.curveAreaBounds, timestampMeasureNew_ms);

			String statusMessage = this.curveSurvey.drawMeasurementGraphics();
			this.application.setStatusMessage(statusMessage);

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
		}
		else if (trailRecordSet.isDeltaMeasurementMode(measureRecordKey)) {
			long timestampMeasureNew_ms = isRefresh ? this.curveSurvey.getTimestampMeasure_ms() : this.timeLine.getAdjacentTimestamp(this.curveAreaBounds.width / 4);
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "timestampMeasure_ms=" + timestampMeasureNew_ms + " isRefresh=" + isRefresh); //$NON-NLS-1$ //$NON-NLS-2$

			this.curveSurvey.setPosMeasure(this.curveAreaBounds, timestampMeasureNew_ms);

			long timestampDeltaNew_ms = isRefresh ? this.curveSurvey.getTimestampDelta_ms() : this.timeLine.getAdjacentTimestamp(this.curveAreaBounds.width / 3 * 2);

			this.curveSurvey.setPosDelta(this.curveAreaBounds, timestampDeltaNew_ms);

			String statusMessage = this.curveSurvey.drawDeltaMeasurementGraphics();
			this.application.setStatusMessage(statusMessage);

			this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
		}
		this.canvasGC.dispose();

	}

	/**
	 * clean (old) measurement pointer - check pointer in curve area
	 */
	public void cleanMeasurementPointer() {
		if (this.curveSurvey != null) {
			if (this.canvasGC != null && this.canvasGC.isDisposed()) {
				this.canvasGC = new GC(this.graphicCanvas);
				this.curveSurvey.setCanvasGC(this.canvasGC);
			}
			this.curveSurvey.cleanMeasurementPointer(this.canvasImage);
		}
		this.curveSurvey = null;

		setRecordSetCommentStandard();
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
	 * @param mode MODE_RESET, MODE_MEASURE, MODE_DELTA_MEASURE
	 */
	public void setModeState(HistoGraphicsMode mode) {
		switch (mode) {
		case MEASURE:
			this.isLeftMouseMeasure = true;
			this.isRightMouseMeasure = false;

			break;
		case MEASURE_DELTA:
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = true;
			break;
		case RESET:
		default:
			this.cleanMeasurementPointer();
			this.isLeftMouseMeasure = false;
			this.isRightMouseMeasure = false;
			this.application.setStatusMessage(GDE.STRING_EMPTY);
			break;
		}
	}

	/**
	 * check input x,y value against curve are bounds and correct to bound if required
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
		int tmpxPos = xPos - this.curveAreaBounds.x;
		int tmpyPos = yPos - this.curveAreaBounds.y;
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
			TrailRecordSet trailRecordSet = getTrailRecordSet();
			if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0 && this.canvasImage != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				evt.x = point.x;
				evt.y = point.y;

				{
					this.graphicCanvas.setCursor(this.application.getCursor());
					if (evt.x > 0 && evt.y > this.curveAreaBounds.height - this.curveAreaBounds.y) {
						final Long timestamp_ms = this.timeLine.getSnappedTimestamp(evt.x);
						final String text = timestamp_ms != null ? Paths.get(trailRecordSet.getDataTags(trailRecordSet.getIndex(timestamp_ms)).get(DataTag.FILE_PATH)).getFileName().toString() : null;
						if (text != null) {
							if (this.graphicCanvas.getToolTipText() == null || !(text.equals(this.graphicCanvas.getToolTipText()))) this.graphicCanvas.setToolTipText(text);
						}
						else
							this.graphicCanvas.setToolTipText(null);
					}
					else
						this.graphicCanvas.setToolTipText(null);
				}
				String measureRecordKey = trailRecordSet.getRecordKeyMeasurement();

				if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
					if (this.isLeftMouseMeasure) {
						this.canvasGC = new GC(this.graphicCanvas);
						this.curveSurvey.setCanvasGC(this.canvasGC);
						TrailRecord trailRecord = (TrailRecord) trailRecordSet.getRecord(measureRecordKey);
						this.canvasGC.setForeground(trailRecord.getColor());

						long timestampMeasureNew_ms = this.timeLine.getAdjacentTimestamp(evt.x); // evt.x is already relative to curve area
						int yPosMeasureNew = trailRecord.getVerticalDisplayPos(trailRecordSet.getIndex(timestampMeasureNew_ms));
						if (timestampMeasureNew_ms != this.curveSurvey.getTimestampMeasure_ms() || yPosMeasureNew != this.curveSurvey.getyPosMeasure()) {
							// all obsolete lines are cleaned up now draw new position marker
							this.curveSurvey.clearOldMeasureLines(this.canvasImage);

							if (trailRecordSet.isMeasurementMode(measureRecordKey)) {
								this.curveSurvey.setPosMeasure(this.curveAreaBounds, timestampMeasureNew_ms);
								String statusMessage = this.curveSurvey.drawMeasurementGraphics();
								this.application.setStatusMessage(statusMessage);
							}
							else if (trailRecordSet.isDeltaMeasurementMode(measureRecordKey)) {
								this.curveSurvey.setPosMeasure(this.curveAreaBounds, timestampMeasureNew_ms);
								String statusMessage = this.curveSurvey.drawDeltaMeasurementGraphics();
								this.application.setStatusMessage(statusMessage);
							}
							else {
								this.application.setStatusMessage(Messages.getString(MessageIds.GDE_MSGT0848, new Object[] { trailRecord.getName(), GDE.STRING_STAR, trailRecord.getUnit(), GDE.STRING_STAR }));
								throw new UnsupportedOperationException();
							}

							this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
						}
						this.canvasGC.dispose();
					}
					else if (this.isRightMouseMeasure) {
						this.canvasGC = new GC(this.graphicCanvas);
						this.curveSurvey.setCanvasGC(this.canvasGC);
						TrailRecord trailRecord = (TrailRecord) trailRecordSet.getRecord(measureRecordKey);
						this.canvasGC.setForeground(trailRecord.getColor());

						long timestampDeltaNew_ms = this.timeLine.getAdjacentTimestamp(evt.x); // evt.x is already relative to curve area
						int yPosDeltaNew = trailRecord.getVerticalDisplayPos(trailRecordSet.getIndex(timestampDeltaNew_ms));
						if (timestampDeltaNew_ms != this.curveSurvey.getTimestampDelta_ms() || yPosDeltaNew != this.curveSurvey.getyPosDelta()) {
							this.curveSurvey.clearOldMeasureLines(this.canvasImage);

							this.curveSurvey.setPosDelta(this.curveAreaBounds, timestampDeltaNew_ms);
							String statusMessage = this.curveSurvey.drawDeltaMeasurementGraphics();
							this.application.setStatusMessage(statusMessage);

							this.recordSetComment.setText(this.getSelectedMeasurementsAsTable());
						}
						this.canvasGC.dispose();
					}
				}
				else if (measureRecordKey != null && (trailRecordSet.isMeasurementMode(measureRecordKey) || trailRecordSet.isDeltaMeasurementMode(measureRecordKey))) {
					if (evt.x > 0 && this.curveSurvey.getxPosMeasure() > 0 && this.curveSurvey.getxPosMeasure() + 1 >= evt.x && this.curveSurvey.getxPosMeasure() - 1 <= evt.x
							|| evt.x > 0 && this.curveSurvey.getxPosDelta() > 0 && this.curveSurvey.getxPosDelta() + 1 >= evt.x && this.curveSurvey.getxPosDelta() - 1 <= evt.x) { // snap mouse pointer
						this.graphicCanvas.setCursor(SWTResourceManager.getCursor("gde/resource/MoveH.gif")); //$NON-NLS-1$
					}
					else {
						this.graphicCanvas.setCursor(this.application.getCursor());
					}
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
			TrailRecordSet trailRecordSet = getTrailRecordSet();
			if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
				String measureRecordKey = trailRecordSet.getRecordKeyMeasurement();
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xDown = point.x;
				this.yDown = point.y;

				if (evt.button == 1) {
					if (measureRecordKey != null && (trailRecordSet.isMeasurementMode(measureRecordKey) || trailRecordSet.isDeltaMeasurementMode(measureRecordKey))
							&& this.curveSurvey.getxPosMeasure() + 1 >= this.xDown && this.curveSurvey.getxPosMeasure() - 1 <= this.xDown) { // snap mouse pointer
						this.isLeftMouseMeasure = true;
						this.isRightMouseMeasure = false;
					}
					else if (measureRecordKey != null && trailRecordSet.isDeltaMeasurementMode(measureRecordKey) && this.curveSurvey.getxPosDelta() + 1 >= this.xDown
							&& this.curveSurvey.getxPosDelta() - 1 <= this.xDown) { // snap mouse pointer
						this.isRightMouseMeasure = true;
						this.isLeftMouseMeasure = false;
					}
					else {
						this.isLeftMouseMeasure = false;
						this.isRightMouseMeasure = false;
					}
				}
				else if (evt.button == 3) { // right button
					HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), GDE.STRING_TRUE);
					HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), this.histoSet.getExcludedTrussesAsText());
					if (this.xDown == 0 || this.xDown == this.curveAreaBounds.width) {
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), GDE.STRING_EMPTY);
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), GDE.STRING_EMPTY);
					}
					else {
						final Map<DataTag, String> dataTags = getTrailRecordSet().getDataTags(getTrailRecordSet().getIndex(HistoGraphicsComposite.this.timeLine.getAdjacentTimestamp(this.xDown))); // evt.x is already relative to curve area
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), dataTags.get(DataTag.FILE_PATH));
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), dataTags.get(DataTag.RECORDSET_BASE_NAME));
					}
					HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), this.histoSet.getExcludedTrussesAsText());
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
			TrailRecordSet trailRecordSet = getTrailRecordSet();
			if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xUp = point.x;
				this.yUp = point.y;

				if (evt.button == 1) {
					if (this.isLeftMouseMeasure) {
						this.isLeftMouseMeasure = false;
						// application.setStatusMessage(GDE.STRING_EMPTY);
					}
					else if (this.isRightMouseMeasure) {
						this.isRightMouseMeasure = false;
						// application.setStatusMessage(GDE.STRING_EMPTY);
					}
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "isMouseMeasure = " + this.isLeftMouseMeasure + " isMouseDeltaMeasure = " + this.isRightMouseMeasure); //$NON-NLS-1$ //$NON-NLS-2$
				}
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
			TrailRecordSet trailRecordSet = getTrailRecordSet();
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
		TrailRecordSet trailRecordSet = getTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getRecordDataSize(true) > 0) {
			Vector<Record> records = trailRecordSet.getVisibleAndDisplayableRecords();

			StringBuilder sb = new StringBuilder().append(String.format("%-11.11s", Messages.getString(MessageIds.GDE_MSGT0799))); //$NON-NLS-1$
			sb.append(GDE.STRING_OR).append(String.format("%-16s", Messages.getString(MessageIds.GDE_MSGT0652))); //$NON-NLS-1$
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = (TrailRecord) records.get(i);
				if (displayProps.getProperty(record.getName()) != null)
					sb.append(GDE.STRING_OR).append(String.format("%-10s", displayProps.getProperty(record.getName()))); //$NON-NLS-1$
				else {
					final String unit = GDE.STRING_LEFT_BRACKET + record.getUnit() + GDE.STRING_RIGHT_BRACKET;
					final String name = record.getName().substring(0, record.getName().length() >= 10 - unit.length() ? 10 - unit.length() : record.getName().length());
					final String format = "%-" + (10 - unit.length()) + "s%" + unit.length() + "s"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					sb.append(GDE.STRING_OR).append(String.format(format, name, unit));
				}
			}
			sb.append(GDE.STRING_OR).append(GDE.LINE_SEPARATOR);

			final long timestamp_ms = this.timeLine.getAdjacentTimestamp(this.curveSurvey.getxPosMeasure());
			final int index = trailRecordSet.getIndex(timestamp_ms);
			sb.append(String.format("%-11.11s", trailRecordSet.getDataTags(index).get(DataTag.RECORDSET_BASE_NAME))); //$NON-NLS-1$
			sb.append(GDE.STRING_OR).append(String.format("%-16s", LocalizedDateTime.getFormatedTime(DateTimePattern.yyyyMMdd_HHmm, timestamp_ms))); //$NON-NLS-1$
			for (int i = 0; i < records.size(); i++) {
				TrailRecord record = (TrailRecord) records.get(i);
				sb.append(GDE.STRING_OR).append(String.format("%.10s", StringHelper.center(record.getFormattedMeasureValue(index), 10))); //$NON-NLS-1$
			}
			this.recordSetComment.setFont(SWTResourceManager.getFont("Courier New", GDE.WIDGET_FONT_SIZE - 1, SWT.BOLD)); //$NON-NLS-1$
			return sb.append(GDE.STRING_OR).toString();
		}
		else {
			return GDE.STRING_EMPTY;
		}
	}

}
