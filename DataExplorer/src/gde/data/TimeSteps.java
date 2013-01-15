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
    
    Copyright (c) 2008,2009,2010,2011,2012,2013 Winfried Bruegmann
****************************************************************************************/
package gde.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import gde.log.Level;
import java.util.logging.Logger;

/**
 * TimeSteps class handles all the time steps of a record set or a record of part of compare set
 * @author Winfried Brügmann
 */
public class TimeSteps extends Vector<Long> {
	final static String			$CLASS_NAME				= RecordSet.class.getName();
	final static long				serialVersionUID	= 26031957;
	final static Logger			log								= Logger.getLogger(RecordSet.class.getName());

	final boolean						isConstant;				// true if the time step is constant and the consumed time is a number of measurement points * timeStep_ms
	final SimpleDateFormat	timeFormat				= new SimpleDateFormat("HH:mm:ss.SSS");
	final SimpleDateFormat	absoluteTimeFormat= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	long  startTime = 0;

	
	/**
	 * Constructs a new TimeSteps class, a give time step greater than 0 signals that the time step is constant between measurement points
	 * This class should hide time step calculations for constant or individual time steps of a device.
	 * @param newTimeStep_ms
	 */
	public TimeSteps(double newTimeStep_ms) {
		super(1, 1);
		this.timeFormat.getTimeZone().setRawOffset(0);
		this.isConstant = newTimeStep_ms > 0;
		if (this.isConstant) 
			super.add((long) (newTimeStep_ms * 10));
		this.startTime = new Date().getTime();
	}
	
	/**
	 * copy constructor
	 */
	private TimeSteps(TimeSteps toBeClonedTimeSteps) {
  	super(toBeClonedTimeSteps);
  	this.isConstant = toBeClonedTimeSteps.isConstant;
		this.startTime = toBeClonedTimeSteps.startTime;
	}
	
	/**
	 * copy constructor
	 */
	private TimeSteps(TimeSteps toBeClonedTimeSteps, int index, boolean isFromBegin) {
  	super(toBeClonedTimeSteps);
  	this.clear();
  	if (!(this.isConstant = toBeClonedTimeSteps.isConstant)) {
			this.startTime = isFromBegin ? toBeClonedTimeSteps.startTime + toBeClonedTimeSteps.get(index)/10 : toBeClonedTimeSteps.startTime;
			if (isFromBegin) {
				long cutOffVal = toBeClonedTimeSteps.get(index);
				for (int i = index; i < toBeClonedTimeSteps.elementCount; i++) {
					super.add(toBeClonedTimeSteps.get(i) - cutOffVal);
				}
			}
			else {
				for (int i = 0; i < index; i++) {
					this.add(toBeClonedTimeSteps.get(i));
				}
			}
		}
  	else {
  		this.startTime = toBeClonedTimeSteps.startTime;
  	}
  	if (log.isLoggable(Level.FINER)) log.log(Level.FINER, this.toString());
	}
	
	/**
	 * overwritten clone method
	 */
	@Override
	public synchronized TimeSteps clone() {
		super.clone();
  	return new TimeSteps(this);
  }
	
	/**
	 * clone method re-writes time steps
	 * - if isFromBegin == true, the given index is the index where the record starts after this operation
	 * - if isFromBegin == false, the given index represents the last data point index of the records.
	 */
	public synchronized TimeSteps clone(int index, boolean isFromBegin) {
  	return new TimeSteps(this, index, isFromBegin);
  }
	

 	/**
	 * query the delta time in msec between two index positions
	 * @param indexStart
	 * @param indexEnd
	 * @return delta time in msec
	 */
	public double getDeltaTime(int indexStart, int indexEnd) {
		if (indexStart < 0) {
			log.log(Level.WARNING, "indexStart < 0 " + indexStart);
			indexStart = 0;
		}
		if (indexEnd > this.elementCount - 1) {
			log.log(Level.WARNING, "indexEnd > this.elementCount - 1 " + indexEnd);
			indexEnd = this.elementCount - 1;
		}
		indexEnd = indexEnd > this.elementCount - 1 ? this.elementCount - 1 : indexEnd;
		return (this.get(indexEnd) - this.get(indexStart)) / 10.0;
	}

	/**
	 * query time at an index position
	 * @param index
	 * @return time fit to index
	 */
	public double getTime_ms(int index) {
		synchronized (this) {
			return this.isConstant ? (index == 0 ? 0.0 : this.get(0) / 10.0 * index) : (index < 0 ? this.firstElement() : index > elementCount - 1 ? this.lastElement() / 10.0 : this.get(index) / 10.0);
		}
	}

	/**
	 * query time at an index position and return as HH:mm:ss:SSS formated string
	 * @param formatPattern yy:mm:dd HH:mm:ss:SSS
	 * @param index
	 * @return time fit to index
	 */
	public String getFormattedTime(String formatPattern, int index, boolean isAbsolute) {
		this.timeFormat.applyPattern(formatPattern);			
		return String.format("%25s", isAbsolute ? this.getIndexDateTime(index) : this.timeFormat.format(this.getTime_ms(index)));
	}

	/**
	 * add a new time step
	 * @param value
	 * @return true if add was successful
	 */
	public synchronized boolean add(double value) {
		synchronized (this) {
			return this.isConstant ? true : super.add((long) (value * 10));
		}
	}
	
	/**
	 * @return the const. time step in msec
	 */
	public double getAverageTimeStep_ms() {
		try {
			return this.isConstant ? this.getTime_ms(1) : (this.elementCount > 2 ? (double)this.lastElement()/(elementCount-1)/10.0 : this.get(1)/10.0);
		}
		catch (Exception e) {
			// a redraw event where the record set has no records 
			return 0.0;
		} 
	}

	/**
	 * @return the isConstant true if time step is a constant value between measurement points
	 */
	public boolean isConstant() {
		return isConstant;
	}
	
	/**
	 * @return the maximum time relative to the displayable area 
	 */
	public double getMaxTime_ms() {
		double maxTime = 0.0;
		if (isConstant) {
			if (this.size() < 1)
				System.out.println();
			maxTime = this.get(0)/10.0; 
		}
		else {
			maxTime = this.elementCount > 1 ? this.lastElement()/10.0 : 0.0;
		}
		//return isConstant ? this.get(0)*this.parent.realSize()/10.0 : this.lastElement()/10.0;
		return maxTime;
	}
	
	/**
	 * Find the indexes in this time vector where the given time value is placed
	 * In case of the given time in in between two available measurement points both bounding indexes are returned, 
	 * only in case where the given time matches an existing entry both indexes are equal.
	 * In cases where the returned indexes are not equal the related point x/y has to be interpolated.
	 * @param time_ms
	 * @return two indexes around given time
	 */
	public int[] findBoundingIndexes(double time_ms) {
		//log.log(Level.INFO, "time_ms = " + time_ms);
		int index1 = 0, index2 = 0;
		if (time_ms > 0 && elementCount > 0) {
			if (isConstant) {
				double position = time_ms / (this.get(0) / 10.0);
				index1 = (int) position;
				index2 = (int) (position + 0.5);
			}
			else {
				int value = Double.valueOf(time_ms * 10.0).intValue();
				for (; index2 < elementCount - 1; index2++) {
					if (value == get(index2)) {
						index1 = index2;
						break;
					}
					else if (value < get(index2)) {
						index1 = index2 > 0 ? index2 - 1 : 0;
						break;
					}
					index1 = index2 + 1;
				}
			}
		}
		//log.log(Level.INFO, index1 + " - " + index2);
		return new int[] {index1, index2};
	}

	/**
	 * find the index closest to given time in msec
	 * @param time_ms
	 * @return index closest to given time
	 */
	public int findBestIndex(double time_ms) {
		int index = 0;
		if (time_ms > 0) {
			double position = 0.0;
			if (this.isConstant) {
				position = time_ms / (this.get(0) / 10.0);
				index = (int) (position + 0.5);
			}
			else {
				index = (int) (time_ms / (this.lastElement() / (double)(this.elementCount - 1) / 10.0) / 2.0);
				int value = Double.valueOf(time_ms * 10.0).intValue();
				for (; index < elementCount; index++) {
					if (value <= this.get(index)) break;
				}
				if (index + 1 <= this.elementCount - 1 && value > (this.get(index + 1) + this.get(index)) / 2) index = index + 1;
			}
		}
		//log.log(Level.INFO, "index=" + index);
		return index;
	}
	
	/**
	 * set absolute start and end time of this record set
	 * @param startTimeStamp
	 */
	public void setStartTimeStamp(long startTimeStamp) {
		this.startTime = startTimeStamp;
	}

	/**
	 * return the start time stamp
	 * @return
	 */
	public long getStartTimeStamp() {
		return this.startTime;
	}
	
	/**
	 * query the formated absolute date time at index
	 * @param index
	 * @return
	 */
	public String getIndexDateTime(int index) {
		return absoluteTimeFormat.format(this.startTime + getTime_ms(index));
	}
}
