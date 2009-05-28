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
 * KIND, either express or implied.  See the L0icense for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.uima.caseditor.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.swt.widgets.Display;

/**
 * An Action to open the Find/Annotate Dialog.
 * 
 * @see FindAnnotateDialog
 */
public class FindAnnotateAction extends Action {

  private AnnotationEditor editor;
  private IFindReplaceTarget target;

  FindAnnotateAction(AnnotationEditor editor, IFindReplaceTarget target) {
    this.editor = editor;
    this.target = target;
  }

  @Override
  public void run() {
    FindAnnotateDialog dialog = new FindAnnotateDialog(
            Display.getCurrent().getActiveShell(),
            editor.getDocument(), target);

    dialog.open();
  }
}
