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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;

import gde.GDE;
import gde.config.Settings;
import gde.histo.datasources.HistoSet;
import gde.histo.datasources.HistoSet.RebuildStep;
import gde.histo.recordings.TrailDataTags;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * History module supplement for the main application class of DataExplorer.
 * @author Thomas Eickert (USER)
 */
public class HistoExplorer {
	static final String								$CLASS_NAME		= HistoExplorer.class.getName();
	static final Logger								log						= Logger.getLogger($CLASS_NAME);

	private final DataExplorer				application		= DataExplorer.getInstance();
	private final Settings						settings			= Settings.getInstance();

	private final CTabFolder					displayTab;
	private final HistoSet						histoSet;

	private List<AbstractChartWindow>	chartTabItems	= new ArrayList<>();
	private List<HistoTableWindow>		tableTabItems	= new ArrayList<>();

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
		chartTabItems.add(HistoGraphicsWindow.create(displayTab, SWT.NONE, positionG));

		int positionS = tabLength < TAB_INDEX_HISTO_SUMMARY ? tabLength : TAB_INDEX_HISTO_SUMMARY;
		chartTabItems.add(HistoSummaryWindow.create(displayTab, SWT.NONE, positionS));

		int positionT = tabLength < TAB_INDEX_HISTO_TABLE ? tabLength : TAB_INDEX_HISTO_TABLE;
		tableTabItems.add(HistoTableWindow.create(displayTab, SWT.NONE, positionT));

		updateHistoTabs(RebuildStep.A_HISTOSET, true);
	}

	public void disposeHisto() {
		for (AbstractChartWindow chartWindow : chartTabItems) {
			chartWindow.dispose();
		}
		chartTabItems.clear();
		for (HistoTableWindow tableWindow : tableTabItems) {
			tableWindow.dispose();
		}
		tableTabItems.clear();
	}

	/**
	 * Rebuilds the contents of the histo windows.
	 * Does nothing if the histoActive setting is false.
	 */
	public synchronized void resetHisto() {
		for (AbstractChartWindow c : chartTabItems) {
			resetWindowHeaderAndMeasuring(c);
		}
		updateHistoTabs(RebuildStep.A_HISTOSET, true);
	}

	/**
	 * @return true if the trail recordset data are available
	 */
	public boolean hasRecords() {
		TrailRecordSet activeRecordSet = getHistoSet().getTrailRecordSet();
		return activeRecordSet != null ? activeRecordSet.size() > 0 : false;
	}

	/**
	 * Updates (redraws) the histo table if visible.
	 */
	public synchronized void updateHistoTableWindow(boolean forceClean) {
		if (displayTab != null && !(displayTab.getSelection() instanceof HistoTableWindow)) return;

		GDE.display.asyncExec(new Runnable() {
			@Override
			public void run() {
				for (HistoTableWindow c : tableTabItems) {
					if (forceClean || !c.isRowTextAndTrailValid() || !c.isHeaderTextValid()) {
						c.setHeader();
					}
					TrailRecordSet trailRecordSet = histoSet.getTrailRecordSet();
					if (trailRecordSet != null) {
						int tableRowCount = trailRecordSet.getVisibleAndDisplayableRecordsForTable().size();
						if (settings.isDisplayTags()) {
							TrailDataTags dataTags = trailRecordSet.getDataTags();
							dataTags.defineActiveDisplayTags();
							if (dataTags.getActiveDisplayTags() != null) {
								tableRowCount += dataTags.getActiveDisplayTags().size();
							}
						}
						c.setRowCount(tableRowCount);
					}
				}
			}
		});

	}

	/**
	 * Set the next graph into the window or alternatively restore to full vertical size.
	 * Does not support redrawing.
	 */
	public void scrollSummaryComposite() {
		if (isHistoChartWindowVisible()) {
			((AbstractChartWindow) displayTab.getSelection()).scrollComposite();
		}
	}

	/**
	 * update (redraw) any visible histo chart window from the histo recordset.
	 * @param redrawCurveSelector
	 */
	public void updateHistoChartWindow(boolean redrawCurveSelector) {
		if (!(displayTab.getSelection() instanceof AbstractChartWindow)) return;

		if (Thread.currentThread().getId() == application.getThreadId()) {
			AbstractChartWindow chartWindow = (AbstractChartWindow) displayTab.getSelection();
			if (!chartWindow.isActiveCurveSelectorContextMenu()) {
				chartWindow.redrawGraphics(redrawCurveSelector);
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					AbstractChartWindow chartWindow = (AbstractChartWindow) displayTab.getSelection();
					if (!chartWindow.isActiveCurveSelectorContextMenu()) {
						chartWindow.redrawGraphics(redrawCurveSelector);
					}
				}
			});
		}
	}

	public boolean isHistoChartWindowVisible() {
		return displayTab.getSelection() instanceof AbstractChartWindow;
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
			log.log(Level.FINER, "initial size=", getTrailRecordSet() != null
					? getTrailRecordSet().getDisplayRecords().size() + "  " + getTrailRecordSet().getVisibleAndDisplayableRecords().size() : "0   0");
			if (isHistoChartWindowVisible() || isHistoTableWindowVisible()) {
				Thread rebuilThread = new Thread((Runnable) () -> rebuildHisto(rebuildStep, isWithUi), "rebuild4Screening"); //$NON-NLS-1$
				try {
					rebuilThread.start();
				} catch (RuntimeException e) {
					log.log(Level.WARNING, e.getMessage(), e);
				}
			}
			log.log(Level.FINER, "rebuild size=", getTrailRecordSet() != null
					? getTrailRecordSet().getDisplayRecords().size() + "  " + getTrailRecordSet().getVisibleAndDisplayableRecords().size() : "0   0");
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (isHistoChartWindowVisible() || isHistoTableWindowVisible()) {
						Thread rebuilThread = new Thread((Runnable) () -> rebuildHisto(rebuildStep, isWithUi), "rebuild4Screening"); //$NON-NLS-1$
						try {
							rebuilThread.start();
						} catch (RuntimeException e) {
							log.log(Level.WARNING, e.getMessage(), e);
						}
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
				if (histoSet.getTrailRecordSet() != null) {
					histoSet.getTrailRecordSet().setDisplayable();
					histoSet.getTrailRecordSet().updateVisibleAndDisplayableRecordsForTable();
				}
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
	 * Reset the window measurement pointer including table and header.
	 */
	private void resetWindowHeaderAndMeasuring(AbstractChartWindow tabItem) {
		if (Thread.currentThread().getId() == application.getThreadId()) {
			tabItem.clearHeaderAndComment();
			tabItem.cleanMeasuring();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					tabItem.clearHeaderAndComment();
					tabItem.cleanMeasuring();
				}
			});
		}
	}

	/**
	 * Interactive curve measuring display.
	 */
	public void enableCurveSurvey(boolean enabled) {
		settings.setCurveSurvey(enabled);
	}

	public void updateVisibleTab(SelectionEvent evt) {
		if (isHistoChartWindowVisible()) {
			log.log(Level.FINER, "HistoChartWindow in displayTab.widgetSelected, event=", evt); //$NON-NLS-1$
			updateHistoTabs(histoSet.getRebuildStepInvisibleTab(), true); // saves some time compared to HistoSet.RebuildStep.E_USER_INTERFACE
		} else if (isHistoTableWindowVisible()) {
			log.log(Level.FINER, "HistoTableWindow in displayTab.widgetSelected, event=", evt); //$NON-NLS-1$
			updateHistoTabs(HistoSet.RebuildStep.E_USER_INTERFACE, true); // ensures rebuild after trails change or record selector change
		}
	}

	/**
	 * Redraw if visible window.
	 */
	public void updateGraphicsWindow(boolean refreshCurveSelector) {
		if ((displayTab.getSelection() instanceof AbstractChartWindow)) {
			((AbstractChartWindow) displayTab.getSelection()).redrawGraphics(refreshCurveSelector);
		}
	}

	/**
	 * Set the inner area background color.
	 */
	public void setInnerAreaBackground(Color innerAreaBackground) {
		settings.setObjectDescriptionInnerAreaBackground(innerAreaBackground);
		for (AbstractChartWindow c : chartTabItems) {
			c.setCurveAreaBackground(innerAreaBackground);
		}
	}

	/**
	 * Set the border color (for curve graphics the curve area border color, ...).
	 */
	public void setBorderColor(Color borderColor) {
		settings.setCurveGraphicsBorderColor(borderColor);
		for (AbstractChartWindow c : chartTabItems) {
			c.setCurveAreaBorderColor(borderColor);
		}
	}

	/**
	 * Set the surrounding area background color (for curve graphics, the area surrounding the curve area, ...).
	 */
	public void setSurroundingBackground(Color surroundingBackground) {
		settings.setUtilitySurroundingBackground(surroundingBackground);
		for (AbstractChartWindow c : chartTabItems) {
			c.setSurroundingBackground(surroundingBackground);
		}
	}

	public void enableGraphicsHeader(boolean enabled) {
		for (AbstractChartWindow c : chartTabItems) {
			c.enableGraphicsHeader(enabled);
		}
	}

	public void enableRecordSetComment(boolean enabled) {
		for (AbstractChartWindow c : chartTabItems) {
			c.enableRecordSetComment(enabled);
		}
	}

	public void enableCurveSelector(boolean enabled) {
		for (AbstractChartWindow c : chartTabItems) {
			c.enableCurveSelector(enabled);
		}
	}

	/**
	 * @return the canvasImage alias graphics window for the visible tab.
	 */
	public Optional<Image> getGraphicsPrintImage() {
		return isHistoChartWindowVisible() ? Optional.of(((AbstractChartWindow) displayTab.getSelection()).getGraphicsPrintImage()) : Optional.empty();
	}

	/**
	 * Get tabulator content as image.
	 */
	public Optional<Image> getContentAsImage() {
		Image graphicsImage = null;
		if (isHistoChartWindowVisible()) {
			graphicsImage = ((AbstractChartWindow) displayTab.getSelection()).getContentAsImage();
		} else if (isHistoTableWindowVisible()) {
			graphicsImage = ((HistoTableWindow) displayTab.getSelection()).getContentAsImage();
		}
		return Optional.ofNullable(graphicsImage);
	}

	/**
	 * return the first histo graphics window content as image
	 */
	public Image getHistoGraphicsContentAsImage() {
		return chartTabItems.stream().filter(c -> c instanceof HistoGraphicsWindow).findFirst().map(c -> c.getContentAsImage()).orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * return the first histo summary window content as image
	 */
	public Image getHistoSummaryContentAsImage() {
		return chartTabItems.stream().filter(c -> c instanceof HistoSummaryWindow).findFirst().map(c -> c.getContentAsImage()).orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * return the histo table window content as image
	 */
	public Image getHistoTableContentAsImage() {
		return tableTabItems.stream().findFirst().map(c -> c.getContentAsImage()).orElseThrow(UnsupportedOperationException::new);
	}

	public HistoSet getHistoSet() {
		return histoSet;
	}

	public TrailRecordSet getTrailRecordSet() {
		return histoSet.getTrailRecordSet();
	}

	public HistoSummaryWindow getHistoSummaryTabItem() {
		return (HistoSummaryWindow) chartTabItems.stream().filter(c -> c instanceof HistoSummaryWindow).findFirst().orElseThrow(UnsupportedOperationException::new);
	}

	/**
	 * @return a visible histo chart window>/br>
	 * throws Exception if the window is not visible
	 */
	public AbstractChartWindow getActiveHistoChartTabItem() {
		return (AbstractChartWindow) this.displayTab.getSelection();
	}

}