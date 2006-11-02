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

package org.apache.uima.taeconfigurator.editors.ui.dialogs;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.apache.uima.taeconfigurator.CDEpropertyPage;
import org.apache.uima.taeconfigurator.editors.ui.AbstractSection;

public class ExportImportablePartDialog extends AbstractDialog {

	private Text baseFileNameUI;
	
	private Button importByNameUI;
	private Button importByLocationUI;
	public boolean isImportByName;
	
	private String rootPath;

	private DialogModifyListener m_dialogModifyListener = 
		new DialogModifyListener();
	private Text genFilePathUI;
	public String genFilePath;
	public String baseFileName;
		
	private class DialogModifyListener implements ModifyListener {
		public void modifyText(ModifyEvent e) {
			String text = genFilePathUI.getText();
			int pos = text.lastIndexOf(baseFileName + ".xml");
			if (pos == -1) 
				pos = text.length();
			baseFileName = baseFileNameUI.getText();
			genFilePathUI.setText(text.substring(0, pos) + baseFileName + ".xml");
			enableOK();
		}
	}
	
  private class DialogVerifyListener implements VerifyListener {
		public void verifyText(VerifyEvent e) {
			if (0 <= e.text.indexOf('.')) {
				setErrorMessage(MessageFormat.format("invalid character(s): ''{0}''", 
						new Object[] {e.text}));
				e.doit = false;
			}
			else
				setErrorMessage("");
		} 
  }
		
	public ExportImportablePartDialog(AbstractSection aSection) {
		super(aSection, "Export an importable part", 
		    "Specify a base file name, and perhaps alter the path where it should be stored, and press OK");
		rootPath = aSection.editor.getFile().getParent().getLocation().toString() + '/';
	}
	
	protected Control createDialogArea(Composite parent) {

		Composite composite = (Composite)super.createDialogArea(parent);
		AbstractSection.spacer(composite);
				
		createWideLabel(composite, "Base file name (without path or following \".xml\":");
			
		baseFileNameUI = new Text(composite, SWT.BORDER) ;
		baseFileNameUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		baseFileNameUI.addModifyListener(m_dialogModifyListener);
		baseFileNameUI.addVerifyListener(new DialogVerifyListener());
		baseFileName = "";
		
		newErrorMessage(composite);
		
		createWideLabel(composite, "Where the generated part descriptor file will be stored:");
		genFilePathUI = new Text(composite, SWT.BORDER | SWT.H_SCROLL);
		genFilePathUI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		genFilePathUI.setText(rootPath + ".xml");

		new Label(composite, SWT.NONE).setText("");
		importByNameUI = new Button(composite, SWT.RADIO);
		importByNameUI.setText("Import by Name");
		importByNameUI.setToolTipText(
		  "Importing by name looks up the name on the classpath and datapath.");
		
		importByLocationUI = new Button(composite, SWT.RADIO);
		importByLocationUI.setText("Import By Location");
		importByLocationUI.setToolTipText(
				"Importing by location requires a relative or absolute URL");

    String defaultBy = CDEpropertyPage.getImportByDefault(editor.getProject());
    if (defaultBy.equals("location")) {
      importByNameUI.setSelection(false);
      importByLocationUI.setSelection(true);
    }
    else {
      importByNameUI.setSelection(true);
      importByLocationUI.setSelection(false);
    }
    
    baseFileNameUI.setFocus();
		return composite;
	}
	
	public void enableOK() {
		boolean bEnableOk = (baseFileNameUI != null &&  !baseFileNameUI.getText().trim().equals(""));
		okButton.setEnabled(bEnableOk);
	}
	
  /* (non-Javadoc)
   * @see org.apache.uima.taeconfigurator.editors.ui.dialogs.AbstractDialog#copyValuesFromGUI()
   */
  public void copyValuesFromGUI() {
  	genFilePath = genFilePathUI.getText(); 
		isImportByName = importByNameUI.getSelection();
    CDEpropertyPage.setImportByDefault(editor.getProject(), isImportByName ? "name" : "location");
  }

  public boolean isValid() {
    return true;
  }

}

