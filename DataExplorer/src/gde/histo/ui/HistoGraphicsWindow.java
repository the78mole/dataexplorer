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
import gde.ui.SWTResourceManager;

/**
 * Histo graphics window as a sash form of a curve selection table and a drawing canvas.
 * @author Thomas Eickert
 */
public final class HistoGraphicsWindow extends AbstractHistoChartWindow {
	private final static String	$CLASS_NAME	= HistoGraphicsWindow.class.getName();
	final static Logger					log					= Logger.getLogger($CLASS_NAME);

	public HistoGraphicsWindow(CTabFolder currentDisplayTab, int style, int index) {
		super(currentDisplayTab, style, index);
	}

	public synchronized void create() {
		this.graphicSashForm = new SashForm(this.tabFolder, SWT.HORIZONTAL);
		this.setControl(this.graphicSashForm);

		// determine the selector which is shared by all chart windows
		HistoSelectorComposite selectorComposite = this.application.getHistoChartSelectorComposite();
		this.curveSelectorComposite = selectorComposite != null ? selectorComposite : new HistoSelectorComposite(this.graphicSashForm);

		this.graphicsComposite = new HistoGraphicsComposite(this.graphicSashForm);
		this.graphicSashForm.setWeights(new int[] { SELECTOR_WIDTH, GDE.shell.getClientArea().width - SELECTOR_WIDTH });

		this.setFont(SWTResourceManager.getFont(this.application, GDE.WIDGET_FONT_SIZE + 1, SWT.NORMAL));
		this.setText(Messages.getString(MessageIds.GDE_MSGT0792));
}

	@Override
	public HistoGraphicsComposite getGraphicsComposite() {
		return (HistoGraphicsComposite) this.graphicsComposite;
	}

}
