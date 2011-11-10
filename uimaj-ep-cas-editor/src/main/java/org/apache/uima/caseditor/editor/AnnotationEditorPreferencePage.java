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

package org.apache.uima.caseditor.editor;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * Preference page to manage preferences for the Annotation Editor.
 */
public class AnnotationEditorPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

  private IntegerFieldEditor mEditorLineLengthHint;
  private IntegerFieldEditor mEditorTextSize;
  private BooleanFieldEditor mEditorPartialTypeystem;
  
  public AnnotationEditorPreferencePage() {
    setPreferenceStore(CasEditorPlugin.getDefault().getPreferenceStore());
    setDescription("UIMA Annotation Editor Preferences.");
  }
  

  @Override
  protected void createFieldEditors() {
    // editor line length hint
    mEditorLineLengthHint = new IntegerFieldEditor(
            AnnotationEditorPreferenceConstants.EDITOR_LINE_LENGTH_HINT,
            "Line Length Hint", getFieldEditorParent());
    addField(mEditorLineLengthHint);
    
    // editor text size
    mEditorTextSize = new IntegerFieldEditor(
            AnnotationEditorPreferenceConstants.ANNOTATION_EDITOR_TEXT_SIZE,
            "Editor Text Size", getFieldEditorParent());
    addField(mEditorTextSize);
    
    // load CAS with partial type system
    mEditorPartialTypeystem = new BooleanFieldEditor(
            AnnotationEditorPreferenceConstants.ANNOTATION_EDITOR_PARTIAL_TYPESYSTEM,
            "Load CAS leniently (WARNING: only for experienced users)", getFieldEditorParent());
    addField(mEditorPartialTypeystem);
    
  }

  public void init(IWorkbench workbench) {
  }
  
  @Override
  protected void checkState() {
    super.checkState();
    
    if (mEditorLineLengthHint.getIntValue() > 0) {
      setErrorMessage(null);
      setValid(true);
    }
    else {
      setErrorMessage("Line length hint must be a larger than zero!");
      setValid(false);
    }
    
    if (mEditorTextSize.getIntValue() > 5) {
      setErrorMessage(null);
      setValid(true);
    }
    else {
      setErrorMessage("Editor text size must be a larger than five!");
      setValid(false);
    }
  }
}
