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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
import gde.data.Channels;
import gde.histo.cache.ExtendedVault;
import gde.histo.datasources.DirectoryScanner.DirectoryType;
import gde.histo.datasources.HistoSet;
import gde.histo.exclusions.ExclusionData;
import gde.histo.exclusions.InclusionData;
import gde.histo.recordings.TrailDataTags.DataTag;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordFormatter;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.recordings.TrailRecordSet.Outliers;
import gde.histo.recordings.TrailRecordSetFormatter;
import gde.histo.ui.data.SummarySpots;
import gde.histo.ui.data.SummarySpots.Density;
import gde.histo.ui.menu.AbstractTabAreaContextMenu.TabMenuOnDemand;
import gde.histo.ui.menu.ChartTabAreaContextMenu;
import gde.histo.ui.menu.ChartTabAreaContextMenu.SummaryWarning;
import gde.histo.utils.HistoCurveUtils;
import gde.log.Level;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.MathUtils;
import gde.utils.StringHelper;

/**
 * Curves for the histo summary window.
 * @author Thomas Eickert
 */
public final class SummaryComposite extends AbstractChartComposite {
	private static final String	$CLASS_NAME	= SummaryComposite.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	static final int						UNK_GAP			= 2;

	private VolatileComment			volatileComment;

	/**
	 * Comment caching.
	 * The comment text and tooltip remains active for a defined number of drawing actions.
	 */
	private class VolatileComment {
		final String	textLines;
		final String	toolTip;
		int						remainingAccessCounter;

		private VolatileComment(String textLines, String toolTip, int accessCounter) {
			this.textLines = textLines;
			this.toolTip = toolTip;
			this.remainingAccessCounter = accessCounter;
		}

		private String getTextLines() {
			if (--remainingAccessCounter < 0) throw new UnsupportedOperationException();
			return remainingAccessCounter >= 0 ? this.textLines : GDE.STRING_EMPTY;
		}

		private String getToolTip() {
			return remainingAccessCounter > 0 ? this.toolTip : GDE.STRING_EMPTY;
		}

		private boolean isAvailable() {
			return remainingAccessCounter > 0;
		}
	}

	SummaryComposite(SashForm useParent, CTabItem parentWindow) {
		super(useParent, parentWindow, SWT.NONE);
		SWTResourceManager.registerResourceUser(this);

		// get the background colors
		this.curveAreaBackground = this.settings.getGraphicsCurveAreaBackground();
		this.surroundingBackground = this.settings.getGraphicsSurroundingBackground();
		this.curveAreaBorderColor = this.settings.getGraphicsCurvesBorderColor();

		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new ChartTabAreaContextMenu();

		init();

		enableGraphicsScale(false);
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
						for (Entry<DirectoryType, Path> directoryEntry : windowActor.getHistoSet().getValidatedDirectories().entrySet()) {
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
					SummaryComposite.this.graphicCanvas.setCursor(DataExplorer.getInstance().getCursor());
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

	void mouseMoveAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0 && this.canvasImage != null) {
			Point point = checkCurveBounds(evt.x, evt.y);

			this.graphicCanvas.setCursor(this.application.getCursor());
			List<Integer> snappedIndices = getSnappedIndices(point);
			TrailRecord record = getDisplayRecord(point);
			if (point.x > 0 && point.x < curveAreaBounds.width && !snappedIndices.isEmpty()) {
				this.graphicCanvas.setCursor(SWTResourceManager.getCursor(SWT.CURSOR_CROSS));
				String text = TrailRecordSetFormatter.getFileNameLines(snappedIndices);
				if (snappedIndices.size() == 1) {
					TrailRecordFormatter formatter = new TrailRecordFormatter(record);
					ExtendedVault vault = trailRecordSet.getPickedVaults().getVault(snappedIndices.get(0));
					String outliers = Arrays.stream(record.getVaultOutliers(vault)).mapToObj(o -> formatter.getScaleValue(o)).collect(Collectors.joining(GDE.STRING_BLANK_COLON_BLANK));
					String scraps = Arrays.stream(record.getVaultScraps(vault)).mapToObj(o -> formatter.getScaleValue(o)).collect(Collectors.joining(GDE.STRING_BLANK_COLON_BLANK));
					if (!outliers.isEmpty()) text += "\n o " + outliers;
					if (!scraps.isEmpty()) text += "\n s " + scraps;
				}
				if (this.graphicCanvas.getToolTipText() == null || !(text.equals(this.graphicCanvas.getToolTipText()))) {
					log.log(Level.FINEST, "", text);
					this.graphicCanvas.setToolTipText(text);
				}
			} else {
				if (point.x == 0 && record != null && record.getSummary().getMinMaxWarning()[0] != null) { // left scale warnings
					String hintForClick = GDE.STRING_NEW_LINE + Messages.getString(MessageIds.GDE_MSGT0914);
					this.graphicCanvas.setToolTipText(new TrailRecordFormatter(record).defineFormattedMinWarning() + hintForClick);
				} else if (point.x == curveAreaBounds.width && record != null && record.getSummary().getMinMaxWarning()[1] != null) { // right
					String hintForClick = GDE.STRING_NEW_LINE + Messages.getString(MessageIds.GDE_MSGT0914);
					this.graphicCanvas.setToolTipText(new TrailRecordFormatter(record).defineFormattedMaxWarning() + hintForClick);
				} else {
					this.graphicCanvas.setToolTipText(null);
				}
			}

			if ((evt.stateMask & SWT.NO_FOCUS) == SWT.NO_FOCUS) {
// windowActor.processMouseDownMove(this.timeLine.getAdjacentTimestamp(point.x));
			} else
				windowActor.processMouseUpMove(point);
		}
	}

	/**
	 * @param point is the mouse coordinate relative and corrected to the curve area bounds
	 * @return the record identified by the mouse position or null
	 */
	private TrailRecord getDisplayRecord(Point point) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet == null) return null; // graphics data have not been determined yet
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return null; // concurrent activity

		int stripHeight = fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();
		int xPos = point.x;
		int yPos = point.y; // todo - UNK_GAP;
		log.finest(() -> String.format("x=%d y=%d  stripXPos=%d  stripYPos=%d  stripHeight=%d", //
				point.x, point.y, xPos, yPos, stripHeight));
		if (yPos < 0 || yPos >= stripHeight * trailRecordSet.getDisplayRecords().size() - 1) // below or above the drawing area
			return null;

		int displayOrdinal = Math.min(yPos / stripHeight, trailRecordSet.getDisplayRecords().size() - 1);
		if (displayOrdinal < 0) return null; // mouse position does not point to a visible record

		TrailRecord record = trailRecordSet.getDisplayRecords().get(displayOrdinal);
		log.finest(() -> String.format("x=%d y=%d  displayOrdinal=%d  record=%s", //
				point.x, point.y, displayOrdinal, record.getName()));
		return record;
	}

	/**
	 * @param point is the mouse coordinate relative and restricted to the curve area bounds
	 * @return the recordset timestamp indices of the log data identified by the mouse position or an empty list
	 */
	private List<Integer> getSnappedIndices(Point point) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet == null) return new ArrayList<>(); // graphics data have not been determined yet
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return new ArrayList<>(); // concurrent activity

		TrailRecord record = getDisplayRecord(point);
		if (record == null || (!record.isVisible() && settings.isPartialDataTable())) return new ArrayList<>(); // mouse not pointing to a visible record

		int stripHeight = fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();
		int stripYPos = (point.y - UNK_GAP) % stripHeight;

		List<Integer> snappedIndexes = record.getSummary().getSummarySpots().getSnappedIndexes(point.x, stripYPos);
		log.log(FINER, "", Arrays.toString(snappedIndexes.toArray()));
		return snappedIndexes;
	}

	void mouseDownAction(MouseEvent evt) {
		if (Channels.getInstance().getActiveChannel() == null) return;

		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		if (this.canvasImage != null && trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			Point point = checkCurveBounds(evt.x, evt.y);

			TrailRecord record = getDisplayRecord(point);
			if (point.x == 0 && record != null && record.getSummary().getMinMaxWarning()[0] != null) { // left scale warnings
				changeChartAsPerWarning(record, 0);
			} else if (point.x == curveAreaBounds.width && record != null && record.getSummary().getMinMaxWarning()[1] != null) { // right
				changeChartAsPerWarning(record, 1);
			}

			if (evt.button == 1) {
				windowActor.processMouseDownAction(point);
			} else if (evt.button == 3) { // right button
				popupmenu.setData(TabMenuOnDemand.IS_CURSOR_IN_CANVAS.name(), GDE.STRING_TRUE);
				popupmenu.setData(TabMenuOnDemand.EXCLUDED_LIST.name(), Arrays.stream(ExclusionData.getExcludedTrusses()).collect(Collectors.joining(GDE.STRING_CSV_SEPARATOR)));
				Path dataPath = application.getPresentHistoExplorer().getHistoSet().getValidatedDirectories().get(DirectoryType.DATA);
				String[] includedRecordNames = InclusionData.getInstance(dataPath).getIncludedRecordNames();
				SummaryWarning summaryWarning = new SummaryWarning(dataPath, record != null ? record.getName() : GDE.STRING_EMPTY, includedRecordNames);
				popupmenu.setData(TabMenuOnDemand.SUMMARY_WARNING.name(), summaryWarning);

				List<Integer> snappedIndices = getSnappedIndices(point);
				if (snappedIndices.size() == 1) {
					Map<DataTag, String> dataTags = trailRecordSet.getDataTags(snappedIndices.get(0));
					popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), dataTags.get(DataTag.LINK_PATH));
					popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), dataTags.get(DataTag.FILE_PATH));
					popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), dataTags.get(DataTag.RECORDSET_BASE_NAME));
				} else {
					if (point.x == 0 && record != null && record.getSummary().getMinMaxWarning()[0] != null) { // left scale warnings
						Outliers outlier = record.getSummary().getMinMaxWarning()[0];
						ExtendedVault vault = record.getParent().getPickedVaults().getVault(outlier.getIndices().get(0));
						popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), vault.getLogLinkPath());
						popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), vault.getLogFilePath());
						popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), vault.getLogRecordsetBaseName());
					} else if (point.x == curveAreaBounds.width && record != null && record.getSummary().getMinMaxWarning()[1] != null) { // right
						Outliers outlier = record.getSummary().getMinMaxWarning()[1];
						ExtendedVault vault = record.getParent().getPickedVaults().getVault(outlier.getIndices().get(0));
						popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), vault.getLogLinkPath());
						popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), vault.getLogFilePath());
						popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), vault.getLogRecordsetBaseName());
					} else {
						popupmenu.setData(TabMenuOnDemand.DATA_LINK_PATH.name(), GDE.STRING_EMPTY);
						popupmenu.setData(TabMenuOnDemand.DATA_FILE_PATH.name(), GDE.STRING_EMPTY);
						popupmenu.setData(TabMenuOnDemand.RECORDSET_BASE_NAME.name(), GDE.STRING_EMPTY);
					}
				}
			}
		}

	}

	/**
	 * @param actionType is 0 for a change after clicking on the left (min) side of the chart
	 */
	private void changeChartAsPerWarning(TrailRecord record, int actionType) {
		TrailRecordFormatter recordFormatter = new TrailRecordFormatter(record);
		volatileComment = new VolatileComment(recordFormatter.defineMinMaxWarningText(), Messages.getString(MessageIds.GDE_MSGT0909), 3);
		windowActor.setTrailVisible(record, record.getSummary().getMinMaxWarning()[actionType].getSelectIndex());
		windowActor.updateHistoTabs(false, false);
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
		if (Settings.getInstance().isSmartStatistics()) {
			int heightWithScale = realBounds.height + getXScaleHeight() + AbstractChartComposite.DEFAULT_TOP_GAP;
			setFixedGraphicCanvas(realBounds.y - AbstractChartComposite.DEFAULT_TOP_GAP, heightWithScale);
		} else {
			setFixedGraphicCanvas(realBounds.y - AbstractChartComposite.DEFAULT_TOP_GAP, AbstractChartComposite.ZERO_CANVAS_HEIGHT);
		}
	}

	@Override
	protected void drawCurveArea(int dataScaleWidth) {
		canvasImageGC.setBackground(this.curveAreaBackground);
		canvasImageGC.fillRectangle(this.curveAreaBounds);
		canvasImageGC.setBackground(this.surroundingBackground);

		HistoCurveUtils.drawCurveAreaBorders(canvasImageGC, curveAreaBounds, curveAreaBorderColor);

		long startTime = new Date().getTime();
		if (fixedCanvasHeight > AbstractChartComposite.ZERO_CANVAS_HEIGHT) drawTrailRecordSet(dataScaleWidth);
		log.fine(() -> "draw records time = " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() - startTime)));
	}

	@Override
	protected void defineLayoutParams() {
		// initialize early in order to avoid problems in mouse move events
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		for (TrailRecord record : trailRecordSet.getValues()) {
			record.resetSummary();
		}
		trailRecordSet.updateSyncSummaryScale();
		if (trailRecordSet.getDisplayRecords() == null || trailRecordSet.getDisplayRecords().isEmpty()) return; // concurrent activity

		Density density = Density.toDensity(curveAreaBounds.width, trailRecordSet.getTimeStepSize());
		int stripHeight = fixedCanvasHeight / trailRecordSet.getDisplayRecords().size();
		log.fine(() -> "curve area bounds = " + curveAreaBounds.toString());

		for (int i = 0; i < trailRecordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = trailRecordSet.getDisplayRecords().get(i);
			if (record.isVisible()) log.fine(() -> String.format("record=%s  isVisible=%b isDisplayable=%b isScaleVisible=%b", //$NON-NLS-1$
					record.getName(), record.isVisible(), record.isDisplayable(), record.isScaleSynced(), record.isScaleVisible()));
			//setRecordDisplayValues(record);

			Rectangle drawStripBounds = new Rectangle(curveAreaBounds.x, curveAreaBounds.y + stripHeight * i + UNK_GAP, curveAreaBounds.width, stripHeight);
			log.finer(() -> record.getName() + "  x0=" + curveAreaBounds.x + " y0=" + drawStripBounds.y + " width=" + curveAreaBounds.width + " height=" + stripHeight);

			SummarySpots summarySpots = record.getSummary().getSummarySpots();
			summarySpots.initialize(drawStripBounds, density);
		}
	}

	/**
	 * Draw all the plots for all channel items.
	 * Support multiple plots for one single item.
	 */
	private void drawTrailRecordSet(int dataScaleWidth) {
		boolean isDrawScaleInRecordColor = settings.isDrawScaleInRecordColor();
		boolean isDrawNameInRecordColor = settings.isDrawNameInRecordColor();
		boolean isDrawNumbersInRecordColor = settings.isDrawNumbersInRecordColor();
		boolean isPartialDataTable = settings.isPartialDataTable();
		boolean isSummaryBoxVisible = settings.isSummaryBoxVisible();
		boolean isSummarySpreadVisible = settings.isSummarySpreadVisible();
		boolean isSummarySpotsVisible = settings.isSummarySpotsVisible();
		boolean isCurveSelector = windowActor.isCurveSelectorEnabled();
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		Path dataDir = application.getPresentHistoExplorer().getHistoSet().getValidatedDirectories().get(DirectoryType.DATA);
		Optional<InclusionData> inclusionData = InclusionData.getExistingInstance(dataDir, trailRecordSet.getRecordNames());
		List<String> exclusiveNames = Arrays.asList(inclusionData.map(d -> d.getIncludedRecordNames(trailRecordSet.getRecordNames())).orElse(new String[0]));

		for (int i = 0; i < trailRecordSet.getDisplayRecords().size(); i++) {
			TrailRecord record = trailRecordSet.getDisplayRecords().get(i);
			SummarySpots summarySpots = record.getSummary().getSummarySpots();

			double decodedScaleMin = record.getSummary().defineScaleMin();
			double decodedScaleMax = record.getSummary().defineScaleMax();

			HistoCurveUtils.drawChannelItemScale(record, canvasImageGC, dataScaleWidth, decodedScaleMin, decodedScaleMax, //
					isDrawScaleInRecordColor, isDrawNumbersInRecordColor);
			if (exclusiveNames.contains(record.getName()))
				HistoCurveUtils.drawChannelItemWarnMarker(record, canvasImageGC, dataScaleWidth, isDrawNumbersInRecordColor);
			if (record.size() == 0) continue; // nothing to display

			if (record.isVisible() || !isPartialDataTable) {
				if (isSummarySpreadVisible) HistoCurveUtils.drawChannelItemSpread(record, canvasImageGC);
				if (!isCurveSelector && record.isVisible()) HistoCurveUtils.drawChannelItemText(record, canvasImageGC, isDrawNameInRecordColor);
				if (isSummaryBoxVisible) HistoCurveUtils.drawChannelItemBoxplot(record, canvasImageGC, dataScaleWidth, decodedScaleMin, decodedScaleMax, //
						isDrawNumbersInRecordColor, isSpaceBelow(i));
				if (isSummarySpotsVisible) summarySpots.drawMarkers(canvasImageGC);
				summarySpots.drawRecentMarkers(canvasImageGC);
			}
			boolean isDefaultOrExclusiveWarning = inclusionData.map(d -> d.isIncluded(record.getName())).orElse(true);
			if (isDefaultOrExclusiveWarning && !HistoSet.isGpsCoordinates(record)) {
				HistoCurveUtils.drawChannelItemWarnings(record, canvasImageGC, dataScaleWidth);
			}
			canvasImageGC.setBackground(this.surroundingBackground);
		}
	}

	/**
	 * @param displayOrdinal is the counter based on the record display sequence
	 * @return true if there is no record drawn below the current record
	 */
	private boolean isSpaceBelow(int displayOrdinal) {
		TrailRecordSet trailRecordSet = retrieveTrailRecordSet();
		int nextOrdinal = displayOrdinal + 1;
		if (nextOrdinal < trailRecordSet.getDisplayRecords().size()) {
			TrailRecord nextRecord = trailRecordSet.getDisplayRecords().get(nextOrdinal);
			return !nextRecord.isVisible() && settings.isPartialDataTable();
		} else
			return true;
	}

	@Override
	protected int[] defineNumberLeftRightScales() {
		return new int[] { 1, 1 };
	}

	private void setRecordDisplayValues(TrailRecord record) { // todo simplify the implementation
		// (yMaxValue - yMinValue) defines the area to be used for the curve
		// point values divided by 1000
		double yMaxValue = record.getSyncMaxValue() / 1000.0;
		double yMinValue = record.getSyncMinValue() / 1000.0;
		if (log.isLoggable(FINE)) log.log(FINE, "unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue); //$NON-NLS-1$ //$NON-NLS-2$

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;

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
			yMinValue = HistoSet.encodeVaultValue(record, yMinValueDisplay);
			yMaxValue = HistoSet.encodeVaultValue(record, yMaxValueDisplay);
			if (log.isLoggable(FINE)) log.log(FINE, String.format("rounded yMinValue = %5.3f - yMaxValue = %5.3f", yMinValue, yMaxValue)); //$NON-NLS-1$
			if (log.isLoggable(FINE)) log.log(FINE, "rounded -> yMinValueDisplay = " + yMinValueDisplay + "; yMaxValueDisplay = " + yMaxValueDisplay); //$NON-NLS-1$ //$NON-NLS-2$
		}

		record.setMinMaxScaleValue(yMinValueDisplay, yMaxValueDisplay);
		record.setSyncedMinMaxDisplayValues(yMinValue, yMaxValue);
	}

	@Override
	protected void setRecordSetCommentStandard() {
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		if (settings.isSmartStatistics()) {
			if (volatileComment == null || !volatileComment.isAvailable()) {
				this.recordSetComment.setText(windowActor.getHistoSet().getDirectoryScanStatistics());
				this.recordSetComment.setToolTipText(Messages.getString(MessageIds.GDE_MSGT0896));
			} else {
				this.recordSetComment.setText(volatileComment.getTextLines());
				this.recordSetComment.setToolTipText(volatileComment.getToolTip());
				if (!volatileComment.isAvailable()) volatileComment = null; // heap gc
			}
		} else {
			this.recordSetComment.setText("supported for smart statistics only");
			this.recordSetComment.setToolTipText("supported for smart statistics only");
		}
	}

	@Override
	public void setMeasuringActive(Measure measure) {
		measuring = new SummaryMeasuring(this, measure);
		if (this.canvasBounds == null) return; // fixed window size
		// draw full graph at first because the curve area might have changed (due to new new scales)
		drawAreaPaintControl();

		if (this.canvasBounds.height == 0) return; // fixed window size
		measuring.drawMeasuring();
	}

}