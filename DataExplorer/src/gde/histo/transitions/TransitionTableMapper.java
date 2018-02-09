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

package gde.histo.transitions;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import gde.data.RecordSet;
import gde.device.ChannelType;
import gde.device.IDevice;
import gde.device.SettlementType;
import gde.device.TransitionAmountType;
import gde.device.TransitionCalculusType;
import gde.device.TransitionFigureType;
import gde.device.TransitionGroupType;
import gde.histo.settlements.AmountEvaluator;
import gde.histo.settlements.CalculusEvaluator;
import gde.histo.settlements.FigureEvaluator;
import gde.histo.settlements.SettlementRecord;
import gde.histo.transitions.GroupTransitions.TransitionChronicle;
import gde.log.Logger;
import gde.ui.DataExplorer;

/**
 * Transition data mapping for the table tab.
 * Support synchronization for ongoing data gatherings.
 * @author Thomas Eickert (USER)
 */
public final class TransitionTableMapper {
	private static final String	$CLASS_NAME	= TransitionTableMapper.class.getName();
	private static final Logger	log					= Logger.getLogger($CLASS_NAME);

	private final DataExplorer	application	= DataExplorer.application;
	private final IDevice				device			= DataExplorer.application.getActiveDevice();

	private final RecordSet			recordSet;
	private final ChannelType		channel;

	public TransitionTableMapper(RecordSet recordSet) {
		this.recordSet = recordSet;
		this.channel = device.getDeviceConfiguration().getChannel(recordSet.getChannelConfigNumber());
	}

	public static synchronized String[] getExtendedRow(RecordSet recordSet, int index, String[] dataTableRow) {
		return new TransitionTableMapper(recordSet).defineRowWithSettlements(index, dataTableRow);
	}

	/**
	 * @param index
	 * @param dataTableRow
	 * @return the row with additional columns for the active settlements with reasonable data
	 */
	public synchronized String[] defineRowWithSettlements(int index, String[] dataTableRow) {
		LinkedHashMap<Integer, SettlementType> settlementTypes = defineActiveAndDisplayableSettlements();
		HashMap<Integer, TransitionGroupType> transitionGroups = channel.getTransitionGroups();
		int tableColumnsSize = recordSet.getVisibleAndDisplayableRecordsForTable().size() + 1;
		String[] tableRow = Arrays.copyOf(dataTableRow, tableColumnsSize + settlementTypes.size() + transitionGroups.size());

		GroupTransitions histoTransitions = recordSet.getHistoTransitions();
		LinkedHashMap<String, SettlementRecord> settlements = determineSettlements(histoTransitions, settlementTypes.values());

		int columnIndex = tableColumnsSize;
		for (SettlementRecord settlementRecord : settlements.values()) {
			Double value = settlementRecord.getTranslatedValue(index);
			if (value != null) {
				tableRow[columnIndex] = String.format("%.3f", value);
			}
			columnIndex++;
		}

		for (Entry<Integer, TransitionGroupType> transitionsGroupsEntry : transitionGroups.entrySet()) {
			TransitionChronicle transitionChronicle = recordSet.getHistoTransitions().get(transitionsGroupsEntry.getKey());
			if (transitionChronicle != null) {
				Transition transition = transitionChronicle.get((long) recordSet.getTime_ms(index));
				if (transition != null) {
					tableRow[columnIndex] = Integer.toString(transition.getTransitionType().getTransitionId());
				}
			}
			columnIndex++;
		}
		return tableRow;
	}

	/**
	 * @return the settlementTypes with reasonable data (key is settlementId)
	 */
	public LinkedHashMap<Integer, SettlementType> defineActiveAndDisplayableSettlements() {
		LinkedHashMap<Integer, SettlementType> channelSettlements = new LinkedHashMap<>(channel.getSettlements());
		for (Iterator<Entry<Integer, SettlementType>> iterator = channelSettlements.entrySet().iterator(); iterator.hasNext();) {
			Entry<Integer, SettlementType> entry = iterator.next();
			if (entry.getValue().isActive() && hasTransitions(entry.getValue())) {

			} else {
				iterator.remove();
			}
		}
		log.finer(() -> "settlementTypes.size=" + channel.getSettlements().size() + " size=" + channelSettlements.size());
		return channelSettlements;
	}

	private boolean hasTransitions(SettlementType settlementType) {
		GroupTransitions histoTransitions = recordSet.getHistoTransitions();
		int transitionGroupId = settlementType.getEvaluation().getTransitionCalculus().getTransitionGroupId();
		TransitionChronicle transitionChronicle = histoTransitions.get(transitionGroupId);
		if (transitionChronicle != null) {
			return true;
		} else {
			return false;
		}
	}

	private boolean hasReasonableData(SettlementType settlementType) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	/**
	 * @return the calculated settlements calculated from transitions (key is the name of the settlementType)
	 */
	private LinkedHashMap<String, SettlementRecord> determineSettlements(GroupTransitions transitions, Collection<SettlementType> settlementTypes) {
		LinkedHashMap<String, SettlementRecord> histoSettlements = new LinkedHashMap<String, SettlementRecord>();
		for (SettlementType settlementType : settlementTypes) {
			if (settlementType.getEvaluation() != null) {
				SettlementRecord record = new SettlementRecord(settlementType, recordSet, application.getActiveChannelNumber());
				histoSettlements.put(settlementType.getName(), record);
				if (transitions.isEmpty()) continue;

				TransitionFigureType transitionFigureType = settlementType.getEvaluation().getTransitionFigure();
				TransitionAmountType transitionAmountType = settlementType.getEvaluation().getTransitionAmount();
				TransitionCalculusType calculationType = settlementType.getEvaluation().getTransitionCalculus();
				if (transitionFigureType != null) {
					FigureEvaluator evaluator = new FigureEvaluator(record);
					for (Transition transition : transitions.get(transitionFigureType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (transitionAmountType != null) {
					AmountEvaluator evaluator = new AmountEvaluator(record);
					for (Transition transition : transitions.get(transitionAmountType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else if (calculationType != null) {
					CalculusEvaluator evaluator = new CalculusEvaluator(record);
					for (Transition transition : transitions.get(calculationType.getTransitionGroupId()).values()) {
						evaluator.addFromTransition(transition);
					}
				} else {
					throw new UnsupportedOperationException();
				}
			}
		}
		return histoSettlements;
	}

}
