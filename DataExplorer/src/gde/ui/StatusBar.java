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
	private CLabel									activePortLabel;
	private ProgressBar							progressBar;
	private CLabel									activeDeviceLabel;

	public StatusBar(OpenSerialDataExplorer application, Composite statusComposite) {
		this.statusComposite = statusComposite;
	}

	public void create() {
		{
			activeDeviceLabel = new CLabel(statusComposite, SWT.NONE);
			RowData activeDeviceLabelLData = new RowData();
			activeDeviceLabelLData.width = 180;
			activeDeviceLabelLData.height = 23;
			activeDeviceLabel.setLayoutData(activeDeviceLabelLData);
			activeDeviceLabel.setText("aktives Gerät");
		}
		{
			activePortLabel = new CLabel(statusComposite, SWT.NONE);
			RowData activePortLData = new RowData();
			activePortLData.width = 80;
			activePortLData.height = 23;
			activePortLabel.setText("comport");
			activePortLabel.setLayoutData(activePortLData);
		}
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
					//portButton.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					portButton.setForeground(OpenSerialDataExplorer.COLOR_DARK_GREEN);
					portButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
				}
				{
					conText = new Text(comComposite, SWT.CENTER);
					conText.setText("CON ");
					conText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
					conText.setEditable(false);
					conText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
				}
				{
					rxButton = new CLabel(comComposite, SWT.NONE);
					rxButton.setBounds(2,2, 50, 20);
					rxButton.setForeground(OpenSerialDataExplorer.COLOR_DARK_GREEN);
					rxButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
				}
				{
					rxText = new Text(comComposite, SWT.CENTER);
					rxText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					rxText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
					rxText.setEditable(false);
					rxText.setText("RX ");
				}
				{
					txButton = new CLabel(comComposite, SWT.NONE);
					txButton.setBounds(2,2, 50, 20);
					txButton.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					txButton.setImage(SWTResourceManager.getImage("osde/resource/LEDGreen.gif"));
				}
				{
					txText = new Text(comComposite, SWT.CENTER);
					txText.setBackground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
					txText.setFont(SWTResourceManager.getFont("Microsoft Sans Serif", 8, 0, false, false));
					txText.setEditable(false);
					txText.setText("TX ");
				}
				comComposite.pack();
			}
			{
				RowData progressBarLData = new RowData();
				progressBarLData.width = 350;
				progressBarLData.height = 20;
				progressBar = new ProgressBar(statusComposite, SWT.NONE);
				progressBar.setMinimum(0);
				progressBar.setMaximum(100);
				progressBar.setSelection(0);
				progressBar.setLayoutData(progressBarLData);
			}
		}
	}

	/**
	 * update device name and com port activated
	 * @param activeName
	 * @param activePort
	 */
	public void update(String activeName, String activePort) {
		activeDeviceLabel.setText(activeName);
		activePortLabel.setText(activePort);
		statusComposite.layout(true);
	}

	public CLabel getActiveDeviceLabel() {
		return activeDeviceLabel;
	}

	public ProgressBar getProgressBar() {
		return progressBar;
	}

	public CLabel getPortLabel() {
		return activePortLabel;
	}

	//	private void initGUI() {
	//		try {
	//			{
	//				statusComposite.setSize(747, 38);
	//			}
	//		}
	//		catch (Exception e) {
	//			e.printStackTrace();
	//		}
	//	}

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
