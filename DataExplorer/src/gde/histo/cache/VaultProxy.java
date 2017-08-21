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

package gde.histo.cache;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.SchemaFactory;

import gde.config.Settings;

/**
 * Vaults IO operations with marshaller / unmarshaller caching.
 * @author Thomas Eickert
 */
public final class VaultProxy {

	private static Unmarshaller	jaxbUnmarshaller	= null;
	private static Marshaller		jaxbMarshaller		= null;

	public VaultProxy() {
		;
	}

	/**
	 * @return cached instance (unmarshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	public static Unmarshaller getUnmarshaller() {
		if (jaxbUnmarshaller == null) {
			final Path path = ExtendedVault.getCacheDirectory().resolve(Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			try {
				jaxbUnmarshaller = HistoVault.getJaxbContext().createUnmarshaller();
				jaxbUnmarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(path.toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return jaxbUnmarshaller;
	}

	/**
	 * @return cached instance (marshaller is not thread safe) which is ~100 ms faster than creating a new instance from a cached JaxbContext instance
	 */
	public static Marshaller getMarshaller() {
		if (jaxbMarshaller == null) {
			final Path path = ExtendedVault.getCacheDirectory().resolve(Settings.HISTO_CACHE_ENTRIES_XSD_NAME);
			try {
				jaxbMarshaller = HistoVault.getJaxbContext().createMarshaller();
				jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
				jaxbMarshaller.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(path.toFile()));
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return jaxbMarshaller;
	}

	/**
	 * @param fullQualifiedFileName is the vault path
	 * @return the vault or null
	 * @throws JAXBException
	 */
	public HistoVault load(Path fullQualifiedFileName) throws JAXBException {
		return (HistoVault) getUnmarshaller().unmarshal(fullQualifiedFileName.toFile());
	}

	/**
	 * @param inputStream is a stream to the source path
	 * @return the vault or null
	 * @throws JAXBException
	 */
	public HistoVault load(InputStream inputStream) throws JAXBException {
		return (HistoVault) getUnmarshaller().unmarshal(inputStream);
	}

	/**
	 * @param newVault is the vault to be stored
	 * @param fullQualifiedFileName is the vault path
	 * @throws JAXBException
	 */
	public void store(HistoVault newVault, Path fullQualifiedFileName) throws JAXBException {
		getMarshaller().marshal(this, fullQualifiedFileName.toFile());
	}

	/**
	 * @param newVault is the vault to be stored
	 * @param outputStream is a stream to the target path
	 * @throws JAXBException
	 */
	public void store(HistoVault newVault, OutputStream outputStream) throws JAXBException {
		getMarshaller().marshal(newVault, outputStream);
	}

}