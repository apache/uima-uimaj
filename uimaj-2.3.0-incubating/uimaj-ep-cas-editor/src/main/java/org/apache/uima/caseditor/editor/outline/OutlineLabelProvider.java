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

package org.apache.uima.caseditor.editor.outline;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * This <code>OutlineLabelProvider</code> returns the covered text of an <code>AnnotationFS</code>.
 */
class OutlineLabelProvider extends LabelProvider implements ITableLabelProvider {
  public Image getColumnImage(Object element, int columnIndex) {
    // no image available, just return null
    return null;
  }

  public String getColumnText(Object element, int columnIndex) {
    // there is only one column, if column index something
    // else than 0, then there is an error
    if (columnIndex != 0) {
      // ... just return null
      return null;
    }

    AnnotationFS annotation = (AnnotationFS) ((IAdaptable) element).getAdapter(AnnotationFS.class);

    if (annotation != null) {
    	return getStringWithoutNewLine(annotation.getCoveredText());
    }
    
    Type type = (Type) ((IAdaptable) element).getAdapter(Type.class);
    
    if (type != null) {
    	return type.getShortName();
    }
    
    return "Unkown type";
  }

  private static String getStringWithoutNewLine(String string) {
    StringBuilder stringBuilder = new StringBuilder(string.length());

    char stringChars[] = string.toCharArray();

    for (char element : stringChars) {
      if (element == '\r') {
        continue;
      }

      if (element == '\n') {
        stringBuilder.append(' ');
        continue;
      }

      stringBuilder.append(element);
    }

    return stringBuilder.toString();
  }
}