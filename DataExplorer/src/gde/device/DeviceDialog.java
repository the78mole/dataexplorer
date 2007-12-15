/**
 * 
 */
package osde.device;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Winfried Bruegmann
 *
 */
public abstract class DeviceDialog extends Dialog {

	protected Shell	dialogShell;

	/**
	 * constructor for the dialog, in most cases this dialog should not modal  
	 * @param parent
	 */
	public DeviceDialog(Shell parent) {
		super(parent);
	}

	/**
	 * default method where the default controls are defined, this needs to be overwritten by specific device dialog
	 */
	abstract public void open();

	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public void dispose() {
		dialogShell.dispose();
	}

	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public boolean isDisposed() {
		return dialogShell != null ? dialogShell.isDisposed() : true;
	}
}
