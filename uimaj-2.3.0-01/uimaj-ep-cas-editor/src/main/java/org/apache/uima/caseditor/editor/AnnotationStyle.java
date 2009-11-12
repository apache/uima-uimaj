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

package org.apache.uima.caseditor.editor;

import java.awt.Color;


/**
 * The <code>AnnotationStyle</code> describes the look of an certain annotation type in the
 * <code>AnnotationEditor</code>.
 */
public final class AnnotationStyle {
  /**
   * The styles that can be used to draw an annotation.
   */
  public enum Style {

    /**
     * The background color style.
     */
    BACKGROUND,

    /**
     * The text color style.
     */
    TEXT_COLOR,

    /**
     * The token style.
     */
    TOKEN,

    /**
     * The squiggles style.
     */
    SQUIGGLES,

    /**
     * The box style.
     */
    BOX,

    /**
     * The underline style.
     */
    UNDERLINE,

    /**
     * The bracket style.
     */
    BRACKET
  }

  /**
   * The default <code>DrawingStyle<code>.
   */
  public static final Style DEFAULT_STYLE = Style.SQUIGGLES;

  /**
   * The default drawing color.
   */
  public static final Color DEFAULT_COLOR = new Color(0xff, 0, 0);

  public static final int DEFAULT_LAYER = 0;

  private final String annotation;

  private final Style style;

  private final Color color;

  private final int layer;

  /**
   * Initialize a new instance.
   *
   * @param annotation -
   *          the annotation type
   * @param style -
   *          the drawing style
   * @param color -
   *          annotation color
   *
   * @param layer - drawing layer
   */
  public AnnotationStyle(String annotation, Style style, Color color, int layer) {

    if (annotation == null || style == null || color == null) {
      throw new IllegalArgumentException("parameters must be not null!");
    }

    this.annotation = annotation;
    this.style = style;
    this.color = color;

    if (layer < 0) {
      throw new IllegalArgumentException("layer must be a positive or zero");
    }

    this.layer = layer;

  }

  /**
   * Retrieves the annotation type.
   *
   * @return - annotation type.
   */
  public String getAnnotation() {
    return annotation;
  }

  /**
   * Retrieves the drawing style of the annotation.
   *
   * @return - annotation drawing style
   */
  public Style getStyle() {
    return style;
  }

  /**
   * Retrieves the color of the annotation.
   *
   * @return - annotation color
   */
  public Color getColor() {
    return color;
  }

  /**
   * Retrieves the drawing layer.
   *
   * @return
   */
  public int getLayer() {
    return layer;
  }

  /**
   * Compares if current is equal to another object.
   */
  @Override
  public boolean equals(Object object) {
    boolean isEqual;

    if (object == this) {
      isEqual = true;
    } else if (object instanceof AnnotationStyle) {
      AnnotationStyle style = (AnnotationStyle) object;

      isEqual = annotation.equals(style.annotation) && this.style.equals(style.style)
              && color.equals(style.color) && layer == style.layer;
    } else {
      isEqual = false;
    }

    return isEqual;
  }

  /**
   * Generates a hash code using of toString()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * Represents this object as string.
   */
  @Override
  public String toString() {
    String annotationStyle = "Type: " + annotation;
    annotationStyle += " Style: " + getStyle().name();
    annotationStyle += " Color: " + getColor().toString();
    annotationStyle += " Layer: " + getLayer();
    return annotationStyle;
  }
}
