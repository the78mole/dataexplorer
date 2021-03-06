//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.12.13 at 01:53:37 PM CET 
//


package gde.device;

import java.math.BigInteger;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java class for SerialPortType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SerialPortType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="port" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="baudeRate" type="{}baud_rate_types"/>
 *         &lt;element name="dataBits" type="{}data_bits_types"/>
 *         &lt;element name="stopBits" type="{}stop_bits_types"/>
 *         &lt;element name="parity" type="{}parity_types"/>
 *         &lt;element name="flowControlMode" type="{}flow_control_types"/>
 *         &lt;element name="isRTS" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="isDTR" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;sequence>
 *           &lt;element name="TimeOut" type="{}TimeOutType" minOccurs="0"/>
 *         &lt;/sequence>
 *         &lt;element name="request" type="{http://www.w3.org/2001/XMLSchema}hexBinary" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SerialPortType", propOrder = {
    "port",
    "baudeRate",
    "dataBits",
    "stopBits",
    "parity",
    "flowControlMode",
    "isRTS",
    "isDTR",
    "timeOut",
    "request"
})
public class SerialPortType {

    @XmlElement(required = true)
    protected String port;
    @XmlElement(required = true)
    protected BigInteger baudeRate;
    @XmlElement(required = true)
    protected DataBitsTypes dataBits;
    @XmlElement(required = true)
    protected StopBitsTypes stopBits;
    @XmlElement(required = true)
    protected ParityTypes parity;
    @XmlElement(required = true)
    protected FlowControlTypes flowControlMode;
    protected boolean isRTS;
    protected boolean isDTR;
    @XmlElement(name = "TimeOut")
    protected TimeOutType timeOut;
    @XmlElement(type = String.class)
    @XmlJavaTypeAdapter(HexBinaryAdapter.class)
    @XmlSchemaType(name = "hexBinary")
    protected byte[] request;

    /**
     * Gets the value of the port property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPort() {
        return port;
    }

    /**
     * Sets the value of the port property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPort(String value) {
        this.port = value;
    }

    /**
     * Gets the value of the baudeRate property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getBaudeRate() {
        return baudeRate;
    }

    /**
     * Sets the value of the baudeRate property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setBaudeRate(BigInteger value) {
        this.baudeRate = value;
    }

    /**
     * Gets the value of the dataBits property.
     * 
     * @return
     *     possible object is
     *     {@link DataBitsTypes }
     *     
     */
    public DataBitsTypes getDataBits() {
        return dataBits;
    }

    /**
     * Sets the value of the dataBits property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataBitsTypes }
     *     
     */
    public void setDataBits(DataBitsTypes value) {
        this.dataBits = value;
    }

    /**
     * Gets the value of the stopBits property.
     * 
     * @return
     *     possible object is
     *     {@link StopBitsTypes }
     *     
     */
    public StopBitsTypes getStopBits() {
        return stopBits;
    }

    /**
     * Sets the value of the stopBits property.
     * 
     * @param value
     *     allowed object is
     *     {@link StopBitsTypes }
     *     
     */
    public void setStopBits(StopBitsTypes value) {
        this.stopBits = value;
    }

    /**
     * Gets the value of the parity property.
     * 
     * @return
     *     possible object is
     *     {@link ParityTypes }
     *     
     */
    public ParityTypes getParity() {
        return parity;
    }

    /**
     * Sets the value of the parity property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParityTypes }
     *     
     */
    public void setParity(ParityTypes value) {
        this.parity = value;
    }

    /**
     * Gets the value of the flowControlMode property.
     * 
     * @return
     *     possible object is
     *     {@link FlowControlTypes }
     *     
     */
    public FlowControlTypes getFlowControlMode() {
        return flowControlMode;
    }

    /**
     * Sets the value of the flowControlMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link FlowControlTypes }
     *     
     */
    public void setFlowControlMode(FlowControlTypes value) {
        this.flowControlMode = value;
    }

    /**
     * Gets the value of the isRTS property.
     * 
     */
    public boolean isIsRTS() {
        return isRTS;
    }

    /**
     * Sets the value of the isRTS property.
     * 
     */
    public void setIsRTS(boolean value) {
        this.isRTS = value;
    }

    /**
     * Gets the value of the isDTR property.
     * 
     */
    public boolean isIsDTR() {
        return isDTR;
    }

    /**
     * Sets the value of the isDTR property.
     * 
     */
    public void setIsDTR(boolean value) {
        this.isDTR = value;
    }

    /**
     * Gets the value of the timeOut property.
     * 
     * @return
     *     possible object is
     *     {@link TimeOutType }
     *     
     */
    public TimeOutType getTimeOut() {
        return timeOut;
    }

    /**
     * Sets the value of the timeOut property.
     * 
     * @param value
     *     allowed object is
     *     {@link TimeOutType }
     *     
     */
    public void setTimeOut(TimeOutType value) {
        this.timeOut = value;
    }

    /**
     * Gets the value of the request property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public byte[] getRequest() {
        return request;
    }

    /**
     * Sets the value of the request property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRequest(byte[] value) {
        this.request = ((byte[]) value);
    }

}
