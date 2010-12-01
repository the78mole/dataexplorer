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
    
    Copyright (c) 2008,2009,2010 Winfried Bruegmann
****************************************************************************************/
package gde.data;

import java.util.HashMap;
import gde.log.Level;
import java.util.logging.Logger;

import gde.GDE;
import gde.device.ChannelTypes;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

/**
 * Channels class is a map where all possible channels of a device are collected, this is a application singleton
 * @author Winfried Brügmann
 */
public class Channels extends HashMap<Integer, Channel> {
	final static long							serialVersionUID		= 26031957;
	final static Logger						log									= Logger.getLogger(Channels.class.getName());

	static Channels								channles								= null;
	int														activeChannelNumber			= 1;		// default at least one channel must exist
	String[]											channelNames 						= new String[1];
	final DataExplorer	application;

	/**
	 *  getInstance returns the instance of this singleton, this may called during creation time of the application
	 *  therefore it is required to give the application instance as argument
	 */
	public static synchronized Channels getInstance(DataExplorer application) {
		if (Channels.channles == null) {
			Channels.channles = new Channels(application, 4);
		}
		return Channels.channles;
	}

	/**
	 *  getInstance returns the instance of this singleton
	 */
	public static synchronized Channels getInstance() {
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
		this.application = DataExplorer.getInstance();
	}

	/**
	 * singleton
	 * @param initialCapacity
	 */
	private Channels(DataExplorer currentApplication, int initialCapacity) {
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
		
		if (channelName != null && channelName.length() > 5) {
			// "2 : Outlet", use the first digit to calculate the channel number
			if (channelName.contains(GDE.STRING_COLON) && channelName.split(GDE.STRING_COLON).length >= 1 && Character.isDigit(channelName.split(GDE.STRING_COLON)[0].trim().charAt(0))) {
				return new Integer(channelName.split(GDE.STRING_COLON)[0].trim());
			}
			else // old file contnet "Outlet 2" use the last digit to calculate the channel number
				if (channelName.contains(GDE.STRING_BLANK) && channelName.split(GDE.STRING_BLANK).length > 1 && Character.isDigit(channelName.split(GDE.STRING_BLANK)[1].trim().charAt(0))) {
					return new Integer(channelName.split(GDE.STRING_BLANK)[1].trim());
			}
			else {
				for (String name : this.getChannelNames()) {
					// try name matching "Outlet"
					if (name.split(GDE.STRING_COLON)[1].trim().equals(channelName) || name.split(GDE.STRING_COLON)[1].trim().split(GDE.STRING_BLANK)[0].trim().equals(channelName)) {
						break;
					}
					++searchedNumber;
				}
			}
		}
		return searchedNumber;
	}

	/**
	 * @return array with channel names
	 */
	public String[] getChannelNames() {
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
			sb.append(channelName.split(GDE.STRING_COLON)[1]).append(", "); //$NON-NLS-1$
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
		
		this.switchChannel(new Integer(channelName.split(GDE.STRING_COLON)[0].trim()).intValue(), GDE.STRING_EMPTY);
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelNumber 1 -> " 1 : Ausgang"
	 * @param recordSetKey or empty string if switched to first record set
	 */
	public void switchChannel(int channelNumber, String recordSetKey) {
		log.log(Level.FINE, "switching to channel " + channelNumber);		 //$NON-NLS-1$
		this.application.checkUpdateFileComment();
		this.application.checkUpdateRecordSetComment();
			
		if (!(channelNumber > this.keySet().size())) {
			if (channelNumber != this.getActiveChannelNumber() || this.getActiveChannel().getActiveRecordSet() == null) {
				this.setActiveChannelNumber(channelNumber);
				this.application.getMenuToolBar().updateChannelToolItems();
				if (recordSetKey == null || recordSetKey.length() < 1)
					this.getActiveChannel().setActiveRecordSet(this.getActiveChannel().getFirstRecordSetName()); // set record set to the first
				else
					this.getActiveChannel().setActiveRecordSet(recordSetKey);
				
				if (this.getActiveChannel().type == ChannelTypes.TYPE_OUTLET && this.getActiveChannel().getFileName() != null) {
					this.application.updateTitleBar(this.application.getObjectKey(), this.application.getActiveDevice().getName(), this.application.getActiveDevice().getPort());
				}
				DataExplorer.display.asyncExec(new Runnable() {
					public void run() {
						if (!Channels.this.application.getObjectKey().equals(Channels.this.getActiveChannel().getObjectKey())) {
							log.log(Level.FINE, "switch to channels object key \"" + Channels.this.getActiveChannel().getObjectKey() + "\""); //$NON-NLS-1$
							Channels.this.application.getMenuToolBar().updateObjectSelector();
						}
					}
				});
			}
			else {
				log.log(Level.FINE, "nothing to do selected channel == active channel"); //$NON-NLS-1$
			}
			this.application.cleanHeaderAndCommentInGraphicsWindow();
			Channel activeChannel = this.getActiveChannel();
			if (activeChannel != null) {
				RecordSet recordSet = activeChannel.getActiveRecordSet();
				if (recordSet != null) {
					if (!recordSet.hasDisplayableData) {
						recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), true);
					}
					recordSet.resetZoomAndMeasurement();
					if (recordSet.isRecalculation)
						recordSet.checkAllDisplayable(); // updates graphics window
				}
				this.application.resetGraphicsWindowZoomAndMeasurement();
				// update viewable
				//this.application.getMenuToolBar().updateObjectSelector();
				this.application.getMenuToolBar().updateChannelSelector();
				this.application.getMenuToolBar().updateRecordSetSelectCombo();
				this.application.getMenuToolBar().updateGoogleEarthToolItem();
				this.application.updateAllTabs(true);
			}
		}
		else
			this.application.openMessageDialogAsync(Messages.getString(MessageIds.GDE_MSGW0006)); 
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
		this.activeChannelNumber	= 1;		// default at least one channel must exist
		this.channelNames = new String[1];
		try {
			Channel activeChannel = Channels.getInstance().getActiveChannel();
			if (activeChannel != null) {
				activeChannel.objectKey = GDE.STRING_EMPTY;
				RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
				if (activeRecordSet != null) {
					String activeRecordName = activeRecordSet.getName();
					activeChannel.get(activeRecordName).clear();
				}
			}
		}
		catch (RuntimeException e) {
			// ignore since this clear operations are not really required
			log.log(Level.WARNING, e.getMessage(), e);
		}
		// use super.size instead of this.size to enable only one channel for multiple channel configurations
		for (int i = 1; i <= super.size(); i++) { 
			Channel channel = this.get(i);
			channel.setFileName(GDE.STRING_EMPTY);
			channel.setSaved(false);
			for (int j = 0; j < channel.size(); j++) {
				channel.getRecordSets().clear(); // clear records
			}
			channel.clear(); // clear record set
		}
		this.clear(); // clear channel
		log.log(Level.FINE, "visited"); //$NON-NLS-1$
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
}
