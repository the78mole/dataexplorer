//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.12.29 at 04:02:07 PM GMT+01:00 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import gde.DE;


/**
 * <p>Java class for measurement_property_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="measurement_property_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="offset"/>
 *     &lt;enumeration value="factor"/>
 *     &lt;enumeration value="reduction"/>
 *     &lt;enumeration value="do_subtract_first"/>
 *     &lt;enumeration value="do_subtract_last"/>
 *     &lt;enumeration value="regression_type"/>
 *     &lt;enumeration value="regression_type_curve"/>
 *     &lt;enumeration value="regression_type_linear"/>
 *     &lt;enumeration value="regression_interval_sec"/>
 *     &lt;enumeration value="number_cells"/>
 *     &lt;enumeration value="prop_n100W"/>
 *     &lt;enumeration value="is_invert_current"/>
 *     &lt;enumeration value="number_motor"/>
 *     &lt;enumeration value="revolution_factor"/>
 *     &lt;enumeration value="none_specified"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "measurement_property_types")
@XmlEnum
public enum MeasurementPropertyTypes {

    @XmlEnumValue("offset")
    OFFSET("offset"),
    @XmlEnumValue("factor")
    FACTOR("factor"),
    @XmlEnumValue("reduction")
    REDUCTION("reduction"),
    @XmlEnumValue("do_subtract_first")
    DO_SUBTRACT_FIRST("do_subtract_first"),
    @XmlEnumValue("do_subtract_last")
    DO_SUBTRACT_LAST("do_subtract_last"),
    @XmlEnumValue("regression_type")
    REGRESSION_TYPE("regression_type"),
    @XmlEnumValue("regression_type_curve")
    REGRESSION_TYPE_CURVE("regression_type_curve"),
    @XmlEnumValue("regression_type_linear")
    REGRESSION_TYPE_LINEAR("regression_type_linear"),
    @XmlEnumValue("regression_interval_sec")
    REGRESSION_INTERVAL_SEC("regression_interval_sec"),
    @XmlEnumValue("number_cells")
    NUMBER_CELLS("number_cells"),
    @XmlEnumValue("prop_n100W")
    PROP_N_100_W("prop_n100W"),
    @XmlEnumValue("is_invert_current")
    IS_INVERT_CURRENT("is_invert_current"),
    @XmlEnumValue("number_motor")
    NUMBER_MOTOR("number_motor"),
    @XmlEnumValue("revolution_factor")
    REVOLUTION_FACTOR("revolution_factor"),
    @XmlEnumValue("none_specified")
    NONE_SPECIFIED("none_specified");
    private final String value;

    MeasurementPropertyTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static MeasurementPropertyTypes fromValue(String v) {
        for (MeasurementPropertyTypes c: MeasurementPropertyTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
    
  	public static String[] valuesAsStingArray() {
  		StringBuilder sb = new StringBuilder();
  		for (MeasurementPropertyTypes element : MeasurementPropertyTypes.values()) {
  			if(element.equals(NONE_SPECIFIED)) continue;
  			sb.append(element.value).append(GDE.STRING_SEMICOLON);
  		}
  		return sb.toString().split(GDE.STRING_SEMICOLON);
  	}

  	public static boolean isNoneSpecified(String checkName) {
  		boolean isNoneSpecified = true;
  		for (String element : MeasurementPropertyTypes.valuesAsStingArray()) {
  			if(element.equals(checkName)) {
  				isNoneSpecified = false;
  				break;
  			}
  		}
  		return isNoneSpecified;
  	}
}
