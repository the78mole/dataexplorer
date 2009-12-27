//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2009.11.15 at 10:25:53 AM MEZ 
//

package osde.device;


import java.math.BigInteger;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for StatisticsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StatisticsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="trigger" type="{}triggerType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="min" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="max" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="avg" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="sigma" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="triggerRefOrdinal" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="sumByTriggerRefOrdinal" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="sumTriggerText" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="countByTrigger" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="countTriggerText" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="sumTriggerTimeText" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="ratioRefOrdinal" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="ratioText" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StatisticsType", propOrder = { "trigger" })
public class StatisticsType {

  protected TriggerType trigger;
	@XmlAttribute(required = true)
	protected boolean									min;
	@XmlAttribute(required = true)
	protected boolean									max;
	@XmlAttribute(required = true)
	protected boolean									avg;
	@XmlAttribute(required = true)
	protected boolean									sigma;
	@XmlAttribute
	protected Integer									triggerRefOrdinal;
	@XmlAttribute
	protected Integer									sumByTriggerRefOrdinal;
	@XmlAttribute
	protected String									sumTriggerText;
	@XmlAttribute
	protected Boolean									countByTrigger;
	@XmlAttribute
	protected String									countTriggerText;
	@XmlAttribute
	protected String									comment;
	@XmlAttribute
	protected String									sumTriggerTimeText;
	@XmlAttribute
	protected Integer									ratioRefOrdinal;
	@XmlAttribute
	protected String									ratioText;

	/**
	 * default constructor
	 */
	public StatisticsType() {
	}

	/**
	 * copy constructor
	 * @param statistics
	 */
	private StatisticsType(StatisticsType statistics) {
	  this.trigger = statistics.trigger;
		this.min = statistics.min;
		this.max = statistics.max;
		this.avg = statistics.avg;
		this.sigma = statistics.sigma;
		this.triggerRefOrdinal = statistics.triggerRefOrdinal;
		this.sumByTriggerRefOrdinal = statistics.sumByTriggerRefOrdinal;
		this.sumTriggerText = statistics.sumTriggerText;
		this.countByTrigger = statistics.countByTrigger;
		this.countTriggerText = statistics.countTriggerText;
		this.comment = statistics.comment;
		this.sumTriggerTimeText = statistics.sumTriggerTimeText;
		this.ratioRefOrdinal = statistics.ratioRefOrdinal;
		this.ratioText = statistics.ratioText;
	}

	/**
	 * clone method - calls the private copy constructor
	 */
	@Override
	public StatisticsType clone() {
		return new StatisticsType(this);
	}

	/**
	 * Gets the value of the trigger property.
	 * 
	 * @return
	 *     possible object is
     *     {@link TriggerType }
	 *     
	 */
    public TriggerType getTrigger() {
		return this.trigger;
	}

	/**
	 * Sets the value of the trigger property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link StatisticsType.Trigger }
	 *     
	 */
	public void setTrigger(TriggerType value) {
		this.trigger = value;
	}

	/**
	 * Gets the value of the trigger property.
	 * 
	 * @return
	 *     possible object is
     *     {@link TriggerType }
	 *     
	 */
    public void removeTrigger() {
		this.trigger = null;
	}

	/**
	 * Gets the value of the min property.
	 * 
	 */
	public boolean isMin() {
		return this.min;
	}

	/**
	 * Sets the value of the min property.
	 * 
	 */
	public void setMin(boolean value) {
		this.min = value;
	}

	/**
	 * Gets the value of the max property.
	 * 
	 */
	public boolean isMax() {
		return this.max;
	}

	/**
	 * Sets the value of the max property.
	 * 
	 */
	public void setMax(boolean value) {
		this.max = value;
	}

	/**
	 * Gets the value of the avg property.
	 * 
	 */
	public boolean isAvg() {
		return this.avg;
	}

	/**
	 * Sets the value of the avg property.
	 * 
	 */
	public void setAvg(boolean value) {
		this.avg = value;
	}

	/**
	 * Gets the value of the sigma property.
	 * 
	 */
	public boolean isSigma() {
		return this.sigma;
	}

	/**
	 * Sets the value of the sigma property.
	 * 
	 */
	public void setSigma(boolean value) {
		this.sigma = value;
	}

	/**
	 * Gets the value of the triggerRefOrdinal property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public Integer getTriggerRefOrdinal() {
		return this.triggerRefOrdinal;
	}

	/**
	 * Sets the value of the triggerRefOrdinal property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setTriggerRefOrdinal(Integer value) {
		this.triggerRefOrdinal = value;
	}

	/**
	 * Gets the value of the sumByTriggerRefOrdinal property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Integer }
	 *     
	 */
	public Integer getSumByTriggerRefOrdinal() {
		return this.sumByTriggerRefOrdinal;
	}

	/**
	 * Sets the value of the sumByTriggerRefOrdinal property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Integer }
	 *     
	 */
	public void setSumByTriggerRefOrdinal(Integer value) {
		this.sumByTriggerRefOrdinal = value;
	}

	/**
	 * Gets the value of the sumTriggerText property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getSumTriggerText() {
		return this.sumTriggerText;
	}

	/**
	 * Sets the value of the sumTriggerText property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setSumTriggerText(String value) {
		this.sumTriggerText = value;
	}

	/**
	 * Gets the value of the countByTrigger property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link Boolean }
	 *     
	 */
	public Boolean isCountByTrigger() {
		return this.countByTrigger;
	}

	/**
	 * Sets the value of the countByTrigger property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link Boolean }
	 *     
	 */
	public void setCountByTrigger(Boolean value) {
		this.countByTrigger = value;
	}

	/**
	 * Gets the value of the countTriggerText property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getCountTriggerText() {
		return this.countTriggerText;
	}

	/**
	 * Sets the value of the countTriggerText property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setCountTriggerText(String value) {
		this.countTriggerText = value;
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

	/**
	 * Gets the value of the sumTriggerTimeText property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getSumTriggerTimeText() {
		return this.sumTriggerTimeText;
	}

	/**
	 * Sets the value of the sumTriggerTimeText property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setSumTriggerTimeText(String value) {
		this.sumTriggerTimeText = value;
	}

	/**
	 * Gets the value of the ratioRefOrdinal property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link BigInteger }
	 *     
	 */
	public Integer getRatioRefOrdinal() {
		return this.ratioRefOrdinal;
	}

	/**
	 * Sets the value of the ratioRefOrdinal property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link BigInteger }
	 *     
	 */
	public void setRatioRefOrdinal(Integer value) {
		this.ratioRefOrdinal = value;
	}

	/**
	 * Gets the value of the ratioComment property.
	 * 
	 * @return
	 *     possible object is
	 *     {@link String }
	 *     
	 */
	public String getRatioText() {
		return this.ratioText;
	}

	/**
	 * Sets the value of the ratioComment property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link String }
	 *     
	 */
	public void setRatioText(String value) {
		this.ratioText = value;
	}

}
