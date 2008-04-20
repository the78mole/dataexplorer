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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import osde.config.GraphicsTemplate;
import osde.config.Settings;
import osde.device.ChannelTypes;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.RecordSetNameComparator;

/**
 * Channel class represents on channel (Ausgang 1, Ausgang 2, ...) where data record sets are accessible (1) laden, 2)Entladen, 1) Flugaufzeichnung, ..)
 * @author Winfried Brügmann
 */
public class Channel extends HashMap<String, RecordSet> {
	static final long							serialVersionUID	= 26031957;
	static final Logger						log								= Logger.getLogger(Channel.class.getName());
	static final String						fileSep = System.getProperty("file.separator");
	
	String												name;																														// 1: Ausgang
	final int											type;
	GraphicsTemplate							template;																												// graphics template holds view configuration
	RecordSet											activeRecordSet;
	final OpenSerialDataExplorer	application;
	Comparator<String> 						comparator = new RecordSetNameComparator();


	/**
	 * constructor, where channelNumber is used to calculate the name of the channel 1: Ausgang
	 * @param channelNumber 1 -> " 1 : Ausgang"
	 */
	public Channel(int channelNumber, String channelName, int channelType) {
		super(1);
		this.name = " " + channelNumber + " : " + channelName;
		this.type = channelType;
		
		this.application = OpenSerialDataExplorer.getInstance();
		String filename = this.application.getDevice().getName() + "_" + this.name.split(":")[0].trim();
		this.template = new GraphicsTemplate(filename);
	}

	/**
	 * Constructor, where channelNumber is used to calculate the name of the channel K1: type
	 * @param channelNumber
	 * @param newRecordSet
	 */
	public Channel(int channelNumber, String channelName, int channelType, RecordSet newRecordSet) {
		super(1);
		this.name = " " + channelNumber + " : " + channelName;
		this.type = channelType;
		this.put(newRecordSet.getName(), newRecordSet);

		this.application = OpenSerialDataExplorer.getInstance();
		String filename = this.application.getDevice().getName() + "_" + this.name.split(":")[0];
		this.template = new GraphicsTemplate(filename);
	}

	/**
	 * overwrites the size method to return faked size in case of channel type is ChannelTypes.TYPE_CONFIG
	 */
	public int size() {
		int size;
		if(this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
			size = super.size();
		}
		else { // ChannelTypes.TYPE_CONFIG
			size = 0;
			Channels channels = Channels.getInstance();
			for (Integer channelNumber : Channels.getInstance().keySet()) {
				size += channels.get(channelNumber)._size();
			}
		}
		return size;
	}

	/**
	 * method to get size within channels instance to avoid stack overflow due to never ending recursion 
	 */
	private int _size(){
		return super.size();
	}
	
	/**
	 * @return the graphics template
	 */
	public GraphicsTemplate getTemplate() {
		return this.template;
	}

	/**
	 * method to get the record set names "1) Laden, 2) Entladen, ..."
	 * @return String[] containing the records names
	 */
	public String[] getRecordSetNames() {
		String[] keys;
		if(this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
			keys = this.keySet().toArray( new String[1]);
		}
		else { // ChannelTypes.TYPE_CONFIG
			Channels channels = Channels.getInstance();
			Vector<String> namesVector = new Vector<String>();
 			for (int i=1; i <= channels.size(); ++i) {
 				String[] recordSetNames = channels.get(i).getUnsortedRecordSetNames();
 				for (int j = 0; j < recordSetNames.length; j++) {
 	 				if (recordSetNames[j] != null) namesVector.add(recordSetNames[j]);
				}
			}
			keys = namesVector.toArray( new String[1]);
		}
		Arrays.sort(keys, this.comparator);
		return keys;
	}
	
	/**
	 * method to get unsorted recordNames within channels instance to avoid stack overflow due to never ending recursion 
	 */
	public String[] getUnsortedRecordSetNames() {
		return this.keySet().toArray( new String[1]);
	}

	/**
	 * query the first record set name, in case of ChannelTypes.TYPE_CONFIG the first entry of keySet might returned
	 */ 
	public String getFirstRecordSetName() {
		if (this.type == ChannelTypes.TYPE_CONFIG.ordinal() && this.keySet() != null)
			return this.keySet().toArray(new String[1])[0];
		
		return this.getRecordSetNames()[0];
	}
	
	/**
	 * get the name of the channel " 1: Ausgang"
	 * @return String
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * get the name of the channel to be used as configuration key " 1: Ausgang" -> "Ausgang"
	 * @return String
	 */
	public String getConfigKey() {
		return this.name.split(":")[1].trim();
	}

	/**
	 * method to get all the record sets of this channel
	 * @return HashMap<Integer, Records>
	 */
	public HashMap<String, RecordSet> getRecordSets() {
		HashMap<String, RecordSet> content = new HashMap<String, RecordSet>(this.size());
		for (String key : this.getRecordSetNames()) {
			content.put(key, this.get(key));
		}
		return content;
	}

	/**
	 * method to save the graphics definition into template file
	 */
	public void saveTemplate() {
		final RecordSet recordSet = this.getActiveRecordSet();

		if (recordSet != null) {
			for (String recordName : recordSet.getRecordNames()) {
				Record record = recordSet.get(recordName);
				this.template.setProperty(recordName + Record.IS_VISIBLE, new Boolean(record.isVisible()).toString());
				this.template.setProperty(recordName + Record.IS_POSITION_LEFT, new Boolean(record.isPositionLeft()).toString());
				Color color = record.getColor();
				String rgb = color.getRGB().red + "," + color.getRGB().green + "," + color.getRGB().blue;
				this.template.setProperty(recordName + Record.COLOR, rgb);
				this.template.setProperty(recordName + Record.LINE_WITH, new Integer(record.getLineWidth()).toString());
				this.template.setProperty(recordName + Record.LINE_STYLE, new Integer(record.getLineStyle()).toString());
				this.template.setProperty(recordName + Record.IS_ROUND_OUT, new Boolean(record.isRoundOut()).toString());
				this.template.setProperty(recordName + Record.IS_START_POINT_ZERO, new Boolean(record.isStartpointZero()).toString());
				this.template.setProperty(recordName + Record.NUMBER_FORMAT, new Integer(record.getNumberFormat()).toString());
				this.template.setProperty(recordName + Record.IS_START_END_DEFINED, new Boolean(record.isStartEndDefined()).toString());
				this.template.setProperty(recordName + Record.DEFINED_MAX_VALUE, new Double(record.getMaxScaleValue()).toString());
				this.template.setProperty(recordName + Record.DEFINED_MIN_VALUE, new Double(record.getMinScaleValue()).toString());
				// time grid
				color = recordSet.getColorTimeGrid();
				rgb = color.getRGB().red + "," + color.getRGB().green + "," + color.getRGB().blue;
				this.template.setProperty(RecordSet.TIME_GRID_COLOR, rgb);
				this.template.setProperty(RecordSet.TIME_GRID_LINE_STYLE, new Integer(recordSet.getLineStyleTimeGrid()).toString());
				this.template.setProperty(RecordSet.TIME_GRID_TYPE, new Integer(recordSet.getTimeGridType()).toString());
				// curve grid
				color = recordSet.getHorizontalGridColor();
				rgb = color.getRGB().red + "," + color.getRGB().green + "," + color.getRGB().blue;
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_COLOR, rgb);
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, new Integer(recordSet.getHorizontalGridLineStyle()).toString());
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_TYPE, new Integer(recordSet.getHorizontalGridType()).toString());
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_RECORD, recordSet.getHorizontalGridRecordName());
			}
			this.template.store();
			log.fine("creating graphics template file " + Settings.getInstance().getApplHomePath() + fileSep + this.getActiveRecordSet().getName() + this.name);
		}
	}

	/**
	 * method to apply the graphics template definition to an record set
	 */
	public void applyTemplate(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);

		if (this.template != null) this.template.load();

		if (this.template.isAvailable()&& recordSet != null) {
			for (String recordName : recordSet.getRecordNames()) {
				Record record = recordSet.get(recordName);
				record.setVisible(new Boolean(this.template.getProperty(recordName + Record.IS_VISIBLE, "true")).booleanValue());
				record.setPositionLeft(new Boolean(this.template.getProperty(recordName + Record.IS_POSITION_LEFT, "true")).booleanValue());
				int r, g, b;
				String color = this.template.getProperty(recordName + Record.COLOR, "128,128,255");
				r = new Integer(color.split(",")[0].trim()).intValue();
				g = new Integer(color.split(",")[1].trim()).intValue();
				b = new Integer(color.split(",")[2].trim()).intValue();
				record.setColor(SWTResourceManager.getColor(r, g, b));
				record.setLineWidth(new Integer(this.template.getProperty(recordName + Record.LINE_WITH, "1")).intValue());
				record.setLineStyle(new Integer(this.template.getProperty(recordName + Record.LINE_STYLE, "" + SWT.LINE_SOLID)).intValue());
				record.setRoundOut(new Boolean(this.template.getProperty(recordName + Record.IS_ROUND_OUT, "false")).booleanValue());
				record.setStartpointZero(new Boolean(this.template.getProperty(recordName + Record.IS_START_POINT_ZERO, "false")).booleanValue());
				record.setStartEndDefined(new Boolean(this.template.getProperty(recordName + Record.IS_START_END_DEFINED, "false")).booleanValue(), new Double(this.template.getProperty(recordName + Record.DEFINED_MIN_VALUE, "0"))
						.doubleValue(), new Double(this.template.getProperty(recordName + Record.DEFINED_MAX_VALUE, "0")).doubleValue());
				record.setNumberFormat(new Integer(this.template.getProperty(recordName + Record.NUMBER_FORMAT, "1")).intValue());
				// time grid
				color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128");
				r = new Integer(color.split(",")[0].trim()).intValue();
				g = new Integer(color.split(",")[1].trim()).intValue();
				b = new Integer(color.split(",")[2].trim()).intValue();
				recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setTimeGridLineStyle(new Integer(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, "" + SWT.LINE_DOT)).intValue());
				recordSet.setTimeGridType(new Integer(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue());
				// curve grid
				color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128");
				r = new Integer(color.split(",")[0].trim()).intValue();
				g = new Integer(color.split(",")[1].trim()).intValue();
				b = new Integer(color.split(",")[2].trim()).intValue();
				recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setHorizontalGridLineStyle(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, "" + SWT.LINE_DOT)).intValue());
				recordSet.setHorizontalGridType(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue());
				recordSet.setHorizontalGridRecordKey(this.template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD, "0"));
			}
			log.fine("applied graphics template file " + this.template.getCurrentFilePath());
			if (recordSet.equals(this.getActiveRecordSet())) 
				this.application.updateGraphicsWindow();
		}
	}
	
	/**
	 * remove active record set and records
	 * @param deleteRecordSetName
	 */
	public void remove(String deleteRecordSetName) {
		super.remove(deleteRecordSetName);
		if (this.size() == 0) this.activeRecordSet = null;
		else this.activeRecordSet = this.get(this.getRecordSetNames()[0]);
	}
	
	/**
	 * @return the activeRecordSet
	 */
	public RecordSet getActiveRecordSet() {
		return this.activeRecordSet;
	}

	/**
	 * @param recordSetKey of the activeRecordSet to set
	 */
	public void setActiveRecordSet(String recordSetKey) {
		this.activeRecordSet = this.get(recordSetKey);
	}
	
	/**
	 * switch the record set according selection and set applications active channel
	 * @param recordSetName p.e. "1) Laden"
	 */
	public void switchRecordSet(String recordSetName) {
		log.fine("switching to record set " + recordSetName);
		final Channel activeChannel = this;
		final String recordSetKey = recordSetName;
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			updateForSwitchRecordSet(activeChannel, recordSetKey);
		}
		else { // execute asynchronous
			OpenSerialDataExplorer.display.asyncExec(new Runnable() {
				public void run() {
					updateForSwitchRecordSet(activeChannel, recordSetKey);
				}
			});
		}
	}

	/**
	 * @param activeChannel
	 * @param recordSetKey
	 */
	void updateForSwitchRecordSet(final Channel activeChannel, final String recordSetKey) {
		//reset old record set before switching
		RecordSet oldRecordSet = activeChannel.getActiveRecordSet();
		if (oldRecordSet != null) oldRecordSet.resetZoomAndMeasurement();

		RecordSet recordSet = activeChannel.get(recordSetKey);
		if (recordSet == null) { //activeChannel do not have this record set, try to switch
			int channelNumber = this.findChannelOfRecordSet(recordSetKey);
			if (channelNumber > 0) {
				Channels.getInstance().switchChannel(channelNumber, recordSetKey);
				recordSet = activeChannel.get(recordSetKey);
				//recordSet.checkAllDisplayable(); // updates graphics window
				//activeChannel.applyTemplate(recordSetKey); // updates graphics window
			}
		}
		else { // record  set exist
			activeChannel.setActiveRecordSet(recordSetKey);
			recordSet.resetZoomAndMeasurement();
			this.application.resetGraphicsWindowZoomAndMeasurement();
			recordSet.checkAllDisplayable(); // updates graphics window
			//activeChannel.applyTemplate(recordSetKey); // updates graphics window
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			this.application.updateDigitalWindow();
			this.application.updateAnalogWindow();
			this.application.updateCellVoltageWindow();
			this.application.updateDataTable();
			this.application.updateFileCommentWindow();
			this.application.updateRecordCommentWindow();
		}
	}

	/**
	 * search through all channels/configurations for the channel which owns a record set with the given key
	 * @param recordSetKey
	 * @return 0 if record set does not exist
	 */
	public int findChannelOfRecordSet(String recordSetKey) {
		int channelNumber = 0;
		Channels channels = Channels.getInstance();
		for (Integer number : Channels.getInstance().keySet()) {
			Channel channel = channels.get(number);
			if (channel.get(recordSetKey) != null) {
				channelNumber = number.intValue();
			}
		}
		return channelNumber;
	}
	
	/**
	 * @return the type
	 */
	public int getType() {
		return this.type;
	}

	/**
	 * @param newName the name to set
	 */
	public void setName(String newName) {
		this.name = newName;
	}
}
