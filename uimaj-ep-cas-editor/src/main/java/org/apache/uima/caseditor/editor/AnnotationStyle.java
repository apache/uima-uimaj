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
import java.util.HashMap;
import java.util.Map;

import org.apache.uima.caseditor.core.model.DefaultColors;
import org.eclipse.jface.preference.IPreferenceStore;


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
    BRACKET,
    
    TAG
  }

  /**
   * The default <code>DrawingStyle</code>.
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

  private final String configuration;
  
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
   * 
   * @param configuration the configuration string for the style or null if no configuration
   */
  public AnnotationStyle(String annotation, Style style, Color color, int layer, String configuration) {

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
    this.configuration = configuration;
  }

  public AnnotationStyle(String annotation, Style style, Color color, int layer) {
    this(annotation, style, color, layer, null);
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
   * @return the drawing layer
   */
  public int getLayer() {
    return layer;
  }

  public String getConfiguration() {
    return configuration;
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
      
      boolean isConfigEqual = configuration == style.configuration ||
          (configuration != null ? false : configuration.equals(style.configuration));
      
      isEqual = annotation.equals(style.annotation) && this.style.equals(style.style)
              && color.equals(style.color) && layer == style.layer && isConfigEqual;
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
    annotationStyle += " Config: " + getConfiguration();
    return annotationStyle;
  }
  
  // TODO: Format must be redefined, so that only one key/string pair is needed to save it!
  
  // key -> type name + ."style"
  // value -> key/value pairs -> key=value; key=value;
  // split on ";" and then split on = to parse values into a map
  // maybe make a util which can save a String, String map to a line, and load it from String ...
  private static String serializeProperties(Map<String, String> properties) {
    
    StringBuilder configString = new StringBuilder();
    
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      configString.append(entry.getKey().trim());
      configString.append("=");
      configString.append(entry.getValue().trim());
      configString.append(";");
    }
    
    return configString.toString();
  }
  
  private static Map<String, String> parseProperties(String line) {
    Map<String, String> properties = new HashMap<String, String>();
    
    String keyValueStrings[] = line.split(";");
    
    for (String keyValueString : keyValueStrings) {
     String keyValuePair[] = keyValueString.split("=");
     
     if (keyValuePair.length == 2) {
       properties.put(keyValuePair[0], keyValuePair[1]);
     }
    }
    
    return properties;
  }
  
  /**
   * Note: This method must not be called by user code! It is only public because the migration
   * code in the Cas Editor Ide Plugin needs to access this method.
   * 
   * @param store
   * @param style
   */
  public static void putAnnotatationStyleToStore(IPreferenceStore store, AnnotationStyle style) {
    
    Color color = new Color(style.getColor().getRed(), style.getColor().getGreen(),
            style.getColor().getBlue());
    
    Map<String, String> styleProperties = new HashMap<String, String>();
    
    styleProperties.put("color", Integer.toString(color.getRGB()));
    styleProperties.put("strategy", style.getStyle().toString());
    styleProperties.put("layer", Integer.toString(style.getLayer()));
    
    if (style.getConfiguration() != null) {
      styleProperties.put("config", style.getConfiguration());
    }
    
    store.setValue(style.getAnnotation() + ".style", serializeProperties(styleProperties));
  }
  
  /**
   * Retrieves an annotation style from the provided preference store.
   * <p>
   * Note: This method must not be called by user code! It is only public because the migration
   * code in the Cas Editor Ide Plugin needs to access this method.
   * 
   * @param store
   * @param typeName
   * @return an annotation style from the provided preference store
   */
  public static AnnotationStyle getAnnotationStyleFromStore(IPreferenceStore store, String typeName) {
    
    Map<String, String> styleProperties = parseProperties(store.getString(typeName + ".style"));
    
    // initialize with random background style for the case if the store contains no style information
    AnnotationStyle.Style style = AnnotationStyle.Style.BACKGROUND;
    int index = (int) Math.round(Math.random() * (DefaultColors.COLORS.length-1));
    Color color = DefaultColors.COLORS[index];
    
    String styleString = styleProperties.get("strategy");
    if (styleString != null && styleString.length() != 0) {
      // TODO: Might throw exception, catch it and use default!
      try {
        style = AnnotationStyle.Style.valueOf(styleString);
      }
      catch (IllegalArgumentException e) {
      }
    }
    
    
    String colorString = styleProperties.get("color");
    if (colorString != null && colorString.length() != 0) {
      try {
        int colorInteger = Integer.parseInt(colorString);
        color = new Color(colorInteger);
      }
      catch (NumberFormatException e) {
      }
    }
    
    int layer = 0;
    
    String layerString = styleProperties.get("layer");
    
    if (layerString != null && layerString.length() != 0) {
      try {
        layer = Integer.parseInt(layerString);
      }
      catch (NumberFormatException e) {
      }
    }
    
    String configuration = styleProperties.get("config");
    
    if (configuration != null && configuration.length() != 0)
      configuration = null;
    
    AnnotationStyle annotationStyle = new AnnotationStyle(typeName, style, color, layer, configuration);
    
    // store style if it is not known yet
    if(styleProperties == null || styleProperties.isEmpty()) {
      putAnnotatationStyleToStore(store, annotationStyle);
    }
    
    return annotationStyle;
  }
}
