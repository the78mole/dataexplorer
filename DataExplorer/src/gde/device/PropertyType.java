//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2008.02.08 at 09:38:52 PM CET 
//


package osde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

import osde.DE;


/**
 * <p>Java class for PropertyType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="PropertyType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" />
 *       &lt;attribute name="type" use="required" type="{}data_types" />
 *       &lt;attribute name="description" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PropertyType") //$NON-NLS-1$
public class PropertyType {

    @XmlAttribute(required = true)
    protected String name;
    @XmlAttribute(required = true)
    @XmlSchemaType(name = "anySimpleType") //$NON-NLS-1$
    protected String value;
    @XmlAttribute(required = true)
    protected DataTypes type;
    @XmlAttribute
    protected String description;

    /**
     * default constructor
     */
    public PropertyType() {}
    
    /**
     * copy constructor - used for clone method
     */
    private PropertyType(PropertyType property) {
    	this.name = property.name;
    	this.value = property.value;
    	this.type = property.type;
    	this.description = property.description;
    }
    
    /**
     * clone methods to deep copy PropertyType
     */
    public PropertyType clone() {
    	return new PropertyType(this);
    }
    
    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param newValue
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String newValue) {
        this.name = newValue;
    }

    /**
     * Gets the value of the value property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValue() {
    	if (this.type == DataTypes.DOUBLE)
        return this.value.trim().replace(',', '.');
    	 
    	return this.value.trim();
    }

    /**
     * Sets the value of the value property.
     * 
     * @param newValue
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValue(Object newValue) {
        this.value = (DE.STRING_EMPTY + newValue).trim();
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link DataTypes }
     *     
     */
    public DataTypes getType() {
        return this.type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param newValue
     *     allowed object is
     *     {@link DataTypes }
     *     
     */
    public void setType(DataTypes newValue) {
        this.type = newValue;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDescription() {
        return this.description != null ? this.description : DE.STRING_EMPTY;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param newValue
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDescription(String newValue) {
        this.description = newValue.trim().equals(DE.STRING_EMPTY) ? null : newValue.trim();
    }

}
