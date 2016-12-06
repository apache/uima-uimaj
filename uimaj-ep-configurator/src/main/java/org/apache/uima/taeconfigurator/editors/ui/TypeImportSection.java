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

import java.io.IOException;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;


/**
 * The Class TypeImportSection.
 */
public class TypeImportSection extends ImportSection {

  /** The saved CAS. */
  private CAS savedCAS;

  /** The import was removed. */
  private boolean importWasRemoved;

  /**
   * Instantiates a new type import section.
   *
   * @param editor the editor
   * @param parent the parent
   */
  public TypeImportSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Imported Type Systems",
            "The following type systems are included as part of this one.");
  }

  // **************************************
  // * Code to support type import section
  // **************************************

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#isAppropriate()
   */
  @Override
  protected boolean isAppropriate() {
    if (isAggregate()) {
      getSection().setText("Not Used");
      getSection().setDescription("Types can't be imported in an Aggregate Descriptor");
      return false;
    }
    getSection().setText("Imported Type Systems");
    getSection().setDescription("The following type systems are included as part of this one.");
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#getDescriptionFromImport(java.lang.String)
   */
  @Override
  protected String getDescriptionFromImport(String source) throws InvalidXMLException, IOException {
    TypeSystemDescription parsedImportItem = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(source));
    return parsedImportItem.getDescription();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#getModelImportArray()
   */
  @Override
  protected Import[] getModelImportArray() {
    return getTypeSystemDescription().getImports();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#setModelImportArray(org.apache.uima.resource.metadata.Import[])
   */
  @Override
  protected void setModelImportArray(Import[] imports) {
    savedCAS = editor.getCurrentView();
    Import[] oldImports = getTypeSystemDescription().getImports();
    importWasRemoved = (null != oldImports) && (oldImports.length > imports.length);
    getTypeSystemDescription().setImports(imports);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#clearModelBaseValue()
   */
  @Override
  protected void clearModelBaseValue() {
    getTypeSystemDescription().setTypes(typeDescription0);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#isValidImport(java.lang.String, java.lang.String)
   */
  @Override
  protected boolean isValidImport(String title, String msg) {
    TypeSystemDescription savedTSD = getMergedTypeSystemDescription();
    TypeSystemDescription savedITSD = editor.getImportedTypeSystemDesription();

    try {
      editor.setMergedTypeSystemDescription();
      editor.descriptorCAS.validate();
    } catch (ResourceInitializationException e1) {
      revertMsg(title, msg, editor.getMessagesToRootCause(e1));
      editor.setMergedTypeSystemDescription(savedTSD);
      editor.setImportedTypeSystemDescription(savedITSD);
      editor.descriptorCAS.set(savedCAS);
      return false;
    }
    if (importWasRemoved)
      if (Window.CANCEL == Utility.popOkCancel("May need to remove dependencies",
              "A type import is being removed.  If this would removed some types or features in the"
                      + " merged type system, which are referenced in the Capabilities or Indexes "
                      + "section, you will need to update those sections as appropriate.",
              MessageDialog.INFORMATION)) {
        revertMsg(title, msg, "Cancelled by user.");
        editor.setMergedTypeSystemDescription(savedTSD);
        editor.setImportedTypeSystemDescription(savedITSD);
        editor.descriptorCAS.set(savedCAS);
        return false;
      }

    return true;
  }

  /**
   * At this point, the basic type system description is updated. and validated. Validation has
   * updated the merged type system description, and updated the CAS.
   */
  @Override
  protected void finishImportChangeAction() {

    // at this point, the tsd validation has updated the resolved version
    editor.getTypePage().getTypeSection().refresh();
    editor.addDirtyTypeName("<import>");
  }

}
