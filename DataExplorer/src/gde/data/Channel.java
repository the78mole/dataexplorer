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
    
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
package gde.data;

import gde.GDE;
import gde.config.GraphicsTemplate;
import gde.config.Settings;
import gde.device.ChannelTypes;
import gde.log.Level;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.RecordSetNameComparator;
import gde.utils.StringHelper;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

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
	
	final int											number;							// 1
	String												channelConfigName;	// Ausgang
	String												name;								// 1 : Ausgang
	final ChannelTypes						type;								// ChannelTypes.TYPE_OUTLET or ChannelTypes.TYPE_CONFIG
	GraphicsTemplate							template;						// graphics template holds view configuration
	RecordSet											activeRecordSet;
	RecordSet											lastActiveRecordSet;
	String												objectKey	= GDE.STRING_EMPTY;
	String 												fileName;
	String												fileDescription		= StringHelper.getDate();
	boolean												isSaved = false;
	final DataExplorer						application;
	final Channels								parent;
	Comparator<String> 						comparator = new RecordSetNameComparator();
	
	public final static String		UNSAVED_REASON_ADD_OBJECT_KEY	= Messages.getString(MessageIds.GDE_MSGT0400);
	public final static String		UNSAVED_REASON_REMOVE_OBJECT_KEY	= Messages.getString(MessageIds.GDE_MSGT0401);
	public final static String		UNSAVED_REASON_CHANGED_OBJECT_DATA	= Messages.getString(MessageIds.GDE_MSGT0402);


	/**
	 * constructor where channel configuration name is used with the channels.ordinal+1 to construct the channel name
	 * @param useChannelConfigName channelNumber 1 -> " 1 : Ausgang 1"
	 */
	public Channel(String useChannelConfigName, ChannelTypes channelType) {
		super(1);
		this.application = DataExplorer.getInstance();
		this.parent = Channels.getInstance(this.application);
		this.number = this.parent.size() + 1;
		this.channelConfigName = useChannelConfigName;
		this.name = GDE.STRING_BLANK + this.number + GDE.STRING_BLANK_COLON_BLANK + useChannelConfigName;
		this.type = channelType;
		
		String templateFileName = this.application.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + this.name.split(GDE.STRING_COLON)[0].trim();
		this.template = new GraphicsTemplate(templateFileName);
		this.fileDescription = DataExplorer.getInstance().isObjectoriented() 
			? this.fileDescription + GDE.STRING_BLANK + this.application.getObjectKey() : this.fileDescription;
	}

	/**
	 * constructor where channel configuration name is used with the channels.ordinal+1 to construct the channel name and a new record set will be added asap
	 * @param useChannelConfigName
	 * @param channelType
	 * @param newRecordSet
	 */
	public Channel(String useChannelConfigName, ChannelTypes channelType, RecordSet newRecordSet) {
		super(1);
		this.application = DataExplorer.getInstance();
		this.parent = Channels.getInstance(this.application);
		this.number = this.parent.size() + 1;
		this.channelConfigName = useChannelConfigName;
		this.name = GDE.STRING_BLANK + this.number + GDE.STRING_BLANK_COLON_BLANK + useChannelConfigName;
		this.type = channelType;
		this.put(newRecordSet.getName(), newRecordSet);

		String templateFileName = this.application.getActiveDevice().getName() + GDE.STRING_UNDER_BAR + this.name.split(GDE.STRING_COLON)[0];
		this.template = new GraphicsTemplate(templateFileName);
		this.fileDescription = DataExplorer.getInstance().isObjectoriented() 
			? this.fileDescription + GDE.STRING_BLANK + this.application.getObjectKey() : this.fileDescription;
	}

	/**
	 * overwrites the size method to return faked size in case of channel type is ChannelTypes.TYPE_CONFIG
	 * TYPE_CONFIG means the all record sets depends to the object and the different (configuration) channels enable differnt views to it
	 */
	@Override
	public int size() {
		int size;
		if(this.type == ChannelTypes.TYPE_OUTLET) {
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
	 * method to calculate next record set number, usually a record starts with a number followed by ")"
	 * this method is used to build a new record set name while gathering data "3") flight record
	 * @return next record set number
	 */
	public int getNextRecordSetNumber() {
		int recordNumber = 1;
		if (this.size() != 0) {
			String[] sortedRecordSetNames = this.getRecordSetNames();
			for (int i = sortedRecordSetNames.length - 1; i >= 0; --i) {
				try {
					recordNumber = Integer.valueOf(sortedRecordSetNames[i].split("[)]")[0]) + 1; //$NON-NLS-1$
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
	public synchronized String[] getRecordSetNames() {
		String[] keys;
		if(this.type == ChannelTypes.TYPE_OUTLET) {
			keys = this.keySet().toArray( new String[1]);
		}
		else { // ChannelTypes.TYPE_CONFIG
			Channels channels = Channels.getInstance();
			Vector<String> namesVector = new Vector<String>();
			synchronized (channels) {
				for (int i = 1; i <= channels.size(); ++i) {
					String[] recordSetNames = channels.get(i).getUnsortedRecordSetNames();
					for (int j = 0; j < recordSetNames.length; j++) {
						if (recordSetNames[j] != null) namesVector.add(recordSetNames[j]);
					}
				}
			}
			keys = namesVector.toArray(new String[1]);
		}
		Arrays.sort(keys, this.comparator);
		return keys;
	}
	
	/**
	 * method to get unsorted recordNames within channels instance to avoid stack overflow due to never ending recursion 
	 * @return String[] containing the records names
	 */
	public synchronized String[] getUnsortedRecordSetNames() {
		return this.keySet().toArray( new String[1]);
	}

	/**
	 * query the last displayed record set name, in case of ChannelTypes.TYPE_CONFIG the last used entry is returned
	 */ 
	public String getLastActiveRecordSetName() {
//		if (this.type == ChannelTypes.TYPE_CONFIG && this.keySet() != null)
//			return this.keySet().toArray(new String[1])[0];
//		
		return this.lastActiveRecordSet == null ? this.getFirstRecordSetName() : this.lastActiveRecordSet.name;
	}

	/**
	 * set the last used record set
	 * @param lastRecordSet
	 */
	public void setLastActiveRecordSet(RecordSet lastRecordSet) {
		this.lastActiveRecordSet = lastRecordSet;
	}

	/**
	 * query the first record set name, in case of ChannelTypes.TYPE_CONFIG the first entry of keySet might returned
	 */ 
	public String getFirstRecordSetName() {
		if (this.type == ChannelTypes.TYPE_CONFIG && this.keySet() != null)
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
	public String getChannelConfigKey() {
		return this.channelConfigName;
	}

	/**
	 * method to get all the record sets of this channel
	 * @return HashMap<Integer, Records>
	 */
	public synchronized HashMap<String, RecordSet> getRecordSets() {
		HashMap<String, RecordSet> content = new HashMap<String, RecordSet>(this.size());
		if(this.type == ChannelTypes.TYPE_OUTLET) {
			for (String key : this.getRecordSetNames()) {
				content.put(key, this.get(key));
			}
		}
		else { // ChannelTypes.TYPE_CONFIG
			Channels channels = Channels.getInstance();
			synchronized (channels) {
				for (int i = 1; i <= channels.size(); ++i) {
					for (String key : channels.get(i).getUnsortedRecordSetNames()) {
						if (key != null && key.length() > 1) content.put(key, channels.get(i).get(key));
					}
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
				this.template.setProperty(i + Record.IS_VISIBLE, Boolean.valueOf(record.isVisible()).toString());
				this.template.setProperty(i + Record.IS_POSITION_LEFT, Boolean.valueOf(record.isPositionLeft()).toString());
				Color color = record.getColor();
				String rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
				this.template.setProperty(i + Record.COLOR, rgb);
				this.template.setProperty(i + Record.LINE_WITH, Integer.valueOf(record.getLineWidth()).toString());
				this.template.setProperty(i + Record.LINE_STYLE, Integer.valueOf(record.getLineStyle()).toString());
				this.template.setProperty(i + Record.IS_ROUND_OUT, Boolean.valueOf(record.isRoundOut()).toString());
				this.template.setProperty(i + Record.IS_START_POINT_ZERO, Boolean.valueOf(record.isStartpointZero()).toString());
				this.template.setProperty(i + Record.NUMBER_FORMAT, Integer.valueOf(record.getNumberFormat()).toString());
				this.template.setProperty(i + Record.IS_START_END_DEFINED, Boolean.valueOf(record.isStartEndDefined()).toString());
				this.template.setProperty(i + Record.DEFINED_MAX_VALUE, Double.valueOf(record.getMaxScaleValue()).toString());
				this.template.setProperty(i + Record.DEFINED_MIN_VALUE, Double.valueOf(record.getMinScaleValue()).toString());
				//smooth current droprecordS
				this.template.setProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, Boolean.toString(recordSet.isSmoothAtCurrentDrop()));
				recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
				// time grid
				color = recordSet.getColorTimeGrid();
				rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
				this.template.setProperty(RecordSet.TIME_GRID_COLOR, rgb);
				this.template.setProperty(RecordSet.TIME_GRID_LINE_STYLE, Integer.valueOf(recordSet.getLineStyleTimeGrid()).toString());
				this.template.setProperty(RecordSet.TIME_GRID_TYPE, Integer.valueOf(recordSet.getTimeGridType()).toString());
				// curve grid
				color = recordSet.getHorizontalGridColor();
				rgb = color.getRGB().red + GDE.STRING_COMMA + color.getRGB().green + GDE.STRING_COMMA + color.getRGB().blue;
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_COLOR, rgb);
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, Integer.valueOf(recordSet.getHorizontalGridLineStyle()).toString());
				this.template.setProperty(RecordSet.HORIZONTAL_GRID_TYPE, Integer.valueOf(recordSet.getHorizontalGridType()).toString());
				if (recordSet.get(recordSet.getHorizontalGridRecordName()) != null) {
					this.template.setProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, Integer.valueOf(recordSet.get(recordSet.getHorizontalGridRecordName()).ordinal).toString());
				}
			}
			this.template.store();
			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "creating graphics template file " + Settings.getInstance().getApplHomePath() + GDE.FILE_SEPARATOR_UNIX + this.getActiveRecordSet().getName() + this.name); //$NON-NLS-1$
		}
	}
	
	/**
	 * method to apply the graphics template definition colors to an record set
	 */
	public void applyTemplateBasics(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);
		this.activeRecordSet = this.application.getActiveRecordSet();
		if (recordSet != null) {
			log.log(Level.OFF, "this.size() > 1 " + (this.size() > 1) + "; this.lastActiveRecordSet = " + this.getLastActiveRecordSetName() +  " - " + this.get(this.getLastActiveRecordSetName()).getChannelConfigNumber() + "!=" +  this.get(recordSetKey).getChannelConfigNumber());
			if (this.size() <= 1 || (this.type == ChannelTypes.TYPE_CONFIG && this.get(this.getLastActiveRecordSetName()).getChannelConfigNumber() != this.get(recordSetKey).getChannelConfigNumber())) { //apply values from template
				if (this.template != null) this.template.load();
				if (this.template != null && this.template.isAvailable()) {
					log.log(Level.FINER, "name = " + this.template.getDefaultFileName());
					for (int i = 0; i < recordSet.realSize(); ++i) {
						Record record = recordSet.get(i);
						record.setVisible(Boolean.valueOf(this.template.getProperty(i + Record.IS_VISIBLE, "true"))); //$NON-NLS-1$
						record.setPositionLeft(Boolean.valueOf(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true"))); //$NON-NLS-1$
						int r, g, b;
						String color = this.template.getProperty(i + Record.COLOR, "128,128,255"); //$NON-NLS-1$
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						record.setColor(SWTResourceManager.getColor(r, g, b));
						record.setLineWidth(Integer.valueOf(this.template.getProperty(i + Record.LINE_WITH, "1")).intValue()); //$NON-NLS-1$
						record.setLineStyle(Integer.valueOf(this.template.getProperty(i + Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID)).intValue());
						record.setRoundOut(Boolean.valueOf(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"))); //$NON-NLS-1$
						record.setStartpointZero(Boolean.valueOf(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false"))); //$NON-NLS-1$
						record.setStartEndDefined(Boolean.valueOf(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")), new Double(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")) //$NON-NLS-1$ //$NON-NLS-2$
								.doubleValue(), new Double(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "0")).doubleValue()); //$NON-NLS-1$
						record.setNumberFormat(Integer.valueOf(this.template.getProperty(i + Record.NUMBER_FORMAT, "1")).intValue()); //$NON-NLS-1$
						//smooth current drop
						recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
						// time grid
						color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
						recordSet.setTimeGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
						recordSet.setTimeGridType(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
						// curve grid
						color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
						r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
						g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
						b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
						recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
						recordSet.setHorizontalGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
						recordSet.setHorizontalGridType(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
						recordSet.setHorizontalGridRecordOrdinal(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, "-1")).intValue()); //$NON-NLS-1$
					}
				}
			}
			else { //take over values from last active record set
				RecordSet _lastActiveRecordSet = this.get(this.getLastActiveRecordSetName());
				for (int i = 0; i < recordSet.realSize(); ++i) {
					Record record = recordSet.get(i);
					Record lastActiveRecord = _lastActiveRecordSet.get(i);
					if(log.isLoggable(Level.FINER)) log.log(Level.FINER, "lastActiveRecord = " + lastActiveRecord.name + " isVisible=" + lastActiveRecord.isVisible + " isPositionLeft=" + lastActiveRecord.isPositionLeft + " isStartpointZero=" + lastActiveRecord.isStartpointZero);
					record.setVisible(lastActiveRecord.isVisible);
					record.setPositionLeft(lastActiveRecord.isPositionLeft);
					record.setColor(lastActiveRecord.color);
					record.setLineWidth(lastActiveRecord.lineWidth);
					record.setLineStyle(lastActiveRecord.lineStyle);
					record.setRoundOut(lastActiveRecord.isRoundOut); //$NON-NLS-1$
					record.setStartpointZero(lastActiveRecord.isStartpointZero); //$NON-NLS-1$
					record.setStartEndDefined(lastActiveRecord.isStartEndDefined, lastActiveRecord.getMinScaleValue(), lastActiveRecord.getMaxScaleValue());
					record.setNumberFormat(lastActiveRecord.numberFormat); //$NON-NLS-1$
					//smooth current drop
					recordSet.setSmoothAtCurrentDrop(lastActiveRecord.parent.isSmoothAtCurrentDrop);
					// time grid
					recordSet.setTimeGridColor(lastActiveRecord.parent.timeGridColor);
					recordSet.setTimeGridLineStyle(lastActiveRecord.parent.timeGridLineStyle);
					recordSet.setTimeGridType(lastActiveRecord.parent.timeGridType);
					// curve grid
					recordSet.setHorizontalGridColor(lastActiveRecord.parent.horizontalGridColor);
					recordSet.setHorizontalGridLineStyle(lastActiveRecord.parent.horizontalGridLineStyle);
					recordSet.setHorizontalGridType(lastActiveRecord.parent.horizontalGridType);
					recordSet.setHorizontalGridRecordOrdinal(lastActiveRecord.parent.horizontalGridRecordOrdinal >= 0 ? lastActiveRecord.parent.horizontalGridRecordOrdinal : 0);
					if(log.isLoggable(Level.FINER)) log.log(Level.FINER, "record = " + record.name + " isVisible=" + record.isVisible + " isPositionLeft=" + record.isPositionLeft + " isStartpointZero=" + record.isStartpointZero);
				}
			}

			if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "applied graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
			if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && this.application.getMenuBar() != null) {
				this.application.updateGraphicsWindow();
			}
		}
	}	

	/**
	 * method to apply the graphics template definition to an record set
	 * @param recordSetKey
	 * @param doUpdateVisibilityStatus
	 */
	public void applyTemplate(String recordSetKey, boolean doUpdateVisibilityStatus) {
		RecordSet recordSet = this.get(recordSetKey);
		this.activeRecordSet = this.application.getActiveRecordSet();
		if (recordSet != null) {
			if (this.template != null) this.template.load();
			if (this.template != null && this.template.isAvailable()) {
				for (int i = 0; i < recordSet.getRecordNames().length; ++i) {
					Record record = recordSet.get(recordSet.getRecordNames()[i]);
					record.setVisible(Boolean.valueOf(this.template.getProperty(i + Record.IS_VISIBLE, "true"))); //$NON-NLS-1$
					record.setPositionLeft(Boolean.valueOf(this.template.getProperty(i + Record.IS_POSITION_LEFT, "true"))); //$NON-NLS-1$
					int r, g, b;
					String color = this.template.getProperty(i + Record.COLOR, "128,128,255"); //$NON-NLS-1$
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					record.setColor(SWTResourceManager.getColor(r, g, b));
					record.setLineWidth(Integer.valueOf(this.template.getProperty(i + Record.LINE_WITH, "1")).intValue()); //$NON-NLS-1$
					record.setLineStyle(Integer.valueOf(this.template.getProperty(i + Record.LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_SOLID)).intValue());
					record.setRoundOut(Boolean.valueOf(this.template.getProperty(i + Record.IS_ROUND_OUT, "false"))); //$NON-NLS-1$
					record.setStartpointZero(Boolean.valueOf(this.template.getProperty(i + Record.IS_START_POINT_ZERO, "false"))); //$NON-NLS-1$
					record.setStartEndDefined(Boolean.valueOf(this.template.getProperty(i + Record.IS_START_END_DEFINED, "false")), new Double(this.template.getProperty(i + Record.DEFINED_MIN_VALUE, "0")) //$NON-NLS-1$ //$NON-NLS-2$
							.doubleValue(), new Double(this.template.getProperty(i + Record.DEFINED_MAX_VALUE, "0")).doubleValue()); //$NON-NLS-1$
					record.setNumberFormat(Integer.valueOf(this.template.getProperty(i + Record.NUMBER_FORMAT, "1")).intValue()); //$NON-NLS-1$
					//smooth current drop
					recordSet.setSmoothAtCurrentDrop(Boolean.valueOf(this.template.getProperty(RecordSet.SMOOTH_AT_CURRENT_DROP, "false"))); //$NON-NLS-1$
					// time grid
					color = this.template.getProperty(RecordSet.TIME_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					recordSet.setTimeGridColor(SWTResourceManager.getColor(r, g, b));
					recordSet.setTimeGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
					recordSet.setTimeGridType(Integer.valueOf(this.template.getProperty(RecordSet.TIME_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
					// curve grid
					color = this.template.getProperty(RecordSet.HORIZONTAL_GRID_COLOR, "128,128,128"); //$NON-NLS-1$
					r = Integer.valueOf(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
					g = Integer.valueOf(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
					b = Integer.valueOf(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
					recordSet.setHorizontalGridColor(SWTResourceManager.getColor(r, g, b));
					recordSet.setHorizontalGridLineStyle(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_LINE_STYLE, GDE.STRING_EMPTY + SWT.LINE_DOT)).intValue());
					recordSet.setHorizontalGridType(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_TYPE, "0")).intValue()); //$NON-NLS-1$
					recordSet.setHorizontalGridRecordOrdinal(Integer.valueOf(this.template.getProperty(RecordSet.HORIZONTAL_GRID_RECORD_ORDINAL, "-1")).intValue()); //$NON-NLS-1$
				}
				recordSet.setUnsaved(RecordSet.UNSAVED_REASON_GRAPHICS);
				if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "applied graphics template file " + this.template.getCurrentFilePath()); //$NON-NLS-1$
				if (doUpdateVisibilityStatus) {
					recordSet.device.updateVisibilityStatus(recordSet, false);
				}
				if (this.activeRecordSet != null && recordSet.getName().equals(this.activeRecordSet.name) && this.application.getMenuBar() != null) {
					this.application.updateGraphicsWindow();
				}
			}
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
		this.application.checkUpdateFileComment();
		this.application.checkUpdateRecordSetComment();
		
		RecordSet newActiveRecordSet = this.get(recordSetKey);
		if (newActiveRecordSet != null) {
			this.activeRecordSet = newActiveRecordSet;
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
	public synchronized void switchRecordSet(String recordSetName) {
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, String.format("switching to record set threadId = %06d", Thread.currentThread().getId())); //$NON-NLS-1$
		int percentage = this.application.getProgressPercentage();
		if (percentage > 99 || percentage == 0)
			this.application.setProgress(0, null);
		final Channel activeChannel = this;
		final String recordSetKey = recordSetName;
		this.lastActiveRecordSet = this.get(recordSetKey);
		if (Thread.currentThread().getId() == this.application.getThreadId()) {
			updateForSwitchRecordSet(activeChannel, recordSetKey);
		}
		else { // execute asynchronous
			GDE.display.asyncExec(new Runnable() {
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
			
			this.application.getMenuToolBar().updateRecordSetSelectCombo();
			this.application.getMenuToolBar().updateGoogleEarthToolItem();
			this.application.cleanHeaderAndCommentInGraphicsWindow();
			this.application.updateAllTabs(true);
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
		for (Integer tmpNumber : Channels.getInstance().keySet()) {
			Channel channel = channels.get(tmpNumber);
			if (channel.get(recordSetKey) != null) {
				channelNumber = tmpNumber.intValue();
			}
		}
		return channelNumber;
	}
	
	/**
	 * @return the type as ordinal
	 */
	public ChannelTypes getType() {
		return this.type;
	}

	/**
	 * @param newName the name to set
	 */
	public void setName(String newName) {
		this.name = newName;
	}

	public String getFileName() {
		return this.fileName!= null ? this.fileName.substring(this.fileName.lastIndexOf(GDE.FILE_SEPARATOR_UNIX)+1) : null;
	}

	public String getFullQualifiedFileName() {
		return this.fileName;
	}

	public void setFileName(String newFileName) {
		if(this.type == ChannelTypes.TYPE_CONFIG) {
			Channels channels = Channels.getInstance();
			for (int i = 1; i<= channels.getChannelNames().length; ++i) {
				channels.get(i).fileName = newFileName;
			}
		}
		else {
			this.fileName = newFileName;
		}
		if (this.fileName != null && this.application.getActiveDevice() != null) this.application.updateTitleBar(this.application.getObjectKey(), this.application.getActiveDevice().getName(), this.application.getActiveDevice().getPort());
	}


	public String getFileDescription() {
		return this.fileDescription;
	}

	public void setFileDescription(String newFileDescription) {
		this.fileDescription = newFileDescription;
		this.application.updateGraphicsCaptions();
	}

	public boolean isSaved() {
		return this.isSaved;
	}

	public void setSaved(boolean is_saved) {
		if(this.type == ChannelTypes.TYPE_CONFIG) {
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
	 * set a unsaved reason marker to enable unsaved data warning
	 * valid arguments are UNSAVED_REASON_ADD_OBJECT_KEY, UNSAVED_REASON_REMOVE_OBJECT_KEY, UNSAVED_REASON_CHANGED_OBJECT_DATA
	 * @param unsavedReason
	 */
	public void setUnsaved(String unsavedReason) {
		this.activeRecordSet = this.getActiveRecordSet();
		if (this.activeRecordSet != null) {
			this.activeRecordSet.setUnsaved(unsavedReason);
		}
	}

	
	/**
	 * check if all record sets have its data loaded, if required load data from file
	 * this method can be used to check prior to save modified data
	 * the behavior which record set data is checked and loaded depends on the method this.getRecordSetNames() 
	 */
	public void checkAndLoadData() {
		String fullQualifiedFileName = this.getFullQualifiedFileName();
		for (String tmpRecordSetName : this.getRecordSetNames()) {
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "tmpRecordSetName = " + tmpRecordSetName); //$NON-NLS-1$
			Channel activeChannel = Channels.getInstance().getActiveChannel();
			if (activeChannel != null) {
				//if ChannelTypes.TYPE_OUTLET only record sets associated to that channel goes into one file
				//if ChannelTypes.TYPE_CONFIG all record sets with different configurations goes into one file
				Channel selectedChannel = activeChannel.getType().equals(ChannelTypes.TYPE_OUTLET) ? activeChannel : Channels.getInstance().get(this.findChannelOfRecordSet(tmpRecordSetName));
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "selectedChannel = " + (selectedChannel != null ? selectedChannel.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
				if (selectedChannel != null) {
					RecordSet tmpRecordSet = selectedChannel.get(tmpRecordSetName);
					if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "tmpRecordSet = " + (tmpRecordSet != null ? tmpRecordSet.getName() : "null")); //$NON-NLS-1$ //$NON-NLS-2$
					if (tmpRecordSet != null && !tmpRecordSet.hasDisplayableData()) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "tmpRecordSetName needs data to loaded"); //$NON-NLS-1$
						if (tmpRecordSet.fileDataSize != 0 && tmpRecordSet.fileDataPointer != 0) {
							if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "loading data ..."); //$NON-NLS-1$
							tmpRecordSet.loadFileData(fullQualifiedFileName, this.application.getStatusBar() != null);
						}
					}
				}
			}
		}
	}

	/**
	 * @return the channel/config number
	 */
	public int getNumber() {
		return this.number;
	}

	public String getObjectKey() {
		return this.objectKey;
	}

	public void setObjectKey(String newObjectKey) {
		this.objectKey = newObjectKey;
		if (this.activeRecordSet != null) {
			if (newObjectKey.equals(GDE.STRING_EMPTY))	this.activeRecordSet.setUnsaved(Channel.UNSAVED_REASON_REMOVE_OBJECT_KEY);
			else 																				this.activeRecordSet.setUnsaved(Channel.UNSAVED_REASON_ADD_OBJECT_KEY);
		}
	}

	/**
	 * overloaded clear method to enable implementation specific clear actions
	 */
	@Override
	public void clear() {
		for (String recordSetKey : this.getRecordSetNames()) {
			if (recordSetKey != null && recordSetKey.length() > 3) this.remove(recordSetKey);
		}

		super.clear();
		this.objectKey = GDE.STRING_EMPTY;
	}
}

