//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.21 at 10:16:55 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for reference_rule_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="reference_rule_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="avg"/>
 *     &lt;enumeration value="max"/>
 *     &lt;enumeration value="min"/>
 *     &lt;enumeration value="product"/>
 *     &lt;enumeration value="quotient"/>
 *     &lt;enumeration value="spread"/>
 *     &lt;enumeration value="sum"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "reference_rule_types")
@XmlEnum
public enum ReferenceRuleTypes {

    @XmlEnumValue("avg")
    AVG("avg"),
    @XmlEnumValue("max")
    MAX("max"),
    @XmlEnumValue("min")
    MIN("min"),
    @XmlEnumValue("product")
    PRODUCT("product"),
    @XmlEnumValue("quotient")
    QUOTIENT("quotient"),
    @XmlEnumValue("spread")
    SPREAD("spread"),
    @XmlEnumValue("sum")
    SUM("sum");
    private final String value;

    ReferenceRuleTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ReferenceRuleTypes fromValue(String v) {
        for (ReferenceRuleTypes c: ReferenceRuleTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}