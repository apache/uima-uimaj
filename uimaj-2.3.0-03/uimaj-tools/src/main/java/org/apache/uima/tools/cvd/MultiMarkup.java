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

package org.apache.uima.tools.cvd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.text.Style;
import javax.swing.text.StyleContext;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationFS;

/**
 * TODO: add type comment for <code>MultiMarkup</code>.
 * 
 * 
 */
public abstract class MultiMarkup {

  private static class Extent {

    protected int start;

    protected int end;

    protected int depth;

    private Extent(int start, int end, int depth) {
      this.start = start;
      this.end = end;
      this.depth = depth;
    }

  }

  private static class AnnotExtent extends Extent {

    private String annotName;

    private AnnotExtent(int start, int end, int depth, String annotName) {
      super(start, end, depth);
      this.annotName = annotName;
    }

  }

  public static AnnotationExtent[] createAnnotationMarkups(FSIterator it, int textLen,
          Map<String, Style> styleMap) {
    List<AnnotExtent> list = new ArrayList<AnnotExtent>();
    list.add(new AnnotExtent(0, textLen, 0, null));
    AnnotationFS fs;
    AnnotExtent ext;
    int pos = 0, tmp;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      fs = (AnnotationFS) it.get();
      // If the annotation is empty, skip.
      if (fs.getEnd() == fs.getBegin()) {
        continue;
      }
      ext = list.get(pos);
      // Skip extents until we have overlap with the current annotation.
      while (fs.getBegin() >= ext.end) {
        ++pos;
        ext = list.get(pos);
      }
      // If the extent starts before the annotation, split the current
      // extent.
      if (ext.start < fs.getBegin()) {
        ++pos;
        list.add(pos,
                new AnnotExtent(fs.getBegin(), ext.end, ext.depth + 1, fs.getType().getName()));
        ext.end = fs.getBegin();
        ext = list.get(pos);
      } else {
        // Start at same point.
        ++ext.depth;
      }
      if (ext.end > fs.getEnd()) {
        // The annotation is shorter than the extent, so we need to
        // split
        // the extent.
        list.add(pos + 1, new AnnotExtent(fs.getEnd(), ext.end, ext.depth - 1, ext.annotName));
        ext.end = fs.getEnd();
        // ++ext.depth;
      } else if (ext.end < fs.getEnd()) {
        // The annotation is longer than the extent, so we increase the
        // depth
        // of the extent until we come to an extent that's at least as
        // long
        // as the annotation.
        tmp = pos;
        while (ext.end < fs.getEnd()) {
          ++tmp;
          ext = list.get(tmp);
          ++ext.depth;
        }
        // We now have an extent that finishes at or after the
        // annotation. If
        // it finishes after, we need to split it. Otherwise, we just
        // increase
        // its depth.
        if (ext.start < fs.getEnd()) {
          list.add(tmp + 1, new AnnotExtent(fs.getEnd(), ext.end, ext.depth, ext.annotName));
          ext.end = fs.getEnd();
        }
        ++ext.depth;
      }
      // else {
      // // Annotation and extent span the same text.
      // ++ext.depth;
      // }

    }
    Style unmarkedStyle = StyleContext.getDefaultStyleContext()
            .getStyle(StyleContext.DEFAULT_STYLE);
    Style annotStyle = styleMap.get(CAS.TYPE_NAME_ANNOTATION);
    // Copy our internal extents to the public representation.
    final int size = list.size();
    AnnotationExtent[] extentArray = new AnnotationExtent[size];
    Style style;
    for (int i = 0; i < size; i++) {
      ext = list.get(i);
      switch (ext.depth) {
        case 0: {
          extentArray[i] = new AnnotationExtent(ext.start, ext.end, unmarkedStyle);
          break;
        }
        case 1: {
          style = styleMap.get(ext.annotName);
          if (style == null) {
            style = annotStyle;
          }
          extentArray[i] = new AnnotationExtent(ext.start, ext.end, style);
          break;
        }
        default: {
          extentArray[i] = new AnnotationExtent(ext.start, ext.end, annotStyle);
          break;
        }
      }
    }
    return extentArray;
  }

  public static MarkupExtent[] createMarkupExtents(FSIterator it, int textLen) {

    List<Extent> list = new ArrayList<Extent>();
    list.add(new Extent(0, textLen, 0));
    AnnotationFS fs;
    Extent ext;
    int pos = 0, tmp;
    for (it.moveToFirst(); it.isValid(); it.moveToNext()) {
      fs = (AnnotationFS) it.get();
      // If the annotation is empty, skip.
      if (fs.getEnd() == fs.getBegin()) {
        continue;
      }
      ext = list.get(pos);
      // Skip extents until we have overlap with the current annotation.
      while (fs.getBegin() >= ext.end) {
        ++pos;
        ext = list.get(pos);
      }
      // If the extent starts before the annotation, split the current
      // extent.
      if (ext.start < fs.getBegin()) {
        ++pos;
        list.add(pos, new Extent(fs.getBegin(), ext.end, ext.depth + 1));
        ext.end = fs.getBegin();
        ext = list.get(pos);
      } else {
        // Start at same point.
        ++ext.depth;
      }
      if (ext.end > fs.getEnd()) {
        // The annotation is shorter than the extent, so we need to
        // split
        // the extent.
        list.add(pos + 1, new Extent(fs.getEnd(), ext.end, ext.depth - 1));
        ext.end = fs.getEnd();
        // ++ext.depth;
      } else if (ext.end < fs.getEnd()) {
        // The annotation is longer than the extent, so we increase the
        // depth
        // of the extent until we come to an extent that's at least as
        // long
        // as the annotation.
        tmp = pos;
        while (ext.end < fs.getEnd()) {
          ++tmp;
          ext = list.get(tmp);
          ++ext.depth;
        }
        // We now have an extent that finishes at or after the
        // annotation. If
        // it finishes after, we need to split it. Otherwise, we just
        // increase
        // its depth.
        if (ext.start < fs.getEnd()) {
          list.add(tmp + 1, new Extent(fs.getEnd(), ext.end, ext.depth));
          ext.end = fs.getEnd();
        }
        ++ext.depth;
      }
      // else {
      // // Annotation and extent span the same text.
      // ++ext.depth;
      // }

    }
    // Copy our internal extents to the public representation.
    final int size = list.size();
    MarkupExtent[] extentArray = new MarkupExtent[size];
    for (int i = 0; i < size; i++) {
      ext = list.get(i);
      extentArray[i] = new MarkupExtent(ext.start, ext.end, ext.depth);
    }
    return extentArray;
  }

}
