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
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.eclipse.swt.widgets.Composite;


/**
 * The Class IndexImportSection.
 */
public class IndexImportSection extends ImportSection {

  /**
   * Instantiates a new index import section.
   *
   * @param editor the editor
   * @param parent the parent
   */
  public IndexImportSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Index Imports",
            "The following index definitions are included as part of this one."); // or !
    // DESCRIPTION
  }

  // **************************************
  // * Code to support type import section
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#isAppropriate()
   */
  // **************************************
  @Override
  protected boolean isAppropriate() {
    return true; // always show
  }

  /**
   * used when hovering.
   *
   * @param source the source
   * @return the description from import
   * @throws InvalidXMLException the invalid XML exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Override
  protected String getDescriptionFromImport(String source) throws InvalidXMLException, IOException {
    FsIndexCollection parsedImportItem = UIMAFramework.getXMLParser().parseFsIndexCollection(
            new XMLInputSource(source));
    return parsedImportItem.getDescription();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#getModelImportArray()
   */
  @Override
  protected Import[] getModelImportArray() {
    return getFsIndexCollection().getImports();
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#setModelImportArray(org.apache.uima.resource.metadata.Import[])
   */
  @Override
  protected void setModelImportArray(Import[] imports) {
    if (imports == null)
      throw new InternalErrorCDE("invalid state");
    getFsIndexCollection().setImports(imports);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#clearModelBaseValue()
   */
  @Override
  protected void clearModelBaseValue() {
    getAnalysisEngineMetaData().setFsIndexes(fsIndexDescription0);
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#isValidImport(java.lang.String, java.lang.String)
   */
  // indexes are checked and merged when the CAS is built
  @Override
  protected boolean isValidImport(String title, String message) {

    FsIndexCollection savedIC = editor.getMergedFsIndexCollection();
    if (null != savedIC)
      savedIC = (FsIndexCollection) savedIC.clone();
    CAS savedCAS = editor.getCurrentView();
    try {
      editor.setMergedFsIndexCollection();
      editor.descriptorCAS.validate();
    } catch (ResourceInitializationException e) {
      revertMsg(title, message, editor.getMessagesToRootCause(e));
      editor.setMergedFsIndexCollection(savedIC);
      editor.descriptorCAS.set(savedCAS);
      return false;
    }
    return true;
  }

  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.ImportSection#finishImportChangeAction()
   */
  @Override
  protected void finishImportChangeAction() {
  }

}
