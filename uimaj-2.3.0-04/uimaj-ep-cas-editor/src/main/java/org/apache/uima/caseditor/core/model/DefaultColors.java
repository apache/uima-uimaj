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

package org.apache.uima.caseditor.core.model;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.editor.AnnotationStyle;

/**
 * Utility to assign default colors to the annotation types in the
 * TypeSystem.
 */
class DefaultColors {

  // colors to use for highlighting annotations
  // (use high brightness for best contrast against black text)
  private static final float BRIGHT = 0.95f;
  
  private static final Color[] COLORS = new Color[] {
    // low saturation colors are best, so put them first
    Color.getHSBColor(55f / 360, 0.25f, BRIGHT), // butter yellow?
    Color.getHSBColor(0f / 360, 0.25f, BRIGHT), // pink?
    Color.getHSBColor(210f / 360, 0.25f, BRIGHT), // baby blue?
    Color.getHSBColor(120f / 360, 0.25f, BRIGHT), // mint green?
    Color.getHSBColor(290f / 360, 0.25f, BRIGHT), // lavender?
    Color.getHSBColor(30f / 360, 0.25f, BRIGHT), // tangerine?
    Color.getHSBColor(80f / 360, 0.25f, BRIGHT), // celery green?
    Color.getHSBColor(330f / 360, 0.25f, BRIGHT), // light coral?
    Color.getHSBColor(160f / 360, 0.25f, BRIGHT), // aqua?
    Color.getHSBColor(250f / 360, 0.25f, BRIGHT), // light violet?
    
    // higher saturation colors
    Color.getHSBColor(55f / 360, 0.5f, BRIGHT), 
    Color.getHSBColor(0f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(210f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(120f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(290f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(30f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(80f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(330f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(160f / 360, 0.5f, BRIGHT),
    Color.getHSBColor(250f / 360, 0.5f, BRIGHT),
    
    // even higher saturation colors
    Color.getHSBColor(55f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(0f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(210f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(120f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(290f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(30f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(80f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(330f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(160f / 360, 0.75f, BRIGHT),
    Color.getHSBColor(250f / 360, 0.75f, BRIGHT) };
  
  /**
   * Assigns color to the annotation types in the provided <code>TypeSystem</code>.
   * If a user provides an already existing mapping these will not be changed
   * and taken into account when mapping the not assigned types.
   * 
   * @param ts the <code>TypeSystem</code>
   * @param styles already existing styles which map an annotation to a color
   * @return
   */
  static Collection<AnnotationStyle> assignColors(TypeSystem ts, Collection<AnnotationStyle> styles) {
    
    Map<String, Color> typeNameToColorMap = new HashMap<String, Color>();
    
    for (AnnotationStyle style : styles) {
      typeNameToColorMap.put(style.getAnnotation(), style.getColor());
    }
    
    for (Type type : ts.getProperlySubsumedTypes(ts.getType(CAS.TYPE_NAME_ANNOTATION))) {
      if (!typeNameToColorMap.containsKey(type.getName())) {
        Color c = COLORS[typeNameToColorMap.size() % COLORS.length];
        typeNameToColorMap.put(type.getName(), c);
      }
    }
    
    Set<AnnotationStyle> newStyles = new HashSet<AnnotationStyle>();
    
    for (AnnotationStyle style : styles) {
      typeNameToColorMap.remove(style.getAnnotation());
      newStyles.add(style);
    }
    
    for (Map.Entry<String, Color> entry : typeNameToColorMap.entrySet()) {
      newStyles.add(new AnnotationStyle(entry.getKey(), AnnotationStyle.Style.BACKGROUND,
              entry.getValue(), 0));
    }
    
    return Collections.unmodifiableSet(newStyles);
  }
}
