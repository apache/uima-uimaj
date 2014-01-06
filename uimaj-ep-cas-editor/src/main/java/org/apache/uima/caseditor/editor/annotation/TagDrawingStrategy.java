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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;



// TODO: Check if its possible to increase the space between characters,
//       or suggest to use mono space font for long tags ...
class TagDrawingStrategy implements IDrawingStrategy {

  private static final int TAG_FONT_SIZE = 11;
  private static final int MAX_LEFT_TAG_OVERLAP = 1;
  private static final int MAX_RIGHT_TAG_OVERLAP = 1;
  private static final int TAG_OVERLAP = MAX_LEFT_TAG_OVERLAP + MAX_RIGHT_TAG_OVERLAP;
  
  private IDrawingStrategy annotationDrawingStyle = new BoxDrawingStrategy();
  
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
    
    // Always draw a box around the annotation itself
    
    // TODO: It seems that the call to the box drawing strategy is rather
    // expensive, test how fast it is when the box drawing is "inlined".
    // The box drawing strategy could be changed to do the drawing via
    // static methods
    annotationDrawingStyle.draw(annotation, gc, textWidget, offset, length, color);
    
    if (annotation instanceof EclipseAnnotationPeer) {
      AnnotationFS annotationFS = ((EclipseAnnotationPeer) annotation).getAnnotationFS();
      
      Feature feature = annotationFS.getType().getFeatureByBaseName(featureName);
      
      if (feature != null && feature.getRange().isPrimitive()) {
        
        String featureValue = annotationFS.getFeatureValueAsString(feature);
        
        // Annotation can be rendered into multiple lines, always draw
        // the tag in the first line where annotation starts
        if (featureValue != null && annotationFS.getBegin() == offset) {
          
          // Calculate how much overhang on both sides of the annotation for a tag is allowed
          int lineIndex = textWidget.getLineAtOffset(offset);
          int firstCharInLineOffset = textWidget.getOffsetAtLine(lineIndex);
          
          int maxBeginOverhang;
          if (firstCharInLineOffset == offset) {
            maxBeginOverhang = 0;
          }
          else {
            maxBeginOverhang = MAX_LEFT_TAG_OVERLAP;
          }
          
          int maxEndOverhang;
          int lineLength;
          int lineCount = textWidget.getLineCount();
          if (lineCount > lineIndex +1) {
            int offsetNextLine = textWidget.getOffsetAtLine(lineIndex + 1);
            lineLength = offsetNextLine - firstCharInLineOffset -1;
          }
          else {
            // Its the last line
            lineLength = textWidget.getCharCount() - offset;
          }
          
          if ((firstCharInLineOffset + lineLength) == offset + length) {
            maxEndOverhang = 0;
          }
          else {
            maxEndOverhang = MAX_RIGHT_TAG_OVERLAP;
          }
        
          Rectangle bounds = textWidget.getTextBounds(offset, offset + length);
  
          if (gc != null) {

            int lastCharIndex;
            
            if (length == 0)
              lastCharIndex = offset;
            else
              lastCharIndex = offset + length - 1;
            
            Point annotationStringExtent = gc.stringExtent(
                    textWidget.getText(offset, lastCharIndex));
            
            Font currentFont = gc.getFont();
            
            // TODO: Figure out if that is safe
            Font tagFont = new Font(currentFont.getDevice(), currentFont.getFontData()[0].getName(),
                    TAG_FONT_SIZE, currentFont.getFontData()[0].getStyle());
            gc.setFont(tagFont);
            gc.setForeground(color);
            
            // Cutoffs the tag if two chars longer than annotation
            // and replaces it with a scissor
            int maxAllowedLength = length + maxBeginOverhang + maxEndOverhang;
            if (featureValue.length() > maxAllowedLength) {
              if (length > 0) {
                // Trim featureValue to substring which is length -1 + scissor symbol
                char scissorChar = (char) 0x2704;
                featureValue = featureValue.substring(0, maxAllowedLength - 1) + scissorChar;
              }
              else
                // If zero length annotation, just draw nothing
                featureValue = "";
            }
            
            // Drawing after the end of a line, and before the begin does not work ...
            Point tagStringExtent = gc.stringExtent(featureValue);
            
            int centerOfAnnotation = annotationStringExtent.x / 2;
            int centerOfTag = tagStringExtent.x / 2;
            
            // Tag can be positioned at three different places
            // if there is an overhang on both side, its centered
            int newX = bounds.x + centerOfAnnotation - centerOfTag;
            
            // Figure out if there is an overhang allowed, and recalculate the position.
            
            // No overhang means that the annotation is the first or last in the line,
            // in this case the tag should be placed as close as possible to the begin
            // or end of the annotation boundary.
            
            // if no overhang on the left side allowed, 
            // the tag must start with the annotation bound
            if (maxBeginOverhang == 0) {
              newX = bounds.x;
            }
            // if no overhang on the right side allowed, 
            // the tag must end with the annotation bound
            else if (maxEndOverhang == 0) {
              newX = bounds.x + (annotationStringExtent.x - tagStringExtent.x);
            }
            
            // TODO: Might not be too safe, if bounds.height == 0,
            // passed parameter could be -1
            gc.drawString(featureValue, newX, bounds.y + bounds.height - 1, true);
            gc.setFont(currentFont);
            
            // After done using the tag font it must be disposed, otherwise
            // eclipse on windows runs out of handles and crashes
            tagFont.dispose();
          } else {
            
            // The area into which the tag will be drawn must be marked
            // to be redrawn, that requires a calculation of that area.
            
            // Note: Did not find a way to calculate the tag extent correctly here
            // GC.stringExtent cannot be called, because the passed GC is null
            // 
            // The width is assumed to be the font size multiplied by the length
            // of annotation string + TAG_OVERLAP, this is not optimal, but should give
            // a little to large estimate
            // 
            // Note: It unknown what happens if a too big area is drawn
            // Doesn't seem to cause a crash on OS X
            // TODO: Test that on windows 7, Windows XP and Linux
           
            textWidget.redraw(bounds.x - TAG_FONT_SIZE * TAG_OVERLAP, bounds.y + bounds.height -1, 
                    TAG_FONT_SIZE * (length + TAG_OVERLAP), 
                    bounds.height, true);
          }
        }
      }
    }
  }
}
