//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.12.20 at 02:08:46 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for flow_control_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="flow_control_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="FLOWCONTROL_NONE"/>
 *     &lt;enumeration value="FLOWCONTROL_XON_XOFF"/>
 *     &lt;enumeration value="FLOWCONTROL_HARDWARE"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "flow_control_types")
@XmlEnum
public enum FlowControlTypes {

    FLOWCONTROL_NONE,
    FLOWCONTROL_XON_XOFF,
    FLOWCONTROL_HARDWARE;

    public String value() {
        return name();
    }

    public static FlowControlTypes fromValue(String v) {
        return valueOf(v);
    }

}
