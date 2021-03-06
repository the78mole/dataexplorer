//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.19 at 09:19:25 AM MEZ 
//


package gde.device;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import gde.GDE;


/**
 * <p>Java class for TransitionAmountType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TransitionAmountType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="transitionGroupId" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="amountType" use="required" type="{}amount_types" />
 *       &lt;attribute name="unsigned" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="refOrdinal" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="leveling" use="required" type="{}leveling_types" />
 *       &lt;attribute name="comment" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransitionAmountType")
public class TransitionAmountType {

    @XmlAttribute(required = true)
    protected int transitionGroupId;
    @XmlAttribute(required = true)
    protected AmountTypes amountType;
    @XmlAttribute(required = true)
    protected boolean unsigned;
    @XmlAttribute(required = true)
    protected int refOrdinal;
    @XmlAttribute(required = true)
    protected LevelingTypes leveling;
    @XmlAttribute
    protected String comment;

    /**
     * Gets the value of the transitionGroupId property.
     * 
     */
    public int getTransitionGroupId() {
        return transitionGroupId;
    }

    /**
     * Sets the value of the transitionGroupId property.
     * 
     */
    public void setTransitionGroupId(int value) {
        this.transitionGroupId = value;
    }

    /**
     * Gets the value of the amountType property.
     * 
     * @return
     *     possible object is
     *     {@link AmountTypes }
     *     
     */
    public AmountTypes getAmountType() {
        return amountType;
    }

    /**
     * Sets the value of the amountType property.
     * 
     * @param value
     *     allowed object is
     *     {@link AmountTypes }
     *     
     */
    public void setAmountType(AmountTypes value) {
        this.amountType = value;
    }

    /**
     * Gets the value of the unsigned property.
     * 
     */
    public boolean isUnsigned() {
        return unsigned;
    }

    /**
     * Sets the value of the unsigned property.
     * 
     */
    public void setUnsigned(boolean value) {
        this.unsigned = value;
    }

    /**
     * Gets the value of the refOrdinal property.
     * 
     */
    public int getRefOrdinal() {
        return refOrdinal;
    }

    /**
     * Sets the value of the refOrdinal property.
     * 
     */
    public void setRefOrdinal(int value) {
        this.refOrdinal = value;
    }

    /**
     * Gets the value of the leveling property.
     * 
     * @return
     *     possible object is
     *     {@link LevelingTypes }
     *     
     */
    public LevelingTypes getLeveling() {
        return leveling;
    }

    /**
     * Sets the value of the leveling property.
     * 
     * @param value
     *     allowed object is
     *     {@link LevelingTypes }
     *     
     */
    public void setLeveling(LevelingTypes value) {
        this.leveling = value;
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
        return comment;
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

  	@Override
  	public String toString() {
  		StringBuilder sb = new StringBuilder();
  		sb.append("transitionGroupId=").append(this.getTransitionGroupId()).append(GDE.STRING_COMMA_BLANK).append("amountType=").append(this.getAmountType().value()).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$ //$NON-NLS-2$
  		sb.append("unsigned=").append(this.isUnsigned()).append(GDE.STRING_COMMA_BLANK).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$ 
  		sb.append("refOrdinal=").append(this.getRefOrdinal()).append(GDE.STRING_OR).append(this.getLeveling() != null ? this.getLeveling().value() : GDE.STRING_EMPTY).append(GDE.STRING_COMMA_BLANK); //$NON-NLS-1$
  		return sb.toString();
  	}

}
