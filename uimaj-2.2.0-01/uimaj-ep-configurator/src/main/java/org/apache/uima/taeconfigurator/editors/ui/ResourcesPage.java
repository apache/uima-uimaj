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

package org.apache.uima.taeconfigurator.editors.ui;

import org.apache.uima.taeconfigurator.editors.Form2Panel;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.eclipse.ui.forms.IManagedForm;

public class ResourcesPage extends HeaderPageWithSash {

  private ResourceDependencySection resourceDependencySection;

  // private ExternalResourceSection externalResourceSection;
  private ExtnlResBindSection extnlResBindSection;

  private ImportResBindSection resBindImportSection;

  public ResourcesPage(MultiPageEditor editor) {
    super(editor, "Resource Definitions and Bindings");
  }

  /**
   * Called by the framework to fill in the contents
   */
  protected void createFormContent(IManagedForm managedForm) {

    // Only primitive engines can declare Resource Dependencies
    // Both Primitive and aggregates can delcare External Resources and their bindings
    // Bindings always refer to a primitive, if needed via
    // annotatorkey / key / key ... / declaredDependencyName
    // Bindings for same dependencyName at multiple levels:
    // outer ones override inner ones.

    managedForm.getForm().setText("Resources");
    Form2Panel form2panel = setup2ColumnLayout(managedForm, 50, 50);

    managedForm.addPart(extnlResBindSection = new ExtnlResBindSection(editor, form2panel.left));
    managedForm.addPart(resBindImportSection = new ImportResBindSection(editor, form2panel.left));
    managedForm.addPart(resourceDependencySection = new ResourceDependencySection(editor,
            form2panel.right));
    createToolBarActions(managedForm);
  }

  public ResourceDependencySection getResourceDependencySection() {
    return resourceDependencySection;
  }

  public ExtnlResBindSection getResourceBindingsSection() {
    return extnlResBindSection;
  }

  public ImportResBindSection getResBindImportSection() {
    return resBindImportSection;
  }

}
