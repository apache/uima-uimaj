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

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationPainter.IDrawingStrategy;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;

// TODO: needs a path to a feature to draw
// TODO: Annotation Editor needs to modify line spacing
// TODO: needs a strategy to avoid overlaps
// TODO: Check if its possible to increase the space which is used to
//       draw a white space (e.g. space char, tab char)
// Add to annotation editor: sourceViewer.getTextWidget().setLineSpacing(12);
/**
 * Experimental implementation, to figure out how tag drawing could be implemented.
 * To test it, add the line spacing to the Annotation Editor and enable this class to be a
 * drawing strategy.
 * 
 * Issue for this work is UIMA-1875.
 */
class TagDrawingStrategy implements IDrawingStrategy {

  /**
   * The name of the feature to print.
   */
  private final String featureName;
  
  TagDrawingStrategy(String featureName) {
    
    if (featureName == null)
      throw new IllegalArgumentException("featureName must not be null!");
    
    this.featureName = featureName;
  }
  
  public void draw(Annotation annotation, GC gc, StyledText textWidget, int offset, int length,
          Color color) {
    
    if (annotation instanceof EclipseAnnotationPeer) {
      AnnotationFS annotationFS = ((EclipseAnnotationPeer) annotation).getAnnotationFS();
      
      Feature feature = annotationFS.getType().getFeatureByBaseName(featureName);
      
      if (feature != null && feature.getRange().isPrimitive()) {
        
        String featureValue = annotationFS.getFeatureValueAsString(feature);
        
        if (featureValue != null) {
          Rectangle bounds = textWidget.getTextBounds(offset, offset + length - 1);
  
          if (gc != null && featureValue != null) {
  
            gc.setForeground(color);
  
            Font currentFont = gc.getFont();
            
            // TODO: Figure out if that is safe
            Font tagFont = new Font(currentFont.getDevice(), currentFont.getFontData()[0].getName(),
                    12, currentFont.getFontData()[0].getStyle());
            gc.setFont(tagFont);
            
            // TODO: if string does not fit, still draw ?!
            
            gc.drawString(featureValue, bounds.x, bounds.y + bounds.height - 2, true);
            gc.setFont(currentFont);
            
          } else {
            // TODO: How to calculate redraw area ?!
            textWidget.redraw(bounds.x, bounds.y + bounds.height -2, bounds.width, bounds.height, true);
          }
        }
      }
    }
  }
}
