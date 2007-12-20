/**
 * Utility class to draw time line with tick marks and numbers
 */
package osde.utils;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import osde.data.RecordSet;

/**
 * @author Winfried Bruegmann
 *	this class contains utilities to draw horizontal scales like time line 
 */
public class TimeLine {
	private Logger				log									= Logger.getLogger(this.getClass().getName());

	private static String	timeLineText				= "Zeit   t   [min]";
	private boolean				isTimeLinePrepared	= false;

	/**
	 * calculates the maximum time number to be displayed and the scale number factor
	 * @return maxTimeNumber, scale number factor
	 */
	public synchronized int[] getScaleMaxTimeNumber(RecordSet recordSet) {
		int factor = 10; // for the most cases (make factor 10 based to enable 0.5 by factor 5)
		int numberOfPoints = (recordSet.getMaxSize() == 0) ? recordSet.getRecord(recordSet.getRecordNames()[0]).size() : recordSet.getMaxSize();
		if (log.isLoggable(Level.FINE)) log.fine("numberOfPoints = " + numberOfPoints + "; timeStep_ms = " + recordSet.getTimeStep_ms());
		long totalTime_msec = recordSet.getTimeStep_ms() * numberOfPoints / 100;
		long totalTime_sec = recordSet.getTimeStep_ms() * numberOfPoints / 1000;
		long totalTime_min = TimeUnit.MINUTES.convert(totalTime_sec, TimeUnit.SECONDS);
		long totalTime_std = TimeUnit.HOURS.convert(totalTime_sec, TimeUnit.SECONDS);
		if (log.isLoggable(Level.FINE)) log.fine("totalTime_std = " + totalTime_std + "; totalTime_min = " + totalTime_min + "; totalTime_sec = " + totalTime_sec + "; totalTime_ms = " + totalTime_msec);
		int maxTimeNumber; // the biggest number in the scale to be displayed

		if (totalTime_std > 5) {
			maxTimeNumber = (int) totalTime_std;
			timeLineText = "Zeit   t   [Std]";
		}
		else if (totalTime_min > 60) {
			maxTimeNumber = (int) totalTime_min;
			timeLineText = "Zeit   t   [min, Std]";
		}
		else if (totalTime_min > 10) {
			maxTimeNumber = (int) totalTime_min;
			timeLineText = "Zeit   t   [min]";
		}
		else if (totalTime_sec > 60) {
			maxTimeNumber = (int) totalTime_sec;
			timeLineText = "Zeit   t   [sek, min]";
		}
		else if (totalTime_sec > 10) {
			maxTimeNumber = (int) totalTime_sec;
			timeLineText = "Zeit   t   [sec]";
		}
		else if (totalTime_sec > 1) {
			maxTimeNumber = (int) totalTime_msec;
			timeLineText = "Zeit   t   [sec]";
			factor = 100;
		}
		else {
			maxTimeNumber = (int) totalTime_msec;
			timeLineText = "Zeit   t   [msec]";
			factor = 1000;
		}
		if (log.isLoggable(Level.FINE)) log.fine(timeLineText + "  " + maxTimeNumber);

		isTimeLinePrepared = true;

		if (log.isLoggable(Level.FINE)) log.fine("timeLineText = " + timeLineText + " maxTimeNumber = " + maxTimeNumber + " factor = " + factor);
		return new int[] { maxTimeNumber, factor };
	}

	/**
	 * draws the time line - requires to call preparation steps
	 * @param evt
	 * @param x0
	 * @param y0
	 * @param width
	 * @param maxNumber
	 * @param factor to multiply scale numbers
	 * @param color
	 */
	public synchronized void drawTimeLine(GC gc, int x0, int y0, int width, int maxNumber, int scaleFactor, Color color) {
		if (isTimeLinePrepared == false) return;

		// Set the line color and draw a horizontal time axis
		gc.setForeground(color);
		gc.drawLine(x0, y0, x0 + width, y0);
		if (log.isLoggable(Level.FINE)) log.fine(String.format("Zeitachse - x0=%d y0=%d - width=%d - maxNumber=%d - scaleFactor=%d", x0, y0, width, maxNumber, scaleFactor));

		Point pt = gc.textExtent(timeLineText);
		int ticklength = pt.y / 2;
		int gap = pt.y / 3;
		int miniTicks = 3;
		drawHorizontalTickMarks(gc, x0, y0, width, 0, maxNumber, scaleFactor, ticklength, miniTicks, gap);

		// draw the scale description centered
		GraphicsUtils.drawText(timeLineText, (int) (x0 + width / 2), y0 + ticklength + pt.y * 2, gc, SWT.HORIZONTAL);
	}

	/**
	 * draws tick marks to a scale in horizontal direction
	 * @param evt PaintEvent
	 * @param y0 start point in y horizontal direction
	 * @param x0 start point in x vertical direction
	 * @param width in points where the ticks should be drawn
	 * @param startNumber the number where the scale should start to count
	 * @param endNumber the number where the scale should start to count, endNumber - startNumber -> number of ticks drawn
	 * @param ticklength of the main ticks
	 * @param miniticks number of mini ticks drawn between the main ticks
	 * @param gap distance between ticks and the number scale
	 */
	private void drawHorizontalTickMarks(GC gc, int x0, int y0, int width, int startNumber, int endNumber, int scaleFactor, int ticklength, int miniticks, int gap) {

		double numberTicks;
		int timeDelta = endNumber - startNumber;
		if (timeDelta >= 0 && timeDelta < 100 && scaleFactor == 1000) {
			numberTicks = timeDelta ; // every 1 th units one tick
			scaleFactor = scaleFactor/10;
			if (log.isLoggable(Level.FINE)) log.fine("0 numberTicks = " + numberTicks + " startNumber = " + startNumber + " endNumber = " + endNumber);
		}
		else if (timeDelta >= 10 && timeDelta < 30 && scaleFactor == 10) {
			numberTicks = timeDelta / 5.0; // every 5 th units one tick
			scaleFactor = scaleFactor * 2;
			if (log.isLoggable(Level.FINE)) log.fine("1 numberTicks = " + numberTicks + " startNumber = " + startNumber + " endNumber = " + endNumber);
		}
		else {
			numberTicks = timeDelta / 10.0; // every 10th units one tick
			if (log.isLoggable(Level.FINE)) log.fine("2 numberTicks = " + numberTicks + " startNumber = " + startNumber + " endNumber = " + endNumber);
		}
		double deltaTick = 1.0 * width / numberTicks;
		miniticks++;
		Point pt = gc.textExtent("000,00");
		for (int i = 0; i <= numberTicks; i++) { // <= end of time scale tick 
			//draw the main scale ticks, length = 5 and gap to scale = 2
			double xTickPosition = x0 + i * deltaTick;
			int intXTickPosition = new Double(xTickPosition).intValue();
			gc.drawLine(intXTickPosition, y0, intXTickPosition, y0 + ticklength);

			//draw the sub mini ticks to the scale according number of miniTicks
			double deltaPosMini = deltaTick / miniticks;
			for (int j = 1; j < miniticks && i < numberTicks; j++) {
				double xMiniTickPos = (xTickPosition + j * deltaPosMini);
				int intMiniTickPos = new Double(xMiniTickPos).intValue();
				if (log.isLoggable(Level.FINEST)) log.finest("intXTickPosition=" + intXTickPosition + ", width=" + width);
				if (intMiniTickPos < (x0 + width)) {
					gc.drawLine(intMiniTickPos, y0, intMiniTickPos, y0 + ticklength / 2);
				}
			}
			//draw numbers to the scale	
			int actualInt = startNumber + i * 100 / scaleFactor; // correct scaleFactor base 10
			boolean isMod60 = (actualInt % 60) == 0;
			int numberInt = isMod60 ? actualInt / 60 : actualInt % 60;  // min ; Std
			if (log.isLoggable(Level.FINER)) log.finer("actualInt = " + actualInt + ", numberInt = " + numberInt);
			String numberStr = new Integer(numberInt).toString();
			FontData[] fd = gc.getFont().getFontData();
			if (isMod60 && actualInt != 0)	fd[0].setStyle(SWT.BOLD);
			gc.setFont(new Font(gc.getDevice(), fd));
			GraphicsUtils.drawText(numberStr, intXTickPosition, y0 + ticklength + gap + pt.y / 2, gc, SWT.HORIZONTAL);
			fd[0].setStyle(SWT.NORMAL);
			gc.setFont(new Font(gc.getDevice(), fd));
		}
	}

}
