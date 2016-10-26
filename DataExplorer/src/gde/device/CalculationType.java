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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for calculationType complex type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CalculationType")
public class CalculationType {

	public enum CalcType {
		MIN("min"), //
		MAX("max"), //
		AVG("avg"), //
		SIGMA("sigma"), //
		SUM("sum"), //
		TIMESUM_MS("timeSum_ms"),
		COUNT("count"), //
		RATIO("ratio"), //
		RATIOINVERSE("ratioInverse");

		public final String value;

		private CalcType(String v) {
			this.value = v;
		}
	}

	@XmlAttribute(required = true)
	protected Integer	transitionId;
	@XmlAttribute(required = true)
	protected String	calcType;
	@XmlAttribute(required = true)
	protected Boolean unsigned;
	@XmlAttribute(required = true)
	protected Integer	refOrdinal;
	@XmlAttribute(required = true)
	protected String	leveling;
	@XmlAttribute
	protected Integer	refOrdinalDivisor;
	@XmlAttribute
	protected String	divisorLeveling;
	@XmlAttribute
	protected Boolean	isBasedOnRecovery;
	@XmlAttribute
	protected String	comment;

	/**
	 * Gets the value of the transitionId property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public Integer getTransitionId() {
		return this.transitionId;
	}

	/**
	 * Sets the value of the transitionId property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setTransitionId(Integer value) {
		this.transitionId = value;
	}

	/**
	 * Gets the value of the calcType property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getCalcType() {
		return this.calcType;
	}

	/**
	 * Sets the value of the calcType property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setCalcType(String value) {
		this.calcType = value;
	}

	/**
	 * Gets the value of the unsigned property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Boolean }
	 *     
	 */
	public Boolean isUnsigned() {
		return this.unsigned;
	}

	/**
	 * Sets the value of the unsigned property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Boolean }
	 *     
	 */

	public void setUnsigned(Boolean value) {
		this.unsigned = value;
	}

	/**
	 * Gets the value of the refOrdinal property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public Integer getRefOrdinal() {
		return this.refOrdinal;
	}

	/**
	 * Sets the value of the refOrdinal property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setRefOrdinal(Integer value) {
		this.refOrdinal = value;
	}

	/**
	 * Gets the value of the leveling property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public String getLeveling() {
		return this.leveling;
	}

	/**
	 * Sets the value of the leveling property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setLeveling(String value) {
		this.leveling = value;
	}

	/**
	 * Gets the value of the refOrdinalInverse property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public Integer getRefOrdinalDivisor() {
		return this.refOrdinalDivisor;
	}

	/**
	 * Sets the value of the refOrdinalInverse property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setRefOrdinalDivisor(Integer value) {
		this.refOrdinalDivisor = value;
	}

	/**
	 * Gets the value of the divisorLeveling property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public String getDivisorLeveling() {
		return this.divisorLeveling;
	}

	/**
	 * Sets the value of the divisorLeveling property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setDivisorLeveling(String value) {
		this.divisorLeveling = value;
	}

	/**
	 * Gets the value of the BasedOnRecovery property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Boolean }
	 *     
	 */
	public Boolean isBasedOnRecovery() {
		return this.isBasedOnRecovery;
	}

	/**
	 * Sets the value of the BasedOnRecovery property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Boolean }
	 *     
	 */

	public void setBasedOnRecovery(Boolean value) {
		this.isBasedOnRecovery = value;
	}

	/**
	 * Gets the value of the comment property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getComment() {
		return this.comment;
	}

	/**
	 * Sets the value of the comment property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setComment(String value) {
		this.comment = value;
	}

}
