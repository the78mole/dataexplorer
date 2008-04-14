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
package osde.ui.tab;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CurveUtils;
import osde.utils.GraphicsUtils;

/**
 * Child display class displaying analog active measurements
 * @author Winfried Brügmann
 */
public class AnalogDisplay extends Composite {
	final static Logger			log								= Logger.getLogger(AnalogDisplay.class.getName());

	private Canvas					tacho;
	private CLabel					textDigitalLabel;
	private final int				textHeight				= 30;

	private Image						tachoImage;
	private GC							tachoImageGC;
	private Rectangle				tachoImageBounds	= new Rectangle(0, 0, 0, 0);
	private boolean					isChanged					= false;

	private int							width							= 0;
	private int							height						= 0;
	private int							centerX;
	private int							centerY;
	private int							radius;
	private int							angleStart;
	private int							angleDelta;

	private double					actualValue				= 0.0;
	private double					minValue					= 0.0;
	private double					maxValue					= 1.0;

	private final Channels	channels;
	private final String		recordKey;
	private final IDevice		device;

	/**
	 * 
	 */
	public AnalogDisplay(Composite analogWindow, String currentRecordKey, IDevice currentDevice) {
		super(analogWindow, SWT.BORDER);
		FillLayout AnalogDisplayLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
		GridData analogDisplayLData = new GridData();
		analogDisplayLData.grabExcessVerticalSpace = true;
		analogDisplayLData.grabExcessHorizontalSpace = true;
		analogDisplayLData.verticalAlignment = GridData.FILL;
		analogDisplayLData.horizontalAlignment = GridData.FILL;
		this.setLayoutData(analogDisplayLData);
		this.setLayout(AnalogDisplayLayout);
		this.recordKey = currentRecordKey;
		this.device = currentDevice;
		this.channels = Channels.getInstance();
	}

	public void create() {
		this.tacho = new Canvas(this, SWT.NONE);
		this.tacho.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				tachoPaintControl(evt);
			}
		});
		this.textDigitalLabel = new CLabel(this.tacho, SWT.CENTER);
		this.textDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 14, 1, false, false));
		this.textDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		this.textDigitalLabel.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
		this.textDigitalLabel.setBounds(0, 0, this.tacho.getSize().x, this.textHeight);
	}

	void tachoPaintControl(PaintEvent evt) {
		if (log.isLoggable(Level.FINEST)) log.finest("tacho.paintControl, event=" + evt);
		Channel activeChannel = this.channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				Record record = activeRecordSet.getRecord(this.recordKey);
				if (log.isLoggable(Level.FINER)) log.finer("record name = " + record.getName());

				// Get the canvas and its dimensions to check if size changed
				Rectangle tmpTachoImageBounds = ((Canvas) evt.widget).getBounds();
				if (this.tachoImageBounds.width != tmpTachoImageBounds.width || this.tachoImageBounds.height != tmpTachoImageBounds.height) {
					this.tachoImageBounds = tmpTachoImageBounds;
					setChanged(true);
				}

				// get min max values and check if this has been changed
				double tmpMinValue = this.device.translateValue(record, record.getMinValue() / 1000.0);
				double tmpMaxValue = this.device.translateValue(record, record.getMaxValue() / 1000.0);
				double[] roundValues = CurveUtils.round(tmpMinValue, tmpMaxValue);
				tmpMinValue = roundValues[0]; // min
				tmpMaxValue = roundValues[1]; // max
				if (tmpMinValue != this.minValue || tmpMaxValue != this.maxValue) {
					this.minValue = tmpMinValue;
					this.maxValue = tmpMaxValue;
					setChanged(true);
				}

				// draw new tacho only if some thing has changed
				if (isChanged()) {
					if (log.isLoggable(Level.FINE)) log.fine("tacho redaw required for " + this.recordKey);
					this.width = this.tachoImageBounds.width;
					this.height = this.tachoImageBounds.height;
					if (log.isLoggable(Level.FINER)) log.finer("canvas size = " + this.width + " x " + this.height);
					// get the image and prepare GC
					this.tachoImage = SWTResourceManager.getImage(this.width, this.height, this.recordKey);
					this.tachoImageGC = SWTResourceManager.getGC(this.tachoImage);
					//clear image with background color
					this.tachoImageGC.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
					this.tachoImageGC.fillRectangle(0, 0, this.width, this.height);
					String recordText = record.getName() + " [ " + record.getUnit() + " ]";
					this.textDigitalLabel.setSize(this.width, this.textHeight);
					this.textDigitalLabel.setText(recordText);
					this.centerX = this.width / 2;
					this.centerY = (int) (this.height * 0.75);
					int radiusW = (int) (this.width / 2 * 0.80);
					int radiusH = (int) (this.height / 2 * 0.90);
					this.radius = radiusW < radiusH ? radiusW : radiusH;
					this.angleStart = -20;
					this.angleDelta = 220;
					this.tachoImageGC.setForeground(record.getColor());
					this.tachoImageGC.setLineWidth(4);
					this.tachoImageGC.drawArc(this.centerX - this.radius, this.centerY - this.radius, 2 * this.radius, 2 * this.radius, this.angleStart, this.angleDelta);
					this.tachoImageGC.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
					int numberTicks = 10; //new Double(maxValue - minValue).intValue();
					double deltaValue = (this.maxValue - this.minValue) / numberTicks;
					double angleSteps = this.angleDelta * 1.0 / numberTicks;
					int tickRadius = this.radius + 2;
					int dxr, dxtick, dyr, dytick, dxtext, dytext;
					this.tachoImageGC.setLineWidth(2);
					for (int i = 0; i <= numberTicks; ++i) {
						double angle = this.angleStart + i * angleSteps; // -20, 0, 20, 40, ...
						dxr = new Double(tickRadius * Math.cos(angle * Math.PI / 180)).intValue();
						dyr = new Double(tickRadius * Math.sin(angle * Math.PI / 180)).intValue();
						dxtick = new Double((tickRadius + 10) * Math.cos(angle * Math.PI / 180)).intValue();
						dytick = new Double((tickRadius + 10) * Math.sin(angle * Math.PI / 180)).intValue();
						this.tachoImageGC.drawLine(this.centerX - dxtick, this.centerY - dytick, this.centerX - dxr, this.centerY - dyr);

						dxtext = new Double((this.radius + 30) * Math.cos(angle * Math.PI / 180)).intValue();
						dytext = new Double((this.radius + 30) * Math.sin(angle * Math.PI / 180)).intValue();
						String valueText = record.getDecimalFormat().format(this.minValue + (i * deltaValue));
						GraphicsUtils.drawText(valueText, this.centerX - dxtext, this.centerY - dytext, this.tachoImageGC, SWT.HORIZONTAL);
					}
					// center knob
					this.tachoImageGC.setBackground(OpenSerialDataExplorer.COLOR_GREY);
					int knobRradius = (int) (this.radius * 0.1);
					this.tachoImageGC.fillArc(this.centerX - knobRradius, this.centerY - knobRradius, 2 * knobRradius, 2 * knobRradius, 0, 360);
					this.tachoImageGC.setBackground(OpenSerialDataExplorer.COLOR_BLACK);
					knobRradius = (int) (this.radius / 10 * 0.1);
					this.tachoImageGC.fillArc(this.centerX - knobRradius, this.centerY - knobRradius, 2 * knobRradius, 2 * knobRradius, 0, 360);
				}
				evt.gc.drawImage(this.tachoImage, 0, 0, this.width, this.height, 0, 0, this.width, this.height);

				double tmpActualValue = this.device.translateValue(record, new Double(record.get(record.size() - 1) / 1000.0));
				if (log.isLoggable(Level.FINE)) log.fine(String.format("value = %3.2f; min = %3.2f; max = %3.2f", this.actualValue, this.minValue, this.maxValue));
				//drawTachoNeedle(evt.gc, actualValue, OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
				this.actualValue = tmpActualValue;
				drawTachoNeedle(evt.gc, this.actualValue, OpenSerialDataExplorer.COLOR_BLACK);

				setChanged(false);
			}
		}
	}

	/**
	 * draw tacho needle in specified color, before new needle is drawn is must be erased using the background color
	 * @param gc
	 * @param value
	 * @param color
	 */
	private void drawTachoNeedle(GC gc, double value, Color color) {
		int needleRadius = this.radius - 5;
		int innerRadius = (int) (this.radius * 0.1) + 3;
		double angle = this.angleStart + (value - this.minValue) / (this.maxValue - this.minValue) * this.angleDelta;
		log.finer("angle = " + angle + " actualValue = " + value);

		int posXo = new Double(this.centerX - (needleRadius * Math.cos(angle * Math.PI / 180))).intValue();
		int posYo = new Double(this.centerY - (needleRadius * Math.sin(angle * Math.PI / 180))).intValue();
		int posXi = new Double(this.centerX - (innerRadius * Math.cos(angle * Math.PI / 180))).intValue();
		int posYi = new Double(this.centerY - (innerRadius * Math.sin(angle * Math.PI / 180))).intValue();
		int posX1 = new Double(this.centerX - ((needleRadius - 30) * Math.cos((angle - 3) * Math.PI / 180))).intValue();
		int posY1 = new Double(this.centerY - ((needleRadius - 30) * Math.sin((angle - 3) * Math.PI / 180))).intValue();
		int posX2 = new Double(this.centerX - ((needleRadius - 30) * Math.cos((angle + 3) * Math.PI / 180))).intValue();
		int posY2 = new Double(this.centerY - ((needleRadius - 30) * Math.sin((angle + 3) * Math.PI / 180))).intValue();
		int[] needle = { posX1, posY1, posXo, posYo, posX2, posY2, posXi, posYi };
		gc.setBackground(color);
		gc.setLineWidth(1);
		gc.fillPolygon(needle);
	}

	/**
	 * @return the tacho
	 */
	public Canvas getTacho() {
		return this.tacho;
	}

	/**
	 * @param enabled the isChanged to set
	 */
	public void setChanged(boolean enabled) {
		this.isChanged = enabled;
	}

	/**
	 * @return the isChanged
	 */
	public boolean isChanged() {
		return this.isChanged;
	}
}
