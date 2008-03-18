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

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
	private Logger					log	= Logger.getLogger(this.getClass().getName());

	private Canvas					tacho;
	private CLabel					textDigitalLabel;
	private final int				textHeight = 30;

	private final Channels	channels;
	private final String		recordKey;
	private final IDevice		device;

	/**
	 * 
	 */
	public AnalogDisplay(Composite analogWindow, String recordKey, IDevice device) {
		super(analogWindow, SWT.BORDER);
		FillLayout AnalogDisplayLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
		GridData analogDisplayLData = new GridData();
		analogDisplayLData.grabExcessVerticalSpace = true;
		analogDisplayLData.grabExcessHorizontalSpace = true;
		analogDisplayLData.verticalAlignment = GridData.FILL;
		analogDisplayLData.horizontalAlignment = GridData.FILL;
		this.setLayoutData(analogDisplayLData);
		this.setLayout(AnalogDisplayLayout);
		this.recordKey = recordKey;
		this.device = device;
		this.channels = Channels.getInstance();
	}

	public void create() {
		tacho = new Canvas(this, SWT.NONE);
		tacho.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		tacho.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				tachoPaintControl(evt);
			}
		});
		textDigitalLabel = new CLabel(tacho, SWT.CENTER);
		textDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 14, 1, false, false));
		textDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		textDigitalLabel.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
		textDigitalLabel.setBounds(0, 0, tacho.getSize().x, textHeight);
	}

	private void tachoPaintControl(PaintEvent evt) {
		log.finest("tacho.paintControl, event=" + evt);
		Channel activeChannel = channels.getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			final String channelConfigKey = activeRecordSet.getChannelName();
			if (activeRecordSet != null) {
				Record record = activeRecordSet.getRecord(recordKey);
				log.fine("record name = " + record.getName());
				// Get the canvas and its dimensions
				Canvas canvas = (Canvas) evt.widget;
				int width = canvas.getSize().x;
				int height = canvas.getSize().y;
				log.fine("canvas size = " + width + " x " + height);
				
				String recordText = record.getName() + " [ " + record.getUnit() + " ]";
				textDigitalLabel.setSize(width, textHeight);
				textDigitalLabel.setText(recordText);

				double actualValue = device.translateValue(channelConfigKey, recordKey, new Double(record.get(record.size() - 1) / 1000.0));
				double minValue = device.translateValue(channelConfigKey, recordKey, record.getMinValue()/1000.0);
				double maxValue = device.translateValue(channelConfigKey, recordKey, record.getMaxValue()/1000.0);
				log.fine(recordKey + " minValue = " + record.getMinValue() + " maxValue = " + record.getMaxValue());
				log.fine(recordKey + " minValue = " + minValue + " maxValue = " + maxValue);
				double[] roundValues = CurveUtils.round(minValue, maxValue);
				minValue = roundValues[0]; 	// min
				maxValue = roundValues[1];	// max
				log.fine(String.format("value = %3.2f; min = %3.2f; max = %3.2f", actualValue, minValue, maxValue));

				int centerX = width / 2;
				int centerY = (int) (height * 0.75);

				int radiusW = (int) (width / 2 * 0.80);
				int radiusH = (int) (height / 2 * 0.90);
				int radius = radiusW < radiusH ? radiusW : radiusH;
				int angleStart = -20;
				int angleDelta = 220;
				evt.gc.setForeground(record.getColor());
				evt.gc.setLineWidth(4);
				evt.gc.drawArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius, angleStart, angleDelta);
				evt.gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);

				int numberTicks = 10; //new Double(maxValue - minValue).intValue();
				double deltaValue = (maxValue - minValue) / numberTicks;
				double angleSteps = angleDelta * 1.0 / numberTicks;
				int tickRadius = radius + 2;
				int dxr, dxtick, dyr, dytick, dxtext, dytext;
				evt.gc.setLineWidth(2);
				for (int i = 0; i <= numberTicks; ++i) {
					double angle = angleStart + i * angleSteps; // -20, 0, 20, 40, ...
					dxr = new Double(tickRadius * Math.cos(angle * Math.PI / 180)).intValue();
					dyr = new Double(tickRadius * Math.sin(angle * Math.PI / 180)).intValue();
					dxtick = new Double((tickRadius + 10) * Math.cos(angle * Math.PI / 180)).intValue();
					dytick = new Double((tickRadius + 10) * Math.sin(angle * Math.PI / 180)).intValue();
					evt.gc.drawLine(centerX - dxtick, centerY - dytick, centerX - dxr, centerY - dyr);

					dxtext = new Double((radius + 30) * Math.cos(angle * Math.PI / 180)).intValue();
					dytext = new Double((radius + 30) * Math.sin(angle * Math.PI / 180)).intValue();
					String valueText = record.getDecimalFormat().format(minValue + (i * deltaValue));
					GraphicsUtils.drawText(valueText, centerX - dxtext, centerY - dytext, evt.gc, SWT.HORIZONTAL);
				}

				// draw tacho needle
				int needleRadius = radius - 5;
				int innerRadius = (int) (width / 2 * 0.1) + 3;
				double angle = angleStart + (actualValue - minValue) / (maxValue - minValue) * angleDelta;
				log.finer("angle = " + angle + " actualValue = " + actualValue);

				int posXo = new Double(centerX - (needleRadius * Math.cos(angle * Math.PI / 180))).intValue();
				int posYo = new Double(centerY - (needleRadius * Math.sin(angle * Math.PI / 180))).intValue();
				int posXi = new Double(centerX - (innerRadius * Math.cos(angle * Math.PI / 180))).intValue();
				int posYi = new Double(centerY - (innerRadius * Math.sin(angle * Math.PI / 180))).intValue();
				int posX1 = new Double(centerX - ((needleRadius - 30) * Math.cos((angle - 3) * Math.PI / 180))).intValue();
				int posY1 = new Double(centerY - ((needleRadius - 30) * Math.sin((angle - 3) * Math.PI / 180))).intValue();
				int posX2 = new Double(centerX - ((needleRadius - 30) * Math.cos((angle + 3) * Math.PI / 180))).intValue();
				int posY2 = new Double(centerY - ((needleRadius - 30) * Math.sin((angle + 3) * Math.PI / 180))).intValue();
				int[] needle = { posX1, posY1, posXo, posYo, posX2, posY2, posXi, posYi };
				evt.gc.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
				evt.gc.setLineWidth(1);
				evt.gc.fillPolygon(needle);

				// center knob
				evt.gc.setBackground(OpenSerialDataExplorer.COLOR_GREY);
				radius = (int) (width / 2 * 0.1);
				evt.gc.fillArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius, 0, 360);
				evt.gc.setBackground(OpenSerialDataExplorer.COLOR_BLACK);
				radius = (int) (width / 20 * 0.1);
				evt.gc.fillArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius, 0, 360);
			}
		}
	}
}
