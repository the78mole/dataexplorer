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
package osde.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import osde.OSDE;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.utils.StringHelper;

/**
 * Channels class is a map where all possible channels of a device are collected, this is a application singleton
 * @author Winfried Brügmann
 */
public class Channels extends HashMap<Integer, Channel> {
	final static long							serialVersionUID		= 26031957;
	final static Logger						log									= Logger.getLogger(Channels.class.getName());

	static Channels								channles								= null;
	String												fileDescription					= StringHelper.getDate();
	int														activeChannelNumber			= 1;		// default at least one channel must exist
	String[]											channelNames 						= new String[1];
	final OpenSerialDataExplorer	application;

	/**
	 *  getInstance returns the instance of this singleton, this may called during creation time of the application
	 *  therefore it is required to give the application instance as argument
	 */
	public static Channels getInstance(OpenSerialDataExplorer application) {
		if (Channels.channles == null) {
			Channels.channles = new Channels(application, 4);
		}
		return Channels.channles;
	}

	/**
	 *  getInstance returns the instance of this singleton
	 */
	public static Channels getInstance() {
		if (Channels.channles == null) {
			Channels.channles = new Channels(4);
		}
		return Channels.channles;
	}

	/**
	 * singleton
	 * @param initialCapacity
	 */
	private Channels(int initialCapacity) {
		super(initialCapacity);
		this.application = OpenSerialDataExplorer.getInstance();
	}

	/**
	 * singleton
	 * @param initialCapacity
	 */
	private Channels(OpenSerialDataExplorer currentApplication, int initialCapacity) {
		super(initialCapacity);
		this.application = currentApplication;
	}
		
	/**
	 * query the channel number by given string, if string not found channel number 1 is returned 
	 * @param channelName
	 * @return channel number
	 */
	public int getChannelNumber(String channelName) {
		int searchedNumber = 1;
		boolean isFound = false;
		for (String name : this.getChannelNames()) {
			if (name != null && name.length() > 5 && name.split(OSDE.STRING_COLON)[1].trim().equals(channelName)) {
				isFound = true;
				break;
			}
			++searchedNumber;
		}
		return isFound ? searchedNumber : 0;
	}

	/**
	 * @return array with channel names
	 */
	public String[] getChannelNames() {
		//return this.application.getMenuToolBar().getChannelSelectCombo().getItems();
		return this.channelNames;
	}

	/**
	 * @param newChannelNames the channel names to set
	 */
	public void setChannelNames(String[] newChannelNames) {
		this.channelNames = newChannelNames.clone();
	}

	/**
	 * @return array with channel names
	 */
	public String getChannelNamesToString() {
		StringBuilder sb = new StringBuilder();
		for (String channelName : this.application.getMenuToolBar().getChannelSelectCombo().getItems()) {
			sb.append(channelName.split(OSDE.STRING_COLON)[1]).append(", "); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelName assuming p.e. " 1 : Ausgang"
	 */
	public synchronized void switchChannel(String channelName) {
		RecordSet recordSet = this.getActiveChannel().getActiveRecordSet();
		if (recordSet != null) recordSet.resetZoomAndMeasurement();
		
		this.switchChannel(new Integer(channelName.split(OSDE.STRING_COLON)[0].trim()).intValue(), OSDE.STRING_EMPTY);
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelNumber 1 -> " 1 : Ausgang"
	 * @param recordSetKey or empty string if switched to first record set
	 */
	public void switchChannel(int channelNumber, String recordSetKey) {
		log.fine("switching to channel " + channelNumber);		 //$NON-NLS-1$
		if (!(channelNumber > this.keySet().size())) {
			if (channelNumber != this.getActiveChannelNumber() || this.getActiveChannel().getActiveRecordSet() == null) {
				this.setActiveChannelNumber(channelNumber);
				this.application.getMenuToolBar().updateChannelToolItems();
				if (recordSetKey == null || recordSetKey.length() < 1)
					this.getActiveChannel().setActiveRecordSet(this.getActiveChannel().getFirstRecordSetName()); // set record set to the first
				else
					this.getActiveChannel().setActiveRecordSet(recordSetKey);
			}
			else {
				log.fine("nothing to do selected channel == active channel"); //$NON-NLS-1$
			}
			this.application.cleanHeaderAndCommentInGraphicsWindow();
			Channel activeChannel = this.getActiveChannel();
			if (activeChannel != null) {
				RecordSet recordSet = activeChannel.getActiveRecordSet();
				if (recordSet != null) {
					recordSet.resetZoomAndMeasurement();
					if (recordSet.isRecalculation)
						recordSet.checkAllDisplayable(); // updates graphics window
					else
						this.application.updateGraphicsWindow();
				}
				this.application.resetGraphicsWindowZoomAndMeasurement();
				// update viewable
				this.application.getMenuToolBar().updateChannelSelector();
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.updateDigitalWindow();
				this.application.updateAnalogWindow();
				this.application.updateCellVoltageWindow();
				this.application.updateFileCommentWindow();
				this.application.updateDataTable();
			}
		}
		else
			this.application.openMessageDialogAsync(Messages.getString(MessageIds.OSDE_MSGW0006)); 
	}

	/**
	 * @return the activeChannelNumber
	 */
	public int getActiveChannelNumber() {
		return this.activeChannelNumber;
	}

	/**
	 * @param newActiveChannelNumber the activeChannelNumber to set
	 */
	public void setActiveChannelNumber(int newActiveChannelNumber) {
		this.activeChannelNumber = newActiveChannelNumber;
	}

	/**
	 * @return activeChannel
	 */
	public Channel getActiveChannel() {
		return this.get(this.activeChannelNumber);
	}

	/**
	 * method to cleanup all child and dependent
	 */
	public void cleanup() {
		this.fileDescription	= new SimpleDateFormat("yyyy-MM-dd").format(new Date()); //$NON-NLS-1$
		this.activeChannelNumber	= 1;		// default at least one channel must exist
		this.channelNames = new String[1];
		Channel activeChannel = Channels.getInstance().getActiveChannel();
		if (activeChannel != null) {
			RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
			if (activeRecordSet != null) {
				String activeRecordName = activeRecordSet.getName();
				activeChannel.get(activeRecordName).clear();
			}
		}
		// use super.size instead of this.size to enable only one channel for multiple channel configurations
		for (int i = 1; i <= super.size(); i++) { 
			Channel channel = this.get(i);
			channel.setFileName(OSDE.STRING_EMPTY);
			channel.setSaved(false);
			for (int j = 0; j < channel.size(); j++) {
				channel.getRecordSets().clear(); // clear records
			}
			channel.clear(); // clear record set
		}
		this.clear(); // clear channel
		log.fine("visited"); //$NON-NLS-1$
	}

	public String getFileDescription() {
		return this.fileDescription;
	}

	public void setFileDescription(String newFileDescription) {
		this.fileDescription = newFileDescription;
	}
	
	/**
	 * method checking all channels has saved record set
	 * @return string array of record sets not saved, length == 0 for all record sets saved
	 */
	public String checkRecordSetsSaved() {
		StringBuffer sb = new StringBuffer();
		// use super.size instead of this.size to enable only one channel for multiple channel configurations
		for (int i = 1; i <= super.size(); i++) {
			Channel channel = this.get(i);
			for (String recordSetkey : channel.getRecordSetNames()) {
				if (channel.get(recordSetkey) != null && !channel.get(recordSetkey).isSaved()) {
					sb.append(System.getProperty("line.separator")).append(channel.getName()).append(" -> ").append(channel.get(recordSetkey).getName()); //$NON-NLS-1$ //$NON-NLS-2$
					if (channel.get(recordSetkey).getUnsaveReasons().size() > 0) {
						sb.append(" ("); //$NON-NLS-1$
						for (String reason : channel.get(recordSetkey).getUnsaveReasons()) {
							sb.append(reason).append(", "); //$NON-NLS-1$
						}
						sb.delete(sb.lastIndexOf(", "), sb.lastIndexOf(", ") + 2).append(")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		}		
		return sb.toString();
	}
	
	public void setSaved(boolean value) {
		this.getActiveChannel().setSaved(value);
	}
	
	public void setFileName(String newFileName) {
		this.getActiveChannel().setFileName(newFileName);
	}
}
