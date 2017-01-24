//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1.5-b01-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2017.01.24 at 05:52:20 PM MEZ 
//


package gde.histocache;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import gde.GDE;


/**
 * holds either measurements or settlements
 * 			
 * 
 * <p>Java class for compartmentsType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="compartmentsType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="compartment" type="{}compartmentType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "compartmentsType", propOrder = {
    "compartment"
})
public class CompartmentsType {

    protected List<CompartmentType> compartment;

    /**
     * Gets the value of the compartment property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the compartment property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCompartment().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CompartmentType }
     * 
     * 
     */
    public List<CompartmentType> getCompartment() {
        if (compartment == null) {
            compartment = new ArrayList<CompartmentType>();
        }
        return this.compartment;
    }

  	@Override
  	public String toString() {
  		StringBuilder sb = new StringBuilder();
  		if (this.compartment != null) {
  			for (CompartmentType tmpCompartment : this.compartment) {
  				sb.append(tmpCompartment.getText()).append(GDE.STRING_EQUAL).append(tmpCompartment.getId()).append(GDE.STRING_BLANK);
  			}
  		}
  		return sb.toString();
  	}

}
