package gde.ui;
/**************************************************************************************
This file is part of GNU DataExplorer.

GNU DataExplorer is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

GNU DataExplorer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

Copyright (c) 2011 Winfried Bruegmann
****************************************************************************************/
import gde.log.Level;

import java.util.logging.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Text;

public class ParameterConfigControl {
	final static Logger	log						= Logger.getLogger(ParameterConfigControl.class.getName());
	final Composite			baseComposite;
	final CLabel				nameLabel, descriptionLabel;
	Composite						separator;
	final Text					text;
	final Slider				slider;

	int									controlHeight	= 20;
	
	/**
	 * create a parameter configuration group, calculate with total height of 30
	 * @param parent
	 * @param valueArray
	 * @param valuePosition
	 * @param parameterName
	 * @param parameterDescription
	 * @param isValueEditable
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 * @param sliderFactor
	 * @param sliderOffset
	 */
	public ParameterConfigControl(Composite parent, final int[] valueArray, final int valuePosition, final String parameterName, final String parameterDescription, final boolean isValueEditable,
			final int sliderMinValue, final int sliderMaxValue, final double sliderFactor, final int sliderOffset) {
		RowData separatorLData = new RowData();
		separatorLData.width = 5;
		separatorLData.height = controlHeight;

		baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		baseComposite.setLayout(group1Layout);
		{
			new Composite(baseComposite, SWT.NONE).setLayoutData(separatorLData);
		}
		{
			nameLabel = new CLabel(baseComposite, SWT.NONE);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = 150;
			nameLabelLData.height = controlHeight;
			nameLabel.setLayoutData(nameLabelLData);
			nameLabel.setText(parameterName);
		}
		{
			text = new Text(baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = 50;
			textLData.height = controlHeight - 5;
			text.setLayoutData(textLData);
			text.setText(String.format("%d", valueArray[valuePosition]));
			text.setEditable(isValueEditable);
			text.setBackground(SWTResourceManager.getColor(isValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			if (isValueEditable) {
				text.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						log.log(Level.FINEST, "text.verifyText, event=" + evt);
						evt.doit = true; //TODO
					}
				});
				text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						log.log(Level.FINEST, "text.keyPressed, event=" + evt);
						valueArray[valuePosition] = Integer.parseInt(text.getText());
					}
				});
			}
		}
		{
			new Composite(baseComposite, SWT.NONE).setLayoutData(separatorLData);
		}
		{
			descriptionLabel = new CLabel(baseComposite, SWT.NONE);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = 150;
			descriptionLabelLData.height = controlHeight;
			descriptionLabel.setLayoutData(descriptionLabelLData);
			descriptionLabel.setText(parameterDescription);
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = 150;
			sliderLData.height = controlHeight;
			slider = new Slider(baseComposite, SWT.NONE);
			slider.setLayoutData(sliderLData);
			slider.setMinimum(sliderMinValue);
			slider.setMaximum(sliderMaxValue);
			System.out.println(valueArray[valuePosition] + " - " + (int) ((valueArray[valuePosition] - sliderOffset) / sliderFactor));
			slider.setSelection((int) ((valueArray[valuePosition] - sliderOffset) / sliderFactor));
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "slider.widgetSelected, event=" + evt);
					valueArray[valuePosition] = (int) (slider.getSelection() * sliderFactor + sliderOffset);
					text.setText(String.format("%d", valueArray[valuePosition]));
				}
			});
		}
		}

	/**
	 * create a parameter configuration group, calculate with total height of 30
	 * @param parent
	 * @param valueArray
	 * @param valuePosition
	 * @param parameterName
	 * @param parameterDescription
	 * @param isValueEditable
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 * @param sliderFactor
	 */
	public ParameterConfigControl(Composite parent, final int[] valueArray, final int valuePosition, final String parameterName, final String parameterDescription, final boolean isValueEditable,
			final int sliderMinValue, final int sliderMaxValue, final double sliderFactor) {
		RowData separatorLData = new RowData();
		separatorLData.width = 5;
		separatorLData.height = controlHeight;

		baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		baseComposite.setLayout(group1Layout);
		{
			new Composite(baseComposite, SWT.NONE).setLayoutData(separatorLData);
		}
		{
			nameLabel = new CLabel(baseComposite, SWT.NONE);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = 150;
			nameLabelLData.height = controlHeight;
			nameLabel.setLayoutData(nameLabelLData);
			nameLabel.setText(parameterName);
		}
		{
			text = new Text(baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = 50;
			textLData.height = controlHeight - 5;
			text.setLayoutData(textLData);
			text.setText(String.format("%d", valueArray[valuePosition]));
			text.setEditable(isValueEditable);
			text.setBackground(SWTResourceManager.getColor(isValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			if (isValueEditable) {
				text.addVerifyListener(new VerifyListener() {
					public void verifyText(VerifyEvent evt) {
						log.log(Level.FINEST, "text.verifyText, event=" + evt);
						evt.doit = true; //TODO
					}
				});
				text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyPressed(KeyEvent evt) {
						log.log(Level.FINEST, "text.keyPressed, event=" + evt);
						valueArray[valuePosition] = Integer.parseInt(text.getText());
					}
				});
			}
		}
		{
			new Composite(baseComposite, SWT.NONE).setLayoutData(separatorLData);
		}
		{
			descriptionLabel = new CLabel(baseComposite, SWT.NONE);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = 150;
			descriptionLabelLData.height = controlHeight;
			descriptionLabel.setLayoutData(descriptionLabelLData);
			descriptionLabel.setText(parameterDescription);
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = 150;
			sliderLData.height = controlHeight;
			slider = new Slider(baseComposite, SWT.NONE);
			slider.setLayoutData(sliderLData);
			slider.setMinimum(sliderMinValue);
			slider.setMaximum(sliderMaxValue);
			System.out.println(valueArray[valuePosition] + " - " + (int) (valueArray[valuePosition] * sliderFactor));
			slider.setSelection((int) (valueArray[valuePosition] / sliderFactor));
			slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(Level.FINEST, "slider.widgetSelected, event=" + evt);
					valueArray[valuePosition] = (int) (slider.getSelection() * sliderFactor);
					text.setText(String.format("%d", valueArray[valuePosition]));
				}
			});
		}
	}
	
	public void setVisible(boolean enable) {
		nameLabel.setVisible(enable);
		text.setEnabled(enable);
		descriptionLabel.setVisible(enable);
		slider.setEnabled(enable);
	}
	
	public ParameterConfigControl dispose() {
		baseComposite.dispose();		
		return null;
	}
}
