package osde.device;

import org.w3c.dom.Element;

import osde.utils.XMLUtils;

public class DataBlockType {
	private int						size;
	private final String	checkSumType;
	private final byte		endingByte;

	private Element				domElement;

	/**
	 * constructs a DataBlock class using a XML DOM element
	 * @param DOM element
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
