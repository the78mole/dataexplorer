/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.utils;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import osde.data.RecordSet;
import osde.ui.SWTResourceManager;

/**
 * Utility class to draw time line with tick marks and numbers
 * @author Winfried Brügmann
 */
public class TimeLine {
	private final static Logger	log									= Logger.getLogger(TimeLine.class.getName());

	public final static int			TIME_LINE_MSEC			= 0;
	public final static int			TIME_LINE_SEC				= 1;
	public final static int			TIME_LINE_SEC_MIN		= 2;
	public final static int			TIME_LINE_MIN				= 3;
	public final static int			TIME_LINE_MIN_HRS		= 4;
	public final static int			TIME_LINE_HRS				= 5;

	private static String				timeLineText				= "Zeit   t   [min]";
	private boolean							isTimeLinePrepared	= false;

	/**
	 * calculates the maximum time number to be displayed and the scale number factor
	 * @return maxTimeNumber, scale number factor
	 */
	public synchronized int[] getScaleMaxTimeNumber(RecordSet recordSet) {
		int factor = 10; // for the most cases (make factor 10 based to enable 0.5 by factor 5)
		int format = TIME_LINE_MSEC; // the time format type 
		
		int numberOfPoints = recordSet.getSize();
		if (log.isLoggable(Level.FINE)) log.fine("numberOfPoints = " + numberOfPoints + "; timeStep_ms = " + recordSet.getTimeStep_ms());

		long totalTime_msec = recordSet.getTimeStep_ms() * (numberOfPoints - 1) / 100;
		long totalTime_sec = recordSet.getTimeStep_ms() * (numberOfPoints - 1) / 1000;
		long totalTime_min = TimeUnit.MINUTES.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_std = TimeUnit.HOURS.convert(totalTime_sec, TimeUnit.SECONDS);
		if (log.isLoggable(Level.FINE)) log.fine("totalTime_std = " + totalTime_std + "; totalTime_min = " + totalTime_min + "; totalTime_sec = " + totalTime_sec + "; totalTime_ms = " + totalTime_msec);
		int maxTimeNumber; // the biggest number in the scale to be displayed

		if (totalTime_std > 5) {
			maxTimeNumber = (int) totalTime_std;
			timeLineText = "Zeit   t   [Std]";
			format = TIME_LINE_HRS;
		}
		else if (totalTime_min > 60) {
			maxTimeNumber = (int) totalTime_min;
			timeLineText = "Zeit   t   [min, Std]";
			format = TIME_LINE_MIN_HRS;
		}
		else if (totalTime_min > 10) {
			maxTimeNumber = (int) totalTime_min;
			timeLineText = "Zeit   t   [min]";
			format = TIME_LINE_MIN;
		}
		else if (totalTime_sec > 60) {
			maxTimeNumber = (int) totalTime_sec;
			timeLineText = "Zeit   t   [sek, min]";
			format = TIME_LINE_SEC_MIN;
		}
		else if (totalTime_sec > 10) {
			maxTimeNumber = (int) totalTime_sec;
			timeLineText = "Zeit   t   [sec]";
			format = TIME_LINE_SEC;
		}
		else if (totalTime_sec > 1) {
			maxTimeNumber = (int) totalTime_msec;
			timeLineText = "Zeit   t   [sec]";
			factor = 100;
			format = TIME_LINE_SEC;
		}
		else {
			maxTimeNumber = (int) totalTime_msec;
			timeLineText = "Zeit   t   [msec]";
			factor = 1000;
			format = TIME_LINE_MSEC;
		}
		if (log.isLoggable(Level.FINE)) log.fine(timeLineText + "  " + maxTimeNumber);

		isTimeLinePrepared = true;

		if (log.isLoggable(Level.FINE)) log.fine("timeLineText = " + timeLineText + " maxTimeNumber = " + maxTimeNumber + " factor = " + factor);
		return new int[] { maxTimeNumber, factor, format };
	}

	/**
	 * draws the time line - requires to call preparation steps
	 * - x0, y0 defines the start point of the scale, y0, width the nd point
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startTimeValue  the time value where the scale should start to count
	 * @param endTimeValue depends on the time gap and the unit calculated from the gap
	 * @param scaleFactor - factor to multiply scale numbers
	 * @param color
	 * @return position, where the time line text is drawn
	 */
	public synchronized void drawTimeLine(GC gc, int x0, int y0, int width, int startTimeValue, int endTimeValue, int scaleFactor, Color color) {
		if (isTimeLinePrepared == false) {
			log.log(Level.WARNING, "isTimeLinePrepared == false -> getScaleMaxTimeNumber(RecordSet recordSet) needs to be called first");
			return ;
		}

		// set the line color and draw a horizontal time line
		gc.setLineWidth(1);
		gc.setLineStyle(SWT.LINE_SOLID);
		gc.setForeground(color);
		gc.drawLine(x0, y0, x0 + width, y0);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("time line - x0=%d y0=%d - width=%d - maxNumber=%d - scaleFactor=%d", x0, y0, width, endTimeValue, scaleFactor));

		Point pt = gc.textExtent(timeLineText);
		int ticklength = pt.y / 2;
		int gap = pt.y / 3;
		int miniTicks = 3;
		
		drawHorizontalTickMarks(gc, x0, y0, width, startTimeValue, endTimeValue, scaleFactor, ticklength, miniTicks, gap);

		// draw the scale description centered
		GraphicsUtils.drawText(timeLineText, (int) (x0 + width / 2), y0 + ticklength + pt.y * 2, gc, SWT.HORIZONTAL);
	}

	/**
	 * draws tick marks to a scale in horizontal direction
	 * @param gc graphics context
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startTimeValue the time value where the scale should start to count
	 * @param endTimeValue the number where the time scale should end, endTimeValue - startTimeValue -> number of ticks drawn
	 * @param ticklength of the main ticks
	 * @param miniticks number of mini ticks drawn between the main ticks
	 * @param gap distance between ticks and the number scale
	 */
	private void drawHorizontalTickMarks(GC gc, int x0, int y0, int width, int startTimeValue, int endTimeValue, int scaleFactor, int ticklength, int miniticks, int gap) {
		// adapt x0 and width, measurement scales are outside the curve draw area
		x0 = x0 - 1;
		width = width + 1;
		double numberTicks;
		//int offset = (startTimeValue != 0) ? 10 - startTimeValue % 10 : 0;
		int timeDelta = endTimeValue - startTimeValue;
		if (log.isLoggable(Level.FINE)) log.fine("timeDelta = " + timeDelta + " startTime = " + startTimeValue + " endTime = " + endTimeValue);
		
		if (timeDelta > 0) {
			// calculate a scale factor, a big time difference would have to much ticks
			if (timeDelta >= 0 && timeDelta < 100 && scaleFactor == 1000) {
				numberTicks = timeDelta; // every 1'th units one tick
				scaleFactor = scaleFactor / 10;
				if (log.isLoggable(Level.FINER)) log.fine("0 numberTicks = " + numberTicks + " startTimeValue = " + startTimeValue + " endTimeValue = " + endTimeValue);
			}
			else if (timeDelta >= 10 && timeDelta < 60 && scaleFactor == 10) {
				numberTicks = timeDelta / 5.0; // every 5 th units one tick
				scaleFactor = scaleFactor * 2;
				if (log.isLoggable(Level.FINER)) log.fine("1 numberTicks = " + numberTicks + " startTimeValue = " + startTimeValue + " endTimeValue = " + endTimeValue);
			}
			else {
				numberTicks = timeDelta / 10.0; // every 10th units one tick
				if (log.isLoggable(Level.FINER)) log.fine("2 numberTicks = " + numberTicks + " startTimeValue = " + startTimeValue + " endTimeValue = " + endTimeValue);
			}

			double deltaTick = 1.0 * width / numberTicks;
			miniticks++;

			// calculate the space required to draw the time values
			Point pt = gc.textExtent("00");

			for (int i = 0; i <= numberTicks; i++) { // <= end of time scale tick 

				//draw the main scale ticks, length = 5 and gap to scale = 2
				double xTickPosition = x0 + i * deltaTick;
				int intXTickPosition = new Double(xTickPosition).intValue();
				gc.drawLine(intXTickPosition, y0, intXTickPosition, y0 + ticklength);

				//draw the sub ticks to the scale according number of miniTicks
				double deltaPosMini = deltaTick / miniticks;
				for (int j = 1; j < miniticks && i < numberTicks; j++) {
					double xMiniTickPos = (xTickPosition + j * deltaPosMini);
					int intMiniTickPos = new Double(xMiniTickPos).intValue();
					if (log.isLoggable(Level.FINEST)) log.finest("intXTickPosition=" + intXTickPosition + ", width=" + width);
					if (intMiniTickPos < (x0 + width)) {
						gc.drawLine(intMiniTickPos, y0, intMiniTickPos, y0 + ticklength / 2);
					}
				}
				//draw values to the scale	
				int timeValue = i * 100 / scaleFactor;

				// prepare to make every minute or hour to bold
				boolean isMod60 = (timeValue % 60) == 0;
				int timeValue60 = isMod60 ? timeValue / 60 : timeValue % 60; // minute, hour
				if (log.isLoggable(Level.FINER)) log.finer("timeValue = " + timeValue + ", timeValue60 = " + timeValue60);

				String numberStr = new Integer(timeValue60).toString();
				FontData[] fd = gc.getFont().getFontData();
				if (isMod60 && timeValue != 0) {
					fd[0].setStyle(SWT.BOLD);
					gc.setFont(SWTResourceManager.getFont(fd[0]));
				}

				GraphicsUtils.drawText(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
				if (isMod60 && timeValue != 0) { // reset font style
					fd[0].setStyle(SWT.NORMAL);
					gc.setFont(SWTResourceManager.getFont(fd[0]));
				}
			}
		}
	}

	/**
	 * converts a given time in m_sec into time format used for time scale 
	 * @return converted time value
	 */
	public static int convertTimeInFormatNumber(long time_ms, int timeFormat) {
		long time_sec = time_ms / 1000;
		long time_min = TimeUnit.MINUTES.convert(time_sec, TimeUnit.SECONDS);
		long time_std = TimeUnit.HOURS.convert(time_sec, TimeUnit.SECONDS);
		if (log.isLoggable(Level.FINE)) log.fine("time_std = " + time_std + "; time_min = " + time_min + "; time_sec = " + time_sec + "; time_ms = " + time_ms);
		int result;

		switch (timeFormat) {
		case TIME_LINE_HRS:		
			result = new Long(time_std).intValue();
			break;
		case TIME_LINE_MIN_HRS:	
		case TIME_LINE_MIN:		
			result = new Long(time_min).intValue();
			break;
		case TIME_LINE_SEC_MIN:			
		case TIME_LINE_SEC:		
			result = new Long(time_sec).intValue();
			break;
		default: // TIME_LINE_MSEC
			result = new Long(time_ms).intValue();
			break;
		}
		return result;
	}

	/**
	 * get the formatted time of time value in m_sec, if hours are 0 or minutes are 0 the string will be cut of
	 * @param millis
	 * @return string of time value in simple date format HH:mm:ss:SSS
	 */
	public static String getFomatedTime(int milliSeconds) {
		String time = "0";
		if (milliSeconds >= 0)
		{
			long	lSeconds = milliSeconds / 1000;
			milliSeconds %= 1000;
			long	lMinutes = lSeconds / 60;
			lSeconds %= 60;
			long	lHours = lMinutes / 60;
			lMinutes %= 60;
			
			if (lMinutes == 0 && lHours == 0)
				time = String.format("%02d:%03d [ss:SSS]", lSeconds, milliSeconds);
			else if (lHours == 0)
				time = String.format("%02d:%02d:%03d [mm:ss:SSS]", lMinutes, lSeconds, milliSeconds);
			else
				time = String.format("%02d:%02d:%02d:%03d [HH:mm:ss:SSS]", lHours, lMinutes, lSeconds, milliSeconds);
		}
		return time;
	}

}
