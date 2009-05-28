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

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationPainter.IDrawingStrategy;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
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
        Rectangle bounds = textWidget.getTextBounds(offset, offset + length - 1);

        gc.setBackground(color);
        gc.fillRectangle(bounds);

        int start = offset;
        int end = offset + length;


//        Point selection = textWidget.getSelection();
//        boolean isPartOfAnnotationSelected = selection.x < end && start < selection.y &&
//            selection.y - selection.x > 0;
//        if (isPartOfAnnotationSelected) {
//
//          int startInAnnotation = selection.x < start ? start : selection.x;
//          int endInAnnotation = selection.y < end ? selection.y : end;
//
//          Rectangle boundsInAnnotation = textWidget.getTextBounds(startInAnnotation, endInAnnotation);
//
//          gc.setBackground(new Color(gc.getDevice(), 100, 100, 100));
//          gc.fillRectangle(boundsInAnnotation);
//        }

        gc.setForeground(new Color(gc.getDevice(), 0, 0, 0));

        gc.drawText(textWidget.getText(start, end), bounds.x, bounds.y, true);
      } else {
        textWidget.redrawRange(offset, length, true);
      }
    }
  }
}