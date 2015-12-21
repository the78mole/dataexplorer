package gde.ui.dialog;

import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
* This class enables a simple capability to select a number, for instance a font size
*/
public class FontSizeDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger						log	= Logger.getLogger(FontSizeDialog.class.getName());

	 Shell dialogShell;
	 CCombo fontSizeCombo;
	 Button okButton;
	int selectedFontSize = 50;


	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			FontSizeDialog inst = new FontSizeDialog(shell, SWT.NULL);
			inst.open(new String[]{"20", "25", "30", "35", "40", "45", "50", "55", "60", "65", "70"}, 5);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FontSizeDialog(Shell parent, int style) {
		super(parent, style);
	}

	public int open(String[] sizeValues, int selectionIndex) {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			dialogShell.setText(Messages.getString(MessageIds.GDE_MSGT0727));
			FormLayout dialogShellLayout = new FormLayout();
			dialogShell.setLayout(dialogShellLayout);
			{
				fontSizeCombo = new CCombo(dialogShell, SWT.BORDER);
				FormData fontSizeComboLData = new FormData();
				//fontSizeComboLData.width = 50;
				//fontSizeComboLData.height = 18;
				fontSizeComboLData.left =  new FormAttachment(0, 1000, 12);
				fontSizeComboLData.top =  new FormAttachment(0, 1000, 12);
				fontSizeCombo.setLayoutData(fontSizeComboLData);
				fontSizeCombo.setItems(sizeValues);
				fontSizeCombo.select(selectionIndex);
				fontSizeCombo.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "fontSizeCombo.widgetSelected, event="+evt);
						selectedFontSize = Integer.parseInt(fontSizeCombo.getText().trim());
					}
				});
			}
			{
				okButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				FormData okButtonLData = new FormData();
				//okButtonLData.width = 50;
				//okButtonLData.height = 28;
				okButtonLData.right =  new FormAttachment(1000, 1000, -12);
				okButtonLData.top =  new FormAttachment(0, 1000, 7);
				okButton.setLayoutData(okButtonLData);
				okButton.setText("OK");
				okButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "okButton.widgetSelected, event="+evt);
						dialogShell.dispose();
					}
				});
			}
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent evt) {
					log.log(Level.FINEST, "dialogShell.widgetDisposed, event="+evt);
					selectedFontSize = Integer.parseInt(fontSizeCombo.getText().trim());
				}
			});
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return selectedFontSize;
	}
	
}
