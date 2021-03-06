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
    along with GNU DataExplorer.  If not, see <https://www.gnu.org/licenses/>.

    Copyright (c) 2018,2019 Thomas Eickert
****************************************************************************************/

package gde;

import gde.data.Channels;
import gde.device.IDevice;

/**
 * Kernel for analyzing logging data.
 * Use this with the integrated DataExplorer UI.
 * @author Thomas Eickert (USER)
 */
public class Explorer extends Analyzer {

	Explorer() {
		super();
	}

	private Explorer(Explorer analyzer) {
		super(analyzer);
	}

	public void setChannels() {
		this.channels = Channels.getInstance();
	}

	@Override
	public Analyzer getReplica() {
		return this;
	}

	@Override
	public void setActiveDevice(String deviceName) {
		throw new UnsupportedOperationException("use UI functions instead");
	}

	@Override
	public void setChannelNumber(int channelNumber) {
		throw new UnsupportedOperationException("use UI functions instead");
	}

	@Override
	public void setArena(IDevice device, int channelNumber, String objectKey) {
		throw new UnsupportedOperationException("use UI functions instead");
	}

}
