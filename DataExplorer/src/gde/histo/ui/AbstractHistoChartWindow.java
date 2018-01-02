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
import static java.util.logging.Level.WARNING;

import java.util.Optional;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.HistoGraphicsMeasurement.HistoGraphicsMode;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo chart window base class.
 * @author Thomas Eickert
 */
public abstract class AbstractHistoChartWindow extends CTabItem {
	private final static String	$CLASS_NAME				= AbstractHistoChartWindow.class.getName();
	private final static Logger	log								= Logger.getLogger($CLASS_NAME);

	protected static final int	SELECTOR_WIDTH		= 117;
	/**
	 * y position of the first table row and graphics chart gap.
	 */
	protected static final int	HEADER_ROW_HEIGHT	= 33;

	private static ImageData flipHorizontal(ImageData inputImageData) {
		int bytesPerPixel = inputImageData.bytesPerLine / inputImageData.width;
		int outBytesPerLine = inputImageData.width * bytesPerPixel;
		byte[] outDataBytes = new byte[inputImageData.data.length];
		int outX = 0, outY = 0, inIndex = 0, outIndex = 0;

		for (int y = 0; y < inputImageData.height; y++) {
			for (int x = 0; x < inputImageData.width; x++) {
				outX = x;
				outY = inputImageData.height - y - 1;
				inIndex = (y * inputImageData.bytesPerLine) + (x * bytesPerPixel);
				outIndex = (outY * outBytesPerLine) + (outX * bytesPerPixel);
				System.arraycopy(inputImageData.data, inIndex, outDataBytes, outIndex, bytesPerPixel);
			}
		}
		return new ImageData(inputImageData.width, inputImageData.height, inputImageData.depth, inputImageData.palette, outBytesPerLine, outDataBytes);
	}

	Optional<HistoGraphicsMeasurement>		graphicsMeasurement			= Optional.empty();

	protected final HistoExplorer					presentHistoExplorer		= DataExplorer.getInstance().getPresentHistoExplorer();

	protected final CTabFolder						tabFolder;

	protected SashForm										graphicSashForm;
	protected HistoSelectorComposite			curveSelectorComposite;
	protected AbstractHistoChartComposite	graphicsComposite;
	protected boolean											isCurveSelectorEnabled	= true;
	private int[]													sashFormWeights					= new int[] { 100, 1000 };

	protected AbstractHistoChartWindow(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = parent;
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	public void redrawGraphics(final boolean redrawCurveSelector) {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			if (redrawCurveSelector) this.curveSelectorComposite.doUpdateCurveSelectorTable();
			setFixedGraphicCanvas(graphicsComposite);

			this.graphicsComposite.doRedrawGraphics();
			this.graphicsComposite.updateCaptions();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (redrawCurveSelector) AbstractHistoChartWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
					setFixedGraphicCanvas(graphicsComposite);

					AbstractHistoChartWindow.this.graphicsComposite.doRedrawGraphics();
					AbstractHistoChartWindow.this.graphicsComposite.updateCaptions();
				}

			});
		}
	}

	/**
	 * Option to set a graphics area with position and size not depending on the header and the comment (e.g. summary graphics)
	 */
	protected abstract void setFixedGraphicCanvas(AbstractHistoChartComposite composite);

	/**
	 * Update graphics window header and description.
	 */
	@Deprecated
	public void updateCaptions() {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			this.graphicsComposite.updateCaptions();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					AbstractHistoChartWindow.this.graphicsComposite.updateCaptions();
				}
			});
		}
	}

	/**
	 * Method to update the curves displayed in the curve selector panel.
	 */
	@Deprecated
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == DataExplorer.getInstance().getThreadId()) {
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					AbstractHistoChartWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * @param newSelectorCompositeWidth the changed curve selector width
	 */
	public void setSashFormWeights(int newSelectorCompositeWidth) {
		log.log(FINER, "newSelectorCompositeWidth= ", newSelectorCompositeWidth); //$NON-NLS-1$
		int tabFolderClientAreaWidth = this.tabFolder.getBounds().width;
		// begin workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ????
		int bestGuessWidth = DataExplorer.getInstance().getClientArea().width;
		if (tabFolderClientAreaWidth > bestGuessWidth) {
			log.log(WARNING, "tabFolder clientAreaWidth missmatch, tabFolderWidth = " + tabFolderClientAreaWidth + " vs applicationWidth = " + bestGuessWidth); //$NON-NLS-1$ //$NON-NLS-2$
			tabFolderClientAreaWidth = bestGuessWidth;
			this.tabFolder.setSize(tabFolderClientAreaWidth, this.tabFolder.getBounds().height);
		}
		// end workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ????
		int selectorCompositeWidth = newSelectorCompositeWidth > tabFolderClientAreaWidth / 2 ? tabFolderClientAreaWidth / 2 : newSelectorCompositeWidth;
		int[] newWeights = new int[] { selectorCompositeWidth, tabFolderClientAreaWidth - selectorCompositeWidth };
		log.log(FINER, "newSelectorCompositeWidth= ", selectorCompositeWidth); //$NON-NLS-1$
		if (this.sashFormWeights[0] != newWeights[0] || this.sashFormWeights[1] != newWeights[1]) {
			this.sashFormWeights = newWeights;
			try {
				this.graphicSashForm.setWeights(this.sashFormWeights);
			} catch (IllegalArgumentException e) {
				log.log(WARNING, "graphicSashForm.setWeights(this.sashFormWeights) failed!", e); //$NON-NLS-1$
			}
		}
		if (log.isLoggable(FINE))
			log.log(FINE, "sash weight = " + this.sashFormWeights[0] + ", " + this.sashFormWeights[1] + " tabFolderClientAreaWidth = " + tabFolderClientAreaWidth); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public int getWidth() {
		return this.graphicSashForm.getSize().x;
	}

	public int[] getSashFormWeights() {
		return this.sashFormWeights;
	}

	public SashForm getGraphicSashForm() {
		return this.graphicSashForm;
	}

	public boolean isVisible() {
		return this.graphicsComposite.isVisible();
	}

	public void enableRecordSetComment(boolean enabled) {
		this.graphicsComposite.enableRecordSetComment(enabled);
	}

	public void clearHeaderAndComment() {
		this.graphicsComposite.clearHeaderAndComment();
	}

	public HistoSelectorComposite getCurveSelectorComposite() {
		return this.curveSelectorComposite;
	}

	public abstract AbstractHistoChartComposite getGraphicsComposite();

	public boolean isActiveCurveSelectorContextMenu() {
		return this.curveSelectorComposite.contextMenu.isActive();
	}

	/**
	 * Create visible tab window content as image.
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.graphicSashForm.getClientArea();
		Image tabContentImage = new Image(GDE.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.graphicSashForm.print(imageGC);
		if (GDE.IS_MAC) {
			Image graphics = this.graphicsComposite.getGraphicsPrintImage();
			imageGC.drawImage(SWTResourceManager.getImage(flipHorizontal(graphics.getImageData())), bounds.width - graphics.getBounds().width, 0);
		}
		imageGC.dispose();

		return tabContentImage;
	}

	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsComposite.enableGraphicsHeader(enabled);
	}

	public void enableGraphicsScale(boolean enabled) {
		this.graphicsComposite.enableGraphicsScale(enabled);
	}

	public void setCurveAreaBackground(Color curveAreaBackground) {
		this.graphicsComposite.curveAreaBackground = curveAreaBackground;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	public void setCurveAreaBorderColor(Color borderColor) {
		this.graphicsComposite.curveAreaBorderColor = borderColor;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	public void setSurroundingBackground(Color surroundingBackground) {
		this.graphicsComposite.surroundingBackground = surroundingBackground;
		this.graphicsComposite.setBackground(surroundingBackground);
		this.graphicsComposite.graphicsHeader.setBackground(surroundingBackground);
		this.graphicsComposite.recordSetComment.setBackground(surroundingBackground);
		this.graphicsComposite.doRedrawGraphics();
	}

	/**
	 * @return true if selector is enabled
	 */
	public boolean isCurveSelectorEnabled() {
		return this.isCurveSelectorEnabled;
	}

	/**
	 * Enable curve selector which relect to the sash form weights using the column widths.
	 */
	public void enableCurveSelector(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		this.graphicsComposite.setCurveSelectorEnabled(enabled);
		if (enabled) {
			setSashFormWeights(this.curveSelectorComposite.getCompositeWidth());
		} else {
			setSashFormWeights(0);
		}
	}

	/**
	 * @return true if there is a measuring record with this name
	 */
	public boolean isMeasureRecord(String recordKeyName) {
		return getMeasureRecord().map(r -> r.getName().equals(recordKeyName)).orElse(false);
	}

	/**
	 * Select the next graph or alternatively restore to full vertical size.
	 */
	public abstract void scrollSummaryComposite();

	/**
	 * Switch application into measurement mode for the visible record set using the selected record.
	 */
	public void setMeasurementActive(String recordKey, boolean enabled, boolean isDeltaMeasuring) {
		setMeasurementActive(recordKey, enabled, HistoGraphicsMode.getMode(isDeltaMeasuring));
	}

	/**
	 * Switch application into measurement mode for the visible record set using the selected record.
	 */
	private void setMeasurementActive(String recordKey, boolean enabled, HistoGraphicsMode graphicsMode) {
		TrailRecordSet trailRecordSet = presentHistoExplorer.getTrailRecordSet();
		if (presentHistoExplorer.isHistoChartWindowVisible() && trailRecordSet.containsKey(recordKey)) {
			cleanMeasurement();
			if (enabled) {
				TrailRecord trailRecord = trailRecordSet.get(recordKey);
				if (trailRecord.isVisible()) {
					getGraphicsComposite().drawMeasurePointer(trailRecord, graphicsMode);
				} else {
					getCurveSelectorComposite().setRecordSelection(trailRecord, true);
					getGraphicsComposite().redrawGraphics();
				}
			}
		}
	}

	/**
	 * Clean everything related to the measurement.
	 */
	public void cleanMeasurement() {
		graphicsMeasurement.ifPresent(m -> m.cleanMeasurement());
		graphicsMeasurement = Optional.empty();
	}

	public Optional<TrailRecord> getMeasureRecord() {
		return graphicsMeasurement.map(m -> m.getTrailRecord());
	}

	public void setModeState(HistoGraphicsMode mode) {
		graphicsMeasurement.ifPresent(m -> m.setModeState(mode));
	}

}
