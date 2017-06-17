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

package gde.histo.settlements;

import java.util.logging.Logger;

import gde.GDE;
import gde.device.FigureTypes;
import gde.device.TransitionFigureType;
import gde.histo.recordings.RecordingsCollector;
import gde.histo.transitions.Transition;
import gde.log.Level;

/**
 * Collect settlement data for the trail recordset and subordinate objects.
 * @author Thomas Eickert (USER)
 */
public final class FigureEvaluator {
	private final static String			$CLASS_NAME	= RecordingsCollector.class.getName();
	private final static Logger			log					= Logger.getLogger($CLASS_NAME);

	private final SettlementRecord	histoSettlement;

	public FigureEvaluator(SettlementRecord newHistoSettlement) {
		this.histoSettlement = newHistoSettlement;
	}

	/**
	 * Figure out the result value and add result value.
	 * The timeSum and timeStep value has the unit seconds with 3 decimal places.
	 * @param transition
	 */
	public void addFromTransition(Transition transition) {
		TransitionFigureType transitionFigure = this.histoSettlement.getSettlement().getEvaluation().getTransitionFigure();
		log.log(Level.FINE, GDE.STRING_GREATER, transitionFigure);
		final int reverseTranslatedResult;
		if (transitionFigure.getFigureType() == FigureTypes.COUNT) {
			reverseTranslatedResult = transition.getThresholdSize() * 1000; // all internal values are multiplied by 1000
		}
		else if (transitionFigure.getFigureType() == FigureTypes.TIME_SUM_SEC) {
			reverseTranslatedResult = (int) (this.histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1) - this.histoSettlement.getParent().getTime_ms(transition.getThresholdStartIndex()));
		}
		else if (transitionFigure.getFigureType() == FigureTypes.TIME_STEP_SEC) {
			reverseTranslatedResult = (int) this.histoSettlement.getParent().getTime_ms(transition.getThresholdStartIndex());
		}
		else {
			reverseTranslatedResult = 0;
			throw new UnsupportedOperationException();
		}
		// add to settlement record
		this.histoSettlement.add(reverseTranslatedResult);
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE,
				String.format("%s: timeStamp_ms=%d  reverseTranslatedResult=%d  figureType=%s", this.histoSettlement.getName(), (int) this.histoSettlement.getParent().getTime_ms(transition.getThresholdEndIndex() + 1) //$NON-NLS-1$
						, reverseTranslatedResult, transitionFigure.getFigureType()));
	}

}
