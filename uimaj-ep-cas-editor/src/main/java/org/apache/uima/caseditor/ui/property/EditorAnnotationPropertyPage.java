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

package org.apache.uima.caseditor.ui.property;

import java.util.Collection;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.apache.uima.caseditor.editor.styleview.AnnotationTypeNode;

public class EditorAnnotationPropertyPage extends AnnotationPropertyPage {

  AnnotationEditor getEditor() {
    AnnotationTypeNode typeNode = (AnnotationTypeNode) getElement().getAdapter(AnnotationTypeNode.class);
    
    return typeNode.getEditor();
  }
  
  @Override
  protected AnnotationStyle getAnnotationStyle(Type type) {
    return getEditor().getAnnotationStyle(type);
  }

  @Override
  protected TypeSystem getTypeSystem() {
    return getEditor().getDocument().getCAS().getTypeSystem();
  }

  @Override
  protected boolean saveChanges(Collection<AnnotationStyle> changedStyles) {
    
    // TODO: Add method to change all styles at once, instead of writing
    // the dotCorpus file for changed style
    for (AnnotationStyle style : changedStyles) {
      getEditor().setAnnotationStyle(style);
    }
    
    return true;
  }
}
