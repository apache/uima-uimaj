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

package org.apache.uima.tools.stylemap;

import java.awt.Color;

public class StyleMapEntry {
  /**
   * The name of the annotation type to which this style map entry applies.
   */
  private String annotationTypeName;

  /**
   * Label that identifies the type of annotation. This will appear in the legend in the annotation
   * viewer.
   */
  private String label;

  /**
   * Feature value. Features with this value will adopt corresponding style.
   */

  private String featureValue;

  /**
   * Foreground color used to display this type of annotation.
   */
  private Color foreground;

  /**
   * Background color used to display this type of annotation.
   */
  private Color background;

  private boolean isChecked; // true if element is to be checked

  private boolean isHidden;

  public String toString() {
    return "(" + annotationTypeName + ";" + label + ";" + featureValue + ";"
            + Integer.toHexString(foreground.getRGB()) + ";"
            + Integer.toHexString(background.getRGB())

            + ")";
  }

  public boolean getHidden() {
    return isHidden;
  }

  public void setHidden(Boolean hid) {
    isHidden = hid.booleanValue();
  }

  public boolean getChecked() {
    return isChecked;
  }

  public void setChecked(Boolean chk) {
    isChecked = chk.booleanValue();
  }

  /**
   * This method returns a pattern representing either simply an annotation type or else an
   * annotation type/feature value. In the case of the latter: e.g.
   * SYNTAX_ANNOT_TYPE[@SYNTAXLABEL_STRING='NP']
   */

  public String getPattern() {
    int colonIndex = annotationTypeName.indexOf(':');
    if (colonIndex == -1) {
      return annotationTypeName;
    } else {
      String annotationType = annotationTypeName.substring(0, colonIndex);
      String featureName = annotationTypeName.substring(colonIndex + 1);
      return annotationType + "[@" + featureName + "='" + featureValue + "']";
    }
  }

  /**
   * @return Returns the background.
   */
  public Color getBackground() {
    return background;
  }

  /**
   * @param background
   *          The background to set.
   */
  public void setBackground(Color background) {
    this.background = background;
  }

  /**
   * @return Returns the featureValue.
   */
  public String getFeatureValue() {
    return featureValue;
  }

  /**
   * @param featureValue
   *          The featureValue to set.
   */
  public void setFeatureValue(String featureValue) {
    this.featureValue = featureValue;
  }

  /**
   * @return Returns the foreground.
   */
  public Color getForeground() {
    return foreground;
  }

  /**
   * @param foreground
   *          The foreground to set.
   */
  public void setForeground(Color foreground) {
    this.foreground = foreground;
  }

  /**
   * @return Returns the label.
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label
   *          The label to set.
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * @return Returns the annotationTypeName.
   */
  public String getAnnotationTypeName() {
    return annotationTypeName;
  }

  /**
   * @param annotationTypeName
   *          The annotationTypeName to set.
   */
  public void setAnnotationTypeName(String annotationTypeName) {
    this.annotationTypeName = annotationTypeName;
  }

}
