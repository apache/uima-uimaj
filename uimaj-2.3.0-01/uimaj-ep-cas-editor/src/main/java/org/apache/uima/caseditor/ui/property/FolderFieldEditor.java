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

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.core.model.NlpProject;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * TODO: add javadoc here
 */
class FolderFieldEditor extends StringButtonFieldEditor {
  private NlpProject mProject;

  FolderFieldEditor(String name, String labelText, Composite parent, NlpProject project) {
    super(name, labelText, parent);

    mProject = project;
  }

  @Override
  protected String changePressed() {
    // TODO: preselect entered text entry

    final ElementTreeSelectionDialog folderSelectionDialog = new ElementTreeSelectionDialog(
            getShell(), new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());

    folderSelectionDialog.setInput(mProject);
    folderSelectionDialog.setTitle(getLabelText());
    folderSelectionDialog.setMessage(getPreferenceName());
    folderSelectionDialog.setValidator(new ISelectionStatusValidator() {
      public IStatus validate(Object[] selection) {
        if (selection.length == 1 && selection[0] instanceof IFolder) {
          return new Status(IStatus.OK, CasEditorPlugin.ID, 0, "", null);
        }

        return new Status(IStatus.ERROR, CasEditorPlugin.ID, 0, "Please select a folder!", null);
      }
    });

    folderSelectionDialog.open();

    Object[] results = folderSelectionDialog.getResult();

    if (results.length != 1) {
      return null;
    }

    return ((IFolder) results[0]).getFullPath().removeFirstSegments(1).toString();
  }

}