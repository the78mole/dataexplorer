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

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

import gde.log.Logger;

/**
 * Histo chart window base class.
 * @author Thomas Eickert
 */
public abstract class AbstractHistoChartWindow extends CTabItem {
	private final static String			$CLASS_NAME							= AbstractHistoChartWindow.class.getName();
	private final static Logger			log											= Logger.getLogger($CLASS_NAME);

	/**
	 * y position of the first table row and graphics chart gap.
	 */
	static final int				HEADER_ROW_HEIGHT				= 33;

	protected AbstractHistoChartWindow(CTabFolder parent, int style, int index) {
		super(parent, style, index);
		// TODO Auto-generated constructor stub
	}

}
