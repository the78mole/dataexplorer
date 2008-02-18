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
package osde.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

/**
 * user interface status bar class, device, serial port, port activity, progress bar, messages
 * @author Winfried Brügmann
 */
public class StatusBar {
	private Composite								statusComposite;
	private Composite								connectionComposite;
	private Text										txText;
	private Text										rxText;
	private Text										conText;
	private CLabel									portButton;
	private CLabel									txButton;
	private CLabel									rxButton;
	private Composite								comComposite;
	private CLabel									msgLabel;
	private ProgressBar							progressBar;
	private int 										progessPercentage = 0;

	public StatusBar(OpenSerialDataExplorer application, Composite statusComposite) {
		this.statusComposite = statusComposite;
	}

	public void create() {
		{
			connectionComposite = new Composite(statusComposite, SWT.NONE);
			RowData composite2LData = new RowData();
			composite2LData.width = 170;
			composite2LData.height = 23;
			GridLayout composite2Layout1 = new GridLayout();
			composite2Layout1.makeColumnsEqualWidth = true;
			connectionComposite.setLayout(composite2Layout1);
			connectionComposite.setLayoutData(composite2LData);
			{
				comComposite = new Composite(connectionComposite, SWT.NONE);
				FillLayout comCompositeLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
				GridData comCompositeLData = new GridData();
				comCompositeLData.verticalAlignment = GridData.FILL;
				comCompositeLData.horizontalAlignment = GridData.FILL;
				comCompositeLData.grabExcessVerticalSpace = true;
				comComposite.setLayoutData(comCompositeLData);
				comComposite.setLayout(comCompositeLayout);
				{
					portButton = new CLabel(comComposite, SWT.NONE);
					portButton.setBounds(2,2, 50, 20);
					portButton.setForeground(OpenSerialDataExplorer.COLOR_DARK_GREEN);
					portButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
				}
				{
					conText = new Text(comComposite, SWT.LEFT);
					conText.setText("CON");
					conText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
					conText.setEditable(false);
					conText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					rxButton = new CLabel(comComposite, SWT.CENTER);
					rxButton.setBounds(2,2, 50, 20);
					rxButton.setForeground(OpenSerialDataExplorer.COLOR_DARK_GREEN);
					rxButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
				}
				{
					rxText = new Text(comComposite, SWT.LEFT);
					rxText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					rxText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
					rxText.setEditable(false);
					rxText.setText("RX");
				}
				{
					txButton = new CLabel(comComposite, SWT.CENTER);
					txButton.setBounds(2,2, 50, 20);
					txButton.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					txButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
				}
				{
					txText = new Text(comComposite, SWT.LEFT);
					txText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					txText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
					txText.setEditable(false);
					txText.setText("TX");
				}
				comComposite.pack();
			}
			{
				RowData progressBarLData = new RowData();
				progressBarLData.width = 250;
				progressBarLData.height = 20;
				progressBar = new ProgressBar(statusComposite, SWT.NONE);
				progressBar.setMinimum(0);
				progressBar.setMaximum(100);
				progressBar.setSelection(0);
				progressBar.setLayoutData(progressBarLData);
			}
			{
				msgLabel = new CLabel(statusComposite, SWT.LEFT);
				//msgLabel.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
				//msgLabel.setBackground(OpenSerialDataExplorer.COLOR_CANVAS_YELLOW);

			}
		}
	}

	/**
	 * method to set a message text to the message label of the status bar
	 */
	public void setMessage(final String text) {
		if (text.length() > 5) msgLabel.setText("   " + text + "   ");
		else msgLabel.setText(text);
		msgLabel.pack(true);
	}
	
	/**
	 * method to set a message text to the message label of the status bar
	 */
	public void setMessageAsync(final String text) {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				if (text.length() > 5) msgLabel.setText("   " + text + "   ");
				else msgLabel.setText(text);
				msgLabel.pack(true);
			}
		});
	}


	public void setProgress(final int precent) {
		progressBar.setSelection(precent);
	}
	
	public void setProgressAsync(final int precent) {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				progressBar.setSelection(precent);
			}
		});
	}

	public synchronized int getProgressPercentage() {
		return progressBar.getSelection();
	}

	public synchronized int getProgressPercentageAsync() {
		progessPercentage = 0;
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				progessPercentage = progressBar.getSelection();
			}
		});
		return progessPercentage;
	}

	/**
	 * set the serial com port rx light on
	 */
	public void setSerialRxOn() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				rxButton.setImage(SWTResourceManager.getImage("osde/resource/LEDHotGreen.gif"));
			}
		});
	}

	/**
	 * set the serial com port rx light off
	 */
	public void setSerialRxOff() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				rxButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
			}
		});
	}

	/**
	 * set the serial com port tx light on
	 */
	public void setSerialTxOn() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				txButton.setImage(SWTResourceManager.getImage("osde/resource/LEDHotGreen.gif"));
			}
		});
	}

	/**
	 * set the serial com port tx light off
	 */
	public void setSerialTxOff() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				txButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
			}
		});
	}

	/**
	 * set the serial com port light on
	 */
	public void setSerialPortConnected() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				portButton.setImage(SWTResourceManager.getImage("osde/resource/LEDHotGreen.gif"));
			}
		});
	}

	/**
	 * set the serial com port light off
	 */
	public void setSerialPortDisconnected() {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				portButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
			}
		});
	}

	/**
	 * update the progress bar percentage
	 */
	public void updateProgressbar(final int precentage) {
		OpenSerialDataExplorer.display.asyncExec(new Runnable() {
			public void run() {
				progressBar.setSelection(precentage);
				progressBar.redraw();
			}
		});
	}
}
