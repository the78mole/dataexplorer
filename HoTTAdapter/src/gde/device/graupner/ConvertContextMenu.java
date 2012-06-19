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
    
    Copyright (c) 2012 Winfried Bruegmann
****************************************************************************************/
package gde.device.graupner;

import gde.GDE;
import gde.log.Level;
import gde.utils.FileUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * @author Winfried Brügmann
 * This class provides a context menu to tabulator area, curve graphics, compare window, etc. and enable selection of background color, ...
 */
public class ConvertContextMenu {
	final static Logger						log	= Logger.getLogger(ConvertContextMenu.class.getName());
	
	MenuItem											convert2mc32;
	MenuItem											convert2mc20;
	MenuItem											convert2mx20;
	MenuItem											convert2mx16;
	MenuItem											convert2mx12;
	boolean												isCreated = false;
	String sourceFilePath;

	public void createMenu(Menu popupMenu, final Transmitter transmiterCode, final String filePath) {
		sourceFilePath = filePath;
		popupMenu.addMenuListener(new MenuListener() {
			public void menuShown(MenuEvent e) {
				switch (transmiterCode) {
				case MC_32:
					ConvertContextMenu.this.convert2mc32.setEnabled(false);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MC_20:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(false);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MX_20:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(false);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MX_16:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				case MX_12:
					ConvertContextMenu.this.convert2mc32.setEnabled(true);
					ConvertContextMenu.this.convert2mc20.setEnabled(true);
					ConvertContextMenu.this.convert2mx20.setEnabled(true);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				default:
					ConvertContextMenu.this.convert2mc32.setEnabled(false);
					ConvertContextMenu.this.convert2mc20.setEnabled(false);
					ConvertContextMenu.this.convert2mx20.setEnabled(false);
					ConvertContextMenu.this.convert2mx16.setEnabled(false);
					ConvertContextMenu.this.convert2mx12.setEnabled(false);
					break;
				}
			}
			public void menuHidden(MenuEvent e) {
				//ignore
			}
		});
		if (!isCreated) {

			this.convert2mc32 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc32.setText("-> " + Transmitter.MC_32);
			this.convert2mc32.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc32 action performed! " + e); //$NON-NLS-1$
					convert2target(sourceFilePath, Transmitter.MC_32);
				}
			});
			this.convert2mc20 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mc20.setText("-> " + Transmitter.MC_20);
			this.convert2mc20.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mc20 action performed! " + e); //$NON-NLS-1$
					convert2target(sourceFilePath, Transmitter.MC_20);
				}
			});
			this.convert2mx20 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mx20.setText("-> " + Transmitter.MX_20);
			this.convert2mx20.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mx20 action performed! " + e); //$NON-NLS-1$
					convert2target(sourceFilePath, Transmitter.MX_20);
				}
			});
			this.convert2mx16 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mx16.setText("-> " + Transmitter.MX_16);
			this.convert2mx16.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mx16 action performed! " + e); //$NON-NLS-1$
					convert2target(sourceFilePath, Transmitter.MX_16);
				}
			});
			this.convert2mx12 = new MenuItem(popupMenu, SWT.NONE);
			this.convert2mx12.setText("-> " + Transmitter.MX_12);
			this.convert2mx12.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event e) {
					ConvertContextMenu.log.log(Level.FINEST, "convert2mx12 action performed! " + e); //$NON-NLS-1$
					convert2target(sourceFilePath, Transmitter.MX_12);
				}
			});
			isCreated = true;
		}
	}
	
	private void convert2target(String filepath, Transmitter target) {
		DataInputStream in = null;
		DataOutputStream out = null;
		byte[] bytes = new byte[8192];

		try {
			filepath = filepath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
			File inputFile = new File(filepath);
			in = new DataInputStream( new FileInputStream(inputFile));
			String outFilePath = filepath.substring(0, filepath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX));
			outFilePath = outFilePath.substring(0, outFilePath.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1) + target.value() + GDE.FILE_SEPARATOR_UNIX;
			FileUtils.checkDirectoryAndCreate(outFilePath);
			outFilePath = outFilePath+ inputFile.getName();
			File outputFile = new File(outFilePath);
			
			out = new DataOutputStream( new FileOutputStream(outputFile));
			in.read(bytes);
			switch (target) {
			case MC_32:
				System.arraycopy(Transmitter.mc_32_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_32_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE8;
				//System.arraycopy(mc_32_TxRFID, 0, bytes, 0x100, mc_32_TxRFID.length);
				bytes[0x108] = (byte) 0xE8;
				System.arraycopy(Transmitter.mc_32_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_32_MEM_INFO.length);
				bytes[0x160] = (byte) 0xFF;
				break;
			case MC_20:
				System.arraycopy(Transmitter.mc_20_PROD_CODE, 0, bytes, 0x00, Transmitter.mc_20_PROD_CODE.length);
				bytes[0x08] = (byte) 0xEA;
				//System.arraycopy(mc_20_TxRFID, 0, bytes, 0x100, mc_20_TxRFID.length);
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_20_MEM_INFO.length);
				bytes[0x160] = (byte) 0x05;
				break;
			case MX_20:
				System.arraycopy(Transmitter.mx_20_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_20_PROD_CODE.length);
				bytes[0x08] = (byte) 0xEA;
				//System.arraycopy(mx_20_TxRFID, 0, bytes, 0x100, mx_20_TxRFID.length);
				bytes[0x108] = (byte) 0xEA;
				System.arraycopy(Transmitter.mc_20_MEM_INFO, 0, bytes, 0x140, Transmitter.mc_20_MEM_INFO.length);
				break;
			case MX_16:
				System.arraycopy(Transmitter.mx_16_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_16_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE9;
				//System.arraycopy(mx_16_TxRFID, 0, bytes, 0x100, mx_16_TxRFID.length);
				bytes[0x108] = (byte) 0xE9;
				break;
			case MX_12:
				System.arraycopy(Transmitter.mx_12_PROD_CODE, 0, bytes, 0x00, Transmitter.mx_12_PROD_CODE.length);
				bytes[0x08] = (byte) 0xE9;
				//System.arraycopy(mx_12_TxRFID, 0, bytes, 0x100, mx_12_TxRFID.length);
				bytes[0x108] = (byte) 0xE9;
				break;
			}
			out.write(bytes);
			byte[] rest = new byte[4096];
			int count = in.read(rest);

			//mc-32 conversion padding
			switch (target) {
			case MC_32:
			case MC_20:
			case MX_20:
				if (count > 0) {
					byte[] writable = new byte[count];
					System.arraycopy(rest, 0, writable, 0, count);
					out.write(writable);
				}
				int i = count >= 0 ? count : 0;
				for (; i < 4096; i++) {
					out.write(0xFF);
				}
				break;
			}			
			in.close();
			in = null;
			out.close();
			out = null;
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
		finally {
			try {
				if (in != null) in.close();
				if (out != null) out.close();
			}
			catch (IOException e) {
				log.log(Level.WARNING, e.getMessage(), e);
			}
		}

	}
}
