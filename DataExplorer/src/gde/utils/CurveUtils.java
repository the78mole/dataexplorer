package osde.utils;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;

import osde.data.Record;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;

/**
 * @author Winfried Bruegmann
 * this class contains utilities to draw curves and vertical scales
 */
public class CurveUtils {
	private static Logger				log			= Logger.getLogger(CurveUtils.class.getClass().getName());
	private static final String	lineSep	= System.getProperty("line.separator");

	/**
	 * draws the data as graph using gives rectangle for display
	 * all data point are multiplied with factor 1000 to avoid rounding errors for values below 1.0 (0.5 -> 0)
	 * @param evt
	 * @param x0
	 * @param y0
	 * @param width
	 * @param height
	 * @param scaleWidthSpace
	 */
	public static void draw(Record record, GC gc, int x0, int y0, int width, int height, int scaleWidthSpace) {
		final IDevice device = record.getDevice(); // defines the link to a device where values may corrected

		if (log.isLoggable(Level.FINEST)) log.finest("x0=" + x0 + " y0=" + y0 + " width=" + width + " height=" + height + " horizontalSpace=" + scaleWidthSpace);
		if (record.isEmpty() && !record.isDisplayable()) return; // nothing to display
		boolean isCompareSet = record.getParent().isCompareSet();
		String recordName = isCompareSet ? record.getKeyName() : record.getName();
		log.fine("drawing record =" + recordName + " isCompareSet = " + isCompareSet);

		//Draw the curve
		//(yMaxValue - yMinValue) defines the area to be used for the curve
		double yMaxValue = record.getMaxValue() / 1000.0;
		double yMinValue = record.getMinValue() / 1000.0;
		if (log.isLoggable(Level.FINE)) log.fine("unmodified yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);

		// yMinValueDisplay and yMaxValueDisplay used for scales and adapted values device and measure unit dependent
		double yMinValueDisplay = yMinValue, yMaxValueDisplay = yMaxValue;

		if (yMaxValue == yMinValue) {
			yMaxValue = new Double(yMaxValue + 1).intValue();
			yMinValue = new Double(yMinValue - 1).intValue();
		}
		boolean isRaw = record.getParent().isRaw();
		if (record.isStartEndDefined()) {
			yMinValueDisplay = record.getDefinedMinValue();
			if (isRaw) yMinValue = device.reverseTranslateValue(recordName, yMinValueDisplay);

			yMaxValueDisplay = record.getDefinedMaxValue();
			if (isRaw) yMaxValue = device.reverseTranslateValue(recordName, yMaxValueDisplay);

			if (log.isLoggable(Level.FINE)) log.fine("defined yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);
		}
		else {
			// TODO exclude imported data where values don't need correction
			if (device != null && isRaw) { // adapt to device specific range
				yMinValueDisplay = device.translateValue(recordName, yMinValue);
				yMaxValueDisplay = device.translateValue(recordName, yMaxValue);
			}

			if (record.isRoundOut() || yMaxValue == yMinValue) { // equal value disturbs the scaling alogorithm
				if (yMinValueDisplay < 0)
					yMinValueDisplay = yMinValueDisplay > -10 ? (int) (yMinValueDisplay - 1) : yMinValueDisplay - (10 + (yMinValueDisplay % 10));
				else
					yMinValueDisplay = yMinValueDisplay < 10 ? (int) (yMinValueDisplay - 1) : yMinValueDisplay - (yMinValueDisplay % 10);
				if (isRaw) yMinValue = device.reverseTranslateValue(recordName, yMinValueDisplay);

				if (yMaxValueDisplay < 0)
					yMaxValueDisplay = yMaxValueDisplay < -10 ? (int) (yMaxValueDisplay + 1) : yMaxValueDisplay + 10 - (yMaxValueDisplay % 10);
				else
					yMaxValueDisplay = yMaxValueDisplay < 10 ? (int) (yMaxValueDisplay + 1) : yMaxValueDisplay + 10 - (yMaxValueDisplay % 10);

				if (isRaw) yMaxValue = device.reverseTranslateValue(recordName, yMaxValueDisplay);

				if (log.isLoggable(Level.FINE)) log.fine("rounded yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);
			}
			if (record.isStartpointZero()) {
				yMinValue = yMinValueDisplay = 0;
				if (log.isLoggable(Level.FINE)) log.fine("start 0 yMinValue=" + yMinValue + "; yMaxValue=" + yMaxValue);
			}
		}
		record.setMinDisplayValue(yMinValueDisplay);
		record.setMaxDisplayValue(yMaxValueDisplay);
		String graphText = recordName.split("_")[0] + "   " + record.getSymbol() + "   [" + device.getDataUnit(recordName) + "]";

		// adapt number space calculation to real displayed max number
		//Point pt = gc.textExtent(df.format(yMaxValueDisplay));
		//log.fine(df.format(yMaxValueDisplay) + " gc.textExtent = " + pt.toString());
		Point pt = gc.textExtent("000,00");
		int ticklength = 5;
		int gap = 10;
		int miniticks = 3;

		// prepare axis position
		boolean isPositionLeft = record.isPositionLeft();
		int positionNumber = isCompareSet ? 0 : record.getParent().getAxisPosition(recordName, isPositionLeft);
		log.fine(recordName + " positionNumber = " + positionNumber);
		DecimalFormat df = record.getDecimalFormat();
		gc.setForeground(record.getColor()); // draw the main scale line in same color as the curve
		if (isPositionLeft) {
			int xPos = x0 - positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0, xPos, y0 - height); //xPos = x0
			if (log.isLoggable(Level.FINE)) log.fine("y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax
			gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			GraphicsUtils.drawVerticalTickMarks(gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, df);
			if (log.isLoggable(Level.FINEST)) log.finest("drawText x = " + (int) (xPos - pt.y - 15)); //xPosition Text Spannung []
			GraphicsUtils.drawText(graphText, (int) (xPos - pt.x - pt.y - 15), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
		else {
			int xPos = x0 + width + positionNumber * scaleWidthSpace;
			gc.drawLine(xPos, y0, xPos, y0 - height); //yMax
			gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			if (log.isLoggable(Level.FINEST)) log.finest("y-Achse = " + xPos + ", " + y0 + ", " + xPos + ", " + (y0 - height)); //yMax
			GraphicsUtils.drawVerticalTickMarks(gc, xPos, y0, height, yMinValueDisplay, yMaxValueDisplay, ticklength, miniticks, gap, isPositionLeft, df);
			GraphicsUtils.drawText(graphText, (int) (xPos + pt.x + 15), y0 / 2 + (y0 - height), gc, SWT.UP);
		}
		// draw the curve
		gc.setForeground(record.getColor());
		gc.setLineWidth(record.getLineWidth());
		gc.setLineStyle(record.getLineStyle());
		gc.setClipping(x0, y0 - height, width, height);
		Integer[] intRecord = record.get();
		int intRecordSize = intRecord.length;

		int i = 0;
		int timeStep = record.getTimeStep_ms();
		double adaptXMaxValue = isCompareSet ? (1.0 * (intRecordSize - 1) * record.getParent().getMaxSize() / intRecordSize * timeStep) : (1.0 * (intRecordSize - 1) * timeStep);
		double factorX = (1.0 * width) / adaptXMaxValue;
		double factorY = (1.0 * height) / (yMaxValue - yMinValue);
		if (log.isLoggable(Level.FINER)) log.finer(String.format("factorX = %.3f factorY = %.3f (yMaxValue - yMinValue) = %.3f", factorX, factorY, (yMaxValue - yMinValue)));
		StringBuffer sb = new StringBuffer();
		Point newPoint, oldPoint;
		oldPoint = new Point(x0, (int) (y0 - ((record.get(0) / 1000.0) - yMinValue) * factorY));
		// scale existing points to draw area and connect the point
		if (intRecord != null && record.size() > 1) {
			// calculate xScale for curves with much to many data points
			int xScale = 1;
			if (intRecordSize > (width << 1)) {
				xScale = intRecordSize / (width << 1);
				factorX = factorX * xScale;
			}
			for (int j = 0; j < intRecordSize; j = j + xScale) {
				int intValue = intRecord[j];
				int pointX = new Double((x0 + (timeStep * i++) * factorX)).intValue();

				double deltaY = (intValue / 1000.0) - yMinValue;
				int pointY = new Double(y0 - (deltaY * factorY)).intValue();
				if (log.isLoggable(Level.FINEST)) log.finest("(intValue / 1000.0) = " + (intValue / 1000.0) + " yMinValue = " + yMinValue + " factorY = " + factorY);

				if (log.isLoggable(Level.FINEST)) sb.append(lineSep).append("pointX = " + pointX + " -  pointY = " + pointY);

				newPoint = new Point(pointX, pointY);
				gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
				//				else {
				//					if (newPoint.x != oldPoint.x || newPoint.y != oldPoint.y) {
				//						if (log.isLoggable(Level.FINE)) sb.append(lineSep).append("newPoint != oldPoint  ----------------------------------------");
				//						gc.drawLine(veryOldPoint.x, veryOldPoint.y, oldPoint.x, oldPoint.y);
				//						gc.drawLine(oldPoint.x, oldPoint.y, newPoint.x, newPoint.y);
				//						veryOldPoint = newPoint;
				//					}
				oldPoint = newPoint;
			}
		}
		if (log.isLoggable(Level.FINE)) log.fine(sb.toString());
	}
}
