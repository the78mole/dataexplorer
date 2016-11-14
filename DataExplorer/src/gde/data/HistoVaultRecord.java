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

package gde.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * history data points for trail types or score types. 
 * supports measurements and settlements and scores.
 * history data persistence prototype.
 * @author Thomas Eickert
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HistoVaultRecordS", propOrder = { //$NON-NLS-1$
		"pointTypes", "points" //$NON-NLS-2$
})
public class HistoVaultRecord {

	@XmlElement(required = false)
	private String[]	pointLabels;	// trail types or score label types
	@XmlElement(required = true)
	private Integer[]	points;				// history data points per trail type or score type, index is enum ordinal

	public HistoVaultRecord() {
		this.pointLabels = null;
	}

	/**
	* creates an empty list of history data points holding one entry for each trail type or score type.
	* @param pointLabels defines the persistence data structure
	*/
	public HistoVaultRecord(String[] pointLabels) {
		this.pointLabels = pointLabels;
		this.points = new Integer[pointLabels.length];
	}

	/** 
	 * set data points for all trails or scores.
	 * @param points values with array index equal to the trail type ordinal or scoreType ordinal.
	 */
	public void setPoints(Integer[] points) {
		this.points = points;
	}

	/**
	 * @return data points sorted by point type ordinal 
	 */
	public Integer[] getPoints() {
		return this.points;
	}

}