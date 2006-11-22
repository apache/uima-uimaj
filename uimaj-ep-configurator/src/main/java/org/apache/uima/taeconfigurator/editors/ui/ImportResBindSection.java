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

import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.impl.ResourceManagerConfiguration_impl;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.util.InvalidXMLException;
import org.eclipse.swt.widgets.Composite;

/**
 */
public class ImportResBindSection extends ImportSection {

  public ImportResBindSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Imports for External Resources and Bindings",
                    "The following definitions are included:"); // or ! DESCRIPTION
  }

  // **************************************
  // * Code to support type import section
  // **************************************
  protected boolean isAppropriate() {
    return true; // always show
  }

  /**
   * used when hovering
   */
  protected String getDescriptionFromImport(String source) {
    return ""; // imports for resource bindings don't have descriptions
  }

  protected Import[] getModelImportArray() {
    return getResourceManagerConfiguration().getImports();
  }

  protected void setModelImportArray(Import[] imports) {
    getResourceManagerConfiguration().setImports(imports);
  }

  protected void clearModelBaseValue() {
    getResourceManagerConfiguration().setExternalResourceBindings(externalResourceBinding0);
    getResourceManagerConfiguration().setExternalResources(externalResourceDescription0);
  }

  // indexes are checked and merged when the TCAS is built
  protected boolean isValidImport(String title, String message) {
    ResourceManagerConfiguration savedRmc = editor.getResolvedExternalResourcesAndBindings();
    if (null != savedRmc)
      savedRmc = (ResourceManagerConfiguration) ((ResourceManagerConfiguration_impl) savedRmc)
                      .clone();
    try {
      editor.setResolvedExternalResourcesAndBindings();
    } catch (InvalidXMLException e) {
      Utility.popMessage(title, message + editor.getMessagesToRootCause(e), Utility.ERROR);
      revert(savedRmc);
      return false;
    }
    if (!isValidAe()) {
      revert(savedRmc);
      return false;
    }
    return true;
  }

  private void revert(ResourceManagerConfiguration rmc) {
    getResourceManagerConfiguration()
                    .setExternalResourceBindings(rmc.getExternalResourceBindings());
    getResourceManagerConfiguration().setExternalResources(rmc.getExternalResources());
    editor.setResolvedExternalResourcesAndBindings(rmc);
  }

  protected void finishImportChangeAction() {
    editor.getResourcesPage().getResourceDependencySection().refresh(); // to change Binding flag
  }

  public void enable() {
    super.enable();
    addButton.setEnabled(true); // can add buttons even for aggregate
  }

}
