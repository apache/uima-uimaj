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
import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.DotCorpusElement;
import org.apache.uima.caseditor.core.model.INlpElement;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.apache.uima.caseditor.core.model.TypesystemElement;
import org.apache.uima.caseditor.editor.AnnotationEditor;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.eclipse.core.runtime.CoreException;

public class NlpAnnotationPropertyPage extends AnnotationPropertyPage {

  private NlpProject mProject;
  private DotCorpusElement mDotCorpusElement;
  
  protected AnnotationStyle getAnnotationStyle(Type type) {
    return mDotCorpusElement.getAnnotation(type);
  }
  
  protected TypeSystem getTypeSystem() {
    
    // TODO: Refactor this code
    mProject = ((INlpElement) getElement()).getNlpProject();

    mDotCorpusElement = mProject.getDotCorpus();
    
    TypesystemElement tsElement = mProject.getTypesystemElement();
    
    if (tsElement != null) {
      return tsElement.getTypeSystem();
    }
    else {
      return null;
    }
  }
  
  protected boolean saveChanges(Collection<AnnotationStyle> changedStyles) {
    
    for (AnnotationStyle style : changedStyles) {
      mDotCorpusElement.setStyle(style);
    }
    
    // workaround for type system not present problem
    if (mProject.getTypesystemElement() == null
            || mProject.getTypesystemElement().getTypeSystem() == null) {
      return true;
    }

    try {
      mDotCorpusElement.serialize();
    } catch (CoreException e) {
      CasEditorPlugin.log(e);
      return false;
    }
    
    // Repaint annotations of all open editors
    AnnotationEditor editors[] = AnnotationEditor.getAnnotationEditors();
    
    if (editors.length > 0)
      editors[0].getDocumentProvider().fireAnnotationStyleChanged(editors[0].getEditorInput(), 
              changedStyles);
    
    return true;
  }
  
}
