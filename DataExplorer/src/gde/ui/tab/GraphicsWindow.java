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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.ui.tab;

import gde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

import gde.config.Settings;
import gde.data.Channels;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.TimeLine;

/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class GraphicsWindow extends CTabItem {
	final static Logger						log											= Logger.getLogger(GraphicsWindow.class.getName());

	public static final int				TYPE_NORMAL							= 0;
	public static final int				TYPE_COMPARE						= 1;
	public static final int				TYPE_UTIL								= 2;
	public static final String		WINDOW_TYPE							= "window_type"; //$NON-NLS-1$

	final CTabFolder							tabFolder;
	
	SashForm											graphicSashForm;
	boolean												isCurveSelectorEnabled	= true;
	int[]													sashFormWeights					= new int[] { 100, 1000 };
	
	// Curve Selector Table with popup menu
	SelectorComposite							curveSelectorComposite;

	// drawing canvas
	GraphicsComposite							graphicsComposite;

	final DataExplorer	application;
	final Channels								channels;
	final Settings								settings;
	final String									tabName;
	final TimeLine								timeLine								= new TimeLine();
	final int											windowType;
	
	public GraphicsWindow(CTabFolder currentDisplayTab, int style, int currentType, String useTabName, int index) {
		super(currentDisplayTab, style, index);
		SWTResourceManager.registerResourceUser(this);
		this.application = DataExplorer.getInstance();
		this.tabFolder = currentDisplayTab;
		this.windowType = currentType;
		this.tabName = useTabName;
		this.channels = Channels.getInstance();
		this.settings = Settings.getInstance();
		this.setFont(SWTResourceManager.getFont(this.application, 10, SWT.NORMAL));
		this.setText(this.tabName);
	}

	public void create() {
		// graphicSashForm
		this.graphicSashForm = new SashForm(this.tabFolder, SWT.HORIZONTAL);
		this.setControl(this.graphicSashForm);

		{ // curveSelector
			this.curveSelectorComposite = new SelectorComposite(this.graphicSashForm, this.windowType, "  " + Messages.getString(MessageIds.GDE_MSGT0254));
		} // curveSelector

		{ // graphics composite
			this.graphicsComposite = new GraphicsComposite(this.graphicSashForm, this.windowType);
		} // graphics composite

		// graphicSashForm
	}

	/**
	 * query if this component is visible 
	 * @return true if graphics window is visible
	 */
	public boolean isVisible() {
		return this.graphicsComposite.isVisible();
	}
	
	/**
	 * redraws the graphics canvas as well as the curve selector table
	 */
	public void redrawGraphics() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.graphicsComposite.doRedrawGraphics();
			this.graphicsComposite.updateCaptions();
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					GraphicsWindow.this.graphicsComposite.doRedrawGraphics();
					GraphicsWindow.this.graphicsComposite.updateCaptions();
					GraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}
	
	/**
	 * update graphics window header and description
   */ 
	public void updateCaptions() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.graphicsComposite.updateCaptions();
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					GraphicsWindow.this.graphicsComposite.updateCaptions();
				}
			});
		}
	}

	/**
	 * method to update the curves displayed in the curve selector panel 
	 */
	public void updateCurveSelectorTable() {
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			DataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					GraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
				}
			});
		}
	}

	/**
	 * set the sashForm weights
	 * @param newSelectorCompositeWidth the changed curve selector width
	 */
	public void setSashFormWeights(int newSelectorCompositeWidth) {
		int tabFolderClientAreaWidth = this.tabFolder.getBounds().width;
		// begin workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ???? 
		int bestGuessWidth = this.application.getClientArea().width;
		if (tabFolderClientAreaWidth > bestGuessWidth) {
			log.log(Level.WARNING, "tabFolder clientAreaWidth missmatch, tabFolderWidth = " + tabFolderClientAreaWidth + " vs applicationWidth = " + bestGuessWidth);
			tabFolderClientAreaWidth = bestGuessWidth;
			this.tabFolder.setSize(tabFolderClientAreaWidth, this.tabFolder.getBounds().height);
		}
		// end workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ???? 
		int[] newWeights = new int[] { newSelectorCompositeWidth, tabFolderClientAreaWidth - newSelectorCompositeWidth};
		if (this.sashFormWeights[0] != newWeights[0] || this.sashFormWeights[1] != newWeights[1]) {
			this.sashFormWeights = newWeights;
			this.graphicSashForm.setWeights(this.sashFormWeights);
			log.log(Level.FINE, "sash weight = " + this.sashFormWeights[0] + ", " + this.sashFormWeights[1] + " tabFolderClientAreaWidth = " + tabFolderClientAreaWidth + " windowType = " + this.windowType);
		}
	}
	
	/**
	 * query the width
	 */
	public int getWidth() {
		return this.graphicSashForm.getSize().x;
	}

	/**
	 * @return the selectorColumnWidth
	 */
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
	 * enable curve selector which relect to the sash form weights using the selector colum width
	 * @param enabled
	 */
	public void setCurveSelectorEnabled(boolean enabled) {
		this.isCurveSelectorEnabled = enabled;
		if (enabled)
			setSashFormWeights(this.curveSelectorComposite.selectorColumnWidth);
		else 
			setSashFormWeights(0);
	}

	public SashForm getGraphicSashForm() {
		return this.graphicSashForm;
	}
	
	/**
	 * enable display of graphics header
	 */
	public void enableGraphicsHeader(boolean enabled) {
		this.graphicsComposite.enableGraphicsHeader(enabled);
	}

	/**
	 * enable display of record set comment
	 */
	public void enableRecordSetComment(boolean enabled) {
		this.graphicsComposite.enableRecordSetComment(enabled);
	}
	
	public void clearHeaderAndComment() {
		this.graphicsComposite.clearHeaderAndComment();
	}
	
	/**
	 * switch graphics window mouse mode
	 * @param mode MODE_RESET, MODE_ZOOM, MODE_MEASURE, MODE_DELTA_MEASURE
	 */
	public void setModeState(int mode) {
		this.graphicsComposite.setModeState(mode);
	}

	/**
	 * @return the curveSelectorComposite
	 */
	public SelectorComposite getCurveSelectorComposite() {
		return this.curveSelectorComposite;
	}

	/**
	 * @return the graphicsComposite
	 */
	public GraphicsComposite getGraphicsComposite() {
		return this.graphicsComposite;
	}

	/**
	 * query the context menu activation state
	 * @return state true/false
	 */
	public boolean isActiveCurveSelectorContextMenu() {
		return this.curveSelectorComposite.contextMenu.isActive();
	}
	
	/**
	 * create visible tab window content as image
	 * @return image with content
	 */
	public Image getContentAsImage() {
		Rectangle bounds = this.graphicSashForm.getClientArea();
		Image tabContentImage = new Image(DataExplorer.display, bounds.width, bounds.height);
		GC imageGC = new GC(tabContentImage);
		this.graphicSashForm.print(imageGC);
		imageGC.dispose();

		return tabContentImage;
	}
	
	/**
	 * set the curve graphics background color
	 * @param curveAreaBackground the curveAreaBackground to set
	 */
	public void setCurveAreaBackground(Color curveAreaBackground) {
		this.graphicsComposite.curveAreaBackground = curveAreaBackground;
		this.graphicsComposite.graphicCanvas.redraw();
	}
	
	/**
	 * set the curve graphics background color
	 * @param borderColor the curveAreaBackground to set
	 */
	public void setCurveAreaBorderColor(Color borderColor) {
		this.graphicsComposite.curveAreaBorderColor = borderColor;
		this.graphicsComposite.graphicCanvas.redraw();
	}

	/**
	 * set the curve graphics surrounding background color
	 * @param surroundingBackground the surroundingBackground to set
	 */
	public void setSurroundingBackground(Color surroundingBackground) {
		this.graphicsComposite.surroundingBackground = surroundingBackground;
		this.graphicsComposite.setBackground(surroundingBackground);
		this.graphicsComposite.graphicsHeader.setBackground(surroundingBackground);
		this.graphicsComposite.recordSetComment.setBackground(surroundingBackground);
		this.graphicsComposite.doRedrawGraphics();
	}

	/**
	 * @return the windowType (TYPE_NORMAL or TYPE_COMPARE)
	 */
	public int getWindowType() {
		return windowType;
	}
}
