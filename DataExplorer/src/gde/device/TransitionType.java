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
 * Java class for transitionType complex type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransitionType")
public class TransitionType {

	@XmlAttribute(required = true)
	protected int		transitionId;
	@XmlAttribute(required = true)
	protected int		refOrdinal;
	@XmlAttribute(required = true)
	protected boolean	isPeak;
	@XmlAttribute(required = true)
	protected boolean	isGreater;
	@XmlAttribute(required = true)
	protected boolean	isPercent;
	@XmlAttribute(required = true)
	protected int		triggerLevel;
	@XmlAttribute(required = true)
	protected int		recoveryLevel;
	@XmlAttribute(required = true)
	protected int		referenceTimeMsec;
	@XmlAttribute(required = true)
	protected int		thresholdTimeMsec;
	@XmlAttribute(required = true)
	protected int		recoveryTimeMsec;
	@XmlAttribute
	protected String	comment;

	/**
	 * Gets the value of the transitionId property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getTransitionId() {
		return this.transitionId;
	}

	/**
	 * Sets the value of the transitionId property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setTransitionId(int value) {
		this.transitionId = value;
	}

	/**
	 * Gets the value of the refOrdinal property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getRefOrdinal() {
		return this.refOrdinal;
	}

	/**
	 * Sets the value of the refOrdinal property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setRefOrdinal(int value) {
		this.refOrdinal = value;
	}

	/**
	 * Gets the value of the isPeak property.
	 * 
	 */
	public boolean isPeak() {
		return this.isPeak;
	}

	/**
	 * Sets the value of the isPeak property.
	 * 
	 */
	public void setIsPeak(boolean value) {
		this.isPeak = value;
	}

	/**
	 * Gets the value of the isGreater property.
	 * 
	 */
	public boolean isGreater() {
		return this.isGreater;
	}

	/**
	 * Sets the value of the isGreater property.
	 * 
	 */
	public void setIsGreater(boolean value) {
		this.isGreater = value;
	}

	/**
	 * Gets the value of the isPercent property.
	 * 
	 */
	public boolean isPercent() {
		return this.isPercent;
	}

	/**
	 * Sets the value of the isPercent property.
	 * 
	 */
	public void setPercent(boolean value) {
		this.isPercent = value;
	}

	/**
	 * Gets the value of the triggerLevel property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getTriggerLevel() {
		return this.triggerLevel;
	}

	/**
	 * Sets the value of the triggerLevel property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setTriggerLevel(int value) {
		this.triggerLevel = value;
	}

	/**
	 * Gets the value of the recoveryLevel property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getRecoveryLevel() {
		return this.recoveryLevel;
	}

	/**
	 * Sets the value of the recoveryLevel property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setrecoveryLevel(int value) {
		this.recoveryLevel = value;
	}

	/**
	 * Gets the value of the referenceTime_ms property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getReferenceTimeMsec() {
		return this.referenceTimeMsec;
	}

	/**
	 * Sets the value of the referenceTime_ms property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setReferenceTimeMsec(int value) {
		this.referenceTimeMsec = value;
	}

	/**
	 * Gets the value of the setThresholdTime_ms property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getThresholdTimeMsec() {
		return this.thresholdTimeMsec;
	}

	/**
	 * Sets the value of the setThresholdTime_ms property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setThresholdTimeMsec(int value) {
		this.thresholdTimeMsec = value;
	}

	/**
	 * Gets the value of the setRecoveryTime_ms property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link int }
	 *     
	 */
	public int getRecoveryTimeMsec() {
		return this.recoveryTimeMsec;
	}

	/**
	 * Sets the value of the setRecoveryTime_ms property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link int }
	 *     
	 */
	public void setRecoveryTimeMsec(int value) {
		this.recoveryTimeMsec = value;
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
