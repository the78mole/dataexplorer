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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

import osde.OSDE;
import osde.config.GraphicsTemplate;
import osde.config.Settings;
import osde.device.ChannelTypes;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.RecordSetNameComparator;

/**
 * Channel class represents on channel (Ausgang 1, Ausgang 2, ...) where data record sets are accessible (1) laden, 2)Entladen, 1) Flugaufzeichnung, ..)
 * The behavior of this class depends on its type (ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG)
 * TYPE_OUTLET means that a channel represents exact one object with one view, like a battery
 * TPPE_CONFIG means one objects may have different views, so all channels represent one object
 * @author Winfried Brügmann
 */
public class Channel extends HashMap<String, RecordSet> {
	static final long							serialVersionUID	= 26031957;
	static final Logger						log								= Logger.getLogger(Channel.class.getName());
	
	String												name;							// 1 : Ausgang
	int														ordinal;
	final int											type;							// ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG
	GraphicsTemplate							template;					// graphics template holds view configuration
	RecordSet											activeRecordSet;
	String												objectKey	= OSDE.STRING_EMPTY;
	String 												fileName;
	boolean												isSaved = false;
	final OpenSerialDataExplorer	application;
	Comparator<String> 						comparator = new RecordSetNameComparator();
	
	public final static String		UNSAVED_REASON_ADD_OBJECT_KEY	= Messages.getString(MessageIds.OSDE_MSGT0210);
	public final static String		UNSAVED_REASON_REMOVE_OBJECT_KEY	= Messages.getString(MessageIds.OSDE_MSGT0211);


	/**
	 * constructor, where channelNumber is used to calculate the name of the channel 1: Ausgang
	 * @param channelNumber 1 -> " 1 : Ausgang"
	 */
	public Channel(int channelNumber, String channelName, int channelType) {
		super(1);
		this.name = OSDE.STRING_BLANK + channelNumber + OSDE.STRING_BLANK_COLON_BLANK + channelName;
		this.ordinal = channelNumber;
		this.type = channelType;
		
		this.application = OpenSerialDataExplorer.getInstance();
		String templateFileName = this.application.getActiveDevice().getName() + OSDE.STRING_UNDER_BAR + this.name.split(OSDE.STRING_COLON)[0].trim();
		this.template = new GraphicsTemplate(templateFileName);
	}

	/**
	 * Constructor, where channelNumber is used to calculate the name of the channel K1: type
	 * @param channelNumber
	 * @param newRecordSet
	 */
	public Channel(int channelNumber, String channelName, int channelType, RecordSet newRecordSet) {
		super(1);
		this.name = OSDE.STRING_BLANK + channelNumber + OSDE.STRING_BLANK_COLON_BLANK + channelName;
		this.type = channelType;
		this.put(newRecordSet.getName(), newRecordSet);

		this.application = OpenSerialDataExplorer.getInstance();
		String templateFileName = this.application.getActiveDevice().getName() + OSDE.STRING_UNDER_BAR + this.name.split(OSDE.STRING_COLON)[0];
		this.template = new GraphicsTemplate(templateFileName);
	}

	/**
	 * overwrites the size method to return faked size in case of channel type is ChannelTypes.TYPE_CONFIG
	 * TYPE_CONFIG means the all record sets depends to the object and the different (configuration) channels enable differnt views to it
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
	 * method to calculate next record set number, usally a record starts with a number followed by ")"
	 * this method is used to build a new record set name while gathering data "3") flight record
	 * @return preceeding record set number
	 */
	public int getNextRecordSetNumber() {
		int recordNumber = 1;
		if (this.size() != 0) {
			String[] sortedRecordSetNames = this.getRecordSetNames();
			for (int i = sortedRecordSetNames.length - 1; i >= 0; --i) {
				try {
					recordNumber = new Integer(sortedRecordSetNames[i].split("[)]")[0]) + 1;
					break;
				}
				catch (NumberFormatException e) {
					// is alpha no numeric or no ")"
				}
			}
		}
		else 
			recordNumber = 1;
		
		return recordNumber;
	}
	/**
	 * @return the graphics template
	 */
	public GraphicsTemplate getTemplate() {
		return this.template;
	}

	/**
	 * method to get the record set names "1) Laden, 2) Entladen, ...".
	 * the behavior of this method depends on this.type (ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG)
	 * TYPE_OUTLET means that a channel represents exact one object, like a battery
	 * TPPE_CONFIG measn one objects has different views, so this method returns all record set names for all channels
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
	 * @return String[] containing the records names
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
		return this.name.split(OSDE.STRING_COLON)[1].trim();
	}

	/**
	 * method to get all the record sets of this channel
	 * @return HashMap<Integer, Records>
	 */
	public HashMap<String, RecordSet> getRecordSets() {
		HashMap<String, RecordSet> content = new HashMap<String, RecordSet>(this.size());
		if(this.getType() == ChannelTypes.TYPE_OUTLET.ordinal()) {
			for (String key : this.getRecordSetNames()) {
				content.put(key, this.get(key));
			}
		}
		else { // ChannelTypes.TYPE_CONFIG
			Channels channels = Channels.getInstance();
 			for (int i=1; i <= channels.size(); ++i) {
 				for (String key : channels.get(i).getUnsortedRecordSetNames()) {
 					if (key !=null && key.length() > 1) content.put(key, channels.get(i).get(key));
 				}
			}
		}
		return content;
	}

	/**
	 * method to save the graphics definition into template file
	 */
	public void saveTemplate() {
		final RecordSet recordSet = this.getActiveRecordSet();

		if (recordSet != null) {
			for (int i=0; i<recordSet.getRecordNames().length; ++i) {
				Record record = recordSet.get(recordSet.getRecordNames()[i]);
				this.template.setProperty(i + Record.IS_VISIBLE, new Boolean(record.isVisible()).toString());
				this.template.setProperty(i + Record.IS_POSITION_LEFT, new Boolean(record.isPositionLeft()).toString());
				Color color = record.getColor();
				String rgb = color.getRGB().red + OSDE.STRING_COMMA + color.getRGB().green + OSDE.STRING_COMMA + color.getRGB().blue;
				this.template.setProperty(i + Record.COLOR, rgb);
				this.template.setProperty(i + Record.LINE_WITH, new Integer(record.getLineWidth()).toString());
				this.template.setProperty(i + Record.LINE_STYLE, new Integer(record.getLineStyle()).toString());
				this.template.setProperty(i + Record.IS_ROUND_OUT, new Boolean(record.isRoundOut()).toString());
				this.template.setProperty(i + Record.IS_START_POINT_ZERO, new Boolean(record.isStartpointZero()).toString());
				this.template.setProperty(i + Record.NUMBER_FORMAT, new Integer(record.getNumberFormat()).toString());
				this.template.setProperty(i + Record.IS_START_END_DEFINED, new Boolean(record.isStartEndDefined()).toString());
				this.template.setProperty(i + Record.DEFINED_MAX_VALUE, new Double(record.getMaxScaleValue()).toString());
				this.template.setProperty(i + Record.DEFINED_MIN_VALUE, new Double(record.getMinScaleValue()).toString());
				// time grid
				color = recordSet.getColorTimeGrid();
				rgb = color.getRGB().red + OSDE.STRING_COMMA + color.getRGB().green + OSDE.STRING_COMMA + color.getRGB().blue;
				this.template.setProperty(RecordSet.TIME_GRID_COLOR, rgb);
				this.template.setProperty(RecordSet.TIME_GRID_LINE_STYLE, new Integer(recordSet.getLineStyleTimeGrid()).toString());
				this.template.setProperty(RecordSet.TIME_GRID_TYPE, new Integer(recordSet.getTimeGridType()).toString());
				// curve grid
				color = recordSet.getHorizontalGridColor();
				rgb = color.getRGB().red + OSDE.STRING_COMMA + color.getRGB().green + OSDE.STRING_COMMA + color.getRGB().blue;
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_COLOR, rgb);
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, new Integer(recordSet.getHorizontalGridLineStyle()).toString());
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_TYPE, new Integer(recordSet.getHorizontalGridType()).toString());
				if (recordSet.get(recordSet.getHorizontalGridRecordName()) != null) {
					this.template.setProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, new Integer(recordSet.get(recordSet.getHorizontalGridRecordName()).ordinal).toString());
				}
			}
			this.template.store();
			log.log(Level.FINE, "creating graphics template file " + Settings.getInstance().getApplHomePath() + OSDE.FILE_SEPARATOR_UNIX + this.getActiveRecordSet().getName() + this.name); //$NON-NLS-1$
		}
	}
	
	/**
	 * method to apply the graphics template definition colors to an record set
	 */
	public void applyTemplateBasics(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);

		if (this.template != null) this.template.load();

		if (this.template.isAvailable()&& recordSet != null) {
			for (int i=0; i<recordSet.getRecordNames().length; ++i) {
				Record record = recordSet.get(recordSet.getRecordNames()[i]);
				//record.setVisible(new Boolean(this.template.getProperty(recordName + Record.IS_VISIBLE, "true")).booleanValue());
				//record.setPositionLeft(new Boolean(this.template.getProperty(recordName + Record.IS_POSITION_LEFT, "true")).booleanValue());
				int r, g, b;
				String color = this.template.getProperty(i + Record.COLOR, "128,128,255"); //$NON-NLS-1$
				r = new Integer(color.split(OSDE.STRING_COMMA)[0].trim()).intValue();
				g = new Integer(color.split(OSDE.STRING_COMMA)[1].trim()).intValue();
				b = new Integer(color.split(OSDE.STRING_COMMA)[2].trim()).intValue();
				record.setColor(SWTResourceManager.getColor(r, g, b));
				record.setLineWidth(new Integer(this.template.getProperty(i + Record.LINE_WITH, "1")).intValue()); //$NON-NLS-1$
				record.setLineStyle(new Integer(this.template.getProperty(i + Record.LINE_STYLE, OSDE.STRING_EMPTY + SWT.LINE_SOLID)).intValue());
				//record.setRoundOut(new Boolean(this.template.getProperty(recordName + Record.IS_ROUND_OUT, "false")).booleanValue());
				//record.setStartpointZero(new Boolean(this.template.getProperty(recordName + Record.IS_START_POINT_ZERO, "false")).booleanValue());
				//record.setStartEndDefined(new Boolean(this.template.getProperty(recordName + Record.IS_START_END_DEFINED, "false")).booleanValue(), new Double(this.template.getProperty(recordName + Record.DEFINED_MIN_VALUE, "0"))
				//		.doubleValue(), new Double(this.template.getProperty(recordName + Record.DEFINED_MAX_VALUE, "0")).doubleValue());
				record.setNumberFormat(new Integer(this.template.getProperty(i + Record.NUMBER_FORMAT, "1")).intValue()); //$NON-NLS-1$
				// time grid
				color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				r = new Integer(color.split(OSDE.STRING_COMMA)[0].trim()).intValue();
				g = new Integer(color.split(OSDE.STRING_COMMA)[1].trim()).intValue();
				b = new Integer(color.split(OSDE.STRING_COMMA)[2].trim()).intValue();
				recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setTimeGridLineStyle(new Integer(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, OSDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				recordSet.setTimeGridType(new Integer(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				// curve grid
				color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				r = new Integer(color.split(OSDE.STRING_COMMA)[0].trim()).intValue();
				g = new Integer(color.split(OSDE.STRING_COMMA)[1].trim()).intValue();
				b = new Integer(color.split(OSDE.STRING_COMMA)[2].trim()).intValue();
				recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setHorizontalGridLineStyle(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, OSDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				recordSet.setHorizontalGridType(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				recordSet.setHorizontalGridRecordKey(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, "-1")).intValue()); //$NON-NLS-1$
			}
			log.log(Level.FINE, "applied graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (this.getActiveRecordSet() != null && recordSet.equals(this.getActiveRecordSet())) 
				this.application.updateGraphicsWindow();
		}
	}	

	/**
	 * method to apply the graphics template definition to an record set
	 */
	public void applyTemplate(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);

		if (this.template != null) this.template.load();

		if (this.template.isAvailable()&& recordSet != null) {
			for (int i=0; i<recordSet.getRecordNames().length; ++i) {
				Record record = recordSet.get(recordSet.getRecordNames()[i]);
				record.setVisible(new Boolean(this.template.getProperty(i + Record.IS_VISIBLE, "true")).booleanValue()); //$NON-NLS-1$
				record.setPositionLeft(new Boolean(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true")).booleanValue()); //$NON-NLS-1$
				int r, g, b;
				String color = this.template.getProperty(i + Record.COLOR, "128,128,255"); //$NON-NLS-1$
				r = new Integer(color.split(OSDE.STRING_COMMA)[0].trim()).intValue();
				g = new Integer(color.split(OSDE.STRING_COMMA)[1].trim()).intValue();
				b = new Integer(color.split(OSDE.STRING_COMMA)[2].trim()).intValue();
				record.setColor(SWTResourceManager.getColor(r, g, b));
				record.setLineWidth(new Integer(this.template.getProperty(i + Record.LINE_WITH, "1")).intValue()); //$NON-NLS-1$
				record.setLineStyle(new Integer(this.template.getProperty(i + Record.LINE_STYLE, OSDE.STRING_EMPTY + SWT.LINE_SOLID)).intValue());
				record.setRoundOut(new Boolean(this.template.getProperty(i + Record.IS_ROUND_OUT, "false")).booleanValue()); //$NON-NLS-1$
				record.setStartpointZero(new Boolean(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false")).booleanValue()); //$NON-NLS-1$
				record.setStartEndDefined(new Boolean(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")).booleanValue(), new Double(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")) //$NON-NLS-1$ //$NON-NLS-2$
						.doubleValue(), new Double(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "0")).doubleValue()); //$NON-NLS-1$
				record.setNumberFormat(new Integer(this.template.getProperty(i + Record.NUMBER_FORMAT, "1")).intValue()); //$NON-NLS-1$
				// time grid
				color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				r = new Integer(color.split(OSDE.STRING_COMMA)[0].trim()).intValue();
				g = new Integer(color.split(OSDE.STRING_COMMA)[1].trim()).intValue();
				b = new Integer(color.split(OSDE.STRING_COMMA)[2].trim()).intValue();
				recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setTimeGridLineStyle(new Integer(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, OSDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				recordSet.setTimeGridType(new Integer(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				// curve grid
				color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
				r = new Integer(color.split(OSDE.STRING_COMMA)[0].trim()).intValue();
				g = new Integer(color.split(OSDE.STRING_COMMA)[1].trim()).intValue();
				b = new Integer(color.split(OSDE.STRING_COMMA)[2].trim()).intValue();
				recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
				recordSet.setHorizontalGridLineStyle(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, OSDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
				recordSet.setHorizontalGridType(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
				recordSet.setHorizontalGridRecordKey(new Integer(this.template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, "-1")).intValue()); //$NON-NLS-1$
			}
			recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
			log.log(Level.FINE, "applied graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (this.getActiveRecordSet() != null && recordSet.getName().equals(this.getActiveRecordSet().getName()) && this.application.getMenuBar() != null) 
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
		RecordSet newActiveRecordSet = this.get(recordSetKey);
		if (newActiveRecordSet != null) {
			this.activeRecordSet = newActiveRecordSet;
			this.activeRecordSet.check4SyncableRecords();
		}
	}
	
	/**
	 * @param newActiveRecordSet to set
	 */
	public void setActiveRecordSet(RecordSet newActiveRecordSet) {
		this.activeRecordSet = newActiveRecordSet;
	}
	
	/**
	 * switch the record set according selection and set applications active channel
	 * @param recordSetName p.e. "1) Laden"
	 */
	public void switchRecordSet(String recordSetName) {
		log.log(Level.FINE, String.format("switching to record set threadId = %06d", Thread.currentThread().getId())); //$NON-NLS-1$
		int percentage = this.application.getProgressPercentage();
		if (percentage > 99 || percentage == 0)
			this.application.setProgress(0, null);
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
				if (recordSet != null && recordSet.isRecalculation)
					recordSet.checkAllDisplayable();
			}
		}
		else { // record  set exist
			activeChannel.setActiveRecordSet(recordSetKey);
			if (!recordSet.hasDisplayableData)
				recordSet.loadFileData(activeChannel.getFullQualifiedFileName(), true);
			recordSet.resetZoomAndMeasurement();
			this.application.resetGraphicsWindowZoomAndMeasurement();
			if (recordSet.isRecalculation)
				recordSet.checkAllDisplayable(); // updates graphics window
			
			this.application.cleanHeaderAndCommentInGraphicsWindow();
			this.application.updateGraphicsWindow();
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			this.application.updateDigitalWindow();
			this.application.updateAnalogWindow();
			this.application.updateCellVoltageWindow();
			this.application.updateFileCommentWindow();
			this.application.updateStatisticsData();
			this.application.updateDataTable(recordSetKey);
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
	 * @return the type as ordinal
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

	public String getFileName() {
		return this.fileName!= null ? this.fileName.substring(this.fileName.lastIndexOf(OSDE.FILE_SEPARATOR_UNIX)+1) : null;
	}

	public String getFullQualifiedFileName() {
		return this.fileName;
	}

	public void setFileName(String newFileName) {
		if(this.getType() == ChannelTypes.TYPE_CONFIG.ordinal()) {
			Channels channels = Channels.getInstance();
			for (int i = 1; i<= channels.getChannelNames().length; ++i) {
				channels.get(i).fileName = newFileName;
			}
		}
		else {
			this.fileName = newFileName;
		}
		if (this.fileName != null) this.application.updateTitleBar(this.application.getActiveDevice().getName(), this.application.getActiveDevice().getPort());
	}

	public boolean isSaved() {
		return this.isSaved;
	}

	public void setSaved(boolean is_saved) {
		if(this.getType() == ChannelTypes.TYPE_CONFIG.ordinal()) {
			Channels channels = Channels.getInstance();
			for (int i = 1; i<= channels.getChannelNames().length; ++i) {
				channels.get(i).isSaved = is_saved;
			}
		}
		else {
			this.isSaved = is_saved;
		}		
	}
	
	/**
	 * check if all record sets have its data loaded, if required load data from file
	 * this method can be used to check prior to save modified data
	 * the behavior which recordset data is checked and loaded depends on the method this.getRecordSetNames() 
	 */
	public void checkAndLoadData() {
		String fullQualifiedFileName = this.getFullQualifiedFileName();
		for (String tmpRecordSetName : this.getRecordSetNames()) {
			log.log(Level.FINER, "tmpRecordSetName = " + tmpRecordSetName);
			Channel selectedChannel = Channels.getInstance().get(this.findChannelOfRecordSet(tmpRecordSetName));
			log.log(Level.FINER, "selectedChannel = " + (selectedChannel != null ? selectedChannel.getName() : "null"));
			if (selectedChannel != null) {
				RecordSet tmpRecordSet = selectedChannel.get(tmpRecordSetName);
				log.log(Level.FINER, "tmpRecordSet = " + (tmpRecordSet != null ? tmpRecordSet.getName() : "null"));
				if (tmpRecordSet != null && !tmpRecordSet.hasDisplayableData()) {
					log.log(Level.FINER, "tmpRecordSetName needs data to loaded");
					if (tmpRecordSet.fileDataSize != 0 && tmpRecordSet.fileDataPointer != 0) {
						log.log(Level.FINER, "loading data ...");
						tmpRecordSet.loadFileData(fullQualifiedFileName, this.application.getStatusBar() != null);
					}	
				}
			}
		}
	}

	/**
	 * @return the channel/config number
	 */
	public int getOrdinal() {
		return this.ordinal;
	}

	public String getObjectKey() {
		return this.objectKey;
	}

	public void setObjectKey(String newObjectkey) {
		this.objectKey = newObjectkey;
		if (this.activeRecordSet != null) {
			if (newObjectkey.equals(OSDE.STRING_EMPTY))	this.activeRecordSet.setUnsaved(Channel.UNSAVED_REASON_REMOVE_OBJECT_KEY);
			else 																				this.activeRecordSet.setUnsaved(Channel.UNSAVED_REASON_ADD_OBJECT_KEY);
		}
	}

	/**
	 * overloaded clear method to enable implementation specific clear actions
	 */
	public void clear() {
		for (String recordSetKey : this.getRecordSetNames()) {
			if (recordSetKey != null && recordSetKey.length() > 3) this.remove(recordSetKey);
		}

		super.clear();
		this.objectKey = OSDE.STRING_EMPTY;
	}
}

