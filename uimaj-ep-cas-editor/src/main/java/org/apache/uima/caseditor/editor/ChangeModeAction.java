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

import org.apache.uima.cas.Type;
import org.eclipse.jface.action.Action;

/**
 * The {@link ChangeModeAction} changes the editor annotation mode to the newly selected one.
 */
final class ChangeModeAction extends Action {
  /**
   * The {@link AnnotationEditor} the current instance belongs to.
   */
  private AnnotationEditor mEditor;

  /**
   * The new target mode.
   */
  private Type mMode;

  /**
   * Initializes a new instance.
   *
   * @param newMode -target mode
   * @param name - name of the action
   * @param editor
   */
  ChangeModeAction(Type newMode, String name, AnnotationEditor editor) {
    mMode = newMode;
    mEditor = editor;
    setText(name);
  }

  /**
   * Retrieves the document.
   *
   * @return the document
   */
  protected AnnotationDocument getDocument() {
    return mEditor.getDocument();
  }

  /**
   * Executes the current action.
   */
  @Override
  public void run() {
    if (mEditor != null) {
      mEditor.setAnnotationMode(mMode);
    }
  }
}