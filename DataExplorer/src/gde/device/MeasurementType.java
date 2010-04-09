//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.11.13 at 07:00:40 PM MEZ 
//

package gde.device;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for MeasurementType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MeasurementType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="symbol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="unit" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="active" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *         &lt;element name="statistics" type="{}StatisticsType" minOccurs="0"/>
 *         &lt;element name="property" type="{}PropertyType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MeasurementType", propOrder = { //$NON-NLS-1$
		"name", //$NON-NLS-1$
		"symbol", //$NON-NLS-1$
		"unit", //$NON-NLS-1$
		"active", //$NON-NLS-1$
		"statistics", //$NON-NLS-1$
		"property" //$NON-NLS-1$
})
public class MeasurementType {

	@XmlElement(required = true)
	protected String							name;
	@XmlElement(required = true)
	protected String							symbol;
	@XmlElement(required = true)
	protected String							unit;
	protected Boolean							active;
	protected StatisticsType			statistics;
	protected List<PropertyType>	property;

	/**
	 * default constructor
	 */
	public MeasurementType() {
	}

	/**
	 * copy constructor
	 * @param measurement
	 */
	private MeasurementType(MeasurementType measurement) {
		this.name = measurement.name;
		this.symbol = measurement.symbol;
		this.unit = measurement.unit;
		this.active = measurement.active;
		this.statistics = measurement.statistics != null ? measurement.statistics.clone() : null;
		if (measurement.property != null) {
			this.property = new ArrayList<PropertyType>();
			for (PropertyType tmpProperty : this.property) {
				this.property.add(tmpProperty.clone());
			}
		}
		else {
			this.property = null; //new ArrayList<PropertyType>();
		}
	}

	/**
	 * clone method - calls the private copy constructor
	 */
	@Override
	public MeasurementType clone() {
		return new MeasurementType(this);
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
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setName(String value) {
		this.name = value;
	}

	/**
	 * Gets the value of the symbol property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getSymbol() {
		return this.symbol;
	}

	/**
	 * Sets the value of the symbol property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setSymbol(String value) {
		this.symbol = value;
	}

	/**
	 * Gets the value of the unit property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getUnit() {
		return this.unit;
	}

	/**
	 * Sets the value of the unit property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setUnit(String value) {
		this.unit = value;
	}

	/**
	 * Gets the value of the active property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Boolean }
	 *     
	 */
	public boolean isActive() {
		return this.active != null ? this.active : false;
	}

	/**
	 * Sets the value of the active property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Boolean }
	 *     
	 */
	public void setActive(boolean value) {
		this.active = value;
	}

	/**
	 * Gets the value of the calculation 
	 * 
	 * @return
	 *     possible object is
	 *     {@link Boolean }
	 *     
	 */
	public boolean isCalculation() {
		return this.active == null ? true : false;
	}

    /**
     * Gets the value of the statistics property.
     * 
     * @return
     *     possible object is
     *     {@link StatisticsType }
     *     
     */
    public StatisticsType getStatistics() {
        return statistics;
    }

    /**
     * Sets the value of the statistics property.
     * 
     * @param value
     *     allowed object is
     *     {@link StatisticsType }
     *     
     */
    public void setStatistics(StatisticsType value) {
        this.statistics = value;
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
	public List<PropertyType> getProperty() {
		if (this.property == null) {
			this.property = new ArrayList<PropertyType>();
		}
		return this.property;
	}

	/**
	 * @param propertyKey
	 * @param type
	 * @param value
	 */
	private void createProperty(String propertyKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(propertyKey);
		newProperty.setType(type);
		newProperty.setValue("" + value); //$NON-NLS-1$
		this.getProperty().add(newProperty);
	}

	/**
	 * get property type with given key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public PropertyType getProperty(String propertyKey) {
		PropertyType tmpProperty = null;
		List<PropertyType> properties = this.getProperty();
		for (PropertyType propertyType : properties) {
			if (propertyType.getName().equals(propertyKey)) {
				tmpProperty = propertyType;
				break;
			}
		}
		return tmpProperty;
	}

	/**
	 * get the offset value
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getOffset() {
		double value = 0.0;
		PropertyType tmpProperty = this.getProperty(IDevice.OFFSET);
		if (tmpProperty != null) value = new Double(tmpProperty.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for offset
	 * @param offset the offset to set
	 */
	public void setOffset(double offset) {
		PropertyType tmpProperty = this.getProperty(IDevice.OFFSET);
		if (tmpProperty == null) {
			createProperty(IDevice.OFFSET, DataTypes.DOUBLE, offset);
		}
		else {
			tmpProperty.setValue("" + offset); //$NON-NLS-1$
		}
	}

	/**
	 * get the factor value
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getFactor() {
		double value = 1.0;
		PropertyType tmpProperty = getProperty(IDevice.FACTOR);
		if (tmpProperty != null) value = new Double(tmpProperty.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for factor
	 * @param factor the offset to set
	 */
	public void setFactor(double factor) {
		PropertyType tmpProperty = this.getProperty(IDevice.FACTOR);
		if (tmpProperty == null) {
			createProperty(IDevice.FACTOR, DataTypes.DOUBLE, factor);
		}
		else {
			tmpProperty.setValue("" + factor); //$NON-NLS-1$
		}
	}
}
