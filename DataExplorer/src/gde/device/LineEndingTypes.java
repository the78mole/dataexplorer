//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.12.31 at 07:19:32 PM GMT+01:00 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import gde.GDE;


/**
 * <p>Java class for line_ending_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="line_ending_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="crlf"/>
 *     &lt;enumeration value="cr"/>
 *     &lt;enumeration value="lf"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "line_ending_types")
@XmlEnum
public enum LineEndingTypes {

    @XmlEnumValue("crlf")
    CRLF("<CR><LF>"),
    @XmlEnumValue("cr")
    CR("<CR>"),
    @XmlEnumValue("lf")
    LF("<LF>");
    private final String value;

    LineEndingTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static LineEndingTypes fromValue(String v) {
        for (LineEndingTypes c: LineEndingTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

  	public static String[] valuesAsStingArray() {
  		StringBuilder sb = new StringBuilder();
  		for (LineEndingTypes element : LineEndingTypes.values()) {
  			sb.append(element.value).append(GDE.STRING_DASH);
  		}
  		return sb.toString().split(GDE.STRING_DASH);
  	}
  	
    public static byte[] bytesFromValue(String v) {
    	byte[] bytes = new byte[] {0x0D, 0x0A};
    	switch (LineEndingTypes.fromValue(v)) {
			case CRLF:
				return new byte[] {0x0D, 0x0A};
			case CR:
				return new byte[] {0x0D};
			case LF:
				return new byte[] {0x0A};
			}
    	return bytes;
  }
  	
	public static String valueFrom(byte[] bytes) {
		String value = LineEndingTypes.CRLF.value;
		if (bytes[0] == 0x0A)																									value = LineEndingTypes.LF.value;
		else if (bytes[0] == 0x0D && bytes.length > 1 && bytes[1] == 0x0A)		value = LineEndingTypes.CRLF.value;
		else if (bytes[0] == 0x0D && bytes.length == 1)												value = LineEndingTypes.CR.value;
		return value;
	}
}
