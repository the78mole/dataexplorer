/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.ui.tab;

import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Child display class displaying digital active measurements
 * @author Winfried Brügmann
 */
public class DigitalDisplay extends Composite {
	final static Logger			log	= Logger.getLogger(DigitalDisplay.class.getName());

	CLabel					textDigitalLabel;
	CLabel					actualDigitalLabel, maxDigitalLabel, minDigitalLabel;
	Composite				minMaxComposite;

	final Channels	channels;
	final String		recordKey;
	final IDevice		device;

	public DigitalDisplay(Composite digitalWindow, String currentRecordKey, IDevice currentDevice) {
		super(digitalWindow, SWT.BORDER);
		FillLayout digitalComposite1Layout = new FillLayout(SWT.VERTICAL);
		this.setLayout(digitalComposite1Layout);
		this.recordKey = currentRecordKey;
		this.device = currentDevice;
		this.channels = Channels.getInstance();
	}

	public void create() {
		{
			this.textDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.textDigitalLabel.setFont(SWTResourceManager.getFont("Sans Serif", 14, 1, false, false)); //$NON-NLS-1$
			this.textDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.textDigitalLabel.addPaintListener(new PaintListener() {
				public void paintControl(PaintEvent evt) {
					log.log(Level.FINEST, "textDigitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
							log.log(Level.FINE, "update label for " + DigitalDisplay.this.recordKey); //$NON-NLS-1$
							DigitalDisplay.this.textDigitalLabel.setText(activeRecordSet.get(DigitalDisplay.this.recordKey).getName() + " [ " + activeRecordSet.get(DigitalDisplay.this.recordKey).getUnit() + " ]");
						}
					}
				}
			});
		}
		{
			this.actualDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.actualDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
			this.actualDigitalLabel.setText("00,00"); //$NON-NLS-1$
			this.actualDigitalLabel.setFont(SWTResourceManager.getFont("Sans Serif", 72, 0, false, false)); //$NON-NLS-1$
			this.actualDigitalLabel.addPaintListener(new PaintListener() {
				public void paintControl(final PaintEvent evt) {
					log.log(Level.FINEST, "digitalLabel.paintControl, event=" + evt); //$NON-NLS-1$
					Channel activeChannel = DigitalDisplay.this.channels.getActiveChannel();
					if (activeChannel != null) {
						RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
						if (activeRecordSet != null) {
								Record record = activeRecordSet.getRecord(DigitalDisplay.this.recordKey);
								if (record != null) {
									CLabel label = (CLabel) evt.widget;
									label.setForeground(record.getColor());
									DecimalFormat df = record.getDecimalFormat();
									DigitalDisplay.this.actualDigitalLabel.setText(df.format(DigitalDisplay.this.device.translateValue(record, new Double(record.get(record.size() - 1) / 1000.0))));
									DigitalDisplay.this.maxDigitalLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0236) + df.format(DigitalDisplay.this.device.translateValue(record, new Double(record.getMaxValue()) / 1000.0)));
									DigitalDisplay.this.minDigitalLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0237) + df.format(DigitalDisplay.this.device.translateValue(record, new Double(record.getMinValue()) / 1000.0)));
								}
						}
					}
				}
			});
		}
		{
			this.minMaxComposite = new Composite(this, SWT.NONE);
			FillLayout digitalComposite1Layout = new FillLayout(SWT.HORIZONTAL);
			this.minMaxComposite.setLayout(digitalComposite1Layout);

			this.minDigitalLabel = new CLabel(this.minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			this.minDigitalLabel.setText("MIN : 00,00"); //$NON-NLS-1$
			this.minDigitalLabel.setFont(SWTResourceManager.getFont("Sans Serif", 12, 1, false, false)); //$NON-NLS-1$
			this.minDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

			this.maxDigitalLabel = new CLabel(this.minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			this.maxDigitalLabel.setText("MAX : 00,00"); //$NON-NLS-1$
			this.maxDigitalLabel.setFont(SWTResourceManager.getFont("Sans Serif", 12, 1, false, false)); //$NON-NLS-1$
			this.maxDigitalLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);
		}
	}

	/**
	 * @return the digitalLabel
	 */
	public CLabel getDigitalLabel() {
		return this.actualDigitalLabel;
	}
}
