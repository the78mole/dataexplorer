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
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.GraphicsUtils;

/**
 * Child display class displaying analog active measurements
 * @author Winfried Brügmann
 */
public class AnalogDisplay extends Composite {
	private Logger							log	= Logger.getLogger(this.getClass().getName());

	private Canvas							tacho;

	private final Channels			channels;
	private final String				recordKey;
	private final IDevice	device;

	/**
	 * 
	 */
	public AnalogDisplay(Composite analogWindow, String recordKey, IDevice device) {
		super(analogWindow, SWT.NONE);
		GridLayout AnalogDisplayLayout = new GridLayout();
		AnalogDisplayLayout.makeColumnsEqualWidth = true;
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
		GridData tachoLData = new GridData();
		tachoLData.horizontalAlignment = GridData.FILL;
		tachoLData.verticalAlignment = GridData.FILL;
		tachoLData.grabExcessVerticalSpace = true;
		tachoLData.grabExcessHorizontalSpace = true;
		tacho = new Canvas(this, SWT.NONE);
		tacho.setLayoutData(tachoLData);
		tacho.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				tachoPaintControl(evt);
			}
		});
	}

	private void tachoPaintControl(PaintEvent evt) {
		log.finest("tacho.paintControl, event=" + evt);
		RecordSet activeRecordSet = channels.getActiveChannel().getActiveRecordSet();
		final String channelConfigKey = activeRecordSet.getChannelName();
		if (activeRecordSet != null) {
			Record record = activeRecordSet.getRecord(recordKey);
			log.fine("record name = " + record.getName());
			// Get the canvas and its dimensions
			Canvas canvas = (Canvas) evt.widget;
			int width = canvas.getSize().x;
			int height = canvas.getSize().y;
			log.info("canvas size = " + width + " x " + height);
			//canvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/WorkItem.gif"));

			double actualValue = device.translateValue(channelConfigKey, recordKey, new Double(record.get(record.size() - 1) / 1000.0));
			double maxValue =  device.translateValue(channelConfigKey, recordKey, new Double(record.getMaxValue()) / 1000.0);
			double minValue =  device.translateValue(channelConfigKey, recordKey, new Double(record.getMinValue()) / 1000.0);
			log.fine(String.format("value = %3.2f; min = %3.2f; max = %3.2f", actualValue, minValue, maxValue));

			// draw clipping bounding 
			//evt.gc.setClipping(10, 10, width-20, height-20);
			evt.gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			canvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
	    
	    int centerX = width / 2;
	    int centerY = (int)(height * 0.75);
	    evt.gc.drawLine(0, centerY, width, centerY);
	    evt.gc.drawLine(centerX, 0, centerX, height);

	    int radiusW = (int)(width / 2 * 0.85);
	    int radiusH = (int)(height / 2 * 0.90);
	    int radius = radiusW < radiusH ? radiusW : radiusH;
	    int angleStart = -20;
	    int angleDelta = 220;
	    evt.gc.drawArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius, angleStart, angleDelta);

	    int minV = 0;
	    int maxV = 15;
	    int numberTicks = maxV - minV;
	    double angleSteps = angleDelta * 1.0 / numberTicks; 
	    int dxr, dxtick, dyr, dytick, dxtext, dytext;
	    for (int i = 0; i <= numberTicks; ++i) {
	    	double angle = angleStart + i * angleSteps;  // -20, 0, 20, 40, ...
		    log.info("angle = " + angle);
		    dxr = new Double(radius * Math.cos(angle * Math.PI / 180)).intValue();
		    dyr = new Double(radius * Math.sin(angle * Math.PI / 180)).intValue();
		    dxtick = new Double((radius + 10) * Math.cos(angle * Math.PI / 180)).intValue();
		    dytick = new Double((radius + 10) * Math.sin(angle * Math.PI / 180)).intValue();
		    dxtext = new Double((radius + 20) * Math.cos(angle * Math.PI / 180)).intValue();
		    dytext = new Double((radius + 20) * Math.sin(angle * Math.PI / 180)).intValue();

		    evt.gc.drawLine(centerX - dxtick, centerY - dytick, centerX - dxr, centerY - dyr);	
		    GraphicsUtils.drawText("" + (minV + i * 1), centerX - dxtext, centerY - dytext, evt.gc, SWT.HORIZONTAL);
			}

	    evt.gc.setBackground(OpenSerialDataExplorer.COLOR_GREY);
			radius = (int)(width / 2 * 0.1);
			evt.gc.fillArc(centerX - radius, centerY - radius, 2 * radius, 2 * radius, 0, 360);
		}
	}
}
