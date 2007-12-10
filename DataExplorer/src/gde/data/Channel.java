package osde.data;

import java.util.HashMap;
import java.util.logging.Logger;

import osde.config.GraphicsTemplate;
import osde.config.Settings;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * Channel represents on channel (Kanal 1, Kanal 2, ...) where data record sets are accessible (1) laden, 2)Entladen, 1) Flugaufzeichnung, ..)
 */
public class Channel extends HashMap<String, RecordSet> {
	static final long											serialVersionUID	= 26031957;
	private Logger												log								= Logger.getLogger(this.getClass().getName());
	private String												fileSep = System.getProperty("file.separator");
	private String												name;																														// K1: Kanal1
	private GraphicsTemplate							template;																												// graphics template holds view configuration
	private RecordSet											activeRecordSet;
	private final OpenSerialDataExplorer	application;

	/**
	 * Constructor, where channelNumber is used to calculate the name of the channel K1: Kanal1
	 * @param name
	 * @param recordNames
	 */
	public Channel(int channelNumber) {
		super(5);
		this.name = "K" + channelNumber + ": Kanal " + channelNumber;

		this.application = OpenSerialDataExplorer.getInstance();
		String filename = application.getActiveConfig().getName() + "_" + this.name.split(":")[0];
		this.template = new GraphicsTemplate(Settings.getInstance().getApplHomePath(), filename);
	}

	/**
	 * Constructor, where channelNumber is used to calculate the name of the channel K1: Kanal1
	 * @param name
	 * @param recordNames
	 */
	public Channel(int channelNumber, RecordSet newRecordSet) {
		super(5);
		this.name = "K" + channelNumber + ": Kanal " + channelNumber;
		this.put(newRecordSet.getName(), newRecordSet);

		this.application = OpenSerialDataExplorer.getInstance();
		String filename = application.getActiveConfig().getName() + "_" + this.name.split(":")[0];
		this.template = new GraphicsTemplate(Settings.getInstance().getApplHomePath(), filename);
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
		return this.keySet().toArray(new String[1]);
	}

	/**
	 * get the name of the channel "K1: Kanal1"
	 * @return String
	 */
	public String getName() {
		return name;
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

		for (String recordName : recordSet.getRecordNames()) {
			Record record = recordSet.get(recordName);
			template.setProperty(recordName + Record.IS_VISIBLE, new Boolean(record.isVisible()).toString());
			template.setProperty(recordName + Record.IS_POSITION_LEFT, new Boolean(record.isPositionLeft()).toString());
			String rgb = record.getColor().getRGB().red + "," + record.getColor().getRGB().green + "," + record.getColor().getRGB().blue;
			template.setProperty(recordName + Record.COLOR, rgb);
			template.setProperty(recordName + Record.LINE_WITH, new Integer(record.getLineWidth()).toString());
			template.setProperty(recordName + Record.LINE_STYLE, new Integer(record.getLineStyle()).toString());
			template.setProperty(recordName + Record.IS_ROUND_OUT, new Boolean(record.isRoundOut()).toString());
			template.setProperty(recordName + Record.IS_START_POINT_ZERO, new Boolean(record.isStartpointZero()).toString());
			template.setProperty(recordName + Record.NUMBER_FORMAT, new Integer(record.getNumberFormat()).toString());
			//template.setProperty(recordName + Record.MAX_VALUE, new Double(record.getMaxValue()).toString());
			//template.setProperty(recordName + Record.MIN_VALUE, new Double(record.getMinValue()).toString());
			template.setProperty(recordName + Record.IS_START_END_DEFINED, new Boolean(record.isStartEndDefined()).toString());
			template.setProperty(recordName + Record.DEFINED_MAX_VALUE, new Double(record.getDefinedMaxValue()).toString());
			template.setProperty(recordName + Record.DEFINED_MIN_VALUE, new Double(record.getDefinedMinValue()).toString());
		}
		template.store();
		log.fine("creating graphics template file " + Settings.getInstance().getApplHomePath() + fileSep + this.getActiveRecordSet().getName() + this.name);
	}

	/**
	 * method to apply the graphics template definition to an record set
	 */
	public void applyTemplate(String recordSetKey) {
		RecordSet recordSet = this.get(recordSetKey);

		if (template != null) template.load();

		if (template.isAvailable()) {
			for (String recordName : recordSet.getRecordNames()) {
				Record record = recordSet.get(recordName);
				record.setVisible(new Boolean(template.getProperty(recordName + Record.IS_VISIBLE)).booleanValue());
				record.setPositionLeft(new Boolean(template.getProperty(recordName + Record.IS_POSITION_LEFT)).booleanValue());
				int r, g, b;
				r = new Integer(((String) (template.get(recordName + Record.COLOR))).split(",")[0]).intValue();
				g = new Integer(((String) (template.get(recordName + Record.COLOR))).split(",")[1]).intValue();
				b = new Integer(((String) (template.get(recordName + Record.COLOR))).split(",")[2]).intValue();
				record.setColor(SWTResourceManager.getColor(r, g, b));
				record.setLineWidth(new Integer(template.getProperty(recordName + Record.LINE_WITH)).intValue());
				record.setLineStyle(new Integer(template.getProperty(recordName + Record.LINE_STYLE)).intValue());
				record.setRoundOut(new Boolean(template.getProperty(recordName + Record.IS_ROUND_OUT)).booleanValue());
				record.setStartpointZero(new Boolean(template.getProperty(recordName + Record.IS_START_POINT_ZERO)).booleanValue());
				record.setStartEndDefined(new Boolean(template.getProperty(recordName + Record.IS_START_END_DEFINED)).booleanValue(), new Double(template.getProperty(recordName + Record.DEFINED_MIN_VALUE))
						.doubleValue(), new Double(template.getProperty(recordName + Record.DEFINED_MAX_VALUE)).doubleValue());
				record.setNumberFormat(new Integer(template.getProperty(recordName + Record.NUMBER_FORMAT)).intValue());
				//record.setMaxValue(new Double(template.getProperty(recordName + Record.DEFINED_MAX_VALUE)).intValue());
				//record.setMinValue(new Double(template.getProperty(recordName + Record.DEFINED_MIN_VALUE)).intValue());
			}
			log.fine("applied graphics template file " + template.getCurrentFilePath());
		}
	}

	/**
	 * @return the activeRecordSet
	 */
	public RecordSet getActiveRecordSet() {
		return this.activeRecordSet;
	}

	/**
	 * @param activeRecordSet the activeRecordSet to set
	 */
	public void setActiveRecordSet(String recordSetKey) {
		this.activeRecordSet = this.get(recordSetKey);
	}
}
