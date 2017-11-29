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
package gde.histo.ui;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.histo.cache.VaultCollector;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.exclusions.ExclusionFormatter;
import gde.histo.recordings.RecordingsCollector;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.recordings.TrailRecordSet.DataTag;
import gde.histo.ui.HistoGraphicsMeasurement.HistoGraphicsMode;
import gde.histo.utils.HistoCurveUtils;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.ui.menu.TabAreaContextMenu.TabMenuOnDemand;
import gde.ui.menu.TabAreaContextMenu.TabMenuType;
import gde.utils.MathUtils;
import gde.utils.StringHelper;

/**
 * Curves for the histo graphics window.
 * @author Thomas Eickert
 */
public final class HistoGraphicsComposite extends AbstractHistoChartComposite {
	private final static String	$CLASS_NAME	= HistoGraphicsComposite.class.getName();
	private final static Logger	log					= Logger.getLogger($CLASS_NAME);

	final HistoTimeLine					timeLine		= new HistoTimeLine();

	/** composite size - control resized */
	Point												oldSize			= new Point(0, 0);

	// mouse actions
	int													xDown				= 0;
	int													xUp					= 0;
	int													xLast				= 0;
	int													yDown				= 0;
	int													yUp					= 0;

	HistoGraphicsMeasurement		graphicsMeasurement;

	HistoGraphicsComposite(SashForm useParent) {
		super(useParent, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);

		// get the background colors
		this.curveAreaBackground = this.settings.getGraphicsCurveAreaBackground();
		this.surroundingBackground = this.settings.getGraphicsSurroundingBackground();
		this.curveAreaBorderColor = this.settings.getGraphicsCurvesBorderColor();

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();

		init();

		enableGraphicsScale(true);
	}

	private void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(this.surroundingBackground);

		this.contextMenu.createMenu(this.popupmenu, TabMenuType.HISTOGRAPHICS);

		// help lister does not get active on Composite as well as on Canvas
		this.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				log.finer(() -> "GraphicsComposite.controlResized() = " + evt); //$NON-NLS-1$
				Rectangle clientRect = HistoGraphicsComposite.this.getClientArea();
				Point size = new Point(clientRect.width, clientRect.height);
				log.finer(() -> HistoGraphicsComposite.this.oldSize + " - " + size); //$NON-NLS-1$
				if (!HistoGraphicsComposite.this.oldSize.equals(size)) {
					log.fine(() -> "size changed, update " + HistoGraphicsComposite.this.oldSize + " - " + size); //$NON-NLS-1$ //$NON-NLS-2$
					HistoGraphicsComposite.this.oldSize = size;
					setComponentBounds();
					doRedrawGraphics();
				}
			}
		});
		this.addHelpListener(new HelpListener() {
			@Override
			public void helpRequested(HelpEvent evt) {
				log.finer(() -> "GraphicsComposite.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_94.html"); //$NON-NLS-1$ //$NON-NLS-2$
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
					log.finer(() -> "recordSetHeader.paintControl, event=" + evt); //$NON-NLS-1$
					String headerText = GDE.STRING_EMPTY;
					String toolTipText = GDE.STRING_EMPTY;
					{ // getBaseTexts
						StringBuilder sb = new StringBuilder();
						String ellipsisText = Messages.getString(MessageIds.GDE_MSGT0864);
						for (Entry<DirectoryType, Path> directoryEntry : DataExplorer.getInstance().getHistoSet().getValidatedDirectories().entrySet()) {
							String fileName = directoryEntry.getValue().getFileName().toString();
							String truncatedPath = fileName.length() > 22 ? fileName.substring(0, 22) + ellipsisText : fileName;
							sb.append(GDE.STRING_BLANK + GDE.STRING_OR + GDE.STRING_BLANK).append(truncatedPath);
							toolTipText += GDE.STRING_NEW_LINE + directoryEntry.getKey().toString() + GDE.STRING_BLANK_COLON_BLANK + directoryEntry.getValue().toString();
						}
						headerText = sb.length() >= 3 ? sb.substring(3) : GDE.STRING_EMPTY;
						if (!toolTipText.isEmpty()) toolTipText = toolTipText.substring(1);
					}
					if (!headerText.equals(HistoGraphicsComposite.this.graphicsHeaderText)) {
						HistoGraphicsComposite.this.graphicsHeaderText = headerText;
						HistoGraphicsComposite.this.graphicsHeader.setText(headerText);
					}
					{ // getFullText
						int levelMax = Settings.getInstance().getSubDirectoryLevelMax();
						String levelsText = levelMax > 0 ? GDE.STRING_NEW_LINE + "+ " + levelMax + GDE.STRING_BLANK + Messages.getString(MessageIds.GDE_MSGT0870)
								: GDE.STRING_EMPTY;
						if (!toolTipText.isEmpty()) toolTipText = toolTipText + levelsText;
					}
					HistoGraphicsComposite.this.graphicsHeader.setToolTipText(toolTipText);
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
					log.finest(() -> "graphicCanvas.mouseMove = " + evt); //$NON-NLS-1$
					mouseMoveAction(evt);
				}
			});
			this.graphicCanvas.addMouseTrackListener(new MouseTrackAdapter() {
				@Override
				public void mouseExit(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseExit, event=" + evt); //$NON-NLS-1$
					HistoGraphicsComposite.this.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
				}
			});
			this.graphicCanvas.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseDown, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1 || evt.button == 3) {
						mouseDownAction(evt);
					}
				}

				@Override
				public void mouseUp(MouseEvent evt) {
					log.finest(() -> "graphicCanvas.mouseUp, event=" + evt); //$NON-NLS-1$
					if (evt.button == 1 || evt.button == 3) {
						mouseUpAction(evt);
					}
				}
			});
			this.graphicCanvas.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					log.finer(() -> "graphicCanvas.paintControl, event=" + evt); //$NON-NLS-1$
					// System.out.println("width = " + GraphicsComposite.this.getSize().x);
					try {
						drawAreaPaintControl(evt);
					} catch (Exception e) {
						log.log(SEVERE, e.getMessage(), e);
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
					log.finer(() -> "recordSetComment.paintControl, event=" + evt); //$NON-NLS-1$
				}
			});
		}
		{
			this.xScale = new Text(this, SWT.MULTI | SWT.LEFT | SWT.READ_ONLY);
			this.xScale.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.xScale.setBackground(this.surroundingBackground);
			this.xScale.setMenu(this.popupmenu);
			this.xScale.addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent evt) {
					log.finer(() -> "xScale.paintControl, event=" + evt); //$NON-NLS-1$
				}
			});
		}
	}

	/**
	 * Draw the containing records.
	 * Called in case of an paint event (redraw).
	 * @param evt
	 */
	private void drawAreaPaintControl(PaintEvent evt) {
		log.finest(() -> "drawAreaPaintControl.paintControl, event=" + evt); //$NON-NLS-1$
		drawAreaPaintControl();
	}

	/**
	 * Draw the containing records and sets the comment.
	 */
	private void drawAreaPaintControl() {
		long startTime = new Date().getTime();
		// Get the canvas and its dimensions
		this.canvasBounds = this.graphicCanvas.getClientArea();
		log.finer(() -> "canvas size = " + this.canvasBounds); //$NON-NLS-1$

		if (this.canvasImage != null) this.canvasImage.dispose();
		this.canvasImage = new Image(GDE.display, this.canvasBounds);
		this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
		this.canvasImageGC.setBackground(this.surroundingBackground);
		this.canvasImageGC.fillRectangle(this.canvasBounds);
		this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		// get gc for other drawing operations
		this.canvasGC = new GC(this.graphicCanvas);

		setRecordSetCommentStandard();

		trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			drawCurves();
			this.canvasGC.drawImage(this.canvasImage, 0, 0);
			// changed curve selection may change the scale end values
			trailRecordSet.syncScaleOfSyncableRecords();

			if (this.graphicsMeasurement != null) {
				this.graphicsMeasurement.drawMeasurement();
			}
		} else
			this.canvasGC.drawImage(this.canvasImage, 0, 0);

		this.canvasGC.dispose();
		this.canvasImageGC.dispose();
		// this.canvasImage.dispose(); //zooming, marking, ... needs a reference to canvasImage
		log.time(() -> "draw time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - startTime)));
	}

	@Override
	public void drawMeasurePointer(TrailRecordSet trailRecordSet, HistoGraphicsMode mode) {
		// draw full graph at first because the curve area might change (due to new new scales)
		drawAreaPaintControl();

		this.graphicsMeasurement = new HistoGraphicsMeasurement(this);
		this.setModeState(mode);

		mode.drawInitialMeasurement(this);
	}

	@Override
	public void cleanMeasurement() {
		if (this.graphicsMeasurement != null) {
			this.graphicsMeasurement.cleanMeasurement();

			this.graphicsMeasurement = null;
		}
	}

	/**
	 * Set graphics window measurement mode.
	 * @param mode
	 */
	public void setModeState(HistoGraphicsMode mode) {
		this.graphicsMeasurement.setModeState(mode);
	}

	/**
	 * Check input x,y value against curve are bounds and correct to bound if required.
	 * @param Point containing corrected x,y position value
	 */
	private Point checkCurveBounds(int xPos, int yPos) {
		log.finer(() -> "in  xPos = " + xPos + " yPos = " + yPos); //$NON-NLS-1$ //$NON-NLS-2$
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
		if (log.isLoggable(FINER)) log.log(FINER, "out xPos = " + tmpxPos + " yPos = " + tmpyPos); //$NON-NLS-1$ //$NON-NLS-2$
		return new Point(tmpxPos, tmpyPos);
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0 && this.canvasImage != null) {
				Point point = checkCurveBounds(evt.x, evt.y);
				evt.x = point.x;
				evt.y = point.y;

				{
					this.graphicCanvas.setCursor(this.application.getCursor());
					if (evt.x > 0 && evt.y > this.curveAreaBounds.height - this.curveAreaBounds.y) {
						Long timestamp_ms = this.timeLine.getSnappedTimestamp(evt.x);
						String text = timestamp_ms != null
								? Paths.get(trailRecordSet.getDataTags().getByIndex(trailRecordSet.getIndex(timestamp_ms)).get(DataTag.FILE_PATH)).getFileName().toString()
								: null;
						if (text != null) {
							if (this.graphicCanvas.getToolTipText() == null || !(text.equals(this.graphicCanvas.getToolTipText())))
								this.graphicCanvas.setToolTipText(text);
						} else
							this.graphicCanvas.setToolTipText(null);
					} else
						this.graphicCanvas.setToolTipText(null);
				}

				if (this.graphicsMeasurement != null) {
					if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
						this.graphicsMeasurement.processMouseDownMove(this.timeLine.getAdjacentTimestamp(evt.x));
					} else
						this.graphicsMeasurement.processMouseUpMove(evt.x);
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
			if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xDown = point.x;
				this.yDown = point.y;

				if (evt.button == 1) {
					if (this.graphicsMeasurement != null) {
						this.graphicsMeasurement.processMouseDownAction(this.xDown);
					}
				} else if (evt.button == 3) { // right button
					HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), GDE.STRING_TRUE);
					HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), ExclusionFormatter.getExcludedTrussesAsText());
					if (this.xDown == 0 || this.xDown == this.curveAreaBounds.width) {
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), GDE.STRING_EMPTY);
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), GDE.STRING_EMPTY);
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), GDE.STRING_EMPTY);
					} else {
						Map<DataTag, String> dataTags = trailRecordSet.getDataTags().getByIndex(trailRecordSet //
								.getIndex(HistoGraphicsComposite.this.timeLine.getAdjacentTimestamp(this.xDown))); // is already relative to curve area
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), dataTags.get(DataTag.LINK_PATH));
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), dataTags.get(DataTag.FILE_PATH));
						HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), dataTags.get(DataTag.RECORDSET_BASE_NAME));
					}
					HistoGraphicsComposite.this.popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), ExclusionFormatter.getExcludedTrussesAsText());
				}
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
				Point point = checkCurveBounds(evt.x, evt.y);
				this.xUp = point.x;
				this.yUp = point.y;

				if (evt.button == 1) {
					if (this.graphicsMeasurement != null) {
						this.graphicsMeasurement.processMouseUpAction();
					}
				}
			}
		}
	}

	@Override
	protected void drawCurveArea(int dataScaleWidth) {
		canvasImageGC.setBackground(this.curveAreaBackground);
		canvasImageGC.fillRectangle(this.curveAreaBounds);
		canvasImageGC.setBackground(this.surroundingBackground);

		// draw draw area bounding
		canvasImageGC.setForeground(this.curveAreaBorderColor);

		int xMax = curveAreaBounds.x + curveAreaBounds.width;
		int y0 = curveAreaBounds.y + curveAreaBounds.height;
		canvasImageGC.drawLine(curveAreaBounds.x - 1, curveAreaBounds.y - 1, xMax + 1, curveAreaBounds.y - 1); // top line
		canvasImageGC.drawLine(curveAreaBounds.x - 1, curveAreaBounds.y - 1, curveAreaBounds.x - 1, y0); // left fence
		canvasImageGC.drawLine(xMax + 1, curveAreaBounds.y - 1, xMax + 1, y0); // right fence

		// initialize early in order to avoid problems in mouse move events
		this.timeLine.initialize(trailRecordSet, this.curveAreaBounds.width);
		this.timeLine.drawTimeLine(canvasImageGC, curveAreaBounds.x, y0);

		long startTime = new Date().getTime();
		drawTrailRecordSet(dataScaleWidth);
		log.fine(() -> "draw records time = " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() //$NON-NLS-1$ //$NON-NLS-2$
				- startTime)));
	}

	/**
	 * Draw the visible curves for all channel items.
	 * Support multiple curves for one single item.
	 */
	private void drawTrailRecordSet(int dataScaleWidth) {
		int x0 = curveAreaBounds.x;
		int y0 = curveAreaBounds.y + curveAreaBounds.height;
		int width = curveAreaBounds.width;
		int height = curveAreaBounds.height;

		// check for activated horizontal grid
		boolean isCurveGridEnabled = trailRecordSet.getHorizontalGridType() > 0;

		// draw each record using sorted record set names
		boolean isDrawScaleInRecordColor = settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = settings.isDrawNumbersInRecordColor();

		// sync scales are used for suites (e.g. boxplot) AND synced records
		trailRecordSet.updateSyncRecordScale(); // should be better done in case of trail selection
		for (int i = 0; i < trailRecordSet.getRecordsSortedForDisplay().length; i++) {
			TrailRecord actualRecord = trailRecordSet.getRecordsSortedForDisplay()[i];
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (isActualRecordEnabled) log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b", //$NON-NLS-1$
					actualRecord.getName(), actualRecord.isVisible(), actualRecord.isDisplayable(), actualRecord.isScaleSynced(), actualRecord.isScaleVisible()));
			int[] numberTickMarks = setRecordDisplayValues(actualRecord);
			if (actualRecord.isScaleVisible())
				HistoCurveUtils.drawHistoScale(actualRecord, canvasImageGC, x0, y0, width, height, dataScaleWidth, isDrawScaleInRecordColor, isDrawNameInRecordColor, isDrawNumbersInRecordColor, numberTickMarks);

			if (isCurveGridEnabled && actualRecord.getOrdinal() == trailRecordSet.getHorizontalGridRecordOrdinal()) // check for activated horizontal grid
				HistoCurveUtils.drawCurveGrid(trailRecordSet, canvasImageGC, curveAreaBounds, settings.getGridDashStyle());

			if (isActualRecordEnabled) {
				// gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				// gc.drawRectangle(x0, y0-height, width, height);
				canvasImageGC.setClipping(x0 - 1, y0 - height - 1, width + 2, height + 2);
				if (actualRecord.getTrailSelector().isTrailSuite()) {
					HistoCurveUtils.drawHistoSuite(actualRecord, canvasImageGC, x0, y0, width, height, timeLine);
				} else {
					// CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
					HistoCurveUtils.drawHistoCurve(actualRecord, canvasImageGC, x0, y0, width, height, timeLine);
				}
				canvasImageGC.setClipping(canvasBounds);
			}
		}
	}

	@Override
	protected int[] defineNumberLeftRightScales() {
		// calculate number of curve scales, left and right side
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (int i = 0; i < trailRecordSet.getRecordsSortedForDisplay().length; i++) {
			TrailRecord tmpRecord = trailRecordSet.getRecordsSortedForDisplay()[i];
			if (tmpRecord != null && tmpRecord.isScaleVisible()) {
				log.finer(() -> "==>> " + tmpRecord.getName() + " isScaleVisible = " + tmpRecord.isScaleVisible()); //$NON-NLS-1$ //$NON-NLS-2$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		if (log.isLoggable(FINE)) log.log(FINE, "nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$
		int[] numberLeftRightScales = new int[] { numberCurvesLeft, numberCurvesRight };
		return numberLeftRightScales;
	}

	/**
	 * @param record
	 * @return the number of ticks {numberTicks, numberMiniTicks}
	 */
	private int[] setRecordDisplayValues(TrailRecord record) {
		int[] numberTickMarks = new int[] { 10, 5 };

		// (yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = record.getSyncMaxValue() / 1000.0;
		double yMinValue = record.getSyncMinValue() / 1000.0;
		if (log.isLoggable(FINE)) log.log(FINE, "unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;

		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getMinScaleValue();
			yMaxValueDisplay = record.getMaxScaleValue();
			yMinValue = VaultCollector.encodeVaultValue(record, yMinValueDisplay);
			yMaxValue = VaultCollector.encodeVaultValue(record, yMaxValueDisplay);
			if (log.isLoggable(FINE)) log.log(FINE, "defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
			if (log.isLoggable(FINE)) log.log(FINE, "defined -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			if (!record.getTrailSelector().isTrailSuite() && record.parallelStream().noneMatch(Objects::nonNull))
				; // in case of an empty record leave the values unchanged
			else {
				yMinValueDisplay = RecordingsCollector.decodeVaultValue(record, yMinValue);
				yMaxValueDisplay = RecordingsCollector.decodeVaultValue(record, yMaxValue);
			}
			if (log.isLoggable(FINE)) log.log(FINE, "undefined -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$

			if (Math.abs(yMaxValue - yMinValue) < .0001) { // equal value disturbs the scaling algorithm
				double deltaValueDisplay = yMaxValueDisplay - yMinValueDisplay;
				yMaxValueDisplay = MathUtils.roundUp(yMaxValueDisplay, deltaValueDisplay); // max
				yMinValueDisplay = MathUtils.roundDown(yMinValueDisplay, deltaValueDisplay); // min
				Object[] roundResult = MathUtils.adaptRounding(yMinValueDisplay, yMaxValueDisplay, false, curveAreaBounds.height / 25 >= 3
						? curveAreaBounds.height / 25 : 2);
				yMinValueDisplay = (Double) roundResult[0];
				yMaxValueDisplay = (Double) roundResult[1];
				numberTickMarks[0] = (Integer) roundResult[2];
				numberTickMarks[1] = (Integer) roundResult[3];
				yMinValue = VaultCollector.encodeVaultValue(record, yMinValueDisplay);
				yMaxValue = VaultCollector.encodeVaultValue(record, yMaxValueDisplay);
				if (log.isLoggable(FINE)) log.log(FINE, String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue)); //$NON-NLS-1$
				if (log.isLoggable(FINE)) log.log(FINE, "rounded -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (record.isStartpointZero()) {
				// check if the main part of the curve is on positive side
				if (record.getAvgValue() > 0) { // main part of curve is on positive side
					yMinValueDisplay = 0;
					yMinValue = VaultCollector.encodeVaultValue(record, yMinValueDisplay);
				} else {// main part of curve is on negative side
					yMaxValueDisplay = 0;
					yMaxValue = VaultCollector.encodeVaultValue(record, yMaxValueDisplay);
				}
				if (log.isLoggable(FINE)) log.log(FINE, "scale starts at 0; yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
				if (log.isLoggable(FINE))
					log.log(FINE, "scale starts at 0 -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		record.setMinMaxScaleValue(yMinValueDisplay, yMaxValueDisplay);
		record.setSyncedMinMaxDisplayValues(yMinValue, yMaxValue);
		return numberTickMarks;
	}

}
