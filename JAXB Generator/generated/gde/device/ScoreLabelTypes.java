//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.09.13 at 08:58:28 AM MESZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for score_label_types.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="score_label_types">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="duration_mm"/>
 *     &lt;enumeration value="averageTimeStep_ms"/>
 *     &lt;enumeration value="maximumTimeStep_ms"/>
 *     &lt;enumeration value="minimumTimeStep_ms"/>
 *     &lt;enumeration value="sigmaTimeStep_ms"/>
 *     &lt;enumeration value="sampledReadings"/>
 *     &lt;enumeration value="totalReadings"/>
 *     &lt;enumeration value="totalPackages"/>
 *     &lt;enumeration value="lostPackages"/>
 *     &lt;enumeration value="lostPackagesPerMille"/>
 *     &lt;enumeration value="lostPackagesAvg_ms"/>
 *     &lt;enumeration value="lostPackagesMax_ms"/>
 *     &lt;enumeration value="lostPackagesMin_ms"/>
 *     &lt;enumeration value="lostPackagesSigma_ms"/>
 *     &lt;enumeration value="sensors"/>
 *     &lt;enumeration value="sensorVario"/>
 *     &lt;enumeration value="sensorGps"/>
 *     &lt;enumeration value="sensorGam"/>
 *     &lt;enumeration value="sensorEam"/>
 *     &lt;enumeration value="sensorEsc"/>
 *     &lt;enumeration value="logDataVersion"/>
 *     &lt;enumeration value="logDataExplorerVersion"/>
 *     &lt;enumeration value="logFileVersion"/>
 *     &lt;enumeration value="logFileBytes"/>
 *     &lt;enumeration value="logRecordSetBytes"/>
 *     &lt;enumeration value="logFileRecordSets"/>
 *     &lt;enumeration value="elapsedHistoRecordSet_ms"/>
 *     &lt;enumeration value="sensorCount"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "score_label_types")
@XmlEnum
public enum ScoreLabelTypes {

    @XmlEnumValue("duration_mm")
    DURATION_MM("duration_mm"),
    @XmlEnumValue("averageTimeStep_ms")
    AVERAGE_TIME_STEP_MS("averageTimeStep_ms"),
    @XmlEnumValue("maximumTimeStep_ms")
    MAXIMUM_TIME_STEP_MS("maximumTimeStep_ms"),
    @XmlEnumValue("minimumTimeStep_ms")
    MINIMUM_TIME_STEP_MS("minimumTimeStep_ms"),
    @XmlEnumValue("sigmaTimeStep_ms")
    SIGMA_TIME_STEP_MS("sigmaTimeStep_ms"),
    @XmlEnumValue("sampledReadings")
    SAMPLED_READINGS("sampledReadings"),
    @XmlEnumValue("totalReadings")
    TOTAL_READINGS("totalReadings"),
    @XmlEnumValue("totalPackages")
    TOTAL_PACKAGES("totalPackages"),
    @XmlEnumValue("lostPackages")
    LOST_PACKAGES("lostPackages"),
    @XmlEnumValue("lostPackagesPerMille")
    LOST_PACKAGES_PER_MILLE("lostPackagesPerMille"),
    @XmlEnumValue("lostPackagesAvg_ms")
    LOST_PACKAGES_AVG_MS("lostPackagesAvg_ms"),
    @XmlEnumValue("lostPackagesMax_ms")
    LOST_PACKAGES_MAX_MS("lostPackagesMax_ms"),
    @XmlEnumValue("lostPackagesMin_ms")
    LOST_PACKAGES_MIN_MS("lostPackagesMin_ms"),
    @XmlEnumValue("lostPackagesSigma_ms")
    LOST_PACKAGES_SIGMA_MS("lostPackagesSigma_ms"),
    @XmlEnumValue("sensors")
    SENSORS("sensors"),
    @XmlEnumValue("sensorVario")
    SENSOR_VARIO("sensorVario"),
    @XmlEnumValue("sensorGps")
    SENSOR_GPS("sensorGps"),
    @XmlEnumValue("sensorGam")
    SENSOR_GAM("sensorGam"),
    @XmlEnumValue("sensorEam")
    SENSOR_EAM("sensorEam"),
    @XmlEnumValue("sensorEsc")
    SENSOR_ESC("sensorEsc"),
    @XmlEnumValue("logDataVersion")
    LOG_DATA_VERSION("logDataVersion"),
    @XmlEnumValue("logDataExplorerVersion")
    LOG_DATA_EXPLORER_VERSION("logDataExplorerVersion"),
    @XmlEnumValue("logFileVersion")
    LOG_FILE_VERSION("logFileVersion"),
    @XmlEnumValue("logFileBytes")
    LOG_FILE_BYTES("logFileBytes"),
    @XmlEnumValue("logRecordSetBytes")
    LOG_RECORD_SET_BYTES("logRecordSetBytes"),
    @XmlEnumValue("logFileRecordSets")
    LOG_FILE_RECORD_SETS("logFileRecordSets"),
    @XmlEnumValue("elapsedHistoRecordSet_ms")
    ELAPSED_HISTO_RECORD_SET_MS("elapsedHistoRecordSet_ms"),
    @XmlEnumValue("sensorCount")
    SENSOR_COUNT("sensorCount");
    private final String value;

    ScoreLabelTypes(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ScoreLabelTypes fromValue(String v) {
        for (ScoreLabelTypes c: ScoreLabelTypes.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
