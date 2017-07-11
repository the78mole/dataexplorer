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

package gde.histo.device;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Sample candidate without any further selection support.
 * @author Thomas Eickert (USER)
 */
public class Candidate {
	private final static String		$CLASS_NAME	= Candidate.class.getName();
	private final static Logger		log					= Logger.getLogger($CLASS_NAME);

	protected int[]									points;
	protected long									timeStep_ms	= -1;

	public Candidate() {
	}

	/**
	 *
	 * @param newPoints
	 * @param newTimeStep_ms
	 */
	public void processSample(int[] newPoints, long newTimeStep_ms) {
		set(newPoints, newTimeStep_ms);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("timeStep_ms=%,d", this.timeStep_ms));
		if (this.points != null)
			sb.append("\n").append(Arrays.toString(this.points));
		else
			sb.append("\n").append("no points");
		return sb.toString();
	}

	/**
	 * @param newPoints or null if it is no valid sample
	 * @param newTimeStep_ms
	 */
	private void set(int[] newPoints, long newTimeStep_ms) {
		this.points = newPoints;
		this.timeStep_ms = newTimeStep_ms;
	}

	/**
	 * @return the points or null if there is no valid sample
	 */
	public int[] getSamplePoints() {
		return this.points;
	}

	/**
	 * @return the timestep or -1 if there is no valid sample
	 */
	public long getSampleTimeStep_ms() {
		return this.timeStep_ms;
	}

}
