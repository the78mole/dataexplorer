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

import gde.data.RecordSet;
import gde.exception.DataInconsitsentException;
import gde.exception.DataTypeException;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * devices with history support implementations.
 * @author Thomas Eickert
 */
public interface IHistoDevice { //todo merging with IDevice later

	/**
	 * add record data size points from binary file to each measurement.
	 * it is possible to add only none calculation records if makeInActiveDisplayable calculates the rest.
	 * do not forget to call makeInActiveDisplayable afterwards to calculate the missing data.
	 * since this is a long term operation the progress bar should be updated to signal business to user. 
	 * reduces memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param recordSet target object holding the records (curves) which include measurement curves and calculated curves 
	 * @param filePath 
	 * @throws DataInconsitsentException 
	 * @throws Exception 
	 */
	public void addImportFileAsRawDataPoints(RecordSet recordSet, String filePath) throws DataInconsitsentException, FileNotFoundException, IOException, DataTypeException;

	/**
	 * reduce memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param pointsLength number of non-calculation measurement points  
	 */
	public void setSampling(int pointsLength);

	/**
	 * reduce memory and cpu load by taking measurement samples every x ms based on device setting |histoSamplingTime| .
	 * @param maxPoints maximum values from the data buffer which are verified during sampling
	 * @param minPoints minimum values from the data buffer which are verified during sampling
	 * @throws DataInconsitsentException 
	 */
	public void setSampling(int[] maxPoints, int[] minPoints) throws DataInconsitsentException ;


}
