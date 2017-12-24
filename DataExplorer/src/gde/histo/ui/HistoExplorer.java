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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.ui;

import static gde.ui.DataExplorer.TAB_INDEX_HISTO_GRAPHIC;
import static gde.ui.DataExplorer.TAB_INDEX_HISTO_SUMMARY;
import static gde.ui.DataExplorer.TAB_INDEX_HISTO_TABLE;
import static java.util.logging.Level.SEVERE;

import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.recordings.TrailDataTags;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.HistoGraphicsMeasurement.HistoGraphicsMode;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.menu.MenuBar;

/**
 * History module supplement for the main application class of DataExplorer.
 * @author Thomas Eickert (USER)
 */
public class HistoExplorer {
	static final String					$CLASS_NAME	= HistoExplorer.class.getName();
	static final Logger					log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application	= DataExplorer.getInstance();
	private final Settings			settings		= Settings.getInstance();

	private final CTabFolder		displayTab;
	private final HistoSet			histoSet;

	private HistoGraphicsWindow	histoGraphicsTabItem;
	private HistoSummaryWindow	histoSummaryTabItem;
	private HistoTableWindow		histoTableTabItem;

	private boolean							isCurveSurveyVisible;

	public HistoExplorer(CTabFolder displayTab) {
		this.displayTab = displayTab;

		this.settings.setHistoActive(true);
		this.histoSet = new HistoSet();
	}

	/**
	 * Build and fill the tabs.
	 */
	public void initHisto() {
		int tabLength = displayTab.getItems().length;
		int positionG = tabLength < TAB_INDEX_HISTO_GRAPHIC ? tabLength : TAB_INDEX_HISTO_GRAPHIC;
		histoGraphicsTabItem = HistoGraphicsWindow.create(displayTab, SWT.NONE, positionG);

		int positionS = tabLength < TAB_INDEX_HISTO_SUMMARY ? tabLength : TAB_INDEX_HISTO_SUMMARY;
		histoSummaryTabItem = HistoSummaryWindow.create(displayTab, SWT.NONE, positionS);

		int positionT = tabLength < TAB_INDEX_HISTO_TABLE ? tabLength : TAB_INDEX_HISTO_TABLE;
		histoTableTabItem = HistoTableWindow.create(displayTab, SWT.NONE, positionT);

		updateHistoTabs(RebuildStep.A_HISTOSET, true);
	}

	/**
	 * Rebuilds the contents of the histo windows.
	 * Does nothing if the histoActive setting is false.
	 */
	public synchronized void resetHisto() {
		log.fine(() -> String.format("isHistoActive=%b", settings.isHistoActive())); //$NON-NLS-1$
		if (settings.isHistoActive()) {
			resetGraphicsWindowHeaderAndMeasurement();
			resetSummaryWindowHeaderAndMeasurement();
			updateHistoTabs(RebuildStep.A_HISTOSET, true);
		}
	}

	/**
	 * @return true if the trail recordset data are available
	 */
	public boolean hasRecords() {
		TrailRecordSet activeRecordSet = getHistoSet().getTrailRecordSet();
		return activeRecordSet != null ? activeRecordSet.size() > 0 : false;
	}

	/**
	 * updates (redraws) the histo table if visible.
	 */
	public synchronized void updateHistoTableWindow(final boolean forceClean) {
		if (!(displayTab.getSelection() instanceof HistoTableWindow)) return;

		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				HistoTableWindow tableTabItem = histoTableTabItem;
				if (forceClean || !tableTabItem.isRowTextAndTrailValid() || !tableTabItem.isHeaderTextValid()) {
					tableTabItem.setHeader();
				}
				TrailRecordSet trailRecordSet = histoSet.getTrailRecordSet();
				if (trailRecordSet != null) {
					int tableRowCount = trailRecordSet.getVisibleAndDisplayableRecordsForTable().size();
					if (settings.isDisplayTags()) {
						TrailDataTags dataTags = trailRecordSet.getDataTags();
						dataTags.defineActiveDisplayTags();
						if (dataTags.getActiveDisplayTags() != null) tableRowCount += dataTags.getActiveDisplayTags().size();
					}
					tableTabItem.setRowCount(tableRowCount);
				}
			}
		});
	}

	/**
	 * update (redraw) any visible histo chart window from the histo recordset.
	 * @param redrawCurveSelector
	 */
	public void updateHistoChartWindow(boolean redrawCurveSelector) {
		if (!(displayTab.getSelection() instanceof AbstractHistoChartWindow)) return;

		if (Thread.currentThread().getId() == application.getThreadId()) {
			AbstractHistoChartWindow chartWindow = (AbstractHistoChartWindow) displayTab.getSelection();
			if (!chartWindow.isActiveCurveSelectorContextMenu()) {
				chartWindow.redrawGraphics(redrawCurveSelector);
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					AbstractHistoChartWindow chartWindow = (AbstractHistoChartWindow) displayTab.getSelection();
					if (!chartWindow.isActiveCurveSelectorContextMenu()) {
						chartWindow.redrawGraphics(redrawCurveSelector);
					}
				}
			});
		}
	}

	public boolean isHistoChartWindowVisible() {
		return displayTab.getSelection() instanceof AbstractHistoChartWindow;
	}

	public boolean isHistoTableWindowVisible() {
		return displayTab.getSelection() instanceof HistoTableWindow;
	}

	/**
	 * update any visible histo tab.
	 * @param createRecordSet true creates the recordset from the histo vaults; false uses the existing recordset
	 * @param rebuildTrails true refills the recordset and keeps the selector settings; false only rebuilds the UI
	 */
	public void updateHistoTabs(boolean createRecordSet, boolean rebuildTrails) {
		updateHistoTabs(createRecordSet ? RebuildStep.C_TRAILRECORDSET : rebuildTrails ? RebuildStep.D_TRAIL_DATA : RebuildStep.E_USER_INTERFACE, true);
	}

	private void updateHistoTabs(RebuildStep rebuildStep, boolean isWithUi) {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			if (isHistoChartWindowVisible() || isHistoTableWindowVisible()) {
				Thread rebuilThread = new Thread((Runnable) () -> rebuildHisto(rebuildStep, isWithUi), "rebuild4Screening"); //$NON-NLS-1$
				try {
					rebuilThread.start();
				} catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
		} else {
			GDE.display.asyncExec((Runnable) () -> {
				if (isHistoChartWindowVisible() || isHistoTableWindowVisible()) {
					Thread rebuilThread = new Thread((Runnable) () -> rebuildHisto(rebuildStep, isWithUi), "rebuild4Screening"); //$NON-NLS-1$
					try {
						rebuilThread.start();
					} catch (RuntimeException e) {
						log.log(Level.WARNING, e.getMessage(), e);
					}
				}
			});
		}
	}

	public synchronized void rebuildHisto(RebuildStep rebuildStep, boolean isWithUi) {
		boolean isRebuilt = false;
		try {
			isRebuilt = histoSet.rebuild4Screening(rebuildStep, isWithUi);

			if (isRebuilt || rebuildStep == RebuildStep.E_USER_INTERFACE) {
				if (histoSet.getTrailRecordSet() != null) histoSet.getTrailRecordSet().updateVisibleAndDisplayableRecordsForTable();
				updateHistoChartWindow(true);
				updateHistoTableWindow(rebuildStep.scopeOfWork >= RebuildStep.E_USER_INTERFACE.scopeOfWork);
			}
			if (isWithUi && rebuildStep == RebuildStep.B_HISTOVAULTS && histoSet.getTrailRecordSet().getTimeStepSize() == 0) {
				StringBuilder sb = new StringBuilder();
				for (Path path : histoSet.getValidatedDirectories().values()) {
					sb.append(path.toString()).append(GDE.STRING_NEW_LINE);
				}
				String objectOrDevice = application.getObjectKey().isEmpty() ? application.getActiveDevice().getName() : application.getObjectKey();
				application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGI0066, new Object[] { objectOrDevice, sb.toString() }));
			}
			histoSet.setRebuildStepInvisibleTabs(rebuildStep, isRebuilt);
		} catch (Exception e) {
			log.log(SEVERE, e.getMessage(), e);
			if (isWithUi) application.openMessageDialog(Messages.getString(MessageIds.GDE_MSGE0007) + e.getMessage());
		}
	}

	/**
	 * Set the sashForm weights for all histo windows which use the composite.
	 */
	public void setHistoChartSashFormWeights(HistoSelectorComposite composite) {
		if (histoGraphicsTabItem.getCurveSelectorComposite().equals(composite)) {
			int compositeWidth = histoGraphicsTabItem.isCurveSelectorEnabled() ? composite.getCompositeWidth() : 0;
			histoGraphicsTabItem.setSashFormWeights(compositeWidth);
		}
		if (histoSummaryTabItem.getCurveSelectorComposite().equals(composite)) {
			int compositeWidth = histoSummaryTabItem.isCurveSelectorEnabled() ? composite.getCompositeWidth() : 0;
			histoSummaryTabItem.setSashFormWeights(compositeWidth);
		}
	}

	/**
	 * reset the histo graphics window measurement pointer including table and header
	 */
	private void resetGraphicsWindowHeaderAndMeasurement() {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			histoGraphicsTabItem.clearHeaderAndComment();
			histoGraphicsTabItem.getGraphicsComposite().cleanMeasurement();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					histoGraphicsTabItem.clearHeaderAndComment();
					histoGraphicsTabItem.getGraphicsComposite().cleanMeasurement();
				}
			});
		}
	}

	/**
	 * reset the histo summary window measurement pointer including table and header
	 */
	private void resetSummaryWindowHeaderAndMeasurement() {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			histoSummaryTabItem.clearHeaderAndComment();
			histoSummaryTabItem.getGraphicsComposite().cleanMeasurement();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					histoSummaryTabItem.clearHeaderAndComment();
					histoSummaryTabItem.getGraphicsComposite().cleanMeasurement();
				}
			});
		}
	}

	/**
	 * Interactive curve measuring display.
	 */
	public void enableCurveSurvey(boolean enabled) {
		settings.setCurveSurvey(enabled);
		isCurveSurveyVisible = enabled;
	}

	public void paintControl(PaintEvent evt) {
		if (isHistoChartWindowVisible()) {
			AbstractHistoChartWindow chartWindow = ((AbstractHistoChartWindow) displayTab.getSelection());
			if (chartWindow.isCurveSelectorEnabled())
				chartWindow.setSashFormWeights(chartWindow.getCurveSelectorComposite().getCompositeWidth());
			else
				chartWindow.setSashFormWeights(0);
		}
	}

	public void widgetSelected(SelectionEvent evt) {
		CTabFolder tabFolder = (CTabFolder) evt.widget;
		int tabSelectionIndex = tabFolder.getSelectionIndex();
		if (isHistoChartWindowVisible()) {
			log.log(Level.FINER, "HistoChartWindow in displayTab.widgetSelected, event=", evt); //$NON-NLS-1$
			updateHistoTabs(histoSet.getRebuildStepInvisibleTab(), true); // saves some time compared to HistoSet.RebuildStep.E_USER_INTERFACE
		} else if (displayTab.getItem(tabSelectionIndex) instanceof HistoTableWindow) {
			log.log(Level.FINER, "HistoTableWindow in displayTab.widgetSelected, event=", evt); //$NON-NLS-1$
			updateHistoTabs(HistoSet.RebuildStep.E_USER_INTERFACE, true); // ensures rebuild after trails change or record selector change
		}
	}

	public void restoreWindowsSettings(MenuBar menuBar) {
		isCurveSurveyVisible = settings.isCurveSurvey();
		if (isCurveSurveyVisible) {
			menuBar.setCurveSurveyMenuItemSelection(isCurveSurveyVisible);
			enableCurveSurvey(isCurveSurveyVisible);
		}
	}

	public void updateGraphicsWindow(boolean refreshCurveSelector) {
		if ((displayTab.getSelection() instanceof HistoSummaryWindow)) {
			histoSummaryTabItem.redrawGraphics(refreshCurveSelector);
		} else if ((displayTab.getSelection() instanceof HistoGraphicsWindow)) {
			histoGraphicsTabItem.redrawGraphics(refreshCurveSelector);
		}
	}

	/**
	 * clear measurement pointer of visible tab window.
	 */
	public void clearMeasurementModes() {
		if (isHistoChartWindowVisible()) {
			histoSet.getTrailRecordSet().clearMeasurementModes();
			((AbstractHistoChartWindow) displayTab.getSelection()).getGraphicsComposite().cleanMeasurement();
		}
	}

	/**
	 * switch application into measurement mode for the visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setMeasurementActive(String recordKey, boolean enabled) {
		TrailRecordSet trailRecordSet = histoSet.getTrailRecordSet();
		if (isHistoChartWindowVisible() && trailRecordSet.containsKey(recordKey)) {
			histoGraphicsTabItem.getGraphicsComposite().cleanMeasurement();
			trailRecordSet.setMeasurementMode(recordKey, enabled);
			TrailRecord trailRecord = trailRecordSet.get(recordKey);
			if (enabled && !trailRecord.isVisible()) {
				histoGraphicsTabItem.getCurveSelectorComposite().setRecordSelection(trailRecord, true);
				histoGraphicsTabItem.getGraphicsComposite().redrawGraphics();
			}
			if (enabled) histoGraphicsTabItem.getGraphicsComposite().drawMeasurePointer(trailRecordSet, HistoGraphicsMode.MEASURE);
		}
	}

	/**
	 * switch application into delta measurement mode for visible record set using selected record
	 * @param recordKey
	 * @param enabled
	 */
	public void setDeltaMeasurementActive(String recordKey, boolean enabled) {
		TrailRecordSet trailRecordSet = histoSet.getTrailRecordSet();
		if (isHistoChartWindowVisible() && trailRecordSet.containsKey(recordKey)) {
			histoGraphicsTabItem.getGraphicsComposite().cleanMeasurement();
			trailRecordSet.setDeltaMeasurementMode(recordKey, enabled);
			TrailRecord trailRecord = trailRecordSet.get(recordKey);
			if (enabled && !trailRecord.isVisible()) {
				histoGraphicsTabItem.getCurveSelectorComposite().setRecordSelection(trailRecord, true);
				histoGraphicsTabItem.getGraphicsComposite().redrawGraphics();
			}
			if (enabled) histoGraphicsTabItem.getGraphicsComposite().drawMeasurePointer(trailRecordSet, HistoGraphicsMode.MEASURE_DELTA);
		}
	}

	/**
	 * set the inner area background color
	 * @param innerAreaBackground the innerAreaBackground to set
	 */
	public void setInnerAreaBackground(Color innerAreaBackground) {
			settings.setObjectDescriptionInnerAreaBackground(innerAreaBackground);
			histoGraphicsTabItem.setCurveAreaBackground(innerAreaBackground);
			histoSummaryTabItem.setCurveAreaBackground(innerAreaBackground);
	}

	/**
	 * set the border color (for curve graphics the curve area border color, ...)
	 * @param borderColor the borderColor to set
	 */
	public void setBorderColor(Color borderColor) {
			settings.setCurveGraphicsBorderColor(borderColor);
			histoGraphicsTabItem.setCurveAreaBorderColor(borderColor);
			histoSummaryTabItem.setCurveAreaBorderColor(borderColor);
	}

	/**
	 * set the surrounding area background color (for curve graphics, the area surrounding the curve area, ...)
	 * @param surroundingBackground the surroundingBackground to set
	 */
	public void setSurroundingBackground(Color surroundingBackground) {
			settings.setUtilitySurroundingBackground(surroundingBackground);
			histoGraphicsTabItem.setSurroundingBackground(surroundingBackground);
			histoSummaryTabItem.setSurroundingBackground(surroundingBackground);
	}

	public void enableGraphicsHeader(boolean enabled) {
		histoGraphicsTabItem.enableGraphicsHeader(enabled);
		histoSummaryTabItem.enableGraphicsHeader(enabled);
	}

	public void enableRecordSetComment(boolean enabled) {
		histoGraphicsTabItem.enableRecordSetComment(enabled);
		histoSummaryTabItem.enableRecordSetComment(enabled);
	}

	public void enableCurveSelector(boolean enabled) {
		histoGraphicsTabItem.enableCurveSelector(enabled);
		histoSummaryTabItem.enableCurveSelector(enabled);
	}

	/**
	 * @return the canvasImage alias graphics window for the visible tab.
	 */
	public Optional<Image> getGraphicsPrintImage() {
		return isHistoChartWindowVisible()
				? Optional.of(((AbstractHistoChartWindow) displayTab.getSelection()).getGraphicsComposite().getGraphicsPrintImage()) : Optional.empty();
	}

	/**
	 * get tabulator content as image
	 */
	public Optional<Image> getContentAsImage() {
		Image graphicsImage = null;
		if (displayTab.getSelection() instanceof HistoGraphicsWindow) {
			graphicsImage = histoGraphicsTabItem.getContentAsImage();
		} else if (displayTab.getSelection() instanceof HistoSummaryWindow) {
			graphicsImage = histoSummaryTabItem.getContentAsImage();
		} else if (displayTab.getSelection() instanceof HistoTableWindow) {
			graphicsImage = histoTableTabItem.getContentAsImage();
		}
		return Optional.ofNullable(graphicsImage);
	}

	/**
	 * return the histo graphics window content as image
	 */
	public Image getHistoGraphicsContentAsImage() {
		return histoGraphicsTabItem.getContentAsImage();
	}

	/**
	 * return the histo summary window content as image
	 */
	public Image getHistoSummaryContentAsImage() {
		return histoSummaryTabItem.getContentAsImage();
	}

	/**
	 * return the histo table window content as image
	 */
	public Image getHistoTableContentAsImage() {
		return histoTableTabItem.getContentAsImage();
	}

	public HistoSet getHistoSet() {
		return histoSet;
	}

	public TrailRecordSet getTrailRecordSet() {
		return histoSet.getTrailRecordSet();
	}

}