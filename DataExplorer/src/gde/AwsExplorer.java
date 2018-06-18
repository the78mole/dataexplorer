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

import gde.log.Logger;

/**
 *
 * @author Thomas Eickert (USER)
 */
public class AwsExplorer implements IExplorer{
	private static final String	$CLASS_NAME	= AwsExplorer.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	private final DataAccess			dataAccess;

	/**
	 *
	 */
	public AwsExplorer() {
		this.dataAccess = null;
	}
	/**
	 * @return the roaming data sources support
	 */
	@Override
	public DataAccess getDataAccess() {
		return this.dataAccess;
	}

}
