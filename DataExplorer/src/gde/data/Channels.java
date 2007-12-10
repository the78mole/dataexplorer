/**
 * 
 */
package osde.data;

import java.util.HashMap;
import java.util.logging.Logger;

import osde.ui.OpenSerialDataExplorer;

/**
 * @author winfried bruegmann
 *
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
	 * @param channelText assuming p.e. "K1: Kanal 1"
	 */
	public synchronized void switchChannel(String channelName) {
		this.switchChannel(new Integer(channelName.split(" ")[2]).intValue());
	}

	/**
	 * switch the channel according selection and set applications active channel
	 * @param channelText assuming p.e. "K1: Kanal 1"
	 */
	public synchronized void switchChannel(int channelNumber) {
		log.fine("switching to channel " + channelNumber);
		if (channelNumber != this.getActiveChannelNumber()) {
			this.setActiveChannelNumber(channelNumber);
			application.getDataToolBar().updateChannelToolItems();
			// set record set to the first
			this.getActiveChannel().setActiveRecordSet(instance.getActiveChannel().getRecordSetNames()[0]);
			application.getDataToolBar().updateRecordSetSelectCombo();
			// update viewable
			//application.getGraphicsWindow().redrawGrahics();
			application.updateDataTable();
			application.updateDigitalWindowChilds();
		}
		else {
			log.fine("nothing to do selected channel == active channel");
		}
	}

	/**
	 * method to set the channel number for active channel displayed
	 * @param activeNumber the channel number of the active channel
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
	 * @param activeChannelNumber the activeChannelNumber to set
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
}
