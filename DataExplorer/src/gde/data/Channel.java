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
	static final long											serialVersionUID	= 26031957;
	private Logger												log								= Logger.getLogger(this.getClass().getName());
	
	private String												fileSep = System.getProperty("file.separator");
	private String												name;																														// 1: Ausgang
	private final int											type;
	private GraphicsTemplate							template;																												// graphics template holds view configuration
	private RecordSet											activeRecordSet;
	private final OpenSerialDataExplorer	application;
	private Comparator<String> 						comparator = new RecordSetNameComparator();


	/**
	 * constructor, where channelNumber is used to calculate the name of the channel 1: Ausgang
	 * @param channelNumber 1 -> " 1 : Ausgang"
	 */
	public Channel(int channelNumber, String channelName, int channelType) {
		super(1);
		this.name = " " + channelNumber + " : " + channelName;
		this.type = channelType;
		
		this.application = OpenSerialDataExplorer.getInstance();
		String filename = application.getDevice().getName() + "_" + this.name.split(":")[0].trim();
		this.template = new GraphicsTemplate(Settings.getInstance().getApplHomePath(), filename);
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
		String filename = application.getDevice().getName() + "_" + this.name.split(":")[0];
		this.template = new GraphicsTemplate(Settings.getInstance().getApplHomePath(), filename);
	}

//	/**
//	 * overwrites the put method to fake multiple channel if channel type is ChannelTypes.TYPE_CONFIG
//	 */
//	public RecordSet put(String newRecordkey, RecordSet newRecord) {
//		if(this.name.startsWith(" 1 ") || this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
//			super.put(newRecordkey, newRecord);
//		}
//		else { // ChannelTypes.TYPE_CONFIG
//			Channels.getInstance().get(1).put(newRecordkey, newRecord);
//		}
//		return super.get(newRecordkey);
//	}
//	
//	/**
//	 * overwrites the get method to fake multiple channel if channel type is ChannelTypes.TYPE_CONFIG
//	 */
//	public RecordSet get(String recordkey) {
//		if(this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
//			return super.get(recordkey);
//		}
//		else { // ChannelTypes.TYPE_CONFIG
//			Channels channels = Channels.getInstance();
//			RecordSet recordSet = null;
//			for (Integer channelNumber : Channels.getInstance().keySet()) {
//				recordSet = channels.get(channelNumber)._get(recordkey);
//				if (recordSet != null) break;
//			}
//			return recordSet;
//		}
//	}
//	
//	/**
//	 * method to get record set within channels instance to avoid stack overflow due to never ending recursion 
//	 */
//	private RecordSet _get(String recordkey) {
//		return super.get(recordkey);
//	}
	
	/**
	 * overwrites the size method to return faked size in case of channel type is ChannelTypes.TYPE_CONFIG
	 */
	public int size() {
		if(this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
			return super.size();
		}
		else { // ChannelTypes.TYPE_CONFIG
			int size = 0;
			Channels channels = Channels.getInstance();
			for (Integer channelNumber : Channels.getInstance().keySet()) {
				size += channels.get(channelNumber)._size();
			}
			return size;
		}
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
		return template;
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
 				String[] recordSetNames = channels.get(i).getRealRecordSetNames();
 				for (int j = 0; j < recordSetNames.length; j++) {
 	 				if (recordSetNames[j] != null) namesVector.add(recordSetNames[j]);
				}
			}
			keys = namesVector.toArray( new String[1]);
		}
		Arrays.sort(keys, comparator);
		return keys;
	}
	
	/**
	 * method to get unsorted recordNames within channels instance to avoid stack overflow due to never ending recursion 
	 */
	public String[] getRealRecordSetNames() {
		return this.keySet().toArray( new String[1]);
	}

	/**
	 * get the name of the channel " 1: Ausgang"
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * get the name of the channel to be used as configuration key " 1: Ausgang" -> "Ausgang"
	 * @return String
	 */
	public String getConfigKey() {
		return name.split(":")[1].trim();
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
				template.setProperty(recordName + Record.IS_VISIBLE, new Boolean(record.isVisible()).toString());
				template.setProperty(recordName + Record.IS_POSITION_LEFT, new Boolean(record.isPositionLeft()).toString());
				Color color = record.getColor();
				String rgb = color.getRGB().red + "," + color.getRGB().green + "," + color.getRGB().blue;
				template.setProperty(recordName + Record.COLOR, rgb);
				template.setProperty(recordName + Record.LINE_WITH, new Integer(record.getLineWidth()).toString());
				template.setProperty(recordName + Record.LINE_STYLE, new Integer(record.getLineStyle()).toString());
				template.setProperty(recordName + Record.IS_ROUND_OUT, new Boolean(record.isRoundOut()).toString());
				template.setProperty(recordName + Record.IS_START_POINT_ZERO, new Boolean(record.isStartpointZero()).toString());
				template.setProperty(recordName + Record.NUMBER_FORMAT, new Integer(record.getNumberFormat()).toString());
				template.setProperty(recordName + Record.IS_START_END_DEFINED, new Boolean(record.isStartEndDefined()).toString());
				template.setProperty(recordName + Record.DEFINED_MAX_VALUE, new Double(record.getMaxScaleValue()).toString());
				template.setProperty(recordName + Record.DEFINED_MIN_VALUE, new Double(record.getMinScaleValue()).toString());
				// time grid
				color = recordSet.getColorTimeGrid();
				rgb = color.getRGB().red + "," + color.getRGB().green + "," + color.getRGB().blue;
				template.setProperty(RecordSet.TIME_GRID_COLOR, rgb);
				template.setProperty(RecordSet.TIME_GRID_LINE_STYLE, new Integer(recordSet.getLineStyleTimeGrid()).toString());
				template.setProperty(RecordSet.TIME_GRID_STATE, new Integer(recordSet.getTimeGridType()).toString());
				// curve grid
				color = recordSet.getHorizontalGridColor();
				rgb = color.getRGB().red + "," + color.getRGB().green + "," + color.getRGB().blue;
				template.setProperty(RecordSet.HORIZONTAL_GRID_COLOR, rgb);
				template.setProperty(RecordSet.HORIZONTAL_GRID_LINE_STYSLE, new Integer(recordSet.getHorizontalGridLineStyle()).toString());
				template.setProperty(RecordSet.HORIZONTAL_GRID_STATE, new Integer(recordSet.getHorizontalGridType()).toString());
				template.setProperty(RecordSet.HORIZONTAL_GRID_RECORD, recordSet.getHorizontalGridRecordName());
			}
			template.store();
			log.fine("creating graphics template file " + Settings.getInstance().getApplHomePath() + fileSep + this.getActiveRecordSet().getName() + this.name);
		}
	}

	/**
	 * method to apply the graphics template definition to an record set
	 */
	public void applyTemplate(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);

		if (template != null) template.load();

		if (template.isAvailable()&& recordSet != null) {
			for (String recordName : recordSet.getRecordNames()) {
				Record record = recordSet.get(recordName);
				record.setVisible(new Boolean(template.getProperty(recordName + Record.IS_VISIBLE, "true")).booleanValue());
				record.setPositionLeft(new Boolean(template.getProperty(recordName + Record.IS_POSITION_LEFT, "true")).booleanValue());
				int r, g, b;
				String color = template.getProperty(recordName + Record.COLOR, "128,128,255");
				r = new Integer(color.split(",")[0]).intValue();
				g = new Integer(color.split(",")[1]).intValue();
				b = new Integer(color.split(",")[2]).intValue();
				record.setColor(SWTResourceManager.getColor(r, g, b));
				record.setLineWidth(new Integer(template.getProperty(recordName + Record.LINE_WITH, "1")).intValue());
				record.setLineStyle(new Integer(template.getProperty(recordName + Record.LINE_STYLE, "" + SWT.LINE_SOLID)).intValue());
				record.setRoundOut(new Boolean(template.getProperty(recordName + Record.IS_ROUND_OUT, "false")).booleanValue());
				record.setStartpointZero(new Boolean(template.getProperty(recordName + Record.IS_START_POINT_ZERO, "false")).booleanValue());
				record.setStartEndDefined(new Boolean(template.getProperty(recordName + Record.IS_START_END_DEFINED, "false")).booleanValue(), new Double(template.getProperty(recordName + Record.DEFINED_MIN_VALUE, "0"))
						.doubleValue(), new Double(template.getProperty(recordName + Record.DEFINED_MAX_VALUE, "0")).doubleValue());
				record.setNumberFormat(new Integer(template.getProperty(recordName + Record.NUMBER_FORMAT, "1")).intValue());
				// time grid
				color = template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128");
				r = new Integer(color.split(",")[0]).intValue();
				g = new Integer(color.split(",")[1]).intValue();
				b = new Integer(color.split(",")[2]).intValue();
				recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setTimeGridLineStyle(new Integer(template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, "" + SWT.LINE_DOT)).intValue());
				recordSet.setTimeGridType(new Integer(template.getProperty(RecordSet.TIME_GRID_STATE, "0")).intValue());
				// curve grid
				color = template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128");
				r = new Integer(color.split(",")[0]).intValue();
				g = new Integer(color.split(",")[1]).intValue();
				b = new Integer(color.split(",")[2]).intValue();
				recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setHorizontalGridLineStyle(new Integer(template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYSLE, "" + SWT.LINE_DOT)).intValue());
				recordSet.setHorizontalGridType(new Integer(template.getProperty(RecordSet.HORIZONTAL_GRID_STATE, "0")).intValue());
				recordSet.setHorizontalGridRecordKey(template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD, "0"));
			}
			log.fine("applied graphics template file " + template.getCurrentFilePath());
			if (recordSet.equals(this.getActiveRecordSet())) 
				application.updateGraphicsWindow();
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
		//return (this.name.startsWith(" 1 ") || this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) ? this.activeRecordSet : Channels.getInstance().get(1).getActiveRecordSet();
		return this.activeRecordSet;
	}

	/**
	 * @param recordSetKey of the activeRecordSet to set
	 */
	public void setActiveRecordSet(String recordSetKey) {
		this.activeRecordSet = this.get(recordSetKey);
//		if(this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
//			this.activeRecordSet = this.get(recordSetKey);
//		}
//		else { // ChannelTypes.TYPE_CONFIG
//			RecordSet recordSet = this.get(recordSetKey);
//			Channels channels = Channels.getInstance();
//			for (int i =1; i <= channels.size(); ++i) {
//				channels.get(i).setActiveRecordSet(recordSet);
//			}
//		}
//	}
//
//	private void setActiveRecordSet(RecordSet recordSet) {
//		this.activeRecordSet = recordSet;
	}
	
	/**
	 * switch the record set according selection and set applications active channel
	 * @param recordSetName p.e. "1) Laden"
	 */
	public void switchRecordSet(String recordSetName) {
		log.fine("switching to record set " + recordSetName);
		final Channel activeChannel = this;
		final String recordSetKey = recordSetName;
		if (Thread.currentThread().getId() == application.getThreadId()) {
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
	private void updateForSwitchRecordSet(final Channel activeChannel, final String recordSetKey) {
		//reset old record set before switching
		RecordSet oldRecordSet = activeChannel.getActiveRecordSet();
		if (oldRecordSet != null) oldRecordSet.reset();

		RecordSet recordSet = activeChannel.get(recordSetKey);
		if (recordSet == null) { //activeChannel do not have this record set, try to switch
			int channelNumber = this.findChannelOfRecordSet(recordSetKey);
			if (channelNumber > 0) {
				Channels.getInstance().switchChannel(channelNumber, recordSetKey);
				recordSet = activeChannel.get(recordSetKey);
				activeChannel.applyTemplate(recordSetKey); // updates graphics window
			}
		}
		else { // record  set exist
			activeChannel.setActiveRecordSet(recordSetKey);
			activeChannel.applyTemplate(recordSetKey); // updates graphics window
			application.getMenuToolBar().updateRecordSetSelectCombo();
			application.updateDigitalWindow();
			application.updateAnalogWindow();
			application.updateCellVoltageWindow();
			application.updateDataTable();
			application.updateFileCommentWindow();
			application.updateRecordCommentWindow();
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
		return type;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
}
