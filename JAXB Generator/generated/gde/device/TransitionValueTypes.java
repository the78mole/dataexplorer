//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.04 at 01:56:10 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for transition_value_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="transition_value_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="lowerThreshold"/>
 *     &lt;enumeration value="upperThreshold"/>
 *     &lt;enumeration value="deltaValue"/>
 *     &lt;enumeration value="deltaFactor"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "transition_value_types")
@XmlEnum
public enum TransitionValueTypes {

    @XmlEnumValue("lowerThreshold")
    LOWER_THRESHOLD("lowerThreshold"),
    @XmlEnumValue("upperThreshold")
    UPPER_THRESHOLD("upperThreshold"),
    @XmlEnumValue("deltaValue")
    DELTA_VALUE("deltaValue"),
    @XmlEnumValue("deltaFactor")
    DELTA_FACTOR("deltaFactor");
    private final String value;

    TransitionValueTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static TransitionValueTypes fromValue(String v) {
        for (TransitionValueTypes c: TransitionValueTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
