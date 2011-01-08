/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.ui;

import gde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import gde.GDE;

/**
 * user interface status bar class, device, serial port, port activity, progress bar, messages
 * @author Winfried Brügmann
 */
public class StatusBar {
	final Logger	log	= Logger.getLogger(this.getClass().getName());
	
	final Composite					statusComposite;
	Composite								connectionComposite;
	Label										txText;
	Label										rxText;
	Label										conText;
	CLabel									portButton;
	CLabel									txButton;
	CLabel									rxButton;
	Composite								comComposite;
	Label										msgLabel;
	ProgressBar							progressBar;
	
	public StatusBar(Composite currentStatusComposite) {
		this.statusComposite = currentStatusComposite;
		this.statusComposite.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				StatusBar.this.log.log(Level.FINER, "statusComposite.paintControl evt=" + evt); //$NON-NLS-1$
				Point statusCompositeSize = StatusBar.this.statusComposite.getSize();
				Point comCompositeSize = StatusBar.this.comComposite.getSize();
				int offsetX = comCompositeSize.x+280;
				int offsetY = (statusCompositeSize.y - 20) / 2;
				int width = statusCompositeSize.x-offsetX-10;
				StatusBar.this.msgLabel.setBounds(offsetX, offsetY, width, 20);
			}
		});
	}

	public void create() {
		{
			this.connectionComposite = new Composite(this.statusComposite, SWT.NONE);
			RowData composite2LData = new RowData();
			composite2LData.width = 170;
			composite2LData.height = 23;
			GridLayout composite2Layout1 = new GridLayout();
			composite2Layout1.makeColumnsEqualWidth = true;
			this.connectionComposite.setLayout(composite2Layout1);
			this.connectionComposite.setLayoutData(composite2LData);
			{
				this.comComposite = new Composite(this.connectionComposite, SWT.NONE);
				FillLayout comCompositeLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				GridData comCompositeLData = new GridData();
				comCompositeLData.verticalAlignment = GridData.FILL;
				comCompositeLData.horizontalAlignment = GridData.FILL;
				comCompositeLData.grabExcessVerticalSpace = true;
				this.comComposite.setLayoutData(comCompositeLData);
				this.comComposite.setLayout(comCompositeLayout);
				{
					this.portButton = new CLabel(this.comComposite, SWT.NONE);
					this.portButton.setBounds(2,2, 50, 20);
					this.portButton.setForeground(DataExplorer.COLOR_DARK_GREEN);
					this.portButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
				}
				{
					this.conText = new Label(this.comComposite, SWT.LEFT);
					this.conText.setText("CON"); //$NON-NLS-1$
					this.conText.setFont(SWTResourceManager.getFont(this.statusComposite.getParent(), 8, SWT.NORMAL)); //$NON-NLS-1$
					this.conText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
				}
				{
					this.rxButton = new CLabel(this.comComposite, SWT.CENTER);
					this.rxButton.setBounds(2,2, 50, 20);
					this.rxButton.setForeground(DataExplorer.COLOR_DARK_GREEN);
					this.rxButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
				}
				{
					this.rxText = new Label(this.comComposite, SWT.LEFT);
					this.rxText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.rxText.setFont(SWTResourceManager.getFont(this.statusComposite.getParent(), 8, SWT.NORMAL)); //$NON-NLS-1$
					this.rxText.setText("RX"); //$NON-NLS-1$
				}
				{
					this.txButton = new CLabel(this.comComposite, SWT.CENTER);
					this.txButton.setBounds(2,2, 50, 20);
					this.txButton.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.txButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
				}
				{
					this.txText = new Label(this.comComposite, SWT.LEFT);
					this.txText.setBackground(DataExplorer.COLOR_LIGHT_GREY);
					this.txText.setFont(SWTResourceManager.getFont(this.statusComposite.getParent(), 8, SWT.NORMAL)); //$NON-NLS-1$
					this.txText.setText("TX"); //$NON-NLS-1$
				}
				this.comComposite.pack();
			}
			{
				RowData progressBarLData = new RowData();
				progressBarLData.width = 250;
				progressBarLData.height = 20;
				this.progressBar = new ProgressBar(this.statusComposite, SWT.NONE);
				this.progressBar.setMinimum(0);
				this.progressBar.setMaximum(100);
				this.progressBar.setSelection(0);
				this.progressBar.setLayoutData(progressBarLData);
			}
			{
				this.msgLabel = new Label(this.statusComposite, SWT.LEFT | SWT.SINGLE);
				this.msgLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			}
		}
	}

	/**
	 * method to set a message text to the message label of the status bar
	 */
	public void setMessage(final String text, int swtColor) {
		this.msgLabel.setForeground(SWTResourceManager.getColor(swtColor));
		this.msgLabel.setText(text);
	}

	/**
	 * method to set a message text to the message label of the status bar
	 */
	public void setMessage(final String text) {
		this.msgLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		this.msgLabel.setText(text);
	}
	
	public void setProgress(final int percentage) {
		this.progressBar.setSelection(percentage);
	}
	
	public int getProgressPercentage() {
		return this.progressBar.getSelection();
	}

	/**
	 * set the serial com port rx light on
	 */
	public void setSerialRxOn() {
		if (!this.rxButton.isDisposed()) this.rxButton.setImage(SWTResourceManager.getImage("gde/resource/LEDHotGreen.gif")); //$NON-NLS-1$
	}

	/**
	 * set the serial com port rx light off
	 */
	public void setSerialRxOff() {
		if (!this.rxButton.isDisposed()) this.rxButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
	}

	/**
	 * set the serial com port tx light on
	 */
	public void setSerialTxOn() {
		if (!this.txButton.isDisposed()) this.txButton.setImage(SWTResourceManager.getImage("gde/resource/LEDHotGreen.gif")); //$NON-NLS-1$
	}

	/**
	 * set the serial com port tx light off
	 */
	public void setSerialTxOff() {
		if (!this.txButton.isDisposed()) this.txButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
	}

	/**
	 * set the serial com port light on
	 */
	public void setSerialPortConnected() {
		if (!this.portButton.isDisposed()) this.portButton.setImage(SWTResourceManager.getImage("gde/resource/LEDHotGreen.gif")); //$NON-NLS-1$
	}

	/**
	 * set the serial com port light off
	 */
	public void setSerialPortDisconnected() {
		if (!this.portButton.isDisposed()) this.portButton.setImage(SWTResourceManager.getImage("gde/resource/LEDGreen.gif")); //$NON-NLS-1$
	}
}
