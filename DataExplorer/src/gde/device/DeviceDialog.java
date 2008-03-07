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
package osde.device;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Shell;

import osde.ui.OpenSerialDataExplorer;

/**
 * DeviceDialog is the abstract class as parent for device dialog implementations
 * @author Winfried Brügmann
 */
public abstract class DeviceDialog extends Dialog{

	protected Shell	dialogShell;
	
	protected boolean isClosePossible = true; // use this variable to manage if dialog can be disposed 
	protected String disposeDisabledMessage = "Der Dialog ist aktiv und kann nicht geschlossen werden !";
	
	protected final OpenSerialDataExplorer application;

	/**
	 * constructor for the dialog, in most cases this dialog should not modal  
	 * @param parent
	 */
	public DeviceDialog(Shell parent) {
		super(parent, SWT.NONE);
		this.application = OpenSerialDataExplorer.getInstance();
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
		if (this.isClosePossible) {
			dialogShell.dispose();
			application.setStatusMessage("");
		}
		else application.setStatusMessage(disposeDisabledMessage);
	}

	public void close() {
		this.dispose();
	}

	/**
	 * default method to dispose (close) a dialog shell
	 * implement all cleanup operation in a disposeListener method
	 */
	public boolean isDisposed() {
		return dialogShell != null ? dialogShell.isDisposed() : true;
	}

	/**
	 * default method to drive visibility of a dialog shell
	 */
	public void setVisible(boolean value) {
		dialogShell.setVisible(value);
	}

	/**
	 * default method to set the focus of a dialog shell
	 */
	public boolean setFocus() {
		return dialogShell != null ? dialogShell.setFocus() : false;
	}
}
