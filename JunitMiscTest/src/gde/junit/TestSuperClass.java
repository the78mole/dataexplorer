/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012 Winfried Bruegmann
****************************************************************************************/
package gde.junit;

import gde.GDE;
import gde.config.Settings;
import gde.data.Channel;
import gde.data.Channels;
import gde.data.Record;
import gde.data.RecordSet;
import gde.device.DeviceConfiguration;
import gde.device.IDevice;
import gde.log.LogFormatter;
import gde.messages.MessageIds;
import gde.messages.Messages;
import gde.ui.DataExplorer;
import gde.ui.SWTResourceManager;
import gde.utils.CurveUtils;
import gde.utils.TimeLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class TestSuperClass extends TestCase {
	Logger																rootLogger;
	static {
	Settings.getInstance();
	GDE.display														= new Display();
	GDE.shell															= new Shell(GDE.display);
	}

	final DataExplorer										application	= DataExplorer.getInstance();
	final Channels												channels		= Channels.getInstance();
	final Settings												settings		= Settings.getInstance();
	final String 													tmpDir 			= System.getProperty("java.io.tmpdir").endsWith(GDE.FILE_SEPARATOR) 
																												? System.getProperty("java.io.tmpdir") 
																												: System.getProperty("java.io.tmpdir") + GDE.FILE_SEPARATOR ;


	final TimeLine												timeLine		= new TimeLine();
	Image																	curveArea;
	GC																		curveAreaGC;
	Rectangle															curveAreaBounds;
	GC																		canvasGC;
	int																		offSetX, offSetY;

	TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	Vector<String>												activeDevices;
	File																	dataPath;
	HashMap<String,String>								legacyDeviceNames = new HashMap<String,String>(2);

	Handler																ch					= new ConsoleHandler();
	LogFormatter													lf					= new LogFormatter();

	Logger																logger1			= Logger.getLogger("gde.data.Record");
	Logger																logger2			= Logger.getLogger("gde.data.RecordSet");

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.rootLogger = Logger.getLogger("");

		// clean up all handlers from outside
		Handler[] handlers = this.rootLogger.getHandlers();
		for (Handler handler : handlers) {
			this.rootLogger.removeHandler(handler);
		}
		this.rootLogger.setLevel(Level.WARNING);
		this.rootLogger.addHandler(this.ch);
		this.ch.setFormatter(this.lf);
		this.ch.setLevel(Level.FINER);

		//this.logger1.setLevel(Level.FINE);
		//this.logger1.setUseParentHandlers(true);
		//this.logger2.setLevel(Level.FINE);
		//this.logger2.setUseParentHandlers(true);

		Thread.currentThread().setContextClassLoader(GDE.getClassLoader());

		this.initialize();
		
		//add this two renamed device plug-ins to the list of legacy devices
		this.legacyDeviceNames.put("GPSLogger", "GPS-Logger");
		this.legacyDeviceNames.put("QuadroControl", "QC-Copter");

		Canvas canvas = new Canvas(new Composite(Display.getDefault().getShells()[0], SWT.NONE), SWT.NONE);
		this.canvasGC = SWTResourceManager.getGC(canvas, "curveArea_" + 0); //$NON-NLS-1$
	}

	/**
	 * goes through the existing device properties files and set active flagged devices into active devices list
	 * @throws FileNotFoundException 
	 */
	public void initialize() throws FileNotFoundException {

		File file = new File(this.settings.getDevicesPath());
		if (!file.exists()) throw new FileNotFoundException(this.settings.getDevicesPath());
		String[] files = file.list();
		DeviceConfiguration devConfig;
		this.deviceConfigurations = new TreeMap<String, DeviceConfiguration>(String.CASE_INSENSITIVE_ORDER);
		
		
		//wait until schema is setup
		while (this.settings.isXsdThreadAlive()) {
			try {
				Thread.sleep(5);
			}
			catch (InterruptedException e) {
				//ignore
			}
		}

		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(".xml")) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(this.settings.getDevicesPath() + GDE.FILE_SEPARATOR + files[i]);

					// store all device configurations in a map					
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					System.out.println(deviceKey + GDE.STRING_MESSAGE_CONCAT + keyString);
					this.deviceConfigurations.put(keyString, devConfig);
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				fail(e.toString());
			}
		}
	}

	/**
	 * calculates the new class name for the device
	 */
	protected IDevice getInstanceOfDevice(DeviceConfiguration selectedActiveDeviceConfig) {
		IDevice newInst = null;
		String selectedDeviceName = selectedActiveDeviceConfig.getDeviceImplName().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY);
		//selectedDeviceName = selectedDeviceName.substring(0, 1).toUpperCase() + selectedDeviceName.substring(1);
		String className = selectedDeviceName.contains(GDE.STRING_DOT) ? selectedDeviceName  // full qualified
				: "gde.device." + selectedActiveDeviceConfig.getManufacturer().toLowerCase().replace(GDE.STRING_BLANK, GDE.STRING_EMPTY).replace(GDE.STRING_DASH, GDE.STRING_EMPTY) + "." + selectedDeviceName; //$NON-NLS-1$
		try {
			//String className = "gde.device.DefaultDeviceDialog";
			//log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class<?> c = loader.loadClass(className);
			//Class c = Class.forName(className);
			Constructor<?> constructor = c.getDeclaredConstructor(new Class[] { String.class });
			//log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {
				newInst = (IDevice) constructor.newInstance(new Object[] { selectedActiveDeviceConfig.getPropertiesFileName() });
			}
			else
				throw new NoClassDefFoundError(Messages.getString(MessageIds.GDE_MSGE0016));

		}
		catch (NoClassDefFoundError e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return newInst;
	}

	/**
	 * this will setup empty channels according the device properties file
	 * copied and modified from DeviceSelectionDialog.setupDataChannels();
	 * @param activeDevice (IDevice is the abstract type)
	 */
	protected void setupDataChannels(IDevice activeDevice) {
		// cleanup existing channels and record sets
		this.channels.cleanup();

		if (activeDevice != null) {
			String[] channelNames = new String[activeDevice.getChannelCount()];
			// buildup new structure  - set up the channels
			for (int i = 1; i <= activeDevice.getChannelCount(); i++) {
				Channel newChannel = new Channel(activeDevice.getChannelName(i), activeDevice.getChannelTypes(i));
				this.channels.put(i, newChannel);
				channelNames[i - 1] = i + " : " + activeDevice.getChannelName(i);
			}
			this.channels.setChannelNames(channelNames);
		}
	}

	/**
	 * method to draw the curves with it scales and defines the curve area
	 * copied and modified from GraphicsComposite.drawCurves()
	 * @param recordSet
	 * @param maxX
	 * @param maxY
	 */
	protected void drawCurves(RecordSet recordSet, int maxX, int maxY) {
		// get the image and prepare GC
		this.curveArea = SWTResourceManager.getImage(maxX, maxY);
		GC gc = this.curveAreaGC = SWTResourceManager.getGC(this.curveArea);
		Rectangle bounds = new Rectangle(0, 0, maxX, maxY);
			
		//prepare time scale
		double totalDisplayDeltaTime_ms = recordSet.get(0).getDrawTimeWidth_ms();
		int[] timeScale = this.timeLine.getScaleMaxTimeNumber(totalDisplayDeltaTime_ms);
		int maxTimeFormated = timeScale[0];
		int scaleFactor = timeScale[1];
		int timeFormat = timeScale[2];

		//calculate number of curve scales, left and right side
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (String recordKey : recordSet.getRecordNames()) {
			Record tmpRecord = recordSet.getRecord(recordKey);
			if (tmpRecord != null && tmpRecord.isVisible() && tmpRecord.isDisplayable()) {
				//System.out.println("==>> " + recordKey + " isVisible = " + tmpRecord.isVisible() + " isDisplayable = " + tmpRecord.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		//correct scales and scale position according compare set requirements
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = 1; //numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = 0; //numberCurvesRight > 0 && numberCurvesLeft == 0 ? 1 : 0;
		}
		//System.out.println("nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		//calculate the bounds left for the curves
		int dataScaleWidth; // horizontal space used for text and scales, numbers and caption
		int x0, y0; // the lower left corner of the curve area
		int xMax, yMax; // the upper right corner of the curve area
		int width; // x coordinate width	- time scale
		int height; // y coordinate - make modulo 10 ??
		int startTimeFormated, endTimeFormated;
		
		// calculate the horizontal space width to be used for the scales
		Point pt = gc.textExtent("-000,00"); //$NON-NLS-1$
		int horizontalGap = pt.x/5;
		int horizontalNumberExtend = pt.x;
		int horizontalCaptionExtend = pt.y;
		dataScaleWidth = recordSet.isCompareSet() ? horizontalNumberExtend + horizontalGap : horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;	
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;
		
		// calculate the horizontal area available for plotting graphs
		int gapSide = 10; // free gap left or right side of the curves
		x0 = spaceLeft + (numberCurvesLeft > 0 ? gapSide/2 : gapSide);// enable a small gap if no axis is shown
		xMax = bounds.width - spaceRight - (numberCurvesRight > 0 ? gapSide/2 : gapSide);
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);
		
		// calculate the vertical area available for plotting graphs
		yMax = 10; // free gap on top of the curves
		int gapBot = 3 * pt.y + 4; // space used for time scale text and scales with description or legend;
		y0 = bounds.height - yMax - gapBot;
		height = y0 - yMax; // recalculate due to modulo 10 ??
		//System.out.println("draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		// set offset values used for mouse measurement pointers
		this.offSetX = x0;
		this.offSetY = y0 - height;

		// draw curves for each active record
		this.curveAreaBounds = new Rectangle(x0, y0 - height, width, height);
		recordSet.setDrawAreaBounds(this.curveAreaBounds);
		//System.out.println("curve bounds = " + this.curveAreaBounds); //$NON-NLS-1$
		
		//gc.setBackground(this.curveAreaBackground);
		gc.fillRectangle(this.curveAreaBounds);
		//gc.setBackground(this.surroundingBackground);

		//draw the time scale
		//System.out.println("average time step record 0 = " + recordSet.getAverageTimeStep_ms());
		startTimeFormated = TimeLine.convertTimeInFormatNumber(recordSet.getStartTime(), timeFormat);
		endTimeFormated = startTimeFormated + maxTimeFormated;
		//System.out.println("startTime = " + startTimeFormated + " detaTime_ms = " + (int)totalDisplayDeltaTime_ms + " endTime = " + endTimeFormated);
		this.timeLine.drawTimeLine(recordSet, gc, x0, y0+1, width, startTimeFormated, endTimeFormated, scaleFactor, timeFormat, (long)totalDisplayDeltaTime_ms, DataExplorer.COLOR_BLACK);

		// draw draw area bounding 
		//gc.setForeground(this.curveAreaBorderColor);
		
		gc.drawLine(x0-1, yMax-1, xMax+1, yMax-1);
		gc.drawLine(x0-1, yMax-1, x0-1, y0); 
		gc.drawLine(xMax+1, yMax-1, xMax+1, y0);

		// check for activated time grid
		if (recordSet.getTimeGridType() > 0) 
			drawTimeGrid(recordSet, gc, this.curveAreaBounds, this.settings.getGridDashStyle());

		// check for activated horizontal grid
		boolean isCurveGridEnabled = recordSet.getHorizontalGridType() > 0;
		String curveGridRecordName = recordSet.getHorizontalGridRecordName();
		String[] recordNames = recordSet.getRecordNames().clone();
		// sort the record set names to get the one which makes the grid lines drawn first
		for (int i = 0; i < recordNames.length; i++) {
			if (recordNames[i].equals(curveGridRecordName)) {
				recordNames[i] = recordNames[0]; // exchange with record set at index 0
				recordNames[0] = curveGridRecordName; // replace with the one which makes the grid lines
				break;
			}
		}

		// draw each record using sorted record set names
		recordSet.updateSyncRecordScale();
		for (String record : recordNames) {
			Record actualRecord = recordSet.getRecord(record);
			boolean isActualRecordEnabled = actualRecord.isVisible() && actualRecord.isDisplayable() && actualRecord.realSize() > 0;
			if (actualRecord.isScaleVisible()) 
				CurveUtils.drawScale(actualRecord, gc, x0, y0, width, height, dataScaleWidth);

			if (isCurveGridEnabled && record.equals(curveGridRecordName)) // check for activated horizontal grid
				drawCurveGrid(recordSet, this.curveAreaGC, this.offSetY, width, this.settings.getGridDashStyle());

			if (isActualRecordEnabled) {
				//gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
				//gc.drawRectangle(x0, y0-height, width, height);
				gc.setClipping(x0-1, y0-height-1, width+2, height+2);
				CurveUtils.drawCurve(actualRecord, gc, x0, y0, width, height, recordSet.isCompareSet());
				gc.setClipping(bounds);
			}
		}

		// draw start time for zoom mode or scope mode
		if (startTimeFormated != 0) { 
			String strStartTime = Messages.getString(MessageIds.GDE_MSGT0255) + TimeLine.getFomatedTimeWithUnit(recordSet.getStartTime());
			Point point = gc.textExtent(strStartTime);
			int yPosition = (int) (y0 + pt.y * 2.5);
			gc.setForeground(SWTResourceManager.getColor(SWT.COLOR_DARK_RED));
			gc.drawText(strStartTime, 10, yPosition - point.y / 2);
		}
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during drawing of time scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param height
	 * @param dash to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, Rectangle bounds, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x - bounds.x, 0, x - bounds.x, bounds.height - 1);
		}
	}

	/**
	 * draw horizontal (curve) grid lines according the vector prepared during daring specified curve scale 
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param useOffsetY the offset in vertical direction
	 * @param width
	 * @param dash to be used for the custom line style
	 */
	private void drawCurveGrid(RecordSet recordSet, GC gc, int useOffSetY, int width, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getHorizontalGridColor());
		//curveAreaGC.setLineStyle(recordSet.getHorizontalGridLineStyle());
		Vector<Integer> horizontalGridVector = recordSet.getHorizontalGrid();
		for (int i = 0; i < horizontalGridVector.size() - 1; i += recordSet.getHorizontalGridType()) {
			int y = horizontalGridVector.get(i);
			gc.drawLine(0, y - useOffSetY, width - 1, y - useOffSetY);
		}
	}
	
	/**
	 * ger the path where the class GDE gets loaded
	 * @return
	 */
	protected String getLoaderPath() {
		return GDE.class.getProtectionDomain().getCodeSource().getLocation().getPath();
	}
	
	protected File setDataPath() {
		boolean settingsPropertiesExist = new File(this.settings.getSettingsFilePath()).exists();
		boolean isDataPathConfigured = new File(this.settings.getDataFilePath()).getPath() != GDE.FILE_SEPARATOR_UNIX;
		
		if (settingsPropertiesExist && isDataPathConfigured) {
			this.dataPath = new File(this.settings.getDataFilePath());
		}
		else {
			String srcDataPath = this.getLoaderPath().replace(GDE.FILE_SEPARATOR_WINDOWS, GDE.FILE_SEPARATOR_UNIX);
			if (srcDataPath.endsWith("bin/")) { // running inside eclipse
				srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf(GDE.NAME_LONG)) + "DataFilesTestSamples/" + GDE.NAME_LONG  ;
			}
			else {
				srcDataPath = srcDataPath.substring(0, srcDataPath.indexOf("build")) + "DataFilesTestSamples/" + GDE.NAME_LONG  ;
			}
			this.dataPath = new File(srcDataPath); ///usr/src/dataexplorer-2.23-src/build/target/<os_arch>/DataExplorer/DataExplorer.jar
		}
		
		this.settings.setDataFilePath(this.dataPath.getPath());
		System.out.println("this.devicePath = " + this.dataPath.getPath());
		return this.dataPath;
	}
}
