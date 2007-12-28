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
import osde.ui.SWTResourceManager;

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
		if (activeRecordSet != null) {
			Record record = activeRecordSet.getRecord(recordKey);
			log.fine("record name = " + record.getName());
			// Get the canvas and its dimensions
			Canvas canvas = (Canvas) evt.widget;
			int width = canvas.getSize().x;
			int height = canvas.getSize().y;
			log.finer("canvas size = " + width + " x " + height);
			canvas.setBackgroundImage(SWTResourceManager.getImage("osde/resource/WorkItem.gif"));

			double actualValue = device.translateValue(recordKey, new Double(record.get(record.size() - 1) / 1000.0));
			double maxValue =  device.translateValue(recordKey, new Double(record.getMaxValue()) / 1000.0);
			double minValue =  device.translateValue(recordKey, new Double(record.getMinValue()) / 1000.0);
			log.fine(String.format("value = %3.2f; min = %3.2f; max = %3.2f", actualValue, minValue, maxValue));

			// draw clipping bounding 
			//evt.gc.setClipping(10, 10, width-20, height-20);
			evt.gc.setForeground(OpenSerialDataExplorer.COLOR_BLACK);
			canvas.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			evt.gc.setLineWidth(2);
			evt.gc.drawLine(width / 2, height, width / 2, height / 2);
			evt.gc.drawArc(10, 25, width - 20, (width - 20), -20, 220);
			evt.gc.drawArc((width / 2) - 20, (int) (height / 1.4), 40, 40, 0, 360);
		}
	}
}
