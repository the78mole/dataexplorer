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
    
    Copyright (c) 2008 - 2010 Winfried Bruegmann
****************************************************************************************/
package gde.utils;

import java.util.Vector;
import java.util.concurrent.TimeUnit;
import gde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import gde.data.RecordSet;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.SWTResourceManager;

/**
 * Utility class to draw time line with tick marks and numbers
 * @author Winfried Brügmann
 */
public class TimeLine {
	final static Logger			log									= Logger.getLogger(TimeLine.class.getName());

	public final static int	TIME_LINE_MSEC			= 0;
	public final static int	TIME_LINE_SEC				= 1;
	public final static int	TIME_LINE_SEC_MIN		= 2;
	public final static int	TIME_LINE_MIN				= 3;
	public final static int	TIME_LINE_MIN_HRS		= 4;
	public final static int	TIME_LINE_HRS				= 5;

	String									timeLineText				= Messages.getString(MessageIds.GDE_MSGT0267);
	boolean									isTimeLinePrepared	= false;

	/**
	 * calculates the maximum time to be displayed and the scale number factor
	 * @param totalDisplayTime_ms
	 * @return maxTimeNumber, scale number factor
	 */
	public int[] getScaleMaxTimeNumber(double totalDisplayTime_ms) {
		int factor = 10; // for the most cases (make factor 10 based to enable 0.5 by factor 5)
		int format = TimeLine.TIME_LINE_MSEC; // the time format type 

		log.log(Level.FINE, "totalDisplayTime_ms = " + totalDisplayTime_ms); //$NON-NLS-1$

		long totalTime_msec = Double.valueOf(totalDisplayTime_ms).longValue();
		long totalTime_sec = Double.valueOf(totalDisplayTime_ms / 1000.0).longValue();
		long totalTime_min = TimeUnit.MINUTES.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_std = TimeUnit.HOURS.convert(totalTime_sec, TimeUnit.SECONDS);
		log.log(Level.FINE, "totalTime_std = " + totalTime_std + "; totalTime_min = " + totalTime_min + "; totalTime_sec = " + totalTime_sec + "; totalTime_ms = " + totalTime_msec + " - " + Integer.MAX_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int maxTimeNumberFormated; // the biggest number in the scale to be displayed

		if (totalTime_std > 5) {
			maxTimeNumberFormated = (int) totalTime_std;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0265);
			format = TimeLine.TIME_LINE_HRS;
		}
		else if (totalTime_min > 60) {
			maxTimeNumberFormated = (int) totalTime_min;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0266);
			format = TimeLine.TIME_LINE_MIN_HRS;
		}
		else if (totalTime_min > 10) {
			maxTimeNumberFormated = (int) totalTime_min;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0267);
			format = TimeLine.TIME_LINE_MIN;
		}
		else if (totalTime_sec > 60) {
			maxTimeNumberFormated = (int) totalTime_sec;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0268);
			format = TimeLine.TIME_LINE_SEC_MIN;
		}
		else if (totalTime_sec > 10) {
			maxTimeNumberFormated = (int) totalTime_sec;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0269);
			format = TimeLine.TIME_LINE_SEC;
		}
		else if (totalTime_sec > 1) {
			maxTimeNumberFormated = (int) totalTime_msec;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0269);
			factor = 1000; // 2900 -> 2,9 sec
			format = TimeLine.TIME_LINE_SEC;
		}
		else {
			maxTimeNumberFormated = (int) totalTime_msec;
			timeLineText = Messages.getString(MessageIds.GDE_MSGT0271);
			factor = 1;
			format = TimeLine.TIME_LINE_MSEC;
		}
		log.log(Level.FINE, timeLineText + "  " + maxTimeNumberFormated); //$NON-NLS-1$

		this.isTimeLinePrepared = true;

		log.log(Level.FINE, "timeLineText = " + timeLineText + " maxTimeNumber = " + maxTimeNumberFormated + " factor = " + factor); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return new int[] { maxTimeNumberFormated, factor, format, Long.valueOf(totalTime_msec).intValue() };
	}

	/**
	 * draws the time line - requires to call preparation steps
	 * - x0, y0 defines the start point of the scale, y0, width the nd point
	 * @param recordSet
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startTimeValue  the time value where the scale should start to count
	 * @param endTimeValue depends on the time gap and the unit calculated from the gap
	 * @param timeFormat - TIME_LINE_MSEC, TIME_LINE_SEC, TIME_LINE_SEC_MIN, ..
	 * @param deltaTime_ms the difference in ms between start and end time
	 * @param color
	 */
	public void drawTimeLine(RecordSet recordSet, GC gc, int x0, int y0, int width, int startTimeValue, int endTimeValue, int scaleFactor, int timeFormat, int deltaTime_ms, Color color) {
		if (this.isTimeLinePrepared == false) {
			log.log(Level.WARNING, "isTimeLinePrepared == false -> getScaleMaxTimeNumber(RecordSet recordSet) needs to be called first"); //$NON-NLS-1$
			return;
		}

		// set the line color and draw a horizontal time line
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(color);
		gc.drawLine(x0-1, y0, x0+width+1, y0);
		log.log(Level.FINER, String.format("time line - x0=%d y0=%d - width=%d - maxNumber=%d - scaleFactor=%d", x0, y0, width, endTimeValue, timeFormat)); //$NON-NLS-1$

		Point pt = gc.textExtent(timeLineText);
		int ticklength = pt.y / 2;
		int gap = pt.y / 3;
		int miniTicks = 3;

		drawTickMarks(recordSet, gc, x0, y0, width, startTimeValue, endTimeValue, scaleFactor, timeFormat, deltaTime_ms, ticklength, miniTicks, gap);

		// draw the scale description centered
		GraphicsUtils.drawTimeLineText(timeLineText, (x0 + width / 2), y0 + ticklength + pt.y * 2, gc, SWT.HORIZONTAL);
	}

	/**
	 * draws tick marks to a scale in horizontal direction
	 * @param recordSet
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startTimeValue the time value where the scale should start to count
	 * @param endTimeValue the number where the time scale should end, endTimeValue - startTimeValue -> number of ticks drawn
	 * @param scaleFactor (100 -> deltaTime of 2900 = 2.9)
	 * @param timeFormat - TIME_LINE_MSEC, TIME_LINE_SEC, TIME_LINE_SEC_MIN, ..
	 * @param deltaTime_ms the difference in ms between start and end time
	 * @param ticklength of the main ticks
	 * @param miniticks number of mini ticks drawn between the main ticks
	 * @param gap distance between ticks and the number scale
	 */
	private void drawTickMarks(RecordSet recordSet, GC gc, int x0, int y0, int width, int startTimeValue, int endTimeValue, double scaleFactor, int timeFormat, int deltaTime_ms, int ticklength, int miniticks, int gap) {
		double numberTicks, timeDelta;
		//int offset = (startTimeValue != 0) ? 10 - startTimeValue % 10 : 0;
		int timeDeltaValue = endTimeValue - startTimeValue;
		log.log(Level.FINER, "timeDelta = " + timeDeltaValue + " startTime = " + startTimeValue + " endTime = " + endTimeValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		// calculate a scale factor, a big time difference would have to much ticks
		if (timeDeltaValue > 0) {
			switch (timeFormat) {
			case TimeLine.TIME_LINE_SEC:
			case TimeLine.TIME_LINE_SEC_MIN:
				timeDelta = (deltaTime_ms/1000.0);
				break;
			case TimeLine.TIME_LINE_MIN:
			case TimeLine.TIME_LINE_MIN_HRS:
				timeDelta = (deltaTime_ms/60000.0);
				break;
			case TimeLine.TIME_LINE_HRS:
				timeDelta = (deltaTime_ms/3600000.0);
				break;
			case TimeLine.TIME_LINE_MSEC:
			default:
				timeDelta =(deltaTime_ms * 1.0);
				break;
			}
			
			switch (timeFormat * (int)scaleFactor) { // TIME_LINE_MSEC, TIME_LINE_SEC, TIME_LINE_SEC_MIN, ..
			case TimeLine.TIME_LINE_MSEC * 1:
				if (timeDeltaValue <= width*2) {
					numberTicks = timeDelta / 100.0; // every 1'th units one tick
					scaleFactor = 1;
				}
				else {
					numberTicks = timeDelta / 200.0; // every 2'th units one tick
					scaleFactor = 0.5;
				}
//				numberTicks = timeDelta / 100; // every 1'th units one tick
//				scaleFactor = 1;
				break;
			case TimeLine.TIME_LINE_SEC * 100:
				if (timeDeltaValue <= width*10) {
					numberTicks = timeDelta / 1000.0; // every 1'th units one tick
					scaleFactor = 100;
				}
				else {
					numberTicks = timeDelta / 500.0; // every 2'th units one tick
					scaleFactor = 200;
				}
				break;
			case TimeLine.TIME_LINE_SEC * 1000:
				if (timeDeltaValue <= width*10) {
					numberTicks = timeDelta / 0.5; // every 1'th units one tick
					scaleFactor = 200;
				}
				else {
					numberTicks = timeDelta / 1.0; // every 2'th units one tick
					scaleFactor = 100;
				}
				break;
			default:
			case TimeLine.TIME_LINE_SEC * 10:
			case TimeLine.TIME_LINE_MIN * 10:
			case TimeLine.TIME_LINE_HRS * 10:
				if (timeDeltaValue > 0 && timeDeltaValue <= width/100) {
					numberTicks = timeDelta * 2.0; // every 0.5'th units one tick
					scaleFactor = scaleFactor * 20.0;
				}
				else if (timeDeltaValue > width/100 && timeDeltaValue <= width/70) {
					numberTicks = timeDelta; // every 1'th units one tick
					scaleFactor = scaleFactor * 10.0;
				}
				else if (timeDeltaValue > width/70 && timeDeltaValue <= width/25) {
					numberTicks = timeDelta / 2.5; // every 2.5 th units one tick
					scaleFactor = scaleFactor * 4.0;
				}
				else if (timeDeltaValue > width/25 && timeDeltaValue < width/10) {
					numberTicks = timeDelta / 5.0; // every 5 th units one tick
					scaleFactor = scaleFactor * 2.0;
				}
				else {
					numberTicks = timeDelta / 10.0; // every 10th units one tick
				}
				break;
			case TimeLine.TIME_LINE_SEC_MIN * 10:
			case TimeLine.TIME_LINE_MIN_HRS * 10:
				if (timeDeltaValue >= 0 && timeDeltaValue <= 30) {
					numberTicks = timeDelta / 2.5; // every 2.5 th units one tick
					scaleFactor = scaleFactor * 4.0;
				}
				else if (timeDeltaValue > width/25 && timeDeltaValue <= width/7) {
					numberTicks = timeDelta / 5.0; // every 5 th units one tick
					scaleFactor = scaleFactor * 2.0;
				}
				else if (timeDeltaValue >= width/7 && timeDeltaValue <= width/1.5) {
					numberTicks = timeDelta / 10.0; // every 10 th units one tick
					scaleFactor = scaleFactor * 1.0;
				}
				else {
					numberTicks = timeDelta / 20.0; // every 20 th units one tick
					scaleFactor = scaleFactor / 2.0;
				}
				break;
			}
			log.log(Level.FINE, "timeFormat = " + timeFormat + " numberTicks = " + numberTicks + " startTimeValue = " + startTimeValue + " endTimeValue = " + endTimeValue); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			double deltaTick = width / numberTicks;
			miniticks++;

			// calculate the space required to draw the time values
			Point pt = gc.textExtent("00"); //$NON-NLS-1$

			// prepare grid vector
			Vector<Integer> timeGrid = new Vector<Integer>();
			boolean isBuildGridVector = recordSet.getTimeGridType() == RecordSet.TIME_GRID_MAIN;

			for (int i = 0; i <= numberTicks; i++) { // <= end of time scale tick 
				//draw the main scale ticks, length = 5 and gap to scale = 2
				double xTickPosition = x0 + i * deltaTick;
				int intXTickPosition = Double.valueOf(xTickPosition).intValue();
				if (i > 0 && isBuildGridVector) 
					timeGrid.add(intXTickPosition);
				gc.drawLine(intXTickPosition, y0, intXTickPosition, y0 + ticklength);

				//draw the sub ticks to the scale according number of miniTicks
				double deltaPosMini = deltaTick / miniticks;
				for (int j = 1; j < miniticks && i < numberTicks; j++) {
					double xMiniTickPos = (xTickPosition + j * deltaPosMini);
					int intMiniTickPos = Double.valueOf(xMiniTickPos).intValue();
					log.log(Level.FINEST, "intXTickPosition=" + intXTickPosition + ", width=" + width); //$NON-NLS-1$ //$NON-NLS-2$
					if (intMiniTickPos < (x0 + width)) {
						gc.drawLine(intMiniTickPos, y0, intMiniTickPos, y0 + ticklength / 2);
					}
				}
				//draw values to the scale	
				double timeValue = i * 100.0 / scaleFactor;
				log.log(Level.FINER, "timeValue = " + timeValue); //$NON-NLS-1$
				// prepare to make every minute or hour to bold
				boolean isMod60 = (timeValue % 60) == 0;
				String numberStr;
				if (timeFormat != TimeLine.TIME_LINE_MSEC) { // msec
					double timeValue60 = isMod60 ? timeValue / 60 : timeValue % 60; // minute, hour
					log.log(Level.FINER, "timeValue = " + timeValue + ", timeValue60 = " + timeValue60); //$NON-NLS-1$ //$NON-NLS-2$
					numberStr = (timeValue60 % 1 == 0 || isMod60) ? String.format("%.0f", timeValue60) : String.format("%.1f", timeValue60); //$NON-NLS-1$ //$NON-NLS-2$
					if (isMod60 && timeValue > 0.0) {
						gc.setFont(SWTResourceManager.getFont(gc, SWT.BOLD));
						if (i != 0 && recordSet.getTimeGridType() == RecordSet.TIME_GRID_MOD60) 
							timeGrid.add(intXTickPosition);
						GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
						gc.setFont(SWTResourceManager.getFont(gc, SWT.NORMAL));
					}
					else {
						GraphicsUtils.drawTextCentered(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
					}
				}
				else
					GraphicsUtils.drawTextCentered(("" + timeValue), intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL); //$NON-NLS-1$
			}
			recordSet.setTimeGrid(timeGrid);
		}
	}

	/**
	 * converts a given time in m_sec into time format used for time scale 
	 * @return converted time value
	 */
	public static int convertTimeInFormatNumber(double time_ms, int timeFormat) {
		long time_sec = Double.valueOf(time_ms / 1000.0).longValue();
		long time_min = TimeUnit.MINUTES.convert(time_sec, TimeUnit.SECONDS);
		long time_std = TimeUnit.HOURS.convert(time_sec, TimeUnit.SECONDS);
		log.log(Level.FINE, "time_std = " + time_std + "; time_min = " + time_min + "; time_sec = " + time_sec + "; time_ms = " + time_ms); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		int result;

		switch (timeFormat) {
		case TIME_LINE_HRS:
			result = Long.valueOf(time_std).intValue();
			break;
		case TIME_LINE_MIN_HRS:
		case TIME_LINE_MIN:
			result = Long.valueOf(time_min).intValue();
			break;
		case TIME_LINE_SEC_MIN:
		case TIME_LINE_SEC:
			result = Long.valueOf(time_sec).intValue();
			break;
		default: // TIME_LINE_MSEC
			result = Double.valueOf(time_ms).intValue();
			break;
		}
		return result;
	}

	/**
	 * get the formatted time of time value in m_sec, if hours are 0 or minutes are 0 the string will be cut of
	 * unit string [HH:mm:ss:SSS] will be appended accordingly
	 * @param milliSeconds
	 * @return string of time value in simple date format HH:mm:ss:SSS
	 */
	public static String getFomatedTimeWithUnit(double milliSeconds) {
		String time = "0"; //$NON-NLS-1$
		if (milliSeconds >= 0) {
			long lSeconds = Double.valueOf(milliSeconds / 1000.0).longValue();
			milliSeconds %= 1000;
			long lMinutes = lSeconds / 60;
			lSeconds %= 60;
			long lHours = lMinutes / 60;
			lMinutes %= 60;

			if (lMinutes == 0 && lHours == 0)
				time = String.format("%02d:%03d [ss:SSS]", lSeconds, Double.valueOf(milliSeconds).intValue()); //$NON-NLS-1$
			else if (lHours == 0)
				time = String.format("%02d:%02d:%03d [mm:ss:SSS]", lMinutes, lSeconds, Double.valueOf(milliSeconds).intValue()); //$NON-NLS-1$
			else
				time = String.format("%02d:%02d:%02d:%03d [HH:mm:ss:SSS]", lHours, lMinutes, lSeconds, Double.valueOf(milliSeconds).intValue()); //$NON-NLS-1$
		}
		return time;
	}

	/**
	 * get the formatted time of time value in m_sec, if hours are 0 or minutes are 0 the string will be cut of
	 * no unit string [HH:mm:ss:SSS] will be appended
	 * @param milliSeconds
	 * @return string of time value in simple date format HH:mm:ss:SSS
	 */
	public static String getFomatedTime(double milliSeconds) {
		String time = "0"; //$NON-NLS-1$
		if (milliSeconds >= 0) {
			long lSeconds = Double.valueOf(milliSeconds / 1000.0).longValue();
			milliSeconds %= 1000;
			long lMinutes = lSeconds / 60;
			lSeconds %= 60;
			long lHours = lMinutes / 60;
			lMinutes %= 60;

			if (lMinutes == 0 && lHours == 0)
				time = String.format("%02d:%03d", lSeconds, Double.valueOf(milliSeconds).intValue()); //$NON-NLS-1$
			else if (lHours == 0)
				time = String.format("%02d:%02d:%03d", lMinutes, lSeconds, Double.valueOf(milliSeconds).intValue()); //$NON-NLS-1$
			else
				time = String.format("%02d:%02d:%02d:%03d", lHours, lMinutes, lSeconds, Double.valueOf(milliSeconds).intValue()); //$NON-NLS-1$
		}
		return time;
	}

}
