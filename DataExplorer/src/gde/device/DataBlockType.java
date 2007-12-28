/**************************************************************************************
  	This file is part of OpenSerialdataExplorer.

    OpenSerialdataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialdataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialdataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/
package osde.device;

import org.w3c.dom.Element;

import osde.utils.XMLUtils;

/**
 * @author Winfried Brügmann
 * DataBlockType class represents one element in the DeviceConfiguration DOM tree, refer to DeviceProperties style sheet
 */
public class DataBlockType {
	private int						size;
	private final String	checkSumType;
	private final byte		endingByte;

	private Element				domElement;

	/**
	 * constructs a DataBlock class using a XML DOM element
	 * @param element (DOM)
	 */
	public DataBlockType(Element element) {
		this.domElement = element;
		this.size = XMLUtils.getIntValue(element, "size");
		this.checkSumType = XMLUtils.getTextValue(element, "checkSumType");
		this.endingByte = XMLUtils.getByteValue(element, "endingByte");
	}

	public DataBlockType() {
		this.size = 0;
		this.checkSumType = "";
		this.endingByte = 0x00;
	}

	public DataBlockType(int size) {
		this.size = size;
		this.checkSumType = "";
		this.endingByte = 0x00;
	}

	public DataBlockType(int size, String checkSum) {
		this.size = size;
		this.checkSumType = checkSum;
		this.endingByte = 0x00;
	}

	public DataBlockType(int size, String checkSum, byte endingByte) {
		this.size = size;
		this.checkSumType = checkSum;
		this.endingByte = endingByte;
	}

	public String toString() {
		return String.format("<DataBlock> size = %d, checkSumType = %s, endingByte = 0x%x", size, checkSumType, endingByte);
	}

	/**
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	/**
	 * @return the checkSum
	 */
	public String getCheckSum() {
		return checkSumType;
	}

	/**
	 * @param size the size to set
	 */
	public void setSize(int size) {
		if (domElement != null) XMLUtils.setIntValue(domElement, "size", size);
		this.size = size;
	}

	/**
	 * @return the endingByte
	 */
	public byte getEndingByte() {
		return endingByte;
	}
}
