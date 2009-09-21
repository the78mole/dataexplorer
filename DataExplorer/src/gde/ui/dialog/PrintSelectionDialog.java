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
package osde.ui.dialog;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import osde.OSDE;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;

/**
 * simple print configuration dialog
 * @author Winfried Brügmann
 */
public class PrintSelectionDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger log = Logger.getLogger(PrintSelectionDialog.class.getName());

	Shell dialogShell;
	Button okButton;
	Group configurationGroup;
	Button portraitButton;
	private Button cancelButton;
	Button objectButton;
	Button statisticsButton;
	Button graphicsButton;
	Group orientationGroup;
	Button landscapeButton;
	
	final OpenSerialDataExplorer application;


	/**
	* Auto-generated main method to display this 
	* org.eclipse.swt.widgets.Dialog inside a new Shell.
	*/
	public static void main(String[] args) {
		try {
			Display display = Display.getDefault();
			Shell shell = new Shell(display);
			PrintSelectionDialog inst = new PrintSelectionDialog(shell, SWT.NULL);
			inst.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PrintSelectionDialog(Shell parent, int style) {
		super(parent, style);
		this.application = OpenSerialDataExplorer.getInstance();
	}

	public void open() {
		try {
			Shell parent = getParent();
			dialogShell = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
			SWTResourceManager.registerResourceUser(dialogShell);
			dialogShell.setLayout(null);
			dialogShell.setImage(SWTResourceManager.getImage("osde/resource/Print.gif"));
			dialogShell.setText("Print configuration");
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.setSize(350, 250);
			{
				configurationGroup = new Group(dialogShell, SWT.NONE);
				configurationGroup.setLayout(null);
				configurationGroup.setText("selection");
				configurationGroup.setBounds(5, 10, 160, 155);
				configurationGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "configurationGroup.paintControl, event="+evt);
						boolean isObjectOriented = PrintSelectionDialog.this.application.isObjectoriented();
						objectButton.setEnabled(isObjectOriented);
						objectButton.setSelection(isObjectOriented);
					}
				});
				{
					graphicsButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					graphicsButton.setText("graphics");
					graphicsButton.setImage(SWTResourceManager.getImage("osde/resource/Graphics.gif"));
					graphicsButton.setSelection(true);
					graphicsButton.setBounds(8, 16, 143, 45);
				}
				{
					statisticsButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					statisticsButton.setText("statistics");
					statisticsButton.setImage(SWTResourceManager.getImage("osde/resource/Statistics.gif"));
					statisticsButton.setSelection(true);
					statisticsButton.setBounds(8, 61, 143, 45);
				}
				{
					objectButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					objectButton.setText("object");
					objectButton.setImage(SWTResourceManager.getImage("osde/resource/Object.gif"));
					objectButton.setBounds(8, 106, 143, 45);
				}
			}
			{
				orientationGroup = new Group(dialogShell, SWT.NONE);
				orientationGroup.setLayout(null);
				orientationGroup.setBounds(175, 10, 160, 155);
				orientationGroup.setText("orientation");
				{
					portraitButton = new Button(orientationGroup, SWT.RADIO | SWT.LEFT);
					portraitButton.setText("portrait");
					portraitButton.setImage(SWTResourceManager.getImage("osde/resource/Portrait.gif"));
					portraitButton.setSelection(true);
					portraitButton.setBounds(8, 16, 145, 70);
					portraitButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "portraitButton.widgetSelected, event="+evt);
							portraitButton.setSelection(true);
							landscapeButton.setSelection(false);
						}
					});
				}
				{
					landscapeButton = new Button(orientationGroup, SWT.RADIO | SWT.LEFT);
					landscapeButton.setText("landscape");
					landscapeButton.setImage(SWTResourceManager.getImage("osde/resource/Landscape.gif"));
					landscapeButton.setBounds(8, 83, 145, 70);
					landscapeButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "landscapeButton.widgetSelected, event="+evt);
							portraitButton.setSelection(false);
							landscapeButton.setSelection(true);
						}
					});
				}
			}
			{
				okButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				okButton.setText("print");
				okButton.setBounds(174, 178, 149, 30);
				okButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "okButton.widgetSelected, event="+evt);
						final boolean isLandscape = landscapeButton.getSelection();
						final boolean isGraphics = graphicsButton.getSelection();
						final boolean isStatistics = statisticsButton.getSelection();
						final boolean isObject = objectButton.getSelection();
						OpenSerialDataExplorer.display.asyncExec(new Runnable() {
							public void run() {
						    initiatePrinting(isLandscape, isGraphics, isStatistics, isObject);
							}
						});
						dialogShell.dispose();
					}
				});
			}
			{
				cancelButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				cancelButton.setText("cancel");
				cancelButton.setBounds(14, 178, 149, 30);
				cancelButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "cancelButton.widgetSelected, event="+evt);
						dialogShell.dispose();
					}
				});
			}
			dialogShell.setLocation(getParent().toDisplay(100, 100));
			dialogShell.open();
			Display display = dialogShell.getDisplay();
			while (!dialogShell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
	}

	void initiatePrinting(boolean isLandscape, boolean isGraphics, boolean isStatistics, boolean isObject) {
		// Show the Choose Printer dialog
		PrintDialog dialog = new PrintDialog(OpenSerialDataExplorer.shell, SWT.NULL);
		PrinterData printerData = dialog.open();

		if (printerData != null) {
			// Create the printer object
			Printer printer = new Printer(printerData);

			// Calculate the scale factor between the screen resolution and printer
			// resolution in order to correctly size the image for the printer
			Point screenDPI = Display.getCurrent().getDPI();
			Point printerDPI = printer.getDPI();

			// Determine the bounds of the entire area of the printer
			Rectangle trim = printer.computeTrim(0, 0, 0, 0);
			Rectangle clientArea = printer.getClientArea();
			Rectangle bounds = printer.getBounds();
			log.log(Level.INFO, "trim = " + trim);
			log.log(Level.INFO, "clientArea = " + clientArea);
			log.log(Level.INFO, "bounds = " + bounds);
			Rectangle printBounds = new Rectangle(-trim.x, -trim.y, clientArea.width-(trim.width), clientArea.height-(trim.height)); 
			log.log(Level.INFO, "printBounds = " + printBounds);

			double scaleFactor = printerDPI.x / screenDPI.x;

			// Start the print job
			if (isLandscape) {
				
			}
			else { // is portrait
				if (printer.startJob(OSDE.OSDE_NAME_LONG)) {
					if (isGraphics && printer.startPage()) {
						GC gc = new GC(printer);
						gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
						//gc.drawRectangle(printBounds);
						Point pt = drawHeader(printBounds, gc, "Graphics");

						Image graphicsImage = this.application.getGraphicsAsImage();
						ImageData graphicsImageData = graphicsImage.getImageData();
						Image graphicsPrinterImage = new Image(printer, graphicsImageData);
						scaleFactor = 1.0 * printBounds.width / graphicsImageData.width;
						gc.drawImage(graphicsPrinterImage, 0, 0, graphicsImageData.width, graphicsImageData.height, 
							printBounds.x, printBounds.y + pt.y + 20, (int) (scaleFactor * graphicsImageData.width), (int) (scaleFactor * graphicsImageData.height));
						graphicsPrinterImage.dispose();
						graphicsImage.dispose();
						isGraphics = false;

						if (isStatistics) {
							gc.setFont(SWTResourceManager.getFont("Lucida Console", 30, SWT.NORMAL));
							String statistics = this.application.getStatisticsAsText();
							statistics = statistics.substring(statistics.indexOf(OSDE.LINE_SEPARATOR));
							gc.drawText(statistics, printBounds.x, printBounds.y + printBounds.height / 2);
							isStatistics = false;
						}
						else if (isObject) {
							Image objectImage = this.application.getObjectContentAsImage();
							ImageData objectImageData = objectImage.getImageData();
							Image objectPrinterImage = new Image(printer, objectImageData);
							scaleFactor = 1.0 * printBounds.width / objectImageData.width;
							gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height, 
								printBounds.x, printBounds.y + printBounds.height / 2, (int) (scaleFactor * objectImageData.width),	(int) (scaleFactor * objectImageData.height));
							objectPrinterImage.dispose();
							objectImage.dispose();
							isObject = false;
						}

						gc.dispose();
						printer.endPage();
					}
					if (isStatistics && printer.startPage()) {
						GC gc = new GC(printer);
						gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
						Point pt = drawHeader(printBounds, gc, "Statistics");

						gc.setFont(SWTResourceManager.getFont("Lucida Console", 30, SWT.NORMAL));
						String statistics = this.application.getStatisticsAsText();
						statistics = statistics.substring(statistics.indexOf(OSDE.LINE_SEPARATOR));
						gc.drawText(statistics, printBounds.x, printBounds.y + pt.y + 20);
						isStatistics = false;

						if (isObject) {
							Image objectImage = this.application.getObjectContentAsImage();
							ImageData objectImageData = objectImage.getImageData();
							Image objectPrinterImage = new Image(printer, objectImageData);
							scaleFactor = 1.0 * printBounds.width / objectImageData.width;
							gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height, 
								printBounds.x, printBounds.y + printBounds.height / 2, (int) (scaleFactor * objectImageData.width),	(int) (scaleFactor * objectImageData.height));
							objectPrinterImage.dispose();
							objectImage.dispose();
							isObject = false;
						}

						gc.dispose();
						printer.endPage();
					}
					if (isObject && printer.startPage()) {
						GC gc = new GC(printer, SWT.IMAGE_JPEG);
						gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
						Point pt = drawHeader(printBounds, gc, "Object characteristics");

						Image objectImage = this.application.getObjectContentAsImage();
						ImageData objectImageData = objectImage.getImageData();
						Image objectPrinterImage = new Image(printer, objectImageData);
						scaleFactor = 1.0 * printBounds.width / objectImageData.width;
						gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height, 
							printBounds.x, printBounds.y + pt.y + 20, (int)(scaleFactor * objectImageData.width),	(int)(scaleFactor * objectImageData.height));
						objectPrinterImage.dispose();
						objectImage.dispose();
						isObject = false;

						gc.dispose();
						printer.endPage();
					}
				}
			}
			// End the job and dispose the printer
			printer.endJob();
			printer.dispose();
		}
	}

	Point drawHeader(Rectangle printBounds, GC gc, String type) {
		gc.drawText(OSDE.OSDE_NAME_LONG + OSDE.STRING_MESSAGE_CONCAT + type, printBounds.x, printBounds.y);
		String date = new Date().toString();
		Point pt = gc.textExtent(date); // date string dimensions
		gc.drawText(date, printBounds.width-pt.x, printBounds.y);
		return pt;
	}
	
	BufferedImage transformImage(ImageData data) {
		DirectColorModel colorModel = new DirectColorModel(data.depth, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000);

		BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
		if (data.getTransparencyType() == SWT.TRANSPARENCY_MASK) {
			ImageData alphaMask = data.getTransparencyMask();
			PaletteData palette = data.palette;
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[4];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);

					RGB rgb = palette.getRGB(pixel);

					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					int bit = y * data.width + x;
					byte transparencyMask = alphaMask.data[bit / 8];
					int mask = 1;
					mask = mask << (7 - (bit % 8));
					if ((mask & transparencyMask) == mask) {
						pixelArray[3] = 255;
					}
					else
						pixelArray[3] = 0;

					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
		}
		return bufferedImage;
	}
	
	static BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if (palette.isDirect) {
			colorModel = new DirectColorModel(data.depth, palette.redMask, palette.greenMask, palette.blueMask);
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					pixelArray[0] = rgb.red;
					pixelArray[1] = rgb.green;
					pixelArray[2] = rgb.blue;
					raster.setPixels(x, y, 1, 1, pixelArray);
				}
			}
			return bufferedImage;
		}
		else {
			RGB[] rgbs = palette.getRGBs();
			byte[] red = new byte[rgbs.length];
			byte[] green = new byte[rgbs.length];
			byte[] blue = new byte[rgbs.length];
			for (int i = 0; i < rgbs.length; i++) {
				RGB rgb = rgbs[i];
				red[i] = (byte) rgb.red;
				green[i] = (byte) rgb.green;
				blue[i] = (byte) rgb.blue;
			}
			if (data.transparentPixel != -1) {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
			}
			else {
				colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
			}
			BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					pixelArray[0] = pixel;
					raster.setPixel(x, y, pixelArray);
				}
			}
			return bufferedImage;
		}
	}

  static ImageData convertToSWT(BufferedImage bufferedImage) {
    if (bufferedImage.getColorModel() instanceof DirectColorModel) {
      DirectColorModel colorModel = (DirectColorModel) bufferedImage
          .getColorModel();
      PaletteData palette = new PaletteData(colorModel.getRedMask(),
          colorModel.getGreenMask(), colorModel.getBlueMask());
      ImageData data = new ImageData(bufferedImage.getWidth(),
          bufferedImage.getHeight(), colorModel.getPixelSize(),
          palette);
      WritableRaster raster = bufferedImage.getRaster();
      int[] pixelArray = new int[3];
      for (int y = 0; y < data.height; y++) {
        for (int x = 0; x < data.width; x++) {
          raster.getPixel(x, y, pixelArray);
          int pixel = palette.getPixel(new RGB(pixelArray[0],
              pixelArray[1], pixelArray[2]));
          data.setPixel(x, y, pixel);
        }
      }
      return data;
    } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
      IndexColorModel colorModel = (IndexColorModel) bufferedImage
          .getColorModel();
      int size = colorModel.getMapSize();
      byte[] reds = new byte[size];
      byte[] greens = new byte[size];
      byte[] blues = new byte[size];
      colorModel.getReds(reds);
      colorModel.getGreens(greens);
      colorModel.getBlues(blues);
      RGB[] rgbs = new RGB[size];
      for (int i = 0; i < rgbs.length; i++) {
        rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
            blues[i] & 0xFF);
      }
      PaletteData palette = new PaletteData(rgbs);
      ImageData data = new ImageData(bufferedImage.getWidth(),
          bufferedImage.getHeight(), colorModel.getPixelSize(),
          palette);
      data.transparentPixel = colorModel.getTransparentPixel();
      WritableRaster raster = bufferedImage.getRaster();
      int[] pixelArray = new int[1];
      for (int y = 0; y < data.height; y++) {
        for (int x = 0; x < data.width; x++) {
          raster.getPixel(x, y, pixelArray);
          data.setPixel(x, y, pixelArray[0]);
        }
      }
      return data;
    }
    return null;
  }
}
