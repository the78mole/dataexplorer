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

    Copyright (c) 2018 Thomas Eickert
****************************************************************************************/

package gde;

import gde.data.Channels;
import gde.log.Logger;

/**
 * Kernel for analyzing logging data.
 * Use this with the integrated DataExplorer UI.
 * @author Thomas Eickert (USER)
 */
public class Explorer extends Analyzer {
	private static final String	$CLASS_NAME	= Explorer.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	Explorer() {
		super();
	}

	private Explorer(Explorer analyzer) {
		super(analyzer);
	}

	public void setChannels(Channels channels) {
		this.channels = channels;
	}

	@Override
	public Explorer clone() {
		return new Explorer(this);
	}

}
