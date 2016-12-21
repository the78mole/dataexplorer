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
    
    Copyright (c) 2016 Thomas Eickert
****************************************************************************************/
package gde.device;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;
import gde.histocache.HistoVault;

/**
 * devices with history support implementations.
 * @author Thomas Eickert
 */
public interface IHistoDevice { //todo merging with IDevice later

	/**
	 * create history recordSet and add record data size points from binary file to each measurement.
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data.
	 * since this is a long term operation the progress bar should be updated to signal business to user. 
	 * collects life data if device setting |isLiveDataActive| is true.
	 * reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param filePath 
	 * @param trusses referencing a subset of the recordsets in the file
	 * @throws DataInconsitsentException 
	 * @throws DataTypeException 
	 * @throws IOException 
	 * @return the histo vault list collected for the trusses (may contain vaults without measurements, settlements and scores)
	 */
	public List<HistoVault>  getRecordSetFromImportFile(Path filePath, Collection<HistoVault> trusses) throws DataInconsitsentException, IOException, DataTypeException;

	/**
	 * reduce memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param maxPoints maximum values from the data buffer which are verified during sampling
	 * @param minPoints minimum values from the data buffer which are verified during sampling
	 * @throws DataInconsitsentException 
	 */
	public void setSampling(int[] maxPoints, int[] minPoints) throws DataInconsitsentException;

}
