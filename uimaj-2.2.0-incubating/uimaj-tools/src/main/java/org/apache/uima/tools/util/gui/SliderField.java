/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.tools.util.gui;

import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SliderField extends JPanel implements ChangeListener, PropertyChangeListener {
  private static final long serialVersionUID = 8200124988260091779L;

  private JSlider slider;

  private IntegerField textField;

  public SliderField(int min, int max, int initialValue) {
    setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));

    slider = new JSlider(min, max, initialValue);

    // Try to determine sensible tick spacing:
    int majorTickSpacing = determineMajorTickSpacing(max - min);

    slider.setMajorTickSpacing(majorTickSpacing);
    slider.setPaintTicks(true);

    slider.addChangeListener(this);
    add(slider);

    textField = new IntegerField(min, max, initialValue);
    textField.setColumns(5);
    textField.addPropertyChangeListener(this);

    add(textField);
  }

  public void stateChanged(ChangeEvent e) {
    int sliderValue = slider.getValue();
    textField.setValue(new Integer(sliderValue));

    if (!slider.getValueIsAdjusting())
      textField.setValue(new Integer(sliderValue));
    else
      // value is adjusting; just set the text
      textField.setText(String.valueOf(sliderValue));
  }

  public void propertyChange(PropertyChangeEvent e) {
    if (e.getPropertyName().equals("value")) {
      Number value = (Number) e.getNewValue();
      if (slider != null && value != null)
        slider.setValue(value.intValue());
    }
  }

  public void setEnabled(boolean onOff) {
    slider.setEnabled(onOff);
    textField.setEnabled(onOff);
  }

  public int getValue() {
    Integer value = (Integer) textField.getValue();
    return value.intValue();
  }

  private int determineMajorTickSpacing(int range) {
    if (range > 1000000)
      return 1000000;
    else if (range > 100000)
      return 100000;
    else if (range > 10000)
      return 10000;
    else if (range > 1000)
      return 1000;
    else if (range > 100)
      return 100;
    else
      return 10;
  }
}
