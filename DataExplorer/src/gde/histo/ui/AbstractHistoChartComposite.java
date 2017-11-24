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

import static java.util.logging.Level.FINE;

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

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordSet;
import gde.log.Logger;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.ui.menu.TabAreaContextMenu;
import gde.utils.GraphicsUtils;
import gde.utils.StringHelper;

/**
 * Histo chart drawing area base class.
 * @author Thomas Eickert (USER)
 */
public abstract class AbstractHistoChartComposite extends Composite {
	private final static String		$CLASS_NAME			= HistoSummaryComposite.class.getName();
	private final static Logger	log							= Logger.getLogger($CLASS_NAME);

	protected final DataExplorer	application			= DataExplorer.getInstance();
	protected final Settings			settings				= Settings.getInstance();
	protected final Channels			channels				= Channels.getInstance();

	protected Menu								popupmenu;
	protected TabAreaContextMenu	contextMenu;

	protected Color								curveAreaBackground;
	protected Color								surroundingBackground;
	protected Color								curveAreaBorderColor;

	protected Text								graphicsHeader;
	protected Text								recordSetComment;
	protected Canvas							graphicCanvas;
	int														headerHeight		= 0;
	int														headerGap				= 0;
	int														commentHeight		= 0;
	int														commentGap			= 0;
	protected String							graphicsHeaderText;

	protected Rectangle						canvasBounds;
	protected Image								canvasImage;
	protected GC									canvasImageGC;
	protected GC									canvasGC;
	protected Rectangle						curveAreaBounds	= new Rectangle(0, 0, 1, 1);

	public AbstractHistoChartComposite(Composite parent, int style) {
		super(parent, style);
		// TODO Auto-generated constructor stub
	}

	protected TrailRecordSet getTrailRecordSet() {
		TrailRecordSet trailRecordSet = null;
		if (this.channels.getActiveChannel() != null) {
			trailRecordSet = this.application.getHistoSet().getTrailRecordSet();
		}
		return trailRecordSet;
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
			this.headerGap = 5;
			GC gc = new GC(this.graphicsHeader);
			int stringHeight = gc.stringExtent(this.graphicsHeader.getText()).y;
			this.headerGap = 5;
			this.headerHeight = stringHeight;
			gc.dispose();
		} else {
			this.headerGap = DEFAULT_HEADER_GAP;
			this.headerHeight = 0;
		}
		setComponentBounds();
	}

	public void enableRecordSetComment(boolean enabled) {
		if (enabled) {
			this.commentGap = 0;
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

		y = this.headerGap + this.headerHeight;
		height = graphicsBounds.height - (this.headerGap + this.commentGap + this.commentHeight + this.headerHeight);
		this.graphicCanvas.setBounds(x, y, width, height);
		log.finer(() -> "graphicCanvas.setBounds " + this.graphicCanvas.getBounds()); //$NON-NLS-1$

		y = this.headerGap + this.headerHeight + height + this.commentGap;
		height = this.commentHeight;
		this.recordSetComment.setBounds(20, y, width - 40, height - 5);
		log.finer(() -> "recordSetComment.setBounds " + this.recordSetComment.getBounds()); //$NON-NLS-1$
	}

	/**
	 * @return the graphic window content as image
	 */
	public Image getGraphicsPrintImage() {
		Image graphicsImage = null;
		int graphicsHeight = 30 + this.canvasBounds.height + 40;
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			TrailRecordSet trailRecordSet = getTrailRecordSet();
			if (trailRecordSet != null && trailRecordSet.getTimeStepSize() > 0) {
				if (this.canvasImage != null) this.canvasImage.dispose();
				this.canvasImage = new Image(GDE.display, this.canvasBounds);
				this.canvasImageGC = new GC(this.canvasImage); // SWTResourceManager.getGC(this.canvasImage);
				this.canvasImageGC.setBackground(this.surroundingBackground);
				this.canvasImageGC.fillRectangle(this.canvasBounds);
				this.canvasImageGC.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
				this.canvasGC = new GC(this.graphicCanvas);
				drawCurves(trailRecordSet, this.canvasBounds, this.canvasImageGC);
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
		}
		return graphicsImage;
	}

	/**
	 * Draw the curves with its scales and define the curve area.
	 * @param trailRecordSet the record set to be drawn
	 * @param bounds the bounds where the curves and scales are drawn
	 * @param gc the graphics context to be used for the graphics operations
	 */
	protected void drawCurves(TrailRecordSet trailRecordSet, Rectangle bounds, GC gc) {
		long startInitTime = new Date().getTime();

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

		// calculate the bounds left for the curves
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width - time scale
		int height; // y coordinate - make modulo 10 ??

		// calculate the horizontal space width to be used for the scales
		Point pt = gc.textExtent("-000,00"); //$NON-NLS-1$
		int horizontalGap = pt.x / 5;
		int horizontalNumberExtend = pt.x;
		int horizontalCaptionExtend = pt.y;
		dataScaleWidth = horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;

		// calculate the horizontal area available for plotting graphs
		int gapSide = 10; // free gap left or right side of the curves
		x0 = spaceLeft + (numberCurvesLeft > 0 ? gapSide / 2 : gapSide);// enable a small gap if no axis is shown
		xMax = bounds.width - spaceRight - (numberCurvesRight > 0 ? gapSide / 2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);

		// calculate the vertical area available for plotting graphs
		yMax = 10; // free gap on top of the curves
		int gapBot = 3 * pt.y + 4; // space used for time scale text and scales with description or legend;
		y0 = bounds.height - yMax - gapBot;
		height = y0 - yMax; // recalculate due to modulo 10 ??
		log.finer(() -> "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				+ width + ", height=" + height); //$NON-NLS-1$
		this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		log.finer(() -> "curve bounds = " + this.curveAreaBounds); //$NON-NLS-1$

		log.fine(() -> "draw init time   =  " + StringHelper.getFormatedDuration("ss.SSS", (new Date().getTime() //$NON-NLS-1$ //$NON-NLS-2$
				- startInitTime)));

		drawCurveArea(trailRecordSet, gc, dataScaleWidth);
	}

	/**
	 * Draw the curves into the curve area.
	 * @param trailRecordSet
	 * @param gc
	 * @param dataScaleWidth is the width of one single scale
	 */
	protected abstract void drawCurveArea(TrailRecordSet trailRecordSet, GC gc, int dataScaleWidth);
}
