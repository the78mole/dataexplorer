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
import gde.GDE;
import gde.device.DataTypes;
import gde.utils.StringHelper;

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
import org.eclipse.swt.widgets.Event;
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

	int									value					= 0;
	int									offset				= 0;

	/**
	 * create a parameter configuration control for number values with factor and offset, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param parameterName
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param isTextValueEditable
	 * @param textFieldWidth
	 * @param sliderWidth
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 * @param sliderFactor
	 * @param sliderOffset
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String parameterName, final int nameWidth, final String parameterDescription,
			final int descriptionWidth, final boolean isTextValueEditable, final int textFieldWidth, final int sliderWidth, final int sliderMinValue, final int sliderMaxValue, final int sliderOffset) {
		this.value = valueArray[valueIndex];
		this.offset = sliderOffset;
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		this.baseComposite.setLayout(group1Layout);
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight - 5;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(isTextValueEditable);
			this.text.setBackground(SWTResourceManager.getColor(isTextValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			if (isTextValueEditable) {
				this.text.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "text.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
				this.text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
						ParameterConfigControl.this.value = Integer.parseInt(ParameterConfigControl.this.text.getText());
						if (ParameterConfigControl.this.value < sliderMinValue) {
							ParameterConfigControl.this.value = sliderMinValue;
							ParameterConfigControl.this.text.setText(String.format("%d", ParameterConfigControl.this.value)); //$NON-NLS-1$
						}
						if (ParameterConfigControl.this.value > sliderMaxValue) {
							ParameterConfigControl.this.value = sliderMaxValue;
							ParameterConfigControl.this.text.setText(String.format("%d", ParameterConfigControl.this.value)); //$NON-NLS-1$
						}
						valueArray[valueIndex] = ParameterConfigControl.this.value;
						ParameterConfigControl.this.slider.setSelection(ParameterConfigControl.this.value + ParameterConfigControl.this.offset);
						ParameterConfigControl.this.slider.notifyListeners(SWT.Selection, new Event());
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}

					@Override
					public void keyPressed(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
					}
				});
			}
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(sliderMinValue + this.offset);
			this.slider.setMaximum(sliderMaxValue + this.offset + 10);
			this.slider.setIncrement((sliderMaxValue + this.offset) >= 1000 ? 10 : 1);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(java.util.logging.Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection() - ParameterConfigControl.this.offset;
					ParameterConfigControl.this.text.setText(String.format("%d", ParameterConfigControl.this.value)); //$NON-NLS-1$
					valueArray[valueIndex] = ParameterConfigControl.this.value;
					Event changeEvent = new Event();
					changeEvent.index = valueIndex;
					parent.notifyListeners(SWT.Selection, changeEvent);
				}
			});
		}
	}

	/**
	 * create a parameter configuration control for number values with factor and without offset, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param parameterName
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param isTextValueEditable
	 * @param textFieldWidth
	 * @param sliderWidth
	 * @param sliderMinValue
	 * @param sliderMaxValue
	 * @param sliderFactor
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String parameterName, final int nameWidth, final String parameterDescription,
			final int descriptionWidth, final boolean isTextValueEditable, final int textFieldWidth, final int sliderWidth, final int sliderMinValue, final int sliderMaxValue) {
		this.value = valueArray[valueIndex];
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		this.baseComposite.setLayout(group1Layout);
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight - 5;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(isTextValueEditable);
			this.text.setBackground(SWTResourceManager.getColor(isTextValueEditable ? SWT.COLOR_WHITE : SWT.COLOR_WIDGET_LIGHT_SHADOW));
			if (isTextValueEditable) {
				this.text.addVerifyListener(new VerifyListener() {
					@Override
					public void verifyText(VerifyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "text.verifyText, event=" + evt); //$NON-NLS-1$
						evt.doit = StringHelper.verifyTypedInput(DataTypes.INTEGER, evt.text);
					}
				});
				this.text.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "text.keyReleased, event=" + evt); //$NON-NLS-1$
						ParameterConfigControl.this.value = Integer.parseInt(ParameterConfigControl.this.text.getText());
						if (ParameterConfigControl.this.value < sliderMinValue) {
							ParameterConfigControl.this.value = sliderMinValue;
							ParameterConfigControl.this.text.setText(String.format("%d", ParameterConfigControl.this.value)); //$NON-NLS-1$
						}
						if (ParameterConfigControl.this.value > sliderMaxValue) {
							ParameterConfigControl.this.value = sliderMaxValue;
							ParameterConfigControl.this.text.setText(String.format("%d", ParameterConfigControl.this.value)); //$NON-NLS-1$
						}
						valueArray[valueIndex] = ParameterConfigControl.this.value;
						ParameterConfigControl.this.slider.setSelection(ParameterConfigControl.this.value);
						ParameterConfigControl.this.slider.notifyListeners(SWT.Selection, new Event());
						Event changeEvent = new Event();
						changeEvent.index = valueIndex;
						parent.notifyListeners(SWT.Selection, changeEvent);
					}

					@Override
					public void keyPressed(KeyEvent evt) {
						log.log(java.util.logging.Level.FINEST, "text.keyPressed, event=" + evt); //$NON-NLS-1$
					}
				});
			}
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(sliderMinValue);
			this.slider.setMaximum(sliderMaxValue + 10);
			this.slider.setIncrement(sliderMaxValue >= 1000 ? 10 : 1);
			this.slider.setSelection(this.value);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(java.util.logging.Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection();
					ParameterConfigControl.this.text.setText(String.format("%d", ParameterConfigControl.this.value)); //$NON-NLS-1$
					valueArray[valueIndex] = ParameterConfigControl.this.value;
					Event changeEvent = new Event();
					changeEvent.index = valueIndex;
					parent.notifyListeners(SWT.Selection, changeEvent);
				}
			});
		}
	}

	/**
	 * create a parameter configuration control for values array, where the array size drives the slider, calculate with total height of 25 to 30
	 * @param parent
	 * @param valueArray
	 * @param valueIndex
	 * @param nameWidth
	 * @param parameterDescription
	 * @param descriptionWidth
	 * @param textFiledValues
	 * @param textFieldWidth
	 * @param sliderWidth
	 */
	public ParameterConfigControl(final Composite parent, final int[] valueArray, final int valueIndex, final String parameterName, final int nameWidth, final String parameterDescription,
			final int descriptionWidth, final String[] textFiledValues, final int textFieldWidth, final int sliderWidth) {
		this.value = valueArray[valueIndex];
		this.baseComposite = new Composite(parent, SWT.NONE);
		RowLayout group1Layout = new RowLayout(org.eclipse.swt.SWT.HORIZONTAL);
		this.baseComposite.setLayout(group1Layout);
		{
			this.nameLabel = new CLabel(this.baseComposite, SWT.RIGHT);
			RowData nameLabelLData = new RowData();
			nameLabelLData.width = nameWidth;
			nameLabelLData.height = this.controlHeight;
			this.nameLabel.setLayoutData(nameLabelLData);
			this.nameLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.nameLabel.setText(parameterName);
		}
		{
			this.text = new Text(this.baseComposite, SWT.CENTER | SWT.BORDER);
			RowData textLData = new RowData();
			textLData.width = textFieldWidth;
			textLData.height = this.controlHeight - 5;
			this.text.setLayoutData(textLData);
			this.text.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.text.setEditable(false);
			this.text.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
		}
		{
			this.descriptionLabel = new CLabel(this.baseComposite, SWT.LEFT);
			RowData descriptionLabelLData = new RowData();
			descriptionLabelLData.width = descriptionWidth;
			descriptionLabelLData.height = this.controlHeight;
			this.descriptionLabel.setLayoutData(descriptionLabelLData);
			this.descriptionLabel.setFont(SWTResourceManager.getFont(GDE.WIDGET_FONT_NAME, GDE.WIDGET_FONT_SIZE, SWT.NORMAL));
			this.descriptionLabel.setText(parameterDescription);
		}
		{
			RowData sliderLData = new RowData();
			sliderLData.width = sliderWidth;
			sliderLData.height = this.controlHeight;
			this.slider = new Slider(this.baseComposite, SWT.NONE);
			this.slider.setLayoutData(sliderLData);
			this.slider.setMinimum(0);
			this.slider.setMaximum(textFiledValues.length < 10 ? 10 + textFiledValues.length - 1 : textFiledValues.length + 1);
			this.slider.setSelection(this.value);
			this.slider.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					log.log(java.util.logging.Level.FINEST, "slider.widgetSelected, event=" + evt); //$NON-NLS-1$
					ParameterConfigControl.this.value = ParameterConfigControl.this.slider.getSelection();
					ParameterConfigControl.this.text.setText(textFiledValues[ParameterConfigControl.this.value]);
					valueArray[valueIndex] = ParameterConfigControl.this.value;
					Event changeEvent = new Event();
					changeEvent.index = valueIndex;
					parent.notifyListeners(SWT.Selection, changeEvent);
				}
			});
		}
	}

	public void setVisible(boolean enable) {
		this.nameLabel.setVisible(enable);
		this.text.setEnabled(enable);
		this.descriptionLabel.setVisible(enable);
		this.slider.setEnabled(enable);
	}

	public ParameterConfigControl dispose() {
		this.baseComposite.dispose();
		return null;
	}

	/**
	 * set the slider selection index
	 * @param useValue the value to be set in dependency of factor and offset if applicable
	 */
	public void setSliderSelection(int useValue) {
		this.value = useValue;
		if (!this.slider.isDisposed()) {
			this.slider.setSelection(this.value + this.offset);
			this.slider.notifyListeners(SWT.Selection, new Event());
		}
	}

	/**
	 * get the slider selection index
	 */
	public int getSliderSelectionIndex() {
		return this.slider.getSelection();
	}

	/**
	 * @return the slider
	 */
	public Slider getSlider() {
		return this.slider;
	}

	/**
	 * redraw the control widget
	 */
	public void redraw() {
		this.baseComposite.redraw();
	}
}
