//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2010.12.22 at 07:33:08 PM MEZ
//


package gde.device;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for DevicePropertiesType complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="DevicePropertiesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Device" type="{}DeviceType"/>
 *         &lt;element name="SerialPort" type="{}SerialPortType" minOccurs="0"/>
 *         &lt;element name="UsbPort" type="{}UsbPortType" minOccurs="0"/>
 *         &lt;element name="TimeBase" type="{}TimeBaseType"/>
 *         &lt;element name="DataBlock" type="{}DataBlockType" minOccurs="0"/>
 *         &lt;element name="State" type="{}StateType" minOccurs="0"/>
 *         &lt;element name="Channels">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element name="Channel" type="{}ChannelType" maxOccurs="unbounded"/>
 *                   &lt;element name="property" type="{}PropertyType" maxOccurs="unbounded" minOccurs="0"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="lastUseOrdinal" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="Desktop" type="{}DesktopType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DevicePropertiesType", propOrder = {
    "device",
    "serialPort",
    "usbPort",
    "timeBase",
    "dataBlock",
    "state",
    "channels",
    "desktop"
})
public class DevicePropertiesType {

    @XmlElement(name = "Device", required = true)
    protected DeviceType device;
    @XmlElement(name = "SerialPort")
    protected SerialPortType serialPort;
    @XmlElement(name = "UsbPort")
    protected UsbPortType usbPort;
    @XmlElement(name = "TimeBase", required = true)
    protected TimeBaseType timeBase;
    @XmlElement(name = "DataBlock")
    protected DataBlockType dataBlock;
    @XmlElement(name = "State")
    protected StateType state;
    @XmlElement(name = "Channels", required = true)
    protected DevicePropertiesType.Channels channels;
    @XmlElement(name = "Desktop", required = true)
    protected DesktopType desktop;

    /**
     * Gets the value of the device property.
     *
     * @return
     *     possible object is
     *     {@link DeviceType }
     *
     */
    public DeviceType getDevice() {
        return device;
    }

    /**
     * Sets the value of the device property.
     *
     * @param value
     *     allowed object is
     *     {@link DeviceType }
     *
     */
    public void setDevice(DeviceType value) {
        this.device = value;
    }

    /**
     * Gets the value of the serialPort property.
     *
     * @return
     *     possible object is
     *     {@link SerialPortType }
     *
     */
    public SerialPortType getSerialPort() {
        return serialPort;
    }

    /**
     * Sets the value of the serialPort property.
     *
     * @param value
     *     allowed object is
     *     {@link SerialPortType }
     *
     */
    public void setSerialPort(SerialPortType value) {
        this.serialPort = value;
    }

    /**
     * Gets the value of the usbPort property.
     *
     * @return
     *     possible object is
     *     {@link UsbPortType }
     *
     */
    public UsbPortType getUsbPort() {
        return usbPort;
    }

    /**
     * Sets the value of the usbPort property.
     *
     * @param value
     *     allowed object is
     *     {@link UsbPortType }
     *
     */
    public void setUsbPort(UsbPortType value) {
        this.usbPort = value;
    }

    /**
     * Gets the value of the timeBase property.
     *
     * @return
     *     possible object is
     *     {@link TimeBaseType }
     *
     */
    public TimeBaseType getTimeBase() {
        return timeBase;
    }

    /**
     * Sets the value of the timeBase property.
     *
     * @param value
     *     allowed object is
     *     {@link TimeBaseType }
     *
     */
    public void setTimeBase(TimeBaseType value) {
        this.timeBase = value;
    }

    /**
     * Gets the value of the dataBlock property.
     *
     * @return
     *     possible object is
     *     {@link DataBlockType }
     *
     */
    public DataBlockType getDataBlock() {
        return dataBlock;
    }

    /**
     * Sets the value of the dataBlock property.
     *
     * @param value
     *     allowed object is
     *     {@link DataBlockType }
     *
     */
    public void setDataBlock(DataBlockType value) {
        this.dataBlock = value;
    }

    /**
     * Gets the value of the state property.
     *
     * @return
     *     possible object is
     *     {@link StateType }
     *
     */
    public StateType getState() {
        return state;
    }

    /**
     * Sets the value of the state property.
     *
     * @param value
     *     allowed object is
     *     {@link StateType }
     *
     */
    public void setState(StateType value) {
        this.state = value;
    }

    /**
     * Gets the value of the channels property.
     *
     * @return
     *     possible object is
     *     {@link DevicePropertiesType.Channels }
     *
     */
    public DevicePropertiesType.Channels getChannels() {
        return channels;
    }

    /**
     * Sets the value of the channels property.
     *
     * @param value
     *     allowed object is
     *     {@link DevicePropertiesType.Channels }
     *
     */
    public void setChannels(DevicePropertiesType.Channels value) {
        this.channels = value;
    }

    /**
     * Gets the value of the desktop property.
     *
     * @return
     *     possible object is
     *     {@link DesktopType }
     *
     */
    public DesktopType getDesktop() {
        return desktop;
    }

    /**
     * Sets the value of the desktop property.
     *
     * @param value
     *     allowed object is
     *     {@link DesktopType }
     *
     */
    public void setDesktop(DesktopType value) {
        this.desktop = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     *
     * <p>The following schema fragment specifies the expected content contained within this class.
     *
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element name="Channel" type="{}ChannelType" maxOccurs="unbounded"/>
     *         &lt;element name="property" type="{}PropertyType" maxOccurs="unbounded" minOccurs="0"/>
     *       &lt;/sequence>
     *       &lt;attribute name="lastUseOrdinal" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     *
     *
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "channel",
        "property"
    })
    public static class Channels {

        @XmlElement(name = "Channel", required = true)
        protected List<ChannelType> channel;
        protected List<ChannelPropertyType> property;
        @XmlAttribute
        protected Integer lastUseOrdinal;

        /**
         * Gets the value of the channel property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the channel property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getChannel().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link ChannelType }
         *
         *
         */
        public List<ChannelType> getChannel() {
            if (channel == null) {
                channel = new ArrayList<ChannelType>();
            }
            return this.channel;
        }

        /**
         * Gets the value of the property property.
         *
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the property property.
         *
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getProperty().add(newItem);
         * </pre>
         *
         *
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link PropertyType }
         *
         *
         */
        public List<ChannelPropertyType> getProperty() {
            if (property == null) {
                property = new ArrayList<ChannelPropertyType>();
            }
            return this.property;
        }

        /**
         * Gets the value of the lastUseOrdinal property.
         *
         * @return
         *     possible object is
         *     {@link Integer }
         *
         */
        @Deprecated
        public Integer getLastUseOrdinal() {
            return lastUseOrdinal != null ? lastUseOrdinal : 0;
        }

        /**
         * Sets the value of the lastUseOrdinal property.
         *
         * @param value
         *     allowed object is
         *     {@link Integer }
         *
         */
        @Deprecated
        public void setLastUseOrdinal(Integer value) {
            this.lastUseOrdinal = value;
        }

    }

}
