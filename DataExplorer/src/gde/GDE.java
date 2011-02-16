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
package gde;

import gde.config.Settings;
import gde.data.RecordSet;
import gde.exception.ApplicationConfigurationException;
import gde.log.Level;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;

import java.awt.SplashScreen;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Winfried Brügmann
 * class with less import statements hosting the main method to build a controlled classpath
 *  - to be independent of external specified environment like CLASSPATH
 *  - to be independent from JRE updates
 *  - class defines also generic constants used for DataExplorer (GDE)
 */
public class GDE {
	public static final String	STRING_WITHIN_ECLIPSE	= "/bin/";
	final static String $CLASS_NAME = GDE.class.getName();
	public final static long 	StartTime 	= new Date().getTime();
	public static Handler logHandler = null;
	
	// ****** begin global constants section *******
	public static final String		VERSION														= "Version 2.72";						//$NON-NLS-1$
	public static final String		NAME_SHORT												= "GDE";										//$NON-NLS-1$
	public static final String		NAME_LONG													= "DataExplorer";						//$NON-NLS-1$
	public final static String		DEVICE_PROPERTIES_XSD_VERSION			= "_V12"; 									//$NON-NLS-1$
	public final static String		GRAPHICS_TEMPLATES_XSD_VERSION		= "_V05"; 									//$NON-NLS-1$
	
	public static final boolean		IS_WINDOWS								= System.getProperty("os.name").toLowerCase().startsWith("windows");//$NON-NLS-1$ //$NON-NLS-2$
	public static final boolean		IS_LINUX									= System.getProperty("os.name").toLowerCase().startsWith("linux");	//$NON-NLS-1$ //$NON-NLS-2$
	public static final boolean		IS_MAC										= System.getProperty("os.name").toLowerCase().startsWith("mac");		//$NON-NLS-1$ //$NON-NLS-2$
	public static final boolean		IS_MAC_COCOA							= GDE.IS_MAC && System.getProperty("DO_NOT_USE_COCOA") == null && SWT.getPlatform().toLowerCase().startsWith("cocoa");		//$NON-NLS-1$ //$NON-NLS-2$
	public static final boolean		IS_ARCH_DATA_MODEL_64			= System.getProperty("sun.arch.data.model").equals("64");		//$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String		STRING_BASE_PACKAGE				= "gde"; //$NON-NLS-1$
	public final static int				WIDGET_FONT_SIZE					= GDE.IS_LINUX ? 8 : GDE.IS_MAC ? 12 : 9;
	public final static String		WIDGET_FONT_NAME					= GDE.IS_WINDOWS ? "Microsoft Sans Serif" : "Sans Serif"; 					//$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String		BIT_MODE									= System.getProperty("sun.arch.data.model") != null   							//$NON-NLS-1$
																															? System.getProperty("sun.arch.data.model")											//$NON-NLS-1$
																															: System.getProperty("com.ibm.vm.bitmode");											//$NON-NLS-1$
	public static final String		FILE_SEPARATOR_UNIX				= "/";																															//$NON-NLS-1$
	public static final String		FILE_SEPARATOR_WINDOWS		= "\\";																															//$NON-NLS-1$
	public static final String		FILE_SEPARATOR						= System.getProperty("file.separator");															//$NON-NLS-1$
	public static final String		JAVA_IO_TMPDIR						= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR)	//$NON-NLS-1$
																															? System.getProperty("java.io.tmpdir")  //$NON-NLS-1$
																															: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR;		//$NON-NLS-1$
																															

	public final static int				SIZE_BYTES_INTEGER				= Integer.SIZE / 8;			// 32 bits / 8 bits per byte 
	public final static int				SIZE_BYTES_LONG						= Long.SIZE / 8;				// 64 bits / 8 bits per byte 
	public final static int				SIZE_UTF_SIGNATURE				= 2;										// 2 byte UTF line header 

	public final static String		STRING_NEW_LINE						= "\n"; 				// is OS dependent //$NON-NLS-1$
	public static final String		STRING_MESSAGE_CONCAT			= " - ";				//$NON-NLS-1$
	public static final String		STRING_DASH								= "-";					//$NON-NLS-1$
	public static final String		STRING_UNDER_BAR					= "_";					//$NON-NLS-1$
	public static final String		STRING_EMPTY							= "";						//$NON-NLS-1$
	public static final String		STRING_BLANK							= " ";					//$NON-NLS-1$
	public static final String		STRING_URL_BLANK					= "%20";				//$NON-NLS-1$
	public static final String		STRING_COLON							= ":";					//$NON-NLS-1$
	public static final String		STRING_BLANK_COLON_BLANK	= " : ";				//$NON-NLS-1$
	public static final String		STRING_COMMA							= ",";					//$NON-NLS-1$
	public static final String		STRING_SEMICOLON					= ";";					//$NON-NLS-1$
	public static final String		STRING_DOT								= ".";					//$NON-NLS-1$
	public static final String		STRING_EQUAL							= "=";					//$NON-NLS-1$
	public static final String		STRING_STAR								= "*";					//$NON-NLS-1$
	public static final String		STRING_LEFT_BRACKET				= "[";					//$NON-NLS-1$
	public static final String		STRING_RIGHT_BRACKET			= "]";					//$NON-NLS-1$
	public static final String		STRING_OR									= "|";					//$NON-NLS-1$
	public static final String		STRING_DOLLAR							= "$";					//$NON-NLS-1$
	public static final String		STRING_OS_NAME						= "os.name";		//$NON-NLS-1$
	public static final String		STRING_UTF_8							= "UTF-8";			//$NON-NLS-1$
	public static final String		STRING_ISO_8895_1					= "ISO-8859-1"; //$NON-NLS-1$
	public static final String		STRING_PLUS								= "+"; 					//$NON-NLS-1$
	public static final String		STRING_TRUE								= "true"; 			//$NON-NLS-1$
	public static final String		STRING_FALSE							= "false"; 			//$NON-NLS-1$
	public static final String		STRING_SINGLE_QUOAT				= "'"; 					//$NON-NLS-1$
	
	public static final String[]	STRING_ARRAY_TRUE_FALSE		= new String[] { "true", "false" }; 	//$NON-NLS-1$ //$NON-NLS-2$
	
	public static final String		STRING_MAC_APP_BASE_PATH	= "/Applications/"; 									//$NON-NLS-1$
	public static final String		STRING_MAC_DOT_APP				= ".app"; 														//$NON-NLS-1$
	public static final String		STRING_MAC_APP_EXE_PATH		= "/Contents/MacOS/"; 								//$NON-NLS-1$
	public static final String		STRING_MAC_APP_RES_PATH		= "/Contents/Resources"; 							//$NON-NLS-1$
	public static final String		STRING_MAC_APP_OPEN				= "open";															//$NON-NLS-1$
	
	public static final String		FILE_ENDING_STAR_LOV			= "*.lov";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_OSD			= "*.osd";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_CSV			= "*.csv";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_JPG			= "*.jpg";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_PNG			= "*.png";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_GIF			= "*.gif";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_XML			= "*.xml";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_KMZ			= "*.kmz";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_HEX			= "*.hex";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_INI			= "*.ini";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_TXT			= "*.txt";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_LOG			= "*.log";//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR_STAR			= "*.*";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_JAR				= ".jar";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_LOV				= ".lov";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_OSD				= ".osd";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_CSV				= ".csv";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_NMEA			= ".nmea";//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_JPG				= ".jpg";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_PNG				= ".png";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_GIF				= ".gif";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_TXT				= ".txt";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_LOG				= ".log";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_STF				= ".stf";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_ZIP				= ".zip";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_BAK				= ".bak";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_TMP				= ".tmp";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_XML				= ".xml";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_XSD				= ".xsd";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_HEX				= ".hex";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_KMZ				= ".kmz";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_KML				= ".kml";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_EXE				= ".exe";	//$NON-NLS-1$
	public static final String		FILE_ENDING_DOT_INI				= ".ini";	//$NON-NLS-1$
	public static final String		FILE_ENDING_LOV						= "lov";	//$NON-NLS-1$
	public static final String		FILE_ENDING_OSD						= "osd";	//$NON-NLS-1$
	public static final String		FILE_ENDING_CSV						= "csv";	//$NON-NLS-1$
	public static final String		FILE_ENDING_NMEA					= "nmea";	//$NON-NLS-1$
	public static final String		FILE_ENDING_XML						= "xml";	//$NON-NLS-1$
	public static final String		FILE_ENDING_JPG						= "jpg";	//$NON-NLS-1$
	public static final String		FILE_ENDING_PNG						= "png";	//$NON-NLS-1$
	public static final String		FILE_ENDING_GIF						= "gif";	//$NON-NLS-1$
	public static final String		FILE_ENDING_BAK						= "bak";	//$NON-NLS-1$
	public static final String		FILE_ENDING_TMP						= "tmp";	//$NON-NLS-1$
	public static final String		FILE_ENDING_KMZ						= "kmz";	//$NON-NLS-1$
	public static final String		FILE_ENDING_STAR					= "*";		//$NON-NLS-1$
	public static final String		FILE_ENDING_HEX						= "hex";	//$NON-NLS-1$
	public static final String		FILE_ENDING_EXE						= "exe";	//$NON-NLS-1$
	public static final String		FILE_ENDING_INI						= "ini";	//$NON-NLS-1$

	public final static String		LINE_SEPARATOR						= System.getProperty("line.separator"); // is OS dependent //$NON-NLS-1$

	public static final String	  BOOTSTRAP_LOG							= "/bootstrap.log";	//$NON-NLS-1$
	public static final String		ECLIPSE_STRING						= "ECLIPSE";				//$NON-NLS-1$
	// ****** end global constants section *******
	
	// begin OSD file format
	public static final String		DATA_EXPLORER_FILE_VERSION			= "DataExplorer version : ";		//$NON-NLS-1$
	public static final String		LEGACY_FILE_VERSION							= "OpenSerialData version : ";	//$NON-NLS-1$
	public static final String		DATA_EXPLORER_FILE							= "DataExplorer";								//$NON-NLS-1$
	public static final String		LEGACY_OSDE_FILE								= "OpenSerialData";							//$NON-NLS-1$
	public static final int				DATA_EXPLORER_FILE_VERSION_INT	= 2; 														// actual version

	public static final String		CREATION_TIME_STAMP				= "Created : "; 									//$NON-NLS-1$
	public static final String		LAST_UPDATE_TIME_STAMP		= "Updated : "; 									//$NON-NLS-1$
	public static final String		FILE_COMMENT							= "FileComment : "; 							//$NON-NLS-1$
	public static final String		DEVICE_NAME								= "DeviceName : "; 								//$NON-NLS-1$
	public static final String		CHANNEL_CONFIG_TYPE				= "Channel/Configuration Type : ";//$NON-NLS-1$
	public static final String		RECORD_SET_SIZE						= "NumberRecordSets : "; 					//$NON-NLS-1$
	public static final String		RECORD_SET_NAME						= "RecordSetName : "; 						//$NON-NLS-1$
	public static final String		CHANNEL_CONFIG_NUMBER			= "Channel/Configuration Number : "; //$NON-NLS-1$

	public static final String		DATA_DELIMITER						= "||::||"; 											//$NON-NLS-1$
	public static final String		CHANNEL_CONFIG_NAME				= "Channel/Configuration Name: "; //$NON-NLS-1$
	public static final String		OBJECT_KEY								= "ObjectKey : "; 								//$NON-NLS-1$

	public static final String		RECORD_SET_COMMENT				= "RecordSetComment : "; 					//$NON-NLS-1$
	public static final String		RECORD_SET_PROPERTIES			= "RecordSetProperties : "; 			//$NON-NLS-1$
	public static final String		RECORDS_PROPERTIES				= "RecordProperties : "; 					//$NON-NLS-1$
	public static final String		RECORD_DATA_SIZE					= "RecordDataSize : "; 						//$NON-NLS-1$
	public static final String		RECORD_SET_DATA_POINTER		= "RecordSetDataPointer : "; 			//$NON-NLS-1$
	public static final String		RECORD_SET_DATA_BYTES			= "RecordSetDataBytes : "; 				//$NON-NLS-1$
	
	public static final String[]	OSD_FORMAT_HEADER_KEYS		= new String[] { CREATION_TIME_STAMP, FILE_COMMENT, DEVICE_NAME, OBJECT_KEY, CHANNEL_CONFIG_TYPE, RECORD_SET_SIZE };
	public static final String[]	OSD_FORMAT_DATA_KEYS			= new String[] { CHANNEL_CONFIG_NAME, RECORD_SET_NAME, RECORD_SET_COMMENT, RECORD_SET_PROPERTIES, RECORDS_PROPERTIES,
																																RECORD_DATA_SIZE, RECORD_SET_DATA_POINTER	};
	// begin OSD file format

	// begin LogView file format
	public static final String	LOV_CONFIG_DATA 		= "logview_config_data"; 			//$NON-NLS-1$

	public static final String	DATA_POINTER_POS		= "Data_Pointer_Pos : "; 			//$NON-NLS-1$
	public static final String	LOV_HEADER_SIZE			= "Header_Size : "; 					//$NON-NLS-1$
	public static final String	LOV_FORMAT_VERSION	= "Format_Version : "; 				//$NON-NLS-1$
	public static final String	LOV_SSTREAM_VERSION	= "String_Stream_Version : "; //$NON-NLS-1$
	public static final String	LOV_STREAM_VERSION	= "Stream_Version : "; 				//$NON-NLS-1$
	
	public static final String	LOV_TIME_STEP = "TimeStep_ms=";							 			//$NON-NLS-1$
	public static final String	LOV_NUM_MEASUREMENTS = "WerteAnzahl="; 						//$NON-NLS-1$
		
	public static final String[]	LOV_FORMAT_HEADER_KEYS		= new String[] { CREATION_TIME_STAMP, FILE_COMMENT, DEVICE_NAME, CHANNEL_CONFIG_TYPE, RECORD_SET_SIZE };
	public static final String[]	LOV_FORMAT_DATA_KEYS			= new String[] { CHANNEL_CONFIG_NAME, RECORD_SET_NAME, RECORD_SET_COMMENT, RECORD_SET_PROPERTIES, RECORDS_PROPERTIES,
																																RECORD_DATA_SIZE, RECORD_SET_DATA_POINTER, RECORD_SET_DATA_BYTES, RecordSet.TIME_STEP_MS, LOV_NUM_MEASUREMENTS};
	// end LogView file format
	
	// begin CSV file format
	public static final String		CSV_DATA_HEADER								= "CSV_data_header : ";								//$NON-NLS-1$
	public static final String		CSV_DATA_HEADER_MEASUREMENTS	= "CSV_data_header_measurements : ";	//$NON-NLS-1$
	public static final String		CSV_DATA_HEADER_UNITS					= "CSV_data_header_units : ";					//$NON-NLS-1$
	public static final String		CSV_DATA_TYPE									= "CSV_data_type : ";									//$NON-NLS-1$
	public static final String		CSV_DATA_TYPE_RAW							= "raw";															//$NON-NLS-1$
	public static final String		CSV_DATA_TYPE_ABS							= "abs";															//$NON-NLS-1$
	// begin CSV file format


	final static Logger	log										= Logger.getLogger(GDE.class.getName());
	static Logger				rootLogger;
	static Vector<String> initErrors = new Vector<String>(0);
	public static Shell splash;
	public static ProgressBar progBar;
	
	static final String	DEVICES_PLUG_IN_DIR		= "devices/"; //$NON-NLS-1$
	
	/**
	 * main method to start the DataExplorer application
	 * @param args
	 */
	public static void main(String[] args) {
		final String $METHOD_NAME = "main"; //$NON-NLS-1$
		String inputFilePath = STRING_EMPTY;
		try {
			Settings.getInstance();
			//DeviceData	data = new DeviceData();
			//data.tracking = true;
			DataExplorer.display														= new Display(); //data);
			DataExplorer.shell															= new Shell(DataExplorer.display);
			GDE.showSplash();
			//Sleak sleak = new Sleak();
			//sleak.open();
			GDE.initLogger();
			log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, GDE.NAME_LONG + GDE.STRING_BLANK + VERSION); //$NON-NLS-1$
			
			//build the main thread context classloader to enable dynamic plugin class loading 
			Thread.currentThread().setContextClassLoader(GDE.getClassLoader());
			//ClassLoader loader = Thread.currentThread().getContextClassLoader();
			//log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "class loader build, test it");
			//Class c = loader.loadClass("org.eclipse.swt.widgets.Composite");
			//log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, c.getProtectionDomain().getCodeSource().getLocation().toExternalForm());
			//c = loader.loadClass("gde.ui.DataExplorer");
			//log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "Class object loaded successfully");
			//Object o = c.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
			//log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "Class instance loaded successfully");
			
			Display.setAppName(GDE.NAME_LONG);
			Display.setAppVersion(GDE.VERSION);

			DataExplorer application = DataExplorer.getInstance();
			for (int i=0; i<args.length; ++i) {
				log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "commandline arg[" + i + "] = " + args[i]);//$NON-NLS-1$ //$NON-NLS-2$ $NON-NLS-2$
			}
			if (args.length > 0) {
				args[0] = args[0].trim();
				if (args[0].toLowerCase().endsWith(FILE_ENDING_DOT_OSD) || args[0].toLowerCase().endsWith(FILE_ENDING_DOT_LOV)) {
					inputFilePath = args[0];
					log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "inputFilePath = " + inputFilePath); //$NON-NLS-1$
				}
			}
			
	    // list system properties
			StringBuilder sb = new StringBuilder().append("Environment : \n"); //$NON-NLS-1$
			Properties props = System.getProperties();
	    Enumeration<?> e = props.propertyNames();
	    while (e.hasMoreElements()) {
	    	String propName = (String)e.nextElement();
	    	sb.append(propName).append(" = ").append(props.get(propName)).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
	    }
	    sb.append("SWT.PLATFORM = ").append(SWT.getPlatform()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
	    log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, sb.toString());

			GDE.seStartupProgress(40);
			application.execute(inputFilePath);
		}
		catch (Throwable e) {
			log.logp(Level.SEVERE, GDE.$CLASS_NAME, $METHOD_NAME, e.getMessage(), e);
			Throwable t = e;
			while (t!=null) {
				log.logp(Level.SEVERE, GDE.$CLASS_NAME, $METHOD_NAME, t.getMessage(), t);
				t = t.getCause();
			}
		}
	}

	/**
	 * find the class loader of the given class
	 * @return URL ClassLoader
	 * @throws MalformedURLException
	 * @throws URISyntaxException 
	 * @throws ApplicationConfigurationException 
	 * @throws ClassNotFoundException 
	 */
	public static ClassLoader getClassLoader() throws MalformedURLException, URISyntaxException, ApplicationConfigurationException {
		final String $METHOD_NAME = "getClassLoader"; //$NON-NLS-1$
		String basePath;
		final Vector<URL> urls = new Vector<URL>();
		URL url = GDE.class.getProtectionDomain().getCodeSource().getLocation();
		log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "base URL = " + url.toString()); //$NON-NLS-1$
		if (url.getPath().endsWith(FILE_SEPARATOR_UNIX)) { // running inside Eclipse
			log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "started inside Eclipse"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().indexOf(DataExplorer.class.getSimpleName()));
			basePath = basePath.replace(STRING_URL_BLANK, STRING_BLANK);
			log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "basePath = " + basePath); //$NON-NLS-1$
			File file = new File(basePath);
			String[] files = file.list();
			if (files == null) {
				throw new ApplicationConfigurationException(Messages.getString(MessageIds.GDE_MSGE0001, new Object[]{ basePath }));
			}
			for (String path : files) {
				if (!path.startsWith(GDE.STRING_DOT)) urls.add(new URL("file:" + basePath + path + STRING_WITHIN_ECLIPSE)); //$NON-NLS-1$
			}
		}
		else { // started outside java -jar *.jar
			log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "started outside with: java -jar *.jar"); //$NON-NLS-1$
			basePath = url.getFile().substring(0, url.getPath().lastIndexOf(FILE_SEPARATOR_UNIX)+1);
			basePath = basePath.replace(STRING_URL_BLANK, STRING_BLANK).replace(FILE_SEPARATOR_WINDOWS,	FILE_SEPARATOR_UNIX) + DEVICES_PLUG_IN_DIR;

			log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "basePath = " + basePath); //$NON-NLS-1$
			File file = new File(basePath);
			String[] files = file.list();
			if (files == null) {
				throw new ApplicationConfigurationException(Messages.getString(MessageIds.GDE_MSGE0001, new Object[]{ basePath }));
			}
			for (String path : files) {
				if (path.endsWith(FILE_ENDING_DOT_JAR)) {
					URL fileUrl = new File(basePath + path).toURI().toURL();
					urls.add(fileUrl);
					log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "adding : " + fileUrl.toURI()); //$NON-NLS-1$
				}
			}
		}
		log.logp(Level.INFO, GDE.$CLASS_NAME, $METHOD_NAME, "using class loader URL = " + urls.toString()); //$NON-NLS-1$
		ClassLoader newLoader = AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
			public URLClassLoader run() {
				return new URLClassLoader(urls.toArray(new URL[1]));
			}
		});
		
		return newLoader;
	}	
	
	public static String getDevicesClasspathAsString() throws Exception {
    URL[] urls = ((URLClassLoader)GDE.getClassLoader()).getURLs();

    StringBuilder sb = new StringBuilder(); //.append(GDE.class.getProtectionDomain().getCodeSource().getLocation().getFile());
    String pathSeparator = System.getProperty("path.separator"); //$NON-NLS-1$
    for(URL url : urls) {
        sb.append(pathSeparator).append(url.getFile());
    }  
    return sb.toString();
	}
	
	/**
	 * init logger
	 */
	public static void initLogger() {
		LogFormatter lf = new LogFormatter();
		rootLogger = Logger.getLogger(STRING_EMPTY);
		
		// clean up all handlers from outside
		Handler[] handlers = rootLogger.getHandlers();
    for ( int index = 0; index < handlers.length; index++ ) {
    	rootLogger.removeHandler(handlers[index]);
    }
		rootLogger.setLevel(Level.ALL);
		
		if (System.getProperty(ECLIPSE_STRING) == null) { // running outside eclipse
			try {
				logHandler = new FileHandler(GDE.JAVA_IO_TMPDIR + BOOTSTRAP_LOG, 50000, 1);
				logHandler.setFormatter(lf);
				logHandler.setLevel(Level.INFO);
				rootLogger.addHandler(logHandler);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			logHandler = new ConsoleHandler();
			logHandler.setFormatter(lf);
			logHandler.setLevel(Level.INFO);
			rootLogger.addHandler(logHandler);
		}
	}

	/**
	 * @return the initErrors
	 */
	public static Vector<String> getInitErrors() {
		return initErrors;
	}

	/**
	 * @param errorMessage the initial errors message to set
	 */
	public static void setInitError(String errorMessage) {
		GDE.initErrors.add(errorMessage);
	}
	/**
	 * display the splash image for the given time inseconds
	 * @param display
	 * @param timeoutSec
	 */
	private static void showSplash() {
		final SplashScreen startSplash = SplashScreen.getSplashScreen();
    if (startSplash != null) {
  		final Image image = new Image(DataExplorer.display, GDE.class.getClassLoader().getResourceAsStream("gde/resource/splash.png"));
  		GC gc = new GC(image);
  		gc.drawImage(image, 0, 0);
  		gc.dispose();
  		final Shell splash = new Shell(SWT.ON_TOP|SWT.BORDER);
  		final ProgressBar bar = new ProgressBar(splash, SWT.NONE);
  		bar.setMaximum(100);
  		final Label label = new Label(splash, SWT.NONE);
  		label.setImage(image);
  		FormLayout layout = new FormLayout();
  		splash.setLayout(layout);
  		FormData labelData = new FormData();
  		labelData.right = new FormAttachment(100, 0);
  		labelData.bottom = new FormAttachment(100, 0);
  		label.setLayoutData(labelData);
  		FormData progressData = new FormData();
  		progressData.height = 15;
  		progressData.left = new FormAttachment(0, 5);
  		progressData.right = new FormAttachment(100, -5);
  		progressData.bottom = new FormAttachment(100, -5);
  		bar.setLayoutData(progressData);
  		java.awt.Rectangle splashRect = startSplash.getBounds();
  		bar.setSize(splashRect.width-20,15);
  		splash.pack();
  		splash.setLocation(splashRect.x, splashRect.y);
  		splash.addDisposeListener(new DisposeListener() {	
  			@Override
  			public void widgetDisposed(DisposeEvent arg0) {
  				splash.close();
  				startSplash.close();
  			}
  		});
  		splash.open();
  		bar.setSelection(15);
  		GDE.splash = splash;
  		GDE.progBar = bar;
    }
    System.out.println("splash = null");

	}
	
	public static void seStartupProgress(int percent) {
		if(GDE.progBar != null && !GDE.progBar.isDisposed()) GDE.progBar.setSelection(percent);
	}
}
