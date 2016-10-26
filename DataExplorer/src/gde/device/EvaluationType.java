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

import gde.GDE;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for EvaluationType complex type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EvaluationType", propOrder = { "calculation" })
public class EvaluationType implements Cloneable {

	protected CalculationType	calculation;
	@XmlAttribute(required = true)
	protected boolean				avg;
	@XmlAttribute(required = true)
	protected boolean				first;
	@XmlAttribute(required = true)
	protected boolean				last;
	@XmlAttribute(required = true)
	protected boolean				min;
	@XmlAttribute(required = true)
	protected boolean				max;
	@XmlAttribute(required = true)
	protected boolean				sigma;
	@XmlAttribute(required = true)
	protected boolean				sum;
	@XmlAttribute
	protected String				comment;

	/**
	 * default constructor
	 */
	public EvaluationType() {
		// ignore
	}

	/**
	 * copy constructor
	 * @param evaluation
	 */
	private EvaluationType(EvaluationType evaluation) {
		this.calculation = evaluation.calculation;
		this.avg = evaluation.avg;
		this.first = evaluation.first;
		this.last = evaluation.last;
		this.min = evaluation.min;
		this.max = evaluation.max;
		this.sigma = evaluation.sigma;
		this.sum = evaluation.sum;
		this.comment = evaluation.comment;
	}

	/**
	 * clone method - calls the private copy constructor
	 */
	@Override
	public EvaluationType clone() {
		try {
			super.clone();
		} catch (CloneNotSupportedException e) {
			// ignore
		}
		return new EvaluationType(this);
	}

	/**
	 * Gets the value of the calculation property.
	 * 
	 * @return
	 *     possible object is
	  *     {@link CalculationType }
	 *     
	 */
	public CalculationType getCalculation() {
		return this.calculation;
	}

	/**
	 * Sets the value of the calculation property.
	 * 
	 * @param value
	 *     allowed object is
	 *     {@link CalculationType }
	 *     
	 */
	public void setCalculation(CalculationType value) {
		this.calculation = value;
	}

	/**
	 * Gets the value of the calculation property.
	 */
	public void removeCalculation() {
		this.calculation = null;
	}

	/**
	 * Gets the value of the avg property.
	 * 
	 */
	public boolean isAvg() {
		return this.avg;
	}

	/**
	 * Gets the value of the first property.
	 * 
	 */
	public boolean isFirst() {
		return this.first;
	}

	/**
	 * Sets the value of the first property.
	 * 
	 */
	public void setFirst(boolean value) {
		this.first = value;
	}

	/**
	 * Gets the value of the first property.
	 * 
	 */
	public boolean isLast() {
		return this.last;
	}

	/**
	 * Sets the value of the first property.
	 * 
	 */
	public void setlast(boolean value) {
		this.last = value;
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
	 * Gets the value of the sum property.
	 * 
	 */
	public boolean isSum() {
		return this.sum;
	}

	/**
	 * Sets the value of the max property.
	 * 
	 */
	public void setSum(boolean value) {
		this.sum = value;
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
	 * serialize evaluation type as string
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		// <statistics min="true" max="true" avg="true" sigma="true" countByTrigger="true" countTriggerText="Anzahl Motorsteigflüge:" sumTriggerTimeText="Gesamte Motorlaufzeit:">
		// <statistics min="true" max="true" avg="true" sigma="true" sumByTriggerRefOrdinal="9" sumTriggerText="Mit Motorsteigflügen erreichte Höhe" ratioRefOrdinal="10" ratioText="Verbrauchte Kapazität/Höhen-Meter:"/>
		// <trigger level="3000" isGreater="true" minTimeSec="10" comment="Motorstromtrigger: &gt;3A, &gt;10 Sekunden"/>
		sb.append(String.format("evaluation sum=%b min=%b max=%b avg=%b sigma=%b", this.sum, this.min, this.max, this.avg, this.sigma));
		if (this.comment != null)
			sb.append(String.format(" comment=%s", this.comment.replaceAll(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR)));

		if (this.calculation != null) {
			sb.append(String.format(" calculation refOrdinal1=%s refOrdinal1=%s", this.calculation.refOrdinal, this.calculation.refOrdinalDivisor));
			if (this.calculation.comment != null)
				sb.append(String.format(" comment=%s", this.calculation.comment.replaceAll(GDE.STRING_BLANK, GDE.STRING_UNDER_BAR)));
		}

		return sb.toString();
	}

	/**
	 * parse stringified evaluation to EvaluationType
	 * @param evaluationAsText
	 * @return
	 */
	public static EvaluationType fromString(String evaluationAsText) {
		EvaluationType evaluation = new EvaluationType();
		String[] tmpEvaluation = evaluationAsText.split(" calculation ");
		if (tmpEvaluation.length == 2) { // evaluation contains a defined calculation
			CalculationType tmpCalculation = new CalculationType();
			for (String property : tmpEvaluation[1].split(GDE.STRING_BLANK)) {
				String[] props = property.split(GDE.STRING_EQUAL);
				if (props.length == 2) { // contains =
					if (props[0].equals("refOrdinal1"))
						tmpCalculation.refOrdinal = Integer.valueOf(props[1]);
					else if (props[0].equals("refOrdinal2"))
						tmpCalculation.refOrdinalDivisor = Integer.valueOf(props[1]);
					else if (props[0].equals("comment"))
						tmpCalculation.comment = props[1].replace(GDE.STRING_UNDER_BAR, GDE.STRING_BLANK);
				}
			}
			evaluation.setCalculation(tmpCalculation);
		}
		String strEvaluation = tmpEvaluation.length == 2 ? tmpEvaluation[0] : evaluationAsText;
		for (String property : strEvaluation.split(GDE.STRING_BLANK)) {
			String[] props = property.split(GDE.STRING_EQUAL);
			if (props.length == 2) { // contains =
				if (props[0].equals("sum"))
					evaluation.sum = Boolean.valueOf(props[1]);
				else if (props[0].equals("min"))
					evaluation.min = Boolean.valueOf(props[1]);
				else if (props[0].equals("max"))
					evaluation.max = Boolean.valueOf(props[1]);
				else if (props[0].equals("avg"))
					evaluation.avg = Boolean.valueOf(props[1]);
				else if (props[0].equals("sigma"))
					evaluation.sigma = Boolean.valueOf(props[1]);
				else if (props[0].equals("first"))
					evaluation.first = Boolean.valueOf(props[1]);
				else if (props[0].equals("last"))
					evaluation.last = Boolean.valueOf(props[1]);
				else if (props[0].equals("comment"))
					evaluation.comment = props[1].replace(GDE.STRING_UNDER_BAR, GDE.STRING_BLANK);
			}
		}

		return evaluation;
	}
}
