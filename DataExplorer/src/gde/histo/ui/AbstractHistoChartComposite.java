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

import static java.util.logging.Level.FINER;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channels;
import gde.histo.recordings.TrailRecordSet;
import gde.histo.ui.HistoGraphicsMeasurement.HistoGraphicsMode;
import gde.histo.ui.menu.HistoTabAreaContextMenu;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.GraphicsUtils;
import gde.utils.StringHelper;

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;

/**
 * Histo chart drawing area base class.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractHistoChartComposite extends Composite {
	private final static String				$CLASS_NAME							= HistoSummaryComposite.class.getName();
	private final static Logger				log											= Logger.getLogger($CLASS_NAME);

	protected final static int				DEFAULT_TOP_GAP					= 5;																		// free gap on top of the curves
	protected final static int				DEFAULT_SIDE_GAP				= 10;																		// free gap at the leftmost and rightmost graphics
	protected final static int				DEFAULT_BOTTOM_GAP			= 20;																		// space at the bottom of the plots for the scale
	protected final static int				DEFAULT_HEADER_GAP			= 5;
	protected final static int				DEFAULT_COMMENT_GAP			= 5;

	protected final static int				ZERO_CANVAS_HEIGHT			= 11;																		// minimize if smart statistics is not active

	protected final DataExplorer			application							= DataExplorer.getInstance();
	protected final Settings					settings								= Settings.getInstance();
	protected final Channels					channels								= Channels.getInstance();

	protected TrailRecordSet					trailRecordSet;

	protected Menu										popupmenu;
	protected HistoTabAreaContextMenu	contextMenu;

	protected Color										curveAreaBackground;
	protected Color										surroundingBackground;
	protected Color										curveAreaBorderColor;

	protected Text										graphicsHeader;
	protected Text										recordSetComment;
	protected Text										xScale;
	protected Canvas									graphicCanvas;
	int																headerHeight						= 0;
	int																headerGap								= 0;
	int																commentHeight						= 0;
	int																commentGap							= 0;
	int																xScaleHeight						= 0;
	protected String									graphicsHeaderText;

	protected Rectangle								canvasBounds;
	protected Image										canvasImage;
	protected GC											canvasImageGC;
	protected GC											canvasGC;
	protected Rectangle								curveAreaBounds					= new Rectangle(0, 0, 1, 1);
	protected int											fixedCanvasY						= -1;
	protected int											fixedCanvasHeight				= -1;
	protected boolean									isCurveSelectorEnabled	= true;

	public AbstractHistoChartComposite(Composite parent, int style) {
		super(parent, style);
		trailRecordSet = this.channels.getActiveChannel() != null ? this.application.getHistoSet().getTrailRecordSet() : null;
	}

	protected TrailRecordSet retrieveTrailRecordSet() {
		return this.channels.getActiveChannel() != null ? this.application.getHistoSet().getTrailRecordSet() : null;
	}

	/**
	 * Redraw the graphics canvas as well as the curve selector table.
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			doRedrawGraphics();
		} else {
			GDE.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					doRedrawGraphics();
				}
			});
		}
	}

	/**
	 * Update the graphics canvas, while repeatable redraw calls it optimized to the required area.
	 */
	protected synchronized void doRedrawGraphics() {
		if (!GDE.IS_LINUX) { // old code changed due to Mountain Lion refresh problems
			log.finer(() -> "this.graphicCanvas.redraw(5,5,5,5,true); // image based - let OS handle the update"); //$NON-NLS-1$
			Point size = this.graphicCanvas.getSize();
			this.graphicCanvas.redraw(5, 5, 5, 5, true); // image based - let OS handle the update
			this.graphicCanvas.redraw(size.x - 5, 5, 5, 5, true);
			this.graphicCanvas.redraw(5, size.y - 5, 5, 5, true);
			this.graphicCanvas.redraw(size.x - 5, size.y - 5, 5, 5, true);
		} else {
			log.finer(() -> "this.graphicCanvas.redraw(); // do full update where required"); //$NON-NLS-1$
			this.graphicCanvas.redraw(); // do full update where required
		}
		this.recordSetComment.redraw();
	}

	public void notifySelected() {
		this.recordSetComment.notifyListeners(SWT.FocusOut, new Event());
	}

	protected void setRecordSetCommentStandard() {
		this.recordSetComment.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
		this.recordSetComment.setText(this.application.getHistoSet().getDirectoryScanStatistics());
	}

	/**
	 * Enable display of graphics header.
	 */
	public void enableGraphicsHeader(boolean enabled) {
		if (enabled) {
			this.headerGap = DEFAULT_HEADER_GAP;
			this.headerHeight = AbstractHistoChartWindow.HEADER_ROW_HEIGHT - DEFAULT_TOP_GAP;
		} else {
			this.headerGap = DEFAULT_HEADER_GAP;
			this.headerHeight = 0;
		}
		setComponentBounds();
	}

	public void enableGraphicsScale(boolean enabled) {
		if (enabled) {
			GC gc = new GC(this.xScale);
			Point pt = gc.textExtent("0"); //$NON-NLS-1$
			this.xScaleHeight = 3 * pt.y;
			gc.dispose();
		} else {
			this.xScaleHeight = 0;
		}
		setComponentBounds();
	}

	public void enableRecordSetComment(boolean enabled) {
		if (enabled) {
			this.commentGap = DEFAULT_COMMENT_GAP;
			GC gc = new GC(this.recordSetComment);
			int stringHeight = gc.stringExtent(this.recordSetComment.getText()).y;
			this.commentHeight = stringHeight * 2 + 8;
			gc.dispose();
		} else {
			this.commentGap = 0;
			this.commentHeight = 0;
		}
		setComponentBounds();
	}

	public void clearHeaderAndComment() {
		if (this.channels.getActiveChannel() != null) {
			this.recordSetComment.setText(GDE.STRING_EMPTY);
			this.graphicsHeader.setText(GDE.STRING_EMPTY);
			this.graphicsHeaderText = null;
			updateCaptions();
		}
	}

	public synchronized void updateCaptions() {
		this.recordSetComment.redraw();
		this.graphicsHeader.redraw();
	}

	/**
	 * Resize the three areas: header, curve, comment.
	 */
	protected void setComponentBounds() {
		Rectangle graphicsBounds = this.getClientArea();
		// this.application.setGraphicsSashFormWeights(this.graphicSashForm.getSize().x - graphicsBounds.width);
		int x = 0;
		int y = this.headerGap;
		int width = graphicsBounds.width;
		int height = this.headerHeight;
		this.graphicsHeader.setBounds(x, y, width, height);
		log.finer(() -> "recordSetHeader.setBounds " + this.graphicsHeader.getBounds()); //$NON-NLS-1$

		y = this.fixedCanvasY < 0 ? this.headerGap + this.headerHeight : this.fixedCanvasY;
		height = this.fixedCanvasHeight < 0 ? graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight)
				: this.fixedCanvasHeight + this.headerGap + DEFAULT_BOTTOM_GAP;
		this.graphicCanvas.setBounds(x, y, width, height);
		log.finer(() -> "graphicCanvas.setBounds " + this.graphicCanvas.getBounds()); //$NON-NLS-1$

		y += height + this.commentGap;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width - 40, height - 5);
		log.finer(() -> "recordSetComment.setBounds " + this.recordSetComment.getBounds()); //$NON-NLS-1$
	}

	/**
	 * @param fixedY is the constant left upper position or -1 in case of a flexible value
	 * @param fixedHeight is the constant height or -1 in case of a flexible height
	 */
	public void setFixedGraphicCanvas(int fixedY, int fixedHeight) {
		this.fixedCanvasY = fixedY;
		this.fixedCanvasHeight = fixedHeight;
		log.fine(() -> "y = " + fixedY + "  height = " + fixedHeight); //$NON-NLS-1$
	}

	/**
	 * @return the graphic window content as image
	 */
	public Image getGraphicsPrintImage() {
		Image graphicsImage = null;
		int graphicsHeight = 30 + this.canvasBounds.height + 40;
		trailRecordSet = retrieveTrailRecordSet();
		if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
			if (this.canvasImage != null) this.canvasImage.dispose();
			this.canvasImage = new Image(GDE.display, this.canvasBounds);
			this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
			this.canvasImageGC.setBackground(this.surroundingBackground);
			this.canvasImageGC.fillRectangle(this.canvasBounds);
			this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.canvasGC = new GC(this.graphicCanvas);
			drawCurves();
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
		return graphicsImage;
	}

	/**
	 * Draw the record graphs with their scales and define the curve area.
	 */
	protected void drawCurves() {
		long startInitTime = new Date().getTime();
		int dataScaleWidth = defineDataScaleWidth();
		setCurveAreaBounds(dataScaleWidth, defineNumberLeftRightScales());
		log.fine(() -> "draw init time   =  " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() - startInitTime)));

		if (trailRecordSet.getTimeStepSize() > 0) {
			trailRecordSet.setDrawAreaBounds(this.curveAreaBounds);
			drawCurveArea(dataScaleWidth);
		}
	}

	/**
	 * @return the space width to be used for any of the scales
	 */
	private int defineDataScaleWidth() {
		// calculate the space width to be used for the scales
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		{
			Point pt = canvasImageGC.textExtent("-000,00"); //$NON-NLS-1$
			int horizontalGap = pt.x / 5;
			int horizontalNumberExtend = pt.x;
			int horizontalCaptionExtend = pt.y;
			dataScaleWidth = horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		}
		return dataScaleWidth;
	}

	/**
	 * @param dataScaleWidth is the space width to be used for any of the scales
	 * @param numberLeftRightScales holds the numbers of scales {left, right}
	 */
	private void setCurveAreaBounds(int dataScaleWidth, int[] numberLeftRightScales) {
		int spaceLeft = numberLeftRightScales[0] * dataScaleWidth;
		int spaceRight = numberLeftRightScales[1] * dataScaleWidth;

		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width - time scale
		int height; // y coordinate - make modulo 10 ??

		// calculate the horizontal area available for plotting graphs
		int gapSide = DEFAULT_SIDE_GAP; // free gap left or right side of the curves
		x0 = spaceLeft + (numberLeftRightScales[0] > 0 ? gapSide / 2 : gapSide);// enable a small gap if no axis is shown
		xMax = canvasBounds.width - spaceRight - (numberLeftRightScales[1] > 0 ? gapSide / 2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = DEFAULT_TOP_GAP; // free gap on top of the curves
		y0 = canvasBounds.height - yMax - getXScaleHeight();
		height = y0 - yMax; // recalculate due to modulo 10 ??
		log.finer(() -> "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-6$

		curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		log.finer(() -> "curve bounds = " + curveAreaBounds); //$NON-NLS-1$
	}

	/**
	 * @return the height of the scales with description or legend and scale text
	 */
	public int getXScaleHeight() {
		return this.xScaleHeight;
	}

	/**
	 * @return numberLeftRightScales holds the numbers of scales {left, right}
	 */
	protected abstract int[] defineNumberLeftRightScales();

	/**
	 * Draw the curves into the curve area.
	 * @param dataScaleWidth is the width of one single scale
	 */
	protected abstract void drawCurveArea(int dataScaleWidth);

	/**
	 * Clean everything related to the measurement.
	 */
	public abstract void cleanMeasurement();

	/**
	 * Draw the pointer for measurement modes.
	 * Select only valid timestamps on the x axis.
	 */
	public abstract void drawMeasurePointer(TrailRecordSet trailRecordSet, HistoGraphicsMode mode);

	/**
	 * @param enabled
	 */
	public void setCurveSelectorEnabled(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
	}

	/**
	 * Check input x,y value against curve are bounds and correct to bound if required.
	 * @param Point containing corrected x,y position value
	 */
	protected Point checkCurveBounds(int xPos, int yPos) {
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

}