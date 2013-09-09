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

package org.apache.uima.caseditor.editor.annotation;

import java.util.HashMap;
import java.util.Map;

import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.apache.uima.caseditor.editor.AnnotationStyle.Style;
import org.eclipse.jface.text.source.AnnotationPainter.IDrawingStrategy;

/**
 * A factory for drawing styles.
 * 
 *  @see org.apache.uima.caseditor.editor.AnnotationStyle.Style
 */
public class DrawingStyle {

  private static Map<AnnotationStyle.Style, IDrawingStrategy> statelessStyles =
      new HashMap<AnnotationStyle.Style, IDrawingStrategy>();
  
  static {
    statelessStyles.put(Style.BACKGROUND, new BackgroundDrawingStrategy());
    statelessStyles.put(Style.TEXT_COLOR, new TextColorDrawingStrategy());
    statelessStyles.put(Style.TOKEN, new TokenDrawingStrategy());
    // deprecated since 3.4, but minimum version is 3.3
    statelessStyles.put(Style.SQUIGGLES, 
            new org.eclipse.jface.text.source.AnnotationPainter.SquigglesStrategy());
    statelessStyles.put(Style.BOX, new BoxDrawingStrategy());
    statelessStyles.put(Style.UNDERLINE, new UnderlineDrawingStrategy());
    statelessStyles.put(Style.BRACKET, new BracketDrawingStrategy());
  }

  private DrawingStyle() {
  }

  /**
   * Retrieves the {@link IDrawingStrategy}.
   *
   * @return the {@link IDrawingStrategy} or null if does not exist.
   */
  public static IDrawingStrategy createStrategy(AnnotationStyle style) {
    
    IDrawingStrategy strategy = statelessStyles.get(style.getStyle());
    
    if (strategy == null && Style.TAG.equals(style.getStyle()) && style.getConfiguration() != null) {
      strategy = new TagDrawingStrategy(style.getConfiguration());
    }
    
    return strategy;
  }
}