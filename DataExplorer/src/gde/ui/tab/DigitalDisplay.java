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
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.TabAreaContextMenu;

/**
 * Child display class displaying digital active measurements
 * @author Winfried Brügmann
 */
public class DigitalDisplay extends Composite {
	final static Logger			log	= Logger.getLogger(DigitalDisplay.class.getName());

	CLabel					textDigitalLabel;
	CLabel					actualDigitalLabel, maxDigitalLabel, minDigitalLabel;
	Composite				minMaxComposite;

	final OpenSerialDataExplorer	application;
	final Channels								channels;
	final String									recordKey;
	final IDevice									device;
	
	final Color										backgroundColor;
	final Menu										popupmenu;
	final TabAreaContextMenu			contextMenu;

	public DigitalDisplay(OpenSerialDataExplorer currentApplication, Composite digitalWindow, String currentRecordKey, IDevice currentDevice) {
		super(digitalWindow, SWT.BORDER);
		FillLayout digitalComposite1Layout = new FillLayout(SWT.VERTICAL);
		this.setLayout(digitalComposite1Layout);
		this.recordKey = currentRecordKey;
		this.device = currentDevice;
		this.application = currentApplication;
		this.channels = Channels.getInstance();
		
		this.backgroundColor = Settings.getInstance().getDigitalInnerAreaBackground();
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.contextMenu.createMenu(this.popupmenu, TabAreaContextMenu.TYPE_SIMPLE);
	}

	public void create() {
		{
			this.addHelpListener(new HelpListener() {
				public void helpRequested(HelpEvent evt) {
					log.log(Level.FINER, "DigitalDisplay.helpRequested " + evt); //$NON-NLS-1$
					OpenSerialDataExplorer.getInstance().openHelpDialog("", "HelpInfo_7.html"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			});
			this.textDigitalLabel = new CLabel(this, SWT.CENTER | SWT.EMBEDDED);
			this.textDigitalLabel.setFont(SWTResourceManager.getFont(this.application, 14, SWT.BOLD));
			this.textDigitalLabel.setBackground(this.backgroundColor);
			this.textDigitalLabel.setMenu(this.popupmenu);
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
			this.actualDigitalLabel.setBackground(this.backgroundColor);
			this.actualDigitalLabel.setText("00,00"); //$NON-NLS-1$
			this.actualDigitalLabel.setFont(SWTResourceManager.getFont(this.application, 72, SWT.NORMAL));
			this.actualDigitalLabel.setMenu(this.popupmenu);
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
									DigitalDisplay.this.actualDigitalLabel.setText(df.format(DigitalDisplay.this.device.translateValue(record, (record.get(record.size() - 1) / 1000.0))));
									DigitalDisplay.this.maxDigitalLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0236) + df.format(DigitalDisplay.this.device.translateValue(record, (record.getMaxValue() / 1000.0))));
									DigitalDisplay.this.minDigitalLabel.setText(Messages.getString(MessageIds.OSDE_MSGT0237) + df.format(DigitalDisplay.this.device.translateValue(record, (record.getMinValue() / 1000.0))));
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
			this.minDigitalLabel.setFont(SWTResourceManager.getFont(this.application, 12, SWT.BOLD)); 
			this.minDigitalLabel.setBackground(this.backgroundColor);
			this.minDigitalLabel.setMenu(this.popupmenu);

			this.maxDigitalLabel = new CLabel(this.minMaxComposite, SWT.CENTER | SWT.EMBEDDED);
			this.maxDigitalLabel.setText("MAX : 00,00"); //$NON-NLS-1$
			this.maxDigitalLabel.setFont(SWTResourceManager.getFont(this.application, 12, SWT.BOLD));
			this.maxDigitalLabel.setBackground(this.backgroundColor);
			this.maxDigitalLabel.setMenu(this.popupmenu);
		}
	}

	/**
	 * @return the digitalLabel
	 */
	public CLabel getDigitalLabel() {
		return this.actualDigitalLabel;
	}
}
