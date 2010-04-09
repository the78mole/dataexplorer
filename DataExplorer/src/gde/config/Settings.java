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
****************************************************************************************/
package gde.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
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

import gde.DE;
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

/**
 * Settings class will read and write/update application settings, like window size and default path ....
 * @author Winfried Brügmann
 */
public class Settings extends Properties {
	private final static long		serialVersionUID						= 26031957;
	final static Logger					log													= Logger.getLogger(Settings.class.getName());
	final static String $CLASS_NAME = Settings.class.getName();

	// JAXB XML environment
	private Schema											schema;
	private JAXBContext									jc;
	private Unmarshaller								unmarshaller;
	private Marshaller									marshaller;
	private String											xmlBasePath;
	private Thread 											xsdThread;

	public  static final String	EMPTY												= "---"; //$NON-NLS-1$
	public  static final String EMPTY_SIGNATURE 						= EMPTY + GDE.STRING_SEMICOLON + EMPTY + GDE.STRING_SEMICOLON + EMPTY;
	private static final String	UNIX_PORT_DEV_TTY						= "/dev/tty";
	private static final String	WINDOWS_PORT_COM						= "COM";
	private static final String	PERMISSION_555							= "555";
	private static final String	PATH_RESOURCE								= "resource/";
	private static final String PATH_RESOURCE_TEMPLATE 			= "resource/template/";

	private final static String		HEADER_TEXT										= "# -- DataExplorer Settings File -- "; //$NON-NLS-1$
	private final static String		DEVICE_BLOCK									= "#[Actual-Device-Port-Settings]";																				// Picolario;Renschler;COM2 //$NON-NLS-1$
	private final static String		WINDOW_BLOCK									= "#[Window-Settings]"; //$NON-NLS-1$
	private final static String		WINDOW_LEFT										= "window_left"; //$NON-NLS-1$
	private final static String		WINDOW_TOP										= "window_top"; //$NON-NLS-1$
	private final static String		WINDOW_WIDTH									= "window_width"; //$NON-NLS-1$
	private final static String		WINDOW_HEIGHT									= "window_height"; //$NON-NLS-1$
	private final static String		COOLBAR_ORDER									= "coolbar_order"; //$NON-NLS-1$
	private final static String		COOLBAR_WRAPS									= "coolbar_wraps"; //$NON-NLS-1$
	private final static String		COOLBAR_SIZES									= "coolbar_sizes"; //$NON-NLS-1$
	private final static String		RECORD_COMMENT_VISIBLE				= "record_comment_visible"; //$NON-NLS-1$
	private final static String		GRAPHICS_HEADER_VISIBLE				= "graphics_header_visible"; //$NON-NLS-1$
	private final static String		GRAPHICS_AREA_BACKGROUND			= "graphics_area_background"; //$NON-NLS-1$
	private final static String		GRAPHICS_SURROUND_BACKGRD			= "graphics_surround_backgrd"; //$NON-NLS-1$
	private final static String		GRAPHICS_BORDER_COLOR					= "graphics_border_color"; //$NON-NLS-1$
	private final static String		COMPARE_AREA_BACKGROUND				= "compare_area_background"; //$NON-NLS-1$
	private final static String		COMPARE_SURROUND_BACKGRD			= "compare_surround_backgrd"; //$NON-NLS-1$
	private final static String		COMPARE_BORDER_COLOR					= "compare_border_color"; //$NON-NLS-1$
	private final static String		STATISTICS_INNER_BACKGROUND		= "statistics_inner_background"; //$NON-NLS-1$
	private final static String		STATISTICS_SURROUND_BACKGRD		= "statistics_surround_backgrd"; //$NON-NLS-1$
	private final static String		ANALOG_INNER_BACKGROUND				= "analog_inner_background"; //$NON-NLS-1$
	private final static String		ANALOG_SURROUND_BACKGRD				= "analog_surround_backgrd"; //$NON-NLS-1$
	private final static String		DIGITAL_INNER_BACKGROUND			= "digital_inner_background"; //$NON-NLS-1$
	private final static String		DIGITAL_SURROUND_BACKGRD			= "digital_surround_backgrd"; //$NON-NLS-1$
	private final static String		CELL_VOLTAGE_INNER_BACKGROUND	= "cell_voltage_inner_background"; //$NON-NLS-1$
	private final static String		CELL_VOLTAGE_SURROUND_BACKGRD	= "cell_voltage_surround_backgrd"; //$NON-NLS-1$
	private final static String		FILE_COMMENT_INNER_BACKGROUND	= "file_comment_inner_background"; //$NON-NLS-1$
	private final static String		FILE_COMMENT_SURROUND_BACKGRD	= "file_comment_surround_backgrd"; //$NON-NLS-1$
	private final static String		OBJECT_DESC_INNER_BACKGROUND	= "object_desciption_inner_background"; //$NON-NLS-1$
	private final static String		OBJECT_DESC_SURROUND_BACKGRD	= "object_desciption_surround_backgrd"; //$NON-NLS-1$

	private final static String		FILE_HISTORY_BLOCK						= "#[File-History-List]"; //$NON-NLS-1$
	private final static String		FILE_HISTORY_BEGIN						= "history_file_"; //$NON-NLS-1$
	private List<String>					fileHistory										= new ArrayList<String>();

	private final static String		APPL_BLOCK										= "#[Program-Settings]"; //$NON-NLS-1$
	private final static String		TABLE_BLOCK										= "#[Table-Settings]"; //$NON-NLS-1$
	private final static String		LOGGING_BLOCK									= "#[Logging-Settings]"; //$NON-NLS-1$
	private final static String		LOG_PATH											= "Logs"; //$NON-NLS-1$
	private final static String		LOG_FILE											= "trace.log"; //$NON-NLS-1$
	private final static String		SERIAL_LOG_FILE								= "serial.log"; //$NON-NLS-1$
	public final static String[]	LOGGING_LEVEL									= new String[] { "SEVERE", "WARNING", "TIME", "INFO", "FINE", "FINER", "FINEST" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	public final static String		ACTIVE_DEVICE									= "active_device"; //$NON-NLS-1$
	public final static String		OBJECT_LIST										= "object_list"; //$NON-NLS-1$
	public final static String		ACTIVE_OBJECT									= "active_object"; //$NON-NLS-1$
	public final static String		DATA_FILE_PATH								= "data_file_path"; //$NON-NLS-1$
	public final static String		LIST_SEPARATOR								= "list_separator"; //$NON-NLS-1$
	public final static String		DECIMAL_SEPARATOR							= "decimal_separator"; //$NON-NLS-1$
	public final static String		USE_DATA_FILE_NAME_LEADER			= "use_date_file_name_leader"; //$NON-NLS-1$
	public final static String		USE_OBJECT_KEY_IN_FILE_NAME		= "use_object_key_in_file_name"; //$NON-NLS-1$
	public static final String		ALPHA_BLENDING_VALUE					= "alpha_blending_value"; //$NON-NLS-1$
	public static final String		APLHA_BLENDING_ENABLED				= "aplha_blending_enabled"; //$NON-NLS-1$
	public final static String		IS_GLOBAL_PORT								= "is_global_port"; //$NON-NLS-1$
	public final static String		GLOBAL_PORT_NAME							= "global_port_name"; //$NON-NLS-1$
	public final static String		DO_PORT_AVAILABLE_TEST				= "do_port_available_test"; //$NON-NLS-1$
	public final static String		IS_PORT_BLACKLIST							= "is_port_black_list"; //$NON-NLS-1$
	public final static String		PORT_BLACKLIST								= "port_black_list"; //$NON-NLS-1$
	public final static String		IS_PORT_WHITELIST							= "is_port_white_list"; //$NON-NLS-1$
	public final static String		PORT_WHITELIST								= "port_white_list"; //$NON-NLS-1$
	public final static String		DEVICE_DIALOG_USE_MODAL				= "device_dialogs_modal"; //$NON-NLS-1$
	public static final String		DEVICE_DIALOG_ON_TOP					= "device_dialogs_on_top"; //$NON-NLS-1$
	public final static String		IS_GLOBAL_LOG_LEVEL						= "is_global_log_level"; //$NON-NLS-1$
	public final static String		GLOBAL_LOG_LEVEL							= "global_log_level"; //$NON-NLS-1$
	public final static String		UI_LOG_LEVEL									= "ui_log_leve"; //$NON-NLS-1$
	public final static String		DEVICE_LOG_LEVEL							= "device_log_level"; //$NON-NLS-1$
	public final static String		DATA_LOG_LEVEL								= "data_log_level"; //$NON-NLS-1$
	public final static String		CONFIG_LOG_LEVEL							= "config_log_level"; //$NON-NLS-1$
	public final static String		UTILS_LOG_LEVEL								= "utils_log_level"; //$NON-NLS-1$
	public final static String		FILE_IO_LOG_LEVEL							= "file_IO_log_level"; //$NON-NLS-1$
	public final static String		SERIAL_IO_LOG_LEVEL						= "serial_IO_log_level"; //$NON-NLS-1$
	public final static	Properties classbasedLogger							=	new Properties();

	public final static String		AUTO_OPEN_SERIAL_PORT					= "auto_open_port"; //$NON-NLS-1$
	public final static String		AUTO_OPEN_TOOL_BOX						= "auto_open_tool_box"; //$NON-NLS-1$
	public static final String		LOCALE_IN_USE									= "locale_in_use"; //$NON-NLS-1$
	public static final String		LOCALE_CHANGED								= "locale_changed"; //$NON-NLS-1$
	public static final String		IS_DESKTOP_SHORTCUT_CREATED		= "is_desktop_shotcut_created"; //$NON-NLS-1$
	public static final String		IS_APPL_REGISTERED						= "is_OSDE_registered"; //$NON-NLS-1$
	public static final String		IS_LOCK_UUCP_HINTED						= "is_lock_uucp_hinted"; //$NON-NLS-1$

	public final static String		GRID_DASH_STYLE								= "grid_dash_style"; //$NON-NLS-1$
	public final static String		GRID_COMPARE_WINDOW_HOR_TYPE	= "grid_compare_horizontal_type"; //$NON-NLS-1$
	public final static String		GRID_COMPARE_WINDOW_HOR_COLOR	= "grid_compare_horizontal_color"; //$NON-NLS-1$
	public final static String		GRID_COMPARE_WINDOW_VER_TYPE	= "grid_compare_vertical_type"; //$NON-NLS-1$
	public final static String		GRID_COMPARE_WINDOW_VER_COLOR	= "grid_compare_vertical_color"; //$NON-NLS-1$

	public final static String		DEVICE_PROPERTIES_DIR_NAME		= "Devices"; //$NON-NLS-1$
	public final static String		DEVICE_PROPERTIES_XSD_NAME		= "DeviceProperties" + GDE.DEVICE_PROPERTIES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD; //$NON-NLS-1$
	public final static String		GRAPHICS_TEMPLATES_DIR_NAME		= "GraphicsTemplates"; //$NON-NLS-1$
	public final static String		GRAPHICS_TEMPLATES_XSD_NAME		= "GraphicsTemplates" + GDE.GRAPHICS_TEMPLATES_XSD_VERSION + GDE.FILE_ENDING_DOT_XSD; //$NON-NLS-1$
	public final static String		GRAPHICS_TEMPLATES_EXTENSION	= GDE.FILE_ENDING_STAR_XML;

	private static Settings	instance											= null;	// singelton
	private BufferedReader	reader;																// to read the application settings
	private BufferedWriter	writer;																// to write the application settings

	private boolean 				isDevicePropertiesUpdated			= false;
	private boolean 				isDevicePropertiesReplaced		= false;
	private boolean 				isGraphicsTemplateUpdated			= false;


	private Rectangle				window;
	private String					cbOrder;
	private	String					cbWraps;
	private String					cbSizes;
	private String					settingsFilePath;											// full qualified path to settings file
	private String					applHomePath;													// default path to application home directory
	Comparator<String> 			comparator 										= new RecordSetNameComparator(); //used to sort object key list


	//	/**
	//	 * for unit test only
	//	 */
	//	public static void main(String[] args) {
	//		Settings settings = Settings.getInstance();
	//		settings.write();
	//		settings.load();
	//		System.out.println(settings.toString());
	//		settings.write();
	//
	//	}

	/**
	 * a singleton needs a static method to get the instance of this calss
	 * @return DataExplorer instance
	 * @throws JAXBException
	 * @throws SAXException
	 */
	public static Settings getInstance() {
		final String $METHOD_NAME = "getInstance"; //$NON-NLS-1$
		if (Settings.instance == null) {
			try {
				Settings.instance = new Settings();
				log.logp(Level.TIME, Settings.$CLASS_NAME, $METHOD_NAME, "init time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$
			}
			catch (Exception e) {
				log.logp(Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
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

		if (GDE.IS_WINDOWS) { //$NON-NLS-1$
			this.applHomePath = (System.getenv("APPDATA") + GDE.FILE_SEPARATOR_UNIX + "DataExplorer").replace("\\", GDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			this.settingsFilePath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + "DataExplorer.properties"; //$NON-NLS-1$
		}
		else if (GDE.IS_LINUX) { //$NON-NLS-1$ //$NON-NLS-2$
			this.applHomePath = System.getProperty("user.home") + GDE.FILE_SEPARATOR_UNIX + ".DataExplorer"; //$NON-NLS-1$ //$NON-NLS-2$
			this.settingsFilePath = this.applHomePath  + GDE.FILE_SEPARATOR_UNIX + "DataExplorer.properties"; //$NON-NLS-1$
		}
		// OPET - start - add
		else if (GDE.IS_MAC) { //$NON-NLS-1$ //$NON-NLS-2$
			this.applHomePath = System.getProperty("user.home") + GDE.FILE_SEPARATOR_UNIX + "Library" + GDE.FILE_SEPARATOR_UNIX + "Application Support" + GDE.FILE_SEPARATOR_UNIX + "DataExplorer"; //$NON-NLS-1$ //$NON-NLS-2$
			this.settingsFilePath = this.applHomePath  + GDE.FILE_SEPARATOR_UNIX + "DataExplorer.properties"; //$NON-NLS-1$
		}
		// OPET - end
		else {
			log.logp(Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, Messages.getString(MessageIds.GDE_MSGW0001));
		}

		this.xmlBasePath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + Settings.DEVICE_PROPERTIES_DIR_NAME + GDE.FILE_SEPARATOR_UNIX;
		xsdThread = new Thread() {
			public void run() {
				final String $METHOD_NAME = "xsdThread.run()";
				// device properties context
				try {
					Settings.this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(new File(Settings.this.xmlBasePath + Settings.DEVICE_PROPERTIES_XSD_NAME));
					Settings.this.jc = JAXBContext.newInstance("de.device"); //$NON-NLS-1$
					Settings.this.unmarshaller = Settings.this.jc.createUnmarshaller();
					Settings.this.unmarshaller.setSchema(Settings.this.schema);
					Settings.this.marshaller = Settings.this.jc.createMarshaller();
					Settings.this.marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.valueOf(true));
					Settings.this.marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, Settings.DEVICE_PROPERTIES_XSD_NAME);
					log.logp(Level.TIME, Settings.$CLASS_NAME, $METHOD_NAME, "schema factory setup time = " + StringHelper.getFormatedTime("ss:SSS", (new Date().getTime() - GDE.StartTime))); //$NON-NLS-1$ //$NON-NLS-2$		
				}
				catch (Exception e) {
					log.logp(Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
				}
			}
		};
		
		this.load();

		// check existens of application home directory, check XSD version, copy all device XML+XSD and image files
		FileUtils.checkDirectoryAndCreate(this.applHomePath);
		String devicePropertiesTargetpath = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + DEVICE_PROPERTIES_DIR_NAME;
		if (!FileUtils.checkDirectoryAndCreate(devicePropertiesTargetpath, DEVICE_PROPERTIES_XSD_NAME)) {
			FileUtils.extract(this.getClass(), DEVICE_PROPERTIES_XSD_NAME, PATH_RESOURCE, devicePropertiesTargetpath, PERMISSION_555);
			updateDeviceProperties(devicePropertiesTargetpath + GDE.FILE_SEPARATOR_UNIX, true);
			this.isDevicePropertiesUpdated = true;
		}
		else {	// execute every time application starts to enable update from added plug-in
			updateDeviceProperties(devicePropertiesTargetpath + GDE.FILE_SEPARATOR_UNIX, true);
		}
		xsdThread.start(); // wait to start the thread until the device XMLs are getting updated

		// locale settings has been changed, replacement of device property files required
		if (this.getLocaleChanged() && !this.isDevicePropertiesUpdated) {
			updateDeviceProperties(devicePropertiesTargetpath + GDE.FILE_SEPARATOR_UNIX, false);
			this.isDevicePropertiesReplaced = true;
		}

		String templateDirectory = this.applHomePath + GDE.FILE_SEPARATOR_UNIX + GRAPHICS_TEMPLATES_DIR_NAME;
		if (!FileUtils.checkDirectoryAndCreate(templateDirectory, GRAPHICS_TEMPLATES_XSD_NAME)) { // there is no old XSD version
			FileUtils.extract(this.getClass(), GRAPHICS_TEMPLATES_XSD_NAME, PATH_RESOURCE, templateDirectory, PERMISSION_555);
			this.isGraphicsTemplateUpdated = true;
		}
		checkDeviceTemplates(templateDirectory + GDE.FILE_SEPARATOR_UNIX);

		FileUtils.checkDirectoryAndCreate(this.applHomePath + GDE.FILE_SEPARATOR_UNIX + "Logs"); //$NON-NLS-1$

		log.logp(Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, String.format("settingsFilePath = %s", this.settingsFilePath)); //$NON-NLS-1$

		if (this.getProperty(WINDOW_LEFT) != null && this.getProperty(WINDOW_TOP) != null
				&& this.getProperty(WINDOW_WIDTH) != null && this.getProperty(WINDOW_HEIGHT) != null) {
			this.window = new Rectangle(new Integer(this.getProperty(WINDOW_LEFT).trim()).intValue(), new Integer(this.getProperty(WINDOW_TOP).trim()).intValue(), new Integer(this.getProperty(WINDOW_WIDTH).trim()).intValue(),
					new Integer(this.getProperty(WINDOW_HEIGHT).trim()).intValue());
		}
		else
			this.window = new Rectangle(50, 50, 900, 600);

		this.setProperty(LOCALE_CHANGED, "false"); //$NON-NLS-1$
	}

	/**
	 * check existens of directory, create if required and update all
	 * @param devicePropertiesTargetpath
	 */
	private void updateDeviceProperties(String devicePropertiesTargetpath, boolean existCheck) {
		final String $METHOD_NAME = "updateDeviceProperties"; //$NON-NLS-1$

		String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
		log.logp(Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, "deviceJarBasePath = " + deviceJarBasePath); //$NON-NLS-1$
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
				log.logp(Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}
			if (jarFile != null) {
				for (String plugin : plugins) {
					if (existCheck) {
						if (!FileUtils.checkFileExist(devicePropertiesTargetpath + plugin + GDE.FILE_ENDING_DOT_XML))
							FileUtils.extract(jarFile, plugin + ".xml", PATH_RESOURCE + this.getLocale().getLanguage() + GDE.FILE_SEPARATOR_UNIX, devicePropertiesTargetpath, PERMISSION_555); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					else {
						FileUtils.extract(jarFile, plugin + ".xml", PATH_RESOURCE + this.getLocale().getLanguage() + GDE.FILE_SEPARATOR_UNIX, devicePropertiesTargetpath, PERMISSION_555); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
				}
			}
		}
	}

	/**
	 * check existens of device graphics default templates, extract if required
	 * @param templateDirectoryTargetPath
	 */
	private void checkDeviceTemplates(String templateDirectoryTargetPath) {
		final String $METHOD_NAME 					= "checkDeviceTemplates"; //$NON-NLS-1$

		String deviceJarBasePath = FileUtils.getDevicePluginJarBasePath();
		log.logp(Level.CONFIG, Settings.$CLASS_NAME, $METHOD_NAME, "deviceJarBasePath = " + deviceJarBasePath); //$NON-NLS-1$
		String[] files = new File(deviceJarBasePath).list();
		for (String jarFileName : files) {
			if (!jarFileName.endsWith(GDE.FILE_ENDING_DOT_JAR)) continue;
			JarFile jarFile = null;
			try {
				jarFile = new JarFile(deviceJarBasePath + GDE.FILE_SEPARATOR_UNIX + jarFileName);
			}
			catch (IOException e) {
				log.logp(Level.SEVERE, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			}

			if (jarFile != null) {
				log.logp(Level.FINER, Settings.$CLASS_NAME, $METHOD_NAME, "templateDirectoryTargetPath=" + templateDirectoryTargetPath); //$NON-NLS-1$
				Enumeration<JarEntry> e=jarFile.entries();
        while (e.hasMoreElements()) {
            String entryName = e.nextElement().getName();
            if (entryName.startsWith(PATH_RESOURCE_TEMPLATE) && entryName.endsWith(GDE.FILE_ENDING_DOT_XML)) {
            	String defaultTemplateName = entryName.substring(PATH_RESOURCE_TEMPLATE.length());
  						if (!FileUtils.checkFileExist(templateDirectoryTargetPath + defaultTemplateName))		{
    						log.logp(Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "jarFile=" + jarFile.getName() + "; defaultTemplateName=" + entryName); //$NON-NLS-1$ //$NON-NLS-2$
  							FileUtils.extract(jarFile, defaultTemplateName, PATH_RESOURCE_TEMPLATE, templateDirectoryTargetPath, PERMISSION_555); //$NON-NLS-1$ //$NON-NLS-2$
  						}
            }
        }
			}
		}
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
			for (int i = 0; i < 10; i++) {
				String entry = this.getProperty(FILE_HISTORY_BEGIN + i);
				if (entry != null && entry.length() > 4) {
					if (!this.fileHistory.contains(entry))
						this.fileHistory.add(entry);
				}
				else
					break;
			}
		}
		catch (Exception e) {
			log.logp(Level.WARNING, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}

	}

	/**
	 * write the application settings file
	 */
	public void store() {
		final String $METHOS_NAME = "store()"; //$NON-NLS-1$
		try {
			if (!new File(this.settingsFilePath).exists()) {
				new File(this.settingsFilePath).createNewFile();
			}
			this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.settingsFilePath), "UTF-8")); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", HEADER_TEXT)); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", DEVICE_BLOCK)); // [Gerät] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", ACTIVE_DEVICE, this.getActiveDevice())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", OBJECT_LIST, this.getObjectListAsString())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", ACTIVE_OBJECT, this.getActiveObject())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", WINDOW_BLOCK)); // [Fenster Einstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", WINDOW_LEFT, this.window.x)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", WINDOW_TOP, this.window.y)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", WINDOW_WIDTH, this.window.width)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", WINDOW_HEIGHT, this.window.height)); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", COOLBAR_ORDER, this.cbOrder)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", COOLBAR_WRAPS, this.cbWraps)); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", COOLBAR_SIZES, this.cbSizes)); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", RECORD_COMMENT_VISIBLE, isRecordCommentVisible())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRAPHICS_HEADER_VISIBLE, isGraphicsHeaderVisible())); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", GRID_DASH_STYLE, getGridDashStyleAsString())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRID_COMPARE_WINDOW_HOR_TYPE, getGridCompareWindowHorizontalType())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRID_COMPARE_WINDOW_HOR_COLOR, getGridCompareWindowHorizontalColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRID_COMPARE_WINDOW_VER_TYPE, getGridCompareWindowVerticalType())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRID_COMPARE_WINDOW_VER_COLOR, getGridCompareWindowVerticalColorStr())); //$NON-NLS-1$

			this.writer.write(String.format("%-40s \t=\t %s\n", GRAPHICS_AREA_BACKGROUND, getGraphicsCurveAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRAPHICS_SURROUND_BACKGRD, getGraphicsSurroundingBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GRAPHICS_BORDER_COLOR, getGraphicsCurvesBorderColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", COMPARE_AREA_BACKGROUND, getCompareCurveAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", COMPARE_SURROUND_BACKGRD, getCompareSurroundingBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", COMPARE_BORDER_COLOR, getCurveCompareBorderColorStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", STATISTICS_INNER_BACKGROUND, getStatisticsInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", STATISTICS_SURROUND_BACKGRD, getStatisticsSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", ANALOG_INNER_BACKGROUND, getAnalogInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", ANALOG_SURROUND_BACKGRD, getAnalogSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DIGITAL_INNER_BACKGROUND, getDigitalInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DIGITAL_SURROUND_BACKGRD, getDigitalSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", CELL_VOLTAGE_INNER_BACKGROUND, getCellVoltageInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", CELL_VOLTAGE_SURROUND_BACKGRD, getCellVoltageSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", FILE_COMMENT_INNER_BACKGROUND, getFileCommentInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", FILE_COMMENT_SURROUND_BACKGRD, getFileCommentSurroundingAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", OBJECT_DESC_INNER_BACKGROUND, getObjectDescriptionInnerAreaBackgroundStr())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", OBJECT_DESC_SURROUND_BACKGRD, getObjectDescriptionSurroundingAreaBackgroundStr())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", FILE_HISTORY_BLOCK)); // [Datei History Liste] //$NON-NLS-1$
			for (int i = 0; i < 10 && i < this.fileHistory.size(); i++) {
				if( this.fileHistory.get(i) == null) break;
				this.writer.write(String.format("%-40s \t=\t %s\n", FILE_HISTORY_BEGIN + i, this.fileHistory.get(i))); //$NON-NLS-1$
			}

			this.writer.write(String.format("%s\n", APPL_BLOCK)); // [Programmeinstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DATA_FILE_PATH, getDataFilePath().replace("\\", "\\\\"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			this.writer.write(String.format("%-40s \t=\t %s\n", USE_DATA_FILE_NAME_LEADER, getUsageDateAsFileNameLeader())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", USE_OBJECT_KEY_IN_FILE_NAME, getUsageObjectKeyInFileName())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", ALPHA_BLENDING_VALUE, getDialogAlphaValue())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", APLHA_BLENDING_ENABLED, isDeviceDialogAlphaEnabled())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_GLOBAL_PORT, isGlobalSerialPort())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GLOBAL_PORT_NAME, getSerialPort())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DO_PORT_AVAILABLE_TEST, doPortAvailabilityCheck())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_PORT_BLACKLIST, isSerialPortBlackListEnabled())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", PORT_BLACKLIST, getSerialPortBlackList())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_PORT_WHITELIST, isSerialPortWhiteListEnabled())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", PORT_WHITELIST, getSerialPortWhiteListString())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DEVICE_DIALOG_USE_MODAL, isDeviceDialogsModal())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DEVICE_DIALOG_ON_TOP, isDeviceDialogsOnTop())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", AUTO_OPEN_SERIAL_PORT, isAutoOpenSerialPort())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", AUTO_OPEN_TOOL_BOX, isAutoOpenToolBox())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", LOCALE_IN_USE, getLocale().getLanguage())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", LOCALE_CHANGED, getLocaleChanged())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_DESKTOP_SHORTCUT_CREATED, this.isDesktopShortcutCreated())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_APPL_REGISTERED, this.isApplicationRegistered())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_LOCK_UUCP_HINTED, this.isLockUucpHinted())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", TABLE_BLOCK)); // [Tabellen Einstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", LIST_SEPARATOR, getListSeparator())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DECIMAL_SEPARATOR, getDecimalSeparator())); //$NON-NLS-1$

			this.writer.write(String.format("%s\n", LOGGING_BLOCK)); // [Logging Einstellungen] //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", IS_GLOBAL_LOG_LEVEL, isGlobalLogLevel())); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", GLOBAL_LOG_LEVEL, getLogLevel(GLOBAL_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", UI_LOG_LEVEL, getLogLevel(UI_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DEVICE_LOG_LEVEL, getLogLevel(DEVICE_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", DATA_LOG_LEVEL, getLogLevel(DATA_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", CONFIG_LOG_LEVEL, getLogLevel(CONFIG_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", UTILS_LOG_LEVEL, getLogLevel(UTILS_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", FILE_IO_LOG_LEVEL, getLogLevel(FILE_IO_LOG_LEVEL))); //$NON-NLS-1$
			this.writer.write(String.format("%-40s \t=\t %s\n", SERIAL_IO_LOG_LEVEL, getLogLevel(SERIAL_IO_LOG_LEVEL))); //$NON-NLS-1$

			this.writer.flush();
			this.writer.close();
		}
		catch (IOException e) {
			log.logp(Level.SEVERE, Settings.$CLASS_NAME, $METHOS_NAME, e.getMessage(), e);
		}

	}

	/*
	 * overload Properties method due to loading properties from file returns "null" instead of null
	 * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
	 */
  public String getProperty(String key, String defaultValue) {
  	String val = getProperty(key);
		if (val == null || val.equals(GDE.STRING_EMPTY) || val.equals("null")) val = defaultValue; //$NON-NLS-2$
  	return val;
  }

	public Rectangle getWindow() {
		return this.window;
	}

	public void setWindow(Point location, Point size) {
		this.window = new Rectangle(location.x, location.y, size.x, size.y);
	}

	public void setCoolBarStates(int[] order, int[] wraps, Point[] sizes) {
		this.cbOrder = StringHelper.intArrayToString(order);
		this.cbWraps = StringHelper.intArrayToString(wraps);
		this.cbSizes = StringHelper.pointArrayToString(sizes);
	}

	public int[] getCoolBarOrder() {
		int[] intOrder = StringHelper.stringToIntArray(this.getProperty(COOLBAR_ORDER, "0;1;2;3;4").trim()); //$NON-NLS-1$
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
		return StringHelper.stringToIntArray(this.getProperty(COOLBAR_WRAPS, "0;3").trim()); //$NON-NLS-1$
	}

	public Point[] getCoolBarSizes() {
		return StringHelper.stringToPointArray(DataExplorer.getInstance().getMenuToolBar().getCoolBarSizes());
	}

  public List<String> getFileHistory() {
		return this.fileHistory;
	}

	public String getActiveDevice() {
		return this.getProperty(ACTIVE_DEVICE, EMPTY_SIGNATURE).split(GDE.STRING_SEMICOLON)[0].trim();
	}

	public void setActiveDevice(String activeDeviceString) {
		this.setProperty(ACTIVE_DEVICE, activeDeviceString.trim());
	}

	public String getObjectListAsString() {
		return this.getProperty(OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200));
	}

	public String[] getObjectList() {
		String[] objectKeys = this.getProperty(OBJECT_LIST, Messages.getString(MessageIds.GDE_MSGT0200)).split(GDE.STRING_SEMICOLON);
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
			log.log(Level.WARNING, e.getMessage(), e);
		}
		setObjectList(activeObjectList, activeObjectKey);
	}

	public void setObjectList(String[] activeObjectList, String newObjectKey) {
		// keep object oriented out of the sorting game
		boolean startsWithDeviceOriented = activeObjectList[0].startsWith(Messages.getString(MessageIds.GDE_MSGT0200).substring(0, 10));
		if (startsWithDeviceOriented) {
			String[] tmpObjectKeys = new String[activeObjectList.length - 1];
			System.arraycopy(activeObjectList, 1, tmpObjectKeys, 0, activeObjectList.length - 1);
			Arrays.sort(tmpObjectKeys, this.comparator);
			System.arraycopy(tmpObjectKeys, 0, activeObjectList, 1, activeObjectList.length - 1);
		}
		else {
			Arrays.sort(activeObjectList, this.comparator);
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
		for (int i = 0; i < activeObjectList.length; ++i) {
			sb.append(activeObjectList[i]).append(GDE.STRING_SEMICOLON);
		}
		this.setProperty(OBJECT_LIST, sb.toString());
		this.setProperty(ACTIVE_OBJECT, newObjectKey);
	}

	public int getActiveObjectIndex() {
		Vector<String> tmpObjectVector = new Vector<String>();
		for (String objectKey : this.getObjectList()) {
			if (objectKey.length() > 1) tmpObjectVector.add(objectKey);
		}
		int index = tmpObjectVector.indexOf(this.getProperty(ACTIVE_OBJECT, Messages.getString(MessageIds.GDE_MSGT0200).split(GDE.STRING_SEMICOLON)[0]).trim());
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
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + DEVICE_PROPERTIES_DIR_NAME;
	}

	/**
	 * @return the graphicsTemplatePath
	 */
	public String getGraphicsTemplatePath() {
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + GRAPHICS_TEMPLATES_DIR_NAME;
	}

	/**
	 * @return the log file path
	 */
	public String getLogFilePath() {
		final String $METHOD_NAME = "getLogFilePath"; //$NON-NLS-1$
		log.logp(Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "applHomePath = " + this.applHomePath); //$NON-NLS-1$
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + LOG_PATH + GDE.FILE_SEPARATOR_UNIX + LOG_FILE;
	}

	/**
	 * @return the log file path for the serial trace logs
	 */
	public String getSerialLogFilePath() {
		return this.applHomePath.trim() + GDE.FILE_SEPARATOR_UNIX + LOG_PATH + GDE.FILE_SEPARATOR_UNIX + SERIAL_LOG_FILE;
	}

	/**
	 * @return the default dataFilePath
	 */
	public String getDataFilePath() {
		final String $METHOD_NAME = "getDataFilePath"; //$NON-NLS-1$
		String dataPath = this.getProperty(DATA_FILE_PATH, GDE.FILE_SEPARATOR_UNIX).replace("\\\\", GDE.FILE_SEPARATOR_UNIX).replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX); //$NON-NLS-1$
		log.logp(Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "dataFilePath = " + dataPath); //$NON-NLS-1$
		return dataPath.trim();
	}

	/**
	 * set the default dataFilePath
	 */
	public void setDataFilePath(String newDataFilePath) {
		final String $METHOD_NAME = "setDataFilePath"; //$NON-NLS-1$
		String filePath = newDataFilePath.replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX).trim();
		log.logp(Level.FINE, Settings.$CLASS_NAME, $METHOD_NAME, "newDataFilePath = " + filePath); //$NON-NLS-1$
		this.setProperty(DATA_FILE_PATH, filePath);
	}

	/**
	 * @return the list separator
	 */
	public char getListSeparator() {
		if (this.getProperty(LIST_SEPARATOR) == null) this.setProperty(LIST_SEPARATOR, GDE.STRING_SEMICOLON);
		return this.getProperty(LIST_SEPARATOR).trim().charAt(0);
	}

	/**
	 * set the list separator
	 */
	public void setListSeparator(String newListSeparator) {
		this.setProperty(LIST_SEPARATOR, newListSeparator.trim());
	}

	/**
	 * @return the decimal separator, default value is '.'
	 */
	public char getDecimalSeparator() {
		if (this.getProperty(DECIMAL_SEPARATOR) == null) this.setProperty(DECIMAL_SEPARATOR, GDE.STRING_DOT);
		return this.getProperty(DECIMAL_SEPARATOR).trim().charAt(0);
	}

	/**
	 * set the decimal separator
	 */
	public void setDecimalSeparator(String newDecimalSeparator) {
		this.setProperty(DECIMAL_SEPARATOR, newDecimalSeparator.trim());
	}

	/**
	 * set the usage of suggest date as leader of the to be saved filename
	 */
	public void setUsageDateAsFileNameLeader(boolean usage) {
		this.setProperty(USE_DATA_FILE_NAME_LEADER, GDE.STRING_EMPTY+usage);
	}

	/**
	 * set usage of the object key within the file name
	 */
	public void setUsageObjectKeyInFileName(boolean usage) {
		this.setProperty(USE_OBJECT_KEY_IN_FILE_NAME, GDE.STRING_EMPTY+usage);
	}

	/**
	 * get the usage of suggest date as leader of the to be saved filename
	 */
	public boolean getUsageDateAsFileNameLeader() {
		return Boolean.valueOf(this.getProperty(USE_DATA_FILE_NAME_LEADER, "true")); //$NON-NLS-1$
	}

	/**
	 * get usage of the object key within the file name
	 */
	public boolean getUsageObjectKeyInFileName() {
		return Boolean.valueOf(this.getProperty(USE_OBJECT_KEY_IN_FILE_NAME, "false")); //$NON-NLS-1$
	}

	/**
	 * @return the global serial port
	 */
	public boolean isGlobalSerialPort() {
		return Boolean.valueOf(this.getProperty(IS_GLOBAL_PORT, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global serial port
	 */
	public void setIsGlobalSerialPort(String isGlobalSerialPort) {
		this.setProperty(IS_GLOBAL_PORT, isGlobalSerialPort.trim());
	}

	/**
	 * @return boolean value of port black list enablement
	 */
	public boolean isSerialPortBlackListEnabled() {
		return Boolean.valueOf(this.getProperty(IS_PORT_BLACKLIST, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global serial port black list enabled
	 */
	public void setSerialPortBlackListEnabled(boolean enabled) {
		this.setProperty(IS_PORT_BLACKLIST, GDE.STRING_EMPTY+enabled);
	}

	/**
	 * @return port black list
	 */
	public String getSerialPortBlackList() {
		StringBuffer blackList = new StringBuffer();
		for (String port : this.getProperty(PORT_BLACKLIST, GDE.STRING_EMPTY).trim().split(GDE.STRING_BLANK)) {
			if(port != null && port.length() > 3) blackList.append(port).append(GDE.STRING_BLANK);
		}
		return blackList.toString().trim();
	}

	/**
	 * set the serial port black list
	 */
	public void setSerialPortBlackList(String newPortBlackList) {
		StringBuilder blackList = new StringBuilder();
		for (String tmpPort : newPortBlackList.split(GDE.STRING_BLANK)) {
			if (GDE.IS_WINDOWS && tmpPort.toUpperCase().startsWith(WINDOWS_PORT_COM))
				blackList.append(tmpPort.toUpperCase()).append(GDE.STRING_BLANK);
			else if (tmpPort.startsWith(UNIX_PORT_DEV_TTY))
				blackList.append(tmpPort).append(GDE.STRING_BLANK);
		}
		this.setProperty(PORT_BLACKLIST, blackList.toString());
	}


	/**
	 * @return boolean value of port white list enablement
	 */
	public boolean isSerialPortWhiteListEnabled() {
		return Boolean.valueOf(this.getProperty(IS_PORT_WHITELIST, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set the serial port white list enabled
	 */
	public void setSerialPortWhiteListEnabled(boolean enabled) {
		this.setProperty(IS_PORT_WHITELIST, GDE.STRING_EMPTY+enabled);
	}

	/**
	 * @return port white list as vector
	 */
	public Vector<String> getSerialPortWhiteList() {
		Vector<String> whiteList = new Vector<String>();
		for (String port : this.getProperty(PORT_WHITELIST, GDE.STRING_EMPTY).trim().split(";| ")) { //$NON-NLS-1$
			if(port != null && port.length() > 3) whiteList.add(port);
		}
		return whiteList;
	}

	/**
	 * @return port white list as String
	 */
	public String getSerialPortWhiteListString() {
		StringBuffer whiteList = new StringBuffer();
		for (String port : this.getProperty(PORT_WHITELIST, GDE.STRING_EMPTY).trim().split(";| ")) { //$NON-NLS-1$
			if(port != null && port.length() > 3) whiteList.append(port).append(GDE.STRING_BLANK);
		}
		return whiteList.toString().trim();
	}

	/**
	 * set the serial port white list
	 */
	public void setSerialPortWhiteList(String newPortWhiteList) {
		StringBuilder whiteList = new StringBuilder();
		for (String tmpPort : newPortWhiteList.split(GDE.STRING_BLANK)) {
			if (GDE.IS_WINDOWS && tmpPort.toUpperCase().startsWith(WINDOWS_PORT_COM))
				whiteList.append(tmpPort.toUpperCase()).append(GDE.STRING_SEMICOLON);
			else if (tmpPort.startsWith(UNIX_PORT_DEV_TTY))
					whiteList.append(tmpPort).append(GDE.STRING_BLANK);
		}
		this.setProperty(PORT_WHITELIST, whiteList.toString());
	}

	/**
	 * @return the global log level
	 */
	public boolean isGlobalLogLevel() {
		return Boolean.valueOf(this.getProperty(IS_GLOBAL_LOG_LEVEL, "true").trim()); //$NON-NLS-1$
	}

	/**
	 * set the global log level
	 */
	public void setIsGlobalLogLevel(String isGlobalLogLevel) {
		this.setProperty(IS_GLOBAL_LOG_LEVEL, isGlobalLogLevel.trim());
	}

	/**
	 * @return the serial port name as string
	 */
	public String getSerialPort() {
		String port = getProperty(GLOBAL_PORT_NAME, EMPTY).trim();
		return port == null ? EMPTY : port;
	}

	/**
	 * set property if during port scan a availability check should executed (disable for slow systems)
	 */
	public void setPortAvailabilityCheck(boolean enabled) {
		setProperty(DO_PORT_AVAILABLE_TEST, GDE.STRING_EMPTY+enabled);
	}


	/**
	 * get property if during port scan a availability check should executed (disable for slow systems)
	 */
	public boolean doPortAvailabilityCheck() {
		return Boolean.valueOf(getProperty(DO_PORT_AVAILABLE_TEST, "false")); //$NON-NLS-1$
	}

	/**
	 * set the decimal separator
	 */
	public void setSerialPort(String newSerialPort) {
		this.setProperty(GLOBAL_PORT_NAME, newSerialPort.trim());
	}

	/**
	 * check if minimal settings available
	 */
	public boolean isOK() {
		boolean ok = false;
		if (getProperty(DATA_FILE_PATH) != null || getProperty(LIST_SEPARATOR) != null || getProperty(DECIMAL_SEPARATOR) != null || getProperty(IS_GLOBAL_PORT) != null
				|| getProperty(GLOBAL_PORT_NAME) != null) {
			ok = true;
		}

		return ok;
	}

	/**
	 * query if serial port opened right after closing device selection dialog
	 */
	public boolean isAutoOpenSerialPort() {
		return Boolean.valueOf(this.getProperty(AUTO_OPEN_SERIAL_PORT, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * query if device tool box to be opened right after closing device selection dialog
	 */
	public boolean isAutoOpenToolBox() {
		return Boolean.valueOf(this.getProperty(AUTO_OPEN_TOOL_BOX, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * query if record set comment window is visible
	 */
	public boolean isRecordCommentVisible() {
		return Boolean.valueOf(this.getProperty(RECORD_COMMENT_VISIBLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set property if record set comment window is visible
	 */
	public void setRecordCommentVisible(boolean enabled) {
		this.setProperty(RECORD_COMMENT_VISIBLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query if record set comment window is visible
	 */
	public boolean isGraphicsHeaderVisible() {
		return Boolean.valueOf(this.getProperty(GRAPHICS_HEADER_VISIBLE, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set property if record set comment window is visible
	 */
	public void setGraphicsHeaderVisible(boolean enabled) {
		this.setProperty(GRAPHICS_HEADER_VISIBLE, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the grid line style
	 * @return actual grid line style as integer array
	 */
	public int[] getGridDashStyle() {
		String[] gridLineStyle = this.getProperty(GRID_DASH_STYLE, "10, 10").split(GDE.STRING_COMMA); //$NON-NLS-1$
		return new int[] {new Integer(gridLineStyle[0].trim()).intValue(), new Integer(gridLineStyle[1].trim()).intValue()};
	}

	/**
	 * @return actual grid line style as string integer array
	 */
	private String getGridDashStyleAsString() {
		return this.getProperty(GRID_DASH_STYLE, "10, 10").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid line style in pixel length
	 * @param newGridDashStyle {drawn, blank}
	 */
	public void setGridDaschStyle(int[] newGridDashStyle) {
		this.setProperty(GRID_DASH_STYLE, GDE.STRING_EMPTY + newGridDashStyle[0] + ", " + newGridDashStyle[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @return the grid horizontal type of the compare window (0=none;1=each,2=eachSecond)
	 */
	public int getGridCompareWindowHorizontalType() {
		return new Integer(this.getProperty(GRID_COMPARE_WINDOW_HOR_TYPE, "0").trim()).intValue(); //$NON-NLS-1$
	}

	/**
	 * set the grid horizontal type of the compare window
	 * @param newHorizontalGridType (0=none;1=each,2=eachSecond)
	 */
	public void setGridCompareWindowHorizontalType(int newHorizontalGridType) {
		this.setProperty(GRID_COMPARE_WINDOW_HOR_TYPE, GDE.STRING_EMPTY + newHorizontalGridType);
	}

	/**
	 * @return the grid horizontal color of the compare window (r,g,b)
	 */
	public Color getGridCompareWindowHorizontalColor() {
		return getColor(GRID_COMPARE_WINDOW_HOR_COLOR, "200,200,200"); //$NON-NLS-1$
	}

	/**
	 * @return the grid horizontal color of the compare window as string of (r,g,b)
	 */
	public String getGridCompareWindowHorizontalColorStr() {
		return this.getProperty(GRID_COMPARE_WINDOW_HOR_COLOR, "200,200,200").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid horizontal color of the compare window
	 * @param newColor (r,g,b)
	 */
	public void setGridCompareWindowHorizontalColor(Color newColor) {
		String rgb = newColor.getRGB().red + GDE.STRING_COMMA + newColor.getRGB().green + GDE.STRING_COMMA + newColor.getRGB().blue;
		this.setProperty(GRID_COMPARE_WINDOW_HOR_COLOR, rgb);
	}

	/**
	 * @return the grid vertical type of the compare window (0=none;1=each,2=mod60)
	 */
	public int getGridCompareWindowVerticalType() {
		return new Integer(this.getProperty(GRID_COMPARE_WINDOW_VER_TYPE, "0").trim()).intValue(); //$NON-NLS-1$
	}

	/**
	 * set the grid vertical type of the compare window
	 * @param newVerticalGridType (0=none;1=each,2=eachSecond)
	 */
	public void setGridCompareWindowVerticalType(int newVerticalGridType) {
		this.setProperty(GRID_COMPARE_WINDOW_VER_TYPE, GDE.STRING_EMPTY + newVerticalGridType); //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window (r,g,b)
	 */
	public Color getGridCompareWindowVerticalColor() {
		return getColor(GRID_COMPARE_WINDOW_VER_COLOR, "200,200,200"); //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGridCompareWindowVerticalColorStr() {
		return this.getProperty(GRID_COMPARE_WINDOW_VER_COLOR, "200,200,200").trim(); //$NON-NLS-1$
	}

	/**
	 * set the grid vertical color of the compare window
	 * @param newColor (r,g,b)
	 */
	public void setGridCompareWindowVerticalColor(Color newColor) {
		String rgb = newColor.getRGB().red + GDE.STRING_COMMA + newColor.getRGB().green + GDE.STRING_COMMA + newColor.getRGB().blue; //$NON-NLS-1$ //$NON-NLS-2$
		this.setProperty(GRID_COMPARE_WINDOW_VER_COLOR, rgb);
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
			java.util.logging.Level globalLogLevel = Level.parse(getProperty(Settings.GLOBAL_LOG_LEVEL, "WARNING").trim()); //$NON-NLS-1$
			setIndividualLogLevel("de.ui", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("de.data", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("de.config", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("de.device", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("de.utils", globalLogLevel); //$NON-NLS-1$
			setIndividualLogLevel("de.io", globalLogLevel); //$NON-NLS-1$
			setGlobalLogLevel(globalLogLevel); //$NON-NLS-1$
			setLevelSerialIO(globalLogLevel);
			Enumeration<Object> e = classbasedLogger.keys();
			while (e.hasMoreElements()) {
				String loggerName = (String) e.nextElement();
				setIndividualLogLevel(loggerName, Level.parse("SEVERE")); //$NON-NLS-1$
			}
			classbasedLogger.clear();
		}
		else {
			setGlobalLogLevel(Level.parse(getProperty(Settings.GLOBAL_LOG_LEVEL, "WARNING").trim())); //$NON-NLS-1$
			setIndividualLogLevel("de.ui", getLogLevel(Settings.UI_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("de.data", getLogLevel(Settings.DATA_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("de.config", getLogLevel(Settings.CONFIG_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("de.device", getLogLevel(Settings.DEVICE_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("de.utils", getLogLevel(Settings.UTILS_LOG_LEVEL)); //$NON-NLS-1$
			setIndividualLogLevel("de.io", getLogLevel(Settings.FILE_IO_LOG_LEVEL)); //$NON-NLS-1$
			setLevelSerialIO(getLogLevel(Settings.SERIAL_IO_LOG_LEVEL));
			Enumeration<Object> e = classbasedLogger.keys();
			while (e.hasMoreElements()) {
				String loggerName = (String) e.nextElement();
				setIndividualLogLevel(loggerName, Level.parse(classbasedLogger.getProperty(loggerName)));
			}
		}
	}

	/**
	 * @return log level from the given categorie, in case of parse error fall back to Level.WARNING
	 */
	java.util.logging.Level getLogLevel(String logCategorie) {
		java.util.logging.Level logLevel = Level.WARNING;
		try {
			logLevel = Level.parse(getProperty(logCategorie, "WARNING").trim()); //$NON-NLS-1$
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
			Logger logger = Logger.getLogger("de.serial.DeviceSerialPort"); //$NON-NLS-1$
			for(Handler handler : logger.getHandlers()) {
				logger.removeHandler(handler);
			}
			logger.setLevel(logLevel);
			if (logLevel.intValue() < Level.parse("WARNING").intValue()) { //$NON-NLS-1$
				logger.setUseParentHandlers(false);
				Handler fh = new FileHandler(this.getSerialLogFilePath(), 15000000, 3);
				fh.setFormatter(new LogFormatter());
				logger.addHandler(new MemoryHandler(fh, 5000000, logLevel));
			}
			else
				logger.setUseParentHandlers(true); // enable global log settings

		}
		catch (Exception e) {
			log.logp(Level.WARNING, Settings.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
		}
	}

	/**
	 * set device dialog behavior, application modal or application equivalent
	 * @param enabled
	 */
	public void enabelModalDeviceDialogs(boolean enabled) {
		this.setProperty(DEVICE_DIALOG_USE_MODAL, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the the device dialogs behavior
	 * @return boolean value to signal the modality of the device dialog
	 */
	public boolean isDeviceDialogsModal() {
		return Boolean.valueOf(this.getProperty(DEVICE_DIALOG_USE_MODAL, "false").trim()); //$NON-NLS-1$
	}

	/**
	 * set device dialog behavior, always visible
	 * @param enabled
	 */
	public void enabelDeviceDialogsOnTop(boolean enabled) {
		this.setProperty(DEVICE_DIALOG_ON_TOP, GDE.STRING_EMPTY + enabled);
	}

	/**
	 * query the the device dialogs behavior
	 * @return boolean value to signal the placement of the device dialog
	 */
	public boolean isDeviceDialogsOnTop() {
		return Boolean.valueOf(this.getProperty(DEVICE_DIALOG_ON_TOP, "false").trim()); //$NON-NLS-1$
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
		return Boolean.valueOf(this.getProperty(LOCALE_CHANGED, "false")); //$NON-NLS-1$
	}

	/**
	 * set new locale language (de,en, ..), if language has been changed set locale changed to true to indicate copy device properties
	 * @param newLanguage
	 */
	public void setLocaleLanguage(String newLanguage) {
		if (!this.getLocale().getLanguage().equals(newLanguage)) {
			this.setProperty(LOCALE_CHANGED, "true"); //$NON-NLS-1$
			this.setProperty(LOCALE_IN_USE, newLanguage);
		}
	}

	/**
	 * query the locale language (en, de, ...) to be used to copy set of localized device property files
	 * if local is not supported, ENGLISH is used as default
	 * @return used locale
	 */
	public Locale getLocale() {
		// get the locale from loaded properties or system default
		Locale locale =  new Locale(this.getProperty(LOCALE_IN_USE, Locale.getDefault().getLanguage()));
		if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage()) || locale.getLanguage().equals(Locale.GERMAN.getLanguage())) {
			this.setProperty(LOCALE_IN_USE, locale.getLanguage());
		}
		else {
			this.setProperty(LOCALE_IN_USE, Locale.ENGLISH.getLanguage()); // fall back to 'en' as default for not supported localse
			locale = Locale.ENGLISH;
		}
		return locale;
	}

	/**
	 * get the device dialog alpha value, 50 is a good transparency starting point
	 * @return the alphablending value
	 */
	public int getDialogAlphaValue() {
		return new Integer(this.getProperty(ALPHA_BLENDING_VALUE, "50").trim()).intValue(); //$NON-NLS-1$
	}

	/**
	 * set a new alpha transparency value for the device dialog
	 * @param newAlphaValue
	 */
	public void setDialogAlphaValue(int newAlphaValue) {
		this.setProperty(ALPHA_BLENDING_VALUE, GDE.STRING_EMPTY + newAlphaValue);
	}

	/**
	 * set if alpha blending for devive dialog should be used
	 * (supporting window manager is pre-req, covered by SWT)
	 * @param enable
	 */
	public void setDeviceDialogAlphaEnabled(boolean enable) {
		this.setProperty(APLHA_BLENDING_ENABLED, GDE.STRING_EMPTY + enable);
	}

	/**
	 * query usage of alpha blending for device dialog
	 * @return true if alphablending is enabled
	 */
	public boolean isDeviceDialogAlphaEnabled() {
		return Boolean.valueOf(this.getProperty(APLHA_BLENDING_ENABLED, "false")); //$NON-NLS-1$
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
	 * @return the isDevicePropertiesReplaced
	 */
	public boolean isDevicePropertiesReplaced() {
		return this.isDevicePropertiesReplaced;
	}

	/**
	 * query value if desktop shortcut needs to be created
	 */
	public boolean isDesktopShortcutCreated() {
		return Boolean.valueOf(this.getProperty(IS_DESKTOP_SHORTCUT_CREATED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if DataExplorer application is registerd to operating system
	 */
	public boolean isApplicationRegistered() {
		return Boolean.valueOf(this.getProperty(IS_APPL_REGISTERED, "false")); //$NON-NLS-1$
	}

	/**
	 * query value if a hint was displayed to enalble uucp locking used on UNIX based systems with RXTXcomm
	 */
	public boolean isLockUucpHinted() {
		return Boolean.valueOf(this.getProperty(IS_LOCK_UUCP_HINTED, "false")); //$NON-NLS-1$
	}

	/**
	 * set the background color of the main graphics curve area
	 * @param curveAreaBackground
	 */
	public void setGraphicsCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(GRAPHICS_AREA_BACKGROUND, curveAreaBackground.getRed()+GDE.STRING_COMMA + curveAreaBackground.getGreen()+GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * set the background color of the main graphics curve area
	 * @return requested color
	 */
	public Color getGraphicsCurveAreaBackground() {
		return getColor(COMPARE_AREA_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsCurveAreaBackgroundStr() {
		return this.getProperty(COMPARE_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve area
	 * @param curveAreaBackground
	 */
	public void setCompareCurveAreaBackground(Color curveAreaBackground) {
		this.setProperty(COMPARE_AREA_BACKGROUND, curveAreaBackground.getRed()+GDE.STRING_COMMA + curveAreaBackground.getGreen()+GDE.STRING_COMMA + curveAreaBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve area
	 * @return requested color
	 */
	public Color getCompareCurveAreaBackground() {
		return getColor(COMPARE_AREA_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCompareCurveAreaBackgroundStr() {
		return this.getProperty(COMPARE_AREA_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the main graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setGraphicsSurroundingBackground(Color surroundingBackground) {
		this.setProperty(GRAPHICS_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the main graphics curve surrounding area
	 * @return requested color
	 */
	public Color getGraphicsSurroundingBackground() {
		return getColor(GRAPHICS_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsSurroundingBackgroundStr() {
		return this.getProperty(GRAPHICS_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the compare graphics curve surrounding area
	 * @param surroundingBackground
	 */
	public void setCompareSurroundingBackground(Color surroundingBackground) {
		this.setProperty(COMPARE_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the compare graphics curve surrounding area
	 * @return requested color
	 */
	public Color getCompareSurroundingBackground() {
		return getColor(COMPARE_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCompareSurroundingBackgroundStr() {
		return this.getProperty(COMPARE_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the graphics curve area
	 * @param borderColor
	 */
	public void setCurveGraphicsBorderColor(Color borderColor) {
		this.setProperty(GRAPHICS_BORDER_COLOR, borderColor.getRed()+GDE.STRING_COMMA + borderColor.getGreen()+GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * get the border color of the graphics curve area
	 * @return requested color
	 */
	public Color getGraphicsCurvesBorderColor() {
		return getColor(GRAPHICS_BORDER_COLOR, "180,180,180"); //COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getGraphicsCurvesBorderColorStr() {
		return this.getProperty(GRAPHICS_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the border color of the curve compare graphics area
	 * @param borderColor
	 */
	public void setCurveCompareBorderColor(Color borderColor) {
		this.setProperty(COMPARE_BORDER_COLOR, borderColor.getRed()+GDE.STRING_COMMA + borderColor.getGreen()+GDE.STRING_COMMA + borderColor.getBlue());
	}

	/**
	 * set the background color of the curve compare graphics area
	 * @return requested color
	 */
	public Color getCurveCompareBorderColor() {
		return getColor(COMPARE_BORDER_COLOR, "180,180,180"); //COLOR_GREY //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCurveCompareBorderColorStr() {
		return this.getProperty(COMPARE_BORDER_COLOR, "180,180,180").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the statistics window surrounding area
	 * @param surroundingBackground
	 */
	public void setSatisticsSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(STATISTICS_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getStatisticsSurroundingAreaBackground() {
		return getColor(STATISTICS_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getStatisticsSurroundingAreaBackgroundStr() {
		return this.getProperty(STATISTICS_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the statistics window surrounding area
	 * @param innerAreaBackground
	 */
	public void setSatisticsInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(STATISTICS_INNER_BACKGROUND, innerAreaBackground.getRed()+GDE.STRING_COMMA + innerAreaBackground.getGreen()+GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getStatisticsInnerAreaBackground() {
		return getColor(STATISTICS_INNER_BACKGROUND, "255,255,255"); //COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getStatisticsInnerAreaBackgroundStr() {
		return this.getProperty(STATISTICS_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the analog window surrounding area
	 * @param surroundingBackground
	 */
	public void setAnalogSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(ANALOG_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the analog window surrounding area
	 * @return requested color
	 */
	public Color getAnalogSurroundingAreaBackground() {
		return getColor(ANALOG_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getAnalogSurroundingAreaBackgroundStr() {
		return this.getProperty(ANALOG_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the analog window surrounding area
	 * @param surroundingBackground
	 */
	public void setAnalogInnerAreaBackground(Color surroundingBackground) {
		this.setProperty(ANALOG_INNER_BACKGROUND, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the analog window surrounding area
	 * @return requested color
	 */
	public Color getAnalogInnerAreaBackground() {
		return getColor(ANALOG_INNER_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getAnalogInnerAreaBackgroundStr() {
		return this.getProperty(ANALOG_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the digital window surrounding area
	 * @param surroundingBackground
	 */
	public void setDigitalSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(DIGITAL_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the digital window surrounding area
	 * @return requested color
	 */
	public Color getDigitalSurroundingAreaBackground() {
		return getColor(DIGITAL_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getDigitalSurroundingAreaBackgroundStr() {
		return this.getProperty(DIGITAL_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the digital window surrounding area
	 * @param innerAreaBackground
	 */
	public void setDigitalInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(DIGITAL_INNER_BACKGROUND, innerAreaBackground.getRed()+GDE.STRING_COMMA + innerAreaBackground.getGreen()+GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the statistics window surrounding area
	 * @return requested color
	 */
	public Color getDigitalInnerAreaBackground() {
		return getColor(DIGITAL_INNER_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getDigitalInnerAreaBackgroundStr() {
		return this.getProperty(DIGITAL_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the cell voltage window surrounding area
	 * @param surroundingBackground
	 */
	public void setCellVoltageSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(CELL_VOLTAGE_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the cell voltage window surrounding area
	 * @return requested color
	 */
	public Color getCellVoltageSurroundingAreaBackground() {
		return getColor(CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCellVoltageSurroundingAreaBackgroundStr() {
		return this.getProperty(CELL_VOLTAGE_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the cell voltage window surrounding area
	 * @param innerAreaBackground
	 */
	public void setCellVoltageInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(CELL_VOLTAGE_INNER_BACKGROUND, innerAreaBackground.getRed()+GDE.STRING_COMMA + innerAreaBackground.getGreen()+GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the cell voltage window surrounding area
	 * @return requested color
	 */
	public Color getCellVoltageInnerAreaBackground() {
		return getColor(CELL_VOLTAGE_INNER_BACKGROUND, "250,249,211"); //COLOR_CANVAS_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getCellVoltageInnerAreaBackgroundStr() {
		return this.getProperty(CELL_VOLTAGE_INNER_BACKGROUND, "250,249,211").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the file comment window surrounding area
	 * @param surroundingBackground
	 */
	public void setFileCommentSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(FILE_COMMENT_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the file comment window surrounding area
	 * @return requested color
	 */
	public Color getFileCommentSurroundingAreaBackground() {
		return getColor(FILE_COMMENT_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getFileCommentSurroundingAreaBackgroundStr() {
		return this.getProperty(FILE_COMMENT_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the file comment window surrounding area
	 * @param innerAreaBackground
	 */
	public void setFileCommentInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(FILE_COMMENT_INNER_BACKGROUND, innerAreaBackground.getRed()+GDE.STRING_COMMA + innerAreaBackground.getGreen()+GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the file comment window surrounding area
	 * @return requested color
	 */
	public Color getFileCommentInnerAreaBackground() {
		return getColor(FILE_COMMENT_INNER_BACKGROUND, "255,255,255"); //COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getFileCommentInnerAreaBackgroundStr() {
		return this.getProperty(FILE_COMMENT_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the object description window surrounding area
	 * @param surroundingBackground
	 */
	public void setObjectDescriptionSurroundingAreaBackground(Color surroundingBackground) {
		this.setProperty(OBJECT_DESC_SURROUND_BACKGRD, surroundingBackground.getRed()+GDE.STRING_COMMA + surroundingBackground.getGreen()+GDE.STRING_COMMA + surroundingBackground.getBlue());
	}

	/**
	 * get the background color of the object description window surrounding area
	 * @return requested color
	 */
	public Color getObjectDescriptionSurroundingAreaBackground() {
		return getColor(OBJECT_DESC_SURROUND_BACKGRD, "250,249,230"); //COLOR_VERY_LIGHT_YELLOW //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getObjectDescriptionSurroundingAreaBackgroundStr() {
		return this.getProperty(OBJECT_DESC_SURROUND_BACKGRD, "250,249,230").trim(); //$NON-NLS-1$
	}

	/**
	 * set the background color of the object description window surrounding area
	 * @param innerAreaBackground
	 */
	public void setObjectDescriptionInnerAreaBackground(Color innerAreaBackground) {
		this.setProperty(OBJECT_DESC_INNER_BACKGROUND, innerAreaBackground.getRed()+GDE.STRING_COMMA + innerAreaBackground.getGreen()+GDE.STRING_COMMA + innerAreaBackground.getBlue());
	}

	/**
	 * get the background color of the object description window surrounding area
	 * @return requested color
	 */
	public Color getObjectDescriptionInnerAreaBackground() {
		return getColor(OBJECT_DESC_INNER_BACKGROUND, "255,255,255"); //COLOR_WHITE //$NON-NLS-1$
	}

	/**
	 * @return the grid vertical color of the compare window as string of (r,g,b)
	 */
	public String getObjectDescriptionInnerAreaBackgroundStr() {
		return this.getProperty(OBJECT_DESC_INNER_BACKGROUND, "255,255,255").trim(); //$NON-NLS-1$
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
		return xsdThread != null ? xsdThread.isAlive() : false ;
	}
}
