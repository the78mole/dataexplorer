/**
 * 
 */
package osde.junit;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
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

import osde.OSDE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.DeviceConfiguration;
import osde.device.IDevice;
import osde.log.LogFormatter;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.CurveUtils;
import osde.utils.TimeLine;

/**
 * @author brueg
 *
 */
public class TestSuperClass extends TestCase {
	Logger																rootLogger;

	final OpenSerialDataExplorer					application	= OpenSerialDataExplorer.getInstance();
	final Channels												channels		= Channels.getInstance();
	final Settings												settings		= Settings.getInstance();
	final String 													tmpDir 			= System.getProperty("java.io.tmpdir");


	final TimeLine												timeLine		= new TimeLine();
	Image																	curveArea;
	GC																		curveAreaGC;
	Rectangle															curveAreaBounds;
	GC																		canvasGC;
	int																		offSetX, offSetY;

	TreeMap<String, DeviceConfiguration>	deviceConfigurations;
	Vector<String>												activeDevices;
	File																	devicePath;

	Handler																ch					= new ConsoleHandler();
	LogFormatter													lf					= new LogFormatter();

	Logger																logger1			= Logger.getLogger("osde.data.Record");
	Logger																logger2			= Logger.getLogger("osde.data.RecordSet");

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
		this.ch.setLevel(Level.FINE);

		//this.logger1.setLevel(Level.FINE);
		//this.logger1.setUseParentHandlers(true);
		//this.logger2.setLevel(Level.FINE);
		//this.logger2.setUseParentHandlers(true);

		Thread.currentThread().setContextClassLoader(OSDE.getClassLoader());

		this.initialize();

		Canvas canvas = new Canvas(new Composite(Display.getDefault().getShells()[0], SWT.NONE), SWT.NONE);
		this.canvasGC = SWTResourceManager.getGC(canvas, "curveArea_" + 0); //$NON-NLS-1$

		//this.devicePath = new File(this.tmpDir + "Write_0_OSD"); 
		this.devicePath = new File(this.settings.getDataFilePath());
		//this.devicePath = new File(this.settings.getDataFilePath() + OSDE.FILE_SEPARATOR + "UniLog");
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
		this.activeDevices = new Vector<String>(2, 1);

		for (int i = 0; files != null && i < files.length; i++) {
			try {
				// loop through all device properties XML and check if device used
				if (files[i].endsWith(".xml")) {
					String deviceKey = files[i].substring(0, files[i].length() - 4);
					devConfig = new DeviceConfiguration(this.settings.getDevicesPath() + OSDE.FILE_SEPARATOR + files[i]);

					// store all device configurations in a map					
					String keyString;
					if (devConfig.getName() != null)
						keyString = devConfig.getName();
					else {
						devConfig.setName(deviceKey);
						keyString = deviceKey;
					}
					//log.log(Level.FINE, deviceKey + OSDE.STRING_MESSAGE_CONCAT + keyString);
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
	@SuppressWarnings("unchecked")
	protected IDevice getInstanceOfDevice(DeviceConfiguration selectedActiveDeviceConfig) {
		IDevice newInst = null;
		String selectedDeviceName = selectedActiveDeviceConfig.getName().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY);
		//selectedDeviceName = selectedDeviceName.substring(0, 1).toUpperCase() + selectedDeviceName.substring(1);
		String className = "osde.device." + selectedActiveDeviceConfig.getManufacturer().toLowerCase().replace(OSDE.STRING_BLANK, OSDE.STRING_EMPTY).replace(OSDE.STRING_DASH, OSDE.STRING_EMPTY) + "." + selectedDeviceName; //$NON-NLS-1$
		try {
			//String className = "osde.device.DefaultDeviceDialog";
			//log.log(Level.FINE, "loading Class " + className); //$NON-NLS-1$
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			Class c = loader.loadClass(className);
			//Class c = Class.forName(className);
			Constructor constructor = c.getDeclaredConstructor(new Class[] { String.class });
			//log.log(Level.FINE, "constructor != null -> " + (constructor != null ? "true" : "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (constructor != null) {
				newInst = (IDevice) constructor.newInstance(new Object[] { selectedActiveDeviceConfig.getPropertiesFileName() });
			}
			else
				throw new NoClassDefFoundError(Messages.getString(MessageIds.OSDE_MSGE0016));

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
				Channel newChannel = new Channel(i, activeDevice.getChannelName(i), activeDevice.getChannelType(i));
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
		int[] timeScale = this.timeLine.getScaleMaxTimeNumber(recordSet);
		int maxTimeFormated = timeScale[0];
		int scaleFactor = timeScale[1];
		int timeFormat = timeScale[2];
		int maxTime_ms = timeScale[3];

		//prepare measurement scales
		int numberCurvesRight = 0;
		int numberCurvesLeft = 0;
		for (String recordKey : recordSet.getRecordNames()) {
			Record tmpRecord = recordSet.getRecord(recordKey);
			if (tmpRecord.isVisible() && tmpRecord.isDisplayable()) {
				//log.log(Level.FINE, "==>> " + recordKey + " isVisible = " + tmpRecord.isVisible() + " isDisplayable = " + tmpRecord.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (tmpRecord.isPositionLeft())
					numberCurvesLeft++;
				else
					numberCurvesRight++;
			}
		}
		// correct scales and scale position according synced scales requirements
		if (recordSet.isSyncableSynced()) {
			for (String recordKey : recordSet.getSyncableRecords()) {
				Record tmpRecord = recordSet.getRecord(recordKey);
				if (tmpRecord.isVisible() && tmpRecord.isDisplayable()) {
					//log.log(Level.FINE, "==>> " + recordKey + " isVisible = " + tmpRecord.isVisible() + " isDisplayable = " + tmpRecord.isDisplayable()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (tmpRecord.isPositionLeft())
						numberCurvesLeft--;
					else
						numberCurvesRight--;
				}
			}
			if (recordSet.get(recordSet.getSyncableName()).isPositionLeft())
				numberCurvesLeft++;
			else
				numberCurvesRight++;
		}
		// correct scales and scale position according compare set requirements
		if (recordSet.isCompareSet()) {
			numberCurvesLeft = numberCurvesLeft > 0 ? 1 : 0;
			numberCurvesRight = numberCurvesRight > 0 && numberCurvesLeft == 0 ? 1 : 0;
		}
		//log.log(Level.FINE, "nCurveLeft=" + numberCurvesLeft + ", nCurveRight=" + numberCurvesRight); //$NON-NLS-1$ //$NON-NLS-2$

		int dataScaleWidth; // space used for text and scales with description or legend
		int x0; // enable a small gap if no axis is shown
		int width; // make the time width  the width for the curves
		int y0;
		int height; // make modulo 20
		// draw x coordinate	- time scale
		int startTimeFormated, endTimeFormated;
		// Calculate the horizontal area to used for plotting graphs
		Point pt = this.canvasGC.textExtent("000,00"); //$NON-NLS-1$
		int horizontalGap = pt.x / 5;
		int horizontalNumberExtend = pt.x;
		int horizontalCaptionExtend = pt.y;
		dataScaleWidth = horizontalNumberExtend + horizontalCaptionExtend + horizontalGap;
		int spaceLeft = numberCurvesLeft * dataScaleWidth;
		int spaceRight = numberCurvesRight * dataScaleWidth;
		x0 = maxX - (maxX - spaceLeft) + 5;
		int xMax = maxX - spaceRight;
		width = ((xMax - x0) <= 0) ? 1 : (xMax - x0);
		xMax = x0 + width;
		int verticalSpace = 3 * pt.y;// space used for time scale text and scales with description or legend
		int spaceTop = 20;
		int spaceBot = verticalSpace;
		y0 = maxY - spaceBot;
		int yMax = maxY - (maxY - spaceTop);
		height = ((y0 - yMax) - (y0 - yMax) % 10) <= 0 ? 1 : (y0 - yMax) - (y0 - yMax) % 10;
		yMax = y0 - height;
		//log.log(Level.FINE, "draw area x0=" + x0 + ", y0=" + y0 + ", xMax=" + xMax + ", yMax=" + yMax + ", width=" + width + ", height=" + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		// draw curves for each active record
		recordSet.setDrawAreaBounds(new Rectangle(x0, y0 - height, width, height));
		//log.log(Level.FINE, "curve bounds = " + x0 + " " + (y0 - height) + " " + width + " " + height); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		startTimeFormated = TimeLine.convertTimeInFormatNumber(recordSet.getStartTime(), timeFormat);
		endTimeFormated = startTimeFormated + maxTimeFormated;
		this.timeLine.drawTimeLine(recordSet, this.canvasGC, x0, y0, width, startTimeFormated, endTimeFormated, scaleFactor, timeFormat, (maxTime_ms - TimeLine.convertTimeInFormatNumber(recordSet
				.getStartTime(), TimeLine.TIME_LINE_MSEC)), OpenSerialDataExplorer.COLOR_BLACK);

		// get the image and prepare GC
		this.curveArea = SWTResourceManager.getImage(width, height);
		this.curveAreaGC = SWTResourceManager.getGC(this.curveArea);
		this.curveAreaBounds = this.curveArea.getBounds();

		// clear the image
		this.curveAreaGC.setBackground(this.canvasGC.getBackground());
		this.curveAreaGC.fillRectangle(this.curveArea.getBounds());

		// draw draw area bounding 
		if (System.getProperty("os.name").toLowerCase().startsWith("windows")) //$NON-NLS-1$ //$NON-NLS-2$
			this.curveAreaGC.setForeground(OpenSerialDataExplorer.COLOR_LIGHT_GREY);
		else
			this.curveAreaGC.setForeground(OpenSerialDataExplorer.COLOR_GREY);
		this.curveAreaGC.drawLine(0, 0, width, 0);
		this.curveAreaGC.drawLine(0, 0, 0, height - 1);
		this.curveAreaGC.drawLine(width - 1, 0, width - 1, height - 1);

		// prepare grid lines
		this.offSetX = x0;
		this.offSetY = y0 - height;
		int[] dash = Settings.getInstance().getGridDashStyle();

		// check for activated time grid
		if (recordSet.getTimeGridType() > 0) drawTimeGrid(recordSet, this.curveAreaGC, this.offSetX, height, dash);

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

		if (recordSet.isSyncableSynced()) {
			CurveUtils.drawScale(recordSet.get(recordSet.getSyncableName()), this.canvasGC, x0, y0, width, height, dataScaleWidth);
			recordSet.updateSyncedScaleValues();
		}
		// draw each record using sorted record set names
		for (String record : recordSet.getNoneCalculationRecordNames()) {
			Record actualRecord = recordSet.getRecord(record);
			boolean isActualRecordEnabled = true;
			//log.log(Level.FINE, "drawing record = " + actualRecord.getName() + " isVisibel=" + actualRecord.isVisible() + " isDisplayable=" + actualRecord.isDisplayable() + " isScaleSynced=" + actualRecord.isScaleSynced()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (isActualRecordEnabled && !actualRecord.isScaleSynced()) CurveUtils.drawScale(actualRecord, this.canvasGC, x0, y0, width, height, dataScaleWidth);

			if (isCurveGridEnabled && record.equals(curveGridRecordName)) // check for activated horizontal grid
				drawCurveGrid(recordSet, this.curveAreaGC, this.offSetY, width, dash);

			if (isActualRecordEnabled) {
				//System.out.println("drawing record = " + record);
				CurveUtils.drawCurve(actualRecord, this.curveAreaGC, 0, height, width, height, recordSet.isCompareSet(), recordSet.isZoomMode());
			}
		}

		this.canvasGC.drawImage(this.curveArea, this.offSetX, this.offSetY);

		if (startTimeFormated != 0) { // scaled window 
			String strStartTime = Messages.getString(MessageIds.OSDE_MSGT0255) + TimeLine.getFomatedTimeWithUnit(recordSet.getStartTime());
			Point point = this.canvasGC.textExtent(strStartTime);
			int yPosition = (int) (y0 + pt.y * 2.5);
			this.canvasGC.drawText(strStartTime, 10, yPosition - point.y / 2);
		}
	}

	/**
	 * draw vertical (time) grid lines according the vector defined during drawing of time scale
	 * @param recordSet
	 * @param gc the graphics context to be used
	 * @param height
	 * @param dash to be used for the custom line style
	 */
	public void drawTimeGrid(RecordSet recordSet, GC gc, int useOffSetX, int height, int[] dash) {
		gc.setLineWidth(1);
		gc.setLineDash(dash);
		gc.setLineStyle(SWT.LINE_CUSTOM);
		gc.setForeground(recordSet.getColorTimeGrid());
		for (Integer x : recordSet.getTimeGrid()) {
			gc.drawLine(x - useOffSetX, 0, x - useOffSetX, height - 1);
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
}
