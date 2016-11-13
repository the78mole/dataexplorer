//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.11.13 at 09:53:47 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for calculation_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="calculation_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="min"/>
 *     &lt;enumeration value="max"/>
 *     &lt;enumeration value="avg"/>
 *     &lt;enumeration value="sigma"/>
 *     &lt;enumeration value="sum"/>
 *     &lt;enumeration value="timeSum_ms"/>
 *     &lt;enumeration value="count"/>
 *     &lt;enumeration value="ratio"/>
 *     &lt;enumeration value="ratioInverse"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "calculation_types")
@XmlEnum
public enum CalculationTypes {

    @XmlEnumValue("min")
    MIN("min"),
    @XmlEnumValue("max")
    MAX("max"),
    @XmlEnumValue("avg")
    AVG("avg"),
    @XmlEnumValue("sigma")
    SIGMA("sigma"),
    @XmlEnumValue("sum")
    SUM("sum"),
    @XmlEnumValue("timeSum_ms")
    TIME_SUM_MS("timeSum_ms"),
    @XmlEnumValue("count")
    COUNT("count"),
    @XmlEnumValue("ratio")
    RATIO("ratio"),
    @XmlEnumValue("ratioInverse")
    RATIO_INVERSE("ratioInverse");
    private final String value;

    CalculationTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static CalculationTypes fromValue(String v) {
        for (CalculationTypes c: CalculationTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
