//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2018.02.02 at 02:42:15 PM MEZ 
//


package gde.histo.cache;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for compartmentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="compartmentType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="trails" type="{}pointsType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="text" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="dataType" use="required" type="{}data_types" />
 *       &lt;attribute name="outlierPoints" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="scrappedPoints" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "compartmentType", propOrder = {
    "trails"
})
public class CompartmentType {

    @XmlElement(required = true)
    protected PointsType trails;
    @XmlAttribute(required = true)
    protected int id;
    @XmlAttribute(required = true)
    protected String text;
    @XmlAttribute(required = true)
    protected DataTypes dataType;
    @XmlAttribute
    protected String outlierPoints;
    @XmlAttribute
    protected String scrappedPoints;

    /**
     * Gets the value of the trails property.
     * 
     * @return
     *     possible object is
     *     {@link PointsType }
     *     
     */
    public PointsType getTrails() {
        return trails;
    }

    /**
     * Sets the value of the trails property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointsType }
     *     
     */
    public void setTrails(PointsType value) {
        this.trails = value;
    }

    /**
     * Gets the value of the id property.
     * 
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     */
    public void setId(int value) {
        this.id = value;
    }

    /**
     * Gets the value of the text property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the value of the text property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setText(String value) {
        this.text = value;
    }

    /**
     * Gets the value of the dataType property.
     * 
     * @return
     *     possible object is
     *     {@link DataTypes }
     *     
     */
    public DataTypes getDataType() {
        return dataType;
    }

    /**
     * Sets the value of the dataType property.
     * 
     * @param value
     *     allowed object is
     *     {@link DataTypes }
     *     
     */
    public void setDataType(DataTypes value) {
        this.dataType = value;
    }

    /**
     * Gets the value of the outlierPoints property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOutlierPoints() {
        return outlierPoints;
    }

    /**
     * Sets the value of the outlierPoints property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOutlierPoints(String value) {
        this.outlierPoints = value;
    }

    /**
     * Gets the value of the scrappedPoints property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScrappedPoints() {
        return scrappedPoints;
    }

    /**
     * Sets the value of the scrappedPoints property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScrappedPoints(String value) {
        this.scrappedPoints = value;
    }

}
