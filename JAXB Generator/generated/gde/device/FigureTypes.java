//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.21 at 01:09:13 PM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for figure_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="figure_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="time_sum_sec"/>
 *     &lt;enumeration value="time_step_sec"/>
 *     &lt;enumeration value="count"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "figure_types")
@XmlEnum
public enum FigureTypes {

    @XmlEnumValue("time_sum_sec")
    TIME_SUM_SEC("time_sum_sec"),
    @XmlEnumValue("time_step_sec")
    TIME_STEP_SEC("time_step_sec"),
    @XmlEnumValue("count")
    COUNT("count");
    private final String value;

    FigureTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static FigureTypes fromValue(String v) {
        for (FigureTypes c: FigureTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}