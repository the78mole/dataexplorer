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
package osde.data;

import java.util.HashMap;
import java.util.logging.Logger;

import osde.ui.OpenSerialDataExplorer;

/**
 * Channels class is a map where all possible channels of a device are collected, this is a application singleton
 * @author Winfried Brügmann
 */
public class Channels extends HashMap<Integer, Channel> {
	final static long											serialVersionUID		= 26031957;
	private Logger												log									= Logger.getLogger(this.getClass().getName());

	private static Channels								instance						= null;
	private String												fileDescription			= "Dateikommentar : ";
	private int														activeChannelNumber	= 1;																						// default at least one channel must exist
	private final OpenSerialDataExplorer	application;

	/**
	 *  getInstance returns the instance of this singleton, this may called during creation time of the application
	 *  therefore it is required to give the application instance as argument
	 */
	public static Channels getInstance(OpenSerialDataExplorer application) {
		if (Channels.instance == null) {
			Channels.instance = new Channels(application, 4);
		}
		return Channels.instance;
	}

	/**
	 *  getInstance returns the instance of this singleton
	 */
	public static Channels getInstance() {
		if (Channels.instance == null) {
			Channels.instance = new Channels(4);
		}
		return Channels.instance;
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
	private Channels(OpenSerialDataExplorer application, int initialCapacity) {
		super(initialCapacity);
		this.application = application;
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelName assuming p.e. "K1: Kanal 1"
	 */
	public synchronized void switchChannel(String channelName) {
		this.switchChannel(new Integer(channelName.split(" ")[2]).intValue());
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelNumber 1 -> "K1: Kanal 1"
	 */
	public synchronized void switchChannel(int channelNumber) {
		log.fine("switching to channel " + channelNumber);
		// clean active record set before switch to the other
		this.getActiveChannel().getActiveRecordSet().resetAllModes();
		
		if (channelNumber != this.getActiveChannelNumber()) {
			this.setActiveChannelNumber(channelNumber);
			application.getMenuToolBar().updateChannelToolItems();
			// set record set to the first
			this.getActiveChannel().setActiveRecordSet(instance.getActiveChannel().getRecordSetNames()[0]);
			// update viewable
			application.getMenuToolBar().updateRecordSetSelectCombo();
			application.updateDataTable();
			application.updateDigitalWindowChilds();
		}
		else {
			log.fine("nothing to do selected channel == active channel");
		}
	}

	/**
	 * method to set the channel number for active channel displayed
	 * @param activeChannelNumber the channel number of the active channel
	 * @param doRedrawRecordSet - flag to manage update
	 */
	public void setActiveChannelNumber(int activeChannelNumber, boolean doRedrawRecordSet) {
		this.activeChannelNumber = activeChannelNumber;
		if (doRedrawRecordSet) {
			if (this.getActiveChannel().getActiveRecordSet() != null) this.getActiveChannel().getActiveRecordSet().refreshAll();
		}
	}

	/**
	 * @return the activeChannelNumber
	 */
	public int getActiveChannelNumber() {
		return activeChannelNumber;
	}

	/**
	 * @param activeChannelNumber the activeChannelNumber to set
	 */
	public void setActiveChannelNumber(int activeChannelNumber) {
		this.activeChannelNumber = activeChannelNumber;
	}

	/**
	 * @return activeChannel
	 */
	public Channel getActiveChannel() {
		return this.get(activeChannelNumber);
	}

	/**
	 * method to cleanup all child and dependent
	 */
	public void cleanup() {
		for (int i = 1; i <= this.size(); i++) {
			Channel channel = this.get(i);
			for (int j = 0; j < channel.size(); j++) {
				channel.getRecordSets().clear(); // clear records
			}
			channel.clear(); // clear record set
		}
		this.clear(); // clear channel
		log.fine("visited");
	}

	public String getFileDescription() {
		return fileDescription;
	}

	public void setFileDescription(String fileDescription) {
		this.fileDescription = fileDescription;
	}
	
	/**
	 * method checking all channels has saved record set
	 * @return string array of record sets not saved, length == 0 for all record sets saved
	 */
	public String checkRecordSetsSaved() {
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i <= this.size(); i++) {
			Channel channel = this.get(i);
			for (String recordSetkey : channel.getRecordSetNames()) {
				if (channel.get(recordSetkey)!= null && !channel.get(recordSetkey).isSaved())
					sb.append(System.getProperty("line.separator")).append(channel.getName()).append(" -> ").append(channel.get(recordSetkey).getName());
			}
		}		
		return sb.toString();
	}
}
