/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.tab;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;

import osde.data.Channels;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.CurveSelectorContextMenu;
import osde.utils.TimeLine;

/**
 * This class defines the main graphics window as a sash form of a curve selection table and a drawing canvas
 * @author Winfried Brügmann
 */
public class GraphicsWindow {
	final static Logger						log											= Logger.getLogger(GraphicsWindow.class.getName());

	public static final int				TYPE_NORMAL							= 0;
	public static final int				TYPE_COMPARE						= 1;
	public static final String		WINDOW_TYPE							= "window_type"; //$NON-NLS-1$

	public final static int				MODE_RESET							= 0;
	public final static int				MODE_ZOOM								= 1;
	public final static int				MODE_MEASURE						= 2;
	public final static int				MODE_MEASURE_DELTA			= 3;
	public final static int				MODE_PAN								= 4;
	public final static int				MODE_CUT_LEFT						= 6;
	public final static int				MODE_CUT_RIGHT					= 7;

	final CTabFolder							tabFolder;
	CTabItem											graphic;
	
	SashForm											graphicSashForm;
	boolean												isCurveSelectorEnabled	= true;
	int[]													sashFormWeights					= new int[] { 100, 1000 };
	
	// Curve Selector Table with popup menu
	SelectorComposite							curveSelectorComposite;
	Menu													popupmenu;
	CurveSelectorContextMenu			contextMenu;

	// drawing canvas
	GraphicsComposite							graphicsComposite;

	final OpenSerialDataExplorer	application;
	final Channels								channels;
	final String									tabName;
	final TimeLine								timeLine								= new TimeLine();
	final int											windowType;
	
	public GraphicsWindow(OpenSerialDataExplorer currentApplication, CTabFolder currentDisplayTab, int currentType, String useTabName) {
		this.application = currentApplication;
		this.tabFolder = currentDisplayTab;
		this.windowType = currentType;
		this.tabName = useTabName;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.graphic = new CTabItem(this.tabFolder, SWT.NONE);
		this.graphic.setText(this.tabName);
		SWTResourceManager.registerResourceUser(this.graphic);
		this.graphic.addListener(SWT.RESIZE, new Listener() {
			public void handleEvent(Event evt) {
				log.log(Level.FINE, "controlRezized " + evt);
				GraphicsWindow.this.setSashFormWeights(GraphicsWindow.this.getCurveSelectorComposite().getSelectorColumnWidth());
			}
		});

		{ // graphicSashForm
			this.graphicSashForm = new SashForm(this.tabFolder, SWT.HORIZONTAL);
			this.graphic.setControl(this.graphicSashForm);
			
			{ // curveSelector
				this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
				this.contextMenu = new CurveSelectorContextMenu();
				this.contextMenu.createMenu(this.popupmenu);
				this.curveSelectorComposite = new SelectorComposite(this.graphicSashForm, this.windowType, "  " + Messages.getString(MessageIds.OSDE_MSGT0254), this.popupmenu);
				
			} // curveSelector
			
			{ // graphics composite
				this.graphicsComposite = new GraphicsComposite(this.graphicSashForm, this.windowType);
			} // graphics composite
			
		} // graphicSashForm
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
			this.curveSelectorComposite.doUpdateCurveSelectorTable();
		}
		else {
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					GraphicsWindow.this.graphicsComposite.doRedrawGraphics();
					GraphicsWindow.this.curveSelectorComposite.doUpdateCurveSelectorTable();
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
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
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
		int tabFolderClientAreaWidth = this.tabFolder.getClientArea().width;
		// begin workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ???? 
		int bestGuessWidth = this.application.getClientArea().width;
		if (tabFolderClientAreaWidth > bestGuessWidth) {
			log.log(Level.WARNING, "tabFolder clientAreaWidth missmatch, tabFolderWidth = " + tabFolderClientAreaWidth + " vs applicationWidth = " + bestGuessWidth);
			tabFolderClientAreaWidth = bestGuessWidth;
			this.tabFolder.setSize(tabFolderClientAreaWidth, this.tabFolder.getSize().y);
			this.tabFolder.layout(true);
		}
		// end workaround: sometimes tabFolder.getClientArea().width returned values greater than screen size ???? 
		int[] newWeights = new int[] { newSelectorCompositeWidth, tabFolderClientAreaWidth - newSelectorCompositeWidth};
		if (this.sashFormWeights[0] != newWeights[0] || this.sashFormWeights[1] != newWeights[1]) {
			this.sashFormWeights = newWeights;
			this.graphicSashForm.setWeights(this.sashFormWeights);
			log.log(Level.FINE, "sash weight = " + this.sashFormWeights[0] + ", " + this.sashFormWeights[1] + " windowType = " + this.windowType);
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
	
	public void updateHeaderText(String newHeaderText) {
		this.graphicsComposite.updateHeaderText(newHeaderText);
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
}
