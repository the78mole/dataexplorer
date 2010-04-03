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
package osde.ui.tab;

import java.text.DecimalFormat;
import osde.log.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;

import osde.DE;
import osde.config.Settings;
import osde.data.Channel;
import osde.data.Channels;
import osde.data.Record;
import osde.data.RecordSet;
import osde.device.IDevice;
import osde.ui.DataExplorer;
import osde.ui.SWTResourceManager;
import osde.ui.menu.TabAreaContextMenu;
import osde.utils.GraphicsUtils;
import osde.utils.MathUtils;

/**
 * Child display class displaying analog active measurements
 * @author Winfried Brügmann
 */
public class AnalogDisplay extends Composite {
	final static Logger			log								= Logger.getLogger(AnalogDisplay.class.getName());

	Canvas					tacho;
	CLabel					textAnalogLabel;
	final int				textHeight				= 30;

	Image						tachoImage;
	GC							tachoImageGC;
	Rectangle				tachoImageBounds	= new Rectangle(0, 0, 0, 0);
	int[] 					needle = {0,0,0,0,0,0,0};

	int							width							= 0;
	int							height						= 0;
	int							centerX;
	int							centerY;
	int							radius;
	int							angleStart;
	int							angleDelta;

	double					actualValue				= 0.0;
	double					minValue					= 0.0;
	double					maxValue					= 1.0;

	final DataExplorer	application;
	final Channel									channel;
	final RecordSet								recordSet;
	final Record									record;
	final String									recordKey;
	final IDevice									device;
	
	final Color										backgroundColor;
	final Menu										popupmenu;
	final TabAreaContextMenu			contextMenu;

	/**
	 * 
	 */
	public AnalogDisplay(Composite analogMainComposite, String currentRecordKey, IDevice currentDevice) {
		super(analogMainComposite, SWT.BORDER);
		FillLayout AnalogDisplayLayout = new FillLayout(org.eclipse.swt.SWT.HORIZONTAL);
		GridData analogDisplayLData = new GridData();
		analogDisplayLData.grabExcessVerticalSpace = true;
		analogDisplayLData.grabExcessHorizontalSpace = true;
		analogDisplayLData.verticalAlignment = GridData.FILL;
		analogDisplayLData.horizontalAlignment = GridData.FILL;
		this.setLayoutData(analogDisplayLData);
		this.setLayout(AnalogDisplayLayout);		
		this.application = DataExplorer.getInstance();
		this.recordKey = currentRecordKey;
		this.device = currentDevice;
		this.channel = Channels.getInstance().getActiveChannel();
		if (this.channel != null) {
			this.recordSet = this.channel.getActiveRecordSet();
			if (this.recordSet != null) {
				this.record = this.recordSet.get(this.recordKey);
			}
			else {
				this.record = null;
			}
		}	
		else {
			this.recordSet = null;
			this.record = null;
		}
		
		this.backgroundColor = Settings.getInstance().getAnalogInnerAreaBackground();
		this.popupmenu = new Menu(this.application.getShell(), SWT.POP_UP);
		this.contextMenu = new TabAreaContextMenu();
		this.contextMenu.createMenu(this.popupmenu, TabAreaContextMenu.TYPE_SIMPLE);
	}

	public void create() {
		this.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent evt) {
				log.log(Level.FINER, "AnalogDisplay.helpRequested " + evt); //$NON-NLS-1$
				DataExplorer.getInstance().openHelpDialog("", "HelpInfo_8.html"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		});
		this.tacho = new Canvas(this, SWT.NONE);
		this.tacho.setMenu(this.popupmenu);
		this.tacho.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent evt) {
				tachoPaintControl(evt);
			}
		});
		this.textAnalogLabel = new CLabel(this.tacho, SWT.CENTER);
		this.textAnalogLabel.setFont(SWTResourceManager.getFont(this.application, 14, SWT.BOLD)); //$NON-NLS-1$
		this.textAnalogLabel.setBackground(this.backgroundColor);
		this.textAnalogLabel.setForeground(DataExplorer.COLOR_BLACK);
		this.textAnalogLabel.setBounds(0, 0, this.tacho.getSize().x, this.textHeight);
		this.textAnalogLabel.setMenu(this.popupmenu);
	}

	void tachoPaintControl(PaintEvent evt) {
		log.log(Level.FINEST, "tacho.paintControl, event=" + evt); //$NON-NLS-1$
		if (this.record != null) {
			log.log(Level.FINER, "record name = " + this.recordKey); //$NON-NLS-1$

			// Get the canvas and its dimensions to check if size changed
			this.tachoImageBounds = ((Canvas) evt.widget).getClientArea();

			// get min max values and check if this has been changed
			double tmpMinValue = record.isScaleSynced() ? record.getParent().get(record.getParent().getSyncableName()).getMinValue() : this.record.getMinValue();
			tmpMinValue = this.device.translateValue(this.record, tmpMinValue / 1000.0);
			double tmpMaxValue = record.isScaleSynced() ? record.getParent().get(record.getParent().getSyncableName()).getMaxValue() : this.record.getMaxValue();
			tmpMaxValue = this.device.translateValue(this.record, tmpMaxValue / 1000.0);
			double deltaScale = tmpMaxValue - tmpMinValue;
			tmpMinValue = MathUtils.roundDown(tmpMinValue, deltaScale);
			tmpMaxValue = MathUtils.roundUp(tmpMaxValue, deltaScale);
			if (tmpMinValue != this.minValue || tmpMaxValue != this.maxValue) {
				this.minValue = tmpMinValue;
				this.maxValue = tmpMaxValue;
				redraw(this.tachoImageBounds.x, this.tachoImageBounds.y, this.tachoImageBounds.width, this.tachoImageBounds.height, true);
			}

			// draw new tacho
			log.log(Level.FINE, "tacho redaw required for " + this.recordKey); //$NON-NLS-1$
			this.width = this.tachoImageBounds.width;
			this.height = this.tachoImageBounds.height;
			log.log(Level.FINER, "canvas size = " + this.width + " x " + this.height); //$NON-NLS-1$ //$NON-NLS-2$
			// get the image and prepare GC
			this.tachoImage = SWTResourceManager.getImage(this.width, this.height, this.recordKey);
			this.tachoImageGC = SWTResourceManager.getGC(this.tachoImage);
			//clear image with background color
			this.tachoImageGC.setBackground(this.backgroundColor);
			this.tachoImageGC.fillRectangle(0, 0, this.width, this.height);
			String recordText = this.recordKey + " [ " + this.record.getUnit() + " ]"; //$NON-NLS-1$ //$NON-NLS-2$
			this.textAnalogLabel.setSize(this.width, this.textHeight);
			this.textAnalogLabel.setText(recordText);
			this.centerX = this.width / 2;
			this.centerY = (int) (this.height * 0.75);
			double radiusW = (this.width / 2 * 0.80);
			//int radiusH = (int) (this.height / 2 * 0.90);
			double radiusH = this.height * 0.75 - this.textHeight - 40;
			//log.log(Level.FINE, "radiusH = " + radiusH + " radiusLimitH = " + radiusLimitH);
			//radiusH = radiusH < radiusLimitH ? radiusH : radiusLimitH;
			this.radius = (int)(radiusW < radiusH ? radiusW : radiusH);
			log.log(Level.FINER, "radius = " + this.radius + " height = " + this.height + " width = " + this.width);
			this.angleStart = -20;
			this.angleDelta = 220;
			this.tachoImageGC.setForeground(this.record.getColor());
			this.tachoImageGC.setLineWidth(4);
			this.tachoImageGC.drawArc(this.centerX - this.radius, this.centerY - this.radius, 2 * this.radius, 2 * this.radius, this.angleStart, this.angleDelta);
			this.tachoImageGC.setForeground(DataExplorer.COLOR_BLACK);
			this.tachoImageGC.setFont(SWTResourceManager.getFont(DE.WIDGET_FONT_NAME, DE.WIDGET_FONT_SIZE, SWT.NORMAL));
			int numberTicks = 10; //new Double(maxValue - minValue).intValue();
			double deltaValue = (this.maxValue - this.minValue) / numberTicks;
			double angleSteps = this.angleDelta * 1.0 / numberTicks;
			int tickRadius = this.radius + 2;
			int dxr, dxtick, dyr, dytick, dxtext, dytext;
			this.tachoImageGC.setLineWidth(2);
			DecimalFormat df = record.isScaleSynced() ? record.getParent().get(record.getParent().getSyncableName()).getDecimalFormat() : record.getDecimalFormat();
			for (int i = 0; i <= numberTicks; ++i) {
				double angle = this.angleStart + i * angleSteps; // -20, 0, 20, 40, ...
				dxr = Double.valueOf(tickRadius * Math.cos(angle * Math.PI / 180)).intValue();
				dyr = Double.valueOf(tickRadius * Math.sin(angle * Math.PI / 180)).intValue();
				dxtick = Double.valueOf((tickRadius + 10) * Math.cos(angle * Math.PI / 180)).intValue();
				dytick = Double.valueOf((tickRadius + 10) * Math.sin(angle * Math.PI / 180)).intValue();
				this.tachoImageGC.drawLine(this.centerX - dxtick, this.centerY - dytick, this.centerX - dxr, this.centerY - dyr);

				dxtext = Double.valueOf((this.radius + 30) * Math.cos(angle * Math.PI / 180)).intValue();
				dytext = Double.valueOf((this.radius + 30) * Math.sin(angle * Math.PI / 180)).intValue();
				String valueText = df.format(this.minValue + (i * deltaValue));
				log.log(Level.FINE, "value = " + valueText);
				GraphicsUtils.drawTextCentered(valueText, this.centerX - dxtext, this.centerY - dytext, this.tachoImageGC, SWT.HORIZONTAL);
			}
			// center knob
			this.tachoImageGC.setBackground(DataExplorer.COLOR_GREY);
			int knobRradius = (int) (this.radius * 0.1);
			this.tachoImageGC.fillArc(this.centerX - knobRradius, this.centerY - knobRradius, 2 * knobRradius, 2 * knobRradius, 0, 360);
			this.tachoImageGC.setBackground(DataExplorer.COLOR_BLACK);
			knobRradius = Double.valueOf(this.radius / 10 * 0.2).intValue();
			this.tachoImageGC.fillArc(this.centerX - knobRradius, this.centerY - knobRradius, 2 * knobRradius, 2 * knobRradius, 0, 360);

			evt.gc.drawImage(this.tachoImage, 0, 0, this.width, this.height, 0, 0, this.width, this.height);

			//draw the new needle if required
			Rectangle damageBounds = getNeedleBounds();
			double tmpActualValue = this.device.translateValue(this.record, (this.record.lastElement() / 1000.0));
			log.log(Level.FINE, String.format("value = %3.2f; min = %3.2f; max = %3.2f", this.actualValue, this.minValue, this.maxValue)); //$NON-NLS-1$
			if (tmpActualValue != this.actualValue) {
				this.actualValue = tmpActualValue;
				damageBounds = getNeedleBounds();
				redraw(damageBounds.x, damageBounds.y, damageBounds.width, damageBounds.height, true);
			}

			drawTachoNeedle(evt.gc, DataExplorer.COLOR_BLACK);
		}
	}

	/**
	 * draw tacho needle in specified color, before new needle is drawn is must be erased using the background color
	 * - the needle polygon has to be updated prior to call this method
	 * @param gc
	 * @param color
	 */
	private void drawTachoNeedle(GC gc, Color color) {
		gc.setBackground(color);
		gc.setLineWidth(1);
		gc.fillPolygon(this.needle);
	}

	/**
	 * calculate the tacho needle ploygon using the actual measurement value
	 */
	private void calculateNeedle() {
		int needleRadius = this.radius - 5;
		int innerRadius = (int) (this.radius * 0.1) + 3;
		double angle = this.angleStart + (this.actualValue - this.minValue) / (this.maxValue - this.minValue) * this.angleDelta;
		log.log(Level.FINE, "angle = " + angle + " actualValue = " + this.actualValue); //$NON-NLS-1$ //$NON-NLS-2$

		int posXo = Double.valueOf(this.centerX - (needleRadius * Math.cos(angle * Math.PI / 180))).intValue();
		int posYo = Double.valueOf(this.centerY - (needleRadius * Math.sin(angle * Math.PI / 180))).intValue();
		int posXi = Double.valueOf(this.centerX - (innerRadius * Math.cos(angle * Math.PI / 180))).intValue();
		int posYi = Double.valueOf(this.centerY - (innerRadius * Math.sin(angle * Math.PI / 180))).intValue();
		int posX1 = Double.valueOf(this.centerX - ((needleRadius - 30) * Math.cos((angle - 3) * Math.PI / 180))).intValue();
		int posY1 = Double.valueOf(this.centerY - ((needleRadius - 30) * Math.sin((angle - 3) * Math.PI / 180))).intValue();
		int posX2 = Double.valueOf(this.centerX - ((needleRadius - 30) * Math.cos((angle + 3) * Math.PI / 180))).intValue();
		int posY2 = Double.valueOf(this.centerY - ((needleRadius - 30) * Math.sin((angle + 3) * Math.PI / 180))).intValue();
		this.needle = new int[] { posX1, posY1, posXo, posYo, posX2, posY2, posXi, posYi };
	}

	/**
	 * updates the needle polygon and calculates the actual needle bounds to enable redraw of exact this area
	 * @return rectangle bounds of the area where the needle is drawn 
	 */
	Rectangle getNeedleBounds() {
		calculateNeedle(); // make this actual
		
		int x = this.needle[0], xWidth = this.needle[0];
		int y = this.needle[1], yHeight = this.needle[1];
		
		for (int i = 0; i < this.needle.length; i+=2) {
			x = x < this.needle[i] ? x : this.needle[i];
			xWidth = xWidth > this.needle[i] ? xWidth : this.needle[i];
		}
		for (int i = 1; i < this.needle.length; i+=2) {
			y = y < this.needle[i] ? y : this.needle[i];
			yHeight = yHeight > this.needle[i] ? yHeight : this.needle[i];
		}
		
		return new Rectangle(x, y, xWidth-x, yHeight-y);
	}
	
	/**
	 * updates the tacho needle if position has been changed
	 * - this may initiate redraw of the whole tacho if scale values are changed
	 */
	public void checkTachoNeedlePosition() {
		double tmpActualValue = this.device.translateValue(this.record, (this.record.lastElement() / 1000.0));
		log.log(Level.FINE, String.format("value = %3.2f; min = %3.2f; max = %3.2f", this.actualValue, this.minValue, this.maxValue)); //$NON-NLS-1$
		if (tmpActualValue != this.actualValue) {
			Rectangle damageBounds = getNeedleBounds(); 
			redraw(damageBounds.x, damageBounds.y, damageBounds.width, damageBounds.height, true);
		}
	}
}
