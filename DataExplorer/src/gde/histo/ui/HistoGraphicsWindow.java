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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;

import gde.GDE;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo graphics window as a sash form of a curve selection table and a drawing canvas.
 * @author Thomas Eickert
 */
public final class HistoGraphicsWindow extends CTabItem {
	private final static String			$CLASS_NAME							= HistoGraphicsWindow.class.getName();
	private final static Logger			log											= Logger.getLogger($CLASS_NAME);

	private final CTabFolder				tabFolder;

	private SashForm								graphicSashForm;
	private boolean									isCurveSelectorEnabled	= true;
	private int[]										sashFormWeights					= new int[] { 100, 1000 };

	private HistoSelectorComposite	curveSelectorComposite;
	private HistoGraphicsComposite	graphicsComposite;

	private final DataExplorer			application = DataExplorer.getInstance();

	public HistoGraphicsWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
		SWTResourceManager.registerResourceUser(this);
		this.tabFolder = currentDisplayTab;
		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0792));
	}

	public synchronized void create() {
		this.graphicSashForm = new SashForm(this.tabFolder, SWT.HORIZONTAL);
		this.setControl(this.graphicSashForm);
		this.curveSelectorComposite = new HistoSelectorComposite(this.graphicSashForm);
		this.graphicsComposite = new HistoGraphicsComposite(this.graphicSashForm);
		this.graphicSashForm.setWeights(new int[] { 117, GDE.shell.getClientArea().width - 117 });
	}

	/**
	 * Query if this component is visible.
	 * @return true if graphics window is visible
	 */
	public boolean isVisible() {
		return this.graphicsComposite.isVisible();
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	public void redrawGraphics(final boolean redrawCurveSelector) {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.graphicsComposite.doRedrawGraphics();
			this.graphicsComposite.updateCaptions();
			if (redrawCurveSelector) this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					HistoGraphicsWindow.this.graphicsComposite.doRedrawGraphics();
					HistoGraphicsWindow.this.graphicsComposite.updateCaptions();
					if (redrawCurveSelector) HistoGraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * Update graphics window header and description.
	*/
	public void updateCaptions() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.graphicsComposite.updateCaptions();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					HistoGraphicsWindow.this.graphicsComposite.updateCaptions();
				}
			});
		}
	}

	/**
	 * Method to update the curves displayed in the curve selector panel.
	 */
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					HistoGraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * @param newSelectorCompositeWidth the changed curve selector width
	 */
	public void setSashFormWeights(int newSelectorCompositeWidth) {
		log.log(Level.FINER, "newSelectorCompositeWidth= ", newSelectorCompositeWidth); //$NON-NLS-1$
		int tabFolderClientAreaWidth = this.tabFolder.getBounds().width;
		// begin workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ????
		int bestGuessWidth = this.application.getClientArea().width;
		if (tabFolderClientAreaWidth > bestGuessWidth) {
			log.log(Level.WARNING, "tabFolder clientAreaWidth missmatch, tabFolderWidth = " + tabFolderClientAreaWidth + " vs applicationWidth = " + bestGuessWidth); //$NON-NLS-1$ //$NON-NLS-2$
			tabFolderClientAreaWidth = bestGuessWidth;
			this.tabFolder.setSize(tabFolderClientAreaWidth, this.tabFolder.getBounds().height);
		}
		// end workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ????
		newSelectorCompositeWidth = newSelectorCompositeWidth > tabFolderClientAreaWidth / 2 ? tabFolderClientAreaWidth / 2 : newSelectorCompositeWidth;
		int[] newWeights = new int[] { newSelectorCompositeWidth, tabFolderClientAreaWidth - newSelectorCompositeWidth };
		log.log(Level.FINER, "newSelectorCompositeWidth= ", newSelectorCompositeWidth); //$NON-NLS-1$
		if (this.sashFormWeights[0] != newWeights[0] || this.sashFormWeights[1] != newWeights[1]) {
			this.sashFormWeights = newWeights;
			try {
				this.graphicSashForm.setWeights(this.sashFormWeights);
			}
			catch (IllegalArgumentException e) {
				log.log(Level.WARNING, "graphicSashForm.setWeights(this.sashFormWeights) failed!", e); //$NON-NLS-1$
			}
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "sash weight = " + this.sashFormWeights[0] + ", " + this.sashFormWeights[1] + " tabFolderClientAreaWidth = " + tabFolderClientAreaWidth); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	public int getWidth() {
		return this.graphicSashForm.getSize().x;
	}

	public int[] getSashFormWeights() {
		return this.sashFormWeights;
	}

	/**
	 * @return true if selector is enabled
	 */
	public boolean isCurveSelectorEnabled() {
		return this.isCurveSelectorEnabled;
	}

	/**
	 * Enable curve selector which relect to the sash form weights using the column widths.
	 * @param enabled
	 */
	public void setCurveSelectorEnabled(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		if (enabled)
			setSashFormWeights(this.curveSelectorComposite.getCompositeWidth());
		else
			setSashFormWeights(0);
	}

	public SashForm getGraphicSashForm() {
		return this.graphicSashForm;
	}

	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsComposite.enableGraphicsHeader(enabled);
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

	public HistoGraphicsComposite getGraphicsComposite() {
		return this.graphicsComposite;
	}

	/**
	 * Query the context menu activation state.
	 * @return the state true/false
	 */
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
			imageGC.drawImage(SWTResourceManager.getImage(flipHorizontal(this.graphicsComposite.getGraphicsPrintImage().getImageData())), bounds.width - graphics.getBounds().width, 0);
		}
		imageGC.dispose();

		return tabContentImage;
	}

	ImageData flipHorizontal(ImageData inputImageData) {
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

	/**
	 * @param curveAreaBackground the curveAreaBackground to set
	 */
	public void setCurveAreaBackground(Color curveAreaBackground) {
		this.graphicsComposite.curveAreaBackground = curveAreaBackground;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	/**
	 * @param borderColor the curveAreaBackground to set
	 */
	public void setCurveAreaBorderColor(Color borderColor) {
		this.graphicsComposite.curveAreaBorderColor = borderColor;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	/**
	 * @param surroundingBackground the surroundingBackground to set
	 */
	public void setSurroundingBackground(Color surroundingBackground) {
		this.graphicsComposite.surroundingBackground = surroundingBackground;
		this.graphicsComposite.setBackground(surroundingBackground);
		this.graphicsComposite.graphicsHeader.setBackground(surroundingBackground);
		this.graphicsComposite.recordSetComment.setBackground(surroundingBackground);
		this.graphicsComposite.doRedrawGraphics();
	}

}