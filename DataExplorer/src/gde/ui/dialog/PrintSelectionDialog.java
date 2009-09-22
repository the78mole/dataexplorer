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
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import osde.OSDE;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.RecordSet;
import osde.messages.MessageIds;
import osde.messages.Messages;
import osde.ui.OpenSerialDataExplorer;
import osde.ui.SWTResourceManager;
import osde.utils.StringHelper;

/**
 * simple print configuration dialog
 * @author Winfried Brügmann
 */
public class PrintSelectionDialog extends org.eclipse.swt.widgets.Dialog {
	final static Logger log = Logger.getLogger(PrintSelectionDialog.class.getName());

	Shell dialogShell;
	Button printButton;
	Group configurationGroup;
	Button portraitButton;
	Button landscapeReverseButton;
	Button cancelButton;
	Button objectButton;
	Button statisticsButton;
	Button graphicsButton;
	Button curveCompareButton;
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
			dialogShell.setText("Print layout configuration");
			dialogShell.layout();
			dialogShell.pack();			
			dialogShell.setSize(400, 320);
			{
				configurationGroup = new Group(dialogShell, SWT.NONE);
				configurationGroup.setLayout(null);
				configurationGroup.setText("selection");
				configurationGroup.setBounds(7, 10, 168, 206);
				configurationGroup.addPaintListener(new PaintListener() {
					public void paintControl(PaintEvent evt) {
						log.log(Level.FINEST, "configurationGroup.paintControl, event="+evt);
						Channel activeChannel = Channels.getInstance().getActiveChannel();
						boolean isRecordSetGraphicsPrintable = false;
						if(activeChannel != null) {
							RecordSet activeRecordSet = activeChannel.getActiveRecordSet();
							if(activeRecordSet != null)
								isRecordSetGraphicsPrintable = activeRecordSet.size() > 0;
						}
						graphicsButton.setEnabled(isRecordSetGraphicsPrintable);
						graphicsButton.setSelection(isRecordSetGraphicsPrintable);
						statisticsButton.setEnabled(isRecordSetGraphicsPrintable);
						statisticsButton.setSelection(isRecordSetGraphicsPrintable);

						
						boolean isObjectOriented = PrintSelectionDialog.this.application.isObjectoriented();
						objectButton.setEnabled(isObjectOriented);
						objectButton.setSelection(isObjectOriented);

						boolean isCopareWindowPrintable = PrintSelectionDialog.this.application.getCompareSet().size() > 0;
						curveCompareButton.setEnabled(isCopareWindowPrintable);
						curveCompareButton.setSelection(isCopareWindowPrintable);
					}
				});
				{
					graphicsButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					graphicsButton.setText("graphics");
					graphicsButton.setImage(SWTResourceManager.getImage("osde/resource/Graphics.gif"));
					graphicsButton.setSelection(true);
					graphicsButton.setBounds(8, 16, 148, 45);
				}
				{
					statisticsButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					statisticsButton.setText("statistics");
					statisticsButton.setImage(SWTResourceManager.getImage("osde/resource/Statistics.gif"));
					statisticsButton.setSelection(true);
					statisticsButton.setBounds(8, 62, 148, 45);
				}
				{
					objectButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					objectButton.setText("object");
					objectButton.setImage(SWTResourceManager.getImage("osde/resource/Object.gif"));
					objectButton.setBounds(8, 108, 148, 45);
				}
				{
					curveCompareButton = new Button(configurationGroup, SWT.CHECK | SWT.LEFT);
					curveCompareButton.setText("compare");
					curveCompareButton.setImage(SWTResourceManager.getImage("osde/resource/Graphics.gif"));
					curveCompareButton.setSelection(false);
					curveCompareButton.setBounds(8, 155, 148, 45);
				}
			}
			{
				orientationGroup = new Group(dialogShell, SWT.NONE);
				orientationGroup.setLayout(null);
				orientationGroup.setBounds(181, 10, 203, 206);
				orientationGroup.setText("orientation");
				{
					portraitButton = new Button(orientationGroup, SWT.RADIO | SWT.LEFT);
					portraitButton.setText("Portrait");
					portraitButton.setToolTipText("if possible will print more then one selection on one page");
					portraitButton.setImage(SWTResourceManager.getImage("osde/resource/Portrait.gif"));
					portraitButton.setSelection(true);
					portraitButton.setBounds(8, 16, 183, 65);
					portraitButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "portraitButton.widgetSelected, event="+evt);
							portraitButton.setSelection(true);
							landscapeReverseButton.setSelection(false);
							landscapeButton.setSelection(false);
						}
					});
				}
				{
					landscapeButton = new Button(orientationGroup, SWT.RADIO | SWT.LEFT);
					landscapeButton.setText("Landscape");
					landscapeButton.setToolTipText("this will print one selection on one page");
					landscapeButton.setImage(SWTResourceManager.getImage("osde/resource/Landscape.gif"));
					landscapeButton.setSelection(false);
					landscapeButton.setBounds(8, 74, 183, 65);
					landscapeButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "landscapeButton.widgetSelected, event="+evt);
							portraitButton.setSelection(false);
							landscapeReverseButton.setSelection(false);
							landscapeButton.setSelection(true);
						}
					});
				}
				{
					landscapeReverseButton = new Button(orientationGroup, SWT.RADIO | SWT.LEFT);
					landscapeReverseButton.setText("Landscape(rev)");
					landscapeReverseButton.setToolTipText("this will print all selections on one large (height) page\nusable for PDF printer");
					landscapeReverseButton.setImage(SWTResourceManager.getImage("osde/resource/LandscapeReverse.gif"));
					landscapeReverseButton.setSelection(false);
					landscapeReverseButton.setBounds(8, 134, 183, 65);
					landscapeReverseButton.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent evt) {
							log.log(Level.FINEST, "portraitAllButton.widgetSelected, event="+evt);
							landscapeReverseButton.setSelection(true);
							portraitButton.setSelection(false);
							landscapeButton.setSelection(false);
						}
					});
				}
			}
			{
				printButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				printButton.setText("print");
				printButton.setBounds(212, 241, 149, 30);
				printButton.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent evt) {
						log.log(Level.FINEST, "okButton.widgetSelected, event="+evt);
						final int orientation = landscapeButton.getSelection() ? PageFormat.REVERSE_LANDSCAPE 
								: portraitButton.getSelection() ? PageFormat.PORTRAIT
								: PageFormat.LANDSCAPE;
						final boolean isGraphics = graphicsButton.getSelection();
						final boolean isCompare = curveCompareButton.getSelection();
						final boolean isStatistics = statisticsButton.getSelection();
						final boolean isObject = objectButton.getSelection();
				    initiatePrinting(orientation, isGraphics, isCompare, isStatistics, isObject);
						dialogShell.dispose();
					}
				});
			}
			{
				cancelButton = new Button(dialogShell, SWT.PUSH | SWT.CENTER);
				cancelButton.setText("cancel");
				cancelButton.setBounds(29, 241, 149, 30);
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

	void initiatePrinting(final int orientation, final boolean isGraphics, final boolean isCompare, final boolean isStatistics, final boolean isObject) {

		org.eclipse.swt.graphics.Image graphicsImageSWT, compareImageSWT, statisticsImageSWT, objectImageSWT;
		final java.awt.Image graphicsImageAWT, compareImageAWT, statisticsImageAWT, objectImageAWT;
		int selectionIndex = this.application.getTabSelectionIndex();

		//get all required images
		if (isGraphics) {
			this.application.selectTab(0);
			graphicsImageAWT = convertToAWT((graphicsImageSWT = this.application.getGraphicsAsImage()).getImageData());
			graphicsImageSWT.dispose();
		}
		else
			graphicsImageAWT = null;

		if (isStatistics) {
			this.application.selectTab(1);
			statisticsImageAWT = convertToAWT((statisticsImageSWT = this.application.getStatisticsAsImage()).getImageData());
			statisticsImageSWT.dispose();
		}
		else
			statisticsImageAWT = null;

		if (this.application.isObjectoriented() && isObject) {
			this.application.selectTab(8);
			objectImageAWT = convertToAWT((objectImageSWT = this.application.getObjectContentAsImage()).getImageData());
			objectImageSWT.dispose();
		}
		else
			objectImageAWT = null;

		if (isCompare) {
			this.application.selectTab(6);
			compareImageAWT = convertToAWT((compareImageSWT = this.application.getGraphicsAsImage()).getImageData());
			compareImageSWT.dispose();
		}
		else
			compareImageAWT = null;
		
		this.application.selectTab(selectionIndex);

		Thread printThread = new Thread() {
			@Override
			public void run() {
				PrinterJob printJob = PrinterJob.getPrinterJob();
				Book book = new Book();
				PageFormat documentPageFormat = new PageFormat();

				printJob.setJobName(System.getProperty("user.name") + OSDE.STRING_MESSAGE_CONCAT + OSDE.OSDE_NAME_LONG);
				printJob.setPageable(book);

				// show the choose printer dialog
				if (printJob.printDialog()) {
					if (orientation == PageFormat.REVERSE_LANDSCAPE) {
						documentPageFormat.setOrientation(PageFormat.REVERSE_LANDSCAPE);
						if (isGraphics) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0143), graphicsImageAWT), documentPageFormat);
						if (isStatistics) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0350), statisticsImageAWT), documentPageFormat);
						if (isObject) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0403), objectImageAWT), documentPageFormat);
						if (isCompare) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0144), compareImageAWT), documentPageFormat);
					}
					else if (orientation == PageFormat.LANDSCAPE) {
						documentPageFormat.setOrientation(PageFormat.LANDSCAPE);
						if (isGraphics) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0143), graphicsImageAWT), documentPageFormat);
						if (isStatistics) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0350), statisticsImageAWT), documentPageFormat);
						if (isObject) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0403), objectImageAWT), documentPageFormat);
						if (isCompare) book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0144), compareImageAWT), documentPageFormat);
					}
					else if (orientation == PageFormat.PORTRAIT) {
						documentPageFormat.setOrientation(PageFormat.PORTRAIT);
						boolean isGraphicsToBePrinted = isGraphics;
						boolean isStatisticsToBePrinted = isStatistics;
						boolean isObjectToBePrinted = isObject;
						boolean isCompareToBePrinted = isCompare;

						if (isGraphicsToBePrinted && isStatisticsToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0143), graphicsImageAWT, Messages.getString(MessageIds.OSDE_MSGT0350), statisticsImageAWT), documentPageFormat);
							isGraphicsToBePrinted = isStatisticsToBePrinted = false;
						}
						else if (isGraphicsToBePrinted && isObjectToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0143), graphicsImageAWT, Messages.getString(MessageIds.OSDE_MSGT0403), objectImageAWT), documentPageFormat);
							isGraphicsToBePrinted = isObjectToBePrinted = false;
						}
						else if (isGraphicsToBePrinted && isObjectToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0143), graphicsImageAWT, Messages.getString(MessageIds.OSDE_MSGT0144), compareImageAWT), documentPageFormat);
							isGraphicsToBePrinted = isObjectToBePrinted = false;
						}
						else if (isGraphicsToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0143), graphicsImageAWT), documentPageFormat);
							isGraphicsToBePrinted = false;
						}

						if (isStatisticsToBePrinted && isObjectToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0350), statisticsImageAWT, Messages.getString(MessageIds.OSDE_MSGT0403), objectImageAWT), documentPageFormat);
							isStatisticsToBePrinted = isObjectToBePrinted = false;
						}
						else if (isStatisticsToBePrinted && isCompareToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0350), statisticsImageAWT, Messages.getString(MessageIds.OSDE_MSGT0144), compareImageAWT), documentPageFormat);
							isStatisticsToBePrinted = isCompareToBePrinted = false;
						}
						else if (isStatisticsToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0350), statisticsImageAWT), documentPageFormat);
							isStatisticsToBePrinted = false;
						}

						if (isObjectToBePrinted && isCompareToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0403), objectImageAWT, Messages.getString(MessageIds.OSDE_MSGT0144), compareImageAWT), documentPageFormat);
							isObjectToBePrinted = isCompareToBePrinted = false;
						}
						else if (isObjectToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0403), objectImageAWT), documentPageFormat);
							isObjectToBePrinted = false;
						}
						else if (isCompareToBePrinted) {
							book.append(new Document(Messages.getString(MessageIds.OSDE_MSGT0144), compareImageAWT), documentPageFormat);
							isCompareToBePrinted = false;
						}
					}
					try {
						printJob.print();
					}
					catch (Exception e) {
						log.log(Level.SEVERE, e.getMessage(), e);
						PrintSelectionDialog.this.application.openMessageDialog(OpenSerialDataExplorer.shell, e.getClass().getSimpleName() + OSDE.STRING_MESSAGE_CONCAT + e.getMessage());
					}
				}
			}
		};
		printThread.start();
	}
		
  /**
   * This class is the painter for the document content. 
   * Depending of the image data loaded it will paint graphics, statistics or object characteristics
   */
  class Document extends Component implements Printable {
		private static final long	serialVersionUID	= 1L;
		
		final Image awtBufferedImage1, awtBufferedImage2;
		final String docType1, docType2;
		
		Document(String documentType, Image awtImage) {
			super();
			this.docType1 = documentType;
			this.awtBufferedImage1 = awtImage;
			this.docType2 = "";
			this.awtBufferedImage2 = null;
		}

		Document(String documentType1, Image awtImage1, String documentType2, Image awtImage2) {
			super();
			this.docType1 = documentType1;
			this.awtBufferedImage1 = awtImage1;
			this.docType2 = documentType2;
			this.awtBufferedImage2 = awtImage2;
		}

		/**
     * implementation of the print method
     * @param g the graphics context
     * @param pageFormat
     * @param page
     * @return 
     */
    public int print(Graphics g, PageFormat pageFormat, int page) {

      Graphics2D g2d = (Graphics2D) g;
      g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY()); // set the origin to 0,0 for the top left corner
      g2d.setPaint(Color.black);
      g2d.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 10));
      String date = StringHelper.getDate();
      Rectangle2D rectDate = g2d.getFontMetrics().getStringBounds(date, g2d);

//      log.log(Level.INFO, "this.awtBufferedImage.getWidth(this) = " + this.awtBufferedImage1.getWidth(this));
//      log.log(Level.INFO, "this.awtBufferedImage.getHeight(this) = " + this.awtBufferedImage1.getHeight(this));
//      log.log(Level.INFO, "pageFormat.getImageableWidth() = " + pageFormat.getImageableWidth());
//      log.log(Level.INFO, "pageFormat.getImageableHeight() = " + pageFormat.getImageableHeight());
//      log.log(Level.INFO, "pageFormat.getWidth() = " + pageFormat.getWidth());
//      log.log(Level.INFO, "pageFormat.getHeight() = " + pageFormat.getHeight());
//      log.log(Level.INFO, "pageFormat.getImageableX() = " + pageFormat.getImageableX());
//      log.log(Level.INFO, "pageFormat.getImageableY() = " + pageFormat.getImageableY());
      
      double scaleFactor1 = pageFormat.getImageableWidth() / this.awtBufferedImage1.getWidth(this);
      g2d.drawString(OSDE.OSDE_NAME_LONG + OSDE.STRING_MESSAGE_CONCAT + docType1, 0, 10);
      g2d.drawString(date, (int)(pageFormat.getImageableWidth()-rectDate.getWidth()), 10);
      g2d.drawImage(awtBufferedImage1, 0, 20, (int)pageFormat.getImageableWidth(), (int)(scaleFactor1*awtBufferedImage1.getHeight(this)), this);

      if (this.awtBufferedImage2 != null) {
        double scaleFactor2 = pageFormat.getImageableWidth() / this.awtBufferedImage2.getWidth(this);
        g2d.drawString(OSDE.OSDE_NAME_LONG + OSDE.STRING_MESSAGE_CONCAT + docType2, 0, (int) (scaleFactor1 * awtBufferedImage1.getHeight(this) + 40));
        g2d.drawString(date, (int)(pageFormat.getImageableWidth()-rectDate.getWidth()), (int) (scaleFactor1 * awtBufferedImage1.getHeight(this) + 40));
				g2d.drawImage(awtBufferedImage2, 0, (int) (scaleFactor1 * awtBufferedImage1.getHeight(this) + 50), (int) pageFormat.getImageableWidth(), (int) (scaleFactor2 * awtBufferedImage2.getHeight(this)), this);
			}
			return (PAGE_EXISTS);
    }
  }


	BufferedImage convertToAWT(ImageData data) {
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
	
//////// backup from pure SWT printing where color problems exist while printing jpeg images (Blaustich) //////////
//	// Show the Choose Printer dialog
//	PrintDialog dialog = new PrintDialog(OpenSerialDataExplorer.shell, SWT.NULL);
//	PrinterData printerData = dialog.open();
//
//	if (printerData != null) {
//		// Create the printer object
//		Printer printer = new Printer(printerData);
//
//		// Calculate the scale factor between the screen resolution and printer
//		// resolution in order to correctly size the image for the printer
//		Point screenDPI = Display.getCurrent().getDPI();
//		Point printerDPI = printer.getDPI();
//
//		// Determine the bounds of the entire area of the printer
//		Rectangle trim = printer.computeTrim(0, 0, 0, 0);
//		Rectangle clientArea = printer.getClientArea();
//		Rectangle bounds = printer.getBounds();
//		log.log(Level.INFO, "trim = " + trim);
//		log.log(Level.INFO, "clientArea = " + clientArea);
//		log.log(Level.INFO, "bounds = " + bounds);
//		Rectangle printBounds = new Rectangle(-trim.x, -trim.y, clientArea.width-(trim.width), clientArea.height-(trim.height)); 
//		log.log(Level.INFO, "printBounds = " + printBounds);
//
//		double scaleFactor = printerDPI.x / screenDPI.x;
//
//		// Start the print job
//		if (isLandscape) {
//			
//		}
//		else { // is portrait
//			if (printer.startJob(OSDE.OSDE_NAME_LONG)) {
//				if (isGraphics && printer.startPage()) {
//					GC gc = new GC(printer);
//					gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
//					//gc.drawRectangle(printBounds);
//					Point pt = drawHeader(printBounds, gc, "Graphics");
//
//					Image graphicsImage = this.application.getGraphicsAsImage();
//					ImageData graphicsImageData = graphicsImage.getImageData();
//					Image graphicsPrinterImage = new Image(printer, graphicsImageData);
//					scaleFactor = 1.0 * printBounds.width / graphicsImageData.width;
//					gc.drawImage(graphicsPrinterImage, 0, 0, graphicsImageData.width, graphicsImageData.height, 
//						printBounds.x, printBounds.y + pt.y + 20, (int) (scaleFactor * graphicsImageData.width), (int) (scaleFactor * graphicsImageData.height));
//					graphicsPrinterImage.dispose();
//					graphicsImage.dispose();
//					isGraphics = false;
//
//					if (isStatistics) {
//						gc.setFont(SWTResourceManager.getFont("Lucida Console", 30, SWT.NORMAL));
//						String statistics = this.application.getStatisticsAsText();
//						statistics = statistics.substring(statistics.indexOf(OSDE.LINE_SEPARATOR));
//						gc.drawText(statistics, printBounds.x, printBounds.y + printBounds.height / 2);
//						isStatistics = false;
//					}
//					else if (isObject) {
//						Image objectImage = this.application.getObjectContentAsImage();
//						ImageData objectImageData = objectImage.getImageData();
//						Image objectPrinterImage = new Image(printer, objectImageData);
//						scaleFactor = 1.0 * printBounds.width / objectImageData.width;
//						gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height, 
//							printBounds.x, printBounds.y + printBounds.height / 2, (int) (scaleFactor * objectImageData.width),	(int) (scaleFactor * objectImageData.height));
//						objectPrinterImage.dispose();
//						objectImage.dispose();
//						isObject = false;
//					}
//
//					gc.dispose();
//					printer.endPage();
//				}
//				if (isStatistics && printer.startPage()) {
//					GC gc = new GC(printer);
//					gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
//					Point pt = drawHeader(printBounds, gc, "Statistics");
//
//					gc.setFont(SWTResourceManager.getFont("Lucida Console", 30, SWT.NORMAL));
//					String statistics = this.application.getStatisticsAsText();
//					statistics = statistics.substring(statistics.indexOf(OSDE.LINE_SEPARATOR));
//					gc.drawText(statistics, printBounds.x, printBounds.y + pt.y + 20);
//					isStatistics = false;
//
//					if (isObject) {
//						Image objectImage = this.application.getObjectContentAsImage();
//						ImageData objectImageData = objectImage.getImageData();
//						Image objectPrinterImage = new Image(printer, objectImageData);
//						scaleFactor = 1.0 * printBounds.width / objectImageData.width;
//						gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height, 
//							printBounds.x, printBounds.y + printBounds.height / 2, (int) (scaleFactor * objectImageData.width),	(int) (scaleFactor * objectImageData.height));
//						objectPrinterImage.dispose();
//						objectImage.dispose();
//						isObject = false;
//					}
//
//					gc.dispose();
//					printer.endPage();
//				}
//				if (isObject && printer.startPage()) {
//					GC gc = new GC(printer, SWT.IMAGE_JPEG);
//					gc.setFont(SWTResourceManager.getFont(this.application, 50, SWT.NORMAL));
//					Point pt = drawHeader(printBounds, gc, "Object characteristics");
//
//					Image objectImage = this.application.getObjectContentAsImage();
//					ImageData objectImageData = objectImage.getImageData();
//					Image objectPrinterImage = new Image(printer, objectImageData);
//					scaleFactor = 1.0 * printBounds.width / objectImageData.width;
//					gc.drawImage(objectPrinterImage, 0, 0, objectImageData.width, objectImageData.height, 
//						printBounds.x, printBounds.y + pt.y + 20, (int)(scaleFactor * objectImageData.width),	(int)(scaleFactor * objectImageData.height));
//					objectPrinterImage.dispose();
//					objectImage.dispose();
//					isObject = false;
//
//					gc.dispose();
//					printer.endPage();
//				}
//			}
//		}
//		// End the job and dispose the printer
//		printer.endJob();
//		printer.dispose();

//	Point drawHeader(Rectangle printBounds, GC gc, String type) {
//		gc.drawText(OSDE.OSDE_NAME_LONG + OSDE.STRING_MESSAGE_CONCAT + type, printBounds.x, printBounds.y);
//		String date = StringHelper.getDate();
//		Point pt = gc.textExtent(date); // date string dimensions
//		gc.drawText(date, printBounds.width-pt.x, printBounds.y);
//		return pt;
//	}
	

}
