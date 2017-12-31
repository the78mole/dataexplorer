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

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.config.Settings;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo summary window as a sash form of a curve selection table and a drawing canvas.
 * The drawing canvas holds multiple charts.
 * @author Thomas Eickert
 */
public final class HistoSummaryWindow extends AbstractHistoChartWindow {
	private static final String			$CLASS_NAME						= HistoSummaryWindow.class.getName();
	private static final Logger			log										= Logger.getLogger($CLASS_NAME);

	public static final int[]				DEFAULT_CHART_WEIGHTS	= new int[] {0, 10000};							// 2nd chart is the default chart

	protected SashForm							compositeSashForm;
	protected HistoSummaryComposite	summaryComposite;

	protected HistoSummaryWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
	}

	public static AbstractHistoChartWindow create(CTabFolder dataTab, int style, int position) {
		HistoSummaryWindow window = new HistoSummaryWindow(dataTab, style, position);

		window.graphicSashForm = new SashForm(window.tabFolder, SWT.HORIZONTAL);
		window.setControl(window.graphicSashForm);

		window.curveSelectorComposite = new HistoSelectorComposite(window.graphicSashForm);
		window.compositeSashForm = new SashForm(window.graphicSashForm, SWT.VERTICAL);
		window.summaryComposite = new HistoSummaryComposite(window.compositeSashForm); // at the top
		window.graphicsComposite = new HistoGraphicsComposite(window.compositeSashForm);
		window.graphicSashForm.setWeights(new int[] { SELECTOR_WIDTH, GDE.shell.getClientArea().width - SELECTOR_WIDTH });
		window.compositeSashForm.setWeights(DEFAULT_CHART_WEIGHTS.clone());

		window.setFont(SWTResourceManager.getFont(DataExplorer.getInstance(), GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		window.setText(Messages.getString(MessageIds.GDE_MSGT0883));
		return window;
	}

	@Override
	public AbstractHistoChartComposite getGraphicsComposite() {
		return this.graphicsComposite;
	}

	@Override
	protected void setFixedGraphicCanvas(AbstractHistoChartComposite composite) {
		Rectangle realBounds = this.curveSelectorComposite.getRealBounds();
		if (Settings.getInstance().isSmartStatistics()) {
			int heightWithScale = realBounds.height + composite.getXScaleHeight() + AbstractHistoChartComposite.DEFAULT_TOP_GAP;
			composite.setFixedGraphicCanvas(realBounds.y - AbstractHistoChartComposite.DEFAULT_TOP_GAP, heightWithScale);
		} else {
			composite.setFixedGraphicCanvas(realBounds.y - AbstractHistoChartComposite.DEFAULT_TOP_GAP, AbstractHistoChartComposite.ZERO_CANVAS_HEIGHT);
		}
	}

	protected void resetFixedGraphicCanvas(AbstractHistoChartComposite composite) {
		composite.setFixedGraphicCanvas(-1, -1);
	}

	@Override
	public void scrollSummaryComposite() {
		if (Settings.getInstance().isSmartStatistics()) {
			int sumWeights = Arrays.stream(compositeSashForm.getWeights()).sum();
			if (compositeSashForm.getWeights()[0] / sumWeights < compositeSashForm.getWeights()[1] / sumWeights) {
				compositeSashForm.setWeights(new int[] { 10000, 0 });
			} else {
				compositeSashForm.setWeights(new int[] { 0, 10000 });
			}
		} else {
			setDefaultChart();
		}
	}

	/**
	 * Set the graphics chart into the window without redrawing.
	 */
	protected void setDefaultChart() {
		compositeSashForm.setWeights(DEFAULT_CHART_WEIGHTS.clone());
	}

	public int[] getChartWeights() {
		return compositeSashForm.getWeights();
	}

	public void setChartWeights(int[] chartWeights) {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			compositeSashForm.setWeights(chartWeights);
		} else {
			GDE.display.asyncExec((Runnable) () -> {
				this.compositeSashForm.setWeights(chartWeights);
			});
		}
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	@Override
	public void redrawGraphics(final boolean redrawCurveSelector) {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			if (redrawCurveSelector) curveSelectorComposite.doUpdateCurveSelectorTable();
			if (!Settings.getInstance().isSmartStatistics()) setDefaultChart();

			{
				resetFixedGraphicCanvas(graphicsComposite);
				graphicsComposite.doRedrawGraphics();
				graphicsComposite.updateCaptions();
			}
			{
				setFixedGraphicCanvas(summaryComposite);
				summaryComposite.doRedrawGraphics();
				summaryComposite.updateCaptions();
			}
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (redrawCurveSelector) curveSelectorComposite.doUpdateCurveSelectorTable();
					if (!Settings.getInstance().isSmartStatistics()) setDefaultChart();

					{
						resetFixedGraphicCanvas(graphicsComposite);
						graphicsComposite.doRedrawGraphics();
						graphicsComposite.updateCaptions();
					}
					{
						setFixedGraphicCanvas(summaryComposite);
						summaryComposite.doRedrawGraphics();
						summaryComposite.updateCaptions();
					}
				}

			});
		}
	}

	@Override
	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsComposite.enableGraphicsHeader(enabled);
		this.summaryComposite.enableGraphicsHeader(enabled);
	}

	@Override
	public void enableRecordSetComment(boolean enabled) {
		this.graphicsComposite.enableRecordSetComment(enabled);
		this.summaryComposite.enableRecordSetComment(enabled);
	}

	@Override
	public void clearHeaderAndComment() {
		this.graphicsComposite.clearHeaderAndComment();
		this.summaryComposite.clearHeaderAndComment();
	}

}
