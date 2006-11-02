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
import org.apache.uima.resource.metadata.Import;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.taeconfigurator.editors.MultiPageEditor;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;

/**
 */
public class TypeImportSection extends ImportSection {
 
	private TCAS savedTCAS;
	private boolean importWasRemoved;
	
	public TypeImportSection(MultiPageEditor editor, Composite parent) {
		super(editor, parent, "Imported Type Systems", "The following type systems are included as part of this one." );  
	}

	//**************************************
	//* Code to support type import section
	//**************************************
	
	protected boolean isAppropriate(){
	  if (isAggregate()) {
	    getSection().setText("Not Used");
	    getSection().setDescription("Types can't be imported in an Aggregate Descriptor");
	    return false;
	  }
    getSection().setText("Imported Type Systems");
    getSection().setDescription("The following type systems are included as part of this one.");
    return true;
	}
	
	protected String getDescriptionFromImport(String source) throws InvalidXMLException, IOException {
	  TypeSystemDescription parsedImportItem = UIMAFramework.getXMLParser().parseTypeSystemDescription(
        new XMLInputSource(source));
    return parsedImportItem.getDescription();
  }

  protected Import [] getModelImportArray () {
    return getTypeSystemDescription().getImports();
  }
  
  protected void setModelImportArray(Import [] imports) {
    savedTCAS = editor.getTCAS();
    Import [] oldImports = getTypeSystemDescription().getImports();
    importWasRemoved = (null != oldImports) &&
    		               (oldImports.length > imports.length);  
    getTypeSystemDescription().setImports(imports);
  }
  
  protected void clearModelBaseValue() {
    getTypeSystemDescription().setTypes(typeDescription0);
  }
  
  protected boolean isValidImport(String title, String msg) {
    TypeSystemDescription savedTSD = getMergedTypeSystemDescription();
    TypeSystemDescription savedITSD = editor.getImportedTypeSystemDesription();

    try {
      editor.setMergedTypeSystemDescription();
      editor.descriptorTCAS.validate();
    } catch (ResourceInitializationException e1) {
      revertMsg(title, msg, editor.getMessagesToRootCause(e1));
      editor.setMergedTypeSystemDescription(savedTSD);
      editor.setImportedTypeSystemDescription(savedITSD);
      editor.descriptorTCAS.set(savedTCAS);
      return false;
    }
    if (importWasRemoved)
    	if (Window.CANCEL ==Utility.popOkCancel("May need to remove dependencies",
    			"A type import is being removed.  If this would removed some types or features in the" +
					" merged type system, which are referenced in the Capabilities or Indexes " +
					"section, you will need to update those sections as appropriate.",
					MessageDialog.INFORMATION)) {
        revertMsg(title, msg, "Cancelled by user.");
        editor.setMergedTypeSystemDescription(savedTSD);
        editor.setImportedTypeSystemDescription(savedITSD);
        editor.descriptorTCAS.set(savedTCAS);
        return false;
    	}
    		
    return true;
	}


  /**
   * At this point, the basic type system description is updated.
   * and validated.  Validation has updated the merged type system description,
   * and updated the TCAS.
   */
  protected void finishImportChangeAction() {

    // at this point, the tsd validation has updated the resolved version
    editor.getTypePage().getTypeSection().refresh();
    editor.addDirtyTypeName("<import>");
  }
 
}
