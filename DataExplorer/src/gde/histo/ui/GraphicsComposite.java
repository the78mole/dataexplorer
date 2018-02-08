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
import static java.util.logging.Level.SEVERE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

import gde.GDE;
import gde.config.Settings;
import gde.data.AbstractRecordSet;
import gde.data.Channels;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.datasources.HistoSet;
import gde.histo.exclusions.ExclusionData;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.menu.AbstractTabAreaContextMenu.TabMenuOnDemand;
import gde.histo.ui.menu.ChartTabAreaContextMenu;
import gde.histo.utils.HistoCurveUtils;
import gde.histo.utils.HistoTimeLine;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.MathUtils;
import gde.utils.StringHelper;

/**
 * Curves for the histo graphics window.
 * @author Thomas Eickert
 */
public final class GraphicsComposite extends AbstractChartComposite {
	private static final String	$CLASS_NAME	= GraphicsComposite.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	/**
	 * Data for the life cycle of a graphics composite drawing.
	 */
	public static final class Graphics implements IChartData {

		private final TrailRecord				trailRecord;
		private final GraphicsComposite	parent;

		// synchronize
		protected int										syncMaxValue			= Integer.MAX_VALUE;	// max value of the curve if synced
		protected int										syncMinValue			= Integer.MIN_VALUE;	// min value of the curve if synced

		// display the record
		double													displayScaleFactorTime;
		protected double								displayScaleFactorValue;
		protected double								syncMasterFactor	= 1.0;								// synchronized scale and different measurement factors
		protected double								minDisplayValue;												// min value in device units, correspond to draw area
		protected double								maxDisplayValue;												// max value in device units, correspond to draw area

		int															numberScaleTicks	= 0;

		private int[]										numberTickMarks;

		public Graphics(TrailRecord trailRecord, GraphicsComposite parent) {
			this.trailRecord = trailRecord;
			this.parent = parent;
		}

		public double getDisplayScaleFactorValue() {
			return this.displayScaleFactorValue;
		}

		/**
		 * @param drawAreaHeight - used to calculate the displayScaleFactorValue to set
		 */
		public void setDisplayScaleFactorValue(int drawAreaHeight) {
			displayScaleFactorValue = (1.0 * drawAreaHeight) / (maxDisplayValue - minDisplayValue);
			AbstractRecordSet abstractParent = trailRecord.getAbstractParent();
			if (abstractParent.isOneOfSyncableRecord(trailRecord.getName()) && trailRecord.getFactor() / abstractParent.get(abstractParent.getSyncMasterRecordOrdinal(trailRecord.getName())).getFactor() != 1) {
				syncMasterFactor = trailRecord.getFactor() / abstractParent.get(abstractParent.getSyncMasterRecordOrdinal(trailRecord.getName())).getFactor();
				displayScaleFactorValue = displayScaleFactorValue * syncMasterFactor;
			}
			if (log.isLoggable(Level.FINER))
				log.log(Level.FINER, String.format(Locale.ENGLISH, "drawAreaHeight = %d displayScaleFactorValue = %.3f (this.maxDisplayValue - this.minDisplayValue) = %.3f", //$NON-NLS-1$
						drawAreaHeight, displayScaleFactorValue, (maxDisplayValue - minDisplayValue)));

		}

		public double getMinDisplayValue() {
			return this.minDisplayValue;
		}

		public double getMaxDisplayValue() {
			return this.maxDisplayValue;
		}

		public int getNumberScaleTicks() {
			return this.numberScaleTicks;
		}

		public void setNumberScaleTicks(int newNumberScaleTicks) {
			this.numberScaleTicks = newNumberScaleTicks;
		}

		public int[] getNumberTickMarks() {
			return this.numberTickMarks;
		}

		public void setNumberTickMarks(int[] numberTickMarks) {
			this.numberTickMarks = numberTickMarks;
		}

		public void setSyncedMinMaxDisplayValues(double newMinValue, double newMaxValue) {
			this.minDisplayValue = HistoSet.decodeVaultValue(trailRecord, newMinValue);
			this.maxDisplayValue = HistoSet.decodeVaultValue(trailRecord, newMaxValue);

			TrailRecordSet trailRecordset = trailRecord.getParent();
			if (trailRecordset.isOneOfSyncableRecord(trailRecord.getName())) {
				for (TrailRecord record : trailRecordset.getScaleSyncedRecords(trailRecordset.getSyncMasterRecordOrdinal(trailRecord.getName()))) {
					parent.getGraphics(record).minDisplayValue = this.minDisplayValue;
					parent.getGraphics(record).maxDisplayValue = this.maxDisplayValue;
				}
			}
			log.fine(trailRecord.getName() + " yMinValue = " + newMinValue + "; yMaxValue = " + newMaxValue);
		}

		/**
		 * Take the current max/minValues form this record and recalculate the synced max/minValues.
		 * Support suites.
		 */
		public void setSyncMaxMinValue() {
			if (trailRecord.getTrailSelector().isTrailSuite()) {
				int suiteMaxValue = trailRecord.getSuiteRecords().getSuiteMaxValue();
				int suiteMinValue = trailRecord.getSuiteRecords().getSuiteMinValue();
				int tmpMaxValue = suiteMaxValue == suiteMinValue ? suiteMaxValue + 100 : suiteMaxValue;
				int tmpMinValue = suiteMaxValue == suiteMinValue ? suiteMinValue - 100 : suiteMinValue;
				this.syncMaxValue = (int) (tmpMaxValue * getSyncMasterFactor());
				this.syncMinValue = (int) (tmpMinValue * getSyncMasterFactor());
			} else {
				this.syncMaxValue = (int) (trailRecord.getMaxValue() * getSyncMasterFactor());
				this.syncMinValue = (int) (trailRecord.getMinValue() * getSyncMasterFactor());
			}
			log.finer(() -> trailRecord.getName() + "  syncMin = " + this.getSyncMinValue() + "; syncMax = " + this.getSyncMaxValue());
		}

		public void setSyncMinMax(int newMin, int newMax) {
			if (newMin == Integer.MIN_VALUE && newMax == Integer.MAX_VALUE) return; // for compatibility with initSyncedScales
			this.syncMinValue = newMin;
			this.syncMaxValue = newMax;
			log.finer(() -> trailRecord.getName() + " syncMinValue=" + newMin + " syncMaxValue=" + newMax);
		}

		public int getSyncMinValue() {
			return this.syncMinValue == this.syncMaxValue ? this.syncMinValue - 100 : this.syncMinValue;
		}

		public int getSyncMaxValue() {
			return this.syncMaxValue == this.syncMinValue ? this.syncMaxValue + 100 : this.syncMaxValue;
		}

		public double getSyncMasterFactor() {
			return this.syncMasterFactor;
		}

		@Override
		public TrailRecord getTrailRecord() {
			return this.trailRecord;
		}

	}

	private final HistoTimeLine					timeLine	= new HistoTimeLine();
	/**
	 * Key is the record name.
	 */
	private final Map<String, Graphics>	chartData	= new LinkedHashMap<>();

	GraphicsComposite(SashForm useParent, CTabItem parentWindow) {
		super(useParent, parentWindow, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);

		// get the background colors
		this.curveAreaBackground = this.settings.getGraphicsCurveAreaBackground();
		this.surroundingBackground = this.settings.getGraphicsSurroundingBackground();
		this.curveAreaBorderColor = this.settings.getGraphicsCurvesBorderColor();

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new ChartTabAreaContextMenu();

		init();

		enableGraphicsScale(true);
	}

	private void init() {
		this.setLayout(null);
		this.setDragDetect(false);
		this.setBackground(this.surroundingBackground);

		this.contextMenu.createMenu(this.popupmenu);

		// help lister does not get active on Composite as well as on Canvas
		this.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event evt) {
				log.finer(() -> "GraphicsComposite.controlResized() = " + evt); //$NON-NLS-1$
				Rectangle clientRect = getClientArea();
				Point size = new Point(clientRect.width, clientRect.height);
				log.finer(() -> oldSize + " - " + size); //$NON-NLS-1$
				if (!oldSize.equals(size)) {
					log.fine(() -> "size changed, update " + oldSize + " - " + size); //$NON-NLS-1$ //$NON-NLS-2$
					oldSize = size;
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
						for (Entry<DirectoryType, Path> directoryEntry : DataExplorer.getInstance().getPresentHistoExplorer().getHistoSet().getValidatedDirectories().entrySet()) {
							String fileName = directoryEntry.getValue().getFileName().toString();
							String truncatedPath = fileName.length() > 22 ? fileName.substring(0, 22) + ellipsisText : fileName;
							sb.append(GDE.STRING_BLANK + GDE.STRING_OR + GDE.STRING_BLANK).append(truncatedPath);
							toolTipText += GDE.STRING_NEW_LINE + directoryEntry.getKey().toString() + GDE.STRING_BLANK_COLON_BLANK + directoryEntry.getValue().toString();
						}
						headerText = sb.length() >= 3 ? sb.substring(3) : GDE.STRING_EMPTY;
						if (!toolTipText.isEmpty()) toolTipText = toolTipText.substring(1);
					}
					if (!headerText.equals(graphicsHeaderText)) {
						graphicsHeaderText = headerText;
						graphicsHeader.setText(headerText);
					}
					{ // getFullText
						int levelMax = Settings.getInstance().getSubDirectoryLevelMax();
						String levelsText = levelMax > 0 ? GDE.STRING_NEW_LINE + "+ " + levelMax + GDE.STRING_BLANK + Messages.getString(MessageIds.GDE_MSGT0870)
								: GDE.STRING_EMPTY;
						if (!toolTipText.isEmpty()) toolTipText = toolTipText + levelsText;
					}
					graphicsHeader.setToolTipText(toolTipText);
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
					GraphicsComposite.this.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
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
		long nanoTime = System.nanoTime();
		drawAreaPaintControl();
		log.time(() -> "drawTime=" + StringHelper.getFormatedTime("ss:SSS", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanoTime)));
	}

	/**
	 * @param evt
	 */
	void mouseMoveAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0 && this.canvasImage != null) {
			Point point = checkCurveBounds(evt.x, evt.y);

			this.graphicCanvas.setCursor(this.application.getCursor());
			if (point.x > 0 && point.y > this.curveAreaBounds.height - this.curveAreaBounds.y) {
				Long timestamp_ms = this.timeLine.getSnappedTimestamp(point.x);
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

			if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
				windowActor.processMouseDownMove(this.timeLine.getAdjacentTimestamp(point.x));
			} else {
				windowActor.processMouseUpMove(point);
			}
		}

	}

	/**
	 * @param evt
	 */
	void mouseDownAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Point point = checkCurveBounds(evt.x, evt.y);

			if (evt.button == 1) {
				windowActor.processMouseDownAction(point);
			} else if (evt.button == 3) { // right button
				popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), GDE.STRING_TRUE);
				popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), Arrays.stream(ExclusionData.getExcludedTrusses()).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR)));
				if (point.x > 0 && point.x < this.curveAreaBounds.width) {
					Map<DataTag, String> dataTags = trailRecordSet.getDataTags(trailRecordSet //
							.getIndex(timeLine.getAdjacentTimestamp(point.x))); // is already relative to curve area
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), dataTags.get(DataTag.LINK_PATH));
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), dataTags.get(DataTag.FILE_PATH));
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), dataTags.get(DataTag.RECORDSET_BASE_NAME));
				} else {
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), GDE.STRING_EMPTY);
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), GDE.STRING_EMPTY);
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), GDE.STRING_EMPTY);
				}
			}
		}
	}

	/**
	 * @param evt
	 */
	void mouseUpAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Point point = checkCurveBounds(evt.x, evt.y);

			if (evt.button == 1) {
				windowActor.processMouseUpAction(point);
			}
		}
	}

	@Override
	protected void setFixedGraphicCanvas(Rectangle realBounds) {
		// not supported
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

		this.timeLine.drawTimeLine(canvasImageGC);

		long startTime = new Date().getTime();
		drawTrailRecordSet(dataScaleWidth);
		log.fine(() -> "draw records time = " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() - startTime)));
	}

	/**
	 *
	 */
	@Override
	protected void defineLayoutParams() {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		this.timeLine.initialize(trailRecordSet, curveAreaBounds);
		this.chartData.clear();
		for (TrailRecord record : trailRecordSet.getValues()) {
			this.chartData.put(record.getName(), new Graphics(record, this));
		}
		// sync scales are used for suites (e.g. boxplot) AND synced records
		trailRecordSet.updateSyncGraphicsScale(this);
		for (int i = 0; i < trailRecordSet.getRecordsSortedForDisplay().length; i++) {
			TrailRecord actualRecord = trailRecordSet.getRecordsSortedForDisplay()[i];
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (isActualRecordEnabled) log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b", //$NON-NLS-1$
					actualRecord.getName(), actualRecord.isVisible(), actualRecord.isDisplayable(), actualRecord.isScaleSynced(), actualRecord.isScaleVisible()));
			setRecordDisplayValues(actualRecord);
		}
	}

	/**
	 * Draw the visible curves for all channel items.
	 * Support multiple curves for one single item.
	 */
	private void drawTrailRecordSet(int dataScaleWidth) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();

		// check for activated horizontal grid
		boolean isCurveGridEnabled = trailRecordSet.getValueGridType() > 0;

		// draw each record using sorted record set names
		boolean isDrawScaleInRecordColor = settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = settings.isDrawNumbersInRecordColor();

		for (int i = 0; i < trailRecordSet.getRecordsSortedForDisplay().length; i++) {
			TrailRecord actualRecord = trailRecordSet.getRecordsSortedForDisplay()[i];
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable();
			if (isActualRecordEnabled) log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b", //$NON-NLS-1$
					actualRecord.getName(), actualRecord.isVisible(), actualRecord.isDisplayable(), actualRecord.isScaleSynced(), actualRecord.isScaleVisible()));
			if (actualRecord.isScaleVisible())
				HistoCurveUtils.drawHistoScale(getGraphics(actualRecord), canvasImageGC, curveAreaBounds, dataScaleWidth, isDrawScaleInRecordColor, isDrawNameInRecordColor, isDrawNumbersInRecordColor);

			if (isCurveGridEnabled && actualRecord.getOrdinal() == trailRecordSet.getValueGridRecordOrdinal()) // check for activated horizontal grid
				HistoCurveUtils.drawCurveGrid(trailRecordSet, canvasImageGC, curveAreaBounds, settings.getGridDashStyle());

			if (isActualRecordEnabled) {
				// gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				// gc.drawRectangle(x0, y0-height, width, height);
				canvasImageGC.setClipping(curveAreaBounds.x - 1, curveAreaBounds.y - 1, curveAreaBounds.width + 2, curveAreaBounds.height + 2);
				if (actualRecord.getTrailSelector().isTrailSuite()) {
					HistoCurveUtils.drawHistoSuite(this.getGraphics(actualRecord), canvasImageGC, curveAreaBounds, timeLine);
				} else {
					// CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
					HistoCurveUtils.drawHistoCurve(this.getGraphics(actualRecord), canvasImageGC, curveAreaBounds, timeLine);
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
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
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

	private void setRecordDisplayValues(TrailRecord record) { // todo simplify the implementation
		int[] numberTickMarks = new int[] { 10, 5 };

		// (yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = getGraphics(record).getSyncMaxValue() / 1000.0;
		double yMinValue = getGraphics(record).getSyncMinValue() / 1000.0;
		if (log.isLoggable(FINE)) log.log(FINE, "unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;

		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getMinScaleValue();
			yMaxValueDisplay = record.getMaxScaleValue();
			yMinValue = HistoSet.encodeVaultValue(record, yMinValueDisplay);
			yMaxValue = HistoSet.encodeVaultValue(record, yMaxValueDisplay);
			if (log.isLoggable(FINE)) log.log(FINE, "defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
			if (log.isLoggable(FINE)) log.log(FINE, "defined -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			if (!record.getTrailSelector().isTrailSuite() && record.parallelStream().noneMatch(Objects::nonNull))
				; // in case of an empty record leave the values unchanged
			else {
				yMinValueDisplay = HistoSet.decodeVaultValue(record, yMinValue);
				yMaxValueDisplay = HistoSet.decodeVaultValue(record, yMaxValue);
			}
			if (log.isLoggable(FINE)) log.log(FINE, "undefined -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$

			double deltaValueDisplay = yMaxValueDisplay - yMinValueDisplay;
			if (Math.abs(deltaValueDisplay) < .0001) { // equal value disturbs the scaling algorithm
				yMaxValueDisplay = MathUtils.roundUp(yMaxValueDisplay, deltaValueDisplay); // max
				yMinValueDisplay = MathUtils.roundDown(yMinValueDisplay, deltaValueDisplay); // min
				Object[] roundResult = MathUtils.adaptRounding(yMinValueDisplay, yMaxValueDisplay, false, curveAreaBounds.height / 25 >= 3
						? curveAreaBounds.height / 25 : 2);
				yMinValueDisplay = (Double) roundResult[0];
				yMaxValueDisplay = (Double) roundResult[1];
				numberTickMarks[0] = (Integer) roundResult[2];
				numberTickMarks[1] = (Integer) roundResult[3];
				yMinValue = HistoSet.encodeVaultValue(record, yMinValueDisplay);
				yMaxValue = HistoSet.encodeVaultValue(record, yMaxValueDisplay);
				if (log.isLoggable(FINE)) log.log(FINE, String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue)); //$NON-NLS-1$
				if (log.isLoggable(FINE)) log.log(FINE, "rounded -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (record.isStartpointZero()) {
				// check if the main part of the curve is on positive side
				if (record.getAvgValue() > 0) { // main part of curve is on positive side
					yMinValueDisplay = 0;
					yMinValue = HistoSet.encodeVaultValue(record, yMinValueDisplay);
				} else {// main part of curve is on negative side
					yMaxValueDisplay = 0;
					yMaxValue = HistoSet.encodeVaultValue(record, yMaxValueDisplay);
				}
				if (log.isLoggable(FINE)) log.log(FINE, "scale starts at 0; yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$
				if (log.isLoggable(FINE))
					log.log(FINE, "scale starts at 0 -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		record.setMinMaxScaleValue(yMinValueDisplay, yMaxValueDisplay);
		getGraphics(record).setSyncedMinMaxDisplayValues(yMinValue, yMaxValue);
		getGraphics(record).setNumberTickMarks(numberTickMarks);
	}

	@Override
	public void setMeasuringActive(Measure measure) {
		measuring = new GraphicsMeasuring(this, measure);
		if (this.canvasBounds == null) return; // fixed window size
		// draw full graph at first because the curve area might have changed (due to new new scales)
		drawAreaPaintControl();

		// if (this.canvasBounds.height == 0) return; // fixed window size
		measuring.drawMeasuring();
	}

	public HistoTimeLine getTimeLine() {
		return this.timeLine;
	}

	public Map<String, Graphics> getChartData() {
		return this.chartData;
	}

	@Override
	public IChartData getChartData(TrailRecord trailRecord) {
		return this.chartData.get(trailRecord.getName());
	}

	public Graphics getGraphics(TrailRecord record) {
		return this.chartData.get(record.getName());
	}

}
