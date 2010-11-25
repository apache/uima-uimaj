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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.uima.caseditor.editor.util.Span;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationPainter.IDrawingStrategy;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Fills the background of an annotation.
 */
final class BackgroundDrawingStrategy implements IDrawingStrategy {
  /**
   * Fill the background of the given annotation in the specified color.
   *
   * @param annotation
   * @param gc
   * @param textWidget
   * @param offset
   * @param length
   * @param color
   */
  public void draw(Annotation annotation, GC gc, StyledText textWidget, int offset, int length,
          Color color) {
    if (length != 0) {
      if (gc != null) {
    	int annotationBegin = offset;
    	int annotationEnd = offset + length;
    	  
        Rectangle bounds = textWidget.getTextBounds(annotationBegin, annotationEnd - 1);

        // Selection in the text widget are drawn before the annotations are drawn,
        // to make a selection visible the selection is redrawn over the background
        // rectangle
        //
        // The annotation background to be drawn is a span which has a begin and end offset.
        // Inside the background are areas (spans) which should not be over drawn. 
        // That can be visualized like this (annotation goes from first to last z, an 
        // X is the selection which should not be overdrawn):
        //
        // zzzzXXXzzzzXXXXzzzXX
        //
        // The z offsets should be drawn, the X offsets should not be overdrawn. That is solved
        // by drawing for every z offset area one background rectangle.
        
        List<Span> dontOverDrawSpans = new ArrayList<Span>();
        
        Span annotationSpan = new Span(offset, length);
        
        // add all style ranges to the list in the range of the annotation
        for (StyleRange styleRange : textWidget.getStyleRanges(offset, length)) {
        	Span styleRangeSpan = new Span(styleRange.start, styleRange.length);
        	if (styleRangeSpan.getLength() > 0)
        	    dontOverDrawSpans.add(styleRangeSpan);
        }
        
        // add text selection to the list if intersects with annotation
        Point selection = textWidget.getSelection();
        Span selectionSpan = new Span(selection.x, selection.y - selection.x);
        if (annotationSpan.isIntersecting(selectionSpan) && selectionSpan.getLength() > 0) {
          dontOverDrawSpans.add(selectionSpan);
        }
        
        Collections.sort(dontOverDrawSpans);
        // TODO: Asks on mailing list for help ...
        // strange that we need that here ...
        Collections.reverse(dontOverDrawSpans); 
        
        gc.setBackground(color);
        
        if (dontOverDrawSpans.size() > 0) {
	        int zBegin = offset;
	    	for (Span xSpan : dontOverDrawSpans) {
	    	  if (xSpan.getLength() > 0 && zBegin < xSpan.getStart()) {
	    		  Rectangle selectionBounds = textWidget.getTextBounds(zBegin, xSpan.getStart() -1);
	    		  gc.fillRectangle(selectionBounds);
	    	   }
	    	  
	    	  if (zBegin < xSpan.getEnd())
	    	      zBegin = xSpan.getEnd(); 
	    	}
	    	
	    	// If the annotation ends with z offsets these must still be drawn
	    	if (zBegin < annotationEnd) {
	    		  Rectangle selectionBounds = textWidget.getTextBounds(zBegin, annotationEnd -1);
	    		  gc.fillRectangle(selectionBounds);
	    	  }
        }
        else {
  		    Rectangle selectionBounds = textWidget.getTextBounds(annotationBegin, annotationEnd -1);
		      gc.fillRectangle(selectionBounds);
        }
        
        int start = offset;
        int end = offset + length - 1;

        gc.setForeground(new Color(gc.getDevice(), 0, 0, 0));
        
        // Instead of a tab draw textWidget.getTabs() spaces
        String annotationText = textWidget.getText(start, end);
        
        if (annotationText.contains("\t")) {
        	char replacementSpaces[] = new char[textWidget.getTabs()];
        	for (int i = 0; i < replacementSpaces.length; i++) {
        		replacementSpaces[i] = ' ';
        	}
        	annotationText = annotationText.replace(new String(new char[]{'\t'}), new String(replacementSpaces));
        }
        gc.drawText(annotationText, bounds.x, bounds.y, true);
      } 
      else {
        textWidget.redrawRange(offset, length, true);
      }
    }
  }
}
