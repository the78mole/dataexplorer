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
import org.eclipse.swt.widgets.Composite;

import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Child display class displaying digital active measurements
 * @author Winfried Brügmann
 */
public class DigitalDisplay extends Composite {
	private Logger							log	= Logger.getLogger(this.getClass().getName());

	private CLabel							textDigitalLabel;
	private CLabel							actualDigitalLabel, maxDigitalLabel, minDigitalLabel;
	private Composite						minMaxComposite;

	private final Channels			channels;
	private final String				recordKey;
	private final IDevice	device;

	public DigitalDisplay(Composite digitalWindow, String recordKey, IDevice device) {
		super(digitalWindow, SWT.NONE);
		FillLayout digitalComposite1Layout = new FillLayout(SWT.VERTICAL);
		this.setLayout(digitalComposite1Layout);
		this.recordKey = recordKey;
		this.device = device;
		this.channels = Channels.getInstance();
	}

	public void create() {
		{
			textDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			textDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 14, 1, false, false));
			textDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			//textDigitalLabel.setText("Spannung [V]");
			textDigitalLabel.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.finest("textDigitalLabel.paintControl, event=" + evt);
					RecordSet activeRecordSet = channels.getActiveChannel().getActiveRecordSet();
					if (activeRecordSet != null) {
						log.fine("update label for " + recordKey);
						textDigitalLabel.setText(activeRecordSet.get(recordKey).getName() + " [" + activeRecordSet.get(recordKey).getUnit() + "]");
					}
				}
			});
		}
		{
			actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			actualDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			actualDigitalLabel.setText("00,00");
			actualDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 72, 0, false, false));
			actualDigitalLabel.addPaintListener(new PaintListener() {
				public void paintControl(final PaintEvent evt) {
					log.finest("digitalLabel.paintControl, event=" + evt);
					RecordSet activeRecordSet = channels.getActiveChannel().getActiveRecordSet();
					if (activeRecordSet != null) {
						String channelConfigKey = activeRecordSet.getChannelName();
						if (activeRecordSet != null) {
							Record record = activeRecordSet.getRecord(recordKey);
							if (record != null) {
								CLabel label = (CLabel) evt.widget;
								label.setForeground(record.getColor());
								actualDigitalLabel.setText(String.format("%3.2f", device.translateValue(channelConfigKey, recordKey, new Double(record.get(record.size() - 1) / 1000.0))));
								maxDigitalLabel.setText(String.format("MAX : %3.2f", device.translateValue(channelConfigKey, recordKey, new Double(record.getMaxValue()) / 1000.0)));
								minDigitalLabel.setText(String.format("MIN : %3.2f", device.translateValue(channelConfigKey, recordKey, new Double(record.getMinValue()) / 1000.0)));
							}
						}
					}
				}
			});
		}
		{
			minMaxComposite = new Composite(this, SWT.NONE);
			FillLayout digitalComposite1Layout = new FillLayout(SWT.HORIZONTAL);
			minMaxComposite.setLayout(digitalComposite1Layout);

			minDigitalLabel = new CLabel(minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			minDigitalLabel.setText("MIN : 00,00");
			minDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
			minDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

			maxDigitalLabel = new CLabel(minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			maxDigitalLabel.setText("MAX : 00,00");
			maxDigitalLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 12, 1, false, false));
			maxDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		}
	}

	/**
	 * @return the digitalLabel
	 */
	public CLabel getDigitalLabel() {
		return actualDigitalLabel;
	}
}
