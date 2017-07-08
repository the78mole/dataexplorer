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

    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016,2017 Winfried Bruegmann
    							2016,2017 Thomas Eickert
****************************************************************************************/
package gde.config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.MemoryHandler;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.xml.sax.SAXException;

import gde.GDE;
import gde.device.DeviceConfiguration;
import gde.exception.ApplicationConfigurationException;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.FileUtils;
import gde.utils.RecordSetNameComparator;
import gde.utils.StringHelper;
import gde.utils.WaitTimer;

/**
 * Settings class will read and write/update application settings, like window size and default path ....
 * @author Winfried Brügmann
 */
public class Settings extends Properties {
	private final static long				serialVersionUID								= 26031957;
	final static Logger							log															= Logger.getLogger(Settings.class.getName());
	final static String							$CLASS_NAME											= Settings.class.getName();

	private static Settings					instance												= null;																																														// singelton

	// JAXB XML environment
	Schema													schema;
	JAXBContext											jc;
	Unmarshaller										unmarshaller;
	Marshaller											marshaller;
	String													xmlBasePath;
	Thread													xsdThread;
	Thread													migrationThread;

	public static final String			EMPTY														= "---";																																													//$NON-NLS-1$
	public static final String			EMPTY_SIGNATURE									= Settings.EMPTY + GDE.STRING_SEMICOLON + Settings.EMPTY + GDE.STRING_SEMICOLON + Settings.EMPTY;
	static final String							UNIX_PORT_DEV_TTY								= "/dev/tty";																																											//$NON-NLS-1$
	static final String							WINDOWS_PORT_COM								= "COM";																																													//$NON-NLS-1$
	static final String							PERMISSION_555									= "555";																																													//$NON-NLS-1$
	static final String							PATH_RESOURCE										= "resource/";																																										//$NON-NLS-1$
	static final String							PATH_RESOURCE_TEMPLATE					= "resource/template/";																																						//$NON-NLS-1$

	final static String							HEADER_TEXT											= "# -- DataExplorer Settings File -- ";																													//$NON-NLS-1$
	final static String							DEVICE_BLOCK										= "#[Actual-Device-Port-Settings]";																																// Picolario;Renschler;COM2 //$NON-NLS-1$
	final static String							WINDOW_BLOCK										= "#[Window-Settings]";																																						//$NON-NLS-1$
	final static String							WINDOW_MAXIMIZED								= "window_maximized";																																							//$NON-NLS-1$
	final static String							WINDOW_LEFT											= "window_left";																																									//$NON-NLS-1$
	final static String							WINDOW_TOP											= "window_top";																																										//$NON-NLS-1$
	final static String							WINDOW_WIDTH										= "window_width";																																									//$NON-NLS-1$
	final static String							WINDOW_HEIGHT										= "window_height";																																								//$NON-NLS-1$
	final static String							COOLBAR_ORDER										= "coolbar_order";																																								//$NON-NLS-1$
	final static String							COOLBAR_WRAPS										= "coolbar_wraps";																																								//$NON-NLS-1$
	final static String							COOLBAR_SIZES										= "coolbar_sizes";																																								//$NON-NLS-1$
	final static String							RECORD_COMMENT_VISIBLE					= "record_comment_visible";																																				//$NON-NLS-1$
	final static String							GRAPHICS_HEADER_VISIBLE					= "graphics_header_visible";																																			//$NON-NLS-1$
	final static String							GRAPHICS_AREA_BACKGROUND				= "graphics_area_background";																																			//$NON-NLS-1$
	final static String							GRAPHICS_SURROUND_BACKGRD				= "graphics_surround_backgrd";																																		//$NON-NLS-1$
	final static String							GRAPHICS_BORDER_COLOR						= "graphics_border_color";																																				//$NON-NLS-1$
	final static String							IS_GRAPHICS_SCALE_COLOR					= "is_graphics_scale_color";																																			//$NON-NLS-1$
	final static String							IS_GRAPHICS_NUMBERS_COLOR				= "is_graphics_number_color";																																			//$NON-NLS-1$
	final static String							IS_GRAPHICS_NAME_COLOR					= "is_graphics_text_color";																																				//$NON-NLS-1$
	final static String							COMPARE_AREA_BACKGROUND					= "compare_area_background";																																			//$NON-NLS-1$
	final static String							COMPARE_SURROUND_BACKGRD				= "compare_surround_backgrd";																																			//$NON-NLS-1$
	final static String							COMPARE_BORDER_COLOR						= "compare_border_color";																																					//$NON-NLS-1$
	final static String							IS_COMPARE_CHANNELCONFIG				= "is_compare_channel_config_name";																																//$NON-NLS-1$
	final static String							UTILITY_AREA_BACKGROUND					= "utility_area_background";																																			//$NON-NLS-1$
	final static String							UTILITY_SURROUND_BACKGRD				= "utility_surround_backgrd";																																			//$NON-NLS-1$
	final static String							UTILITY_BORDER_COLOR						= "utility_border_color";																																					//$NON-NLS-1$
	final static String							STATISTICS_INNER_BACKGROUND			= "statistics_inner_background";																																	//$NON-NLS-1$
	final static String							STATISTICS_SURROUND_BACKGRD			= "statistics_surround_backgrd";																																	//$NON-NLS-1$
	final static String							ANALOG_INNER_BACKGROUND					= "analog_inner_background";																																			//$NON-NLS-1$
	final static String							ANALOG_SURROUND_BACKGRD					= "analog_surround_backgrd";																																			//$NON-NLS-1$
	final static String							DIGITAL_INNER_BACKGROUND				= "digital_inner_background";																																			//$NON-NLS-1$
	final static String							DIGITAL_SURROUND_BACKGRD				= "digital_surround_backgrd";																																			//$NON-NLS-1$
	final static String							CELL_VOLTAGE_INNER_BACKGROUND		= "cell_voltage_inner_background";																																//$NON-NLS-1$
	final static String							CELL_VOLTAGE_SURROUND_BACKGRD		= "cell_voltage_surround_backgrd";																																//$NON-NLS-1$
	final static String							FILE_COMMENT_INNER_BACKGROUND		= "file_comment_inner_background";																																//$NON-NLS-1$
	final static String							FILE_COMMENT_SURROUND_BACKGRD		= "file_comment_surround_backgrd";																																//$NON-NLS-1$
	final static String							OBJECT_DESC_INNER_BACKGROUND		= "object_desciption_inner_background";																														//$NON-NLS-1$
	final static String							OBJECT_DESC_SURROUND_BACKGRD		= "object_desciption_surround_backgrd";																														//$NON-NLS-1$
	final static String							DISPLAY_DENSITY_FONT_CORRECT		= "display_density_font_correction";																															//$NON-NLS-1$

	final static String							IS_HISTO_ACTIVE									= "is_histo_active";																																							//$NON-NLS-1$
	final static String							IS_SMART_STATISTICS							= "is_smart_statistics";																																					//$NON-NLS-1$
	final static String							BOXPLOT_SCALE_ORDINAL						= "boxplot_scale_ordinal";																																				//$NON-NLS-1$
	final static String							BOXPLOT_SIZE_ADAPTATION_ORDINAL	= "boxplot_size_adaptation_ordinal";																															//$NON-NLS-1$
	final static String							X_SPREAD_GRADE_ORDINAL					= "x_spread_grade_ordinal";																																				//$NON-NLS-1$
	final static String							IS_X_LOGARITHMIC_DISTANCE				= "is_x_logarithmic_distance";																																		//$NON-NLS-1$
	final static String							IS_X_REVERSED										= "is_x_reversed";																																								//$NON-NLS-1$
	final static String							SEARCH_IMPORT_PATH							= "search_import_path";																																						//$NON-NLS-1$
	final static String							SEARCH_DATAPATH_IMPORTS					= "search_datapath_imports";																																			//$NON-NLS-1$
	final static String							IS_CHANNEL_MIX									= "is_channel_mix";																																								//$NON-NLS-1$
	final static String							SAMPLING_TIMESPAN_ORDINAL				= "sampling_timespan_ordinal";																																		//$NON-NLS-1$
	final static String							FILES_WITHOUT_OBJECT						= "files_without_object";																																					//$NON-NLS-1$
	final static String							FILES_WITH_OTHER_OBJECT					= "files_with_other_object";																																			//$NON-NLS-1$
	final static String							RETROSPECT_MONTHS								= "retrospect_months";																																						//$NON-NLS-1$
	final static String							IS_ZIPPED_CACHE									= "zipped_cache";																																									//$NON-NLS-1$
	final static String							MINMAX_QUANTILE_DISTANCE				= "minmax_quantile_distance";																																			//$NON-NLS-1$
	final static String							ABSOLUTE_TRANSITION_LEVEL				= "absolute_transition_level";																																		//$NON-NLS-1$
	final static String							IS_DATETIME_UTC									= "is_datetime_utc";																																							//$NON-NLS-1$
	final static String							IS_DISPLAY_SETTLEMENTS					= "is_display_settlements";																																				//$NON-NLS-1$
	final static String							IS_DISPLAY_SCORES								= "is_display_scores";																																						//$NON-NLS-1$
	final static String							IS_DISPLAY_TAGS									= "is_display_tags";																																							//$NON-NLS-1$
	final static String							IS_DATA_SETTINGS_AT_HOME_PATH		= "is_data_settings_at_home_path";																																//$NON-NLS-1$
	final static String							IS_SUPPRESS_MODE								= "is_suppress_mode";																																							//$NON-NLS-1$
	final static String							IS_CURVE_SURVEY									= "is_curve_survey";																																							//$NON-NLS-1$
	final static String							GPS_LOCATION_RADIUS							= "gps_location_radius";																																					//$NON-NLS-1$
	final static String							GPS_ADDRESS_TYPE								= "gps_address_type";																																							//$NON-NLS-1$
	final static String							SUBDIRECTORY_LEVEL_MAX					= "subdirectory_level_max";																																				//$NON-NLS-1$
	final static String							IS_DATA_TABLE_TRANSITIONS					= "is_data_table_transitions";																																				//$NON-NLS-1$

	final static String							FILE_HISTORY_BLOCK							= "#[File-History-List]";																																					//$NON-NLS-1$
	final static String							FILE_HISTORY_BEGIN							= "history_file_";																																								//$NON-NLS-1$
	List<String>										fileHistory											= new ArrayList<String>();

	final static String							APPL_BLOCK											= "#[Program-Settings]";																																					//$NON-NLS-1$
	final static String							TABLE_BLOCK											= "#[Table-Settings]";																																						//$NON-NLS-1$
	final static String							LOGGING_BLOCK										= "#[Logging-Settings]";																																					//$NON-NLS-1$
	final static String							HISTO_BLOCK											= "#[Histo-Settings]";																																						//$NON-NLS-1$
	final static String							LOG_PATH												= "Logs";																																													//$NON-NLS-1$
	final static String							LOG_FILE												= "trace.log";																																										//$NON-NLS-1$
	final static String							SERIAL_LOG_FILE									= "serial.log";																																										//$NON-NLS-1$
	public final static String[]		LOGGING_LEVEL										= new String[] { "SEVERE", "WARNING", "TIME", "INFO", "FINE", "FINER", "FINEST" };								//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$

	public final static String			ACTIVE_DEVICE										= "active_device";																																								//$NON-NLS-1$
	public final static String			OBJECT_LIST											= "object_list";																																									//$NON-NLS-1$
	public final static String			ACTIVE_OBJECT										= "active_object";																																								//$NON-NLS-1$
	public final static String			DATA_FILE_PATH									= "data_file_path";																																								//$NON-NLS-1$
	public final static String			OBJECT_IMAGE_FILE_PATH					= "object_image_file_path";																																				//$NON-NLS-1$
	public final static String			LIST_SEPARATOR									= "list_separator";																																								//$NON-NLS-1$
	public final static String			DECIMAL_SEPARATOR								= "decimal_separator";																																						//$NON-NLS-1$
	public final static String			USE_DATA_FILE_NAME_LEADER				= "use_date_file_name_leader";																																		//$NON-NLS-1$
	public final static String			USE_OBJECT_KEY_IN_FILE_NAME			= "use_object_key_in_file_name";																																	//$NON-NLS-1$
	public final static String			WRITE_TMP_FILES									= "write_tmp_files";																																							//$NON-NLS-1$
	public static final String			ALPHA_BLENDING_VALUE						= "alpha_blending_value";																																					//$NON-NLS-1$
	public static final String			APLHA_BLENDING_ENABLED					= "aplha_blending_enabled";																																				//$NON-NLS-1$
	public static final String			KEEP_IMPORT_DIR_OBJECT_RELATED	= "keep_import_dir_object_related";																																//$NON-NLS-1$
	public final static String			IS_GLOBAL_PORT									= "is_global_port";																																								//$NON-NLS-1$
	public final static String			GLOBAL_PORT_NAME								= "global_port_name";																																							//$NON-NLS-1$
	public final static String			SKIP_BLUETOOTH_DEVICES					= "skip_bluetooth_devices";																																				//$NON-NLS-1$
	public final static String			DO_PORT_AVAILABLE_TEST					= "do_port_available_test";																																				//$NON-NLS-1$
	public final static String			IS_PORT_BLACKLIST								= "is_port_black_list";																																						//$NON-NLS-1$
	public final static String			PORT_BLACKLIST									= "port_black_list";																																							//$NON-NLS-1$
	public final static String			IS_PORT_WHITELIST								= "is_port_white_list";																																						//$NON-NLS-1$
	public final static String			PORT_WHITELIST									= "port_white_list";																																							//$NON-NLS-1$
	public final static String			DEVICE_DIALOG_USE_MODAL					= "device_dialogs_modal";																																					//$NON-NLS-1$
	public static final String			DEVICE_DIALOG_ON_TOP						= "device_dialogs_on_top";																																				//$NON-NLS-1$
	public final static String			IS_GLOBAL_LOG_LEVEL							= "is_global_log_level";																																					//$NON-NLS-1$
	public static final String			IS_REDUCE_CHARGE_DISCHARGE			= "is_reduce_charge_discharge";																																		//$NON-NLS-1$
	public final static String			IS_ALL_IN_ONE_RECORDSET					= "is_all_in_one_record_set";																																			//$NON-NLS-1$
	public final static String			IS_PARTIAL_DATA_TABLE						= "is_partial_data_table";																																				//$NON-NLS-1$
	public final static String			IS_DATA_TABLE_EDITABLE					= "is_data_table_editable";																																				//$NON-NLS-1$
	public final static String			GLOBAL_LOG_LEVEL								= "global_log_level";																																							//$NON-NLS-1$
	public final static String			UI_LOG_LEVEL										= "ui_log_leve";																																									//$NON-NLS-1$
	public final static String			DEVICE_LOG_LEVEL								= "device_log_level";																																							//$NON-NLS-1$
	public final static String			DATA_LOG_LEVEL									= "data_log_level";																																								//$NON-NLS-1$
	public final static String			CONFIG_LOG_LEVEL								= "config_log_level";																																							//$NON-NLS-1$
	public final static String			UTILS_LOG_LEVEL									= "utils_log_level";																																							//$NON-NLS-1$
	public final static String			FILE_IO_LOG_LEVEL								= "file_IO_log_level";																																						//$NON-NLS-1$
	public final static String			SERIAL_IO_LOG_LEVEL							= "serial_IO_log_level";																																					//$NON-NLS-1$
	public final static Properties	classbasedLogger								= new Properties();

	public final static String			AUTO_OPEN_TOOL_BOX							= "auto_open_tool_box";																																						//$NON-NLS-1$
	public static final String			LOCALE_IN_USE										= "locale_in_use";																																								//$NON-NLS-1$
	public static final String			LOCALE_CHANGED									= "locale_changed";																																								//$NON-NLS-1$
	public static final String			TIME_FORMAT_IN_USE							= "time_format_in_use";																																						//$NON-NLS-1$
	public static final String			IS_DESKTOP_SHORTCUT_CREATED			= "is_desktop_shotcut_created";																																		//$NON-NLS-1$
	public static final String			IS_APPL_REGISTERED							= "is_GDE_registered";																																						//$NON-NLS-1$
	public static final String			IS_LOCK_UUCP_HINTED							= "is_lock_uucp_hinted";																																					//$NON-NLS-1$
	public static final String			LAST_UPDATE_CHECK								= "last_update_check";																																						//$NON-NLS-1$

	public final static String			GRID_DASH_STYLE									= "grid_dash_style";																																							//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_HOR_TYPE		= "grid_compare_horizontal_type";																																	//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_HOR_COLOR		= "grid_compare_horizontal_color";																																//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_VER_TYPE		= "grid_compare_vertical_type";																																		//$NON-NLS-1$
	public final static String			GRID_COMPARE_WINDOW_VER_COLOR		= "grid_compare_vertical_color";																																	//$NON-NLS-1$

	public final static String			DEVICE_PROPERTIES_DIR_NAME			= "Devices";																																											//$NON-NLS-1$
	public final static String			DEVICE_PROPERTIES_XSD_NAME			= "DeviceProperties" + GDE.DEVICE_PROPERTIES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD;								//$NON-NLS-1$
	public final static String			GRAPHICS_TEMPLATES_DIR_NAME			= "GraphicsTemplates";																																						//$NON-NLS-1$
	public final static String			GRAPHICS_TEMPLATES_XSD_NAME			= "GraphicsTemplates" + GDE.GRAPHICS_TEMPLATES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD;							//$NON-NLS-1$
	public final static String			GRAPHICS_TEMPLATES_EXTENSION		= GDE.FILE_ENDING_STAR_XML;

	public final static String			HISTO_CACHE_ENTRIES_DIR_NAME		= "Cache";																																												//$NON-NLS-1$
	public final static String			HISTO_CACHE_ENTRIES_XSD_NAME		= "HistoVault" + GDE.HISTO_CACHE_ENTRIES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD;										//$NON-NLS-1$
	public static final String			HISTO_EXCLUSIONS_FILE_NAME			= ".gdeignore";																																										//$NON-NLS-1$
	public static final String			HISTO_EXCLUSIONS_DIR_NAME				= ".gdeignore";																																										//$NON-NLS-1$
	public static final String			GPS_LOCATIONS_DIR_NAME					= "Locations";																																										//$NON-NLS-1$
	public static final String			GPS_API_URL											= "http://maps.googleapis.com/maps/api/geocode/xml?latlng=";																			//$NON-NLS-1$

	BufferedReader									reader;																																																														// to read the application settings
	BufferedWriter									writer;																																																														// to write the application settings

	boolean													isDevicePropertiesUpdated				= false;
	//boolean													isDevicePropertiesReplaced			= false;
	boolean													isGraphicsTemplateUpdated				= false;
	boolean													isHistocacheTemplateUpdated			= false;

	Rectangle												window;
	boolean													isWindowMaximized								= false;
	String													cbOrder;
	private String									cbWraps;
	String													cbSizes;
	String													settingsFilePath;																																																									// full qualified path to settings file
	String													applHomePath;																																																											// default path to application home directory
	Comparator<String>							comparator											= new RecordSetNameComparator();																																	//used to sort object key list
	Properties											measurementProperties						= new Properties();

	public enum GeoCodeGoogle {
		STREET_ADDRESS, ROUTE, POLITICAL, ADMINISTRATIVE_AREA_LEVEL_3, ADMINISTRATIVE_AREA_LEVEL_2;
		/**
		 * use this instead of values() to avoid repeatedly cloning actions.
		 */
		public static final GeoCodeGoogle VALUES[] = values();
	};

	/**
	 * a singleton needs a static method to get the instance of this calss
	 * @return DataExplorer instance
	 */
	public static Settings getInstance() {
		final String $METHOD_NAME = "getInstance"; //$NON-NLS-1$
		if (Settings.instance == null) {
			try {
				Settings.instance = new Settings();
				Settings.log.logp(Level.TIME, Settings.$CLASS_NAME, $METHOD_NAME, "init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (Exception e) {
				Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
		}
		return Settings.instance;
	}

	/**
	 * singleton private constructor
	 * @throws SAXException
	 * @throws JAXBException
	 * @throws ApplicationConfigurationException
	 */
	private Settings() throws SAXException, JAXBException {
		final String $METHOD_NAME = "Settings"; //$NON-NLS-1$

		if (GDE.IS_WINDOWS) {
			this.applHomePath = (System.getenv("APPDATA") + GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG).replace("\\", GDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$
			this.settingsFilePath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG + ".properties"; //$NON-NLS-1$
		}
		else if (GDE.IS_LINUX) {
			this.applHomePath = System.getProperty("user.home") + GDE.FILE_SEPARATOR_UNIX + "." + GDE.NAME_LONG; //$NON-NLS-1$ //$NON-NLS-2$
			this.settingsFilePath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG + ".properties"; //$NON-NLS-1$
		}
		// OPET - start - add
		else if (GDE.IS_MAC) {
			this.applHomePath = System.getProperty("user.home") + GDE.FILE_SEPARATOR_UNIX + "Library" + GDE.FILE_SEPARATOR_UNIX + "Application Support" + GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG; //$NON-NLS-1$ //$NON-NLS-2$
			this.settingsFilePath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + GDE.NAME_LONG + ".properties"; //$NON-NLS-1$
		}
		// OPET - end
		else {
			Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGW0001));
		}

		this.xmlBasePath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + GDE.FILE_SEPARATOR_UNIX;
		this.xsdThread = new Thread("xsdValidation") {
			@Override
			public void run() {
				// device properties context
				try {
					Settings.this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(Settings.this.xmlBasePath + Settings.DEVICE_PROPERTIES_XSD_NAME));
					Settings.this.jc = JAXBContext.newInstance("gde.device"); //$NON-NLS-1$
					Settings.this.unmarshaller = Settings.this.jc.createUnmarshaller();
					Settings.this.unmarshaller.setSchema(Settings.this.schema);
					Settings.this.marshaller = Settings.this.jc.createMarshaller();
					Settings.this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
					Settings.this.marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, Settings.DEVICE_PROPERTIES_XSD_NAME);
					Settings.log.logp(Level.TIME, Settings.$CLASS_NAME, $METHOD_NAME, "schema factory setup time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (Exception e) {
					Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, "xsdThread.run()", e.getMessage(), e);
				}
			}
		};

		this.load();

		// check existence of application home directory, check XSD version, copy all device XML+XSD and image files
		FileUtils.checkDirectoryAndCreate(this.applHomePath);
		String devicePropertiesTargetpath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME;
		devicePropertiesTargetpath = devicePropertiesTargetpath.replace(GDE.STRING_URL_BLANK, GDE.STRING_BLANK);
		if (!FileUtils.checkDirectoryAndCreate(devicePropertiesTargetpath, Settings.DEVICE_PROPERTIES_XSD_NAME)) {
			FileUtils.extract(this.getClass(), Settings.DEVICE_PROPERTIES_XSD_NAME, Settings.PATH_RESOURCE, devicePropertiesTargetpath, Settings.PERMISSION_555);
			updateDeviceProperties(devicePropertiesTargetpath + GDE.FILE_SEPARATOR_UNIX, true, true);
			this.isDevicePropertiesUpdated = true;
		}
		else { // execute every time application starts to enable update from added plug-in
			updateDeviceProperties(devicePropertiesTargetpath + GDE.FILE_SEPARATOR_UNIX, true, true);
		}

		this.readMeasurementDiplayProperties();

		this.xsdThread.start(); // wait to start the thread until the device XMLs are getting updated, local switch comes with the same XSD

		// locale settings has been changed, replacement of device property files required
		if (this.getLocaleChanged()) {
			updateDeviceProperties(devicePropertiesTargetpath + GDE.FILE_SEPARATOR_UNIX, false, false);
			//this.isDevicePropertiesReplaced = true;
		}

		if (this.isDevicePropertiesUpdated) { //check if previous devices exist and migrate device usage, default import directory, ....
			this.migrationThread = new Thread("migration") {
				@Override
				public void run() {
					int lastVersion = Integer.valueOf(GDE.DEVICE_PROPERTIES_XSD_VERSION.substring(GDE.DEVICE_PROPERTIES_XSD_VERSION.lastIndexOf("_V") + 2)) - 1;
					for (int i = lastVersion; i >= 10; i--) {
						String migratePropertyPath = Settings.this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + "_V" + i;
						if (new File(migratePropertyPath).exists()) {
							log.log(Level.INFO, "previous devices exist, migrate from " + migratePropertyPath);
							try {
								while (Settings.this.isXsdThreadAlive() || Settings.this.getUnmarshaller() == null) {
									WaitTimer.delay(7);
								}

								Unmarshaller tmpUnmarshaller = JAXBContext.newInstance("gde.device").createUnmarshaller();//$NON-NLS-1$
								tmpUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(migratePropertyPath + "/DeviceProperties_V" + i + GDE.FILE_ENDING_DOT_XSD)));
								TreeMap<String, DeviceConfiguration> actualConfigurations = DataExplorer.getInstance().getDeviceSelectionDialog().getDevices();
								List<File> deviceProperties = FileUtils.getFileListing(new File(migratePropertyPath), 1);
								for (File file : deviceProperties) {
									if (file.getAbsolutePath().endsWith(GDE.FILE_ENDING_DOT_XML)) {
										DeviceConfiguration oldConfig = new DeviceConfiguration(file.getAbsolutePath(), tmpUnmarshaller);
										DeviceConfiguration newConfig = actualConfigurations.get(oldConfig.getName());
										if (oldConfig.isUsed() && newConfig != null) {
											newConfig.setUsed(true);
											if (oldConfig.getPort().length() > 1 && !oldConfig.getPort().startsWith("USB")) newConfig.setPort(oldConfig.getPort());
											if (oldConfig.getDataBlockPreferredDataLocation().length() > 1) newConfig.setDataBlockPreferredDataLocation(oldConfig.getDataBlockPreferredDataLocation());
											if (oldConfig.getLastChannelNumber() != 0) newConfig.setLastChannelNumber(oldConfig.getLastChannelNumber());

											newConfig.storeDeviceProperties();
											log.log(Level.OFF, "migrated device configuration " + newConfig.getName());
										}
									}
								}
							}
							catch (Exception e) {
								Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, "xsdThread.run()", e.getMessage(), e);
							}
							break;
						}
					}
				}
			};
		}

		String templateDirectory = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.GRAPHICS_TEMPLATES_DIR_NAME;
		if (!FileUtils.checkDirectoryAndCreate(templateDirectory, Settings.GRAPHICS_TEMPLATES_XSD_NAME)) {
			FileUtils.extract(this.getClass(), Settings.GRAPHICS_TEMPLATES_XSD_NAME, Settings.PATH_RESOURCE, templateDirectory, Settings.PERMISSION_555);
			this.isGraphicsTemplateUpdated = true;
		}
		checkDeviceTemplates(templateDirectory + GDE.FILE_SEPARATOR_UNIX);

		String histoCacheDirectory = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.HISTO_CACHE_ENTRIES_DIR_NAME;
		if (!FileUtils.checkDirectoryAndCreate(histoCacheDirectory, Settings.HISTO_CACHE_ENTRIES_XSD_NAME)) {
			resetHistoCache();
			this.isHistocacheTemplateUpdated = true;
		}

		FileUtils.checkDirectoryAndCreate(this.applHomePath + GDE.FILE_SEPARATOR_UNIX + "Logs"); //$NON-NLS-1$

		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, String.format("settingsFilePath = %s", this.settingsFilePath)); //$NON-NLS-1$

		this.isWindowMaximized = Boolean.parseBoolean(this.getProperty(Settings.WINDOW_MAXIMIZED, "false"));

		if (this.getProperty(Settings.WINDOW_LEFT) != null && this.getProperty(Settings.WINDOW_TOP) != null && this.getProperty(Settings.WINDOW_WIDTH) != null
				&& this.getProperty(Settings.WINDOW_HEIGHT) != null) {
			this.window = new Rectangle(new Integer(this.getProperty(Settings.WINDOW_LEFT).trim()).intValue(), new Integer(this.getProperty(Settings.WINDOW_TOP).trim()).intValue(),
					new Integer(this.getProperty(Settings.WINDOW_WIDTH).trim()).intValue(), new Integer(this.getProperty(Settings.WINDOW_HEIGHT).trim()).intValue());
		}
		else
			this.window = new Rectangle(50, 50, 950, 600);

		this.setProperty(Settings.LOCALE_CHANGED, "false"); //$NON-NLS-1$
	}

	public Path getHistoCacheDirectory() {
		return Paths.get(this.applHomePath, Settings.HISTO_CACHE_ENTRIES_DIR_NAME);
	}

	public Path getHistoLocationsDirectory() {
		return Paths.get(this.applHomePath, Settings.GPS_LOCATIONS_DIR_NAME);
	}

	public String resetHistoCache() {
		final String $METHOD_NAME = "resetHistoCache"; //$NON-NLS-1$
		int initialSize_KiB = (int) FileUtils.size(getHistoCacheDirectory()) / 1024;
		FileUtils.deleteDirectory(getHistoCacheDirectory().toString());
		FileUtils.checkDirectoryAndCreate(getHistoCacheDirectory().toString());
		int deletedSize_KiB = (int) FileUtils.size(getHistoCacheDirectory()) / 1024;
		FileUtils.extract(this.getClass(), Settings.HISTO_CACHE_ENTRIES_XSD_NAME, Settings.PATH_RESOURCE, getHistoCacheDirectory().toString(), Settings.PERMISSION_555);
		String message = Messages.getString(MessageIds.GDE_MSGT0831, new Object[] { initialSize_KiB, deletedSize_KiB, getHistoCacheDirectory() });
		Settings.log.logp(java.util.logging.Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, message);
		return message;
	}

	/**
	 * @return true if files were actually deleted
	 */
	public boolean resetHistolocations() {
		if (FileUtils.checkDirectoryExist(getHistoLocationsDirectory().toString())) {
			FileUtils.deleteDirectory(getHistoLocationsDirectory().toString());
			Settings.log.log(java.util.logging.Level.CONFIG, "histo geo locations deleted"); //$NON-NLS-1$
			return true;
		}
		else
			return false;
	}

	/**
	 * check existence of directory, create if required and update all
	 * @param devicePropertiesTargetpath
	 */
	private void updateDeviceProperties(String devicePropertiesTargetpath, boolean existCheck, boolean replaceDeviceXmlFiles) {
		final String $METHOD_NAME = "updateDeviceProperties"; //$NON-NLS-1$
		final String lang = this.getLocale().getLanguage().contains("de") || this.getLocale().getLanguage().contains("en") ? this.getLocale().getLanguage() : "en";

		if (replaceDeviceXmlFiles) {
			String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
			Settings.log.logp(java.util.logging.Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, "deviceJarBasePath = " + deviceJarBasePath); //$NON-NLS-1$
			String[] files = new File(deviceJarBasePath).list();
			for (String jarFileName : files) {
				if (!jarFileName.endsWith(GDE.FILE_ENDING_DOT_JAR)) continue;
				JarFile jarFile = null;
				String[] plugins = new String[0];
				try {
					jarFile = new JarFile(deviceJarBasePath + GDE.FILE_SEPARATOR_UNIX + jarFileName);
					plugins = FileUtils.getDeviceJarServicesNames(jarFile);
				}
				catch (Throwable e) {
					Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				}
				if (jarFile != null) {
					for (String plugin : plugins) {
						if (existCheck) {
							if (!FileUtils.checkFileExist(devicePropertiesTargetpath + plugin + GDE.FILE_ENDING_DOT_XML))
								FileUtils.extract(jarFile, plugin + GDE.FILE_ENDING_DOT_XML, Settings.PATH_RESOURCE, devicePropertiesTargetpath, Settings.PERMISSION_555);
						}
						else {
							FileUtils.extract(jarFile, plugin + GDE.FILE_ENDING_DOT_XML, Settings.PATH_RESOURCE, devicePropertiesTargetpath, Settings.PERMISSION_555);
						}
					}
				}
			}
		}
		File path = new File(this.getApplHomePath() + "/Mapping/"); //$NON-NLS-1$
		String propertyFilePath = this.getApplHomePath() + "/Mapping/MeasurementDisplayProperties.xml"; //$NON-NLS-1$
		if (existCheck) {
			if (!FileUtils.checkFileExist(propertyFilePath))
				FileUtils.extract(this.getClass(), "MeasurementDisplayProperties.xml", Settings.PATH_RESOURCE + lang + GDE.FILE_SEPARATOR_UNIX, path.getAbsolutePath(), Settings.PERMISSION_555); //$NON-NLS-1$
		}
		else {
			if (FileUtils.checkFileExist(propertyFilePath)) {
				new File(propertyFilePath).delete();
				try {
					Thread.sleep(5);
				}
				catch (InterruptedException e) {
					//ignore
				}
			}
			FileUtils.extract(this.getClass(), "MeasurementDisplayProperties.xml", Settings.PATH_RESOURCE + lang + GDE.FILE_SEPARATOR_UNIX, path.getAbsolutePath(), Settings.PERMISSION_555); //$NON-NLS-1$
		}
	}

	/**
	 * check existence of device graphics default templates, extract if required
	 * @param templateDirectoryTargetPath
	 */
	private void checkDeviceTemplates(String templateDirectoryTargetPath) {
		final String $METHOD_NAME = "checkDeviceTemplates"; //$NON-NLS-1$

		String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
		Settings.log.logp(java.util.logging.Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, "deviceJarBasePath = " + deviceJarBasePath); //$NON-NLS-1$
		String[] files = new File(deviceJarBasePath).list();
		for (String jarFileName : files) {
			if (!jarFileName.endsWith(GDE.FILE_ENDING_DOT_JAR)) continue;
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(deviceJarBasePath + GDE.FILE_SEPARATOR_UNIX + jarFileName);
			}
			catch (IOException e) {
				Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}

			if (jarFile != null) {
				Settings.log.logp(java.util.logging.Level.FINER, Settings.$CLASS_NAME, $METHOD_NAME, "templateDirectoryTargetPath=" + templateDirectoryTargetPath); //$NON-NLS-1$
				Enumeration<JarEntry> e = jarFile.entries();
				while (e.hasMoreElements()) {
					String entryName = e.nextElement().getName();
					if (entryName.startsWith(Settings.PATH_RESOURCE_TEMPLATE) && entryName.endsWith(GDE.FILE_ENDING_DOT_XML)) {
						String defaultTemplateName = entryName.substring(Settings.PATH_RESOURCE_TEMPLATE.length());
						if (!FileUtils.checkFileExist(templateDirectoryTargetPath + defaultTemplateName)) {
							Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "jarFile=" + jarFile.getName() + "; defaultTemplateName=" + entryName); //$NON-NLS-1$ //$NON-NLS-2$
							FileUtils.extract(jarFile, defaultTemplateName, Settings.PATH_RESOURCE_TEMPLATE, templateDirectoryTargetPath, Settings.PERMISSION_555);
						}
					}
				}
			}
		}
	}

	/**
	 * read special properties to enable configuration to specific GPX extent values
	 * @throws FileNotFoundException
	 */
	private void readMeasurementDiplayProperties() {
		final String $METHOD_NAME = "readMeasurementDiplayProperties"; //$NON-NLS-1$
		String propertyFilePath = this.getApplHomePath() + "/Mapping/MeasurementDisplayProperties.xml"; //$NON-NLS-1$
		try {
			if (!new File(propertyFilePath).exists()) {
				File path = new File(this.getApplHomePath() + "/Mapping"); //$NON-NLS-1$
				if (!path.exists() && !path.isDirectory()) path.mkdir();
				//extract initial property files
				FileUtils.extract(this.getClass(), "MeasurementDisplayProperties.xml", Locale.getDefault().equals(Locale.ENGLISH) ? "resource/en" : "resource/de", path.getAbsolutePath(), //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
						Settings.PERMISSION_555);
			}
			//BufferedInputStream stream = new BufferedReader(new InputStreamReader(new FileInputStream(propertyFilePath), "UTF-8")); //$NON-NLS-1$
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream(new File(propertyFilePath)));
			this.measurementProperties.loadFromXML(stream);
			stream.close();
		}
		catch (Exception e) {
			Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage());
		}
	}

	/**
	 * @return the measurement display properties set
	 */
	public Properties getMeasurementDisplayProperties() {
		return this.measurementProperties;
	}

	/**
	 * read the application settings file
	 */
	void load() {
		final String $METHOD_NAME = "load"; //$NON-NLS-1$
		try {
			this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(this.settingsFilePath), "UTF-8")); //$NON-NLS-1$
			this.load(this.reader);
			this.reader.close();

			//update file history
			for (int i = 0; i < 9; i++) {
				String entry = this.getProperty(Settings.FILE_HISTORY_BEGIN + i);
				if (entry != null && entry.length() > 4) {
					if (!this.fileHistory.contains(entry)) this.fileHistory.add(entry);
				}
				else
					break;
			}
		}
		catch (Exception e) {
			Settings.log.logp(java.util.logging.Level.WARNING, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage());
		}

	}

	/**
	 * write the application settings file
	 */
	public void store() {
		final String $METHOS_NAME = "store()"; //$NON-NLS-1$
		try {
			File tmpFilePath = new File(this.settingsFilePath);
			if (!tmpFilePath.exists()) {
				if (!tmpFilePath.createNewFile()) Settings.log.logp(java.util.logging.Level.WARNING, Settings.$CLASS_NAME, $METHOS_NAME, "failed creating " + this.settingsFilePath);
			}
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.settingsFilePath), "UTF-8")); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.HEADER_TEXT)); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.DEVICE_BLOCK)); // [Gerät] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.ACTIVE_DEVICE, this.getActiveDevice())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_LIST, this.getObjectListAsString())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.ACTIVE_OBJECT, this.getActiveObject())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.WINDOW_BLOCK)); // [Fenster Einstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_MAXIMIZED, this.isWindowMaximized)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_LEFT, this.window.x)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_TOP, this.window.y)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_WIDTH, this.window.width)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.WINDOW_HEIGHT, this.window.height)); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.COOLBAR_ORDER, this.cbOrder)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.COOLBAR_WRAPS, this.cbWraps)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.COOLBAR_SIZES, this.cbSizes)); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.RECORD_COMMENT_VISIBLE, isRecordCommentVisible())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_HEADER_VISIBLE, isGraphicsHeaderVisible())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DISPLAY_DENSITY_FONT_CORRECT, getFontDisplayDensityAdaptionFactor())); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_DASH_STYLE, getGridDashStyleAsString())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_HOR_TYPE, getGridCompareWindowHorizontalType())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_HOR_COLOR, getGridCompareWindowHorizontalColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_VER_TYPE, getGridCompareWindowVerticalType())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRID_COMPARE_WINDOW_VER_COLOR, getGridCompareWindowVerticalColorStr())); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_AREA_BACKGROUND, getGraphicsCurveAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_SURROUND_BACKGRD, getGraphicsSurroundingBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GRAPHICS_BORDER_COLOR, getGraphicsCurvesBorderColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_SCALE_COLOR, isDrawScaleInRecordColor())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_NAME_COLOR, isDrawNameInRecordColor())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GRAPHICS_NUMBERS_COLOR, isDrawNumbersInRecordColor())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.COMPARE_AREA_BACKGROUND, getCompareCurveAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.COMPARE_SURROUND_BACKGRD, getCompareSurroundingBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.COMPARE_BORDER_COLOR, getCurveCompareBorderColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_COMPARE_CHANNELCONFIG, isCurveCompareChannelConfigName())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILITY_AREA_BACKGROUND, getUtilityCurveAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILITY_SURROUND_BACKGRD, getUtilitySurroundingBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILITY_BORDER_COLOR, getUtilityCurvesBorderColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.STATISTICS_INNER_BACKGROUND, getStatisticsInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.STATISTICS_SURROUND_BACKGRD, getStatisticsSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.ANALOG_INNER_BACKGROUND, getAnalogInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.ANALOG_SURROUND_BACKGRD, getAnalogSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DIGITAL_INNER_BACKGROUND, getDigitalInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DIGITAL_SURROUND_BACKGRD, getDigitalSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.CELL_VOLTAGE_INNER_BACKGROUND, getCellVoltageInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.CELL_VOLTAGE_SURROUND_BACKGRD, getCellVoltageSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_COMMENT_INNER_BACKGROUND, getFileCommentInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_COMMENT_SURROUND_BACKGRD, getFileCommentSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_DESC_INNER_BACKGROUND, getObjectDescriptionInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_DESC_SURROUND_BACKGRD, getObjectDescriptionSurroundingAreaBackgroundStr())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.FILE_HISTORY_BLOCK)); // [Datei History Liste] //$NON-NLS-1$
			for (int i = 0; i < 9 && i < this.fileHistory.size(); i++) {
				if (this.fileHistory.get(i) == null) break;
				this.writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_HISTORY_BEGIN + i, this.fileHistory.get(i))); //$NON-NLS-1$
			}

			this.writer.write(String.format("%s\n", Settings.APPL_BLOCK)); // [Programmeinstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DATA_FILE_PATH, getDataFilePath())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.OBJECT_IMAGE_FILE_PATH, getObjectImageFilePath())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.USE_DATA_FILE_NAME_LEADER, getUsageDateAsFileNameLeader())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.USE_OBJECT_KEY_IN_FILE_NAME, getUsageObjectKeyInFileName())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.WRITE_TMP_FILES, getUsageWritingTmpFiles())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.ALPHA_BLENDING_VALUE, getDialogAlphaValue())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.APLHA_BLENDING_ENABLED, isDeviceDialogAlphaEnabled())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.KEEP_IMPORT_DIR_OBJECT_RELATED, isDeviceImportDirectoryObjectRelated())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GLOBAL_PORT, isGlobalSerialPort())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GLOBAL_PORT_NAME, getSerialPort())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.SKIP_BLUETOOTH_DEVICES, isSkipBluetoothDevices())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DO_PORT_AVAILABLE_TEST, doPortAvailabilityCheck())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_PORT_BLACKLIST, isSerialPortBlackListEnabled())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.PORT_BLACKLIST, getSerialPortBlackList())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_PORT_WHITELIST, isSerialPortWhiteListEnabled())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.PORT_WHITELIST, getSerialPortWhiteListString())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_DIALOG_USE_MODAL, isDeviceDialogsModal())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_DIALOG_ON_TOP, isDeviceDialogsOnTop())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.AUTO_OPEN_TOOL_BOX, isAutoOpenToolBox())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.LOCALE_IN_USE, getLocale().getLanguage())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.LOCALE_CHANGED, getLocaleChanged())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.TIME_FORMAT_IN_USE, getTimeFormat())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DESKTOP_SHORTCUT_CREATED, this.isDesktopShortcutCreated())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_APPL_REGISTERED, this.isApplicationRegistered())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_LOCK_UUCP_HINTED, this.isLockUucpHinted())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.LAST_UPDATE_CHECK, StringHelper.getDate())); //$NON-NLS-1$
			//charger specials
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_REDUCE_CHARGE_DISCHARGE, this.isReduceChargeDischarge())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_ALL_IN_ONE_RECORDSET, this.isContinuousRecordSet())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_PARTIAL_DATA_TABLE, this.isPartialDataTable())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.TABLE_BLOCK)); // [Tabellen Einstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.LIST_SEPARATOR, getListSeparator())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DECIMAL_SEPARATOR, getDecimalSeparator())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.LOGGING_BLOCK)); // [Logging Einstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_GLOBAL_LOG_LEVEL, isGlobalLogLevel())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GLOBAL_LOG_LEVEL, getLogLevel(Settings.GLOBAL_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.UI_LOG_LEVEL, getLogLevel(Settings.UI_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DEVICE_LOG_LEVEL, getLogLevel(Settings.DEVICE_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.DATA_LOG_LEVEL, getLogLevel(Settings.DATA_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.CONFIG_LOG_LEVEL, getLogLevel(Settings.CONFIG_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.UTILS_LOG_LEVEL, getLogLevel(Settings.UTILS_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.FILE_IO_LOG_LEVEL, getLogLevel(Settings.FILE_IO_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.SERIAL_IO_LOG_LEVEL, getLogLevel(Settings.SERIAL_IO_LOG_LEVEL))); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", Settings.HISTO_BLOCK)); // [Histo Settings] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_HISTO_ACTIVE, isHistoActive())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SMART_STATISTICS, isSmartStatistics())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.BOXPLOT_SCALE_ORDINAL, getBoxplotScaleOrdinal())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.BOXPLOT_SIZE_ADAPTATION_ORDINAL, getBoxplotSizeAdaptationOrdinal())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.X_SPREAD_GRADE_ORDINAL, getXAxisSpreadOrdinal())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_X_LOGARITHMIC_DISTANCE, isXAxisLogarithmicDistance())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_X_REVERSED, isXAxisReversed())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.RETROSPECT_MONTHS, getRetrospectMonths())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.SEARCH_IMPORT_PATH, getSearchImportPath())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.SEARCH_DATAPATH_IMPORTS, getSearchDataPathImports())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_CHANNEL_MIX, isChannelMix())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.SAMPLING_TIMESPAN_ORDINAL, getSamplingTimespanOrdinal())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.FILES_WITHOUT_OBJECT, getFilesWithoutObject())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.FILES_WITH_OTHER_OBJECT, getFilesWithOtherObject())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_ZIPPED_CACHE, isZippedCache())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.MINMAX_QUANTILE_DISTANCE, getMinmaxQuantileDistance())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.ABSOLUTE_TRANSITION_LEVEL, getAbsoluteTransitionLevel())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DATETIME_UTC, isDateTimeUtc())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DISPLAY_SETTLEMENTS, isDisplaySettlements())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DISPLAY_SCORES, isDisplayScores())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DISPLAY_TAGS, isDisplayTags())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DATA_SETTINGS_AT_HOME_PATH, isDataSettingsAtHomePath())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_SUPPRESS_MODE, isSuppressMode())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_CURVE_SURVEY, isCurveSurvey())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GPS_LOCATION_RADIUS, getGpsLocationRadius())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.GPS_ADDRESS_TYPE, getGpsAddressType())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.SUBDIRECTORY_LEVEL_MAX, getSubDirectoryLevelMax())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", Settings.IS_DATA_TABLE_TRANSITIONS, isDataTableTransitions())); //$NON-NLS-1$

			this.writer.flush();
			this.writer.close();
		}
		catch (IOException e) {
			Settings.log.logp(java.util.logging.Level.SEVERE, Settings.$CLASS_NAME, $METHOS_NAME, e.getMessage(), e);
		}

	}

	/*
	 * overload Properties method due to loading properties from file returns "null" instead of null
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public String getProperty(String key, String defaultValue) {
		String val = getProperty(key);
		if (val == null || val.equals(GDE.STRING_EMPTY) || val.equals("null")) val = defaultValue;
		return val;
	}

	public Rectangle getWindow() {
		return this.window;
	}

	public void setWindow(Point location, Point size) {
		this.window = new Rectangle(location.x, location.y, size.x, size.y);
	}

	public boolean isWindowMaximized() {
		return this.isWindowMaximized;
	}

	public void setWindowMaximized(boolean isMaximized) {
		this.isWindowMaximized = isMaximized;
	}

	public void setCoolBarStates(int[] order, int[] wraps, Point[] sizes) {
		this.cbOrder = StringHelper.intArrayToString(order);
		this.cbWraps = StringHelper.intArrayToString(wraps);
		this.cbSizes = StringHelper.pointArrayToString(sizes);
	}

	public int[] getCoolBarOrder() {
		int[] intOrder = StringHelper.stringToIntArray(this.getProperty(Settings.COOLBAR_ORDER, "0;1;2;3;4").trim()); //$NON-NLS-1$
		int coolBarSize = this.getCoolBarSizes().length;
		if (intOrder.length != coolBarSize) {
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < coolBarSize; i++) {
				sb.append(i).append(";");
			}
			intOrder = StringHelper.stringToIntArray(sb.toString());
		}
		return intOrder;
	}

	public int[] getCoolBarWraps() {
		return StringHelper.stringToIntArray(this.getProperty(Settings.COOLBAR_WRAPS, "0;3").trim()); //$NON-NLS-1$
	}

	public Point[] getCoolBarSizes() {
		return StringHelper.stringToPointArray(this.getProperty(Settings.COOLBAR_SIZES, DataExplorer.getInstance().getMenuToolBar().getCoolBarSizes()).trim());
	}

	public List<String> getFileHistory() {
		return this.fileHistory;
	}

	public String getActiveDevice() {
		return this.getProperty(Settings.ACTIVE_DEVICE, Settings.EMPTY_SIGNATURE).split(GDE.STRING_SEMICOLON)[0].trim();
	}

	public void setActiveDevice(String activeDeviceString) {
		this.setProperty(Settings.ACTIVE_DEVICE, activeDeviceString.trim());
	}

	/**
	 * @return standard data directory path with trailing device and / or object stripped off
	 */
	@Deprecated
	public Path getDataBaseDir() {
		Path path = null;
		String tmpDataDirPath = getDataFilePath();
		if (!(tmpDataDirPath == null || tmpDataDirPath.trim().isEmpty() || tmpDataDirPath.equals(GDE.FILE_SEPARATOR_UNIX))) {
			path = Paths.get(tmpDataDirPath);
			// ignore object if path ends with a valid object
			String directoryName = path.getFileName().toString();
			path = getValidatedObjectKey(directoryName).isPresent() ? path.getParent() : path;
			// ignore device if path ends with a valid device
			String directoryName2 = path.getFileName().toString();
			path = DataExplorer.getInstance().getDeviceSelectionDialog().getDevices().keySet().stream().filter(s -> s.equals(directoryName2)).findFirst().isPresent() ? path.getParent() : path;
		}
		log.log(Level.INFO, "DataBaseDir " + path); //$NON-NLS-1$
		return path;
	}

	/**
	 * Scan the sub-directories in the data file and import file paths.
	 * @return all non-empty sub-directories which neither represent devices nor objects
	 */
	public List<String> getObjectKeyCandidates() {
		Set<String> result = new HashSet<String>();
		ArrayList<Path> dirPaths = new ArrayList<Path>();
		{
			final String dataFilePath = getDataFilePath();
			if (dataFilePath != null && !dataFilePath.trim().isEmpty() && !dataFilePath.equals(GDE.FILE_SEPARATOR_UNIX)) {
				dirPaths.add(Paths.get(dataFilePath));
				log.log(Level.FINE, "data path ", dataFilePath); //$NON-NLS-1$
			}
		}
		{
			String tmpImportDirPath = DataExplorer.getInstance().getActiveDevice().getDeviceConfiguration().getDataBlockType().getPreferredDataLocation();
			if (getSearchImportPath() && tmpImportDirPath != null && !tmpImportDirPath.trim().isEmpty() && !tmpImportDirPath.equals(GDE.FILE_SEPARATOR_UNIX)) {
				Path path = Paths.get(tmpImportDirPath);
				// ignore object if path ends with a valid object
				String directoryName = path.getFileName().toString();
				if (Settings.getInstance().getValidatedObjectKey(directoryName).isPresent())
					path = path.getParent();
				else {
					// ignore device if path ends with a valid device
					String directoryName2 = path.getFileName().toString();
					if (DataExplorer.getInstance().getDeviceSelectionDialog().getDevices().keySet().stream().filter(s -> s.equals(directoryName2)).findFirst().isPresent())
						path = path.getParent();
					else
						// the directory is supposed to be a new object
						path = path.getParent();
					;
				}
				log.log(Level.FINE, "ImportBaseDir ", path); //$NON-NLS-1$
				dirPaths.add(path);
			}
		}
		final Set<String> actualObjects = getRealObjectKeys();
		final TreeMap<String, DeviceConfiguration> actualConfigurations = DataExplorer.getInstance().getDeviceSelectionDialog().getDevices();
		for (Path dirPath : dirPaths) {
			if (!(dirPath == null || dirPath.toString().isEmpty())) {
				try {
					final File filePath = dirPath.toFile();
					for (File file : FileUtils.getDirectories(filePath)) {
						if (!actualConfigurations.containsKey(file.getName()) && !actualObjects.stream().filter(s -> s.equalsIgnoreCase(file.getName())).findFirst().isPresent()) {
							if (!FileUtils.getFileListing(file, Integer.MAX_VALUE).isEmpty()) {
								result.add(file.getName());
							}
						}
					}
				}
				catch (FileNotFoundException e) {
					log.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		}
		if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "object candidates=", result); //$NON-NLS-1$
		return new ArrayList<String>(result);
	}

	/**
	 * @param objectKeyCandidate is supposed to be a valid object key
	 * @return empty or the validated object key in the correct case sensitive format
	 */
	public Optional<String> getValidatedObjectKey(String objectKeyCandidate) {
		String key = objectKeyCandidate.trim();
		return Arrays.stream(getObjectList()).filter(s -> s.equalsIgnoreCase(key)).findFirst();
	}

	/**
	 * @return the cloned object keys without the deviceoriented entry
	 */
	public Set<String> getRealObjectKeys() {
		String[] objectKeys = this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200)).split(GDE.STRING_SEMICOLON);
		Set<String> objectList = new HashSet<>(Arrays.asList(objectKeys));
		String deviceOriented = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		objectList.remove(deviceOriented);
		return objectList;
	}

	public String getObjectListAsString() {
		return this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200));
	}

	public String[] getObjectList() {
		String[] objectKeys = this.getProperty(Settings.OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200)).split(GDE.STRING_SEMICOLON);
		objectKeys[0] = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
		return objectKeys;
	}

	public void setObjectList(String[] activeObjectList, int newActiveObjectIndex) {
		String activeObjectKey = activeObjectList[0];
		try {
			activeObjectKey = activeObjectList[newActiveObjectIndex];
		}
		catch (Exception e) {
			// IndexOutOfBounds may occur while object keys are renamed and not deleted
			Settings.log.log(java.util.logging.Level.WARNING, e.getMessage(), e);
		}
		setObjectList(activeObjectList, activeObjectKey);
	}

	public void setObjectList(String[] activeObjectList, String newObjectKey) {
		// keep object oriented out of the sorting game
		boolean startsWithDeviceOriented = activeObjectList[0].startsWith(Messages.getString(MessageIds.GDE_MSGT0200).substring(0, 10));
		if (startsWithDeviceOriented) {
			String[] tmpObjectKeys = new String[activeObjectList.length - 1];
			System.arraycopy(activeObjectList, 1, tmpObjectKeys, 0, activeObjectList.length - 1);
			Arrays.sort(tmpObjectKeys, String.CASE_INSENSITIVE_ORDER);
			System.arraycopy(tmpObjectKeys, 0, activeObjectList, 1, activeObjectList.length - 1);
		}
		else {
			Arrays.sort(activeObjectList, String.CASE_INSENSITIVE_ORDER);
			String[] tmpObjectKeys = new String[activeObjectList.length + 1];
			tmpObjectKeys[0] = Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0];
			System.arraycopy(activeObjectList, 0, tmpObjectKeys, 1, activeObjectList.length);
			activeObjectList = tmpObjectKeys;
		}
		//check for invalid object key
		Vector<String> tmpObjectVector = new Vector<String>();
		for (String objectKey : activeObjectList) {
			if (objectKey.length() > 1) tmpObjectVector.add(objectKey.trim());
		}
		activeObjectList = tmpObjectVector.toArray(new String[1]);

		//find the active object index within sorted array
		StringBuffer sb = new StringBuffer();
		for (String element : activeObjectList) {
			sb.append(element).append(GDE.STRING_SEMICOLON);
		}
		this.setProperty(Settings.OBJECT_LIST, sb.toString());
		this.setProperty(Settings.ACTIVE_OBJECT, newObjectKey);
	}

	public int getActiveObjectIndex() {
		Vector<String> tmpObjectVector = new Vector<String>();
		for (String objectKey : this.getObjectList()) {
			if (objectKey.length() > 1) tmpObjectVector.add(objectKey);
		}
		int index = tmpObjectVector.indexOf(this.getProperty(Settings.ACTIVE_OBJECT, Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0]).trim());
		return index < 0 ? 0 : index;
	}

	public String getActiveObject() {
		return getObjectList()[getActiveObjectIndex()];
	}

	/**
	 * @return the settingsFilePath
	 */
	public String getSettingsFilePath() {
		return this.settingsFilePath.trim();
	}

	/**
	 * @return the applHomePath
	 */
	public String getApplHomePath() {
		return this.applHomePath.trim();
	}

	/**
	 * @return the devicesFilePath
	 */
	public String getDevicesPath() {
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME;
	}

	/**
	 * @return the graphicsTemplatePath
	 */
	public String getGraphicsTemplatePath() {
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + Settings.GRAPHICS_TEMPLATES_DIR_NAME;
	}

	/**
	 * @return the log file path
	 */
	public String getLogFilePath() {
		final String $METHOD_NAME = "getLogFilePath"; //$NON-NLS-1$
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "applHomePath = " + this.applHomePath); //$NON-NLS-1$
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + Settings.LOG_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.LOG_FILE;
	}

	/**
	 * @return the log file path for the serial trace logs
	 */
	public String getSerialLogFilePath() {
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + Settings.LOG_PATH + GDE.FILE_SEPARATOR_UNIX + Settings.SERIAL_LOG_FILE;
	}

	/**
	 * @return the default dataFilePath
	 */
	public String getDataFilePath() {
		final String $METHOD_NAME = "getDataFilePath"; //$NON-NLS-1$
		String dataPath = this.getProperty(Settings.DATA_FILE_PATH, System.getProperty("user.home")).replace("\\\\", GDE.FILE_SEPARATOR_UNIX).replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "dataFilePath = " + dataPath); //$NON-NLS-1$
		return dataPath.trim();
	}

	/**
	 * set the default dataFilePath
	 */
	public void setDataFilePath(String newDataFilePath) {
		final String $METHOD_NAME = "setDataFilePath"; //$NON-NLS-1$
		String filePath = newDataFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX).trim();
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "newDataFilePath = " + filePath); //$NON-NLS-1$
		this.setProperty(Settings.DATA_FILE_PATH, filePath);
	}

	/**
	 * @return the list separator
	 */
	public char getListSeparator() {
		if (this.getProperty(Settings.LIST_SEPARATOR) == null) this.setProperty(Settings.LIST_SEPARATOR, GDE.STRING_SEMICOLON);
		return this.getProperty(Settings.LIST_SEPARATOR).trim().charAt(0);
	}

	/**
	 * set the list separator
	 */
	public void setListSeparator(String newListSeparator) {
		this.setProperty(Settings.LIST_SEPARATOR, newListSeparator.trim());
	}

	/**
	 * @return the decimal separator, default value is '.'
	 */
	public char getDecimalSeparator() {
		if (this.getProperty(Settings.DECIMAL_SEPARATOR) == null) this.setProperty(Settings.DECIMAL_SEPARATOR, GDE.STRING_DOT);
		return this.getProperty(Settings.DECIMAL_SEPARATOR).trim().charAt(0);
	}

	/**
	 * set the decimal separator
	 */
	public void setDecimalSeparator(String newDecimalSeparator) {
		this.setProperty(Settings.DECIMAL_SEPARATOR, newDecimalSeparator.trim());
	}

	/**
	 * set the usage of suggest date as leader of the to be saved filename
	 */
	public void setUsageDateAsFileNameLeader(boolean usage) {
		this.setProperty(Settings.USE_DATA_FILE_NAME_LEADER, GDE.STRING_EMPTY + usage);
	}

	/**
	 * set usage of the object key within the file name
	 */
	public void setUsageObjectKeyInFileName(boolean usage) {
		this.setProperty(Settings.USE_OBJECT_KEY_IN_FILE_NAME, GDE.STRING_EMPTY + usage);
	}

	/**
	 * set usage if files should be written each 5 minutes
	 */
	public void setUsageWritingTmpFiles(boolean usage) {
		this.setProperty(Settings.WRITE_TMP_FILES, GDE.STRING_EMPTY + usage);
	}

	/**
	 * get the usage of suggest date as leader of the to be saved filename
	 */
	public boolean getUsageDateAsFileNameLeader() {
		return Boolean.valueOf(this.getProperty(Settings.USE_DATA_FILE_NAME_LEADER, "true")); //$NON-NLS-1$
	}

	/**
	 * get usage of the object key within the file name
	 */
	public boolean getUsageObjectKeyInFileName() {
		return Boolean.valueOf(this.getProperty(Settings.USE_OBJECT_KEY_IN_FILE_NAME, "false")); //$NON-NLS-1$
	}

	/**
	 * get usage if files should be written each 5 minutes
	 */
	public boolean getUsageWritingTmpFiles() {
		return Boolean.valueOf(this.getProperty(Settings.WRITE_TMP_FILES, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the global serial port
	 */
	public boolean isGlobalSerialPort() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GLOBAL_PORT, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global serial port
	 */
	public void setIsGlobalSerialPort(String isGlobalSerialPort) {
		this.setProperty(Settings.IS_GLOBAL_PORT, isGlobalSerialPort.trim());
	}

	/**
	 * @return boolean value of port black list enablement
	 */
	public boolean isSerialPortBlackListEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.IS_PORT_BLACKLIST, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global serial port black list enabled
	 */
	public void setSerialPortBlackListEnabled(boolean enabled) {
		this.setProperty(Settings.IS_PORT_BLACKLIST, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return port black list
	 */
	public String getSerialPortBlackList() {
		StringBuffer blackList = new StringBuffer();
		for (String port : this.getProperty(Settings.PORT_BLACKLIST, GDE.STRING_EMPTY).trim().split(GDE.STRING_BLANK)) {
			if (port != null && port.length() > 3) blackList.append(port).append(GDE.STRING_BLANK);
		}
		return blackList.toString().trim();
	}

	/**
	 * set the serial port black list
	 */
	public void setSerialPortBlackList(String newPortBlackList) {
		StringBuilder blackList = new StringBuilder();
		for (String tmpPort : newPortBlackList.split(GDE.STRING_BLANK)) {
			if (GDE.IS_WINDOWS && tmpPort.toUpperCase().startsWith(Settings.WINDOWS_PORT_COM))
				blackList.append(tmpPort.toUpperCase()).append(GDE.STRING_BLANK);
			else if (tmpPort.startsWith(Settings.UNIX_PORT_DEV_TTY)) blackList.append(tmpPort).append(GDE.STRING_BLANK);
		}
		this.setProperty(Settings.PORT_BLACKLIST, blackList.toString());
	}

	/**
	 * @return boolean value of port white list enablement
	 */
	public boolean isSerialPortWhiteListEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.IS_PORT_WHITELIST, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the serial port white list enabled
	 */
	public void setSerialPortWhiteListEnabled(boolean enabled) {
		this.setProperty(Settings.IS_PORT_WHITELIST, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return port white list as vector
	 */
	public Vector<String> getSerialPortWhiteList() {
		Vector<String> whiteList = new Vector<String>();
		for (String port : this.getProperty(Settings.PORT_WHITELIST, GDE.STRING_EMPTY).trim().split(";| ")) { //$NON-NLS-1$
			if (port != null && port.length() > 3) whiteList.add(port);
		}
		return whiteList;
	}

	/**
	 * @return port white list as String
	 */
	public String getSerialPortWhiteListString() {
		StringBuffer whiteList = new StringBuffer();
		for (String port : this.getProperty(Settings.PORT_WHITELIST, GDE.STRING_EMPTY).trim().split(";| ")) { //$NON-NLS-1$
			if (port != null && port.length() > 3) whiteList.append(port).append(GDE.STRING_BLANK);
		}
		return whiteList.toString().trim();
	}

	/**
	 * set the serial port white list
	 */
	public void setSerialPortWhiteList(String newPortWhiteList) {
		StringBuilder whiteList = new StringBuilder();
		for (String tmpPort : newPortWhiteList.split(GDE.STRING_BLANK)) {
			if (GDE.IS_WINDOWS && tmpPort.toUpperCase().startsWith(Settings.WINDOWS_PORT_COM))
				whiteList.append(tmpPort.toUpperCase()).append(GDE.STRING_SEMICOLON);
			else if (tmpPort.startsWith(Settings.UNIX_PORT_DEV_TTY)) whiteList.append(tmpPort).append(GDE.STRING_BLANK);
		}
		this.setProperty(Settings.PORT_WHITELIST, whiteList.toString());
	}

	/**
	 * @return boolean value if data gathering should only add charge and discharge data
	 */
	public boolean isReduceChargeDischarge() {
		return Boolean.valueOf(this.getProperty(Settings.IS_REDUCE_CHARGE_DISCHARGE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data gathering should only add charge and discharge data
	 */
	public void setReduceChargeDischarge(boolean enabled) {
		this.setProperty(Settings.IS_REDUCE_CHARGE_DISCHARGE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return boolean value if data gathering should result in a single recordSet
	 */
	public boolean isContinuousRecordSet() {
		return Boolean.valueOf(this.getProperty(Settings.IS_ALL_IN_ONE_RECORDSET, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data gathering should result in a single recordSet
	 */
	public void setContinuousRecordSet(boolean enabled) {
		this.setProperty(Settings.IS_ALL_IN_ONE_RECORDSET, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return boolean value if data table display selected records from graphics
	 */
	public boolean isPartialDataTable() {
		return Boolean.valueOf(this.getProperty(Settings.IS_PARTIAL_DATA_TABLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data table should show only selected records from curve view
	 */
	public void setPartialDataTable(boolean enabled) {
		this.setProperty(Settings.IS_PARTIAL_DATA_TABLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return boolean value if data table displayed selected record entry is editable, this preference is not persistence
	 */
	public boolean isDataTableEditable() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATA_TABLE_EDITABLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set boolean value if data table should enable editing selected record entry, this preference will not be made persistence
	 */
	public void setDataTableEditable(boolean enabled) {
		this.setProperty(Settings.IS_DATA_TABLE_EDITABLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * @return the global log level
	 */
	public boolean isGlobalLogLevel() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GLOBAL_LOG_LEVEL, "true").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global log level
	 */
	public void setIsGlobalLogLevel(String isGlobalLogLevel) {
		this.setProperty(Settings.IS_GLOBAL_LOG_LEVEL, isGlobalLogLevel.trim());
	}

	/**
	 * @return the serial port name as string
	 */
	public String getSerialPort() {
		String port = getProperty(Settings.GLOBAL_PORT_NAME, Settings.EMPTY).trim();
		return port == null ? Settings.EMPTY : port;
	}

	/**
	 * set property if during port scan disable detection of bluetooth devices
	 */
	public void setSkipBluetoothDevices(boolean enabled) {
		setProperty(Settings.SKIP_BLUETOOTH_DEVICES, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * get property if during port scan disable detection of bluetooth devices
	 */
	public boolean isSkipBluetoothDevices() {
		return Boolean.valueOf(getProperty(Settings.SKIP_BLUETOOTH_DEVICES, "true")); //$NON-NLS-1$
	}

	/**
	 * set property if during port scan a availability check should executed (disable for slow systems)
	 */
	public void setPortAvailabilityCheck(boolean enabled) {
		setProperty(Settings.DO_PORT_AVAILABLE_TEST, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * get property if during port scan a availability check should executed (disable for slow systems)
	 */
	public boolean doPortAvailabilityCheck() {
		return Boolean.valueOf(getProperty(Settings.DO_PORT_AVAILABLE_TEST, "false")); //$NON-NLS-1$
	}

	/**
	 * set the decimal separator
	 */
	public void setSerialPort(String newSerialPort) {
		this.setProperty(Settings.GLOBAL_PORT_NAME, newSerialPort.trim());
	}

	/**
	 * check if minimal settings available
	 */
	public boolean isOK() {
		boolean ok = false;
		if (getProperty(Settings.DATA_FILE_PATH) != null || getProperty(Settings.LIST_SEPARATOR) != null || getProperty(Settings.DECIMAL_SEPARATOR) != null || getProperty(Settings.IS_GLOBAL_PORT) != null
				|| getProperty(Settings.GLOBAL_PORT_NAME) != null) {
			ok = true;
		}

		return ok;
	}

	/**
	 * query if device tool box to be opened right after closing device selection dialog
	 */
	public boolean isAutoOpenToolBox() {
		return Boolean.valueOf(this.getProperty(Settings.AUTO_OPEN_TOOL_BOX, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * query if record set comment window is visible
	 */
	public boolean isRecordCommentVisible() {
		return Boolean.valueOf(this.getProperty(Settings.RECORD_COMMENT_VISIBLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set property if record set comment window is visible
	 */
	public void setRecordCommentVisible(boolean enabled) {
		this.setProperty(Settings.RECORD_COMMENT_VISIBLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query if record set comment window is visible
	 */
	public boolean isGraphicsHeaderVisible() {
		return Boolean.valueOf(this.getProperty(Settings.GRAPHICS_HEADER_VISIBLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set property if record set comment window is visible
	 */
	public void setGraphicsHeaderVisible(boolean enabled) {
		this.setProperty(Settings.GRAPHICS_HEADER_VISIBLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the grid line style
	 * @return actual grid line style as integer array
	 */
	public int[] getGridDashStyle() {
		String[] gridLineStyle = this.getProperty(Settings.GRID_DASH_STYLE, "10, 10").split(GDE.STRING_COMMA); //$NON-NLS-1$
		return new int[] { new Integer(gridLineStyle[0].trim()).intValue(), new Integer(gridLineStyle[1].trim()).intValue() };
	}

	/**
	 * @return actual grid line style as string integer array
	 */
	private String getGridDashStyleAsString() {
		return this.getProperty(Settings.GRID_DASH_STYLE, "10, 10").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid line style in pixel length
	 * @param newGridDashStyle {drawn, blank}
	 */
	public void setGridDaschStyle(int[] newGridDashStyle) {
		this.setProperty(Settings.GRID_DASH_STYLE, GDE.STRING_EMPTY + newGridDashStyle[0] + ", " + newGridDashStyle[1]); //$NON-NLS-1$
	}

	/**
	 * @return the grid horizontal type of the compare window (0=none;1=each,2=eachSecond)
	 */
	public int getGridCompareWindowHorizontalType() {
		return new Integer(this.getProperty(Settings.GRID_COMPARE_WINDOW_HOR_TYPE, "0").trim()).intValue(); //$NON-NLS-1$
	}

	/**
	 * set the grid horizontal type of the compare window
	 * @param newHorizontalGridType (0=none;1=each,2=eachSecond)
	 */
	public void setGridCompareWindowHorizontalType(int newHorizontalGridType) {
		this.setProperty(Settings.GRID_COMPARE_WINDOW_HOR_TYPE, GDE.STRING_EMPTY + newHorizontalGridType);
	}

	/**
	 * @return the grid horizontal color of the compare window (r,g,b)
	 */
	public Color getGridCompareWindowHorizontalColor() {
		return getColor(Settings.GRID_COMPARE_WINDOW_HOR_COLOR, "200,200,200"); //$NON-NLS-1$
	}

	/**
	 * @return the grid horizontal color of the compare window as string of (r,g,b)
	 */
	public String getGridCompareWindowHorizontalColorStr() {
		return this.getProperty(Settings.GRID_COMPARE_WINDOW_HOR_COLOR, "200,200,200").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid horizontal color of the compare window
	 * @param newColor (r,g,b)
	 */
	public void setGridCompareWindowHorizontalColor(Color newColor) {
		String rgb = newColor.getRGB().red + GDE.STRING_COMMA + newColor.getRGB().green + GDE.STRING_COMMA + newColor.getRGB().blue;
		this.setProperty(Settings.GRID_COMPARE_WINDOW_HOR_COLOR, rgb);
	}

	/**
	 * @return the grid vertical type of the compare window (0=none;1=each,2=mod60)
	 */
	public int getGridCompareWindowVerticalType() {
		return new Integer(this.getProperty(Settings.GRID_COMPARE_WINDOW_VER_TYPE, "0").trim()).intValue(); //$NON-NLS-1$
	}

	/**
	 * set the grid vertical type of the compare window
	 * @param newVerticalGridType (0=none;1=each,2=eachSecond)
	 */
	public void setGridCompareWindowVerticalType(int newVerticalGridType) {
		this.setProperty(Settings.GRID_COMPARE_WINDOW_VER_TYPE, GDE.STRING_EMPTY + newVerticalGridType);
	}

	/**
	 * @return the grid vertical color of the compare window (r,g,b)
	 */
	public Color getGridCompareWindowVerticalColor() {
		return getColor(Settings.GRID_COMPARE_WINDOW_VER_COLOR, "200,200,200"); //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGridCompareWindowVerticalColorStr() {
		return this.getProperty(Settings.GRID_COMPARE_WINDOW_VER_COLOR, "200,200,200").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid vertical color of the compare window
	 * @param newColor (r,g,b)
	 */
	public void setGridCompareWindowVerticalColor(Color newColor) {
		String rgb = newColor.getRGB().red + GDE.STRING_COMMA + newColor.getRGB().green + GDE.STRING_COMMA + newColor.getRGB().blue;
		this.setProperty(Settings.GRID_COMPARE_WINDOW_VER_COLOR, rgb);
	}

	/**
	 * set global log level
	 */
	public void setGlobalLogLevel(java.util.logging.Level logLevel) {
		Logger logger = Logger.getLogger(GDE.STRING_EMPTY);
		logger.setLevel(logLevel);
		logger.setUseParentHandlers(true);
	}

	/**
	 * set individual log level
	 */
	public void setIndividualLogLevel(String packageName, java.util.logging.Level logLevel) {
		Logger logger = Logger.getLogger(packageName);
		logger.setLevel(logLevel);
		logger.setUseParentHandlers(true);
	}

	/**
	 * method to update the logging level
	 */
	public void updateLogLevel() {
		if (isGlobalLogLevel()) {
			java.util.logging.Level globalLogLevel = java.util.logging.Level.parse(getProperty(Settings.GLOBAL_LOG_LEVEL, "WARNING").trim()); //$NON-NLS-1$
			setIndividualLogLevel("gde.ui", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.data", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.histocache", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.config", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.device", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.utils", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("gde.io", globalLogLevel); //$NON-NLS-1$
			setGlobalLogLevel(globalLogLevel);
			setLevelSerialIO(globalLogLevel);
			Enumeration<Object> e = Settings.classbasedLogger.keys();
			while (e.hasMoreElements()) {
				String loggerName = (String) e.nextElement();
				setIndividualLogLevel(loggerName, java.util.logging.Level.parse("SEVERE")); //$NON-NLS-1$
			}
			Settings.classbasedLogger.clear();
		}
		else {
			setGlobalLogLevel(java.util.logging.Level.parse(getProperty(Settings.GLOBAL_LOG_LEVEL, "WARNING").trim())); //$NON-NLS-1$
			setIndividualLogLevel("gde.ui", getLogLevel(Settings.UI_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.data", getLogLevel(Settings.DATA_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.histocache", getLogLevel(Settings.DATA_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.config", getLogLevel(Settings.CONFIG_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.device", getLogLevel(Settings.DEVICE_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.utils", getLogLevel(Settings.UTILS_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("gde.io", getLogLevel(Settings.FILE_IO_LOG_LEVEL)); //$NON-NLS-1$
			setLevelSerialIO(getLogLevel(Settings.SERIAL_IO_LOG_LEVEL));
			Enumeration<Object> e = Settings.classbasedLogger.keys();
			while (e.hasMoreElements()) {
				String loggerName = (String) e.nextElement();
				setIndividualLogLevel(loggerName, java.util.logging.Level.parse(Settings.classbasedLogger.getProperty(loggerName)));
			}
		}
	}

	/**
	 * @return log level from the given categorie, in case of parse error fall back to Level.WARNING
	 */
	java.util.logging.Level getLogLevel(String logCategorie) {
		java.util.logging.Level logLevel = java.util.logging.Level.WARNING;
		try {
			logLevel = java.util.logging.Level.parse(getProperty(logCategorie, "WARNING").trim()); //$NON-NLS-1$
		}
		catch (IllegalArgumentException e) {
			// ignore and fall back to WARNING
			setProperty(logCategorie, "WARNING"); //$NON-NLS-1$
		}
		return logLevel;
	}

	private void setLevelSerialIO(java.util.logging.Level logLevel) {
		final String $METHOD_NAME = "setLevelSerialIO"; //$NON-NLS-1$
		try {
			Logger logger = Logger.getLogger("gde.comm.DeviceSerialPortImpl"); //$NON-NLS-1$
			for (Handler handler : logger.getHandlers()) {
				logger.removeHandler(handler);
			}
			logger.setLevel(logLevel);
			if (logLevel.intValue() < java.util.logging.Level.parse("WARNING").intValue()) { //$NON-NLS-1$
				logger.setUseParentHandlers(false);
				Handler fh = new FileHandler(this.getSerialLogFilePath(), 15000000, 3);
				fh.setFormatter(new LogFormatter());
				logger.addHandler(new MemoryHandler(fh, 5000000, logLevel));
			}
			else
				logger.setUseParentHandlers(true); // enable global log settings

		}
		catch (Exception e) {
			Settings.log.logp(java.util.logging.Level.WARNING, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
	}

	/**
	 * set device dialog behavior, application modal or application equivalent
	 * @param enabled
	 */
	public void enabelModalDeviceDialogs(boolean enabled) {
		this.setProperty(Settings.DEVICE_DIALOG_USE_MODAL, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the the device dialogs behavior
	 * @return boolean value to signal the modality of the device dialog
	 */
	public boolean isDeviceDialogsModal() {
		return Boolean.valueOf(this.getProperty(Settings.DEVICE_DIALOG_USE_MODAL, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set device dialog behavior, always visible
	 * @param enabled
	 */
	public void enabelDeviceDialogsOnTop(boolean enabled) {
		this.setProperty(Settings.DEVICE_DIALOG_ON_TOP, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the the device dialogs behavior
	 * @return boolean value to signal the placement of the device dialog
	 */
	public boolean isDeviceDialogsOnTop() {
		return Boolean.valueOf(this.getProperty(Settings.DEVICE_DIALOG_ON_TOP, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * @return the unmarshaller
	 */
	public Unmarshaller getUnmarshaller() {
		return this.unmarshaller;
	}

	/**
	 * @return the marshaller
	 */
	public Marshaller getMarshaller() {
		return this.marshaller;
	}

	/**
	 * query if locale has been changed, this will be used to copy new set of device property files to users application directory
	 * @return
	 */
	boolean getLocaleChanged() {
		return Boolean.valueOf(this.getProperty(Settings.LOCALE_CHANGED, "false")); //$NON-NLS-1$
	}

	/**
	 * set new locale language (de,en, ..), if language has been changed set locale changed to true to indicate copy device properties
	 * @param newLanguage
	 */
	public void setLocaleLanguage(String newLanguage) {
		if (!this.getLocale().getLanguage().equals(newLanguage)) {
			this.setProperty(Settings.LOCALE_CHANGED, "true"); //$NON-NLS-1$
			this.setProperty(Settings.LOCALE_IN_USE, newLanguage);
		}
	}

	/**
	 * query the locale language (en, de, ...) to be used to copy set of localized device property files
	 * if local is not supported, ENGLISH is used as default
	 * @return used locale
	 */
	public Locale getLocale() {
		// get the locale from loaded properties or system default
		Locale locale = new Locale(this.getProperty(Settings.LOCALE_IN_USE, Locale.getDefault().getLanguage()));
		if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) || locale.getLanguage().equals(Locale.GERMAN.getLanguage()) || locale.getLanguage().equals(Locale.ITALIAN.getLanguage())) {
			this.setProperty(Settings.LOCALE_IN_USE, locale.getLanguage());
		}
		else {
			this.setProperty(Settings.LOCALE_IN_USE, Locale.ENGLISH.getLanguage()); // fall back to 'en' as default for not supported localse
			locale = Locale.ENGLISH;
		}
		return locale;
	}

	/**
	 * set new time format to be used in table, while measurements and at start time while zooming
	 * @param newTimeFormat
	 */
	public void setTimeFormat(String newTimeFormat) {
		this.setProperty(Settings.TIME_FORMAT_IN_USE, newTimeFormat);
	}

	/**
	 * query the time format to be used in table, while measurements and at start time while zooming
	 * @return used time format as string
	 */
	public String getTimeFormat() {
		return this.getProperty(Settings.TIME_FORMAT_IN_USE, Messages.getString(MessageIds.GDE_MSGT0684));
	}

	/**
	 * query the time format is set to absolute
	 * @return used time format as string
	 */
	public boolean isTimeFormatAbsolute() {
		return this.getProperty(Settings.TIME_FORMAT_IN_USE, Messages.getString(MessageIds.GDE_MSGT0684)).trim().equals(Messages.getString(MessageIds.GDE_MSGT0359));
	}

	/**
	 * get the device dialog alpha value, 50 is a good transparency starting point
	 * @return the alphablending value
	 */
	public int getDialogAlphaValue() {
		return new Integer(this.getProperty(Settings.ALPHA_BLENDING_VALUE, "50").trim()).intValue(); //$NON-NLS-1$
	}

	/**
	 * set a new alpha transparency value for the device dialog
	 * @param newAlphaValue
	 */
	public void setDialogAlphaValue(int newAlphaValue) {
		this.setProperty(Settings.ALPHA_BLENDING_VALUE, GDE.STRING_EMPTY + newAlphaValue);
	}

	/**
	 * set if alpha blending for device dialog should be used
	 * (supporting window manager is pre-req, covered by SWT)
	 * @param enable
	 */
	public void setDeviceDialogAlphaEnabled(boolean enable) {
		this.setProperty(Settings.APLHA_BLENDING_ENABLED, GDE.STRING_EMPTY + enable);
	}

	/**
	 * query usage of alpha blending for device dialog
	 * @return true if alphablending is enabled
	 */
	public boolean isDeviceDialogAlphaEnabled() {
		return Boolean.valueOf(this.getProperty(Settings.APLHA_BLENDING_ENABLED, "false")); //$NON-NLS-1$
	}

	/**
	 * set if the device import directory suggestion is independent of selected object key
	 * @param enable
	 */
	public void setDeviceImportDirectoryObjectRelated(boolean enable) {
		this.setProperty(Settings.KEEP_IMPORT_DIR_OBJECT_RELATED, GDE.STRING_EMPTY + enable);
	}

	/**
	 * query usage of device import directory
	 * @return true if directory suggestion follows object key
	 */
	public boolean isDeviceImportDirectoryObjectRelated() {
		return Boolean.valueOf(this.getProperty(Settings.KEEP_IMPORT_DIR_OBJECT_RELATED, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the isDevicePropertiesUpdated
	 */
	public boolean isDevicePropertiesUpdated() {
		return this.isDevicePropertiesUpdated;
	}

	/**
	 * @return the isGraphicsTemplateUpdated
	 */
	public boolean isGraphicsTemplateUpdated() {
		return this.isGraphicsTemplateUpdated;
	}

	/**
	 * @return the isHistoCacheTemplateUpdated
	 */
	public boolean isHistoCacheTemplateUpdated() {
		return this.isHistocacheTemplateUpdated;
	}

	/**
	 * query value if desktop shortcut needs to be created
	 */
	public boolean isDesktopShortcutCreated() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DESKTOP_SHORTCUT_CREATED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if DataExplorer application is registered to operating system
	 */
	public boolean isApplicationRegistered() {
		return Boolean.valueOf(this.getProperty(Settings.IS_APPL_REGISTERED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if a hint was displayed to enalble uucp locking used on UNIX based systems with RXTXcomm
	 */
	public boolean isLockUucpHinted() {
		return Boolean.valueOf(this.getProperty(Settings.IS_LOCK_UUCP_HINTED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if a hint was displayed to enalble uucp locking used on UNIX based systems with RXTXcomm
	 */
	public boolean isUpdateChecked() {
		return this.getProperty(Settings.LAST_UPDATE_CHECK, "2000-01-01").equals(StringHelper.getDate()); //$NON-NLS-1$
	}

	/**
	 * set the background color of the main graphics curve area
	 * @param curveAreaBackground
	 */
	public void setGraphicsCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(Settings.GRAPHICS_AREA_BACKGROUND, curveAreaBackground.getRed() + GDE.STRING_COMMA + curveAreaBackground.getGreen() + GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * set the background color of the main graphics curve area
	 * @return requested color
	 */
	public Color getGraphicsCurveAreaBackground() {
		return getColor(Settings.GRAPHICS_AREA_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsCurveAreaBackgroundStr() {
		return this.getProperty(Settings.GRAPHICS_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve area
	 * @param curveAreaBackground
	 */
	public void setCompareCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(Settings.COMPARE_AREA_BACKGROUND, curveAreaBackground.getRed() + GDE.STRING_COMMA + curveAreaBackground.getGreen() + GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve area
	 * @return requested color
	 */
	public Color getCompareCurveAreaBackground() {
		return getColor(Settings.COMPARE_AREA_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCompareCurveAreaBackgroundStr() {
		return this.getProperty(Settings.COMPARE_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve area
	 * @param curveAreaBackground
	 */
	public void setUtilityCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(Settings.UTILITY_AREA_BACKGROUND, curveAreaBackground.getRed() + GDE.STRING_COMMA + curveAreaBackground.getGreen() + GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve area
	 * @return requested color
	 */
	public Color getUtilityCurveAreaBackground() {
		return getColor(Settings.UTILITY_AREA_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getUtilityCurveAreaBackgroundStr() {
		return this.getProperty(Settings.UTILITY_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the main graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setGraphicsSurroundingBackground(Color surroundingBackground) {
		this.setProperty(Settings.GRAPHICS_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the main graphics curve surrounding area
	 * @return requested color
	 */
	public Color getGraphicsSurroundingBackground() {
		return getColor(Settings.GRAPHICS_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsSurroundingBackgroundStr() {
		return this.getProperty(Settings.GRAPHICS_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setCompareSurroundingBackground(Color surroundingBackground) {
		this.setProperty(Settings.COMPARE_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve surrounding area
	 * @return requested color
	 */
	public Color getCompareSurroundingBackground() {
		return getColor(Settings.COMPARE_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCompareSurroundingBackgroundStr() {
		return this.getProperty(Settings.COMPARE_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setUtilitySurroundingBackground(Color surroundingBackground) {
		this.setProperty(Settings.UTILITY_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve surrounding area
	 * @return requested color
	 */
	public Color getUtilitySurroundingBackground() {
		return getColor(Settings.UTILITY_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getUtilitySurroundingBackgroundStr() {
		return this.getProperty(Settings.UTILITY_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the graphics curve area
	 * @param borderColor
	 */
	public void setCurveGraphicsBorderColor(Color borderColor) {
		this.setProperty(Settings.GRAPHICS_BORDER_COLOR, borderColor.getRed() + GDE.STRING_COMMA + borderColor.getGreen() + GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * get the border color of the graphics curve area
	 * @return requested color
	 */
	public Color getGraphicsCurvesBorderColor() {
		return getColor(Settings.GRAPHICS_BORDER_COLOR, "180,180,180"); //COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsCurvesBorderColorStr() {
		return this.getProperty(Settings.GRAPHICS_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the curve compare graphics area
	 * @param borderColor
	 */
	public void setCurveCompareBorderColor(Color borderColor) {
		this.setProperty(Settings.COMPARE_BORDER_COLOR, borderColor.getRed() + GDE.STRING_COMMA + borderColor.getGreen() + GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * set the background color of the curve compare graphics area
	 * @return requested color
	 */
	public Color getCurveCompareBorderColor() {
		return getColor(Settings.COMPARE_BORDER_COLOR, "180,180,180"); //COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCurveCompareBorderColorStr() {
		return this.getProperty(Settings.COMPARE_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the graphics curve area
	 * @param borderColor
	 */
	public void setUtilityCurvesBorderColor(Color borderColor) {
		this.setProperty(Settings.UTILITY_BORDER_COLOR, borderColor.getRed() + GDE.STRING_COMMA + borderColor.getGreen() + GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * get the border color of the graphics curve area
	 * @return requested color
	 */
	public Color getUtilityCurvesBorderColor() {
		return getColor(Settings.UTILITY_BORDER_COLOR, "180,180,180"); //COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the utility window as string of (r,g,b)
	 */
	public String getUtilityCurvesBorderColorStr() {
		return this.getProperty(Settings.UTILITY_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the statistics window surrounding area
	 * @param surroundingBackground
	 */
	public void setSatisticsSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.STATISTICS_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getStatisticsSurroundingAreaBackground() {
		return getColor(Settings.STATISTICS_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getStatisticsSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.STATISTICS_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the statistics window surrounding area
	 * @param innerAreaBackground
	 */
	public void setSatisticsInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.STATISTICS_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getStatisticsInnerAreaBackground() {
		return getColor(Settings.STATISTICS_INNER_BACKGROUND, "255,255,255"); //COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getStatisticsInnerAreaBackgroundStr() {
		return this.getProperty(Settings.STATISTICS_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the analog window surrounding area
	 * @param surroundingBackground
	 */
	public void setAnalogSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.ANALOG_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the analog window surrounding area
	 * @return requested color
	 */
	public Color getAnalogSurroundingAreaBackground() {
		return getColor(Settings.ANALOG_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getAnalogSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.ANALOG_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the analog window surrounding area
	 * @param surroundingBackground
	 */
	public void setAnalogInnerAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.ANALOG_INNER_BACKGROUND, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the analog window surrounding area
	 * @return requested color
	 */
	public Color getAnalogInnerAreaBackground() {
		return getColor(Settings.ANALOG_INNER_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getAnalogInnerAreaBackgroundStr() {
		return this.getProperty(Settings.ANALOG_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the digital window surrounding area
	 * @param surroundingBackground
	 */
	public void setDigitalSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.DIGITAL_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the digital window surrounding area
	 * @return requested color
	 */
	public Color getDigitalSurroundingAreaBackground() {
		return getColor(Settings.DIGITAL_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getDigitalSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.DIGITAL_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the digital window surrounding area
	 * @param innerAreaBackground
	 */
	public void setDigitalInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.DIGITAL_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getDigitalInnerAreaBackground() {
		return getColor(Settings.DIGITAL_INNER_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getDigitalInnerAreaBackgroundStr() {
		return this.getProperty(Settings.DIGITAL_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the cell voltage window surrounding area
	 * @param surroundingBackground
	 */
	public void setCellVoltageSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.CELL_VOLTAGE_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the cell voltage window surrounding area
	 * @return requested color
	 */
	public Color getCellVoltageSurroundingAreaBackground() {
		return getColor(Settings.CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCellVoltageSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the cell voltage window surrounding area
	 * @param innerAreaBackground
	 */
	public void setCellVoltageInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.CELL_VOLTAGE_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the cell voltage window surrounding area
	 * @return requested color
	 */
	public Color getCellVoltageInnerAreaBackground() {
		return getColor(Settings.CELL_VOLTAGE_INNER_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCellVoltageInnerAreaBackgroundStr() {
		return this.getProperty(Settings.CELL_VOLTAGE_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the file comment window surrounding area
	 * @param surroundingBackground
	 */
	public void setFileCommentSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.FILE_COMMENT_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the file comment window surrounding area
	 * @return requested color
	 */
	public Color getFileCommentSurroundingAreaBackground() {
		return getColor(Settings.FILE_COMMENT_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getFileCommentSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.FILE_COMMENT_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the file comment window surrounding area
	 * @param innerAreaBackground
	 */
	public void setFileCommentInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.FILE_COMMENT_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the file comment window surrounding area
	 * @return requested color
	 */
	public Color getFileCommentInnerAreaBackground() {
		return getColor(Settings.FILE_COMMENT_INNER_BACKGROUND, "255,255,255"); //COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getFileCommentInnerAreaBackgroundStr() {
		return this.getProperty(Settings.FILE_COMMENT_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the object description window surrounding area
	 * @param surroundingBackground
	 */
	public void setObjectDescriptionSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(Settings.OBJECT_DESC_SURROUND_BACKGRD, surroundingBackground.getRed() + GDE.STRING_COMMA + surroundingBackground.getGreen() + GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the object description window surrounding area
	 * @return requested color
	 */
	public Color getObjectDescriptionSurroundingAreaBackground() {
		return getColor(Settings.OBJECT_DESC_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getObjectDescriptionSurroundingAreaBackgroundStr() {
		return this.getProperty(Settings.OBJECT_DESC_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the object description window surrounding area
	 * @param innerAreaBackground
	 */
	public void setObjectDescriptionInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(Settings.OBJECT_DESC_INNER_BACKGROUND, innerAreaBackground.getRed() + GDE.STRING_COMMA + innerAreaBackground.getGreen() + GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the object description window surrounding area
	 * @return requested color
	 */
	public Color getObjectDescriptionInnerAreaBackground() {
		return getColor(Settings.OBJECT_DESC_INNER_BACKGROUND, "255,255,255"); //COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getObjectDescriptionInnerAreaBackgroundStr() {
		return this.getProperty(Settings.OBJECT_DESC_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * @return color according the given color key or the default value
	 */
	private Color getColor(String colorKey, String colorDefault) {
		String color = this.getProperty(colorKey, colorDefault); // CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230"
		int r = new Integer(color.split(GDE.STRING_COMMA)[0].trim()).intValue();
		int g = new Integer(color.split(GDE.STRING_COMMA)[1].trim()).intValue();
		int b = new Integer(color.split(GDE.STRING_COMMA)[2].trim()).intValue();
		return SWTResourceManager.getColor(r, g, b);
	}

	/**
	 * @return true if the xsdThread is alive
	 */
	public boolean isXsdThreadAlive() {
		return this.xsdThread != null ? this.xsdThread.isAlive() : false;
	}

	public void startMigationThread() {
		if (this.migrationThread != null) this.migrationThread.run();
	}

	/**
	 * @return true if the xsdThread is alive
	 */
	public boolean isMigationThreadAlive() {
		return this.migrationThread != null ? this.migrationThread.isAlive() : false;
	}

	/**
	 * @return the image data file path
	 */
	public String getObjectImageFilePath() {
		final String $METHOD_NAME = "getObjectImageFilePath"; //$NON-NLS-1$
		String objectImageFilePath = this.getProperty(Settings.OBJECT_IMAGE_FILE_PATH, getDataFilePath()).replace("\\\\", GDE.FILE_SEPARATOR_UNIX).replace(GDE.FILE_SEPARATOR_WINDOWS, //$NON-NLS-1$
				GDE.FILE_SEPARATOR_UNIX);
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "objectImageFilePath = " + objectImageFilePath); //$NON-NLS-1$
		return objectImageFilePath.trim();
	}

	/**
	 * set the image data file path
	 */
	public void setObjectImageFilePath(String newImageFilePath) {
		final String $METHOD_NAME = "setObjectImageFilePath"; //$NON-NLS-1$
		String filePath = newImageFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX).trim();
		Settings.log.logp(java.util.logging.Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "newDataFilePath = " + filePath); //$NON-NLS-1$
		this.setProperty(Settings.OBJECT_IMAGE_FILE_PATH, filePath);
	}

	/**
	 * set boolean value if the scale of the records drawn in graphics should be in same color as the curve
	 * @param isUseRecordColor
	 */
	public void setDrawScaleInRecordColor(boolean isUseRecordColor) {
		this.setProperty(Settings.IS_GRAPHICS_SCALE_COLOR, GDE.STRING_EMPTY + isUseRecordColor);
	}

	/**
	 * @return boolean value of true if the scale of the records drawn in graphics should be in same color as the curve
	 */
	public boolean isDrawScaleInRecordColor() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_SCALE_COLOR, "true")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the name of the records drawn in graphics should be in same color as the curve
	 * @param isUseRecordColor
	 */
	public void setDrawNameInRecordColor(boolean isUseRecordColor) {
		this.setProperty(Settings.IS_GRAPHICS_NAME_COLOR, GDE.STRING_EMPTY + isUseRecordColor);
	}

	/**
	 * @return boolean value of true if the name at the scale of the records drawn in graphics should be in same color as the curve
	 */
	public boolean isDrawNameInRecordColor() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_NAME_COLOR, "true")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the scale of the records drawn in graphics should be in same color as the curve
	 * @param isUseRecordColor
	 */
	public void setDrawNumbersInRecordColor(boolean isUseRecordColor) {
		this.setProperty(Settings.IS_GRAPHICS_NUMBERS_COLOR, GDE.STRING_EMPTY + isUseRecordColor);
	}

	/**
	 * @return boolean value of true if the number at the scale of the records drawn in graphics should be in same color as the curve
	 */
	public boolean isDrawNumbersInRecordColor() {
		return Boolean.valueOf(this.getProperty(Settings.IS_GRAPHICS_NUMBERS_COLOR, "false")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the channel/configuration name should be used as leader of record name in curve compare
	 * @param isUseChannelConfigName
	 */
	public void setCurveCompareChannelConfigName(boolean isUseChannelConfigName) {
		this.setProperty(Settings.IS_COMPARE_CHANNELCONFIG, GDE.STRING_EMPTY + isUseChannelConfigName);
	}

	/**
	 * @return boolean value of true if the channel/configuration name should be used as leader of record name in curve compare
	 */
	public boolean isCurveCompareChannelConfigName() {
		return Boolean.valueOf(this.getProperty(Settings.IS_COMPARE_CHANNELCONFIG, "false")); //$NON-NLS-1$
	}

	/**
	 * set double value of display density font correction value
	 */
	public void setFontDisplayDensityAdaptionFactor(double newCorrectionValue) {
		this.setProperty(Settings.DISPLAY_DENSITY_FONT_CORRECT, GDE.STRING_EMPTY + newCorrectionValue);
	}

	/**
	 * @return double value of display density font correction value
	 */
	public double getFontDisplayDensityAdaptionFactor() {
		return Double.valueOf(this.getProperty(Settings.DISPLAY_DENSITY_FONT_CORRECT, "1.0")); //$NON-NLS-1$;
	}

	/**
	 * set boolean value if the history analysis tabs should be visible
	 * @param isActive
	 */
	public void setHistoActive(boolean isActive) {
		this.setProperty(Settings.IS_HISTO_ACTIVE, String.valueOf(isActive));
	}

	/**
	 * @return boolean true if the history labels should be visible if the current device supports the history
	 */
	public boolean isHistoActive() {
		return Boolean.valueOf(this.getProperty(Settings.IS_HISTO_ACTIVE, "false")); //$NON-NLS-1$
	}

	/**
	 * set boolean value if the history analysis contains quantile values instead on the legacy statistics
	 * @param isActive
	 */
	public void setSmartStatistics(boolean isActive) {
		this.setProperty(Settings.IS_SMART_STATISTICS, String.valueOf(isActive));
	}

	/**
	 * @return boolean true if the history analysis contains quantile values instead on the legacy statistics
	 */
	public boolean isSmartStatistics() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SMART_STATISTICS, "true")); //$NON-NLS-1$
	}

	/**
	 * @return three boxplot graphics sizes as localized texts
	 */
	public String[] getBoxplotScaleNomenclatures() {
		return Messages.getString(MessageIds.GDE_MSGT0802).split(GDE.STRING_COMMA);
	}

	/**
	 * set the boxplot size for the history
	 * @param scaleNomenclature
	 */
	public void setBoxplotScale(String scaleNomenclature) {
		this.setProperty(Settings.BOXPLOT_SCALE_ORDINAL, String.valueOf(Arrays.asList(getBoxplotScaleNomenclatures()).indexOf(scaleNomenclature)));
	}

	/**
	 * @return the boxplot size for the history (default is medium size)
	 */
	public String getBoxplotScale() {
		return getBoxplotScaleNomenclatures()[getBoxplotScaleOrdinal()];
	}

	/**
	 * @return the boxplot size ordinal for the history (default is medium size)
	 */
	public int getBoxplotScaleOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.BOXPLOT_SCALE_ORDINAL, String.valueOf(1)));
	}

	/**
	 * @return four boxplot size adaptation levels as localized texts ranging from none to large. the adaptation is based on the log duration.
	 */
	public String[] getBoxplotSizeAdaptationNomenclatures() {
		return Messages.getString(MessageIds.GDE_MSGT0803).split(GDE.STRING_COMMA);
	}

	/**
	 * set the boxplot size adaptation level for the history
	 * @param scaleNomenclature
	 */
	public void setBoxplotSizeAdaptation(String scaleNomenclature) {
		this.setProperty(Settings.BOXPLOT_SIZE_ADAPTATION_ORDINAL, String.valueOf(Arrays.asList(getBoxplotSizeAdaptationNomenclatures()).indexOf(scaleNomenclature)));
	}

	/**
	 * @return the boxplot size adaptation level for the history (default is medium adaptation)
	 */
	public String getBoxplotSizeAdaptation() {
		return getBoxplotSizeAdaptationNomenclatures()[getBoxplotSizeAdaptationOrdinal()];
	}

	/**
	 * @return the ordinal of the boxplot size adaptation level for the history (default is medium adaptation)
	 */
	public int getBoxplotSizeAdaptationOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.BOXPLOT_SIZE_ADAPTATION_ORDINAL, String.valueOf(2)));
	}

	/**
	 * @return six spreading labels starting with 0 to 5 for the history x axis
	 */
	public String[] getXAxisSpreadGradeNomenclatures() {
		return Messages.getString(MessageIds.GDE_MSGT0823).split(GDE.STRING_COMMA);
	}

	/**
	 * set the extent of logarithmic spreading of the x axis distances between trails
	 * @param gradeText
	 */
	public void setXAxisSpreadGrade(String gradeText) {
		this.setProperty(Settings.X_SPREAD_GRADE_ORDINAL, String.valueOf(Arrays.asList(getXAxisSpreadGradeNomenclatures()).indexOf(gradeText)));
	}

	/**
	 * @return the extent of logarithmic spreading of the x axis distances between timesteps (default is grade 2 which is just before the middle of six grades)
	 */
	public String getXAxisSpreadGrade() {
		return getXAxisSpreadGradeNomenclatures()[getXAxisSpreadOrdinal()];
	}

	/**
	 * @return the ordinal of the extent of logarithmic spreading of the x axis distances between trails (default is grade 2 which is just before the middle of six grades)
	 */
	public int getXAxisSpreadOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.X_SPREAD_GRADE_ORDINAL, String.valueOf(2)));
	}

	/**
	 * set true if the history x axis distances between the timesteps are based on logarithmic values
	 * @param isActive
	 */
	public void setXAxisLogarithmicDistance(boolean isActive) {
		this.setProperty(Settings.IS_X_LOGARITHMIC_DISTANCE, String.valueOf(isActive));
	}

	/**
	 * @return true if the history x axis distances between the timesteps are based on logarithmic values
	 */
	public boolean isXAxisLogarithmicDistance() {
		return Boolean.valueOf(this.getProperty(Settings.IS_X_LOGARITHMIC_DISTANCE, "false")); //$NON-NLS-1$
	}

	/**
	 * set true if the history x axis starts with the most recent timesteps
	 * @param isActive
	 */
	public void setXAxisReversed(boolean isActive) {
		this.setProperty(Settings.IS_X_REVERSED, String.valueOf(isActive));
	}

	/**
	 * @return true if the history x axis starts with the most recent timesteps
	 */
	public boolean isXAxisReversed() {
		return Boolean.valueOf(this.getProperty(Settings.IS_X_REVERSED, "true")); //$NON-NLS-1$
	}

	/**
	* @param isActive true if files from the device import directory are read for the history
	*/
	public void setSearchImportPath(boolean isActive) {
		this.setProperty(Settings.SEARCH_IMPORT_PATH, String.valueOf(isActive));
	}

	/**
	 * @return true if files from the device import directory are read for the history
	 */
	public boolean getSearchImportPath() {
		return Boolean.valueOf(this.getProperty(Settings.SEARCH_IMPORT_PATH, String.valueOf(true)));
	}

	/**
	 * @param isActive true if import files from the data directory are read for the history
	 */
	public void setSearchDataPathImports(boolean isActive) {
		this.setProperty(Settings.SEARCH_DATAPATH_IMPORTS, String.valueOf(isActive));
	}

	/**
	 * @return true if import files from the data directory are read for the history
	 */
	public boolean getSearchDataPathImports() {
		return Boolean.valueOf(this.getProperty(Settings.SEARCH_DATAPATH_IMPORTS, String.valueOf(true)));
	}

	/**
	 * @param isActive true if channels with identical measurements are selected for the history
	 */
	public void setChannelMix(boolean isActive) {
		this.setProperty(Settings.IS_CHANNEL_MIX, String.valueOf(isActive));
	}

	/**
	 * @return true true if channels with identical measurements are selected for the history
	 */
	public boolean isChannelMix() {
		return Boolean.valueOf(this.getProperty(Settings.IS_CHANNEL_MIX, "true")); //$NON-NLS-1$
	}

	/**
	 * @return sampling timespan values in seconds with seven values ranging from 0.001 to 10.0 based on the current locale
	 */
	public String[] getSamplingTimespanValues() {
		double[] values_ss = { 10., 5., 1., .5, .1, .05, .001 };
		String[] textValues = new String[7];
		for (int i = 0; i < values_ss.length; i++) {
			textValues[i] = String.valueOf(values_ss[i]);
		}
		return textValues;
	}

	/**
	 * set the sampling time which defines the timespan for one single sample value for the history
	 * @param valueText
	 */
	public void setSamplingTimespan_ms(String valueText) {
		this.setProperty(Settings.SAMPLING_TIMESPAN_ORDINAL, String.valueOf(Arrays.asList(getSamplingTimespanValues()).indexOf(valueText)));
	}

	/**
	 * repairs the properties file setting if it holds an invalid index value.
	 * @return the sampling time which defines the timespan for one single sample value for the history (default is 1 sec)
	 */
	public int getSamplingTimespan_ms() {
		String textValue;
		try {
			textValue = getSamplingTimespanValues()[getSamplingTimespanOrdinal()];
		}
		catch (Exception e) {
			setSamplingTimespan_ms(Double.toString(1.)); // one second
			textValue = getSamplingTimespanValues()[getSamplingTimespanOrdinal()];
		}
		return (int) (Double.valueOf(textValue) * 1000.);
	}

	/**
	 * @return the ordinal of the sampling time which defines the timespan for one single sample value for the history (default is 1 ms which is the value at index 4)
	 */
	public int getSamplingTimespanOrdinal() {
		return Integer.parseInt(this.getProperty(Settings.SAMPLING_TIMESPAN_ORDINAL, String.valueOf(2)));
	}

	/**
	 * @param value true if the history should select files in the object directory which do not hold the object key internally
	 */
	public void setFilesWithoutObject(boolean value) {
		this.setProperty(Settings.FILES_WITHOUT_OBJECT, String.valueOf(value));
	}

	/**
	 * @return true if the history should select files in the object directory which do not hold the object key internally
	 */
	public boolean getFilesWithoutObject() {
		return Boolean.valueOf(this.getProperty(Settings.FILES_WITHOUT_OBJECT, "true")); //$NON-NLS-1$
	}

	/**
	 * @param value true if the history should select files in the object directory which hold a different object key internally
	 */
	public void setFilesWithOtherObject(boolean value) {
		this.setProperty(Settings.FILES_WITH_OTHER_OBJECT, String.valueOf(value));
	}

	/**
	 * @return true if the history should select files in the object directory which hold a different object key internally
	 */
	public boolean getFilesWithOtherObject() {
		return Boolean.valueOf(this.getProperty(Settings.FILES_WITH_OTHER_OBJECT, "true")); //$NON-NLS-1$
	}

	/**
	 * @return the maximum number of full calendar months which is used for history log selection (default is 12)
	 */
	public int getRetrospectMonths() {
		return Integer.valueOf(this.getProperty(Settings.RETROSPECT_MONTHS, String.valueOf(12)));
	}

	/**
	 * @param uintValue the maximum number of full calendar months which is used for history log selection
	 */
	public void setRetrospectMonths(String uintValue) {
		try {
			int value = Integer.parseUnsignedInt(uintValue.trim());
			if (value < 1 || value > 120) value = 12;
			this.setProperty(Settings.RETROSPECT_MONTHS, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * @return true if the history cache directories are zip files (performs better for more than about 100 directory entries)
	 */
	public boolean isZippedCache() {
		return Boolean.valueOf(this.getProperty(Settings.IS_ZIPPED_CACHE, "false")); //$NON-NLS-1$
	}

	/**
	 * @param value true if the history cache directories are zip files (performs better for more than about 100 directory entries)
	 */
	public void setZippedCache(boolean value) {
		this.setProperty(Settings.IS_ZIPPED_CACHE, String.valueOf(value));
	}

	/**
	 * @return the minmax distance value used in quantile calculations for settlements based on transitions
	 */
	public double getMinmaxQuantileDistance() {
		return Double.valueOf(this.getProperty(Settings.MINMAX_QUANTILE_DISTANCE, ".1")); //$NON-NLS-1$
	}

	/**
	 * @param doubleValue is the minmax distance value used in quantile calculations for settlements based on transitions
	 */
	public void setMinMaxQuantileDistance(String doubleValue) {
		try {
			double value = Double.parseDouble(doubleValue.trim());
			if (value > 1 || value < 0) value = .1;
			this.setProperty(Settings.MINMAX_QUANTILE_DISTANCE, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * small values select transitions with a smaller amplitude. values close to 1 only select peaks with an amplitude close to the min-max distance.
	 * @return the factor for the calculation of the minimum absolute transition level required for firing the trigger
	 */
	public double getAbsoluteTransitionLevel() {
		return Double.valueOf(this.getProperty(Settings.ABSOLUTE_TRANSITION_LEVEL, ".5")); //$NON-NLS-1$
	}

	/**
	 * small values select peaks with a smaller amplitude. values close to 1 only select peaks with an amplitude close to the min-max distance.
	 * @param doubleValue is the factor for the calculation of the minimum absolute transition level required for firing the trigger
	 */
	public void setAbsoluteTransitionLevel(String doubleValue) {
		try {
			double value = Double.parseDouble(doubleValue.trim());
			if (value >= 1 || value <= 0) value = .5;
			this.setProperty(Settings.ABSOLUTE_TRANSITION_LEVEL, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * @param isUtc true if the date and time values output are formatted in UTC
	 */
	public void setDateTimeUtc(boolean isUtc) {
		this.setProperty(Settings.IS_DATETIME_UTC, String.valueOf(isUtc));
	}

	/**
	 * @return true if the date and time values output are formatted in UTC
	 */
	public boolean isDateTimeUtc() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATETIME_UTC, "false")); //$NON-NLS-1$
	}

	/**
	 * @param isDisplayActive true if the log properties are displayed in the histo table tab
	 */
	public void setDisplayTags(boolean isDisplayActive) {
		this.setProperty(Settings.IS_DISPLAY_TAGS, String.valueOf(isDisplayActive));
	}

	/**
	 * @return true if the log properties are displayed in the histo table tab
	 */
	public boolean isDisplayTags() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DISPLAY_TAGS, "true")); //$NON-NLS-1$
	}

	/**
	 * @param isDisplayActive true if the history scores are displayed in the histo tabs
	 */
	public void setDisplayScores(boolean isDisplayActive) {
		this.setProperty(Settings.IS_DISPLAY_SCORES, String.valueOf(isDisplayActive));
	}

	/**
	 * @return true if the history scores are displayed in the histo tabs
	 */
	public boolean isDisplayScores() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DISPLAY_SCORES, "true")); //$NON-NLS-1$
	}

	/**
	 * @param isDisplayActive true if the history settlements are displayed in the histo tabs
	 */
	public void setDisplaySettlements(boolean isDisplayActive) {
		this.setProperty(Settings.IS_DISPLAY_SETTLEMENTS, String.valueOf(isDisplayActive));
	}

	/**
	 * @return true if the history settlements are displayed in the histo tabs
	 */
	public boolean isDisplaySettlements() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DISPLAY_SETTLEMENTS, "false")); //$NON-NLS-1$
	}

	/**
	 * @param isDataSettingsAtHomePath true if the history data settings are stored in the user's home path
	 */
	public void setDataSettingsAtHomePath(boolean isDataSettingsAtHomePath) {
		this.setProperty(Settings.IS_DATA_SETTINGS_AT_HOME_PATH, String.valueOf(isDataSettingsAtHomePath));
	}

	/**
	 * @return true if the history data settings are stored in the user's home path
	 */
	public boolean isDataSettingsAtHomePath() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATA_SETTINGS_AT_HOME_PATH, "true")); //$NON-NLS-1$
	}

	/**
	 * @param isSuppressMode true if ignoring recordsets in the history is active
	 */
	public void setSuppressMode(boolean isSuppressMode) {
		this.setProperty(Settings.IS_SUPPRESS_MODE, String.valueOf(isSuppressMode));
	}

	/**
	 * @return true if ignoring recordsets in the history is active
	 */
	public boolean isSuppressMode() {
		return Boolean.valueOf(this.getProperty(Settings.IS_SUPPRESS_MODE, "false")); //$NON-NLS-1$
	}

	/**
	 * @param isCurveSurvey true extended curve delta measuring display
	 */
	public void setCurveSurvey(boolean isCurveSurvey) {
		this.setProperty(Settings.IS_CURVE_SURVEY, String.valueOf(isCurveSurvey));
	}

	/**
	 * @return true for extended curve delta measuring display
	 */
	public boolean isCurveSurvey() {
		return Boolean.valueOf(this.getProperty(Settings.IS_CURVE_SURVEY, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the radius in km for GPS coordinates assignment to the same cluster (default 0.5 km)
	 */
	public double getGpsLocationRadius() {
		return Double.valueOf(this.getProperty(Settings.GPS_LOCATION_RADIUS, ".5")); //$NON-NLS-1$
	}

	/**
	 * @param doubleValue the radius in km for GPS coordinates assignment to the same cluster  (default 0.5 km)
	 */
	public void setGpsLocationRadius(double doubleValue) {
		this.setProperty(Settings.GPS_LOCATION_RADIUS, String.valueOf(doubleValue));
	}

	/**
	 * @return the GPS address type (default is the 2nd entry)
	 */
	public GeoCodeGoogle getGpsAddressType() {
		return GeoCodeGoogle.valueOf(this.getProperty(Settings.GPS_ADDRESS_TYPE, String.valueOf(GeoCodeGoogle.STREET_ADDRESS)));
	}

	/**
	 * @param geoCodeGoogleText
	 */
	public void setGpsAddressType(String geoCodeGoogleText) {
		try {
			final GeoCodeGoogle ordinal = GeoCodeGoogle.valueOf(geoCodeGoogleText);
			this.setProperty(Settings.GPS_ADDRESS_TYPE, ordinal.name());
		}
		catch (Exception e) {
			this.setProperty(Settings.GPS_ADDRESS_TYPE, GeoCodeGoogle.VALUES[1].name());
		}
	}

	/**
	 * @return the maximum number of subdirectories which is used for history log selection (default is 0)
	 */
	public int getSubDirectoryLevelMax() {
		return Integer.valueOf(this.getProperty(Settings.SUBDIRECTORY_LEVEL_MAX, String.valueOf(0)));
	}

	/**
	 * @param uintValue the maximum number of subdirectories which is used for history log selection
	 */
	public void setSubDirectoryLevelMax(String uintValue) {
		try {
			int value = Integer.parseUnsignedInt(uintValue.trim());
			if (value < 0 || value > 9) value = 0;
			this.setProperty(Settings.SUBDIRECTORY_LEVEL_MAX, String.valueOf(value));
		}
		catch (Exception e) {
		}
	}

	/**
	 * @param isCurveSurvey true extended curve delta measuring display
	 */
	public void setDataTableTransitions(boolean enabled) {
		this.setProperty(Settings.IS_DATA_TABLE_TRANSITIONS, String.valueOf(enabled));
	}

	/**
	 * @return true for extended curve delta measuring display
	 */
	public boolean isDataTableTransitions() {
		return Boolean.valueOf(this.getProperty(Settings.IS_DATA_TABLE_TRANSITIONS, "false")); //$NON-NLS-1$
	}

}
