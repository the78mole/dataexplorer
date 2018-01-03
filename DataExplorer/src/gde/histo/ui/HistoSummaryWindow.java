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
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
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
public final class HistoSummaryWindow extends AbstractChartWindow {
	private static final String	$CLASS_NAME						= HistoSummaryWindow.class.getName();
	private static final Logger	log										= Logger.getLogger($CLASS_NAME);

	public static final int[]		DEFAULT_CHART_WEIGHTS	= new int[] { 0, 10000 };							// 2nd chart is the default chart

	protected SashForm					compositeSashForm;

	protected HistoSummaryWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
	}

	public static AbstractChartWindow create(CTabFolder dataTab, int style, int position) {
		HistoSummaryWindow window = new HistoSummaryWindow(dataTab, style, position);

		window.graphicSashForm = new SashForm(window.tabFolder, SWT.HORIZONTAL);
		window.setControl(window.graphicSashForm);
		window.curveSelectorComposite = new SelectorComposite(window.graphicSashForm, window);
		window.compositeSashForm = new SashForm(window.graphicSashForm, SWT.VERTICAL);
		window.graphicSashForm.setWeights(new int[] { SELECTOR_WIDTH, GDE.shell.getClientArea().width - SELECTOR_WIDTH });

		new SummaryComposite(window.compositeSashForm, window); // at the top
		new GraphicsComposite(window.compositeSashForm, window);
		window.compositeSashForm.setWeights(DEFAULT_CHART_WEIGHTS.clone());

		window.setFont(SWTResourceManager.getFont(DataExplorer.getInstance(), GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		window.setText(Messages.getString(MessageIds.GDE_MSGT0883));
		return window;
	}

	@Override
	public AbstractChartComposite getGraphicsComposite() {
		return getFirstGraphics().orElseThrow(UnsupportedOperationException::new);
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
			GDE.display.asyncExec(() -> {
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

			getChartStream().forEach(c -> {
				c.setFixedGraphicCanvas(curveSelectorComposite.getRealBounds());
				c.doRedrawGraphics();
				c.updateCaptions();
			});
		} else {
			GDE.display.asyncExec(() -> {
				if (redrawCurveSelector) curveSelectorComposite.doUpdateCurveSelectorTable();
				if (!Settings.getInstance().isSmartStatistics()) setDefaultChart();

				getChartStream().forEach(c -> {
					c.setFixedGraphicCanvas(curveSelectorComposite.getRealBounds());
					c.doRedrawGraphics();
					c.updateCaptions();
				});
			});
		}
	}

	@Override
	public void enableGraphicsHeader(boolean enabled) {
		getChartStream().forEach(c -> {
			c.enableGraphicsHeader(enabled);
		});
	}

	@Override
	public void enableRecordSetComment(boolean enabled) {
		getChartStream().forEach(c -> {
			c.enableRecordSetComment(enabled);
		});
	}

	@Override
	public void clearHeaderAndComment() {
		getChartStream().forEach(c -> {
			c.clearHeaderAndComment();
		});
	}

	@Override
	public void enableGraphicsScale(boolean enabled) {
		getChartStream().forEach(c -> {
			c.enableGraphicsScale(enabled);
		});
	}

	@Override
	public void setCurveAreaBackground(Color curveAreaBackground) {
		getChartStream().forEach(c -> {
			c.curveAreaBackground = curveAreaBackground;
			c.graphicCanvas.redraw();
		});
	}

	@Override
	public void setCurveAreaBorderColor(Color borderColor) {
		getChartStream().forEach(c -> {
			c.curveAreaBorderColor = borderColor;
			c.graphicCanvas.redraw();
		});
	}

	@Override
	public void setSurroundingBackground(Color surroundingBackground) {
		getChartStream().forEach(c -> {
			c.surroundingBackground = surroundingBackground;
			c.setBackground(surroundingBackground);
			c.graphicsHeader.setBackground(surroundingBackground);
			c.recordSetComment.setBackground(surroundingBackground);
			c.doRedrawGraphics();
		});
	}

	/**
	 * Enable curve selector which relect to the sash form weights using the column widths.
	 */
	@Override
	public void enableCurveSelector(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		if (enabled) {
			setSashFormWeights(this.curveSelectorComposite.getCompositeWidth());
		} else {
			setSashFormWeights(0);
		}
	}

	public Optional<SummaryComposite> getFirstSummary() {
		return Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof SummaryComposite).map(c -> (SummaryComposite) c).findFirst();
	}

	public Optional<GraphicsComposite> getFirstGraphics() {
		return Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof GraphicsComposite).map(c -> (GraphicsComposite) c).findFirst();
	}

	public AbstractChartComposite[] getCharts() {
		return (AbstractChartComposite[]) Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof AbstractChartComposite).map(c -> (AbstractChartComposite) c).toArray();
	}

	public Stream<AbstractChartComposite> getChartStream() {
		return Arrays.stream(this.compositeSashForm.getChildren()).filter(c -> c instanceof AbstractChartComposite).map(c -> (AbstractChartComposite) c);
	}

	/**
	 * Update graphics window header and description.
	 */
	@Override
	@Deprecated
	public void updateCaptions() {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			getChartStream().forEach(c -> {
				c.updateCaptions();
			});
		} else {
			GDE.display.asyncExec(() -> {
				getChartStream().forEach(c -> {
					c.updateCaptions();
				});
			});
		}
	}

	/**
	 * Create visible tab window content as image.
	 * @return image with content
	 */
	@Override
	public Image getContentAsImage() {
		Rectangle bounds = this.graphicSashForm.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.graphicSashForm.print(imageGC);
		if (GDE.IS_MAC) {
			Image graphics = this.getGraphicsComposite().getGraphicsPrintImage();
			imageGC.drawImage(SWTResourceManager.getImage(flipHorizontal(graphics.getImageData())), bounds.width - graphics.getBounds().width, 0);
		}
		imageGC.dispose();

		return tabContentImage;
	}

}
