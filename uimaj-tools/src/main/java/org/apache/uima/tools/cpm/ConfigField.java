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

package org.apache.uima.tools.cpm;

import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTextField;

import org.apache.uima.tools.util.gui.FileSelector;
import org.apache.uima.tools.util.gui.ListSelector;

/**
 * Configuration Field class used for representation of resource parameters in dynamically created
 * MetaDataPanels.
 * 
 * @see org.apache.uima.tools.cpm.MetaDataPanel
 */
public class ConfigField {
  private String parameterName;

  private String classString;

  private boolean isMultiValued;

  private JComponent fieldComponent;

  private Object originalValue;

  private Object lastSavedValue;

  /**
   * @param pn
   *          Resource parameter name e.g. outputDir
   * @param cs
   *          String value of Java type used for value e.g. Integer
   * @param mv
   *          true if the value is a multi-value array
   * @param c
   *          Component used to represent field - Could be JTextField, JCheckBox, FileSelector or
   *          ListSelector
   */
  public ConfigField(String pn, String cs, boolean mv, JComponent c) {
    parameterName = pn;
    classString = cs;
    isMultiValued = mv;
    fieldComponent = c;
    originalValue = getFieldValue();
    lastSavedValue = originalValue;
  }

  public String getParameterName() {
    return parameterName;
  }

  public String getClassString() {
    return classString;
  }

  public JComponent getFieldComponent() {
    return fieldComponent;
  }

  public boolean isMultiValued() {
    return isMultiValued;
  }

  public Object getFieldValue() {
    if (fieldComponent instanceof JTextField) {
      String fieldString = ((JTextField) fieldComponent).getText();
      if (classString.equals("Integer")) {
        try {
          return Integer.valueOf(fieldString);
        } catch (NumberFormatException e) {
          return fieldString;
        }
      } else if (classString.equals("Float")) {
        try {
          return Float.valueOf(fieldString);
        } catch (NumberFormatException e) {
          return fieldString;
        }
      } else
        return fieldString;
    } else if (fieldComponent instanceof JCheckBox)
      return Boolean.valueOf(((JCheckBox) fieldComponent).isSelected());
    else if (fieldComponent instanceof FileSelector)
      return ((FileSelector) fieldComponent).getSelected();
    else if (fieldComponent instanceof ListSelector) {
      String[] valueStrings = ((ListSelector) fieldComponent).getValues();
      if (valueStrings == null) {
        return null;
      }
      try {
        if (classString.equals("Integer")) {
          Integer[] intValues = new Integer[valueStrings.length];
          for (int i = 0; i < valueStrings.length; i++) {
            intValues[i] = Integer.valueOf(valueStrings[i]);
          }
          return intValues;
        } else if (classString.equals("Float")) {
          Float[] floatValues = new Float[valueStrings.length];
          for (int i = 0; i < valueStrings.length; i++) {
            floatValues[i] = Float.valueOf(valueStrings[i]);
          }
          return floatValues;
        } else {
          return valueStrings;
        }
      } catch (NumberFormatException e) {
        return valueStrings;
      }
    } else
      return null;
  }

  public void setFieldValue(Object fieldValue) {
    if (fieldComponent instanceof JTextField) {
      ((JTextField) fieldComponent).setText(fieldValue.toString());
    } else if (fieldComponent instanceof JCheckBox) {
      boolean onOff = "true".equalsIgnoreCase(fieldValue.toString());
      ((JCheckBox) fieldComponent).setSelected(onOff);
    } else if (fieldComponent instanceof FileSelector)
      ((FileSelector) fieldComponent).setSelected(fieldValue.toString());
    else if (isMultiValued) {
      Object[] vals;
      if (fieldValue instanceof Object[]) {
        vals = (Object[]) fieldValue;
      } else if (fieldValue == null) {
        vals = new Object[0];
      } else {
        vals = new Object[] { fieldValue };
      }
      ((ListSelector) fieldComponent).populate(vals);
    }
  }

  /**
   * Returns whether this field has been modified from its original value. This is not affected by
   * whether the user has saved the new value; for that use isDirty().
   */
  public boolean isModified() {
    Object currentValue = getFieldValue();
    if (originalValue == null) {
      return (currentValue != null);
    }
    if (originalValue instanceof Object[] && currentValue instanceof Object[]) {
      return !Arrays.equals((Object[]) originalValue, (Object[]) currentValue);
    } else {
      return !originalValue.equals(currentValue);
    }
  }

  /**
   * Returns whether this field has been modified since the last time the CPE descriptor was saved.
   */
  public boolean isDirty() {
    Object currentValue = getFieldValue();
    if (lastSavedValue == null) {
      return (currentValue != null);
    }
    if (lastSavedValue instanceof Object[] && currentValue instanceof Object[]) {
      return !Arrays.equals((Object[]) lastSavedValue, (Object[]) currentValue);
    } else {
      return !lastSavedValue.equals(currentValue);
    }
  }

  /**
   * To be called when the CPE descriptor is saved. Sets this field to be not dirty, until it is
   * next modified.
   */
  public void clearDirty() {
    lastSavedValue = getFieldValue();
  }

}
