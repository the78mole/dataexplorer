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

    Copyright (c) 2017 Thomas Eickert
****************************************************************************************/

package gde.histo.ui;

import gde.histo.recordings.TrailRecord;
import gde.histo.recordings.TrailRecordCutter;

/**
 * General measuring object for data exchange with window composites.
 */
class Measure {
	final boolean			isDeltaMeasure;
	final TrailRecord	measureRecord;
	long							timestampMeasure_ms;
	long							timestampDelta_ms;
	TrailRecordCutter	recordSection;

	Measure(boolean isDeltaMeasuring, TrailRecord measuringRecord) {
		this.isDeltaMeasure = isDeltaMeasuring;
		this.measureRecord = measuringRecord;
	}

}