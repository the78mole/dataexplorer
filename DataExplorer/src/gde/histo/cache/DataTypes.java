//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.03.25 at 11:38:15 AM MEZ
//


package gde.histo.cache;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import gde.data.Record.DataType;


/**
 * <p>Java class for data_types.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="data_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="default"/>
 *     &lt;enumeration value="gps_latitude"/>
 *     &lt;enumeration value="gps_longitude"/>
 *     &lt;enumeration value="gps_altitude"/>
 *     &lt;enumeration value="gps_azimuth"/>
 *     &lt;enumeration value="speed"/>
 *     &lt;enumeration value="date_time"/>
 *     &lt;enumeration value="current"/>
 *     &lt;enumeration value="voltage"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 *
 */
@XmlType(name = "data_types")
@XmlEnum
public enum DataTypes {


    /**
     * all normal measurement values which do not require special handling
     *
     *
     */
    @XmlEnumValue("default")
    DEFAULT("default"),

    /**
     * GPS geo-coordinate require at least 6 decimal digits [°]
     *
     *
     */
    @XmlEnumValue("gps_latitude")
    GPS_LATITUDE("gps_latitude"),

    /**
     * GPS geo-coordinate require at least 6 decimal digits [°]
     *
     *
     */
    @XmlEnumValue("gps_longitude")
    GPS_LONGITUDE("gps_longitude"),

    /**
     * GPS or absolute altitude required in some case for GPS related calculations
     * 						like speed, distance, ...
     *
     *
     */
    @XmlEnumValue("gps_altitude")
    GPS_ALTITUDE("gps_altitude"),

    /**
     * GPS azimuth, to be used for live display and positioning of icon if used
     *
     *
     */
    @XmlEnumValue("gps_azimuth")
    GPS_AZIMUTH("gps_azimuth"),

    /**
     * speed, to be used for KMZ export with colors of specified velocity
     *
     *
     */
    @XmlEnumValue("speed")
    SPEED("speed"),

    /**
     * special data type where no formatting or calculation can be executed,
     * 						just display drops
     *
     *
     */
    @XmlEnumValue("date_time")
    DATE_TIME("date_time"),

    /**
     * data type to unique identify current type, mainly used for smoothing current
     * 						voltage values
     *
     *
     */
    @XmlEnumValue("current")
    CURRENT("current"),

    /**
     * data type to unique identify voltage type, to smoothing reflex or pulsing
     * 						voltage values
     *
     *
     */
    @XmlEnumValue("voltage")
    VOLTAGE("voltage");
    private final String value;

		/**
		 * use this instead of values() to avoid repeatedly cloning actions.
		 */
		public static final DataTypes VALUES[] = values();

    DataTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static DataTypes fromValue(String v) {
        for (DataTypes c: VALUES) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

    public static DataTypes fromDataType(DataType v) {
    	for (DataTypes c: VALUES) {
    		if (c.name().equals(v.name())) {
    			return c;
    		}
    	}
    	throw new IllegalArgumentException(v.name());
    }

		public static List<DataTypes> getAsList() {
			List<DataTypes> dataTypes = new ArrayList<DataTypes>();
			for (DataTypes type : VALUES) {
				dataTypes.add(type);
			}
			return dataTypes;
		}

		public static List<String> getValuesAsList() {
			List<String> dataTypeValues = new ArrayList<String>();
			for (DataTypes type : VALUES) {
				dataTypeValues.add(type.value);
			}
			return dataTypeValues;
		}

}
