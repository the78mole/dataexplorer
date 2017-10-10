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

 Copyright (c) 2017 Thomas Eickert
****************************************************************************************/
package gde.histo.gpslocations;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import gde.GDE;
import gde.config.Settings;
import gde.histo.utils.GpsCoordinate;
import gde.log.Level;
import gde.utils.FileUtils;

/**
 * Gives access to geocode data based on the GPS geocode API.
 * @author Thomas Eickert
 */
public final class GeoCodes {
	final static String						$CLASS_NAME	= GeoCodes.class.getName();
	final static Logger						log					= Logger.getLogger($CLASS_NAME);

	private final static Settings	settings		= Settings.getInstance();

	/**
	 * @param gpsCoordinate
	 * @return an empty string or the formatted address of the GPS location based on the address type setting, e.g. 73441 Bopfingen, Germany
	 */
	public static String getLocation(GpsCoordinate gpsCoordinate) {
		String location = GDE.STRING_EMPTY;
		FileUtils.checkDirectoryAndCreate(settings.getHistoLocationsDirectory().toString());
		List<File> files;
		try {
			File geoFile = null;
			files = FileUtils.getFileListing(settings.getHistoLocationsDirectory().toFile(), 1);
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("%04d files found in locationsDir %s", files.size(), settings.getHistoLocationsDirectory().toString())); //$NON-NLS-1$
			{
				final Comparator<File> distanceComparison = (File f1, File f2) -> Double.compare(new GpsCoordinate(f1.getName()).getDistance(gpsCoordinate),
						(new GpsCoordinate(f2.getName()).getDistance(gpsCoordinate)));
				final Predicate<File> minDistanceFilter = f -> (new GpsCoordinate(f.getName())).getDistance(gpsCoordinate) < GeoCodes.settings.getGpsLocationRadius();
				File closestFile = files.parallelStream().filter(minDistanceFilter).min(distanceComparison).orElseGet(() -> GeoCodes.aquireGeoData(gpsCoordinate));
				geoFile = closestFile;
				if (log.isLoggable(Level.FINER)) log.log(Level.FINER, String.format("file from stream %s   file from loop %s", geoFile.getName(), closestFile.getName())); //$NON-NLS-1$
			}
			if (FileUtils.checkFileExist(geoFile.getPath()))
				location = getLocation(geoFile).replace("Unnamed Road, ", ""); //$NON-NLS-1$ //$NON-NLS-2$
			else
				log.log(Level.WARNING, "geoCode file not found"); //$NON-NLS-1$
		}
		catch (FileNotFoundException e) {
			log.log(Level.WARNING, e.getMessage(), e); //$NON-NLS-1$
		}
		return location;

	}

	/**
	 * @param file
	 * @return the formatted address of the GPS location based on the address type setting, e.g. 73441 Bopfingen, Germany
	 */
	public static String getLocation(File file) {
		String location = GDE.STRING_EMPTY;
		try (InputStream inputStream = new FileInputStream(file)) {
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
			XPath xpath = XPathFactory.newInstance().newXPath();
			//			XPathExpression expr = xpath.compile("/GeocodeResponse/result[3]/formatted_address"); // 1-based
			final XPathExpression expr = xpath.compile("/GeocodeResponse/result[type/text()='" + GeoCodes.settings.getGpsAddressType().name().toLowerCase() + "']/formatted_address"); //$NON-NLS-1$ //$NON-NLS-2$
			location = expr.evaluate(doc, XPathConstants.STRING).toString();
			log.log(Level.FINER, "1st try formatted_address=", location); //$NON-NLS-1$

			if (location.isEmpty()) {
				NodeList nodes = (NodeList) xpath.evaluate("/GeocodeResponse/result/formatted_address", doc, XPathConstants.NODESET); //$NON-NLS-1$
				for (int i = 0; i < nodes.getLength(); i++) {
					location = nodes.item(i).getTextContent();
					if (!location.isEmpty()) {
						if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "loop formatted_address=" + nodes.item(i).getTextContent()); //$NON-NLS-1$
						break;
					}
				}
			}
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage()); //$NON-NLS-1$
		}
		return location;
	}

	private static File aquireGeoData(GpsCoordinate gpsCoordinate) {
		long milliTime = System.currentTimeMillis();
		File geoData = settings.getHistoLocationsDirectory().resolve(gpsCoordinate.toAngularCoordinate()).toFile();
		FileUtils.checkDirectoryAndCreate(geoData.getParent().toString());
		try (InputStream inputStream = new URL(Settings.GPS_API_URL + gpsCoordinate.toCsvString()).openStream();
				ReadableByteChannel readableByteChannel = java.nio.channels.Channels.newChannel(inputStream);
				FileOutputStream fileOutputStream = new FileOutputStream(geoData)) {
			fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, 1 << 20); // limit 1 MB
			if (log.isLoggable(Level.FINER)) log.log(Level.FINER, "http read in " + (System.currentTimeMillis() - milliTime) + " ms!  gpsCoordinate=" + gpsCoordinate.toCsvString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (Exception e1) {
			log.log(Level.WARNING, "internet connection failed, check network capability if location data required"); //$NON-NLS-1$
		}
		return geoData;
	}

}
