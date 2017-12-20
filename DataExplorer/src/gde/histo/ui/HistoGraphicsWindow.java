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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;

import gde.GDE;
import gde.log.Logger;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;

/**
 * Histo graphics window as a sash form of a curve selection table and a drawing canvas.
 * @author Thomas Eickert
 */
public final class HistoGraphicsWindow extends AbstractHistoChartWindow {
	private final static String	$CLASS_NAME	= HistoGraphicsWindow.class.getName();
	final static Logger					log					= Logger.getLogger($CLASS_NAME);

	private HistoGraphicsWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
	}

	public static HistoGraphicsWindow create(CTabFolder dataTab, int style, int position) {
		HistoGraphicsWindow window = new HistoGraphicsWindow(dataTab, style, position);

		window.graphicSashForm = new SashForm(window.tabFolder, SWT.HORIZONTAL);
		window.setControl(window.graphicSashForm);

		window.curveSelectorComposite = new HistoSelectorComposite(window.graphicSashForm);
		window.graphicsComposite = new HistoGraphicsComposite(window.graphicSashForm);
		window.graphicSashForm.setWeights(new int[] { SELECTOR_WIDTH, GDE.shell.getClientArea().width - SELECTOR_WIDTH });

		window.setFont(SWTResourceManager.getFont(DataExplorer.getInstance(), GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		window.setText(Messages.getString(MessageIds.GDE_MSGT0792));
		return window;
}

	@Override
	public AbstractHistoChartComposite getGraphicsComposite() {
		return this.graphicsComposite;
	}

	@Override
	protected void setFixedGraphicCanvas() {
		// not required for this chart type
	}

}
