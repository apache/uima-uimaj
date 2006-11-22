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
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.taeconfigurator.InternalErrorCDE;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.eclipse.swt.widgets.Composite;

/**
 */
public class IndexImportSection extends ImportSection {

  public IndexImportSection(MultiPageEditor editor, Composite parent) {
    super(editor, parent, "Index Imports",
                    "The following index definitions are included as part of this one."); // or !
                                                                                          // DESCRIPTION
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
  protected String getDescriptionFromImport(String source) throws InvalidXMLException, IOException {
    FsIndexCollection parsedImportItem = UIMAFramework.getXMLParser().parseFsIndexCollection(
                    new XMLInputSource(source));
    return parsedImportItem.getDescription();
  }

  protected Import[] getModelImportArray() {
    return getFsIndexCollection().getImports();
  }

  protected void setModelImportArray(Import[] imports) {
    if (imports == null)
      throw new InternalErrorCDE("invalid state");
    getFsIndexCollection().setImports(imports);
  }

  protected void clearModelBaseValue() {
    getAnalysisEngineMetaData().setFsIndexes(fsIndexDescription0);
  }

  // indexes are checked and merged when the TCAS is built
  protected boolean isValidImport(String title, String message) {

    FsIndexCollection savedIC = editor.getMergedFsIndexCollection();
    if (null != savedIC)
      savedIC = (FsIndexCollection) savedIC.clone();
    TCAS savedTCAS = editor.getTCAS();
    try {
      editor.setMergedFsIndexCollection();
      editor.descriptorTCAS.validate();
    } catch (ResourceInitializationException e) {
      revertMsg(title, message, editor.getMessagesToRootCause(e));
      editor.setMergedFsIndexCollection(savedIC);
      editor.descriptorTCAS.set(savedTCAS);
      return false;
    }
    return true;
  }

  protected void finishImportChangeAction() {
  }

}
