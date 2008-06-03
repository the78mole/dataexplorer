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
package osde.ui;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;

/**
 * Class to manage SWT resources (Font, Color, Image and Cursor)
 * There are no restrictions on the use of this code.
 *
 * You may change this code and your changes will not be overwritten,
 * but if you change the version number below then this class will be
 * completely overwritten by Jigloo.
 * #SWTResourceManager:version4.0.0#
 */

/**
 * Initial created by Jigloo plug-in, add some methods to cache additional image data created
 * @author Winfried Brügmann
 */
public class SWTResourceManager {
	private static Logger log = Logger.getLogger(SWTResourceManager.class.getName());

	@SuppressWarnings("unchecked")
	private static HashMap resources = new HashMap();
	@SuppressWarnings("unchecked")
	static Vector users = new Vector();
	private static SWTResourceManager instance = new SWTResourceManager();

	private static DisposeListener disposeListener = new DisposeListener() {
		public void widgetDisposed(DisposeEvent e) {
			users.remove(e.getSource());
			if (users.size() == 0)
				dispose();
		}
	};

	/**
	 * This method should be called by *all* Widgets which use resources
	 * provided by this SWTResourceManager. When widgets are disposed,
	 * they are removed from the "users" Vector, and when no more
	 * registered Widgets are left, all resources are disposed.
	 * <P>
	 * If this method is not called for all Widgets then it should not be called
	 * at all, and the "dispose" method should be explicitly called after all
	 * resources are no longer being used.
	 */
	@SuppressWarnings("unchecked")
	public static void registerResourceUser(Widget widget) {
		if (users.contains(widget))
			return;
		users.add(widget);
		widget.addDisposeListener(disposeListener);
	}

	@SuppressWarnings("unchecked")
	public static void dispose() {
		Iterator it = resources.keySet().iterator();
		while (it.hasNext()) {
			Object resource = resources.get(it.next());
			if (resource instanceof Font)
				 ((Font) resource).dispose();
			else if (resource instanceof Color)
				 ((Color) resource).dispose();
			else if (resource instanceof Image)
				 ((Image) resource).dispose();
			else if (resource instanceof Cursor)
				 ((Cursor) resource).dispose();
		}
		resources.clear();
	}
	
	public static Font getFont(FontData fd) {
		return getFont(fd.getName(), fd.getHeight(), fd.getStyle(), false, false);
	}

	public static Font getFont(String name, int size, int style) {
		return getFont(name, size, style, false, false);
	}

	@SuppressWarnings("unchecked")
	public static Font getFont(String name, int size, int style, boolean strikeout, boolean underline) {
		String fontName = name + "|" + size + "|" + style + "|" + strikeout + "|" + underline;
		if (resources.containsKey(fontName))
			return (Font) resources.get(fontName);
		FontData fd = new FontData(name, size, style);
		if (strikeout || underline) {
			try {
				Class lfCls = Class.forName("org.eclipse.swt.internal.win32.LOGFONT");
				Object lf = FontData.class.getField("data").get(fd);
				if (lf != null && lfCls != null) {
					if (strikeout)
						lfCls.getField("lfStrikeOut").set(lf, new Byte((byte) 1));
					if (underline)
						lfCls.getField("lfUnderline").set(lf, new Byte((byte) 1));
				}
			} catch (Throwable e) {
				log.log(Level.SEVERE, e.getMessage(), e);
			}
		}
		Font font = new Font(Display.getDefault(), fd);
		if (log.isLoggable(Level.FINE)) log.fine("new font created = " + fontName);
		resources.put(fontName, font);
		return font;
	}

	public static Image getImage(String url, Control widget) {
		Image img = getImage(url);
		if(img != null && widget != null)
			img.setBackground(widget.getBackground());
		return img;
	}
	
	@SuppressWarnings("unchecked")
	public static Image getImage(int x, int y) {
		String key = "IMAGE:" + x + "_" + y;
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), x, y);
			if (log.isLoggable(Level.FINE)) log.fine("new image created = " + key);
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Image getImage(int x, int y, String imgKey) {
		String key = "IMAGE:" + x + "_" + y + "_" + imgKey;
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), x, y);
			if (log.isLoggable(Level.FINE)) log.fine("new image created = " + key);
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Image getRotatedImage(Image image, int style, String imgKey) {	
		Image resultImg = null;
		
		// Use the image's data to create a rotated image's data
		ImageData sd = image.getImageData();
		boolean up = (style & SWT.UP) == SWT.UP;
		String key = "IMAGE:" + sd.width + "_" + sd.height + "_" + style + "_" + imgKey;

		try {
			if (resources.containsKey(key)) {
				resultImg = (Image) resources.get(key);
			}
			else {
				ImageData dd = new ImageData(sd.height, sd.width, sd.depth, sd.palette);
				// Determine which way to rotate, depending on up or down
				int dx = 0, dy = 0;
				// Run through the horizontal pixels
				for (int sx = 0; sx < sd.width; sx++) {
					// Run through the vertical pixels
					for (int sy = 0; sy < sd.height; sy++) {
						// Determine where to move pixel to in destination image data
						dx = up ? sy : sd.height - sy - 1;
						dy = up ? sd.width - sx - 1 : sx;

						// Swap the x, y source data to y, x in the destination
						dd.setPixel(dx, dy, sd.getPixel(sx, sy));
					}
				}
				// Create the vertical image
				Image vertical = new Image(Display.getDefault(), dd);
				if (log.isLoggable(Level.FINE)) log.fine("new image created = " + key);
				resources.put(key, vertical);
				resultImg = vertical;
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		return resultImg;
	}

	@SuppressWarnings("unchecked")
	public static Image getImage(ImageData imageData) {
		//sd.height, sd.width, sd.depth, sd.palette
		String key = "IMAGE_DATA:" + imageData.height + "_" + imageData.width + "_" + imageData.depth;
		try {
			if (resources.containsKey(key))
				return (Image) resources.get(key);
			Image img = new Image(Display.getDefault(), imageData);
			if (log.isLoggable(Level.FINE)) log.fine("new image created = " + key);
			resources.put(key, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Image getImage(String url) {
		try {
			String tmpUrl = url.replace('\\', '/');
			if (tmpUrl.startsWith("/"))
				tmpUrl = tmpUrl.substring(1);
			if (resources.containsKey(tmpUrl))
				return (Image) resources.get(tmpUrl);
			Image img = new Image(Display.getDefault(), instance.getClass().getClassLoader().getResourceAsStream(tmpUrl));
			if (log.isLoggable(Level.FINE)) log.fine("new image created = " + tmpUrl);
			resources.put(tmpUrl, img);
			return img;
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static Color getColor(int swtColor) {
		String name = "COLOR:" + swtColor;
		if (resources.containsKey(name))
			return (Color) resources.get(name);
		Color color = Display.getDefault().getSystemColor(swtColor);
		if (log.isLoggable(Level.FINE)) log.fine("new color created = " + name);
		resources.put(name, color);
		return color;
	}

	@SuppressWarnings("unchecked")
	public static Color getColor(int red, int green, int blue) {
		String name = "COLOR:" + red + "," + green + "," + blue;
		if (resources.containsKey(name))
			return (Color) resources.get(name);
		Color color = new Color(Display.getDefault(), red, green, blue);
		if (log.isLoggable(Level.FINE)) log.fine("new color created = " + name);
		resources.put(name, color);
		return color;
	}

	@SuppressWarnings("unchecked")
	public static Pattern getPattern(float x1, float y1, float x2, float y2, int swtColor1, int alpha1, int swtColor2, int alpha2) {
		String name = "PATTERN:" + x1 + "," + y1 + "," + x2 + "," + y2 + swtColor1 + "," + alpha1 + "," + swtColor2 + "," + alpha2;
		if (resources.containsKey(name))
			return (Pattern) resources.get(name);
		Pattern pattern = new Pattern(Display.getDefault(), x1, y1, x2, y2, SWTResourceManager.getColor(swtColor1), alpha1, SWTResourceManager.getColor(swtColor2), alpha2);
		if (log.isLoggable(Level.FINE)) log.fine("new pattern created = " + name);
		resources.put(name, pattern);
		return pattern;
	}

	@SuppressWarnings("unchecked")
	public static Pattern getPattern(float x1, float y1, float x2, float y2, int swtColor1, int swtColor2) {
		String name = "PATTERN:" + x1 + "," + y1 + "," + x2 + "," + y2 + swtColor1 + "," + swtColor2;
		if (resources.containsKey(name))
			return (Pattern) resources.get(name);
		Pattern pattern = new Pattern(Display.getDefault(), x1, y1, x2, y2, SWTResourceManager.getColor(swtColor1), SWTResourceManager.getColor(swtColor2));
		if (log.isLoggable(Level.FINE)) log.fine("new pattern created = " + name);
		resources.put(name, pattern);
		return pattern;
	}

	@SuppressWarnings("unchecked")
	public static Cursor getCursor(int type) {
		String name = "CURSOR:" + type;
		if (resources.containsKey(name))
			return (Cursor) resources.get(name);
		Cursor cursor = new Cursor(Display.getDefault(), type);
		if (log.isLoggable(Level.FINE)) log.fine("new cursor created = " + name);
		resources.put(name, cursor);
		return cursor;
	}

	@SuppressWarnings("unchecked")
	public static Cursor getCursor(String url) {
		try {
			String tmpUrl = url.replace('\\', '/');
			if (tmpUrl.startsWith("/")) tmpUrl = tmpUrl.substring(1);
			if (resources.containsKey(tmpUrl)) return (Cursor) resources.get(tmpUrl);
			ImageData imgCur = new ImageData(instance.getClass().getClassLoader().getResourceAsStream(tmpUrl));
			Cursor cursor = new Cursor(Display.getDefault(), imgCur, imgCur.width/2, imgCur.height/2);
			if (log.isLoggable(Level.FINE)) log.fine("new cursor created = " + tmpUrl);
			resources.put(url, cursor);
			return cursor;
		}
		catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public static GC getGC(Image img) {
		String name = "GC_IMAGE:" + img.hashCode();
		if (resources.containsKey(name))
			return (GC) resources.get(name);
		GC gc = new GC(img);
		if (log.isLoggable(Level.FINE)) log.fine("new GC created = " + name);
		resources.put(name, gc);
		return gc;
	}

	@SuppressWarnings("unchecked")
	public static GC getGC(Display display) {
		String name = "GC_IMAGE:" + display.hashCode();
		if (resources.containsKey(name))
			return (GC) resources.get(name);
		GC gc = new GC(display);
		if (log.isLoggable(Level.FINE)) log.fine("new GC created = " + name);
		resources.put(name, gc);
		return gc;
	}
	
	@SuppressWarnings("unchecked")
	public static GC getGC(Canvas canvas, String descriptorKey) {
		String name = "GC_CANVAS:" + descriptorKey;
		if (resources.containsKey(name))
			return (GC) resources.get(name);
		GC gc = new GC(canvas);
		if (log.isLoggable(Level.FINE)) log.fine("new GC created = " + name);
		resources.put(name, gc);
		return gc;
	}

}
