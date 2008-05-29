/**
 * 
 */
package osde.junit;

import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;
import osde.io.LogViewReader;
import osde.log.LogFormatter;

/**
 * @author brueg
 *
 */
public class LogViewReaderTester extends TestCase {

	Logger				rootLogger;

	String[] files_1_13 = {
	// 1.13
	"d:/Documents/LogView/Picolario/2006_05_12.lov",
	"d:/Documents/LogView/Picolario/2006_06_07.lov",
	"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-02SenderSubC7-K4.lov",
	};
	
	String[] files_1_15 = {
	// 1.15
	"d:/Documents/LogView/UniLog/Starling electro 20070604.lov",
	"d:/Documents/LogView/UniLog/Starling electro 20071010 UniLog.lov",
	"d:/Documents/LogView/Picolario/2007_07_20.lov"
	};
	
	String[] files_1_50 = {
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuB.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuA.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuB.lov", 
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-09-06-Empfänger1300-K1.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/akkumaster.lov", 
	};
	
	String[] files_2_0 = {
	// 2.0
	"d:/Documents/LogView/Picolario/2006_05_12(2).lov",
	"d:/Documents/LogView/Picolario/2006_06_07(2).lov",
	"d:/Documents/LogView/UniLog/2008_13_04_ASW27_LiPo_Westhang.lov",
	"d:/Documents/LogViewOBJ/ASW-27/ASW-27 23.5.2008 19.32.lov",
	"d:/Documents/LogViewOBJ/ASW-27/ASW-27 24.5.2008 15.44.lov",
	"d:/Documents/LogView/UniLog/Auto-Geschwindigkeit.lov",
	};

	String[] files_mixed = {
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuA.lov", 
			//"d:/Documents/LogView/Htronic Akkumaster C4/2007-05-23-FlugakkuB.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-05-24-EmpfängerAA5.lov", 
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-05-24-FlugakkuA.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-05-24-FlugakkuB.lov", 
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerAA4-K2.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerSubCMPX-K1.lov", 
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-01-EmpfängerSubCPan-K3.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-02-AntriebAA8-K2.lov", 
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-02-AntriebTPLiPo2s2p-K2.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-06-02SenderSubC7-K4.lov", 
			//"d:/Documents/LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuA.lov",
			//"d:/Documents/LogView/Htronic Akkumaster C4/2007-07-21-FlugakkuB.lov", 
			"d:/Documents/LogView/Htronic Akkumaster C4/2007-09-06-Empfänger1300-K1.lov",
			"d:/Documents/LogView/Htronic Akkumaster C4/akkumaster.lov", 
			//"d:/Documents/LogView/Htronic Akkumaster C4/Beta2.0_9VoltBlockTest.lov",
			
			"d:/Documents/LogView/Picolario/2006_05_12(2).lov",
			"d:/Documents/LogView/Picolario/2006_05_12.lov",
			"d:/Documents/LogView/Picolario/2006_06_07(2).lov",
			"d:/Documents/LogView/Picolario/2006_06_07.lov",	
			"d:/Documents/LogView/Picolario/2006_06_10.lov", 
			"d:/Documents/LogView/Picolario/2006_06_30.lov",
			"d:/Documents/LogView/Picolario/2006_07_15.lov", 
			"d:/Documents/LogView/Picolario/2006_07_29.lov",
			"d:/Documents/LogView/Picolario/2007_04_04.lov",
			"d:/Documents/LogView/Picolario/2007_04_15.lov",
			"d:/Documents/LogView/Picolario/2007_04_28.lov", 
			"d:/Documents/LogView/Picolario/2007_06_30.lov",
			"d:/Documents/LogView/Picolario/2007_07_20.lov",
			"d:/Documents/LogView/Picolario/Gunnar_pico_test.lov",
			"d:/Documents/LogView/Picolario/Starling e pico 20080426.lov", 
			"d:/Documents/LogView/Picolario/test_8_4.lov",
			
			"d:/Documents/LogView/UniLog/1_Empfaenger.lov",
			"d:/Documents/LogView/UniLog/2008_13_04_ASW27_LiPo_Westhang.lov",
			"d:/Documents/LogView/UniLog/2008_13_04_ASW27_LiPo_Westhang_.lov",
			"d:/Documents/LogView/UniLog/2008_24_02_ASW27_Westang.lov", 
			"d:/Documents/LogView/UniLog/2008_24_02_ASW27_Westang_.lov",
			"d:/Documents/LogView/UniLog/Auto-Geschwindigkeit.lov",
			"d:/Documents/LogView/UniLog/Auto-Geschwindigkeit_1.lov",
			"d:/Documents/LogView/UniLog/BigExcel_20071014.lov",
			"d:/Documents/LogView/UniLog/Capu 20080427.lov",
			"d:/Documents/LogView/UniLog/Starling electro 20070604.lov",
			"d:/Documents/LogView/UniLog/Starling electro 20071010 UniLog.lov",
			"d:/Documents/LogView/UniLog/Starling electro 20071014 UniLog.lov",
			
			"d:/Documents/LogViewOBJ/ASW-27/ASW-27 23.5.2008 19.32.lov",
			"d:/Documents/LogViewOBJ/ASW-27/ASW-27 24.5.2008 15.44.lov"
	};
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		Handler ch = new ConsoleHandler();
		LogFormatter lf = new LogFormatter();
		this.rootLogger = Logger.getLogger("");
		
		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
	  for ( int index = 0; index < handlers.length; index++ ) {
	  	this.rootLogger.removeHandler(handlers[index]);
	  }
		this.rootLogger.setLevel(Level.ALL);
		this.rootLogger.addHandler(ch);
		ch.setFormatter(lf);
		ch.setLevel(Level.ALL);
		
	}
	
	/**
	 * Test method for {@link osde.io.LogViewReader#getHeader(osde.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_1_13() {
		for (String filePath : this.files_1_13) {
			try {
				LogViewReader.getHeader(filePath);
			}
			catch (Exception e) {
				fail(filePath + " - " + e.getMessage() + "\n");
			}
		}
	}
	
	/**
	 * Test method for {@link osde.io.LogViewReader#getHeader(osde.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_1_15() {
		for (String filePath : this.files_1_15) {
			try {
				LogViewReader.getHeader(filePath);
			}
			catch (Exception e) {
				e.printStackTrace();
				fail(filePath + " - " + e.getMessage());
			}
		}
	}
	
	/**
	 * Test method for {@link osde.io.LogViewReader#getHeader(osde.device.IDevice, java.lang.String)}.
	 */
//	public final void testGetHeader_1_50() {
//		for (String filePath : this.files_1_50) {
//			try {
//				LogViewReader.getHeader(filePath);
//			}
//			catch (Exception e) {
//				e.printStackTrace();
//				fail(filePath + " - " + e.getMessage());
//			}
//		}
//	}
	
	/**
	 * Test method for {@link osde.io.LogViewReader#getHeader(osde.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_2_0() {
		for (String filePath : this.files_2_0) {
			try {
				LogViewReader.getHeader(filePath);
			}
			catch (Exception e) {
				e.printStackTrace();
				fail(filePath + " - " + e.getMessage());
			}
		}
	}
	
	/**
	 * Test method for {@link osde.io.LogViewReader#getHeader(osde.device.IDevice, java.lang.String)}.
	 */
	public final void testGetHeader_mixed() {
		HashMap<String, Exception> failures = new HashMap<String, Exception>();
		for (String filePath : this.files_mixed) {
			try {
				LogViewReader.getHeader(filePath);
			}
			catch (Exception e) {
				failures.put(filePath, e);
			}
		}
		
		StringBuilder sb = new StringBuilder();
		for (String key : failures.keySet()) {
			sb.append(key).append(" - ").append(failures.get(key).getMessage()).append("\n");
		}
		if (failures.size() > 0) fail(sb.toString());
	}

}
