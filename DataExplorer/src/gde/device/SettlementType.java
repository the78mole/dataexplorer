/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008,2009,2010,2011,2012,2013,2014,2015,2016 Winfried Bruegmann
    					2016 Thomas Eickert
****************************************************************************************/
package gde.device;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for SettlementType complex type.
 * @author Thomas Eickert
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SettlementType", propOrder = { //$NON-NLS-1$
		"name", //$NON-NLS-1$
		"symbol", //$NON-NLS-1$
		"unit", //$NON-NLS-1$
		"active", //$NON-NLS-1$
		"evaluation", //$NON-NLS-1$
		"score", //$NON-NLS-1$
		"property" //$NON-NLS-1$
})
public class SettlementType implements Cloneable {

	@XmlElement(required = true)
	protected String					name;
	@XmlElement(required = true)
	protected String					symbol;
	@XmlElement(required = true)
	protected String					unit;
	protected Boolean					active;
	protected EvaluationType		evaluation;
	protected List<PropertyType>	score;
	protected List<PropertyType>	property;

	/**
	 * default constructor
	 */
	public SettlementType() {
		// ignore
	}

	/**
	 * copy constructor
	 * @param settlement
	 */
	private SettlementType(SettlementType settlement) {
		this.name = settlement.name;
		this.symbol = settlement.symbol;
		this.unit = settlement.unit;
		this.active = settlement.active;
		this.evaluation = settlement.evaluation != null ? settlement.evaluation.clone() : null;
		if (settlement.getScores().size() != 0)
			this.score = new ArrayList<PropertyType>();
		for (PropertyType tmpScore : settlement.getScores()) {
			this.score.add(tmpScore.clone());
		}
		if (settlement.getProperty().size() != 0)
			this.property = new ArrayList<PropertyType>();
		for (PropertyType tmpProperty : settlement.getProperty()) {
			this.property.add(tmpProperty.clone());
		}
	}

	/**
	 * clone method - calls the private copy constructor
	 */
	@Override
	public SettlementType clone() {
		return new SettlementType(this);
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
	public void setActive(Boolean value) {
		this.active = value;
	}

	/**
	 * Gets the value of the evaluation property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link EvaluationType }
	 *     
	 */
	public EvaluationType getEvaluation() {
		return this.evaluation;
	}

	/**
	 * Sets the value of the statistics property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link EvaluationType }
	 *     
	 */
	public void setEvaluation(EvaluationType value) {
		this.evaluation = value;
	}

	/**
	 * Gets the value of the score property.
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
	public List<PropertyType> getScores() {
		if (this.score == null) {
			this.score = new ArrayList<PropertyType>();
		}
		return this.score;
	}

	/**
	 * @param scoreKey
	 * @param type
	 * @param value
	 */
	private void createScore(String scoreKey, DataTypes type, Object value) {
		ObjectFactory factory = new ObjectFactory();
		PropertyType newProperty = factory.createPropertyType();
		newProperty.setName(scoreKey);
		newProperty.setType(type);
		newProperty.setValue("" + value); //$NON-NLS-1$
		this.getScores().add(newProperty);
	}

	/**
	 * remove all score types
	 */
	public void removeScores() {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	/**
	 * remove score type with given key (IDevice.OFFSET, ...)
	 * @param scoreKey
	 */
	public void removeScore(String scoreKey) {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			PropertyType tmpProp = iterator.next();
			if (tmpProp.name.equals(scoreKey))
				iterator.remove();
		}
	}

	/**
	 * get score type with given key (IDevice.OFFSET, ...)
	 * @param scoreKey
	 * @return PropertyType object
	 */
	public PropertyType getScore(String scoreKey) {
		PropertyType tmpScore = null;
		List<PropertyType> scores = this.getScores();
		for (PropertyType scoreType : scores) {
			if (scoreType.getName().equals(scoreKey)) {
				tmpScore = scoreType;
				break;
			}
		}
		return tmpScore;
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
	 * remove all property types
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public void removeProperties() {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			iterator.next();
			iterator.remove();
		}
	}

	/**
	 * remove property type with given key (IDevice.OFFSET, ...)
	 * @param propertyKey
	 * @return PropertyType object
	 */
	public void removeProperty(String propertyKey) {
		Iterator<PropertyType> iterator = this.getProperty().iterator();

		while (iterator.hasNext()) {
			PropertyType tmpProp = iterator.next();
			if (tmpProp.name.equals(propertyKey))
				iterator.remove();
		}
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
		if (tmpProperty != null)
			value = new Double(tmpProperty.getValue()).doubleValue();

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
		} else {
			tmpProperty.setValue("" + offset); //$NON-NLS-1$
		}
	}

	/**
	 * get the reduction value
	 * @return the offset, if property does not exist return 0.0 as default value
	 */
	public double getReduction() {
		double value = 0.0;
		PropertyType tmpProperty = this.getProperty(IDevice.REDUCTION);
		if (tmpProperty != null)
			value = new Double(tmpProperty.getValue()).doubleValue();

		return value;
	}

	/**
	 * set new value for reduction
	 * @param reduction the offset to set
	 */
	public void setReduction(double reduction) {
		PropertyType tmpProperty = this.getProperty(IDevice.REDUCTION);
		if (tmpProperty == null) {
			createProperty(IDevice.REDUCTION, DataTypes.DOUBLE, reduction);
		} else {
			tmpProperty.setValue("" + reduction); //$NON-NLS-1$
		}
	}

	/**
	 * get the factor value
	 * @return the factor, if property does not exist return 1.0 as default value
	 */
	public double getFactor() {
		double value = 1.0;
		PropertyType tmpProperty = getProperty(IDevice.FACTOR);
		if (tmpProperty != null)
			value = new Double(tmpProperty.getValue()).doubleValue();

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
		} else {
			tmpProperty.setValue("" + factor); //$NON-NLS-1$
		}
	}
}
